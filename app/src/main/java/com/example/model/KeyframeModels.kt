package com.example.model

import java.util.UUID

enum class AnimatableProperty(val displayName: String, val unit: String, val defaultValue: Float, val minVal: Float, val maxVal: Float) {
    POSITION_X("Position X", "px", 0f, -500f, 500f),
    POSITION_Y("Position Y", "px", 0f, -500f, 500f),
    SCALE("Scale", "x", 1.0f, 0.1f, 5.0f),
    ROTATION("Rotation", "°", 0f, -360f, 360f),
    OPACITY("Opacity", "%", 1.0f, 0.0f, 1.0f),
    ROTATION_X_3D("3D Pitch X", "°", 18f, -360f, 360f),
    ROTATION_Y_3D("3D Yaw Y", "°", -28f, -360f, 360f),
    ROTATION_Z_3D("3D Roll Z", "°", 0f, -360f, 360f),
    DEPTH_Z_3D("3D Depth Z", "z", 0f, -2.5f, 2.5f),
    EXTRUSION_3D("3D Extrude", "d", 0.40f, 0.05f, 1.5f),
    FOV_3D("Camera FOV", "°", 45f, 20f, 110f)
}

sealed class EasingType(val label: String) {
    object Linear : EasingType("Linear")
    object EaseIn : EasingType("Ease In")
    object EaseOut : EasingType("Ease Out")
    object EaseInOut : EasingType("Ease In-Out")
    data class CubicBezier(
        val p1x: Float = 0.42f,
        val p1y: Float = 0.0f,
        val p2x: Float = 0.58f,
        val p2y: Float = 1.0f
    ) : EasingType("Bezier Graph")
    object Bounce : EasingType("Bounce")
    object Elastic : EasingType("Elastic")
}

data class Keyframe(
    val id: String = UUID.randomUUID().toString(),
    val timeMs: Long,
    val value: Float,
    val easing: EasingType = EasingType.CubicBezier(0.42f, 0.0f, 0.58f, 1.0f)
)

data class KeyframeTrack(
    val property: AnimatableProperty,
    val keyframes: List<Keyframe> = emptyList()
) {
    fun getKeyframeAt(timeMs: Long, toleranceMs: Long = 60L): Keyframe? {
        return keyframes.firstOrNull { kotlin.math.abs(it.timeMs - timeMs) <= toleranceMs }
    }
}
