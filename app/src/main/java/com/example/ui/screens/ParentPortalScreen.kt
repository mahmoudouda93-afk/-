package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudentEntity
import com.example.ui.MainViewModel
import com.example.ui.components.AcademicReportDialog
import com.example.ui.components.StudentIdCard
import com.example.ui.theme.*

@Composable
fun ParentPortalScreen(
    viewModel: MainViewModel
) {
    val students by viewModel.students.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val attendance by viewModel.attendance.collectAsState()
    val payments by viewModel.payments.collectAsState()
    val exams by viewModel.exams.collectAsState()
    val scores by viewModel.examScores.collectAsState()

    var inputCodeOrToken by remember { mutableStateOf("") }
    var matchedStudent by remember { mutableStateOf<StudentEntity?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showFullReport by remember { mutableStateOf(false) }

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
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BlueLight))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.FamilyRestroom,
                        contentDescription = null,
                        tint = BlueLight,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "بوابة ولي الأمر الذكية",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "متابعة فورية للحضور والدرجات والاشتراكات الشهرية",
                            color = BlueLight,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Code input card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "أدخل كود الطالب (مثال: ST10001) أو رمز ولي الأمر الخاص:",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = inputCodeOrToken,
                        onValueChange = {
                            inputCodeOrToken = it
                            errorMessage = null
                        },
                        placeholder = { Text("كود الطالب أو الرمز...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val q = inputCodeOrToken.trim().lowercase()
                            val found = students.find {
                                it.code.lowercase() == q || it.parentToken.lowercase() == q || it.phone == q || it.parentPhone == q
                            }
                            if (found != null) {
                                matchedStudent = found
                                errorMessage = null
                            } else {
                                matchedStudent = null
                                errorMessage = "لم يتم العثور على طالب بهذا الرمز أو الكود"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("عرض ملف الطالب الأكاديمي", fontWeight = FontWeight.Bold)
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            color = RedDanger,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Results View
            matchedStudent?.let { student ->
                val grp = groups.find { it.id == student.groupId }
                val studentAttendance = attendance.filter { it.studentId == student.id }
                val presentCount = studentAttendance.count { it.status == "present" || it.status == "late" }
                val absentCount = studentAttendance.count { it.status == "absent" }
                val totalSessions = studentAttendance.size
                val attendanceRate = if (totalSessions > 0) (presentCount * 100) / totalSessions else 0

                val studentPayments = payments.filter { it.studentId == student.id }
                val totalPaid = studentPayments.sumOf { it.amount }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(GreenLight))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "ملف الطالب: ${student.name}",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )

                        // Student ID Card
                        StudentIdCard(student = student, group = grp)

                        Spacer(modifier = Modifier.height(4.dp))

                        // Stats Summary
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ReportMetricBox("حضور", "$presentCount حصص", GreenLight, Modifier.weight(1f))
                            ReportMetricBox("غياب", "$absentCount حصص", RedDanger, Modifier.weight(1f))
                            ReportMetricBox("الالتزام", "$attendanceRate%", PurpleAccent, Modifier.weight(1f))
                            ReportMetricBox("المدفوع", "${totalPaid.toInt()} ج", GoldAccent, Modifier.weight(1f))
                        }

                        Button(
                            onClick = { showFullReport = true },
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("فتح تقرير الأداء الأكاديمي الشامل")
                        }
                    }
                }
            }
        }
    }

    if (showFullReport && matchedStudent != null) {
        val st = matchedStudent!!
        val grp = groups.find { it.id == st.groupId }
        AcademicReportDialog(
            student = st,
            group = grp,
            attendanceList = attendance,
            paymentsList = payments,
            examsList = exams,
            scoresList = scores,
            onDismiss = { showFullReport = false }
        )
    }
}
