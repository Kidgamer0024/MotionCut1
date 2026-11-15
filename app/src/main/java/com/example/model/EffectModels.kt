package com.example.model

import java.util.UUID

enum class EffectSuite(val displayName: String, val shortName: String, val accentHex: Long) {
    ALL("All Suites", "All", 0xFF00E5FF),
    ALIGHT_MOTION("Alight Motion", "Alight", 0xFF00E5FF),
    GOLD_MOTION("Gold Motion", "Gold", 0xFFFFB020),
    ULTRA_MOTION("Ultra Motion", "Ultra", 0xFFD580FF),
    NODE_VIDEO("Node Video", "Node", 0xFF10E878),
    CAPCUT("CapCut", "CapCut", 0xFFFF4D6D)
}

enum class EffectKernel {
    CHROMATIC_ABERRATION,
    GLOW,
    MOTION_BLUR,
    WAVE_WARP,
    FILM_GRAIN,
    SCANLINES,
    LENS_DISTORTION,
    CAMERA_SHAKE,
    VIGNETTE,
    PIXELATE,
    KALEIDOSCOPE,
    PRISM_SHIFT,
    COLOR_PULSE,
    HALFTONE,
    NEON_EDGES,
    LIGHT_SWEEP,
    STARBURST,
    MIRROR_SPLIT,
    GLITCH_BLOCK,
    PARTICLE_SPARK;

    fun toLegacyEffectType(): EffectType = when (this) {
        CHROMATIC_ABERRATION, PRISM_SHIFT, GLITCH_BLOCK -> EffectType.CHROMATIC_ABERRATION
        GLOW, LIGHT_SWEEP, STARBURST, PARTICLE_SPARK -> EffectType.GLOW
        MOTION_BLUR -> EffectType.MOTION_BLUR
        WAVE_WARP, KALEIDOSCOPE, MIRROR_SPLIT -> EffectType.WAVE_WARP
        FILM_GRAIN, VIGNETTE, COLOR_PULSE -> EffectType.FILM_GRAIN
        SCANLINES, PIXELATE, HALFTONE, NEON_EDGES -> EffectType.SCANLINES
        LENS_DISTORTION -> EffectType.LENS_DISTORTION
        CAMERA_SHAKE -> EffectType.CAMERA_SHAKE
    }
}

enum class EffectType(val displayName: String, val category: String, val iconDescription: String) {
    CHROMATIC_ABERRATION("RGB Split", "Distortion", "Separates red and cyan channels for modern glitch look"),
    GLOW("Diffusion Glow", "Light & Glow", "Cinematic bloom and soft highlight haze"),
    MOTION_BLUR("Directional Blur", "Blur", "Dynamic velocity blur along directional vectors"),
    WAVE_WARP("Wave Warp", "Distortion", "Liquid oscillation wave distortion"),
    FILM_GRAIN("35mm Film Grain", "Retro", "Authentic analog cinematic silver halide grain"),
    SCANLINES("CRT Scanlines", "Retro", "Vintage arcade cathode ray monitor lines"),
    LENS_DISTORTION("Anamorphic Lens", "3D & Camera", "Curved glass optical barrel distortion"),
    CAMERA_SHAKE("Action Cam Shake", "3D & Camera", "Handheld chaotic camera turbulence");

    fun toKernel(): EffectKernel = when (this) {
        CHROMATIC_ABERRATION -> EffectKernel.CHROMATIC_ABERRATION
        GLOW -> EffectKernel.GLOW
        MOTION_BLUR -> EffectKernel.MOTION_BLUR
        WAVE_WARP -> EffectKernel.WAVE_WARP
        FILM_GRAIN -> EffectKernel.FILM_GRAIN
        SCANLINES -> EffectKernel.SCANLINES
        LENS_DISTORTION -> EffectKernel.LENS_DISTORTION
        CAMERA_SHAKE -> EffectKernel.CAMERA_SHAKE
    }
}

data class EffectPreset(
    val id: String,
    val name: String,
    val suite: EffectSuite,
    val category: String,
    val kernel: EffectKernel,
    val description: String,
    val defaultIntensity: Float = 0.60f,
    val defaultSpeed: Float = 1.0f,
    val defaultParamA: Float = 0.50f,
    val defaultParamB: Float = 0.50f,
    val paramALabel: String = "Detail",
    val paramBLabel: String = "Phase",
    val tags: List<String> = emptyList()
)

data class VideoEffect(
    val id: String = UUID.randomUUID().toString(),
    val type: EffectType,
    val isEnabled: Boolean = true,
    val intensity: Float = 0.5f,
    val speed: Float = 1.0f,
    val presetId: String? = null,
    val customName: String? = null,
    val suite: EffectSuite? = null,
    val category: String? = null,
    val kernel: EffectKernel? = null,
    val paramA: Float = 0.5f,
    val paramB: Float = 0.5f,
    val paramALabel: String = "Detail",
    val paramBLabel: String = "Phase"
) {
    val effectiveName: String
        get() = customName ?: type.displayName

    val effectiveCategory: String
        get() = category ?: type.category

    val effectiveKernel: EffectKernel
        get() = kernel ?: type.toKernel()
}
