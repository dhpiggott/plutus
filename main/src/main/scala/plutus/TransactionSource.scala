package plutus

import cats.effect.*

import java.time.Instant

// The one shape every transaction source produces and every sink consumes: a
// window's transactions grouped by the account they belong to, and the pot
// behind each pot backing account — name, currency, deleted. Every account
// appears in byAccount, keeping an empty transaction list when nothing was
// fetched for it, because pot naming needs every owner present.
//
// `incremental` says the window carried on from wherever the last run left off
// rather than being one the caller named. It's here because it's the one thing
// about a fetch a sink can't work out for itself and can't ignore: --since
// belongs to the Monzo source and means nothing to a source reading a file,
// yet whether the OFX file is a complete statement or a window's worth of new
// rows decides whether writing it may replace one already there. See
// exportTransactions.
type Fetched = (
    byAccount: List[(FetchedAccount, List[monzo.Transaction])],
    pots: Map[monzo.AccountId, monzo.Pot],
    incremental: Boolean
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

// Scoped rather than a plain IO[Fetched] because a source can carry a
// write-back that has to follow the consumer rather than precede it: the Monzo
// source advances its bookmarks only once the OFX has been written, so a run
// that fails at the sink leaves its window to be fetched again. A consumer
// with nothing to gate that way passes IO.pure and does its own work
// afterwards, outside whatever the source held open.
//
// `now` is the run's instant, taken by the command at invocation and handed
// down rather than read here, so the window a source resolves and the rows a
// sink stamps carry the one instant — the same thing archive-accounts and
// restore-account do with their own. A source with no use for it ignores it.
trait TransactionSource:
  def use[A](now: Instant)(consume: Fetched => IO[A])(using Verbosity): IO[A]
