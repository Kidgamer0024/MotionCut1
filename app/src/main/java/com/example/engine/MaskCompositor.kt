package com.example.engine

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import com.example.model.MaskConfig
import com.example.model.MaskKeyframe
import com.example.model.MaskShapeType

object MaskCompositor {

    data class EvaluatedMask(
        val positionX: Float,
        val positionY: Float,
        val scaleX: Float,
        val scaleY: Float,
        val rotation: Float,
        val featherPx: Float,
        val path: Path,
        val isInverted: Boolean,
        val opacity: Float,
        val borderWidthPx: Float = 0f,
        val borderColorHex: Long = 0xFFFFFFFF
    )

    fun evaluateMask(mask: MaskConfig, currentTimeMs: Long, clipStartTimeMs: Long, clipWidth: Float, clipHeight: Float): EvaluatedMask {
        val relTime = (currentTimeMs - clipStartTimeMs).coerceAtLeast(0L)

        // Interpolate keyframes if animated
        var posX = mask.positionX
        var posY = mask.positionY
        var scale = mask.scaleX
        var rot = mask.rotation
        var feather = mask.featherPx

        if (mask.isAnimated && mask.keyframes.isNotEmpty()) {
            val kfs = mask.keyframes.sortedBy { it.timeMs }
            if (relTime <= kfs.first().timeMs) {
                posX = kfs.first().positionX
                posY = kfs.first().positionY
                scale = kfs.first().scale
                rot = kfs.first().rotation
                feather = kfs.first().featherPx
            } else if (relTime >= kfs.last().timeMs) {
                posX = kfs.last().positionX
                posY = kfs.last().positionY
                scale = kfs.last().scale
                rot = kfs.last().rotation
                feather = kfs.last().featherPx
            } else {
                for (i in 0 until kfs.size - 1) {
                    val kA = kfs[i]
                    val kB = kfs[i + 1]
                    if (relTime in kA.timeMs..kB.timeMs) {
                        val progress = (relTime - kA.timeMs).toFloat() / (kB.timeMs - kA.timeMs).toFloat()
                        posX = kA.positionX + progress * (kB.positionX - kA.positionX)
                        posY = kA.positionY + progress * (kB.positionY - kA.positionY)
                        scale = kA.scale + progress * (kB.scale - kA.scale)
                        rot = kA.rotation + progress * (kB.rotation - kA.rotation)
                        feather = kA.featherPx + progress * (kB.featherPx - kA.featherPx)
                        break
                    }
                }
            }
        }

        // Build base path based on shape type
        val basePath = Path()
        val halfW = ((clipWidth * 0.4f * scale * mask.aspectWidth.coerceIn(0.25f, 2.5f)) + mask.expansion)
            .coerceAtLeast(4f)
        val halfH = ((clipHeight * 0.4f * scale * mask.aspectHeight.coerceIn(0.25f, 2.5f)) + mask.expansion)
            .coerceAtLeast(4f)

        when (mask.shapeType) {
            MaskShapeType.RECTANGLE -> {
                val radius = mask.cornerRadiusPx.coerceIn(0f, minOf(halfW, halfH))
                basePath.addRoundRect(
                    RoundRect(
                        left = -halfW,
                        top = -halfH,
                        right = halfW,
                        bottom = halfH,
                        cornerRadius = CornerRadius(radius)
                    )
                )
            }
            MaskShapeType.CIRCLE -> {
                val radius = ((minOf(clipWidth, clipHeight) * 0.35f * scale) + mask.expansion)
                    .coerceAtLeast(4f)
                basePath.addOval(
                    Rect(
                        left = -radius,
                        top = -radius,
                        right = radius,
                        bottom = radius
                    )
                )
            }
            MaskShapeType.ELLIPSE, MaskShapeType.RADIAL_VIGNETTE -> {
                basePath.addOval(
                    Rect(
                        left = -halfW,
                        top = -halfH,
                        right = halfW,
                        bottom = halfH
                    )
                )
            }
            MaskShapeType.LINEAR_SPLIT -> {
                val splitOffset = mask.expansion
                basePath.moveTo(-clipWidth * 1.5f, -clipHeight * 1.5f)
                basePath.lineTo(clipWidth * 1.5f, -clipHeight * 1.5f)
                basePath.lineTo(clipWidth * 1.5f, splitOffset)
                basePath.lineTo(-clipWidth * 1.5f, splitOffset)
                basePath.close()
            }
            MaskShapeType.FREEFORM_POLYGON -> {
                if (mask.vertices.isNotEmpty()) {
                    basePath.moveTo(mask.vertices[0].x * scale, mask.vertices[0].y * scale)
                    for (idx in 1 until mask.vertices.size) {
                        basePath.lineTo(mask.vertices[idx].x * scale, mask.vertices[idx].y * scale)
                    }
                    basePath.close()
                } else {
                    basePath.addRoundRect(
                        RoundRect(-halfW, -halfH, halfW, halfH, CornerRadius(8f))
                    )
                }
            }
        }

        // Handle invert by subtracting from canvas bounds if inverted
        val finalPath = if (mask.isInverted) {
            val outerBounds = Path().apply {
                addRect(Rect(-clipWidth * 1.5f, -clipHeight * 1.5f, clipWidth * 1.5f, clipHeight * 1.5f))
            }
            val inverted = Path()
            inverted.op(outerBounds, basePath, PathOperation.Difference)
            inverted
        } else {
            basePath
        }

        return EvaluatedMask(
            positionX = posX,
            positionY = posY,
            scaleX = scale,
            scaleY = scale,
            rotation = rot,
            featherPx = feather,
            path = finalPath,
            isInverted = mask.isInverted,
            opacity = mask.opacity,
            borderWidthPx = mask.borderWidthPx,
            borderColorHex = mask.borderColorHex
        )
    }
}
