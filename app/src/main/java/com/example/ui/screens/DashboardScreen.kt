package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
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
import kotlinx.coroutines.delay
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
import com.example.data.CustomCategory
import com.example.ui.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale

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
    val transactionsError by viewModel.transactionsError.collectAsState()

    var showBudgetDialog by remember { mutableStateOf(false) }
    var tempPrimaryHex by remember(primaryColorHex) { mutableStateOf(primaryColorHex) }
    var tempSecondaryHex by remember(secondaryColorHex) { mutableStateOf(secondaryColorHex) }
    var tempAccentHex by remember(accentColorHex) { mutableStateOf(accentColorHex) }
    var activeColorPickerTarget by remember { mutableStateOf<String?>(null) }
    var showManualAddForm by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var editingRecurringTransaction by remember { mutableStateOf<com.example.data.RecurringTransaction?>(null) }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Transactions, 1 = Recurring
    var allTimeSortOption by remember { mutableStateOf("date_desc") }
    var showSortMenu by remember { mutableStateOf(false) }

    var transactionToEditSeriesOption by remember { mutableStateOf<Transaction?>(null) }
    var transactionToDeleteSeriesOption by remember { mutableStateOf<Transaction?>(null) }

    var showLedgerSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current

    var ledgerStartDate by remember { mutableStateOf<Long?>(null) }
    var ledgerEndDate by remember { mutableStateOf<Long?>(null) }
    var ledgerSelectedFilter by remember { mutableStateOf("All Time") }

    val ledgerFilteredTransactions = remember(transactions, ledgerStartDate, ledgerEndDate) {
        transactions.filter { t ->
            val afterStart = ledgerStartDate?.let { t.date >= it } ?: true
            val beforeEnd = ledgerEndDate?.let { t.date <= it } ?: true
            afterStart && beforeEnd
        }
    }

    // Automatically update ledger start/end dates when quick filter changes
    LaunchedEffect(ledgerSelectedFilter) {
        when (ledgerSelectedFilter) {
            "All Time" -> {
                ledgerStartDate = null
                ledgerEndDate = null
            }
            "Current Month" -> {
                val start = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val end = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                ledgerStartDate = start.timeInMillis
                ledgerEndDate = end.timeInMillis
            }
            "Last 30 Days" -> {
                val start = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -30)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val end = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                ledgerStartDate = start.timeInMillis
                ledgerEndDate = end.timeInMillis
            }
            "Last 60 Days" -> {
                val start = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -60)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val end = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                ledgerStartDate = start.timeInMillis
                ledgerEndDate = end.timeInMillis
            }
            "Next 30 Days" -> {
                val start = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val end = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, 30)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                ledgerStartDate = start.timeInMillis
                ledgerEndDate = end.timeInMillis
            }
        }
    }

    // CSV Document Creation Launcher
    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                val csvContent = buildString {
                    append("Date,Type,Category,Description,Amount,Paid\n")
                    val csvFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    ledgerFilteredTransactions.forEach { t ->
                        val dateStr = csvFormatter.format(Date(t.date))
                        val escapedDesc = t.description.replace("\"", "\"\"")
                        val escapedCat = t.category.replace("\"", "\"\"")
                        val paidStr = if (t.type.uppercase() == "EXPENSE") {
                            if (t.paid) "Paid" else "Unpaid"
                        } else {
                            if (t.paid) "Received" else ""
                        }
                        append("\"$dateStr\",${t.type},\"$escapedCat\",\"$escapedDesc\",${t.amount},$paidStr\n")
                    }

                    // Summary rows
                    val incomes = ledgerFilteredTransactions.filter { it.type.uppercase() == "INCOME" }
                    val expenses = ledgerFilteredTransactions.filter { it.type.uppercase() == "EXPENSE" }
                    val totalIncome = incomes.sumOf { it.amount }
                    val totalExpense = expenses.sumOf { it.amount }
                    val totalLeftToPay = expenses.filter { !it.paid }.sumOf { it.amount }
                    val netBalance = totalIncome - totalExpense
                    val currentBalance = totalIncome - totalLeftToPay

                    append("\n")
                    append("--- Summary ---\n")
                    append("Total Income,,,,${String.format(Locale.US, "%.2f", totalIncome)},\n")
                    append("Total Expenses,,,,${String.format(Locale.US, "%.2f", totalExpense)},\n")
                    append("Total Left to Pay,,,,${String.format(Locale.US, "%.2f", totalLeftToPay)},\n")
                    append("Net Balance,,,,${String.format(Locale.US, "%.2f", netBalance)},\n")
                    append("Current Balance,,,,${String.format(Locale.US, "%.2f", currentBalance)},\n")
                }
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(csvContent.toByteArray())
                }
                Toast.makeText(context, "Ledger exported to CSV successfully", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to export CSV: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // PDF Document Creation Launcher
    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            try {
                val pdfDocument = android.graphics.pdf.PdfDocument()
                
                // standard A4 page size
                val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
                var page = pdfDocument.startPage(pageInfo)
                var canvas = page.canvas
                
                val titlePaint = android.graphics.Paint().apply {
                    textSize = 20f
                    isFakeBoldText = true
                    color = android.graphics.Color.BLACK
                }
                val subPaint = android.graphics.Paint().apply {
                    textSize = 10f
                    color = android.graphics.Color.GRAY
                }
                val headerPaint = android.graphics.Paint().apply {
                    textSize = 12f
                    isFakeBoldText = true
                    color = android.graphics.Color.DKGRAY
                }
                val colHeaderPaint = android.graphics.Paint().apply {
                    textSize = 8f
                    isFakeBoldText = true
                    color = android.graphics.Color.GRAY
                }
                val textPaint = android.graphics.Paint().apply {
                    textSize = 9f
                    color = android.graphics.Color.BLACK
                }
                val expensePaint = android.graphics.Paint().apply {
                    textSize = 9f
                    color = android.graphics.Color.parseColor("#E53935") // Red
                }
                val incomePaint = android.graphics.Paint().apply {
                    textSize = 9f
                    color = android.graphics.Color.parseColor("#43A047") // Green
                }
                val paidPaint = android.graphics.Paint().apply {
                    textSize = 8.5f
                    isFakeBoldText = true
                    color = android.graphics.Color.parseColor("#2E7D32") // Green
                }
                val unpaidPaint = android.graphics.Paint().apply {
                    textSize = 8.5f
                    isFakeBoldText = true
                    color = android.graphics.Color.parseColor("#D32F2F") // Red
                }
                val linePaint = android.graphics.Paint().apply {
                    strokeWidth = 1f
                    color = android.graphics.Color.LTGRAY
                }
                
                var yPosition = 50f
                
                // Title
                canvas.drawText("Ledgerly Complete Ledger Report", 45f, yPosition, titlePaint)
                yPosition += 20f
                
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val filterDesc = when {
                    ledgerStartDate != null && ledgerEndDate != null -> {
                        val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        "Range: ${df.format(Date(ledgerStartDate!!))} to ${df.format(Date(ledgerEndDate!!))}"
                    }
                    else -> "Range: All Time"
                }
                canvas.drawText("Generated on: ${sdf.format(Date())} | Email: ${currentUserEmail ?: ""} | $filterDesc", 45f, yPosition, subPaint)
                yPosition += 30f
                
                // Column Headers
                canvas.drawText("INCOME", 45f, yPosition, headerPaint)
                canvas.drawText("EXPENSES", 310f, yPosition, headerPaint)
                yPosition += 14f
                canvas.drawText("DATE / ITEM", 45f, yPosition, colHeaderPaint)
                canvas.drawText("AMOUNT", 185f, yPosition, colHeaderPaint)
                canvas.drawText("STATUS", 250f, yPosition, colHeaderPaint)

                canvas.drawText("DATE / ITEM", 310f, yPosition, colHeaderPaint)
                canvas.drawText("AMOUNT", 450f, yPosition, colHeaderPaint)
                canvas.drawText("STATUS", 515f, yPosition, colHeaderPaint)
                yPosition += 6f
                canvas.drawLine(45f, yPosition, 285f, yPosition, linePaint)
                canvas.drawLine(310f, yPosition, 550f, yPosition, linePaint)
                yPosition += 16f
                
                val incomes = ledgerFilteredTransactions.filter { it.type.uppercase() == "INCOME" }
                val expenses = ledgerFilteredTransactions.filter { it.type.uppercase() == "EXPENSE" }
                
                var incomeIndex = 0
                var expenseIndex = 0
                val itemHeight = 16f
                val sdfDate = SimpleDateFormat("MM-dd", Locale.getDefault())
                
                while (incomeIndex < incomes.size || expenseIndex < expenses.size) {
                    if (yPosition > 780f) {
                        pdfDocument.finishPage(page)
                        val newPageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pdfDocument.pages.size + 1).create()
                        page = pdfDocument.startPage(newPageInfo)
                        canvas = page.canvas
                        yPosition = 50f
                        
                        canvas.drawText("INCOME (cont.)", 45f, yPosition, headerPaint)
                        canvas.drawText("EXPENSES (cont.)", 310f, yPosition, headerPaint)
                        yPosition += 14f
                        canvas.drawText("DATE / ITEM", 45f, yPosition, colHeaderPaint)
                        canvas.drawText("AMOUNT", 185f, yPosition, colHeaderPaint)
                        canvas.drawText("STATUS", 250f, yPosition, colHeaderPaint)

                        canvas.drawText("DATE / ITEM", 310f, yPosition, colHeaderPaint)
                        canvas.drawText("AMOUNT", 450f, yPosition, colHeaderPaint)
                        canvas.drawText("STATUS", 515f, yPosition, colHeaderPaint)
                        yPosition += 6f
                        canvas.drawLine(45f, yPosition, 285f, yPosition, linePaint)
                        canvas.drawLine(310f, yPosition, 550f, yPosition, linePaint)
                        yPosition += 16f
                    }
                    
                    // Draw Income row item
                    if (incomeIndex < incomes.size) {
                        val t = incomes[incomeIndex]
                        val dateStr = sdfDate.format(Date(t.date))
                        val cleanDesc = if (t.description.length > 17) t.description.take(15) + ".." else t.description
                        canvas.drawText("$dateStr $cleanDesc", 45f, yPosition, textPaint)
                        canvas.drawText("+$${String.format(Locale.US, "%.2f", t.amount)}", 185f, yPosition, incomePaint)
                        val paidStatus = if (t.paid) "Received" else ""
                        val pPaint = if (t.paid) paidPaint else textPaint
                        canvas.drawText(paidStatus, 250f, yPosition, pPaint)
                        incomeIndex++
                    }
                    
                    // Draw Expense row item
                    if (expenseIndex < expenses.size) {
                        val t = expenses[expenseIndex]
                        val dateStr = sdfDate.format(Date(t.date))
                        val cleanDesc = if (t.description.length > 17) t.description.take(15) + ".." else t.description
                        canvas.drawText("$dateStr $cleanDesc", 310f, yPosition, textPaint)
                        canvas.drawText("-$${String.format(Locale.US, "%.2f", t.amount)}", 450f, yPosition, expensePaint)
                        val paidStatus = if (t.paid) "Paid" else "Unpaid"
                        val pPaint = if (t.paid) paidPaint else unpaidPaint
                        canvas.drawText(paidStatus, 515f, yPosition, pPaint)
                        expenseIndex++
                    }
                    
                    yPosition += itemHeight
                }
                
                // Totals
                val totalIncome = incomes.sumOf { it.amount }
                val totalExpense = expenses.sumOf { it.amount }
                val totalLeftToPay = expenses.filter { !it.paid }.sumOf { it.amount }
                val netBalance = totalIncome - totalExpense
                
                if (yPosition > 700f) {
                    pdfDocument.finishPage(page)
                    val newPageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pdfDocument.pages.size + 1).create()
                    page = pdfDocument.startPage(newPageInfo)
                    canvas = page.canvas
                    yPosition = 50f
                }
                
                yPosition += 10f
                canvas.drawLine(45f, yPosition, 285f, yPosition, linePaint)
                canvas.drawLine(310f, yPosition, 550f, yPosition, linePaint)
                yPosition += 15f
                
                canvas.drawText("Total Income:", 45f, yPosition, headerPaint)
                canvas.drawText("+$${String.format(Locale.US, "%.2f", totalIncome)}", 185f, yPosition, incomePaint)
                
                canvas.drawText("Total Expenses:", 310f, yPosition, headerPaint)
                canvas.drawText("-$${String.format(Locale.US, "%.2f", totalExpense)}", 450f, yPosition, expensePaint)
                
                yPosition += 16f
                val unpaidHeaderPaint = android.graphics.Paint().apply {
                    textSize = 12f
                    isFakeBoldText = true
                    color = android.graphics.Color.parseColor("#D32F2F")
                }
                canvas.drawText("Total Left to Pay:", 310f, yPosition, unpaidHeaderPaint)
                canvas.drawText("-$${String.format(Locale.US, "%.2f", totalLeftToPay)}", 450f, yPosition, unpaidPaint.apply { textSize = 9f })

                yPosition += 22f
                canvas.drawLine(45f, yPosition, 550f, yPosition, linePaint)
                yPosition += 18f

                val balancePaint = android.graphics.Paint().apply {
                    textSize = 12f
                    isFakeBoldText = true
                    color = if (netBalance >= 0) android.graphics.Color.parseColor("#43A047") else android.graphics.Color.parseColor("#E53935")
                }
                canvas.drawText("Net Balance: $${String.format(Locale.US, "%.2f", netBalance)}", 45f, yPosition, balancePaint)

                val currentBalance = totalIncome - totalLeftToPay
                val currentBalancePaint = android.graphics.Paint().apply {
                    textSize = 12f
                    isFakeBoldText = true
                    color = if (currentBalance >= 0) android.graphics.Color.parseColor("#43A047") else android.graphics.Color.parseColor("#E53935")
                }
                val cbSign = if (currentBalance >= 0) "+" else "-"
                canvas.drawText("Current Balance: $cbSign$${String.format(Locale.US, "%.2f", kotlin.math.abs(currentBalance))}", 310f, yPosition, currentBalancePaint)
                
                pdfDocument.finishPage(page)
                
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    pdfDocument.writeTo(os)
                }
                pdfDocument.close()
                Toast.makeText(context, "PDF Report exported successfully", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to generate PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val focusManager = LocalFocusManager.current

    val suggestedPrompts = remember {
        listOf(
            "Spent $15 on coffee today",
            "Salary of $2500 received",
            "Spent $12 on lunch",
            "Paid utility bill of $75",
            "Bought groceries for $65",
            "Earned $150 from freelancing",
            "Spent $45 on gas yesterday",
            "Subscribed to music for $10",
            "Spent $35 on movie tickets",
            "Bought book for $20"
        )
    }

    var currentPromptIndex by remember { mutableStateOf(0) }
    LaunchedEffect(suggestedPrompts) {
        while (true) {
            delay(4000) // Rotate every 4 seconds
            currentPromptIndex = (currentPromptIndex + 1) % suggestedPrompts.size
        }
    }
    val placeholderText = suggestedPrompts[currentPromptIndex]

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
                            text = "Ledgerly",
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
                    
                    IconButton(
                        onClick = { viewModel.logout() },
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.primary
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
                                        placeholderText,
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Monthly Budget",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                )

                                // Premium Status Pill (Seamlessly translucent backdrop matching any background)
                                val isOver = remainingBudget < 0
                                Box(
                                    modifier = Modifier
                                        .background(
                                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f),
                                            RoundedCornerShape(50)
                                        )
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                                            RoundedCornerShape(50)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isOver) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = if (isOver) "OVER BUDGET" else "ON TRACK",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Black,
                                                letterSpacing = 0.5.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "$${String.format("%,.2f", remainingBudget)}",
                                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 38.sp),
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .padding(vertical = 6.dp)
                                    .testTag("remaining_budget_text")
                            )

                            Text(
                                text = "Remaining of $${String.format("%,.2f", monthlyBudget + monthlySummary.totalIncome)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Budget utilization bar (uses the clean contrast onPrimary color dynamically)
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.onPrimary,
                                trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)
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
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "Limit: $${String.format("%,.0f", monthlyBudget)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
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
                                        MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.08f),
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Column {
                                Text(
                                    text = "EXPENSES",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "$${String.format("%,.2f", monthlySummary.totalExpense)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSecondary,
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
                                        MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.08f),
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Column {
                                Text(
                                    text = "INCOME",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "$${String.format("%,.2f", monthlySummary.totalIncome)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onTertiary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // "Complete Ledger" Button directly below the Expenses & Income Bento Grid row
                OutlinedButton(
                    onClick = { showLedgerSheet = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .testTag("complete_ledger_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Complete Ledger",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // --- 3. DATA VISUALIZATION SECTION ---
                VisualAnalyticsSection(transactions = transactions)

                // --- 4. TRANSACTION / RECURRING SWITCH HEADER ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            label = { Text("Recent", fontWeight = FontWeight.ExtraBold) },
                            modifier = Modifier.testTag("tab_transactions")
                        )
                        FilterChip(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            label = { Text("All Time", fontWeight = FontWeight.ExtraBold) },
                            modifier = Modifier.testTag("tab_all_time")
                        )
                        FilterChip(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            label = { Text("Recurring", fontWeight = FontWeight.ExtraBold) },
                            modifier = Modifier.testTag("tab_recurring")
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (selectedTab) {
                                0 -> "${monthlySummary.currentMonthList.size} items"
                                1 -> "${transactions.size} items"
                                else -> "${recurringTransactions.size} rules"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )

                        if (selectedTab == 1) {
                            Box {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { showSortMenu = true }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sort,
                                        contentDescription = "Sort Options",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Sort: " + when (allTimeSortOption) {
                                            "date_desc" -> "Newest"
                                            "date_asc" -> "Oldest"
                                            "amount_desc" -> "Highest Amount"
                                            "amount_asc" -> "Lowest Amount"
                                            "category_asc" -> "Category"
                                            "description_asc" -> "Description"
                                            else -> "Newest"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Date: Newest First") },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.ArrowDownward,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            allTimeSortOption = "date_desc"
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Date: Oldest First") },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.ArrowUpward,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            allTimeSortOption = "date_asc"
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Amount: Highest First") },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.TrendingDown,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            allTimeSortOption = "amount_desc"
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Amount: Lowest First") },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.TrendingUp,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            allTimeSortOption = "amount_asc"
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Category (A to Z)") },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Category,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            allTimeSortOption = "category_asc"
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Description (A to Z)") },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Description,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            allTimeSortOption = "description_asc"
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // --- 5. CONDITIONALLY RENDER SELECTED TAB LIST ---
                if (selectedTab == 0) {
                    if (monthlySummary.currentMonthList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
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
                                        if (tx.recurringId.isNotEmpty()) {
                                            transactionToEditSeriesOption = tx
                                        } else {
                                            editingTransaction = tx
                                            editingRecurringTransaction = null
                                            showManualAddForm = true
                                        }
                                    },
                                    onDelete = {
                                        if (tx.recurringId.isNotEmpty()) {
                                            transactionToDeleteSeriesOption = tx
                                        } else {
                                            viewModel.deleteTransaction(tx.id)
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else if (selectedTab == 1) {
                    val allTimeList = remember(transactions, allTimeSortOption) {
                        when (allTimeSortOption) {
                            "date_desc" -> transactions.sortedByDescending { it.date }
                            "date_asc" -> transactions.sortedBy { it.date }
                            "amount_desc" -> transactions.sortedByDescending { it.amount }
                            "amount_asc" -> transactions.sortedBy { it.amount }
                            "category_asc" -> transactions.sortedBy { it.category.lowercase() }
                            "description_asc" -> transactions.sortedBy { it.description.lowercase() }
                            else -> transactions.sortedByDescending { it.date }
                        }
                    }
                    if (allTimeList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
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
                                    text = "No Transactions Yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Try adding a manual transaction or use the Gemini AI prompt!",
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
                                .testTag("all_time_transactions_list"),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            allTimeList.forEach { tx ->
                                TransactionRowItem(
                                    transaction = tx,
                                    onEdit = {
                                        if (tx.recurringId.isNotEmpty()) {
                                            transactionToEditSeriesOption = tx
                                        } else {
                                            editingTransaction = tx
                                            editingRecurringTransaction = null
                                            showManualAddForm = true
                                        }
                                    },
                                    onDelete = {
                                        if (tx.recurringId.isNotEmpty()) {
                                            transactionToDeleteSeriesOption = tx
                                        } else {
                                            viewModel.deleteTransaction(tx.id)
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else {
                    if (recurringTransactions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
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

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    // --- DIALOGS FOR ERRORS & SERIES OPTIONS ---
    if (transactionsError != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearTransactionsError() },
            title = { Text("Notice", fontWeight = FontWeight.Bold) },
            text = { Text(transactionsError ?: "") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.clearTransactionsError() },
                    modifier = Modifier.testTag("dismiss_error_button")
                ) {
                    Text("OK")
                }
            }
        )
    }

    if (transactionToEditSeriesOption != null) {
        AlertDialog(
            onDismissRequest = { transactionToEditSeriesOption = null },
            title = { Text("Edit Series or One-Off?", fontWeight = FontWeight.Bold) },
            text = { Text("Do you want to edit just this single transaction instance, or the entire recurring series?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val matchingRecurring = recurringTransactions.find { it.id == transactionToEditSeriesOption!!.recurringId }
                        if (matchingRecurring != null) {
                            editingRecurringTransaction = matchingRecurring
                            editingTransaction = null
                        } else {
                            editingTransaction = transactionToEditSeriesOption
                            editingRecurringTransaction = null
                        }
                        transactionToEditSeriesOption = null
                        showManualAddForm = true
                    },
                    modifier = Modifier.testTag("edit_series_button")
                ) {
                    Text("Entire Series")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        editingTransaction = transactionToEditSeriesOption
                        editingRecurringTransaction = null
                        transactionToEditSeriesOption = null
                        showManualAddForm = true
                    },
                    modifier = Modifier.testTag("edit_instance_button")
                ) {
                    Text("Only This Instance")
                }
            }
        )
    }

    if (transactionToDeleteSeriesOption != null) {
        AlertDialog(
            onDismissRequest = { transactionToDeleteSeriesOption = null },
            title = { Text("Delete Series or One-Off?", fontWeight = FontWeight.Bold) },
            text = { Text("Do you want to delete just this single transaction instance, or the entire recurring series including all its generated instances?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRecurringSeries(transactionToDeleteSeriesOption!!.recurringId)
                        transactionToDeleteSeriesOption = null
                    },
                    modifier = Modifier.testTag("delete_series_button")
                ) {
                    Text("Entire Series")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(transactionToDeleteSeriesOption!!.id)
                        transactionToDeleteSeriesOption = null
                    },
                    modifier = Modifier.testTag("delete_instance_button")
                ) {
                    Text("Only This Instance")
                }
            }
        )
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

                    Text(
                        text = "Theme Customization",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )

                    ElegantColorSelectionRow(
                        label = "Primary Theme Color",
                        description = "Used for the Budget Card & Main Accents",
                        colorHex = tempPrimaryHex,
                        onClick = { activeColorPickerTarget = "primary" },
                        modifier = Modifier.testTag("primary_color_selector_card")
                    )

                    ElegantColorSelectionRow(
                        label = "Secondary Theme Color",
                        description = "Used for the Expenses Card & Outflows",
                        colorHex = tempSecondaryHex,
                        onClick = { activeColorPickerTarget = "secondary" },
                        modifier = Modifier.testTag("secondary_color_selector_card")
                    )

                    ElegantColorSelectionRow(
                        label = "Accent Theme Color",
                        description = "Used for the Income Card & Inflows",
                        colorHex = tempAccentHex,
                        onClick = { activeColorPickerTarget = "accent" },
                        modifier = Modifier.testTag("accent_color_selector_card")
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
                    viewModel = viewModel,
                    initialTransaction = editingTransaction,
                    initialRecurringTransaction = editingRecurringTransaction,
                    onSubmit = { id, amount, category, type, description, isRecurring, frequency, selectedDate, numInstances ->
                        if (editingTransaction != null) {
                            if (isRecurring) {
                                // Toggled from non-recurring to recurring
                                viewModel.addRecurringTransaction(amount, category, type, description, frequency, selectedDate, numInstances = numInstances)
                                viewModel.deleteTransaction(editingTransaction!!.id)
                            } else {
                                viewModel.addTransaction(amount, category, type, description, selectedDate, id, recurringId = editingTransaction!!.recurringId, paid = editingTransaction!!.paid)
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
                                    lastLoggedDate = editingRecurringTransaction!!.lastLoggedDate,
                                    numInstances = numInstances
                                )
                            }
                        } else {
                            // New entry
                            if (isRecurring) {
                                viewModel.addRecurringTransaction(amount, category, type, description, frequency, selectedDate, numInstances = numInstances)
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

    // --- CUSTOM COLOR PICKER BOTTOM DRAWER ---
    activeColorPickerTarget?.let { target ->
        val title = when (target) {
            "primary" -> "Primary Theme Color"
            "secondary" -> "Secondary Theme Color"
            else -> "Accent Theme Color"
        }
        val initialColor = when (target) {
            "primary" -> tempPrimaryHex
            "secondary" -> tempSecondaryHex
            else -> tempAccentHex
        }
        
        ColorPickerBottomSheet(
            title = title,
            initialColorHex = initialColor,
            onColorSelected = { hex ->
                when (target) {
                    "primary" -> {
                        tempPrimaryHex = hex
                        viewModel.updatePrimaryColor(hex)
                    }
                    "secondary" -> {
                        tempSecondaryHex = hex
                        viewModel.updateSecondaryColor(hex)
                    }
                    "accent" -> {
                        tempAccentHex = hex
                        viewModel.updateAccentColor(hex)
                    }
                }
            },
            onDismiss = { activeColorPickerTarget = null }
        )
    }

    // --- COMPLETE LEDGER BOTTOM DRAWER ---
    if (showLedgerSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showLedgerSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            modifier = Modifier
                .fillMaxHeight(0.9f)
                .testTag("ledger_bottom_sheet")
        ) {
            val incomeList = remember(ledgerFilteredTransactions) {
                ledgerFilteredTransactions.filter { it.type.uppercase() == "INCOME" }.sortedByDescending { it.date }
            }
            val rawExpenseList = remember(ledgerFilteredTransactions) {
                ledgerFilteredTransactions.filter { it.type.uppercase() == "EXPENSE" }.sortedByDescending { it.date }
            }

            var hidePaidExpenses by remember { mutableStateOf(false) }

            val expenseList = remember(rawExpenseList, hidePaidExpenses) {
                if (hidePaidExpenses) {
                    rawExpenseList.filter { !it.paid }
                } else {
                    rawExpenseList
                }
            }

            val totalIncome = remember(incomeList) { incomeList.sumOf { it.amount } }
            val totalExpense = remember(expenseList) { expenseList.sumOf { it.amount } }
            val ledgerFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

            val ledgerListNestedScrollConnection = remember {
                object : NestedScrollConnection {
                    override fun onPostScroll(
                        consumed: Offset,
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        return if (available.y != 0f) {
                            Offset(0f, available.y)
                        } else {
                            Offset.Zero
                        }
                    }

                    override suspend fun onPostFling(
                        consumed: Velocity,
                        available: Velocity
                    ): Velocity {
                        return if (available.y != 0f) {
                            Velocity(0f, available.y)
                        } else {
                            Velocity.Zero
                        }
                    }
                }
            }

            val showStartDatePicker = {
                val cal = Calendar.getInstance().apply {
                    if (ledgerStartDate != null) {
                        timeInMillis = ledgerStartDate!!
                    }
                }
                DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        val resultCal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month)
                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        ledgerStartDate = resultCal.timeInMillis
                        ledgerSelectedFilter = "Custom"
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            }

            val showEndDatePicker = {
                val cal = Calendar.getInstance().apply {
                    if (ledgerEndDate != null) {
                        timeInMillis = ledgerEndDate!!
                    }
                }
                DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        val resultCal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month)
                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                            set(Calendar.MILLISECOND, 999)
                        }
                        ledgerEndDate = resultCal.timeInMillis
                        ledgerSelectedFilter = "Custom"
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Complete Ledger",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Synchronized income & expense statements",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { showLedgerSheet = false },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Date Filters Quick Selection Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val filtersList = listOf("All Time", "Current Month", "Last 30 Days", "Last 60 Days", "Next 30 Days", "Custom")
                    filtersList.forEach { filter ->
                        FilterChip(
                            selected = ledgerSelectedFilter == filter,
                            onClick = { ledgerSelectedFilter = filter },
                            label = { 
                                Text(
                                    text = filter, 
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                ) 
                            },
                            modifier = Modifier.testTag("ledger_filter_${filter.lowercase().replace(" ", "_")}")
                        )
                    }
                }

                // Custom Date Range Selectors
                if (ledgerSelectedFilter == "Custom") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Start Date Card
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showStartDatePicker() }
                                .testTag("ledger_custom_start_date")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "From Date",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = ledgerStartDate?.let { ledgerFormatter.format(Date(it)) } ?: "Select Date",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Select Start Date",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // End Date Card
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showEndDatePicker() }
                                .testTag("ledger_custom_end_date")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "To Date",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = ledgerEndDate?.let { ledgerFormatter.format(Date(it)) } ?: "Select Date",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Select End Date",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Display current date range info if not Custom and not All Time
                    if (ledgerStartDate != null && ledgerEndDate != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Range: ${ledgerFormatter.format(Date(ledgerStartDate!!))} to ${ledgerFormatter.format(Date(ledgerEndDate!!))}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Hide Paid Expenses Toggle Button Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { hidePaidExpenses = !hidePaidExpenses },
                        colors = if (hidePaidExpenses) {
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        } else {
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("ledger_hide_paid_expenses_button")
                    ) {
                        Icon(
                            imageVector = if (hidePaidExpenses) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (hidePaidExpenses) "Show Paid Expenses" else "Hide Paid Expenses",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    if (hidePaidExpenses) {
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Paid Hidden",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Export Options Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Export CSV
                    Button(
                        onClick = {
                            csvLauncher.launch("Finance_Ledger_${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}.csv")
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export CSV", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    // Export PDF
                    Button(
                        onClick = {
                            pdfLauncher.launch("Finance_Ledger_${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}.pdf")
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export PDF", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // The Dual-Column Layout with Scrollable Lists
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    // Left Column (Income)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        // Sticky Income Header
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f))
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "INCOME",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }

                        // Divider line
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Income List
                        if (incomeList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No income records",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .nestedScroll(ledgerListNestedScrollConnection)
                                    .padding(horizontal = 8.dp),
                                contentPadding = PaddingValues(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(incomeList) { item ->
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = item.description.ifEmpty { item.category },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(
                                                    text = "+$${String.format("%,.2f", item.amount)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.tertiary
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = item.category,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                                )
                                                Text(
                                                    text = ledgerFormatter.format(Date(item.date)),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // "Received" indicator/toggle button
                                                Text(
                                                    text = "Received",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (item.paid) {
                                                        Color(0xFF2E7D32) // Soft beautiful Green
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) // Default grey
                                                    },
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .clickable {
                                                            viewModel.toggleTransactionPaid(item.id)
                                                        }
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        .testTag("ledger_received_toggle_${item.id}")
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))

                                                IconButton(
                                                    onClick = {
                                                        if (item.recurringId.isNotEmpty()) {
                                                            transactionToEditSeriesOption = item
                                                            showLedgerSheet = false
                                                        } else {
                                                            editingTransaction = item
                                                            editingRecurringTransaction = null
                                                            showManualAddForm = true
                                                            showLedgerSheet = false
                                                        }
                                                    },
                                                    modifier = Modifier.size(24.dp).testTag("ledger_edit_${item.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Edit",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                IconButton(
                                                    onClick = {
                                                        if (item.recurringId.isNotEmpty()) {
                                                            transactionToDeleteSeriesOption = item
                                                            showLedgerSheet = false
                                                        } else {
                                                            viewModel.deleteTransaction(item.id)
                                                        }
                                                    },
                                                    modifier = Modifier.size(24.dp).testTag("ledger_delete_${item.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.DeleteOutline,
                                                        contentDescription = "Delete",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Middle vertical divider
                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Right Column (Expenses)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        // Sticky Expenses Header
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (hidePaidExpenses) "UNPAID EXPENSES" else "EXPENSES",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        // Divider line
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Expense List
                        if (expenseList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (hidePaidExpenses) "No unpaid expenses" else "No expense records",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .nestedScroll(ledgerListNestedScrollConnection)
                                    .padding(horizontal = 8.dp),
                                contentPadding = PaddingValues(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(expenseList) { item ->
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = item.description.ifEmpty { item.category },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(
                                                    text = "-$${String.format("%,.2f", item.amount)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = item.category,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                                )
                                                Text(
                                                    text = ledgerFormatter.format(Date(item.date)),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // "Paid" indicator/toggle button
                                                Text(
                                                    text = "Paid",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (item.paid) {
                                                        Color(0xFF2E7D32) // Soft beautiful Green
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) // Default grey
                                                    },
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .clickable {
                                                            viewModel.toggleTransactionPaid(item.id)
                                                        }
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        .testTag("ledger_paid_toggle_${item.id}")
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))

                                                IconButton(
                                                    onClick = {
                                                        if (item.recurringId.isNotEmpty()) {
                                                            transactionToEditSeriesOption = item
                                                            showLedgerSheet = false
                                                        } else {
                                                            editingTransaction = item
                                                            editingRecurringTransaction = null
                                                            showManualAddForm = true
                                                            showLedgerSheet = false
                                                        }
                                                    },
                                                    modifier = Modifier.size(24.dp).testTag("ledger_edit_${item.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Edit",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                IconButton(
                                                    onClick = {
                                                        if (item.recurringId.isNotEmpty()) {
                                                            transactionToDeleteSeriesOption = item
                                                            showLedgerSheet = false
                                                        } else {
                                                            viewModel.deleteTransaction(item.id)
                                                        }
                                                    },
                                                    modifier = Modifier.size(24.dp).testTag("ledger_delete_${item.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.DeleteOutline,
                                                        contentDescription = "Delete",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(14.dp)
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

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Totals Section Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = "Total Income",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "+$${String.format("%,.2f", totalIncome)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            
                            // Vertical separator
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(40.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (hidePaidExpenses) "Unpaid Expenses" else "Total Expenses",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "-$${String.format("%,.2f", totalExpense)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        val netBalance = totalIncome - totalExpense
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Net Balance",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val isPositive = netBalance >= 0
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isPositive) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                            contentDescription = null,
                                            tint = if (isPositive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = if (isPositive) "SURPLUS" else "DEFICIT",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            ),
                                            color = if (isPositive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                Text(
                                    text = "${if (netBalance >= 0) "+" else ""}$${String.format("%,.2f", netBalance)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ManualAddForm(
    viewModel: ExpenseViewModel,
    onSubmit: (id: String, Double, String, String, String, Boolean, String, Long, Int) -> Unit,
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
    var numInstancesText by remember { mutableStateOf("12") }

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

    val defaultCategories = remember { listOf("Food", "Transport", "Utilities", "Entertainment", "Shopping", "Salary", "Investment", "Housing", "Others") }
    val customCategoriesList by viewModel.customCategories.collectAsState()
    val allCategories = remember(customCategoriesList) {
        defaultCategories + customCategoriesList.map { it.name }
    }
    var showCategoryDialog by remember { mutableStateOf(false) }

    if (showCategoryDialog) {
        var newCategoryName by remember { mutableStateOf("") }
        var editingCatId by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = {
                Text(
                    text = "Manage Custom Categories",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                ) {
                    // Input to Add/Edit Category
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newCategoryName,
                            onValueChange = { newCategoryName = it },
                            label = { Text(if (editingCatId == null) "New Category" else "Edit Category") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("category_input_field")
                        )
                        Button(
                            onClick = {
                                if (newCategoryName.isNotBlank()) {
                                    val catId = editingCatId
                                    if (catId == null) {
                                        viewModel.addCustomCategory(newCategoryName)
                                    } else {
                                        viewModel.updateCustomCategory(catId, newCategoryName)
                                        editingCatId = null
                                    }
                                    newCategoryName = ""
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("save_category_button")
                        ) {
                            Text(if (editingCatId == null) "Add" else "Save")
                        }
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            .padding(vertical = 8.dp)
                    )

                    if (customCategoriesList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No custom categories yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(customCategoriesList) { cat ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = cat.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                newCategoryName = cat.name
                                                editingCatId = cat.id
                                            },
                                            modifier = Modifier.size(28.dp).testTag("edit_category_${cat.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit category",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                viewModel.deleteCustomCategory(cat.id)
                                                if (editingCatId == cat.id) {
                                                    editingCatId = null
                                                    newCategoryName = ""
                                                }
                                            },
                                            modifier = Modifier.size(28.dp).testTag("delete_category_${cat.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Delete category",
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
                    onClick = { showCategoryDialog = false },
                    modifier = Modifier.testTag("close_category_dialog_button")
                ) {
                    Text("Close")
                }
            }
        )
    }

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

            // Category Selection Header with Edit Button for Custom Category Management
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Text(
                    text = "Select Category",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = { showCategoryDialog = true },
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("manage_categories_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Manage Custom Categories",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Clean, non-scrollable grid of category chips
            val rows = allCategories.chunked(3)
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
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Frequency",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val frequencies = listOf("DAILY", "WEEKLY", "MONTHLY", "YEARLY")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
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
                                    .height(40.dp)
                                    .testTag("freq_chip_$freq")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = freq,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 11.sp
                                        ),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = numInstancesText,
                        onValueChange = { input ->
                            numInstancesText = input.filter { it.isDigit() }
                        },
                        label = { Text("Number of Instances to Add") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("num_instances_input")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0 && description.isNotBlank()) {
                        val id = initialTransaction?.id ?: initialRecurringTransaction?.id ?: ""
                        val numInstances = numInstancesText.toIntOrNull() ?: 12
                        onSubmit(id, amount, category, type, description, isRecurring, frequency, selectedDate, numInstances)
                        amountText = ""
                        description = ""
                        type = "EXPENSE"
                        category = "Food"
                        isRecurring = false
                        frequency = "MONTHLY"
                        numInstancesText = "12"
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
    val clean = category.trim().lowercase(Locale.getDefault())
    return when (clean) {
        "food" -> CategoryStyle(Icons.Default.Restaurant, Color(0xFFE65100))
        "transport" -> CategoryStyle(Icons.Default.DirectionsCar, Color(0xFF1565C0))
        "utilities" -> CategoryStyle(Icons.Default.Lightbulb, Color(0xFFF57F17))
        "entertainment" -> CategoryStyle(Icons.Default.ConfirmationNumber, Color(0xFF6A1B9A))
        "shopping" -> CategoryStyle(Icons.Default.ShoppingBag, Color(0xFFC2185B))
        "salary" -> CategoryStyle(Icons.Default.AttachMoney, Color(0xFF2E7D32))
        "investment" -> CategoryStyle(Icons.Default.ShowChart, Color(0xFF00695C))
        "housing" -> CategoryStyle(Icons.Default.Home, Color(0xFF0277BD))
        else -> {
            val colors = listOf(
                Color(0xFF8D6E63), // Brown
                Color(0xFF78909C), // Blue Grey
                Color(0xFFEC407A), // Pink
                Color(0xFFAB47BC), // Purple
                Color(0xFF7E57C2), // Deep Purple
                Color(0xFF5C6BC0), // Indigo
                Color(0xFF26A69A), // Teal
                Color(0xFF9CCC65), // Light Green
                Color(0xFFD4E157)  // Lime
            )
            val index = Math.abs(category.hashCode()) % colors.size
            CategoryStyle(Icons.Default.Category, colors[index])
        }
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

@Composable
fun VisualAnalyticsSection(
    transactions: List<Transaction>,
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

    // 6-Month Spending Trends (Bar Chart)
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
            
            trends.add(
                MonthTrend(
                    label = label,
                    amount = totalSpent,
                    month = targetMonth,
                    year = targetYear
                )
            )
            cal.add(Calendar.MONTH, 1)
        }
        trends
    }

    val maxAmount = remember(monthlyTrends) {
        monthlyTrends.maxOfOrNull { it.amount } ?: 0.0
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
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
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // The Donut Pie Chart Canvas
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
                                val style = getCategoryStyle(cat)
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
                            text = "$${String.format("%.0f", totalCurrentMonthSpent)}",
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
                            val style = getCategoryStyle(cat)
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
                                    text = "$${String.format("%.0f", amt)} ($pct%)",
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    .padding(vertical = 12.dp)
            )

            // BAR CHART SECTION (6-MONTH TRENDS)
            Text(
                text = "Monthly Spending Trend",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .testTag("bar_chart_trend"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                monthlyTrends.forEach { trend ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (trend.amount > 0) "$${trend.amount.toInt()}" else "$0",
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
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            // Track
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(14.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            )
                            // Filled Bar
                            val barHeightPct = if (maxAmount > 0) (trend.amount / maxAmount).toFloat() else 0f
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight(barHeightPct.coerceAtLeast(0.03f))
                                    .width(14.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                            )
                                        )
                                    )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = trend.label,
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

data class MonthTrend(
    val label: String,
    val amount: Double,
    val month: Int,
    val year: Int
)

// --- CUSTOM COLOR PICKER HELPERS & COMPOSABLES ---

fun hexToHsv(hex: String): FloatArray {
    val hsv = FloatArray(3)
    try {
        val sanitized = if (hex.startsWith("#")) hex else "#$hex"
        android.graphics.Color.colorToHSV(android.graphics.Color.parseColor(sanitized), hsv)
    } catch (e: Exception) {
        hsv[0] = 0f
        hsv[1] = 1f
        hsv[2] = 1f
    }
    return hsv
}

fun hsvToHex(h: Float, s: Float, v: Float): String {
    val hsv = floatArrayOf(h, s, v)
    val colorInt = android.graphics.Color.HSVToColor(hsv)
    return String.format("#%06X", 0xFFFFFF and colorInt)
}

@Composable
fun ElegantColorSelectionRow(
    label: String,
    description: String,
    colorHex: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = remember(colorHex) {
        try { Color(android.graphics.Color.parseColor(colorHex)) } catch (e: Exception) { Color.Gray }
    }
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Color Preview Circle
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), CircleShape)
                )
                
                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$description • $colorHex",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Edit Color",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerBottomSheet(
    title: String,
    initialColorHex: String,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    val initialHsv = remember(initialColorHex) { hexToHsv(initialColorHex) }
    
    var currentHue by remember(initialColorHex) { mutableStateOf(initialHsv[0]) }
    var currentSaturation by remember(initialColorHex) { mutableStateOf(initialHsv[1]) }
    var currentValue by remember(initialColorHex) { mutableStateOf(initialHsv[2]) }
    
    val activeColorHex = remember(currentHue, currentSaturation, currentValue) {
        hsvToHex(currentHue, currentSaturation, currentValue)
    }
    
    val activeColor = remember(activeColorHex) {
        try { Color(android.graphics.Color.parseColor(activeColorHex)) } catch (e: Exception) { Color.Gray }
    }
    
    var hexInputText by remember { mutableStateOf(initialColorHex.replace("#", "")) }
    var hueInputText by remember { mutableStateOf(currentHue.toInt().toString()) }
    
    LaunchedEffect(activeColorHex) {
        onColorSelected(activeColorHex)
        val cleanActive = activeColorHex.replace("#", "")
        if (hexInputText.uppercase() != cleanActive.uppercase()) {
            hexInputText = cleanActive
        }
    }
    
    LaunchedEffect(currentHue) {
        val hInt = currentHue.toInt().toString()
        if (hueInputText != hInt) {
            hueInputText = hInt
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF121212), // Sleek pitch black/dark gray theme
        contentColor = Color.White,
        tonalElevation = 8.dp,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray.copy(alpha = 0.5f)) },
        modifier = Modifier.testTag("color_picker_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp
                    ),
                    color = Color.White
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_color_picker")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
            
            // Large Centered Color Preview Square
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(24.dp))
                    .background(activeColor)
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
            )
            
            // HEX CODE display box (with manual entry support)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = hexInputText,
                    onValueChange = { input ->
                        val filtered = input.filter { c -> c.isDigit() || c.lowercaseChar() in 'a'..'f' }.take(6)
                        hexInputText = filtered
                        if (filtered.length == 6) {
                            try {
                                val parsedHsv = hexToHsv("#$filtered")
                                currentHue = parsedHsv[0]
                                currentSaturation = parsedHsv[1]
                                currentValue = parsedHsv[2]
                            } catch (e: Exception) {
                                // ignore invalid parse
                            }
                        }
                    },
                    leadingIcon = {
                        Text(
                            text = "#",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        )
                    },
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = Color.White
                    ),
                    placeholder = {
                        Text(
                            text = "ffffff",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = Color.DarkGray
                            )
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Done
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1E1E1E),
                        unfocusedContainerColor = Color(0xFF1E1E1E),
                        focusedBorderColor = Color.White.copy(alpha = 0.4f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        cursorColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .width(220.dp)
                        .testTag("color_picker_hex_input")
                )
                Text(
                    text = "ENTER HEX CODE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    ),
                    color = Color.Gray
                )
            }
            
            // HUE SPECTRUM selection (with manual degree entry support)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HUE SPECTRUM",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = Color.Gray
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        BasicTextField(
                            value = hueInputText,
                            onValueChange = { input ->
                                val filtered = input.filter { c -> c.isDigit() }.take(3)
                                hueInputText = filtered
                                if (filtered.isNotEmpty()) {
                                    val parsedHue = filtered.toFloatOrNull() ?: 0f
                                    val clampedHue = parsedHue.coerceIn(0f, 360f)
                                    currentHue = clampedHue
                                    if (currentSaturation < 0.15f) currentSaturation = 0.9f
                                    if (currentValue < 0.15f) currentValue = 0.9f
                                }
                            },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.End
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier
                                .width(50.dp)
                                .background(Color(0xFF1E1E1E), RoundedCornerShape(6.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("color_picker_hue_input")
                        )
                        Text(
                            text = "°",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.LightGray
                            )
                        )
                    }
                }
                
                HueSlider(
                    hue = currentHue,
                    onHueChange = { newHue ->
                        currentHue = newHue
                        if (currentSaturation < 0.15f) currentSaturation = 0.9f
                        if (currentValue < 0.15f) currentValue = 0.9f
                    }
                )
            }
            
            // PREMIUM PRESETS selection
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "PREMIUM PRESETS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = Color.Gray
                )
                
                val premiumPresets = listOf(
                    "#84CC16", "#E11D48", "#D97706", "#2563EB",
                    "#06B6D4", "#4F46E5", "#7C3AED", "#DB2777",
                    "#EC4899", "#EA580C", "#CA8A04", "#EAB308"
                )
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (row in 0 until 3) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (col in 0 until 4) {
                                val presetIndex = row * 4 + col
                                if (presetIndex < premiumPresets.size) {
                                    val presetHex = premiumPresets[presetIndex]
                                    val presetColor = remember(presetHex) {
                                        try { Color(android.graphics.Color.parseColor(presetHex)) } catch (e: Exception) { Color.Gray }
                                    }
                                    val isSelected = activeColorHex.equals(presetHex, ignoreCase = true)
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(presetColor)
                                            .border(
                                                width = if (isSelected) 3.dp else 0.dp,
                                                color = if (isSelected) Color.White else Color.Transparent,
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .clickable {
                                                val hsv = hexToHsv(presetHex)
                                                currentHue = hsv[0]
                                                currentSaturation = hsv[1]
                                                currentValue = hsv[2]
                                            }
                                            .testTag("color_preset_$presetIndex")
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

@Composable
fun HueSlider(
    hue: Float,
    onHueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        
        val hueColors = remember {
            listOf(
                Color.Red, Color.Yellow, Color.Green, Color.Cyan,
                Color.Blue, Color.Magenta, Color.Red
            )
        }
        
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { offset ->
                            val rawX = offset.x.coerceIn(0f, width)
                            val selectedHue = (rawX / width) * 360f
                            onHueChange(selectedHue)
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val rawX = change.position.x.coerceIn(0f, width)
                        val selectedHue = (rawX / width) * 360f
                        onHueChange(selectedHue)
                    }
                }
        ) {
            val trackHeight = 12.dp.toPx()
            val trackY = (height - trackHeight) / 2
            
            // Draw continuous hue gradient track
            drawRoundRect(
                brush = Brush.linearGradient(hueColors),
                topLeft = Offset(0f, trackY),
                size = Size(width, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2, trackHeight / 2)
            )
            
            // Draw selector thumb
            val thumbRadius = 14.dp.toPx()
            val thumbX = (hue / 360f) * width
            
            // Outer shadow
            drawCircle(
                color = Color.Black.copy(alpha = 0.2f),
                radius = thumbRadius + 2.dp.toPx(),
                center = Offset(thumbX, height / 2)
            )
            
            // White ring
            drawCircle(
                color = Color.White,
                radius = thumbRadius,
                center = Offset(thumbX, height / 2)
            )
            
            // Inner colored center
            drawCircle(
                color = Color.hsv(hue, 1f, 1f),
                radius = thumbRadius - 4.dp.toPx(),
                center = Offset(thumbX, height / 2)
            )
        }
    }
}

