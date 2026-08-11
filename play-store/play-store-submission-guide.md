# Count & Play — Play Console Reference (v6 era)

Paste-ready material for the Play Console listing and release flow, kept in
step with the rebuilt app. The original v1 submission pack (PWABuilder zip,
Netlify TWA) is history — everything below describes the native app.

## Irreplaceable

`signing.keystore` + `signing-key-info.txt` (vault folder outside the repo,
base64 copy in the `KEYSTORE_BASE64` GitHub secret). If they are lost the app
can never be updated again. Keep copies in two places.

## Store listing (v6 — verify against what is actually live before pasting)

**App name** (30 chars max): `Count & Play`

**Short description** (80 chars max):
`See addition and subtraction happen. Three tap-to-play games, ages 3-5.`

**Full description**:
```
Count & Play helps a very young child SEE what numbers are — and what adding
and taking away do to them. Three big picture games, chosen with one tap:

• Count them — tap each object once, hear its number, and a numbered chip
  stays on it. One tap, one object, one number.
• Put together — two plates pour into one bowl as your child taps, while the
  voice counts ON across the join (1, 2, 3... then 4, 5). The parts keep their
  colours inside the whole, so five reads as three-and-two at a glance.
• Take away — "take away two": tap two out, the voice counts what is left.

Every round ends with the fact huge on screen — 3 + 2 = 5 — as the voice says
it. Difficulty creeps up invisibly and eases when your child struggles. There
is no score, no timer, and no fail state.

Made for little learners:
• Everything is spoken aloud, in English and বাংলা — no reading needed
• Huge touch targets for small fingers
• No music, no characters — just shapes, colour, motion and voice
• No ads, no purchases, no data collection, zero permissions — ever
• Fully offline

Made by a parent, for parents who want to sit with their child and teach.
```

**App category**: Education
**Contact email**: muntasim.haque@gmail.com
**Privacy policy URL**: `https://muntasimulhaque.github.io/count-and-play/privacy.html`
(hosted on this repo's GitHub Pages, `docs/privacy.html`; the declaration it
makes — no data collected — is exactly true of the native app)

> ⚠️ Checked 2026-08-03: the live listing may still carry the v1-era text
> (LEARN/QUIZ modes, stars, numbers to 20), which describes an app that no
> longer exists. Update the full description and the screenshots the next time
> you touch the listing — a Families-reviewed app should match what ships.

## Screenshots

All three sets are committed at `play-store/screenshots/`, captured by CI's
`ScreenshotTest` (last refreshed for v6.1):

- phone — `phone_01_home.png` … `phone_08_bangla.png`
- 7" tablet — `tablet7_01_home.png` … `tablet7_08_bangla.png`
- 10" tablet — `tablet10_01_home.png` … `tablet10_08_bangla.png`

Refresh the Play listing from these whenever the UI changes.

## Console questionnaires — answers

**App content → Ads**: No, the app does not contain ads.

**App content → App access**: All functionality is available without special
access (no login).

**App content → Content rating (IARC)**: category "Utility, Productivity,
Communication, or Other". No to all violence/sexuality/language/controlled-
substance questions, No to user interaction, No to sharing location, No to
purchases. Expected rating: Everyone / PEGI 3.

**App content → Target audience**: **5 and under** bands. This puts the app in
the Families program — correct and intended. It is *designed for* children.

**App content → Data safety**: the app collects and shares **no user data**.
Nothing transmitted, nothing stored beyond local preferences (language, mute,
progress), no SDKs, no network calls.

**App content → Families Policy / child safety**: self-certify compliance.
Contact: muntasim.haque@gmail.com. No user-generated content, no chat, no
external links reachable by a child.

**Government apps / Financial features / Health**: No to all.

## Release path (current state, 2026-08)

- Closed testing (14 days, 12 testers) completed 2026-07-28; **production
  access granted 2026-08-03**.
- Alpha keeps running for testers; Production is the public track. Managed
  publishing is off → an approved release publishes itself.
- Every release: bump versions → push `main` → CI signs → pull the AAB from the
  `builds` branch → Play Console → create release on alpha and/or Production →
  notes → submit for review.
- The app is fully native and fully offline: every change, however small, needs
  a new build and a Play release. Nothing ships without it.
