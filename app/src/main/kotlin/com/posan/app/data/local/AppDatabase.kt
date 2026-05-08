package com.posan.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.posan.app.data.local.dao.CategoryDao
import com.posan.app.data.local.dao.CustomerDao
import com.posan.app.data.local.dao.PlnTokenTemplateDao
import com.posan.app.data.local.dao.PrintSettingsDao
import com.posan.app.data.local.dao.ProductDao
import com.posan.app.data.local.dao.StockMovementDao
import com.posan.app.data.local.dao.TransactionDao
import com.posan.app.data.local.dao.UserDao
import com.posan.app.data.local.entity.CategoryEntity
import com.posan.app.data.local.entity.CustomerEntity
import com.posan.app.data.local.entity.PlnTokenTemplateEntity
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
        PrintSettingsEntity::class,
        PlnTokenTemplateEntity::class
    ],
    version = 3,
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
    abstract fun plnTokenTemplateDao(): PlnTokenTemplateDao

    companion object {
        const val DB_NAME = "posan.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE print_settings ADD COLUMN mmFeedBeforeCut INTEGER NOT NULL DEFAULT 5")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE customers ADD COLUMN plnIdPelanggan TEXT")
                db.execSQL("ALTER TABLE customers ADD COLUMN plnMeterNo TEXT")
                db.execSQL("ALTER TABLE customers ADD COLUMN plnTarif TEXT")
                db.execSQL("ALTER TABLE customers ADD COLUMN plnDaya TEXT")
                db.execSQL("ALTER TABLE customers ADD COLUMN plnNamaLengkap TEXT")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS pln_token_templates (" +
                        "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "tarif TEXT NOT NULL, " +
                        "daya TEXT NOT NULL, " +
                        "nominalRupiah INTEGER NOT NULL, " +
                        "rpStroom REAL NOT NULL, " +
                        "adminFee REAL NOT NULL, " +
                        "materai REAL NOT NULL, " +
                        "ppn REAL NOT NULL, " +
                        "ppj REAL NOT NULL, " +
                        "kwh REAL NOT NULL, " +
                        "updatedAt INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_pln_token_templates_tarif_daya_nominalRupiah " +
                        "ON pln_token_templates(tarif, daya, nominalRupiah)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_pln_token_templates_nominalRupiah " +
                        "ON pln_token_templates(nominalRupiah)"
                )
            }
        }
    }
}
