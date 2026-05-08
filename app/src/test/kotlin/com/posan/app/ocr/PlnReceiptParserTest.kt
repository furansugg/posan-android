package com.posan.app.ocr

import org.junit.Assert.assertEquals
import org.junit.Test

class PlnReceiptParserTest {

    private val parser = PlnReceiptParser()

    @Test
    fun parses_shopee_mitra_image1_raw_text() {
        val raw = """
            Mitra Shopee
            Nama Mitra: Ranz Cell
            Alamat Mitra: JL NUSA INDAH GG BUNG
            A LK IV 000/000 ASAM KUMBANG
            Struk Pembayaran Tagihan Listrik
            PLN Prepaid 20000
            Waktu Dibuat : 18/03/2026 13:57:50
            Username : Admin
            Staf
            Stroom/Token
            3563-0576-7845-73
            49-7025
            No. Pesanan : APC177381707084655
            1422
            Meter no : 14298161994
            IDPEL : 120120601168
            Nama : IRWANSYAH
            Tarif Daya : R1/00000900 VA
            No. Ref : 2OPT210ZF41D759275
            C78BEDA4C52BFD
            Meterai : Rp 0,00
            PPN : 0,00
            PPJ : Rp 1.396,00
            Angsuran : Rp 0,00
            Jumlah kwh : 30,8
            Rp Stroom/Token : Rp 18.604,00
            Total Tagihan : Rp 24.000
            Informasi hubungi call center 123 atau
            hub PLN terdekat
            Simpan struk ini sebagai bukti pembayaran
            yang sah
        """.trimIndent()

        val parsed = parser.parse(raw)

        assertEquals("120120601168", parsed.customerId)
        assertEquals("IRWANSYAH", parsed.customerName)
        assertEquals("14298161994", parsed.meterNo)
        assertEquals("R1", parsed.tariff)
        assertEquals("900VA", parsed.power)
        assertEquals("2OPT210ZF41D759275C78BEDA4C52BFD", parsed.referenceNo)
        assertEquals("35630576784573497025", parsed.token)
        assertEquals("30.8", parsed.kwh)
        assertEquals("18604", parsed.rpStroom)
        assertEquals("0", parsed.materai)
        assertEquals("0", parsed.ppn)
        assertEquals("1396", parsed.ppj)
        assertEquals("24000", parsed.totalBayar)
    }

    @Test
    fun parses_shopeepay_image2_two_column_layout() {
        val raw = """
            ShopeePay Invoice
            Powered By Shopee
            STRUK PEMBELIAN LISTRIK PRABAYAR
            No. Pesanan: 2778248206487211270
            Tanggal Transaksi 2026-05-08 20:50:53
            Stroom/Nomor Token 27501306022373856718
            Nomor Meter 86271580044
            Nomor Pelanggan 120120567119
            Nama NURLINA SIMANJUNTAK
            Tarif Daya R1M / 900 VA
            No. Ref FEFEA7FA44DC4F37875C8B267EEEBE96
            Jumlah KwH 3450 kWh
            Rp Stroom/Token Rp46.511
            Biaya Admin Rp1.400
            PPn Rp0
            PBJT-TL Rp3.489
            Materai Rp0
            Angsuran Rp0
            Total tagihan Rp47.911
        """.trimIndent()

        val parsed = parser.parse(raw)

        assertEquals("120120567119", parsed.customerId)
        assertEquals("NURLINA SIMANJUNTAK", parsed.customerName)
        assertEquals("86271580044", parsed.meterNo)
        assertEquals("R1M", parsed.tariff)
        assertEquals("900VA", parsed.power)
        assertEquals("27501306022373856718", parsed.token)
        assertEquals("3450", parsed.kwh)
        assertEquals("46511", parsed.rpStroom)
        assertEquals("1400", parsed.adminFee)
        assertEquals("0", parsed.materai)
        assertEquals("0", parsed.ppn)
        assertEquals("3489", parsed.ppj)
        assertEquals("47911", parsed.totalBayar)
    }
}
