package com.posan.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Static breakdown for a PLN token purchase, keyed by (tarif × daya × nominal).
 * Lets the cashier pick a customer + nominal and have all amounts pre-filled
 * without having to re-derive them from a screenshot every time.
 *
 * `nominalRupiah` is the total the customer pays in rupiah (e.g. 20000, 50000, 100000).
 * `kwh` is stored as Double because PLN tokens commonly have one decimal (e.g. 30.8 kWh).
 */
@Entity(
    tableName = "pln_token_templates",
    indices = [
        Index(value = ["tarif", "daya", "nominalRupiah"], unique = true),
        Index(value = ["nominalRupiah"])
    ]
)
data class PlnTokenTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tarif: String,
    val daya: String,
    val nominalRupiah: Int,
    val rpStroom: Double = 0.0,
    val adminFee: Double = 0.0,
    val materai: Double = 0.0,
    val ppn: Double = 0.0,
    val ppj: Double = 0.0,
    val kwh: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis()
)
