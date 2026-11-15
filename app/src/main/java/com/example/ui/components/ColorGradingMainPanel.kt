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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ColorGradingSettings
import com.example.model.LutPreset
import com.example.ui.theme.MotionCyan
import com.example.ui.theme.PlayheadAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioDarkBg
import com.example.ui.theme.StudioSurfaceVariant

enum class GradingSubTab(val label: String) {
    WHEELS("3-Way Wheels"),
    PRIMARIES("Primaries"),
    CURVES("RGB Curves"),
    HSL("HSL Selective"),
    LUTS("Cinematic LUTs")
}

@Composable
fun ColorGradingMainPanel(
    settings: ColorGradingSettings,
    onUpdateSettings: (ColorGradingSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    var subTab by remember { mutableStateOf(GradingSubTab.WHEELS) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioDarkBg)
    ) {
        // Sub-tabs row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(GradingSubTab.values()) { tab ->
                val isSelected = tab == subTab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) MotionCyan.copy(alpha = 0.25f) else StudioSurfaceVariant)
                        .border(
                            1.dp,
                            if (isSelected) MotionCyan else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { subTab = tab }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MotionCyan else Color.LightGray,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Tab Content
        when (subTab) {
            GradingSubTab.WHEELS -> {
                ColorWheelsEditor(
                    lift = settings.lift,
                    gamma = settings.gamma,
                    gain = settings.gain,
                    offset = settings.offset,
                    onUpdateLift = { onUpdateSettings(settings.copy(lift = it)) },
                    onUpdateGamma = { onUpdateSettings(settings.copy(gamma = it)) },
                    onUpdateGain = { onUpdateSettings(settings.copy(gain = it)) },
                    onUpdateOffset = { onUpdateSettings(settings.copy(offset = it)) }
                )
            }
            GradingSubTab.PRIMARIES -> {
                PrimariesSlidersView(
                    settings = settings,
                    onUpdateSettings = onUpdateSettings
                )
            }
            GradingSubTab.CURVES -> {
                ColorCurvesEditor(
                    masterCurve = settings.masterCurve,
                    redCurve = settings.redCurve,
                    greenCurve = settings.greenCurve,
                    blueCurve = settings.blueCurve,
                    onUpdateCurve = { channel, curve ->
                        val updated = when (channel) {
                            CurveChannel.MASTER -> settings.copy(masterCurve = curve)
                            CurveChannel.RED -> settings.copy(redCurve = curve)
                            CurveChannel.GREEN -> settings.copy(greenCurve = curve)
                            CurveChannel.BLUE -> settings.copy(blueCurve = curve)
                        }
                        onUpdateSettings(updated)
                    }
                )
            }
            GradingSubTab.HSL -> {
                HslSecondaryEditor(
                    adjustments = settings.hslAdjustments,
                    onUpdateAdjustment = { band, adj ->
                        val updatedMap = settings.hslAdjustments.toMutableMap()
                        updatedMap[band] = adj
                        onUpdateSettings(settings.copy(hslAdjustments = updatedMap))
                    }
                )
            }
            GradingSubTab.LUTS -> {
                LutPresetsView(
                    activePreset = settings.lutPreset,
                    intensity = settings.lutIntensity,
                    onSelectPreset = { preset -> onUpdateSettings(settings.copy(lutPreset = preset)) },
                    onUpdateIntensity = { intensity -> onUpdateSettings(settings.copy(lutIntensity = intensity)) }
                )
            }
        }
    }
}

@Composable
private fun PrimariesSlidersView(
    settings: ColorGradingSettings,
    onUpdateSettings: (ColorGradingSettings) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .padding(horizontal = 10.dp)
    ) {
        item {
            PrimarySliderRow(
                label = "Exposure",
                value = settings.exposure,
                valueRange = -2.0f..2.0f,
                formattedValue = String.format("%+.2f EV", settings.exposure),
                onValueChange = { onUpdateSettings(settings.copy(exposure = it)) }
            )
        }
        item {
            PrimarySliderRow(
                label = "Contrast",
                value = settings.contrast,
                valueRange = 0.5f..2.0f,
                formattedValue = String.format("%.2f", settings.contrast),
                onValueChange = { onUpdateSettings(settings.copy(contrast = it)) }
            )
        }
        item {
            PrimarySliderRow(
                label = "Temp (Warm)",
                value = settings.temperature,
                valueRange = -100f..100f,
                formattedValue = "${settings.temperature.toInt()}",
                onValueChange = { onUpdateSettings(settings.copy(temperature = it)) }
            )
        }
        item {
            PrimarySliderRow(
                label = "Tint (Green/Mag)",
                value = settings.tint,
                valueRange = -100f..100f,
                formattedValue = "${settings.tint.toInt()}",
                onValueChange = { onUpdateSettings(settings.copy(tint = it)) }
            )
        }
        item {
            PrimarySliderRow(
                label = "Shadows",
                value = settings.shadows,
                valueRange = -100f..100f,
                formattedValue = "${settings.shadows.toInt()}",
                onValueChange = { onUpdateSettings(settings.copy(shadows = it)) }
            )
        }
        item {
            PrimarySliderRow(
                label = "Saturation",
                value = settings.saturation,
                valueRange = 0.0f..2.0f,
                formattedValue = String.format("%.2f", settings.saturation),
                onValueChange = { onUpdateSettings(settings.copy(saturation = it)) }
            )
        }
        item {
            PrimarySliderRow(
                label = "Vignette",
                value = settings.vignette,
                valueRange = 0f..100f,
                formattedValue = "${settings.vignette.toInt()}%",
                onValueChange = { onUpdateSettings(settings.copy(vignette = it)) }
            )
        }
    }
}

@Composable
private fun PrimarySliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    formattedValue: String,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(36.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.LightGray,
            modifier = Modifier.size(width = 85.dp, height = 18.dp),
            fontSize = 11.sp
        )

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = MotionCyan,
                activeTrackColor = MotionCyan,
                inactiveTrackColor = StudioBorder
            ),
            modifier = Modifier.weight(1f)
        )

        Text(
            text = formattedValue,
            style = MaterialTheme.typography.labelSmall,
            color = MotionCyan,
            modifier = Modifier.size(width = 54.dp, height = 18.dp),
            fontSize = 11.sp
        )
    }
}

@Composable
private fun LutPresetsView(
    activePreset: LutPreset,
    intensity: Float,
    onSelectPreset: (LutPreset) -> Unit,
    onUpdateIntensity: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
    ) {
        // Intensity Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "LUT Intensity",
                style = MaterialTheme.typography.labelSmall,
                color = Color.LightGray,
                fontSize = 11.sp
            )
            Slider(
                value = intensity,
                onValueChange = onUpdateIntensity,
                valueRange = 0.0f..1.0f,
                colors = SliderDefaults.colors(
                    thumbColor = PlayheadAmber,
                    activeTrackColor = PlayheadAmber,
                    inactiveTrackColor = StudioBorder
                ),
                modifier = Modifier.weight(1f).height(32.dp)
            )
            Text(
                text = "${(intensity * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = PlayheadAmber,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // LUT Preset Cards
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(LutPreset.values()) { preset ->
                val isSelected = preset == activePreset
                Surface(
                    modifier = Modifier
                        .size(width = 115.dp, height = 110.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MotionCyan else StudioBorder,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelectPreset(preset) },
                    color = StudioSurfaceVariant
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(preset.previewColor))
                        )
                        Text(
                            text = preset.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MotionCyan else Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            fontSize = 11.sp
                        )
                        Text(
                            text = preset.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            maxLines = 2,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}
