package com.example.data

import java.util.Locale
import kotlin.math.abs

data class CurrencyInfo(
    val code: String,
    val symbol: String,
    val name: String,
    val rateToUsd: Double, // 1 unit of this currency = rateToUsd USD
    val flag: String
)

object CurrencyHelper {

    // Currencies list with reference rates to USD
    val supportedCurrencies = listOf(
        CurrencyInfo("USD", "$", "US Dollar", 1.0, "🇺🇸"),
        CurrencyInfo("EUR", "€", "Euro", 1.08, "🇪🇺"),
        CurrencyInfo("GBP", "£", "British Pound", 1.28, "🇬🇧"),
        CurrencyInfo("CAD", "CA$", "Canadian Dollar", 0.74, "🇨🇦"),
        CurrencyInfo("AUD", "A$", "Australian Dollar", 0.66, "🇦🇺"),
        CurrencyInfo("JPY", "¥", "Japanese Yen", 0.0065, "🇯🇵"),
        CurrencyInfo("INR", "₹", "Indian Rupee", 0.012, "🇮🇳"),
        CurrencyInfo("CNY", "¥", "Chinese Yuan", 0.14, "🇨🇳"),
        CurrencyInfo("CHF", "CHF", "Swiss Franc", 1.11, "🇨🇭"),
        CurrencyInfo("MXN", "MX$", "Mexican Peso", 0.058, "🇲🇽"),
        CurrencyInfo("BRL", "R$", "Brazilian Real", 0.19, "🇧🇷")
    )

    private val currencyMap = supportedCurrencies.associateBy { it.code.uppercase() }

    fun getAllCurrencies(): List<CurrencyInfo> = supportedCurrencies

    fun getCurrency(code: String): CurrencyInfo {
        return currencyMap[code.uppercase().trim()] ?: CurrencyInfo(code.uppercase(), code.uppercase(), code.uppercase(), 1.0, "🌐")
    }

    fun getSymbol(code: String): String {
        return getCurrency(code).symbol
    }

    /**
     * Calculates the exchange rate to convert from [fromCode] to [toCode].
     * Example: 1 unit of [fromCode] = rate units of [toCode].
     */
    fun getRate(fromCode: String, toCode: String): Double {
        val from = getCurrency(fromCode)
        val to = getCurrency(toCode)
        if (from.code == to.code) return 1.0
        if (to.rateToUsd == 0.0) return 1.0
        return from.rateToUsd / to.rateToUsd
    }

    /**
     * Converts an [amount] from [fromCode] to [toCode] using standard or custom exchange rate.
     */
    fun convert(amount: Double, fromCode: String, toCode: String, customRate: Double? = null): Double {
        if (fromCode.equals(toCode, ignoreCase = true)) return amount
        val rate = customRate ?: getRate(fromCode, toCode)
        return amount * rate
    }

    /**
     * Formats an amount with the currency symbol and 2 decimal places (or 0 for currencies like JPY).
     */
    fun formatAmount(amount: Double, currencyCode: String, includeCode: Boolean = false): String {
        val curr = getCurrency(currencyCode)
        val decimals = if (curr.code == "JPY") 0 else 2
        val formatStr = if (decimals == 0) "%,.0f" else "%,.2f"
        val formattedNumber = String.format(Locale.US, formatStr, abs(amount))
        val sign = if (amount < 0) "-" else ""
        return if (includeCode) {
            "$sign${curr.symbol}$formattedNumber ${curr.code}"
        } else {
            "$sign${curr.symbol}$formattedNumber"
        }
    }
}
