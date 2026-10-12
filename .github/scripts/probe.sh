#!/usr/bin/env bash

# Probe for #81: does Host.zone resolve the machine's zone on Scala Native, does
# a post date near midnight land on the local day, and how much does
# scala-java-time-tzdb add to the linked binary?

set -euo pipefail

cd "$(dirname "$0")/../.."

cat > main/src/main/scalanative/plutus/ZoneProbe.scala <<'EOF'
package plutus

import cats.effect.*

import java.time.Instant

object ZoneProbe extends IOApp.Simple:
  given Verbosity = Verbosity.INFO
  // 00:30 BST on 2 July, 19:30 EDT on 1 July, 05:00 IST on 2 July.
  val instant = Instant.parse("2026-07-01T23:30:00Z")
  def run = for
    zone <- Host.zone
    _ <- IO.println(
      s"zone=$zone (${zone.getClass.getSimpleName}) " +
        s"offsetAt($instant)=${zone.getRules.getOffset(instant)} " +
        s"neutralPostDate=${neutralPostDate(instant, zone)} " +
        s"csvCreated(02/07/2026 00:30:00)=${csvCreated("02/07/2026", "00:30:00", zone)} " +
        s"csvCreated(02/01/2026 00:30:00)=${csvCreated("02/01/2026", "00:30:00", zone)}"
    )
  yield ()
EOF

echo "::group::Link ZoneProbe"
sbt --batch --no-colors \
  'set mainNative3 / Compile / mainClass := Some("plutus.ZoneProbe")' \
  mainNative3/nativeLink
echo "::endgroup::"
probe=$(find main/target/native-3 -type f -name 'plutus.ZoneProbe' | head -1)
echo "ZoneProbe binary: $probe"

ls -l /etc/localtime

for tz in UNSET Europe/London America/New_York UTC Asia/Kolkata \
  :Europe/London /usr/share/zoneinfo/Europe/London \
  GMT0BST,M3.5.0/1,M10.5.0 Europe/Kyiv ""; do
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

echo "::group::Link plutus with tzdb"
sbt --batch --no-colors mainNative3/nativeLink
echo "::endgroup::"
binary=$(find main/target/native-3 -type f -name 'plutus.Plutus' | head -1)
with=$(stat -f %z "$binary")
env -u TZ "$binary" transactions --help >/dev/null && echo "transactions --help ok"

echo "::group::Link plutus without tzdb"
sed -i '' '/scala-java-time-tzdb/d' build.sbt
sbt --batch --no-colors mainNative3/nativeLink
git checkout build.sbt
echo "::endgroup::"
without=$(stat -f %z "$binary")

echo "plutus.Plutus (debug link) with tzdb: $with bytes, without: $without bytes, difference: $((with - without)) bytes"
