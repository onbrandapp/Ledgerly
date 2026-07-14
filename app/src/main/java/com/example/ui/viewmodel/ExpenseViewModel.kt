package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

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

    // Transaction Operations
    fun addTransaction(amount: Double, category: String, type: String, description: String) {
        val email = currentUserEmail.value ?: return
        val newTx = Transaction(
            id = "",
            amount = amount,
            category = category.trim(),
            type = type,
            description = description.trim(),
            date = System.currentTimeMillis()
        )
        viewModelScope.launch {
            transactionRepository.addTransaction(email, newTx)
                .onFailure { error ->
                    _transactionsError.value = "Failed to add transaction: ${error.message}"
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

    fun updateMonthlyBudget(newLimit: Double) {
        budgetPrefs.edit().putFloat("limit", newLimit.toFloat()).apply()
        _monthlyBudget.value = newLimit
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
