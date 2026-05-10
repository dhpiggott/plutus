# Simplify follow-ups

Items surfaced by `/simplify` over the whole codebase that were not addressed
in the "Simplify main module" commit that introduces this file.

## Probably leave alone

1. Unify the two `Since` enums (`ExportTransactionsSince` /
   `ListTransactionsSince`) in `ExportTransactions.scala`.
2. Hard-coded `http://localhost:8080/oauth/callback` redirect URI in
   `ExportTransactions.scala:345` (port `8080` also implicit in
   `EmberServerBuilder.default`).
3. TOCTOU on `Files[IO].exists(output)` precheck at
   `ExportTransactions.scala:88` — cosmetic given the network-bound
   runtime.
4. Mutating `var continue` in `porcupine-native/dbplatform.scala` —
   vendored upstream Porcupine; refactoring drifts the fork further from
   upstream.
