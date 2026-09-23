# Product & Engineering Roadmap - Ledgerly

This roadmap outlines the evolution of **Ledgerly**, detailing completed milestones through production release **v88.0**, current deliverables, and strategic future initiatives.

---

## 🎯 Release Milestones

```
  v1.0 - v34.0         v35.0 - v70.0        v71.0 - v88.0 (Current)        v89.0+ (Upcoming)
 ──────────────       ───────────────      ─────────────────────────      ───────────────────
  Core MVP, Gemini     Biometrics Lock,     Duplicate Detection,           Gemini Vision OCR,
  Parsing, Forecast    Audit Trails, PDF    Search Overlay, Net Balance    Multi-Currency FX,
  & Milestone Engine   & CSV Reports        Balance Formula, v88.0 Release Glance Widgets
```

---

## 📋 Detailed Phases

### Phase 1: Core Foundation & AI Intelligence *(Completed - v1.0 - v10.0)*
- [x] **Natural Language AI Parsing**: Integrated Gemini 2.5 Flash model for natural language transaction extraction.
- [x] **Offline-First Room SQLite Storage**: Complete local storage and DAO operations for zero-latency execution.
- [x] **Cloud Sync via Firebase Firestore**: Real-time multi-device synchronization with Firestore snapshot listeners.
- [x] **Google Identity Authentication**: Seamless authentication using Jetpack Credential Manager with Guest mode fallback.
- [x] **Material 3 Dynamic Theming**: Expressive design system with customizable primary and secondary color accents.

### Phase 2: Power Tools, Automations & Analytics *(Completed - v11.0 - v25.0)*
- [x] **Complete Financial Ledger**: Dedicated ledger sheet with multi-criteria search, date filters, and custom ranges.
- [x] **Recurring Automations Engine**: Automatic logging for Daily, Weekly, Monthly, and Yearly bills/subscriptions.
- [x] **Reconciliation & Status Flags**: Paid / Unpaid toggle for tracking outstanding debts and pending receipts.
- [x] **Custom Categories & Budgets**: User-created spending categories with monthly budget tracking and limit warnings.
- [x] **Interactive Visualizations**: Category breakdown graphs and daily cash flow trend charts.

### Phase 3: Future Forecasting & Milestone Planner *(Completed - v26.0 - v34.0)*
- [x] **Future Income Pipeline**: Projections for invoices, freelance retainers, and bonuses with confidence ratings (*Confirmed / Expected / Tentative*).
- [x] **One-Tap Realization**: Direct conversion of forecasted items into active ledger income entries upon receipt.
- [x] **Interactive Milestones**: Bulleted checklists attached to forecast cards with strikethrough completion states.
- [x] **Brainstorming Notes & Scratchpad**: Color-tagged income notes with customizable checklist items.

### Phase 4: Biometric Security & Vault Shield *(Completed - v35.0 - v50.0)*
- [x] **Biometric Authentication**: Fingerprint and face unlock integration via AndroidX `BiometricPrompt`.
- [x] **Configurable Auto-Lock**: Automatic security barrier presented on app backgrounding / resume.
- [x] **Manual Vault Lock**: One-tap quick lock option with clear status indicators in user settings.
- [x] **Privacy Protection**: Data obfuscation when device lock is active.

### Phase 5: Auditing, PDF & CSV Financial Reporting *(Completed - v51.0 - v70.0)*
- [x] **Comprehensive Audit Trail**: Dedicated tracking of all deleted transactions in Room database.
- [x] **Native PDF Exporter**: Professional on-device PDF generation using Android `PdfDocument` with print and share intents.
- [x] **Advanced CSV Export Sheet**: Multi-criteria date range filtering, column configuration, and live summary totals.
- [x] **Visual Drill-Down Analysis**: Interactive breakdown by category, description, and daily spending velocity.

### Phase 6: Duplicate Detection, Search Overlay & Backups *(Completed - v71.0 - v88.0 - Current)*
- [x] **Duplicate Detection Engine**: Centralized algorithm detecting identical entries by description (vendor/customer), amount, category, and calendar day. If description differs, transactions are uniquely distinguished.
- [x] **Real-time Add Form Warning**: Interactive warning banner and confirmation dialog with "Add Anyway" override.
- [x] **Ledger Duplicate Badging**: Amber warning badges on conflicting ledger cards with one-tap conflict inspection and deletion.
- [x] **Ledger Review Filter**: Quick toggle to isolate and review all flagged duplicate entries.
- [x] **Search Overlay Integration**: Full-screen search overlay with date presets, keyword search, category filters, and live net balance totals.
- [x] **Snapshot Backup & Restore**: Local JSON snapshot archives, file sharing, and Google account cloud backup sync.
- [x] **Streamlined Navigation Header**: De-cluttered top app bar by consolidating Custom Categories into the primary Settings sheet.
- [x] **Net Balance Formula Refinement**: Corrected Net Balance formula across Ledger screen, CSV export, and PDF statements to calculate `Total Income - Total Expenses` strictly without adding Cash on Hand.
- [x] **Target SDK 36 Alignment & Version 88.0 Production Readiness**.

---

## 🚀 Near-Term Priorities (v86.0 - v95.0)

### 1. Vision & Multimodal OCR Receipt Scanning
- Utilize **Gemini 2.5 Flash Vision** to scan paper receipts and invoice photos.
- Auto-extract merchant, total amount, taxes, date, and line-item categories directly from camera or photo picker.

### 2. Multi-Currency Support & Live FX Conversion
- Support multiple world currencies (USD, EUR, GBP, JPY, CAD, AUD, INR, etc.) with real-time exchange rates.
- Automatic currency conversion for international transactions and foreign income forecasts.

### 3. Home Screen & Lock Screen Glance Widgets
- Jetpack Glance Compose widgets:
  - **Quick Add Widget**: One-tap AI voice/text logging shortcut.
  - **Budget Meter Widget**: Real-time monthly spending progress bar.
  - **Upcoming Income Widget**: Next expected forecast payment reminder.

---

## 🔮 Mid-Term Innovations (v96.0 - v110.0)

### 1. Proactive AI Financial Advisor & Anomaly Detection
- Gemini-powered automated weekly insights:
  - *"You spent 24% more on Dining Out this week compared to your 3-month average."*
  - *"Your utility bill increased by $12.00 compared to last month."*
  - *"Based on your expected freelance pipeline, you are on track to exceed your savings goal by 15%."*

### 2. Split Bills & Household Shared Ledgers
- Shared ledgers for couples, roommates, and small project teams.
- Automated balance calculations and debt settlement summaries.

---

## 💬 Community & Feedback

Have ideas, feature requests, or suggestions? Submit them via our feedback channel or open an issue in the project tracker.
