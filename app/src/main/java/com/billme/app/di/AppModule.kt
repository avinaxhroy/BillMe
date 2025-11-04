package com.billme.app.di

import android.content.Context
import androidx.room.Room
import com.billme.app.core.database.*
import com.billme.app.data.local.BillMeDatabase
import com.billme.app.data.local.DatabaseManager
import com.billme.app.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Hilt module for providing application-level dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun provideContext(@ApplicationContext context: Context): Context = context
    
    // Database core components
    @Provides
    @Singleton
    fun provideDatabaseManager(
        @ApplicationContext context: Context
    ): DatabaseManager = DatabaseManager(context)

    @Provides
    @Singleton
    fun provideDatabase(
        databaseManager: DatabaseManager
    ): BillMeDatabase = databaseManager.getDatabase()
    
    // Database utilities - SIMPLIFIED versions without PRAGMA/SQL commands
    @Provides
    @Singleton
    fun provideDatabaseHealthMonitor(
        database: BillMeDatabase,
        @ApplicationContext context: Context
    ): DatabaseHealthMonitor = DatabaseHealthMonitor(database, context)

    @Provides
    @Singleton
    fun provideDatabaseBackupManager(
        @ApplicationContext context: Context,
        database: BillMeDatabase
    ): DatabaseBackupManager = DatabaseBackupManager(context, database)

    @Provides
    @Singleton
    fun provideDatabaseCacheManager(): DatabaseCacheManager = DatabaseCacheManager()

    @Provides
    fun provideProductDao(database: BillMeDatabase): ProductDao = database.productDao()

    @Provides
    fun provideSupplierDao(database: BillMeDatabase): SupplierDao = database.supplierDao()

    @Provides
    fun provideTransactionDao(database: BillMeDatabase): TransactionDao = database.transactionDao()

    @Provides
    fun provideTransactionLineItemDao(database: BillMeDatabase): TransactionLineItemDao = database.transactionLineItemDao()

    @Provides
    fun provideCustomerDao(database: BillMeDatabase): CustomerDao = database.customerDao()

    @Provides
    fun provideAppSettingDao(database: BillMeDatabase): AppSettingDao = database.appSettingDao()

    @Provides
    fun provideGSTConfigurationDao(database: BillMeDatabase): GSTConfigurationDao = database.gstConfigurationDao()

    @Provides
    fun provideGSTRateDao(database: BillMeDatabase): GSTRateDao = database.gstRateDao()

    @Provides
    fun provideInvoiceGSTDetailsDao(database: BillMeDatabase): InvoiceGSTDetailsDao = database.invoiceGSTDetailsDao()
    
    @Provides
    fun provideInvoiceDao(database: BillMeDatabase): InvoiceDao = database.invoiceDao()
    
    @Provides
    fun provideInvoiceLineItemDao(database: BillMeDatabase): InvoiceLineItemDao = database.invoiceLineItemDao()

    @Provides
    fun provideProductIMEIDao(database: BillMeDatabase): ProductIMEIDao = database.productIMEIDao()

    @Provides
    fun provideStockAdjustmentDao(database: BillMeDatabase): StockAdjustmentDao = database.stockAdjustmentDao()

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob())
    }
}

/**
 * Qualifier for application-scoped coroutine scope
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope