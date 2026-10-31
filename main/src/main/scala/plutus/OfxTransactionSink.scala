package plutus

import cats.effect.*
import cats.syntax.all.*
import porcupine.*
import smithy4s.xml.*

import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// An OFX run reconciled against a book, for someone who imports through
// GnuCash's own importer rather than --to-book: each account starts where the
// book left off, as --to-book's does, and what the book already holds is left
// out of the file. The book is only read, so a run whose last file hasn't been
// imported yet just exports those rows again, which the importer's own
// online_id dedup skips.
def ofxTransactionSinkFromBook(
    source: TransactionSourceFor,
    book: fs2.io.file.Path,
    output: fs2.io.file.Path,
    dryRun: Boolean
)(using verbosity: Verbosity): IO[Unit] = for
  (starts, onlineIds) <- peekBook(book): db =>
    given Database[IO] = db
    (startsFromBook, Slot.onlineIds).tupled
  _ <- ofxTransactionSink(
    source(starts),
    output,
    dryRun,
    filed = onlineIds.map((value, _) => value).toSet
  )
yield ()

// `filed` holds the transaction IDs to leave out, because a book already has
// them; a run that names its own window leaves out nothing.
def ofxTransactionSink(
    source: TransactionSource,
    output: fs2.io.file.Path,
    dryRun: Boolean,
    filed: Set[String]
)(using verbosity: Verbosity): IO[Unit] = for
  // The run's instant, taken here rather than by the source, so a source with
  // no clock of its own still resolves its window against this run's. See
  // TransactionSource.
  now <- IO.realTimeInstant
  fetched <- source.fetch(now)
  // An account with nothing material would render as an empty OFX statement
  // block, so drop it.
  materialAccountIdsAndTransactions = fetched.byAccount
    .map: (account, transactions) =>
      account.id -> materialTransactions(transactions).filterNot: transaction =>
        filed(transaction.id.value)
    .filter: (_, transactions) =>
      transactions.nonEmpty
  // Fail fast on an account a source says is denominated in anything but the
  // currency majorUnits divides by: an OFX file has no book behind it to read
  // a fraction from, so its amounts would be off by whatever that currency's
  // minor unit is, and silently — the file states no currency. The book sink
  // makes the same check against the book's own commodity.
  foreignAccounts = fetched.byAccount
    .flatMap: (account, _) =>
      account.currency
        .filterNot(_.value == gbpCurrencyCode)
        .map: currency =>
          s"${account.id.value} (${currency.value})"
    .distinct
  _ <- IO.raiseUnless(foreignAccounts.isEmpty):
    Error:
      s"Account(s) not denominated in $gbpCurrencyCode: ${foreignAccounts.sorted.mkString(", ")}."
  _ <- IO.whenA(dryRun):
    materialAccountIdsAndTransactions.traverse_ : (accountId, transactions) =>
      info:
        s"Would write ${transactions.size} transaction(s) for ${accountId.value}."
  _ <- writeOfx(
    toOfx:
      materialAccountIdsAndTransactions
    ,
    output,
    dryRun
  )
yield ()

val ofxDateTimeFormatter: DateTimeFormatter =
  DateTimeFormatter.ofPattern:
    "yyyyMMddHHmmss.SSS"

def toOfx(
    accountsIdsAndTransactions: List[
      (monzo.AccountId, List[monzo.Transaction])
    ]
): ofx.Ofx =
  ofx.Ofx:
    ofx.BankMessageSetResponse:
      accountsIdsAndTransactions.map: (accountId, transactions) =>
        ofx.StatementTransactionsResponse:
          ofx.StatementResponse(
            ofx.BankAccountFrom:
              ofx.AccountId:
                accountId.value
            ,
            ofx.BankTransactionList:
              transactions.map: transaction =>
                ofx.StatementTransaction(
                  datePosted = ofx.Datetime:
                    transaction.created.value.asInstant
                      .atOffset:
                        ZoneOffset.UTC
                      .format:
                        ofxDateTimeFormatter
                  ,
                  transactionAmount = ofx.TransactionAmount:
                    majorUnits(transaction.amount.value)
                  ,
                  financialInstitutionId = ofx.FinancialInstitutionId:
                    transaction.id.value
                  ,
                  name = ofx.Name(
                    payee(transaction)
                  ),
                  memo = Some:
                    ofx.Memo:
                      transaction.notes.value
                )
          )

// Always over whatever file is already there: a run names the whole of the
// window it renders, or one a book bounds, and a book-bounded file that was
// never imported loses nothing by being replaced, since its rows are still
// missing from the book and so in the next file too.
def writeOfx(
    content: ofx.Ofx,
    output: fs2.io.file.Path,
    dryRun: Boolean
)(using verbosity: Verbosity): IO[Unit] =
  if dryRun then
    info:
      s"Would write OFX to $output."
  else
    ((fs2.Stream:
      "ENCODING:UTF-8\n"
    )
    ++
      XmlDocument.documentEventifier
        .eventify:
          XmlDocument.Encoder
            .fromSchema:
              ofx.Ofx.schema
            .encode:
              content
        // GnuCash imports OFX through libofx, an SGML parser that doesn't
        // recognise the &apos; / &quot; entities smithy4s escapes apostrophes
        // and quotes to; both are valid literally in element text, so undo the
        // escapes.
        .map:
          case fs2.data.xml.XmlEvent.XmlString(s, isCDATA) =>
            fs2.data.xml.XmlEvent.XmlString(
              s.replace("&apos;", "'").replace("&quot;", "\""),
              isCDATA
            )
          case event => event
        .through:
          fs2.data.xml.render.prettyPrint(width = 60, indent = 4)
    )
      .through:
        fs2.io.file
          .Files[IO]
          .writeUtf8(output, fs2.io.file.Flags.Write)
      .compile
      .drain *>
      info:
        s"Wrote OFX to $output."
