package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import android.app.DatePickerDialog
import java.util.Calendar
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
    val recurringTransactions by viewModel.recurringTransactions.collectAsState()

    val primaryColorHex by viewModel.primaryColor.collectAsState()
    val secondaryColorHex by viewModel.secondaryColor.collectAsState()
    val accentColorHex by viewModel.accentColor.collectAsState()

    val promptInput by viewModel.promptInput.collectAsState()
    val isParsing by viewModel.isParsing.collectAsState()
    val parseError by viewModel.parseError.collectAsState()
    val parseSuccessMessage by viewModel.parseSuccessMessage.collectAsState()

    var showBudgetDialog by remember { mutableStateOf(false) }
    var showManualAddForm by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var editingRecurringTransaction by remember { mutableStateOf<com.example.data.RecurringTransaction?>(null) }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Transactions, 1 = Recurring

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
                onClick = {
                    if (!showManualAddForm) {
                        editingTransaction = null
                        editingRecurringTransaction = null
                    }
                    showManualAddForm = !showManualAddForm
                },
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
                    .verticalScroll(rememberScrollState())
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
                        containerColor = MaterialTheme.colorScheme.primary
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
                            tint = Color(0xFF121214).copy(alpha = 0.05f),
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
                                color = Color(0xFF121214).copy(alpha = 0.7f)
                            )

                            Text(
                                text = "$${String.format("%,.2f", remainingBudget)}",
                                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 38.sp),
                                fontWeight = FontWeight.Black,
                                color = if (remainingBudget >= 0) Color(0xFF121214) else Color(0xFFD32F2F),
                                modifier = Modifier
                                    .padding(vertical = 6.dp)
                                    .testTag("remaining_budget_text")
                            )

                            Text(
                                text = "Remaining of $${String.format("%,.2f", monthlyBudget + monthlySummary.totalIncome)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF121214).copy(alpha = 0.8f)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Budget utilization bar
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = if (isBudgetAlert) Color(0xFFD32F2F) else Color(0xFF121214),
                                trackColor = Color(0xFF121214).copy(alpha = 0.15f)
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
                                    color = Color(0xFF121214).copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "Limit: $${String.format("%,.0f", monthlyBudget)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF121214).copy(alpha = 0.8f)
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
                    // Card 2: Today's Expenses (Using Secondary/Purple Background)
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondary
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
                                        Color(0xFF121214).copy(alpha = 0.08f),
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = Color(0xFF121214),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Column {
                                Text(
                                    text = "EXPENSES",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF121214).copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "$${String.format("%,.2f", monthlySummary.totalExpense)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF121214),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Card 3: Income (Using Accent/Lime Green Background)
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
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
                                        Color(0xFF121214).copy(alpha = 0.08f),
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = Color(0xFF121214),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Column {
                                Text(
                                    text = "INCOME",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF121214).copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "$${String.format("%,.2f", monthlySummary.totalIncome)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF121214),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }



                // --- 4. TRANSACTION / RECURRING SWITCH HEADER ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            label = { Text("Recent Ledger", fontWeight = FontWeight.ExtraBold) },
                            modifier = Modifier.testTag("tab_transactions")
                        )
                        FilterChip(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            label = { Text("Recurring Rules", fontWeight = FontWeight.ExtraBold) },
                            modifier = Modifier.testTag("tab_recurring")
                        )
                    }

                    Text(
                        text = if (selectedTab == 0) "${monthlySummary.currentMonthList.size} items" else "${recurringTransactions.size} rules",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }

                // --- 5. CONDITIONALLY RENDER SELECTED TAB LIST ---
                if (selectedTab == 0) {
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
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 80.dp)
                                .testTag("transactions_list"),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            monthlySummary.currentMonthList.forEach { tx ->
                                TransactionRowItem(
                                    transaction = tx,
                                    onEdit = {
                                        editingTransaction = tx
                                        editingRecurringTransaction = null
                                        showManualAddForm = true
                                    },
                                    onDelete = { viewModel.deleteTransaction(tx.id) }
                                )
                            }
                        }
                    }
                } else {
                    if (recurringTransactions.isEmpty()) {
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
                                    imageVector = Icons.Default.Autorenew,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    modifier = Modifier.size(72.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No Recurring Schedules",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Tap the '+' button below, toggle 'Repeat transaction', and set up a rule that logs automatically!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 80.dp)
                                .testTag("recurring_list"),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            recurringTransactions.forEach { rec ->
                                RecurringRowItem(
                                    recurring = rec,
                                    onEdit = {
                                        editingRecurringTransaction = rec
                                        editingTransaction = null
                                        showManualAddForm = true
                                    },
                                    onDelete = { viewModel.deleteRecurringTransaction(rec.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- SETTINGS & BUDGET CONFIGURATION BOTTOM DRAWER ---
    if (showBudgetDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showBudgetDialog = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            modifier = Modifier.testTag("budget_bottom_sheet")
        ) {
            var budgetText by remember { mutableStateOf(monthlyBudget.toInt().toString()) }
            var tempPrimaryHex by remember { mutableStateOf(primaryColorHex) }
            var tempSecondaryHex by remember { mutableStateOf(secondaryColorHex) }
            var tempAccentHex by remember { mutableStateOf(accentColorHex) }

            val primaryPresets = listOf("#FFD97D", "#FF8A80", "#80D8FF", "#FF80DF", "#FFD54F")
            val secondaryPresets = listOf("#A78BFA", "#BA68C8", "#82B1FF", "#FF8A80", "#B2DFDB")
            val accentPresets = listOf("#D9F99D", "#A7F3D0", "#FFE082", "#80DEEA", "#E6C2FF")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Settings & Customization",
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Monthly Budget Limit",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = budgetText,
                        onValueChange = { budgetText = it },
                        label = { Text("Monthly Limit ($)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("monthly_budget_input")
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    ColorPresetRow(
                        selectedColorHex = tempPrimaryHex,
                        presets = primaryPresets,
                        onColorSelected = { hex ->
                            tempPrimaryHex = hex
                            viewModel.updatePrimaryColor(hex)
                        },
                        customHexValue = tempPrimaryHex,
                        onCustomHexChanged = { hex ->
                            tempPrimaryHex = hex
                        },
                        label = "Primary Color (Budget Card)"
                    )

                    ColorPresetRow(
                        selectedColorHex = tempSecondaryHex,
                        presets = secondaryPresets,
                        onColorSelected = { hex ->
                            tempSecondaryHex = hex
                            viewModel.updateSecondaryColor(hex)
                        },
                        customHexValue = tempSecondaryHex,
                        onCustomHexChanged = { hex ->
                            tempSecondaryHex = hex
                        },
                        label = "Secondary Color (Expenses Card)"
                    )

                    ColorPresetRow(
                        selectedColorHex = tempAccentHex,
                        presets = accentPresets,
                        onColorSelected = { hex ->
                            tempAccentHex = hex
                            viewModel.updateAccentColor(hex)
                        },
                        customHexValue = tempAccentHex,
                        onCustomHexChanged = { hex ->
                            tempAccentHex = hex
                        },
                        label = "Accent Color (Income Card)"
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            viewModel.resetThemeToDefault()
                            tempPrimaryHex = "#FFD97D"
                            tempSecondaryHex = "#A78BFA"
                            tempAccentHex = "#D9F99D"
                        }
                    ) {
                        Text("Reset Theme", color = MaterialTheme.colorScheme.error)
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showBudgetDialog = false }) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                val parsed = budgetText.toDoubleOrNull() ?: 2000.0
                                viewModel.updateMonthlyBudget(parsed)
                                showBudgetDialog = false
                            },
                            modifier = Modifier.testTag("save_budget_button")
                        ) {
                            Text("Done")
                        }
                    }
                }
            }
        }
    }

    // --- MANUAL ENTRY BOTTOM DRAWER ---
    if (showManualAddForm) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = {
                showManualAddForm = false
                editingTransaction = null
                editingRecurringTransaction = null
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            modifier = Modifier.testTag("manual_add_bottom_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                ManualAddForm(
                    initialTransaction = editingTransaction,
                    initialRecurringTransaction = editingRecurringTransaction,
                    onSubmit = { id, amount, category, type, description, isRecurring, frequency, selectedDate ->
                        if (editingTransaction != null) {
                            if (isRecurring) {
                                // Toggled from non-recurring to recurring
                                viewModel.addRecurringTransaction(amount, category, type, description, frequency, selectedDate)
                                viewModel.deleteTransaction(editingTransaction!!.id)
                            } else {
                                viewModel.addTransaction(amount, category, type, description, selectedDate, id)
                            }
                        } else if (editingRecurringTransaction != null) {
                            if (!isRecurring) {
                                // Toggled from recurring to non-recurring
                                viewModel.addTransaction(amount, category, type, description, selectedDate)
                                viewModel.deleteRecurringTransaction(editingRecurringTransaction!!.id)
                            } else {
                                viewModel.addRecurringTransaction(
                                    amount = amount,
                                    category = category,
                                    type = type,
                                    description = description,
                                    frequency = frequency,
                                    startDate = selectedDate,
                                    id = id,
                                    lastLoggedDate = editingRecurringTransaction!!.lastLoggedDate
                                )
                            }
                        } else {
                            // New entry
                            if (isRecurring) {
                                viewModel.addRecurringTransaction(amount, category, type, description, frequency, selectedDate)
                            } else {
                                viewModel.addTransaction(amount, category, type, description, selectedDate)
                            }
                        }
                        showManualAddForm = false
                        editingTransaction = null
                        editingRecurringTransaction = null
                    }
                )
            }
        }
    }
}

@Composable
fun ManualAddForm(
    onSubmit: (id: String, Double, String, String, String, Boolean, String, Long) -> Unit,
    initialTransaction: Transaction? = null,
    initialRecurringTransaction: com.example.data.RecurringTransaction? = null,
    modifier: Modifier = Modifier
) {
    var amountText by remember(initialTransaction, initialRecurringTransaction) {
        mutableStateOf(
            if (initialTransaction != null) {
                if (initialTransaction.amount == 0.0) "" else initialTransaction.amount.toString()
            } else if (initialRecurringTransaction != null) {
                if (initialRecurringTransaction.amount == 0.0) "" else initialRecurringTransaction.amount.toString()
            } else {
                ""
            }
        )
    }
    var category by remember(initialTransaction, initialRecurringTransaction) {
        mutableStateOf(initialTransaction?.category ?: initialRecurringTransaction?.category ?: "Food")
    }
    var type by remember(initialTransaction, initialRecurringTransaction) {
        mutableStateOf(initialTransaction?.type ?: initialRecurringTransaction?.type ?: "EXPENSE")
    }
    var description by remember(initialTransaction, initialRecurringTransaction) {
        mutableStateOf(initialTransaction?.description ?: initialRecurringTransaction?.description ?: "")
    }
    var isRecurring by remember(initialTransaction, initialRecurringTransaction) {
        mutableStateOf(initialRecurringTransaction != null)
    }
    var frequency by remember(initialTransaction, initialRecurringTransaction) {
        mutableStateOf(initialRecurringTransaction?.frequency ?: "MONTHLY")
    }

    val context = LocalContext.current
    val calendar = remember(initialTransaction, initialRecurringTransaction) {
        Calendar.getInstance().apply {
            timeInMillis = initialTransaction?.date ?: initialRecurringTransaction?.startDate ?: System.currentTimeMillis()
        }
    }
    var selectedDate by remember(initialTransaction, initialRecurringTransaction) { mutableStateOf(calendar.timeInMillis) }
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                selectedDate = calendar.timeInMillis
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

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
                text = if (initialTransaction != null || initialRecurringTransaction != null) "Edit Ledger Entry" else "Manual Ledger Entry",
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

            // Amount Input
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount ($)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("manual_amount_input")
            )

            // Category Selection Header
            Text(
                text = "Select Category",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Clean, non-scrollable grid of category chips
            val rows = categories.chunked(3)
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                rows.forEach { rowItems ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowItems.forEach { cat ->
                            val isSelected = category == cat
                            val style = getCategoryStyle(cat)
                            
                            Surface(
                                onClick = { category = cat },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) {
                                    style.color.copy(alpha = 0.15f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                },
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) style.color else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("manual_category_chip_$cat")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = style.icon,
                                        contentDescription = cat,
                                        tint = if (isSelected) style.color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = cat,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 11.sp
                                        ),
                                        color = if (isSelected) style.color else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Date Selector Row/Field
            OutlinedTextField(
                value = dateFormatter.format(Date(selectedDate)),
                onValueChange = {},
                readOnly = true,
                label = { Text("Transaction Date") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Select Date"
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Change Date"
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { datePickerDialog.show() }
                    .testTag("manual_date_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Recurring Switch Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isRecurring = !isRecurring }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Autorenew,
                        contentDescription = null,
                        tint = if (isRecurring) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Repeat transaction (Recurring)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isRecurring) FontWeight.Bold else FontWeight.Normal,
                        color = if (isRecurring) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                Switch(
                    checked = isRecurring,
                    onCheckedChange = { isRecurring = it },
                    modifier = Modifier.testTag("recurring_switch")
                )
            }

            if (isRecurring) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Frequency:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    val frequencies = listOf("DAILY", "WEEKLY", "MONTHLY", "YEARLY")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(2.5f)
                    ) {
                        frequencies.forEach { freq ->
                            val isSelected = frequency == freq
                            Surface(
                                onClick = { frequency = freq },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                },
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .testTag("freq_chip_$freq")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = freq,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 9.sp
                                        ),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0 && description.isNotBlank()) {
                        val id = initialTransaction?.id ?: initialRecurringTransaction?.id ?: ""
                        onSubmit(id, amount, category, type, description, isRecurring, frequency, selectedDate)
                        amountText = ""
                        description = ""
                        type = "EXPENSE"
                        category = "Food"
                        isRecurring = false
                        frequency = "MONTHLY"
                        selectedDate = System.currentTimeMillis()
                    }
                },
                enabled = amountText.isNotBlank() && description.isNotBlank(),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("manual_submit_button")
            ) {
                Text(if (initialTransaction != null || initialRecurringTransaction != null) "Save Changes" else "Post Entry")
            }
        }
    }
}

@Composable
fun TransactionRowItem(
    transaction: Transaction,
    onEdit: () -> Unit,
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
            val valColor = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary

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
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("edit_transaction_${transaction.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit entry",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
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

@Composable
fun ColorPresetRow(
    selectedColorHex: String,
    presets: List<String>,
    onColorSelected: (String) -> Unit,
    customHexValue: String,
    onCustomHexChanged: (String) -> Unit,
    label: String
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            presets.forEach { hex ->
                val color = remember(hex) {
                    try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.Gray }
                }
                val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(hex) }
                )
            }
            
            Spacer(modifier = Modifier.width(4.dp))
            
            OutlinedTextField(
                value = customHexValue,
                onValueChange = { newValue ->
                    onCustomHexChanged(newValue)
                    if (newValue.length == 7 && newValue.startsWith("#")) {
                        onColorSelected(newValue)
                    } else if (newValue.length == 6) {
                        onColorSelected("#$newValue")
                    }
                },
                placeholder = { Text("#HEX", fontSize = 11.sp) },
                singleLine = true,
                maxLines = 1,
                textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .width(85.dp)
                    .height(44.dp)
            )
        }
    }
}

@Composable
fun RecurringRowItem(
    recurring: com.example.data.RecurringTransaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryStyle = getCategoryStyle(recurring.category)
    val isExpense = recurring.type == "EXPENSE"
    val formatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("recurring_item_${recurring.id}")
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
                    contentDescription = recurring.category,
                    tint = categoryStyle.color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text Metadata Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recurring.description,
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
                        text = recurring.category,
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
                    
                    // Frequency Chip Style Tag
                    Text(
                        text = recurring.frequency.uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Starts ${formatter.format(Date(recurring.startDate))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Value Amount Text
            val valSign = if (isExpense) "-" else "+"
            val valColor = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$valSign$${String.format("%.2f", recurring.amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = valColor
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("edit_recurring_${recurring.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit recurring rule",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("delete_recurring_${recurring.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete recurring rule",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

