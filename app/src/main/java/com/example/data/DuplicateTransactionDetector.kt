package com.example.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Utility for detecting potential duplicate transactions within the ledger.
 * A transaction is flagged as a potential duplicate if its amount, category,
 * and calendar date are identical to an existing entry.
 */
object DuplicateTransactionDetector {

    /**
     * Checks if two timestamps represent the exact same calendar day (Year and Day of Year).
     */
    fun isSameCalendarDay(date1: Long, date2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = date1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Checks if a given amount, category, and date match an existing transaction.
     */
    fun isPotentialDuplicate(
        amount: Double,
        category: String,
        date: Long,
        existingTx: Transaction,
        excludeId: String? = null
    ): Boolean {
        if (!excludeId.isNullOrEmpty() && existingTx.id == excludeId) {
            return false
        }
        if (amount <= 0.0 || category.isBlank()) {
            return false
        }
        val sameAmount = abs(existingTx.amount - amount) < 0.001
        val sameCategory = existingTx.category.trim().equals(category.trim(), ignoreCase = true)
        val sameDate = isSameCalendarDay(existingTx.date, date)
        return sameAmount && sameCategory && sameDate
    }

    /**
     * Finds all transactions in [transactions] that match the provided amount, category, and date.
     */
    fun findPotentialDuplicates(
        amount: Double,
        category: String,
        date: Long,
        transactions: List<Transaction>,
        excludeId: String? = null
    ): List<Transaction> {
        if (amount <= 0.0 || category.isBlank()) {
            return emptyList()
        }
        return transactions.filter { existing ->
            isPotentialDuplicate(amount, category, date, existing, excludeId)
        }
    }

    /**
     * Returns a set of all transaction IDs that have at least one other transaction
     * with the identical amount, category, and calendar date in the given list.
     */
    fun findAllDuplicateIds(transactions: List<Transaction>): Set<String> {
        val duplicateIds = mutableSetOf<String>()
        val map = mutableMapOf<String, MutableList<Transaction>>()
        val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        for (tx in transactions) {
            if (tx.amount <= 0.0 || tx.category.isBlank()) continue
            val dateKey = dayFormat.format(Date(tx.date))
            val amountKey = String.format(Locale.US, "%.2f", tx.amount)
            val catKey = tx.category.trim().lowercase(Locale.ROOT)
            val groupKey = "$amountKey|$catKey|$dateKey"

            val list = map.getOrPut(groupKey) { mutableListOf() }
            list.add(tx)
        }

        for ((_, group) in map) {
            if (group.size > 1) {
                group.forEach { duplicateIds.add(it.id) }
            }
        }
        return duplicateIds
    }

    /**
     * Returns other transactions sharing identical amount, category, and date with [tx].
     */
    fun findMatchingDuplicatesFor(
        tx: Transaction,
        allTransactions: List<Transaction>
    ): List<Transaction> {
        return findPotentialDuplicates(
            amount = tx.amount,
            category = tx.category,
            date = tx.date,
            transactions = allTransactions,
            excludeId = tx.id
        )
    }
}
