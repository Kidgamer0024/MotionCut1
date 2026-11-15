package com.example.data

import com.example.R
import com.example.model.AnimatableProperty
import com.example.model.AspectRatio
import com.example.model.ClipItem
import com.example.model.ClipType
import com.example.model.ColorGradingSettings
import com.example.model.ColorWheelValue
import com.example.model.EasingType
import com.example.model.EffectType
import com.example.model.ExportResolution
import com.example.model.FrameRate
import com.example.model.Keyframe
import com.example.model.KeyframeTrack
import com.example.model.LutPreset
import com.example.model.Project
import com.example.model.ShapeConfig
import com.example.model.ShapeType
import com.example.model.TextConfig
import com.example.model.Track
import com.example.model.TrackType
import com.example.model.TransformState
import com.example.model.VideoEffect
import java.util.UUID

object SampleProjects {

    fun createCyberpunkProject(): Project {
        val projectId = "project_cyberpunk_neon"
        val mainVideoTrackId = "track_main_cyberpunk"
        val textTrackId = "track_text_cyberpunk"
        val shapeTrackId = "track_shape_cyberpunk"
        val audioTrackId = "track_audio_cyberpunk"

        val mainClipId = "clip_cyberpunk_city"
        val cityClip = ClipItem(
            id = mainClipId,
            trackId = mainVideoTrackId,
            title = "Cyberpunk City Street",
            type = ClipType.VIDEO_SAMPLE,
            assetResId = R.drawable.cyberpunk_city,
            startTimeMs = 0L,
            durationMs = 10_000L,
            baseTransform = TransformState(scaleX = 1.0f, scaleY = 1.0f),
            keyframeTracks = mapOf(
                AnimatableProperty.SCALE to KeyframeTrack(
                    property = AnimatableProperty.SCALE,
                    keyframes = listOf(
                        Keyframe(timeMs = 0L, value = 1.0f, easing = EasingType.CubicBezier(0.25f, 0.1f, 0.25f, 1.0f)),
                        Keyframe(timeMs = 3000L, value = 1.35f, easing = EasingType.CubicBezier(0.42f, 0.0f, 0.58f, 1.0f)),
                        Keyframe(timeMs = 7000L, value = 1.15f, easing = EasingType.CubicBezier(0.17f, 0.67f, 0.83f, 0.67f)),
                        Keyframe(timeMs = 10000L, value = 1.45f)
                    )
                ),
                AnimatableProperty.POSITION_X to KeyframeTrack(
                    property = AnimatableProperty.POSITION_X,
                    keyframes = listOf(
                        Keyframe(timeMs = 0L, value = 0f, easing = EasingType.EaseInOut),
                        Keyframe(timeMs = 3000L, value = -35f, easing = EasingType.EaseInOut),
                        Keyframe(timeMs = 7000L, value = 40f, easing = EasingType.EaseInOut),
                        Keyframe(timeMs = 10000L, value = 0f)
                    )
                ),
                AnimatableProperty.ROTATION to KeyframeTrack(
                    property = AnimatableProperty.ROTATION,
                    keyframes = listOf(
                        Keyframe(timeMs = 0L, value = 0f),
                        Keyframe(timeMs = 3000L, value = -3.5f),
                        Keyframe(timeMs = 7000L, value = 2.0f),
                        Keyframe(timeMs = 10000L, value = 0f)
                    )
                )
            ),
            colorGrading = ColorGradingSettings(
                lutPreset = LutPreset.CYBER_NEON,
                lutIntensity = 0.95f,
                exposure = 0.25f,
                contrast = 1.25f,
                saturation = 1.30f,
                temperature = -20f,
                tint = 30f,
                lift = ColorWheelValue(hue = 190f, saturation = 0.45f, luminance = -0.05f),
                gain = ColorWheelValue(hue = 295f, saturation = 0.50f, luminance = 0.10f)
            ),
            effects = listOf(
                VideoEffect(type = EffectType.CHROMATIC_ABERRATION, intensity = 0.65f),
                VideoEffect(type = EffectType.GLOW, intensity = 0.50f),
                VideoEffect(type = EffectType.SCANLINES, intensity = 0.30f)
            )
        )

        val titleClip = ClipItem(
            id = "clip_cyber_title",
            trackId = textTrackId,
            title = "NEON PROTOCOL",
            type = ClipType.TEXT,
            startTimeMs = 1000L,
            durationMs = 6000L,
            baseTransform = TransformState(positionY = -120f, scaleX = 1f, scaleY = 1f),
            keyframeTracks = mapOf(
                AnimatableProperty.OPACITY to KeyframeTrack(
                    property = AnimatableProperty.OPACITY,
                    keyframes = listOf(
                        Keyframe(timeMs = 0L, value = 0f, easing = EasingType.EaseOut),
                        Keyframe(timeMs = 600L, value = 1.0f),
                        Keyframe(timeMs = 5000L, value = 1.0f),
                        Keyframe(timeMs = 6000L, value = 0f)
                    )
                ),
                AnimatableProperty.SCALE to KeyframeTrack(
                    property = AnimatableProperty.SCALE,
                    keyframes = listOf(
                        Keyframe(timeMs = 0L, value = 0.5f, easing = EasingType.Bounce),
                        Keyframe(timeMs = 800L, value = 1.1f, easing = EasingType.EaseInOut),
                        Keyframe(timeMs = 6000L, value = 1.25f)
                    )
                )
            ),
            textConfig = TextConfig(
                text = "NEON PROTOCOL",
                fontSizeSp = 34f,
                textColor = 0xFF00F0FF,
                isBold = true,
                strokeColor = 0xFF9900FF,
                strokeWidth = 3f,
                letterSpacing = 4f
            )
        )

        val shapeClip = ClipItem(
            id = "clip_cyber_badge",
            trackId = shapeTrackId,
            title = "Tech HUD Hexagon",
            type = ClipType.SHAPE,
            startTimeMs = 1500L,
            durationMs = 5000L,
            baseTransform = TransformState(positionY = 180f, scaleX = 0.85f, scaleY = 0.85f),
            shapeConfig = ShapeConfig(
                type = ShapeType.POLYGON,
                fillColor = 0x2200F0FF,
                strokeColor = 0xFF00F0FF,
                strokeWidth = 3f
            ),
            keyframeTracks = mapOf(
                AnimatableProperty.ROTATION to KeyframeTrack(
                    property = AnimatableProperty.ROTATION,
                    keyframes = listOf(
                        Keyframe(timeMs = 0L, value = 0f, easing = EasingType.Linear),
                        Keyframe(timeMs = 5000L, value = 360f)
                    )
                )
            )
        )

        val audioClip = ClipItem(
            id = "clip_synth_bgm",
            trackId = audioTrackId,
            title = "Dark Electro Waveform (Master)",
            type = ClipType.COLOR_BG,
            colorHex = 0xFF144D38,
            startTimeMs = 0L,
            durationMs = 10_000L,
            volume = 0.85f
        )

        return Project(
            id = projectId,
            title = "Cyberpunk Neon Trailer",
            aspectRatio = AspectRatio.PORTRAIT_9_16,
            frameRate = FrameRate.FPS_60,
            resolution = ExportResolution.FHD_1080P,
            durationMs = 10_000L,
            tracks = listOf(
                Track(id = mainVideoTrackId, name = "Video Track 1", type = TrackType.MAIN_VIDEO, clips = listOf(cityClip)),
                Track(id = shapeTrackId, name = "Vector Motion Graphics", type = TrackType.OVERLAY, clips = listOf(shapeClip)),
                Track(id = textTrackId, name = "Titles & Typography", type = TrackType.TEXT_STICKER, clips = listOf(titleClip)),
                Track(id = audioTrackId, name = "Audio BGM", type = TrackType.AUDIO, clips = listOf(audioClip))
            )
        )
    }

    fun createDroneCinemaProject(): Project {
        val projectId = "project_drone_cinema"
        val mainTrackId = "track_main_drone"
        val textTrackId = "track_text_drone"

        val droneClip = ClipItem(
            id = "clip_drone_coast",
            trackId = mainTrackId,
            title = "Pacific Waves Aerial",
            type = ClipType.VIDEO_SAMPLE,
            assetResId = R.drawable.drone_coastline,
            startTimeMs = 0L,
            durationMs = 12_000L,
            baseTransform = TransformState(scaleX = 1.1f, scaleY = 1.1f),
            keyframeTracks = mapOf(
                AnimatableProperty.POSITION_Y to KeyframeTrack(
                    property = AnimatableProperty.POSITION_Y,
                    keyframes = listOf(
                        Keyframe(timeMs = 0L, value = -30f, easing = EasingType.EaseInOut),
                        Keyframe(timeMs = 12_000L, value = 30f)
                    )
                ),
                AnimatableProperty.SCALE to KeyframeTrack(
                    property = AnimatableProperty.SCALE,
                    keyframes = listOf(
                        Keyframe(timeMs = 0L, value = 1.10f, easing = EasingType.EaseOut),
                        Keyframe(timeMs = 12_000L, value = 1.30f)
                    )
                )
            ),
            colorGrading = ColorGradingSettings(
                lutPreset = LutPreset.TEAL_AND_ORANGE,
                lutIntensity = 1.0f,
                exposure = 0.15f,
                contrast = 1.20f,
                temperature = 12f,
                tint = -5f,
                saturation = 1.25f,
                lift = ColorWheelValue(hue = 195f, saturation = 0.35f, luminance = -0.05f),
                gain = ColorWheelValue(hue = 38f, saturation = 0.45f, luminance = 0.12f)
            ),
            effects = listOf(
                VideoEffect(type = EffectType.FILM_GRAIN, intensity = 0.35f),
                VideoEffect(type = EffectType.LENS_DISTORTION, intensity = 0.20f)
            )
        )

        val titleClip = ClipItem(
            id = "clip_coast_title",
            trackId = textTrackId,
            title = "Cinematic Pacific",
            type = ClipType.TEXT,
            startTimeMs = 1500L,
            durationMs = 8000L,
            baseTransform = TransformState(positionY = 140f),
            textConfig = TextConfig(
                text = "PACIFIC DRIFT // 4K",
                fontSizeSp = 26f,
                textColor = 0xFFFFFFFF,
                isBold = true,
                strokeColor = 0xAA000000,
                strokeWidth = 2f,
                letterSpacing = 6f
            ),
            keyframeTracks = mapOf(
                AnimatableProperty.OPACITY to KeyframeTrack(
                    property = AnimatableProperty.OPACITY,
                    keyframes = listOf(
                        Keyframe(timeMs = 0L, value = 0f, easing = EasingType.EaseInOut),
                        Keyframe(timeMs = 1000L, value = 1.0f),
                        Keyframe(timeMs = 7000L, value = 1.0f),
                        Keyframe(timeMs = 8000L, value = 0f)
                    )
                )
            )
        )

        return Project(
            id = projectId,
            title = "Drone Coastline Cinema",
            aspectRatio = AspectRatio.LANDSCAPE_16_9,
            frameRate = FrameRate.FPS_24,
            resolution = ExportResolution.UHD_4K,
            durationMs = 12_000L,
            tracks = listOf(
                Track(id = mainTrackId, name = "Aerial 4K Track", type = TrackType.MAIN_VIDEO, clips = listOf(droneClip)),
                Track(id = textTrackId, name = "Cinematic Lower Third", type = TrackType.TEXT_STICKER, clips = listOf(titleClip))
            )
        )
    }

    fun createSunsetSkaterProject(): Project {
        val projectId = "project_sunset_skater"
        val mainTrackId = "track_main_skater"

        val skaterClip = ClipItem(
            id = "clip_sunset_skater",
            trackId = mainTrackId,
            title = "Golden Hour Skate Flow",
            type = ClipType.VIDEO_SAMPLE,
            assetResId = R.drawable.sunset_skater,
            startTimeMs = 0L,
            durationMs = 8_000L,
            baseTransform = TransformState(scaleX = 1.0f, scaleY = 1.0f),
            keyframeTracks = mapOf(
                AnimatableProperty.SCALE to KeyframeTrack(
                    property = AnimatableProperty.SCALE,
                    keyframes = listOf(
                        Keyframe(timeMs = 0L, value = 1.0f, easing = EasingType.EaseOut),
                        Keyframe(timeMs = 2500L, value = 1.40f, easing = EasingType.Bounce),
                        Keyframe(timeMs = 4000L, value = 1.15f, easing = EasingType.EaseInOut),
                        Keyframe(timeMs = 8000L, value = 1.30f)
                    )
                ),
                AnimatableProperty.ROTATION to KeyframeTrack(
                    property = AnimatableProperty.ROTATION,
                    keyframes = listOf(
                        Keyframe(timeMs = 2000L, value = 0f, easing = EasingType.Elastic),
                        Keyframe(timeMs = 2800L, value = -6f, easing = EasingType.EaseInOut),
                        Keyframe(timeMs = 3600L, value = 0f)
                    )
                )
            ),
            colorGrading = ColorGradingSettings(
                lutPreset = LutPreset.GOLDEN_HOUR,
                lutIntensity = 1.0f,
                exposure = 0.20f,
                contrast = 1.15f,
                temperature = 35f,
                tint = 10f,
                saturation = 1.20f
            ),
            effects = listOf(
                VideoEffect(type = EffectType.MOTION_BLUR, intensity = 0.60f),
                VideoEffect(type = EffectType.CAMERA_SHAKE, intensity = 0.40f)
            )
        )

        return Project(
            id = projectId,
            title = "Sunset Skater Flow",
            aspectRatio = AspectRatio.PORTRAIT_9_16,
            frameRate = FrameRate.FPS_60,
            resolution = ExportResolution.FHD_1080P,
            durationMs = 8_000L,
            tracks = listOf(
                Track(id = mainTrackId, name = "Action Clip Track", type = TrackType.MAIN_VIDEO, clips = listOf(skaterClip))
            )
        )
    }

    fun getAllInitialProjects(): List<Project> {
        return listOf(
            createCyberpunkProject(),
            createDroneCinemaProject(),
            createSunsetSkaterProject()
        )
    }
}
