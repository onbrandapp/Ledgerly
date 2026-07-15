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
}

object TransactionRepositoryFactory {
    fun create(context: Context): TransactionRepository {
        return try {
            FirebaseApp.getInstance()
            FirebaseFirestore.getInstance()
            FirebaseTransactionRepository()
        } catch (e: Exception) {
            RoomTransactionRepository(context)
        }
    }
}
