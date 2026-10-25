package plutus

import cats.data.Validated
import cats.data.ValidatedNel
import cats.effect.*
import cats.syntax.all.*
import com.monovore.decline.*
import com.monovore.decline.time.*

import java.time.Instant

// One command for copying transactions, named for what it does rather than for
// either end of it, because neither end is fixed any more: --from-… says where
// they come from and --to-… where they go, and all four pairs are runs someone
// wants — including CSV to OFX, which is neither an import nor an export in
// any sense a user would recognise. Stating both ends is also what lets
// decline refuse what doesn't exist (two sources, no sink, --since with a CSV
// or a book) as a usage error rather than as a flag quietly ignored, and what
// puts the whole matrix in --help: a sum of products renders as one usage line
// per pair.
lazy val transactionsOpts: Opts[IO[Unit]] = Opts.subcommand(
  name = "transactions",
  help = "Copy Monzo transactions into a GnuCash book or an OFX file."
):
  (
    verbosityOpts,
    sourceAndSinkOpts,
    transactionsDryRunOpts
  ).tupled.map: (verbosity, sourceAndSink, dryRun) =>
    val (source, sink) = sourceAndSink
    sink(source, dryRun)(using verbosity)

// One branch per sink, each with the sources it accepts, rather than a product
// of every source with every sink, because --since and --before only mean
// something to one of them: the book already knows where each account left
// off, from what it has filed (see startsFromBook), so a --since on it could
// only skip rows or re-fetch ones dedup would skip anyway, and a --before
// would only leave rows for the next run to fetch.
lazy val sourceAndSinkOpts: Opts[(TransactionSourceFor, TransactionSink)] =
  (toBookSourceOpts, toBookOpts).tupled orElse
    (toOfxSourceOpts, toOfxOpts).tupled

// A source that hasn't yet been told where its window starts when --since
// doesn't say, because that is the sink's answer rather than the command
// line's: the OFX sink resumes from the Monzo bookmarks and advances them,
// while the book resumes from what it has already filed and leaves them alone.
// See Resume.
type TransactionSourceFor = Resume => TransactionSource

lazy val toBookSourceOpts: Opts[TransactionSourceFor] =
  monzoToBookSourceOpts orElse csvSourceOpts

lazy val monzoToBookSourceOpts: Opts[TransactionSourceFor] =
  fromMonzoOpts.map: _ =>
    resume => monzoTransactionSource(since = None, before = None, resume)

lazy val fromMonzoOpts: Opts[Unit] =
  Opts.flag(
    "from-monzo",
    help =
      "Read transactions from the Monzo API. Requires authorisation on first use, and holds a refresh token in the Keychain from then on."
  )

lazy val toOfxSourceOpts: Opts[TransactionSourceFor] =
  monzoToOfxSourceOpts orElse csvSourceOpts

lazy val monzoToOfxSourceOpts: Opts[TransactionSourceFor] =
  (fromMonzoOpts, sinceOpts, beforeOpts).tupled.map: (_, since, before) =>
    resume => monzoTransactionSource(since, before, resume)

// Windowing options on the API call rather than on the command, so they're
// unparseable alongside --from-csv rather than accepted and ignored: a
// statement is its own window, with nothing to bound.
lazy val sinceOpts: Opts[Option[Instant]] =
  Opts
    .option[Instant](
      "since",
      help =
        "Timestamp to read transactions from. If not specified defaults to each account's last exported transaction; an account with none is not read."
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
  ((fromCsvOpts, fromCsvPotOpts.withDefault(Nil)).tupled
    orElse (fromCsvOpts.withDefault(Nil), fromCsvPotOpts).tupled)
    .map: (accounts, potAccounts) =>
      _ => csvTransactionSource(accounts, potAccounts)

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

// What a sink is, once its own options are parsed: something that consumes a
// source and honours --dry-run. The two aren't peers beyond that — the book
// sink needs a lock, a backup and a transaction, the OFX sink a path and an
// overwrite rule — so this names what they share rather than a trait spanning
// what they don't. Verbosity stays a context parameter, as everywhere else, so
// a context function type rather than a third argument.
type TransactionSink =
  (TransactionSourceFor, Boolean) => Verbosity ?=> IO[Unit]

// --ignore-lock nests here, since gnclock is a book concern and means nothing
// to a run writing an OFX file.
lazy val toBookOpts: Opts[TransactionSink] =
  (toBookPathOpts, ignoreLockOpts).tupled.map: (input, ignoreLock) =>
    (source, dryRun) =>
      gnuCashTransactionSink(source, input, dryRun, ignoreLock)

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
    (source, dryRun) =>
      ofxTransactionSink(
        source(Resume.FromBookmarks(advance = !dryRun)),
        output,
        dryRun
      )

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
