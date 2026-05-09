package com.posan.app.data.repository

import com.posan.app.data.local.dao.StockMovementDao
import com.posan.app.data.local.entity.StockMovementEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StockRepository @Inject constructor(
    private val stockMovementDao: StockMovementDao
) {
    fun observeAll(): Flow<List<StockMovementEntity>> = stockMovementDao.observeAll()
    fun observeByProduct(productId: Long): Flow<List<StockMovementEntity>> =
        stockMovementDao.observeByProduct(productId)
}
