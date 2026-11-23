# MyMidiCC – Android und Updates

## Architektur

Die App enthält eine von der APK **lokal installierte Version** sämtlicher Dateien aus \`web/\`. Ein Android-WebView lädt die Dateien über die sichere, lokale WebViewAssetLoader-Origin \`https://appassets.androidplatform.net/web/\`.

Bei jedem App-Start wird über **HTTPS** der öffentliche GitHub-Raw-Endpunkt für \`web/manifest.json\` geprüft. Bei Änderungen werden ausschließlich die dort aufgelisteten Dateien (\`web/**\`) heruntergeladen, SHA-256-verifiziert, vollständig in ein Staging-Verzeichnis geschrieben und erst dann atomar als aktueller Satz aktiviert. Ein unterbrochener Download oder eine fehlende Internetverbindung lässt die vorherige Offline-Version unverändert. Die WebView nutzt den gleichen Origin über Updates hinweg, sodass \`localStorage\` und IndexedDB erhalten bleiben.

Die App stellt **keine JavaScript-Bridge in das Android-System** zur Verfügung. Externe Web-Links öffnen den Browser. Es ist **keine native Android-MIDI-Anbindung** enthalten; insbesondere kann eine HTML-Datei, die \`navigator.requestMIDIAccess()\` voraussetzt, in Android-WebView ohne zusätzliche native Implementierung nicht einfach als funktionierend angenommen werden.

### HTML aktualisieren

1. Originaldatei nach \`web/index.html\` kopieren. Relative CSS-, JS-, Bilddateien unter \`web/\` hinzufügen.
2. Einchecken und nach \`main\` pushen.
3. GitHub Actions aktualisiert automatisch \`web/manifest.json\` und baut optional eine neue APK. Für reine Webänderungen muss man sie **nicht** neu installieren.
4. Mit Internetverbindung MyMidiCC öffnen: Die App lädt die neue Webversion. Ohne Internet bleibt die vorherige Version verfügbar.

**Achtung:** Die öffentliche Repository-Version macht die in \`web/\` eingecheckten Dateien für alle sichtbar. Keine persönlichen Daten, Tokens, Schlüssel oder vertrauliche Controller-Presets hochladen. Externe CDN-Abhängigkeiten machen Teile der Website offline eventuell unbenutzbar.

### Android APK testen

- In **Actions → Build MyMidiCC Android APK** einen erfolgreichen Lauf öffnen.
- Unter **Artifacts** \`MyMidiCC-Android-debug-APK\` herunterladen, ZIP entpacken und \`app-debug.apk\` am eigenen Android-Telefon installieren. Erforderlichenfalls Installation aus dieser Quelle erlauben.
- Die Debug-APK hat die App-ID \`music.nuspective.mymidicc\`. Der Debug-Signaturschlüssel ist bei GitHub-Runnern nicht dauerhaft gleich. Zum späteren **APK-Upgrade ohne Neuinstallation** brauchen wir daher eine dauerhaft gesicherte **private Release-Keystore** und einen signierten Release-Workflow. Niemals die Keystore in ein öffentliches Repository einchecken. Für reine HTML-Updates ist keine neue APK erforderlich.

### Noch offen

Die originale `index.html` wurde aus dem Chat-Upload nach `web/index.html` übernommen (MIDI-CC-Nachschlagewerk mit 15 Geräten, Filter/Suche, CC-Paaren und NRPN). HTML, CSS und JavaScript sind in einer Datei enthalten und offline nutzbar. Externe Herstellerlinks benötigen Internet und öffnen den Browser. Offen: Test der Darstellung, Suche und Offline-Updates auf echter Android-Hardware. iOS wird nicht mitgebaut.
