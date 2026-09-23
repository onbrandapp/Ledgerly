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
}

