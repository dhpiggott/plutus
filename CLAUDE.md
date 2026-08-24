# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

`README.md` is the canonical user-facing description: commands, flags, the first-run Monzo OAuth flow, build prerequisites, and the per-module table. Read it first; this file only adds notes that are specific to working *inside* the codebase.

## sbt projects under projectMatrix

Every module is a `projectMatrix`, so the project names you pass to sbt are not the directory names. JVM rows take the bare module name suffixed with the Scala major version (`main3`, `keychain-jvm3`, `porcupine-jvm3`). Native rows also append the platform (`mainNative3`, `keychain-nativeNative3`, `porcupine-nativeNative3`). `sbt main/run` is not a valid task — use `main3/run` (JVM) or `mainNative3/run` (Native).

When a change touches code, build configuration, or smithy IDL that's reachable from both platforms, build both rows before declaring it done — they resolve different transitives and surface different errors:

```
sbt 'main3/compile' 'mainNative3/compile'
```

There are no tests in the repo — `sbt test` is a no-op, and there is no `src/test` directory in any module. `.github/scripts/verify.sh` is the whole verification story: `scalafmtCheckAll`, `scalafmtSbtCheck`, a bare `compile` (the root project aggregates every row, so this covers both platforms and survives a new module), and `scalafixAll --check`. Prefer running it over hand-assembling sbt invocations, so that what you check locally is what CI checks.

`scalafmtCheckAll` does not cover `build.sbt` or `project/` — those are what `scalafmtSbtCheck` is for, and `.scalafmt.conf` has an `sbt1` `fileOverride` for them.

The four `Test / *Bindings := Seq.empty` overrides in `build.sbt` are load-bearing for anything that touches the Test configuration. Without them every FFI module regenerates its bindings a second time into `src_managed/test`, which nothing consumes, concurrently with the Compile run — and `scalafixAll` has been seen to die there, sn-bindgen exiting 10 (Scala Native's unhandled-exception code) after an `Unrecoverable NullPointerException`. That crash is intermittent and stopped reproducing, so the overrides are justified by the duplicated work rather than by a confirmed diagnosis; if you remove them and `scalafixAll` looks fine, that is not evidence they were unnecessary.

## Smithy codegen output

`Smithy4sCodegenPlugin` is enabled on `main` and regenerates Scala on every compile of either row, into `main/target/<row>-3/src_managed/main/smithy4s/...` (so the generated sources are duplicated across `main/target/jvm-3/...` and `main/target/native-3/...`). When chasing a "type doesn't compile" error or trying to understand the shape of a generated case class, look there. Don't edit those files; change the corresponding `*.smithy` source under `main/src/main/smithy/`.

## State-store boundary

The state-store boundary is just `object Keychain` exposed by `keychain-jvm` / `keychain-native` — `load(account: String): IO[Option[Array[Byte]]]` and `save(account: String, bytes: Array[Byte]): IO[Unit]`. `main` JSON-encodes/decodes `State` against those via top-level `loadState` / `saveState` in `MonzoCommands.scala`; whichever platform row is being built supplies the implementation.

## SQLite boundary

`porcupine-jvm` and `porcupine-native` each expose an `object porcupine.Sqlite` with parallel `Connection` / `Statement` traits over the `sqlite3` C API, trafficking only in primitive types (`Long`, `Double`, `String`, `Array[Byte]`, `Any | Null`). `porcupine` depends on both (one per row) and layers `Database[F]` on top — codec encoding/decoding, `Mutex`-serialised access, `F.blocking`, `Resource`. Adding a column or function to `Sqlite` means matching changes in *both* platform files; the shapes drift silently because there's no shared trait.

## GnuCash book access

Every table the CLI touches has a row type in `main` that owns *all* the SQL for it — `Account`, `Commodity`, `Transaction`, `Split`, `Slot`, and `Posting` (a transaction plus its two balanced splits). Queries belong in those objects, not in the command files; `GnuCashCommands.scala` composes them and takes `Database[IO]` as a `using` parameter.

`Transact.scala` adds three extensions used by all three commands: `db.transact` (one `begin immediate` … `commit`, rolled back on failure), `db.withoutCommitting` (a deferred `begin` always rolled back, which is how `--dry-run` guarantees nothing survives), and `db.transactOrRollBack(dryRun)` picking between them. `db.rowsChanged` reads SQLite's own `total_changes()`, so no write path has to report itself — after `transact` it answers "did this run change anything?", after `withoutCommitting` "did this run try to?".

`withBook` in `GnuCashCommands.scala` is the file-level wrapper all three commands open the book through: it refuses a missing `--input` (porcupine opens with `SQLITE_OPEN_CREATE`, so a typo would otherwise create an empty book), takes the `.bak.tmp` copy, and afterwards promotes it to `<input>.<timestamp>.bak`, deletes it, or — in a dry run — fails if `rowsChanged` is non-zero. It deliberately does *not* wrap the body in a transaction: `restore-account` has to keep its interactive prompt outside the write lock, so each body calls `db.transactOrRollBack` itself.

Transactions are written with `post_date` normalised to GnuCash's neutral time, 10:59:00 UTC on the local calendar date (`neutralPostDate` in `Transaction.scala`) — the same normalisation `xaccTransSetDatePostedSecsNormalized` applies to everything GnuCash records, so a date renders identically in any timezone. `enter_date` stays the real instant.

The `online_id` slot carries two unrelated meanings, both of which this code writes: on an *account* it is GnuCash's OFX association, keyed here by Monzo account ID; on a *split* it is the imported row's dedup key, the Monzo transaction ID. `Slot.onlineIds` returns both in one scan — `slots` is unindexed and grows with history, so prefer widening that prefetch to adding another scan.

## macOS Keychain FFI gotchas

The Keychain is reached two different ways: `keychain-native` (Scala Native, sn-bindgen) and `keychain-jvm` (JVM, Java's Foreign Function & Memory API). Both end up calling `SecItemCopyMatching`/`SecItemAdd`/`SecItemUpdate`; the `extern const CFStringRef` constants (`kSecClass`, …) are the main wrinkle on both sides.

`keychain-native`:

- **`build.sbt` generates `macos.h` at build time** with the macOS SDK path resolved by `xcrun --show-sdk-path` baked into absolute `#include` lines. sn-bindgen filters declarations out of headers it considers "system headers"; angle-bracket includes (`<CoreFoundation/CFNumber.h>`) get that tag, absolute-path includes don't. Don't "simplify" it back to angle-bracket form — the bindings come out silently empty.
- **`src/main/scala/macos/Globals.scala`** declares `kSecClass`, `kSecAttrAccount`, `kCFBooleanTrue`, … as `var name: T = extern` inside an `@extern object`. sn-bindgen only emits Scala bindings for functions, types and structs, not for `extern const` variables, but Scala Native's `var = extern` reads the C global directly — no C forwarder needed.

`keychain-jvm`:

- **Frameworks are loaded by absolute path** (`/System/Library/Frameworks/CoreFoundation.framework/CoreFoundation`, same for Security) via `SymbolLookup.libraryLookup(...)`. Using the bare `"CoreFoundation"` name won't work — `System.loadLibrary` only searches `java.library.path` / `DYLD_LIBRARY_PATH`, neither of which covers `/System/Library/Frameworks`.
- **`extern const CFTypeRef` globals need an extra dereference.** `SymbolLookup.find("kSecClass")` returns the *address* of the symbol — a pointer-sized cell that itself holds the actual `CFTypeRef`. So the value to pass to `SecItem*` is `cell.reinterpret(ADDRESS.byteSize).get(ADDRESS, 0)`, not the segment from `find()` itself. (This is the JVM analogue of `Forwarders.c` on the native side.)
- **`MethodHandle.invokeWithArguments`, not `invokeExact`.** `invokeExact` is signature-polymorphic and brittle from Scala 3 — small mismatches (e.g. an unboxed `Int` vs. boxed `Integer` return) silently miscompile. `invokeWithArguments` boxes everything, returns `Object`, and a tiny number of keychain calls per CLI run makes the overhead irrelevant.
- **`--enable-native-access=ALL-UNNAMED`** is set in `main`'s JVM `javaOptions` to suppress the "restricted method" warning on JDK 22+. Required for `sbt main3/run` to not spam stderr, not for correctness.
- **`javacOptions += "-parameters"`** on `keychain-jvm` keeps jextract's parameter names (`query`, `result`, `attributes`, …) in the generated Java bytecode. Without it, Scala only sees `arg0`/`arg1`/… and the named-arg call style (`SecItemCopyMatching(query = …, result = …)`) used in `Keychain.scala` doesn't compile.

## sqlite3 FFI gotchas

`porcupine-native` (sn-bindgen):

- **sqlite3 is sourced via `VcpkgNativePlugin`**, which builds `libsqlite3.a` from source on first compile (slow — needs `cmake`, `ninja`, `pkg-config`) and exposes its include dir for codegen. The plugin only injects link/include config into the project where it's enabled, so **both `porcupine-native` and `main` enable it and re-declare `vcpkgDependencies := VcpkgDependencies("sqlite3")`** — sbt's `dependsOn` does not propagate vcpkg config.
- **`main` enables `VcpkgNativePlugin` on the native row only**, via `.nativePlatform(scalaVersions, axisValues, configure = _.enablePlugins(VcpkgNativePlugin).settings(...))`. Enabling it at the projectMatrix level instead poisons JVM compilation: `VcpkgNativePlugin` auto-loads `ScalaNativePlugin`, which (a) overrides `crossVersion` so `%%%` deps resolve as `_native0.5_3` instead of `_3`, (b) injects nscplugin as a compiler plugin, and (c) appends `-P:scalanative:positionRelativizationPaths:…` to `Compile / compile / scalacOptions` — and the plain Scala 3 compiler rejects (c) with "bad option" on the JVM row.
- **`main`'s `nativeConfig ~=` must append, not replace.** `VcpkgNativePlugin` runs first and injects `-L<vcpkg-install>/lib -lsqlite3 -pthread`; a bare `_.withLinkingOptions(Seq(...))` silently discards those and the binary falls back to the system `libsqlite3.dylib` (or fails to link). Use the `c => c.withLinkingOptions(c.linkingOptions ++ Seq(...))` form.
- **Binding package is `libsqlite`, not `sqlite3`.** sn-bindgen emits a struct called `sqlite3` (the opaque DB handle); a package of the same name shadows it after `import <pkg>.all.*`.
- **`SQLITE_OK`/`SQLITE_ROW`/`SQLITE_OPEN_*`** come from sn-bindgen's `withMacros(Set("SQLITE_*"))` + `withOnlyValidMacros(true)`. The second flag is required — some `SQLITE_OK_*` macros are composite expressions (`SQLITE_OK | (1<<8)`) sn-bindgen can't render, and without `onlyValidMacros` codegen fails outright instead of skipping them.
- **`sqlite3_bind_text`/`bind_blob64` get `null` as the destructor** (SQLITE_STATIC), so the byte arrays bound from Scala must stay GC-live until `sqlite3_reset`. `Sqlite.StatementImpl` holds them in a `bindRefs: List[Array[Byte]]` that `reset()` clears.

`porcupine-jvm` (jextract):

- **`libsqlite.h` is a one-line shim** that `#include`s `<sdkPath>/usr/include/sqlite3.h` with the absolute SDK path baked in by `xcrun --show-sdk-path`. Same rationale as the `keychain-jvm` shim — jextract uses the shim's filename for the generated `*_h` class names (`libsqlite_h`, `libsqlite_h_1`), so naming the shim `libsqlite.h` keeps the symbol surface under `libsqlite.*` and parallels the sn-bindgen package.
- **The library is loaded by absolute path** (`-l :/usr/lib/libsqlite3.dylib`). On modern macOS that file is only resolvable via the dyld shared cache (`ls` returns nothing), but `dlopen` finds it — `System.loadLibrary("sqlite3")` would not, because `/usr/lib` isn't in `java.library.path` by default.
- **The bulk of the symbols (functions, `SQLITE_OK`/`OPEN_*`/column-type codes) land in `libsqlite_h_1`**, with `libsqlite_h` only holding obscure overflow constants. Importing just `libsqlite.libsqlite_h_1.*` covers everything `Sqlite.scala` needs.
- **`SQLITE_STATIC` (`MemorySegment.NULL`) is the destructor passed to `sqlite3_bind_text`/`bind_blob`**, so sqlite reads straight from the pointer we hand it and the bound segments must outlive the statement's last `step()`. `StatementImpl` allocates them from a per-statement `bindArena: Arena.ofConfined()`; `reset()` closes it and re-opens a fresh one, `close()` closes it after `sqlite3_finalize`. Heap arrays can't take their place — the JVM GC is moving, so a long-lived `MemorySegment.ofArray(byte[])` pointer would dangle the moment the call returned. This is the JVM analogue of the native side's `bindRefs` list.

## GitHub Actions

Two workflows, both in `.github/workflows/`: `ci.yml` (build) and `claude.yml` (`@claude` mentions on issues and PRs).

There is deliberately no automatic reviewer. `claude-code-review.yml` existed until it was removed, and never worked: it produced a green required check without reviewing anything, three separate ways — the `Skill` tool missing from the action's tool set (#32), `Task` missing too so the review plugin's agent fan-out could not run (#43), and the action's workflow-validation skip on any PR that edits `claude*.yml`. Each failure exits 0. Ask for review instead by commenting `@claude review this` on the pull request, which runs `claude.yml`: Opus, on macOS, able to run `verify.sh` — none of which the reviewer could do.

For a heavier pass, `claude.yml` also installs the `code-review@claude-code-plugins` skill. **When a comment asks for a full, deep or thorough review — `@claude full review` is the intended phrasing — invoke it for the pull request the comment is on:**

```
/code-review:code-review --comment <owner>/<repo>/pull/<N>
```

**Always pass `--comment`.** The skill's step 7 reads "If `--comment` argument was NOT provided, stop here. Do not post any GitHub comments", so without the flag a review that finds something keeps it to itself. Fill in the repository and PR number from the comment's own context rather than asking for them.

A plain `@claude review this` means the unaided review, which is cheaper and has been enough so far — reach for the skill when the diff is large or touches the FFI and build rules.

The skill fans out to four parallel reviewers — two on CLAUDE.md compliance, two on bugs — then validates every finding with a second agent before posting. It needs `Skill` and `Task`, which the action does not grant, so it works only because `claude.yml` runs under `--permission-mode auto`.

- **`ci.yml` and `claude.yml` run on `macos-latest`, and have to.** Not just for the Scala Native row: the JVM row's `keychain-jvm` and `porcupine-jvm` bindings are generated by jextract against the SDK that `xcrun --show-sdk-path` resolves, so *nothing* in this build compiles on Linux. If a build step is failing at the very first bindings task, check the runner OS before anything else.
- **`.github/actions/setup-build` is the one place that provisions the toolchain** — Homebrew packages via `.github/scripts/install-build-deps.sh`, a JDK (22+, for the Foreign Function & Memory API), sbt via `sbt/setup-sbt`, and caches for coursier/sbt, the vcpkg-built `libsqlite3.a`, and the ~150MB jextract distribution. Add setup there rather than inline in a workflow, so the build and Claude workflows can't drift apart.
- **The action allows no Bash commands by default, and `claude.yml` answers that with `--permission-mode auto` rather than a long allowlist.** The action runs headless under `acceptEdits`, where an unlisted call has no prompt to fall back to and is denied outright; under auto mode a classifier reviews it instead, so `sbt`, `git`, `gh` and `verify.sh` are reachable without being enumerated. So the fix for "Claude couldn't run X" is no longer to widen a list. The one surviving entry, `mcp__github_inline_comment__create_inline_comment`, is not there for permission: `install-mcp-server.ts` starts the `github_inline_comment` server only when a tool with that prefix is in the allowlist, so omitting it removes the tool rather than deferring it to the classifier.
- **`mcp__github_inline_comment__create_inline_comment` is in that allowlist so review-on-request lands on the diff.** Listing an `mcp__github_*` tool does two jobs: the action's `install-mcp-server.ts` only starts the matching MCP server when a tool with that prefix appears in the allowlist, so omitting it doesn't merely deny the tool, the server never starts and the tool does not exist.
- **A green Claude check is not evidence Claude did anything.** Headless runs have no prompt handler, so a call the mode won't auto-approve is refused rather than asked about, and the job still exits 0 — under `acceptEdits` because the tool wasn't allowlisted, under auto mode because the classifier blocked it and "the action doesn't run and Claude keeps working". The tells are in the run log's result block: `permission_denials_count` above zero, and `No buffered inline comments` from the post-step.
- **Checkout is `fetch-depth: 0` everywhere on purpose**, for two different reasons. In the workflows that build, sbt-dynver derives `version` from the tag history and that version lands in `BuildInfo`, so a shallow clone silently yields `0.0.0+…`. It also gives Claude the history to read while it works.
- `ci.yml` runs on pushes to `main` as well as PRs specifically so the caches it populates are visible to PR and `@claude` runs. Those runs are also the only ones that reach `nativeLink`, so they are exempt from `cancel-in-progress`.

### The ruleset on `main`

`main` is governed by a repository ruleset: pull requests required with no bypass actors, `Build` must pass, commits must be signed, and only squash and merge commits are allowed.

- **Rebase merging is impossible while signatures are required.** GitHub refuses it outright — "Rebase merges cannot be automatically signed by GitHub" — because it rewrites commits server-side and cannot sign the results. Squash is fine: GitHub authors that commit and signs it. A merge commit preserves the branch's own commits, so every one of them must already be signed.
- **A job's `name` is its check-run name, and `Build` is a ruleset context.** Renaming a job therefore breaks merging until the ruleset's required contexts are updated — including for the renaming PR itself, which reports under the new names while the old ones are still required. Sequence: open the PR, let its checks report, update the contexts, then merge.
- **Auto-merge is re-evaluated on pull request events, not on ruleset edits.** Clearing the last blocking condition by changing the ruleset leaves the PR mergeable but unmerged; a check completing afterwards is what makes GitHub look again.
- Commits Claude pushes are created through the GitHub API and signed by GitHub (`use_commit_signing: true` in `claude.yml`), which is what lets them satisfy the signature rule. That path builds single-parent commits, so Claude cannot produce a signed merge commit this way.
- **`bot_id: "209825114"` is a workaround, not a preference.** The action's default is `41898282`, which is `github-actions[bot]` despite being documented as Claude's — see anthropics/claude-code-action#759. GitHub resolves a commit to an account by the numeric ID in its noreply address, so on the default every commit Claude pushes is misattributed. Drop the override once upstream fixes the constant.

## Conventions

- **Scala 3 colon-block / fewer-braces**, enforced by `.scalafmt.conf` (`rewrite.scala3.convertToNewSyntax = true`, `removeOptionalBraces = true`). Match surrounding style — don't introduce braces.
- **`Verbosity` is a Scala 3 enum (`Main.scala`), passed implicitly, never as a parameter**: every logging-aware function takes `using verbosity: Verbosity`.
- **No `// what` comments**: existing comments are exclusively non-obvious *why* (e.g. the `xcrun --show-sdk-path` rationale, the `-L/opt/homebrew/lib` hint about epollcat/s2n in `build.sbt`, the sn-bindgen `extern const` rationale in `Globals.scala`).
- **`dependencyUpdatesFailBuild := true`** on every module: `sbt dependencyUpdates` *fails* rather than just reporting when any dep has a newer release. It only affects that task — `compile` is unaffected, so a stale dependency never breaks a build or CI. When bumping, expect cascading changes across both platforms; `porcupine-jvm` / `porcupine-native` have no runtime deps but `porcupine` (cats-effect, fs2, scodec-bits) does.
