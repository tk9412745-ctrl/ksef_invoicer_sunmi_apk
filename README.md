# ksef_invoicer_sunmi_apk

Cienki WebView wrapper APK dla **Sunmi V2s** (Android 7.1, drukarka 58mm, laser scanner).
Otwiera webową aplikację `https://invoicer.kasaflow.cloud/m/faktury/quick?app=sunmi-v2s`
i daje JS bridge do natywnego Sunmi Printer Service (AIDL) + Scanner BroadcastReceiver.

## Architektura

```
┌─────────────────────────────────────────────┐
│  MainActivity (Kotlin)                       │
│  ┌──────────────────────────────────────┐   │
│  │ WebView                              │   │
│  │ → invoicer.kasaflow.cloud/m/...      │   │
│  │                                       │   │
│  │ window.SunmiBridge:                  │   │
│  │   - scanBarcode()    → laser SDK     │   │
│  │   - printReceipt(t)  → Printer SDK   │   │
│  │   - getDeviceInfo()  → Build.MODEL   │   │
│  └──────────────────────────────────────┘   │
│                                              │
│  SunmiScannerReceiver (BroadcastReceiver)   │
│  → łapie laser intent → forward do JS:      │
│    window.onSunmiBarcodeScanned(code)       │
│                                              │
│  SunmiPrinterManager (AIDL)                 │
│  → bind woyou.aidlservice.jiuiv5            │
│  → printText/printBitmap/cutPaper           │
└─────────────────────────────────────────────┘
```

## Wymagania środowiska

| Co | Wersja | Status |
|---|---|---|
| **Android Studio** | Ladybug 2024.2.2+ | Manual install (NSIS wymaga UAC — patrz niżej) |
| **JDK** | 17 (Temurin) | Bundled w AS |
| **Android SDK Platform** | 34 (compileSdk) | AS sciagnie przy pierwszym sync |
| **Build Tools** | 34.0.0 | AS sciagnie automatycznie |
| **Min device** | API 24 = Android 7.0 | Pokrywa V2/V2s/V3 |

## Pierwszy setup

### 1. Install Android Studio (manualnie, wymaga admin/UAC)

```powershell
# Installer jest pobrany w C:\Claude Code\_downloads\android-studio-installer.exe
# (1.17 GB, Ladybug 2024.2.2)

# Uruchom z UAC — wybierz "Yes" na admin prompt
& "C:\Claude Code\_downloads\android-studio-installer.exe"

# W kreatorze wybierz:
# - Standard installation
# - Theme: dowolny
# - Komponenty: Android SDK + Android Virtual Device (jak chcesz emulator)
```

### 2. Otworz projekt w AS

```
File → Open → C:\Claude Code\ksef_invoicer_sunmi_apk
```

AS automatycznie wykryje brak `gradle/wrapper/gradle-wrapper.jar` i zaproponuje
**"Gradle sync"** — kliknij. To pobierze:
- Gradle 8.5 wrapper
- Android Gradle Plugin 8.5.2
- Kotlin 1.9.24
- Wszystkie dependencies (~500 MB pierwszego razy)

### 3. Sunmi Printer SDK — opcjonalnie

W `app/build.gradle.kts` jest zakomentowana linijka:
```kotlin
// implementation("com.sunmi:printerlibrary:1.0.20")
```

Sunmi nie publikuje SDK w Maven Central. Opcje:
- **Opcja A (zalecana):** Sciagnij AAR z https://developer.sunmi.com → wrzuc do
  `app/libs/sunmi-printer-1.0.20.aar` + dodaj `implementation(fileTree("libs"))`
- **Opcja B (działa już teraz, mamy AIDL):** używamy bezpośrednio AIDL z
  `app/src/main/aidl/woyou/aidlservice/jiuiv5/` — bez SDK zewnętrznego.
  Wadą: brak high-level helpers (np. `SunmiPrintHelper`), ale w naszym use case
  wystarczy `printText` + `cutPaper`.

Obecnie projekt używa **Opcja B** — kompiluje się bez dodatkowego SDK.

### 4. Build APK (debug)

```powershell
cd "C:\Claude Code\ksef_invoicer_sunmi_apk"
.\gradlew assembleDebug

# Output:
# app\build\outputs\apk\debug\app-debug.apk  (~5 MB)
```

### 5. Sideload na Sunmi V2s

```powershell
# Włącz USB debugging na V2s:
# Settings → About → Build number 7x tap → Developer options ON
# → USB debugging ON

# Podlacz USB
adb devices

# Install
adb install -r app\build\outputs\apk\debug\app-debug.apk

# Uruchom (lub kliknij ikonę na V2s)
adb shell am start -n cloud.kasaflow.ksefinvoicer/.MainActivity
```

## Web app integration

Webowa app `ksef_invoicer` musi mieć:

- `/m/faktury/quick?app=sunmi-v2s` — quick FV form
- `/m/api/paragon-text/{id}?width=58` — zwraca JSON `{text}` dla printera
- `/m/api/produkty/by-barcode/{code}` — lookup produktu po EAN/UPC

Wszystko jest zaimplementowane w `ksef_invoicer` od Sprint 30b
(commit 8d1603d, PR #41).

JS detection w webowej:
```js
const SUNMI_ACTIVE = (typeof window.SunmiBridge !== 'undefined') ||
                     (new URLSearchParams(window.location.search).get('app') === 'sunmi-v2s');
```

JS calls do APK:
```js
window.SunmiBridge.scanBarcode();           // trigger laser
window.SunmiBridge.printReceipt(text);      // drukuj paragon
JSON.parse(window.SunmiBridge.getDeviceInfo())  // info o V2s
```

Callback APK → JS po laser scan:
```js
window.onSunmiBarcodeScanned = function(code) {
    // np. fetch('/m/api/produkty/by-barcode/' + code)
};
```

## Debugging

### WebView debug (Chrome DevTools)

```kotlin
// W MainActivity.onCreate przed loadUrl:
if (BuildConfig.DEBUG) {
    WebView.setWebContentsDebuggingEnabled(true)
}
```

Potem w Chrome desktop: `chrome://inspect/#devices` → znajdz V2s → Inspect.

### Logcat

```powershell
adb logcat -s KsefSunmi,SunmiBridge,SunmiPrinter,ScannerRx
```

### Test scanner bez fizycznego skanu (emulator)

```powershell
adb shell am broadcast -a com.sunmi.scanner.ACTION_DATA_CODE_RECEIVED --es data "1234567890123"
```

### Test printer (Sunmi V2s w terenie)

W WebView fire JS:
```js
window.SunmiBridge.printReceipt("Test\n----\nABC 123\n");
```

## Roadmap

- [x] Sprint 30b — web side feature detect
- [x] Sprint 30c — Android project + WebView + JS bridge + AIDL
- [ ] Sprint 30d — Sunmi SDK helpers (optional, jak Opcja A)
- [ ] Sprint 30e — Build APK + sideload na fizyczny V2s
- [ ] Sprint 30f — docs/SUNMI_V2S_APK_GUIDE.md + memory file
- [ ] Sprint 31 — offline mode (Service Worker + IndexedDB queue draft FV)
- [ ] Sprint 32 — Sunmi App Store deployment (zamiast sideload)

## Linki

- [Sunmi developer docs](https://developer.sunmi.com)
- [SunmiPrinterDemo (oficjalny ref)](https://github.com/sunmi-OS/SunmiPrinterDemo)
- [ksef_invoicer (main app)](../ksef_invoicer)
- [Sprint 30b PR](https://github.com/tk9412745-ctrl/ksef_invoicer/pull/41)
