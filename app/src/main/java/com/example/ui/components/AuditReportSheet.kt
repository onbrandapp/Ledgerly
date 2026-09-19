package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AuditDeletedItem
import com.example.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditReportSheet(
    viewModel: ExpenseViewModel,
    userEmail: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val auditList by viewModel.auditDeletedItems.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterType by remember { mutableStateOf("ALL") } // ALL, TRANSACTION, FORECAST, NOTE, RECURRING
    var selectedSourceFilter by remember { mutableStateOf("ALL") } // ALL, USER, EXTERNAL
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var itemToRestore by remember { mutableStateOf<AuditDeletedItem?>(null) }
    var isExporting by remember { mutableStateOf(false) }

    // Filter items
    val filteredItems = remember(auditList, searchQuery, selectedFilterType, selectedSourceFilter) {
        auditList.filter { item ->
            val matchesType = when (selectedFilterType) {
                "TRANSACTION" -> item.itemType.equals("TRANSACTION", ignoreCase = true)
                "FORECAST" -> item.itemType.equals("FORECAST", ignoreCase = true)
                "NOTE" -> item.itemType.equals("NOTE", ignoreCase = true)
                "RECURRING" -> item.itemType.equals("RECURRING", ignoreCase = true)
                else -> true
            }

            val isUser = item.sourceOrDeletedBy.contains("User", ignoreCase = true)
            val matchesSource = when (selectedSourceFilter) {
                "USER" -> isUser
                "EXTERNAL" -> !isUser
                else -> true
            }

            val matchesQuery = if (searchQuery.isBlank()) true else {
                item.title.contains(searchQuery, ignoreCase = true) ||
                        item.details.contains(searchQuery, ignoreCase = true) ||
                        item.sourceOrDeletedBy.contains(searchQuery, ignoreCase = true) ||
                        item.categoryOrStatus.contains(searchQuery, ignoreCase = true)
            }

            matchesType && matchesSource && matchesQuery
        }
    }

    // Counts
    val txCount = auditList.count { it.itemType.equals("TRANSACTION", ignoreCase = true) }
    val forecastCount = auditList.count { it.itemType.equals("FORECAST", ignoreCase = true) }
    val notesCount = auditList.count { it.itemType.equals("NOTE", ignoreCase = true) }
    val recurringCount = auditList.count { it.itemType.equals("RECURRING", ignoreCase = true) }
    val externalCount = auditList.count { !it.sourceOrDeletedBy.contains("User", ignoreCase = true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxHeight(0.94f)
            .testTag("audit_report_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Audit Deletion Report",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Compliance & forensics audit log",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .testTag("close_audit_sheet_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Export to PDF Action Bar
            FilledTonalButton(
                onClick = {
                    isExporting = true
                    AuditPdfExporter.generateAndShareAuditPdf(
                        context = context,
                        userEmail = userEmail,
                        deletedItems = filteredItems,
                        onSuccess = {
                            isExporting = false
                            Toast.makeText(context, "Audit PDF generated successfully", Toast.LENGTH_SHORT).show()
                        },
                        onError = { err ->
                            isExporting = false
                            Toast.makeText(context, "Export error: $err", Toast.LENGTH_LONG).show()
                        }
                    )
                },
                enabled = !isExporting,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .padding(bottom = 4.dp)
                    .testTag("export_pdf_button")
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Generating Audit PDF...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "Export to PDF",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Export Official Audit PDF Report",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Summary Bento Cards Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AuditMetricCard(
                    title = "Total Logged",
                    value = "${auditList.size}",
                    subtitle = "All deleted records",
                    icon = Icons.Default.History,
                    modifier = Modifier.weight(1f)
                )
                AuditMetricCard(
                    title = "External / Other",
                    value = "$externalCount",
                    subtitle = "Non-user sources",
                    icon = Icons.Default.CloudSync,
                    accentColor = Color(0xFFE11D48),
                    modifier = Modifier.weight(1f)
                )
            }

            // Breakdown quick pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilterType == "ALL",
                    onClick = { selectedFilterType = "ALL" },
                    label = { Text("All (${auditList.size})", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("audit_filter_all")
                )
                FilterChip(
                    selected = selectedFilterType == "TRANSACTION",
                    onClick = { selectedFilterType = "TRANSACTION" },
                    leadingIcon = { Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("Transactions ($txCount)", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("audit_filter_transactions")
                )
                FilterChip(
                    selected = selectedFilterType == "FORECAST",
                    onClick = { selectedFilterType = "FORECAST" },
                    leadingIcon = { Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("Forecasting ($forecastCount)", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("audit_filter_forecast")
                )
                FilterChip(
                    selected = selectedFilterType == "NOTE",
                    onClick = { selectedFilterType = "NOTE" },
                    leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("Notes ($notesCount)", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("audit_filter_notes")
                )
                if (recurringCount > 0) {
                    FilterChip(
                        selected = selectedFilterType == "RECURRING",
                        onClick = { selectedFilterType = "RECURRING" },
                        leadingIcon = { Icon(Icons.Default.Autorenew, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Recurring ($recurringCount)", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("audit_filter_recurring")
                    )
                }
            }

            // Source Filter & Simulation Buttons Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Source segmented toggle
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Source:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = {
                            selectedSourceFilter = when (selectedSourceFilter) {
                                "ALL" -> "USER"
                                "USER" -> "EXTERNAL"
                                else -> "ALL"
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(
                            text = when (selectedSourceFilter) {
                                "USER" -> "User Actions Only"
                                "EXTERNAL" -> "External / Sync Only"
                                else -> "All Sources"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Simulation: Test External Deletion trigger button
                TextButton(
                    onClick = {
                        val sampleTypes = listOf("TRANSACTION", "FORECAST", "NOTE")
                        val chosen = sampleTypes.random()
                        when (chosen) {
                            "TRANSACTION" -> viewModel.recordExternalDeletion(
                                itemType = "TRANSACTION",
                                title = "Automated Server Sweep TX",
                                amount = 45.00,
                                categoryOrStatus = "EXPENSE • Cloud Sweep",
                                details = "Removed by automated reconciliation system",
                                sourceOrDeletedBy = "Reconciliation Engine (Remote)"
                            )
                            "FORECAST" -> viewModel.recordExternalDeletion(
                                itemType = "FORECAST",
                                title = "Expired Q3 Consulting Forecast",
                                amount = 3500.00,
                                categoryOrStatus = "TENTATIVE • Consulting",
                                details = "De-scoped by Client Portal Webhook",
                                sourceOrDeletedBy = "External Client Webhook"
                            )
                            "NOTE" -> viewModel.recordExternalDeletion(
                                itemType = "NOTE",
                                title = "Archived Proposal Note",
                                amount = 0.0,
                                categoryOrStatus = "Income Note",
                                details = "Pruned during scheduled data retention sync",
                                sourceOrDeletedBy = "Cloud Retention Cron Job"
                            )
                        }
                        Toast.makeText(context, "Simulated deletion from external source logged", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("simulate_external_delete_button")
                ) {
                    Icon(imageVector = Icons.Default.AddAlert, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Simulate External Source", fontSize = 11.sp)
                }
            }

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("audit_search_input"),
                placeholder = { Text("Search deleted records, sources, categories...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear search", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Content List
            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircleOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (auditList.isEmpty()) "Audit Trail Clean" else "No matching deleted records",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (auditList.isEmpty()) {
                                "When transactions, forecasts, or notes are deleted by you or an external sync source, they will be archived here and exportable to PDF."
                            } else {
                                "Try broadening your filter chips or search keywords."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        AuditItemCard(
                            item = item,
                            onRestore = { itemToRestore = item }
                        )
                    }
                }
            }

            // Bottom Clear History Action
            if (auditList.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Showing ${filteredItems.size} of ${auditList.size} logged events",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    TextButton(
                        onClick = { showClearConfirmDialog = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("clear_audit_history_button")
                    ) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear Audit Trail", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear Audit Trail History?") },
            text = {
                Text("This permanently removes all archived deletion audit records for this account. Active transactions, forecasting entries, and notes are not affected.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAuditDeletedItems()
                        showClearConfirmDialog = false
                        Toast.makeText(context, "Audit log cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    itemToRestore?.let { target ->
        AlertDialog(
            onDismissRequest = { itemToRestore = null },
            title = {
                Text(
                    text = "Restore Deleted ${target.itemType.lowercase().replaceFirstChar { it.uppercase() }}?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Restore \"${target.title}\"${if (target.amount > 0) " ($${String.format(Locale.US, "%,.2f", target.amount)})" else ""} back to your active ${target.itemType.lowercase()}s? This record will be moved out of the audit log."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toRestore = target
                        itemToRestore = null
                        viewModel.restoreDeletedItem(toRestore) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("confirm_restore_dialog_button")
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToRestore = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AuditMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AuditItemCard(
    item: AuditDeletedItem,
    onRestore: () -> Unit
) {
    val isExternal = !item.sourceOrDeletedBy.contains("User", ignoreCase = true)

    val (typeColor, typeBg, typeIcon) = when (item.itemType.uppercase()) {
        "TRANSACTION" -> Triple(
            Color(0xFF4F46E5),
            Color(0xFFEEF2FF),
            Icons.Default.Receipt
        )
        "FORECAST" -> Triple(
            Color(0xFFD97706),
            Color(0xFFFEF3C7),
            Icons.Default.TrendingUp
        )
        "NOTE" -> Triple(
            Color(0xFF059669),
            Color(0xFFECFDF5),
            Icons.Default.Notes
        )
        "RECURRING" -> Triple(
            Color(0xFFE11D48),
            Color(0xFFFFF1F2),
            Icons.Default.Autorenew
        )
        else -> Triple(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer,
            Icons.Default.Delete
        )
    }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()) }
    val deletedDateStr = remember(item.deletedAt) { dateFormat.format(Date(item.deletedAt)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("audit_item_${item.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        border = BorderStroke(1.dp, if (isExternal) Color(0xFFFCA5A5).copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Top Row: Type Pill + Source Badge + Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(typeBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = typeIcon,
                                contentDescription = null,
                                tint = typeColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = item.itemType,
                                color = typeColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Source Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isExternal) Color(0xFFFFE4E6) else Color(0xFFF1F5F9))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isExternal) "Source: ${item.sourceOrDeletedBy}" else "Source: User Action",
                            color = if (isExternal) Color(0xFFE11D48) else Color(0xFF475569),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = deletedDateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title and Amount Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.title.ifBlank { "Untitled ${item.itemType.lowercase().capitalize(Locale.ROOT)}" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (item.amount > 0.0) {
                    Text(
                        text = "$${String.format(Locale.US, "%,.2f", item.amount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Category / Status & Details
            if (item.categoryOrStatus.isNotBlank() || item.details.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.categoryOrStatus.isNotBlank()) {
                        Text(
                            text = item.categoryOrStatus,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (item.details.isNotBlank()) {
                            Text(
                                text = " • ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (item.details.isNotBlank()) {
                        Text(
                            text = item.details,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Restore action row
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                thickness = 0.5.dp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ref: ${item.originalId.take(8).ifBlank { item.id.take(8) }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 10.sp
                )

                FilledTonalButton(
                    onClick = onRestore,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("restore_button_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = "Restore ${item.title}",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Restore",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
