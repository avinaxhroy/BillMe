package com.billme.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant
import java.math.BigDecimal

/**
 * GST Modes for flexible GST handling
 */
enum class GSTMode {
    FULL_GST,           // Show complete GST breakdown (CGST/SGST/IGST)
    PARTIAL_GST,        // Hide GST from customer, show in owner reports
    GST_REFERENCE,      // Show only GSTIN, no tax calculations
    NO_GST;             // Disable GST completely
    
    fun getDisplayName(): String {
        return when (this) {
            FULL_GST -> "Full GST Mode"
            PARTIAL_GST -> "Partial GST (Hidden from Customer)"
            GST_REFERENCE -> "GST Reference Only"
            NO_GST -> "No GST"
        }
    }
    
    fun getDescription(): String {
        return when (this) {
            FULL_GST -> "Show complete GST breakdown with CGST, SGST, IGST on all invoices"
            PARTIAL_GST -> "Apply GST internally but hide from customer invoice"
            GST_REFERENCE -> "No GST calculation, only show GSTIN as reference"
            NO_GST -> "Completely remove GST from invoices"
        }
    }
    
    fun shouldApplyGST(): Boolean {
        return this == FULL_GST || this == PARTIAL_GST
    }
    
    fun shouldShowGSTOnCustomerInvoice(): Boolean {
        return this == FULL_GST
    }
    
    fun shouldShowGSTIN(): Boolean {
        return this != NO_GST
    }
}

/**
 * GST Types for Indian taxation system
 */
enum class GSTType {
    CGST,         // Central GST (Intra-state)
    SGST,         // State GST (Intra-state)  
    CGST_SGST,    // Combined CGST+SGST (Intra-state) - for UI selection
    IGST,         // Integrated GST (Inter-state)
    CESS          // Compensation Cess (if applicable)
}

/**
 * GST Rate categories
 */
enum class GSTRateCategory {
    EXEMPT,      // 0% GST
    GST_5,       // 5% GST
    GST_12,      // 12% GST  
    GST_18,      // 18% GST
    GST_28,      // 28% GST
    CUSTOM       // Custom rate
}

/**
 * Display name extension property for GSTRateCategory
 */
val GSTRateCategory.displayName: String
    get() = when (this) {
        GSTRateCategory.EXEMPT -> "Exempt (0%)"
        GSTRateCategory.GST_5 -> "GST 5%"
        GSTRateCategory.GST_12 -> "GST 12%"
        GSTRateCategory.GST_18 -> "GST 18%"
        GSTRateCategory.GST_28 -> "GST 28%"
        GSTRateCategory.CUSTOM -> "Custom Rate"
    }

/**
 * GST Configuration Entity
 * Stores shop-level GST settings and preferences
 */
@Entity(
    tableName = "gst_configuration",
    indices = [
        Index(value = ["shop_gstin"], unique = true),
        Index(value = ["is_active"])
    ]
)
data class GSTConfiguration(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "config_id")
    val configId: Long = 0,
    
    // Shop GST Details
    @ColumnInfo(name = "shop_gstin")
    val shopGSTIN: String? = null,
    
    @ColumnInfo(name = "shop_legal_name")
    val shopLegalName: String? = null,
    
    @ColumnInfo(name = "shop_trade_name")
    val shopTradeName: String? = null,
    
    @ColumnInfo(name = "shop_state_code")
    val shopStateCode: String? = null,
    
    @ColumnInfo(name = "shop_state_name")
    val shopStateName: String? = null,
    
    // GST Configuration
    @ColumnInfo(name = "default_gst_mode")
    val defaultGSTMode: GSTMode = GSTMode.NO_GST,
    
    @ColumnInfo(name = "allow_per_invoice_mode")
    val allowPerInvoiceMode: Boolean = false,
    
    @ColumnInfo(name = "default_gst_rate")
    val defaultGSTRate: Double = 18.0,
    
    @ColumnInfo(name = "default_gst_category")
    val defaultGSTCategory: GSTRateCategory = GSTRateCategory.GST_18,
    
    // Invoice Settings
    @ColumnInfo(name = "show_gstin_on_invoice")
    val showGSTINOnInvoice: Boolean = true,
    
    @ColumnInfo(name = "show_gst_summary")
    val showGSTSummary: Boolean = true,
    
    @ColumnInfo(name = "include_gst_in_price")
    val includeGSTInPrice: Boolean = false,
    
    @ColumnInfo(name = "round_off_gst")
    val roundOffGST: Boolean = true,
    
    // Compliance Settings
    @ColumnInfo(name = "enable_gst_validation")
    val enableGSTValidation: Boolean = true,
    
    @ColumnInfo(name = "require_customer_gstin")
    val requireCustomerGSTIN: Boolean = false,
    
    @ColumnInfo(name = "auto_detect_interstate")
    val autoDetectInterstate: Boolean = true,
    
    @ColumnInfo(name = "hsn_code_mandatory")
    val hsnCodeMandatory: Boolean = false,
    
    // Metadata
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant,
    
    @ColumnInfo(name = "updated_by")
    val updatedBy: String = "system"
) {
    /**
     * Check if shop is GST registered
     */
    val isGSTRegistered: Boolean
        get() = !shopGSTIN.isNullOrBlank()
    
    /**
     * Get shop state code from GSTIN
     */
    val stateCodeFromGSTIN: String?
        get() = shopGSTIN?.take(2)
    
    /**
     * Check if configuration allows GST calculations
     */
    val allowsGSTCalculation: Boolean
        get() = defaultGSTMode != GSTMode.NO_GST && isGSTRegistered
    
    /**
     * Check if GST should be shown to customers
     */
    val showGSTToCustomer: Boolean
        get() = when (defaultGSTMode) {
            GSTMode.FULL_GST, GSTMode.GST_REFERENCE -> true
            GSTMode.PARTIAL_GST, GSTMode.NO_GST -> false
        }
}

/**
 * GST Rate Configuration Entity
 * Stores product category-wise GST rates
 */
@Entity(
    tableName = "gst_rates",
    indices = [
        Index(value = ["category", "hsn_code"], unique = true),
        Index(value = ["is_active"])
    ]
)
data class GSTRate(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "rate_id")
    val rateId: Long = 0,
    
    @ColumnInfo(name = "category")
    val category: String,
    
    @ColumnInfo(name = "subcategory")
    val subcategory: String? = null,
    
    @ColumnInfo(name = "hsn_code")
    val hsnCode: String? = null,
    
    @ColumnInfo(name = "gst_rate")
    val gstRate: Double,
    
    @ColumnInfo(name = "gst_category")
    val gstCategory: GSTRateCategory,
    
    @ColumnInfo(name = "cgst_rate")
    val cgstRate: Double? = null,
    
    @ColumnInfo(name = "sgst_rate")
    val sgstRate: Double? = null,
    
    @ColumnInfo(name = "igst_rate")
    val igstRate: Double? = null,
    
    @ColumnInfo(name = "cess_rate")
    val cessRate: Double = 0.0,
    
    @ColumnInfo(name = "description")
    val description: String? = null,
    
    @ColumnInfo(name = "effective_from")
    val effectiveFrom: Instant,
    
    @ColumnInfo(name = "effective_to")
    val effectiveTo: Instant? = null,
    
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant
) {
    /**
     * Calculate CGST and SGST rates (for intra-state)
     */
    fun getCGSTSGSTRates(): Pair<Double, Double> {
        return when {
            cgstRate != null && sgstRate != null -> Pair(cgstRate, sgstRate)
            else -> {
                val halfRate = gstRate / 2.0
                Pair(halfRate, halfRate)
            }
        }
    }
    
    /**
     * Get IGST rate (for inter-state)
     */
    fun getIGSTRate(): Double {
        return igstRate ?: gstRate
    }
    
    /**
     * Check if rate is currently effective
     */
    val isCurrentlyEffective: Boolean
        get() {
            val now = kotlinx.datetime.Clock.System.now()
            return now >= effectiveFrom && (effectiveTo == null || now <= effectiveTo)
        }
}

/**
 * Invoice GST Details Entity
 * Stores GST breakdown for each invoice
 */
@Entity(
    tableName = "invoice_gst_details",
    indices = [
        Index(value = ["transaction_id"], unique = true),
        Index(value = ["gst_mode"])
    ]
)
data class InvoiceGSTDetails(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "gst_detail_id")
    val gstDetailId: Long = 0,
    
    @ColumnInfo(name = "transaction_id")
    val transactionId: Long,
    
    // GST Configuration for this invoice
    @ColumnInfo(name = "gst_mode")
    val gstMode: GSTMode,
    
    @ColumnInfo(name = "shop_gstin")
    val shopGSTIN: String? = null,
    
    @ColumnInfo(name = "customer_gstin")
    val customerGSTIN: String? = null,
    
    @ColumnInfo(name = "is_interstate")
    val isInterstate: Boolean = false,
    
    // GST Calculations
    @ColumnInfo(name = "taxable_amount")
    val taxableAmount: BigDecimal,
    
    @ColumnInfo(name = "cgst_amount")
    val cgstAmount: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "sgst_amount")
    val sgstAmount: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "igst_amount")
    val igstAmount: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "cess_amount")
    val cessAmount: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "total_gst_amount")
    val totalGSTAmount: BigDecimal,
    
    @ColumnInfo(name = "round_off_amount")
    val roundOffAmount: BigDecimal = BigDecimal.ZERO,
    
    // Breakdown by rates
    @ColumnInfo(name = "gst_rate_breakdown")
    val gstRateBreakdown: String? = null, // JSON string of rate-wise breakdown
    
    @ColumnInfo(name = "hsn_summary")
    val hsnSummary: String? = null, // JSON string of HSN-wise summary
    
    @ColumnInfo(name = "created_at")
    val createdAt: Instant
) {
    /**
     * Get total tax amount including cess
     */
    val totalTaxAmount: BigDecimal
        get() = totalGSTAmount + cessAmount + roundOffAmount
    
    /**
     * Get effective GST rate
     */
    val effectiveGSTRate: Double
        get() = if (taxableAmount > BigDecimal.ZERO) {
            (totalGSTAmount / taxableAmount * BigDecimal(100)).toDouble()
        } else 0.0
    
    /**
     * Check if this is a valid GST transaction
     */
    val isValidGSTTransaction: Boolean
        get() = gstMode != GSTMode.NO_GST && 
                !shopGSTIN.isNullOrBlank() && 
                taxableAmount > BigDecimal.ZERO
}