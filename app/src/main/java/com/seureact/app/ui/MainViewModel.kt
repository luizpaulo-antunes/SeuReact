package com.seureact.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.seureact.app.AppContainer
import com.seureact.app.domain.model.CameraShape
import com.seureact.app.domain.model.CameraFacing
import com.seureact.app.domain.model.CameraOverlayGeometry
import com.seureact.app.domain.model.CameraTransform
import com.seureact.app.domain.model.OutputAspect
import com.seureact.app.domain.model.OutputResolution
import com.seureact.app.domain.model.PointerMarker
import com.seureact.app.domain.model.PointerStyle
import com.seureact.app.domain.model.ProjectStatus
import com.seureact.app.domain.model.ReactionProject
import com.seureact.app.domain.model.TimelineEvent
import com.seureact.app.domain.model.SourceTransform
import com.seureact.app.domain.session.RecordingFinalizationPolicy
import com.seureact.app.domain.session.RecordingCompletionIntent
import com.seureact.app.media.capture.CameraCaptureController
import com.seureact.app.media.capture.SessionClock
import com.seureact.app.worker.ExportWorker
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppScreen { HOME, STUDIO, EXPORT }

data class MainUiState(
    val screen: AppScreen = AppScreen.HOME,
    val project: ReactionProject? = null,
    val importing: Boolean = false,
    val isRecording: Boolean = false,
    val isFinalizing: Boolean = false,
    val isDiscarding: Boolean = false,
    val isPlaying: Boolean = false,
    val isMuted: Boolean = false,
    val elapsedNanos: Long = 0,
    val sourcePositionMs: Long = 0,
    val message: String? = null,
    val exportProgress: Int = 0,
    val exportState: WorkInfo.State? = null,
    val sourceTransform: SourceTransform = SourceTransform(),
    val pointerStyle: PointerStyle = PointerStyle.FINGER,
    val activePointer: PointerMarker? = null,
)

class MainViewModel(application: Application, private val container: AppContainer) : AndroidViewModel(application) {
    private val repository = container.projectRepository
    private val mutableState = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = mutableState.asStateFlow()
    val projects = repository.observeProjects().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val player = ExoPlayer.Builder(application).build()
    private var clock: SessionClock? = null
    private var recordingController: CameraCaptureController? = null
    private var completionIntent = RecordingCompletionIntent.INTERRUPT
    private var activeRecordingProjectId: String? = null
    private var interruptionReason: String? = null
    private var finalizationTimeout: Job? = null
    private var pointerClearJob: Job? = null

    fun importVideo(uri: Uri) {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(importing = true, message = null)
            runCatching { container.sourceImporter.import(uri) }
                .onSuccess { project ->
                    repository.createProject(project)
                    loadInPlayer(project)
                    mutableState.value = mutableState.value.copy(
                        screen = AppScreen.STUDIO, project = project, importing = false,
                    )
                }
                .onFailure { error ->
                    mutableState.value = mutableState.value.copy(importing = false, message = error.message)
                }
        }
    }

    fun openProject(project: ReactionProject) {
        val destination = when (project.status) {
            ProjectStatus.PREPARING -> AppScreen.STUDIO
            ProjectStatus.RECORDING -> AppScreen.STUDIO
            else -> AppScreen.EXPORT
        }
        mutableState.value = mutableState.value.copy(
            screen = destination,
            project = project,
            exportProgress = project.exportProgress,
            message = project.lastError,
        )
        loadInPlayer(project)
        if (project.status == ProjectStatus.EXPORTING) observeExport(project.id)
    }

    fun backHome() {
        if (!mutableState.value.isRecording && !mutableState.value.isFinalizing) {
            player.pause()
            mutableState.value = MainUiState()
        }
    }

    fun updateSetup(
        shape: CameraShape? = null,
        aspect: OutputAspect? = null,
        resolution: OutputResolution? = null,
        sourceVolume: Float? = null,
        microphoneVolume: Float? = null,
    ) {
        val current = mutableState.value.project ?: return
        val configured = current.copy(
            cameraShape = shape ?: current.cameraShape,
            outputAspect = aspect ?: current.outputAspect,
            outputResolution = resolution ?: current.outputResolution,
            sourceVolume = sourceVolume ?: current.sourceVolume,
            microphoneVolume = microphoneVolume ?: current.microphoneVolume,
            updatedAtEpochMs = System.currentTimeMillis(),
        )
        val updated = configured.copy(cameraTransform = CameraOverlayGeometry.constrain(
            configured.cameraTransform,
            configured.cameraShape,
            CameraOverlayGeometry.outputAspectRatio(configured),
        ))
        mutableState.value = mutableState.value.copy(project = updated)
        player.volume = if (mutableState.value.isMuted) 0f else updated.sourceVolume
        if (mutableState.value.isRecording && sourceVolume != null) {
            persist(TimelineEvent.SourceVolumeChanged(clock!!.elapsedNanos(), player.currentPosition, sourceVolume))
        }
        if (mutableState.value.isRecording && microphoneVolume != null) {
            persist(TimelineEvent.MicrophoneVolumeChanged(clock!!.elapsedNanos(), player.currentPosition, microphoneVolume))
        }
        viewModelScope.launch { repository.updateProject(updated) }
    }

    fun setCameraFacing(facing: CameraFacing) {
        val project = mutableState.value.project ?: return
        val updated = project.copy(
            cameraFacing = facing,
            cameraTransform = project.cameraTransform.copy(mirrored = facing == CameraFacing.FRONT),
            updatedAtEpochMs = System.currentTimeMillis(),
        )
        mutableState.value = mutableState.value.copy(project = updated)
        viewModelScope.launch { repository.updateProject(updated) }
    }

    fun startRecording(controller: CameraCaptureController) {
        val project = mutableState.value.project ?: return
        if (mutableState.value.isRecording || mutableState.value.isFinalizing || activeRecordingProjectId != null) return
        val rawFile = File(getApplication<Application>().filesDir, "projects/${project.id}/camera.mp4")
        recordingController = controller
        clock = null
        completionIntent = RecordingCompletionIntent.INTERRUPT
        activeRecordingProjectId = project.id
        interruptionReason = null
        runCatching { controller.start(
            outputFile = rawFile,
            onError = { message -> completeRecording(project.id, message) },
            onStarted = {
                viewModelScope.launch {
                    val sessionClock = SessionClock().also { it.start() }
                    clock = sessionClock
                    val updated = project.copy(
                        status = ProjectStatus.RECORDING,
                        cameraVideoPath = rawFile.absolutePath,
                        microphoneAudioPath = rawFile.absolutePath,
                        updatedAtEpochMs = System.currentTimeMillis(),
                    )
                    repository.updateProject(updated)
                    repository.appendEvent(project.id, TimelineEvent.SessionStarted(
                        sourcePositionMs = player.currentPosition,
                        sourcePlaying = true,
                        sourceVolume = updated.sourceVolume,
                        microphoneVolume = updated.microphoneVolume,
                        cameraTransform = updated.cameraTransform,
                        cameraShape = updated.cameraShape,
                        cameraFacing = updated.cameraFacing,
                        sourceTransform = mutableState.value.sourceTransform,
                    ))
                    mutableState.value = mutableState.value.copy(
                        project = updated, isRecording = true, isPlaying = true, activePointer = null, message = null,
                    )
                    player.play()
                }
            },
            onFinalized = { completeRecording(project.id, null) },
        ) }.onFailure { error ->
            completeRecording(project.id, error.message ?: "Não foi possível iniciar a câmera.")
        }
    }

    fun togglePlayback() {
        if (!mutableState.value.isRecording) return
        val elapsed = clock?.elapsedNanos() ?: return
        val position = player.currentPosition
        val event = if (player.isPlaying) {
            player.pause()
            TimelineEvent.SourcePause(elapsed, position)
        } else {
            player.play()
            TimelineEvent.SourcePlay(elapsed, position)
        }
        mutableState.value = mutableState.value.copy(isPlaying = player.isPlaying, sourcePositionMs = position)
        persist(event)
    }

    fun seekTo(positionMs: Long) {
        val project = mutableState.value.project ?: return
        val target = positionMs.coerceIn(0, project.metadata.durationMs)
        player.seekTo(target)
        mutableState.value = mutableState.value.copy(sourcePositionMs = target)
        if (mutableState.value.isRecording) persist(TimelineEvent.SourceSeek(clock!!.elapsedNanos(), target))
    }

    fun toggleMute() {
        val muted = !mutableState.value.isMuted
        player.volume = if (muted) 0f else (mutableState.value.project?.sourceVolume ?: 1f)
        mutableState.value = mutableState.value.copy(isMuted = muted)
        if (mutableState.value.isRecording) {
            persist(TimelineEvent.SourceMuted(clock!!.elapsedNanos(), player.currentPosition, muted))
        }
    }

    fun changeTransform(transform: CameraTransform) {
        val project = mutableState.value.project ?: return
        val constrained = CameraOverlayGeometry.constrain(
            transform,
            project.cameraShape,
            CameraOverlayGeometry.outputAspectRatio(project),
        )
        val updated = project.copy(cameraTransform = constrained)
        mutableState.value = mutableState.value.copy(project = updated)
        if (mutableState.value.isRecording) {
            persist(TimelineEvent.CameraTransformChanged(clock!!.elapsedNanos(), player.currentPosition, constrained))
        } else {
            viewModelScope.launch { repository.updateProject(updated) }
        }
    }

    fun changeSourceTransform(transform: SourceTransform) {
        if (!mutableState.value.isRecording) return
        val constrained = transform.copy(
            scale = transform.scale.coerceIn(1f, 4f),
            focusX = transform.focusX.coerceIn(0f, 1f),
            focusY = transform.focusY.coerceIn(0f, 1f),
        )
        mutableState.value = mutableState.value.copy(sourceTransform = constrained)
        persist(TimelineEvent.SourceTransformChanged(clock!!.elapsedNanos(), player.currentPosition, constrained))
    }

    fun resetSourceZoom() = changeSourceTransform(SourceTransform())

    fun setPointerStyle(style: PointerStyle) {
        mutableState.value = mutableState.value.copy(pointerStyle = style)
    }

    fun placePointer(x: Float, y: Float) {
        if (!mutableState.value.isRecording) return
        val marker = PointerMarker(x.coerceIn(0.1f, 0.9f), y.coerceIn(0.08f, 0.82f), mutableState.value.pointerStyle)
        mutableState.value = mutableState.value.copy(activePointer = marker)
        persist(TimelineEvent.PointerPlaced(clock!!.elapsedNanos(), player.currentPosition, marker))
        pointerClearJob?.cancel()
        pointerClearJob = viewModelScope.launch {
            delay(POINTER_DURATION_MS)
            if (mutableState.value.isRecording) {
                persist(TimelineEvent.PointerCleared(clock!!.elapsedNanos(), player.currentPosition))
                mutableState.value = mutableState.value.copy(activePointer = null)
            }
        }
    }

    fun tick() {
        val currentClock = clock ?: return
        val previousWasPlaying = mutableState.value.isPlaying
        if (previousWasPlaying && player.playbackState == Player.STATE_ENDED) {
            persist(TimelineEvent.SourcePause(currentClock.elapsedNanos(), player.currentPosition))
        }
        mutableState.value = mutableState.value.copy(
            elapsedNanos = currentClock.elapsedNanos(),
            sourcePositionMs = player.currentPosition,
            isPlaying = player.isPlaying,
        )
    }

    fun finishRecording() {
        if (!mutableState.value.isRecording) return
        player.pause()
        pointerClearJob?.cancel()
        completionIntent = RecordingCompletionIntent.EXPORT
        interruptionReason = null
        mutableState.value = mutableState.value.copy(
            isRecording = false,
            isFinalizing = true,
            isDiscarding = false,
            isPlaying = false,
            message = null,
        )
        recordingController?.stop()
        scheduleFinalizationTimeout(mutableState.value.project?.id ?: return)
    }

    fun interruptRecording(reason: String = "Sessão interrompida ao sair do aplicativo") {
        val project = mutableState.value.project ?: return
        if (!mutableState.value.isRecording) return
        completionIntent = RecordingCompletionIntent.INTERRUPT
        interruptionReason = reason
        recordingController?.stop()
        player.pause()
        pointerClearJob?.cancel()
        mutableState.value = mutableState.value.copy(
            isRecording = false,
            isFinalizing = true,
            isDiscarding = false,
            isPlaying = false,
            message = null,
        )
        scheduleFinalizationTimeout(project.id)
    }

    fun discardRecording() {
        val project = mutableState.value.project ?: return
        if (!mutableState.value.isRecording) return
        completionIntent = RecordingCompletionIntent.DISCARD
        player.pause()
        pointerClearJob?.cancel()
        mutableState.value = mutableState.value.copy(
            isRecording = false,
            isFinalizing = true,
            isDiscarding = true,
            isPlaying = false,
            activePointer = null,
            message = null,
        )
        recordingController?.stop()
        scheduleFinalizationTimeout(project.id)
    }

    fun retryExport() {
        val project = mutableState.value.project ?: return
        enqueueExport(project.id)
    }

    fun cancelExport() {
        val project = mutableState.value.project ?: return
        WorkManager.getInstance(getApplication()).cancelUniqueWork(workName(project.id))
    }

    fun clearMessage() {
        mutableState.value = mutableState.value.copy(message = null)
    }

    private fun completeRecording(projectId: String, captureError: String?) {
        if (activeRecordingProjectId != projectId) return
        activeRecordingProjectId = null
        player.pause()
        finalizationTimeout?.cancel()
        finalizationTimeout = null
        if (clock == null) {
            completionIntent = RecordingCompletionIntent.INTERRUPT
            interruptionReason = null
            mutableState.value = mutableState.value.copy(
                isRecording = false,
                isFinalizing = false,
                isDiscarding = false,
                isPlaying = false,
                message = captureError ?: "Não foi possível iniciar a gravação.",
            )
            return
        }
        val decision = RecordingFinalizationPolicy.decide(completionIntent, captureError, interruptionReason)
        completionIntent = RecordingCompletionIntent.INTERRUPT
        interruptionReason = null
        viewModelScope.launch {
            if (decision.shouldDiscard) {
                repository.deleteProject(projectId)
                withContext(Dispatchers.IO) {
                    File(getApplication<Application>().filesDir, "projects/$projectId").deleteRecursively()
                }
                clock = null
                mutableState.value = MainUiState(message = "Gravação descartada.")
                return@launch
            }
            val elapsed = clock?.elapsedNanos() ?: 0L
            repository.appendEvent(projectId, TimelineEvent.SessionFinished(elapsed, player.currentPosition))
            repository.updateStatus(projectId, requireNotNull(decision.status), decision.error)
            val updated = repository.getProject(projectId)
            mutableState.value = mutableState.value.copy(
                project = updated,
                isRecording = false,
                isFinalizing = false,
                isDiscarding = false,
                isPlaying = false,
                activePointer = null,
                screen = if (decision.shouldExport) AppScreen.EXPORT else AppScreen.STUDIO,
                message = decision.error,
            )
            if (decision.shouldExport) enqueueExport(projectId)
        }
    }

    private fun scheduleFinalizationTimeout(projectId: String) {
        finalizationTimeout?.cancel()
        finalizationTimeout = viewModelScope.launch {
            delay(15_000)
            completeRecording(projectId, "A câmera não conseguiu finalizar o arquivo. O material bruto foi preservado.")
        }
    }

    private fun enqueueExport(projectId: String) {
        val request = OneTimeWorkRequestBuilder<ExportWorker>()
            .setInputData(workDataOf(ExportWorker.KEY_PROJECT_ID to projectId))
            .addTag(workName(projectId))
            .build()
        WorkManager.getInstance(getApplication()).enqueueUniqueWork(workName(projectId), ExistingWorkPolicy.REPLACE, request)
        mutableState.value = mutableState.value.copy(screen = AppScreen.EXPORT, exportProgress = 0, exportState = WorkInfo.State.ENQUEUED)
        observeExport(projectId)
    }

    private fun observeExport(projectId: String) {
        viewModelScope.launch {
            WorkManager.getInstance(getApplication()).getWorkInfosForUniqueWorkFlow(workName(projectId)).collect { works ->
                val work = works.firstOrNull() ?: return@collect
                mutableState.value = mutableState.value.copy(
                    exportProgress = work.progress.getInt(ExportWorker.KEY_PROGRESS, mutableState.value.exportProgress),
                    exportState = work.state,
                    message = work.outputData.getString(ExportWorker.KEY_ERROR) ?: mutableState.value.message,
                )
                if (work.state.isFinished) {
                    repository.getProject(projectId)?.let { completed ->
                        mutableState.value = mutableState.value.copy(project = completed)
                    }
                }
            }
        }
    }

    private fun persist(event: TimelineEvent) {
        val projectId = mutableState.value.project?.id ?: return
        viewModelScope.launch { repository.appendEvent(projectId, event) }
    }

    private fun loadInPlayer(project: ReactionProject) {
        player.setMediaItem(MediaItem.fromUri(Uri.fromFile(File(project.localSourcePath))))
        player.prepare()
        player.volume = project.sourceVolume
    }

    override fun onCleared() {
        player.release()
        recordingController?.release()
    }

    private fun workName(projectId: String) = "export_$projectId"

    private companion object {
        const val POINTER_DURATION_MS = 1_200L
    }
}
