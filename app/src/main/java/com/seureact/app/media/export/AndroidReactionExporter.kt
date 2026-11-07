package com.seureact.app.media.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.media3.common.C
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.ChannelMixingAudioProcessor
import androidx.media3.common.audio.ToInt16PcmAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Size
import androidx.media3.effect.OverlayEffect
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult as Media3ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.seureact.app.domain.model.ReactionProject
import com.seureact.app.domain.timeline.RenderSegment
import com.seureact.app.domain.timeline.SourceRenderMode
import com.seureact.app.media.SourceVideoGeometry
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

@UnstableApi
class AndroidReactionExporter(private val context: Context) : ReactionExporter {
    override suspend fun export(
        project: ReactionProject,
        segments: List<RenderSegment>,
        onProgress: suspend (Int) -> Unit,
    ): ExportResult {
        require(segments.isNotEmpty()) { "A reação não possui conteúdo" }
        val cameraFile = project.cameraVideoPath?.let(::File)
            ?: error("Arquivo bruto da câmera ausente")
        require(File(project.localSourcePath).canRead()) { "Vídeo original indisponível" }
        require(cameraFile.canRead() && cameraFile.length() > 0) { "Gravação da câmera indisponível" }

        val exportDirectory = File(context.cacheDir, "exports").apply { mkdirs() }
        val temporaryOutput = File(exportDirectory, "${project.id}.mp4")
        if (temporaryOutput.exists()) temporaryOutput.delete()
        val composition = withContext(Dispatchers.IO) { buildComposition(project, segments, cameraFile) }
        awaitExport(composition, temporaryOutput, onProgress)
        val outputUri = withContext(Dispatchers.IO) { copyToGallery(project, temporaryOutput) }
        return ExportResult(outputUri, segments.last().finalEndNanos / 1_000_000L)
    }

    private fun buildComposition(
        project: ReactionProject,
        segments: List<RenderSegment>,
        cameraFile: File,
    ): Composition {
        val stillDirectory = File(context.cacheDir, "stills/${project.id}").apply { mkdirs() }
        val retriever = MediaMetadataRetriever().apply { setDataSource(project.localSourcePath) }
        val encodedWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
            ?: project.metadata.width
        val encodedHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
            ?: project.metadata.height
        val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        val orientedDimensions = SourceVideoGeometry.orientedSize(encodedWidth, encodedHeight, rotation)
        val sourceDisplaySize = Size(orientedDimensions.width, orientedDimensions.height)
        val cameraDisplaySize = videoDisplaySize(cameraFile)
        val sourceItems = try {
            segments.mapIndexed { index, segment ->
                when (segment.sourceMode) {
                    SourceRenderMode.PLAYING -> editedVideo(
                        path = project.localSourcePath,
                        startMs = segment.sourceStartMs,
                        endMs = segment.sourceEndMs,
                        volume = if (segment.sourceMuted) 0f else segment.sourceVolume,
                        videoEffects = sourceVideoEffects(segment),
                    )
                    SourceRenderMode.FROZEN -> {
                        val frameFile = File(stillDirectory, "frame_$index.jpg")
                        val bitmap = retriever.getFrameAtTime(
                            segment.sourceStartMs * 1_000L,
                            MediaMetadataRetriever.OPTION_CLOSEST,
                        ) ?: error("Não foi possível obter o frame congelado em ${segment.sourceStartMs} ms")
                        frameFile.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
                        bitmap.recycle()
                        editedImage(frameFile, segment.durationNanos / 1_000_000L, sourceVideoEffects(segment))
                    }
                }
            }
        } finally {
            retriever.release()
        }
        val cameraItems = segments.map { segment ->
            editedVideo(
                path = cameraFile.absolutePath,
                startMs = segment.cameraStartNanos / 1_000_000L,
                endMs = segment.cameraEndNanos / 1_000_000L,
                volume = segment.microphoneVolume,
                videoEffects = cameraVideoEffects(segment.cameraShape),
            )
        }
        val sourceSequence = EditedMediaItemSequence.withAudioAndVideoFrom(sourceItems)
        val cameraSequence = EditedMediaItemSequence.withAudioAndVideoFrom(cameraItems)
        val sequences = mapOf(
            ReactionVideoLayer.CAMERA to cameraSequence,
            ReactionVideoLayer.SOURCE to sourceSequence,
        )
        return Composition.Builder(ReactionVideoLayerOrder.compositionOrder.map(sequences::getValue))
            .setVideoCompositorSettings(
                ReactionVideoCompositorSettings(project, segments, sourceDisplaySize, cameraDisplaySize),
            )
            .build()
    }

    private fun videoDisplaySize(file: File): Size {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val width = checkNotNull(
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull(),
            ) { "Largura da câmera indisponível" }
            val height = checkNotNull(
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull(),
            ) { "Altura da câmera indisponível" }
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            val oriented = SourceVideoGeometry.orientedSize(width, height, rotation)
            Size(oriented.width, oriented.height)
        } finally {
            retriever.release()
        }
    }

    private fun editedVideo(
        path: String,
        startMs: Long,
        endMs: Long,
        volume: Float,
        videoEffects: List<Effect> = emptyList(),
    ): EditedMediaItem {
        val mediaItem = MediaItem.Builder()
            .setUri(Uri.fromFile(File(path)))
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startMs.coerceAtLeast(0))
                    .setEndPositionMs(endMs.coerceAtLeast(startMs + 1))
                    .build(),
            )
            .build()
        return EditedMediaItem.Builder(mediaItem)
            .setEffects(mediaEffects(volume, videoEffects))
            .build()
    }

    private fun editedImage(file: File, durationMs: Long, videoEffects: List<Effect>): EditedMediaItem {
        val mediaItem = MediaItem.Builder()
            .setUri(Uri.fromFile(file))
            .setImageDurationMs(durationMs.coerceAtLeast(1))
            .build()
        return EditedMediaItem.Builder(mediaItem)
            .setFrameRate(30)
            .setEffects(Effects(emptyList(), videoEffects))
            .build()
    }

    private fun mediaEffects(volume: Float, videoEffects: List<Effect>): Effects {
        val channelMixer = ChannelMixingAudioProcessor().apply {
            (1..8).forEach { inputChannels ->
                putChannelMixingMatrix(StereoChannelMixingMatrixFactory.create(inputChannels))
            }
        }
        val processors: List<AudioProcessor> = listOf(
            ToInt16PcmAudioProcessor(),
            channelMixer,
            PcmVolumeAudioProcessor(volume.coerceIn(0f, 2f) * MIX_HEADROOM),
        )
        return Effects(processors, videoEffects)
    }

    private fun sourceVideoEffects(segment: RenderSegment): List<Effect> = buildList {
        segment.pointerMarker?.let { marker ->
            add(OverlayEffect(listOf(PointerCanvasOverlay(context, marker))))
        }
    }

    private suspend fun awaitExport(
        composition: Composition,
        output: File,
        onProgress: suspend (Int) -> Unit,
    ) = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            lateinit var transformer: Transformer
            transformer = Transformer.Builder(context)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: Media3ExportResult) {
                        if (continuation.isActive) continuation.resume(Unit)
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: Media3ExportResult,
                        exportException: ExportException,
                    ) {
                        if (continuation.isActive) continuation.resumeWithException(exportException)
                    }
                })
                .build()
            continuation.invokeOnCancellation { transformer.cancel() }
            transformer.start(composition, output.absolutePath)
            kotlinx.coroutines.CoroutineScope(continuation.context).launch {
                val holder = ProgressHolder()
                while (continuation.isActive) {
                    if (transformer.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) {
                        onProgress(holder.progress.coerceIn(0, 99))
                    }
                    delay(500)
                }
            }
        }
    }

    private fun copyToGallery(project: ReactionProject, source: File): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "SeuReact_${project.id.take(8)}.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/SeuReact")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Não foi possível criar o vídeo na galeria")
        try {
            resolver.openOutputStream(uri)?.use { output -> source.inputStream().use { it.copyTo(output) } }
                ?: error("Não foi possível escrever o vídeo na galeria")
            resolver.update(uri, ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }, null, null)
            return uri
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    private companion object {
        const val MIX_HEADROOM = 0.7f
    }
}
