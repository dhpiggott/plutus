#!/usr/bin/env bash

# Temporary: regenerates both native rows' Scala bindings from clean through
# sbt, so through the wrapper build.sbt installs, N times in one sbt session.

set -euo pipefail

cd "$(dirname "$0")/../.."

iterations=$1

commands=("show keychainNative3/bindgenBinary" "show porcupineNative3/bindgenBinary")
for i in $(seq 1 "$iterations"); do
  commands+=(
    "keychainNative3/clean" "keychainNative3/bindgenGenerateScalaSources"
    "porcupineNative3/clean" "porcupineNative3/bindgenGenerateScalaSources"
  )
done

sbt --batch -no-colors "${commands[@]}" 2>&1 |
  grep -E "Successfully regenerated|FAILED|Unrecoverable|signal|error|bindgen-without-crash-recovery" |
  grep -v "is not supported"
echo "=== sbt loop finished"
cat keychain/target/native-3/bindgen-without-crash-recovery

.github/scripts/verify.sh
