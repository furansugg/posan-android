package com.posan.app.data.repository

import com.posan.app.data.local.dao.PrintSettingsDao
import com.posan.app.data.local.entity.PrintSettingsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrintSettingsRepository @Inject constructor(
    private val dao: PrintSettingsDao
) {
    fun observe(): Flow<PrintSettingsEntity> =
        dao.observe().map { it ?: PrintSettingsEntity() }

    suspend fun get(): PrintSettingsEntity = dao.get() ?: PrintSettingsEntity()

    suspend fun ensureSeeded() {
        if (dao.get() == null) {
            dao.upsert(PrintSettingsEntity())
        }
    }

    suspend fun save(settings: PrintSettingsEntity) {
        dao.upsert(settings.copy(id = 1, updatedAt = System.currentTimeMillis()))
    }
}
