# Plan: CSV-based `import-transactions`

A future variant of `gnucash import-transactions` that reads the CSV
statements the Monzo app exports, instead of calling the API. This is the
"future variant" the code comment on `importTransactionsOpts` anticipates; the
command already lives under `gnucash` rather than `monzo` for this reason.

## Why

- **Full history.** The API's Strong Customer Authentication rules cap pot
  windows at 90 days, and main-account full history is only available for five
  minutes after authorisation. App-exported CSVs carry the complete history of
  an account or pot, so a CSV import is the only way to backfill pot interest
  older than 90 days (today the README points at QIF for this, which loses the
  categorisation and dedup benefits of our own importer).
- **No session.** No OAuth client, no refresh-token lifecycle, no state store
  — a book plus files is enough. Useful for one-off backfills and for
  machines that never hold Monzo credentials.

## What already carries over

The import was deliberately split at `fetchTransactionsByAccount`: everything
downstream — `AssetAccounts` mapping, `categoryTarget` filing, `Posting`
construction, `online_id` dedup, fail-fast checks, retired-account archiving —
consumes `monzo.Transaction` values grouped by account, plus per-pot details.
A CSV source only has to produce the same shapes.

Two existing decisions pay off directly:

- **Dedup interoperates.** The CSV's `Transaction ID` column carries the same
  `tx_…` IDs the API returns, and dedup is by `online_id` slot, not by
  bookmark. Mixed API and CSV imports of overlapping windows stay idempotent
  in both directions.
- **Pot identity is already in the book.** Each pot's asset account is tagged
  with its backing-account ID in an account-level `online_id` slot, so a CSV
  import can resolve previously-imported pots without the API's `/pots` at
  all.

## Decoding

Parse with **fs2-data-csv** — the CSV sibling of the fs2-data-xml module the
OFX exporter already uses, so no new dependency family.

Map columns to a `monzo.Transaction` (only the members the importer consumes
need to be faithful):

| CSV column | Transaction member | Notes |
| --- | --- | --- |
| `Transaction ID` | `id` | Same `tx_…` values as the API — the dedup key. |
| `Date` + `Time` | `created` | Combine; the export is in local time, so document the zone assumption (likely Europe/London) and convert to an instant. |
| `Amount` | `amount` | Decimal major units in the CSV; scale by the currency's minor unit to recover the API's integer representation. |
| `Currency` | — | Drives the same book-currency fail-fast as `Pot.currency` does today. |
| `Category` | `category` | Same strings as the API. |
| `Notes and #tags` | `notes` | |
| `Name` | `merchant.name` | The CSV flattens merchant/counterparty into one column; feeding it through `merchant` keeps the shared `payee` precedence working unchanged. |
| `Description` | `description` | Fallback payee, as with the API. |
| — | `declineReason` | Absent: the app doesn't export declined transactions. `None` throughout; `materialTransactions` still drops £0 active-card checks by amount. |
| — | `metadata` | No pot metadata in the CSV — see below. |

Open verification task before building: confirm the exact header row against a
fresh export (personal account, joint account, and a pot statement), since
Monzo has changed it before.

## Account identity

A CSV has no account-ID column, so the user must say which account each file
belongs to. Proposed shape:

```
plutus gnucash import-transactions --csv acc_xxx=statement.csv [--csv pot_backing_acc=pot.csv …] [--input PATH] [--dry-run]
```

- Repeatable `id=path` pairs, mirroring how one file corresponds to one
  account or pot in the app's export flow.
- `--csv` is mutually exclusive with `--since`/`--before` (decline `Opts`
  handle this); with `--csv` present the command never touches the network or
  the state store.
- The ID keys into the same machinery as the API path: a `uk_retail`-type
  lookup can't work from a bare ID, so mapping needs either the account's type
  (`--csv uk_retail:acc_xxx=path`?) or, simpler, resolution purely by the
  book's `online_id` tags with `AssetAccounts` as the fallback for
  first-time accounts. Decide when building; the second keeps the CLI cleaner.

## Pots

Without the API there is no `/pots`, no `pot_id` metadata, and no
`State.potIds`:

- **Already-imported pots** resolve by their book tag — no further input
  needed. This will be the common case for the motivating backfill scenario
  (the pot exists; we want its old interest).
- **Never-imported pots** can't be named automatically. Fail fast (as the API
  path does) with an error saying either to run an API import first (which
  records the link and tags the account) or to create and tag the account by
  hand. No silent fallback, for the same mis-filing-is-permanent reason.
- **Deleted-pot archiving and closed-account archiving don't apply** — the CSV
  says nothing about closure. Skip that step in CSV mode rather than infer.

## Phases

1. **Decoder**: fs2-data-csv row → `monzo.Transaction`, with tests against
   captured export files (this would introduce the repo's first tests; the
   pure decoder is the right first test surface).
2. **CLI + wiring**: `--csv` options, mutual exclusion, a `fetch`-shaped
   function producing `(byAccount, pots)` from files, everything downstream
   untouched.
3. **Resolution rules**: book-tag-first account resolution, first-time-pot
   fail-fast, book-currency check from the `Currency` column.
4. **README**: document the export flow in the app (Settings → statement
   export → CSV; Pot → Pot documents → Pot statement) and the zone caveat.

## Open questions

- Which timezone the app stamps `Date`/`Time` in, and whether it varies.
- Whether joint-account exports differ in shape.
- Whether `Category split` rows (one transaction split across categories in
  the app) should map to multi-split GnuCash transactions or file under the
  primary category initially.
