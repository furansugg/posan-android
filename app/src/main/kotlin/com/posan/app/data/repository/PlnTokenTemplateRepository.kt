package com.posan.app.data.repository

import com.posan.app.data.local.dao.PlnTokenTemplateDao
import com.posan.app.data.local.entity.PlnTokenTemplateEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlnTokenTemplateRepository @Inject constructor(
    private val dao: PlnTokenTemplateDao
) {
    fun observeAll(): Flow<List<PlnTokenTemplateEntity>> = dao.observeAll()
    fun observeForTariffDaya(tarif: String, daya: String): Flow<List<PlnTokenTemplateEntity>> =
        dao.observeForTariffDaya(tarif, daya)

    suspend fun findExact(tarif: String, daya: String, nominal: Int): PlnTokenTemplateEntity? =
        dao.findExact(tarif.trim(), daya.trim(), nominal)

    suspend fun count(): Int = dao.count()

    suspend fun upsert(template: PlnTokenTemplateEntity): Long = dao.upsert(template)
    suspend fun upsertAll(templates: List<PlnTokenTemplateEntity>) = dao.upsertAll(templates)
    suspend fun update(template: PlnTokenTemplateEntity) = dao.update(template)
    suspend fun delete(template: PlnTokenTemplateEntity) = dao.delete(template)

    /**
     * Seeds representative defaults for the tarif/daya combos most commonly bought by
     * residential PLN customers, so the cashier has something to work with on first launch.
     * Values are typical Jakarta-region figures (admin Rp2.500, PPJ ~3% of stroom, no PPN/materai
     * for residential prabayar). Real numbers vary per region/provider — the templates screen
     * lets the cashier override every figure.
     */
    suspend fun seedDefaultsIfEmpty() {
        if (dao.count() > 0) return
        val defaults = listOf(
            // R1/900VA non-subsidi @ ~Rp1.352/kWh
            template("R1", "900VA", 20_000, stroom = 16_990.0, admin = 2_500.0, ppj = 510.0, kwh = 12.6),
            template("R1", "900VA", 50_000, stroom = 46_116.0, admin = 2_500.0, ppj = 1_384.0, kwh = 34.1),
            template("R1", "900VA", 100_000, stroom = 94_660.0, admin = 2_500.0, ppj = 2_840.0, kwh = 70.0),
            // R1/1300VA @ Rp1.444,7/kWh
            template("R1", "1300VA", 20_000, stroom = 16_990.0, admin = 2_500.0, ppj = 510.0, kwh = 11.8),
            template("R1", "1300VA", 50_000, stroom = 46_116.0, admin = 2_500.0, ppj = 1_384.0, kwh = 31.9),
            template("R1", "1300VA", 100_000, stroom = 94_660.0, admin = 2_500.0, ppj = 2_840.0, kwh = 65.5),
            // R1M/2200VA @ Rp1.444,7/kWh (R1M = listrik prabayar non-subsidi)
            template("R1M", "2200VA", 20_000, stroom = 16_990.0, admin = 2_500.0, ppj = 510.0, kwh = 11.8),
            template("R1M", "2200VA", 50_000, stroom = 46_116.0, admin = 2_500.0, ppj = 1_384.0, kwh = 31.9),
            template("R1M", "2200VA", 100_000, stroom = 94_660.0, admin = 2_500.0, ppj = 2_840.0, kwh = 65.5)
        )
        dao.upsertAll(defaults)
    }

    private fun template(
        tarif: String,
        daya: String,
        nominal: Int,
        stroom: Double,
        admin: Double,
        ppj: Double,
        kwh: Double,
        materai: Double = 0.0,
        ppn: Double = 0.0
    ) = PlnTokenTemplateEntity(
        tarif = tarif,
        daya = daya,
        nominalRupiah = nominal,
        rpStroom = stroom,
        adminFee = admin,
        materai = materai,
        ppn = ppn,
        ppj = ppj,
        kwh = kwh
    )
}
