package com.example.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Utility for detecting potential duplicate transactions within the ledger.
 * A transaction is flagged as a potential duplicate only if its description,
 * amount, category, and calendar date are identical to an existing entry.
 * If Description is different, the transactions are NOT duplicates.
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
     * Checks if a given description, amount, category, and date match an existing transaction.
     * If description is different (different customer, vendor, or note), they are not duplicates.
     */
    fun isPotentialDuplicate(
        description: String,
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
        val sameDescription = existingTx.description.trim().equals(description.trim(), ignoreCase = true)
        val sameAmount = abs(existingTx.amount - amount) < 0.001
        val sameCategory = existingTx.category.trim().equals(category.trim(), ignoreCase = true)
        val sameDate = isSameCalendarDay(existingTx.date, date)
        return sameDescription && sameAmount && sameCategory && sameDate
    }

    /**
     * Finds all transactions in [transactions] that match the provided description, amount, category, and date.
     */
    fun findPotentialDuplicates(
        description: String,
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
            isPotentialDuplicate(
                description = description,
                amount = amount,
                category = category,
                date = date,
                existingTx = existing,
                excludeId = excludeId
            )
        }
    }

    /**
     * Helper to compute consistent pair key for two transaction IDs.
     */
    fun getPairKey(id1: String, id2: String): String {
        return if (id1 < id2) "$id1:$id2" else "$id2:$id1"
    }

    fun isPairReconciled(id1: String, id2: String, reconciledPairs: Set<String>): Boolean {
        if (reconciledPairs.isEmpty() || id1.isBlank() || id2.isBlank()) return false
        return reconciledPairs.contains(getPairKey(id1, id2))
    }

    data class DuplicatePair(
        val tx1: Transaction,
        val tx2: Transaction
    ) {
        val pairKey: String get() = getPairKey(tx1.id, tx2.id)
    }

    /**
     * Returns a set of all transaction IDs that have at least one other transaction
     * with the identical description, amount, category, and calendar date in the given list,
     * ignoring any pairs that have been marked as reconciled.
     */
    fun findAllDuplicateIds(
        transactions: List<Transaction>,
        reconciledPairs: Set<String> = emptySet()
    ): Set<String> {
        val duplicateIds = mutableSetOf<String>()
        val map = mutableMapOf<String, MutableList<Transaction>>()
        val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        for (tx in transactions) {
            if (tx.amount <= 0.0 || tx.category.isBlank()) continue
            val dateKey = dayFormat.format(Date(tx.date))
            val amountKey = String.format(Locale.US, "%.2f", tx.amount)
            val catKey = tx.category.trim().lowercase(Locale.ROOT)
            val descKey = tx.description.trim().lowercase(Locale.ROOT)
            val groupKey = "$amountKey|$catKey|$descKey|$dateKey"

            val list = map.getOrPut(groupKey) { mutableListOf() }
            list.add(tx)
        }

        for ((_, group) in map) {
            if (group.size > 1) {
                for (i in group.indices) {
                    val a = group[i]
                    for (j in group.indices) {
                        if (i != j) {
                            val b = group[j]
                            if (!isPairReconciled(a.id, b.id, reconciledPairs)) {
                                duplicateIds.add(a.id)
                                duplicateIds.add(b.id)
                            }
                        }
                    }
                }
            }
        }
        return duplicateIds
    }

    /**
     * Returns all unique pairs of potential duplicate transactions that haven't been reconciled.
     */
    fun findDuplicatePairs(
        transactions: List<Transaction>,
        reconciledPairs: Set<String> = emptySet()
    ): List<DuplicatePair> {
        val pairs = mutableListOf<DuplicatePair>()
        val seenKeys = mutableSetOf<String>()
        val map = mutableMapOf<String, MutableList<Transaction>>()
        val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        for (tx in transactions) {
            if (tx.amount <= 0.0 || tx.category.isBlank()) continue
            val dateKey = dayFormat.format(Date(tx.date))
            val amountKey = String.format(Locale.US, "%.2f", tx.amount)
            val catKey = tx.category.trim().lowercase(Locale.ROOT)
            val descKey = tx.description.trim().lowercase(Locale.ROOT)
            val groupKey = "$amountKey|$catKey|$descKey|$dateKey"

            val list = map.getOrPut(groupKey) { mutableListOf() }
            list.add(tx)
        }

        for ((_, group) in map) {
            if (group.size > 1) {
                for (i in 0 until group.size - 1) {
                    for (j in i + 1 until group.size) {
                        val a = group[i]
                        val b = group[j]
                        val key = getPairKey(a.id, b.id)
                        if (!reconciledPairs.contains(key) && seenKeys.add(key)) {
                            pairs.add(DuplicatePair(a, b))
                        }
                    }
                }
            }
        }
        return pairs
    }

    /**
     * Returns other transactions sharing identical description, amount, category, and date with [tx],
     * excluding any that have been marked as reconciled with [tx].
     */
    fun findMatchingDuplicatesFor(
        tx: Transaction,
        allTransactions: List<Transaction>,
        reconciledPairs: Set<String> = emptySet()
    ): List<Transaction> {
        return findPotentialDuplicates(
            description = tx.description,
            amount = tx.amount,
            category = tx.category,
            date = tx.date,
            transactions = allTransactions,
            excludeId = tx.id
        ).filter { other ->
            !isPairReconciled(tx.id, other.id, reconciledPairs)
        }
    }
}
