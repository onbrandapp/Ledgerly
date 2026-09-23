package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "local_transactions")
data class LocalTransaction(
    @PrimaryKey val id: String,
    val amount: Double,
    val category: String,
    val type: String,
    val description: String,
    val date: Long,
    val recurringId: String = "",
    val paid: Boolean = false,
    val currency: String = "USD",
    val originalAmount: Double = 0.0,
    val exchangeRate: Double = 1.0
) {
    fun toDomain(): Transaction = Transaction(
        id = id,
        amount = amount,
        category = category,
        type = type,
        description = description,
        date = date,
        recurringId = recurringId,
        paid = paid,
        currency = currency.ifBlank { "USD" },
        originalAmount = if (originalAmount > 0.0) originalAmount else amount,
        exchangeRate = if (exchangeRate > 0.0) exchangeRate else 1.0
    )

    companion object {
        fun fromDomain(t: Transaction): LocalTransaction = LocalTransaction(
            id = t.id.ifEmpty { java.util.UUID.randomUUID().toString() },
            amount = t.amount,
            category = t.category,
            type = t.type,
            description = t.description,
            date = t.date,
            recurringId = t.recurringId,
            paid = t.paid,
            currency = t.currency.ifBlank { "USD" },
            originalAmount = if (t.originalAmount > 0.0) t.originalAmount else t.amount,
            exchangeRate = if (t.exchangeRate > 0.0) t.exchangeRate else 1.0
        )
    }
}

@Entity(tableName = "local_recurring_transactions")
data class LocalRecurringTransaction(
    @PrimaryKey val id: String,
    val amount: Double,
    val category: String,
    val type: String,
    val description: String,
    val frequency: String,
    val startDate: Long,
    val lastLoggedDate: Long,
    val currency: String = "USD",
    val originalAmount: Double = 0.0,
    val exchangeRate: Double = 1.0
) {
    fun toDomain(): RecurringTransaction = RecurringTransaction(
        id = id,
        amount = amount,
        category = category,
        type = type,
        description = description,
        frequency = frequency,
        startDate = startDate,
        lastLoggedDate = lastLoggedDate,
        currency = currency.ifBlank { "USD" },
        originalAmount = if (originalAmount > 0.0) originalAmount else amount,
        exchangeRate = if (exchangeRate > 0.0) exchangeRate else 1.0
    )

    companion object {
        fun fromDomain(r: RecurringTransaction): LocalRecurringTransaction = LocalRecurringTransaction(
            id = r.id.ifEmpty { java.util.UUID.randomUUID().toString() },
            amount = r.amount,
            category = r.category,
            type = r.type,
            description = r.description,
            frequency = r.frequency,
            startDate = r.startDate,
            lastLoggedDate = r.lastLoggedDate,
            currency = r.currency.ifBlank { "USD" },
            originalAmount = if (r.originalAmount > 0.0) r.originalAmount else r.amount,
            exchangeRate = if (r.exchangeRate > 0.0) r.exchangeRate else 1.0
        )
    }
}

@Entity(tableName = "local_categories")
data class LocalCategory(
    @PrimaryKey val id: String,
    val name: String,
    val userEmail: String,
    val iconName: String = "category",
    val colorHex: String = "#00897B",
    val type: String = "EXPENSE"
) {
    fun toDomain(): CustomCategory = CustomCategory(
        id = id,
        name = name,
        userEmail = userEmail,
        iconName = iconName,
        colorHex = colorHex,
        type = type
    )

    companion object {
        fun fromDomain(c: CustomCategory): LocalCategory = LocalCategory(
            id = c.id.ifEmpty { java.util.UUID.randomUUID().toString() },
            name = c.name,
            userEmail = c.userEmail,
            iconName = c.iconName.ifEmpty { "category" },
            colorHex = c.colorHex.ifEmpty { "#00897B" },
            type = c.type.ifEmpty { "EXPENSE" }
        )
    }
}

@Entity(tableName = "local_forecast_incomes")
data class LocalForecastIncome(
    @PrimaryKey val id: String,
    val title: String,
    val amount: Double,
    val expectedDate: Long,
    val category: String,
    val status: String,
    val notes: String,
    val bulletPointsJson: String,
    val completedBulletsJson: String,
    val isRealized: Boolean,
    val userEmail: String,
    val colorTag: String = "#FFD97D",
    val createdAt: Long
) {
    fun toDomain(): ForecastIncome = ForecastIncome(
        id = id,
        title = title,
        amount = amount,
        expectedDate = expectedDate,
        category = category,
        status = status,
        notes = notes,
        bulletPoints = JsonListHelper.jsonToStringList(bulletPointsJson),
        completedBullets = JsonListHelper.jsonToIntList(completedBulletsJson),
        isRealized = isRealized,
        userEmail = userEmail,
        colorTag = colorTag.ifBlank { "#FFD97D" },
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(f: ForecastIncome): LocalForecastIncome = LocalForecastIncome(
            id = f.id.ifEmpty { java.util.UUID.randomUUID().toString() },
            title = f.title,
            amount = f.amount,
            expectedDate = f.expectedDate,
            category = f.category,
            status = f.status,
            notes = f.notes,
            bulletPointsJson = JsonListHelper.stringListToJson(f.bulletPoints),
            completedBulletsJson = JsonListHelper.intListToJson(f.completedBullets),
            isRealized = f.isRealized,
            userEmail = f.userEmail,
            colorTag = f.colorTag.ifBlank { "#FFD97D" },
            createdAt = f.createdAt
        )
    }
}

@Entity(tableName = "local_future_income_notes")
data class LocalFutureIncomeNote(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val bulletPointsJson: String,
    val completedBulletsJson: String,
    val userEmail: String,
    val colorTag: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): FutureIncomeNote = FutureIncomeNote(
        id = id,
        title = title,
        content = content,
        bulletPoints = JsonListHelper.jsonToStringList(bulletPointsJson),
        completedBullets = JsonListHelper.jsonToIntList(completedBulletsJson),
        userEmail = userEmail,
        colorTag = colorTag,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(n: FutureIncomeNote): LocalFutureIncomeNote = LocalFutureIncomeNote(
            id = n.id.ifEmpty { java.util.UUID.randomUUID().toString() },
            title = n.title,
            content = n.content,
            bulletPointsJson = JsonListHelper.stringListToJson(n.bulletPoints),
            completedBulletsJson = JsonListHelper.intListToJson(n.completedBullets),
            userEmail = n.userEmail,
            colorTag = n.colorTag,
            createdAt = n.createdAt,
            updatedAt = n.updatedAt
        )
    }
}

@Entity(tableName = "local_audit_deleted_items")
data class LocalAuditDeletedItem(
    @PrimaryKey val id: String,
    val originalId: String,
    val itemType: String,
    val title: String,
    val amount: Double,
    val categoryOrStatus: String,
    val details: String,
    val sourceOrDeletedBy: String,
    val deletedAt: Long,
    val originalDate: Long,
    val userEmail: String,
    val payloadJson: String = ""
) {
    fun toDomain(): AuditDeletedItem = AuditDeletedItem(
        id = id,
        originalId = originalId,
        itemType = itemType,
        title = title,
        amount = amount,
        categoryOrStatus = categoryOrStatus,
        details = details,
        sourceOrDeletedBy = sourceOrDeletedBy,
        deletedAt = deletedAt,
        originalDate = originalDate,
        userEmail = userEmail,
        payloadJson = payloadJson
    )

    companion object {
        fun fromDomain(item: AuditDeletedItem): LocalAuditDeletedItem = LocalAuditDeletedItem(
            id = item.id.ifEmpty { java.util.UUID.randomUUID().toString() },
            originalId = item.originalId,
            itemType = itemTypeSafe(item.itemType),
            title = item.title,
            amount = item.amount,
            categoryOrStatus = item.categoryOrStatus,
            details = item.details,
            sourceOrDeletedBy = item.sourceOrDeletedBy.ifBlank { "User Action" },
            deletedAt = item.deletedAt,
            originalDate = item.originalDate,
            userEmail = item.userEmail,
            payloadJson = item.payloadJson
        )

        private fun itemTypeSafe(type: String): String = when (type.uppercase()) {
            "FORECAST", "FORECAST_INCOME" -> "FORECAST"
            "NOTE", "INCOME_NOTE", "FUTURE_INCOME_NOTE" -> "NOTE"
            "RECURRING" -> "RECURRING"
            else -> "TRANSACTION"
        }
    }
}

object JsonListHelper {
    fun stringListToJson(list: List<String>): String {
        val arr = org.json.JSONArray()
        list.forEach { arr.put(it) }
        return arr.toString()
    }

    fun jsonToStringList(json: String): List<String> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = org.json.JSONArray(json)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun intListToJson(list: List<Int>): String {
        val arr = org.json.JSONArray()
        list.forEach { arr.put(it) }
        return arr.toString()
    }

    fun jsonToIntList(json: String): List<Int> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = org.json.JSONArray(json)
            (0 until arr.length()).map { arr.getInt(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM local_transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<LocalTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: LocalTransaction)

    @Query("DELETE FROM local_transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: String)

    @Query("SELECT * FROM local_recurring_transactions ORDER BY startDate DESC")
    fun getAllRecurringTransactions(): Flow<List<LocalRecurringTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringTransaction(recurring: LocalRecurringTransaction)

    @Query("DELETE FROM local_recurring_transactions WHERE id = :id")
    suspend fun deleteRecurringTransactionById(id: String)

    @Query("SELECT * FROM local_categories WHERE userEmail = :userEmail ORDER BY name ASC")
    fun getCustomCategories(userEmail: String): Flow<List<LocalCategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomCategory(category: LocalCategory)

    @Query("DELETE FROM local_categories WHERE id = :id")
    suspend fun deleteCustomCategoryById(id: String)

    // Forecast Income Queries
    @Query("SELECT * FROM local_forecast_incomes WHERE userEmail = :userEmail ORDER BY expectedDate ASC")
    fun getForecastIncomes(userEmail: String): Flow<List<LocalForecastIncome>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForecastIncome(forecast: LocalForecastIncome)

    @Query("DELETE FROM local_forecast_incomes WHERE id = :id")
    suspend fun deleteForecastIncomeById(id: String)

    // Future Income Notes Queries
    @Query("SELECT * FROM local_future_income_notes WHERE userEmail = :userEmail ORDER BY updatedAt DESC")
    fun getFutureIncomeNotes(userEmail: String): Flow<List<LocalFutureIncomeNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFutureIncomeNote(note: LocalFutureIncomeNote)

    @Query("DELETE FROM local_future_income_notes WHERE id = :id")
    suspend fun deleteFutureIncomeNoteById(id: String)

    // Audit Deleted Items Queries
    @Query("SELECT * FROM local_audit_deleted_items WHERE userEmail = :userEmail ORDER BY deletedAt DESC")
    fun getAuditDeletedItems(userEmail: String): Flow<List<LocalAuditDeletedItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditDeletedItem(item: LocalAuditDeletedItem)

    @Query("DELETE FROM local_audit_deleted_items WHERE id = :id")
    suspend fun deleteAuditDeletedItemById(id: String)

    @Query("DELETE FROM local_audit_deleted_items WHERE userEmail = :userEmail")
    suspend fun clearAuditDeletedItems(userEmail: String)

    // Direct Snapshot Queries for Backup
    @Query("SELECT * FROM local_transactions ORDER BY date DESC")
    suspend fun getAllTransactionsSnapshot(): List<LocalTransaction>

    @Query("SELECT * FROM local_recurring_transactions ORDER BY startDate DESC")
    suspend fun getAllRecurringTransactionsSnapshot(): List<LocalRecurringTransaction>

    @Query("SELECT * FROM local_categories WHERE userEmail = :userEmail ORDER BY name ASC")
    suspend fun getCustomCategoriesSnapshot(userEmail: String): List<LocalCategory>

    @Query("SELECT * FROM local_forecast_incomes WHERE userEmail = :userEmail ORDER BY expectedDate ASC")
    suspend fun getForecastIncomesSnapshot(userEmail: String): List<LocalForecastIncome>

    @Query("SELECT * FROM local_future_income_notes WHERE userEmail = :userEmail ORDER BY updatedAt DESC")
    suspend fun getFutureIncomeNotesSnapshot(userEmail: String): List<LocalFutureIncomeNote>

    @Query("SELECT * FROM local_audit_deleted_items WHERE userEmail = :userEmail ORDER BY deletedAt DESC")
    suspend fun getAuditDeletedItemsSnapshot(userEmail: String): List<LocalAuditDeletedItem>

    // Batch Restore & Clean Methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<LocalTransaction>)

    @Query("DELETE FROM local_transactions")
    suspend fun clearAllTransactions()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringTransactions(recurring: List<LocalRecurringTransaction>)

    @Query("DELETE FROM local_recurring_transactions")
    suspend fun clearAllRecurringTransactions()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomCategories(categories: List<LocalCategory>)

    @Query("DELETE FROM local_categories WHERE userEmail = :userEmail")
    suspend fun clearCustomCategories(userEmail: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForecastIncomes(forecasts: List<LocalForecastIncome>)

    @Query("DELETE FROM local_forecast_incomes WHERE userEmail = :userEmail")
    suspend fun clearForecastIncomes(userEmail: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFutureIncomeNotes(notes: List<LocalFutureIncomeNote>)

    @Query("DELETE FROM local_future_income_notes WHERE userEmail = :userEmail")
    suspend fun clearFutureIncomeNotes(userEmail: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditDeletedItems(items: List<LocalAuditDeletedItem>)
}

@Database(
    entities = [
        LocalTransaction::class,
        LocalRecurringTransaction::class,
        LocalCategory::class,
        LocalForecastIncome::class,
        LocalFutureIncomeNote::class,
        LocalAuditDeletedItem::class
    ],
    version = 12,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_tracker_db"
                )
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
