# Simplify follow-ups

Items surfaced by `/simplify` over the whole codebase that were not addressed
in the "Simplify main module" commit that introduces this file.

## Likely worth doing

1. **`parTraverse` over Monzo accounts** in `ExportTransactions.scala:463` —
   overlap per-account pagination latency. Risk: confirm Monzo rate-limiting
   behaviour.
2. **N+1 SQL in `Account.allChildren` / `Account.path`** — replace the
   recursive single-row queries with a `WITH RECURSIVE` CTE, or one eager load
   of the `accounts` table into `Map[guid, Account]` walked in memory. Called
   from tight loops in `ArchiveAccounts.scala` and `RestoreAccount.scala`.
3. **Unify mirror-twin recursive methods** `createOrRetrieveArchiveParent`
   and `createOrRetrieveNonArchiveParent` in `Account.scala:155-216` — they
   differ only in the boundary pair; collapse to one helper taking
   `(from, to)`.
4. **Dedup archive/restore mirror-cleanup** between
   `ArchiveAccounts.scala:46-78` and `RestoreAccount.scala:51-82` — same
   "reparent existing children, delete redundant mirror, log" structure with
   different wording.
5. **Migrate `IO.print` prompts to cue4s** — three TODO-marked
   `IO.print`/`IO.readLine` prompts in `ExportTransactions.scala`. cue4s is
   already used in `RestoreAccount`.

## Probably leave alone

6. Unify the two `Since` enums (`ExportTransactionsSince` /
   `ListTransactionsSince`) in `ExportTransactions.scala`.
7. Hard-coded `http://localhost:8080/oauth/callback` redirect URI in
   `ExportTransactions.scala:345` (port `8080` also implicit in
   `EmberServerBuilder.default`).
8. `BearerAuthMiddleware` takes `String` instead of `monzo.AccessToken` — the
   newtype is unwrapped at the call site.
9. `Log.log`'s unused `S: Show[A]` parameter — `IO.println(a)` already does
   the right thing via its own default.
10. JVM noop StateStore `val _ = verbosity` discard — harmless suppression
    matching the cross-platform `using` interface.
11. TOCTOU on `Files[IO].exists(output)` precheck at
    `ExportTransactions.scala:88` — cosmetic given the network-bound
    runtime.
12. Mutating `var continue` in `porcupine-native/dbplatform.scala` —
    vendored upstream Porcupine; refactoring drifts the fork further from
    upstream.
