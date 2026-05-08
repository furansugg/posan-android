package com.posan.app.ocr

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds extracted PLN token receipt fields. Every field is optional because OCR
 * accuracy depends on screenshot quality and source app layout.
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
 * Line-based, label-aware parser for PLN token receipt screenshots.
 *
 * Strategy:
 *  - Split OCR output into trimmed lines.
 *  - For each field, scan lines top-down until any of its label aliases match.
 *  - Extract the value either on the same line (after the label) or, if blank,
 *    from the next non-empty line (handles screenshots where label and value
 *    are on separate visual rows like ShopeePay's invoice template).
 *  - Amounts are normalised to whole-rupiah strings using Indonesian
 *    convention: `.` is thousands separator, `,` is decimal — so
 *    "Rp 1.396,00" becomes "1396" (we drop sub-rupiah cents).
 *  - Token detection looks for a 20-digit sequence (with optional dashes /
 *    spaces) and prefers candidates appearing near a STROOM/TOKEN label.
 */
@Singleton
class PlnReceiptParser @Inject constructor() {

    fun parse(rawText: String): ParsedPlnReceipt {
        val text = rawText.replace('\u00A0', ' ')
        val lines = text.split('\n').map { it.trim() }
        val joined = lines.joinToString(" ").replace(Regex("\\s+"), " ")

        val tariffDaya = findText(lines, listOf(
            "TARIF\\s*[/\\\\]?\\s*DAYA",
            "GOLONGAN\\s*TARIF",
            "GOL\\.?\\s*TARIF",
            "TARIF",
            "DAYA"
        ))

        return ParsedPlnReceipt(
            customerId = findDigits(lines, listOf(
                "ID\\s*PEL(?:ANGGAN)?",
                "IDPEL",
                "NOMOR\\s*PEL(?:ANGGAN)?",
                "NO\\.?\\s*PEL(?:ANGGAN)?",
                "ID\\s*PLN",
                "ID\\s*CUSTOMER"
            ), minLen = 8, maxLen = 14),
            customerName = findText(lines, listOf(
                "NAMA\\s*PELANGGAN",
                "NAMA\\s*PEL(?:ANGGAN)?",
                "NAMA",
                "CUSTOMER\\s*NAME"
            ))?.let { sanitizeName(it) },
            meterNo = findDigits(lines, listOf(
                "NOMOR\\s*METER",
                "NO\\.?\\s*METER",
                "METER\\s*NO\\.?",
                "NOMETER",
                "METER\\s*NUMBER"
            ), minLen = 8, maxLen = 14),
            tariff = tariffDaya?.let { extractTariff(it) },
            power = tariffDaya?.let { extractPower(it) }
                ?: findPowerInline(joined),
            referenceNo = findText(lines, listOf(
                "NO\\.?\\s*REF(?:ERENSI)?",
                "REF\\.?\\s*NO\\.?",
                "REFF",
                "ID\\s*TRANSAKSI",
                "NO\\.?\\s*TRANSAKSI"
            ))?.let { sanitizeRef(it, lines) },
            token = extractToken(joined),
            kwh = findKwh(lines),
            rpStroom = findAmount(lines, listOf(
                "RP\\s*STROOM\\s*[/\\\\]?\\s*TOKEN",
                "RP\\s*STROOM",
                "STROOM\\s*[/\\\\]?\\s*TOKEN",
                "NILAI\\s*TOKEN",
                "TOKEN\\s*LISTRIK",
                "RP\\s*TOKEN"
            )),
            adminFee = findAmount(lines, listOf(
                "BIAYA\\s*ADM(?:IN)?",
                "ADM(?:IN)?\\s*BANK",
                "ADMIN\\s*FEE",
                "ADM(?:IN)?"
            )),
            materai = findAmount(lines, listOf(
                "BEA\\s*MATERAI",
                "MATERAI"
            )),
            ppn = findAmount(lines, listOf(
                "PPN"
            )),
            ppj = findAmount(lines, listOf(
                "PPJ",
                "PBJT[\\s\\-]?TL",
                "PBJT",
                "PAJAK\\s*PJU",
                "PJU"
            )),
            totalBayar = findAmount(lines, listOf(
                "TOTAL\\s*BAYAR",
                "TOTAL\\s*PEMBAYARAN",
                "TOTAL\\s*TAGIHAN",
                "JUMLAH\\s*BAYAR",
                "TOTAL"
            ))
        )
    }

    private fun findLabelMatch(lines: List<String>, labels: List<String>): Pair<Int, MatchResult>? {
        for ((i, line) in lines.withIndex()) {
            for (label in labels) {
                val r = Regex("(?i)(?<![A-Za-z])$label(?![A-Za-z])")
                val m = r.find(line) ?: continue
                return i to m
            }
        }
        return null
    }

    private fun findText(lines: List<String>, labels: List<String>): String? {
        val match = findLabelMatch(lines, labels) ?: return null
        val (idx, m) = match
        val sameLine = lines[idx].substring(m.range.last + 1)
            .trimStart(' ', '\t', ':', '=', '-')
            .trim()
        if (sameLine.isNotBlank()) return sameLine
        // Try the next non-blank line (label and value on separate rows).
        for (j in (idx + 1) until minOf(lines.size, idx + 3)) {
            if (lines[j].isNotBlank()) return lines[j]
        }
        return null
    }

    private fun findDigits(
        lines: List<String>,
        labels: List<String>,
        minLen: Int,
        maxLen: Int
    ): String? {
        val raw = findText(lines, labels) ?: return null
        // Strip everything except digits, then enforce length window.
        val digits = raw.filter { it.isDigit() }
        if (digits.length < minLen) return null
        return digits.take(maxLen)
    }

    private fun findAmount(lines: List<String>, labels: List<String>): String? {
        val raw = findText(lines, labels) ?: return null
        return parseIndonesianAmount(raw)
    }

    /**
     * Indonesian rupiah convention: `.` = thousands separator, `,` = decimal.
     * "Rp 1.396,00" -> 1396, "Rp46.511" -> 46511, "Rp 0,00" -> 0.
     * Returns the captured whole-rupiah portion as a digit string, or null
     * if no numeric content could be found.
     */
    private fun parseIndonesianAmount(raw: String): String? {
        val match = Regex("(?:RP\\.?\\s*)?(-?[0-9][0-9.,\\s]{0,18})", RegexOption.IGNORE_CASE).find(raw)
            ?: return null
        var captured = match.groupValues[1].trim()
        if (captured.isBlank()) return null
        // Drop a trailing decimal section like ",00" or ".50".
        captured = captured.replace(Regex("[.,]\\s*\\d{1,2}\\s*$"), "")
        // Strip non-digits — leaves only the whole-rupiah portion.
        val digits = captured.filter { it.isDigit() }
        if (digits.isEmpty()) return null
        return digits
    }

    /**
     * Extract a tariff identifier like "R1", "R1M", "B1", "R2" from a
     * "Tarif Daya" string. Tolerates "R1/00000900 VA" and "R1M / 900 VA".
     */
    private fun extractTariff(raw: String): String? {
        val r = Regex("([A-Z]\\s*[0-9]{1,2}[A-Z]?)", RegexOption.IGNORE_CASE).find(raw) ?: return null
        return r.groupValues[1].replace(" ", "").uppercase()
    }

    /**
     * Extract a power rating like "900VA" or "1300VA". Strips leading zeros
     * (some apps render "00000900 VA"). Falls back to inserting "VA" when no
     * unit is on the line.
     */
    private fun extractPower(raw: String): String? {
        val r = Regex("([0-9]{2,8})\\s*(VA|KVA|W|KW)?", RegexOption.IGNORE_CASE).find(raw) ?: return null
        val num = r.groupValues[1].trimStart('0').ifBlank { "0" }
        if (num == "0") return null
        val unit = r.groupValues[2].ifBlank { "VA" }.uppercase()
        return "${num}${unit}"
    }

    private fun findPowerInline(joined: String): String? {
        val r = Regex("([0-9]{3,5})\\s*(VA|KVA)", RegexOption.IGNORE_CASE).find(joined) ?: return null
        return "${r.groupValues[1]}${r.groupValues[2].uppercase()}"
    }

    /**
     * Find the 20-digit STROOM/Token. Strategy:
     *   1. Locate any sequence of 20 digits (possibly grouped with dashes or
     *      single-character spaces) anywhere in the joined OCR text.
     *   2. If multiple candidates, prefer the one closest after a
     *      STROOM/TOKEN label.
     *   3. Otherwise return the first 20-digit candidate.
     */
    private fun extractToken(joined: String): String? {
        val candidates = Regex("[0-9][0-9 \\-]{18,40}").findAll(joined).toList()
        val twenties = candidates.mapNotNull { m ->
            val digits = m.value.filter { it.isDigit() }
            if (digits.length == 20) m to digits else null
        }
        if (twenties.isEmpty()) return null
        val labelEnds = Regex("(?i)(?:STROOM|TOKEN)").findAll(joined).map { it.range.last }.toList()
        if (labelEnds.isNotEmpty()) {
            val nearLabel = twenties.firstOrNull { (m, _) ->
                labelEnds.any { e -> m.range.first in (e + 1)..(e + 80) }
            }
            if (nearLabel != null) return nearLabel.second
        }
        return twenties.first().second
    }

    private fun findKwh(lines: List<String>): String? {
        val raw = findText(lines, listOf(
            "JUMLAH\\s*KWH",
            "JML\\s*KWH",
            "TOTAL\\s*KWH",
            "KWH"
        ))
        if (raw != null) {
            val m = Regex("([0-9]+(?:[.,][0-9]+)?)").find(raw)
            if (m != null) return m.groupValues[1].replace(',', '.')
        }
        for (line in lines) {
            val m = Regex("([0-9]+(?:[.,][0-9]+)?)\\s*KWH", RegexOption.IGNORE_CASE).find(line)
            if (m != null) return m.groupValues[1].replace(',', '.')
        }
        return null
    }

    private fun sanitizeName(raw: String): String {
        return raw.split(Regex("\\s{2,}|[|]")).first().trim()
            .takeIf { it.length in 2..60 } ?: raw.take(60)
    }

    /**
     * Reference numbers are alphanumeric and sometimes wrap to a second line.
     * We greedily merge the next line if it looks like a continuation
     * (uppercase alnum-only).
     */
    private fun sanitizeRef(raw: String, lines: List<String>): String {
        val first = raw.trim()
        val idx = lines.indexOfFirst { it.contains(first) }
        if (idx >= 0 && idx + 1 < lines.size) {
            val next = lines[idx + 1].trim()
            if (next.isNotEmpty() && next.all { it.isLetterOrDigit() } && next.length in 4..32 &&
                next.uppercase() == next
            ) {
                return (first + next).take(40)
            }
        }
        return first.take(40)
    }
}
