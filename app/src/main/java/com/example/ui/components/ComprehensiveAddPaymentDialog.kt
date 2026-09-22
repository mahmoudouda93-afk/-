package com.example.ui.components

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.GroupEntity
import com.example.data.model.StudentEntity
import com.example.ui.theme.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComprehensiveAddPaymentDialog(
    groups: List<GroupEntity>,
    students: List<StudentEntity>,
    initialStudentId: String? = null,
    onDismiss: () -> Unit,
    onSave: (studentId: String, groupId: String, amount: Double, type: String, otherDesc: String?, targetMonth: Int) -> Unit
) {
    var selectedGroupId by remember {
        val initialStudent = students.find { it.id == initialStudentId }
        mutableStateOf(initialStudent?.groupId ?: "")
    }

    var selectedStudentId by remember {
        mutableStateOf(initialStudentId ?: "")
    }

    var studentSearchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("monthly") }
    var otherDesc by remember { mutableStateOf("") }
    var targetMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH) + 1) }

    // Filter students by selected group and search query
    val candidateStudents = remember(students, selectedGroupId, studentSearchQuery) {
        students.filter { st ->
            (selectedGroupId.isBlank() || st.groupId == selectedGroupId) &&
                    (studentSearchQuery.isBlank() || st.name.contains(studentSearchQuery.trim(), ignoreCase = true) || st.code.contains(studentSearchQuery.trim()))
        }
    }

    val selectedStudent = students.find { it.id == selectedStudentId }
    val studentGroup = groups.find { it.id == (selectedStudent?.groupId ?: selectedGroupId) }

    // Calculate recommended price based on group price & student discount
    val defaultPrice = remember(selectedStudent, studentGroup, selectedType) {
        val basePrice = studentGroup?.price ?: 0.0
        when (selectedType) {
            "monthly" -> {
                when (selectedStudent?.discountType) {
                    "percentage" -> basePrice * (1.0 - (selectedStudent.discountValue / 100.0)).coerceAtLeast(0.0)
                    "fixed" -> (basePrice - selectedStudent.discountValue).coerceAtLeast(0.0)
                    else -> basePrice
                }
            }
            "half_monthly" -> (basePrice / 2.0).toInt().toDouble()
            "explanation_notes" -> 50.0
            "review_notes" -> 40.0
            else -> basePrice
        }
    }

    var amountText by remember { mutableStateOf(defaultPrice.toInt().toString()) }

    // Update amount if student or type changes
    LaunchedEffect(defaultPrice) {
        amountText = defaultPrice.toInt().toString()
    }

    val paymentTypes = listOf(
        "monthly" to "اشتراك شهر كامل",
        "half_monthly" to "اشتراك نصف شهر",
        "explanation_notes" to "مذكرة شرح",
        "review_notes" to "مذكرة مراجعة",
        "other" to "أخرى"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
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
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💳 تسجيل دفعة وإصدار إيصال",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.Gray)
                    }
                }

                HorizontalDivider(color = NavyBorder)

                // Group Filter Chips (Optional filter to narrow down students)
                if (groups.isNotEmpty()) {
                    Text("تصفية بالمجموعة الدراسية:", color = BlueLight, fontSize = 11.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = selectedGroupId.isBlank(),
                            onClick = { selectedGroupId = "" },
                            label = { Text("جميع المجموعات", fontSize = 10.sp) }
                        )
                        groups.take(3).forEach { grp ->
                            FilterChip(
                                selected = selectedGroupId == grp.id,
                                onClick = {
                                    selectedGroupId = if (selectedGroupId == grp.id) "" else grp.id
                                },
                                label = { Text(grp.name.take(12), fontSize = 10.sp) }
                            )
                        }
                    }
                }

                // Student Search Bar
                OutlinedTextField(
                    value = studentSearchQuery,
                    onValueChange = { studentSearchQuery = it },
                    placeholder = { Text("ابحث عن اسم الطالب أو الكود...", fontSize = 11.sp, color = Color.Gray) },
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

                // Candidate Students List
                Text("اختر الطالب المستهدف *", color = BlueLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(NavyCard)
                        .padding(6.dp)
                ) {
                    if (candidateStudents.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("لا يوجد طلاب مطابقين للبحث", color = Color.Gray, fontSize = 11.sp)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(candidateStudents, key = { it.id }) { st ->
                                val isSelected = selectedStudentId == st.id
                                val grp = groups.find { it.id == st.groupId }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) BluePrimary.copy(alpha = 0.25f) else Color.Transparent)
                                        .clickable {
                                            selectedStudentId = st.id
                                            if (selectedGroupId.isBlank()) {
                                                selectedGroupId = st.groupId
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { selectedStudentId = st.id },
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(st.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("${grp?.name ?: "المجموعة"} • كود: ${st.code}", color = BlueLight, fontSize = 10.sp)
                                        }
                                    }

                                    if (st.discountType != "none") {
                                        Text(
                                            text = if (st.discountType == "percentage") "خصم ${st.discountValue.toInt()}%" else "خصم ${st.discountValue.toInt()} ج",
                                            color = GoldAccent,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Scrollable payment parameters
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Amount field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            label = { Text("المبلغ المطلوب تحصيله (ج.م) *", fontSize = 11.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GreenSuccess,
                                unfocusedBorderColor = NavyBorder,
                                focusedTextColor = GreenLight,
                                unfocusedTextColor = GreenLight
                            )
                        )

                        // Student Group Price tag
                        studentGroup?.let { grp ->
                            Column(horizontalAlignment = Alignment.End) {
                                Text("السعر الأصلي", color = Color.Gray, fontSize = 10.sp)
                                Text("${grp.price.toInt()} ج.م", color = BlueLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Target Month Selector (1-12)
                    Text("الشهر المستهدف:", color = BlueLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        (1..6).forEach { m ->
                            FilterChip(
                                selected = targetMonth == m,
                                onClick = { targetMonth = m },
                                label = { Text("ش$m", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
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
                                label = { Text("ش$m", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Payment Type
                    Text("نوع البند / الدفع:", color = BlueLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    paymentTypes.forEach { (type, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { selectedType = type }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(label, color = Color.White, fontSize = 12.sp)
                        }
                    }

                    if (selectedType == "other") {
                        OutlinedTextField(
                            value = otherDesc,
                            onValueChange = { otherDesc = it },
                            label = { Text("توضيح البند...") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BluePrimary,
                                unfocusedBorderColor = NavyBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }
                }

                // Submit Button
                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        val targetGrpId = selectedStudent?.groupId ?: selectedGroupId.ifBlank { groups.firstOrNull()?.id ?: "" }
                        if (selectedStudentId.isNotBlank() && amt > 0 && targetGrpId.isNotBlank()) {
                            onSave(
                                selectedStudentId,
                                targetGrpId,
                                amt,
                                selectedType,
                                if (selectedType == "other") otherDesc else null,
                                targetMonth
                            )
                        }
                    },
                    enabled = selectedStudentId.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حفظ العملية وطباعة الإيصال", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
