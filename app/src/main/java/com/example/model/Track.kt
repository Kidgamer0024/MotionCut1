package com.example.model

import java.util.UUID

enum class TrackType(val label: String, val colorHex: Long) {
    MAIN_VIDEO("Main Video", 0xFF1E3A5F),
    OVERLAY("Overlay / PiP", 0xFF2A2050),
    EFFECTS("Effects Layer", 0xFF432860),
    TEXT_STICKER("Titles & Shapes", 0xFF5A381E),
    AUDIO("Audio Track", 0xFF144D38)
}

data class Track(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: TrackType,
    val isMuted: Boolean = false,
    val isLocked: Boolean = false,
    val isVisible: Boolean = true,
    val clips: List<ClipItem> = emptyList()
)
