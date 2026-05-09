package com.posan.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["code"], unique = true),
        Index(value = ["createdAt"]),
        Index(value = ["userId"]),
        Index(value = ["customerId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val userId: Long? = null,
    val customerId: Long? = null,
    val subtotal: Double,
    val discount: Double = 0.0,
    val taxPercent: Double = 0.0,
    val taxAmount: Double = 0.0,
    val total: Double,
    val paymentMethod: String,
    val paymentReceived: Double = 0.0,
    val change: Double = 0.0,
    val note: String? = null,
    val status: String,
    val createdAt: Long = System.currentTimeMillis()
)
