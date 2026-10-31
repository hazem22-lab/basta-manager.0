package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Customer
import com.example.data.model.CustomerWithDebtSummary
import com.example.data.model.Product
import com.example.data.model.Sale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("مدير البسطة", appName)
    }

    @Test
    fun `main activity launches without crash`() {
        val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
        org.junit.Assert.assertNotNull(controller.get())
    }

    @Test
    fun `product profit margin and valuation calculations with currentStock`() {
        val product = Product(
            id = 1,
            name = "عصير مانجو",
            category = "مشروبات",
            costPrice = 3.5,
            sellPrice = 5.0,
            quantity = 20,
            currentStock = 15,
            minStockAlert = 5
        )

        assertEquals(15, product.currentStock)
        assertEquals(1.5, product.profitMarginPerUnit, 0.001)
        assertEquals(52.5, product.totalCostValue, 0.001)
        assertEquals(75.0, product.totalRetailValue, 0.001)
        assertTrue(!product.isLowStock)
    }

    @Test
    fun `product entity typealias compatibility and default stock assignment`() {
        val entity: com.example.data.local.entity.ProductEntity = Product(
            id = 2,
            name = "بسكويت شاي",
            category = "بسكويت",
            costPrice = 1.0,
            sellPrice = 2.0,
            quantity = 10
        )
        // currentStock defaults to quantity when not specified
        assertEquals(10, entity.currentStock)
    }

    @Test
    fun `customer debt balance calculations`() {
        val customer = Customer(id = 1, name = "أبو محمد")
        val summary = CustomerWithDebtSummary(
            customer = customer,
            totalPurchasesOnCredit = 150.0,
            totalPaid = 50.0,
            remainingBalance = 100.0
        )

        assertEquals(100.0, summary.remainingBalance, 0.001)
    }

    @Test
    fun `verify room entities structures for sales, inventory, debts and expenses`() {
        // Sales entity
        val sale = com.example.data.local.entity.SaleEntity(
            id = 101,
            totalAmount = 50.0,
            totalCost = 30.0,
            profit = 20.0,
            paymentType = "CASH"
        )
        assertEquals(20.0, sale.profit, 0.001)

        // Expense entity
        val expense = com.example.data.local.entity.ExpenseEntity(
            id = 201,
            title = "فاتورة كهرباء",
            amount = 80.0,
            category = "إيجار وكهرباء"
        )
        assertEquals(80.0, expense.amount, 0.001)

        // Debt payment entity
        val debt = com.example.data.local.entity.DebtPaymentEntity(
            id = 301,
            customerId = 1,
            amount = 50.0,
            paymentMethod = "PALPAY"
        )
        assertEquals(50.0, debt.amount, 0.001)

        // Database DAO accessors
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.example.data.local.BastaDatabase.getDatabase(context)
        org.junit.Assert.assertNotNull(db.saleDao())
        org.junit.Assert.assertNotNull(db.productDao())
        org.junit.Assert.assertNotNull(db.debtDao())
        org.junit.Assert.assertNotNull(db.expenseDao())
    }
}
