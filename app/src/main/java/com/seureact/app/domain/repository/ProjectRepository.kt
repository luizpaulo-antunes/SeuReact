package com.seureact.app.domain.repository

import com.seureact.app.domain.model.ProjectStatus
import com.seureact.app.domain.model.ReactionProject
import com.seureact.app.domain.model.TimelineEvent
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun observeProjects(): Flow<List<ReactionProject>>
    fun observeProject(id: String): Flow<ReactionProject?>
    suspend fun getProject(id: String): ReactionProject?
    suspend fun createProject(project: ReactionProject)
    suspend fun updateProject(project: ReactionProject)
    suspend fun updateStatus(id: String, status: ProjectStatus, error: String? = null)
    suspend fun appendEvent(projectId: String, event: TimelineEvent)
    suspend fun getEvents(projectId: String): List<TimelineEvent>
    suspend fun markRecordingProjectsInterrupted()
    suspend fun deleteProject(id: String)
}
