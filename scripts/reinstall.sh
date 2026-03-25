#!/usr/bin/env bash
# Uninstalls the app (clearing all data), rebuilds, and reinstalls.
set -e

cd "$(dirname "$0")/.."

echo "Uninstalling app.defide..."
adb uninstall app.defide || true  # don't fail if not installed

echo "Building and installing..."
./gradlew installDebug

echo "Done. App installed fresh on device."
