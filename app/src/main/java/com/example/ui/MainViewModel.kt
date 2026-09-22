package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.AppRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class AssistantAccount(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val code: String,
    val pin: String
)

sealed class AuthState {
    object LoggedOut : AuthState()
    data class LoggedIn(val userRole: String, val name: String) : AuthState()

    val isLoggedIn: Boolean
        get() = this is LoggedIn

    val userName: String
        get() = when (this) {
            is LoggedIn -> name
            is LoggedOut -> ""
        }

    val role: String
        get() = when (this) {
            is LoggedIn -> userRole
            is LoggedOut -> ""
        }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val syncManager = com.example.data.sync.SyncManager(application, db)
    val repository = AppRepository(db, syncManager)

    // Sync Flows
    val isOnline: StateFlow<Boolean> = syncManager.isOnline
    val isSyncing: StateFlow<Boolean> = syncManager.isSyncing
    val lastSyncTimestamp: StateFlow<Long?> = syncManager.lastSyncTimestamp
    val syncMessage: StateFlow<String> = syncManager.syncMessage
    val syncState: StateFlow<com.example.data.sync.SyncState> = syncManager.syncState
    val pendingSyncCount: StateFlow<Int> = syncManager.pendingCount

    fun syncNow() {
        viewModelScope.launch {
            val result = syncManager.syncPendingQueue(isAuto = false)
            showMessage(result)
        }
    }

    // Auth State
    private val _authState = MutableStateFlow<AuthState>(AuthState.LoggedIn("معلم", "مستر محمود عوده"))
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // Assistants
    private val _assistants = MutableStateFlow<List<AssistantAccount>>(
        listOf(
            AssistantAccount(name = "أ. أحمد (مساعد)", code = "AST01", pin = "1234"),
            AssistantAccount(name = "أ. سارة (مساعد)", code = "AST02", pin = "5678")
        )
    )
    val assistants: StateFlow<List<AssistantAccount>> = _assistants.asStateFlow()

    // Data Flows
    val students: StateFlow<List<StudentEntity>> = repository.allStudents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groups: StateFlow<List<GroupEntity>> = repository.allGroups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attendance: StateFlow<List<AttendanceEntity>> = repository.allAttendance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val payments: StateFlow<List<PaymentEntity>> = repository.allPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exams: StateFlow<List<ExamEntity>> = repository.allExams
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val examScores: StateFlow<List<ExamScoreEntity>> = repository.allScores
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<TaskEntity>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val submissions: StateFlow<List<SubmissionEntity>> = repository.allSubmissions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cashDrawer: StateFlow<List<CashDrawerEntity>> = repository.allCashDrawer
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shifts: StateFlow<List<CashDrawerEntity>> = cashDrawer

    val auditLogs: StateFlow<List<AuditLogEntity>> = repository.recentAuditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Feedback state (snackbar / toast message)
    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    fun clearUiMessage() {
        _uiMessage.value = null
    }

    fun showMessage(msg: String) {
        _uiMessage.value = msg
    }

    // Auth methods
    fun login(username: String, pass: String): Boolean {
        val u = username.trim()
        val p = pass.trim()
        if (u == "1" && p == "1") {
            _authState.value = AuthState.LoggedIn("teacher", "مستر محمود عوده")
            showMessage("مرحباً بك مستر محمود عوده")
            return true
        }

        val foundAsst = _assistants.value.find { it.code.equals(u, ignoreCase = true) && it.pin == p }
        if (foundAsst != null) {
            _authState.value = AuthState.LoggedIn("assistant", foundAsst.name)
            showMessage("تم تسجيل دخول المساعد: ${foundAsst.name}")
            return true
        }

        showMessage("كود المستخدم أو كلمة المرور غير صحيحة")
        return false
    }

    fun loginTeacher(id: String, pass: String): Boolean = login(id, pass)
    fun loginAssistant(code: String, pin: String): Boolean = login(code, pin)

    fun logout() {
        _authState.value = AuthState.LoggedOut
    }

    fun addAssistant(name: String, code: String, pin: String) {
        val newAsst = AssistantAccount(name = name, code = code, pin = pin)
        _assistants.value = _assistants.value + newAsst
        showMessage("تم إضافة المساعد $name بنجاح")
    }

    fun deleteAssistant(assistant: AssistantAccount) {
        _assistants.value = _assistants.value.filter { it.id != assistant.id }
        showMessage("تم حذف حساب المساعد")
    }

    // Student Operations
    fun addStudent(
        name: String,
        groupId: String,
        grade: String,
        phone: String,
        parentPhone: String,
        parentPhone2: String = "",
        parentRelation: String = "أب",
        discountType: String = "none",
        discountValue: Double = 0.0,
        joinDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    ) {
        viewModelScope.launch {
            try {
                val student = repository.addStudent(
                    name = name,
                    groupId = groupId,
                    grade = grade,
                    phone = phone,
                    parentPhone = parentPhone,
                    parentPhone2 = parentPhone2,
                    parentRelation = parentRelation,
                    discountType = discountType,
                    discountValue = discountValue,
                    joinDate = joinDate
                )
                showMessage("تم إضافة الطالب ${student.name} بكود ${student.code}")
            } catch (e: Exception) {
                showMessage("خطأ في إضافة الطالب: ${e.localizedMessage}")
            }
        }
    }

    fun updateStudent(student: StudentEntity) {
        viewModelScope.launch {
            repository.updateStudent(student)
            showMessage("تم تعديل بيانات الطالب ${student.name}")
        }
    }

    fun deleteStudent(student: StudentEntity) {
        viewModelScope.launch {
            repository.deleteStudent(student)
            showMessage("تم حذف الطالب ${student.name}")
        }
    }

    fun bulkImport(studentsList: List<StudentEntity>) {
        viewModelScope.launch {
            repository.bulkImportStudents(studentsList)
            showMessage("تم استيراد ${studentsList.size} طالب بنجاح")
        }
    }

    fun awardPointsAndBadge(studentId: String, points: Int, badgeId: String?, badgeTitle: String) {
        viewModelScope.launch {
            repository.awardPoints(studentId, points, badgeId)
            showMessage("تم منح $badgeTitle (+ $points نقطة)")
        }
    }

    // Group Operations
    fun addGroup(group: GroupEntity) {
        viewModelScope.launch {
            repository.addGroup(group)
            showMessage("تم إنشاء المجموعة ${group.name}")
        }
    }

    fun updateGroup(group: GroupEntity) {
        viewModelScope.launch {
            repository.updateGroup(group)
            showMessage("تم تعديل المجموعة ${group.name}")
        }
    }

    fun deleteGroup(group: GroupEntity) {
        viewModelScope.launch {
            repository.deleteGroup(group)
            showMessage("تم حذف المجموعة ${group.name}")
        }
    }

    // Attendance Operations
    fun markAttendance(
        studentId: String,
        groupId: String,
        date: String,
        status: String,
        excuseReason: String? = null,
        makeupGroupId: String? = null,
        makeupDate: String? = null
    ) {
        viewModelScope.launch {
            repository.markAttendance(
                studentId, groupId, date, status, excuseReason, makeupGroupId, makeupDate
            )
            val stName = students.value.find { it.id == studentId }?.name ?: ""
            val statusAr = when (status) {
                "present" -> "حاضر ✅"
                "late" -> "متأخر ⏰"
                "absent" -> "غائب ❌"
                "excused" -> "اعتذر 💙"
                else -> status
            }
            showMessage("$stName: $statusAr")
        }
    }

    fun deleteAttendanceRecord(id: String) {
        viewModelScope.launch {
            repository.deleteAttendance(id)
            showMessage("تم إلغاء تسجيل الحضور")
        }
    }

    // Payment Operations
    fun addPayment(
        studentId: String,
        groupId: String,
        amount: Double,
        type: String,
        otherDescription: String? = null,
        targetMonth: Int,
        date: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
        onSuccess: (PaymentEntity) -> Unit = {}
    ) {
        viewModelScope.launch {
            val payment = repository.addPayment(
                studentId, groupId, amount, type, otherDescription, targetMonth, date
            )
            showMessage("تم تسجيل دفعة بقيمة $amount ج.م (إيصال ${payment.receiptNumber})")
            onSuccess(payment)
        }
    }

    fun deletePayment(payment: PaymentEntity) {
        viewModelScope.launch {
            repository.deletePayment(payment)
            showMessage("تم حذف الإيصال ${payment.receiptNumber}")
        }
    }

    // Exam Operations
    fun addExam(exam: ExamEntity) {
        viewModelScope.launch {
            repository.addExam(exam)
            showMessage("تم إنشاء الامتحان ${exam.name}")
        }
    }

    fun deleteExam(exam: ExamEntity) {
        viewModelScope.launch {
            repository.deleteExam(exam)
            showMessage("تم حذف الامتحان ${exam.name}")
        }
    }

    fun setExamScore(examId: String, studentId: String, score: Double) {
        viewModelScope.launch {
            repository.setScore(examId, studentId, score)
        }
    }

    // Cash Drawer
    fun closeCashDrawer(
        date: String,
        openingBalance: Double,
        closingBalance: Double,
        physicalCash: Double,
        systemCash: Double,
        note: String? = null
    ) {
        viewModelScope.launch {
            val entry = CashDrawerEntity(
                date = date,
                openingBalance = openingBalance,
                closingBalance = closingBalance,
                physicalCash = physicalCash,
                systemCash = systemCash,
                note = note
            )
            repository.addCashDrawerEntry(entry)
            showMessage("تم إغلاق الدرج بنجاح ليوم $date")
        }
    }

    fun closeCashShift(
        openingBalance: Double,
        systemTotal: Double,
        physicalCount: Double,
        notes: String
    ) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        closeCashDrawer(
            date = today,
            openingBalance = openingBalance,
            closingBalance = physicalCount,
            physicalCash = physicalCount,
            systemCash = systemTotal,
            note = notes
        )
    }

    // Comprehensive Backup & Restore
    suspend fun exportFullBackupJson(): String {
        return com.example.data.backup.BackupManager.generateFullBackupJson(db)
    }

    suspend fun createLocalBackupFile(): java.io.File {
        val json = exportFullBackupJson()
        return com.example.data.backup.BackupManager.createBackupFile(getApplication(), json)
    }

    fun shareBackup(file: java.io.File) {
        val intent = com.example.data.backup.BackupManager.createShareOrDriveIntent(getApplication(), file)
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(intent)
    }

    suspend fun saveBackupToUri(destinationUri: android.net.Uri): Boolean {
        val json = exportFullBackupJson()
        return com.example.data.backup.BackupManager.writeJsonToUri(getApplication(), destinationUri, json)
    }

    suspend fun readBackupFromUri(sourceUri: android.net.Uri): String {
        return com.example.data.backup.BackupManager.readJsonFromUri(getApplication(), sourceUri)
    }

    fun parseBackup(json: String): com.example.data.backup.BackupParsedData {
        return com.example.data.backup.BackupManager.parseBackupJson(json)
    }

    fun restoreFullBackup(
        data: com.example.data.backup.BackupParsedData,
        replaceAll: Boolean,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (!data.metadata.isValid) {
                    val err = data.metadata.errorMessage ?: "بيانات النسخة غير صالحة"
                    showMessage(err)
                    onComplete(false, err)
                    return@launch
                }
                com.example.data.backup.BackupManager.restoreIntoDatabase(db, data, replaceAll)
                val msg = "تمت استعادة ${data.students.size} طالب و ${data.payments.size} دفعة و ${data.attendance.size} سجل حضور بنجاح ✅"
                showMessage(msg)
                onComplete(true, msg)
            } catch (e: Exception) {
                val err = "حدث خطأ أثناء الاستعادة: ${e.localizedMessage}"
                showMessage(err)
                onComplete(false, err)
            }
        }
    }

    // Legacy JSON quick export (students only)
    fun exportBackupJson(): String {
        val root = JSONObject()
        val studentsArr = JSONArray()
        students.value.forEach { s ->
            studentsArr.put(JSONObject().apply {
                put("id", s.id)
                put("name", s.name)
                put("code", s.code)
                put("groupId", s.groupId)
                put("grade", s.grade)
                put("phone", s.phone)
                put("parentPhone", s.parentPhone)
            })
        }
        root.put("students", studentsArr)
        return root.toString(2)
    }

    fun importBackupJson(jsonString: String) {
        viewModelScope.launch {
            try {
                val parsed = com.example.data.backup.BackupManager.parseBackupJson(jsonString)
                if (parsed.metadata.isValid && (parsed.payments.isNotEmpty() || parsed.attendance.isNotEmpty())) {
                    restoreFullBackup(parsed, replaceAll = false) { _, _ -> }
                    return@launch
                }

                // Fallback for simple legacy json
                val root = JSONObject(jsonString)
                val arr = root.optJSONArray("students") ?: return@launch
                val list = mutableListOf<StudentEntity>()
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        StudentEntity(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            name = obj.getString("name"),
                            code = obj.getString("code"),
                            groupId = obj.optString("groupId", ""),
                            grade = obj.optString("grade", "ثانوي"),
                            phone = obj.optString("phone", ""),
                            parentPhone = obj.optString("parentPhone", ""),
                            joinDate = today
                        )
                    )
                }
                repository.bulkImportStudents(list)
                showMessage("تم استعادة ${list.size} طالب بنجاح")
            } catch (e: Exception) {
                showMessage("خطأ في قراءة ملف النسخة: ${e.localizedMessage}")
            }
        }
    }

    // Wipe
    fun wipeData() {
        viewModelScope.launch {
            repository.clearAllData()
            showMessage("تم مسح جميع البيانات بنجاح")
        }
    }

    fun wipeAllData() = wipeData()
}
