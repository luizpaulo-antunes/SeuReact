package com.seureact.app.media.export

import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.ExportException
import org.junit.Assert.assertTrue
import org.junit.Test

@UnstableApi
class ExportFailureFormatterTest {
    @Test
    fun `frame processing code receives actionable message`() {
        val message = ExportFailureFormatter.messageForCode(ExportException.ERROR_CODE_VIDEO_FRAME_PROCESSING_FAILED)

        assertTrue(message.contains("frames e efeitos"))
    }

    @Test
    fun `decoder encoder audio and muxer use distinct messages`() {
        val decoder = ExportFailureFormatter.messageForCode(ExportException.ERROR_CODE_DECODING_FAILED)
        val encoder = ExportFailureFormatter.messageForCode(ExportException.ERROR_CODE_ENCODING_FAILED)
        val audio = ExportFailureFormatter.messageForCode(ExportException.ERROR_CODE_AUDIO_PROCESSING_FAILED)
        val muxer = ExportFailureFormatter.messageForCode(ExportException.ERROR_CODE_MUXING_FAILED)

        assertTrue(setOf(decoder, encoder, audio, muxer).size == 4)
    }

    @Test
    fun `deepest cause is reduced to a short single line detail`() {
        val error = IllegalStateException("wrapper", IllegalArgumentException("GPU\nrejeitou o frame"))

        val detail = ExportFailureFormatter.deepestTechnicalDetail(error)

        assertTrue(detail == "IllegalArgumentException: GPU rejeitou o frame")
    }
}
