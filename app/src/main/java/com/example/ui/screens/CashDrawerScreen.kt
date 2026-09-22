package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
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
import com.example.data.model.CashDrawerEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CashDrawerScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val shifts by viewModel.cashDrawer.collectAsState()
    val payments by viewModel.payments.collectAsState()

    val todayDate = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    val todayPaymentsTotal = remember(payments, todayDate) {
        payments.filter { it.date == todayDate }.sumOf { it.amount }
    }

    var openingBalanceText by remember { mutableStateOf("0") }
    var physicalCountText by remember { mutableStateOf("") }
    var shiftNotes by remember { mutableStateOf("") }

    val openingBalance = openingBalanceText.toDoubleOrNull() ?: 0.0
    val systemTotal = openingBalance + todayPaymentsTotal
    val physicalCount = physicalCountText.toDoubleOrNull() ?: 0.0
    val diff = physicalCount - systemTotal

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
            // Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(GoldAccent))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "إدارة درج النقدية وتقفيل الوردية",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "تسوية ومطابقة النقدية الفعلية مع تحصيلات النظام",
                            color = BlueLight,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Calculations Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyBorder))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "تفاصيل الوردية الحالية ($todayDate)",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    OutlinedTextField(
                        value = openingBalanceText,
                        onValueChange = { openingBalanceText = it },
                        label = { Text("الرصيد الافتتاحي للدرج (ج.م)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(NavyCard)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("إجمالي تحصيلات اليوم (النظام):", color = BlueLight, fontSize = 12.sp)
                        Text("${todayPaymentsTotal.toInt()} ج.م", color = GreenLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(NavyCard)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("الإجمالي المتوقع بالدرج:", color = BlueLight, fontSize = 12.sp)
                        Text("${systemTotal.toInt()} ج.م", color = GoldAccent, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }

                    OutlinedTextField(
                        value = physicalCountText,
                        onValueChange = { physicalCountText = it },
                        label = { Text("المبلغ الفعلي الموجود بالدرج (ج.م) *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Discrepancy Status Badge
                    if (physicalCountText.isNotBlank()) {
                        val badgeColor = when {
                            diff == 0.0 -> GreenSuccess
                            diff > 0 -> Color(0xFF2E7D32)
                            else -> RedDanger
                        }
                        val statusText = when {
                            diff == 0.0 -> "✅ متطابق تماماً بدون عجز أو زيادة"
                            diff > 0 -> "🟢 يوجد فائض بالدرج قدره: +${diff.toInt()} ج.م"
                            else -> "🔴 يوجد عجز بالدرج قدره: ${diff.toInt()} ج.م"
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(badgeColor.copy(alpha = 0.2f))
                                .border(1.dp, badgeColor, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = statusText,
                                color = badgeColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    OutlinedTextField(
                        value = shiftNotes,
                        onValueChange = { shiftNotes = it },
                        label = { Text("ملاحظات إغلاق الوردية (اختياري)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (physicalCountText.isNotBlank()) {
                                viewModel.closeCashShift(
                                    openingBalance = openingBalance,
                                    systemTotal = systemTotal,
                                    physicalCount = physicalCount,
                                    notes = shiftNotes
                                )
                                Toast.makeText(context, "تم إغلاق الوردية وتسجيلها بنجاح ✅", Toast.LENGTH_SHORT).show()
                                physicalCountText = ""
                                shiftNotes = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldDark),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🔒 اعتماد وإغلاق الوردية", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Past Closed Shifts Log
            Text(
                text = "سجل الورديات السابقة (${shifts.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = BlueLight
            )

            if (shifts.isEmpty()) {
                Text("لا توجد ورديات مغلقة سابقة", color = Color.Gray, fontSize = 12.sp)
            } else {
                shifts.forEach { shift ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
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
                            Column {
                                Text(text = "وردية: ${shift.date}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                if (!shift.note.isNullOrBlank()) {
                                    Text(text = shift.note, color = Color.Gray, fontSize = 10.sp)
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "الفعلي: ${shift.physicalCash.toInt()} ج", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                val diffVal = shift.physicalCash - shift.systemCash
                                val diffText = when {
                                    diffVal == 0.0 -> "متطابق"
                                    diffVal > 0 -> "+${diffVal.toInt()} ج"
                                    else -> "${diffVal.toInt()} ج"
                                }
                                val diffColor = if (diffVal >= 0) GreenLight else RedDanger
                                Text(text = "الفارق: $diffText", color = diffColor, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
