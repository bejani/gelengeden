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

1. Open **Android Studio**
2. **File → Open** and select the `gelengeden` folder
3. Wait for Gradle sync
4. Run on an emulator or device (API 26+)

If Gradle wrapper jars are missing, Android Studio will offer to generate them, or use:

```bash
gradle wrapper
```

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
- minSdk 26 / targetSdk 35
