package com.posan.app.ui.textrecognition

import com.posan.app.util.Format

object ReceiptFormatter {

    fun format(receipt: ParsedReceipt, width: Int = 32): String {
        val sb = StringBuilder()

        if (receipt.storeName.isNotBlank()) {
            sb.appendLine(center(receipt.storeName, width))
        }
        if (receipt.storeAddress.isNotBlank()) {
            receipt.storeAddress.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach {
                sb.appendLine(center(it, width))
            }
        }
        if (receipt.storePhone.isNotBlank()) {
            sb.appendLine(center("Telp: ${receipt.storePhone}", width))
        }

        sb.appendLine("=".repeat(width))

        if (receipt.transactionCode.isNotBlank()) {
            sb.appendLine("No  : ${receipt.transactionCode}")
        }
        if (receipt.dateTime.isNotBlank()) {
            sb.appendLine("Tgl : ${receipt.dateTime}")
        }
        if (receipt.cashier.isNotBlank()) {
            sb.appendLine("Kasir: ${receipt.cashier}")
        }
        if (receipt.customer.isNotBlank()) {
            sb.appendLine("Plg  : ${receipt.customer}")
        }

        sb.appendLine("-".repeat(width))

        if (receipt.items.isNotEmpty()) {
            for (item in receipt.items) {
                sb.appendLine(item.name)
                if (item.quantity > 0 && item.price > 0) {
                    val qtyPrice = "  ${item.quantity} x ${formatNumber(item.price)}"
                    val sub = formatNumber(item.subtotal)
                    sb.appendLine(twoColumn(qtyPrice, sub, width))
                } else if (item.subtotal > 0) {
                    sb.appendLine(twoColumn("  ", formatNumber(item.subtotal), width))
                }
            }
            sb.appendLine("-".repeat(width))
        }

        if (receipt.subtotal > 0) {
            sb.appendLine(twoColumn("Subtotal", formatNumber(receipt.subtotal), width))
        }
        if (receipt.discount > 0) {
            sb.appendLine(twoColumn("Diskon", "-${formatNumber(receipt.discount)}", width))
        }
        if (receipt.tax > 0) {
            sb.appendLine(twoColumn("Pajak", formatNumber(receipt.tax), width))
        }

        sb.appendLine("=".repeat(width))

        if (receipt.total > 0) {
            sb.appendLine(twoColumn("TOTAL", "Rp ${formatNumber(receipt.total)}", width))
        }
        if (receipt.paymentMethod.isNotBlank() && receipt.paymentReceived > 0) {
            sb.appendLine(twoColumn("Bayar (${receipt.paymentMethod})", formatNumber(receipt.paymentReceived), width))
        }
        if (receipt.change > 0) {
            sb.appendLine(twoColumn("Kembali", formatNumber(receipt.change), width))
        }

        sb.appendLine("=".repeat(width))

        if (receipt.footer.isNotBlank()) {
            receipt.footer.split("\n").forEach {
                sb.appendLine(center(it.trim(), width))
            }
        } else {
            sb.appendLine(center("Terima kasih", width))
            sb.appendLine(center("atas kunjungan Anda", width))
        }

        return sb.toString()
    }

    private fun formatNumber(value: Double): String {
        return Format.number(value)
    }

    private fun center(text: String, width: Int): String {
        if (text.length >= width) return text
        val pad = (width - text.length) / 2
        return " ".repeat(pad) + text
    }

    private fun twoColumn(left: String, right: String, width: Int): String {
        val maxLeft = (width - right.length - 1).coerceAtLeast(1)
        val l = if (left.length > maxLeft) left.take(maxLeft) else left
        val pad = (width - l.length - right.length).coerceAtLeast(1)
        return l + " ".repeat(pad) + right
    }
}
