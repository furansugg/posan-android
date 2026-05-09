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
    val footer: String = "",
    val extraFields: Map<String, String> = emptyMap(),
    val receiptTitle: String = ""
)

object ReceiptParser {

    private val PRICE_PATTERN = Regex("""[Rr][Pp]\.?\s*[\d.,]+""")
    private val AMOUNT_PATTERN = Regex("""[Rr][Pp]\.?\s*[\d.,]+|[\d.,]+[.,]\d{3}""")
    private val NUMBER_PATTERN = Regex("""[\d.,]+""")
    private val QTY_PRICE_PATTERN = Regex("""(\d+)\s*[xX×]\s*[Rr]?[Pp]?\.?\s*([\d.,]+)""")
    private val DATE_TIME_PATTERN = Regex("""\d{1,2}[/\-]\d{1,2}[/\-]\d{2,4}\s+\d{1,2}[:.]\d{2}(?:[:.]\d{2})?""")
    private val DATE_PATTERN = Regex("""\d{1,2}[/\-]\d{1,2}[/\-]\d{2,4}""")
    private val TIME_PATTERN = Regex("""\d{1,2}[:.]\d{2}(?:[:.]\d{2})?""")
    private val TOKEN_PATTERN = Regex("""\d{4}[-\s]\d{4}[-\s]\d{4}[-\s]\d{2,4}""")
    private val METER_ID_PATTERN = Regex("""\d{11,13}""")

    private data class LabelDef(
        val key: String,
        val patterns: List<String>,
        val valueType: ValueType = ValueType.TEXT
    )

    private enum class ValueType { TEXT, AMOUNT, DATE, TOKEN }

    private val KNOWN_LABELS = listOf(
        LabelDef("storeName", listOf("nama mitra", "nama toko", "merchant"), ValueType.TEXT),
        LabelDef("storeAddress", listOf("alamat mitra", "alamat toko", "alamat"), ValueType.TEXT),
        LabelDef("receiptTitle", listOf("struk pembayaran", "bukti pembayaran", "receipt"), ValueType.TEXT),
        LabelDef("dateTime", listOf("waktu dibuat", "tanggal", "tgl", "date", "waktu"), ValueType.DATE),
        LabelDef("username", listOf("username", "user name"), ValueType.TEXT),
        LabelDef("staff", listOf("staf", "staff", "kasir", "cashier"), ValueType.TEXT),
        LabelDef("orderNo", listOf("no. pesanan", "no pesanan", "no order", "order no"), ValueType.TEXT),
        LabelDef("meterNo", listOf("meter no", "no meter", "no. meter"), ValueType.TEXT),
        LabelDef("idpel", listOf("idpel", "id pel", "id pelanggan"), ValueType.TEXT),
        LabelDef("customerName", listOf("nama pelanggan", "pelanggan", "customer"), ValueType.TEXT),
        LabelDef("name", listOf("^nama$"), ValueType.TEXT),
        LabelDef("tarifDaya", listOf("tarif daya", "tarif/daya", "daya"), ValueType.TEXT),
        LabelDef("noRef", listOf("no. ref", "no ref", "ref no", "referensi"), ValueType.TEXT),
        LabelDef("meterai", listOf("meterai", "materai"), ValueType.AMOUNT),
        LabelDef("ppn", listOf("^ppn$"), ValueType.AMOUNT),
        LabelDef("ppj", listOf("^ppj$"), ValueType.AMOUNT),
        LabelDef("angsuran", listOf("angsuran", "installment"), ValueType.AMOUNT),
        LabelDef("jumlahKwh", listOf("jumlah kwh", "kwh", "jml kwh"), ValueType.TEXT),
        LabelDef("token", listOf("stroom/token", "token listrik", "stroom", "token"), ValueType.TOKEN),
        LabelDef("admin", listOf("admin", "biaya admin"), ValueType.AMOUNT),
        LabelDef("total", listOf("total tagihan", "total bayar", "grand total", "^total$"), ValueType.AMOUNT),
        LabelDef("tagihan", listOf("^tagihan$"), ValueType.AMOUNT),
        LabelDef("subtotal", listOf("subtotal", "sub total"), ValueType.AMOUNT),
        LabelDef("discount", listOf("diskon", "discount", "disc"), ValueType.AMOUNT),
        LabelDef("tax", listOf("pajak", "tax"), ValueType.AMOUNT),
        LabelDef("paymentMethod", listOf("metode bayar", "pembayaran", "payment method"), ValueType.TEXT),
        LabelDef("paid", listOf("bayar", "tunai", "cash", "paid"), ValueType.AMOUNT),
        LabelDef("change", listOf("kembali", "kembalian", "change"), ValueType.AMOUNT)
    )

    fun parse(rawText: String): ParsedReceipt {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return ParsedReceipt()

        val labelValueMap = extractByLabelValueMatching(lines)

        val storeName = labelValueMap["storeName"] ?: ""
        val storeAddress = labelValueMap["storeAddress"] ?: extractAddressFromHeader(lines)
        val receiptTitle = labelValueMap["receiptTitle"] ?: detectReceiptTitle(lines)
        val dateTime = labelValueMap["dateTime"] ?: findDateTime(lines)
        val staff = labelValueMap["staff"] ?: ""
        val orderNo = labelValueMap["orderNo"] ?: ""
        val meterNo = labelValueMap["meterNo"] ?: ""
        val idpel = labelValueMap["idpel"] ?: ""
        val customerName = labelValueMap["customerName"]
            ?: labelValueMap["name"]
            ?: ""
        val tarifDaya = labelValueMap["tarifDaya"] ?: ""
        val noRef = labelValueMap["noRef"] ?: ""
        val tokenValue = labelValueMap["token"] ?: findToken(lines)

        val ppn = parseAmountStr(labelValueMap["ppn"])
        val ppj = parseAmountStr(labelValueMap["ppj"])
        val meterai = parseAmountStr(labelValueMap["meterai"])
        val angsuran = parseAmountStr(labelValueMap["angsuran"])
        val adminFee = parseAmountStr(labelValueMap["admin"])
        val kwh = labelValueMap["jumlahKwh"] ?: ""

        val totalStr = labelValueMap["total"] ?: labelValueMap["tagihan"] ?: ""
        val total = parseAmountStr(totalStr)
        val subtotalVal = parseAmountStr(labelValueMap["subtotal"])
        val discount = parseAmountStr(labelValueMap["discount"])
        val tax = parseAmountStr(labelValueMap["tax"])
        val paid = parseAmountStr(labelValueMap["paid"])
        val change = parseAmountStr(labelValueMap["change"])

        val items = extractItems(lines)

        val footer = extractFooterText(lines)

        val extra = mutableMapOf<String, String>()
        if (orderNo.isNotBlank()) extra["No. Pesanan"] = orderNo
        if (meterNo.isNotBlank()) extra["Meter No"] = meterNo
        if (idpel.isNotBlank()) extra["IDPEL"] = idpel
        if (tarifDaya.isNotBlank()) extra["Tarif/Daya"] = tarifDaya
        if (noRef.isNotBlank()) extra["No. Ref"] = noRef
        if (tokenValue.isNotBlank()) extra["Token"] = tokenValue
        if (kwh.isNotBlank()) extra["Jumlah kWh"] = kwh
        if (ppn > 0) extra["PPN"] = formatRp(ppn)
        if (ppj > 0) extra["PPJ"] = formatRp(ppj)
        if (meterai > 0) extra["Meterai"] = formatRp(meterai)
        if (angsuran > 0) extra["Angsuran"] = formatRp(angsuran)
        if (adminFee > 0) extra["Admin"] = formatRp(adminFee)

        val computedSubtotal = if (subtotalVal > 0) subtotalVal
            else if (items.isNotEmpty()) items.sumOf { it.subtotal }
            else 0.0
        val computedTotal = if (total > 0) total
            else if (computedSubtotal > 0) computedSubtotal - discount + tax
            else 0.0

        return ParsedReceipt(
            storeName = storeName,
            storeAddress = storeAddress,
            transactionCode = orderNo,
            dateTime = dateTime,
            cashier = staff,
            customer = customerName,
            items = items,
            subtotal = computedSubtotal,
            discount = discount,
            tax = tax,
            total = computedTotal,
            paymentMethod = detectPaymentMethod(lines),
            paymentReceived = paid,
            change = change,
            footer = footer,
            extraFields = extra,
            receiptTitle = receiptTitle
        )
    }

    private fun extractByLabelValueMatching(lines: List<String>): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val usedLines = mutableSetOf<Int>()

        for (i in lines.indices) {
            val line = lines[i]
            for (labelDef in KNOWN_LABELS) {
                if (result.containsKey(labelDef.key)) continue
                val matchedLabel = labelDef.patterns.any { pattern ->
                    if (pattern.startsWith("^") && pattern.endsWith("$")) {
                        line.lowercase().trim() == pattern.removePrefix("^").removeSuffix("$")
                    } else {
                        line.lowercase().contains(pattern)
                    }
                }
                if (!matchedLabel) continue

                val inlineValue = extractInlineValue(line, labelDef)
                if (inlineValue != null && inlineValue.isNotBlank()) {
                    result[labelDef.key] = inlineValue
                    usedLines.add(i)
                }
            }
        }

        val labelOnlyLines = mutableListOf<Pair<Int, LabelDef>>()
        for (i in lines.indices) {
            if (usedLines.contains(i)) continue
            val line = lines[i]
            for (labelDef in KNOWN_LABELS) {
                if (result.containsKey(labelDef.key)) continue
                val isLabelOnly = labelDef.patterns.any { pattern ->
                    val p = pattern.removePrefix("^").removeSuffix("$")
                    line.lowercase().trim() == p || line.lowercase().trim().removeSuffix(":") == p
                }
                if (isLabelOnly) {
                    labelOnlyLines.add(Pair(i, labelDef))
                    usedLines.add(i)
                    break
                }
            }
        }

        val valueLines = mutableListOf<Pair<Int, String>>()
        for (i in lines.indices) {
            if (usedLines.contains(i)) continue
            val line = lines[i]
            val isKnownLabel = KNOWN_LABELS.any { labelDef ->
                labelDef.patterns.any { pattern ->
                    val p = pattern.removePrefix("^").removeSuffix("$")
                    line.lowercase().trim() == p || line.lowercase().trim().removeSuffix(":") == p ||
                        line.lowercase().contains(p)
                }
            }
            if (!isKnownLabel) {
                valueLines.add(Pair(i, line))
            }
        }

        for ((labelIdx, labelDef) in labelOnlyLines) {
            val bestValue = findBestValueForLabel(labelDef, labelIdx, valueLines, lines)
            if (bestValue != null) {
                result[labelDef.key] = bestValue.second
                valueLines.removeAll { it.first == bestValue.first }
            }
        }

        return result
    }

    private fun extractInlineValue(line: String, labelDef: LabelDef): String? {
        for (pattern in labelDef.patterns) {
            val p = pattern.removePrefix("^").removeSuffix("$")
            val idx = line.lowercase().indexOf(p)
            if (idx < 0) continue
            val afterLabel = line.substring(idx + p.length).trimStart(':', ' ', '\t')
            if (afterLabel.isNotBlank()) return afterLabel
        }
        return null
    }

    private fun findBestValueForLabel(
        labelDef: LabelDef,
        labelIdx: Int,
        valueLines: List<Pair<Int, String>>,
        allLines: List<String>
    ): Pair<Int, String>? {
        val candidates = valueLines.filter { it.first > labelIdx }
        if (candidates.isEmpty()) return null

        when (labelDef.valueType) {
            ValueType.DATE -> {
                for (c in candidates) {
                    if (DATE_TIME_PATTERN.containsMatchIn(c.second) || DATE_PATTERN.containsMatchIn(c.second)) {
                        val dt = DATE_TIME_PATTERN.find(c.second)?.value
                            ?: run {
                                val d = DATE_PATTERN.find(c.second)?.value ?: ""
                                val t = TIME_PATTERN.find(c.second)?.value ?: ""
                                "$d $t".trim()
                            }
                        return Pair(c.first, dt)
                    }
                }
            }
            ValueType.AMOUNT -> {
                for (c in candidates) {
                    if (AMOUNT_PATTERN.containsMatchIn(c.second) ||
                        c.second.matches(Regex("""[\d.,]+"""))) {
                        return c
                    }
                }
            }
            ValueType.TOKEN -> {
                for (c in candidates) {
                    if (TOKEN_PATTERN.containsMatchIn(c.second)) {
                        return Pair(c.first, TOKEN_PATTERN.find(c.second)!!.value)
                    }
                    if (c.second.length >= 10 && c.second.all { it.isLetterOrDigit() }) {
                        return c
                    }
                }
            }
            ValueType.TEXT -> {
                return candidates.firstOrNull()
            }
        }

        return candidates.firstOrNull()
    }

    private fun extractAddressFromHeader(lines: List<String>): String {
        for (line in lines) {
            val lower = line.lowercase()
            if (lower.startsWith("alamat") || lower.contains("alamat mitra") || lower.contains("alamat toko")) {
                val afterLabel = line.substringAfter(":").trim()
                if (afterLabel.isNotBlank()) {
                    val idx = lines.indexOf(line)
                    val addressParts = mutableListOf(afterLabel)
                    if (idx + 1 < lines.size) {
                        val nextLine = lines[idx + 1]
                        val nextLower = nextLine.lowercase()
                        if (!isLabelLine(nextLower) && !AMOUNT_PATTERN.containsMatchIn(nextLine)) {
                            addressParts.add(nextLine)
                        }
                    }
                    return addressParts.joinToString(", ")
                }
            }
        }
        return ""
    }

    private fun isLabelLine(lower: String): Boolean {
        return KNOWN_LABELS.any { labelDef ->
            labelDef.patterns.any { pattern ->
                val p = pattern.removePrefix("^").removeSuffix("$")
                lower.trim() == p || lower.trim().removeSuffix(":") == p
            }
        }
    }

    private fun detectReceiptTitle(lines: List<String>): String {
        for (line in lines.take(10)) {
            val lower = line.lowercase()
            if (lower.contains("struk pembayaran") || lower.contains("bukti pembayaran") ||
                lower.contains("receipt") || lower.contains("invoice")) {
                return line
            }
        }
        return ""
    }

    private fun findDateTime(lines: List<String>): String {
        for (line in lines) {
            val dtMatch = DATE_TIME_PATTERN.find(line)
            if (dtMatch != null) return dtMatch.value
        }
        for (line in lines) {
            val dateMatch = DATE_PATTERN.find(line)
            if (dateMatch != null) {
                val timeMatch = TIME_PATTERN.find(line)
                val time = timeMatch?.value ?: ""
                return "${dateMatch.value} $time".trim()
            }
        }
        return ""
    }

    private fun findToken(lines: List<String>): String {
        for (line in lines) {
            val match = TOKEN_PATTERN.find(line)
            if (match != null) return match.value
        }
        return ""
    }

    private fun detectPaymentMethod(lines: List<String>): String {
        for (line in lines) {
            val lower = line.lowercase()
            when {
                lower.contains("shopee") -> return "Mitra Shopee"
                lower.contains("tunai") || lower.contains("cash") -> return "Tunai"
                lower.contains("qris") -> return "QRIS"
                lower.contains("debit") -> return "Kartu Debit"
                lower.contains("kredit") || lower.contains("credit") -> return "Kartu Kredit"
                lower.contains("gopay") -> return "GoPay"
                lower.contains("ovo") -> return "OVO"
                lower.contains("dana") -> return "DANA"
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
            if (line.matches(Regex("^[-=]{3,}$"))) {
                inItemSection = !inItemSection
                i++
                continue
            }
            if (!inItemSection) { i++; continue }

            val lower = line.lowercase()
            if (isLabelLine(lower) || lower.contains("total") || lower.contains("subtotal")) {
                i++; continue
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
                }
                i++; continue
            }

            val priceMatch = AMOUNT_PATTERN.find(line)
            if (priceMatch != null) {
                val priceVal = parseNumber(priceMatch.value)
                val namePart = line.substring(0, priceMatch.range.first).trim()
                if (namePart.isNotBlank() && priceVal > 0) {
                    items.add(ParsedReceiptItem(name = namePart, quantity = 1, price = priceVal, subtotal = priceVal))
                }
                i++; continue
            }

            i++
        }
        return items
    }

    private fun extractFooterText(lines: List<String>): String {
        val footerPatterns = listOf("informasi", "hubungi", "call center", "simpan struk",
            "terima kasih", "thank", "bukti pembayaran", "hub pln")
        val footerLines = mutableListOf<String>()
        var inFooter = false
        for (line in lines) {
            val lower = line.lowercase()
            if (!inFooter && footerPatterns.any { lower.contains(it) }) {
                inFooter = true
            }
            if (inFooter) {
                footerLines.add(line)
            }
        }
        return footerLines.joinToString("\n")
    }

    private fun parseAmountStr(text: String?): Double {
        if (text.isNullOrBlank()) return 0.0
        val match = AMOUNT_PATTERN.find(text) ?: NUMBER_PATTERN.find(text) ?: return 0.0
        return parseNumber(match.value)
    }

    private fun parseNumber(text: String): Double {
        val cleaned = text.replace(Regex("[Rr][Pp]\\.?\\s*"), "")
            .replace(".", "")
            .replace(",", ".")
            .trim()
        return cleaned.toDoubleOrNull() ?: 0.0
    }

    private fun formatRp(value: Double): String {
        if (value == 0.0) return "Rp 0"
        val long = value.toLong()
        val formatted = long.toString().reversed().chunked(3).joinToString(".").reversed()
        return "Rp $formatted"
    }
}
