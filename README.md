# De Fide

A free, open-source Catholic app for Android. No tracking, no accounts, no internet required.

## Features

- **Bible** — Douay-Rheims (1899), World English Bible (Catholic), Latin Vulgate, Latin Vulgate (English Translation), Bíblia Ave-Maria (pt-BR), and Bíblia dos Capuchinhos (pt-PT)
- **Catechism** — Links to the Catechism of the Catholic Church on the USCCB website (opens in browser)
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

## Building

Requires Android Studio or the Android SDK with Gradle.

```bash
./gradlew assembleDebug
```

To recompile the content database from source JSON files:

```bash
python scripts/compile_content.py
```

Then bump `CONTENT_VERSION` in `ContentDatabase.kt` and rebuild.

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
