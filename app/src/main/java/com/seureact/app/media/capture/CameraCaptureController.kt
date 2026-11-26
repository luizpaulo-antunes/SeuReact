package com.seureact.app.media.capture

import android.annotation.SuppressLint
import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.ExperimentalPersistentRecording
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine

class CameraCaptureController(private val context: Context) {
    private var provider: ProcessCameraProvider? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var lifecycleOwner: LifecycleOwner? = null
    private var previewView: PreviewView? = null
    private var facing: CameraFacing = CameraFacing.FRONT
    private var isBound = false
    private val bindingMutex = Mutex()

    val activeFacing: CameraFacing get() = facing

    suspend fun bind(owner: LifecycleOwner, view: PreviewView, cameraFacing: CameraFacing = facing): CameraFacing =
        bindingMutex.withLock {
            val bindingMatches = isBound && lifecycleOwner === owner && previewView === view && facing == cameraFacing
            lifecycleOwner = owner
            previewView = view
            val cameraProvider = awaitProvider()
            provider = cameraProvider
            val capture = videoCapture ?: createVideoCapture().also { videoCapture = it }
            val selectedFacing = resolveFacing(cameraProvider, cameraFacing)
            if (bindingMatches && facing == selectedFacing) {
                updatePreviewMirror(view, selectedFacing)
                return@withLock selectedFacing
            }
            bindUseCases(cameraProvider, owner, view, capture, selectedFacing)
            selectedFacing
        }

    suspend fun switchCamera(): CameraFacing = bindingMutex.withLock {
        val owner = checkNotNull(lifecycleOwner) { "Câmera ainda não preparada" }
        val view = checkNotNull(previewView) { "Preview da câmera indisponível" }
        val cameraProvider = provider ?: awaitProvider().also { provider = it }
        val capture = checkNotNull(videoCapture) { "Câmera ainda não preparada" }
        val previousFacing = facing
        val requestedFacing = if (previousFacing == CameraFacing.FRONT) CameraFacing.BACK else CameraFacing.FRONT
        check(cameraProvider.hasCamera(selector(requestedFacing))) {
            if (requestedFacing == CameraFacing.FRONT) {
                "Este aparelho não possui câmera frontal disponível."
            } else {
                "Este aparelho não possui câmera traseira disponível."
            }
        }
        try {
            bindUseCases(cameraProvider, owner, view, capture, requestedFacing)
            requestedFacing
        } catch (error: Exception) {
            val rollback = runCatching { bindUseCases(cameraProvider, owner, view, capture, previousFacing) }
            if (rollback.isFailure) {
                recording?.stop()
                throw IllegalStateException(
                    "A troca de câmera falhou e a gravação foi interrompida com segurança.",
                    error,
                )
            }
            throw IllegalStateException("Não foi possível trocar de câmera neste aparelho.", error)
        }
    }

    @androidx.annotation.OptIn(markerClass = [ExperimentalPersistentRecording::class])
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
            .asPersistentRecording()
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
    }

    fun release() {
        stop()
        provider?.unbindAll()
        isBound = false
    }

    private fun createVideoCapture(): VideoCapture<Recorder> {
        val recorder = Recorder.Builder().setQualitySelector(
            QualitySelector.fromOrderedList(listOf(Quality.FHD, Quality.HD, Quality.SD)),
        ).build()
        return VideoCapture.withOutput(recorder)
    }

    private fun bindUseCases(
        cameraProvider: ProcessCameraProvider,
        owner: LifecycleOwner,
        view: PreviewView,
        capture: VideoCapture<Recorder>,
        cameraFacing: CameraFacing,
    ) {
        cameraProvider.unbindAll()
        isBound = false
        val preview = Preview.Builder().build().also { it.surfaceProvider = view.surfaceProvider }
        cameraProvider.bindToLifecycle(owner, selector(cameraFacing), preview, capture)
        facing = cameraFacing
        isBound = true
        updatePreviewMirror(view, cameraFacing)
    }

    private fun resolveFacing(cameraProvider: ProcessCameraProvider, requested: CameraFacing): CameraFacing = when {
        cameraProvider.hasCamera(selector(requested)) -> requested
        requested == CameraFacing.FRONT && cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraFacing.BACK
        requested == CameraFacing.BACK && cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraFacing.FRONT
        else -> error("Nenhuma câmera compatível foi encontrada neste aparelho.")
    }

    private fun updatePreviewMirror(view: PreviewView, cameraFacing: CameraFacing) {
        view.scaleX = if (cameraFacing == CameraFacing.FRONT) -1f else 1f
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
