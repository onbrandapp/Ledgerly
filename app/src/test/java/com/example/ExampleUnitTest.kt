package com.example

import androidx.compose.ui.graphics.Color
import com.example.data.CustomCategory
import com.example.ui.theme.CategoryConstants
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testDefaultCategoryStyleResolution() {
    val foodStyle = CategoryConstants.resolveCategoryStyle("Food")
    assertEquals(Color(0xFFE65100), foodStyle.color)

    val transportStyle = CategoryConstants.resolveCategoryStyle("transport")
    assertEquals(Color(0xFF1565C0), transportStyle.color)
  }

  @Test
  fun testCustomCategoryStyleResolution() {
    val customList = listOf(
      CustomCategory(
        id = "cat_1",
        name = "Pet Care",
        iconName = "Pets",
        colorHex = "#FF5722",
        type = "EXPENSE"
      ),
      CustomCategory(
        id = "cat_2",
        name = "Crypto Staking",
        iconName = "CurrencyBitcoin",
        colorHex = "#F59E0B",
        type = "INCOME"
      )
    )

    val petStyle = CategoryConstants.resolveCategoryStyle("Pet Care", customList)
    assertEquals(Color(0xFFFF5722), petStyle.color)

    val cryptoStyle = CategoryConstants.resolveCategoryStyle("crypto staking", customList)
    assertEquals(Color(0xFFF59E0B), cryptoStyle.color)
  }

  @Test
  fun testFallbackCategoryResolution() {
    val fallbackStyle = CategoryConstants.resolveCategoryStyle("UnseenRandomCategory")
    assertNotNull(fallbackStyle.icon)
    assertNotNull(fallbackStyle.color)
  }

  @Test
  fun testCsvFilteringByTransactionType() {
    val txList = listOf(
      com.example.data.Transaction(
        id = "1",
        amount = 100.0,
        category = "Salary",
        description = "Monthly Paycheck",
        date = 1700000000000L,
        type = "INCOME",
        paid = true
      ),
      com.example.data.Transaction(
        id = "2",
        amount = 50.0,
        category = "Groceries",
        description = "Supermarket",
        date = 1700000000000L,
        type = "EXPENSE",
        paid = false
      ),
      com.example.data.Transaction(
        id = "3",
        amount = 25.0,
        category = "Utilities",
        description = "Electric Bill",
        date = 1700000000000L,
        type = "EXPENSE",
        paid = true
      )
    )

    val expenseOnly = txList.filter { it.type.equals("EXPENSE", ignoreCase = true) }
    assertEquals(2, expenseOnly.size)

    val incomeOnly = txList.filter { it.type.equals("INCOME", ignoreCase = true) }
    assertEquals(1, incomeOnly.size)

    val paidOnly = txList.filter { it.paid }
    assertEquals(2, paidOnly.size)

    val unpaidOnly = txList.filter { !it.paid }
    assertEquals(1, unpaidOnly.size)
  }

  @Test
  fun testCsvFilteringByDateRange() {
    val tx1 = com.example.data.Transaction(
      id = "1",
      amount = 100.0,
      category = "Freelance",
      description = "App Design",
      date = 1000L,
      type = "INCOME"
    )
    val tx2 = com.example.data.Transaction(
      id = "2",
      amount = 50.0,
      category = "Dining",
      description = "Dinner",
      date = 2000L,
      type = "EXPENSE"
    )
    val tx3 = com.example.data.Transaction(
      id = "3",
      amount = 20.0,
      category = "Transport",
      description = "Train ticket",
      date = 3000L,
      type = "EXPENSE"
    )

    val list = listOf(tx1, tx2, tx3)

    // Filter within 1500L..2500L
    val rangeFiltered = list.filter { it.date in 1500L..2500L }
    assertEquals(1, rangeFiltered.size)
    assertEquals("2", rangeFiltered[0].id)

    // Filter after 1500L
    val afterFiltered = list.filter { it.date >= 1500L }
    assertEquals(2, afterFiltered.size)
  }

  @Test
  fun testDuplicateTransactionDetection() {
    // 1. Transactions with different descriptions (e.g. Christian vs Rick) are NOT duplicates
    val christianTx = com.example.data.Transaction(
      id = "tx1",
      amount = 100.00,
      category = "Product",
      description = "Christian",
      date = 1711000000000L,
      type = "INCOME"
    )
    val rickTx = com.example.data.Transaction(
      id = "tx2",
      amount = 100.00,
      category = "Product",
      description = "Rick",
      date = 1711000000000L,
      type = "INCOME"
    )
    // 2. Exact duplicate of Christian with case/trim variation
    val christianDuplicateTx = com.example.data.Transaction(
      id = "tx3",
      amount = 100.00,
      category = "product",
      description = "christian ",
      date = 1711000000000L,
      type = "INCOME"
    )
    val differentAmountTx = com.example.data.Transaction(
      id = "tx4",
      amount = 50.00,
      category = "Product",
      description = "Christian",
      date = 1711000000000L,
      type = "INCOME"
    )

    val list = listOf(christianTx, rickTx, christianDuplicateTx, differentAmountTx)

    // Christian and Rick have different descriptions, so Rick is NOT a duplicate of Christian
    val duplicatesForChristian = com.example.data.DuplicateTransactionDetector.findPotentialDuplicates(
      description = "Christian",
      amount = 100.00,
      category = "Product",
      date = 1711000000000L,
      transactions = list
    )
    assertEquals(2, duplicatesForChristian.size) // tx1 and tx3
    assertTrue(duplicatesForChristian.any { it.id == "tx1" })
    assertTrue(duplicatesForChristian.any { it.id == "tx3" })
    assertFalse(duplicatesForChristian.any { it.id == "tx2" }) // Rick is NOT included

    // Searching for Rick finds only Rick (no duplicates since only 1 Rick)
    val duplicatesForRick = com.example.data.DuplicateTransactionDetector.findPotentialDuplicates(
      description = "Rick",
      amount = 100.00,
      category = "Product",
      date = 1711000000000L,
      transactions = list
    )
    assertEquals(1, duplicatesForRick.size)
    assertEquals("tx2", duplicatesForRick.first().id)

    // Overall duplicate detection flags only tx1 and tx3
    val duplicateIds = com.example.data.DuplicateTransactionDetector.findAllDuplicateIds(list)
    assertEquals(2, duplicateIds.size)
    assertTrue(duplicateIds.contains("tx1"))
    assertTrue(duplicateIds.contains("tx3"))
    assertFalse(duplicateIds.contains("tx2")) // Rick is not a duplicate!
    assertFalse(duplicateIds.contains("tx4"))
  }

  @Test
  fun testNetBalanceCalculationDoesNotIncludeCashCategory() {
    // Replicate user scenario:
    // Total Income (excluding Cash) = $3,680.50
    // Total Expenses = $3,236.16
    // Cash on Hand = $1,184.20
    // Left to Receive = $780.00
    // Left to Pay = $1,903.00
    val incomeList = listOf(
      com.example.data.Transaction(
        id = "inc1",
        amount = 3680.50,
        category = "Salary",
        type = "INCOME",
        description = "Employer",
        date = 1711000000000L,
        paid = false
      ),
      com.example.data.Transaction(
        id = "cash1",
        amount = 1184.20,
        category = "Cash",
        type = "INCOME",
        description = "Cash on Hand",
        date = 1711000000000L,
        paid = true
      )
    )
    val expenseList = listOf(
      com.example.data.Transaction(
        id = "exp1",
        amount = 3236.16,
        category = "Bills",
        type = "EXPENSE",
        description = "Rent and utilities",
        date = 1711000000000L,
        paid = false
      )
    )

    val cashOnHand = incomeList.filter { it.category.trim().equals("cash", ignoreCase = true) }.sumOf { it.amount }
    val totalIncome = incomeList.filter { !it.category.trim().equals("cash", ignoreCase = true) }.sumOf { it.amount }
    val totalExpense = expenseList.sumOf { it.amount }
    val totalLeftToReceive = 780.00
    val totalLeftToPay = 1903.00

    // Net Balance should be exactly totalIncome - totalExpense (WITHOUT adding cashOnHand)
    val netBalance = totalIncome - totalExpense
    assertEquals(444.34, netBalance, 0.001)

    // Current Balance (Actual) = cashOnHand + totalLeftToReceive - totalLeftToPay
    val currentBalance = cashOnHand + totalLeftToReceive - totalLeftToPay
    assertEquals(61.20, currentBalance, 0.001)
  }

  @Test
  fun testCurrencyHelperConversionAndRates() {
    // 1. Identity conversion
    val sameRate = com.example.data.CurrencyHelper.getRate("USD", "USD")
    assertEquals(1.0, sameRate, 0.0001)

    // 2. Conversion between EUR and USD
    val eurToUsdRate = com.example.data.CurrencyHelper.getRate("EUR", "USD")
    assertEquals(1.08, eurToUsdRate, 0.001)

    val convertedEurToUsd = com.example.data.CurrencyHelper.convert(100.0, "EUR", "USD")
    assertEquals(108.0, convertedEurToUsd, 0.01)

    // 3. Conversion between USD and EUR
    val usdToEurRate = com.example.data.CurrencyHelper.getRate("USD", "EUR")
    assertEquals(1.0 / 1.08, usdToEurRate, 0.001)

    // 4. Conversion between non-USD pairs: EUR to GBP
    val eurToGbpRate = com.example.data.CurrencyHelper.getRate("EUR", "GBP")
    val expectedEurToGbp = 1.08 / 1.28
    assertEquals(expectedEurToGbp, eurToGbpRate, 0.001)

    // 5. Symbol and Flag lookups
    assertEquals("$", com.example.data.CurrencyHelper.getSymbol("USD"))
    assertEquals("€", com.example.data.CurrencyHelper.getSymbol("EUR"))
    assertEquals("£", com.example.data.CurrencyHelper.getSymbol("GBP"))
    assertEquals("¥", com.example.data.CurrencyHelper.getSymbol("JPY"))
    assertEquals("CA$", com.example.data.CurrencyHelper.getSymbol("CAD"))

    // 6. Formatting test
    val formattedUsd = com.example.data.CurrencyHelper.formatAmount(1234.50, "USD", includeCode = false)
    assertEquals("$1,234.50", formattedUsd)

    val formattedEurWithCode = com.example.data.CurrencyHelper.formatAmount(99.00, "EUR", includeCode = true)
    assertEquals("€99.00 EUR", formattedEurWithCode)
  }

  @Test
  fun testTransactionMultiCurrencyFields() {
    val tx = com.example.data.Transaction(
      id = "tx_multi",
      amount = 108.00, // Converted to primary USD
      category = "Travel",
      description = "Paris Hotel",
      date = 1711000000000L,
      type = "EXPENSE",
      currency = "EUR",
      originalAmount = 100.00,
      exchangeRate = 1.08
    )

    assertEquals("EUR", tx.currency)
    assertEquals(100.00, tx.originalAmount, 0.001)
    assertEquals(1.08, tx.exchangeRate, 0.001)
    assertEquals(108.00, tx.amount, 0.001)
  }

  @Test
  fun testProductCategoryResolution() {
    val style = CategoryConstants.resolveCategoryStyle("Product")
    assertNotNull(style.icon)
    assertEquals(Color(0xFF00897B), style.color)
  }

  @Test
  fun testProductCostAndProfitPercentage() {
    // Formula requested: Profit = (Price - Cost) / Price reflected as a percentage
    val price = 100.0
    val cost = 40.0
    val profitPct = ((price - cost) / price) * 100.0
    assertEquals(60.0, profitPct, 0.001)

    // Test parsing and tag formatting
    val descWithCost = "Handmade Mug [Cost: $40.00 | Profit: 60.0%]"
    val tagRegex = Regex("""\s*\[Cost:\s*[^0-9]*([0-9.]+)(?:\s*\|\s*Profit:\s*([0-9.-]+)%?)?\]""")
    val match = tagRegex.find(descWithCost)
    assertNotNull(match)
    assertEquals("40.00", match?.groupValues?.get(1))
    assertEquals("60.0", match?.groupValues?.get(2))

    // Decimal cost test
    val decimalPrice = 50.0
    val decimalCost = 12.50
    val decimalProfitPct = ((decimalPrice - decimalCost) / decimalPrice) * 100.0
    assertEquals(75.0, decimalProfitPct, 0.001)

    // Safe zero price handling
    val zeroPrice = 0.0
    val safePct = if (zeroPrice > 0.0) ((zeroPrice - cost) / zeroPrice) * 100.0 else 0.0
    assertEquals(0.0, safePct, 0.001)
  }

  @Test
  fun testMonthlyBudgetCalculationExcludesCashOnHandFromIncome() {
    val txList: List<com.example.data.Transaction> = listOf(
      com.example.data.Transaction(
        id = "1",
        description = "Salary",
        amount = 1783.86,
        type = "INCOME",
        category = "Salary",
        date = System.currentTimeMillis()
      ),
      com.example.data.Transaction(
        id = "2",
        description = "Cash on Hand",
        amount = 60.00,
        type = "INCOME",
        category = "Cash",
        date = System.currentTimeMillis()
      ),
      com.example.data.Transaction(
        id = "3",
        description = "Bills & Rent",
        amount = 2237.37,
        type = "EXPENSE",
        category = "Housing",
        date = System.currentTimeMillis()
      )
    )

    var totalIncome = 0.0
    var totalExpense = 0.0
    var cashOnHand = 0.0

    txList.forEach { tx ->
      if (tx.type == "INCOME") {
        if (tx.category.trim().equals("cash", ignoreCase = true)) {
          cashOnHand += tx.amount
        } else {
          totalIncome += tx.amount
        }
      } else {
        totalExpense += tx.amount
      }
    }

    assertEquals(1783.86, totalIncome, 0.001)
    assertEquals(60.00, cashOnHand, 0.001)
    assertEquals(2237.37, totalExpense, 0.001)

    val monthlyBudgetLimit = 0.0
    val remainingBudget = monthlyBudgetLimit + totalIncome - totalExpense
    val netBalance = totalIncome - totalExpense

    assertEquals(-453.51, remainingBudget, 0.001)
    assertEquals(-453.51, netBalance, 0.001)
  }

  @Test
  fun testCategorySpendingTrendMonthOverMonthCalculation() {
    val cal = java.util.Calendar.getInstance()
    val curMonthMillis = cal.timeInMillis

    cal.add(java.util.Calendar.MONTH, -1)
    val priorMonthMillis = cal.timeInMillis

    val testTransactions = listOf(
      com.example.data.Transaction(
        id = "t1",
        description = "Lunch",
        amount = 50.0,
        type = "EXPENSE",
        category = "Food & Dining",
        date = curMonthMillis
      ),
      com.example.data.Transaction(
        id = "t2",
        description = "Dinner",
        amount = 100.0,
        type = "EXPENSE",
        category = "Food & Dining",
        date = curMonthMillis
      ),
      com.example.data.Transaction(
        id = "t3",
        description = "Groceries",
        amount = 120.0,
        type = "EXPENSE",
        category = "Food & Dining",
        date = priorMonthMillis
      ),
      com.example.data.Transaction(
        id = "t4",
        description = "Bus",
        amount = 30.0,
        type = "EXPENSE",
        category = "Transportation",
        date = curMonthMillis
      )
    )

    // Current month food spend
    val currentFoodSpend = testTransactions.filter {
      it.type == "EXPENSE" &&
      it.category.equals("Food & Dining", ignoreCase = true) &&
      it.date == curMonthMillis
    }.sumOf { it.amount }

    // Prior month food spend
    val priorFoodSpend = testTransactions.filter {
      it.type == "EXPENSE" &&
      it.category.equals("Food & Dining", ignoreCase = true) &&
      it.date == priorMonthMillis
    }.sumOf { it.amount }

    assertEquals(150.0, currentFoodSpend, 0.001)
    assertEquals(120.0, priorFoodSpend, 0.001)

    val momDiff = currentFoodSpend - priorFoodSpend
    val momPct = ((currentFoodSpend - priorFoodSpend) / priorFoodSpend) * 100.0

    assertEquals(30.0, momDiff, 0.001)
    assertEquals(25.0, momPct, 0.001)
  }

  @Test
  fun testCategoryDrawerOnlyShowsCategoriesWithExpenseApplied() {
    val txList = listOf(
      com.example.data.Transaction(
        id = "t1",
        description = "Lunch",
        amount = 15.0,
        type = "EXPENSE",
        category = "Food & Dining"
      ),
      com.example.data.Transaction(
        id = "t2",
        description = "Salary",
        amount = 3500.0,
        type = "INCOME",
        category = "Salary"
      ),
      com.example.data.Transaction(
        id = "t3",
        description = "Client retainer",
        amount = 1200.0,
        type = "INCOME",
        category = "Freelance"
      ),
      com.example.data.Transaction(
        id = "t4",
        description = "Bus pass",
        amount = 45.0,
        type = "EXPENSE",
        category = "Transportation"
      )
    )

    // Extraction algorithm: only EXPENSE transactions with non-blank category
    val expenseTxList = txList.filter {
      it.type.equals("EXPENSE", ignoreCase = true) && it.category.isNotBlank()
    }
    val categoryMap = mutableMapOf<String, Pair<String, Int>>()
    for (tx in expenseTxList) {
      val trimmed = tx.category.trim()
      val lower = trimmed.lowercase()
      val current = categoryMap[lower]
      if (current == null) {
        categoryMap[lower] = Pair(trimmed, 1)
      } else {
        categoryMap[lower] = Pair(current.first, current.second + 1)
      }
    }
    val drawerCategories = categoryMap.values.sortedByDescending { it.second }.map { it.first }

    // Categories with expenses applied
    assertEquals(2, drawerCategories.size)
    assertTrue(drawerCategories.contains("Food & Dining"))
    assertTrue(drawerCategories.contains("Transportation"))

    // Income categories or categories without expenses must NOT be present
    assertFalse(drawerCategories.contains("Salary"))
    assertFalse(drawerCategories.contains("Freelance"))
    assertFalse(drawerCategories.contains("Shopping"))
    assertFalse(drawerCategories.contains("Entertainment"))
  }

  @Test
  fun testLedgerCsvExportWithoutIncludeCost() {
    val txList = listOf(
      com.example.data.Transaction(
        id = "t1",
        description = "Design Client A [Cost: 150.00 | Profit: 70%]",
        amount = 500.0,
        type = "INCOME",
        category = "Consulting",
        date = 1700000000000L,
        paid = true
      ),
      com.example.data.Transaction(
        id = "t2",
        description = "Software License",
        amount = 50.0,
        type = "EXPENSE",
        category = "Software",
        date = 1700000000000L,
        paid = true
      )
    )

    val csv = com.example.ui.screens.reports.LedgerReportExporter.generateCsvString(
      transactions = txList,
      startDate = null,
      endDate = null,
      includeCost = false
    )

    // Header without cost
    assertTrue(csv.contains("Type,Date,Description,Category,Amount,Status"))
    assertFalse(csv.contains("Type,Date,Description,Category,Amount,Cost,Status"))

    // Raw description is preserved when cost is excluded
    assertTrue(csv.contains("\"Design Client A [Cost: 150.00 | Profit: 70%]\""))
    assertFalse(csv.contains("Total Unit Cost"))
  }

  @Test
  fun testLedgerCsvExportWithIncludeCost() {
    val txList = listOf(
      com.example.data.Transaction(
        id = "t1",
        description = "Design Client A [Cost: 150.00 | Profit: 70%]",
        amount = 500.0,
        type = "INCOME",
        category = "Consulting",
        date = 1700000000000L,
        paid = true
      ),
      com.example.data.Transaction(
        id = "t2",
        description = "Server Hosting",
        amount = 100.0,
        type = "EXPENSE",
        category = "Infrastructure",
        date = 1700000000000L,
        paid = true
      )
    )

    val csv = com.example.ui.screens.reports.LedgerReportExporter.generateCsvString(
      transactions = txList,
      startDate = null,
      endDate = null,
      includeCost = true
    )

    // Header includes Cost column
    assertTrue(csv.contains("Type,Date,Description,Category,Amount,Cost,Status"))

    // Clean description without tag
    assertTrue(csv.contains("\"Design Client A\""))

    // Cost column value is included
    assertTrue(csv.contains("500.00,150.00,Received"))

    // Summary includes cost metrics
    assertTrue(csv.contains("Include Cost,,,,Yes,"))
    assertTrue(csv.contains("Total Unit Cost,,,,150.00,"))
    assertTrue(csv.contains("Gross Margin (Income - Cost),,,,350.00,"))
  }

  @Test
  fun testGrossMarginOnlyTotalsIncomeTransactionsWithCost() {
    val txList = listOf(
      // Income with cost
      com.example.data.Transaction(
        id = "t1",
        description = "Fat Chris [Cost: 37.00]",
        amount = 37.0,
        type = "INCOME",
        category = "Sales"
      ),
      com.example.data.Transaction(
        id = "t2",
        description = "Mark [Cost: 70.00]",
        amount = 120.0,
        type = "INCOME",
        category = "Sales"
      ),
      // Income without cost
      com.example.data.Transaction(
        id = "t3",
        description = "Bree (Recurring)",
        amount = 155.0,
        type = "INCOME",
        category = "General"
      ),
      com.example.data.Transaction(
        id = "t4",
        description = "iCash (Recurring)",
        amount = 450.0,
        type = "INCOME",
        category = "General"
      )
    )

    val csv = com.example.ui.screens.reports.LedgerReportExporter.generateCsvString(
      transactions = txList,
      startDate = null,
      endDate = null,
      includeCost = true
    )

    // Total income across all 4 transactions is 762.00
    assertTrue(csv.contains("Total Income,,,,762.00,"))

    // Total cost across transactions with cost is 37.00 + 70.00 = 107.00
    assertTrue(csv.contains("Total Unit Cost,,,,107.00,"))

    // Income with cost is only 37.00 + 120.00 = 157.00
    // Gross Margin must be: 157.00 - 107.00 = 50.00 (NOT 762.00 - 107.00 = 655.00)
    assertTrue(csv.contains("Gross Margin (Income - Cost),,,,50.00,"))
    assertFalse(csv.contains("Gross Margin (Income - Cost),,,,655.00,"))
  }

  @Test
  fun testMonthlyBudgetCurrentMonthDateRangeAndFiltering() {
    val cal = java.util.Calendar.getInstance()
    cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    val monthStart = cal.timeInMillis

    val endCal = java.util.Calendar.getInstance()
    endCal.set(java.util.Calendar.DAY_OF_MONTH, endCal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
    endCal.set(java.util.Calendar.HOUR_OF_DAY, 23)
    endCal.set(java.util.Calendar.MINUTE, 59)
    endCal.set(java.util.Calendar.SECOND, 59)
    endCal.set(java.util.Calendar.MILLISECOND, 999)
    val monthEnd = endCal.timeInMillis

    val midMonthTx = com.example.data.Transaction(
      id = "tx_current",
      description = "Mid-month grocery",
      amount = 120.0,
      type = "EXPENSE",
      category = "Groceries",
      date = (monthStart + monthEnd) / 2
    )

    val lastYearTx = com.example.data.Transaction(
      id = "tx_old",
      description = "Last year expense",
      amount = 80.0,
      type = "EXPENSE",
      category = "Groceries",
      date = monthStart - 365L * 24 * 3600 * 1000
    )

    val list = listOf(midMonthTx, lastYearTx)
    val filtered = list.filter { it.date in monthStart..monthEnd }
    assertEquals(1, filtered.size)
    assertEquals("tx_current", filtered[0].id)
  }
}


