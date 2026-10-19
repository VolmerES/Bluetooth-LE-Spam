# Bluetooth LE Spam — VolmerES fork

A fork of [simondankelmann/Bluetooth-LE-Spam](https://github.com/simondankelmann/Bluetooth-LE-Spam).
The app itself — a proof-of-concept BLE advertisement toolkit — is unchanged in purpose; this fork
focuses on a **restyled UI** and on making the existing features **quicker to reach**. For the full
description of what the app does, its requirements and the original disclaimer, see
[`README.md`](README.md).

> Working branch: `feature/ui-crt-ambar`.

## What this fork adds

### 1. Amber-CRT terminal theme
A monochrome amber-phosphor-on-black identity that makes the whole app read like an old terminal.

- `ui/theme/Color.kt` — `AmberCrtDarkColors` / `AmberCrtLightColors` schemes and CRT semantic colors.
- `ui/theme/Type.kt` — monospace type scale (uses the platform monospace family; drop a `.ttf` in
  `res/font` to swap in a dedicated terminal face).
- `ui/theme/Shape.kt` — sharp, boxy corners.
- `ui/theme/TerminalUi.kt` — reusable `TerminalPanel`, `TerminalKeyValue`, `TerminalButton` and a
  cheap static `crtScanlines()` modifier.
- The **Info/Start** screen was redesigned around these panels, with explanatory and responsible-use
  copy.

Dynamic color (Material You) now defaults to **off** (`Helpers/ThemeManager.kt`) so the CRT identity
ships out of the box. Re-enable Material You, or pick another seed color, from **Settings → Theme**.

### 2. Quick Settings tile
`Services/SpamTileService.kt` — a tile in the notification shade that starts/stops advertising and
reflects the current state. With nothing loaded it opens the app on the Advertise screen instead of
doing nothing.

### 3. Home-screen widget
`Widgets/SpamWidgetProvider.kt` (+ `res/layout/widget_spam.xml`, `res/xml/spam_widget_info.xml`) — a
one-tap start/stop button styled to match the CRT theme.

Both the tile and the widget reuse `AdvertisementSetQueueHandler.toggle()`, so they **only start or
stop the advertisement collection you have already loaded and checked** — they never change *what*
is advertised. `BleSpamApplication` keeps them in sync with the app and the notification controls.

## Build

```bash
./gradlew assembleDebug        # debug APK
# or verify without packaging:
./gradlew :app:processDebugResources :app:compileDebugKotlin
```

Requires the Android SDK with platform 37 and `minSdk 26` (Android 8.0+).

## Responsible use

This is a research / proof-of-concept tool. Only broadcast at devices you own or are explicitly
authorised to test. Broadcasting unsolicited advertisements at other people's devices may be illegal
where you live. See the disclaimer in [`README.md`](README.md).

## Not merged from sibling forks

Two other forks were surveyed as possible sources of features:

- `DanTheManIsBack/Bluetooth-LE-Spamagain` — pre-Compose, 270 commits behind this fork; its only
  unique change is a 3-line tweak to an Apple payload.
- `ars2nb/blespam` — unrelated git history and a View/XML architecture, so not mergeable; the tile
  and widget here were re-implemented in its spirit against this fork's own code.

New **advertisement payloads** from those forks (e.g. the Yandex target) were intentionally **not**
ported, as they expand what gets broadcast at third-party devices rather than improving usability.
