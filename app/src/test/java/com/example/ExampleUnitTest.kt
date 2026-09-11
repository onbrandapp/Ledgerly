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
}

