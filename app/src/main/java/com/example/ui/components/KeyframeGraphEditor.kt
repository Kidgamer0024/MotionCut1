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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.KeyframeEvaluator
import com.example.model.AnimatableProperty
import com.example.model.ClipItem
import com.example.model.CurveBreakpoint
import com.example.model.EasingType
import com.example.ui.theme.CutRose
import com.example.ui.theme.KeyframeViolet
import com.example.ui.theme.MotionCyan
import com.example.ui.theme.PlayheadAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioDarkBg
import com.example.ui.theme.StudioSurfaceVariant
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot

data class CurvePreset(
    val name: String,
    val p1x: Float,
    val p1y: Float,
    val p2x: Float,
    val p2y: Float
)

val CURVE_PRESETS = listOf(
    CurvePreset("Linear", 0.0f, 0.0f, 1.0f, 1.0f),
    CurvePreset("Ease In", 0.42f, 0.0f, 1.0f, 1.0f),
    CurvePreset("Ease Out", 0.0f, 0.0f, 0.58f, 1.0f),
    CurvePreset("Ease In-Out", 0.42f, 0.0f, 0.58f, 1.0f),
    CurvePreset("Punchy Whip", 0.15f, 0.85f, 0.35f, 1.15f),
    CurvePreset("Overshoot", 0.34f, 1.56f, 0.64f, 1.0f)
)

private fun easingToBezier(easing: EasingType?): EasingType.CubicBezier {
    return when (easing) {
        is EasingType.CubicBezier -> easing
        is EasingType.Linear -> EasingType.CubicBezier(0.0f, 0.0f, 1.0f, 1.0f)
        is EasingType.EaseIn -> EasingType.CubicBezier(0.42f, 0.0f, 1.0f, 1.0f)
        is EasingType.EaseOut -> EasingType.CubicBezier(0.0f, 0.0f, 0.58f, 1.0f)
        is EasingType.EaseInOut -> EasingType.CubicBezier(0.42f, 0.0f, 0.58f, 1.0f)
        is EasingType.Bounce -> EasingType.CubicBezier(0.25f, 0.9f, 0.35f, 1.25f)
        is EasingType.Elastic -> EasingType.CubicBezier(0.2f, 1.35f, 0.45f, 1.0f)
        is EasingType.MultiPoint -> EasingType.CubicBezier(0.42f, 0.0f, 0.58f, 1.0f)
        null -> EasingType.CubicBezier(0.42f, 0.0f, 0.58f, 1.0f)
    }
}

private fun defaultBreakpoints(): List<CurveBreakpoint> = listOf(
    CurveBreakpoint(0.30f, 0.18f),
    CurveBreakpoint(0.70f, 0.86f)
)

@Composable
fun KeyframeGraphEditor(
    clip: ClipItem,
    currentTimeMs: Long,
    selectedProperty: AnimatableProperty,
    onSelectProperty: (AnimatableProperty) -> Unit,
    onToggleKeyframe: () -> Unit = {},
    onDeleteKeyframe: (AnimatableProperty, String) -> Unit = { _, _ -> },
    onClearTrack: (AnimatableProperty) -> Unit = {},
    onJumpToKeyframe: (Boolean) -> Unit = {},
    onSeekInClip: (Long) -> Unit = {},
    onUpdateEasing: (AnimatableProperty, String, EasingType) -> Unit,
    onUpdateValue: (AnimatableProperty, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val clipDuration = clip.durationMs.coerceAtLeast(1L)
    val relTime = (currentTimeMs - clip.startTimeMs).coerceIn(0L, clipDuration)
    val track = clip.keyframeTracks[selectedProperty]
    val keyframes = track?.keyframes?.sortedBy { it.timeMs }.orEmpty()

    val nearbyKeyframe = keyframes.minByOrNull { abs(it.timeMs - relTime) }
        ?.takeIf { abs(it.timeMs - relTime) <= 160L }
    val hasKeyframeAtPlayhead = nearbyKeyframe != null

    var selectedKeyframeId by remember(clip.id, selectedProperty) { mutableStateOf<String?>(null) }
    val activeIndex = keyframes.indexOfLast { it.timeMs <= relTime + 40L }.coerceAtLeast(0)
    val activeKeyframe = keyframes.find { it.id == selectedKeyframeId }
        ?: nearbyKeyframe
        ?: keyframes.getOrNull(activeIndex)
    val segmentKeyframe = if (keyframes.size >= 2) {
        val idx = activeKeyframe?.let { keyframes.indexOfFirst { k -> k.id == it.id } } ?: activeIndex
        if (idx >= keyframes.lastIndex) keyframes[keyframes.lastIndex - 1]
        else keyframes[idx.coerceAtLeast(0)]
    } else {
        activeKeyframe
    }

    val isMultiPointEasing = segmentKeyframe?.easing is EasingType.MultiPoint
    var useBreakpointMode by remember(segmentKeyframe?.id, isMultiPointEasing) {
        mutableStateOf(isMultiPointEasing)
    }

    val currentBezier = easingToBezier(segmentKeyframe?.easing)
    var p1x by remember(segmentKeyframe?.id, currentBezier) { mutableStateOf(currentBezier.p1x) }
    var p1y by remember(segmentKeyframe?.id, currentBezier) { mutableStateOf(currentBezier.p1y) }
    var p2x by remember(segmentKeyframe?.id, currentBezier) { mutableStateOf(currentBezier.p2x) }
    var p2y by remember(segmentKeyframe?.id, currentBezier) { mutableStateOf(currentBezier.p2y) }
    var draggingHandle by remember { mutableIntStateOf(0) } // 1 = P1, 2 = P2

    val initialPoints = (segmentKeyframe?.easing as? EasingType.MultiPoint)?.points?.ifEmpty { defaultBreakpoints() }
        ?: defaultBreakpoints()
    var breakpoints by remember(segmentKeyframe?.id, initialPoints) { mutableStateOf(initialPoints) }
    var selectedBpIndex by remember(segmentKeyframe?.id) { mutableIntStateOf(0) }
    var draggingBpIndex by remember { mutableIntStateOf(-1) }

    val currentValue = KeyframeEvaluator.evaluateProperty(clip, selectedProperty, currentTimeMs)

    val formattedValue = when (selectedProperty) {
        AnimatableProperty.OPACITY -> "${(currentValue * 100f).toInt()}%"
        AnimatableProperty.SCALE,
        AnimatableProperty.DEPTH_Z_3D,
        AnimatableProperty.EXTRUSION_3D -> String.format(Locale.US, "%.2f%s", currentValue, selectedProperty.unit)
        else -> String.format(Locale.US, "%.1f%s", currentValue, selectedProperty.unit)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .background(StudioDarkBg)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("keyframe_graph_editor")
    ) {
        // 1. Property Selector Chips
        val props = remember(clip.type) {
            if (clip.type == com.example.model.ClipType.MODEL_3D) {
                AnimatableProperty.entries
            } else {
                listOf(
                    AnimatableProperty.POSITION_X,
                    AnimatableProperty.POSITION_Y,
                    AnimatableProperty.SCALE,
                    AnimatableProperty.ROTATION,
                    AnimatableProperty.OPACITY
                )
            }
        }
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(props) { prop ->
                val isSelected = prop == selectedProperty
                val hasKeyframes = clip.keyframeTracks[prop]?.keyframes?.isNotEmpty() == true

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) KeyframeViolet.copy(alpha = 0.25f) else StudioSurfaceVariant)
                        .border(
                            1.dp,
                            if (isSelected) KeyframeViolet else if (hasKeyframes) KeyframeViolet.copy(alpha = 0.45f) else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelectProperty(prop) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("kf_prop_${prop.name.lowercase()}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (hasKeyframes) {
                            Icon(
                                imageVector = Icons.Default.Diamond,
                                contentDescription = null,
                                tint = KeyframeViolet,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Text(
                            text = prop.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) Color.White else Color.LightGray,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Value Readout + Keyframe Navigation + Toggle/Delete KF + Clear Track + Reset Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "${selectedProperty.displayName}: $formattedValue",
                    style = MaterialTheme.typography.labelMedium,
                    color = MotionCyan,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = if (keyframes.isNotEmpty()) KeyframeViolet.copy(alpha = 0.3f) else StudioSurfaceVariant,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (keyframes.isNotEmpty()) "KF ${activeIndex + 1}/${keyframes.size}" else "No KFs",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (keyframes.isNotEmpty()) KeyframeViolet else Color.Gray,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = { onJumpToKeyframe(false) },
                    enabled = keyframes.isNotEmpty(),
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("prev_keyframe_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Previous Keyframe",
                        tint = if (keyframes.isNotEmpty()) Color.White else Color.DarkGray,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Surface(
                    color = if (hasKeyframeAtPlayhead) KeyframeViolet else KeyframeViolet.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (hasKeyframeAtPlayhead) Color.White else KeyframeViolet),
                    modifier = Modifier
                        .clickable { onToggleKeyframe() }
                        .testTag("graph_toggle_keyframe_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Diamond,
                            contentDescription = if (hasKeyframeAtPlayhead) "Remove Keyframe at Playhead" else "Add Keyframe",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (hasKeyframeAtPlayhead) "- KF" else "+ KF",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(
                    onClick = { onJumpToKeyframe(true) },
                    enabled = keyframes.isNotEmpty(),
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("next_keyframe_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next Keyframe",
                        tint = if (keyframes.isNotEmpty()) Color.White else Color.DarkGray,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Delete selected or active keyframe explicitly
                val removableKf = nearbyKeyframe ?: activeKeyframe
                IconButton(
                    onClick = {
                        if (removableKf != null) {
                            onDeleteKeyframe(selectedProperty, removableKf.id)
                            selectedKeyframeId = null
                        }
                    },
                    enabled = removableKf != null,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("delete_keyframe_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Selected Keyframe",
                        tint = if (removableKf != null) CutRose else Color.DarkGray,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = {
                        onClearTrack(selectedProperty)
                        selectedKeyframeId = null
                    },
                    enabled = keyframes.isNotEmpty(),
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("clear_track_keyframe_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear All Keyframes on Property",
                        tint = if (keyframes.isNotEmpty()) CutRose.copy(alpha = 0.85f) else Color.DarkGray,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = { onUpdateValue(selectedProperty, selectedProperty.defaultValue) },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("reset_property_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Property Value",
                        tint = Color.LightGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // 3. Mini Keyframe Scrubber Strip inside Clip Duration (tap diamond to select & seek)
        Surface(
            color = Color(0xFF0A0E16),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, StudioBorder),
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .testTag("keyframe_mini_scrubber")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(clip.id, clip.startTimeMs, clipDuration, keyframes) {
                        detectTapGestures { offset ->
                            val ratio = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            val tappedRelMs = (ratio * clipDuration).toLong()
                            val hitKf = keyframes.minByOrNull { abs(it.timeMs - tappedRelMs) }
                                ?.takeIf { abs(it.timeMs - tappedRelMs) <= (clipDuration * 0.045f).toLong().coerceAtLeast(160L) }
                            if (hitKf != null) {
                                selectedKeyframeId = hitKf.id
                                onSeekInClip(clip.startTimeMs + hitKf.timeMs)
                            } else {
                                onSeekInClip(clip.startTimeMs + tappedRelMs)
                            }
                        }
                    }
                    .pointerInput(clip.id, clip.startTimeMs, clipDuration) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val ratio = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                            val targetMs = clip.startTimeMs + (ratio * clipDuration).toLong()
                            onSeekInClip(targetMs)
                        }
                    }
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val cy = h / 2f

                    drawLine(
                        color = StudioBorder,
                        start = Offset(0f, cy),
                        end = Offset(w, cy),
                        strokeWidth = 3.dp.toPx()
                    )

                    keyframes.forEach { kf ->
                        val ratio = (kf.timeMs.toFloat() / clipDuration.toFloat()).coerceIn(0f, 1f)
                        val kx = ratio * w
                        val isNearPlayhead = abs(kf.timeMs - relTime) <= 160L
                        val isSelectedKf = kf.id == activeKeyframe?.id
                        val d = if (isSelectedKf) 7.5.dp.toPx() else 6.dp.toPx()
                        val diamond = Path().apply {
                            moveTo(kx, cy - d)
                            lineTo(kx + d, cy)
                            lineTo(kx, cy + d)
                            lineTo(kx - d, cy)
                            close()
                        }
                        drawPath(
                            path = diamond,
                            color = when {
                                isNearPlayhead -> PlayheadAmber
                                isSelectedKf -> MotionCyan
                                else -> KeyframeViolet
                            }
                        )
                        drawPath(
                            path = diamond,
                            color = Color.White,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }

                    val playRatio = (relTime.toFloat() / clipDuration.toFloat()).coerceIn(0f, 1f)
                    val px = playRatio * w
                    drawLine(
                        color = PlayheadAmber,
                        start = Offset(px, 2.dp.toPx()),
                        end = Offset(px, h - 2.dp.toPx()),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                Text(
                    text = String.format(Locale.US, "%.2fs", relTime / 1000f),
                    color = PlayheadAmber,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 6.dp)
                )
            }
        }

        // 4. Live Value Adjustment Slider
        Slider(
            value = currentValue.coerceIn(selectedProperty.minVal, selectedProperty.maxVal),
            onValueChange = { newVal -> onUpdateValue(selectedProperty, newVal) },
            valueRange = selectedProperty.minVal..selectedProperty.maxVal,
            colors = SliderDefaults.colors(
                thumbColor = MotionCyan,
                activeTrackColor = MotionCyan,
                inactiveTrackColor = StudioBorder
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .testTag("keyframe_value_slider")
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 4b. Curve Mode Toggle (Cubic Bezier vs Multi-Point Breakpoints)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    color = if (!useBreakpointMode) MotionCyan.copy(alpha = 0.22f) else StudioSurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (!useBreakpointMode) MotionCyan else StudioBorder),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            useBreakpointMode = false
                            if (segmentKeyframe != null) {
                                onUpdateEasing(selectedProperty, segmentKeyframe.id, EasingType.CubicBezier(p1x, p1y, p2x, p2y))
                            }
                        }
                        .testTag("easing_mode_bezier")
                ) {
                    Text(
                        text = "Cubic Bezier",
                        color = if (!useBreakpointMode) MotionCyan else Color.LightGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Surface(
                    color = if (useBreakpointMode) KeyframeViolet.copy(alpha = 0.28f) else StudioSurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (useBreakpointMode) KeyframeViolet else StudioBorder),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            useBreakpointMode = true
                            if (segmentKeyframe != null) {
                                val nextPts = breakpoints.ifEmpty { defaultBreakpoints() }
                                breakpoints = nextPts
                                onUpdateEasing(selectedProperty, segmentKeyframe.id, EasingType.MultiPoint(nextPts))
                            }
                        }
                        .testTag("easing_mode_breakpoints")
                ) {
                    Text(
                        text = "Breakpoints (${breakpoints.size})",
                        color = if (useBreakpointMode) Color.White else Color.LightGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            if (useBreakpointMode) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        color = MotionCyan.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, MotionCyan.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                if (breakpoints.size < 8) {
                                    val newX = ((breakpoints.lastOrNull()?.x ?: 0.25f) + 0.15f).coerceIn(0.08f, 0.92f)
                                    val updated = (breakpoints + CurveBreakpoint(newX, newX)).sortedBy { it.x }
                                    breakpoints = updated
                                    selectedBpIndex = updated.indexOfFirst { abs(it.x - newX) < 0.001f }.coerceAtLeast(0)
                                    if (segmentKeyframe != null) {
                                        onUpdateEasing(selectedProperty, segmentKeyframe.id, EasingType.MultiPoint(updated))
                                    }
                                }
                            }
                            .testTag("add_curve_breakpoint_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Breakpoint", tint = MotionCyan, modifier = Modifier.size(12.dp))
                            Text("Point", color = MotionCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(
                        color = CutRose.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, CutRose.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                if (breakpoints.isNotEmpty()) {
                                    val idx = selectedBpIndex.coerceIn(0, breakpoints.lastIndex)
                                    val updated = breakpoints.filterIndexed { i, _ -> i != idx }
                                    breakpoints = updated
                                    selectedBpIndex = (idx - 1).coerceAtLeast(0)
                                    if (segmentKeyframe != null) {
                                        onUpdateEasing(selectedProperty, segmentKeyframe.id, EasingType.MultiPoint(updated))
                                    }
                                }
                            }
                            .testTag("remove_curve_breakpoint_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Remove Breakpoint", tint = CutRose, modifier = Modifier.size(12.dp))
                            Text("Del", color = CutRose, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 5. Interactive Easing Graph Canvas (Cubic Bezier OR MultiPoint Breakpoints)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(126.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, StudioBorder, RoundedCornerShape(8.dp)),
            color = Color(0xFF070A0F)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .pointerInput(segmentKeyframe?.id, useBreakpointMode) {
                            if (useBreakpointMode) {
                                detectTapGestures { tapOffset ->
                                    val w = size.width.toFloat().coerceAtLeast(1f)
                                    val h = size.height.toFloat().coerceAtLeast(1f)
                                    val tx = (tapOffset.x / w).coerceIn(0.05f, 0.95f)
                                    val ty = (1f - tapOffset.y / h).coerceIn(-0.25f, 1.35f)
                                    val nearest = breakpoints.indices.minByOrNull { i ->
                                        hypot(tx - breakpoints[i].x, ty - breakpoints[i].y)
                                    }
                                    val dist = nearest?.let { hypot(tx - breakpoints[it].x, ty - breakpoints[it].y) } ?: 99f
                                    if (nearest != null && dist < 0.08f) {
                                        selectedBpIndex = nearest
                                    } else if (breakpoints.size < 8) {
                                        val updated = (breakpoints + CurveBreakpoint(tx, ty)).sortedBy { it.x }
                                        breakpoints = updated
                                        selectedBpIndex = updated.indexOfFirst { abs(it.x - tx) < 0.001f }.coerceAtLeast(0)
                                        if (segmentKeyframe != null) {
                                            onUpdateEasing(selectedProperty, segmentKeyframe.id, EasingType.MultiPoint(updated))
                                        }
                                    }
                                }
                            }
                        }
                        .pointerInput(segmentKeyframe?.id, useBreakpointMode) {
                            detectDragGestures(
                                onDragStart = { startOffset ->
                                    val w = size.width.toFloat().coerceAtLeast(1f)
                                    val h = size.height.toFloat().coerceAtLeast(1f)
                                    val touchX = startOffset.x / w
                                    val touchY = 1f - (startOffset.y / h)
                                    if (useBreakpointMode) {
                                        draggingBpIndex = breakpoints.indices.minByOrNull { i ->
                                            hypot(touchX - breakpoints[i].x, touchY - breakpoints[i].y)
                                        } ?: -1
                                        if (draggingBpIndex >= 0) selectedBpIndex = draggingBpIndex
                                    } else {
                                        val dist1 = hypot(touchX - p1x, touchY - p1y)
                                        val dist2 = hypot(touchX - p2x, touchY - p2y)
                                        draggingHandle = if (dist1 <= dist2) 1 else 2
                                    }
                                },
                                onDragEnd = {
                                    draggingHandle = 0
                                    draggingBpIndex = -1
                                },
                                onDragCancel = {
                                    draggingHandle = 0
                                    draggingBpIndex = -1
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val w = size.width.toFloat().coerceAtLeast(1f)
                                    val h = size.height.toFloat().coerceAtLeast(1f)
                                    if (useBreakpointMode && draggingBpIndex in breakpoints.indices) {
                                        val touchX = (change.position.x / w).coerceIn(0.04f, 0.96f)
                                        val touchY = (1f - (change.position.y / h)).coerceIn(-0.25f, 1.35f)
                                        val mutable = breakpoints.toMutableList()
                                        mutable[draggingBpIndex] = CurveBreakpoint(touchX, touchY)
                                        val target = mutable[draggingBpIndex]
                                        val sorted = mutable.sortedBy { it.x }
                                        breakpoints = sorted
                                        draggingBpIndex = sorted.indexOf(target).coerceAtLeast(0)
                                        selectedBpIndex = draggingBpIndex
                                        if (segmentKeyframe != null) {
                                            onUpdateEasing(selectedProperty, segmentKeyframe.id, EasingType.MultiPoint(sorted))
                                        }
                                    } else if (!useBreakpointMode) {
                                        val touchX = (change.position.x / w).coerceIn(0f, 1f)
                                        val touchY = (1f - (change.position.y / h)).coerceIn(-0.5f, 1.8f)
                                        if (draggingHandle == 1) {
                                            p1x = touchX
                                            p1y = touchY
                                        } else {
                                            p2x = touchX
                                            p2y = touchY
                                        }
                                        if (segmentKeyframe != null) {
                                            onUpdateEasing(
                                                selectedProperty,
                                                segmentKeyframe.id,
                                                EasingType.CubicBezier(p1x, p1y, p2x, p2y)
                                            )
                                        }
                                    }
                                }
                            )
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    // Background Grid Lines
                    drawLine(Color(0x22FFFFFF), Offset(0f, 0f), Offset(w, 0f), strokeWidth = 1f)
                    drawLine(Color(0x22FFFFFF), Offset(0f, h / 2f), Offset(w, h / 2f), strokeWidth = 1f)
                    drawLine(Color(0x22FFFFFF), Offset(0f, h), Offset(w, h), strokeWidth = 1f)
                    drawLine(Color(0x22FFFFFF), Offset(w / 2f, 0f), Offset(w / 2f, h), strokeWidth = 1f)
                    drawLine(Color(0x15FFFFFF), Offset(0f, h), Offset(w, 0f), strokeWidth = 1f)

                    val p0 = Offset(0f, h)
                    val p3 = Offset(w, 0f)

                    if (useBreakpointMode) {
                        val samples = 64
                        val path = Path()
                        for (s in 0..samples) {
                            val t = s.toFloat() / samples.toFloat()
                            val yVal = KeyframeEvaluator.evaluateMultiPoint(t, breakpoints)
                            val sx = t * w
                            val sy = h - yVal * h
                            if (s == 0) path.moveTo(sx, sy) else path.lineTo(sx, sy)
                        }
                        drawPath(path, color = PlayheadAmber, style = Stroke(width = 3.dp.toPx()))

                        breakpoints.forEachIndexed { idx, bp ->
                            val pt = Offset(bp.x * w, h - bp.y * h)
                            val active = idx == selectedBpIndex
                            drawCircle(
                                color = if (active) PlayheadAmber else Color.White,
                                radius = if (active) 7.5.dp.toPx() else 6.dp.toPx(),
                                center = pt
                            )
                            drawCircle(
                                color = KeyframeViolet,
                                radius = if (active) 7.5.dp.toPx() else 6.dp.toPx(),
                                center = pt,
                                style = Stroke(2.dp.toPx())
                            )
                        }
                    } else {
                        val p1 = Offset(p1x * w, h - p1y * h)
                        val p2 = Offset(p2x * w, h - p2y * h)

                        drawLine(MotionCyan.copy(alpha = 0.65f), p0, p1, strokeWidth = 2.dp.toPx())
                        drawLine(KeyframeViolet.copy(alpha = 0.65f), p3, p2, strokeWidth = 2.dp.toPx())

                        val curvePath = Path().apply {
                            moveTo(p0.x, p0.y)
                            cubicTo(p1.x, p1y * -h + h, p2.x, p2y * -h + h, p3.x, p3.y)
                        }
                        drawPath(curvePath, color = PlayheadAmber, style = Stroke(width = 3.dp.toPx()))

                        drawCircle(Color.White, 7.dp.toPx(), p1)
                        drawCircle(MotionCyan, 7.dp.toPx(), p1, style = Stroke(2.dp.toPx()))

                        drawCircle(Color.White, 7.dp.toPx(), p2)
                        drawCircle(KeyframeViolet, 7.dp.toPx(), p2, style = Stroke(2.dp.toPx()))
                    }

                    drawCircle(Color.LightGray, 4.dp.toPx(), p0)
                    drawCircle(Color.LightGray, 4.dp.toPx(), p3)
                }

                Text(
                    text = when {
                        keyframes.size < 2 -> "Scrub & tap + KF at 2 points to animate curve"
                        useBreakpointMode -> "Tap curve to add breakpoint • Drag dots to shape speed"
                        else -> String.format(Locale.US, "P1: (%.2f, %.2f)  P2: (%.2f, %.2f)", p1x, p1y, p2x, p2y)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (keyframes.size < 2) PlayheadAmber.copy(alpha = 0.85f) else Color.Gray,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 6. Curve Preset Buttons
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(CURVE_PRESETS) { preset ->
                Surface(
                    color = StudioSurfaceVariant,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, StudioBorder),
                    modifier = Modifier.clickable {
                        useBreakpointMode = false
                        p1x = preset.p1x
                        p1y = preset.p1y
                        p2x = preset.p2x
                        p2y = preset.p2y
                        if (segmentKeyframe != null) {
                            onUpdateEasing(
                                selectedProperty,
                                segmentKeyframe.id,
                                EasingType.CubicBezier(p1x, p1y, p2x, p2y)
                            )
                        }
                    }
                ) {
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}
