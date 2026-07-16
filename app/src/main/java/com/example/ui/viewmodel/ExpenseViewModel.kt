package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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

    // Monthly Budget State
    private val budgetPrefs = application.getSharedPreferences("budget_prefs", Context.MODE_PRIVATE)
    private val _monthlyBudget = MutableStateFlow(budgetPrefs.getFloat("limit", 2000f).toDouble())
    val monthlyBudget = _monthlyBudget.asStateFlow()

    // Custom Theme Colors State (Polysure Theme defaults: Primary = Light Orange, Secondary = Purple, Accent = Lime Green)
    private val _primaryColor = MutableStateFlow(budgetPrefs.getString("primary_color", "#FFD97D") ?: "#FFD97D")
    val primaryColor = _primaryColor.asStateFlow()

    private val _secondaryColor = MutableStateFlow(budgetPrefs.getString("secondary_color", "#A78BFA") ?: "#A78BFA")
    val secondaryColor = _secondaryColor.asStateFlow()

    private val _accentColor = MutableStateFlow(budgetPrefs.getString("accent_color", "#D9F99D") ?: "#D9F99D")
    val accentColor = _accentColor.asStateFlow()

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

    fun deleteTransaction(id: String) {
        val email = currentUserEmail.value ?: return
        viewModelScope.launch {
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

    fun deleteRecurringTransaction(id: String) {
        val email = currentUserEmail.value ?: return
        viewModelScope.launch {
            transactionRepository.deleteRecurringTransaction(email, id)
                .onFailure { error ->
                    _transactionsError.value = "Failed to delete recurring transaction: ${error.message}"
                }
        }
    }

    fun deleteRecurringSeries(recurringId: String) {
        val email = currentUserEmail.value ?: return
        viewModelScope.launch {
            transactionRepository.deleteRecurringTransaction(email, recurringId)
                .onFailure { error ->
                    _transactionsError.value = "Failed to delete recurring transaction: ${error.message}"
                }
            val seriesTxs = transactions.value.filter { it.recurringId == recurringId }
            seriesTxs.forEach { tx ->
                transactionRepository.deleteTransaction(email, tx.id)
            }
        }
    }

    fun addCustomCategory(name: String) {
        val email = currentUserEmail.value ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            val newCat = CustomCategory(
                id = java.util.UUID.randomUUID().toString(),
                name = name.trim(),
                userEmail = email
            )
            transactionRepository.addCustomCategory(email, newCat)
        }
    }

    fun updateCustomCategory(id: String, newName: String) {
        val email = currentUserEmail.value ?: return
        if (newName.isBlank()) return
        viewModelScope.launch {
            val updatedCat = CustomCategory(
                id = id,
                name = newName.trim(),
                userEmail = email
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

    fun updatePrimaryColor(hex: String) {
        budgetPrefs.edit().putString("primary_color", hex).apply()
        _primaryColor.value = hex
    }

    fun updateSecondaryColor(hex: String) {
        budgetPrefs.edit().putString("secondary_color", hex).apply()
        _secondaryColor.value = hex
    }

    fun updateAccentColor(hex: String) {
        budgetPrefs.edit().putString("accent_color", hex).apply()
        _accentColor.value = hex
    }

    fun resetThemeToDefault() {
        budgetPrefs.edit()
            .putString("primary_color", "#FFD97D")
            .putString("secondary_color", "#A78BFA")
            .putString("accent_color", "#D9F99D")
            .apply()
        _primaryColor.value = "#FFD97D"
        _secondaryColor.value = "#A78BFA"
        _accentColor.value = "#D9F99D"
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
}

data class MonthlySummary(
    val totalIncome: Double,
    val totalExpense: Double,
    val currentMonthList: List<Transaction>
)
