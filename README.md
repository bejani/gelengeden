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
- **Bank SMS capture**: opt-in parsing for trusted senders, with a required title and user confirmation before any transaction is saved
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

## Bank SMS capture

Open **Settings → Bank SMS capture** to enable SMS reception and add each bank sender address that the app may process. The app only evaluates messages from those configured senders. It recognizes the amount and transaction direction, stores a local pending draft, and asks the user to enter a title and confirm before creating an income or expense transaction. The full SMS body is not transmitted or displayed in the confirmation flow.

Amounts in Gelengeden are stored in **Toman**. For every sender, choose whether its SMS amounts are already in Toman or are in Rial and should be divided by ten. The Bank Melli withdrawal form `برداشت:2,397,273-` is covered by the parser and becomes an expense suggestion.

> `RECEIVE_SMS` is a sensitive permission. For Google Play distribution, review the current [SMS and Call Log Permissions policy](https://support.google.com/googleplay/android-developer/answer/10208820) and submit any required permissions declaration before publishing. The feature works only after the user explicitly grants SMS access at runtime.

## Continuous integration

GitHub Actions validates the Gradle Wrapper, runs debug unit tests, builds a debug APK, and retains the resulting APK as a short-lived workflow artifact for every push and pull request to `main`.
