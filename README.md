# Plutus

A small personal-finance CLI that does two unrelated jobs:

- **GnuCash housekeeping** — archive hidden accounts in a local GnuCash SQLite file, and restore them later.
- **Monzo → OFX export** — pull transactions from the Monzo API and write a single `monzo.ofx` file suitable for import into GnuCash (or anything else that reads OFX).

It is built as a single binary using Cats Effect, http4s, decline, smithy4s, and an inlined fork of [Porcupine](https://github.com/armanbilge/porcupine) for SQLite access. It targets both the JVM and Scala Native; the Scala Native build stores Monzo OAuth credentials in the macOS Keychain.

## Commands

```
plutus archive-accounts    [--input PATH] [verbosity]
plutus restore-account     [--input PATH] [verbosity]
plutus export-transactions [--since INSTANT] [--before INSTANT]
                           [--output PATH] [--dry-run] [verbosity]
```

Verbosity flags (mutually exclusive, default `--info`): `--error`, `--warn`, `--info`, `--verbose`, `--trace`.

### `archive-accounts`

Finds hidden accounts in the GnuCash file at `--input` (default `./Accounts.gnucash`) and moves them under an `Archive` sub-root, creating it on first run.

### `restore-account`

Lists archived accounts and prompts (via [cue4s](https://github.com/neandertech/cue4s)) for one to move back to its original parent.

### `export-transactions`

Fetches Monzo transactions and writes them to `--output` (default `./monzo.ofx`).

By default the export starts from each account's last-exported transaction (a per-account bookmark held in the state store) and ends at the current time; pass `--since` / `--before` (ISO-8601 timestamps) to override. `--dry-run` writes the OFX file without advancing the bookmarks.

On first run there is no saved state, so the command will:

1. Prompt on stdin for your Monzo developer `client_id` and `client_secret`.
2. Start a tiny http4s server on `http://localhost:8080/oauth/callback`.
3. Wait for you to complete the OAuth flow in the Monzo app, then SCA, then press enter.
4. Persist the resulting refresh token (and your client credentials) in the state store so subsequent runs are non-interactive — until the refresh token also expires.

You will need to register an OAuth client at <https://developers.monzo.com> with `http://localhost:8080/oauth/callback` as the redirect URI.

## Building and running

The build is sbt with `sbt-projectmatrix`. The two interesting projects are `main3` (JVM) and `mainNative3` (Scala Native).

### JVM

```
sbt 'main3/run archive-accounts --input Accounts.gnucash'
```

The JVM build uses a no-op state store, so `export-transactions` will re-prompt for credentials on every run. Useful for working on the GnuCash subcommands; not what you want day-to-day for Monzo exports.

### Scala Native (macOS only)

```
sbt 'mainNative3/run export-transactions --output monzo.ofx'
```

Prerequisites:

- The macOS SDK (`xcrun --show-sdk-path` must succeed) — the build generates Keychain bindings against it via [sn-bindgen](https://sn-bindgen.indoorvivants.com/).
- Homebrew packages `sqlite` and `s2n` (the latter is pulled in via epollcat for TLS); the build links against `/opt/homebrew/lib`.

To produce a standalone binary instead of running through sbt:

```
sbt mainNative3/nativeLink
```

sbt will print the path to the linked binary at the end of the run.

### Formatting and linting

```
sbt scalafmtCheckAll       # check
sbt scalafmtAll            # apply
sbt scalafixAll             # OrganizeImports
sbt dependencyUpdates       # also fails the regular build if any dep is stale
```

## Project layout

| Module | Platforms | Purpose |
| --- | --- | --- |
| `smithy4s-schemas` | jvm + native | Smithy IDL for the `StateStore` service, the Monzo API, OFX, and the `Verbosity` enum. |
| `log` | jvm + native | Tiny `fansi`-coloured logging façade keyed off an implicit `Verbosity`. |
| `jvm-noop-state-store` | jvm | No-op `StateStore` (placeholder until a Java FFI Keychain backend lands). |
| `native-macos-keychain-state-store` | native | Real `StateStore` backed by the macOS Keychain via sn-bindgen. |
| `porcupine` (+ `-jvm`, `-native`) | cross | Inlined Porcupine fork. JVM impl uses `sqlite-jdbc`; Native impl uses direct `sqlite3` C externs. |
| `main` | jvm + native | The CLI entry point — wires the platform-specific state store and Porcupine impl into `decline`'s `CommandIOApp`. |

## Status

This is a personal tool — interfaces, command names and on-disk state shape may change at any time without migration paths.
