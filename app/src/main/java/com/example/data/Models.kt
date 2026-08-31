package com.example.data

data class Transaction(
    val id: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val type: String = "EXPENSE", // "EXPENSE" or "INCOME"
    val description: String = "",
    val date: Long = System.currentTimeMillis(),
    val recurringId: String = "",
    val paid: Boolean = false
) {
    // Zero-argument constructor required for Firestore deserialization
    constructor() : this("", 0.0, "", "EXPENSE", "", System.currentTimeMillis(), "", false)
}

data class RecurringTransaction(
    val id: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val type: String = "EXPENSE", // "EXPENSE" or "INCOME"
    val description: String = "",
    val frequency: String = "MONTHLY", // "DAILY", "WEEKLY", "MONTHLY", "YEARLY"
    val startDate: Long = System.currentTimeMillis(),
    val lastLoggedDate: Long = 0L
) {
    // Zero-argument constructor required for Firestore deserialization
    constructor() : this("", 0.0, "", "EXPENSE", "", "MONTHLY", System.currentTimeMillis(), 0L)
}

data class CustomCategory(
    val id: String = "",
    val name: String = "",
    val userEmail: String = ""
) {
    // Zero-argument constructor required for Firestore deserialization
    constructor() : this("", "", "")
}

data class ForecastIncome(
    val id: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val expectedDate: Long = System.currentTimeMillis(),
    val category: String = "Freelance",
    val status: String = "EXPECTED", // "CONFIRMED", "EXPECTED", "TENTATIVE", "RECEIVED"
    val notes: String = "",
    val bulletPoints: List<String> = emptyList(),
    val completedBullets: List<Int> = emptyList(),
    val isRealized: Boolean = false,
    val userEmail: String = "",
    val colorTag: String = "#FFD97D",
    val createdAt: Long = System.currentTimeMillis()
) {
    // Zero-argument constructor required for Firestore deserialization
    constructor() : this("", "", 0.0, System.currentTimeMillis(), "Freelance", "EXPECTED", "", emptyList(), emptyList(), false, "", "#FFD97D", System.currentTimeMillis())
}

data class FutureIncomeNote(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val bulletPoints: List<String> = emptyList(),
    val completedBullets: List<Int> = emptyList(),
    val userEmail: String = "",
    val colorTag: String = "#FFD97D",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    // Zero-argument constructor required for Firestore deserialization
    constructor() : this("", "", "", emptyList(), emptyList(), "", "#FFD97D", System.currentTimeMillis(), System.currentTimeMillis())
}

