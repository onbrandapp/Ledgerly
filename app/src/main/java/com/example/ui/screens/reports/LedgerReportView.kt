package com.example.ui.screens.reports

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CustomCategory
import com.example.data.Transaction
import com.example.ui.theme.CategoryConstants
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun LedgerReportView(
    transactions: List<Transaction>,
    userEmail: String?,
    customCategories: List<CustomCategory> = emptyList(),
    onTogglePaid: (String) -> Unit,
    onEditTransaction: (Transaction) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var ledgerSelectedFilter by remember { mutableStateOf("All Time") }
    var ledgerStartDate by remember { mutableStateOf<Long?>(null) }
    var ledgerEndDate by remember { mutableStateOf<Long?>(null) }
    var hidePaidExpenses by remember { mutableStateOf(false) }
    var pendingDeleteTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showCsvExportSheet by remember { mutableStateOf(false) }
    var pendingCsvExportList by remember { mutableStateOf<List<Transaction>?>(null) }
    var pendingCsvStartDate by remember { mutableStateOf<Long?>(null) }
    var pendingCsvEndDate by remember { mutableStateOf<Long?>(null) }

    // Automatic date range calculations
    LaunchedEffect(ledgerSelectedFilter) {
        val cal = Calendar.getInstance()
        when (ledgerSelectedFilter) {
            "All Time" -> {
                ledgerStartDate = null
                ledgerEndDate = null
            }
            "Current Month" -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                ledgerStartDate = cal.timeInMillis

                val endCal = Calendar.getInstance()
                endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH))
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                endCal.set(Calendar.SECOND, 59)
                endCal.set(Calendar.MILLISECOND, 999)
                ledgerEndDate = endCal.timeInMillis
            }
            "Last 30 Days" -> {
                val endCal = Calendar.getInstance()
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                endCal.set(Calendar.SECOND, 59)
                endCal.set(Calendar.MILLISECOND, 999)
                ledgerEndDate = endCal.timeInMillis

                cal.timeInMillis = endCal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, -30)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                ledgerStartDate = cal.timeInMillis
            }
            "Last 60 Days" -> {
                val endCal = Calendar.getInstance()
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                endCal.set(Calendar.SECOND, 59)
                endCal.set(Calendar.MILLISECOND, 999)
                ledgerEndDate = endCal.timeInMillis

                cal.timeInMillis = endCal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, -60)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                ledgerStartDate = cal.timeInMillis
            }
            "Next 30 Days" -> {
                val startCal = Calendar.getInstance()
                startCal.set(Calendar.HOUR_OF_DAY, 0)
                startCal.set(Calendar.MINUTE, 0)
                startCal.set(Calendar.SECOND, 0)
                startCal.set(Calendar.MILLISECOND, 0)
                ledgerStartDate = startCal.timeInMillis

                cal.timeInMillis = startCal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, 30)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                ledgerEndDate = cal.timeInMillis
            }
            "Custom" -> {
                if (ledgerStartDate == null || ledgerEndDate == null) {
                    val startCal = Calendar.getInstance()
                    startCal.set(Calendar.DAY_OF_MONTH, 1)
                    startCal.set(Calendar.HOUR_OF_DAY, 0)
                    startCal.set(Calendar.MINUTE, 0)
                    startCal.set(Calendar.SECOND, 0)
                    startCal.set(Calendar.MILLISECOND, 0)
                    ledgerStartDate = startCal.timeInMillis

                    val endCal = Calendar.getInstance()
                    endCal.set(Calendar.HOUR_OF_DAY, 23)
                    endCal.set(Calendar.MINUTE, 59)
                    endCal.set(Calendar.SECOND, 59)
                    endCal.set(Calendar.MILLISECOND, 999)
                    ledgerEndDate = endCal.timeInMillis
                }
            }
        }
    }

    val filteredTransactions = remember(transactions, ledgerStartDate, ledgerEndDate) {
        transactions.filter { tx ->
            val start = ledgerStartDate
            val end = ledgerEndDate
            if (start != null && end != null) {
                tx.date in start..end
            } else {
                true
            }
        }
    }

    val incomeList = remember(filteredTransactions) {
        filteredTransactions.filter { it.type.uppercase() == "INCOME" }.sortedByDescending { it.date }
    }
    val rawExpenseList = remember(filteredTransactions) {
        filteredTransactions.filter { it.type.uppercase() == "EXPENSE" }.sortedByDescending { it.date }
    }
    val expenseList = remember(rawExpenseList, hidePaidExpenses) {
        if (hidePaidExpenses) rawExpenseList.filter { !it.paid } else rawExpenseList
    }

    val cashOnHand = remember(incomeList) {
        incomeList.filter { it.category.trim().equals("cash", ignoreCase = true) }.sumOf { it.amount }
    }
    val totalIncome = remember(incomeList) {
        incomeList.filter { !it.category.trim().equals("cash", ignoreCase = true) }.sumOf { it.amount }
    }
    val totalLeftToReceive = remember(incomeList) {
        incomeList.filter { !it.paid && !it.category.trim().equals("cash", ignoreCase = true) }.sumOf { it.amount }
    }
    val totalExpense = remember(expenseList) {
        expenseList.sumOf { it.amount }
    }
    val totalLeftToPay = remember(rawExpenseList) {
        rawExpenseList.filter { !it.paid }.sumOf { it.amount }
    }
    val netBalance = cashOnHand + (totalIncome - totalExpense)
    val currentBalance = cashOnHand + totalLeftToReceive - totalLeftToPay

    val ledgerFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    // Export Launchers
    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            val listToExport = pendingCsvExportList ?: filteredTransactions
            val exportStart = pendingCsvStartDate ?: ledgerStartDate
            val exportEnd = pendingCsvEndDate ?: ledgerEndDate
            try {
                LedgerReportExporter.exportToCsv(
                    context = context,
                    uri = uri,
                    transactions = listToExport,
                    startDate = exportStart,
                    endDate = exportEnd
                )
                Toast.makeText(context, "Exported ${listToExport.size} transactions to CSV successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to export CSV: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                pendingCsvExportList = null
                pendingCsvStartDate = null
                pendingCsvEndDate = null
            }
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            try {
                LedgerReportExporter.exportToPdf(
                    context = context,
                    uri = uri,
                    userEmail = userEmail,
                    transactions = filteredTransactions,
                    startDate = ledgerStartDate,
                    endDate = ledgerEndDate
                )
                Toast.makeText(context, "Ledger exported to PDF successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to export PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val showStartDatePicker = {
        val cal = Calendar.getInstance().apply {
            if (ledgerStartDate != null) {
                timeInMillis = ledgerStartDate!!
            }
        }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val resultCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                ledgerStartDate = resultCal.timeInMillis
                ledgerSelectedFilter = "Custom"
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val showEndDatePicker = {
        val cal = Calendar.getInstance().apply {
            if (ledgerEndDate != null) {
                timeInMillis = ledgerEndDate!!
            }
        }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val resultCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                ledgerEndDate = resultCal.timeInMillis
                ledgerSelectedFilter = "Custom"
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val pageScrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(pageScrollState)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        // Date Filters Quick Selection Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val filtersList = listOf("All Time", "Current Month", "Last 30 Days", "Last 60 Days", "Next 30 Days", "Custom")
            filtersList.forEach { filter ->
                FilterChip(
                    selected = ledgerSelectedFilter == filter,
                    onClick = { ledgerSelectedFilter = filter },
                    label = {
                        Text(
                            text = filter,
                            fontWeight = if (ledgerSelectedFilter == filter) FontWeight.Bold else FontWeight.Medium,
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("ledger_filter_${filter.lowercase().replace(" ", "_")}")
                )
            }
        }

        // Custom Date Range Selectors
        if (ledgerSelectedFilter == "Custom") {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Start Date Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showStartDatePicker() }
                        .testTag("ledger_custom_start_date")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "From Date",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = ledgerStartDate?.let { ledgerFormatter.format(Date(it)) } ?: "Select Date",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Select Start Date",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // End Date Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showEndDatePicker() }
                        .testTag("ledger_custom_end_date")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "To Date",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = ledgerEndDate?.let { ledgerFormatter.format(Date(it)) } ?: "Select Date",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Select End Date",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        } else {
            if (ledgerStartDate != null && ledgerEndDate != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Range: ${ledgerFormatter.format(Date(ledgerStartDate!!))} to ${ledgerFormatter.format(Date(ledgerEndDate!!))}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Action Controls Row: Hide/Show Paid Expenses + Export CSV + Export PDF
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { hidePaidExpenses = !hidePaidExpenses },
                colors = if (hidePaidExpenses) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier
                    .weight(1.2f)
                    .height(38.dp)
                    .testTag("ledger_hide_paid_expenses_button")
            ) {
                Icon(
                    imageVector = if (hidePaidExpenses) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (hidePaidExpenses) "Show Paid" else "Hide Paid",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }

            // Export CSV
            Button(
                onClick = {
                    showCsvExportSheet = true
                },
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .testTag("export_csv_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.TableChart,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("CSV", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
            }

            // Export PDF
            Button(
                onClick = {
                    pdfLauncher.launch("Finance_Ledger_${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}.pdf")
                },
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("PDF", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Dual-Column Synchronized Layout
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(540.dp)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface),
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            // Left Column (Income)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f))
                        .padding(vertical = 6.dp, horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "INCOME",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                if (incomeList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No income records",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(incomeList, key = { it.id }) { item ->
                            val catStyle = CategoryConstants.resolveCategoryStyle(item.category, customCategories)
                            val isCash = item.category.trim().equals("cash", ignoreCase = true)
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (item.paid) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                    // Row 1: Date & Amount on a clean single line with ample spacing
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = ledgerFormatter.format(Date(item.date)),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "+$${String.format(Locale.US, "%,.2f", item.amount)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.tertiary,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(3.dp))

                                    // Row 2: Description on a single clean line with ellipsis
                                    Text(
                                        text = item.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Row 3: Edit button at far left, Category badge & Status chip in center, Delete button at far right
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Far Left: Edit Button
                                        IconButton(
                                            onClick = { onEditTransaction(item) },
                                            modifier = Modifier
                                                .size(24.dp)
                                                .testTag("ledger_edit_${item.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }

                                        // Middle: Category badge and Status chip
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier
                                                .weight(1f, fill = false)
                                                .padding(horizontal = 2.dp)
                                        ) {
                                            Surface(
                                                color = catStyle.color.copy(alpha = 0.18f),
                                                shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier.weight(1f, fill = false)
                                            ) {
                                                Text(
                                                    text = item.category,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = catStyle.color,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }

                                            if (!isCash) {
                                                Surface(
                                                    color = if (item.paid) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(4.dp),
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .clickable { onTogglePaid(item.id) }
                                                        .testTag("ledger_received_toggle_${item.id}")
                                                ) {
                                                    Text(
                                                        text = if (item.paid) "Received" else "Pending",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 9.sp,
                                                        maxLines = 1,
                                                        color = if (item.paid) Color(0xFF10B981) else Color(0xFFD97706),
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // Far Right: Delete Button
                                        IconButton(
                                            onClick = { pendingDeleteTransaction = item },
                                            modifier = Modifier
                                                .size(24.dp)
                                                .testTag("ledger_delete_${item.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Vertical Divider separating Income and Expense
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            // Right Column (Expenses)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                        .padding(vertical = 6.dp, horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "EXPENSES",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                if (expenseList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (hidePaidExpenses) "No unpaid expenses" else "No expense records",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(expenseList, key = { it.id }) { item ->
                            val catStyle = CategoryConstants.resolveCategoryStyle(item.category, customCategories)
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (item.paid) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                    // Row 1: Date & Amount on a clean single line with ample spacing
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = ledgerFormatter.format(Date(item.date)),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "-$${String.format(Locale.US, "%,.2f", item.amount)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.error,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(3.dp))

                                    // Row 2: Description on a single clean line with ellipsis
                                    Text(
                                        text = item.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Row 3: Edit button at far left, Category badge & Status chip in center, Delete button at far right
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Far Left: Edit Button
                                        IconButton(
                                            onClick = { onEditTransaction(item) },
                                            modifier = Modifier
                                                .size(24.dp)
                                                .testTag("ledger_edit_${item.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }

                                        // Middle: Category badge and Status chip
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier
                                                .weight(1f, fill = false)
                                                .padding(horizontal = 2.dp)
                                        ) {
                                            Surface(
                                                color = catStyle.color.copy(alpha = 0.18f),
                                                shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier.weight(1f, fill = false)
                                            ) {
                                                Text(
                                                    text = item.category,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = catStyle.color,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }

                                            Surface(
                                                color = if (item.paid) Color(0xFF10B981).copy(alpha = 0.2f) else MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .clickable { onTogglePaid(item.id) }
                                                    .testTag("ledger_paid_toggle_${item.id}")
                                            ) {
                                                Text(
                                                    text = if (item.paid) "Paid" else "Unpaid",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 9.sp,
                                                    maxLines = 1,
                                                    color = if (item.paid) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        // Far Right: Delete Button
                                        IconButton(
                                            onClick = { pendingDeleteTransaction = item },
                                            modifier = Modifier
                                                .size(24.dp)
                                                .testTag("ledger_delete_${item.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom Totals Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "Total Income",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "+$${String.format(Locale.US, "%,.2f", totalIncome)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        if (totalLeftToReceive > 0) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Left to Receive: +$${String.format(Locale.US, "%,.2f", totalLeftToReceive)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE65100),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (cashOnHand > 0) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Cash on Hand: +$${String.format(Locale.US, "%,.2f", cashOnHand)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Vertical separator
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(if (totalLeftToReceive > 0 || totalLeftToPay > 0) 46.dp else 36.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (hidePaidExpenses) "Unpaid Expenses" else "Total Expenses",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "-$${String.format(Locale.US, "%,.2f", totalExpense)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.error
                        )
                        if (totalLeftToPay > 0) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Left to Pay: -$${String.format(Locale.US, "%,.2f", totalLeftToPay)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Net Balance",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isPositive = netBalance >= 0
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isPositive) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                    contentDescription = null,
                                    tint = if (isPositive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = if (isPositive) "SURPLUS" else "DEFICIT",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = if (isPositive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Text(
                            text = "${if (netBalance >= 0) "+" else ""}$${String.format(Locale.US, "%,.2f", netBalance)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current Balance",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isPositive = currentBalance >= 0
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isPositive) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                    contentDescription = null,
                                    tint = if (isPositive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "ACTUAL",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = if (isPositive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Text(
                            text = "${if (currentBalance >= 0) "+" else ""}$${String.format(Locale.US, "%,.2f", currentBalance)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // Delete Confirmation Dialog
    if (pendingDeleteTransaction != null) {
        val tx = pendingDeleteTransaction!!
        val isIncome = tx.type.equals("INCOME", ignoreCase = true)
        AlertDialog(
            onDismissRequest = { pendingDeleteTransaction = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Delete Entry?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Are you sure you want to delete this ledger entry? This action cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = tx.description.ifBlank { "Untitled" },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tx.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${if (isIncome) "+" else "-"}$${String.format(Locale.US, "%,.2f", tx.amount)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isIncome) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = pendingDeleteTransaction
                        pendingDeleteTransaction = null
                        if (target != null) {
                            onDeleteTransaction(target)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_delete_ledger_btn")
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingDeleteTransaction = null },
                    modifier = Modifier.testTag("cancel_delete_ledger_btn")
                ) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showCsvExportSheet) {
        CsvExportSheet(
            transactions = transactions,
            customCategories = customCategories,
            initialStartDate = ledgerStartDate,
            initialEndDate = ledgerEndDate,
            initialPreset = ledgerSelectedFilter,
            onDismiss = { showCsvExportSheet = false },
            onConfirmExport = { exportList, fileName, exportStart, exportEnd ->
                pendingCsvExportList = exportList
                pendingCsvStartDate = exportStart
                pendingCsvEndDate = exportEnd
                showCsvExportSheet = false
                csvLauncher.launch(fileName)
            }
        )
    }
}
