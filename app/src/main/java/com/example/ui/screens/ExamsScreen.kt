package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExamEntity
import com.example.ui.MainViewModel
import com.example.ui.components.CreateExamDialog
import com.example.ui.components.ExamGradebookDialog
import com.example.ui.screens.InClassQuizDialog
import com.example.ui.theme.*

@Composable
fun ExamsScreen(
    viewModel: MainViewModel
) {
    val exams by viewModel.exams.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val students by viewModel.students.collectAsState()
    val scores by viewModel.examScores.collectAsState()

    var showAddExamDialog by remember { mutableStateOf(false) }
    var showNoGroupsAlert by remember { mutableStateOf(false) }
    var inClassQuizExam by remember { mutableStateOf<ExamEntity?>(null) }
    var gradebookExam by remember { mutableStateOf<ExamEntity?>(null) }
    var examToDelete by remember { mutableStateOf<ExamEntity?>(null) }

    var selectedGroupIdFilter by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredExams = remember(exams, selectedGroupIdFilter, searchQuery) {
        exams.filter { exam ->
            (selectedGroupIdFilter == null || exam.groupId == selectedGroupIdFilter) &&
                    (searchQuery.isBlank() || exam.name.contains(searchQuery.trim(), ignoreCase = true))
        }
    }

    Scaffold(
        containerColor = NavyDark,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (groups.isEmpty()) {
                        showNoGroupsAlert = true
                    } else {
                        showAddExamDialog = true
                    }
                },
                containerColor = BluePrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة امتحان")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyBorder))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "سجل الامتحانات والاختبارات",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "إجمالي الاختبارات: ${exams.size} اختبار",
                            color = BlueLight,
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = {
                            if (groups.isEmpty()) {
                                showNoGroupsAlert = true
                            } else {
                                showAddExamDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("امتحان جديد", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("ابحث في أسماء الامتحانات...", fontSize = 12.sp, color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BluePrimary,
                    unfocusedBorderColor = NavyBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            // Group Filter Chips
            if (groups.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedGroupIdFilter == null,
                        onClick = { selectedGroupIdFilter = null },
                        label = { Text("الكل (${exams.size})", fontSize = 11.sp) }
                    )
                    groups.forEach { grp ->
                        val count = exams.count { it.groupId == grp.id }
                        FilterChip(
                            selected = selectedGroupIdFilter == grp.id,
                            onClick = {
                                selectedGroupIdFilter = if (selectedGroupIdFilter == grp.id) null else grp.id
                            },
                            label = { Text("${grp.name} ($count)", fontSize = 10.sp) }
                        )
                    }
                }
            }

            // Exams List
            if (filteredExams.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📝", fontSize = 42.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("لا توجد امتحانات مسجلة", color = Color.Gray, fontSize = 14.sp)
                        if (groups.isEmpty()) {
                            Text("قم بإنشاء مجموعة دراسية أولاً ثم أضف الامتحانات", color = BlueLight, fontSize = 11.sp)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredExams, key = { it.id }) { exam ->
                        val group = groups.find { it.id == exam.groupId }
                        val groupStudents = students.filter { it.groupId == exam.groupId }
                        val examScores = scores.filter { it.examId == exam.id }
                        val gradedCount = examScores.count { it.score > 0 }
                        val avgScore = if (examScores.isNotEmpty()) {
                            (examScores.sumOf { it.score } / examScores.size).toInt()
                        } else 0
                        val passCount = examScores.count { it.score >= (exam.maxScore * 0.5) }
                        val passRate = if (gradedCount > 0) (passCount * 100 / gradedCount) else 0

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = NavySurface),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyBorder))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Title & Group
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = exam.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = "${group?.name ?: "المجموعة"} (${group?.grade ?: ""}) • ${exam.date}",
                                            color = BlueLight,
                                            fontSize = 11.sp
                                        )
                                    }

                                    IconButton(
                                        onClick = { examToDelete = exam },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف الامتحان", tint = RedDanger, modifier = Modifier.size(16.dp))
                                    }
                                }

                                // Quick Stats Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(NavyCard)
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "الدرجة العظمى: ${exam.maxScore.toInt()}",
                                        color = GoldAccent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "الطلاب: $gradedCount/${groupStudents.size}",
                                        color = Color.White,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "المتوسط: $avgScore",
                                        color = BlueLight,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "نسبة النجاح: $passRate%",
                                        color = if (passRate >= 70) GreenLight else RedDanger,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Actions: Gradebook & In-class Quiz
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Gradebook Button
                                    Button(
                                        onClick = { gradebookExam = exam },
                                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("رصد الدرجات 📊", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // Live Quiz Button
                                    Button(
                                        onClick = { inClassQuizExam = exam },
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldDark),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("كويز مباشر ⚡", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Exam Dialog
    if (showAddExamDialog && groups.isNotEmpty()) {
        CreateExamDialog(
            groups = groups,
            initialGroupId = selectedGroupIdFilter,
            onDismiss = { showAddExamDialog = false },
            onSave = { newExam ->
                viewModel.addExam(newExam)
                showAddExamDialog = false
            }
        )
    }

    // No Groups Alert
    if (showNoGroupsAlert) {
        AlertDialog(
            onDismissRequest = { showNoGroupsAlert = false },
            title = { Text("تنبيه", fontWeight = FontWeight.Bold, color = Color.White) },
            text = { Text("يرجى إنشاء مجموعة دراسية أولاً لربط الامتحان بها.", color = BlueLight) },
            confirmButton = {
                Button(onClick = { showNoGroupsAlert = false }) {
                    Text("حسناً")
                }
            },
            containerColor = NavySurface
        )
    }

    // Delete Exam Confirmation
    examToDelete?.let { exam ->
        AlertDialog(
            onDismissRequest = { examToDelete = null },
            title = { Text("حذف الامتحان", fontWeight = FontWeight.Bold, color = Color.White) },
            text = { Text("هل أنت متأكد من حذف امتحان \"${exam.name}\" وجميع الدرجات المرتبطة به؟", color = BlueLight) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteExam(exam)
                        examToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedDanger)
                ) {
                    Text("نعم، حذف")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { examToDelete = null }) {
                    Text("إلغاء", color = Color.Gray)
                }
            },
            containerColor = NavySurface
        )
    }

    // Gradebook Dialog
    gradebookExam?.let { exam ->
        val group = groups.find { it.id == exam.groupId }
        ExamGradebookDialog(
            exam = exam,
            group = group,
            students = students,
            scores = scores,
            onSetScore = { studentId, score ->
                viewModel.setExamScore(exam.id, studentId, score)
            },
            onDismiss = { gradebookExam = null }
        )
    }

    // Live In-Class Quiz Dialog
    inClassQuizExam?.let { exam ->
        val groupStudents = students.filter { it.groupId == exam.groupId }
        InClassQuizDialog(
            exam = exam,
            students = if (groupStudents.isNotEmpty()) groupStudents else students,
            scores = scores.filter { it.examId == exam.id },
            onSetScore = { stId, sc -> viewModel.setExamScore(exam.id, stId, sc) },
            onDismiss = { inClassQuizExam = null }
        )
    }
}

