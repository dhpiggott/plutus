package plutus

import cats.effect.*
import cats.syntax.all.*
import fs2.data.csv.CsvException
import fs2.data.csv.CsvRow
import smithy4s.Document
import smithy4s.json.Json
import smithy4s.time.Timestamp

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

// The CSV statements the Monzo app exports, read as a transaction source: one
// file per account or pot, and no session at all — no OAuth client, no refresh
// token, no state store. That is the point of it rather than a side effect.
// The API caps a pot's window at 90 days once Strong Customer Authentication
// has lapsed, so a statement is the only way to reach a pot's older interest,
// and the same IDs come back either way (the export's Transaction ID column
// carries the tx_… the API returns), so online_id dedup makes a CSV import and
// an API import of overlapping windows idempotent in both directions.
//
// A statement has no account-ID column, so each file is paired on the command
// line with the account it belongs to, and whether that account backs a pot is
// stated the same way (--from-csv-pot) rather than inferred: the Monzo source
// recognises a pot backing account by its missing type, which is an artefact
// of /accounts not listing them and answers nothing for a bare acc_… (see
// FetchedAccount).
//
// `pots` comes back empty, because /pots is what fills it, and no sink is left
// guessing by that: a pot whose asset account the book already carries
// resolves by its online_id tag with no name needed, and a pot the book has
// never seen fails the run rather than being mis-filed. Nothing infers closure
// either — a statement says nothing about whether the account or the pot
// behind it still exists — so archiving is left to a later API run.
def csvTransactionSource(
    accounts: List[(monzo.AccountId, fs2.io.file.Path)],
    potAccounts: List[(monzo.AccountId, fs2.io.file.Path)],
    zone: Option[ZoneId]
): TransactionSource = new TransactionSource:
  def use[A](now: Instant)(consume: Fetched => IO[A])(using
      verbosity: Verbosity
  ): IO[A] =
    val statements =
      accounts.map((accountId, path) =>
        (accountId = accountId, path = path, potBacking = false)
      ) ++ potAccounts.map((accountId, path) =>
        (accountId = accountId, path = path, potBacking = true)
      )
    for
      // The zone the statements' local Date and Time are read in, defaulting
      // to this machine's own — the same one the book sink normalises post
      // dates against, so a run agrees with itself. Read here rather than
      // where the option is parsed because a region ID needs a time zone
      // database and Scala Native has none: on that row this is a fixed
      // offset, so a statement spanning a daylight-saving change wants
      // --from-csv-zone and the JVM build. See fromCsvZoneOpts.
      resolvedZone <- zone.fold(IO.delay(ZoneId.systemDefault))(IO.pure)
      // One account, one statement. Two files under one ID would both be read
      // and both filed, and the run's own duplicate check would then fail on
      // every row they share (see importTransactions) — after the whole of
      // both files had been decoded, and with a message about Monzo rather
      // than about the command line.
      _ <- IO.raiseUnless(
        statements.map(_.accountId).distinct.sizeIs == statements.size
      ):
        Error(
          s"More than one statement given for the same account: ${statements
              .groupBy(_.accountId)
              .collect:
                case (accountId, duplicates) if duplicates.sizeIs > 1 =>
                  s"${accountId.value} (${duplicates.map(_.path.toString).sorted.mkString(", ")})"
              .toList
              .sorted
              .mkString("; ")}."
        )
      byAccount <- statements.traverse: statement =>
        csvStatement(statement.path, resolvedZone).map: read =>
          FetchedAccount(
            id = statement.accountId,
            // A statement names no type, and the ID alone can't be looked up
            // without the API. Resolution is by the book's own online_id tags
            // instead — see importTransactions.
            accountType = None,
            // Nothing in a statement says the account behind it is closed;
            // absence of one says nothing either, since the run is only given
            // the files it was asked to import.
            closed = false,
            potBacking = statement.potBacking,
            currency = read.currency
          ) -> read.transactions
      _ <- log(Verbosity.VERBOSE):
        Json.writeDocumentAsPrettyString:
          Document.array:
            byAccount.map: (account, transactions) =>
              Document.obj(
                "account" -> Document.encode:
                  account.id
                ,
                "transactions" -> Document.array:
                  transactions.map:
                    Document.encode(_)
              )
      result <- consume(
        (
          byAccount = byAccount,
          pots = Map.empty[monzo.AccountId, monzo.Pot],
          // A statement is the whole of an account's history, not a window
          // carried on from where a previous run stopped, so an OFX file
          // rendered from one may replace an existing one. See Fetched.
          incremental = false
        )
      )
    yield result

// One statement, decoded by header name rather than by column position: the
// app's export has gained and reordered columns before, and only nine of them
// are read here, so a change to the rest shouldn't fail a decode. The currency
// comes back with the transactions because it's a fact about the account
// rather than about a row — every row of one statement carries the same one,
// and rows that disagree mean the file isn't one account's statement.
def csvStatement(
    path: fs2.io.file.Path,
    zone: ZoneId
)(using
    verbosity: Verbosity
): IO[
  (currency: Option[monzo.Currency], transactions: List[monzo.Transaction])
] = for
  // Checked rather than left to the read: a mistyped path surfaces as an
  // errno on one platform and a NoSuchFileException on the other, and neither
  // names the option it came from.
  exists <- fs2.io.file.Files[IO].exists(path)
  _ <- IO.raiseUnless(exists):
    Error(s"No CSV statement at $path.")
  rows <- fs2.io.file
    .Files[IO]
    .readUtf8(path)
    .through:
      fs2.data.csv.lowlevel.rows()
    .through:
      fs2.data.csv.lowlevel.headers[IO, String]
    .compile
    .toList
    .adaptError:
      case csvException: CsvException =>
        Error(s"$path isn't readable as CSV: ${csvException.getMessage}")
  decoded <- rows.traverse: row =>
    IO.fromEither:
      csvTransaction(row, zone).leftMap: message =>
        // The parser's own line number where it has one, so the message
        // points at the line an editor shows even though a quoted cell may
        // span several.
        Error(row.line.fold(s"$path: $message"): line =>
          s"$path line $line: $message")
  currencies = decoded.map(_.currency).distinct
  _ <- IO.raiseUnless(currencies.sizeIs <= 1):
    Error(
      s"$path holds more than one currency (${currencies.map(_.value).sorted.mkString(", ")}), so it isn't one account's statement."
    )
  transactions = decoded.map(_.transaction)
  _ <- info:
    s"Read ${transactions.size} transaction(s) from $path."
yield (currency = currencies.headOption, transactions = transactions)

// The nine columns import consumes, named as the app's own export names them.
// Only the members downstream reads are filled: declineReason stays None
// because the app doesn't export declined authorisations at all (and
// materialTransactions still drops £0 active-card checks by their amount), and
// metadata stays None because a statement carries no pot_account_id — which is
// why csvTransactionSource takes pot identity from the command line instead.
//
// Name is the app's single column for what the API splits between merchant and
// counterparty, so it goes in as the merchant and payee's existing precedence
// falls back to Description unchanged.
//
// Category split — one transaction the app divided between several categories
// — is not read: filing the whole amount under the primary category is what
// the API path does with the same transaction, so the two agree, and splitting
// it would need a posting with more than two legs.
def csvTransaction(
    row: CsvRow[String],
    zone: ZoneId
): Either[
  String,
  (currency: monzo.Currency, transaction: monzo.Transaction)
] = for
  id <- requiredCell(row, "Transaction ID")
  date <- requiredCell(row, "Date")
  time <- requiredCell(row, "Time")
  created <- csvCreated(date, time, zone)
  amount <- requiredCell(row, "Amount")
  minorUnits <- csvMinorUnits(amount)
  currency <- requiredCell(row, "Currency")
  description <- cell(row, "Description")
yield (
  currency = monzo.Currency(currency),
  transaction = monzo.Transaction(
    id = monzo.TransactionId(id),
    created = monzo.Created(created),
    amount = monzo.Amount(minorUnits),
    counterparty = monzo.Counterparty(),
    description = monzo.Description(description),
    notes = monzo.Notes(optionalCell(row, "Notes and #tags").getOrElse("")),
    category = optionalCell(row, "Category").map(monzo.Category(_)),
    merchant = optionalCell(row, "Name").map: name =>
      monzo.Merchant(monzo.Name(name))
  )
)

// A cell whose column must be there and whose value must say something: an
// empty ID, date, amount or currency is a row nothing downstream can use.
def requiredCell(row: CsvRow[String], header: String): Either[String, String] =
  cell(row, header).flatMap: value =>
    Either.cond(value.nonEmpty, value, s"$header is empty")

// A missing column is a change to the export's shape rather than a bad row, so
// the message lists what the file does carry: Monzo has renamed these before,
// and the header row is the first thing to check against a fresh export.
def cell(row: CsvRow[String], header: String): Either[String, String] =
  row(header).toRight(
    s"no $header column; this file's columns are ${row.headers.toList.flatMap(_.toList).mkString(", ")}"
  )

// Absent and empty mean the same thing for the three optional columns: a
// transaction with no category, no counterparty name or no note.
def optionalCell(row: CsvRow[String], header: String): Option[String] =
  row(header).filter(_.nonEmpty)

// The app stamps Date and Time in the account's own local time and says
// nowhere which zone that is, so --from-csv-zone names it and it defaults to
// this machine's own. It decides more than tidiness: a transaction either side
// of midnight falls on a different calendar date read in the wrong zone, and
// the calendar date is what a GnuCash post_date is normalised to (see
// neutralPostDate).
//
// One spelling each, because that is what the export writes. A fresh export
// that writes another is a change to the file's shape, and a row it can't read
// says so by name rather than being silently read a second way.
lazy val csvDateFormat: DateTimeFormatter =
  DateTimeFormatter.ofPattern("dd/MM/yyyy")

lazy val csvTimeFormat: DateTimeFormatter =
  DateTimeFormatter.ofPattern("HH:mm:ss")

def csvCreated(
    date: String,
    time: String,
    zone: ZoneId
): Either[String, Timestamp] = for
  localDate <- Either
    .catchOnly[DateTimeParseException](LocalDate.parse(date, csvDateFormat))
    .leftMap(_ => s"Date isn't a dd/MM/yyyy date: $date")
  localTime <- Either
    .catchOnly[DateTimeParseException](LocalTime.parse(time, csvTimeFormat))
    .leftMap(_ => s"Time isn't an HH:mm:ss time: $time")
yield localDate.atTime(localTime).atZone(zone).toInstant.asSmithyTimestamp

// The export writes an amount in major units with a decimal point, where the
// API reports the minor units every consumer downstream expects, so the scale
// has to be put back. The 100 is GBP's, the one currency either sink handles
// — a statement in anything else is refused by the sink rather than here, once
// the book's own currency (or, for the OFX file, the currency majorUnits
// divides by) is known. See gbpMinorUnitsPerMajorUnit.
def csvMinorUnits(amount: String): Either[String, BigInt] =
  Either
    .catchOnly[NumberFormatException](BigDecimal(amount))
    .leftMap(_ => s"Amount isn't a number: $amount")
    .flatMap: major =>
      val minor = major * gbpMinorUnitsPerMajorUnit
      Either.cond(
        minor.isWhole,
        minor.toBigInt,
        s"Amount has more decimal places than GBP has: $amount"
      )
