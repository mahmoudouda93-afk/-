package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val code: String, // ST10001
    val name: String,
    val groupId: String,
    val grade: String,
    val phone: String,
    val parentPhone: String,
    val parentPhone2: String = "",
    val parentRelation: String = "أب",
    val discountType: String = "none", // "none", "fixed", "percentage"
    val discountValue: Double = 0.0,
    val joinDate: String,
    val parentToken: String = UUID.randomUUID().toString().take(8).uppercase(),
    val points: Int = 0,
    val badges: String = "", // Comma-separated badge IDs
    val isActive: Boolean = true
)

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val stage: String = "ثانوي",
    val grade: String,
    val subject: String = "التاريخ",
    val location: String = "",
    val capacity: Int = 30,
    val price: Double = 0.0,
    val sessionsPerMonth: Int = 8,
    val whatsappLink: String = "",
    val scheduleJson: String = "[]", // JSON array of {day, from, to}
    val extraSessionsJson: String = "[]" // JSON array of {id, date, time, title}
)

@Entity(tableName = "attendance")
data class AttendanceEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val studentId: String,
    val groupId: String,
    val date: String, // yyyy-MM-dd
    val status: String, // "present", "late", "absent", "excused"
    val excuseReason: String? = null,
    val makeupGroupId: String? = null,
    val makeupDate: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val studentId: String,
    val groupId: String,
    val amount: Double,
    val type: String, // "monthly", "half_monthly", "explanation_notes", "review_notes", "other"
    val otherDescription: String? = null,
    val targetMonth: Int = 1, // 1 to 12
    val date: String, // yyyy-MM-dd
    val receiptNumber: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val groupId: String,
    val name: String,
    val maxScore: Double = 100.0,
    val date: String
)

@Entity(tableName = "exam_scores")
data class ExamScoreEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val examId: String,
    val studentId: String,
    val score: Double
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val groupId: String,
    val title: String,
    val type: String = "teacher", // "teacher", "student"
    val dueDate: String? = null,
    val completed: Boolean = false
)

@Entity(tableName = "submissions")
data class SubmissionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val taskId: String,
    val studentId: String,
    val submitted: Boolean = true,
    val submittedAt: String = ""
)

@Entity(tableName = "session_notes")
data class SessionNoteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val groupId: String,
    val date: String,
    val title: String,
    val notes: String
)

@Entity(tableName = "cash_drawer")
data class CashDrawerEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val date: String,
    val openingBalance: Double = 0.0,
    val closingBalance: Double = 0.0,
    val physicalCash: Double = 0.0,
    val systemCash: Double = 0.0,
    val note: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val action: String,
    val entity: String,
    val entityId: String,
    val details: String,
    val userRole: String = "معلم",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val entityType: String, // "attendance", "payment", "student", "group", "exam", "score"
    val entityId: String,
    val operation: String = "UPSERT", // "UPSERT", "DELETE"
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "pending", // "pending", "syncing", "synced", "failed"
    val retryCount: Int = 0,
    val errorMessage: String? = null
)
