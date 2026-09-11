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
    val paid: Boolean = false
) {
    fun toDomain(): Transaction = Transaction(
        id = id,
        amount = amount,
        category = category,
        type = type,
        description = description,
        date = date,
        recurringId = recurringId,
        paid = paid
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
            paid = t.paid
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
    val lastLoggedDate: Long
) {
    fun toDomain(): RecurringTransaction = RecurringTransaction(
        id = id,
        amount = amount,
        category = category,
        type = type,
        description = description,
        frequency = frequency,
        startDate = startDate,
        lastLoggedDate = lastLoggedDate
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
            lastLoggedDate = r.lastLoggedDate
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
}

@Database(
    entities = [
        LocalTransaction::class,
        LocalRecurringTransaction::class,
        LocalCategory::class,
        LocalForecastIncome::class,
        LocalFutureIncomeNote::class
    ],
    version = 9,
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
