#!/usr/bin/env bash

# Probe for #81: with the zone rules read from /usr/share/zoneinfo by
# java.time.zone.TzdbZoneRulesProvider, does Host.zone resolve the machine's
# zone on Scala Native, and do dates near midnight land on the local day?

set -euo pipefail

cd "$(dirname "$0")/../.."

cat > main/src/main/scalanative/plutus/ZoneProbe.scala <<'EOF'
package plutus

import cats.effect.*

import java.time.Instant
import java.time.ZoneId
import java.time.zone.ZoneRulesProvider

object ZoneProbe extends IOApp.Simple:
  given Verbosity = Verbosity.INFO
  // 00:30 BST on 2 July, 19:30 EDT on 1 July, 05:00 IST on 2 July.
  val instant = Instant.parse("2026-07-01T23:30:00Z")
  def run = for
    started <- IO.monotonic
    zone <- Host.zone
    finished <- IO.monotonic
    _ <- IO.println(
      s"zone=$zone (${zone.getClass.getSimpleName}) in ${(finished - started).toMillis} ms " +
        s"offsetAt($instant)=${zone.getRules.getOffset(instant)} " +
        s"neutralPostDate=${neutralPostDate(instant, zone)} " +
        s"csvCreated(02/07/2026 00:30:00)=${csvCreated("02/07/2026", "00:30:00", zone)} " +
        s"csvCreated(02/01/2026 00:30:00)=${csvCreated("02/01/2026", "00:30:00", zone)}"
    )
    _ <- IO.whenA(sys.env.contains("PROBE_ZONES")):
      IO.println(
        s"${ZoneRulesProvider.getAvailableZoneIds.size} zone IDs, version ${Tzif.version}"
      ) *> IO.println(
        List(
          "Europe/London" -> "2040-07-01T12:00:00Z",
          "Europe/London" -> "2040-12-01T12:00:00Z",
          "Asia/Jerusalem" -> "2040-03-25T12:00:00Z",
          "America/Nuuk" -> "2040-07-01T12:00:00Z",
          "Australia/Sydney" -> "2040-01-01T12:00:00Z",
          "Australia/Sydney" -> "2040-07-01T12:00:00Z",
          "Europe/Kyiv" -> "2026-07-01T12:00:00Z",
          "America/Ciudad_Juarez" -> "2026-07-01T12:00:00Z"
        ).map((id, at) =>
          s"$id at $at: ${ZoneId.of(id).getRules.getOffset(Instant.parse(at))}"
        ).mkString("\n")
      )
  yield ()
EOF

echo "::group::Link ZoneProbe"
sbt --batch --no-colors \
  'set LocalProject("mainNative3") / Compile / mainClass := Some("plutus.ZoneProbe")' \
  mainNative3/nativeLink
echo "::endgroup::"
probe=$(find main/target/native-3 -type f -name 'plutus.ZoneProbe' | head -1)

ls -l /etc/localtime /usr/share/zoneinfo
ls -l "$(readlink -f /usr/share/zoneinfo)/Europe/London"
tail -c 40 /usr/share/zoneinfo/Europe/London | od -c | tail -3
cat /usr/share/zoneinfo/+VERSION || true

echo "--- TZ unset, PROBE_ZONES"
env -u TZ PROBE_ZONES=1 "$probe" || echo "exit $?"
for tz in Europe/London America/New_York UTC Asia/Kolkata Europe/Kyiv \
  :Europe/London /usr/share/zoneinfo/Europe/London \
  GMT0BST,M3.5.0/1,M10.5.0 Nowhere/Special ""; do
  echo "--- TZ=$tz"
  TZ="$tz" "$probe" || echo "exit $?"
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
