package com.billme.app.data.repository.mappers

import com.billme.app.data.local.dao.*
import com.billme.app.data.repository.*
import java.math.BigDecimal

/**
 * Small mapping helpers converting DAO DTOs to repository models.
 * Add mappings here incrementally as the migration proceeds.
 */
object DaoMappers {
    fun CategorySalesInfo.toCategorySales(): CategorySales = CategorySales(
        category = this.category ?: "Uncategorized",
        totalSales = this.totalSales,
        quantity = this.quantity,
        averagePrice = this.averagePrice
    )

    fun ProductSalesInfo.toProductSales(): ProductSales = ProductSales(
        productId = this.productId,
        productName = this.productName,
        totalSales = this.totalSales,
        quantitySold = this.quantitySold,
        averagePrice = this.averagePrice
    )

    fun DeviceSaleData.toDeviceSale(): com.billme.app.data.repository.DeviceSale = com.billme.app.data.repository.DeviceSale(
        imei = this.imei,
        productName = this.productName,
        saleDate = this.saleDate,
        salePrice = this.salePrice,
        customerName = this.customerName
    )

    fun TopSellingProductData.toProductSales(): com.billme.app.data.repository.ProductSales = com.billme.app.data.repository.ProductSales(
        productId = this.productId,
        productName = this.productName,
        totalSales = this.totalRevenue,
        quantitySold = this.totalQuantity.toInt(),
        averagePrice = this.averagePrice
    )
}
