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
//
// description is what the book sink describes the account's asset account
// with, where the source knows it: /accounts gives one, a statement doesn't.
// None from a source that can't say is harmless, because only an account with
// a type has its placement enforced, and a CSV account has none.
final case class FetchedAccount(
    id: monzo.AccountId,
    accountType: Option[monzo.AccountType],
    closed: Boolean,
    potBacking: Boolean,
    currency: Option[monzo.Currency],
    description: Option[monzo.Description]
)

// `now` is the run's instant, taken by the command at invocation and handed
// down rather than read here, so the window a source resolves and the rows a
// sink stamps carry the one instant — the same thing archive-accounts and
// restore-account do with their own. A source with no use for it ignores it.
trait TransactionSource:
  def fetch(now: Instant)(using Verbosity): IO[Fetched]

// What a book knows about where to read from, asked one ID at a time rather
// than listed, because a book can't say which of its tags are Monzo's (see
// monzoTransactionSource). `start` is where an account left off: the UTC
// midnight before the latest transaction filed into it, or None when nothing
// has been, whether or not the book has an account for it. Those two are one
// answer because they start alike: a tagged account with nothing filed may
// have taken transactions since it was tagged, and nothing in the book says
// when it was last read, so only its opening is safe to read from (see
// AccountStarts). `potAccount` is the backing account of a pot the book
// already has an account for. The Monzo source reads from these (see
// accountStarts); a CSV statement is its own window and ignores them.
final case class BookStarts(
    start: monzo.AccountId => Option[Instant],
    potAccount: monzo.PotId => Option[monzo.AccountId]
)
