package com.seureact.app.media.export

import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import com.seureact.app.domain.model.ReactionProject
import com.seureact.app.domain.model.SourceMetadata
import org.junit.Assert.assertEquals
import org.junit.Test

@UnstableApi
class ReactionVideoCompositorSettingsTest {
    @Test
    fun `original output keeps portrait source aspect ratio`() {
        val project = ReactionProject(
            sourceUri = "content://source",
            localSourcePath = "/source.mp4",
            metadata = SourceMetadata("vertical.mp4", 1_000, 1080, 1920, true, "video/mp4"),
        )
        val settings = ReactionVideoCompositorSettings(project, emptyList(), Size(1080, 1920))

        val output = settings.getOutputSize(listOf(Size(1920, 1080), Size(1080, 1920)))

        assertEquals(720, output.width)
        assertEquals(1280, output.height)
    }
}
