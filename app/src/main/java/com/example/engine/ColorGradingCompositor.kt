package com.example.engine

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import com.example.model.ColorGradingSettings
import com.example.model.ColorWheelValue
import com.example.model.LutPreset
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object ColorGradingCompositor {

    fun createColorFilter(settings: ColorGradingSettings): ColorFilter {
        val matrix = createColorMatrix(settings)
        return ColorFilter.colorMatrix(matrix)
    }

    fun createColorMatrix(settings: ColorGradingSettings): ColorMatrix {
        val cm = ColorMatrix()

        // 1. Exposure & Contrast
        val expScale = Math.pow(2.0, settings.exposure.toDouble()).toFloat()
        val contrast = settings.contrast
        val contrastOffset = 128f * (1f - contrast)

        // 2. Temperature (-100..100) and Tint (-100..100)
        val tempNorm = settings.temperature / 100f
        val tintNorm = settings.tint / 100f
        val rTemp = 1f + tempNorm * 0.35f + tintNorm * 0.15f
        val gTemp = 1f - tintNorm * 0.25f
        val bTemp = 1f - tempNorm * 0.35f + tintNorm * 0.15f

        // 3. 3-Way Wheels (Lift, Gamma, Gain, Offset)
        val liftRgb = wheelToRgbShift(settings.lift)
        val gainRgb = wheelToRgbScale(settings.gain)
        val offsetRgb = wheelToRgbShift(settings.offset)

        // 4. Combined primary channel scaling
        var rScale = expScale * contrast * rTemp * gainRgb[0]
        var gScale = expScale * contrast * gTemp * gainRgb[1]
        var bScale = expScale * contrast * bTemp * gainRgb[2]

        var rOffset = contrastOffset + (liftRgb[0] + offsetRgb[0]) * 80f + settings.shadows * 0.4f
        var gOffset = contrastOffset + (liftRgb[1] + offsetRgb[1]) * 80f + settings.shadows * 0.4f
        var bOffset = contrastOffset + (liftRgb[2] + offsetRgb[2]) * 80f + settings.shadows * 0.4f

        // 5. LUT Pre-scaling
        when (settings.lutPreset) {
            LutPreset.NONE -> {}
            LutPreset.TEAL_AND_ORANGE -> {
                rScale *= (1.0f + 0.22f * settings.lutIntensity)
                gScale *= (1.0f + 0.05f * settings.lutIntensity)
                bScale *= (1.0f - 0.20f * settings.lutIntensity)
                bOffset += 18f * settings.lutIntensity
                rOffset += 6f * settings.lutIntensity
            }
            LutPreset.CYBER_NEON -> {
                rScale *= (1.0f + 0.30f * settings.lutIntensity)
                gScale *= (1.0f - 0.12f * settings.lutIntensity)
                bScale *= (1.0f + 0.40f * settings.lutIntensity)
                bOffset += 24f * settings.lutIntensity
            }
            LutPreset.BLEACH_BYPASS -> {
                rScale *= (1.0f + 0.25f * settings.lutIntensity)
                gScale *= (1.0f + 0.25f * settings.lutIntensity)
                bScale *= (1.0f + 0.25f * settings.lutIntensity)
                rOffset -= 20f * settings.lutIntensity
                gOffset -= 20f * settings.lutIntensity
                bOffset -= 20f * settings.lutIntensity
            }
            LutPreset.KODACHROME -> {
                rScale *= (1.0f + 0.28f * settings.lutIntensity)
                gScale *= (1.0f + 0.12f * settings.lutIntensity)
                bScale *= (1.0f - 0.15f * settings.lutIntensity)
                rOffset += 12f * settings.lutIntensity
            }
            LutPreset.FUJI_ETERNA -> {
                rScale *= (1.0f - 0.05f * settings.lutIntensity)
                gScale *= (1.0f + 0.08f * settings.lutIntensity)
                bScale *= (1.0f + 0.05f * settings.lutIntensity)
                rOffset += 14f * settings.lutIntensity
                gOffset += 16f * settings.lutIntensity
                bOffset += 18f * settings.lutIntensity
            }
            LutPreset.GOLDEN_HOUR -> {
                rScale *= (1.0f + 0.35f * settings.lutIntensity)
                gScale *= (1.0f + 0.20f * settings.lutIntensity)
                bScale *= (1.0f - 0.25f * settings.lutIntensity)
                rOffset += 20f * settings.lutIntensity
                gOffset += 10f * settings.lutIntensity
            }
            LutPreset.BLACK_AND_GOLD -> {
                rScale *= (1.0f + 0.30f * settings.lutIntensity)
                gScale *= (1.0f + 0.18f * settings.lutIntensity)
                bScale *= (1.0f - 0.35f * settings.lutIntensity)
                rOffset -= 15f * settings.lutIntensity
                gOffset -= 15f * settings.lutIntensity
                bOffset -= 15f * settings.lutIntensity
            }
        }

        // Apply primary color matrix
        val primaryMatrix = floatArrayOf(
            rScale, 0f, 0f, 0f, rOffset,
            0f, gScale, 0f, 0f, gOffset,
            0f, 0f, bScale, 0f, bOffset,
            0f, 0f, 0f, 1f, 0f
        )
        cm.set(ColorMatrix(primaryMatrix))

        // 6. Saturation and Vibrance adjustment
        var sat = settings.saturation
        if (settings.lutPreset == LutPreset.BLEACH_BYPASS) {
            sat *= (1.0f - 0.45f * settings.lutIntensity)
        }
        val satMatrix = ColorMatrix()
        satMatrix.setToSaturation(sat.coerceAtLeast(0f))
        cm.timesAssign(satMatrix)

        return cm
    }

    private fun wheelToRgbShift(wheel: ColorWheelValue): FloatArray {
        if (wheel.saturation <= 0.001f && wheel.luminance == 0f) return floatArrayOf(0f, 0f, 0f)
        val rad = Math.toRadians(wheel.hue.toDouble())
        val x = (cos(rad) * wheel.saturation).toFloat()
        val y = (sin(rad) * wheel.saturation).toFloat()

        // Map 2D wheel to RGB
        val r = (x * 0.8f + wheel.luminance * 0.5f).coerceIn(-1f, 1f)
        val g = (-0.4f * x + 0.7f * y + wheel.luminance * 0.5f).coerceIn(-1f, 1f)
        val b = (-0.4f * x - 0.7f * y + wheel.luminance * 0.5f).coerceIn(-1f, 1f)
        return floatArrayOf(r, g, b)
    }

    private fun wheelToRgbScale(wheel: ColorWheelValue): FloatArray {
        val shift = wheelToRgbShift(wheel)
        return floatArrayOf(
            (1.0f + shift[0] * 0.5f).coerceAtLeast(0.1f),
            (1.0f + shift[1] * 0.5f).coerceAtLeast(0.1f),
            (1.0f + shift[2] * 0.5f).coerceAtLeast(0.1f)
        )
    }

    /**
     * Synthesizes 64-sample Waveform & Histogram levels for Scopes HUD.
     */
    fun computeScopeSamples(settings: ColorGradingSettings, sampleCount: Int = 64): ScopeData {
        val lumaSamples = FloatArray(sampleCount)
        val rSamples = FloatArray(sampleCount)
        val gSamples = FloatArray(sampleCount)
        val bSamples = FloatArray(sampleCount)

        val exp = settings.exposure
        val contrast = settings.contrast
        val temp = settings.temperature / 100f
        val sat = settings.saturation

        for (i in 0 until sampleCount) {
            val progress = i.toFloat() / (sampleCount - 1)
            // Base gradient curve
            val base = progress.toDouble().let { Math.pow(it, 1.0 / contrast.toDouble()) }.toFloat()
            val rVal = (base * (1f + temp * 0.2f) * (1f + exp * 0.25f)).coerceIn(0.05f, 0.98f)
            val gVal = (base * (1f + exp * 0.25f)).coerceIn(0.05f, 0.98f)
            val bVal = (base * (1f - temp * 0.2f) * (1f + exp * 0.25f)).coerceIn(0.05f, 0.98f)

            rSamples[i] = rVal
            gSamples[i] = gVal
            bSamples[i] = bVal
            lumaSamples[i] = (0.299f * rVal + 0.587f * gVal + 0.114f * bVal).coerceIn(0.02f, 1.0f)
        }

        return ScopeData(
            lumaWaveform = lumaSamples,
            rParade = rSamples,
            gParade = gSamples,
            bParade = bSamples
        )
    }
}

data class ScopeData(
    val lumaWaveform: FloatArray,
    val rParade: FloatArray,
    val gParade: FloatArray,
    val bParade: FloatArray
)
