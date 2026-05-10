# Simplify follow-ups

Items surfaced by `/simplify` over the whole codebase that were not addressed
in the "Simplify main module" commit that introduces this file.

## Likely worth doing

1. **Dedup archive/restore mirror-cleanup** between
   `ArchiveAccounts.scala:46-78` and `RestoreAccount.scala:51-82` — same
   "reparent existing children, delete redundant mirror, log" structure with
   different wording.
2. **Migrate `IO.print` prompts to cue4s** — three TODO-marked
   `IO.print`/`IO.readLine` prompts in `ExportTransactions.scala`. cue4s is
   already used in `RestoreAccount`.

## Probably leave alone

3. Unify the two `Since` enums (`ExportTransactionsSince` /
   `ListTransactionsSince`) in `ExportTransactions.scala`.
4. Hard-coded `http://localhost:8080/oauth/callback` redirect URI in
   `ExportTransactions.scala:345` (port `8080` also implicit in
   `EmberServerBuilder.default`).
5. `BearerAuthMiddleware` takes `String` instead of `monzo.AccessToken` — the
   newtype is unwrapped at the call site.
6. `Log.log`'s unused `S: Show[A]` parameter — `IO.println(a)` already does
   the right thing via its own default.
7. JVM noop StateStore `val _ = verbosity` discard — harmless suppression
   matching the cross-platform `using` interface.
8. TOCTOU on `Files[IO].exists(output)` precheck at
   `ExportTransactions.scala:88` — cosmetic given the network-bound
   runtime.
9. Mutating `var continue` in `porcupine-native/dbplatform.scala` —
   vendored upstream Porcupine; refactoring drifts the fork further from
   upstream.
