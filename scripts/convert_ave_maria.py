#!/usr/bin/env python3
"""
convert_ave_maria.py — Converts bibliaAveMaria.json into the per-book DR format
expected by compile_content.py.

Usage:
    python scripts/convert_ave_maria.py

Input:
    content/bible/pt/ave-maria/bibliaAveMaria.json

Output:
    content/bible/pt/ave-maria/books/<DR-name>.json  (one file per book)
    content/bible/pt/ave-maria/metadata.json
"""

import json
import os
import sys

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

SOURCE = os.path.join(REPO_ROOT, "content", "bible", "pt", "ave-maria", "bibliaAveMaria.json")
OUT_DIR = os.path.join(REPO_ROOT, "content", "bible", "pt", "ave-maria", "books")
META_OUT = os.path.join(REPO_ROOT, "content", "bible", "pt", "ave-maria", "metadata.json")

# Maps Portuguese book name (as it appears in bibliaAveMaria.json) → DR filename (no .json)
_PT_TO_DR = {
    "Gênesis":                "Genesis",
    "Êxodo":                  "Exodus",
    "Levítico":               "Leviticus",
    "Números":                "Numbers",
    "Deuteronômio":           "Deuteronomy",
    "Josué":                  "Josue",
    "Juízes":                 "Judges",
    "Rute":                   "Ruth",
    "I Samuel":               "1 Kings",
    "II Samuel":              "2 Kings",
    "I Reis":                 "3 Kings",
    "II Reis":                "4 Kings",
    "I Crônicas":             "1 Paralipomenon",
    "II Crônicas":            "2 Paralipomenon",
    "Esdras":                 "1 Esdras",
    "Neemias":                "2 Esdras",
    "Tobias":                 "Tobias",
    "Judite":                 "Judith",
    "Ester":                  "Esther",
    "Jó":                     "Job",
    "Salmos":                 "Psalms",
    "I Macabeus":             "1 Machabees",
    "II Macabeus":            "2 Machabees",
    "Provérbios":             "Proverbs",
    "Eclesiastes":            "Ecclesiastes",
    "Cântico dos Cânticos":   "Canticles",
    "Sabedoria":              "Wisdom",
    "Eclesiástico":           "Ecclesiasticus",
    "Isaías":                 "Isaias",
    "Jeremias":               "Jeremias",
    "Lamentações":            "Lamentations",
    "Baruc":                  "Baruch",
    "Ezequiel":               "Ezechiel",
    "Daniel":                 "Daniel",
    "Oséias":                 "Osee",
    "Joel":                   "Joel",
    "Amós":                   "Amos",
    "Abdias":                 "Abdias",
    "Jonas":                  "Jonas",
    "Miquéias":               "Micheas",
    "Naum":                   "Nahum",
    "Habacuc":                "Habacuc",
    "Sofonias":               "Sophonias",
    "Ageu":                   "Aggeus",
    "Zacarias":               "Zacharias",
    "Malaquias":              "Malachias",
    "São Mateus":             "Matthew",
    "São Marcos":             "Mark",
    "São Lucas":              "Luke",
    "São João":               "John",
    "Atos dos Apóstolos":     "Acts",
    "Romanos":                "Romans",
    "I Coríntios":            "1 Corinthians",
    "II Coríntios":           "2 Corinthians",
    "Gálatas":                "Galatians",
    "Efésios":                "Ephesians",
    "Filipenses":             "Philippians",
    "Colossenses":            "Colossians",
    "I Tessalonicenses":      "1 Thessalonians",
    "II Tessalonicenses":     "2 Thessalonians",
    "I Timóteo":              "1 Timothy",
    "II Timóteo":             "2 Timothy",
    "Tito":                   "Titus",
    "Filêmon":                "Philemon",
    "Hebreus":                "Hebrews",
    "São Tiago":              "James",
    "I São Pedro":            "1 Peter",
    "II São Pedro":           "2 Peter",
    "I São João":             "1 John",
    "II São João":            "2 John",
    "III São João":           "3 John",
    "São Judas":              "Jude",
    "Apocalipse":             "Apocalypse",
}


def main() -> None:
    if not os.path.exists(SOURCE):
        print(f"ERROR: source file not found: {SOURCE}", file=sys.stderr)
        print("Place bibliaAveMaria.json at content/bible/pt/ave-maria/bibliaAveMaria.json")
        sys.exit(1)

    os.makedirs(OUT_DIR, exist_ok=True)

    print(f"Reading {SOURCE} ...")
    with open(SOURCE, encoding="utf-8") as f:
        data = json.load(f)

    # Collect books from antigoTestamento + novoTestamento sections (or flat list)
    if isinstance(data, dict) and "antigoTestamento" in data:
        books = data["antigoTestamento"] + data["novoTestamento"]
    elif isinstance(data, dict):
        books = data.get("livros") or data.get("books") or list(data.values())[0]
    else:
        books = data

    converted = 0
    skipped = 0

    for book_entry in books:
        pt_name = book_entry.get("livro") or book_entry.get("nome") or book_entry.get("name", "")
        pt_name = pt_name.strip()

        dr_name = _PT_TO_DR.get(pt_name)
        if dr_name is None:
            print(f"  WARN: no DR mapping for '{pt_name}' — skipping")
            skipped += 1
            continue

        chapters_raw = book_entry.get("capitulos") or book_entry.get("chapters", [])
        out: dict[str, dict[str, str]] = {}

        for ch in chapters_raw:
            ch_num = str(ch.get("capitulo") or ch.get("chapter") or ch.get("numero", 0))
            verses_raw = ch.get("versiculos") or ch.get("verses", [])
            out[ch_num] = {}
            for v in verses_raw:
                v_num = str(v.get("versiculo") or v.get("verse") or v.get("numero", 0))
                text = (v.get("texto") or v.get("text") or "").strip()
                out[ch_num][v_num] = text

        out_path = os.path.join(OUT_DIR, f"{dr_name}.json")
        with open(out_path, "w", encoding="utf-8") as f:
            json.dump(out, f, ensure_ascii=False, indent=2)

        total_verses = sum(len(vs) for vs in out.values())
        print(f"  {pt_name:35s} → {dr_name}.json  ({len(out)} chapters, {total_verses} verses)")
        converted += 1

    # Write metadata
    meta = {
        "id": "ave-maria",
        "name": "Bíblia Ave-Maria",
        "language": "pt",
        "license": "© Editora Ave-Maria. Used with permission."
    }
    with open(META_OUT, "w", encoding="utf-8") as f:
        json.dump(meta, f, ensure_ascii=False, indent=2)

    print(f"\nDone. {converted} books converted, {skipped} skipped.")
    print(f"Metadata written to {META_OUT}")
    if skipped:
        print("Review WARNs above and update _PT_TO_DR if any books are missing.")


if __name__ == "__main__":
    main()
