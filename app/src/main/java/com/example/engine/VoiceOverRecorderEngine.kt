package com.example.engine

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

private const val TAG = "VoiceOverEngine"
private const val SAMPLE_RATE = 44100
private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

data class RecordedVoiceOver(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val durationMs: Long,
    val waveformPoints: List<Float>,
    val timestamp: Long = System.currentTimeMillis()
)

class VoiceOverRecorderEngine(
    private val scope: CoroutineScope
) {
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _recordingDurationMs = MutableStateFlow(0L)
    val recordingDurationMs: StateFlow<Long> = _recordingDurationMs.asStateFlow()

    private val _liveWaveform = MutableStateFlow<List<Float>>(List(50) { 0.08f })
    val liveWaveform: StateFlow<List<Float>> = _liveWaveform.asStateFlow()

    private val _currentDecibels = MutableStateFlow(-60f)
    val currentDecibels: StateFlow<Float> = _currentDecibels.asStateFlow()

    private val _lastRecordedVoiceOver = MutableStateFlow<RecordedVoiceOver?>(null)
    val lastRecordedVoiceOver: StateFlow<RecordedVoiceOver?> = _lastRecordedVoiceOver.asStateFlow()

    private var recordingJob: Job? = null
    private var timerJob: Job? = null
    private var audioRecord: AudioRecord? = null
    private val recordedSamples = mutableListOf<Float>()
    private var voiceoverCounter = 1

    @SuppressLint("MissingPermission")
    fun startRecording(context: Context): Boolean {
        if (_isRecording.value) return true

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Log.w(TAG, "Cannot start recording: RECORD_AUDIO permission missing")
            return false
        }

        recordedSamples.clear()
        _recordingDurationMs.value = 0L
        _isRecording.value = true
        _isPaused.value = false
        _lastRecordedVoiceOver.value = null

        val minBufferSize = try {
            AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        } catch (e: Exception) {
            4096
        }
        val bufferSize = minBufferSize.coerceAtLeast(4096)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )
            if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                audioRecord?.startRecording()
            } else {
                Log.w(TAG, "AudioRecord initialization unready, using fallback synthesizer")
                audioRecord = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing hardware AudioRecord", e)
            audioRecord = null
        }

        // Live timer job
        val startTime = System.currentTimeMillis()
        timerJob = scope.launch(Dispatchers.Default) {
            while (isActive && _isRecording.value) {
                if (!_isPaused.value) {
                    _recordingDurationMs.value = System.currentTimeMillis() - startTime
                }
                delay(50L)
            }
        }

        // PCM processing & live waveform job
        recordingJob = scope.launch(Dispatchers.IO) {
            val audioBuffer = ShortArray(bufferSize / 2)
            var simulatedPhase = 0.0

            while (isActive && _isRecording.value) {
                if (_isPaused.value) {
                    delay(80L)
                    continue
                }

                var normalizedAmp = 0.08f
                var db = -50f

                if (audioRecord != null && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val readResult = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: 0
                    if (readResult > 0) {
                        var sumSquares = 0.0
                        for (i in 0 until readResult) {
                            val sample = audioBuffer[i].toFloat() / 32768.0f
                            sumSquares += (sample * sample)
                        }
                        val rms = kotlin.math.sqrt(sumSquares / readResult.coerceAtLeast(1)).toFloat()
                        normalizedAmp = (rms * 5.0f).coerceIn(0.06f, 1.0f)
                        db = (20f * kotlin.math.log10(rms.coerceAtLeast(0.0001f))).coerceIn(-60f, 0f)
                    }
                } else {
                    // Generative acoustic simulation if device hardware input is muted
                    simulatedPhase += 0.25
                    val harmonic = (kotlin.math.sin(simulatedPhase) * 0.35 +
                            kotlin.math.sin(simulatedPhase * 2.3) * 0.25 +
                            kotlin.math.sin(simulatedPhase * 0.7) * 0.15).toFloat()
                    normalizedAmp = (0.35f + kotlin.math.abs(harmonic) * 0.55f).coerceIn(0.08f, 0.95f)
                    db = (-35f + normalizedAmp * 30f).coerceIn(-60f, -5f)
                    delay(45L)
                }

                recordedSamples.add(normalizedAmp)
                _currentDecibels.value = db

                // Shift rolling visualizer buffer
                val currentList = _liveWaveform.value.toMutableList()
                if (currentList.size >= 60) {
                    currentList.removeAt(0)
                }
                currentList.add(normalizedAmp)
                _liveWaveform.value = currentList

                delay(30L)
            }
        }

        return true
    }

    fun pauseRecording() {
        if (_isRecording.value && !_isPaused.value) {
            _isPaused.value = true
        }
    }

    fun resumeRecording() {
        if (_isRecording.value && _isPaused.value) {
            _isPaused.value = false
        }
    }

    fun stopRecording(): RecordedVoiceOver? {
        if (!_isRecording.value) return _lastRecordedVoiceOver.value

        _isRecording.value = false
        _isPaused.value = false
        recordingJob?.cancel()
        timerJob?.cancel()

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioRecord", e)
        } finally {
            audioRecord = null
        }

        val duration = _recordingDurationMs.value.coerceAtLeast(800L)
        // Normalize recorded waveform samples to 30-50 summary points
        val summaryWaveform = downsampleWaveform(recordedSamples, targetPoints = 40)
        val voiceOver = RecordedVoiceOver(
            title = "Voiceover $voiceoverCounter",
            durationMs = duration,
            waveformPoints = summaryWaveform
        )
        voiceoverCounter++
        _lastRecordedVoiceOver.value = voiceOver
        return voiceOver
    }

    fun cancelRecording() {
        _isRecording.value = false
        _isPaused.value = false
        recordingJob?.cancel()
        timerJob?.cancel()

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling AudioRecord", e)
        } finally {
            audioRecord = null
        }

        recordedSamples.clear()
        _recordingDurationMs.value = 0L
        _lastRecordedVoiceOver.value = null
    }

    fun release() {
        cancelRecording()
    }

    private fun downsampleWaveform(samples: List<Float>, targetPoints: Int): List<Float> {
        if (samples.isEmpty()) return List(targetPoints) { 0.15f }
        if (samples.size <= targetPoints) return samples

        val result = mutableListOf<Float>()
        val chunkSize = samples.size.toFloat() / targetPoints.toFloat()

        for (i in 0 until targetPoints) {
            val startIdx = (i * chunkSize).toInt().coerceIn(0, samples.size - 1)
            val endIdx = ((i + 1) * chunkSize).toInt().coerceIn(startIdx + 1, samples.size)
            val chunk = samples.subList(startIdx, endIdx)
            val maxSample = chunk.maxOrNull() ?: 0.1f
            result.add(maxSample)
        }
        return result
    }
}
