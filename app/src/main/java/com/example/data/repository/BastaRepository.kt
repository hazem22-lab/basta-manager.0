package com.example.data.repository

import com.example.data.local.BastaDao
import com.example.data.model.CashboxConfig
import com.example.data.model.Customer
import com.example.data.model.CustomerWithDebtSummary
import com.example.data.model.DebtPayment
import com.example.data.model.Expense
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.data.model.SaleWithItems
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import com.example.data.model.Category

class BastaRepository(val dao: BastaDao) {

    // === Categories ===
    val allCategories: Flow<List<Category>> = dao.getAllCategories()

    suspend fun insertCategory(category: Category): Long = dao.insertCategory(category)
    suspend fun updateCategory(category: Category) {
        dao.updateCategory(category)
    }
    suspend fun deleteCategory(category: Category) {
        dao.reassignCategory(category.name, "أخرى")
        dao.deleteCategory(category)
    }

    suspend fun initDefaultCategoriesIfEmpty() {
        val existing = dao.dumpCategories()
        if (existing.isEmpty()) {
            val defaults = listOf(
                Category(name = "حلويات", isDefault = true, displayOrder = 1),
                Category(name = "مشروبات", isDefault = true, displayOrder = 2),
                Category(name = "سكاكر وشيبس", isDefault = true, displayOrder = 3),
                Category(name = "بسكويت ومخبوزات", isDefault = true, displayOrder = 4),
                Category(name = "معلبات", isDefault = true, displayOrder = 5),
                Category(name = "مكسرات وموالح", isDefault = true, displayOrder = 6),
                Category(name = "أخرى", isDefault = true, displayOrder = 99)
            )
            for (c in defaults) {
                dao.insertCategory(c)
            }
        }
    }

    // === Products ===
    val allProducts: Flow<List<Product>> = dao.getAllProducts()

    suspend fun insertProduct(product: Product): Long = dao.insertProduct(product)
    suspend fun updateProduct(product: Product) = dao.updateProduct(product)
    suspend fun deleteProduct(product: Product) = dao.deleteProduct(product)
    suspend fun adjustProductStock(productId: Long, delta: Int) = dao.adjustProductStock(productId, delta)
    suspend fun reduceProductStock(productId: Long, quantity: Int): Int = dao.reduceProductStock(productId, quantity)
    suspend fun reduceStock(productId: Long, quantity: Int): Int = dao.reduceStock(productId, quantity)
    suspend fun deductStock(productId: Long, quantity: Int): Int = dao.deductStock(productId, quantity)
    suspend fun getRemainingStock(productId: Long): Int? = dao.getRemainingStock(productId)
    suspend fun getProductStock(productId: Long): Int? = dao.getProductStock(productId)
    suspend fun getStockByProductId(productId: Long): Int? = dao.getStockByProductId(productId)
    fun observeProductStock(productId: Long): Flow<Int?> = dao.observeProductStock(productId)

    // === Sales ===
    val allSalesWithItems: Flow<List<SaleWithItems>> = dao.getAllSalesWithItems()

    suspend fun recordSale(sale: Sale, items: List<SaleItem>) {
        dao.recordSaleWithItems(sale, items)
    }

    suspend fun recordQuickDailySale(sale: Sale) {
        dao.insertSale(sale)
    }

    suspend fun cancelSale(sale: Sale) {
        if (sale.isDailyAggregate) {
            dao.deleteSale(sale)
        } else {
            dao.cancelSaleAndRestoreStock(sale)
        }
    }

    // === Customers & Debts ===
    val allCustomers: Flow<List<Customer>> = dao.getAllCustomers()
    val allDebtPayments: Flow<List<DebtPayment>> = dao.getAllDebtPayments()

    suspend fun insertCustomer(customer: Customer): Long = dao.insertCustomer(customer)
    suspend fun updateCustomer(customer: Customer) = dao.updateCustomer(customer)
    suspend fun deleteCustomer(customer: Customer) = dao.deleteCustomer(customer)

    suspend fun recordDebtPayment(payment: DebtPayment): Long = dao.insertDebtPayment(payment)
    suspend fun deleteDebtPayment(payment: DebtPayment) = dao.deleteDebtPayment(payment)

    fun getPaymentsForCustomer(customerId: Long): Flow<List<DebtPayment>> =
        dao.getPaymentsForCustomer(customerId)

    fun getSalesForCustomer(customerId: Long): Flow<List<Sale>> =
        dao.getSalesForCustomer(customerId)

    // Customers with debt calculations
    val customersWithDebt: Flow<List<CustomerWithDebtSummary>> = combine(
        allCustomers,
        allSalesWithItems,
        allDebtPayments
    ) { customers, salesWithItems, payments ->
        customers.map { customer ->
            val creditSales = salesWithItems
                .filter { it.sale.paymentType == "CREDIT" && it.sale.customerId == customer.id }
                .sumOf { it.sale.totalAmount }
            val customerPayments = payments
                .filter { it.customerId == customer.id }
                .sumOf { it.amount }
            CustomerWithDebtSummary(
                customer = customer,
                totalPurchasesOnCredit = creditSales,
                totalPaid = customerPayments,
                remainingBalance = creditSales - customerPayments
            )
        }
    }

    // === Expenses ===
    val allExpenses: Flow<List<Expense>> = dao.getAllExpenses()

    suspend fun insertExpense(expense: Expense): Long = dao.insertExpense(expense)
    suspend fun updateExpense(expense: Expense) = dao.updateExpense(expense)
    suspend fun deleteExpense(expense: Expense) = dao.deleteExpense(expense)

    // === Cashbox Config ===
    val cashboxConfig: Flow<CashboxConfig?> = dao.getCashboxConfig()

    suspend fun setStartingCash(amount: Double) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
        dao.setCashboxConfig(CashboxConfig(id = 1, startingCash = amount, lastUpdatedDate = todayStr))
    }

    // === Calculations Helper Functions ===
    companion object {
        fun getStartOfDay(timeMillis: Long = System.currentTimeMillis()): Long {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = timeMillis
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            return calendar.timeInMillis
        }

        fun getEndOfDay(timeMillis: Long = System.currentTimeMillis()): Long {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = timeMillis
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            return calendar.timeInMillis
        }

        fun getStartOfWeek(timeMillis: Long = System.currentTimeMillis()): Long {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = timeMillis
            calendar.firstDayOfWeek = Calendar.SATURDAY // الأسبوع يبدأ السبت في العالم العربي
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            return calendar.timeInMillis
        }

        fun getStartOfMonth(timeMillis: Long = System.currentTimeMillis()): Long {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = timeMillis
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            return calendar.timeInMillis
        }
    }

    // === Export CSV & Backup JSON ===
    suspend fun generateSalesCsv(): String {
        val sales = dao.dumpSales()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH)
        val sb = StringBuilder()
        sb.append("\uFEFF") // UTF-8 BOM for Excel Arabic support
        sb.append("رقم الفاتورة,التاريخ والوقت,نوع الدفع,الزبون,إجمالي المبيعات (₪),التكلفة (₪),الربح (₪),ملاحظات\n")
        for (s in sales) {
            val dateStr = dateFormat.format(Date(s.saleDate))
            val pType = if (s.paymentType == "CASH") "نقدي" else "آجل / دين"
            val cust = s.customerName ?: "-"
            val notes = s.notes.replace(",", " ")
            sb.append("${s.id},\"$dateStr\",$pType,\"$cust\",${s.totalAmount},${s.totalCost},${s.profit},\"$notes\"\n")
        }
        return sb.toString()
    }

    suspend fun generateProductsCsv(): String {
        val products = dao.dumpProducts()
        val sb = StringBuilder()
        sb.append("\uFEFF")
        sb.append("رقم المنتج,اسم المنتج,التصنيف,سعر الشراء (₪),سعر البيع (₪),الكمية,قيمة التكلفة (₪),قيمة البيع (₪)\n")
        for (p in products) {
            sb.append("${p.id},\"${p.name}\",\"${p.category}\",${p.costPrice},${p.sellPrice},${p.quantity},${p.totalCostValue},${p.totalRetailValue}\n")
        }
        return sb.toString()
    }

    suspend fun generateDebtsCsv(): String {
        val customers = dao.dumpCustomers()
        val sales = dao.dumpSales()
        val payments = dao.dumpDebtPayments()
        val sb = StringBuilder()
        sb.append("\uFEFF")
        sb.append("رقم الزبون,اسم الزبون,رقم الهاتف,إجمالي المشتريات بالدين (₪),المسدد (₪),الرصيد المتبقي (₪)\n")
        for (c in customers) {
            val totalCredit = sales.filter { it.customerId == c.id && it.paymentType == "CREDIT" }.sumOf { it.totalAmount }
            val totalPaid = payments.filter { it.customerId == c.id }.sumOf { it.amount }
            val remaining = totalCredit - totalPaid
            sb.append("${c.id},\"${c.name}\",\"${c.phone}\",$totalCredit,$totalPaid,$remaining\n")
        }
        return sb.toString()
    }

    suspend fun generateExpensesCsv(): String {
        val expenses = dao.dumpExpenses()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH)
        val sb = StringBuilder()
        sb.append("\uFEFF")
        sb.append("رقم المصروف,البيان,المبلغ (₪),التصنيف,التاريخ,ملاحظات\n")
        for (e in expenses) {
            val dateStr = dateFormat.format(Date(e.expenseDate))
            val notes = e.notes.replace(",", " ")
            sb.append("${e.id},\"${e.title}\",${e.amount},\"${e.category}\",\"$dateStr\",\"$notes\"\n")
        }
        return sb.toString()
    }

    suspend fun createFullJsonBackup(): String {
        val root = JSONObject()
        root.put("app", "BastaManager")
        root.put("version", 1)
        root.put("backupDate", System.currentTimeMillis())

        val productsArr = JSONArray()
        for (p in dao.dumpProducts()) {
            val o = JSONObject()
            o.put("id", p.id)
            o.put("name", p.name)
            o.put("category", p.category)
            o.put("costPrice", p.costPrice)
            o.put("sellPrice", p.sellPrice)
            o.put("quantity", p.quantity)
            o.put("currentStock", p.currentStock)
            o.put("minStockAlert", p.minStockAlert)
            o.put("barcode", p.barcode)
            o.put("notes", p.notes)
            o.put("updatedAt", p.updatedAt)
            productsArr.put(o)
        }
        root.put("products", productsArr)

        val customersArr = JSONArray()
        for (c in dao.dumpCustomers()) {
            val o = JSONObject()
            o.put("id", c.id)
            o.put("name", c.name)
            o.put("phone", c.phone)
            o.put("notes", c.notes)
            o.put("createdAt", c.createdAt)
            customersArr.put(o)
        }
        root.put("customers", customersArr)

        val salesArr = JSONArray()
        for (s in dao.dumpSales()) {
            val o = JSONObject()
            o.put("id", s.id)
            o.put("saleDate", s.saleDate)
            o.put("totalAmount", s.totalAmount)
            o.put("totalCost", s.totalCost)
            o.put("profit", s.profit)
            o.put("paymentType", s.paymentType)
            if (s.customerId != null) o.put("customerId", s.customerId)
            if (s.customerName != null) o.put("customerName", s.customerName)
            o.put("notes", s.notes)
            o.put("isDailyAggregate", s.isDailyAggregate)
            salesArr.put(o)
        }
        root.put("sales", salesArr)

        val saleItemsArr = JSONArray()
        for (si in dao.dumpSaleItems()) {
            val o = JSONObject()
            o.put("id", si.id)
            o.put("saleId", si.saleId)
            o.put("productId", si.productId)
            o.put("productName", si.productName)
            o.put("quantity", si.quantity)
            o.put("unitCost", si.unitCost)
            o.put("unitPrice", si.unitPrice)
            o.put("subtotal", si.subtotal)
            saleItemsArr.put(o)
        }
        root.put("saleItems", saleItemsArr)

        val paymentsArr = JSONArray()
        for (dp in dao.dumpDebtPayments()) {
            val o = JSONObject()
            o.put("id", dp.id)
            o.put("customerId", dp.customerId)
            o.put("amount", dp.amount)
            o.put("paymentDate", dp.paymentDate)
            o.put("notes", dp.notes)
            o.put("paymentMethod", dp.paymentMethod)
            paymentsArr.put(o)
        }
        root.put("debtPayments", paymentsArr)

        val expensesArr = JSONArray()
        for (ex in dao.dumpExpenses()) {
            val o = JSONObject()
            o.put("id", ex.id)
            o.put("title", ex.title)
            o.put("amount", ex.amount)
            o.put("category", ex.category)
            o.put("expenseDate", ex.expenseDate)
            o.put("notes", ex.notes)
            expensesArr.put(o)
        }
        root.put("expenses", expensesArr)

        val config = dao.dumpCashboxConfig()
        if (config != null) {
            val cfgObj = JSONObject()
            cfgObj.put("startingCash", config.startingCash)
            cfgObj.put("lastUpdatedDate", config.lastUpdatedDate)
            root.put("cashboxConfig", cfgObj)
        }

        return root.toString(2)
    }

    suspend fun restoreFromJsonBackup(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)

            val productsList = mutableListOf<Product>()
            val productsArr = root.optJSONArray("products")
            if (productsArr != null) {
                for (i in 0 until productsArr.length()) {
                    val o = productsArr.getJSONObject(i)
                    val qty = o.optInt("quantity", 0)
                    val stock = if (o.has("currentStock")) o.getInt("currentStock") else qty
                    productsList.add(
                        Product(
                            id = o.optLong("id", 0),
                            name = o.optString("name"),
                            category = o.optString("category", "أخرى"),
                            costPrice = o.optDouble("costPrice", 0.0),
                            sellPrice = o.optDouble("sellPrice", 0.0),
                            quantity = qty,
                            currentStock = stock,
                            minStockAlert = o.optInt("minStockAlert", 5),
                            barcode = o.optString("barcode", ""),
                            notes = o.optString("notes", ""),
                            updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            val customersList = mutableListOf<Customer>()
            val customersArr = root.optJSONArray("customers")
            if (customersArr != null) {
                for (i in 0 until customersArr.length()) {
                    val o = customersArr.getJSONObject(i)
                    customersList.add(
                        Customer(
                            id = o.optLong("id", 0),
                            name = o.optString("name"),
                            phone = o.optString("phone", ""),
                            notes = o.optString("notes", ""),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            val salesList = mutableListOf<Sale>()
            val salesArr = root.optJSONArray("sales")
            if (salesArr != null) {
                for (i in 0 until salesArr.length()) {
                    val o = salesArr.getJSONObject(i)
                    salesList.add(
                        Sale(
                            id = o.optLong("id", 0),
                            saleDate = o.optLong("saleDate", System.currentTimeMillis()),
                            totalAmount = o.optDouble("totalAmount", 0.0),
                            totalCost = o.optDouble("totalCost", 0.0),
                            profit = o.optDouble("profit", 0.0),
                            paymentType = o.optString("paymentType", "CASH"),
                            customerId = if (o.has("customerId")) o.getLong("customerId") else null,
                            customerName = if (o.has("customerName")) o.getString("customerName") else null,
                            notes = o.optString("notes", ""),
                            isDailyAggregate = o.optBoolean("isDailyAggregate", false)
                        )
                    )
                }
            }

            val saleItemsList = mutableListOf<SaleItem>()
            val saleItemsArr = root.optJSONArray("saleItems")
            if (saleItemsArr != null) {
                for (i in 0 until saleItemsArr.length()) {
                    val o = saleItemsArr.getJSONObject(i)
                    saleItemsList.add(
                        SaleItem(
                            id = o.optLong("id", 0),
                            saleId = o.optLong("saleId", 0),
                            productId = o.optLong("productId", 0),
                            productName = o.optString("productName"),
                            quantity = o.optInt("quantity", 1),
                            unitCost = o.optDouble("unitCost", 0.0),
                            unitPrice = o.optDouble("unitPrice", 0.0),
                            subtotal = o.optDouble("subtotal", 0.0)
                        )
                    )
                }
            }

            val paymentsList = mutableListOf<DebtPayment>()
            val paymentsArr = root.optJSONArray("debtPayments")
            if (paymentsArr != null) {
                for (i in 0 until paymentsArr.length()) {
                    val o = paymentsArr.getJSONObject(i)
                    paymentsList.add(
                        DebtPayment(
                            id = o.optLong("id", 0),
                            customerId = o.optLong("customerId", 0),
                            amount = o.optDouble("amount", 0.0),
                            paymentDate = o.optLong("paymentDate", System.currentTimeMillis()),
                            notes = o.optString("notes", ""),
                            paymentMethod = o.optString("paymentMethod", "CASH")
                        )
                    )
                }
            }

            val expensesList = mutableListOf<Expense>()
            val expensesArr = root.optJSONArray("expenses")
            if (expensesArr != null) {
                for (i in 0 until expensesArr.length()) {
                    val o = expensesArr.getJSONObject(i)
                    expensesList.add(
                        Expense(
                            id = o.optLong("id", 0),
                            title = o.optString("title"),
                            amount = o.optDouble("amount", 0.0),
                            category = o.optString("category", "أخرى"),
                            expenseDate = o.optLong("expenseDate", System.currentTimeMillis()),
                            notes = o.optString("notes", "")
                        )
                    )
                }
            }

            // Perform atomic restore
            dao.clearAllSaleItems()
            dao.clearAllSales()
            dao.clearAllDebtPayments()
            dao.clearAllCustomers()
            dao.clearAllExpenses()
            dao.clearAllProducts()

            dao.restoreProducts(productsList)
            dao.restoreCustomers(customersList)
            dao.restoreSales(salesList)
            dao.restoreSaleItems(saleItemsList)
            dao.restoreDebtPayments(paymentsList)
            dao.restoreExpenses(expensesList)

            val cfgObj = root.optJSONObject("cashboxConfig")
            if (cfgObj != null) {
                dao.setCashboxConfig(
                    CashboxConfig(
                        id = 1,
                        startingCash = cfgObj.optDouble("startingCash", 0.0),
                        lastUpdatedDate = cfgObj.optString("lastUpdatedDate", "")
                    )
                )
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun clearAllData() {
        dao.clearAllSaleItems()
        dao.clearAllSales()
        dao.clearAllDebtPayments()
        dao.clearAllCustomers()
        dao.clearAllExpenses()
        dao.clearAllProducts()
        dao.setCashboxConfig(CashboxConfig(id = 1, startingCash = 0.0, lastUpdatedDate = ""))
    }
}
