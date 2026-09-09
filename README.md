# Plutus

A small personal-finance CLI that does two related jobs:

- **Move Monzo transactions** — read them from the Monzo API or from the CSV statements the Monzo app exports, and write them either into a GnuCash SQLite book (filing each by its Monzo category and skipping rows already imported) or to a single `monzo.ofx` file suitable for import into GnuCash (or anything else that reads OFX). Source and sink are named separately, so all four combinations are runs.
- **GnuCash housekeeping** — archive hidden accounts in a local GnuCash SQLite file, and restore them later.

It is built as a single binary using Cats Effect, http4s, decline, smithy4s, and an inlined fork of [Porcupine](https://github.com/armanbilge/porcupine) for SQLite access. It targets both the JVM and Scala Native; both builds reach `sqlite3` and the macOS Keychain through the same FFI mechanism per platform — the JVM build via the [Foreign Function & Memory API](https://openjdk.org/jeps/454) (using jextract for bindings), the Scala Native build via [sn-bindgen](https://sn-bindgen.indoorvivants.com/).

## Commands

```
plutus transactions     ( --from-monzo [--since INSTANT] [--before INSTANT]
                        | --from-csv ACCOUNT_ID=PATH... [--from-csv-pot ACCOUNT_ID=PATH]...
                          [--from-csv-zone ZONE] )
                        ( --to-book[=PATH] [--ignore-lock] | --to-ofx[=PATH] )
                        [--dry-run] [verbosity]
plutus archive-accounts [--input PATH] [--ignore-lock] [--dry-run] [verbosity]
plutus restore-account  [--input PATH] [--ignore-lock] [--dry-run] [verbosity]
```

Verbosity flags (mutually exclusive, default `--info`): `--error`, `--warn`, `--info`, `--verbose`, `--trace`.

### `archive-accounts`

Finds hidden accounts in the GnuCash file at `--input` (default `./Accounts.gnucash`) and moves them under an `Archive` sub-root, creating it on first run.

### `restore-account`

Lists archived accounts and prompts (via [cue4s](https://github.com/neandertech/cue4s)) for one to move back to its original parent.

Both of these move — and, when they find a redundant mirror, delete — accounts, so both carry the same safety net as `transactions --to-book`: a missing `--input` fails before anything is opened, a book GnuCash has open is refused, the whole run is one SQLite transaction, a non-dry run that changes something keeps a timestamped `.bak` of the book as it was beforehand, and `--dry-run` prints what would be archived, restored, created, moved or deleted without writing or backing up.

### The GnuCash lock

All three `gnucash` commands honour GnuCash's own `gnclock` table, the cooperative lock its SQL backends use: opening a book read/write, GnuCash records its hostname and PID there, and warns when it finds a row that isn't its own.

A command that finds the book locked names the holder and stops. There is no save to collide with — GnuCash's SQL backends write each change straight through as it is made, which is why a book opened from SQLite has no active Save button — but GnuCash reads the whole book into memory when it opens it and nothing tells it the file changed underneath, so the hazard is the other way round: our writes land in a book whose open copy still describes the old one, and the next edit GnuCash commits to anything we touched writes that object's row back out from its own copy, parent account and all. A real run takes the lock for its own duration and releases it at the end, including when it is interrupted, so a GnuCash started mid-run gets the same warning. A `--dry-run` reads the lock but never takes one: it writes nothing, and a plan computed against a book somebody else has open is worth saying so about.

GnuCash deletes its row on close, so a GnuCash that crashed leaves one behind and every later run is refused. Only you can tell that from a live one, so `--ignore-lock` overrides it. Don't reach for it against a GnuCash that is actually open.

A book with no `gnclock` table at all — one no GnuCash with a SQL backend has ever opened — is not refused: the run warns that it can't tell and proceeds.

### `transactions`

Reads Monzo transactions from the source named by `--from-…` and writes them to the sink named by `--to-…`. Exactly one of each is required, and the two are independent, so there are four runs: Monzo API or CSV statements in, a GnuCash book or an OFX file out. `--help` prints one usage line per combination.

Everything between the two ends is shared: the same £0-and-declined filter, the same payee precedence (merchant, then counterparty, then Monzo's own description), and the same `online_id` transaction ID on the way out — so a book filed from a CSV statement and a book filed from the API dedup against each other, whichever ran first.

`--dry-run` means no durable change at either end: nothing is written to the book or to the OFX file, no backup is taken, and the state store's bookmarks stay where they are.

### Reading from the Monzo API (`--from-monzo`)

By default the fetch starts from each account's last-exported transaction (a per-account bookmark held in the state store) and ends at the current time; pass `--since` / `--before` (ISO-8601 timestamps) to override. The two are options on this source, so they aren't accepted alongside `--from-csv` — a statement is its own window, with nothing to bound.

#### Pot transactions

Interest paid into a pot never appears in the main account's feed — it only exists on the pot's backing account, which `/accounts` doesn't list. The export reaches it via an [undocumented API behaviour](https://community.monzo.com/t/expose-pot-transaction-data-via-public-api-parity-with-main-account-transactions/193089/11): pot-transfer transactions carry the backing account's ID in their metadata (`pot_account_id`), and `/transactions` accepts it like any other account ID.

- On a `--since` run, every pot referenced by a pot transfer in the window is exported in full — transfer legs and interest credits — as its own OFX statement, and gets its own bookmark.
- On bookmark runs, already-bookmarked pots keep syncing like any other account. Pots discovered in the window but not yet bookmarked are skipped with a warning; re-run with `--since` to onboard them. `--since` applies to *every* account, not just the new pot, and each account's bookmark then advances to the last transaction in that window — so a `--since` later than an account's existing bookmark leaves the transactions between the two unexported, and no later run revisits them. Choose a `--since` no later than the earliest bookmark you care about, or fill the gap from the app's own CSV/QIF exports.
- **Pot windows are capped at 90 days.** Strong Customer Authentication verification only covers the accounts on the OAuth consent screen, which pot backing accounts never are — so asking for pot transactions older than 90 days fails with `forbidden.verification_required`, even inside the 5-minute full-history window that main accounts get after authorisation. Keep `--since` within the last 90 days whenever pots are involved (a run with an older `--since` fails outright once it reaches the pots), and run exports at least every 90 days so pot bookmarks never fall off the back of the window. For older pot history, download the pot's statement from the app (Pot → Pot documents → Pot statement; QIF imports straight into GnuCash).
- A pot whose transfers all fall outside the windows you export is never discovered, and pot spending via virtual cards isn't returned by the API at all. Being undocumented, the whole mechanism may break without notice.
- Every run (even `--dry-run`) also records the backing-account↔pot links it sees in the state store — a fact about Monzo's account topology rather than export progress — which `--to-book` uses to name each pot's asset account.

On first run there is no saved state, so the command will:

1. Prompt on stdin for your Monzo developer `client_id` and `client_secret`.
2. Start a tiny http4s server on `http://localhost:8080/oauth/callback`.
3. Wait for you to complete the OAuth flow in the Monzo app, then SCA, then press enter.
4. Persist the resulting refresh token (and your client credentials) in the state store so subsequent runs are non-interactive — until the refresh token also expires.

You will need to register an OAuth client at <https://developers.monzo.com> with `http://localhost:8080/oauth/callback` as the redirect URI.

#### Refresh-token expiry reminder

Monzo's token endpoint doesn't tell you when a refresh token expires, but the Monzo app's **Settings > Security > Manage apps** screen states that access lasts 90 days, so Plutus computes the expiry from the grant time it records at authorization plus that 90-day lifetime. From 45 days before expiry, every run that talks to the Monzo API warns that access is approaching expiry and asks you to extend it in the Monzo app under **Manage apps > Refresh permissions**. After you've done so, answer the follow-up prompt — only a `yes` resets the expiry to 90 days from when you confirm, so the reminder keeps nagging until you've actually extended access. (After a refresh the Manage apps screen shows the session valid for 90 days from that moment — it resets the lifetime rather than stacking onto the time remaining — so Plutus anchors the new expiry on when you confirm, not on the old deadline. The 90-day lifetime is fixed, but the app-side extension itself isn't visible over the API, so if you've extended but Plutus still warns, just confirm at the prompt to record it.)

### Reading from CSV statements (`--from-csv`, `--from-csv-pot`)

Reads the CSV statements the Monzo app exports, one file per account or pot, with no session at all — no OAuth client, no refresh token, no state store. That is the point of it: the API caps a pot's window at 90 days once Strong Customer Authentication has lapsed (see [Pot transactions](#pot-transactions)), so a statement is the only way to reach a pot's older interest, and this is the way to backfill it without losing the categorisation and dedup a QIF import gives up.

Export them in the app: an account statement from **Settings → Statements** (choose CSV), and a pot statement from **Pot → Pot documents → Pot statement**.

A statement has no account-ID column, so each file is paired on the command line with the account it belongs to, and the two options say which kind it is:

```
plutus transactions \
  --from-csv acc_00009237aqc8c5u8u7fake=CurrentAccount.csv \
  --from-csv-pot acc_00009abcpotbackingfake=Savings.csv \
  --to-book \
  --dry-run
```

Either option is a run on its own — a pot backfill needs no main account's statement — and both are repeatable, once per account. `--from-csv-pot` takes the pot's *backing* account ID, the same `acc_…` the API's pot-transfer metadata carries and the one the book's pot account is tagged with; the account tree shows it in the account's own name.

- **The IDs must already be in the book.** Resolution is by the `online_id` tags a previous run wrote, and nothing else: a statement doesn't say what type of account it came from, so there's no path to create one at, and a guess would be permanent — dedup skips a mis-filed row on every later run. An ID no account is tagged with fails the run, naming it; run once with `--from-monzo` to create and tag the accounts (the pot's name comes from the API too), then every CSV run afterwards finds them. That makes this a backfill tool for a book already being imported into, which is what motivates it.
- **Dedup interoperates in both directions.** The statement's `Transaction ID` column carries the same `tx_…` IDs the API returns, and dedup is by the `online_id` slot rather than by any bookmark, so importing overlapping windows from the API and from a statement — in either order — is idempotent.
- **Nothing is inferred about closure.** A statement says nothing about whether the account or pot behind it still exists, and the absence of a file says nothing either, so no account is archived on this path; that is left to a `--from-monzo` run.
- **Timestamps are local.** The export's `Date` and `Time` columns are in the account's own local time, and the file doesn't say which zone that is, so `--from-csv-zone` names it. It decides more than tidiness: a transaction either side of midnight falls on a different calendar date read in the wrong zone, and the calendar date is what a GnuCash post date is normalised to. Left unspecified it is this machine's own zone — the same one post dates are normalised against, so a run agrees with itself. A named region (`Europe/London`) needs a time zone database: the JVM build has one, the Scala Native build does not and refuses the name, so on that build pass a fixed offset (`+01:00`, `+00:00`) or run a statement spanning a daylight-saving change through the JVM build.
- **Nine columns are read**, by name rather than by position, so a column Monzo adds or moves doesn't break the decode: `Transaction ID`, `Date`, `Time`, `Amount`, `Currency`, `Category`, `Notes and #tags`, `Name` and `Description`. `Name` is the app's single column for what the API splits between merchant and counterparty, so it feeds the same payee precedence. A missing column fails the run and lists the columns the file does carry — Monzo has renamed these before.
- **`Category split` is not read.** A transaction the app divided between several categories files in full under its primary `Category`, which is what the API path does with the same transaction, so the two agree.
- Declined authorisations aren't exported by the app at all, and £0 active-card checks are dropped by amount, as on the API path.
- Amounts are decimal major units in the file and are scaled back to the minor units everything downstream expects. The scale is GBP's; a statement in any other currency fails at the sink, against the book's own commodity or against the OFX file's assumed one.

### Writing an OFX file (`--to-ofx`)

Writes an OFX file at the path given as `--to-ofx=PATH`, or `./monzo.ofx` when `--to-ofx` is given bare. (The path is bound with `=` only: `--to-ofx PATH` is a usage error rather than a path quietly ignored.)

A source that names its own window — a `--since` run, or any CSV statement — renders the whole of it, so writing it over an existing file is what was asked for; a bookmark run holds only what has happened since the last one, so it refuses to overwrite one rather than stand in for the full export already there.

`--dry-run` prints what would be exported — a line per account with its transaction count, and the file that would be written — while writing nothing and leaving the bookmarks where they are. It makes the same refusal a real run would, so a plan that prints is a run that would have succeeded.

### Writing into a GnuCash book (`--to-book`)

Writes straight into the GnuCash SQLite book at the path given as `--to-book=PATH`, or `./Accounts.gnucash` when `--to-book` is given bare, instead of producing a file for GnuCash's own importer to read. (As with `--to-ofx`, the path is bound with `=` only.)

A single fetch spans your current account, joint account, Flex and pots, and each Monzo account maps deterministically to a GnuCash asset account by its **type** via `AssetAccounts.default` (e.g. `uk_retail → Assets:Current Assets:Monzo:Current`, `uk_monzo_flex → Liabilities:Monzo Flex`) — no per-account flags. An account type not in the map fails the run before anything is written; add it to the map rather than have its transactions silently skipped. Every asset account's leaf name also carries the Monzo account ID that posts into it, with the `acc_` prefix dropped and the rest upper-cased (`…:Monzo:Current (00009237AQC8…)`), because the map is keyed by type and several Monzo accounts can share a type — a closed account and the one that replaced it. That gives each its own account rather than one shared between them, so the book can always say which Monzo account a row came from, and each is archived on its own closure. Like pot accounts, these are found by their `online_id` tags, and the tag is the only thing resolution matches on: an account you matched by hand in a past GUI import of the exported OFX already carries one, written by GnuCash's own importer. An account with no tag is one nothing has ever posted to, so rather than being looked for anywhere else a fresh one is created at its canonical place and tagged, and every run after finds it by tag. That creation is unconditional: if an account is already sitting at that path — hand-made, or left over from before the tags existed — it is left alone and the new account lands beside it under the same name, which GnuCash permits. Adopting it would be a guess, and a wrong one is permanent, since dedup then skips those rows on every re-run; two same-named siblings are visible and fixable instead, so move the untagged one's transactions into the tagged one and delete the empty account. Pot backing accounts carry no type or name over the API, but pot-transfer metadata links each to its pot and [List Pots](https://docs.monzo.com/#list-pots) supplies the pot's name, so each pot posts into its own child of the `Pots` account, named for the pot and its backing account (e.g. `…:Monzo:Pots:Savings (00009237AQC8…)`, so two pots that share a name stay apart), created on first sight and tagged with its backing-account ID in an account-level `online_id` slot — the same association GnuCash's own OFX importer stores, so a pot account you matched by hand in a past GUI import of the exported OFX is recognised too. The book itself thereby carries the association durably: later runs resolve the account by tag, and a book moved to a new machine (fresh state store) keeps working. Every run (export or import) also records the backing-account↔pot links it sees in the state store, so a pot linked once stays nameable even in a window whose only pot activity is interest. A pot the book doesn't know *and* no recorded link names fails the run before anything is written — a mis-filed row would be permanent, since dedup skips it on every re-run; one run whose window spans a transfer for the pot records the missing link. A pot denominated in anything other than the book's currency also fails the run, as does any resolved asset or category account the book holds in another commodity — Monzo's minor units would otherwise be posted as though they were the book's.

Monzo is authoritative for each of these asset accounts' whole placement, enforced on every run: while the Monzo side is live the account sits at its code-defined path, under the pot's current Monzo name (renames propagate); once retired — a closed account, or a deleted pot — it moves to the *same* path nested under `Archive`, and is hidden. Their descriptions are enforced empty, because the name already carries everything the book knows about the account — which Monzo account or pot posts into it, and the ID that keeps two of a kind apart — so a description could only restate it. Enforcement works in both directions, so hand-moves, renames, hides, descriptions and archivals of these accounts last only until the next import. Two safeguards: if a *different* account already occupies a canonical spot the run fails for you to resolve by hand — automatically merging or deleting it could orphan transactions — and if two Monzo accounts ever resolve to one book account (which only happens if the book itself says so, via an `online_id` tag added by hand to an account another Monzo account already answers to, since resolution never adopts an account by location) the run fails before filing anything, rather than commingling their transactions permanently. Each transaction becomes one balanced GnuCash transaction with two splits: the signed amount on that account's asset account, and its negation on a category account.

Monzo's own transaction `category` is authoritative for filing, with a small kind map because not every category is spending: `income` files under `Income:General`; `transfers` and `savings` post to a single wash account, `Assets:Transfers`, where the two legs of a pot transfer net to zero and what remains is money moved to institutions the book imports nothing from (still an asset, not an expense — re-file those by hand); every other category becomes a child of `Expenses` named by title-casing it (`eating_out → Expenses:Eating Out`). A transaction with no category files under `Expenses:General` (Monzo's default category), and a refund arrives sign-flipped in its spending category, negating the expense. All created accounts inherit their parent's account type and commodity, with intermediate path segments created as placeholders — there is no mapping to maintain. Only the top-level `Assets`, `Expenses`, `Income` and `Liabilities` accounts must already exist, and GnuCash creates them in every new book, so an import can run against a freshly created (GBP) book.

Re-runs are idempotent: the Monzo transaction ID is written into an `online_id` KVP slot on the asset split (the same ID `--to-ofx` writes as the OFX `FITID`, and where GnuCash's generic importer stores it), and any transaction already carrying that ID is skipped. GnuCash's own import matcher recognises these rows too.

- Unlike `--to-ofx`, this sink doesn't advance the state-store bookmarks — dedup is by `online_id`, not by bookmark — so `--since` defaults to each account's bookmark only for choosing the fetch window.
- Both splits are written unreconciled; you reconcile them against a statement yourself, as with an OFX import.
- A missing book fails immediately, before the source is read: SQLite would otherwise create an empty book at a mistyped path and fail obscurely several queries later.
- A book GnuCash has open is refused, and a real run holds the lock while it writes. See [The GnuCash lock](#the-gnucash-lock).
- Each transaction is posted at GnuCash's own "neutral time", 10:59:00 UTC on the transaction's local calendar date, which is what `xaccTransSetDatePostedSecsNormalized` does to every date GnuCash itself records. That leaves enough slack either side for the row to render as the same day in any timezone, and puts imported rows in the same within-day position as hand-entered ones. The split's `enter_date` keeps the real instant.
- Before any non-dry run the book is copied aside, and once the run has actually written something that copy is kept as `<input>.<yyyyMMddTHHmmssZ>.bak` — the state the book was in just before that run, so restoring it undoes exactly that run and no other. A run that turns out to change nothing (nothing new to file, no account to create, move, rename, un-hide or tag) leaves no backup behind, so a scheduled import that finds nothing doesn't age out the backup that could undo the last one that did. The whole write runs in a single SQLite transaction, so a mid-run failure rolls back to the pre-run state (and the backup, kept in that case too, is the belt-and-braces restore). **Nothing ever deletes these** — an import won't remove a file you might need — so prune them yourself: they're the size of the book, one per run that changed it, and the names sort chronologically. If a run is killed between the copy and that decision, the copy is left behind as a `.bak.tmp`; the next run keeps it (under the dead run's own timestamp, since that's the run it would undo) rather than overwriting it, because there's no way to tell from the outside whether that run had already committed.
- Every transaction filed is printed as it goes — post date, signed amount, the asset and category accounts it lands in, and the payee — followed by the run's totals, so what a run did (or would do) can be read row by row rather than trusted as a count.
- `--dry-run` prints that same plan (each transaction as "would file", the already-present count, and the accounts that would be created) without writing to the book and without taking a backup. It runs inside a transaction that is always rolled back, so even a bug that reached a write would leave the book unchanged; a dry run that touches any row fails with an error saying so, rather than passing the plan off as complete.
- With `--from-monzo`, the same 90-day pot-window cap applies, for the same SCA reason. `--from-csv` is how you get round it.

## Building and running

The build is sbt with `sbt-projectmatrix`. The two interesting projects are `main3` (JVM) and `mainNative3` (Scala Native).

### JVM

```
sbt 'main3/run archive-accounts --input Accounts.gnucash'
```

Prerequisites:

- JDK 22 or later — the Keychain state store uses the [Foreign Function & Memory API](https://openjdk.org/jeps/454), which is final in JDK 22.

### Scala Native (macOS only)

```
sbt 'mainNative3/run transactions --from-monzo --to-ofx=monzo.ofx'
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
| `keychain` | jvm + native | `object Keychain` (`load(account)` / `save(account, bytes)`) backed by the macOS Keychain. One row per platform under `src/main/scalajvm` and `src/main/scalanative`, reaching it through Java's Foreign Function & Memory API (jextract) and sn-bindgen respectively. |
| `porcupine` | jvm + native | Inlined Porcupine fork: the cats-effect `Database` interface, on top of a per-platform `object Sqlite` (`Connection` / `Statement` over sqlite3) that sits alongside it under `src/main/scalajvm` and `src/main/scalanative`. |
| `main` | jvm + native | The CLI entry point. Hosts the smithy IDL (Monzo API, OFX, state-store state), the `Verbosity` enum + `fansi`-coloured `Log` façade, and wires `Keychain` + `Database` into `decline`'s `CommandIOApp`. |

## Status

This is a personal tool — interfaces, command names and on-disk state shape may change at any time without migration paths.
