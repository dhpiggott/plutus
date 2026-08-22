#!/usr/bin/env bash

# The whole verification story for this repo. There are no tests, so what
# "verified" means here is: formatting and imports are clean, and every module
# still compiles. The rows resolve different transitives and run different
# codegen, so a change that compiles on one can fail on the other.
#
# A bare `compile` rather than naming main3 and mainNative3: the root project
# aggregates every row, so this covers the same ground and keeps working when
# projectMatrix adds one.

set -euo pipefail

cd "$(dirname "$0")/../.."

# scalafmtCheckAll skips build.sbt and project/, which .scalafmt.conf has a
# dedicated sbt1 fileOverride for; scalafmtSbtCheck is what covers those.
sbt --batch -no-colors \
  scalafmtCheckAll \
  scalafmtSbtCheck \
  compile \
  "scalafixAll --check"
