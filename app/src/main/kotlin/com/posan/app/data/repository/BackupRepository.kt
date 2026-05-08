package com.posan.app.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.posan.app.data.local.AppDatabase
import com.posan.app.data.local.entity.CategoryEntity
import com.posan.app.data.local.entity.CustomerEntity
import com.posan.app.data.local.entity.PrintSettingsEntity
import com.posan.app.data.local.entity.ProductEntity
import com.posan.app.data.local.entity.StockMovementEntity
import com.posan.app.data.local.entity.TransactionEntity
import com.posan.app.data.local.entity.TransactionItemEntity
import com.posan.app.data.local.entity.UserEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class BackupBundle(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val users: List<UserEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val products: List<ProductEntity> = emptyList(),
    val customers: List<CustomerEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val transactionItems: List<TransactionItemEntity> = emptyList(),
    val stockMovements: List<StockMovementEntity> = emptyList(),
    val printSettings: PrintSettingsEntity? = null
)

@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase
) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun export(uri: Uri): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val users = database.userDao().observeAll().first()
            val categories = database.categoryDao().observeAll().first()
            val products = database.productDao().observeAll().first()
            val customers = database.customerDao().observeAll().first()
            val transactions = database.transactionDao().observeAll().first()
            val items = transactions.flatMap { database.transactionDao().findItemsByTransactionId(it.id) }
            val movements = database.stockMovementDao().observeAll().first()
            val settings = database.printSettingsDao().get()
            val bundle = BackupBundle(
                users = users,
                categories = categories,
                products = products,
                customers = customers,
                transactions = transactions,
                transactionItems = items,
                stockMovements = movements,
                printSettings = settings
            )
            val json = gson.toJson(bundle)
            context.contentResolver.openOutputStream(uri, "w")?.use { stream ->
                stream.write(json.toByteArray(Charsets.UTF_8))
                stream.flush()
            } ?: error("Tidak bisa menulis ke file backup")
            json.length.toLong()
        }
    }

    suspend fun import(uri: Uri, replaceExisting: Boolean): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val text = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader().readText()
            } ?: error("Tidak bisa membaca file backup")
            val bundle = gson.fromJson(text, BackupBundle::class.java)
                ?: error("Format backup tidak valid")
            database.withTransaction {
                if (replaceExisting) {
                    database.transactionDao().deleteAllItems()
                    database.transactionDao().deleteAll()
                    database.stockMovementDao().deleteAll()
                    database.productDao().deleteAll()
                    database.categoryDao().deleteAll()
                    database.customerDao().deleteAll()
                }
                bundle.users.forEach { runCatching { database.userDao().insert(it) } }
                bundle.categories.forEach { runCatching { database.categoryDao().insert(it) } }
                bundle.products.forEach { runCatching { database.productDao().insert(it) } }
                bundle.customers.forEach { runCatching { database.customerDao().insert(it) } }
                bundle.transactions.forEach { runCatching { database.transactionDao().insertTransaction(it) } }
                if (bundle.transactionItems.isNotEmpty()) {
                    runCatching { database.transactionDao().insertItems(bundle.transactionItems) }
                }
                bundle.stockMovements.forEach { runCatching { database.stockMovementDao().insert(it) } }
                bundle.printSettings?.let { database.printSettingsDao().upsert(it) }
            }
            bundle.products.size + bundle.transactions.size + bundle.customers.size
        }
    }
}
