#!/usr/bin/env bash

# The whole verification story for this repo. There are no tests, so what
# "verified" means here is: formatting and imports are clean, and both platform
# rows still compile. The rows resolve different transitives and run different
# codegen, so a change that compiles on one can fail on the other.

set -euo pipefail

cd "$(dirname "$0")/../.."

# `scalafix`, not `scalafixAll`: the latter also runs the Test configuration,
# which re-triggers jextract and sn-bindgen codegen into src_managed/test for
# the four FFI modules. sn-bindgen exits 10 there with no diagnostic, so
# `scalafixAll` fails on a clean tree regardless of the state of the code.
# There are no test sources for it to check anyway.
sbt --batch -no-colors \
  scalafmtCheckAll \
  "main3/compile" \
  "mainNative3/compile" \
  "scalafix --check"
