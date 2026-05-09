package com.posan.app.ui.textrecognition

data class ParsedReceiptItem(
    val name: String,
    val quantity: Int = 1,
    val price: Double = 0.0,
    val subtotal: Double = 0.0
)

data class ParsedReceipt(
    val storeName: String = "",
    val storeAddress: String = "",
    val storePhone: String = "",
    val transactionCode: String = "",
    val dateTime: String = "",
    val cashier: String = "",
    val customer: String = "",
    val items: List<ParsedReceiptItem> = emptyList(),
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val tax: Double = 0.0,
    val total: Double = 0.0,
    val paymentMethod: String = "",
    val paymentReceived: Double = 0.0,
    val change: Double = 0.0,
    val footer: String = ""
)

object ReceiptParser {

    private val PRICE_PATTERN = Regex("""[Rr][Pp]\.?\s*[\d.,]+|[\d.,]+[.,]\d{3}""")
    private val NUMBER_PATTERN = Regex("""[\d.,]+""")
    private val QTY_PRICE_PATTERN = Regex("""(\d+)\s*[xX×]\s*[Rr]?[Pp]?\.?\s*([\d.,]+)""")
    private val PHONE_PATTERN = Regex("""(?:Telp|Tel|HP|Phone|Tlp)[.:)}\s]*\s*([\d\s\-+()]+)""", RegexOption.IGNORE_CASE)
    private val DATE_PATTERN = Regex("""\d{1,2}[/\-]\d{1,2}[/\-]\d{2,4}|\d{1,2}\s+\w+\s+\d{4}""")
    private val TIME_PATTERN = Regex("""\d{1,2}[:.]\d{2}(?:[:.]\d{2})?""")

    private val SKIP_KEYWORDS = listOf(
        "subtotal", "sub total", "total", "diskon", "discount", "pajak", "tax", "ppn",
        "tunai", "cash", "kembali", "change", "bayar", "payment", "qris",
        "kartu", "card", "debit", "kredit", "credit", "kembalian",
        "terima kasih", "thank", "selamat", "datang", "kunjungan",
        "telp", "alamat", "kasir", "pelanggan", "no.", "tgl", "tanggal"
    )

    fun parse(rawText: String): ParsedReceipt {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return ParsedReceipt()

        val storeName = extractStoreName(lines)
        val storeAddress = extractStoreAddress(lines)
        val storePhone = extractPhone(lines)
        val transactionCode = extractTransactionCode(lines)
        val dateTime = extractDateTime(lines)
        val cashier = extractLabeledValue(lines, listOf("kasir", "cashier"))
        val customer = extractLabeledValue(lines, listOf("pelanggan", "plg", "customer", "member"))
        val items = extractItems(lines)
        val subtotal = extractLabeledAmount(lines, listOf("subtotal", "sub total", "sub-total"))
        val discount = extractLabeledAmount(lines, listOf("diskon", "discount", "disc"))
        val tax = extractLabeledAmount(lines, listOf("pajak", "tax", "ppn"))
        val total = extractLabeledAmount(lines, listOf("total", "grand total", "total bayar"))
        val paymentMethod = extractPaymentMethod(lines)
        val paymentReceived = extractLabeledAmount(lines, listOf("bayar", "tunai", "cash", "payment", "paid"))
        val change = extractLabeledAmount(lines, listOf("kembali", "kembalian", "change"))
        val footer = extractFooter(lines)

        val computedSubtotal = if (subtotal > 0) subtotal
        else if (items.isNotEmpty()) items.sumOf { it.subtotal }
        else 0.0

        val computedTotal = if (total > 0) total else computedSubtotal - discount + tax

        return ParsedReceipt(
            storeName = storeName,
            storeAddress = storeAddress,
            storePhone = storePhone,
            transactionCode = transactionCode,
            dateTime = dateTime,
            cashier = cashier,
            customer = customer,
            items = items,
            subtotal = computedSubtotal,
            discount = discount,
            tax = tax,
            total = computedTotal,
            paymentMethod = paymentMethod,
            paymentReceived = paymentReceived,
            change = change,
            footer = footer
        )
    }

    private fun extractStoreName(lines: List<String>): String {
        for (i in 0 until minOf(3, lines.size)) {
            val line = lines[i]
            if (line.length > 2 && !PRICE_PATTERN.containsMatchIn(line) &&
                !DATE_PATTERN.containsMatchIn(line) &&
                !line.startsWith("=") && !line.startsWith("-") &&
                !PHONE_PATTERN.containsMatchIn(line)
            ) {
                return line
            }
        }
        return ""
    }

    private fun extractStoreAddress(lines: List<String>): String {
        val addressLines = mutableListOf<String>()
        val storeNameIdx = 0
        for (i in (storeNameIdx + 1) until minOf(5, lines.size)) {
            val line = lines[i]
            if (line.startsWith("=") || line.startsWith("-")) break
            if (PHONE_PATTERN.containsMatchIn(line)) continue
            if (DATE_PATTERN.containsMatchIn(line)) break
            if (line.lowercase().let { l ->
                    l.startsWith("no") || l.startsWith("tgl") || l.startsWith("kasir")
                }) break
            if (!PRICE_PATTERN.containsMatchIn(line) && line.length > 3) {
                addressLines.add(line)
            }
        }
        return addressLines.joinToString(", ")
    }

    private fun extractPhone(lines: List<String>): String {
        for (line in lines) {
            PHONE_PATTERN.find(line)?.let {
                return it.groupValues[1].trim()
            }
        }
        return ""
    }

    private fun extractTransactionCode(lines: List<String>): String {
        for (line in lines) {
            val lower = line.lowercase()
            if (lower.startsWith("no") || lower.contains("invoice") || lower.contains("trx")) {
                val parts = line.split(":", "=", " ").filter { it.isNotBlank() }
                if (parts.size >= 2) {
                    return parts.drop(1).joinToString(" ").trim()
                }
            }
        }
        return ""
    }

    private fun extractDateTime(lines: List<String>): String {
        for (line in lines) {
            val dateMatch = DATE_PATTERN.find(line)
            val timeMatch = TIME_PATTERN.find(line)
            if (dateMatch != null) {
                val date = dateMatch.value
                val time = timeMatch?.value ?: ""
                return "$date $time".trim()
            }
        }
        return ""
    }

    private fun extractLabeledValue(lines: List<String>, labels: List<String>): String {
        for (line in lines) {
            val lower = line.lowercase()
            for (label in labels) {
                if (lower.contains(label)) {
                    val parts = line.split(":", "=").filter { it.isNotBlank() }
                    if (parts.size >= 2) {
                        return parts.drop(1).joinToString(":").trim()
                    }
                }
            }
        }
        return ""
    }

    private fun extractItems(lines: List<String>): List<ParsedReceiptItem> {
        val items = mutableListOf<ParsedReceiptItem>()
        var inItemSection = false
        var i = 0

        while (i < lines.size) {
            val line = lines[i]
            val lower = line.lowercase()

            if (line.matches(Regex("^[-=]+$"))) {
                inItemSection = !inItemSection
                i++
                continue
            }

            if (!inItemSection) {
                i++
                continue
            }

            if (SKIP_KEYWORDS.any { lower.contains(it) }) {
                i++
                continue
            }

            val qtyMatch = QTY_PRICE_PATTERN.find(line)
            if (qtyMatch != null) {
                val qty = qtyMatch.groupValues[1].toIntOrNull() ?: 1
                val price = parseNumber(qtyMatch.groupValues[2])
                val sub = qty * price
                val namePart = line.substring(0, qtyMatch.range.first).trim()

                if (namePart.isNotBlank()) {
                    items.add(ParsedReceiptItem(name = namePart, quantity = qty, price = price, subtotal = sub))
                } else if (items.isNotEmpty()) {
                    val last = items.last()
                    items[items.lastIndex] = last.copy(quantity = qty, price = price, subtotal = sub)
                } else if (i > 0) {
                    val prevLine = lines[i - 1].trim()
                    if (prevLine.isNotBlank() && !prevLine.matches(Regex("^[-=]+$"))) {
                        items.add(ParsedReceiptItem(name = prevLine, quantity = qty, price = price, subtotal = sub))
                    }
                }
                i++
                continue
            }

            val priceMatch = PRICE_PATTERN.find(line)
            if (priceMatch != null) {
                val priceVal = parseNumber(priceMatch.value)
                val namePart = line.substring(0, priceMatch.range.first).trim()
                if (namePart.isNotBlank() && priceVal > 0) {
                    items.add(ParsedReceiptItem(name = namePart, quantity = 1, price = priceVal, subtotal = priceVal))
                }
                i++
                continue
            }

            if (line.length > 2 && !line.matches(Regex("^\\d+$"))) {
                if (i + 1 < lines.size) {
                    val nextLine = lines[i + 1]
                    val nextQty = QTY_PRICE_PATTERN.find(nextLine)
                    val nextPrice = PRICE_PATTERN.find(nextLine)
                    if (nextQty != null || nextPrice != null) {
                        i++
                        continue
                    }
                }
                items.add(ParsedReceiptItem(name = line, quantity = 1))
            }

            i++
        }

        return items
    }

    private fun extractLabeledAmount(lines: List<String>, labels: List<String>): Double {
        for (line in lines) {
            val lower = line.lowercase()
            for (label in labels) {
                if (lower.contains(label)) {
                    val priceMatch = PRICE_PATTERN.find(line)
                    if (priceMatch != null) {
                        return parseNumber(priceMatch.value)
                    }
                    val afterLabel = line.substringAfter(label, "")
                        .substringAfter(":", "")
                        .substringAfter("=", "")
                        .trim()
                    val numMatch = NUMBER_PATTERN.find(afterLabel)
                    if (numMatch != null) {
                        return parseNumber(numMatch.value)
                    }
                }
            }
        }
        return 0.0
    }

    private fun extractPaymentMethod(lines: List<String>): String {
        for (line in lines) {
            val lower = line.lowercase()
            when {
                lower.contains("tunai") || lower.contains("cash") -> return "Tunai"
                lower.contains("qris") -> return "QRIS"
                lower.contains("debit") -> return "Kartu Debit"
                lower.contains("kredit") || lower.contains("credit") -> return "Kartu Kredit"
                lower.contains("kartu") || lower.contains("card") -> return "Kartu"
            }
        }
        return ""
    }

    private fun extractFooter(lines: List<String>): String {
        val footerLines = mutableListOf<String>()
        var foundLastSeparator = false
        for (i in lines.indices.reversed()) {
            val line = lines[i]
            if (line.matches(Regex("^[-=]+$"))) {
                foundLastSeparator = true
                break
            }
            if (!PRICE_PATTERN.containsMatchIn(line) && line.length > 3) {
                footerLines.add(0, line)
            }
        }
        return if (foundLastSeparator) footerLines.joinToString("\n") else ""
    }

    private fun parseNumber(text: String): Double {
        val cleaned = text.replace(Regex("[Rr][Pp]\\.?\\s*"), "")
            .replace(".", "")
            .replace(",", ".")
            .trim()
        return cleaned.toDoubleOrNull() ?: 0.0
    }
}
