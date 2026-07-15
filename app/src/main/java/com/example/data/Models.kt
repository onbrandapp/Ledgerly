package com.example.data

data class Transaction(
    val id: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val type: String = "EXPENSE", // "EXPENSE" or "INCOME"
    val description: String = "",
    val date: Long = System.currentTimeMillis()
) {
    // Zero-argument constructor required for Firestore deserialization
    constructor() : this("", 0.0, "", "EXPENSE", "", System.currentTimeMillis())
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
