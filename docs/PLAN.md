# Vocabuilder: product and build plan

Status: **1.3.0 released.** All of v1 is built, and from v1.1 the quiz (a word's meaning only), a
history screen with the streak, the daily reminder and diagnostics, plus two things the plan didn't
have: a dictionary for the synonyms and antonyms that aren't words of the app, and trouble words.
[Section 6](#6-build-order-and-releases) has what's left, and [section 7](#7-decisions) the answers
to the plan's open questions.

## 1. Goal

An Android vocabulary app for exam prep (GRE first), in the spirit of
[Magoosh's GRE Flashcards](https://play.google.com/store/apps/details?id=com.magoosh.flashcards.gre):
short decks of high-value words, a card you flip, and an honest "I knew it / I didn't" that decides
when the word comes back. On top of that, it fixes what Magoosh's users complain about and borrows the
best ideas from Magoosh's other apps and from competing apps.

It follows the conventions of your other apps: Kotlin, Jetpack Compose, Material 3, ExpiryWatch's
multi-module architecture, free with no ads, no account, and no Internet permission.

## 2. What the research found

The Play Store and App Store pages can't be reached from the build container, so this comes from
search results (store mirrors, review aggregators, forums) plus what's known about the apps. Treat
the user-feedback part as indicative, not a survey.

### 2.1 Magoosh GRE Flashcards (the reference app)

| Aspect | What it does |
|--------|--------------|
| Content | ~1,000 GRE words on Android (1,500 on iOS), each with a definition and at least one example sentence |
| Structure | Decks by difficulty (common, basic/intermediate, advanced), ~50 words each; start at any level |
| Study loop | Word on the front, tap to flip; mark *I knew this word* / *I didn't know this word* |
| Scheduling | Spaced repetition: missed words come back often, known words less and less, mastered words not at all |
| Progress | Per-deck progress bars, words mastered vs. still learning |
| Account | Optional Magoosh account to sync progress to the web |
| Ratings | ~4.7–4.8 stars (tens of thousands of ratings); content quality is the main praise |

### 2.2 Magoosh's other apps (same developer)

- **Flashcards for other exams**: TOEFL, IELTS, SAT, ACT vocabulary; GMAT *math* flashcards.
  → One app with **word packs** instead of one app per exam.
- **Vocabulary Builder**: multiple-choice quizzes in levels; beating a level unlocks a harder one,
  missed words come back until learned. → **Quiz mode** with levels.
- **Prep & Practice apps**: lessons and practice questions per exam. → Later: **GRE-style practice**
  (sentence equivalence) generated from the word data.

### 2.3 What users ask for or complain about (Magoosh and similar apps)

| Feedback | Response in Vocabuilder |
|----------|-------------------------|
| "Can't skip words I already know, or star words I want to focus on" | **Already know** (removes a word from study) and **Star** (a starred-words session) |
| "Let us add our own words" | **Custom words and decks**, plus CSV import |
| "Some practice beyond flashcards would help" | **Quiz mode** with several question types |
| "Progress reset to the first day" | Progress stored locally, keyed by stable word IDs so content updates never reset it; **backup and restore** |
| Buttons overlap the system bars on some phones | Edge-to-edge layout with proper insets, tested on small and large screens |
| Intrusive ads during study (other GRE apps) | No ads, ever; every feature free |
| Review games behind a paywall (other vocab apps) | Everything free |
| Want pronunciation / listening | **Pronunciation** with the system text-to-speech (offline) |
| Want more meanings per part of speech | Words can have **several senses**, each with its own example |
| Want a configurable review schedule | **New words per day** and **mastery threshold** in Settings |

### 2.4 Good ideas from competitors

- **Anki / FSRS**: a daily review queue mixing due reviews and a few new words. (Simpler scheduler in
  v1, see §5.2.)
- **Vocabulary.com**: varied question types per word; review continues after mastery.
- **GregMat word groups**: words clustered by meaning (e.g. *praise*: extol, laud, eulogize), which is
  how GRE sentence-equivalence questions test them.
- **GRE Vocabulary Cartoons**: mnemonics and a streak.
- **Word of the day** widgets and gentle daily reminders (common across vocab apps).

## 3. Reused from your other apps

| From | What |
|------|------|
| ExpiryWatch | Module layout (`app`, `core:*`, `feature:*`, `build-logic` convention plugins), Hilt, Room, DataStore, Navigation 3, WorkManager, Glance widget, adaptive navigation (rail on tablets), swipe gestures with undo, JSON backup/restore with its own versioned format, Insights-style stats screen, opt-in diagnostics log, CI workflow (format, unit tests, build, lint, Room schema check), release workflow with signed APKs, Spotless + ktlint |
| ExpiryWatch / ZzzTimer / CaffeineDose | Theme approach: Material 3 tonal-spot palette generated from one brand seed, dynamic color toggle, System/Light/Dark; semantic colors that stay fixed under dynamic color (ExpiryWatch's `StatusColors` becomes **mastery colors**); rounded shapes scale (6/10/16/24/32 dp) |
| ZzzTimer / CaffeineDose | `PRIVACY.md`, fastlane metadata, README with "verifying a download" |
| ExpiryWatch-Play | Optional later: a Play edition with a welcome message and optional tips |

**Brand seed**: the existing apps use green (`#3F7D3A`), indigo (`#4F5B92`) and coffee (`#7B4F2C`).
Proposed for Vocabuilder: a deep **ink teal `#1F6F78`**, distinct from the others and calm for long
study sessions. **Mastery colors**: New (neutral grey), Learning (amber), Reviewing (blue),
Mastered (green).

Package: `io.github.abhik9.vocabuilder` (debug: `.debug` suffix), like the other apps.

## 4. Features

### v1: Magoosh parity, done better

1. **Home**: today's review count, daily goal and streak, *Continue* (last deck), *Daily review*.
2. **Decks**: GRE pack in three levels (Common, Intermediate, Advanced), ~50 words per deck, each
   with a progress bar split by mastery state. Start anywhere.
3. **Flashcard session**: word on the front (with part of speech), tap to flip to definition(s) and
   example sentence(s). *Knew it* / *Didn't know* buttons, or swipe right / left; undo the last
   answer. Missed words come back later in the same session, as in Magoosh. Session summary at the
   end.
4. **Spaced repetition and mastery** (§5.2): every word is New, Learning, Reviewing or Mastered.
5. **Daily review**: due words across all decks plus up to *N* new words per day.
6. **Star and Already know**: from the card or the word page.
7. **Word browser**: search (ignores case and accents), filter by deck, level, state, starred; word
   detail page with all senses, examples, synonyms and antonyms, related words, your note.
8. **Pronunciation**: speaker button using the device's text-to-speech; optional auto-play on flip.
9. **Settings**: theme, dynamic color, new words per day, card front (word or definition first),
   auto-pronounce, reset progress (per deck or all, with confirmation).

### v1.1: beyond Magoosh

10. **Quiz mode**: multiple choice (word → definition, definition → word, fill in the blank from the
    example sentence, pick the synonym); levels that unlock like Magoosh's Vocabulary Builder.
    Answers feed the same mastery schedule. *Built in 1.3.0 in part: word → definition, for the daily
    review and any deck.*
11. **Stats**: words mastered over time, reviews per day, accuracy, upcoming reviews forecast, a
    calendar heatmap; streak and daily goal. *Built in 1.3.0 in part: a history screen with the
    current and longest streaks, the answers of each of the last four weeks' days, and the words
    missed most.*
12. **Daily reminder** notification at a chosen time ("12 words to review"), skipped once you've
    studied that day. *Built in 1.3.0.*
13. **Word of the day** home-screen widget (Glance), tap to open the word.
14. **Personal notes / mnemonics** on any word.
15. **Custom words and decks**: add, edit, delete; CSV import (word, part of speech, definition,
    example) and export.
16. **Backup and restore** to a JSON file (progress, stars, notes, custom words, settings).
17. **Diagnostics**: opt-in log export, as in the other apps. *Built in 1.1.0.*

### Added along the way

- **Dictionary** (1.2.0): the meanings of the synonyms and antonyms that aren't words of the app,
  from Open English WordNet (CC BY 4.0), so any of them on a card or word page can be looked up.
- **Trouble words** (1.3.0): the words missed at least twice and not yet mastered, studied together.
- **Flick to answer** (1.3.0): a quick flick answers a turned card, as a long drag does.

### Later (not scheduled)

- More word packs: SAT, TOEFL, IELTS (the content format supports packs from day one).
- **Word groups** by meaning and a GRE **sentence-equivalence** drill generated from them
  ("pick the two words that fit").
- **Word roots** (Latin/Greek) with the words built from them.
- Spelling / typing recall mode.
- FSRS scheduler as an option.
- Play edition with optional tips (as ExpiryWatch-Play).

## 5. Design

### 5.1 Architecture and modules

Mirrors ExpiryWatch: UI → ViewModel → use cases (pure Kotlin) → repositories.

The modules as built:

| Module | Contents |
|--------|----------|
| `app` | Application, activity, navigation shell, work started with the process |
| `feature:home` | Today (daily review, streak, the deck to continue, starred and trouble words) and History |
| `feature:decks` | Decks and deck detail |
| `feature:study` | Flashcard and quiz sessions, and the summary |
| `feature:words` | Word browser, search and word page |
| `feature:settings` | Settings, the reminder's time, diagnostics export |
| `core:model` | Word, Sense, Deck, Pack, WordProgress, History… (pure Kotlin) |
| `core:domain` | Scheduler, study plan and session, quiz choices, today and history, repository interfaces (pure Kotlin) |
| `core:content` | Bundled word packs and dictionary (JSON assets), loader and validation |
| `core:database` | Room: content tables, progress, review log |
| `core:datastore` | User settings |
| `core:data` | Repository implementations, content seeding |
| `core:speech` | Text-to-speech wrapper |
| `core:reminders` | Daily reminder: its alarm, receiver and notification |
| `core:diagnostics` | Opt-in diagnostics log and its report |
| `core:designsystem`, `core:ui`, `core:navigation`, `core:common`, `core:testing` | As in ExpiryWatch |

Still to come: a `feature:widget` module for the word of the day; the backup and CSV codecs in
`core:domain`.

### 5.2 Scheduling and mastery

A Leitner-style schedule: simple, predictable and easy to explain in the UI.

| Stage | Next review after *Knew it* | State shown |
|-------|----------------------------|-------------|
| 0 | in this session | New / Learning |
| 1 | 1 day | Reviewing |
| 2 | 3 days | Reviewing |
| 3 | 7 days | Reviewing |
| 4 | 16 days | Mastered |
| 5 | 35 days | Mastered |
| 6 | 90 days | Mastered |

- *Didn't know* drops the word to stage 0 and brings it back a few cards later in the same session.
- *Knew it* on a word's very first appearance skips ahead (to stage 3), so you don't waste reviews on
  words you already know; *Already know* skips it entirely.
- Mastered words still come back occasionally, so they stay mastered.
- Every answer is logged (time, mode, result), which feeds stats and lets the scheduler be swapped
  later (e.g. for FSRS) without losing history.

### 5.3 Content

- **Original content**: Magoosh's definitions and example sentences are copyrighted, so none are
  copied. The word list is selected from words widely recognized as high-frequency on the GRE, and
  every definition and example sentence is written fresh for this app.
- Each word: stable ID (`gre:abate`), headword, senses (part of speech, definition, example), synonyms,
  antonyms, level, optional note on usage or tone.
- Source files live in `core/content/src/main/assets/content/` as one JSON file per deck, reviewed
  like code (see [the content guide](CONTENT.md)). A JVM test validates
  them: unique IDs, every field present, the example uses the word, synonyms that are themselves in the
  pack link to each other, no duplicate words across decks.
- On first launch and whenever the bundled content version changes, it is seeded into Room. Progress
  is keyed by word ID, so updates never touch it; removed words keep their history.

### 5.4 Privacy

No Internet permission, no analytics, no ads, no account. Your progress, notes and settings stay on
the device, plus Android's own device backup and phone-to-phone transfer (as in ExpiryWatch), so a new
phone doesn't mean starting over. The diagnostics log is excluded from those backups and only leaves the
device if you export it.

## 6. Build order and releases

One commit per feature, or a few for a large one.

| # | Commit | Planned for | Released in |
|---|--------|-------------|-------------|
| 1 | Project scaffold: Gradle, version catalog, convention plugins, Spotless/ktlint, `.editorconfig`, README | v1 | 1.0.0 |
| 2 | CI workflow (format, unit tests, build, lint, Room schema) | v1 | 1.0.0 |
| 3 | Design system: theme, mastery colors, shapes, base components, app icon | v1 | 1.0.0 |
| 4 | Domain models and content format, with the validation test | v1 | 1.0.0 |
| 5–7 | GRE word pack: Common, Intermediate, Advanced (one commit per level) | v1 | 1.0.0 |
| 8 | Room database, content seeding, repositories | v1 | 1.0.0 |
| 9 | Scheduler and session builder (domain, unit tested) | v1 | 1.0.0 |
| 10 | App shell: navigation, adaptive layout, settings storage | v1 | 1.0.0 |
| 11 | Decks screen and deck detail | v1 | 1.0.0 |
| 12 | Flashcard session (flip, answer, swipe, undo, summary) | v1 | 1.0.0 |
| 13 | Home dashboard and daily review | v1 | 1.0.0 |
| 14 | Star and Already know | v1 | 1.0.0 |
| 15 | Word browser, search and word detail | v1 | 1.0.0 |
| 16 | Pronunciation | v1 | 1.0.0 |
| 17 | Settings screen | v1 | 1.0.0 |
| 18 | Quiz mode | v1.1 | 1.3.0 (word → definition only) |
| 19 | Stats and streak | v1.1 | 1.3.0 (history and streak) |
| 20 | Daily reminder | v1.1 | 1.3.0 |
| 21 | Word of the day widget | v1.1 | Not yet |
| 22 | Notes and mnemonics | v1.1 | Not yet |
| 23 | Custom words and decks, CSV import/export | v1.1 | Not yet |
| 24 | Backup and restore | v1.1 | Not yet |
| 25 | Diagnostics | v1.1 | 1.1.0 |
| 26 | Release: workflow, signing, `PRIVACY.md`, fastlane metadata | v1.1 | 1.0.0 (no `PRIVACY.md` or fastlane metadata yet) |

Each feature commit includes its tests: JVM unit tests for domain logic, Robolectric for Room and
ViewModels, as in ExpiryWatch.

The releases so far:

| Release | What's new |
|---------|------------|
| 1.0.0 to 1.0.2 | v1; cards shuffled by default; a card's synonyms and antonyms open their words |
| 1.1.0 | A fix for the crash when starting a study session; diagnostics; the device test workflow |
| 1.2.0 | The dictionary for the synonyms and antonyms that aren't words of the app |
| 1.3.0 | Quiz, history and streak, trouble words, the daily reminder, flick to answer |

**Build environment note**: this container can't download the Android SDK (its host is blocked), so
pure-Kotlin modules (`core:common`, `core:model`, `core:domain`, `core:testing`) are built and tested
here, and the Android build, lint and Robolectric tests run in GitHub Actions: on `main` and pull
requests, and from the Actions tab for other branches. Failures there get fixed before releasing.

## 7. Decisions

The plan's open questions, as answered (each with the recommended option):

1. **Platform**: Android only, native Kotlin and Compose like the other apps.
2. **Exams**: GRE only for now; the content format allows packs for SAT, TOEFL and IELTS later.
3. **Word count**: ~1,000 GRE words, in 20 decks.
4. **Content**: original definitions and examples, written for the app. Open English WordNet is used
   only for the dictionary of synonyms and antonyms that aren't words of the app (1.2.0).
5. **Scope**: v1 first, then a review before v1.1.
6. **Distribution**: GitHub releases, like the other apps; a Play edition maybe later.
7. **Theme**: ink teal `#1F6F78` as the brand seed.
8. **Name**: "Vocabuilder", with the application ID `io.github.abhik9.vocabuilder`.
9. **Default branch**: `main`, created from the first working branch.
