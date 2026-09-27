package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CategoryUsage
import com.example.data.CustomCategory
import com.example.ui.theme.CategoryConstants
import com.example.ui.viewmodel.ExpenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkCategoryReassignmentDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    val categoryUsageList by viewModel.categoryUsageList.collectAsState()
    val customCategoriesList by viewModel.customCategories.collectAsState()

    var selectedSourceCategory by remember { mutableStateOf<CategoryUsage?>(null) }
    var targetCategoryName by remember { mutableStateOf("") }
    var includeRecurring by remember { mutableStateOf(true) }
    var includeForecast by remember { mutableStateOf(true) }

    var isProcessing by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showConfirmPrompt by remember { mutableStateOf(false) }
    var showSourcePickerDropdown by remember { mutableStateOf(false) }
    var targetCategorySearchQuery by remember { mutableStateOf("") }

    // If initial source is not set and usage list is not empty, select the first one
    LaunchedEffect(categoryUsageList) {
        if (selectedSourceCategory == null && categoryUsageList.isNotEmpty()) {
            selectedSourceCategory = categoryUsageList.first()
        } else if (selectedSourceCategory != null) {
            // Keep source up to date with new counts
            val updated = categoryUsageList.find { it.name.equals(selectedSourceCategory?.name, ignoreCase = true) }
            selectedSourceCategory = updated
        }
    }

    val sourceStyle = remember(selectedSourceCategory, customCategoriesList) {
        selectedSourceCategory?.let {
            CategoryConstants.resolveCategoryStyle(it.name, customCategoriesList)
        }
    }

    val targetStyle = remember(targetCategoryName, customCategoriesList) {
        if (targetCategoryName.isNotBlank()) {
            CategoryConstants.resolveCategoryStyle(targetCategoryName, customCategoriesList)
        } else null
    }

    // Calculate affected entries count based on checkboxes
    val affectedEntriesCount = remember(selectedSourceCategory, includeRecurring, includeForecast) {
        val src = selectedSourceCategory ?: return@remember 0
        var total = src.transactionCount
        if (includeRecurring) total += src.recurringCount
        if (includeForecast) total += src.forecastCount
        total
    }

    // Curated list of target category suggestions (predefined icons + custom categories)
    val availableTargetSuggestions = remember(customCategoriesList, targetCategorySearchQuery) {
        val predefinedNames = CategoryConstants.PREDEFINED_ICONS.map { it.label }
        val customNames = customCategoriesList.map { it.name }
        val combined = (customNames + predefinedNames).distinct()
        if (targetCategorySearchQuery.isBlank()) {
            combined
        } else {
            combined.filter { it.contains(targetCategorySearchQuery.trim(), ignoreCase = true) }
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!isProcessing) onDismiss()
        },
        modifier = modifier
            .fillMaxWidth()
            .testTag("bulk_category_reassignment_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bulk Category Utility",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Reassign all entries from one category to another",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Success / Info Banner
                if (successMessage != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bulk_reassign_success_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Reassignment Complete",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = successMessage.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Error Banner
                if (errorMessage != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = errorMessage.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                if (categoryUsageList.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Inbox,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "No Categorized Entries Found",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Add transactions first to reassign categories in bulk.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    // STEP 1: CURRENT CATEGORY (FROM)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "1. Current Category (From)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Dropdown/Selector Card
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showSourcePickerDropdown = !showSourcePickerDropdown }
                                    .testTag("source_category_selector")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val currentSrc = selectedSourceCategory
                                    if (currentSrc != null && sourceStyle != null) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .background(sourceStyle.color.copy(alpha = 0.16f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = sourceStyle.icon,
                                                    contentDescription = null,
                                                    tint = sourceStyle.color,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = currentSrc.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "${currentSrc.totalCount} total entries (${currentSrc.transactionCount} tx" +
                                                            if (currentSrc.recurringCount > 0) ", ${currentSrc.recurringCount} recurring" else "" +
                                                            if (currentSrc.forecastCount > 0) ", ${currentSrc.forecastCount} forecast" else "" + ")",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = "Select source category",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Icon(
                                        imageVector = if (showSourcePickerDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Expandable Source Categories List
                            AnimatedVisibility(visible = showSourcePickerDropdown) {
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(6.dp)
                                            .heightIn(max = 200.dp)
                                    ) {
                                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                            items(categoryUsageList) { usage ->
                                                val isSelected = selectedSourceCategory?.name.equals(usage.name, ignoreCase = true)
                                                val style = CategoryConstants.resolveCategoryStyle(usage.name, customCategoriesList)

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(
                                                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                            else Color.Transparent
                                                        )
                                                        .clickable {
                                                            selectedSourceCategory = usage
                                                            showSourcePickerDropdown = false
                                                            errorMessage = null
                                                            successMessage = null
                                                        }
                                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                                        .testTag("source_option_${usage.name}"),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(28.dp)
                                                                .background(style.color.copy(alpha = 0.15f), CircleShape),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = style.icon,
                                                                contentDescription = null,
                                                                tint = style.color,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                        Text(
                                                            text = usage.name,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }

                                                    Surface(
                                                        shape = RoundedCornerShape(12.dp),
                                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                                    ) {
                                                        Text(
                                                            text = "${usage.totalCount} entries",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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

                    // STEP 2: NEW CATEGORY (TO)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "2. New Category (To)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Target Category Text Input
                            OutlinedTextField(
                                value = targetCategoryName,
                                onValueChange = {
                                    targetCategoryName = it
                                    targetCategorySearchQuery = it
                                    errorMessage = null
                                    successMessage = null
                                },
                                label = { Text("Assign New Category") },
                                placeholder = { Text("e.g. Groceries, Food & Dining") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                leadingIcon = {
                                    if (targetStyle != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .background(targetStyle.color.copy(alpha = 0.15f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = targetStyle.icon,
                                                contentDescription = null,
                                                tint = targetStyle.color,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Category,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                trailingIcon = {
                                    if (targetCategoryName.isNotBlank()) {
                                        IconButton(onClick = {
                                            targetCategoryName = ""
                                            targetCategorySearchQuery = ""
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Clear",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("target_category_input")
                            )

                            // Quick Suggestions Row
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Quick Select Suggestion:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(availableTargetSuggestions.take(12)) { suggestion ->
                                        val isSelected = targetCategoryName.equals(suggestion, ignoreCase = true)
                                        val sugStyle = CategoryConstants.resolveCategoryStyle(suggestion, customCategoriesList)

                                        SuggestionChip(
                                            onClick = {
                                                targetCategoryName = suggestion
                                                targetCategorySearchQuery = ""
                                                errorMessage = null
                                                successMessage = null
                                            },
                                            label = {
                                                Text(
                                                    text = suggestion,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            icon = {
                                                Icon(
                                                    imageVector = sugStyle.icon,
                                                    contentDescription = null,
                                                    tint = sugStyle.color,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            },
                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                containerColor = if (isSelected) sugStyle.color.copy(alpha = 0.15f)
                                                else MaterialTheme.colorScheme.surface
                                            ),
                                            border = SuggestionChipDefaults.suggestionChipBorder(
                                                enabled = true,
                                                borderColor = if (isSelected) sugStyle.color
                                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                            ),
                                            modifier = Modifier.testTag("suggestion_chip_${suggestion}")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // STEP 3: SCOPE & OPTIONS
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Scope of Change",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )

                                // Transactions Row (Always applied)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ReceiptLong,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Standard Transactions",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Text(
                                        text = "${selectedSourceCategory?.transactionCount ?: 0} entries",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )

                                // Recurring Transactions Option
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { includeRecurring = !includeRecurring },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Checkbox(
                                            checked = includeRecurring,
                                            onCheckedChange = { includeRecurring = it },
                                            modifier = Modifier.size(24.dp).testTag("include_recurring_checkbox")
                                        )
                                        Text(
                                            text = "Recurring Transaction Series",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Text(
                                        text = "${selectedSourceCategory?.recurringCount ?: 0} rules",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if ((selectedSourceCategory?.forecastCount ?: 0) > 0) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    )

                                    // Forecast Income Option
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { includeForecast = !includeForecast },
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Checkbox(
                                                checked = includeForecast,
                                                onCheckedChange = { includeForecast = it },
                                                modifier = Modifier.size(24.dp).testTag("include_forecast_checkbox")
                                            )
                                            Text(
                                                text = "Forecast Income Items",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        Text(
                                            text = "${selectedSourceCategory?.forecastCount ?: 0} entries",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // STEP 4: VISUAL TRANSFORMATION PREVIEW
                    if (selectedSourceCategory != null && targetCategoryName.isNotBlank() && sourceStyle != null && targetStyle != null) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("bulk_reassignment_preview_card")
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Source Tag
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .background(sourceStyle.color.copy(alpha = 0.2f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = sourceStyle.icon,
                                                    contentDescription = null,
                                                    tint = sourceStyle.color,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Text(
                                                text = selectedSourceCategory?.name.orEmpty(),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .padding(horizontal = 8.dp)
                                                .size(20.dp)
                                        )

                                        // Target Tag
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .background(targetStyle.color.copy(alpha = 0.2f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = targetStyle.icon,
                                                    contentDescription = null,
                                                    tint = targetStyle.color,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Text(
                                                text = targetCategoryName.trim(),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = targetStyle.color,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Will reassign $affectedEntriesCount total entries to '${targetCategoryName.trim()}'.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val isSameCategory = selectedSourceCategory?.name?.trim().equals(targetCategoryName.trim(), ignoreCase = true)
            val canReassign = selectedSourceCategory != null &&
                    targetCategoryName.isNotBlank() &&
                    !isSameCategory &&
                    affectedEntriesCount > 0 &&
                    !isProcessing

            Button(
                onClick = {
                    showConfirmPrompt = true
                },
                enabled = canReassign,
                modifier = Modifier.testTag("apply_bulk_reassign_button")
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reassigning...")
                } else {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (affectedEntriesCount > 0) "Reassign $affectedEntriesCount Entries"
                        else "Reassign Entries"
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isProcessing,
                modifier = Modifier.testTag("cancel_bulk_reassign_button")
            ) {
                Text(if (successMessage != null) "Close" else "Cancel")
            }
        }
    )

    // Confirmation Alert Dialog
    if (showConfirmPrompt) {
        val srcName = selectedSourceCategory?.name.orEmpty()
        val destName = targetCategoryName.trim()

        AlertDialog(
            onDismissRequest = { showConfirmPrompt = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Confirm Bulk Reassignment",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Are you sure you want to reassign all entries currently categorized as \"$srcName\" to \"$destName\"?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• $affectedEntriesCount entries will be modified.\n• All dashboard totals, reports, and charts will immediately reflect the new category.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmPrompt = false
                        isProcessing = true
                        errorMessage = null
                        successMessage = null

                        viewModel.bulkUpdateCategory(
                            oldCategory = srcName,
                            newCategory = destName,
                            includeRecurring = includeRecurring,
                            includeForecast = includeForecast
                        ) { result ->
                            isProcessing = false
                            result.onSuccess { count ->
                                successMessage = "Successfully updated $count entries from \"$srcName\" to \"$destName\"."
                                targetCategoryName = ""
                            }.onFailure { err ->
                                errorMessage = err.message ?: "Failed to reassign categories. Please try again."
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_bulk_reassign_dialog_button")
                ) {
                    Text("Yes, Reassign Entries")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmPrompt = false }
                ) {
                    Text("Go Back")
                }
            }
        )
    }
}
