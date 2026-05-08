package com.posan.app.domain.model

enum class UserRole(val displayName: String) {
    ADMIN("Admin"),
    KASIR("Kasir");

    companion object {
        fun fromName(name: String): UserRole = entries.firstOrNull { it.name == name } ?: KASIR
    }
}

enum class PaymentMethod(val displayName: String) {
    CASH("Tunai"),
    QRIS("QRIS"),
    CARD("Kartu Debit/Kredit");

    companion object {
        fun fromName(name: String): PaymentMethod = entries.firstOrNull { it.name == name } ?: CASH
    }
}

enum class PrintAlignment(val displayName: String) {
    LEFT("Kiri"),
    CENTER("Tengah"),
    RIGHT("Kanan");

    companion object {
        fun fromName(name: String): PrintAlignment = entries.firstOrNull { it.name == name } ?: CENTER
    }
}

enum class PaperWidth(val displayName: String, val charsNormal: Int, val charsSmall: Int) {
    MM_58("58 mm", 32, 42),
    MM_80("80 mm", 48, 64);

    companion object {
        fun fromName(name: String): PaperWidth = entries.firstOrNull { it.name == name } ?: MM_58
    }
}

enum class StockMovementType(val displayName: String) {
    IN("Masuk"),
    OUT("Keluar"),
    ADJUSTMENT("Penyesuaian"),
    SALE("Penjualan");

    companion object {
        fun fromName(name: String): StockMovementType = entries.firstOrNull { it.name == name } ?: ADJUSTMENT
    }
}

enum class TransactionStatus(val displayName: String) {
    PAID("Lunas"),
    VOID("Batal");

    companion object {
        fun fromName(name: String): TransactionStatus = entries.firstOrNull { it.name == name } ?: PAID
    }
}
