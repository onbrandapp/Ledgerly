package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ForecastIncome
import com.example.data.FutureIncomeNote
import com.example.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForecastIncomeSection(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val forecastIncomes by viewModel.forecastIncomes.collectAsState()
    val futureIncomeNotes by viewModel.futureIncomeNotes.collectAsState()
    val forecastSummary by viewModel.forecastSummary.collectAsState()

    var forecastSubTab by remember { mutableStateOf(0) } // 0 = Pipeline Forecast, 1 = Bulleted Notes & Ideas
    var statusFilter by remember { mutableStateOf("ALL") } // "ALL", "CONFIRMED", "EXPECTED", "TENTATIVE", "REALIZED"

    var showAddForecastDialog by remember { mutableStateOf(false) }
    var editingForecast by remember { mutableStateOf<ForecastIncome?>(null) }
    var forecastToRealize by remember { mutableStateOf<ForecastIncome?>(null) }

    var showAddNoteDialog by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<FutureIncomeNote?>(null) }

    val filteredForecasts = remember(forecastIncomes, statusFilter) {
        when (statusFilter) {
            "CONFIRMED" -> forecastIncomes.filter { it.status.equals("CONFIRMED", ignoreCase = true) && !it.isRealized }
            "EXPECTED" -> forecastIncomes.filter { it.status.equals("EXPECTED", ignoreCase = true) && !it.isRealized }
            "TENTATIVE" -> forecastIncomes.filter { it.status.equals("TENTATIVE", ignoreCase = true) && !it.isRealized }
            "REALIZED" -> forecastIncomes.filter { it.isRealized }
            else -> forecastIncomes.filter { !it.isRealized }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 80.dp)
    ) {
        // --- 1. PIPELINE SUMMARY BENTO CARD ---
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "FUTURE INCOME FORECAST",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "Pipeline & Planning",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${forecastSummary.activeCount} upcoming",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = String.format(Locale.US, "$%.2f", forecastSummary.totalPipeline),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Projected upcoming income not yet credited to active ledger",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                // Breakdown Chips Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusSummaryPill(
                        label = "Confirmed",
                        amount = forecastSummary.confirmedAmount,
                        color = Color(0xFF2E7D32),
                        backgroundColor = Color(0xFFE8F5E9),
                        modifier = Modifier.weight(1f)
                    )
                    StatusSummaryPill(
                        label = "Expected",
                        amount = forecastSummary.expectedAmount,
                        color = Color(0xFFD97706),
                        backgroundColor = Color(0xFFFEF3C7),
                        modifier = Modifier.weight(1f)
                    )
                    StatusSummaryPill(
                        label = "Tentative",
                        amount = forecastSummary.tentativeAmount,
                        color = Color(0xFF7C3AED),
                        backgroundColor = Color(0xFFEDE9FE),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // --- 2. SUB-TABS (Pipeline vs Bulleted Notes) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = forecastSubTab == 0,
                onClick = { forecastSubTab = 0 },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = {
                    Text(
                        text = "Income Pipeline (${forecastIncomes.count { !it.isRealized }})",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                },
                modifier = Modifier.testTag("tab_forecast_pipeline")
            )
            FilterChip(
                selected = forecastSubTab == 1,
                onClick = { forecastSubTab = 1 },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.FormatListBulleted,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = {
                    Text(
                        text = "Notes & Ideas (${futureIncomeNotes.size})",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                },
                modifier = Modifier.testTag("tab_forecast_notes")
            )
        }

        // --- 3. TAB 0: PIPELINE FORECAST CONTENT ---
        if (forecastSubTab == 0) {
            // Action bar & status filter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick filter chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val filters = listOf(
                        "ALL" to "All Active",
                        "CONFIRMED" to "Confirmed",
                        "EXPECTED" to "Expected",
                        "TENTATIVE" to "Tentative",
                        "REALIZED" to "Realized"
                    )
                    items(filters) { (key, label) ->
                        val isSelected = statusFilter == key
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clickable { statusFilter = key }
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                FilledTonalButton(
                    onClick = {
                        editingForecast = null
                        showAddForecastDialog = true
                    },
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .wrapContentWidth()
                        .testTag("add_forecast_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Add Forecast",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredForecasts.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (statusFilter == "REALIZED") "No Realized Forecasts Yet" else "No Forecasted Income In This View",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Track upcoming invoices, anticipated client retainers, bonuses, or prospective income with bulleted milestones.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                editingForecast = null
                                showAddForecastDialog = true
                            },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Your First Forecast")
                        }
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    filteredForecasts.forEach { item ->
                        ForecastIncomeCard(
                            item = item,
                            onEdit = {
                                editingForecast = item
                                showAddForecastDialog = true
                            },
                            onDelete = { viewModel.deleteForecastIncome(item.id) },
                            onToggleBullet = { bulletIndex ->
                                viewModel.toggleForecastBulletCompleted(item.id, bulletIndex)
                            },
                            onRealize = { forecastToRealize = item },
                            onToggleRealizedOnly = { viewModel.toggleForecastRealizedState(item.id) }
                        )
                    }
                }
            }
        }

        // --- 4. TAB 1: BULLETED NOTES & IDEAS CONTENT ---
        if (forecastSubTab == 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Income Notes & Ideas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.width(8.dp))

                FilledTonalButton(
                    onClick = {
                        editingNote = null
                        showAddNoteDialog = true
                    },
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier
                        .wrapContentWidth()
                        .testTag("add_income_note_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "New Note",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (futureIncomeNotes.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatListBulleted,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Future Income Notes Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Add bulleted lists of future income streams, prospective client lists, rate increase ideas, or side hustle goals.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                editingNote = null
                                showAddNoteDialog = true
                            },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create Bulleted Note")
                        }
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    futureIncomeNotes.forEach { note ->
                        FutureIncomeNoteCard(
                            note = note,
                            onEdit = {
                                editingNote = note
                                showAddNoteDialog = true
                            },
                            onDelete = { viewModel.deleteFutureIncomeNote(note.id) },
                            onToggleBullet = { bulletIndex ->
                                viewModel.toggleNoteBulletCompleted(note.id, bulletIndex)
                            }
                        )
                    }
                }
            }
        }
    }

    // --- DIALOGS ---
    if (showAddForecastDialog) {
        AddEditForecastIncomeDialog(
            initialItem = editingForecast,
            onDismiss = {
                showAddForecastDialog = false
                editingForecast = null
            },
            onSave = { title, amount, expectedDate, category, status, notes, bullets ->
                viewModel.addOrUpdateForecastIncome(
                    title = title,
                    amount = amount,
                    expectedDate = expectedDate,
                    category = category,
                    status = status,
                    notes = notes,
                    bulletPoints = bullets,
                    id = editingForecast?.id ?: "",
                    completedBullets = editingForecast?.completedBullets ?: emptyList(),
                    isRealized = editingForecast?.isRealized ?: false
                )
                showAddForecastDialog = false
                editingForecast = null
            }
        )
    }

    if (showAddNoteDialog) {
        AddEditFutureIncomeNoteDialog(
            initialNote = editingNote,
            onDismiss = {
                showAddNoteDialog = false
                editingNote = null
            },
            onSave = { title, content, bullets, colorTag ->
                viewModel.addOrUpdateFutureIncomeNote(
                    title = title,
                    content = content,
                    bulletPoints = bullets,
                    id = editingNote?.id ?: "",
                    colorTag = colorTag,
                    completedBullets = editingNote?.completedBullets ?: emptyList()
                )
                showAddNoteDialog = false
                editingNote = null
            }
        )
    }

    forecastToRealize?.let { forecast ->
        AlertDialog(
            onDismissRequest = { forecastToRealize = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Realize Forecasted Income", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Would you like to move this forecasted item directly into your active Ledgerly Income?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFE8F5E9),
                        border = BorderStroke(1.dp, Color(0xFFC8E6C9)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = forecast.title,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20)
                            )
                            Text(
                                text = String.format(Locale.US, "+$%.2f  •  %s", forecast.amount, forecast.category),
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This will create an actual Income transaction in your active ledger.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.convertForecastToActualIncome(forecast)
                        forecastToRealize = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("Add to Ledger")
                }
            },
            dismissButton = {
                TextButton(onClick = { forecastToRealize = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StatusSummaryPill(
    label: String,
    amount: Double,
    color: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = backgroundColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text(
                text = label.uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
            Text(
                text = String.format(Locale.US, "$%.0f", amount),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
    }
}

@Composable
fun ForecastIncomeCard(
    item: ForecastIncome,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleBullet: (Int) -> Unit,
    onRealize: () -> Unit,
    onToggleRealizedOnly: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val dateStr = remember(item.expectedDate) { dateFormatter.format(Date(item.expectedDate)) }

    val (statusColor, statusBg, statusLabel) = when (item.status.uppercase()) {
        "CONFIRMED" -> Triple(Color(0xFF2E7D32), Color(0xFFE8F5E9), "Confirmed")
        "EXPECTED" -> Triple(Color(0xFFD97706), Color(0xFFFEF3C7), "Expected")
        else -> Triple(Color(0xFF7C3AED), Color(0xFFEDE9FE), "Tentative")
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (item.isRealized) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            width = if (item.isRealized) 1.dp else 1.5.dp,
            color = if (item.isRealized) MaterialTheme.colorScheme.outline.copy(alpha = 0.3f) else statusColor.copy(alpha = 0.4f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Title + Category + Status Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (item.isRealized) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (item.isRealized) TextDecoration.LineThrough else TextDecoration.None
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = item.category.ifBlank { "Income" },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Target: $dateStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (item.isRealized) Color(0xFFE0E0E0) else statusBg,
                    border = BorderStroke(1.dp, if (item.isRealized) Color(0xFFBDBDBD) else statusColor.copy(alpha = 0.3f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (item.isRealized) Color.Gray else statusColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (item.isRealized) "Realized" else statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (item.isRealized) Color.DarkGray else statusColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Amount row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format(Locale.US, "+$%.2f", item.amount),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = if (item.isRealized) Color.Gray else Color(0xFF2E7D32)
                )

                if (!item.isRealized) {
                    Button(
                        onClick = onRealize,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Received / Realize",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onToggleRealizedOnly,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(
                            text = "Undo Realized",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            // Optional Notes Paragraph
            if (item.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = item.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // Bulleted Milestones / Items
            if (item.bulletPoints.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Checklist,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Milestones & Checklist (${item.completedBullets.size}/${item.bulletPoints.size})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    item.bulletPoints.forEachIndexed { index, bullet ->
                        val isDone = item.completedBullets.contains(index)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onToggleBullet(index) }
                                .padding(vertical = 2.dp, horizontal = 4.dp)
                        ) {
                            Checkbox(
                                checked = isDone,
                                onCheckedChange = { onToggleBullet(index) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = bullet,
                                style = MaterialTheme.typography.bodySmall,
                                textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                                color = if (isDone) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Bottom Actions: Edit & Delete
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Forecast",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Forecast",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FutureIncomeNoteCard(
    note: FutureIncomeNote,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleBullet: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault()) }
    val dateStr = remember(note.updatedAt) { dateFormatter.format(Date(note.updatedAt)) }

    val accentColor = remember(note.colorTag) {
        try {
            Color(android.graphics.Color.parseColor(note.colorTag))
        } catch (e: Exception) {
            Color(0xFFFFD97D)
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.6f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Accent bar + Title + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .width(5.dp)
                            .height(22.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(accentColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = note.title.ifBlank { "Untitled Note" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Note",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Note",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Text(
                text = "Updated $dateStr",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )

            // Content if any
            if (note.content.isNotBlank()) {
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            // Bulleted items list
            if (note.bulletPoints.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    note.bulletPoints.forEachIndexed { index, bullet ->
                        val isDone = note.completedBullets.contains(index)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onToggleBullet(index) }
                                .padding(vertical = 2.dp)
                        ) {
                            Checkbox(
                                checked = isDone,
                                onCheckedChange = { onToggleBullet(index) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = accentColor,
                                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = bullet,
                                style = MaterialTheme.typography.bodyMedium,
                                textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                                color = if (isDone) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditForecastIncomeDialog(
    initialItem: ForecastIncome?,
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Double, expectedDate: Long, category: String, status: String, notes: String, bullets: List<String>) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(initialItem?.title ?: "") }
    var amountText by remember { mutableStateOf(if (initialItem != null && initialItem.amount > 0) initialItem.amount.toString() else "") }
    var expectedDate by remember { mutableStateOf(initialItem?.expectedDate ?: (System.currentTimeMillis() + 14L * 24 * 60 * 60 * 1000)) }
    var category by remember { mutableStateOf(initialItem?.category ?: "Freelance") }
    var status by remember { mutableStateOf(initialItem?.status ?: "EXPECTED") }
    var notes by remember { mutableStateOf(initialItem?.notes ?: "") }

    var bulletsList by remember { mutableStateOf(initialItem?.bulletPoints ?: emptyList()) }
    var newBulletInput by remember { mutableStateOf("") }

    var isCategoryDropdownOpen by remember { mutableStateOf(false) }

    val categories = listOf("Freelance", "Client Invoice", "Bonus", "Salary Increase", "Dividend/Investment", "Tax Refund", "Product Sales", "Contract Gig", "Other")

    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val dateDisplay = remember(expectedDate) { dateFormatter.format(Date(expectedDate)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialItem != null) "Edit Forecasted Income" else "New Forecasted Income",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Income Source / Project Name") },
                    placeholder = { Text("e.g. Q4 Website Redesign, Year-end Bonus") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Expected Amount ($)") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category selection
                ExposedDropdownMenuBox(
                    expanded = isCategoryDropdownOpen,
                    onExpandedChange = { isCategoryDropdownOpen = !isCategoryDropdownOpen }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownOpen) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isCategoryDropdownOpen,
                        onDismissRequest = { isCategoryDropdownOpen = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    isCategoryDropdownOpen = false
                                }
                            )
                        }
                    }
                }

                // Confidence / Status
                Text("Forecast Confidence / Status", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val statusOptions = listOf("CONFIRMED" to "Confirmed", "EXPECTED" to "Expected", "TENTATIVE" to "Tentative")
                    statusOptions.forEach { (key, label) ->
                        val isSelected = status == key
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { status = key }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                // Target Date Picker
                OutlinedTextField(
                    value = dateDisplay,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Expected Target Date") },
                    trailingIcon = {
                        IconButton(onClick = {
                            val cal = Calendar.getInstance().apply { timeInMillis = expectedDate }
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val newCal = Calendar.getInstance().apply {
                                        set(year, month, day, 12, 0, 0)
                                    }
                                    expectedDate = newCal.timeInMillis
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Pick Date")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Notes input
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("Client contact, terms, payment methods...") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                // --- Bulleted Milestones / Items Builder ---
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Milestones & Bullet Points (${bulletsList.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Existing Bullets
                if (bulletsList.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        bulletsList.forEachIndexed { index, b ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("•", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = b,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            bulletsList = bulletsList.filterIndexed { i, _ -> i != index }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Add new bullet item row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newBulletInput,
                        onValueChange = { newBulletInput = it },
                        placeholder = { Text("e.g. Deposit paid, Deliver draft...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = {
                            if (newBulletInput.isNotBlank()) {
                                bulletsList = bulletsList + newBulletInput.trim()
                                newBulletInput = ""
                            }
                        },
                        enabled = newBulletInput.isNotBlank()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add bullet")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountVal = amountText.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank()) {
                        val finalBullets = if (newBulletInput.isNotBlank()) {
                            bulletsList + newBulletInput.trim()
                        } else {
                            bulletsList
                        }
                        onSave(title, amountVal, expectedDate, category, status, notes, finalBullets)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Save Forecast")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddEditFutureIncomeNoteDialog(
    initialNote: FutureIncomeNote?,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, bullets: List<String>, colorTag: String) -> Unit
) {
    var title by remember { mutableStateOf(initialNote?.title ?: "") }
    var content by remember { mutableStateOf(initialNote?.content ?: "") }
    var colorTag by remember { mutableStateOf(initialNote?.colorTag ?: "#FFD97D") }

    var bulletsList by remember { mutableStateOf(initialNote?.bulletPoints ?: emptyList()) }
    var newBulletInput by remember { mutableStateOf("") }

    val presetColors = listOf("#FFD97D", "#A78BFA", "#D9F99D", "#93C5FD", "#FCA5A5", "#FDBA74")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialNote != null) "Edit Income Note" else "New Income Planning Note",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Note Title") },
                    placeholder = { Text("e.g. Q4 Side Hustles, Prospective Retainers") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Description / Ideas") },
                    placeholder = { Text("Brainstorm future income plans, targets, or details...") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                // Color accent picker
                Text("Note Color Accent", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    presetColors.forEach { hex ->
                        val col = remember(hex) {
                            try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.Gray }
                        }
                        val isSelected = colorTag.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(col)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { colorTag = hex }
                        )
                    }
                }

                // Bullet items builder
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = "Bulleted Items & Action List (${bulletsList.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                if (bulletsList.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        bulletsList.forEachIndexed { index, b ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("•", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = b,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            bulletsList = bulletsList.filterIndexed { i, _ -> i != index }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Add new bullet item row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newBulletInput,
                        onValueChange = { newBulletInput = it },
                        placeholder = { Text("Add bulleted item...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = {
                            if (newBulletInput.isNotBlank()) {
                                bulletsList = bulletsList + newBulletInput.trim()
                                newBulletInput = ""
                            }
                        },
                        enabled = newBulletInput.isNotBlank()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add bullet")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() || content.isNotBlank() || bulletsList.isNotEmpty() || newBulletInput.isNotBlank()) {
                        val finalBullets = if (newBulletInput.isNotBlank()) {
                            bulletsList + newBulletInput.trim()
                        } else {
                            bulletsList
                        }
                        onSave(title, content, finalBullets, colorTag)
                    }
                },
                enabled = title.isNotBlank() || content.isNotBlank() || bulletsList.isNotEmpty() || newBulletInput.isNotBlank()
            ) {
                Text("Save Note")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
