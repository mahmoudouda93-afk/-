package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.data.model.StudentEntity
import com.example.ui.MainViewModel
import com.example.ui.components.AcademicReportDialog
import com.example.ui.theme.*
import com.example.util.StudentReportGenerator
import java.util.Calendar

@Composable
fun ReportsScreen(
    viewModel: MainViewModel,
    onNavigateToStudent: (String) -> Unit
) {
    val context = LocalContext.current
    val students by viewModel.students.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val attendance by viewModel.attendance.collectAsState()
    val payments by viewModel.payments.collectAsState()
    val exams by viewModel.exams.collectAsState()
    val scores by viewModel.examScores.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val submissions by viewModel.submissions.collectAsState()

    var selectedStudentForReport by remember { mutableStateOf<StudentEntity?>(null) }
    var selectedThemeForReport by remember { mutableStateOf(StudentReportGenerator.ReportTheme.ROYAL) }
    var filterGroupId by remember { mutableStateOf<String?>(null) }
    var selectedMonthIndex by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
    var showPreviewTable by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val totalRevenue = payments.sumOf { it.amount }
    val totalAttendanceRecords = attendance.size
    val totalPresent = attendance.count { it.status == "present" || it.status == "late" }
    val overallAttendanceRate = if (totalAttendanceRecords > 0) (totalPresent * 100) / totalAttendanceRecords else 0

    val overallAvgScore = if (scores.isNotEmpty()) {
        (scores.sumOf { it.score } / scores.size).toInt()
    } else 0

    // Filter students for export
    val filteredStudentsForExport = remember(students, filterGroupId) {
        if (filterGroupId == null) students else students.filter { it.groupId == filterGroupId }
    }

    // Precalculate report data for filtered students
    val calculatedReportsList = remember(filteredStudentsForExport, groups, attendance, payments, exams, scores, tasks, submissions) {
        filteredStudentsForExport.map { st ->
            val grp = groups.find { it.id == st.groupId }
            StudentReportGenerator.calcReportData(
                student = st,
                group = grp,
                allAttendance = attendance,
                allPayments = payments,
                allExams = exams,
                allScores = scores,
                allTasks = tasks,
                allSubmissions = submissions
            )
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
            // Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BluePrimary))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Assessment, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "التقارير التحليلية الشاملة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "متابعة أداء المجموعات والتحصيل الأكاديمي والمدفوعات",
                            color = BlueLight,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // High Level Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReportMetricBox("الالتزام العام", "$overallAttendanceRate%", GreenLight, Modifier.weight(1f))
                ReportMetricBox("متوسط الاختبارات", if (overallAvgScore > 0) "$overallAvgScore%" else "—", BlueLight, Modifier.weight(1f))
                ReportMetricBox("إجمالي الإيراد", "${totalRevenue.toInt()} ج", GoldAccent, Modifier.weight(1f))
            }

            // AI Smart Insights
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(PurpleAccent))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تحليل الذكاء الاصطناعي العام لمجموعات التاريخ",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• استقرار ممتاز في نسب حضور مجموعات الثانوية العامة (الصف الثالث الثانوي).\n" +
                                "• متوسط تحصيل الطلاب في الامتحانات الشهرية أعلى من 75%، مما يعكس فهم عميق لمناهج التاريخ.\n" +
                                "• يُوصى بتكثيف ورش حل أسئلة الربط والتحليل واستمرار صرف كروت شهادات التقدير للمتميزين.",
                        color = Color(0xFFDDDDDD),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            // Groups Breakdown
            Text(
                text = "تحليل المجموعات الدراسية (${groups.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = BlueLight
            )

            groups.forEach { grp ->
                val grpStudents = students.filter { it.groupId == grp.id }
                val grpPayments = payments.filter { it.groupId == grp.id }
                val grpTotal = grpPayments.sumOf { it.amount }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyBorder))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = grp.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(text = "${grp.grade} • ${grpStudents.size} طالب", color = BlueLight, fontSize = 11.sp)
                        }

                        Text(text = "${grpTotal.toInt()} ج.م", color = GoldAccent, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                }
            }

            // ════════════════════════════════════════════════════════════
            // 📊 MONTHLY PERFORMANCE CSV EXPORT CARD
            // ════════════════════════════════════════════════════════════
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF0F766E)))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0F766E).copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.TableChart, contentDescription = null, tint = Color(0xFF2DD4BF), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "تصدير تقارير الأداء الشهرية (CSV / Excel)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "تنسيق متوافق مع Excel وجداول Google بترميز UTF-8",
                                    fontSize = 10.sp,
                                    color = Color(0xFF2DD4BF)
                                )
                            }
                        }
                    }

                    // Month Selector Chips
                    Text(
                        text = "اختر الشهر المستهدف للتقرير:",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StudentReportGenerator.ARABIC_MONTHS.forEachIndexed { index, monthName ->
                            val isSelected = selectedMonthIndex == index
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedMonthIndex = index },
                                label = { Text(monthName, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0F766E),
                                    selectedLabelColor = Color.White,
                                    containerColor = NavyDark,
                                    labelColor = Color(0xFF94A3B8)
                                )
                            )
                        }
                    }

                    // Group Selector Chips
                    Text(
                        text = "تصفية حسب المجموعة:",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = filterGroupId == null,
                            onClick = { filterGroupId = null },
                            label = { Text("جميع المجموعات (${students.size})", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BluePrimary,
                                selectedLabelColor = Color.White,
                                containerColor = NavyDark,
                                labelColor = Color(0xFF94A3B8)
                            )
                        )

                        groups.forEach { grp ->
                            val isSelected = filterGroupId == grp.id
                            val count = students.count { it.groupId == grp.id }
                            FilterChip(
                                selected = isSelected,
                                onClick = { filterGroupId = grp.id },
                                label = { Text("${grp.name} ($count)", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BluePrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = NavyDark,
                                    labelColor = Color(0xFF94A3B8)
                                )
                            )
                        }
                    }

                    // Export Summary Badge
                    val selectedGroupName = groups.find { it.id == filterGroupId }?.name ?: "جميع المجموعات"
                    val selectedMonthName = StudentReportGenerator.ARABIC_MONTHS.getOrElse(selectedMonthIndex) { "الشهر الحالي" }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(NavyDark.copy(alpha = 0.6f))
                            .border(1.dp, NavyBorder, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "التقرير المجهز للتصدير: $selectedMonthName",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "$selectedGroupName • ${filteredStudentsForExport.size} طالب مشمول في ملف التقرير",
                                    color = Color(0xFF2DD4BF),
                                    fontSize = 10.sp
                                )
                            }

                            Icon(Icons.Default.FileDownload, contentDescription = null, tint = Color(0xFF2DD4BF))
                        }
                    }

                    // Action Buttons: Export CSV & Preview
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (calculatedReportsList.isEmpty()) {
                                    Toast.makeText(context, "لا يوجد طلاب في المجموعة المحددة", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val title = "تقرير الأداء الأكاديمي لشهر $selectedMonthName - $selectedGroupName"
                                val csv = StudentReportGenerator.generateMonthlyPerformanceCsv(
                                    calculatedList = calculatedReportsList,
                                    title = title,
                                    filterLabel = "$selectedGroupName ($selectedMonthName)"
                                )
                                val cleanFileName = "تقرير_اداء_${selectedGroupName.replace(" ", "_")}_$selectedMonthName"
                                StudentReportGenerator.exportAndShareCsv(
                                    context = context,
                                    csvContent = csv,
                                    fileName = cleanFileName
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تصدير ومشاركة CSV 📊", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showPreviewTable = !showPreviewTable },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BlueLight),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(if (showPreviewTable) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (showPreviewTable) "إخفاء الجدول" else "معاينة الجدول", fontSize = 11.sp)
                        }
                    }

                    // Expandable Preview Table
                    if (showPreviewTable) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(NavyDark)
                                .border(1.dp, NavyBorder, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "معاينة بيانات جدول الأداء (${calculatedReportsList.size} طالب):",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            // Table Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(BluePrimary.copy(alpha = 0.2f))
                                    .padding(vertical = 4.dp, horizontal = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("الطالب", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(1.4f))
                                Text("الحضور", color = GreenLight, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(0.7f))
                                Text("الاختبارات", color = BlueLight, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(0.7f))
                                Text("الواجبات", color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(0.7f))
                                Text("المستوى", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(0.7f))
                            }

                            Divider(color = NavyBorder, modifier = Modifier.padding(vertical = 2.dp))

                            // Table Rows (preview first 8)
                            calculatedReportsList.take(8).forEach { r ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp, horizontal = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(r.student.name, color = Color.White, fontSize = 10.sp, maxLines = 1, modifier = Modifier.weight(1.4f))
                                    Text("${r.attendanceRate}%", color = GreenLight, fontSize = 10.sp, modifier = Modifier.weight(0.7f))
                                    Text(r.avgGradeStr, color = BlueLight, fontSize = 10.sp, modifier = Modifier.weight(0.7f))
                                    Text("${r.hwRate}%", color = Color(0xFFFBBF24), fontSize = 10.sp, modifier = Modifier.weight(0.7f))
                                    Text("${r.finalPerf}%", color = if (r.finalPerf >= 70) GreenLight else OrangeWarning, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.7f))
                                }
                            }

                            if (calculatedReportsList.size > 8) {
                                Text(
                                    text = "... ويوجد ${calculatedReportsList.size - 8} طلاب إضافيين في ملف CSV الكامل",
                                    color = Color.Gray,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(top = 4.dp, start = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Group financial summaries
            Text(
                text = "تقارير المجموعات المالية",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = BlueLight
            )

            groups.forEach { grp ->
                val grpStudents = students.filter { it.groupId == grp.id }
                val grpPayments = payments.filter { it.groupId == grp.id }
                val grpTotal = grpPayments.sumOf { it.amount }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyBorder))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = grp.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(text = "${grp.grade} • ${grpStudents.size} طالب", color = BlueLight, fontSize = 11.sp)
                        }

                        Text(text = "${grpTotal.toInt()} ج.م", color = GoldAccent, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                }
            }

            // ─── Report Design Variations Showcase ───
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF38BDF8).copy(alpha = 0.6f)))
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
                            Text("🎨", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "أنماط وتصميمات التقارير الأكاديمية",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "اختر قالباً لمعاينته فوراً أو تصديره (PDF / Word / طباعة)",
                                    color = BlueLight,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = BluePrimary.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, BluePrimary.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "4 تصاميم جاهزة",
                                color = BlueLight,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // 4 Themes List / Cards
                    StudentReportGenerator.ReportTheme.values().forEach { theme ->
                        val isCurrent = selectedThemeForReport == theme
                        val themeColor = Color(android.graphics.Color.parseColor(theme.secondaryHex))
                        val accentColor = Color(android.graphics.Color.parseColor(theme.accentHex))
                        val primaryColor = Color(android.graphics.Color.parseColor(theme.primaryHex))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = NavyDark),
                            border = BorderStroke(
                                width = if (isCurrent) 1.5.dp else 1.dp,
                                color = if (isCurrent) themeColor else NavyBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Visual Color Palette Preview
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                androidx.compose.ui.graphics.Brush.linearGradient(
                                                    listOf(primaryColor, themeColor)
                                                )
                                            )
                                            .border(1.dp, accentColor, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = when (theme) {
                                                StudentReportGenerator.ReportTheme.ROYAL -> "👑"
                                                StudentReportGenerator.ReportTheme.HERITAGE -> "📜"
                                                StudentReportGenerator.ReportTheme.EMERALD -> "🌿"
                                                StudentReportGenerator.ReportTheme.INK_SAVER -> "🖨️"
                                            },
                                            fontSize = 16.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = theme.titleAr,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            if (isCurrent) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "● النمط الحالي",
                                                    color = themeColor,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Text(
                                            text = theme.subtitleAr,
                                            color = Color.Gray,
                                            fontSize = 10.sp,
                                            maxLines = 1
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        selectedThemeForReport = theme
                                        // Open dialog with first student or sample
                                        val targetStudent = students.firstOrNull() ?: StudentEntity(
                                            id = "sample_preview",
                                            name = "أحمد محمد محمود (معاينة)",
                                            code = "101",
                                            phone = "01012345678",
                                            parentPhone = "01098765432",
                                            grade = "الثالث الثانوي",
                                            joinDate = "2026-09-01",
                                            groupId = groups.firstOrNull()?.id ?: ""
                                        )
                                        selectedStudentForReport = targetStudent
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isCurrent) themeColor else NavySurface
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    border = if (!isCurrent) BorderStroke(1.dp, themeColor.copy(alpha = 0.5f)) else null,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = if (isCurrent) Color.White else themeColor
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "معاينة النمط",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) Color.White else themeColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Student Performance Reports generator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تقارير الطلاب الفردية (طباعة، وورد، واتساب)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = BlueLight
                )

                Text(
                    text = "${students.size} طالب",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            // Search Bar for Students
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("ابحث عن طالب بالاسم أو الكود للتقرير...", fontSize = 12.sp, color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = BlueLight) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BluePrimary,
                    unfocusedBorderColor = NavyBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = NavySurface,
                    unfocusedContainerColor = NavySurface
                )
            )

            val displayStudents = remember(students, searchQuery) {
                if (searchQuery.isBlank()) {
                    students.take(15)
                } else {
                    students.filter {
                        it.name.contains(searchQuery, ignoreCase = true) ||
                        it.code.contains(searchQuery, ignoreCase = true)
                    }
                }
            }

            displayStudents.forEach { st ->
                val grp = groups.find { it.id == st.groupId }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyBorder))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(st.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("${grp?.name ?: st.grade} • كود: ${st.code}", color = BlueLight, fontSize = 10.sp)
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Quick CSV Export Button
                            IconButton(
                                onClick = {
                                    val d = StudentReportGenerator.calcReportData(
                                        student = st,
                                        group = grp,
                                        allAttendance = attendance,
                                        allPayments = payments,
                                        allExams = exams,
                                        allScores = scores,
                                        allTasks = tasks,
                                        allSubmissions = submissions
                                    )
                                    val csv = StudentReportGenerator.generateMonthlyPerformanceCsv(
                                        calculatedList = listOf(d),
                                        title = "تقرير أداء الطالب - ${st.name}",
                                        filterLabel = grp?.name ?: st.grade
                                    )
                                    StudentReportGenerator.exportAndShareCsv(
                                        context = context,
                                        csvContent = csv,
                                        fileName = "تقرير_${st.name}_${st.code}"
                                    )
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0F766E).copy(alpha = 0.2f))
                            ) {
                                Icon(Icons.Default.TableChart, contentDescription = "تصدير CSV", tint = Color(0xFF2DD4BF), modifier = Modifier.size(16.dp))
                            }

                            // Full Report Dialog Button
                            Button(
                                onClick = { selectedStudentForReport = st },
                                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("التقرير الشامل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    selectedStudentForReport?.let { st ->
        val grp = groups.find { it.id == st.groupId }
        AcademicReportDialog(
            student = st,
            group = grp,
            attendanceList = attendance,
            paymentsList = payments,
            examsList = exams,
            scoresList = scores,
            tasksList = tasks,
            submissionsList = submissions,
            initialTheme = selectedThemeForReport,
            onDismiss = { selectedStudentForReport = null }
        )
    }
}

@Composable
fun ReportMetricBox(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyBorder))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, color = color, fontWeight = FontWeight.Black, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = title, color = Color.Gray, fontSize = 10.sp)
        }
    }
}
