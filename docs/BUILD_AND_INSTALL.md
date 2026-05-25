# Build & Install APK — Sunmi V2s

## Wymagania (juz spelnione na tej maszynie 2026-05-25)

| Co | Sciezka |
|---|---|
| Android Studio + JBR (JDK 21) | `C:\Program Files\Android\Android Studio\jbr\` |
| Android SDK | `C:\Android\Sdk` (platform-tools, android-34, build-tools 34.0.0) |
| Gradle 8.7 | `C:\Android\Gradle\gradle-8.7\` (uzywany tylko do bootstrap wrappera) |

## Build APK (debug)

```powershell
$env:ANDROID_HOME = "C:\Android\Sdk"
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
cd "C:\Claude Code\ksef_invoicer_sunmi_apk"
.\gradlew assembleDebug
```

Output: `app\build\outputs\apk\debug\app-debug.apk` (~5.4 MB)

Cold build: ~45s. Warm rebuild po zmianie Kotlin: ~5-10s.

## Sideload na Sunmi V2s

### Krok 1: Wlacz USB debugging na V2s

1. Settings → About phone → Build number → **tap 7x** (Developer mode unlocked)
2. Settings → System → Developer options → **USB debugging** ON
3. Podlacz USB do laptopa. Na V2s pojawi sie prompt "Allow USB debugging?" → **Allow always**

### Krok 2: Verify ADB widzi V2s

```powershell
& "C:\Android\Sdk\platform-tools\adb.exe" devices
# Powinno pokazac:
# List of devices attached
# 1234567890ABCDEF    device
```

Jezeli `unauthorized` — odlacz/podlacz USB, accept prompt na V2s.

### Krok 3: Install APK

```powershell
& "C:\Android\Sdk\platform-tools\adb.exe" install -r `
  "C:\Claude Code\ksef_invoicer_sunmi_apk\app\build\outputs\apk\debug\app-debug.apk"
# Output: Performing Streamed Install... Success
```

### Krok 4: Uruchom aplikacje

Na V2s: znajdz "KSeF Invoicer POS" w app drawer i tap. Powinno otworzyc WebView
z naszej apki `invoicer.kasaflow.cloud/m/faktury/quick?app=sunmi-v2s`.

Albo z laptopa:
```powershell
& "C:\Android\Sdk\platform-tools\adb.exe" shell am start `
  -n cloud.kasaflow.ksefinvoicer/.MainActivity
```

## Test e2e

1. **Login** na V2s (admin / wpisz haslo)
2. Sprawdz banner: **"📲 Sunmi POS aktywne · V2s (xxxxxx)"**
   - jezeli widac → SunmiBridge dziala, getDeviceInfo() zwrocil model
   - jezeli nie widac → sprawdz `adb logcat -s KsefSunmi,SunmiBridge` na bridge errors
3. Tap "Skanuj" (powinno byc "🔫 Laser scan" zamiast "📷 Skanuj kod")
   - APK wysle broadcast `com.sunmi.scanner.Scan_start`
   - Sunmi scanner zaswieci laser
   - Skan kodu → SunmiScannerReceiver lapie intent → JS `window.onSunmiBarcodeScanned(code)`
   - Web fetchuje `/m/api/produkty/by-barcode/{code}` → auto-fill formularza
4. Wystaw FV → POST /m/faktury/quick → redirect do sukces.html
5. Na sukces: pojedynczy przycisk "🔫 Drukuj 58mm (wbudowana)"
   - fetch `/m/api/paragon-text/{id}?width=58`
   - APK wywoluje `SunmiBridge.printReceipt(text)`
   - AIDL `ISunmiPrinterService.printText` → drukarka 58mm wylewa paragon
   - cutPaper na koncu

## Troubleshooting

### "Sunmi POS aktywne" sie nie pokazuje

- Banner pokazuje sie tylko jak URL ma `?app=sunmi-v2s` LUB `window.SunmiBridge` dostepny
- APK powinno automatycznie dodawac `?app=sunmi-v2s` (zakodowane w MainActivity.WEB_URL)
- Jezeli nie widac → sprawdz w Chrome DevTools (przez `chrome://inspect/#devices` na laptopie):
  - WebView musi miec `setWebContentsDebuggingEnabled(true)` — domyslnie dla debug build
  - `typeof window.SunmiBridge` w console → powinno byc "object"

### Laser nie skanuje

- Sprawdz pozwolenie: Settings → Apps → KSeF Invoicer POS → Permissions → wszystko enabled
- Logcat: `adb logcat -s ScannerRx,SunmiBridge` — czy widac broadcast
- Niektore firmware uzywa `com.summi.scanner.*` (literowka) — manifest juz lapie oba

### Drukarka nie drukuje

- Sprawdz: Settings → Apps → KSeF Invoicer POS → Permissions → "com.sunmi.permission.PRINTER" granted
- Logcat: `adb logcat -s SunmiPrinter` — czy widac "Sunmi Printer Service connected"
- Jezeli `STUB print (no Sunmi)` → APK nie ma dostepu do system service. Restart V2s.

### Polskie znaki (ą,ę,ł,ó,ś...) sie nie drukuja

- V2s firmware 7.1 native UTF-8 — powinno dzialac
- Jezeli nie: w `SunmiPrinterManager.printPlainText` zmien font na `printTextWithFont(text, "Default", 24f, null)`

## Aktualizacje webowej

**Webowa aplikacja `ksef_invoicer` jest deploymentowana na VPS osobno** (Docker
compose na 91.108.120.205, Caddy reverse proxy do invoicer.kasaflow.cloud).
Zmiany w `app/templates/mobile/*` lub `app/routers/mobile.py` → push to GitHub
→ rebuild Docker image → restart. **APK nie wymaga update!** WebView ladowne URL
zawsze zwraca najnowsza wersje.

## Update APK

Tylko gdy zmiana w Kotlin/AIDL/Manifest:
1. `cd "C:\Claude Code\ksef_invoicer_sunmi_apk"`
2. Edit kod
3. `.\gradlew assembleDebug` (lub `assembleRelease` dla signed)
4. `adb install -r app\build\outputs\apk\debug\app-debug.apk`

Wersja w `app/build.gradle.kts`: `versionCode = 1`, `versionName = "0.1.0-alpha"`.
Bumpuj przy kazdym release.
