package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SaleWithItems
import com.example.ui.BastaViewModel
import com.example.ui.components.CurrencyText
import com.example.ui.components.MetricCard
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.DebtPurple
import com.example.ui.theme.DebtPurpleContainer
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.PrimaryGreenLight
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenContainer
import com.example.ui.theme.StockAlertOrange
import com.example.ui.theme.StockAlertRed
import com.example.ui.theme.StockAlertRedContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: BastaViewModel,
    onNavigateToSales: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToDebts: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.dashboardStats.collectAsState()
    val salesWithItems by viewModel.salesWithItems.collectAsState()
    val products by viewModel.products.collectAsState()
    val lowStockCount = stats.lowStockCount

    var showStartingCashDialog by remember { mutableStateOf(false) }
    var showQuickExpenseDialog by remember { mutableStateOf(false) }

    val todayDateFormat = remember { SimpleDateFormat("EEEE، d MMMM yyyy", Locale("ar")) }
    val todayDateStr = remember { todayDateFormat.format(Date()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "نظرة عامة على البسطة",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = todayDateStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Quick Cashbox button
                FilledTonalButton(
                    onClick = { showStartingCashDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("starting_cash_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "صندوق البداية", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // Low stock warning banner if applicable
        if (lowStockCount > 0) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToInventory() }
                        .testTag("low_stock_banner"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = StockAlertRedContainer,
                        contentColor = StockAlertRed
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "تنبيه المخزون: $lowStockCount منتجات أوشكت على النفاد!",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "اضغط هنا لفحص المخزون وإعادة التعبئة",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }

        // Key Metrics: Cash in till & Net Profit Hero Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cash in till (النقد المتوفر في الصندوق)
                MetricCard(
                    title = "النقد في الصندوق",
                    value = String.format(Locale.US, "%.2f ₪", stats.cashInBox),
                    subtext = "صندوق البداية: ${String.format(Locale.US, "%.0f ₪", stats.startingCash)}",
                    icon = Icons.Default.AccountBalanceWallet,
                    containerColor = PrimaryGreenLight,
                    contentColor = PrimaryGreen,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("metric_cash_in_box"),
                    onClick = { showStartingCashDialog = true }
                )

                // Net Profit (صافي الربح)
                val isProfitPositive = stats.todayNetProfit >= 0
                MetricCard(
                    title = "صافي الربح اليوم",
                    value = String.format(Locale.US, "%.2f ₪", stats.todayNetProfit),
                    subtext = if (isProfitPositive) "أرباح اليوم المحققة" else "عجز اليوم",
                    icon = if (isProfitPositive) Icons.Default.TrendingUp else Icons.Default.ArrowDownward,
                    containerColor = if (isProfitPositive) ProfitGreenContainer else StockAlertRedContainer,
                    contentColor = if (isProfitPositive) ProfitGreen else StockAlertRed,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("metric_net_profit")
                )
            }
        }

        // Secondary Metrics Grid: Sales Breakdown & Expenses
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "إجمالي المبيعات",
                    value = String.format(Locale.US, "%.2f ₪", stats.todaySalesTotal),
                    icon = Icons.Default.PointOfSale,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToSales
                )

                MetricCard(
                    title = "المصاريف اليومية",
                    value = String.format(Locale.US, "%.2f ₪", stats.todayExpensesTotal),
                    icon = Icons.Default.MoneyOff,
                    containerColor = AmberContainer,
                    contentColor = AmberPrimary,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToExpenses
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "المبيعات النقدية",
                    value = String.format(Locale.US, "%.2f ₪", stats.todayCashSales),
                    icon = Icons.Default.Money,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "المبيعات بالدين",
                    value = String.format(Locale.US, "%.2f ₪", stats.todayCreditSales),
                    icon = Icons.Default.CreditCard,
                    containerColor = DebtPurpleContainer,
                    contentColor = DebtPurple,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToDebts
                )
            }
        }

        // Electronic Sales Card (PalPay, Jawwal Pay, Bank of Palestine)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("metric_electronic_sales_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE0F2FE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payment,
                                    contentDescription = null,
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "مبيعات التطبيقات البنكية (إلكتروني)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "بال بي • جوال بي • بنك فلسطين",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = String.format(Locale.US, "%.2f ₪", stats.todayElectronicSales),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0284C7)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // PalPay
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE0F2FE))
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "بال بي", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                            Text(
                                text = String.format(Locale.US, "%.2f ₪", stats.todayPalPaySales),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0284C7)
                            )
                        }

                        // Jawwal Pay
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFEDD5))
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "جوال بي", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEA580C))
                            Text(
                                text = String.format(Locale.US, "%.2f ₪", stats.todayJawwalPaySales),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFEA580C)
                            )
                        }

                        // Bank of Palestine
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFEE2E2))
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "بنك فلسطين", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                            Text(
                                text = String.format(Locale.US, "%.2f ₪", stats.todayBopSales),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                }
            }
        }

        // Quick Actions Row
        item {
            Text(
                text = "إجراءات سريعة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onNavigateToSales,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_new_sale"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Icon(imageVector = Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "تسجيل بيع", fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = { showQuickExpenseDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_new_expense"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.MoneyOff, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "تسجيل مصروف", fontSize = 13.sp)
                }
            }
        }

        // Recent Sales List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "آخر المبيعات المسجلة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = onNavigateToSales) {
                    Text(text = "عرض الكل")
                }
            }
        }

        val recentSales = salesWithItems.take(5)
        if (recentSales.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد مبيعات مسجلة حتى الآن.\nاضغط على 'تسجيل بيع' للبدء.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(recentSales) { saleWithItems ->
                RecentSaleItemRow(saleWithItems = saleWithItems)
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Starting Cash Dialog
    if (showStartingCashDialog) {
        StartingCashDialog(
            currentAmount = stats.startingCash,
            onDismiss = { showStartingCashDialog = false },
            onConfirm = { newAmount ->
                viewModel.setStartingCash(newAmount)
                showStartingCashDialog = false
            }
        )
    }

    // Quick Expense Dialog
    if (showQuickExpenseDialog) {
        QuickExpenseDialog(
            onDismiss = { showQuickExpenseDialog = false },
            onConfirm = { title, amount, category, notes ->
                viewModel.addExpense(title, amount, category, notes)
                showQuickExpenseDialog = false
            }
        )
    }
}

@Composable
fun RecentSaleItemRow(saleWithItems: SaleWithItems) {
    val s = saleWithItems.sale
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale("ar")) }
    val timeStr = remember(s.saleDate) { timeFormat.format(Date(s.saleDate)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (s.paymentType == "CASH") PrimaryGreenLight else DebtPurpleContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (s.paymentType == "CASH") Icons.Default.AttachMoney else Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = if (s.paymentType == "CASH") PrimaryGreen else DebtPurple,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (s.isDailyAggregate) "مبيعات إجمالية (نهاية اليوم)"
                        else if (saleWithItems.items.isNotEmpty()) {
                            saleWithItems.items.joinToString("، ") { "${it.productName} (x${it.quantity})" }
                        } else "فاتورة بيع #${s.id}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (s.paymentType == "CASH") "نقدي • $timeStr" else "دين (${s.customerName ?: "عميل"}) • $timeStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                CurrencyText(amount = s.totalAmount, fontSize = 15, fontWeight = FontWeight.Bold)
                Text(
                    text = "ربح: +${String.format(Locale.US, "%.1f", s.profit)} ₪",
                    style = MaterialTheme.typography.bodySmall,
                    color = ProfitGreen,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun StartingCashDialog(
    currentAmount: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amountText by remember { mutableStateOf(if (currentAmount > 0) currentAmount.toString() else "") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "صندوق بداية اليوم (الفكة)", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    text = "أدخل المبلغ النقدي الموجود في الصندوق عند بداية اليوم لضبط حساب النقدية اليومية بدقة:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        isError = false
                    },
                    label = { Text("المبلغ بالشيكل (₪)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = isError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("starting_cash_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = amountText.toDoubleOrNull()
                    if (parsed != null && parsed >= 0) {
                        onConfirm(parsed)
                    } else {
                        isError = true
                    }
                },
                modifier = Modifier.testTag("confirm_starting_cash_button")
            ) {
                Text("حفظ")
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
fun QuickExpenseDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, amount: Double, category: String, notes: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("بضاعة ومشتريات") }
    var notes by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    val categories = listOf("بضاعة ومشتريات", "إيجار وكهرباء", "نقل ومواصلات", "أكياس وتغليف", "شخصي ويومية", "أخرى")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "تسجيل مصروف جديد", fontWeight = FontWeight.Bold)
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("بيان المصروف (مثال: شراء مياه، كرتونة شيبس)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("expense_title_input")
                    )
                }
                item {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = {
                            amountText = it
                            isError = false
                        },
                        label = { Text("المبلغ بالشيكل (₪)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = isError,
                        modifier = Modifier.fillMaxWidth().testTag("expense_amount_input")
                    )
                }
                item {
                    Text(text = "التصنيف:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Display top 3 categories as quick selectable chips
                        categories.take(3).forEach { cat ->
                            val isSel = cat == selectedCategory
                            FilledTonalButton(
                                onClick = { selectedCategory = cat },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = cat, fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.drop(3).forEach { cat ->
                            val isSel = cat == selectedCategory
                            FilledTonalButton(
                                onClick = { selectedCategory = cat },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = cat, fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("ملاحظات (اختياري)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (title.isNotBlank() && amount != null && amount > 0) {
                        onConfirm(title, amount, selectedCategory, notes)
                    } else {
                        isError = true
                    }
                },
                modifier = Modifier.testTag("confirm_expense_button")
            ) {
                Text("حفظ المصروف")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
