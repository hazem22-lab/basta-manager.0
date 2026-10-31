package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "debt_payments",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("customerId"), Index("paymentDate")]
)
data class DebtPayment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long,
    val amount: Double,
    val paymentDate: Long = System.currentTimeMillis(),
    val notes: String = "",
    val paymentMethod: String = PaymentMethods.CASH // "CASH", "PALPAY", "JAWWAL_PAY", "BOP"
)

data class CustomerWithDebtSummary(
    val customer: Customer,
    val totalPurchasesOnCredit: Double,
    val totalPaid: Double,
    val remainingBalance: Double
)

typealias CustomerEntity = Customer
typealias DebtPaymentEntity = DebtPayment
typealias DebtEntity = DebtPayment
