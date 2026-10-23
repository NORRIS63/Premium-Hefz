# What has and has not been verified

## Run and passed (JVM, Kotlin 2.0.21)

* **28 unit tests** over the domain layer (page numbering, index lookups against markers read off the page images,
  bookmark import modes, streaks and dates, page extraction and verification, the animation timeline).
* **Animation timeline**: the line progress is 0 at 90 ms, 1 at 820 ms, never decreases, and is strictly increasing in
  between (so it travels bottom to top and never backtracks); total 1,300 ms; the cover stays closed until the line
  has arrived and faded; identity appears only after the line passes mid-screen.
* **Real APK**: extracting from the 50 MB `Hefz_Quran.apk` with the same code the app uses: 610 of 610 pages verified
  (SHA-256) in 2.4 s; an independent re-hash of the output agreed; a second run skipped all 610; after deleting one page
  and corrupting another, a third run re-extracted exactly those two. No `.part` files were left behind.
* The same through `tools/make_page_pack.py` output (610 of 610), and a one-bit change to one page was rejected.
* All 13 XML resource files parse, and `aapt2 compile` accepts every resource.
* Every `R.string` reference exists, and every format string receives the right number of arguments.
* The Kotlin sources have no syntax (parse) errors. Compiled without the Android libraries, kotlinc reports 2,267 type errors, 2,123 of them "unresolved reference" to Android, Compose or Room symbols; the others I inspected cascade from those (for example a lambda that is not recognised as suspending because `viewModelScope` is unresolved). This is a weak check, not a substitute for a real build.

## Not run

* **Compose, Room and Android code has not been compiled**, and nothing has run on a device or emulator. The build
  environment could not reach Google's Maven repository or Gradle's servers. Expect to fix a handful of compile errors
  on first build; run `./gradlew :app:assembleDebug` or use the GitHub Actions workflow and send the errors back.
* Rendering of the opening animation, the page tones, zoom gestures and the pager direction need a visual check on a device.
* Page-by-page checks of the surah start pages beyond the five opened.
