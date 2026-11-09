# TBWhatsApp V2.0.0

TBWhatsApp adalah aplikasi Android messenger mandiri (independent messenger prototype). Aplikasi ini bukan modifikasi APK WhatsApp resmi, tidak terafiliasi dengan WhatsApp/Meta, dan tidak dapat masuk ke akun WhatsApp resmi.

## Fitur V2 yang tersedia secara lokal

- Lima tab: Chat, Update, Panggilan, Komunitas, Setelan.
- Percakapan lokal: buat chat pribadi/grup, kirim pesan teks, emoji, hapus pesan, salin dan bagikan pesan.
- Penyimpanan lokal permanen menggunakan SharedPreferences + JSON.
- Pencarian percakapan dan filter Semua, Belum dibaca, Favorit, Grup, Diarsipkan.
- Pin, favorit, mute, arsip, hapus, dan ekspor percakapan.
- Status teks lokal: buat, lihat, dan hapus.
- Komunitas lokal: buat dan hapus ruang komunitas contoh.
- Riwayat panggilan contoh; tombol panggilan tidak memulai panggilan sungguhan.
- Tema gelap, tampilan ringkas, sembunyikan pratinjau, preferensi tanda baca lokal.
- Kunci aplikasi dengan PIN lokal berbasis hash PBKDF2 + salt.
- Edit profil lokal, ekspor data melalui Android Share Sheet, dan reset data demo.

## Batas fungsi saat ini

Pesan, status, komunitas, dan profil hanya tersimpan di perangkat yang sama. Belum ada akun online, server, pengiriman pesan antarpengguna, sinkronisasi, enkripsi end-to-end, panggilan suara/video, push notification, atau pengiriman media. Tombol dan label demo tidak boleh dianggap sebagai layanan yang sudah aktif.

## Persyaratan build

- Android Gradle Plugin 8.13.2
- Gradle 8.13
- JDK 17
- Android SDK 36
- minSdk 23 (Android 6.0)

## Build APK lewat GitHub Actions

1. Ekstrak proyek dan push seluruh isi ke repository GitHub.
2. Buka tab **Actions**.
3. Workflow **TBWhatsApp V2 - Build APK** berjalan otomatis saat push ke `main` atau `master`.
4. Unduh artifact `TBWhatsApp-V2-debug` untuk APK debug.

## Membuat signed release APK

Release APK harus ditandatangani dengan keystore milik pembuat aplikasi. Dari Termux, setelah berada di root repository dan sudah login `gh`, jalankan:

```bash
bash scripts/setup-release-signing.sh
```

Skrip membuat `tbwhatsapp-release.jks` dan mengunggah empat GitHub Actions Secrets ke repository aktif: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`.

Setelah secrets tersedia, push commit baru atau jalankan workflow secara manual. Workflow akan menghasilkan artifact `TBWhatsApp-V2-release-signed`.

Untuk menerbitkan GitHub Release, buat dan push tag versi, misalnya:

```bash
git tag v2.0.0
git push origin v2.0.0
```

Jika signing secrets tersedia, APK signed akan dilampirkan pada GitHub Release untuk tag tersebut.

**Keystore wajib dibackup.** Jika file keystore/password hilang, pembaruan aplikasi yang menggunakan applicationId dan sertifikat yang sama tidak dapat ditandatangani dengan identitas lama. Jangan masukkan keystore atau password ke Git.

## Perintah Termux

```bash
cd ~/projects/TBWhatsApp
git add .
git commit -m "TBWhatsApp V2.0.0"
git push origin main
```

Jangan menjalankan `git init` berulang kali jika repository sudah ada.
