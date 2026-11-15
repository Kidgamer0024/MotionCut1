package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Database entity representing a saved video project.
 * Contains thumbnail references, video metadata, aspect ratio,
 * creation and last-modified timestamps for the landing view.
 */
@Entity(tableName = "video_projects")
data class ProjectEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val aspectRatioName: String = "PORTRAIT_9_16",
    val aspectRatioRatioText: String = "9:16",
    val frameRateFps: Int = 60,
    val resolutionLabel: String = "1080p Full HD",
    val durationMs: Long = 10_000L,
    val thumbnailResId: Int? = null,
    val thumbnailColorHex: Long = 0xFF1E1E2E,
    val clipCount: Int = 1,
    val trackCount: Int = 1,
    val projectJson: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
