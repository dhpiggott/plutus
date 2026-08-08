#!/usr/bin/env bash

# The whole verification story for this repo. There are no tests, so what
# "verified" means here is: formatting and imports are clean, and both platform
# rows still compile. The rows resolve different transitives and run different
# codegen, so a change that compiles on one can fail on the other.

set -euo pipefail

cd "$(dirname "$0")/../.."

# scalafmtCheckAll skips build.sbt and project/, which .scalafmt.conf has a
# dedicated sbt1 fileOverride for; scalafmtSbtCheck is what covers those.
sbt --batch -no-colors \
  scalafmtCheckAll \
  scalafmtSbtCheck \
  "main3/compile" \
  "mainNative3/compile" \
  "scalafixAll --check"
