package com.posan.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    indices = [Index(value = ["sku"], unique = true), Index(value = ["barcode"]), Index(value = ["categoryId"])],
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sku: String,
    val barcode: String? = null,
    val price: Double,
    val cost: Double = 0.0,
    val stock: Int = 0,
    val unit: String = "pcs",
    val categoryId: Long? = null,
    val active: Boolean = true,
    val imagePath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
