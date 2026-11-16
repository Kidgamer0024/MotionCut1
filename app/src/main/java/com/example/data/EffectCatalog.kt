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
        "Background",
        "Blur",
        "Lighting",
        "Filter",
        "Other"
    )

    val allPresets: List<EffectPreset> by lazy {
        listOf(
            // --- BACKGROUND ---
            EffectPreset(
                id = "fx_ambient_backdrop",
                name = "Ambient Radial Backdrop",
                suite = EffectSuite.CAPCUT,
                category = "Background",
                kernel = EffectKernel.AMBIENT_BACKDROP,
                description = "Soft atmospheric colored radial stage behind transparent cutouts and titles",
                defaultIntensity = 0.65f,
                defaultSpeed = 0.8f,
                defaultParamA = 0.55f,
                defaultParamB = 0.40f,
                defaultColorHex = 0xFF00E5FF,
                paramALabel = "Spread",
                paramBLabel = "Drift",
                tags = listOf("background", "backdrop", "radial", "stage")
            ),
            EffectPreset(
                id = "fx_bokeh_backdrop",
                name = "Soft Bokeh Stage",
                suite = EffectSuite.GOLD_MOTION,
                category = "Background",
                kernel = EffectKernel.BOKEH_BACKDROP,
                description = "Layered out-of-focus bokeh circles for depth behind overlays",
                defaultIntensity = 0.60f,
                defaultSpeed = 0.7f,
                defaultParamA = 0.60f,
                defaultParamB = 0.35f,
                defaultColorHex = 0xFFFFB020,
                paramALabel = "Orb Size",
                paramBLabel = "Motion",
                tags = listOf("background", "bokeh", "depth", "stage")
            ),
            EffectPreset(
                id = "fx_grid_matrix_bg",
                name = "Perspective Studio Grid",
                suite = EffectSuite.NODE_VIDEO,
                category = "Background",
                kernel = EffectKernel.GRID_MATRIX,
                description = "Clean geometric coordinate grid pattern for graphics and product shots",
                defaultIntensity = 0.58f,
                defaultSpeed = 0.9f,
                defaultParamA = 0.50f,
                defaultParamB = 0.25f,
                defaultColorHex = 0xFF10E878,
                paramALabel = "Cell Size",
                paramBLabel = "Scroll",
                tags = listOf("background", "grid", "pattern", "studio")
            ),
            EffectPreset(
                id = "fx_harmonic_wash_bg",
                name = "Spectrum Tint Backdrop",
                suite = EffectSuite.ULTRA_MOTION,
                category = "Background",
                kernel = EffectKernel.COLOR_PULSE,
                description = "Smooth shifting studio color wash synced to playback",
                defaultIntensity = 0.48f,
                defaultSpeed = 0.9f,
                defaultParamA = 0.50f,
                defaultParamB = 0.25f,
                defaultColorHex = 0xFFD580FF,
                paramALabel = "Chroma",
                paramBLabel = "Hue Start",
                tags = listOf("background", "color", "wash", "gradient")
            ),

            // --- BLUR ---
            EffectPreset(
                id = "fx_gaussian_soft_blur",
                name = "Gaussian Soft Haze",
                suite = EffectSuite.CAPCUT,
                category = "Blur",
                kernel = EffectKernel.GAUSSIAN_BLUR,
                description = "Smooth omnidirectional softening veil calibrated for backgrounds and dreams",
                defaultIntensity = 0.62f,
                defaultSpeed = 0.6f,
                defaultParamA = 0.58f,
                defaultParamB = 0.40f,
                defaultColorHex = 0xFFE2E8F0,
                paramALabel = "Radius",
                paramBLabel = "Softness",
                tags = listOf("blur", "gaussian", "soft", "haze")
            ),
            EffectPreset(
                id = "fx_radial_zoom_blur",
                name = "Radial Speed Zoom",
                suite = EffectSuite.ALIGHT_MOTION,
                category = "Blur",
                kernel = EffectKernel.RADIAL_BLUR,
                description = "Inward/outward radial motion lines focused on frame center",
                defaultIntensity = 0.66f,
                defaultSpeed = 1.2f,
                defaultParamA = 0.60f,
                defaultParamB = 0.45f,
                defaultColorHex = 0xFF00E5FF,
                paramALabel = "Strength",
                paramBLabel = "Spin",
                tags = listOf("blur", "radial", "zoom", "action")
            ),
            EffectPreset(
                id = "fx_directional_blur",
                name = "Directional Whip Blur",
                suite = EffectSuite.ALIGHT_MOTION,
                category = "Blur",
                kernel = EffectKernel.MOTION_BLUR,
                description = "Directional speed streaks ideal for fast camera pans and beat transitions",
                defaultIntensity = 0.65f,
                defaultSpeed = 1.3f,
                defaultParamA = 0.60f,
                defaultParamB = 0.50f,
                defaultColorHex = 0xFFFFFFFF,
                paramALabel = "Streak",
                paramBLabel = "Phase",
                tags = listOf("blur", "whip", "motion", "speed")
            ),
            EffectPreset(
                id = "fx_privacy_mosaic",
                name = "Censor / Mosaic Blur",
                suite = EffectSuite.CAPCUT,
                category = "Blur",
                kernel = EffectKernel.PIXELATE,
                description = "Clean square mosaic grid useful for faces, plates, or stylized tiles",
                defaultIntensity = 0.68f,
                defaultSpeed = 1.0f,
                defaultParamA = 0.55f,
                defaultParamB = 0.50f,
                defaultColorHex = 0xFFFFFFFF,
                paramALabel = "Block Size",
                paramBLabel = "Grid Alpha",
                tags = listOf("blur", "pixelate", "mosaic", "censor")
            ),

            // --- LIGHTING ---
            EffectPreset(
                id = "fx_optical_glow",
                name = "Optical Diffusion Glow",
                suite = EffectSuite.NODE_VIDEO,
                category = "Lighting",
                kernel = EffectKernel.GLOW,
                description = "Soft cinematic highlight bloom and atmospheric halation",
                defaultIntensity = 0.55f,
                defaultSpeed = 0.9f,
                defaultParamA = 0.65f,
                defaultParamB = 0.35f,
                defaultColorHex = 0xFF00E5FF,
                paramALabel = "Radius",
                paramBLabel = "Pulse",
                tags = listOf("lighting", "glow", "bloom", "halation")
            ),
            EffectPreset(
                id = "fx_light_sweep",
                name = "Anamorphic Light Sweep",
                suite = EffectSuite.GOLD_MOTION,
                category = "Lighting",
                kernel = EffectKernel.LIGHT_SWEEP,
                description = "Diagonal specular light beam sweeping smoothly across titles and clips",
                defaultIntensity = 0.72f,
                defaultSpeed = 1.0f,
                defaultParamA = 0.45f,
                defaultParamB = 0.25f,
                defaultColorHex = 0xFFFFB020,
                paramALabel = "Beam Width",
                paramBLabel = "Offset",
                tags = listOf("lighting", "sweep", "shine", "flare")
            ),
            EffectPreset(
                id = "fx_gold_rim_bloom",
                name = "Warm Sunburst Rays",
                suite = EffectSuite.GOLD_MOTION,
                category = "Lighting",
                kernel = EffectKernel.STARBURST,
                description = "Warm radial light rays and specular highlight catch",
                defaultIntensity = 0.58f,
                defaultSpeed = 0.8f,
                defaultParamA = 0.55f,
                defaultParamB = 0.40f,
                defaultColorHex = 0xFFFFB020,
                paramALabel = "Ray Span",
                paramBLabel = "Rotation",
                tags = listOf("lighting", "rays", "sun", "starburst")
            ),
            EffectPreset(
                id = "fx_atmospheric_dust",
                name = "Floating Light Motes",
                suite = EffectSuite.CAPCUT,
                category = "Lighting",
                kernel = EffectKernel.PARTICLE_SPARK,
                description = "Subtle drifting atmospheric embers and warm dust particles",
                defaultIntensity = 0.60f,
                defaultSpeed = 0.75f,
                defaultParamA = 0.50f,
                defaultParamB = 0.40f,
                defaultColorHex = 0xFFFFB020,
                paramALabel = "Density",
                paramBLabel = "Drift",
                tags = listOf("lighting", "particles", "dust", "sparks")
            ),
            EffectPreset(
                id = "fx_cinema_vignette_deep",
                name = "Theater Spotlight Vignette",
                suite = EffectSuite.NODE_VIDEO,
                category = "Lighting",
                kernel = EffectKernel.VIGNETTE,
                description = "Deep corner falloff calibrated for dramatic center lighting",
                defaultIntensity = 0.74f,
                defaultSpeed = 1.0f,
                defaultParamA = 0.58f,
                defaultParamB = 0.50f,
                defaultColorHex = 0xFF0A0A0F,
                paramALabel = "Radius",
                paramBLabel = "Feather",
                tags = listOf("lighting", "vignette", "spotlight", "cinema")
            ),

            // --- FILTER ---
            EffectPreset(
                id = "fx_rgb_split",
                name = "RGB Chromatic Split",
                suite = EffectSuite.ALIGHT_MOTION,
                category = "Filter",
                kernel = EffectKernel.CHROMATIC_ABERRATION,
                description = "Color channel separation filter for punchy impact frames",
                defaultIntensity = 0.65f,
                defaultSpeed = 1.1f,
                defaultParamA = 0.52f,
                defaultParamB = 0.30f,
                defaultColorHex = 0xFF00E5FF,
                paramALabel = "Spread",
                paramBLabel = "Angle",
                tags = listOf("filter", "rgb", "chromatic", "split")
            ),
            EffectPreset(
                id = "fx_prism_refract",
                name = "Crystal Prism Filter",
                suite = EffectSuite.ULTRA_MOTION,
                category = "Filter",
                kernel = EffectKernel.PRISM_SHIFT,
                description = "Optical spectral refraction around high-contrast edges",
                defaultIntensity = 0.60f,
                defaultSpeed = 1.4f,
                defaultParamA = 0.70f,
                defaultParamB = 0.50f,
                defaultColorHex = 0xFFD580FF,
                paramALabel = "Dispersion",
                paramBLabel = "Phase",
                tags = listOf("filter", "prism", "refraction", "spectral")
            ),
            EffectPreset(
                id = "fx_35mm_film_grain",
                name = "35mm Kodak Film Grain",
                suite = EffectSuite.GOLD_MOTION,
                category = "Filter",
                kernel = EffectKernel.FILM_GRAIN,
                description = "Organic silver-halide emulsion texture to soften sterile digital footage",
                defaultIntensity = 0.52f,
                defaultSpeed = 1.0f,
                defaultParamA = 0.50f,
                defaultParamB = 0.40f,
                defaultColorHex = 0xFFF8FAFC,
                paramALabel = "Density",
                paramBLabel = "Texture",
                tags = listOf("filter", "film", "35mm", "grain")
            ),
            EffectPreset(
                id = "fx_halftone_comic",
                name = "Halftone Comic Print",
                suite = EffectSuite.NODE_VIDEO,
                category = "Filter",
                kernel = EffectKernel.HALFTONE,
                description = "Clean magazine dot-matrix screen print filter",
                defaultIntensity = 0.60f,
                defaultSpeed = 1.0f,
                defaultParamA = 0.50f,
                defaultParamB = 0.50f,
                defaultColorHex = 0xFF10E878,
                paramALabel = "Dot Pitch",
                paramBLabel = "Contrast",
                tags = listOf("filter", "halftone", "comic", "dots")
            ),
            EffectPreset(
                id = "fx_crt_scanlines",
                name = "CRT Broadcast Scanlines",
                suite = EffectSuite.CAPCUT,
                category = "Filter",
                kernel = EffectKernel.SCANLINES,
                description = "Retro broadcast CRT raster lines and gentle roll",
                defaultIntensity = 0.58f,
                defaultSpeed = 1.0f,
                defaultParamA = 0.52f,
                defaultParamB = 0.30f,
                defaultColorHex = 0xFF0F172A,
                paramALabel = "Line Pitch",
                paramBLabel = "Roll",
                tags = listOf("filter", "crt", "vhs", "scanlines")
            ),

            // --- OTHER ---
            EffectPreset(
                id = "fx_digital_glitch",
                name = "Digital Signal Tear",
                suite = EffectSuite.ULTRA_MOTION,
                category = "Other",
                kernel = EffectKernel.GLITCH_BLOCK,
                description = "Horizontal signal slice displacements for energetic transitions",
                defaultIntensity = 0.66f,
                defaultSpeed = 1.5f,
                defaultParamA = 0.55f,
                defaultParamB = 0.40f,
                defaultColorHex = 0xFFFF4D6D,
                paramALabel = "Slice Height",
                paramBLabel = "Jitter",
                tags = listOf("other", "glitch", "tear", "transition")
            ),
            EffectPreset(
                id = "fx_handheld_shake",
                name = "Natural Handheld Camera",
                suite = EffectSuite.CAPCUT,
                category = "Other",
                kernel = EffectKernel.CAMERA_SHAKE,
                description = "Subtle organic handheld camera sway that animates still photos",
                defaultIntensity = 0.36f,
                defaultSpeed = 0.85f,
                defaultParamA = 0.35f,
                defaultParamB = 0.25f,
                defaultColorHex = 0xFF00E5FF,
                paramALabel = "Amplitude",
                paramBLabel = "Seed",
                tags = listOf("other", "handheld", "camera", "motion")
            ),
            EffectPreset(
                id = "fx_bass_impact_shake",
                name = "Impact Drop Shake",
                suite = EffectSuite.ULTRA_MOTION,
                category = "Other",
                kernel = EffectKernel.CAMERA_SHAKE,
                description = "Punchy kinetic camera turbulence for beat drops and impacts",
                defaultIntensity = 0.82f,
                defaultSpeed = 1.8f,
                defaultParamA = 0.78f,
                defaultParamB = 0.50f,
                defaultColorHex = 0xFFD580FF,
                paramALabel = "Punch",
                paramBLabel = "Frequency",
                tags = listOf("other", "impact", "drop", "shake")
            ),
            EffectPreset(
                id = "fx_anamorphic_lens",
                name = "Anamorphic Barrel Lens",
                suite = EffectSuite.NODE_VIDEO,
                category = "Other",
                kernel = EffectKernel.LENS_DISTORTION,
                description = "Curved optical cinema glass framing around viewport perimeter",
                defaultIntensity = 0.58f,
                defaultSpeed = 1.0f,
                defaultParamA = 0.45f,
                defaultParamB = 0.50f,
                defaultColorHex = 0xFF10E878,
                paramALabel = "Curvature",
                paramBLabel = "Ring Size",
                tags = listOf("other", "lens", "fisheye", "anamorphic")
            ),
            EffectPreset(
                id = "fx_liquid_wave_warp",
                name = "Liquid Wave Warp",
                suite = EffectSuite.ALIGHT_MOTION,
                category = "Other",
                kernel = EffectKernel.WAVE_WARP,
                description = "Smooth sinusoidal heat and water ripple modulation",
                defaultIntensity = 0.52f,
                defaultSpeed = 1.1f,
                defaultParamA = 0.48f,
                defaultParamB = 0.30f,
                defaultColorHex = 0xFF00E5FF,
                paramALabel = "Frequency",
                paramBLabel = "Phase",
                tags = listOf("other", "wave", "water", "warp")
            ),
            EffectPreset(
                id = "fx_kaleido_mirror",
                name = "Kaleidoscope Symmetry",
                suite = EffectSuite.ALIGHT_MOTION,
                category = "Other",
                kernel = EffectKernel.KALEIDOSCOPE,
                description = "Rotating radial symmetry planes for stylized visuals",
                defaultIntensity = 0.58f,
                defaultSpeed = 0.8f,
                defaultParamA = 0.50f,
                defaultParamB = 0.20f,
                defaultColorHex = 0xFF00E5FF,
                paramALabel = "Slices",
                paramBLabel = "Spin",
                tags = listOf("other", "kaleidoscope", "symmetry", "mirror")
            ),
            EffectPreset(
                id = "fx_split_mirror",
                name = "Split Frame Mirror",
                suite = EffectSuite.CAPCUT,
                category = "Other",
                kernel = EffectKernel.MIRROR_SPLIT,
                description = "Geometric axis split guides for montage compositions",
                defaultIntensity = 0.62f,
                defaultSpeed = 0.4f,
                defaultParamA = 0.25f,
                defaultParamB = 0.0f,
                defaultColorHex = 0xFFFF4D6D,
                paramALabel = "Splits",
                paramBLabel = "Tilt",
                tags = listOf("other", "mirror", "split", "montage")
            ),
            EffectPreset(
                id = "fx_glow_contour",
                name = "Glowing Edge Contour",
                suite = EffectSuite.NODE_VIDEO,
                category = "Other",
                kernel = EffectKernel.NEON_EDGES,
                description = "Luminous perimeter contour frame ideal for highlight callouts",
                defaultIntensity = 0.72f,
                defaultSpeed = 1.0f,
                defaultParamA = 0.55f,
                defaultParamB = 0.50f,
                defaultColorHex = 0xFF10E878,
                paramALabel = "Stroke",
                paramBLabel = "Glow",
                tags = listOf("other", "border", "outline", "contour")
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

