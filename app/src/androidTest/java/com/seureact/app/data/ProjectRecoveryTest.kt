package com.seureact.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.seureact.app.data.local.ReactionDatabase
import com.seureact.app.domain.model.ProjectStatus
import com.seureact.app.domain.model.ReactionProject
import com.seureact.app.domain.model.SourceMetadata
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProjectRecoveryTest {
    private lateinit var database: ReactionDatabase
    private lateinit var repository: RoomProjectRepository

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            ReactionDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = RoomProjectRepository(database.reactionDao())
    }

    @After
    fun close() = database.close()

    @Test
    fun recordingProjectBecomesInterruptedAfterRestart() = runTest {
        val project = project(ProjectStatus.RECORDING)
        repository.createProject(project)
        repository.markRecordingProjectsInterrupted()
        assertEquals(ProjectStatus.INTERRUPTED, repository.getProject(project.id)?.status)
    }

    @Test
    fun rawFilePathsRemainAfterFailedExport() = runTest {
        val project = project(ProjectStatus.READY_TO_EXPORT).copy(
            cameraVideoPath = "/private/camera.mp4",
            microphoneAudioPath = "/private/camera.mp4",
        )
        repository.createProject(project)
        repository.updateStatus(project.id, ProjectStatus.FAILED, "codec")
        val recovered = repository.getProject(project.id)!!
        assertEquals("/private/camera.mp4", recovered.cameraVideoPath)
        assertEquals("/private/camera.mp4", recovered.microphoneAudioPath)
    }

    private fun project(status: ProjectStatus) = ReactionProject(
        sourceUri = "content://video/1",
        localSourcePath = "/private/source.mp4",
        metadata = SourceMetadata("video.mp4", 1_000, 1280, 720, true, "video/mp4"),
        status = status,
    )
}
