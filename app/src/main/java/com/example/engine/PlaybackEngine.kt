package com.example.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlaybackEngine(
    private val scope: CoroutineScope
) {
    private val _currentTimeMs = MutableStateFlow(0L)
    val currentTimeMs: StateFlow<Long> = _currentTimeMs.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _isLooping = MutableStateFlow(true)
    val isLooping: StateFlow<Boolean> = _isLooping.asStateFlow()

    private var durationMs = 10_000L
    private var playbackJob: Job? = null

    fun setDuration(duration: Long) {
        durationMs = duration.coerceAtLeast(1000L)
        if (_currentTimeMs.value > durationMs) {
            _currentTimeMs.value = durationMs
        }
    }

    fun play() {
        if (_isPlaying.value) return
        _isPlaying.value = true
        startTicker()
    }

    fun pause() {
        _isPlaying.value = false
        playbackJob?.cancel()
        playbackJob = null
    }

    fun togglePlay() {
        if (_isPlaying.value) pause() else play()
    }

    fun seek(timeMs: Long) {
        _currentTimeMs.value = timeMs.coerceIn(0L, durationMs)
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed.coerceIn(0.25f, 4.0f)
    }

    fun setLooping(loop: Boolean) {
        _isLooping.value = loop
    }

    fun stepFrame(forward: Boolean, fps: Int = 60) {
        pause()
        val frameDurationMs = (1000L / fps).coerceAtLeast(1L)
        val delta = if (forward) frameDurationMs else -frameDurationMs
        seek(_currentTimeMs.value + delta)
    }

    private fun startTicker() {
        playbackJob?.cancel()
        playbackJob = scope.launch(Dispatchers.Default) {
            val frameIntervalMs = 16L // ~60fps
            var lastRealTime = System.currentTimeMillis()

            while (_isPlaying.value) {
                delay(frameIntervalMs)
                val now = System.currentTimeMillis()
                val realDelta = now - lastRealTime
                lastRealTime = now

                val scaledDelta = (realDelta * _playbackSpeed.value).toLong()
                val nextTime = _currentTimeMs.value + scaledDelta

                if (nextTime >= durationMs) {
                    if (_isLooping.value) {
                        _currentTimeMs.value = 0L
                    } else {
                        _currentTimeMs.value = durationMs
                        _isPlaying.value = false
                        break
                    }
                } else {
                    _currentTimeMs.value = nextTime
                }
            }
        }
    }

    fun release() {
        pause()
    }
}
