package com.example.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import com.example.model.MusicCategory
import com.example.model.MusicTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.sin

class AudioSynthesizerEngine(
    private val scope: CoroutineScope
) {
    private val _isPlayingPreview = MutableStateFlow(false)
    val isPlayingPreview: StateFlow<Boolean> = _isPlayingPreview.asStateFlow()

    private val _currentTrackId = MutableStateFlow<String?>(null)
    val currentTrackId: StateFlow<String?> = _currentTrackId.asStateFlow()

    private val _previewProgress = MutableStateFlow(0f)
    val previewProgress: StateFlow<Float> = _previewProgress.asStateFlow()

    private var audioJob: Job? = null
    private var audioTrack: AudioTrack? = null
    private var mediaPlayer: MediaPlayer? = null

    fun playTrack(track: MusicTrack) {
        if (_isPlayingPreview.value && _currentTrackId.value == track.id) {
            stop()
            return
        }

        stop()
        _currentTrackId.value = track.id
        _isPlayingPreview.value = true

        audioJob = scope.launch(Dispatchers.Default) {
            val sampleRate = 44100
            val minBufSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(4096)

            val trackPlayer = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBufSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack = trackPlayer
            try {
                trackPlayer.play()
            } catch (e: Exception) {
                _isPlayingPreview.value = false
                return@launch
            }

            // Generate musical chord progression & melody based on genre
            val chordProgression = when (track.category) {
                MusicCategory.UPBEAT -> listOf(261.63f, 329.63f, 392.00f, 440.00f) // C - E - G - A
                MusicCategory.CINEMATIC -> listOf(220.00f, 261.63f, 329.63f, 196.00f) // A minor - C - E - G
                MusicCategory.AMBIENT -> listOf(174.61f, 220.00f, 261.63f, 329.63f) // F maj7 ambient
                MusicCategory.ELECTRONIC -> listOf(146.83f, 174.61f, 220.00f, 293.66f) // D minor punch
                else -> listOf(220.00f, 277.18f, 329.63f, 440.00f)
            }

            val totalDurationMs = track.durationSec * 1000L
            val startTime = System.currentTimeMillis()
            val chunkSamples = 2048
            val buffer = ShortArray(chunkSamples)
            var phase = 0.0

            while (_isPlayingPreview.value) {
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed >= totalDurationMs) {
                    _previewProgress.value = 1.0f
                    break
                }
                _previewProgress.value = (elapsed.toFloat() / totalDurationMs).coerceIn(0f, 1f)

                // Select note from progression
                val stepIdx = ((elapsed / 500) % chordProgression.size).toInt()
                val targetFreq = chordProgression[stepIdx]

                for (i in 0 until chunkSamples) {
                    val t = (elapsed * 0.001) + (i.toDouble() / sampleRate)
                    // Harmonic blend of fundamental + 2nd harmonic + sub bass
                    val wave = 0.6 * sin(phase) +
                               0.25 * sin(phase * 2.0) +
                               0.15 * sin(phase * 0.5)

                    phase += 2.0 * Math.PI * targetFreq / sampleRate
                    if (phase > 2.0 * Math.PI) phase -= 2.0 * Math.PI

                    // Apply soft envelope
                    val sample = (wave * 18000.0).toInt().coerceIn(-32767, 32767).toShort()
                    buffer[i] = sample
                }

                trackPlayer.write(buffer, 0, chunkSamples)
            }

            stop()
        }
    }

    fun playAudioUri(context: Context, id: String, uriString: String, durationMs: Long) {
        if (_isPlayingPreview.value && _currentTrackId.value == id) {
            stop()
            return
        }

        stop()
        _currentTrackId.value = id
        _isPlayingPreview.value = true

        audioJob = scope.launch(Dispatchers.Main) {
            try {
                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    setDataSource(context, Uri.parse(uriString))
                    prepare()
                    start()
                }
                mediaPlayer = player
                val total = (player.duration.takeIf { it > 0 }?.toLong() ?: durationMs).coerceAtLeast(1000L)
                while (_isPlayingPreview.value && player.isPlaying) {
                    _previewProgress.value = (player.currentPosition.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                    delay(120L)
                }
            } catch (_: Exception) {
                // fallback clean stop
            } finally {
                stop()
            }
        }
    }

    fun stop() {
        _isPlayingPreview.value = false
        _currentTrackId.value = null
        _previewProgress.value = 0f
        audioJob?.cancel()
        audioJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            // ignore cleanup errors
        }
        audioTrack = null
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            // ignore cleanup errors
        }
        mediaPlayer = null
    }

    fun release() {
        stop()
    }
}
