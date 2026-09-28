package com.example.ui.screens.reports

import com.example.data.Transaction
import java.util.Locale
import kotlin.math.max

data class ProfitItemMetric(
    val description: String,
    val count: Int,
    val totalRevenue: Double,
    val totalCost: Double,
    val totalProfit: Double,
    val profitPercentage: Double, // ((totalRevenue - totalCost) / totalRevenue) * 100
    val latestDate: Long,
    val sampleCategory: String = ""
)

object ProfitMarginHelper {

    val costProfitRegex = Regex("""\s*\[Cost:\s*[^0-9]*([0-9.]+)(?:\s*\|\s*Profit:\s*([0-9.-]+)%?)?\]""")

    data class ParsedCostProfit(
        val cleanDescription: String,
        val cost: Double?,
        val profitPercentage: Double?,
        val hasCostTag: Boolean
    )

    fun parseTransaction(tx: Transaction): ParsedCostProfit {
        val match = costProfitRegex.find(tx.description)
        return if (match != null) {
            val costVal = match.groupValues.getOrNull(1)?.toDoubleOrNull()
            val profitVal = match.groupValues.getOrNull(2)?.toDoubleOrNull()
            val clean = tx.description.replace(costProfitRegex, "").trim()
            ParsedCostProfit(
                cleanDescription = if (clean.isNotBlank()) clean else tx.category.ifBlank { "Item" },
                cost = costVal,
                profitPercentage = profitVal,
                hasCostTag = true
            )
        } else {
            ParsedCostProfit(
                cleanDescription = if (tx.description.isNotBlank()) tx.description.trim() else tx.category.ifBlank { "Item" },
                cost = null,
                profitPercentage = null,
                hasCostTag = false
            )
        }
    }

    /**
     * Aggregates transactions by clean description into [ProfitItemMetric].
     * @param onlyWithCostData if true, only includes transactions with explicit [Cost: ...] tags.
     */
    fun aggregateByDescription(
        transactions: List<Transaction>,
        onlyWithCostData: Boolean = true
    ): List<ProfitItemMetric> {
        val incomeList = transactions.filter { it.type.equals("INCOME", ignoreCase = true) }
        val map = mutableMapOf<String, MutableList<Pair<Transaction, ParsedCostProfit>>>()

        for (tx in incomeList) {
            val parsed = parseTransaction(tx)
            if (onlyWithCostData && !parsed.hasCostTag) {
                continue
            }
            val key = parsed.cleanDescription.lowercase(Locale.ROOT)
            val list = map.getOrPut(key) { mutableListOf() }
            list.add(Pair(tx, parsed))
        }

        val result = mutableListOf<ProfitItemMetric>()
        for ((_, pairs) in map) {
            val displayDesc = pairs.first().second.cleanDescription
            val count = pairs.size
            val totalRevenue = pairs.sumOf { it.first.amount }
            val totalCost = pairs.sumOf { (tx, parsed) ->
                parsed.cost ?: 0.0
            }
            val totalProfit = totalRevenue - totalCost
            val profitPct = if (totalRevenue > 0.0) {
                ((totalRevenue - totalCost) / totalRevenue) * 100.0
            } else {
                0.0
            }
            val latestDate = pairs.maxOf { it.first.date }
            val sampleCategory = pairs.first().first.category

            result.add(
                ProfitItemMetric(
                    description = displayDesc,
                    count = count,
                    totalRevenue = totalRevenue,
                    totalCost = totalCost,
                    totalProfit = totalProfit,
                    profitPercentage = profitPct,
                    latestDate = latestDate,
                    sampleCategory = sampleCategory
                )
            )
        }

        // Default sort by profit percentage descending
        return result.sortedByDescending { it.profitPercentage }
    }
}
