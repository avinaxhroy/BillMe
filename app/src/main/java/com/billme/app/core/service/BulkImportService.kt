package com.billme.app.core.service

import android.content.Context
import android.net.Uri
import com.billme.app.core.util.ImeiValidator
import com.billme.app.data.local.BillMeDatabase
import com.billme.app.data.local.dao.ProductDao
import com.billme.app.data.local.dao.StockAdjustmentDao
import com.billme.app.data.local.entity.StockAdjustment
import com.billme.app.data.local.entity.StockAdjustmentReason
import com.billme.app.data.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import java.io.BufferedReader
import java.io.InputStreamReader
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bulk import service for CSV/Excel product imports
 * Uses lazy initialization to avoid circular dependency with BillMeDatabase
 */
@Singleton
class BulkImportService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: BillMeDatabase
) {
    
    private val productDao: ProductDao = database.productDao()
    private val stockAdjustmentDao: StockAdjustmentDao = database.stockAdjustmentDao()
    
    private val _importProgress = MutableStateFlow(BulkImportProgress(ImportPhase.IDLE, 0, 0))
    val importProgress: StateFlow<BulkImportProgress> = _importProgress.asStateFlow()
    
    /**
     * Parse CSV file from URI
     */
    suspend fun parseCSVFile(
        uri: Uri,
        config: BulkImportConfig = BulkImportConfig()
    ): BulkImportParseResult = withContext(Dispatchers.IO) {
        _importProgress.value = BulkImportProgress(ImportPhase.PARSING_FILE, 0, 0)
        
        val validProducts = mutableListOf<ProductImportRow>()
        val invalidProducts = mutableListOf<ProductImportError>()
        val warnings = mutableListOf<String>()
        
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val lines = reader.readLines()
                    
                    if (lines.isEmpty()) {
                        return@withContext BulkImportParseResult(
                            emptyList(), 
                            listOf(ProductImportError(0, emptyMap(), listOf("File is empty"))),
                            0, 0, 1
                        )
                    }
                    
                    // Parse header
                    val header = lines[0].split(",").map { it.trim().replace("\"", "") }
                    val dataLines = lines.drop(1)
                    
                    _importProgress.value = BulkImportProgress(
                        ImportPhase.VALIDATING_DATA, 
                        0, 
                        dataLines.size
                    )
                    
                    // Process each row
                    dataLines.forEachIndexed { index, line ->
                        val rowNumber = index + 2 // +2 because header is row 1 and index starts at 0
                        
                        try {
                            val values = parseCSVLine(line)
                            val rowData = header.zip(values).toMap()
                            
                            val validationResult = validateRow(rowNumber, rowData, config)
                            
                            when (validationResult) {
                                is RowValidationResult.Valid -> {
                                    validProducts.add(validationResult.product)
                                    if (validationResult.product.hasWarning) {
                                        warnings.add("Row $rowNumber: ${validationResult.product.warningMessage}")
                                    }
                                }
                                is RowValidationResult.Invalid -> {
                                    invalidProducts.add(validationResult.error)
                                }
                            }
                        } catch (e: Exception) {
                            invalidProducts.add(
                                ProductImportError(
                                    rowNumber,
                                    emptyMap(),
                                    listOf("Parse error: ${e.message}")
                                )
                            )
                        }
                        
                        _importProgress.value = BulkImportProgress(
                            ImportPhase.VALIDATING_DATA,
                            index + 1,
                            dataLines.size,
                            "Validating row $rowNumber..."
                        )
                    }
                }
            }
            
            BulkImportParseResult(
                validProducts = validProducts,
                invalidProducts = invalidProducts,
                totalRows = validProducts.size + invalidProducts.size,
                validCount = validProducts.size,
                errorCount = invalidProducts.size,
                warnings = warnings
            )
            
        } catch (e: Exception) {
            BulkImportParseResult(
                emptyList(),
                listOf(ProductImportError(0, emptyMap(), listOf("File read error: ${e.message}"))),
                0, 0, 1
            )
        }
    }
    
    /**
     * Parse CSV line handling quoted values
     */
    private fun parseCSVLine(line: String): List<String> {
        val values = mutableListOf<String>()
        var currentValue = StringBuilder()
        var inQuotes = false
        
        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    values.add(currentValue.toString().trim())
                    currentValue = StringBuilder()
                }
                else -> currentValue.append(char)
            }
        }
        values.add(currentValue.toString().trim())
        
        return values
    }
    
    /**
     * Validate a single row
     */
    private fun validateRow(
        rowNumber: Int,
        rowData: Map<String, String>,
        config: BulkImportConfig
    ): RowValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // Extract and validate required fields
        val productName = rowData["Product Name"]?.takeIf { it.isNotBlank() }
            ?: return RowValidationResult.Invalid(
                ProductImportError(rowNumber, rowData, listOf("Product Name is required"))
            )
        
        val brand = rowData["Brand"]?.takeIf { it.isNotBlank() }
            ?: return RowValidationResult.Invalid(
                ProductImportError(rowNumber, rowData, listOf("Brand is required"))
            )
        
        val model = rowData["Model"]?.takeIf { it.isNotBlank() }
            ?: return RowValidationResult.Invalid(
                ProductImportError(rowNumber, rowData, listOf("Model is required"))
            )
        
        val category = rowData["Category"]?.takeIf { it.isNotBlank() }
            ?: config.defaultCategory
            ?: return RowValidationResult.Invalid(
                ProductImportError(rowNumber, rowData, listOf("Category is required"))
            )
        
        val imei1 = rowData["IMEI1"]?.takeIf { it.isNotBlank() }
            ?: return RowValidationResult.Invalid(
                ProductImportError(rowNumber, rowData, listOf("IMEI1 is required"))
            )
        
        // Validate IMEI1
        val imei1Validation = ImeiValidator.validateImei(imei1)
        if (!imei1Validation.isValid) {
            errors.add("Invalid IMEI1: ${imei1Validation.errorMessage}")
        }
        
        // Validate IMEI2 if provided
        val imei2 = rowData["IMEI2"]?.takeIf { it.isNotBlank() }
        if (imei2 != null) {
            val imei2Validation = ImeiValidator.validateImei(imei2)
            if (!imei2Validation.isValid) {
                errors.add("Invalid IMEI2: ${imei2Validation.errorMessage}")
            }
            if (imei2 == imei1) {
                errors.add("IMEI1 and IMEI2 cannot be the same")
            }
        }
        
        // Validate prices
        val costPrice = try {
            BigDecimal(rowData["Cost Price"] ?: "0")
        } catch (e: Exception) {
            errors.add("Invalid Cost Price")
            BigDecimal.ZERO
        }
        
        val sellingPrice = try {
            BigDecimal(rowData["Selling Price"] ?: "0")
        } catch (e: Exception) {
            errors.add("Invalid Selling Price")
            BigDecimal.ZERO
        }
        
        if (costPrice <= BigDecimal.ZERO) {
            errors.add("Cost Price must be greater than 0")
        }
        
        if (sellingPrice <= BigDecimal.ZERO) {
            errors.add("Selling Price must be greater than 0")
        }
        
        if (config.validatePrices && sellingPrice < costPrice) {
            warnings.add("Selling Price is less than Cost Price (potential loss)")
        }
        
        // Parse optional fields
        val variant = rowData["Variant"]?.takeIf { it.isNotBlank() }
        val mrp = try {
            rowData["MRP"]?.takeIf { it.isNotBlank() }?.let { BigDecimal(it) }
        } catch (e: Exception) {
            warnings.add("Invalid MRP, will be ignored")
            null
        }
        
        val stock = try {
            rowData["Stock"]?.takeIf { it.isNotBlank() }?.toInt() ?: 1
        } catch (e: Exception) {
            warnings.add("Invalid Stock, using default: 1")
            1
        }
        
        val minStock = try {
            rowData["Min Stock"]?.takeIf { it.isNotBlank() }?.toInt() ?: 1
        } catch (e: Exception) {
            warnings.add("Invalid Min Stock, using default: 1")
            1
        }
        
        val barcode = rowData["Barcode"]?.takeIf { it.isNotBlank() }
        val supplierName = rowData["Supplier"]?.takeIf { it.isNotBlank() }
        val warrantyMonths = try {
            rowData["Warranty (Months)"]?.takeIf { it.isNotBlank() }?.toInt()
        } catch (e: Exception) {
            null
        }
        val description = rowData["Description"]?.takeIf { it.isNotBlank() }
        
        // Return result
        if (errors.isNotEmpty()) {
            return RowValidationResult.Invalid(
                ProductImportError(rowNumber, rowData, errors)
            )
        }
        
        return RowValidationResult.Valid(
            ProductImportRow(
                rowNumber = rowNumber,
                productName = productName,
                brand = brand,
                model = model,
                variant = variant,
                category = category,
                imei1 = imei1Validation.cleanImei!!,
                imei2 = imei2?.let { ImeiValidator.cleanImei(it) },
                mrp = mrp,
                costPrice = costPrice,
                sellingPrice = sellingPrice,
                currentStock = stock,
                minStockLevel = minStock,
                barcode = barcode,
                supplierName = supplierName,
                warrantyMonths = warrantyMonths,
                description = description,
                hasWarning = warnings.isNotEmpty(),
                warningMessage = warnings.joinToString("; ")
            )
        )
    }
    
    /**
     * Import products to database
     */
    suspend fun importProducts(
        products: List<ProductImportRow>,
        config: BulkImportConfig = BulkImportConfig()
    ): BulkImportResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val importedIds = mutableListOf<Long>()
        val errors = mutableListOf<ProductImportError>()
        var skippedCount = 0
        
        _importProgress.value = BulkImportProgress(
            ImportPhase.CHECKING_DUPLICATES,
            0,
            products.size
        )
        
        products.forEachIndexed { index, row ->
            try {
                // Check for duplicate IMEI
                val existingProduct = productDao.getProductByImei(row.imei1)
                
                if (existingProduct != null) {
                    if (config.skipDuplicateIMEI) {
                        skippedCount++
                        errors.add(
                            ProductImportError(
                                row.rowNumber,
                                emptyMap(),
                                listOf("Duplicate IMEI1: ${row.imei1} (Skipped)"),
                                ImportErrorSeverity.WARNING
                            )
                        )
                    } else if (config.updateExistingProducts) {
                        // Update existing product
                        val updated = existingProduct.copy(
                            productName = row.productName,
                            brand = row.brand,
                            model = row.model,
                            variant = row.variant,
                            category = row.category,
                            costPrice = row.costPrice,
                            sellingPrice = row.sellingPrice,
                            currentStock = row.currentStock,
                            updatedAt = Clock.System.now()
                        )
                        productDao.updateProduct(updated)
                        importedIds.add(existingProduct.productId)
                    }
                } else {
                    // Insert new product
                    val product = row.toProduct()
                    val productId = productDao.insertProduct(product)
                    importedIds.add(productId)
                    
                    // Create stock adjustment record
                    if (row.currentStock > 0) {
                        stockAdjustmentDao.insertAdjustment(
                            StockAdjustment(
                                productId = productId,
                                previousStock = 0,
                                newStock = row.currentStock,
                                adjustmentQuantity = row.currentStock,
                                reason = StockAdjustmentReason.INITIAL_STOCK,
                                notes = "Imported via bulk import",
                                adjustedBy = "System",
                                adjustmentDate = Clock.System.now()
                            )
                        )
                    }
                }
                
                _importProgress.value = BulkImportProgress(
                    ImportPhase.IMPORTING_PRODUCTS,
                    index + 1,
                    products.size,
                    "Importing ${row.productName}..."
                )
                
            } catch (e: Exception) {
                errors.add(
                    ProductImportError(
                        row.rowNumber,
                        emptyMap(),
                        listOf("Import failed: ${e.message}"),
                        ImportErrorSeverity.ERROR
                    )
                )
            }
        }
        
        val duration = System.currentTimeMillis() - startTime
        
        _importProgress.value = BulkImportProgress(ImportPhase.COMPLETED, products.size, products.size)
        
        BulkImportResult(
            success = errors.none { it.severity == ImportErrorSeverity.ERROR },
            importedCount = importedIds.size,
            skippedCount = skippedCount,
            errorCount = errors.count { it.severity == ImportErrorSeverity.ERROR },
            duration = duration,
            errors = errors,
            warnings = errors.filter { it.severity == ImportErrorSeverity.WARNING }.map { 
                "Row ${it.rowNumber}: ${it.errors.joinToString()}" 
            },
            importedProductIds = importedIds
        )
    }
    
    /**
     * Generate CSV template
     */
    fun generateCSVTemplate(): String {
        val template = BulkImportTemplate.getDefaultTemplate()
        val sb = StringBuilder()
        
        // Header
        sb.appendLine(template.headers.joinToString(","))
        
        // Sample data
        template.sampleData.forEach { row ->
            val values = template.headers.map { header ->
                val value = row[header] ?: ""
                if (value.contains(",")) "\"$value\"" else value
            }
            sb.appendLine(values.joinToString(","))
        }
        
        return sb.toString()
    }
}

private sealed class RowValidationResult {
    data class Valid(val product: ProductImportRow) : RowValidationResult()
    data class Invalid(val error: ProductImportError) : RowValidationResult()
}
