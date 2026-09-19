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
}
