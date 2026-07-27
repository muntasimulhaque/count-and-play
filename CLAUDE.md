# Count & Play — working rules

An Android app that helps a 3-year-old *see* what addition and subtraction are.
Built by a father for his son; published on Google Play so other families can
use it too.

## Two hard constraints (non-negotiable)

1. **No music.** No melodies, tunes, rhythmic beats or instrumental loops. The
   six effects in `res/raw` are deliberately inharmonic; `chime` is the only
   pitched sound and `SoundBoard` refuses to play it twice inside 1200 ms,
   because two pitched notes in sequence make an interval and intervals are
   where melody starts.
2. **No depiction of animate beings.** No humans, animals, faces, mascots,
   characters, or eyes-on-objects — in the app, the launcher icon, or store art.
   `ShapeKind` is the structural guarantee: there is no code path that renders a
   countable except through those ten shapes.

Warmth comes from material, light, weight and voice instead. That is a design
strength, not a workaround — it rules out the lazy affective devices and forces
the app toward what actually works at three.

## Shipping constraints

- `applicationId` is permanently `app.maqsadah.count_and_play.twa`. The `.twa`
  is a scar from the original Trusted Web Activity; Play ties an app to its
  first package ID forever. The *code namespace* is clean.
- `versionCode` only ever increases. `targetSdk` must stay at 36.
- **Zero manifest permissions.** This underpins the Data-safety declaration and
  the Families listing. Do not add one without a very good reason.
- The signing keystore lives outside the repo and in the `KEYSTORE_BASE64`
  GitHub secret. If it is lost the app can never be updated again.

## Architecture

```
core/     pure Kotlin, zero Android imports — the rules
copy/     what the words are, per language
host/     ViewModel, script runner, all timing
speech/   TTS
sound/    SoundPool
data/     SharedPreferences + the one-shot v3.5 migration
ui/       Compose
```

The organising principle: **the rules are pure data and functions; Android is a
player of those rules, not a participant.** `core/` and `copy/` are runnable in
a plain JVM test, which is why the whole game — six activities, the nudge
ladder, the pour-back, zero — is provable without an emulator.

## Forbidden

1. `core/` and `copy/` import nothing from `android.*`.
2. `delay()`, `sleep`, and wall-clock reads live **only** in `ScriptRunner` and
   `Timing`. The domain emits a `Script` of beats; the host performs it.
3. No composable takes a ViewModel. They take `(state, (Event) -> Unit)`.
4. No `mutableStateOf` inside a domain type.
5. No user-facing string outside `copy/`.
6. No fire-and-forget speech. Every utterance is a `Beat.Say` inside a script.
7. Cancel with `cancelAndJoin`, never a bare `cancel()` — a bare cancel returns
   before cleanup runs, and that window is exactly where two voices overlap.
8. **No `busy` flag that swallows input.** A tap always produces an outcome.
   Impatience may fast-forward; it is never ignored. To a 3-year-old an
   unresponsive screen is not "wait", it is broken.
9. No countable is ever an emoji or a bitmap. `ShapeKind` only.
10. No fail state. Struggling *eases* the ladder. The word "wrong" is not spoken
    anywhere, and `CopyTest` enforces that.
11. Arithmetic stays at totals ≤ `MAX_TOTAL` (5); counting practice ≤
    `MAX_COUNT` (10). These are not stylistic — a 3-year-old's subitizing limit
    is 3 and object-tracking limit is 1–2.
12. ≤ 400 lines per file, ≤ 40 per function. The point is that no file becomes
    a god object: the largest today is `GameHost` at 354 lines (v3.5's
    `GameViewModel` was 851 and held the rules, the timing, the speech and the
    persistence at once), and the median is under 150. Five files sit between
    250 and 360 — `GameHost`, `LessonFlow`, `ShapeArt`, `Tray`, `GameScreen` —
    and each is one coherent job. If any of them passes 400, split it.

## Pedagogy this rests on

- Most 3-year-olds are **subset-knowers** (Wynn): they can chant "one two three"
  and have no idea "three" answers *how many*. Answering "how many?" by
  repeating the last count word is a verbal habit that *mimics* cardinality.
- So **Give-N** — "put three in the bowl" — is the keystone activity. Where the
  child stops is his knower-level, read directly. It is also the best-evidenced
  route out of subset-knowing.
- **Re-taps are permitted and recorded, never blocked.** Where a child stops is
  the single most diagnostic behaviour in early number; blocking it hides it.
- Addition enters **nonverbally** first (under the leaf), because 3-year-olds
  succeed there while failing the identical problem posed in words.
- The **prediction beat** before every join is what turns counting from labour
  into verification.
- The **pour-back** is what turns addition from an event into a relation. Cut it
  and you have a good activity; keep it and you have the app's central idea.
- Praise the mathematics, not the child: *"Five! Three and two make five."*

## Build

```
./gradlew :app:testReleaseUnitTest      # the rules
./gradlew :app:assembleRelease          # R8 release
python tools/make_sounds.py             # regenerate sound assets (deterministic)
```

An Android SDK and the Android Studio JBR are installed on the owner's machine,
so builds and the emulator run locally — CI is a check, not the only path.

## Releasing

Bump `versionCode` +1 and `versionName` +0.1, push to `main`, let CI build the
signed AAB, pull it from the `builds` branch, upload to Play Console → Closed
testing → alpha. Only capture new store screenshots when the UI actually
changed.
