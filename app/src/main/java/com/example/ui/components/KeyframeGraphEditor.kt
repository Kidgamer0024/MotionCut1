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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Refresh
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
import com.example.model.EasingType
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
        null -> EasingType.CubicBezier(0.42f, 0.0f, 0.58f, 1.0f)
    }
}

@Composable
fun KeyframeGraphEditor(
    clip: ClipItem,
    currentTimeMs: Long,
    selectedProperty: AnimatableProperty,
    onSelectProperty: (AnimatableProperty) -> Unit,
    onToggleKeyframe: () -> Unit = {},
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

    val hasKeyframeAtPlayhead = keyframes.any { abs(it.timeMs - relTime) <= 80L }

    // Find active keyframe at/before playhead, and outgoing segment keyframe for curve editing
    val activeIndex = keyframes.indexOfLast { it.timeMs <= relTime + 40L }.coerceAtLeast(0)
    val activeKeyframe = keyframes.getOrNull(activeIndex)
    val segmentKeyframe = if (keyframes.size >= 2 && activeIndex >= keyframes.lastIndex) {
        keyframes[keyframes.lastIndex - 1]
    } else {
        activeKeyframe
    }

    val currentBezier = easingToBezier(segmentKeyframe?.easing)
    var p1x by remember(segmentKeyframe?.id, currentBezier) { mutableStateOf(currentBezier.p1x) }
    var p1y by remember(segmentKeyframe?.id, currentBezier) { mutableStateOf(currentBezier.p1y) }
    var p2x by remember(segmentKeyframe?.id, currentBezier) { mutableStateOf(currentBezier.p2x) }
    var p2y by remember(segmentKeyframe?.id, currentBezier) { mutableStateOf(currentBezier.p2y) }
    var draggingHandle by remember { mutableIntStateOf(0) } // 1 = P1, 2 = P2

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

        // 2. Value Readout + Keyframe Navigation + Toggle KF + Reset Row
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
                horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                            contentDescription = if (hasKeyframeAtPlayhead) "Remove Keyframe" else "Add Keyframe",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (hasKeyframeAtPlayhead) "KF ●" else "+ KF",
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

        // 3. Mini Keyframe Scrubber Strip inside Clip Duration
        Surface(
            color = Color(0xFF0A0E16),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, StudioBorder),
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .testTag("keyframe_mini_scrubber")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(clip.id, clip.startTimeMs, clipDuration) {
                        detectTapGestures { offset ->
                            val ratio = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            val targetMs = clip.startTimeMs + (ratio * clipDuration).toLong()
                            onSeekInClip(targetMs)
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

                    // Track rail
                    drawLine(
                        color = StudioBorder,
                        start = Offset(0f, cy),
                        end = Offset(w, cy),
                        strokeWidth = 3.dp.toPx()
                    )

                    // Draw keyframe diamonds along clip
                    keyframes.forEach { kf ->
                        val ratio = (kf.timeMs.toFloat() / clipDuration.toFloat()).coerceIn(0f, 1f)
                        val kx = ratio * w
                        val isNearPlayhead = abs(kf.timeMs - relTime) <= 80L
                        val d = 6.dp.toPx()
                        val diamond = Path().apply {
                            moveTo(kx, cy - d)
                            lineTo(kx + d, cy)
                            lineTo(kx, cy + d)
                            lineTo(kx - d, cy)
                            close()
                        }
                        drawPath(
                            path = diamond,
                            color = if (isNearPlayhead) PlayheadAmber else KeyframeViolet
                        )
                        drawPath(
                            path = diamond,
                            color = Color.White,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }

                    // Playhead marker
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

        // 4. Live Value Adjustment Slider (updates keyframe or auto-keyframes when track has keyframes)
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

        // 5. Interactive Cubic Bezier Graph Canvas
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(118.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, StudioBorder, RoundedCornerShape(8.dp)),
            color = Color(0xFF070A0F)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .pointerInput(segmentKeyframe?.id) {
                            detectDragGestures(
                                onDragStart = { startOffset ->
                                    val w = size.width.toFloat().coerceAtLeast(1f)
                                    val h = size.height.toFloat().coerceAtLeast(1f)
                                    val touchX = startOffset.x / w
                                    val touchY = 1f - (startOffset.y / h)
                                    val dist1 = hypot(touchX - p1x, touchY - p1y)
                                    val dist2 = hypot(touchX - p2x, touchY - p2y)
                                    draggingHandle = if (dist1 <= dist2) 1 else 2
                                },
                                onDragEnd = { draggingHandle = 0 },
                                onDragCancel = { draggingHandle = 0 },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val w = size.width.toFloat().coerceAtLeast(1f)
                                    val h = size.height.toFloat().coerceAtLeast(1f)
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

                    // Diagonal reference line
                    drawLine(Color(0x15FFFFFF), Offset(0f, h), Offset(w, 0f), strokeWidth = 1f)

                    val p0 = Offset(0f, h)
                    val p3 = Offset(w, 0f)
                    val p1 = Offset(p1x * w, h - p1y * h)
                    val p2 = Offset(p2x * w, h - p2y * h)

                    // Tangent Handle Stalks
                    drawLine(MotionCyan.copy(alpha = 0.65f), p0, p1, strokeWidth = 2.dp.toPx())
                    drawLine(KeyframeViolet.copy(alpha = 0.65f), p3, p2, strokeWidth = 2.dp.toPx())

                    // Cubic Bezier Curve
                    val curvePath = Path().apply {
                        moveTo(p0.x, p0.y)
                        cubicTo(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y)
                    }
                    drawPath(curvePath, color = PlayheadAmber, style = Stroke(width = 3.dp.toPx()))

                    // Control Handles
                    drawCircle(Color.White, 7.dp.toPx(), p1)
                    drawCircle(MotionCyan, 7.dp.toPx(), p1, style = Stroke(2.dp.toPx()))

                    drawCircle(Color.White, 7.dp.toPx(), p2)
                    drawCircle(KeyframeViolet, 7.dp.toPx(), p2, style = Stroke(2.dp.toPx()))

                    // Endpoints
                    drawCircle(Color.LightGray, 4.dp.toPx(), p0)
                    drawCircle(Color.LightGray, 4.dp.toPx(), p3)
                }

                Text(
                    text = if (keyframes.size < 2) {
                        "Scrub & tap + KF at 2 points to animate curve"
                    } else {
                        String.format(Locale.US, "P1: (%.2f, %.2f)  P2: (%.2f, %.2f)", p1x, p1y, p2x, p2y)
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
