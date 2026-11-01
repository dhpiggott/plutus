#!/usr/bin/env bash

# What the Probe workflow runs. A branch that needs evidence from a real macOS
# build replaces the body below with its question and dispatches Probe against
# itself; this default just links and starts the native binary, which is the
# step a Linux container can never take.

set -euo pipefail

cd "$(dirname "$0")/../.."

# Temporary: starts both rows after DefinitionOrder's moves, through every
# subcommand's help and the missing-book error path, to catch an
# initialisation-order break that compiling and linking can't.
missing=/tmp/does-not-exist.gnucash
for row in main3 mainNative3; do
  for args in "--help" "transactions --help" "archive-accounts --help" \
    "restore-account --help" "archive-accounts --dry-run --input $missing" \
    "restore-account --dry-run --input $missing" \
    "transactions --from-monzo --to-book --input $missing"; do
    echo "===== $row: $args"
    status=0
    sbt --batch --no-colors "$row/run $args" || status=$?
    echo "===== exit $status"
  done
done 2>&1 | grep -vE '^\[(info|success)\]|bindgen|^ {5,}' || true
