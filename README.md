# Premium Hefz (Stage 3 foundation)

A native Android reader for the 610-page mushaf used by the original **Hefz Quran** app
(`com.golap.hefzquran` 6.2.0), redesigned in Emerald & Gold, working fully offline.

Kotlin, Jetpack Compose (Material 3), Room. Minimum Android 6.0 (API 23, same as the original), target API 35.
This app has **no INTERNET permission** and its own application ID (`app.premiumhefz`), so it installs next to
the original and never touches the original's data.

## What works in this stage

| Area | Status |
|---|---|
| Opening animation: a hairline rises bottom to top, reveals the identity, then the cover parts to show Home (about 1.3 s, plays once per launch, honours reduced motion) | written; timing unit tested |
| Emerald & Gold design system, light and dark | written |
| Home: Continue reading, today's pages and streak, quick actions, honest offline status | written |
| Reader: the original page images, swipe in reading direction, pinch and double-tap zoom, long-press or icon to bookmark, last-read position, Paper / Night / Original tones, screen kept awake | written |
| Browse: surah list with search, juz list, Go To Page (page, or juz + page-in-juz as in the original) | written |
| Bookmarks: list, delete, import from the old app | written |
| Settings: theme, page tone, daily reading goal, reduce motion, page import, page verification | written |
| Page import with byte-for-byte verification, resume and repair | extraction logic tested on the real APK |

Not in this stage (next): Hifz Mode, Revision Planner, Progress screens, daily reminders, Bengali strings, bundled fonts.
Nothing in the UI is a placeholder for those: their entry points are simply not shown yet.

## Build

Requirements: JDK 17, Android Studio (Ladybug or newer) or the Android SDK command-line tools.

```
./gradlew :app:testDebugUnitTest :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

No local toolchain? Push this folder to a GitHub repository and run the **Build debug APK** workflow
(`.github/workflows/build-debug-apk.yml`); the APK is attached to the run.

> The Compose, Room and Android layers have **not** been compiled yet: the environment this was written in
> cannot reach Google's Maven repository. See `docs/VERIFICATION.md` for exactly what has and has not been run.

## First run: adding the Quran pages

The page images belong to the original app and are not copied into this project. Import them once:

1. **Easiest:** keep the original Hefz Quran installed, open *Settings > Import pages > Use the installed Hefz Quran app*.
2. Or choose the original APK (or a pack file) with *Choose an APK or pack file*.
3. To make a smaller pack file on a computer: `python tools/make_page_pack.py Hefz_Quran.apk` then copy
   `hefz_pages_pack.zip` to the phone.

Every page is checked against `app/src/main/assets/page_manifest.csv` (SHA-256 of all 610 originals) before it is
kept, so what ends up on the device is byte-identical to the original. Anything that does not match is rejected.

## Existing user data

The original app keeps bookmarks and the last-read page in its private storage; no other app can read it, and a
different signing key means this app can't replace it in place. Re-enter bookmarks with *Bookmarks > Import*
(accepts the old Bookmarks screen's "Para: 3, Page: 9" form, printed page numbers, the old Go To numbers, or raw saved entries).

## Layout

```
app/src/main/kotlin/app/premiumhefz/
  domain/   pure Kotlin, no Android: page numbering, index lookups, bookmark import, streaks, animation timeline,
            page-pack extraction and verification   (unit tested)
  data/     Room database, settings, page store, repository
  ui/       theme, components, startup overlay, Home, Reader, Browse, Bookmarks, Settings, Setup, navigation
docs/       ARCHITECTURE.md, QURAN_INTEGRITY.md, VERIFICATION.md
tools/      make_page_pack.py, gen_test_fixture.py
```
