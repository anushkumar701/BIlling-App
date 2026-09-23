package com.fruitbilling.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.fruitbilling.app.data.model.BillWithItems
import com.fruitbilling.app.data.model.PaymentMethod
import com.fruitbilling.app.data.preferences.ShopPreferences
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Native Android PDF Export & Print Manager.
 * Uses 100% native Android PdfDocument API — zero external dependencies.
 * Generates beautiful, print-ready A4 invoices and sales summary reports.
 */
object PdfExportManager {

    private const val PAGE_WIDTH = 595 // Standard A4 points (72 dpi)
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36f

    /**
     * Exports a comprehensive sales report of bills to an A4 PDF document.
     */
    fun exportBillsToPdf(
        context: Context,
        bills: List<BillWithItems>,
        reportTitle: String = "Sales Report"
    ): Result<File> {
        return try {
            val exportDir = File(context.cacheDir, "pdf_exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val fileTimestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val pdfFile = File(exportDir, "RetailBilling_${reportTitle.replace(" ", "_")}_$fileTimestamp.pdf")

            val shopName = ShopPreferences.getShopName(context).ifBlank { "Retail Billing POS" }
            val shopPhone = ShopPreferences.getShopPhone(context)

            val doc = PdfDocument()

            // Calculate totals
            var totalSales = BigDecimal.ZERO
            var cashSales = BigDecimal.ZERO
            var upiSales = BigDecimal.ZERO
            var pendingSales = BigDecimal.ZERO

            for (item in bills) {
                val amt = item.bill.effectiveChargedAmount
                totalSales = totalSales.add(amt)
                when (item.bill.paymentMethod) {
                    PaymentMethod.CASH -> cashSales = cashSales.add(amt)
                    PaymentMethod.UPI -> upiSales = upiSales.add(amt)
                    PaymentMethod.PENDING -> pendingSales = pendingSales.add(amt)
                    null -> {}
                }
            }

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 10f
            }

            val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
            }

            val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(27, 94, 32) // Forest Green
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
            }

            val subHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.DKGRAY
                textSize = 11f
            }

            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(200, 200, 200)
                strokeWidth = 1f
            }

            val rowBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(245, 248, 245)
            }

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            var page = doc.startPage(pageInfo)
            var canvas: Canvas = page.canvas

            var y = MARGIN + 20f

            fun drawHeader() {
                // Shop Name
                canvas.drawText(shopName, MARGIN, y, headerPaint)
                y += 18f

                // Contact
                val contactText = if (!shopPhone.isNullOrBlank()) "Phone: $shopPhone  |  " else ""
                val dateText = "Generated: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())}"
                canvas.drawText("$contactText$dateText", MARGIN, y, subHeaderPaint)
                y += 16f

                // Report Title
                val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK
                    textSize = 13f
                    typeface = Typeface.DEFAULT_BOLD
                }
                canvas.drawText("$reportTitle (${bills.size} Bills)", MARGIN, y, titlePaint)
                y += 12f

                canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
                y += 14f

                // Summary Stats Box (only on first page)
                if (pageNumber == 1) {
                    val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.rgb(240, 246, 240)
                    }
                    canvas.drawRoundRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 42f, 8f, 8f, boxPaint)

                    val kpiTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.rgb(80, 80, 80)
                        textSize = 9f
                    }
                    val kpiValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.BLACK
                        textSize = 11f
                        typeface = Typeface.DEFAULT_BOLD
                    }

                    val colWidth = (PAGE_WIDTH - 2 * MARGIN) / 4f

                    // Total Sales
                    canvas.drawText("TOTAL SALES", MARGIN + 12f, y + 15f, kpiTitlePaint)
                    canvas.drawText(MoneyUtils.formatPrice(totalSales), MARGIN + 12f, y + 32f, kpiValPaint)

                    // Cash
                    canvas.drawText("CASH", MARGIN + colWidth + 12f, y + 15f, kpiTitlePaint)
                    canvas.drawText(MoneyUtils.formatPrice(cashSales), MARGIN + colWidth + 12f, y + 32f, kpiValPaint)

                    // UPI
                    canvas.drawText("UPI", MARGIN + 2 * colWidth + 12f, y + 15f, kpiTitlePaint)
                    canvas.drawText(MoneyUtils.formatPrice(upiSales), MARGIN + 2 * colWidth + 12f, y + 32f, kpiValPaint)

                    // Pending
                    canvas.drawText("PENDING", MARGIN + 3 * colWidth + 12f, y + 15f, kpiTitlePaint)
                    val pendingPaint = Paint(kpiValPaint).apply {
                        if (pendingSales > BigDecimal.ZERO) color = Color.rgb(230, 81, 0)
                    }
                    canvas.drawText(MoneyUtils.formatPrice(pendingSales), MARGIN + 3 * colWidth + 12f, y + 32f, pendingPaint)

                    y += 54f
                }

                // Table Column Headers
                val thBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(220, 235, 220)
                }
                canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 20f, thBgPaint)

                val thPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(27, 94, 32)
                    textSize = 9.5f
                    typeface = Typeface.DEFAULT_BOLD
                }

                canvas.drawText("BILL #", MARGIN + 8f, y + 14f, thPaint)
                canvas.drawText("DATE & TIME", MARGIN + 55f, y + 14f, thPaint)
                canvas.drawText("CUSTOMER", MARGIN + 155f, y + 14f, thPaint)
                canvas.drawText("ITEMS / SUMMARY", MARGIN + 245f, y + 14f, thPaint)
                canvas.drawText("PAY MODE", MARGIN + 395f, y + 14f, thPaint)
                canvas.drawText("AMOUNT", PAGE_WIDTH - MARGIN - 65f, y + 14f, thPaint)

                y += 24f
            }

            drawHeader()

            val dateFormat = SimpleDateFormat("dd-MM-yy hh:mm a", Locale.getDefault())

            bills.forEachIndexed { index, item ->
                // Check page height overflow
                if (y > PAGE_HEIGHT - MARGIN - 40f) {
                    // Draw page number footer
                    val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.GRAY
                        textSize = 8.5f
                    }
                    canvas.drawText("Page $pageNumber  |  Retail Billing POS", MARGIN, PAGE_HEIGHT - MARGIN + 15f, footerPaint)

                    doc.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                    page = doc.startPage(pageInfo)
                    canvas = page.canvas
                    y = MARGIN + 20f
                    drawHeader()
                }

                val bill = item.bill
                val compTime = bill.completedAt ?: bill.createdAt
                val dateStr = dateFormat.format(Date(compTime))
                val custName = bill.customerName?.take(16) ?: "-"

                val itemsSummary = if (item.items.isNotEmpty()) {
                    val firstItem = item.items[0].productNameSnapshot ?: item.items[0].displayExpression
                    if (item.items.size > 1) "$firstItem +${item.items.size - 1} more" else firstItem
                } else "Empty"

                val payMode = when (bill.paymentMethod) {
                    PaymentMethod.CASH -> "Cash"
                    PaymentMethod.UPI -> "UPI"
                    PaymentMethod.PENDING -> "Pending"
                    null -> "Unspecified"
                }

                val amountStr = MoneyUtils.formatPrice(bill.effectiveChargedAmount)

                // Zebra striping
                if (index % 2 == 1) {
                    canvas.drawRect(MARGIN, y - 11f, PAGE_WIDTH - MARGIN, y + 8f, rowBgPaint)
                }

                canvas.drawText(bill.formattedBillNumber, MARGIN + 8f, y, boldPaint)
                canvas.drawText(dateStr, MARGIN + 55f, y, textPaint)
                canvas.drawText(custName, MARGIN + 155f, y, textPaint)

                val summaryTruncated = if (itemsSummary.length > 25) itemsSummary.take(23) + "…" else itemsSummary
                canvas.drawText(summaryTruncated, MARGIN + 245f, y, textPaint)

                val payPaint = Paint(textPaint).apply {
                    when (bill.paymentMethod) {
                        PaymentMethod.CASH -> color = Color.rgb(46, 125, 50)
                        PaymentMethod.UPI -> color = Color.rgb(21, 101, 192)
                        PaymentMethod.PENDING -> color = Color.rgb(230, 81, 0)
                        null -> color = Color.DKGRAY
                    }
                }
                canvas.drawText(payMode, MARGIN + 395f, y, payPaint)
                canvas.drawText(amountStr, PAGE_WIDTH - MARGIN - 65f, y, boldPaint)

                y += 18f
            }

            // Bottom line & footer
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
            y += 14f

            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.GRAY
                textSize = 8.5f
            }
            canvas.drawText("Page $pageNumber  |  Generated by Retail Billing POS", MARGIN, PAGE_HEIGHT - MARGIN + 15f, footerPaint)

            doc.finishPage(page)

            FileOutputStream(pdfFile).use { out ->
                doc.writeTo(out)
            }
            doc.close()

            Result.success(pdfFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Exports an individual bill invoice to an A4 printable PDF.
     */
    fun exportSingleBillToPdf(
        context: Context,
        billWithItems: BillWithItems
    ): Result<File> {
        return try {
            val exportDir = File(context.cacheDir, "pdf_exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val bill = billWithItems.bill
            val pdfFile = File(exportDir, "Invoice_${bill.formattedBillNumber.replace("#", "")}_${System.currentTimeMillis()}.pdf")

            val shopName = ShopPreferences.getShopName(context).ifBlank { "Retail Billing POS" }
            val shopPhone = ShopPreferences.getShopPhone(context)

            val doc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas

            val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(27, 94, 32)
                textSize = 20f
                typeface = Typeface.DEFAULT_BOLD
            }

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 10.5f
            }

            val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 10.5f
                typeface = Typeface.DEFAULT_BOLD
            }

            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(200, 200, 200)
                strokeWidth = 1f
            }

            var y = MARGIN + 25f

            // Store Header
            canvas.drawText(shopName, MARGIN, y, headerPaint)
            y += 18f

            if (!shopPhone.isNullOrBlank()) {
                val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.DKGRAY
                    textSize = 10.5f
                }
                canvas.drawText("📞 Phone: $shopPhone", MARGIN, y, subPaint)
                y += 16f
            }

            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
            y += 18f

            // Invoice Meta Box
            val metaPaint = Paint(boldPaint).apply { textSize = 13f }
            canvas.drawText("RETAIL INVOICE", MARGIN, y, metaPaint)
            canvas.drawText(bill.formattedBillNumber, PAGE_WIDTH - MARGIN - 60f, y, metaPaint)
            y += 16f

            val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(
                Date(bill.completedAt ?: bill.createdAt)
            )
            canvas.drawText("Date: $dateStr", MARGIN, y, textPaint)

            if (!bill.customerName.isNullOrBlank()) {
                canvas.drawText("Customer: ${bill.customerName}", PAGE_WIDTH - MARGIN - 180f, y, boldPaint)
            }
            y += 16f

            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
            y += 16f

            // Table Header
            val thBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(235, 245, 235) }
            canvas.drawRect(MARGIN, y - 10f, PAGE_WIDTH - MARGIN, y + 14f, thBg)

            val thPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(27, 94, 32)
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
            }

            canvas.drawText("#", MARGIN + 8f, y + 5f, thPaint)
            canvas.drawText("ITEM DESCRIPTION", MARGIN + 35f, y + 5f, thPaint)
            canvas.drawText("RATE / QTY", MARGIN + 280f, y + 5f, thPaint)
            canvas.drawText("AMOUNT", PAGE_WIDTH - MARGIN - 65f, y + 5f, thPaint)

            y += 24f

            // Table Items
            val rowBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(250, 252, 250) }
            billWithItems.items.forEachIndexed { idx, it ->
                if (idx % 2 == 1) {
                    canvas.drawRect(MARGIN, y - 11f, PAGE_WIDTH - MARGIN, y + 9f, rowBg)
                }

                val title = it.productNameSnapshot ?: it.displayExpression
                val rateQty = if (it.productNameSnapshot != null && it.expression.isNotBlank()) {
                    it.displayExpression
                } else "-"

                canvas.drawText("${idx + 1}", MARGIN + 8f, y, textPaint)
                canvas.drawText(title.take(35), MARGIN + 35f, y, boldPaint)
                canvas.drawText(rateQty.take(25), MARGIN + 280f, y, textPaint)
                canvas.drawText(MoneyUtils.formatPrice(it.calculatedAmount), PAGE_WIDTH - MARGIN - 65f, y, boldPaint)

                y += 20f
            }

            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
            y += 16f

            // Totals
            val totalCalculated = MoneyUtils.formatPrice(bill.calculatedTotal)
            val grandTotal = MoneyUtils.formatPrice(bill.effectiveChargedAmount)

            canvas.drawText("Subtotal:", PAGE_WIDTH - MARGIN - 180f, y, textPaint)
            canvas.drawText(totalCalculated, PAGE_WIDTH - MARGIN - 65f, y, textPaint)
            y += 16f

            if (bill.finalAmount != null && bill.finalAmount < bill.calculatedTotal) {
                val discount = bill.calculatedTotal.subtract(bill.finalAmount)
                val discPaint = Paint(textPaint).apply { color = Color.rgb(211, 47, 47) }
                canvas.drawText("Discount / Round Off:", PAGE_WIDTH - MARGIN - 180f, y, discPaint)
                canvas.drawText("-${MoneyUtils.formatPrice(discount)}", PAGE_WIDTH - MARGIN - 65f, y, discPaint)
                y += 16f
            }

            val grandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(27, 94, 32)
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText("GRAND TOTAL:", PAGE_WIDTH - MARGIN - 180f, y, grandPaint)
            canvas.drawText(grandTotal, PAGE_WIDTH - MARGIN - 65f, y, grandPaint)
            y += 22f

            // Payment Mode
            val payModeText = when (bill.paymentMethod) {
                PaymentMethod.CASH -> "💵 Paid via Cash"
                PaymentMethod.UPI -> "📱 Paid via UPI"
                PaymentMethod.PENDING -> "⏳ Payment Pending (Pay Later)"
                null -> "⚠️ Payment Mode Unspecified"
            }
            canvas.drawText("Payment Status: $payModeText", MARGIN, y, boldPaint)
            y += 30f

            // Footer note
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
            y += 18f

            val footPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.DKGRAY
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText("Thank you for your business! Please visit again.", MARGIN, y, footPaint)

            doc.finishPage(page)

            FileOutputStream(pdfFile).use { out ->
                doc.writeTo(out)
            }
            doc.close()

            Result.success(pdfFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Shares or prints the PDF using Android system share / viewer / print sheet.
     */
    fun sharePdfFile(context: Context, pdfFile: File, title: String = "Share PDF Invoice") {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, pdfFile.nameWithoutExtension)
            putExtra(Intent.EXTRA_TEXT, "Here is your invoice / sales report PDF from Retail Billing POS.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
