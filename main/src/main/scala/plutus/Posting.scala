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
      // GnuCash's generic importer stores this dedup ID as an online_id slot on
      // the split belonging to the imported account (gnc_import_set_split_online_id),
      // so it hangs off the asset split. (Account-level online_id slots are a
      // different association — GnuCash's OFX importer remembers which account
      // a statement's bank account maps to, and Plutus tags pot accounts the
      // same way (Slot.OnlineId) — so not every online_id slot
      // in a book marks an imported split.)
      _ <- Slot
        .stringSlot(
          objGuid = assetSplit.guid,
          name = Slot.OnlineId,
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
      // zero and the transaction balances. The chain: amount is `bigInteger` in
      // the smithy IDL, so smithy4s models it as a newtype over scala.BigInt —
      // .value unwraps the newtype, .bigInteger is the underlying
      // java.math.BigInteger, and .longValueExact narrows to the Long the
      // splits table stores, throwing rather than wrapping if it wouldn't fit
      // (scala.BigInt has no exact narrowing of its own).
      val minorUnits = monzoTransaction.amount.value.bigInteger.longValueExact
      val transaction = Transaction(
        guid = transactionGuid,
        currencyGuid = currency.guid,
        // GnuCash's transaction "num" (cheque/reference number) has no Monzo
        // equivalent, so it's left blank, matching a hand-entered transaction.
        num = "",
        postDate = monzoTransaction.created.value.asInstant,
        enterDate = enterDate,
        description = Some(payee(monzoTransaction))
      )
      def split(
          guid: String,
          account: Account,
          value: Long,
          memo: Option[String]
      ) =
        Split(
          guid = guid,
          txGuid = transactionGuid,
          accountGuid = account.guid,
          // splits.memo is NOT NULL — GnuCash writes a memo-less split as the
          // empty string, never NULL.
          memo = memo.getOrElse(""),
          valueNum = value,
          valueDenom = currency.fraction,
          // value is what the split is worth in the transaction's currency;
          // quantity is how much of the account's own commodity moved. The
          // two only diverge across commodities, and the importer posts into
          // book-currency accounts only — it fails the run otherwise, since
          // it has no exchange rate to convert with — so quantity is the same
          // rational number as value, written the same way. Not
          // account.commodityScu as the denominator: an account whose
          // Smallest Commodity Unit differs from the currency's fraction
          // (GnuCash allows it — accounts.non_std_scu) would then be handed a
          // numerator scaled to the currency and a denominator scaled to the
          // account, i.e. a quantity wrong by the ratio between them.
          quantityNum = value,
          quantityDenom = currency.fraction
        )
      Posting(
        transaction = transaction,
        // Monzo's free-text notes become the asset split's memo — the
        // per-split note GnuCash shows next to the line, distinct from the
        // transaction-wide description above. Only the asset leg carries them:
        // the note belongs to the money movement, not the category's
        // balancing entry.
        assetSplit = split(
          assetSplitGuid,
          assetAccount,
          minorUnits,
          memo = Some(monzoTransaction.notes.value)
        ),
        categorySplit = split(
          categorySplitGuid,
          categoryAccount,
          -minorUnits,
          memo = None
        ),
        onlineId = monzoTransaction.id.value
      )
