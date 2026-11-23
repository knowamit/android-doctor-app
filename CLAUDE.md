# Android Doctor App

## What is this?
Companion Android app + landing page for [android-doctor](https://github.com/knowamit/android-doctor) CLI.
The app runs on-device diagnostics (CPU, RAM, storage, battery, bloatware) without root, using Shizuku for privileged shell access.

## Project structure
```
app/src/main/java/com/androidoctor/
├── ui/
│   ├── screens/        # Home, Diagnose, Fix, Benchmark, Battery, Trends, Bloatware
│   ├── components/     # ScoreCard, shared composables
│   ├── share/          # ReportCardRenderer (1080x1920 PNG)
│   ├── navigation/     # NavGraph
│   └── theme/          # Material3 theme
├── domain/
│   ├── model/          # DeviceInfo, DiagnosisResult, BenchmarkResult, BloatwareEntry, AppBatteryUsage
│   ├── repository/     # DiagnosisRepository interface
│   └── usecase/        # DiagnoseUseCase (writes ScoreSnapshot), FixUseCase
├── data/
│   ├── repository/     # Impl classes
│   ├── parser/         # CPU, Memory, Storage, Battery, Bloatware, VerdictEngine,
│   │                   # BatteryAttributionParser, BloatwareCatalogLoader
│   ├── shell/          # ShellExecutor (Shizuku + fallback)
│   └── local/          # Room DB v3 (MIGRATION_1_2, MIGRATION_2_3; schemas in app/schemas)
├── worker/             # WeeklyHealthWorker (WorkManager + Hilt)
├── tile/               # DoctorTileService (Quick Settings)
├── di/                 # Hilt AppModule
├── MainActivity.kt
└── DoctorApp.kt        # Application + WorkManager Configuration.Provider
app/src/main/assets/bloatware/   # JSON catalogs: generic, samsung, xiaomi, oneplus, oppo, realme
app/schemas/            # Room exported schemas (checked in for migration testing)
docs/                   # Landing page (GitHub Pages), designs/
store-listing/          # Play Store metadata
fastlane/               # CI/CD: build, deploy, promote lanes
```

## Tech stack
- **Language**: Kotlin, Java 17 target
- **UI**: Jetpack Compose + Material3
- **DI**: Hilt (KSP)
- **Local DB**: Room
- **Preferences**: DataStore
- **Shell access**: Shizuku 13.1.5 (no root needed)
- **Async**: Kotlin Coroutines
- **Build**: Gradle 8.7.3, version catalog (`gradle/libs.versions.toml`)
- **Deploy**: Fastlane → Google Play (triple-play plugin)
- **Analytics/Crash**: Firebase Analytics + Crashlytics (BoM 33.7.0, project `android-doctor-17709`, owner Ammmroid@gmail.com)
- **Landing page**: Static HTML in `docs/` (GitHub Pages)

## Key commands
```bash
# Build debug APK
./gradlew assembleDebug

# Build release AAB
./gradlew bundleRelease

# Run tests
./gradlew test

# Fastlane
bundle exec fastlane build          # Signed release AAB
bundle exec fastlane deploy_internal # Deploy to internal track
bundle exec fastlane bump name:1.1.0 # Bump version
```

## Architecture
Clean Architecture with 3 layers:
- **domain/** — models, repository interfaces, use cases (no Android deps)
- **data/** — repository impls, parsers, shell execution, Room DB
- **ui/** — Compose screens + ViewModels (MVVM)

Data flow: Screen → ViewModel → UseCase → Repository → ShellExecutor/Room

## Design system
- Zinc-950 background, copper (#C2703E) accent
- 1px borders over shadows, 4px border-radius
- Follow anti-AI UI rules from global config

## Signing
- Keystore: `release-keystore.jks` (root dir)
- Secrets via env vars: `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`
- Play Console: `play-service-account.json` (root dir)
- **NEVER commit these files to public repos**

## Package
- ID: `com.androidoctor`
- Min SDK 26 (Android 8.0), Target SDK 35
- Current: v1.1.0, versionCode 4

## GitHub
- Repo: `knowamit/android-doctor-app`
- Pages: `https://knowamit.github.io/android-doctor-app/`
- CLI repo: `knowamit/android-doctor`
