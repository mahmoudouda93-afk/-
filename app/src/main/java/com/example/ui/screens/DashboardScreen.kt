package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.model.GroupEntity
import com.example.ui.MainViewModel
import com.example.ui.components.CameraBarcodeScannerModal
import com.example.ui.components.MonthlyAttendanceRechartsCard
import com.example.ui.components.MonthlyPaymentsRechartsCard
import com.example.ui.components.PaymentRemindersDialog
import com.example.ui.components.ScannerMode
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val students by viewModel.students.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val attendance by viewModel.attendance.collectAsState()
    val payments by viewModel.payments.collectAsState()

    var showScannerDialog by remember { mutableStateOf(false) }
    var showRemindersDialog by remember { mutableStateOf(false) }

    val todayDate = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    val totalStudents = students.size
    val totalGroups = groups.size
    val todayAttendanceCount = attendance.count { it.date == todayDate && (it.status == "present" || it.status == "late") }
    val totalRevenue = payments.sumOf { it.amount }

    // Identify Students lagging behind on payments (Financial Arrears)
    val arrearsStudents = remember(students, groups, payments) {
        students.filter { st ->
            val group = groups.find { it.id == st.groupId }
            if (group == null) false
            else {
                val paid = payments.filter { it.studentId == st.id }.sumOf { it.amount }
                val discount = if (st.discountType == "percentage") {
                    group.price * (st.discountValue / 100.0)
                } else st.discountValue
                val due = (group.price - discount).coerceAtLeast(0.0)
                paid < due
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sync Status Banner
        item {
            com.example.ui.components.SyncStatusBanner(viewModel = viewModel)
        }

        // Teacher Welcome Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(BluePrimary)
                            .border(2.dp, GoldAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📚",
                            fontSize = 24.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "مستر محمود عوده",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "أستاذ التاريخ — المنظومة الذكية لإدارة المجموعات والطلاب",
                            style = MaterialTheme.typography.bodySmall,
                            color = BlueLight
                        )
                    }

                    // Connection status badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0D5A42))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "🟢 يعمل محلياً",
                            color = GreenLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Metrics Grid (5 Cards)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCard(
                        title = "إجمالي الطلاب",
                        value = "$totalStudents",
                        icon = Icons.Default.School,
                        color = BlueLight,
                        bgColor = NavySurface,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("students") }
                    )
                    MetricCard(
                        title = "إجمالي المجموعات",
                        value = "$totalGroups",
                        icon = Icons.Default.Groups,
                        color = GoldAccent,
                        bgColor = NavySurface,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("groups") }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCard(
                        title = "حاضر اليوم",
                        value = "$todayAttendanceCount",
                        icon = Icons.Default.CheckCircle,
                        color = GreenLight,
                        bgColor = NavySurface,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("attendance") }
                    )
                    MetricCard(
                        title = "إجمالي الإيرادات",
                        value = "${totalRevenue.toInt()} ج",
                        icon = Icons.Default.Payments,
                        color = Color(0xFFFFD54F),
                        bgColor = NavySurface,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("payments") }
                    )
                }
            }
        }

        // Financial Arrears Widget (قائمة المتعثرين مالياً)
        if (arrearsStudents.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E0E12)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(RedDanger))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = RedDanger, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "المتعثرون مالياً (${arrearsStudents.size})",
                                    color = Color(0xFFFF8A80),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            // 1-Click WhatsApp reminder
                            Button(
                                onClick = {
                                    val names = arrearsStudents.joinToString("\n") { "• ${it.name} (${it.phone})" }
                                    val msg = "تذكير بسداد اشتراك مادة التاريخ:\n$names\nمع تحيات مستر محمود عوده"
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse("https://api.whatsapp.com/send?text=" + Uri.encode(msg))
                                    }
                                    context.startActivity(Intent.createChooser(intent, "إرسال تذكير عبر"))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("تذكير جماعي", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        arrearsStudents.take(4).forEach { st ->
                            val group = groups.find { it.id == st.groupId }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${st.name} — ${group?.name ?: "مجموعة"}",
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                                if (st.phone.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            val msg = "مرحباً ولي أمر الطالب ${st.name}، نود تذكيركم بسداد اشتراك مادة التاريخ لدى مستر محمود عوده. شكراً لكم."
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                data = Uri.parse("https://api.whatsapp.com/send?phone=2${st.phone}&text=" + Uri.encode(msg))
                                            }
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Text("💬", fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ─── Monthly Attendance Recharts BarChart ─────────────────
        item {
            MonthlyAttendanceRechartsCard(attendance = attendance)
        }

        // ─── Monthly Payments Recharts Collection Rate Chart ───────
        item {
            MonthlyPaymentsRechartsCard(payments = payments)
        }

        // Quick Actions Grid (6 Actions)
        item {
            Text(
                text = "الإجراءات السريعة",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = BlueLight
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionCard(
                        title = "تسجيل حضور",
                        icon = Icons.Default.CheckCircle,
                        color = GreenSuccess,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("attendance") }
                    )
                    QuickActionCard(
                        title = "تسجيل دفع",
                        icon = Icons.Default.Paid,
                        color = BluePrimary,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("payments") }
                    )
                    QuickActionCard(
                        title = "إضافة طالب",
                        icon = Icons.Default.PersonAdd,
                        color = PurpleAccent,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("students") }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionCard(
                        title = "مجموعة جديدة",
                        icon = Icons.Default.GroupAdd,
                        color = TealAccent,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("groups") }
                    )
                    QuickActionCard(
                        title = "الشهادات",
                        icon = Icons.Default.MilitaryTech,
                        color = GoldAccent,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("certificates") }
                    )
                    QuickActionCard(
                        title = "إغلاق الدرج",
                        icon = Icons.Default.AccountBalanceWallet,
                        color = OrangeWarning,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate("cash_drawer") }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionCard(
                        title = "مسح باركود 📷",
                        icon = Icons.Default.QrCodeScanner,
                        color = BlueLight,
                        modifier = Modifier.weight(1f),
                        onClick = { showScannerDialog = true }
                    )
                    QuickActionCard(
                        title = "تنبيهات المدفوعات 🔔",
                        icon = Icons.Default.NotificationsActive,
                        color = GoldAccent,
                        modifier = Modifier.weight(1f),
                        onClick = { showRemindersDialog = true }
                    )
                }
            }
        }

        // Today's Groups Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مجموعات الحصص (${groups.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = BlueLight
                )
                TextButton(onClick = { onNavigate("groups") }) {
                    Text("عرض الكل", color = BluePrimary, fontSize = 12.sp)
                }
            }
        }

        if (groups.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📚", fontSize = 42.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("لا توجد مجموعات بعد", color = Color.Gray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { onNavigate("groups") },
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                        ) {
                            Text("إضافة مجموعة الآن")
                        }
                    }
                }
            }
        } else {
            items(groups) { group ->
                GroupDashboardCard(
                    group = group,
                    studentsCount = students.count { it.groupId == group.id },
                    onClick = { onNavigate("group_detail/${group.id}") }
                )
            }
        }
    }

    // CameraX Barcode Scanner
    if (showScannerDialog) {
        CameraBarcodeScannerModal(
            mode = ScannerMode.ATTENDANCE,
            students = students,
            groups = groups,
            onDismiss = { showScannerDialog = false },
            onStudentScanned = { st, _ ->
                val grpId = st.groupId
                if (grpId.isNotBlank()) {
                    viewModel.markAttendance(st.id, grpId, todayDate, "present")
                }
                showScannerDialog = false
            }
        )
    }

    // Payment Reminders Dialog
    if (showRemindersDialog) {
        PaymentRemindersDialog(
            students = students,
            groups = groups,
            payments = payments,
            onDismiss = { showRemindersDialog = false },
            onQuickPay = { _ ->
                showRemindersDialog = false
                onNavigate("payments")
            }
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyBorder))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = value, fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.White)
                Text(text = title, color = BlueLight, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyBorder))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(26.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
fun GroupDashboardCard(
    group: GroupEntity,
    studentsCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyBorder))
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
                    text = group.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "${group.grade} • ${group.location.ifBlank { "سنتر التفوق" }}",
                    color = BlueLight,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NavyCard)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "👥 $studentsCount / ${group.capacity} طالب",
                            color = BlueLight,
                            fontSize = 10.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NavyCard)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "💰 ${group.price.toInt()} ج.م",
                            color = GoldAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Icon(
                Icons.Default.ChevronLeft,
                contentDescription = null,
                tint = BlueLight
            )
        }
    }
}
