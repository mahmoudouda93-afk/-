package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.model.*
import com.example.ui.MainViewModel
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.components.CameraBarcodeScannerModal
import com.example.ui.components.ScannerMode
import com.example.ui.components.ThermalReceiptDialog
import com.example.ui.theme.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

val GAMIFICATION_BADGES = listOf(
    Triple("week_star", "طالب الأسبوع ⭐", 10),
    Triple("fastest", "أسرع إجابة ⚡", 5),
    Triple("active", "الأكثر تفاعلاً 🔥", 7),
    Triple("perfect", "إجابة مثالية 💯", 8),
    Triple("helper", "مساعد الزملاء 🤝", 6)
)

val GROUP_TABS = listOf(
    "attendance" to "الحضور ✅",
    "payments" to "المدفوعات 💵",
    "exams" to "الامتحانات 📝",
    "students" to "الطلاب 🎓",
    "gamification" to "تفاعل الحصة ⚡",
    "tasks" to "المهام 📋",
    "sessions" to "سجل الحصص 📚"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: String,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToStudent: (String) -> Unit
) {
    val context = LocalContext.current
    val groups by viewModel.groups.collectAsState()
    val students by viewModel.students.collectAsState()
    val attendance by viewModel.attendance.collectAsState()
    val payments by viewModel.payments.collectAsState()
    val exams by viewModel.exams.collectAsState()
    val scores by viewModel.examScores.collectAsState()

    val group = groups.find { it.id == groupId }
    if (group == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = onNavigateBack) { Text("المجموعة غير موجودة — العودة") }
        }
        return
    }

    val groupStudents = remember(students, groupId) { students.filter { it.groupId == groupId } }
    val groupPayments = remember(payments, groupId) { payments.filter { it.groupId == groupId } }
    val groupExams = remember(exams, groupId) { exams.filter { it.groupId == groupId } }

    var activeTab by remember { mutableStateOf("attendance") }
    var selectedDate by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    }

    // Dialogs
    var showExtraSessionDialog by remember { mutableStateOf(false) }
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var showAddPaymentDialog by remember { mutableStateOf(false) }
    var showAddExamDialog by remember { mutableStateOf(false) }
    var showExcuseDialogForStudent by remember { mutableStateOf<StudentEntity?>(null) }
    var thermalReceiptPayment by remember { mutableStateOf<PaymentEntity?>(null) }
    var inClassQuizExam by remember { mutableStateOf<ExamEntity?>(null) }

    // Parse extra sessions
    val extraSessions = remember(group.extraSessionsJson) {
        try {
            val arr = JSONArray(group.extraSessionsJson)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                Triple(obj.optString("date"), obj.optString("time"), obj.optString("title"))
            }
        } catch (e: Exception) { emptyList() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(group.name, fontWeight = FontWeight.Black, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    // WhatsApp group button
                    if (group.whatsappLink.isNotBlank()) {
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(group.whatsappLink))
                            try { context.startActivity(intent) } catch (e: Exception) { }
                        }) {
                            Text("💬", fontSize = 18.sp)
                        }
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
        ) {
            // Group Top Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyBorder))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${group.stage} • ${group.grade}",
                                color = BlueLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "📍 ${group.location.ifBlank { "السنتر" }}",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${group.price.toInt()} ج.م",
                                color = GoldAccent,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { showExtraSessionDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF332005)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("+ حصة إضافية", color = GoldAccent, fontSize = 11.sp)
                            }
                        }
                    }

                    if (extraSessions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            extraSessions.take(2).forEach { (d, t, ttl) ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF332005))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("✨ إضافية: $ttl ($d $t)", color = GoldAccent, fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Horizontal Tab Bar
            ScrollableTabRow(
                selectedTabIndex = GROUP_TABS.indexOfFirst { it.first == activeTab }.coerceAtLeast(0),
                containerColor = NavySurface,
                contentColor = BlueLight,
                edgePadding = 8.dp
            ) {
                GROUP_TABS.forEach { (key, label) ->
                    Tab(
                        selected = activeTab == key,
                        onClick = { activeTab = key },
                        text = {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (activeTab == key) FontWeight.Bold else FontWeight.Normal,
                                color = if (activeTab == key) Color.White else BlueLight
                            )
                        }
                    )
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (activeTab) {
                    "attendance" -> {
                        AttendanceTabContent(
                            group = group,
                            students = groupStudents,
                            attendance = attendance,
                            selectedDate = selectedDate,
                            onDateChange = { selectedDate = it },
                            onMarkAttendance = { stId, status ->
                                if (status == "excused") {
                                    showExcuseDialogForStudent = groupStudents.find { it.id == stId }
                                } else {
                                    viewModel.markAttendance(stId, group.id, selectedDate, status)
                                }
                            },
                            onScanBarcode = { showBarcodeScanner = true }
                        )
                    }

                    "payments" -> {
                        PaymentsTabContent(
                            group = group,
                            students = groupStudents,
                            payments = groupPayments,
                            onAddPaymentClick = { showAddPaymentDialog = true },
                            onReceiptClick = { thermalReceiptPayment = it }
                        )
                    }

                    "exams" -> {
                        ExamsTabContent(
                            group = group,
                            students = groupStudents,
                            exams = groupExams,
                            scores = scores,
                            onAddExamClick = { showAddExamDialog = true },
                            onStartQuiz = { inClassQuizExam = it },
                            onSetScore = { exId, stId, sc -> viewModel.setExamScore(exId, stId, sc) },
                            onDeleteExam = { viewModel.deleteExam(it) }
                        )
                    }

                    "students" -> {
                        GroupStudentsTabContent(
                            students = groupStudents,
                            onStudentClick = onNavigateToStudent
                        )
                    }

                    "gamification" -> {
                        GamificationTabContent(
                            students = groupStudents,
                            onAward = { stId, pts, badgeId, badgeTitle ->
                                viewModel.awardPointsAndBadge(stId, pts, badgeId, badgeTitle)
                            }
                        )
                    }

                    "tasks" -> {
                        GroupTasksTabContent(groupId = group.id, viewModel = viewModel)
                    }

                    "sessions" -> {
                        SessionNotesTabContent(groupId = group.id, viewModel = viewModel)
                    }
                }
            }
        }
    }

    // CameraX Barcode Scanner Dialog
    if (showBarcodeScanner) {
        CameraBarcodeScannerModal(
            mode = ScannerMode.ATTENDANCE,
            students = groupStudents,
            groups = groups,
            onDismiss = { showBarcodeScanner = false },
            onStudentScanned = { found, _ ->
                viewModel.markAttendance(found.id, group.id, selectedDate, "present")
                showBarcodeScanner = false
            }
        )
    }

    // Excuse Reason Dialog
    showExcuseDialogForStudent?.let { st ->
        ExcuseReasonDialog(
            student = st,
            onDismiss = { showExcuseDialogForStudent = null },
            onSubmit = { reason, makeupDate ->
                viewModel.markAttendance(
                    studentId = st.id,
                    groupId = group.id,
                    date = selectedDate,
                    status = "excused",
                    excuseReason = reason,
                    makeupDate = makeupDate
                )
                showExcuseDialogForStudent = null
            }
        )
    }

    // Add Payment Dialog
    if (showAddPaymentDialog) {
        AddGroupPaymentDialog(
            group = group,
            students = groupStudents,
            onDismiss = { showAddPaymentDialog = false },
            onSave = { stId, amount, type, desc, targetMonth ->
                viewModel.addPayment(
                    studentId = stId,
                    groupId = group.id,
                    amount = amount,
                    type = type,
                    otherDescription = desc,
                    targetMonth = targetMonth
                ) { paymentEntity ->
                    showAddPaymentDialog = false
                    thermalReceiptPayment = paymentEntity
                }
            }
        )
    }

    // Thermal Receipt Dialog
    thermalReceiptPayment?.let { pay ->
        val st = students.find { it.id == pay.studentId }
        ThermalReceiptDialog(
            payment = pay,
            studentName = st?.name ?: "طالب",
            groupName = group.name,
            onDismiss = { thermalReceiptPayment = null }
        )
    }

    // Add Exam Dialog
    if (showAddExamDialog) {
        AddExamDialog(
            groupId = group.id,
            onDismiss = { showAddExamDialog = false },
            onSave = { exam ->
                viewModel.addExam(exam)
                showAddExamDialog = false
            }
        )
    }

    // In-Class Quiz Live Mode Dialog
    inClassQuizExam?.let { exam ->
        InClassQuizDialog(
            exam = exam,
            students = groupStudents,
            scores = scores.filter { it.examId == exam.id },
            onSetScore = { stId, sc -> viewModel.setExamScore(exam.id, stId, sc) },
            onDismiss = { inClassQuizExam = null }
        )
    }

    // Extra Session Dialog
    if (showExtraSessionDialog) {
        AddExtraSessionDialog(
            onDismiss = { showExtraSessionDialog = false },
            onSave = { date, time, title ->
                val arr = try { JSONArray(group.extraSessionsJson) } catch (e: Exception) { JSONArray() }
                arr.put(JSONObject().apply {
                    put("date", date)
                    put("time", time)
                    put("title", title)
                })
                viewModel.updateGroup(group.copy(extraSessionsJson = arr.toString()))
                showExtraSessionDialog = false
            }
        )
    }
}

// ─── TAB 1: Attendance Content ─────────────────────────────────
@Composable
fun AttendanceTabContent(
    group: GroupEntity,
    students: List<StudentEntity>,
    attendance: List<AttendanceEntity>,
    selectedDate: String,
    onDateChange: (String) -> Unit,
    onMarkAttendance: (studentId: String, status: String) -> Unit,
    onScanBarcode: () -> Unit
) {
    val dateAttendance = attendance.filter { it.groupId == group.id && it.date == selectedDate }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onScanBarcode,
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("📷 مسح كارت الباركود", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                text = "التاريخ: $selectedDate",
                color = BlueLight,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Attendance stats chips
        val presentTotal = dateAttendance.count { it.status == "present" }
        val lateTotal = dateAttendance.count { it.status == "late" }
        val absentTotal = dateAttendance.count { it.status == "absent" }
        val excusedTotal = dateAttendance.count { it.status == "excused" }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ChipCount("حاضر: $presentTotal", GreenLight, Modifier.weight(1f))
            ChipCount("متأخر: $lateTotal", GoldAccent, Modifier.weight(1f))
            ChipCount("غائب: $absentTotal", RedDanger, Modifier.weight(1f))
            ChipCount("اعتذر: $excusedTotal", BlueLight, Modifier.weight(1f))
        }

        if (students.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا يوجد طلاب في هذه المجموعة", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(students, key = { it.id }) { student ->
                    val record = dateAttendance.find { it.studentId == student.id }
                    val currentStatus = record?.status

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = NavySurface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = student.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = student.code,
                                    color = BlueLight,
                                    fontSize = 10.sp
                                )
                            }

                            // 4 Attendance Status Buttons
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                StatusIconButton("✅", currentStatus == "present", GreenSuccess) {
                                    onMarkAttendance(student.id, "present")
                                }
                                StatusIconButton("⏰", currentStatus == "late", OrangeWarning) {
                                    onMarkAttendance(student.id, "late")
                                }
                                StatusIconButton("❌", currentStatus == "absent", RedDanger) {
                                    onMarkAttendance(student.id, "absent")
                                }
                                StatusIconButton("💙", currentStatus == "excused", PurpleAccent) {
                                    onMarkAttendance(student.id, "excused")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChipCount(label: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(NavySurface)
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StatusIconButton(
    emoji: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) activeColor else NavyCard)
            .border(
                if (isSelected) 1.5.dp else 0.dp,
                if (isSelected) Color.White else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, fontSize = 14.sp)
    }
}

// ─── TAB 2: Payments Content ───────────────────────────────────
@Composable
fun PaymentsTabContent(
    group: GroupEntity,
    students: List<StudentEntity>,
    payments: List<PaymentEntity>,
    onAddPaymentClick: () -> Unit,
    onReceiptClick: (PaymentEntity) -> Unit
) {
    val totalRevenue = payments.sumOf { it.amount }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onAddPaymentClick,
                colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("تسجيل دفع", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                text = "الإجمالي: ${totalRevenue.toInt()} ج.م",
                color = GoldAccent,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            )
        }

        if (payments.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا توجد مدفوعات مسجلة لهذه المجموعة", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(payments) { payment ->
                    val student = students.find { it.id == payment.studentId }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onReceiptClick(payment) },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = NavySurface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = student?.name ?: "طالب",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "شهر ${payment.targetMonth} • ${payment.date} • ${payment.receiptNumber}",
                                    color = BlueLight,
                                    fontSize = 10.sp
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${payment.amount.toInt()} ج.م",
                                    color = GreenLight,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("🧾", fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── TAB 3: Exams & Live Quiz Content ──────────────────────────
@Composable
fun ExamsTabContent(
    group: GroupEntity,
    students: List<StudentEntity>,
    exams: List<ExamEntity>,
    scores: List<ExamScoreEntity>,
    onAddExamClick: () -> Unit,
    onStartQuiz: (ExamEntity) -> Unit,
    onSetScore: (examId: String, studentId: String, score: Double) -> Unit,
    onDeleteExam: (ExamEntity) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onAddExamClick,
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("إضافة امتحان", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Text("الامتحانات المسجلة (${exams.size})", color = BlueLight, fontSize = 12.sp)
        }

        if (exams.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا توجد امتحانات حتى الآن", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(exams) { exam ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = NavySurface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = exam.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "الدرجة النهائية: ${exam.maxScore.toInt()} | التاريخ: ${exam.date}",
                                        color = BlueLight,
                                        fontSize = 11.sp
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // Start In-Class Quiz Live Mode
                                    Button(
                                        onClick = { onStartQuiz(exam) },
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldDark),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("⚡ كويز مباشر", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    IconButton(
                                        onClick = { onDeleteExam(exam) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = RedDanger, modifier = Modifier.size(16.dp))
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

// ─── TAB 4: Group Students ─────────────────────────────────────
@Composable
fun GroupStudentsTabContent(
    students: List<StudentEntity>,
    onStudentClick: (String) -> Unit
) {
    if (students.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لا يوجد طلاب مسجلين في هذه المجموعة", color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(students) { student ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onStudentClick(student.id) },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = NavySurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(BluePrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(student.name.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(student.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(student.code, color = BlueLight, fontSize = 10.sp)
                            }
                        }

                        Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = BlueLight)
                    }
                }
            }
        }
    }
}

// ─── TAB 5: Gamification Content ───────────────────────────────
@Composable
fun GamificationTabContent(
    students: List<StudentEntity>,
    onAward: (studentId: String, points: Int, badgeId: String, badgeTitle: String) -> Unit
) {
    var selectedStudentId by remember { mutableStateOf(students.firstOrNull()?.id ?: "") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "⚡ نظام التفاعل والشارات والتحفيز",
            color = GoldAccent,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )

        // Select student
        Text("اختر طالباً لمنحه نقاط وشارة:", color = BlueLight, fontSize = 12.sp)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(students) { st ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedStudentId == st.id) NavyCard else NavySurface)
                        .clickable { selectedStudentId = st.id }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = st.name, color = Color.White, fontSize = 12.sp)
                    Text(text = "⭐ ${st.points} نقطة", color = GoldAccent, fontSize = 11.sp)
                }
            }
        }

        Text("اختر الشارة للتكريم الفوري:", color = BlueLight, fontSize = 12.sp)

        GAMIFICATION_BADGES.forEach { (id, title, pts) ->
            Button(
                onClick = {
                    if (selectedStudentId.isNotBlank()) {
                        onAward(selectedStudentId, pts, id, title)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(GoldAccent)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = title, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(text = "+$pts نقطة", color = GreenLight, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

// ─── TAB 6: Tasks Content ──────────────────────────────────────
@Composable
fun GroupTasksTabContent(groupId: String, viewModel: MainViewModel) {
    val tasksFlow = remember(groupId) { viewModel.repository.getTasksByGroup(groupId) }
    val tasks by tasksFlow.collectAsState(initial = emptyList())

    var newTaskTitle by remember { mutableStateOf("") }
    var taskType by remember { mutableStateOf("teacher") }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newTaskTitle,
                onValueChange = { newTaskTitle = it },
                placeholder = { Text("أضف مهمة جديدة...") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Button(
                onClick = {
                    if (newTaskTitle.isNotBlank()) {
                        viewModel.repository.let {
                            // launch
                        }
                    }
                }
            ) {
                Text("إضافة")
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(tasks) { task ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = NavySurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(task.title, color = Color.White)
                        IconButton(onClick = { /* delete */ }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = RedDanger, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

// ─── TAB 7: Session Notes Archive ──────────────────────────────
@Composable
fun SessionNotesTabContent(groupId: String, viewModel: MainViewModel) {
    val notesFlow = remember(groupId) { viewModel.repository.getSessionNotesByGroup(groupId) }
    val notes by notesFlow.collectAsState(initial = emptyList())

    var showAddNoteDialog by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("سجل ومذكرات الحصص", color = BlueLight, fontWeight = FontWeight.Bold)
            Button(
                onClick = { showAddNoteDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
            ) {
                Text("+ تسجيل درس")
            }
        }

        if (notes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا توجد مذكرات حصص مسجلة", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notes) { note ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = NavySurface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(note.title, color = Color.White, fontWeight = FontWeight.Bold)
                                Text(note.date, color = BlueLight, fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(note.notes, color = Color(0xFFDDDDDD), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// ─── In-Class Quiz Live Mode Dialog ───────────────────────────
@Composable
fun InClassQuizDialog(
    exam: ExamEntity,
    students: List<StudentEntity>,
    scores: List<ExamScoreEntity>,
    onSetScore: (studentId: String, score: Double) -> Unit,
    onDismiss: () -> Unit
) {
    // Leaderboard sorted by score descending
    val leaderboard = remember(students, scores) {
        students.map { st ->
            val sc = scores.find { it.studentId == st.id }?.score ?: 0.0
            st to sc
        }.sortedByDescending { it.second }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NavySurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚡ كويز مباشر — ${exam.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = GoldAccent
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                    }
                }

                Text(
                    text = "الدرجة العظمى: ${exam.maxScore.toInt()} | رصد فوري للدرجات وترتيب الأوائل",
                    color = BlueLight,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Live Leaderboard Top 3 Banner
                if (leaderboard.isNotEmpty() && leaderboard.first().second > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(NavyCard)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        leaderboard.take(3).forEachIndexed { idx, (st, sc) ->
                            val medal = when (idx) {
                                0 -> "🥇"
                                1 -> "🥈"
                                else -> "🥉"
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(medal, fontSize = 20.sp)
                                Text(st.name.take(8), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text("${sc.toInt()} درجة", color = GoldAccent, fontSize = 10.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Grading list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(leaderboard) { (st, currentScore) ->
                        var scoreInput by remember { mutableStateOf(if (currentScore > 0) currentScore.toInt().toString() else "") }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = NavyCard)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(st.name, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))

                                OutlinedTextField(
                                    value = scoreInput,
                                    onValueChange = {
                                        scoreInput = it
                                        val num = it.toDoubleOrNull()
                                        if (num != null) {
                                            onSetScore(st.id, num.coerceIn(0.0, exam.maxScore))
                                        }
                                    },
                                    placeholder = { Text("0") },
                                    modifier = Modifier.width(70.dp),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إنهاء الكويز وحفظ النتائج")
                }
            }
        }
    }
}

// ─── Excuse Dialog ─────────────────────────────────────────────
@Composable
fun ExcuseReasonDialog(
    student: StudentEntity,
    onDismiss: () -> Unit,
    onSubmit: (reason: String, makeupDate: String?) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var needsMakeup by remember { mutableStateOf(false) }
    var makeupDate by remember { mutableStateOf("") }

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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "تسجيل اعتذار — ${student.name}",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("سبب الاعتذار (عذر مرضي / ظروف...)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = needsMakeup,
                        onCheckedChange = { needsMakeup = it },
                        colors = CheckboxDefaults.colors(checkedColor = BluePrimary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تحديد موعد حصة تعويضية؟", color = Color.White, fontSize = 12.sp)
                }

                if (needsMakeup) {
                    OutlinedTextField(
                        value = makeupDate,
                        onValueChange = { makeupDate = it },
                        label = { Text("تاريخ وموعد التعويض (مثال: السبت 18:00)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onSubmit(reason, if (needsMakeup) makeupDate else null) },
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("تسجيل الاعتذار")
                    }
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("إلغاء")
                    }
                }
            }
        }
    }
}

// ─── Add Payment Dialog ────────────────────────────────────────
@Composable
fun AddGroupPaymentDialog(
    group: GroupEntity,
    students: List<StudentEntity>,
    initialStudentId: String? = null,
    onDismiss: () -> Unit,
    onSave: (studentId: String, amount: Double, type: String, otherDesc: String?, targetMonth: Int) -> Unit
) {
    var selectedStudentId by remember {
        mutableStateOf(initialStudentId ?: students.firstOrNull()?.id ?: "")
    }
    var amountText by remember { mutableStateOf(group.price.toInt().toString()) }
    var selectedType by remember { mutableStateOf("monthly") }
    var otherDesc by remember { mutableStateOf("") }
    var targetMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH) + 1) }

    val paymentTypes = listOf(
        "monthly" to "اشتراك شهر",
        "half_monthly" to "اشتراك نصف شهر",
        "explanation_notes" to "مذكرة شرح",
        "review_notes" to "مذكرة مراجعة",
        "other" to "أخرى"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NavySurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "💵 تسجيل دفعة جديدة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text("اختر الطالب:", color = BlueLight, fontSize = 11.sp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    students.forEach { st ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selectedStudentId == st.id) NavyCard else Color.Transparent)
                                .clickable { selectedStudentId = st.id }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedStudentId == st.id, onClick = { selectedStudentId = st.id })
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(st.name, color = Color.White, fontSize = 12.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("المبلغ (ج.م) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Target Month Selector (1-12)
                Text("الشهر المستهدف:", color = BlueLight, fontSize = 11.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    (1..6).forEach { m ->
                        FilterChip(
                            selected = targetMonth == m,
                            onClick = { targetMonth = m },
                            label = { Text("شهر $m", fontSize = 10.sp) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    (7..12).forEach { m ->
                        FilterChip(
                            selected = targetMonth == m,
                            onClick = { targetMonth = m },
                            label = { Text("شهر $m", fontSize = 10.sp) }
                        )
                    }
                }

                // Payment Type
                Text("نوع الدفع:", color = BlueLight, fontSize = 11.sp)
                paymentTypes.forEach { (type, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedType = type },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedType == type, onClick = { selectedType = type })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(label, color = Color.White, fontSize = 12.sp)
                    }
                }

                if (selectedType == "other") {
                    OutlinedTextField(
                        value = otherDesc,
                        onValueChange = { otherDesc = it },
                        label = { Text("توضيح الغرض...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (selectedStudentId.isNotBlank() && amt > 0) {
                                onSave(selectedStudentId, amt, selectedType, if (selectedType == "other") otherDesc else null, targetMonth)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("حفظ وإصدار إيصال", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("إلغاء")
                    }
                }
            }
        }
    }
}

// ─── Add Exam Dialog ───────────────────────────────────────────
@Composable
fun AddExamDialog(
    groupId: String,
    onDismiss: () -> Unit,
    onSave: (ExamEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var maxScoreText by remember { mutableStateOf("100") }
    var date by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    }

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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("إضافة امتحان جديد", fontWeight = FontWeight.Bold, color = Color.White)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الامتحان (مثال: امتحان الفصل الأول)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = maxScoreText,
                    onValueChange = { maxScoreText = it },
                    label = { Text("الدرجة العظمى (النهائية)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("تاريخ الامتحان") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(
                                    ExamEntity(
                                        groupId = groupId,
                                        name = name,
                                        maxScore = maxScoreText.toDoubleOrNull() ?: 100.0,
                                        date = date
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("حفظ")
                    }

                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("إلغاء")
                    }
                }
            }
        }
    }
}

// ─── Add Extra Session Dialog ──────────────────────────────────
@Composable
fun AddExtraSessionDialog(
    onDismiss: () -> Unit,
    onSave: (date: String, time: String, title: String) -> Unit
) {
    var title by remember { mutableStateOf("مراجعة ليلة الامتحان") }
    var date by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    }
    var time by remember { mutableStateOf("17:00") }

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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("إضافة موعد حصة إضافية", fontWeight = FontWeight.Bold, color = GoldAccent)

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان الحصة الإضافية") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("التاريخ (yyyy-MM-dd)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("الوقت (مثال: 17:00)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (title.isNotBlank()) onSave(date, time, title)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldDark),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إضافة الحصة")
                    }
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("إلغاء")
                    }
                }
            }
        }
    }
}
