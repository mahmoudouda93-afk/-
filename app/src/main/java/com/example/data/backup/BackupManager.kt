package com.example.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class BackupMetadata(
    val isValid: Boolean,
    val title: String = "",
    val exportDate: String = "",
    val studentsCount: Int = 0,
    val groupsCount: Int = 0,
    val attendanceCount: Int = 0,
    val paymentsCount: Int = 0,
    val examsCount: Int = 0,
    val scoresCount: Int = 0,
    val cashDrawerCount: Int = 0,
    val totalAmountPayments: Double = 0.0,
    val errorMessage: String? = null
)

data class BackupParsedData(
    val metadata: BackupMetadata,
    val students: List<StudentEntity>,
    val groups: List<GroupEntity>,
    val attendance: List<AttendanceEntity>,
    val payments: List<PaymentEntity>,
    val exams: List<ExamEntity>,
    val scores: List<ExamScoreEntity>,
    val cashDrawer: List<CashDrawerEntity>
)

object BackupManager {

    suspend fun generateFullBackupJson(db: AppDatabase): String = withContext(Dispatchers.IO) {
        val students = db.studentDao().getAllStudents().first()
        val groups = db.groupDao().getAllGroups().first()
        val attendance = db.attendanceDao().getAllAttendance().first()
        val payments = db.paymentDao().getAllPayments().first()
        val exams = db.examDao().getAllExams().first()
        val scores = db.examScoreDao().getAllScores().first()
        val cashDrawer = db.cashDrawerDao().getAllEntries().first()

        val root = JSONObject()
        root.put("app", "AlMusaed")
        root.put("systemName", "منظومة المساعد لإدارة المراكز التعليمية")
        root.put("teacher", "مستر محمود عوده")
        root.put("version", 2)
        root.put("exportTimestamp", System.currentTimeMillis())
        val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("ar")).format(Date())
        root.put("exportDate", formattedDate)

        // Summary
        val summary = JSONObject().apply {
            put("studentsCount", students.size)
            put("groupsCount", groups.size)
            put("attendanceCount", attendance.size)
            put("paymentsCount", payments.size)
            put("examsCount", exams.size)
            put("scoresCount", scores.size)
            put("cashDrawerCount", cashDrawer.size)
            put("totalPaymentsSum", payments.sumOf { it.amount })
        }
        root.put("summary", summary)

        // Students Array
        val studentsArr = JSONArray()
        students.forEach { s ->
            studentsArr.put(JSONObject().apply {
                put("id", s.id)
                put("code", s.code)
                put("name", s.name)
                put("groupId", s.groupId)
                put("grade", s.grade)
                put("phone", s.phone)
                put("parentPhone", s.parentPhone)
                put("parentPhone2", s.parentPhone2)
                put("parentRelation", s.parentRelation)
                put("discountType", s.discountType)
                put("discountValue", s.discountValue)
                put("joinDate", s.joinDate)
                put("parentToken", s.parentToken)
                put("points", s.points)
                put("badges", s.badges)
                put("isActive", s.isActive)
            })
        }
        root.put("students", studentsArr)

        // Groups Array
        val groupsArr = JSONArray()
        groups.forEach { g ->
            groupsArr.put(JSONObject().apply {
                put("id", g.id)
                put("name", g.name)
                put("stage", g.stage)
                put("grade", g.grade)
                put("subject", g.subject)
                put("location", g.location)
                put("capacity", g.capacity)
                put("price", g.price)
                put("sessionsPerMonth", g.sessionsPerMonth)
                put("whatsappLink", g.whatsappLink)
                put("scheduleJson", g.scheduleJson)
                put("extraSessionsJson", g.extraSessionsJson)
            })
        }
        root.put("groups", groupsArr)

        // Attendance Array
        val attendanceArr = JSONArray()
        attendance.forEach { a ->
            attendanceArr.put(JSONObject().apply {
                put("id", a.id)
                put("studentId", a.studentId)
                put("groupId", a.groupId)
                put("date", a.date)
                put("status", a.status)
                put("excuseReason", a.excuseReason ?: "")
                put("makeupGroupId", a.makeupGroupId ?: "")
                put("makeupDate", a.makeupDate ?: "")
                put("timestamp", a.timestamp)
            })
        }
        root.put("attendance", attendanceArr)

        // Payments Array
        val paymentsArr = JSONArray()
        payments.forEach { p ->
            paymentsArr.put(JSONObject().apply {
                put("id", p.id)
                put("studentId", p.studentId)
                put("groupId", p.groupId)
                put("amount", p.amount)
                put("type", p.type)
                put("otherDescription", p.otherDescription ?: "")
                put("targetMonth", p.targetMonth)
                put("date", p.date)
                put("receiptNumber", p.receiptNumber)
                put("timestamp", p.timestamp)
            })
        }
        root.put("payments", paymentsArr)

        // Exams Array
        val examsArr = JSONArray()
        exams.forEach { e ->
            examsArr.put(JSONObject().apply {
                put("id", e.id)
                put("groupId", e.groupId)
                put("name", e.name)
                put("maxScore", e.maxScore)
                put("date", e.date)
            })
        }
        root.put("exams", examsArr)

        // Exam Scores Array
        val scoresArr = JSONArray()
        scores.forEach { sc ->
            scoresArr.put(JSONObject().apply {
                put("id", sc.id)
                put("examId", sc.examId)
                put("studentId", sc.studentId)
                put("score", sc.score)
            })
        }
        root.put("exam_scores", scoresArr)

        // Cash Drawer Array
        val cashArr = JSONArray()
        cashDrawer.forEach { cd ->
            cashArr.put(JSONObject().apply {
                put("id", cd.id)
                put("date", cd.date)
                put("openingBalance", cd.openingBalance)
                put("closingBalance", cd.closingBalance)
                put("physicalCash", cd.physicalCash)
                put("systemCash", cd.systemCash)
                put("note", cd.note ?: "")
                put("timestamp", cd.timestamp)
            })
        }
        root.put("cash_drawer", cashArr)

        root.toString(2)
    }

    suspend fun createBackupFile(context: Context, jsonContent: String): File = withContext(Dispatchers.IO) {
        val backupDir = File(context.filesDir, "backups")
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "AlMusaed_Backup_$timeStamp.json"
        val file = File(backupDir, fileName)
        file.writeText(jsonContent, Charsets.UTF_8)
        file
    }

    fun createShareOrDriveIntent(context: Context, file: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "نسخة احتياطية - منظومة المساعد (${file.name})")
            putExtra(
                Intent.EXTRA_TEXT,
                "نسخة احتياطية شاملة لبيانات الطلاب والحضور والمدفوعات - منظومة المساعد التاريخية."
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(intent, "حفظ النسخة الاحتياطية في Google Drive أو مشاركتها")
    }

    suspend fun writeJsonToUri(context: Context, destinationUri: Uri, json: String): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                outputStream.write(json.toByteArray(Charsets.UTF_8))
                outputStream.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun readJsonFromUri(context: Context, sourceUri: Uri): String = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun parseBackupJson(jsonString: String): BackupParsedData {
        try {
            val root = JSONObject(jsonString)
            val exportDate = root.optString("exportDate", "غير معروف")
            val title = root.optString("systemName", "منظومة المساعد")

            // Students
            val studentsList = mutableListOf<StudentEntity>()
            val studentsArr = root.optJSONArray("students")
            if (studentsArr != null) {
                for (i in 0 until studentsArr.length()) {
                    val obj = studentsArr.getJSONObject(i)
                    studentsList.add(
                        StudentEntity(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            code = obj.optString("code", "ST${10000 + i}"),
                            name = obj.getString("name"),
                            groupId = obj.optString("groupId", ""),
                            grade = obj.optString("grade", "ثانوي"),
                            phone = obj.optString("phone", ""),
                            parentPhone = obj.optString("parentPhone", ""),
                            parentPhone2 = obj.optString("parentPhone2", ""),
                            parentRelation = obj.optString("parentRelation", "أب"),
                            discountType = obj.optString("discountType", "none"),
                            discountValue = obj.optDouble("discountValue", 0.0),
                            joinDate = obj.optString("joinDate", "2026-09-01"),
                            parentToken = obj.optString("parentToken", UUID.randomUUID().toString().take(8).uppercase()),
                            points = obj.optInt("points", 0),
                            badges = obj.optString("badges", ""),
                            isActive = obj.optBoolean("isActive", true)
                        )
                    )
                }
            }

            // Groups
            val groupsList = mutableListOf<GroupEntity>()
            val groupsArr = root.optJSONArray("groups")
            if (groupsArr != null) {
                for (i in 0 until groupsArr.length()) {
                    val obj = groupsArr.getJSONObject(i)
                    groupsList.add(
                        GroupEntity(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            name = obj.getString("name"),
                            stage = obj.optString("stage", "ثانوي"),
                            grade = obj.optString("grade", "الأول الثانوي"),
                            subject = obj.optString("subject", "التاريخ"),
                            location = obj.optString("location", ""),
                            capacity = obj.optInt("capacity", 30),
                            price = obj.optDouble("price", 0.0),
                            sessionsPerMonth = obj.optInt("sessionsPerMonth", 8),
                            whatsappLink = obj.optString("whatsappLink", ""),
                            scheduleJson = obj.optString("scheduleJson", "[]"),
                            extraSessionsJson = obj.optString("extraSessionsJson", "[]")
                        )
                    )
                }
            }

            // Attendance
            val attendanceList = mutableListOf<AttendanceEntity>()
            val attendanceArr = root.optJSONArray("attendance")
            if (attendanceArr != null) {
                for (i in 0 until attendanceArr.length()) {
                    val obj = attendanceArr.getJSONObject(i)
                    attendanceList.add(
                        AttendanceEntity(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            studentId = obj.getString("studentId"),
                            groupId = obj.optString("groupId", ""),
                            date = obj.getString("date"),
                            status = obj.optString("status", "present"),
                            excuseReason = obj.optString("excuseReason").takeIf { it.isNotBlank() },
                            makeupGroupId = obj.optString("makeupGroupId").takeIf { it.isNotBlank() },
                            makeupDate = obj.optString("makeupDate").takeIf { it.isNotBlank() },
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Payments
            val paymentsList = mutableListOf<PaymentEntity>()
            val paymentsArr = root.optJSONArray("payments")
            if (paymentsArr != null) {
                for (i in 0 until paymentsArr.length()) {
                    val obj = paymentsArr.getJSONObject(i)
                    paymentsList.add(
                        PaymentEntity(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            studentId = obj.getString("studentId"),
                            groupId = obj.optString("groupId", ""),
                            amount = obj.optDouble("amount", 0.0),
                            type = obj.optString("type", "monthly"),
                            otherDescription = obj.optString("otherDescription").takeIf { it.isNotBlank() },
                            targetMonth = obj.optInt("targetMonth", 1),
                            date = obj.optString("date", "2026-09-01"),
                            receiptNumber = obj.optString("receiptNumber", "REC-${1000 + i}"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Exams
            val examsList = mutableListOf<ExamEntity>()
            val examsArr = root.optJSONArray("exams")
            if (examsArr != null) {
                for (i in 0 until examsArr.length()) {
                    val obj = examsArr.getJSONObject(i)
                    examsList.add(
                        ExamEntity(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            groupId = obj.optString("groupId", ""),
                            name = obj.getString("name"),
                            maxScore = obj.optDouble("maxScore", 100.0),
                            date = obj.optString("date", "2026-09-01")
                        )
                    )
                }
            }

            // Scores
            val scoresList = mutableListOf<ExamScoreEntity>()
            val scoresArr = root.optJSONArray("exam_scores")
            if (scoresArr != null) {
                for (i in 0 until scoresArr.length()) {
                    val obj = scoresArr.getJSONObject(i)
                    scoresList.add(
                        ExamScoreEntity(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            examId = obj.getString("examId"),
                            studentId = obj.getString("studentId"),
                            score = obj.optDouble("score", 0.0)
                        )
                    )
                }
            }

            // Cash Drawer
            val cashList = mutableListOf<CashDrawerEntity>()
            val cashArr = root.optJSONArray("cash_drawer")
            if (cashArr != null) {
                for (i in 0 until cashArr.length()) {
                    val obj = cashArr.getJSONObject(i)
                    cashList.add(
                        CashDrawerEntity(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            date = obj.optString("date", "2026-09-01"),
                            openingBalance = obj.optDouble("openingBalance", 0.0),
                            closingBalance = obj.optDouble("closingBalance", 0.0),
                            physicalCash = obj.optDouble("physicalCash", 0.0),
                            systemCash = obj.optDouble("systemCash", 0.0),
                            note = obj.optString("note").takeIf { it.isNotBlank() },
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            }

            val metadata = BackupMetadata(
                isValid = true,
                title = title,
                exportDate = exportDate,
                studentsCount = studentsList.size,
                groupsCount = groupsList.size,
                attendanceCount = attendanceList.size,
                paymentsCount = paymentsList.size,
                examsCount = examsList.size,
                scoresCount = scoresList.size,
                cashDrawerCount = cashList.size,
                totalAmountPayments = paymentsList.sumOf { it.amount }
            )

            return BackupParsedData(
                metadata = metadata,
                students = studentsList,
                groups = groupsList,
                attendance = attendanceList,
                payments = paymentsList,
                exams = examsList,
                scores = scoresList,
                cashDrawer = cashList
            )
        } catch (e: Exception) {
            return BackupParsedData(
                metadata = BackupMetadata(isValid = false, errorMessage = e.localizedMessage),
                students = emptyList(),
                groups = emptyList(),
                attendance = emptyList(),
                payments = emptyList(),
                exams = emptyList(),
                scores = emptyList(),
                cashDrawer = emptyList()
            )
        }
    }

    suspend fun restoreIntoDatabase(
        db: AppDatabase,
        data: BackupParsedData,
        replaceAll: Boolean
    ): Unit = withContext(Dispatchers.IO) {
        if (replaceAll) {
            db.studentDao().clearAll()
            db.groupDao().clearAll()
            db.attendanceDao().clearAll()
            db.paymentDao().clearAll()
            db.examDao().clearAll()
            db.examScoreDao().clearAll()
            db.cashDrawerDao().clearAll()
        }

        // Insert groups first (foreign relation logical ordering)
        data.groups.forEach { db.groupDao().insertGroup(it) }
        data.students.forEach { db.studentDao().insertStudent(it) }
        data.attendance.forEach { db.attendanceDao().insertAttendance(it) }
        data.payments.forEach { db.paymentDao().insertPayment(it) }
        data.exams.forEach { db.examDao().insertExam(it) }
        data.scores.forEach { db.examScoreDao().insertScore(it) }
        data.cashDrawer.forEach { db.cashDrawerDao().insertEntry(it) }

        // Log audit
        db.auditLogDao().insertLog(
            AuditLogEntity(
                action = "restore_backup",
                entity = "system",
                entityId = "backup",
                details = "تمت استعادة ${data.students.size} طالب، ${data.payments.size} دفعة، ${data.attendance.size} سجل حضور."
            )
        )
    }
}
