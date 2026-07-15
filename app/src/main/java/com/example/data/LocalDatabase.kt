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
    val recurringId: String = ""
) {
    fun toDomain(): Transaction = Transaction(
        id = id,
        amount = amount,
        category = category,
        type = type,
        description = description,
        date = date,
        recurringId = recurringId
    )

    companion object {
        fun fromDomain(t: Transaction): LocalTransaction = LocalTransaction(
            id = t.id.ifEmpty { java.util.UUID.randomUUID().toString() },
            amount = t.amount,
            category = t.category,
            type = t.type,
            description = t.description,
            date = t.date,
            recurringId = t.recurringId
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
    val userEmail: String
) {
    fun toDomain(): CustomCategory = CustomCategory(
        id = id,
        name = name,
        userEmail = userEmail
    )

    companion object {
        fun fromDomain(c: CustomCategory): LocalCategory = LocalCategory(
            id = c.id.ifEmpty { java.util.UUID.randomUUID().toString() },
            name = c.name,
            userEmail = c.userEmail
        )
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
}

@Database(entities = [LocalTransaction::class, LocalRecurringTransaction::class, LocalCategory::class], version = 4, exportSchema = false)
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
