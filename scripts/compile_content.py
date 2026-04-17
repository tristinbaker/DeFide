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
    content/bible/web-c/books/<BookName>.json (one per book, DR naming)
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
            id       TEXT NOT NULL,
            language TEXT NOT NULL DEFAULT 'en',
            title    TEXT NOT NULL,
            body     TEXT NOT NULL,
            source   TEXT,
            category TEXT NOT NULL,
            PRIMARY KEY (id, language)
        );

        CREATE TABLE IF NOT EXISTS prayer_tags (
            prayer_id TEXT NOT NULL,
            language  TEXT NOT NULL DEFAULT 'en',
            tag       TEXT NOT NULL,
            PRIMARY KEY (prayer_id, language, tag)
        );

        CREATE VIRTUAL TABLE IF NOT EXISTS prayers_fts USING fts4(
            content="prayers",
            title, body
        );

        -- Novenas
        CREATE TABLE IF NOT EXISTS novenas (
            id          TEXT NOT NULL,
            language    TEXT NOT NULL DEFAULT 'en',
            title       TEXT NOT NULL,
            description TEXT,
            total_days  INTEGER NOT NULL DEFAULT 9,
            feast_day   TEXT,
            PRIMARY KEY (id, language)
        );

        CREATE TABLE IF NOT EXISTS novena_days (
            id         INTEGER PRIMARY KEY,
            novena_id  TEXT NOT NULL,
            language   TEXT NOT NULL DEFAULT 'en',
            day_number INTEGER NOT NULL,
            title      TEXT,
            body       TEXT NOT NULL
        );

        -- Rosary
        CREATE TABLE IF NOT EXISTS mysteries (
            id               TEXT NOT NULL,
            language         TEXT NOT NULL DEFAULT 'en',
            name             TEXT NOT NULL,
            traditional_days TEXT,
            PRIMARY KEY (id, language)
        );

        CREATE TABLE IF NOT EXISTS mystery_beads (
            id                 INTEGER PRIMARY KEY,
            mystery_id         TEXT NOT NULL,
            language           TEXT NOT NULL DEFAULT 'en',
            variant            TEXT NOT NULL DEFAULT 'dominican',
            position           INTEGER NOT NULL,
            prayer_id          TEXT,
            mystery_number     INTEGER,
            mystery_title      TEXT,
            mystery_scripture  TEXT,
            mystery_meditation TEXT
        );

        -- Indexes for fast chapter / book lookups
        CREATE INDEX IF NOT EXISTS idx_verses_book_chapter ON verses(book_id, chapter);
        CREATE INDEX IF NOT EXISTS idx_books_translation ON books(translation_id, book_number);
        CREATE INDEX IF NOT EXISTS idx_mystery_beads_mystery ON mystery_beads(mystery_id, language, variant, position);
        CREATE INDEX IF NOT EXISTS idx_novena_days_novena ON novena_days(novena_id, day_number);
    """)


# Starting book_id offsets per translation so IDs never collide across translations.
# DRA: 1–999, NRSVCE: 1001–1999, Vulgate: 2001–2999, Vulgate-ET: 3001–3999, WEB-C: 4001–4999
# Ave-Maria (PT): 5001–5999
_TRANSLATION_BOOK_ID_OFFSET = {
    "dra":        1,
    "vulgate":    2001,
    "vulgate-et": 3001,
    "web-c":      4001,
    "ave-maria":  5001,
    "porcap":     6001,
    "crampon":    7001,
    "rk1998":     8001,
}

# Portuguese display names for Ave-Maria, keyed by DR filename (no extension).
_PT_BOOK_NAMES = {
    "Genesis":          "Gênesis",
    "Exodus":           "Êxodo",
    "Leviticus":        "Levítico",
    "Numbers":          "Números",
    "Deuteronomy":      "Deuteronômio",
    "Josue":            "Josué",
    "Judges":           "Juízes",
    "Ruth":             "Rute",
    "1 Kings":          "I Samuel",
    "2 Kings":          "II Samuel",
    "3 Kings":          "I Reis",
    "4 Kings":          "II Reis",
    "1 Paralipomenon":  "I Crônicas",
    "2 Paralipomenon":  "II Crônicas",
    "1 Esdras":         "Esdras",
    "2 Esdras":         "Neemias",
    "Tobias":           "Tobias",
    "Judith":           "Judite",
    "Esther":           "Ester",
    "Job":              "Jó",
    "Psalms":           "Salmos",
    "1 Machabees":      "I Macabeus",
    "2 Machabees":      "II Macabeus",
    "Proverbs":         "Provérbios",
    "Ecclesiastes":     "Eclesiastes",
    "Canticles":        "Cântico dos Cânticos",
    "Wisdom":           "Sabedoria",
    "Ecclesiasticus":   "Eclesiástico",
    "Isaias":           "Isaías",
    "Jeremias":         "Jeremias",
    "Lamentations":     "Lamentações",
    "Baruch":           "Baruc",
    "Ezechiel":         "Ezequiel",
    "Daniel":           "Daniel",
    "Osee":             "Oséias",
    "Joel":             "Joel",
    "Amos":             "Amós",
    "Abdias":           "Abdias",
    "Jonas":            "Jonas",
    "Micheas":          "Miquéias",
    "Nahum":            "Naum",
    "Habacuc":          "Habacuc",
    "Sophonias":        "Sofonias",
    "Aggeus":           "Ageu",
    "Zacharias":        "Zacarias",
    "Malachias":        "Malaquias",
    "Matthew":          "São Mateus",
    "Mark":             "São Marcos",
    "Luke":             "São Lucas",
    "John":             "São João",
    "Acts":             "Atos dos Apóstolos",
    "Romans":           "Romanos",
    "1 Corinthians":    "I Coríntios",
    "2 Corinthians":    "II Coríntios",
    "Galatians":        "Gálatas",
    "Ephesians":        "Efésios",
    "Philippians":      "Filipenses",
    "Colossians":       "Colossenses",
    "1 Thessalonians":  "I Tessalonicenses",
    "2 Thessalonians":  "II Tessalonicenses",
    "1 Timothy":        "I Timóteo",
    "2 Timothy":        "II Timóteo",
    "Titus":            "Tito",
    "Philemon":         "Filêmon",
    "Hebrews":          "Hebreus",
    "James":            "São Tiago",
    "1 Peter":          "I São Pedro",
    "2 Peter":          "II São Pedro",
    "1 John":           "I São João",
    "2 John":           "II São João",
    "3 John":           "III São João",
    "Jude":             "São Judas",
    "Apocalypse":       "Apocalipse",
}


# European Portuguese display names for PorCap (Bíblia dos Capuchinhos).
# Key differences from pt-BR: Génesis, Deuteronómio, Crónicas, Filémon, etc.
_PORCAP_BOOK_NAMES = {
    "Genesis":          "Génesis",
    "Exodus":           "Êxodo",
    "Leviticus":        "Levítico",
    "Numbers":          "Números",
    "Deuteronomy":      "Deuteronómio",
    "Josue":            "Josué",
    "Judges":           "Juízes",
    "Ruth":             "Rute",
    "1 Kings":          "1 Samuel",
    "2 Kings":          "2 Samuel",
    "3 Kings":          "1 Reis",
    "4 Kings":          "2 Reis",
    "1 Paralipomenon":  "1 Crónicas",
    "2 Paralipomenon":  "2 Crónicas",
    "1 Esdras":         "Esdras",
    "2 Esdras":         "Neemias",
    "Tobias":           "Tobite",
    "Judith":           "Judite",
    "Esther":           "Ester",
    "Job":              "Job",
    "Psalms":           "Salmos",
    "1 Machabees":      "1 Macabeus",
    "2 Machabees":      "2 Macabeus",
    "Proverbs":         "Provérbios",
    "Ecclesiastes":     "Eclesiastes",
    "Canticles":        "Cântico dos Cânticos",
    "Wisdom":           "Sabedoria",
    "Ecclesiasticus":   "Eclesiástico",
    "Isaias":           "Isaías",
    "Jeremias":         "Jeremias",
    "Lamentations":     "Lamentações",
    "Baruch":           "Baruc",
    "Ezechiel":         "Ezequiel",
    "Daniel":           "Daniel",
    "Osee":             "Oseias",
    "Joel":             "Joel",
    "Amos":             "Amós",
    "Abdias":           "Abdias",
    "Jonas":            "Jonas",
    "Micheas":          "Miqueias",
    "Nahum":            "Naum",
    "Habacuc":          "Habacuc",
    "Sophonias":        "Sofonias",
    "Aggeus":           "Ageu",
    "Zacharias":        "Zacarias",
    "Malachias":        "Malaquias",
    "Matthew":          "Mateus",
    "Mark":             "Marcos",
    "Luke":             "Lucas",
    "John":             "João",
    "Acts":             "Atos dos Apóstolos",
    "Romans":           "Romanos",
    "1 Corinthians":    "1 Coríntios",
    "2 Corinthians":    "2 Coríntios",
    "Galatians":        "Gálatas",
    "Ephesians":        "Efésios",
    "Philippians":      "Filipenses",
    "Colossians":       "Colossenses",
    "1 Thessalonians":  "1 Tessalonicenses",
    "2 Thessalonians":  "2 Tessalonicenses",
    "1 Timothy":        "1 Timóteo",
    "2 Timothy":        "2 Timóteo",
    "Titus":            "Tito",
    "Philemon":         "Filémon",
    "Hebrews":          "Hebreus",
    "James":            "Tiago",
    "1 Peter":          "1 Pedro",
    "2 Peter":          "2 Pedro",
    "1 John":           "1 João",
    "2 John":           "2 João",
    "3 John":           "3 João",
    "Jude":             "Judas",
    "Apocalypse":       "Apocalipse",
}


# French display names for Crampon, keyed by DR filename (no extension).
_FR_BOOK_NAMES = {
    "Genesis":          "Genèse",
    "Exodus":           "Exode",
    "Leviticus":        "Lévitique",
    "Numbers":          "Nombres",
    "Deuteronomy":      "Deutéronome",
    "Josue":            "Josué",
    "Judges":           "Juges",
    "Ruth":             "Ruth",
    "1 Kings":          "1 Samuel",
    "2 Kings":          "2 Samuel",
    "3 Kings":          "1 Rois",
    "4 Kings":          "2 Rois",
    "1 Paralipomenon":  "1 Chroniques",
    "2 Paralipomenon":  "2 Chroniques",
    "1 Esdras":         "Esdras",
    "2 Esdras":         "Néhémie",
    "Tobias":           "Tobie",
    "Judith":           "Judith",
    "Esther":           "Esther",
    "1 Machabees":      "1 Maccabées",
    "2 Machabees":      "2 Maccabées",
    "Job":              "Job",
    "Psalms":           "Psaumes",
    "Proverbs":         "Proverbes",
    "Ecclesiastes":     "Ecclésiaste",
    "Canticles":        "Cantique des Cantiques",
    "Wisdom":           "Sagesse",
    "Ecclesiasticus":   "Siracide",
    "Isaias":           "Isaïe",
    "Jeremias":         "Jérémie",
    "Lamentations":     "Lamentations",
    "Baruch":           "Baruch",
    "Ezechiel":         "Ézéchiel",
    "Daniel":           "Daniel",
    "Osee":             "Osée",
    "Joel":             "Joël",
    "Amos":             "Amos",
    "Abdias":           "Abdias",
    "Jonas":            "Jonas",
    "Micheas":          "Michée",
    "Nahum":            "Nahoum",
    "Habacuc":          "Habacuc",
    "Sophonias":        "Sophonie",
    "Aggeus":           "Aggée",
    "Zacharias":        "Zacharie",
    "Malachias":        "Malachie",
    "Matthew":          "Matthieu",
    "Mark":             "Marc",
    "Luke":             "Luc",
    "John":             "Jean",
    "Acts":             "Actes des Apôtres",
    "Romans":           "Romains",
    "1 Corinthians":    "1 Corinthiens",
    "2 Corinthians":    "2 Corinthiens",
    "Galatians":        "Galates",
    "Ephesians":        "Éphésiens",
    "Philippians":      "Philippiens",
    "Colossians":       "Colossiens",
    "1 Thessalonians":  "1 Thessaloniciens",
    "2 Thessalonians":  "2 Thessaloniciens",
    "1 Timothy":        "1 Timothée",
    "2 Timothy":        "2 Timothée",
    "Titus":            "Tite",
    "Philemon":         "Philémon",
    "Hebrews":          "Hébreux",
    "James":            "Jacques",
    "1 Peter":          "1 Pierre",
    "2 Peter":          "2 Pierre",
    "1 John":           "1 Jean",
    "2 John":           "2 Jean",
    "3 John":           "3 Jean",
    "Jude":             "Jude",
    "Apocalypse":       "Apocalypse",
}


# Classical Latin names for the Clementine Vulgate, keyed by DR filename.
_LA_BOOK_NAMES = {
    "Genesis":          "Genesis",
    "Exodus":           "Exodus",
    "Leviticus":        "Leviticus",
    "Numbers":          "Numeri",
    "Deuteronomy":      "Deuteronomium",
    "Josue":            "Iosue",
    "Judges":           "Iudicum",
    "Ruth":             "Ruth",
    "1 Kings":          "I Regum",
    "2 Kings":          "II Regum",
    "3 Kings":          "III Regum",
    "4 Kings":          "IV Regum",
    "1 Paralipomenon":  "I Paralipomenon",
    "2 Paralipomenon":  "II Paralipomenon",
    "1 Esdras":         "I Esdrae",
    "2 Esdras":         "II Esdrae",
    "Tobias":           "Tobias",
    "Judith":           "Iudith",
    "Esther":           "Esther",
    "1 Machabees":      "I Machabaeorum",
    "2 Machabees":      "II Machabaeorum",
    "Job":              "Iob",
    "Psalms":           "Psalmi",
    "Proverbs":         "Proverbia",
    "Ecclesiastes":     "Ecclesiastes",
    "Canticles":        "Canticum Canticorum",
    "Wisdom":           "Sapientia",
    "Ecclesiasticus":   "Ecclesiasticus",
    "Isaias":           "Isaias",
    "Jeremias":         "Ieremias",
    "Lamentations":     "Threni",
    "Baruch":           "Baruch",
    "Ezechiel":         "Ezechiel",
    "Daniel":           "Daniel",
    "Osee":             "Osee",
    "Joel":             "Ioel",
    "Amos":             "Amos",
    "Abdias":           "Abdias",
    "Jonas":            "Ionas",
    "Micheas":          "Micheas",
    "Nahum":            "Nahum",
    "Habacuc":          "Habacuc",
    "Sophonias":        "Sophonias",
    "Aggeus":           "Aggaeus",
    "Zacharias":        "Zacharias",
    "Malachias":        "Malachias",
    "Matthew":          "Matthaeus",
    "Mark":             "Marcus",
    "Luke":             "Lucas",
    "John":             "Ioannes",
    "Acts":             "Actus Apostolorum",
    "Romans":           "Ad Romanos",
    "1 Corinthians":    "Ad Corinthios I",
    "2 Corinthians":    "Ad Corinthios II",
    "Galatians":        "Ad Galatas",
    "Ephesians":        "Ad Ephesios",
    "Philippians":      "Ad Philippenses",
    "Colossians":       "Ad Colossenses",
    "1 Thessalonians":  "Ad Thessalonicenses I",
    "2 Thessalonians":  "Ad Thessalonicenses II",
    "1 Timothy":        "Ad Timotheum I",
    "2 Timothy":        "Ad Timotheum II",
    "Titus":            "Ad Titum",
    "Philemon":         "Ad Philemonem",
    "Hebrews":          "Ad Hebraeos",
    "James":            "Epistola Iacobi",
    "1 Peter":          "Epistola Petri I",
    "2 Peter":          "Epistola Petri II",
    "1 John":           "Epistola Ioannis I",
    "2 John":           "Epistola Ioannis II",
    "3 John":           "Epistola Ioannis III",
    "Jude":             "Epistola Iudae",
    "Apocalypse":       "Apocalypsis",
}


_LT_BOOK_NAMES = {
    "Genesis":          "Pradžios knyga",
    "Exodus":           "Išėjimo knyga",
    "Leviticus":        "Kunigų knyga",
    "Numbers":          "Skaičių knyga",
    "Deuteronomy":      "Pakartoto Įstatymo knyga",
    "Josue":            "Jozuės knyga",
    "Judges":           "Teisėjų knyga",
    "Ruth":             "Rutos knyga",
    "1 Kings":          "1 Samuelio knyga",
    "2 Kings":          "2 Samuelio knyga",
    "3 Kings":          "1 Karalių knyga",
    "4 Kings":          "2 Karalių knyga",
    "1 Paralipomenon":  "1 Kronikų knyga",
    "2 Paralipomenon":  "2 Kronikų knyga",
    "1 Esdras":         "Ezdro knyga",
    "2 Esdras":         "Nehemijo knyga",
    "Tobias":           "Tobito knyga",
    "Judith":           "Juditos knyga",
    "Esther":           "Esteros knyga",
    "1 Machabees":      "1 Makabiejų knyga",
    "2 Machabees":      "2 Makabiejų knyga",
    "Job":              "Jobo knyga",
    "Psalms":           "Psalmių knyga",
    "Proverbs":         "Patarlių knyga",
    "Ecclesiastes":     "Koheleto knyga",
    "Canticles":        "Giesmių Giesmė",
    "Wisdom":           "Išminties knyga",
    "Ecclesiasticus":   "Siracido knyga",
    "Isaias":           "Izaijo knyga",
    "Jeremias":         "Jeremijo knyga",
    "Lamentations":     "Raudų knyga",
    "Baruch":           "Barucho knyga",
    "Ezechiel":         "Ezekielio knyga",
    "Daniel":           "Danieliaus knyga",
    "Osee":             "Ozėjo knyga",
    "Joel":             "Joelio knyga",
    "Amos":             "Amoso knyga",
    "Abdias":           "Abdijo knyga",
    "Jonas":            "Jonos knyga",
    "Micheas":          "Michėjo knyga",
    "Nahum":            "Nahumo knyga",
    "Habacuc":          "Habakuko knyga",
    "Sophonias":        "Sofonijo knyga",
    "Aggeus":           "Agėjo knyga",
    "Zacharias":        "Zacharijo knyga",
    "Malachias":        "Malachijo knyga",
    "Matthew":          "Evangelija pagal Matą",
    "Mark":             "Evangelija pagal Morkų",
    "Luke":             "Evangelija pagal Luką",
    "John":             "Evangelija pagal Joną",
    "Acts":             "Apaštalų darbai",
    "Romans":           "Laiškas romiečiams",
    "1 Corinthians":    "Pirmasis laiškas korintiečiams",
    "2 Corinthians":    "Antrasis laiškas korintiečiams",
    "Galatians":        "Laiškas galatams",
    "Ephesians":        "Laiškas efeziečiams",
    "Philippians":      "Laiškas filipiečiams",
    "Colossians":       "Laiškas kolosiečiams",
    "1 Thessalonians":  "Pirmasis laiškas tesalonikiečiams",
    "2 Thessalonians":  "Antrasis laiškas tesalonikiečiams",
    "1 Timothy":        "Pirmasis laiškas Timotiejui",
    "2 Timothy":        "Antrasis laiškas Timotiejui",
    "Titus":            "Laiškas Titui",
    "Philemon":         "Laiškas Filemonui",
    "Hebrews":          "Laiškas žydams",
    "James":            "Jokūbo laiškas",
    "1 Peter":          "Pirmasis Petro laiškas",
    "2 Peter":          "Antrasis Petro laiškas",
    "1 John":           "Pirmasis Jono laiškas",
    "2 John":           "Antrasis Jono laiškas",
    "3 John":           "Trečiasis Jono laiškas",
    "Jude":             "Judo laiškas",
    "Apocalypse":       "Apreiškimas Jonui",
}


def compile_dr_format(
    conn: sqlite3.Connection,
    translation_id: str,
    lang: str = "en",
    book_full_names: dict | None = None,
) -> None:
    """
    Ingests any translation stored in the DRA per-book folder format:
        content/bible/<lang>/<translation_id>/metadata.json
        content/bible/<lang>/<translation_id>/books/<DR-name>.json
    Book files must be named using BOOK_MANIFEST keys.

    book_full_names: optional dict mapping DR filename (no extension) → display name override.
                     Use this for non-English translations so the full_name column is localized.
    """
    translation_dir = os.path.join(CONTENT_DIR, "bible", lang, translation_id)
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
        if book_full_names and dr_name in book_full_names:
            full_name = book_full_names[dr_name]
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


def compile_prayers(conn: sqlite3.Connection, lang: str) -> None:
    path = os.path.join(CONTENT_DIR, "prayers", lang, "prayers.json")
    if not os.path.exists(path):
        print(f"  SKIP: {path} not found.")
        return
    with open(path) as f:
        prayers = json.load(f)

    if not prayers:
        print(f"  SKIP: prayers.json ({lang}) is empty.")
        return

    for p in prayers:
        conn.execute(
            "INSERT INTO prayers (id, language, title, body, source, category) VALUES (?, ?, ?, ?, ?, ?)",
            (p["id"], lang, p["title"], p["body"], p.get("source"), p["category"]),
        )
        for tag in p.get("tags", []):
            conn.execute(
                "INSERT INTO prayer_tags (prayer_id, language, tag) VALUES (?, ?, ?)",
                (p["id"], lang, tag),
            )

    conn.execute("INSERT INTO prayers_fts(prayers_fts) VALUES('rebuild')")
    print(f"  Prayers ({lang}): {len(prayers)} entries indexed.")


def compile_novenas(conn: sqlite3.Connection, lang: str) -> None:
    path = os.path.join(CONTENT_DIR, "novenas", lang, "novenas.json")
    if not os.path.exists(path):
        print(f"  SKIP: {path} not found.")
        return
    with open(path) as f:
        novenas = json.load(f)

    if not novenas:
        print(f"  SKIP: novenas.json ({lang}) is empty.")
        return

    for n in novenas:
        conn.execute(
            "INSERT INTO novenas (id, language, title, description, total_days, feast_day) VALUES (?, ?, ?, ?, ?, ?)",
            (n["id"], lang, n["title"], n.get("description"),
             n.get("total_days", 9), n.get("feast_day")),
        )
        for day in n["days"]:
            conn.execute(
                "INSERT INTO novena_days (novena_id, language, day_number, title, body) VALUES (?, ?, ?, ?, ?)",
                (n["id"], lang, day["day"], day.get("title"), day["body"]),
            )
    print(f"  Novenas ({lang}): {len(novenas)} entries loaded.")


def compile_rosary(conn: sqlite3.Connection, lang: str, variant: str = "dominican") -> None:
    path = os.path.join(CONTENT_DIR, "rosary", lang, "mysteries.json")
    if not os.path.exists(path):
        print(f"  SKIP: content/rosary/{lang}/mysteries.json not found.")
        return

    with open(path) as f:
        mystery_sets = json.load(f)

    # ── Sequence definitions ──────────────────────────────────────────────────
    INTRO_EN = [
        ("apostles-creed", None),
        ("our-father",     None),
        ("hail-mary",      "For an increase in Faith"),
        ("hail-mary",      "For an increase in Hope"),
        ("hail-mary",      "For an increase in Charity"),
        ("glory-be",       None),
    ]
    INTRO_PT = [
        ("apostles-creed", None),
        ("our-father",     None),
        ("hail-mary",      "Por um aumento em Fé"),
        ("hail-mary",      "Por um aumento em Esperança"),
        ("hail-mary",      "Por um aumento em Caridade"),
        ("glory-be",       None),
    ]
    # Fátima opening: Deus in adjutorium + Glory Be (no Our Father or Hail Marys)
    INTRO_FATIMA_PT = [
        ("deus-in-adjutorium", None),
        ("glory-be",           None),
    ]
    # Fátima closing: 3 Hail Marys (faith/hope/charity) then Hail Holy Queen
    CLOSING_FATIMA_PT = [
        ("hail-mary",       "Por um aumento em Fé"),
        ("hail-mary",       "Por um aumento em Esperança"),
        ("hail-mary",       "Por um aumento em Caridade"),
        ("hail-holy-queen", None),
    ]

    if variant == "fatima":
        INTRO = INTRO_FATIMA_PT
        CLOSING = CLOSING_FATIMA_PT
        EXTRA_DECADE = ["oh-maria-concebida", "fatima-prayer"]
    else:
        INTRO = INTRO_PT if lang in ("pt-BR", "pt-PT") else INTRO_EN
        CLOSING = [("hail-holy-queen", None)]
        EXTRA_DECADE = ["fatima-prayer"]

    # ── Insert beads ──────────────────────────────────────────────────────────
    def insert_bead(ms_id, pos, prayer_id, mystery_title=None, mystery_number=None,
                    mystery_scripture=None, mystery_meditation=None):
        conn.execute(
            """INSERT INTO mystery_beads
               (mystery_id, language, variant, position, prayer_id, mystery_title,
                mystery_number, mystery_scripture, mystery_meditation)
               VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""",
            (ms_id, lang, variant, pos, prayer_id, mystery_title,
             mystery_number, mystery_scripture, mystery_meditation),
        )

    total_beads = 0
    for ms in mystery_sets:
        # mysteries rows are shared across variants; only insert once per (id, language)
        conn.execute(
            "INSERT OR IGNORE INTO mysteries (id, language, name, traditional_days) VALUES (?, ?, ?, ?)",
            (ms["id"], lang, ms["name"], ms.get("traditional_days")),
        )

        position = 1

        for prayer_id, intention in INTRO:
            insert_bead(ms["id"], position, prayer_id, mystery_title=intention)
            position += 1

        for m in ms["mysteries"]:
            # Mystery announcement — dedicated page, no prayer_id
            insert_bead(ms["id"], position, None,
                        mystery_number=m["number"], mystery_title=m["title"],
                        mystery_scripture=m.get("scripture"), mystery_meditation=m.get("meditation"))
            position += 1

            insert_bead(ms["id"], position, "our-father", mystery_number=m["number"])
            position += 1

            for _ in range(10):
                insert_bead(ms["id"], position, "hail-mary", mystery_number=m["number"])
                position += 1

            insert_bead(ms["id"], position, "glory-be", mystery_number=m["number"])
            position += 1

            for prayer_id in EXTRA_DECADE:
                insert_bead(ms["id"], position, prayer_id, mystery_number=m["number"])
                position += 1

        for prayer_id, intention in CLOSING:
            insert_bead(ms["id"], position, prayer_id, mystery_title=intention)
            position += 1

        total_beads += position - 1
        print(f"  {ms['name']} ({variant}): {position - 1} beads")

    print(f"  Rosary ({lang}, {variant}): {len(mystery_sets)} mystery sets, {total_beads} total beads.")


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
        compile_dr_format(conn, "dra", lang="en")

        print("Compiling Bible (Latin Vulgate)...")
        compile_dr_format(conn, "vulgate", lang="la", book_full_names=_LA_BOOK_NAMES)

        print("Compiling Bible (Latin Vulgate — English Translation)...")
        compile_dr_format(conn, "vulgate-et", lang="en")

        print("Compiling Bible (World English Bible — Catholic Edition)...")
        compile_dr_format(conn, "web-c", lang="en")

        print("Compiling Bible (Bíblia Ave-Maria — Portuguese)...")
        compile_dr_format(conn, "ave-maria", lang="pt-BR", book_full_names=_PT_BOOK_NAMES)

        print("Compiling Bible (Bíblia dos Capuchinhos — European Portuguese)...")
        compile_dr_format(conn, "porcap", lang="pt-PT", book_full_names=_PORCAP_BOOK_NAMES)

        print("Compiling Bible (Crampon 1923 — French)...")
        compile_dr_format(conn, "crampon", lang="fr", book_full_names=_FR_BOOK_NAMES)

        print("Compiling Bible (Biblija RK K1998 — Lithuanian)...")
        compile_dr_format(conn, "rk1998", lang="lt", book_full_names=_LT_BOOK_NAMES)

        print("Compiling Catechism...")
        compile_catechism(conn)

        print("Compiling Prayers...")
        compile_prayers(conn, "en")
        compile_prayers(conn, "pt-BR")
        compile_prayers(conn, "pt-PT")
        compile_prayers(conn, "fr")
        compile_prayers(conn, "lt")

        print("Compiling Novenas...")
        compile_novenas(conn, "en")
        compile_novenas(conn, "pt-BR")
        compile_novenas(conn, "pt-PT")
        compile_novenas(conn, "fr")

        print("Compiling Rosary...")
        compile_rosary(conn, "en")
        compile_rosary(conn, "pt-BR")
        compile_rosary(conn, "pt-PT")
        compile_rosary(conn, "pt-PT", variant="fatima")
        compile_rosary(conn, "fr")
        compile_rosary(conn, "lt")

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
