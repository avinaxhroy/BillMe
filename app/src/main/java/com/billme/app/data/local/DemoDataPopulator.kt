package com.billme.app.data.local

import com.billme.app.data.local.entity.Product
import com.billme.app.data.local.entity.ProductIMEI
import com.billme.app.data.local.entity.IMEIStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import java.math.BigDecimal
import kotlin.time.Duration.Companion.days

class DemoDataPopulator(private val database: BillMeDatabase) {
    
    suspend fun populateDemoData() = withContext(Dispatchers.IO) {
        // Always insert demo data (user can delete if needed)
        // Check if iPhone 15 demo product already exists by brand+model
        val existingProduct = database.productDao().getProductByBrandAndModel("Apple", "iPhone 15")
        if (existingProduct != null) {
            return@withContext // Demo data already loaded
        }
        
        // Insert demo products
        val now = Clock.System.now()
        val iphone15 = Product(
            productName = "iPhone 15",
            brand = "Apple",
            model = "iPhone 15",
            color = "Blue",
            variant = "128GB",
            category = "Smartphones",
            imei1 = "352987654321001",
            costPrice = BigDecimal("65000.00"),
            sellingPrice = BigDecimal("72000.00"),
            minStockLevel = 2,
            description = "Apple iPhone 15 with A16 Bionic chip",
            purchaseDate = now - 15.days,
            createdAt = now,
            updatedAt = now
        )
        val iphone15Id = database.productDao().insertProduct(iphone15)
        
        val iphone15Pro = Product(
            productName = "iPhone 15 Pro",
            brand = "Apple",
            model = "iPhone 15 Pro",
            color = "Natural Titanium",
            variant = "256GB",
            category = "Smartphones",
            imei1 = "358912345678901",
            costPrice = BigDecimal("105000.00"),
            sellingPrice = BigDecimal("115000.00"),
            minStockLevel = 2,
            description = "Apple iPhone 15 Pro with Titanium design",
            purchaseDate = now - 20.days,
            createdAt = now,
            updatedAt = now
        )
        val iphone15ProId = database.productDao().insertProduct(iphone15Pro)
        
        val galaxyS24 = Product(
            productName = "Galaxy S24",
            brand = "Samsung",
            model = "Galaxy S24",
            color = "Phantom Black",
            variant = "8GB/256GB",
            category = "Smartphones",
            imei1 = "354123987654001",
            costPrice = BigDecimal("62000.00"),
            sellingPrice = BigDecimal("68000.00"),
            minStockLevel = 2,
            description = "Samsung Galaxy S24 with AI features",
            purchaseDate = now - 8.days,
            createdAt = now,
            updatedAt = now
        )
        val galaxyS24Id = database.productDao().insertProduct(galaxyS24)
        
        val pixel8 = Product(
            productName = "Pixel 8 Pro",
            brand = "Google",
            model = "Pixel 8 Pro",
            color = "Obsidian",
            variant = "12GB/128GB",
            category = "Smartphones",
            imei1 = "356789012345001",
            costPrice = BigDecimal("58000.00"),
            sellingPrice = BigDecimal("64000.00"),
            minStockLevel = 1,
            description = "Google Pixel 8 Pro with advanced AI camera",
            purchaseDate = now - 6.days,
            createdAt = now,
            updatedAt = now
        )
        val pixel8Id = database.productDao().insertProduct(pixel8)
        
        val oneplus12 = Product(
            productName = "OnePlus 12",
            brand = "OnePlus",
            model = "OnePlus 12",
            color = "Flowy Emerald",
            variant = "12GB/256GB",
            category = "Smartphones",
            imei1 = "359876543210001",
            costPrice = BigDecimal("54000.00"),
            sellingPrice = BigDecimal("59000.00"),
            minStockLevel = 1,
            description = "OnePlus 12 with Snapdragon 8 Gen 3",
            purchaseDate = now - 10.days,
            createdAt = now,
            updatedAt = now
        )
        val oneplus12Id = database.productDao().insertProduct(oneplus12)
        
        // Add more realistic demo products with different colors and variants
        val redmiNote14 = Product(
            productName = "Redmi Note 14 5G",
            brand = "Redmi",
            model = "Note 14 5G",
            color = "Crimson Art",
            variant = "8GB/256GB",
            category = "Smartphones",
            imei1 = "869958079210901",
            costPrice = BigDecimal("15000.00"),
            sellingPrice = BigDecimal("18000.00"),
            minStockLevel = 3,
            description = "Redmi Note 14 5G with MediaTek Dimensity",
            purchaseDate = now - 3.days,
            createdAt = now,
            updatedAt = now
        )
        val redmiNote14Id = database.productDao().insertProduct(redmiNote14)
        
        val redmiNote14Purple = Product(
            productName = "Redmi Note 14 5G",
            brand = "Redmi",
            model = "Note 14 5G",
            color = "Phantom Purple",
            variant = "8GB/256GB",
            category = "Smartphones",
            imei1 = "867906074128182",
            costPrice = BigDecimal("15000.00"),
            sellingPrice = BigDecimal("18000.00"),
            minStockLevel = 3,
            description = "Redmi Note 14 5G with MediaTek Dimensity",
            purchaseDate = now - 3.days,
            createdAt = now,
            updatedAt = now
        )
        val redmiNote14PurpleId = database.productDao().insertProduct(redmiNote14Purple)
        
        val redmiNote14Green = Product(
            productName = "Redmi Note 14 5G",
            brand = "Redmi",
            model = "Note 14 5G",
            color = "Ivy Green",
            variant = "8GB/256GB",
            category = "Smartphones",
            imei1 = "869880074530167",
            costPrice = BigDecimal("15000.00"),
            sellingPrice = BigDecimal("18000.00"),
            minStockLevel = 3,
            description = "Redmi Note 14 5G with MediaTek Dimensity",
            purchaseDate = now - 3.days,
            createdAt = now,
            updatedAt = now
        )
        val redmiNote14GreenId = database.productDao().insertProduct(redmiNote14Green)
        
        val galaxyS24Ultra = Product(
            productName = "Galaxy S24 Ultra",
            brand = "Samsung",
            model = "Galaxy S24 Ultra",
            color = "Titanium Gray",
            variant = "12GB/512GB",
            category = "Smartphones",
            imei1 = "354123987654099",
            costPrice = BigDecimal("98000.00"),
            sellingPrice = BigDecimal("108000.00"),
            minStockLevel = 1,
            description = "Samsung Galaxy S24 Ultra with S Pen",
            purchaseDate = now - 5.days,
            createdAt = now,
            updatedAt = now
        )
        val galaxyS24UltraId = database.productDao().insertProduct(galaxyS24Ultra)
        
        val iphone15ProMax = Product(
            productName = "iPhone 15 Pro Max",
            brand = "Apple",
            model = "iPhone 15 Pro Max",
            color = "Blue Titanium",
            variant = "512GB",
            category = "Smartphones",
            imei1 = "358912345678999",
            costPrice = BigDecimal("135000.00"),
            sellingPrice = BigDecimal("148000.00"),
            minStockLevel = 1,
            description = "Apple iPhone 15 Pro Max with A17 Pro chip",
            purchaseDate = now - 12.days,
            createdAt = now,
            updatedAt = now
        )
        val iphone15ProMaxId = database.productDao().insertProduct(iphone15ProMax)
        
        // Insert demo IMEIs for iPhone 15 (3 units)
        database.productIMEIDao().insertIMEI(
            ProductIMEI(
                productId = iphone15Id,
                imeiNumber = "352987654321001",
                imei2Number = "352987654321002",
                serialNumber = "C7JK8MN2P9Q1",
                status = IMEIStatus.AVAILABLE,
                purchasePrice = BigDecimal("65000.00"),
                purchaseDate = now - 15.days,
                boxNumber = "BOX-APL-001",
                warrantyCardNumber = "WAR-APL-001",
                notes = "New stock - October batch",
                createdAt = now,
                updatedAt = now
            )
        )
        
        database.productIMEIDao().insertIMEI(
            ProductIMEI(
                productId = iphone15Id,
                imeiNumber = "352987654321003",
                imei2Number = "352987654321004",
                serialNumber = "C7JK8MN2P9Q2",
                status = IMEIStatus.AVAILABLE,
                purchasePrice = BigDecimal("64500.00"),
                purchaseDate = now - 15.days,
                boxNumber = "BOX-APL-002",
                warrantyCardNumber = "WAR-APL-002",
                notes = "New stock - October batch",
                createdAt = now,
                updatedAt = now
            )
        )
        
        database.productIMEIDao().insertIMEI(
            ProductIMEI(
                productId = iphone15Id,
                imeiNumber = "352987654321005",
                imei2Number = "352987654321006",
                serialNumber = "C7JK8MN2P9Q3",
                status = IMEIStatus.RESERVED,
                purchasePrice = BigDecimal("65000.00"),
                purchaseDate = now - 15.days,
                boxNumber = "BOX-APL-003",
                warrantyCardNumber = "WAR-APL-003",
                notes = "Reserved for customer ABC",
                createdAt = now,
                updatedAt = now
            )
        )
        
        // Insert demo IMEIs for iPhone 15 Pro (2 units - 1 sold)
        database.productIMEIDao().insertIMEI(
            ProductIMEI(
                productId = iphone15ProId,
                imeiNumber = "358912345678901",
                imei2Number = "358912345678902",
                serialNumber = "D9MN7PQ4R2S3",
                status = IMEIStatus.SOLD,
                purchasePrice = BigDecimal("105000.00"),
                purchaseDate = now - 20.days,
                soldDate = now - 5.days,
                boxNumber = "BOX-APL-PRO-001",
                warrantyCardNumber = "WAR-APL-PRO-001",
                notes = "Sold to premium customer",
                createdAt = now,
                updatedAt = now
            )
        )
        
        database.productIMEIDao().insertIMEI(
            ProductIMEI(
                productId = iphone15ProId,
                imeiNumber = "358912345678903",
                imei2Number = "358912345678904",
                serialNumber = "D9MN7PQ4R2S4",
                status = IMEIStatus.AVAILABLE,
                purchasePrice = BigDecimal("104500.00"),
                purchaseDate = now - 10.days,
                boxNumber = "BOX-APL-PRO-002",
                warrantyCardNumber = "WAR-APL-PRO-002",
                createdAt = now,
                updatedAt = now
            )
        )
        
        // Insert demo IMEIs for Galaxy S24 (4 units)
        database.productIMEIDao().insertIMEI(
            ProductIMEI(
                productId = galaxyS24Id,
                imeiNumber = "354123987654001",
                imei2Number = "354123987654002",
                serialNumber = "RF8CKZX5N4M1",
                status = IMEIStatus.AVAILABLE,
                purchasePrice = BigDecimal("62000.00"),
                purchaseDate = now - 8.days,
                boxNumber = "BOX-SAM-001",
                warrantyCardNumber = "WAR-SAM-001",
                createdAt = now,
                updatedAt = now
            )
        )
        
        database.productIMEIDao().insertIMEI(
            ProductIMEI(
                productId = galaxyS24Id,
                imeiNumber = "354123987654003",
                serialNumber = "RF8CKZX5N4M2",
                status = IMEIStatus.AVAILABLE,
                purchasePrice = BigDecimal("61500.00"),
                purchaseDate = now - 8.days,
                boxNumber = "BOX-SAM-002",
                warrantyCardNumber = "WAR-SAM-002",
                createdAt = now,
                updatedAt = now
            )
        )
        
        database.productIMEIDao().insertIMEI(
            ProductIMEI(
                productId = galaxyS24Id,
                imeiNumber = "354123987654005",
                imei2Number = "354123987654006",
                serialNumber = "RF8CKZX5N4M3",
                status = IMEIStatus.AVAILABLE,
                purchasePrice = BigDecimal("62000.00"),
                purchaseDate = now - 5.days,
                boxNumber = "BOX-SAM-003",
                warrantyCardNumber = "WAR-SAM-003",
                createdAt = now,
                updatedAt = now
            )
        )
        
        database.productIMEIDao().insertIMEI(
            ProductIMEI(
                productId = galaxyS24Id,
                imeiNumber = "354123987654007",
                serialNumber = "RF8CKZX5N4M4",
                status = IMEIStatus.DAMAGED,
                purchasePrice = BigDecimal("62000.00"),
                purchaseDate = now - 12.days,
                boxNumber = "BOX-SAM-004",
                warrantyCardNumber = "WAR-SAM-004",
                notes = "Screen damage - sent for repair",
                createdAt = now,
                updatedAt = now
            )
        )
        
        // Insert demo IMEIs for Pixel 8 Pro (2 units)
        database.productIMEIDao().insertIMEI(
            ProductIMEI(
                productId = pixel8Id,
                imeiNumber = "356789012345001",
                serialNumber = "G8PXL9ZY3W2Q",
                status = IMEIStatus.AVAILABLE,
                purchasePrice = BigDecimal("58000.00"),
                purchaseDate = now - 6.days,
                boxNumber = "BOX-GOO-001",
                warrantyCardNumber = "WAR-GOO-001",
                createdAt = now,
                updatedAt = now
            )
        )
        
        database.productIMEIDao().insertIMEI(
            ProductIMEI(
                productId = pixel8Id,
                imeiNumber = "356789012345003",
                serialNumber = "G8PXL9ZY3W2R",
                status = IMEIStatus.AVAILABLE,
                purchasePrice = BigDecimal("57500.00"),
                purchaseDate = now - 3.days,
                boxNumber = "BOX-GOO-002",
                warrantyCardNumber = "WAR-GOO-002",
                createdAt = now,
                updatedAt = now
            )
        )
        
        // Insert demo IMEIs for OnePlus 12 (3 units - 1 returned)
        database.productIMEIDao().insertIMEI(
            ProductIMEI(
                productId = oneplus12Id,
                imeiNumber = "359876543210001",
                imei2Number = "359876543210002",
                serialNumber = "OP12XYZ789AB",
                status = IMEIStatus.AVAILABLE,
                purchasePrice = BigDecimal("54000.00"),
                purchaseDate = now - 10.days,
                boxNumber = "BOX-OPL-001",
                warrantyCardNumber = "WAR-OPL-001",
                createdAt = now,
                updatedAt = now
            )
        )
        
        database.productIMEIDao().insertIMEI(
            ProductIMEI(
                productId = oneplus12Id,
                imeiNumber = "359876543210003",
                imei2Number = "359876543210004",
                serialNumber = "OP12XYZ789CD",
                status = IMEIStatus.AVAILABLE,
                purchasePrice = BigDecimal("53500.00"),
                purchaseDate = now - 7.days,
                boxNumber = "BOX-OPL-002",
                warrantyCardNumber = "WAR-OPL-002",
                createdAt = now,
                updatedAt = now
            )
        )
        
        database.productIMEIDao().insertIMEI(
            ProductIMEI(
                productId = oneplus12Id,
                imeiNumber = "359876543210005",
                imei2Number = "359876543210006",
                serialNumber = "OP12XYZ789EF",
                status = IMEIStatus.RETURNED,
                purchasePrice = BigDecimal("54000.00"),
                purchaseDate = now - 15.days,
                soldDate = now - 8.days,
                boxNumber = "BOX-OPL-003",
                warrantyCardNumber = "WAR-OPL-003",
                notes = "Customer returned - battery issue, refunded",
                createdAt = now,
                updatedAt = now
            )
        )
        
        // Insert IMEIs for Redmi Note 14 5G (3 different colors)
        database.productIMEIDao().insertIMEI(
            ProductIMEI(
                productId = redmiNote14Id,
                imeiNumber = "869958079210901",
                serialNumber = "RN14CRA123",
                status = IMEIStatus.AVAILABLE,
                purchasePrice = BigDecimal("15000.00"),
                purchaseDate = now - 3.days,
                boxNumber = "BOX-RMI-001",
                warrantyCardNumber = "WAR-RMI-001",
                notes = "Crimson Art color",
                createdAt = now,
                updatedAt = now
            )
        )
        
        database.productIMEIDao().insertIMEI(
            ProductIMEI(
                productId = redmiNote14PurpleId,
                imeiNumber = "867906074128182",
                serialNumber = "RN14PPR456",
                status = IMEIStatus.AVAILABLE,
                purchasePrice = BigDecimal("15000.00"),
                purchaseDate = now - 3.days,
                boxNumber = "BOX-RMI-002",
                warrantyCardNumber = "WAR-RMI-002",
                notes = "Phantom Purple color",
                createdAt = now,
                updatedAt = now
            )
        )
        
        database.productIMEIDao().insertIMEI(
            ProductIMEI(
                productId = redmiNote14GreenId,
                imeiNumber = "869880074530167",
                serialNumber = "RN14IVG789",
                status = IMEIStatus.AVAILABLE,
                purchasePrice = BigDecimal("15000.00"),
                purchaseDate = now - 3.days,
                boxNumber = "BOX-RMI-003",
                warrantyCardNumber = "WAR-RMI-003",
                notes = "Ivy Green color",
                createdAt = now,
                updatedAt = now
            )
        )
        
        // Insert IMEIs for Galaxy S24 Ultra
        database.productIMEIDao().insertIMEI(
            ProductIMEI(
                productId = galaxyS24UltraId,
                imeiNumber = "354123987654099",
                imei2Number = "354123987654100",
                serialNumber = "S24UTGY001",
                status = IMEIStatus.AVAILABLE,
                purchasePrice = BigDecimal("98000.00"),
                purchaseDate = now - 5.days,
                boxNumber = "BOX-SAM-ULT-001",
                warrantyCardNumber = "WAR-SAM-ULT-001",
                notes = "S24 Ultra Titanium Gray",
                createdAt = now,
                updatedAt = now
            )
        )
        
        // Insert IMEIs for iPhone 15 Pro Max
        database.productIMEIDao().insertIMEI(
            ProductIMEI(
                productId = iphone15ProMaxId,
                imeiNumber = "358912345678999",
                imei2Number = "358912345679000",
                serialNumber = "I15PMBT512",
                status = IMEIStatus.AVAILABLE,
                purchasePrice = BigDecimal("135000.00"),
                purchaseDate = now - 12.days,
                boxNumber = "BOX-APL-PMAX-001",
                warrantyCardNumber = "WAR-APL-PMAX-001",
                notes = "Blue Titanium 512GB",
                createdAt = now,
                updatedAt = now
            )
        )
    }
}
