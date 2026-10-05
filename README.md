# Society Sarv

Society management app for residents, security teams, and committee members.
Built with **Flutter**, backed by **Cloud Firestore** (named database
`societysarv-app-db` in Firebase project `multisarv-mobileapps`).

> This branch is the Flutter port of the original Kotlin / Jetpack Compose app
> (still on `main`). The Firestore schema, collection names, field names and
> enum strings are unchanged, so both apps read and write the same data.

## Run locally

Prerequisites: Flutter 3.44+ (Dart 3.12+), Android SDK, a device or emulator.

```bash
flutter pub get
flutter run            # debug build on the connected device
flutter test           # unit tests (billing, payment allocation, UPI URI, ...)
```

`android/app/google-services.json` is committed, so Firebase works out of the
box on Android. iOS needs `ios/Runner/GoogleService-Info.plist` from the
Firebase console before it will start.

## Release build (Play Store)

The application id is `com.multisarv.societysarv`, the same as the Kotlin app,
so the Flutter build installs as an update over it. The version lives in
`pubspec.yaml` (`version: 1.6.0+12` → versionName 1.6.0, versionCode 12).

Signing reads `keystore.properties` at the repository root (git-ignored), or
the `KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` environment
variables:

```properties
storeFile=societysarv-release-key.jks
storePassword=...
keyAlias=upload
keyPassword=...
```

```bash
flutter build appbundle --release   # build/app/outputs/bundle/release/app-release.aab
flutter build apk --release         # build/app/outputs/flutter-apk/app-release.apk
```

Without a keystore the release build falls back to the debug key.

Phone OTP (Forgot PIN) needs the SHA-1/SHA-256 of the signing key registered
in the Firebase console, exactly as before.

## Project layout

```
lib/
  main.dart                    app shell, bottom navigation by role
  data/                        models, Firestore mappers, repository, session, OTP
  state/society_view_model.dart  all business logic (ChangeNotifier + Provider)
  l10n/                        English / Hindi / Marathi strings, location lists
  ui/theme/                    colours and Material 3 theme
  ui/components/               shared widgets (top bar, badges, receipt, UPI QR)
  ui/screens/                  one file per tab, plus auth and profile
  util/                        formatting, image blobs, UPI QR, receipt/ledger PDFs
test/                          pure-logic unit tests
firestore.rules, firestore.indexes.json, firebase.json   backend config (unchanged)
```

See `README-FIREBASE.md` for the backend setup and `PLAY-STORE-DEMO-LOGIN.md`
for the Play Store review login.
