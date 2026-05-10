# Simplify follow-ups

Items surfaced by `/simplify` over the whole codebase that were not addressed
in the "Simplify main module" commit that introduces this file.

## Probably leave alone

1. Mutating `var continue` in `porcupine-native/dbplatform.scala` —
   vendored upstream Porcupine; refactoring drifts the fork further from
   upstream.
