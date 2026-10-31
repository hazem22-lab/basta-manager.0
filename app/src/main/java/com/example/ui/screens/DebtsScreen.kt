package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Customer
import com.example.data.model.CustomerWithDebtSummary
import com.example.data.model.DebtPayment
import com.example.data.model.PaymentMethods
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DebtsScreen(
    viewModel: BastaViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customersWithDebt by viewModel.customersWithDebt.collectAsState()
    val allPayments by viewModel.debtPayments.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showOnlyWithDebts by remember { mutableStateOf(false) }

    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var customerToEdit by remember { mutableStateOf<Customer?>(null) }
    var customerToDelete by remember { mutableStateOf<Customer?>(null) }
    var customerForPayment by remember { mutableStateOf<CustomerWithDebtSummary?>(null) }
    var customerForLedger by remember { mutableStateOf<CustomerWithDebtSummary?>(null) }

    val totalOutstandingDebts = remember(customersWithDebt) {
        customersWithDebt.sumOf { maxOf(0.0, it.remainingBalance) }
    }

    val filteredCustomers = customersWithDebt.filter { item ->
        val matchesSearch = item.customer.name.contains(searchQuery, ignoreCase = true) ||
                item.customer.phone.contains(searchQuery) ||
                item.customer.notes.contains(searchQuery, ignoreCase = true)
        val matchesDebt = !showOnlyWithDebts || item.remainingBalance > 0.01
        matchesSearch && matchesDebt
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "سجل الديون والعملاء",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "متابعة حسابات الزبائن والمبالغ المستحقة وسجل السداد",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    FilledTonalButton(
                        onClick = { viewModel.shareCustomerDebtsPdf(context) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("share_debts_pdf_button")
                    ) {
                        PdfIcon(modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "كشف الديون PDF", fontSize = 12.sp)
                    }
                }
            }

            // Total Outstanding Debt Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("debts_summary_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DebtPurpleContainer)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "إجمالي الديون المستحقة بالخارج",
                                style = MaterialTheme.typography.titleSmall,
                                color = DebtPurple,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            CurrencyText(
                                amount = totalOutstandingDebts,
                                fontSize = 24,
                                fontWeight = FontWeight.Bold,
                                color = DebtPurple
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(DebtPurple.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = null,
                                tint = DebtPurple,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // Search & Filter
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("بحث عن اسم الزبون...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = null)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("search_customer_input")
                    )

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showOnlyWithDebts = !showOnlyWithDebts },
                        color = if (showOnlyWithDebts) DebtPurple else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (showOnlyWithDebts) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Text(
                            text = "المدينون فقط",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Customers List
            if (filteredCustomers.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.Person,
                        title = "لا يوجد زبائن مسجلين",
                        description = "أضف زبون جديد لتسجيل مشترياته بالدين ومتابعة السداد."
                    )
                }
            } else {
                items(filteredCustomers, key = { it.customer.id }) { item ->
                    CustomerDebtCard(
                        summary = item,
                        onRecordPayment = { customerForPayment = item },
                        onViewLedger = { customerForLedger = item },
                        onShareStatementPdf = { viewModel.shareCustomerStatementPdf(context, item) },
                        onEdit = { customerToEdit = item.customer },
                        onDelete = { customerToDelete = item.customer }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // FAB to add customer
        FloatingActionButton(
            onClick = { showAddCustomerDialog = true },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .testTag("add_customer_fab"),
            containerColor = DebtPurple,
            contentColor = Color.White
        ) {
            Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "إضافة زبون")
        }
    }

    // Add Customer Dialog
    if (showAddCustomerDialog) {
        CustomerFormDialog(
            title = "إضافة زبون جديد",
            customer = null,
            onDismiss = { showAddCustomerDialog = false },
            onConfirm = { name, phone, notes ->
                viewModel.addCustomer(name, phone, notes)
                showAddCustomerDialog = false
            }
        )
    }

    // Edit Customer Dialog
    customerToEdit?.let { cust ->
        CustomerFormDialog(
            title = "تعديل بيانات الزبون",
            customer = cust,
            onDismiss = { customerToEdit = null },
            onConfirm = { name, phone, notes ->
                viewModel.updateCustomer(cust.copy(name = name, phone = phone, notes = notes))
                customerToEdit = null
            }
        )
    }

    // Delete Customer Dialog
    customerToDelete?.let { cust ->
        ConfirmDeleteDialog(
            title = "حذف الزبون",
            message = "هل أنت متأكد من حذف الزبون '${cust.name}'؟ سيتم حذف جميع سجلات ديونه ودفعاته.",
            onConfirm = {
                viewModel.deleteCustomer(cust)
                customerToDelete = null
            },
            onDismiss = { customerToDelete = null }
        )
    }

    // Record Payment Dialog
    customerForPayment?.let { custSummary ->
        RecordPaymentDialog(
            customerSummary = custSummary,
            onDismiss = { customerForPayment = null },
            onConfirm = { amount, notes, paymentMethod ->
                viewModel.recordDebtPayment(custSummary.customer.id, amount, notes, paymentMethod)
                customerForPayment = null
            }
        )
    }

    // Customer Debt Ledger Dialog
    customerForLedger?.let { custSummary ->
        val customerPayments = allPayments.filter { it.customerId == custSummary.customer.id }
        CustomerLedgerDialog(
            customerSummary = custSummary,
            payments = customerPayments,
            onDismiss = { customerForLedger = null },
            onRecordPayment = {
                customerForPayment = custSummary
                customerForLedger = null
            },
            onSharePdf = { viewModel.shareCustomerStatementPdf(context, custSummary) }
        )
    }
}

@Composable
fun CustomerDebtCard(
    summary: CustomerWithDebtSummary,
    onRecordPayment: () -> Unit,
    onViewLedger: () -> Unit,
    onShareStatementPdf: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val cust = summary.customer
    val context = LocalContext.current
    val progress = if (summary.totalPurchasesOnCredit > 0) {
        (summary.totalPaid / summary.totalPurchasesOnCredit).toFloat().coerceIn(0f, 1f)
    } else 1f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("customer_card_${cust.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Name, Phone, Action icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cust.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (cust.phone.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${cust.phone}"))
                                    context.startActivity(intent)
                                }
                                .padding(vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = cust.phone,
                                style = MaterialTheme.typography.bodySmall,
                                color = PrimaryGreen,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    if (cust.notes.isNotBlank()) {
                        Text(
                            text = "ملاحظة: ${cust.notes}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    IconButton(onClick = onShareStatementPdf, modifier = Modifier.size(36.dp)) {
                        PdfIcon(modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Debt Breakdown: Total Credit, Total Paid, Remaining Balance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "إجمالي المشتريات", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    CurrencyText(amount = summary.totalPurchasesOnCredit, fontSize = 14)
                }
                Column {
                    Text(text = "المسدد", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    CurrencyText(amount = summary.totalPaid, fontSize = 14, color = ProfitGreen)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "الرصيد المتبقي", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    CurrencyText(
                        amount = summary.remainingBalance,
                        fontSize = 16,
                        fontWeight = FontWeight.Bold,
                        color = if (summary.remainingBalance > 0.01) DebtPurple else ProfitGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Payment Progress Bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = ProfitGreen,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Actions: Settle / Record Payment button, View Ledger, Share PDF
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onRecordPayment,
                    modifier = Modifier.weight(1.2f).testTag("record_payment_button_${cust.id}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = PrimaryGreenLight,
                        contentColor = PrimaryGreen
                    )
                ) {
                    Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "تسجيل سداد", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onViewLedger,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "السجل", fontSize = 12.sp)
                }

                FilledTonalButton(
                    onClick = onShareStatementPdf,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = DebtPurpleContainer,
                        contentColor = DebtPurple
                    )
                ) {
                    PdfIcon(modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "كشف PDF", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun RecordPaymentDialog(
    customerSummary: CustomerWithDebtSummary,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, notes: String, paymentMethod: String) -> Unit
) {
    val remaining = customerSummary.remainingBalance
    var amountText by remember { mutableStateOf(if (remaining > 0) String.format(Locale.US, "%.1f", remaining) else "") }
    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethods.CASH) }
    var notes by remember { mutableStateOf("دفعة سداد") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "تسجيل سداد دين: ${customerSummary.customer.name}", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DebtPurpleContainer)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "الرصيد المتبقي بذمته:", style = MaterialTheme.typography.bodyMedium, color = DebtPurple)
                        CurrencyText(amount = remaining, fontSize = 18, fontWeight = FontWeight.Bold, color = DebtPurple)
                    }
                }

                // Payment Method Selector
                Text(text = "طريقة استلام الدفعة *:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val methods = listOf(
                        PaymentMethods.CASH to "كاش",
                        PaymentMethods.PALPAY to "بال بي",
                        PaymentMethods.JAWWAL_PAY to "جوال بي",
                        PaymentMethods.BOP to "بنك فلسطين"
                    )

                    methods.forEach { (method, label) ->
                        val isSelected = selectedPaymentMethod == method
                        val color = PaymentMethods.getColor(method)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) color else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedPaymentMethod = method }
                                .testTag("payment_method_$method")
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        isError = false
                    },
                    label = { Text("المبلغ المسدد (₪) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = isError,
                    modifier = Modifier.fillMaxWidth().testTag("payment_amount_input")
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick note tags
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("دفعة نقدية", "تسديد كامل الحساب", "حوالة بنكية", "شيك", "تسوية وخصم").take(3).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { notes = tag }
                        ) {
                            Text(text = tag, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                if (isError) {
                    Text(text = "يرجى إدخال مبلغ صحيح أكبر من 0", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (amt != null && amt > 0) {
                        onConfirm(amt, notes, selectedPaymentMethod)
                    } else {
                        isError = true
                    }
                },
                modifier = Modifier.testTag("confirm_record_payment_button")
            ) {
                Text("تأكيد السداد")
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
fun CustomerLedgerDialog(
    customerSummary: CustomerWithDebtSummary,
    payments: List<DebtPayment>,
    onDismiss: () -> Unit,
    onRecordPayment: () -> Unit,
    onSharePdf: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale("ar")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "كشف حساب: ${customerSummary.customer.name}", fontWeight = FontWeight.Bold)
                    Text(
                        text = "الرصيد المتبقي: ${String.format(Locale.US, "%.2f", customerSummary.remainingBalance)} ₪",
                        style = MaterialTheme.typography.bodySmall,
                        color = DebtPurple
                    )
                }
                IconButton(onClick = onSharePdf) {
                    PdfIcon(modifier = Modifier.size(20.dp))
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = "سجل الدفعات المسددة:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))

                if (payments.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "لم يتم تسجيل أي دفعات سداد سابقة لهذا الزبون.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(payments) { p ->
                            val methodBadge = PaymentMethods.getShortName(p.paymentMethod)
                            val methodColor = PaymentMethods.getColor(p.paymentMethod)
                            val methodContainer = PaymentMethods.getContainerColor(p.paymentMethod)

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = methodContainer
                                            ) {
                                                Text(
                                                    text = methodBadge,
                                                    color = methodColor,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(text = dateFormat.format(Date(p.paymentDate)), style = MaterialTheme.typography.bodySmall)
                                        }
                                        if (p.notes.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(text = p.notes, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    CurrencyText(amount = p.amount, fontSize = 15, fontWeight = FontWeight.Bold, color = ProfitGreen)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onSharePdf) {
                    PdfIcon(modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("كشف PDF")
                }
                Button(onClick = onRecordPayment) {
                    Text("تسجيل سداد")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

@Composable
fun CustomerFormDialog(
    title: String,
    customer: Customer?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, notes: String) -> Unit
) {
    var name by remember { mutableStateOf(customer?.name ?: "") }
    var phone by remember { mutableStateOf(customer?.phone ?: "") }
    var notes by remember { mutableStateOf(customer?.notes ?: "") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        isError = false
                    },
                    label = { Text("اسم الزبون *") },
                    singleLine = true,
                    isError = isError,
                    modifier = Modifier.fillMaxWidth().testTag("customer_name_input")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف (اختياري)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("customer_phone_input")
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات (مكان السكن، عنوان، إلخ)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isError) {
                    Text(text = "يرجى كتابة اسم الزبون", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, phone, notes)
                    } else {
                        isError = true
                    }
                },
                modifier = Modifier.testTag("confirm_customer_form_button")
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
