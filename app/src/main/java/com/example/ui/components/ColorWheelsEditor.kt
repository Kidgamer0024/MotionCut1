package com.example.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ColorWheelValue
import com.example.ui.theme.MotionCyan
import com.example.ui.theme.PlayheadAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioDarkBg
import com.example.ui.theme.StudioSurfaceVariant
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

enum class ActiveWheel(val label: String, val desc: String) {
    LIFT("Lift", "Shadows & Black Point"),
    GAMMA("Gamma", "Midtones & Skin Tones"),
    GAIN("Gain", "Highlights & Whites"),
    OFFSET("Offset", "Master Global Tint")
}

@Composable
fun ColorWheelsEditor(
    lift: ColorWheelValue,
    gamma: ColorWheelValue,
    gain: ColorWheelValue,
    offset: ColorWheelValue,
    onUpdateLift: (ColorWheelValue) -> Unit,
    onUpdateGamma: (ColorWheelValue) -> Unit,
    onUpdateGain: (ColorWheelValue) -> Unit,
    onUpdateOffset: (ColorWheelValue) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeWheel by remember { mutableStateOf(ActiveWheel.GAMMA) }

    val currentWheelValue = when (activeWheel) {
        ActiveWheel.LIFT -> lift
        ActiveWheel.GAMMA -> gamma
        ActiveWheel.GAIN -> gain
        ActiveWheel.OFFSET -> offset
    }

    val updateCurrentWheel = { newVal: ColorWheelValue ->
        when (activeWheel) {
            ActiveWheel.LIFT -> onUpdateLift(newVal)
            ActiveWheel.GAMMA -> onUpdateGamma(newVal)
            ActiveWheel.GAIN -> onUpdateGain(newVal)
            ActiveWheel.OFFSET -> onUpdateOffset(newVal)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioDarkBg)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Wheel Selector Tabs (Lift, Gamma, Gain, Offset)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ActiveWheel.values().forEach { wheel ->
                val isSelected = wheel == activeWheel
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) MotionCyan.copy(alpha = 0.25f) else StudioSurfaceVariant)
                        .border(
                            1.dp,
                            if (isSelected) MotionCyan else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { activeWheel = wheel }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = wheel.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MotionCyan else Color.LightGray,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Wheel Title & Readout with Reset Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${activeWheel.label} • ${activeWheel.desc}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Hue: ${currentWheelValue.hue.toInt()}°  Sat: ${(currentWheelValue.saturation * 100).toInt()}%  Luma: ${(currentWheelValue.luminance * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MotionCyan,
                    fontSize = 11.sp
                )
            }

            IconButton(
                onClick = { updateCurrentWheel(ColorWheelValue()) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset Color Wheel",
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2D Chromatic Color Wheel Canvas
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
                .border(2.dp, StudioBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(activeWheel) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val maxRadius = size.width / 2f

                            val dx = change.position.x - center.x
                            val dy = change.position.y - center.y
                            val dist = hypot(dx, dy)
                            val sat = (dist / maxRadius).coerceIn(0f, 1f)

                            var angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            if (angleDeg < 0) angleDeg += 360f

                            updateCurrentWheel(
                                currentWheelValue.copy(
                                    hue = angleDeg,
                                    saturation = sat
                                )
                            )
                        }
                    }
                    .pointerInput(activeWheel) {
                        detectTapGestures { offset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val maxRadius = size.width / 2f

                            val dx = offset.x - center.x
                            val dy = offset.y - center.y
                            val dist = hypot(dx, dy)
                            val sat = (dist / maxRadius).coerceIn(0f, 1f)

                            var angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            if (angleDeg < 0) angleDeg += 360f

                            updateCurrentWheel(
                                currentWheelValue.copy(
                                    hue = angleDeg,
                                    saturation = sat
                                )
                            )
                        }
                    }
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.width / 2f

                // Draw chromatic hue ring gradient
                val sweepColors = listOf(
                    Color.Red,
                    Color(0xFFFF8800),
                    Color.Yellow,
                    Color.Green,
                    Color.Cyan,
                    Color.Blue,
                    Color.Magenta,
                    Color.Red
                )
                drawCircle(
                    brush = Brush.sweepGradient(sweepColors, center),
                    radius = radius
                )

                // White/Neutral center radial desaturation gradient
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, Color.Transparent),
                        center = center,
                        radius = radius
                    ),
                    radius = radius
                )

                // Dark outer vignette
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color(0x66000000)),
                        center = center,
                        radius = radius
                    ),
                    radius = radius
                )

                // Neutral Crosshairs
                drawLine(Color(0x55000000), Offset(center.x - 12f, center.y), Offset(center.x + 12f, center.y), strokeWidth = 1.5f)
                drawLine(Color(0x55000000), Offset(center.x, center.y - 12f), Offset(center.x, center.y + 12f), strokeWidth = 1.5f)

                // Draw current color puck
                val angleRad = Math.toRadians(currentWheelValue.hue.toDouble())
                val puckDistance = currentWheelValue.saturation * radius
                val puckX = center.x + (cos(angleRad) * puckDistance).toFloat()
                val puckY = center.y + (sin(angleRad) * puckDistance).toFloat()

                // Puck handle with glowing white stroke
                drawCircle(Color.Black, 8.dp.toPx(), Offset(puckX, puckY))
                drawCircle(Color.White, 7.dp.toPx(), Offset(puckX, puckY))
                drawCircle(MotionCyan, 4.dp.toPx(), Offset(puckX, puckY))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Luminance Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Luma",
                style = MaterialTheme.typography.labelSmall,
                color = Color.LightGray,
                fontSize = 11.sp
            )

            Slider(
                value = currentWheelValue.luminance,
                onValueChange = { newLum ->
                    updateCurrentWheel(currentWheelValue.copy(luminance = newLum))
                },
                valueRange = -1.0f..1.0f,
                colors = SliderDefaults.colors(
                    thumbColor = PlayheadAmber,
                    activeTrackColor = PlayheadAmber,
                    inactiveTrackColor = StudioBorder
                ),
                modifier = Modifier.weight(1f).height(32.dp)
            )

            Text(
                text = "${(currentWheelValue.luminance * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = PlayheadAmber,
                fontSize = 11.sp
            )
        }
    }
}
