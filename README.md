# MyMidiCC

Private-use Android app for a user-supplied MIDI-CC HTML page. The app contains an **offline copy** and updates HTML/CSS/JS from this public GitHub repository without requiring a new APK on every edit.

- Android app title: **MyMidiCC**
- Original **nuspective music** icon from the user's MidiMapper repository
- Packaged local site under `web/`, served by Android WebViewAssetLoader
- Automatic HTTPS update checks at startup, downloaded files verified by SHA-256 and staged before activation
- GitHub Actions builds an installable **debug APK**, no Play Store required
- No secret signing key in this public repository

## Current status

**Scaffold only:** The original file `C:\Users\dennis1579\Documents\ChatGPT\AnlogJoystickMidiLooper\index.html` is not yet accessible from this conversation or GitHub. Thus `web/index.html` is an **honestly marked placeholder**, not the user's MIDI-CC interface. Upload the original `index.html` and any relative assets into `web/` to complete the actual interface. The app uses no native MIDI API currently; browser-specific functions require testing and possibly a native bridge.

## Install on Android

Go to **Actions → Build MyMidiCC Android APK**, select a successful run, download the artifact **MyMidiCC-Android-debug-APK**, unpack the ZIP and install `app-debug.apk` on the phone (allow installs from this source if prompted).

**Debug signing is not stable between GitHub runners.** New debug APKs may require uninstalling an older debug installation and will lose its local data. For reliable future APK upgrades, configure a private persistent release-signing keystore as GitHub Actions secrets. HTML-only changes don't need a new APK.

## Update the site

1. Replace `web/index.html` with the original HTML file.
2. Put any relative CSS, JS, SVG, images, etc. under `web/` in matching paths. Avoid external CDNs for reliable offline usage.
3. Commit/push the web files to `main`. GitHub Actions regenerates `web/manifest.json` and builds the APK.
4. Launch MyMidiCC while connected to the internet. It checks the manifest, fetches the changed files and retains the last working site offline.

For full architecture and APK-build notes, see [Android documentation](docs/ANDROID.md).

## Naming note

The GitHub repository itself is currently named **`MyMidiCC.`** (with a trailing period), because that is the name actually created on GitHub. The Android app is named **MyMidiCC** (without a period). Its remote update URL includes that repository period. If you rename the GitHub repository, update `UPDATE_BASE` in `MainActivity.java` and rebuild the APK once.

This repository is **public**. Do not commit private presets, account details or API keys.
