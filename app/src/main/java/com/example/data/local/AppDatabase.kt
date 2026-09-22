package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.*

@Database(
    entities = [
        StudentEntity::class,
        GroupEntity::class,
        AttendanceEntity::class,
        PaymentEntity::class,
        ExamEntity::class,
        ExamScoreEntity::class,
        TaskEntity::class,
        SubmissionEntity::class,
        SessionNoteEntity::class,
        CashDrawerEntity::class,
        AuditLogEntity::class,
        SyncQueueEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao
    abstract fun groupDao(): GroupDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun paymentDao(): PaymentDao
    abstract fun examDao(): ExamDao
    abstract fun examScoreDao(): ExamScoreDao
    abstract fun taskDao(): TaskDao
    abstract fun submissionDao(): SubmissionDao
    abstract fun sessionNoteDao(): SessionNoteDao
    abstract fun cashDrawerDao(): CashDrawerDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "almusaed_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
