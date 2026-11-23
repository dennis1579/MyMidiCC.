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

Die originale `index.html` wurde aus dem Chat-Upload nach `web/index.html` übernommen (MIDI-CC-Nachschlagewerk mit 17 Geräten, Filter/Suche, CC-Paaren und NRPN). HTML, CSS und JavaScript sind in einer Datei enthalten und offline nutzbar. Externe Herstellerlinks benötigen Internet und öffnen den Browser. Offen: Test der Darstellung, Suche und Offline-Updates auf echter Android-Hardware. iOS wird nicht mitgebaut.

### JD-08 (Roland, 23.11.2026)

Nachtrag zum MIDI-CC-Nachschlagewerk anhand der offiziellen Roland-Referenz: 81 empfangbare Part-CCs und sechs zusätzliche empfangbare Kanalmodus-Kommandos; Tone Palette CC 68–71 sind lediglich TX (kein Empfang) und fehlen deshalb bewusst als auswählbare Ziel-CCs. Wichtig: CC 3 (TVF Cutoff), CC 9 (TVF Resonance), Part A/B Kanäle statt System-CH. Keine NRPN oder 14-Bit-Paare im Chart dokumentiert. Originalquellen direkt im Gerätedatensatz verlinkt. Test: `python3 tests/test_jd08_reference.py`; Android-Build über GitHub Actions. Praxistest an realem JD-08 bleibt offen.

### Bassline DB-01 (Erica Synths, 23.11.2026)

Offizielles [Handbuch](https://www.ericasynths.lv/media/BASSLINE_DB-01_manual.pdf) und Erica-Synths-Firmwarehinweise geprüft: MIDI-IN-Noten, Clock, Modulation Wheel (CC 1) und Pitch Bend. Als CC-Ziel wird nur **CC 1** aufgeführt, ohne im Herstellerhandbuch unbelegte Regler-CCs zu erfinden. **Filter-Cutoff-Offset als Wirkung von CC 1 ist nur durch Anwenderberichte gestützt**, nicht durch eine explizite Hersteller-CC-Tabelle. Pitch Bend ist kein CC; ab FW 1.07 gibt es Pattern-Wechsel über Program Change (ebenfalls kein CC). Kein bestätigtes NRPN-/14-Bit-Paar-Mapping. Quellen und Konfigurationshinweise in der Geräteansicht. Host-Regressionstest: `python3 tests/test_db01_reference.py`. Offene Prüfung: Verhalten am realen Gerät sowie Android-HTML-Update.
