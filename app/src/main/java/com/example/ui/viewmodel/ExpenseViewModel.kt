package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepositoryFactory.create(application)
    private val transactionRepository = TransactionRepositoryFactory.create(application)

    // Auth State
    val currentUserEmail = authRepository.currentUserEmail
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isUserLoggedIn = authRepository.isUserLoggedIn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isFirebaseMode = authRepository.isFirebaseMode

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading = _isAuthLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError = _authError.asStateFlow()

    // Biometric Security State
    private val securityPrefs = application.getSharedPreferences("security_prefs", Context.MODE_PRIVATE)
    private val _isBiometricEnabled = MutableStateFlow(securityPrefs.getBoolean("biometric_enabled", false))
    val isBiometricEnabled = _isBiometricEnabled.asStateFlow()

    private val _isBiometricUnlocked = MutableStateFlow(false)
    val isBiometricUnlocked = _isBiometricUnlocked.asStateFlow()

    fun setBiometricEnabled(enabled: Boolean) {
        securityPrefs.edit().putBoolean("biometric_enabled", enabled).apply()
        _isBiometricEnabled.value = enabled
        if (enabled) {
            _isBiometricUnlocked.value = true
            currentUserEmail.value?.let { email ->
                securityPrefs.edit().putString("biometric_email", email).apply()
            }
        }
    }

    val biometricEmail: String?
        get() = securityPrefs.getString("biometric_email", null)

    fun recordBiometricCredentials(email: String, password: String?) {
        securityPrefs.edit().putString("biometric_email", email).apply()
        if (password != null) {
            securityPrefs.edit().putString("biometric_pwd", password).apply()
        }
    }

    fun loginWithBiometrics(onSuccess: () -> Unit = {}, onFailure: (String) -> Unit = {}) {
        val email = securityPrefs.getString("biometric_email", null)
        val password = securityPrefs.getString("biometric_pwd", null)
        if (email != null && password != null) {
            viewModelScope.launch {
                _isAuthLoading.value = true
                _authError.value = null
                authRepository.login(email.trim(), password)
                    .onSuccess {
                        _isAuthLoading.value = false
                        _isBiometricUnlocked.value = true
                        onSuccess()
                    }
                    .onFailure { exception ->
                        _isAuthLoading.value = false
                        val err = exception.message ?: "Biometric login failed."
                        _authError.value = err
                        onFailure(err)
                    }
            }
        } else if (email != null) {
            viewModelScope.launch {
                _isAuthLoading.value = true
                _authError.value = null
                authRepository.login(email.trim(), "biometric_authorized")
                    .onSuccess {
                        _isAuthLoading.value = false
                        _isBiometricUnlocked.value = true
                        onSuccess()
                    }
                    .onFailure { exception ->
                        _isAuthLoading.value = false
                        val err = exception.message ?: "Biometric login failed."
                        _authError.value = err
                        onFailure(err)
                    }
            }
        } else {
            val err = "No account linked with biometrics yet. Please sign in with email/password first."
            _authError.value = err
            onFailure(err)
        }
    }

    fun unlockWithBiometric() {
        _isBiometricUnlocked.value = true
    }

    fun lockApp() {
        _isBiometricUnlocked.value = false
    }

    // Monthly Budget State
    private val budgetPrefs = application.getSharedPreferences("budget_prefs", Context.MODE_PRIVATE)
    private val _monthlyBudget = MutableStateFlow(budgetPrefs.getFloat("limit", 2000f).toDouble())
    val monthlyBudget = _monthlyBudget.asStateFlow()

    // Theme Mode State (Light / Dark)
    private val _isDarkMode = MutableStateFlow(budgetPrefs.getBoolean("is_dark_mode", false))
    val isDarkMode = _isDarkMode.asStateFlow()

    fun toggleDarkMode() {
        val next = !_isDarkMode.value
        budgetPrefs.edit().putBoolean("is_dark_mode", next).apply()
        _isDarkMode.value = next
    }

    fun setDarkMode(enabled: Boolean) {
        budgetPrefs.edit().putBoolean("is_dark_mode", enabled).apply()
        _isDarkMode.value = enabled
    }

    // Custom Theme Accent Color State (Default signature Indigo #4F46E5 matching web overhaul)
    private val savedAccent = budgetPrefs.getString("accent_color", null)
        ?: budgetPrefs.getString("primary_color", "#4F46E5")
        ?: "#4F46E5"
    private val normalizedAccent = if (savedAccent == "#D9F99D" || savedAccent == "#FFD97D") "#4F46E5" else savedAccent

    private val _accentColor = MutableStateFlow(normalizedAccent)
    val accentColor = _accentColor.asStateFlow()

    private val _primaryColor = MutableStateFlow(normalizedAccent)
    val primaryColor = _primaryColor.asStateFlow()

    private val _secondaryColor = MutableStateFlow(normalizedAccent)
    val secondaryColor = _secondaryColor.asStateFlow()

    // Transactions State
    private val _isTransactionsLoading = MutableStateFlow(false)
    val isTransactionsLoading = _isTransactionsLoading.asStateFlow()

    private val _transactionsError = MutableStateFlow<String?>(null)
    val transactionsError = _transactionsError.asStateFlow()

    // Exposed flow of transactions for the logged-in user
    val transactions: StateFlow<List<Transaction>> = currentUserEmail
        .flatMapLatest { email ->
            if (email != null) {
                _isTransactionsLoading.value = true
                transactionRepository.getTransactions(email)
                    .catch { error ->
                        _transactionsError.value = error.message
                        emit(emptyList())
                    }
                    .onEach { _isTransactionsLoading.value = false }
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customCategories: StateFlow<List<CustomCategory>> = currentUserEmail
        .flatMapLatest { email ->
            if (email != null) {
                transactionRepository.getCustomCategories(email)
                    .catch { emit(emptyList()) }
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Forecast Income State Flow
    val forecastIncomes: StateFlow<List<ForecastIncome>> = currentUserEmail
        .flatMapLatest { email ->
            if (email != null) {
                transactionRepository.getForecastIncomes(email)
                    .catch { emit(emptyList()) }
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Future Income Notes State Flow
    val futureIncomeNotes: StateFlow<List<FutureIncomeNote>> = currentUserEmail
        .flatMapLatest { email ->
            if (email != null) {
                transactionRepository.getFutureIncomeNotes(email)
                    .catch { emit(emptyList()) }
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Audit Deleted Items State Flow (Transactions, Forecasting Entries, Notes)
    val auditDeletedItems: StateFlow<List<AuditDeletedItem>> = currentUserEmail
        .flatMapLatest { email ->
            if (email != null) {
                transactionRepository.getAuditDeletedItems(email)
                    .catch { emit(emptyList()) }
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Forecast Summary Pipeline Calculation
    val forecastSummary: StateFlow<ForecastSummary> = forecastIncomes.map { list ->
        val activeList = list.filter { !it.isRealized && !it.status.equals("RECEIVED", ignoreCase = true) }
        val confirmed = activeList.filter { it.status.equals("CONFIRMED", ignoreCase = true) }.sumOf { it.amount }
        val expected = activeList.filter { it.status.equals("EXPECTED", ignoreCase = true) }.sumOf { it.amount }
        val tentative = activeList.filter { it.status.equals("TENTATIVE", ignoreCase = true) }.sumOf { it.amount }
        val total = activeList.sumOf { it.amount }
        val realizedList = list.filter { it.isRealized || it.status.equals("RECEIVED", ignoreCase = true) }

        ForecastSummary(
            totalPipeline = total,
            confirmedAmount = confirmed,
            expectedAmount = expected,
            tentativeAmount = tentative,
            activeCount = activeList.size,
            realizedCount = realizedList.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ForecastSummary(0.0, 0.0, 0.0, 0.0, 0, 0))

    // Calculations Flow for Current Month
    val monthlySummary = transactions.map { list ->
        val currentCal = Calendar.getInstance()
        val curMonth = currentCal.get(Calendar.MONTH)
        val curYear = currentCal.get(Calendar.YEAR)

        var totalIncome = 0.0
        var totalExpense = 0.0

        val currentMonthList = list.filter { tx ->
            val txCal = Calendar.getInstance().apply { timeInMillis = tx.date }
            txCal.get(Calendar.MONTH) == curMonth && txCal.get(Calendar.YEAR) == curYear
        }

        currentMonthList.forEach { tx ->
            if (tx.type == "INCOME") {
                totalIncome += tx.amount
            } else {
                totalExpense += tx.amount
            }
        }

        MonthlySummary(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            currentMonthList = currentMonthList
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthlySummary(0.0, 0.0, emptyList()))

    // Recurring Transactions State Flow
    val recurringTransactions: StateFlow<List<RecurringTransaction>> = currentUserEmail
        .flatMapLatest { email ->
            if (email != null) {
                transactionRepository.getRecurringTransactions(email)
                    .catch { emit(emptyList()) }
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            combine(currentUserEmail, recurringTransactions) { email, templates ->
                Pair(email, templates)
            }.collect { (email, templates) ->
                if (email != null && templates.isNotEmpty()) {
                    processRecurringTransactions(email, templates)
                }
            }
        }
    }

    // AI Natural Language Parsing State
    private val _promptInput = MutableStateFlow("")
    val promptInput = _promptInput.asStateFlow()

    private val _isParsing = MutableStateFlow(false)
    val isParsing = _isParsing.asStateFlow()

    private val _parseError = MutableStateFlow<String?>(null)
    val parseError = _parseError.asStateFlow()

    private val _parseSuccessMessage = MutableStateFlow<String?>(null)
    val parseSuccessMessage = _parseSuccessMessage.asStateFlow()

    fun updatePromptInput(input: String) {
        _promptInput.value = input
    }

    fun clearParseMessages() {
        _parseError.value = null
        _parseSuccessMessage.value = null
    }

    // Auth Operations
    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authError.value = "Email and Password cannot be blank."
            return
        }
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            authRepository.login(email.trim(), password)
                .onSuccess {
                    recordBiometricCredentials(email.trim(), password)
                    _isBiometricUnlocked.value = true
                    _isAuthLoading.value = false
                }
                .onFailure { exception ->
                    _isAuthLoading.value = false
                    _authError.value = exception.message ?: "Login failed."
                }
        }
    }

    fun signup(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authError.value = "Email and Password cannot be blank."
            return
        }
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            authRepository.signup(email.trim(), password)
                .onSuccess {
                    recordBiometricCredentials(email.trim(), password)
                    _isBiometricUnlocked.value = true
                    _isAuthLoading.value = false
                }
                .onFailure { exception ->
                    _isAuthLoading.value = false
                    _authError.value = exception.message ?: "Sign up failed."
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _isBiometricUnlocked.value = false
            authRepository.logout()
        }
    }

    fun loginWithGoogle(idToken: String, email: String, onFailure: (String) -> Unit = {}) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            authRepository.loginWithGoogle(idToken, email)
                .onSuccess {
                    _isAuthLoading.value = false
                }
                .onFailure { exception ->
                    _isAuthLoading.value = false
                    val err = exception.message ?: "Google Sign-In failed."
                    _authError.value = err
                    onFailure(err)
                }
        }
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun clearTransactionsError() {
        _transactionsError.value = null
    }

    fun isDuplicateTransaction(newTx: Transaction): Boolean {
        val list = transactions.value
        val newCal = Calendar.getInstance().apply { timeInMillis = newTx.date }
        return list.any { existing ->
            if (existing.id == newTx.id) return@any false

            val existCal = Calendar.getInstance().apply { timeInMillis = existing.date }
            val sameDay = existCal.get(Calendar.YEAR) == newCal.get(Calendar.YEAR) &&
                    existCal.get(Calendar.DAY_OF_YEAR) == newCal.get(Calendar.DAY_OF_YEAR)

            val sameDetails = existing.amount == newTx.amount &&
                    existing.category.trim().equals(newTx.category.trim(), ignoreCase = true) &&
                    existing.type == newTx.type &&
                    existing.description.trim().equals(newTx.description.trim(), ignoreCase = true)

            val sameRecurringDay = newTx.recurringId.isNotEmpty() &&
                    existing.recurringId == newTx.recurringId &&
                    sameDay

            sameRecurringDay || (sameDetails && sameDay)
        }
    }

    // Transaction Operations
    fun addTransaction(
        amount: Double,
        category: String,
        type: String,
        description: String,
        date: Long = System.currentTimeMillis(),
        id: String = "",
        recurringId: String = "",
        paid: Boolean = false
    ) {
        val email = currentUserEmail.value ?: return
        val newTx = Transaction(
            id = id,
            amount = amount,
            category = category.trim(),
            type = type,
            description = description.trim(),
            date = date,
            recurringId = recurringId,
            paid = paid
        )
        if (isDuplicateTransaction(newTx)) {
            _transactionsError.value = "This ledger item already exists. Please edit the existing ledger item from the series as necessary."
            return
        }
        viewModelScope.launch {
            transactionRepository.addTransaction(email, newTx)
                .onFailure { error ->
                    _transactionsError.value = "Failed to add transaction: ${error.message}"
                }
        }
    }

    fun toggleTransactionPaid(id: String) {
        val email = currentUserEmail.value ?: return
        viewModelScope.launch {
            val transaction = transactions.value.find { it.id == id } ?: return@launch
            val updatedTx = transaction.copy(paid = !transaction.paid)
            transactionRepository.addTransaction(email, updatedTx)
                .onFailure { error ->
                    _transactionsError.value = "Failed to update transaction status: ${error.message}"
                }
        }
    }

    fun deleteTransaction(id: String, source: String = "User Action") {
        val email = currentUserEmail.value ?: return
        viewModelScope.launch {
            val existingTx = transactions.value.find { it.id == id }
            if (existingTx != null) {
                val payload = try {
                    JSONObject().apply {
                        put("id", existingTx.id)
                        put("amount", existingTx.amount)
                        put("category", existingTx.category)
                        put("description", existingTx.description)
                        put("date", existingTx.date)
                        put("type", existingTx.type)
                        put("paid", existingTx.paid)
                        put("recurringId", existingTx.recurringId ?: "")
                    }.toString()
                } catch (e: Exception) { "" }

                val auditItem = AuditDeletedItem(
                    id = java.util.UUID.randomUUID().toString(),
                    originalId = existingTx.id,
                    itemType = "TRANSACTION",
                    title = existingTx.description.ifBlank { "${existingTx.category} Transaction" },
                    amount = existingTx.amount,
                    categoryOrStatus = "${existingTx.type} • ${existingTx.category}",
                    details = "Type: ${existingTx.type}, Category: ${existingTx.category}, Paid: ${existingTx.paid}",
                    sourceOrDeletedBy = source,
                    deletedAt = System.currentTimeMillis(),
                    originalDate = existingTx.date,
                    userEmail = email,
                    payloadJson = payload
                )
                transactionRepository.recordAuditDeletedItem(email, auditItem)
            }
            transactionRepository.deleteTransaction(email, id)
                .onFailure { error ->
                    _transactionsError.value = "Failed to delete transaction: ${error.message}"
                }
        }
    }

    fun addRecurringTransaction(
        amount: Double,
        category: String,
        type: String,
        description: String,
        frequency: String,
        startDate: Long,
        id: String = "",
        lastLoggedDate: Long = 0L,
        numInstances: Int = 12
    ) {
        val email = currentUserEmail.value ?: return
        viewModelScope.launch {
            val ruleId = id.ifEmpty { java.util.UUID.randomUUID().toString() }
            val recurring = RecurringTransaction(
                id = ruleId,
                amount = amount,
                category = category.trim(),
                type = type,
                description = description.trim(),
                frequency = frequency,
                startDate = startDate,
                lastLoggedDate = lastLoggedDate
            )
            transactionRepository.addRecurringTransaction(email, recurring)

            // If we are editing an existing series, remove old instances first
            if (id.isNotEmpty()) {
                val existingTxs = transactions.value.filter { it.recurringId == ruleId }
                existingTxs.forEach { tx ->
                    transactionRepository.deleteTransaction(email, tx.id)
                }
            }

            // Generate transactions for the series
            val cal = Calendar.getInstance()
            cal.timeInMillis = startDate

            var loggedCount = 0
            var lastDateLogged = 0L

            for (i in 0 until numInstances) {
                if (i > 0) {
                    when (frequency.uppercase()) {
                        "DAILY" -> cal.add(Calendar.DAY_OF_YEAR, 1)
                        "WEEKLY" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                        "MONTHLY" -> cal.add(Calendar.MONTH, 1)
                        "YEARLY" -> cal.add(Calendar.YEAR, 1)
                        else -> cal.add(Calendar.MONTH, 1)
                    }
                }
                val instanceDate = cal.timeInMillis
                val txDesc = description.trim() + " (Recurring)"
                val nextTx = Transaction(
                    id = java.util.UUID.randomUUID().toString(),
                    amount = amount,
                    category = category.trim(),
                    type = type,
                    description = txDesc,
                    date = instanceDate,
                    recurringId = ruleId
                )

                if (!isDuplicateTransaction(nextTx)) {
                    transactionRepository.addTransaction(email, nextTx)
                    loggedCount++
                    lastDateLogged = instanceDate
                } else {
                    _transactionsError.value = "This ledger item already exists. Please edit the existing ledger item from the series as necessary."
                }
            }

            if (loggedCount > 0) {
                val updatedRec = recurring.copy(lastLoggedDate = lastDateLogged)
                transactionRepository.addRecurringTransaction(email, updatedRec)
            }
        }
    }

    fun deleteRecurringTransaction(id: String, source: String = "User Action") {
        val email = currentUserEmail.value ?: return
        viewModelScope.launch {
            val existingRec = recurringTransactions.value.find { it.id == id }
            if (existingRec != null) {
                val payload = try {
                    JSONObject().apply {
                        put("id", existingRec.id)
                        put("amount", existingRec.amount)
                        put("category", existingRec.category)
                        put("description", existingRec.description)
                        put("frequency", existingRec.frequency)
                        put("startDate", existingRec.startDate)
                        put("type", existingRec.type)
                    }.toString()
                } catch (e: Exception) { "" }

                val auditItem = AuditDeletedItem(
                    id = java.util.UUID.randomUUID().toString(),
                    originalId = existingRec.id,
                    itemType = "RECURRING",
                    title = existingRec.description.ifBlank { "${existingRec.category} Recurring Rule" },
                    amount = existingRec.amount,
                    categoryOrStatus = "${existingRec.frequency} • ${existingRec.category}",
                    details = "Rule Frequency: ${existingRec.frequency}, Type: ${existingRec.type}",
                    sourceOrDeletedBy = source,
                    deletedAt = System.currentTimeMillis(),
                    originalDate = existingRec.startDate,
                    userEmail = email,
                    payloadJson = payload
                )
                transactionRepository.recordAuditDeletedItem(email, auditItem)
            }
            transactionRepository.deleteRecurringTransaction(email, id)
                .onFailure { error ->
                    _transactionsError.value = "Failed to delete recurring transaction: ${error.message}"
                }
        }
    }

    fun deleteRecurringSeries(recurringId: String, source: String = "User Action (Series Purge)") {
        val email = currentUserEmail.value ?: return
        viewModelScope.launch {
            val existingRec = recurringTransactions.value.find { it.id == recurringId }
            if (existingRec != null) {
                val payload = try {
                    JSONObject().apply {
                        put("id", existingRec.id)
                        put("amount", existingRec.amount)
                        put("category", existingRec.category)
                        put("description", existingRec.description)
                        put("frequency", existingRec.frequency)
                        put("startDate", existingRec.startDate)
                        put("type", existingRec.type)
                    }.toString()
                } catch (e: Exception) { "" }

                val auditItem = AuditDeletedItem(
                    id = java.util.UUID.randomUUID().toString(),
                    originalId = existingRec.id,
                    itemType = "RECURRING",
                    title = existingRec.description.ifBlank { "${existingRec.category} Recurring Series" },
                    amount = existingRec.amount,
                    categoryOrStatus = "${existingRec.frequency} • ${existingRec.category}",
                    details = "Series deleted along with generated instances",
                    sourceOrDeletedBy = source,
                    deletedAt = System.currentTimeMillis(),
                    originalDate = existingRec.startDate,
                    userEmail = email,
                    payloadJson = payload
                )
                transactionRepository.recordAuditDeletedItem(email, auditItem)
            }
            transactionRepository.deleteRecurringTransaction(email, recurringId)
                .onFailure { error ->
                    _transactionsError.value = "Failed to delete recurring transaction: ${error.message}"
                }
            val seriesTxs = transactions.value.filter { it.recurringId == recurringId }
            seriesTxs.forEach { tx ->
                deleteTransaction(tx.id, source = "Series Cleanup ($recurringId)")
            }
        }
    }

    fun addCustomCategory(
        name: String,
        iconName: String = "category",
        colorHex: String = "#00897B",
        type: String = "EXPENSE"
    ) {
        val email = currentUserEmail.value ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            val newCat = CustomCategory(
                id = java.util.UUID.randomUUID().toString(),
                name = name.trim(),
                userEmail = email,
                iconName = iconName,
                colorHex = colorHex,
                type = type
            )
            transactionRepository.addCustomCategory(email, newCat)
        }
    }

    fun updateCustomCategory(
        id: String,
        newName: String,
        iconName: String = "category",
        colorHex: String = "#00897B",
        type: String = "EXPENSE"
    ) {
        val email = currentUserEmail.value ?: return
        if (newName.isBlank()) return
        viewModelScope.launch {
            val updatedCat = CustomCategory(
                id = id,
                name = newName.trim(),
                userEmail = email,
                iconName = iconName,
                colorHex = colorHex,
                type = type
            )
            transactionRepository.addCustomCategory(email, updatedCat)
        }
    }

    fun deleteCustomCategory(id: String) {
        val email = currentUserEmail.value ?: return
        viewModelScope.launch {
            transactionRepository.deleteCustomCategory(email, id)
        }
    }

    private var isProcessingRecurring = false

    private fun processRecurringTransactions(userEmail: String, templates: List<RecurringTransaction>) {
        if (isProcessingRecurring) return
        isProcessingRecurring = true
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                for (template in templates) {
                    var lastLogged = template.lastLoggedDate
                    val startDate = template.startDate
                    
                    var checkTime = if (lastLogged == 0L) startDate else lastLogged
                    if (checkTime > now) continue

                    val cal = Calendar.getInstance()
                    cal.timeInMillis = checkTime

                    var updatedLastLogged = lastLogged
                    var anyLogged = false

                    if (lastLogged == 0L) {
                        val firstTx = Transaction(
                            id = java.util.UUID.randomUUID().toString(),
                            amount = template.amount,
                            category = template.category,
                            type = template.type,
                            description = template.description + " (Recurring)",
                            date = startDate
                        )
                        transactionRepository.addTransaction(userEmail, firstTx)
                        updatedLastLogged = startDate
                        anyLogged = true
                    }

                    while (true) {
                        when (template.frequency.uppercase()) {
                            "DAILY" -> cal.add(Calendar.DAY_OF_YEAR, 1)
                            "WEEKLY" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                            "MONTHLY" -> cal.add(Calendar.MONTH, 1)
                            "YEARLY" -> cal.add(Calendar.YEAR, 1)
                            else -> cal.add(Calendar.MONTH, 1)
                        }
                        
                        val nextTime = cal.timeInMillis
                        if (nextTime <= now) {
                            val nextTx = Transaction(
                                id = java.util.UUID.randomUUID().toString(),
                                amount = template.amount,
                                category = template.category,
                                type = template.type,
                                description = template.description + " (Recurring)",
                                date = nextTime
                            )
                            transactionRepository.addTransaction(userEmail, nextTx)
                            updatedLastLogged = nextTime
                            anyLogged = true
                        } else {
                            break
                        }
                    }

                    if (anyLogged) {
                        val updatedTemplate = template.copy(lastLoggedDate = updatedLastLogged)
                        transactionRepository.addRecurringTransaction(userEmail, updatedTemplate)
                    }
                }
            } catch (e: Exception) {
                _transactionsError.value = "Failed processing recurring transactions: ${e.message}"
            } finally {
                isProcessingRecurring = false
            }
        }
    }

    fun updateMonthlyBudget(newLimit: Double) {
        budgetPrefs.edit().putFloat("limit", newLimit.toFloat()).apply()
        _monthlyBudget.value = newLimit
    }

    fun updateAccentColor(hex: String) {
        val validHex = if (hex.startsWith("#")) hex else "#$hex"
        budgetPrefs.edit()
            .putString("accent_color", validHex)
            .putString("primary_color", validHex)
            .putString("secondary_color", validHex)
            .apply()
        _accentColor.value = validHex
        _primaryColor.value = validHex
        _secondaryColor.value = validHex
    }

    fun updatePrimaryColor(hex: String) = updateAccentColor(hex)

    fun updateSecondaryColor(hex: String) = updateAccentColor(hex)

    fun resetThemeToDefault() {
        updateAccentColor("#4F46E5")
    }

    // Parse with Gemini
    fun parseAndAddTransaction() {
        val text = _promptInput.value
        if (text.isBlank()) return

        val email = currentUserEmail.value ?: return

        viewModelScope.launch {
            _isParsing.value = true
            _parseError.value = null
            _parseSuccessMessage.value = null

            GeminiParser.parseTransaction(text)
                .onSuccess { parsedTx ->
                    transactionRepository.addTransaction(email, parsedTx)
                        .onSuccess {
                            _promptInput.value = ""
                            val sign = if (parsedTx.type == "INCOME") "+" else "-"
                            _parseSuccessMessage.value = "Added: ${parsedTx.description} (${parsedTx.category}) $sign$${String.format("%.2f", parsedTx.amount)}"
                        }
                        .onFailure { error ->
                            _parseError.value = "Transaction parsed but save failed: ${error.message}"
                        }
                    _isParsing.value = false
                }
                .onFailure { error ->
                    _isParsing.value = false
                    _parseError.value = error.message ?: "Failed to parse input with Gemini AI."
                }
        }
    }
    // Forecast Income Operations
    fun addOrUpdateForecastIncome(
        title: String,
        amount: Double,
        expectedDate: Long,
        category: String,
        status: String,
        notes: String,
        bulletPoints: List<String>,
        id: String = "",
        completedBullets: List<Int> = emptyList(),
        isRealized: Boolean = false,
        colorTag: String = "#FFD97D"
    ) {
        val email = currentUserEmail.value ?: return
        if (title.isBlank()) return
        viewModelScope.launch {
            val isItemRealized = if (status.equals("RECEIVED", ignoreCase = true)) {
                true
            } else if (status.equals("CONFIRMED", ignoreCase = true) || status.equals("EXPECTED", ignoreCase = true) || status.equals("TENTATIVE", ignoreCase = true)) {
                false
            } else {
                isRealized
            }
            val forecast = ForecastIncome(
                id = id.ifEmpty { java.util.UUID.randomUUID().toString() },
                title = title.trim(),
                amount = amount,
                expectedDate = expectedDate,
                category = category.trim(),
                status = status,
                notes = notes.trim(),
                bulletPoints = bulletPoints.filter { it.isNotBlank() }.map { it.trim() },
                completedBullets = completedBullets,
                isRealized = isItemRealized,
                userEmail = email,
                colorTag = colorTag.ifBlank { "#FFD97D" }
            )
            transactionRepository.addForecastIncome(email, forecast)
        }
    }

    fun deleteForecastIncome(id: String, source: String = "User Action") {
        val email = currentUserEmail.value ?: return
        viewModelScope.launch {
            val existingForecast = forecastIncomes.value.find { it.id == id }
            if (existingForecast != null) {
                val payload = try {
                    JSONObject().apply {
                        put("id", existingForecast.id)
                        put("title", existingForecast.title)
                        put("amount", existingForecast.amount)
                        put("expectedDate", existingForecast.expectedDate)
                        put("category", existingForecast.category)
                        put("notes", existingForecast.notes)
                        put("status", existingForecast.status)
                        put("bulletPoints", JSONArray(existingForecast.bulletPoints))
                        put("completedBullets", JSONArray(existingForecast.completedBullets))
                        put("isRealized", existingForecast.isRealized)
                        put("colorTag", existingForecast.colorTag)
                    }.toString()
                } catch (e: Exception) { "" }

                val auditItem = AuditDeletedItem(
                    id = java.util.UUID.randomUUID().toString(),
                    originalId = existingForecast.id,
                    itemType = "FORECAST",
                    title = existingForecast.title.ifBlank { "Forecast Entry" },
                    amount = existingForecast.amount,
                    categoryOrStatus = "${existingForecast.status} • ${existingForecast.category}",
                    details = "Notes: ${existingForecast.notes.ifBlank { "None" }}, Realized: ${existingForecast.isRealized}",
                    sourceOrDeletedBy = source,
                    deletedAt = System.currentTimeMillis(),
                    originalDate = existingForecast.expectedDate,
                    userEmail = email,
                    payloadJson = payload
                )
                transactionRepository.recordAuditDeletedItem(email, auditItem)
            }
            transactionRepository.deleteForecastIncome(email, id)
        }
    }

    fun toggleForecastBulletCompleted(forecastId: String, bulletIndex: Int) {
        val email = currentUserEmail.value ?: return
        viewModelScope.launch {
            val item = forecastIncomes.value.find { it.id == forecastId } ?: return@launch
            val newCompleted = if (item.completedBullets.contains(bulletIndex)) {
                item.completedBullets - bulletIndex
            } else {
                item.completedBullets + bulletIndex
            }
            val updated = item.copy(completedBullets = newCompleted)
            transactionRepository.addForecastIncome(email, updated)
        }
    }

    fun convertForecastToActualIncome(forecast: ForecastIncome, markRealizedOnly: Boolean = false) {
        val email = currentUserEmail.value ?: return
        viewModelScope.launch {
            if (!markRealizedOnly) {
                // Add as actual Income transaction
                val newTx = Transaction(
                    id = java.util.UUID.randomUUID().toString(),
                    amount = forecast.amount,
                    category = forecast.category.ifBlank { "Income" },
                    type = "INCOME",
                    description = "${forecast.title} (From Forecast)",
                    date = System.currentTimeMillis(),
                    paid = true
                )
                transactionRepository.addTransaction(email, newTx)
            }

            // Update forecast status to RECEIVED and isRealized to true
            val updatedForecast = forecast.copy(isRealized = true, status = "RECEIVED")
            transactionRepository.addForecastIncome(email, updatedForecast)
        }
    }

    fun toggleForecastRealizedState(id: String) {
        val email = currentUserEmail.value ?: return
        viewModelScope.launch {
            val item = forecastIncomes.value.find { it.id == id } ?: return@launch
            val wasReceived = item.isRealized || item.status.equals("RECEIVED", ignoreCase = true)
            val newRealized = !wasReceived
            val newStatus = if (newRealized) "RECEIVED" else "CONFIRMED"
            val updated = item.copy(isRealized = newRealized, status = newStatus)
            transactionRepository.addForecastIncome(email, updated)
        }
    }

    // Future Income Notes Operations
    fun addOrUpdateFutureIncomeNote(
        title: String,
        content: String,
        bulletPoints: List<String>,
        id: String = "",
        colorTag: String = "#FFD97D",
        completedBullets: List<Int> = emptyList()
    ) {
        val email = currentUserEmail.value ?: return
        if (title.isBlank() && content.isBlank() && bulletPoints.none { it.isNotBlank() }) return
        viewModelScope.launch {
            val note = FutureIncomeNote(
                id = id.ifEmpty { java.util.UUID.randomUUID().toString() },
                title = title.trim(),
                content = content.trim(),
                bulletPoints = bulletPoints.filter { it.isNotBlank() }.map { it.trim() },
                completedBullets = completedBullets,
                userEmail = email,
                colorTag = colorTag,
                updatedAt = System.currentTimeMillis()
            )
            transactionRepository.addFutureIncomeNote(email, note)
        }
    }

    fun deleteFutureIncomeNote(id: String, source: String = "User Action") {
        val email = currentUserEmail.value ?: return
        viewModelScope.launch {
            val existingNote = futureIncomeNotes.value.find { it.id == id }
            if (existingNote != null) {
                val payload = try {
                    JSONObject().apply {
                        put("id", existingNote.id)
                        put("title", existingNote.title)
                        put("content", existingNote.content)
                        put("bulletPoints", JSONArray(existingNote.bulletPoints))
                        put("completedBullets", JSONArray(existingNote.completedBullets))
                        put("colorTag", existingNote.colorTag)
                        put("createdAt", existingNote.createdAt)
                        put("updatedAt", existingNote.updatedAt)
                    }.toString()
                } catch (e: Exception) { "" }

                val bulletPreview = if (existingNote.bulletPoints.isNotEmpty()) " (${existingNote.bulletPoints.size} bullets)" else ""
                val auditItem = AuditDeletedItem(
                    id = java.util.UUID.randomUUID().toString(),
                    originalId = existingNote.id,
                    itemType = "NOTE",
                    title = existingNote.title.ifBlank { "Future Income Note" },
                    amount = 0.0,
                    categoryOrStatus = "Income Note$bulletPreview",
                    details = existingNote.content.ifBlank { "No content description" },
                    sourceOrDeletedBy = source,
                    deletedAt = System.currentTimeMillis(),
                    originalDate = existingNote.createdAt,
                    userEmail = email,
                    payloadJson = payload
                )
                transactionRepository.recordAuditDeletedItem(email, auditItem)
            }
            transactionRepository.deleteFutureIncomeNote(email, id)
        }
    }

    fun recordExternalDeletion(
        itemType: String,
        title: String,
        amount: Double = 0.0,
        categoryOrStatus: String = "",
        details: String = "",
        sourceOrDeletedBy: String = "External Source"
    ) {
        val email = currentUserEmail.value ?: return
        viewModelScope.launch {
            val auditItem = AuditDeletedItem(
                id = java.util.UUID.randomUUID().toString(),
                originalId = "ext-${System.currentTimeMillis()}",
                itemType = itemType,
                title = title,
                amount = amount,
                categoryOrStatus = categoryOrStatus,
                details = details,
                sourceOrDeletedBy = sourceOrDeletedBy,
                deletedAt = System.currentTimeMillis(),
                originalDate = System.currentTimeMillis() - 86400000L,
                userEmail = email
            )
            transactionRepository.recordAuditDeletedItem(email, auditItem)
        }
    }

    fun clearAuditDeletedItems() {
        val email = currentUserEmail.value ?: return
        viewModelScope.launch {
            transactionRepository.clearAuditDeletedItems(email)
        }
    }

    fun restoreDeletedItem(auditItem: AuditDeletedItem, onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        val email = currentUserEmail.value ?: run {
            onComplete(false, "User email not found")
            return
        }
        viewModelScope.launch {
            try {
                var restoredName = auditItem.title.ifBlank { "Item" }
                when (auditItem.itemType.uppercase()) {
                    "TRANSACTION" -> {
                        val tx = parseTransactionFromAudit(auditItem)
                        restoredName = tx.description.ifBlank { tx.category }
                        transactionRepository.addTransaction(email, tx)
                            .onSuccess {
                                transactionRepository.deleteAuditDeletedItem(email, auditItem.id)
                                onComplete(true, "Restored \"$restoredName\" to transactions")
                            }
                            .onFailure { error ->
                                onComplete(false, "Failed to restore: ${error.message}")
                            }
                    }
                    "FORECAST" -> {
                        val forecast = parseForecastFromAudit(auditItem, email)
                        restoredName = forecast.title
                        transactionRepository.addForecastIncome(email, forecast)
                            .onSuccess {
                                transactionRepository.deleteAuditDeletedItem(email, auditItem.id)
                                onComplete(true, "Restored \"$restoredName\" to forecast income")
                            }
                            .onFailure { error ->
                                onComplete(false, "Failed to restore: ${error.message}")
                            }
                    }
                    "RECURRING" -> {
                        val rec = parseRecurringFromAudit(auditItem)
                        restoredName = rec.description.ifBlank { rec.category }
                        transactionRepository.addRecurringTransaction(email, rec)
                            .onSuccess {
                                transactionRepository.deleteAuditDeletedItem(email, auditItem.id)
                                onComplete(true, "Restored \"$restoredName\" recurring rule")
                            }
                            .onFailure { error ->
                                onComplete(false, "Failed to restore: ${error.message}")
                            }
                    }
                    "NOTE" -> {
                        val note = parseNoteFromAudit(auditItem, email)
                        restoredName = note.title
                        transactionRepository.addFutureIncomeNote(email, note)
                            .onSuccess {
                                transactionRepository.deleteAuditDeletedItem(email, auditItem.id)
                                onComplete(true, "Restored \"$restoredName\" note")
                            }
                            .onFailure { error ->
                                onComplete(false, "Failed to restore: ${error.message}")
                            }
                    }
                    else -> {
                        val tx = parseTransactionFromAudit(auditItem)
                        restoredName = tx.description.ifBlank { tx.category }
                        transactionRepository.addTransaction(email, tx)
                            .onSuccess {
                                transactionRepository.deleteAuditDeletedItem(email, auditItem.id)
                                onComplete(true, "Restored \"$restoredName\"")
                            }
                            .onFailure { error ->
                                onComplete(false, "Failed to restore: ${error.message}")
                            }
                    }
                }
            } catch (e: Exception) {
                onComplete(false, "Restore error: ${e.message}")
            }
        }
    }

    private fun parseTransactionFromAudit(item: AuditDeletedItem): Transaction {
        if (item.payloadJson.isNotBlank()) {
            try {
                val json = JSONObject(item.payloadJson)
                return Transaction(
                    id = json.optString("id", if (item.originalId.isNotBlank()) item.originalId else java.util.UUID.randomUUID().toString()),
                    amount = json.optDouble("amount", item.amount),
                    category = json.optString("category", parseCategoryFromStatus(item.categoryOrStatus, "Other")),
                    description = json.optString("description", item.title),
                    date = json.optLong("date", if (item.originalDate > 0) item.originalDate else item.deletedAt),
                    type = json.optString("type", parseTypeFromStatus(item.categoryOrStatus, "EXPENSE")),
                    paid = json.optBoolean("paid", true),
                    recurringId = json.optString("recurringId", "")
                )
            } catch (_: Exception) {}
        }
        val type = parseTypeFromStatus(item.categoryOrStatus, "EXPENSE")
        val category = parseCategoryFromStatus(item.categoryOrStatus, "Other")
        return Transaction(
            id = if (item.originalId.isNotBlank()) item.originalId else java.util.UUID.randomUUID().toString(),
            amount = item.amount,
            category = category,
            description = item.title,
            date = if (item.originalDate > 0) item.originalDate else item.deletedAt,
            type = type,
            paid = true,
            recurringId = ""
        )
    }

    private fun parseForecastFromAudit(item: AuditDeletedItem, email: String): ForecastIncome {
        if (item.payloadJson.isNotBlank()) {
            try {
                val json = JSONObject(item.payloadJson)
                val bullets = mutableListOf<String>()
                json.optJSONArray("bulletPoints")?.let { arr ->
                    for (i in 0 until arr.length()) bullets.add(arr.optString(i))
                }
                val completed = mutableListOf<Int>()
                json.optJSONArray("completedBullets")?.let { arr ->
                    for (i in 0 until arr.length()) completed.add(arr.optInt(i))
                }
                return ForecastIncome(
                    id = json.optString("id", if (item.originalId.isNotBlank()) item.originalId else java.util.UUID.randomUUID().toString()),
                    title = json.optString("title", item.title.ifBlank { "Restored Forecast" }),
                    amount = json.optDouble("amount", item.amount),
                    expectedDate = json.optLong("expectedDate", if (item.originalDate > 0) item.originalDate else item.deletedAt),
                    category = json.optString("category", parseCategoryFromStatus(item.categoryOrStatus, "Sales")),
                    notes = json.optString("notes", item.details),
                    status = json.optString("status", "PENDING"),
                    bulletPoints = bullets,
                    completedBullets = completed,
                    isRealized = json.optBoolean("isRealized", false),
                    userEmail = email,
                    colorTag = json.optString("colorTag", "#4F46E5")
                )
            } catch (_: Exception) {}
        }
        return ForecastIncome(
            id = if (item.originalId.isNotBlank()) item.originalId else java.util.UUID.randomUUID().toString(),
            title = item.title.ifBlank { "Restored Forecast" },
            amount = item.amount,
            expectedDate = if (item.originalDate > 0) item.originalDate else item.deletedAt,
            category = parseCategoryFromStatus(item.categoryOrStatus, "Sales"),
            notes = item.details,
            status = "PENDING",
            bulletPoints = emptyList(),
            completedBullets = emptyList(),
            isRealized = false,
            userEmail = email,
            colorTag = "#4F46E5"
        )
    }

    private fun parseRecurringFromAudit(item: AuditDeletedItem): RecurringTransaction {
        if (item.payloadJson.isNotBlank()) {
            try {
                val json = JSONObject(item.payloadJson)
                return RecurringTransaction(
                    id = json.optString("id", if (item.originalId.isNotBlank()) item.originalId else java.util.UUID.randomUUID().toString()),
                    amount = json.optDouble("amount", item.amount),
                    category = json.optString("category", parseCategoryFromStatus(item.categoryOrStatus, "General")),
                    type = json.optString("type", parseTypeFromStatus(item.categoryOrStatus, "EXPENSE")),
                    description = json.optString("description", item.title),
                    frequency = json.optString("frequency", "MONTHLY"),
                    startDate = json.optLong("startDate", if (item.originalDate > 0) item.originalDate else item.deletedAt),
                    lastLoggedDate = json.optLong("lastLoggedDate", 0L)
                )
            } catch (_: Exception) {}
        }
        return RecurringTransaction(
            id = if (item.originalId.isNotBlank()) item.originalId else java.util.UUID.randomUUID().toString(),
            amount = item.amount,
            category = parseCategoryFromStatus(item.categoryOrStatus, "General"),
            type = parseTypeFromStatus(item.categoryOrStatus, "EXPENSE"),
            description = item.title,
            frequency = "MONTHLY",
            startDate = if (item.originalDate > 0) item.originalDate else item.deletedAt,
            lastLoggedDate = 0L
        )
    }

    private fun parseNoteFromAudit(item: AuditDeletedItem, email: String): FutureIncomeNote {
        if (item.payloadJson.isNotBlank()) {
            try {
                val json = JSONObject(item.payloadJson)
                val bullets = mutableListOf<String>()
                json.optJSONArray("bulletPoints")?.let { arr ->
                    for (i in 0 until arr.length()) bullets.add(arr.optString(i))
                }
                val completed = mutableListOf<Int>()
                json.optJSONArray("completedBullets")?.let { arr ->
                    for (i in 0 until arr.length()) completed.add(arr.optInt(i))
                }
                return FutureIncomeNote(
                    id = json.optString("id", if (item.originalId.isNotBlank()) item.originalId else java.util.UUID.randomUUID().toString()),
                    title = json.optString("title", item.title.ifBlank { "Restored Note" }),
                    content = json.optString("content", item.details),
                    bulletPoints = bullets,
                    completedBullets = completed,
                    userEmail = email,
                    colorTag = json.optString("colorTag", "#4F46E5"),
                    createdAt = json.optLong("createdAt", if (item.originalDate > 0) item.originalDate else item.deletedAt),
                    updatedAt = System.currentTimeMillis()
                )
            } catch (_: Exception) {}
        }
        return FutureIncomeNote(
            id = if (item.originalId.isNotBlank()) item.originalId else java.util.UUID.randomUUID().toString(),
            title = item.title.ifBlank { "Restored Note" },
            content = item.details,
            bulletPoints = emptyList(),
            completedBullets = emptyList(),
            userEmail = email,
            colorTag = "#4F46E5",
            createdAt = if (item.originalDate > 0) item.originalDate else item.deletedAt,
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun parseTypeFromStatus(catOrStatus: String, default: String): String {
        return if (catOrStatus.contains("•")) {
            val candidate = catOrStatus.substringBefore("•").trim().uppercase()
            if (candidate == "EXPENSE" || candidate == "INCOME") candidate else default
        } else {
            default
        }
    }

    private fun parseCategoryFromStatus(catOrStatus: String, default: String): String {
        return if (catOrStatus.contains("•")) {
            catOrStatus.substringAfter("•").trim().ifBlank { default }
        } else if (catOrStatus.isNotBlank()) {
            catOrStatus.trim()
        } else {
            default
        }
    }

    fun toggleNoteBulletCompleted(noteId: String, bulletIndex: Int) {
        val email = currentUserEmail.value ?: return
        viewModelScope.launch {
            val item = futureIncomeNotes.value.find { it.id == noteId } ?: return@launch
            val newCompleted = if (item.completedBullets.contains(bulletIndex)) {
                item.completedBullets - bulletIndex
            } else {
                item.completedBullets + bulletIndex
            }
            val updated = item.copy(completedBullets = newCompleted, updatedAt = System.currentTimeMillis())
            transactionRepository.addFutureIncomeNote(email, updated)
        }
    }
}

data class MonthlySummary(
    val totalIncome: Double,
    val totalExpense: Double,
    val currentMonthList: List<Transaction>
)

data class ForecastSummary(
    val totalPipeline: Double,
    val confirmedAmount: Double,
    val expectedAmount: Double,
    val tentativeAmount: Double,
    val activeCount: Int,
    val realizedCount: Int
)

