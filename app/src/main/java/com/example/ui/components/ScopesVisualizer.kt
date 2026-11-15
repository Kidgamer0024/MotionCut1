package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ColorGradingCompositor
import com.example.model.ColorGradingSettings
import com.example.ui.theme.MotionCyan
import com.example.ui.theme.ScopeBlue
import com.example.ui.theme.ScopeGreen
import com.example.ui.theme.ScopeRed
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioDarkBg
import com.example.ui.theme.StudioSurfaceVariant

enum class ScopeMode(val label: String) {
    WAVEFORM("Waveform"),
    RGB_PARADE("RGB Parade"),
    HISTOGRAM("Histogram")
}

@Composable
fun ScopesVisualizer(
    colorGradingSettings: ColorGradingSettings,
    selectedScope: ScopeMode,
    onSelectScope: (ScopeMode) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scopeData = remember(colorGradingSettings) {
        ColorGradingCompositor.computeScopeSamples(colorGradingSettings)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, StudioBorder, RoundedCornerShape(12.dp)),
        color = StudioDarkBg.copy(alpha = 0.92f)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header with Scope Type Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ScopeMode.values().forEach { mode ->
                        val isSelected = mode == selectedScope
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) MotionCyan.copy(alpha = 0.25f) else StudioSurfaceVariant)
                                .border(
                                    1.dp,
                                    if (isSelected) MotionCyan else Color.Transparent,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { onSelectScope(mode) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MotionCyan else Color.LightGray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Scopes",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Canvas drawing scope curves
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(Color(0xFF05070B), RoundedCornerShape(6.dp))
                    .padding(4.dp)
            ) {
                val w = size.width
                val h = size.height

                // Draw reference grid lines (100 IRE, 70 IRE, 50 IRE, 0 IRE)
                val ire100 = h * 0.05f
                val ire70 = h * 0.30f
                val ire50 = h * 0.50f
                val ire0 = h * 0.95f

                drawLine(Color(0x33FFFFFF), Offset(0f, ire100), Offset(w, ire100), strokeWidth = 1f)
                drawLine(Color(0x22FFFFFF), Offset(0f, ire70), Offset(w, ire70), strokeWidth = 1f)
                drawLine(Color(0x22FFFFFF), Offset(0f, ire50), Offset(w, ire50), strokeWidth = 1f)
                drawLine(Color(0x33FFFFFF), Offset(0f, ire0), Offset(w, ire0), strokeWidth = 1f)

                when (selectedScope) {
                    ScopeMode.WAVEFORM -> {
                        // Single green oscilloscope waveform
                        drawOscilloscopeChannel(scopeData.lumaWaveform, w, h, ScopeGreen)
                    }
                    ScopeMode.RGB_PARADE -> {
                        // 3 Channels side-by-side: Red (left), Green (center), Blue (right)
                        val channelW = w / 3f
                        drawOscilloscopeChannel(scopeData.rParade, channelW, h, ScopeRed, xOffset = 0f)
                        drawOscilloscopeChannel(scopeData.gParade, channelW, h, ScopeGreen, xOffset = channelW)
                        drawOscilloscopeChannel(scopeData.bParade, channelW, h, ScopeBlue, xOffset = channelW * 2f)

                        // Channel dividers
                        drawLine(Color(0x44FFFFFF), Offset(channelW, 0f), Offset(channelW, h), strokeWidth = 1f)
                        drawLine(Color(0x44FFFFFF), Offset(channelW * 2f, 0f), Offset(channelW * 2f, h), strokeWidth = 1f)
                    }
                    ScopeMode.HISTOGRAM -> {
                        // Overlapping R, G, B histogram mountain paths
                        drawHistogramChannel(scopeData.rParade, w, h, ScopeRed.copy(alpha = 0.5f))
                        drawHistogramChannel(scopeData.gParade, w, h, ScopeGreen.copy(alpha = 0.5f))
                        drawHistogramChannel(scopeData.bParade, w, h, ScopeBlue.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawOscilloscopeChannel(
    samples: FloatArray,
    width: Float,
    height: Float,
    color: Color,
    xOffset: Float = 0f
) {
    val path = Path()
    val step = width / (samples.size - 1)

    for (i in samples.indices) {
        val x = xOffset + i * step
        val y = height * (1f - samples[i])
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }

    drawPath(path, color = color, style = Stroke(width = 2f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHistogramChannel(
    samples: FloatArray,
    width: Float,
    height: Float,
    color: Color
) {
    val path = Path()
    val step = width / (samples.size - 1)
    path.moveTo(0f, height)

    for (i in samples.indices) {
        val x = i * step
        val y = height * (1f - samples[i] * 0.85f)
        path.lineTo(x, y)
    }
    path.lineTo(width, height)
    path.close()

    drawPath(path, color = color)
}
