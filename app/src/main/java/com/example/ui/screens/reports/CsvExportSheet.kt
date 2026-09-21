package com.example.ui.screens.reports

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.CustomCategory
import com.example.data.Transaction
import com.example.ui.theme.CategoryConstants
import com.example.ui.theme.CategoryStyle
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class CsvTransactionType(val label: String, val shortLabel: String) {
    ALL("All Transactions", "All"),
    EXPENSE("Expenses Only", "Expenses"),
    INCOME("Income Only", "Income")
}

enum class CsvStatusFilter(val label: String, val title: String, val subtitle: String) {
    ALL("All Statuses", "All", "All Statuses"),
    PAID_ONLY("Paid / Received Only", "Paid", "Paid / Received"),
    UNPAID_ONLY("Pending / Unpaid Only", "Pending", "Pending / Unpaid")
}

enum class CsvVisualDrillDownTab(val label: String) {
    CATEGORY("Category Breakdown"),
    DESCRIPTION("Description Breakdown")
}

data class CategoryStatItem(
    val category: String,
    val amount: Double,
    val count: Int,
    val isIncome: Boolean,
    val percentage: Int
)

data class DescriptionStatItem(
    val description: String,
    val category: String,
    val amount: Double,
    val count: Int,
    val isIncome: Boolean,
    val percentage: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CsvExportSheet(
    transactions: List<Transaction>,
    customCategories: List<CustomCategory> = emptyList(),
    initialStartDate: Long? = null,
    initialEndDate: Long? = null,
    initialPreset: String = "All Time",
    onDismiss: () -> Unit,
    onConfirmExport: (filteredTransactions: List<Transaction>, suggestedFileName: String, startDate: Long?, endDate: Long?) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Transaction Type State
    var selectedType by remember { mutableStateOf(CsvTransactionType.ALL) }

    // Status Filter State
    var selectedStatus by remember { mutableStateOf(CsvStatusFilter.ALL) }

    // Category and Description Drill-Down States
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedDescriptionQuery by remember { mutableStateOf("") }
    var visualDrillDownTab by remember { mutableStateOf(CsvVisualDrillDownTab.CATEGORY) }
    var isCategoryChartExpanded by remember { mutableStateOf(false) }
    var isDescriptionChartExpanded by remember { mutableStateOf(false) }

    // Pre-download modal state
    var showPreDownloadModal by remember { mutableStateOf(false) }

    // Date Range Presets
    var selectedPreset by remember {
        mutableStateOf(if (initialStartDate != null && initialEndDate != null) initialPreset else "All Time")
    }

    var startDate by remember { mutableStateOf<Long?>(initialStartDate) }
    var endDate by remember { mutableStateOf<Long?>(initialEndDate) }

    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    // Preset Calculation
    fun applyPreset(preset: String) {
        selectedPreset = preset
        val cal = Calendar.getInstance()
        when (preset) {
            "All Time" -> {
                startDate = null
                endDate = null
            }
            "This Month" -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                startDate = cal.timeInMillis

                val endCal = Calendar.getInstance()
                endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH))
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                endCal.set(Calendar.SECOND, 59)
                endCal.set(Calendar.MILLISECOND, 999)
                endDate = endCal.timeInMillis
            }
            "Last Month" -> {
                cal.add(Calendar.MONTH, -1)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                startDate = cal.timeInMillis

                val endCal = Calendar.getInstance()
                endCal.add(Calendar.MONTH, -1)
                endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH))
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                endCal.set(Calendar.SECOND, 59)
                endCal.set(Calendar.MILLISECOND, 999)
                endDate = endCal.timeInMillis
            }
            "Last 30 Days" -> {
                val endCal = Calendar.getInstance()
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                endCal.set(Calendar.SECOND, 59)
                endCal.set(Calendar.MILLISECOND, 999)
                endDate = endCal.timeInMillis

                cal.timeInMillis = endCal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, -30)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                startDate = cal.timeInMillis
            }
            "Last 90 Days" -> {
                val endCal = Calendar.getInstance()
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                endCal.set(Calendar.SECOND, 59)
                endCal.set(Calendar.MILLISECOND, 999)
                endDate = endCal.timeInMillis

                cal.timeInMillis = endCal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, -90)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                startDate = cal.timeInMillis
            }
            "This Year" -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                startDate = cal.timeInMillis

                val endCal = Calendar.getInstance()
                endCal.set(Calendar.MONTH, Calendar.DECEMBER)
                endCal.set(Calendar.DAY_OF_MONTH, 31)
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                endCal.set(Calendar.SECOND, 59)
                endCal.set(Calendar.MILLISECOND, 999)
                endDate = endCal.timeInMillis
            }
            "Custom" -> {
                if (startDate == null || endDate == null) {
                    val startCal = Calendar.getInstance()
                    startCal.set(Calendar.DAY_OF_MONTH, 1)
                    startCal.set(Calendar.HOUR_OF_DAY, 0)
                    startCal.set(Calendar.MINUTE, 0)
                    startCal.set(Calendar.SECOND, 0)
                    startCal.set(Calendar.MILLISECOND, 0)
                    startDate = startCal.timeInMillis

                    val endCal = Calendar.getInstance()
                    endCal.set(Calendar.HOUR_OF_DAY, 23)
                    endCal.set(Calendar.MINUTE, 59)
                    endCal.set(Calendar.SECOND, 59)
                    endCal.set(Calendar.MILLISECOND, 999)
                    endDate = endCal.timeInMillis
                }
            }
        }
    }

    // Step 1: Base scoped transactions matching date, type, and payment status
    val scopeTransactions = remember(transactions, selectedType, selectedStatus, startDate, endDate) {
        transactions.filter { tx ->
            // Date Filter
            val matchesDate = when {
                startDate != null && endDate != null -> tx.date in startDate!!..endDate!!
                startDate != null -> tx.date >= startDate!!
                endDate != null -> tx.date <= endDate!!
                else -> true
            }

            // Type Filter
            val matchesType = when (selectedType) {
                CsvTransactionType.ALL -> true
                CsvTransactionType.EXPENSE -> tx.type.equals("EXPENSE", ignoreCase = true)
                CsvTransactionType.INCOME -> tx.type.equals("INCOME", ignoreCase = true)
            }

            // Status Filter
            val matchesStatus = when (selectedStatus) {
                CsvStatusFilter.ALL -> true
                CsvStatusFilter.PAID_ONLY -> tx.paid
                CsvStatusFilter.UNPAID_ONLY -> !tx.paid
            }

            matchesDate && matchesType && matchesStatus
        }
    }

    // Available categories in the current scope
    val availableCategories = remember(scopeTransactions) {
        scopeTransactions.groupBy { it.category }
            .map { (cat, list) -> cat to list.size }
            .sortedByDescending { it.second }
    }

    // Category statistics for the visual bar chart
    val categoryStats = remember(scopeTransactions) {
        val totalScopeAmount = scopeTransactions.sumOf { it.amount }.takeIf { it > 0 } ?: 1.0
        scopeTransactions.groupBy { it.category }
            .map { (cat, list) ->
                val sum = list.sumOf { it.amount }
                val hasMoreIncome = list.count { it.type.equals("INCOME", ignoreCase = true) } > list.size / 2
                CategoryStatItem(
                    category = cat,
                    amount = sum,
                    count = list.size,
                    isIncome = hasMoreIncome,
                    percentage = ((sum / totalScopeAmount) * 100).toInt()
                )
            }.sortedByDescending { it.amount }
    }
    val maxCategoryAmount = remember(categoryStats) {
        categoryStats.maxOfOrNull { it.amount }?.takeIf { it > 0 } ?: 1.0
    }

    // Description statistics for the visual bar chart
    val descriptionStats = remember(scopeTransactions, selectedCategory) {
        val targetList = if (selectedCategory != null) {
            scopeTransactions.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        } else {
            scopeTransactions
        }
        val totalScopeAmount = targetList.sumOf { it.amount }.takeIf { it > 0 } ?: 1.0
        targetList.groupBy { it.description.trim() }
            .filter { it.key.isNotBlank() }
            .map { (desc, list) ->
                val sum = list.sumOf { it.amount }
                val cat = list.firstOrNull()?.category ?: ""
                val hasMoreIncome = list.count { it.type.equals("INCOME", ignoreCase = true) } > list.size / 2
                DescriptionStatItem(
                    description = desc,
                    category = cat,
                    amount = sum,
                    count = list.size,
                    isIncome = hasMoreIncome,
                    percentage = ((sum / totalScopeAmount) * 100).toInt()
                )
            }.sortedByDescending { it.amount }
    }
    val maxDescriptionAmount = remember(descriptionStats) {
        descriptionStats.maxOfOrNull { it.amount }?.takeIf { it > 0 } ?: 1.0
    }

    // Top descriptions for quick suggestion chips
    val topDescriptions = remember(descriptionStats) {
        descriptionStats.take(6).map { it.description }
    }

    // Step 2: Final filtered transactions applying Category and Description drill-downs
    val filteredTransactions = remember(scopeTransactions, selectedCategory, selectedDescriptionQuery) {
        scopeTransactions.filter { tx ->
            val matchesCategory = selectedCategory == null || tx.category.equals(selectedCategory, ignoreCase = true)
            val matchesDescription = selectedDescriptionQuery.isBlank() ||
                    tx.description.contains(selectedDescriptionQuery.trim(), ignoreCase = true)
            matchesCategory && matchesDescription
        }.sortedByDescending { it.date }
    }

    // Summary statistics for preview
    val incomeCount = remember(filteredTransactions) {
        filteredTransactions.count { it.type.equals("INCOME", ignoreCase = true) }
    }
    val expenseCount = remember(filteredTransactions) {
        filteredTransactions.count { it.type.equals("EXPENSE", ignoreCase = true) }
    }
    val incomeSum = remember(filteredTransactions) {
        filteredTransactions.filter { it.type.equals("INCOME", ignoreCase = true) }.sumOf { it.amount }
    }
    val expenseSum = remember(filteredTransactions) {
        filteredTransactions.filter { it.type.equals("EXPENSE", ignoreCase = true) }.sumOf { it.amount }
    }

    // Suggested File Name Generator (includes category or description tags if drilled down)
    val suggestedFileName = remember(selectedType, selectedPreset, startDate, endDate, selectedCategory, selectedDescriptionQuery) {
        val dateStamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val typeTag = when (selectedType) {
            CsvTransactionType.ALL -> "Ledger"
            CsvTransactionType.EXPENSE -> "Expenses"
            CsvTransactionType.INCOME -> "Income"
        }
        val rangeTag = if (startDate != null && endDate != null) {
            val f = SimpleDateFormat("yyyyMMdd", Locale.US)
            "${f.format(Date(startDate!!))}_to_${f.format(Date(endDate!!))}"
        } else {
            selectedPreset.replace(" ", "")
        }
        val catTag = if (!selectedCategory.isNullOrBlank()) {
            "_" + selectedCategory!!.replace(Regex("[^a-zA-Z0-9]"), "")
        } else ""
        val descTag = if (selectedDescriptionQuery.isNotBlank()) {
            "_" + selectedDescriptionQuery.trim().take(12).replace(Regex("[^a-zA-Z0-9]"), "")
        } else ""
        "Finance_${typeTag}${catTag}${descTag}_${rangeTag}_$dateStamp.csv"
    }

    // Date Picker dialog helpers
    fun showStartDatePicker() {
        val cal = Calendar.getInstance()
        startDate?.let { cal.timeInMillis = it }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance()
                newCal.set(year, month, dayOfMonth, 0, 0, 0)
                newCal.set(Calendar.MILLISECOND, 0)
                startDate = newCal.timeInMillis
                selectedPreset = "Custom"
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun showEndDatePicker() {
        val cal = Calendar.getInstance()
        endDate?.let { cal.timeInMillis = it }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance()
                newCal.set(year, month, dayOfMonth, 23, 59, 59)
                newCal.set(Calendar.MILLISECOND, 999)
                endDate = newCal.timeInMillis
                selectedPreset = "Custom"
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Export to CSV",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Select date range and transaction types",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_csv_export_sheet")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // SECTION 1: Transaction Type Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Transaction Type",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // All Transactions Card
                    TypeSelectCard(
                        title = "All Types",
                        subtitle = "Both Income & Expense",
                        icon = Icons.Default.ListAlt,
                        isSelected = selectedType == CsvTransactionType.ALL,
                        activeColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("csv_type_all"),
                        onClick = { selectedType = CsvTransactionType.ALL }
                    )

                    // Expenses Only Card
                    TypeSelectCard(
                        title = "Expenses",
                        subtitle = "Spending only",
                        icon = Icons.AutoMirrored.Filled.TrendingDown,
                        isSelected = selectedType == CsvTransactionType.EXPENSE,
                        activeColor = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("csv_type_expenses"),
                        onClick = { selectedType = CsvTransactionType.EXPENSE }
                    )

                    // Income Only Card
                    TypeSelectCard(
                        title = "Income",
                        subtitle = "Earnings only",
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        isSelected = selectedType == CsvTransactionType.INCOME,
                        activeColor = Color(0xFF10B981),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("csv_type_income"),
                        onClick = { selectedType = CsvTransactionType.INCOME }
                    )
                }
            }

            // SECTION 2: Date Range Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Date Range",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (selectedPreset == "Custom" || startDate != null) {
                        TextButton(
                            onClick = { applyPreset("All Time") },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Reset to All Time", fontSize = 12.sp)
                        }
                    }
                }

                // Preset Chips
                val presets = listOf(
                    "All Time",
                    "This Month",
                    "Last Month",
                    "Last 30 Days",
                    "Last 90 Days",
                    "This Year",
                    "Custom"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    presets.forEach { preset ->
                        val isSelected = selectedPreset == preset
                        FilterChip(
                            selected = isSelected,
                            onClick = { applyPreset(preset) },
                            label = {
                                Text(
                                    text = preset,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            },
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("csv_preset_${preset.replace(" ", "_")}")
                        )
                    }
                }

                // Custom Date Range Pickers (Visible if Custom or if Start/End is set)
                AnimatedVisibility(visible = selectedPreset == "Custom" || (startDate != null && endDate != null)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Start Date Button
                        OutlinedCard(
                            onClick = { showStartDatePicker() },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("csv_start_date_picker")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Start Date",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = startDate?.let { dateFormatter.format(Date(it)) } ?: "Select",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // End Date Button
                        OutlinedCard(
                            onClick = { showEndDatePicker() },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("csv_end_date_picker")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "End Date",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = endDate?.let { dateFormatter.format(Date(it)) } ?: "Select",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // SECTION 3: Payment Status Filter
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Payment Status",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CsvStatusFilter.entries.forEach { status ->
                        val isSelected = selectedStatus == status
                        val activeColor = when (status) {
                            CsvStatusFilter.ALL -> MaterialTheme.colorScheme.primary
                            CsvStatusFilter.PAID_ONLY -> Color(0xFF10B981)
                            CsvStatusFilter.UNPAID_ONLY -> Color(0xFFF59E0B)
                        }

                        Card(
                            onClick = { selectedStatus = status },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) activeColor.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) activeColor
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("csv_status_${status.name}")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = when (status) {
                                            CsvStatusFilter.ALL -> Icons.Default.FilterAlt
                                            CsvStatusFilter.PAID_ONLY -> Icons.Default.CheckCircle
                                            CsvStatusFilter.UNPAID_ONLY -> Icons.Default.Schedule
                                        },
                                        contentDescription = null,
                                        tint = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = status.title,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }

                                Text(
                                    text = status.subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // SECTION 4: Category & Description Drill-Down Filters
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Drill Down: Category & Description",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (selectedCategory != null || selectedDescriptionQuery.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = {
                                selectedCategory = null
                                selectedDescriptionQuery = ""
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Reset Drill Down",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Category Drill-Down Row
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Filter by Category",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (selectedCategory != null) {
                            Text(
                                text = "Active: $selectedCategory",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // "All Categories" chip
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = {
                                Text(
                                    text = "All Categories (${scopeTransactions.size})",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            leadingIcon = if (selectedCategory == null) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            } else null,
                            modifier = Modifier.testTag("csv_category_all")
                        )

                        // Chips for each category in current scope
                        availableCategories.forEach { (cat, count) ->
                            val isSelected = selectedCategory.equals(cat, ignoreCase = true)
                            val catStyle = remember(cat, customCategories) {
                                CategoryConstants.resolveCategoryStyle(cat, customCategories)
                            }

                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedCategory = if (isSelected) null else cat
                                },
                                label = {
                                    Text(
                                        text = "$cat ($count)",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = catStyle.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else catStyle.color,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.testTag("csv_category_$cat")
                            )
                        }
                    }
                }

                // Description Drill-Down Search & Quick Chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Filter by Description",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = selectedDescriptionQuery,
                        onValueChange = { selectedDescriptionQuery = it },
                        placeholder = {
                            Text(
                                "Type description (e.g. Grocery, Coffee, Salary)...",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (selectedDescriptionQuery.isNotEmpty()) {
                                IconButton(onClick = { selectedDescriptionQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear description search",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("csv_description_search_field")
                    )

                    // Top description suggestion chips
                    if (topDescriptions.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            topDescriptions.forEach { desc ->
                                val isSelected = selectedDescriptionQuery.equals(desc, ignoreCase = true)
                                SuggestionChip(
                                    onClick = {
                                        selectedDescriptionQuery = if (isSelected) "" else desc
                                    },
                                    label = {
                                        Text(
                                            text = desc,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // SECTION 5: Visual Distribution Bar Charts (Similar to Ledger Income & Expenses Reports)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Visual Drill-Down Analysis",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "Interactive Breakdown",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 28.dp)
                        )
                    }

                    // Tabs: Category Breakdown vs Description Breakdown
                    TabRow(
                        selectedTabIndex = visualDrillDownTab.ordinal,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        CsvVisualDrillDownTab.entries.forEach { tab ->
                            val isSelected = visualDrillDownTab == tab
                            Tab(
                                selected = isSelected,
                                onClick = { visualDrillDownTab = tab },
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = when (tab) {
                                                CsvVisualDrillDownTab.CATEGORY -> Icons.Default.Category
                                                CsvVisualDrillDownTab.DESCRIPTION -> Icons.Default.Notes
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = tab.label,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            )
                        }
                    }

                    // TAB 1: Category Distribution Bar Chart
                    if (visualDrillDownTab == CsvVisualDrillDownTab.CATEGORY) {
                        if (categoryStats.isEmpty()) {
                            Text(
                                text = "No category data available for current date and type filters.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            val displayItems = if (isCategoryChartExpanded) categoryStats else categoryStats.take(5)

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Tap any category bar to filter CSV export:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                displayItems.forEach { item ->
                                    val isSelected = selectedCategory.equals(item.category, ignoreCase = true)
                                    val catStyle = remember(item.category, customCategories) {
                                        CategoryConstants.resolveCategoryStyle(item.category, customCategories)
                                    }
                                    val progressFraction = (item.amount / maxCategoryAmount).coerceIn(0.0, 1.0).toFloat()
                                    val animatedProgress by animateFloatAsState(
                                        targetValue = progressFraction,
                                        label = "cat_progress"
                                    )

                                    Card(
                                        onClick = {
                                            selectedCategory = if (isSelected) null else item.category
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) catStyle.color.copy(alpha = 0.14f)
                                            else MaterialTheme.colorScheme.surface
                                        ),
                                        border = BorderStroke(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) catStyle.color else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .background(catStyle.color.copy(alpha = 0.2f), CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = catStyle.icon,
                                                            contentDescription = null,
                                                            tint = catStyle.color,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }

                                                    Text(
                                                        text = item.category,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )

                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = MaterialTheme.colorScheme.surfaceVariant
                                                    ) {
                                                        Text(
                                                            text = "${item.count} txns",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontSize = 9.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }

                                                    if (isSelected) {
                                                        Icon(
                                                            imageVector = Icons.Default.CheckCircle,
                                                            contentDescription = "Selected",
                                                            tint = catStyle.color,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }

                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = String.format(Locale.US, "$%.2f", item.amount),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (item.isIncome) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "(${item.percentage}%)",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            // Animated Bar Indicator
                                            LinearProgressIndicator(
                                                progress = { animatedProgress },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(3.dp)),
                                                color = catStyle.color,
                                                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }

                                if (categoryStats.size > 5) {
                                    TextButton(
                                        onClick = { isCategoryChartExpanded = !isCategoryChartExpanded },
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    ) {
                                        Text(
                                            text = if (isCategoryChartExpanded) "Show Less" else "Show All (${categoryStats.size}) Categories",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        Icon(
                                            imageVector = if (isCategoryChartExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // TAB 2: Description Distribution Bar Chart
                    if (visualDrillDownTab == CsvVisualDrillDownTab.DESCRIPTION) {
                        if (descriptionStats.isEmpty()) {
                            Text(
                                text = "No description data available for current filters.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            val displayItems = if (isDescriptionChartExpanded) descriptionStats else descriptionStats.take(5)

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Tap any description to filter CSV export:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                displayItems.forEach { item ->
                                    val isSelected = selectedDescriptionQuery.equals(item.description, ignoreCase = true)
                                    val catStyle = remember(item.category, customCategories) {
                                        CategoryConstants.resolveCategoryStyle(item.category, customCategories)
                                    }
                                    val progressFraction = (item.amount / maxDescriptionAmount).coerceIn(0.0, 1.0).toFloat()
                                    val animatedProgress by animateFloatAsState(
                                        targetValue = progressFraction,
                                        label = "desc_progress"
                                    )

                                    Card(
                                        onClick = {
                                            selectedDescriptionQuery = if (isSelected) "" else item.description
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        ),
                                        border = BorderStroke(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .background(
                                                                if (item.isIncome) Color(0xFF10B981).copy(alpha = 0.2f)
                                                                else catStyle.color.copy(alpha = 0.2f),
                                                                CircleShape
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = if (item.isIncome) Icons.Default.ArrowDownward else Icons.Default.Notes,
                                                            contentDescription = null,
                                                            tint = if (item.isIncome) Color(0xFF10B981) else catStyle.color,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }

                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = item.description,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        if (item.category.isNotBlank()) {
                                                            Text(
                                                                text = item.category,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontSize = 9.sp,
                                                                color = catStyle.color,
                                                                maxLines = 1
                                                            )
                                                        }
                                                    }

                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = MaterialTheme.colorScheme.surfaceVariant
                                                    ) {
                                                        Text(
                                                            text = "${item.count}x",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontSize = 9.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }

                                                    if (isSelected) {
                                                        Icon(
                                                            imageVector = Icons.Default.CheckCircle,
                                                            contentDescription = "Selected",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }

                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = String.format(Locale.US, "$%.2f", item.amount),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (item.isIncome) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "(${item.percentage}%)",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            // Animated Bar Indicator
                                            LinearProgressIndicator(
                                                progress = { animatedProgress },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(3.dp)),
                                                color = if (item.isIncome) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                                                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }

                                if (descriptionStats.size > 5) {
                                    TextButton(
                                        onClick = { isDescriptionChartExpanded = !isDescriptionChartExpanded },
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    ) {
                                        Text(
                                            text = if (isDescriptionChartExpanded) "Show Less" else "Show All (${descriptionStats.size}) Descriptions",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        Icon(
                                            imageVector = if (isDescriptionChartExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 6: Live Export Preview Summary Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (filteredTransactions.isNotEmpty()) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                    }
                ),
                border = BorderStroke(
                    1.dp,
                    if (filteredTransactions.isNotEmpty()) MaterialTheme.colorScheme.outlineVariant
                    else MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (filteredTransactions.isNotEmpty()) Icons.Default.ReceiptLong else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (filteredTransactions.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "${filteredTransactions.size} Records Ready",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Date Range Label on its own line
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = if (startDate != null && endDate != null) {
                                "${dateFormatter.format(Date(startDate!!))} - ${dateFormatter.format(Date(endDate!!))}"
                            } else {
                                selectedPreset
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    // Active Drill-Down Indicators
                    if (selectedCategory != null || selectedDescriptionQuery.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (selectedCategory != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Category: $selectedCategory",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear category",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clickable { selectedCategory = null }
                                        )
                                    }
                                }
                            }

                            if (selectedDescriptionQuery.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Desc: ${selectedDescriptionQuery.trim()}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear description",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clickable { selectedDescriptionQuery = "" }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (filteredTransactions.isEmpty()) {
                        Text(
                            text = "No transactions match your chosen filters. Please adjust the date range, category, description, or transaction type.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        // Breakdown pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (selectedType != CsvTransactionType.EXPENSE) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Income ($incomeCount)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = String.format(Locale.US, "$%.2f", incomeSum),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981)
                                    )
                                }
                            }

                            if (selectedType != CsvTransactionType.INCOME) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Expenses ($expenseCount)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = String.format(Locale.US, "$%.2f", expenseSum),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        // Preview of file name and Quick Pre-Download Modal Preview Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.InsertDriveFile,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = suggestedFileName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (filteredTransactions.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = { showPreDownloadModal = true },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .height(30.dp)
                                        .testTag("preview_csv_summary_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Preview",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 5: Action Buttons (Cancel & Download CSV with Pre-Download Modal)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(top = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(0.75f)
                        .height(48.dp)
                        .testTag("cancel_csv_export_button")
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = {
                        showPreDownloadModal = true
                    },
                    enabled = filteredTransactions.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(1.85f)
                        .height(48.dp)
                        .testTag("download_csv_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Preview & Download",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }

    if (showPreDownloadModal) {
        CsvPreDownloadSummaryModal(
            filteredTransactions = filteredTransactions,
            suggestedFileName = suggestedFileName,
            customCategories = customCategories,
            startDate = startDate,
            endDate = endDate,
            selectedPreset = selectedPreset,
            selectedType = selectedType,
            selectedCategory = selectedCategory,
            selectedDescriptionQuery = selectedDescriptionQuery,
            selectedStatus = selectedStatus,
            incomeSum = incomeSum,
            expenseSum = expenseSum,
            onDismiss = { showPreDownloadModal = false },
            onConfirmDownload = {
                showPreDownloadModal = false
                onConfirmExport(filteredTransactions, suggestedFileName, startDate, endDate)
            }
        )
    }
}

@Composable
private fun TypeSelectCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) activeColor.copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) activeColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        if (isSelected) activeColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CsvPreDownloadSummaryModal(
    filteredTransactions: List<Transaction>,
    suggestedFileName: String,
    customCategories: List<CustomCategory>,
    startDate: Long?,
    endDate: Long?,
    selectedPreset: String,
    selectedType: CsvTransactionType,
    selectedCategory: String?,
    selectedDescriptionQuery: String,
    selectedStatus: CsvStatusFilter,
    incomeSum: Double,
    expenseSum: Double,
    onDismiss: () -> Unit,
    onConfirmDownload: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val netBalance = incomeSum - expenseSum

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .navigationBarsPadding()
                .padding(vertical = 12.dp)
                .testTag("csv_pre_download_modal")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Export Preview & Summary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Review transactions before file is generated",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close preview",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Summary Highlights Card - Executive Financial Overview
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // File and count row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            RoundedCornerShape(6.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.InsertDriveFile,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = suggestedFileName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                            ) {
                                Text(
                                    text = "${filteredTransactions.size} Records",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Unified Executive 3-Column Financial Metric Strip
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Column 1: Total Income
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "INCOME",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.6.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = String.format(Locale.US, "+$%.2f", incomeSum),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF10B981),
                                        maxLines = 1
                                    )
                                }

                                VerticalDivider(
                                    modifier = Modifier.height(28.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                )

                                // Column 2: Total Expenses
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "EXPENSES",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.6.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = String.format(Locale.US, "-$%.2f", expenseSum),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (expenseSum > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }

                                VerticalDivider(
                                    modifier = Modifier.height(28.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                )

                                // Column 3: Net Balance
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "NET",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.6.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = String.format(Locale.US, "$%.2f", netBalance),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (netBalance >= 0) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        // Filter Scope Pills Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Date Range Chip
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                            ) {
                                Text(
                                    text = if (startDate != null && endDate != null) {
                                        "${dateFormatter.format(Date(startDate))} - ${dateFormatter.format(Date(endDate))}"
                                    } else {
                                        selectedPreset
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            // Type Chip
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                            ) {
                                Text(
                                    text = selectedType.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            // Status Chip (if not ALL)
                            if (selectedStatus != CsvStatusFilter.ALL) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                                ) {
                                    Text(
                                        text = selectedStatus.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Category Chip (if set)
                            if (!selectedCategory.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "Cat: $selectedCategory",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Description Chip (if set)
                            if (selectedDescriptionQuery.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "Desc: ${selectedDescriptionQuery.trim()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Table Header Bar (Category, Description, Amount)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Category",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1.05f)
                        )
                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1.35f)
                        )
                        Text(
                            text = "Amount",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(0.9f)
                        )
                    }
                }

                // Table Rows (LazyColumn previewing Category, Description, Amount)
                if (filteredTransactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No transactions match the selected filters.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp)),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredTransactions, key = { it.id.ifEmpty { "${it.date}_${it.description}_${it.amount}" } }) { tx ->
                            val catStyle = remember(tx.category, customCategories) {
                                CategoryConstants.resolveCategoryStyle(tx.category, customCategories)
                            }
                            val isIncome = tx.type.equals("INCOME", ignoreCase = true)

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Column 1: Category with Icon & Label
                                    Row(
                                        modifier = Modifier.weight(1.05f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(catStyle.color.copy(alpha = 0.18f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = catStyle.icon,
                                                contentDescription = null,
                                                tint = catStyle.color,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }

                                        Text(
                                            text = tx.category.ifBlank { "Uncategorized" },
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Column 2: Description & Date
                                    Column(
                                        modifier = Modifier.weight(1.35f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = tx.description.ifBlank { "No description" },
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = if (tx.description.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = dateFormatter.format(Date(tx.date)),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (tx.paid) {
                                                Text(
                                                    text = "• Paid",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF10B981)
                                                )
                                            }
                                        }
                                    }

                                    // Column 3: Amount
                                    Text(
                                        text = String.format(
                                            Locale.US,
                                            "%s$%.2f",
                                            if (isIncome) "+" else "-",
                                            tx.amount
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.End,
                                        color = if (isIncome) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(0.9f)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Bottom Action Buttons: Cancel and Confirm Export
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(0.85f)
                            .height(48.dp)
                            .testTag("pre_download_cancel_button")
                    ) {
                        Text(
                            text = "Back",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }

                    Button(
                        onClick = onConfirmDownload,
                        enabled = filteredTransactions.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1.55f)
                            .height(48.dp)
                            .testTag("pre_download_confirm_export_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Generate CSV",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}
