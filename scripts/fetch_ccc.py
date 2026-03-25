#!/usr/bin/env python3
"""
fetch_ccc.py — Downloads CCC paragraphs from catholicism-in-json and writes
               content/catechism/ccc_paragraphs.json in our schema format.

Usage:
    python scripts/fetch_ccc.py

Source: https://github.com/aseemsavio/catholicism-in-json
"""

import json
import os
import urllib.request

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT_PATH = os.path.join(REPO_ROOT, "content", "catechism", "ccc_paragraphs.json")

SOURCE_URL = (
    "https://github.com/aseemsavio/catholicism-in-json"
    "/releases/download/v2.0.0/catechism.json"
)


def fetch() -> None:
    print(f"Downloading CCC from {SOURCE_URL} ...")
    with urllib.request.urlopen(SOURCE_URL) as resp:
        raw = json.loads(resp.read().decode())

    # Source format: list of {"id": <int>, "text": "<paragraph text>"}
    # Our format: {"id": <int>, "part": null, ..., "heading": null, "body": "<text>"}
    paragraphs = []
    for entry in raw:
        paragraphs.append({
            "id": entry["id"],
            "part": None,
            "section": None,
            "chapter": None,
            "article": None,
            "heading": None,
            "body": entry["text"],
        })

    paragraphs.sort(key=lambda p: p["id"])

    with open(OUT_PATH, "w", encoding="utf-8") as f:
        json.dump(paragraphs, f, ensure_ascii=False, indent=2)

    print(f"Written {len(paragraphs)} paragraphs to {OUT_PATH}")


if __name__ == "__main__":
    fetch()
