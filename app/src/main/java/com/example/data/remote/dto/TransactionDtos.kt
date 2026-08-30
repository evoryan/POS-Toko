package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateTransactionItemDto(
    @Json(name = "productId") val productId: Long,
    @Json(name = "productName") val productName: String,
    @Json(name = "barcode") val barcode: String,
    @Json(name = "unitPrice") val unitPrice: Double,
    @Json(name = "quantity") val quantity: Int,
    @Json(name = "subtotal") val subtotal: Double
)

@JsonClass(generateAdapter = true)
data class CreateTransactionRequest(
    @Json(name = "invoiceNo") val invoiceNo: String,
    @Json(name = "cashierName") val cashierName: String,
    @Json(name = "totalAmount") val totalAmount: Double,
    @Json(name = "discount") val discount: Double = 0.0,
    @Json(name = "finalAmount") val finalAmount: Double,
    @Json(name = "paymentMethod") val paymentMethod: String,
    @Json(name = "amountPaid") val amountPaid: Double,
    @Json(name = "changeAmount") val changeAmount: Double,
    @Json(name = "notes") val notes: String = "",
    @Json(name = "items") val items: List<CreateTransactionItemDto>
)

@JsonClass(generateAdapter = true)
data class TransactionResponse(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "message") val message: String? = null,
    @Json(name = "invoiceNo") val invoiceNo: String? = null
)

@JsonClass(generateAdapter = true)
data class DailyReportDto(
    @Json(name = "date") val date: String,
    @Json(name = "totalRevenue") val totalRevenue: Double = 0.0,
    @Json(name = "totalProfit") val totalProfit: Double = 0.0,
    @Json(name = "totalTransactions") val totalTransactions: Int = 0,
    @Json(name = "totalItemsSold") val totalItemsSold: Int = 0
)
