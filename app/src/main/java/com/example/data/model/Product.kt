package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String, // حلويات, مشروبات, سكاكر وشيبس, بسكويت, معلبات, أخرى
    val costPrice: Double, // سعر الشراء / التكلفة
    val sellPrice: Double, // سعر البيع
    val quantity: Int = 0, // الكمية المسجلة
    val currentStock: Int = quantity, // الكمية الحالية في المخزون
    val minStockAlert: Int = 5, // حد التنبيه
    val barcode: String = "",
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isLowStock: Boolean
        get() = currentStock <= minStockAlert

    val isOutOfStock: Boolean
        get() = currentStock <= 0

    val profitMarginPerUnit: Double
        get() = sellPrice - costPrice

    val totalCostValue: Double
        get() = currentStock * costPrice

    val totalRetailValue: Double
        get() = currentStock * sellPrice
}

typealias ProductEntity = Product
