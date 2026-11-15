package com.example.data

import com.example.model.EffectKernel
import com.example.model.EffectPreset
import com.example.model.EffectSuite
import java.util.Locale

/**
 * Curated & combinatorially indexed 2,200+ effects library spanning
 * Alight Motion, Gold Motion, Ultra Motion, Node Video, and CapCut.
 */
object EffectCatalog {

    val categories: List<String> = listOf(
        "All",
        "Distortion",
        "Blur",
        "Color & LUT",
        "Light & Glow",
        "Glitch",
        "3D & Camera",
        "Retro",
        "Particles",
        "Stylize",
        "Transitions",
        "Matte",
        "Audio Reactive"
    )

    private data class KernelSeed(
        val category: String,
        val kernel: EffectKernel,
        val baseNames: List<String>,
        val paramALabel: String,
        val paramBLabel: String
    )

    private val categorySeeds: List<KernelSeed> = listOf(
        KernelSeed(
            category = "Distortion",
            kernel = EffectKernel.WAVE_WARP,
            baseNames = listOf(
                "Tiles", "Oscillate", "Wave Warp", "Turbulent Displace", "Fractal Warp",
                "Swirl", "Pinch & Bulge", "Circular Ripple", "Polar Coordinates", "Squeeze",
                "Liquid Flow", "Stretch Axis", "Flag Flutter", "Elastic Warp"
            ),
            paramALabel = "Frequency",
            paramBLabel = "Phase"
        ),
        KernelSeed(
            category = "Distortion",
            kernel = EffectKernel.KALEIDOSCOPE,
            baseNames = listOf(
                "Kaleidoscope", "Mirror Tile", "Hexagon Fold", "Prism Mirror", "Radial Fold"
            ),
            paramALabel = "Segments",
            paramBLabel = "Angle"
        ),
        KernelSeed(
            category = "Blur",
            kernel = EffectKernel.MOTION_BLUR,
            baseNames = listOf(
                "Directional Blur", "Gaussian Blur", "Zoom Blur", "Spin Blur", "Whip Blur",
                "Lens Blur", "Box Blur", "Tilt-Shift Focus", "Chroma Blur", "Velocity Trail",
                "Ghost Motion", "Speed Streaks"
            ),
            paramALabel = "Angle",
            paramBLabel = "Samples"
        ),
        KernelSeed(
            category = "Color & LUT",
            kernel = EffectKernel.COLOR_PULSE,
            baseNames = listOf(
                "Exposure / Gamma", "Hue Shift", "Channel Remap", "Hot Color Boost", "Gradient Map",
                "Duotone Matte", "Colorama", "Vibrance Lift", "Solarize Tint", "Color Pulse",
                "Split Tone Gold", "Teal & Amber"
            ),
            paramALabel = "Hue Shift",
            paramBLabel = "Contrast"
        ),
        KernelSeed(
            category = "Light & Glow",
            kernel = EffectKernel.GLOW,
            baseNames = listOf(
                "Diffusion Glow", "Bloom Mist", "Optical Flare", "Saber Rim", "Halo Aura",
                "Light Rays", "God Rays", "Exposure Flash", "Soft Light Leak", "Edge Bloom"
            ),
            paramALabel = "Radius",
            paramBLabel = "Threshold"
        ),
        KernelSeed(
            category = "Light & Glow",
            kernel = EffectKernel.LIGHT_SWEEP,
            baseNames = listOf(
                "Light Sweep", "Trophy Shine", "Metallic Gleam", "Anamorphic Streak", "Laser Scan",
                "Starburst Glint"
            ),
            paramALabel = "Width",
            paramBLabel = "Angle"
        ),
        KernelSeed(
            category = "Glitch",
            kernel = EffectKernel.CHROMATIC_ABERRATION,
            baseNames = listOf(
                "RGB Split", "Spectral Shift", "Chromatic Prism", "Analog Fringe", "RGB Shake",
                "3D Anaglyph Split", "Channel Offset", "Holo Split"
            ),
            paramALabel = "Spread",
            paramBLabel = "Angle"
        ),
        KernelSeed(
            category = "Glitch",
            kernel = EffectKernel.GLITCH_BLOCK,
            baseNames = listOf(
                "Digital Glitch", "Pixel Tear", "Signal Corruption", "Block Slice", "Cyber Static",
                "Data Mosh", "Bad Reception", "Frame Stutter"
            ),
            paramALabel = "Block Size",
            paramBLabel = "Jitter"
        ),
        KernelSeed(
            category = "3D & Camera",
            kernel = EffectKernel.LENS_DISTORTION,
            baseNames = listOf(
                "Anamorphic Lens", "Fisheye Dome", "Sphere Wrap", "Cylinder Bend", "Perspective Tilt",
                "3D Card Flip", "Page Turn", "Tunnel Warp", "Convex Glass"
            ),
            paramALabel = "Curvature",
            paramBLabel = "FOV"
        ),
        KernelSeed(
            category = "3D & Camera",
            kernel = EffectKernel.CAMERA_SHAKE,
            baseNames = listOf(
                "Action Cam Shake", "Handheld Camera", "Sub-Bass Earthquake", "Swing Pendulum",
                "Randomize Dolly", "Micro Jitter", "Impact Slam", "Orbit Wobble"
            ),
            paramALabel = "Amplitude",
            paramBLabel = "Damping"
        ),
        KernelSeed(
            category = "Retro",
            kernel = EffectKernel.SCANLINES,
            baseNames = listOf(
                "CRT Scanlines", "VHS Tracking", "Betamax Tape", "Arcade Phosphor", "MiniDV Timecode",
                "CCTV Monitor"
            ),
            paramALabel = "Line Pitch",
            paramBLabel = "Tracking"
        ),
        KernelSeed(
            category = "Retro",
            kernel = EffectKernel.FILM_GRAIN,
            baseNames = listOf(
                "35mm Film Grain", "Super 8 Dust", "Kodak Warm Noise", "16mm Cinema", "Sepia Archival",
                "Gate Weave"
            ),
            paramALabel = "Grain Size",
            paramBLabel = "Softness"
        ),
        KernelSeed(
            category = "Particles",
            kernel = EffectKernel.PARTICLE_SPARK,
            baseNames = listOf(
                "Gold Dust", "Ember Sparks", "Snowdrift Flakes", "Rainfall Streaks", "Bokeh Orbs",
                "Sakura Drift", "Cyber Matrix", "Stardust Float"
            ),
            paramALabel = "Density",
            paramBLabel = "Drift"
        ),
        KernelSeed(
            category = "Stylize",
            kernel = EffectKernel.PIXELATE,
            baseNames = listOf(
                "Mosaic Pixelate", "Posterize 8-Bit", "Block Censor", "Crystallize", "Stained Glass"
            ),
            paramALabel = "Cell Size",
            paramBLabel = "Sharpness"
        ),
        KernelSeed(
            category = "Stylize",
            kernel = EffectKernel.NEON_EDGES,
            baseNames = listOf(
                "Find Edges Neon", "Saber Contour", "Sketch Ink", "Emboss Relief", "Halftone Dot Matrix"
            ),
            paramALabel = "Edge Boost",
            paramBLabel = "Threshold"
        ),
        KernelSeed(
            category = "Transitions",
            kernel = EffectKernel.PRISM_SHIFT,
            baseNames = listOf(
                "Whip Pan Flash", "Prism Refraction", "Slide Tile In", "Zoom Dissolve",
                "Burn Exposure", "Spin Swap", "Glitch Cut", "Diamond Iris"
            ),
            paramALabel = "Curve",
            paramBLabel = "Blur"
        ),
        KernelSeed(
            category = "Matte",
            kernel = EffectKernel.VIGNETTE,
            baseNames = listOf(
                "Vignette Spotlight", "Chroma Key Assist", "Luma Matte", "Wipe Strip",
                "Radial Feather", "Cinema Bars Matte"
            ),
            paramALabel = "Feather",
            paramBLabel = "Roundness"
        ),
        KernelSeed(
            category = "Audio Reactive",
            kernel = EffectKernel.STARBURST,
            baseNames = listOf(
                "Bass Beat Scale", "Spectrum Aura", "Drop Flash", "Kick Camera Punch",
                "Hi-Hat Spark", "Tempo Glow Pulse", "808 Wave"
            ),
            paramALabel = "Sensitivity",
            paramBLabel = "Attack"
        )
    )

    private val suiteSignatures: Map<EffectSuite, List<Pair<String, EffectKernel>>> = mapOf(
        EffectSuite.ALIGHT_MOTION to listOf(
            "Alight Tiles Mirror" to EffectKernel.WAVE_WARP,
            "Alight Oscillate Swing" to EffectKernel.CAMERA_SHAKE,
            "Alight Turbulent Displace" to EffectKernel.WAVE_WARP,
            "Alight Strip Repeat" to EffectKernel.MIRROR_SPLIT,
            "Alight Smooth Bevel Glow" to EffectKernel.GLOW,
            "Alight Squeeze & Stretch" to EffectKernel.LENS_DISTORTION,
            "Alight Fractal Ridges" to EffectKernel.NEON_EDGES,
            "Alight Directional Whip" to EffectKernel.MOTION_BLUR
        ),
        EffectSuite.GOLD_MOTION to listOf(
            "Gold 24K Luxury Bloom" to EffectKernel.GLOW,
            "Gold Foil Shimmer" to EffectKernel.PARTICLE_SPARK,
            "Gold Champagne Sweep" to EffectKernel.LIGHT_SWEEP,
            "Gold Chrome Specular" to EffectKernel.PRISM_SHIFT,
            "Gold Velvet Vignette" to EffectKernel.VIGNETTE,
            "Gold Gilded Edge" to EffectKernel.NEON_EDGES,
            "Gold Warm 35mm" to EffectKernel.FILM_GRAIN,
            "Gold Drop Impact" to EffectKernel.CAMERA_SHAKE
        ),
        EffectSuite.ULTRA_MOTION to listOf(
            "Ultra Velocity Shake" to EffectKernel.CAMERA_SHAKE,
            "Ultra Hyper-Prism" to EffectKernel.PRISM_SHIFT,
            "Ultra Kinetic Echo" to EffectKernel.MOTION_BLUR,
            "Ultra RGB Overload" to EffectKernel.CHROMATIC_ABERRATION,
            "Ultra Neon Ray" to EffectKernel.STARBURST,
            "Ultra Cyber Slice" to EffectKernel.GLITCH_BLOCK,
            "Ultra Tempo Flash" to EffectKernel.COLOR_PULSE,
            "Ultra Warp Horizon" to EffectKernel.LENS_DISTORTION
        ),
        EffectSuite.NODE_VIDEO to listOf(
            "Node Glow Saber" to EffectKernel.NEON_EDGES,
            "Node Raindrops Glass" to EffectKernel.PARTICLE_SPARK,
            "Node 3D Camera Orbit" to EffectKernel.LENS_DISTORTION,
            "Node Ray-Marched Shine" to EffectKernel.LIGHT_SWEEP,
            "Node Radial God Rays" to EffectKernel.STARBURST,
            "Node RGB Chromatic" to EffectKernel.CHROMATIC_ABERRATION,
            "Node Halftone Print" to EffectKernel.HALFTONE,
            "Node Audio Spectrum" to EffectKernel.COLOR_PULSE
        ),
        EffectSuite.CAPCUT to listOf(
            "CapCut Diamond Zoom" to EffectKernel.LENS_DISTORTION,
            "CapCut Retro DV Cam" to EffectKernel.SCANLINES,
            "CapCut Rebound Swing" to EffectKernel.CAMERA_SHAKE,
            "CapCut Halo Dream Blur" to EffectKernel.GLOW,
            "CapCut Playful Glitch" to EffectKernel.GLITCH_BLOCK,
            "CapCut Astral Prism" to EffectKernel.PRISM_SHIFT,
            "CapCut Club Beat Flash" to EffectKernel.STARBURST,
            "CapCut Soft Pixel" to EffectKernel.PIXELATE
        )
    )

    private val styleVariants: List<Pair<String, Float>> = listOf(
        "Classic" to 0.55f,
        "Pro" to 0.72f,
        "Soft" to 0.35f,
        "Extreme" to 0.90f,
        "Smooth" to 0.45f,
        "Turbo" to 0.85f
    )

    val allPresets: List<EffectPreset> by lazy { buildCatalog() }

    private fun buildCatalog(): List<EffectPreset> {
        val list = ArrayList<EffectPreset>(2400)
        val suites = listOf(
            EffectSuite.ALIGHT_MOTION,
            EffectSuite.GOLD_MOTION,
            EffectSuite.ULTRA_MOTION,
            EffectSuite.NODE_VIDEO,
            EffectSuite.CAPCUT
        )

        var seq = 1
        // 1. Add flagship signature presets per suite (8 each = 40)
        suites.forEach { suite ->
            suiteSignatures[suite].orEmpty().forEach { (sigName, kernel) ->
                val cat = categorySeeds.firstOrNull { it.kernel == kernel }?.category ?: "Stylize"
                list.add(
                    EffectPreset(
                        id = "fx_sig_${suite.name.lowercase(Locale.US)}_$seq",
                        name = sigName,
                        suite = suite,
                        category = cat,
                        kernel = kernel,
                        description = "${suite.displayName} signature preset ($cat)",
                        defaultIntensity = 0.65f,
                        defaultSpeed = 1.0f,
                        defaultParamA = 0.55f,
                        defaultParamB = 0.45f,
                        paramALabel = "Strength",
                        paramBLabel = "Phase",
                        tags = listOf(suite.displayName, suite.shortName, cat, "signature")
                    )
                )
                seq++
            }
        }

        // 2. Expand curated category kernels × suites × variants (~2,200+ effects)
        for (suite in suites) {
            for (seed in categorySeeds) {
                for (baseName in seed.baseNames) {
                    for ((variantLabel, baseIntensity) in styleVariants) {
                        val displayName = if (variantLabel == "Classic") {
                            "${suite.shortName}: $baseName"
                        } else {
                            "${suite.shortName}: $baseName ($variantLabel)"
                        }
                        val speedFactor = when (variantLabel) {
                            "Turbo", "Extreme" -> 1.6f
                            "Smooth", "Soft" -> 0.7f
                            else -> 1.0f
                        }
                        val paramA = ((seq % 9) + 1) / 10f
                        val paramB = (((seq * 3) % 9) + 1) / 10f
                        list.add(
                            EffectPreset(
                                id = "fx_${suite.shortName.lowercase(Locale.US)}_$seq",
                                name = displayName,
                                suite = suite,
                                category = seed.category,
                                kernel = seed.kernel,
                                description = "${suite.displayName} ${seed.category} engine — $baseName ($variantLabel)",
                                defaultIntensity = baseIntensity,
                                defaultSpeed = speedFactor,
                                defaultParamA = paramA,
                                defaultParamB = paramB,
                                paramALabel = seed.paramALabel,
                                paramBLabel = seed.paramBLabel,
                                tags = listOf(
                                    suite.displayName.lowercase(Locale.US),
                                    seed.category.lowercase(Locale.US),
                                    baseName.lowercase(Locale.US),
                                    variantLabel.lowercase(Locale.US)
                                )
                            )
                        )
                        seq++
                    }
                }
            }
        }

        return list
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
                preset.category.lowercase(Locale.US).contains(q) ||
                preset.suite.displayName.lowercase(Locale.US).contains(q)
            suiteOk && catOk && favOk && queryOk
        }
    }

    fun findById(presetId: String): EffectPreset? = allPresets.firstOrNull { it.id == presetId }
}
