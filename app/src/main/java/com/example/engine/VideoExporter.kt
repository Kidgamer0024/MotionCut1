package com.example.engine

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import com.example.model.AspectRatio
import com.example.model.ClipItem
import com.example.model.ClipType
import com.example.model.EffectKernel
import com.example.model.ExportResolution
import com.example.model.FrameRate
import com.example.model.Model3DConfig
import com.example.model.Project
import com.example.model.RenderMode3D
import com.example.model.ShapeType
import com.example.model.TrackType
import com.example.model.VideoEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class ExportedVideoResult(
    val localFile: File,
    val shareUri: Uri,
    val galleryUri: Uri?,
    val displayLocation: String,
    val width: Int,
    val height: Int,
    val fileSizeBytes: Long,
    val totalFrames: Int
)

object VideoExporter {
    private const val TAG = "VideoExporter"
    private const val MIME_AVC = MediaFormat.MIMETYPE_VIDEO_AVC

    fun computeExportDimensions(
        aspectRatio: AspectRatio,
        resolution: ExportResolution
    ): Pair<Int, Int> {
        val ratio = aspectRatio.ratioFloat.coerceAtLeast(0.1f)
        val shortEdge = when (resolution) {
            ExportResolution.HD_720P -> 720
            ExportResolution.FHD_1080P -> 1080
            ExportResolution.UHD_4K -> 2160
        }
        val (rawW, rawH) = if (ratio >= 1f) {
            val h = shortEdge
            val w = (shortEdge * ratio).roundToInt()
            w to h
        } else {
            val w = shortEdge
            val h = (shortEdge / ratio).roundToInt()
            w to h
        }
        val evenW = (rawW / 2) * 2
        val evenH = (rawH / 2) * 2
        return evenW.coerceAtLeast(320) to evenH.coerceAtLeast(320)
    }

    fun sanitizeFileName(rawTitle: String): String {
        val cleaned = rawTitle.trim().replace(Regex("[^a-zA-Z0-9._-]+"), "_")
            .trim('_', '-', '.')
        return cleaned.ifBlank { "MotionCut_Export" }.take(64)
    }

    suspend fun exportProjectToMp4(
        context: Context,
        project: Project,
        resolution: ExportResolution,
        frameRate: FrameRate,
        onProgress: (progress: Float, currentFrame: Int, totalFrames: Int) -> Unit
    ): ExportedVideoResult = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val exportsDir = File(appContext.cacheDir, "exports").apply { mkdirs() }
        val safeName = sanitizeFileName(project.title)
        val fileName = "${safeName}_${System.currentTimeMillis()}.mp4"
        val outFile = File(exportsDir, fileName)

        val durationMs = project.durationMs.coerceAtLeast(1000L)
        val fps = frameRate.fps.coerceIn(15, 60)
        val totalFrames = max(1, ((durationMs / 1000f) * fps).roundToInt())

        val bitmapCache = preloadClipBitmaps(appContext, project)

        try {
            val (targetW, targetH) = computeExportDimensions(project.aspectRatio, resolution)
            val encodeDimensions = try {
                encodeFramesToFile(
                    context = appContext,
                    project = project,
                    bitmapCache = bitmapCache,
                    outFile = outFile,
                    width = targetW,
                    height = targetH,
                    fps = fps,
                    bitrateMbps = resolution.bitrateMbps,
                    totalFrames = totalFrames,
                    onProgress = onProgress
                )
                targetW to targetH
            } catch (primaryError: Exception) {
                if (resolution == ExportResolution.UHD_4K) {
                    Log.w(TAG, "4K encoder failed (${primaryError.message}), falling back to 1080p", primaryError)
                    outFile.delete()
                    val (fallbackW, fallbackH) = computeExportDimensions(project.aspectRatio, ExportResolution.FHD_1080P)
                    encodeFramesToFile(
                        context = appContext,
                        project = project,
                        bitmapCache = bitmapCache,
                        outFile = outFile,
                        width = fallbackW,
                        height = fallbackH,
                        fps = fps,
                        bitrateMbps = ExportResolution.FHD_1080P.bitrateMbps,
                        totalFrames = totalFrames,
                        onProgress = onProgress
                    )
                    fallbackW to fallbackH
                } else {
                    throw primaryError
                }
            }

            val galleryResult = saveToMediaStoreGallery(appContext, outFile, "${safeName}.mp4")
            val shareUri = galleryResult.first ?: FileProvider.getUriForFile(
                appContext,
                "${appContext.packageName}.fileprovider",
                outFile
            )

            ExportedVideoResult(
                localFile = outFile,
                shareUri = shareUri,
                galleryUri = galleryResult.first,
                displayLocation = galleryResult.second,
                width = encodeDimensions.first,
                height = encodeDimensions.second,
                fileSizeBytes = outFile.length(),
                totalFrames = totalFrames
            )
        } finally {
            bitmapCache.values.forEach { bmp ->
                if (!bmp.isRecycled) bmp.recycle()
            }
        }
    }

    private suspend fun encodeFramesToFile(
        context: Context,
        project: Project,
        bitmapCache: Map<String, Bitmap>,
        outFile: File,
        width: Int,
        height: Int,
        fps: Int,
        bitrateMbps: Float,
        totalFrames: Int,
        onProgress: (Float, Int, Int) -> Unit
    ) {
        val format = MediaFormat.createVideoFormat(MIME_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, (bitrateMbps * 1_000_000f).roundToInt().coerceAtLeast(1_500_000))
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        val codec = MediaCodec.createEncoderByType(MIME_AVC)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val inputSurface = codec.createInputSurface()
        codec.start()

        val muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var trackIndex = -1
        var muxerStarted = false
        val bufferInfo = MediaCodec.BufferInfo()
        val frameDurationUs = 1_000_000L / fps
        var lastPtsUs = -frameDurationUs
        var encodedFrameIndex = 0L

        val frameBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val frameCanvas = Canvas(frameBitmap)
        val surfaceBlitPaint = Paint(Paint.FILTER_BITMAP_FLAG)

        fun drainEncoder(endOfStream: Boolean) {
            if (endOfStream) {
                codec.signalEndOfInputStream()
            }
            val timeoutUs = if (endOfStream) 10_000L else 0L
            while (true) {
                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
                when {
                    outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        if (!endOfStream) break
                    }
                    outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (!muxerStarted) {
                            trackIndex = muxer.addTrack(codec.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                    }
                    outputIndex >= 0 -> {
                        val encodedData = codec.getOutputBuffer(outputIndex)
                        if (encodedData != null) {
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                bufferInfo.size = 0
                            }
                            if (bufferInfo.size > 0 && muxerStarted) {
                                encodedData.position(bufferInfo.offset)
                                encodedData.limit(bufferInfo.offset + bufferInfo.size)
                                val pts = encodedFrameIndex * frameDurationUs
                                bufferInfo.presentationTimeUs = if (pts <= lastPtsUs) lastPtsUs + frameDurationUs else pts
                                lastPtsUs = bufferInfo.presentationTimeUs
                                encodedFrameIndex++
                                muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                            }
                        }
                        codec.releaseOutputBuffer(outputIndex, false)
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            break
                        }
                    }
                }
            }
        }

        try {
            val renderContext = currentCoroutineContext()
            for (frameIdx in 0 until totalFrames) {
                renderContext.ensureActive()
                val timeMs = ((frameIdx.toLong() * 1000L) / fps).coerceAtMost(project.durationMs)
                renderProjectFrame(context, frameCanvas, project, bitmapCache, timeMs, width, height)

                val surfaceCanvas = inputSurface.lockCanvas(null)
                try {
                    surfaceCanvas.drawBitmap(frameBitmap, 0f, 0f, surfaceBlitPaint)
                } finally {
                    inputSurface.unlockCanvasAndPost(surfaceCanvas)
                }
                drainEncoder(endOfStream = false)

                val progress = (frameIdx + 1).toFloat() / totalFrames.toFloat()
                onProgress(progress, frameIdx + 1, totalFrames)
            }
            drainEncoder(endOfStream = true)
        } finally {
            if (!frameBitmap.isRecycled) frameBitmap.recycle()
            runCatching { codec.stop() }
            runCatching { codec.release() }
            runCatching { inputSurface.release() }
            if (muxerStarted) {
                runCatching { muxer.stop() }
            }
            runCatching { muxer.release() }
        }
    }

    private fun renderProjectFrame(
        context: Context,
        canvas: Canvas,
        project: Project,
        bitmapCache: Map<String, Bitmap>,
        timeMs: Long,
        canvasW: Int,
        canvasH: Int
    ) {
        canvas.drawColor(Color.parseColor("#0A0A0F"))

        val activeClips = project.tracks
            .filter { it.isVisible }
            .sortedBy { track ->
                when (track.type) {
                    TrackType.MAIN_VIDEO -> 0
                    TrackType.OVERLAY -> 1
                    TrackType.AUDIO -> 2
                    TrackType.TEXT_STICKER -> 3
                    TrackType.EFFECTS -> 4
                }
            }
            .flatMap { it.clips }
            .filter { clip -> timeMs in clip.startTimeMs..clip.endTimeMs }

        val scaleRef = min(canvasW, canvasH) / 420f

        for (clip in activeClips) {
            if (clip.type == ClipType.AUDIO || clip.type == ClipType.NULL_OBJECT) continue
            val transform = KeyframeEvaluator.evaluateClipTransform(clip, timeMs)
            val opacity = transform.opacity.coerceIn(0f, 1f)
            if (opacity <= 0.01f) continue

            var shakeX = 0f
            var shakeY = 0f
            for (fx in clip.effects) {
                if (fx.isEnabled && fx.effectiveKernel == EffectKernel.CAMERA_SHAKE) {
                    val phase = (timeMs / 1000f) * fx.speed + fx.paramB * 6.28f
                    val jitter = (4f + fx.paramA * 10f) * fx.intensity
                    shakeX += sin(phase * 7f) * jitter
                    shakeY += cos(phase * 9f) * jitter
                }
            }

            val cx = canvasW / 2f + (transform.positionX + shakeX) * scaleRef
            val cy = canvasH / 2f + (transform.positionY + shakeY) * scaleRef
            val sx = transform.scaleX
            val sy = transform.scaleY
            val rot = transform.rotation

            canvas.save()
            canvas.translate(cx, cy)
            canvas.rotate(rot)
            canvas.scale(sx, sy)

            val cm = ColorMatrix(ColorGradingCompositor.createColorMatrix(clip.colorGrading).values)
            val colorFilter = ColorMatrixColorFilter(cm)

            when (clip.type) {
                ClipType.VIDEO_SAMPLE -> {
                    val bmp = bitmapCache[clip.id]
                    if (bmp != null && !bmp.isRecycled) {
                        val bmpRatio = bmp.width.toFloat() / bmp.height.toFloat().coerceAtLeast(1f)
                        val canvasRatio = canvasW.toFloat() / canvasH.toFloat().coerceAtLeast(1f)
                        val drawW: Float
                        val drawH: Float
                        if (bmpRatio > canvasRatio) {
                            drawW = canvasW.toFloat()
                            drawH = drawW / bmpRatio
                        } else {
                            drawH = canvasH.toFloat()
                            drawW = drawH * bmpRatio
                        }
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                            alpha = (opacity * 255).roundToInt().coerceIn(0, 255)
                            this.colorFilter = colorFilter
                        }
                        val dst = RectF(-drawW / 2f, -drawH / 2f, drawW / 2f, drawH / 2f)
                        canvas.drawBitmap(bmp, null, dst, paint)
                    } else {
                        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = clip.colorHex.toInt()
                            alpha = (opacity * 255).roundToInt().coerceIn(0, 255)
                            this.colorFilter = colorFilter
                        }
                        canvas.drawRect(-canvasW / 2f, -canvasH / 2f, canvasW / 2f, canvasH / 2f, bgPaint)
                    }
                }
                ClipType.COLOR_BG -> {
                    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = clip.colorHex.toInt()
                        alpha = (opacity * 255).roundToInt().coerceIn(0, 255)
                        this.colorFilter = colorFilter
                    }
                    canvas.drawRect(-canvasW / 2f, -canvasH / 2f, canvasW / 2f, canvasH / 2f, bgPaint)
                }
                ClipType.SHAPE -> {
                    val cfg = clip.shapeConfig
                    if (cfg != null) {
                        drawShapeToCanvas(
                            canvas = canvas,
                            type = cfg.type,
                            shapeSize = min(canvasW, canvasH) * 0.34f,
                            fillColor = cfg.fillColor.toInt(),
                            strokeColor = cfg.strokeColor.toInt(),
                            strokeWidth = cfg.strokeWidth * scaleRef,
                            alpha = opacity
                        )
                    }
                }
                ClipType.TEXT -> {
                    val tc = clip.textConfig
                    if (tc != null && tc.text.isNotBlank()) {
                        val textSizePx = tc.fontSizeSp * scaleRef * 1.45f
                        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            style = Paint.Style.STROKE
                            strokeWidth = tc.strokeWidth * scaleRef * 2f
                            color = tc.strokeColor.toInt()
                            alpha = (opacity * 255).roundToInt().coerceIn(0, 255)
                            textSize = textSizePx
                            textAlign = Paint.Align.CENTER
                            isFakeBoldText = tc.isBold
                        }
                        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            style = Paint.Style.FILL
                            color = tc.textColor.toInt()
                            alpha = (opacity * 255).roundToInt().coerceIn(0, 255)
                            textSize = textSizePx
                            textAlign = Paint.Align.CENTER
                            isFakeBoldText = tc.isBold
                        }
                        val yOff = -(fillPaint.descent() + fillPaint.ascent()) / 2f
                        if (tc.strokeWidth > 0f) {
                            canvas.drawText(tc.text, 0f, yOff, strokePaint)
                        }
                        canvas.drawText(tc.text, 0f, yOff, fillPaint)
                    }
                }
                ClipType.MODEL_3D -> {
                    val eval3d = KeyframeEvaluator.evaluateModel3D(clip, timeMs) ?: clip.model3dConfig
                    if (eval3d != null) {
                        drawModel3DToCanvas(
                            context = context,
                            canvas = canvas,
                            config = eval3d,
                            viewportDim = min(canvasW, canvasH) * 0.55f,
                            opacity = opacity
                        )
                    }
                }
                ClipType.ADJUSTMENT, ClipType.AUDIO, ClipType.NULL_OBJECT -> {
                    // Adjustment layer effects render via drawEffectOverlaysToCanvas below
                }
            }

            if (clip.effects.isNotEmpty()) {
                drawEffectOverlaysToCanvas(
                    canvas = canvas,
                    effects = clip.effects,
                    canvasW = canvasW.toFloat(),
                    canvasH = canvasH.toFloat(),
                    scaleRef = scaleRef,
                    timeMs = timeMs
                )
            }

            canvas.restore()
        }
    }

    private fun drawEffectOverlaysToCanvas(
        canvas: Canvas,
        effects: List<VideoEffect>,
        canvasW: Float,
        canvasH: Float,
        scaleRef: Float,
        timeMs: Long
    ) {
        for (effect in effects) {
            if (!effect.isEnabled || effect.intensity <= 0.01f) continue
            val intensity = effect.intensity.coerceIn(0f, 1f)
            val timePhase = (timeMs / 1000f) * effect.speed + effect.paramB * 6.28f
            val detail = effect.paramA.coerceIn(0.05f, 1f)

            when (effect.effectiveKernel) {
                EffectKernel.SCANLINES -> {
                    val spacing = (14f - detail * 9f).coerceAtLeast(4f) * scaleRef
                    val offset = (timePhase * 12f) % spacing
                    val paint = Paint().apply {
                        color = Color.argb((intensity * 0.40f * 255).roundToInt(), 0, 0, 0)
                        strokeWidth = 2f * scaleRef
                    }
                    var ly = -canvasH / 2f + offset
                    while (ly < canvasH / 2f) {
                        canvas.drawLine(-canvasW / 2f, ly, canvasW / 2f, ly, paint)
                        ly += spacing
                    }
                }
                EffectKernel.CHROMATIC_ABERRATION, EffectKernel.PRISM_SHIFT -> {
                    val shift = intensity * (6f + detail * 14f) * scaleRef
                    val angle = timePhase * 0.5f
                    val dx = cos(angle) * shift
                    val dy = sin(angle) * shift * 0.35f
                    val pCyan = Paint().apply {
                        color = Color.argb((0.14f * intensity * 255).roundToInt(), 0, 229, 255)
                    }
                    val pPink = Paint().apply {
                        color = Color.argb((0.14f * intensity * 255).roundToInt(), 255, 46, 99)
                    }
                    canvas.drawRect(-canvasW / 2f - dx, -canvasH / 2f - dy, canvasW / 2f - dx, canvasH / 2f - dy, pCyan)
                    canvas.drawRect(-canvasW / 2f + dx, -canvasH / 2f + dy, canvasW / 2f + dx, canvasH / 2f + dy, pPink)
                }
                EffectKernel.GLOW -> {
                    val radius = (canvasW * (0.45f + detail * 0.40f)).coerceAtLeast(16f)
                    val pulse = 0.85f + 0.15f * sin(timePhase)
                    val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = RadialGradient(
                            0f, 0f, radius,
                            intArrayOf(
                                Color.argb((0.20f * intensity * pulse * 255).roundToInt().coerceIn(0, 255), 255, 255, 255),
                                Color.argb((0.14f * intensity * pulse * 255).roundToInt().coerceIn(0, 255), 0, 229, 255),
                                Color.TRANSPARENT
                            ),
                            floatArrayOf(0f, 0.5f, 1f),
                            Shader.TileMode.CLAMP
                        )
                    }
                    canvas.drawCircle(0f, 0f, radius, glowPaint)
                }
                EffectKernel.VIGNETTE -> {
                    val radius = (max(canvasW, canvasH) * (0.65f - detail * 0.18f)).coerceAtLeast(32f)
                    val vigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = RadialGradient(
                            0f, 0f, radius,
                            intArrayOf(
                                Color.TRANSPARENT,
                                Color.argb((0.65f * intensity * 255).roundToInt().coerceIn(0, 255), 0, 0, 0)
                            ),
                            floatArrayOf(0.45f, 1f),
                            Shader.TileMode.CLAMP
                        )
                    }
                    canvas.drawRect(-canvasW / 2f, -canvasH / 2f, canvasW / 2f, canvasH / 2f, vigPaint)
                }
                EffectKernel.COLOR_PULSE -> {
                    val hue = ((timePhase * 60f) % 360f).let { if (it < 0f) it + 360f else it }
                    val tintArgb = Color.HSVToColor(
                        (0.16f * intensity * 255).roundToInt().coerceIn(0, 255),
                        floatArrayOf(hue, 0.85f, 1f)
                    )
                    val p = Paint().apply { color = tintArgb }
                    canvas.drawRect(-canvasW / 2f, -canvasH / 2f, canvasW / 2f, canvasH / 2f, p)
                }
                EffectKernel.NEON_EDGES -> {
                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = (2f + detail * 3f) * scaleRef
                        color = Color.argb((0.45f * intensity * 255).roundToInt(), 155, 89, 255)
                    }
                    val rect = RectF(-canvasW * 0.44f, -canvasH * 0.44f, canvasW * 0.44f, canvasH * 0.44f)
                    canvas.drawRoundRect(rect, 12f * scaleRef, 12f * scaleRef, p)
                }
                EffectKernel.PARTICLE_SPARK -> {
                    val count = (12 + (detail * 18).toInt()).coerceAtMost(30)
                    val alphaInt = ((0.35f + 0.55f * intensity).coerceAtMost(0.9f) * 255).roundToInt()
                    val p = Paint(Paint.ANTI_ALIAS_FLAG)
                    for (idx in 0 until count) {
                        val px = sin(idx * 1.9f + timePhase * 0.8f) * canvasW * 0.42f
                        val py = cos(idx * 2.3f + timePhase * 0.6f) * canvasH * 0.42f
                        p.color = when {
                            idx % 3 == 0 -> Color.argb(alphaInt, 255, 179, 0)
                            idx % 2 == 0 -> Color.argb(alphaInt, 0, 229, 255)
                            else -> Color.argb(alphaInt, 255, 255, 255)
                        }
                        canvas.drawCircle(px, py, (1.8f + (idx % 3)) * scaleRef, p)
                    }
                }
                else -> {
                    // Subtle animated highlight wash for remaining kernels
                    val wash = Paint().apply {
                        color = Color.argb((0.06f * intensity * 255).roundToInt(), 0, 229, 255)
                    }
                    canvas.drawRect(-canvasW / 2f, -canvasH / 2f, canvasW / 2f, canvasH / 2f, wash)
                }
            }
        }
    }

    private fun drawShapeToCanvas(
        canvas: Canvas,
        type: ShapeType,
        shapeSize: Float,
        fillColor: Int,
        strokeColor: Int,
        strokeWidth: Float,
        alpha: Float
    ) {
        val half = shapeSize / 2f
        val alphaInt = (alpha * 255).roundToInt().coerceIn(0, 255)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = fillColor
            this.alpha = alphaInt
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = strokeColor
            this.strokeWidth = strokeWidth
            this.alpha = alphaInt
        }

        when (type) {
            ShapeType.CIRCLE -> {
                canvas.drawCircle(0f, 0f, half, fillPaint)
                if (strokeWidth > 0f) canvas.drawCircle(0f, 0f, half, strokePaint)
            }
            ShapeType.SQUARE -> {
                canvas.drawRect(-half, -half, half, half, fillPaint)
                if (strokeWidth > 0f) canvas.drawRect(-half, -half, half, half, strokePaint)
            }
            ShapeType.ROUNDED -> {
                val rect = RectF(-half, -half, half, half)
                val r = shapeSize * 0.24f
                canvas.drawRoundRect(rect, r, r, fillPaint)
                if (strokeWidth > 0f) canvas.drawRoundRect(rect, r, r, strokePaint)
            }
            ShapeType.RECTANGLE -> {
                val rw = shapeSize * 0.59f
                val rh = shapeSize * 0.36f
                val rect = RectF(-rw, -rh, rw, rh)
                val r = rh * 0.2f
                canvas.drawRoundRect(rect, r, r, fillPaint)
                if (strokeWidth > 0f) canvas.drawRoundRect(rect, r, r, strokePaint)
            }
            else -> {
                val sides = when (type) {
                    ShapeType.TRIANGLE -> 3
                    ShapeType.PENTAGON -> 5
                    ShapeType.HEXAGON, ShapeType.POLYGON -> 6
                    ShapeType.OCTAGON -> 8
                    ShapeType.STAR -> 10
                    ShapeType.STAR_6 -> 12
                    ShapeType.DIAMOND, ShapeType.QUAD -> 4
                    else -> 6
                }
                val path = Path()
                for (i in 0 until sides) {
                    val angle = -PI / 2.0 + (2.0 * PI * i / sides)
                    val radius = if ((type == ShapeType.STAR || type == ShapeType.STAR_6) && i % 2 == 1) half * 0.45f else half
                    val px = (radius * cos(angle)).toFloat()
                    val py = (radius * sin(angle)).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                canvas.drawPath(path, fillPaint)
                if (strokeWidth > 0f) canvas.drawPath(path, strokePaint)
            }
        }
    }

    private fun drawModel3DToCanvas(
        context: Context,
        canvas: Canvas,
        config: Model3DConfig,
        viewportDim: Float,
        opacity: Float
    ) {
        val mesh = Mesh3DParser.getOrBuildMesh(context, config, config.extrusionDepth)
        if (mesh.vertices.isEmpty() || mesh.faces.isEmpty()) return

        val rx = Math.toRadians(config.rotationX.toDouble())
        val ry = Math.toRadians(config.rotationY.toDouble())
        val rz = Math.toRadians(config.rotationZ.toDouble())
        val cx = cos(rx).toFloat(); val sx = sin(rx).toFloat()
        val cy = cos(ry).toFloat(); val sy = sin(ry).toFloat()
        val cz = cos(rz).toFloat(); val sz = sin(rz).toFloat()

        val lAz = Math.toRadians(config.lightAzimuthDeg.toDouble())
        val lEl = Math.toRadians(config.lightElevationDeg.toDouble())
        val lx = (cos(lEl) * sin(lAz)).toFloat()
        val ly = sin(lEl).toFloat()
        val lz = (cos(lEl) * cos(lAz)).toFloat()

        val fov = config.fovDegrees.coerceIn(25f, 110f)
        val camDist = 3.2f / kotlin.math.tan(Math.toRadians((fov / 2.0))).toFloat().coerceAtLeast(0.25f)
        val scaleProj = viewportDim * 0.36f * config.modelScale

        val transformed = Array(mesh.vertices.size) { idx ->
            val v = mesh.vertices[idx]
            val vx = v.x
            val vy = v.y
            val vz = v.z
            val y1 = vy * cx - vz * sx
            val z1 = vy * sx + vz * cx
            val x2 = vx * cy + z1 * sy
            val z2 = -vx * sy + z1 * cy
            val x3 = x2 * cz - y1 * sz
            val y3 = x2 * sz + y1 * cz
            floatArrayOf(x3, y3, z2 + config.depthZ)
        }

        val sortedFaces = mesh.faces.mapNotNull { face ->
            if (face.i0 !in transformed.indices || face.i1 !in transformed.indices || face.i2 !in transformed.indices) {
                null
            } else {
                val p0 = transformed[face.i0]
                val p1 = transformed[face.i1]
                val p2 = transformed[face.i2]
                val avgZ = (p0[2] + p1[2] + p2[2]) / 3f
                Triple(face, avgZ, arrayOf(p0, p1, p2))
            }
        }.sortedBy { it.second }

        val baseColor = config.materialColorHex.toInt()
        val baseR = Color.red(baseColor)
        val baseG = Color.green(baseColor)
        val baseB = Color.blue(baseColor)
        val wirePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.WHITE
            strokeWidth = (viewportDim * 0.006f).coerceAtLeast(1.2f)
            alpha = ((opacity * 0.45f) * 255).roundToInt().coerceIn(0, 255)
        }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        val wireframeOnly = config.renderMode == RenderMode3D.WIREFRAME
        val isFlat = config.renderMode == RenderMode3D.SOLID

        for ((face, _, tri) in sortedFaces) {
            val p0 = tri[0]; val p1 = tri[1]; val p2 = tri[2]
            val ux = p1[0] - p0[0]; val uy = p1[1] - p0[1]; val uz = p1[2] - p0[2]
            val vx = p2[0] - p0[0]; val vy = p2[1] - p0[1]; val vz = p2[2] - p0[2]
            val nx = uy * vz - uz * vy
            val ny = uz * vx - ux * vz
            val nz = ux * vy - uy * vx
            val nLen = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-5f)
            val ndl = ((nx / nLen) * lx + (ny / nLen) * ly + (nz / nLen) * lz).coerceIn(-1f, 1f)
            val light = if (isFlat) {
                0.85f * face.tintFactor
            } else {
                (0.35f + max(0f, ndl) * 0.65f * (0.85f + config.metallic * 0.3f)).coerceIn(0.25f, 1.15f) * face.tintFactor
            }

            fun proj(p: FloatArray): Pair<Float, Float> {
                val denom = (camDist - p[2]).coerceAtLeast(0.4f)
                val persp = camDist / denom
                return (p[0] * scaleProj * persp) to (-p[1] * scaleProj * persp)
            }
            val (x0, y0) = proj(p0)
            val (x1, y1) = proj(p1)
            val (x2, y2) = proj(p2)

            val path = Path().apply {
                moveTo(x0, y0)
                lineTo(x1, y1)
                lineTo(x2, y2)
                close()
            }
            if (!wireframeOnly) {
                fillPaint.color = Color.argb(
                    (opacity * 255).roundToInt().coerceIn(0, 255),
                    (baseR * light).roundToInt().coerceIn(0, 255),
                    (baseG * light).roundToInt().coerceIn(0, 255),
                    (baseB * light).roundToInt().coerceIn(0, 255)
                )
                canvas.drawPath(path, fillPaint)
            }
            if (wireframeOnly || config.showEdges) {
                canvas.drawPath(path, wirePaint)
            }
        }
    }

    private fun preloadClipBitmaps(context: Context, project: Project): Map<String, Bitmap> {
        val result = mutableMapOf<String, Bitmap>()
        for (track in project.tracks) {
            for (clip in track.clips) {
                if (clip.type != ClipType.VIDEO_SAMPLE) continue
                val bmp = decodeClipBitmap(context, clip)
                if (bmp != null) {
                    result[clip.id] = bmp
                }
            }
        }
        return result
    }

    private fun decodeClipBitmap(context: Context, clip: ClipItem): Bitmap? {
        clip.assetResId?.let { resId ->
            runCatching {
                val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                return BitmapFactory.decodeResource(context.resources, resId, opts)
            }
        }
        val uriStr = clip.mediaUri ?: return null
        val uri = runCatching { Uri.parse(uriStr) }.getOrNull() ?: return null

        runCatching {
            val stream = if (uri.scheme == "file") {
                FileInputStream(File(uri.path.orEmpty()))
            } else {
                context.contentResolver.openInputStream(uri)
            }
            stream?.use {
                val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                BitmapFactory.decodeStream(it, null, opts)
            }?.let { return it }
        }

        val retriever = MediaMetadataRetriever()
        return try {
            if (uri.scheme == "file" && uri.path != null) {
                retriever.setDataSource(uri.path)
            } else {
                retriever.setDataSource(context, uri)
            }
            retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun saveToMediaStoreGallery(
        context: Context,
        sourceFile: File,
        displayName: String
    ): Pair<Uri?, String> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/MotionCut")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = runCatching {
                resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            }.getOrNull()
            if (uri != null) {
                return try {
                    resolver.openOutputStream(uri)?.use { out ->
                        sourceFile.inputStream().use { inp -> inp.copyTo(out) }
                    }
                    values.clear()
                    values.put(MediaStore.Video.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    uri to "Movies/MotionCut/$displayName"
                } catch (e: Exception) {
                    Log.w(TAG, "Failed writing video to MediaStore", e)
                    runCatching { resolver.delete(uri, null, null) }
                    null to sourceFile.absolutePath
                }
            }
        }
        return null to sourceFile.absolutePath
    }
}
