package com.example.ui.screens.reports

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.data.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

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
                drawWatermark(canvas)
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
        // Summary block with enhanced spacing requires ~180f vertical clearance
        if (yPosition > 600f) {
            drawWatermark(canvas)
            pdfDocument.finishPage(page)
            val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, pdfDocument.pages.size + 1).create()
            page = pdfDocument.startPage(newPageInfo)
            canvas = page.canvas
            yPosition = 50f
        } else {
            yPosition += 28f
        }

        val summaryDividerPaint = Paint().apply {
            strokeWidth = 0.8f
            color = Color.rgb(203, 213, 225)
        }
        canvas.drawLine(45f, yPosition, 550f, yPosition, summaryDividerPaint)
        yPosition += 26f

        val cashOnHand = incomes.filter { it.category.trim().equals("cash", ignoreCase = true) }.sumOf { it.amount }
        val totalIncome = incomes.filter { !it.category.trim().equals("cash", ignoreCase = true) }.sumOf { it.amount }
        val totalLeftToReceive = incomes.filter { !it.paid && !it.category.trim().equals("cash", ignoreCase = true) }.sumOf { it.amount }
        val totalExpense = expenses.sumOf { it.amount }
        val totalLeftToPay = expenses.filter { !it.paid }.sumOf { it.amount }
        val netBalance = cashOnHand + (totalIncome - totalExpense)
        val currentBalance = cashOnHand + totalLeftToReceive - totalLeftToPay

        val summaryTitlePaint = Paint().apply {
            textSize = 12f
            isFakeBoldText = true
            color = Color.rgb(15, 23, 42)
            isAntiAlias = true
        }

        val summaryItemPaint = Paint().apply {
            textSize = 9.5f
            color = Color.rgb(51, 65, 85)
            isAntiAlias = true
        }

        val summaryBoldPaint = Paint().apply {
            textSize = 10.5f
            isFakeBoldText = true
            color = Color.rgb(15, 23, 42)
            isAntiAlias = true
        }

        // Summary Title
        canvas.drawText("SUMMARY TOTALS", 45f, yPosition, summaryTitlePaint)
        yPosition += 24f

        // Total Income line with generous spacing
        val incomeStr = "Total Income: +$${String.format(Locale.US, "%,.2f", totalIncome)}  (Left to Receive: +$${String.format(Locale.US, "%,.2f", totalLeftToReceive)})"
        canvas.drawText(incomeStr, 45f, yPosition, summaryItemPaint)
        yPosition += 22f

        // Cash on Hand line
        val cashStr = "Cash on Hand: +$${String.format(Locale.US, "%,.2f", cashOnHand)}"
        canvas.drawText(cashStr, 45f, yPosition, summaryItemPaint)
        yPosition += 22f

        // Total Expenses line
        val expenseStr = "Total Expenses: -$${String.format(Locale.US, "%,.2f", totalExpense)}  (Left to Pay: -$${String.format(Locale.US, "%,.2f", totalLeftToPay)})"
        canvas.drawText(expenseStr, 45f, yPosition, summaryItemPaint)
        yPosition += 26f

        // Subtle divider separating breakdown from balance conclusions
        canvas.drawLine(45f, yPosition, 550f, yPosition, summaryDividerPaint)
        yPosition += 24f

        // Net Balance line
        val netSign = if (netBalance < 0) "-" else ""
        val netBalanceStr = "Net Balance: $netSign$${String.format(Locale.US, "%,.2f", abs(netBalance))}"
        canvas.drawText(netBalanceStr, 45f, yPosition, summaryBoldPaint)
        yPosition += 24f

        // Projected Current Balance line
        val currentSign = if (currentBalance < 0) "-" else ""
        val currentBalanceStr = "Projected Current Balance: $currentSign$${String.format(Locale.US, "%,.2f", abs(currentBalance))}"
        canvas.drawText(currentBalanceStr, 45f, yPosition, summaryBoldPaint)

        // Draw watermark across the middle of the page
        drawWatermark(canvas)

        pdfDocument.finishPage(page)

        context.contentResolver.openOutputStream(uri)?.use { os ->
            pdfDocument.writeTo(os)
        }
        pdfDocument.close()
    }

    private fun drawWatermark(canvas: Canvas, pageWidth: Float = 595f, pageHeight: Float = 842f) {
        val watermarkPaint = Paint().apply {
            color = Color.argb(34, 100, 116, 139) // Crisp slate grey with ~13% opacity
            textSize = 66f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.12f
        }

        canvas.save()
        canvas.translate(pageWidth / 2f, pageHeight / 2f)
        canvas.rotate(-35f)
        val yOffset = (watermarkPaint.descent() + watermarkPaint.ascent()) / 2f
        canvas.drawText("LEDGERLY", 0f, -yOffset, watermarkPaint)
        canvas.restore()
    }
}
