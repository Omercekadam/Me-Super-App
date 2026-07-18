# Me SuperApp

A personal "life dashboard" Android app — finance tracking, agenda & habits, and an activity archive — built with **Kotlin + Jetpack Compose** on the UI side and an **embedded Python analytics engine** (pandas via [Chaquopy](https://chaquo.com/chaquopy/)) doing the number crunching on-device.

> Offline-first: all data lives on the device in SQLite (Room). No accounts, no cloud.

## Why embedded Python?

Data analysis is my home turf, so the analytics layer (spending breakdowns, month-end forecasts, insight generation, play/work balance) is written as a plain Python package with pandas. The same package:

- runs **inside the APK** through Chaquopy at runtime,
- is tested **on the desktop with pytest** — no device or emulator needed.

The Kotlin ↔ Python surface is a single function: `run(fn, payload_json) -> result_json`. Kotlin owns the database and all writes; Python is a pure, stateless calculator.

```
┌────────────── Compose UI (Material 3) ──────────────┐
│ Dashboard │ Finance │ Agenda │ Activity │ Quick-add │
└──────────────────────┬───────────────────────────────┘
                 ViewModel (StateFlow)
                       │
                Repository (Kotlin)
          ┌────────────┼────────────────┐
     Room (SQLite)  Retrofit         WorkManager
                       │
        AnalyticsEngine (Chaquopy bridge)
                       │  JSON in → JSON out
        Python package: analytics/ (pandas)
```

## Modules

| Module | Status | Highlights |
|---|---|---|
| Finance | 🚧 in progress | income/expense tracking, category pie, month-end forecast, budget limits, subscriptions, savings goals, rule-based insights |
| Dashboard | 🚧 in progress | today at a glance + quick-add bar |
| Agenda | planned | habit chains, to-dos, pomodoro (with task pairing), GitHub commit streak |
| Activity archive | planned | Steam playtime sync, game/movie/TV logging & ratings (RAWG + TMDB), sim-racing lap journal, weekly play/work balance |

## Tech stack

Kotlin 2.3 · Jetpack Compose (Material 3) · Hilt · Room · WorkManager · kotlinx.serialization · Chaquopy 17 (Python 3.13, pandas) · GitHub Actions

## Development

```bash
# Python analytics tests (desktop, no device needed)
pip install pandas pytest
pytest

# Android build
./gradlew assembleDebug
```

Money is stored as integer kuruş (minor units) — never floats. API keys (GitHub/Steam/TMDB/RAWG) are entered in-app and stored on-device only.
