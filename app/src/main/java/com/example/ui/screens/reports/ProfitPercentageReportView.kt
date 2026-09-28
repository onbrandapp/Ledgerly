package com.example.ui.screens.reports

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CurrencyHelper
import com.example.data.Transaction
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitPercentageReportView(
    transactions: List<Transaction>,
    userEmail: String? = null,
    primaryCurrency: String = "USD",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currencySymbol = remember(primaryCurrency) { CurrencyHelper.getSymbol(primaryCurrency) }

    var selectedDateFilter by remember { mutableStateOf("All Time") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedSort by remember { mutableStateOf("MARGIN_DESC") }
    var selectedViewMode by remember { mutableStateOf("CHART") } // "CHART" or "TABLE"

    // Date Range calculation
    val dateRange = remember(selectedDateFilter) {
        val cal = Calendar.getInstance()
        when (selectedDateFilter) {
            "Current Month" -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val start = cal.timeInMillis
                val endCal = Calendar.getInstance()
                endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH))
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                endCal.set(Calendar.SECOND, 59)
                Pair(start, endCal.timeInMillis)
            }
            "Last 30 Days" -> {
                val end = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, -30)
                Pair(cal.timeInMillis, end)
            }
            "Last 90 Days" -> {
                val end = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, -90)
                Pair(cal.timeInMillis, end)
            }
            "This Year" -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val start = cal.timeInMillis
                val endCal = Calendar.getInstance()
                endCal.set(Calendar.MONTH, Calendar.DECEMBER)
                endCal.set(Calendar.DAY_OF_MONTH, 31)
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                Pair(start, endCal.timeInMillis)
            }
            else -> Pair(null, null)
        }
    }

    // Filter transactions by date
    val filteredTransactions = remember(transactions, dateRange) {
        val (start, end) = dateRange
        if (start != null && end != null) {
            transactions.filter { it.date in start..end }
        } else {
            transactions
        }
    }

    // Aggregate by description - strictly includes only transactions with an inputted cost
    val rawAggregatedItems = remember(filteredTransactions) {
        ProfitMarginHelper.aggregateByDescription(filteredTransactions, onlyWithCostData = true)
    }

    // Filter by search & sort
    val displayItems = remember(rawAggregatedItems, searchQuery, selectedSort) {
        val filtered = if (searchQuery.isBlank()) {
            rawAggregatedItems
        } else {
            rawAggregatedItems.filter { it.description.contains(searchQuery.trim(), ignoreCase = true) }
        }

        when (selectedSort) {
            "MARGIN_DESC" -> filtered.sortedByDescending { it.profitPercentage }
            "MARGIN_ASC" -> filtered.sortedBy { it.profitPercentage }
            "PROFIT_DESC" -> filtered.sortedByDescending { it.totalProfit }
            "REVENUE_DESC" -> filtered.sortedByDescending { it.totalRevenue }
            "NAME_ASC" -> filtered.sortedBy { it.description.lowercase(Locale.ROOT) }
            else -> filtered.sortedByDescending { it.profitPercentage }
        }
    }

    // Export Launchers
    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                ProfitReportExporter.exportToCsv(
                    context = context,
                    uri = uri,
                    items = displayItems,
                    startDate = dateRange.first,
                    endDate = dateRange.second
                )
                Toast.makeText(context, "Exported ${displayItems.size} profit items to CSV successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to export CSV: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            try {
                ProfitReportExporter.exportToPdf(
                    context = context,
                    uri = uri,
                    userEmail = userEmail,
                    items = displayItems,
                    startDate = dateRange.first,
                    endDate = dateRange.second
                )
                Toast.makeText(context, "Exported Profit Report to PDF successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to export PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // KPI Summary Calculations
    val totalRevenue = remember(displayItems) { displayItems.sumOf { it.totalRevenue } }
    val totalCost = remember(displayItems) { displayItems.sumOf { it.totalCost } }
    val totalProfit = remember(displayItems) { totalRevenue - totalCost }
    val avgMargin = remember(totalRevenue, totalProfit) {
        if (totalRevenue > 0) (totalProfit / totalRevenue) * 100.0 else 0.0
    }
    val topMarginItem = remember(displayItems) { displayItems.maxByOrNull { it.profitPercentage } }

    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("profit_percentage_report_view"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. KPI Summary Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Average Margin Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Avg Profit Margin",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF047857),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.1f", avgMargin)}%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF047857)
                        )
                        Text(
                            text = "${displayItems.size} items analyzed",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF047857).copy(alpha = 0.8f),
                            fontSize = 10.sp
                        )
                    }
                }

                // Total Profit Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Total Gross Profit",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$currencySymbol${String.format(Locale.US, "%,.2f", totalProfit)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Rev: $currencySymbol${String.format(Locale.US, "%,.0f", totalRevenue)} • Cost: $currencySymbol${String.format(Locale.US, "%,.0f", totalCost)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 2. Action & Export Toolbar
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Export Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Export Report",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Export CSV Button
                            Button(
                                onClick = {
                                    val time = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                                    csvLauncher.launch("profit_by_description_$time.csv")
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("export_profit_csv_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("CSV", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // Export PDF Button
                            OutlinedButton(
                                onClick = {
                                    val time = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                                    pdfLauncher.launch("profit_by_description_$time.pdf")
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("export_profit_pdf_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Date Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("All Time", "Current Month", "Last 30 Days", "Last 90 Days", "This Year").forEach { filter ->
                            val isSelected = selectedDateFilter == filter
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedDateFilter = filter },
                                label = { Text(filter, fontSize = 11.sp) },
                                modifier = Modifier.height(30.dp)
                            )
                        }
                    }

                    // Search & View Mode Switcher
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search by description...", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("search_profit_description")
                        )

                        // Chart vs Table Toggle
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Row(modifier = Modifier.padding(2.dp)) {
                                IconButton(
                                    onClick = { selectedViewMode = "CHART" },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(
                                            if (selectedViewMode == "CHART") MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BarChart,
                                        contentDescription = "Bar Chart View",
                                        tint = if (selectedViewMode == "CHART") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { selectedViewMode = "TABLE" },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(
                                            if (selectedViewMode == "TABLE") MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TableChart,
                                        contentDescription = "Table View",
                                        tint = if (selectedViewMode == "TABLE") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Scope & Sort selector row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterAlt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Cost Inputted Only",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Sort toggle button
                        AssistChip(
                            onClick = {
                                selectedSort = when (selectedSort) {
                                    "MARGIN_DESC" -> "MARGIN_ASC"
                                    "MARGIN_ASC" -> "PROFIT_DESC"
                                    "PROFIT_DESC" -> "REVENUE_DESC"
                                    "REVENUE_DESC" -> "NAME_ASC"
                                    else -> "MARGIN_DESC"
                                }
                            },
                            label = {
                                val label = when (selectedSort) {
                                    "MARGIN_DESC" -> "Margin % (High → Low)"
                                    "MARGIN_ASC" -> "Margin % (Low → High)"
                                    "PROFIT_DESC" -> "Gross Profit $"
                                    "REVENUE_DESC" -> "Total Revenue $"
                                    "NAME_ASC" -> "Name (A-Z)"
                                    else -> "Margin %"
                                }
                                Text("Sort: $label", fontSize = 10.sp)
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            }
        }

        // 3. Graphical Chart or Table of Items
        if (displayItems.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No Cost-Inputted Items Found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "This report exclusively tracks transactions with an inputted cost. Enter the Cost & Profit % when recording income to view profit margin analytics.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // Header for Chart / Table
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedViewMode == "CHART") "Graphical Profit Margin Ranking" else "Itemized Profit Breakdown",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${displayItems.size} items",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (selectedViewMode == "CHART") {
                // GRAPHICAL HORIZONTAL BAR CHART
                itemsIndexed(displayItems, key = { _, item -> item.description }) { index, item ->
                    val marginPct = item.profitPercentage.coerceIn(0.0, 100.0) / 100.0
                    val isLoss = item.profitPercentage < 0.0

                    val barBrush = remember(item.profitPercentage) {
                        when {
                            isLoss -> Brush.horizontalGradient(listOf(Color(0xFFEF4444), Color(0xFFDC2626)))
                            item.profitPercentage >= 50.0 -> Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF047857)))
                            item.profitPercentage >= 30.0 -> Brush.horizontalGradient(listOf(Color(0xFF06B6D4), Color(0xFF0E7490)))
                            item.profitPercentage >= 15.0 -> Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706)))
                            else -> Brush.horizontalGradient(listOf(Color(0xFFFB923C), Color(0xFFEA580C)))
                        }
                    }

                    val badgeColor = remember(item.profitPercentage) {
                        when {
                            isLoss -> Color(0xFFDC2626)
                            item.profitPercentage >= 50.0 -> Color(0xFF047857)
                            item.profitPercentage >= 30.0 -> Color(0xFF0E7490)
                            else -> Color(0xFFD97706)
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profit_bar_item_${item.description.filter { it.isLetterOrDigit() }}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Top row: Item Name and Profit Percentage Badge
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
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "#${index + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = item.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${item.count} order${if (item.count == 1) "" else "s"} • Latest: ${dateFormatter.format(Date(item.latestDate))}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                // Profit % Badge
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = badgeColor.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.35f))
                                ) {
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", item.profitPercentage)}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = badgeColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            // Graphical Horizontal Fill Bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(marginPct.toFloat().coerceIn(0.02f, 1f))
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(barBrush)
                                )
                            }

                            // Financial Numbers Breakdown
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Rev: $currencySymbol${String.format(Locale.US, "%.2f", item.totalRevenue)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "Cost: $currencySymbol${String.format(Locale.US, "%.2f", item.totalCost)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "Profit: +$currencySymbol${String.format(Locale.US, "%.2f", item.totalProfit)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            } else {
                // DETAILED TABLE VIEW
                itemsIndexed(displayItems, key = { _, item -> item.description }) { index, item ->
                    val badgeColor = if (item.profitPercentage >= 50.0) Color(0xFF047857)
                    else if (item.profitPercentage >= 30.0) Color(0xFF0E7490)
                    else if (item.profitPercentage >= 0.0) Color(0xFFD97706)
                    else Color(0xFFDC2626)

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Rev: $currencySymbol${String.format(Locale.US, "%.2f", item.totalRevenue)} • Cost: $currencySymbol${String.format(Locale.US, "%.2f", item.totalCost)} • Qty: ${item.count}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "+$currencySymbol${String.format(Locale.US, "%.2f", item.totalProfit)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = badgeColor.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", item.profitPercentage)}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = badgeColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 10.sp
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
