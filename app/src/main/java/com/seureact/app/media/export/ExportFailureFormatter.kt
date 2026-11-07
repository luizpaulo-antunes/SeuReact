package com.seureact.app.media.export

import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.ExportException

@UnstableApi
object ExportFailureFormatter {
    fun format(error: Throwable): String {
        val exportError = generateSequence(error as Throwable?) { it.cause }
            .filterIsInstance<ExportException>()
            .firstOrNull()
        if (exportError == null) {
            return error.message?.takeIf { it.isNotBlank() } ?: "Falha inesperada na exportação"
        }
        val technicalCode = exportError.getErrorCodeName()
        val detail = deepestTechnicalDetail(error)
        return buildString {
            append(messageForCode(exportError.errorCode))
            append(" ($technicalCode)")
            if (detail != null) append(". Detalhe: $detail")
            append(". Os arquivos brutos foram preservados.")
        }
    }

    internal fun deepestTechnicalDetail(error: Throwable): String? =
        generateSequence(error as Throwable?) { it.cause }
            .toList()
            .asReversed()
            .firstOrNull { it !is ExportException }
            ?.let { cause ->
                val message = cause.message
                    ?.replace(Regex("\\s+"), " ")
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
                listOfNotNull(cause::class.java.simpleName.takeIf(String::isNotEmpty), message)
                    .joinToString(": ")
                    .take(180)
            }

    internal fun messageForCode(errorCode: Int): String = when (errorCode) {
        ExportException.ERROR_CODE_VIDEO_FRAME_PROCESSING_FAILED ->
            "Falha ao processar os frames e efeitos do vídeo"
        ExportException.ERROR_CODE_DECODER_INIT_FAILED,
        ExportException.ERROR_CODE_DECODING_FAILED,
        ExportException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED ->
            "O aparelho não conseguiu decodificar uma das trilhas de vídeo"
        ExportException.ERROR_CODE_ENCODER_INIT_FAILED,
        ExportException.ERROR_CODE_ENCODING_FAILED,
        ExportException.ERROR_CODE_ENCODING_FORMAT_UNSUPPORTED ->
            "O aparelho não conseguiu codificar o vídeo final"
        ExportException.ERROR_CODE_AUDIO_PROCESSING_FAILED ->
            "Falha ao processar ou misturar o áudio"
        ExportException.ERROR_CODE_MUXING_FAILED,
        ExportException.ERROR_CODE_MUXING_TIMEOUT,
        ExportException.ERROR_CODE_MUXING_APPEND ->
            "Falha ao montar o arquivo MP4 final"
        ExportException.ERROR_CODE_IO_FILE_NOT_FOUND,
        ExportException.ERROR_CODE_IO_NO_PERMISSION,
        ExportException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE,
        ExportException.ERROR_CODE_IO_UNSPECIFIED ->
            "Não foi possível ler ou gravar um dos arquivos da reação"
        else -> "Falha inesperada no motor de exportação"
    }
}
