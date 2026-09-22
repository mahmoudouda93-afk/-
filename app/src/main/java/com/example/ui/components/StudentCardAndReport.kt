package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AttendanceEntity
import com.example.data.model.ExamEntity
import com.example.data.model.ExamScoreEntity
import com.example.data.model.GroupEntity
import com.example.data.model.PaymentEntity
import com.example.data.model.StudentEntity
import com.example.data.model.TaskEntity
import com.example.data.model.SubmissionEntity
import com.example.util.StudentReportGenerator
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Send
import com.example.ui.theme.*

// ─── Student ID Card View ────────────────────────────────────
@Composable
fun StudentIdCard(
    student: StudentEntity,
    group: GroupEntity?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Column (Barcode & Code)
            Column(
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight()
                    .background(Color.White)
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "📚 كارت طالب",
                    color = Color(0xFF1A7A5E),
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp
                )
                Text(
                    text = "2026/2027",
                    color = Color(0xFF888888),
                    fontSize = 9.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Barcode simulation graphic (vertical black bars)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val pattern = listOf(2, 1, 3, 1, 2, 2, 1, 3, 1, 2, 1, 3, 2, 1, 2, 3, 1)
                    pattern.forEachIndexed { i, width ->
                        Box(
                            modifier = Modifier
                                .width(width.dp)
                                .fillMaxHeight()
                                .background(Color(0xFF111111))
                        )
                        Spacer(modifier = Modifier.width(if (i % 2 == 0) 2.dp else 1.dp))
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = student.code,
                    color = Color(0xFF111111),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = "POWERED BY المساعد",
                    color = Color(0xFFAAAAAA),
                    fontSize = 7.sp
                )
            }

            // Right Column (Student & Teacher Details)
            Column(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF1A7A5E), Color(0xFF0D5A42))
                        )
                    )
                    .padding(10.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "مستر محمود عوده — التاريخ",
                    color = Color(0xCCFFFFFF),
                    fontSize = 9.sp
                )
                Text(
                    text = student.name,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    maxLines = 1
                )
                Text(
                    text = group?.name ?: student.grade,
                    color = Color(0xEEFFFFFF),
                    fontSize = 10.sp
                )
                if (student.phone.isNotBlank()) {
                    Text(
                        text = "📞 ${student.phone}",
                        color = Color(0xDDFFFFFF),
                        fontSize = 10.sp
                    )
                }
                Text(
                    text = "📅 ${student.joinDate}",
                    color = Color(0xBBFFFFFF),
                    fontSize = 9.sp
                )

                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(Color(0x44FFFFFF))
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "هذا الكارت خاص بالطالب يرجى عدم إعارته للآخرين",
                    color = Color(0x99FFFFFF),
                    fontSize = 7.sp,
                    lineHeight = 10.sp
                )
            }
        }
    }
}

// ─── Comprehensive Academic Performance Report Modal ────────
@Composable
fun AcademicReportDialog(
    student: StudentEntity,
    group: GroupEntity?,
    attendanceList: List<AttendanceEntity>,
    paymentsList: List<PaymentEntity>,
    examsList: List<ExamEntity>,
    scoresList: List<ExamScoreEntity>,
    tasksList: List<TaskEntity> = emptyList(),
    submissionsList: List<SubmissionEntity> = emptyList(),
    initialTheme: StudentReportGenerator.ReportTheme = StudentReportGenerator.ReportTheme.ROYAL,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTheme by remember { mutableStateOf(initialTheme) }

    val themeGradient = when (selectedTheme) {
        StudentReportGenerator.ReportTheme.ROYAL -> listOf(Color(0xFF1A4FA0), Color(0xFF1A73E8))
        StudentReportGenerator.ReportTheme.HERITAGE -> listOf(Color(0xFF831843), Color(0xFF991B1B))
        StudentReportGenerator.ReportTheme.EMERALD -> listOf(Color(0xFF064E3B), Color(0xFF0D9488))
        StudentReportGenerator.ReportTheme.INK_SAVER -> listOf(Color(0xFF0F172A), Color(0xFF334155))
    }
    val themePrimary = when (selectedTheme) {
        StudentReportGenerator.ReportTheme.ROYAL -> Color(0xFF1A73E8)
        StudentReportGenerator.ReportTheme.HERITAGE -> Color(0xFF991B1B)
        StudentReportGenerator.ReportTheme.EMERALD -> Color(0xFF0D9488)
        StudentReportGenerator.ReportTheme.INK_SAVER -> Color(0xFF334155)
    }
    val themeAccent = when (selectedTheme) {
        StudentReportGenerator.ReportTheme.ROYAL -> Color(0xFFF59E0B)
        StudentReportGenerator.ReportTheme.HERITAGE -> Color(0xFFD97706)
        StudentReportGenerator.ReportTheme.EMERALD -> Color(0xFF10B981)
        StudentReportGenerator.ReportTheme.INK_SAVER -> Color(0xFF475569)
    }

    // Computations using the comprehensive StudentReportGenerator
    val d = StudentReportGenerator.calcReportData(
        student = student,
        group = group,
        allAttendance = attendanceList,
        allPayments = paymentsList,
        allExams = examsList,
        allScores = scoresList,
        allTasks = tasksList,
        allSubmissions = submissionsList
    )

    val finalPerformance = d.finalPerf
    val perfColor = when {
        finalPerformance >= 75 -> GreenSuccess
        finalPerformance >= 60 -> OrangeWarning
        else -> RedDanger
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f)
                .padding(4.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header (Theme Gradient Banner)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(themeGradient))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "المساعد",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "مساعد المعلم الذكي • التاريخ",
                                color = Color(0xCCFFFFFF),
                                fontSize = 10.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "تقرير الأداء الأكاديمي",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "الفصل الدراسي الأول 2026/2027",
                                color = Color(0xCCFFFFFF),
                                fontSize = 10.sp
                            )
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                        }
                    }
                }

                // Interactive Theme Selector Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1F5F9))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎨 تصميم ونمط التقرير:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color(0xFF334155)
                        )
                        Text(
                            text = selectedTheme.titleAr,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            color = themePrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StudentReportGenerator.ReportTheme.values().forEach { themeItem ->
                            val isSelected = selectedTheme == themeItem
                            val chipGradient = when (themeItem) {
                                StudentReportGenerator.ReportTheme.ROYAL -> listOf(Color(0xFF1A4FA0), Color(0xFF1A73E8))
                                StudentReportGenerator.ReportTheme.HERITAGE -> listOf(Color(0xFF831843), Color(0xFF991B1B))
                                StudentReportGenerator.ReportTheme.EMERALD -> listOf(Color(0xFF064E3B), Color(0xFF0D9488))
                                StudentReportGenerator.ReportTheme.INK_SAVER -> listOf(Color(0xFF0F172A), Color(0xFF334155))
                            }
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedTheme = themeItem },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color.White else Color(0xFFE2E8F0),
                                border = if (isSelected) BorderStroke(2.dp, themePrimary) else null,
                                shadowElevation = if (isSelected) 2.dp else 0.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(Brush.linearGradient(chipGradient))
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = when (themeItem) {
                                            StudentReportGenerator.ReportTheme.ROYAL -> "الملكي"
                                            StudentReportGenerator.ReportTheme.HERITAGE -> "التراثي"
                                            StudentReportGenerator.ReportTheme.EMERALD -> "الزمردي"
                                            StudentReportGenerator.ReportTheme.INK_SAVER -> "الموفر"
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                                        color = if (isSelected) themePrimary else Color(0xFF475569),
                                        maxLines = 1,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                // Student Info Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8F9FA))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(themePrimary)
                            .border(2.dp, themeAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = student.name.take(1),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = student.name,
                                color = Color(0xFF111111),
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFDCFCE7))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (student.isActive) "✅ نشط" else "❌ غير نشط",
                                    color = Color(0xFF16A34A),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = "الصف: ${student.grade.ifBlank { group?.grade ?: "—" }} | المجموعة: ${group?.name ?: "—"}",
                            color = Color(0xFF555555),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "كود: ${student.code} | هاتف: ${student.phone.ifBlank { "—" }} | ولي الأمر: ${student.parentPhone.ifBlank { "—" }}",
                            color = Color(0xFF777777),
                            fontSize = 10.sp
                        )
                    }
                }

                // 3 Summary Cards
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReportSummaryCard(
                        icon = "💰",
                        value = "${d.totalPaid.toInt()} ج.م",
                        label = "إجمالي المدفوع",
                        bgColor = Color(0xFFFFFBEB),
                        borderColor = Color(0xFFFBBF24),
                        valueColor = Color(0xFFD97706),
                        modifier = Modifier.weight(1f)
                    )
                    ReportSummaryCard(
                        icon = "📊",
                        value = d.avgGradeStr,
                        label = "متوسط الدرجات",
                        bgColor = Color(0xFFEFF6FF),
                        borderColor = Color(0xFF93C5FD),
                        valueColor = Color(0xFF1D4ED8),
                        modifier = Modifier.weight(1f)
                    )
                    ReportSummaryCard(
                        icon = "📅",
                        value = "${d.attendanceRate}%",
                        label = "نسبة الحضور",
                        bgColor = Color(0xFFF0FDF4),
                        borderColor = Color(0xFF86EFAC),
                        valueColor = Color(0xFF16A34A),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Stats Grid (8 Key Metrics)
                Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                    Text(
                        text = "ملخص إحصائيات الأداء الأكاديمي",
                        color = Color(0xFF111111),
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ReportStatItem("نسبة الحضور", "${d.attendanceRate}%", GreenSuccess, Modifier.weight(1f))
                        ReportStatItem("عدد الغياب", "${d.absentCount} مرة", RedDanger, Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ReportStatItem("الواجبات المُسلّمة", "${d.onTimeCount} من ${d.groupHomeworks.size}", BluePrimary, Modifier.weight(1f))
                        ReportStatItem("لم تُسلّم", "${d.notSubmittedCount}", RedDanger, Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ReportStatItem("متوسط الاختبارات", d.avgGradeStr, BluePrimary, Modifier.weight(1f))
                        ReportStatItem("نسبة المشاركة", "${d.interactRate}%", OrangeWarning, Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ReportStatItem("تسليم الواجبات", "${d.hwRate}%", BluePrimary, Modifier.weight(1f))
                        ReportStatItem("الأداء النهائي", "${d.finalPerf}%", perfColor, Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE5E7EB))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(finalPerformance / 100f)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(perfColor)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Interaction Cards
                Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                    Text(
                        text = "تفاعل الطالب داخل الحصة ⚡",
                        color = Color(0xFF111111),
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ReportSummaryCard(
                            icon = "🌟",
                            value = "${d.interactAll}",
                            label = "جاوب على الكل",
                            bgColor = Color(0xFFF0FDF4),
                            borderColor = Color(0xFF86EFAC),
                            valueColor = Color(0xFF16A34A),
                            modifier = Modifier.weight(1f)
                        )
                        ReportSummaryCard(
                            icon = "👍",
                            value = "${d.interactMost}",
                            label = "جاوب على معظمه",
                            bgColor = Color(0xFFEFF6FF),
                            borderColor = Color(0xFF93C5FD),
                            valueColor = Color(0xFF1D4ED8),
                            modifier = Modifier.weight(1f)
                        )
                        ReportSummaryCard(
                            icon = "😔",
                            value = "${d.interactNone}",
                            label = "لم يجب",
                            bgColor = Color(0xFFFEF2F2),
                            borderColor = Color(0xFFFCA5A5),
                            valueColor = Color(0xFFDC2626),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Financial Overview Cards
                Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                    Text(
                        text = "الوضع المالي 💰",
                        color = Color(0xFF111111),
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FinancialCard(
                            label = "حالة الاشتراك",
                            value = if (d.arrears <= 0) "مسدد ✅" else "متعثر ⚠️",
                            color = if (d.arrears <= 0) GreenSuccess else RedDanger,
                            Modifier.weight(1f)
                        )
                        FinancialCard(
                            label = "المتأخرات",
                            value = "${d.arrears.toInt()} ج.م",
                            color = if (d.arrears > 0) RedDanger else GreenSuccess,
                            Modifier.weight(1f)
                        )
                        FinancialCard(
                            label = "الخصم",
                            value = d.discountLabel,
                            color = Color(0xFF555555),
                            Modifier.weight(1f)
                        )
                        FinancialCard(
                            label = "المدفوع",
                            value = "${d.totalPaid.toInt()} ج.م",
                            color = BluePrimary,
                            Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Footer with Teacher Sign and Stamp
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF9FAFB))
                        .border(1.dp, Color(0xFFE5E7EB))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "توقيع المعلم", color = Color(0xFF888888), fontSize = 10.sp)
                            Text(
                                text = d.teacher.name,
                                color = Color(0xFF111111),
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                            Text(text = d.teacher.subject, color = themePrimary, fontSize = 10.sp)
                        }

                        // Seal
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, themePrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "ختم\nرسمي",
                                color = themePrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "تاريخ الإصدار", color = Color(0xFF888888), fontSize = 10.sp)
                            Text(
                                text = d.todayAr,
                                color = Color(0xFF111111),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons Section: CSV, Word, PDF, WhatsApp
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 1: CSV Export & Word Export
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val csv = StudentReportGenerator.generateMonthlyPerformanceCsv(
                                    calculatedList = listOf(d),
                                    title = "تقرير أداء الطالب - ${student.name}",
                                    filterLabel = group?.name ?: student.grade
                                )
                                StudentReportGenerator.exportAndShareCsv(
                                    context = context,
                                    csvContent = csv,
                                    fileName = "تقرير_${student.name}_${student.code}"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)), // Teal
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تصدير CSV (Excel)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                StudentReportGenerator.exportReportWord(context, d, theme = selectedTheme)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)), // Royal Blue
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تصدير Word (.doc)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Row 2: Print/PDF & WhatsApp
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                StudentReportGenerator.printOrSavePdf(context, d, theme = selectedTheme)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)), // Amber/Print
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("طباعة / حفظ PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                StudentReportGenerator.sendReportWhatsApp(context, d)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مشاركة واتساب", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("إغلاق التقرير", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ReportSummaryCard(
    icon: String,
    value: String,
    label: String,
    bgColor: Color,
    borderColor: Color,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = icon, fontSize = 16.sp)
            Text(text = value, color = valueColor, fontWeight = FontWeight.Black, fontSize = 13.sp)
            Text(text = label, color = Color(0xFF666666), fontSize = 9.sp)
        }
    }
}

@Composable
fun ReportStatItem(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF9FAFB))
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color(0xFF666666), fontSize = 10.sp)
        Text(text = value, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
fun FinancialCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF9FAFB))
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(8.dp))
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = value, color = color, fontWeight = FontWeight.Black, fontSize = 11.sp)
            Text(text = label, color = Color(0xFF777777), fontSize = 8.sp)
        }
    }
}
