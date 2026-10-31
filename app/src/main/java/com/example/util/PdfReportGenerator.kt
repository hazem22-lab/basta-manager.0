package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.data.model.CustomerWithDebtSummary
import com.example.data.model.DebtPayment
import com.example.data.model.Expense
import com.example.data.model.PaymentMethods
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.SaleWithItems
import com.example.ui.PeriodReportStats
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    private const val PAGE_WIDTH = 595  // A4 standard points
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_X = 36f
    private const val MARGIN_Y = 40f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN_X * 2)

    private val COLOR_PRIMARY = Color.rgb(30, 94, 72)     // #1E5E48
    private val COLOR_PRIMARY_LIGHT = Color.rgb(235, 245, 240)
    private val COLOR_TEXT_DARK = Color.rgb(30, 41, 59)
    private val COLOR_TEXT_MUTED = Color.rgb(100, 116, 139)
    private val COLOR_BORDER = Color.rgb(226, 232, 240)
    private val COLOR_ROW_ALT = Color.rgb(248, 250, 252)
    private val COLOR_ROW_WHITE = Color.WHITE
    private val COLOR_ACCENT_RED = Color.rgb(220, 38, 38)
    private val COLOR_ACCENT_GREEN = Color.rgb(22, 163, 74)
    private val COLOR_ACCENT_PURPLE = Color.rgb(124, 58, 237)

    private fun getReportsDir(context: Context): File {
        val dir = File(context.cacheDir, "reports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun sharePdf(context: Context, pdfFile: File, title: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة تقرير PDF عبر"))
    }

    fun viewPdf(context: Context, pdfFile: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "عرض تقرير PDF"))
    }

    // Helper to draw text with full Arabic shaping & RTL BiDi
    private fun drawArabicText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        width: Float,
        textSize: Float = 10f,
        isBold: Boolean = false,
        color: Int = COLOR_TEXT_DARK,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
    ): Float {
        val textPaint = TextPaint().apply {
            this.color = color
            this.textSize = textSize
            this.isAntiAlias = true
            this.typeface = if (isBold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val safeText = if (text.isBlank()) "-" else text
        val safeWidth = maxOf(10, width.toInt())

        val layout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(safeText, 0, safeText.length, textPaint, safeWidth)
                .setAlignment(alignment)
                .setTextDirection(TextDirectionHeuristics.RTL)
                .setIncludePad(false)
                .build()
        } else {
            StaticLayout(safeText, textPaint, safeWidth, alignment, 1f, 0f, false)
        }

        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()

        return layout.height.toFloat()
    }

    private fun drawHeader(canvas: Canvas, title: String, subtitle: String, dateStr: String): Float {
        val paint = Paint().apply { isAntiAlias = true }

        // Top Header Box
        paint.color = COLOR_PRIMARY
        canvas.drawRoundRect(RectF(MARGIN_X, MARGIN_Y, MARGIN_X + CONTENT_WIDTH, MARGIN_Y + 70f), 8f, 8f, paint)

        // Title
        drawArabicText(
            canvas = canvas,
            text = "مدير البسطة - $title",
            x = MARGIN_X + 16f,
            y = MARGIN_Y + 12f,
            width = CONTENT_WIDTH - 32f,
            textSize = 16f,
            isBold = true,
            color = Color.WHITE
        )

        // Subtitle & Date
        drawArabicText(
            canvas = canvas,
            text = "$subtitle  •  تاريخ التقرير: $dateStr",
            x = MARGIN_X + 16f,
            y = MARGIN_Y + 40f,
            width = CONTENT_WIDTH - 32f,
            textSize = 10f,
            isBold = false,
            color = Color.rgb(209, 250, 229)
        )

        return MARGIN_Y + 84f
    }

    // 1. Sales Report PDF
    fun generateSalesReportPdf(context: Context, sales: List<SaleWithItems>, periodTitle: String): File {
        val document = PdfDocument()
        val dateFormat = SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale("ar"))
        val nowStr = dateFormat.format(Date())

        var pageNum = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
        var canvas = page.canvas

        var currentY = drawHeader(canvas, "كشف وسجل المبيعات", "الفترة: $periodTitle", nowStr)

        // Summary Card
        val totalRevenue = sales.sumOf { it.sale.totalAmount }
        val cashRevenue = sales.filter { it.sale.paymentType == PaymentMethods.CASH }.sumOf { it.sale.totalAmount }
        val electronicRevenue = sales.filter { PaymentMethods.isElectronic(it.sale.paymentType) }.sumOf { it.sale.totalAmount }
        val creditRevenue = sales.filter { it.sale.paymentType == PaymentMethods.CREDIT }.sumOf { it.sale.totalAmount }
        val totalProfit = sales.sumOf { it.sale.profit }

        val bgPaint = Paint().apply { isAntiAlias = true; color = COLOR_PRIMARY_LIGHT }
        canvas.drawRoundRect(RectF(MARGIN_X, currentY, MARGIN_X + CONTENT_WIDTH, currentY + 46f), 6f, 6f, bgPaint)

        val summaryText = "إجمالي المبيعات: ${String.format(Locale.US, "%.2f", totalRevenue)} ₪  |  كاش: ${String.format(Locale.US, "%.2f", cashRevenue)} ₪  |  تطبيقات وبنك: ${String.format(Locale.US, "%.2f", electronicRevenue)} ₪  |  دين: ${String.format(Locale.US, "%.2f", creditRevenue)} ₪  |  الربح: ${String.format(Locale.US, "%.2f", totalProfit)} ₪"
        drawArabicText(canvas, summaryText, MARGIN_X + 10f, currentY + 14f, CONTENT_WIDTH - 20f, textSize = 9.5f, isBold = true, color = COLOR_PRIMARY)

        currentY += 58f

        // Table Columns (RTL): [رقم الفاتورة, التاريخ, نوع الدفع, الزبون / الأصناف, التكلفة, الإجمالي, الربح]
        val colWidths = floatArrayOf(45f, 90f, 65f, 155f, 60f, 60f, 60f)
        val headers = arrayOf("رقم", "التاريخ والوقت", "طريقة الدفع", "الأصناف / الملاحظات", "التكلفة", "المبلغ", "الربح")

        fun drawTableHeader(c: Canvas, y: Float) {
            val hPaint = Paint().apply { isAntiAlias = true; color = COLOR_PRIMARY }
            c.drawRect(MARGIN_X, y, MARGIN_X + CONTENT_WIDTH, y + 24f, hPaint)
            var curX = MARGIN_X + CONTENT_WIDTH
            for (i in headers.indices) {
                curX -= colWidths[i]
                drawArabicText(c, headers[i], curX + 4f, y + 6f, colWidths[i] - 8f, textSize = 9f, isBold = true, color = Color.WHITE)
            }
        }

        drawTableHeader(canvas, currentY)
        currentY += 24f

        val rowPaint = Paint().apply { isAntiAlias = true }
        val linePaint = Paint().apply { color = COLOR_BORDER; strokeWidth = 0.5f }

        for ((index, item) in sales.withIndex()) {
            if (currentY > PAGE_HEIGHT - 60f) {
                document.finishPage(page)
                pageNum++
                page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
                canvas = page.canvas
                currentY = MARGIN_Y
                drawTableHeader(canvas, currentY)
                currentY += 24f
            }

            rowPaint.color = if (index % 2 == 0) COLOR_ROW_WHITE else COLOR_ROW_ALT
            canvas.drawRect(MARGIN_X, currentY, MARGIN_X + CONTENT_WIDTH, currentY + 28f, rowPaint)
            canvas.drawLine(MARGIN_X, currentY + 28f, MARGIN_X + CONTENT_WIDTH, currentY + 28f, linePaint)

            val s = item.sale
            val pType = when (s.paymentType) {
                PaymentMethods.CASH -> "كاش"
                PaymentMethods.PALPAY -> "بال بي"
                PaymentMethods.JAWWAL_PAY -> "جوال بي"
                PaymentMethods.BOP -> "بنك فلسطين"
                PaymentMethods.CREDIT -> "دين (${s.customerName ?: "عميل"})"
                else -> s.paymentType
            }
            val itemsDesc = if (s.isDailyAggregate) "مبيعات إجمالية نهاية اليوم"
            else if (item.items.isNotEmpty()) item.items.joinToString("، ") { "${it.productName}(x${it.quantity})" }
            else if (s.notes.isNotBlank()) s.notes else "فاتورة سريعة"

            val timeStr = SimpleDateFormat("MM/dd HH:mm", Locale.ENGLISH).format(Date(s.saleDate))

            val values = arrayOf(
                "#${s.id}",
                timeStr,
                pType,
                itemsDesc,
                String.format(Locale.US, "%.1f ₪", s.totalCost),
                String.format(Locale.US, "%.1f ₪", s.totalAmount),
                String.format(Locale.US, "%.1f ₪", s.profit)
            )

            var curX = MARGIN_X + CONTENT_WIDTH
            for (i in values.indices) {
                curX -= colWidths[i]
                val textColor = if (i == 5) COLOR_PRIMARY else if (i == 6) COLOR_ACCENT_GREEN else COLOR_TEXT_DARK
                drawArabicText(canvas, values[i], curX + 4f, currentY + 6f, colWidths[i] - 8f, textSize = 8.5f, isBold = (i == 0 || i == 5), color = textColor)
            }

            currentY += 28f
        }

        document.finishPage(page)

        val outputFile = File(getReportsDir(context), "تقرير_مبيعات_البسطة_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outputFile).use { out -> document.writeTo(out) }
        document.close()
        return outputFile
    }

    // 2. Single Sale Invoice Receipt PDF
    fun generateSingleInvoicePdf(context: Context, saleWithItems: SaleWithItems): File {
        val document = PdfDocument()
        val s = saleWithItems.sale
        val page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
        val canvas = page.canvas

        val dateFormat = SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale("ar"))
        val dateStr = dateFormat.format(Date(s.saleDate))

        var currentY = drawHeader(canvas, "فاتورة بيع رقم #${s.id}", "إشعار بيع صادر", dateStr)

        // Customer & Payment Info Card
        val infoPaint = Paint().apply { isAntiAlias = true; color = COLOR_PRIMARY_LIGHT }
        canvas.drawRoundRect(RectF(MARGIN_X, currentY, MARGIN_X + CONTENT_WIDTH, currentY + 60f), 8f, 8f, infoPaint)

        val payText = when (s.paymentType) {
            PaymentMethods.CASH -> "طريقة الدفع: نقداً (كاش)"
            PaymentMethods.PALPAY -> "طريقة الدفع: تطبيق بال بي (PalPay)"
            PaymentMethods.JAWWAL_PAY -> "طريقة الدفع: تطبيق جوال بي (Jawwal Pay)"
            PaymentMethods.BOP -> "طريقة الدفع: بنك فلسطين (Bank of Palestine)"
            PaymentMethods.CREDIT -> "طريقة الدفع: ذمة / دين على الزبون (${s.customerName ?: "-"})"
            else -> "طريقة الدفع: ${s.paymentType}"
        }
        drawArabicText(canvas, payText, MARGIN_X + 16f, currentY + 12f, CONTENT_WIDTH - 32f, textSize = 11f, isBold = true, color = COLOR_PRIMARY)

        if (s.notes.isNotBlank()) {
            drawArabicText(canvas, "ملاحظات الفاتورة: ${s.notes}", MARGIN_X + 16f, currentY + 36f, CONTENT_WIDTH - 32f, textSize = 9.5f, isBold = false, color = COLOR_TEXT_DARK)
        }

        currentY += 76f

        // Items Table
        val colWidths = floatArrayOf(40f, 235f, 80f, 80f, 100f)
        val headers = arrayOf("#", "اسم الصنف والبيان", "الكمية", "سعر الوحدة", "الإجمالي")

        val hPaint = Paint().apply { isAntiAlias = true; color = COLOR_PRIMARY }
        canvas.drawRect(MARGIN_X, currentY, MARGIN_X + CONTENT_WIDTH, currentY + 26f, hPaint)
        var curX = MARGIN_X + CONTENT_WIDTH
        for (i in headers.indices) {
            curX -= colWidths[i]
            drawArabicText(canvas, headers[i], curX + 6f, currentY + 6f, colWidths[i] - 12f, textSize = 10f, isBold = true, color = Color.WHITE)
        }
        currentY += 26f

        val linePaint = Paint().apply { color = COLOR_BORDER; strokeWidth = 0.5f }
        val rowPaint = Paint().apply { isAntiAlias = true }

        if (saleWithItems.items.isEmpty()) {
            rowPaint.color = COLOR_ROW_WHITE
            canvas.drawRect(MARGIN_X, currentY, MARGIN_X + CONTENT_WIDTH, currentY + 32f, rowPaint)
            drawArabicText(canvas, if (s.isDailyAggregate) "مبيعات إجمالية نهاية اليوم" else "فاتورة مباشرة", MARGIN_X + 10f, currentY + 8f, CONTENT_WIDTH - 20f, textSize = 10f, isBold = true)
            currentY += 32f
        } else {
            for ((index, itm) in saleWithItems.items.withIndex()) {
                rowPaint.color = if (index % 2 == 0) COLOR_ROW_WHITE else COLOR_ROW_ALT
                canvas.drawRect(MARGIN_X, currentY, MARGIN_X + CONTENT_WIDTH, currentY + 28f, rowPaint)
                canvas.drawLine(MARGIN_X, currentY + 28f, MARGIN_X + CONTENT_WIDTH, currentY + 28f, linePaint)

                val values = arrayOf(
                    "${index + 1}",
                    itm.productName,
                    "${itm.quantity} قطعة",
                    String.format(Locale.US, "%.2f ₪", itm.unitPrice),
                    String.format(Locale.US, "%.2f ₪", itm.subtotal)
                )

                curX = MARGIN_X + CONTENT_WIDTH
                for (i in values.indices) {
                    curX -= colWidths[i]
                    drawArabicText(canvas, values[i], curX + 6f, currentY + 6f, colWidths[i] - 12f, textSize = 9.5f, isBold = (i == 4), color = COLOR_TEXT_DARK)
                }
                currentY += 28f
            }
        }

        // Total Box
        currentY += 16f
        val totalBoxPaint = Paint().apply { isAntiAlias = true; color = COLOR_PRIMARY_LIGHT }
        canvas.drawRoundRect(RectF(MARGIN_X + CONTENT_WIDTH - 220f, currentY, MARGIN_X + CONTENT_WIDTH, currentY + 50f), 6f, 6f, totalBoxPaint)

        drawArabicText(
            canvas,
            "المبلغ المطلوب سداده:",
            MARGIN_X + CONTENT_WIDTH - 210f,
            currentY + 8f,
            200f,
            textSize = 10f,
            isBold = false,
            color = COLOR_TEXT_MUTED
        )
        drawArabicText(
            canvas,
            String.format(Locale.US, "%.2f ₪", s.totalAmount),
            MARGIN_X + CONTENT_WIDTH - 210f,
            currentY + 24f,
            200f,
            textSize = 16f,
            isBold = true,
            color = COLOR_PRIMARY
        )

        // Footer note
        currentY += 90f
        drawArabicText(
            canvas,
            "شكراً لتعاملكم معنا! نتمنى لكم يوماً سعيداً.",
            MARGIN_X,
            currentY,
            CONTENT_WIDTH,
            textSize = 11f,
            isBold = true,
            color = COLOR_PRIMARY,
            alignment = Layout.Alignment.ALIGN_CENTER
        )

        document.finishPage(page)

        val outputFile = File(getReportsDir(context), "فاتورة_بيع_${s.id}.pdf")
        FileOutputStream(outputFile).use { out -> document.writeTo(out) }
        document.close()
        return outputFile
    }

    // 3. Products & Inventory Report PDF
    fun generateInventoryReportPdf(context: Context, products: List<Product>): File {
        val document = PdfDocument()
        val nowStr = SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale("ar")).format(Date())

        var pageNum = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
        var canvas = page.canvas

        var currentY = drawHeader(canvas, "كشف جرد المنتجات والمخزون", "قائمة الأصناف وتقييم البضاعة", nowStr)

        val totalCost = products.sumOf { it.totalCostValue }
        val totalRetail = products.sumOf { it.totalRetailValue }
        val expectedProfit = totalRetail - totalCost
        val lowStockCount = products.count { it.isLowStock }

        // Summary Card
        val bgPaint = Paint().apply { isAntiAlias = true; color = COLOR_PRIMARY_LIGHT }
        canvas.drawRoundRect(RectF(MARGIN_X, currentY, MARGIN_X + CONTENT_WIDTH, currentY + 46f), 6f, 6f, bgPaint)

        val summaryText = "إجمالي الأصناف: ${products.size}  |  قيمة التكلفة: ${String.format(Locale.US, "%.1f", totalCost)} ₪  |  قيمة البيع: ${String.format(Locale.US, "%.1f", totalRetail)} ₪  |  الأرباح المتوقعة: ${String.format(Locale.US, "%.1f", expectedProfit)} ₪  |  النواقص: $lowStockCount"
        drawArabicText(canvas, summaryText, MARGIN_X + 10f, currentY + 14f, CONTENT_WIDTH - 20f, textSize = 9.5f, isBold = true, color = COLOR_PRIMARY)

        currentY += 58f

        // Table Columns (RTL): [الرقم, اسم الصنف, التصنيف, الكمية, سعر الشراء, سعر البيع, إجمالي التكلفة, إجمالي البيع]
        val colWidths = floatArrayOf(35f, 130f, 75f, 55f, 60f, 60f, 60f, 60f)
        val headers = arrayOf("م", "اسم المنتج", "التصنيف", "الكمية", "سعر الشراء", "سعر البيع", "إجمالي التكلفة", "إجمالي البيع")

        fun drawTableHeader(c: Canvas, y: Float) {
            val hPaint = Paint().apply { isAntiAlias = true; color = COLOR_PRIMARY }
            c.drawRect(MARGIN_X, y, MARGIN_X + CONTENT_WIDTH, y + 24f, hPaint)
            var curX = MARGIN_X + CONTENT_WIDTH
            for (i in headers.indices) {
                curX -= colWidths[i]
                drawArabicText(c, headers[i], curX + 4f, y + 6f, colWidths[i] - 8f, textSize = 8.5f, isBold = true, color = Color.WHITE)
            }
        }

        drawTableHeader(canvas, currentY)
        currentY += 24f

        val rowPaint = Paint().apply { isAntiAlias = true }
        val linePaint = Paint().apply { color = COLOR_BORDER; strokeWidth = 0.5f }

        for ((index, prod) in products.withIndex()) {
            if (currentY > PAGE_HEIGHT - 60f) {
                document.finishPage(page)
                pageNum++
                page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
                canvas = page.canvas
                currentY = MARGIN_Y
                drawTableHeader(canvas, currentY)
                currentY += 24f
            }

            rowPaint.color = if (index % 2 == 0) COLOR_ROW_WHITE else COLOR_ROW_ALT
            canvas.drawRect(MARGIN_X, currentY, MARGIN_X + CONTENT_WIDTH, currentY + 26f, rowPaint)
            canvas.drawLine(MARGIN_X, currentY + 26f, MARGIN_X + CONTENT_WIDTH, currentY + 26f, linePaint)

            val qtyText = if (prod.isOutOfStock) "نفد (0)" else if (prod.isLowStock) "${prod.currentStock} (منخفض)" else "${prod.currentStock}"

            val values = arrayOf(
                "${index + 1}",
                prod.name,
                prod.category,
                qtyText,
                String.format(Locale.US, "%.1f ₪", prod.costPrice),
                String.format(Locale.US, "%.1f ₪", prod.sellPrice),
                String.format(Locale.US, "%.1f ₪", prod.totalCostValue),
                String.format(Locale.US, "%.1f ₪", prod.totalRetailValue)
            )

            var curX = MARGIN_X + CONTENT_WIDTH
            for (i in values.indices) {
                curX -= colWidths[i]
                val color = if (i == 3 && prod.isLowStock) COLOR_ACCENT_RED else if (i == 7) COLOR_PRIMARY else COLOR_TEXT_DARK
                drawArabicText(canvas, values[i], curX + 4f, currentY + 5f, colWidths[i] - 8f, textSize = 8.5f, isBold = (i == 1 || i == 7), color = color)
            }

            currentY += 26f
        }

        document.finishPage(page)

        val outputFile = File(getReportsDir(context), "كشف_مخزون_البسطة_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outputFile).use { out -> document.writeTo(out) }
        document.close()
        return outputFile
    }

    // 4. Customer Debts Report PDF
    fun generateCustomerDebtsPdf(context: Context, customersWithDebt: List<CustomerWithDebtSummary>): File {
        val document = PdfDocument()
        val nowStr = SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale("ar")).format(Date())

        var pageNum = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
        var canvas = page.canvas

        var currentY = drawHeader(canvas, "كشف ديون الزبائن والذمم المدينة", "سجل الذمم والمبالغ المتبقية بذمة العملاء", nowStr)

        val totalDebts = customersWithDebt.sumOf { maxOf(0.0, it.remainingBalance) }
        val totalPurchases = customersWithDebt.sumOf { it.totalPurchasesOnCredit }
        val totalPaid = customersWithDebt.sumOf { it.totalPaid }

        val bgPaint = Paint().apply { isAntiAlias = true; color = COLOR_PRIMARY_LIGHT }
        canvas.drawRoundRect(RectF(MARGIN_X, currentY, MARGIN_X + CONTENT_WIDTH, currentY + 46f), 6f, 6f, bgPaint)

        val summaryText = "إجمالي الديون المستحقة بالخارج: ${String.format(Locale.US, "%.2f", totalDebts)} ₪  |  المسدد: ${String.format(Locale.US, "%.2f", totalPaid)} ₪  |  إجمالي المشتريات: ${String.format(Locale.US, "%.2f", totalPurchases)} ₪"
        drawArabicText(canvas, summaryText, MARGIN_X + 10f, currentY + 14f, CONTENT_WIDTH - 20f, textSize = 10f, isBold = true, color = COLOR_ACCENT_PURPLE)

        currentY += 58f

        // Table Columns (RTL): [م, اسم الزبون, رقم الهاتف, الملاحظات, إجمالي المشتريات, المبالغ المسددة, الرصيد المتبقي]
        val colWidths = floatArrayOf(35f, 110f, 80f, 110f, 65f, 65f, 70f)
        val headers = arrayOf("م", "اسم الزبون", "رقم الهاتف", "ملاحظات", "المشتريات", "المسدد", "الرصيد المتبقي")

        fun drawTableHeader(c: Canvas, y: Float) {
            val hPaint = Paint().apply { isAntiAlias = true; color = COLOR_ACCENT_PURPLE }
            c.drawRect(MARGIN_X, y, MARGIN_X + CONTENT_WIDTH, y + 24f, hPaint)
            var curX = MARGIN_X + CONTENT_WIDTH
            for (i in headers.indices) {
                curX -= colWidths[i]
                drawArabicText(c, headers[i], curX + 4f, y + 6f, colWidths[i] - 8f, textSize = 8.5f, isBold = true, color = Color.WHITE)
            }
        }

        drawTableHeader(canvas, currentY)
        currentY += 24f

        val rowPaint = Paint().apply { isAntiAlias = true }
        val linePaint = Paint().apply { color = COLOR_BORDER; strokeWidth = 0.5f }

        for ((index, item) in customersWithDebt.withIndex()) {
            if (currentY > PAGE_HEIGHT - 60f) {
                document.finishPage(page)
                pageNum++
                page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
                canvas = page.canvas
                currentY = MARGIN_Y
                drawTableHeader(canvas, currentY)
                currentY += 24f
            }

            rowPaint.color = if (index % 2 == 0) COLOR_ROW_WHITE else COLOR_ROW_ALT
            canvas.drawRect(MARGIN_X, currentY, MARGIN_X + CONTENT_WIDTH, currentY + 28f, rowPaint)
            canvas.drawLine(MARGIN_X, currentY + 28f, MARGIN_X + CONTENT_WIDTH, currentY + 28f, linePaint)

            val c = item.customer
            val values = arrayOf(
                "${index + 1}",
                c.name,
                c.phone.ifEmpty { "-" },
                c.notes.ifEmpty { "-" },
                String.format(Locale.US, "%.1f ₪", item.totalPurchasesOnCredit),
                String.format(Locale.US, "%.1f ₪", item.totalPaid),
                String.format(Locale.US, "%.1f ₪", item.remainingBalance)
            )

            var curX = MARGIN_X + CONTENT_WIDTH
            for (i in values.indices) {
                curX -= colWidths[i]
                val color = if (i == 6 && item.remainingBalance > 0.01) COLOR_ACCENT_PURPLE else if (i == 6) COLOR_ACCENT_GREEN else COLOR_TEXT_DARK
                drawArabicText(canvas, values[i], curX + 4f, currentY + 6f, colWidths[i] - 8f, textSize = 8.5f, isBold = (i == 1 || i == 6), color = color)
            }

            currentY += 28f
        }

        document.finishPage(page)

        val outputFile = File(getReportsDir(context), "كشف_ديون_الزبائن_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outputFile).use { out -> document.writeTo(out) }
        document.close()
        return outputFile
    }

    // 5. Individual Customer Statement PDF (كشف حساب زبون تفصيلي)
    fun generateCustomerStatementPdf(
        context: Context,
        customerSummary: CustomerWithDebtSummary,
        sales: List<Sale>,
        payments: List<DebtPayment>
    ): File {
        val document = PdfDocument()
        val c = customerSummary.customer
        val page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
        val canvas = page.canvas

        val nowStr = SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale("ar")).format(Date())
        var currentY = drawHeader(canvas, "كشف حساب زبون: ${c.name}", "تفاصيل العمليات والدفعات المسددة", nowStr)

        // Customer Summary Header
        val bgPaint = Paint().apply { isAntiAlias = true; color = COLOR_PRIMARY_LIGHT }
        canvas.drawRoundRect(RectF(MARGIN_X, currentY, MARGIN_X + CONTENT_WIDTH, currentY + 54f), 8f, 8f, bgPaint)

        val text1 = "الاسم: ${c.name}  |  الهاتف: ${c.phone.ifEmpty { "-" }}  |  ملاحظات: ${c.notes.ifEmpty { "-" }}"
        val text2 = "إجمالي المشتريات بالدين: ${String.format(Locale.US, "%.2f ₪", customerSummary.totalPurchasesOnCredit)}   |   المسدد: ${String.format(Locale.US, "%.2f ₪", customerSummary.totalPaid)}   |   الرصيد المتبقي: ${String.format(Locale.US, "%.2f ₪", customerSummary.remainingBalance)}"

        drawArabicText(canvas, text1, MARGIN_X + 12f, currentY + 10f, CONTENT_WIDTH - 24f, textSize = 10f, isBold = false, color = COLOR_TEXT_DARK)
        drawArabicText(canvas, text2, MARGIN_X + 12f, currentY + 30f, CONTENT_WIDTH - 24f, textSize = 11f, isBold = true, color = COLOR_ACCENT_PURPLE)

        currentY += 68f

        // Combined chronological ledger (sales and payments)
        data class LedgerEntry(val timestamp: Long, val type: String, val description: String, val debit: Double, val credit: Double)

        val entries = mutableListOf<LedgerEntry>()
        for (s in sales) {
            entries.add(LedgerEntry(s.saleDate, "شراء بالدين", if (s.notes.isNotBlank()) s.notes else "فاتورة بيع #${s.id}", s.totalAmount, 0.0))
        }
        for (p in payments) {
            val methodLabel = PaymentMethods.getShortName(p.paymentMethod)
            val desc = if (p.notes.isNotBlank()) "${p.notes} [$methodLabel]" else "سداد دفعة [$methodLabel]"
            entries.add(LedgerEntry(p.paymentDate, "سداد ($methodLabel)", desc, 0.0, p.amount))
        }
        entries.sortBy { it.timestamp }

        // Table
        val colWidths = floatArrayOf(85f, 80f, 170f, 65f, 65f, 70f)
        val headers = arrayOf("التاريخ", "نوع الحركة", "البيان / ملاحظات", "مدين (+)", "دائن (-)", "الرصيد")

        val hPaint = Paint().apply { isAntiAlias = true; color = COLOR_PRIMARY }
        canvas.drawRect(MARGIN_X, currentY, MARGIN_X + CONTENT_WIDTH, currentY + 24f, hPaint)
        var curX = MARGIN_X + CONTENT_WIDTH
        for (i in headers.indices) {
            curX -= colWidths[i]
            drawArabicText(canvas, headers[i], curX + 4f, currentY + 6f, colWidths[i] - 8f, textSize = 9f, isBold = true, color = Color.WHITE)
        }
        currentY += 24f

        var runningBalance = 0.0
        val rowPaint = Paint().apply { isAntiAlias = true }
        val linePaint = Paint().apply { color = COLOR_BORDER; strokeWidth = 0.5f }

        for ((idx, entry) in entries.withIndex()) {
            runningBalance += (entry.debit - entry.credit)
            rowPaint.color = if (idx % 2 == 0) COLOR_ROW_WHITE else COLOR_ROW_ALT
            canvas.drawRect(MARGIN_X, currentY, MARGIN_X + CONTENT_WIDTH, currentY + 26f, rowPaint)
            canvas.drawLine(MARGIN_X, currentY + 26f, MARGIN_X + CONTENT_WIDTH, currentY + 26f, linePaint)

            val dateStr = SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH).format(Date(entry.timestamp))
            val debitStr = if (entry.debit > 0) String.format(Locale.US, "%.1f ₪", entry.debit) else "-"
            val creditStr = if (entry.credit > 0) String.format(Locale.US, "%.1f ₪", entry.credit) else "-"
            val balStr = String.format(Locale.US, "%.1f ₪", runningBalance)

            val values = arrayOf(dateStr, entry.type, entry.description, debitStr, creditStr, balStr)

            curX = MARGIN_X + CONTENT_WIDTH
            for (i in values.indices) {
                curX -= colWidths[i]
                val color = if (i == 3 && entry.debit > 0) COLOR_ACCENT_RED else if (i == 4 && entry.credit > 0) COLOR_ACCENT_GREEN else COLOR_TEXT_DARK
                drawArabicText(canvas, values[i], curX + 4f, currentY + 5f, colWidths[i] - 8f, textSize = 8.5f, isBold = (i == 5), color = color)
            }
            currentY += 26f
        }

        document.finishPage(page)

        val outputFile = File(getReportsDir(context), "كشف_حساب_${c.name}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outputFile).use { out -> document.writeTo(out) }
        document.close()
        return outputFile
    }

    // 6. Expenses Report PDF
    fun generateExpensesReportPdf(context: Context, expenses: List<Expense>, periodTitle: String): File {
        val document = PdfDocument()
        val nowStr = SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale("ar")).format(Date())

        var pageNum = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
        var canvas = page.canvas

        var currentY = drawHeader(canvas, "سجل المصاريف والنفقات", "الفترة: $periodTitle", nowStr)

        val totalExpenses = expenses.sumOf { it.amount }

        val bgPaint = Paint().apply { isAntiAlias = true; color = COLOR_PRIMARY_LIGHT }
        canvas.drawRoundRect(RectF(MARGIN_X, currentY, MARGIN_X + CONTENT_WIDTH, currentY + 44f), 6f, 6f, bgPaint)
        drawArabicText(
            canvas,
            "إجمالي المصاريف المسجلة: ${String.format(Locale.US, "%.2f ₪", totalExpenses)}   |   عدد السجلات: ${expenses.size}",
            MARGIN_X + 12f,
            currentY + 14f,
            CONTENT_WIDTH - 24f,
            textSize = 11f,
            isBold = true,
            color = COLOR_PRIMARY
        )

        currentY += 56f

        val colWidths = floatArrayOf(40f, 100f, 120f, 145f, 65f, 65f)
        val headers = arrayOf("#", "التاريخ والوقت", "التصنيف", "بيان المصروف", "الملاحظات", "المبلغ")

        fun drawTableHeader(c: Canvas, y: Float) {
            val hPaint = Paint().apply { isAntiAlias = true; color = Color.rgb(180, 83, 9) } // Amber color
            c.drawRect(MARGIN_X, y, MARGIN_X + CONTENT_WIDTH, y + 24f, hPaint)
            var curX = MARGIN_X + CONTENT_WIDTH
            for (i in headers.indices) {
                curX -= colWidths[i]
                drawArabicText(c, headers[i], curX + 4f, y + 6f, colWidths[i] - 8f, textSize = 9f, isBold = true, color = Color.WHITE)
            }
        }

        drawTableHeader(canvas, currentY)
        currentY += 24f

        val rowPaint = Paint().apply { isAntiAlias = true }
        val linePaint = Paint().apply { color = COLOR_BORDER; strokeWidth = 0.5f }

        for ((index, exp) in expenses.withIndex()) {
            if (currentY > PAGE_HEIGHT - 60f) {
                document.finishPage(page)
                pageNum++
                page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
                canvas = page.canvas
                currentY = MARGIN_Y
                drawTableHeader(canvas, currentY)
                currentY += 24f
            }

            rowPaint.color = if (index % 2 == 0) COLOR_ROW_WHITE else COLOR_ROW_ALT
            canvas.drawRect(MARGIN_X, currentY, MARGIN_X + CONTENT_WIDTH, currentY + 26f, rowPaint)
            canvas.drawLine(MARGIN_X, currentY + 26f, MARGIN_X + CONTENT_WIDTH, currentY + 26f, linePaint)

            val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.ENGLISH).format(Date(exp.expenseDate))
            val values = arrayOf(
                "${index + 1}",
                dateStr,
                exp.category,
                exp.title,
                exp.notes.ifEmpty { "-" },
                String.format(Locale.US, "%.2f ₪", exp.amount)
            )

            var curX = MARGIN_X + CONTENT_WIDTH
            for (i in values.indices) {
                curX -= colWidths[i]
                val color = if (i == 5) COLOR_ACCENT_RED else COLOR_TEXT_DARK
                drawArabicText(canvas, values[i], curX + 4f, currentY + 5f, colWidths[i] - 8f, textSize = 8.5f, isBold = (i == 5), color = color)
            }
            currentY += 26f
        }

        document.finishPage(page)

        val outputFile = File(getReportsDir(context), "سجل_مصاريف_البسطة_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outputFile).use { out -> document.writeTo(out) }
        document.close()
        return outputFile
    }

    // 7. Comprehensive Financial & Profit Statement PDF
    fun generateFinancialReportPdf(
        context: Context,
        stats: PeriodReportStats,
        periodTitle: String,
        totalInventoryCost: Double,
        totalDebts: Double,
        cashInTill: Double
    ): File {
        val document = PdfDocument()
        val page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
        val canvas = page.canvas

        val nowStr = SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale("ar")).format(Date())
        var currentY = drawHeader(canvas, "التقرير المالي وقائمة الأرباح", "الفترة: $periodTitle", nowStr)

        // Net Profit Box
        val isProfit = stats.netProfit >= 0
        val boxPaint = Paint().apply { isAntiAlias = true; color = if (isProfit) Color.rgb(220, 252, 231) else Color.rgb(254, 226, 226) }
        canvas.drawRoundRect(RectF(MARGIN_X, currentY, MARGIN_X + CONTENT_WIDTH, currentY + 70f), 8f, 8f, boxPaint)

        drawArabicText(
            canvas,
            "صافي الأرباح المحققة خلال ($periodTitle):",
            MARGIN_X + 16f,
            currentY + 12f,
            CONTENT_WIDTH - 32f,
            textSize = 12f,
            isBold = false,
            color = COLOR_TEXT_MUTED
        )
        drawArabicText(
            canvas,
            String.format(Locale.US, "%.2f ₪", stats.netProfit),
            MARGIN_X + 16f,
            currentY + 34f,
            CONTENT_WIDTH - 32f,
            textSize = 22f,
            isBold = true,
            color = if (isProfit) COLOR_ACCENT_GREEN else COLOR_ACCENT_RED
        )

        currentY += 86f

        // Accounting breakdown table
        val colWidths = floatArrayOf(300f, 235f)
        val headers = arrayOf("البند المحاسبي", "القيمة بالشيكل (₪)")

        val hPaint = Paint().apply { isAntiAlias = true; color = COLOR_PRIMARY }
        canvas.drawRect(MARGIN_X, currentY, MARGIN_X + CONTENT_WIDTH, currentY + 26f, hPaint)
        drawArabicText(canvas, headers[0], MARGIN_X + CONTENT_WIDTH - colWidths[0] + 8f, currentY + 6f, colWidths[0] - 16f, textSize = 10f, isBold = true, color = Color.WHITE)
        drawArabicText(canvas, headers[1], MARGIN_X + 8f, currentY + 6f, colWidths[1] - 16f, textSize = 10f, isBold = true, color = Color.WHITE)
        currentY += 26f

        val rows = listOf(
            Pair("إجمالي المبيعات (الإيرادات)", String.format(Locale.US, "%.2f ₪", stats.totalRevenue)),
            Pair("  • مبيعات نقدية (كاش)", String.format(Locale.US, "%.2f ₪", stats.cashRevenue)),
            Pair("  • مبيعات إلكترونية (بال بي / جوال بي / بنك فلسطين)", String.format(Locale.US, "%.2f ₪", stats.electronicRevenue)),
            Pair("  • مبيعات بالدين (آجلة)", String.format(Locale.US, "%.2f ₪", stats.creditRevenue)),
            Pair("تكلفة البضاعة المباعة (COGS)", String.format(Locale.US, "%.2f ₪", stats.costOfGoodsSold)),
            Pair("مجمل الربح من المبيعات (المبيعات - التكلفة)", String.format(Locale.US, "%.2f ₪", stats.grossProfit)),
            Pair("إجمالي المصاريف والنفقات التشغيلية", String.format(Locale.US, "%.2f ₪", stats.totalExpenses)),
            Pair("صافي الربح / الخسارة النهائي", String.format(Locale.US, "%.2f ₪", stats.netProfit)),
            Pair("النقد المتوفر حالياً بالصندوق", String.format(Locale.US, "%.2f ₪", cashInTill)),
            Pair("قيمة بضاعة المخزون الحالية (بسعر التكلفة)", String.format(Locale.US, "%.2f ₪", totalInventoryCost)),
            Pair("إجمالي الديون المستحقة بالخارج (ذمم الزبائن)", String.format(Locale.US, "%.2f ₪", totalDebts))
        )

        val rowPaint = Paint().apply { isAntiAlias = true }
        val linePaint = Paint().apply { color = COLOR_BORDER; strokeWidth = 0.5f }

        for ((idx, row) in rows.withIndex()) {
            val isHighlight = idx == 0 || idx == 4 || idx == 6
            rowPaint.color = if (isHighlight) COLOR_PRIMARY_LIGHT else if (idx % 2 == 0) COLOR_ROW_WHITE else COLOR_ROW_ALT
            canvas.drawRect(MARGIN_X, currentY, MARGIN_X + CONTENT_WIDTH, currentY + 28f, rowPaint)
            canvas.drawLine(MARGIN_X, currentY + 28f, MARGIN_X + CONTENT_WIDTH, currentY + 28f, linePaint)

            val valColor = if (idx == 6) (if (isProfit) COLOR_ACCENT_GREEN else COLOR_ACCENT_RED) else COLOR_TEXT_DARK
            drawArabicText(canvas, row.first, MARGIN_X + CONTENT_WIDTH - colWidths[0] + 8f, currentY + 6f, colWidths[0] - 16f, textSize = 10f, isBold = isHighlight, color = COLOR_TEXT_DARK)
            drawArabicText(canvas, row.second, MARGIN_X + 8f, currentY + 6f, colWidths[1] - 16f, textSize = 10.5f, isBold = isHighlight, color = valColor)

            currentY += 28f
        }

        document.finishPage(page)

        val outputFile = File(getReportsDir(context), "التقرير_المالي_وقائمة_الأرباح_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outputFile).use { out -> document.writeTo(out) }
        document.close()
        return outputFile
    }
}
