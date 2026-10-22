# Vocabuilder

[![CI](https://github.com/abhi-k9/Vocabuilder/actions/workflows/ci.yml/badge.svg)](https://github.com/abhi-k9/Vocabuilder/actions/workflows/ci.yml)

Vocabuilder helps you learn the vocabulary of the GRE with flashcards: 1,000 words in 20 decks,
each coming back just as you're about to forget it. It's free, with no ads and no account, and it
works offline: the app has no Internet access.

Download the APK from the [latest release](https://github.com/abhi-k9/Vocabuilder/releases/latest).
It runs on Android 8.0 and newer.

## Usage

- **Today** has your daily review: the words due today, plus a few new ones (10 a day by default).
  It also shows your streak (the days in a row you've studied) and offers the deck to continue
  with, your starred words, your trouble words and your overall progress.
- **Decks** lists the 20 decks, from common to advanced words, with how much of each you've
  learned. Open one to study it, see its words or reset it.
- **Cards** come in random order. Tap a card to turn it over, then answer *Knew it* or *Didn't
  know*, or swipe it right or left: a long drag or a quick flick. A card not yet turned over can't be
  swiped. A missed card comes back a few cards later, and the last answer can be undone. A session
  ends with a summary.
- **Quiz**: the daily review and each deck can be studied as a quiz instead, picking the word's
  meaning among four. A right pick counts as *Knew it*, a wrong one as *Didn't know*.
- **Trouble words** are the words you've missed at least twice and not yet mastered. Today offers
  them to study together, the most missed first (up to 30).
- **History** (from *Your progress*): your current and longest streaks, the answers of each of the
  last four weeks' days, known and missed, and the words you miss most.
- **Synonyms and antonyms** on a card's back can be tapped to see their meanings and hear them:
  their own when they're words of the app, or else the dictionary's.
- **I know this** takes a word you already know out of study (its page can bring it back); a
  **star** marks a word to study with the other starred ones.
- **Words**: search by word or by its main meaning (ignoring case and accents), filter by
  progress or star, and open a word's page: its meanings with examples, synonyms and antonyms (to
  open, or to look up in the dictionary), a note on a look-alike word or a second meaning for some,
  and when it's due.
- **Pronunciation**: the speaker button says a word aloud with your device's text-to-speech (when
  it has an English voice), or turn on *Pronounce automatically* to hear each word when its card
  is turned over.
- **Daily reminder** (off by default): a notification at the time you pick, on days there are words
  to review. It's set as an inexact alarm, so it can come a few minutes late while the phone
  sleeps. On Android 13 and newer, turning it on asks for permission to send notifications.
- **Settings**: the number of new words a day (0 to 50), whether cards show the word or its meaning
  first, pronouncing automatically, random or alphabetical order, the daily reminder, the theme,
  dynamic color (Android 12 and newer) and resetting all progress.

Progress is included in Android's backup and device-to-device transfer.

## How words come back

Each time you know a word, it moves up a stage and comes back later: after 1, 3, 7, 16, 35, then
90 days.

- A word you know the first time you see it starts at 7 days.
- A word you miss starts over: it comes back a few cards later until you know it, then the next
  day.
- From 16 days on, a word counts as **mastered**, as do words marked as known. Before that, it's
  **learning** (just missed) or **reviewing**.
- A deck you're caught up with can be practiced anyway. Practicing doesn't move words ahead of
  their schedule, but a word you miss still starts over.

## Words

The definitions, example sentences and notes are written for Vocabuilder. The word lists are JSON
files in [`core/content/src/main/assets/content`](core/content/src/main/assets/content); the
[content guide](docs/CONTENT.md) describes their format and the style rules a test checks them
against.

The meanings of the synonyms and antonyms that aren't words of the app come from [Open English
WordNet](https://en-word.net/) 2025, by the Open English WordNet team, under [CC BY
4.0](https://creativecommons.org/licenses/by/4.0/), and derived from Princeton WordNet (see the
[license](core/content/src/main/assets/content/dictionary/LICENSE.txt)); a few that WordNet doesn't
have are written for Vocabuilder.

## Diagnostics

To investigate an issue, turn on **Record diagnostics** in Settings, reproduce the issue, then
**Export log** to a file and attach it to the report. Debug builds record by default.

The log records what the app does: process starts and crashes, the screens opened, study sessions
and answers (by word), settings changes, the daily reminder, copying the word lists into the
database and the text-to-speech engine. The export starts with the app's version, the device, recent process exits
and how many words are at each stage. It's capped at about 512 KB, isn't included in backups, and
never leaves the device unless you export it.

Release builds are obfuscated by R8, so a crash in the log names classes like `x21`. To read it, run
the [Retrace workflow](.github/workflows/retrace.yml) from the Actions tab with the version shown
at the top of the log, and paste the stack trace. The [Device test
workflow](.github/workflows/device-test.yml) goes through the app on an emulator, and retraces a
crash the same way.

## Development

Requirements: JDK 21 or newer, and the Android SDK (Android Studio sets this up for you).

```bash
./gradlew installDebug       # build and install on a device
./gradlew spotlessApply      # format the code
./gradlew :core:common:test :core:model:test :core:domain:test testDebugUnitTest
./gradlew :app:lintDebug     # Android Lint
```

Debug builds have the application ID suffix `.debug`, so they can be installed next to a release.
[CI](.github/workflows/ci.yml) runs the formatting check, unit tests, lint and a release build,
and checks that the Room schema is committed, in one job. To save Actions minutes it runs only for
main and pull requests, skips changes to docs and scripts, and runs for other branches from the
Actions tab.

The app is written in Kotlin with Jetpack Compose, Material 3, Hilt, Room and Navigation 3. The
screens are in the `feature:*` modules, and the scheduler and study logic are plain Kotlin in
`core:domain`, unit tested on the JVM. The [plan](docs/PLAN.md) lists every module, what's been
built and what's still to come.
