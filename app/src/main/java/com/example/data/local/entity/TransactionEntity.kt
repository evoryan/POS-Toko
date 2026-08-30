package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNo: String,
    val timestamp: Long = System.currentTimeMillis(),
    val cashierName: String,
    val totalAmount: Double,
    val discount: Double = 0.0,
    val finalAmount: Double,
    val paymentMethod: String, // "TUNAI", "QRIS", "DEBIT/KARTU"
    val amountPaid: Double,
    val changeAmount: Double,
    val notes: String = "",
    val isSynced: Boolean = false
)

@Entity(tableName = "transaction_items")
data class TransactionItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionInvoiceNo: String,
    val productId: Long,
    val productName: String,
    val barcode: String,
    val unitPrice: Double,
    val costPrice: Double = 0.0,
    val quantity: Int,
    val subtotal: Double
)
