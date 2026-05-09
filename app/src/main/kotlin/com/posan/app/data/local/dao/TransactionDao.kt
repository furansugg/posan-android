package com.posan.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.posan.app.data.local.entity.TransactionEntity
import com.posan.app.data.local.entity.TransactionItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE createdAt BETWEEN :start AND :end ORDER BY createdAt DESC")
    fun observeBetween(start: Long, end: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun findById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId ORDER BY id ASC")
    suspend fun findItemsByTransactionId(transactionId: Long): List<TransactionItemEntity>

    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId ORDER BY id ASC")
    fun observeItems(transactionId: Long): Flow<List<TransactionItemEntity>>

    @Query("SELECT COUNT(*) FROM transactions WHERE createdAt BETWEEN :start AND :end AND status = 'PAID'")
    suspend fun countPaidBetween(start: Long, end: Long): Int

    @Query("SELECT COALESCE(SUM(total), 0) FROM transactions WHERE createdAt BETWEEN :start AND :end AND status = 'PAID'")
    suspend fun totalRevenueBetween(start: Long, end: Long): Double

    @Query("SELECT COALESCE(SUM(total), 0) FROM transactions WHERE createdAt BETWEEN :start AND :end AND status = 'PAID' AND paymentMethod = :method")
    suspend fun totalRevenueByMethod(start: Long, end: Long, method: String): Double

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItems(items: List<TransactionItemEntity>): List<Long>

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("DELETE FROM transaction_items")
    suspend fun deleteAllItems()
}
