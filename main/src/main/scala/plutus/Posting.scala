package plutus

import cats.effect.*
import porcupine.*

import java.time.Instant

// A Posting is the balanced unit Plutus actually writes: one transaction and
// its two splits, constructed so the double-entry invariant (values sum to 0)
// cannot be violated — the category leg is *defined* as the negation of the
// asset leg, exactly as GnuCash requires.
final case class Posting(
    transaction: Transaction,
    assetSplit: Split,
    categorySplit: Split,
    onlineId: String // the Monzo transaction id; our dedup key
):
  def insert(using db: Database[IO]): IO[Unit] =
    for
      _ <- transaction.insert
      _ <- assetSplit.insert
      _ <- categorySplit.insert
      // Mirror export's OFX FITID convention (the Monzo id) so re-runs are
      // idempotent and GnuCash's own importer treats these as already-seen.
      // VERIFY obj_guid target (split vs transaction) against a GUI-OFX-imported
      // row before trusting it:
      //   sqlite> select obj_guid from slots where name='online_id' limit 5;
      _ <- Slot
        .stringSlot(
          objGuid = assetSplit.guid,
          name = "online_id",
          value = onlineId
        )
        .insert
    yield ()

object Posting:

  // Monzo amounts are signed minor units (negative = money out), which is why
  // export feeds amount.value straight into OFX. We do the same: the asset leg
  // moves by the signed amount, the category leg by its negation.
  def fromMonzo(
      monzoTransaction: monzo.Transaction,
      assetAccount: Account,
      categoryAccount: Account,
      currency: Commodity,
      enterDate: Instant
  ): IO[Posting] =
    for
      txGuid <- newGuid
      assetGuid <- newGuid
      categoryGuid <- newGuid
    yield
      val pence = monzoTransaction.amount.value.bigInteger.longValueExact
      val description = monzoTransaction.merchant
        .map(_.name)
        .orElse(monzoTransaction.counterparty.name)
        .map(_.value)
        .getOrElse(monzoTransaction.description.value)
      val transaction = Transaction(
        guid = txGuid,
        currencyGuid = currency.guid,
        num = "",
        postDate = monzoTransaction.created.value.asInstant,
        enterDate = enterDate,
        description = Some(description)
      )
      def split(guid: String, account: Account, num: Long) = Split(
        guid = guid,
        txGuid = txGuid,
        accountGuid = account.guid,
        memo = monzoTransaction.notes.value,
        valueNum = num,
        valueDenom = currency.fraction,
        // quantity is in the account's own commodity; equal to value in a
        // single-currency book, but sourced from the account to stay honest.
        quantityNum = num,
        quantityDenom = account.commodityScu
      )
      Posting(
        transaction = transaction,
        assetSplit = split(assetGuid, assetAccount, pence),
        categorySplit = split(categoryGuid, categoryAccount, -pence),
        onlineId = monzoTransaction.id.value
      )
