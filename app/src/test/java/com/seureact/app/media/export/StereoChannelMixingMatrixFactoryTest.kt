package com.seureact.app.media.export

import androidx.media3.common.util.UnstableApi
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@UnstableApi
class StereoChannelMixingMatrixFactoryTest {
    @Test
    fun `seven channels always produce a stereo matrix`() {
        val matrix = StereoChannelMixingMatrixFactory.create(7)

        assertEquals(7, matrix.inputChannelCount)
        assertEquals(2, matrix.outputChannelCount)
    }

    @Test
    fun `fallback downmix keeps output power normalized`() {
        val coefficients = StereoChannelMixingMatrixFactory.fallbackCoefficients(7)

        repeat(2) { outputChannel ->
            val power = (0 until 7).sumOf { inputChannel ->
                val coefficient = coefficients[inputChannel * 2 + outputChannel].toDouble()
                coefficient * coefficient
            }
            assertTrue(power <= 1.000001)
        }
    }

    @Test
    fun `unpaired surround channel remains centered`() {
        val coefficients = StereoChannelMixingMatrixFactory.fallbackCoefficients(7)
        val left = coefficients[6 * 2]
        val right = coefficients[6 * 2 + 1]

        assertTrue(abs(left - right) < 0.000001f)
    }
}
