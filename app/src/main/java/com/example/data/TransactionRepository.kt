package com.example.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

interface TransactionRepository {
    fun getTransactions(userEmail: String): Flow<List<Transaction>>
    suspend fun addTransaction(userEmail: String, transaction: Transaction): Result<Unit>
    suspend fun deleteTransaction(userEmail: String, id: String): Result<Unit>

    fun getRecurringTransactions(userEmail: String): Flow<List<RecurringTransaction>>
    suspend fun addRecurringTransaction(userEmail: String, recurring: RecurringTransaction): Result<Unit>
    suspend fun deleteRecurringTransaction(userEmail: String, id: String): Result<Unit>

    fun getCustomCategories(userEmail: String): Flow<List<CustomCategory>>
    suspend fun addCustomCategory(userEmail: String, category: CustomCategory): Result<Unit>
    suspend fun deleteCustomCategory(userEmail: String, id: String): Result<Unit>

    fun getForecastIncomes(userEmail: String): Flow<List<ForecastIncome>>
    suspend fun addForecastIncome(userEmail: String, forecast: ForecastIncome): Result<Unit>
    suspend fun deleteForecastIncome(userEmail: String, id: String): Result<Unit>

    fun getFutureIncomeNotes(userEmail: String): Flow<List<FutureIncomeNote>>
    suspend fun addFutureIncomeNote(userEmail: String, note: FutureIncomeNote): Result<Unit>
    suspend fun deleteFutureIncomeNote(userEmail: String, id: String): Result<Unit>
}

class FirebaseTransactionRepository : TransactionRepository {
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun getTransactions(userEmail: String): Flow<List<Transaction>> = callbackFlow {
        val query = firestore.collection("users")
            .document(userEmail)
            .collection("transactions")
            .orderBy("date", Query.Direction.DESCENDING)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val transactions = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Transaction::class.java)?.copy(id = doc.id)
                }
                trySend(transactions)
            }
        }
        awaitClose { registration.remove() }
    }

    override suspend fun addTransaction(userEmail: String, transaction: Transaction): Result<Unit> = suspendCancellableCoroutine { continuation ->
        val collection = firestore.collection("users")
            .document(userEmail)
            .collection("transactions")

        val docRef = if (transaction.id.isEmpty()) collection.document() else collection.document(transaction.id)
        val finalTransaction = transaction.copy(id = docRef.id)

        docRef.set(finalTransaction)
            .addOnSuccessListener {
                if (continuation.isActive) continuation.resume(Result.success(Unit))
            }
            .addOnFailureListener { exception ->
                if (continuation.isActive) continuation.resume(Result.failure(exception))
            }
    }

    override suspend fun deleteTransaction(userEmail: String, id: String): Result<Unit> = suspendCancellableCoroutine { continuation ->
        firestore.collection("users")
            .document(userEmail)
            .collection("transactions")
            .document(id)
            .delete()
            .addOnSuccessListener {
                if (continuation.isActive) continuation.resume(Result.success(Unit))
            }
            .addOnFailureListener { exception ->
                if (continuation.isActive) continuation.resume(Result.failure(exception))
            }
    }

    override fun getRecurringTransactions(userEmail: String): Flow<List<RecurringTransaction>> = callbackFlow {
        val query = firestore.collection("users")
            .document(userEmail)
            .collection("recurring_transactions")
            .orderBy("startDate", Query.Direction.DESCENDING)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val recurrings = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(RecurringTransaction::class.java)?.copy(id = doc.id)
                }
                trySend(recurrings)
            }
        }
        awaitClose { registration.remove() }
    }

    override suspend fun addRecurringTransaction(userEmail: String, recurring: RecurringTransaction): Result<Unit> = suspendCancellableCoroutine { continuation ->
        val collection = firestore.collection("users")
            .document(userEmail)
            .collection("recurring_transactions")

        val docRef = if (recurring.id.isEmpty()) collection.document() else collection.document(recurring.id)
        val finalRecurring = recurring.copy(id = docRef.id)

        docRef.set(finalRecurring)
            .addOnSuccessListener {
                if (continuation.isActive) continuation.resume(Result.success(Unit))
            }
            .addOnFailureListener { exception ->
                if (continuation.isActive) continuation.resume(Result.failure(exception))
            }
    }

    override suspend fun deleteRecurringTransaction(userEmail: String, id: String): Result<Unit> = suspendCancellableCoroutine { continuation ->
        firestore.collection("users")
            .document(userEmail)
            .collection("recurring_transactions")
            .document(id)
            .delete()
            .addOnSuccessListener {
                if (continuation.isActive) continuation.resume(Result.success(Unit))
            }
            .addOnFailureListener { exception ->
                if (continuation.isActive) continuation.resume(Result.failure(exception))
            }
    }

    override fun getCustomCategories(userEmail: String): Flow<List<CustomCategory>> = callbackFlow {
        val query = firestore.collection("users")
            .document(userEmail)
            .collection("categories")
            .orderBy("name", Query.Direction.ASCENDING)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val categories = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(CustomCategory::class.java)?.copy(id = doc.id)
                }
                trySend(categories)
            }
        }
        awaitClose { registration.remove() }
    }

    override suspend fun addCustomCategory(userEmail: String, category: CustomCategory): Result<Unit> = suspendCancellableCoroutine { continuation ->
        val collection = firestore.collection("users")
            .document(userEmail)
            .collection("categories")

        val docRef = if (category.id.isEmpty()) collection.document() else collection.document(category.id)
        val finalCategory = category.copy(id = docRef.id, userEmail = userEmail)

        docRef.set(finalCategory)
            .addOnSuccessListener {
                if (continuation.isActive) continuation.resume(Result.success(Unit))
            }
            .addOnFailureListener { exception ->
                if (continuation.isActive) continuation.resume(Result.failure(exception))
            }
    }

    override suspend fun deleteCustomCategory(userEmail: String, id: String): Result<Unit> = suspendCancellableCoroutine { continuation ->
        firestore.collection("users")
            .document(userEmail)
            .collection("categories")
            .document(id)
            .delete()
            .addOnSuccessListener {
                if (continuation.isActive) continuation.resume(Result.success(Unit))
            }
            .addOnFailureListener { exception ->
                if (continuation.isActive) continuation.resume(Result.failure(exception))
            }
    }

    override fun getForecastIncomes(userEmail: String): Flow<List<ForecastIncome>> = callbackFlow {
        val query = firestore.collection("users")
            .document(userEmail)
            .collection("forecast_incomes")
            .orderBy("expectedDate", Query.Direction.ASCENDING)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ForecastIncome::class.java)?.copy(id = doc.id)
                }
                trySend(list)
            }
        }
        awaitClose { registration.remove() }
    }

    override suspend fun addForecastIncome(userEmail: String, forecast: ForecastIncome): Result<Unit> = suspendCancellableCoroutine { continuation ->
        val collection = firestore.collection("users")
            .document(userEmail)
            .collection("forecast_incomes")

        val docRef = if (forecast.id.isEmpty()) collection.document() else collection.document(forecast.id)
        val finalForecast = forecast.copy(id = docRef.id, userEmail = userEmail)

        docRef.set(finalForecast)
            .addOnSuccessListener {
                if (continuation.isActive) continuation.resume(Result.success(Unit))
            }
            .addOnFailureListener { exception ->
                if (continuation.isActive) continuation.resume(Result.failure(exception))
            }
    }

    override suspend fun deleteForecastIncome(userEmail: String, id: String): Result<Unit> = suspendCancellableCoroutine { continuation ->
        firestore.collection("users")
            .document(userEmail)
            .collection("forecast_incomes")
            .document(id)
            .delete()
            .addOnSuccessListener {
                if (continuation.isActive) continuation.resume(Result.success(Unit))
            }
            .addOnFailureListener { exception ->
                if (continuation.isActive) continuation.resume(Result.failure(exception))
            }
    }

    override fun getFutureIncomeNotes(userEmail: String): Flow<List<FutureIncomeNote>> = callbackFlow {
        val query = firestore.collection("users")
            .document(userEmail)
            .collection("future_income_notes")
            .orderBy("updatedAt", Query.Direction.DESCENDING)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(FutureIncomeNote::class.java)?.copy(id = doc.id)
                }
                trySend(list)
            }
        }
        awaitClose { registration.remove() }
    }

    override suspend fun addFutureIncomeNote(userEmail: String, note: FutureIncomeNote): Result<Unit> = suspendCancellableCoroutine { continuation ->
        val collection = firestore.collection("users")
            .document(userEmail)
            .collection("future_income_notes")

        val docRef = if (note.id.isEmpty()) collection.document() else collection.document(note.id)
        val finalNote = note.copy(id = docRef.id, userEmail = userEmail)

        docRef.set(finalNote)
            .addOnSuccessListener {
                if (continuation.isActive) continuation.resume(Result.success(Unit))
            }
            .addOnFailureListener { exception ->
                if (continuation.isActive) continuation.resume(Result.failure(exception))
            }
    }

    override suspend fun deleteFutureIncomeNote(userEmail: String, id: String): Result<Unit> = suspendCancellableCoroutine { continuation ->
        firestore.collection("users")
            .document(userEmail)
            .collection("future_income_notes")
            .document(id)
            .delete()
            .addOnSuccessListener {
                if (continuation.isActive) continuation.resume(Result.success(Unit))
            }
            .addOnFailureListener { exception ->
                if (continuation.isActive) continuation.resume(Result.failure(exception))
            }
    }
}

class RoomTransactionRepository(context: Context) : TransactionRepository {
    private val dao = AppDatabase.getDatabase(context).transactionDao()

    override fun getTransactions(userEmail: String): Flow<List<Transaction>> {
        return dao.getAllTransactions().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun addTransaction(userEmail: String, transaction: Transaction): Result<Unit> {
        return try {
            val localTx = LocalTransaction.fromDomain(transaction)
            dao.insertTransaction(localTx)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTransaction(userEmail: String, id: String): Result<Unit> {
        return try {
            dao.deleteTransactionById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getRecurringTransactions(userEmail: String): Flow<List<RecurringTransaction>> {
        return dao.getAllRecurringTransactions().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun addRecurringTransaction(userEmail: String, recurring: RecurringTransaction): Result<Unit> {
        return try {
            val localRec = LocalRecurringTransaction.fromDomain(recurring)
            dao.insertRecurringTransaction(localRec)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteRecurringTransaction(userEmail: String, id: String): Result<Unit> {
        return try {
            dao.deleteRecurringTransactionById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getCustomCategories(userEmail: String): Flow<List<CustomCategory>> {
        return dao.getCustomCategories(userEmail).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun addCustomCategory(userEmail: String, category: CustomCategory): Result<Unit> {
        return try {
            val localCat = LocalCategory.fromDomain(category.copy(userEmail = userEmail))
            dao.insertCustomCategory(localCat)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteCustomCategory(userEmail: String, id: String): Result<Unit> {
        return try {
            dao.deleteCustomCategoryById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getForecastIncomes(userEmail: String): Flow<List<ForecastIncome>> {
        return dao.getForecastIncomes(userEmail).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun addForecastIncome(userEmail: String, forecast: ForecastIncome): Result<Unit> {
        return try {
            val localForecast = LocalForecastIncome.fromDomain(forecast.copy(userEmail = userEmail))
            dao.insertForecastIncome(localForecast)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteForecastIncome(userEmail: String, id: String): Result<Unit> {
        return try {
            dao.deleteForecastIncomeById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getFutureIncomeNotes(userEmail: String): Flow<List<FutureIncomeNote>> {
        return dao.getFutureIncomeNotes(userEmail).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun addFutureIncomeNote(userEmail: String, note: FutureIncomeNote): Result<Unit> {
        return try {
            val localNote = LocalFutureIncomeNote.fromDomain(note.copy(userEmail = userEmail))
            dao.insertFutureIncomeNote(localNote)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteFutureIncomeNote(userEmail: String, id: String): Result<Unit> {
        return try {
            dao.deleteFutureIncomeNoteById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

object TransactionRepositoryFactory {
    fun create(context: Context): TransactionRepository {
        return try {
            val app = initFirebase(context)
            if (app != null) {
                FirebaseFirestore.getInstance(app)
                FirebaseTransactionRepository()
            } else {
                RoomTransactionRepository(context)
            }
        } catch (e: Exception) {
            RoomTransactionRepository(context)
        }
    }
}
