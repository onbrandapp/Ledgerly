package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.ExpenseViewModel

data class MetricCardInfo(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val badge: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeOverviewCardsDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    val visibleCards by viewModel.visibleOverviewCards.collectAsState()

    val cardList = remember {
        listOf(
            MetricCardInfo(
                id = ExpenseViewModel.CARD_BUDGET,
                title = "Monthly Budget & Limit",
                subtitle = "Remaining budget, limit utilization bar, and on-track status",
                icon = Icons.Default.AccountBalanceWallet,
                badge = "Core"
            ),
            MetricCardInfo(
                id = ExpenseViewModel.CARD_EXPENSES,
                title = "Monthly Expenses",
                subtitle = "Total expenses recorded in the current calendar month",
                icon = Icons.Default.Payments
            ),
            MetricCardInfo(
                id = ExpenseViewModel.CARD_INCOME,
                title = "Monthly Income",
                subtitle = "Total money earned and received this month",
                icon = Icons.AutoMirrored.Filled.TrendingUp
            ),
            MetricCardInfo(
                id = ExpenseViewModel.CARD_NET_SAVINGS,
                title = "Net Savings & Cash Flow",
                subtitle = "Net surplus or deficit (Income minus Expenses)",
                icon = Icons.Default.Savings,
                badge = "New"
            ),
            MetricCardInfo(
                id = ExpenseViewModel.CARD_RECONCILIATION,
                title = "Expense Reconciliation",
                subtitle = "Paid vs unpaid breakdown and settlement progress bar",
                icon = Icons.Default.CheckCircle
            ),
            MetricCardInfo(
                id = ExpenseViewModel.CARD_AI_INPUT,
                title = "AI Smart Prompt Bar",
                subtitle = "Natural language expense parsing box at the top of overview",
                icon = Icons.Default.AutoAwesome
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier
            .fillMaxWidth()
            .testTag("customize_overview_cards_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DashboardCustomize,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Customize Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${visibleCards.size} of ${cardList.size} cards visible",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Choose which metric cards to show on your Monthly Overview to focus on the numbers that matter most to you.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(cardList, key = { it.id }) { item ->
                        val isChecked = visibleCards.contains(item.id)
                        val isOnlyOneLeft = isChecked && visibleCards.size == 1

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isChecked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(
                                1.dp,
                                if (isChecked) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!isOnlyOneLeft) {
                                        viewModel.toggleOverviewCard(item.id)
                                    }
                                }
                                .testTag("card_toggle_row_${item.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isChecked) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isChecked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                        if (item.badge.isNotEmpty()) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                            ) {
                                                Text(
                                                    text = item.badge,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontSize = 9.sp,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = item.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }

                                Switch(
                                    checked = isChecked,
                                    enabled = !isOnlyOneLeft,
                                    onCheckedChange = {
                                        viewModel.setOverviewCardVisibility(item.id, it)
                                    },
                                    modifier = Modifier.testTag("switch_${item.id}")
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
                modifier = Modifier.testTag("done_customize_overview_dialog")
            ) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { viewModel.resetOverviewCardsToDefault() },
                modifier = Modifier.testTag("reset_overview_cards_button")
            ) {
                Text("Reset All")
            }
        }
    )
}
