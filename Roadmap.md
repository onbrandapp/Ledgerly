# Product & Engineering Roadmap - Ledgerly

This roadmap outlines the evolution of **Ledgerly**, detailing completed milestones, current deliverables, and strategic future initiatives.

---

## 🎯 Release Milestones

```
  v1.0 - v10.0         v11.0 - v25.0        v26.0 - v34.0 (Current)        v35.0+ (Upcoming)
 ──────────────       ───────────────      ─────────────────────────      ───────────────────
  Core MVP &           Automations,         Future Forecasts, Notes,       OCR Receipt Scan,
  Gemini 2.5 Flash     Complete Ledger,     Milestone Checklists,          Multi-Currency,
  Parsing              Cloud & Room Sync    Version 34.0 Release           Biometrics & Widgets
```

---

## 📋 Detailed Phases

### Phase 1: Core Foundation & AI Intelligence *(Completed)*
- [x] **Natural Language AI Parsing**: Integrated Gemini 2.5 Flash model for natural language transaction extraction.
- [x] **Offline-First Room SQLite Storage**: Complete local storage and DAO operations for zero-latency execution.
- [x] **Cloud Sync via Firebase Firestore**: Real-time multi-device synchronization with Firestore snapshot listeners.
- [x] **Google Identity Authentication**: Seamless authentication using Jetpack Credential Manager with Guest mode fallback.
- [x] **Material 3 Dynamic Theming**: Expressive design system with customizable primary and secondary color accents.

### Phase 2: Power Tools, Automations & Analytics *(Completed)*
- [x] **Complete Financial Ledger**: Dedicated bottom sheet with multi-criteria search, date filters, and custom ranges.
- [x] **Recurring Automations Engine**: Automatic logging for Daily, Weekly, Monthly, and Yearly bills/subscriptions.
- [x] **Reconciliation & Status Flags**: Paid / Unpaid toggle for tracking outstanding debts and pending receipts.
- [x] **Custom Categories & Budgets**: User-created spending categories with monthly budget tracking and limit warnings.
- [x] **Interactive Visualizations**: Category breakdown graphs and daily cash flow trend charts.

### Phase 3: Future Forecasting & Milestone Planner *(Completed - v34.0)*
- [x] **Future Income Pipeline**: Projections for invoices, freelance retainers, and bonuses with confidence ratings (*Confirmed / Expected / Tentative*).
- [x] **One-Tap Realization**: Direct conversion of forecasted items into active ledger income entries upon receipt.
- [x] **Interactive Milestones**: Bulleted checklists attached to forecast cards with strikethrough completion states.
- [x] **Brainstorming Notes & Scratchpad**: Color-tagged income notes with customizable checklist items.
- [x] **Target SDK 36 Alignment & Version 34.0 Production Readiness**.

---

## 🚀 Near-Term Priorities (v35.0 - v40.0)

### 1. Vision & Multimodal OCR Receipt Scanning
- Utilize **Gemini 2.5 Flash Vision** to scan paper receipts and invoice photos.
- Auto-extract merchant, total amount, taxes, date, and line-item categories directly from the camera or gallery.

### 2. Multi-Currency Support & Live FX Conversion
- Support multiple world currencies (USD, EUR, GBP, JPY, CAD, AUD, INR, etc.) with real-time exchange rates.
- Automatic currency conversion for international transactions and foreign income forecasts.

### 3. Biometric App Lock & Privacy Shield
- Biometric authentication (Fingerprint / Face Unlock / PIN) using Android `BiometricPrompt`.
- Privacy masking option to blur financial figures in public settings or app switcher.

### 4. Home Screen & Lock Screen Widgets
- Jetpack Glance Compose widgets:
  - **Quick Add Widget**: One-tap AI voice/text logging shortcut.
  - **Budget Meter Widget**: Real-time monthly spending progress bar.
  - **Upcoming Income Widget**: Next expected forecast payment reminder.

---

## 🔮 Mid-Term Innovations (v41.0 - v50.0)

### 1. Proactive AI Financial Advisor & Anomaly Detection
- Gemini-powered automated weekly insights:
  - *"You spent 24% more on Dining Out this week compared to your 3-month average."*
  - *"Your Netflix subscription increased by $2.00 this month."*
  - *"Based on your expected freelance pipeline, you are on track to exceed your savings goal by 15%."*

### 2. Split Bills & Household Shared Ledgers
- Shared ledgers for couples, roommates, and small project teams.
- Automated balance calculations and debt settlement summaries.

### 3. Comprehensive Tax Estimator & Invoicing
- Tag deductible expenses and generate quarterly estimated tax reports.
- Export PDF invoices from realized forecast items for freelance clients.

---

## 🌐 Long-Term Vision (v51.0+)

### 1. Kotlin Multiplatform (KMP) Desktop & Web Companion
- Cross-platform synchronization across Android, Web, macOS, and Windows with unified Kotlin business logic.

### 2. Open Banking API Integration
- Optional direct bank synchronization via Plaid or Open Banking APIs for automatic balance verification.

---

## 💬 Community & Feedback

Have ideas, feature requests, or suggestions? Submit them via our feedback channel or open an issue in the project tracker.
