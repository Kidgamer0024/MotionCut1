package com.example.engine

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
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
import java.io.File
import java.io.RandomAccessFile
import java.util.UUID
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

private const val TAG = "VoiceOverEngine"
private const val SAMPLE_RATE = 44100
private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
private const val WAV_HEADER_SIZE = 44

data class RecordedVoiceOver(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val durationMs: Long,
    val waveformPoints: List<Float>,
    val fileUri: String? = null,
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

    private val _isHardwareMicActive = MutableStateFlow(false)
    val isHardwareMicActive: StateFlow<Boolean> = _isHardwareMicActive.asStateFlow()

    private val _recorderError = MutableStateFlow<String?>(null)
    val recorderError: StateFlow<String?> = _recorderError.asStateFlow()

    private var recordingJob: Job? = null
    private var timerJob: Job? = null
    private var audioRecord: AudioRecord? = null
    private val recordedSamples = mutableListOf<Float>()
    private var voiceoverCounter = 1

    private val wavLock = Any()
    private var activeWavFile: File? = null
    private var wavWriter: RandomAccessFile? = null
    private var totalPcmBytesWritten = 0L
    @Volatile
    private var isWavWritingActive = false

    private var accumulatedActiveMs = 0L
    private var activeSegmentStartMs = 0L

    @SuppressLint("MissingPermission")
    fun startRecording(context: Context): Boolean {
        if (_isRecording.value) return true

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            _recorderError.value = "Microphone permission is required before recording."
            Log.w(TAG, "Cannot start recording: RECORD_AUDIO permission missing")
            return false
        }

        _recorderError.value = null
        recordedSamples.clear()
        _liveWaveform.value = List(50) { 0.08f }
        _currentDecibels.value = -60f
        _recordingDurationMs.value = 0L
        accumulatedActiveMs = 0L
        activeSegmentStartMs = System.currentTimeMillis()
        _isRecording.value = true
        _isPaused.value = false
        _lastRecordedVoiceOver.value = null

        val minBufferSize = try {
            AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
                .takeIf { it > 0 } ?: 4096
        } catch (_: Exception) {
            4096
        }
        val bufferSize = (minBufferSize * 2).coerceAtLeast(8192)

        audioRecord = createAndStartAudioRecord(bufferSize)
        val micActive = audioRecord != null
        _isHardwareMicActive.value = micActive
        if (!micActive) {
            _recorderError.value = "Hardware microphone unavailable on this device; audio waveform simulated."
        }

        openWavOutputFile(context)

        timerJob = scope.launch(Dispatchers.Default) {
            while (isActive && _isRecording.value) {
                if (!_isPaused.value) {
                    val now = System.currentTimeMillis()
                    _recordingDurationMs.value = accumulatedActiveMs + (now - activeSegmentStartMs)
                }
                delay(50L)
            }
        }

        recordingJob = scope.launch(Dispatchers.IO) {
            val shortCount = (bufferSize / 2).coerceAtLeast(1024)
            val audioBuffer = ShortArray(shortCount)
            val byteBuffer = ByteArray(shortCount * 2)
            var simulatedPhase = 0.0

            while (isActive && _isRecording.value) {
                if (_isPaused.value) {
                    delay(60L)
                    continue
                }

                var normalizedAmp = 0.08f
                var db = -50f
                val activeRecorder = audioRecord

                if (activeRecorder != null && activeRecorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val readResult = try {
                        activeRecorder.read(audioBuffer, 0, audioBuffer.size)
                    } catch (_: Exception) {
                        0
                    }
                    if (readResult > 0) {
                        var sumSquares = 0.0
                        var byteIdx = 0
                        for (i in 0 until readResult) {
                            val pcm = audioBuffer[i]
                            val sample = pcm.toFloat() / 32768.0f
                            sumSquares += (sample * sample)
                            val pcmInt = pcm.toInt()
                            byteBuffer[byteIdx++] = (pcmInt and 0xFF).toByte()
                            byteBuffer[byteIdx++] = ((pcmInt ushr 8) and 0xFF).toByte()
                        }
                        writePcmBytesToWav(byteBuffer, readResult * 2)

                        val rms = sqrt(sumSquares / readResult.coerceAtLeast(1)).toFloat()
                        normalizedAmp = (rms * 5.0f).coerceIn(0.06f, 1.0f)
                        db = (20f * log10(rms.coerceAtLeast(0.0001f))).coerceIn(-60f, 0f)
                    } else if (readResult < 0) {
                        delay(20L)
                    }
                } else {
                    // Fallback metering when host/emulator hardware mic cannot open
                    simulatedPhase += 0.25
                    val harmonic = (sin(simulatedPhase) * 0.35 +
                        sin(simulatedPhase * 2.3) * 0.25 +
                        sin(simulatedPhase * 0.7) * 0.15).toFloat()
                    normalizedAmp = (0.28f + abs(harmonic) * 0.45f).coerceIn(0.08f, 0.90f)
                    db = (-35f + normalizedAmp * 26f).coerceIn(-60f, -8f)
                    delay(45L)
                }

                synchronized(recordedSamples) {
                    recordedSamples.add(normalizedAmp)
                }
                _currentDecibels.value = db

                val currentList = _liveWaveform.value.toMutableList()
                if (currentList.size >= 60) {
                    currentList.removeAt(0)
                }
                currentList.add(normalizedAmp)
                _liveWaveform.value = currentList
            }
        }

        return true
    }

    @SuppressLint("MissingPermission")
    private fun createAndStartAudioRecord(bufferSize: Int): AudioRecord? {
        val sources = intArrayOf(
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.DEFAULT
        )
        for (source in sources) {
            try {
                val candidate = AudioRecord(
                    source,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )
                if (candidate.state == AudioRecord.STATE_INITIALIZED) {
                    candidate.startRecording()
                    if (candidate.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        return candidate
                    }
                }
                runCatching { candidate.release() }
            } catch (e: Exception) {
                Log.w(TAG, "AudioRecord source $source unavailable: ${e.message}")
            }
        }
        return null
    }

    private fun openWavOutputFile(context: Context) {
        synchronized(wavLock) {
            closeWavOutputQuietly(deleteFile = false)
            totalPcmBytesWritten = 0L
            runCatching {
                val dir = File(context.filesDir, "voiceovers").apply { mkdirs() }
                val outFile = File(dir, "voiceover_${System.currentTimeMillis()}.wav")
                val raf = RandomAccessFile(outFile, "rw")
                raf.setLength(0L)
                raf.write(buildWavHeader(0L))
                activeWavFile = outFile
                wavWriter = raf
                isWavWritingActive = true
            }.onFailure { err ->
                Log.w(TAG, "Could not open WAV file for voiceover: ${err.message}")
                activeWavFile = null
                wavWriter = null
                isWavWritingActive = false
            }
        }
    }

    private fun writePcmBytesToWav(bytes: ByteArray, length: Int) {
        if (length <= 0 || !isWavWritingActive) return
        synchronized(wavLock) {
            if (!isWavWritingActive) return
            val raf = wavWriter ?: return
            runCatching {
                raf.write(bytes, 0, length)
                totalPcmBytesWritten += length.toLong()
            }.onFailure { err ->
                Log.w(TAG, "Failed writing WAV PCM bytes: ${err.message}")
            }
        }
    }

    private fun finalizeWavOutputFile(): Pair<String?, Long?> {
        synchronized(wavLock) {
            isWavWritingActive = false
            val file = activeWavFile
            val raf = wavWriter
            val pcmBytes = totalPcmBytesWritten
            wavWriter = null
            activeWavFile = null
            totalPcmBytesWritten = 0L

            if (raf == null || file == null) {
                return null to null
            }

            return runCatching {
                if (pcmBytes <= 0L) {
                    raf.close()
                    file.delete()
                    null to null
                } else {
                    raf.seek(0L)
                    raf.write(buildWavHeader(pcmBytes))
                    raf.fd.sync()
                    raf.close()
                    val wavDurationMs = ((pcmBytes * 1000L) / (SAMPLE_RATE * 2L)).coerceAtLeast(300L)
                    Uri.fromFile(file).toString() to wavDurationMs
                }
            }.getOrElse {
                runCatching { raf.close() }
                runCatching { file.delete() }
                null to null
            }
        }
    }

    private fun closeWavOutputQuietly(deleteFile: Boolean) {
        synchronized(wavLock) {
            runCatching { wavWriter?.close() }
            if (deleteFile) {
                runCatching { activeWavFile?.delete() }
            }
            wavWriter = null
            activeWavFile = null
            totalPcmBytesWritten = 0L
        }
    }

    fun pauseRecording() {
        if (_isRecording.value && !_isPaused.value) {
            accumulatedActiveMs += (System.currentTimeMillis() - activeSegmentStartMs).coerceAtLeast(0L)
            _recordingDurationMs.value = accumulatedActiveMs
            _isPaused.value = true
        }
    }

    fun resumeRecording() {
        if (_isRecording.value && _isPaused.value) {
            activeSegmentStartMs = System.currentTimeMillis()
            _isPaused.value = false
        }
    }

    fun stopRecording(): RecordedVoiceOver? {
        if (!_isRecording.value) return _lastRecordedVoiceOver.value

        if (!_isPaused.value) {
            accumulatedActiveMs += (System.currentTimeMillis() - activeSegmentStartMs).coerceAtLeast(0L)
            _recordingDurationMs.value = accumulatedActiveMs
        }

        isWavWritingActive = false
        _isRecording.value = false
        _isPaused.value = false
        recordingJob?.cancel()
        timerJob?.cancel()
        recordingJob = null
        timerJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioRecord", e)
        } finally {
            audioRecord = null
            _isHardwareMicActive.value = false
        }

        val (savedUri, wavDurationMs) = finalizeWavOutputFile()
        val duration = (wavDurationMs ?: _recordingDurationMs.value).coerceAtLeast(800L)
        val samplesSnapshot = synchronized(recordedSamples) { recordedSamples.toList() }
        val summaryWaveform = downsampleWaveform(samplesSnapshot, targetPoints = 40)

        val voiceOver = RecordedVoiceOver(
            title = "Voiceover $voiceoverCounter",
            durationMs = duration,
            waveformPoints = summaryWaveform,
            fileUri = savedUri
        )
        voiceoverCounter++
        _lastRecordedVoiceOver.value = voiceOver
        return voiceOver
    }

    fun cancelRecording() {
        isWavWritingActive = false
        _isRecording.value = false
        _isPaused.value = false
        recordingJob?.cancel()
        timerJob?.cancel()
        recordingJob = null
        timerJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling AudioRecord", e)
        } finally {
            audioRecord = null
            _isHardwareMicActive.value = false
        }

        closeWavOutputQuietly(deleteFile = true)
        synchronized(recordedSamples) {
            recordedSamples.clear()
        }
        accumulatedActiveMs = 0L
        _recordingDurationMs.value = 0L
        _lastRecordedVoiceOver.value = null
        _recorderError.value = null
    }

    fun clearLastRecording() {
        if (!_isRecording.value) {
            _lastRecordedVoiceOver.value = null
            _recorderError.value = null
            _recordingDurationMs.value = 0L
        }
    }

    fun release() {
        cancelRecording()
    }

    private fun buildWavHeader(pcmDataBytes: Long): ByteArray {
        val totalDataLen = pcmDataBytes + (WAV_HEADER_SIZE - 8)
        val byteRate = (SAMPLE_RATE * 1 * 16) / 8
        val header = ByteArray(WAV_HEADER_SIZE)

        // "RIFF"
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        writeIntLE(header, 4, totalDataLen.toInt())
        // "WAVE"
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        // "fmt "
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        writeIntLE(header, 16, 16) // Subchunk1Size for PCM
        writeShortLE(header, 20, 1) // AudioFormat = 1 (PCM)
        writeShortLE(header, 22, 1) // NumChannels = 1 (Mono)
        writeIntLE(header, 24, SAMPLE_RATE)
        writeIntLE(header, 28, byteRate)
        writeShortLE(header, 32, 2) // BlockAlign = NumChannels * BitsPerSample / 8
        writeShortLE(header, 34, 16) // BitsPerSample
        // "data"
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        writeIntLE(header, 40, pcmDataBytes.toInt())
        return header
    }

    private fun writeIntLE(buf: ByteArray, offset: Int, value: Int) {
        buf[offset] = (value and 0xFF).toByte()
        buf[offset + 1] = ((value ushr 8) and 0xFF).toByte()
        buf[offset + 2] = ((value ushr 16) and 0xFF).toByte()
        buf[offset + 3] = ((value ushr 24) and 0xFF).toByte()
    }

    private fun writeShortLE(buf: ByteArray, offset: Int, value: Int) {
        buf[offset] = (value and 0xFF).toByte()
        buf[offset + 1] = ((value ushr 8) and 0xFF).toByte()
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
