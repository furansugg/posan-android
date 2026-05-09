package com.posan.app.data.repository

import com.posan.app.data.local.dao.ProductDao
import com.posan.app.data.local.dao.StockMovementDao
import com.posan.app.data.local.entity.ProductEntity
import com.posan.app.data.local.entity.StockMovementEntity
import com.posan.app.domain.model.StockMovementType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor(
    private val productDao: ProductDao,
    private val stockMovementDao: StockMovementDao
) {
    fun observeAll(): Flow<List<ProductEntity>> = productDao.observeAll()
    fun observeActive(): Flow<List<ProductEntity>> = productDao.observeActive()
    fun search(query: String, categoryId: Long?): Flow<List<ProductEntity>> =
        productDao.search(query, categoryId)

    fun observeLowStockCount(threshold: Int = 5): Flow<Int> = productDao.countLowStock(threshold)

    suspend fun findById(id: Long): ProductEntity? = productDao.findById(id)
    suspend fun findBySku(sku: String): ProductEntity? = productDao.findBySku(sku)
    suspend fun findByBarcode(barcode: String): ProductEntity? = productDao.findByBarcode(barcode)

    suspend fun insert(product: ProductEntity): Long {
        val id = productDao.insert(product)
        if (product.stock > 0) {
            stockMovementDao.insert(
                StockMovementEntity(
                    productId = id,
                    type = StockMovementType.IN.name,
                    quantity = product.stock,
                    note = "Stok awal"
                )
            )
        }
        return id
    }

    suspend fun update(product: ProductEntity) = productDao.update(product)
    suspend fun delete(product: ProductEntity) = productDao.delete(product)

    suspend fun adjustStock(productId: Long, delta: Int, type: StockMovementType, note: String?, reference: String? = null) {
        productDao.adjustStock(productId, delta)
        stockMovementDao.insert(
            StockMovementEntity(
                productId = productId,
                type = type.name,
                quantity = delta,
                note = note,
                reference = reference
            )
        )
    }
}
