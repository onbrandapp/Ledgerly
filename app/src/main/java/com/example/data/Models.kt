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
