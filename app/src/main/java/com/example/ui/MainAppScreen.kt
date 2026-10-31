package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.ui.components.CurrencyText
import com.example.ui.screens.BackupExportScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DebtsScreen
import com.example.ui.screens.ExpensesScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SalesScreen
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.PrimaryGreenLight
import com.example.ui.theme.StockAlertRed

enum class BastaNavScreen(val title: String, val icon: ImageVector) {
    DASHBOARD("الرئيسية", Icons.Default.Dashboard),
    INVENTORY("المخزون", Icons.Default.Inventory2),
    SALES("المبيعات", Icons.Default.PointOfSale),
    DEBTS("الديون", Icons.Default.CreditCard),
    EXPENSES("المصاريف", Icons.Default.ReceiptLong),
    REPORTS("التقارير", Icons.Default.Assessment)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: BastaViewModel = viewModel()
) {
    var currentScreen by remember { mutableStateOf(BastaNavScreen.DASHBOARD) }
    var isBackupScreenOpen by remember { mutableStateOf(false) }

    val userMessage by viewModel.userMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val dashboardStats by viewModel.dashboardStats.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val cartCount = remember(cart) { cart.values.sum() }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    // BackHandler to handle custom screen backstack
    BackHandler(enabled = isBackupScreenOpen || currentScreen != BastaNavScreen.DASHBOARD) {
        if (isBackupScreenOpen) {
            isBackupScreenOpen = false
        } else if (currentScreen != BastaNavScreen.DASHBOARD) {
            currentScreen = BastaNavScreen.DASHBOARD
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (!isBackupScreenOpen) {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PrimaryGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "مدير البسطة",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    actions = {
                        // Backup & Export icon button
                        IconButton(
                            onClick = { isBackupScreenOpen = true },
                            modifier = Modifier.testTag("topbar_backup_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Backup,
                                contentDescription = "النسخ الاحتياطي والتصدير",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        bottomBar = {
            if (!isBackupScreenOpen) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    BastaNavScreen.values().forEach { screen ->
                        val isSelected = currentScreen == screen
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentScreen = screen },
                            icon = {
                                if (screen == BastaNavScreen.SALES && cartCount > 0) {
                                    BadgedBox(badge = { Badge { Text("$cartCount") } }) {
                                        Icon(imageVector = screen.icon, contentDescription = screen.title)
                                    }
                                } else if (screen == BastaNavScreen.INVENTORY && dashboardStats.lowStockCount > 0) {
                                    BadgedBox(
                                        badge = {
                                            Badge(containerColor = StockAlertRed) {
                                                Text("${dashboardStats.lowStockCount}")
                                            }
                                        }
                                    ) {
                                        Icon(imageVector = screen.icon, contentDescription = screen.title)
                                    }
                                } else {
                                    Icon(imageVector = screen.icon, contentDescription = screen.title)
                                }
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryGreen,
                                selectedTextColor = PrimaryGreen,
                                indicatorColor = PrimaryGreenLight
                            ),
                            modifier = Modifier.testTag("nav_item_${screen.name}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isBackupScreenOpen) {
                BackupExportScreen(
                    viewModel = viewModel,
                    onBack = { isBackupScreenOpen = false }
                )
            } else {
                when (currentScreen) {
                    BastaNavScreen.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToSales = { currentScreen = BastaNavScreen.SALES },
                        onNavigateToInventory = { currentScreen = BastaNavScreen.INVENTORY },
                        onNavigateToDebts = { currentScreen = BastaNavScreen.DEBTS },
                        onNavigateToExpenses = { currentScreen = BastaNavScreen.EXPENSES }
                    )
                    BastaNavScreen.INVENTORY -> InventoryScreen(viewModel = viewModel)
                    BastaNavScreen.SALES -> SalesScreen(viewModel = viewModel)
                    BastaNavScreen.DEBTS -> DebtsScreen(viewModel = viewModel)
                    BastaNavScreen.EXPENSES -> ExpensesScreen(viewModel = viewModel)
                    BastaNavScreen.REPORTS -> ReportsScreen(
                        viewModel = viewModel,
                        onNavigateToBackup = { isBackupScreenOpen = true }
                    )
                }
            }
        }
    }
}
