package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.model.DocumentFormat
import com.example.data.model.DocumentItem
import com.example.data.model.DocumentType
import com.example.data.model.ScheduledPost
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.data.model.WorkflowTask

class Converters {
    @TypeConverter
    fun fromDocumentType(value: DocumentType): String = value.name

    @TypeConverter
    fun toDocumentType(value: String): DocumentType = DocumentType.valueOf(value)

    @TypeConverter
    fun fromDocumentFormat(value: DocumentFormat): String = value.name

    @TypeConverter
    fun toDocumentFormat(value: String): DocumentFormat = DocumentFormat.valueOf(value)

    @TypeConverter
    fun fromTaskStatus(value: TaskStatus): String = value.name

    @TypeConverter
    fun toTaskStatus(value: String): TaskStatus = TaskStatus.valueOf(value)

    @TypeConverter
    fun fromTaskPriority(value: TaskPriority): String = value.name

    @TypeConverter
    fun toTaskPriority(value: String): TaskPriority = TaskPriority.valueOf(value)
}

@Database(
    entities = [
        ChatSession::class,
        ChatMessage::class,
        DocumentItem::class,
        WorkflowTask::class,
        ScheduledPost::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun documentDao(): DocumentDao
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gemini_omni_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
