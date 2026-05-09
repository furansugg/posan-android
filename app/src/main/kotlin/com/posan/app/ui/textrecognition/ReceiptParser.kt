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

/**
 * Receipt parser that handles two-column OCR output.
 *
 * OCR on two-column receipts reads the left column (labels) top-to-bottom
 * first, then the right column (values) top-to-bottom, producing output
 * where all labels appear before all values.
 *
 * Strategy:
 * 1. Extract inline values (lines with "Label: Value"), skip label-only lines
 * 2. Search entire text for unique-pattern values (dates, tokens, IDs)
 * 3. Find total amount separately (last/largest Rp value)
 * 4. Match remaining amount labels to amount values by positional order
 * 5. Find kWh after amounts are consumed (to avoid kWh/amount confusion)
 */
object ReceiptParser {

    private val AMOUNT_PATTERN = Regex("""[Rr][Pp]\.?\s*[\d.,]+""")
    // Bare amounts must have comma + exactly 2 decimal digits (like "0,00")
    // This excludes bare integers (like "1422") and kWh values (like "30,8")
    private val BARE_AMOUNT_PATTERN = Regex("""^\d[\d.]*[,]\d{2}$""")
    private val QTY_PRICE_PATTERN = Regex("""(\d+)\s*[xX×]\s*[Rr]?[Pp]?\.?\s*([\d.,]+)""")
    private val DATE_TIME_PATTERN = Regex("""\d{1,2}[/\-]\d{1,2}[/\-]\d{2,4}\s+\d{1,2}[:.]\d{2}(?:[:.]\d{2})?""")
    private val DATE_PATTERN = Regex("""\d{1,2}[/\-]\d{1,2}[/\-]\d{2,4}""")
    private val TIME_PATTERN = Regex("""\d{1,2}[:.]\d{2}(?:[:.]\d{2})?""")
    private val TOKEN_PATTERN = Regex("""\d{4}[-\s]\d{4}[-\s]\d{4}[-\s]\d{2,4}""")
    private val ORDER_NO_PATTERN = Regex("""[A-Z]{2,5}\d{5,}""")
    private val TARIF_DAYA_PATTERN = Regex("""R\d[/\\]\d+\s*VA""", RegexOption.IGNORE_CASE)
    private val REF_NO_PATTERN = Regex("""^[A-Z0-9]{10,}\s*[A-Z0-9]*$""")
    private val ALLCAPS_NAME_PATTERN = Regex("""^[A-Z\s]{3,}$""")
    private val PHONE_PATTERN = Regex("""(?:Telp|Tel|HP|Phone|Tlp)[.:)}\s]*\s*([\d\s\-+()]+)""", RegexOption.IGNORE_CASE)
    private val KWH_PATTERN = Regex("""^\d+[.,]\d{1}$""")

    private val LABEL_PATTERNS = mapOf(
        "storeName" to listOf("nama mitra", "nama toko", "merchant"),
        "storeAddress" to listOf("alamat mitra", "alamat toko", "alamat"),
        "receiptTitle" to listOf("struk pembayaran", "bukti pembayaran", "receipt"),
        "dateTime" to listOf("waktu dibuat", "tanggal", "tgl", "date", "waktu"),
        "username" to listOf("username", "user name"),
        "staff" to listOf("staf", "staff", "kasir", "cashier"),
        "orderNo" to listOf("no. pesanan", "no pesanan", "no order", "order no"),
        "meterNo" to listOf("meter no", "no meter", "no. meter"),
        "idpel" to listOf("idpel", "id pel", "id pelanggan"),
        "customerName" to listOf("nama pelanggan", "pelanggan", "customer"),
        "name" to listOf("nama"),
        "tarifDaya" to listOf("tarif daya", "tarif/daya"),
        "noRef" to listOf("no. ref", "no ref", "ref no", "referensi"),
        "token" to listOf("stroom/token", "token listrik", "stroom", "token"),
        "jumlahKwh" to listOf("jumlah kwh", "jml kwh")
    )

    private val AMOUNT_LABELS = listOf(
        "meterai" to listOf("meterai", "materai"),
        "ppn" to listOf("ppn"),
        "ppj" to listOf("ppj"),
        "angsuran" to listOf("angsuran", "installment"),
        "stroom" to listOf("stroom/token", "stroom", "token"),
        "admin" to listOf("admin", "biaya admin"),
        "subtotal" to listOf("subtotal", "sub total"),
        "discount" to listOf("diskon", "discount", "disc"),
        "tax" to listOf("pajak", "tax"),
        "paid" to listOf("bayar", "tunai", "cash", "paid"),
        "change" to listOf("kembali", "kembalian", "change")
    )

    private val TOTAL_PATTERNS = listOf("total tagihan", "total bayar", "grand total", "total", "tagihan")

    private val FOOTER_TRIGGERS = listOf("informasi", "hubungi", "call center", "simpan struk",
        "terima kasih", "thank", "bukti pembayaran", "hub pln")

    /**
     * Collect all label pattern strings for exact-match checks.
     */
    private val ALL_LABEL_STRINGS: Set<String> by lazy {
        val set = mutableSetOf<String>()
        for ((_, patterns) in LABEL_PATTERNS) set.addAll(patterns)
        for ((_, patterns) in AMOUNT_LABELS) set.addAll(patterns)
        set.addAll(TOTAL_PATTERNS)
        set
    }

    fun parse(rawText: String): ParsedReceipt {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return ParsedReceipt()

        val consumed = mutableSetOf<Int>()
        val result = mutableMapOf<String, String>()

        // Phase 1: Extract inline "Label: Value" pairs (skip label-only lines)
        extractInlineValues(lines, result, consumed)

        // Phase 2: Search entire text for unique-pattern values
        findByPattern(lines, result, consumed)

        // Phase 3: Find total amount (last Rp value heuristic)
        findTotalAmount(lines, result, consumed)

        // Phase 4: Match remaining amount labels to values by positional order
        matchAmountsPositionally(lines, result, consumed)

        // Phase 5: Find kWh after amounts are consumed
        findKwh(lines, result, consumed)

        // Build result
        val storeName = result["storeName"] ?: ""
        val storeAddress = result["storeAddress"] ?: extractAddressFromHeader(lines)
        val receiptTitle = result["receiptTitle"] ?: detectReceiptTitle(lines)
        val dateTime = result["dateTime"] ?: ""
        val staff = result["staff"] ?: ""
        val orderNo = result["orderNo"] ?: ""
        val meterNo = result["meterNo"] ?: ""
        val idpel = result["idpel"] ?: ""
        val customerName = result["customerName"] ?: result["name"] ?: ""
        val tarifDaya = result["tarifDaya"] ?: ""
        val noRef = result["noRef"] ?: ""
        val tokenValue = result["token"] ?: ""
        val kwh = result["jumlahKwh"] ?: ""

        val ppn = parseAmountStr(result["ppn"])
        val ppj = parseAmountStr(result["ppj"])
        val meterai = parseAmountStr(result["meterai"])
        val angsuran = parseAmountStr(result["angsuran"])
        val adminFee = parseAmountStr(result["admin"])
        val stroomAmount = parseAmountStr(result["stroom"])
        val totalVal = parseAmountStr(result["total"] ?: result["tagihan"])
        val subtotalVal = parseAmountStr(result["subtotal"])
        val discount = parseAmountStr(result["discount"])
        val tax = parseAmountStr(result["tax"])
        val paid = parseAmountStr(result["paid"])
        val change = parseAmountStr(result["change"])

        val items = extractItems(lines)
        val footer = extractFooterText(lines)

        val extra = linkedMapOf<String, String>()
        if (orderNo.isNotBlank()) extra["No. Pesanan"] = orderNo
        if (meterNo.isNotBlank()) extra["Meter No"] = meterNo
        if (idpel.isNotBlank()) extra["IDPEL"] = idpel
        if (tarifDaya.isNotBlank()) extra["Tarif/Daya"] = tarifDaya
        if (noRef.isNotBlank()) extra["No. Ref"] = noRef
        if (tokenValue.isNotBlank()) extra["Token"] = tokenValue
        if (kwh.isNotBlank()) extra["Jumlah kWh"] = kwh
        if (stroomAmount > 0) extra["Stroom/Token"] = formatRp(stroomAmount)
        if (ppn > 0) extra["PPN"] = formatRp(ppn)
        if (ppj > 0) extra["PPJ"] = formatRp(ppj)
        if (meterai > 0) extra["Meterai"] = formatRp(meterai)
        if (angsuran > 0) extra["Angsuran"] = formatRp(angsuran)
        if (adminFee > 0) extra["Admin"] = formatRp(adminFee)

        val computedSubtotal = if (subtotalVal > 0) subtotalVal
            else if (items.isNotEmpty()) items.sumOf { it.subtotal }
            else 0.0
        val computedTotal = if (totalVal > 0) totalVal
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

    /**
     * Check if a line exactly matches any known label pattern.
     * Used to prevent extracting part of a label as a value
     * (e.g. "Waktu Dibuat" matching "waktu" and extracting "Dibuat").
     */
    private fun isExactLabelMatch(line: String): Boolean {
        val lower = line.lowercase().trim().removeSuffix(":")
        return ALL_LABEL_STRINGS.any { pattern -> lower == pattern }
    }

    /**
     * Phase 1: Find lines that contain "label: value" inline.
     * IMPORTANT: Skip lines where the entire text is a known label
     * (to prevent "Waktu Dibuat" from extracting "Dibuat" as a value
     * when "waktu" is a shorter pattern match).
     */
    private fun extractInlineValues(
        lines: List<String>,
        result: MutableMap<String, String>,
        consumed: MutableSet<Int>
    ) {
        for (i in lines.indices) {
            val line = lines[i]
            if (isExactLabelMatch(line)) continue

            for ((key, patterns) in LABEL_PATTERNS) {
                if (result.containsKey(key)) continue
                // Try longest patterns first to avoid partial matches
                val sortedPatterns = patterns.sortedByDescending { it.length }
                for (pattern in sortedPatterns) {
                    val idx = line.lowercase().indexOf(pattern)
                    if (idx < 0) continue
                    val afterLabel = line.substring(idx + pattern.length)
                        .trimStart(':', ' ', '\t')
                    if (afterLabel.isNotBlank() && afterLabel.length > 1) {
                        result[key] = afterLabel
                        consumed.add(i)
                        break
                    }
                }
                if (result.containsKey(key)) break
            }
        }
    }

    /**
     * Phase 2: Search ENTIRE text for values with unique patterns.
     * Each field has a specific regex/heuristic to find its value anywhere.
     */
    private fun findByPattern(
        lines: List<String>,
        result: MutableMap<String, String>,
        consumed: MutableSet<Int>
    ) {
        // DateTime: find DD/MM/YYYY HH:MM:SS pattern
        if (!result.containsKey("dateTime")) {
            for (i in lines.indices) {
                if (consumed.contains(i)) continue
                val dtMatch = DATE_TIME_PATTERN.find(lines[i])
                if (dtMatch != null) {
                    result["dateTime"] = dtMatch.value
                    consumed.add(i)
                    break
                }
            }
            if (!result.containsKey("dateTime")) {
                for (i in lines.indices) {
                    if (consumed.contains(i)) continue
                    val dateMatch = DATE_PATTERN.find(lines[i])
                    if (dateMatch != null) {
                        val timeMatch = TIME_PATTERN.find(lines[i])
                        result["dateTime"] = "${dateMatch.value} ${timeMatch?.value ?: ""}".trim()
                        consumed.add(i)
                        break
                    }
                }
            }
        }

        // Token: find XXXX-XXXX-XXXX-XX pattern
        if (!result.containsKey("token")) {
            for (i in lines.indices) {
                if (consumed.contains(i)) continue
                val match = TOKEN_PATTERN.find(lines[i])
                if (match != null) {
                    result["token"] = match.value
                    consumed.add(i)
                    break
                }
            }
        }

        // Order number: find APC/TRX + digits pattern
        if (!result.containsKey("orderNo")) {
            for (i in lines.indices) {
                if (consumed.contains(i)) continue
                val match = ORDER_NO_PATTERN.find(lines[i])
                if (match != null) {
                    result["orderNo"] = lines[i]
                    consumed.add(i)
                    break
                }
            }
        }

        // IDPEL: find 12-digit number
        if (!result.containsKey("idpel")) {
            for (i in lines.indices) {
                if (consumed.contains(i)) continue
                val line = lines[i].replace(" ", "")
                if (line.matches(Regex("""^\d{12}$"""))) {
                    result["idpel"] = line
                    consumed.add(i)
                    break
                }
            }
        }

        // Meter No: find 11-digit number (different from IDPEL)
        if (!result.containsKey("meterNo")) {
            for (i in lines.indices) {
                if (consumed.contains(i)) continue
                val line = lines[i].replace(" ", "")
                if (line.matches(Regex("""^\d{11}$"""))) {
                    result["meterNo"] = line
                    consumed.add(i)
                    break
                }
            }
        }

        // Tarif/Daya: find R1/XXXXXXX VA pattern
        if (!result.containsKey("tarifDaya")) {
            for (i in lines.indices) {
                if (consumed.contains(i)) continue
                val match = TARIF_DAYA_PATTERN.find(lines[i])
                if (match != null) {
                    result["tarifDaya"] = lines[i]
                    consumed.add(i)
                    break
                }
            }
        }

        // No. Ref: find alphanumeric reference code (10+ chars, mixed letters & digits)
        if (!result.containsKey("noRef")) {
            for (i in lines.indices) {
                if (consumed.contains(i)) continue
                val line = lines[i]
                if (REF_NO_PATTERN.matches(line) && line.any { it.isLetter() } && line.any { it.isDigit() }) {
                    result["noRef"] = line
                    consumed.add(i)
                    break
                }
            }
        }

        // Customer name: find ALL-CAPS name (not a known label, not consumed)
        if (!result.containsKey("name") && !result.containsKey("customerName")) {
            for (i in lines.indices) {
                if (consumed.contains(i)) continue
                val line = lines[i]
                if (ALLCAPS_NAME_PATTERN.matches(line) && !isKnownLabelText(line)) {
                    result["name"] = line
                    consumed.add(i)
                    break
                }
            }
        }

        // Phone number
        for (i in lines.indices) {
            val match = PHONE_PATTERN.find(lines[i])
            if (match != null) {
                result["storePhone"] = match.groupValues[1].trim()
                break
            }
        }
    }

    /**
     * Phase 3: Find total amount separately.
     * Total is usually the last Rp value in the receipt.
     * Also consumes "total"/"tagihan" label lines.
     */
    private fun findTotalAmount(
        lines: List<String>,
        result: MutableMap<String, String>,
        consumed: MutableSet<Int>
    ) {
        // Consume total/tagihan label lines and merge consecutive ones
        for (i in lines.indices) {
            if (consumed.contains(i)) continue
            val lower = lines[i].lowercase().trim()
            if (TOTAL_PATTERNS.any { lower == it || lower.removeSuffix(":") == it }) {
                consumed.add(i)
            }
        }

        // Find the last Rp amount in the text (before footer) as total
        var lastRpLine = -1
        var lastRpValue = ""
        for (i in lines.indices) {
            if (consumed.contains(i)) continue
            val line = lines[i]
            val lower = line.lowercase()
            if (FOOTER_TRIGGERS.any { lower.contains(it) }) break
            if (AMOUNT_PATTERN.containsMatchIn(line)) {
                lastRpLine = i
                lastRpValue = line
            }
        }

        if (lastRpLine >= 0 && lastRpValue.isNotBlank()) {
            result["total"] = lastRpValue
            consumed.add(lastRpLine)
        }
    }

    /**
     * Phase 4: Match remaining amount labels to values by positional order.
     *
     * In two-column OCR, amount labels appear in one block and their
     * corresponding Rp values appear later in the same order.
     */
    private fun matchAmountsPositionally(
        lines: List<String>,
        result: MutableMap<String, String>,
        consumed: MutableSet<Int>
    ) {
        // Find amount label lines in order they appear
        val assignedKeys = mutableSetOf<String>()
        val amountLabelOrder = mutableListOf<Pair<Int, String>>()
        for (i in lines.indices) {
            if (consumed.contains(i)) continue
            val lower = lines[i].lowercase().trim()
            for ((key, patterns) in AMOUNT_LABELS) {
                if (result.containsKey(key) || assignedKeys.contains(key)) continue
                val isMatch = patterns.any { p -> lower == p || lower.removeSuffix(":") == p }
                if (isMatch) {
                    amountLabelOrder.add(Pair(i, key))
                    assignedKeys.add(key)
                    consumed.add(i)
                    break
                }
            }
        }

        // Find amount value lines (Rp X.XXX or bare "X,XX" with 2 decimals)
        val amountValues = mutableListOf<Pair<Int, String>>()
        for (i in lines.indices) {
            if (consumed.contains(i)) continue
            val line = lines[i]
            if (AMOUNT_PATTERN.containsMatchIn(line) || BARE_AMOUNT_PATTERN.matches(line)) {
                amountValues.add(Pair(i, line))
            }
        }

        // Pair by positional index
        for (idx in amountLabelOrder.indices) {
            if (idx >= amountValues.size) break
            val (_, key) = amountLabelOrder[idx]
            val (valueLineIdx, valueLine) = amountValues[idx]
            result[key] = valueLine
            consumed.add(valueLineIdx)
        }
    }

    /**
     * Phase 5: Find kWh value after amounts are consumed.
     * kWh values have 1 decimal place (like "30,8") vs amounts with 2 ("0,00").
     */
    private fun findKwh(
        lines: List<String>,
        result: MutableMap<String, String>,
        consumed: MutableSet<Int>
    ) {
        if (result.containsKey("jumlahKwh")) return
        for (i in lines.indices) {
            if (consumed.contains(i)) continue
            val line = lines[i]
            if (KWH_PATTERN.matches(line)) {
                result["jumlahKwh"] = line
                consumed.add(i)
                break
            }
        }
    }

    private fun isKnownLabelText(text: String): Boolean {
        val lower = text.lowercase().trim()
        for ((_, patterns) in LABEL_PATTERNS) {
            if (patterns.any { lower == it || lower.removeSuffix(":") == it }) return true
        }
        for ((_, patterns) in AMOUNT_LABELS) {
            if (patterns.any { lower == it || lower.removeSuffix(":") == it }) return true
        }
        return false
    }

    private fun extractAddressFromHeader(lines: List<String>): String {
        for (i in lines.indices) {
            val lower = lines[i].lowercase()
            if (lower.contains("alamat mitra") || lower.contains("alamat toko") || lower.startsWith("alamat")) {
                val afterLabel = lines[i].substringAfter(":").trim()
                if (afterLabel.isNotBlank()) {
                    val addressParts = mutableListOf(afterLabel)
                    if (i + 1 < lines.size) {
                        val nextLine = lines[i + 1]
                        if (!isKnownLabelText(nextLine) && !AMOUNT_PATTERN.containsMatchIn(nextLine) &&
                            !nextLine.lowercase().contains("struk") && !nextLine.lowercase().contains("pembayaran")) {
                            addressParts.add(nextLine)
                        }
                    }
                    return addressParts.joinToString(", ")
                }
            }
        }
        return ""
    }

    private fun detectReceiptTitle(lines: List<String>): String {
        val titleParts = mutableListOf<String>()
        for (line in lines.take(10)) {
            val lower = line.lowercase()
            if (lower.contains("struk pembayaran") || lower.contains("bukti pembayaran") ||
                lower.contains("tagihan listrik") || lower.contains("pln") ||
                lower.contains("receipt") || lower.contains("invoice")) {
                titleParts.add(line)
            }
        }
        return titleParts.joinToString(" ").trim()
    }

    private fun detectPaymentMethod(lines: List<String>): String {
        for (line in lines) {
            val lower = line.lowercase()
            when {
                lower.contains("mitra shopee") || lower.contains("milra shopee") ||
                    lower.contains("et mitra") || lower.contains("et milra") -> return "Mitra Shopee"
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
                i++; continue
            }
            if (!inItemSection) { i++; continue }
            val lower = line.lowercase()
            if (isKnownLabelText(line) || lower.contains("total") || lower.contains("subtotal")) {
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
                    items[items.lastIndex] = items.last().copy(quantity = qty, price = price, subtotal = sub)
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
        val footerLines = mutableListOf<String>()
        var inFooter = false
        for (line in lines) {
            val lower = line.lowercase()
            if (!inFooter && FOOTER_TRIGGERS.any { lower.contains(it) }) {
                inFooter = true
            }
            if (inFooter) footerLines.add(line)
        }
        return footerLines.joinToString("\n")
    }

    private fun parseAmountStr(text: String?): Double {
        if (text.isNullOrBlank()) return 0.0
        val match = AMOUNT_PATTERN.find(text)
        if (match != null) return parseNumber(match.value)
        val numMatch = Regex("""[\d.,]+""").find(text) ?: return 0.0
        return parseNumber(numMatch.value)
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
