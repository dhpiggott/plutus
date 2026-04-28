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

Two non-obvious things in `native-macos-keychain-state-store`:

- **`build.sbt` generates `macos.h` at build time** with the macOS SDK path resolved by `xcrun --show-sdk-path` baked into absolute `#include` lines. sn-bindgen filters declarations out of headers it considers "system headers"; angle-bracket includes (`<CoreFoundation/CFNumber.h>`) get that tag, absolute-path includes don't. Don't "simplify" it back to angle-bracket form — the bindings come out silently empty.
- **`src/main/resources/scala-native/Forwarders.c`** wraps `extern const` globals (`kSecClass`, `kSecAttrAccount`, `kCFBooleanTrue`, …) in trivial getter functions. sn-bindgen only emits Scala bindings for functions, types and structs, not for `extern const` variables, so without the forwarders those constants are unreachable from Scala Native.

## Conventions

- **Scala 3 colon-block / fewer-braces**, enforced by `.scalafmt.conf` (`rewrite.scala3.convertToNewSyntax = true`, `removeOptionalBraces = true`). Match surrounding style — don't introduce braces.
- **`Verbosity` is implicit, not a parameter**: every logging-aware function takes `using verbosity: Verbosity`.
- **No `// what` comments**: existing comments are exclusively non-obvious *why* (e.g. the `xcrun --show-sdk-path` rationale, the `-L/opt/homebrew/lib` hint about epollcat/s2n in `build.sbt`, the sn-bindgen forwarders rationale in `Forwarders.c`).
- **`dependencyUpdatesFailBuild := true`** on every module: a regular `sbt compile` fails when any dep has a newer release. When bumping, expect cascading changes across both platforms, and keep the three porcupine modules' deps aligned (`porcupine`, `porcupine-jvm`, `porcupine-native`).
