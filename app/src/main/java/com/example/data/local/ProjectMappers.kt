package com.example.data.local

import com.example.data.SampleProjects
import com.example.model.AspectRatio
import com.example.model.ExportResolution
import com.example.model.FrameRate
import com.example.model.Project
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Mapping extensions between domain model [Project] and Room database entity [ProjectEntity],
 * as well as formatting helpers for timestamps and video duration.
 */
object ProjectMappers {

    fun toEntity(project: Project): ProjectEntity {
        val firstVisualClip = project.tracks.flatMap { it.clips }.firstOrNull { it.assetResId != null }
        val thumbResId = firstVisualClip?.assetResId
        val thumbColor = project.tracks.flatMap { it.clips }.firstOrNull { it.colorHex != null }?.colorHex ?: 0xFF141424
        val totalClips = project.tracks.sumOf { it.clips.size }

        return ProjectEntity(
            id = project.id,
            title = project.title,
            aspectRatioName = project.aspectRatio.name,
            aspectRatioRatioText = project.aspectRatio.ratioText,
            frameRateFps = project.frameRate.fps,
            resolutionLabel = project.resolution.label,
            durationMs = project.durationMs,
            thumbnailResId = thumbResId,
            thumbnailColorHex = thumbColor,
            clipCount = totalClips.coerceAtLeast(1),
            trackCount = project.tracks.size.coerceAtLeast(1),
            projectJson = "",
            createdAt = project.createdAt,
            updatedAt = project.updatedAt
        )
    }

    fun toProject(entity: ProjectEntity, fallbackProject: Project? = null): Project {
        if (fallbackProject != null) {
            return fallbackProject.copy(
                id = entity.id,
                title = entity.title,
                updatedAt = entity.updatedAt
            )
        }

        // Check if matches one of the sample projects by id or title pattern
        val matchingSample = SampleProjects.getAllInitialProjects().find { it.id == entity.id }
        if (matchingSample != null) {
            return matchingSample.copy(
                title = entity.title,
                updatedAt = entity.updatedAt
            )
        }

        val aspect = runCatching { AspectRatio.valueOf(entity.aspectRatioName) }.getOrDefault(AspectRatio.PORTRAIT_9_16)
        val fps = when (entity.frameRateFps) {
            24 -> FrameRate.FPS_24
            30 -> FrameRate.FPS_30
            else -> FrameRate.FPS_60
        }
        val resolution = when {
            entity.resolutionLabel.contains("4K", ignoreCase = true) -> ExportResolution.UHD_4K
            entity.resolutionLabel.contains("720", ignoreCase = true) -> ExportResolution.HD_720P
            else -> ExportResolution.FHD_1080P
        }

        return Project(
            id = entity.id,
            title = entity.title,
            aspectRatio = aspect,
            frameRate = fps,
            resolution = resolution,
            durationMs = entity.durationMs,
            tracks = emptyList(),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    fun getInitialEntities(): List<ProjectEntity> {
        return SampleProjects.getAllInitialProjects().map { toEntity(it) }
    }

    fun formatProjectTimestamp(timestampMs: Long): String {
        val now = System.currentTimeMillis()
        val diffMs = (now - timestampMs).coerceAtLeast(0L)
        return when {
            diffMs < 60_000L -> "Just now"
            diffMs < 3_600_000L -> "${diffMs / 60_000L}m ago"
            diffMs < 86_400_000L -> "${diffMs / 3_600_000L}h ago"
            diffMs < 172_800_000L -> "Yesterday"
            else -> {
                val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                sdf.format(Date(timestampMs))
            }
        }
    }

    fun formatDuration(durationMs: Long): String {
        val totalSec = durationMs / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format(Locale.US, "%02d:%02d", min, sec)
    }
}
