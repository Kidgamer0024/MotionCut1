package com.example.model

import java.util.UUID

enum class AspectRatio(val ratioText: String, val useCase: String, val ratioFloat: Float) {
    PORTRAIT_9_16("9:16", "TikTok / Reels / Shorts", 9f / 16f),
    LANDSCAPE_16_9("16:9", "YouTube / Cinema", 16f / 9f),
    SQUARE_1_1("1:1", "Instagram Feed", 1f),
    PORTRAIT_4_5("4:5", "Portrait Feed", 4f / 5f),
    CINEMATIC_21_9("21:9", "Anamorphic Scope", 21f / 9f)
}

enum class ExportResolution(val label: String, val width: Int, val height: Int, val bitrateMbps: Float) {
    HD_720P("720p HD", 1280, 720, 6.0f),
    FHD_1080P("1080p Full HD", 1920, 1080, 12.0f),
    UHD_4K("4K Ultra HD", 3840, 2160, 35.0f)
}

enum class FrameRate(val fps: Int, val label: String) {
    FPS_24(24, "24 fps (Cinematic)"),
    FPS_30(30, "30 fps (Standard)"),
    FPS_60(60, "60 fps (Smooth)")
}

data class Project(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val aspectRatio: AspectRatio = AspectRatio.PORTRAIT_9_16,
    val frameRate: FrameRate = FrameRate.FPS_60,
    val resolution: ExportResolution = ExportResolution.FHD_1080P,
    val durationMs: Long = 10_000L,
    val tracks: List<Track> = emptyList(),
    val masterColorGrading: ColorGradingSettings = ColorGradingSettings(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun findClip(clipId: String): ClipItem? {
        for (track in tracks) {
            val clip = track.clips.find { it.id == clipId }
            if (clip != null) return clip
        }
        return null
    }

    fun findTrackForClip(clipId: String): Track? {
        return tracks.find { track -> track.clips.any { it.id == clipId } }
    }
}
