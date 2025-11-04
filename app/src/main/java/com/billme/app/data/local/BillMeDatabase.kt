package com.billme.app.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.billme.app.data.local.converter.Converters
import com.billme.app.data.local.dao.*
import com.billme.app.data.local.entity.*

@Database(
    entities = [
        Product::class,
        ProductIMEI::class,
        Supplier::class,
        Transaction::class,
        TransactionLineItem::class,
        Customer::class,
        AppSetting::class,
        GSTConfiguration::class,
        GSTRate::class,
        InvoiceGSTDetails::class,
        Invoice::class,
        InvoiceLineItem::class,
        StockAdjustment::class,
        ProductCostHistory::class,
        Signature::class
    ],
    version = 11,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class BillMeDatabase : RoomDatabase() {
    
    abstract fun productDao(): ProductDao
    abstract fun productIMEIDao(): ProductIMEIDao
    abstract fun supplierDao(): SupplierDao
    abstract fun transactionDao(): TransactionDao
    abstract fun transactionLineItemDao(): TransactionLineItemDao
    abstract fun customerDao(): CustomerDao
    abstract fun appSettingDao(): AppSettingDao
    abstract fun gstConfigurationDao(): GSTConfigurationDao
    abstract fun gstRateDao(): GSTRateDao
    abstract fun invoiceGSTDetailsDao(): InvoiceGSTDetailsDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun invoiceLineItemDao(): InvoiceLineItemDao
    abstract fun stockAdjustmentDao(): StockAdjustmentDao
    abstract fun productCostHistoryDao(): ProductCostHistoryDao
    abstract fun signatureDao(): SignatureDao
    
    companion object {
        @Volatile
        private var INSTANCE: BillMeDatabase? = null
        
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create suppliers table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `suppliers` (
                        `supplier_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `supplier_name` TEXT NOT NULL,
                        `supplier_phone` TEXT,
                        `supplier_email` TEXT,
                        `supplier_address` TEXT,
                        `contact_person` TEXT,
                        `gst_number` TEXT,
                        `credit_limit` TEXT NOT NULL DEFAULT '0',
                        `outstanding_balance` TEXT NOT NULL DEFAULT '0',
                        `payment_terms_days` INTEGER NOT NULL DEFAULT 30,
                        `discount_percentage` REAL NOT NULL DEFAULT 0.0,
                        `notes` TEXT,
                        `is_active` INTEGER NOT NULL DEFAULT 1,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL
                    )
                """)
                
                // Create indices for suppliers
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_suppliers_supplier_name` ON `suppliers` (`supplier_name`)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_suppliers_supplier_phone` ON `suppliers` (`supplier_phone`)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_suppliers_gst_number` ON `suppliers` (`gst_number`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_suppliers_is_active` ON `suppliers` (`is_active`)")
                
                // Add new columns to products table
                database.execSQL("ALTER TABLE products ADD COLUMN `variant` TEXT")
                database.execSQL("ALTER TABLE products ADD COLUMN `mrp` TEXT")
                database.execSQL("ALTER TABLE products ADD COLUMN `product_status` TEXT NOT NULL DEFAULT 'IN_STOCK'")
                database.execSQL("ALTER TABLE products ADD COLUMN `warranty_period_months` INTEGER")
                database.execSQL("ALTER TABLE products ADD COLUMN `warranty_start_date` TEXT")
                database.execSQL("ALTER TABLE products ADD COLUMN `warranty_expiry_date` TEXT")
                
                // Create new indices for products
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_products_variant` ON `products` (`variant`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_products_product_status` ON `products` (`product_status`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_products_supplier_id` ON `products` (`supplier_id`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_products_warranty_expiry_date` ON `products` (`warranty_expiry_date`)")
            }
        }
        
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create GST configuration table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `gst_configuration` (
                        `config_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `shop_gstin` TEXT,
                        `shop_legal_name` TEXT,
                        `shop_trade_name` TEXT,
                        `shop_state_code` TEXT,
                        `shop_state_name` TEXT,
                        `default_gst_mode` TEXT NOT NULL DEFAULT 'NO_GST',
                        `allow_per_invoice_mode` INTEGER NOT NULL DEFAULT 0,
                        `default_gst_rate` REAL NOT NULL DEFAULT 18.0,
                        `default_gst_category` TEXT NOT NULL DEFAULT 'GST_18',
                        `show_gstin_on_invoice` INTEGER NOT NULL DEFAULT 1,
                        `show_gst_summary` INTEGER NOT NULL DEFAULT 1,
                        `include_gst_in_price` INTEGER NOT NULL DEFAULT 0,
                        `round_off_gst` INTEGER NOT NULL DEFAULT 1,
                        `enable_gst_validation` INTEGER NOT NULL DEFAULT 1,
                        `require_customer_gstin` INTEGER NOT NULL DEFAULT 0,
                        `auto_detect_interstate` INTEGER NOT NULL DEFAULT 1,
                        `hsn_code_mandatory` INTEGER NOT NULL DEFAULT 0,
                        `is_active` INTEGER NOT NULL DEFAULT 1,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        `updated_by` TEXT NOT NULL DEFAULT 'system'
                    )
                """)
                
                // Create GST rates table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `gst_rates` (
                        `rate_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `category` TEXT NOT NULL,
                        `subcategory` TEXT,
                        `hsn_code` TEXT,
                        `gst_rate` REAL NOT NULL,
                        `gst_category` TEXT NOT NULL,
                        `cgst_rate` REAL,
                        `sgst_rate` REAL,
                        `igst_rate` REAL,
                        `cess_rate` REAL NOT NULL DEFAULT 0.0,
                        `description` TEXT,
                        `effective_from` INTEGER NOT NULL,
                        `effective_to` INTEGER,
                        `is_active` INTEGER NOT NULL DEFAULT 1,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL
                    )
                """)
                
                // Create invoice GST details table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `invoice_gst_details` (
                        `gst_detail_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `transaction_id` INTEGER NOT NULL,
                        `gst_mode` TEXT NOT NULL,
                        `shop_gstin` TEXT,
                        `customer_gstin` TEXT,
                        `is_interstate` INTEGER NOT NULL DEFAULT 0,
                        `taxable_amount` TEXT NOT NULL,
                        `cgst_amount` TEXT NOT NULL DEFAULT '0',
                        `sgst_amount` TEXT NOT NULL DEFAULT '0',
                        `igst_amount` TEXT NOT NULL DEFAULT '0',
                        `cess_amount` TEXT NOT NULL DEFAULT '0',
                        `total_gst_amount` TEXT NOT NULL,
                        `round_off_amount` TEXT NOT NULL DEFAULT '0',
                        `gst_rate_breakdown` TEXT,
                        `hsn_summary` TEXT,
                        `created_at` INTEGER NOT NULL
                    )
                """)
                
                // Create indices for GST tables
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_gst_configuration_shop_gstin` ON `gst_configuration` (`shop_gstin`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_gst_configuration_is_active` ON `gst_configuration` (`is_active`)")
                
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_gst_rates_category_hsn_code` ON `gst_rates` (`category`, `hsn_code`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_gst_rates_is_active` ON `gst_rates` (`is_active`)")
                
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_invoice_gst_details_transaction_id` ON `invoice_gst_details` (`transaction_id`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_invoice_gst_details_gst_mode` ON `invoice_gst_details` (`gst_mode`)")
            }
        }
        
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create invoices table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `invoices` (
                        `invoice_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `invoice_number` TEXT NOT NULL,
                        `transaction_id` INTEGER NOT NULL,
                        `customer_id` INTEGER,
                        `customer_name` TEXT,
                        `customer_phone` TEXT,
                        `customer_gstin` TEXT,
                        `customer_address` TEXT,
                        `customer_state_code` TEXT,
                        `invoice_date` INTEGER NOT NULL,
                        `due_date` INTEGER,
                        `subtotal_amount` TEXT NOT NULL,
                        `discount_amount` TEXT NOT NULL DEFAULT '0',
                        `discount_percentage` REAL NOT NULL DEFAULT 0.0,
                        `taxable_amount` TEXT NOT NULL,
                        `gst_config_id` INTEGER,
                        `gst_mode` TEXT NOT NULL,
                        `is_interstate` INTEGER NOT NULL DEFAULT 0,
                        `shop_gstin` TEXT,
                        `shop_state_code` TEXT,
                        `cgst_amount` TEXT NOT NULL DEFAULT '0',
                        `sgst_amount` TEXT NOT NULL DEFAULT '0',
                        `igst_amount` TEXT NOT NULL DEFAULT '0',
                        `cess_amount` TEXT NOT NULL DEFAULT '0',
                        `total_gst_amount` TEXT NOT NULL DEFAULT '0',
                        `gst_rate_applied` REAL NOT NULL DEFAULT 0.0,
                        `round_off_amount` TEXT NOT NULL DEFAULT '0',
                        `grand_total` TEXT NOT NULL,
                        `amount_in_words` TEXT,
                        `payment_method` TEXT NOT NULL DEFAULT 'CASH',
                        `payment_status` TEXT NOT NULL DEFAULT 'PAID',
                        `amount_paid` TEXT NOT NULL DEFAULT '0',
                        `amount_due` TEXT NOT NULL DEFAULT '0',
                        `show_gstin` INTEGER NOT NULL DEFAULT 1,
                        `show_gst_summary` INTEGER NOT NULL DEFAULT 1,
                        `include_gst_in_price` INTEGER NOT NULL DEFAULT 0,
                        `invoice_type` TEXT NOT NULL DEFAULT 'SALE',
                        `place_of_supply` TEXT,
                        `terms_and_conditions` TEXT,
                        `notes` TEXT,
                        `is_cancelled` INTEGER NOT NULL DEFAULT 0,
                        `cancelled_at` INTEGER,
                        `cancellation_reason` TEXT,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        `created_by` TEXT NOT NULL DEFAULT 'Admin',
                        FOREIGN KEY (`transaction_id`) REFERENCES `transactions` (`transaction_id`) ON DELETE CASCADE,
                        FOREIGN KEY (`customer_id`) REFERENCES `customers` (`customer_id`) ON DELETE SET NULL,
                        FOREIGN KEY (`gst_config_id`) REFERENCES `gst_configuration` (`config_id`) ON DELETE SET NULL
                    )
                """)
                
                // Create invoice_line_items table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `invoice_line_items` (
                        `line_item_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `invoice_id` INTEGER NOT NULL,
                        `product_id` INTEGER NOT NULL,
                        `product_name` TEXT NOT NULL,
                        `product_description` TEXT,
                        `hsn_code` TEXT,
                        `unit_of_measure` TEXT NOT NULL DEFAULT 'PCS',
                        `quantity` TEXT NOT NULL DEFAULT '1',
                        `unit_price` TEXT NOT NULL,
                        `discount_amount` TEXT NOT NULL DEFAULT '0',
                        `discount_percentage` REAL NOT NULL DEFAULT 0.0,
                        `taxable_amount` TEXT NOT NULL,
                        `gst_rate_id` INTEGER,
                        `cgst_rate` REAL NOT NULL DEFAULT 0.0,
                        `sgst_rate` REAL NOT NULL DEFAULT 0.0,
                        `igst_rate` REAL NOT NULL DEFAULT 0.0,
                        `cess_rate` REAL NOT NULL DEFAULT 0.0,
                        `cgst_amount` TEXT NOT NULL DEFAULT '0',
                        `sgst_amount` TEXT NOT NULL DEFAULT '0',
                        `igst_amount` TEXT NOT NULL DEFAULT '0',
                        `cess_amount` TEXT NOT NULL DEFAULT '0',
                        `total_gst_amount` TEXT NOT NULL DEFAULT '0',
                        `line_total` TEXT NOT NULL,
                        `imei_serial` TEXT,
                        `batch_number` TEXT,
                        `warranty_period` TEXT,
                        FOREIGN KEY (`invoice_id`) REFERENCES `invoices` (`invoice_id`) ON DELETE CASCADE,
                        FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`) ON DELETE RESTRICT,
                        FOREIGN KEY (`gst_rate_id`) REFERENCES `gst_rates` (`rate_id`) ON DELETE SET NULL
                    )
                """)
                
                // Create indices for invoices
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_invoices_invoice_number` ON `invoices` (`invoice_number`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_invoices_transaction_id` ON `invoices` (`transaction_id`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_invoices_customer_id` ON `invoices` (`customer_id`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_invoices_invoice_date` ON `invoices` (`invoice_date`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_invoices_gst_mode` ON `invoices` (`gst_mode`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_invoices_payment_status` ON `invoices` (`payment_status`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_invoices_is_interstate` ON `invoices` (`is_interstate`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_invoices_gst_config_id` ON `invoices` (`gst_config_id`)")
                
                // Create indices for invoice_line_items
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_invoice_line_items_invoice_id` ON `invoice_line_items` (`invoice_id`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_invoice_line_items_product_id` ON `invoice_line_items` (`product_id`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_invoice_line_items_hsn_code` ON `invoice_line_items` (`hsn_code`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_invoice_line_items_gst_rate_id` ON `invoice_line_items` (`gst_rate_id`)")
            }
        }
        
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create stock_adjustments table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `stock_adjustments` (
                        `adjustment_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `product_id` INTEGER NOT NULL,
                        `previous_stock` INTEGER NOT NULL,
                        `new_stock` INTEGER NOT NULL,
                        `adjustment_quantity` INTEGER NOT NULL,
                        `reason` TEXT NOT NULL,
                        `notes` TEXT,
                        `adjusted_by` TEXT NOT NULL,
                        `adjustment_date` INTEGER NOT NULL,
                        `reference_number` TEXT,
                        FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`) ON DELETE CASCADE
                    )
                """)
                
                // Create indices for stock_adjustments
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_adjustments_product_id` ON `stock_adjustments` (`product_id`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_adjustments_adjustment_date` ON `stock_adjustments` (`adjustment_date`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_adjustments_reason` ON `stock_adjustments` (`reason`)")
            }
        }
        
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create product_imeis table for individual IMEI tracking
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `product_imeis` (
                        `imei_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `product_id` INTEGER NOT NULL,
                        `imei_number` TEXT NOT NULL,
                        `imei2_number` TEXT,
                        `serial_number` TEXT,
                        `status` TEXT NOT NULL DEFAULT 'AVAILABLE',
                        `purchase_date` INTEGER NOT NULL,
                        `purchase_price` TEXT NOT NULL,
                        `sold_date` INTEGER,
                        `sold_price` TEXT,
                        `sold_in_transaction_id` INTEGER,
                        `box_number` TEXT,
                        `warranty_card_number` TEXT,
                        `notes` TEXT,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`) ON DELETE CASCADE,
                        FOREIGN KEY (`sold_in_transaction_id`) REFERENCES `transactions` (`transaction_id`) ON DELETE SET NULL
                    )
                """)
                
                // Create indices for product_imeis
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_product_imeis_imei_number` ON `product_imeis` (`imei_number`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_product_imeis_product_id` ON `product_imeis` (`product_id`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_product_imeis_status` ON `product_imeis` (`status`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_product_imeis_sold_in_transaction_id` ON `product_imeis` (`sold_in_transaction_id`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_product_imeis_purchase_date` ON `product_imeis` (`purchase_date`)")
            }
        }
        
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // AppSettingDao.getSettingsCount() query method addition
            }
        }
        
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Invoice table schema updates - handled by fallback migration
                // New columns will be created when database is recreated
            }
        }
        
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create product_cost_history table for tracking multiple cost prices
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `product_cost_history` (
                        `cost_history_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `product_id` INTEGER NOT NULL,
                        `cost_price` TEXT NOT NULL,
                        `purchase_date` INTEGER NOT NULL,
                        `quantity_purchased` INTEGER NOT NULL DEFAULT 1,
                        `supplier_name` TEXT,
                        `invoice_number` TEXT,
                        `is_current` INTEGER NOT NULL DEFAULT 1,
                        `notes` TEXT,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`) ON DELETE CASCADE
                    )
                """)
                
                // Create indices for product_cost_history
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_product_cost_history_product_id` ON `product_cost_history` (`product_id`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_product_cost_history_purchase_date` ON `product_cost_history` (`purchase_date`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_product_cost_history_is_current` ON `product_cost_history` (`is_current`)")
            }
        }
        
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add color column to products table
                database.execSQL("""
                    ALTER TABLE `products` ADD COLUMN `color` TEXT DEFAULT NULL
                """)
                
                // Create index for color field to optimize queries by color
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_products_color` ON `products` (`color`)")
                
                // Create combined index for brand, model, color, variant for faster product lookup
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_products_brand_model_color_variant` ON `products` (`brand`, `model`, `color`, `variant`)")
            }
        }
        
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create signatures table for storing signature images
                // Note: created_at and updated_at are stored as INTEGER (milliseconds since epoch)
                // because they use kotlinx.datetime.Instant which Room converts to Long
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `signatures` (
                        `signature_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `signature_name` TEXT NOT NULL,
                        `signature_file_path` TEXT NOT NULL,
                        `is_active` INTEGER NOT NULL DEFAULT 1,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL
                    )
                """)
                
                // Create indices for signatures table
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_signatures_is_active` ON `signatures` (`is_active`)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_signatures_name` ON `signatures` (`signature_name`)")
            }
        }
        
        // REMOVED: getDatabase() function
        // Database should ONLY be created through DatabaseManager to ensure consistent configuration
        // Using multiple getInstance methods causes SQLITE_OK errors due to conflicting database instances
    }
}
