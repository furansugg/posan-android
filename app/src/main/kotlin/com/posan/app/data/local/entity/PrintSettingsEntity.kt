package com.posan.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "print_settings")
data class PrintSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val storeName: String = "Toko Saya",
    val storeAddress: String = "",
    val storePhone: String = "",
    val headerText: String = "",
    val footerText: String = "Terima kasih atas kunjungan Anda",
    val logoPath: String? = null,
    val showLogo: Boolean = false,
    val showCashier: Boolean = true,
    val showCustomer: Boolean = true,
    val showItemSku: Boolean = false,
    val paperWidth: String = "MM_58",
    val titleAlignment: String = "CENTER",
    val titleBold: Boolean = true,
    val titleDoubleSize: Boolean = true,
    val bodyFontSmall: Boolean = false,
    val printCopies: Int = 1,
    val cutPaper: Boolean = true,
    val mmFeedBeforeCut: Int = 5,
    val openCashDrawer: Boolean = false,
    val savedDeviceAddress: String? = null,
    val savedDeviceName: String? = null,
    val taxPercentDefault: Double = 0.0,
    val currencySymbol: String = "Rp",
    val updatedAt: Long = System.currentTimeMillis()
)
