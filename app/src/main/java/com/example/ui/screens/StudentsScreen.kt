package com.example.ui.screens

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.GroupEntity
import com.example.data.model.StudentEntity
import com.example.ui.MainViewModel
import com.example.ui.components.StudentIdCard
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

val GRADES_LIST = listOf(
    "الصف الأول الإعدادي",
    "الصف الثاني الإعدادي",
    "الصف الثالث الإعدادي",
    "الصف الأول الثانوي",
    "الصف الثاني الثانوي",
    "الصف الثالث الثانوي"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentsScreen(
    viewModel: MainViewModel,
    onNavigateToStudent: (String) -> Unit
) {
    val students by viewModel.students.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val attendance by viewModel.attendance.collectAsState()
    val payments by viewModel.payments.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedGroupFilter by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingStudent by remember { mutableStateOf<StudentEntity?>(null) }
    var showBulkImportDialog by remember { mutableStateOf(false) }
    var showBatchCardsDialog by remember { mutableStateOf(false) }

    val filteredStudents = remember(students, searchQuery, selectedGroupFilter) {
        students.filter { st ->
            val matchesGroup = selectedGroupFilter == null || st.groupId == selectedGroupFilter
            val q = searchQuery.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                    st.name.lowercase().contains(q) ||
                    st.code.lowercase().contains(q) ||
                    st.phone.contains(q) ||
                    st.parentPhone.contains(q)
            matchesGroup && matchesQuery
        }
    }

    Scaffold(
        containerColor = NavyDark,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = BluePrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add Student")
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
            // Top action bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "سجل الطلاب (${students.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { showBulkImportDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A5F)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp), tint = BlueLight)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("استيراد Excel", fontSize = 11.sp, color = BlueLight)
                    }

                    if (filteredStudents.isNotEmpty()) {
                        Button(
                            onClick = { showBatchCardsDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("طباعة كروت", fontSize = 11.sp)
                        }
                    }
                }
            }

            // Search & Filter
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("بحث بالاسم، الكود، أو رقم الهاتف...", color = Color.Gray, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = BlueLight) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = BlueLight)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = BluePrimary,
                    unfocusedBorderColor = NavyBorder,
                    focusedContainerColor = NavySurface,
                    unfocusedContainerColor = NavySurface
                )
            )

            // Group Filter Chips
            if (groups.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedGroupFilter == null,
                        onClick = { selectedGroupFilter = null },
                        label = { Text("الكل (${students.size})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BluePrimary,
                            selectedLabelColor = Color.White,
                            labelColor = BlueLight
                        )
                    )
                    groups.forEach { grp ->
                        val count = students.count { it.groupId == grp.id }
                        FilterChip(
                            selected = selectedGroupFilter == grp.id,
                            onClick = { selectedGroupFilter = if (selectedGroupFilter == grp.id) null else grp.id },
                            label = { Text("${grp.name} ($count)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BluePrimary,
                                selectedLabelColor = Color.White,
                                labelColor = BlueLight
                            )
                        )
                    }
                }
            }

            // Students List
            if (filteredStudents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎓", fontSize = 44.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("لا يوجد طلاب مسجلين", color = Color.Gray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                        ) {
                            Text("إضافة طالب جديد")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredStudents, key = { it.id }) { student ->
                        val group = groups.find { it.id == student.groupId }
                        val attCount = attendance.count { it.studentId == student.id && (it.status == "present" || it.status == "late") }
                        val paidTotal = payments.filter { it.studentId == student.id }.sumOf { it.amount }

                        StudentListItem(
                            student = student,
                            group = group,
                            attendanceCount = attCount,
                            totalPaid = paidTotal,
                            onCardClick = { onNavigateToStudent(student.id) },
                            onEditClick = { editingStudent = student },
                            onDeleteClick = { viewModel.deleteStudent(student) }
                        )
                    }
                }
            }
        }
    }

    // Add Student Dialog
    if (showAddDialog) {
        StudentFormDialog(
            groups = groups,
            existing = null,
            onDismiss = { showAddDialog = false },
            onSave = { name, grpId, grade, phone, parentPhone, parentPhone2, relation, discType, discVal, joinDate ->
                viewModel.addStudent(name, grpId, grade, phone, parentPhone, parentPhone2, relation, discType, discVal, joinDate)
                showAddDialog = false
            }
        )
    }

    // Edit Student Dialog
    editingStudent?.let { st ->
        StudentFormDialog(
            groups = groups,
            existing = st,
            onDismiss = { editingStudent = null },
            onSave = { name, grpId, grade, phone, parentPhone, parentPhone2, relation, discType, discVal, joinDate ->
                viewModel.updateStudent(
                    st.copy(
                        name = name,
                        groupId = grpId,
                        grade = grade,
                        phone = phone,
                        parentPhone = parentPhone,
                        parentPhone2 = parentPhone2,
                        parentRelation = relation,
                        discountType = discType,
                        discountValue = discVal,
                        joinDate = joinDate
                    )
                )
                editingStudent = null
            }
        )
    }

    // Bulk Import Dialog
    if (showBulkImportDialog) {
        BulkImportDialog(
            groups = groups,
            onDismiss = { showBulkImportDialog = false },
            onImport = { list ->
                viewModel.bulkImport(list)
                showBulkImportDialog = false
            }
        )
    }

    // Batch Print Cards Dialog
    if (showBatchCardsDialog) {
        BatchCardsDialog(
            students = filteredStudents,
            groups = groups,
            onDismiss = { showBatchCardsDialog = false }
        )
    }
}

@Composable
fun StudentListItem(
    student: StudentEntity,
    group: GroupEntity?,
    attendanceCount: Int,
    totalPaid: Double,
    onCardClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyBorder))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(BluePrimary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = student.name.take(1),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = student.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = student.code,
                        color = BlueLight,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = "${group?.name ?: "بدون مجموعة"} • ${student.grade}",
                    color = Color.Gray,
                    fontSize = 11.sp
                )

                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "حضور: $attendanceCount",
                        color = GreenLight,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "مدفوع: ${totalPaid.toInt()} ج",
                        color = GoldAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = BlueLight, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RedDanger, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ─── Add/Edit Student Dialog ─────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentFormDialog(
    groups: List<GroupEntity>,
    existing: StudentEntity?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        groupId: String,
        grade: String,
        phone: String,
        parentPhone: String,
        parentPhone2: String,
        parentRelation: String,
        discountType: String,
        discountValue: Double,
        joinDate: String
    ) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var selectedGroupId by remember { mutableStateOf(existing?.groupId ?: groups.firstOrNull()?.id ?: "") }
    var grade by remember { mutableStateOf(existing?.grade ?: GRADES_LIST[3]) }
    var phone by remember { mutableStateOf(existing?.phone ?: "") }
    var parentPhone by remember { mutableStateOf(existing?.parentPhone ?: "") }
    var parentPhone2 by remember { mutableStateOf(existing?.parentPhone2 ?: "") }
    var parentRelation by remember { mutableStateOf(existing?.parentRelation ?: "أب") }
    var discountType by remember { mutableStateOf(existing?.discountType ?: "none") }
    var discountValueText by remember { mutableStateOf(existing?.discountValue?.toInt()?.toString() ?: "0") }
    var joinDate by remember {
        mutableStateOf(existing?.joinDate ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    }

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
                    text = if (existing == null) "➕ إضافة طالب جديد" else "✏ تعديل بيانات الطالب",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الطالب ثلاثي أو رباعي *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Group selector
                Text("المجموعة الدراسية *", color = BlueLight, fontSize = 12.sp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    groups.forEach { grp ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedGroupId == grp.id) NavyCard else Color.Transparent)
                                .clickable { selectedGroupId = grp.id }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            RadioButton(
                                selected = selectedGroupId == grp.id,
                                onClick = { selectedGroupId = grp.id },
                                colors = RadioButtonDefaults.colors(selectedColor = BluePrimary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "${grp.name} (${grp.grade})", color = Color.White, fontSize = 13.sp)
                        }
                    }
                }

                // Grade
                Text("الصف الدراسي", color = BlueLight, fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    GRADES_LIST.take(3).forEach { g ->
                        FilterChip(
                            selected = grade == g,
                            onClick = { grade = g },
                            label = { Text(g.replace("الصف ", ""), fontSize = 10.sp) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    GRADES_LIST.drop(3).forEach { g ->
                        FilterChip(
                            selected = grade == g,
                            onClick = { grade = g },
                            label = { Text(g.replace("الصف ", ""), fontSize = 10.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("هاتف الطالب (01xxxxxxxxx)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = parentPhone,
                        onValueChange = { parentPhone = it },
                        label = { Text("هاتف ولي الأمر") },
                        singleLine = true,
                        modifier = Modifier.weight(1.5f)
                    )
                    OutlinedTextField(
                        value = parentRelation,
                        onValueChange = { parentRelation = it },
                        label = { Text("صلة القرابة") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = parentPhone2,
                    onValueChange = { parentPhone2 = it },
                    label = { Text("هاتف إضافي لولي الأمر (اختياري)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Discount selection
                Text("نوع الخصم (إن وجد)", color = BlueLight, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("none" to "بدون خصم", "fixed" to "مبلغ ثابت", "percentage" to "نسبة مئوية %").forEach { (type, lbl) ->
                        FilterChip(
                            selected = discountType == type,
                            onClick = { discountType = type },
                            label = { Text(lbl, fontSize = 11.sp) }
                        )
                    }
                }

                if (discountType != "none") {
                    OutlinedTextField(
                        value = discountValueText,
                        onValueChange = { discountValueText = it },
                        label = { Text(if (discountType == "percentage") "نسبة الخصم %" else "قيمة الخصم (ج.م)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (name.isNotBlank() && selectedGroupId.isNotBlank()) {
                                onSave(
                                    name,
                                    selectedGroupId,
                                    grade,
                                    phone,
                                    parentPhone,
                                    parentPhone2,
                                    parentRelation,
                                    discountType,
                                    discountValueText.toDoubleOrNull() ?: 0.0,
                                    joinDate
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (existing == null) "حفظ الطالب" else "تحديث البيانات", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء")
                    }
                }
            }
        }
    }
}

// ─── Bulk Import Modal ───────────────────────────────────────
@Composable
fun BulkImportDialog(
    groups: List<GroupEntity>,
    onDismiss: () -> Unit,
    onImport: (List<StudentEntity>) -> Unit
) {
    var rawText by remember {
        mutableStateOf(
            "أحمد محمد علي, 01011112222, 01133334444\n" +
                    "محمود إبراهيم حسن, 01055556666, 01277778888\n" +
                    "ياسمين خالد فاروق, 01099990000, 01122223333"
        )
    }
    var targetGroupId by remember { mutableStateOf(groups.firstOrNull()?.id ?: "") }
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NavySurface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "📥 استيراد جماعي للطلاب (Excel/CSV)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "الصق قائمة الطلاب (كل سطر: الاسم، هاتف الطالب، هاتف ولي الأمر):",
                    color = BlueLight,
                    fontSize = 11.sp
                )

                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Text("المجموعة المستهدفة:", color = BlueLight, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    groups.forEach { grp ->
                        FilterChip(
                            selected = targetGroupId == grp.id,
                            onClick = { targetGroupId = grp.id },
                            label = { Text(grp.name, fontSize = 10.sp) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val lines = rawText.split("\n").filter { it.isNotBlank() }
                            val newStudents = lines.mapIndexed { idx, line ->
                                val parts = line.split(",").map { it.trim() }
                                val stName = parts.getOrNull(0) ?: "طالب $idx"
                                val stPhone = parts.getOrNull(1) ?: ""
                                val pPhone = parts.getOrNull(2) ?: ""
                                StudentEntity(
                                    code = "ST${10001 + idx + (1..999).random()}",
                                    name = stName,
                                    groupId = targetGroupId,
                                    grade = groups.find { it.id == targetGroupId }?.grade ?: "ثانوي",
                                    phone = stPhone,
                                    parentPhone = pPhone,
                                    joinDate = today
                                )
                            }
                            onImport(newStudents)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("استيراد الآن", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء")
                    }
                }
            }
        }
    }
}

// ─── Batch Printable Cards Dialog ────────────────────────────
@Composable
fun BatchCardsDialog(
    students: List<StudentEntity>,
    groups: List<GroupEntity>,
    onDismiss: () -> Unit
) {
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
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🖨 طباعة كروت الطلاب (${students.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = BlueLight)
                    }
                }

                Text(
                    text = "معاينة كروت الباركود جاهزة للطباعة والتوزيع:",
                    color = BlueLight,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(students) { student ->
                        val group = groups.find { it.id == student.groupId }
                        StudentIdCard(student = student, group = group)
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("تمت المعاينة")
                }
            }
        }
    }
}
