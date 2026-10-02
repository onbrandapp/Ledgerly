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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CategoryConstants
import com.example.ui.viewmodel.ExpenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CostProfitCategoriesDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    val costProfitCategories by viewModel.costProfitCategories.collectAsState()
    val customCategoriesList by viewModel.customCategories.collectAsState()
    val categoryUsageList by viewModel.categoryUsageList.collectAsState()

    val defaultCategories = remember {
        listOf("Product", "Food", "Transport", "Utilities", "Entertainment", "Shopping", "Salary", "Investment", "Housing", "Others")
    }

    var searchQuery by remember { mutableStateOf("") }

    // Combined unique list of all categories across defaults, custom, and entries
    val allUniqueCategories = remember(customCategoriesList, categoryUsageList, defaultCategories) {
        val combined = (defaultCategories + customCategoriesList.map { it.name } + categoryUsageList.map { it.name })
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .sortedWith { a, b ->
                // Sort "Product" first, then alphabetically
                if (a.equals("Product", ignoreCase = true)) -1
                else if (b.equals("Product", ignoreCase = true)) 1
                else a.compareTo(b, ignoreCase = true)
            }
        combined
    }

    val filteredCategories = remember(allUniqueCategories, searchQuery) {
        if (searchQuery.isBlank()) {
            allUniqueCategories
        } else {
            allUniqueCategories.filter { it.contains(searchQuery.trim(), ignoreCase = true) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier
            .fillMaxWidth()
            .testTag("cost_profit_categories_dialog"),
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
                        imageVector = Icons.Default.Calculate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Cost & Profit Categories",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Show Cost & Profit % fields for chosen categories",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Info & Count Badge Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Active in Drawer:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${costProfitCategories.size} selected",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Quick Action Chips Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssistChip(
                        onClick = {
                            viewModel.setCostProfitCategories(allUniqueCategories.toSet())
                        },
                        label = { Text("Select All", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f).testTag("select_all_cost_profit_button")
                    )
                    AssistChip(
                        onClick = {
                            viewModel.resetCostProfitCategoriesToDefault()
                        },
                        label = { Text("Default (Product)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1.3f).testTag("reset_default_cost_profit_button")
                    )
                    AssistChip(
                        onClick = {
                            viewModel.setCostProfitCategories(emptySet())
                        },
                        label = { Text("Clear All", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f).testTag("clear_all_cost_profit_button")
                    )
                }

                // Search Filter TextField
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search categories...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_cost_profit_categories_input")
                )

                // Categories Checklist
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredCategories, key = { it }) { catName ->
                        val isChecked = costProfitCategories.any { it.equals(catName, ignoreCase = true) }
                        val style = CategoryConstants.resolveCategoryStyle(catName, customCategoriesList)
                        val usage = categoryUsageList.find { it.name.equals(catName, ignoreCase = true) }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                            else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                width = if (isChecked) 1.5.dp else 1.dp,
                                color = if (isChecked) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleCostProfitCategory(catName) }
                                .testTag("cost_profit_row_$catName")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(style.color.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = style.icon,
                                            contentDescription = null,
                                            tint = style.color,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = catName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (usage != null && usage.totalCount > 0) {
                                            Text(
                                                text = "${usage.totalCount} entries in ledger",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }

                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { viewModel.toggleCostProfitCategory(catName) },
                                    modifier = Modifier.testTag("cost_profit_checkbox_$catName")
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("done_cost_profit_dialog_button")
            ) {
                Text("Done")
            }
        }
    )
}
