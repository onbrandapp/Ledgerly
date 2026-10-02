package com.example.ui.screens.reports

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CustomCategory
import com.example.data.Transaction
import com.example.ui.theme.CategoryConstants
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

data class CategoryMonthPoint(
    val monthIndex: Int,
    val year: Int,
    val label: String,
    val amount: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySpendingTrendCard(
    transactions: List<Transaction>,
    customCategories: List<CustomCategory> = emptyList(),
    modifier: Modifier = Modifier
) {
    // 1. Gather all unique expense categories with spending activity or custom definitions
    val usedExpenseCategories = remember(transactions) {
        transactions
            .filter { it.type.uppercase() == "EXPENSE" && it.category.isNotBlank() }
            .map { it.category.trim() }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }
    }

    val allAvailableCategories = remember(usedExpenseCategories, customCategories) {
        val list = mutableListOf<String>()
        list.addAll(usedExpenseCategories)
        customCategories.forEach { cc ->
            if (!list.any { it.equals(cc.name, ignoreCase = true) }) {
                list.add(cc.name)
            }
        }
        val defaultExpensePresets = CategoryConstants.PREDEFINED_ICONS
            .filter { it.categoryType == "EXPENSE" || it.categoryType == "BOTH" }
            .map { it.label }
        defaultExpensePresets.forEach { label ->
            if (!list.any { it.equals(label, ignoreCase = true) }) {
                list.add(label)
            }
        }
        if (list.isEmpty()) {
            listOf("Food & Dining", "Transportation", "Shopping", "Housing & Rent", "Utilities & Power")
        } else {
            list
        }
    }

    // Default chosen category: top spent category, or first available
    var selectedCategory by remember(allAvailableCategories) {
        mutableStateOf(allAvailableCategories.firstOrNull() ?: "Food & Dining")
    }

    val categoryStyle = remember(selectedCategory, customCategories) {
        CategoryConstants.resolveCategoryStyle(selectedCategory, customCategories)
    }
    val themeColor = categoryStyle.color

    // 2. Compute 6-Month historical data points for the chosen category
    val monthPoints = remember(transactions, selectedCategory) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -5)
        val sdf = SimpleDateFormat("MMM", Locale.getDefault())
        val points = mutableListOf<CategoryMonthPoint>()

        for (i in 0 until 6) {
            val targetMonth = cal.get(Calendar.MONTH)
            val targetYear = cal.get(Calendar.YEAR)
            val label = sdf.format(cal.time)

            val total = transactions.filter { tx ->
                if (tx.type.uppercase() != "EXPENSE") return@filter false
                if (!tx.category.trim().equals(selectedCategory.trim(), ignoreCase = true)) return@filter false
                val txCal = Calendar.getInstance().apply { timeInMillis = tx.date }
                txCal.get(Calendar.MONTH) == targetMonth && txCal.get(Calendar.YEAR) == targetYear
            }.sumOf { it.amount }

            points.add(CategoryMonthPoint(targetMonth, targetYear, label, total))
            cal.add(Calendar.MONTH, 1)
        }
        points
    }

    val totalSpending = remember(monthPoints) { monthPoints.sumOf { it.amount } }
    val averageMonthlySpend = remember(monthPoints) { if (monthPoints.isNotEmpty()) totalSpending / monthPoints.size else 0.0 }
    val currentMonthSpend = monthPoints.lastOrNull()?.amount ?: 0.0
    val priorMonthSpend = if (monthPoints.size >= 2) monthPoints[monthPoints.size - 2].amount else 0.0

    // MoM change computation
    val momDifference = currentMonthSpend - priorMonthSpend
    val momPercentage = remember(currentMonthSpend, priorMonthSpend) {
        when {
            priorMonthSpend > 0.0 -> ((currentMonthSpend - priorMonthSpend) / priorMonthSpend) * 100.0
            currentMonthSpend > 0.0 -> 100.0
            else -> 0.0
        }
    }

    val peakMonth = remember(monthPoints) {
        monthPoints.maxByOrNull { it.amount }
    }

    var selectedPointIndex by remember(selectedCategory) {
        mutableStateOf(monthPoints.lastIndex.coerceAtLeast(0))
    }

    var isCategoryDropdownOpen by remember { mutableStateOf(false) }

    // Animation progress when switching categories
    val animProgress = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(selectedCategory) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing))
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("category_spending_trend_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header: Icon, Title & Category Selector Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(themeColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = categoryStyle.icon,
                            contentDescription = null,
                            tint = themeColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Category Spending Trend",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Month-over-month trajectory",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Category Selector Dropdown Button
                Box {
                    Surface(
                        onClick = { isCategoryDropdownOpen = true },
                        shape = RoundedCornerShape(12.dp),
                        color = themeColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, themeColor.copy(alpha = 0.35f)),
                        modifier = Modifier.testTag("category_trend_selector_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = selectedCategory,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = themeColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Change Category",
                                tint = themeColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = isCategoryDropdownOpen,
                        onDismissRequest = { isCategoryDropdownOpen = false },
                        modifier = Modifier
                            .widthIn(min = 180.dp, max = 260.dp)
                            .testTag("category_trend_dropdown")
                    ) {
                        allAvailableCategories.forEach { cat ->
                            val catStyleItem = CategoryConstants.resolveCategoryStyle(cat, customCategories)
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .background(catStyleItem.color.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = catStyleItem.icon,
                                                contentDescription = null,
                                                tint = catStyleItem.color,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                        Text(
                                            text = cat,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (cat == selectedCategory) FontWeight.Bold else FontWeight.Normal,
                                            color = if (cat == selectedCategory) themeColor else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                },
                                onClick = {
                                    selectedCategory = cat
                                    isCategoryDropdownOpen = false
                                },
                                modifier = Modifier.testTag("category_option_$cat")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Category Filter Chips Row
            val quickCategories = remember(allAvailableCategories) { allAvailableCategories.take(6) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickCategories.forEach { cat ->
                    val isSelected = cat.equals(selectedCategory, ignoreCase = true)
                    val chipStyle = CategoryConstants.resolveCategoryStyle(cat, customCategories)
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = {
                            Text(
                                text = cat,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = chipStyle.icon,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = chipStyle.color.copy(alpha = 0.18f),
                            selectedLabelColor = chipStyle.color,
                            selectedLeadingIconColor = chipStyle.color
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            selectedBorderColor = chipStyle.color.copy(alpha = 0.5f),
                            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("quick_chip_$cat")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3 KPI Metric Cards (Current Month, MoM Change, 6-Mo Average)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // KPI 1: Current Month
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Current Month",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$${String.format(Locale.US, "%,.2f", currentMonthSpend)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // KPI 2: MoM Change
                val isIncrease = momDifference > 0.0
                val isDecrease = momDifference < 0.0
                val momColor = when {
                    isIncrease -> MaterialTheme.colorScheme.error // More expense
                    isDecrease -> Color(0xFF10B981) // Less expense = savings
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "MoM Change",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isIncrease || isDecrease) {
                                Icon(
                                    imageVector = if (isIncrease) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                    contentDescription = null,
                                    tint = momColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                            }
                            val sign = if (isIncrease) "+" else if (isDecrease) "-" else ""
                            Text(
                                text = "$sign${String.format(Locale.US, "%.1f", abs(momPercentage))}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = momColor
                            )
                        }
                    }
                }

                // KPI 3: 6-Mo Average
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "6-Mo Average",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$${String.format(Locale.US, "%,.2f", averageMonthlySpend)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Line Chart & Interactive Point Inspection
            if (totalSpending <= 0.0) {
                // Empty state for category with no expenses
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No spending recorded for \"$selectedCategory\"",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Select another category above to view spending trends",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Interactive selected node inspection pill
                val activePoint = monthPoints.getOrNull(selectedPointIndex)
                if (activePoint != null) {
                    val prevIndex = selectedPointIndex - 1
                    val pointPrevAmount = if (prevIndex >= 0) monthPoints[prevIndex].amount else null
                    val pointMomPct = pointPrevAmount?.let { p ->
                        if (p > 0.0) ((activePoint.amount - p) / p) * 100.0 else if (activePoint.amount > 0.0) 100.0 else 0.0
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Viewing: ${activePoint.label} ${activePoint.year}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = themeColor.copy(alpha = 0.14f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "$${String.format(Locale.US, "%,.2f", activePoint.amount)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = themeColor
                                )
                                if (pointMomPct != null) {
                                    val isPctUp = pointMomPct > 0.0
                                    val isPctDown = pointMomPct < 0.0
                                    val color = if (isPctUp) MaterialTheme.colorScheme.error else if (isPctDown) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                                    val prefix = if (isPctUp) "+" else if (isPctDown) "-" else ""
                                    Text(
                                        text = "($prefix${String.format(Locale.US, "%.1f", abs(pointMomPct))}%)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = color,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }

                val maxGraphAmount = remember(monthPoints) {
                    val maxVal = monthPoints.maxOfOrNull { it.amount } ?: 100.0
                    if (maxVal > 0.0) maxVal * 1.15 else 100.0
                }

                // Smooth Bezier Curve Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(monthPoints) {
                                detectTapGestures { offset ->
                                    val availableWidth = size.width - 50.dp.toPx()
                                    val stepX = if (monthPoints.size > 1) availableWidth / (monthPoints.size - 1) else 0f
                                    val paddingLeft = 36.dp.toPx()

                                    // Find closest point index
                                    var closestIdx = 0
                                    var minDistance = Float.MAX_VALUE
                                    monthPoints.indices.forEach { i ->
                                        val nodeX = paddingLeft + i * stepX
                                        val dist = abs(offset.x - nodeX)
                                        if (dist < minDistance) {
                                            minDistance = dist
                                            closestIdx = i
                                        }
                                    }
                                    selectedPointIndex = closestIdx
                                }
                            }
                            .testTag("trend_line_chart_canvas")
                    ) {
                        val paddingLeft = 36.dp.toPx()
                        val paddingRight = 14.dp.toPx()
                        val paddingTop = 16.dp.toPx()
                        val paddingBottom = 24.dp.toPx()

                        val chartWidth = size.width - paddingLeft - paddingRight
                        val chartHeight = size.height - paddingTop - paddingBottom
                        val bottomY = size.height - paddingBottom
                        val stepX = if (monthPoints.size > 1) chartWidth / (monthPoints.size - 1) else 0f
                        val progress = animProgress.value

                        // 1. Draw subtle horizontal grid lines (0%, 50%, 100%)
                        val gridLevels = listOf(0.0, 0.5, 1.0)
                        gridLevels.forEach { level ->
                            val gridY = bottomY - (chartHeight * level).toFloat()
                            drawLine(
                                color = Color.Gray.copy(alpha = 0.20f),
                                start = Offset(paddingLeft, gridY),
                                end = Offset(size.width - paddingRight, gridY),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // 2. Build Smooth Bezier Path & Gradient Area
                        val linePath = Path()
                        val fillPath = Path()

                        monthPoints.indices.forEach { i ->
                            val x = paddingLeft + i * stepX
                            val ratio = (monthPoints[i].amount / maxGraphAmount).toFloat().coerceIn(0f, 1f) * progress
                            val y = bottomY - ratio * chartHeight

                            if (i == 0) {
                                linePath.moveTo(x, y)
                                fillPath.moveTo(x, y)
                            } else {
                                val prevX = paddingLeft + (i - 1) * stepX
                                val prevRatio = (monthPoints[i - 1].amount / maxGraphAmount).toFloat().coerceIn(0f, 1f) * progress
                                val prevY = bottomY - prevRatio * chartHeight

                                val cX1 = prevX + (x - prevX) / 2f
                                val cY1 = prevY
                                val cX2 = prevX + (x - prevX) / 2f
                                val cY2 = y

                                linePath.cubicTo(cX1, cY1, cX2, cY2, x, y)
                                fillPath.cubicTo(cX1, cY1, cX2, cY2, x, y)
                            }
                        }

                        val lastX = paddingLeft + (monthPoints.size - 1) * stepX
                        fillPath.lineTo(lastX, bottomY)
                        fillPath.lineTo(paddingLeft, bottomY)
                        fillPath.close()

                        // Draw area under curve
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    themeColor.copy(alpha = 0.35f),
                                    themeColor.copy(alpha = 0.01f)
                                ),
                                startY = paddingTop,
                                endY = bottomY
                            )
                        )

                        // Draw the main spline stroke
                        drawPath(
                            path = linePath,
                            color = themeColor,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        // 3. Draw Point Nodes
                        monthPoints.indices.forEach { i ->
                            val x = paddingLeft + i * stepX
                            val ratio = (monthPoints[i].amount / maxGraphAmount).toFloat().coerceIn(0f, 1f) * progress
                            val y = bottomY - ratio * chartHeight
                            val isSelected = (i == selectedPointIndex)

                            if (isSelected) {
                                drawCircle(
                                    color = themeColor.copy(alpha = 0.22f),
                                    radius = 10.dp.toPx(),
                                    center = Offset(x, y)
                                )
                            }
                            drawCircle(
                                color = themeColor,
                                radius = if (isSelected) 5.5.dp.toPx() else 4.dp.toPx(),
                                center = Offset(x, y)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = if (isSelected) 2.5.dp.toPx() else 2.dp.toPx(),
                                center = Offset(x, y)
                            )
                        }
                    }
                }

                // Month labels along the X-axis
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 28.dp, end = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    monthPoints.indices.forEach { i ->
                        val pt = monthPoints[i]
                        val isSelected = (i == selectedPointIndex)
                        Text(
                            text = pt.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                            color = if (isSelected) themeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { selectedPointIndex = i }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Peak Month and Insight footer
            if (peakMonth != null && peakMonth.amount > 0.0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Highest Spending Month",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${peakMonth.label} ${peakMonth.year} ($${String.format(Locale.US, "%,.2f", peakMonth.amount)})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
