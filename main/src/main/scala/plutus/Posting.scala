package plutus

import cats.effect.*
import porcupine.*

import java.time.Instant

// A Posting is the balanced unit Plutus actually writes: one transaction and
// its two splits, constructed so the double-entry invariant (values sum to 0)
// cannot be violated — the category leg is *defined* as the negation of the
// asset leg, exactly as GnuCash requires. Both splits are written unreconciled
// (Split.NotReconciled), the state GnuCash gives freshly imported entries; you
// reconcile them yourself against a statement, as you would an OFX import.
final case class Posting(
    transaction: Transaction,
    assetSplit: Split,
    categorySplit: Split,
    onlineId: String // the Monzo transaction ID; our dedup key
):
  def insert(using db: Database[IO]): IO[Unit] =
    for
      _ <- transaction.insert
      _ <- assetSplit.insert
      _ <- categorySplit.insert
      // Mirror export's OFX FITID convention (the Monzo ID) so re-runs are
      // idempotent and GnuCash's own importer treats these as already-seen.
      // GnuCash's generic importer stores this dedup id as an online_id slot on
      // the split belonging to the imported account (gnc_import_set_split_online_id),
      // so it hangs off the asset split. (Account-level online_id slots — the
      // OFX account association — are a different thing whose obj_guid is an
      // account guid, not a split's.)
      _ <- Slot
        .stringSlot(
          objGuid = assetSplit.guid,
          name = "online_id",
          value = onlineId
        )
        .insert
    yield ()

object Posting:

  def fromMonzo(
      monzoTransaction: monzo.Transaction,
      assetAccount: Account,
      categoryAccount: Account,
      currency: Commodity,
      enterDate: Instant
  ): IO[Posting] =
    for
      transactionGuid <- newGuid
      assetSplitGuid <- newGuid
      categorySplitGuid <- newGuid
    yield
      // Monzo amounts are signed integers in the currency's minor unit (pence
      // for a GBP account) — negative for money out — which is why export feeds
      // amount.value straight into OFX. We do the same: the asset leg moves by
      // the signed amount, the category leg by its negation, so the two sum to
      // zero and the transaction balances.
      val minorUnits = monzoTransaction.amount.value.bigInteger.longValueExact
      // The human-readable payee for the GnuCash transaction description,
      // preferring the merchant (card spend), then the counterparty (transfers),
      // then Monzo's own description as a last resort — the same precedence
      // export uses for the OFX NAME field, so both outputs read identically.
      val description = monzoTransaction.merchant
        .map(_.name)
        .orElse(monzoTransaction.counterparty.name)
        .map(_.value)
        .getOrElse(monzoTransaction.description.value)
      val transaction = Transaction(
        guid = transactionGuid,
        currencyGuid = currency.guid,
        // GnuCash's transaction "num" (cheque/reference number) has no Monzo
        // equivalent, so it's left blank, matching a hand-entered transaction.
        num = "",
        postDate = monzoTransaction.created.value.asInstant,
        enterDate = enterDate,
        description = Some(description)
      )
      def split(guid: String, account: Account, value: Long) = Split(
        guid = guid,
        txGuid = transactionGuid,
        accountGuid = account.guid,
        // Monzo's free-text notes become the split memo — the per-split note
        // GnuCash shows next to the line, distinct from the transaction-wide
        // description above.
        memo = monzoTransaction.notes.value,
        valueNum = value,
        valueDenom = currency.fraction,
        // value is in the book's currency; quantity is in the account's own
        // commodity. They're equal in a single-currency book, but the quantity
        // denominator is sourced from the account (commodity_scu) rather than
        // assumed, so a differently-scaled account still gets a valid split.
        quantityNum = value,
        quantityDenom = account.commodityScu
      )
      Posting(
        transaction = transaction,
        assetSplit = split(assetSplitGuid, assetAccount, minorUnits),
        categorySplit = split(categorySplitGuid, categoryAccount, -minorUnits),
        onlineId = monzoTransaction.id.value
      )
