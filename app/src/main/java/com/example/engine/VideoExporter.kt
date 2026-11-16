package com.example.engine

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.LightingColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
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
import androidx.compose.ui.graphics.asAndroidPath
import androidx.core.content.FileProvider
import com.example.data.DeviceMediaScanner
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
import com.example.model.Vec3
import com.example.model.VideoEffect
import com.example.ui.components.TextStyleRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
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
        val retrieverCache = preloadVideoRetrievers(appContext, project)

        try {
            val (targetW, targetH) = computeExportDimensions(project.aspectRatio, resolution)
            val encodeDimensions = try {
                encodeFramesToFile(
                    context = appContext,
                    project = project,
                    bitmapCache = bitmapCache,
                    retrieverCache = retrieverCache,
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
                        retrieverCache = retrieverCache,
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
            retrieverCache.values.forEach { ret ->
                runCatching { ret.release() }
            }
        }
    }

    private suspend fun encodeFramesToFile(
        context: Context,
        project: Project,
        bitmapCache: Map<String, Bitmap>,
        retrieverCache: Map<String, MediaMetadataRetriever>,
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
                renderProjectFrame(context, frameCanvas, project, bitmapCache, retrieverCache, timeMs, width, height)

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
        retrieverCache: Map<String, MediaMetadataRetriever>,
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

            val enabledMask = clip.masks.firstOrNull { it.isEnabled }
            val evaluatedMask = enabledMask?.let { mask ->
                MaskCompositor.evaluateMask(
                    mask = mask,
                    currentTimeMs = timeMs,
                    clipStartTimeMs = clip.startTimeMs,
                    clipWidth = canvasW.toFloat(),
                    clipHeight = canvasH.toFloat()
                )
            }
            if (evaluatedMask != null) {
                canvas.save()
                canvas.translate(evaluatedMask.positionX * scaleRef, evaluatedMask.positionY * scaleRef)
                canvas.rotate(evaluatedMask.rotation)
                canvas.clipPath(evaluatedMask.path.asAndroidPath())
                canvas.rotate(-evaluatedMask.rotation)
                canvas.translate(-evaluatedMask.positionX * scaleRef, -evaluatedMask.positionY * scaleRef)
            }

            when (clip.type) {
                ClipType.VIDEO_SAMPLE -> {
                    val videoRet = retrieverCache[clip.id]
                    val dynamicFrame = if (videoRet != null) {
                        val srcTimeMs = KeyframeEvaluator.mapTimelineToSourceTimeMs(clip, timeMs)
                        runCatching {
                            videoRet.getFrameAtTime(srcTimeMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        }.getOrNull()
                    } else null
                    val bmp = dynamicFrame ?: bitmapCache[clip.id]
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
                        if (dynamicFrame != null && dynamicFrame !== bitmapCache[clip.id]) {
                            dynamicFrame.recycle()
                        }
                    } else if (clip.mediaUri == null && clip.assetResId == null) {
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
                        val anim = TextStyleRegistry.evaluateAnimatedText(
                            config = tc,
                            relTimeMs = (timeMs - clip.startTimeMs).coerceAtLeast(0L),
                            durationMs = clip.durationMs
                        )
                        val effOpacity = (opacity * anim.alphaMultiplier).coerceIn(0f, 1f)
                        val alphaInt = (effOpacity * 255).roundToInt().coerceIn(0, 255)
                        val textSizePx = tc.fontSizeSp * scaleRef * 1.45f * anim.scaleMultiplier
                        val tf = TextStyleRegistry.androidTypeface(context, tc.fontChoice, tc.isBold)

                        canvas.save()
                        canvas.translate(anim.offsetX * scaleRef + anim.glitchXShift * scaleRef, anim.offsetY * scaleRef)

                        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            style = Paint.Style.FILL
                            color = tc.textColor.toInt()
                            alpha = alphaInt
                            textSize = textSizePx
                            textAlign = Paint.Align.CENTER
                            typeface = tf
                            letterSpacing = (tc.letterSpacing + anim.letterSpacingDelta) * 0.04f
                            if (tc.shadowEnabled) {
                                setShadowLayer(6f * scaleRef, 2f * scaleRef, 3f * scaleRef, Color.argb((effOpacity * 180).roundToInt(), 0, 0, 0))
                            }
                        }

                        if (tc.hasBackgroundPill && anim.visibleText.isNotEmpty()) {
                            val tw = fillPaint.measureText(anim.visibleText)
                            val padX = 18f * scaleRef
                            val padY = 10f * scaleRef
                            val boxH = textSizePx + padY * 2f
                            val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = tc.backgroundColor.toInt()
                                alpha = (effOpacity * Color.alpha(tc.backgroundColor.toInt())).roundToInt().coerceIn(0, 255)
                            }
                            val rect = RectF(-tw / 2f - padX, -boxH / 2f, tw / 2f + padX, boxH / 2f)
                            canvas.drawRoundRect(rect, tc.backgroundCornerRadius * scaleRef, tc.backgroundCornerRadius * scaleRef, pillPaint)
                        }

                        val strokePaint = Paint(fillPaint).apply {
                            style = Paint.Style.STROKE
                            strokeWidth = tc.strokeWidth * scaleRef * 2f
                            color = tc.strokeColor.toInt()
                            clearShadowLayer()
                        }
                        val yOff = -(fillPaint.descent() + fillPaint.ascent()) / 2f
                        if (tc.strokeWidth > 0f) {
                            canvas.drawText(anim.visibleText, 0f, yOff, strokePaint)
                        }
                        canvas.drawText(anim.visibleText, 0f, yOff, fillPaint)
                        canvas.restore()
                    }
                }
                ClipType.MODEL_3D -> {
                    val eval3d = KeyframeEvaluator.evaluateModel3D(clip, timeMs) ?: clip.model3dConfig
                    if (eval3d != null) {
                        val texKey = "tex3d_${clip.id}"
                        drawModel3DToCanvas(
                            context = context,
                            canvas = canvas,
                            config = eval3d,
                            viewportDim = min(canvasW, canvasH) * 0.55f,
                            opacity = opacity,
                            textureBitmap = bitmapCache[texKey]
                        )
                    }
                }
                ClipType.ADJUSTMENT, ClipType.AUDIO, ClipType.NULL_OBJECT -> {
                    // Adjustment layer effects render via drawEffectOverlaysToCanvas below
                }
            }

            if (evaluatedMask != null) {
                canvas.restore()
                if (evaluatedMask.borderWidthPx > 0.2f) {
                    val bPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = evaluatedMask.borderWidthPx * scaleRef
                        color = evaluatedMask.borderColorHex.toInt()
                    }
                    canvas.save()
                    canvas.translate(evaluatedMask.positionX * scaleRef, evaluatedMask.positionY * scaleRef)
                    canvas.rotate(evaluatedMask.rotation)
                    canvas.drawPath(evaluatedMask.path.asAndroidPath(), bPaint)
                    canvas.restore()
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
            val fxInt = effect.colorHex.toInt()
            val fxR = Color.red(fxInt)
            val fxG = Color.green(fxInt)
            val fxB = Color.blue(fxInt)

            when (effect.effectiveKernel) {
                EffectKernel.SCANLINES -> {
                    val spacing = (14f - detail * 9f).coerceAtLeast(4f) * scaleRef
                    val offset = (timePhase * 12f) % spacing
                    val paint = Paint().apply {
                        color = Color.argb((intensity * 0.38f * 255).roundToInt(), fxR, fxG, fxB)
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
                    val pPrimary = Paint().apply {
                        color = Color.argb((0.14f * intensity * 255).roundToInt(), fxR, fxG, fxB)
                    }
                    val pPink = Paint().apply {
                        color = Color.argb((0.14f * intensity * 255).roundToInt(), 255, 46, 99)
                    }
                    canvas.drawRect(-canvasW / 2f - dx, -canvasH / 2f - dy, canvasW / 2f - dx, canvasH / 2f - dy, pPrimary)
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
                                Color.argb((0.16f * intensity * pulse * 255).roundToInt().coerceIn(0, 255), fxR, fxG, fxB),
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
                                Color.argb((0.65f * intensity * 255).roundToInt().coerceIn(0, 255), fxR, fxG, fxB)
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
                    val p = Paint().apply {
                        shader = LinearGradient(
                            0f, -canvasH / 2f, 0f, canvasH / 2f,
                            Color.argb((0.18f * intensity * 255).roundToInt().coerceIn(0, 255), fxR, fxG, fxB),
                            tintArgb,
                            Shader.TileMode.CLAMP
                        )
                    }
                    canvas.drawRect(-canvasW / 2f, -canvasH / 2f, canvasW / 2f, canvasH / 2f, p)
                }
                EffectKernel.NEON_EDGES -> {
                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = (2f + detail * 3f) * scaleRef
                        color = Color.argb((0.50f * intensity * 255).roundToInt(), fxR, fxG, fxB)
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
                        p.color = if (idx % 2 == 0) Color.argb(alphaInt, fxR, fxG, fxB) else Color.argb(alphaInt, 255, 255, 255)
                        canvas.drawCircle(px, py, (1.8f + (idx % 3)) * scaleRef, p)
                    }
                }
                EffectKernel.LIGHT_SWEEP -> {
                    val sweepProgress = ((timePhase * 0.45f) % 1.4f) - 0.2f
                    val sweepX = -canvasW / 2f + sweepProgress * canvasW
                    val barW = canvasW * (0.14f + detail * 0.22f)
                    canvas.save()
                    canvas.rotate(22f)
                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = LinearGradient(
                            sweepX - barW / 2f, 0f, sweepX + barW / 2f, 0f,
                            intArrayOf(
                                Color.TRANSPARENT,
                                Color.argb((0.32f * intensity * 255).roundToInt().coerceIn(0, 255), 255, 255, 255),
                                Color.argb((0.22f * intensity * 255).roundToInt().coerceIn(0, 255), fxR, fxG, fxB),
                                Color.TRANSPARENT
                            ),
                            null,
                            Shader.TileMode.CLAMP
                        )
                    }
                    canvas.drawRect(sweepX - barW / 2f, -canvasH, sweepX + barW / 2f, canvasH, p)
                    canvas.restore()
                }
                EffectKernel.MOTION_BLUR -> {
                    val streaks = (4 + (detail * 8).toInt()).coerceAtMost(12)
                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.argb((0.12f * intensity * 255).roundToInt().coerceIn(0, 255), fxR, fxG, fxB)
                        strokeWidth = 3f * scaleRef
                    }
                    for (i in 0 until streaks) {
                        val yOff = (-canvasH * 0.38f) + (i / streaks.toFloat()) * (canvasH * 0.76f)
                        val dx = sin(timePhase + i) * 18f * intensity * scaleRef
                        canvas.drawLine(-canvasW * 0.45f + dx, yOff, canvasW * 0.45f + dx, yOff, p)
                    }
                }
                EffectKernel.GAUSSIAN_BLUR -> {
                    for (l in 1..3) {
                        val frac = 0.90f - l * 0.12f
                        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.argb((((0.09f + detail * 0.10f) * intensity / l) * 255).roundToInt().coerceIn(0, 255), fxR, fxG, fxB)
                        }
                        val rect = RectF(-canvasW * frac / 2f, -canvasH * frac / 2f, canvasW * frac / 2f, canvasH * frac / 2f)
                        canvas.drawRoundRect(rect, 18f * l * scaleRef, 18f * l * scaleRef, p)
                    }
                }
                EffectKernel.RADIAL_BLUR -> {
                    val rings = (3 + (detail * 5).toInt()).coerceIn(3, 8)
                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = (3f + detail * 4f) * scaleRef
                    }
                    for (i in 1..rings) {
                        p.color = Color.argb(((0.18f / i) * intensity * 255).roundToInt().coerceIn(0, 255), fxR, fxG, fxB)
                        canvas.drawCircle(0f, 0f, (min(canvasW, canvasH) * 0.12f) * i, p)
                    }
                }
                EffectKernel.AMBIENT_BACKDROP -> {
                    val radius = (max(canvasW, canvasH) * (0.48f + detail * 0.35f)).coerceAtLeast(32f)
                    val shiftX = sin(timePhase * 0.6f) * canvasW * 0.08f
                    val shiftY = cos(timePhase * 0.5f) * canvasH * 0.08f
                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = RadialGradient(
                            shiftX, shiftY, radius,
                            intArrayOf(
                                Color.argb((0.32f * intensity * 255).roundToInt().coerceIn(0, 255), fxR, fxG, fxB),
                                Color.argb((0.10f * intensity * 255).roundToInt().coerceIn(0, 255), fxR, fxG, fxB),
                                Color.TRANSPARENT
                            ),
                            floatArrayOf(0f, 0.55f, 1f),
                            Shader.TileMode.CLAMP
                        )
                    }
                    canvas.drawRect(-canvasW / 2f, -canvasH / 2f, canvasW / 2f, canvasH / 2f, p)
                }
                EffectKernel.BOKEH_BACKDROP -> {
                    val orbCount = (6 + (detail * 10).toInt()).coerceIn(6, 16)
                    val p = Paint(Paint.ANTI_ALIAS_FLAG)
                    for (i in 0 until orbCount) {
                        val ox = sin(i * 1.618f + timePhase * 0.4f) * canvasW * 0.38f
                        val oy = cos(i * 2.13f + timePhase * 0.35f) * canvasH * 0.38f
                        val rad = (12f + (i % 4) * 9f + detail * 14f) * scaleRef
                        p.color = Color.argb(((0.12f + (i % 3) * 0.06f) * intensity * 255).roundToInt().coerceIn(0, 255), fxR, fxG, fxB)
                        canvas.drawCircle(ox, oy, rad, p)
                    }
                }
                EffectKernel.GRID_MATRIX -> {
                    val cell = (28f - detail * 14f).coerceAtLeast(12f) * scaleRef
                    val shiftY = (timePhase * 10f) % cell
                    val p = Paint().apply {
                        color = Color.argb((0.20f * intensity * 255).roundToInt().coerceIn(0, 255), fxR, fxG, fxB)
                        strokeWidth = 1f * scaleRef
                    }
                    var gx = -canvasW / 2f
                    while (gx <= canvasW / 2f) {
                        canvas.drawLine(gx, -canvasH / 2f, gx, canvasH / 2f, p)
                        gx += cell
                    }
                    var gy = -canvasH / 2f + shiftY
                    while (gy <= canvasH / 2f) {
                        canvas.drawLine(-canvasW / 2f, gy, canvasW / 2f, gy, p)
                        gy += cell
                    }
                }
                EffectKernel.WAVE_WARP -> {
                    val waves = 5
                    val amp = 14f * scaleRef * intensity
                    val freq = 2f + detail * 4f
                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = 1.5f * scaleRef
                        color = Color.argb((0.24f * intensity * 255).roundToInt().coerceIn(0, 255), fxR, fxG, fxB)
                    }
                    for (w in 0 until waves) {
                        val baseY = -canvasH * 0.35f + (w / (waves - 1f)) * (canvasH * 0.70f)
                        val path = Path()
                        for (s in 0..20) {
                            val t = s / 20f
                            val x = -canvasW / 2f + t * canvasW
                            val y = baseY + sin(t * freq * PI.toFloat() + timePhase + w) * amp
                            if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        canvas.drawPath(path, p)
                    }
                }
                EffectKernel.CAMERA_SHAKE -> {
                    val jitter = (4f + detail * 10f) * intensity * scaleRef
                    val xOff = sin(timePhase * 7f) * jitter
                    val yOff = cos(timePhase * 9f) * jitter
                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = 1.5f * scaleRef
                        color = Color.argb((0.16f * intensity * 255).roundToInt().coerceIn(0, 255), fxR, fxG, fxB)
                    }
                    canvas.drawRect(-canvasW * 0.46f + xOff, -canvasH * 0.46f + yOff, canvasW * 0.46f + xOff, canvasH * 0.46f + yOff, p)
                }
                EffectKernel.FILM_GRAIN -> {
                    val count = (18 + (detail * 32).toInt()).coerceAtMost(50)
                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.argb((0.18f * intensity * 255).roundToInt().coerceIn(0, 255), fxR, fxG, fxB)
                    }
                    for (idx in 0 until count) {
                        val gx = (((idx * 73 + (timeMs / 50L).toInt() * 31) % 100) / 100f - 0.5f) * canvasW
                        val gy = (((idx * 137 + (timeMs / 50L).toInt() * 19) % 100) / 100f - 0.5f) * canvasH
                        canvas.drawCircle(gx, gy, (1f + (idx % 2)) * scaleRef, p)
                    }
                }
                EffectKernel.LENS_DISTORTION -> {
                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = (1.5f + intensity * 2f) * scaleRef
                        color = Color.argb((0.24f * intensity * 255).roundToInt().coerceIn(0, 255), fxR, fxG, fxB)
                    }
                    canvas.drawCircle(0f, 0f, canvasW * (0.48f - detail * 0.10f), p)
                }
                EffectKernel.PIXELATE, EffectKernel.HALFTONE -> {
                    val step = (24f - detail * 12f).coerceAtLeast(10f) * scaleRef
                    val isPix = effect.effectiveKernel == EffectKernel.PIXELATE
                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = if (isPix) Paint.Style.STROKE else Paint.Style.FILL
                        strokeWidth = 1f
                        color = Color.argb(((if (isPix) 0.10f else 0.16f) * intensity * 255).roundToInt().coerceIn(0, 255), fxR, fxG, fxB)
                    }
                    var gx = -canvasW * 0.42f
                    while (gx <= canvasW * 0.42f) {
                        var gy = -canvasH * 0.42f
                        while (gy <= canvasH * 0.42f) {
                            if (isPix) canvas.drawRect(gx, gy, gx + step * 0.72f, gy + step * 0.72f, p)
                            else canvas.drawCircle(gx, gy, step * 0.22f, p)
                            gy += step
                        }
                        gx += step
                    }
                }
                EffectKernel.KALEIDOSCOPE, EffectKernel.MIRROR_SPLIT -> {
                    val slices = (4 + (detail * 6).toInt()).coerceIn(2, 10)
                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        strokeWidth = 1.5f * scaleRef
                        color = Color.argb((0.25f * intensity * 255).roundToInt().coerceIn(0, 255), fxR, fxG, fxB)
                    }
                    for (i in 0 until slices) {
                        val deg = (180f / slices) * i + timePhase * 15f
                        canvas.save()
                        canvas.rotate(deg)
                        canvas.drawLine(-canvasW, 0f, canvasW, 0f, p)
                        canvas.restore()
                    }
                }
                EffectKernel.STARBURST -> {
                    val rays = 8
                    val r = canvasW * (0.28f + detail * 0.25f)
                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        strokeWidth = 2f * scaleRef
                        color = Color.argb((0.30f * intensity * 255).roundToInt().coerceIn(0, 255), fxR, fxG, fxB)
                    }
                    for (i in 0 until rays) {
                        val a = (i / rays.toFloat()) * 2f * PI.toFloat() + timePhase * 0.5f
                        canvas.drawLine(0f, 0f, cos(a) * r, sin(a) * r, p)
                    }
                }
                EffectKernel.GLITCH_BLOCK -> {
                    val p = Paint(Paint.ANTI_ALIAS_FLAG)
                    for (b in 0 until 5) {
                        val y = (-canvasH * 0.4f) + ((b * 0.2f + (timePhase * 0.2f) % 0.2f) * canvasH)
                        val xShift = sin(timePhase * 5f + b * 2f) * 28f * scaleRef * intensity
                        val aInt = (0.24f * intensity * 255).roundToInt().coerceIn(0, 255)
                        p.color = if (b % 2 == 0) Color.argb(aInt, fxR, fxG, fxB) else Color.argb(aInt, 255, 46, 99)
                        canvas.drawRect(-canvasW * 0.35f + xShift, y, canvasW * 0.35f + xShift, y + (8f + detail * 10f) * scaleRef, p)
                    }
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
        opacity: Float,
        textureBitmap: Bitmap? = null
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

        var minMx = Float.MAX_VALUE; var maxMx = -Float.MAX_VALUE
        var minMy = Float.MAX_VALUE; var maxMy = -Float.MAX_VALUE
        var minMz = Float.MAX_VALUE; var maxMz = -Float.MAX_VALUE
        for (v in mesh.vertices) {
            if (v.x < minMx) minMx = v.x; if (v.x > maxMx) maxMx = v.x
            if (v.y < minMy) minMy = v.y; if (v.y > maxMy) maxMy = v.y
            if (v.z < minMz) minMz = v.z; if (v.z > maxMz) maxMz = v.z
        }
        val spanX = (maxMx - minMx).coerceAtLeast(0.01f)
        val spanY = (maxMy - minMy).coerceAtLeast(0.01f)
        val spanZ = (maxMz - minMz).coerceAtLeast(0.01f)

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
        val texMix = config.textureMix.coerceIn(0f, 1f)
        val useTex = textureBitmap != null && !textureBitmap.isRecycled && !wireframeOnly && texMix > 0.02f
        val shader = if (useTex) BitmapShader(textureBitmap!!, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP) else null
        val texPaint = if (shader != null) Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { this.shader = shader } else null
        val texMatrix = Matrix()

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
                val baseAlpha = if (useTex) (1f - texMix * 0.85f).coerceAtLeast(0.08f) * opacity else opacity
                fillPaint.color = Color.argb(
                    (baseAlpha * 255).roundToInt().coerceIn(0, 255),
                    (baseR * light).roundToInt().coerceIn(0, 255),
                    (baseG * light).roundToInt().coerceIn(0, 255),
                    (baseB * light).roundToInt().coerceIn(0, 255)
                )
                canvas.drawPath(path, fillPaint)

                if (shader != null && texPaint != null && textureBitmap != null) {
                    val ma = mesh.vertices[face.i0]
                    val mb = mesh.vertices[face.i1]
                    val mc = mesh.vertices[face.i2]
                    val mux = mb.x - ma.x; val muy = mb.y - ma.y; val muz = mb.z - ma.z
                    val mvx = mc.x - ma.x; val mvy = mc.y - ma.y; val mvz = mc.z - ma.z
                    val mnx = abs(muy * mvz - muz * mvy)
                    val mny = abs(muz * mvx - mux * mvz)
                    val mnz = abs(mux * mvy - muy * mvx)
                    fun uvFor(v: Vec3): Pair<Float, Float> = when {
                        mnx >= mny && mnx >= mnz -> ((v.z - minMz) / spanZ) to ((v.y - minMy) / spanY)
                        mny >= mnx && mny >= mnz -> ((v.x - minMx) / spanX) to ((v.z - minMz) / spanZ)
                        else -> ((v.x - minMx) / spanX) to ((v.y - minMy) / spanY)
                    }
                    val tw = textureBitmap.width.toFloat().coerceAtLeast(2f)
                    val th = textureBitmap.height.toFloat().coerceAtLeast(2f)
                    val (u0, v0) = uvFor(ma); val (u1, v1) = uvFor(mb); val (u2, v2) = uvFor(mc)
                    val su0 = u0 * tw; val sv0 = v0 * th
                    val su1 = u1 * tw; val sv1 = v1 * th
                    val su2 = u2 * tw; val sv2 = v2 * th
                    val uvArea = abs((su1 - su0) * (sv2 - sv0) - (su2 - su0) * (sv1 - sv0))
                    val scrArea = abs((x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0))
                    if (uvArea > 1f && scrArea > 2f) {
                        texMatrix.reset()
                        val src = floatArrayOf(su0, sv0, su1, sv1, su2, sv2)
                        val dst = floatArrayOf(x0, y0, x1, y1, x2, y2)
                        if (texMatrix.setPolyToPoly(src, 0, dst, 0, 3)) {
                            shader.setLocalMatrix(texMatrix)
                            val lightC = (light * 235f).roundToInt().coerceIn(55, 255)
                            texPaint.colorFilter = LightingColorFilter(Color.rgb(lightC, lightC, lightC), 0)
                            texPaint.alpha = (texMix * opacity * 255f).roundToInt().coerceIn(0, 255)
                            canvas.drawPath(path, texPaint)
                        }
                    }
                }
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
                if (clip.type == ClipType.VIDEO_SAMPLE) {
                    decodeClipBitmap(context, clip)?.let { result[clip.id] = it }
                } else if (clip.type == ClipType.MODEL_3D) {
                    val texUri = clip.model3dConfig?.imageUri
                    if (!texUri.isNullOrBlank()) {
                        decodeUriBitmapArgb8888(context, texUri, sampleSize = 2)?.let {
                            result["tex3d_${clip.id}"] = it
                        }
                    }
                }
            }
        }
        return result
    }

    private fun preloadVideoRetrievers(context: Context, project: Project): Map<String, MediaMetadataRetriever> {
        val map = mutableMapOf<String, MediaMetadataRetriever>()
        for (track in project.tracks) {
            for (clip in track.clips) {
                if (clip.type != ClipType.VIDEO_SAMPLE) continue
                val uriStr = clip.mediaUri ?: continue
                if (!DeviceMediaScanner.isVideoUri(context, uriStr, true)) continue
                runCatching {
                    val uri = Uri.parse(uriStr)
                    val ret = MediaMetadataRetriever()
                    if (uri.scheme == "file" && uri.path != null) ret.setDataSource(uri.path)
                    else ret.setDataSource(context, uri)
                    map[clip.id] = ret
                }
            }
        }
        return map
    }

    private fun decodeUriBitmapArgb8888(context: Context, uriStr: String, sampleSize: Int = 2): Bitmap? {
        val uri = runCatching { Uri.parse(uriStr) }.getOrNull() ?: return null
        return runCatching {
            val raw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = if (uri.scheme == "file" && uri.path != null) {
                    ImageDecoder.createSource(File(uri.path!!))
                } else {
                    ImageDecoder.createSource(context.contentResolver, uri)
                }
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    if (sampleSize > 1) {
                        decoder.setTargetSize(
                            (info.size.width / sampleSize).coerceAtLeast(1),
                            (info.size.height / sampleSize).coerceAtLeast(1)
                        )
                    }
                }
            } else {
                val opts = DeviceMediaScanner.transparentBitmapOptions(sampleSize)
                val stream = if (uri.scheme == "file") {
                    FileInputStream(File(uri.path.orEmpty()))
                } else {
                    context.contentResolver.openInputStream(uri)
                }
                stream?.use { BitmapFactory.decodeStream(it, null, opts) }
            } ?: return null

            if (raw.config != Bitmap.Config.ARGB_8888) {
                raw.copy(Bitmap.Config.ARGB_8888, false)?.also {
                    it.setHasAlpha(true)
                    raw.recycle()
                } ?: raw
            } else {
                raw.apply { setHasAlpha(true) }
            }
        }.getOrNull()
    }

    private fun decodeClipBitmap(context: Context, clip: ClipItem): Bitmap? {
        clip.assetResId?.let { resId ->
            runCatching {
                val opts = DeviceMediaScanner.transparentBitmapOptions(2)
                return BitmapFactory.decodeResource(context.resources, resId, opts)
            }
        }
        val uriStr = clip.mediaUri ?: return null
        if (!DeviceMediaScanner.isVideoUri(context, uriStr, true)) {
            decodeUriBitmapArgb8888(context, uriStr, sampleSize = 1)?.let { return it }
        }
        val uri = runCatching { Uri.parse(uriStr) }.getOrNull() ?: return null
        val retriever = MediaMetadataRetriever()
        return try {
            if (uri.scheme == "file" && uri.path != null) {
                retriever.setDataSource(uri.path)
            } else {
                retriever.setDataSource(context, uri)
            }
            retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (_: Exception) {
            decodeUriBitmapArgb8888(context, uriStr, sampleSize = 2)
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
