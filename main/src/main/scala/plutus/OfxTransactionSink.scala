package plutus

import cats.effect.*
import cats.syntax.all.*
import smithy4s.xml.*

import java.nio.file.FileAlreadyExistsException
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// Writing the OFX inside `use` is what gates the source's bookmarks on it: a
// failed write leaves the window to be fetched again next run.
def ofxTransactionSink(
    source: TransactionSource,
    output: fs2.io.file.Path,
    dryRun: Boolean
)(using verbosity: Verbosity): IO[Unit] = for
  // The run's instant, taken here rather than by the source, so a source with
  // no clock of its own still resolves its window against this run's. See
  // TransactionSource.
  now <- IO.realTimeInstant
  _ <- source.use(now): fetched =>
    // An account with nothing material would render as an empty OFX statement
    // block, so drop it.
    val materialAccountIdsAndTransactions = fetched.byAccount
      .map: (account, transactions) =>
        account.id -> materialTransactions(transactions)
      .filter: (_, transactions) =>
        transactions.nonEmpty
    // Fail fast on an account a source says is denominated in anything but
    // the currency majorUnits divides by: an OFX file has no book behind it
    // to read a fraction from, so its amounts would be off by whatever that
    // currency's minor unit is, and silently — the file states no currency.
    // The book sink makes the same check against the book's own commodity.
    val foreignAccounts = fetched.byAccount
      .flatMap: (account, _) =>
        account.currency
          .filterNot(_.value == gbpCurrencyCode)
          .map: currency =>
            s"${account.id.value} (${currency.value})"
      .distinct
    IO.raiseUnless(foreignAccounts.isEmpty)(
      Error(
        s"Account(s) not denominated in $gbpCurrencyCode: ${foreignAccounts.sorted.mkString(", ")}."
      )
    ) *> (IO.whenA(dryRun):
      materialAccountIdsAndTransactions.traverse_ : (accountId, transactions) =>
        info:
          s"Would write ${transactions.size} transaction(s) for ${accountId.value}.") *> writeOfx(
      toOfx:
        materialAccountIdsAndTransactions
      ,
      output,
      // A --since run names the whole of the window it renders, so writing it
      // over an existing file is what was asked for; a bookmark run would
      // replace a full export with whatever has happened since it. The source
      // says which it fetched, because the transactions it hands over don't
      // say what window asked for them.
      overwrite = !fetched.incremental,
      dryRun
    ).adaptError:
      case _: FileAlreadyExistsException =>
        Error:
          s"Cannot overwrite existing output in from-last-transactions mode. Delete $output or specify --since."
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

// A dry run reports rather than writes, and checks the one thing that would
// have stopped the write, so what it prints is a plan that would have
// succeeded — the same bargain import's dry run strikes with the book.
def writeOfx(
    content: ofx.Ofx,
    output: fs2.io.file.Path,
    overwrite: Boolean,
    dryRun: Boolean
)(using verbosity: Verbosity): IO[Unit] =
  if dryRun then
    for
      exists <- fs2.io.file.Files[IO].exists(output)
      _ <- IO.raiseWhen(exists && !overwrite):
        FileAlreadyExistsException(output.toString)
      _ <- info:
        s"Would write OFX to $output."
    yield ()
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
          .writeUtf8(
            output,
            if overwrite then fs2.io.file.Flags.Write
            else
              fs2.io.file.Flags(
                fs2.io.file.Flag.Write,
                fs2.io.file.Flag.CreateNew
              )
          )
      .compile
      .drain *>
      info:
        s"Wrote OFX to $output."
