#!/usr/bin/env bash

# Installs the Homebrew packages the build needs. Intended for a fresh macOS
# GitHub runner; safe to run on a developer machine, where it is a no-op.

set -euo pipefail

# llvm@17 specifically, not llvm: the sn-bindgen binary the plugin downloads
# has /opt/homebrew/opt/llvm@17/lib/libclang.dylib baked in as an absolute
# install name, so any other LLVM leaves it aborting with SIGABRT (exit 134)
# before it prints anything useful.
#
# s2n is pulled in by epollcat for TLS on the Scala Native row, and the build
# links against /opt/homebrew/lib to find it. cmake, ninja and pkg-config are
# what sbt-vcpkg-native shells out to when it builds sqlite3 from source.
packages=(llvm@17 s2n cmake ninja pkg-config)

# GITHUB_ACTIONS= is about the channel, not the output. Homebrew promotes
# every warning it prints to a `::warning::` when it sees that variable set, so
# the two it prints on every fresh runner arrive as annotations on the job,
# where they read as though the build found something. Neither is ours to fix:
# nothing here wants llvm@17 linked over the runner image's llvm@20 (sn-bindgen
# opens it by absolute path, linked or not), and the untrusted-tap warning is
# Homebrew reporting that it is ignoring a tap the image ships and this build
# never uses. Cleared, the same text still prints as `Warning:` in the log. The
# only checks the variable otherwise suppresses are check_xcode_up_to_date and
# check_clt_up_to_date, which run when a package builds from source rather than
# from a bottle.
GITHUB_ACTIONS= HOMEBREW_NO_AUTO_UPDATE=1 HOMEBREW_NO_INSTALL_CLEANUP=1 \
  brew install "${packages[@]}"

# Both FFI toolchains generate their headers against the SDK path this prints,
# so a runner without the command line tools has to fail here rather than
# halfway through codegen.
echo "macOS SDK: $(xcrun --show-sdk-path)"
