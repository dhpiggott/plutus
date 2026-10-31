package plutus

import cats.effect.*

import java.time.Instant

// The one shape every transaction source produces and every sink consumes: a
// window's transactions grouped by the account they belong to, and the pot
// behind each pot backing account — name, currency, deleted. Every account
// appears in byAccount, keeping an empty transaction list when nothing was
// fetched for it, because pot naming needs every owner present.
type Fetched = (
    byAccount: List[(FetchedAccount, List[monzo.Transaction])],
    pots: Map[monzo.AccountId, monzo.Pot]
)

// The account a source attributes transactions to, stated rather than
// inferred. potBacking is why this isn't just monzo.Account: the Monzo source
// recognises a pot backing account by its missing type (see isPotBacking),
// which is an artefact of /accounts not listing them and answers nothing for a
// source handed a bare acc_… on a command line. A sink that re-derived the
// rule would file such an account down the pot naming path, and a mis-filed
// row is permanent — online_id dedup skips it on every later run.
//
// currency is what the account's amounts are denominated in, where the source
// knows: a CSV statement carries a Currency column, while /accounts and
// /transactions never say, so the Monzo source leaves it None and only the
// pots it fetches (see Pot.currency) answer for themselves. It's optional
// rather than defaulted because "the source didn't say" and "the source said
// GBP" are different things, and only the first may be let through unchecked.
final case class FetchedAccount(
    id: monzo.AccountId,
    accountType: Option[monzo.AccountType],
    closed: Boolean,
    potBacking: Boolean,
    currency: Option[monzo.Currency]
)

// `now` is the run's instant, taken by the command at invocation and handed
// down rather than read here, so the window a source resolves and the rows a
// sink stamps carry the one instant — the same thing archive-accounts and
// restore-account do with their own. A source with no use for it ignores it.
trait TransactionSource:
  def fetch(now: Instant)(using Verbosity): IO[Fetched]

// Where each account a book knows left off: the UTC midnight before the latest
// transaction filed into it, or None when nothing has been. The Monzo source
// reads from these (see accountStarts); a CSV statement is its own window and
// ignores them.
type BookStarts = Map[monzo.AccountId, Option[Instant]]
