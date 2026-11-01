package plutus

import cats.effect.*
import porcupine.*
import porcupine.Codec.*

import java.time.Instant

/** sqlite> .schema splits CREATE TABLE splits( guid text(32) PRIMARY KEY NOT
  * NULL, tx_guid text(32) NOT NULL, account_guid text(32) NOT NULL, memo
  * text(2048) NOT NULL, action text(2048) NOT NULL, reconcile_state text(1) NOT
  * NULL, reconcile_date text(14) NOT NULL, value_num integer NOT NULL,
  * value_denom integer NOT NULL, quantity_num integer NOT NULL, quantity_denom
  * integer NOT NULL, lot_guid text(32) );
  *
  * memo/action/reconcile_state/reconcile_date are all NOT NULL — empty string
  * or sentinel, never NULL. `value` is in the transaction's currency;
  * `quantity` is in the account's own commodity. For an all-GBP book the two
  * are equal; they only diverge across currencies.
  */
final case class Split(
    guid: String,
    txGuid: String,
    accountGuid: String,
    memo: String,
    valueNum: Long,
    valueDenom: Long,
    quantityNum: Long,
    quantityDenom: Long
):
  def insert(using db: Database[IO]): IO[Unit] =
    db.execute(
      query = sql"""
        insert into splits (
          guid,
          tx_guid,
          account_guid,
          memo,
          action,
          reconcile_state,
          reconcile_date,
          value_num,
          value_denom,
          quantity_num,
          quantity_denom,
          lot_guid
        )
        values (
          $text, $text, $text, $text,
          '',
          $text,
          $text,
          $integer, $integer, $integer, $integer,
          null
        )
      """.command,
      args = (
        guid,
        txGuid,
        accountGuid,
        memo,
        Split.NotReconciled,
        Split.ReconcileEpoch,
        valueNum,
        valueDenom,
        quantityNum,
        quantityDenom
      )
    )

object Split:
  // Imported splits are written unreconciled, the state GnuCash gives any fresh
  // entry; you reconcile them against a statement yourself, exactly as with an
  // OFX import. GnuCash stores the epoch (not an empty string, which the NOT
  // NULL column forbids) as the reconcile_date of an 'n' split, in the same
  // "yyyy-MM-dd HH:mm:ss" form it uses for post_date/enter_date. See
  // Transaction.gnuCashTimestamp.
  val NotReconciled: String = "n"
  val ReconcileEpoch: String = "1970-01-01 00:00:00"

  def countForAccount(accountGuid: String)(using
      db: Database[IO]
  ): IO[Long] =
    db.unique(
      query = sql"""
        select count(*) from splits
        where account_guid = $text
      """.query(integer),
      args = accountGuid
    )

  // Every account's trimmed online_id tag, with the latest post_date among the
  // account's imported splits: an account-level online_id names the account a
  // statement belongs to, and a split-level one marks a row imported from it
  // (see Slot.OnlineId), so the pair says how far that statement has been
  // filed. A tagged account with no imported split comes back with None. One
  // scan of slots for the tags, then index lookups — GnuCash indexes slots by
  // obj_guid and splits by account and transaction — rather than a pass over
  // every split. post_date's "yyyy-MM-dd HH:mm:ss" sorts as it reads, so max()
  // is the latest; it comes back as epoch seconds rather than as GnuCash's
  // string, so nothing here parses one.
  def latestImportedPostDates(using
      db: Database[IO]
  ): IO[List[(String, Option[Instant])]] =
    db.execute(
      query = sql"""
        select
          trim(account_tags.string_val),
          cast(strftime('%s', max(transactions.post_date)) as integer)
        from slots as account_tags
        join accounts on accounts.guid = account_tags.obj_guid
        left join splits on splits.account_guid = accounts.guid
        left join slots as split_ids
          on split_ids.obj_guid = splits.guid
          and split_ids.name = $text
        left join transactions
          on transactions.guid = splits.tx_guid
          and split_ids.obj_guid is not null
        where account_tags.name = $text
          and account_tags.string_val is not null
        group by trim(account_tags.string_val)
      """.query(text *: integer.opt *: nil),
      args = (Slot.OnlineId, Slot.OnlineId)
    ).map:
      _.map: (tag, postDate) =>
        tag -> postDate.map(Instant.ofEpochSecond)
