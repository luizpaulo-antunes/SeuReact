package com.seureact.app.media.importer

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.seureact.app.domain.model.ReactionProject
import com.seureact.app.domain.model.SourceMetadata
import com.seureact.app.media.SourceVideoGeometry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class SourceVideoImporter(private val context: Context) {
    suspend fun import(uri: Uri): ReactionProject = withContext(Dispatchers.IO) {
        val mimeType = context.contentResolver.getType(uri).orEmpty()
        require(mimeType.startsWith("video/") || mimeType.isBlank()) { "Selecione um arquivo de vídeo" }
        val displayName = queryName(uri) ?: "video.mp4"
        val projectId = java.util.UUID.randomUUID().toString()
        val projectDirectory = File(context.filesDir, "projects/$projectId").apply { mkdirs() }
        val localSource = File(projectDirectory, "source.mp4")
        context.contentResolver.openInputStream(uri)?.use { input ->
            localSource.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Não foi possível abrir o vídeo")
        require(localSource.length() > 0) { "O vídeo selecionado está vazio" }

        val retriever = MediaMetadataRetriever()
        val extractor = MediaExtractor()
        try {
            retriever.setDataSource(localSource.absolutePath)
            extractor.setDataSource(localSource.absolutePath)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            require(durationMs > 0 && width > 0 && height > 0) { "Vídeo inválido ou não decodificável" }
            val orientedSize = SourceVideoGeometry.orientedSize(width, height, rotation)
            var hasVideo = false
            var hasAudio = false
            repeat(extractor.trackCount) { index ->
                val trackMime = extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME).orEmpty()
                hasVideo = hasVideo || trackMime.startsWith("video/")
                hasAudio = hasAudio || trackMime.startsWith("audio/")
            }
            require(hasVideo) { "O arquivo não possui uma trilha de vídeo" }
            ReactionProject(
                id = projectId,
                sourceUri = uri.toString(),
                localSourcePath = localSource.absolutePath,
                metadata = SourceMetadata(
                    displayName, durationMs, orientedSize.width, orientedSize.height,
                    hasAudio, mimeType.ifBlank { "video/mp4" },
                ),
            )
        } finally {
            retriever.release()
            extractor.release()
        }
    }

    private fun queryName(uri: Uri): String? = context.contentResolver.query(
        uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null,
    )?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }
}
