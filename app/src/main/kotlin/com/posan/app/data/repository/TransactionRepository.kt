package com.posan.app.data.repository

import androidx.room.withTransaction
import com.posan.app.data.local.AppDatabase
import com.posan.app.data.local.dao.ProductDao
import com.posan.app.data.local.dao.StockMovementDao
import com.posan.app.data.local.dao.TransactionDao
import com.posan.app.data.local.entity.StockMovementEntity
import com.posan.app.data.local.entity.TransactionEntity
import com.posan.app.data.local.entity.TransactionItemEntity
import com.posan.app.domain.model.PaymentMethod
import com.posan.app.domain.model.StockMovementType
import com.posan.app.domain.model.TransactionStatus
import com.posan.app.util.Format
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

data class CartLine(
    val productId: Long,
    val name: String,
    val sku: String?,
    val price: Double,
    val quantity: Int,
    val discount: Double = 0.0
) {
    val subtotal: Double get() = (price * quantity) - discount
}

data class CheckoutInput(
    val userId: Long?,
    val customerId: Long?,
    val lines: List<CartLine>,
    val discount: Double,
    val taxPercent: Double,
    val paymentMethod: PaymentMethod,
    val paymentReceived: Double,
    val note: String?
)

data class CheckoutResult(
    val transactionId: Long,
    val code: String,
    val total: Double,
    val change: Double
)

data class DailyReport(
    val date: Long,
    val transactionCount: Int,
    val totalRevenue: Double,
    val cashRevenue: Double,
    val qrisRevenue: Double,
    val cardRevenue: Double
)

@Singleton
class TransactionRepository @Inject constructor(
    private val database: AppDatabase,
    private val transactionDao: TransactionDao,
    private val productDao: ProductDao,
    private val stockMovementDao: StockMovementDao
) {
    fun observeAll(): Flow<List<TransactionEntity>> = transactionDao.observeAll()
    fun observeBetween(start: Long, end: Long): Flow<List<TransactionEntity>> =
        transactionDao.observeBetween(start, end)

    fun observeItems(transactionId: Long): Flow<List<TransactionItemEntity>> =
        transactionDao.observeItems(transactionId)

    suspend fun findById(id: Long): TransactionEntity? = transactionDao.findById(id)
    suspend fun findItems(transactionId: Long): List<TransactionItemEntity> =
        transactionDao.findItemsByTransactionId(transactionId)

    suspend fun checkout(input: CheckoutInput): Result<CheckoutResult> {
        if (input.lines.isEmpty()) return Result.failure(IllegalStateException("Keranjang kosong"))

        val subtotal = input.lines.sumOf { it.subtotal }
        val afterDiscount = (subtotal - input.discount).coerceAtLeast(0.0)
        val tax = afterDiscount * (input.taxPercent / 100.0)
        val total = afterDiscount + tax

        if (input.paymentMethod == PaymentMethod.CASH && input.paymentReceived < total) {
            return Result.failure(IllegalArgumentException("Pembayaran tunai kurang"))
        }
        val change = (input.paymentReceived - total).coerceAtLeast(0.0)
        val now = System.currentTimeMillis()
        val code = Format.generateTransactionCode(now)

        return runCatching {
            database.withTransaction {
                val txId = transactionDao.insertTransaction(
                    TransactionEntity(
                        code = code,
                        userId = input.userId,
                        customerId = input.customerId,
                        subtotal = subtotal,
                        discount = input.discount,
                        taxPercent = input.taxPercent,
                        taxAmount = tax,
                        total = total,
                        paymentMethod = input.paymentMethod.name,
                        paymentReceived = if (input.paymentMethod == PaymentMethod.CASH) input.paymentReceived else total,
                        change = change,
                        note = input.note,
                        status = TransactionStatus.PAID.name,
                        createdAt = now
                    )
                )
                val items = input.lines.map { line ->
                    TransactionItemEntity(
                        transactionId = txId,
                        productId = line.productId,
                        name = line.name,
                        sku = line.sku,
                        price = line.price,
                        quantity = line.quantity,
                        discount = line.discount,
                        subtotal = line.subtotal
                    )
                }
                transactionDao.insertItems(items)
                input.lines.forEach { line ->
                    productDao.adjustStock(line.productId, -line.quantity)
                    stockMovementDao.insert(
                        StockMovementEntity(
                            productId = line.productId,
                            type = StockMovementType.SALE.name,
                            quantity = -line.quantity,
                            reference = code,
                            note = "Penjualan"
                        )
                    )
                }
                CheckoutResult(transactionId = txId, code = code, total = total, change = change)
            }
        }
    }

    suspend fun voidTransaction(id: Long): Result<Unit> = runCatching {
        database.withTransaction {
            val tx = transactionDao.findById(id) ?: error("Transaksi tidak ditemukan")
            if (tx.status == TransactionStatus.VOID.name) return@withTransaction
            val items = transactionDao.findItemsByTransactionId(id)
            items.forEach { item ->
                if (item.productId != null) {
                    productDao.adjustStock(item.productId, item.quantity)
                    stockMovementDao.insert(
                        StockMovementEntity(
                            productId = item.productId,
                            type = StockMovementType.IN.name,
                            quantity = item.quantity,
                            reference = tx.code,
                            note = "Pembatalan transaksi"
                        )
                    )
                }
            }
            transactionDao.update(tx.copy(status = TransactionStatus.VOID.name))
        }
    }

    suspend fun dailyReport(timestamp: Long): DailyReport {
        val start = Format.startOfDay(timestamp)
        val end = Format.endOfDay(timestamp)
        return DailyReport(
            date = timestamp,
            transactionCount = transactionDao.countPaidBetween(start, end),
            totalRevenue = transactionDao.totalRevenueBetween(start, end),
            cashRevenue = transactionDao.totalRevenueByMethod(start, end, PaymentMethod.CASH.name),
            qrisRevenue = transactionDao.totalRevenueByMethod(start, end, PaymentMethod.QRIS.name),
            cardRevenue = transactionDao.totalRevenueByMethod(start, end, PaymentMethod.CARD.name)
        )
    }
}
