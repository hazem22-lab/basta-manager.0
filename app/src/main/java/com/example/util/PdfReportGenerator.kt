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
            val desc = if (s.notes.isNotBlank()) s.notes else "فاتورة بيع #${s.id}"
            entries.add(LedgerEntry(s.saleDate, "شراء بالدين", desc, s.totalAmount, 0.0))
        }
        for (p in payments) {
            val methodLabel = PaymentMethods.getShortName(p.paymentMethod)
            val desc = if (p.notes.isNotBlank()) "${p.notes} [$methodLabel]" else "سداد دفعة [$methodLabel]"
            entries.add(LedgerEntry(p.paymentDate, "سداد ($methodLabel)", desc, 0.0, p.amount))
        }
        entries.sortBy { it.timestamp }

        // Table
        val colWidths = floatArrayOf(80f, 75f, 175f, 65f, 65f, 63f)
        val headers = arrayOf("التاريخ", "نوع الحركة", "البيان / الأصناف", "مدين (+)", "دائن (-)", "الرصيد")

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
