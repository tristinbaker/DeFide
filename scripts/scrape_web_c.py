#!/usr/bin/env python3
"""
scrape_web_c.py — Scrapes the World English Bible (Catholic) from ebible.org
and saves each book as a per-chapter JSON file matching the existing format.

Outputs:
    content/bible/web-c/metadata.json
    content/bible/web-c/books/<DR-name>.json

Usage:
    python scripts/scrape_web_c.py
"""

import html
import json
import os
import re
import sys
import time
import urllib.request
from urllib.error import URLError

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUTPUT_DIR = os.path.join(REPO_ROOT, "content", "bible", "web-c")
BASE_URL = "https://ebible.org/eng-web-c/"

# Delay between HTTP requests (seconds) — be polite to the server
REQUEST_DELAY = 0.4

# ---------------------------------------------------------------------------
# Mapping from ebible.org 3-letter codes → DR file names (as used by compile_content.py)
# Codes verified against the live index at https://ebible.org/eng-web-c/
# ---------------------------------------------------------------------------
CODE_TO_DR = {
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
    "ESG": "Esther",       # Greek Esther (full Catholic canon)
    "1MA": "1 Machabees",
    "2MA": "2 Machabees",
    "JOB": "Job",
    "PSA": "Psalms",
    "PRO": "Proverbs",
    "ECC": "Ecclesiastes",
    "SNG": "Canticles",
    "WIS": "Wisdom",
    "SIR": "Ecclesiasticus",
    "ISA": "Isaias",
    "JER": "Jeremias",
    "LAM": "Lamentations",
    "BAR": "Baruch",
    "EZK": "Ezechiel",
    "DAG": "Daniel",       # Greek Daniel (full Catholic canon with additions)
    "HOS": "Osee",
    "JOL": "Joel",
    "AMO": "Amos",
    "OBA": "Abdias",
    "JON": "Jonas",
    "MIC": "Micheas",
    "NAM": "Nahum",        # ebible uses NAM not NAH
    "HAB": "Habacuc",
    "ZEP": "Sophonias",
    "HAG": "Aggeus",
    "ZEC": "Zacharias",
    "MAL": "Malachias",
    "MAT": "Matthew",
    "MRK": "Mark",
    "LUK": "Luke",
    "JHN": "John",
    "ACT": "Acts",
    "ROM": "Romans",
    "1CO": "1 Corinthians",
    "2CO": "2 Corinthians",
    "GAL": "Galatians",
    "EPH": "Ephesians",
    "PHP": "Philippians",
    "COL": "Colossians",
    "1TH": "1 Thessalonians",
    "2TH": "2 Thessalonians",
    "1TI": "1 Timothy",
    "2TI": "2 Timothy",
    "TIT": "Titus",
    "PHM": "Philemon",
    "HEB": "Hebrews",
    "JAS": "James",
    "1PE": "1 Peter",
    "2PE": "2 Peter",
    "1JN": "1 John",
    "2JN": "2 John",
    "3JN": "3 John",
    "JUD": "Jude",
    "REV": "Apocalypse",
}


def fetch(path: str) -> str:
    url = BASE_URL + path
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
        with urllib.request.urlopen(req, timeout=30) as resp:
            return resp.read().decode("utf-8", errors="replace")
    except URLError as e:
        print(f"  ERROR fetching {url}: {e}", file=sys.stderr)
        return ""


def extract_book_list(index_html: str) -> list[dict]:
    """
    Parse the index page and return a list of {code, display_name} dicts.
    Each entry is unique by code (first chapter link per book).
    """
    books = []
    seen = set()
    # Links look like: <a class='oo' href='GEN01.htm'>Genesis</a>
    for m in re.finditer(
        r"<a\s+[^>]*href=['\"]([A-Z0-9]{3,4}\d+\.htm)['\"][^>]*>([^<]+)</a>",
        index_html,
    ):
        href, name = m.group(1), m.group(2).strip()
        # Extract book code: strip trailing digits and .htm
        code = re.sub(r"\d+\.htm$", "", href)
        if code and code not in seen:
            seen.add(code)
            books.append({"code": code, "name": name})
    return books


def extract_chapter_links(chapter_index_html: str, book_code: str) -> list[str]:
    """
    Parse a book's chapter-index page (e.g. GEN.htm) and return sorted chapter hrefs.
    """
    pattern = re.compile(
        rf"href=['\"]({re.escape(book_code)}\d+\.htm)['\"]", re.IGNORECASE
    )
    links = list(dict.fromkeys(pattern.findall(chapter_index_html)))  # preserve order, dedupe
    return sorted(links, key=lambda h: int(re.search(r"\d+", h).group()))


def chapter_number_from_href(href: str, book_code: str) -> int:
    """GEN01.htm -> 1,  PSA001.htm -> 1"""
    num_str = re.sub(rf"^{re.escape(book_code)}", "", href, flags=re.IGNORECASE)
    num_str = num_str.replace(".htm", "")
    return int(num_str)


# Matches a verse span: <span class="verse" id="V3">3 </span>
_VERSE_SPAN_RE = re.compile(
    r'<span[^>]+class=["\']verse["\'][^>]+id=["\']V(\d+)["\'][^>]*>.*?</span>',
    re.DOTALL | re.IGNORECASE,
)

# Matches any remaining HTML tag
_TAG_RE = re.compile(r"<[^>]+>", re.DOTALL)


def extract_verses(chapter_html: str) -> dict[str, str]:
    """
    Extract {verse_number_str: verse_text} from a chapter page.
    Strategy:
      0. Isolate the main content between the top and bottom tnav blocks.
      1. Strip footnote divs and notemark anchors.
      2. Replace verse spans with unique markers.
      3. Strip all remaining HTML.
      4. Split text by markers and collect verse text.
    """
    # 0. Isolate main content (between top nav and bottom nav)
    #    Page layout: [header] <ul class='tnav'> [content] <ul class='tnav'> [footer]
    nav_parts = re.split(
        r"<ul[^>]+class=['\"]tnav['\"][^>]*>.*?</ul>",
        chapter_html,
        flags=re.DOTALL | re.IGNORECASE,
    )
    # nav_parts[0]=header, nav_parts[1]=content, nav_parts[2]=footer
    if len(nav_parts) >= 2:
        chapter_html = nav_parts[1]

    # 1. Remove footnote sections entirely
    chapter_html = re.sub(
        r'<div[^>]+class=["\']footnote["\'][^>]*>.*?</div>',
        "",
        chapter_html,
        flags=re.DOTALL | re.IGNORECASE,
    )
    chapter_html = re.sub(
        r'<div[^>]+class=["\']notes["\'][^>]*>.*?</div>',
        "",
        chapter_html,
        flags=re.DOTALL | re.IGNORECASE,
    )
    # Remove notemark anchors (including their popup spans)
    chapter_html = re.sub(
        r'<a[^>]+class=["\']notemark["\'][^>]*>.*?</a>',
        "",
        chapter_html,
        flags=re.DOTALL | re.IGNORECASE,
    )

    # 2. Replace verse spans with markers  (V0 = chapter label, skip later)
    chapter_html = _VERSE_SPAN_RE.sub(lambda m: f"\x00VERSE{m.group(1)}\x00", chapter_html)

    # 3. Strip remaining tags, decode entities
    chapter_html = _TAG_RE.sub(" ", chapter_html)
    chapter_html = html.unescape(chapter_html)

    # 4. Split by markers
    parts = re.split(r"\x00VERSE(\d+)\x00", chapter_html)
    # parts = [pre_text, verse_num, verse_text, verse_num, verse_text, ...]

    verses: dict[str, str] = {}
    i = 1
    while i < len(parts) - 1:
        verse_num = parts[i]
        verse_text = parts[i + 1]
        # Normalize whitespace
        verse_text = " ".join(verse_text.split()).strip()
        # Skip V0 (chapter header) and empty verses
        if verse_num != "0" and verse_text:
            verses[verse_num] = verse_text
        i += 2

    return verses


def main() -> None:
    os.makedirs(os.path.join(OUTPUT_DIR, "books"), exist_ok=True)

    # Write metadata
    metadata = {
        "id": "web-c",
        "name": "World English Bible (Catholic)",
        "language": "en",
        "license": "public domain",
    }
    meta_path = os.path.join(OUTPUT_DIR, "metadata.json")
    with open(meta_path, "w", encoding="utf-8") as f:
        json.dump(metadata, f, indent=2)
    print(f"Wrote {meta_path}")

    print("Fetching book list from index...")
    index_html = fetch("index.htm")
    if not index_html:
        print("Failed to fetch index page. Aborting.", file=sys.stderr)
        sys.exit(1)

    books = extract_book_list(index_html)
    print(f"Found {len(books)} books on index page.\n")

    skipped = []

    for book in books:
        code = book["code"]
        display_name = book["name"]

        dr_name = CODE_TO_DR.get(code)

        if dr_name is None:
            print(f"  SKIP: no DR mapping for code '{code}' ({display_name})")
            skipped.append(code)
            continue

        print(f"Processing {dr_name} ({code})...")

        # Fetch chapter list
        time.sleep(REQUEST_DELAY)
        chapter_index_html = fetch(f"{code}.htm")
        if not chapter_index_html:
            print(f"  WARNING: could not fetch chapter index for {code}")
            skipped.append(code)
            continue

        chapter_links = extract_chapter_links(chapter_index_html, code)
        if not chapter_links:
            print(f"  WARNING: no chapter links found for {code}")
            skipped.append(code)
            continue

        print(f"  {len(chapter_links)} chapters")
        book_data: dict[str, dict[str, str]] = {}

        for href in chapter_links:
            chapter_num = chapter_number_from_href(href, code)
            time.sleep(REQUEST_DELAY)
            chapter_html = fetch(href)
            if not chapter_html:
                print(f"    WARNING: could not fetch {href}")
                continue

            verses = extract_verses(chapter_html)
            if not verses:
                print(f"    WARNING: no verses found in {href}")
            else:
                book_data[str(chapter_num)] = verses
                print(f"    Chapter {chapter_num}: {len(verses)} verses")

        if book_data:
            out_path = os.path.join(OUTPUT_DIR, "books", f"{dr_name}.json")
            with open(out_path, "w", encoding="utf-8") as f:
                json.dump(book_data, f, indent=1, ensure_ascii=False)
            print(f"  -> Saved {out_path}")
        else:
            print(f"  WARNING: no data collected for {dr_name}")
            skipped.append(code)

    print("\nDone.")
    if skipped:
        print(f"Skipped/failed books: {skipped}")


if __name__ == "__main__":
    main()
