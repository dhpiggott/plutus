#!/usr/bin/env bash

# What the Probe workflow runs. A branch that needs evidence from a real macOS
# build replaces the body below with its question and dispatches Probe against
# itself; this default just links and starts the native binary, which is the
# step a Linux container can never take.
#
# Asking now (third round): every TARGET_OS_* value, and
# DYNAMIC_TARGETS_ENABLED, that jextract's libclang 13 ends up with when it
# parses the default SDK's TargetConditionals.h, with and without the -D.

set -uo pipefail

cd "$(dirname "$0")/../.."

tc="$(xcrun --show-sdk-path)/usr/include/TargetConditionals.h"
jextract=$(sbt --batch --no-colors --error 'print keychain3/jextractBinary' | tail -1)
d=$(mktemp -d)
printf '#include "%s"\nint probe_marker(void);\n' "$tc" > "$d/tc.h"
for variant in plain withD; do
  if [ $variant = withD ]; then extra=(-D '__is_target_environment(x)=0'); else extra=(); fi
  echo "== jextract $variant"
  "$jextract" ${extra[@]+"${extra[@]}"} --output "$d/$variant" "$d/tc.h" 2>&1 | head -20
  grep -rh -E "static final int (TARGET_OS_[A-Z_]+|DYNAMIC_TARGETS_ENABLED) =" "$d/$variant" | sed -E 's/.*int ([A-Z_]+) = \(int\)([0-9]+)L;/\1=\2/' | sort
done
