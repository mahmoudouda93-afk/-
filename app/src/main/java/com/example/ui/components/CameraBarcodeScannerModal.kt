package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.model.GroupEntity
import com.example.data.model.StudentEntity
import com.example.ui.theme.*
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

enum class ScannerMode {
    ATTENDANCE,
    PAYMENT,
    LOOKUP
}

@Composable
fun CameraBarcodeScannerModal(
    mode: ScannerMode = ScannerMode.ATTENDANCE,
    students: List<StudentEntity>,
    groups: List<GroupEntity>,
    onDismiss: () -> Unit,
    onStudentScanned: (student: StudentEntity, mode: ScannerMode) -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    var manualCodeInput by remember { mutableStateOf("") }
    var lastScannedCode by remember { mutableStateOf<String?>(null) }
    var matchedStudent by remember { mutableStateOf<StudentEntity?>(null) }
    var scanSuccessMessage by remember { mutableStateOf<String?>(null) }

    fun playBeepAndVibrate() {
        try {
            val toneG = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            toneG.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        } catch (_: Exception) {}

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                v?.vibrate(100)
            }
        } catch (_: Exception) {}
    }

    fun processBarcodeResult(rawCode: String) {
        val clean = rawCode.trim()
        if (clean.isBlank() || clean == lastScannedCode) return

        lastScannedCode = clean
        val found = students.find {
            it.code.equals(clean, ignoreCase = true) ||
            it.phone == clean ||
            it.id == clean
        }

        if (found != null) {
            matchedStudent = found
            playBeepAndVibrate()
            scanSuccessMessage = "تم العثور على: ${found.name}"
            onStudentScanned(found, mode)
        } else {
            scanSuccessMessage = "كود غير مسجل: $clean"
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NavyDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(GoldAccent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (mode) {
                                    ScannerMode.ATTENDANCE -> Icons.Default.QrCodeScanner
                                    ScannerMode.PAYMENT -> Icons.Default.Payments
                                    ScannerMode.LOOKUP -> Icons.Default.PersonSearch
                                },
                                contentDescription = null,
                                tint = GoldAccent
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = when (mode) {
                                    ScannerMode.ATTENDANCE -> "مسح باركود الحضور 📷"
                                    ScannerMode.PAYMENT -> "مسح باركود لتحصيل الدفع 💳"
                                    ScannerMode.LOOKUP -> "مسح بطاقة الطالب 🔍"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "وجه الكاميرا نحو باركود كارنيه الطالب لتسجيله فورياً",
                                fontSize = 11.sp,
                                color = BlueLight
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                    }
                }

                // Camera Viewport or Permission Request
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                        .border(2.dp, BlueLight.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasCameraPermission) {
                        CameraPreviewWithBarcodeScanner(
                            onBarcodeDetected = { code ->
                                processBarcodeResult(code)
                            }
                        )

                        // Reticle Overlay
                        ScannerOverlayReticle()
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(48.dp))
                            Text(
                                text = "مطلوب إذن الكاميرا لمسح الباركود",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Button(
                                onClick = { launcher.launch(Manifest.permission.CAMERA) },
                                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                            ) {
                                Text("منح إذن الكاميرا")
                            }
                        }
                    }
                }

                // Scanned Student Feedback Card
                if (matchedStudent != null) {
                    val st = matchedStudent!!
                    val grp = groups.find { it.id == st.groupId }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = NavySurface),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(GreenLight))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "✅ ${st.name}",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "كود: ${st.code} • ${grp?.name ?: st.grade}",
                                    color = GoldAccent,
                                    fontSize = 11.sp
                                )
                            }

                            Button(
                                onClick = {
                                    onStudentScanned(st, mode)
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("تأكيد العملية", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                } else if (scanSuccessMessage != null) {
                    Text(
                        text = scanSuccessMessage!!,
                        color = GoldAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                // Manual Input Fallback
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = manualCodeInput,
                        onValueChange = { manualCodeInput = it },
                        placeholder = { Text("أو اكتب كود الطالب يدوياً...", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = NavyBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Button(
                        onClick = {
                            if (manualCodeInput.isNotBlank()) {
                                processBarcodeResult(manualCodeInput)
                                manualCodeInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("بحث", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraPreviewWithBarcodeScanner(
    onBarcodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember { BarcodeScanning.getClient() }

    var cameraControl by remember { mutableStateOf<Camera?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )
                            barcodeScanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    for (barcode in barcodes) {
                                        val rawValue = barcode.rawValue
                                        if (!rawValue.isNullOrBlank()) {
                                            onBarcodeDetected(rawValue)
                                            break
                                        }
                                    }
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraControl = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Flash Torch Toggle Button
        IconButton(
            onClick = {
                isTorchOn = !isTorchOn
                cameraControl?.cameraControl?.enableTorch(isTorchOn)
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(
                imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                contentDescription = "كشاف",
                tint = if (isTorchOn) GoldAccent else Color.White
            )
        }
    }
}

@Composable
private fun ScannerOverlayReticle() {
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserYRatio by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserY"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val boxWidth = w * 0.75f
        val boxHeight = h * 0.5f
        val left = (w - boxWidth) / 2f
        val top = (h - boxHeight) / 2f
        val right = left + boxWidth
        val bottom = top + boxHeight

        // Semi-transparent darkened background around reticle
        val darkMaskColor = Color.Black.copy(alpha = 0.5f)
        drawRect(darkMaskColor, topLeft = Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(w, top))
        drawRect(darkMaskColor, topLeft = Offset(0f, bottom), size = androidx.compose.ui.geometry.Size(w, h - bottom))
        drawRect(darkMaskColor, topLeft = Offset(0f, top), size = androidx.compose.ui.geometry.Size(left, boxHeight))
        drawRect(darkMaskColor, topLeft = Offset(right, top), size = androidx.compose.ui.geometry.Size(w - right, boxHeight))

        // Gold Corners of reticle
        val cornerLen = 32f
        val strokeW = 4f
        val cornerColor = GoldAccent

        // Top Left
        drawLine(cornerColor, Offset(left, top), Offset(left + cornerLen, top), strokeW)
        drawLine(cornerColor, Offset(left, top), Offset(left, top + cornerLen), strokeW)
        // Top Right
        drawLine(cornerColor, Offset(right, top), Offset(right - cornerLen, top), strokeW)
        drawLine(cornerColor, Offset(right, top), Offset(right, top + cornerLen), strokeW)
        // Bottom Left
        drawLine(cornerColor, Offset(left, bottom), Offset(left + cornerLen, bottom), strokeW)
        drawLine(cornerColor, Offset(left, bottom), Offset(left, bottom - cornerLen), strokeW)
        // Bottom Right
        drawLine(cornerColor, Offset(right, bottom), Offset(right - cornerLen, bottom), strokeW)
        drawLine(cornerColor, Offset(right, bottom), Offset(right, bottom - cornerLen), strokeW)

        // Animated Red Laser Line
        val currentLaserY = top + (boxHeight * laserYRatio)
        drawLine(
            color = RedDanger,
            start = Offset(left + 8f, currentLaserY),
            end = Offset(right - 8f, currentLaserY),
            strokeWidth = 2.5f
        )
    }
}
