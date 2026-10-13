package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ScheduledPost
import com.example.data.model.WorkflowTask
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM workflow_tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<WorkflowTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: WorkflowTask)

    @Update
    suspend fun updateTask(task: WorkflowTask)

    @Query("DELETE FROM workflow_tasks WHERE id = :id")
    suspend fun deleteTask(id: String)

    @Query("SELECT * FROM scheduled_posts ORDER BY id DESC")
    fun getAllScheduledPosts(): Flow<List<ScheduledPost>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduledPost(post: ScheduledPost)

    @Query("DELETE FROM scheduled_posts WHERE id = :id")
    suspend fun deleteScheduledPost(id: String)
}
