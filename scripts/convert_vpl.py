#!/usr/bin/env python3
"""
convert_vpl.py — Converts a VPL Bible file to the per-book JSON format
expected by compile_content.py.

Usage:
    python scripts/convert_vpl.py <vpl_file> <translation_id> <lang> <name> [--license LICENSE]

Example:
    python scripts/convert_vpl.py \
        "bibles/fr-Saint Bible néo-Crampon/francl_vpl.txt" \
        crampon fr "Crampon 1923" --license "public domain"

Output:
    content/bible/<lang>/<translation_id>/metadata.json
    content/bible/<lang>/<translation_id>/books/<DR-name>.json
"""

import argparse
import json
import os
import sys

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# Maps VPL/OSIS book codes → DR file name (used as key in BOOK_MANIFEST)
VPL_TO_DR = {
    "GEN": "Genesis",
    "EXO": "Exodus",
    "LEV": "Leviticus",
    "NUM": "Numbers",
    "DEU": "Deuteronomy",
    "JOS": "Josue",
    "JDG": "Judges",
    "RUT": "Ruth",
    "1SA": "1 Kings",
    "2SA": "2 Kings",
    "1KI": "3 Kings",
    "2KI": "4 Kings",
    "1CH": "1 Paralipomenon",
    "2CH": "2 Paralipomenon",
    "EZR": "1 Esdras",
    "NEH": "2 Esdras",
    "TOB": "Tobias",
    "JDT": "Judith",
    "EST": "Esther",
    "1MA": "1 Machabees",
    "2MA": "2 Machabees",
    "JOB": "Job",
    "PSA": "Psalms",
    "PRO": "Proverbs",
    "ECC": "Ecclesiastes",
    "SOL": "Canticles",
    "WIS": "Wisdom",
    "SIR": "Ecclesiasticus",
    "ISA": "Isaias",
    "JER": "Jeremias",
    "LAM": "Lamentations",
    "BAR": "Baruch",
    "EZE": "Ezechiel",
    "DAN": "Daniel",
    "HOS": "Osee",
    "JOE": "Joel",
    "AMO": "Amos",
    "OBA": "Abdias",
    "JON": "Jonas",
    "MIC": "Micheas",
    "NAH": "Nahum",
    "HAB": "Habacuc",
    "ZEP": "Sophonias",
    "HAG": "Aggeus",
    "ZEC": "Zacharias",
    "MAL": "Malachias",
    "MAT": "Matthew",
    "MAR": "Mark",
    "MRK": "Mark",
    "LUK": "Luke",
    "JOH": "John",
    "ACT": "Acts",
    "ROM": "Romans",
    "1CO": "1 Corinthians",
    "2CO": "2 Corinthians",
    "GAL": "Galatians",
    "EPH": "Ephesians",
    "PHI": "Philippians",
    "PHP": "Philippians",
    "COL": "Colossians",
    "1TH": "1 Thessalonians",
    "2TH": "2 Thessalonians",
    "1TI": "1 Timothy",
    "2TI": "2 Timothy",
    "TIT": "Titus",
    "PHM": "Philemon",
    "HEB": "Hebrews",
    "JAM": "James",
    "JAS": "James",
    "1PE": "1 Peter",
    "2PE": "2 Peter",
    "1JO": "1 John",
    "2JO": "2 John",
    "3JO": "3 John",
    "JUD": "Jude",
    "REV": "Apocalypse",
}


def convert(vpl_path: str, translation_id: str, lang: str, name: str, license_: str):
    books: dict[str, dict[str, dict[str, str]]] = {}
    unknown_codes = set()

    with open(vpl_path, encoding="utf-8") as f:
        for lineno, line in enumerate(f, 1):
            line = line.strip()
            if not line:
                continue
            parts = line.split(" ", 2)
            if len(parts) < 3:
                print(f"  Warning: skipping malformed line {lineno}: {line!r}")
                continue
            code, ref, text = parts
            if ":" not in ref:
                print(f"  Warning: skipping line {lineno} with bad ref {ref!r}")
                continue
            chapter, verse = ref.split(":", 1)
            dr_name = VPL_TO_DR.get(code.upper())
            if dr_name is None:
                unknown_codes.add(code)
                continue
            books.setdefault(dr_name, {}).setdefault(chapter, {})[verse] = text

    if unknown_codes:
        print(f"  Warning: unrecognised book codes (skipped): {sorted(unknown_codes)}")

    out_dir = os.path.join(REPO_ROOT, "content", "bible", lang, translation_id)
    books_dir = os.path.join(out_dir, "books")
    os.makedirs(books_dir, exist_ok=True)

    # metadata.json
    metadata = {"id": translation_id, "name": name, "language": lang, "license": license_}
    with open(os.path.join(out_dir, "metadata.json"), "w", encoding="utf-8") as f:
        json.dump(metadata, f, ensure_ascii=False, indent=2)

    # per-book files
    for dr_name, chapters in books.items():
        out_path = os.path.join(books_dir, f"{dr_name}.json")
        with open(out_path, "w", encoding="utf-8") as f:
            json.dump(chapters, f, ensure_ascii=False, indent=1)
        print(f"  {dr_name}: {sum(len(v) for v in chapters.values())} verses")

    print(f"\nDone. {len(books)} books written to {out_dir}")


def main():
    parser = argparse.ArgumentParser(description="Convert VPL Bible to De Fide JSON format")
    parser.add_argument("vpl_file", help="Path to the .txt VPL file")
    parser.add_argument("translation_id", help="Short ID, e.g. 'crampon'")
    parser.add_argument("lang", help="Language code, e.g. 'fr'")
    parser.add_argument("name", help="Full translation name, e.g. 'Crampon 1923'")
    parser.add_argument("--license", default="public domain", dest="license_")
    args = parser.parse_args()
    convert(args.vpl_file, args.translation_id, args.lang, args.name, args.license_)


if __name__ == "__main__":
    main()
