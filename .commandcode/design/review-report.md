# Design review: Count & Play

Mode: review (bare /design routing: audit, then apply the critical fixes in the same pass)
Scope: full UI surface, phone/tablet, EN + BN
Audience: 3 to 5 year old children, with grown-ups for setup
Date: 2026-08-31

## First impression

A candy toy-box hung in a calm gallery. Huge white keys on warm paper, saturated objects, one display face at child scale, and a material language (keys float, wells hold, cards interrupt) that is applied without exception. The first read says exactly what this is and who it is for. The most distinctive choice is that the arithmetic itself is the celebration, and it works.

## Scores

| # | Lens | Score | What would move it |
|---|---|---|---|
| 1 | First impression | 9/10 | Cold start flashes a different yellow before Compose paints (finding 2) |
| 2 | Hierarchy | 9/10 | Nothing needed; one dominant action per screen at every phase |
| 3 | Color voice | 8/10 | The launch window background token drifted from the Compose ground (finding 2) |
| 4 | Type voice | 9/10 | One family, strong scale; the adult-facing voice note is the smallest text in the app (finding 3) |
| 5 | Interaction feel | 7/10 | Press physics, haptics, gated sound and reduced motion are exemplary; the system back gesture ends the whole app mid-round (finding 1) |

**Total: 42/50**

## Findings

| # | Severity | Discipline | Location | Before | After | Why |
|---|---|---|---|---|---|---|
| 1 | HIGH | Interaction | `ui/GameScreen.kt` (Stage) with `host/GameHost.kt:115` | No `BackHandler` anywhere (grep: zero matches). The back gesture or back button from any game screen finishes the activity and loses the round | Back gesture resolves one level at a time: close the settings sheet first, else go home from a round, else (on the shelf) the system default exit | A toddler's palm or a stray back swipe ends the app mid-count; the on-screen house button was the only way back |
| 2 | MEDIUM | Color | `app/src/main/res/values/colors.xml:6` | `tabletop = #FDF2D2` (yellow) while the Compose `Ground` is `#F6F2EA` (warm paper) | Set the token to `#F6F2EA` so the window background matches the ground it hands off to | Every cold start paints a yellow frame that snaps to paper; the theme comment already promises the first frame is the app's world, and it wasn't |
| 3 | LOW | Type | `ui/Sheets.kt:120` | `voiceMissingNote` at 13sp | 14sp | The one adult-facing recovery message is the smallest text in the app, in the display face |

## Applied in this pass

- Finding 1: added `BackHandler` wiring in `Stage()` (settings first, then round to home, shelf keeps the system default). No new strings, no new permissions, no timing.
- Finding 2: `colors.xml` tabletop set to `#F6F2EA`, matching `Ground` in `ui/Theme.kt`.
- Finding 3: note size 13sp to 14sp.

## Considered but rejected

| Location | Candidate | Rejected because |
|---|---|---|
| `ui/Objects.kt` ghost slots (contrast ~1.55:1) | Raise ghost contrast | Deliberate and documented: a ghost must not read as an object or it gets counted; the quiet outline is the pedagogy |
| `ui/Sheets.kt` sound row dot | Flag the state as colour-only | State is already carried twice without hue: the label (Sound on/off) and the speaker icon shape (waves vs slash). The dot is decorative |
| `ui/AddBowl.kt` pour cue over bowl seats | Move the cue off the seats | Documented owner decision in AGENTS.md: the cue lives where the finger goes |
| `res/values-night` | Introduce a dark palette | Documented decision: the app is always light; the night theme intentionally mirrors day so launch never flashes dark |
| `ui/Sheets.kt` grab handle | Add swipe to dismiss | Drag physics on a parent-only surface with a 44dp close button buys nothing for the cost |

## Verification

Checks run:

- `grep BackHandler` across the repo before the change: zero matches (finding 1 confirmed).
- Contrast math on the flash variants on white: FlashBlue 5.3:1, FlashOrange 5.0:1, FlashPink 5.5:1; all clear the large-text floor with margin.
- Read all 8 committed store screenshots (phone) plus the full UI source; findings tied to both.
- `:app:testReleaseUnitTest` before and after the change: see the chat summary for the final verdict.

Not verified here, deliberately:

- Rendered pixel output after the change. Per AGENTS.md this comes from CI (`screenshots.yml`) on the next push. All three fixes are non-visual in the captured scenes (the window background is fully covered by the Compose ground, a BackHandler renders nothing, and scene 07 has the voice note absent), so the refreshed references are expected to come back byte-identical. Git status after the CI run is the drift check.

## Verdict

**Block** as found: one HIGH interaction failure stood between a child and their round. All three findings were remediated in this same pass, per the bare `/design` routing (report is the diagnostic, fixes are the treatment). Remaining step is the CI confirmation described above.
