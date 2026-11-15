package com.example.data

import com.example.model.EffectKernel
import com.example.model.EffectPreset
import com.example.model.EffectSuite
import java.util.Locale

/**
 * Curated library of practical, distinct video VFX presets.
 */
object EffectCatalog {

    val categories: List<String> = listOf(
        "All",
        "Light & Glow",
        "Blur & Focus",
        "Color & Look",
        "Glitch & RGB",
        "Camera & Shake",
        "Lens & Warp",
        "Retro & Film",
        "Stylize & Matte"
    )

    val allPresets: List<EffectPreset> by lazy {
        listOf(
            // --- LIGHT & GLOW ---
            EffectPreset(
                id = "fx_optical_glow",
                name = "Optical Diffusion Glow",
                suite = EffectSuite.NODE_VIDEO,
                category = "Light & Glow",
                kernel = EffectKernel.GLOW,
                description = "Soft cinematic highlight bloom and atmospheric halation",
                defaultIntensity = 0.55f,
                defaultSpeed = 0.9f,
                defaultParamA = 0.65f,
                defaultParamB = 0.35f,
                paramALabel = "Radius",
                paramBLabel = "Pulse",
                tags = listOf("glow", "bloom", "halation", "dream")
            ),
            EffectPreset(
                id = "fx_light_sweep",
                name = "Anamorphic Light Sweep",
                suite = EffectSuite.GOLD_MOTION,
                category = "Light & Glow",
                kernel = EffectKernel.LIGHT_SWEEP,
                description = "Diagonal metallic light beam sweeping smoothly across titles and clips",
                defaultIntensity = 0.72f,
                defaultSpeed = 1.0f,
                defaultParamA = 0.45f,
                defaultParamB = 0.25f,
                paramALabel = "Beam Width",
                paramBLabel = "Offset",
                tags = listOf("sweep", "shine", "flare", "title")
            ),
            EffectPreset(
                id = "fx_gold_rim_bloom",
                name = "Golden Rim Sheen",
                suite = EffectSuite.GOLD_MOTION,
                category = "Light & Glow",
                kernel = EffectKernel.STARBURST,
                description = "Warm radial light rays and specular highlight catch",
                defaultIntensity = 0.58f,
                defaultSpeed = 0.8f,
                defaultParamA = 0.55f,
                defaultParamB = 0.40f,
                paramALabel = "Ray Span",
                paramBLabel = "Rotation",
                tags = listOf("rays", "sun", "gold", "starburst")
            ),
            EffectPreset(
                id = "fx_atmospheric_dust",
                name = "Floating Dust & Bokeh",
                suite = EffectSuite.CAPCUT,
                category = "Light & Glow",
                kernel = EffectKernel.PARTICLE_SPARK,
                description = "Subtle drifting atmospheric embers and micro bokeh motes",
                defaultIntensity = 0.60f,
                defaultSpeed = 0.75f,
                defaultParamA = 0.50f,
                defaultParamB = 0.40f,
                paramALabel = "Density",
                paramBLabel = "Drift",
                tags = listOf("particles", "dust", "bokeh", "sparks")
            ),

            // --- BLUR & FOCUS ---
            EffectPreset(
                id = "fx_directional_blur",
                name = "Directional Whip Blur",
                suite = EffectSuite.ALIGHT_MOTION,
                category = "Blur & Focus",
                kernel = EffectKernel.MOTION_BLUR,
                description = "Directional speed streaks ideal for fast camera pans and beat transitions",
                defaultIntensity = 0.65f,
                defaultSpeed = 1.3f,
                defaultParamA = 0.60f,
                defaultParamB = 0.50f,
                paramALabel = "Streak",
                paramBLabel = "Phase",
                tags = listOf("blur", "whip", "motion", "speed")
            ),
            EffectPreset(
                id = "fx_soft_focus_vignette",
                name = "Portrait Edge Focus",
                suite = EffectSuite.CAPCUT,
                category = "Blur & Focus",
                kernel = EffectKernel.VIGNETTE,
                description = "Draws viewer attention inward with smooth radial corner falloff",
                defaultIntensity = 0.55f,
                defaultSpeed = 1.0f,
                defaultParamA = 0.45f,
                defaultParamB = 0.50f,
                paramALabel = "Tightness",
                paramBLabel = "Falloff",
                tags = listOf("vignette", "focus", "portrait", "spotlight")
            ),
            EffectPreset(
                id = "fx_privacy_mosaic",
                name = "Censor / Mosaic Blur",
                suite = EffectSuite.CAPCUT,
                category = "Blur & Focus",
                kernel = EffectKernel.PIXELATE,
                description = "Clean square mosaic grid useful for faces, plates, or stylized pixel art",
                defaultIntensity = 0.68f,
                defaultSpeed = 1.0f,
                defaultParamA = 0.55f,
                defaultParamB = 0.50f,
                paramALabel = "Block Size",
                paramBLabel = "Grid Alpha",
                tags = listOf("pixelate", "mosaic", "censor", "privacy")
            ),

            // --- COLOR & LOOK ---
            EffectPreset(
                id = "fx_beat_color_pulse",
                name = "Harmonic Color Pulse",
                suite = EffectSuite.ULTRA_MOTION,
                category = "Color & Look",
                kernel = EffectKernel.COLOR_PULSE,
                description = "Smooth ambient spectrum wash synced cleanly over dark or neutral clips",
                defaultIntensity = 0.48f,
                defaultSpeed = 0.9f,
                defaultParamA = 0.50f,
                defaultParamB = 0.25f,
                paramALabel = "Chroma",
                paramBLabel = "Hue Start",
                tags = listOf("color", "hue", "wash", "music")
            ),
            EffectPreset(
                id = "fx_cinema_vignette_deep",
                name = "Cinema Theater Vignette",
                suite = EffectSuite.NODE_VIDEO,
                category = "Color & Look",
                kernel = EffectKernel.VIGNETTE,
                description = "Deep moody corner darkening calibrated for dramatic storytelling",
                defaultIntensity = 0.78f,
                defaultSpeed = 1.0f,
                defaultParamA = 0.62f,
                defaultParamB = 0.50f,
                paramALabel = "Radius",
                paramBLabel = "Feather",
                tags = listOf("noir", "drama", "vignette", "cinema")
            ),

            // --- GLITCH & RGB ---
            EffectPreset(
                id = "fx_rgb_split",
                name = "RGB Chromatic Split",
                suite = EffectSuite.ALIGHT_MOTION,
                category = "Glitch & RGB",
                kernel = EffectKernel.CHROMATIC_ABERRATION,
                description = "Cyan and crimson channel separation for impact hits and intros",
                defaultIntensity = 0.65f,
                defaultSpeed = 1.1f,
                defaultParamA = 0.52f,
                defaultParamB = 0.30f,
                paramALabel = "Spread",
                paramBLabel = "Angle",
                tags = listOf("rgb", "chromatic", "split", "glitch")
            ),
            EffectPreset(
                id = "fx_prism_refract",
                name = "Crystal Prism Shift",
                suite = EffectSuite.ULTRA_MOTION,
                category = "Glitch & RGB",
                kernel = EffectKernel.PRISM_SHIFT,
                description = "Optical spectral refraction around high-motion frames",
                defaultIntensity = 0.60f,
                defaultSpeed = 1.4f,
                defaultParamA = 0.70f,
                defaultParamB = 0.50f,
                paramALabel = "Dispersion",
                paramBLabel = "Phase",
                tags = listOf("prism", "refraction", "spectral")
            ),
            EffectPreset(
                id = "fx_digital_glitch",
                name = "Digital Tear Glitch",
                suite = EffectSuite.ULTRA_MOTION,
                category = "Glitch & RGB",
                kernel = EffectKernel.GLITCH_BLOCK,
                description = "Horizontal signal slice displacements for high-energy cuts",
                defaultIntensity = 0.66f,
                defaultSpeed = 1.5f,
                defaultParamA = 0.55f,
                defaultParamB = 0.40f,
                paramALabel = "Slice Height",
                paramBLabel = "Jitter",
                tags = listOf("glitch", "tear", "cyber", "transition")
            ),

            // --- CAMERA & SHAKE ---
            EffectPreset(
                id = "fx_handheld_shake",
                name = "Natural Handheld Camera",
                suite = EffectSuite.CAPCUT,
                category = "Camera & Shake",
                kernel = EffectKernel.CAMERA_SHAKE,
                description = "Subtle organic handheld sway that brings static photos and text to life",
                defaultIntensity = 0.36f,
                defaultSpeed = 0.85f,
                defaultParamA = 0.35f,
                defaultParamB = 0.25f,
                paramALabel = "Amplitude",
                paramBLabel = "Seed",
                tags = listOf("handheld", "camera", "organic", "motion")
            ),
            EffectPreset(
                id = "fx_bass_impact_shake",
                name = "Impact Drop Shake",
                suite = EffectSuite.ULTRA_MOTION,
                category = "Camera & Shake",
                kernel = EffectKernel.CAMERA_SHAKE,
                description = "Punchy kinetic turbulence for beat drops and action impacts",
                defaultIntensity = 0.82f,
                defaultSpeed = 1.8f,
                defaultParamA = 0.78f,
                defaultParamB = 0.50f,
                paramALabel = "Punch",
                paramBLabel = "Frequency",
                tags = listOf("impact", "drop", "shake", "action")
            ),

            // --- LENS & WARP ---
            EffectPreset(
                id = "fx_anamorphic_lens",
                name = "Anamorphic Barrel Lens",
                suite = EffectSuite.NODE_VIDEO,
                category = "Lens & Warp",
                kernel = EffectKernel.LENS_DISTORTION,
                description = "Curved optical cinema glass framing around frame perimeter",
                defaultIntensity = 0.58f,
                defaultSpeed = 1.0f,
                defaultParamA = 0.45f,
                defaultParamB = 0.50f,
                paramALabel = "Curvature",
                paramBLabel = "Ring Size",
                tags = listOf("lens", "fisheye", "anamorphic", "cinema")
            ),
            EffectPreset(
                id = "fx_liquid_wave_warp",
                name = "Liquid Wave Warp",
                suite = EffectSuite.ALIGHT_MOTION,
                category = "Lens & Warp",
                kernel = EffectKernel.WAVE_WARP,
                description = "Smooth sinusoidal heat/water ripple modulation",
                defaultIntensity = 0.52f,
                defaultSpeed = 1.1f,
                defaultParamA = 0.48f,
                defaultParamB = 0.30f,
                paramALabel = "Frequency",
                paramBLabel = "Phase",
                tags = listOf("wave", "water", "dream", "warp")
            ),
            EffectPreset(
                id = "fx_kaleido_mirror",
                name = "Kaleidoscope Symmetry",
                suite = EffectSuite.ALIGHT_MOTION,
                category = "Lens & Warp",
                kernel = EffectKernel.KALEIDOSCOPE,
                description = "Rotating radial symmetry planes for music visuals",
                defaultIntensity = 0.58f,
                defaultSpeed = 0.8f,
                defaultParamA = 0.50f,
                defaultParamB = 0.20f,
                paramALabel = "Slices",
                paramBLabel = "Spin",
                tags = listOf("kaleidoscope", "symmetry", "mirror")
            ),
            EffectPreset(
                id = "fx_split_mirror",
                name = "Split Frame Mirror",
                suite = EffectSuite.CAPCUT,
                category = "Lens & Warp",
                kernel = EffectKernel.MIRROR_SPLIT,
                description = "Crisp geometric axis split overlay for montage compositions",
                defaultIntensity = 0.62f,
                defaultSpeed = 0.4f,
                defaultParamA = 0.25f,
                defaultParamB = 0.0f,
                paramALabel = "Splits",
                paramBLabel = "Tilt",
                tags = listOf("mirror", "split", "montage")
            ),

            // --- RETRO & FILM ---
            EffectPreset(
                id = "fx_35mm_film_grain",
                name = "35mm Kodak Film Grain",
                suite = EffectSuite.GOLD_MOTION,
                category = "Retro & Film",
                kernel = EffectKernel.FILM_GRAIN,
                description = "Organic silver-halide emulsion texture to remove digital sterility",
                defaultIntensity = 0.52f,
                defaultSpeed = 1.0f,
                defaultParamA = 0.50f,
                defaultParamB = 0.40f,
                paramALabel = "Density",
                paramBLabel = "Texture",
                tags = listOf("film", "35mm", "grain", "analog")
            ),
            EffectPreset(
                id = "fx_crt_scanlines",
                name = "CRT Monitor Scanlines",
                suite = EffectSuite.CAPCUT,
                category = "Retro & Film",
                kernel = EffectKernel.SCANLINES,
                description = "Authentic broadcast CRT raster lines and gentle roll",
                defaultIntensity = 0.58f,
                defaultSpeed = 1.0f,
                defaultParamA = 0.52f,
                defaultParamB = 0.30f,
                paramALabel = "Line Pitch",
                paramBLabel = "Roll",
                tags = listOf("crt", "vhs", "retro", "scanlines")
            ),

            // --- STYLIZE & MATTE ---
            EffectPreset(
                id = "fx_neon_saber_frame",
                name = "Neon Saber Contour",
                suite = EffectSuite.NODE_VIDEO,
                category = "Stylize & Matte",
                kernel = EffectKernel.NEON_EDGES,
                description = "Glowing neon border outline ideal for highlight callouts",
                defaultIntensity = 0.72f,
                defaultSpeed = 1.0f,
                defaultParamA = 0.55f,
                defaultParamB = 0.50f,
                paramALabel = "Stroke",
                paramBLabel = "Glow",
                tags = listOf("neon", "saber", "border", "outline")
            ),
            EffectPreset(
                id = "fx_halftone_comic",
                name = "Halftone Comic Print",
                suite = EffectSuite.NODE_VIDEO,
                category = "Stylize & Matte",
                kernel = EffectKernel.HALFTONE,
                description = "Clean magazine dot-matrix screen pattern overlay",
                defaultIntensity = 0.60f,
                defaultSpeed = 1.0f,
                defaultParamA = 0.50f,
                defaultParamB = 0.50f,
                paramALabel = "Dot Pitch",
                paramBLabel = "Contrast",
                tags = listOf("halftone", "comic", "print", "dots")
            )
        )
    }

    fun countForSuite(suite: EffectSuite): Int {
        return if (suite == EffectSuite.ALL) allPresets.size
        else allPresets.count { it.suite == suite }
    }

    fun filterPresets(
        suite: EffectSuite = EffectSuite.ALL,
        category: String = "All",
        query: String = "",
        favoritesOnly: Boolean = false,
        favoriteIds: Set<String> = emptySet()
    ): List<EffectPreset> {
        val q = query.trim().lowercase(Locale.US)
        return allPresets.filter { preset ->
            val suiteOk = suite == EffectSuite.ALL || preset.suite == suite
            val catOk = category == "All" || preset.category.equals(category, ignoreCase = true)
            val favOk = !favoritesOnly || favoriteIds.contains(preset.id)
            val queryOk = q.isEmpty() ||
                preset.name.lowercase(Locale.US).contains(q) ||
                preset.description.lowercase(Locale.US).contains(q) ||
                preset.category.lowercase(Locale.US).contains(q) ||
                preset.tags.any { it.contains(q) }
            suiteOk && catOk && favOk && queryOk
        }
    }

    fun findById(presetId: String): EffectPreset? = allPresets.firstOrNull { it.id == presetId }
}

