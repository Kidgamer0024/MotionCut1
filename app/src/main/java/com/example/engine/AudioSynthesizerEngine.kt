package com.example.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
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

    fun playTrack(
        track: MusicTrack,
        speed: Float = 1.0f,
        preservePitch: Boolean = true,
        isReversed: Boolean = false
    ) {
        if (_isPlayingPreview.value && _currentTrackId.value == track.id) {
            stop()
            return
        }

        stop()
        _currentTrackId.value = track.id
        _isPlayingPreview.value = true
        val safeSpeed = speed.coerceIn(0.25f, 4.0f)

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

            val baseProgression = when (track.category) {
                MusicCategory.UPBEAT -> listOf(261.63f, 329.63f, 392.00f, 440.00f)
                MusicCategory.CINEMATIC -> listOf(220.00f, 261.63f, 329.63f, 196.00f)
                MusicCategory.AMBIENT -> listOf(174.61f, 220.00f, 261.63f, 329.63f)
                MusicCategory.ELECTRONIC -> listOf(146.83f, 174.61f, 220.00f, 293.66f)
                else -> listOf(220.00f, 277.18f, 329.63f, 440.00f)
            }
            val chordProgression = if (isReversed) baseProgression.reversed() else baseProgression

            val totalDurationMs = ((track.durationSec * 1000L) / safeSpeed).toLong().coerceAtLeast(500L)
            val startTime = System.currentTimeMillis()
            val chunkSamples = 2048
            val buffer = ShortArray(chunkSamples)
            var phase = 0.0
            val pitchMult = if (preservePitch) 1.0f else safeSpeed
            val stepMs = (500f / safeSpeed).toLong().coerceAtLeast(100L)

            while (_isPlayingPreview.value) {
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed >= totalDurationMs) {
                    _previewProgress.value = 1.0f
                    break
                }
                _previewProgress.value = (elapsed.toFloat() / totalDurationMs).coerceIn(0f, 1f)

                val stepIdx = ((elapsed / stepMs) % chordProgression.size).toInt()
                val targetFreq = chordProgression[stepIdx] * pitchMult

                for (i in 0 until chunkSamples) {
                    val wave = 0.6 * sin(phase) +
                               0.25 * sin(phase * 2.0) +
                               0.15 * sin(phase * 0.5)

                    phase += 2.0 * Math.PI * targetFreq / sampleRate
                    if (phase > 2.0 * Math.PI) phase -= 2.0 * Math.PI

                    val sample = (wave * 18000.0).toInt().coerceIn(-32767, 32767).toShort()
                    buffer[i] = sample
                }

                trackPlayer.write(buffer, 0, chunkSamples)
            }

            stop()
        }
    }

    fun playAudioUri(
        context: Context,
        id: String,
        uriString: String,
        durationMs: Long,
        speed: Float = 1.0f,
        preservePitch: Boolean = true
    ) {
        if (_isPlayingPreview.value && _currentTrackId.value == id) {
            stop()
            return
        }

        stop()
        _currentTrackId.value = id
        _isPlayingPreview.value = true
        val safeSpeed = speed.coerceIn(0.25f, 4.0f)

        audioJob = scope.launch(Dispatchers.Main) {
            try {
                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    val parsedUri = Uri.parse(uriString)
                    if (parsedUri.scheme == "file" && !parsedUri.path.isNullOrBlank()) {
                        setDataSource(parsedUri.path)
                    } else {
                        setDataSource(context, parsedUri)
                    }
                    prepare()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        runCatching {
                            playbackParams = PlaybackParams().apply {
                                this.speed = safeSpeed
                                this.pitch = if (preservePitch) 1.0f else safeSpeed
                            }
                        }
                    }
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
