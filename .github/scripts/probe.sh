#!/usr/bin/env bash

# What the Probe workflow runs. A branch that needs evidence from a real macOS
# build replaces the body below with its question and dispatches Probe against
# itself; this default just links and starts the native binary, which is the
# step a Linux container can never take.
#
# Asking now: what exactly does TargetConditionals.h's KernelKit guard say, how
# do Apple's clang and jextract's bundled libclang answer
# `__is_target_environment(kernelkit)` for arm64-apple-darwin, and does
# `-D '__is_target_environment(x)=0'` change jextract's outcome?

set -uo pipefail

cd "$(dirname "$0")/../.."

sdk=$(xcrun --show-sdk-path)
tc="$sdk/usr/include/TargetConditionals.h"
echo "== SDK: $sdk"
xcrun --show-sdk-version
echo "== Apple clang"
clang --version

echo "== TargetConditionals.h: lines mentioning the guard"
grep -n -i -E "kernelkit|is_target_environment|define.target.os.macros|#error" "$tc"

echo "== TargetConditionals.h: context around each kernelkit mention"
grep -n -i kernelkit "$tc" | cut -d: -f1 | while read -r n; do
  echo "-- around line $n"
  sed -n "$((n > 15 ? n - 15 : 1)),$((n + 15))p" "$tc"
done

probe_dir=$(mktemp -d)
cat > "$probe_dir/env.h" <<'EOF'
#if __is_target_environment(kernelkit)
#warning "__is_target_environment(kernelkit) is TRUE"
#else
#warning "__is_target_environment(kernelkit) is false"
#endif
#if __is_target_environment(bogusenvironmentname)
#warning "__is_target_environment(bogusenvironmentname) is TRUE"
#else
#warning "__is_target_environment(bogusenvironmentname) is false"
#endif
#ifdef TARGET_OS_OSX
#warning "TARGET_OS_OSX predefined by the compiler"
#else
#warning "TARGET_OS_OSX not predefined by the compiler"
#endif
int probe_marker(void);
EOF

echo "== Apple clang: environment answers (default target)"
clang -fsyntax-only "$probe_dir/env.h" 2>&1
echo "== Apple clang: target-OS macros it predefines"
clang -dM -E -x c /dev/null | grep -E "TARGET_OS_" || echo "(none)"

echo "== jextract binary"
jextract=$(sbt --batch --no-colors --error 'print keychain3/jextractBinary' | tail -1)
echo "$jextract"
"$jextract" --version 2>&1 | head -5

echo "== jextract (libclang 13): environment answers"
"$jextract" --output "$probe_dir/out1" "$probe_dir/env.h" 2>&1 | head -20
echo "exit=${PIPESTATUS[0]}"

printf '#include "%s"\nint probe_marker(void);\n' "$tc" > "$probe_dir/tc.h"
echo "== jextract including TargetConditionals.h, no -D"
"$jextract" --output "$probe_dir/out2" "$probe_dir/tc.h" 2>&1 | head -20
echo "exit=${PIPESTATUS[0]}"

echo "== jextract including TargetConditionals.h, with -D"
"$jextract" -D '__is_target_environment(x)=0' --output "$probe_dir/out3" "$probe_dir/tc.h" 2>&1 | head -20
echo "exit=${PIPESTATUS[0]}"
find "$probe_dir/out3" -type f | head
