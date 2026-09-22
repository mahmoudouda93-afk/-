package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.GroupEntity
import com.example.data.model.PaymentEntity
import com.example.data.model.StudentEntity
import com.example.ui.theme.*

// ─── Thermal Receipt Dialog ──────────────────────────────────
@Composable
fun ThermalReceiptDialog(
    payment: PaymentEntity,
    studentName: String,
    groupName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val paymentTypeLabel = when (payment.type) {
        "monthly" -> "اشتراك شهر"
        "half_monthly" -> "اشتراك نصف شهر"
        "explanation_notes" -> "ثمن مذكرة شرح"
        "review_notes" -> "ثمن مذكرة مراجعة"
        else -> payment.otherDescription ?: "أخرى"
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NavySurface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🧾 إيصال سداد نقدي",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Receipt body styled like 58mm thermal paper
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFAFAFA))
                        .padding(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "سنتر التفوق التعليمي",
                            color = Color(0xFF333333),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "مستر محمود عوده — التاريخ",
                            color = Color(0xFF1565C0),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "هاتف: 01000000000",
                            color = Color(0xFF666666),
                            fontSize = 10.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFFCCCCCC))
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        ReceiptRow(label = "اسم الطالب:", value = studentName)
                        ReceiptRow(label = "المجموعة:", value = groupName)
                        ReceiptRow(label = "الشهر المستهدف:", value = "شهر ${payment.targetMonth}")
                        ReceiptRow(label = "البند:", value = paymentTypeLabel)
                        ReceiptRow(label = "التاريخ:", value = payment.date)
                        ReceiptRow(label = "رقم الإيصال:", value = payment.receiptNumber)

                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFFCCCCCC))
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "المبلغ المسدد:",
                                color = Color(0xFF111111),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${payment.amount.toInt()} ج.م",
                                color = Color(0xFF16A34A),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "شكراً لثقتكم — نتمنى لكم دوام التفوق",
                            color = Color(0xFF888888),
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions: WhatsApp & Dismiss
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val msg = "🧾 *إيصال سداد رسوم*\n" +
                                    "━━━━━━━━━━━━\n" +
                                    "👤 الطالب: $studentName\n" +
                                    "📚 المجموعة: $groupName\n" +
                                    "📅 التاريخ: ${payment.date}\n" +
                                    "📌 البيان: $paymentTypeLabel (شهر ${payment.targetMonth})\n" +
                                    "💰 المبلغ: ${payment.amount.toInt()} ج.م\n" +
                                    "🔢 رقم الإيصال: ${payment.receiptNumber}\n" +
                                    "━━━━━━━━━━━━\n" +
                                    "👨‍🏫 مستر محمود عوده — أستاذ التاريخ"
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://api.whatsapp.com/send?text=" + Uri.encode(msg))
                            }
                            context.startActivity(Intent.createChooser(intent, "إرسال الإيصال عبر"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("واتساب", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BlueLight),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إغلاق", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ReceiptRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color(0xFF666666), fontSize = 11.sp)
        Text(text = value, color = Color(0xFF111111), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ─── Barcode Scanner / Manual Input Dialog ───────────────────
@Composable
fun BarcodeScannerDialog(
    students: List<StudentEntity>,
    onStudentFound: (StudentEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var codeInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun processCode(code: String) {
        val trimmed = code.trim()
        val found = students.find { it.code.equals(trimmed, ignoreCase = true) || it.phone == trimmed }
        if (found != null) {
            errorMessage = null
            onStudentFound(found)
        } else {
            errorMessage = "⚠ كارت غير مسجل: $trimmed"
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NavySurface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📷 مسح كارت الباركود",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = BlueLight)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Simulated Scanner Viewfinder with visual scanning line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF07111E))
                        .border(2.dp, BluePrimary, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "وجّه الكاميرا إلى كود الطالب",
                            color = BlueLight,
                            fontSize = 12.sp
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(RedDanger.copy(alpha = 0.2f))
                            .border(1.dp, RedDanger, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFFF8A80),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Manual Code Input
                OutlinedTextField(
                    value = codeInput,
                    onValueChange = {
                        codeInput = it
                        errorMessage = null
                    },
                    label = { Text("أو أدخل الكود يدوياً (مثال: ST10001)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = BluePrimary,
                        unfocusedBorderColor = NavyBorder
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { processCode(codeInput) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Text("تحقق وتسجيل", fontWeight = FontWeight.Bold)
                }

                // Quick Select from Registered Students List (Demo / Testing convenience)
                if (students.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "أو اختر طالباً سريعاً:",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        students.take(3).forEach { st ->
                            AssistChip(
                                onClick = { processCode(st.code) },
                                label = { Text(st.name.take(8), fontSize = 10.sp) },
                                colors = AssistChipDefaults.assistChipColors(labelColor = BlueLight)
                            )
                        }
                    }
                }
            }
        }
    }
}
