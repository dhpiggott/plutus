#!/usr/bin/env bash

# PROBE (dhpiggott/plutus#78): can --from-csv-zone's default be resolved
# eagerly, as a plain ZoneId, on both rows? This branch makes it eager and
# prints, from inside --help, what ZoneId.systemDefault and friends actually
# return. Not for merging.

set -uo pipefail

cd "$(dirname "$0")/../.."

zones=("" "Europe/London")

probe_line() {
  tr -s ' \n' ' ' | sed -n 's/.*\(PROBE.*\)A named region.*/\1/p'
}

echo "=== Runner's own zone"
ls -l /etc/localtime || true
date

echo "=== java.time on the Native classpath"
sbt --batch --no-colors "export mainNative3/Compile/fullClasspath" |
  tail -1 | tr ':' '\n' | grep -iE 'time|tzdb|javalib' || true

echo "=== Linking the native binary"
sbt --batch --no-colors "show mainNative3/nativeLink" | tee link.log | tail -5
bin=$(grep -oE '/[^ ]*main/target/native-3/[^ ]+' link.log | tail -1)
echo "binary: $bin"

for tz in "${zones[@]}"; do
  echo "=== Native, TZ='${tz}'"
  if [ -z "$tz" ]; then out=$(env -u TZ "$bin" transactions --help 2>&1); rc=$?
  else out=$(TZ="$tz" "$bin" transactions --help 2>&1); rc=$?; fi
  echo "exit $rc"
  echo "$out" | probe_line
  [ $rc -gt 0 ] && echo "$out" | tail -20
done

echo "=== Native, with the runner's system zone set to Europe/London"
sudo systemsetup -settimezone Europe/London 2>&1 || sudo ln -sf /var/db/timezone/zoneinfo/Europe/London /etc/localtime
ls -l /etc/localtime; date
out=$(env -u TZ "$bin" transactions --help 2>&1); echo "exit $?"; echo "$out" | probe_line

echo "=== Native, default zone used by a real run (missing file expected)"
TZ=Europe/London "$bin" transactions --from-csv acc_probe=/nonexistent.csv \
  --to-ofx=probe.ofx --dry-run 2>&1 | tail -5
echo "exit $?"

echo "=== Native, --from-csv-zone Europe/London"
"$bin" transactions --from-csv acc_probe=/nonexistent.csv \
  --from-csv-zone Europe/London --to-ofx=probe.ofx --dry-run 2>&1 | tail -5

echo "=== JVM, system zone Europe/London, TZ unset"
out=$(env -u TZ sbt --batch --no-colors "main3/run transactions --help" 2>&1); echo "exit $?"; echo "$out" | probe_line

for tz in "${zones[@]}"; do
  echo "=== JVM, TZ='${tz}'"
  if [ -z "$tz" ]; then out=$(env -u TZ sbt --batch --no-colors "main3/run transactions --help" 2>&1); rc=$?
  else out=$(TZ="$tz" sbt --batch --no-colors "main3/run transactions --help" 2>&1); rc=$?; fi
  echo "exit $rc"
  echo "$out" | probe_line
done

echo "=== done"
