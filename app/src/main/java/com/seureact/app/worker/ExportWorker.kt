package com.seureact.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.seureact.app.R
import com.seureact.app.ReactionApplication
import com.seureact.app.domain.model.ProjectStatus
import com.seureact.app.domain.timeline.TimelineCompiler
import com.seureact.app.media.export.ExportFailureFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
class ExportWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    private val container = (context.applicationContext as ReactionApplication).container

    override suspend fun doWork(): Result {
        val projectId = inputData.getString(KEY_PROJECT_ID) ?: return Result.failure(errorData("Projeto inválido"))
        val project = container.projectRepository.getProject(projectId)
            ?: return Result.failure(errorData("Projeto não encontrado"))
        return try {
            setForeground(foregroundInfo(0))
            container.projectRepository.updateStatus(projectId, ProjectStatus.EXPORTING)
            val segments = TimelineCompiler().compile(container.projectRepository.getEvents(projectId))
            val result = container.reactionExporter.export(project, segments) { progress ->
                setProgress(Data.Builder().putInt(KEY_PROGRESS, progress).build())
                setForeground(foregroundInfo(progress))
                container.projectRepository.updateProject(
                    (container.projectRepository.getProject(projectId) ?: project).copy(exportProgress = progress),
                )
            }
            container.projectRepository.updateProject(
                project.copy(
                    status = ProjectStatus.COMPLETED,
                    outputUri = result.outputUri.toString(),
                    exportProgress = 100,
                    lastError = null,
                    updatedAtEpochMs = System.currentTimeMillis(),
                ),
            )
            Result.success(Data.Builder().putString(KEY_OUTPUT_URI, result.outputUri.toString()).build())
        } catch (error: CancellationException) {
            withContext(NonCancellable) {
                container.projectRepository.updateStatus(projectId, ProjectStatus.READY_TO_EXPORT)
            }
            throw error
        } catch (error: Throwable) {
            Log.e(TAG, "Falha ao exportar projeto $projectId", error)
            val message = ExportFailureFormatter.format(error)
            container.projectRepository.updateStatus(projectId, ProjectStatus.FAILED, message)
            Result.failure(errorData(message))
        }
    }

    private fun foregroundInfo(progress: Int): ForegroundInfo {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Exportações", NotificationManager.IMPORTANCE_LOW))
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle("Exportando reação")
            .setContentText("$progress% concluído")
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()
        return ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    private fun errorData(message: String) = Data.Builder().putString(KEY_ERROR, message).build()

    companion object {
        const val KEY_PROJECT_ID = "project_id"
        const val KEY_PROGRESS = "progress"
        const val KEY_OUTPUT_URI = "output_uri"
        const val KEY_ERROR = "error"
        private const val CHANNEL_ID = "reaction_exports"
        private const val NOTIFICATION_ID = 2001
        private const val TAG = "SeuReactExport"
    }
}
