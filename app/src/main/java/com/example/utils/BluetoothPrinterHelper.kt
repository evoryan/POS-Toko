package com.example.utils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.UUID

data class BtDeviceItem(
    val name: String,
    val address: String,
    val isBonded: Boolean = true
)

object BluetoothPrinterHelper {
    private const val TAG = "BluetoothPrinter"
    // Standard SPP (Serial Port Profile) UUID for ESC/POS Bluetooth printers
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // ESC/POS Commands
    private val ESC_INIT = byteArrayOf(0x1B, 0x40) // ESC @
    private val ESC_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)
    private val ESC_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)
    private val ESC_ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02)
    private val ESC_BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)
    private val ESC_BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)
    private val ESC_DOUBLE_SIZE = byteArrayOf(0x1D, 0x21, 0x11) // GS ! 0x11 (2x width & height)
    private val ESC_NORMAL_SIZE = byteArrayOf(0x1D, 0x21, 0x00)
    private val ESC_FEED_LINES = byteArrayOf(0x1B, 0x64, 0x03) // Feed 3 lines
    private val GS_CUT = byteArrayOf(0x1D, 0x56, 0x42, 0x00) // Cut paper

    fun getBluetoothAdapter(): BluetoothAdapter? {
        return try {
            BluetoothAdapter.getDefaultAdapter()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting BluetoothAdapter: ${e.message}")
            null
        }
    }

    fun isBluetoothSupported(): Boolean = getBluetoothAdapter() != null

    fun isBluetoothEnabled(): Boolean {
        val adapter = getBluetoothAdapter() ?: return false
        return try {
            adapter.isEnabled
        } catch (e: SecurityException) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BtDeviceItem> {
        val adapter = getBluetoothAdapter() ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()

        return try {
            val bonded = adapter.bondedDevices ?: emptySet()
            bonded.map { device ->
                BtDeviceItem(
                    name = device.name ?: "Printer Bluetooth (${device.address})",
                    address = device.address,
                    isBonded = true
                )
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Bluetooth permission not granted for bonded devices: ${e.message}")
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching bonded devices: ${e.message}")
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun printReceipt(
        macAddress: String,
        storeName: String,
        storeAddress: String,
        storePhone: String,
        transaction: TransactionEntity,
        items: List<TransactionItemEntity>,
        footerNote: String,
        paperSize: String = "58mm"
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val adapter = getBluetoothAdapter()
            ?: return@withContext Result.failure(Exception("Bluetooth tidak didukung pada perangkat ini"))

        if (!adapter.isEnabled) {
            return@withContext Result.failure(Exception("Bluetooth belum diaktifkan"))
        }

        var socket: BluetoothSocket? = null
        var outputStream: OutputStream? = null

        try {
            val device: BluetoothDevice = adapter.getRemoteDevice(macAddress)
            // Cancel discovery before connecting to speed up connection
            try {
                adapter.cancelDiscovery()
            } catch (e: Exception) {
                // Ignore
            }

            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            outputStream = socket.outputStream

            val width = if (paperSize == "80mm") 48 else 32
            val separator = "=".repeat(width)
            val dashed = "-".repeat(width)
            val charset = Charset.forName("CP437")

            // 1. Initialize
            outputStream.write(ESC_INIT)

            // 2. Header (Centered, Bold, Double Size)
            outputStream.write(ESC_ALIGN_CENTER)
            outputStream.write(ESC_BOLD_ON)
            outputStream.write(ESC_DOUBLE_SIZE)
            outputStream.write("$storeName\n".toByteArray(charset))
            outputStream.write(ESC_NORMAL_SIZE)
            outputStream.write(ESC_BOLD_OFF)

            if (storeAddress.isNotBlank()) {
                outputStream.write("$storeAddress\n".toByteArray(charset))
            }
            if (storePhone.isNotBlank()) {
                outputStream.write("Telp: $storePhone\n".toByteArray(charset))
            }
            outputStream.write("$separator\n".toByteArray(charset))

            // 3. Transaction Meta
            outputStream.write(ESC_ALIGN_LEFT)
            outputStream.write("No. Struk : ${transaction.invoiceNo}\n".toByteArray(charset))
            outputStream.write("Tanggal   : ${DateUtils.formatDateTime(transaction.timestamp)}\n".toByteArray(charset))
            outputStream.write("Kasir     : ${transaction.cashierName}\n".toByteArray(charset))
            outputStream.write("Metode    : ${transaction.paymentMethod}\n".toByteArray(charset))
            outputStream.write("$dashed\n".toByteArray(charset))

            // 4. Items
            for (item in items) {
                outputStream.write("${item.productName}\n".toByteArray(charset))
                val qtyPrice = "  ${item.quantity} x ${CurrencyFormatter.formatNumber(item.unitPrice)}"
                val subtotal = CurrencyFormatter.formatRupiah(item.subtotal)
                val line = formatTwoColumns(qtyPrice, subtotal, width)
                outputStream.write("$line\n".toByteArray(charset))
            }

            outputStream.write("$dashed\n".toByteArray(charset))

            // 5. Total & Calculation
            val subtotalLine = formatTwoColumns("Subtotal", CurrencyFormatter.formatRupiah(transaction.totalAmount), width)
            outputStream.write("$subtotalLine\n".toByteArray(charset))

            if (transaction.discount > 0) {
                val discLine = formatTwoColumns("Diskon", "-${CurrencyFormatter.formatRupiah(transaction.discount)}", width)
                outputStream.write("$discLine\n".toByteArray(charset))
            }

            outputStream.write(ESC_BOLD_ON)
            val totalLine = formatTwoColumns("TOTAL", CurrencyFormatter.formatRupiah(transaction.finalAmount), width)
            outputStream.write("$totalLine\n".toByteArray(charset))
            outputStream.write(ESC_BOLD_OFF)

            val paidLine = formatTwoColumns("Bayar (${transaction.paymentMethod})", CurrencyFormatter.formatRupiah(transaction.amountPaid), width)
            outputStream.write("$paidLine\n".toByteArray(charset))

            val changeLine = formatTwoColumns("Kembalian", CurrencyFormatter.formatRupiah(transaction.changeAmount), width)
            outputStream.write("$changeLine\n".toByteArray(charset))
            outputStream.write("$separator\n".toByteArray(charset))

            // 6. Footer
            outputStream.write(ESC_ALIGN_CENTER)
            footerNote.lines().forEach { line ->
                if (line.isNotBlank()) {
                    outputStream.write("$line\n".toByteArray(charset))
                }
            }
            outputStream.write("pos.akbarmediagroup.me\n".toByteArray(charset))

            // 7. Feed & Cut
            outputStream.write(ESC_FEED_LINES)
            try {
                outputStream.write(GS_CUT)
            } catch (e: Exception) {
                // Ignore cut failure on manual tear printers
            }

            outputStream.flush()
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error printing via Bluetooth: ${e.message}", e)
            Result.failure(Exception("Gagal mencetak ke printer Bluetooth: ${e.localizedMessage ?: "Koneksi gagal"}"))
        } finally {
            try {
                outputStream?.close()
                socket?.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun printTestReceipt(
        macAddress: String,
        storeName: String,
        paperSize: String = "58mm"
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val adapter = getBluetoothAdapter()
            ?: return@withContext Result.failure(Exception("Bluetooth tidak didukung"))

        var socket: BluetoothSocket? = null
        var outputStream: OutputStream? = null

        try {
            val device: BluetoothDevice = adapter.getRemoteDevice(macAddress)
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            outputStream = socket.outputStream

            val width = if (paperSize == "80mm") 48 else 32
            val separator = "=".repeat(width)
            val charset = Charset.forName("CP437")

            outputStream.write(ESC_INIT)
            outputStream.write(ESC_ALIGN_CENTER)
            outputStream.write(ESC_BOLD_ON)
            outputStream.write(ESC_DOUBLE_SIZE)
            outputStream.write("TES CETAK BLUETOOTH\n".toByteArray(charset))
            outputStream.write(ESC_NORMAL_SIZE)
            outputStream.write(ESC_BOLD_OFF)
            outputStream.write("$storeName\n".toByteArray(charset))
            outputStream.write("$separator\n".toByteArray(charset))
            outputStream.write(ESC_ALIGN_LEFT)
            outputStream.write("Printer: ${device.name ?: "Thermal Printer"}\n".toByteArray(charset))
            outputStream.write("MAC: $macAddress\n".toByteArray(charset))
            outputStream.write("Status: BERHASIL TERHUBUNG!\n".toByteArray(charset))
            outputStream.write("$separator\n".toByteArray(charset))
            outputStream.write(ESC_ALIGN_CENTER)
            outputStream.write("Siap digunakan untuk Kasir POS\n".toByteArray(charset))
            outputStream.write(ESC_FEED_LINES)
            outputStream.flush()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(Exception("Koneksi gagal: ${e.localizedMessage ?: "Tidak dapat tersambung"}"))
        } finally {
            try {
                outputStream?.close()
                socket?.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun printBarcodeLabel(
        macAddress: String,
        storeName: String,
        product: ProductEntity,
        copies: Int = 1,
        paperSize: String = "58mm"
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val adapter = getBluetoothAdapter()
            ?: return@withContext Result.failure(Exception("Bluetooth tidak didukung pada perangkat ini"))

        if (!adapter.isEnabled) {
            return@withContext Result.failure(Exception("Bluetooth belum diaktifkan"))
        }

        var socket: BluetoothSocket? = null
        var outputStream: OutputStream? = null

        try {
            val device: BluetoothDevice = adapter.getRemoteDevice(macAddress)
            try {
                adapter.cancelDiscovery()
            } catch (e: Exception) {
                // Ignore
            }

            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            outputStream = socket.outputStream

            val width = if (paperSize == "80mm") 48 else 32
            val separator = "-".repeat(width)
            val charset = Charset.forName("CP437")

            val safeCopies = copies.coerceIn(1, 50)
            repeat(safeCopies) {
                // 1. Initialize
                outputStream.write(ESC_INIT)

                // 2. Store Header (Centered)
                outputStream.write(ESC_ALIGN_CENTER)
                if (storeName.isNotBlank()) {
                    outputStream.write("$storeName\n".toByteArray(charset))
                }

                // 3. Product Name (Bold)
                outputStream.write(ESC_BOLD_ON)
                outputStream.write("${product.name}\n".toByteArray(charset))
                outputStream.write(ESC_BOLD_OFF)

                // 4. Price (Double size, bold)
                outputStream.write(ESC_BOLD_ON)
                outputStream.write(ESC_DOUBLE_SIZE)
                outputStream.write("${CurrencyFormatter.formatRupiah(product.sellPrice)}\n".toByteArray(charset))
                outputStream.write(ESC_NORMAL_SIZE)
                outputStream.write(ESC_BOLD_OFF)

                // 5. Barcode Command (GS k)
                // Set barcode height (50 dots) -> GS h 50
                outputStream.write(byteArrayOf(0x1D, 0x68, 50.toByte()))
                // Set barcode width (2) -> GS w 2
                outputStream.write(byteArrayOf(0x1D, 0x77, 2.toByte()))
                // Set HRI characters position below barcode -> GS H 2
                outputStream.write(byteArrayOf(0x1D, 0x48, 2.toByte()))

                val barcodeClean = product.barcode.filter { it.isLetterOrDigit() }
                if (barcodeClean.isNotBlank()) {
                    try {
                        // Standard ESC/POS CODE128 (Format B: GS k 73 n [0x7B 0x42] data)
                        val codeBytes = barcodeClean.toByteArray(charset)
                        val gsK128 = byteArrayOf(0x1D, 0x6B, 73.toByte(), (codeBytes.size + 2).toByte(), 0x7B, 0x42) + codeBytes
                        outputStream.write(gsK128)
                    } catch (e: Exception) {
                        outputStream.write("*${product.barcode}*\n".toByteArray(charset))
                    }
                } else {
                    outputStream.write("*${product.barcode}*\n".toByteArray(charset))
                }

                outputStream.write("\n".toByteArray(charset))
                outputStream.write("Kategori: ${product.category} | Satuan: ${product.unit}\n".toByteArray(charset))
                outputStream.write("$separator\n".toByteArray(charset))
                outputStream.write(byteArrayOf(0x1B, 0x64, 0x02)) // feed 2 lines
            }

            outputStream.flush()
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error printing barcode label: ${e.message}", e)
            Result.failure(Exception("Gagal cetak barcode: ${e.localizedMessage ?: "Koneksi printer gagal"}"))
        } finally {
            try {
                outputStream?.close()
                socket?.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
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
}
