package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Notes
import com.example.ui.components.PdfIcon
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.data.model.Category
import com.example.data.model.Product
import com.example.ui.BastaViewModel
import com.example.ui.components.CategoryChip
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.CurrencyText
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.PrimaryGreenLight
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.StockAlertRed
import com.example.ui.theme.StockAlertRedContainer

@Composable
fun InventoryScreen(
    viewModel: BastaViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val products by viewModel.products.collectAsState()
    val dbCategories by viewModel.categories.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("الكل") }
    var showOnlyLowStock by remember { mutableStateOf(false) }

    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showCategoryManagerDialog by remember { mutableStateOf(false) }

    // Categories list: "الكل" + dynamic categories from db
    val categoryNames = remember(dbCategories) {
        listOf("الكل") + dbCategories.map { it.name }
    }

    // Filtered products
    val filteredProducts = products.filter { p ->
        val matchesSearch = p.name.contains(searchQuery, ignoreCase = true) ||
                p.barcode.contains(searchQuery) ||
                p.notes.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == "الكل" || p.category == selectedCategory
        val matchesLowStock = !showOnlyLowStock || p.isLowStock
        matchesSearch && matchesCategory && matchesLowStock
    }

    // Valuation calculations
    val totalCostValue = products.sumOf { it.totalCostValue }
    val totalRetailValue = products.sumOf { it.totalRetailValue }
    val expectedProfit = totalRetailValue - totalCostValue
    val lowStockCount = products.count { it.isLowStock }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // Screen Title and Header Actions (PDF Export + Category Manager)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "المنتجات والمخزون",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "إجمالي ${products.size} صنف في المستودع والبسطة",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // PDF Share Button
                        FilledTonalButton(
                            onClick = { viewModel.shareInventoryReportPdf(context) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("share_inventory_pdf_button")
                        ) {
                            PdfIcon(modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تقرير PDF", fontSize = 12.sp)
                        }

                        // Category Manager Button
                        IconButton(
                            onClick = { showCategoryManagerDialog = true },
                            modifier = Modifier.testTag("manage_categories_button")
                        ) {
                            Icon(imageVector = Icons.Default.Category, contentDescription = "إدارة التصنيفات", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // Inventory Valuation Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("inventory_summary_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "تقييم المخزون الحالي",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "سعر التكلفة (رأس المال)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                CurrencyText(amount = totalCostValue, fontSize = 16, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text(text = "سعر البيع المتوقع", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                CurrencyText(amount = totalRetailValue, fontSize = 16, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Column {
                                Text(text = "الأرباح المتوقعة", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                CurrencyText(amount = expectedProfit, fontSize = 16, fontWeight = FontWeight.Bold, color = ProfitGreen)
                            }
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_product_input"),
                    placeholder = { Text("بحث عن منتج، باركود، أو ملاحظات...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "مسح")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
            }

            // Categories Filter Chips & Low Stock toggle
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Low stock toggle chip
                    if (lowStockCount > 0) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { showOnlyLowStock = !showOnlyLowStock }
                                .testTag("low_stock_filter_chip"),
                            shape = RoundedCornerShape(20.dp),
                            color = if (showOnlyLowStock) StockAlertRed else StockAlertRedContainer,
                            contentColor = if (showOnlyLowStock) Color.White else StockAlertRed
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "نواقص ($lowStockCount)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    categoryNames.forEach { cat ->
                        CategoryChip(
                            category = cat,
                            isSelected = selectedCategory == cat && !showOnlyLowStock,
                            onClick = {
                                selectedCategory = cat
                                showOnlyLowStock = false
                            }
                        )
                    }
                }
            }

            // Product Cards List
            if (filteredProducts.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.Inventory,
                        title = "لا توجد منتجات مطابقة",
                        description = "أضف منتجك الأول من خلال الضغط على زر (+) في الأسفل."
                    )
                }
            } else {
                items(filteredProducts, key = { it.id }) { product ->
                    ProductCard(
                        product = product,
                        onEdit = { productToEdit = product },
                        onDelete = { productToDelete = product },
                        onAdjustStock = { delta -> viewModel.adjustStock(product.id, delta) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Floating Action Button to Add Product
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .testTag("add_product_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "إضافة منتج")
        }
    }

    // Add Product Dialog
    if (showAddDialog) {
        ProductFormDialog(
            title = "إضافة منتج جديد",
            product = null,
            categories = dbCategories,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, category, cost, sell, qty, minStock, notes ->
                viewModel.addProduct(name, category, cost, sell, qty, minStock, notes)
                showAddDialog = false
            },
            onAddNewCategory = { newCat -> viewModel.addCategory(newCat) }
        )
    }

    // Edit Product Dialog
    productToEdit?.let { prod ->
        ProductFormDialog(
            title = "تعديل المنتج",
            product = prod,
            categories = dbCategories,
            onDismiss = { productToEdit = null },
            onConfirm = { name, category, cost, sell, qty, minStock, notes ->
                viewModel.updateProduct(
                    prod.copy(
                        name = name,
                        category = category,
                        costPrice = cost,
                        sellPrice = sell,
                        quantity = qty,
                        currentStock = qty,
                        minStockAlert = minStock,
                        notes = notes
                    )
                )
                productToEdit = null
            },
            onAddNewCategory = { newCat -> viewModel.addCategory(newCat) }
        )
    }

    // Delete Confirmation Dialog
    productToDelete?.let { prod ->
        ConfirmDeleteDialog(
            title = "حذف المنتج",
            message = "هل أنت متأكد من حذف المنتج '${prod.name}' نهائياً من النظام؟",
            onConfirm = {
                viewModel.deleteProduct(prod)
                productToDelete = null
            },
            onDismiss = { productToDelete = null }
        )
    }

    // Category Manager Dialog
    if (showCategoryManagerDialog) {
        CategoryManagerDialog(
            categories = dbCategories,
            onDismiss = { showCategoryManagerDialog = false },
            onAddCategory = { viewModel.addCategory(it) },
            onUpdateCategory = { cat, newName -> viewModel.updateCategory(cat, newName) },
            onDeleteCategory = { viewModel.deleteCategory(it) }
        )
    }
}

@Composable
fun ProductCard(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAdjustStock: (delta: Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_card_${product.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Name, Category, Edit & Delete buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = product.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Detailed Notes if present
            if (product.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notes, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = product.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Pricing & Margins Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "سعر الشراء", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    CurrencyText(amount = product.costPrice, fontSize = 14)
                }
                Column {
                    Text(text = "سعر البيع", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    CurrencyText(amount = product.sellPrice, fontSize = 15, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                }
                Column {
                    Text(text = "ربح القطعة", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    CurrencyText(amount = product.profitMarginPerUnit, fontSize = 14, color = ProfitGreen)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stock Controls Row (+ / - buttons and low stock warning)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stock Badge
                if (product.isOutOfStock) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(StockAlertRedContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = "نفد المخزون!", color = StockAlertRed, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                } else if (product.isLowStock) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(StockAlertRedContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = "منخفض (متبقي: ${product.currentStock})", color = StockAlertRed, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(
                        text = "الكمية المتوفرة: ${product.currentStock}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Quick Stock Adjustment Buttons (+ / -)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { if (product.currentStock > 0) onAdjustStock(-1) },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        enabled = product.currentStock > 0
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "إنقاص 1", modifier = Modifier.size(16.dp))
                    }

                    Text(
                        text = "${product.currentStock}",
                        modifier = Modifier.padding(horizontal = 12.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = { onAdjustStock(1) },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(PrimaryGreenLight)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "زيادة 1", tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ProductFormDialog(
    title: String,
    product: Product?,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, category: String, costPrice: Double, sellPrice: Double, quantity: Int, minStock: Int, notes: String) -> Unit,
    onAddNewCategory: (String) -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var selectedCategory by remember { mutableStateOf(product?.category ?: (categories.firstOrNull()?.name ?: "حلويات")) }
    var costPriceText by remember { mutableStateOf(product?.costPrice?.toString() ?: "") }
    var sellPriceText by remember { mutableStateOf(product?.sellPrice?.toString() ?: "") }
    var quantityText by remember { mutableStateOf(product?.quantity?.toString() ?: "10") }
    var minStockText by remember { mutableStateOf(product?.minStockAlert?.toString() ?: "5") }
    var notes by remember { mutableStateOf(product?.notes ?: "") }
    var isError by remember { mutableStateOf(false) }

    var showNewCategoryInput by remember { mutableStateOf(false) }
    var newCategoryText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title, fontWeight = FontWeight.Bold)
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم المنتج *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("product_name_input")
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "التصنيف:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { showNewCategoryInput = !showNewCategoryInput }) {
                            Text(text = if (showNewCategoryInput) "إلغاء" else "+ تصنيف جديد", fontSize = 12.sp)
                        }
                    }

                    if (showNewCategoryInput) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = newCategoryText,
                                onValueChange = { newCategoryText = it },
                                label = { Text("اسم التصنيف الجديد") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    if (newCategoryText.isNotBlank()) {
                                        onAddNewCategory(newCategoryText.trim())
                                        selectedCategory = newCategoryText.trim()
                                        newCategoryText = ""
                                        showNewCategoryInput = false
                                    }
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("إضافة")
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSel = cat.name == selectedCategory
                            FilledTonalButton(
                                onClick = { selectedCategory = cat.name },
                                shape = RoundedCornerShape(8.dp),
                                colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text(text = cat.name, fontSize = 12.sp)
                            }
                        }
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = costPriceText,
                            onValueChange = { costPriceText = it },
                            label = { Text("سعر الشراء (₪) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("product_cost_input")
                        )
                        OutlinedTextField(
                            value = sellPriceText,
                            onValueChange = { sellPriceText = it },
                            label = { Text("سعر البيع (₪) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("product_sell_input")
                        )
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = quantityText,
                            onValueChange = { quantityText = it },
                            label = { Text("الكمية الحالية *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("product_qty_input")
                        )
                        OutlinedTextField(
                            value = minStockText,
                            onValueChange = { minStockText = it },
                            label = { Text("تنبيه عند أقل من") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("ملاحظات وتفاصيل الصنف (المورد، مكان الحفظ، إلخ)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (isError) {
                    item {
                        Text(
                            text = "يرجى تعبئة الحقول الأساسية بشكل صحيح وقيم موجبة.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cost = costPriceText.toDoubleOrNull()
                    val sell = sellPriceText.toDoubleOrNull()
                    val qty = quantityText.toIntOrNull()
                    val minStock = minStockText.toIntOrNull() ?: 5

                    if (name.isNotBlank() && cost != null && sell != null && qty != null && cost >= 0 && sell >= 0 && qty >= 0) {
                        onConfirm(name, selectedCategory, cost, sell, qty, minStock, notes)
                    } else {
                        isError = true
                    }
                },
                modifier = Modifier.testTag("confirm_product_form_button")
            ) {
                Text("حفظ المنتج")
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
fun CategoryManagerDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onAddCategory: (String) -> Unit,
    onUpdateCategory: (Category, String) -> Unit,
    onDeleteCategory: (Category) -> Unit
) {
    var newCategoryName by remember { mutableStateOf("") }
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }
    var editName by remember { mutableStateOf("") }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "إدارة تصنيفات المنتجات", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Add category input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        placeholder = { Text("اسم التصنيف الجديد...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (newCategoryName.isNotBlank()) {
                                onAddCategory(newCategoryName.trim())
                                newCategoryName = ""
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("إضافة")
                    }
                }

                Divider()

                Text(text = "التصنيفات الحالية (${categories.size}):", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(categories) { cat ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = cat.name, fontWeight = FontWeight.Medium)
                                Row {
                                    IconButton(
                                        onClick = {
                                            categoryToEdit = cat
                                            editName = cat.name
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    }
                                    if (cat.name != "أخرى") {
                                        IconButton(
                                            onClick = { categoryToDelete = cat },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )

    // Edit Category Sub-Dialog
    categoryToEdit?.let { cat ->
        AlertDialog(
            onDismissRequest = { categoryToEdit = null },
            title = { Text(text = "تعديل اسم التصنيف") },
            text = {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("الاسم الجديد") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank()) {
                            onUpdateCategory(cat, editName.trim())
                            categoryToEdit = null
                        }
                    }
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToEdit = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Delete Category Confirmation
    categoryToDelete?.let { cat ->
        ConfirmDeleteDialog(
            title = "حذف التصنيف",
            message = "هل أنت متأكد من حذف تصنيف '${cat.name}'؟ سيتم نقل المنتجات التابعة له تلقائياً إلى تصنيف 'أخرى'.",
            onConfirm = {
                onDeleteCategory(cat)
                categoryToDelete = null
            },
            onDismiss = { categoryToDelete = null }
        )
    }
}
