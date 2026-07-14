package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Transaction
import com.example.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val currentUserEmail by viewModel.currentUserEmail.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val monthlySummary by viewModel.monthlySummary.collectAsState()
    val monthlyBudget by viewModel.monthlyBudget.collectAsState()

    val promptInput by viewModel.promptInput.collectAsState()
    val isParsing by viewModel.isParsing.collectAsState()
    val parseError by viewModel.parseError.collectAsState()
    val parseSuccessMessage by viewModel.parseSuccessMessage.collectAsState()

    var showBudgetDialog by remember { mutableStateOf(false) }
    var showManualAddForm by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    val currentMonthYear = remember {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentMonthYear.uppercase(Locale.getDefault()),
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "Finance.ai",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showBudgetDialog = true },
                        modifier = Modifier.testTag("edit_budget_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Budget Settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    
                    val firstLetter = remember(currentUserEmail) {
                        currentUserEmail?.substringBefore("@")?.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
                    }
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { viewModel.logout() }
                            .testTag("logout_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = firstLetter,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showManualAddForm = !showManualAddForm },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_transaction")
            ) {
                Icon(
                    imageVector = if (showManualAddForm) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = "Manual Transaction Entry"
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {

                // --- 1. AI CHAT INPUT BOX AT THE TOP ---
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Ask Gemini AI to Parse",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Input field Row with soft light background and outline
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                            
                            OutlinedTextField(
                                value = promptInput,
                                onValueChange = { viewModel.updatePromptInput(it) },
                                placeholder = {
                                    Text(
                                        "Spent $15 on coffee today",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Send,
                                    keyboardType = KeyboardType.Text
                                ),
                                keyboardActions = KeyboardActions(
                                    onSend = {
                                        focusManager.clearFocus()
                                        viewModel.parseAndAddTransaction()
                                    }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    disabledBorderColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ai_prompt_input")
                            )

                            IconButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.parseAndAddTransaction()
                                },
                                enabled = promptInput.isNotBlank() && !isParsing,
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .testTag("ai_submit_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Send prompt",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Parse Suggestions Chips
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            val prompt1 = "Spent $12 on lunch"
                            val prompt2 = "Salary of $2500 received"

                            SuggestionChip(
                                onClick = {
                                    viewModel.updatePromptInput(prompt1)
                                },
                                label = { Text(prompt1, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                                shape = RoundedCornerShape(12.dp),
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    labelColor = MaterialTheme.colorScheme.primary
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            )

                            SuggestionChip(
                                onClick = {
                                    viewModel.updatePromptInput(prompt2)
                                },
                                label = { Text(prompt2, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                                shape = RoundedCornerShape(12.dp),
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    labelColor = MaterialTheme.colorScheme.primary
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            )
                        }

                // Progress/State Indicator
                AnimatedVisibility(visible = isParsing) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Gemini is parsing financial input...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Success / Error Feedback
                        AnimatedVisibility(visible = parseSuccessMessage != null || parseError != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                            ) {
                                parseSuccessMessage?.let { success ->
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFFE8F5E9)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF2E7D32),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = success,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF1B5E20),
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Dismiss",
                                                tint = Color(0xFF2E7D32),
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clickable { viewModel.clearParseMessages() }
                                            )
                                        }
                                    }
                                }

                                parseError?.let { err ->
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Error,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = err,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Dismiss",
                                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clickable { viewModel.clearParseMessages() }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // --- 2. THE BUDGET SUMMARY PANEL ---
                val remainingBudget = monthlyBudget + monthlySummary.totalIncome - monthlySummary.totalExpense
                val isBudgetAlert = remainingBudget < 0.1 * monthlyBudget
                val progress = if (monthlyBudget > 0) {
                    (monthlySummary.totalExpense / (monthlyBudget + monthlySummary.totalIncome)).coerceIn(0.0, 1.0).toFloat()
                } else 0f

                // Bento Card 1: Remaining Budget Panel (Span 2)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(32.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        // Background Watermark Wallet Icon (Rotated and decorative)
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            modifier = Modifier
                                .size(130.dp)
                                .align(Alignment.BottomEnd)
                                .offset(x = 20.dp, y = 20.dp)
                                .rotate(12f)
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Monthly Budget",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Text(
                                text = "$${String.format("%,.2f", remainingBudget)}",
                                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 38.sp),
                                fontWeight = FontWeight.Black,
                                color = if (remainingBudget >= 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .padding(vertical = 6.dp)
                                    .testTag("remaining_budget_text")
                            )

                            Text(
                                text = "Remaining of $${String.format("%,.2f", monthlyBudget + monthlySummary.totalIncome)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Budget utilization bar
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = if (isBudgetAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.background.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${(progress * 100).toInt()}% utilized",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Limit: $${String.format("%,.0f", monthlyBudget)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Bento Cards Row: Expenses & Income (Grid of 2 items)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Card 2: Today's Expenses (White, border, rounded-28)
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Icon Container
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        MaterialTheme.colorScheme.background,
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Column {
                                Text(
                                    text = "EXPENSES",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "$${String.format("%,.2f", monthlySummary.totalExpense)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Card 3: Income (Sage Gray Container, rounded-28)
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Icon Container
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Column {
                                Text(
                                    text = "INCOME",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "$${String.format("%,.2f", monthlySummary.totalIncome)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // --- 3. QUICK MANUAL ADD FORM (COLLAPSED BY DEFAULT) ---
                AnimatedVisibility(
                    visible = showManualAddForm,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    ManualAddForm(
                        onSubmit = { amount, category, type, description ->
                            viewModel.addTransaction(amount, category, type, description)
                            showManualAddForm = false
                        }
                    )
                }

                // --- 4. TRANSACTION HISTORY HEADER ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Text(
                        text = "${monthlySummary.currentMonthList.size} items",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }

                // --- 5. TRANSACTION HISTORY LIST ---
                if (monthlySummary.currentMonthList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No Transactions This Month",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Try typing 'Spent $15 on pizza' at the top and let Gemini AI parse it automatically!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("transactions_list"),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(monthlySummary.currentMonthList, key = { it.id }) { tx ->
                            TransactionRowItem(
                                transaction = tx,
                                onDelete = { viewModel.deleteTransaction(tx.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    // --- BUDGET CONFIGURATION DIALOG ---
    if (showBudgetDialog) {
        var budgetText by remember { mutableStateOf(monthlyBudget.toInt().toString()) }

        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = {
                Text(
                    "Configure Monthly Limit",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column {
                    Text(
                        "Set your core monthly expense limit. We use this to calculate remaining balances dynamically.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    OutlinedTextField(
                        value = budgetText,
                        onValueChange = { budgetText = it },
                        label = { Text("Monthly Limit ($)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("monthly_budget_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = budgetText.toDoubleOrNull() ?: 2000.0
                        viewModel.updateMonthlyBudget(parsed)
                        showBudgetDialog = false
                    },
                    modifier = Modifier.testTag("save_budget_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ManualAddForm(
    onSubmit: (Double, String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Food") }
    var type by remember { mutableStateOf("EXPENSE") }
    var description by remember { mutableStateOf("") }

    val categories = listOf("Food", "Transport", "Utilities", "Entertainment", "Shopping", "Salary", "Investment", "Housing", "Others")

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Manual Ledger Entry",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Select Expense or Income
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = type == "EXPENSE",
                    onClick = { type = "EXPENSE" },
                    label = { Text("Expense", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = type == "INCOME",
                    onClick = { type = "INCOME" },
                    label = { Text("Income", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Description input
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("manual_desc_input")
            )

            // Amount & Category Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount ($)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("manual_amount_input")
                )

                // Simple Category Select Box (Uses a dropdown menu)
                var expanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1.2f)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        label = { Text("Category") },
                        readOnly = true,
                        shape = RoundedCornerShape(8.dp),
                        trailingIcon = {
                            IconButton(onClick = { expanded = !expanded }) {
                                Icon(Icons.Default.ArrowDropDown, "Show Categories")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = !expanded }
                            .testTag("manual_category_trigger")
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0 && description.isNotBlank()) {
                        onSubmit(amount, category, type, description)
                        amountText = ""
                        description = ""
                        type = "EXPENSE"
                        category = "Food"
                    }
                },
                enabled = amountText.isNotBlank() && description.isNotBlank(),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("manual_submit_button")
            ) {
                Text("Post Entry")
            }
        }
    }
}

@Composable
fun TransactionRowItem(
    transaction: Transaction,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryStyle = getCategoryStyle(transaction.category)
    val formatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val isExpense = transaction.type == "EXPENSE"

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("transaction_item_${transaction.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Visual Icon Box
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        categoryStyle.color.copy(alpha = 0.15f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryStyle.icon,
                    contentDescription = transaction.category,
                    tint = categoryStyle.color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text Metadata Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = transaction.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = categoryStyle.color,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formatter.format(Date(transaction.date)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Value Amount Text
            val valSign = if (isExpense) "-" else "+"
            val valColor = if (isExpense) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$valSign$${String.format("%.2f", transaction.amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = valColor
                )
                
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("delete_transaction_${transaction.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete entry",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

data class CategoryStyle(
    val icon: ImageVector,
    val color: Color
)

fun getCategoryStyle(category: String): CategoryStyle {
    return when (category.lowercase(Locale.getDefault())) {
        "food" -> CategoryStyle(Icons.Default.Restaurant, Color(0xFFE65100))
        "transport" -> CategoryStyle(Icons.Default.DirectionsCar, Color(0xFF1565C0))
        "utilities" -> CategoryStyle(Icons.Default.Lightbulb, Color(0xFFF57F17))
        "entertainment" -> CategoryStyle(Icons.Default.ConfirmationNumber, Color(0xFF6A1B9A))
        "shopping" -> CategoryStyle(Icons.Default.ShoppingBag, Color(0xFFC2185B))
        "salary" -> CategoryStyle(Icons.Default.AttachMoney, Color(0xFF2E7D32))
        "investment" -> CategoryStyle(Icons.Default.ShowChart, Color(0xFF00695C))
        "housing" -> CategoryStyle(Icons.Default.Home, Color(0xFF0277BD))
        else -> CategoryStyle(Icons.Default.Category, Color(0xFF37474F))
    }
}
