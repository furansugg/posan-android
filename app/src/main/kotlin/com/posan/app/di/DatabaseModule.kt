package com.posan.app.di

import android.content.Context
import androidx.room.Room
import com.posan.app.data.local.AppDatabase
import com.posan.app.data.local.dao.CategoryDao
import com.posan.app.data.local.dao.CustomerDao
import com.posan.app.data.local.dao.PrintSettingsDao
import com.posan.app.data.local.dao.ProductDao
import com.posan.app.data.local.dao.StockMovementDao
import com.posan.app.data.local.dao.TransactionDao
import com.posan.app.data.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DB_NAME)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideProductDao(db: AppDatabase): ProductDao = db.productDao()

    @Provides
    fun provideCustomerDao(db: AppDatabase): CustomerDao = db.customerDao()

    @Provides
    fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideStockMovementDao(db: AppDatabase): StockMovementDao = db.stockMovementDao()

    @Provides
    fun providePrintSettingsDao(db: AppDatabase): PrintSettingsDao = db.printSettingsDao()
}
