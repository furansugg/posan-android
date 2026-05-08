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
 * Each lookup iterates label hits across all lines and skips a hit when the
 * resolved value fails an optional `valid` predicate, so a noisy/early match
 * (e.g. "Nama Mitra: ...") doesn't prevent the correct one further down.
 */
@Singleton
class PlnReceiptParser @Inject constructor() {

    fun parse(rawText: String): ParsedPlnReceipt {
        val text = rawText.replace('\u00A0', ' ')
        val lines = text.split('\n').map { it.trim() }
            .filter { line -> line.isNotBlank() && !line.matches(Regex("^[\\s\\-=._:•·]+$")) }
        val joined = lines.joinToString(" ").replace(Regex("\\s+"), " ")

        val tariffDayaText = findText(lines, listOf(
            "TARIF\\s*[/\\\\]?\\s*DAYA",
            "GOLONGAN\\s*TARIF",
            "GOL\\.?\\s*TARIF",
            "TARIF",
            "DAYA"
        ), valid = ::looksLikeTariffDaya)

        return ParsedPlnReceipt(
            customerId = findId(lines, listOf(
                "ID\\s*PEL(?:ANGGAN)?",
                "IDPEL",
                "NOMOR\\s*PEL(?:ANGGAN)?",
                "NO\\.?\\s*PEL(?:ANGGAN)?",
                "ID\\s*PLN",
                "ID\\s*CUSTOMER"
            ), 8, 14),
            customerName = findText(lines, listOf(
                "NAMA\\s*PELANGGAN",
                "NAMA\\s*PEL(?:ANGGAN)?",
                "NAMA",
                "CUSTOMER\\s*NAME"
            ), valid = ::looksLikeName)?.let { sanitizeName(it) },
            meterNo = findId(lines, listOf(
                "NOMOR\\s*METER",
                "NO\\.?\\s*METER",
                "METER\\s*NO\\.?",
                "NOMETER",
                "METER\\s*NUMBER"
            ), 8, 14),
            tariff = tariffDayaText?.let { extractTariff(it) },
            power = tariffDayaText?.let { extractPower(it) }
                ?: findPowerInline(joined),
            referenceNo = findText(lines, listOf(
                "NO\\.?\\s*REF(?:ERENSI)?",
                "REF\\.?\\s*NO\\.?",
                "REFF",
                "ID\\s*TRANSAKSI",
                "NO\\.?\\s*TRANSAKSI"
            ), valid = ::looksLikeRef)?.let { sanitizeRef(it, lines) },
            token = extractToken(joined),
            kwh = findKwh(lines),
            rpStroom = findRpStroom(lines),
            adminFee = findAmount(lines, listOf(
                "BIAYA\\s*ADM(?:IN)?",
                "ADM(?:IN)?\\s*BANK",
                "ADMIN\\s*FEE",
                "ADM(?:IN)?"
            )),
            materai = findAmount(lines, listOf(
                "BEA\\s*M[AE]TERAI",
                "M[AE]TERAI"
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

    private data class LabelHit(val lineIdx: Int, val match: MatchResult)

    private fun findLabelHits(lines: List<String>, labels: List<String>): Sequence<LabelHit> = sequence {
        val patterns = labels.map { Regex("(?i)(?<![A-Za-z])$it(?![A-Za-z])") }
        for (i in lines.indices) {
            for (p in patterns) {
                val m = p.find(lines[i]) ?: continue
                yield(LabelHit(i, m))
                break
            }
        }
    }

    private fun extractValue(lines: List<String>, hit: LabelHit): String? {
        val sameLine = lines[hit.lineIdx].substring(hit.match.range.last + 1)
            .trimStart(' ', '\t', ':', '=', '-')
            .trim()
        if (sameLine.isNotBlank()) return sameLine
        for (j in (hit.lineIdx + 1) until minOf(lines.size, hit.lineIdx + 3)) {
            if (lines[j].isNotBlank()) return lines[j]
        }
        return null
    }

    private fun findText(
        lines: List<String>,
        labels: List<String>,
        valid: (String) -> Boolean = { true }
    ): String? {
        for (hit in findLabelHits(lines, labels)) {
            val v = extractValue(lines, hit) ?: continue
            if (valid(v)) return v
        }
        return null
    }

    private fun findId(lines: List<String>, labels: List<String>, minLen: Int, maxLen: Int): String? {
        for (hit in findLabelHits(lines, labels)) {
            val v = extractValue(lines, hit) ?: continue
            // Reject obvious tokens (long with internal dashes).
            if (v.contains('-') && v.filter { it.isDigit() }.length > maxLen + 2) continue
            val digits = v.filter { it.isDigit() }
            if (digits.length in minLen..(maxLen + 4)) return digits.take(maxLen)
        }
        return null
    }

    private fun findAmount(lines: List<String>, labels: List<String>): String? {
        for (hit in findLabelHits(lines, labels)) {
            val v = extractValue(lines, hit) ?: continue
            if (!looksLikeAmount(v)) continue
            parseIndonesianAmount(v)?.let { return it }
        }
        return null
    }

    /**
     * Indonesian rupiah convention: `.` = thousands separator, `,` = decimal.
     * "Rp 1.396,00" -> "1396", "Rp46.511" -> "46511".
     */
    private fun parseIndonesianAmount(raw: String): String? {
        val match = Regex("(?:RP\\.?\\s*)?(-?[0-9][0-9.,\\s]{0,18})", RegexOption.IGNORE_CASE).find(raw)
            ?: return null
        var captured = match.groupValues[1].trim()
        if (captured.isBlank()) return null
        captured = captured.replace(Regex("[.,]\\s*\\d{1,2}\\s*$"), "")
        val digits = captured.filter { it.isDigit() }
        if (digits.isEmpty()) return null
        return digits
    }

    private fun extractTariff(raw: String): String? {
        val r = Regex("([A-Z]\\s*[0-9]{1,2}[A-Z]?)", RegexOption.IGNORE_CASE).find(raw) ?: return null
        return r.groupValues[1].replace(" ", "").uppercase()
    }

    private fun extractPower(raw: String): String? {
        val matches = Regex("([0-9]{1,8})\\s*(VA|KVA|W|KW)?", RegexOption.IGNORE_CASE).findAll(raw).toList()
        for (m in matches) {
            val numRaw = m.groupValues[1]
            val num = numRaw.trimStart('0').ifBlank { "0" }
            if (num == "0") continue
            val unit = m.groupValues[2].ifBlank { "VA" }.uppercase()
            val hasUnit = m.groupValues[2].isNotEmpty()
            val plausible = num.toIntOrNull()?.let { it in 100..200_000 } == true
            if (hasUnit || plausible) return "$num$unit"
        }
        return null
    }

    private fun findPowerInline(joined: String): String? {
        val r = Regex("([0-9]{3,5})\\s*(VA|KVA)", RegexOption.IGNORE_CASE).find(joined) ?: return null
        return "${r.groupValues[1]}${r.groupValues[2].uppercase()}"
    }

    /**
     * Find the 20-digit STROOM/Token. Looks for any digit run (with optional
     * dashes/spaces) totalling exactly 20 digits, preferring the candidate
     * appearing within ~80 chars after a STROOM/TOKEN label.
     */
    private fun extractToken(joined: String): String? {
        val twenties = Regex("[0-9][0-9 \\-]{18,40}").findAll(joined)
            .mapNotNull { m ->
                val digits = m.value.filter { it.isDigit() }
                if (digits.length == 20) m to digits else null
            }.toList()
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

    /**
     * Special handling: some receipts (Shopee Mitra) wrap "Rp Stroom/Token"
     * across two lines — line N is "Rp : Rp 18.604,00", line N+1 is just
     * "Stroom/Token". Tries 3 patterns in order of specificity.
     */
    private fun findRpStroom(lines: List<String>): String? {
        // A: full label on a single line.
        val tight = listOf(
            "RP\\s*STROOM\\s*[/\\\\]?\\s*(?:NOMOR\\s*)?TOKEN",
            "RP\\s*STROOM",
            "NILAI\\s*TOKEN",
            "RP\\s*TOKEN"
        )
        findAmount(lines, tight)?.let { return it }

        // B: "Stroom/Token" alone on a line whose previous line starts with "Rp <amount>".
        val splitToken = Regex("(?i)^\\s*STROOM\\s*[/\\\\]?\\s*(?:NOMOR\\s*)?TOKEN\\s*$")
        val rpHead = Regex("(?i)^\\s*Rp\\b\\s*[:=]?\\s*(.+)$")
        for (i in 1 until lines.size) {
            if (!splitToken.containsMatchIn(lines[i])) continue
            val pm = rpHead.find(lines[i - 1]) ?: continue
            val v = pm.groupValues[1].trim()
            if (looksLikeAmount(v)) parseIndonesianAmount(v)?.let { return it }
        }

        // C: bare "Stroom/Token" / "Token Listrik" with amount on same or next line.
        findAmount(lines, listOf(
            "STROOM\\s*[/\\\\]?\\s*(?:NOMOR\\s*)?TOKEN",
            "TOKEN\\s*LISTRIK"
        ))?.let { return it }

        return null
    }

    private fun looksLikeAmount(v: String): Boolean {
        val t = v.trim()
        if (t.isBlank()) return false
        if (t.contains('-') && !t.startsWith("-")) return false  // tokens have internal dashes
        if (t.contains('/')) return false
        if (!t.any { it.isDigit() }) return false
        // Indonesian rupiah values rarely exceed 7 digits without separators; a long
        // unbroken digit run is almost certainly a token / ID, not an amount.
        val digits = t.filter { it.isDigit() }
        val hasSeparator = t.any { it == '.' || it == ',' || it == ' ' }
        if (digits.length >= 9 && !hasSeparator) return false
        // Accept "Rp" prefix as long as it isn't followed by another letter — covers
        // "Rp0", "Rp 0", "Rp46.511", "Rp 1.400". Word-boundary `\b` would FAIL here
        // because both "p" and a following digit are word characters.
        if (Regex("(?i)^\\s*Rp\\.?(?![A-Za-z])").containsMatchIn(t)) return true
        return t.matches(Regex("[0-9][0-9.,\\s]*"))
    }

    private fun looksLikeName(v: String): Boolean {
        val s = v.trim().trimStart(':', ' ').uppercase()
        if (s.length !in 2..60) return false
        // Reject "Nama Mitra/Toko/Outlet/Staf/Username/Alamat/..." continuations.
        val excluded = listOf(
            "MITRA", "TOKO", "STAF", "USAHA", "OUTLET", "USERNAME", "USER",
            "ALAMAT", "PEMBELIAN", "PEMBAYARAN", "TANGGAL", "TANGAL", "CABANG"
        )
        if (excluded.any { s.startsWith(it) }) return false
        // Reject if the value is mostly digits (probably a misaligned ID/amount).
        val letters = s.count { it.isLetter() }
        val digits = s.count { it.isDigit() }
        if (digits > letters) return false
        return true
    }

    private fun looksLikeRef(v: String): Boolean {
        val t = v.trim()
        if (t.length < 4) return false
        if (Regex("(?i)\\bRp\\b").containsMatchIn(t)) return false
        return true
    }

    private fun looksLikeTariffDaya(v: String): Boolean {
        if (Regex("(?i)\\b[A-Z]\\s*[0-9]{1,2}[A-Z]?\\b").containsMatchIn(v)) return true
        if (Regex("(?i)\\b[0-9]{2,8}\\s*VA\\b").containsMatchIn(v)) return true
        return false
    }

    private fun sanitizeName(raw: String): String {
        return raw.split(Regex("\\s{2,}|[|]")).first().trim()
            .takeIf { it.length in 2..60 } ?: raw.take(60)
    }

    private fun sanitizeRef(raw: String, lines: List<String>): String {
        val first = raw.trim()
        val idx = lines.indexOfFirst { it.contains(first) }
        if (idx >= 0 && idx + 1 < lines.size) {
            val next = lines[idx + 1].trim()
            if (next.isNotEmpty() && next.all { it.isLetterOrDigit() } &&
                next.length in 4..32 && next.uppercase() == next
            ) {
                return (first + next).take(40)
            }
        }
        return first.take(40)
    }
}
