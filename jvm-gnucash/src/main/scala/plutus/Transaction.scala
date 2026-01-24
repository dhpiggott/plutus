package plutus

import cats.effect.*
import porcupine.*
import porcupine.Codec.*

object Transaction:

  // TODO: Remove?
  def read(using db: Database[IO]): IO[Map[String, Transaction]] =
    db.stream(
      query = sql"""
        select *
        from transactions
      """.query:
        decoder
      ,
      args = (),
      chunkSize = 1024
    ).map: transaction =>
      transaction.guid -> transaction
    .compile
      .to:
        Map

  val decoder: Decoder[Transaction] =
    (text *:
      text *:
      text *:
      text.opt *:
      text.opt *:
      text.opt *:
      nil).pmap[Transaction]

/** sqlite> .schema transactions CREATE TABLE transactions( guid text(32)
  * PRIMARY KEY NOT NULL, currency_guid text(32) NOT NULL, num text(2048) NOT
  * NULL, post_date text(19), enter_date text(19), description text(2048) );
  * CREATE INDEX tx_post_date_index ON transactions(post_date);
  *
  * @param guid
  * @param currencyGuid
  * @param num
  * @param postDate
  * @param enterDate
  * @param description
  */
final case class Transaction(
    guid: String,
    currencyGuid: String,
    num: String,
    postDate: Option[String],
    enterDate: Option[String],
    description: Option[String]
)
