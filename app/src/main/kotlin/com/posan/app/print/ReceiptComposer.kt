package com.posan.app.print

import com.posan.app.data.local.entity.PrintSettingsEntity
import com.posan.app.data.local.entity.TransactionEntity
import com.posan.app.data.local.entity.TransactionItemEntity
import com.posan.app.domain.model.PaperWidth
import com.posan.app.domain.model.PaymentMethod
import com.posan.app.domain.model.PrintAlignment
import com.posan.app.util.Format
import javax.inject.Inject
import javax.inject.Singleton

data class ReceiptInput(
    val transaction: TransactionEntity,
    val items: List<TransactionItemEntity>,
    val cashierName: String? = null,
    val customerName: String? = null
)

@Singleton
class ReceiptComposer @Inject constructor() {

    private fun width(settings: PrintSettingsEntity): Int {
        val paper = PaperWidth.fromName(settings.paperWidth)
        return if (settings.bodyFontSmall) paper.charsSmall else paper.charsNormal
    }

    fun composePlain(input: ReceiptInput, settings: PrintSettingsEntity): String {
        val w = width(settings)
        val sb = StringBuilder()
        val align = PrintAlignment.fromName(settings.titleAlignment)

        if (settings.storeName.isNotBlank()) sb.appendLine(alignText(settings.storeName, w, align))
        if (settings.storeAddress.isNotBlank()) {
            settings.storeAddress.split('\n').forEach {
                sb.appendLine(alignText(it.trim(), w, align))
            }
        }
        if (settings.storePhone.isNotBlank()) sb.appendLine(alignText("Telp: ${settings.storePhone}", w, align))
        if (settings.headerText.isNotBlank()) {
            sb.appendLine(alignText(settings.headerText, w, align))
        }
        sb.appendLine("=".repeat(w))
        sb.appendLine("No  : ${input.transaction.code}")
        sb.appendLine("Tgl : ${Format.datetime(input.transaction.createdAt)}")
        if (settings.showCashier && !input.cashierName.isNullOrBlank()) {
            sb.appendLine("Kasir: ${input.cashierName}")
        }
        if (settings.showCustomer && !input.customerName.isNullOrBlank()) {
            sb.appendLine("Plg  : ${input.customerName}")
        }
        sb.appendLine("-".repeat(w))
        input.items.forEach { item ->
            val nameLine = if (settings.showItemSku && !item.sku.isNullOrBlank()) "${item.name} [${item.sku}]" else item.name
            sb.appendLine(nameLine)
            val qtyPrice = "${item.quantity} x ${Format.number(item.price)}"
            val sub = Format.number(item.subtotal)
            sb.appendLine(twoColumn("  $qtyPrice", sub, w))
            if (item.discount > 0) {
                sb.appendLine(twoColumn("  Diskon", "-${Format.number(item.discount)}", w))
            }
        }
        sb.appendLine("-".repeat(w))
        sb.appendLine(twoColumn("Subtotal", Format.number(input.transaction.subtotal), w))
        if (input.transaction.discount > 0) {
            sb.appendLine(twoColumn("Diskon", "-${Format.number(input.transaction.discount)}", w))
        }
        if (input.transaction.taxPercent > 0) {
            sb.appendLine(
                twoColumn(
                    "Pajak ${Format.number(input.transaction.taxPercent)}%",
                    Format.number(input.transaction.taxAmount),
                    w
                )
            )
        }
        sb.appendLine("=".repeat(w))
        sb.appendLine(twoColumn("TOTAL", "${settings.currencySymbol} ${Format.number(input.transaction.total)}", w))
        sb.appendLine(
            twoColumn(
                "Bayar (${PaymentMethod.fromName(input.transaction.paymentMethod).displayName})",
                Format.number(input.transaction.paymentReceived),
                w
            )
        )
        if (input.transaction.paymentMethod == PaymentMethod.CASH.name) {
            sb.appendLine(twoColumn("Kembali", Format.number(input.transaction.change), w))
        }
        sb.appendLine("=".repeat(w))
        if (settings.footerText.isNotBlank()) {
            settings.footerText.split('\n').forEach {
                sb.appendLine(alignText(it.trim(), w, align))
            }
        }
        return sb.toString()
    }

    fun composeEscPos(input: ReceiptInput, settings: PrintSettingsEntity): String {
        val align = PrintAlignment.fromName(settings.titleAlignment)
        val alignTag = when (align) {
            PrintAlignment.LEFT -> "L"
            PrintAlignment.CENTER -> "C"
            PrintAlignment.RIGHT -> "R"
        }
        val w = width(settings)
        val bodyTag = "L"
        val sb = StringBuilder()

        sb.appendLine("[$alignTag]<u><b>${settings.storeName}</b></u>")
        if (settings.storeAddress.isNotBlank()) {
            settings.storeAddress.split('\n').forEach {
                sb.appendLine("[$alignTag]${it.trim()}")
            }
        }
        if (settings.storePhone.isNotBlank()) sb.appendLine("[$alignTag]Telp: ${settings.storePhone}")
        if (settings.headerText.isNotBlank()) sb.appendLine("[$alignTag]${settings.headerText}")
        sb.appendLine("[$bodyTag]${"-".repeat(w)}")
        sb.appendLine("[$bodyTag]No  : ${input.transaction.code}")
        sb.appendLine("[$bodyTag]Tgl : ${Format.datetime(input.transaction.createdAt)}")
        if (settings.showCashier && !input.cashierName.isNullOrBlank()) sb.appendLine("[$bodyTag]Kasir: ${input.cashierName}")
        if (settings.showCustomer && !input.customerName.isNullOrBlank()) sb.appendLine("[$bodyTag]Plg  : ${input.customerName}")
        sb.appendLine("[$bodyTag]${"-".repeat(w)}")
        input.items.forEach { item ->
            val nameLine = if (settings.showItemSku && !item.sku.isNullOrBlank()) "${item.name} [${item.sku}]" else item.name
            sb.appendLine("[$bodyTag]$nameLine")
            sb.appendLine("[$bodyTag]${twoColumn("  ${item.quantity} x ${Format.number(item.price)}", Format.number(item.subtotal), w)}")
            if (item.discount > 0) sb.appendLine("[$bodyTag]${twoColumn("  Diskon", "-${Format.number(item.discount)}", w)}")
        }
        sb.appendLine("[$bodyTag]${"-".repeat(w)}")
        sb.appendLine("[$bodyTag]${twoColumn("Subtotal", Format.number(input.transaction.subtotal), w)}")
        if (input.transaction.discount > 0) sb.appendLine("[$bodyTag]${twoColumn("Diskon", "-${Format.number(input.transaction.discount)}", w)}")
        if (input.transaction.taxPercent > 0) sb.appendLine("[$bodyTag]${twoColumn("Pajak ${Format.number(input.transaction.taxPercent)}%", Format.number(input.transaction.taxAmount), w)}")
        sb.appendLine("[$bodyTag]${"=".repeat(w)}")
        sb.appendLine("[$bodyTag]<b>${twoColumn("TOTAL", "${settings.currencySymbol} ${Format.number(input.transaction.total)}", w)}</b>")
        sb.appendLine("[$bodyTag]${twoColumn("Bayar (${PaymentMethod.fromName(input.transaction.paymentMethod).displayName})", Format.number(input.transaction.paymentReceived), w)}")
        if (input.transaction.paymentMethod == PaymentMethod.CASH.name) {
            sb.appendLine("[$bodyTag]${twoColumn("Kembali", Format.number(input.transaction.change), w)}")
        }
        sb.appendLine("[$bodyTag]${"=".repeat(w)}")
        if (settings.footerText.isNotBlank()) {
            settings.footerText.split('\n').forEach { sb.appendLine("[$alignTag]${it.trim()}") }
        }
        sb.appendLine("[C]\n")
        return sb.toString()
    }

    private fun alignText(text: String, width: Int, alignment: PrintAlignment): String {
        if (text.length >= width) return text
        return when (alignment) {
            PrintAlignment.LEFT -> text
            PrintAlignment.RIGHT -> " ".repeat(width - text.length) + text
            PrintAlignment.CENTER -> {
                val pad = (width - text.length) / 2
                " ".repeat(pad) + text
            }
        }
    }

    private fun twoColumn(left: String, right: String, width: Int): String {
        val maxLeft = (width - right.length - 1).coerceAtLeast(1)
        val l = if (left.length > maxLeft) left.take(maxLeft) else left
        val pad = (width - l.length - right.length).coerceAtLeast(1)
        return l + " ".repeat(pad) + right
    }
}
