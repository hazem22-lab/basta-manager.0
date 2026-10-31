package com.example.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "sales",
    indices = [Index("saleDate"), Index("customerId")]
)
data class Sale(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val saleDate: Long = System.currentTimeMillis(),
    val totalAmount: Double, // إجمالي المبيعات
    val totalCost: Double,   // إجمالي تكلفة البضاعة المباعة
    val profit: Double,      // الربح = totalAmount - totalCost
    val paymentType: String, // "CASH" (نقدي) أو "CREDIT" (آجل / دين)
    val customerId: Long? = null,
    val customerName: String? = null,
    val notes: String = "",
    val isDailyAggregate: Boolean = false // هل تم تسجيلها كإجمالي نهاية اليوم
)

@Entity(
    tableName = "sale_items",
    foreignKeys = [
        ForeignKey(
            entity = Sale::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("saleId"), Index("productId")]
)
data class SaleItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val saleId: Long,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val unitCost: Double,
    val unitPrice: Double,
    val subtotal: Double
) {
    val totalCost: Double
        get() = quantity * unitCost

    val itemProfit: Double
        get() = subtotal - totalCost
}

data class SaleWithItems(
    @Embedded val sale: Sale,
    @Relation(
        parentColumn = "id",
        entityColumn = "saleId"
    )
    val items: List<SaleItem>
)

typealias SaleEntity = Sale
typealias SaleItemEntity = SaleItem
typealias SaleWithItemsEntity = SaleWithItems
