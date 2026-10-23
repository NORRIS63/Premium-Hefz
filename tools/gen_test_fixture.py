#!/usr/bin/env python3
"""Regenerates app/src/test/.../NavigationFixture.kt from app/src/main/assets/navigation.json."""
import json, pathlib
root = pathlib.Path(__file__).resolve().parent.parent
nav = json.load(open(root / "app/src/main/assets/navigation.json", encoding="utf-8"))
q = lambda s: json.dumps(s, ensure_ascii=False)
out = ["package app.premiumhefz.domain", "",
       "// Generated from app/src/main/assets/navigation.json by tools/gen_test_fixture.py. Do not edit by hand.",
       "object NavigationFixture {", "    val surahs: List<Surah> = listOf("]
out += [f"        Surah({s['n']}, {q(s['en'])}, {q(s['ar'])}, {q(s['type'])}, {s['ayahs']}, {s['page']})," for s in nav["surahs"]]
out += ["    )", "", "    val juz: List<Juz> = listOf("]
out += [f"        Juz({j['n']}, {q(j['en'])}, {q(j['ar'])}, {j['page']})," for j in nav["juz"]]
out += ["    )", "", "    val index: QuranIndex get() = QuranIndex(surahs, juz)", "}", ""]
(root / "app/src/test/kotlin/app/premiumhefz/domain/NavigationFixture.kt").write_text("\n".join(out), encoding="utf-8")
