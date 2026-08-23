# De Fide

A free, open-source Catholic app for Android. No tracking, no accounts, no internet required.

## Features

- **Bible** — Douay-Rheims (1899), World English Bible (Catholic), Latin Vulgate, Latin Vulgate (English Translation), Bíblia Ave-Maria (pt-BR), and Bíblia dos Capuchinhos (pt-PT), plus support for [importing your own translations from JSON](#importing-your-own-bible)
- **Catechism** — Catechism of the Catholic Church (online via browser) and Baltimore Catechism (offline)
- **Divine Office / Breviary** — Latin Divine Office from the [DivinumOfficium project](https://github.com/DivinumOfficium/divinum-officium), with antiphons, hymns, psalms, readings, and prayers for Lauds, Vespers, and Matins
- **Rosary** — Guided sessions for all four mysteries with bead indicator and scripture references
- **Prayers** — Traditional Catholic prayers with tag-based browsing
- **Novenas** — Nine-day prayer tracker with progress persistence
- **Home screen** — Verse of the Day and today's suggested Rosary mystery
- **Localization** — English, Português (Brasil), and Português (Portugal)

## Philosophy

De Fide is built on three principles:

1. **Fully offline** — all content ships with the app, no network calls ever
2. **No data collection** — no analytics, no crash reporting, no accounts
3. **FOSS only** — all Bible translations are public domain or license-free; the app is AGPL-3.0

## Importing Your Own Bible

De Fide only bundles translations that are public domain or freely licensed. If you have a copy of another translation as JSON (for example an NRSVUE dump), you can import it on-device. Imported translations get full-text search, Verse of the Day, bookmarks, highlights, and reading progress, just like the built-in ones.

**How:** Settings > Bible Translation > "Import translation (JSON)", pick the file, and give the translation a name. Re-importing under the same name replaces the previous copy. The delete icon next to an imported translation removes it again.

**Expected format:** a single JSON object organized testament > book > chapter > verse:

```json
{
  "Old Testament": {
    "Genesis": {
      "1": {
        "1": "In the beginning God created the heavens and the earth.",
        "2": "The earth was without form and void..."
      },
      "2": { "1": "Thus the heavens and the earth were finished." }
    }
  },
  "New Testament": {
    "Matthew": { "1": { "1": "The book of the genealogy of Jesus Christ..." } }
  },
  "Deuterocanonical": {
    "Tobit": { "1": { "1": "The book of the words of Tobit..." } }
  }
}
```

Format details:

- Top-level keys are testaments: `Old Testament`, `New Testament`, and `Deuterocanonical`. The section label used by NRSVUE-style dumps for the deuterocanonical books is also recognized, so those files work unmodified. Deuterocanonical books appear in the app's Old Testament section, in canonical order.
- Book names are matched case-insensitively against standard English names and mapped to the app's canonical book numbering, so reading progress lines up across translations. Common variants are accepted (`Psalm`/`Psalms`, `Song of Songs`/`Song of Solomon`/`Canticles`, `Wisdom`/`Wisdom of Solomon`, `Sirach`/`Ecclesiasticus`, Douay-Rheims names like `Isaias`), as are additional books outside the 73-book canon that some dumps include (`1 Esdras`, `2 Esdras`, `3 Maccabees`, `4 Maccabees`, `Letter of Jeremiah`, `Prayer of Manasseh`, `Psalm 151`). Unrecognized books are still imported and listed after the known ones.
- Chapter and verse keys must be numeric strings (`"1"`, `"2"`, ...).
- Leading superscript verse numbers (¹ ² ³) in verse text are stripped automatically, so dumps that embed them work as-is.
- Imported translations are stored in a separate on-device database and are never uploaded anywhere. They are not included in app backups, so keep the JSON file if you might want to re-import it on a new device.
- Only import texts you have the right to use. Copyrighted translations are for your personal use only and must not be redistributed.

## Building

Requires Android Studio or the Android SDK with Gradle.

```bash
git clone https://github.com/joshbowyer/DeFide.git
cd DeFide
git submodule update --init --recursive  # clone the DivinumOfficium source
./gradlew assembleDebug
```

The build automatically runs `scripts/compile_content.py` (which pulls from the DivinumOfficium submodule) before assembling, so no manual steps are needed.

## Tech Stack

- Kotlin + Jetpack Compose
- Hilt (dependency injection)
- Room (user data — bookmarks, novena progress, rosary sessions)
- Raw SQLite (read-only content database, bypasses Room to support FTS4)
- DataStore (preferences)

## Contributing

### Translations

UI translations are managed on Weblate:

**[hosted.weblate.org/projects/de-fide/app-strings](https://hosted.weblate.org/projects/de-fide/app-strings/)**

If you'd like to improve an existing translation or add a new language, you can contribute directly through the Weblate interface without any coding required.

### Content (prayers, novenas, rosary)

Prayers, novenas, and rosary meditations live in the `content/` directory as JSON files, organized by language. Pull requests with corrections or new content are welcome.

## License

GNU Affero General Public License v3.0 only — see [LICENSE](LICENSE).
