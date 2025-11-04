package com.billme.app.data.repository

import com.billme.app.data.local.dao.TransactionDao
import com.billme.app.data.local.dao.TransactionLineItemDao
import com.billme.app.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for device-related operations
 */
@Singleton
class DeviceRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val transactionLineItemDao: TransactionLineItemDao
) {
    
    /**
     * Check if device exists in transaction records
     */
    suspend fun checkDeviceExists(imei: String): Boolean {
        return transactionLineItemDao.checkIMEIExists(imei)
    }
    
    /**
     * Get device transaction history by IMEI
     */
    suspend fun getDeviceHistory(imei: String): List<DeviceTransaction> {
        return transactionLineItemDao.getTransactionsByIMEI(imei).map { transaction ->
            DeviceTransaction(
                transactionId = transaction.transactionId,
                imei = imei,
                transactionDate = transaction.createdAt,
                customerName = transaction.customerName ?: "Unknown",
                salePrice = transaction.totalAmount,
                status = "SOLD"
            )
        }
    }
    
    /**
     * Record device sale
     */
    suspend fun recordDeviceSale(
        imei: String,
        transactionId: Long,
        salePrice: BigDecimal
    ) {
        // This would typically update a device tracking table
        // For now, we'll rely on transaction line items with IMEI
    }
    
    /**
     * Get devices sold in date range
     */
    suspend fun getDevicesSoldInRange(
        startDate: Instant,
        endDate: Instant
    ): List<com.billme.app.data.repository.DeviceSale> {
        val daoList = transactionLineItemDao.getDeviceSalesInRange(startDate, endDate)
        return daoList.map { dao ->
            com.billme.app.data.repository.DeviceSale(
                imei = dao.imei,
                productName = dao.productName,
                salePrice = dao.salePrice,
                saleDate = dao.saleDate,
                customerName = dao.customerName
            )
        }
    }
    
    /**
     * Validate device IMEI format
     */
    fun validateIMEI(imei: String): Boolean {
        if (imei.length != 15) return false
        if (!imei.all { it.isDigit() }) return false
        
        // Luhn algorithm check for IMEI validation
        var sum = 0
        var alternate = false
        
        for (i in imei.length - 1 downTo 0) {
            var digit = imei[i].digitToInt()
            
            if (alternate) {
                digit *= 2
                if (digit > 9) {
                    digit = (digit % 10) + 1
                }
            }
            
            sum += digit
            alternate = !alternate
        }
        
        return sum % 10 == 0
    }
}

/**
 * Data classes for device operations
 */
data class DeviceTransaction(
    val transactionId: Long,
    val imei: String,
    val transactionDate: Instant,
    val customerName: String,
    val salePrice: BigDecimal,
    val status: String
)

data class DeviceSale(
    val imei: String,
    val productName: String,
    val saleDate: Instant,
    val salePrice: BigDecimal,
    val customerName: String?
)