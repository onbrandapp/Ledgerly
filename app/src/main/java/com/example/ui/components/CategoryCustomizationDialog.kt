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
import com.example.data.CustomCategory
import com.example.ui.theme.CategoryConstants
import com.example.ui.viewmodel.ExpenseViewModel

@Composable
fun CategoryCustomizationDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    val customCategoriesList by viewModel.customCategories.collectAsState()

    var newCategoryName by remember { mutableStateOf("") }
    var selectedIconName by remember { mutableStateOf("restaurant") }
    var selectedColorHex by remember { mutableStateOf("#00897B") }
    var selectedType by remember { mutableStateOf("EXPENSE") }
    var customHexInput by remember { mutableStateOf("#00897B") }
    var iconFilterTab by remember { mutableStateOf("ALL") } // ALL, EXPENSE, INCOME
    var editingCatId by remember { mutableStateOf<String?>(null) }
    var showForm by remember { mutableStateOf(false) }

    val resolvedColor = remember(selectedColorHex) {
        CategoryConstants.parseColor(selectedColorHex)
    }

    val filteredIcons = remember(iconFilterTab) {
        when (iconFilterTab) {
            "EXPENSE" -> CategoryConstants.PREDEFINED_ICONS.filter { it.categoryType == "EXPENSE" || it.categoryType == "BOTH" }
            "INCOME" -> CategoryConstants.PREDEFINED_ICONS.filter { it.categoryType == "INCOME" || it.categoryType == "BOTH" }
            else -> CategoryConstants.PREDEFINED_ICONS
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier
            .fillMaxWidth()
            .testTag("category_customization_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
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
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Custom Categories",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Vector icons & custom colors",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                if (!showForm && editingCatId == null) {
                    FilledTonalButton(
                        onClick = {
                            showForm = true
                            newCategoryName = ""
                            selectedIconName = if (selectedType == "INCOME") "attach_money" else "restaurant"
                            selectedColorHex = "#00897B"
                            customHexInput = "#00897B"
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("add_new_category_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Add / Edit Form
                if (showForm || editingCatId != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (editingCatId == null) "New Category" else "Edit Category",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    IconButton(
                                        onClick = {
                                            showForm = false
                                            editingCatId = null
                                            newCategoryName = ""
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Cancel",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                // Category Name Input
                                OutlinedTextField(
                                    value = newCategoryName,
                                    onValueChange = { newCategoryName = it },
                                    label = { Text("Category Name") },
                                    placeholder = { Text("e.g. Subscriptions, Consulting") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("category_input_field")
                                )

                                // Category Type Selection (Expense vs Income)
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Category Type",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilterChip(
                                            selected = selectedType == "EXPENSE",
                                            onClick = {
                                                selectedType = "EXPENSE"
                                                iconFilterTab = "EXPENSE"
                                                if (selectedIconName == "attach_money") selectedIconName = "restaurant"
                                            },
                                            label = { Text("Expense") },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.TrendingDown,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("category_type_expense")
                                        )
                                        FilterChip(
                                            selected = selectedType == "INCOME",
                                            onClick = {
                                                selectedType = "INCOME"
                                                iconFilterTab = "INCOME"
                                                if (selectedIconName == "restaurant") selectedIconName = "attach_money"
                                            },
                                            label = { Text("Income") },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.TrendingUp,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("category_type_income")
                                        )
                                    }
                                }

                                // Live Preview Card
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, resolvedColor.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .background(resolvedColor.copy(alpha = 0.15f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = CategoryConstants.getIconByName(selectedIconName),
                                                contentDescription = selectedIconName,
                                                tint = resolvedColor,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (newCategoryName.isNotBlank()) newCategoryName else "Category Preview",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = selectedType,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = resolvedColor
                                                )
                                                Text(
                                                    text = "•",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = selectedColorHex.uppercase(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }

                                // Vector Icon Selection
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Select Icon",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        // Quick category filter for icons
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            listOf("ALL", "EXPENSE", "INCOME").forEach { tab ->
                                                val isTabSelected = iconFilterTab == tab
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(
                                                            if (isTabSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                            else Color.Transparent
                                                        )
                                                        .clickable { iconFilterTab = tab }
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = tab,
                                                        fontSize = 10.sp,
                                                        fontWeight = if (isTabSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isTabSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Predefined Vector Icons Grid (Chunked into rows of 6 for smooth compact display)
                                    val iconRows = filteredIcons.chunked(6)
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        iconRows.forEach { rowItems ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                rowItems.forEach { item ->
                                                    val isSelected = selectedIconName.equals(item.id, ignoreCase = true)
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .aspectRatio(1f)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(
                                                                if (isSelected) resolvedColor.copy(alpha = 0.2f)
                                                                else MaterialTheme.colorScheme.surface
                                                            )
                                                            .border(
                                                                width = if (isSelected) 2.dp else 1.dp,
                                                                color = if (isSelected) resolvedColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                                                shape = RoundedCornerShape(8.dp)
                                                            )
                                                            .clickable { selectedIconName = item.id }
                                                            .testTag("category_icon_${item.id}"),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = item.icon,
                                                            contentDescription = item.label,
                                                            tint = if (isSelected) resolvedColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                                // Fill empty spaces if last row has fewer than 6 items
                                                if (rowItems.size < 6) {
                                                    repeat(6 - rowItems.size) {
                                                        Spacer(modifier = Modifier.weight(1f))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Color Palette Selection
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Icon Color",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    // Predefined color presets
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(CategoryConstants.COLOR_PRESETS) { hex ->
                                            val color = CategoryConstants.parseColor(hex)
                                            val isSelected = selectedColorHex.equals(hex, ignoreCase = true)

                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                                    .border(
                                                        width = if (isSelected) 3.dp else 1.dp,
                                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                                        shape = CircleShape
                                                    )
                                                    .clickable {
                                                        selectedColorHex = hex
                                                        customHexInput = hex
                                                    }
                                                    .testTag("category_color_$hex"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Custom Hex Input Row
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(resolvedColor)
                                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                        )
                                        OutlinedTextField(
                                            value = customHexInput,
                                            onValueChange = { input ->
                                                customHexInput = input
                                                val clean = if (input.startsWith("#")) input else "#$input"
                                                if (clean.length == 7) {
                                                    try {
                                                        android.graphics.Color.parseColor(clean)
                                                        selectedColorHex = clean
                                                    } catch (_: Exception) {}
                                                }
                                            },
                                            placeholder = { Text("#RRGGBB", fontSize = 11.sp) },
                                            singleLine = true,
                                            shape = RoundedCornerShape(8.dp),
                                            textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                // Submit Button
                                Button(
                                    onClick = {
                                        if (newCategoryName.isNotBlank()) {
                                            val catId = editingCatId
                                            if (catId == null) {
                                                viewModel.addCustomCategory(
                                                    name = newCategoryName.trim(),
                                                    iconName = selectedIconName,
                                                    colorHex = selectedColorHex,
                                                    type = selectedType
                                                )
                                            } else {
                                                viewModel.updateCustomCategory(
                                                    id = catId,
                                                    newName = newCategoryName.trim(),
                                                    iconName = selectedIconName,
                                                    colorHex = selectedColorHex,
                                                    type = selectedType
                                                )
                                                editingCatId = null
                                            }
                                            newCategoryName = ""
                                            showForm = false
                                        }
                                    },
                                    enabled = newCategoryName.isNotBlank(),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("save_category_button")
                                ) {
                                    Icon(
                                        imageVector = if (editingCatId == null) Icons.Default.AddCircleOutline else Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (editingCatId == null) "Add Custom Category" else "Save Changes")
                                }
                            }
                        }
                    }
                }

                // Existing Custom Categories Section
                item {
                    Text(
                        text = "Existing Custom Categories (${customCategoriesList.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (customCategoriesList.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "No custom categories created yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Tap '+ Add' above to create customized categories with vector icons and colors.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(customCategoriesList, key = { it.id }) { cat ->
                        val catColor = remember(cat.colorHex) {
                            CategoryConstants.parseColor(cat.colorHex)
                        }
                        val catIcon = remember(cat.iconName) {
                            CategoryConstants.getIconByName(cat.iconName)
                        }

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_category_item_${cat.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
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
                                            .size(36.dp)
                                            .background(catColor.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = catIcon,
                                            contentDescription = cat.name,
                                            tint = catColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = cat.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = cat.type,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = catColor
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(catColor)
                                            )
                                        }
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            editingCatId = cat.id
                                            newCategoryName = cat.name
                                            selectedIconName = cat.iconName.ifEmpty { "category" }
                                            selectedColorHex = cat.colorHex.ifEmpty { "#00897B" }
                                            selectedType = cat.type.ifEmpty { "EXPENSE" }
                                            customHexInput = selectedColorHex
                                            showForm = true
                                        },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("edit_category_${cat.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Category",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            viewModel.deleteCustomCategory(cat.id)
                                            if (editingCatId == cat.id) {
                                                editingCatId = null
                                                showForm = false
                                                newCategoryName = ""
                                            }
                                        },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("delete_category_${cat.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete Category",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_category_dialog_button")
            ) {
                Text("Done")
            }
        }
    )
}
