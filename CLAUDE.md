# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

`README.md` is the canonical user-facing description: commands, flags, the first-run Monzo OAuth flow, build prerequisites, and the per-module table. Read it first; this file only adds notes that are specific to working *inside* the codebase.

## sbt projects under projectMatrix

Every module is a `projectMatrix`, so the project names you pass to sbt are not the directory names. JVM projects keep the bare module name suffixed with the Scala major version (`main3`, `log3`, `porcupine-jvm3`). Native projects also append the platform (`mainNative3`, `logNative3`). `sbt main/run` is not a valid task — use `main3/run` (JVM) or `mainNative3/run` (Native).

There are no tests in the repo — `sbt test` is a no-op, and there is no `src/test` directory in any module.

## Smithy codegen output

`Smithy4sCodegenPlugin` regenerates Scala on every compile of `smithy4s-schemas`, into `smithy4s-schemas/target/<platform>-3/src_managed/main/smithy4s/...`. When chasing a "type doesn't compile" error or trying to understand the shape of a generated case class, look there. Don't edit those files; change the corresponding `*.smithy` source under `smithy4s-schemas/src/main/smithy/`.

The Smithy `StateStore` service is the abstraction boundary between `main` and the platform-specific state-store modules — `main` only sees the generated `StateStore[F[_]]` algebra, never the concrete impl.

## macOS Keychain FFI gotchas

The Keychain is reached two different ways: `macos-keychain-state-store-native` (Scala Native, sn-bindgen) and `macos-keychain-state-store-jvm` (JVM, Java's Foreign Function & Memory API). Both end up calling `SecItemCopyMatching`/`SecItemAdd`/`SecItemUpdate`; the `extern const CFStringRef` constants (`kSecClass`, …) are the main wrinkle on both sides.

`macos-keychain-state-store-native`:

- **`build.sbt` generates `macos.h` at build time** with the macOS SDK path resolved by `xcrun --show-sdk-path` baked into absolute `#include` lines. sn-bindgen filters declarations out of headers it considers "system headers"; angle-bracket includes (`<CoreFoundation/CFNumber.h>`) get that tag, absolute-path includes don't. Don't "simplify" it back to angle-bracket form — the bindings come out silently empty.
- **`src/main/resources/scala-native/Forwarders.c`** wraps `extern const` globals (`kSecClass`, `kSecAttrAccount`, `kCFBooleanTrue`, …) in trivial getter functions. sn-bindgen only emits Scala bindings for functions, types and structs, not for `extern const` variables, so without the forwarders those constants are unreachable from Scala Native.

`macos-keychain-state-store-jvm`:

- **Frameworks are loaded by absolute path** (`/System/Library/Frameworks/CoreFoundation.framework/CoreFoundation`, same for Security) via `SymbolLookup.libraryLookup(...)`. Using the bare `"CoreFoundation"` name won't work — `System.loadLibrary` only searches `java.library.path` / `DYLD_LIBRARY_PATH`, neither of which covers `/System/Library/Frameworks`.
- **`extern const CFTypeRef` globals need an extra dereference.** `SymbolLookup.find("kSecClass")` returns the *address* of the symbol — a pointer-sized cell that itself holds the actual `CFTypeRef`. So the value to pass to `SecItem*` is `cell.reinterpret(ADDRESS.byteSize).get(ADDRESS, 0)`, not the segment from `find()` itself. (This is the JVM analogue of `Forwarders.c` on the native side.)
- **`MethodHandle.invokeWithArguments`, not `invokeExact`.** `invokeExact` is signature-polymorphic and brittle from Scala 3 — small mismatches (e.g. an unboxed `Int` vs. boxed `Integer` return) silently miscompile. `invokeWithArguments` boxes everything, returns `Object`, and a tiny number of keychain calls per CLI run makes the overhead irrelevant.
- **`--enable-native-access=ALL-UNNAMED`** is set in `main`'s JVM `javaOptions` to suppress the "restricted method" warning on JDK 22+. Required for `sbt main3/run` to not spam stderr, not for correctness.

## porcupine-native sqlite3 FFI gotchas

- **sqlite3 is sourced via `VcpkgNativePlugin`**, which builds `libsqlite3.a` from source on first compile (slow — needs `cmake`, `ninja`, `pkg-config`) and exposes its include dir for codegen. The plugin only injects link/include config into the project where it's enabled, so **both `porcupine-native` and `main` enable it and re-declare `vcpkgDependencies := VcpkgDependencies("sqlite3")`** — sbt's `dependsOn` does not propagate vcpkg config.
- **`main` enables `VcpkgNativePlugin` on the native row only**, via `.nativePlatform(scalaVersions, axisValues, configure = _.enablePlugins(VcpkgNativePlugin).settings(...))`. Enabling it at the projectMatrix level instead poisons JVM compilation: `VcpkgNativePlugin` auto-loads `ScalaNativePlugin`, which (a) overrides `crossVersion` so `%%%` deps resolve as `_native0.5_3` instead of `_3`, (b) injects nscplugin as a compiler plugin, and (c) appends `-P:scalanative:positionRelativizationPaths:…` to `Compile / compile / scalacOptions` — and the plain Scala 3 compiler rejects (c) with "bad option" on the JVM row.
- **`main`'s `nativeConfig ~=` must append, not replace.** `VcpkgNativePlugin` runs first and injects `-L<vcpkg-install>/lib -lsqlite3 -pthread`; a bare `_.withLinkingOptions(Seq(...))` silently discards those and the binary falls back to the system `libsqlite3.dylib` (or fails to link). Use the `c => c.withLinkingOptions(c.linkingOptions ++ Seq(...))` form.
- **Binding package is `libsqlite`, not `sqlite3`.** sn-bindgen emits a struct called `sqlite3` (the opaque DB handle); a package of the same name shadows it after `import <pkg>.all.*`.
- **`SQLITE_OK`/`SQLITE_ROW`/`SQLITE_OPEN_*`** come from sn-bindgen's `withMacros(Set("SQLITE_*"))` + `withOnlyValidMacros(true)`. The second flag is required — some `SQLITE_OK_*` macros are composite expressions (`SQLITE_OK | (1<<8)`) sn-bindgen can't render, and without `onlyValidMacros` codegen fails outright instead of skipping them.

## Conventions

- **Scala 3 colon-block / fewer-braces**, enforced by `.scalafmt.conf` (`rewrite.scala3.convertToNewSyntax = true`, `removeOptionalBraces = true`). Match surrounding style — don't introduce braces.
- **`Verbosity` is implicit, not a parameter**: every logging-aware function takes `using verbosity: Verbosity`.
- **No `// what` comments**: existing comments are exclusively non-obvious *why* (e.g. the `xcrun --show-sdk-path` rationale, the `-L/opt/homebrew/lib` hint about epollcat/s2n in `build.sbt`, the sn-bindgen forwarders rationale in `Forwarders.c`).
- **`dependencyUpdatesFailBuild := true`** on every module: a regular `sbt compile` fails when any dep has a newer release. When bumping, expect cascading changes across both platforms, and keep the three porcupine modules' deps aligned (`porcupine`, `porcupine-jvm`, `porcupine-native`).
