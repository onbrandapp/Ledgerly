package com.example.ui.screens.reports

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.ui.theme.CategoryStyle
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ChartViewType {
    GROUPED,
    STACKED
}

data class MonthTrend(
    val label: String,
    val expenseAmount: Double,
    val incomeAmount: Double = 0.0,
    val month: Int,
    val year: Int
) {
    val amount: Double get() = expenseAmount
}

private fun resolveCategoryStyle(
    category: String,
    customCategories: List<CustomCategory> = emptyList()
): CategoryStyle {
    return CategoryConstants.resolveCategoryStyle(category, customCategories)
}

@Composable
fun AnalyticsReportView(
    transactions: List<Transaction>,
    customCategories: List<CustomCategory> = emptyList(),
    monthlyBudget: Double = 0.0,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // The Full Visual Analytics Section (Pie Chart + 6-Month Bar Chart)
        VisualAnalyticsSection(
            transactions = transactions,
            customCategories = customCategories
        )
    }
}

@Composable
fun VisualAnalyticsSection(
    transactions: List<Transaction>,
    customCategories: List<CustomCategory> = emptyList(),
    modifier: Modifier = Modifier
) {
    // Current Month Spending Distribution (Pie Chart)
    val currentCal = Calendar.getInstance()
    val curMonth = currentCal.get(Calendar.MONTH)
    val curYear = currentCal.get(Calendar.YEAR)

    val currentMonthExpenses = remember(transactions) {
        transactions.filter { tx ->
            val txCal = Calendar.getInstance().apply { timeInMillis = tx.date }
            tx.type == "EXPENSE" && txCal.get(Calendar.MONTH) == curMonth && txCal.get(Calendar.YEAR) == curYear
        }
    }

    val spendingByCategory = remember(currentMonthExpenses) {
        currentMonthExpenses.groupBy { it.category }.mapValues { entry ->
            entry.value.sumOf { it.amount }
        }
    }

    val totalCurrentMonthSpent = remember(spendingByCategory) {
        spendingByCategory.values.sum()
    }

    var isCategoryShareExpanded by remember { mutableStateOf(false) }

    val sortedCategories = remember(spendingByCategory) {
        spendingByCategory.entries.sortedByDescending { it.value }
    }

    // 6-Month Income & Spending Trends (Grouped Bar Chart)
    val monthlyTrends = remember(transactions) {
        val trends = mutableListOf<MonthTrend>()
        val cal = Calendar.getInstance()

        // Start from 5 months ago to current month (6 months total)
        cal.add(Calendar.MONTH, -5)

        val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())

        for (i in 0 until 6) {
            val targetMonth = cal.get(Calendar.MONTH)
            val targetYear = cal.get(Calendar.YEAR)
            val label = monthFormat.format(cal.time)

            val totalSpent = transactions.filter { tx ->
                val txCal = Calendar.getInstance().apply { timeInMillis = tx.date }
                txCal.get(Calendar.MONTH) == targetMonth &&
                        txCal.get(Calendar.YEAR) == targetYear &&
                        tx.type == "EXPENSE"
            }.sumOf { it.amount }

            val totalEarned = transactions.filter { tx ->
                val txCal = Calendar.getInstance().apply { timeInMillis = tx.date }
                txCal.get(Calendar.MONTH) == targetMonth &&
                        txCal.get(Calendar.YEAR) == targetYear &&
                        tx.type == "INCOME"
            }.sumOf { it.amount }

            trends.add(
                MonthTrend(
                    label = label,
                    expenseAmount = totalSpent,
                    incomeAmount = totalEarned,
                    month = targetMonth,
                    year = targetYear
                )
            )
            cal.add(Calendar.MONTH, 1)
        }
        trends
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("analytics_section_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ShowChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Spending Analytics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // PIE CHART SECTION (CURRENT MONTH SPENDING SHARE)
            Text(
                text = "Category Share (Current Month)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Donut Pie Chart Canvas
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .testTag("pie_chart_canvas"),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        var startAngle = -90f
                        val strokeWidth = 14.dp.toPx()
                        val radius = (this.size.minDimension - strokeWidth) / 2

                        if (totalCurrentMonthSpent == 0.0) {
                            drawCircle(
                                color = Color.LightGray.copy(alpha = 0.3f),
                                radius = radius,
                                style = Stroke(width = strokeWidth)
                            )
                        } else {
                            spendingByCategory.forEach { (cat, amt) ->
                                val angle = (360f * amt / totalCurrentMonthSpent).toFloat()
                                val style = resolveCategoryStyle(cat, customCategories)
                                drawArc(
                                    color = style.color,
                                    startAngle = startAngle,
                                    sweepAngle = angle,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth)
                                )
                                startAngle += angle
                            }
                        }
                    }

                    // Center label
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "TOTAL",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "$${String.format(Locale.US, "%.0f", totalCurrentMonthSpent)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Legend Column
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .animateContentSize()
                ) {
                    if (totalCurrentMonthSpent == 0.0) {
                        Text(
                            text = "No spending recorded this month.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    } else {
                        val categoriesToShow = if (isCategoryShareExpanded) sortedCategories else sortedCategories.take(3)
                        categoriesToShow.forEach { (cat, amt) ->
                            val pct = (amt / totalCurrentMonthSpent * 100).toInt()
                            val style = resolveCategoryStyle(cat, customCategories)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(style.color)
                                )
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$${String.format(Locale.US, "%.0f", amt)} ($pct%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (sortedCategories.size > 3) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { isCategoryShareExpanded = !isCategoryShareExpanded }
                                    .padding(top = 2.dp, bottom = 2.dp)
                            ) {
                                Text(
                                    text = if (isCategoryShareExpanded) "Show Less" else "+ ${sortedCategories.size - 3} more categories (View All)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = if (isCategoryShareExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isCategoryShareExpanded) "Show Less" else "View All Categories",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Divider
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 20.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                thickness = 1.dp
            )

            val maxGroupedAmount = remember(monthlyTrends) {
                monthlyTrends.maxOfOrNull { maxOf(it.expenseAmount, it.incomeAmount) }?.takeIf { it > 0 } ?: 1.0
            }
            val maxStackedAmount = remember(monthlyTrends) {
                monthlyTrends.maxOfOrNull { it.expenseAmount + it.incomeAmount }?.takeIf { it > 0 } ?: 1.0
            }

            var chartViewType by remember { mutableStateOf(ChartViewType.GROUPED) }
            val isStacked = chartViewType == ChartViewType.STACKED
            val stackTransition by animateFloatAsState(
                targetValue = if (isStacked) 1f else 0f,
                animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
                label = "chartStackTransition"
            )

            // BAR CHART SECTION (6-MONTH INCOME VS SPENDING TRENDS)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Monthly Overview: Income vs Spending",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Controls Row: Legend on the left, Grouped / Stacked toggle aligned on the right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Legend
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFF10B981))
                            )
                            Text(
                                text = "Income",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Text(
                                text = "Expenses",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Grouped / Stacked View Mode Toggle
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        modifier = Modifier.testTag("chart_view_toggle")
                    ) {
                        Row(
                            modifier = Modifier.padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isGroupedActive = chartViewType == ChartViewType.GROUPED
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isGroupedActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { chartViewType = ChartViewType.GROUPED }
                                    .testTag("toggle_grouped_view")
                            ) {
                                Text(
                                    text = "Grouped",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isGroupedActive) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isGroupedActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            val isStackedActive = chartViewType == ChartViewType.STACKED
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isStackedActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { chartViewType = ChartViewType.STACKED }
                                    .testTag("toggle_stacked_view")
                            ) {
                                Text(
                                    text = "Stacked",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isStackedActive) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isStackedActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            var hoveredTrend by remember { mutableStateOf<MonthTrend?>(null) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("bar_chart_trend"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    monthlyTrends.forEach { trend ->
                        MonthlyTrendBarItem(
                            trend = trend,
                            isStacked = isStacked,
                            maxGroupedAmount = maxGroupedAmount,
                            maxStackedAmount = maxStackedAmount,
                            stackTransition = stackTransition,
                            isHovered = hoveredTrend == trend,
                            onHoverChange = { hovered ->
                                hoveredTrend = if (hovered) trend else null
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Tooltip overlay
                androidx.compose.animation.AnimatedVisibility(
                    visible = hoveredTrend != null,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { 20 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { 20 }),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    hoveredTrend?.let { trend ->
                        val net = trend.incomeAmount - trend.expenseAmount
                        val netColor = if (net >= 0) Color(0xFF10B981) else MaterialTheme.colorScheme.error

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "${trend.label} ${trend.year}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.width(140.dp)) {
                                    Text("Income:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("+$${String.format(Locale.US, "%,.2f", trend.incomeAmount)}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.width(140.dp)) {
                                    Text("Expense:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("-$${String.format(Locale.US, "%,.2f", trend.expenseAmount)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                                if (isStacked) {
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.width(140.dp)) {
                                        Text("Total:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("$${String.format(Locale.US, "%,.2f", trend.incomeAmount + trend.expenseAmount)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.width(140.dp)) {
                                    Text("Net:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    val netSign = if (net >= 0) "+" else "-"
                                    Text("$netSign$${String.format(Locale.US, "%,.2f", kotlin.math.abs(net))}", style = MaterialTheme.typography.labelMedium, color = netColor, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyTrendBarItem(
    trend: MonthTrend,
    isStacked: Boolean,
    maxGroupedAmount: Double,
    maxStackedAmount: Double,
    stackTransition: Float,
    isHovered: Boolean,
    onHoverChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val targetIncomePct = if (isStacked) {
        if (maxStackedAmount > 0) (trend.incomeAmount / maxStackedAmount).toFloat().coerceIn(0f, 1f) else 0f
    } else {
        if (maxGroupedAmount > 0) (trend.incomeAmount / maxGroupedAmount).toFloat().coerceIn(0f, 1f) else 0f
    }

    val targetExpensePct = if (isStacked) {
        if (maxStackedAmount > 0) (trend.expenseAmount / maxStackedAmount).toFloat().coerceIn(0f, 1f) else 0f
    } else {
        if (maxGroupedAmount > 0) (trend.expenseAmount / maxGroupedAmount).toFloat().coerceIn(0f, 1f) else 0f
    }

    val animatedIncomePct by animateFloatAsState(
        targetValue = targetIncomePct,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "incomePct_${trend.month}_${trend.year}"
    )

    val animatedExpensePct by animateFloatAsState(
        targetValue = targetExpensePct,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "expensePct_${trend.month}_${trend.year}"
    )

    val primaryColor = MaterialTheme.colorScheme.primary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .pointerInput(trend) {
                detectTapGestures(
                    onPress = {
                        onHoverChange(true)
                        tryAwaitRelease()
                        onHoverChange(false)
                    },
                    onTap = {
                        onHoverChange(!isHovered)
                    }
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onHoverChange(!isHovered)
            }
    ) {
        val totalVolume = (trend.incomeAmount + trend.expenseAmount).toInt()
        val topLabelText = if (isStacked) {
            if (totalVolume > 0) "$$totalVolume" else "$0"
        } else {
            if (trend.expenseAmount > 0) "$${trend.expenseAmount.toInt()}" else "$0"
        }

        Text(
            text = topLabelText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val w = size.width
                val h = size.height

                val barWidthGrouped = 7.dp.toPx()
                val barWidthStacked = 14.dp.toPx()
                val gap = 3.dp.toPx()
                val currentBarWidth = barWidthGrouped + (barWidthStacked - barWidthGrouped) * stackTransition
                val cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())

                val centerX = w / 2f

                // Income Bar geometry
                val groupedIncomeCenterX = centerX - (barWidthGrouped + gap) / 2f
                val incomeCenterX = groupedIncomeCenterX + (centerX - groupedIncomeCenterX) * stackTransition
                val incomeHeight = if (trend.incomeAmount > 0) {
                    maxOf(h * animatedIncomePct, 4.dp.toPx())
                } else {
                    0f
                }
                val incomeLeft = incomeCenterX - currentBarWidth / 2f
                val incomeTop = h - incomeHeight

                // Expense Bar geometry
                val groupedExpenseCenterX = centerX + (barWidthGrouped + gap) / 2f
                val expenseCenterX = groupedExpenseCenterX + (centerX - groupedExpenseCenterX) * stackTransition
                val expenseHeight = if (trend.expenseAmount > 0) {
                    maxOf(h * animatedExpensePct, 4.dp.toPx())
                } else {
                    0f
                }
                val expenseLeft = expenseCenterX - currentBarWidth / 2f

                // Baseline for expense
                val gapStacked = 1.5.dp.toPx() * stackTransition
                val stackedBaseY = if (incomeHeight > 0f) (h - incomeHeight - gapStacked) else h
                val expenseBaseY = h + (stackedBaseY - h) * stackTransition
                val expenseTop = (expenseBaseY - expenseHeight).coerceAtLeast(0f)

                // Draw Income Bar
                if (incomeHeight > 0f) {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF34D399), Color(0xFF059669)),
                            startY = incomeTop,
                            endY = h
                        ),
                        topLeft = Offset(incomeLeft, incomeTop),
                        size = Size(currentBarWidth, incomeHeight),
                        cornerRadius = cornerRadius
                    )
                }

                // Draw Expense Bar
                if (expenseHeight > 0f) {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                primaryColor,
                                primaryColor.copy(alpha = 0.65f)
                            ),
                            startY = expenseTop,
                            endY = expenseBaseY
                        ),
                        topLeft = Offset(expenseLeft, expenseTop),
                        size = Size(currentBarWidth, (expenseBaseY - expenseTop).coerceAtLeast(0f)),
                        cornerRadius = cornerRadius
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = trend.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isHovered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
