package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MilitaryTech
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
import com.example.ui.components.CertificateDialog
import com.example.ui.theme.*

@Composable
fun CertificatesScreen(
    viewModel: MainViewModel
) {
    val students by viewModel.students.collectAsState()
    val groups by viewModel.groups.collectAsState()

    var selectedStudentForCert by remember { mutableStateOf<StudentEntity?>(null) }
    var filterGroupId by remember { mutableStateOf<String?>(null) }

    val filteredStudents = remember(students, filterGroupId) {
        if (filterGroupId == null) students
        else students.filter { it.groupId == filterGroupId }
    }

    Scaffold(
        containerColor = NavyDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
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
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(GoldDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🏆", fontSize = 26.sp)
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "منظومة شهادات التقدير والتحفيز",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "3 نماذج احترافية: شهادة تفوق | شهادة التزام | شهادة ثبات",
                            color = BlueLight,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Group filter chips
            if (groups.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = filterGroupId == null,
                        onClick = { filterGroupId = null },
                        label = { Text("جميع الطلاب (${students.size})", fontSize = 11.sp) }
                    )
                    groups.forEach { grp ->
                        FilterChip(
                            selected = filterGroupId == grp.id,
                            onClick = { filterGroupId = if (filterGroupId == grp.id) null else grp.id },
                            label = { Text(grp.name, fontSize = 11.sp) }
                        )
                    }
                }
            }

            Text("اختر طالباً لإصدار شهادة تقدير له:", color = BlueLight, fontSize = 12.sp)

            if (filteredStudents.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("لا يوجد طلاب", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredStudents) { student ->
                        val grp = groups.find { it.id == student.groupId }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedStudentForCert = student },
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(BluePrimary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(student.name.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(student.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${grp?.name ?: ""} • كود: ${student.code}", color = BlueLight, fontSize = 11.sp)
                                    }
                                }

                                Button(
                                    onClick = { selectedStudentForCert = student },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldDark),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.MilitaryTech, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("إصدار شهادة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedStudentForCert?.let { st ->
        val grp = groups.find { it.id == st.groupId }
        CertificateDialog(
            student = st,
            group = grp,
            onDismiss = { selectedStudentForCert = null }
        )
    }
}
