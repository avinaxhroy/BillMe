package com.billme.app.data.repository

import com.billme.app.data.local.dao.TransactionDao
import com.billme.app.data.local.dao.TransactionLineItemDao
import com.billme.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val lineItemDao: TransactionLineItemDao,
    private val productRepository: ProductRepository,
    private val settingsRepository: SettingsRepository
) {
    
    fun getAllCompletedTransactions(): Flow<List<Transaction>> = 
        transactionDao.getAllCompletedTransactions()
    
    fun getAllDraftTransactions(): Flow<List<Transaction>> = 
        transactionDao.getAllDraftTransactions()
    
    suspend fun getTransactionById(transactionId: Long): Transaction? = 
        transactionDao.getTransactionById(transactionId)
    
    fun getTransactionsByDateRange(startDate: Instant, endDate: Instant): Flow<List<Transaction>> =
        transactionDao.getTransactionsByDateRange(startDate, endDate)
    
    fun getTransactionsByCustomer(phoneNumber: String): Flow<List<Transaction>> =
        transactionDao.getTransactionsByCustomer(phoneNumber)
    
    fun getRecentTransactions(limit: Int = 10): Flow<List<Transaction>> =
        transactionDao.getRecentTransactions(limit)
    
    /**
     * Get line items for a transaction
     */
    fun getTransactionLineItems(transactionId: Long): Flow<List<TransactionLineItem>> =
        lineItemDao.getLineItemsByTransactionId(transactionId)
    
    /**
     * Creates a new transaction (draft or completed)
     */
    suspend fun createTransaction(
        customerPhone: String? = null,
        customerName: String? = null,
        cartItems: List<CartItem>,
        discountAmount: BigDecimal = BigDecimal.ZERO,
        discountType: DiscountType = DiscountType.AMOUNT,
        paymentMethod: PaymentMethod,
        notes: String? = null,
        isDraft: Boolean = false
    ): Result<Long> {
        return try {
            if (cartItems.isEmpty()) {
                return Result.failure(Exception("Cart cannot be empty"))
            }
            
            // Validate all products exist and have sufficient stock
            for (item in cartItems) {
                val product = productRepository.getProductById(item.productId)
                    ?: return Result.failure(Exception("Product not found: ${item.productName}"))
                
                if (product.currentStock < item.quantity) {
                    return Result.failure(Exception("Insufficient stock for ${product.productName}. Available: ${product.currentStock}, Required: ${item.quantity}"))
                }
            }
            
            // Calculate totals
            val subtotal = cartItems.sumOf { it.lineTotal }
            val taxRate = if (settingsRepository.isGstEnabled()) {
                settingsRepository.getDefaultGstRate()
            } else {
                0.0
            }
            
            val finalDiscountAmount = when (discountType) {
                DiscountType.PERCENT -> subtotal * (discountAmount / BigDecimal(100))
                DiscountType.AMOUNT -> discountAmount
            }
            
            val taxableAmount = subtotal - finalDiscountAmount
            val taxAmount = taxableAmount * BigDecimal(taxRate / 100)
            val grandTotal = taxableAmount + taxAmount
            val totalProfit = cartItems.sumOf { it.lineProfit }
            
            // Generate transaction number
            val transactionNumber = generateTransactionNumber()
            
            // Create transaction
            val transaction = Transaction(
                transactionNumber = transactionNumber,
                customerPhone = customerPhone,
                customerName = customerName,
                transactionDate = Clock.System.now(),
                subtotal = subtotal,
                discountAmount = finalDiscountAmount,
                discountType = discountType,
                taxAmount = taxAmount.setScale(2, RoundingMode.HALF_UP),
                taxRate = taxRate,
                grandTotal = grandTotal.setScale(2, RoundingMode.HALF_UP),
                paymentMethod = paymentMethod,
                paymentStatus = if (isDraft) PaymentStatus.PENDING else PaymentStatus.PAID,
                profitAmount = totalProfit.setScale(2, RoundingMode.HALF_UP),
                notes = notes,
                isDraft = isDraft,
                createdAt = Clock.System.now()
            )
            
            val transactionId = transactionDao.insertTransaction(transaction)
            
            // Create line items
            val lineItems = cartItems.map { item ->
                TransactionLineItem(
                    transactionId = transactionId,
                    productId = item.productId,
                    productName = item.productName,
                    imeiSold = item.imeiSold,
                    quantity = item.quantity,
                    unitCost = item.unitCost,
                    unitSellingPrice = item.unitSellingPrice,
                    lineTotal = item.lineTotal,
                    lineProfit = item.lineProfit,
                    serialNumber = item.serialNumber
                )
            }
            
            lineItemDao.insertLineItems(lineItems)
            
            // Reduce stock if not a draft
            if (!isDraft) {
                for (item in cartItems) {
                    productRepository.reduceStock(item.productId, item.quantity)
                }
            }
            
            Result.success(transactionId)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Completes a draft transaction
     */
    suspend fun completeDraftTransaction(transactionId: Long): Result<Unit> {
        return try {
            val transaction = transactionDao.getTransactionById(transactionId)
                ?: return Result.failure(Exception("Transaction not found"))
            
            if (!transaction.isDraft) {
                return Result.failure(Exception("Transaction is already completed"))
            }
            
            // Get line items to reduce stock
            val lineItems = lineItemDao.getLineItemsByTransactionIdSync(transactionId)
            
            // Reduce stock for each item
            for (item in lineItems) {
                productRepository.reduceStock(item.productId, item.quantity)
            }
            
            // Mark as completed
            transactionDao.completeDraftTransaction(transactionId)
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Updates a draft transaction
     */
    suspend fun updateDraftTransaction(
        transactionId: Long,
        cartItems: List<CartItem>,
        discountAmount: BigDecimal = BigDecimal.ZERO,
        discountType: DiscountType = DiscountType.AMOUNT,
        customerPhone: String? = null,
        customerName: String? = null,
        notes: String? = null
    ): Result<Unit> {
        return try {
            val existingTransaction = transactionDao.getTransactionById(transactionId)
                ?: return Result.failure(Exception("Transaction not found"))
            
            if (!existingTransaction.isDraft) {
                return Result.failure(Exception("Cannot update completed transaction"))
            }
            
            // Delete existing line items
            lineItemDao.deleteLineItemsByTransactionId(transactionId)
            
            // Recalculate totals
            val subtotal = cartItems.sumOf { it.lineTotal }
            val taxRate = if (settingsRepository.isGstEnabled()) {
                settingsRepository.getDefaultGstRate()
            } else {
                0.0
            }
            
            val finalDiscountAmount = when (discountType) {
                DiscountType.PERCENT -> subtotal * (discountAmount / BigDecimal(100))
                DiscountType.AMOUNT -> discountAmount
            }
            
            val taxableAmount = subtotal - finalDiscountAmount
            val taxAmount = taxableAmount * BigDecimal(taxRate / 100)
            val grandTotal = taxableAmount + taxAmount
            val totalProfit = cartItems.sumOf { it.lineProfit }
            
            // Update transaction
            val updatedTransaction = existingTransaction.copy(
                customerPhone = customerPhone,
                customerName = customerName,
                subtotal = subtotal,
                discountAmount = finalDiscountAmount,
                discountType = discountType,
                taxAmount = taxAmount.setScale(2, RoundingMode.HALF_UP),
                taxRate = taxRate,
                grandTotal = grandTotal.setScale(2, RoundingMode.HALF_UP),
                profitAmount = totalProfit.setScale(2, RoundingMode.HALF_UP),
                notes = notes
            )
            
            transactionDao.updateTransaction(updatedTransaction)
            
            // Create new line items
            val lineItems = cartItems.map { item ->
                TransactionLineItem(
                    transactionId = transactionId,
                    productId = item.productId,
                    productName = item.productName,
                    imeiSold = item.imeiSold,
                    quantity = item.quantity,
                    unitCost = item.unitCost,
                    unitSellingPrice = item.unitSellingPrice,
                    lineTotal = item.lineTotal,
                    lineProfit = item.lineProfit,
                    serialNumber = item.serialNumber
                )
            }
            
            lineItemDao.insertLineItems(lineItems)
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Mark receipt as printed
     */
    suspend fun markReceiptPrinted(transactionId: Long): Result<Unit> {
        return try {
            transactionDao.markReceiptPrinted(transactionId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete a draft transaction
     */
    suspend fun deleteDraftTransaction(transactionId: Long): Result<Unit> {
        return try {
            val transaction = transactionDao.getTransactionById(transactionId)
                ?: return Result.failure(Exception("Transaction not found"))
            
            if (!transaction.isDraft) {
                return Result.failure(Exception("Cannot delete completed transaction"))
            }
            
            transactionDao.deleteTransaction(transaction)
            Result.success(Unit)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get daily sales summary
     */
    suspend fun getDailySummary(date: Instant = Clock.System.now()): DailySummary {
        return DailySummary(
            sales = transactionDao.getDailySales(date),
            profit = transactionDao.getDailyProfit(date),
            transactionCount = transactionDao.getDailyTransactionCount(date)
        )
    }
    
    /**
     * Get yesterday's sales summary
     */
    suspend fun getYesterdaySummary(): DailySummary {
        val yesterday = Clock.System.now().minus(kotlin.time.Duration.parse("1d"))
        return getDailySummary(yesterday)
    }
    
    /**
     * Get sales data for the last 7 days
     */
    suspend fun getLast7DaysSales(): List<Double> {
        val salesList = mutableListOf<Double>()
        val now = Clock.System.now()
        
        // Get sales for each of the last 7 days
        for (i in 6 downTo 0) {
            val date = now.minus(kotlin.time.Duration.parse("${i}d"))
            val sales = transactionDao.getDailySales(date)
            salesList.add(sales.toDouble())
        }
        
        return salesList
    }
    
    /**
     * Generate transaction number
     */
    private suspend fun generateTransactionNumber(): String {
        val year = Clock.System.now().toString().substring(0, 4)
        val lastNumber = transactionDao.getLastTransactionNumber() ?: 0
        val nextNumber = (lastNumber + 1).toString().padStart(3, '0')
        return "TXN-$year-$nextNumber"
    }
    
    /**
     * Clean up old draft transactions
     */
    suspend fun cleanupOldDrafts(olderThanDays: Int = 7) {
        val duration = kotlin.time.Duration.parse("${olderThanDays}d")
        val cutoffDate = Clock.System.now().minus(duration)
        transactionDao.deleteOldDrafts(cutoffDate)
    }
}

/**
 * Cart item for transaction creation
 */
data class CartItem(
    val productId: Long,
    val productName: String,
    val imeiSold: String,
    val quantity: Int = 1,
    val unitCost: BigDecimal,
    val unitSellingPrice: BigDecimal,
    val serialNumber: String? = null
) {
    val lineTotal: BigDecimal = unitSellingPrice * BigDecimal(quantity)
    val lineProfit: BigDecimal = (unitSellingPrice - unitCost) * BigDecimal(quantity)
}

/**
 * Daily sales summary
 */
data class DailySummary(
    val sales: BigDecimal,
    val profit: BigDecimal,
    val transactionCount: Int
)