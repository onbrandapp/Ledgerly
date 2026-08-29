# Ledgerly - Smart Android Financial Ledger & Expense Tracker

[![Android Target SDK](https://img.shields.io/badge/Target%20SDK-36-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-purple.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-blue.svg)](https://developer.android.com/jetpack/compose)
[![Firebase](https://img.shields.io/badge/Firebase-Firestore%20%7C%20Auth-orange.svg)](https://firebase.google.com)
[![Gemini AI](https://img.shields.io/badge/Gemini%20AI-2.5%20Flash-4285F4.svg)](https://ai.google.dev)

**Ledgerly** is a modern, privacy-respecting, intelligent personal finance and expense tracking Android application. Built natively using **Kotlin**, **Jetpack Compose (Material Design 3)**, **Android Jetpack Room**, **Firebase Firestore**, and the **Gemini 2.5 Flash API**, Ledgerly simplifies personal bookkeeping through smart natural-language parsing, real-time analytics, recurring payment automations, and future revenue pipeline forecasting.

---

## Key Features

### 1. Smart AI Natural Language Logging
- Powered by **Google Gemini 2.5 Flash**.
- Type or paste natural phrases like:
  - *"Spent $14.50 on lunch at Chipotle"*
  - *"Received $2,400 monthly freelance retainer from Acme Corp"*
  - *"Paid $68 for electric utility bill"*
- Gemini intelligently extracts the financial **amount**, **category**, **type (Expense/Income)**, and **description** in structured JSON format and pre-fills the entry sheet with a single tap.

### 2. Complete Financial Ledger & Search
- Full transaction history with quick search by note or merchant.
- Filter by date range (Today, This Week, This Month, Custom Date Spans), transaction type (Expense, Income, All), or Category.
- Instant CSV export and data summary totals.
- Mark transactions as Paid / Unpaid to reconcile bills and pending receipts.

### 3. Future Income Forecasting & Pipeline Manager
- Project upcoming invoices, anticipated client retainers, year-end bonuses, and expected dividends.
- Track pipeline status (*Confirmed*, *Expected*, *Tentative*).
- **One-Tap Realization**: Convert forecasted revenue directly into active ledger income once funds are received.
- **Interactive Milestones**: Attach bulleted checklists to forecasts with completion toggles and strikethroughs.

### 4. Brainstorming Notes & Scratchpad
- Dedicated bulleted notes for future income ideas, rate increase goals, and side hustle milestones.
- Customizable color-coded tags and interactive checklist items.

### 5. Automated Recurring Transactions
- Create recurring rules for subscriptions, rent, payroll, and utilities on Daily, Weekly, Monthly, or Yearly cadences.
- Auto-logs entries with notification controls and series management.

### 6. Budgeting & Visual Analytics
- Monthly spending targets with dynamic progress indicators and visual warning thresholds.
- Dynamic expense breakdowns by category and interactive daily cash-flow trend charts.
- Custom categories creation with personalized color palettes.

### 7. Dual Persistence: Offline-First + Cloud Sync
- **Local Database**: Zero-latency, completely offline functionality powered by Jetpack Room (SQLite).
- **Cloud Database**: Real-time cross-device sync with Firebase Firestore and Google Identity authentication.

---

## Tech Stack & Architecture

| Component | Technology |
|---|---|
| **Language** | Kotlin 2.0+ (100% Kotlin DSL Gradle) |
| **UI Framework** | Jetpack Compose with Material Design 3 (M3) |
| **Architecture** | MVVM (Model-View-ViewModel) + Reactive Unidirectional Data Flow |
| **Concurrency** | Kotlin Coroutines & Kotlin StateFlow / SharedFlow |
| **Local Database** | Jetpack Room (SQLite with KSP codegen) |
| **Cloud Backend** | Firebase Firestore (Real-time reactive snapshot listeners) |
| **Authentication** | Google Sign-In via Jetpack Credential Manager + Guest Mode |
| **AI Integration** | Gemini 2.5 Flash REST API via OkHttp |
| **Target Platforms** | Android 7.0 (API 24) to Android 15+ (API 36) |

---

## Project Structure

```
app/src/main/java/com/example/
├── MainActivity.kt                # Application entry point, Edge-to-Edge setup & Navigation
├── data/
│   ├── AuthRepository.kt          # Google Credential Manager & user session handling
│   ├── GeminiParser.kt            # Gemini 2.5 Flash natural language parser
│   ├── LocalDatabase.kt           # Room Database, DAOs, and Local Entities
│   ├── Models.kt                  # Domain data classes (Transaction, ForecastIncome, Note, etc.)
│   └── TransactionRepository.kt   # Dual Repository (Firebase & Room implementations)
└── ui/
    ├── screens/
    │   ├── DashboardScreen.kt     # Main financial dashboard, ledger drawer, charts & bento grids
    │   ├── ForecastIncomeSection.kt# Future income pipeline, milestone checklists & notes
    │   └── LoginScreen.kt         # Google Sign-In & Guest authentication interface
    ├── theme/
    │   ├── Color.kt               # Dynamic M3 light & dark color schemes
    │   ├── Theme.kt               # Centralized Material3 Theme provider
    │   └── Type.kt                # Typography configurations
    └── viewmodel/
        └── ExpenseViewModel.kt    # Core business logic, StateFlows, calculations & aggregations
```

---

## Getting Started

### Prerequisites
- **Android Studio** Ladybug (2024.2+) or later
- **JDK 17** or **JDK 21**
- **Android SDK** API 36 installed
- A Google Cloud / Gemini API key

### Configuration

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/onbrandapp/Ledgerly.git
   cd Ledgerly
   ```

2. **Set up API Keys**:
   Create or update `.env` in the root directory (or inject via AI Studio Secrets Panel):
   ```properties
   GEMINI_API_KEY=your_gemini_api_key_here
   ```

3. **Firebase Setup**:
   Ensure `google-services.json` is present in `/app` for cloud Firestore synchronization.

4. **Build & Run**:
   ```bash
   ./gradlew assembleDebug
   ```

---

## License

Copyright (c) 2026 Ledgerly App. All rights reserved.
