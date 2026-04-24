#!/usr/bin/env python3
"""
extract_platense.py — Converts the SpaPlatense SWORD module (Biblia Platense, Straubinger)
into the per-book JSON format expected by compile_content.py.

Usage:
    python scripts/extract_platense.py

Input:
    platense/  (SWORD module directory, sibling of scripts/)

Output:
    content/bible/es/platense/metadata.json
    content/bible/es/platense/books/<BookName>.json

License: Public Domain (copyright expired in Argentina, 1948 + 70 = 2018)
"""

import json
import os
import sys

try:
    from pysword.modules import SwordModules
except ImportError:
    print("ERROR: pysword not installed. Run: pip install pysword", file=sys.stderr)
    sys.exit(1)

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODULE_DIR = os.path.join(REPO_ROOT, "platense")
OUT_DIR = os.path.join(REPO_ROOT, "content", "bible", "es", "platense")
BOOKS_DIR = os.path.join(OUT_DIR, "books")

PYSWORD_TO_DR = {
    "Genesis":            "Genesis",
    "Exodus":             "Exodus",
    "Leviticus":          "Leviticus",
    "Numbers":            "Numbers",
    "Deuteronomy":        "Deuteronomy",
    "Joshua":             "Josue",
    "Judges":             "Judges",
    "Ruth":               "Ruth",
    "I Samuel":           "1 Kings",
    "II Samuel":          "2 Kings",
    "I Kings":            "3 Kings",
    "II Kings":           "4 Kings",
    "I Chronicles":       "1 Paralipomenon",
    "II Chronicles":      "2 Paralipomenon",
    "Ezra":               "1 Esdras",
    "Nehemiah":           "2 Esdras",
    "Tobit":              "Tobias",
    "Judith":             "Judith",
    "Esther":             "Esther",
    "I Maccabees":        "1 Machabees",
    "II Maccabees":       "2 Machabees",
    "Job":                "Job",
    "Psalms":             "Psalms",
    "Proverbs":           "Proverbs",
    "Ecclesiastes":       "Ecclesiastes",
    "Song of Solomon":    "Canticles",
    "Wisdom":             "Wisdom",
    "Sirach":             "Ecclesiasticus",
    "Isaiah":             "Isaias",
    "Jeremiah":           "Jeremias",
    "Lamentations":       "Lamentations",
    "Baruch":             "Baruch",
    "Ezekiel":            "Ezechiel",
    "Daniel":             "Daniel",
    "Hosea":              "Osee",
    "Joel":               "Joel",
    "Amos":               "Amos",
    "Obadiah":            "Abdias",
    "Jonah":              "Jonas",
    "Micah":              "Micheas",
    "Nahum":              "Nahum",
    "Habakkuk":           "Habacuc",
    "Zephaniah":          "Sophonias",
    "Haggai":             "Aggeus",
    "Zechariah":          "Zacharias",
    "Malachi":            "Malachias",
    "Matthew":            "Matthew",
    "Mark":               "Mark",
    "Luke":               "Luke",
    "John":               "John",
    "Acts":               "Acts",
    "Romans":             "Romans",
    "I Corinthians":      "1 Corinthians",
    "II Corinthians":     "2 Corinthians",
    "Galatians":          "Galatians",
    "Ephesians":          "Ephesians",
    "Philippians":        "Philippians",
    "Colossians":         "Colossians",
    "I Thessalonians":    "1 Thessalonians",
    "II Thessalonians":   "2 Thessalonians",
    "I Timothy":          "1 Timothy",
    "II Timothy":         "2 Timothy",
    "Titus":              "Titus",
    "Philemon":           "Philemon",
    "Hebrews":            "Hebrews",
    "James":              "James",
    "I Peter":            "1 Peter",
    "II Peter":           "2 Peter",
    "I John":             "1 John",
    "II John":            "2 John",
    "III John":           "3 John",
    "Jude":               "Jude",
    "Revelation of John": "Apocalypse",
}


def clean_verse(text: str) -> str:
    """Strip OSIS section markers (*) and extra whitespace from verse text."""
    text = text.strip()
    if text.startswith("*"):
        text = text[1:].strip()
    return text


def extract_book(bible, book) -> dict:
    """Return {chapter_str: {verse_str: text}} for an entire book."""
    chapters = {}
    for chapter_num in range(1, book.num_chapters + 1):
        verse_iter = bible.get_iter(
            books=[book.osis_name.lower()],
            chapters=[chapter_num],
        )
        verses = {}
        for verse_num, raw in enumerate(verse_iter, start=1):
            text = clean_verse(raw)
            if text:
                verses[str(verse_num)] = text
        if verses:
            chapters[str(chapter_num)] = verses
    return chapters


def main() -> None:
    if not os.path.isdir(MODULE_DIR):
        print(f"ERROR: SWORD module not found at {MODULE_DIR}", file=sys.stderr)
        sys.exit(1)

    os.makedirs(BOOKS_DIR, exist_ok=True)

    modules = SwordModules(MODULE_DIR)
    modules.parse_modules()
    bible = modules.get_bible_from_module("SpaPlatense")
    structure = bible.get_structure()
    all_books = structure._books["ot"] + structure._books["nt"]

    skipped = []
    converted = 0

    for book in all_books:
        dr_name = PYSWORD_TO_DR.get(book.name)
        if dr_name is None:
            print(f"  SKIP (unmapped): {book.name}")
            skipped.append(book.name)
            continue

        print(f"  Extracting {book.name} → {dr_name}.json ...", end=" ", flush=True)
        data = extract_book(bible, book)
        out_path = os.path.join(BOOKS_DIR, f"{dr_name}.json")
        with open(out_path, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        chapter_count = len(data)
        verse_count = sum(len(v) for v in data.values())
        print(f"{chapter_count} chapters, {verse_count} verses")
        converted += 1

    metadata = {
        "id": "platense",
        "name": "Biblia Platense (Straubinger)",
        "language": "es",
        "license": "Public Domain",
    }
    meta_path = os.path.join(OUT_DIR, "metadata.json")
    with open(meta_path, "w", encoding="utf-8") as f:
        json.dump(metadata, f, ensure_ascii=False, indent=2)

    print(f"\nDone. {converted} books written to {OUT_DIR}")
    if skipped:
        print(f"Skipped (unmapped): {skipped}")


if __name__ == "__main__":
    main()
