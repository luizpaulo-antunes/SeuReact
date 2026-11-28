package com.seureact.app.media.capture

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Rational
import android.view.Surface
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CompositionSettings
import androidx.camera.core.ConcurrentCamera
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.annotation.MainThread
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.seureact.app.domain.model.DualCameraGeometry
import com.seureact.app.domain.model.DualCameraPipTransform
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class DualCameraUnavailableException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)

class DualCameraCaptureController(private val context: Context) {
    private var provider: ProcessCameraProvider? = null
    private var concurrentCamera: ConcurrentCamera? = null
    private var preview: Preview? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var stopRequested = false
    private var releasePending = false
    private var frontIsPip = true
    private var pipTransform = DualCameraPipTransform()

    val isRecording: Boolean get() = recording != null

    @MainThread
    suspend fun bind(
        owner: LifecycleOwner,
        view: PreviewView,
        initialTransform: DualCameraPipTransform = pipTransform,
        initialFrontIsPip: Boolean = frontIsPip,
    ) {
        check(recording == null) { "Aguarde a gravação DualCam atual ser finalizada." }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            throw DualCameraUnavailableException("O modo DualCam requer Android 11 ou mais recente.")
        }
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_CONCURRENT)) {
            throw DualCameraUnavailableException(
                "Este aparelho não informou suporte a câmeras frontal e traseira simultâneas.",
            )
        }
        val cameraProvider = awaitProvider()
        val pair = findFrontBackPair(cameraProvider)
            ?: throw DualCameraUnavailableException(
                "O CameraX não encontrou um par frontal/traseiro que possa gravar simultaneamente.",
            )
        val targetRotation = view.display?.rotation ?: Surface.ROTATION_0
        val recorder = Recorder.Builder()
            .setQualitySelector(
                QualitySelector.from(
                    Quality.HD,
                    FallbackStrategy.lowerQualityOrHigherThan(Quality.HD),
                ),
            )
            .build()
        val capture = VideoCapture.Builder(recorder)
            .setTargetRotation(targetRotation)
            .build()
        val previewUseCase = Preview.Builder()
            .setTargetRotation(targetRotation)
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                    .build(),
            )
            .build()
            .also { it.surfaceProvider = view.surfaceProvider }
        val viewPort = ViewPort.Builder(
            Rational(
                DualCameraGeometry.PORTRAIT_OUTPUT_WIDTH,
                DualCameraGeometry.PORTRAIT_OUTPUT_HEIGHT,
            ),
            targetRotation,
        ).setScaleType(ViewPort.FILL_CENTER).build()
        val group = UseCaseGroup.Builder()
            .addUseCase(previewUseCase)
            .addUseCase(capture)
            .setViewPort(viewPort)
            .build()

        val nextPipTransform = DualCameraGeometry.constrain(initialTransform)
        val nextFrontIsPip = initialFrontIsPip
        unbindCurrent()
        val settings = compositionSettings(nextFrontIsPip, nextPipTransform)
        val boundCamera = try {
            cameraProvider.bindToLifecycle(
                listOf(
                    ConcurrentCamera.SingleCameraConfig(pair.back.cameraSelector, group, settings[BACK_INDEX], owner),
                    ConcurrentCamera.SingleCameraConfig(pair.front.cameraSelector, group, settings[FRONT_INDEX], owner),
                ),
            )
        } catch (error: IllegalArgumentException) {
            runCatching { cameraProvider.unbindAll() }
            throw DualCameraUnavailableException(
                "Este aparelho não conseguiu combinar preview e gravação nas duas câmeras.",
                error,
            )
        } catch (error: UnsupportedOperationException) {
            runCatching { cameraProvider.unbindAll() }
            throw DualCameraUnavailableException(
                "A composição DualCam não é compatível com a implementação de câmera deste aparelho.",
                error,
            )
        } catch (error: RuntimeException) {
            runCatching { cameraProvider.unbindAll() }
            throw error
        }
        provider = cameraProvider
        concurrentCamera = boundCamera
        preview = previewUseCase
        videoCapture = capture
        pipTransform = nextPipTransform
        frontIsPip = nextFrontIsPip
        releasePending = false
        view.scaleX = 1f
    }

    @MainThread
    fun updatePip(transform: DualCameraPipTransform): DualCameraPipTransform {
        val constrained = DualCameraGeometry.constrain(transform)
        applyComposition(frontIsPip, constrained)
        pipTransform = constrained
        return constrained
    }

    @MainThread
    fun swapCameras(): Boolean {
        val nextFrontIsPip = !frontIsPip
        applyComposition(nextFrontIsPip, pipTransform)
        frontIsPip = nextFrontIsPip
        return frontIsPip
    }

    @SuppressLint("MissingPermission")
    @MainThread
    fun start(
        onStarted: () -> Unit,
        onFinalized: (Result<Uri>) -> Unit,
    ) {
        check(recording == null) { "Já existe uma gravação DualCam ativa." }
        check(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            "Permita o acesso à câmera antes de gravar."
        }
        check(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            "Permita o acesso ao microfone antes de gravar."
        }
        val capture = checkNotNull(videoCapture) { "A câmera DualCam ainda não está pronta." }
        releasePending = false
        stopRequested = false
        val output = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        ).setContentValues(
            ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, outputName())
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/SeuReact")
            },
        ).build()

        recording = capture.output
            .prepareRecording(context, output)
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(context)) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> onStarted()
                    is VideoRecordEvent.Finalize -> {
                        recording = null
                        stopRequested = false
                        val outputUri = event.outputResults.outputUri
                        val sourceStoppedWithOutput =
                            event.error == VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE &&
                                outputUri != Uri.EMPTY &&
                                event.recordingStats.recordedDurationNanos > 0L &&
                                event.recordingStats.numBytesRecorded > 0L
                        if ((!event.hasError() || sourceStoppedWithOutput) && outputUri != Uri.EMPTY) {
                            onFinalized(Result.success(outputUri))
                        } else {
                            if (outputUri != Uri.EMPTY) {
                                runCatching { context.contentResolver.delete(outputUri, null, null) }
                            }
                            onFinalized(Result.failure(IllegalStateException(finalizeMessage(event))))
                        }
                        if (releasePending) unbindCurrent()
                    }
                }
            }
    }

    @MainThread
    fun stop() {
        if (!stopRequested) {
            stopRequested = true
            recording?.stop()
        }
    }

    @MainThread
    fun release() {
        if (recording != null) {
            releasePending = true
            stop()
        } else {
            unbindCurrent()
        }
    }

    @MainThread
    private fun unbindCurrent() {
        try {
            if (concurrentCamera != null) provider?.unbindAll()
        } finally {
            concurrentCamera = null
            preview = null
            videoCapture = null
            provider = null
        }
    }

    @MainThread
    private fun applyComposition(frontPip: Boolean, transform: DualCameraPipTransform) {
        val camera = checkNotNull(concurrentCamera) { "As câmeras DualCam não estão ativas." }
        camera.setCompositionSettings(compositionSettings(frontPip, transform))
    }

    private fun compositionSettings(
        frontPip: Boolean,
        transform: DualCameraPipTransform,
    ): List<CompositionSettings> {
        val back = if (frontPip) pipOrFullSettings(isPip = false, transform = transform) else {
            pipOrFullSettings(isPip = true, transform = transform)
        }
        val front = if (frontPip) pipOrFullSettings(isPip = true, transform = transform) else {
            pipOrFullSettings(isPip = false, transform = transform)
        }
        return listOf(back, front)
    }

    private fun pipOrFullSettings(
        isPip: Boolean,
        transform: DualCameraPipTransform,
    ): CompositionSettings {
        if (!isPip) {
            // CameraX aplica a política de espelhamento de cada lente depois da composição.
            return CompositionSettings.Builder()
                .setScale(1f, 1f)
                .setZOrder(BACKGROUND_Z_ORDER)
                .build()
        }
        val composition = DualCameraGeometry.compositionTransform(transform)
        return CompositionSettings.Builder()
            .setScale(composition.scaleX, composition.scaleY)
            .setOffset(composition.offsetX, composition.offsetY)
            .setZOrder(PIP_Z_ORDER)
            .setRoundedCornerRatio(1f)
            .setBorderWidthRatio(PIP_BORDER_RATIO)
            .setBorderColor(android.graphics.Color.WHITE)
            .build()
    }

    private fun findFrontBackPair(cameraProvider: ProcessCameraProvider): CameraPair? =
        cameraProvider.availableConcurrentCameraInfos.firstNotNullOfOrNull { combination ->
            val front = combination.firstOrNull { it.lensFacing == CameraSelector.LENS_FACING_FRONT }
            val back = combination.firstOrNull { it.lensFacing == CameraSelector.LENS_FACING_BACK }
            if (front != null && back != null) CameraPair(front, back) else null
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

    private fun finalizeMessage(event: VideoRecordEvent.Finalize): String = when (event.error) {
        VideoRecordEvent.Finalize.ERROR_INSUFFICIENT_STORAGE -> "Não há espaço suficiente para salvar o vídeo DualCam."
        VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE -> "As câmeras foram interrompidas antes da finalização."
        VideoRecordEvent.Finalize.ERROR_ENCODING_FAILED -> "O aparelho não conseguiu codificar a gravação DualCam."
        else -> event.cause?.message ?: "A gravação DualCam não pôde ser finalizada."
    }

    private fun outputName(): String = "SeuReact_DualCam_${LocalDateTime.now().format(FILE_STAMP)}.mp4"

    private data class CameraPair(val front: CameraInfo, val back: CameraInfo)

    private companion object {
        const val BACK_INDEX = 0
        const val FRONT_INDEX = 1
        const val BACKGROUND_Z_ORDER = 0
        const val PIP_Z_ORDER = 1
        const val PIP_BORDER_RATIO = 0.035f
        val FILE_STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
    }
}
