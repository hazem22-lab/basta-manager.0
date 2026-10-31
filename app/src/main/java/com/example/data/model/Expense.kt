package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    indices = [Index("expenseDate")]
)
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String, // البيان
    val amount: Double, // المبلغ
    val category: String, // بضاعة ومشتريات, إيجار وكهرباء, نقل ومواصلات, أكياس وتغليف, شخصي ويومية, أخرى
    val expenseDate: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(tableName = "cashbox_config")
data class CashboxConfig(
    @PrimaryKey
    val id: Int = 1,
    val startingCash: Double = 0.0, // رصيد الصندوق عند بداية اليوم
    val lastUpdatedDate: String = "" // YYYY-MM-DD
)

typealias ExpenseEntity = Expense
typealias CashboxConfigEntity = CashboxConfig
