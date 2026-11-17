package com.example.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.model.ClipItem
import com.example.model.ClipType
import com.example.model.Project
import com.example.model.TrackType
import java.io.File
import kotlin.math.abs

private const val AUDIO_PLAYER_TAG = "TimelineAudioPlayer"

/**
 * Synchronizes active timeline audio / voice-over clips with the editor's playhead.
 */
class TimelineAudioPlayer(context: Context) {

    private val appContext = context.applicationContext ?: context

    private data class PlayerEntry(
        val mediaUri: String,
        val player: MediaPlayer,
        val durationMs: Long,
        var lastSpeed: Float,
        var lastPreservePitch: Boolean,
        var lastVolume: Float,
        var completed: Boolean = false
    )

    private val activePlayers = mutableMapOf<String, PlayerEntry>()

    fun sync(
        project: Project,
        isPlaying: Boolean,
        currentTimeMs: Long,
        globalSpeed: Float = 1.0f
    ) {
        if (!isPlaying) {
            if (activePlayers.isNotEmpty()) {
                releaseAll()
            }
            return
        }

        val activeClips = project.tracks
            .filter { it.type == TrackType.AUDIO && !it.isMuted }
            .flatMap { it.clips }
            .filter { clip ->
                clip.type == ClipType.AUDIO &&
                    clip.volume > 0.001f &&
                    !clip.mediaUri.isNullOrBlank() &&
                    currentTimeMs in clip.startTimeMs until clip.endTimeMs
            }

        val activeMap = activeClips.associateBy { it.id }

        // Release any clips no longer under the playhead or whose URI changed
        val staleIds = activePlayers.keys.filter { id ->
            val currentClip = activeMap[id]
            currentClip == null || currentClip.mediaUri != activePlayers[id]?.mediaUri
        }
        staleIds.forEach { id ->
            releaseEntry(activePlayers.remove(id))
        }

        for (clip in activeClips) {
            val uriString = clip.mediaUri ?: continue
            val desiredSourceMs = KeyframeEvaluator.mapTimelineToSourceTimeMs(clip, currentTimeMs)
            val targetSpeed = (clip.speed * globalSpeed).coerceIn(0.25f, 4.0f)
            val targetVolume = clip.volume.coerceIn(0f, 1f)

            val existing = activePlayers[clip.id]
            if (existing == null) {
                val created = createPreparedPlayer(
                    uriString = uriString,
                    initialSeekMs = desiredSourceMs,
                    speed = targetSpeed,
                    preservePitch = clip.preserveAudioPitch,
                    volume = targetVolume
                )
                if (created != null) {
                    activePlayers[clip.id] = created
                }
            } else {
                updateExistingPlayer(
                    entry = existing,
                    desiredSourceMs = desiredSourceMs,
                    speed = targetSpeed,
                    preservePitch = clip.preserveAudioPitch,
                    volume = targetVolume
                )
            }
        }
    }

    private fun createPreparedPlayer(
        uriString: String,
        initialSeekMs: Long,
        speed: Float,
        preservePitch: Boolean,
        volume: Float
    ): PlayerEntry? {
        val player = MediaPlayer()
        return try {
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            val parsed = Uri.parse(uriString)
            val directPath = when {
                parsed.scheme == "file" && !parsed.path.isNullOrBlank() -> parsed.path
                parsed.scheme == null && uriString.startsWith("/") -> uriString
                else -> null
            }
            if (directPath != null) {
                val file = File(directPath)
                if (!file.exists() || file.length() <= 44L) {
                    player.release()
                    return null
                }
                player.setDataSource(file.absolutePath)
            } else {
                player.setDataSource(appContext, parsed)
            }
            player.prepare()
            val mediaDuration = player.duration.takeIf { it > 0 }?.toLong() ?: 1000L
            val clampedSeek = initialSeekMs.coerceIn(0L, (mediaDuration - 30L).coerceAtLeast(0L))
            if (clampedSeek > 0L) {
                player.seekTo(clampedSeek.toInt())
            }
            player.setVolume(volume, volume)
            applySpeedParams(player, speed, preservePitch)

            lateinit var entry: PlayerEntry
            entry = PlayerEntry(
                mediaUri = uriString,
                player = player,
                durationMs = mediaDuration,
                lastSpeed = speed,
                lastPreservePitch = preservePitch,
                lastVolume = volume
            )
            player.setOnCompletionListener {
                entry.completed = true
            }
            player.start()
            entry
        } catch (e: Exception) {
            Log.w(AUDIO_PLAYER_TAG, "Unable to play audio clip $uriString: ${e.message}")
            runCatching { player.release() }
            null
        }
    }

    private fun updateExistingPlayer(
        entry: PlayerEntry,
        desiredSourceMs: Long,
        speed: Float,
        preservePitch: Boolean,
        volume: Float
    ) {
        runCatching {
            if (abs(entry.lastVolume - volume) > 0.01f) {
                entry.player.setVolume(volume, volume)
                entry.lastVolume = volume
            }
            if (abs(entry.lastSpeed - speed) > 0.02f || entry.lastPreservePitch != preservePitch) {
                applySpeedParams(entry.player, speed, preservePitch)
                entry.lastSpeed = speed
                entry.lastPreservePitch = preservePitch
            }

            if (desiredSourceMs >= entry.durationMs) return

            val currentPos = entry.player.currentPosition.toLong()
            val drift = abs(currentPos - desiredSourceMs)
            if (drift > 380L) {
                entry.completed = false
                entry.player.seekTo(desiredSourceMs.toInt())
                if (!entry.player.isPlaying) {
                    entry.player.start()
                }
            } else if (!entry.completed && !entry.player.isPlaying) {
                entry.player.start()
            }
        }
    }

    private fun applySpeedParams(player: MediaPlayer, speed: Float, preservePitch: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching {
                player.playbackParams = PlaybackParams().apply {
                    this.speed = speed
                    this.pitch = if (preservePitch) 1.0f else speed
                }
            }
        }
    }

    private fun releaseEntry(entry: PlayerEntry?) {
        if (entry == null) return
        runCatching {
            if (entry.player.isPlaying) {
                entry.player.stop()
            }
        }
        runCatching { entry.player.release() }
    }

    fun releaseAll() {
        activePlayers.values.forEach { releaseEntry(it) }
        activePlayers.clear()
    }
}
