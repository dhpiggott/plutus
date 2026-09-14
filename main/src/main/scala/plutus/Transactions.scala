package plutus

import cats.data.Validated
import cats.data.ValidatedNel
import cats.effect.*
import cats.syntax.all.*
import com.monovore.decline.*
import com.monovore.decline.time.*

import java.time.Instant
import java.time.ZoneId

// One command for copying transactions, named for what it does rather than for
// either end of it, because neither end is fixed any more: --from-… says where
// they come from and --to-… where they go, and all four pairs are runs someone
// wants — including CSV to OFX, which is neither an import nor an export in
// any sense a user would recognise. Stating both ends is also what lets
// decline refuse what doesn't exist (two sources, no sink, --since with a CSV)
// as a usage error rather than as a flag quietly ignored, and what puts the
// whole matrix in --help: a product of two sums renders as one usage line per
// pair.
lazy val transactionsOpts: Opts[IO[Unit]] = Opts.subcommand(
  name = "transactions",
  help = "Copy Monzo transactions into a GnuCash book or an OFX file."
):
  (
    verbosityOpts,
    sourceOpts,
    sinkOpts,
    transactionsDryRunOpts
  ).tupled.map: (verbosity, source, sink, dryRun) =>
    sink(source, dryRun)(using verbosity)

// A source that hasn't yet been told whether a successful run should advance
// the Monzo bookmarks, because that is the sink's answer rather than the
// command line's: the book dedups on the online_id slot instead, so a run that
// advanced them would make the next OFX export skip the window it had just
// filed. The CSV source ignores it — it has no bookmarks to advance and no
// state store to advance them in.
type TransactionSourceFor = Boolean => TransactionSource

lazy val sourceOpts: Opts[TransactionSourceFor] =
  monzoSourceOpts orElse csvSourceOpts

lazy val monzoSourceOpts: Opts[TransactionSourceFor] =
  (
    Opts.flag(
      "from-monzo",
      help =
        "Read transactions from the Monzo API. Requires authorisation on first use, and holds a refresh token in the Keychain from then on."
    ),
    sinceOpts,
    beforeOpts
  ).tupled.map: (_, since, before) =>
    advanceBookmarks => monzoTransactionSource(since, before, advanceBookmarks)

// Windowing options on the API call rather than on the command, so they're
// unparseable alongside --from-csv rather than accepted and ignored: a
// statement is its own window, with nothing to bound.
lazy val sinceOpts: Opts[Option[Instant]] =
  Opts
    .option[Instant](
      "since",
      help =
        "Timestamp to read transactions from. If not specified defaults to the last recorded transaction ID for each account, unless there is no last recorded transaction for that account, in which case no transactions will be read for it."
    )
    .orNone

lazy val beforeOpts: Opts[Option[Instant]] =
  Opts
    .option[Instant](
      "before",
      help =
        "Timestamp to read transactions to. If not specified defaults to now."
    )
    .orNone

// Either option on its own is a run, so the branch is the union of the two
// products rather than one product of two optional lists: a pot backfill is
// the whole reason this source exists and needs no main account's statement,
// while an all-optional branch would match a command line naming no source at
// all and make `orElse` above ambiguous rather than exclusive.
lazy val csvSourceOpts: Opts[TransactionSourceFor] =
  ((fromCsvOpts, fromCsvPotOpts.withDefault(Nil), fromCsvZoneOpts).tupled
    orElse (
      fromCsvOpts.withDefault(Nil),
      fromCsvPotOpts,
      fromCsvZoneOpts
    ).tupled)
    .map: (accounts, potAccounts, zone) =>
      _ => csvTransactionSource(accounts, potAccounts, zone)

lazy val fromCsvOpts: Opts[List[(monzo.AccountId, fs2.io.file.Path)]] =
  Opts
    .options[String](
      "from-csv",
      metavar = "id=path",
      help =
        "Read an account's transactions from a CSV statement exported by the Monzo app, given as the Monzo account ID the statement belongs to and the path to it. Repeatable, once per account."
    )
    .mapValidated:
      _.traverse(accountStatement)
    .map:
      _.toList

// ACCOUNT_ID=PATH, split at the first = and no further: a path may hold one
// and a Monzo account ID may not.
def accountStatement(
    value: String
): ValidatedNel[String, (monzo.AccountId, fs2.io.file.Path)] =
  value.split("=", 2) match
    case Array(accountId, path) if accountId.nonEmpty && path.nonEmpty =>
      Validated.validNel:
        monzo.AccountId(accountId) -> fs2.io.file.Path(path)
    case _ =>
      Validated.invalidNel:
        s"Not an ACCOUNT_ID=PATH pair: $value."

// A separate option rather than a prefix on the one above, because it matches
// how the app exports — an account statement and a pot statement come from two
// different screens — and because what it sets has to be stated rather than
// derived: see FetchedAccount.
lazy val fromCsvPotOpts: Opts[List[(monzo.AccountId, fs2.io.file.Path)]] =
  Opts
    .options[String](
      "from-csv-pot",
      metavar = "id=path",
      help =
        "Read a pot's transactions from a CSV statement exported by the Monzo app, given as the pot's backing account ID and the path to the statement. Repeatable, once per pot."
    )
    .mapValidated:
      _.traverse(accountStatement)
    .map:
      _.toList

// Left unresolved here rather than defaulted to a zone: a region ID is only
// resolvable where a tzdb is (Scala Native ships none, so ZoneId.of("Europe/
// London") throws on that row), and this is a lazy val forced while the
// command is built, so a default naming one would take down --help and every
// other command with it. The source falls back to the machine's own zone,
// which is also what the book sink normalises post dates against, so one run
// reads one zone unless told otherwise.
lazy val fromCsvZoneOpts: Opts[Option[ZoneId]] =
  Opts
    .option[String](
      "from-csv-zone",
      help =
        "Time zone the CSV statements' Date and Time columns are stamped in. If not specified defaults to this machine's own zone. A named region (Europe/London) needs a time zone database, which the JVM build has and the Scala Native build doesn't; a fixed offset (+01:00) works on both."
    )
    .mapValidated: zone =>
      Validated
        .catchNonFatal:
          ZoneId.of(zone)
        .leftMap: _ =>
          s"Not a time zone this build can resolve: $zone."
        .toValidatedNel
    .orNone

// What a sink is, once its own options are parsed: something that consumes a
// source and honours --dry-run. The two aren't peers beyond that — the book
// sink needs a lock, a backup and a transaction, the OFX sink a path and an
// overwrite rule — so this names what they share rather than a trait spanning
// what they don't. Verbosity stays a context parameter, as everywhere else, so
// a context function type rather than a third argument.
type TransactionSink =
  (TransactionSourceFor, Boolean) => Verbosity ?=> IO[Unit]

lazy val sinkOpts: Opts[TransactionSink] = toBookOpts orElse toOfxOpts

// --ignore-lock nests here, since gnclock is a book concern and means nothing
// to a run writing an OFX file.
lazy val toBookOpts: Opts[TransactionSink] =
  (toBookPathOpts, ignoreLockOpts).tupled.map: (input, ignoreLock) =>
    (source, dryRun) =>
      // Never !dryRun: the book dedups on the online_id slot rather than on
      // bookmarks, so advancing them would make the next OFX export skip the
      // window this run just imported. See monzoTransactionSource.
      importTransactions(source(false), input, dryRun, ignoreLock)

// An optional-argument option (--to-book, or --to-book=PATH), so naming the
// sink doesn't force a path on a run that wants the default. decline binds an
// optional argument through the = form only, so the space-separated
// --to-book PATH is a usage error rather than a path silently ignored.
lazy val toBookPathOpts: Opts[fs2.io.file.Path] =
  Opts
    .flagOption[java.nio.file.Path](
      "to-book",
      metavar = "path",
      help =
        "Write transactions into a GnuCash SQLite3 file. If no path is given defaults to Accounts.gnucash in the current directory; give one as --to-book=PATH."
    )
    .map:
      _.fold(fs2.io.file.Path("Accounts.gnucash")):
        fs2.io.file.Path.fromNioPath

lazy val toOfxOpts: Opts[TransactionSink] =
  toOfxPathOpts.map: output =>
    (source, dryRun) => exportTransactions(source(!dryRun), output, dryRun)

lazy val toOfxPathOpts: Opts[fs2.io.file.Path] =
  Opts
    .flagOption[java.nio.file.Path](
      "to-ofx",
      metavar = "path",
      help =
        "Write transactions to an OFX file, for importing with GnuCash's own importer. If no path is given defaults to monzo.ofx in the current directory; give one as --to-ofx=PATH."
    )
    .map:
      _.fold(fs2.io.file.Path("monzo.ofx")):
        fs2.io.file.Path.fromNioPath

// One flag for both ends of the run: whichever sink was named writes nothing,
// and the Monzo source leaves its bookmarks where they were.
lazy val transactionsDryRunOpts: Opts[Boolean] =
  Opts
    .flag(
      "dry-run",
      help =
        "Print what would be copied without writing to the book or the OFX file, without taking a backup, and without updating the state file's last-transactions bookmarks."
    )
    .orFalse
