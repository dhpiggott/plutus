#!/usr/bin/env bash

# What the Probe workflow runs. A branch that needs evidence from a real macOS
# build replaces the body below with its question and dispatches Probe against
# itself; this default just links and starts the native binary, which is the
# step a Linux container can never take.

set -euo pipefail

cd "$(dirname "$0")/../.."

sbt --batch --no-colors "mainNative3/run --help"
