#!/usr/bin/env bash

# Temporary probe for dhpiggott/plutus#82: does sn-bindgen's crash rate follow
# how often its GC runs? Restore the default probe before merging.

set -uo pipefail

iterations=1000
parallelism=4

work=$(mktemp -d)
bin=$work/bindgen.exe
curl -sSfL -o "$bin" \
  "https://repo1.maven.org/maven2/com/indoorvivants/bindgen_native0.5_3/0.4.4/bindgen_native0.5_3-0.4.4-aarch64-osx.exe"
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

run_one() {
  local out=$1/$2
  mkdir -p "$out"
  "$bin" --header "$header" --package macos --c-import CoreFoundation/CFString.h \
    --info --scala --flavour scala-native05 --clang-path /usr/bin/clang \
    --out "$out/macos.scala" --print-files >"$out/stdout" 2>"$out/stderr"
  echo $? >"$out/code"
  rm -f "$out/macos.scala"
}
export -f run_one
export bin header

# Each row: a name, then the environment to run the binary under.
row() {
  local name=$1
  shift
  local dir=$work/$name
  mkdir -p "$dir"
  local start=$(date +%s)
  seq 1 "$iterations" | env "$@" xargs -P "$parallelism" -I{} bash -c "run_one $dir {}"
  local end=$(date +%s)
  local runs crashed collections crashedWithOpenGc
  runs=$(ls "$dir" | wc -l | tr -d ' ')
  crashed=$(grep -lv '^0$' "$dir"/*/code | wc -l | tr -d ' ')
  collections=$(cat "$dir"/*/stderr | grep -c "GC collection started")
  # A crashed run whose last GC line is a collection start with no matching
  # finish crashed mid-collection.
  crashedWithOpenGc=0
  for code in $(grep -lv '^0$' "$dir"/*/code); do
    last=$(grep -E "GC collection (started|finished)" "$(dirname "$code")/stderr" | tail -1)
    case $last in *started*) crashedWithOpenGc=$((crashedWithOpenGc + 1)) ;; esac
  done
  echo "=== $name: $crashed of $runs crashed; $collections GC collections in total" \
    "($(awk "BEGIN{printf \"%.2f\", $collections/$runs}") per run);" \
    "$crashedWithOpenGc crashes mid-collection; $((end - start))s"
  echo "    exit codes: $(cat "$dir"/*/code | sort | uniq -c | tr -s ' ' | tr '\n' ',')"
  local example
  example=$(grep -lv '^0$' "$dir"/*/code | head -1)
  if [ -n "$example" ]; then
    echo "    last stderr lines of one crashed run:"
    grep -v "is not supported" "$(dirname "$example")/stderr" | tail -6 | sed 's/^/      /'
  fi
}

clang --version | head -1
row baseline-quiet
row baseline SCALANATIVE_GC_LOG_LEVEL=info
row big-heap SCALANATIVE_GC_LOG_LEVEL=info GC_INITIAL_HEAP_SIZE=2g
row small-heap SCALANATIVE_GC_LOG_LEVEL=info GC_MAXIMUM_HEAP_SIZE=32m
row small-heap-no-crash-recovery SCALANATIVE_GC_LOG_LEVEL=info GC_MAXIMUM_HEAP_SIZE=32m LIBCLANG_DISABLE_CRASH_RECOVERY=1
