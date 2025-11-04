package com.billme.app.data.repository

import com.billme.app.core.util.GSTValidator
import com.billme.app.data.local.dao.*
import com.billme.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GSTRepository @Inject constructor(
    private val gstConfigDao: GSTConfigurationDao,
    private val gstRateDao: GSTRateDao,
    private val gstDetailsDao: InvoiceGSTDetailsDao
) {
    
    // GST Configuration Management
    
    fun getCurrentGSTConfiguration(): Flow<GSTConfiguration?> = 
        gstConfigDao.getCurrentGSTConfigurationFlow()
    
    suspend fun getCurrentGSTConfigurationSync(): GSTConfiguration? = 
        gstConfigDao.getCurrentGSTConfiguration()
    
    suspend fun getGSTConfigurationById(configId: Long): GSTConfiguration? = 
        gstConfigDao.getGSTConfigurationById(configId)
    
    suspend fun getGSTConfigurationByGSTIN(gstin: String): GSTConfiguration? = 
        gstConfigDao.getGSTConfigurationByGSTIN(gstin)
    
    fun getAllGSTConfigurations(): Flow<List<GSTConfiguration>> = 
        gstConfigDao.getAllGSTConfigurations()
    
    /**
     * Create or update GST configuration with validation
     */
    suspend fun saveGSTConfiguration(config: GSTConfiguration): Result<Long> {
        return try {
            // Validate GSTIN if provided
            config.shopGSTIN?.let { gstin ->
                val validation = GSTValidator.validateGSTINWithDetails(gstin)
                if (!validation.isValid) {
                    return Result.failure(Exception("Invalid GSTIN: ${validation.errorMessage}"))
                }
            }
            
            val now = Clock.System.now()
            val configToSave = config.copy(
                updatedAt = now,
                createdAt = if (config.configId == 0L) now else config.createdAt
            )
            
            val configId = gstConfigDao.setActiveGSTConfiguration(configToSave)
            Result.success(configId)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Initialize default GST configuration
     */
    suspend fun initializeDefaultGSTConfiguration(): Result<Long> {
        val existing = getCurrentGSTConfigurationSync()
        if (existing != null) {
            return Result.success(existing.configId)
        }
        
        val now = Clock.System.now()
        val defaultConfig = GSTConfiguration(
            defaultGSTMode = GSTMode.NO_GST,
            allowPerInvoiceMode = true,
            defaultGSTRate = 18.0,
            defaultGSTCategory = GSTRateCategory.GST_18,
            showGSTINOnInvoice = true,
            showGSTSummary = true,
            includeGSTInPrice = false,
            roundOffGST = true,
            enableGSTValidation = true,
            requireCustomerGSTIN = false,
            autoDetectInterstate = true,
            hsnCodeMandatory = false,
            createdAt = now,
            updatedAt = now
        )
        
        return saveGSTConfiguration(defaultConfig)
    }
    
    // GST Rate Management
    
    fun getAllActiveGSTRates(): Flow<List<GSTRate>> = gstRateDao.getAllActiveGSTRates()
    
    fun getGSTRatesByCategory(category: String): Flow<List<GSTRate>> = 
        gstRateDao.getGSTRatesByCategory(category)
    
    suspend fun getCurrentGSTRateByHSN(hsnCode: String): GSTRate? = 
        gstRateDao.getCurrentGSTRateByHSN(hsnCode)
    
    fun getGSTRatesByGSTCategory(category: GSTRateCategory): Flow<List<GSTRate>> = 
        gstRateDao.getGSTRatesByGSTCategory(category)
    
    suspend fun getEffectiveGSTRate(
        category: String,
        subcategory: String? = null,
        currentTime: Instant = Clock.System.now()
    ): GSTRate? = gstRateDao.getEffectiveGSTRate(category, subcategory, currentTime)
    
    fun getAllCategories(): Flow<List<String>> = gstRateDao.getAllCategories()
    
    fun getAllUniqueGSTRates(): Flow<List<Double>> = gstRateDao.getAllUniqueGSTRates()
    
    fun getAllHSNCodes(): Flow<List<String>> = gstRateDao.getAllHSNCodes()
    
    /**
     * Add GST rate with validation
     */
    suspend fun addGSTRate(rate: GSTRate): Result<Long> {
        return try {
            // Validate GST rate
            if (!GSTValidator.isValidGSTRate(rate.gstRate)) {
                return Result.failure(Exception("Invalid GST rate: ${rate.gstRate}%"))
            }
            
            // Validate HSN code if provided
            rate.hsnCode?.let { hsn ->
                if (!GSTValidator.isValidHSNCode(hsn)) {
                    return Result.failure(Exception("Invalid HSN code: $hsn"))
                }
            }
            
            val now = Clock.System.now()
            val rateToSave = rate.copy(
                createdAt = now,
                updatedAt = now,
                effectiveFrom = if (rate.effectiveFrom == Instant.DISTANT_PAST) now else rate.effectiveFrom
            )
            
            val rateId = gstRateDao.insertGSTRate(rateToSave)
            Result.success(rateId)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update GST rate
     */
    suspend fun updateGSTRate(rate: GSTRate): Result<Unit> {
        return try {
            if (!GSTValidator.isValidGSTRate(rate.gstRate)) {
                return Result.failure(Exception("Invalid GST rate: ${rate.gstRate}%"))
            }
            
            val updatedRate = rate.copy(updatedAt = Clock.System.now())
            gstRateDao.updateGSTRate(updatedRate)
            Result.success(Unit)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete GST rate
     */
    suspend fun deleteGSTRate(rateId: Long): Result<Unit> {
        return try {
            gstRateDao.deleteGSTRateById(rateId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Initialize default GST rates
     */
    suspend fun initializeDefaultGSTRates(): Result<Unit> {
        return try {
            val now = Clock.System.now()
            val defaultRates = listOf(
                // Mobile phones - 18% GST
                GSTRate(
                    category = "Mobile",
                    subcategory = "Smartphone",
                    hsnCode = "8517",
                    gstRate = 18.0,
                    gstCategory = GSTRateCategory.GST_18,
                    description = "Mobile Phones and Smartphones",
                    effectiveFrom = now,
                    createdAt = now,
                    updatedAt = now
                ),
                // Mobile accessories - 18% GST
                GSTRate(
                    category = "Accessory",
                    subcategory = "Charger",
                    hsnCode = "8544",
                    gstRate = 18.0,
                    gstCategory = GSTRateCategory.GST_18,
                    description = "Mobile Chargers and Cables",
                    effectiveFrom = now,
                    createdAt = now,
                    updatedAt = now
                ),
                // Mobile cases - 12% GST
                GSTRate(
                    category = "Accessory",
                    subcategory = "Case",
                    hsnCode = "3926",
                    gstRate = 12.0,
                    gstCategory = GSTRateCategory.GST_12,
                    description = "Mobile Cases and Covers",
                    effectiveFrom = now,
                    createdAt = now,
                    updatedAt = now
                ),
                // Screen protectors - 18% GST
                GSTRate(
                    category = "Accessory",
                    subcategory = "Screen Protector",
                    hsnCode = "3920",
                    gstRate = 18.0,
                    gstCategory = GSTRateCategory.GST_18,
                    description = "Screen Guards and Protectors",
                    effectiveFrom = now,
                    createdAt = now,
                    updatedAt = now
                )
            )
            
            gstRateDao.insertGSTRates(defaultRates)
            Result.success(Unit)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Invoice GST Details Management
    
    suspend fun getGSTDetailsByTransactionId(transactionId: Long): InvoiceGSTDetails? = 
        gstDetailsDao.getGSTDetailsByTransactionId(transactionId)
    
    fun getGSTDetailsByTransactionIdFlow(transactionId: Long): Flow<InvoiceGSTDetails?> = 
        gstDetailsDao.getGSTDetailsByTransactionIdFlow(transactionId)
    
    fun getGSTDetailsByMode(gstMode: GSTMode): Flow<List<InvoiceGSTDetails>> = 
        gstDetailsDao.getGSTDetailsByMode(gstMode)
    
    fun getGSTDetailsForPeriod(
        shopGSTIN: String,
        startDate: Instant,
        endDate: Instant
    ): Flow<List<InvoiceGSTDetails>> = 
        gstDetailsDao.getGSTDetailsForPeriod(shopGSTIN, startDate, endDate)
    
    suspend fun getTotalGSTForPeriod(
        shopGSTIN: String,
        startDate: Instant,
        endDate: Instant
    ): Double = gstDetailsDao.getTotalGSTForPeriod(shopGSTIN, startDate, endDate) ?: 0.0
    
    suspend fun getGSTBreakdownForPeriod(
        shopGSTIN: String,
        startDate: Instant,
        endDate: Instant
    ): GSTSummary? = gstDetailsDao.getGSTBreakdownForPeriod(shopGSTIN, startDate, endDate)
    
    suspend fun getGSTTransactionCountForPeriod(
        startDate: Instant,
        endDate: Instant
    ): Int = gstDetailsDao.getGSTTransactionCountForPeriod(startDate, endDate)
    
    fun getGSTDetailsByState(
        isInterstate: Boolean,
        startDate: Instant,
        endDate: Instant
    ): Flow<List<InvoiceGSTDetails>> = 
        gstDetailsDao.getGSTDetailsByState(isInterstate, startDate, endDate)
    
    /**
     * Save GST details for an invoice
     */
    suspend fun saveGSTDetails(details: InvoiceGSTDetails): Result<Long> {
        return try {
            // Validate GST details
            if (details.gstMode != GSTMode.NO_GST) {
                if (details.shopGSTIN.isNullOrBlank()) {
                    return Result.failure(Exception("Shop GSTIN is required for GST transactions"))
                }
                
                if (!GSTValidator.isValidGSTIN(details.shopGSTIN)) {
                    return Result.failure(Exception("Invalid shop GSTIN"))
                }
                
                details.customerGSTIN?.let { customerGSTIN ->
                    if (!GSTValidator.isValidGSTIN(customerGSTIN)) {
                        return Result.failure(Exception("Invalid customer GSTIN"))
                    }
                }
            }
            
            val detailsId = gstDetailsDao.insertGSTDetails(details)
            Result.success(detailsId)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update GST details
     */
    suspend fun updateGSTDetails(details: InvoiceGSTDetails): Result<Unit> {
        return try {
            gstDetailsDao.updateGSTDetails(details)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete GST details by transaction ID
     */
    suspend fun deleteGSTDetailsByTransactionId(transactionId: Long): Result<Unit> {
        return try {
            gstDetailsDao.deleteGSTDetailsByTransactionId(transactionId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Analytics and Reporting
    
    suspend fun getGSTModeAnalytics(
        startDate: Instant,
        endDate: Instant
    ): List<GSTModeAnalytics> = gstDetailsDao.getGSTModeAnalytics(startDate, endDate)
    
    suspend fun getStateWiseGSTAnalytics(
        startDate: Instant,
        endDate: Instant
    ): List<StateWiseGSTAnalytics> = gstDetailsDao.getStateWiseGSTAnalytics(startDate, endDate)
    
    /**
     * Get comprehensive GST report
     */
    suspend fun getGSTReport(
        shopGSTIN: String,
        startDate: Instant,
        endDate: Instant
    ): GSTReport {
        val gstDetails = gstDetailsDao.getGSTDetailsForPeriod(shopGSTIN, startDate, endDate)
        val gstBreakdown = getGSTBreakdownForPeriod(shopGSTIN, startDate, endDate)
        val totalGST = getTotalGSTForPeriod(shopGSTIN, startDate, endDate)
        val transactionCount = getGSTTransactionCountForPeriod(startDate, endDate)
        val modeAnalytics = getGSTModeAnalytics(startDate, endDate)
        val stateAnalytics = getStateWiseGSTAnalytics(startDate, endDate)
        
        return GSTReport(
            shopGSTIN = shopGSTIN,
            periodStart = startDate,
            periodEnd = endDate,
            totalGSTAmount = totalGST,
            transactionCount = transactionCount,
            gstBreakdown = gstBreakdown,
            modeAnalytics = modeAnalytics,
            stateAnalytics = stateAnalytics,
            generatedAt = Clock.System.now()
        )
    }
    
    /**
     * Validate GST configuration
     */
    suspend fun validateGSTConfiguration(config: GSTConfiguration): List<String> {
        val errors = mutableListOf<String>()
        
        // Validate GSTIN
        config.shopGSTIN?.let { gstin ->
            val validation = GSTValidator.validateGSTINWithDetails(gstin)
            if (!validation.isValid) {
                errors.add("Invalid GSTIN: ${validation.errorMessage}")
            }
        }
        
        // Validate GST rate
        if (!GSTValidator.isValidGSTRate(config.defaultGSTRate)) {
            errors.add("Invalid default GST rate: ${config.defaultGSTRate}%")
        }
        
        // Business logic validations
        if (config.defaultGSTMode != GSTMode.NO_GST && config.shopGSTIN.isNullOrBlank()) {
            errors.add("GSTIN is required when GST mode is not NO_GST")
        }
        
        if (config.requireCustomerGSTIN && config.defaultGSTMode == GSTMode.NO_GST) {
            errors.add("Cannot require customer GSTIN when GST mode is NO_GST")
        }
        
        return errors
    }
    
    // Convenience methods for EnhancedBillingService compatibility
    
    /**
     * Get active GST configuration wrapped in a value object
     * Used by EnhancedBillingService
     */
    suspend fun getActiveConfiguration(): GSTConfigurationValue {
        val config = getCurrentGSTConfigurationSync()
        return GSTConfigurationValue(config)
    }
    
    /**
     * Get GST rate for a specific product by HSN code
     * Used by EnhancedBillingService
     */
    suspend fun getGSTRateForProduct(
        productId: Long,
        hsnCode: String? = null
    ): GSTRate? {
        // First try to get by HSN code if provided
        if (!hsnCode.isNullOrBlank()) {
            val rateByHSN = getCurrentGSTRateByHSN(hsnCode)
            if (rateByHSN != null) {
                return rateByHSN
            }
        }
        
        // Fall back to getting default effective GST rate
        return getEffectiveGSTRate("Standard") ?: gstRateDao.getRateByPercentage(18.0)
    }
    
    /**
     * Get default GST rate for a category
     * Used by EnhancedBillingService
     */
    suspend fun getDefaultGSTRate(category: GSTRateCategory): GSTRate? {
        // Get rates by category and return the first one
        val rates = gstRateDao.getRatesByCategory(category.name)
        return rates.firstOrNull() ?: gstRateDao.getRateByPercentage(
            when (category) {
                GSTRateCategory.EXEMPT -> 0.0
                GSTRateCategory.GST_5 -> 5.0
                GSTRateCategory.GST_12 -> 12.0
                GSTRateCategory.GST_18 -> 18.0
                GSTRateCategory.GST_28 -> 28.0
                GSTRateCategory.CUSTOM -> 18.0
            }
        )
    }
    
    // Convenience methods for InvoiceRepository compatibility
    
    /**
     * Save invoice GST details - convenience wrapper
     */
    suspend fun saveInvoiceGSTDetails(details: InvoiceGSTDetails): Result<Long> {
        return saveGSTDetails(details)
    }
    
    /**
     * Update invoice GST details - convenience wrapper
     */
    suspend fun updateInvoiceGSTDetails(details: InvoiceGSTDetails): Result<Unit> {
        return updateGSTDetails(details)
    }
    
    /**
     * Get invoice GST details by invoice ID
     */
    suspend fun getInvoiceGSTDetails(invoiceId: Long): InvoiceGSTDetails? {
        return gstDetailsDao.getGSTDetailsByTransactionId(invoiceId)
    }
}

/**
 * Comprehensive GST Report
 */
data class GSTReport(
    val shopGSTIN: String,
    val periodStart: Instant,
    val periodEnd: Instant,
    val totalGSTAmount: Double,
    val transactionCount: Int,
    val gstBreakdown: GSTSummary?,
    val modeAnalytics: List<GSTModeAnalytics>,
    val stateAnalytics: List<StateWiseGSTAnalytics>,
    val generatedAt: Instant
)

/**
 * Wrapper for GSTConfiguration to provide .value property access
 */
data class GSTConfigurationValue(
    val value: GSTConfiguration?
)
