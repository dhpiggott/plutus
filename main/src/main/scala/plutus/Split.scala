package plutus

import cats.effect.*
import porcupine.*
import porcupine.Codec.*

object Split:

  // TODO: Remove?
  def read(using db: Database[IO]): IO[Map[String, Split]] =
    db.stream(
      query = sql"""
        select *
        from splits
      """.query:
        decoder
      ,
      args = (),
      chunkSize = 1024
    ).map: split =>
      split.guid -> split
    .compile
      .to:
        Map

  val decoder: Decoder[Split] =
    (text *:
      text *:
      text *:
      text *:
      text *:
      text *:
      text.opt *:
      integer *:
      integer *:
      integer *:
      integer *:
      text.opt *:
      nil).pmap[Split]

/** sqlite> .schema splits CREATE TABLE splits( guid text(32) PRIMARY KEY NOT
  * NULL, tx_guid text(32) NOT NULL, account_guid text(32) NOT NULL, memo
  * text(2048) NOT NULL, action text(2048) NOT NULL, reconcile_state text(1) NOT
  * NULL, reconcile_date text(19), value_num bigint NOT NULL, value_denom bigint
  * NOT NULL, quantity_num bigint NOT NULL, quantity_denom bigint NOT NULL,
  * lot_guid text(32) ); CREATE INDEX splits_tx_guid_index ON splits(tx_guid);
  * CREATE INDEX splits_account_guid_index ON splits(account_guid);
  *
  * @param guid
  * @param txGuid
  * @param accountGuid
  * @param memo
  * @param action
  * @param reconcileState
  * @param reconcileDate
  * @param valueNum
  * @param valueDenom
  * @param quantityNum
  * @param quantityDenom
  * @param lotGuid
  */
final case class Split(
    guid: String,
    txGuid: String,
    accountGuid: String,
    memo: String,
    action: String,
    reconcileState: String,
    reconcileDate: Option[String],
    valueNum: Long,
    valueDenom: Long,
    quantityNum: Long,
    quantityDenom: Long,
    lotGuid: Option[String]
)
