package com.billme.app.data.local.converter

import androidx.room.TypeConverter
import com.billme.app.data.local.entity.*
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import java.math.BigDecimal

class Converters {
    
    // BigDecimal converters
    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? {
        return value?.toString()
    }
    
    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? {
        return value?.let { BigDecimal(it) }
    }
    
    // Instant converters (kotlinx.datetime)
    @TypeConverter
    fun fromInstant(value: Instant?): Long? {
        return value?.toEpochMilliseconds()
    }
    
    @TypeConverter
    fun toInstant(value: Long?): Instant? {
        return value?.let { Instant.fromEpochMilliseconds(it) }
    }
    
    // LocalDate converters (kotlinx.datetime)
    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? {
        return value?.toString()
    }
    
    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it) }
    }
    
    // Enum converters
    @TypeConverter
    fun fromDiscountType(value: DiscountType?): String? {
        return value?.name
    }
    
    @TypeConverter
    fun toDiscountType(value: String?): DiscountType? {
        return value?.let { DiscountType.valueOf(it) }
    }
    
    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod?): String? {
        return value?.name
    }
    
    @TypeConverter
    fun toPaymentMethod(value: String?): PaymentMethod? {
        return value?.let { PaymentMethod.valueOf(it) }
    }
    
    @TypeConverter
    fun fromPaymentStatus(value: PaymentStatus?): String? {
        return value?.name
    }
    
    @TypeConverter
    fun toPaymentStatus(value: String?): PaymentStatus? {
        return value?.let { PaymentStatus.valueOf(it) }
    }
    
    @TypeConverter
    fun fromCustomerSegment(value: CustomerSegment?): String? {
        return value?.name
    }
    
    @TypeConverter
    fun toCustomerSegment(value: String?): CustomerSegment? {
        return value?.let { CustomerSegment.valueOf(it) }
    }
    
    @TypeConverter
    fun fromSettingValueType(value: SettingValueType?): String? {
        return value?.name
    }
    
    @TypeConverter
    fun toSettingValueType(value: String?): SettingValueType? {
        return value?.let { SettingValueType.valueOf(it) }
    }
    
    @TypeConverter
    fun fromSettingCategory(value: SettingCategory?): String? {
        return value?.name
    }
    
    @TypeConverter
    fun toSettingCategory(value: String?): SettingCategory? {
        return value?.let { SettingCategory.valueOf(it) }
    }
    
    @TypeConverter
    fun fromProductStatus(value: ProductStatus?): String? {
        return value?.name
    }
    
    @TypeConverter
    fun toProductStatus(value: String?): ProductStatus? {
        return value?.let { ProductStatus.valueOf(it) }
    }
    
    @TypeConverter
    fun fromGSTMode(value: GSTMode?): String? {
        return value?.name
    }
    
    @TypeConverter
    fun toGSTMode(value: String?): GSTMode? {
        return value?.let { GSTMode.valueOf(it) }
    }
    
    @TypeConverter
    fun fromGSTType(value: GSTType?): String? {
        return value?.name
    }
    
    @TypeConverter
    fun toGSTType(value: String?): GSTType? {
        return value?.let { GSTType.valueOf(it) }
    }
    
    @TypeConverter
    fun fromGSTRateCategory(value: GSTRateCategory?): String? {
        return value?.name
    }
    
    @TypeConverter
    fun toGSTRateCategory(value: String?): GSTRateCategory? {
        return value?.let { GSTRateCategory.valueOf(it) }
    }
}
