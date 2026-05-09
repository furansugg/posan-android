package com.posan.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val note: String? = null,
    val plnIdPelanggan: String? = null,
    val plnMeterNo: String? = null,
    val plnTarif: String? = null,
    val plnDaya: String? = null,
    val plnNamaLengkap: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val hasPlnData: Boolean
        get() = !plnIdPelanggan.isNullOrBlank() || !plnMeterNo.isNullOrBlank()
}
