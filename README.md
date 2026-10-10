# Vocabuilder

[![CI](https://github.com/abhi-k9/Vocabuilder/actions/workflows/ci.yml/badge.svg)](https://github.com/abhi-k9/Vocabuilder/actions/workflows/ci.yml)

An Android app for building exam vocabulary (GRE first) with flashcards and spaced repetition.

Built with Kotlin, Jetpack Compose and Material 3, following Google's recommended app architecture.
See the [plan](docs/PLAN.md) for the features under way.

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
./gradlew spotlessApply      # format the code
./gradlew testDebugUnitTest  # unit tests
./gradlew :app:lintDebug     # Android Lint
```

The [CI workflow](.github/workflows/ci.yml) runs the formatting check, unit tests, debug and release
builds and lint on every push.
