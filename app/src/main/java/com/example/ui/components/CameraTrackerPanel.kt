package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ClipItem
import com.example.model.ClipType
import com.example.model.Project
import com.example.ui.theme.KeyframeViolet
import com.example.ui.theme.MotionCyan
import com.example.ui.theme.PlayheadAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioDarkBg
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.SuccessEmerald
import kotlin.math.roundToInt

@Composable
fun CameraTrackerPanel(
    project: Project,
    selectedClip: ClipItem?,
    isTracking: Boolean,
    statusMessage: String?,
    onRunTracking: (
        sourceClipId: String,
        targetClipId: String,
        roiX: Float,
        roiY: Float,
        applyTranslation: Boolean,
        applyScale: Boolean,
        stabilizeMode: Boolean
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val allClips = remember(project) { project.tracks.flatMap { it.clips } }
    val videoClips = remember(allClips) {
        allClips.filter { it.type == ClipType.VIDEO_SAMPLE && it.mediaUri != null }
    }
    val defaultSourceClip = remember(selectedClip, videoClips, allClips) {
        if (selectedClip != null && selectedClip.type == ClipType.VIDEO_SAMPLE && selectedClip.mediaUri != null) {
            selectedClip
        } else {
            videoClips.firstOrNull() ?: selectedClip ?: allClips.firstOrNull()
        }
    }

    var sourceClipId by remember(defaultSourceClip?.id, videoClips.size) {
        mutableStateOf(defaultSourceClip?.id)
    }
    val resolvedSource = allClips.find { it.id == sourceClipId } ?: defaultSourceClip

    val nonSourceDefaultTarget = remember(selectedClip, resolvedSource, allClips) {
        when {
            selectedClip != null && selectedClip.id != resolvedSource?.id -> selectedClip.id
            else -> allClips.firstOrNull {
                it.id != resolvedSource?.id &&
                    (it.type == ClipType.TEXT || it.type == ClipType.MODEL_3D ||
                        it.type == ClipType.NULL_OBJECT || it.type == ClipType.SHAPE)
            }?.id ?: resolvedSource?.id
        }
    }
    var targetClipId by remember(nonSourceDefaultTarget, resolvedSource?.id) {
        mutableStateOf(nonSourceDefaultTarget)
    }
    val resolvedTarget = allClips.find { it.id == targetClipId } ?: resolvedSource

    var roiX by remember { mutableFloatStateOf(0.5f) }
    var roiY by remember { mutableFloatStateOf(0.5f) }
    var stabilizeMode by remember { mutableStateOf(false) }
    var applyTranslation by remember { mutableStateOf(true) }
    var applyScale by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(StudioDarkBg)
            .padding(10.dp)
            .testTag("camera_tracker_panel"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.TrackChanges, contentDescription = null, tint = MotionCyan, modifier = Modifier.size(16.dp))
                Text(
                    text = "2D Optical Camera Tracker",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            val pointCount = resolvedTarget?.cameraTrackPoints?.size ?: 0
            if (pointCount > 0) {
                val avgConf = resolvedTarget?.cameraTrackPoints
                    ?.map { it.confidence }?.average()?.let { (it * 100).roundToInt() } ?: 100
                Surface(
                    color = SuccessEmerald.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, SuccessEmerald)
                ) {
                    Text(
                        text = "$pointCount pts • $avgConf% conf",
                        color = SuccessEmerald,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // Source Video Picker
        Text("Source Video Clip", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        if (videoClips.isEmpty()) {
            Surface(
                color = StudioCardBg,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, PlayheadAmber.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Add a device video clip to track real camera motion across frames.",
                    color = PlayheadAmber,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(videoClips, key = { it.id }) { clip ->
                    val active = clip.id == resolvedSource?.id
                    Surface(
                        color = if (active) MotionCyan.copy(alpha = 0.22f) else StudioSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (active) MotionCyan else StudioBorder),
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { sourceClipId = clip.id }
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = clip.title,
                                color = if (active) MotionCyan else Color.White,
                                fontSize = 11.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Interactive ROI Target Crosshair Pad
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Track Target Region (Tap or Drag)", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "X ${(roiX * 100).roundToInt()}% • Y ${(roiY * 100).roundToInt()}%",
                color = PlayheadAmber,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(StudioSurface)
                .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
                .testTag("tracker_roi_pad")
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        roiX = (offset.x / size.width.toFloat()).coerceIn(0.15f, 0.85f)
                        roiY = (offset.y / size.height.toFloat()).coerceIn(0.15f, 0.85f)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        roiX = (change.position.x / size.width.toFloat()).coerceIn(0.15f, 0.85f)
                        roiY = (change.position.y / size.height.toFloat()).coerceIn(0.15f, 0.85f)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stepX = size.width / 4f
                val stepY = size.height / 4f
                for (i in 1..3) {
                    drawLine(
                        color = StudioBorder.copy(alpha = 0.55f),
                        start = Offset(stepX * i, 0f),
                        end = Offset(stepX * i, size.height),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = StudioBorder.copy(alpha = 0.55f),
                        start = Offset(0f, stepY * i),
                        end = Offset(size.width, stepY * i),
                        strokeWidth = 1f
                    )
                }
                val cx = roiX * size.width
                val cy = roiY * size.height
                drawLine(
                    color = MotionCyan.copy(alpha = 0.7f),
                    start = Offset(cx, 0f),
                    end = Offset(cx, size.height),
                    strokeWidth = 1.5f
                )
                drawLine(
                    color = MotionCyan.copy(alpha = 0.7f),
                    start = Offset(0f, cy),
                    end = Offset(size.width, cy),
                    strokeWidth = 1.5f
                )
                drawRect(
                    color = PlayheadAmber,
                    topLeft = Offset(cx - 18f, cy - 18f),
                    size = Size(36f, 36f),
                    style = Stroke(width = 2.5f)
                )
                drawCircle(
                    color = PlayheadAmber,
                    radius = 4f,
                    center = Offset(cx, cy)
                )
            }
        }

        // Mode & Target Layer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = if (!stabilizeMode) MotionCyan.copy(alpha = 0.22f) else StudioSurfaceVariant,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (!stabilizeMode) MotionCyan else StudioBorder),
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { stabilizeMode = false }
                    .testTag("track_mode_follow")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Follow Motion",
                        color = if (!stabilizeMode) MotionCyan else Color.LightGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Surface(
                color = if (stabilizeMode) KeyframeViolet.copy(alpha = 0.25f) else StudioSurfaceVariant,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (stabilizeMode) KeyframeViolet else StudioBorder),
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { stabilizeMode = true }
                    .testTag("track_mode_stabilize")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Stabilize Camera",
                        color = if (stabilizeMode) Color.White else Color.LightGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Target Layer to Receive Keyframes
        if (allClips.isNotEmpty()) {
            Text("Apply Keyframes To Layer", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(allClips.filter { it.type != ClipType.AUDIO }, key = { "tgt_${it.id}" }) { clip ->
                    val active = clip.id == resolvedTarget?.id
                    Surface(
                        color = if (active) KeyframeViolet.copy(alpha = 0.24f) else StudioSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (active) KeyframeViolet else StudioBorder),
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { targetClipId = clip.id }
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (clip.id == resolvedSource?.id) "${clip.title} (Self)" else clip.title,
                                color = if (active) Color.White else Color.LightGray,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Channel toggles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = StudioSurfaceVariant,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, StudioBorder),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Position X/Y", color = Color.White, fontSize = 11.sp)
                    Switch(
                        checked = applyTranslation,
                        onCheckedChange = { applyTranslation = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = MotionCyan),
                        modifier = Modifier.testTag("track_translation_switch")
                    )
                }
            }
            Surface(
                color = StudioSurfaceVariant,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, StudioBorder),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Scale / Zoom", color = Color.White, fontSize = 11.sp)
                    Switch(
                        checked = applyScale,
                        onCheckedChange = { applyScale = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = PlayheadAmber),
                        modifier = Modifier.testTag("track_scale_switch")
                    )
                }
            }
        }

        if (!statusMessage.isNullOrBlank()) {
            Surface(
                color = StudioCardBg,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, StudioBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = statusMessage,
                    color = MotionCyan,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        Button(
            onClick = {
                val srcId = resolvedSource?.id ?: return@Button
                val tgtId = resolvedTarget?.id ?: srcId
                onRunTracking(srcId, tgtId, roiX, roiY, applyTranslation, applyScale, stabilizeMode)
            },
            enabled = !isTracking && resolvedSource != null && resolvedSource.type == ClipType.VIDEO_SAMPLE && resolvedSource.mediaUri != null,
            colors = ButtonDefaults.buttonColors(
                containerColor = MotionCyan,
                contentColor = Color.Black,
                disabledContainerColor = StudioSurfaceVariant,
                disabledContentColor = Color.Gray
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("run_camera_track_button")
        ) {
            if (isTracking) {
                CircularProgressIndicator(
                    color = MotionCyan,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tracking Video Frames...", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            } else {
                Icon(Icons.Default.CenterFocusStrong, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (stabilizeMode) "Analyze & Stabilize Video" else "Track Video Motion",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp
                )
            }
        }
    }
}
