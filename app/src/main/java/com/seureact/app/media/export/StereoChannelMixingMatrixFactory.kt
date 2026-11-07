package com.seureact.app.media.export

import androidx.media3.common.audio.ChannelMixingMatrix
import androidx.media3.common.util.UnstableApi
import kotlin.math.sqrt

@UnstableApi
internal object StereoChannelMixingMatrixFactory {
    const val OUTPUT_CHANNEL_COUNT = 2

    fun create(inputChannelCount: Int): ChannelMixingMatrix = try {
        ChannelMixingMatrix.createForConstantPower(inputChannelCount, OUTPUT_CHANNEL_COUNT)
    } catch (_: UnsupportedOperationException) {
        ChannelMixingMatrix(inputChannelCount, OUTPUT_CHANNEL_COUNT, fallbackCoefficients(inputChannelCount))
    }

    internal fun fallbackCoefficients(inputChannelCount: Int): FloatArray {
        require(inputChannelCount > 0) { "A quantidade de canais deve ser positiva" }
        val coefficients = FloatArray(inputChannelCount * OUTPUT_CHANNEL_COUNT)
        if (inputChannelCount == 1) {
            val coefficient = 1f / sqrt(2f)
            coefficients[0] = coefficient
            coefficients[1] = coefficient
            return coefficients
        }

        set(coefficients, 0, left = 1f, right = 0f)
        set(coefficients, 1, left = 0f, right = 1f)
        if (inputChannelCount > 2) set(coefficients, 2, left = CENTER_GAIN, right = CENTER_GAIN)
        if (inputChannelCount > 3) set(coefficients, 3, left = LFE_GAIN, right = LFE_GAIN)

        for (channel in 4 until inputChannelCount) {
            val isUnpairedLastChannel = channel == inputChannelCount - 1 && inputChannelCount % 2 != 0
            when {
                isUnpairedLastChannel -> set(coefficients, channel, CENTER_GAIN, CENTER_GAIN)
                channel % 2 == 0 -> set(coefficients, channel, left = 1f, right = 0f)
                else -> set(coefficients, channel, left = 0f, right = 1f)
            }
        }
        normalizeOutputPower(coefficients, inputChannelCount)
        return coefficients
    }

    private fun set(coefficients: FloatArray, inputChannel: Int, left: Float, right: Float) {
        val offset = inputChannel * OUTPUT_CHANNEL_COUNT
        coefficients[offset] = left
        coefficients[offset + 1] = right
    }

    private fun normalizeOutputPower(coefficients: FloatArray, inputChannelCount: Int) {
        repeat(OUTPUT_CHANNEL_COUNT) { outputChannel ->
            var power = 0f
            repeat(inputChannelCount) { inputChannel ->
                val coefficient = coefficients[inputChannel * OUTPUT_CHANNEL_COUNT + outputChannel]
                power += coefficient * coefficient
            }
            if (power > 1f) {
                val scale = 1f / sqrt(power)
                repeat(inputChannelCount) { inputChannel ->
                    val index = inputChannel * OUTPUT_CHANNEL_COUNT + outputChannel
                    coefficients[index] *= scale
                }
            }
        }
    }

    private const val CENTER_GAIN = 0.70710677f
    private const val LFE_GAIN = 0.5f
}
