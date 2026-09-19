package com.example.ui.screens.reports

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.data.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LedgerReportExporter {

    fun exportToCsv(
        context: Context,
        uri: Uri,
        transactions: List<Transaction>,
        startDate: Long?,
        endDate: Long?
    ) {
        val csvContent = buildString {
            append("Type,Date,Description,Category,Amount,Status\n")
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            transactions.forEach { tx ->
                val dateStr = formatter.format(Date(tx.date))
                val desc = "\"${tx.description.replace("\"", "\"\"")}\""
                val status = if (tx.type.uppercase() == "INCOME") {
                    if (tx.paid) "Received" else "Pending"
                } else {
                    if (tx.paid) "Paid" else "Unpaid"
                }
                append("${tx.type},$dateStr,$desc,${tx.category},${String.format(Locale.US, "%.2f", tx.amount)},$status\n")
            }

            val incomes = transactions.filter { it.type.uppercase() == "INCOME" }
            val expenses = transactions.filter { it.type.uppercase() == "EXPENSE" }
            val cashOnHand = incomes.filter { it.category.trim().equals("cash", ignoreCase = true) }.sumOf { it.amount }
            val totalIncome = incomes.filter { !it.category.trim().equals("cash", ignoreCase = true) }.sumOf { it.amount }
            val totalLeftToReceive = incomes.filter { !it.paid && !it.category.trim().equals("cash", ignoreCase = true) }.sumOf { it.amount }
            val totalExpense = expenses.sumOf { it.amount }
            val totalLeftToPay = expenses.filter { !it.paid }.sumOf { it.amount }
            val netBalance = cashOnHand + (totalIncome - totalExpense)
            val currentBalance = cashOnHand + totalLeftToReceive - totalLeftToPay

            append("\n")
            append("--- Summary ---\n")
            append("Total Income,,,,${String.format(Locale.US, "%.2f", totalIncome)},\n")
            append("Total Left to Receive,,,,${String.format(Locale.US, "%.2f", totalLeftToReceive)},\n")
            append("Cash on Hand,,,,${String.format(Locale.US, "%.2f", cashOnHand)},\n")
            append("Total Expenses,,,,${String.format(Locale.US, "%.2f", totalExpense)},\n")
            append("Total Left to Pay,,,,${String.format(Locale.US, "%.2f", totalLeftToPay)},\n")
            append("Net Balance,,,,${String.format(Locale.US, "%.2f", netBalance)},\n")
            append("Current Balance,,,,${String.format(Locale.US, "%.2f", currentBalance)},\n")
        }
        context.contentResolver.openOutputStream(uri)?.use { os ->
            os.write(csvContent.toByteArray())
        }
    }

    fun exportToPdf(
        context: Context,
        uri: Uri,
        userEmail: String?,
        transactions: List<Transaction>,
        startDate: Long?,
        endDate: Long?
    ) {
        val pdfDocument = PdfDocument()

        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val titlePaint = Paint().apply {
            textSize = 20f
            isFakeBoldText = true
            color = Color.BLACK
        }
        val subPaint = Paint().apply {
            textSize = 10f
            color = Color.GRAY
        }
        val headerPaint = Paint().apply {
            textSize = 12f
            isFakeBoldText = true
            color = Color.DKGRAY
        }
        val colHeaderPaint = Paint().apply {
            textSize = 8f
            isFakeBoldText = true
            color = Color.GRAY
        }
        val textPaint = Paint().apply {
            textSize = 9f
            color = Color.BLACK
        }
        val expensePaint = Paint().apply {
            textSize = 9f
            color = Color.parseColor("#E53935")
        }
        val incomePaint = Paint().apply {
            textSize = 9f
            color = Color.parseColor("#43A047")
        }
        val paidPaint = Paint().apply {
            textSize = 8.5f
            isFakeBoldText = true
            color = Color.parseColor("#2E7D32")
        }
        val unpaidPaint = Paint().apply {
            textSize = 8.5f
            isFakeBoldText = true
            color = Color.parseColor("#D32F2F")
        }
        val linePaint = Paint().apply {
            strokeWidth = 1f
            color = Color.LTGRAY
        }

        var yPosition = 50f

        // Title
        canvas.drawText("Ledgerly Financial Ledger Report", 45f, yPosition, titlePaint)
        yPosition += 20f

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val filterDesc = when {
            startDate != null && endDate != null -> {
                val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                "Range: ${df.format(Date(startDate))} to ${df.format(Date(endDate))}"
            }
            else -> "Range: All Time"
        }
        canvas.drawText("Generated on: ${sdf.format(Date())} | Email: ${userEmail ?: ""} | $filterDesc", 45f, yPosition, subPaint)
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

        val incomes = transactions.filter { it.type.uppercase() == "INCOME" }
        val expenses = transactions.filter { it.type.uppercase() == "EXPENSE" }

        var incomeIndex = 0
        var expenseIndex = 0
        val itemHeight = 16f
        val sdfDate = SimpleDateFormat("MM-dd", Locale.getDefault())

        while (incomeIndex < incomes.size || expenseIndex < expenses.size) {
            if (yPosition > 780f) {
                pdfDocument.finishPage(page)
                val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, pdfDocument.pages.size + 1).create()
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

            if (incomeIndex < incomes.size) {
                val item = incomes[incomeIndex]
                val dateLabel = sdfDate.format(Date(item.date))
                val title = item.description.take(16)
                canvas.drawText("$dateLabel $title", 45f, yPosition, textPaint)
                canvas.drawText("+$${String.format(Locale.US, "%.2f", item.amount)}", 185f, yPosition, incomePaint)
                val statusText = if (item.paid) "Received" else "Pending"
                canvas.drawText(statusText, 250f, yPosition, if (item.paid) paidPaint else unpaidPaint)
                incomeIndex++
            }

            if (expenseIndex < expenses.size) {
                val item = expenses[expenseIndex]
                val dateLabel = sdfDate.format(Date(item.date))
                val title = item.description.take(16)
                canvas.drawText("$dateLabel $title", 310f, yPosition, textPaint)
                canvas.drawText("-$${String.format(Locale.US, "%.2f", item.amount)}", 450f, yPosition, expensePaint)
                val statusText = if (item.paid) "Paid" else "Unpaid"
                canvas.drawText(statusText, 515f, yPosition, if (item.paid) paidPaint else unpaidPaint)
                expenseIndex++
            }

            yPosition += itemHeight
        }

        // Summary footer on last page if space allows, or start new page
        if (yPosition > 700f) {
            pdfDocument.finishPage(page)
            val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, pdfDocument.pages.size + 1).create()
            page = pdfDocument.startPage(newPageInfo)
            canvas = page.canvas
            yPosition = 50f
        } else {
            yPosition += 20f
        }

        canvas.drawLine(45f, yPosition, 550f, yPosition, linePaint)
        yPosition += 20f

        val cashOnHand = incomes.filter { it.category.trim().equals("cash", ignoreCase = true) }.sumOf { it.amount }
        val totalIncome = incomes.filter { !it.category.trim().equals("cash", ignoreCase = true) }.sumOf { it.amount }
        val totalLeftToReceive = incomes.filter { !it.paid && !it.category.trim().equals("cash", ignoreCase = true) }.sumOf { it.amount }
        val totalExpense = expenses.sumOf { it.amount }
        val totalLeftToPay = expenses.filter { !it.paid }.sumOf { it.amount }
        val netBalance = cashOnHand + (totalIncome - totalExpense)
        val currentBalance = cashOnHand + totalLeftToReceive - totalLeftToPay

        canvas.drawText("SUMMARY TOTALS", 45f, yPosition, headerPaint)
        yPosition += 16f
        canvas.drawText("Total Income: +$${String.format(Locale.US, "%.2f", totalIncome)} (Left to Receive: +$${String.format(Locale.US, "%.2f", totalLeftToReceive)})", 45f, yPosition, textPaint)
        yPosition += 14f
        canvas.drawText("Cash on Hand: +$${String.format(Locale.US, "%.2f", cashOnHand)}", 45f, yPosition, textPaint)
        yPosition += 14f
        canvas.drawText("Total Expenses: -$${String.format(Locale.US, "%.2f", totalExpense)} (Left to Pay: -$${String.format(Locale.US, "%.2f", totalLeftToPay)})", 45f, yPosition, textPaint)
        yPosition += 14f
        canvas.drawText("Net Balance: $${String.format(Locale.US, "%.2f", netBalance)}", 45f, yPosition, headerPaint)
        yPosition += 14f
        canvas.drawText("Projected Current Balance: $${String.format(Locale.US, "%.2f", currentBalance)}", 45f, yPosition, headerPaint)

        pdfDocument.finishPage(page)

        context.contentResolver.openOutputStream(uri)?.use { os ->
            pdfDocument.writeTo(os)
        }
        pdfDocument.close()
    }
}
