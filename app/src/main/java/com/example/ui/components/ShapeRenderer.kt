package com.example.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.model.ShapeType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun DrawScope.drawVectorShapeGeometry(
    type: ShapeType,
    shapeSize: Float,
    fillColor: Color,
    strokeColor: Color,
    strokeWidth: Float,
    cornerRadius: Float = 16f,
    alpha: Float = 1f
) {
    val half = shapeSize / 2f

    fun emitPath(path: Path) {
        drawPath(path = path, color = fillColor, alpha = alpha)
        if (strokeWidth > 0f) {
            drawPath(path = path, color = strokeColor, style = Stroke(width = strokeWidth), alpha = alpha)
        }
    }

    when (type) {
        ShapeType.CIRCLE -> {
            drawCircle(color = fillColor, radius = half, center = Offset.Zero, alpha = alpha)
            if (strokeWidth > 0f) {
                drawCircle(
                    color = strokeColor,
                    radius = half,
                    center = Offset.Zero,
                    style = Stroke(width = strokeWidth),
                    alpha = alpha
                )
            }
        }
        ShapeType.SQUARE -> {
            drawRect(
                color = fillColor,
                topLeft = Offset(-half, -half),
                size = Size(shapeSize, shapeSize),
                alpha = alpha
            )
            if (strokeWidth > 0f) {
                drawRect(
                    color = strokeColor,
                    topLeft = Offset(-half, -half),
                    size = Size(shapeSize, shapeSize),
                    style = Stroke(width = strokeWidth),
                    alpha = alpha
                )
            }
        }
        ShapeType.ROUNDED -> {
            val r = CornerRadius(shapeSize * 0.24f, shapeSize * 0.24f)
            drawRoundRect(
                color = fillColor,
                topLeft = Offset(-half, -half),
                size = Size(shapeSize, shapeSize),
                cornerRadius = r,
                alpha = alpha
            )
            if (strokeWidth > 0f) {
                drawRoundRect(
                    color = strokeColor,
                    topLeft = Offset(-half, -half),
                    size = Size(shapeSize, shapeSize),
                    cornerRadius = r,
                    style = Stroke(width = strokeWidth),
                    alpha = alpha
                )
            }
        }
        ShapeType.RECTANGLE -> {
            val rectW = shapeSize * 1.18f
            val rectH = shapeSize * 0.72f
            val r = CornerRadius(cornerRadius.coerceAtMost(rectH * 0.2f))
            drawRoundRect(
                color = fillColor,
                topLeft = Offset(-rectW / 2f, -rectH / 2f),
                size = Size(rectW, rectH),
                cornerRadius = r,
                alpha = alpha
            )
            if (strokeWidth > 0f) {
                drawRoundRect(
                    color = strokeColor,
                    topLeft = Offset(-rectW / 2f, -rectH / 2f),
                    size = Size(rectW, rectH),
                    cornerRadius = r,
                    style = Stroke(width = strokeWidth),
                    alpha = alpha
                )
            }
        }
        ShapeType.TRIANGLE -> emitPath(regularPolygonPath(3, half, startAngleDeg = -90.0))
        ShapeType.DIAMOND -> emitPath(regularPolygonPath(4, half, startAngleDeg = 0.0))
        ShapeType.PENTAGON -> emitPath(regularPolygonPath(5, half, startAngleDeg = -90.0))
        ShapeType.HEXAGON, ShapeType.POLYGON -> emitPath(regularPolygonPath(6, half, startAngleDeg = -90.0))
        ShapeType.OCTAGON -> emitPath(regularPolygonPath(8, half, startAngleDeg = -67.5))
        ShapeType.STAR -> emitPath(starPath(5, outerR = half, innerR = half * 0.44f))
        ShapeType.STAR_6 -> emitPath(starPath(6, outerR = half, innerR = half * 0.50f))
        ShapeType.SPARKLE -> emitPath(starPath(4, outerR = half, innerR = half * 0.24f))
        ShapeType.QUAD -> {
            val path = Path().apply {
                moveTo(-half * 0.9f, -half * 0.85f)
                lineTo(half * 0.55f, -half * 0.85f)
                lineTo(half * 0.95f, half * 0.85f)
                lineTo(-half * 0.9f, half * 0.85f)
                close()
            }
            emitPath(path)
        }
        ShapeType.IRREGULAR -> {
            val path = Path().apply {
                moveTo(-half * 0.85f, -half * 0.85f)
                lineTo(-half * 0.05f, -half * 0.85f)
                lineTo(-half * 0.05f, -half * 0.1f)
                lineTo(half * 0.9f, half * 0.85f)
                lineTo(-half * 0.85f, half * 0.85f)
                close()
            }
            emitPath(path)
        }
        ShapeType.MULTIFOIL -> {
            val path = Path()
            val petals = 5
            val step = 5
            val total = petals * 24
            for (i in 0..total) {
                val t = (i.toFloat() / total) * (2f * PI.toFloat())
                val r = half * (0.76f + 0.24f * cos(petals * t))
                val x = r * cos(t - PI.toFloat() / 2f)
                val y = r * sin(t - PI.toFloat() / 2f)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            emitPath(path)
        }
        ShapeType.PLUS -> {
            val arm = half * 0.34f
            val path = Path().apply {
                moveTo(-arm, -half)
                lineTo(arm, -half)
                lineTo(arm, -arm)
                lineTo(half, -arm)
                lineTo(half, arm)
                lineTo(arm, arm)
                lineTo(arm, half)
                lineTo(-arm, half)
                lineTo(-arm, arm)
                lineTo(-half, arm)
                lineTo(-half, -arm)
                lineTo(-arm, -arm)
                close()
            }
            emitPath(path)
        }
        ShapeType.TEARDROP -> {
            val path = Path().apply {
                moveTo(0f, -half)
                cubicTo(half * 0.95f, -half * 0.1f, half * 0.85f, half * 0.9f, 0f, half * 0.95f)
                cubicTo(-half * 0.85f, half * 0.9f, -half * 0.95f, -half * 0.1f, 0f, -half)
                close()
            }
            emitPath(path)
        }
        ShapeType.PIE -> {
            val rect = Rect(-half, -half, half, half)
            val path = Path().apply {
                moveTo(0f, 0f)
                arcTo(rect, startAngleDegrees = -35f, sweepAngleDegrees = 295f, forceMoveTo = false)
                close()
            }
            emitPath(path)
        }
        ShapeType.ARC -> {
            val rect = Rect(-half, -half, half, half)
            val path = Path().apply {
                arcTo(rect, startAngleDegrees = 35f, sweepAngleDegrees = 250f, forceMoveTo = true)
                close()
            }
            emitPath(path)
        }
        ShapeType.MOON -> {
            val path = Path().apply {
                moveTo(half * 0.35f, -half * 0.92f)
                cubicTo(-half * 0.85f, -half * 0.72f, -half * 0.85f, half * 0.72f, half * 0.35f, half * 0.92f)
                cubicTo(-half * 0.15f, half * 0.55f, -half * 0.15f, -half * 0.55f, half * 0.35f, -half * 0.92f)
                close()
            }
            emitPath(path)
        }
        ShapeType.STAMP -> {
            val path = Path()
            val notches = 20
            for (i in 0 until notches) {
                val angle = (i.toFloat() / notches) * 2f * PI.toFloat()
                val baseR = half * 0.86f
                val r = if (i % 2 == 0) baseR + half * 0.12f else baseR - half * 0.05f
                val x = r * cos(angle)
                val y = r * sin(angle)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            emitPath(path)
        }
        ShapeType.CALLOUT -> {
            val path = Path().apply {
                moveTo(-half * 0.85f, -half * 0.75f)
                lineTo(half * 0.85f, -half * 0.75f)
                lineTo(half * 0.85f, half * 0.45f)
                lineTo(-half * 0.05f, half * 0.45f)
                lineTo(-half * 0.35f, half * 0.9f)
                lineTo(-half * 0.35f, half * 0.45f)
                lineTo(-half * 0.85f, half * 0.45f)
                close()
            }
            emitPath(path)
        }
        ShapeType.HEART -> {
            val path = Path().apply {
                moveTo(0f, half * 0.85f)
                cubicTo(-half * 1.15f, half * 0.15f, -half * 0.95f, -half * 0.85f, 0f, -half * 0.30f)
                cubicTo(half * 0.95f, -half * 0.85f, half * 1.15f, half * 0.15f, 0f, half * 0.85f)
                close()
            }
            emitPath(path)
        }
    }
}

private fun regularPolygonPath(sides: Int, radius: Float, startAngleDeg: Double): Path {
    val path = Path()
    for (i in 0 until sides) {
        val rad = Math.toRadians(startAngleDeg + (360.0 / sides) * i)
        val x = (radius * cos(rad)).toFloat()
        val y = (radius * sin(rad)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

private fun starPath(points: Int, outerR: Float, innerR: Float): Path {
    val path = Path()
    val total = points * 2
    for (i in 0 until total) {
        val r = if (i % 2 == 0) outerR else innerR
        val rad = Math.toRadians(-90.0 + (360.0 / total) * i)
        val x = (r * cos(rad)).toFloat()
        val y = (r * sin(rad)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}
