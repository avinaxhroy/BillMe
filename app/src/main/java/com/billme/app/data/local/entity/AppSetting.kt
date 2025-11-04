package com.billme.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity(
    tableName = "app_settings",
    indices = [
        Index(value = ["category"]),
        Index(value = ["is_system"])
    ]
)
data class AppSetting(
    @PrimaryKey
    @ColumnInfo(name = "setting_key")
    val settingKey: String,
    
    @ColumnInfo(name = "setting_value")
    val settingValue: String,
    
    @ColumnInfo(name = "value_type")
    val valueType: SettingValueType,
    
    @ColumnInfo(name = "category")
    val category: SettingCategory,
    
    @ColumnInfo(name = "description")
    val description: String,
    
    @ColumnInfo(name = "is_system")
    val isSystem: Boolean = false,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant
)

enum class SettingValueType {
    STRING, NUMBER, BOOLEAN, JSON
}

enum class SettingCategory {
    BUSINESS, PRINTER, APP, SYNC
}

// Default settings for the app
object DefaultSettings {
    val defaults = mapOf(
        "shop_name" to AppSetting(
            settingKey = "shop_name",
            settingValue = "Mobile Shop Pro",
            valueType = SettingValueType.STRING,
            category = SettingCategory.BUSINESS,
            description = "Shop display name",
            updatedAt = kotlinx.datetime.Clock.System.now()
        ),
        "shop_address" to AppSetting(
            settingKey = "shop_address",
            settingValue = "",
            valueType = SettingValueType.STRING,
            category = SettingCategory.BUSINESS,
            description = "Business address",
            updatedAt = kotlinx.datetime.Clock.System.now()
        ),
        "shop_phone" to AppSetting(
            settingKey = "shop_phone",
            settingValue = "",
            valueType = SettingValueType.STRING,
            category = SettingCategory.BUSINESS,
            description = "Contact number",
            updatedAt = kotlinx.datetime.Clock.System.now()
        ),
        "gst_enabled" to AppSetting(
            settingKey = "gst_enabled",
            settingValue = "true",
            valueType = SettingValueType.BOOLEAN,
            category = SettingCategory.BUSINESS,
            description = "Whether GST calculations are active",
            updatedAt = kotlinx.datetime.Clock.System.now()
        ),
        "default_gst_rate" to AppSetting(
            settingKey = "default_gst_rate",
            settingValue = "18.0",
            valueType = SettingValueType.NUMBER,
            category = SettingCategory.BUSINESS,
            description = "Default tax percentage",
            updatedAt = kotlinx.datetime.Clock.System.now()
        ),
        "currency_code" to AppSetting(
            settingKey = "currency_code",
            settingValue = "INR",
            valueType = SettingValueType.STRING,
            category = SettingCategory.BUSINESS,
            description = "Currency code",
            updatedAt = kotlinx.datetime.Clock.System.now()
        ),
        "printer_model" to AppSetting(
            settingKey = "printer_model",
            settingValue = "",
            valueType = SettingValueType.STRING,
            category = SettingCategory.PRINTER,
            description = "Connected printer model",
            updatedAt = kotlinx.datetime.Clock.System.now()
        ),
        "paper_width" to AppSetting(
            settingKey = "paper_width",
            settingValue = "58",
            valueType = SettingValueType.NUMBER,
            category = SettingCategory.PRINTER,
            description = "Printer paper width in mm",
            updatedAt = kotlinx.datetime.Clock.System.now()
        ),
        "auto_backup_enabled" to AppSetting(
            settingKey = "auto_backup_enabled",
            settingValue = "true",
            valueType = SettingValueType.BOOLEAN,
            category = SettingCategory.APP,
            description = "Automatic backup setting",
            updatedAt = kotlinx.datetime.Clock.System.now()
        ),
        "backup_frequency" to AppSetting(
            settingKey = "backup_frequency",
            settingValue = "DAILY",
            valueType = SettingValueType.STRING,
            category = SettingCategory.APP,
            description = "How often to backup",
            updatedAt = kotlinx.datetime.Clock.System.now()
        )
    )
}