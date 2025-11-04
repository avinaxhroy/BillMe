package com.billme.app.data.local

import com.billme.app.data.local.entity.AppSetting
import com.billme.app.data.local.entity.Product
import com.billme.app.data.local.entity.SettingCategory
import com.billme.app.data.local.entity.SettingValueType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseSeeder @Inject constructor(
    private val database: BillMeDatabase
) {
    
    suspend fun seedIfEmpty() = withContext(Dispatchers.IO) {
        // Always ensure settings are initialized
        val settingsCount = database.appSettingDao().getSettingsCount()
        if (settingsCount == 0) {
            seedSettings()
        }
        
        // Check if products already seeded
        val productCount = database.productDao().getProductCount()
        if (productCount == 0) {
            seedProducts()
        }
    }
    
    private suspend fun seedProducts() {
        val now = Clock.System.now()
        val products = listOf(
            Product(
                productName = "iPhone 14 Pro",
                brand = "Apple",
                model = "iPhone 14 Pro",
                color = "Space Black",
                variant = "256GB",
                category = "Smartphone",
                imei1 = "351234567890001",
                costPrice = BigDecimal("70000"),
                sellingPrice = BigDecimal("85000"),
                currentStock = 5,
                minStockLevel = 2,
                description = "6.1-inch Super Retina XDR display",
                barcode = "194253397229",
                purchaseDate = now,
                createdAt = now,
                updatedAt = now
            ),
            Product(
                productName = "Samsung Galaxy S23",
                brand = "Samsung",
                model = "Galaxy S23",
                color = "Phantom Black",
                variant = "8GB/256GB",
                category = "Smartphone",
                imei1 = "351234567890002",
                costPrice = BigDecimal("55000"),
                sellingPrice = BigDecimal("68000"),
                currentStock = 8,
                minStockLevel = 3,
                description = "6.1-inch Dynamic AMOLED 2X",
                barcode = "887276678122",
                purchaseDate = now,
                createdAt = now,
                updatedAt = now
            ),
            Product(
                productName = "OnePlus 11",
                brand = "OnePlus",
                model = "OnePlus 11",
                color = "Titan Black",
                variant = "16GB/256GB",
                category = "Smartphone",
                imei1 = "351234567890003",
                costPrice = BigDecimal("48000"),
                sellingPrice = BigDecimal("56999"),
                currentStock = 10,
                minStockLevel = 4,
                description = "6.7-inch Fluid AMOLED",
                barcode = "6921815627234",
                purchaseDate = now,
                createdAt = now,
                updatedAt = now
            ),
            Product(
                productName = "Xiaomi 13 Pro",
                brand = "Xiaomi",
                model = "13 Pro",
                color = "Ceramic Black",
                variant = "12GB/256GB",
                category = "Smartphone",
                imei1 = "351234567890004",
                costPrice = BigDecimal("42000"),
                sellingPrice = BigDecimal("52999"),
                currentStock = 6,
                minStockLevel = 3,
                description = "6.73-inch AMOLED Display",
                barcode = "6941812731208",
                purchaseDate = now,
                createdAt = now,
                updatedAt = now
            ),
            Product(
                productName = "iPhone 13",
                brand = "Apple",
                model = "iPhone 13",
                color = "Midnight",
                variant = "128GB",
                category = "Smartphone",
                imei1 = "351234567890005",
                costPrice = BigDecimal("52000"),
                sellingPrice = BigDecimal("59900"),
                currentStock = 12,
                minStockLevel = 5,
                description = "6.1-inch Super Retina XDR",
                barcode = "194252706449",
                purchaseDate = now,
                createdAt = now,
                updatedAt = now
            ),
            Product(
                productName = "Samsung Galaxy A54",
                brand = "Samsung",
                model = "Galaxy A54",
                color = "Awesome Violet",
                variant = "8GB/128GB",
                category = "Smartphone",
                imei1 = "351234567890006",
                costPrice = BigDecimal("28000"),
                sellingPrice = BigDecimal("35999"),
                currentStock = 15,
                minStockLevel = 5,
                description = "6.4-inch Super AMOLED",
                barcode = "887276799456",
                purchaseDate = now,
                createdAt = now,
                updatedAt = now
            ),
            Product(
                productName = "AirPods Pro 2nd Gen",
                brand = "Apple",
                model = "MTJV3",
                category = "Accessories",
                imei1 = "ACC000001",
                costPrice = BigDecimal("18000"),
                sellingPrice = BigDecimal("24900"),
                currentStock = 20,
                minStockLevel = 8,
                description = "Active Noise Cancellation",
                barcode = "194253398769",
                purchaseDate = now,
                createdAt = now,
                updatedAt = now
            ),
            Product(
                productName = "Samsung Buds 2 Pro",
                brand = "Samsung",
                model = "SM-R510",
                category = "Accessories",
                imei1 = "ACC000002",
                costPrice = BigDecimal("12000"),
                sellingPrice = BigDecimal("16999"),
                currentStock = 25,
                minStockLevel = 10,
                description = "Intelligent ANC",
                barcode = "887276623894",
                purchaseDate = now,
                createdAt = now,
                updatedAt = now
            ),
            Product(
                productName = "USB-C Cable 1m",
                brand = "Generic",
                model = "USBC-1M",
                category = "Accessories",
                imei1 = "ACC000003",
                costPrice = BigDecimal("150"),
                sellingPrice = BigDecimal("299"),
                currentStock = 100,
                minStockLevel = 30,
                description = "Fast charging USB-C cable",
                barcode = "1234567890123",
                purchaseDate = now,
                createdAt = now,
                updatedAt = now
            ),
            Product(
                productName = "Tempered Glass",
                brand = "Generic",
                model = "TG-UNIV",
                category = "Accessories",
                imei1 = "ACC000004",
                costPrice = BigDecimal("80"),
                sellingPrice = BigDecimal("199"),
                currentStock = 150,
                minStockLevel = 50,
                description = "9H Hardness Screen Protector",
                barcode = "1234567890124",
                purchaseDate = now,
                createdAt = now,
                updatedAt = now
            ),
            Product(
                productName = "Phone Case",
                brand = "Generic",
                model = "CASE-UNIV",
                category = "Accessories",
                imei1 = "ACC000005",
                costPrice = BigDecimal("120"),
                sellingPrice = BigDecimal("299"),
                currentStock = 80,
                minStockLevel = 30,
                description = "Shockproof Back Cover",
                barcode = "1234567890125",
                purchaseDate = now,
                createdAt = now,
                updatedAt = now
            ),
            Product(
                productName = "20W Fast Charger",
                brand = "Generic",
                model = "CHG-20W",
                category = "Accessories",
                imei1 = "ACC000006",
                costPrice = BigDecimal("300"),
                sellingPrice = BigDecimal("599"),
                currentStock = 50,
                minStockLevel = 20,
                description = "USB-C PD Fast Charger",
                barcode = "1234567890126",
                purchaseDate = now,
                createdAt = now,
                updatedAt = now
            )
        )
        
        products.forEach { product ->
            database.productDao().insertProduct(product)
        }
    }
    
    private suspend fun seedSettings() {
        val now = Clock.System.now()
        val settings = listOf(
            AppSetting(
                settingKey = "shop_name",
                settingValue = "BillMe Pro",
                valueType = SettingValueType.STRING,
                category = SettingCategory.BUSINESS,
                description = "Name of your shop",
                updatedAt = now
            ),
            AppSetting(
                settingKey = "shop_address",
                settingValue = "123 Main Street, City, State - 123456",
                valueType = SettingValueType.STRING,
                category = SettingCategory.BUSINESS,
                description = "Shop address for receipts",
                updatedAt = now
            ),
            AppSetting(
                settingKey = "shop_phone",
                settingValue = "+91 98765 43210",
                valueType = SettingValueType.STRING,
                category = SettingCategory.BUSINESS,
                description = "Contact phone number",
                updatedAt = now
            ),
            AppSetting(
                settingKey = "gst_enabled",
                settingValue = "true",
                valueType = SettingValueType.BOOLEAN,
                category = SettingCategory.BUSINESS,
                description = "Enable GST calculations",
                updatedAt = now
            ),
            AppSetting(
                settingKey = "default_gst_rate",
                settingValue = "18.0",
                valueType = SettingValueType.NUMBER,
                category = SettingCategory.BUSINESS,
                description = "Default GST percentage",
                updatedAt = now
            ),
            AppSetting(
                settingKey = "currency_code",
                settingValue = "INR",
                valueType = SettingValueType.STRING,
                category = SettingCategory.BUSINESS,
                description = "Currency code",
                updatedAt = now
            ),
            AppSetting(
                settingKey = "printer_model",
                settingValue = "Mock Printer",
                valueType = SettingValueType.STRING,
                category = SettingCategory.PRINTER,
                description = "Thermal printer model",
                updatedAt = now
            ),
            AppSetting(
                settingKey = "paper_width",
                settingValue = "58",
                valueType = SettingValueType.NUMBER,
                category = SettingCategory.PRINTER,
                description = "Receipt paper width in mm",
                updatedAt = now
            ),
            AppSetting(
                settingKey = "auto_backup_enabled",
                settingValue = "true",
                valueType = SettingValueType.BOOLEAN,
                category = SettingCategory.APP,
                description = "Enable automatic backups",
                updatedAt = now
            ),
            AppSetting(
                settingKey = "backup_frequency",
                settingValue = "DAILY",
                valueType = SettingValueType.STRING,
                category = SettingCategory.APP,
                description = "How often to backup",
                updatedAt = now
            )
        )
        
        settings.forEach { setting ->
            database.appSettingDao().insertSetting(setting)
        }
    }
}