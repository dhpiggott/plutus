package plutus

import cats.effect.*

import java.time.Instant

// The one shape every transaction source produces and every sink consumes: a
// window's transactions grouped by the account they belong to, the pot behind
// each pot backing account — name, currency, deleted — and the run's single
// clock read, so a consumer stamps its rows without taking a second one. Every
// account appears in byAccount, keeping an empty transaction list when nothing
// was fetched for it, because pot naming needs every owner present.
type Fetched = (
    now: Instant,
    byAccount: List[(monzo.Account, List[monzo.Transaction])],
    pots: Map[monzo.AccountId, monzo.Pot]
)

// Scoped rather than a plain IO[Fetched] because a source can carry a
// write-back that has to follow the consumer rather than precede it: the Monzo
// source advances its bookmarks only once the OFX has been written, so a run
// that fails at the sink leaves its window to be fetched again. A consumer
// with nothing to gate that way passes IO.pure and does its own work
// afterwards, outside whatever the source held open.
trait TransactionSource:
  def use[A](consume: Fetched => IO[A])(using Verbosity): IO[A]
