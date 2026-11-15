package com.example.engine

import com.example.model.AnimatableProperty
import com.example.model.ClipItem
import com.example.model.CurveBreakpoint
import com.example.model.EasingType
import com.example.model.Keyframe
import com.example.model.KeyframeTrack
import com.example.model.Model3DConfig
import com.example.model.TransformState
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

object KeyframeEvaluator {

    fun interpolate(track: KeyframeTrack?, relativeTimeMs: Long, defaultValue: Float): Float {
        if (track == null || track.keyframes.isEmpty()) return defaultValue
        val keyframes = track.keyframes.sortedBy { it.timeMs }

        if (relativeTimeMs <= keyframes.first().timeMs) {
            return keyframes.first().value
        }
        if (relativeTimeMs >= keyframes.last().timeMs) {
            return keyframes.last().value
        }

        // Find the bounding segment
        for (i in 0 until keyframes.size - 1) {
            val kA = keyframes[i]
            val kB = keyframes[i + 1]
            if (relativeTimeMs in kA.timeMs..kB.timeMs) {
                val span = (kB.timeMs - kA.timeMs).toFloat()
                if (span <= 0f) return kA.value
                val rawProgress = ((relativeTimeMs - kA.timeMs) / span).coerceIn(0f, 1f)
                val easedProgress = evaluateEasing(rawProgress, kA.easing)
                return kA.value + easedProgress * (kB.value - kA.value)
            }
        }
        return defaultValue
    }

    fun evaluateEasing(t: Float, easing: EasingType): Float {
        return when (easing) {
            is EasingType.Linear -> t
            is EasingType.EaseIn -> t * t * t
            is EasingType.EaseOut -> 1f - (1f - t).pow(3)
            is EasingType.EaseInOut -> {
                if (t < 0.5f) 4f * t * t * t
                else 1f - (-2f * t + 2f).pow(3) / 2f
            }
            is EasingType.CubicBezier -> solveCubicBezier(t, easing.p1x, easing.p1y, easing.p2x, easing.p2y)
            is EasingType.MultiPoint -> evaluateMultiPoint(t, easing.points)
            is EasingType.Bounce -> evaluateBounce(t)
            is EasingType.Elastic -> evaluateElastic(t)
        }
    }

    fun evaluateMultiPoint(t: Float, breakpoints: List<CurveBreakpoint>): Float {
        val clampedT = t.coerceIn(0f, 1f)
        if (clampedT <= 0f) return 0f
        if (clampedT >= 1f) return 1f
        val interior = breakpoints
            .map { CurveBreakpoint(it.x.coerceIn(0.01f, 0.99f), it.y.coerceIn(-0.4f, 1.4f)) }
            .sortedBy { it.x }
        val anchors = listOf(CurveBreakpoint(0f, 0f)) + interior + listOf(CurveBreakpoint(1f, 1f))
        for (i in 0 until anchors.size - 1) {
            val a = anchors[i]
            val b = anchors[i + 1]
            if (clampedT <= b.x || i == anchors.size - 2) {
                val span = (b.x - a.x).coerceAtLeast(1e-4f)
                val localT = ((clampedT - a.x) / span).coerceIn(0f, 1f)
                // Smooth Hermite blend inside segment so curve passes through every breakpoint seamlessly
                val smooth = localT * localT * (3f - 2f * localT)
                return a.y + (b.y - a.y) * smooth
            }
        }
        return clampedT
    }

    fun mapTimelineToSourceTimeMs(clip: ClipItem, globalTimeMs: Long): Long {
        val localMs = (globalTimeMs - clip.startTimeMs).coerceIn(0L, clip.durationMs)
        val safeSpeed = clip.speed.coerceIn(0.1f, 8.0f)
        val totalSourceSpan = (clip.durationMs * safeSpeed).toLong().coerceAtLeast(1L)
        val scaledMs = (localMs * safeSpeed).toLong().coerceIn(0L, totalSourceSpan)
        return if (clip.isReversed) {
            (totalSourceSpan - scaledMs).coerceIn(0L, totalSourceSpan)
        } else {
            scaledMs
        }
    }

    /**
     * Solves parametric Cubic Bezier for x = t, then returns y(u).
     * B(u) = (1-u)^3*P0 + 3(1-u)^2*u*P1 + 3(1-u)*u^2*P2 + u^3*P3
     * where P0=(0,0), P3=(1,1).
     */
    fun solveCubicBezier(xTarget: Float, p1x: Float, p1y: Float, p2x: Float, p2y: Float): Float {
        if (xTarget <= 0f) return 0f
        if (xTarget >= 1f) return 1f

        // Newton-Raphson iteration with bisection fallback
        var u = xTarget
        for (iter in 0 until 8) {
            val currentX = sampleCubicBezier(u, p1x, p2x) - xTarget
            if (abs(currentX) < 1e-4f) break
            val dX = sampleCubicBezierDerivative(u, p1x, p2x)
            if (abs(dX) < 1e-4f) break
            u -= currentX / dX
            u = u.coerceIn(0f, 1f)
        }
        return sampleCubicBezier(u, p1y, p2y)
    }

    private fun sampleCubicBezier(u: Float, p1: Float, p2: Float): Float {
        // (1-u)^3*0 + 3(1-u)^2*u*p1 + 3(1-u)*u^2*p2 + u^3*1
        val oneMinusU = 1f - u
        return 3f * oneMinusU * oneMinusU * u * p1 +
               3f * oneMinusU * u * u * p2 +
               u * u * u
    }

    private fun sampleCubicBezierDerivative(u: Float, p1: Float, p2: Float): Float {
        val oneMinusU = 1f - u
        return 3f * oneMinusU * oneMinusU * p1 +
               6f * oneMinusU * u * (p2 - p1) +
               3f * u * u * (1f - p2)
    }

    private fun evaluateBounce(t: Float): Float {
        var n1 = t
        if (n1 < (1 / 2.75f)) {
            return 7.5625f * n1 * n1
        } else if (n1 < (2 / 2.75f)) {
            n1 -= (1.5f / 2.75f)
            return 7.5625f * n1 * n1 + 0.75f
        } else if (n1 < (2.5 / 2.75f)) {
            n1 -= (2.25f / 2.75f)
            return 7.5625f * n1 * n1 + 0.9375f
        } else {
            n1 -= (2.625f / 2.75f)
            return 7.5625f * n1 * n1 + 0.984375f
        }
    }

    private fun evaluateElastic(t: Float): Float {
        if (t == 0f || t == 1f) return t
        val p = 0.3f
        val s = p / 4f
        return (2.0.pow(-10.0 * t) * sin((t - s) * (2 * Math.PI) / p) + 1.0).toFloat()
    }

    /**
     * Computes the effective TransformState for a clip at a given global timecode.
     */
    fun evaluateClipTransform(clip: ClipItem, currentTimeMs: Long): TransformState {
        val relTime = currentTimeMs - clip.startTimeMs
        val posX = interpolate(clip.keyframeTracks[AnimatableProperty.POSITION_X], relTime, clip.baseTransform.positionX)
        val posY = interpolate(clip.keyframeTracks[AnimatableProperty.POSITION_Y], relTime, clip.baseTransform.positionY)
        val scale = interpolate(clip.keyframeTracks[AnimatableProperty.SCALE], relTime, clip.baseTransform.scaleX)
        val rot = interpolate(clip.keyframeTracks[AnimatableProperty.ROTATION], relTime, clip.baseTransform.rotation)
        val op = interpolate(clip.keyframeTracks[AnimatableProperty.OPACITY], relTime, clip.baseTransform.opacity)

        return TransformState(
            positionX = posX,
            positionY = posY,
            scaleX = scale,
            scaleY = scale,
            rotation = rot,
            opacity = op.coerceIn(0f, 1f)
        )
    }

    fun evaluateModel3D(clip: ClipItem, currentTimeMs: Long): Model3DConfig? {
        val base = clip.model3dConfig ?: return null
        val relTime = currentTimeMs - clip.startTimeMs
        val rx = interpolate(clip.keyframeTracks[AnimatableProperty.ROTATION_X_3D], relTime, base.rotationX)
        val ry = interpolate(clip.keyframeTracks[AnimatableProperty.ROTATION_Y_3D], relTime, base.rotationY)
        val rz = interpolate(clip.keyframeTracks[AnimatableProperty.ROTATION_Z_3D], relTime, base.rotationZ)
        val dz = interpolate(clip.keyframeTracks[AnimatableProperty.DEPTH_Z_3D], relTime, base.depthZ)
        val ext = interpolate(clip.keyframeTracks[AnimatableProperty.EXTRUSION_3D], relTime, base.extrusionDepth)
        val fov = interpolate(clip.keyframeTracks[AnimatableProperty.FOV_3D], relTime, base.fovDegrees)
        return base.copy(
            rotationX = rx,
            rotationY = ry,
            rotationZ = rz,
            depthZ = dz.coerceIn(AnimatableProperty.DEPTH_Z_3D.minVal, AnimatableProperty.DEPTH_Z_3D.maxVal),
            extrusionDepth = ext.coerceIn(AnimatableProperty.EXTRUSION_3D.minVal, AnimatableProperty.EXTRUSION_3D.maxVal),
            fovDegrees = fov.coerceIn(AnimatableProperty.FOV_3D.minVal, AnimatableProperty.FOV_3D.maxVal)
        )
    }

    fun evaluateProperty(clip: ClipItem, property: AnimatableProperty, currentTimeMs: Long): Float {
        val t = evaluateClipTransform(clip, currentTimeMs)
        val m = evaluateModel3D(clip, currentTimeMs)
        return when (property) {
            AnimatableProperty.POSITION_X -> t.positionX
            AnimatableProperty.POSITION_Y -> t.positionY
            AnimatableProperty.SCALE -> t.scaleX
            AnimatableProperty.ROTATION -> t.rotation
            AnimatableProperty.OPACITY -> t.opacity
            AnimatableProperty.ROTATION_X_3D -> m?.rotationX ?: property.defaultValue
            AnimatableProperty.ROTATION_Y_3D -> m?.rotationY ?: property.defaultValue
            AnimatableProperty.ROTATION_Z_3D -> m?.rotationZ ?: property.defaultValue
            AnimatableProperty.DEPTH_Z_3D -> m?.depthZ ?: property.defaultValue
            AnimatableProperty.EXTRUSION_3D -> m?.extrusionDepth ?: property.defaultValue
            AnimatableProperty.FOV_3D -> m?.fovDegrees ?: property.defaultValue
        }
    }
}
