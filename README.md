# MyMidiCC

Private-use Android app for a user-supplied MIDI-CC HTML page. The app contains an **offline copy** and updates HTML/CSS/JS from this public GitHub repository without requiring a new APK on every edit.

- Android app title: **MyMidiCC**
- Original **nuspective music** icon from the user's MidiMapper repository
- Packaged local site under `web/`, served by Android WebViewAssetLoader
- Automatic HTTPS update checks at startup, downloaded files verified by SHA-256 and staged before activation
- GitHub Actions builds an installable **debug APK**, no Play Store required
- No secret signing key in this public repository

## Current status

**Original HTML imported:** `web/index.html` now contains the complete user-uploaded *MIDI-CC Nachschlagewerk* with 16 devices, search, CC pairs and NRPN entries. The HTML/CSS/JS is self-contained and the reference tables work offline. Manufacturer links open the browser and require internet. Native MIDI is not required for this read-only reference; Android hardware testing remains open.

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

### Roland JD-08

The JD-08 entry is based on Roland's official [MIDI Implementation Chart (Part)](https://static.roland.com/manuals/jd-08_reference/eng/17812001.html) and [MIDI Settings](https://static.roland.com/manuals/jd-08_reference/eng/17812003.html). The chart lists 81 received part CCs plus six received channel-mode commands (87 rows). Transmit-only Tone Palette CC 68–71 are intentionally excluded. The firmware's System MIDI channel does not recognize CC; send to the MIDI channel selected for Part A or B. The JD-08 does not document CC pairs/NRPN in this MIDI chart.
