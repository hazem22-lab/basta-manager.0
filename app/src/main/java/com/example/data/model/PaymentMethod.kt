package com.example.data.model

import androidx.compose.ui.graphics.Color

object PaymentMethods {
    const val CASH = "CASH"                 // كاش (نقدي)
    const val PALPAY = "PALPAY"             // بال بي (PalPay)
    const val JAWWAL_PAY = "JAWWAL_PAY"     // جوال بي (Jawwal Pay)
    const val BOP = "BOP"                   // بنك فلسطين (Bank of Palestine)
    const val CREDIT = "CREDIT"             // آجل (دين)

    fun getDisplayName(method: String): String {
        return when (method) {
            CASH -> "كاش (نقدي)"
            PALPAY -> "بال بي (PalPay)"
            JAWWAL_PAY -> "جوال بي (Jawwal Pay)"
            BOP -> "بنك فلسطين (BOP)"
            CREDIT -> "آجل (دين)"
            else -> method
        }
    }

    fun getShortName(method: String): String {
        return when (method) {
            CASH -> "كاش"
            PALPAY -> "بال بي"
            JAWWAL_PAY -> "جوال بي"
            BOP -> "بنك فلسطين"
            CREDIT -> "دين"
            else -> method
        }
    }

    fun isElectronic(method: String): Boolean {
        return method == PALPAY || method == JAWWAL_PAY || method == BOP
    }

    fun isCash(method: String): Boolean = method == CASH
    fun isCredit(method: String): Boolean = method == CREDIT

    // Palestinian payment brand colors
    val ColorCash = Color(0xFF1E5E48)       // Deep emerald
    val ColorCashContainer = Color(0xFFE6F4EA)

    val ColorPalPay = Color(0xFF0284C7)     // PalPay corporate blue
    val ColorPalPayContainer = Color(0xFFE0F2FE)

    val ColorJawwalPay = Color(0xFFEA580C)  // Jawwal Pay orange
    val ColorJawwalPayContainer = Color(0xFFFFEDD5)

    val ColorBop = Color(0xFFDC2626)        // Bank of Palestine red
    val ColorBopContainer = Color(0xFFFEE2E2)

    val ColorCredit = Color(0xFF7C3AED)     // Debt purple
    val ColorCreditContainer = Color(0xFFF3E8FF)

    fun getColor(method: String): Color {
        return when (method) {
            CASH -> ColorCash
            PALPAY -> ColorPalPay
            JAWWAL_PAY -> ColorJawwalPay
            BOP -> ColorBop
            CREDIT -> ColorCredit
            else -> Color(0xFF475569)
        }
    }

    fun getContainerColor(method: String): Color {
        return when (method) {
            CASH -> ColorCashContainer
            PALPAY -> ColorPalPayContainer
            JAWWAL_PAY -> ColorJawwalPayContainer
            BOP -> ColorBopContainer
            CREDIT -> ColorCreditContainer
            else -> Color(0xFFF1F5F9)
        }
    }

    val saleMethods = listOf(CASH, PALPAY, JAWWAL_PAY, BOP, CREDIT)
    val debtPaymentMethods = listOf(CASH, PALPAY, JAWWAL_PAY, BOP)
}
