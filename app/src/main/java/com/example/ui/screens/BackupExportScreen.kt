package com.example.ui.screens

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.BastaViewModel
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.PdfIcon
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.PrimaryGreenLight
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupExportScreen(
    viewModel: BastaViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val autoBackupEnabled by viewModel.autoBackupEnabled.collectAsState()
    val lastAutoBackupTime by viewModel.lastAutoBackupTime.collectAsState()
    val autoBackupFiles by viewModel.autoBackupFiles.collectAsState()

    var showRestoreDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var restoreJsonText by remember { mutableStateOf("") }
    var fileToRestore by remember { mutableStateOf<File?>(null) }
    var fileToDelete by remember { mutableStateOf<File?>(null) }

    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale("ar")) }

    fun shareText(title: String, content: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, content)
            putExtra(Intent.EXTRA_TITLE, title)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, title)
        context.startActivity(shareIntent)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "رجوع")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "التقارير والنسخ الاحتياطي التلقائي",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "تقارير PDF الرسمية، النسخ الاحتياطي التلقائي واستعادة البيانات",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Automatic Backup Section (النسخ الاحتياطي التلقائي)
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("auto_backup_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryGreenLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "النسخ الاحتياطي التلقائي",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (autoBackupEnabled) "مفعّل (حفظ تلقائي عند كل تغيير)" else "معطّل حالياً",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (autoBackupEnabled) PrimaryGreen else MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Switch(
                            checked = autoBackupEnabled,
                            onCheckedChange = { viewModel.setAutoBackupEnabled(it) },
                            modifier = Modifier.testTag("auto_backup_switch")
                        )
                    }

                    Text(
                        text = "يقوم التطبيق بحفظ نسخة احتياطية آمنة تلقائياً في ذاكرة الجهاز عند تسجيل أي عملية بيع أو سداد دين أو مصروف، مما يضمن عدم فقدان أي بيانات نهائياً.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (lastAutoBackupTime > 0) {
                        val lastDateStr = dateFormat.format(Date(lastAutoBackupTime))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PrimaryGreenLight.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "آخر حفظ تلقائي: $lastDateStr",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PrimaryGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.runAutoBackupNow() },
                            modifier = Modifier.weight(1f).testTag("run_auto_backup_now_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("نسخ احتياطي فوري الآن", fontSize = 12.sp)
                        }
                    }

                    // Recent Auto-Backup Files list
                    if (autoBackupFiles.isNotEmpty()) {
                        Divider()
                        Text(
                            text = "نقاط الاستعادة المحفوظة تلقائياً (${autoBackupFiles.size}):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            autoBackupFiles.take(5).forEach { file ->
                                val timeStr = dateFormat.format(Date(file.lastModified()))
                                val sizeKb = maxOf(1L, file.length() / 1024)

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = timeStr, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                            Text(text = "الحجم: $sizeKb كيلوبايت", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            FilledTonalButton(
                                                onClick = { fileToRestore = file },
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(text = "استعادة", fontSize = 11.sp)
                                            }

                                            IconButton(onClick = { viewModel.shareAutoBackupFile(context, file) }) {
                                                Icon(Icons.Default.Share, contentDescription = "مشاركة", modifier = Modifier.size(16.dp))
                                            }

                                            IconButton(onClick = { fileToDelete = file }) {
                                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // PDF Reports Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PdfIcon(modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تقارير PDF الرسمية الجاهزة للمشاركة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "تقارير PDF احترافية بتنسيق محاسبي أنيق مع دعم كامل للغة العربية (RTL) جاهزة للمشاركة عبر واتساب أو الطباعة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Divider()

                    ExportRow(
                        title = "تقرير سجل المبيعات والفواتير PDF",
                        onShare = { viewModel.shareSalesReportPdf(context) }
                    )

                    ExportRow(
                        title = "تقرير قائمة المنتجات وقيمة المخزون PDF",
                        onShare = { viewModel.shareInventoryReportPdf(context) }
                    )

                    ExportRow(
                        title = "كشف ديون الزبائن والذمم المدينة PDF",
                        onShare = { viewModel.shareCustomerDebtsPdf(context) }
                    )

                    ExportRow(
                        title = "سجل المصاريف والنفقات PDF",
                        onShare = { viewModel.shareExpensesReportPdf(context) }
                    )

                    ExportRow(
                        title = "التقرير المالي وقائمة الدخل والأرباح PDF",
                        onShare = { viewModel.shareFinancialReportPdf(context) }
                    )
                }
            }
        }

        // Manual Backup & Restore Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "النسخ الاحتياطي اليدوي والمشاركة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "تصدير نسخة احتياطية كاملة بصيغة JSON لمشاركتها يدوياً أو استيراد ملف سابق.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Divider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    val backupJson = viewModel.getFullJsonBackup()
                                    shareText("نسخة_احتياطية_مدير_البسطة.json", backupJson)
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("create_backup_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تصدير نسخة JSON", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { showRestoreDialog = true },
                            modifier = Modifier.weight(1f).testTag("restore_backup_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("لصق واستعادة", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Danger Zone: Clear Data
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "منطقة الخطر",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "مسح جميع المبيعات والمنتجات والديون والمصاريف والبدء من جديد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Button(
                        onClick = { showClearDataDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("clear_all_data_button")
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مسح كافة البيانات")
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Confirm Restore from Auto Backup File
    fileToRestore?.let { file ->
        val fileTime = dateFormat.format(Date(file.lastModified()))
        AlertDialog(
            onDismissRequest = { fileToRestore = null },
            title = { Text("تأكيد استعادة النسخة الاحتياطية", fontWeight = FontWeight.Bold) },
            text = {
                Text("هل تريد استعادة البيانات من النسخة المحفوظة بتاريخ ($fileTime)؟ سيتم استبدال البيانات الحالية بالكامل.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreFromAutoBackupFile(file)
                        fileToRestore = null
                    }
                ) {
                    Text("تأكيد الاستعادة")
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToRestore = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Confirm Delete Auto Backup File
    fileToDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("حذف ملف النسخة الاحتياطية", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من رغبتك في حذف هذا الملف من الذاكرة؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAutoBackupFile(file)
                        fileToDelete = null
                    }
                ) {
                    Text("حذف", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Manual Paste JSON Restore Dialog
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = {
                Text(text = "استعادة نسخة احتياطية", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "الصق نص النسخة الاحتياطية (JSON) هنا لاستعادة كافة البيانات:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = restoreJsonText,
                        onValueChange = { restoreJsonText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .testTag("restore_json_input"),
                        placeholder = { Text("الصق بيانات النسخة الاحتياطية هنا...") }
                    )
                    Text(
                        text = "تنبيه: الاستعادة ستستبدل البيانات الحالية بالكامل.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restoreJsonText.isNotBlank()) {
                            viewModel.restoreBackup(restoreJsonText) { success ->
                                if (success) {
                                    showRestoreDialog = false
                                    restoreJsonText = ""
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_restore_button")
                ) {
                    Text("استعادة الآن")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Clear Data Confirmation
    if (showClearDataDialog) {
        ConfirmDeleteDialog(
            title = "مسح كافة بيانات التطبيق",
            message = "تحذير شديد: هذا الإجراء سيحذف كافة المنتجات والمبيعات والديون والمصاريف نهائياً ولا يمكن التراجع عنه. هل أنت متأكد تماماً؟",
            onConfirm = {
                viewModel.clearAllData()
                showClearDataDialog = false
            },
            onDismiss = { showClearDataDialog = false }
        )
    }
}

@Composable
fun ExportRow(
    title: String,
    onShare: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        FilledTonalButton(
            onClick = onShare,
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "مشاركة", fontSize = 12.sp)
        }
    }
}
