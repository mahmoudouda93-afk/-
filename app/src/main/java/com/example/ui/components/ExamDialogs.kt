package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ExamEntity
import com.example.data.model.ExamScoreEntity
import com.example.data.model.GroupEntity
import com.example.data.model.StudentEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateExamDialog(
    groups: List<GroupEntity>,
    initialGroupId: String? = null,
    onDismiss: () -> Unit,
    onSave: (ExamEntity) -> Unit
) {
    var selectedGroupId by remember {
        mutableStateOf(initialGroupId ?: groups.firstOrNull()?.id ?: "")
    }
    var examName by remember { mutableStateOf("") }
    var maxScoreText by remember { mutableStateOf("100") }
    var examDate by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    }
    var isGroupDropdownExpanded by remember { mutableStateOf(false) }

    val quickTitles = listOf(
        "امتحان شامل 1",
        "امتحان الفصل الأول",
        "كويز أسبوعي",
        "اختبار تجريبي",
        "مراجعة نهائية"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NavySurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📝 إنشاء امتحان جديد",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.Gray)
                    }
                }

                HorizontalDivider(color = NavyBorder)

                // Group Selector
                Text("المجموعة الدراسية المستهدفة *", color = BlueLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                ExposedDropdownMenuBox(
                    expanded = isGroupDropdownExpanded,
                    onExpandedChange = { isGroupDropdownExpanded = !isGroupDropdownExpanded }
                ) {
                    val currentGroup = groups.find { it.id == selectedGroupId }
                    OutlinedTextField(
                        value = currentGroup?.let { "${it.name} (${it.grade})" } ?: "اختر المجموعة...",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isGroupDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BluePrimary,
                            unfocusedBorderColor = NavyBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = isGroupDropdownExpanded,
                        onDismissRequest = { isGroupDropdownExpanded = false },
                        modifier = Modifier.background(NavyCard)
                    ) {
                        groups.forEach { grp ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(grp.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${grp.grade} • ${grp.subject}", color = BlueLight, fontSize = 11.sp)
                                    }
                                },
                                onClick = {
                                    selectedGroupId = grp.id
                                    isGroupDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Exam Name
                Text("عنوان أو اسم الامتحان *", color = BlueLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = examName,
                    onValueChange = { examName = it },
                    placeholder = { Text("مثال: امتحان الباب الأول - الحملة الفرنسية", fontSize = 12.sp, color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BluePrimary,
                        unfocusedBorderColor = NavyBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                // Quick suggestions chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickTitles.take(3).forEach { title ->
                        SuggestionChip(
                            onClick = { examName = title },
                            label = { Text(title, fontSize = 10.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = NavyCard,
                                labelColor = BlueLight
                            )
                        )
                    }
                }

                // Max Score with Presets
                Text("الدرجة العظمى (النهائية) *", color = BlueLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = maxScoreText,
                        onValueChange = { maxScoreText = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BluePrimary,
                            unfocusedBorderColor = NavyBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    listOf("20", "50", "100").forEach { preset ->
                        FilterChip(
                            selected = maxScoreText == preset,
                            onClick = { maxScoreText = preset },
                            label = { Text(preset, fontSize = 11.sp) }
                        )
                    }
                }

                // Date
                Text("تاريخ الامتحان", color = BlueLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = examDate,
                    onValueChange = { examDate = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BluePrimary,
                        unfocusedBorderColor = NavyBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val max = maxScoreText.toDoubleOrNull() ?: 100.0
                            if (examName.isNotBlank() && selectedGroupId.isNotBlank() && max > 0) {
                                onSave(
                                    ExamEntity(
                                        groupId = selectedGroupId,
                                        name = examName.trim(),
                                        maxScore = max,
                                        date = examDate.trim()
                                    )
                                )
                            }
                        },
                        enabled = examName.isNotBlank() && selectedGroupId.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ الامتحان", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(0.7f)
                    ) {
                        Text("إلغاء", color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun ExamGradebookDialog(
    exam: ExamEntity,
    group: GroupEntity?,
    students: List<StudentEntity>,
    scores: List<ExamScoreEntity>,
    onSetScore: (studentId: String, score: Double) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    val groupStudents = remember(students, exam.groupId) {
        students.filter { it.groupId == exam.groupId }
    }

    val displayStudents = if (groupStudents.isNotEmpty()) groupStudents else students

    val filteredStudents = remember(displayStudents, searchQuery) {
        if (searchQuery.isBlank()) displayStudents
        else displayStudents.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
    }

    // Stats
    val examScores = scores.filter { it.examId == exam.id }
    val gradedCount = examScores.count { it.score > 0 }
    val avgScore = if (examScores.isNotEmpty()) (examScores.sumOf { it.score } / examScores.size).toInt() else 0
    val maxActualScore = if (examScores.isNotEmpty()) examScores.maxOf { it.score }.toInt() else 0
    val minActualScore = if (examScores.isNotEmpty()) examScores.minOf { it.score }.toInt() else 0

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f)
                .padding(6.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NavySurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "📊 دفتر درجات: ${exam.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "${group?.name ?: "المجموعة"} • الدرجة العظمى: ${exam.maxScore.toInt()}",
                            color = BlueLight,
                            fontSize = 11.sp
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                    }
                }

                // Summary Stats Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NavyCard)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("تم رصدهم", color = Color.Gray, fontSize = 10.sp)
                        Text("$gradedCount / ${displayStudents.size}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("متوسط الدرجات", color = Color.Gray, fontSize = 10.sp)
                        Text("$avgScore / ${exam.maxScore.toInt()}", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("أعلى درجة", color = Color.Gray, fontSize = 10.sp)
                        Text("$maxActualScore", color = GreenLight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("أقل درجة", color = Color.Gray, fontSize = 10.sp)
                        Text("$minActualScore", color = RedDanger, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث عن طالب في المجموعة...", fontSize = 11.sp, color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BluePrimary,
                        unfocusedBorderColor = NavyBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                // Students Scoring List
                if (filteredStudents.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("لا يوجد طلاب مطابقين للبحث", color = Color.Gray, fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredStudents, key = { it.id }) { student ->
                            val currentScoreObj = examScores.find { it.studentId == student.id }
                            val currentScore = currentScoreObj?.score ?: 0.0
                            var scoreText by remember(student.id, currentScore) {
                                mutableStateOf(if (currentScore > 0) currentScore.toInt().toString() else "")
                            }

                            val percentage = if (exam.maxScore > 0) (currentScore / exam.maxScore) * 100 else 0.0
                            val statusBadge = when {
                                currentScore == 0.0 -> "لم يرصد" to Color.Gray
                                percentage >= 85 -> "ممتاز ⭐" to GreenLight
                                percentage >= 75 -> "جيد جداً" to BlueLight
                                percentage >= 65 -> "جيد" to GoldAccent
                                percentage >= 50 -> "مقبول" to Color(0xFFFFA726)
                                else -> "يحتاج متابعة ⚠️" to RedDanger
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = NavyCard)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
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
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = statusBadge.first,
                                                color = statusBadge.second,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (currentScore > 0) {
                                                Text(
                                                    text = "(${percentage.toInt()}%)",
                                                    color = Color.Gray,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // WhatsApp Result to Parent
                                        IconButton(
                                            onClick = {
                                                val phone = student.parentPhone.ifBlank { student.phone }
                                                val message = "📋 *نتيجة امتحان التاريخ*\n" +
                                                        "━━━━━━━━━━━━\n" +
                                                        "👤 اسم الطالب: *${student.name}*\n" +
                                                        "📚 المجموعة: ${group?.name ?: ""}\n" +
                                                        "📝 الاختبار: *${exam.name}*\n" +
                                                        "📅 التاريخ: ${exam.date}\n" +
                                                        "🎯 الدرجة: *${currentScore.toInt()}* من *${exam.maxScore.toInt()}* (${percentage.toInt()}%)\n" +
                                                        "⭐ التقدير: *${statusBadge.first}*\n" +
                                                        "━━━━━━━━━━━━\n" +
                                                        "👨‍🏫 سنتر التفوق — مستر محمود عوده"
                                                val url = if (phone.isNotBlank()) {
                                                    val cleanPhone = if (phone.startsWith("0")) "2$phone" else phone
                                                    "https://api.whatsapp.com/send?phone=$cleanPhone&text=" + Uri.encode(message)
                                                } else {
                                                    "https://api.whatsapp.com/send?text=" + Uri.encode(message)
                                                }
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                context.startActivity(Intent.createChooser(intent, "إرسال النتيجة عبر واتساب"))
                                            },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Send,
                                                contentDescription = "إرسال لواتساب",
                                                tint = GreenLight,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        // Score Input
                                        OutlinedTextField(
                                            value = scoreText,
                                            onValueChange = { newVal ->
                                                scoreText = newVal
                                                val num = newVal.toDoubleOrNull()
                                                if (num != null) {
                                                    val clamped = num.coerceIn(0.0, exam.maxScore)
                                                    onSetScore(student.id, clamped)
                                                }
                                            },
                                            placeholder = { Text("0", color = Color.Gray, fontSize = 11.sp) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.width(65.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = GoldAccent,
                                                unfocusedBorderColor = NavyBorder,
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Done Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("تم حفظ الدرجات", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
