package plutus

// Where Monzo money lands in GnuCash. Main accounts map deterministically by
// their type (uk_retail, uk_retail_joint, …) via byAccountType — no per-account
// CLI wiring — and those accounts must already exist; an account whose type
// isn't in the map is skipped with a warning rather than guessed at. Pot
// backing accounts never appear in /accounts and so carry no type (they're
// discovered from transaction metadata); each posts into a child of `pots`
// named after its pot (names come from /pots, linked via State.potIds, and the
// child is created on first sight), falling back to `pots` itself when no
// recorded link names the backing account.
final case class AssetAccounts(
    byAccountType: Map[String, List[String]],
    pots: List[String]
)

object AssetAccounts:

  // A starting point — edit freely. The mapped accounts (and `pots`) must
  // already exist; the importer resolves the paths it's about to use before
  // anything is written.
  val default: AssetAccounts =
    AssetAccounts(
      byAccountType = Map(
        "uk_retail" -> List("Assets", "Current Assets", "Monzo", "Current"),
        "uk_retail_joint" -> List("Assets", "Current Assets", "Monzo", "Joint")
      ),
      pots = List("Assets", "Current Assets", "Monzo", "Pots")
    )
