#!/usr/bin/env bash

# What the Probe workflow runs. A branch that needs evidence from a real macOS
# build replaces the body below with its question and dispatches Probe against
# itself; this default just links and starts the native binary, which is the
# step a Linux container can never take.
#
# Asking now: with keychain's include filters dropped, does raising jextract's
# `jextract.decls.per.header` (via JEXTRACT_JAVA_OPTIONS, which jextract's
# launcher passes to its JVM) give one class instead of a chain, and does that
# one class compile and initialise?

set -uo pipefail

cd "$(dirname "$0")/../.."

# Generates keychain's macos.h and include symlinks, and fetches jextract.
sbt --batch --no-colors keychain3/compile > /dev/null 2>&1 || echo "keychain3/compile failed"
managed=keychain/target/jvm-3/src_managed/main
ls "$managed"
jextract=$(sbt --batch --no-colors --error 'print keychain3/jextractBinary' | tail -1)
"$jextract" --version 2>&1 | head -3

d=$(mktemp -d)
cat > "$d/Init.java" <<'EOF'
public class Init {
  public static void main(String[] args) throws Exception {
    Class<?> c = Class.forName("macos.macos_h", true, Init.class.getClassLoader());
    System.out.println("loaded and initialised " + c.getName() + ", superclass " + c.getSuperclass().getName()
        + ", " + c.getDeclaredMethods().length + " declared methods");
    Object t = c.getMethod("kCFBooleanTrue").invoke(null);
    System.out.println("kCFBooleanTrue() = " + t);
    System.out.println("CFRelease declared on " + c.getMethod("CFRelease", java.lang.foreign.MemorySegment.class).getDeclaringClass().getName());
  }
}
EOF

for limit in default 100000; do
  out="$d/$limit"
  echo "== decls.per.header=$limit"
  if [ "$limit" = default ]; then unset JEXTRACT_JAVA_OPTIONS; else export JEXTRACT_JAVA_OPTIONS="-Djextract.decls.per.header=$limit"; fi
  start=$(date +%s)
  "$jextract" --output "$out/src" --target-package macos \
    -D '__is_target_environment(x)=0' -I "$managed/include" \
    -l :/System/Library/Frameworks/CoreFoundation.framework/CoreFoundation \
    -l :/System/Library/Frameworks/Security.framework/Security \
    "$managed/macos.h" 2>&1 | tail -5
  echo "jextract exit=${PIPESTATUS[0]} in $(( $(date +%s) - start ))s"
  echo "files: $(find "$out/src" -name '*.java' | wc -l)"
  echo "header classes:"
  for f in "$out"/src/macos/macos_h*.java; do
    echo "  $(basename "$f"): $(wc -c < "$f") bytes, $(grep -c 'public static' "$f") public statics, $(grep -m1 -E '^(public )?class' "$f")"
  done
  mkdir -p "$out/classes"
  start=$(date +%s)
  javac -nowarn -d "$out/classes" $(find "$out/src" -name '*.java') 2>&1 | tail -15
  echo "javac exit=${PIPESTATUS[0]} in $(( $(date +%s) - start ))s"
  ls -l "$out"/classes/macos/macos_h*.class 2>/dev/null | awk '{print "  " $5 " " $9}'
  javac -d "$out/classes" -cp "$out/classes" "$d/Init.java" 2>&1 | tail -5
  java --enable-native-access=ALL-UNNAMED -cp "$out/classes" Init 2>&1 | tail -8
done
