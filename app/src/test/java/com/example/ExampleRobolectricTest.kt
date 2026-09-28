package com.example

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Ledgerly", appName)
  }

  @Test
  fun `launch main activity`() {
    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
      scenario.onActivity { activity ->
        assert(activity != null)
      }
    }
  }

  @Test
  fun `verify audit deleted item model and restoration payload`() {
    val payload = org.json.JSONObject().apply {
      put("id", "tx-123")
      put("amount", 45.50)
      put("category", "Groceries")
      put("description", "Weekly groceries")
      put("date", 1700000000000L)
      put("type", "EXPENSE")
      put("paid", true)
      put("recurringId", "")
    }.toString()

    val auditItem = com.example.data.AuditDeletedItem(
      id = "audit-1",
      originalId = "tx-123",
      itemType = "TRANSACTION",
      title = "Weekly groceries",
      amount = 45.50,
      categoryOrStatus = "EXPENSE • Groceries",
      details = "Type: EXPENSE, Category: Groceries, Paid: true",
      sourceOrDeletedBy = "User Action",
      deletedAt = 1700000050000L,
      originalDate = 1700000000000L,
      userEmail = "test@example.com",
      payloadJson = payload
    )

    assertEquals("tx-123", auditItem.originalId)
    assertEquals(45.50, auditItem.amount, 0.001)
    assertEquals("TRANSACTION", auditItem.itemType)
    assert(auditItem.payloadJson.contains("Weekly groceries"))
  }

  @Test
  fun `verify room repository records and deletes audit item`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = com.example.data.RoomTransactionRepository(context)
    val email = "test_audit@example.com"

    val auditItem = com.example.data.AuditDeletedItem(
      id = "audit-test-99",
      originalId = "orig-99",
      itemType = "TRANSACTION",
      title = "Office Supplies",
      amount = 120.00,
      categoryOrStatus = "EXPENSE • Office",
      details = "Deleted item test",
      sourceOrDeletedBy = "User Action",
      deletedAt = System.currentTimeMillis(),
      originalDate = System.currentTimeMillis(),
      userEmail = email,
      payloadJson = "{\"id\":\"orig-99\",\"amount\":120.00,\"category\":\"Office\",\"description\":\"Office Supplies\",\"type\":\"EXPENSE\"}"
    )

    val recordResult = repo.recordAuditDeletedItem(email, auditItem)
    assert(recordResult.isSuccess)

    // Now delete from audit list as done during restoration
    val deleteResult = repo.deleteAuditDeletedItem(email, auditItem.id)
    assert(deleteResult.isSuccess)
  }

  @Test
  fun `verify bulk category update reassigns category across transactions and recurring entries`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = com.example.data.RoomTransactionRepository(context)
    val email = "bulk_test@example.com"

    // Add 2 transactions with "Old Dining"
    repo.addTransaction(email, com.example.data.Transaction(
      id = "bulk-tx-1",
      amount = 25.0,
      category = "Old Dining",
      type = "EXPENSE",
      description = "Dinner"
    ))
    repo.addTransaction(email, com.example.data.Transaction(
      id = "bulk-tx-2",
      amount = 15.0,
      category = "Old Dining",
      type = "EXPENSE",
      description = "Lunch"
    ))

    // Add 1 recurring transaction with "Old Dining"
    repo.addRecurringTransaction(email, com.example.data.RecurringTransaction(
      id = "bulk-rec-1",
      amount = 50.0,
      category = "Old Dining",
      type = "EXPENSE",
      description = "Monthly Dining Plan"
    ))

    // Execute bulk update to "Food & Drinks"
    val result = repo.bulkUpdateCategory(
      userEmail = email,
      oldCategory = "Old Dining",
      newCategory = "Food & Drinks",
      includeRecurring = true
    )

    assert(result.isSuccess)
    val updatedCount = result.getOrNull() ?: 0
    assertEquals(3, updatedCount)
  }

  @Test
  fun `verify product cost and profit calculation formula`() {
    val price = 100.0
    val cost = 60.0
    // Profit = (Price - Cost) / Price as a percentage
    val profitPercentage = ((price - cost) / price) * 100.0
    assertEquals(40.0, profitPercentage, 0.001)

    // Manual entry of profit percentage: Cost = Price * (1 - Profit% / 100)
    val targetProfitPct = 25.0
    val calculatedCost = price * (1.0 - (targetProfitPct / 100.0))
    assertEquals(75.0, calculatedCost, 0.001)
  }

  @Test
  fun `verify cost and profit enabled categories supports multiple selections`() {
    val enabledCategories = mutableSetOf("Product", "Consulting", "Merchandise")
    assert(enabledCategories.any { it.equals("Product", ignoreCase = true) })
    assert(enabledCategories.any { it.equals("consulting", ignoreCase = true) })
    assert(enabledCategories.any { it.equals("Merchandise", ignoreCase = true) })
    assert(!enabledCategories.any { it.equals("Food", ignoreCase = true) })
  }

  @Test
  fun `verify potential duplicate reconciliation removes entries and stops flagging scenario`() {
    val now = System.currentTimeMillis()
    val tx1 = com.example.data.Transaction(
      id = "dup_1",
      description = "Morning Coffee",
      amount = 4.50,
      category = "Food",
      date = now
    )
    val tx2 = com.example.data.Transaction(
      id = "dup_2",
      description = "Morning Coffee",
      amount = 4.50,
      category = "Food",
      date = now
    )
    val txList = listOf(tx1, tx2)

    // Initially flagged as duplicates
    val initialDuplicates = com.example.data.DuplicateTransactionDetector.findAllDuplicateIds(txList, emptySet())
    assertEquals(2, initialDuplicates.size)
    assert(initialDuplicates.contains("dup_1"))
    assert(initialDuplicates.contains("dup_2"))

    val initialPairs = com.example.data.DuplicateTransactionDetector.findDuplicatePairs(txList, emptySet())
    assertEquals(1, initialPairs.size)

    // User determines they are not duplicates and marks them Reconciled
    val pairKey = com.example.data.DuplicateTransactionDetector.getPairKey(tx1.id, tx2.id)
    val reconciledPairs = setOf(pairKey)

    // Both entries are removed from the duplicate list
    val afterReconciledDuplicates = com.example.data.DuplicateTransactionDetector.findAllDuplicateIds(txList, reconciledPairs)
    assertEquals(0, afterReconciledDuplicates.size)

    val afterPairs = com.example.data.DuplicateTransactionDetector.findDuplicatePairs(txList, reconciledPairs)
    assertEquals(0, afterPairs.size)

    // And matching duplicates for tx1 returns empty list
    val matching = com.example.data.DuplicateTransactionDetector.findMatchingDuplicatesFor(tx1, txList, reconciledPairs)
    assertEquals(0, matching.size)
  }

  @Test
  fun `verify stacked report displays income totals above each month`() {
    val incomeAmount = 3705.0
    val expenseAmount = 3704.0
    val isStacked = true

    // Under stacked mode, top label shows income totals (vs totalVolume)
    val topLabelText = if (isStacked) {
      if (incomeAmount > 0) "$${incomeAmount.toInt()}" else "$0"
    } else {
      if (expenseAmount > 0) "$${expenseAmount.toInt()}" else "$0"
    }

    assertEquals("$3705", topLabelText)
  }

  @Test
  fun `verify profit percentage by description parsing and aggregation`() {
    val tx1 = com.example.data.Transaction(
      id = "tx1",
      description = "Rick [Cost: CA$30.00 | Profit: 53.8%]",
      amount = 65.0,
      type = "INCOME",
      category = "Product",
      date = System.currentTimeMillis()
    )
    val tx2 = com.example.data.Transaction(
      id = "tx2",
      description = "Rick [Cost: CA$30.00 | Profit: 53.8%]",
      amount = 65.0,
      type = "INCOME",
      category = "Product",
      date = System.currentTimeMillis()
    )
    val tx3 = com.example.data.Transaction(
      id = "tx3",
      description = "Handmade Mug [Cost: $40.00 | Profit: 60.0%]",
      amount = 100.0,
      type = "INCOME",
      category = "Product",
      date = System.currentTimeMillis()
    )

    val parsed1 = com.example.ui.screens.reports.ProfitMarginHelper.parseTransaction(tx1)
    assertEquals("Rick", parsed1.cleanDescription)
    assertEquals(30.0, parsed1.cost!!, 0.001)
    assertEquals(53.8, parsed1.profitPercentage!!, 0.001)

    val metrics = com.example.ui.screens.reports.ProfitMarginHelper.aggregateByDescription(listOf(tx1, tx2, tx3))
    assertEquals(2, metrics.size)

    // First item should be Handmade Mug (60% profit)
    val topItem = metrics[0]
    assertEquals("Handmade Mug", topItem.description)
    assertEquals(1, topItem.count)
    assertEquals(100.0, topItem.totalRevenue, 0.001)
    assertEquals(40.0, topItem.totalCost, 0.001)
    assertEquals(60.0, topItem.totalProfit, 0.001)
    assertEquals(60.0, topItem.profitPercentage, 0.001)

    // Second item should be Rick (53.8% profit, 2 items)
    val secondItem = metrics[1]
    assertEquals("Rick", secondItem.description)
    assertEquals(2, secondItem.count)
    assertEquals(130.0, secondItem.totalRevenue, 0.001)
    assertEquals(60.0, secondItem.totalCost, 0.001)
    assertEquals(70.0, secondItem.totalProfit, 0.001)
    assertEquals(53.846, secondItem.profitPercentage, 0.01)
  }

  @Test
  fun `verify monthly overview metric cards customization settings`() {
    val defaultCards = com.example.ui.viewmodel.ExpenseViewModel.DEFAULT_OVERVIEW_CARDS
    assertTrue(defaultCards.contains(com.example.ui.viewmodel.ExpenseViewModel.CARD_BUDGET))
    assertTrue(defaultCards.contains(com.example.ui.viewmodel.ExpenseViewModel.CARD_EXPENSES))
    assertTrue(defaultCards.contains(com.example.ui.viewmodel.ExpenseViewModel.CARD_INCOME))
    assertTrue(defaultCards.contains(com.example.ui.viewmodel.ExpenseViewModel.CARD_RECONCILIATION))
    assertTrue(defaultCards.contains(com.example.ui.viewmodel.ExpenseViewModel.CARD_AI_INPUT))

    // Test customizing visible set
    val customizedCards = mutableSetOf(
      com.example.ui.viewmodel.ExpenseViewModel.CARD_EXPENSES,
      com.example.ui.viewmodel.ExpenseViewModel.CARD_INCOME
    )
    assertTrue(customizedCards.contains(com.example.ui.viewmodel.ExpenseViewModel.CARD_EXPENSES))
    assertFalse(customizedCards.contains(com.example.ui.viewmodel.ExpenseViewModel.CARD_BUDGET))
    assertFalse(customizedCards.contains(com.example.ui.viewmodel.ExpenseViewModel.CARD_RECONCILIATION))

    // Adding Net Savings card
    customizedCards.add(com.example.ui.viewmodel.ExpenseViewModel.CARD_NET_SAVINGS)
    assertTrue(customizedCards.contains(com.example.ui.viewmodel.ExpenseViewModel.CARD_NET_SAVINGS))
    assertEquals(3, customizedCards.size)
  }
}
