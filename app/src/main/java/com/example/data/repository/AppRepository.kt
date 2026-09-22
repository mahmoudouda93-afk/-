package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.sync.SyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class AppRepository(
    private val db: AppDatabase,
    var syncManager: SyncManager? = null
) {

    // Students
    val allStudents: Flow<List<StudentEntity>> = db.studentDao().getAllStudents()

    fun getStudentsByGroup(groupId: String): Flow<List<StudentEntity>> =
        db.studentDao().getStudentsByGroup(groupId)

    suspend fun getStudentById(id: String): StudentEntity? =
        db.studentDao().getStudentById(id)

    suspend fun getStudentByCode(code: String): StudentEntity? =
        db.studentDao().getStudentByCode(code.trim())

    suspend fun getStudentByParentToken(token: String): StudentEntity? =
        db.studentDao().getStudentByParentToken(token.trim())

    suspend fun generateNextStudentCode(): String {
        val current = db.studentDao().getAllStudents().first()
        val numbers = current.mapNotNull { st ->
            if (st.code.startsWith("ST")) {
                st.code.removePrefix("ST").toIntOrNull()
            } else null
        }
        val maxNum = if (numbers.isNotEmpty()) numbers.maxOrNull() ?: 10000 else 10000
        return "ST${maxNum + 1}"
    }

    suspend fun addStudent(
        name: String,
        groupId: String,
        grade: String,
        phone: String,
        parentPhone: String,
        parentPhone2: String = "",
        parentRelation: String = "أب",
        discountType: String = "none",
        discountValue: Double = 0.0,
        joinDate: String
    ): StudentEntity {
        val code = generateNextStudentCode()
        val student = StudentEntity(
            code = code,
            name = name.trim(),
            groupId = groupId,
            grade = grade,
            phone = phone.trim(),
            parentPhone = parentPhone.trim(),
            parentPhone2 = parentPhone2.trim(),
            parentRelation = parentRelation,
            discountType = discountType,
            discountValue = discountValue,
            joinDate = joinDate
        )
        db.studentDao().insertStudent(student)
        val stPayload = JSONObject().apply {
            put("id", student.id)
            put("code", student.code)
            put("name", student.name)
            put("groupId", student.groupId)
            put("grade", student.grade)
            put("phone", student.phone)
            put("parentPhone", student.parentPhone)
            put("parentPhone2", student.parentPhone2)
            put("parentRelation", student.parentRelation)
            put("discountType", student.discountType)
            put("discountValue", student.discountValue)
            put("joinDate", student.joinDate)
            put("parentToken", student.parentToken)
            put("points", student.points)
            put("badges", student.badges)
            put("isActive", student.isActive)
        }
        syncManager?.queueUpsert("student", student.id, stPayload)
        logAudit("add", "student", student.id, "إضافة طالب: ${student.name} ($code)")
        return student
    }

    suspend fun updateStudent(student: StudentEntity) {
        db.studentDao().updateStudent(student)
        val stPayload = JSONObject().apply {
            put("id", student.id)
            put("code", student.code)
            put("name", student.name)
            put("groupId", student.groupId)
            put("grade", student.grade)
            put("phone", student.phone)
            put("parentPhone", student.parentPhone)
            put("parentPhone2", student.parentPhone2)
            put("parentRelation", student.parentRelation)
            put("discountType", student.discountType)
            put("discountValue", student.discountValue)
            put("joinDate", student.joinDate)
            put("parentToken", student.parentToken)
            put("points", student.points)
            put("badges", student.badges)
            put("isActive", student.isActive)
        }
        syncManager?.queueUpsert("student", student.id, stPayload)
        logAudit("update", "student", student.id, "تعديل طالب: ${student.name}")
    }

    suspend fun deleteStudent(student: StudentEntity) {
        db.studentDao().deleteStudent(student)
        syncManager?.queueDelete("student", student.id)
        logAudit("delete", "student", student.id, "حذف طالب: ${student.name}")
    }

    suspend fun bulkImportStudents(imported: List<StudentEntity>) {
        db.studentDao().insertStudents(imported)
        logAudit("bulk_import", "student", "bulk", "استيراد جماعي لـ ${imported.size} طالب")
    }

    suspend fun awardPoints(studentId: String, pointsToAdd: Int, newBadgeId: String? = null) {
        val student = db.studentDao().getStudentById(studentId) ?: return
        val currentBadges = student.badges.split(",").filter { it.isNotBlank() }.toMutableList()
        if (newBadgeId != null && !currentBadges.contains(newBadgeId)) {
            currentBadges.add(newBadgeId)
        }
        val updated = student.copy(
            points = student.points + pointsToAdd,
            badges = currentBadges.joinToString(",")
        )
        db.studentDao().updateStudent(updated)
    }

    // Groups
    val allGroups: Flow<List<GroupEntity>> = db.groupDao().getAllGroups()

    suspend fun getGroupById(id: String): GroupEntity? =
        db.groupDao().getGroupById(id)

    suspend fun addGroup(group: GroupEntity): GroupEntity {
        db.groupDao().insertGroup(group)
        val grpPayload = JSONObject().apply {
            put("id", group.id)
            put("name", group.name)
            put("stage", group.stage)
            put("grade", group.grade)
            put("subject", group.subject)
            put("location", group.location)
            put("capacity", group.capacity)
            put("price", group.price)
            put("sessionsPerMonth", group.sessionsPerMonth)
            put("whatsappLink", group.whatsappLink)
            put("scheduleJson", group.scheduleJson)
            put("extraSessionsJson", group.extraSessionsJson)
        }
        syncManager?.queueUpsert("group", group.id, grpPayload)
        logAudit("add", "group", group.id, "إنشاء مجموعة: ${group.name}")
        return group
    }

    suspend fun updateGroup(group: GroupEntity) {
        db.groupDao().updateGroup(group)
        val grpPayload = JSONObject().apply {
            put("id", group.id)
            put("name", group.name)
            put("stage", group.stage)
            put("grade", group.grade)
            put("subject", group.subject)
            put("location", group.location)
            put("capacity", group.capacity)
            put("price", group.price)
            put("sessionsPerMonth", group.sessionsPerMonth)
            put("whatsappLink", group.whatsappLink)
            put("scheduleJson", group.scheduleJson)
            put("extraSessionsJson", group.extraSessionsJson)
        }
        syncManager?.queueUpsert("group", group.id, grpPayload)
        logAudit("update", "group", group.id, "تعديل مجموعة: ${group.name}")
    }

    suspend fun deleteGroup(group: GroupEntity) {
        db.groupDao().deleteGroup(group)
        syncManager?.queueDelete("group", group.id)
        logAudit("delete", "group", group.id, "حذف مجموعة: ${group.name}")
    }

    // Attendance
    val allAttendance: Flow<List<AttendanceEntity>> = db.attendanceDao().getAllAttendance()

    fun getAttendanceForGroupAndDate(groupId: String, date: String): Flow<List<AttendanceEntity>> =
        db.attendanceDao().getAttendanceForGroupAndDate(groupId, date)

    fun getAttendanceForStudent(studentId: String): Flow<List<AttendanceEntity>> =
        db.attendanceDao().getAttendanceForStudent(studentId)

    suspend fun markAttendance(
        studentId: String,
        groupId: String,
        date: String,
        status: String,
        excuseReason: String? = null,
        makeupGroupId: String? = null,
        makeupDate: String? = null
    ): AttendanceEntity {
        val existing = db.attendanceDao().findExistingRecord(studentId, groupId, date)
        val entity = if (existing != null) {
            existing.copy(
                status = status,
                excuseReason = excuseReason,
                makeupGroupId = makeupGroupId,
                makeupDate = makeupDate,
                timestamp = System.currentTimeMillis()
            )
        } else {
            AttendanceEntity(
                studentId = studentId,
                groupId = groupId,
                date = date,
                status = status,
                excuseReason = excuseReason,
                makeupGroupId = makeupGroupId,
                makeupDate = makeupDate
            )
        }
        db.attendanceDao().insertAttendance(entity)
        val attPayload = JSONObject().apply {
            put("id", entity.id)
            put("studentId", entity.studentId)
            put("groupId", entity.groupId)
            put("date", entity.date)
            put("status", entity.status)
            put("excuseReason", entity.excuseReason ?: "")
            put("makeupGroupId", entity.makeupGroupId ?: "")
            put("makeupDate", entity.makeupDate ?: "")
            put("timestamp", entity.timestamp)
        }
        syncManager?.queueUpsert("attendance", entity.id, attPayload)
        return entity
    }

    suspend fun deleteAttendance(id: String) {
        db.attendanceDao().deleteAttendanceById(id)
        syncManager?.queueDelete("attendance", id)
    }

    // Payments
    val allPayments: Flow<List<PaymentEntity>> = db.paymentDao().getAllPayments()

    fun getPaymentsByGroup(groupId: String): Flow<List<PaymentEntity>> =
        db.paymentDao().getPaymentsByGroup(groupId)

    fun getPaymentsByStudent(studentId: String): Flow<List<PaymentEntity>> =
        db.paymentDao().getPaymentsByStudent(studentId)

    suspend fun generateNextReceiptNumber(): String {
        val count = db.paymentDao().getPaymentCount()
        return "REC-${1001 + count}"
    }

    suspend fun addPayment(
        studentId: String,
        groupId: String,
        amount: Double,
        type: String,
        otherDescription: String? = null,
        targetMonth: Int,
        date: String
    ): PaymentEntity {
        val receiptNumber = generateNextReceiptNumber()
        val payment = PaymentEntity(
            studentId = studentId,
            groupId = groupId,
            amount = amount,
            type = type,
            otherDescription = otherDescription,
            targetMonth = targetMonth,
            date = date,
            receiptNumber = receiptNumber
        )
        db.paymentDao().insertPayment(payment)
        val payPayload = JSONObject().apply {
            put("id", payment.id)
            put("studentId", payment.studentId)
            put("groupId", payment.groupId)
            put("amount", payment.amount)
            put("type", payment.type)
            put("otherDescription", payment.otherDescription ?: "")
            put("targetMonth", payment.targetMonth)
            put("date", payment.date)
            put("receiptNumber", payment.receiptNumber)
            put("timestamp", payment.timestamp)
        }
        syncManager?.queueUpsert("payment", payment.id, payPayload)
        logAudit("add", "payment", payment.id, "تسجيل دفعة: $amount ج.م ($receiptNumber)")
        return payment
    }

    suspend fun deletePayment(payment: PaymentEntity) {
        db.paymentDao().deletePayment(payment)
        syncManager?.queueDelete("payment", payment.id)
        logAudit("delete", "payment", payment.id, "حذف دفعة: ${payment.amount} ج.م (${payment.receiptNumber})")
    }

    // Exams
    val allExams: Flow<List<ExamEntity>> = db.examDao().getAllExams()

    fun getExamsByGroup(groupId: String): Flow<List<ExamEntity>> =
        db.examDao().getExamsByGroup(groupId)

    suspend fun addExam(exam: ExamEntity): ExamEntity {
        db.examDao().insertExam(exam)
        return exam
    }

    suspend fun deleteExam(exam: ExamEntity) {
        db.examDao().deleteExam(exam)
        db.examScoreDao().deleteScoresByExamId(exam.id)
    }

    val allScores: Flow<List<ExamScoreEntity>> = db.examScoreDao().getAllScores()

    fun getScoresForExam(examId: String): Flow<List<ExamScoreEntity>> =
        db.examScoreDao().getScoresForExam(examId)

    suspend fun setScore(examId: String, studentId: String, score: Double) {
        val entity = ExamScoreEntity(id = "${examId}_${studentId}", examId = examId, studentId = studentId, score = score)
        db.examScoreDao().insertScore(entity)
    }

    // Tasks & Submissions
    val allTasks: Flow<List<TaskEntity>> = db.taskDao().getAllTasks()
    val allSubmissions: Flow<List<SubmissionEntity>> = db.submissionDao().getAllSubmissions()

    fun getTasksByGroup(groupId: String): Flow<List<TaskEntity>> =
        db.taskDao().getTasksByGroup(groupId)

    suspend fun addTask(task: TaskEntity) = db.taskDao().insertTask(task)
    suspend fun updateTask(task: TaskEntity) = db.taskDao().updateTask(task)
    suspend fun deleteTask(task: TaskEntity) = db.taskDao().deleteTask(task)

    fun getSubmissionsByStudent(studentId: String): Flow<List<SubmissionEntity>> =
        db.submissionDao().getSubmissionsByStudent(studentId)

    suspend fun setSubmission(taskId: String, studentId: String, submitted: Boolean) =
        db.submissionDao().insertSubmission(SubmissionEntity(taskId = taskId, studentId = studentId, submitted = submitted))

    // Session Notes
    fun getSessionNotesByGroup(groupId: String): Flow<List<SessionNoteEntity>> =
        db.sessionNoteDao().getNotesByGroup(groupId)

    suspend fun addSessionNote(note: SessionNoteEntity) = db.sessionNoteDao().insertNote(note)
    suspend fun deleteSessionNote(note: SessionNoteEntity) = db.sessionNoteDao().deleteNote(note)

    // Cash Drawer
    val allCashDrawer: Flow<List<CashDrawerEntity>> = db.cashDrawerDao().getAllEntries()

    suspend fun addCashDrawerEntry(entry: CashDrawerEntity) {
        db.cashDrawerDao().insertEntry(entry)
        logAudit("add", "cash_drawer", entry.id, "إغلاق خزينة: فعلي ${entry.physicalCash} ج.م | نظام ${entry.systemCash} ج.م")
    }

    // Audit
    val recentAuditLogs: Flow<List<AuditLogEntity>> = db.auditLogDao().getRecentLogs()

    suspend fun logAudit(action: String, entity: String, entityId: String, details: String, userRole: String = "معلم") {
        db.auditLogDao().insertLog(
            AuditLogEntity(
                action = action,
                entity = entity,
                entityId = entityId,
                details = details,
                userRole = userRole
            )
        )
    }

    // Clear All Data
    suspend fun clearAllData() {
        db.studentDao().clearAll()
        db.groupDao().clearAll()
        db.attendanceDao().clearAll()
        db.paymentDao().clearAll()
        db.examDao().clearAll()
        db.examScoreDao().clearAll()
        db.taskDao().clearAll()
        db.submissionDao().clearAll()
        db.sessionNoteDao().clearAll()
        db.cashDrawerDao().clearAll()
        db.auditLogDao().clearAll()
    }
}
