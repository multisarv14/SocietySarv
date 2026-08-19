# SocietySarv 1.1 (versionCode 2)

Upload `societysarv-v1.1-vc2-release.aab` to Play Console.

| | |
|---|---|
| Package | `com.multisarv.societysarv` |
| versionCode / versionName | `2` / `1.1` |
| minSdk / targetSdk | 24 / 36 |
| Signed with | `societysarv-release-key.jks`, alias `societysarv` |
| Cert SHA-1 | `5F:2F:BC:AB:B0:77:DD:ED:B6:8B:70:00:5D:41:46:55:EE:74:CC:37` |
| Store listing icon | `play-store-icon-512.png` (512x512, no alpha) |

## What changed since 1.0

**New app icon.** The SocietySarv logo replaces the template Android icon. The
circle is cut from the artwork itself (centre and radius measured from the
image, not guessed) and placed exactly in the adaptive-icon safe zone, so a
circular launcher mask lands on the logo's own edge - round edge to edge, no
white box. Background `#027C75` is sampled from the logo's ring, so squircle
and square launchers show brand teal in the corners instead of white. A
`monochrome` layer is included for Android 13+ themed icons.

**No mock or demo data left in the app.** The Room database, its seeded
society, the prefilled login form and the "Quick Demo Logins" buttons are all
gone. Every record now comes from Cloud Firestore.

**Login fixed.** The old build resolved the login identifier by looking at its
first character and only accepted `A`/`B`/`C`/`G`, which silently locked out
every other wing and every mobile-number login. It now looks up the flat number
first and falls back to the registered mobile.

**Registration reworked.** The wizard sets the chairman's own login PIN
(entered twice) instead of a hardcoded `4141`, validates each step before
letting the user continue, and refuses a second registration while a society
already exists. Adding a member also takes a PIN, so new members can actually
sign in.

## Before publishing

Firestore rules are still open because Anonymous Auth is not enabled yet.
Enable it in Firebase Console -> Authentication -> Sign-in method -> Anonymous,
then deploy `firestore.rules.secure`. See README-FIREBASE.md.

## Play Console demo login

Username `A-101`, password `1234`, society code `DEMO1`.
Full text in PLAY-STORE-DEMO-LOGIN.md.
