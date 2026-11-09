# TBWhatsApp — Android UI Prototype

TBWhatsApp is an independent Android messenger-style prototype. It is **not affiliated with, endorsed by, or a modified build of WhatsApp/Meta**.

## Current prototype features

- Chat list with local sample conversations.
- Search and chat filters (all, unread, favorites, groups).
- Conversation screen with local in-memory message bubbles and a working demo send button.
- Updates/status and call-history demo screens.
- Dark mode, compact chat rows, and message-preview privacy setting saved locally.
- GitHub Actions workflow that builds and uploads a debug APK.

## Important limitations

This version is an offline UI prototype. Messages are stored only in memory and disappear when the app process is restarted. It does not connect to WhatsApp servers, send messages to WhatsApp users, clone the official WhatsApp app, or access WhatsApp databases. Real messaging requires a separately designed backend and user-consented accounts. Never put API secrets or service-account keys inside the APK.

## Build on GitHub

1. Create an empty GitHub repository named `TBWhatsApp`.
2. Upload/push this project to the repository's `main` branch.
3. Open the repository's **Actions** tab.
4. Select **Build TBWhatsApp APK** and wait for the workflow to finish.
5. Open the completed workflow run and download the `TBWhatsApp-debug-apk` artifact.

The workflow uses Java 17, Gradle 8.13, and Android Gradle Plugin 8.13.2.

## Termux workflow

Termux is used to edit the source and push it to GitHub; GitHub's Linux runner performs the Android SDK build.

```bash
pkg update -y
pkg install git gh nano unzip -y
mkdir -p ~/projects
cd ~/projects
unzip ~/storage/downloads/TBWhatsApp.zip
cd TBWhatsApp

git init
git add .
git commit -m "Initial TBWhatsApp project"
git branch -M main
```

Create an empty GitHub repo, then connect it (replace the URL with your own repository URL):

```bash
git remote add origin https://github.com/USERNAME/TBWhatsApp.git
git push -u origin main
```

After later edits:

```bash
git add .
git commit -m "Update TBWhatsApp"
git push
```

## Package information

- Application ID: `com.tblabs.tbwhatsapp`
- Minimum Android: Android 6.0 (API 23)
- Compile SDK: 36
- Version: 1.0.0
