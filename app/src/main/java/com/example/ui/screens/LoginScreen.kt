package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@Composable
fun LoginScreen(
    viewModel: MainViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToParentPortal: () -> Unit
) {
    var selectedRole by remember { mutableStateOf("teacher") } // "teacher" or "assistant"
    var username by remember { mutableStateOf("1") }
    var password by remember { mutableStateOf("1") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(NavyDark, NavySurface)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyBorder)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Logo & Emblem
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(BluePrimary, GoldAccent))
                        )
                        .border(3.dp, GoldAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📚", fontSize = 34.sp)
                }

                Text(
                    text = "المساعد — Al-Musaed",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                Text(
                    text = "تطبيق المساعد لمستر محمود عوده\nمادة التاريخ — إدارة المجموعات والطلاب",
                    color = BlueLight,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Role Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NavyCard)
                        .padding(4.dp)
                ) {
                    Button(
                        onClick = {
                            selectedRole = "teacher"
                            username = "1"
                            password = "1"
                            errorMessage = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedRole == "teacher") BluePrimary else Color.Transparent
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "👨‍🏫 المعلم (مستر محمود)",
                            fontSize = 11.sp,
                            fontWeight = if (selectedRole == "teacher") FontWeight.Bold else FontWeight.Normal,
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = {
                            selectedRole = "assistant"
                            username = "AST01"
                            password = "1234"
                            errorMessage = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedRole == "assistant") BluePrimary else Color.Transparent
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "💼 مساعد المعلم",
                            fontSize = 11.sp,
                            fontWeight = if (selectedRole == "assistant") FontWeight.Bold else FontWeight.Normal,
                            color = Color.White
                        )
                    }
                }

                // Fields
                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        errorMessage = null
                    },
                    label = { Text(if (selectedRole == "teacher") "اسم المستخدم أو رقم الهوية (1)" else "كود المساعد (AST01)") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = BlueLight) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = { Text(if (selectedRole == "teacher") "كلمة المرور (1)" else "الرقم السري (PIN 1234)") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = BlueLight) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = RedDanger,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        val success = viewModel.login(username, password)
                        if (success) {
                            onLoginSuccess()
                        } else {
                            errorMessage = "بيانات الدخول غير صحيحة. للتجربة: مستخدم 1 كلمة مرور 1"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("تسجيل الدخول", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                // Direct Parent Portal Access
                TextButton(
                    onClick = onNavigateToParentPortal,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FamilyRestroom, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("دخول بوابة ولي الأمر (متابعة درجات وحضور الطالب)", color = GoldAccent, fontSize = 12.sp)
                }
            }
        }
    }
}
