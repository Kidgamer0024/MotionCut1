package com.example.model

data class ColorWheelValue(
    val hue: Float = 0f,          // 0 to 360 degrees
    val saturation: Float = 0f,   // 0.0 to 1.0 (distance from center)
    val luminance: Float = 0f     // -1.0 to 1.0
)

data class CurvePoint(
    val x: Float, // 0.0 to 1.0 input
    val y: Float  // 0.0 to 1.0 output
)

data class ChannelCurve(
    val points: List<CurvePoint> = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f))
)

enum class HslBand(val label: String, val baseColorHex: Long) {
    RED("Red", 0xFFFF3B30),
    ORANGE("Orange", 0xFFFF9500),
    YELLOW("Yellow", 0xFFFFCC00),
    GREEN("Green", 0xFF34C759),
    CYAN("Cyan", 0xFF00C7BE),
    BLUE("Blue", 0xFF007AFF),
    PURPLE("Purple", 0xFF5856D6),
    MAGENTA("Magenta", 0xFFAF52DE)
}

data class HslAdjustment(
    val hueShift: Float = 0f,    // -180 to +180 deg
    val saturation: Float = 0f,  // -100 to +100 %
    val luminance: Float = 0f    // -100 to +100 %
)

enum class LutPreset(val displayName: String, val previewColor: Long, val description: String) {
    NONE("Rec.709 Natural", 0xFF708090, "Standard neutral television broadcast color space"),
    TEAL_AND_ORANGE("Teal & Orange", 0xFF00B4D8, "Hollywood blockbuster contrasting skin tones and shadows"),
    CYBER_NEON("Ultraviolet Cinema", 0xFFE040FB, "Electric ultraviolet highlights and cool studio shadows"),
    BLEACH_BYPASS("Bleach Bypass", 0xFFB0BEC5, "High contrast, silver-retained desaturated gritty action look"),
    KODACHROME("Kodachrome 64", 0xFFFF8A65, "Rich warm vintage 1970s magazine film stock"),
    FUJI_ETERNA("Fuji Eterna", 0xFF4DB6AC, "Soft pastel highlights, subtle shadows, organic film texture"),
    GOLDEN_HOUR("Golden Hour", 0xFFFFB300, "Warm glowing sunset rays with lifted bronze shadows"),
    BLACK_AND_GOLD("Black & Gold", 0xFFCFB53B, "Luxury moody cinematic low-key with rich champagne glow")
}

data class ColorGradingSettings(
    // 3-Way Color Wheels
    val lift: ColorWheelValue = ColorWheelValue(),
    val gamma: ColorWheelValue = ColorWheelValue(),
    val gain: ColorWheelValue = ColorWheelValue(),
    val offset: ColorWheelValue = ColorWheelValue(),

    // Primary Adjustments
    val exposure: Float = 0f,         // -2.0 to +2.0 EV
    val contrast: Float = 1.0f,       // 0.5 to 2.0
    val temperature: Float = 0f,     // -100 to +100 (Cool blue to Warm amber)
    val tint: Float = 0f,            // -100 to +100 (Green to Magenta)
    val highlights: Float = 0f,      // -100 to +100
    val shadows: Float = 0f,         // -100 to +100
    val vibrance: Float = 0f,        // -100 to +100
    val saturation: Float = 1.0f,     // 0.0 to 2.0
    val vignette: Float = 0f,        // 0 to 100%
    val sharpness: Float = 0f,       // 0 to 100%

    // Curves (Master, Red, Green, Blue)
    val masterCurve: ChannelCurve = ChannelCurve(),
    val redCurve: ChannelCurve = ChannelCurve(),
    val greenCurve: ChannelCurve = ChannelCurve(),
    val blueCurve: ChannelCurve = ChannelCurve(),

    // HSL Secondary Color Grading
    val hslAdjustments: Map<HslBand, HslAdjustment> = HslBand.values().associateWith { HslAdjustment() },

    // Active LUT preset
    val lutPreset: LutPreset = LutPreset.NONE,
    val lutIntensity: Float = 1.0f
)
