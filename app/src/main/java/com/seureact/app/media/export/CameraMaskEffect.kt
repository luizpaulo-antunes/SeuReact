package com.seureact.app.media.export

import android.content.Context
import android.opengl.GLES20
import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.common.util.GlProgram
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BaseGlShaderProgram
import androidx.media3.effect.GlEffect
import androidx.media3.effect.GlShaderProgram
import com.seureact.app.R
import com.seureact.app.domain.model.CameraShape
import java.io.IOException

@UnstableApi
internal class CameraMaskEffect(private val shape: CameraShape) : GlEffect {
    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram =
        CameraMaskShaderProgram(context, useHdr, shape)

    internal val shapeUniform: Int get() = when (shape) {
        CameraShape.CIRCLE -> 0
        CameraShape.SQUARE -> 1
        CameraShape.ROUNDED_RECTANGLE -> 2
    }
}

@UnstableApi
private class CameraMaskShaderProgram(
    context: Context,
    useHdr: Boolean,
    shape: CameraShape,
) : BaseGlShaderProgram(useHdr, 1) {
    private val glProgram = try {
        GlProgram(context, R.raw.camera_mask_vertex_shader, R.raw.camera_mask_fragment_shader)
    } catch (error: IOException) {
        throw VideoFrameProcessingException(error)
    } catch (error: GlUtil.GlException) {
        throw VideoFrameProcessingException(error)
    }

    init {
        val identity = GlUtil.create4x4IdentityMatrix()
        glProgram.setBufferAttribute("aFramePosition", GlUtil.getNormalizedCoordinateBounds(), 4)
        glProgram.setFloatsUniform("uTransformationMatrix", identity)
        glProgram.setFloatsUniform("uTexTransformationMatrix", identity)
        glProgram.setIntUniform("uShape", CameraMaskEffect(shape).shapeUniform)
    }

    override fun configure(inputWidth: Int, inputHeight: Int) = Size(inputWidth, inputHeight)

    override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
        try {
            glProgram.use()
            glProgram.setSamplerTexIdUniform("uTexSampler", inputTexId, 0)
            glProgram.bindAttributesAndUniforms()
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            GlUtil.checkGlError()
        } catch (error: GlUtil.GlException) {
            throw VideoFrameProcessingException(error, presentationTimeUs)
        }
    }
}
