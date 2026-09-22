package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.GroupEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import org.json.JSONArray
import org.json.JSONObject

val DAYS_OF_WEEK = listOf(
    "saturday" to "السبت",
    "sunday" to "الأحد",
    "monday" to "الإثنين",
    "tuesday" to "الثلاثاء",
    "wednesday" to "الأربعاء",
    "thursday" to "الخميس",
    "friday" to "الجمعة"
)

@Composable
fun GroupsScreen(
    viewModel: MainViewModel,
    onNavigateToGroup: (String) -> Unit
) {
    val groups by viewModel.groups.collectAsState()
    val students by viewModel.students.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = NavyDark,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = BluePrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.GroupAdd, contentDescription = "Add Group")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "المجموعات الدراسية (${groups.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("مجموعة جديدة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (groups.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("👥", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("لا توجد مجموعات حتى الآن", color = Color.Gray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { showCreateDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                        ) {
                            Text("إنشاء أول مجموعة")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(groups) { group ->
                        val count = students.count { it.groupId == group.id }
                        GroupCardItem(
                            group = group,
                            studentCount = count,
                            onClick = { onNavigateToGroup(group.id) },
                            onDelete = { viewModel.deleteGroup(group) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateGroupDialog(
            existing = null,
            onDismiss = { showCreateDialog = false },
            onSave = { group ->
                viewModel.addGroup(group)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun GroupCardItem(
    group: GroupEntity,
    studentCount: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyBorder))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.name,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "${group.stage} • ${group.grade} • مادة ${group.subject}",
                        color = BlueLight,
                        fontSize = 11.sp
                    )
                    if (group.location.isNotBlank()) {
                        Text(
                            text = "📍 ${group.location}",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NavyCard)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${group.price.toInt()} ج.م/شهر",
                            color = GoldAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = RedDanger, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Capacity & Schedule summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "👥 الطلاب: $studentCount / ${group.capacity}",
                    color = GreenLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "📅 ${group.sessionsPerMonth} حصص/شهر",
                    color = BlueLight,
                    fontSize = 11.sp
                )
            }
        }
    }
}

// ─── Create / Edit Group Dialog ──────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateGroupDialog(
    existing: GroupEntity?,
    onDismiss: () -> Unit,
    onSave: (GroupEntity) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var stage by remember { mutableStateOf(existing?.stage ?: "ثانوي") }
    var grade by remember { mutableStateOf(existing?.grade ?: GRADES_LIST[3]) }
    var location by remember { mutableStateOf(existing?.location ?: "سنتر التفوق") }
    var capacityText by remember { mutableStateOf(existing?.capacity?.toString() ?: "30") }
    var priceText by remember { mutableStateOf(existing?.price?.toInt()?.toString() ?: "200") }
    var sessionsText by remember { mutableStateOf(existing?.sessionsPerMonth?.toString() ?: "8") }
    var whatsappLink by remember { mutableStateOf(existing?.whatsappLink ?: "") }

    // Dynamic Weekly Schedule state
    var selectedDays by remember {
        mutableStateOf(
            if (existing != null) {
                try {
                    val arr = JSONArray(existing.scheduleJson)
                    (0 until arr.length()).map { i ->
                        arr.getJSONObject(i).getString("day")
                    }.toSet()
                } catch (e: Exception) { setOf("saturday", "tuesday") }
            } else setOf("saturday", "tuesday")
        )
    }

    var fromTime by remember { mutableStateOf("16:00") }
    var toTime by remember { mutableStateOf("18:00") }

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
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (existing == null) "➕ إنشاء مجموعة جديدة" else "✏ تعديل المجموعة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم المجموعة (مثال: مجموعة أبطال التاريخ)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Stage & Grade
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("إعدادي", "ثانوي").forEach { stg ->
                        FilterChip(
                            selected = stage == stg,
                            onClick = { stage = stg },
                            label = { Text(stg, fontSize = 11.sp) }
                        )
                    }
                }

                Text("الصف الدراسي", color = BlueLight, fontSize = 11.sp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    GRADES_LIST.forEach { g ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (grade == g) NavyCard else Color.Transparent)
                                .clickable { grade = g }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            RadioButton(selected = grade == g, onClick = { grade = g })
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(g, color = Color.White, fontSize = 12.sp)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("سعر الاشتراك (ج.م)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = capacityText,
                        onValueChange = { capacityText = it },
                        label = { Text("السعة القصوى") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sessionsText,
                        onValueChange = { sessionsText = it },
                        label = { Text("عدد الحصص شهرياً") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("مكان الحصة / السنتر") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = whatsappLink,
                    onValueChange = { whatsappLink = it },
                    label = { Text("رابط جروب واتساب للطلاب (اختياري)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Dynamic Weekly Schedule Builder
                Text(
                    text = "🗓 المواعيد الأسبوعية المنتظمة (اختر الأيام):",
                    color = BlueLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DAYS_OF_WEEK.forEach { (key, label) ->
                        val isSelected = selectedDays.contains(key)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedDays = if (isSelected) selectedDays - key else selectedDays + key
                            },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = fromTime,
                        onValueChange = { fromTime = it },
                        label = { Text("من الساعة (مثال: 16:00)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = toTime,
                        onValueChange = { toTime = it },
                        label = { Text("إلى الساعة (مثال: 18:00)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                // Build schedule JSON
                                val scheduleArr = JSONArray()
                                selectedDays.forEach { dayKey ->
                                    val obj = JSONObject().apply {
                                        put("day", dayKey)
                                        put("from", fromTime)
                                        put("to", toTime)
                                    }
                                    scheduleArr.put(obj)
                                }

                                val group = (existing ?: GroupEntity(name = name, grade = grade)).copy(
                                    name = name,
                                    stage = stage,
                                    grade = grade,
                                    location = location,
                                    capacity = capacityText.toIntOrNull() ?: 30,
                                    price = priceText.toDoubleOrNull() ?: 200.0,
                                    sessionsPerMonth = sessionsText.toIntOrNull() ?: 8,
                                    whatsappLink = whatsappLink,
                                    scheduleJson = scheduleArr.toString()
                                )
                                onSave(group)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (existing == null) "إنشاء المجموعة" else "حفظ التعديلات", fontWeight = FontWeight.Bold)
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
