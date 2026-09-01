package plutus

import cats.effect.*
import porcupine.*
import porcupine.Codec.*

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

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
      // Columns named rather than positional, as in Split.insert and
      // Slot.insert: a GnuCash release that adds one to this table would
      // otherwise shift every value along by a column.
      query = sql"""
        insert into transactions (
          guid,
          currency_guid,
          num,
          post_date,
          enter_date,
          description
        )
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

// GnuCash's SQLite backend stores post_date/enter_date (and reconcile_date) as
// a UTC "yyyy-MM-dd HH:mm:ss" string — confirmed against the book, whose
// unreconciled splits carry reconcile_date '1970-01-01 00:00:00'. (The column
// is declared text(14), but SQLite doesn't enforce the length, so the 19-char
// form is stored as-is.) This is the storage format only: what a run prints to
// the console has its own formatter, so a change here can't silently reshape
// that output.
val gnuCashTimestamp: DateTimeFormatter =
  DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

def formatTimestamp(instant: Instant): String =
  instant.atOffset(ZoneOffset.UTC).format(gnuCashTimestamp)

// GnuCash's "neutral time": every transaction it posts is pinned to 10:59:00
// UTC on its date (NEUTRAL_TIME_STR in libgnucash/engine/gnc-date.h, applied
// by xaccTransSetDatePostedSecsNormalized, which the register and the OFX
// importer both go through). The point is that 10:59Z has enough slack either
// side to render as the same calendar day anywhere from UTC-11 to UTC+12, so a
// book reads the same wherever it's opened. Writing the real clock time
// instead would give rows near midnight no slack at all — a 23:30Z purchase
// would show as the next day east of UTC — and would interleave imported rows
// with hand-entered ones by an artefact rather than by anything meaningful,
// since GnuCash's own rows all sit at 10:59.
val neutralTime: LocalTime = LocalTime.of(10, 59, 0)

// The calendar day is taken in `zone`, matching what GnuCash's own
// normalisation does (it converts to local time and back), so an imported row
// lands on the day the machine's user would say it happened rather than on its
// UTC day. Callers pass the machine's zone; the consequence is that importing
// the same transaction on a machine in another zone could pick the adjacent
// day, which is the same tradeoff GnuCash itself makes for hand-entered rows.
def neutralPostDate(instant: Instant, zone: ZoneId): Instant =
  instant
    .atZone(zone)
    .toLocalDate
    .atTime(neutralTime)
    .toInstant(ZoneOffset.UTC)
