#!/usr/bin/env python3
"""
compile_content.py — Builds defide_content.db from source JSON files.

Usage:
    python scripts/compile_content.py

Output:
    app/src/main/assets/databases/defide_content.db

Expected source layout:
    content/bible/dra/metadata.json
    content/bible/dra/books/<BookName>.json   (one per book, DR naming)
    content/prayers/prayers.json
    content/novenas/novenas.json
    content/catechism/ccc_paragraphs.json
"""

import json
import os
import re
import sqlite3
import sys

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CONTENT_DIR = os.path.join(REPO_ROOT, "content")
OUT_DB = os.path.join(REPO_ROOT, "app", "src", "main", "assets", "databases", "defide_content.db")

# ---------------------------------------------------------------------------
# Canonical Catholic book manifest
# Keys are the DR file names (without .json).
# Values: (book_number, testament, short_name, full_name)
# ---------------------------------------------------------------------------
BOOK_MANIFEST = {
    "Genesis":          (1,  "OT", "Gen",    "Genesis"),
    "Exodus":           (2,  "OT", "Ex",     "Exodus"),
    "Leviticus":        (3,  "OT", "Lev",    "Leviticus"),
    "Numbers":          (4,  "OT", "Num",    "Numbers"),
    "Deuteronomy":      (5,  "OT", "Deut",   "Deuteronomy"),
    "Josue":            (6,  "OT", "Josh",   "Joshua"),
    "Judges":           (7,  "OT", "Judg",   "Judges"),
    "Ruth":             (8,  "OT", "Ruth",   "Ruth"),
    "1 Kings":          (9,  "OT", "1 Sam",  "1 Samuel"),
    "2 Kings":          (10, "OT", "2 Sam",  "2 Samuel"),
    "3 Kings":          (11, "OT", "1 Kgs",  "1 Kings"),
    "4 Kings":          (12, "OT", "2 Kgs",  "2 Kings"),
    "1 Paralipomenon":  (13, "OT", "1 Chr",  "1 Chronicles"),
    "2 Paralipomenon":  (14, "OT", "2 Chr",  "2 Chronicles"),
    "1 Esdras":         (15, "OT", "Ezra",   "Ezra"),
    "2 Esdras":         (16, "OT", "Neh",    "Nehemiah"),
    "Tobias":           (17, "DC", "Tob",    "Tobit"),
    "Judith":           (18, "DC", "Jdt",    "Judith"),
    "Esther":           (19, "OT", "Esth",   "Esther"),
    "1 Machabees":      (20, "DC", "1 Mac",  "1 Maccabees"),
    "2 Machabees":      (21, "DC", "2 Mac",  "2 Maccabees"),
    "Job":              (22, "OT", "Job",    "Job"),
    "Psalms":           (23, "OT", "Ps",     "Psalms"),
    "Proverbs":         (24, "OT", "Prov",   "Proverbs"),
    "Ecclesiastes":     (25, "OT", "Eccl",   "Ecclesiastes"),
    "Canticles":        (26, "OT", "Song",   "Song of Songs"),
    "Wisdom":           (27, "DC", "Wis",    "Wisdom"),
    "Ecclesiasticus":   (28, "DC", "Sir",    "Sirach"),
    "Isaias":           (29, "OT", "Isa",    "Isaiah"),
    "Jeremias":         (30, "OT", "Jer",    "Jeremiah"),
    "Lamentations":     (31, "OT", "Lam",    "Lamentations"),
    "Baruch":           (32, "DC", "Bar",    "Baruch"),
    "Ezechiel":         (33, "OT", "Ezek",   "Ezekiel"),
    "Daniel":           (34, "OT", "Dan",    "Daniel"),
    "Osee":             (35, "OT", "Hos",    "Hosea"),
    "Joel":             (36, "OT", "Joel",   "Joel"),
    "Amos":             (37, "OT", "Amos",   "Amos"),
    "Abdias":           (38, "OT", "Obad",   "Obadiah"),
    "Jonas":            (39, "OT", "Jon",    "Jonah"),
    "Micheas":          (40, "OT", "Mic",    "Micah"),
    "Nahum":            (41, "OT", "Nah",    "Nahum"),
    "Habacuc":          (42, "OT", "Hab",    "Habakkuk"),
    "Sophonias":        (43, "OT", "Zeph",   "Zephaniah"),
    "Aggeus":           (44, "OT", "Hag",    "Haggai"),
    "Zacharias":        (45, "OT", "Zech",   "Zechariah"),
    "Malachias":        (46, "OT", "Mal",    "Malachi"),
    "Matthew":          (47, "NT", "Matt",   "Matthew"),
    "Mark":             (48, "NT", "Mark",   "Mark"),
    "Luke":             (49, "NT", "Luke",   "Luke"),
    "John":             (50, "NT", "John",   "John"),
    "Acts":             (51, "NT", "Acts",   "Acts"),
    "Romans":           (52, "NT", "Rom",    "Romans"),
    "1 Corinthians":    (53, "NT", "1 Cor",  "1 Corinthians"),
    "2 Corinthians":    (54, "NT", "2 Cor",  "2 Corinthians"),
    "Galatians":        (55, "NT", "Gal",    "Galatians"),
    "Ephesians":        (56, "NT", "Eph",    "Ephesians"),
    "Philippians":      (57, "NT", "Phil",   "Philippians"),
    "Colossians":       (58, "NT", "Col",    "Colossians"),
    "1 Thessalonians":  (59, "NT", "1 Thess","1 Thessalonians"),
    "2 Thessalonians":  (60, "NT", "2 Thess","2 Thessalonians"),
    "1 Timothy":        (61, "NT", "1 Tim",  "1 Timothy"),
    "2 Timothy":        (62, "NT", "2 Tim",  "2 Timothy"),
    "Titus":            (63, "NT", "Titus",  "Titus"),
    "Philemon":         (64, "NT", "Phlm",   "Philemon"),
    "Hebrews":          (65, "NT", "Heb",    "Hebrews"),
    "James":            (66, "NT", "Jas",    "James"),
    "1 Peter":          (67, "NT", "1 Pet",  "1 Peter"),
    "2 Peter":          (68, "NT", "2 Pet",  "2 Peter"),
    "1 John":           (69, "NT", "1 Jn",   "1 John"),
    "2 John":           (70, "NT", "2 Jn",   "2 John"),
    "3 John":           (71, "NT", "3 Jn",   "3 John"),
    "Jude":             (72, "NT", "Jude",   "Jude"),
    "Apocalypse":       (73, "NT", "Rev",    "Revelation"),
}


# ---------------------------------------------------------------------------
# NRSVCE book manifest
# Keys are the book names as they appear in nrsvce.json.
# Values: (book_number, testament, short_name, full_name, dr_name)
# book_numbers mirror the DRA manifest; extra DC-only books get 74-77.
# ---------------------------------------------------------------------------
NRSVCE_BOOK_MANIFEST = {
    # Old Testament
    "Genesis":           (1,  "OT", "Gen",       "Genesis",              "Genesis"),
    "Exodus":            (2,  "OT", "Ex",         "Exodus",               "Exodus"),
    "Leviticus":         (3,  "OT", "Lev",        "Leviticus",            "Leviticus"),
    "Numbers":           (4,  "OT", "Num",        "Numbers",              "Numbers"),
    "Deuteronomy":       (5,  "OT", "Deut",       "Deuteronomy",          "Deuteronomy"),
    "Joshua":            (6,  "OT", "Josh",       "Joshua",               "Joshua"),
    "Judges":            (7,  "OT", "Judg",       "Judges",               "Judges"),
    "Ruth":              (8,  "OT", "Ruth",       "Ruth",                 "Ruth"),
    "1 Samuel":          (9,  "OT", "1 Sam",      "1 Samuel",             "1 Samuel"),
    "2 Samuel":          (10, "OT", "2 Sam",      "2 Samuel",             "2 Samuel"),
    "1 Kings":           (11, "OT", "1 Kgs",      "1 Kings",              "1 Kings"),
    "2 Kings":           (12, "OT", "2 Kgs",      "2 Kings",              "2 Kings"),
    "1 Chronicles":      (13, "OT", "1 Chr",      "1 Chronicles",         "1 Chronicles"),
    "2 Chronicles":      (14, "OT", "2 Chr",      "2 Chronicles",         "2 Chronicles"),
    "Ezra":              (15, "OT", "Ezra",       "Ezra",                 "Ezra"),
    "Nehemiah":          (16, "OT", "Neh",        "Nehemiah",             "Nehemiah"),
    "Esther":            (19, "OT", "Esth",       "Esther",               "Esther"),
    "Job":               (22, "OT", "Job",        "Job",                  "Job"),
    "Psalms":            (23, "OT", "Ps",         "Psalms",               "Psalms"),
    "Proverbs":          (24, "OT", "Prov",       "Proverbs",             "Proverbs"),
    "Ecclesiastes":      (25, "OT", "Eccl",       "Ecclesiastes",         "Ecclesiastes"),
    "Song of Solomon":   (26, "OT", "Song",       "Song of Songs",        "Song of Solomon"),
    "Isaiah":            (29, "OT", "Isa",        "Isaiah",               "Isaiah"),
    "Jeremiah":          (30, "OT", "Jer",        "Jeremiah",             "Jeremiah"),
    "Lamentations":      (31, "OT", "Lam",        "Lamentations",         "Lamentations"),
    "Ezekiel":           (33, "OT", "Ezek",       "Ezekiel",              "Ezekiel"),
    "Daniel":            (34, "OT", "Dan",        "Daniel",               "Daniel"),
    "Hosea":             (35, "OT", "Hos",        "Hosea",                "Hosea"),
    "Joel":              (36, "OT", "Joel",       "Joel",                 "Joel"),
    "Amos":              (37, "OT", "Amos",       "Amos",                 "Amos"),
    "Obadiah":           (38, "OT", "Obad",       "Obadiah",              "Obadiah"),
    "Jonah":             (39, "OT", "Jon",        "Jonah",                "Jonah"),
    "Micah":             (40, "OT", "Mic",        "Micah",                "Micah"),
    "Nahum":             (41, "OT", "Nah",        "Nahum",                "Nahum"),
    "Habakkuk":          (42, "OT", "Hab",        "Habakkuk",             "Habakkuk"),
    "Zephaniah":         (43, "OT", "Zeph",       "Zephaniah",            "Zephaniah"),
    "Haggai":            (44, "OT", "Hag",        "Haggai",               "Haggai"),
    "Zechariah":         (45, "OT", "Zech",       "Zechariah",            "Zechariah"),
    "Malachi":           (46, "OT", "Mal",        "Malachi",              "Malachi"),
    # Deuterocanonical
    "Tobit":             (17, "DC", "Tob",        "Tobit",                "Tobit"),
    "Judith":            (18, "DC", "Jdt",        "Judith",               "Judith"),
    "Greek Esther":      (74, "DC", "Grk Esth",   "Greek Esther",         "Greek Esther"),
    "1 Maccabees":       (20, "DC", "1 Mac",      "1 Maccabees",          "1 Maccabees"),
    "2 Maccabees":       (21, "DC", "2 Mac",      "2 Maccabees",          "2 Maccabees"),
    "Wisdom":            (27, "DC", "Wis",        "Wisdom",               "Wisdom"),
    "Sirach":            (28, "DC", "Sir",        "Sirach",               "Sirach"),
    "Baruch":            (32, "DC", "Bar",        "Baruch",               "Baruch"),
    "Prayer Of Azariah": (75, "DC", "Pr Azar",    "Prayer of Azariah",    "Prayer Of Azariah"),
    "Susanna":           (76, "DC", "Sus",        "Susanna",              "Susanna"),
    "Bel And The Dragon":(77, "DC", "Bel",        "Bel and the Dragon",   "Bel And The Dragon"),
    # New Testament
    "Matthew":           (47, "NT", "Matt",       "Matthew",              "Matthew"),
    "Mark":              (48, "NT", "Mark",       "Mark",                 "Mark"),
    "Luke":              (49, "NT", "Luke",       "Luke",                 "Luke"),
    "John":              (50, "NT", "John",       "John",                 "John"),
    "Acts":              (51, "NT", "Acts",       "Acts",                 "Acts"),
    "Romans":            (52, "NT", "Rom",        "Romans",               "Romans"),
    "1 Corinthians":     (53, "NT", "1 Cor",      "1 Corinthians",        "1 Corinthians"),
    "2 Corinthians":     (54, "NT", "2 Cor",      "2 Corinthians",        "2 Corinthians"),
    "Galatians":         (55, "NT", "Gal",        "Galatians",            "Galatians"),
    "Ephesians":         (56, "NT", "Eph",        "Ephesians",            "Ephesians"),
    "Philippians":       (57, "NT", "Phil",       "Philippians",          "Philippians"),
    "Colossians":        (58, "NT", "Col",        "Colossians",           "Colossians"),
    "1 Thessalonians":   (59, "NT", "1 Thess",    "1 Thessalonians",      "1 Thessalonians"),
    "2 Thessalonians":   (60, "NT", "2 Thess",    "2 Thessalonians",      "2 Thessalonians"),
    "1 Timothy":         (61, "NT", "1 Tim",      "1 Timothy",            "1 Timothy"),
    "2 Timothy":         (62, "NT", "2 Tim",      "2 Timothy",            "2 Timothy"),
    "Titus":             (63, "NT", "Titus",      "Titus",                "Titus"),
    "Philemon":          (64, "NT", "Phlm",       "Philemon",             "Philemon"),
    "Hebrews":           (65, "NT", "Heb",        "Hebrews",              "Hebrews"),
    "James":             (66, "NT", "Jas",        "James",                "James"),
    "1 Peter":           (67, "NT", "1 Pet",      "1 Peter",              "1 Peter"),
    "2 Peter":           (68, "NT", "2 Pet",      "2 Peter",              "2 Peter"),
    "1 John":            (69, "NT", "1 Jn",       "1 John",               "1 John"),
    "2 John":            (70, "NT", "2 Jn",       "2 John",               "2 John"),
    "3 John":            (71, "NT", "3 Jn",       "3 John",               "3 John"),
    "Jude":              (72, "NT", "Jude",       "Jude",                 "Jude"),
    "Revelation":        (73, "NT", "Rev",        "Revelation",           "Revelation"),
}

# Superscript digits Unicode → strip from start of verse text
_SUPERSCRIPT_RE = re.compile(r'^[⁰¹²³⁴⁵⁶⁷⁸⁹]+\s*')


def _clean_nrsvce_verse(text: str) -> str:
    """Strip leading superscript verse number and normalize whitespace."""
    return _SUPERSCRIPT_RE.sub('', text).strip()


def create_schema(conn: sqlite3.Connection) -> None:
    conn.executescript("""
        PRAGMA journal_mode=WAL;

        -- Bible
        CREATE TABLE IF NOT EXISTS translations (
            id      TEXT PRIMARY KEY,
            name    TEXT NOT NULL,
            language TEXT NOT NULL,
            license TEXT NOT NULL
        );

        CREATE TABLE IF NOT EXISTS books (
            id              INTEGER PRIMARY KEY,
            translation_id  TEXT NOT NULL,
            book_number     INTEGER NOT NULL,
            testament       TEXT NOT NULL,
            short_name      TEXT NOT NULL,
            full_name       TEXT NOT NULL,
            dr_name         TEXT NOT NULL,
            FOREIGN KEY (translation_id) REFERENCES translations(id)
        );

        CREATE TABLE IF NOT EXISTS verses (
            id      INTEGER PRIMARY KEY,
            book_id INTEGER NOT NULL,
            chapter INTEGER NOT NULL,
            verse   INTEGER NOT NULL,
            text    TEXT NOT NULL,
            FOREIGN KEY (book_id) REFERENCES books(id)
        );

        CREATE VIRTUAL TABLE IF NOT EXISTS verses_fts USING fts4(
            content="verses",
            text
        );

        -- Catechism
        CREATE TABLE IF NOT EXISTS ccc_sections (
            id      INTEGER PRIMARY KEY,
            part    INTEGER,
            section INTEGER,
            chapter INTEGER,
            article INTEGER,
            heading TEXT,
            body    TEXT NOT NULL
        );

        CREATE VIRTUAL TABLE IF NOT EXISTS ccc_fts USING fts4(
            content="ccc_sections",
            heading, body
        );

        -- Prayers
        CREATE TABLE IF NOT EXISTS prayers (
            id       TEXT PRIMARY KEY,
            title    TEXT NOT NULL,
            body     TEXT NOT NULL,
            source   TEXT,
            category TEXT NOT NULL
        );

        CREATE TABLE IF NOT EXISTS prayer_tags (
            prayer_id TEXT NOT NULL,
            tag       TEXT NOT NULL,
            PRIMARY KEY (prayer_id, tag),
            FOREIGN KEY (prayer_id) REFERENCES prayers(id)
        );

        CREATE VIRTUAL TABLE IF NOT EXISTS prayers_fts USING fts4(
            content="prayers",
            title, body
        );

        -- Novenas
        CREATE TABLE IF NOT EXISTS novenas (
            id          TEXT PRIMARY KEY,
            title       TEXT NOT NULL,
            description TEXT,
            total_days  INTEGER NOT NULL DEFAULT 9,
            feast_day   TEXT
        );

        CREATE TABLE IF NOT EXISTS novena_days (
            id         INTEGER PRIMARY KEY,
            novena_id  TEXT NOT NULL,
            day_number INTEGER NOT NULL,
            title      TEXT,
            body       TEXT NOT NULL,
            FOREIGN KEY (novena_id) REFERENCES novenas(id)
        );

        -- Rosary
        CREATE TABLE IF NOT EXISTS mysteries (
            id               TEXT PRIMARY KEY,
            name             TEXT NOT NULL,
            traditional_days TEXT
        );

        CREATE TABLE IF NOT EXISTS mystery_beads (
            id                 INTEGER PRIMARY KEY,
            mystery_id         TEXT NOT NULL,
            position           INTEGER NOT NULL,
            prayer_id          TEXT,
            mystery_number     INTEGER,
            mystery_title      TEXT,
            mystery_scripture  TEXT,
            mystery_meditation TEXT,
            FOREIGN KEY (mystery_id) REFERENCES mysteries(id)
        );

        -- Indexes for fast chapter / book lookups
        CREATE INDEX IF NOT EXISTS idx_verses_book_chapter ON verses(book_id, chapter);
        CREATE INDEX IF NOT EXISTS idx_books_translation ON books(translation_id, book_number);
        CREATE INDEX IF NOT EXISTS idx_mystery_beads_mystery ON mystery_beads(mystery_id, position);
        CREATE INDEX IF NOT EXISTS idx_novena_days_novena ON novena_days(novena_id, day_number);
    """)


# Starting book_id offsets per translation so IDs never collide across translations.
# DRA: 1–999, NRSVCE: 1001–1999, Vulgate: 2001–2999, Vulgate-ET: 3001–3999
_TRANSLATION_BOOK_ID_OFFSET = {
    "dra":        1,
    "vulgate":    2001,
    "vulgate-et": 3001,
}


def compile_dr_format(conn: sqlite3.Connection, translation_id: str) -> None:
    """
    Ingests any translation stored in the DRA per-book folder format:
        content/bible/<translation_id>/metadata.json
        content/bible/<translation_id>/books/<DR-name>.json
    Book files must be named using BOOK_MANIFEST keys.
    """
    translation_dir = os.path.join(CONTENT_DIR, "bible", translation_id)
    books_dir = os.path.join(translation_dir, "books")
    meta_path = os.path.join(translation_dir, "metadata.json")

    if not os.path.isdir(books_dir):
        print(f"  SKIP: {books_dir} not found.")
        return

    with open(meta_path) as f:
        meta = json.load(f)

    conn.execute(
        "INSERT OR REPLACE INTO translations VALUES (?, ?, ?, ?)",
        (meta["id"], meta["name"], meta["language"], meta["license"]),
    )

    book_files = sorted(
        [fn for fn in os.listdir(books_dir) if fn.endswith(".json")],
        key=lambda fn: BOOK_MANIFEST.get(fn[:-5], (999,))[0],
    )

    verse_rows = []
    book_id_counter = _TRANSLATION_BOOK_ID_OFFSET.get(translation_id, 1)

    for filename in book_files:
        dr_name = filename[:-5]
        if dr_name not in BOOK_MANIFEST:
            print(f"  WARN: unknown book file '{filename}' — skipping")
            continue

        book_number, testament, short_name, full_name = BOOK_MANIFEST[dr_name]
        book_id = book_id_counter
        book_id_counter += 1

        conn.execute(
            "INSERT INTO books VALUES (?, ?, ?, ?, ?, ?, ?)",
            (book_id, meta["id"], book_number, testament, short_name, full_name, dr_name),
        )

        with open(os.path.join(books_dir, filename)) as f:
            data = json.load(f)

        for chapter_str, verses in sorted(data.items(), key=lambda x: int(x[0])):
            for verse_str, text in sorted(verses.items(), key=lambda x: int(x[0])):
                verse_rows.append((book_id, int(chapter_str), int(verse_str), text))

        print(f"  {book_number:>2}. {full_name} ({len(verse_rows)} verses so far)")

    conn.executemany(
        "INSERT INTO verses (book_id, chapter, verse, text) VALUES (?, ?, ?, ?)",
        verse_rows,
    )
    conn.execute("INSERT INTO verses_fts(verses_fts) VALUES('rebuild')")
    print(f"  {meta['name']}: {len(verse_rows)} total verses indexed.")


def compile_nrsvce(conn: sqlite3.Connection) -> None:
    nrsvce_path = os.path.join(CONTENT_DIR, "bible", "nrsvce", "nrsvce.json")
    if not os.path.exists(nrsvce_path):
        print(f"  SKIP: {nrsvce_path} not found.")
        return

    conn.execute(
        "INSERT OR REPLACE INTO translations VALUES (?, ?, ?, ?)",
        ("nrsvce", "New Revised Standard Version Catholic Edition", "en", "© 1989, 1993 NCC; Catholic edition © 1993, 2021 USCCB"),
    )

    with open(nrsvce_path) as f:
        data = json.load(f)

    # testament key order in the JSON: Old Testament, Deuterocanonical, New Testament
    testament_map = {
        "Old Testament": "OT",
        "Deuterocanonical": "DC",
        "New Testament": "NT",
    }

    verse_rows = []
    # NRSVCE book IDs start at 1001 to avoid collisions with DRA (1–73)
    book_id_counter = 1001

    # Sort all books by their book_number from the manifest
    all_books = []
    for testament_label, books in data.items():
        for book_name, chapters in books.items():
            if book_name not in NRSVCE_BOOK_MANIFEST:
                print(f"  WARN: unknown NRSVCE book '{book_name}' — skipping")
                continue
            all_books.append((NRSVCE_BOOK_MANIFEST[book_name][0], book_name, chapters))
    all_books.sort(key=lambda x: x[0])

    for book_number, book_name, chapters in all_books:
        entry = NRSVCE_BOOK_MANIFEST[book_name]
        _, testament, short_name, full_name, dr_name = entry
        book_id = book_id_counter
        book_id_counter += 1

        conn.execute(
            "INSERT INTO books VALUES (?, ?, ?, ?, ?, ?, ?)",
            (book_id, "nrsvce", book_number, testament, short_name, full_name, dr_name),
        )

        for chapter_str, verses in sorted(chapters.items(), key=lambda x: int(x[0])):
            for verse_str, text in sorted(verses.items(), key=lambda x: int(x[0])):
                verse_rows.append((book_id, int(chapter_str), int(verse_str), _clean_nrsvce_verse(text)))

        print(f"  {book_number:>2}. {full_name} ({len(verse_rows)} verses so far)")

    conn.executemany(
        "INSERT INTO verses (book_id, chapter, verse, text) VALUES (?, ?, ?, ?)",
        verse_rows,
    )
    conn.execute("INSERT INTO verses_fts(verses_fts) VALUES('rebuild')")
    print(f"  NRSVCE: {len(verse_rows)} total verses indexed.")


def compile_catechism(conn: sqlite3.Connection) -> None:
    path = os.path.join(CONTENT_DIR, "catechism", "ccc_paragraphs.json")
    with open(path) as f:
        paragraphs = json.load(f)

    if not paragraphs:
        print("  SKIP: ccc_paragraphs.json is empty.")
        return

    for p in paragraphs:
        conn.execute(
            "INSERT INTO ccc_sections VALUES (?, ?, ?, ?, ?, ?, ?)",
            (p["id"], p.get("part"), p.get("section"), p.get("chapter"),
             p.get("article"), p.get("heading"), p["body"]),
        )
    conn.execute("INSERT INTO ccc_fts(ccc_fts) VALUES('rebuild')")
    print(f"  Catechism: {len(paragraphs)} paragraphs indexed.")


def compile_prayers(conn: sqlite3.Connection) -> None:
    path = os.path.join(CONTENT_DIR, "prayers", "prayers.json")
    with open(path) as f:
        prayers = json.load(f)

    if not prayers:
        print("  SKIP: prayers.json is empty.")
        return

    for p in prayers:
        conn.execute(
            "INSERT INTO prayers VALUES (?, ?, ?, ?, ?)",
            (p["id"], p["title"], p["body"], p.get("source"), p["category"]),
        )
        for tag in p.get("tags", []):
            conn.execute("INSERT INTO prayer_tags VALUES (?, ?)", (p["id"], tag))

    conn.execute("INSERT INTO prayers_fts(prayers_fts) VALUES('rebuild')")
    print(f"  Prayers: {len(prayers)} entries indexed.")


def compile_novenas(conn: sqlite3.Connection) -> None:
    path = os.path.join(CONTENT_DIR, "novenas", "novenas.json")
    with open(path) as f:
        novenas = json.load(f)

    if not novenas:
        print("  SKIP: novenas.json is empty.")
        return

    for n in novenas:
        conn.execute(
            "INSERT INTO novenas VALUES (?, ?, ?, ?, ?)",
            (n["id"], n["title"], n.get("description"),
             n.get("total_days", 9), n.get("feast_day")),
        )
        for day in n["days"]:
            conn.execute(
                "INSERT INTO novena_days (novena_id, day_number, title, body) VALUES (?, ?, ?, ?)",
                (n["id"], day["day"], day.get("title"), day["body"]),
            )
    print(f"  Novenas: {len(novenas)} entries loaded.")


def compile_rosary(conn: sqlite3.Connection) -> None:
    path = os.path.join(CONTENT_DIR, "rosary", "mysteries.json")
    if not os.path.exists(path):
        print("  SKIP: content/rosary/mysteries.json not found.")
        return

    with open(path) as f:
        mystery_sets = json.load(f)

    # Rosary bead sequence per mystery set:
    # Intro (6 beads): Apostles' Creed, Our Father, 3× Hail Mary (with intentions), Glory Be
    # Per mystery (14 beads): Mystery announcement (no prayer), Our Father, 10× Hail Mary, Glory Be, Fatima Prayer
    # Closing (1 bead): Hail Holy Queen
    # (prayer_id, mystery_title / intention)
    INTRO = [
        ("apostles-creed", None),
        ("our-father",     None),
        ("hail-mary",      "For an increase in Faith"),
        ("hail-mary",      "For an increase in Hope"),
        ("hail-mary",      "For an increase in Charity"),
        ("glory-be",       None),
    ]

    total_beads = 0
    for ms in mystery_sets:
        conn.execute(
            "INSERT INTO mysteries VALUES (?, ?, ?)",
            (ms["id"], ms["name"], ms.get("traditional_days")),
        )

        position = 1
        for prayer_id, intention in INTRO:
            conn.execute(
                "INSERT INTO mystery_beads (mystery_id, position, prayer_id, mystery_title) VALUES (?, ?, ?, ?)",
                (ms["id"], position, prayer_id, intention),
            )
            position += 1

        for m in ms["mysteries"]:
            # Mystery announcement bead — dedicated page, no prayer
            conn.execute(
                """INSERT INTO mystery_beads
                   (mystery_id, position, prayer_id, mystery_number, mystery_title,
                    mystery_scripture, mystery_meditation)
                   VALUES (?, ?, ?, ?, ?, ?, ?)""",
                (ms["id"], position, None, m["number"],
                 m["title"], m.get("scripture"), m.get("meditation")),
            )
            position += 1

            # Our Father bead — prayer only
            conn.execute(
                "INSERT INTO mystery_beads (mystery_id, position, prayer_id, mystery_number) VALUES (?, ?, ?, ?)",
                (ms["id"], position, "our-father", m["number"]),
            )
            position += 1

            for _ in range(10):
                conn.execute(
                    "INSERT INTO mystery_beads (mystery_id, position, prayer_id, mystery_number) VALUES (?, ?, ?, ?)",
                    (ms["id"], position, "hail-mary", m["number"]),
                )
                position += 1

            conn.execute(
                "INSERT INTO mystery_beads (mystery_id, position, prayer_id, mystery_number) VALUES (?, ?, ?, ?)",
                (ms["id"], position, "glory-be", m["number"]),
            )
            position += 1

            conn.execute(
                "INSERT INTO mystery_beads (mystery_id, position, prayer_id, mystery_number) VALUES (?, ?, ?, ?)",
                (ms["id"], position, "fatima-prayer", m["number"]),
            )
            position += 1

        # Closing
        conn.execute(
            "INSERT INTO mystery_beads (mystery_id, position, prayer_id) VALUES (?, ?, ?)",
            (ms["id"], position, "hail-holy-queen"),
        )
        position += 1

        total_beads += position - 1
        print(f"  {ms['name']}: {position - 1} beads")

    print(f"  Rosary: {len(mystery_sets)} mystery sets, {total_beads} total beads.")


def main() -> None:
    os.makedirs(os.path.dirname(OUT_DB), exist_ok=True)

    if os.path.exists(OUT_DB):
        os.remove(OUT_DB)

    print(f"Building {OUT_DB} ...")
    conn = sqlite3.connect(OUT_DB)

    try:
        create_schema(conn)
        print("Schema created.")

        print("Compiling Bible (DRA)...")
        compile_dr_format(conn, "dra")

        print("Compiling Bible (Latin Vulgate)...")
        compile_dr_format(conn, "vulgate")

        print("Compiling Bible (Latin Vulgate — English Translation)...")
        compile_dr_format(conn, "vulgate-et")

        print("Compiling Catechism...")
        compile_catechism(conn)

        print("Compiling Prayers...")
        compile_prayers(conn)

        print("Compiling Novenas...")
        compile_novenas(conn)

        print("Compiling Rosary...")
        compile_rosary(conn)

        conn.commit()
        size_kb = os.path.getsize(OUT_DB) // 1024
        print(f"\nDone. Output: {OUT_DB} ({size_kb} KB)")
    except Exception as e:
        conn.rollback()
        print(f"ERROR: {e}", file=sys.stderr)
        raise
    finally:
        conn.close()


if __name__ == "__main__":
    main()
