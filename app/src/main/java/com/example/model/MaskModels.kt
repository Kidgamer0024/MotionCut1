package com.example.model

import java.util.UUID

enum class MaskShapeType(val displayName: String, val iconName: String) {
    RECTANGLE("Rectangle", "Square bounds with corner radius"),
    CIRCLE("Circle", "Uniform circular mask"),
    ELLIPSE("Oval / Ellipse", "Smooth elliptical soft focus"),
    LINEAR_SPLIT("Linear Gradient", "Directional split wipe mask"),
    RADIAL_VIGNETTE("Radial Spotlight", "Circular spotlight vignette mask"),
    FREEFORM_POLYGON("Custom Polygon", "Multi-point vector polygon path")
}

data class MaskVertex(
    val id: String = UUID.randomUUID().toString(),
    val x: Float, // relative to clip center in px
    val y: Float
)

data class MaskConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Mask 1",
    val shapeType: MaskShapeType = MaskShapeType.RECTANGLE,
    val isEnabled: Boolean = true,
    val isInverted: Boolean = false,
    val featherPx: Float = 0f,       // 0 to 50px soft edge
    val opacity: Float = 1.0f,       // 0 to 1
    val expansion: Float = 0f,       // -100 to +100 px
    val cornerRadiusPx: Float = 16f, // 0 to 160 px
    val aspectWidth: Float = 1.0f,   // 0.25 to 2.5
    val aspectHeight: Float = 1.0f,  // 0.25 to 2.5
    val borderWidthPx: Float = 0f,   // 0 to 24 px
    val borderColorHex: Long = 0xFFFFFFFF,
    // Transform parameters
    val positionX: Float = 0f,
    val positionY: Float = 0f,
    val scaleX: Float = 1.0f,
    val scaleY: Float = 1.0f,
    val rotation: Float = 0f,
    // Custom vertices for polygon mask
    val vertices: List<MaskVertex> = listOf(
        MaskVertex(x = -80f, y = -80f),
        MaskVertex(x = 80f, y = -80f),
        MaskVertex(x = 100f, y = 60f),
        MaskVertex(x = -100f, y = 60f)
    ),
    // Animated mask keyframes
    val isAnimated: Boolean = false,
    val keyframes: List<MaskKeyframe> = emptyList()
)

data class MaskKeyframe(
    val id: String = UUID.randomUUID().toString(),
    val timeMs: Long,
    val positionX: Float,
    val positionY: Float,
    val scale: Float,
    val rotation: Float,
    val featherPx: Float
)
