package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.GroupEntity
import com.example.data.model.StudentEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

enum class CertificateTemplate(
    val title: String,
    val description: String,
    val primaryColor: Color,
    val borderColor: Color,
    val icon: String
) {
    EXCELLENCE(
        title = "شهادة تفوق",
        description = "تقديراً للتفوق الأكاديمي والتحصيل العلمي المتميز في مادة التاريخ",
        primaryColor = Color(0xFFD97706),
        borderColor = Color(0xFFFFD700),
        icon = "🏆"
    ),
    COMMITMENT(
        title = "شهادة التزام",
        description = "تقديراً للالتزام والانضباط وحضور الحصص بانتظام وجدية",
        primaryColor = Color(0xFF16A34A),
        borderColor = Color(0xFF4ADE80),
        icon = "⭐"
    ),
    STABILITY(
        title = "شهادة ثبات واستقرار",
        description = "تقديراً للحفاظ على المستوى والتحصيل الثابت والمثابرة الدائمة",
        primaryColor = Color(0xFF1D4ED8),
        borderColor = Color(0xFF60A5FA),
        icon = "💎"
    )
}

@Composable
fun CertificateDialog(
    student: StudentEntity,
    group: GroupEntity?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTemplate by remember { mutableStateOf(CertificateTemplate.EXCELLENCE) }
    var customNote by remember { mutableStateOf("") }
    val today = remember {
        SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())
    }

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
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🏆 مولد شهادات التقدير",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = BlueLight)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Template Selector Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CertificateTemplate.values().forEach { template ->
                        val isSelected = selectedTemplate == template
                        Button(
                            onClick = { selectedTemplate = template },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) template.primaryColor else NavyCard
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${template.icon} ${template.title}",
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Certificate Visual Sheet
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFFDF5))
                        .border(3.dp, selectedTemplate.borderColor, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "سنتر التفوق التعليمي",
                            color = Color(0xFF666666),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "مستر محمود عوده — مادة التاريخ",
                            color = Color(0xFF1565C0),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = selectedTemplate.icon,
                            fontSize = 32.sp
                        )
                        Text(
                            text = selectedTemplate.title,
                            color = selectedTemplate.primaryColor,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "تُمنح هذه الشهادة بكل فخر واعتزاز للطالب / الطالبة:",
                            color = Color(0xFF444444),
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = student.name,
                            color = Color(0xFF111111),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "${group?.name ?: student.grade}",
                            color = selectedTemplate.primaryColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = selectedTemplate.description,
                            color = Color(0xFF555555),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 15.sp
                        )

                        if (customNote.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "«$customNote»",
                                color = Color(0xFF222222),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(text = "تاريخ التحرير: $today", color = Color(0xFF888888), fontSize = 9.sp)
                                Text(text = "كود الطالب: ${student.code}", color = Color(0xFF888888), fontSize = 9.sp)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "توقيع المعلم",
                                    color = Color(0xFF888888),
                                    fontSize = 9.sp
                                )
                                Text(
                                    text = "مستر محمود عوده",
                                    color = Color(0xFF111111),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom Note Field
                OutlinedTextField(
                    value = customNote,
                    onValueChange = { customNote = it },
                    label = { Text("إضافة ملاحظة أو إشادة خاصة (اختياري)") },
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

                // PDF Export & Direct Share Button
                Button(
                    onClick = {
                        val certKey = when (selectedTemplate) {
                            CertificateTemplate.EXCELLENCE -> "excellence"
                            CertificateTemplate.COMMITMENT -> "commitment"
                            CertificateTemplate.STABILITY -> "stability"
                        }
                        com.example.util.PdfCertificateGenerator.generateAndShareCertificatePdf(
                            context = context,
                            student = student,
                            group = group,
                            certType = certKey,
                            customPraise = customNote
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("توليد ومشاركة ملف PDF (شهادة رسمية) 📄", fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // WhatsApp Share Button
                Button(
                    onClick = {
                        val msg = "🏆 *${selectedTemplate.title}*\n" +
                                "━━━━━━━━━━━━━━━━\n" +
                                "🎓 تُمنح هذه الشهادة بكل فخر للطالب:\n" +
                                "*${student.name}*\n" +
                                "📚 المجموعة: ${group?.name ?: student.grade}\n" +
                                "━━━━━━━━━━━━━━━━\n" +
                                "✨ ${selectedTemplate.description}\n" +
                                (if (customNote.isNotBlank()) "📝 «$customNote»\n" else "") +
                                "📅 تاريخ التحرير: $today\n" +
                                "━━━━━━━━━━━━━━━━\n" +
                                "👨‍🏫 مستر محمود عوده — أستاذ التاريخ"
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://api.whatsapp.com/send?text=" + Uri.encode(msg))
                        }
                        context.startActivity(Intent.createChooser(intent, "إرسال الشهادة عبر"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("مشاركة الشهادة عبر واتساب", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
