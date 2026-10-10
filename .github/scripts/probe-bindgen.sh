#!/usr/bin/env bash

# Temporary: runs the sn-bindgen binary against keychain's macos.h N times and
# counts crashes. Usage: probe-bindgen.sh <version> <iterations> <parallelism>
# Extra environment (LIBCLANG_*) is inherited from the caller.

set -uo pipefail

version=$1
iterations=$2
parallelism=$3

# libclang only checks these for presence, so an empty value would still count.
if [ -n "${PROBE_NOTHREADS-}" ]; then export LIBCLANG_NOTHREADS=1; fi
if [ -n "${PROBE_NOCRASHREC-}" ]; then export LIBCLANG_DISABLE_CRASH_RECOVERY=1; fi

work=$(mktemp -d)
bin=$work/bindgen.exe
curl -sSfL -o "$bin" \
  "https://repo1.maven.org/maven2/com/indoorvivants/bindgen_native0.5_3/$version/bindgen_native0.5_3-$version-aarch64-osx.exe"
chmod +x "$bin"

sdk=$(xcrun --show-sdk-path)
header=$work/macos.h
for p in \
  CoreFoundation.framework/Versions/A/Headers/CFBase.h \
  CoreFoundation.framework/Versions/A/Headers/CFNumber.h \
  CoreFoundation.framework/Versions/A/Headers/CFData.h \
  CoreFoundation.framework/Versions/A/Headers/CFDictionary.h \
  CoreFoundation.framework/Versions/A/Headers/CFString.h \
  Security.framework/Versions/A/Headers/SecBase.h \
  Security.framework/Versions/A/Headers/SecItem.h; do
  echo "#include <$sdk/System/Library/Frameworks/$p>"
done >"$header"

clang --version | head -1
sw_vers
echo "version=$version iterations=$iterations parallelism=$parallelism LIBCLANG_NOTHREADS=${LIBCLANG_NOTHREADS-} LIBCLANG_DISABLE_CRASH_RECOVERY=${LIBCLANG_DISABLE_CRASH_RECOVERY-}"

run_one() {
  local i=$1
  local out=$work/out-$i
  mkdir -p "$out"
  "$bin" --header "$header" --package macos --c-import CoreFoundation/CFString.h \
    --info --scala --flavour scala-native05 --clang-path /usr/bin/clang \
    --out "$out/macos.scala" --print-files >"$out/stdout" 2>"$out/stderr"
  local code=$?
  echo "$code" >"$out/code"
  if [ "$code" -ne 0 ]; then
    echo "--- iteration $i exited $code"
    grep -v "Skipping __\|is not supported" "$out/stderr" | tail -40
  fi
  rm -f "$out/macos.scala"
}
export -f run_one
export bin header work

start=$(date +%s)
seq 1 "$iterations" | xargs -P "$parallelism" -I{} bash -c 'run_one {}'
end=$(date +%s)

echo "=== summary: version=$version parallelism=$parallelism NOTHREADS=${LIBCLANG_NOTHREADS-} NOCRASHREC=${LIBCLANG_DISABLE_CRASH_RECOVERY-} seconds=$((end - start))"
cat "$work"/out-*/code | sort | uniq -c
