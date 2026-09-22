package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.backup.BackupParsedData
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val assistants by viewModel.assistants.collectAsState()

    // Sync States
    val isOnline by viewModel.isOnline.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val lastSyncTimestamp by viewModel.lastSyncTimestamp.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()

    var academicYear by remember { mutableStateOf("2026/2027") }
    var showAddAssistantDialog by remember { mutableStateOf(false) }
    var showRawJsonDialog by remember { mutableStateOf(false) }
    var showManualRestoreDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    // Parsed Backup preview dialog
    var previewBackupData by remember { mutableStateOf<BackupParsedData?>(null) }
    var isProcessingFile by remember { mutableStateOf(false) }

    // SAF Launchers for local file backup & restore
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val success = viewModel.saveBackupToUri(uri)
                if (success) {
                    Toast.makeText(context, "تم حفظ النسخة الاحتياطية بنجاح في جهازك 💾", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "فشل حفظ الملف", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isProcessingFile = true
                val jsonString = viewModel.readBackupFromUri(uri)
                if (jsonString.isNotBlank()) {
                    val parsed = viewModel.parseBackup(jsonString)
                    if (parsed.metadata.isValid) {
                        previewBackupData = parsed
                    } else {
                        Toast.makeText(context, "ملف غير صالح: ${parsed.metadata.errorMessage}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "الملف فارغ أو تعذر الوصول إليه", Toast.LENGTH_SHORT).show()
                }
                isProcessingFile = false
            }
        }
    }

    Scaffold(
        containerColor = NavyDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BlueLight))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(BluePrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = BlueLight, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "إعدادات المنظومة والمزامنة السحابية",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "مزامنة Room مع Firestore، النسخ الاحتياطي، وحسابات المساعدين",
                            color = BlueLight,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // ==========================================
            // 1. FIRESTORE & ROOM SYNC SECTION
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (isOnline) GreenSuccess.copy(alpha = 0.6f) else RedDanger.copy(alpha = 0.6f)
                    )
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = if (isOnline) GreenSuccess else GoldAccent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "المزامنة السحابية (Room ⟷ Firestore)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        // Live connection pill
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isOnline) GreenSuccess.copy(alpha = 0.15f) else RedDanger.copy(alpha = 0.15f),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(if (isOnline) GreenSuccess else RedDanger)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isOnline) GreenSuccess else RedDanger)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isOnline) "متصل بالإنترنت" else "وضع غير متصل",
                                    color = if (isOnline) GreenSuccess else RedDanger,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Metrics Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Pending Queue metric
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = NavyCard)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("السجلات المعلقة للمزامنة", color = Color.Gray, fontSize = 10.sp)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "$pendingSyncCount",
                                        color = if (pendingSyncCount > 0) GoldAccent else GreenSuccess,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = if (pendingSyncCount == 0) "متزامن تماماً" else "في الانتظار",
                                        color = if (pendingSyncCount > 0) GoldAccent else GreenSuccess,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        // Last Sync time metric
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = NavyCard)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("آخر مزامنة ناجحة", color = Color.Gray, fontSize = 10.sp)
                                val syncDateStr = lastSyncTimestamp?.let {
                                    SimpleDateFormat("HH:mm - yyyy/MM/dd", Locale.getDefault()).format(Date(it))
                                } ?: "الآن أو قيد التجهيز"
                                Text(
                                    text = syncDateStr,
                                    color = BlueLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Sync Status message box
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(NavyCard)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isOnline) BlueLight else GoldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = syncMessage,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 11.sp
                        )
                    }

                    // Sync Button
                    Button(
                        onClick = { viewModel.syncNow() },
                        enabled = !isSyncing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isOnline) BluePrimary else NavyCard
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جارِ المزامنة مع Firestore...", fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (pendingSyncCount > 0) "مزامنة العمليات المعلقة الآن ($pendingSyncCount)" else "بدء مزامنة فورية مع السحابة",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Offline Guarantee explainer
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NavyDark.copy(alpha = 0.6f),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(BlueLight.copy(alpha = 0.2f))
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text("🛡️", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تقنية Offline-First: يمكنك تسجيل الحضور والمدفوعات بدون اتصال نهائياً؛ حيث تُحفظ البيانات محلياً في Room أولاً، وفور استعادة الاتصال تتولى المنظومة مزامنتها تلقائياً مع Firestore.",
                                color = Color.Gray,
                                fontSize = 10.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 2. BACKUP & RESTORE / GOOGLE DRIVE SECTION
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = GoldAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "النسخ الاحتياطي وحماية البيانات",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "حفظ نسخة شاملة محلياً أو رفعها إلى Google Drive",
                                color = Color.Gray,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Button 1: Share / Upload to Google Drive
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    val backupFile = viewModel.createLocalBackupFile()
                                    viewModel.shareBackup(backupFile)
                                    Toast.makeText(context, "جارِ فتح قائمة المشاركة / Google Drive...", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "خطأ: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F9D58)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "حفظ ورفع النسخة إلى Google Drive ☁️",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    // Row for Local Export & Import
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Local SAF Save
                        Button(
                            onClick = {
                                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                                createDocumentLauncher.launch("AlMusaed_Backup_$timeStamp.json")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تصدير لجهازك (SAF)", fontSize = 11.sp)
                        }

                        // Local/Drive SAF Open
                        Button(
                            onClick = {
                                openDocumentLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NavyCard),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = BlueLight, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("استيراد من ملف (.json)", fontSize = 11.sp, color = BlueLight)
                        }
                    }

                    // Secondary options (Manual text JSON)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showRawJsonDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("عرض/نسخ الكود", fontSize = 10.sp)
                        }

                        OutlinedButton(
                            onClick = { showManualRestoreDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("لصق كود يدوياً", fontSize = 10.sp)
                        }
                    }
                }
            }

            // ==========================================
            // 3. ACADEMIC YEAR SETTING
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("العام الدراسي النشط", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    OutlinedTextField(
                        value = academicYear,
                        onValueChange = { academicYear = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // ==========================================
            // 4. ASSISTANT ACCOUNTS
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("حسابات المساعدين (${assistants.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Button(
                            onClick = { showAddAssistantDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("+ حساب مساعد", fontSize = 11.sp)
                        }
                    }

                    if (assistants.isEmpty()) {
                        Text("لا يوجد حسابات مساعدين مسجلة", color = Color.Gray, fontSize = 11.sp)
                    } else {
                        assistants.forEach { asst ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NavyCard)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(asst.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("كود: ${asst.code} | PIN: ${asst.pin}", color = GoldAccent, fontSize = 11.sp)
                                }

                                IconButton(
                                    onClick = { viewModel.deleteAssistant(asst) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = RedDanger, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 5. DANGER ZONE: RESET
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E0E12)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(RedDanger))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("منطقة الخطر ⚠", color = RedDanger, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("إعادة ضبط المصنع ومسح كافة بيانات الطلاب والمجموعات نهائياً", color = Color(0xFFFF8A80), fontSize = 11.sp)

                    Button(
                        onClick = { showResetConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = RedDanger),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("مسح كافة البيانات وإعادة الضبط", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ==========================================
            // 6. LOGOUT
            // ==========================================
            Button(
                onClick = {
                    viewModel.logout()
                    onLogout()
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyCard),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null, tint = RedDanger)
                Spacer(modifier = Modifier.width(8.dp))
                Text("تسجيل الخروج", color = RedDanger, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("منظومة المساعد لإدارة المراكز التعليمية v2.0", color = Color.Gray, fontSize = 11.sp)
                Text("مستر محمود عوده — أستاذ التاريخ", color = BlueLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // ==========================================
    // DIALOGS
    // ==========================================

    // 1. Restore Confirmation & Preview Dialog
    previewBackupData?.let { backupData ->
        RestorePreviewDialog(
            data = backupData,
            onDismiss = { previewBackupData = null },
            onConfirmRestore = { replaceAll ->
                viewModel.restoreFullBackup(backupData, replaceAll = replaceAll) { success, msg ->
                    if (success) {
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        previewBackupData = null
                    } else {
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    // 2. Raw JSON View / Copy Dialog
    if (showRawJsonDialog) {
        var jsonText by remember { mutableStateOf("") }
        var isExporting by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            jsonText = viewModel.exportFullBackupJson()
            isExporting = false
        }

        Dialog(onDismissRequest = { showRawJsonDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("نسخة احتياطية من كافة البيانات (JSON)", color = Color.White, fontWeight = FontWeight.Bold)
                    if (isExporting) {
                        Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = BlueLight)
                        }
                    } else {
                        OutlinedTextField(
                            value = jsonText,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val clip = ClipData.newPlainText("AlMusaed Backup", jsonText)
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(clip)
                                Toast.makeText(context, "تم نسخ النسخة الاحتياطية للحافظة ✅", Toast.LENGTH_SHORT).show()
                                showRawJsonDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("نسخ للحافظة")
                        }

                        OutlinedButton(
                            onClick = { showRawJsonDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إغلاق")
                        }
                    }
                }
            }
        }
    }

    // 3. Manual Paste Restore Dialog
    if (showManualRestoreDialog) {
        var importJsonText by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showManualRestoreDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("استعادة من كود JSON", color = Color.White, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        placeholder = { Text("الصق بيانات النسخة الاحتياطية هنا...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (importJsonText.isNotBlank()) {
                                    val parsed = viewModel.parseBackup(importJsonText)
                                    if (parsed.metadata.isValid) {
                                        showManualRestoreDialog = false
                                        previewBackupData = parsed
                                    } else {
                                        Toast.makeText(context, "بيانات غير صالحة: ${parsed.metadata.errorMessage}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("فحص ومعاينة")
                        }

                        OutlinedButton(
                            onClick = { showManualRestoreDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء")
                        }
                    }
                }
            }
        }
    }

    // 4. Add Assistant Dialog
    if (showAddAssistantDialog) {
        AddAssistantDialog(
            onDismiss = { showAddAssistantDialog = false },
            onSave = { name, code, pin ->
                viewModel.addAssistant(name, code, pin)
                showAddAssistantDialog = false
            }
        )
    }

    // 5. Reset Confirm Dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("تأكيد مسح كافة البيانات", color = RedDanger) },
            text = { Text("هل أنت متأكد تماماً من رغبتك في حذف جميع الطلاب والمجموعات وسجلات الحضور والمدفوعات نهائياً؟", color = Color.White) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.wipeAllData()
                        showResetConfirmDialog = false
                        Toast.makeText(context, "تمت إعادة الضبط بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedDanger)
                ) {
                    Text("نعم، امسح كل شيء")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetConfirmDialog = false }) {
                    Text("إلغاء")
                }
            },
            containerColor = NavySurface
        )
    }
}

@Composable
fun RestorePreviewDialog(
    data: BackupParsedData,
    onDismiss: () -> Unit,
    onConfirmRestore: (replaceAll: Boolean) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BlueLight))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = GreenSuccess)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "معاينة النسخة الاحتياطية",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }

                Text(
                    text = "تاريخ النسخة: ${data.metadata.exportDate}",
                    color = BlueLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Divider(color = NavyCard)

                // Stats breakdown
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(NavyCard)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("👨‍🎓 عدد الطلاب:", color = Color.White, fontSize = 12.sp)
                        Text("${data.students.size} طالب", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("👥 المجموعات الدراسية:", color = Color.White, fontSize = 12.sp)
                        Text("${data.groups.size} مجموعة", color = Color.White, fontSize = 12.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("📋 سجلات الحضور:", color = Color.White, fontSize = 12.sp)
                        Text("${data.attendance.size} سجل", color = Color.White, fontSize = 12.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("💰 إيصالات المدفوعات:", color = Color.White, fontSize = 12.sp)
                        Text("${data.payments.size} إيصال (${data.metadata.totalAmountPayments} ج.م)", color = GreenSuccess, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("📊 الامتحانات:", color = Color.White, fontSize = 12.sp)
                        Text("${data.exams.size} امتحان", color = Color.White, fontSize = 12.sp)
                    }
                }

                Text(
                    text = "اختر طريقة الاستعادة المناسبة:",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )

                // Merge Button
                Button(
                    onClick = { onConfirmRestore(false) },
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("دمج مع البيانات الحالية (Merge)")
                }

                // Replace All Button
                Button(
                    onClick = { onConfirmRestore(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("استبدال كامل لقاعدة البيانات (Replace All)", color = NavyDark, fontWeight = FontWeight.Black)
                }

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إلغاء")
                }
            }
        }
    }
}

@Composable
fun AddAssistantDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, code: String, pin: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("AST${(100..999).random()}") }
    var pin by remember { mutableStateOf("${(1000..9999).random()}") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NavySurface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("إضافة مساعد معلم جديد", color = Color.White, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم المساعد") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("كود تسجيل الدخول") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = { Text("الرقم السري (PIN 4 أرقام)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (name.isNotBlank() && code.isNotBlank() && pin.isNotBlank()) {
                                onSave(name, code, pin)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إنشاء الحساب")
                    }
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("إلغاء")
                    }
                }
            }
        }
    }
}
