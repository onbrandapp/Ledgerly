# System Architecture & Technical Design - Ledgerly

This document details the architectural principles, component structures, data models, state flows, and integration paradigms utilized within **Ledgerly** (v87.0).

---

## 1. Architectural Philosophy

Ledgerly is designed adhering strictly to **Modern Android Architecture Guidelines**:
- **Reactive Unidirectional Data Flow (UDF)**: UI states flow downwards from ViewModel to Composable views; user intent events flow upwards from UI to ViewModel.
- **Offline-First Resilience**: Full local persistence via **Jetpack Room (SQLite)** ensures instant responsiveness with zero network latency, with seamless bidirectional synchronization to **Firebase Firestore**.
- **Separation of Concerns**: High cohesion and low coupling across UI, Domain, and Data layers.
- **Defense in Depth**: Zero unencrypted cloud exports, on-device biometric security gating, and duplicate entry safeguards.
- **Type Safety**: Strictly typed Kotlin Coroutines, Kotlin Flows, Room TypeConverters, and Jetpack Compose state primitives.

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           UI Layer (Jetpack Compose)                            │
│  DashboardScreen  │  ForecastIncomeSection  │  ReportsScreen  │  LoginScreen    │
│  SearchOverlay    │  AuditReportSheet       │  BackupSheet    │  BiometricLock  │
└────────────────────────────────────────┬────────────────────────────────────────┘
                                         │ User Actions / UI State
                                         ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              ViewModel Layer                                    │
│                             ExpenseViewModel                                    │
│   - Duplicate Detection Engine            - Backup & Restore Orchestration      │
│   - Audit Trail Tracking                  - Dynamic Aggregations & Metrics      │
│   - Monthly Budget Rules                  - Recurring Series Engine             │
└───────┬────────────────────────┬───────────────────────┬────────────────────────┘
        │                        │                       │
        ▼                        ▼                       ▼
┌──────────────┐ ┌─────────────────────────────┐ ┌────────────────────────────────┐
│  AI Parsing  │ │   Security & Biometrics     │ │     Audit & Document Export    │
│ GeminiParser │ │    BiometricAuthManager     │ │  AuditPdfExporter / CSV Export │
└──────────────┘ └───────────────┬─────────────┘ └────────────────┬───────────────┘
                                 │                                │
                                 ▼                                ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                     Repository Layer (Data Persistence)                         │
│                         TransactionRepository (Interface)                       │
│                       BackupManager (Local & Cloud Snapshots)                   │
│                    ▲                                       ▲                    │
│                    │                                       │                    │
│  ┌─────────────────┴──────────────────┐ ┌──────────────────┴──────────────────┐ │
│  │     FirebaseTransactionRepo        │ │          RoomTransactionRepo        │ │
│  │ (Cloud Firestore Realtime Streams) │ │ (Offline SQLite via Jetpack Room)   │ │
│  └────────────────────────────────────┘ └─────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Layer-by-Layer Breakdown

### 2.1 UI Layer (`com.example.ui`)
- **Jetpack Compose & Material 3 (M3)**: Dynamic theme support (`ColorScheme`), fluid container sizing (`BoxWithConstraints`), adaptive layouts, and strict 48dp touch targets.
- **Primary Screens**:
  - `DashboardScreen.kt`: Primary financial cockpit containing Bento Grid statistics, Interactive Chart visualizations, Complete Ledger list, Category Manager, and Recurring Rules.
  - `ForecastIncomeSection.kt`: Future revenue pipeline visualizer, status chips (*Confirmed / Expected / Tentative / Realized*), milestone checklists, and brainstorming notes.
  - `ReportsScreen.kt`: Comprehensive reports hub incorporating `AuditReportView`, `AnalyticsReportView`, `LedgerReportView`, and `CsvExportSheet`.
  - `LoginScreen.kt`: Google Sign-In sheet with fallback Guest/Offline entry.
  - `BiometricLockScreen.kt`: Security barrier presented on app resume or manual lock requesting biometric verification.
- **Components & Overlays**:
  - `TransactionSearchOverlay.kt`: Full-screen animated search overlay supporting date presets, custom ranges, category filter chips, text query matching, and live financial totals.
  - `AuditReportSheet.kt` & `AuditPdfExporter.kt`: On-demand audit bottom sheet with PDF generation and printing.
  - `BackupRestoreSheet.kt`: Snapshot management allowing local JSON export/import and cloud sync.
- **Testability**: All major components feature explicit `Modifier.testTag(...)` attributes for automated JVM Robolectric and Roborazzi UI tests.

### 2.2 ViewModel Layer (`com.example.ui.viewmodel`)
- **`ExpenseViewModel`**:
  - Exposes immutable `StateFlow<T>` streams collected with lifecycle safety in Compose.
  - Computes dynamic aggregations: `monthlySummary`, `categoryTotals`, `dailySpendingTrend`, and `forecastSummary`.
  - Manages automated recurrence checks: evaluates `RecurringTransaction.lastLoggedDate` against current timestamp and automatically logs due transactions into the ledger.
  - Manages duplicate detection integration with `DuplicateTransactionDetector` for form validation and ledger badging.
  - Controls local backup generation, file restoration, and cloud snapshots via `BackupManager`.
  - Tracks deleted transactions into the audit trail table `audit_deleted_items` for historical verification.

### 2.3 Domain Models (`com.example.data.Models`)
The data contract comprises pure Kotlin data classes equipped with zero-argument constructors for Firestore deserialization:

```kotlin
// Core financial record
data class Transaction(
    val id: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val type: String = "EXPENSE", // "EXPENSE" | "INCOME"
    val description: String = "",
    val date: Long = 0L,
    val recurringId: String = "",
    val paid: Boolean = false
)

// Recurring automation rule
data class RecurringTransaction(
    val id: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val type: String = "EXPENSE",
    val description: String = "",
    val frequency: String = "MONTHLY", // "DAILY" | "WEEKLY" | "MONTHLY" | "YEARLY"
    val startDate: Long = 0L,
    val lastLoggedDate: Long = 0L
)

// Future income pipeline projection
data class ForecastIncome(
    val id: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val expectedDate: Long = 0L,
    val category: String = "Other",
    val status: String = "EXPECTED", // "CONFIRMED" | "EXPECTED" | "TENTATIVE"
    val notes: String = "",
    val bulletPoints: List<String> = emptyList(),
    val completedBullets: List<Int> = emptyList(),
    val isRealized: Boolean = false,
    val userEmail: String = "",
    val createdAt: Long = 0L
)

// Bulleted notes & scratchpad ideas
data class FutureIncomeNote(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val bulletPoints: List<String> = emptyList(),
    val completedBullets: List<Int> = emptyList(),
    val userEmail: String = "",
    val colorTag: String = "#6750A4",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

// Audit log entry for deleted items
data class AuditDeletedItem(
    val id: String = "",
    val transactionId: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val type: String = "EXPENSE",
    val description: String = "",
    val originalDate: Long = 0L,
    val deletedAt: Long = 0L,
    val userEmail: String = ""
)
```

---

## 3. Data Persistence & Synchronization

### 3.1 Dual-Repository Strategy
Ledgerly implements the **Repository Pattern** via `TransactionRepository`:

```kotlin
interface TransactionRepository {
    fun getTransactions(userEmail: String): Flow<List<Transaction>>
    suspend fun addTransaction(userEmail: String, transaction: Transaction): Result<Unit>
    suspend fun deleteTransaction(userEmail: String, id: String): Result<Unit>
    
    fun getRecurringTransactions(userEmail: String): Flow<List<RecurringTransaction>>
    suspend fun addRecurringTransaction(userEmail: String, recurring: RecurringTransaction): Result<Unit>
    suspend fun deleteRecurringTransaction(userEmail: String, id: String): Result<Unit>
    
    fun getForecastIncomes(userEmail: String): Flow<List<ForecastIncome>>
    suspend fun addForecastIncome(userEmail: String, forecast: ForecastIncome): Result<Unit>
    suspend fun deleteForecastIncome(userEmail: String, id: String): Result<Unit>
    
    fun getFutureIncomeNotes(userEmail: String): Flow<List<FutureIncomeNote>>
    suspend fun addFutureIncomeNote(userEmail: String, note: FutureIncomeNote): Result<Unit>
    suspend fun deleteFutureIncomeNote(userEmail: String, id: String): Result<Unit>
}
```

- **`RoomTransactionRepository`**: Backed by Android Jetpack Room with SQLite DAOs (`LocalTransactionDao`), `Converters` for JSON list serialization, and Room-generated Kotlin Flows.
- **`FirebaseTransactionRepository`**: Backed by Google Cloud Firestore with real-time reactive `SnapshotListener` collection streams.

### 3.2 Backup & Snapshot Subsystem (`BackupManager`)
- **JSON Serialization**: Full application state is compiled into portable, versioned JSON snapshots (`BackupData`).
- **Storage Isolation**: Local backup snapshots are archived to the app-specific documents directory with timestamp metadata (`backup_YYYYMMDD_HHmmss.json`).
- **Cloud Snapshots**: Snapshot data can optionally be committed to the user's private Firestore document under `users/{email}/backups/latest`.

---

## 4. Artificial Intelligence & NLP Pipeline

### 4.1 `GeminiParser`
- Integrates Google's `gemini-2.5-flash` model using HTTPS REST over OkHttp.
- Employs strict system instructions and `responseMimeType: application/json` with a low temperature (`0.1`) to guarantee deterministic JSON output.
- Injects `BuildConfig.GEMINI_API_KEY` securely configured via Gradle and `.env` secrets.

---

## 5. Duplicate Detection & Prevention Engine

### 5.1 `DuplicateTransactionDetector`
- Centralized utility comparing transaction amount, trimmed category (case-insensitive), and calendar day (Year and Day of Year).
- Provides proactive checks in `ManualAddForm` prior to database writes.
- Computes global duplicate sets (`findAllDuplicateIds`) to power UI badges in ledger lists and search results with zero performance overhead.

---

## 6. Security & Privacy Safeguards

1. **Zero Hardcoded Secrets**: All API keys and credentials are abstracted into `BuildConfig` generated from `.env`.
2. **On-Device Biometric Lock**: Uses AndroidX `BiometricPrompt` with biometric hardware isolation.
3. **User Data Isolation**: Firestore documents and Room entities are strictly partitioned by `userEmail`.
4. **Audit Trail Accountability**: Retains an un-editable historical log of all deleted transactions.
5. **Graceful Offline Fallback**: In the absence of network connectivity or API tokens, the application retains full local capability without crashing.
