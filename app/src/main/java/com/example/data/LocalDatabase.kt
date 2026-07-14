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
    val date: Long
) {
    fun toDomain(): Transaction = Transaction(
        id = id,
        amount = amount,
        category = category,
        type = type,
        description = description,
        date = date
    )

    companion object {
        fun fromDomain(t: Transaction): LocalTransaction = LocalTransaction(
            id = t.id.ifEmpty { java.util.UUID.randomUUID().toString() },
            amount = t.amount,
            category = t.category,
            type = t.type,
            description = t.description,
            date = t.date
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
}

@Database(entities = [LocalTransaction::class], version = 1, exportSchema = false)
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
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
