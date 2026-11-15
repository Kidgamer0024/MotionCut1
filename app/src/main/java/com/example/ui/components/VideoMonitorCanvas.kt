package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DeviceMediaScanner
import com.example.engine.ColorGradingCompositor
import com.example.engine.EffectRenderer
import com.example.engine.KeyframeEvaluator
import com.example.engine.MaskCompositor
import com.example.engine.Mesh3DParser
import com.example.engine.Mesh3DRenderer.drawModel3D
import com.example.model.ClipItem
import com.example.model.ClipType
import com.example.model.MaskShapeType
import com.example.model.Project
import com.example.model.TransformState
import com.example.ui.theme.KeyframeViolet
import com.example.ui.theme.MotionCyan
import com.example.ui.theme.PlayheadAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioDarkBg

@Composable
fun VideoMonitorCanvas(
    project: Project,
    currentTimeMs: Long,
    selectedClipId: String?,
    isPlaying: Boolean,
    showScopes: Boolean,
    onTogglePlay: () -> Unit,
    onStepFrame: (Boolean) -> Unit,
    onToggleScopes: () -> Unit,
    onUpdateClipTransform: (String, Float, Float, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val textMeasurer = rememberTextMeasurer()

    // Pre-cache drawables for sample clips
    val clipBitmaps = remember(project) {
        val map = mutableMapOf<Int, ImageBitmap>()
        project.tracks.forEach { track ->
            track.clips.forEach { clip ->
                clip.assetResId?.let { resId ->
                    if (!map.containsKey(resId)) {
                        try {
                            val bmp = BitmapFactory.decodeResource(context.resources, resId)
                            if (bmp != null) {
                                map[resId] = bmp.asImageBitmap()
                            }
                        } catch (_: Exception) {
                        }
                    }
                }
            }
        }
        map
    }

    // Async decode device video/photo URIs for clips added from Media
    var uriBitmaps by remember { mutableStateOf<Map<String, ImageBitmap>>(emptyMap()) }
    LaunchedEffect(project) {
        val urisToLoad = project.tracks
            .flatMap { it.clips }
            .mapNotNull { it.mediaUri }
            .distinct()
            .filterNot { uriBitmaps.containsKey(it) }
        if (urisToLoad.isNotEmpty()) {
            val loaded = uriBitmaps.toMutableMap()
            urisToLoad.forEach { uriStr ->
                val bmp = DeviceMediaScanner.loadThumbnailBitmap(context, uriStr, isVideo = true)
                    ?: DeviceMediaScanner.loadThumbnailBitmap(context, uriStr, isVideo = false)
                if (bmp != null) {
                    loaded[uriStr] = bmp
                }
            }
            uriBitmaps = loaded
        }
    }

    // Active color filter for current frame
    val activeColorFilter = remember(project.masterColorGrading, selectedClipId) {
        val selectedClip = selectedClipId?.let { project.findClip(it) }
        val grading = selectedClip?.colorGrading ?: project.masterColorGrading
        ColorGradingCompositor.createColorFilter(grading)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF06080D))
            .border(1.dp, StudioBorder),
        contentAlignment = Alignment.Center
    ) {
        val containerAspectRatio = maxWidth / maxHeight
        val projectRatio = project.aspectRatio.ratioFloat

        // Aspect ratio container box
        Box(
            modifier = Modifier
                .aspectRatio(projectRatio, matchHeightConstraintsFirst = (projectRatio < containerAspectRatio))
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black)
                .pointerInput(selectedClipId) {
                    if (selectedClipId != null) {
                        detectTransformGestures { _, pan, zoom, rotation ->
                            onUpdateClipTransform(
                                selectedClipId,
                                pan.x,
                                pan.y,
                                zoom,
                                rotation
                            )
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasW = size.width
                val canvasH = size.height

                // 1. Draw subtle safety guide margins (90% action safe, 80% title safe)
                drawRect(
                    color = Color(0x15FFFFFF),
                    topLeft = Offset(canvasW * 0.05f, canvasH * 0.05f),
                    size = Size(canvasW * 0.90f, canvasH * 0.90f),
                    style = Stroke(width = 1f)
                )
                drawRect(
                    color = Color(0x10FFFFFF),
                    topLeft = Offset(canvasW * 0.10f, canvasH * 0.10f),
                    size = Size(canvasW * 0.80f, canvasH * 0.80f),
                    style = Stroke(width = 1f)
                )

                // 2. Render all visible tracks from bottom to top
                val visibleTracks = project.tracks.filter { it.isVisible }
                for (track in visibleTracks) {
                    val activeClips = track.clips.filter { clip ->
                        currentTimeMs >= clip.startTimeMs && currentTimeMs <= clip.endTimeMs
                    }

                    for (clip in activeClips) {
                        val transform = KeyframeEvaluator.evaluateClipTransform(clip, currentTimeMs)
                        val isSelected = clip.id == selectedClipId

                        val centerX = canvasW / 2f + transform.positionX
                        val centerY = canvasH / 2f + transform.positionY

                        translate(left = centerX, top = centerY) {
                            rotate(degrees = transform.rotation) {
                                scale(scaleX = transform.scaleX, scaleY = transform.scaleY) {
                                    val enabledMask = clip.masks.firstOrNull { it.isEnabled }

                                    if (enabledMask != null) {
                                        val evaluatedMask = MaskCompositor.evaluateMask(
                                            mask = enabledMask,
                                            currentTimeMs = currentTimeMs,
                                            clipStartTimeMs = clip.startTimeMs,
                                            clipWidth = canvasW,
                                            clipHeight = canvasH
                                        )

                                        // Render clip inside mask clipping path
                                        clipPath(evaluatedMask.path) {
                                            drawClipLayerContent(
                                                context = context,
                                                clip = clip,
                                                currentTimeMs = currentTimeMs,
                                                canvasW = canvasW,
                                                canvasH = canvasH,
                                                clipBitmaps = clipBitmaps,
                                                uriBitmaps = uriBitmaps,
                                                activeColorFilter = activeColorFilter,
                                                transform = transform,
                                                textMeasurer = textMeasurer
                                            )
                                        }

                                        // Render glowing animated mask boundary
                                        if (isSelected) {
                                            drawPath(
                                                path = evaluatedMask.path,
                                                color = KeyframeViolet,
                                                style = Stroke(
                                                    width = 2.dp.toPx(),
                                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                                                )
                                            )

                                            // Draw vertex handles for polygon mask
                                            if (enabledMask.shapeType == MaskShapeType.FREEFORM_POLYGON) {
                                                enabledMask.vertices.forEach { v ->
                                                    val pt = Offset(v.x * evaluatedMask.scaleX, v.y * evaluatedMask.scaleY)
                                                    drawCircle(Color.White, 5.dp.toPx(), pt)
                                                    drawCircle(KeyframeViolet, 5.dp.toPx(), pt, style = Stroke(2.dp.toPx()))
                                                }
                                            }
                                        }
                                    } else {
                                        // Render normally without mask
                                        drawClipLayerContent(
                                            context = context,
                                            clip = clip,
                                            currentTimeMs = currentTimeMs,
                                            canvasW = canvasW,
                                            canvasH = canvasH,
                                            clipBitmaps = clipBitmaps,
                                            uriBitmaps = uriBitmaps,
                                            activeColorFilter = activeColorFilter,
                                            transform = transform,
                                            textMeasurer = textMeasurer
                                        )
                                    }

                                    // Render visual effects using expanded 20-kernel EffectRenderer
                                    clip.effects.filter { it.isEnabled }.forEach { fx ->
                                        EffectRenderer.run {
                                            drawEffectOverlay(
                                                effect = fx,
                                                canvasW = canvasW,
                                                canvasH = canvasH,
                                                currentTimeMs = currentTimeMs
                                            )
                                        }
                                    }

                                    // Direct Manipulation Transform Bounding Box (Alight Motion style)
                                    if (isSelected) {
                                        val bboxW = when (clip.type) {
                                            ClipType.SHAPE -> 150.dp.toPx()
                                            ClipType.MODEL_3D -> 185.dp.toPx()
                                            else -> canvasW * 0.85f
                                        }
                                        val bboxH = when (clip.type) {
                                            ClipType.SHAPE -> 150.dp.toPx()
                                            ClipType.MODEL_3D -> 185.dp.toPx()
                                            else -> canvasH * 0.60f
                                        }
                                        val halfBw = bboxW / 2f
                                        val halfBh = bboxH / 2f

                                        // Neon border
                                        drawRect(
                                            color = MotionCyan,
                                            topLeft = Offset(-halfBw, -halfBh),
                                            size = Size(bboxW, bboxH),
                                            style = Stroke(width = 2.dp.toPx())
                                        )

                                        // Corner handles
                                        val handleR = 6.dp.toPx()
                                        drawCircle(Color.White, handleR, Offset(-halfBw, -halfBh))
                                        drawCircle(MotionCyan, handleR, Offset(-halfBw, -halfBh), style = Stroke(2f))
                                        drawCircle(Color.White, handleR, Offset(halfBw, -halfBh))
                                        drawCircle(MotionCyan, handleR, Offset(halfBw, -halfBh), style = Stroke(2f))
                                        drawCircle(Color.White, handleR, Offset(-halfBw, halfBh))
                                        drawCircle(MotionCyan, handleR, Offset(-halfBw, halfBh), style = Stroke(2f))
                                        drawCircle(Color.White, handleR, Offset(halfBw, halfBh))
                                        drawCircle(MotionCyan, handleR, Offset(halfBw, halfBh), style = Stroke(2f))

                                        // Rotation Handle stalk
                                        val rotHandleY = -halfBh - 28.dp.toPx()
                                        drawLine(
                                            color = MotionCyan,
                                            start = Offset(0f, -halfBh),
                                            end = Offset(0f, rotHandleY),
                                            strokeWidth = 2f
                                        )
                                        drawCircle(Color.White, handleR + 2f, Offset(0f, rotHandleY))
                                        drawCircle(PlayheadAmber, handleR + 2f, Offset(0f, rotHandleY), style = Stroke(2f))

                                        // Center anchor crosshair
                                        drawLine(MotionCyan, Offset(-8f, 0f), Offset(8f, 0f), strokeWidth = 2f)
                                        drawLine(MotionCyan, Offset(0f, -8f), Offset(0f, 8f), strokeWidth = 2f)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Overlay HUD Controls (Timecode, Scopes toggle, Play/Pause)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            // Top HUD: Aspect Ratio badge & Scopes button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = StudioDarkBg.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder)
                ) {
                    Text(
                        text = "${project.aspectRatio.ratioText} • ${project.resolution.label} • ${project.frameRate.fps} FPS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MotionCyan,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    color = if (showScopes) MotionCyan.copy(alpha = 0.3f) else StudioDarkBg.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (showScopes) MotionCyan else StudioBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .clickable { onToggleScopes() }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = "Toggle Pro Scopes",
                            tint = if (showScopes) MotionCyan else Color.LightGray,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Scopes",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (showScopes) MotionCyan else Color.LightGray,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Bottom Transport Bar: Current Timecode and Step buttons
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter),
                color = StudioDarkBg.copy(alpha = 0.90f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val totalSec = project.durationMs / 1000f
                    val curSec = currentTimeMs / 1000f
                    val frameNum = ((currentTimeMs % 1000L) / (1000f / project.frameRate.fps)).toInt()
                    val timecodeStr = String.format("%02d:%02d:%02d", (curSec / 60).toInt(), (curSec % 60).toInt(), frameNum)
                    val totalStr = String.format("%02d:%02d", (totalSec / 60).toInt(), (totalSec % 60).toInt())

                    Text(
                        text = "$timecodeStr / $totalStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )

                    IconButton(
                        onClick = { onStepFrame(false) },
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("step_backward_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastRewind,
                            contentDescription = "Step Back",
                            tint = Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onTogglePlay,
                        modifier = Modifier
                            .size(32.dp)
                            .background(MotionCyan, CircleShape)
                            .testTag("play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { onStepFrame(true) },
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("step_forward_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "Step Forward",
                            tint = Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawClipLayerContent(
    context: android.content.Context,
    clip: ClipItem,
    currentTimeMs: Long,
    canvasW: Float,
    canvasH: Float,
    clipBitmaps: Map<Int, ImageBitmap>,
    uriBitmaps: Map<String, ImageBitmap>,
    activeColorFilter: ColorFilter,
    transform: TransformState,
    textMeasurer: TextMeasurer
) {
    when (clip.type) {
        ClipType.VIDEO_SAMPLE -> {
            val bmp = clip.mediaUri?.let { uriBitmaps[it] } ?: clip.assetResId?.let { clipBitmaps[it] }
            if (bmp != null) {
                val dstW = canvasW
                val dstH = canvasW * (bmp.height.toFloat() / bmp.width.coerceAtLeast(1).toFloat())
                val dstTopLeft = Offset(-dstW / 2f, -dstH / 2f)

                drawImage(
                    image = bmp,
                    dstOffset = androidx.compose.ui.unit.IntOffset(dstTopLeft.x.toInt(), dstTopLeft.y.toInt()),
                    dstSize = androidx.compose.ui.unit.IntSize(dstW.toInt().coerceAtLeast(1), dstH.toInt().coerceAtLeast(1)),
                    colorFilter = activeColorFilter,
                    alpha = transform.opacity
                )
            } else {
                drawRoundRect(
                    color = Color(clip.colorHex),
                    topLeft = Offset(-canvasW / 2f, -canvasH / 2f),
                    size = Size(canvasW, canvasH),
                    alpha = transform.opacity
                )
            }
        }
        ClipType.COLOR_BG -> {
            drawRoundRect(
                color = Color(clip.colorHex),
                topLeft = Offset(-canvasW / 2f, -canvasH / 2f),
                size = Size(canvasW, canvasH),
                alpha = transform.opacity
            )
        }
        ClipType.SHAPE -> {
            val shape = clip.shapeConfig
            if (shape != null) {
                val shapeSize = 140.dp.toPx()
                drawVectorShapeGeometry(
                    type = shape.type,
                    shapeSize = shapeSize,
                    fillColor = Color(shape.fillColor),
                    strokeColor = Color(shape.strokeColor),
                    strokeWidth = shape.strokeWidth,
                    cornerRadius = shape.cornerRadius,
                    alpha = transform.opacity
                )
            }
        }
        ClipType.MODEL_3D -> {
            val evaluated3D = KeyframeEvaluator.evaluateModel3D(clip, currentTimeMs)
            if (evaluated3D != null) {
                val mesh = Mesh3DParser.getOrBuildMesh(context, evaluated3D, evaluated3D.extrusionDepth)
                val planeBmp = evaluated3D.imageUri?.let { uriBitmaps[it] }
                drawModel3D(
                    mesh = mesh,
                    config = evaluated3D,
                    viewportSize = minOf(canvasW, canvasH) * 0.72f,
                    alpha = transform.opacity,
                    planeImageBitmap = planeBmp,
                    colorFilter = activeColorFilter
                )
            }
        }
        ClipType.TEXT -> {
            val textConfig = clip.textConfig
            if (textConfig != null) {
                val textLayout = textMeasurer.measure(
                    text = textConfig.text,
                    style = TextStyle(
                        fontSize = textConfig.fontSizeSp.sp,
                        fontWeight = if (textConfig.isBold) FontWeight.Black else FontWeight.Normal,
                        color = Color(textConfig.textColor)
                    )
                )
                val textOffset = Offset(-textLayout.size.width / 2f, -textLayout.size.height / 2f)
                if (textConfig.strokeWidth > 0f) {
                    drawRoundRect(
                        color = Color(textConfig.strokeColor).copy(alpha = 0.5f * transform.opacity),
                        topLeft = textOffset.copy(x = textOffset.x - 12f, y = textOffset.y - 8f),
                        size = Size(textLayout.size.width + 24f, textLayout.size.height + 16f),
                        cornerRadius = CornerRadius(8f)
                    )
                }
                drawText(
                    textLayoutResult = textLayout,
                    topLeft = textOffset,
                    alpha = transform.opacity
                )
            }
        }
        ClipType.ADJUSTMENT -> {
            drawRect(
                color = Color(clip.colorHex).copy(alpha = 0.08f * transform.opacity),
                topLeft = Offset(-canvasW / 2f, -canvasH / 2f),
                size = Size(canvasW, canvasH)
            )
        }
        ClipType.NULL_OBJECT -> {
            val r = 22.dp.toPx()
            drawCircle(
                color = MotionCyan.copy(alpha = 0.7f * transform.opacity),
                radius = r,
                center = Offset.Zero,
                style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f))
            )
            drawLine(MotionCyan.copy(alpha = transform.opacity), Offset(-r * 1.4f, 0f), Offset(r * 1.4f, 0f), strokeWidth = 2f)
            drawLine(MotionCyan.copy(alpha = transform.opacity), Offset(0f, -r * 1.4f), Offset(0f, r * 1.4f), strokeWidth = 2f)
        }
        ClipType.AUDIO -> {
            // Audio track plays in timeline audio mix without occluding visual layers
        }
    }
}
