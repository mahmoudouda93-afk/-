package com.example.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object StudentReportGenerator {

    val ARABIC_MONTHS = listOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )

    data class TeacherInfo(
        val name: String = "مستر محمود عوده",
        val subject: String = "أستاذ التاريخ للثانوية العامة",
        val phoneNumber: String = "01099887766"
    )

    enum class ReportTheme(
        val id: String,
        val titleAr: String,
        val subtitleAr: String,
        val primaryHex: String,
        val secondaryHex: String,
        val accentHex: String,
        val bgBodyHex: String,
        val cardBorderHex: String
    ) {
        ROYAL(
            id = "royal",
            titleAr = "الملكي الأكاديمي",
            subtitleAr = "أزرق كحلي مع ذهبي ملكي (النمط الرسمي المعتمد)",
            primaryHex = "#1A4FA0",
            secondaryHex = "#1A73E8",
            accentHex = "#F59E0B",
            bgBodyHex = "#FFFFFF",
            cardBorderHex = "#E5E7EB"
        ),
        HERITAGE(
            id = "heritage",
            titleAr = "التراثي التاريخي",
            subtitleAr = "عنابي أصيل مع ورق عاجي (مستوحى من مخطوطات التاريخ)",
            primaryHex = "#831843",
            secondaryHex = "#991B1B",
            accentHex = "#D97706",
            bgBodyHex = "#FFFDF5",
            cardBorderHex = "#FDE68A"
        ),
        EMERALD(
            id = "emerald",
            titleAr = "الزمردي العصري",
            subtitleAr = "أخضر زمردي مع لمسات نعناعية (تصميم مسطح حديث)",
            primaryHex = "#064E3B",
            secondaryHex = "#0D9488",
            accentHex = "#10B981",
            bgBodyHex = "#F0FDF4",
            cardBorderHex = "#A7F3D0"
        ),
        INK_SAVER(
            id = "ink_saver",
            titleAr = "المينيمال الموفر للحبر",
            subtitleAr = "أبيض وأسود عالي النقاء (مخصص للطباعة الكثيفة)",
            primaryHex = "#0F172A",
            secondaryHex = "#334155",
            accentHex = "#475569",
            bgBodyHex = "#FFFFFF",
            cardBorderHex = "#CBD5E1"
        )
    }

    data class CalculatedReportData(
        val student: StudentEntity,
        val group: GroupEntity?,
        val teacher: TeacherInfo,
        val attendance: List<AttendanceEntity>,
        val presentCount: Int,
        val absentCount: Int,
        val attendanceRate: Int,
        val payments: List<PaymentEntity>,
        val totalPaid: Double,
        val subscriptionPrice: Double,
        val discountLabel: String,
        val discountedPrice: Double,
        val arrears: Double,
        val grades: List<ExamScoreEntity>,
        val avgGrade: Int,
        val avgGradeStr: String,
        val groupHomeworks: List<TaskEntity>,
        val onTimeCount: Int,
        val lateCount: Int,
        val notSubmittedCount: Int,
        val hwRate: Int,
        val interactAll: Int,
        val interactMost: Int,
        val interactNone: Int,
        val interactRate: Int,
        val finalPerf: Int,
        val scheduleText: String,
        val todayAr: String,
        val attRows: String,
        val interactRows: String,
        val hwRows: String
    )

    // ─── Data Helpers ─────────────────────────────────────────────
    fun calcReportData(
        student: StudentEntity,
        group: GroupEntity?,
        allAttendance: List<AttendanceEntity>,
        allPayments: List<PaymentEntity>,
        allExams: List<ExamEntity>,
        allScores: List<ExamScoreEntity>,
        allTasks: List<TaskEntity> = emptyList(),
        allSubmissions: List<SubmissionEntity> = emptyList(),
        teacher: TeacherInfo = TeacherInfo()
    ): CalculatedReportData {
        val studentAttendance = allAttendance.filter { it.studentId == student.id }
        val presentCount = studentAttendance.count {
            val s = it.status.lowercase(Locale.ROOT)
            s == "present" || s == "late" || s == "حاضر" || s == "متأخر"
        }
        val absentCount = studentAttendance.count {
            val s = it.status.lowercase(Locale.ROOT)
            s == "absent" || s == "غائب"
        }
        val attendanceRate = if (studentAttendance.isNotEmpty()) {
            Math.round((presentCount.toDouble() / studentAttendance.size) * 100).toInt()
        } else 0

        val studentPayments = allPayments.filter { it.studentId == student.id }
        val totalPaid = studentPayments.sumOf { it.amount }
        val subscriptionPrice = group?.price ?: 0.0

        var discountLabel = "لا يوجد"
        var discountedPrice = subscriptionPrice
        val dt = student.discountType.lowercase(Locale.ROOT)
        if ((dt == "fixed" || dt == "مبلغ ثابت") && student.discountValue > 0) {
            discountLabel = "${student.discountValue.toInt()} ج.م"
            discountedPrice = maxOf(0.0, subscriptionPrice - student.discountValue)
        } else if ((dt == "percentage" || dt == "نسبة مئوية") && student.discountValue > 0) {
            discountLabel = "${student.discountValue.toInt()}%"
            discountedPrice = maxOf(0.0, Math.round(subscriptionPrice * (1.0 - student.discountValue / 100.0)).toDouble())
        }
        val arrears = maxOf(0.0, discountedPrice - totalPaid)

        val studentScores = allScores.filter { it.studentId == student.id }
        val avgGrade = if (studentScores.isNotEmpty()) {
            val percentages = studentScores.map { scoreItem ->
                val exam = allExams.find { it.id == scoreItem.examId }
                val max = if (exam != null && exam.maxScore > 0) exam.maxScore else 100.0
                (scoreItem.score / max) * 100.0
            }
            Math.round(percentages.average()).toInt().coerceIn(0, 100)
        } else 0
        val avgGradeStr = if (studentScores.isNotEmpty()) "$avgGrade%" else "0%"

        val groupHomeworks = allTasks.filter { it.groupId == student.groupId }
        val studentSubmissions = allSubmissions.filter { it.studentId == student.id }
        val onTimeCount = studentSubmissions.count { it.submitted }
        val lateCount = 0
        val notSubmittedCount = maxOf(0, groupHomeworks.size - onTimeCount)
        val hwRate = if (groupHomeworks.isNotEmpty()) {
            Math.round((onTimeCount.toDouble() / groupHomeworks.size) * 100).toInt().coerceIn(0, 100)
        } else 100

        // Interaction statistics
        val interactAll = studentAttendance.count {
            (it.status.lowercase() == "present" || it.status == "حاضر") && (student.points > 20 || it.excuseReason?.contains("تفاعل") == true)
        }
        val interactMost = maxOf(0, presentCount - interactAll)
        val interactNone = absentCount
        val interactRate = if (studentAttendance.isNotEmpty()) {
            Math.round(((interactAll + interactMost).toDouble() / studentAttendance.size) * 100).toInt().coerceIn(0, 100)
        } else 85

        val finalPerf = Math.round((attendanceRate + avgGrade + interactRate + hwRate) / 4.0).toInt().coerceIn(0, 100)

        // Schedule formatting
        val scheduleText = parseSchedule(group?.scheduleJson)

        // Arabic Date
        val cal = Calendar.getInstance()
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val monthIdx = cal.get(Calendar.MONTH)
        val year = cal.get(Calendar.YEAR)
        val todayAr = "$day ${ARABIC_MONTHS.getOrElse(monthIdx) { "سبتمبر" }} $year"

        // Rows for HTML tables
        val attRows = if (studentAttendance.isNotEmpty()) {
            studentAttendance.takeLast(10).reversed().joinToString("\n") { a ->
                val s = a.status.lowercase()
                val (badgeClass, label) = when {
                    s == "present" || s == "حاضر" -> "badge-green" to "حاضر"
                    s == "absent" || s == "غائب" -> "badge-red" to "غائب"
                    else -> "badge-yellow" to (if (s == "late") "متأخر" else a.status)
                }
                """<tr>
                    <td>${a.date}</td>
                    <td><span class="badge $badgeClass">$label</span></td>
                    <td>${a.excuseReason ?: "—"}</td>
                </tr>"""
            }
        } else {
            """<tr><td colspan="3" style="text-align:center;color:#999">لا توجد سجلات حضور مسجلة</td></tr>"""
        }

        val interactRows = if (studentAttendance.isNotEmpty()) {
            studentAttendance.takeLast(10).reversed().joinToString("\n") { a ->
                val s = a.status.lowercase()
                val interactionText = when {
                    s == "present" || s == "حاضر" -> "جاوب على معظمها 👍"
                    s == "late" || s == "متأخر" -> "جاوب على الكل 🌟"
                    else -> "لم يجب 😔"
                }
                """<tr>
                    <td>${a.date}</td>
                    <td>$interactionText</td>
                </tr>"""
            }
        } else {
            """<tr><td colspan="2" style="text-align:center;color:#999">لا توجد سجلات تفاعل مسجلة</td></tr>"""
        }

        val hwRows = if (groupHomeworks.isNotEmpty()) {
            groupHomeworks.joinToString("\n") { hw ->
                val sub = studentSubmissions.find { it.taskId == hw.id }
                val isSub = sub?.submitted == true
                val badgeClass = if (isSub) "badge-green" else "badge-red"
                val statusText = if (isSub) "سلّم في الوقت ✅" else "لم يسلّم ✗"
                """<tr>
                    <td>${hw.title}</td>
                    <td>${hw.dueDate ?: "—"}</td>
                    <td><span class="badge $badgeClass">$statusText</span></td>
                </tr>"""
            }
        } else {
            """<tr><td colspan="3" style="text-align:center;color:#999">لا توجد واجبات دراسية مسجلة للمجموعة</td></tr>"""
        }

        return CalculatedReportData(
            student = student,
            group = group,
            teacher = teacher,
            attendance = studentAttendance,
            presentCount = presentCount,
            absentCount = absentCount,
            attendanceRate = attendanceRate,
            payments = studentPayments,
            totalPaid = totalPaid,
            subscriptionPrice = subscriptionPrice,
            discountLabel = discountLabel,
            discountedPrice = discountedPrice,
            arrears = arrears,
            grades = studentScores,
            avgGrade = avgGrade,
            avgGradeStr = avgGradeStr,
            groupHomeworks = groupHomeworks,
            onTimeCount = onTimeCount,
            lateCount = lateCount,
            notSubmittedCount = notSubmittedCount,
            hwRate = hwRate,
            interactAll = interactAll,
            interactMost = interactMost,
            interactNone = interactNone,
            interactRate = interactRate,
            finalPerf = finalPerf,
            scheduleText = scheduleText,
            todayAr = todayAr,
            attRows = attRows,
            interactRows = interactRows,
            hwRows = hwRows
        )
    }

    private fun parseSchedule(json: String?): String {
        if (json.isNullOrBlank() || json == "[]") return "السبت والثلاثاء 04:00م - 06:00م"
        return try {
            val arr = org.json.JSONArray(json)
            val parts = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val day = obj.optString("day", "")
                val from = obj.optString("from", "")
                val to = obj.optString("to", "")
                if (day.isNotBlank()) parts.add("$day $from-$to")
            }
            if (parts.isNotEmpty()) parts.joinToString("، ") else "السبت والثلاثاء 04:00م"
        } catch (e: Exception) {
            "السبت والثلاثاء 04:00م"
        }
    }

    // ─── CSS Styles ───────────────────────────────────────────────
    fun getReportCSS(theme: ReportTheme = ReportTheme.ROYAL): String {
        return """
        * { margin:0; padding:0; box-sizing:border-box; }
        body { font-family:'Cairo',-apple-system,BlinkMacSystemFont,sans-serif; background:${theme.bgBodyHex}; color:#1a1a1a; font-size:14px; direction:rtl; }
        .page { max-width:900px; margin:0 auto; background:${theme.bgBodyHex}; border:1px solid ${theme.cardBorderHex}; }

        .header { display:flex; justify-content:space-between; align-items:center; padding:18px 30px; background:linear-gradient(135deg,${theme.primaryHex} 0%,${theme.secondaryHex} 100%); color:#fff; }
        .header-logo { font-size:20px; font-weight:900; }
        .header-logo span { display:block; font-size:11px; opacity:.85; font-weight:400; margin-top:2px; }
        .header-title { text-align:left; }
        .header-title h1 { font-size:22px; font-weight:900; }
        .header-title p { opacity:.85; font-size:12px; margin-top:3px; }

        .student-info { display:flex; align-items:center; gap:15px; padding:14px 30px; background:#f8f9fa; border-bottom:1px solid ${theme.cardBorderHex}; }
        .avatar { width:54px; height:54px; border-radius:50%; background:${theme.secondaryHex}; display:flex; align-items:center; justify-content:center; color:#fff; font-size:22px; font-weight:900; border:2px solid ${theme.accentHex}; flex-shrink:0; }
        .student-details { flex:1; }
        .student-name { font-size:19px; font-weight:900; color:#111; }
        .student-meta { font-size:12px; color:#555; margin-top:3px; line-height:1.7; }
        .status-badge { display:inline-flex; align-items:center; gap:4px; background:#dcfce7; color:#16a34a; padding:3px 10px; border-radius:20px; font-size:11px; font-weight:700; border:1px solid #86efac; }

        .summary-cards { display:grid; grid-template-columns:repeat(3,1fr); gap:12px; padding:18px 30px; }
        .sum-card { border:1px solid ${theme.cardBorderHex}; border-radius:12px; padding:14px; text-align:center; }
        .sum-card.yellow { border-color:${theme.accentHex}; background:#fffbeb; }
        .sum-card.blue { border-color:${theme.secondaryHex}; background:#eff6ff; }
        .sum-card.green { border-color:#86efac; background:#f0fdf4; }
        .sum-card .icon { font-size:20px; margin-bottom:5px; }
        .sum-card .value { font-size:22px; font-weight:900; }
        .sum-card.yellow .value { color:#d97706; }
        .sum-card.blue .value { color:${theme.secondaryHex}; }
        .sum-card.green .value { color:#16a34a; }
        .sum-card .label { font-size:11px; color:#6b7280; margin-top:3px; }

        .section { padding:0 30px 18px; }
        .section-title { display:flex; align-items:center; gap:8px; font-size:15px; font-weight:900; color:#1a1a1a; margin-bottom:10px; padding-bottom:7px; border-bottom:2px solid ${theme.secondaryHex}; }
        .section-title:before { content:''; display:inline-block; width:4px; height:17px; background:${theme.secondaryHex}; border-radius:2px; margin-left:6px; }

        .stats-grid { display:grid; grid-template-columns:repeat(2,1fr); gap:8px; }
        .stat-item { display:flex; justify-content:space-between; align-items:center; background:#f8f9fa; border:1px solid ${theme.cardBorderHex}; border-radius:10px; padding:9px 13px; }
        .stat-label { font-size:12px; color:#555; }
        .stat-value { font-weight:900; font-size:14px; color:${theme.secondaryHex}; }
        .stat-value.red { color:#dc2626; }
        .stat-value.green { color:#16a34a; }
        .stat-value.orange { color:#ea580c; }

        .interact-cards { display:grid; grid-template-columns:repeat(3,1fr); gap:10px; margin-bottom:10px; }
        .interact-card { border:1px solid ${theme.cardBorderHex}; border-radius:10px; padding:11px; text-align:center; }
        .interact-card .num { font-size:22px; font-weight:900; }
        .interact-card .lbl { font-size:11px; color:#555; margin-top:3px; }
        .ic-green { border-color:#86efac; background:#f0fdf4; }
        .ic-green .num { color:#16a34a; }
        .ic-blue { border-color:${theme.secondaryHex}; background:#eff6ff; }
        .ic-blue .num { color:${theme.secondaryHex}; }
        .ic-red { border-color:#fca5a5; background:#fef2f2; }
        .ic-red .num { color:#dc2626; }

        table { width:100%; border-collapse:collapse; font-size:12px; margin-top:6px; }
        th { background:${theme.secondaryHex}; color:#fff; padding:8px 12px; text-align:right; font-weight:700; }
        td { padding:7px 12px; border-bottom:1px solid ${theme.cardBorderHex}; }
        tr:nth-child(even) td { background:#f8f9fa; }

        .badge { display:inline-block; padding:2px 10px; border-radius:20px; font-size:11px; font-weight:700; }
        .badge-green { background:#dcfce7; color:#16a34a; }
        .badge-red { background:#fee2e2; color:#dc2626; }
        .badge-yellow { background:#fef9c3; color:#ca8a04; }

        .fin-cards { display:grid; grid-template-columns:repeat(4,1fr); gap:8px; }
        .fin-card { border:1px solid ${theme.cardBorderHex}; border-radius:10px; padding:10px; text-align:center; }
        .fin-card .fval { font-size:15px; font-weight:900; }
        .fin-card .flbl { font-size:10px; color:#6b7280; margin-top:2px; }
        .fc-green { border-color:#86efac; background:#f0fdf4; }
        .fc-green .fval { color:#16a34a; }
        .fc-red { border-color:#fca5a5; background:#fef2f2; }
        .fc-red .fval { color:#dc2626; }
        .fc-blue { border-color:${theme.secondaryHex}; background:#eff6ff; }
        .fc-blue .fval { color:${theme.secondaryHex}; }
        .fc-gray { border-color:#e5e7eb; background:#f9fafb; }
        .fc-gray .fval { color:#374151; }

        .footer-bar { border-top:3px solid ${theme.secondaryHex}; margin-top:16px; background:#fff; }
        .footer-main { display:flex; justify-content:space-between; align-items:flex-end; padding:14px 30px; }
        .sign-block { text-align:center; }
        .sign-label { font-size:11px; color:#666; margin-bottom:6px; }
        .sign-name { font-size:15px; font-weight:900; color:#1a1a1a; margin-bottom:2px; }
        .sign-title { font-size:11px; color:${theme.secondaryHex}; }
        .seal { width:76px; height:76px; border-radius:50%; border:2px dashed ${theme.secondaryHex}; display:flex; align-items:center; justify-content:center; color:${theme.secondaryHex}; font-size:12px; font-weight:700; text-align:center; line-height:1.4; }
        .date-block { text-align:left; }
        .date-pill { display:inline-flex; align-items:center; gap:7px; background:${theme.secondaryHex}; color:#fff; padding:7px 14px; border-radius:8px; font-size:13px; font-weight:700; margin-bottom:6px; }
        .watermark { font-size:11px; color:#6b7280; }
        """
    }

    fun footerHTML(
        teacher: TeacherInfo,
        todayAr: String,
        academicYear: String = "2026/2027",
        theme: ReportTheme = ReportTheme.ROYAL
    ): String {
        return """
        <div class="footer-bar">
          <div class="footer-main">
            <div class="sign-block">
              <div class="sign-label">توقيع المعلم</div>
              <div class="sign-name">${teacher.name}</div>
              <div class="sign-title">${teacher.subject}</div>
            </div>
            <div class="seal">ختم<br>رسمي</div>
            <div class="date-block">
              <div class="date-pill">📅 تاريخ الإصدار: $todayAr</div>
              <div class="watermark">تم إنشاؤه بواسطة تطبيق المساعد الأكاديمي</div>
            </div>
          </div>
        </div>
        """
    }

    // ─── Full HTML Builder ─────────────────────────────────────────
    fun buildReportHTML(
        d: CalculatedReportData,
        currentTerm: String = "الفصل الدراسي الأول",
        currentAcademicYear: String = "2026/2027",
        theme: ReportTheme = ReportTheme.ROYAL
    ): String {
        val student = d.student
        val group = d.group
        val perfClass = when {
            d.finalPerf >= 75 -> "green"
            d.finalPerf >= 60 -> "orange"
            else -> "red"
        }

        return """<!DOCTYPE html>
<html dir="rtl" lang="ar">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>تقرير الأداء الأكاديمي - ${student.name}</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link href="https://fonts.googleapis.com/css2?family=Cairo:wght@400;600;700;900&display=swap" rel="stylesheet">
<style>
${getReportCSS(theme)}
</style>
</head>
<body>
<div class="page">
  <div class="header">
    <div class="header-logo">المساعد<span>مساعد المعلم الذكي • التاريخ</span></div>
    <div class="header-title">
      <h1>تقرير الأداء الأكاديمي</h1>
      <p>$currentTerm — $currentAcademicYear</p>
    </div>
  </div>

  <div class="student-info">
    <div class="avatar">${student.name.take(1)}</div>
    <div class="student-details">
      <div style="display:flex;align-items:center;gap:10px;margin-bottom:4px">
        <span class="student-name">${student.name}</span>
        <span class="status-badge">${if (student.isActive) "✅ نشط" else "❌ غير نشط"}</span>
      </div>
      <div class="student-meta">
        الصف: ${student.grade.ifBlank { group?.grade ?: "—" }} • المجموعة: ${group?.name ?: "—"} • تاريخ الالتحاق: ${student.joinDate}<br>
        📱 ${student.phone.ifBlank { "—" }} • هاتف ولي الأمر: ${student.parentPhone.ifBlank { "—" }} • كود الطالب: <strong>${student.code}</strong>
        ${if (d.scheduleText != "—") "<br>🕐 مواعيد الحصص: ${d.scheduleText}" else ""}
      </div>
    </div>
  </div>

  <div class="summary-cards">
    <div class="sum-card yellow"><div class="icon">💰</div><div class="value">${d.totalPaid.toInt()} ج.م</div><div class="label">إجمالي المدفوع</div></div>
    <div class="sum-card blue"><div class="icon">📊</div><div class="value">${d.avgGradeStr}</div><div class="label">متوسط الدرجات</div></div>
    <div class="sum-card green"><div class="icon">📅</div><div class="value">${d.attendanceRate}%</div><div class="label">نسبة الحضور</div></div>
  </div>

  <div class="section">
    <div class="section-title">ملخص إحصائيات الأداء</div>
    <div class="stats-grid">
      <div class="stat-item"><span class="stat-label">نسبة الحضور الكلية</span><span class="stat-value green">${d.attendanceRate}%</span></div>
      <div class="stat-item"><span class="stat-label">عدد مرات الغياب</span><span class="stat-value red">${d.absentCount} مرة</span></div>
      <div class="stat-item"><span class="stat-label">الواجبات المُسلَّمة</span><span class="stat-value">${d.onTimeCount}</span></div>
      <div class="stat-item"><span class="stat-label">واجبات لم تُسلَّم</span><span class="stat-value red">${d.notSubmittedCount}</span></div>
      <div class="stat-item"><span class="stat-label">متوسط درجات الاختبارات</span><span class="stat-value">${d.avgGradeStr}</span></div>
      <div class="stat-item"><span class="stat-label">نسبة المشاركة الفعالة</span><span class="stat-value orange">${d.interactRate}%</span></div>
      <div class="stat-item"><span class="stat-label">نسبة تسليم الواجبات</span><span class="stat-value">${d.hwRate}%</span></div>
      <div class="stat-item"><span class="stat-label">متوسط الأداء النهائي</span><span class="stat-value $perfClass">${d.finalPerf}%</span></div>
    </div>
  </div>

  <div class="section">
    <div class="section-title">تفاعل الطالب داخل الحصة ⚡</div>
    <div class="interact-cards">
      <div class="interact-card ic-green"><div class="num">${d.interactAll}</div><div class="lbl">جاوب على الكل 🌟</div></div>
      <div class="interact-card ic-blue"><div class="num">${d.interactMost}</div><div class="lbl">جاوب على معظمها 👍</div></div>
      <div class="interact-card ic-red"><div class="num">${d.interactNone}</div><div class="lbl">لم يجب 😔</div></div>
    </div>
    <table>
      <thead>
        <tr><th>التاريخ</th><th>المستوى والتفاعل</th></tr>
      </thead>
      <tbody>
        ${d.interactRows}
      </tbody>
    </table>
  </div>

  <div class="section">
    <div class="section-title">سجل الحضور والغياب 📋</div>
    <table>
      <thead>
        <tr><th>التاريخ</th><th>الحالة</th><th>ملاحظات</th></tr>
      </thead>
      <tbody>
        ${d.attRows}
      </tbody>
    </table>
  </div>

  <div class="section">
    <div class="section-title">سجل تسليم الواجبات والمهام 📚</div>
    <table>
      <thead>
        <tr><th>الواجب</th><th>تاريخ التسليم</th><th>الحالة</th></tr>
      </thead>
      <tbody>
        ${d.hwRows}
      </tbody>
    </table>
  </div>

  <div class="section">
    <div class="section-title">الوضع المالي 💰</div>
    <div class="fin-cards">
      <div class="fin-card fc-green"><div class="fval">${if (d.arrears <= 0) "مسدد ✅" else "متعثر ⚠️"}</div><div class="flbl">حالة الاشتراك</div></div>
      <div class="fin-card fc-red"><div class="fval">${d.arrears.toInt()} ج.م</div><div class="flbl">المتأخرات</div></div>
      <div class="fin-card fc-gray"><div class="fval">${d.discountLabel}</div><div class="flbl">الخصم المطبق</div></div>
      <div class="fin-card fc-blue"><div class="fval">${d.totalPaid.toInt()} ج.م</div><div class="flbl">إجمالي المدفوع</div></div>
    </div>
  </div>

  ${footerHTML(d.teacher, d.todayAr, currentAcademicYear, theme)}
</div>
</body>
</html>"""
    }

    // ─── CSV Export Engine (UTF-8 with BOM for Excel/Sheets) ───────
    fun generateMonthlyPerformanceCsv(
        calculatedList: List<CalculatedReportData>,
        title: String = "تقرير الأداء الشهري للطلاب",
        filterLabel: String = "جميع الطلاب"
    ): String {
        val sb = StringBuilder()
        // UTF-8 BOM so Excel opens Arabic text directly with 100% proper encoding
        sb.append("\uFEFF")

        // Metadata header rows
        sb.append("\"$title - تطبيق المساعد الأكاديمي لمستر محمود عوده (التاريخ)\"\n")
        sb.append("\"التصنيف: $filterLabel\",\"تاريخ التقرير: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}\",\"عدد الطلاب: ${calculatedList.size}\"\n")
        sb.append("\n")

        // CSV Columns
        val columns = listOf(
            "كود الطالب",
            "اسم الطالب",
            "المجموعة الدراسية",
            "الصف الدراسي",
            "نسبة الحضور %",
            "مرات الحضور",
            "مرات الغياب",
            "متوسط الامتحانات %",
            "نسبة تسليم الواجبات %",
            "نسبة التفاعل %",
            "التقييم العام النهائي %",
            "سعر الاشتراك (ج)",
            "قيمة الخصم",
            "المطلوب بعد الخصم (ج)",
            "إجمالي المدفوع (ج)",
            "المتأخرات المتبقية (ج)",
            "حالة السداد",
            "هاتف الطالب",
            "هاتف ولي الأمر"
        )
        sb.append(columns.joinToString(",") { escapeCsv(it) }).append("\n")

        // Student Data Rows
        calculatedList.forEach { d ->
            val row = listOf(
                d.student.code,
                d.student.name,
                d.group?.name ?: "—",
                d.student.grade.ifBlank { d.group?.grade ?: "—" },
                "${d.attendanceRate}%",
                d.presentCount.toString(),
                d.absentCount.toString(),
                d.avgGradeStr,
                "${d.hwRate}%",
                "${d.interactRate}%",
                "${d.finalPerf}%",
                d.subscriptionPrice.toInt().toString(),
                d.discountLabel,
                d.discountedPrice.toInt().toString(),
                d.totalPaid.toInt().toString(),
                d.arrears.toInt().toString(),
                if (d.arrears <= 0) "مسدد بالكامل" else "عليه متأخرات",
                d.student.phone,
                d.student.parentPhone
            )
            sb.append(row.joinToString(",") { escapeCsv(it) }).append("\n")
        }

        // Summary row at bottom
        sb.append("\n")
        val totalRevenue = calculatedList.sumOf { it.totalPaid }
        val totalArrears = calculatedList.sumOf { it.arrears }
        val avgAtt = if (calculatedList.isNotEmpty()) calculatedList.map { it.attendanceRate }.average().toInt() else 0
        val avgGrades = if (calculatedList.isNotEmpty()) calculatedList.map { it.avgGrade }.average().toInt() else 0
        val avgPerf = if (calculatedList.isNotEmpty()) calculatedList.map { it.finalPerf }.average().toInt() else 0

        sb.append(escapeCsv("الإجمالي والمتوسطات")).append(",")
        sb.append(escapeCsv("إجمالي الطلاب: ${calculatedList.size}")).append(",")
        sb.append(escapeCsv("—")).append(",")
        sb.append(escapeCsv("—")).append(",")
        sb.append(escapeCsv("$avgAtt%")).append(",")
        sb.append(escapeCsv("—")).append(",")
        sb.append(escapeCsv("—")).append(",")
        sb.append(escapeCsv("$avgGrades%")).append(",")
        sb.append(escapeCsv("—")).append(",")
        sb.append(escapeCsv("—")).append(",")
        sb.append(escapeCsv("$avgPerf%")).append(",")
        sb.append(escapeCsv("—")).append(",")
        sb.append(escapeCsv("—")).append(",")
        sb.append(escapeCsv("—")).append(",")
        sb.append(escapeCsv("${totalRevenue.toInt()} ج.م")).append(",")
        sb.append(escapeCsv("${totalArrears.toInt()} ج.م")).append(",")
        sb.append(escapeCsv("—")).append(",")
        sb.append(escapeCsv("—")).append(",")
        sb.append(escapeCsv("—")).append("\n")

        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        val clean = value.replace("\"", "\"\"")
        return "\"$clean\""
    }

    // ─── Export & Share CSV File ──────────────────────────────────
    fun exportAndShareCsv(
        context: Context,
        csvContent: String,
        fileName: String,
        subject: String = "تقرير أداء الطلاب الشهري (CSV)"
    ): File? {
        return try {
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()

            val sanitizedName = fileName.replace("[^a-zA-Z0-9_\\-\\u0600-\\u06FF]".toRegex(), "_")
            val file = File(reportsDir, "${sanitizedName}_${System.currentTimeMillis()}.csv")
            FileOutputStream(file).use { fos ->
                fos.write(csvContent.toByteArray(Charsets.UTF_8))
                fos.flush()
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(
                    Intent.EXTRA_TEXT,
                    "مرفق ملف تقرير الأداء الشهري للطلاب بتنسيق CSV للمعاينة في برامج الجداول الحسابية (Excel / Sheets)."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "مشاركة تقرير CSV عبر")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            file
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "خطأ أثناء تصدير ملف CSV: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            null
        }
    }

    // ─── Export Word Document (.doc) ──────────────────────────────
    fun exportReportWord(
        context: Context,
        d: CalculatedReportData,
        theme: ReportTheme = ReportTheme.ROYAL
    ): File? {
        return try {
            val html = buildReportHTML(d, theme = theme)
            val wordDoc = """<html xmlns:o='urn:schemas-microsoft-com:office:office'
    xmlns:w='urn:schemas-microsoft-com:office:word'
    xmlns='http://www.w3.org/TR/REC-html40'>
<head>
  <meta charset='UTF-8'>
  <meta name=ProgId content=Word.Document>
  <meta name=Generator content="Microsoft Word 15">
  <style>
    @page { size:A4; margin:1.5cm 2cm; }
    body { font-family:'Arial Unicode MS','Cairo',sans-serif; direction:rtl; font-size:12pt; background:${theme.bgBodyHex}; }
    table { border-collapse:collapse; width:100%; margin-top:8px; }
    th,td { border:1px solid #ccc; padding:6px 10px; text-align:right; }
    th { background:${theme.secondaryHex}; color:#fff; }
    .badge { padding:2px 8px; font-weight:bold; }
  </style>
</head>
<body>
  ${html.replace(Regex("(?s)<!DOCTYPE html>.*<body>"), "").replace("</body>\n</html>", "")}
</body>
</html>"""

            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()

            val sanitizedStudentName = d.student.name.replace("[^a-zA-Z0-9_\\-\\u0600-\\u06FF]".toRegex(), "_")
            val file = File(reportsDir, "تقرير_${sanitizedStudentName}_${System.currentTimeMillis()}.doc")
            FileOutputStream(file).use { fos ->
                fos.write("\uFEFF".toByteArray(Charsets.UTF_8))
                fos.write(wordDoc.toByteArray(Charsets.UTF_8))
                fos.flush()
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/msword"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "تقرير الأداء الأكاديمي Word - ${d.student.name} (${theme.titleAr})")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "مرفق ملف تقرير الأداء الأكاديمي للطالب ${d.student.name} بصيغة Word (.doc) بنمط [${theme.titleAr}]."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "مشاركة تقرير Word عبر")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            file
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "خطأ في تصدير مستند Word: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            null
        }
    }

    // ─── Native Android Print / PDF (Save as PDF) ─────────────────
    fun printOrSavePdf(
        context: Context,
        d: CalculatedReportData,
        theme: ReportTheme = ReportTheme.ROYAL
    ) {
        try {
            val html = buildReportHTML(d, theme = theme)
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager == null) {
                Toast.makeText(context, "خدمة الطباعة غير متوفرة في النظام", Toast.LENGTH_SHORT).show()
                return
            }

            Handler(Looper.getMainLooper()).post {
                val webView = WebView(context)
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        val jobName = "تقرير_${d.student.name}_${d.student.code}"
                        val printAdapter = webView.createPrintDocumentAdapter(jobName)
                        val printAttributes = PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                            .setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
                            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                            .build()
                        printManager.print(jobName, printAdapter, printAttributes)
                    }
                }
                webView.loadDataWithBaseURL("https://fonts.googleapis.com", html, "text/html", "UTF-8", null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "خطأ في تجهيز أمر الطباعة/PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    // ─── WhatsApp Report ──────────────────────────────────────────
    fun sendReportWhatsApp(context: Context, d: CalculatedReportData) {
        val student = d.student
        val group = d.group
        val teacher = d.teacher

        val msg = """📊 *تقرير أداء الطالب*
━━━━━━━━━━━━━━━━
👤 *${student.name}*
🎓 ${student.grade.ifBlank { group?.grade ?: "—" }} • ${group?.name ?: "—"}
📅 تاريخ التقرير: ${d.todayAr}

📈 *ملخص الأداء:*
✅ نسبة الحضور: ${d.attendanceRate}%
📊 متوسط الدرجات: ${if (d.avgGrade > 0) "${d.avgGrade}%" else "لا توجد بيانات"}
💰 إجمالي المدفوع: ${d.totalPaid.toInt()} ج.م
⏳ المتأخرات: ${d.arrears.toInt()} ج.م
📝 الواجبات المُسلَّمة: ${d.onTimeCount} من ${d.groupHomeworks.size}
⭐ الأداء النهائي: ${d.finalPerf}%

━━━━━━━━━━━━━━━━
👨‍🏫 أ. ${teacher.name} - ${teacher.subject}
📱 ${teacher.phoneNumber}"""

        val phoneTarget = if (student.parentPhone.isNotBlank()) student.parentPhone else student.phone
        val cleanPhone = phoneTarget.replace(Regex("[^0-9]"), "")
        val formattedPhone = when {
            cleanPhone.startsWith("01") -> "2$cleanPhone"
            cleanPhone.startsWith("1") && cleanPhone.length == 10 -> "20$cleanPhone"
            cleanPhone.isNotBlank() -> cleanPhone
            else -> ""
        }

        val uri = if (formattedPhone.isNotBlank()) {
            Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=" + Uri.encode(msg))
        } else {
            Uri.parse("https://api.whatsapp.com/send?text=" + Uri.encode(msg))
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to general share chooser
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, msg)
            }
            context.startActivity(Intent.createChooser(shareIntent, "مشاركة التقرير"))
        }
    }
}
