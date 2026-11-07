package com.seureact.app

import android.content.Context
import com.seureact.app.data.RoomProjectRepository
import com.seureact.app.data.local.ReactionDatabase
import com.seureact.app.domain.repository.ProjectRepository
import com.seureact.app.media.export.AndroidReactionExporter
import com.seureact.app.media.export.ReactionExporter
import com.seureact.app.media.importer.SourceVideoImporter
import androidx.media3.common.util.UnstableApi

class AppContainer(context: Context) {
    private val database = ReactionDatabase.create(context)
    val projectRepository: ProjectRepository = RoomProjectRepository(database.reactionDao())
    val sourceImporter = SourceVideoImporter(context)
    @androidx.annotation.OptIn(markerClass = [UnstableApi::class])
    val reactionExporter: ReactionExporter = AndroidReactionExporter(context)
}
