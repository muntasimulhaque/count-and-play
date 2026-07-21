# Count & Play

A native Android app that teaches a young child addition and subtraction visually. Numbers up to 20, everything narrated aloud, nothing ever vanishes — taken-away items move to a visible "went away" area and stay countable.

- **Play Store package:** `app.maqsadah.count_and_play.twa` (closed testing)
- **License:** MIT

## The app

Three ways to play, all built around **tap-to-count**: the app never counts for the child — the child taps each object, hears its number spoken, and a little badge stays on every counted object (one-to-one correspondence). Objects are laid out in **rows of five** (ten-frame style), so 7 looks like "5 and 2 more".

**▶ Play (guided levels)** — the app opens here. Four levels by number range (within 3, 5, 10, then 20); addition and subtraction alternate automatically. Addition: count one group, count the other, tap the 🧺 basket to put them together, then count them all. Subtraction: count the group, tap objects to take them away (each becomes a faded "ghost hole" that keeps its spot), then count what's left. Five stars level up with confetti and a celebration; progress is saved.

**🧺 Free play** — the classic picker: you or your child pick the numbers (up to 20) and the operation, and the same tap-to-count acting plays out.

**⭐ Quiz** — every problem is acted out visually first, then the child picks from three big number buttons — or tap-counts the objects first. Wrong answers never fail; the app recounts everything slowly with each object highlighted. Quiz difficulty follows the current level.

**Grown-ups settings** (long-press the sound button): pick any of the device's installed English voices, switch the speech rate between Slow and Normal, or reset progress. If the child goes quiet for a few seconds, the app gently pulses the objects and repeats the hint.

Design constraints honored throughout: no human or animal figures (fruits, stars, balloons, balls, cars only), no music ever — voice narration only (Android's built-in text-to-speech), huge touch targets, no ads, no data collection, no network access at all, no fail states.

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
