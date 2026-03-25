# CHECKLIST.md

---

## De Fide Implementation Checklist

> **Key:** `[ ]` = not started · `[~]` = in progress · `[x]` = complete
> Phases 3 and 4 are long-term; do not begin until Phase 2 is shipped.

---

### Phase 0 — Dev Environment (EndeavourOS)

- [ ] Install `android-sdk-cmdline-tools-latest`, `android-sdk-platform-tools`, `android-sdk-build-tools` via AUR
- [ ] Set `ANDROID_HOME` and update `PATH` in shell config
- [ ] Run `sdkmanager --licenses` and accept all
- [ ] Install `platforms;android-35` and `build-tools;35.0.0`
- [ ] Install AOSP system image: `system-images;android-34;default;x86_64`
- [ ] Create AVD `DeFide_Test` with `avdmanager`
- [ ] Verify emulator boots cleanly
- [ ] Install `scrcpy` and `android-tools` for physical device testing
- [ ] Enable USB debugging on test device, verify `adb devices`
- [ ] Install `fdroidserver` via AUR for early compliance testing
- [ ] Scaffold new Android project (Kotlin, Compose, min SDK 26 / target SDK 35)
- [ ] Verify `./gradlew assembleDebug` succeeds and installs on emulator

---

### Phase 1 — MVP Android App

#### Content Pipeline
- [ ] Define final JSON schemas for Bible, Catechism, Prayers, Novenas source files
- [ ] Write `scripts/compile_content.py` to produce `defide_content.db`
- [ ] Populate Douay-Rheims 1899 (DRA) full text (73 books) in `content/bible/dra/`
- [ ] Populate Clementine Vulgate in `content/bible/vulgate/` *(optional for MVP)*
- [ ] Populate CCC full text in `content/catechism/ccc_paragraphs.json`
- [ ] Populate initial prayer library with categories and tags in `content/prayers/prayers.json`
- [ ] Populate novena library in `content/novenas/novenas.json`
- [ ] Populate rosary mystery and bead data
- [ ] Compile all content to `defide_content.db` and place in `app/src/main/assets/databases/`
- [ ] Verify FTS5 virtual tables build correctly and queries return expected results

#### Database Layer (Room)
- [ ] Define Room entities for all `defide_content.db` tables (read-only)
- [ ] Define Room entities for all `defide_user.db` tables (read-write)
- [ ] Write DAOs for each entity (queries, flows, inserts)
- [ ] Write `DeFideContentDatabase` and `DeFideUserDatabase` Room database classes
- [ ] Write asset-copy helper to copy pre-populated content DB from assets on first launch
- [ ] Unit test all DAOs with Robolectric in-memory database

#### Repository Layer
- [ ] `BibleRepository` — verse queries, FTS search, bookmark CRUD
- [ ] `CatechismRepository` — section browsing, FTS search
- [ ] `PrayerRepository` — tag filtering, FTS search, prayer log CRUD
- [ ] `NovenaRepository` — novena list, day content, progress CRUD
- [ ] `RosaryRepository` — mystery + bead queries, session CRUD

#### Dependency Injection (Hilt)
- [ ] `AppModule` — provides Room databases and repositories
- [ ] `DatabaseModule` — provides DAOs

#### UI — Theme & Navigation
- [ ] Define `DeFideTheme` (Compose MaterialTheme with custom colors/typography)
- [ ] Define navigation graph with all top-level routes
- [ ] Implement bottom navigation bar (Rosary, Bible, Catechism, Prayers, Novenas)

#### UI — Rosary
- [ ] `RosaryHomeScreen` — mystery selection cards, traditional day indicator
- [ ] `RosarySessionScreen` — bead counter, prayer text display, mystery meditation drawer
- [ ] `RosaryViewModel` — session state, bead position, persist on completion
- [ ] Rosary session marked complete and written to `rosary_sessions`

#### UI — Bible
- [ ] `BibleHomeScreen` — translation selector, book grid
- [ ] `BibleChapterScreen` — chapter list for selected book
- [ ] `BibleReaderScreen` — verse list with FTS search, long-press context menu
- [ ] Bookmark creation from long-press
- [ ] Highlight creation with color picker
- [ ] Translation switching preserves chapter position where possible

#### UI — Catechism
- [ ] `CatechismHomeScreen` — hierarchical part/section browser + search bar
- [ ] `CatechismDetailScreen` — paragraph rendering with cross-reference links
- [ ] FTS search with result highlighting

#### UI — Prayers
- [ ] `PrayerSearchScreen` — tag cloud chips + free-text search input
- [ ] `PrayerDetailScreen` — full prayer text, share as plain text, log prayer
- [ ] Combine FTS and tag-filter queries in `PrayerRepository`

#### UI — Novenas
- [ ] `NovenaListScreen` — browse + search novena list
- [ ] `NovenaDetailScreen` — overview, feast day, start button with date picker
- [ ] `NovenaSessionScreen` — daily reading with mark complete
- [ ] `NovenaProgressScreen` — active / completed novenas
- [ ] WorkManager task for daily novena notification (opt-in, no FCM)
- [ ] Notification channels registered on app startup

#### Settings
- [ ] DataStore preferences: preferred Bible translation, default mystery day mapping, theme (light/dark/system), notification settings
- [ ] `SettingsScreen` exposing all user preferences

#### General
- [ ] All features work fully offline (no network calls in Phase 1)
- [ ] No `INTERNET` permission in `AndroidManifest.xml` in Phase 1
- [ ] Audit `build.gradle` for any proprietary or Google Services dependencies — remove all
- [ ] Run fdroidserver scanner: `fdroid scanner`
- [ ] Fix any scanner violations
- [ ] Write basic Espresso UI tests for each major screen
- [ ] Write unit tests for all repositories and ViewModels

---

### Phase 2 — F-Droid Submission

- [ ] Set up public GitHub (or Codeberg) repository with full source history
- [ ] Write `README.md` with description, screenshots, build instructions
- [ ] Write `CONTRIBUTING.md` with content contribution guide (how to add prayers/novenas via PR)
- [ ] Add `LICENSE` file (GPL-3.0-only)
- [ ] Create signed release keystore, document key storage approach
- [ ] Build release APK: `./gradlew assembleRelease`
- [ ] Create `fdroid/metadata/app.defide.yml`
- [ ] Prepare F-Droid submission PR to `fdroiddata` repository
- [ ] Take at least 3 screenshots for F-Droid listing
- [ ] Tag release `v1.0.0` in VCS

---

### Phase 3 — Web Companion (Post-MVP)

- [ ] Scaffold Rails 8 app (`rails new defide-web --database=postgresql --skip-action-mailer`)
- [ ] Write seed/import task to load `defide_content.db` into PostgreSQL
- [ ] Verify schema parity between SQLite content DB and PostgreSQL
- [ ] Implement Bible reader (Turbo/Stimulus or React)
- [ ] Implement Catechism browser
- [ ] Implement Prayer search
- [ ] Implement Novena browser
- [ ] Deploy to a FOSS-friendly host (fly.io, Hetzner, self-hosted)
- [ ] Write basic Capybara integration tests

---

### Phase 4 — Accounts + Sync (Long-Term)

#### Backend (Rails)
- [ ] `User` model — `username` (unique), `password_digest` (Argon2id via `bcrypt` gem + custom cost factor), no email column
- [ ] Custom session token model (opaque 256-bit token, `expires_at`)
- [ ] `Rack::Attack` — rate limit registration to 5/hr/IP
- [ ] Proof-of-work challenge endpoint (`GET /api/v1/auth/challenge` returns nonce + difficulty)
- [ ] Registration validates PoW solution before creating account
- [ ] Honeypot field validation on web registration form
- [ ] Timing floor middleware on registration endpoint
- [ ] Sync endpoints: `PUT /api/v1/sync/bookmarks`, `PUT /api/v1/sync/progress`, etc.
- [ ] All endpoints require session token in `Authorization: Bearer` header
- [ ] Write RSpec tests for all auth and sync endpoints

#### Android
- [ ] Add `INTERNET` permission to manifest (for sync only)
- [ ] `AuthRepository` — registration, login, token storage in `EncryptedSharedPreferences`
- [ ] `SyncRepository` — push local user data changes to server
- [ ] `AccountViewModel` and `AccountScreen` — login/register UI
- [ ] Settings: account server URL (default official, overridable for self-hosting)
- [ ] Sync is always opt-in; app is fully functional without account

#### Self-Hosting
- [ ] Write `docker-compose.yml` for Rails app + PostgreSQL
- [ ] Write `SELF_HOSTING.md` setup guide
- [ ] Verify Android app connects and syncs to a local self-hosted instance
