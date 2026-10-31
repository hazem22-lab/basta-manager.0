package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PdfIcon
import androidx.compose.ui.platform.LocalContext
import com.example.ui.BastaViewModel
import com.example.ui.ReportPeriod
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
import com.example.ui.theme.StockAlertRed
import com.example.ui.theme.StockAlertRedContainer
import java.util.Locale

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun ReportsScreen(
    viewModel: BastaViewModel,
    onNavigateToBackup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedPeriod by viewModel.selectedReportPeriod.collectAsState()
    val stats by viewModel.periodReportStats.collectAsState()
    val products by viewModel.products.collectAsState()
    val customersWithDebt by viewModel.customersWithDebt.collectAsState()

    val totalInventoryCost = products.sumOf { it.totalCostValue }
    val totalInventoryRetail = products.sumOf { it.totalRetailValue }
    val totalDebts = customersWithDebt.sumOf { maxOf(0.0, it.remainingBalance) }

    val isProfitPositive = stats.netProfit >= 0

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        text = "التقارير المحاسبية والأرباح",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "تحليل الأداء المالي وصافي الأرباح والخسائر",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(
                        onClick = { viewModel.shareFinancialReportPdf(context) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("reports_pdf_button")
                    ) {
                        PdfIcon(modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "تقرير PDF", fontSize = 12.sp)
                    }

                    FilledTonalButton(
                        onClick = onNavigateToBackup,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("reports_export_button")
                    ) {
                        Icon(imageVector = Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "التقارير", fontSize = 12.sp)
                    }
                }
            }
        }

        // Period Tabs
        item {
            PrimaryTabRow(
                selectedTabIndex = selectedPeriod.ordinal,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            ) {
                ReportPeriod.values().forEach { period ->
                    Tab(
                        selected = selectedPeriod == period,
                        onClick = { viewModel.setReportPeriod(period) },
                        text = { Text(text = period.title, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("tab_period_${period.name}")
                    )
                }
            }
        }

        // Net Profit / Loss Hero Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("net_profit_report_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isProfitPositive) ProfitGreenContainer else StockAlertRedContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "صافي الربح (${selectedPeriod.title})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isProfitPositive) ProfitGreen else StockAlertRed
                            )
                            Text(
                                text = if (isProfitPositive) "مجمل ربح المبيعات بعد خصم كافة المصاريف" else "المصاريف تجاوزت مجمل أرباح المبيعات",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isProfitPositive) ProfitGreen.copy(alpha = 0.8f) else StockAlertRed.copy(alpha = 0.8f)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background((if (isProfitPositive) ProfitGreen else StockAlertRed).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isProfitPositive) Icons.Default.TrendingUp else Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = if (isProfitPositive) ProfitGreen else StockAlertRed,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    CurrencyText(
                        amount = stats.netProfit,
                        fontSize = 32,
                        fontWeight = FontWeight.Bold,
                        color = if (isProfitPositive) ProfitGreen else StockAlertRed
                    )
                }
            }
        }

        // Accounting Income Statement Breakdown Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("income_statement_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "تفصيل قائمة الدخل (${selectedPeriod.title})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    ReportMetricRow(
                        label = "إجمالي المبيعات (الإيرادات):",
                        amount = stats.totalRevenue,
                        color = MaterialTheme.colorScheme.primary,
                        bold = true
                    )

                    ReportMetricRow(
                        label = "  • مبيعات نقدية (كاش):",
                        amount = stats.cashRevenue,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    ReportMetricRow(
                        label = "  • مبيعات بالدين (آجلة):",
                        amount = stats.creditRevenue,
                        color = DebtPurple
                    )

                    Divider(color = MaterialTheme.colorScheme.outlineVariant)

                    ReportMetricRow(
                        label = "تكلفة البضاعة المباعة (COGS):",
                        amount = stats.costOfGoodsSold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    ReportMetricRow(
                        label = "مجمل الربح (المبيعات - التكلفة):",
                        amount = stats.grossProfit,
                        color = ProfitGreen,
                        bold = true
                    )

                    Divider(color = MaterialTheme.colorScheme.outlineVariant)

                    ReportMetricRow(
                        label = "إجمالي المصاريف والنفقات:",
                        amount = stats.totalExpenses,
                        color = AmberPrimary,
                        bold = true
                    )

                    Divider(color = MaterialTheme.colorScheme.outlineVariant)

                    ReportMetricRow(
                        label = "صافي الربح / الخسارة النهائي:",
                        amount = stats.netProfit,
                        color = if (isProfitPositive) ProfitGreen else StockAlertRed,
                        bold = true,
                        fontSize = 18
                    )
                }
            }
        }

        // Assets & Receivables Summary
        item {
            Text(
                text = "الأصول والذمم المدينة الإجمالية",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Total Inventory Value at Cost
                MetricCard(
                    title = "قيمة المخزون (بالتكلفة)",
                    value = String.format(Locale.US, "%.1f ₪", totalInventoryCost),
                    subtext = "بسعر البيع: ${String.format(Locale.US, "%.1f ₪", totalInventoryRetail)}",
                    icon = Icons.Default.Inventory2,
                    containerColor = PrimaryGreenLight,
                    contentColor = PrimaryGreen,
                    modifier = Modifier.weight(1f)
                )

                // Total Receivables (Outstanding debts)
                MetricCard(
                    title = "إجمالي الديون المستحقة",
                    value = String.format(Locale.US, "%.1f ₪", totalDebts),
                    subtext = "ذمم على الزبائن بالخارج",
                    icon = Icons.Default.CreditCard,
                    containerColor = DebtPurpleContainer,
                    contentColor = DebtPurple,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ReportMetricRow(
    label: String,
    amount: Double,
    color: Color = Color.Unspecified,
    bold: Boolean = false,
    fontSize: Int = 15
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
        CurrencyText(
            amount = amount,
            fontSize = fontSize,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = color
        )
    }
}
