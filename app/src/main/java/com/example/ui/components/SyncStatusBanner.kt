package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@Composable
fun SyncStatusBanner(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val isOnline by viewModel.isOnline.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val pendingCount by viewModel.pendingSyncCount.collectAsState()

    // Show banner when offline, syncing, or pending count > 0
    val showBanner = !isOnline || isSyncing || pendingCount > 0

    AnimatedVisibility(
        visible = showBanner,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    isSyncing -> BluePrimary.copy(alpha = 0.25f)
                    !isOnline -> RedDanger.copy(alpha = 0.25f)
                    pendingCount > 0 -> GoldAccent.copy(alpha = 0.25f)
                    else -> GreenSuccess.copy(alpha = 0.25f)
                }
            ),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(
                    when {
                        isSyncing -> BlueLight
                        !isOnline -> RedDanger
                        pendingCount > 0 -> GoldAccent
                        else -> GreenSuccess
                    }
                )
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = when {
                            isSyncing -> Icons.Default.CloudSync
                            !isOnline -> Icons.Default.CloudOff
                            else -> Icons.Default.CloudDone
                        },
                        contentDescription = null,
                        tint = when {
                            isSyncing -> BlueLight
                            !isOnline -> RedDanger
                            pendingCount > 0 -> GoldAccent
                            else -> GreenSuccess
                        },
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = when {
                                isSyncing -> "جارِ مزامنة السجلات مع السحابة (Firestore)..."
                                !isOnline -> "وضع بدون إنترنت — يتم الحفظ في Room محلياً"
                                pendingCount > 0 -> "يوجد $pendingCount سجل معلق للمزامنة التلقائية"
                                else -> "متزامن سحابياً بالكامل"
                            },
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (!isOnline && pendingCount > 0) {
                            Text(
                                text = "$pendingCount سجل حضور/مدفوعات سيتم رفعها تلقائياً فور الاتصال",
                                color = GoldAccent,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                if (isOnline && !isSyncing && pendingCount > 0) {
                    Button(
                        onClick = { viewModel.syncNow() },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, tint = NavyDark, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مزامنة الآن", color = NavyDark, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                } else if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = BlueLight,
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}
