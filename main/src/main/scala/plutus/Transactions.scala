package plutus

import cats.data.Validated
import cats.data.ValidatedNel
import cats.effect.*
import cats.syntax.all.*
import com.monovore.decline.*

// One command for copying transactions, named for what it does rather than for
// either end of it, because neither end is fixed any more: --from-… says where
// they come from and --to-… where they go, and all four pairs are runs someone
// wants — including CSV to OFX, which is neither an import nor an export in
// any sense a user would recognise. Stating both ends is also what lets
// decline refuse what doesn't exist (two sources, no sink, the API to OFX
// with no book) as a usage error rather than as a flag quietly ignored, and what
// puts the whole matrix in --help: a sum of products renders as one usage line
// per pair.
lazy val transactionsOpts: Opts[IO[Unit]] = Opts.subcommand(
  name = "transactions",
  help = "Copy Monzo transactions into a GnuCash book or an OFX file."
):
  (
    verbosityOpts,
    transactionsRunOpts,
    transactionsDryRunOpts
  ).tupled.map: (verbosity, run, dryRun) =>
    run(dryRun)(using verbosity)

// What a run is, once its options are parsed: a source wired to a sink, still
// to be told whether it's a dry run. The sinks aren't peers beyond that — the
// book sink needs a lock, a backup and a transaction, the OFX sink a path —
// so this names what they share rather than a trait spanning what they don't.
// Verbosity stays a context parameter, as everywhere else, so a context
// function type rather than a second argument.
type TransactionsRun = Boolean => Verbosity ?=> IO[Unit]

// One branch per way of deciding the window, each with the sources it
// accepts, rather than a product of every source with every sink. Nothing
// records where a run left off except a book, so an API run always reconciles
// against one — --to-book, or --to-ofx with --book — and a CSV run needs one
// only to write into it or to leave out what it already holds.
lazy val transactionsRunOpts: Opts[TransactionsRun] =
  toBookRunOpts orElse ofxFromBookRunOpts orElse ofxRunOpts

// --ignore-lock nests here, since gnclock is a book concern and means nothing
// to a run that only reads one.
lazy val toBookRunOpts: Opts[TransactionsRun] =
  (bookSourceOpts, toBookPathOpts, ignoreLockOpts).tupled.map:
    (source, input, ignoreLock) =>
      dryRun => gnuCashTransactionSink(source, input, dryRun, ignoreLock)

lazy val ofxFromBookRunOpts: Opts[TransactionsRun] =
  (bookSourceOpts, toOfxPathOpts, bookPathOpts).tupled.map:
    (source, output, book) =>
      dryRun => ofxTransactionSinkFromBook(source, book, output, dryRun)

// A statement is its own window, so CSV to OFX needs no book; the API has no
// window of its own to offer, so it only reaches OFX through one.
lazy val ofxRunOpts: Opts[TransactionsRun] =
  (csvSourceOpts, toOfxPathOpts).tupled.map: (source, output) =>
    dryRun => ofxTransactionSink(source, output, dryRun, filed = Set.empty)

// A source that hasn't yet been told where each account starts, because that
// is the book's answer, read before the fetch. See BookStarts.
type TransactionSourceFor = BookStarts => TransactionSource

lazy val bookSourceOpts: Opts[TransactionSourceFor] =
  monzoFromBookSourceOpts orElse csvSourceOpts.map: source =>
    (_: BookStarts) => source

lazy val monzoFromBookSourceOpts: Opts[TransactionSourceFor] =
  fromMonzoOpts.map: _ =>
    starts => monzoTransactionSource(starts)

lazy val fromMonzoOpts: Opts[Unit] =
  Opts.flag(
    "from-monzo",
    help =
      "Read transactions from the Monzo API. Requires authorisation on first use, and holds a refresh token in the Keychain from then on."
  )

// Either option on its own is a run, so the branch is the union of the two
// products rather than one product of two optional lists: a pot backfill is
// the whole reason this source exists and needs no main account's statement,
// while an all-optional branch would match a command line naming no source at
// all and make `orElse` above ambiguous rather than exclusive.
lazy val csvSourceOpts: Opts[TransactionSource] =
  ((fromCsvOpts, fromCsvPotOpts.withDefault(Nil)).tupled
    orElse (fromCsvOpts.withDefault(Nil), fromCsvPotOpts).tupled)
    .map: (accounts, potAccounts) =>
      csvTransactionSource(accounts, potAccounts)

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

// Same as --to-book's path, but read-only: an OFX run reconciled against a
// book never writes to it, so it takes no lock and no backup.
lazy val bookPathOpts: Opts[fs2.io.file.Path] =
  Opts
    .flagOption[java.nio.file.Path](
      "book",
      metavar = "path",
      help =
        "With --to-ofx, start each account where a GnuCash SQLite3 file left off, as --to-book does, and leave out transactions already in it. The book is only read. If no path is given defaults to Accounts.gnucash in the current directory; give one as --book=PATH."
    )
    .map:
      _.fold(fs2.io.file.Path("Accounts.gnucash")):
        fs2.io.file.Path.fromNioPath

lazy val transactionsDryRunOpts: Opts[Boolean] =
  Opts
    .flag(
      "dry-run",
      help =
        "Print what would be copied without writing to the book or the OFX file, and without taking a backup."
    )
    .orFalse
