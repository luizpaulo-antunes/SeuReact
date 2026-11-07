package com.seureact.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReactionDao {
    @Query("SELECT * FROM projects ORDER BY updatedAtEpochMs DESC")
    fun observeProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun observeProject(id: String): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProject(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProject(project: ProjectEntity)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("UPDATE projects SET status = :status, lastError = :error, updatedAtEpochMs = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, error: String?, updatedAt: Long)

    @Query("UPDATE projects SET status = 'INTERRUPTED', updatedAtEpochMs = :updatedAt WHERE status = 'RECORDING'")
    suspend fun markRecordingProjectsInterrupted(updatedAt: Long)

    @Insert
    suspend fun insertEvent(event: TimelineEventEntity)

    @Query("SELECT * FROM timeline_events WHERE projectId = :projectId ORDER BY finalTimeNanos, id")
    suspend fun getEvents(projectId: String): List<TimelineEventEntity>

    @Transaction
    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProject(id: String)
}
