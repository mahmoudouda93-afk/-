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
import com.example.data.model.PaymentEntity
import com.example.data.model.StudentEntity
import com.example.ui.MainViewModel
import com.example.ui.components.CameraBarcodeScannerModal
import com.example.ui.components.ComprehensiveAddPaymentDialog
import com.example.ui.components.PaymentRemindersDialog
import com.example.ui.components.ScannerMode
import com.example.ui.components.ThermalReceiptDialog
import com.example.ui.theme.*

@Composable
fun PaymentsScreen(
    viewModel: MainViewModel,
    onNavigateToStudent: (String) -> Unit
) {
    val payments by viewModel.payments.collectAsState()
    val students by viewModel.students.collectAsState()
    val groups by viewModel.groups.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showNoStudentsAlert by remember { mutableStateOf(false) }
    var showCameraScanner by remember { mutableStateOf(false) }
    var showRemindersDialog by remember { mutableStateOf(false) }
    var preselectedStudentForPay by remember { mutableStateOf<StudentEntity?>(null) }
    var viewingReceiptPayment by remember { mutableStateOf<PaymentEntity?>(null) }
    var paymentToDelete by remember { mutableStateOf<PaymentEntity?>(null) }

    var selectedMonthFilter by remember { mutableStateOf<Int?>(null) }
    var selectedGroupIdFilter by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredPayments = remember(payments, students, groups, selectedMonthFilter, selectedGroupIdFilter, searchQuery) {
        payments.filter { pay ->
            val st = students.find { it.id == pay.studentId }
            val grp = groups.find { it.id == pay.groupId }
            val matchesMonth = selectedMonthFilter == null || pay.targetMonth == selectedMonthFilter
            val matchesGroup = selectedGroupIdFilter == null || pay.groupId == selectedGroupIdFilter
            val matchesSearch = searchQuery.isBlank() ||
                    (st?.name?.contains(searchQuery.trim(), ignoreCase = true) == true) ||
                    (st?.code?.contains(searchQuery.trim(), ignoreCase = true) == true) ||
                    (pay.receiptNumber.contains(searchQuery.trim(), ignoreCase = true)) ||
                    (grp?.name?.contains(searchQuery.trim(), ignoreCase = true) == true)

            matchesMonth && matchesGroup && matchesSearch
        }
    }

    val totalAmount = filteredPayments.sumOf { it.amount }

    Scaffold(
        containerColor = NavyDark,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (students.isEmpty()) {
                        showNoStudentsAlert = true
                    } else {
                        preselectedStudentForPay = null
                        showAddDialog = true
                    }
                },
                containerColor = GreenSuccess,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "تسجيل دفع")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Live offline sync status banner
            com.example.ui.components.SyncStatusBanner(viewModel = viewModel)

            // Header with Total Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(GoldAccent))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "سجل المدفوعات والإيصالات", color = BlueLight, fontSize = 12.sp)
                        Text(
                            text = "${totalAmount.toInt()} ج.م",
                            color = GoldAccent,
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp
                        )
                        Text(text = "إجمالي العمليات: ${filteredPayments.size}", color = Color.Gray, fontSize = 11.sp)
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Scan Barcode Button
                        IconButton(
                            onClick = { showCameraScanner = true },
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(BluePrimary)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "مسح باركود", tint = Color.White)
                        }

                        // Payment Reminders Button
                        IconButton(
                            onClick = { showRemindersDialog = true },
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(GoldAccent.copy(alpha = 0.25f))
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = "تنبيهات الاستحقاق", tint = GoldAccent)
                        }

                        // Add Payment Button
                        Button(
                            onClick = {
                                if (students.isEmpty()) {
                                    showNoStudentsAlert = true
                                } else {
                                    preselectedStudentForPay = null
                                    showAddDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تسجيل دفع", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("بحث باسم الطالب، الكود، أو رقم الإيصال...", fontSize = 11.sp, color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                },
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

            // Group Filter Chips (if groups available)
            if (groups.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = selectedGroupIdFilter == null,
                        onClick = { selectedGroupIdFilter = null },
                        label = { Text("جميع المجموعات", fontSize = 10.sp) }
                    )
                    groups.forEach { grp ->
                        FilterChip(
                            selected = selectedGroupIdFilter == grp.id,
                            onClick = {
                                selectedGroupIdFilter = if (selectedGroupIdFilter == grp.id) null else grp.id
                            },
                            label = { Text(grp.name, fontSize = 10.sp) }
                        )
                    }
                }
            }

            // Month Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = selectedMonthFilter == null,
                    onClick = { selectedMonthFilter = null },
                    label = { Text("كل الشهور", fontSize = 10.sp) }
                )
                listOf(1, 2, 3, 4, 5, 9, 10, 11, 12).forEach { m ->
                    FilterChip(
                        selected = selectedMonthFilter == m,
                        onClick = { selectedMonthFilter = if (selectedMonthFilter == m) null else m },
                        label = { Text("ش$m", fontSize = 10.sp) }
                    )
                }
            }

            // Payments List
            if (filteredPayments.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🧾", fontSize = 42.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("لا توجد عمليات دفع مطابقة", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredPayments, key = { it.id }) { pay ->
                        val student = students.find { it.id == pay.studentId }
                        val group = groups.find { it.id == pay.groupId }

                        val typeLabel = when (pay.type) {
                            "monthly" -> "اشتراك شهر"
                            "half_monthly" -> "اشتراك نصف شهر"
                            "explanation_notes" -> "مذكرة شرح"
                            "review_notes" -> "مذكرة مراجعة"
                            else -> pay.otherDescription ?: "أخرى"
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewingReceiptPayment = pay },
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
                                        text = student?.name ?: "طالب غير مسجل",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "${group?.name ?: ""} • $typeLabel (شهر ${pay.targetMonth})",
                                        color = BlueLight,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "إيصال: ${pay.receiptNumber} • ${pay.date}",
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${pay.amount.toInt()} ج.م",
                                        color = GreenLight,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp
                                    )

                                    IconButton(
                                        onClick = { viewingReceiptPayment = pay },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ReceiptLong, contentDescription = "عرض الإيصال", tint = GoldAccent, modifier = Modifier.size(20.dp))
                                    }

                                    IconButton(
                                        onClick = { paymentToDelete = pay },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف الإيصال", tint = RedDanger, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Comprehensive Add Payment Dialog
    if (showAddDialog && students.isNotEmpty()) {
        ComprehensiveAddPaymentDialog(
            groups = groups,
            students = students,
            initialStudentId = preselectedStudentForPay?.id,
            onDismiss = {
                showAddDialog = false
                preselectedStudentForPay = null
            },
            onSave = { stId, grpId, amount, type, desc, targetMonth ->
                viewModel.addPayment(
                    studentId = stId,
                    groupId = grpId,
                    amount = amount,
                    type = type,
                    otherDescription = desc,
                    targetMonth = targetMonth
                ) { createdPayment ->
                    showAddDialog = false
                    preselectedStudentForPay = null
                    viewingReceiptPayment = createdPayment
                }
            }
        )
    }

    // No Students Alert
    if (showNoStudentsAlert) {
        AlertDialog(
            onDismissRequest = { showNoStudentsAlert = false },
            title = { Text("تنبيه", fontWeight = FontWeight.Bold, color = Color.White) },
            text = { Text("لا يوجد طلاب مسجلون بعد. يرجى إضافة طلاب أولاً لتسجيل المدفوعات.", color = BlueLight) },
            confirmButton = {
                Button(onClick = { showNoStudentsAlert = false }) {
                    Text("حسناً")
                }
            },
            containerColor = NavySurface
        )
    }

    // Delete Payment Confirmation Dialog
    paymentToDelete?.let { pay ->
        val st = students.find { it.id == pay.studentId }
        AlertDialog(
            onDismissRequest = { paymentToDelete = null },
            title = { Text("إلغاء / حذف الإيصال", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Text(
                    text = "هل أنت متأكد من حذف إيصال السداد رقم ${pay.receiptNumber} الخاص بالطالب \"${st?.name ?: ""}\" بقيمة ${pay.amount.toInt()} ج.م؟",
                    color = BlueLight
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePayment(pay)
                        paymentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedDanger)
                ) {
                    Text("نعم، حذف العملية")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { paymentToDelete = null }) {
                    Text("إلغاء", color = Color.Gray)
                }
            },
            containerColor = NavySurface
        )
    }

    // CameraX Barcode Scanner for Quick Payment
    if (showCameraScanner) {
        CameraBarcodeScannerModal(
            mode = ScannerMode.PAYMENT,
            students = students,
            groups = groups,
            onDismiss = { showCameraScanner = false },
            onStudentScanned = { scannedStudent, _ ->
                preselectedStudentForPay = scannedStudent
                showCameraScanner = false
                showAddDialog = true
            }
        )
    }

    // Payment Reminders & Due Students Dialog
    if (showRemindersDialog) {
        PaymentRemindersDialog(
            students = students,
            groups = groups,
            payments = payments,
            onDismiss = { showRemindersDialog = false },
            onQuickPay = { student ->
                preselectedStudentForPay = student
                showRemindersDialog = false
                showAddDialog = true
            }
        )
    }

    // Thermal Receipt Dialog
    viewingReceiptPayment?.let { pay ->
        val st = students.find { it.id == pay.studentId }
        val grp = groups.find { it.id == pay.groupId }
        ThermalReceiptDialog(
            payment = pay,
            studentName = st?.name ?: "طالب",
            groupName = grp?.name ?: "المجموعة",
            onDismiss = { viewingReceiptPayment = null }
        )
    }
}

