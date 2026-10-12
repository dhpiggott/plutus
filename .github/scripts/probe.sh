#!/usr/bin/env bash

# Probe for #81: how much does scala-java-time-tzdb add to the binary the
# Homebrew formula builds (nativeLinkReleaseFull)?

set -euo pipefail

cd "$(dirname "$0")/../.."

echo "::group::Release link with tzdb"
sbt --batch --no-colors -J-Xmx4G mainNative3/nativeLinkReleaseFull
echo "::endgroup::"
binary=$(find main/target/native-3 -type f -name 'plutus.Plutus' | head -1)
with=$(stat -f %z "$binary")
env -u TZ TZ=Europe/London "$binary" --help >/dev/null && echo "--help ok"

echo "::group::Release link without tzdb"
sed -i '' '/scala-java-time-tzdb/d' build.sbt
sbt --batch --no-colors -J-Xmx4G mainNative3/nativeLinkReleaseFull
git checkout build.sbt
echo "::endgroup::"
without=$(stat -f %z "$binary")

echo "plutus.Plutus (nativeLinkReleaseFull) with tzdb: $with bytes, without: $without bytes, difference: $((with - without)) bytes"
