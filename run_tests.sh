#!/usr/bin/env bash
if [[ "$(uname)" != "Darwin" ]]; then
  echo "SwiftUI is not available on this platform. Skipping tests." >&2
  exit 0
fi
swift test "$@"
