# Vocabuilder

[![CI](https://github.com/abhi-k9/Vocabuilder/actions/workflows/ci.yml/badge.svg)](https://github.com/abhi-k9/Vocabuilder/actions/workflows/ci.yml)

Vocabuilder helps you learn the vocabulary of the GRE with flashcards: 1,000 words in 20 decks,
each coming back just as you're about to forget it. It's free, with no ads and no account, and it
works offline: the app has no Internet access.

Download the APK from the [latest release](https://github.com/abhi-k9/Vocabuilder/releases/latest).
It runs on Android 8.0 and newer.

## Usage

- **Today** has your daily review: the words due today, plus a few new ones (10 a day by default).
  It also offers the deck to continue with, your starred words and your overall progress.
- **Decks** lists the 20 decks, from common to advanced words, with how much of each you've
  learned. Open one to study it, see its words or reset it.
- **Cards**: tap a card to turn it over, then answer *Knew it* or *Didn't know*, or swipe it right
  or left. A missed card comes back a few cards later, and the last answer can be undone. A session
  ends with a summary.
- **I know this** takes a word you already know out of study (its page can bring it back); a
  **star** marks a word to study with the other starred ones.
- **Words**: search by word or by its main meaning (ignoring case and accents), filter by
  progress or star, and open a word's page: its meanings with examples, synonyms and antonyms, a
  note on a look-alike word or a second meaning for some, and when it's due.
- **Pronunciation**: the speaker button says a word aloud with your device's text-to-speech (when
  it has an English voice), or turn on *Pronounce automatically* to hear each word when its card
  is turned over.
- **Settings**: the number of new words a day (0 to 50), whether cards show the word or its meaning
  first, pronouncing automatically, shuffling, the theme, dynamic color (Android 12 and newer) and
  resetting all progress.

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

## Development

Requirements: JDK 21 or newer, and the Android SDK (Android Studio sets this up for you).

```bash
./gradlew installDebug                                          # build and install on a device
./gradlew spotlessApply                                         # format the code
./gradlew :core:model:test :core:domain:test testDebugUnitTest  # unit tests
./gradlew :app:lintDebug                                        # Android Lint
```

Debug builds have the application ID suffix `.debug`, so they can be installed next to a release.
[CI](.github/workflows/ci.yml) runs the formatting check, unit tests, lint and builds on pushes
and pull requests, and checks that the Room schema is committed.

The app is written in Kotlin with Jetpack Compose, Material 3, Hilt, Room and Navigation 3. The
screens are in the `feature:*` modules, and the scheduler and study logic are plain Kotlin in
`core:domain`, unit tested on the JVM.

### Releasing

Release builds are minified with R8 and signed with the key described by `keystore.properties` in
the project root (git-ignored), or with the debug key without it:

```properties
storeFile=path/to/release.jks
storePassword=…
keyAlias=…
keyPassword=…
```

The [Release workflow](.github/workflows/release.yml) publishes a GitHub release with the signed
APK, its R8 mapping and its SHA-256 checksum. Run it from the Actions tab with a version such as
`1.2.0` (it tags the chosen branch's latest commit `v1.2.0`), or push a `v1.2.0` tag. Each part of
the version must be between 0 and 99, as it sets the version code (`10200`). Local builds are
version `1.0.0` unless you pass `-Pvocabuilder.versionName=1.2.0`. The workflow needs these
repository secrets:

| Secret | Value |
|--------|-------|
| `RELEASE_KEYSTORE_BASE64` | The keystore, base64-encoded on one line (`base64 -w 0 release.jks`) |
| `RELEASE_KEYSTORE_PASSWORD` | The keystore's password |
| `RELEASE_KEY_ALIAS` | The key's alias |
| `RELEASE_KEY_PASSWORD` | Only if the key's password differs from the keystore's |
