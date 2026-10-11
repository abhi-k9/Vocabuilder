# Vocabuilder

[![CI](https://github.com/abhi-k9/Vocabuilder/actions/workflows/ci.yml/badge.svg)](https://github.com/abhi-k9/Vocabuilder/actions/workflows/ci.yml)

An Android app for building exam vocabulary with flashcards and spaced repetition, starting with
1,000 GRE words. It's free, has no ads, needs no account and never goes online: your progress stays
on your device.

Built with Kotlin, Jetpack Compose and Material 3, following Google's recommended app architecture.
See the [plan](docs/PLAN.md) for what's next.

## Features

- **1,000 GRE words** in 20 decks of 50, from common to advanced, each with original definitions,
  example sentences, synonyms and antonyms, and notes on easily confused words ("adverse" and
  "averse") and secondary meanings ("table", "temper", "flag").
- **Flashcards**: tap a card to turn it over, then answer *Knew it* or *Didn't know*, or swipe right
  or left. Missed cards come back a few cards later, and the last answer can be undone. The front
  can show the word or its meaning.
- **Spaced repetition**: each word known moves up a stage, reviewed after 1, 3, 7, 16, 35 and 90
  days; a missed word starts over. Words known for 16 days or more count as mastered.
- **Today**: a daily review of the words due plus a few new ones (10 a day by default), the deck to
  continue with, and your progress.
- **Decks** with their progress (new, learning, reviewing and mastered words), each with its word
  list. A caught-up deck can be practiced without changing its schedule, or reset.
- **I know this** takes a word out of study, and **stars** mark words to study on their own.
- **Words**: search all words and their meanings (ignoring case and accents), filter by state or
  stars, and open a word's page with its senses, related words and progress.
- **Pronunciation** with the device's text-to-speech, on demand or when a card is turned over.
- Light and dark themes, dynamic color, an adaptive layout (navigation rail on tablets), and
  progress included in Android's backup and device transfer.

## Architecture

The app follows a layered, multi-module architecture with unidirectional data flow, like
[ExpiryWatch](https://github.com/abhi-k9/ExpiryWatch):

| Module | Contents |
|--------|----------|
| `app` | Application, activity, navigation shell |
| `feature:home`, `feature:decks`, `feature:study`, `feature:words`, `feature:settings` | Screens and their ViewModels |
| `core:model` | Domain models (pure Kotlin) |
| `core:domain` | The scheduler, study sessions, use cases and repository interfaces (pure Kotlin) |
| `core:content` | The bundled word packs and their loader and validator |
| `core:data` | Repository implementations; copies the bundled packs into the database |
| `core:database` | Room database |
| `core:datastore` | User settings |
| `core:speech` | Text-to-speech |
| `core:designsystem`, `core:ui` | Theme, mastery colors and shared components |
| `core:navigation`, `core:common`, `core:testing` | Navigation keys, dispatchers and clock, test fakes |
| `build-logic` | Gradle convention plugins |

The word packs are JSON files in `core/content/src/main/assets/content`; see the
[content guide](docs/CONTENT.md) for their format and style rules, which a test checks.

## Building

Requirements: JDK 17 or newer, and the Android SDK (Android Studio sets this up for you).

```bash
./gradlew assembleDebug      # build
./gradlew installDebug       # install on a connected device
```

Debug builds use the application ID suffix `.debug`, so they can be installed next to a release
build.

### Release signing

Release builds are minified with R8. To sign them with your own key, create `keystore.properties`
in the project root (it's git-ignored):

```properties
storeFile=path/to/release.jks
storePassword=…
keyAlias=…
keyPassword=…
```

Without it, release builds are signed with the debug key.

## Testing and checks

```bash
./gradlew spotlessApply                                      # format the code
./gradlew :core:model:test :core:domain:test testDebugUnitTest  # unit tests
./gradlew :app:lintDebug                                     # Android Lint
```

Domain logic has plain JVM unit tests, the database is tested with Robolectric, and ViewModels are
tested with the fakes in `core:testing`.

The [CI workflow](.github/workflows/ci.yml) runs the formatting check, unit tests, debug and release
builds and lint on every push, and checks that the exported Room schema is committed.
