package com.posan.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.posan.app.data.local.dao.CategoryDao
import com.posan.app.data.local.dao.CustomerDao
import com.posan.app.data.local.dao.PrintSettingsDao
import com.posan.app.data.local.dao.ProductDao
import com.posan.app.data.local.dao.StockMovementDao
import com.posan.app.data.local.dao.TransactionDao
import com.posan.app.data.local.dao.UserDao
import com.posan.app.data.local.entity.CategoryEntity
import com.posan.app.data.local.entity.CustomerEntity
import com.posan.app.data.local.entity.PrintSettingsEntity
import com.posan.app.data.local.entity.ProductEntity
import com.posan.app.data.local.entity.StockMovementEntity
import com.posan.app.data.local.entity.TransactionEntity
import com.posan.app.data.local.entity.TransactionItemEntity
import com.posan.app.data.local.entity.UserEntity
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UserEntity::class,
        CategoryEntity::class,
        ProductEntity::class,
        CustomerEntity::class,
        TransactionEntity::class,
        TransactionItemEntity::class,
        StockMovementEntity::class,
        PrintSettingsEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun customerDao(): CustomerDao
    abstract fun transactionDao(): TransactionDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun printSettingsDao(): PrintSettingsDao

    companion object {
        const val DB_NAME = "posan.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE print_settings ADD COLUMN mmFeedBeforeCut INTEGER NOT NULL DEFAULT 5")
            }
        }
    }
}
