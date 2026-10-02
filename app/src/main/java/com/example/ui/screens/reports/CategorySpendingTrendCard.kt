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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.ui.components.HorizontalScrollWithNavArrows
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

    var isCategoryDrawerOpen by remember { mutableStateOf(false) }

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
            // 1. Header: Listed Vertically
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                    Text(
                        text = "Category Spending Trend",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Month-over-month trajectory for $selectedCategory",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Category Selector Button (Opens bottom drawer)
            Surface(
                onClick = { isCategoryDrawerOpen = true },
                shape = RoundedCornerShape(14.dp),
                color = themeColor.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, themeColor.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("category_trend_selector_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Icon(
                            imageVector = categoryStyle.icon,
                            contentDescription = null,
                            tint = themeColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Category: $selectedCategory",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = themeColor
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Open Category Drawer",
                        tint = themeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Quick Category Filter Chips with Navigation Arrows on the far right mirroring design
            val quickCategories = remember(allAvailableCategories) { allAvailableCategories.take(8) }
            val chipsScrollState = rememberScrollState()

            HorizontalScrollWithNavArrows(
                scrollState = chipsScrollState,
                testTagPrefix = "category_trend_chips_nav",
                arrowsOnTop = true,
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            tint = themeColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "QUICK CATEGORIES",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
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

            Spacer(modifier = Modifier.height(10.dp))

            // 4. KPI Metrics: Listed Vertically vs Horizontally to avoid wrapping numbers
            val isIncrease = momDifference > 0.0
            val isDecrease = momDifference < 0.0
            val momColor = when {
                isIncrease -> MaterialTheme.colorScheme.error // More expense
                isDecrease -> Color(0xFF10B981) // Less expense = savings
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Vertical Item 1: Current Month Spend with MoM status badge
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Current Month Spend",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "$${String.format(Locale.US, "%,.2f", currentMonthSpend)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            // MoM Status Pill
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = momColor.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, momColor.copy(alpha = 0.25f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isIncrease || isDecrease) {
                                        Icon(
                                            imageVector = if (isIncrease) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                            contentDescription = null,
                                            tint = momColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    val sign = if (isIncrease) "+" else if (isDecrease) "-" else ""
                                    Text(
                                        text = "MoM: $sign${String.format(Locale.US, "%.1f", abs(momPercentage))}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = momColor
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    // Vertical Item 2: Prior Month Spend
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Prior Month Spend",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "$${String.format(Locale.US, "%,.2f", priorMonthSpend)}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    // Vertical Item 3: 6-Month Monthly Average
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "6-Mo Monthly Average",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "$${String.format(Locale.US, "%,.2f", averageMonthlySpend)} / mo",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
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

    // Bottom Drawer for Category Selection
    if (isCategoryDrawerOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var categorySearchQuery by remember { mutableStateOf("") }
        val filteredDrawerCategories = remember(allAvailableCategories, categorySearchQuery) {
            if (categorySearchQuery.isBlank()) {
                allAvailableCategories
            } else {
                allAvailableCategories.filter {
                    it.contains(categorySearchQuery.trim(), ignoreCase = true)
                }
            }
        }

        ModalBottomSheet(
            onDismissRequest = { isCategoryDrawerOpen = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            modifier = Modifier.testTag("category_trend_bottom_drawer")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
            ) {
                // Drawer Title & Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Select Category",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Choose category to view spending trend",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { isCategoryDrawerOpen = false },
                        modifier = Modifier.testTag("category_drawer_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Bar to quickly find categories
                OutlinedTextField(
                    value = categorySearchQuery,
                    onValueChange = { categorySearchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search categories...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (categorySearchQuery.isNotEmpty()) {
                            IconButton(onClick = { categorySearchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_drawer_search_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Category Items List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (filteredDrawerCategories.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No categories found matching \"$categorySearchQuery\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(filteredDrawerCategories) { cat ->
                            val isSelected = cat.equals(selectedCategory, ignoreCase = true)
                            val catStyleItem = remember(cat, customCategories) {
                                CategoryConstants.resolveCategoryStyle(cat, customCategories)
                            }

                            Surface(
                                onClick = {
                                    selectedCategory = cat
                                    isCategoryDrawerOpen = false
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) {
                                    catStyleItem.color.copy(alpha = 0.14f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                },
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) {
                                        catStyleItem.color.copy(alpha = 0.6f)
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("category_drawer_item_$cat")
                                    .testTag("category_option_$cat")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .background(
                                                    catStyleItem.color.copy(alpha = 0.18f),
                                                    RoundedCornerShape(10.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = catStyleItem.icon,
                                                contentDescription = null,
                                                tint = catStyleItem.color,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Text(
                                            text = cat,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) catStyleItem.color else MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .background(catStyleItem.color, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
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
}
