package com.posan.app.ocr

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds extracted PLN token receipt fields. Every field is optional because OCR
 * accuracy depends on screenshot quality and source app layout. The UI layer
 * decides which fields actually update the form.
 */
data class ParsedPlnReceipt(
    val customerId: String? = null,
    val customerName: String? = null,
    val meterNo: String? = null,
    val tariff: String? = null,
    val power: String? = null,
    val referenceNo: String? = null,
    val token: String? = null,
    val kwh: String? = null,
    val rpStroom: String? = null,
    val adminFee: String? = null,
    val materai: String? = null,
    val ppn: String? = null,
    val ppj: String? = null,
    val totalBayar: String? = null
) {
    fun isEmpty(): Boolean = listOf(
        customerId, customerName, meterNo, tariff, power, referenceNo,
        token, kwh, rpStroom, adminFee, materai, ppn, ppj, totalBayar
    ).all { it.isNullOrBlank() }
}

/**
 * Best-effort regex parser for PLN token receipt screenshots from various
 * source apps (PLN Mobile, m-banking, marketplace e-money, EDC printouts).
 * Patterns are loose on purpose — OCR errors are common (O <-> 0, l <-> 1).
 */
@Singleton
class PlnReceiptParser @Inject constructor() {

    fun parse(rawText: String): ParsedPlnReceipt {
        val text = rawText.replace('\u00A0', ' ')
        val joined = text.replace('\n', ' ').replace(Regex("\\s+"), " ")

        return ParsedPlnReceipt(
            customerId = findValue(text, listOf(
                "ID\\s*PEL(?:ANGGAN)?",
                "ID\\s*PLN",
                "ID\\s*Pelanggan",
                "IDPEL",
                "ID\\s*Customer"
            ), digitsOnly = true, minLen = 10, maxLen = 14),
            customerName = findValue(text, listOf(
                "NAMA\\s*PELANGGAN",
                "NAMA\\s*PEL(?:ANGGAN)?",
                "NAMA",
                "NAME",
                "CUSTOMER\\s*NAME"
            ), digitsOnly = false, minLen = 2, maxLen = 60),
            meterNo = findValue(text, listOf(
                "NO\\s*METER",
                "NO\\.\\s*METER",
                "NOMETER",
                "METER\\s*NO",
                "NOMOR\\s*METER"
            ), digitsOnly = true, minLen = 8, maxLen = 14),
            tariff = findValue(text, listOf(
                "TARIF",
                "GOLONGAN\\s*TARIF",
                "GOL\\.\\s*TARIF"
            ), digitsOnly = false, minLen = 1, maxLen = 8)?.let { extractTariff(it) },
            power = findValue(text, listOf(
                "DAYA",
                "POWER"
            ), digitsOnly = false, minLen = 1, maxLen = 12)?.let { extractPower(it) }
                ?: findPowerInline(joined),
            referenceNo = findValue(text, listOf(
                "NO\\.?\\s*REF(?:ERENSI)?",
                "REF\\s*NO",
                "NOMOR\\s*REFERENSI",
                "REFF",
                "ID\\s*TRANSAKSI",
                "NO\\s*TRANSAKSI"
            ), digitsOnly = false, minLen = 4, maxLen = 30),
            token = extractToken(joined, text),
            kwh = findKwh(text),
            rpStroom = findValueAmount(text, listOf(
                "STROOM[\\s/\\\\]?TOKEN",
                "RP\\s*STROOM",
                "STROOM",
                "TOKEN[\\s/\\\\]?LISTRIK",
                "NILAI\\s*TOKEN"
            )),
            adminFee = findValueAmount(text, listOf(
                "ADM(?:IN)?(?:\\s*BANK)?",
                "BIAYA\\s*ADM(?:IN)?",
                "ADMIN\\s*FEE"
            )),
            materai = findValueAmount(text, listOf(
                "MATERAI",
                "BEA\\s*MATERAI"
            )),
            ppn = findValueAmount(text, listOf(
                "PPN"
            )),
            ppj = findValueAmount(text, listOf(
                "PPJ",
                "PAJAK\\s*PJU",
                "PJU"
            )),
            totalBayar = findValueAmount(text, listOf(
                "TOTAL\\s*BAYAR",
                "TOTAL\\s*PEMBAYARAN",
                "JUMLAH\\s*BAYAR",
                "TOTAL"
            ))
        )
    }

    private fun findValue(
        text: String,
        labels: List<String>,
        digitsOnly: Boolean,
        minLen: Int,
        maxLen: Int
    ): String? {
        val sep = "[\\s:=\\-]+"
        for (label in labels) {
            val pattern = if (digitsOnly) {
                Regex("$label$sep([0-9 .-]{$minLen,${maxLen + 6}})", RegexOption.IGNORE_CASE)
            } else {
                Regex("$label$sep([^\\n\\r]{$minLen,$maxLen})", RegexOption.IGNORE_CASE)
            }
            val m = pattern.find(text) ?: continue
            val raw = m.groupValues[1]
            val cleaned = if (digitsOnly) raw.filter { it.isDigit() } else raw.trim()
            val truncated = cleaned.take(maxLen)
            if (truncated.length >= minLen) return truncated
        }
        return null
    }

    private fun findValueAmount(text: String, labels: List<String>): String? {
        val sep = "[\\s:=\\-]+"
        for (label in labels) {
            val pattern = Regex(
                "$label$sep(?:RP\\.?\\s*)?([0-9][0-9.,\\s]{2,})",
                RegexOption.IGNORE_CASE
            )
            val m = pattern.find(text) ?: continue
            val raw = m.groupValues[1]
            val digits = raw.filter { it.isDigit() }
            if (digits.isNotEmpty() && digits.length <= 12) {
                return digits
            }
        }
        return null
    }

    private fun extractTariff(raw: String): String? {
        // "R1/900VA", "R1 900 VA", "R-1"
        val r = Regex("[A-Z]\\s*[0-9]{1,2}[A-Z]?", RegexOption.IGNORE_CASE).find(raw)
        return r?.value?.replace(" ", "")?.uppercase()
    }

    private fun extractPower(raw: String): String? {
        // "900 VA", "1300VA", "2200 W", "5500"
        val r = Regex("([0-9][0-9.]{2,5})\\s*(VA|W|KVA)?", RegexOption.IGNORE_CASE).find(raw) ?: return null
        val num = r.groupValues[1].filter { it.isDigit() || it == '.' }
        val unit = r.groupValues[2].ifBlank { "VA" }.uppercase()
        if (num.isBlank()) return null
        return "${num}${unit}"
    }

    private fun findPowerInline(joined: String): String? {
        val r = Regex("([0-9]{3,5})\\s*(VA|KVA)", RegexOption.IGNORE_CASE).find(joined) ?: return null
        return "${r.groupValues[1]}${r.groupValues[2].uppercase()}"
    }

    /**
     * PLN token (STROOM) is always 20 digits, often grouped 4-4-4-4-4.
     * Try to find 20 consecutive digits (with optional dashes/spaces between groups)
     * anywhere in the text, but skip lines that are clearly meter/IDPel.
     */
    private fun extractToken(joined: String, raw: String): String? {
        val labelMatch = Regex(
            "(?:NO\\.?\\s*)?TOKEN(?:\\s*[/\\\\]?\\s*STROOM)?\\s*[:=\\-]?\\s*([0-9 \\-]{20,30})",
            RegexOption.IGNORE_CASE
        ).find(raw) ?: Regex(
            "STROOM(?:\\s*[/\\\\]?\\s*TOKEN)?\\s*[:=\\-]?\\s*([0-9 \\-]{20,30})",
            RegexOption.IGNORE_CASE
        ).find(raw)
        if (labelMatch != null) {
            val digits = labelMatch.groupValues[1].filter { it.isDigit() }
            if (digits.length == 20) return digits
        }
        // Fallback: any 20 consecutive digits (with possible separators)
        val grouped = Regex("(\\d{4})[\\s\\-]?(\\d{4})[\\s\\-]?(\\d{4})[\\s\\-]?(\\d{4})[\\s\\-]?(\\d{4})")
            .find(joined)
        if (grouped != null) {
            return grouped.groupValues.drop(1).joinToString("")
        }
        return null
    }

    private fun findKwh(text: String): String? {
        val patterns = listOf(
            Regex("(JUMLAH\\s*KWH|KWH|JML\\s*KWH|TOTAL\\s*KWH)\\s*[:=\\-]?\\s*([0-9]+(?:[.,][0-9]+)?)", RegexOption.IGNORE_CASE),
            Regex("([0-9]+(?:[.,][0-9]+)?)\\s*KWH", RegexOption.IGNORE_CASE)
        )
        for (p in patterns) {
            val m = p.find(text) ?: continue
            val raw = if (m.groupValues.size > 2) m.groupValues[2] else m.groupValues[1]
            val normalized = raw.replace(',', '.').filter { it.isDigit() || it == '.' }
            if (normalized.isNotBlank()) return normalized
        }
        return null
    }
}
