# S-Locator Fleet Driver — Android App

A premium, bilingual (Arabic-default + English) Android app for fleet drivers to view and execute their daily route segments. Built with **native Kotlin** + **Jetpack Compose**, dark-mode-first, RTL-aware, and engineered to look "expensive" through restrained typography and the brand palette: **Royal Purple `#6B46C1`** + **Emerald `#10B981`** on **Obsidian `#0E0E12`**.

## What it does

1. On launch, the driver enters their **phone number** (which matches an XLSX sheet name).
2. The app downloads the routes workbook from the central server, caches it locally, and finds the driver's sheet.
3. The screen shows:
   - **Today's date**, rendered very large in a bold, weights-heavy display font.
   - One **Part 1**, **Part 2**, **Part 3**… button per route segment scheduled for today, each showing its stop count.
   - A **checkbox** beside each button. Tapping the button launches Google Maps with the route's deep link. Ticking the checkbox locks the button (visual treatment shifts to a quiet emerald confirmation state). Unticking it re-enables.
4. Completion state and the routes file are persisted on-device, so the app **works offline** after first sync.

## Project structure

```
slocator-fleet-driver/
├── settings.gradle.kts            ← root project config
├── build.gradle.kts               ← root build, plugin versions
├── gradle.properties
├── gradle/wrapper/...
└── app/
    ├── build.gradle.kts           ← Compose, Material3, Navigation, AppCompat (locale)
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml    ← supportsRtl, networkSecurityConfig, localeConfig
        ├── java/com/slocator/fleetdriver/
        │   ├── SLocatorApp.kt          ← Application class — defaults locale to Arabic
        │   ├── MainActivity.kt         ← Compose host + nav graph + Maps intent
        │   ├── data/
        │   │   ├── XlsxParser.kt       ← pure-Kotlin XLSX reader (no Apache POI)
        │   │   ├── RoutesRepository.kt ← download + cache + parse
        │   │   ├── DayResolver.kt      ← parses dates from "Day N: 28/april/2026"
        │   │   ├── CompletionStore.kt  ← per-driver-per-day-per-part done flags
        │   │   ├── PreferencesStore.kt ← last driver id, language override
        │   │   └── Models.kt
        │   └── ui/
        │       ├── theme/{Color,Type,Theme}.kt
        │       ├── components/{BrandLogo,PartButton}.kt
        │       └── screens/{LoginScreen,RoutesScreen}.kt
        └── res/
            ├── values/{strings,colors,themes,font_certs}.xml   ← English (secondary)
            ├── values-ar/strings.xml                           ← Arabic (default)
            ├── drawable/                                       ← gradient launcher + splash
            ├── mipmap-anydpi-v26/                              ← adaptive icons
            └── xml/                                            ← network + locales config
```

## How "expensive" minimalism is implemented

| UI Element        | Goal                | Technique                                                                          |
| ----------------- | ------------------- | ---------------------------------------------------------------------------------- |
| Background        | Depth & focus       | Pure obsidian `#0E0E12` with a subtle radial purple wash on login                  |
| Date headline     | Authority           | `displayLarge` 44sp ExtraBold, with a small emerald eyebrow above it                |
| Route buttons     | Tactile feedback    | Soft 22dp rounded shape, 12dp shadow tinted with brand purple, gradient fill        |
| Done state        | Calm completion     | Flattens to a quiet emerald-tinted card, button click is disabled, label re-tints   |
| Checkbox          | Recognition         | Custom 36dp rounded square with crisp emerald check; sits leading (auto-RTL flip)   |
| Typography (AR)   | Calligraphic warmth | Tajawal + Cairo via Google Downloadable Fonts                                       |
| Typography (EN)   | Geometric precision | Inter via Google Downloadable Fonts                                                 |

## RTL & bilingual behavior

* `android:supportsRtl="true"` and Compose's intrinsic LTR/RTL layout direction propagate from the locale.
* `xml/locales_config.xml` declares `ar` and `en`. The first run forces Arabic via `AppCompatDelegate.setApplicationLocales`.
* The language toggle in the top-end corner persists the choice in SharedPreferences and re-applies on every launch.
* Because the checkbox is leading and the maps icon is trailing, the entire row mirrors automatically: in Arabic, the checkbox sits on the right of the label, exactly as specified.

## Maps deep-link behavior

`MainActivity.openInMaps(url)` builds an `ACTION_VIEW` intent with `setPackage("com.google.android.apps.maps")`. If Google Maps isn't installed it falls back to any URL handler, and if none exists it shows a localized toast. The `<queries>` block in the manifest is required on Android 11+ for `resolveActivity` to see the Maps package.

## Network security

The routes URL is served over plain HTTP (`http://37.27.195.216:7080/...`). Cleartext traffic is **strictly limited to that single host** via `xml/network_security_config.xml`. Everything else stays HTTPS-only. Once the source moves to HTTPS, simply delete the `<domain-config>` block.

## Building / running

> **You need Android Studio Hedgehog (2023.1.1) or newer**, JDK 17, and the Android SDK (compile target 34, min 24).

1. **Open the project**: in Android Studio, `File → Open…` → select this folder. Studio will generate the missing `gradle/wrapper/gradle-wrapper.jar` and `gradlew` script automatically on first sync.
2. **Sync Gradle**. The first sync downloads Compose BOM 2024.02 and the Google-Fonts provider artifact.
3. **Run** on a connected device or emulator (`Shift+F10`). API 24+ supported.
4. Grant no permissions — the app only uses INTERNET (declared, not runtime-prompted).

### Building a release APK / AAB for Google Play

```bash
./gradlew :app:bundleRelease     # produces app/build/outputs/bundle/release/app-release.aab
./gradlew :app:assembleRelease   # produces app-release.apk
```

Sign with your upload key:

```bash
keytool -genkey -v -keystore slocator-upload.jks \
    -keyalg RSA -keysize 2048 -validity 10000 -alias slocator
```

Then add a `signingConfig` to `app/build.gradle.kts → buildTypes.release`. Submit the `.aab` via Google Play Console.

## XLSX schema expected

Each **sheet name = driver identifier** (currently "Driver 1", future "+966XXXXXXXXX").
Row 1 = day headers in the form `Day N: D/month/YYYY` (e.g. `Day 1: 28/april/2026`).
Rows 2..N = Part 1, Part 2, Part 3… containing a Google Maps directions URL per cell.

The parser is tolerant of the legacy `Day 1 — 23 stops` format and will keep working through the schema migration. Stop counts are auto-derived from waypoint coordinates in the URL when the header doesn't specify them.

## App Store readiness checklist

* [x] Native Compose UI, no WebView wrappers.
* [x] Adaptive launcher icon (mono + foreground + background) for Android 13 themed icons.
* [x] Splash screen API for Android 12+ with brand-colored logomark.
* [x] Full RTL support with `supportsRtl=true` and dedicated `values-ar/` strings.
* [x] Backup rules + data-extraction rules (Android 12+).
* [x] Network security config with explicit cleartext exception.
* [x] App works offline after first successful download.
* [x] No protected user data collected — only the entered phone number, persisted locally.

## Future enhancements

* Push notifications via FCM ("New route assigned", "Weather alert").
* Biometric fast-resume (FaceID/Fingerprint) over the saved driver id.
* Server-side Google Sheets API instead of the static XLSX URL once the manifest is live.
* "Shake to report issue" using `SensorManager` for an additional native-only proof point at App Store review time.
