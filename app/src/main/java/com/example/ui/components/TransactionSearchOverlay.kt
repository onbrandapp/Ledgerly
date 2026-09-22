package com.example.ui.components

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.CustomCategory
import com.example.data.DuplicateTransactionDetector
import com.example.data.Transaction
import com.example.ui.theme.CategoryConstants
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class SearchDatePreset(val label: String) {
    ALL_TIME("All Dates"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    LAST_30_DAYS("Last 30 Days"),
    CUSTOM("Custom Range")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionSearchOverlay(
    transactions: List<Transaction>,
    customCategories: List<CustomCategory> = emptyList(),
    onDismiss: () -> Unit,
    onTransactionClick: (Transaction) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    // Search filters state
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) } // null = All Categories
    var selectedDatePreset by remember { mutableStateOf(SearchDatePreset.ALL_TIME) }
    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }
    var showCustomDatePickers by remember { mutableStateOf(false) }

    // Extract unique categories from actual transactions and custom categories
    val allCategories = remember(transactions, customCategories) {
        val catNames = linkedSetOf<String>()
        transactions.forEach { tx ->
            if (tx.category.isNotBlank()) {
                catNames.add(tx.category)
            }
        }
        customCategories.forEach { cc ->
            if (cc.name.isNotBlank()) {
                catNames.add(cc.name)
            }
        }
        catNames.toList().sorted()
    }

    // Helper to calculate date range based on preset
    fun applyDatePreset(preset: SearchDatePreset) {
        selectedDatePreset = preset
        val cal = Calendar.getInstance()
        when (preset) {
            SearchDatePreset.ALL_TIME -> {
                startDate = null
                endDate = null
                showCustomDatePickers = false
            }
            SearchDatePreset.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis

                startDate = start
                endDate = end
                showCustomDatePickers = false
            }
            SearchDatePreset.THIS_WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                cal.add(Calendar.DAY_OF_WEEK, 6)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis

                startDate = start
                endDate = end
                showCustomDatePickers = false
            }
            SearchDatePreset.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis

                startDate = start
                endDate = end
                showCustomDatePickers = false
            }
            SearchDatePreset.LAST_30_DAYS -> {
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis

                cal.add(Calendar.DAY_OF_YEAR, -30)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                startDate = start
                endDate = end
                showCustomDatePickers = false
            }
            SearchDatePreset.CUSTOM -> {
                showCustomDatePickers = true
                if (startDate == null) {
                    val c = Calendar.getInstance()
                    c.add(Calendar.DAY_OF_YEAR, -7)
                    c.set(Calendar.HOUR_OF_DAY, 0)
                    c.set(Calendar.MINUTE, 0)
                    c.set(Calendar.SECOND, 0)
                    c.set(Calendar.MILLISECOND, 0)
                    startDate = c.timeInMillis
                }
                if (endDate == null) {
                    val c = Calendar.getInstance()
                    c.set(Calendar.HOUR_OF_DAY, 23)
                    c.set(Calendar.MINUTE, 59)
                    c.set(Calendar.SECOND, 59)
                    c.set(Calendar.MILLISECOND, 999)
                    endDate = c.timeInMillis
                }
            }
        }
    }

    fun openStartDatePicker() {
        val initialCal = Calendar.getInstance()
        startDate?.let { initialCal.timeInMillis = it }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val chosen = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                startDate = chosen
                if (endDate != null && endDate!! < chosen) {
                    endDate = chosen + (24 * 60 * 60 * 1000L - 1)
                }
            },
            initialCal.get(Calendar.YEAR),
            initialCal.get(Calendar.MONTH),
            initialCal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun openEndDatePicker() {
        val initialCal = Calendar.getInstance()
        endDate?.let { initialCal.timeInMillis = it }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val chosen = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis
                endDate = chosen
                if (startDate != null && startDate!! > chosen) {
                    startDate = chosen - (24 * 60 * 60 * 1000L - 1)
                }
            },
            initialCal.get(Calendar.YEAR),
            initialCal.get(Calendar.MONTH),
            initialCal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun resetAllFilters() {
        searchQuery = ""
        selectedCategory = null
        selectedDatePreset = SearchDatePreset.ALL_TIME
        startDate = null
        endDate = null
        showCustomDatePickers = false
    }

    val isFilterActive = searchQuery.isNotBlank() || selectedCategory != null || selectedDatePreset != SearchDatePreset.ALL_TIME || startDate != null || endDate != null

    val duplicateTxIds = remember(transactions) {
        DuplicateTransactionDetector.findAllDuplicateIds(transactions)
    }

    // Compute filtered transactions
    val filteredTransactions = remember(
        transactions,
        searchQuery,
        selectedCategory,
        startDate,
        endDate
    ) {
        transactions.filter { tx ->
            val matchesDescription = if (searchQuery.isBlank()) true else {
                tx.description.contains(searchQuery.trim(), ignoreCase = true)
            }
            val matchesCategory = if (selectedCategory == null) true else {
                tx.category.equals(selectedCategory, ignoreCase = true)
            }
            val matchesDate = when {
                startDate != null && endDate != null -> tx.date in startDate!!..endDate!!
                startDate != null -> tx.date >= startDate!!
                endDate != null -> tx.date <= endDate!!
                else -> true
            }
            matchesDescription && matchesCategory && matchesDate
        }.sortedByDescending { it.date }
    }

    val totalIncome = remember(filteredTransactions) {
        filteredTransactions.filter { it.type.equals("INCOME", ignoreCase = true) }.sumOf { it.amount }
    }
    val totalExpense = remember(filteredTransactions) {
        filteredTransactions.filter { !it.type.equals("INCOME", ignoreCase = true) }.sumOf { it.amount }
    }
    val netBalance = totalIncome - totalExpense

    LaunchedEffect(Unit) {
        // Auto-request focus for quick searching
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag("transaction_search_overlay"),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp)
            ) {
                // 1. TOP HEADER & SEARCH INPUT
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("search_overlay_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Close search",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Search input bar
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))

                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text(
                                        text = "Search description...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        maxLines = 1
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester)
                                    .testTag("search_overlay_input_field")
                            )

                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear text",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (isFilterActive) {
                        TextButton(
                            onClick = { resetAllFilters() },
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier.testTag("search_reset_all_button")
                        ) {
                            Text(
                                text = "Reset",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 2. CATEGORY FILTER CHIPS
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "CATEGORY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.8.sp,
                            fontSize = 10.sp
                        )
                        if (selectedCategory != null) {
                            Text(
                                text = "Clear",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { selectedCategory = null }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // "All Categories" chip
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("All Categories", fontSize = 12.sp) },
                            shape = RoundedCornerShape(16.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedCategory == null,
                                borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.testTag("search_cat_chip_all")
                        )

                        // Category chips
                        allCategories.forEach { categoryName ->
                            val catStyle = remember(categoryName, customCategories) {
                                CategoryConstants.resolveCategoryStyle(categoryName, customCategories)
                            }
                            val isSelected = selectedCategory.equals(categoryName, ignoreCase = true)

                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedCategory = if (isSelected) null else categoryName
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = catStyle.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else catStyle.color,
                                        modifier = Modifier.size(15.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = categoryName,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.testTag("search_cat_chip_$categoryName")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 3. DATE RANGE PRESETS & PICKER
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "DATE RANGE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.8.sp,
                            fontSize = 10.sp
                        )
                        if (startDate != null || endDate != null || selectedDatePreset != SearchDatePreset.ALL_TIME) {
                            Text(
                                text = "All Dates",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { applyDatePreset(SearchDatePreset.ALL_TIME) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SearchDatePreset.values().forEach { preset ->
                            val isSelected = selectedDatePreset == preset
                            FilterChip(
                                selected = isSelected,
                                onClick = { applyDatePreset(preset) },
                                label = {
                                    Text(
                                        text = preset.label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.testTag("search_date_preset_${preset.name}")
                            )
                        }
                    }

                    // Custom Date Range Display / Pickers
                    AnimatedVisibility(
                        visible = showCustomDatePickers || (startDate != null && endDate != null && selectedDatePreset == SearchDatePreset.CUSTOM),
                        enter = slideInVertically() + fadeIn(),
                        exit = slideOutVertically() + fadeOut()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Start date picker chip
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { openStartDatePicker() }
                                    .testTag("search_start_date_picker")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "FROM",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = startDate?.let { dateFormatter.format(Date(it)) } ?: "Select Date",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            // End date picker chip
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { openEndDatePicker() }
                                    .testTag("search_end_date_picker")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "TO",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = endDate?.let { dateFormatter.format(Date(it)) } ?: "Select Date",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4. RESULTS STATS STRIP
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${filteredTransactions.size} Results",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isFilterActive) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "Filtered",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = String.format(Locale.US, "+$%.2f", totalIncome),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                            Text(
                                text = String.format(Locale.US, "-$%.2f", totalExpense),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (totalExpense > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format(Locale.US, "Net: $%.2f", netBalance),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (netBalance >= 0) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 5. TRANSACTIONS RESULTS LIST
                if (filteredTransactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SearchOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Text(
                                text = "No transactions found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = "No records matched your description keywords, category, or date range.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            if (isFilterActive) {
                                Button(
                                    onClick = { resetAllFilters() },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier.testTag("search_empty_reset_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Clear All Filters")
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp)
                    ) {
                        items(
                            items = filteredTransactions,
                            key = { it.id.ifEmpty { "${it.date}_${it.description}_${it.amount}" } }
                        ) { tx ->
                            val catStyle = remember(tx.category, customCategories) {
                                CategoryConstants.resolveCategoryStyle(tx.category, customCategories)
                            }
                            val isIncome = tx.type.equals("INCOME", ignoreCase = true)

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onTransactionClick(tx) }
                                    .testTag("search_result_tx_${tx.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Category icon bubble
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .background(catStyle.color.copy(alpha = 0.15f), CircleShape)
                                            .border(BorderStroke(1.dp, catStyle.color.copy(alpha = 0.35f)), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = catStyle.icon,
                                            contentDescription = null,
                                            tint = catStyle.color,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Description, category, date
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = tx.description.ifBlank { "No description" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = tx.category.ifBlank { "Uncategorized" },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = catStyle.color,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "•",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                            Text(
                                                text = dateFormatter.format(Date(tx.date)),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (tx.paid) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFF10B981).copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = "Paid",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF10B981),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            if (tx.id in duplicateTxIds) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFFFFF8E1),
                                                    border = BorderStroke(0.5.dp, Color(0xFFF59E0B))
                                                ) {
                                                    Text(
                                                        text = "Duplicate",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFB45309),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Amount and arrow
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = String.format(
                                                Locale.US,
                                                "%s$%.2f",
                                                if (isIncome) "+" else "-",
                                                tx.amount
                                            ),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isIncome) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
                                        )

                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = "Edit transaction",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(18.dp)
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
}
