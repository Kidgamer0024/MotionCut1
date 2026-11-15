package com.example.model

import java.util.UUID

enum class ClipType(val label: String) {
    VIDEO_SAMPLE("Video"),
    COLOR_BG("Solid Background"),
    TEXT("Kinetic Text"),
    SHAPE("Vector Shape"),
    AUDIO("Audio Track"),
    ADJUSTMENT("Adjustment Layer"),
    NULL_OBJECT("Null Object"),
    MODEL_3D("3D Model")
}

enum class ShapeType(val label: String) {
    CIRCLE("Circle"),
    SQUARE("Square"),
    ROUNDED("Rounded"),
    RECTANGLE("Rectangle"),
    TRIANGLE("Triangle"),
    DIAMOND("Diamond"),
    PENTAGON("Pentagon"),
    HEXAGON("Hexagon"),
    OCTAGON("Octagon"),
    STAR("Star"),
    STAR_6("6-point star"),
    SPARKLE("Sparkle"),
    QUAD("Quad"),
    IRREGULAR("Irregular"),
    MULTIFOIL("Multifoil"),
    PLUS("Plus"),
    TEARDROP("Teardrop"),
    PIE("Pie"),
    ARC("Arc"),
    MOON("Moon"),
    STAMP("Stamp"),
    CALLOUT("Callout"),
    POLYGON("Hexagon"),
    HEART("Heart")
}

data class ShapeConfig(
    val type: ShapeType = ShapeType.RECTANGLE,
    val fillColor: Long = 0xFF00E5FF,
    val strokeColor: Long = 0xFFFFFFFF,
    val strokeWidth: Float = 4f,
    val cornerRadius: Float = 16f
)

data class TextConfig(
    val text: String = "MOTION CUT",
    val fontSizeSp: Float = 32f,
    val textColor: Long = 0xFFFFFFFF,
    val isBold: Boolean = true,
    val strokeColor: Long = 0xFF000000,
    val strokeWidth: Float = 2f,
    val letterSpacing: Float = 2f
)

data class TransformState(
    val positionX: Float = 0f,
    val positionY: Float = 0f,
    val scaleX: Float = 1.0f,
    val scaleY: Float = 1.0f,
    val rotation: Float = 0f,
    val opacity: Float = 1.0f
)

data class ClipItem(
    val id: String = UUID.randomUUID().toString(),
    val trackId: String = "",
    val title: String,
    val type: ClipType = ClipType.VIDEO_SAMPLE,
    val assetResId: Int? = null,
    val mediaUri: String? = null,
    val colorHex: Long = 0xFF121722,
    val startTimeMs: Long,
    val durationMs: Long,
    val speed: Float = 1.0f,
    val volume: Float = 1.0f,
    val isLocked: Boolean = false,
    val baseTransform: TransformState = TransformState(),
    val keyframeTracks: Map<AnimatableProperty, KeyframeTrack> = emptyMap(),
    val colorGrading: ColorGradingSettings = ColorGradingSettings(),
    val effects: List<VideoEffect> = emptyList(),
    val masks: List<MaskConfig> = emptyList(),
    val textConfig: TextConfig? = null,
    val shapeConfig: ShapeConfig? = null,
    val model3dConfig: Model3DConfig? = null,
    val waveformSamples: List<Float> = emptyList()
) {
    val endTimeMs: Long
        get() = startTimeMs + durationMs

    fun hasKeyframeAt(property: AnimatableProperty, timeMs: Long): Boolean {
        if (timeMs !in startTimeMs..endTimeMs) return false
        val relTime = (timeMs - startTimeMs).coerceIn(0L, durationMs)
        return keyframeTracks[property]?.getKeyframeAt(relTime, toleranceMs = 80L) != null
    }

    fun hasAnyKeyframeAt(timeMs: Long): Boolean {
        if (timeMs !in startTimeMs..endTimeMs) return false
        val relTime = (timeMs - startTimeMs).coerceIn(0L, durationMs)
        return keyframeTracks.values.any { it.getKeyframeAt(relTime, toleranceMs = 80L) != null }
    }
}
