package com.example.data

import com.example.R
import com.example.model.AuthorInfo
import com.example.model.ColorGradingSettings
import com.example.model.ColorWheelValue
import com.example.model.LutPreset
import com.example.model.MarketplaceCategory
import com.example.model.MarketplaceItem
import com.example.model.ReviewItem

object SampleMarketplaceData {

    fun getInitialMarketplaceItems(): List<MarketplaceItem> {
        return listOf(
            // 1. Templates
            MarketplaceItem(
                id = "tmpl_cyber_glitch",
                title = "Cyberpunk Glitch Neon Intro",
                category = MarketplaceCategory.TEMPLATES,
                description = "High-octane futuristic title reveal with chromatic displacement, keyframed camera shake, and electric cyan-violet grade. Perfect for gaming montages and sci-fi edits.",
                author = AuthorInfo(name = "NeonVFX Studio", handle = "@neon_vfx"),
                downloadsCount = 14850,
                averageRating = 4.9f,
                thumbnailRes = R.drawable.cyberpunk_city,
                accentColorHex = 0xFF00F0FF,
                tags = listOf("Cyberpunk", "Glitch", "Intro", "Neon", "60 FPS"),
                sampleDurationSec = 10,
                isStaffPick = true,
                reviews = listOf(
                    ReviewItem(
                        userName = "Marcus Rivera",
                        userHandle = "@marcus_edits",
                        rating = 5,
                        comment = "The keyframe easing on the zoom punch is unbelievable. Saved me hours of motion graphics work!",
                        date = "Yesterday",
                        helpfulCount = 42
                    ),
                    ReviewItem(
                        userName = "Sarah Jenkins",
                        userHandle = "@sarahtech",
                        rating = 5,
                        comment = "Incredible color grade and text distortion. Instantly leveled up my TikTok hooks.",
                        date = "3 days ago",
                        helpfulCount = 19
                    ),
                    ReviewItem(
                        userName = "Alex Chen",
                        userHandle = "@chen_media",
                        rating = 4,
                        comment = "Super clean template. Easy to swap out text and footage.",
                        date = "1 week ago",
                        helpfulCount = 8
                    )
                ),
                presetColorGrading = ColorGradingSettings(
                    lutPreset = LutPreset.CYBER_NEON,
                    exposure = 0.3f,
                    contrast = 1.3f,
                    saturation = 1.35f,
                    temperature = -15f,
                    tint = 25f
                )
            ),

            MarketplaceItem(
                id = "tmpl_golden_reel",
                title = "Cinematic Golden Hour Reel",
                category = MarketplaceCategory.TEMPLATES,
                description = "Warm organic film look with gentle push-in keyframes, 35mm grain, and soft highlights. Tailored for 9:16 Instagram Reels and YouTube Shorts.",
                author = AuthorInfo(name = "LensMaster Film", handle = "@lensmaster"),
                downloadsCount = 22400,
                averageRating = 4.8f,
                thumbnailRes = R.drawable.sunset_skater,
                accentColorHex = 0xFFFFB020,
                tags = listOf("Reels", "Cinematic", "Golden Hour", "Vintage"),
                sampleDurationSec = 8,
                isStaffPick = true,
                reviews = listOf(
                    ReviewItem(
                        userName = "Elena Rostova",
                        userHandle = "@elena_travels",
                        rating = 5,
                        comment = "The skin tone retention in the highlights is better than most desktop presets!",
                        date = "2 days ago",
                        helpfulCount = 31
                    ),
                    ReviewItem(
                        userName = "David Kim",
                        userHandle = "@dk_films",
                        rating = 5,
                        comment = "Smooth slow-motion ramp and filmic curve. Pure magic.",
                        date = "5 days ago",
                        helpfulCount = 14
                    )
                ),
                presetColorGrading = ColorGradingSettings(
                    lutPreset = LutPreset.GOLDEN_HOUR,
                    exposure = 0.2f,
                    contrast = 1.15f,
                    temperature = 30f,
                    tint = 8f
                )
            ),

            MarketplaceItem(
                id = "tmpl_drone_cinema",
                title = "Pacific 4K Drone Travelogue",
                category = MarketplaceCategory.TEMPLATES,
                description = "Ultra-wide anamorphic travel template featuring slow smooth camera drifts, kinetic lower third titles, and teal/orange Hollywood color separation.",
                author = AuthorInfo(name = "SkyCinema Drone Labs", handle = "@skycinema"),
                downloadsCount = 11200,
                averageRating = 4.7f,
                thumbnailRes = R.drawable.drone_coastline,
                accentColorHex = 0xFF00B4D8,
                tags = listOf("Drone", "Nature", "4K", "Landscape", "16:9"),
                sampleDurationSec = 12,
                reviews = listOf(
                    ReviewItem(
                        userName = "Liam Taylor",
                        userHandle = "@liam_drones",
                        rating = 5,
                        comment = "The color harmony between the cyan ocean and golden rocks is breathtaking.",
                        date = "4 days ago",
                        helpfulCount = 11
                    )
                ),
                presetColorGrading = ColorGradingSettings(
                    lutPreset = LutPreset.TEAL_AND_ORANGE,
                    exposure = 0.15f,
                    contrast = 1.2f,
                    saturation = 1.25f
                )
            ),

            // 2. Transitions
            MarketplaceItem(
                id = "trans_whip_pan",
                title = "Hyper-Speed Whip Pan Left/Right",
                category = MarketplaceCategory.TRANSITIONS,
                description = "Seamless velocity-based camera whip transition with directional motion blur and ease-in-out Bezier curve handles.",
                author = AuthorInfo(name = "MotionCraft VFX", handle = "@motioncraft"),
                downloadsCount = 34100,
                averageRating = 4.9f,
                accentColorHex = 0xFFA855F7,
                tags = listOf("Transition", "Whip Pan", "Speed Ramp", "Seamless"),
                reviews = listOf(
                    ReviewItem(
                        userName = "Chloe Bennett",
                        userHandle = "@chloe_creations",
                        rating = 5,
                        comment = "The motion blur mask perfectly hides the cut between my clips. Best transition on the market!",
                        date = "Yesterday",
                        helpfulCount = 67
                    )
                )
            ),

            MarketplaceItem(
                id = "trans_film_burn",
                title = "Vintage 35mm Film Burn Dissolve",
                category = MarketplaceCategory.TRANSITIONS,
                description = "Organic light leak and thermal film sprocket burn overlay. Adds authentic retro analog warmth between clips.",
                author = AuthorInfo(name = "Analog Vault", handle = "@analog_vault"),
                downloadsCount = 19500,
                averageRating = 4.8f,
                accentColorHex = 0xFFFF5722,
                tags = listOf("Film Burn", "Light Leak", "Retro", "Analog"),
                reviews = listOf(
                    ReviewItem(
                        userName = "Julian Ray",
                        userHandle = "@jray_photo",
                        rating = 5,
                        comment = "Feels like real Kodak 16mm stock burned in a projector. Gorgeous!",
                        date = "3 days ago",
                        helpfulCount = 28
                    )
                )
            ),

            MarketplaceItem(
                id = "trans_glitch_rgb",
                title = "Digital Glitch & RGB Split Flash",
                category = MarketplaceCategory.TRANSITIONS,
                description = "Aggressive electronic data corruption cut with audio sync tick marks and horizontal scanline displacement.",
                author = AuthorInfo(name = "PixelStrike FX", handle = "@pixelstrike"),
                downloadsCount = 28300,
                averageRating = 4.9f,
                accentColorHex = 0xFF00FFC2,
                tags = listOf("Glitch", "RGB Split", "Cyber", "Flash"),
                reviews = listOf(
                    ReviewItem(
                        userName = "Niko Bellic",
                        userHandle = "@niko_vids",
                        rating = 5,
                        comment = "Flawless for beat drops. The RGB channel split gives high energy.",
                        date = "5 days ago",
                        helpfulCount = 15
                    )
                )
            ),

            // 3. Color Grading Presets / LUTs
            MarketplaceItem(
                id = "lut_teal_orange_pro",
                title = "Hollywood Teal & Orange 3D LUT",
                category = MarketplaceCategory.PRESETS,
                description = "The definitive blockbuster color grade. Drives midtone skin tones into warm golden amber while pushing shadows into rich cinematic cyan.",
                author = AuthorInfo(name = "Colorist Leo", handle = "@colorist_leo"),
                downloadsCount = 45200,
                averageRating = 5.0f,
                accentColorHex = 0xFF00B4D8,
                tags = listOf("LUT", "Teal & Orange", "Cinema", "Skin Tones"),
                presetColorGrading = ColorGradingSettings(
                    lutPreset = LutPreset.TEAL_AND_ORANGE,
                    exposure = 0.2f,
                    contrast = 1.25f,
                    temperature = 10f,
                    tint = -5f,
                    saturation = 1.3f,
                    lift = ColorWheelValue(hue = 195f, saturation = 0.45f, luminance = -0.05f),
                    gain = ColorWheelValue(hue = 38f, saturation = 0.50f, luminance = 0.12f)
                ),
                reviews = listOf(
                    ReviewItem(
                        userName = "Christian Bale",
                        userHandle = "@cb_director",
                        rating = 5,
                        comment = "Industry standard quality right on mobile. The 3-way wheel mapping is exceptional.",
                        date = "Yesterday",
                        helpfulCount = 89
                    )
                )
            ),

            MarketplaceItem(
                id = "lut_kodachrome_64",
                title = "Kodachrome 64 Vintage Emulation",
                category = MarketplaceCategory.PRESETS,
                description = "Faithful recreation of National Geographic's legendary 1970s color reversal film with rich cherry reds, amber highlights, and crushed blacks.",
                author = AuthorInfo(name = "Retro Heritage", handle = "@retro_heritage"),
                downloadsCount = 31000,
                averageRating = 4.9f,
                accentColorHex = 0xFFFF6F00,
                tags = listOf("LUT", "Kodachrome", "70s", "Warm", "Vintage"),
                presetColorGrading = ColorGradingSettings(
                    lutPreset = LutPreset.KODACHROME,
                    exposure = 0.1f,
                    contrast = 1.25f,
                    temperature = 25f,
                    saturation = 1.2f
                ),
                reviews = listOf(
                    ReviewItem(
                        userName = "Maya Lin",
                        userHandle = "@mayalin_lens",
                        rating = 5,
                        comment = "The yellow-red warmth gives pure nostalgic summer vibes.",
                        date = "1 week ago",
                        helpfulCount = 33
                    )
                )
            ),

            MarketplaceItem(
                id = "lut_fuji_eterna",
                title = "Fuji Eterna Soft Pastel Grade",
                category = MarketplaceCategory.PRESETS,
                description = "Gentle highlight roll-off, desaturated greens, and lifted misty shadows. Ideal for music videos and indie drama aesthetic.",
                author = AuthorInfo(name = "IndieFilm Color", handle = "@indiefilm_co"),
                downloadsCount = 20800,
                averageRating = 4.8f,
                accentColorHex = 0xFF4DB6AC,
                tags = listOf("LUT", "Eterna", "Pastel", "Indie", "Soft"),
                presetColorGrading = ColorGradingSettings(
                    lutPreset = LutPreset.FUJI_ETERNA,
                    exposure = 0.05f,
                    contrast = 0.95f,
                    saturation = 0.9f
                ),
                reviews = listOf(
                    ReviewItem(
                        userName = "Zack Snyder",
                        userHandle = "@zs_vision",
                        rating = 5,
                        comment = "Soft and moody. Doesn't crush the shadows.",
                        date = "4 days ago",
                        helpfulCount = 18
                    )
                )
            )
        )
    }
}
