package plutus

// The readings of a monzo.Transaction that both sinks need and neither owns.
// They sit here rather than in either sink because a reading kept in one of
// them would have to be reached for across the matrix — and rather than in a
// source, because no source uses any of them: they are what a sink does with
// what it was handed. See CLAUDE.md.

// Skip £0 active-card checks and declined authorisations: neither is real
// spend, so the OFX sink leaves them out of the file and the book sink leaves
// them out of the book. Shared so both paths filter identically.
def materialTransactions(
    transactions: List[monzo.Transaction]
): List[monzo.Transaction] =
  transactions.filterNot: transaction =>
    transaction.amount.value == 0 || transaction.declineReason.isDefined

// Monzo reports an amount in the minor unit of the account's own currency, and
// OFX wants major units. The OFX sink has no book to read a fraction from — the
// book sink divides by the book currency's own `fraction` — so the 100 here
// is GBP's, which is the only currency either path handles today, named rather
// than inlined so the two at least spell the same idea the same way. The code
// beside it is what ofxTransactionSink refuses anything else by, and what the
// CSV source's own scaling back to minor units rests on.
val gbpMinorUnitsPerMajorUnit = 100

val gbpCurrencyCode = "GBP"

def majorUnits(minorUnits: BigInt): BigDecimal =
  BigDecimal(minorUnits) / gbpMinorUnitsPerMajorUnit

// The human-readable payee, preferring the merchant (card spend), then the
// counterparty (transfers), then Monzo's own description as a last resort. The
// OFX sink uses it for the NAME and the book sink for the GnuCash transaction
// description, so both outputs read identically.
def payee(transaction: monzo.Transaction): String =
  transaction.merchant
    .map(_.name)
    .orElse(transaction.counterparty.name)
    .map(_.value)
    .getOrElse(transaction.description.value)
