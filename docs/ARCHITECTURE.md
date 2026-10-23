# Architecture (Stage 2)

## Layers

```
ui (Compose, Material 3)  ->  data (Room, settings, page store)  ->  domain (pure Kotlin)
```

* **domain** has no Android imports, so it compiles and runs on a plain JVM and is unit tested there:
  `PageCoordinates`, `QuranIndex`, `BookmarkImport`, `Streaks`/`Dates`, `StartupMotion`, `PageManifest`,
  `PagePackExtractor`, `DirectoryPageSink`, `PackVerifier`.
* **data**: `HefzDatabase` (Room, schema version 1, no destructive migration), `HefzRepository`, `SettingsStore`
  (SharedPreferences exposed as a `StateFlow`), `PagePack` (the on-device page folder and its status), `PageBitmaps`
  (12-page LRU cache), `QuranIndexLoader`.
* **ui**: one `ViewModel` per screen built from a hand-rolled `AppContainer`; state is collected with
  `collectAsStateWithLifecycle`. No DI framework, no extra libraries.

## Navigation

Four tabs (Home, Browse, Bookmarks, Settings) with a bottom bar; Reader and Page import are full-screen routes.

```
home | browse?tab={0 surah,1 juz,2 page} | bookmarks | settings | setup | reader/{printedPage}
```

## Database (Room, v1)

| Table | Key | Purpose |
|---|---|---|
| `bookmarks` | id, unique `page` | one bookmark per printed page, with timestamp |
| `reading_position` | id = 1 | last page the reader settled on |
| `read_events` | (`page`, `epochDay`) | a page counts as read for a day after 3 s on screen; drives daily goal and streak |

Stage 4 adds page memorization status, hifz sessions, revision items and revision log with explicit migrations.

## Design system

Emerald and gold, tonal surfaces instead of outlined cards, one geometric motif (the eight-point star) used sparingly.

| Token | Dark | Light |
|---|---|---|
| background / surface | #052E22 | #FBF8EF |
| container steps | #07382A, #0A4130, #0D4D39 | #F4EFE0, #EFE9D7, #E9E2CD |
| primary | #1B8F63 | #0F5A40 |
| gold accent | #D8B965 | #A9832B |
| page tones | Night: ink #E9E0C4 on #04231A | Paper: ink #14201B on #F4EFE0 |

System fonts for now (Latin, Bengali and Arabic all render); an open-licence UI font is a later step.

## Opening animation (`StartupMotion`, `StartupOverlay`)

| Time (ms) | What happens |
|---|---|
| 0-120 | emerald field; the hairline fades in at the bottom edge (it starts moving at 90) |
| 90-820 | the line's head travels from the bottom edge to the top edge (cubic-bezier 0.40, 0, 0.20, 1); a gold-to-emerald trail follows |
| 470-820 | identity (star and wordmark) is revealed as the line passes mid-screen |
| 820-900 | line fades out at the top |
| 900-1290 | the emerald cover splits along the line's path and slides apart, revealing Home |

Total 1.3 s. A single Canvas reads the clock only in draw and graphics-layer blocks (no per-frame recomposition).
The flag that it has played is saved across rotation, so it never replays when returning from another screen.
Reduced motion (system animator scale 0, or the in-app switch) shows the identity for 350 ms and fades. The system
splash is a plain emerald field with no icon (`Theme.PremiumHefz.Starting`) so there is no flash between the two.

## Offline

No INTERNET permission. Pages live in `filesDir/pages` (excluded from cloud backup; progress and settings are included).
Status is shown honestly: Home says "Offline · 610 pages" only when all 610 are present and otherwise shows the count.
