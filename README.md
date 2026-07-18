# Count & Play

A native Android app that teaches a young child addition and subtraction visually. Numbers up to 20, everything narrated aloud, nothing ever vanishes — taken-away items move to a visible "went away" area and stay countable.

- **Play Store package:** `app.maqsadah.count_and_play.twa` (closed testing)
- **License:** MIT

## The app

Two modes:

**Learn mode** — you or your child pick the numbers (up to 20) and the operation, and the app acts it out. Addition shows two differently-colored fruit groups counted aloud, then merged into one basket with "counting on" (five... six, seven, eight). Subtraction shows the full group counted, then the child taps exactly the right number of fruits into empty take-away slots; the taken group fades but stays visible, and the remainder is counted.

**Quiz mode** — every problem is acted out visually first, then the child picks from three big number buttons. Wrong answers never fail; the app recounts everything slowly with each fruit highlighted.

Design constraints honored throughout: no human or animal figures (fruits, stars, balloons, balls, cars only), voice narration for every action (Android's built-in text-to-speech), huge touch targets, no ads, no data collection, no network access at all, no fail states.

## Tech

Native Kotlin + Jetpack Compose. Fully offline — the manifest requests **zero permissions**. Version 2.0.0 is a ground-up native rewrite of the original web/PWA version (v1 shipped as a Trusted Web Activity; the `.twa` suffix in the package ID is a leftover from that era and can never change, since Play permanently ties an app to its first package ID).

The original web version still lives at https://count-and-play.netlify.app and its source is preserved in this repo's git history (tag `web-final`).

## Building

Every push to `main` triggers GitHub Actions (`.github/workflows/build.yml`), which builds a **signed release AAB and APK** on GitHub's servers. Download them from the workflow run's artifacts.

Signing uses four repository secrets (Settings → Secrets and variables → Actions):

| Secret | Value |
|---|---|
| `KEYSTORE_BASE64` | the release keystore, base64-encoded |
| `KEYSTORE_PASSWORD` | keystore password |
| `KEY_ALIAS` | key alias |
| `KEY_PASSWORD` | key password |

The keystore itself is **never** committed (see `.gitignore`). If it is ever lost, the app can never be updated again — keep offline backups.

Local builds work too: open the repo in Android Studio, or run `gradle :app:assembleDebug`. Without the keystore, release builds are unsigned.

Release builds are **minified** (R8 code + resource shrinking). The first minified build should be smoke-tested from the closed-testing track before promoting to production.

**Gradle wrapper:** this repo pins Gradle 8.10.2 via the committed wrapper. Always build with it — `./gradlew` (macOS/Linux) or `gradlew.bat` (Windows) — so local and CI use the same version. CI validates the wrapper jar against Gradle's known-good checksums on every run.

**Tests:** pure game logic is unit-tested (`app/src/test/…/GameLogicTest.kt`). Run them with:

```
./gradlew :app:testDebugUnitTest
```

## Releasing an update

1. Edit the code; bump `versionCode` (and `versionName`) in `app/build.gradle.kts`.
2. Commit and push to `main`.
3. When the Actions run finishes, download the `count-and-play-aab` artifact.
4. Play Console → Testing (or Production) → Create new release → upload the AAB.

## Project layout

| Path | What it is |
|---|---|
| `app/` | The Android app (Kotlin + Compose) |
| `.github/workflows/build.yml` | CI: builds the signed AAB/APK on every push |
| `play-store/` | Play Store listing assets + submission guide |
