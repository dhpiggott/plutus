# Plutus

A small personal-finance CLI that does three related jobs:

- **GnuCash housekeeping** — archive hidden accounts in a local GnuCash SQLite file, and restore them later.
- **Monzo → OFX export** — pull transactions from the Monzo API and write a single `monzo.ofx` file suitable for import into GnuCash (or anything else that reads OFX).
- **Monzo → GnuCash import** — pull the same transactions and write them straight into a GnuCash SQLite book, filing each by its Monzo category and skipping rows already imported.

It is built as a single binary using Cats Effect, http4s, decline, smithy4s, and an inlined fork of [Porcupine](https://github.com/armanbilge/porcupine) for SQLite access. It targets both the JVM and Scala Native; both builds reach `sqlite3` and the macOS Keychain through the same FFI mechanism per platform — the JVM build via the [Foreign Function & Memory API](https://openjdk.org/jeps/454) (using jextract for bindings), the Scala Native build via [sn-bindgen](https://sn-bindgen.indoorvivants.com/).

## Commands

```
plutus gnucash archive-accounts    [--input PATH] [--dry-run] [verbosity]
plutus gnucash restore-account     [--input PATH] [--dry-run] [verbosity]
plutus gnucash import-transactions [--input PATH]
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

Both of these move — and, when they find a redundant mirror, delete — accounts, so both carry the same safety net as `import-transactions`: a missing `--input` fails before anything is opened, the whole run is one SQLite transaction, a non-dry run that changes something keeps a timestamped `.bak` of the book as it was beforehand, and `--dry-run` prints what would be archived, restored, created, moved or deleted without writing or backing up.

### `export-transactions`

Fetches Monzo transactions and writes them to `--output` (default `./monzo.ofx`).

By default the export starts from each account's last-exported transaction (a per-account bookmark held in the state store) and ends at the current time; pass `--since` / `--before` (ISO-8601 timestamps) to override. `--dry-run` writes the OFX file without advancing the bookmarks.

#### Pot transactions

Interest paid into a pot never appears in the main account's feed — it only exists on the pot's backing account, which `/accounts` doesn't list. The export reaches it via an [undocumented API behaviour](https://community.monzo.com/t/expose-pot-transaction-data-via-public-api-parity-with-main-account-transactions/193089/11): pot-transfer transactions carry the backing account's ID in their metadata (`pot_account_id`), and `/transactions` accepts it like any other account ID.

- On a `--since` run, every pot referenced by a pot transfer in the window is exported in full — transfer legs and interest credits — as its own OFX statement, and gets its own bookmark.
- On bookmark runs, already-bookmarked pots keep syncing like any other account. Pots discovered in the window but not yet bookmarked are skipped with a warning; re-run with `--since` to onboard them. `--since` applies to *every* account, not just the new pot, and each account's bookmark then advances to the last transaction in that window — so a `--since` later than an account's existing bookmark leaves the transactions between the two unexported, and no later run revisits them. Choose a `--since` no later than the earliest bookmark you care about, or fill the gap from the app's own CSV/QIF exports.
- **Pot windows are capped at 90 days.** Strong Customer Authentication verification only covers the accounts on the OAuth consent screen, which pot backing accounts never are — so asking for pot transactions older than 90 days fails with `forbidden.verification_required`, even inside the 5-minute full-history window that main accounts get after authorisation. Keep `--since` within the last 90 days whenever pots are involved (a run with an older `--since` fails outright once it reaches the pots), and run exports at least every 90 days so pot bookmarks never fall off the back of the window. For older pot history, download the pot's statement from the app (Pot → Pot documents → Pot statement; QIF imports straight into GnuCash).
- A pot whose transfers all fall outside the windows you export is never discovered, and pot spending via virtual cards isn't returned by the API at all. Being undocumented, the whole mechanism may break without notice.
- Every run (even `--dry-run`) also records the backing-account↔pot links it sees in the state store — a fact about Monzo's account topology rather than export progress — which `import-transactions` uses to name each pot's asset account.

On first run there is no saved state, so the command will:

1. Prompt on stdin for your Monzo developer `client_id` and `client_secret`.
2. Start a tiny http4s server on `http://localhost:8080/oauth/callback`.
3. Wait for you to complete the OAuth flow in the Monzo app, then SCA, then press enter.
4. Persist the resulting refresh token (and your client credentials) in the state store so subsequent runs are non-interactive — until the refresh token also expires.

You will need to register an OAuth client at <https://developers.monzo.com> with `http://localhost:8080/oauth/callback` as the redirect URI.

#### Refresh-token expiry reminder

Monzo's token endpoint doesn't tell you when a refresh token expires, but the Monzo app's **Settings > Security > Manage apps** screen states that access lasts 90 days, so Plutus computes the expiry from the grant time it records at authorization plus that 90-day lifetime. From 45 days before expiry, every run that talks to the Monzo API (`export-transactions` and `import-transactions` alike) warns that access is approaching expiry and asks you to extend it in the Monzo app under **Manage apps > Refresh permissions**. After you've done so, answer the follow-up prompt — only a `yes` resets the expiry to 90 days from when you confirm, so the reminder keeps nagging until you've actually extended access. (After a refresh the Manage apps screen shows the session valid for 90 days from that moment — it resets the lifetime rather than stacking onto the time remaining — so Plutus anchors the new expiry on when you confirm, not on the old deadline. The 90-day lifetime is fixed, but the app-side extension itself isn't visible over the API, so if you've extended but Plutus still warns, just confirm at the prompt to record it.)

### `import-transactions`

Fetches Monzo transactions the same way `export-transactions` does (same OAuth/refresh flow, same pot discovery, same `--since` / `--before` window) but writes them straight into the GnuCash SQLite book at `--input` (default `./Accounts.gnucash`) instead of producing an OFX file. It lives under `gnucash` rather than `monzo` because it's conceptually a GnuCash import — a future variant could read the CSVs the Monzo app exports instead of the API.

A single fetch spans your current account, joint account, Flex and pots, and each Monzo account maps deterministically to a GnuCash asset account by its **type** via `AssetAccounts.default` (e.g. `uk_retail → Assets:Current Assets:Monzo:Current`, `uk_monzo_flex → Liabilities:Monzo Flex`) — no per-account flags. An account type not in the map fails the run before anything is written; add it to the map rather than have its transactions silently skipped. Every asset account's leaf name also carries the Monzo account ID that posts into it, with the `acc_` prefix dropped and the rest upper-cased (`…:Monzo:Current (00009237AQC8…)`), because the map is keyed by type and several Monzo accounts can share a type — a closed account and the one that replaced it. That gives each its own account rather than one shared between them, so the book can always say which Monzo account a row came from, and each is archived on its own closure. Like pot accounts, these are found by their `online_id` tags, and the tag is the only thing resolution matches on: an account you matched by hand in a past GUI import of the exported OFX already carries one, written by GnuCash's own importer. An account with no tag is one nothing has ever posted to, so rather than being looked for anywhere else a fresh one is created at its canonical place and tagged, and every run after finds it by tag. That creation is unconditional: if an account is already sitting at that path — hand-made, or left over from before the tags existed — it is left alone and the new account lands beside it under the same name, which GnuCash permits. Adopting it would be a guess, and a wrong one is permanent, since dedup then skips those rows on every re-run; two same-named siblings are visible and fixable instead, so move the untagged one's transactions into the tagged one and delete the empty account. Pot backing accounts carry no type or name over the API, but pot-transfer metadata links each to its pot and [List Pots](https://docs.monzo.com/#list-pots) supplies the pot's name, so each pot posts into its own child of the `Pots` account, named for the pot and its backing account (e.g. `…:Monzo:Pots:Savings (00009237AQC8…)`, so two pots that share a name stay apart), created on first sight and tagged with its backing-account ID in an account-level `online_id` slot — the same association GnuCash's own OFX importer stores, so a pot account you matched by hand in a past GUI import of the exported OFX is recognised too. The book itself thereby carries the association durably: later runs resolve the account by tag, and a book moved to a new machine (fresh state store) keeps working. Every run (export or import) also records the backing-account↔pot links it sees in the state store, so a pot linked once stays nameable even in a window whose only pot activity is interest. A pot the book doesn't know *and* no recorded link names fails the run before anything is written — a mis-filed row would be permanent, since dedup skips it on every re-run; one run whose window spans a transfer for the pot records the missing link. A pot denominated in anything other than the book's currency also fails the run, as does any resolved asset or category account the book holds in another commodity — Monzo's minor units would otherwise be posted as though they were the book's.

Monzo is authoritative for each of these asset accounts' whole placement, enforced on every run: while the Monzo side is live the account sits at its code-defined path, under the pot's current Monzo name (renames propagate); once retired — a closed account, or a deleted pot — it moves to the *same* path nested under `Archive`, and is hidden. Their descriptions are enforced empty, because the name already carries everything the book knows about the account — which Monzo account or pot posts into it, and the ID that keeps two of a kind apart — so a description could only restate it. Enforcement works in both directions, so hand-moves, renames, hides, descriptions and archivals of these accounts last only until the next import. Two safeguards: if a *different* account already occupies a canonical spot the run fails for you to resolve by hand — automatically merging or deleting it could orphan transactions — and if two Monzo accounts ever resolve to one book account (which only happens if the book itself says so, via an `online_id` tag added by hand to an account another Monzo account already answers to, since resolution never adopts an account by location) the run fails before filing anything, rather than commingling their transactions permanently. Each transaction becomes one balanced GnuCash transaction with two splits: the signed amount on that account's asset account, and its negation on a category account.

Monzo's own transaction `category` is authoritative for filing, with a small kind map because not every category is spending: `income` files under `Income:General`; `transfers` and `savings` post to a single wash account, `Assets:Transfers`, where the two legs of a pot transfer net to zero and what remains is money moved to institutions the book imports nothing from (still an asset, not an expense — re-file those by hand); every other category becomes a child of `Expenses` named by title-casing it (`eating_out → Expenses:Eating Out`). A transaction with no category files under `Expenses:General` (Monzo's default category), and a refund arrives sign-flipped in its spending category, negating the expense. All created accounts inherit their parent's account type and commodity, with intermediate path segments created as placeholders — there is no mapping to maintain. Only the top-level `Assets`, `Expenses`, `Income` and `Liabilities` accounts must already exist, and GnuCash creates them in every new book, so an import can run against a freshly created (GBP) book.

Re-runs are idempotent: the Monzo transaction ID is written into an `online_id` KVP slot on the asset split (the same ID `export-transactions` uses as the OFX `FITID`, and where GnuCash's generic importer stores it), and any transaction already carrying that ID is skipped. GnuCash's own import matcher recognises these rows too.

- Unlike `export-transactions`, this command doesn't advance the state-store bookmarks — dedup is by `online_id`, not by bookmark — so `--since` defaults to each account's bookmark only for choosing the fetch window.
- Both splits are written unreconciled; you reconcile them against a statement yourself, as with an OFX import.
- A missing `--input` fails immediately, before the Monzo fetch: SQLite would otherwise create an empty book at a mistyped path and fail obscurely several queries later.
- Each transaction is posted at GnuCash's own "neutral time", 10:59:00 UTC on the transaction's local calendar date, which is what `xaccTransSetDatePostedSecsNormalized` does to every date GnuCash itself records. That leaves enough slack either side for the row to render as the same day in any timezone, and puts imported rows in the same within-day position as hand-entered ones. The split's `enter_date` keeps the real instant.
- Before any non-dry run the book is copied aside, and once the run has actually written something that copy is kept as `<input>.<yyyyMMddTHHmmssZ>.bak` — the state the book was in just before that run, so restoring it undoes exactly that run and no other. A run that turns out to change nothing (nothing new to file, no account to create, move, rename, un-hide or tag) leaves no backup behind, so a scheduled import that finds nothing doesn't age out the backup that could undo the last one that did. The whole write runs in a single SQLite transaction, so a mid-run failure rolls back to the pre-run state (and the backup, kept in that case too, is the belt-and-braces restore). **Nothing ever deletes these** — an import won't remove a file you might need — so prune them yourself: they're the size of the book, one per run that changed it, and the names sort chronologically.
- Every transaction filed is printed as it goes — post date, signed amount, the asset and category accounts it lands in, and the payee — followed by the run's totals, so what a run did (or would do) can be read row by row rather than trusted as a count.
- `--dry-run` prints that same plan (each transaction as "would file", the already-present count, and the accounts that would be created) without writing to the book and without taking a backup. It runs inside a transaction that is always rolled back, so even a bug that reached a write would leave the book unchanged; a dry run that touches any row fails with an error saying so, rather than passing the plan off as complete.
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
- Homebrew package `llvm@17` — the sn-bindgen binary has `/opt/homebrew/opt/llvm@17/lib/libclang.dylib` baked in as an absolute install name, so a different LLVM version won't do. Without it, codegen aborts (exit 134) before printing a diagnostic.
- Homebrew package `s2n` (pulled in via epollcat for TLS); the build links against `/opt/homebrew/lib`.
- Homebrew packages `cmake`, `ninja`, and `pkg-config` — needed by [sbt-vcpkg-native](https://github.com/indoorvivants/sbt-vcpkg) to build sqlite3 from source on first run. The static lib is cached under `~/Library/Caches/sbt-vcpkg`.

To produce a standalone binary instead of running through sbt:

```
sbt mainNative3/nativeLink
```

sbt will print the path to the linked binary at the end of the run.

### Formatting and linting

```
sbt scalafmtCheckAll        # check sources
sbt scalafmtAll             # apply to sources
sbt scalafmtSbtCheck        # check build.sbt and project/
sbt scalafmtSbt             # apply to build.sbt and project/
sbt scalafixAll             # OrganizeImports (add --check to verify instead of rewrite)
sbt dependencyUpdates       # fails (rather than just reporting) if any dep is stale
```

### Continuous integration

```
.github/scripts/verify.sh   # scalafmtCheckAll, scalafmtSbtCheck, compile, scalafixAll --check
```

That is what CI runs, and — there being no tests — it is the whole check. `.github/scripts/install-build-deps.sh` installs the Homebrew packages listed above; on a machine that already has them it does nothing.

GitHub Actions runs two workflows: `ci.yml` on pushes to `main` and on pull requests, and `claude.yml`, which answers [Claude Code](https://github.com/anthropics/claude-code-action) `@claude` mentions on issues and pull requests. There is deliberately no automatic reviewer — ask for one by commenting `@claude review this` on the pull request. Both run on macOS runners, because the SDK-generated FFI bindings mean neither platform row compiles on Linux. Their shared toolchain setup lives in `.github/actions/setup-build`.

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
