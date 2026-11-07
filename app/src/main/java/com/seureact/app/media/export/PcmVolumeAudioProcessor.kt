package com.seureact.app.media.export

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

@UnstableApi
class PcmVolumeAudioProcessor(private val gain: Float) : BaseAudioProcessor() {
    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val input = inputBuffer.order(ByteOrder.nativeOrder())
        val output = replaceOutputBuffer(input.remaining()).order(ByteOrder.nativeOrder())
        while (input.remaining() >= Short.SIZE_BYTES) {
            val scaled = (input.short * gain).roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            output.putShort(scaled.toShort())
        }
        inputBuffer.position(inputBuffer.limit())
        output.flip()
    }
}
