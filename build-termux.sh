#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

if ! command -v gradle >/dev/null 2>&1; then
  echo "Gradle belum terpasang. Build APK utama dilakukan lewat GitHub Actions."
  echo "Termux digunakan untuk mengedit kode dan push ke GitHub."
  exit 1
fi

gradle :app:assembleDebug --no-daemon
printf '\nAPK: app/build/outputs/apk/debug/app-debug.apk\n'
