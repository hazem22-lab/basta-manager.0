package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.BastaDatabase
import com.example.data.model.CashboxConfig
import com.example.data.model.Customer
import com.example.data.model.CustomerWithDebtSummary
import com.example.data.model.DebtPayment
import com.example.data.model.Expense
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.data.model.SaleWithItems
import com.example.data.repository.BastaRepository
import android.content.Context
import com.example.data.model.Category
import com.example.data.model.PaymentMethods
import com.example.util.AutoBackupManager
import com.example.util.PdfReportGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class ReportPeriod(val title: String) {
    TODAY("اليوم"),
    THIS_WEEK("هذا الأسبوع"),
    THIS_MONTH("هذا الشهر"),
    ALL_TIME("الكل")
}

data class CartItem(
    val product: Product,
    val quantity: Int
) {
    val subtotal: Double get() = quantity * product.sellPrice
    val totalCost: Double get() = quantity * product.costPrice
    val profit: Double get() = subtotal - totalCost
}

data class DashboardStats(
    val todaySalesTotal: Double = 0.0,
    val todayCashSales: Double = 0.0,
    val todayElectronicSales: Double = 0.0,
    val todayPalPaySales: Double = 0.0,
    val todayJawwalPaySales: Double = 0.0,
    val todayBopSales: Double = 0.0,
    val todayCreditSales: Double = 0.0,
    val todayExpensesTotal: Double = 0.0,
    val todayNetProfit: Double = 0.0,
    val cashInBox: Double = 0.0,
    val startingCash: Double = 0.0,
    val lowStockCount: Int = 0,
    val totalDebtsOutstanding: Double = 0.0
)

data class PeriodReportStats(
    val totalRevenue: Double = 0.0,
    val cashRevenue: Double = 0.0,
    val electronicRevenue: Double = 0.0,
    val creditRevenue: Double = 0.0,
    val costOfGoodsSold: Double = 0.0,
    val grossProfit: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netProfit: Double = 0.0,
    val salesCount: Int = 0
)

class BastaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BastaRepository

    // Auto-Backup State Flows
    private val _autoBackupEnabled = MutableStateFlow(AutoBackupManager.isAutoBackupEnabled(application))
    val autoBackupEnabled: StateFlow<Boolean> = _autoBackupEnabled.asStateFlow()

    private val _lastAutoBackupTime = MutableStateFlow(AutoBackupManager.getLastBackupTime(application))
    val lastAutoBackupTime: StateFlow<Long> = _lastAutoBackupTime.asStateFlow()

    private val _autoBackupFiles = MutableStateFlow(AutoBackupManager.getAutoBackupFiles(application))
    val autoBackupFiles: StateFlow<List<File>> = _autoBackupFiles.asStateFlow()

    init {
        val database = BastaDatabase.getDatabase(application)
        repository = BastaRepository(database.bastaDao())
        viewModelScope.launch {
            repository.initDefaultCategoriesIfEmpty()
            refreshAutoBackupList()
            // Periodic / Startup auto backup check
            AutoBackupManager.performAutoBackup(application, repository)
            refreshAutoBackupList()
        }
    }

    private fun triggerAutoBackup() {
        viewModelScope.launch {
            AutoBackupManager.performAutoBackup(getApplication(), repository)
            refreshAutoBackupList()
        }
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        AutoBackupManager.setAutoBackupEnabled(getApplication(), enabled)
        _autoBackupEnabled.value = enabled
        if (enabled) {
            runAutoBackupNow()
        }
    }

    fun refreshAutoBackupList() {
        _autoBackupFiles.value = AutoBackupManager.getAutoBackupFiles(getApplication())
        _lastAutoBackupTime.value = AutoBackupManager.getLastBackupTime(getApplication())
    }

    fun runAutoBackupNow() {
        viewModelScope.launch {
            val file = AutoBackupManager.performAutoBackup(getApplication(), repository, force = true)
            if (file != null) {
                refreshAutoBackupList()
                showMessage("تم إنشاء نسخة احتياطية تلقائية بنجاح")
            } else {
                showMessage("فشل إنشاء النسخة الاحتياطية")
            }
        }
    }

    fun restoreFromAutoBackupFile(file: File, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val success = AutoBackupManager.restoreFromBackupFile(file, repository)
            if (success) {
                refreshAutoBackupList()
                showMessage("تمت استعادة البيانات من النسخة الاحتياطية بنجاح!")
            } else {
                showMessage("فشلت الاستعادة من النسخة الاحتياطية")
            }
            onComplete?.invoke(success)
        }
    }

    fun deleteAutoBackupFile(file: File) {
        val deleted = AutoBackupManager.deleteBackupFile(file)
        if (deleted) {
            refreshAutoBackupList()
            showMessage("تم حذف ملف النسخة الاحتياطية")
        }
    }

    fun shareAutoBackupFile(context: Context, file: File) {
        AutoBackupManager.shareBackupFile(context, file)
    }

    // State flows
    val categories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val salesWithItems: StateFlow<List<SaleWithItems>> = repository.allSalesWithItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customersWithDebt: StateFlow<List<CustomerWithDebtSummary>> = repository.customersWithDebt
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: StateFlow<List<Expense>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cashboxConfig: StateFlow<CashboxConfig?> = repository.cashboxConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val debtPayments: StateFlow<List<DebtPayment>> = repository.allDebtPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cart for POS
    private val _cart = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val cart: StateFlow<Map<Long, Int>> = _cart.asStateFlow()

    // Selected Report Period
    private val _selectedReportPeriod = MutableStateFlow(ReportPeriod.TODAY)
    val selectedReportPeriod: StateFlow<ReportPeriod> = _selectedReportPeriod.asStateFlow()

    fun setReportPeriod(period: ReportPeriod) {
        _selectedReportPeriod.value = period
    }

    // Toast / Snack message
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun showMessage(msg: String) {
        _userMessage.value = msg
    }

    // Computed Dashboard Stats
    val dashboardStats: StateFlow<DashboardStats> = combine(
        combine(salesWithItems, expenses) { sales, exps -> Pair(sales, exps) },
        combine(cashboxConfig, debtPayments) { cfg, pmts -> Pair(cfg, pmts) },
        combine(products, customersWithDebt) { prods, custs -> Pair(prods, custs) }
    ) { (sales, exps), (config, payments), (prods, custs) ->
        val startOfDay = BastaRepository.getStartOfDay()
        val endOfDay = BastaRepository.getEndOfDay()

        val todaySales = sales.filter { it.sale.saleDate in startOfDay..endOfDay }
        val todayTotalSales = todaySales.sumOf { it.sale.totalAmount }
        val todayCash = todaySales.filter { it.sale.paymentType == PaymentMethods.CASH }.sumOf { it.sale.totalAmount }
        val todayCredit = todaySales.filter { it.sale.paymentType == PaymentMethods.CREDIT }.sumOf { it.sale.totalAmount }
        val todayPalPay = todaySales.filter { it.sale.paymentType == PaymentMethods.PALPAY }.sumOf { it.sale.totalAmount }
        val todayJawwalPay = todaySales.filter { it.sale.paymentType == PaymentMethods.JAWWAL_PAY }.sumOf { it.sale.totalAmount }
        val todayBop = todaySales.filter { it.sale.paymentType == PaymentMethods.BOP }.sumOf { it.sale.totalAmount }
        val todayElectronic = todayPalPay + todayJawwalPay + todayBop
        val todayProfit = todaySales.sumOf { it.sale.profit }

        val todayExps = exps.filter { it.expenseDate in startOfDay..endOfDay }.sumOf { it.amount }
        val todayCashPaymentsCollected = payments.filter {
            it.paymentDate in startOfDay..endOfDay && it.paymentMethod == PaymentMethods.CASH
        }.sumOf { it.amount }

        val starting = config?.startingCash ?: 0.0
        val netProfit = todayProfit - todayExps
        val cashInTill = starting + todayCash + todayCashPaymentsCollected - todayExps

        val lowStock = prods.count { it.isLowStock }
        val totalDebts = custs.sumOf { maxOf(0.0, it.remainingBalance) }

        DashboardStats(
            todaySalesTotal = todayTotalSales,
            todayCashSales = todayCash,
            todayElectronicSales = todayElectronic,
            todayPalPaySales = todayPalPay,
            todayJawwalPaySales = todayJawwalPay,
            todayBopSales = todayBop,
            todayCreditSales = todayCredit,
            todayExpensesTotal = todayExps,
            todayNetProfit = netProfit,
            cashInBox = maxOf(0.0, cashInTill),
            startingCash = starting,
            lowStockCount = lowStock,
            totalDebtsOutstanding = totalDebts
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    // Computed Period Report Stats
    val periodReportStats: StateFlow<PeriodReportStats> = combine(
        salesWithItems,
        expenses,
        selectedReportPeriod
    ) { sales, exps, period ->
        val now = System.currentTimeMillis()
        val (startTime, endTime) = when (period) {
            ReportPeriod.TODAY -> Pair(BastaRepository.getStartOfDay(now), BastaRepository.getEndOfDay(now))
            ReportPeriod.THIS_WEEK -> Pair(BastaRepository.getStartOfWeek(now), BastaRepository.getEndOfDay(now))
            ReportPeriod.THIS_MONTH -> Pair(BastaRepository.getStartOfMonth(now), BastaRepository.getEndOfDay(now))
            ReportPeriod.ALL_TIME -> Pair(0L, Long.MAX_VALUE)
        }

        val filteredSales = sales.filter { it.sale.saleDate in startTime..endTime }
        val filteredExpenses = exps.filter { it.expenseDate in startTime..endTime }

        val totalRev = filteredSales.sumOf { it.sale.totalAmount }
        val cashRev = filteredSales.filter { it.sale.paymentType == PaymentMethods.CASH }.sumOf { it.sale.totalAmount }
        val electronicRev = filteredSales.filter { PaymentMethods.isElectronic(it.sale.paymentType) }.sumOf { it.sale.totalAmount }
        val creditRev = filteredSales.filter { it.sale.paymentType == PaymentMethods.CREDIT }.sumOf { it.sale.totalAmount }
        val cogs = filteredSales.sumOf { it.sale.totalCost }
        val grossProfit = filteredSales.sumOf { it.sale.profit }
        val totalExp = filteredExpenses.sumOf { it.amount }
        val netProfit = grossProfit - totalExp

        PeriodReportStats(
            totalRevenue = totalRev,
            cashRevenue = cashRev,
            electronicRevenue = electronicRev,
            creditRevenue = creditRev,
            costOfGoodsSold = cogs,
            grossProfit = grossProfit,
            totalExpenses = totalExp,
            netProfit = netProfit,
            salesCount = filteredSales.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PeriodReportStats())

    // === Category Operations ===
    fun addCategory(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.insertCategory(Category(name = name.trim()))
            showMessage("تمت إضافة التصنيف: $name")
        }
    }

    fun updateCategory(category: Category, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            val oldName = category.name
            repository.updateCategory(category.copy(name = newName.trim()))
            repository.dao.reassignCategory(oldName, newName.trim())
            showMessage("تم تعديل التصنيف")
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            showMessage("تم حذف التصنيف وإعادة تعيين منتجاته إلى 'أخرى'")
        }
    }

    // === Product Operations ===
    fun addProduct(
        name: String,
        category: String,
        costPrice: Double,
        sellPrice: Double,
        quantity: Int,
        minStock: Int = 5,
        notes: String = "",
        currentStock: Int = quantity
    ) {
        viewModelScope.launch {
            repository.insertProduct(
                Product(
                    name = name.trim(),
                    category = category.trim().ifEmpty { "أخرى" },
                    costPrice = costPrice,
                    sellPrice = sellPrice,
                    quantity = quantity,
                    currentStock = currentStock,
                    minStockAlert = minStock,
                    notes = notes.trim()
                )
            )
            showMessage("تمت إضافة المنتج بنجاح")
        }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            repository.updateProduct(product.copy(updatedAt = System.currentTimeMillis()))
            showMessage("تم تحديث المنتج")
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            showMessage("تم حذف المنتج")
        }
    }

    fun adjustStock(productId: Long, delta: Int) {
        viewModelScope.launch {
            repository.adjustProductStock(productId, delta)
        }
    }

    fun reduceStock(productId: Long, quantity: Int) {
        viewModelScope.launch {
            repository.reduceStock(productId, quantity)
        }
    }

    suspend fun getProductStock(productId: Long): Int? = repository.getProductStock(productId)

    suspend fun getRemainingStock(productId: Long): Int? = repository.getRemainingStock(productId)

    // === Cart Operations ===
    fun addToCart(productId: Long, qty: Int = 1) {
        val current = _cart.value.toMutableMap()
        val currentQty = current[productId] ?: 0
        current[productId] = currentQty + qty
        _cart.value = current
    }

    fun removeFromCart(productId: Long) {
        val current = _cart.value.toMutableMap()
        val currentQty = current[productId] ?: 0
        if (currentQty > 1) {
            current[productId] = currentQty - 1
        } else {
            current.remove(productId)
        }
        _cart.value = current
    }

    fun clearCart() {
        _cart.value = emptyMap()
    }

    fun checkoutCart(paymentType: String, customerId: Long? = null, customerName: String? = null, notes: String = "") {
        viewModelScope.launch {
            val cartMap = _cart.value
            if (cartMap.isEmpty()) return@launch

            val prods = products.value.associateBy { it.id }
            val items = mutableListOf<SaleItem>()
            var totalAmount = 0.0
            var totalCost = 0.0

            for ((pId, qty) in cartMap) {
                val prod = prods[pId] ?: continue
                val subtotal = qty * prod.sellPrice
                val cost = qty * prod.costPrice
                totalAmount += subtotal
                totalCost += cost

                items.add(
                    SaleItem(
                        saleId = 0,
                        productId = prod.id,
                        productName = prod.name,
                        quantity = qty,
                        unitCost = prod.costPrice,
                        unitPrice = prod.sellPrice,
                        subtotal = subtotal
                    )
                )
            }

            val sale = Sale(
                totalAmount = totalAmount,
                totalCost = totalCost,
                profit = totalAmount - totalCost,
                paymentType = paymentType,
                customerId = customerId,
                customerName = customerName,
                notes = notes,
                isDailyAggregate = false
            )

            repository.recordSale(sale, items)
            clearCart()
            triggerAutoBackup()
            val methodLabel = PaymentMethods.getDisplayName(paymentType)
            showMessage("تم تسجيل البيع بنجاح عبر $methodLabel (المبلغ: ${String.format(java.util.Locale.US, "%.2f", totalAmount)} ₪)")
        }
    }

    // Quick Daily End-of-Day Sales Registration
    fun recordQuickSale(amount: Double, estimatedCost: Double, paymentType: String, customerId: Long? = null, customerName: String? = null, notes: String = "") {
        viewModelScope.launch {
            val cost = if (estimatedCost > 0) estimatedCost else amount * 0.7 // fallback default 30% margin if unspecified
            val sale = Sale(
                totalAmount = amount,
                totalCost = cost,
                profit = amount - cost,
                paymentType = paymentType,
                customerId = customerId,
                customerName = customerName,
                notes = notes,
                isDailyAggregate = true
            )
            repository.recordQuickDailySale(sale)
            triggerAutoBackup()
            val methodLabel = PaymentMethods.getDisplayName(paymentType)
            showMessage("تم تسجيل المبيعات بنجاح عبر $methodLabel")
        }
    }

    fun cancelSale(sale: Sale) {
        viewModelScope.launch {
            repository.cancelSale(sale)
            triggerAutoBackup()
            showMessage("تم إلغاء البيع واسترجاع الكمية للمخزون")
        }
    }

    // === Customer & Debt Operations ===
    fun addCustomer(name: String, phone: String = "", notes: String = "") {
        viewModelScope.launch {
            repository.insertCustomer(
                Customer(name = name.trim(), phone = phone.trim(), notes = notes.trim())
            )
            triggerAutoBackup()
            showMessage("تمت إضافة الزبون بنجاح")
        }
    }

    fun updateCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.updateCustomer(customer)
            triggerAutoBackup()
            showMessage("تم تحديث بيانات الزبون")
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
            triggerAutoBackup()
            showMessage("تم حذف الزبون وسجل ديونه")
        }
    }

    fun recordDebtPayment(
        customerId: Long,
        amount: Double,
        notes: String = "",
        paymentMethod: String = PaymentMethods.CASH
    ) {
        viewModelScope.launch {
            repository.recordDebtPayment(
                DebtPayment(
                    customerId = customerId,
                    amount = amount,
                    notes = notes.trim(),
                    paymentMethod = paymentMethod
                )
            )
            triggerAutoBackup()
            val methodLabel = PaymentMethods.getDisplayName(paymentMethod)
            showMessage("تم تسجيل السداد بنجاح عبر $methodLabel (المبلغ: ${String.format(java.util.Locale.US, "%.2f", amount)} ₪)")
        }
    }

    // === Expense Operations ===
    fun addExpense(title: String, amount: Double, category: String, notes: String = "") {
        viewModelScope.launch {
            repository.insertExpense(
                Expense(
                    title = title.trim(),
                    amount = amount,
                    category = category.trim().ifEmpty { "أخرى" },
                    notes = notes.trim()
                )
            )
            triggerAutoBackup()
            showMessage("تم تسجيل المصروف بنجاح")
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            triggerAutoBackup()
            showMessage("تم حذف المصروف")
        }
    }

    // === Cashbox Operations ===
    fun setStartingCash(amount: Double) {
        viewModelScope.launch {
            repository.setStartingCash(amount)
            showMessage("تم تحديث صندوق بداية اليوم: ${String.format(java.util.Locale.US, "%.2f", amount)} ₪")
        }
    }

    // === Export & Backup Operations ===
    suspend fun getSalesCsv(): String = repository.generateSalesCsv()
    suspend fun getProductsCsv(): String = repository.generateProductsCsv()
    suspend fun getDebtsCsv(): String = repository.generateDebtsCsv()
    suspend fun getExpensesCsv(): String = repository.generateExpensesCsv()
    suspend fun getFullJsonBackup(): String = repository.createFullJsonBackup()

    fun restoreBackup(jsonString: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.restoreFromJsonBackup(jsonString)
            if (success) {
                showMessage("تمت استعادة النسخة الاحتياطية بنجاح!")
            } else {
                showMessage("فشلت استعادة البيانات، يرجى التأكد من صحة الملف")
            }
            onComplete(success)
        }
    }

    // === PDF Generation & Sharing Operations ===
    fun shareSalesReportPdf(context: Context) {
        viewModelScope.launch {
            try {
                val pdf = PdfReportGenerator.generateSalesReportPdf(context, salesWithItems.value, selectedReportPeriod.value.title)
                PdfReportGenerator.sharePdf(context, pdf, "تقرير مبيعات البسطة")
            } catch (e: Exception) {
                showMessage("حدث خطأ أثناء إنشاء تقرير المبيعات: ${e.message}")
            }
        }
    }

    fun shareSingleInvoicePdf(context: Context, saleWithItems: SaleWithItems) {
        viewModelScope.launch {
            try {
                val pdf = PdfReportGenerator.generateSingleInvoicePdf(context, saleWithItems)
                PdfReportGenerator.sharePdf(context, pdf, "فاتورة بيع #${saleWithItems.sale.id}")
            } catch (e: Exception) {
                showMessage("حدث خطأ أثناء إنشاء الفاتورة: ${e.message}")
            }
        }
    }

    fun shareInventoryReportPdf(context: Context) {
        viewModelScope.launch {
            try {
                val pdf = PdfReportGenerator.generateInventoryReportPdf(context, products.value)
                PdfReportGenerator.sharePdf(context, pdf, "كشف جرد مخزون البسطة")
            } catch (e: Exception) {
                showMessage("حدث خطأ أثناء إنشاء كشف المخزون: ${e.message}")
            }
        }
    }

    fun shareCustomerDebtsPdf(context: Context) {
        viewModelScope.launch {
            try {
                val pdf = PdfReportGenerator.generateCustomerDebtsPdf(context, customersWithDebt.value)
                PdfReportGenerator.sharePdf(context, pdf, "كشف ديون الزبائن")
            } catch (e: Exception) {
                showMessage("حدث خطأ أثناء إنشاء كشف الديون: ${e.message}")
            }
        }
    }

    fun shareCustomerStatementPdf(context: Context, customerSummary: CustomerWithDebtSummary) {
        viewModelScope.launch {
            try {
                val custSales = salesWithItems.value.map { it.sale }.filter { it.customerId == customerSummary.customer.id && it.paymentType == "CREDIT" }
                val custPayments = debtPayments.value.filter { it.customerId == customerSummary.customer.id }
                val pdf = PdfReportGenerator.generateCustomerStatementPdf(context, customerSummary, custSales, custPayments)
                PdfReportGenerator.sharePdf(context, pdf, "كشف حساب ${customerSummary.customer.name}")
            } catch (e: Exception) {
                showMessage("حدث خطأ أثناء إنشاء كشف الحساب: ${e.message}")
            }
        }
    }

    fun shareExpensesReportPdf(context: Context) {
        viewModelScope.launch {
            try {
                val pdf = PdfReportGenerator.generateExpensesReportPdf(context, expenses.value, selectedReportPeriod.value.title)
                PdfReportGenerator.sharePdf(context, pdf, "سجل مصاريف البسطة")
            } catch (e: Exception) {
                showMessage("حدث خطأ أثناء إنشاء سجل المصاريف: ${e.message}")
            }
        }
    }

    fun shareFinancialReportPdf(context: Context) {
        viewModelScope.launch {
            try {
                val prods = products.value
                val totalCost = prods.sumOf { it.totalCostValue }
                val totalDebts = customersWithDebt.value.sumOf { maxOf(0.0, it.remainingBalance) }
                val cashInTill = dashboardStats.value.cashInBox
                val pdf = PdfReportGenerator.generateFinancialReportPdf(
                    context,
                    periodReportStats.value,
                    selectedReportPeriod.value.title,
                    totalCost,
                    totalDebts,
                    cashInTill
                )
                PdfReportGenerator.sharePdf(context, pdf, "التقرير المالي وقائمة الأرباح")
            } catch (e: Exception) {
                showMessage("حدث خطأ أثناء إنشاء التقرير المالي: ${e.message}")
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            clearCart()
            showMessage("تم مسح جميع البيانات")
        }
    }
}
