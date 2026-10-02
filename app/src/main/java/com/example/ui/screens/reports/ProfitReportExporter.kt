package com.example.ui.screens.reports

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ProfitReportExporter {

    fun exportToCsv(
        context: Context,
        uri: Uri,
        items: List<ProfitItemMetric>,
        startDate: Long?,
        endDate: Long?
    ) {
        val csvContent = buildString {
            append("Description,Entries Count,Total Revenue,Total Cost,Gross Profit,Profit Margin %,Latest Date\n")
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            items.forEach { item ->
                val desc = "\"${item.description.replace("\"", "\"\"")}\""
                val dateStr = formatter.format(Date(item.latestDate))
                append("$desc,${item.count},${String.format(Locale.US, "%.2f", item.totalRevenue)},${String.format(Locale.US, "%.2f", item.totalCost)},${String.format(Locale.US, "%.2f", item.totalProfit)},${String.format(Locale.US, "%.1f%%", item.profitPercentage)},$dateStr\n")
            }

            val totalRevenue = items.sumOf { it.totalRevenue }
            val totalCost = items.sumOf { it.totalCost }
            val totalProfit = totalRevenue - totalCost
            val overallMargin = if (totalRevenue > 0) ((totalRevenue - totalCost) / totalRevenue) * 100.0 else 0.0

            append("\n")
            append("--- Summary ---\n")
            append("Unique Items Ranked,,,,${items.size},\n")
            if (startDate != null && endDate != null) {
                append("Date Range,,,,\"${formatter.format(Date(startDate))} to ${formatter.format(Date(endDate))}\",\n")
            }
            append("Total Revenue,,,,${String.format(Locale.US, "%.2f", totalRevenue)},\n")
            append("Total Cost,,,,${String.format(Locale.US, "%.2f", totalCost)},\n")
            append("Total Gross Profit,,,,${String.format(Locale.US, "%.2f", totalProfit)},\n")
            append("Overall Profit Margin,,,,${String.format(Locale.US, "%.1f%%", overallMargin)},\n")
        }

        context.contentResolver.openOutputStream(uri)?.use { os ->
            os.write(csvContent.toByteArray())
        }
    }

    fun exportToPdf(
        context: Context,
        uri: Uri,
        userEmail: String?,
        items: List<ProfitItemMetric>,
        startDate: Long?,
        endDate: Long?
    ) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val titlePaint = Paint().apply {
            textSize = 18f
            isFakeBoldText = true
            color = Color.BLACK
        }
        val subPaint = Paint().apply {
            textSize = 9f
            color = Color.DKGRAY
        }
        val headerPaint = Paint().apply {
            textSize = 10f
            isFakeBoldText = true
            color = Color.BLACK
        }
        val colHeaderPaint = Paint().apply {
            textSize = 8.5f
            isFakeBoldText = true
            color = Color.parseColor("#4B5563")
        }
        val textPaint = Paint().apply {
            textSize = 8.5f
            color = Color.BLACK
        }
        val rightTextPaint = Paint().apply {
            textSize = 8.5f
            color = Color.BLACK
            textAlign = Paint.Align.RIGHT
        }
        val profitPaint = Paint().apply {
            textSize = 8.5f
            isFakeBoldText = true
            color = Color.parseColor("#15803D") // Vibrant green
            textAlign = Paint.Align.RIGHT
        }
        val marginBadgePaint = Paint().apply {
            textSize = 8.5f
            isFakeBoldText = true
            color = Color.parseColor("#047857")
            textAlign = Paint.Align.RIGHT
        }
        val linePaint = Paint().apply {
            strokeWidth = 1f
            color = Color.parseColor("#E5E7EB")
        }
        val summaryBoxPaint = Paint().apply {
            color = Color.parseColor("#F3F4F6")
            style = Paint.Style.FILL
        }

        var yPosition = 45f

        // Document Title
        canvas.drawText("Profit Percentage by Item Description", 40f, yPosition, titlePaint)
        yPosition += 16f

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val dateRangeDesc = when {
            startDate != null && endDate != null -> {
                val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                "Range: ${df.format(Date(startDate))} to ${df.format(Date(endDate))}"
            }
            else -> "Range: All Time"
        }
        canvas.drawText("Generated on: ${sdf.format(Date())} | Email: ${userEmail ?: "N/A"} | $dateRangeDesc", 40f, yPosition, subPaint)
        yPosition += 22f

        // KPI Summary Box
        val totalRevenue = items.sumOf { it.totalRevenue }
        val totalCost = items.sumOf { it.totalCost }
        val totalProfit = totalRevenue - totalCost
        val overallMargin = if (totalRevenue > 0) ((totalRevenue - totalCost) / totalRevenue) * 100.0 else 0.0

        canvas.drawRoundRect(40f, yPosition, 555f, yPosition + 46f, 8f, 8f, summaryBoxPaint)
        val kpiY = yPosition + 18f
        val kpiValY = yPosition + 34f

        canvas.drawText("TOTAL REVENUE", 55f, kpiY, colHeaderPaint)
        canvas.drawText("$${String.format(Locale.US, "%,.2f", totalRevenue)}", 55f, kpiValY, headerPaint)

        canvas.drawText("TOTAL COST", 185f, kpiY, colHeaderPaint)
        canvas.drawText("$${String.format(Locale.US, "%,.2f", totalCost)}", 185f, kpiValY, headerPaint)

        canvas.drawText("GROSS PROFIT", 315f, kpiY, colHeaderPaint)
        val grossPaint = Paint(headerPaint).apply { color = Color.parseColor("#15803D") }
        canvas.drawText("+$${String.format(Locale.US, "%,.2f", totalProfit)}", 315f, kpiValY, grossPaint)

        canvas.drawText("AVERAGE MARGIN", 445f, kpiY, colHeaderPaint)
        val marginPaint = Paint(headerPaint).apply { color = Color.parseColor("#047857") }
        canvas.drawText("${String.format(Locale.US, "%.1f%%", overallMargin)}", 445f, kpiValY, marginPaint)

        yPosition += 62f

        // Table Header
        canvas.drawText("ITEM DESCRIPTION", 45f, yPosition, colHeaderPaint)
        canvas.drawText("QTY", 250f, yPosition, rightTextPaint)
        canvas.drawText("REVENUE", 325f, yPosition, rightTextPaint)
        canvas.drawText("COST", 400f, yPosition, rightTextPaint)
        canvas.drawText("PROFIT ($)", 475f, yPosition, rightTextPaint)
        canvas.drawText("PROFIT (%)", 550f, yPosition, rightTextPaint)

        yPosition += 6f
        canvas.drawLine(40f, yPosition, 555f, yPosition, linePaint)
        yPosition += 16f

        var pageNumber = 1

        items.forEach { item ->
            if (yPosition > 790f) {
                // Draw footer watermark & page number
                canvas.drawText("Ledgerly • Page $pageNumber", 270f, 825f, subPaint)
                pdfDocument.finishPage(page)
                pageNumber++
                val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(newPageInfo)
                canvas = page.canvas

                yPosition = 50f
                canvas.drawText("Profit Percentage by Item Description (Cont.)", 40f, yPosition, headerPaint)
                yPosition += 16f
                // Redraw table headers
                canvas.drawText("ITEM DESCRIPTION", 45f, yPosition, colHeaderPaint)
                canvas.drawText("QTY", 250f, yPosition, rightTextPaint)
                canvas.drawText("REVENUE", 325f, yPosition, rightTextPaint)
                canvas.drawText("COST", 400f, yPosition, rightTextPaint)
                canvas.drawText("PROFIT ($)", 475f, yPosition, rightTextPaint)
                canvas.drawText("PROFIT (%)", 550f, yPosition, rightTextPaint)
                yPosition += 6f
                canvas.drawLine(40f, yPosition, 555f, yPosition, linePaint)
                yPosition += 16f
            }

            val descText = item.description
            canvas.drawText(descText, 45f, yPosition, textPaint)
            canvas.drawText(item.count.toString(), 250f, yPosition, rightTextPaint)
            canvas.drawText("$${String.format(Locale.US, "%.2f", item.totalRevenue)}", 325f, yPosition, rightTextPaint)
            canvas.drawText("$${String.format(Locale.US, "%.2f", item.totalCost)}", 400f, yPosition, rightTextPaint)
            canvas.drawText("+$${String.format(Locale.US, "%.2f", item.totalProfit)}", 475f, yPosition, profitPaint)
            canvas.drawText(String.format(Locale.US, "%.1f%%", item.profitPercentage), 550f, yPosition, marginBadgePaint)

            yPosition += 5f
            canvas.drawLine(40f, yPosition, 555f, yPosition, linePaint)
            yPosition += 15f
        }

        // Footer on final page
        canvas.drawText("Ledgerly • Page $pageNumber", 270f, 825f, subPaint)
        pdfDocument.finishPage(page)

        context.contentResolver.openOutputStream(uri)?.use { os ->
            pdfDocument.writeTo(os)
        }
        pdfDocument.close()
    }
}
