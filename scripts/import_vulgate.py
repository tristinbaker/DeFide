#!/usr/bin/env python3
"""
import_vulgate.py — Downloads the Latin Vulgate + English Translation JSON from
  https://github.com/aseemsavio/Latin-Vulgate-English-Translation-JSON
and converts it into two per-book translation folders that compile_content.py
can ingest with the same BOOK_MANIFEST used for the DRA.

Outputs:
    content/bible/vulgate/metadata.json
    content/bible/vulgate/books/<DR-name>.json      ← Latin text
    content/bible/vulgate-et/metadata.json
    content/bible/vulgate-et/books/<DR-name>.json   ← English translation

Usage:
    python scripts/import_vulgate.py
"""

import json
import os
import sys
import urllib.request

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CONTENT_DIR = os.path.join(REPO_ROOT, "content", "bible")

BASE_URL = (
    "https://raw.githubusercontent.com/aseemsavio/"
    "Latin-Vulgate-English-Translation-JSON/master/"
    "Generated-JSON/Latin-Vulgate-English-Translation-Study-Bible"
)

# Maps source filename (without .json) → DR name used by BOOK_MANIFEST
# DR names must exactly match the keys in compile_content.py's BOOK_MANIFEST.
FILENAME_TO_DR = {
    "OT-01_Genesis":        "Genesis",
    "OT-02_Exodus":         "Exodus",
    "OT-03_Leviticus":      "Leviticus",
    "OT-04_Numbers":        "Numbers",
    "OT-05_Deuteronomy":    "Deuteronomy",
    "OT-06_Joshua":         "Josue",
    "OT-07_Judges":         "Judges",
    "OT-08_Ruth":           "Ruth",
    "OT-09_1-Samuel":       "1 Kings",
    "OT-10_2-Samuel":       "2 Kings",
    "OT-11_1-Kings":        "3 Kings",
    "OT-12_2-Kings":        "4 Kings",
    "OT-13_1-Chronicles":   "1 Paralipomenon",
    "OT-14_2-Chronicles":   "2 Paralipomenon",
    "OT-15_Ezra":           "1 Esdras",
    "OT-16_Nehemiah":       "2 Esdras",
    "OT-17_Tobit":          "Tobias",
    "OT-18_Judith":         "Judith",
    "OT-19_Esther":         "Esther",
    "OT-20_Job":            "Job",
    "OT-21_Psalms":         "Psalms",
    "OT-22_Proverbs":       "Proverbs",
    "OT-23_Ecclesiastes":   "Ecclesiastes",
    "OT-24_Song":           "Canticles",
    "OT-25_Wisdom":         "Wisdom",
    "OT-26_Sirach":         "Ecclesiasticus",
    "OT-27_Isaiah":         "Isaias",
    "OT-28_Jeremiah":       "Jeremias",
    "OT-29_Lamentations":   "Lamentations",
    "OT-30_Baruch":         "Baruch",
    "OT-31_Ezekiel":        "Ezechiel",
    "OT-32_Daniel":         "Daniel",
    "OT-33_Hosea":          "Osee",
    "OT-34_Joel":           "Joel",
    "OT-35_Amos":           "Amos",
    "OT-36_Obadiah":        "Abdias",
    "OT-37_Jonah":          "Jonas",
    "OT-38_Micah":          "Micheas",
    "OT-39_Nahum":          "Nahum",
    "OT-40_Habakkuk":       "Habacuc",
    "OT-41_Zephaniah":      "Sophonias",
    "OT-42_Haggai":         "Aggeus",
    "OT-43_Zechariah":      "Zacharias",
    "OT-44_Malachi":        "Malachias",
    "OT-45_1-Maccabees":    "1 Machabees",
    # 2 Maccabees not present in source repo
    "NT-01_Matthew":        "Matthew",
    "NT-02_Mark":           "Mark",
    "NT-03_Luke":           "Luke",
    "NT-04_John":           "John",
    "NT-05_Acts":           "Acts",
    "NT-06_Romans":         "Romans",
    "NT-07_1-Corinthians":  "1 Corinthians",
    "NT-08_2-Corinthians":  "2 Corinthians",
    "NT-09_Galatians":      "Galatians",
    "NT-10_Ephesians":      "Ephesians",
    "NT-11_Philippians":    "Philippians",
    "NT-12_Colossians":     "Colossians",
    "NT-13_1-Thessalonians":"1 Thessalonians",
    "NT-14_2-Thessalonians":"2 Thessalonians",
    "NT-15_1-Timothy":      "1 Timothy",
    "NT-16_2-Timothy":      "2 Timothy",
    "NT-17_Titus":          "Titus",
    "NT-18_Philemon":       "Philemon",
    "NT-19_Hebrews":        "Hebrews",
    "NT-20_James":          "James",
    "NT-21_1-Peter":        "1 Peter",
    "NT-22_2-Peter":        "2 Peter",
    "NT-23_1-John":         "1 John",
    "NT-24_2-John":         "2 John",
    "NT-25_3-John":         "3 John",
    "NT-26_Jude":           "Jude",
    "NT-27_Revelation":     "Apocalypse",
}

VULGATE_METADATA = {
    "id": "vulgate",
    "name": "Latin Vulgate",
    "language": "la",
    "license": "public domain",
}

VULGATE_ET_METADATA = {
    "id": "vulgate-et",
    "name": "Latin Vulgate (English Translation)",
    "language": "en",
    "license": "public domain",
}


def fetch_json(filename: str) -> list:
    url = f"{BASE_URL}/{filename}.json"
    print(f"  Fetching {filename}.json …", end=" ", flush=True)
    with urllib.request.urlopen(url, timeout=30) as resp:
        data = json.loads(resp.read().decode("utf-8"))
    print(f"{len(data)} verses")
    return data


def convert(chapters: list) -> tuple[dict, dict]:
    """
    Accepts an array of chapter objects: [{chapter, verses: [{chapter, verse, textLa, textEn}]}]
    Returns (latin_book, english_book) each as {chapter_str: {verse_str: text}}.
    """
    latin: dict = {}
    english: dict = {}
    for ch_obj in chapters:
        for v in ch_obj["verses"]:
            ch = str(v["chapter"])
            ve = str(v["verse"])
            latin.setdefault(ch, {})[ve] = v["textLa"].strip()
            english.setdefault(ch, {})[ve] = v["textEn"].strip()
    return latin, english


def write_book(books_dir: str, dr_name: str, book_data: dict) -> None:
    path = os.path.join(books_dir, f"{dr_name}.json")
    with open(path, "w", encoding="utf-8") as f:
        json.dump(book_data, f, ensure_ascii=False, indent=1)


def main() -> None:
    vul_books_dir = os.path.join(CONTENT_DIR, "vulgate", "books")
    et_books_dir  = os.path.join(CONTENT_DIR, "vulgate-et", "books")
    os.makedirs(vul_books_dir, exist_ok=True)
    os.makedirs(et_books_dir,  exist_ok=True)

    # Write metadata
    for meta in (VULGATE_METADATA, VULGATE_ET_METADATA):
        meta_path = os.path.join(CONTENT_DIR, meta["id"], "metadata.json")
        with open(meta_path, "w") as f:
            json.dump(meta, f, indent=2)
    print("Metadata written.")

    total_latin = 0
    total_english = 0

    for source_name, dr_name in FILENAME_TO_DR.items():
        try:
            verses = fetch_json(source_name)
        except Exception as e:
            print(f"  ERROR fetching {source_name}: {e}", file=sys.stderr)
            continue

        latin_book, english_book = convert(verses)

        latin_count   = sum(len(vs) for vs in latin_book.values())
        english_count = sum(len(vs) for vs in english_book.values())

        write_book(vul_books_dir, dr_name, latin_book)
        write_book(et_books_dir,  dr_name, english_book)

        total_latin   += latin_count
        total_english += english_count
        print(f"    → {dr_name}: {latin_count} verses")

    print(f"\nDone. Vulgate: {total_latin} verses | English Translation: {total_english} verses")
    print("Run `python scripts/compile_content.py` to rebuild the database.")


if __name__ == "__main__":
    main()
