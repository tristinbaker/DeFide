# SUMMARY.md

---

## De Fide — A Privacy-First Catholic Companion App

### What It Is

De Fide is a free, open-source (FOSS) Android application and eventual web companion for Catholic spiritual life. It bundles the tools a practicing Catholic needs for daily prayer and study — guided rosaries, multiple Bible translations, the Catechism of the Catholic Church, novenas with flexible scheduling, and a searchable prayer library — entirely offline and without any surveillance, analytics, or data harvesting.

### Mission Statement

> To build a dignified, trustworthy digital space for Catholic prayer and study — one that respects the user's privacy as seriously as their faith.

Most Catholic apps today are ad-supported, require accounts, phone numbers or email addresses, and report behavioral data to third parties. De Fide rejects that model entirely. It is local-first, account-optional, and designed to run comfortably on privacy-respecting Android builds like GrapheneOS and CalyxOS, and to be distributed through F-Droid.

### Core Features (MVP)

- **Guided Rosary** — all five mysteries (Joyful, Sorrowful, Glorious, Luminous), with bead-by-bead navigation, prayer text, and optional mystery meditations.
- **Bible** — full Catholic canon (73 books) in at least one public domain translation (Douay-Rheims 1899 American Edition) at launch, with architecture supporting additional translations (see Copyright Notes).
- **Catechism of the Catholic Church (CCC)** — full text, searchable by keyword and paragraph number, browsable by Part > Section > Chapter > Article.
- **Novenas** — full library of traditional novenas, each with configurable start dates so users can observe them at any liturgical or personal date rather than being locked to a canonical calendar.
- **Prayer Library** — searchable by circumstance/tag (e.g., "depression", "anxiety", "gratitude", "vocations", "grief") and by category (morning, evening, before meals, saints' intercessions, etc.).

### Long-Term Features

- **Accounts** — username + passphrase only. No email. No phone number. Bot detection via proof-of-work challenge at registration. Syncs progress and stats across devices.
- **Web Companion** — same content and account login via browser, using the same backend and synced database schema.
- **Liturgical Calendar** — daily Mass readings, feast days, fasting reminders.
- **Examination of Conscience** — guided, customizable.

### Values & Non-Goals

| Value | What it means |
|---|---|
| **No tracking** | Zero analytics SDKs, no crash reporters phoning home, no behavioral telemetry |
| **Local-first** | Every feature works 100% offline. Network is only ever used for optional sync. |
| **FOSS** | GPL-3.0 or AGPL-3.0 licensed. No proprietary dependencies. F-Droid compliant. |
| **No dark patterns** | No gamification designed to foster compulsion. Stats are informational, not manipulative. |
| **Minimal account data** | If a user creates an account, only a username and hashed passphrase are stored server-side. |

**Non-goals:** De Fide will never run ads, sell data, require Google Play Services, use Firebase, or implement email/SMS verification.

### Copyright Notes on Bible Translations

> **Important:** Several popular Catholic translations (NRSV-CE, RSV-CE, NJB, NAB-RE) are under active copyright and cannot be bundled in a FOSS app without a license. The implementation plan starts with fully public domain texts and treats licensed translations as opt-in downloadable content packs (if licensing can be obtained).

| Translation | Status |
|---|---|
| Douay-Rheims 1899 (DRA) | ✅ Public domain — bundle freely |
| Clementine Vulgate (Latin) | ✅ Public domain — bundle freely |
| World English Bible Catholic Ed. | ✅ Public domain — bundle freely |
| NRSV-CE | ⚠️ Copyrighted (NCC) — requires license |
| RSV-CE 2nd Ed. | ⚠️ Copyrighted (Ignatius Press) — requires license |
| Knox Bible | ⚠️ Copyrighted (Baronius Press) |
