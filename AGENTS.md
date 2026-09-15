# Count & Play

A native Android app that helps a 3-year-old *see* what addition and
subtraction are. Built by a father for his son; on Google Play so other
families can use it too. In English and বাংলা.

This file holds only what the code cannot say: the two product constraints, the
decisions behind them, the vocabulary, the workflow around the repo, and style
rules that no test can check. Everything else is in the code, which is the
source of truth. If prose here and code disagree, the code is right and this
file is stale: fix it.

These rules are the current best understanding, not the ceiling. If a change
you believe in contradicts one, do not silently drop the idea and do not
implement against the rule either: name the conflict, make the case, and let
the owner decide. An overridden rule is updated here, never left as a dead
letter.

## Two hard constraints (non-negotiable)

1. **No music.** The synthesized effects in `res/raw` are deliberately
   inharmonic; `chime` is the only pitched sound and `SoundBoard` refuses to
   play it twice inside 1200 ms, because two pitched notes in sequence make an
   interval and intervals are where melody starts.
2. **No depiction of animate beings.** No humans, animals, faces, mascots,
   characters, or eyes-on-objects, in the app, the launcher icon, or store art.
   `ShapeKind` is the structural guarantee: there is no code path that renders
   a countable except through those ten shapes.

Warmth comes from material, light, weight and voice instead.

## Map

```
app/src/main/java/.../
  core/   pure Kotlin, zero Android imports: the rules, and where most answers live
  copy/   what the words are, in English and বাংলা
  host/   ViewModel, beat runner, TTS, SoundPool, prefs
  ui/     Compose, one file per idea, named for the idea
tools/    offline asset generators (sounds, store art): plain JVM Kotlin
.github/  build.yml (tests, lint, signed AAB), screenshots.yml (store captures)
play-store/  listing kit: screenshots per form factor, art, listing text, guide
docs/     privacy.html as served on GitHub Pages, bundled font license
```

Read the code first: `core/Flow.kt` (the sitting, the three games),
`core/Model.kt` (what a tap does), `core/Adapt.kt` (invisible difficulty),
`core/Beat.kt` (what the host may be asked to do), `host/GameHost.kt` (how
beats become sound, speech and time). Files carry their reasons in comments.

The organizing principle: **the rules are pure data and functions; Android is
a player of those rules, not a participant.** The domain emits beats (say,
play, show, wait) and the host performs them, which is why the whole game is
playable in plain JVM tests and store screenshots render straight from state.

## Style

**No em-dashes (—), ever, unless absolutely necessary.** Not in chat replies,
release notes, commit messages, code comments, or this file. Use commas,
colons, parentheses, or a sentence break instead. The en-dash stays for number
ranges ("1–2"); the ellipsis is not an em-dash and is fine.

**American English, everywhere.** Spelling, words and idiom: color, license,
center, gray, practice, traveled, honor, organize. Store text included.

**Plain-text store text.** Release notes are pasted into Play Console, where
quotes, markdown fences, and dashes mangle or get auto-corrected: plain prose,
no quote marks around phrases, no markdown, no em-dashes.

**Small pieces.** ≤ 400 lines per file, ≤ 40 per function. If anything passes,
split it. Grep before adding a file to `ui/`; a name that says the idea beats a
name that says the screen.

## Shipping constraints

- `applicationId` is permanently `app.maqsadah.count_and_play.twa`. The `.twa`
  is a scar from the original Trusted Web Activity; Play ties an app to its
  first package ID forever. The *code namespace* is clean.
- `versionCode` only ever increases. `targetSdk` moves only together with an
  AGP that supports it.
- **Zero manifest permissions.** This underpins the Data-safety declaration
  and the Families listing. Do not add one without a very good reason.
- The signing keystore lives outside the repo and in the `KEYSTORE_BASE64`
  GitHub secret. If it is lost the app can never be updated again.

## Architecture rules, and the test that holds each one

| Rule | Held by |
|---|---|
| `core/` and `copy/` import nothing from `android.*` | review; keep it that way |
| Timing (`delay`, sleep, wall-clock) lives only in `host/`; the domain emits beats and never waits | review: every `delay()` sits in `GameHost.kt` |
| No composable takes a ViewModel; they take `UiModel` and callbacks | review |
| No `mutableStateOf` inside a domain type | review; domains are `data class`es |
| No user-facing string outside `copy/` | `CopyTest`, plus review of new lines |
| All speech goes through `Narrator` as host-performed `Beat.Say*` beats, epoch-guarded and gated on foreground/mute, so nothing speaks unseen and a stale callback stays dead | `Narrator` itself |
| Drawn objects only: no countable is an emoji or a bitmap | `ShapeKind` is the only type that reaches `ui/Countable.kt` and `ui/ShapeArt.kt` |
| No fail state; struggling *eases* the ladder and "wrong" is never spoken | `AdaptTest`, `CopyTest.noLineEverScoldsOrSpeaksFailure` |

Two more that only bite if a round is misread: **no `busy` flag that swallows
input** (during play a tap always produces an outcome; celebration-dwell taps
are gently ignored so drumming cannot score a finished round as struggle), and
**the three games are always visible** (difficulty adapts *inside* a game,
never to the home screen; nothing the child has been shown is taken away).

## The three games, and the words for them

Each game is a huge picture button on the home screen; from there the app
guides, and difficulty adapts invisibly inside the game. What each screen does
is in `ui/CountScreen.kt`, `ui/AddScreen.kt`, `ui/TakeScreen.kt`; what each tap
*means* is in `core/Model.kt`. This section is only the vocabulary:

| Word | What it means here |
|---|---|
| **order** | his tap order, never a tray order: whatever he taps first is "one" |
| **the whole** | the total he produces himself, in the bowl or on the tray |
| **the parts** | the plates that went into the whole, each keeping its color |
| **asleep** | shown washed-out gray and not yet countable (the un-counted plate) |
| **left** | a quantity he can still see, not a disappearance |
| **the fact** | the numerals arriving huge at the end of a round, `3 + 2 = 5` |
| **round** | one deal of one game, ending in the fact and confetti |
| **ladder** | `Adapt`: two clean rounds step up, struggle steps down, invisibly |

Every round ends the same way: the fact arrives huge on screen while the voice
says it, then confetti. There is no menu beyond the three pictures, no lock, no
session end, no score, and no fail state.

## Decisions (why this, not the alternatives)

- **There is no correct counting order.** The chip follows his finger, not the
  tray. A tray order would make counting a test of reading someone else's mind.
- **The parts are counted, then the whole**, by him: plates on their own, the
  pour he performs himself, the total he produces rather than was told.
- **The parts stay inside the whole.** The bowl seats each part on its own
  color, so five keeps reading as three-and-two at a glance.
- **Subtraction stays visible.** Taken objects wear their take-away number in
  ghost holes; "left" is a quantity you can see, not a disappearing act.
- **Symbols arrive at the moment of the fact**, never earlier, never as chrome.
  Praise the mathematics, not the child. One deliberate exception: a finished
  COUNT speaks a plain Well done / সাব্বাশ, because his son asked where the
  encouragement went.
- **Re-taps are recorded, never punished.** Hesitation is diagnostic: the
  ladder eases, it never scolds.
- **Numbers stay small on purpose.** Arithmetic totals ≤ 5 at first and ≤ 10 at
  the top level; counting practice ≤ 10, ADD plates ≤ 5; subitizing at three
  tops out near 3. Twenty objects is not harder counting, it is a different and
  much worse task. Plates stay five-frame-shaped and tappable.
- **The counting objects are vector paths, never emoji or bitmaps.** Identical
  on every device, and unable to drift into a creature. `ShapeKind` only.

## Build

```
./gradlew :app:testReleaseUnitTest      # the rules
./gradlew :app:assembleRelease          # R8 release
./gradlew :tools:makeSounds             # regenerate sound assets (byte-identical)
./gradlew :tools:checkSounds            # verify committed sounds match regeneration
./gradlew :tools:makeArt                # regenerate store art + launcher icons
```

An Android SDK and the Android Studio JBR are installed on the owner's machine
(`JAVA_HOME` must point at the JBR; it is not on PATH).

## Pull before working

The owner works from more than one machine; this checkout is only one of
several. Reading this file is the first act of every session, and the second is
immediate and unconditional: right after reading AGENTS.md, before doing
anything else and without waiting to be asked, run `git fetch` and pull
whatever is new on `main` from GitHub, then do all work on that pulled head,
never on a stale local one.

## Commits

**Never add a `Co-Authored-By: Claude` trailer, or any other AI attribution, to
a commit message.** GitHub credits co-author trailers, which would put the tool
in this repository's contributor list. One person writes this app and the
history should say so.

## Verifying UI: CI is the loop

UI changes are verified from CI screenshot artifacts, never by reading code.
Any push touching UI runs `screenshots.yml` (API 35, three form factors, eight
scenes each, about five minutes); manual `workflow_dispatch` works too.

**Captures are English only, and stay eight per form factor (24 in all).** The
listing is reviewed in English, so no scene is shot in Bengali any more; the
app itself still plays in both languages and the listing text says so. Eight is
the Play Console maximum, so the retired Bengali scene was replaced rather than
dropped: slot 08 is the finished count, four shapes from the bag wearing the
numbers of his own taps, the total arriving huge over the tray. That is the
moment COUNT shares with the other two games, and it closes the shelf on a
number he produced. Do not add a non-English scene back without the owner
saying so.

Download the three `store-screenshots-*` artifacts (`gh run download <run-id>
-R muntasimulhaque/count-and-play -D <dir>`; `-R` works from any directory and
either machine), strip the form-factor prefix into `play-store/screenshots/`
(`phone/`, `tablet7/`, `tablet10/`), and delete scenes the test no longer
shoots. Git status is the drift check: a no-change refresh comes back
byte-identical, a real change moves exactly the scenes that changed. Byte noise
is not drift: phone-only pixels off by one step with zero pixels at 2 percent
fuzz means emulator rasterization noise, safe to note in the message and move
on; real pixels on all form factors mean a real change, find it first.

Do not pre-check UI on a local emulator; CI is the loop. Local API 35 AVDs
(`Pixel_4_35`, `Nexus_7_35`, `Pixel_C_35`, the same image CI uses) exist only
for when CI itself cannot answer, and if a local capture fails twice, stop and
let CI do it. Never attempt API 37 capture until Android 17 images boot
reliably under WHPX and on hosted runners; they currently crash the graphics
stack on first render. Lessons that still bite per AVD: set
`disk.dataPartition.size=6G` in config.ini (not `hw.diskSize`) and recreate the
userdata image; free host RAM first (`./gradlew --stop`, kill stray
`qemu-system-x86_64-headless.exe` processes); export `MSYS_NO_PATHCONV=1`
before adb commands carrying `/sdcard/...`; install with `adb install` plus
`am instrument`, since the Gradle split-APK path trips the flaky package
service.

## Releasing

Bump `versionCode` +1 and `versionName` (+0.1 for small releases), push to
`main`, and CI does the rest:

- `build.yml` runs tests, lint and `:tools:checkSounds`, then builds the
  signed AAB and publishes it to the `latest-build` GitHub release. Pull the
  AAB from that release.
- `screenshots.yml` recaptures store screenshots whenever UI files change
  (three form factors, eight scenes each; see above).

Deliver the upload kit in one place: the AAB in `play-store/aab/`, release
notes in chat as bare plain text (English, plain prose, no code fences, no
markdown, no formatting of any kind, so the owner selects the words and pastes
them straight into Play Console), and the notes must fit the Play Console
release-notes field: 500 characters, counted before handing them over, never
assumed. Upload screenshots straight from `play-store/screenshots/`, one
subfolder per form factor (`phone/`, `tablet7/`, `tablet10/`). Listing text,
feature graphic and icon live in `play-store/`; the graphic and icon are
`makeArt` outputs, so whenever either is regenerated, upload the new PNGs to
the listing as well. The owner uploads to Play Console and pastes the notes.
