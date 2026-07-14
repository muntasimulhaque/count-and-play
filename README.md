# Count & Play — Project Handover

An app that teaches a young child addition and subtraction visually. Built July 12–13, 2026.

Live app: **https://count-and-play.netlify.app**
Privacy policy: https://count-and-play.netlify.app/privacy.html

## What we built and why

The goal was to make addition and subtraction *visible* for a three-year-old, not just quizzed. The core design insight: subtraction is hard for toddlers because taken-away objects vanish — so in this app, nothing vanishes. Removed items move to a visible "went away" area and stay countable.

The app has two modes:

**Learn mode** — you or your child pick the numbers (up to 20) and the operation, and the app acts it out. Addition shows two differently-colored fruit groups counted aloud, then merged into one basket with "counting on" (five... six, seven, eight) — both groups stay visible inside the total. Subtraction shows the full group counted, then the child taps exactly the right number of fruits into empty take-away slots; the taken group fades but stays visible, and the remainder is counted.

**Quiz mode** — every problem is acted out visually first, then the child picks from three big number buttons. Wrong answers never fail; the app recounts everything slowly with each fruit highlighted.

Design constraints honored throughout: no human or animal figures (fruits, stars, balloons, balls, cars only), English voice narration for every action, huge touch targets, no ads, no data collection, no fail states.

## Why a PWA wrapped as an Android app (not native Kotlin)

The app needs only touch, animation, and text-to-speech — all fully served by web technology, so native code would add cost with no visible benefit. The web app was upgraded to a PWA (manifest, icons, offline service worker) and packaged with PWABuilder into a Trusted Web Activity — Google's own recommended packaging for web apps. It installs from Play like any other app and runs full-screen with no browser bar (digital asset links are live and verified). The big ongoing advantage: content updates deploy to Netlify instantly and reach every installed copy without a Play Store release.

## What's in this folder

| Path | What it is |
|---|---|
| `Count & Play - Google Play package.zip` | PWABuilder output: `Count and Play.aab` (upload this to Play Console), test APK, and **the signing key** (see warning below) |
| `site/` | Complete deployable website source — this IS the app |
| `site/netlify-deploy.zip` | Ready-to-upload deploy bundle (drag onto Netlify Deploys page) |
| `play-store/play-store-submission-guide.md` | Full submission guide: listing text, questionnaire answers, step-by-step console walkthrough |
| `play-store/play-icon-512.png` | Play Store icon (512×512) |
| `play-store/feature-graphic-1024x500.png` | Play Store feature graphic |

## ⚠️ The one thing you must not lose

Inside the Google Play package zip: `signing.keystore` and `signing-key-info.txt` (its passwords). Google requires this exact key for **every future update** of the app. If it's lost, the app can never be updated. Keep copies in at least two places (password manager + another drive). Also treat it as secret — anyone holding it can impersonate your app.

## Current status

- [x] App built, tested, and live at count-and-play.netlify.app
- [x] PWA complete (manifest, icons, offline service worker v4)
- [x] Signed Android package (.aab) generated — package ID `app.netlify.count_and_play.twa`
- [x] Digital asset links live and verified (full-screen app, no browser bar)
- [x] Privacy policy live
- [x] Store listing text, data-safety and Families-policy answers drafted
- [x] Play Console signup started (About you / contact pages)
- [ ] Finish Play Console registration: $25 fee + identity verification (1–2 days)
- [ ] Take 2+ phone screenshots of the app (Learn picker, addition scene, subtraction slots)
- [ ] Create app in console + complete App content sections (all answers are in the guide)
- [ ] Upload .aab to a closed testing track
- [ ] Recruit 12+ testers (Google account emails), share the opt-in link, all must stay opted in **14 consecutive days**
- [ ] During the window: gather feedback, push at least one small update (looks good to Google's reviewers)
- [ ] After 14 days: apply for production access, then create the production release

## How to update the app later

Content/behavior changes: edit `site/index.html`, re-zip the `site/` files (or reuse the structure of `netlify-deploy.zip`), drag onto the Netlify Deploys page for the count-and-play project. Bump the cache version string `count-play-v4` in `sw.js` on every deploy so installed copies refresh cleanly. No Play Store release needed.

New .aab needed only if the icon, app name, package ID, or start URL changes — regenerate at pwabuilder.com against the live URL, **reusing the existing signing key** (upload `signing.keystore` in PWABuilder's signing options instead of generating a new one).

## Known quirks and lessons learned

- Netlify's drag-and-drop deploys silently strip `.well-known` folders. The fix in place: the file lives at `well-known/` (no dot) and `_redirects` rewrites `/.well-known/*` to it. Don't remove either piece.
- The service worker must never cache non-OK responses (a cached 404 haunted us for an hour). The current `sw.js` guards against this — keep that guard if you edit it.
- The voice uses each device's built-in text-to-speech, so quality varies by device. If it sounds too robotic on your son's tablet, pre-recorded audio is a possible future upgrade.
- Possible future features discussed: Bangla counting voice (a toggle), more object types, and a multiplication mode for when he's older.
