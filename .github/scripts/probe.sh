#!/usr/bin/env bash

# What the Probe workflow runs. A branch that needs evidence from a real macOS
# build replaces the body below with its question and dispatches Probe against
# itself; this default just links and starts the native binary, which is the
# step a Linux container can never take.
#
# Asking now (second round): what does each SDK on the runner say around
# `__is_target_environment` in TargetConditionals.h, and which TARGET_OS_*
# values does jextract's libclang 13 end up with, with and without the -D?

set -uo pipefail

cd "$(dirname "$0")/../.."

echo "== SDKs on the runner"
ls -d /Library/Developer/CommandLineTools/SDKs/* /Applications/Xcode*.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/* 2>/dev/null
echo "== default: $(xcrun --show-sdk-path) ($(xcrun --show-sdk-version))"

echo "== kernelkit mentions in any SDK's TargetConditionals.h"
for f in /Library/Developer/CommandLineTools/SDKs/*/usr/include/TargetConditionals.h /Applications/Xcode*.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/*/usr/include/TargetConditionals.h; do
  [ -f "$f" ] && { echo "-- $f"; grep -n -i -E "kernelkit|exclave" "$f" || echo "(none)"; }
done

tc="$(xcrun --show-sdk-path)/usr/include/TargetConditionals.h"
echo "== default SDK TargetConditionals.h, lines 110-270"
sed -n 110,270p "$tc"

jextract=$(sbt --batch --no-colors --error 'print keychain3/jextractBinary' | tail -1)
d=$(mktemp -d)
printf '#include "%s"\nint probe_marker(void);\n' "$tc" > "$d/tc.h"
for variant in plain withD; do
  if [ $variant = withD ]; then extra=(-D '__is_target_environment(x)=0'); else extra=(); fi
  echo "== jextract $variant: diagnostics"
  "$jextract" ${extra[@]+"${extra[@]}"} --output "$d/$variant" "$d/tc.h" 2>&1 | head -20
  echo "== jextract $variant: TARGET_OS_* constants it generated"
  grep -rho -E "TARGET_OS_[A-Z_]+\(\) \{[^}]*\}|int TARGET_OS_[A-Z_]+ = [0-9]+" "$d/$variant" | sort -u | head -60
  grep -rh -A3 -E "public static int TARGET_OS_(OSX|MAC|EXCLAVECORE|EXCLAVEKIT|KERNELKIT|MACCATALYST|SIMULATOR|IPHONE)\(" "$d/$variant" | grep -E "TARGET_OS_|return" | head -40
done
