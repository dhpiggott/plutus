package plutus

// Where Monzo money lands in GnuCash. Main accounts map deterministically by
// their type (uk_retail, uk_retail_joint, …) via byAccountType — no per-account
// CLI wiring — and are found by their online_id tags (or, untagged, at their
// canonical paths), created on demand, and kept at their code-defined
// placement (see GnuCashCommands.enforcePlacement); an account whose type
// isn't in the map fails the run rather than being guessed at. Pot
// backing accounts never appear in /accounts and so carry no type (they're
// discovered from transaction metadata); each posts into a child of `pots`
// named after its pot (names come from /pots, linked via State.potIds), which
// is created on first sight and tagged with the backing-account ID in a slot —
// the book itself carries the association from then on, so later runs resolve
// by tag rather than name. A backing account the book doesn't know and no
// recorded link names fails the run rather than being mis-filed — mis-filings
// would be permanent, because online_id dedup skips the rows on every re-run.
final case class AssetAccounts(
    byAccountType: Map[String, List[String]],
    pots: List[String]
)

object AssetAccounts:

  // A starting point — edit freely. Only each path's top-level account must
  // already exist (GnuCash creates Assets, Liabilities, Income and Expenses
  // in every new book); everything deeper is created on demand.
  val default: AssetAccounts =
    AssetAccounts(
      byAccountType = Map(
        "uk_retail" -> List("Assets", "Current Assets", "Monzo", "Current"),
        "uk_retail_joint" -> List("Assets", "Current Assets", "Monzo", "Joint"),
        // Flex is borrowing, so its account belongs under Liabilities.
        "uk_monzo_flex" -> List("Liabilities", "Monzo Flex")
      ),
      pots = List("Assets", "Current Assets", "Monzo", "Pots")
    )
