#!/usr/bin/env bash

# Probe for #81: does Host.localDate, via libc's localtime_r, give the
# machine's local calendar date on Scala Native?

set -euo pipefail

cd "$(dirname "$0")/../.."

cat > main/src/main/scalanative/plutus/ZoneProbe.scala <<'EOF'
package plutus

import java.time.Instant

object ZoneProbe:
  def main(args: Array[String]): Unit =
    val instants = List(
      "2026-07-01T23:30:00Z",
      "2026-01-01T23:30:00Z",
      "2026-10-24T23:30:00Z",
      "2040-07-01T23:30:00Z",
      "1990-07-01T23:30:00Z"
    ).map(Instant.parse)
    val started = System.nanoTime
    val dates = instants.map(Host.localDate)
    val micros = (System.nanoTime - started) / 1000 / instants.size
    println(
      instants
        .zip(dates)
        .map((instant, date) => s"$instant->$date")
        .mkString(" ") +
        s" neutralPostDate(${instants.head})=${neutralPostDate(instants.head)}" +
        s" ($micros us each)"
    )
EOF

echo "::group::Link ZoneProbe"
sbt --batch --no-colors \
  'set LocalProject("mainNative3") / Compile / mainClass := Some("plutus.ZoneProbe")' \
  mainNative3/nativeLink
echo "::endgroup::"
probe=$(find main/target/native-3 -type f -name 'plutus.ZoneProbe' | head -1)

ls -l /etc/localtime
for tz in UNSET Europe/London America/New_York UTC Asia/Kolkata Europe/Kyiv \
  :Europe/London /usr/share/zoneinfo/Europe/London \
  GMT0BST,M3.5.0/1,M10.5.0 Nowhere/Special ""; do
  echo "--- TZ=$tz"
  if [ "$tz" = UNSET ]; then
    env -u TZ "$probe" || echo "exit $?"
  else
    TZ="$tz" "$probe" || echo "exit $?"
  fi
done

original=$(readlink /etc/localtime)
for region in Europe/London America/New_York; do
  sudo ln -sf "/var/db/timezone/zoneinfo/$region" /etc/localtime
  echo "--- TZ unset, /etc/localtime -> $(readlink /etc/localtime)"
  env -u TZ "$probe" || echo "exit $?"
done
sudo ln -sf "$original" /etc/localtime
rm main/src/main/scalanative/plutus/ZoneProbe.scala

echo "::group::Link plutus"
sbt --batch --no-colors mainNative3/nativeLink
echo "::endgroup::"
binary=$(find main/target/native-3 -type f -name 'plutus.Plutus' | head -1)
env -u TZ "$binary" transactions --help >/dev/null && echo "transactions --help ok"
echo "plutus.Plutus (debug link): $(stat -f %z "$binary") bytes"
