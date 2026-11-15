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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.HslAdjustment
import com.example.model.HslBand
import com.example.ui.theme.MotionCyan
import com.example.ui.theme.PlayheadAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioDarkBg

@Composable
fun HslSecondaryEditor(
    adjustments: Map<HslBand, HslAdjustment>,
    onUpdateAdjustment: (HslBand, HslAdjustment) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedBand by remember { mutableStateOf(HslBand.RED) }
    val currentAdj = adjustments[selectedBand] ?: HslAdjustment()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioDarkBg)
            .padding(10.dp)
    ) {
        // Color Band Swatches
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            items(HslBand.values()) { band ->
                val isSelected = band == selectedBand
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(band.baseColorHex))
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) Color.White else StudioBorder,
                            shape = CircleShape
                        )
                        .clickable { selectedBand = band },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "${selectedBand.label} Channel Fine-Tuning",
            style = MaterialTheme.typography.labelMedium,
            color = Color(selectedBand.baseColorHex)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Hue Shift Slider (-180..+180)
        HslSliderRow(
            label = "Hue Shift",
            value = currentAdj.hueShift,
            valueRange = -180f..180f,
            unit = "°",
            activeColor = MotionCyan,
            onValueChange = { onUpdateAdjustment(selectedBand, currentAdj.copy(hueShift = it)) }
        )

        // Saturation Slider (-100..+100)
        HslSliderRow(
            label = "Saturation",
            value = currentAdj.saturation,
            valueRange = -100f..100f,
            unit = "%",
            activeColor = PlayheadAmber,
            onValueChange = { onUpdateAdjustment(selectedBand, currentAdj.copy(saturation = it)) }
        )

        // Luminance Slider (-100..+100)
        HslSliderRow(
            label = "Luminance",
            value = currentAdj.luminance,
            valueRange = -100f..100f,
            unit = "%",
            activeColor = Color.White,
            onValueChange = { onUpdateAdjustment(selectedBand, currentAdj.copy(luminance = it)) }
        )
    }
}

@Composable
private fun HslSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    unit: String,
    activeColor: Color,
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
            modifier = Modifier.size(width = 70.dp, height = 18.dp),
            fontSize = 11.sp
        )

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = activeColor,
                activeTrackColor = activeColor,
                inactiveTrackColor = StudioBorder
            ),
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "${value.toInt()}$unit",
            style = MaterialTheme.typography.labelSmall,
            color = activeColor,
            modifier = Modifier.size(width = 44.dp, height = 18.dp),
            fontSize = 11.sp
        )
    }
}
