package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.GroupEntity
import com.example.data.model.StudentEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfCertificateGenerator {

    fun generateAndShareCertificatePdf(
        context: Context,
        student: StudentEntity,
        group: GroupEntity?,
        certType: String = "excellence", // "excellence", "commitment", "stability"
        customPraise: String = ""
    ): File? {
        val pdfDocument = PdfDocument()
        // Standard A4 landscape dimensions in PostScript points: 842 x 595
        val pageWidth = 842
        val pageHeight = 595
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Background
        val bgPaint = Paint().apply {
            color = Color.parseColor("#0A1628") // Dark Navy
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

        // Outer Gold Border
        val borderPaint = Paint().apply {
            color = Color.parseColor("#FFD700") // Gold
            style = Paint.Style.STROKE
            strokeWidth = 4f
            isAntiAlias = true
        }
        canvas.drawRect(24f, 24f, (pageWidth - 24).toFloat(), (pageHeight - 24).toFloat(), borderPaint)

        // Inner Delicate Border
        val innerBorderPaint = Paint().apply {
            color = Color.parseColor("#B8860B") // Dark Gold
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            isAntiAlias = true
        }
        canvas.drawRect(32f, 32f, (pageWidth - 32).toFloat(), (pageHeight - 32).toFloat(), innerBorderPaint)

        // Decorative Corner Ornaments
        val cornerPaint = Paint().apply {
            color = Color.parseColor("#FFD700")
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            isAntiAlias = true
        }
        drawCornerOrnament(canvas, 32f, 32f, cornerPaint, 1, 1)
        drawCornerOrnament(canvas, (pageWidth - 32).toFloat(), 32f, cornerPaint, -1, 1)
        drawCornerOrnament(canvas, 32f, (pageHeight - 32).toFloat(), cornerPaint, 1, -1)
        drawCornerOrnament(canvas, (pageWidth - 32).toFloat(), (pageHeight - 32).toFloat(), cornerPaint, -1, -1)

        // Header: Teacher Identity
        val headerPaint = Paint().apply {
            color = Color.parseColor("#90CAF9")
            textSize = 14f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("منظومة المساعد الأكاديمية • مادة التاريخ للثانوية العامة", (pageWidth / 2).toFloat(), 65f, headerPaint)

        val teacherTitlePaint = Paint().apply {
            color = Color.parseColor("#FFD700")
            textSize = 20f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("مستر محمود عوده — أستاذ التاريخ", (pageWidth / 2).toFloat(), 95f, teacherTitlePaint)

        // Certificate Type Title
        val certTitle = when (certType) {
            "commitment" -> "شـهـادة الـتـزام وتـمـيـز"
            "stability" -> "شـهـادة ثـبـات واسـتـقـرار"
            else -> "شـهـادة تـفـوق وتـقـدير"
        }

        val certTitlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 28f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(certTitle, (pageWidth / 2).toFloat(), 155f, certTitlePaint)

        // Decorative line under title
        val linePaint = Paint().apply {
            color = Color.parseColor("#FFD700")
            strokeWidth = 2f
            isAntiAlias = true
        }
        canvas.drawLine((pageWidth / 2 - 120).toFloat(), 170f, (pageWidth / 2 + 120).toFloat(), 170f, linePaint)

        // "يشهد مستر محمود عوده بأن الطالب / الطالبة:"
        val certBodyIntroPaint = Paint().apply {
            color = Color.parseColor("#E0E0E0")
            textSize = 15f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("يشهد الأستاذ / محمود عوده بأن الطالب الخلوق المتميز:", (pageWidth / 2).toFloat(), 210f, certBodyIntroPaint)

        // Student Name in large Gold Text
        val studentNamePaint = Paint().apply {
            color = Color.parseColor("#FFD700")
            textSize = 30f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(student.name, (pageWidth / 2).toFloat(), 260f, studentNamePaint)

        // Student Group & Grade
        val studentMetaPaint = Paint().apply {
            color = Color.parseColor("#90CAF9")
            textSize = 14f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val groupName = group?.name ?: "المجموعة الدراسية"
        val grade = group?.grade ?: student.grade
        canvas.drawText("كود الطالب: ${student.code}  •  الصف: $grade  •  $groupName", (pageWidth / 2).toFloat(), 290f, studentMetaPaint)

        // Praise Paragraph
        val defaultPraise = when (certType) {
            "commitment" -> "تقديراً لالتزامه التام بالمواعيد وأداء الواجبات بانتظام وحرصه الدؤوب على التفوق الدراسي في مادة التاريخ."
            "stability" -> "تقديراً لثبات مستواه الأكاديمي المرموق واستقراره ضمن صفوة الطلاب المتفوقين في الاختبارات الشهرية."
            else -> "تقديراً لاجتهاده المتميز وتفوقه الباهر وحصوله على أعلى الدرجات في امتحانات مادة التاريخ مع تمنياتنا بدوام التميز."
        }
        val finalPraise = if (customPraise.isNotBlank()) customPraise else defaultPraise

        val praisePaint = Paint().apply {
            color = Color.WHITE
            textSize = 15f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(finalPraise, (pageWidth / 2).toFloat(), 345f, praisePaint)

        // Islamic / Academic Wish
        val wishPaint = Paint().apply {
            color = Color.parseColor("#81C784")
            textSize = 13f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("مع تمنياتنا القلبية بدوام التفوق وحصد أعلى الدرجات في الثانوية العامة بإذن الله", (pageWidth / 2).toFloat(), 380f, wishPaint)

        // Date on Bottom Right (RTL right)
        val todayStr = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())
        val datePaint = Paint().apply {
            color = Color.parseColor("#B0BEC5")
            textSize = 13f
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
        }
        canvas.drawText("تحريراً في: $todayStr", 60f, 470f, datePaint)
        canvas.drawText("العام الدراسي: 2026 / 2027", 60f, 495f, datePaint)

        // Official Signature & Seal on Bottom Left
        val sigTitlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 13f
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        val sigNamePaint = Paint().apply {
            color = Color.parseColor("#FFD700")
            textSize = 17f
            isFakeBoldText = true
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        canvas.drawText("معلم المادة ومعد المنهج:", (pageWidth - 60).toFloat(), 450f, sigTitlePaint)
        canvas.drawText("مستر محمود عوده", (pageWidth - 60).toFloat(), 480f, sigNamePaint)
        canvas.drawText("خبير مادة التاريخ ✦", (pageWidth - 60).toFloat(), 505f, sigTitlePaint)

        // Central Gold Seal Emblem
        val sealCenter = (pageWidth / 2).toFloat()
        val sealY = 480f
        val sealPaint = Paint().apply {
            color = Color.parseColor("#FFD700")
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        canvas.drawCircle(sealCenter, sealY, 26f, sealPaint)

        val sealInnerPaint = Paint().apply {
            color = Color.parseColor("#1565C0")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(sealCenter, sealY, 23f, sealInnerPaint)

        val sealTextPaint = Paint().apply {
            color = Color.parseColor("#FFD700")
            textSize = 10f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("ختم التفوق", sealCenter, sealY + 3f, sealTextPaint)

        pdfDocument.finishPage(page)

        // Save PDF to cache directory
        return try {
            val certDir = File(context.cacheDir, "certificates")
            if (!certDir.exists()) certDir.mkdirs()

            val fileName = "Certificate_${student.code}_${System.currentTimeMillis()}.pdf"
            val file = File(certDir, fileName)
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()

            // Direct Share Intent
            sharePdf(context, file, student.name, certTitle)
            file
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            Toast.makeText(context, "خطأ في إنشاء ملف PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            null
        }
    }

    private fun drawCornerOrnament(canvas: Canvas, x: Float, y: Float, paint: Paint, dirX: Int, dirY: Int) {
        val len = 20f
        canvas.drawLine(x, y, x + (dirX * len), y, paint)
        canvas.drawLine(x, y, x, y + (dirY * len), paint)
        canvas.drawCircle(x + (dirX * 8f), y + (dirY * 8f), 3f, paint)
    }

    fun sharePdf(context: Context, file: File, studentName: String, certTitle: String) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "$certTitle — $studentName")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "شهادة تقدير وتفوق للطالب $studentName من مستر محمود عوده - التاريخ للثانوية العامة."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "مشاركة شهادة التقدير (PDF)")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر مشاركة الملف: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
