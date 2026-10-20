# SocietySarv — Cloud Firestore Backend

The app has **no local seed data, no Room database and no demo/mock records
anywhere**. Every screen reads from and writes to Cloud Firestore in real time,
and every row in the database is one a real user created through the app.

| | |
|---|---|
| Firebase project | `multisarv-mobileapps` |
| Firestore database | `societysarv-app-db` (a **named** database — `(default)` belongs to another app) |
| Android app | `com.multisarv.societysarv` (`1:1085525087237:android:58ce055aa6c0e1b959786c`) |
| Config file | `app/google-services.json` |
| Location / edition | `nam5` · Standard · Firestore Native |

Refresh the config any time with:

```bash
firebase apps:sdkconfig ANDROID 1:1085525087237:android:58ce055aa6c0e1b959786c \
  --project multisarv-mobileapps --out app/google-services.json
```

---

## Collections & schema

Document ids: `society_info` uses the fixed id **`main`**, `residents` uses the
**flat number** (so login is a single direct `get()`), everything else uses
Firestore auto-ids. Every document also carries `docId` (its own id) and `id`
(the numeric key the UI sorts and keys lists on).

Enums are stored as their **name string** (`"CHAIRMAN"`, `"UNPAID"`, …) so the
documents stay readable in the console. An unknown or missing value falls back
to a safe default when read.

### `society_info` — 1 doc (`main`)
`id:int` · `name` · `registrationNumber` · `societyCode` · `address` ·
`totalWings:int` · `totalFlats:int` · `bankAccountName` · `bankAccountNumber` ·
`ifscCode` · `currentBalanceCash:double` · `currentBalanceBank:double` ·
`lateFeePercentPerMonth:double` · `lateFeeGraceDays:int` · `tariff1BHK:double` ·
`tariff2BHK:double` · `tariff3BHK:double` · `tariffPenthouse:double` ·
`tariffShop:double` · `sinkingFundPercent:double` · `repairFundPercent:double` ·
`logoBase64`

### `residents` — doc id = flat number
`id:int` · `flatNumber` · `wing` · `ownerName` · `tenantName` · `contactNumber` ·
`email` · `role` (`CHAIRMAN|SECRETARY|TREASURER|COMMITTEE|RESIDENT|TENANT|GUARD`) ·
`pin` · `familyMemberCount:int` · `vehicleTwoWheelerCount:int` ·
`vehicleFourWheelerCount:int` · `twoWheelerNumber` · `fourWheelerNumber` ·
`parkingSlot` · `flatType` (`ONE_BHK|TWO_BHK|THREE_BHK|PENTHOUSE|COMMERCIAL_SHOP`) ·
`squareFeet:int` · `balanceDue:double` · `isOwnerLiving:bool` · `photoBase64`

### `maintenance_bills`
`id:int` · `flatNumber` · `residentName` · `billMonth` · `baseAmount:double` ·
`waterCharges:double` · `sinkingFund:double` · `repairFund:double` ·
`parkingCharges:double` · `nonOccupancyCharges:double` · `lateFeeAmount:double` ·
`totalAmount:double` · `amountPaid:double` · `status` (`PAID|PARTIAL|UNPAID`) ·
`dueDate` · `paidDate` · `paymentMode` (`UPI|BANK_TRANSFER|CHEQUE|CASH`, nullable) ·
`transactionRef` · `receiptNumber` · `receiptImageBase64`

### `expense_records`
`id:int` · `title` · `category` (`WATER_TANKER|LIFT_AMC|SECURITY_SALARY|GARDENING|`
`HOUSEKEEPING|ELECTRICITY|REPAIRS|MISCELLANEOUS`) · `amount:double` · `date` ·
`vendorName` · `vendorContact` · `paymentMode` · `approvedBy` · `notes` ·
`receiptNumber` · `status` · `billImageBase64`

### `non_fee_revenues`
`id:int` · `title` · `sourceCategory` (`CLUBHOUSE_BOOKING|PARKING_FEE|TOWER_RENT|`
`SOLAR_CREDIT|HALL_RENTAL|PENALTY_FINE`) · `amount:double` · `date` · `payerName` ·
`flatNumber` · `paymentMode` · `receiptNumber` · `notes`

### `visitor_logs`
`id:int` · `visitorName` · `phone` · `flatNumber` ·
`purpose` (`DELIVERY|GUEST|CAB|SERVICE|OTHER`) · `vehicleNumber` · `entryTime` ·
`exitTime` · `status` (`INSIDE|EXITED|PRE_APPROVED|REJECTED`) · `passcode` ·
`guardName` · `photoBase64`

### `notices`
`id:int` · `title` · `content` · `publishedDate` · `isPinned:bool` ·
`category` (`AGM|MAINTENANCE|WATER|FESTIVAL|GENERAL`) · `authorRole` · `imageBase64`

### `complaints`
`id:int` · `flatNumber` · `residentName` ·
`category` (`PLUMBING|ELECTRICAL|LIFT|LEAKAGE|NOISE|SECURITY|OTHER`) ·
`description` · `filedDate` · `status` (`OPEN|IN_PROGRESS|RESOLVED`) ·
`resolutionNotes` · `attachmentBase64`

### `emergency_contacts`
`id:int` · `titleEn` · `titleMr` · `name` · `phoneNumber` · `category`

### `counters`
One doc per collection (`value:int`). A Firestore transaction increments it to
hand out the next numeric `id`, keeping the auto-increment behaviour the UI
relied on under Room.

---

## Images are Base64 blobs inside the JSON

Every `*Base64` field holds a JPEG encoded as a Base64 string, so a document is
self-contained — no Cloud Storage bucket and no second round trip to draw a
photo. `com.example.util.ImageBlob` does the conversion both ways:

```kotlin
val blob   = ImageBlob.fromBitmap(bitmap)        // Bitmap  -> Base64 (for Firestore)
val bitmap = ImageBlob.toBitmap(doc.photoBase64) // Base64  -> Bitmap
val image  = rememberBlobImage(resident.photoBase64) // Compose ImageBitmap
```

`fromBitmap` downscales to 1024 px on the long edge and keeps re-compressing
until the string is under ~700 KB, comfortably inside Firestore's 1 MB document
limit. All blob fields are **exempt from indexing** via `fieldOverrides` in
`firestore.indexes.json` — a large string would otherwise blow the 1500-byte
index entry limit and the write would be rejected.

---

## Rules

`firestore.rules` is deployed. Reads are open; creates and updates validate the
payload shape; deletes are allowed but checked separately, because
`request.resource` is **null** on a delete and must not be inspected there.
Anything outside the ten known collections is denied.

### Locking it down — do these two steps IN ORDER

The app already calls Firebase **Anonymous Auth** on startup
(`FirebaseModule.ensureSignedIn()`), and it currently fails with
`ADMIN_ONLY_OPERATION` because the provider is off.

> ⚠️ **Deploying `firestore.rules.secure` before enabling Anonymous Auth breaks
> the whole app** — every read and write comes back `PERMISSION_DENIED`, because
> the rules demand an identity the app cannot obtain yet.

**Step 1 — enable the provider first:**

> Firebase Console → **Authentication** → **Sign-in method** → **Anonymous** → Enable

**Step 2 — confirm it works**, then deploy the strict rules:

```bash
# should print an idToken, NOT "ADMIN_ONLY_OPERATION"
curl -s -X POST "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=AIzaSyCjodhNA-w07k72T_tpyMZ8YT99veKOngg" \
  -H "Content-Type: application/json" -d '{"returnSecureToken":true}'

cp firestore.rules.secure firestore.rules
firebase deploy --only firestore:rules --project multisarv-mobileapps
```

To roll back if anything goes wrong, flip `allowed()` in `firestore.rules` back
to `return true;` and redeploy.

The app's own PIN login is untouched by this — anonymous auth only gives the
Firestore SDK a token to talk to the backend with.

---

## Indexes

13 composite indexes are deployed from `firestore.indexes.json`, covering every
query the repository issues:

| Collection | Fields |
|---|---|
| `residents` | `wing ASC, flatNumber ASC` · `role ASC, flatNumber ASC` |
| `maintenance_bills` | `flatNumber ASC, id DESC` · `status ASC, id DESC` · `billMonth ASC, flatNumber ASC` |
| `expense_records` | `category ASC, id DESC` |
| `non_fee_revenues` | `sourceCategory ASC, id DESC` |
| `visitor_logs` | `status ASC, id DESC` · `flatNumber ASC, id DESC` |
| `notices` | `isPinned DESC, id DESC` · `category ASC, id DESC` |
| `complaints` | `flatNumber ASC, id DESC` · `status ASC, id DESC` |

---

## Deploying

```bash
firebase deploy --only firestore --project multisarv-mobileapps   # rules + indexes
firebase deploy --only firestore:rules   --project multisarv-mobileapps
firebase deploy --only firestore:indexes --project multisarv-mobileapps
```

`firebase.json` already points at the `societysarv-app-db` database, so no extra
flags are needed.

---

## Code map

| File | Role |
|---|---|
| `data/firebase/FirebaseModule.kt` | Firestore handle for the named DB, offline cache, anonymous sign-in, collection names |
| `data/firebase/FirestoreMappers.kt` | document ⇄ entity conversion, enum-as-string, safe defaults |
| `data/repository/SocietyRepository.kt` | all reads (realtime snapshot `Flow`s) and writes, id counters, treasury updates |
| `data/entity/SocietyEntities.kt` | plain Kotlin models (no Room annotations) |
| `util/ImageBlob.kt` | Base64 ⇄ Bitmap for the image blob fields |

`SocietyViewModel` and every screen are unchanged apart from the repository being
constructed from a `Context` instead of a Room database — the UI, navigation and
user flow are exactly as they were.


---

## Release build & signing

| | |
|---|---|
| Keystore | `societysarv-release-key.jks` (PKCS12, repo root) |
| Alias / store pass / key pass | `societysarv` |
| Certificate | `CN=MultiSarv India, OU=SocietySarv, O=MultiSarv India, L=Navi Mumbai, ST=Maharashtra, C=IN` |
| Valid until | 06 Mar 2054 |
| SHA-1 | `78:5A:00:B6:2F:8D:7B:53:BB:63:55:27:66:A3:41:6A:C5:89:C1:AD` |
| SHA-256 | `0D:0E:25:3C:D7:D7:1F:DC:14:D2:86:DF:A3:B5:C1:45:FB:6F:F1:56:FD:3E:46:2A:9C:A8:BB:49:C3:D3:B7:13` |

Credentials live in `keystore.properties` (git-ignored). `app/build.gradle.kts`
reads that file first and falls back to the `KEYSTORE_PATH` / `STORE_PASSWORD` /
`KEY_ALIAS` / `KEY_PASSWORD` environment variables, so CI can inject them
instead of shipping the file.

**Back up `societysarv-release-key.jks` and `keystore.properties` somewhere
safe.** Without them you can never publish an update to the same Play listing.

```bash
gradle :app:bundleRelease    # -> app/build/outputs/bundle/release/app-release.aab  (upload this)
gradle :app:assembleRelease  # -> app/build/outputs/apk/release/app-release.apk     (sideload testing)
```

If you enable Play App Signing (the default), add the SHA-1 above to the
Firebase Android app **and** add Google's app-signing SHA-1 from the Play
Console once the first release is processed — otherwise features that check the
signing certificate will only work on your locally built APK.


---

## Sign-up and sign-in

There is no seeded account. The very first thing a fresh install can do is
**Register New Society**; everything else follows from that.

### Society registration (4-step wizard)

1. **Society details** - name and a *society login code* are required; the code
   is what every member types on the login screen.
2. **Office bearers** - chairman name, flat number, 10-digit mobile, and a
   **login PIN** they set themselves (entered twice, must match).
3. **Bank & tariff slabs** - optional figures for the opening balance and the
   per-flat-type monthly maintenance rates.
4. **Confirmation** - restates the exact credentials the chairman will log in
   with.

Each step validates before **Save & Continue** enables, so a half-filled
registration cannot be submitted. Finishing writes two documents:

- `society_info/main` - the society master record
- `residents/{chairmanFlat}` - the chairman, `role: CHAIRMAN`, with the PIN
  they chose

and signs the chairman straight in.

Only **one society per database**. Registering again while `society_info/main`
exists is refused with "… is already registered. Please log in instead.", so an
accidental re-run cannot orphan the members and ledgers pointing at the current
society. To start over, delete the `society_info/main` document.

### Adding members

Committee members are added from **Maintenance → Resident Directory → Add New
Resident**. Flat number, owner name, mobile and a **login PIN** are required;
the member then signs in with that flat number and PIN. Duplicate flat numbers
are rejected. Each new member becomes a `residents/{flatNumber}` document with
`role: RESIDENT`.

### Login

`login(societyCode, flatOrMobile, pin)` in `SocietyViewModel`:

1. all three fields must be non-empty;
2. a society must exist - otherwise "No society is registered yet. Use
   'Register New Society' first.";
3. the society code must match `society_info/main.societyCode`
   (case-insensitive);
4. the identifier is resolved as **flat number first, mobile number second** -
   the old build guessed from the first character (`A`/`B`/`C`/`G` only), which
   silently locked out every other wing and every mobile-number login;
5. a member with no PIN set is told to ask the committee rather than being
   rejected as "wrong PIN";
6. the PIN must match.

On success the role decides the landing tab: `GUARD` → Gate Security,
`RESIDENT`/`TENANT` → My Flat Portal, everyone else → Dashboard.

### Verified end to end on device

1. Registered "Ganesh Krupa Co-operative Housing Society" (code `GK001`) with
   chairman Sagar Shinde, flat `A-101`, PIN `2580` → both documents appeared in
   Firestore.
2. Logged out, logged back in as `GK001` / `A-101` / `2580` → Chairman
   dashboard. A deliberately wrong code returned "Invalid society code!".
3. As chairman, added member Priya Kulkarni, flat `B-204`, PIN `7391` →
   `residents/B-204` created with `id: 2`.
4. Logged out, logged in as `GK001` / `B-204` / `7391` → signed in as
   "Resident Member" on the My Flat Portal with her own flat, wing and parking
   slot.

That test society was deleted afterwards, so the database is empty and ready
for the real registration.
