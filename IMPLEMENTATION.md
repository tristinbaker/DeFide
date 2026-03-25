# IMPLEMENTATION.md

---

## Technical Implementation Plan — De Fide

### Architecture Overview

```
┌─────────────────────────────────┐
│         Android App             │
│  Kotlin + Jetpack Compose       │
│  Room (SQLite) — local-first    │
│  Hilt DI / Coroutines / Flow    │
└────────────┬────────────────────┘
             │ (optional, future)
             │ REST/JSON API
             ▼
┌─────────────────────────────────┐
│       Rails API Backend         │  ← Long-term / accounts phase
│  PostgreSQL · Rack::Attack      │
│  No email. No SMS. PoW auth.    │
└─────────────────────────────────┘
```

**Guiding principle:** The Android app is the primary target. The database schema is designed from day one to be portable — the same content schema can be read by a future web frontend or backend. No Android-specific types or Room annotations should leak into the content schema definitions (use plain SQL migrations as the source of truth).

---

### Phase 0: Development Environment Setup (EndeavourOS)

#### 0.1 Install Android SDK and Tooling

```bash
# Android SDK command-line tools via AUR (no Android Studio required)
yay -S android-sdk-cmdline-tools-latest android-sdk-platform-tools android-sdk-build-tools

# Or install Android Studio if you prefer the full IDE
yay -S android-studio

# Accept licenses
yes | sdkmanager --licenses

# Set environment variables (add to ~/.bashrc or ~/.zshrc)
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin

# Install required SDK components
sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"
```

#### 0.2 Create an Android Virtual Device (AVD) for Emulation

```bash
# List available system images
sdkmanager --list | grep "system-images;android-34"

# Install a FOSS-safe system image (AOSP — no GApps)
sdkmanager "system-images;android-34;default;x86_64"

# Create AVD
avdmanager create avd \
  --name "DeFide_Test" \
  --package "system-images;android-34;default;x86_64" \
  --device "pixel_6"

# Launch emulator
emulator -avd DeFide_Test -no-snapshot-load &
```

#### 0.3 Physical Device Testing (Recommended alongside emulator)

```bash
# Install scrcpy for screen mirroring/control of a physical device
sudo pacman -S scrcpy android-tools

# Enable Developer Options on device → USB Debugging
# Verify connection
adb devices

# Mirror screen
scrcpy --turn-screen-off
```

#### 0.4 Project Scaffolding

```bash
# Clone repo (once created)
git clone https://github.com/yourusername/defide.git
cd defide

# Verify Gradle wrapper works
./gradlew assembleDebug

# Install debug APK to connected device/emulator
./gradlew installDebug
```

#### 0.5 F-Droid Reproducibility from Day One

- Use only dependencies available in Maven Central or JCenter mirrors — no Google Maven exclusives where avoidable.
- Audit `build.gradle` for any dependency that transitively pulls in `com.google.firebase`, `com.google.android.gms`, or any proprietary ad/analytics SDK.
- Use `fdroidserver` locally to validate the build early:

```bash
yay -S fdroidserver
fdroid build --verbose
```

---

### Phase 1: MVP Android App

#### 1.1 Project Structure

```
defide/
├── app/
│   ├── src/main/
│   │   ├── kotlin/app/defide/
│   │   │   ├── data/
│   │   │   │   ├── db/           # Room DAOs, entities, database class
│   │   │   │   ├── repository/   # Repository layer (data access abstraction)
│   │   │   │   └── model/        # Plain Kotlin data classes (no Room annotations)
│   │   │   ├── ui/
│   │   │   │   ├── rosary/
│   │   │   │   ├── bible/
│   │   │   │   ├── catechism/
│   │   │   │   ├── novena/
│   │   │   │   ├── prayers/
│   │   │   │   └── theme/        # Compose theme, typography, colors
│   │   │   ├── di/               # Hilt modules
│   │   │   └── MainActivity.kt
│   │   └── assets/
│   │       └── databases/        # Pre-populated SQLite content DBs
│   └── build.gradle.kts
├── content/                      # Source-of-truth content files (JSON/CSV → compiled to SQLite)
│   ├── bible/
│   ├── catechism/
│   ├── prayers/
│   └── novenas/
├── scripts/
│   └── compile_content.py        # Script to build .db files from source content
├── SUMMARY.md
├── IMPLEMENTATION.md
├── CHECKLIST.md
└── README.md
```

#### 1.2 Tech Stack

| Layer | Choice | Rationale |
|---|---|---|
| Language | Kotlin | Android standard, concise, coroutine support |
| UI | Jetpack Compose | Declarative, modern, no XML boilerplate |
| DI | Hilt | Standard Kotlin DI, good Compose integration |
| Database | Room + SQLite | Local-first; Room for type-safe queries |
| Async | Coroutines + Flow | Native Kotlin, Room supports Flow queries |
| Preferences | Jetpack DataStore | Replaces SharedPreferences, typed, async |
| Navigation | Navigation Compose | Single-activity, type-safe nav graph |
| Notifications | WorkManager + NotificationManager | No FCM dependency; FOSS-safe |
| Build | Gradle KTS | Kotlin DSL, reproducible |
| Testing | JUnit5 + Espresso + Robolectric | Standard Android testing stack |

**Explicitly excluded:** Firebase, Google Play Services, any `com.google.gms` plugin, AdMob, Crashlytics, any analytics SDK.

#### 1.3 Database Architecture

Two separate SQLite databases:

**A. Content DB (`defide_content.db`)** — read-only, bundled as an asset, never written to by the app at runtime.

```sql
-- Bible
CREATE TABLE translations (
  id TEXT PRIMARY KEY,        -- 'dra', 'vulgate', 'web'
  name TEXT NOT NULL,
  language TEXT NOT NULL,
  license TEXT NOT NULL
);

CREATE TABLE books (
  id INTEGER PRIMARY KEY,
  translation_id TEXT NOT NULL,
  book_number INTEGER NOT NULL, -- 1–73 for Catholic canon
  testament TEXT NOT NULL,      -- 'OT', 'NT', 'DC' (deuterocanonical)
  short_name TEXT NOT NULL,
  full_name TEXT NOT NULL,
  FOREIGN KEY (translation_id) REFERENCES translations(id)
);

CREATE TABLE verses (
  id INTEGER PRIMARY KEY,
  book_id INTEGER NOT NULL,
  chapter INTEGER NOT NULL,
  verse INTEGER NOT NULL,
  text TEXT NOT NULL,
  FOREIGN KEY (book_id) REFERENCES books(id)
);

CREATE VIRTUAL TABLE verses_fts USING fts5(
  text, content='verses', content_rowid='id'
);

-- Catechism
CREATE TABLE ccc_sections (
  id INTEGER PRIMARY KEY,        -- CCC paragraph number
  part INTEGER,
  section INTEGER,
  chapter INTEGER,
  article INTEGER,
  heading TEXT,
  body TEXT NOT NULL
);

CREATE VIRTUAL TABLE ccc_fts USING fts5(
  heading, body, content='ccc_sections', content_rowid='id'
);

-- Prayers
CREATE TABLE prayers (
  id TEXT PRIMARY KEY,
  title TEXT NOT NULL,
  body TEXT NOT NULL,
  source TEXT,
  category TEXT NOT NULL    -- 'morning', 'evening', 'meals', 'saints', 'devotional', etc.
);

CREATE TABLE prayer_tags (
  prayer_id TEXT NOT NULL,
  tag TEXT NOT NULL,        -- 'depression', 'anxiety', 'grief', 'gratitude', 'vocations', etc.
  PRIMARY KEY (prayer_id, tag),
  FOREIGN KEY (prayer_id) REFERENCES prayers(id)
);

CREATE VIRTUAL TABLE prayers_fts USING fts5(
  title, body, content='prayers', content_rowid='rowid'
);

-- Novenas
CREATE TABLE novenas (
  id TEXT PRIMARY KEY,
  title TEXT NOT NULL,
  description TEXT,
  total_days INTEGER NOT NULL DEFAULT 9,
  feast_day TEXT    -- ISO date string of traditional feast day, nullable
);

CREATE TABLE novena_days (
  id INTEGER PRIMARY KEY,
  novena_id TEXT NOT NULL,
  day_number INTEGER NOT NULL,
  title TEXT,
  body TEXT NOT NULL,
  FOREIGN KEY (novena_id) REFERENCES novenas(id)
);

-- Rosary mysteries
CREATE TABLE mysteries (
  id TEXT PRIMARY KEY,         -- 'joyful', 'sorrowful', 'glorious', 'luminous'
  name TEXT NOT NULL,
  traditional_days TEXT        -- e.g., 'Monday,Thursday'
);

CREATE TABLE mystery_beads (
  id INTEGER PRIMARY KEY,
  mystery_id TEXT NOT NULL,
  position INTEGER NOT NULL,
  prayer_id TEXT,              -- FK to prayers (Our Father, Hail Mary, etc.)
  mystery_number INTEGER,      -- 1–5, nullable for non-mystery beads
  mystery_title TEXT,
  mystery_scripture TEXT,
  mystery_meditation TEXT,
  FOREIGN KEY (mystery_id) REFERENCES mysteries(id)
);
```

**B. User DB (`defide_user.db`)** — read/write, local only (until sync is implemented).

```sql
-- Rosary sessions
CREATE TABLE rosary_sessions (
  id TEXT PRIMARY KEY,          -- UUID
  mystery_id TEXT NOT NULL,
  started_at INTEGER NOT NULL,  -- Unix timestamp
  completed_at INTEGER,
  completed BOOLEAN NOT NULL DEFAULT 0
);

-- Bible reading progress
CREATE TABLE bible_bookmarks (
  id TEXT PRIMARY KEY,
  translation_id TEXT NOT NULL,
  book_number INTEGER NOT NULL,
  chapter INTEGER NOT NULL,
  verse INTEGER NOT NULL,
  note TEXT,
  created_at INTEGER NOT NULL
);

CREATE TABLE bible_highlights (
  id TEXT PRIMARY KEY,
  verse_id INTEGER NOT NULL,
  color TEXT NOT NULL DEFAULT 'yellow',
  created_at INTEGER NOT NULL
);

-- Novena progress
CREATE TABLE novena_progress (
  id TEXT PRIMARY KEY,
  novena_id TEXT NOT NULL,
  start_date TEXT NOT NULL,     -- ISO 8601 date
  last_completed_day INTEGER NOT NULL DEFAULT 0,
  completed BOOLEAN NOT NULL DEFAULT 0,
  notifications_enabled BOOLEAN NOT NULL DEFAULT 0,
  notification_time TEXT        -- HH:MM, nullable
);

-- Prayer log
CREATE TABLE prayer_log (
  id TEXT PRIMARY KEY,
  prayer_id TEXT NOT NULL,
  prayed_at INTEGER NOT NULL
);

-- Settings / preferences stored in DataStore, not SQLite
-- (translation preference, theme, default mystery day assignments, etc.)
```

#### 1.4 Feature Modules

**Rosary (`ui/rosary`)**
- `RosaryHomeScreen` — select mystery, show traditional day suggestion, start session
- `RosarySessionScreen` — bead-by-bead UI, swipe or tap to advance, current prayer displayed prominently, mystery meditation accessible via expand drawer
- State managed in `RosaryViewModel` backed by a `RosarySession` in memory, persisted to `rosary_sessions` on completion
- No timer or gamification — just navigation

**Bible (`ui/bible`)**
- `BibleHomeScreen` — translation picker, book/chapter grid
- `BibleReaderScreen` — verse list, long-press to bookmark/highlight, inline search
- FTS5 full-text search across all verses in selected translation
- Translation switching without losing position (best-effort chapter match)

**Catechism (`ui/catechism`)**
- `CatechismHomeScreen` — hierarchical browser (Part → Section → Chapter → Article) + search bar
- `CatechismDetailScreen` — renders paragraph with cross-references (paragraph numbers linkable to their own entry)
- FTS5 search by keyword or paragraph number

**Prayers (`ui/prayers`)**
- `PrayerSearchScreen` — tag cloud + free-text search, results filtered by FTS5 + tag join
- `PrayerDetailScreen` — full prayer text, share as plain text option, log to `prayer_log`

**Novenas (`ui/novena`)**
- `NovenaListScreen` — browse/search novena library
- `NovenaDetailScreen` — overview, feast day info, start button with date picker (default today, adjustable)
- `NovenaSessionScreen` — daily prayer for current day with mark-as-complete; WorkManager schedules optional daily notification
- `NovenaProgressScreen` — active and completed novenas, resume button

#### 1.5 Content Pipeline

All canonical content lives as structured source files in `content/` (JSON or plain text with frontmatter). A Python script `scripts/compile_content.py` compiles these into the pre-populated `defide_content.db` SQLite file that gets checked into `app/src/main/assets/databases/`.

```
content/
├── bible/
│   └── dra/                  # One folder per translation
│       ├── metadata.json
│       └── books/
│           ├── 01-genesis.json
│           └── ...
├── catechism/
│   └── ccc_paragraphs.json
├── prayers/
│   └── prayers.json          # Array of {id, title, body, category, tags[]}
└── novenas/
    └── novenas.json          # Array of {id, title, days: [{day, title, body}]}
```

This separation means the content can be audited, diffed in git, contributed to via PRs, and reused by the future web backend.

#### 1.6 Notifications (No FCM)

WorkManager with `setExactAndAllowWhileIdle` for novena daily reminders. Uses the system `NotificationManager` directly. No push notifications requiring a server in Phase 1.

---

### Phase 2: F-Droid Submission Prep

#### 2.1 F-Droid Requirements Checklist
- No non-free dependencies (audit with `gradle dependencies` and fdroidserver's scanner)
- `fdroid/metadata/app.defide.yml` metadata file
- Reproducible build (signed APK build must be bit-for-bit reproducible from source)
- No `antifeatures:` flags (no ads, no tracking, no non-free assets)
- Source code in a public VCS repo

#### 2.2 Metadata File (`fdroid/metadata/app.defide.yml`)

```yaml
Categories:
  - Religion
License: GPL-3.0-only
SourceCode: https://github.com/yourusername/defide
IssueTracker: https://github.com/yourusername/defide/issues
Changelog: https://github.com/yourusername/defide/releases

AutoName: De Fide
Description: |-
  A privacy-first, offline-capable Catholic prayer and study companion.
  Guided rosaries, Catholic Bible translations, the Catechism of the Catholic
  Church, novenas with custom scheduling, and a searchable prayer library.
  No accounts required. No analytics. No ads. 100% FOSS.

Builds:
  - versionName: '1.0.0'
    versionCode: 1
    commit: v1.0.0
    gradle:
      - yes
```

---

### Phase 3: Web Companion (Post-MVP)

**Stack:** Ruby on Rails 8, PostgreSQL, Hotwire/Turbo for a lightweight frontend (or a separate React SPA if preferred).

The web app reads from a PostgreSQL database whose schema mirrors the SQLite content schema above. A migration script or seed task imports the compiled `defide_content.db` into Postgres.

The web companion is read-only for content browsing (Bible, Catechism, Prayers, Novenas) without an account, identical to the Android experience minus platform-specific UI.

---

### Phase 4: Accounts + Sync (Long-Term)

#### 4.1 Account Design Principles

- **Username + passphrase only** — no email, no phone number, no OAuth
- **Argon2id** password hashing server-side
- **Session tokens** — long-lived opaque tokens stored in Android's EncryptedSharedPreferences; no JWT (avoids clock-skew issues, easier revocation)
- **Zero server-side stats that aren't explicitly synced** — the server stores only what the client pushes; it never infers behavior from request patterns

#### 4.2 Bot Detection Without Email/SMS

1. **Proof-of-Work challenge at registration** — client must compute a Hashcash-style nonce before account creation request is accepted. Adds ~1–3 seconds of CPU on client, negligible for humans, expensive at scale for bots.
2. **Rate limiting** — `Rack::Attack` on Rails: max 5 registration attempts per IP per hour.
3. **Honeypot field** — invisible form field in web registration; any submission with it filled is rejected.
4. **Timing floor** — registration form must be open for at least N seconds before submission is valid (stops automated form submission).
5. **Account activation delay** — new accounts cannot sync data for 60 seconds after creation (defeats rapid-fire bot loops).

#### 4.3 Sync Protocol

- **Client-wins for user data** (bookmarks, progress, prayer log) — last-write-wins with `updated_at` timestamps
- **Server-wins for content data** — content updates pushed as versioned DB asset downloads
- Sync is opt-in per-category (user can choose to sync rosary history but not prayer log, etc.)
- All sync over HTTPS; no sync over HTTP, no sync to non-official servers without user explicitly configuring a custom endpoint (supports self-hosting)

#### 4.4 Self-Hosting Support

The Rails backend should be documented and containerized (Docker Compose) so privacy-conscious users or parishes can run their own instance. The Android app exposes an "Account server URL" setting defaulting to the official server.
