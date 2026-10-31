package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.CustomerWithDebtSummary
import com.example.data.model.PaymentMethods
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.SaleWithItems
import com.example.ui.BastaViewModel
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.CurrencyText
import com.example.ui.components.EmptyStateView
import com.example.ui.components.PdfIcon
import com.example.ui.theme.DebtPurple
import com.example.ui.theme.DebtPurpleContainer
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.PrimaryGreenLight
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenContainer
import com.example.ui.theme.StockAlertRed
import com.example.ui.theme.StockAlertRedContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(
    viewModel: BastaViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: نقطة البيع (POS), 1: سجل المبيعات
    val products by viewModel.products.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val customersWithDebt by viewModel.customersWithDebt.collectAsState()
    val salesWithItems by viewModel.salesWithItems.collectAsState()

    var showQuickDailySaleDialog by remember { mutableStateOf(false) }
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var saleToCancel by remember { mutableStateOf<Sale?>(null) }
    var selectedSaleDetail by remember { mutableStateOf<SaleWithItems?>(null) }

    val context = LocalContext.current

    Column(modifier = modifier.fillMaxSize()) {
        // Tab row: POS vs Sales History
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("نقطة البيع (كاشير)", fontWeight = FontWeight.Bold)
                    }
                },
                modifier = Modifier.testTag("tab_pos")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("سجل الفواتير (${salesWithItems.size})", fontWeight = FontWeight.Bold)
                    }
                },
                modifier = Modifier.testTag("tab_sales_history")
            )
        }

        if (selectedTab == 0) {
            PosTabContent(
                products = products,
                cart = cart,
                onAddToCart = { pId -> viewModel.addToCart(pId) },
                onRemoveFromCart = { pId -> viewModel.removeFromCart(pId) },
                onClearCart = { viewModel.clearCart() },
                onCheckoutClick = { showCheckoutDialog = true },
                onOpenDailyAggregateSale = { showQuickDailySaleDialog = true }
            )
        } else {
            SalesHistoryTabContent(
                salesWithItems = salesWithItems,
                onSaleClick = { selectedSaleDetail = it },
                onCancelSale = { saleToCancel = it },
                onShareInvoicePdf = { viewModel.shareSingleInvoicePdf(context, it) },
                onShareAllSalesPdf = { viewModel.shareSalesReportPdf(context) }
            )
        }
    }

    // Checkout Dialog (Cash or Credit)
    if (showCheckoutDialog) {
        CheckoutDialog(
            cart = cart,
            products = products,
            customers = customersWithDebt,
            onDismiss = { showCheckoutDialog = false },
            onConfirmCheckout = { paymentType, customerId, customerName, notes ->
                viewModel.checkoutCart(paymentType, customerId, customerName, notes)
                showCheckoutDialog = false
            }
        )
    }

    // Quick Daily Aggregate Sale Dialog ("تسجيل إجمالي مبيعات نهاية اليوم")
    if (showQuickDailySaleDialog) {
        QuickDailyAggregateDialog(
            customers = customersWithDebt,
            onDismiss = { showQuickDailySaleDialog = false },
            onConfirm = { amount, cost, paymentType, custId, custName, notes ->
                viewModel.recordQuickSale(amount, cost, paymentType, custId, custName, notes)
                showQuickDailySaleDialog = false
            }
        )
    }

    // Cancel Sale Dialog
    saleToCancel?.let { sale ->
        ConfirmDeleteDialog(
            title = "إلغاء واسترجاع عملية البيع",
            message = "هل تريد إلغاء فاتورة البيع بقيمة ${String.format(Locale.US, "%.2f", sale.totalAmount)} ₪؟ إذا كانت منتجات سيتم استرجاع كمياتها إلى المخزون تلقائياً.",
            onConfirm = {
                viewModel.cancelSale(sale)
                saleToCancel = null
            },
            onDismiss = { saleToCancel = null }
        )
    }

    // Sale Details Modal
    selectedSaleDetail?.let { detail ->
        SaleDetailDialog(
            saleWithItems = detail,
            onDismiss = { selectedSaleDetail = null },
            onCancelSale = {
                saleToCancel = detail.sale
                selectedSaleDetail = null
            },
            onSharePdf = {
                viewModel.shareSingleInvoicePdf(context, detail)
            }
        )
    }
}

@Composable
fun PosTabContent(
    products: List<Product>,
    cart: Map<Long, Int>,
    onAddToCart: (Long) -> Unit,
    onRemoveFromCart: (Long) -> Unit,
    onClearCart: () -> Unit,
    onCheckoutClick: () -> Unit,
    onOpenDailyAggregateSale: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val productMap = remember(products) { products.associateBy { it.id } }

    val totalCartAmount = remember(cart, products) {
        cart.entries.sumOf { (pId, qty) ->
            val p = productMap[pId]
            (p?.sellPrice ?: 0.0) * qty
        }
    }

    val totalCartItemsCount = remember(cart) { cart.values.sum() }

    val filteredProducts = products.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        // Top Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث عن صنف...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.weight(1f).testTag("pos_search_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Quick End of Day Aggregate button
            FilledTonalButton(
                onClick = onOpenDailyAggregateSale,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("eod_sales_button")
            ) {
                Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("إجمالي نهاية اليوم", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Products Grid
        Box(modifier = Modifier.weight(1f)) {
            if (filteredProducts.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.PointOfSale,
                    title = "لا توجد منتجات مسجلة",
                    description = "أضف منتجات من قسم المخزون أو سجل مبيعات إجمالية بنهاية اليوم."
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        val inCartQty = cart[product.id] ?: 0
                        ProductPosRow(
                            product = product,
                            inCartQty = inCartQty,
                            onAdd = { onAddToCart(product.id) },
                            onRemove = { onRemoveFromCart(product.id) }
                        )
                    }
                }
            }
        }

        // Active Cart Bottom Bar
        AnimatedVisibility(visible = cart.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .testTag("active_cart_bar"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "السلة ($totalCartItemsCount قطع)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = onClearCart) {
                                Text("تفريغ", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            }
                        }
                        CurrencyText(
                            amount = totalCartAmount,
                            fontSize = 20,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Button(
                        onClick = onCheckoutClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        modifier = Modifier.testTag("checkout_button")
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "إتمام البيع", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ProductPosRow(
    product: Product,
    inCartQty: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAdd() }
            .testTag("pos_product_row_${product.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (inCartQty > 0) PrimaryGreenLight.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = product.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "المتبقي: ${product.currentStock}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (product.isLowStock) StockAlertRed else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (product.isLowStock) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                CurrencyText(amount = product.sellPrice, fontSize = 16, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                Spacer(modifier = Modifier.width(12.dp))

                if (inCartQty > 0) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = "$inCartQty",
                        modifier = Modifier.padding(horizontal = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onAdd,
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(PrimaryGreen)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun CheckoutDialog(
    cart: Map<Long, Int>,
    products: List<Product>,
    customers: List<CustomerWithDebtSummary>,
    onDismiss: () -> Unit,
    onConfirmCheckout: (paymentType: String, customerId: Long?, customerName: String?, notes: String) -> Unit
) {
    var paymentType by remember { mutableStateOf("CASH") } // "CASH" or "CREDIT"
    var selectedCustomerId by remember { mutableStateOf<Long?>(null) }
    var notes by remember { mutableStateOf("") }
    var isCustomerError by remember { mutableStateOf(false) }

    val productMap = remember(products) { products.associateBy { it.id } }
    val totalAmount = remember(cart, products) {
        cart.entries.sumOf { (pId, qty) -> (productMap[pId]?.sellPrice ?: 0.0) * qty }
    }
    val totalCost = remember(cart, products) {
        cart.entries.sumOf { (pId, qty) -> (productMap[pId]?.costPrice ?: 0.0) * qty }
    }
    val profit = totalAmount - totalCost

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "إتمام عملية البيع", fontWeight = FontWeight.Bold)
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    // Invoice Total & Profit Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "المبلغ الإجمالي المطلوبة", style = MaterialTheme.typography.bodySmall)
                                CurrencyText(amount = totalAmount, fontSize = 20, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "الربح الصافي للفاتورة", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = "+${String.format(Locale.US, "%.2f", profit)} ₪",
                                    color = ProfitGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }

                item {
                    Text(text = "طريقة الدفع والتحصيل:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilledTonalButton(
                                onClick = { paymentType = PaymentMethods.CASH },
                                modifier = Modifier.weight(1f).testTag("pay_cash_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (paymentType == PaymentMethods.CASH) PaymentMethods.ColorCash else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (paymentType == PaymentMethods.CASH) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("كاش", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
                                onClick = { paymentType = PaymentMethods.CREDIT },
                                modifier = Modifier.weight(1f).testTag("pay_credit_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (paymentType == PaymentMethods.CREDIT) PaymentMethods.ColorCredit else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (paymentType == PaymentMethods.CREDIT) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("دين (آجل)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(text = "تطبيقات الدفع والمحافظ الإلكترونية والبنك:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // PalPay
                            FilledTonalButton(
                                onClick = { paymentType = PaymentMethods.PALPAY },
                                modifier = Modifier.weight(1f).testTag("pay_palpay_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (paymentType == PaymentMethods.PALPAY) PaymentMethods.ColorPalPay else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (paymentType == PaymentMethods.PALPAY) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text("بال بي", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Jawwal Pay
                            FilledTonalButton(
                                onClick = { paymentType = PaymentMethods.JAWWAL_PAY },
                                modifier = Modifier.weight(1f).testTag("pay_jawwal_pay_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (paymentType == PaymentMethods.JAWWAL_PAY) PaymentMethods.ColorJawwalPay else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (paymentType == PaymentMethods.JAWWAL_PAY) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text("جوال بي", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // BOP
                            FilledTonalButton(
                                onClick = { paymentType = PaymentMethods.BOP },
                                modifier = Modifier.weight(1f).testTag("pay_bop_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (paymentType == PaymentMethods.BOP) PaymentMethods.ColorBop else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (paymentType == PaymentMethods.BOP) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text("بنك فلسطين", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // If CREDIT, select customer
                if (paymentType == "CREDIT") {
                    item {
                        Text(text = "اختر الزبون لتسجيل الدين عليه *:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        if (customers.isEmpty()) {
                            Text(
                                text = "لا يوجد زبائن مسجلين حالياً. يرجى إضافة زبون أولاً من قسم الديون.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(customers) { cust ->
                                    val isSelected = cust.customer.id == selectedCustomerId
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable {
                                                selectedCustomerId = cust.customer.id
                                                isCustomerError = false
                                            },
                                        color = if (isSelected) DebtPurpleContainer else Color.Transparent
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = cust.customer.name,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) DebtPurple else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "الدين الحالي: ${String.format(Locale.US, "%.1f", cust.remainingBalance)} ₪",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (isCustomerError) {
                            Text(text = "يجب اختيار زبون لتسجيل البيع بالدين!", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("ملاحظات (اختياري)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (paymentType == "CREDIT" && selectedCustomerId == null) {
                        isCustomerError = true
                        return@Button
                    }
                    val custName = customers.find { it.customer.id == selectedCustomerId }?.customer?.name
                    onConfirmCheckout(paymentType, selectedCustomerId, custName, notes)
                },
                modifier = Modifier.testTag("confirm_checkout_button")
            ) {
                Text("تأكيد وحفظ الفاتورة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun QuickDailyAggregateDialog(
    customers: List<CustomerWithDebtSummary>,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, estimatedCost: Double, paymentType: String, customerId: Long?, customerName: String?, notes: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var costText by remember { mutableStateOf("") }
    var paymentType by remember { mutableStateOf("CASH") }
    var selectedCustomerId by remember { mutableStateOf<Long?>(null) }
    var notes by remember { mutableStateOf("إجمالي مبيعات نهاية اليوم") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "تسجيل إجمالي مبيعات نهاية اليوم", fontWeight = FontWeight.Bold)
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Text(
                        text = "إذا كنت تفضل تسجيل إجمالي مبيعات البسطة دفعة واحدة في نهاية اليوم دون اختيار الأصناف الفردية:",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                item {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = {
                            amountText = it
                            isError = false
                            // Suggest cost ~ 70% if empty
                            val amt = it.toDoubleOrNull()
                            if (amt != null && costText.isEmpty()) {
                                costText = String.format(Locale.US, "%.1f", amt * 0.7)
                            }
                        },
                        label = { Text("إجمالي المبيعات (₪) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("eod_amount_input")
                    )
                }

                item {
                    OutlinedTextField(
                        value = costText,
                        onValueChange = { costText = it },
                        label = { Text("تكلفة البضاعة المباعة التقديرية (₪)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("eod_cost_input")
                    )
                }

                item {
                    Text(text = "طريقة الدفع والتحصيل:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilledTonalButton(
                                onClick = { paymentType = PaymentMethods.CASH },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (paymentType == PaymentMethods.CASH) PaymentMethods.ColorCash else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (paymentType == PaymentMethods.CASH) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text("كاش", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            FilledTonalButton(
                                onClick = { paymentType = PaymentMethods.CREDIT },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (paymentType == PaymentMethods.CREDIT) PaymentMethods.ColorCredit else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (paymentType == PaymentMethods.CREDIT) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text("دين (آجل)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(text = "تطبيقات الدفع الإلكتروني والبنك:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilledTonalButton(
                                onClick = { paymentType = PaymentMethods.PALPAY },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (paymentType == PaymentMethods.PALPAY) PaymentMethods.ColorPalPay else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (paymentType == PaymentMethods.PALPAY) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text("بال بي", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            FilledTonalButton(
                                onClick = { paymentType = PaymentMethods.JAWWAL_PAY },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (paymentType == PaymentMethods.JAWWAL_PAY) PaymentMethods.ColorJawwalPay else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (paymentType == PaymentMethods.JAWWAL_PAY) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text("جوال بي", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            FilledTonalButton(
                                onClick = { paymentType = PaymentMethods.BOP },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (paymentType == PaymentMethods.BOP) PaymentMethods.ColorBop else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (paymentType == PaymentMethods.BOP) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text("بنك فلسطين", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (paymentType == "CREDIT" && customers.isNotEmpty()) {
                    item {
                        Text(text = "اختر الزبون:", style = MaterialTheme.typography.bodySmall)
                        customers.forEach { c ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedCustomerId = c.customer.id },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = selectedCustomerId == c.customer.id, onClick = { selectedCustomerId = c.customer.id })
                                Text(text = c.customer.name)
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("ملاحظات") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                if (isError) {
                    item {
                        Text(text = "يرجى إدخال مبلغ مبيعات صحيح أكبر من 0", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    val cost = costText.toDoubleOrNull() ?: ((amount ?: 0.0) * 0.7)
                    if (amount != null && amount > 0) {
                        val custName = customers.find { it.customer.id == selectedCustomerId }?.customer?.name
                        onConfirm(amount, cost, paymentType, selectedCustomerId, custName, notes)
                    } else {
                        isError = true
                    }
                },
                modifier = Modifier.testTag("confirm_eod_button")
            ) {
                Text("تسجيل وحفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun SalesHistoryTabContent(
    salesWithItems: List<SaleWithItems>,
    onSaleClick: (SaleWithItems) -> Unit,
    onCancelSale: (Sale) -> Unit,
    onShareInvoicePdf: (SaleWithItems) -> Unit,
    onShareAllSalesPdf: () -> Unit
) {
    var paymentFilter by remember { mutableStateOf("ALL") }

    val filteredSales = remember(salesWithItems, paymentFilter) {
        if (paymentFilter == "ALL") salesWithItems
        else salesWithItems.filter { it.sale.paymentType == paymentFilter }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        // Top Action Row: Share Sales History PDF
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "الفواتير السابقة (${filteredSales.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (salesWithItems.isNotEmpty()) {
                FilledTonalButton(
                    onClick = onShareAllSalesPdf,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("share_sales_pdf_button")
                ) {
                    PdfIcon(modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "مشاركة كشف PDF", fontSize = 12.sp)
                }
            }
        }

        // Filter chips for payment methods
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val filters = listOf(
                "ALL" to "الكل",
                PaymentMethods.CASH to "كاش",
                PaymentMethods.PALPAY to "بال بي",
                PaymentMethods.JAWWAL_PAY to "جوال بي",
                PaymentMethods.BOP to "بنك فلسطين",
                PaymentMethods.CREDIT to "دين"
            )

            filters.forEach { (key, label) ->
                val isSelected = paymentFilter == key
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { paymentFilter = key }
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        if (filteredSales.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.Receipt,
                title = if (salesWithItems.isEmpty()) "لا توجد مبيعات مسجلة" else "لا توجد نتائج تطابق الفلتر",
                description = if (salesWithItems.isEmpty()) "سجل أول عملية بيع من تبويب 'نقطة البيع'." else "جرّب اختيار 'الكل' لعرض كافة الفواتير."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredSales, key = { it.sale.id }) { item ->
                    SaleHistoryRow(
                        saleWithItems = item,
                        onClick = { onSaleClick(item) },
                        onCancel = { onCancelSale(item.sale) },
                        onSharePdf = { onShareInvoicePdf(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun SaleHistoryRow(
    saleWithItems: SaleWithItems,
    onClick: () -> Unit,
    onCancel: () -> Unit,
    onSharePdf: () -> Unit
) {
    val s = saleWithItems.sale
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale("ar")) }
    val dateStr = remember(s.saleDate) { dateFormat.format(Date(s.saleDate)) }
    val methodColor = PaymentMethods.getColor(s.paymentType)
    val methodContainerColor = PaymentMethods.getContainerColor(s.paymentType)
    val methodLabel = when (s.paymentType) {
        PaymentMethods.CASH -> "كاش"
        PaymentMethods.PALPAY -> "بال بي"
        PaymentMethods.JAWWAL_PAY -> "جوال بي"
        PaymentMethods.BOP -> "بنك فلسطين"
        PaymentMethods.CREDIT -> "دين (${s.customerName ?: "عميل"})"
        else -> s.paymentType
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("sale_history_card_${s.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(methodContainerColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (s.paymentType) {
                                PaymentMethods.CREDIT -> Icons.Default.CreditCard
                                PaymentMethods.CASH -> Icons.Default.AttachMoney
                                else -> Icons.Default.PointOfSale
                            },
                            contentDescription = null,
                            tint = methodColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (s.isDailyAggregate) "مبيعات إجمالية #${s.id}" else "فاتورة بيع #${s.id}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = dateStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    CurrencyText(amount = s.totalAmount, fontSize = 16, fontWeight = FontWeight.Bold)
                    Text(
                        text = "ربح: +${String.format(Locale.US, "%.1f", s.profit)} ₪",
                        color = ProfitGreen,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (saleWithItems.items.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = saleWithItems.items.joinToString("، ") { "${it.productName} (x${it.quantity})" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            if (s.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ملاحظات: ${s.notes}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = methodContainerColor
                ) {
                    Text(
                        text = "الدفع: $methodLabel",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = methodColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalButton(
                        onClick = onSharePdf,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("share_invoice_pdf_${s.id}")
                    ) {
                        PdfIcon(modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "فاتورة PDF", fontSize = 11.sp)
                    }

                    TextButton(onClick = onCancel) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "إلغاء", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SaleDetailDialog(
    saleWithItems: SaleWithItems,
    onDismiss: () -> Unit,
    onCancelSale: () -> Unit,
    onSharePdf: () -> Unit
) {
    val s = saleWithItems.sale
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale("ar")) }
    val dateStr = remember(s.saleDate) { dateFormat.format(Date(s.saleDate)) }
    val methodColor = PaymentMethods.getColor(s.paymentType)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "تفاصيل فاتورة البيع #${s.id}", fontWeight = FontWeight.Bold)
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Text(text = "التاريخ والوقت: $dateStr", style = MaterialTheme.typography.bodySmall)
                    val payDesc = if (s.paymentType == PaymentMethods.CREDIT)
                        "طريقة الدفع: دين على ${s.customerName ?: "عميل"}"
                    else
                        "طريقة الدفع: ${PaymentMethods.getDisplayName(s.paymentType)}"
                    Text(
                        text = payDesc,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = methodColor
                    )
                    if (s.notes.isNotBlank()) {
                        Text(text = "ملاحظات: ${s.notes}", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Divider()
                }

                if (saleWithItems.items.isNotEmpty()) {
                    item {
                        Text(text = "الأصناف المباعة:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    }
                    items(saleWithItems.items) { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "${item.productName} (x${item.quantity})", fontWeight = FontWeight.Medium)
                                Text(
                                    text = "سعر الحبة: ${String.format(Locale.US, "%.1f", item.unitPrice)} ₪",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            CurrencyText(amount = item.subtotal, fontSize = 14, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                item {
                    Divider()
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "إجمالي الفاتورة:", fontWeight = FontWeight.Bold)
                        CurrencyText(amount = s.totalAmount, fontSize = 16, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "تكلفة البضاعة:", style = MaterialTheme.typography.bodySmall)
                        CurrencyText(amount = s.totalCost, fontSize = 14)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "صافي الربح:", fontWeight = FontWeight.Bold, color = ProfitGreen)
                        CurrencyText(amount = s.profit, fontSize = 16, fontWeight = FontWeight.Bold, color = ProfitGreen)
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilledTonalButton(onClick = onSharePdf) {
                    PdfIcon(modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("مشاركة الفاتورة PDF")
                }
                Button(onClick = onDismiss) {
                    Text("إغلاق")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelSale) {
                Text("إلغاء واسترجاع", color = MaterialTheme.colorScheme.error)
            }
        }
    )
}
