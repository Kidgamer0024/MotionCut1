package com.example.model

import java.util.UUID

enum class MusicCategory(val label: String, val description: String) {
    ALL("All Genres", "Full royalty-free audio catalog"),
    UPBEAT("Upbeat", "High energy, positive, bouncy funk and viral rhythms"),
    CINEMATIC("Cinematic", "Epic orchestral swells, dramatic strings and trailer braams"),
    AMBIENT("Ambient", "Serene atmospheric pads, lofi textures and deep focus"),
    ELECTRONIC("Electronic", "Cyberpunk basslines, synthwave arps and punchy beats")
}

data class MusicTrack(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val artist: String,
    val category: MusicCategory,
    val durationSec: Int,
    val bpm: Int,
    val musicalKey: String,
    val waveformSamples: List<Float>,
    val baseFrequencyHz: Float = 220f,
    val synthPatternId: Int = 0,
    val coverColorHex: Long = 0xFF1E3A5F,
    val tags: List<String> = emptyList()
) {
    val durationFormatted: String
        get() = String.format("%02d:%02d", durationSec / 60, durationSec % 60)
}
