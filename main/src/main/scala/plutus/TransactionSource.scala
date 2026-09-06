package plutus

import cats.effect.*

import java.time.Instant

// The one shape every transaction source produces and every sink consumes: a
// window's transactions grouped by the account they belong to, and the pot
// behind each pot backing account — name, currency, deleted. Every account
// appears in byAccount, keeping an empty transaction list when nothing was
// fetched for it, because pot naming needs every owner present.
type Fetched = (
    byAccount: List[(monzo.Account, List[monzo.Transaction])],
    pots: Map[monzo.AccountId, monzo.Pot]
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
