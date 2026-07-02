package plutus

import cats.effect.*
import porcupine.*
import porcupine.Codec.*

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// GnuCash's SQLite backend stores post_date/enter_date (and reconcile_date) as
// a UTC "yyyy-MM-dd HH:mm:ss" string — confirmed against the book, whose
// unreconciled splits carry reconcile_date '1970-01-01 00:00:00'. (The column
// is declared text(14), but SQLite doesn't enforce the length, so the 19-char
// form is stored as-is.)
val gnuCashTimestamp: DateTimeFormatter =
  DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

def formatTimestamp(instant: Instant): String =
  instant.atOffset(ZoneOffset.UTC).format(gnuCashTimestamp)

/** sqlite> .schema transactions CREATE TABLE transactions( guid text(32)
  * PRIMARY KEY NOT NULL, currency_guid text(32) NOT NULL, num text(2048) NOT
  * NULL, post_date text(14), enter_date text(14), description text(2048) );
  *
  * num is NOT NULL — write "" never NULL (mirrors the splits NOT NULL columns).
  */
final case class Transaction(
    guid: String,
    currencyGuid: String,
    num: String,
    postDate: Instant,
    enterDate: Instant,
    description: Option[String]
):
  def insert(using db: Database[IO]): IO[Unit] =
    db.execute(
      query = sql"""
        insert into transactions
        values ($text, $text, $text, $text, $text, ${text.opt})
      """.command,
      args = (
        guid,
        currencyGuid,
        num,
        formatTimestamp(postDate),
        formatTimestamp(enterDate),
        description
      )
    )
