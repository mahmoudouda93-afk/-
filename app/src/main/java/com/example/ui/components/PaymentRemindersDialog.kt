package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.GroupEntity
import com.example.data.model.PaymentEntity
import com.example.data.model.StudentEntity
import com.example.ui.theme.*
import java.net.URLEncoder
import java.util.*

data class DueStudentInfo(
    val student: StudentEntity,
    val group: GroupEntity?,
    val currentMonth: Int,
    val monthName: String,
    val amountDue: Double,
    val isOverdue: Boolean
)

@Composable
fun PaymentRemindersDialog(
    students: List<StudentEntity>,
    groups: List<GroupEntity>,
    payments: List<PaymentEntity>,
    onDismiss: () -> Unit,
    onQuickPay: (student: StudentEntity) -> Unit
) {
    val context = LocalContext.current
    val currentCal = remember { Calendar.getInstance() }
    val currentMonth = remember { currentCal.get(Calendar.MONTH) + 1 } // 1-12

    val monthNames = listOf(
        "", "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )
    val currentMonthName = monthNames.getOrElse(currentMonth) { "الحالي" }

    // Calculate due students
    val dueStudents = remember(students, groups, payments, currentMonth) {
        students.mapNotNull { student ->
            val grp = groups.find { it.id == student.groupId }
            val hasPaidCurrentMonth = payments.any {
                it.studentId == student.id && it.targetMonth == currentMonth
            }

            if (!hasPaidCurrentMonth) {
                val groupPrice = grp?.price ?: 200.0
                val finalPrice = when (student.discountType) {
                    "free" -> 0.0
                    "percentage" -> (groupPrice * (1.0 - (student.discountValue / 100.0))).coerceAtLeast(0.0)
                    "fixed" -> (groupPrice - student.discountValue).coerceAtLeast(0.0)
                    else -> groupPrice
                }

                // If not completely free, student owes
                if (finalPrice > 0) {
                    val prevMonthUnpaid = currentMonth > 1 && payments.none {
                        it.studentId == student.id && it.targetMonth == currentMonth - 1
                    }

                    DueStudentInfo(
                        student = student,
                        group = grp,
                        currentMonth = currentMonth,
                        monthName = currentMonthName,
                        amountDue = finalPrice,
                        isOverdue = prevMonthUnpaid || currentCal.get(Calendar.DAY_OF_MONTH) > 10
                    )
                } else null
            } else null
        }
    }

    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, OVERDUE, THIS_MONTH
    val filteredList = remember(dueStudents, selectedFilter) {
        when (selectedFilter) {
            "OVERDUE" -> dueStudents.filter { it.isOverdue }
            "THIS_MONTH" -> dueStudents.filter { !it.isOverdue }
            else -> dueStudents
        }
    }

    val totalDueAmount = remember(filteredList) { filteredList.sumOf { it.amountDue } }

    fun sendWhatsAppReminder(info: DueStudentInfo) {
        val phone = info.student.parentPhone.ifBlank { info.student.phone }
        if (phone.isBlank()) {
            Toast.makeText(context, "لا يوجد رقم هاتف مسجل للطالب أو ولي الأمر", Toast.LENGTH_SHORT).show()
            return
        }

        val cleanPhone = if (phone.startsWith("0")) "2$phone" else phone
        val msg = """
السلام عليكم ورحمة الله وبركاته،
ولي أمر الطالب المحترم / ${info.student.name}
نحيط سيادتكم علماً باقتراب/استحقاق سداد اشتراك مادة التاريخ لشهر (${info.monthName}) مع مستر محمود عوده.
المبلغ المستحق: ${info.amountDue.toInt()} ج.م
المجموعة: ${info.group?.name ?: info.student.grade}

شاكرين حسن تعاونكم وحرصكم الدائم على متابعة وتفوق الطالب.
— إدارة مادة التاريخ | مستر محمود عوده
        """.trimIndent()

        try {
            val encodedMsg = URLEncoder.encode(msg, "UTF-8")
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMsg")
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "تطبيق واتساب غير مثبت على الجهاز", Toast.LENGTH_SHORT).show()
        }
    }

    fun callParent(phone: String) {
        if (phone.isBlank()) return
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NavyDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GoldAccent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = GoldAccent)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "تذكيرات وإشعارات المدفوعات 🔔",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "متابعة الطلاب غير المسددين لشهر $currentMonthName وإرسال رسائل واتساب",
                                fontSize = 11.sp,
                                color = BlueLight
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                    }
                }

                // Summary Stats Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = NavySurface),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(RedDanger))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("الطلاب غير المسددين", color = BlueLight, fontSize = 10.sp)
                            Text("${dueStudents.size} طالب", color = RedDanger, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = NavySurface),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(GoldAccent))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("إجمالي المبالغ المطلوبة", color = BlueLight, fontSize = 10.sp)
                            Text("${totalDueAmount.toInt()} ج.م", color = GoldAccent, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }
                    }
                }

                // Filter Chips
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("الكل (${dueStudents.size})", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedFilter == "OVERDUE",
                        onClick = { selectedFilter = "OVERDUE" },
                        label = { Text("متأخر بشدة (${dueStudents.count { it.isOverdue }})", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedFilter == "THIS_MONTH",
                        onClick = { selectedFilter = "THIS_MONTH" },
                        label = { Text("مستحق حالياً (${dueStudents.count { !it.isOverdue }})", fontSize = 11.sp) }
                    )
                }

                // Students List
                if (filteredList.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("🎉 ممتاز! لا يوجد طلاب متأخرين عن السداد في هذه الفئة", color = GreenLight, fontWeight = FontWeight.Bold)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredList, key = { it.student.id }) { item ->
                            val st = item.student
                            val statusColor = if (item.isOverdue) RedDanger else GoldAccent

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
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = st.name,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 13.sp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(statusColor.copy(alpha = 0.2f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (item.isOverdue) "متأخر" else "مستحق",
                                                    color = statusColor,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Text(
                                            text = "كود: ${st.code} • ${item.group?.name ?: st.grade}",
                                            color = BlueLight,
                                            fontSize = 10.sp
                                        )

                                        Text(
                                            text = "المبلغ المطلوب: ${item.amountDue.toInt()} ج.م (شهر ${item.monthName})",
                                            color = GoldAccent,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }

                                    // Action buttons
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        // WhatsApp Reminder Button
                                        IconButton(
                                            onClick = { sendWhatsAppReminder(item) },
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF25D366))
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.Send,
                                                contentDescription = "واتساب",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        // Call Phone Button
                                        val parentPhone = st.parentPhone.ifBlank { st.phone }
                                        if (parentPhone.isNotBlank()) {
                                            IconButton(
                                                onClick = { callParent(parentPhone) },
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(BluePrimary)
                                            ) {
                                                Icon(
                                                    Icons.Default.Phone,
                                                    contentDescription = "اتصال",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        // Quick Pay Button
                                        IconButton(
                                            onClick = {
                                                onQuickPay(st)
                                                onDismiss()
                                            },
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(GreenSuccess)
                                        ) {
                                            Icon(
                                                Icons.Default.AddCard,
                                                contentDescription = "تحصيل فوري",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
