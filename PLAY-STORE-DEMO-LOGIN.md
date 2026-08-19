# Play Console — App access (demo login for review)

Google Play reviewers cannot get past a login screen on their own, so this
information goes into **Play Console → App content → App access →
"All or some functionality is restricted"**.

SocietySarv signs a member in with **three** values — society code, flat number,
and a 4-digit PIN — but the Play Console form only has *Username* and
*Password*. So put the flat number in Username, the PIN in Password, and the
society code in the Instructions box (the form's Instructions field is exactly
what it is meant for).

---

## The credential to enter (main demo account)

| Play Console field | Value |
|---|---|
| **Username** | `A-101` |
| **Password** | `1234` |
| **Any other information** | paste the instructions block below |

Add the extra role logins as separate "Add new instructions" rows if you want
the reviewer to see them; one is enough to pass review.

---

## Instructions block — copy this verbatim

```
SocietySarv is a housing-society management app. Signing in needs three values:
a society code, a flat number and a 4-digit PIN.

On the login screen enter:

  Society Code                     : DEMO1
  Flat / Unit No. / Mobile Number  : A-101      (this is the Username above)
  4-Digit PIN                      : 1234       (this is the Password above)

Then tap "Secure Login". No OTP, email verification, or payment is required.

This demo society is pre-populated with sample members, maintenance bills,
expenses, notices and gate-visitor entries so that every screen has content.

The app has four roles. Each one lands on a different screen, and all four can
be reviewed with the same society code (DEMO1):

  Chairman     - flat A-101   - PIN 1234  - full admin dashboard, accounts, billing
  Secretary    - flat A-102   - PIN 2345  - same committee access as the chairman
  Resident     - flat B-201   - PIN 5678  - "My Flat" portal: own bills and complaints
  Gate Guard   - flat GATE-01 - PIN 9012  - gate security terminal only

To switch roles, tap the logout icon in the top-right corner and sign in again
with another flat number and PIN.

A member can also sign in with their registered mobile number instead of the
flat number, for example 9000000001 with PIN 1234 for the chairman.

Language can be changed at any time from the "ENG" button in the top bar
(English / Hindi / Marathi).

An internet connection is required: all data is stored in Cloud Firestore.
```

---

## All demo accounts

Society code for every account: **`DEMO1`**

| Role | Flat / Username | Mobile (also works) | PIN / Password | Lands on |
|---|---|---|---|---|
| Chairman | `A-101` | `9000000001` | `1234` | Dashboard (full committee access) |
| Secretary | `A-102` | `9000000002` | `2345` | Dashboard (full committee access) |
| Resident | `B-201` | `9000000003` | `5678` | My Flat Portal |
| Gate Guard | `GATE-01` | `9000000004` | `9012` | Gate Security terminal |

---

## What the reviewer will see

Seeded into Firestore (`societysarv-app-db`) as the demo society:

- **Society** — "SocietySarv Demo Society", code `DEMO1`, ₹1,25,000 bank + ₹5,000 cash
- **4 members** — one per role, across wings A, B and the gate
- **3 maintenance bills** — 2 paid (with receipts), 1 unpaid so the payment and
  UPI-QR flows can be exercised
- **2 expenses** — lift AMC and common electricity
- **1 non-fee revenue** — community hall booking
- **2 gate visitor entries** — one inside, one exited, both with passcodes
- **2 notices** — one pinned AGM notice, one water-supply notice
- **1 complaint** — in progress
- **4 emergency contacts**

Nothing here is a real person, phone number, bank account or UPI ID.

---

## Keep this working

- **Do not delete the `DEMO1` society** from Firestore. Play re-reviews the app
  on every update, and the reviewer uses these same credentials each time.
- **The app is single-society.** One Firestore database holds exactly one
  society (`society_info/main`), so while `DEMO1` is seeded, "Register New
  Society" will refuse with "… is already registered. Please log in instead."
  Every install of the published app therefore sees this same demo society.
  Serving real societies needs a per-society data model first — see
  README-FIREBASE.md.
- **Firestore rules are currently open** (no Anonymous Auth yet). Enable
  Anonymous Auth before launch, then deploy `firestore.rules.secure`; the
  predeploy guard in `scripts/check-anonymous-auth.sh` blocks that deploy until
  the provider is on.

To re-seed the demo society after a wipe, re-run the seeding script that created
it (`demo_seed.py`, kept with the release notes).
