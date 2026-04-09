#!/usr/bin/env python3
"""
scrape_biblija_lt.py — Scrapes the Lithuanian Catholic Bible (RK K1998)
from biblija.lt and writes it to the De Fide per-book JSON format.

Usage:
    python scripts/scrape_biblija_lt.py [--delay 1.0]

Output:
    content/bible/lt/rk1998/metadata.json
    content/bible/lt/rk1998/books/<DR-name>.json
"""

import argparse
import json
import os
import re
import time
import urllib.parse
import urllib.request
from html.parser import HTMLParser

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BASE_URL = "https://biblija.lt/index.aspx?cmp=reading&doc=BiblijaRKK1998_{book}_{chapter}"
OUT_DIR = os.path.join(REPO_ROOT, "content", "bible", "lt", "rk1998")

# Lithuanian book code → DR filename
LT_TO_DR = {
    "Pr":     "Genesis",
    "Iš":     "Exodus",
    "Kun":    "Leviticus",
    "Sk":     "Numbers",
    "Ist":    "Deuteronomy",
    "Joz":    "Josue",
    "Ts":     "Judges",
    "Rut":    "Ruth",
    "1_Sam":  "1 Kings",
    "2_Sam":  "2 Kings",
    "1_Kar":  "3 Kings",
    "2_Kar":  "4 Kings",
    "1_Kr":   "1 Paralipomenon",
    "2_Kr":   "2 Paralipomenon",
    "Ezd":    "1 Esdras",
    "Neh":    "2 Esdras",
    "Tob":    "Tobias",
    "Jdt":    "Judith",
    "Est":    "Esther",
    "1_Mak":  "1 Machabees",
    "2_Mak":  "2 Machabees",
    "Job":    "Job",
    "Ps":     "Psalms",
    "Pat":    "Proverbs",
    "Koh":    "Ecclesiastes",
    "Gg":     "Canticles",
    "Išm":    "Wisdom",
    "Sir":    "Ecclesiasticus",
    "Iz":     "Isaias",
    "Jer":    "Jeremias",
    "Rd":     "Lamentations",
    "Bar":    "Baruch",
    "Ez":     "Ezechiel",
    "Dan":    "Daniel",
    "Oz":     "Osee",
    "Jl":     "Joel",
    "Am":     "Amos",
    "Abd":    "Abdias",
    "Jon":    "Jonas",
    "Mch":    "Micheas",
    "Nah":    "Nahum",
    "Hab":    "Habacuc",
    "Sof":    "Sophonias",
    "Ag":     "Aggeus",
    "Zch":    "Zacharias",
    "Mal":    "Malachias",
    "Mt":     "Matthew",
    "Mk":     "Mark",
    "Lk":     "Luke",
    "Jn":     "John",
    "Apd":    "Acts",
    "Rom":    "Romans",
    "1_Kor":  "1 Corinthians",
    "2_Kor":  "2 Corinthians",
    "Gal":    "Galatians",
    "Ef":     "Ephesians",
    "Fil":    "Philippians",
    "Kol":    "Colossians",
    "1_Tes":  "1 Thessalonians",
    "2_Tes":  "2 Thessalonians",
    "1_Tim":  "1 Timothy",
    "2_Tim":  "2 Timothy",
    "Tit":    "Titus",
    "Fm":     "Philemon",
    "Žyd":    "Hebrews",
    "Jok":    "James",
    "1_Pt":   "1 Peter",
    "2_Pt":   "2 Peter",
    "1_Jn":   "1 John",
    "2_Jn":   "2 John",
    "3_Jn":   "3 John",
    "Jud":    "Jude",
    "Apr":    "Apocalypse",
}

# Lithuanian display names, keyed by DR filename
LT_BOOK_NAMES = {
    "Genesis":          "Pradžios",
    "Exodus":           "Išėjimo",
    "Leviticus":        "Kunigų",
    "Numbers":          "Skaičių",
    "Deuteronomy":      "Pakartoto Įstatymo",
    "Josue":            "Jozuės",
    "Judges":           "Teisėjų",
    "Ruth":             "Rutos",
    "1 Kings":          "1 Samuelio",
    "2 Kings":          "2 Samuelio",
    "3 Kings":          "1 Karalių",
    "4 Kings":          "2 Karalių",
    "1 Paralipomenon":  "1 Kronikų",
    "2 Paralipomenon":  "2 Kronikų",
    "1 Esdras":         "Ezdro",
    "2 Esdras":         "Nehemijo",
    "Tobias":           "Tobito",
    "Judith":           "Juditos",
    "Esther":           "Esteros",
    "1 Machabees":      "1 Makabiejų",
    "2 Machabees":      "2 Makabiejų",
    "Job":              "Jobo",
    "Psalms":           "Psalmių",
    "Proverbs":         "Patarlių",
    "Ecclesiastes":     "Koheleto",
    "Canticles":        "Giesmių Giesmė",
    "Wisdom":           "Išminties",
    "Ecclesiasticus":   "Siracido",
    "Isaias":           "Izaijo",
    "Jeremias":         "Jeremijo",
    "Lamentations":     "Raudų",
    "Baruch":           "Barucho",
    "Ezechiel":         "Ezekielio",
    "Daniel":           "Danieliaus",
    "Osee":             "Ozėjo",
    "Joel":             "Joelio",
    "Amos":             "Amoso",
    "Abdias":           "Abdijo",
    "Jonas":            "Jonos",
    "Micheas":          "Michėjo",
    "Nahum":            "Nahumo",
    "Habacuc":          "Habakuko",
    "Sophonias":        "Sofonijo",
    "Aggeus":           "Agėjo",
    "Zacharias":        "Zacharijo",
    "Malachias":        "Malachijo",
    "Matthew":          "Mato",
    "Mark":             "Morkaus",
    "Luke":             "Luko",
    "John":             "Jono",
    "Acts":             "Apaštalų darbai",
    "Romans":           "Romiečiams",
    "1 Corinthians":    "1 Korintiečiams",
    "2 Corinthians":    "2 Korintiečiams",
    "Galatians":        "Galatams",
    "Ephesians":        "Efeziečiams",
    "Philippians":      "Filipiečiams",
    "Colossians":       "Kolosiečiams",
    "1 Thessalonians":  "1 Tesalonikiečiams",
    "2 Thessalonians":  "2 Tesalonikiečiams",
    "1 Timothy":        "1 Timotiejui",
    "2 Timothy":        "2 Timotiejui",
    "Titus":            "Titui",
    "Philemon":         "Filemonui",
    "Hebrews":          "Žydams",
    "James":            "Jokūbo",
    "1 Peter":          "1 Petro",
    "2 Peter":          "2 Petro",
    "1 John":           "1 Jono",
    "2 John":           "2 Jono",
    "3 John":           "3 Jono",
    "Jude":             "Judo",
    "Apocalypse":       "Apreiškimo",
}


def fetch(url: str) -> str:
    # Percent-encode non-ASCII characters in the URL (e.g. Lithuanian book codes like Iš, Žyd)
    encoded = urllib.parse.quote(url, safe=":/?=&")
    req = urllib.request.Request(encoded, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req, timeout=30) as r:
        return r.read().decode("utf-8", errors="replace")


def parse_chapter(html: str, book_code: str, chapter: int) -> dict[str, str]:
    """Extract verse number → text from a chapter page."""
    # Grab all bibl paragraphs as the chapter content
    paragraphs = re.findall(r'<p class="bibl">(.*?)</p>', html, re.DOTALL)
    content = " ".join(paragraphs)

    # Remove <a>...</a> blocks entirely (footnote link text lives here)
    content = re.sub(r'<a[\s\S]*?</a>', '', content)
    # Remove any leftover [iN] markers
    content = re.sub(r'\[i\d+\]', '', content)
    # Remove section heading spans
    content = re.sub(r'<span[\s\S]*?</span>', '', content)
    # Remove all remaining tags EXCEPT <sup> and </sup> (verse-number markers)
    content = re.sub(r'<(?!/?sup\b)[^>]*>', ' ', content)
    # Decode entities
    content = content.replace('&nbsp;', ' ')
    content = re.sub(r'&#\d+;', '', content)
    content = re.sub(r'&[a-zA-Z]+;', '', content)

    # Split on verse number markers <sup>N</sup>
    parts = re.split(r'<sup>\s*(\d+)\s*</sup>', content)

    verses: dict[str, str] = {}
    i = 1
    while i < len(parts) - 1:
        verse_num = parts[i]
        verse_text = parts[i + 1]
        verse_text = re.sub(r'<sup>\s*</sup>', '', verse_text)
        verse_text = re.sub(r'\s+', ' ', verse_text).strip()
        if verse_text:
            verses[verse_num] = verse_text
        i += 2

    return verses


def get_toc_chapters(html: str) -> dict[str, list[int]]:
    """Extract {book_code: [chapter_numbers]} from the TOC page."""
    links = re.findall(r'cmp=reading&doc=BiblijaRKK1998_([^"&]+)', html)
    books: dict[str, list[int]] = {}
    for link in links:
        parts = link.rsplit('_', 1)
        if len(parts) == 2:
            book_code, chapter = parts
            try:
                books.setdefault(book_code, []).append(int(chapter))
            except ValueError:
                pass
    return {k: sorted(set(v)) for k, v in books.items()}


def main(delay: float = 1.0):
    os.makedirs(os.path.join(OUT_DIR, "books"), exist_ok=True)

    print("Fetching TOC...")
    toc_html = fetch("https://biblija.lt/index.aspx?cmp=toc")
    toc = get_toc_chapters(toc_html)
    print(f"Found {len(toc)} books.")

    all_books: dict[str, dict[str, dict[str, str]]] = {}

    for lt_code, chapters in toc.items():
        dr_name = LT_TO_DR.get(lt_code)
        if dr_name is None:
            print(f"  SKIP unknown code: {lt_code!r}")
            continue

        print(f"  {dr_name} ({lt_code}): {len(chapters)} chapters")
        book_data: dict[str, dict[str, str]] = {}

        for chapter in chapters:
            url = BASE_URL.format(book=lt_code, chapter=chapter)
            try:
                html = fetch(url)
                verses = parse_chapter(html, lt_code, chapter)
                if verses:
                    book_data[str(chapter)] = verses
                else:
                    print(f"    WARNING: no verses found for {lt_code} ch {chapter}")
            except Exception as e:
                print(f"    ERROR fetching {url}: {e}")
            time.sleep(delay)

        if book_data:
            out_path = os.path.join(OUT_DIR, "books", f"{dr_name}.json")
            with open(out_path, "w", encoding="utf-8") as f:
                json.dump(book_data, f, ensure_ascii=False, indent=1)
            total = sum(len(v) for v in book_data.values())
            print(f"    → {total} verses written")
        else:
            print(f"    WARNING: no data for {dr_name}")

    # metadata
    metadata = {
        "id": "rk1998",
        "name": "Biblija (RK, K1998)",
        "language": "lt",
        "license": "© Lietuvos Vyskupų Konferencija",
    }
    with open(os.path.join(OUT_DIR, "metadata.json"), "w", encoding="utf-8") as f:
        json.dump(metadata, f, ensure_ascii=False, indent=2)

    print(f"\nDone. Output: {OUT_DIR}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--delay", type=float, default=1.0,
                        help="Seconds to wait between requests (default: 1.0)")
    args = parser.parse_args()
    main(args.delay)
