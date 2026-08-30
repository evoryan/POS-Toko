package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ProductDto(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "barcode") val barcode: String,
    @Json(name = "name") val name: String,
    @Json(name = "category") val category: String,
    @Json(name = "sellPrice") val sellPrice: Double,
    @Json(name = "costPrice") val costPrice: Double? = 0.0,
    @Json(name = "stock") val stock: Int,
    @Json(name = "minStock") val minStock: Int? = 5,
    @Json(name = "unit") val unit: String? = "pcs",
    @Json(name = "imageUrl") val imageUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class ProductResponse(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "data") val data: List<ProductDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SingleProductResponse(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "data") val data: ProductDto? = null,
    @Json(name = "message") val message: String? = null
)
