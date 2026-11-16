package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AnimatableProperty
import com.example.model.ClipItem
import com.example.model.Project
import com.example.model.Track
import com.example.ui.theme.CutRose
import com.example.ui.theme.KeyframeViolet
import com.example.ui.theme.MotionCyan
import com.example.ui.theme.PlayheadAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioDarkBg
import com.example.ui.theme.StudioDivider
import com.example.ui.theme.StudioSurfaceVariant

@Composable
fun TimelineView(
    project: Project,
    currentTimeMs: Long,
    selectedClipId: String?,
    activeProperty: AnimatableProperty,
    canUndo: Boolean = false,
    canRedo: Boolean = false,
    onUndo: () -> Unit = {},
    onRedo: () -> Unit = {},
    onSeek: (Long) -> Unit,
    onSelectClip: (String?) -> Unit,
    onSelectProperty: (AnimatableProperty) -> Unit = {},
    onSplitClip: (String, Long) -> Unit,
    onDuplicateClip: (String) -> Unit,
    onDeleteClip: (String) -> Unit,
    onToggleKeyframe: (String) -> Unit,
    onAddClipRequested: () -> Unit,
    onRecordVoiceOverRequested: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var showPropMenu by remember { mutableStateOf(false) }
    val pxPerSecond = 80f // 80dp per second zoom level
    val totalWidthDp = (project.durationMs / 1000f * pxPerSecond).coerceAtLeast(360f).dp

    // Selected clip details
    val selectedClip = selectedClipId?.let { project.findClip(it) }
    val isMoveActive = activeProperty == AnimatableProperty.POSITION_X || activeProperty == AnimatableProperty.POSITION_Y
    val activePropertyLabel = if (isMoveActive) "Move" else activeProperty.displayName
    val hasKeyframeAtPlayhead = if (isMoveActive) {
        selectedClip?.hasKeyframeAt(AnimatableProperty.POSITION_X, currentTimeMs) == true ||
            selectedClip?.hasKeyframeAt(AnimatableProperty.POSITION_Y, currentTimeMs) == true
    } else {
        selectedClip?.hasKeyframeAt(activeProperty, currentTimeMs) == true
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioDarkBg)
    ) {
        // Quick Action Bar (CapCut / Alight Motion split, keyframe diamond, duplicate, delete, undo, redo)
        Surface(
            color = StudioSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .border(1.dp, StudioBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Undo Button
                    IconButton(
                        onClick = onUndo,
                        enabled = canUndo,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("timeline_undo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Undo,
                            contentDescription = "Undo",
                            tint = if (canUndo) Color.White else Color.DarkGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Redo Button
                    IconButton(
                        onClick = onRedo,
                        enabled = canRedo,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("timeline_redo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Redo,
                            contentDescription = "Redo",
                            tint = if (canRedo) Color.White else Color.DarkGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Split Button
                    IconButton(
                        onClick = {
                            if (selectedClipId != null) {
                                onSplitClip(selectedClipId, currentTimeMs)
                            }
                        },
                        enabled = selectedClipId != null,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("split_clip_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallSplit,
                            contentDescription = "Split Clip at Playhead",
                            tint = if (selectedClipId != null) MotionCyan else Color.DarkGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Keyframe Diamond Button + Property Selector
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                color = if (hasKeyframeAtPlayhead) KeyframeViolet else KeyframeViolet.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(18.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (hasKeyframeAtPlayhead) Color.White else KeyframeViolet
                                ),
                                modifier = Modifier
                                    .clickable(enabled = selectedClipId != null) {
                                        if (selectedClipId != null) {
                                            onToggleKeyframe(selectedClipId)
                                        }
                                    }
                                    .testTag("keyframe_diamond_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Diamond,
                                        contentDescription = "Add or Remove Keyframe",
                                        tint = if (hasKeyframeAtPlayhead) Color.White else KeyframeViolet,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (hasKeyframeAtPlayhead) "$activePropertyLabel ●" else "+ $activePropertyLabel",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (hasKeyframeAtPlayhead) Color.White else KeyframeViolet,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Surface(
                                color = StudioDarkBg,
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
                                modifier = Modifier
                                    .clickable { showPropMenu = true }
                                    .testTag("timeline_keyframe_property_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Choose Keyframe Property",
                                    tint = KeyframeViolet,
                                    modifier = Modifier
                                        .padding(2.dp)
                                        .size(18.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showPropMenu,
                            onDismissRequest = { showPropMenu = false }
                        ) {
                            AnimatableProperty.entries
                                .filter { it != AnimatableProperty.POSITION_Y }
                                .forEach { prop ->
                                    val label = if (prop == AnimatableProperty.POSITION_X) "Move" else prop.displayName
                                    val isCur = if (prop == AnimatableProperty.POSITION_X) isMoveActive else prop == activeProperty
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = label,
                                                fontWeight = if (isCur) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isCur) KeyframeViolet else Color.Unspecified
                                            )
                                        },
                                        onClick = {
                                            onSelectProperty(prop)
                                            showPropMenu = false
                                        }
                                    )
                                }
                        }
                    }

                    // Duplicate Button
                    IconButton(
                        onClick = {
                            if (selectedClipId != null) {
                                onDuplicateClip(selectedClipId)
                            }
                        },
                        enabled = selectedClipId != null,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("duplicate_clip_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Duplicate Clip",
                            tint = if (selectedClipId != null) Color.LightGray else Color.DarkGray,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Delete Button
                    IconButton(
                        onClick = {
                            if (selectedClipId != null) {
                                onDeleteClip(selectedClipId)
                            }
                        },
                        enabled = selectedClipId != null,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("delete_clip_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Clip",
                            tint = if (selectedClipId != null) CutRose else Color.DarkGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Record Voice-over Action Button
                    Surface(
                        color = CutRose.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CutRose.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .clickable { onRecordVoiceOverRequested() }
                            .testTag("open_voiceover_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Record Voice-over",
                                tint = CutRose,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Voiceover",
                                style = MaterialTheme.typography.labelSmall,
                                color = CutRose,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Add Clip / Track action
                    Surface(
                        color = MotionCyan.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MotionCyan.copy(alpha = 0.4f)),
                        modifier = Modifier.clickable { onAddClipRequested() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Layer / Clip",
                                tint = MotionCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Add Layer",
                                style = MaterialTheme.typography.labelSmall,
                                color = MotionCyan,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Timeline Scroll Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .horizontalScroll(scrollState)
        ) {
            Column(modifier = Modifier.width(totalWidthDp)) {
                // 1. Time Ruler Bar
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .background(StudioDarkBg)
                        .pointerInput(project.durationMs) {
                            detectTapGestures { offset ->
                                val timeMs = (offset.x / (pxPerSecond * density) * 1000f).toLong()
                                onSeek(timeMs.coerceIn(0L, project.durationMs))
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    val pxSec = pxPerSecond * density
                    val totalSec = (project.durationMs / 1000).toInt()

                    drawLine(StudioBorder, Offset(0f, h), Offset(w, h), strokeWidth = 1f)

                    for (sec in 0..totalSec) {
                        val x = sec * pxSec
                        // Major tick
                        drawLine(
                            color = Color(0x88FFFFFF),
                            start = Offset(x, h - 10.dp.toPx()),
                            end = Offset(x, h),
                            strokeWidth = 1.5f
                        )
                        // Sub-frame ticks
                        for (sub in 1..3) {
                            val subX = x + (sub / 4f) * pxSec
                            if (subX < w) {
                                drawLine(
                                    color = Color(0x33FFFFFF),
                                    start = Offset(subX, h - 5.dp.toPx()),
                                    end = Offset(subX, h),
                                    strokeWidth = 1f
                                )
                            }
                        }
                    }
                }

                // 2. Track Rows
                project.tracks.forEach { track ->
                    TrackRow(
                        track = track,
                        currentTimeMs = currentTimeMs,
                        activeProperty = activeProperty,
                        pxPerSecond = pxPerSecond,
                        selectedClipId = selectedClipId,
                        onSelectClip = onSelectClip,
                        onSeek = onSeek,
                        onSelectProperty = onSelectProperty
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }

            // 3. Playhead Indicator Line
            val playheadXDp = (currentTimeMs / 1000f * pxPerSecond).dp
            Box(
                modifier = Modifier
                    .offset(x = playheadXDp - 7.dp)
                    .width(14.dp)
                    .fillMaxHeight()
                    .pointerInput(project.durationMs) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaMs = (dragAmount.x / (pxPerSecond * density) * 1000f).toLong()
                            onSeek((currentTimeMs + deltaMs).coerceIn(0L, project.durationMs))
                        }
                    }
            ) {
                // Amber Diamond marker on top
                Canvas(
                    modifier = Modifier
                        .size(14.dp)
                        .align(Alignment.TopCenter)
                ) {
                    val path = Path().apply {
                        moveTo(size.width / 2f, 0f)
                        lineTo(size.width, size.height / 2f)
                        lineTo(size.width / 2f, size.height)
                        lineTo(0f, size.height / 2f)
                        close()
                    }
                    drawPath(path, color = PlayheadAmber)
                }

                // Playhead line
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .align(Alignment.TopCenter)
                        .background(PlayheadAmber)
                )
            }
        }
    }
}

@Composable
private fun TrackRow(
    track: Track,
    currentTimeMs: Long,
    activeProperty: AnimatableProperty,
    pxPerSecond: Float,
    selectedClipId: String?,
    onSelectClip: (String?) -> Unit,
    onSeek: (Long) -> Unit,
    onSelectProperty: (AnimatableProperty) -> Unit
) {
    Surface(
        color = Color(track.type.colorHex).copy(alpha = 0.25f),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .border(1.dp, StudioDivider)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            track.clips.forEach { clip ->
                val clipStartXDp = (clip.startTimeMs / 1000f * pxPerSecond).dp
                val clipWidthDp = (clip.durationMs / 1000f * pxPerSecond).coerceAtLeast(40f).dp
                val isSelected = clip.id == selectedClipId

                Surface(
                    modifier = Modifier
                        .offset(x = clipStartXDp)
                        .width(clipWidthDp)
                        .height(48.dp)
                        .padding(vertical = 2.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MotionCyan else Color(track.type.colorHex),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable {
                            onSelectClip(if (isSelected) null else clip.id)
                        },
                    color = Color(track.type.colorHex).copy(alpha = 0.85f)
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 2.dp)) {
                        // Mini Waveform Background for Audio / Voiceover clips
                        if (clip.waveformSamples.isNotEmpty()) {
                            Canvas(modifier = Modifier.fillMaxSize().padding(top = 16.dp, bottom = 4.dp)) {
                                val w = size.width
                                val h = size.height
                                val samples = clip.waveformSamples
                                val step = w / samples.size.toFloat()
                                for (i in samples.indices) {
                                    val amp = samples[i].coerceIn(0.1f, 1.0f)
                                    val barH = amp * h
                                    val x = i * step + step / 2f
                                    drawLine(
                                        color = Color.White.copy(alpha = 0.45f),
                                        start = Offset(x, h - barH),
                                        end = Offset(x, h),
                                        strokeWidth = (step * 0.7f).coerceIn(1.5f, 4f)
                                    )
                                }
                            }
                        }

                        // Title
                        Text(
                            text = clip.title,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            maxLines = 1,
                            modifier = Modifier.align(Alignment.TopStart)
                        )

                        // Duration & speed badge
                        val speedLabel = buildString {
                            append(String.format(java.util.Locale.US, "%.1fs", clip.durationMs / 1000f))
                            if (kotlin.math.abs(clip.speed - 1.0f) > 0.02f) {
                                append(String.format(java.util.Locale.US, " • %.2gx", clip.speed))
                            }
                            if (clip.isReversed) {
                                append(" REV")
                            }
                        }
                        Text(
                            text = speedLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (clip.speed != 1.0f || clip.isReversed) PlayheadAmber else Color.LightGray,
                            fontSize = 9.sp,
                            modifier = Modifier.align(Alignment.BottomStart)
                        )

                        // Keyframe Diamonds drawn on clip (tap to seek directly to keyframe!)
                        clip.keyframeTracks.forEach { (prop, kTrack) ->
                            kTrack.keyframes.forEach { kf ->
                                val kfOffsetXDp = (kf.timeMs / 1000f * pxPerSecond).dp
                                val kfAbsTime = clip.startTimeMs + kf.timeMs
                                val isAtPlayhead = kotlin.math.abs(kfAbsTime - currentTimeMs) <= 160L
                                val diamondColor = when {
                                    isAtPlayhead -> PlayheadAmber
                                    prop == activeProperty -> Color.White
                                    else -> KeyframeViolet
                                }
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .offset(x = kfOffsetXDp - 12.dp)
                                        .size(24.dp)
                                        .align(Alignment.CenterStart)
                                        .clickable {
                                            onSelectClip(clip.id)
                                            onSelectProperty(prop)
                                            onSeek(kfAbsTime)
                                        }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Diamond,
                                        contentDescription = "Keyframe at ${kf.timeMs}ms",
                                        tint = diamondColor,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }

                        // Trim handles if selected
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .fillMaxHeight()
                                    .background(MotionCyan)
                                    .align(Alignment.CenterStart)
                            )
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .fillMaxHeight()
                                    .background(MotionCyan)
                                    .align(Alignment.CenterEnd)
                            )
                        }
                    }
                }
            }
        }
    }
}
