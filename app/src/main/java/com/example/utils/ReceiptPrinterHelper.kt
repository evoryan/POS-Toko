package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import java.io.FileOutputStream

object ReceiptPrinterHelper {

    fun generateReceiptText(
        storeName: String,
        storeAddress: String,
        storePhone: String,
        transaction: TransactionEntity,
        items: List<TransactionItemEntity>,
        footerNote: String,
        paperSize: String = "58mm"
    ): String {
        val width = if (paperSize == "80mm") 48 else 32
        val separator = "=".repeat(width)
        val dashed = "-".repeat(width)

        val sb = StringBuilder()
        sb.append(centerText(storeName, width)).append("\n")
        sb.append(centerText(storeAddress, width)).append("\n")
        sb.append(centerText("Telp: $storePhone", width)).append("\n")
        sb.append(separator).append("\n")

        sb.append("No. Struk : ").append(transaction.invoiceNo).append("\n")
        sb.append("Tanggal   : ").append(DateUtils.formatDateTime(transaction.timestamp)).append("\n")
        sb.append("Kasir     : ").append(transaction.cashierName).append("\n")
        sb.append("Metode    : ").append(transaction.paymentMethod).append("\n")
        sb.append(dashed).append("\n")

        for (item in items) {
            sb.append(item.productName).append("\n")
            val qtyPrice = "  ${item.quantity} x ${CurrencyFormatter.formatNumber(item.unitPrice)}"
            val subtotal = CurrencyFormatter.formatRupiah(item.subtotal)
            sb.append(formatTwoColumns(qtyPrice, subtotal, width)).append("\n")
        }

        sb.append(dashed).append("\n")
        sb.append(formatTwoColumns("Subtotal", CurrencyFormatter.formatRupiah(transaction.totalAmount), width)).append("\n")
        if (transaction.discount > 0) {
            sb.append(formatTwoColumns("Diskon", "-${CurrencyFormatter.formatRupiah(transaction.discount)}", width)).append("\n")
        }
        sb.append(formatTwoColumns("TOTAL", CurrencyFormatter.formatRupiah(transaction.finalAmount), width)).append("\n")
        sb.append(formatTwoColumns("Bayar (${transaction.paymentMethod})", CurrencyFormatter.formatRupiah(transaction.amountPaid), width)).append("\n")
        sb.append(formatTwoColumns("Kembalian", CurrencyFormatter.formatRupiah(transaction.changeAmount), width)).append("\n")
        sb.append(separator).append("\n")

        footerNote.lines().forEach { line ->
            sb.append(centerText(line, width)).append("\n")
        }
        sb.append(centerText("Web: toko.akbarmediagroup.me:4760", width)).append("\n")

        return sb.toString()
    }

    private fun centerText(text: String, width: Int): String {
        if (text.length >= width) return text
        val padding = (width - text.length) / 2
        return " ".repeat(padding) + text
    }

    private fun formatTwoColumns(left: String, right: String, width: Int): String {
        val totalLen = left.length + right.length
        return if (totalLen >= width) {
            "$left $right"
        } else {
            val spaces = " ".repeat(width - totalLen)
            "$left$spaces$right"
        }
    }

    fun printReceipt(
        context: Context,
        storeName: String,
        storeAddress: String,
        storePhone: String,
        transaction: TransactionEntity,
        items: List<TransactionItemEntity>,
        footerNote: String,
        paperSize: String = "58mm"
    ) {
        val receiptText = generateReceiptText(
            storeName = storeName,
            storeAddress = storeAddress,
            storePhone = storePhone,
            transaction = transaction,
            items = items,
            footerNote = footerNote,
            paperSize = paperSize
        )

        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager != null) {
                val printAdapter = object : PrintDocumentAdapter() {
                    private var pdfDocument: PrintedPdfDocument? = null

                    override fun onLayout(
                        oldAttributes: PrintAttributes?,
                        newAttributes: PrintAttributes,
                        cancellationSignal: CancellationSignal?,
                        callback: LayoutResultCallback,
                        extras: Bundle?
                    ) {
                        pdfDocument = PrintedPdfDocument(context, newAttributes)
                        if (cancellationSignal?.isCanceled == true) {
                            callback.onLayoutCancelled()
                            return
                        }
                        val info = PrintDocumentInfo.Builder("Struk_${transaction.invoiceNo}.pdf")
                            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                            .setPageCount(1)
                            .build()
                        callback.onLayoutFinished(info, true)
                    }

                    override fun onWrite(
                        pages: Array<out PageRange>?,
                        destination: ParcelFileDescriptor,
                        cancellationSignal: CancellationSignal?,
                        callback: WriteResultCallback
                    ) {
                        val document = pdfDocument ?: return
                        val page = document.startPage(0)

                        if (cancellationSignal?.isCanceled == true) {
                            callback.onWriteCancelled()
                            document.close()
                            return
                        }

                        val canvas = page.canvas
                        val paint = Paint().apply {
                            color = Color.BLACK
                            textSize = 10f
                            typeface = Typeface.MONOSPACE
                        }

                        var y = 30f
                        val lineHeight = 14f
                        val lines = receiptText.split("\n")
                        for (line in lines) {
                            canvas.drawText(line, 20f, y, paint)
                            y += lineHeight
                        }

                        document.finishPage(page)

                        try {
                            FileOutputStream(destination.fileDescriptor).use { out ->
                                document.writeTo(out)
                            }
                            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                        } catch (e: Exception) {
                            callback.onWriteFailed(e.message)
                        } finally {
                            document.close()
                        }
                    }
                }

                printManager.print("Struk_${transaction.invoiceNo}", printAdapter, PrintAttributes.Builder().build())
            } else {
                shareReceiptText(context, receiptText)
            }
        } catch (e: Exception) {
            shareReceiptText(context, receiptText)
        }
    }

    fun shareReceiptText(context: Context, text: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Bagikan Struk Belanja")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }
}
