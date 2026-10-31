package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.CashboxConfig
import com.example.data.model.Customer
import com.example.data.model.DebtPayment
import com.example.data.model.Expense
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.data.model.SaleWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface BastaDao {

    // === Products ===
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Long): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("UPDATE products SET currentStock = currentStock + :delta, quantity = quantity + :delta, updatedAt = :timestamp WHERE id = :productId")
    suspend fun adjustProductStock(productId: Long, delta: Int, timestamp: Long = System.currentTimeMillis())

    /**
     * دالة لخصم الكمية التلقائي عند البيع للمنتج.
     * تقوم بإنقاص currentStock و quantity بمقدار الكمية المباعة مع ضمان عدم النزول تحت الصفر.
     */
    @Query("UPDATE products SET currentStock = CASE WHEN currentStock >= :quantity THEN currentStock - :quantity ELSE 0 END, quantity = CASE WHEN quantity >= :quantity THEN quantity - :quantity ELSE 0 END, updatedAt = :timestamp WHERE id = :productId")
    suspend fun reduceProductStock(productId: Long, quantity: Int, timestamp: Long = System.currentTimeMillis()): Int

    @Query("UPDATE products SET currentStock = CASE WHEN currentStock >= :quantity THEN currentStock - :quantity ELSE 0 END, quantity = CASE WHEN quantity >= :quantity THEN quantity - :quantity ELSE 0 END, updatedAt = :timestamp WHERE id = :productId")
    suspend fun reduceStock(productId: Long, quantity: Int, timestamp: Long = System.currentTimeMillis()): Int

    @Query("UPDATE products SET currentStock = CASE WHEN currentStock >= :quantity THEN currentStock - :quantity ELSE 0 END, quantity = CASE WHEN quantity >= :quantity THEN quantity - :quantity ELSE 0 END, updatedAt = :timestamp WHERE id = :productId")
    suspend fun deductStock(productId: Long, quantity: Int, timestamp: Long = System.currentTimeMillis()): Int

    /**
     * دالة لجلب كمية المخزون المتبقية لكل منتج على حدة بواسطة معرف المنتج (productId).
     */
    @Query("SELECT currentStock FROM products WHERE id = :productId")
    suspend fun getRemainingStock(productId: Long): Int?

    @Query("SELECT currentStock FROM products WHERE id = :productId")
    suspend fun getProductStock(productId: Long): Int?

    @Query("SELECT currentStock FROM products WHERE id = :productId")
    suspend fun getStockByProductId(productId: Long): Int?

    @Query("SELECT currentStock FROM products WHERE id = :productId")
    fun observeProductStock(productId: Long): Flow<Int?>

    // === Sales ===
    @Transaction
    @Query("SELECT * FROM sales ORDER BY saleDate DESC")
    fun getAllSalesWithItems(): Flow<List<SaleWithItems>>

    @Transaction
    @Query("SELECT * FROM sales WHERE saleDate >= :startTime AND saleDate <= :endTime ORDER BY saleDate DESC")
    fun getSalesBetweenWithItems(startTime: Long, endTime: Long): Flow<List<SaleWithItems>>

    @Query("SELECT * FROM sales WHERE customerId = :customerId ORDER BY saleDate DESC")
    fun getSalesForCustomer(customerId: Long): Flow<List<Sale>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: Sale): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleItem>)

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getSaleItems(saleId: Long): List<SaleItem>

    @Delete
    suspend fun deleteSale(sale: Sale)

    // Complete sale with automatic inventory deduction
    @Transaction
    suspend fun recordSaleWithItems(sale: Sale, items: List<SaleItem>) {
        val saleId = insertSale(sale)
        val itemsWithId = items.map { it.copy(saleId = saleId) }
        insertSaleItems(itemsWithId)
        for (item in itemsWithId) {
            reduceProductStock(item.productId, item.quantity)
        }
    }

    // Cancel sale and restore inventory
    @Transaction
    suspend fun cancelSaleAndRestoreStock(sale: Sale) {
        val items = getSaleItems(sale.id)
        for (item in items) {
            adjustProductStock(item.productId, item.quantity)
        }
        deleteSale(sale)
    }

    // === Customers & Debts ===
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Long): Customer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Delete
    suspend fun deleteCustomer(customer: Customer)

    // Debt Payments
    @Query("SELECT * FROM debt_payments ORDER BY paymentDate DESC")
    fun getAllDebtPayments(): Flow<List<DebtPayment>>

    @Query("SELECT * FROM debt_payments WHERE customerId = :customerId ORDER BY paymentDate DESC")
    fun getPaymentsForCustomer(customerId: Long): Flow<List<DebtPayment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebtPayment(payment: DebtPayment): Long

    @Delete
    suspend fun deleteDebtPayment(payment: DebtPayment)

    // === Expenses ===
    @Query("SELECT * FROM expenses ORDER BY expenseDate DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE expenseDate >= :startTime AND expenseDate <= :endTime ORDER BY expenseDate DESC")
    fun getExpensesBetween(startTime: Long, endTime: Long): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    // === Cashbox Configuration ===
    @Query("SELECT * FROM cashbox_config WHERE id = 1")
    fun getCashboxConfig(): Flow<CashboxConfig?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setCashboxConfig(config: CashboxConfig)

    // Direct dump for backup / restore
    @Query("SELECT * FROM products")
    suspend fun dumpProducts(): List<Product>

    @Query("SELECT * FROM sales")
    suspend fun dumpSales(): List<Sale>

    @Query("SELECT * FROM sale_items")
    suspend fun dumpSaleItems(): List<SaleItem>

    @Query("SELECT * FROM customers")
    suspend fun dumpCustomers(): List<Customer>

    @Query("SELECT * FROM debt_payments")
    suspend fun dumpDebtPayments(): List<DebtPayment>

    @Query("SELECT * FROM expenses")
    suspend fun dumpExpenses(): List<Expense>

    @Query("SELECT * FROM cashbox_config WHERE id = 1")
    suspend fun dumpCashboxConfig(): CashboxConfig?

    @Query("DELETE FROM products")
    suspend fun clearAllProducts()

    @Query("DELETE FROM sales")
    suspend fun clearAllSales()

    @Query("DELETE FROM sale_items")
    suspend fun clearAllSaleItems()

    @Query("DELETE FROM customers")
    suspend fun clearAllCustomers()

    @Query("DELETE FROM debt_payments")
    suspend fun clearAllDebtPayments()

    @Query("DELETE FROM expenses")
    suspend fun clearAllExpenses()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun restoreProducts(products: List<Product>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun restoreSales(sales: List<Sale>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun restoreSaleItems(items: List<SaleItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun restoreCustomers(customers: List<Customer>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun restoreDebtPayments(payments: List<DebtPayment>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun restoreExpenses(expenses: List<Expense>)

    // === Categories ===
    @Query("SELECT * FROM categories ORDER BY displayOrder ASC, name ASC")
    fun getAllCategories(): Flow<List<com.example.data.model.Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: com.example.data.model.Category): Long

    @Update
    suspend fun updateCategory(category: com.example.data.model.Category)

    @Delete
    suspend fun deleteCategory(category: com.example.data.model.Category)

    @Query("UPDATE products SET category = :newCategory WHERE category = :oldCategory")
    suspend fun reassignCategory(oldCategory: String, newCategory: String)

    @Query("SELECT COUNT(*) FROM products WHERE category = :categoryName")
    suspend fun getProductCountInCategory(categoryName: String): Int

    @Query("SELECT * FROM categories")
    suspend fun dumpCategories(): List<com.example.data.model.Category>

    @Query("DELETE FROM categories")
    suspend fun clearAllCategories()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun restoreCategories(categories: List<com.example.data.model.Category>)
}
