package plutus

// Maps a Monzo account to the GnuCash asset account its transactions post
// into, keyed on the account's type (uk_retail, uk_retail_joint, …) so the
// mapping is deterministic — no per-account CLI wiring. Pot backing accounts
// never appear in /accounts and so carry no type (they're discovered from
// transaction metadata); they all post into `pots`, the one asset account for
// pot money, because the API exposes neither a type nor a name to tell pots
// apart by. An account whose type isn't in the map is skipped with a warning
// rather than guessed at.
final case class AssetAccounts(
    byAccountType: Map[String, List[String]],
    pots: List[String]
):
  def pathFor(account: monzo.Account): Option[List[String]] =
    account.accountType match
      case Some(accountType) => byAccountType.get(accountType.value)
      case None              => Some(pots)

object AssetAccounts:

  // A starting point — edit freely, as with ImportRules.default. Every target
  // account must already exist; the importer resolves the paths it's about to
  // use before anything is written.
  val default: AssetAccounts =
    AssetAccounts(
      byAccountType = Map(
        "uk_retail" -> List("Assets", "Current Assets", "Monzo", "Current"),
        "uk_retail_joint" -> List("Assets", "Current Assets", "Monzo", "Joint")
      ),
      pots = List("Assets", "Current Assets", "Monzo", "Pots")
    )
