# System Architecture & Technical Design - Ledgerly

This document details the architectural principles, component structures, data models, state flows, and integration paradigms utilized within **Ledgerly**.

---

## 1. Architectural Philosophy

Ledgerly is designed adhering strictly to **Modern Android Architecture Guidelines**:
- **Reactive Unidirectional Data Flow (UDF)**: UI states flow downwards from ViewModel to Composable views; user intent events flow upwards from UI to ViewModel.
- **Offline-First Resilience**: Full local persistence via **Jetpack Room (SQLite)** ensures instant responsiveness with zero network latency, with seamless bidirectional synchronization to **Firebase Firestore**.
- **Separation of Concerns**: High cohesion and low coupling across UI, Domain, and Data layers.
- **Type Safety**: Strictly typed Kotlin Coroutines, Kotlin Flows, and Jetpack Compose state primitives.

```
┌─────────────────────────────────────────────────────────────┐
│                       UI Layer (Compose)                    │
│   DashboardScreen  │  ForecastIncomeSection  │  LoginScreen │
└──────────────────────────────┬──────────────────────────────┘
                               │ User Actions / UI State
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                    ViewModel Layer                          │
│                   ExpenseViewModel                          │
│   - Pipeline Summaries       - StateFlow Reductions         │
│   - Monthly Budget Rules     - Recurring Series Engine      │
└──────────────┬───────────────────────────────┬──────────────┘
               │                               │
               ▼                               ▼
┌──────────────────────────────┐ ┌────────────────────────────┐
│      AI Parsing Layer        │ │   Repository Layer (Auth)  │
│        GeminiParser          │ │       AuthRepository       │
│  - Gemini 2.5 Flash Engine   │ │  - Google Credential Mgr   │
└──────────────────────────────┘ └─────────────┬──────────────┘
                                               │
                                               ▼
┌─────────────────────────────────────────────────────────────┐
│           Repository Layer (Data Persistence)               │
│               TransactionRepository (Interface)             │
│            ▲                                     ▲          │
│            │                                     │          │
│  ┌─────────┴───────────────┐           ┌─────────┴────────┐ │
│  │ FirebaseTransactionRepo │           │  RoomTransaction │ │
│  │ (Cloud Firestore Sync)  │           │  Repo (SQLite)   │ │
│  └─────────────────────────┘           └──────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. Layer-by-Layer Breakdown

### 2.1 UI Layer (`com.example.ui`)
- **Jetpack Compose & Material 3 (M3)**: Dynamic theme support (`ColorScheme`), fluid container sizing (`BoxWithConstraints`), adaptive layouts, and strict 48dp touch targets.
- **Screen Architecture**:
  - `DashboardScreen.kt`: Primary financial cockpit containing the Bento Grid statistics, Interactive Chart visualizations, Complete Ledger bottom sheet, Category Manager, and Recurring Rules.
  - `ForecastIncomeSection.kt`: Future revenue pipeline visualizer, status chips (*Confirmed / Expected / Tentative / Realized*), milestone checklists, and brainstorming notes.
  - `LoginScreen.kt`: Google Sign-In sheet with fallback Guest/Offline entry.
- **Testability**: All major components feature explicit `Modifier.testTag(...)` attributes for automated JVM Robolectric and Roborazzi UI tests.

### 2.2 ViewModel Layer (`com.example.ui.viewmodel`)
- **`ExpenseViewModel`**:
  - Exposes immutable `StateFlow<T>` streams collected with lifecycle safety in Compose.
  - Computes dynamic aggregations: `monthlySummary`, `categoryTotals`, `dailySpendingTrend`, and `forecastSummary`.
  - Manages automated recurrence checks: evaluates `RecurringTransaction.lastLoggedDate` against current timestamp and automatically logs due transactions into the ledger.

### 2.3 Domain Models (`com.example.data.Models`)
The data contract comprises pure Kotlin data classes equipped with zero-argument constructors for Firestore deserialization:

```kotlin
// Core financial record
data class Transaction(
    val id: String,
    val amount: Double,
    val category: String,
    val type: String, // "EXPENSE" | "INCOME"
    val description: String,
    val date: Long,
    val recurringId: String,
    val paid: Boolean
)

// Recurring automation rule
data class RecurringTransaction(
    val id: String,
    val amount: Double,
    val category: String,
    val type: String,
    val description: String,
    val frequency: String, // "DAILY" | "WEEKLY" | "MONTHLY" | "YEARLY"
    val startDate: Long,
    val lastLoggedDate: Long
)

// Future income pipeline projection
data class ForecastIncome(
    val id: String,
    val title: String,
    val amount: Double,
    val expectedDate: Long,
    val category: String,
    val status: String, // "CONFIRMED" | "EXPECTED" | "TENTATIVE"
    val notes: String,
    val bulletPoints: List<String>,
    val completedBullets: List<Int>,
    val isRealized: Boolean,
    val userEmail: String,
    val createdAt: Long
)

// Bulleted notes & scratchpad ideas
data class FutureIncomeNote(
    val id: String,
    val title: String,
    val content: String,
    val bulletPoints: List<String>,
    val completedBullets: List<Int>,
    val userEmail: String,
    val colorTag: String,
    val createdAt: Long,
    val updatedAt: Long
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

---

## 4. Artificial Intelligence & NLP Pipeline

### 4.1 `GeminiParser`
- Integrates Google's `gemini-2.5-flash` model using HTTPS REST over OkHttp.
- Employs strict system instructions and `responseMimeType: application/json` with a low temperature (`0.1`) to guarantee deterministic JSON output.
- Injects `BuildConfig.GEMINI_API_KEY` securely configured via Gradle and `.env` secrets.

---

## 5. Security & Privacy Safeguards
1. **Zero Hardcoded Secrets**: All API keys and credentials are abstracted into `BuildConfig` generated from `.env`.
2. **User Data Isolation**: Firestore documents and Room entities are strictly partitioned by `userEmail`.
3. **Graceful Offline Fallback**: In the absence of network connectivity or API tokens, the application retains full local capability without crashing.
