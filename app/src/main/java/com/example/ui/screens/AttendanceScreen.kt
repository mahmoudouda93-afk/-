package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
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
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.components.CameraBarcodeScannerModal
import com.example.ui.components.ScannerMode
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    viewModel: MainViewModel,
    onNavigateToStudent: (String) -> Unit
) {
    val groups by viewModel.groups.collectAsState()
    val students by viewModel.students.collectAsState()
    val attendance by viewModel.attendance.collectAsState()

    var selectedGroupId by remember { mutableStateOf<String?>(null) }
    var selectedDate by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    }

    var showBarcodeScanner by remember { mutableStateOf(false) }
    var showExcuseForStudent by remember { mutableStateOf<StudentEntity?>(null) }

    // 5-second Undo notification
    var undoRecord by remember { mutableStateOf<Pair<String, String>?>(null) } // studentId to previous status
    val coroutineScope = rememberCoroutineScope()

    val currentGroup = groups.find { it.id == selectedGroupId } ?: groups.firstOrNull()
    val groupStudents = remember(students, currentGroup) {
        if (currentGroup != null) students.filter { it.groupId == currentGroup.id }
        else students
    }

    val todayAttendance = remember(attendance, currentGroup, selectedDate) {
        attendance.filter { att ->
            att.date == selectedDate && (currentGroup == null || att.groupId == currentGroup.id)
        }
    }

    val presentCount = todayAttendance.count { it.status == "present" }
    val lateCount = todayAttendance.count { it.status == "late" }
    val absentCount = todayAttendance.count { it.status == "absent" }
    val excusedCount = todayAttendance.count { it.status == "excused" }

    Scaffold(
        containerColor = NavyDark,
        snackbarHost = {
            if (undoRecord != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BluePrimary))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("تم تسجيل الحالة بنجاح", color = Color.White, fontSize = 12.sp)
                        TextButton(
                            onClick = {
                                undoRecord?.let { (stId, oldStatus) ->
                                    if (currentGroup != null) {
                                        viewModel.markAttendance(stId, currentGroup.id, selectedDate, oldStatus)
                                    }
                                }
                                undoRecord = null
                            }
                        ) {
                            Text("تراجع (Undo)", color = GoldAccent, fontWeight = FontWeight.Bold)
                        }
                    }
                }
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
            // Live offline sync status banner
            com.example.ui.components.SyncStatusBanner(viewModel = viewModel)

            // Header with Barcode scanner button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "سجل الحضور والغياب الذكي",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                Button(
                    onClick = { showBarcodeScanner = true },
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("📷 مسح الباركود", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Group Selector Chips
            if (groups.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    groups.forEach { grp ->
                        val isSelected = (currentGroup?.id == grp.id)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedGroupId = grp.id },
                            label = { Text(grp.name, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BluePrimary,
                                selectedLabelColor = Color.White,
                                labelColor = BlueLight
                            )
                        )
                    }
                }
            }

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ChipCount("حاضر: $presentCount", GreenLight, Modifier.weight(1f))
                ChipCount("متأخر: $lateCount", OrangeWarning, Modifier.weight(1f))
                ChipCount("غائب: $absentCount", RedDanger, Modifier.weight(1f))
                ChipCount("اعتذر: $excusedCount", PurpleAccent, Modifier.weight(1f))
            }

            // Student attendance list
            if (groupStudents.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("لا يوجد طلاب في هذه المجموعة", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(groupStudents, key = { it.id }) { student ->
                        val attRecord = todayAttendance.find { it.studentId == student.id }
                        val currentStatus = attRecord?.status

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = student.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.clickable { onNavigateToStudent(student.id) }
                                    )
                                    Text(
                                        text = student.code,
                                        color = BlueLight,
                                        fontSize = 11.sp
                                    )
                                    if (attRecord?.excuseReason?.isNotBlank() == true) {
                                        Text(
                                            text = "عذر: ${attRecord.excuseReason}",
                                            color = PurpleAccent,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    StatusIconButton("✅", currentStatus == "present", GreenSuccess) {
                                        currentGroup?.let {
                                            undoRecord = student.id to (currentStatus ?: "")
                                            viewModel.markAttendance(student.id, it.id, selectedDate, "present")
                                            coroutineScope.launch {
                                                delay(5000)
                                                undoRecord = null
                                            }
                                        }
                                    }

                                    StatusIconButton("⏰", currentStatus == "late", OrangeWarning) {
                                        currentGroup?.let {
                                            undoRecord = student.id to (currentStatus ?: "")
                                            viewModel.markAttendance(student.id, it.id, selectedDate, "late")
                                            coroutineScope.launch {
                                                delay(5000)
                                                undoRecord = null
                                            }
                                        }
                                    }

                                    StatusIconButton("❌", currentStatus == "absent", RedDanger) {
                                        currentGroup?.let {
                                            undoRecord = student.id to (currentStatus ?: "")
                                            viewModel.markAttendance(student.id, it.id, selectedDate, "absent")
                                            coroutineScope.launch {
                                                delay(5000)
                                                undoRecord = null
                                            }
                                        }
                                    }

                                    StatusIconButton("💙", currentStatus == "excused", PurpleAccent) {
                                        showExcuseForStudent = student
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // CameraX Barcode scanner
    if (showBarcodeScanner && currentGroup != null) {
        CameraBarcodeScannerModal(
            mode = ScannerMode.ATTENDANCE,
            students = groupStudents,
            groups = groups,
            onDismiss = { showBarcodeScanner = false },
            onStudentScanned = { found, _ ->
                viewModel.markAttendance(found.id, currentGroup.id, selectedDate, "present")
                showBarcodeScanner = false
            }
        )
    }

    // Excuse modal
    showExcuseForStudent?.let { st ->
        currentGroup?.let { grp ->
            ExcuseReasonDialog(
                student = st,
                onDismiss = { showExcuseForStudent = null },
                onSubmit = { reason, makeupDate ->
                    viewModel.markAttendance(
                        studentId = st.id,
                        groupId = grp.id,
                        date = selectedDate,
                        status = "excused",
                        excuseReason = reason,
                        makeupDate = makeupDate
                    )
                    showExcuseForStudent = null
                }
            )
        }
    }
}
