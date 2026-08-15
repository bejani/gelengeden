# Gelengeden

Personal finance tracker for **income (gelen)** and **expenses (giden)**.

Built with **Kotlin**, **Jetpack Compose**, **Material 3**, and **Room**.

## Features

- Add / edit / delete income and expense transactions
- Categories for income and expenses
- Live balance, total income, and total expense
- Filter list: All · Income · Expense
- **Reports** with period presets, custom range, and type filters
- Charts: category donut, monthly bar & line, net-balance sparkline
- Insights: savings rate, daily averages, top category, busiest month
- **Backup & restore**: export all transactions and categories as JSON, restore with full replace
- **App lock**: local password on first launch, sign-in gate, change password in Settings
- Local Room database (data stays on device)
- Light / dark theme support

## Open in Android Studio

1. Open **Android Studio**.
2. Choose **File → Open** and select the `gelengeden` folder.
3. Wait for Gradle sync to finish.
4. Run on an emulator or device with Android API 26 or later.

The Gradle Wrapper is included. From a terminal, use the following commands:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Project structure

```
app/src/main/java/com/gelengeden/app/
├── GelengedenApp.kt          # Application + DI entry
├── MainActivity.kt           # Compose host
├── data/                     # Room entity, DAO, DB, repository
└── ui/
    ├── components/           # Summary, rows, charts
    ├── navigation/           # Nav graph
    ├── report/               # Report models & aggregation
    ├── screens/              # Home, Login, Settings, Add/Edit, Reports, …
    ├── theme/                # Material 3 theme
    ├── util/                 # Money & date formatters
    └── viewmodel/            # TransactionViewModel, AuthViewModel
```

## Requirements

- Android Studio Ladybug (or newer)
- JDK 17
- Android SDK Platform 34
- minSdk 26 / targetSdk 34

## Data and security

Financial data stays in the local Room database. Android device/cloud backup is intentionally disabled so that app-lock preferences and private financial records are not copied through the system backup channel. Use the in-app JSON backup and restore flow when you choose to move or preserve your data.

Release signing keys are deliberately excluded from this repository. Create and store them securely outside version control before distributing a signed release.

## Continuous integration

GitHub Actions validates the Gradle Wrapper, runs debug unit tests, builds a debug APK, and retains the resulting APK as a short-lived workflow artifact for every push and pull request to `main`.
