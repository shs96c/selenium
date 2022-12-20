#!/usr/bin/env bash
set -eo pipefail

SDK="$(xcrun --sdk macosx --show-sdk-path)"
ROOT=third_party/cpp/macos-sysroot

mkdir -p $ROOT
cp $SDK/usr/lib/libiconv.* $ROOT

mkdir -p $ROOT/Security.framework
cp $SDK/System/Library/Frameworks/Security.framework/Security.tbd $ROOT/Security.framework
