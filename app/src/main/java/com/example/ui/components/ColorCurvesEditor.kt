package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChannelCurve
import com.example.model.CurvePoint
import com.example.ui.theme.MotionCyan
import com.example.ui.theme.ScopeBlue
import com.example.ui.theme.ScopeGreen
import com.example.ui.theme.ScopeRed
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioDarkBg
import com.example.ui.theme.StudioSurfaceVariant

enum class CurveChannel(val label: String, val color: Color) {
    MASTER("RGB Master", Color.White),
    RED("Red", ScopeRed),
    GREEN("Green", ScopeGreen),
    BLUE("Blue", ScopeBlue)
}

@Composable
fun ColorCurvesEditor(
    masterCurve: ChannelCurve,
    redCurve: ChannelCurve,
    greenCurve: ChannelCurve,
    blueCurve: ChannelCurve,
    onUpdateCurve: (CurveChannel, ChannelCurve) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeChannel by remember { mutableStateOf(CurveChannel.MASTER) }

    val currentCurve = when (activeChannel) {
        CurveChannel.MASTER -> masterCurve
        CurveChannel.RED -> redCurve
        CurveChannel.GREEN -> greenCurve
        CurveChannel.BLUE -> blueCurve
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioDarkBg)
            .padding(10.dp)
    ) {
        // Channel Tabs (RGB, Red, Green, Blue)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CurveChannel.values().forEach { channel ->
                val isSelected = channel == activeChannel
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) channel.color.copy(alpha = 0.25f) else StudioSurfaceVariant)
                        .border(
                            1.dp,
                            if (isSelected) channel.color else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { activeChannel = channel }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = channel.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) channel.color else Color.LightGray,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Curve Canvas
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, StudioBorder, RoundedCornerShape(8.dp)),
            color = Color(0xFF07090E)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
                    .pointerInput(activeChannel) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()

                            val touchX = (change.position.x / w).coerceIn(0.1f, 0.9f)
                            val touchY = (1f - (change.position.y / h)).coerceIn(0f, 1f)

                            // Update midtone control point
                            val newPoints = listOf(
                                CurvePoint(0f, 0f),
                                CurvePoint(touchX, touchY),
                                CurvePoint(1f, 1f)
                            )
                            onUpdateCurve(activeChannel, ChannelCurve(newPoints))
                        }
                    }
            ) {
                val w = size.width
                val h = size.height

                // Grid divisions (25%, 50%, 75%)
                val gridColor = Color(0x22FFFFFF)
                drawLine(gridColor, Offset(0f, h * 0.25f), Offset(w, h * 0.25f), strokeWidth = 1f)
                drawLine(gridColor, Offset(0f, h * 0.50f), Offset(w, h * 0.50f), strokeWidth = 1f)
                drawLine(gridColor, Offset(0f, h * 0.75f), Offset(w, h * 0.75f), strokeWidth = 1f)

                drawLine(gridColor, Offset(w * 0.25f, 0f), Offset(w * 0.25f, h), strokeWidth = 1f)
                drawLine(gridColor, Offset(w * 0.50f, 0f), Offset(w * 0.50f, h), strokeWidth = 1f)
                drawLine(gridColor, Offset(w * 0.75f, 0f), Offset(w * 0.75f, h), strokeWidth = 1f)

                // Diagonal baseline
                drawLine(Color(0x33FFFFFF), Offset(0f, h), Offset(w, 0f), strokeWidth = 1f)

                // Render spline curve through points
                val pts = currentCurve.points
                if (pts.size >= 2) {
                    val path = Path()
                    val p0 = Offset(pts[0].x * w, h - pts[0].y * h)
                    path.moveTo(p0.x, p0.y)

                    if (pts.size == 2) {
                        val p1 = Offset(pts[1].x * w, h - pts[1].y * h)
                        path.lineTo(p1.x, p1.y)
                    } else {
                        val pMid = Offset(pts[1].x * w, h - pts[1].y * h)
                        val pEnd = Offset(pts[2].x * w, h - pts[2].y * h)
                        path.quadraticBezierTo(pMid.x, pMid.y, pEnd.x, pEnd.y)
                    }

                    drawPath(path, color = activeChannel.color, style = Stroke(width = 3.dp.toPx()))

                    // Draw control points
                    pts.forEach { pt ->
                        val ptOffset = Offset(pt.x * w, h - pt.y * h)
                        drawCircle(Color.White, 6.dp.toPx(), ptOffset)
                        drawCircle(activeChannel.color, 6.dp.toPx(), ptOffset, style = Stroke(2.dp.toPx()))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Reset Curve
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Drag center point to adjust contrast & curve slope",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                fontSize = 11.sp
            )

            IconButton(
                onClick = { onUpdateCurve(activeChannel, ChannelCurve()) },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset Curve",
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
