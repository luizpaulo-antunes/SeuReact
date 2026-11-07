package com.seureact.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val sourceUri: String,
    val localSourcePath: String,
    val sourceName: String,
    val sourceDurationMs: Long,
    val sourceWidth: Int,
    val sourceHeight: Int,
    val sourceHasAudio: Boolean,
    val sourceMimeType: String,
    val status: String,
    val outputAspect: String,
    val outputResolution: String,
    val cameraFacing: String,
    val cameraShape: String,
    val cameraCenterX: Float,
    val cameraCenterY: Float,
    val cameraWidth: Float,
    val cameraHeight: Float,
    val cameraMirrored: Boolean,
    val sourceVolume: Float,
    val microphoneVolume: Float,
    val cameraVideoPath: String?,
    val microphoneAudioPath: String?,
    val outputUri: String?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val exportProgress: Int,
    val lastError: String?,
)

@Entity(
    tableName = "timeline_events",
    foreignKeys = [ForeignKey(
        entity = ProjectEntity::class,
        parentColumns = ["id"],
        childColumns = ["projectId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("projectId"), Index(value = ["projectId", "finalTimeNanos"])],
)
data class TimelineEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: String,
    val type: String,
    val finalTimeNanos: Long,
    val sourcePositionMs: Long,
    val booleanValue: Boolean? = null,
    val floatValue: Float? = null,
    val secondaryFloatValue: Float? = null,
    val centerX: Float? = null,
    val centerY: Float? = null,
    val width: Float? = null,
    val height: Float? = null,
    val mirrored: Boolean? = null,
    val cameraShape: String? = null,
    val cameraFacing: String? = null,
)
