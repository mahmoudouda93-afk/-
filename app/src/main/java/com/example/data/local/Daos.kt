package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Query("SELECT * FROM students ORDER BY name ASC")
    fun getAllStudents(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE groupId = :groupId ORDER BY name ASC")
    fun getStudentsByGroup(groupId: String): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    suspend fun getStudentById(id: String): StudentEntity?

    @Query("SELECT * FROM students WHERE code = :code LIMIT 1")
    suspend fun getStudentByCode(code: String): StudentEntity?

    @Query("SELECT * FROM students WHERE parentToken = :token LIMIT 1")
    suspend fun getStudentByParentToken(token: String): StudentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<StudentEntity>)

    @Update
    suspend fun updateStudent(student: StudentEntity)

    @Delete
    suspend fun deleteStudent(student: StudentEntity)

    @Query("DELETE FROM students WHERE id = :id")
    suspend fun deleteStudentById(id: String)

    @Query("DELETE FROM students")
    suspend fun clearAll()
}

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups ORDER BY name ASC")
    fun getAllGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE id = :id LIMIT 1")
    suspend fun getGroupById(id: String): GroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity)

    @Update
    suspend fun updateGroup(group: GroupEntity)

    @Delete
    suspend fun deleteGroup(group: GroupEntity)

    @Query("DELETE FROM groups WHERE id = :id")
    suspend fun deleteGroupById(id: String)

    @Query("DELETE FROM groups")
    suspend fun clearAll()
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance ORDER BY timestamp DESC")
    fun getAllAttendance(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE groupId = :groupId AND date = :date")
    fun getAttendanceForGroupAndDate(groupId: String, date: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE studentId = :studentId")
    fun getAttendanceForStudent(studentId: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE studentId = :studentId AND groupId = :groupId AND date = :date LIMIT 1")
    suspend fun findExistingRecord(studentId: String, groupId: String, date: String): AttendanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: AttendanceEntity)

    @Update
    suspend fun updateAttendance(attendance: AttendanceEntity)

    @Query("DELETE FROM attendance WHERE id = :id")
    suspend fun deleteAttendanceById(id: String)

    @Query("DELETE FROM attendance")
    suspend fun clearAll()
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY timestamp DESC")
    fun getAllPayments(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE groupId = :groupId ORDER BY timestamp DESC")
    fun getPaymentsByGroup(groupId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE studentId = :studentId ORDER BY timestamp DESC")
    fun getPaymentsByStudent(studentId: String): Flow<List<PaymentEntity>>

    @Query("SELECT COUNT(*) FROM payments")
    suspend fun getPaymentCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    @Delete
    suspend fun deletePayment(payment: PaymentEntity)

    @Query("DELETE FROM payments")
    suspend fun clearAll()
}

@Dao
interface ExamDao {
    @Query("SELECT * FROM exams ORDER BY date DESC")
    fun getAllExams(): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams WHERE groupId = :groupId ORDER BY date DESC")
    fun getExamsByGroup(groupId: String): Flow<List<ExamEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamEntity)

    @Delete
    suspend fun deleteExam(exam: ExamEntity)

    @Query("DELETE FROM exams WHERE id = :id")
    suspend fun deleteExamById(id: String)

    @Query("DELETE FROM exams")
    suspend fun clearAll()
}

@Dao
interface ExamScoreDao {
    @Query("SELECT * FROM exam_scores")
    fun getAllScores(): Flow<List<ExamScoreEntity>>

    @Query("SELECT * FROM exam_scores WHERE examId = :examId")
    fun getScoresForExam(examId: String): Flow<List<ExamScoreEntity>>

    @Query("SELECT * FROM exam_scores WHERE studentId = :studentId")
    fun getScoresForStudent(studentId: String): Flow<List<ExamScoreEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(score: ExamScoreEntity)

    @Query("DELETE FROM exam_scores WHERE examId = :examId")
    suspend fun deleteScoresByExamId(examId: String)

    @Query("DELETE FROM exam_scores")
    suspend fun clearAll()
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY title ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE groupId = :groupId ORDER BY title ASC")
    fun getTasksByGroup(groupId: String): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks")
    suspend fun clearAll()
}

@Dao
interface SubmissionDao {
    @Query("SELECT * FROM submissions")
    fun getAllSubmissions(): Flow<List<SubmissionEntity>>

    @Query("SELECT * FROM submissions WHERE studentId = :studentId")
    fun getSubmissionsByStudent(studentId: String): Flow<List<SubmissionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubmission(submission: SubmissionEntity)

    @Query("DELETE FROM submissions")
    suspend fun clearAll()
}

@Dao
interface SessionNoteDao {
    @Query("SELECT * FROM session_notes WHERE groupId = :groupId ORDER BY date DESC")
    fun getNotesByGroup(groupId: String): Flow<List<SessionNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: SessionNoteEntity)

    @Delete
    suspend fun deleteNote(note: SessionNoteEntity)

    @Query("DELETE FROM session_notes WHERE id = :id")
    suspend fun deleteNoteById(id: String)

    @Query("DELETE FROM session_notes")
    suspend fun clearAll()
}

@Dao
interface CashDrawerDao {
    @Query("SELECT * FROM cash_drawer ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<CashDrawerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: CashDrawerEntity)

    @Query("DELETE FROM cash_drawer")
    suspend fun clearAll()
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLogEntity)

    @Query("DELETE FROM audit_logs")
    suspend fun clearAll()
}

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue WHERE status = 'pending' ORDER BY createdAt ASC")
    fun getPendingQueue(): Flow<List<SyncQueueEntity>>

    @Query("SELECT * FROM sync_queue WHERE status = 'pending' ORDER BY createdAt ASC")
    suspend fun getPendingQueueList(): List<SyncQueueEntity>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'pending'")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'pending'")
    suspend fun getPendingCountDirect(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncItem(item: SyncQueueEntity)

    @Update
    suspend fun updateSyncItem(item: SyncQueueEntity)

    @Query("UPDATE sync_queue SET status = :status, errorMessage = :error WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, error: String? = null)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteSyncItem(id: String)

    @Query("DELETE FROM sync_queue WHERE status = 'synced'")
    suspend fun clearSynced()

    @Query("DELETE FROM sync_queue")
    suspend fun clearAll()
}
