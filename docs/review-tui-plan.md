# Plan: `--review` — interactive filing review for `import-transactions`

An opt-in TUI step letting the user review and override the category/expense
account each transaction files against, before anything is written.

## Library constraint (prunes the option space)

Whatever we use must build on both rows (`main3`, `mainNative3`). Full-screen
TUI libraries are effectively JVM-only on Scala Native — tui-scala (the
ratatui port) drives its terminal backend through JNA. cue4s — already in the
repo powering `restore-account`'s account picker and the OAuth prompts —
cross-builds JVM/Native and provides single/multiple choice (with
type-to-filter), text input, and confirms. So the design is a prompt-sequence
UX on cue4s, not a curses-style dashboard, unless import is made JVM-only
(not worth it; the constraint costs little).

## Options considered

- **A — per-transaction walk.** Prompt for every not-yet-imported
  transaction: date/payee/amount/proposed target, choose from [proposed
  (default), other targets seen this run, "enter a path…", "accept all
  remaining"]. Maximal control, but dozens of keystrokes per month where 95%
  of answers are "accept" — fatigue kills the habit.
- **B — grouped review (chosen).** Group the plan by (payee, proposed
  target) — "Tesco → Expenses:Groceries (14 transactions, £182.40)" — and
  prompt once per group; overriding a group overrides all its members. A
  typical month collapses to a handful of prompts. Optionally a
  `multipleChoice` "pick the groups to change" pre-step (select none → zero
  further prompts).
- **C — review only the uncertain.** Prompt solely for `Expenses:General`
  landings (no/unmapped category) and optionally the `Assets:Transfers`
  residue. Near-zero friction but can't fix a confidently wrong filing.
- **D — no TUI; use GnuCash's.** `export-transactions` + GnuCash's OFX import
  matcher already is a review-each-transaction UI with account override and
  memory. Named so the TUI is built only for review *without* the GUI.

## Design decisions

- **Opt-in `--review` flag**; the default stays non-interactive so scheduled
  runs keep working.
- **Prompt before the write transaction.** The flow becomes fetch → compute
  plan → prompt → transact-and-write. Human think-time must not sit inside an
  open SQLite write transaction (lock held, journal live), and Ctrl-C during
  review must leave the book untouched. The plan is pure data (paths, not
  created accounts), so the reorder is natural.
- **No persisted override state.** Overrides are one-shot: once written,
  online_id dedup makes the filing permanent, so nothing needs remembering —
  preserving "Monzo is authoritative, no mappings to maintain". A
  remember-forever extension would reintroduce ImportRules through the back
  door; if the same payee needs overriding every month, revisit the rules
  decision explicitly instead.
- **Composability with `--dry-run`**: review-plus-dry-run is a rehearsal mode
  and falls out free if review is a plan transform.
- Choice list per group: the proposed target first (default), then the other
  targets seen this run, then a free-text path that get-or-creates like every
  other leaf, then "accept all remaining".

## Rough shape

A plan data type (transaction, proposed path); one cue4s loop transforming
the plan; the transact boundary moved after the prompts. Smaller than the
placement-enforcement work was.
