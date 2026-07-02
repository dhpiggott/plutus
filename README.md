# Plutus

A small personal-finance CLI that does three related jobs:

- **GnuCash housekeeping** — archive hidden accounts in a local GnuCash SQLite file, and restore them later.
- **Monzo → OFX export** — pull transactions from the Monzo API and write a single `monzo.ofx` file suitable for import into GnuCash (or anything else that reads OFX).
- **Monzo → GnuCash import** — pull the same transactions and write them straight into a GnuCash SQLite book, filing each by its Monzo category and skipping rows already imported.

It is built as a single binary using Cats Effect, http4s, decline, smithy4s, and an inlined fork of [Porcupine](https://github.com/armanbilge/porcupine) for SQLite access. It targets both the JVM and Scala Native; both builds reach `sqlite3` and the macOS Keychain through the same FFI mechanism per platform — the JVM build via the [Foreign Function & Memory API](https://openjdk.org/jeps/454) (using jextract for bindings), the Scala Native build via [sn-bindgen](https://sn-bindgen.indoorvivants.com/).

## Commands

```
plutus gnucash archive-accounts    [--input PATH] [verbosity]
plutus gnucash restore-account     [--input PATH] [verbosity]
plutus gnucash import-transactions [--input PATH]
                                   --asset-account MONZO_ACCOUNT_ID=PATH …
                                   [--since INSTANT] [--before INSTANT]
                                   [--dry-run] [verbosity]
plutus monzo   export-transactions [--since INSTANT] [--before INSTANT]
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

#### Pot transactions

Interest paid into a pot never appears in the main account's feed — it only exists on the pot's backing account, which `/accounts` doesn't list. The export reaches it via an [undocumented API behaviour](https://community.monzo.com/t/expose-pot-transaction-data-via-public-api-parity-with-main-account-transactions/193089/11): pot-transfer transactions carry the backing account's ID in their metadata (`pot_account_id`), and `/transactions` accepts it like any other account ID.

- On a `--since` run, every pot referenced by a pot transfer in the window is exported in full — transfer legs and interest credits — as its own OFX statement, and gets its own bookmark.
- On bookmark runs, already-bookmarked pots keep syncing like any other account. Pots discovered in the window but not yet bookmarked are skipped with a warning; re-run with `--since` to onboard them.
- **Pot windows are capped at 90 days.** Strong Customer Authentication verification only covers the accounts on the OAuth consent screen, which pot backing accounts never are — so asking for pot transactions older than 90 days fails with `forbidden.verification_required`, even inside the 5-minute full-history window that main accounts get after authorisation. Keep `--since` within the last 90 days whenever pots are involved (a run with an older `--since` fails outright once it reaches the pots), and run exports at least every 90 days so pot bookmarks never fall off the back of the window. For older pot history, download the pot's statement from the app (Pot → Pot documents → Pot statement; QIF imports straight into GnuCash).
- A pot whose transfers all fall outside the windows you export is never discovered, and pot spending via virtual cards isn't returned by the API at all. Being undocumented, the whole mechanism may break without notice.

On first run there is no saved state, so the command will:

1. Prompt on stdin for your Monzo developer `client_id` and `client_secret`.
2. Start a tiny http4s server on `http://localhost:8080/oauth/callback`.
3. Wait for you to complete the OAuth flow in the Monzo app, then SCA, then press enter.
4. Persist the resulting refresh token (and your client credentials) in the state store so subsequent runs are non-interactive — until the refresh token also expires.

You will need to register an OAuth client at <https://developers.monzo.com> with `http://localhost:8080/oauth/callback` as the redirect URI.

#### Refresh-token expiry reminder

Monzo's token endpoint doesn't tell you when a refresh token expires, but the Monzo app's **Settings > Privacy & security > Manage apps** screen states that access lasts 90 days, so Plutus computes the expiry from the grant time it records at authorization plus that 90-day lifetime. From 45 days before expiry, every `export-transactions` run warns that access is approaching expiry and asks you to extend it in the Monzo app under **Manage apps > Refresh permissions**. After you've done so, answer the follow-up prompt — only a `yes` resets the expiry to 90 days from when you confirm, so the reminder keeps nagging until you've actually extended access. (After a refresh the Manage apps screen shows the session valid for 90 days from that moment — it resets the lifetime rather than stacking onto the time remaining — so Plutus anchors the new expiry on when you confirm, not on the old deadline. The 90-day lifetime is fixed, but the app-side extension itself isn't visible over the API, so if you've extended but Plutus still warns, just confirm at the prompt to record it.)

### `import-transactions`

Fetches Monzo transactions the same way `export-transactions` does (same OAuth/refresh flow, same pot discovery, same `--since` / `--before` window) but writes them straight into the GnuCash SQLite book at `--input` (default `./Accounts.gnucash`) instead of producing an OFX file. It lives under `gnucash` rather than `monzo` because it's conceptually a GnuCash import — a future variant could read the CSVs the Monzo app exports instead of the API.

A single fetch spans your current account, savings and pots, so you map **each** Monzo account (or pot) to its own GnuCash asset account with a repeated `--asset-account MONZO_ACCOUNT_ID=Assets:…:Path` option. Transactions from a Monzo account you haven't mapped are skipped with a warning (so run once, read the warnings to learn the ids, then add mappings). Each transaction becomes one balanced GnuCash transaction with two splits: the signed amount on that account's asset account, and its negation on a category account.

The category account is chosen from Monzo's own transaction `category` via the map in `ImportRules.default` (e.g. `groceries → Expenses:Groceries`); a category not in the map — or a transaction with none — lands in `Expenses:Uncategorised`. Accounts are never auto-created, so review what accumulates there and add a mapping. Every target account (asset and category) must already exist — a missing one fails the run before anything is written.

Re-runs are idempotent: the Monzo transaction ID is written into an `online_id` KVP slot on the asset split (the same ID `export-transactions` uses as the OFX `FITID`, and where GnuCash's generic importer stores it), and any transaction already carrying that ID is skipped. GnuCash's own import matcher recognises these rows too.

- Unlike `export-transactions`, this command doesn't advance the state-store bookmarks — dedup is by `online_id`, not by bookmark — so `--since` defaults to each account's bookmark only for choosing the fetch window.
- Both splits are written unreconciled; you reconcile them against a statement yourself, as with an OFX import.
- Before any non-dry run the book is copied to `<input>.bak`. The whole write runs in a single SQLite transaction, so a mid-run failure rolls back to the pre-run state (and the backup is the belt-and-braces restore).
- `--dry-run` prints the plan (filed / to-Uncategorised / already-present counts) without writing to the book and without taking a backup.
- The same 90-day pot-window cap as `export-transactions` applies, for the same SCA reason.

## Building and running

The build is sbt with `sbt-projectmatrix`. The two interesting projects are `main3` (JVM) and `mainNative3` (Scala Native).

### JVM

```
sbt 'main3/run gnucash archive-accounts --input Accounts.gnucash'
```

Prerequisites:

- JDK 22 or later — the Keychain state store uses the [Foreign Function & Memory API](https://openjdk.org/jeps/454), which is final in JDK 22.

### Scala Native (macOS only)

```
sbt 'mainNative3/run monzo export-transactions --output monzo.ofx'
```

Prerequisites:

- The macOS SDK (`xcrun --show-sdk-path` must succeed) — the build generates Keychain bindings against it via [sn-bindgen](https://sn-bindgen.indoorvivants.com/).
- Homebrew package `s2n` (pulled in via epollcat for TLS); the build links against `/opt/homebrew/lib`.
- Homebrew packages `cmake`, `ninja`, and `pkg-config` — needed by [sbt-vcpkg-native](https://github.com/indoorvivants/sbt-vcpkg) to build sqlite3 from source on first run. The static lib is cached under `~/Library/Caches/sbt-vcpkg`.

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
| `keychain-jvm` | jvm | `object Keychain` (`load(account)` / `save(account, bytes)`) backed by the macOS Keychain via Java's Foreign Function & Memory API. |
| `keychain-native` | native | `object Keychain` (same surface) backed by the macOS Keychain via sn-bindgen. |
| `porcupine-jvm` | jvm | `object Sqlite` (`Connection` / `Statement` over sqlite3) via jextract + Java's Foreign Function & Memory API. |
| `porcupine-native` | native | `object Sqlite` (same surface) backed by sqlite3 via sn-bindgen. |
| `porcupine` | cross | Inlined Porcupine fork. Builds the cats-effect `Database` interface on top of whichever `Sqlite` impl is on the classpath. |
| `main` | jvm + native | The CLI entry point. Hosts the smithy IDL (Monzo API, OFX, state-store state), the `Verbosity` enum + `fansi`-coloured `Log` façade, and wires `Keychain` + the Porcupine impl into `decline`'s `CommandIOApp`. |

## Status

This is a personal tool — interfaces, command names and on-disk state shape may change at any time without migration paths.
