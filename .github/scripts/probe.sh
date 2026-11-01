#!/usr/bin/env bash

# What the Probe workflow runs. A branch that needs evidence from a real macOS
# build replaces the body below with its question and dispatches Probe against
# itself; this default just links and starts the native binary, which is the
# step a Linux container can never take.

set -euo pipefail

cd "$(dirname "$0")/../.."

# Temporary: applies DefinitionOrder and scalafmt on a Mac and pushes the
# result back to this branch, since nothing builds in the cloud session.
sbt --batch --no-colors scalafmtAll scalafixAll scalafmtAll || true
git --no-pager diff --stat
status=0
.github/scripts/verify.sh || status=$?
git config user.name "github-actions[bot]"
git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
git commit -am "Apply DefinitionOrder and scalafmt (probe, verify exited $status)"
git push origin "HEAD:$GITHUB_REF_NAME"
exit "$status"
