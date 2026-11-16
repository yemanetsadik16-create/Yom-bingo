# YEME BINGO

Real-time multiplayer 75-ball Stake Bingo Android application built with Kotlin, Jetpack Compose (Material 3), Firebase Authentication, and Cloud Firestore.

## Features
- **Synchronized 8-Minute Cycles:** Shared 90-second joining lobby and deterministic 5-second ball calling across 5 ETB stake rooms (`10`, `20`, `50`, `100`, `200` ETB).
- **Real-Time Multiplayer & Payouts:** Live player counts, taken-ticket tracking, automatic solo-room (`< 2` players) stake refunds, and atomic **80%** prize pool payouts on valid `BINGO!` claims.
- **Interactive Game Card & Voice Caller:** 6 rotating target patterns, manual tap + auto-mark support, and on-device English `TextToSpeech` ball announcements.
- **ETB Wallet & Telebirr Flows:** Deposit receipt submission (`Yemane · 0939383425`, minimum `30` ETB), withdrawal requests with upfront balance hold, and transaction ledger.
- **Bilingual (`EN` / `TI`):** Instant English and Tigrinya switching from the top header bar.
- **Admin Dashboard:** Approve/decline deposit requests, mark withdrawals paid or decline & refund, and manage user wallets.

## Tech Stack
- **UI:** Jetpack Compose, Material 3, Navigation Compose
- **Backend:** Firebase Authentication (Username/Password via Email/Password provider), Cloud Firestore
- **Testing:** JUnit, Robolectric, Firebase Local Emulator Suite (`firestore.test.js`, `BingoRepositoryRuleTest`)

## Publishing to GitHub & Generating APKs
1. **Push to GitHub from AI Studio:** Open the settings menu in Google AI Studio and select **Push to GitHub**. Sensitive files (`.env`, `*.jks`, `debug.keystore`, `local.properties`) are excluded via `.gitignore`.
2. **Configure Firebase for GitHub Actions APK Builds:** AI Studio omits `app/google-services.json` when pushing to GitHub. In your GitHub repository, open **Settings → Secrets and variables → Actions → New repository secret**, create a secret named `GOOGLE_SERVICES_JSON`, and paste the contents of `app/google-services.json` (exported from AI Studio or Firebase Console) so the GitHub-built APK connects to Firebase at runtime.
3. **Generate Signed APK / AAB in AI Studio:** Open the settings menu and select **Generate APK / AAB** to export an installable APK or Play Store AAB directly.
4. **Automated GitHub Actions APK Builds:** Every push to `main` builds `app-debug.apk` via `.github/workflows/android-apk.yml` and uploads it as a downloadable workflow artifact; pushing a version tag (e.g. `git tag v1.0.0 && git push origin v1.0.0`) publishes the APK directly to a GitHub Release.
5. **Release Signing Locally:** Set `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD` environment variables (with key alias `upload`) and run:
   ```bash
   gradle :app:assembleRelease
   ```
6. **Firebase Security Rules:** Deploy `firestore.rules` to your Firebase project before production use:
   ```bash
   firebase deploy --only firestore:rules
   ```
