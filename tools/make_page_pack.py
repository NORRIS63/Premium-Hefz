#!/usr/bin/env python3
"""
Builds hefz_pages_pack.zip from your copy of the original Hefz Quran APK.

Every page image is checked against app/src/main/assets/page_manifest.csv (SHA-256). Only images that
match byte for byte go into the pack, and nothing is converted, resized or re-encoded. Copy the pack to
the phone and choose it in the app (Settings > Quran pages > Import), or choose the APK itself.

Usage: python tools/make_page_pack.py path/to/Hefz_Quran.apk [output.zip]
"""
import csv
import hashlib
import pathlib
import re
import sys
import zipfile

ROOT = pathlib.Path(__file__).resolve().parent.parent
MANIFEST = ROOT / "app" / "src" / "main" / "assets" / "page_manifest.csv"
ENTRY = re.compile(r".*?page(\d{3})\.gif$")


def main() -> int:
    if len(sys.argv) < 2:
        print(__doc__)
        return 2
    apk_path = pathlib.Path(sys.argv[1])
    out_path = pathlib.Path(sys.argv[2]) if len(sys.argv) > 2 else pathlib.Path("hefz_pages_pack.zip")

    expected = {}
    with open(MANIFEST, newline="") as fh:
        for row in csv.DictReader(fh):
            expected[int(row["file_index"])] = row["sha256"].lower()

    found, mismatched = {}, []
    with zipfile.ZipFile(apk_path) as apk:
        for info in apk.infolist():
            match = ENTRY.match(info.filename)
            if not match:
                continue
            index = int(match.group(1))
            if index not in expected or index in found:
                continue
            data = apk.read(info)
            if hashlib.sha256(data).hexdigest() == expected[index]:
                found[index] = data
            else:
                mismatched.append(index)

    missing = sorted(set(expected) - set(found))
    print(f"verified {len(found)} of {len(expected)} pages")
    if mismatched:
        print(f"  {len(mismatched)} pages differ from the original: {mismatched[:10]}...")
    if missing:
        print(f"  {len(missing)} pages not found: {missing[:10]}...")
    if mismatched or missing:
        print("Not writing a pack: this does not look like the original, unmodified APK.")
        return 1

    with zipfile.ZipFile(out_path, "w", zipfile.ZIP_STORED) as out:
        for index in sorted(found):
            out.writestr(f"pages/page{index:03d}.gif", found[index])
    print(f"wrote {out_path} ({out_path.stat().st_size / 1048576:.1f} MB)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
