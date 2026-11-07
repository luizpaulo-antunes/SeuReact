package com.seureact.app.media.capture

import android.annotation.SuppressLint
import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.seureact.app.domain.model.CameraFacing
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class CameraCaptureController(private val context: Context) {
    private var provider: ProcessCameraProvider? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var lifecycleOwner: LifecycleOwner? = null
    private var previewView: PreviewView? = null
    private var facing: CameraFacing = CameraFacing.FRONT

    suspend fun bind(owner: LifecycleOwner, view: PreviewView, cameraFacing: CameraFacing = facing) {
        lifecycleOwner = owner
        previewView = view
        facing = cameraFacing
        val cameraProvider = awaitProvider()
        provider = cameraProvider
        cameraProvider.unbindAll()
        val preview = Preview.Builder().build().also { it.surfaceProvider = view.surfaceProvider }
        val recorder = Recorder.Builder().setQualitySelector(
            QualitySelector.fromOrderedList(listOf(Quality.FHD, Quality.HD, Quality.SD)),
        ).build()
        videoCapture = VideoCapture.withOutput(recorder)
        cameraProvider.bindToLifecycle(owner, selector(cameraFacing), preview, videoCapture)
        view.scaleX = if (cameraFacing == CameraFacing.FRONT) -1f else 1f
    }

    suspend fun switchCamera() {
        val owner = lifecycleOwner ?: return
        val view = previewView ?: return
        bind(owner, view, if (facing == CameraFacing.FRONT) CameraFacing.BACK else CameraFacing.FRONT)
    }

    @SuppressLint("MissingPermission")
    fun start(
        outputFile: File,
        onError: (String) -> Unit,
        onStarted: () -> Unit = {},
        onFinalized: () -> Unit = {},
    ) {
        check(recording == null) { "Já existe uma gravação ativa" }
        val capture = checkNotNull(videoCapture) { "Câmera ainda não preparada" }
        outputFile.parentFile?.mkdirs()
        recording = capture.output
            .prepareRecording(context, FileOutputOptions.Builder(outputFile).build())
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(context)) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> onStarted()
                    is VideoRecordEvent.Finalize -> {
                        recording = null
                        if (event.hasError()) onError(event.cause?.message ?: "Falha ao gravar câmera e microfone")
                        else onFinalized()
                    }
                }
            }
    }

    fun stop() {
        recording?.stop()
        recording = null
    }

    fun release() {
        stop()
        provider?.unbindAll()
    }

    private suspend fun awaitProvider(): ProcessCameraProvider = suspendCancellableCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                continuation.resume(future.get())
            } catch (error: Exception) {
                continuation.resumeWithException(error)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun selector(cameraFacing: CameraFacing) = if (cameraFacing == CameraFacing.FRONT) {
        CameraSelector.DEFAULT_FRONT_CAMERA
    } else {
        CameraSelector.DEFAULT_BACK_CAMERA
    }
}
