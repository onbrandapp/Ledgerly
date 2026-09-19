package com.example.ui.screens.reports

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Scalable report category definition.
 * New reports (e.g. Tax Summary, Category Budgets, Cash Flow) can easily be added here.
 */
enum class ReportCategory(
    val id: String,
    val title: String,
    val shortTitle: String,
    val subtitle: String,
    val icon: ImageVector,
    val exportFormats: List<String> = listOf("PDF", "CSV")
) {
    ANALYTICS(
        id = "analytics",
        title = "Visual Analytics",
        shortTitle = "Analytics",
        subtitle = "Spending distribution & 6-month financial trends",
        icon = Icons.Default.ShowChart,
        exportFormats = emptyList()
    ),
    LEDGER(
        id = "ledger",
        title = "Financial Ledger",
        shortTitle = "Ledger",
        subtitle = "Itemized synchronized income & expense statements",
        icon = Icons.Default.ReceiptLong,
        exportFormats = listOf("CSV", "PDF")
    ),
    FORECAST(
        id = "forecast",
        title = "Income Forecast",
        shortTitle = "Forecast",
        subtitle = "Revenue pipeline, milestones & cashflow projections",
        icon = Icons.Default.Timeline,
        exportFormats = emptyList()
    ),
    AUDIT(
        id = "audit",
        title = "Audit & Compliance",
        shortTitle = "Audit Log",
        subtitle = "Compliance audit trail of deleted transactions & records",
        icon = Icons.Default.Assessment,
        exportFormats = listOf("PDF")
    );

    companion object {
        val allReports: List<ReportCategory> = entries
    }
}
