#!/usr/bin/env bash

# Temporary probe for dhpiggott/plutus#82: builds sn-bindgen with crash
# recovery disabled in code (dhpiggott/sn-bindgen,
# disable-libclang-crash-recovery) and compares it with the released 0.4.5
# binary, which is built from the same Scala Native version. Restore the
# default probe before merging.

set -uo pipefail

iterations=1000
parallelism=4

work=$(mktemp -d)

git clone -q --depth 1 --branch disable-libclang-crash-recovery \
  https://github.com/dhpiggott/sn-bindgen "$work/sn-bindgen"
(
  cd "$work/sn-bindgen" &&
    git log --oneline -1 &&
    LLVM_BIN=/opt/homebrew/opt/llvm@17/bin SCALANATIVE_MODE=release-fast \
      sbt --batch --no-colors buildBinary 2>&1 | tail -5
) || exit 1
patched=$work/sn-bindgen/bin/bindgen
test -x "$patched" || { echo "patched binary was not built"; exit 1; }

released=$work/released.exe
curl -sSfL -o "$released" \
  "https://repo1.maven.org/maven2/com/indoorvivants/bindgen_native0.5_3/0.4.5/bindgen_native0.5_3-0.4.5-aarch64-osx.exe"
chmod +x "$released"

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

run_one() {
  local out=$2/$3
  mkdir -p "$out"
  "$1" --header "$header" --package macos --c-import CoreFoundation/CFString.h \
    --info --scala --flavour scala-native05 --clang-path /usr/bin/clang \
    --out "$out/macos.scala" --print-files >"$out/stdout" 2>"$out/stderr"
  echo $? >"$out/code"
  if [ -s "$out/macos.scala" ]; then wc -c <"$out/macos.scala" >"$out/size"; fi
  rm -f "$out/macos.scala"
}
export -f run_one
export header

row() {
  local name=$1 bin=$2
  local dir=$work/$name
  mkdir -p "$dir"
  local start=$(date +%s)
  seq 1 "$iterations" |
    SCALANATIVE_GC_LOG_LEVEL=info xargs -P "$parallelism" -I{} bash -c "run_one $bin $dir {}"
  local end=$(date +%s)
  local collections
  collections=$(cat "$dir"/*/stderr | grep -c "GC collection started")
  echo "=== $name: exit codes [$(cat "$dir"/*/code | sort | uniq -c | tr -s ' ' | tr '\n' ',')]" \
    "$collections GC collections; output sizes [$(cat "$dir"/*/size 2>/dev/null | sort | uniq -c | tr -s ' ' | tr '\n' ',')]" \
    "$((end - start))s"
}

row released-0.4.5 "$released"
row patched "$patched"
