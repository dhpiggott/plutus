#!/usr/bin/env bash

# The whole verification story for this repo. There are no tests, so what
# "verified" means here is: formatting is clean, and both platform rows still
# compile. The rows resolve different transitives and run different codegen, so
# a change that compiles on one can fail on the other.

set -euo pipefail

cd "$(dirname "$0")/../.."

sbt --batch -no-colors \
  scalafmtCheckAll \
  "main3/compile" \
  "mainNative3/compile"
