package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EffectCatalog
import com.example.model.EffectType
import com.example.model.VideoEffect
import com.example.ui.theme.CutRose
import com.example.ui.theme.KeyframeViolet
import com.example.ui.theme.MotionCyan
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioDarkBg
import com.example.ui.theme.StudioSurfaceVariant

@Composable
fun EffectsStackEditor(
    effects: List<VideoEffect>,
    onAddEffect: (EffectType) -> Unit,
    onUpdateEffect: (VideoEffect) -> Unit,
    onDeleteEffect: (String) -> Unit,
    onOpenEffectLibrary: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val totalCount = EffectCatalog.allPresets.size

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioDarkBg)
            .padding(10.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "FX Stack (${effects.size})",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Surface(
                color = KeyframeViolet.copy(alpha = 0.22f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, KeyframeViolet),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onOpenEffectLibrary() }
                    .testTag("open_fx_library_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Browse 2000+ Effects",
                        tint = KeyframeViolet,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Browse FX ($totalCount+)",
                        style = MaterialTheme.typography.labelSmall,
                        color = KeyframeViolet,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Effects List
        if (effects.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(StudioSurfaceVariant)
                    .clickable { onOpenEffectLibrary() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No effects applied yet.\nTap '+ Browse FX ($totalCount+)' to browse Alight Motion, Gold Motion, Ultra Motion, Node Video & CapCut effects.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(effects, key = { it.id }) { fx ->
                    EffectCard(
                        effect = fx,
                        onUpdate = onUpdateEffect,
                        onDelete = { onDeleteEffect(fx.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EffectCard(
    effect: VideoEffect,
    onUpdate: (VideoEffect) -> Unit,
    onDelete: () -> Unit
) {
    val accent = effect.suite?.let { Color(it.accentHex) } ?: MotionCyan

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, StudioBorder, RoundedCornerShape(8.dp)),
        color = StudioSurfaceVariant
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Switch(
                        checked = effect.isEnabled,
                        onCheckedChange = { onUpdate(effect.copy(isEnabled = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = accent,
                            checkedTrackColor = accent.copy(alpha = 0.3f),
                            uncheckedThumbColor = Color.DarkGray,
                            uncheckedTrackColor = StudioDarkBg
                        ),
                        modifier = Modifier.size(width = 40.dp, height = 24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = effect.effectiveName,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${effect.suite?.displayName ?: "Studio"} • ${effect.effectiveCategory}",
                            color = accent,
                            fontSize = 10.sp
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove Effect",
                        tint = CutRose,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Intensity slider
            EffectSliderRow(
                label = "Mix",
                value = effect.intensity,
                range = 0f..1f,
                accent = accent,
                display = "${(effect.intensity * 100).toInt()}%",
                onValueChange = { onUpdate(effect.copy(intensity = it)) }
            )

            // Speed slider
            EffectSliderRow(
                label = "Speed",
                value = effect.speed,
                range = 0.1f..3.0f,
                accent = accent,
                display = String.format("%.1fx", effect.speed),
                onValueChange = { onUpdate(effect.copy(speed = it)) }
            )

            // Secondary param slider
            EffectSliderRow(
                label = effect.paramALabel,
                value = effect.paramA,
                range = 0f..1f,
                accent = accent,
                display = "${(effect.paramA * 100).toInt()}%",
                onValueChange = { onUpdate(effect.copy(paramA = it)) }
            )
        }
    }
}

@Composable
private fun EffectSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    accent: Color,
    display: String,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            fontSize = 10.sp,
            modifier = Modifier.size(width = 56.dp, height = 14.dp)
        )
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = StudioBorder
            ),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = display,
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            fontSize = 10.sp,
            modifier = Modifier.size(width = 34.dp, height = 14.dp)
        )
    }
}
