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
}

