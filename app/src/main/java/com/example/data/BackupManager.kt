package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

data class BackupSettings(
    val monthlyBudget: Double = 2000.0,
    val isDarkMode: Boolean = false,
    val accentColor: String = "#392720",
    val primaryColor: String = "#392720",
    val secondaryColor: String = "#392720",
    val biometricEnabled: Boolean = false
)

data class BackupData(
    val version: Int = 1,
    val appName: String = "Ledgerly",
    val exportDate: Long = System.currentTimeMillis(),
    val formattedDate: String = "",
    val userEmail: String = "",
    val settings: BackupSettings = BackupSettings(),
    val transactions: List<Transaction> = emptyList(),
    val recurringTransactions: List<RecurringTransaction> = emptyList(),
    val customCategories: List<CustomCategory> = emptyList(),
    val forecastIncomes: List<ForecastIncome> = emptyList(),
    val futureIncomeNotes: List<FutureIncomeNote> = emptyList(),
    val auditDeletedItems: List<AuditDeletedItem> = emptyList()
)

data class BackupMetadata(
    val id: String,
    val timestamp: Long,
    val formattedDate: String,
    val transactionsCount: Int,
    val recurringCount: Int,
    val categoriesCount: Int,
    val forecastCount: Int,
    val notesCount: Int,
    val auditCount: Int,
    val isCloud: Boolean,
    val fileSizeBytes: Long = 0L,
    val fileName: String = ""
)

object BackupManager {

    private const val BACKUPS_DIR = "backups"
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.US)
    private val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    fun getBackupsDirectory(context: Context): File {
        val dir = File(context.filesDir, BACKUPS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    // ==========================================
    // JSON SERIALIZATION / DESERIALIZATION
    // ==========================================

    fun serializeBackupData(data: BackupData): String {
        val root = JSONObject().apply {
            put("version", data.version)
            put("appName", data.appName)
            put("exportDate", data.exportDate)
            put("formattedDate", if (data.formattedDate.isNotBlank()) data.formattedDate else dateFormat.format(Date(data.exportDate)))
            put("userEmail", data.userEmail)

            // Settings
            val settingsObj = JSONObject().apply {
                put("monthlyBudget", data.settings.monthlyBudget)
                put("isDarkMode", data.settings.isDarkMode)
                put("accentColor", data.settings.accentColor)
                put("primaryColor", data.settings.primaryColor)
                put("secondaryColor", data.settings.secondaryColor)
                put("biometricEnabled", data.settings.biometricEnabled)
            }
            put("settings", settingsObj)

            // Transactions
            val txArr = JSONArray()
            data.transactions.forEach { tx ->
                txArr.put(JSONObject().apply {
                    put("id", tx.id)
                    put("amount", tx.amount)
                    put("category", tx.category)
                    put("type", tx.type)
                    put("description", tx.description)
                    put("date", tx.date)
                    put("recurringId", tx.recurringId)
                    put("paid", tx.paid)
                })
            }
            put("transactions", txArr)

            // Recurring Transactions
            val recArr = JSONArray()
            data.recurringTransactions.forEach { rec ->
                recArr.put(JSONObject().apply {
                    put("id", rec.id)
                    put("amount", rec.amount)
                    put("category", rec.category)
                    put("type", rec.type)
                    put("description", rec.description)
                    put("frequency", rec.frequency)
                    put("startDate", rec.startDate)
                    put("lastLoggedDate", rec.lastLoggedDate)
                })
            }
            put("recurringTransactions", recArr)

            // Custom Categories
            val catArr = JSONArray()
            data.customCategories.forEach { cat ->
                catArr.put(JSONObject().apply {
                    put("id", cat.id)
                    put("name", cat.name)
                    put("userEmail", cat.userEmail)
                    put("iconName", cat.iconName)
                    put("colorHex", cat.colorHex)
                    put("type", cat.type)
                })
            }
            put("customCategories", catArr)

            // Forecast Incomes
            val forecastArr = JSONArray()
            data.forecastIncomes.forEach { fc ->
                forecastArr.put(JSONObject().apply {
                    put("id", fc.id)
                    put("title", fc.title)
                    put("amount", fc.amount)
                    put("expectedDate", fc.expectedDate)
                    put("category", fc.category)
                    put("status", fc.status)
                    put("notes", fc.notes)
                    put("bulletPoints", JSONArray(fc.bulletPoints))
                    put("completedBullets", JSONArray(fc.completedBullets))
                    put("isRealized", fc.isRealized)
                    put("userEmail", fc.userEmail)
                    put("colorTag", fc.colorTag)
                    put("createdAt", fc.createdAt)
                })
            }
            put("forecastIncomes", forecastArr)

            // Future Income Notes
            val notesArr = JSONArray()
            data.futureIncomeNotes.forEach { note ->
                notesArr.put(JSONObject().apply {
                    put("id", note.id)
                    put("title", note.title)
                    put("content", note.content)
                    put("bulletPoints", JSONArray(note.bulletPoints))
                    put("completedBullets", JSONArray(note.completedBullets))
                    put("userEmail", note.userEmail)
                    put("colorTag", note.colorTag)
                    put("createdAt", note.createdAt)
                    put("updatedAt", note.updatedAt)
                })
            }
            put("futureIncomeNotes", notesArr)

            // Audit Deleted Items
            val auditArr = JSONArray()
            data.auditDeletedItems.forEach { audit ->
                auditArr.put(JSONObject().apply {
                    put("id", audit.id)
                    put("originalId", audit.originalId)
                    put("itemType", audit.itemType)
                    put("title", audit.title)
                    put("amount", audit.amount)
                    put("categoryOrStatus", audit.categoryOrStatus)
                    put("details", audit.details)
                    put("sourceOrDeletedBy", audit.sourceOrDeletedBy)
                    put("deletedAt", audit.deletedAt)
                    put("originalDate", audit.originalDate)
                    put("userEmail", audit.userEmail)
                    put("payloadJson", audit.payloadJson)
                })
            }
            put("auditDeletedItems", auditArr)
        }
        return root.toString(2)
    }

    fun parseBackupData(jsonString: String): Result<BackupData> {
        return try {
            val root = JSONObject(jsonString)
            val version = root.optInt("version", 1)
            val appName = root.optString("appName", "Ledgerly")
            val exportDate = root.optLong("exportDate", System.currentTimeMillis())
            val formattedDate = root.optString("formattedDate", dateFormat.format(Date(exportDate)))
            val userEmail = root.optString("userEmail", "")

            // Settings
            val settingsObj = root.optJSONObject("settings")
            val settings = if (settingsObj != null) {
                BackupSettings(
                    monthlyBudget = settingsObj.optDouble("monthlyBudget", 2000.0),
                    isDarkMode = settingsObj.optBoolean("isDarkMode", false),
                    accentColor = settingsObj.optString("accentColor", "#392720"),
                    primaryColor = settingsObj.optString("primaryColor", "#392720"),
                    secondaryColor = settingsObj.optString("secondaryColor", "#392720"),
                    biometricEnabled = settingsObj.optBoolean("biometricEnabled", false)
                )
            } else {
                BackupSettings()
            }

            // Transactions
            val transactions = mutableListOf<Transaction>()
            val txArr = root.optJSONArray("transactions")
            if (txArr != null) {
                for (i in 0 until txArr.length()) {
                    val obj = txArr.getJSONObject(i)
                    transactions.add(
                        Transaction(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            amount = obj.optDouble("amount", 0.0),
                            category = obj.optString("category", "General"),
                            type = obj.optString("type", "EXPENSE"),
                            description = obj.optString("description", ""),
                            date = obj.optLong("date", System.currentTimeMillis()),
                            recurringId = obj.optString("recurringId", ""),
                            paid = obj.optBoolean("paid", false)
                        )
                    )
                }
            }

            // Recurring Transactions
            val recurring = mutableListOf<RecurringTransaction>()
            val recArr = root.optJSONArray("recurringTransactions")
            if (recArr != null) {
                for (i in 0 until recArr.length()) {
                    val obj = recArr.getJSONObject(i)
                    recurring.add(
                        RecurringTransaction(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            amount = obj.optDouble("amount", 0.0),
                            category = obj.optString("category", "General"),
                            type = obj.optString("type", "EXPENSE"),
                            description = obj.optString("description", ""),
                            frequency = obj.optString("frequency", "MONTHLY"),
                            startDate = obj.optLong("startDate", System.currentTimeMillis()),
                            lastLoggedDate = obj.optLong("lastLoggedDate", 0L)
                        )
                    )
                }
            }

            // Custom Categories
            val categories = mutableListOf<CustomCategory>()
            val catArr = root.optJSONArray("customCategories")
            if (catArr != null) {
                for (i in 0 until catArr.length()) {
                    val obj = catArr.getJSONObject(i)
                    categories.add(
                        CustomCategory(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            name = obj.optString("name", "Category"),
                            userEmail = obj.optString("userEmail", userEmail),
                            iconName = obj.optString("iconName", "category"),
                            colorHex = obj.optString("colorHex", "#00897B"),
                            type = obj.optString("type", "EXPENSE")
                        )
                    )
                }
            }

            // Forecast Incomes
            val forecasts = mutableListOf<ForecastIncome>()
            val fcArr = root.optJSONArray("forecastIncomes")
            if (fcArr != null) {
                for (i in 0 until fcArr.length()) {
                    val obj = fcArr.getJSONObject(i)
                    val bullets = mutableListOf<String>()
                    obj.optJSONArray("bulletPoints")?.let { bArr ->
                        for (b in 0 until bArr.length()) bullets.add(bArr.getString(b))
                    }
                    val completed = mutableListOf<Int>()
                    obj.optJSONArray("completedBullets")?.let { cArr ->
                        for (c in 0 until cArr.length()) completed.add(cArr.getInt(c))
                    }
                    forecasts.add(
                        ForecastIncome(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            title = obj.optString("title", "Forecast"),
                            amount = obj.optDouble("amount", 0.0),
                            expectedDate = obj.optLong("expectedDate", System.currentTimeMillis()),
                            category = obj.optString("category", "General"),
                            status = obj.optString("status", "PENDING"),
                            notes = obj.optString("notes", ""),
                            bulletPoints = bullets,
                            completedBullets = completed,
                            isRealized = obj.optBoolean("isRealized", false),
                            userEmail = obj.optString("userEmail", userEmail),
                            colorTag = obj.optString("colorTag", "#392720"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Future Income Notes
            val notes = mutableListOf<FutureIncomeNote>()
            val notesArr = root.optJSONArray("futureIncomeNotes")
            if (notesArr != null) {
                for (i in 0 until notesArr.length()) {
                    val obj = notesArr.getJSONObject(i)
                    val bullets = mutableListOf<String>()
                    obj.optJSONArray("bulletPoints")?.let { bArr ->
                        for (b in 0 until bArr.length()) bullets.add(bArr.getString(b))
                    }
                    val completed = mutableListOf<Int>()
                    obj.optJSONArray("completedBullets")?.let { cArr ->
                        for (c in 0 until cArr.length()) completed.add(cArr.getInt(c))
                    }
                    notes.add(
                        FutureIncomeNote(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            title = obj.optString("title", "Note"),
                            content = obj.optString("content", ""),
                            bulletPoints = bullets,
                            completedBullets = completed,
                            userEmail = obj.optString("userEmail", userEmail),
                            colorTag = obj.optString("colorTag", "#392720"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Audit Deleted Items
            val auditItems = mutableListOf<AuditDeletedItem>()
            val auditArr = root.optJSONArray("auditDeletedItems")
            if (auditArr != null) {
                for (i in 0 until auditArr.length()) {
                    val obj = auditArr.getJSONObject(i)
                    auditItems.add(
                        AuditDeletedItem(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            originalId = obj.optString("originalId", ""),
                            itemType = obj.optString("itemType", "TRANSACTION"),
                            title = obj.optString("title", "Item"),
                            amount = obj.optDouble("amount", 0.0),
                            categoryOrStatus = obj.optString("categoryOrStatus", ""),
                            details = obj.optString("details", ""),
                            sourceOrDeletedBy = obj.optString("sourceOrDeletedBy", "User Action"),
                            deletedAt = obj.optLong("deletedAt", System.currentTimeMillis()),
                            originalDate = obj.optLong("originalDate", 0L),
                            userEmail = obj.optString("userEmail", userEmail),
                            payloadJson = obj.optString("payloadJson", "")
                        )
                    )
                }
            }

            Result.success(
                BackupData(
                    version = version,
                    appName = appName,
                    exportDate = exportDate,
                    formattedDate = formattedDate,
                    userEmail = userEmail,
                    settings = settings,
                    transactions = transactions,
                    recurringTransactions = recurring,
                    customCategories = categories,
                    forecastIncomes = forecasts,
                    futureIncomeNotes = notes,
                    auditDeletedItems = auditItems
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // LOCAL BACKUPS (Device Storage)
    // ==========================================

    suspend fun createLocalBackup(context: Context, backupData: BackupData): Result<File> = withContext(Dispatchers.IO) {
        try {
            val dir = getBackupsDirectory(context)
            val fileName = "ledgerly_backup_${fileDateFormat.format(Date(backupData.exportDate))}.json"
            val file = File(dir, fileName)
            val jsonString = serializeBackupData(backupData)

            FileOutputStream(file).use { fos ->
                fos.write(jsonString.toByteArray(Charsets.UTF_8))
            }
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listLocalBackups(context: Context): List<BackupMetadata> {
        val dir = getBackupsDirectory(context)
        val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".json") } ?: return emptyList()

        return files.mapNotNull { file ->
            try {
                val json = file.readText(Charsets.UTF_8)
                val root = JSONObject(json)
                val exportDate = root.optLong("exportDate", file.lastModified())
                val formatted = root.optString("formattedDate", dateFormat.format(Date(exportDate)))
                val txCount = root.optJSONArray("transactions")?.length() ?: 0
                val recCount = root.optJSONArray("recurringTransactions")?.length() ?: 0
                val catCount = root.optJSONArray("customCategories")?.length() ?: 0
                val fcCount = root.optJSONArray("forecastIncomes")?.length() ?: 0
                val notesCount = root.optJSONArray("futureIncomeNotes")?.length() ?: 0
                val auditCount = root.optJSONArray("auditDeletedItems")?.length() ?: 0

                BackupMetadata(
                    id = file.name,
                    timestamp = exportDate,
                    formattedDate = formatted,
                    transactionsCount = txCount,
                    recurringCount = recCount,
                    categoriesCount = catCount,
                    forecastCount = fcCount,
                    notesCount = notesCount,
                    auditCount = auditCount,
                    isCloud = false,
                    fileSizeBytes = file.length(),
                    fileName = file.name
                )
            } catch (e: Exception) {
                null
            }
        }.sortedByDescending { it.timestamp }
    }

    fun deleteLocalBackup(context: Context, fileName: String): Boolean {
        val dir = getBackupsDirectory(context)
        val file = File(dir, fileName)
        return if (file.exists()) file.delete() else false
    }

    suspend fun readBackupFromFile(file: File): Result<BackupData> = withContext(Dispatchers.IO) {
        try {
            val json = file.readText(Charsets.UTF_8)
            parseBackupData(json)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun readBackupFromUri(context: Context, uri: Uri): Result<BackupData> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val json = contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            } ?: return@withContext Result.failure(Exception("Unable to read selected file stream."))
            parseBackupData(json)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun shareBackupFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Ledgerly Backup (${file.name})")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Share or Save Backup File")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    // ==========================================
    // CLOUD BACKUPS (Google Firebase Firestore - Spark Free Tier)
    // ==========================================

    fun isCloudAvailable(context: Context): Boolean {
        return try {
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun createCloudBackup(userEmail: String, backupData: BackupData): Result<String> = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            try {
                val firestore = FirebaseFirestore.getInstance()
                val backupId = "backup_${backupData.exportDate}"
                val jsonPayload = serializeBackupData(backupData)

                val docData = hashMapOf(
                    "id" to backupId,
                    "createdAt" to backupData.exportDate,
                    "formattedDate" to if (backupData.formattedDate.isNotBlank()) backupData.formattedDate else dateFormat.format(Date(backupData.exportDate)),
                    "userEmail" to userEmail,
                    "transactionsCount" to backupData.transactions.size,
                    "recurringCount" to backupData.recurringTransactions.size,
                    "categoriesCount" to backupData.customCategories.size,
                    "forecastCount" to backupData.forecastIncomes.size,
                    "notesCount" to backupData.futureIncomeNotes.size,
                    "auditCount" to backupData.auditDeletedItems.size,
                    "payloadJson" to jsonPayload
                )

                firestore.collection("users")
                    .document(userEmail)
                    .collection("backups")
                    .document(backupId)
                    .set(docData)
                    .addOnSuccessListener {
                        if (continuation.isActive) continuation.resume(Result.success(backupId))
                    }
                    .addOnFailureListener { err ->
                        if (continuation.isActive) continuation.resume(Result.failure(err))
                    }
            } catch (e: Exception) {
                if (continuation.isActive) continuation.resume(Result.failure(e))
            }
        }
    }

    suspend fun listCloudBackups(userEmail: String): Result<List<BackupMetadata>> = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            try {
                val firestore = FirebaseFirestore.getInstance()
                firestore.collection("users")
                    .document(userEmail)
                    .collection("backups")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val list = snapshot.documents.mapNotNull { doc ->
                            val id = doc.getString("id") ?: doc.id
                            val createdAt = doc.getLong("createdAt") ?: 0L
                            val formatted = doc.getString("formattedDate") ?: dateFormat.format(Date(createdAt))
                            val txCount = doc.getLong("transactionsCount")?.toInt() ?: 0
                            val recCount = doc.getLong("recurringCount")?.toInt() ?: 0
                            val catCount = doc.getLong("categoriesCount")?.toInt() ?: 0
                            val fcCount = doc.getLong("forecastCount")?.toInt() ?: 0
                            val notesCount = doc.getLong("notesCount")?.toInt() ?: 0
                            val auditCount = doc.getLong("auditCount")?.toInt() ?: 0
                            val payload = doc.getString("payloadJson") ?: ""

                            BackupMetadata(
                                id = id,
                                timestamp = createdAt,
                                formattedDate = formatted,
                                transactionsCount = txCount,
                                recurringCount = recCount,
                                categoriesCount = catCount,
                                forecastCount = fcCount,
                                notesCount = notesCount,
                                auditCount = auditCount,
                                isCloud = true,
                                fileSizeBytes = payload.toByteArray(Charsets.UTF_8).size.toLong(),
                                fileName = "$id.json"
                            )
                        }
                        if (continuation.isActive) continuation.resume(Result.success(list))
                    }
                    .addOnFailureListener { err ->
                        if (continuation.isActive) continuation.resume(Result.failure(err))
                    }
            } catch (e: Exception) {
                if (continuation.isActive) continuation.resume(Result.failure(e))
            }
        }
    }

    suspend fun fetchCloudBackupData(userEmail: String, backupId: String): Result<BackupData> = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            try {
                val firestore = FirebaseFirestore.getInstance()
                firestore.collection("users")
                    .document(userEmail)
                    .collection("backups")
                    .document(backupId)
                    .get()
                    .addOnSuccessListener { doc ->
                        val payload = doc.getString("payloadJson")
                        if (payload.isNullOrBlank()) {
                            if (continuation.isActive) continuation.resume(Result.failure(Exception("Cloud backup payload is empty")))
                        } else {
                            val parsed = parseBackupData(payload)
                            if (continuation.isActive) continuation.resume(parsed)
                        }
                    }
                    .addOnFailureListener { err ->
                        if (continuation.isActive) continuation.resume(Result.failure(err))
                    }
            } catch (e: Exception) {
                if (continuation.isActive) continuation.resume(Result.failure(e))
            }
        }
    }

    suspend fun deleteCloudBackup(userEmail: String, backupId: String): Result<Unit> = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            try {
                val firestore = FirebaseFirestore.getInstance()
                firestore.collection("users")
                    .document(userEmail)
                    .collection("backups")
                    .document(backupId)
                    .delete()
                    .addOnSuccessListener {
                        if (continuation.isActive) continuation.resume(Result.success(Unit))
                    }
                    .addOnFailureListener { err ->
                        if (continuation.isActive) continuation.resume(Result.failure(err))
                    }
            } catch (e: Exception) {
                if (continuation.isActive) continuation.resume(Result.failure(e))
            }
        }
    }
}
