package com.billme.app.data.model

import com.billme.app.data.local.entity.Product
import com.billme.app.data.local.entity.ProductStatus
import kotlinx.datetime.Clock
import java.math.BigDecimal

/**
 * Bulk import models for CSV/Excel product import
 */

/**
 * Result of CSV/Excel parsing
 */
data class BulkImportParseResult(
    val validProducts: List<ProductImportRow>,
    val invalidProducts: List<ProductImportError>,
    val totalRows: Int,
    val validCount: Int,
    val errorCount: Int,
    val warnings: List<String> = emptyList()
)

/**
 * Single row from import file
 */
data class ProductImportRow(
    val rowNumber: Int,
    val productName: String,
    val brand: String,
    val model: String,
    val variant: String? = null,
    val category: String,
    val imei1: String,
    val imei2: String? = null,
    val mrp: BigDecimal? = null,
    val costPrice: BigDecimal,
    val sellingPrice: BigDecimal,
    val currentStock: Int = 1,
    val minStockLevel: Int = 1,
    val barcode: String? = null,
    val supplierName: String? = null,
    val warrantyMonths: Int? = null,
    val description: String? = null,
    val hasWarning: Boolean = false,
    val warningMessage: String? = null
) {
    fun toProduct(supplierId: Long? = null): Product {
        val now = Clock.System.now()
        val warrantyExpiry = warrantyMonths?.let {
            // Add months to current time (simplified - using days approximation)
            kotlinx.datetime.Instant.fromEpochMilliseconds(
                now.toEpochMilliseconds() + (it * 30L * 24 * 60 * 60 * 1000)
            )
        }
        
        return Product(
            productName = productName,
            brand = brand,
            model = model,
            variant = variant,
            category = category,
            imei1 = imei1,
            imei2 = imei2,
            mrp = mrp,
            costPrice = costPrice,
            sellingPrice = sellingPrice,
            productStatus = ProductStatus.IN_STOCK,
            currentStock = currentStock,
            minStockLevel = minStockLevel,
            barcode = barcode,
            supplierId = supplierId,
            warrantyExpiryDate = warrantyExpiry?.toString()?.let { 
                kotlinx.datetime.LocalDate.parse(it.substringBefore('T'))
            },
            description = description,
            purchaseDate = now,
            createdAt = now,
            updatedAt = now,
            isActive = true
        )
    }
}

/**
 * Import error details
 */
data class ProductImportError(
    val rowNumber: Int,
    val rawData: Map<String, String>,
    val errors: List<String>,
    val severity: ImportErrorSeverity = ImportErrorSeverity.ERROR
)

enum class ImportErrorSeverity {
    WARNING,  // Can be imported with manual review
    ERROR,    // Cannot be imported
    CRITICAL  // Duplicate IMEI or major issue
}

/**
 * Import template for CSV/Excel
 */
data class BulkImportTemplate(
    val headers: List<String>,
    val requiredFields: List<String>,
    val optionalFields: List<String>,
    val sampleData: List<Map<String, String>>,
    val validationRules: Map<String, String>
) {
    companion object {
        fun getDefaultTemplate(): BulkImportTemplate {
            return BulkImportTemplate(
                headers = listOf(
                    "Product Name", "Brand", "Model", "Variant", "Category",
                    "IMEI1", "IMEI2", "MRP", "Cost Price", "Selling Price",
                    "Stock", "Min Stock", "Barcode", "Supplier", "Warranty (Months)", "Description"
                ),
                requiredFields = listOf(
                    "Product Name", "Brand", "Model", "Category", 
                    "IMEI1", "Cost Price", "Selling Price"
                ),
                optionalFields = listOf(
                    "Variant", "IMEI2", "MRP", "Stock", "Min Stock", 
                    "Barcode", "Supplier", "Warranty (Months)", "Description"
                ),
                sampleData = listOf(
                    mapOf(
                        "Product Name" to "iPhone 15 Pro",
                        "Brand" to "Apple",
                        "Model" to "iPhone 15 Pro",
                        "Variant" to "256GB Blue Titanium",
                        "Category" to "Smartphones",
                        "IMEI1" to "354886098765432",
                        "IMEI2" to "354886098765433",
                        "MRP" to "139900",
                        "Cost Price" to "125000",
                        "Selling Price" to "134900",
                        "Stock" to "1",
                        "Min Stock" to "1",
                        "Barcode" to "8901234567890",
                        "Supplier" to "Authorized Distributor",
                        "Warranty (Months)" to "12",
                        "Description" to "Latest iPhone with A17 Pro chip"
                    ),
                    mapOf(
                        "Product Name" to "Galaxy S24 Ultra",
                        "Brand" to "Samsung",
                        "Model" to "S24 Ultra",
                        "Variant" to "512GB Titanium Gray",
                        "Category" to "Smartphones",
                        "IMEI1" to "354887098765432",
                        "IMEI2" to "",
                        "MRP" to "129999",
                        "Cost Price" to "115000",
                        "Selling Price" to "124999",
                        "Stock" to "2",
                        "Min Stock" to "1",
                        "Barcode" to "",
                        "Supplier" to "Samsung India",
                        "Warranty (Months)" to "12",
                        "Description" to "Flagship with S Pen"
                    )
                ),
                validationRules = mapOf(
                    "Product Name" to "Required, max 200 characters",
                    "Brand" to "Required, max 100 characters",
                    "Model" to "Required, max 100 characters",
                    "Category" to "Required, max 50 characters",
                    "IMEI1" to "Required, exactly 15 digits, must pass Luhn check",
                    "IMEI2" to "Optional, exactly 15 digits if provided, must pass Luhn check",
                    "Cost Price" to "Required, positive number",
                    "Selling Price" to "Required, positive number, typically >= Cost Price",
                    "Stock" to "Optional, positive integer, default: 1",
                    "Min Stock" to "Optional, positive integer, default: 1"
                )
            )
        }
    }
}

/**
 * Import progress state
 */
data class BulkImportProgress(
    val phase: ImportPhase,
    val processedRows: Int,
    val totalRows: Int,
    val currentItem: String? = null,
    val errors: List<String> = emptyList()
) {
    val progressPercentage: Float
        get() = if (totalRows > 0) (processedRows.toFloat() / totalRows) else 0f
}

enum class ImportPhase {
    IDLE,
    PARSING_FILE,
    VALIDATING_DATA,
    CHECKING_DUPLICATES,
    IMPORTING_PRODUCTS,
    COMPLETED,
    ERROR
}

/**
 * Import configuration
 */
data class BulkImportConfig(
    val skipDuplicateIMEI: Boolean = true,
    val updateExistingProducts: Boolean = false,
    val validatePrices: Boolean = true,
    val autoGenerateBarcode: Boolean = false,
    val defaultCategory: String? = null,
    val defaultSupplier: String? = null,
    val maxRowsPerBatch: Int = 100
)

/**
 * Import result summary
 */
data class BulkImportResult(
    val success: Boolean,
    val importedCount: Int,
    val skippedCount: Int,
    val errorCount: Int,
    val duration: Long, // milliseconds
    val errors: List<ProductImportError>,
    val warnings: List<String>,
    val importedProductIds: List<Long> = emptyList()
)
