#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

cd "$(dirname "$0")/.."

if ! command -v keytool >/dev/null 2>&1; then
  echo "keytool belum tersedia. Pasang OpenJDK di Termux: pkg install openjdk-21"
  exit 1
fi
if ! command -v gh >/dev/null 2>&1; then
  echo "GitHub CLI belum tersedia. Pasang: pkg install gh"
  exit 1
fi

gh auth status >/dev/null

KEYSTORE="tbwhatsapp-release.jks"
if [ -f "$KEYSTORE" ]; then
  echo "File $KEYSTORE sudah ada. Simpan/backup file tersebut; jangan menimpanya."
  exit 1
fi

read -r -s -p "Buat password keystore (minimal 6 karakter): " STORE_PASS
echo
if [ "${#STORE_PASS}" -lt 6 ]; then
  echo "Password minimal 6 karakter."
  exit 1
fi

keytool -genkeypair -v \
  -keystore "$KEYSTORE" \
  -alias tbwhatsapp \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass "$STORE_PASS" -keypass "$STORE_PASS" \
  -dname "CN=TBWhatsApp, OU=Android, O=TBLabs, L=Indonesia, ST=Indonesia, C=ID"

# Simpan signing material sebagai GitHub Actions Secrets; tidak mencetak password.
base64 "$KEYSTORE" | tr -d '\n' | gh secret set ANDROID_KEYSTORE_BASE64
printf '%s' "$STORE_PASS" | gh secret set ANDROID_KEYSTORE_PASSWORD
gh secret set ANDROID_KEY_ALIAS --body "tbwhatsapp"
printf '%s' "$STORE_PASS" | gh secret set ANDROID_KEY_PASSWORD

unset STORE_PASS

echo
echo "GitHub Secrets signing berhasil disetel untuk repository aktif."
echo "PENTING: backup aman file $KEYSTORE. Jangan commit atau hapus file ini."
echo "Jangan bagikan file keystore atau password kepada siapa pun."
