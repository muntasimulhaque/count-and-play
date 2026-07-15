# Count & Play — Google Play Submission Pack

Everything below is ready to paste into Play Console. Work through it top to bottom once your developer account is verified.

## What you already have

| Item | Where |
|---|---|
| Signed app bundle (.aab) | `Count and Play.aab` — in the PWABuilder zip you downloaded |
| Test APK (sideload to verify) | `Count and Play.apk` — same zip |
| Signing key — BACK THIS UP | `signing.keystore` + `signing-key-info.txt` — same zip |
| App icon 512×512 | `play-icon-512.png` |
| Feature graphic 1024×500 | `feature-graphic-1024x500.png` |
| Privacy policy (live) | https://count-and-play.netlify.app/privacy.html |
| Digital asset links (live, verified) | https://count-and-play.netlify.app/.well-known/assetlinks.json |
| Package ID | `app.maqsadah.count_and_play.twa` |

Still needed: at least 2 phone screenshots (and 1 tablet screenshot recommended). Open the app on your tablet/phone and screenshot the Learn picker, an addition scene mid-count, and a subtraction scene with the take-away slots.

## Store listing (paste as-is)

**App name** (30 chars max):
`Count & Play: Learn Math`

**Short description** (80 chars max):
`Watch addition and subtraction happen. Count fruits with your little one.`

**Full description**:
```
Count & Play helps young children (ages 2–5) truly SEE what addition and subtraction mean — not just memorize answers.

LEARN MODE — you or your child pick the numbers (up to 20), and the app acts the problem out:

• Addition: two groups of different fruits appear and are counted aloud. Tap "Put together!" and they join into one basket — then the app counts on ("five... six, seven, eight!"), the way children naturally learn to add. Both groups stay visible inside the total, so 5 + 3 is always visible inside the 8.

• Subtraction: the fruits are counted, then your child taps exactly the right number of fruits to "take away" into empty slots. The taken group stays visible — faded, countable — so 9 − 3 becomes a real quantity, not a disappearing act.

QUIZ MODE — every problem is acted out visually first, then your child picks from three big number buttons. A wrong answer never fails: the app gently counts everything again, fruit by fruit, until the answer is clear. Stars and confetti celebrate every success.

DESIGNED FOR LITTLE LEARNERS
• Everything is narrated aloud — no reading required
• Huge touch targets for small fingers
• A live equation (5 − 2 = 3) builds as your child plays
• No ads, no in-app purchases, no data collection — ever
• Works offline after the first load
• No animal or human characters — just fruits, stars, balloons, balls, and cars

Made by a parent, for parents who want to sit with their child and teach — not hand over a screen and walk away.
```

**App category**: Education
**Tags**: Education, Early learning
**Contact email**: muntasim.haque@gmail.com
**Privacy policy URL**: `https://count-and-play.netlify.app/privacy.html`

## Console questionnaires — answers

**App content → Privacy policy**: paste the URL above.

**App content → Ads**: No, the app does not contain ads.

**App content → App access**: All functionality is available without special access (no login).

**App content → Content rating (IARC questionnaire)**: category "Utility, Productivity, Communication, or Other". Answer No to all violence/sexuality/language/controlled-substance questions, No to user interaction, No to sharing location, No to purchases. Expected rating: Everyone / PEGI 3.

**App content → Target audience**: select age groups **5 and under** (both "Ages 0–5" bands if shown). This puts the app in the Families program — that's correct and intended. When asked if the app could unintentionally appeal to children: it is *designed for* children.

**App content → Data safety**: declare that the app does **not collect or share any user data**. No data collected, no data shared, no encryption needed (nothing transmitted), no deletion mechanism needed (nothing stored). This matches the privacy policy exactly — the app has no analytics, no SDKs, no network calls except loading its own static files.

**App content → Families Policy / child safety**: self-certify compliance. Child safety point of contact: muntasim.haque@gmail.com. The app has no user-generated content, no chat, no external links a child can reach (the privacy policy page is not linked from inside the app UI).

**App content → Government apps / Financial features / Health**: No to all.

## Release path (new personal account rules)

1. **Play Console → Create app** → name "Count & Play: Learn Math", App (not game — or Game > Educational, either is accepted; App/Education is cleaner), Free.
2. Complete ALL "App content" sections above first — the console blocks releases until they're done.
3. **Test and release → Testing → Closed testing** → create track "family-testers" → upload `Count and Play.aab` → add release notes ("First release").
4. Create an email list with your 12+ testers' Google account emails. Share the opt-in link with them; each must click it and install the app.
5. Testers must stay opted in for **14 consecutive days**. During that window, open the app, note feedback, and ideally push at least one small update (I can bump the version and produce a new .aab whenever you want — takes minutes).
6. After 14 days, the console Dashboard shows **Apply for production**. Answer honestly: testing process, feedback received, changes made.
7. Approval typically takes a few days. Then **Production → create release** with the same .aab, and the app goes public after review (Families review can add extra days).

## Version updates later

The app content lives on the website — most changes (new fruits, new modes, fixes) deploy to Netlify instantly and appear in the installed app with no Play Store update at all. You only need a new .aab when the icon, name, or package settings change. That's the quiet superpower of this architecture.

## One warning worth repeating

`signing.keystore` and the passwords in `signing-key-info.txt` are irreplaceable. If they're lost, you can never update the app on Google Play. Keep copies in two places (e.g., password manager + cloud drive).
