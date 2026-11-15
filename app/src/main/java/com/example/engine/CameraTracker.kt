package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.example.data.DeviceMediaScanner
import com.example.model.CameraTrackPoint
import com.example.model.ClipItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class CameraTrackingResult(
    val points: List<CameraTrackPoint>,
    val sampledFrames: Int,
    val isOpticalVideoTrack: Boolean,
    val summary: String
)

/**
 * 2D Planar Camera Motion Tracker using SAD (Sum of Absolute Differences) luminance
 * patch block-matching across real video frames extracted via [MediaMetadataRetriever].
 */
object CameraTracker {

    private const val GRID_SIZE = 96
    private const val PATCH_HALF = 7
    private const val SEARCH_RADIUS = 10

    suspend fun trackClipMotion(
        context: Context,
        clip: ClipItem,
        roiNormX: Float = 0.5f,
        roiNormY: Float = 0.5f,
        sampleCount: Int = 18
    ): CameraTrackingResult = withContext(Dispatchers.IO) {
        val clampedX = roiNormX.coerceIn(0.15f, 0.85f)
        val clampedY = roiNormY.coerceIn(0.15f, 0.85f)
        val uriStr = clip.mediaUri
        val isVideo = uriStr != null && DeviceMediaScanner.isVideoUri(context, uriStr, true)

        if (!isVideo || uriStr == null) {
            return@withContext CameraTrackingResult(
                points = emptyList(),
                sampledFrames = 0,
                isOpticalVideoTrack = false,
                summary = "Optical camera tracking requires a multi-frame video clip on the timeline."
            )
        }

        val retriever = MediaMetadataRetriever()
        val lumaFrames = mutableListOf<Pair<Long, IntArray>>()
        try {
            val uri = Uri.parse(uriStr)
            if (uri.scheme == "file" && uri.path != null) {
                retriever.setDataSource(uri.path)
            } else {
                retriever.setDataSource(context, uri)
            }
            val mediaDurationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()?.coerceAtLeast(500L) ?: clip.durationMs
            val steps = sampleCount.coerceIn(6, 28)
            for (i in 0 until steps) {
                val frac = if (steps <= 1) 0f else i.toFloat() / (steps - 1).toFloat()
                val timelineRelMs = (frac * clip.durationMs).toLong()
                val mappedSourceMs = KeyframeEvaluator.mapTimelineToSourceTimeMs(
                    clip,
                    clip.startTimeMs + timelineRelMs
                ).coerceIn(0L, mediaDurationMs - 16L)
                val frame = retriever.getFrameAtTime(mappedSourceMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST)
                if (frame != null) {
                    val luma = toLumaGrid(frame, GRID_SIZE, GRID_SIZE)
                    frame.recycle()
                    lumaFrames.add(timelineRelMs to luma)
                }
            }
        } catch (e: Exception) {
            return@withContext CameraTrackingResult(
                points = emptyList(),
                sampledFrames = 0,
                isOpticalVideoTrack = false,
                summary = "Unable to decode video frames for tracking: ${e.message ?: "Unsupported source"}"
            )
        } finally {
            runCatching { retriever.release() }
        }

        if (lumaFrames.size < 2) {
            return@withContext CameraTrackingResult(
                points = emptyList(),
                sampledFrames = lumaFrames.size,
                isOpticalVideoTrack = false,
                summary = "Decoded fewer than 2 video frames. Try a longer video clip."
            )
        }

        val trackPoints = computeOpticalTrackPoints(
            lumaFrames = lumaFrames,
            gridW = GRID_SIZE,
            gridH = GRID_SIZE,
            roiNormX = clampedX,
            roiNormY = clampedY
        )

        CameraTrackingResult(
            points = trackPoints,
            sampledFrames = lumaFrames.size,
            isOpticalVideoTrack = true,
            summary = "Tracked ${lumaFrames.size} frames around (${(clampedX * 100).roundToInt()}%, ${(clampedY * 100).roundToInt()}%)."
        )
    }

    internal fun computeOpticalTrackPoints(
        lumaFrames: List<Pair<Long, IntArray>>,
        gridW: Int,
        gridH: Int,
        roiNormX: Float,
        roiNormY: Float
    ): List<CameraTrackPoint> {
        if (lumaFrames.isEmpty()) return emptyList()
        var curX = (roiNormX * gridW).roundToInt().coerceIn(PATCH_HALF + 2, gridW - PATCH_HALF - 3)
        var curY = (roiNormY * gridH).roundToInt().coerceIn(PATCH_HALF + 2, gridH - PATCH_HALF - 3)
        val anchorX = curX
        val anchorY = curY
        val baseSpread = computeGradientSpread(lumaFrames.first().second, gridW, gridH, curX, curY)
            .coerceAtLeast(1f)

        val result = ArrayList<CameraTrackPoint>(lumaFrames.size)
        val patchArea = (PATCH_HALF * 2 + 1) * (PATCH_HALF * 2 + 1)
        val pxPerGridCellX = 320f / gridW.toFloat()
        val pxPerGridCellY = 320f / gridH.toFloat()

        result.add(
            CameraTrackPoint(
                timeMs = lumaFrames.first().first,
                x = 0f,
                y = 0f,
                scale = 1f,
                rotation = 0f,
                confidence = 1f
            )
        )

        for (i in 1 until lumaFrames.size) {
            val prevLuma = lumaFrames[i - 1].second
            val currLuma = lumaFrames[i].second
            val timeMs = lumaFrames[i].first

            var bestDx = 0
            var bestDy = 0
            var bestSad = Long.MAX_VALUE
            var secondSad = Long.MAX_VALUE

            for (dy in -SEARCH_RADIUS..SEARCH_RADIUS) {
                val candY = curY + dy
                if (candY - PATCH_HALF < 0 || candY + PATCH_HALF >= gridH) continue
                for (dx in -SEARCH_RADIUS..SEARCH_RADIUS) {
                    val candX = curX + dx
                    if (candX - PATCH_HALF < 0 || candX + PATCH_HALF >= gridW) continue

                    var sad = 0L
                    for (py in -PATCH_HALF..PATCH_HALF) {
                        val prevRow = (curY + py) * gridW
                        val currRow = (candY + py) * gridW
                        for (px in -PATCH_HALF..PATCH_HALF) {
                            sad += abs(prevLuma[prevRow + curX + px] - currLuma[currRow + candX + px])
                        }
                    }
                    // Tiny smoothness tie-breaker favoring zero displacement when region is flat
                    val penalized = sad + (abs(dx) + abs(dy)) * 3L
                    if (penalized < bestSad) {
                        secondSad = bestSad
                        bestSad = penalized
                        bestDx = dx
                        bestDy = dy
                    } else if (penalized < secondSad) {
                        secondSad = penalized
                    }
                }
            }

            curX = (curX + bestDx).coerceIn(PATCH_HALF + 1, gridW - PATCH_HALF - 2)
            curY = (curY + bestDy).coerceIn(PATCH_HALF + 1, gridH - PATCH_HALF - 2)

            val spread = computeGradientSpread(currLuma, gridW, gridH, curX, curY)
            val estScale = (spread / baseSpread).coerceIn(0.75f, 1.35f)
            val meanErr = bestSad.toFloat() / patchArea.toFloat()
            val confidence = (1f - (meanErr / 80f)).coerceIn(0.25f, 0.99f)

            result.add(
                CameraTrackPoint(
                    timeMs = timeMs,
                    x = (curX - anchorX) * pxPerGridCellX,
                    y = (curY - anchorY) * pxPerGridCellY,
                    scale = 1f + (estScale - 1f) * 0.35f,
                    rotation = 0f,
                    confidence = confidence
                )
            )
        }

        // Smooth jitter with a 3-point moving average on interior points
        if (result.size < 3) return result
        return result.mapIndexed { idx, pt ->
            if (idx == 0 || idx == result.lastIndex) pt
            else {
                val p0 = result[idx - 1]
                val p2 = result[idx + 1]
                pt.copy(
                    x = (p0.x + pt.x * 2f + p2.x) / 4f,
                    y = (p0.y + pt.y * 2f + p2.y) / 4f,
                    scale = (p0.scale + pt.scale * 2f + p2.scale) / 4f
                )
            }
        }
    }

    private fun computeGradientSpread(luma: IntArray, w: Int, h: Int, cx: Int, cy: Int): Float {
        var sum = 0.0
        val r = PATCH_HALF
        for (py in -r until r) {
            val y = (cy + py).coerceIn(1, h - 2)
            val yNext = (y + 1).coerceIn(1, h - 2)
            for (px in -r until r) {
                val x = (cx + px).coerceIn(1, w - 2)
                val gx = luma[y * w + (x + 1)] - luma[y * w + x]
                val gy = luma[yNext * w + x] - luma[y * w + x]
                sum += sqrt((gx * gx + gy * gy).toDouble())
            }
        }
        return sum.toFloat()
    }

    internal fun toLumaGrid(bitmap: Bitmap, w: Int, h: Int): IntArray {
        val scaled = Bitmap.createScaledBitmap(bitmap, w, h, true)
        val pixels = IntArray(w * h)
        scaled.getPixels(pixels, 0, w, 0, 0, w, h)
        if (scaled !== bitmap) scaled.recycle()
        return IntArray(w * h) { i ->
            val c = pixels[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            (r * 77 + g * 150 + b * 29) shr 8
        }
    }
}
