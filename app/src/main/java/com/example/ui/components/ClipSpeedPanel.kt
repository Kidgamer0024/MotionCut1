package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ProjectMappers
import com.example.model.ClipItem
import com.example.model.ClipType
import com.example.ui.theme.CutRose
import com.example.ui.theme.KeyframeViolet
import com.example.ui.theme.MotionCyan
import com.example.ui.theme.PlayheadAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioDarkBg
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.SuccessEmerald
import java.util.Locale
import kotlin.math.abs

private data class SpeedPreset(val label: String, val speed: Float)

private val SPEED_PRESETS = listOf(
    SpeedPreset("0.25x", 0.25f),
    SpeedPreset("0.5x", 0.5f),
    SpeedPreset("0.75x", 0.75f),
    SpeedPreset("1.0x", 1.0f),
    SpeedPreset("1.25x", 1.25f),
    SpeedPreset("1.5x", 1.5f),
    SpeedPreset("2.0x", 2.0f),
    SpeedPreset("3.0x", 3.0f),
    SpeedPreset("4.0x", 4.0f)
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClipSpeedPanel(
    selectedClip: ClipItem?,
    onApplySpeed: (speed: Float, preserveAudioPitch: Boolean, isReversed: Boolean, adjustDuration: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    if (selectedClip == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(StudioDarkBg)
                .padding(16.dp)
                .testTag("clip_speed_empty_state"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = "Clip Speed",
                    tint = MotionCyan,
                    modifier = Modifier.size(34.dp)
                )
                Text(
                    text = "Select any clip (video, audio, text, 3D) to customize playback speed",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        return
    }

    var draftSpeed by remember(selectedClip.id, selectedClip.speed) {
        mutableFloatStateOf(selectedClip.speed.coerceIn(0.25f, 4.0f))
    }
    var preservePitch by remember(selectedClip.id, selectedClip.preserveAudioPitch) {
        mutableStateOf(selectedClip.preserveAudioPitch)
    }
    var isReversed by remember(selectedClip.id, selectedClip.isReversed) {
        mutableStateOf(selectedClip.isReversed)
    }
    var adjustDuration by remember(selectedClip.id) {
        mutableStateOf(true)
    }

    val safeOldSpeed = selectedClip.speed.coerceIn(0.25f, 4.0f)
    val safeNewSpeed = draftSpeed.coerceIn(0.25f, 4.0f)
    val projectedDurationMs = if (adjustDuration) {
        ((selectedClip.durationMs * (safeOldSpeed / safeNewSpeed)).toLong()).coerceIn(250L, 120_000L)
    } else {
        selectedClip.durationMs
    }
    val hasAudioStream = selectedClip.type == ClipType.AUDIO || selectedClip.type == ClipType.VIDEO_SAMPLE

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(StudioDarkBg)
            .padding(10.dp)
            .testTag("clip_speed_panel"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header & Live Duration Readout
        Surface(
            color = StudioCardBg,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, StudioBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (hasAudioStream) Icons.Default.Audiotrack else Icons.Default.Speed,
                            contentDescription = null,
                            tint = if (hasAudioStream) SuccessEmerald else MotionCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = selectedClip.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = if (hasAudioStream) {
                            "${selectedClip.type.label} • Audio & Video Timing"
                        } else {
                            "${selectedClip.type.label} • Layer Timing & Motion Speed"
                        },
                        color = if (hasAudioStream) SuccessEmerald else MotionCyan,
                        fontSize = 10.sp
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format(Locale.US, "%.2fx", safeNewSpeed),
                        color = PlayheadAmber,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "${ProjectMappers.formatDuration(selectedClip.durationMs)} -> ${ProjectMappers.formatDuration(projectedDurationMs)}",
                        color = Color.LightGray,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Preset Multiplier Chips
        Text("Speed Presets", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SPEED_PRESETS.forEach { preset ->
                val isSelected = abs(draftSpeed - preset.speed) < 0.02f
                Surface(
                    color = if (isSelected) MotionCyan.copy(alpha = 0.22f) else StudioSurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (isSelected) MotionCyan else StudioBorder),
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            draftSpeed = preset.speed
                            onApplySpeed(preset.speed, preservePitch, isReversed, adjustDuration)
                        }
                        .testTag("speed_preset_${preset.label}")
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = preset.label,
                            color = if (isSelected) MotionCyan else Color.White,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Fine Speed Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Speed", color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.width(46.dp))
            Slider(
                value = draftSpeed,
                onValueChange = { draftSpeed = ((it * 20f).toInt() / 20f).coerceIn(0.25f, 4.0f) },
                valueRange = 0.25f..4.0f,
                colors = SliderDefaults.colors(
                    thumbColor = MotionCyan,
                    activeTrackColor = MotionCyan,
                    inactiveTrackColor = StudioBorder
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("clip_speed_slider")
            )
            Text(
                text = String.format(Locale.US, "%.2fx", draftSpeed),
                color = MotionCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(42.dp)
            )
        }

        // Audio Pitch, Reverse Playback, Auto-Duration Controls
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SpeedToggleRow(
                title = "Preserve Audio Pitch",
                subtitle = if (hasAudioStream) {
                    "Keeps voice & music pitch natural across speed changes"
                } else {
                    "Saved with clip audio metadata"
                },
                checked = preservePitch,
                accentColor = SuccessEmerald,
                testTag = "preserve_pitch_switch",
                onCheckedChange = {
                    preservePitch = it
                    onApplySpeed(draftSpeed, it, isReversed, adjustDuration)
                }
            )
            SpeedToggleRow(
                title = "Reverse Playback",
                subtitle = "Plays frames, animations, and audio backward",
                checked = isReversed,
                accentColor = CutRose,
                testTag = "reverse_clip_switch",
                onCheckedChange = {
                    isReversed = it
                    onApplySpeed(draftSpeed, preservePitch, it, adjustDuration)
                }
            )
            SpeedToggleRow(
                title = "Auto-Adjust Clip Duration",
                subtitle = "Scales timeline clip length inversely with speed",
                checked = adjustDuration,
                accentColor = KeyframeViolet,
                testTag = "adjust_duration_switch",
                onCheckedChange = { adjustDuration = it }
            )
        }

        // Commit Speed Button (for slider adjustments)
        Button(
            onClick = { onApplySpeed(draftSpeed, preservePitch, isReversed, adjustDuration) },
            colors = ButtonDefaults.buttonColors(
                containerColor = MotionCyan,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("apply_clip_speed_button")
        ) {
            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Apply ${String.format(Locale.US, "%.2fx", draftSpeed)} Speed & Timing",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun SpeedToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    accentColor: Color,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        color = StudioSurfaceVariant,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, StudioBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Color.Gray, fontSize = 10.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedThumbColor = accentColor),
                modifier = Modifier.testTag(testTag)
            )
        }
    }
}
