package com.example.bastamanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// نموذج بيانات المنتج
data class Product(
    val id: String,
    val name: String,
    val stockQuantity: Int, // كمية المخزون المتوفرة
    val price: Double
)

class BastaViewModel : ViewModel() {

    // قائمة المنتجات الحالية (كمثال تجريبي)
    private val _products = MutableStateFlow<List<Product>>(
        listOf(
            Product("1", "صنف أول", 10, 5.0),
            Product("2", "صنف ثانٍ", 0, 12.0) // نفد من المخزون
        )
    )
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    // رسائل التنبيه والخطأ للشاشة
    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    // دالة التحقق وإتمام البيع مع منع البيع عند نفاد المخزون
    fun sellProduct(productId: String, requestedQuantity: Int) {
        val currentList = _products.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == productId }

        if (index != -1) {
            val product = currentList[index]

            // 1. التحقق مما إذا كان المخزون صفراً
            if (product.stockQuantity <= 0) {
                _uiMessage.value = "عذراً، المنتج (${product.name}) نفد من المخزون تماماً!"
                return
            }

            // 2. التحقق مما إذا كانت الكمية المطلوبة أكبر من المتوفر
            if (requestedQuantity > product.stockQuantity) {
                _uiMessage.value = "الكمية المطلوبة غير متوفرة! المتبقي في المخزون فقط: ${product.stockQuantity}"
                return
            }

            // 3. تحديث المخزون بعد البيع بنجاح
            val updatedProduct = product.copy(stockQuantity = product.stockQuantity - requestedQuantity)
            currentList[index] = updatedProduct
            _products.value = currentList
            _uiMessage.value = "تمت عملية البيع بنجاح والمخزون المتبقي: ${updatedProduct.stockQuantity}"
        }
    }

    // تصفير الرسالة بعد عرضها
    fun clearMessage() {
        _uiMessage.value = null
    }
}
