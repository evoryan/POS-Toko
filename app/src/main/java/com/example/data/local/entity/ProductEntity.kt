package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val remoteId: Long? = null,
    val barcode: String,
    val name: String,
    val category: String,
    val sellPrice: Double,
    val costPrice: Double,
    val stock: Int,
    val minStock: Int = 5,
    val unit: String = "pcs",
    val imageUrl: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
