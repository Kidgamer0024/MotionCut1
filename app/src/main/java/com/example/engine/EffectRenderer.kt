package com.example.engine

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.example.model.EffectKernel
import com.example.model.VideoEffect
import com.example.ui.theme.KeyframeViolet
import com.example.ui.theme.MotionCyan
import com.example.ui.theme.PlayheadAmber
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders live visual effect overlays for any applied [VideoEffect] kernel
 * inside [VideoMonitorCanvas]. Supports animated phase progression during playback.
 */
object EffectRenderer {

    fun DrawScope.drawEffectOverlay(
        effect: VideoEffect,
        canvasW: Float,
        canvasH: Float,
        currentTimeMs: Long
    ) {
        if (!effect.isEnabled || effect.intensity <= 0.01f) return
        val intensity = effect.intensity.coerceIn(0f, 1f)
        val timePhase = (currentTimeMs / 1000f) * effect.speed + effect.paramB * 6.28f
        val detail = effect.paramA.coerceIn(0.05f, 1f)
        val fxColor = Color(effect.colorHex)

        when (effect.effectiveKernel) {
            EffectKernel.SCANLINES -> {
                val spacing = (14f - detail * 9f).coerceAtLeast(4f).dp.toPx()
                val offset = ((timePhase * 12f) % spacing)
                var ly = -canvasH / 2f + offset
                while (ly < canvasH / 2f) {
                    drawLine(
                        color = fxColor.copy(alpha = intensity * 0.38f),
                        start = Offset(-canvasW / 2f, ly),
                        end = Offset(canvasW / 2f, ly),
                        strokeWidth = 2f
                    )
                    ly += spacing
                }
            }

            EffectKernel.CHROMATIC_ABERRATION, EffectKernel.PRISM_SHIFT -> {
                val shift = intensity * (6f + detail * 14f).dp.toPx()
                val angle = timePhase * 0.5f
                val dx = cos(angle) * shift
                val dy = sin(angle) * shift * 0.35f
                drawRect(
                    color = fxColor.copy(alpha = 0.14f * intensity),
                    topLeft = Offset(-canvasW / 2f - dx, -canvasH / 2f - dy),
                    size = Size(canvasW, canvasH)
                )
                drawRect(
                    color = Color(0xFFFF2E63).copy(alpha = 0.14f * intensity),
                    topLeft = Offset(-canvasW / 2f + dx, -canvasH / 2f + dy),
                    size = Size(canvasW, canvasH)
                )
            }

            EffectKernel.GLOW -> {
                val radius = canvasW * (0.45f + detail * 0.40f)
                val pulse = 0.85f + 0.15f * sin(timePhase)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.18f * intensity * pulse),
                            fxColor.copy(alpha = 0.14f * intensity * pulse),
                            Color.Transparent
                        ),
                        center = Offset.Zero,
                        radius = radius
                    ),
                    radius = radius,
                    center = Offset.Zero
                )
            }

            EffectKernel.LIGHT_SWEEP -> {
                val sweepProgress = ((timePhase * 0.45f) % 1.4f) - 0.2f
                val sweepX = -canvasW / 2f + sweepProgress * canvasW
                val barW = canvasW * (0.14f + detail * 0.22f)
                rotate(degrees = 22f) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.32f * intensity),
                                fxColor.copy(alpha = 0.22f * intensity),
                                Color.Transparent
                            ),
                            startX = sweepX - barW / 2f,
                            endX = sweepX + barW / 2f
                        ),
                        topLeft = Offset(sweepX - barW / 2f, -canvasH),
                        size = Size(barW, canvasH * 2f)
                    )
                }
            }

            EffectKernel.MOTION_BLUR -> {
                val streaks = (4 + (detail * 8).toInt()).coerceAtMost(12)
                for (i in 0 until streaks) {
                    val yOff = (-canvasH * 0.38f) + (i / streaks.toFloat()) * (canvasH * 0.76f)
                    val dx = sin(timePhase + i) * 18f * intensity
                    drawLine(
                        color = fxColor.copy(alpha = 0.12f * intensity),
                        start = Offset(-canvasW * 0.45f + dx, yOff),
                        end = Offset(canvasW * 0.45f + dx, yOff),
                        strokeWidth = 3.dp.toPx()
                    )
                }
            }

            EffectKernel.GAUSSIAN_BLUR -> {
                val layers = 3
                for (l in 1..layers) {
                    val scaleFrac = 0.90f - l * 0.12f
                    drawRoundRect(
                        color = fxColor.copy(alpha = (0.09f + detail * 0.10f) * intensity / l),
                        topLeft = Offset(-canvasW * scaleFrac / 2f, -canvasH * scaleFrac / 2f),
                        size = Size(canvasW * scaleFrac, canvasH * scaleFrac),
                        cornerRadius = CornerRadius((18f * l).dp.toPx())
                    )
                }
            }

            EffectKernel.RADIAL_BLUR -> {
                val rings = (3 + (detail * 5).toInt()).coerceIn(3, 8)
                for (i in 1..rings) {
                    val radius = (minOf(canvasW, canvasH) * 0.12f) * i
                    drawCircle(
                        color = fxColor.copy(alpha = (0.18f / i) * intensity),
                        radius = radius,
                        center = Offset.Zero,
                        style = Stroke(width = (3f + detail * 4f).dp.toPx())
                    )
                }
            }

            EffectKernel.AMBIENT_BACKDROP -> {
                val radius = maxOf(canvasW, canvasH) * (0.48f + detail * 0.35f)
                val shiftX = sin(timePhase * 0.6f) * canvasW * 0.08f
                val shiftY = cos(timePhase * 0.5f) * canvasH * 0.08f
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            fxColor.copy(alpha = 0.32f * intensity),
                            fxColor.copy(alpha = 0.10f * intensity),
                            Color.Transparent
                        ),
                        center = Offset(shiftX, shiftY),
                        radius = radius
                    ),
                    topLeft = Offset(-canvasW / 2f, -canvasH / 2f),
                    size = Size(canvasW, canvasH)
                )
            }

            EffectKernel.BOKEH_BACKDROP -> {
                val orbCount = (6 + (detail * 10).toInt()).coerceIn(6, 16)
                for (i in 0 until orbCount) {
                    val ox = sin(i * 1.618f + timePhase * 0.4f) * canvasW * 0.38f
                    val oy = cos(i * 2.13f + timePhase * 0.35f) * canvasH * 0.38f
                    val rad = (12f + (i % 4) * 9f + detail * 14f).dp.toPx()
                    drawCircle(
                        color = fxColor.copy(alpha = (0.12f + (i % 3) * 0.06f) * intensity),
                        radius = rad,
                        center = Offset(ox, oy)
                    )
                }
            }

            EffectKernel.GRID_MATRIX -> {
                val cell = (28f - detail * 14f).coerceAtLeast(12f).dp.toPx()
                val shiftY = (timePhase * 10f) % cell
                var gx = -canvasW / 2f
                while (gx <= canvasW / 2f) {
                    drawLine(
                        color = fxColor.copy(alpha = 0.20f * intensity),
                        start = Offset(gx, -canvasH / 2f),
                        end = Offset(gx, canvasH / 2f),
                        strokeWidth = 1f
                    )
                    gx += cell
                }
                var gy = -canvasH / 2f + shiftY
                while (gy <= canvasH / 2f) {
                    drawLine(
                        color = fxColor.copy(alpha = 0.20f * intensity),
                        start = Offset(-canvasW / 2f, gy),
                        end = Offset(canvasW / 2f, gy),
                        strokeWidth = 1f
                    )
                    gy += cell
                }
            }

            EffectKernel.WAVE_WARP -> {
                val waves = 5
                val amp = 14.dp.toPx() * intensity
                val freq = 2f + detail * 4f
                for (w in 0 until waves) {
                    val baseY = -canvasH * 0.35f + (w / (waves - 1f)) * (canvasH * 0.70f)
                    val path = Path()
                    val steps = 20
                    for (s in 0..steps) {
                        val t = s / steps.toFloat()
                        val x = -canvasW / 2f + t * canvasW
                        val y = baseY + sin(t * freq * PI.toFloat() + timePhase + w) * amp
                        if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path = path,
                        color = fxColor.copy(alpha = 0.24f * intensity),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }

            EffectKernel.CAMERA_SHAKE -> {
                val jitter = (4f + detail * 10f) * intensity
                val xOff = sin(timePhase * 7f) * jitter
                val yOff = cos(timePhase * 9f) * jitter
                drawRect(
                    color = fxColor.copy(alpha = 0.16f * intensity),
                    topLeft = Offset(-canvasW * 0.46f + xOff, -canvasH * 0.46f + yOff),
                    size = Size(canvasW * 0.92f, canvasH * 0.92f),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            EffectKernel.FILM_GRAIN -> {
                val count = (18 + (detail * 32).toInt()).coerceAtMost(50)
                for (i in 0 until count) {
                    val gx = (((i * 73 + (currentTimeMs / 50L).toInt() * 31) % 100) / 100f - 0.5f) * canvasW
                    val gy = (((i * 137 + (currentTimeMs / 50L).toInt() * 19) % 100) / 100f - 0.5f) * canvasH
                    drawCircle(
                        color = fxColor.copy(alpha = 0.18f * intensity),
                        radius = (1f + (i % 2)).dp.toPx(),
                        center = Offset(gx, gy)
                    )
                }
            }

            EffectKernel.LENS_DISTORTION -> {
                val radius = canvasW * (0.48f - detail * 0.10f)
                drawCircle(
                    color = fxColor.copy(alpha = 0.24f * intensity),
                    radius = radius,
                    center = Offset.Zero,
                    style = Stroke(width = (1.5f + intensity * 2f).dp.toPx())
                )
            }

            EffectKernel.VIGNETTE -> {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            fxColor.copy(alpha = 0.65f * intensity)
                        ),
                        center = Offset.Zero,
                        radius = maxOf(canvasW, canvasH) * (0.65f - detail * 0.18f)
                    ),
                    topLeft = Offset(-canvasW / 2f, -canvasH / 2f),
                    size = Size(canvasW, canvasH)
                )
            }

            EffectKernel.PIXELATE, EffectKernel.HALFTONE -> {
                val step = (24f - detail * 12f).coerceAtLeast(10f).dp.toPx()
                var gx = -canvasW * 0.42f
                while (gx <= canvasW * 0.42f) {
                    var gy = -canvasH * 0.42f
                    while (gy <= canvasH * 0.42f) {
                        if (effect.effectiveKernel == EffectKernel.PIXELATE) {
                            drawRect(
                                color = fxColor.copy(alpha = 0.10f * intensity),
                                topLeft = Offset(gx, gy),
                                size = Size(step * 0.72f, step * 0.72f),
                                style = Stroke(width = 1f)
                            )
                        } else {
                            drawCircle(
                                color = fxColor.copy(alpha = 0.16f * intensity),
                                radius = step * 0.22f,
                                center = Offset(gx, gy)
                            )
                        }
                        gy += step
                    }
                    gx += step
                }
            }

            EffectKernel.KALEIDOSCOPE, EffectKernel.MIRROR_SPLIT -> {
                val slices = (4 + (detail * 6).toInt()).coerceIn(2, 10)
                for (i in 0 until slices) {
                    val deg = (180f / slices) * i + timePhase * 15f
                    rotate(degrees = deg) {
                        drawLine(
                            color = fxColor.copy(alpha = 0.25f * intensity),
                            start = Offset(-canvasW, 0f),
                            end = Offset(canvasW, 0f),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }
                }
            }

            EffectKernel.COLOR_PULSE -> {
                val hue = ((timePhase * 60f) % 360f).let { if (it < 0f) it + 360f else it }
                val tint = Color.hsv(hue, 0.85f, 1f)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            fxColor.copy(alpha = 0.18f * intensity),
                            tint.copy(alpha = 0.16f * intensity)
                        )
                    ),
                    topLeft = Offset(-canvasW / 2f, -canvasH / 2f),
                    size = Size(canvasW, canvasH)
                )
            }

            EffectKernel.NEON_EDGES -> {
                drawRoundRect(
                    color = fxColor.copy(alpha = 0.50f * intensity),
                    topLeft = Offset(-canvasW * 0.44f, -canvasH * 0.44f),
                    size = Size(canvasW * 0.88f, canvasH * 0.88f),
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    style = Stroke(width = (2f + detail * 3f).dp.toPx())
                )
            }

            EffectKernel.STARBURST -> {
                val rays = 8
                val r = canvasW * (0.28f + detail * 0.25f)
                for (i in 0 until rays) {
                    val a = (i / rays.toFloat()) * 2f * PI.toFloat() + timePhase * 0.5f
                    drawLine(
                        color = fxColor.copy(alpha = 0.30f * intensity),
                        start = Offset.Zero,
                        end = Offset(cos(a) * r, sin(a) * r),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }

            EffectKernel.GLITCH_BLOCK -> {
                val bands = 5
                for (b in 0 until bands) {
                    val y = (-canvasH * 0.4f) + ((b * 0.2f + (timePhase * 0.2f) % 0.2f) * canvasH)
                    val xShift = sin(timePhase * 5f + b * 2f) * 28.dp.toPx() * intensity
                    drawRect(
                        color = if (b % 2 == 0) fxColor.copy(alpha = 0.24f * intensity)
                        else Color(0xFFFF2E63).copy(alpha = 0.22f * intensity),
                        topLeft = Offset(-canvasW * 0.35f + xShift, y),
                        size = Size(canvasW * 0.70f, (8f + detail * 10f).dp.toPx())
                    )
                }
            }

            EffectKernel.PARTICLE_SPARK -> {
                val count = (12 + (detail * 18).toInt()).coerceAtMost(30)
                for (p in 0 until count) {
                    val px = sin(p * 1.9f + timePhase * 0.8f) * canvasW * 0.42f
                    val py = cos(p * 2.3f + timePhase * 0.6f) * canvasH * 0.42f
                    val col = if (p % 2 == 0) fxColor else Color.White
                    drawCircle(
                        color = col.copy(alpha = (0.35f + 0.55f * intensity).coerceAtMost(0.9f)),
                        radius = (1.8f + (p % 3)).dp.toPx(),
                        center = Offset(px, py)
                    )
                }
            }
        }
    }
}
