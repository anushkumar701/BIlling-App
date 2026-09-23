package com.fruitbilling.app.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.fruitbilling.app.data.model.BillWithItems
import com.fruitbilling.app.data.model.PaymentMethod
import java.io.File
import java.io.FileWriter
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Neat & Structured CSV / Excel Sales Export Manager (§6).
 * Formats bill history into clean CSV files compatible with
 * Microsoft Excel, Google Sheets, LibreOffice, and accounting software.
 */
object CsvExportManager {

    fun exportBillsToCsv(
        context: Context,
        bills: List<BillWithItems>,
        reportTitle: String = "Sales Report"
    ): Result<File> {
        return try {
            val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val fileTimestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

            val exportDir = File(context.cacheDir, "csv_exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val csvFile = File(exportDir, "RetailBilling_${reportTitle.replace(" ", "_")}_$fileTimestamp.csv")

            var totalCalculated = BigDecimal.ZERO
            var totalFinal = BigDecimal.ZERO
            var cashTotal = BigDecimal.ZERO
            var upiTotal = BigDecimal.ZERO
            var pendingTotal = BigDecimal.ZERO

            FileWriter(csvFile).use { writer ->
                // Title Header
                writer.append("Retail Billing POS - $reportTitle\n")
                writer.append("Exported On,${SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.getDefault()).format(Date())}\n")
                writer.append("Total Records,${bills.size}\n\n")

                // Table Column Headers
                writer.append("Bill No,Date,Time,Customer,Calculated Amount (INR),Final Amount (INR),Discount / Adj (INR),Payment Mode,Items Breakdown,Status\n")

                for (item in bills) {
                    val bill = item.bill
                    val compTime = bill.completedAt ?: bill.createdAt
                    val dateStr = dateFormat.format(Date(compTime))
                    val timeStr = timeFormat.format(Date(compTime))
                    val custName = (bill.customerName ?: "").replace("\"", "\"\"")

                    val calcAmt = bill.calculatedTotal
                    val finalAmt = bill.effectiveChargedAmount
                    val discountAmt = if (bill.finalAmount != null) {
                        calcAmt.subtract(bill.finalAmount).max(BigDecimal.ZERO)
                    } else BigDecimal.ZERO

                    totalCalculated = totalCalculated.add(calcAmt)
                    totalFinal = totalFinal.add(finalAmt)

                    when (bill.paymentMethod) {
                        PaymentMethod.CASH -> cashTotal = cashTotal.add(finalAmt)
                        PaymentMethod.UPI -> upiTotal = upiTotal.add(finalAmt)
                        PaymentMethod.PENDING -> pendingTotal = pendingTotal.add(finalAmt)
                        null -> {}
                    }

                    // Format items breakdown inside quotes to prevent CSV delimiter clash
                    val itemsSummary = item.items.joinToString("; ") { bi ->
                        val prettyExpr = CalculatorEngine.prettyExpression(bi.expression)
                        val name = bi.productNameSnapshot ?: ""
                        if (name.isNotBlank()) "$name ($prettyExpr = ₹${bi.calculatedAmount})" else "$prettyExpr = ₹${bi.calculatedAmount}"
                    }.replace("\"", "\"\"")

                    val payLabel = when (bill.paymentMethod) {
                        PaymentMethod.CASH -> "Cash"
                        PaymentMethod.UPI -> "UPI"
                        PaymentMethod.PENDING -> "Pending"
                        null -> "Unspecified"
                    }

                    writer.append("\"${bill.formattedBillNumber}\",")
                    writer.append("\"$dateStr\",")
                    writer.append("\"$timeStr\",")
                    writer.append("\"$custName\",")
                    writer.append("\"${calcAmt.toPlainString()}\",")
                    writer.append("\"${finalAmt.toPlainString()}\",")
                    writer.append("\"${discountAmt.toPlainString()}\",")
                    writer.append("\"$payLabel\",")
                    writer.append("\"$itemsSummary\",")
                    writer.append("\"${bill.status}\"\n")
                }

                // Summary Footer
                writer.append("\n")
                writer.append("SUMMARY TOTALS,,,,,,,,,\n")
                writer.append("Total Bills,${bills.size},,,,,,,\n")
                writer.append("Total Sales (INR),${totalFinal.toPlainString()},,,,,,,\n")
                writer.append("Cash in Drawer (INR),${cashTotal.toPlainString()},,,,,,,\n")
                writer.append("UPI / Bank (INR),${upiTotal.toPlainString()},,,,,,,\n")
                writer.append("Pending / Pay Later (INR),${pendingTotal.toPlainString()},,,,,,,\n")
            }

            Result.success(csvFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Shares the exported CSV file via Android share sheet to Excel, Sheets, WhatsApp, or Email.
     */
    fun shareCsvFile(context: Context, csvFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            csvFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Retail Billing Sales Report")
            putExtra(Intent.EXTRA_TEXT, "Here is the sales report export from Retail Billing POS.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Open / Share Sales Report (Excel / Sheets)")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
