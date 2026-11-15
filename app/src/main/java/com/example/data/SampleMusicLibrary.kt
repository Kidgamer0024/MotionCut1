package com.example.data

import com.example.model.MusicCategory
import com.example.model.MusicTrack

object SampleMusicLibrary {

    fun getAllTracks(): List<MusicTrack> {
        return listOf(
            // --- UPBEAT ---
            MusicTrack(
                id = "track_summer_funk",
                title = "Summer Funk Groove",
                artist = "Horizon Wave",
                category = MusicCategory.UPBEAT,
                durationSec = 135,
                bpm = 124,
                musicalKey = "C Major",
                waveformSamples = listOf(0.3f, 0.6f, 0.9f, 0.4f, 0.8f, 0.7f, 0.95f, 0.5f, 0.85f, 0.4f, 0.7f, 0.9f, 0.6f, 0.8f, 0.5f, 0.95f, 0.7f, 0.4f, 0.6f, 0.8f),
                baseFrequencyHz = 261.63f,
                synthPatternId = 0,
                coverColorHex = 0xFFFF9500,
                tags = listOf("Bouncy", "Funk", "Summer", "Upbeat")
            ),
            MusicTrack(
                id = "track_electric_sun",
                title = "Electric Sunshine",
                artist = "Neon Disco Collective",
                category = MusicCategory.UPBEAT,
                durationSec = 108,
                bpm = 128,
                musicalKey = "G Major",
                waveformSamples = listOf(0.4f, 0.7f, 0.5f, 0.9f, 0.8f, 0.6f, 0.95f, 0.7f, 0.5f, 0.85f, 0.9f, 0.7f, 0.6f, 0.85f, 0.5f, 0.8f, 0.9f, 0.6f, 0.7f, 0.85f),
                baseFrequencyHz = 392.00f,
                synthPatternId = 0,
                coverColorHex = 0xFFFFCC00,
                tags = listOf("Disco", "Party", "Pop", "Happy")
            ),
            MusicTrack(
                id = "track_viral_bounce",
                title = "Viral Beat Bounce",
                artist = "TokRhythm",
                category = MusicCategory.UPBEAT,
                durationSec = 95,
                bpm = 132,
                musicalKey = "A Major",
                waveformSamples = listOf(0.5f, 0.9f, 0.6f, 0.8f, 0.95f, 0.7f, 0.6f, 0.9f, 0.5f, 0.8f, 0.95f, 0.7f, 0.5f, 0.85f, 0.9f, 0.6f, 0.7f, 0.9f, 0.8f, 0.6f),
                baseFrequencyHz = 440.00f,
                synthPatternId = 0,
                coverColorHex = 0xFFFF2D55,
                tags = listOf("Viral", "Trap Beat", "Bouncy", "Trending")
            ),

            // --- CINEMATIC ---
            MusicTrack(
                id = "track_kingdom_light",
                title = "Kingdom of Light",
                artist = "London Film Orchestra",
                category = MusicCategory.CINEMATIC,
                durationSec = 190,
                bpm = 88,
                musicalKey = "D Minor",
                waveformSamples = listOf(0.2f, 0.3f, 0.5f, 0.7f, 0.9f, 0.95f, 0.85f, 0.7f, 0.5f, 0.8f, 0.95f, 0.9f, 0.75f, 0.6f, 0.8f, 0.95f, 0.7f, 0.5f, 0.3f, 0.2f),
                baseFrequencyHz = 146.83f,
                synthPatternId = 1,
                coverColorHex = 0xFF5856D6,
                tags = listOf("Epic", "Orchestra", "Trailer", "Drama")
            ),
            MusicTrack(
                id = "track_last_horizon",
                title = "The Last Horizon",
                artist = "Hans Vance",
                category = MusicCategory.CINEMATIC,
                durationSec = 165,
                bpm = 78,
                musicalKey = "A Minor",
                waveformSamples = listOf(0.15f, 0.3f, 0.45f, 0.6f, 0.8f, 0.95f, 0.85f, 0.7f, 0.6f, 0.85f, 0.9f, 0.75f, 0.6f, 0.8f, 0.95f, 0.7f, 0.5f, 0.4f, 0.25f, 0.15f),
                baseFrequencyHz = 220.00f,
                synthPatternId = 1,
                coverColorHex = 0xFF007AFF,
                tags = listOf("Emotional", "Piano", "Strings", "Cinematic")
            ),

            // --- AMBIENT ---
            MusicTrack(
                id = "track_floating_stars",
                title = "Floating in Starlight",
                artist = "Lofi Dreamer",
                category = MusicCategory.AMBIENT,
                durationSec = 150,
                bpm = 70,
                musicalKey = "F Major",
                waveformSamples = listOf(0.3f, 0.4f, 0.5f, 0.6f, 0.55f, 0.5f, 0.65f, 0.6f, 0.5f, 0.55f, 0.6f, 0.5f, 0.45f, 0.55f, 0.6f, 0.5f, 0.45f, 0.4f, 0.35f, 0.3f),
                baseFrequencyHz = 174.61f,
                synthPatternId = 2,
                coverColorHex = 0xFF34C759,
                tags = listOf("Lofi", "Chill", "Relaxing", "Soft Pads")
            ),
            MusicTrack(
                id = "track_deep_nebula",
                title = "Deep Nebula Meditation",
                artist = "Zero Gravity Sounds",
                category = MusicCategory.AMBIENT,
                durationSec = 225,
                bpm = 65,
                musicalKey = "G Minor",
                waveformSamples = listOf(0.2f, 0.35f, 0.4f, 0.5f, 0.6f, 0.55f, 0.5f, 0.6f, 0.65f, 0.55f, 0.5f, 0.6f, 0.55f, 0.5f, 0.45f, 0.5f, 0.45f, 0.35f, 0.3f, 0.2f),
                baseFrequencyHz = 196.00f,
                synthPatternId = 2,
                coverColorHex = 0xFF5AC8FA,
                tags = listOf("Atmosphere", "Space", "Calm", "Drone")
            ),

            // --- ELECTRONIC ---
            MusicTrack(
                id = "track_cyber_pursuit",
                title = "Cyberpunk Pursuit 2099",
                artist = "SynthCore Protocol",
                category = MusicCategory.ELECTRONIC,
                durationSec = 140,
                bpm = 135,
                musicalKey = "D Minor",
                waveformSamples = listOf(0.6f, 0.85f, 0.95f, 0.7f, 0.9f, 0.8f, 0.95f, 0.85f, 0.6f, 0.95f, 0.9f, 0.7f, 0.85f, 0.95f, 0.7f, 0.85f, 0.95f, 0.8f, 0.65f, 0.5f),
                baseFrequencyHz = 146.83f,
                synthPatternId = 3,
                coverColorHex = 0xFF00E5FF,
                tags = listOf("Cyberpunk", "Synthwave", "Driving", "Bass")
            ),
            MusicTrack(
                id = "track_neon_drift",
                title = "Neon Highway Drift",
                artist = "RetroWave King",
                category = MusicCategory.ELECTRONIC,
                durationSec = 160,
                bpm = 128,
                musicalKey = "F# Minor",
                waveformSamples = listOf(0.5f, 0.75f, 0.9f, 0.65f, 0.85f, 0.75f, 0.9f, 0.8f, 0.55f, 0.9f, 0.85f, 0.65f, 0.8f, 0.9f, 0.65f, 0.8f, 0.9f, 0.75f, 0.6f, 0.45f),
                baseFrequencyHz = 185.00f,
                synthPatternId = 3,
                coverColorHex = 0xFFAF52DE,
                tags = listOf("Outrun", "Arpeggio", "Night Drive", "Retro")
            )
        )
    }
}
