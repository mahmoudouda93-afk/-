package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.example.data.model.GroupEntity
import com.example.data.model.StudentEntity
import com.example.ui.MainViewModel
import com.example.ui.components.AcademicReportDialog
import com.example.ui.components.CertificateDialog
import com.example.ui.components.StudentIdCard
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentProfileScreen(
    studentId: String,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val students by viewModel.students.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val attendance by viewModel.attendance.collectAsState()
    val payments by viewModel.payments.collectAsState()
    val exams by viewModel.exams.collectAsState()
    val scores by viewModel.examScores.collectAsState()

    val student = students.find { it.id == studentId }
    if (student == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("الطالب غير موجود", color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onNavigateBack) { Text("العودة") }
            }
        }
        return
    }

    val group = groups.find { it.id == student.groupId }
    val studentAttendance = attendance.filter { it.studentId == student.id }
    val presentCount = studentAttendance.count { it.status == "present" || it.status == "late" }
    val absentCount = studentAttendance.count { it.status == "absent" }
    val totalSessions = studentAttendance.size
    val attendanceRate = if (totalSessions > 0) (presentCount * 100) / totalSessions else 0

    val studentPayments = payments.filter { it.studentId == student.id }
    val totalPaid = studentPayments.sumOf { it.amount }

    // Dialog states
    var showEditDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showCertificateDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Smart Subscription Expiry Warning
    val isNearExpiry = presentCount >= (group?.sessionsPerMonth ?: 8) - 1

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(student.name, fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavySurface)
            )
        },
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
            // Student ID Card
            StudentIdCard(student = student, group = group)

            // Smart Subscription Expiry Alert
            if (isNearExpiry) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF332005)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(GoldAccent))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⏰", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "تنبيه: اقتراب انتهاء الاشتراك الشهري",
                                color = GoldAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "حضر الطالب $presentCount من ${group?.sessionsPerMonth ?: 8} حصص لهذا الشهر.",
                                color = Color(0xFFFFECB3),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Monthly Statistics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatBox(title = "الحضور", value = "$presentCount", color = GreenLight, icon = Icons.Default.CheckCircle, modifier = Modifier.weight(1f))
                StatBox(title = "الغيابات", value = "$absentCount", color = RedDanger, icon = Icons.Default.Cancel, modifier = Modifier.weight(1f))
                StatBox(title = "الالتزام", value = "$attendanceRate%", color = PurpleAccent, icon = Icons.Default.Insights, modifier = Modifier.weight(1f))
                StatBox(title = "المدفوعات", value = "${totalPaid.toInt()} ج", color = GoldAccent, icon = Icons.Default.Payments, modifier = Modifier.weight(1f))
            }

            // Action Trio: Edit, Move, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showEditDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BlueLight),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تعديل", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { showMoveDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldAccent),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("نقل", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RedDanger),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("حذف", fontSize = 12.sp)
                }
            }

            // Primary Feature Action Trio
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showReportDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Assessment, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("معاينة وتصدير تقرير الأداء الشامل", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { showCertificateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.MilitaryTech, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إصدار شهادة تقدير (تفوق / التزام / ثبات)", fontWeight = FontWeight.Bold)
                }
            }

            // AI Performance Insight Box
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BluePrimary))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🤖", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "التحليل الذكي لمستوى الطالب (AI Insight)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    val insightText = remember(student, attendanceRate, presentCount, absentCount) {
                        when {
                            attendanceRate >= 85 -> "الطالب يظهر التزاماً استثنائياً بنسبة حضور $attendanceRate% ويشارك بفاعلية في الحصص. يُوصى بتكريمه بشهادة تفوق لدعم استمرارية تميزه في مادة التاريخ."
                            attendanceRate >= 65 -> "مستوى الحضور متوسط ($attendanceRate%). يحتاج الطالب إلى تحفيز إضافي لتقليل الغياب وحل واجبات المراجعة بانتظام."
                            else -> "تنبيه أكاديمي: نسبة الحضور منخفضة ($attendanceRate%) مع تكرار الغياب ($absentCount مرات). يُوصى بالتواصل المباشر مع ولي الأمر لمعالجة التأخر الدراسي."
                        }
                    }

                    Text(
                        text = insightText,
                        color = BlueLight,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            // Quick WhatsApp Action Buttons
            Text(
                text = "رسائل واتساب السريعة مع ولي الأمر",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = BlueLight
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickWhatsAppButton(
                    label = "تذكير اشتراك 💰",
                    color = Color(0xFF0D5A42),
                    modifier = Modifier.weight(1f)
                ) {
                    val phone = student.parentPhone.ifBlank { student.phone }
                    val msg = "مرحباً ولي أمر الطالب ${student.name}، نود تذكيركم بسداد اشتراك مادة التاريخ للمجموعة ${group?.name ?: ""} مع خالص الشكر — مستر محمود عوده"
                    openWhatsApp(context, phone, msg)
                }

                QuickWhatsAppButton(
                    label = "إشعار غياب ❌",
                    color = Color(0xFF5A1D0D),
                    modifier = Modifier.weight(1f)
                ) {
                    val phone = student.parentPhone.ifBlank { student.phone }
                    val msg = "عزيزي ولي أمر الطالب ${student.name}، نود إحاطتكم علماً بغياب ابنكم عن حصة اليوم في مادة التاريخ. يرجى المتابعة لتعويض الحصة."
                    openWhatsApp(context, phone, msg)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickWhatsAppButton(
                    label = "تقرير متابعة 📊",
                    color = Color(0xFF1E3A5F),
                    modifier = Modifier.weight(1f)
                ) {
                    val phone = student.parentPhone.ifBlank { student.phone }
                    val msg = "تقرير متابعة الطالب ${student.name}:\n✅ الحضور: $presentCount حصص\n❌ الغياب: $absentCount حصص\n📊 نسبة الالتزام: $attendanceRate%\nمع تحيات مستر محمود عوده"
                    openWhatsApp(context, phone, msg)
                }

                QuickWhatsAppButton(
                    label = "تهنئة بالتفوق 🏆",
                    color = Color(0xFF4A3805),
                    modifier = Modifier.weight(1f)
                ) {
                    val phone = student.parentPhone.ifBlank { student.phone }
                    val msg = "نهنئ الطالب المتميز ${student.name} على أدائه وتفوقه في مادة التاريخ! نتمنى له دوام النجاح والريادة 🌟 — مستر محمود عوده"
                    openWhatsApp(context, phone, msg)
                }
            }

            // Parent Portal Link Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "بوابة ولي الأمر (رمز الوصول)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "رمز الطالب: ${student.parentToken}",
                            color = GoldAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "يمكن لولي الأمر متابعة درجات وحضور ابنه فورياً",
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }

                    Button(
                        onClick = {
                            val clip = ClipData.newPlainText("رمز ولي الأمر", student.parentToken)
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(clip)
                            Toast.makeText(context, "تم نسخ رمز ولي الأمر", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyCard)
                    ) {
                        Text("نسخ", color = BlueLight, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // Move Group Dialog
    if (showMoveDialog) {
        MoveGroupDialog(
            student = student,
            groups = groups,
            onDismiss = { showMoveDialog = false },
            onMove = { newGroupId ->
                viewModel.updateStudent(student.copy(groupId = newGroupId))
                showMoveDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("حذف الطالب", color = Color.White) },
            text = { Text("هل أنت متأكد من حذف الطالب ${student.name} وسجلاته؟", color = BlueLight) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteStudent(student)
                        showDeleteConfirm = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedDanger)
                ) {
                    Text("حذف نهائي")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) {
                    Text("إلغاء")
                }
            },
            containerColor = NavySurface
        )
    }

    // Academic Report Dialog
    if (showReportDialog) {
        AcademicReportDialog(
            student = student,
            group = group,
            attendanceList = attendance,
            paymentsList = payments,
            examsList = exams,
            scoresList = scores,
            onDismiss = { showReportDialog = false }
        )
    }

    // Certificate Dialog
    if (showCertificateDialog) {
        CertificateDialog(
            student = student,
            group = group,
            onDismiss = { showCertificateDialog = false }
        )
    }
}

@Composable
fun StatBox(
    title: String,
    value: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyBorder))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
            Text(text = title, color = Color.Gray, fontSize = 10.sp)
        }
    }
}

@Composable
fun QuickWhatsAppButton(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
fun MoveGroupDialog(
    student: StudentEntity,
    groups: List<GroupEntity>,
    onDismiss: () -> Unit,
    onMove: (String) -> Unit
) {
    var selectedGroup by remember { mutableStateOf(student.groupId) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NavySurface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "نقل الطالب إلى مجموعة أخرى",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                groups.forEach { grp ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedGroup == grp.id) NavyCard else Color.Transparent)
                            .clickable { selectedGroup = grp.id }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedGroup == grp.id,
                            onClick = { selectedGroup = grp.id }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "${grp.name} (${grp.grade})", color = Color.White, fontSize = 13.sp)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onMove(selectedGroup) },
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("تأكيد النقل")
                    }
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("إلغاء")
                    }
                }
            }
        }
    }
}

fun openWhatsApp(context: Context, phone: String, message: String) {
    val cleanPhone = phone.trim().replace("+", "").replace(" ", "")
    val uri = Uri.parse("https://api.whatsapp.com/send?phone=2$cleanPhone&text=" + Uri.encode(message))
    val intent = Intent(Intent.ACTION_VIEW, uri)
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "تعذر فتح تطبيق واتساب", Toast.LENGTH_SHORT).show()
    }
}
