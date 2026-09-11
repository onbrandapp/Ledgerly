package com.example.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.CustomCategory
import java.util.Locale

data class CategoryStyle(
    val icon: ImageVector,
    val color: Color
)

data class CategoryIconItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val categoryType: String = "BOTH" // "EXPENSE", "INCOME", or "BOTH"
)

object CategoryConstants {
    // Predefined vibrant, accessible color presets
    val COLOR_PRESETS = listOf(
        "#2E7D32", // Emerald Green
        "#00897B", // Teal
        "#0288D1", // Sky Blue
        "#1976D2", // Royal Blue
        "#3949AB", // Indigo
        "#5E35B1", // Deep Purple
        "#8E24AA", // Violet
        "#D81B60", // Rose
        "#E53935", // Crimson Red
        "#E65100", // Deep Orange
        "#F57F17", // Amber
        "#689F38", // Olive / Green
        "#546E7A", // Blue Grey
        "#6D4C41", // Warm Brown
        "#37474F", // Charcoal
        "#8E8E93"  // Neutral Grey
    )

    // Predefined curated vector icons for both Expense and Income
    val PREDEFINED_ICONS = listOf(
        // Expense & Living
        CategoryIconItem("restaurant", "Food & Dining", Icons.Default.Restaurant, "EXPENSE"),
        CategoryIconItem("local_cafe", "Coffee & Drinks", Icons.Default.LocalCafe, "EXPENSE"),
        CategoryIconItem("shopping_cart", "Groceries", Icons.Default.ShoppingCart, "EXPENSE"),
        CategoryIconItem("shopping_bag", "Shopping", Icons.Default.ShoppingBag, "EXPENSE"),
        CategoryIconItem("directions_car", "Transportation", Icons.Default.DirectionsCar, "EXPENSE"),
        CategoryIconItem("local_gas_station", "Fuel & Gas", Icons.Default.LocalGasStation, "EXPENSE"),
        CategoryIconItem("home", "Housing & Rent", Icons.Default.Home, "EXPENSE"),
        CategoryIconItem("lightbulb", "Utilities & Power", Icons.Default.Lightbulb, "EXPENSE"),
        CategoryIconItem("wifi", "Internet & Mobile", Icons.Default.Wifi, "EXPENSE"),
        CategoryIconItem("confirmation_number", "Entertainment", Icons.Default.ConfirmationNumber, "EXPENSE"),
        CategoryIconItem("movie", "Movies & Media", Icons.Default.Movie, "EXPENSE"),
        CategoryIconItem("fitness_center", "Gym & Health", Icons.Default.FitnessCenter, "EXPENSE"),
        CategoryIconItem("medical_services", "Medical & Pharmacy", Icons.Default.MedicalServices, "EXPENSE"),
        CategoryIconItem("school", "Education", Icons.Default.School, "EXPENSE"),
        CategoryIconItem("flight", "Travel & Vacation", Icons.Default.Flight, "EXPENSE"),
        CategoryIconItem("pets", "Pets & Animal Care", Icons.Default.Pets, "EXPENSE"),
        CategoryIconItem("child_care", "Kids & Family", Icons.Default.ChildCare, "EXPENSE"),
        CategoryIconItem("card_giftcard", "Gifts & Donations", Icons.Default.CardGiftcard, "BOTH"),
        CategoryIconItem("build", "Repairs & Tools", Icons.Default.Build, "EXPENSE"),
        CategoryIconItem("receipt", "Bills & Fees", Icons.Default.Receipt, "EXPENSE"),
        CategoryIconItem("store", "Retail & Store", Icons.Default.Store, "EXPENSE"),

        // Income & Investments
        CategoryIconItem("attach_money", "Salary & Wages", Icons.Default.AttachMoney, "INCOME"),
        CategoryIconItem("payments", "Paycheck", Icons.Default.Payments, "INCOME"),
        CategoryIconItem("work", "Freelance & Consulting", Icons.Default.Work, "INCOME"),
        CategoryIconItem("show_chart", "Investments & Stocks", Icons.Default.ShowChart, "INCOME"),
        CategoryIconItem("account_balance", "Banking & Interest", Icons.Default.AccountBalance, "INCOME"),
        CategoryIconItem("savings", "Savings & Deposits", Icons.Default.Savings, "INCOME"),
        CategoryIconItem("monetization_on", "Cash & Tips", Icons.Default.MonetizationOn, "INCOME"),
        CategoryIconItem("paid", "Dividends & Cashback", Icons.Default.Paid, "INCOME"),
        CategoryIconItem("business_center", "Business Profits", Icons.Default.BusinessCenter, "INCOME"),
        CategoryIconItem("apartment", "Rental Real Estate", Icons.Default.Apartment, "INCOME"),
        CategoryIconItem("redeem", "Bonuses & Rewards", Icons.Default.Redeem, "INCOME"),
        CategoryIconItem("category", "General / Other", Icons.Default.Category, "BOTH")
    )

    fun getIconByName(iconName: String): ImageVector {
        return PREDEFINED_ICONS.find { it.id.equals(iconName, ignoreCase = true) }?.icon
            ?: when (iconName.lowercase()) {
                "food", "restaurant" -> Icons.Default.Restaurant
                "transport", "directions_car" -> Icons.Default.DirectionsCar
                "utilities", "lightbulb" -> Icons.Default.Lightbulb
                "entertainment", "confirmation_number" -> Icons.Default.ConfirmationNumber
                "shopping", "shopping_bag" -> Icons.Default.ShoppingBag
                "salary", "attach_money" -> Icons.Default.AttachMoney
                "investment", "show_chart" -> Icons.Default.ShowChart
                "housing", "home" -> Icons.Default.Home
                else -> Icons.Default.Category
            }
    }

    fun parseColor(hex: String, fallback: Color = Color(0xFF00897B)): Color {
        return try {
            val cleanHex = hex.trim().removePrefix("#")
            val argbLong = when (cleanHex.length) {
                6 -> 0xFF000000L or cleanHex.toLong(16)
                8 -> cleanHex.toLong(16)
                3 -> {
                    val r = cleanHex[0].toString().repeat(2)
                    val g = cleanHex[1].toString().repeat(2)
                    val b = cleanHex[2].toString().repeat(2)
                    0xFF000000L or "$r$g$b".toLong(16)
                }
                else -> return fallback
            }
            Color(argbLong)
        } catch (e: Exception) {
            fallback
        }
    }

    fun resolveCategoryStyle(
        categoryName: String,
        customCategories: List<CustomCategory> = emptyList()
    ): CategoryStyle {
        val clean = categoryName.trim()
        val customMatch = customCategories.find { it.name.equals(clean, ignoreCase = true) }
        if (customMatch != null) {
            val icon = getIconByName(customMatch.iconName)
            val color = parseColor(customMatch.colorHex, Color(0xFF00897B))
            return CategoryStyle(icon, color)
        }

        return when (clean.lowercase(Locale.getDefault())) {
            "food" -> CategoryStyle(Icons.Default.Restaurant, Color(0xFFE65100))
            "transport" -> CategoryStyle(Icons.Default.DirectionsCar, Color(0xFF1565C0))
            "utilities" -> CategoryStyle(Icons.Default.Lightbulb, Color(0xFFF57F17))
            "entertainment" -> CategoryStyle(Icons.Default.ConfirmationNumber, Color(0xFF6A1B9A))
            "shopping" -> CategoryStyle(Icons.Default.ShoppingBag, Color(0xFFC2185B))
            "salary" -> CategoryStyle(Icons.Default.AttachMoney, Color(0xFF2E7D32))
            "investment" -> CategoryStyle(Icons.Default.ShowChart, Color(0xFF00695C))
            "housing" -> CategoryStyle(Icons.Default.Home, Color(0xFF0277BD))
            else -> {
                val colors = listOf(
                    Color(0xFF8D6E63),
                    Color(0xFF78909C),
                    Color(0xFFEC407A),
                    Color(0xFFAB47BC),
                    Color(0xFF7E57C2),
                    Color(0xFF5C6BC0),
                    Color(0xFF26A69A),
                    Color(0xFF9CCC65),
                    Color(0xFFD4E157)
                )
                val index = Math.abs(clean.hashCode()) % colors.size
                CategoryStyle(Icons.Default.Category, colors[index])
            }
        }
    }
}
