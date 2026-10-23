# Quran integrity

## What the original app actually contains

Inspected from `Hefz_Quran.apk` (SHA-256 `f4922078c42ec51843e834e407736e68f3ca76a82e70e0a319ca861c06b6afca`):

* **No Quran text.** The Quran is 610 scanned page images, `page003.gif` to `page612.gif`, contiguous, grayscale,
  about 450 x 760 px, 43.1 MB in total.
* The only Arabic strings in the entire package are 114 surah names and 30 juz opening phrases (1,442 characters,
  longest 23). No verse text exists in the code, resources or assets.
* Therefore nothing here was "recovered" as text, and **no text has been generated, OCR-ed, paraphrased or substituted**.

## What this app does about it

* The reader shows the original images and nothing else. They are imported from the user's own copy and each one is
  accepted only if its SHA-256 equals the original's (`app/src/main/assets/page_manifest.csv`). Rendering tones
  (Paper, Night) are colour filters applied at draw time; files are never modified.
* Settings > Verify pages re-hashes everything on the device against that manifest.
* No separate Quran text layer exists. Adding one (for ayah selection, search, translation, audio or word-by-word
  reveal) is a separate decision that needs an explicitly approved, licensed dataset and a documented comparison.

## Page numbering

The same page carried three different numbers in the original. This app stores and shows only the **printed page number**:

| Number | Range | Meaning |
|---|---|---|
| File index | 3..612 | `pageNNN.gif`; what the original stored for bookmarks and last-read |
| **Printed page** | **2..611** | printed on the page; used in the original's surah and para lists; **used here** |
| Go To Page index | 1..610 | what the original's Go To Page screen accepted (printed minus 1) |

Checked by eye against five pages (file 3, 4, 52, 601, 605): printed number = file number minus 1, and the juz and
page-in-juz markers printed on those pages equal what `QuranIndex` computes (see `QuranIndexTest`).

Consequences, all deliberate and visible:

* Go To Page here takes the printed number. In the original, typing N opened printed page N+1.
* Imported bookmarks are converted exactly (file index - 1, Go To index + 1, or juz start + page-in-juz - 1).
* Two display inconsistencies in the original's lists were corrected: Al-Alaq was listed on page 605 but is printed
  on 604; Para 1 was listed as page 2 but its link opens printed page 3 (the first page counted in juz 1 and the one
  marked "1" in the page itself). Page 2 (Al-Fatihah) is shown as the opening page, before juz 1.

## Navigation data

`navigation.json` holds surah names, ayah counts and start pages recovered from the original's resources and jump
tables. Its ayah counts equal the standard Hafs counts for all 114 surahs (total 6,236); that validates the metadata,
not the images. Start pages were recovered from the original's own tables, not independently verified for all 114
surahs (five pages were opened and matched).
