# Count & Play

A native Android app that helps a very young child *see* what addition and
subtraction are. Built by a father for his 3-year-old son; on Google Play so
other families can use it too.

- **Play Store package:** `app.maqsadah.count_and_play.twa` (closed testing)
- **License:** MIT

## What it is

Everything is **tap-to-count**: the app never counts for the child. He taps each
object, hears its number, and a numbered chip stays on it — one tap, one object,
one number. Objects sit in a **five-frame**, so three apples read as "three, and
two empty".

There are no modes and no menu. A ladder decides what comes next and the child
just plays. Six things it teaches, in the order a child grows into them:

| | |
|---|---|
| **Counting** | Tap each object once; the last number names the whole set. |
| **Make a number** | *"Put three apples in the bowl."* The child **produces** a quantity — and where he stops is the single most informative thing the app can know. |
| **More and fewer** | Two trays; which has more? Including trials where fewer objects are spread over more space. |
| **Hidden adding** | Objects go under a leaf, one more slides in. *How many now?* Answered by building a set, so no numerals or words are needed. |
| **Putting together** | Two dishes pour into one bowl. The child predicts first, then counts — and the parts stay visibly inside the whole. |
| **Taking away** | The same picture, reversed: the bowl pours into a dish. What left is still there, whole and countable. |

Addition and subtraction use the **same furniture in opposite directions**, and
every join or separation is **poured back** at the end. That reversal is the
point: if the join can be undone, the whole genuinely *contains* the parts
rather than replacing them.

Numbers stay small on purpose. Arithmetic never exceeds **five**, counting
practice never exceeds ten. A 3-year-old can see three at a glance and track
one or two moving objects; twenty objects is not harder counting, it is a
different and much worse task.

**Nothing advances by itself.** There is no autoplay, no timer, no score, no
stars, and no fail state. A wrong answer is met with the plain fact — *"We made
five"* — and then an easier one. The app never says "wrong".

Design constraints honoured throughout: **no music**, and **no depiction of any
animate being** — no people, animals, faces, or mascots anywhere, including the
icon. Warmth comes from material, weight, motion and voice instead. No ads, no
data collection, no network access, zero permissions.

## Tech

Kotlin + Jetpack Compose. The counting objects are ten shapes **drawn as vector
paths in code**, not emoji, so they are identical on every device and can never
drift into depicting a creature. The six sound effects are synthesized by
`tools/make_sounds.py` and are deliberately inharmonic; only the success chime
has a pitch, and it can never play twice in quick succession.

```
core/     pure Kotlin, zero Android imports — the rules
copy/     what the words are, in English and বাংলা
host/     ViewModel, script runner, all timing
speech/   TTS      sound/  SoundPool      data/  prefs + migration
ui/       Compose
```

The organising principle: **the rules are pure data and functions; Android is a
player of those rules, not a participant.** The domain emits a script of beats —
say this, play that, show this, wait — and the host performs it, so `delay()`
exists in exactly one small file. That is why the entire game is playable in
plain JVM tests, and why the store screenshots are rendered from state directly
rather than by driving a live emulator.

See [CLAUDE.md](CLAUDE.md) for the working rules and the pedagogy this rests on.

## Building

```
./gradlew :app:testReleaseUnitTest      # the rules
./gradlew :app:assembleRelease          # R8 release (~1 MB)
python tools/make_sounds.py             # regenerate sound assets (deterministic)
```

Every push to `main` triggers GitHub Actions, which runs the unit tests and then
builds a **signed release AAB and APK**. Signing uses four repository secrets:
`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. The
keystore is never committed — if it is lost, the app can never be updated again.

A second workflow renders the Play Store screenshots on phone, 7" and 10"
emulators from `ScreenshotTest`.

## Releasing an update

1. Bump `versionCode` (+1) and `versionName` (+0.1) in `app/build.gradle.kts`.
2. Commit and push to `main`.
3. Take the signed AAB from the `builds` branch once CI is green.
4. Play Console → Testing → Closed testing → alpha → Create new release.

Only capture new store screenshots when the UI actually changed.
