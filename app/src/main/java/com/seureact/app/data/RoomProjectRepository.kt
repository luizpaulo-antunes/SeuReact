package com.seureact.app.data

import com.seureact.app.data.local.ReactionDao
import com.seureact.app.domain.model.ProjectStatus
import com.seureact.app.domain.model.ReactionProject
import com.seureact.app.domain.model.TimelineEvent
import com.seureact.app.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomProjectRepository(private val dao: ReactionDao) : ProjectRepository {
    override fun observeProjects(): Flow<List<ReactionProject>> = dao.observeProjects().map { list -> list.map { it.toDomain() } }
    override fun observeProject(id: String): Flow<ReactionProject?> = dao.observeProject(id).map { it?.toDomain() }
    override suspend fun getProject(id: String): ReactionProject? = dao.getProject(id)?.toDomain()
    override suspend fun createProject(project: ReactionProject) = dao.insertProject(project.toEntity())
    override suspend fun updateProject(project: ReactionProject) = dao.updateProject(project.toEntity())
    override suspend fun updateStatus(id: String, status: ProjectStatus, error: String?) =
        dao.updateStatus(id, status.name, error, System.currentTimeMillis())
    override suspend fun appendEvent(projectId: String, event: TimelineEvent) = dao.insertEvent(event.toEntity(projectId))
    override suspend fun getEvents(projectId: String): List<TimelineEvent> = dao.getEvents(projectId).map { it.toDomain() }
    override suspend fun markRecordingProjectsInterrupted() = dao.markRecordingProjectsInterrupted(System.currentTimeMillis())
    override suspend fun deleteProject(id: String) = dao.deleteProject(id)
}
