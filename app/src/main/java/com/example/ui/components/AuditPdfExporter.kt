package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.AuditDeletedItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AuditPdfExporter {

    fun generateAndShareAuditPdf(
        context: Context,
        userEmail: String,
        deletedItems: List<AuditDeletedItem>,
        onSuccess: (File) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        try {
            val file = createAuditPdfDocument(context, userEmail, deletedItems)
            sharePdfFile(context, file)
            onSuccess(file)
        } catch (e: Exception) {
            onError(e.message ?: "Failed to generate PDF")
        }
    }

    private fun createAuditPdfDocument(
        context: Context,
        userEmail: String,
        items: List<AuditDeletedItem>
    ): File {
        val document = PdfDocument()

        val pageWidth = 595 // A4 standard width in points (72 dpi)
        val pageHeight = 842 // A4 standard height in points (72 dpi)
        val marginX = 36f
        val printableWidth = pageWidth - (marginX * 2)

        // Setup paints
        val titlePaint = Paint().apply {
            color = Color.rgb(20, 24, 33)
            textSize = 18f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = Color.rgb(85, 95, 110)
            textSize = 9.5f
            isAntiAlias = true
        }

        val headerBannerPaint = Paint().apply {
            color = Color.rgb(240, 243, 248)
            style = Paint.Style.FILL
        }

        val sectionHeadingPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 12f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val tableHeaderPaint = Paint().apply {
            color = Color.rgb(71, 85, 105)
            textSize = 9f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val rowTextPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 8.5f
            isAntiAlias = true
        }

        val rowSecondaryPaint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 7.5f
            isAntiAlias = true
        }

        val badgeTextPaint = Paint().apply {
            textSize = 7.5f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val badgeBgPaint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 0.8f
        }

        val zebraPaint = Paint().apply {
            color = Color.rgb(248, 250, 252)
            style = Paint.Style.FILL
        }

        val redAlertPaint = Paint().apply {
            color = Color.rgb(220, 38, 38)
            textSize = 9f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        val generatedDateStr = dateFormat.format(Date())

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        // Header Banner
        canvas.drawRect(marginX, 32f, marginX + printableWidth, 102f, headerBannerPaint)

        // Accent top bar
        val accentPaint = Paint().apply {
            color = Color.rgb(99, 102, 241) // Indigo
            style = Paint.Style.FILL
        }
        canvas.drawRect(marginX, 32f, marginX + printableWidth, 36f, accentPaint)

        canvas.drawText("Ledgerly • Audit Deletion Report", marginX + 16f, 62f, titlePaint)
        canvas.drawText(
            "Target Account: $userEmail  |  Generated: $generatedDateStr",
            marginX + 16f,
            78f,
            subtitlePaint
        )
        canvas.drawText(
            "Permanent audit log of deleted Transactions, Forecast Pipeline & Income Notes",
            marginX + 16f,
            92f,
            subtitlePaint
        )

        var currentY = 125f

        // Metric summary row
        val txCount = items.count { it.itemType.equals("TRANSACTION", ignoreCase = true) }
        val forecastCount = items.count { it.itemType.equals("FORECAST", ignoreCase = true) }
        val notesCount = items.count { it.itemType.equals("NOTE", ignoreCase = true) }
        val userDeletesCount = items.count { it.sourceOrDeletedBy.contains("User", ignoreCase = true) }
        val externalDeletesCount = items.size - userDeletesCount

        canvas.drawText("AUDIT SUMMARY METRICS", marginX, currentY, sectionHeadingPaint)
        currentY += 16f

        val metricBoxWidth = (printableWidth - 24f) / 4f
        val metricBoxHeight = 44f

        val metrics = listOf(
            Pair("TOTAL DELETED", "${items.size}"),
            Pair("TRANSACTIONS", "$txCount"),
            Pair("FORECASTS", "$forecastCount"),
            Pair("NOTES / OTHER", "$notesCount")
        )

        for (i in metrics.indices) {
            val left = marginX + (i * (metricBoxWidth + 8f))
            canvas.drawRect(left, currentY, left + metricBoxWidth, currentY + metricBoxHeight, headerBannerPaint)
            
            val mTitlePaint = Paint().apply {
                color = Color.rgb(100, 116, 139)
                textSize = 7.5f
                isFakeBoldText = true
                isAntiAlias = true
            }
            val mValPaint = Paint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = 14f
                isFakeBoldText = true
                isAntiAlias = true
            }
            canvas.drawText(metrics[i].first, left + 8f, currentY + 16f, mTitlePaint)
            canvas.drawText(metrics[i].second, left + 8f, currentY + 36f, mValPaint)
        }

        currentY += metricBoxHeight + 14f

        val sourceSubtitle = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 8.5f
            isAntiAlias = true
        }
        canvas.drawText(
            "Source Breakdown: User Actions ($userDeletesCount) • External / Remote / Cleanup Sources ($externalDeletesCount)",
            marginX,
            currentY,
            sourceSubtitle
        )
        currentY += 18f

        // Table Header
        fun drawTableHeader(y: Float) {
            canvas.drawRect(marginX, y, marginX + printableWidth, y + 20f, headerBannerPaint)
            val headerY = y + 14f
            canvas.drawText("TYPE", marginX + 6f, headerY, tableHeaderPaint)
            canvas.drawText("TITLE & DETAILS", marginX + 64f, headerY, tableHeaderPaint)
            canvas.drawText("AMOUNT", marginX + 250f, headerY, tableHeaderPaint)
            canvas.drawText("SOURCE / DELETED BY", marginX + 320f, headerY, tableHeaderPaint)
            canvas.drawText("DELETED AT", marginX + 440f, headerY, tableHeaderPaint)
            canvas.drawLine(marginX, y + 20f, marginX + printableWidth, y + 20f, linePaint)
        }

        drawTableHeader(currentY)
        currentY += 21f

        val rowHeight = 36f

        if (items.isEmpty()) {
            currentY += 20f
            canvas.drawText(
                "No deleted records found in audit log. All transactions, forecasts, and notes remain intact.",
                marginX + 16f,
                currentY,
                subtitlePaint
            )
        } else {
            for (index in items.indices) {
                val item = items[index]

                // Check for page break
                if (currentY + rowHeight > pageHeight - 40f) {
                    // Draw footer on current page
                    canvas.drawText(
                        "Page $pageNumber  |  Confidential Audit Log  |  Ledgerly Financial Engine",
                        marginX,
                        pageHeight - 20f,
                        subtitlePaint
                    )
                    document.finishPage(page)

                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas

                    currentY = 40f
                    drawTableHeader(currentY)
                    currentY += 21f
                }

                // Alternating zebra striping
                if (index % 2 == 1) {
                    canvas.drawRect(marginX, currentY, marginX + printableWidth, currentY + rowHeight, zebraPaint)
                }
                canvas.drawLine(marginX, currentY + rowHeight, marginX + printableWidth, currentY + rowHeight, linePaint)

                // 1. TYPE BADGE
                val (badgeBg, badgeFg) = when (item.itemType.uppercase()) {
                    "TRANSACTION" -> Pair(Color.rgb(238, 242, 255), Color.rgb(79, 70, 229))
                    "FORECAST" -> Pair(Color.rgb(254, 243, 199), Color.rgb(180, 83, 9))
                    "NOTE" -> Pair(Color.rgb(236, 253, 245), Color.rgb(4, 120, 87))
                    "RECURRING" -> Pair(Color.rgb(255, 241, 242), Color.rgb(225, 29, 72))
                    else -> Pair(Color.rgb(241, 245, 249), Color.rgb(71, 85, 105))
                }

                badgeBgPaint.color = badgeBg
                badgeTextPaint.color = badgeFg
                val badgeRectLeft = marginX + 6f
                val badgeRectTop = currentY + 7f
                canvas.drawRoundRect(
                    badgeRectLeft,
                    badgeRectTop,
                    badgeRectLeft + 52f,
                    badgeRectTop + 14f,
                    4f,
                    4f,
                    badgeBgPaint
                )
                val typeLabel = if (item.itemType.length > 8) item.itemType.substring(0, 8) else item.itemType
                canvas.drawText(typeLabel, badgeRectLeft + 5f, badgeRectTop + 10f, badgeTextPaint)

                // 2. TITLE & DETAILS
                val titleSafe = if (item.title.length > 32) item.title.substring(0, 32) + "..." else item.title
                canvas.drawText(titleSafe, marginX + 64f, currentY + 14f, rowTextPaint)

                val detailsPreview = if (item.details.isNotBlank()) item.details else item.categoryOrStatus
                val detailsSafe = if (detailsPreview.length > 36) detailsPreview.substring(0, 36) + "..." else detailsPreview
                canvas.drawText(detailsSafe, marginX + 64f, currentY + 28f, rowSecondaryPaint)

                // 3. AMOUNT
                val amountText = if (item.amount > 0.0) {
                    "$${String.format(Locale.US, "%,.2f", item.amount)}"
                } else {
                    "—"
                }
                canvas.drawText(amountText, marginX + 250f, currentY + 18f, rowTextPaint)

                // 4. SOURCE / DELETED BY
                val isExternal = !item.sourceOrDeletedBy.contains("User", ignoreCase = true)
                val sourceTextSafe = if (item.sourceOrDeletedBy.length > 20) {
                    item.sourceOrDeletedBy.substring(0, 20) + "..."
                } else {
                    item.sourceOrDeletedBy
                }
                if (isExternal) {
                    canvas.drawText(sourceTextSafe, marginX + 320f, currentY + 14f, redAlertPaint)
                    canvas.drawText("External / Cloud Source", marginX + 320f, currentY + 26f, rowSecondaryPaint)
                } else {
                    canvas.drawText(sourceTextSafe, marginX + 320f, currentY + 14f, rowTextPaint)
                    canvas.drawText("Direct User Deletion", marginX + 320f, currentY + 26f, rowSecondaryPaint)
                }

                // 5. DELETED AT
                val deletedAtStr = dateFormat.format(Date(item.deletedAt))
                canvas.drawText(deletedAtStr, marginX + 440f, currentY + 18f, rowSecondaryPaint)

                currentY += rowHeight
            }
        }

        // Draw page footer on last page
        canvas.drawText(
            "Page $pageNumber  |  Confidential Audit Log  |  Ledgerly Financial Engine",
            marginX,
            pageHeight - 20f,
            subtitlePaint
        )
        document.finishPage(page)

        // Write file
        val reportsDir = File(context.cacheDir, "reports").apply { if (!exists()) mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val pdfFile = File(reportsDir, "Audit_Deletion_Report_$timeStamp.pdf")

        val outputStream = FileOutputStream(pdfFile)
        document.writeTo(outputStream)
        outputStream.flush()
        outputStream.close()
        document.close()

        return pdfFile
    }

    private fun sharePdfFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Ledgerly Audit Report - Deleted Records")
            putExtra(Intent.EXTRA_TEXT, "Attached is the Audit Deletion Report generated from Ledgerly.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Export / Share Audit PDF").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
