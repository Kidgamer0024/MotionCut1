package com.example.data.local

import com.example.model.AnimatableProperty
import com.example.model.AspectRatio
import com.example.model.CameraTrackPoint
import com.example.model.ChannelCurve
import com.example.model.ClipItem
import com.example.model.ClipType
import com.example.model.ColorGradingSettings
import com.example.model.ColorWheelValue
import com.example.model.CurveBreakpoint
import com.example.model.CurvePoint
import com.example.model.EasingType
import com.example.model.EffectKernel
import com.example.model.EffectSuite
import com.example.model.EffectType
import com.example.model.ExportResolution
import com.example.model.FrameRate
import com.example.model.HslAdjustment
import com.example.model.HslBand
import com.example.model.Keyframe
import com.example.model.KeyframeTrack
import com.example.model.Library3DModel
import com.example.model.LutPreset
import com.example.model.MaskConfig
import com.example.model.MaskKeyframe
import com.example.model.MaskShapeType
import com.example.model.MaskVertex
import com.example.model.Model3DConfig
import com.example.model.Model3DSource
import com.example.model.Primitive3DShape
import com.example.model.Project
import com.example.model.RenderMode3D
import com.example.model.ShapeConfig
import com.example.model.ShapeType
import com.example.model.TextAnimationType
import com.example.model.TextConfig
import com.example.model.TextFontChoice
import com.example.model.Track
import com.example.model.TrackType
import com.example.model.TransformState
import com.example.model.VideoEffect
import org.json.JSONArray
import org.json.JSONObject

/**
 * Dependency-free JSON serializer for full [Project] timelines (tracks, clips, keyframes,
 * easing breakpoints, color grading, effects, masks, 3D model configs, and camera tracking).
 * Uses Android's built-in org.json so Room & Firestore preserve full timelines seamlessly.
 */
object ProjectJsonCodec {

    fun encode(project: Project): String {
        val root = JSONObject()
        root.put("version", 2)
        root.put("id", project.id)
        root.put("title", project.title)
        root.put("aspectRatio", project.aspectRatio.name)
        root.put("frameRate", project.frameRate.name)
        root.put("resolution", project.resolution.name)
        root.put("durationMs", project.durationMs)
        root.put("createdAt", project.createdAt)
        root.put("updatedAt", project.updatedAt)
        root.put("masterColorGrading", encodeColorGrading(project.masterColorGrading))

        val tracksArr = JSONArray()
        project.tracks.forEach { track ->
            tracksArr.put(encodeTrack(track))
        }
        root.put("tracks", tracksArr)
        return root.toString()
    }

    fun encodeBoundedForCloud(project: Project, maxChars: Int = 840_000): String {
        val full = encode(project)
        if (full.length <= maxChars) return full
        val trimmedWaveforms = project.copy(
            tracks = project.tracks.map { track ->
                track.copy(
                    clips = track.clips.map { clip ->
                        clip.copy(
                            waveformSamples = clip.waveformSamples.take(32),
                            cameraTrackPoints = clip.cameraTrackPoints.take(60)
                        )
                    }
                )
            }
        )
        val pass1 = encode(trimmedWaveforms)
        if (pass1.length <= maxChars) return pass1
        val strippedSamples = project.copy(
            tracks = project.tracks.map { track ->
                track.copy(
                    clips = track.clips.map { clip ->
                        clip.copy(waveformSamples = emptyList(), cameraTrackPoints = emptyList())
                    }
                )
            }
        )
        return encode(strippedSamples)
    }

    fun decode(jsonStr: String): Project? {
        if (jsonStr.isBlank()) return null
        return runCatching {
            val root = JSONObject(jsonStr)
            val id = root.optString("id", "").ifBlank { return null }
            val title = root.optString("title", "Untitled Project")
            val aspectRatio = safeEnum(root.optString("aspectRatio"), AspectRatio.PORTRAIT_9_16)
            val frameRate = safeEnum(root.optString("frameRate"), FrameRate.FPS_60)
            val resolution = safeEnum(root.optString("resolution"), ExportResolution.FHD_1080P)
            val durationMs = root.optLong("durationMs", 10_000L).coerceAtLeast(500L)
            val createdAt = root.optLong("createdAt", System.currentTimeMillis())
            val updatedAt = root.optLong("updatedAt", createdAt)
            val masterGrading = root.optJSONObject("masterColorGrading")
                ?.let { decodeColorGrading(it) } ?: ColorGradingSettings()

            val tracks = mutableListOf<Track>()
            val tracksArr = root.optJSONArray("tracks")
            if (tracksArr != null) {
                for (i in 0 until tracksArr.length()) {
                    val obj = tracksArr.optJSONObject(i) ?: continue
                    tracks += decodeTrack(obj)
                }
            }

            Project(
                id = id,
                title = title,
                aspectRatio = aspectRatio,
                frameRate = frameRate,
                resolution = resolution,
                durationMs = durationMs,
                tracks = tracks,
                masterColorGrading = masterGrading,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        }.getOrNull()
    }

    private fun encodeTrack(track: Track): JSONObject {
        val obj = JSONObject()
        obj.put("id", track.id)
        obj.put("name", track.name)
        obj.put("type", track.type.name)
        obj.put("isMuted", track.isMuted)
        obj.put("isLocked", track.isLocked)
        obj.put("isVisible", track.isVisible)
        val clipsArr = JSONArray()
        track.clips.forEach { clipsArr.put(encodeClip(it)) }
        obj.put("clips", clipsArr)
        return obj
    }

    private fun decodeTrack(obj: JSONObject): Track {
        val clips = mutableListOf<ClipItem>()
        val clipsArr = obj.optJSONArray("clips")
        if (clipsArr != null) {
            for (i in 0 until clipsArr.length()) {
                val clipObj = clipsArr.optJSONObject(i) ?: continue
                clips += decodeClip(clipObj)
            }
        }
        return Track(
            id = obj.optString("id"),
            name = obj.optString("name", "Track"),
            type = safeEnum(obj.optString("type"), TrackType.MAIN_VIDEO),
            isMuted = obj.optBoolean("isMuted", false),
            isLocked = obj.optBoolean("isLocked", false),
            isVisible = obj.optBoolean("isVisible", true),
            clips = clips
        )
    }

    private fun encodeClip(clip: ClipItem): JSONObject {
        val obj = JSONObject()
        obj.put("id", clip.id)
        obj.put("trackId", clip.trackId)
        obj.put("title", clip.title)
        obj.put("type", clip.type.name)
        if (clip.assetResId != null) obj.put("assetResId", clip.assetResId)
        if (clip.mediaUri != null) obj.put("mediaUri", clip.mediaUri)
        obj.put("colorHex", clip.colorHex)
        obj.put("startTimeMs", clip.startTimeMs)
        obj.put("durationMs", clip.durationMs)
        obj.put("speed", clip.speed.toDouble())
        obj.put("preserveAudioPitch", clip.preserveAudioPitch)
        obj.put("isReversed", clip.isReversed)
        obj.put("volume", clip.volume.toDouble())
        obj.put("isLocked", clip.isLocked)

        obj.put("baseTransform", JSONObject().apply {
            put("positionX", clip.baseTransform.positionX.toDouble())
            put("positionY", clip.baseTransform.positionY.toDouble())
            put("scaleX", clip.baseTransform.scaleX.toDouble())
            put("scaleY", clip.baseTransform.scaleY.toDouble())
            put("rotation", clip.baseTransform.rotation.toDouble())
            put("opacity", clip.baseTransform.opacity.toDouble())
        })

        val kfTracksObj = JSONObject()
        clip.keyframeTracks.forEach { (prop, track) ->
            val arr = JSONArray()
            track.keyframes.forEach { kf ->
                arr.put(JSONObject().apply {
                    put("id", kf.id)
                    put("timeMs", kf.timeMs)
                    put("value", kf.value.toDouble())
                    put("easing", encodeEasing(kf.easing))
                })
            }
            kfTracksObj.put(prop.name, arr)
        }
        obj.put("keyframeTracks", kfTracksObj)

        obj.put("colorGrading", encodeColorGrading(clip.colorGrading))

        val effectsArr = JSONArray()
        clip.effects.forEach { eff ->
            effectsArr.put(JSONObject().apply {
                put("id", eff.id)
                put("type", eff.type.name)
                put("isEnabled", eff.isEnabled)
                put("intensity", eff.intensity.toDouble())
                put("speed", eff.speed.toDouble())
                if (eff.presetId != null) put("presetId", eff.presetId)
                if (eff.customName != null) put("customName", eff.customName)
                if (eff.suite != null) put("suite", eff.suite.name)
                if (eff.category != null) put("category", eff.category)
                if (eff.kernel != null) put("kernel", eff.kernel.name)
                put("paramA", eff.paramA.toDouble())
                put("paramB", eff.paramB.toDouble())
                put("colorHex", eff.colorHex)
                put("paramALabel", eff.paramALabel)
                put("paramBLabel", eff.paramBLabel)
            })
        }
        obj.put("effects", effectsArr)

        val masksArr = JSONArray()
        clip.masks.forEach { mask -> masksArr.put(encodeMask(mask)) }
        obj.put("masks", masksArr)

        clip.textConfig?.let { tc ->
            obj.put("textConfig", JSONObject().apply {
                put("text", tc.text)
                put("fontSizeSp", tc.fontSizeSp.toDouble())
                put("textColor", tc.textColor)
                put("isBold", tc.isBold)
                put("strokeColor", tc.strokeColor)
                put("strokeWidth", tc.strokeWidth.toDouble())
                put("letterSpacing", tc.letterSpacing.toDouble())
                put("fontChoice", tc.fontChoice.name)
                put("animationType", tc.animationType.name)
                put("animationSpeed", tc.animationSpeed.toDouble())
                put("hasBackgroundPill", tc.hasBackgroundPill)
                put("backgroundColor", tc.backgroundColor)
                put("backgroundCornerRadius", tc.backgroundCornerRadius.toDouble())
                put("shadowEnabled", tc.shadowEnabled)
                if (tc.templateId != null) put("templateId", tc.templateId)
            })
        }

        clip.shapeConfig?.let { sc ->
            obj.put("shapeConfig", JSONObject().apply {
                put("type", sc.type.name)
                put("fillColor", sc.fillColor)
                put("strokeColor", sc.strokeColor)
                put("strokeWidth", sc.strokeWidth.toDouble())
                put("cornerRadius", sc.cornerRadius.toDouble())
            })
        }

        clip.model3dConfig?.let { m3 ->
            obj.put("model3dConfig", JSONObject().apply {
                put("source", m3.source.name)
                put("primitiveShape", m3.primitiveShape.name)
                put("libraryModel", m3.libraryModel.name)
                if (m3.importedUri != null) put("importedUri", m3.importedUri)
                if (m3.importedFileName != null) put("importedFileName", m3.importedFileName)
                put("extrudedText", m3.extrudedText)
                if (m3.imageUri != null) put("imageUri", m3.imageUri)
                put("textureMix", m3.textureMix.toDouble())
                put("rotationX", m3.rotationX.toDouble())
                put("rotationY", m3.rotationY.toDouble())
                put("rotationZ", m3.rotationZ.toDouble())
                put("depthZ", m3.depthZ.toDouble())
                put("extrusionDepth", m3.extrusionDepth.toDouble())
                put("modelScale", m3.modelScale.toDouble())
                put("fovDegrees", m3.fovDegrees.toDouble())
                put("materialColorHex", m3.materialColorHex)
                put("metallic", m3.metallic.toDouble())
                put("lightAzimuthDeg", m3.lightAzimuthDeg.toDouble())
                put("lightElevationDeg", m3.lightElevationDeg.toDouble())
                put("renderMode", m3.renderMode.name)
                put("showEdges", m3.showEdges)
            })
        }

        if (clip.waveformSamples.isNotEmpty()) {
            val wf = JSONArray()
            clip.waveformSamples.take(96).forEach { wf.put(it.toDouble()) }
            obj.put("waveformSamples", wf)
        }

        if (clip.cameraTrackPoints.isNotEmpty()) {
            val ct = JSONArray()
            clip.cameraTrackPoints.take(240).forEach { pt ->
                ct.put(JSONObject().apply {
                    put("timeMs", pt.timeMs)
                    put("x", pt.x.toDouble())
                    put("y", pt.y.toDouble())
                    put("scale", pt.scale.toDouble())
                    put("rotation", pt.rotation.toDouble())
                    put("confidence", pt.confidence.toDouble())
                })
            }
            obj.put("cameraTrackPoints", ct)
        }

        return obj
    }

    private fun decodeClip(obj: JSONObject): ClipItem {
        val btObj = obj.optJSONObject("baseTransform")
        val baseTransform = if (btObj != null) {
            TransformState(
                positionX = btObj.optDouble("positionX", 0.0).toFloat(),
                positionY = btObj.optDouble("positionY", 0.0).toFloat(),
                scaleX = btObj.optDouble("scaleX", 1.0).toFloat(),
                scaleY = btObj.optDouble("scaleY", 1.0).toFloat(),
                rotation = btObj.optDouble("rotation", 0.0).toFloat(),
                opacity = btObj.optDouble("opacity", 1.0).toFloat()
            )
        } else TransformState()

        val kfMap = mutableMapOf<AnimatableProperty, KeyframeTrack>()
        val kfTracksObj = obj.optJSONObject("keyframeTracks")
        if (kfTracksObj != null) {
            val keys = kfTracksObj.keys()
            while (keys.hasNext()) {
                val propKey = keys.next()
                val prop = runCatching { AnimatableProperty.valueOf(propKey) }.getOrNull() ?: continue
                val arr = kfTracksObj.optJSONArray(propKey) ?: continue
                val list = mutableListOf<Keyframe>()
                for (i in 0 until arr.length()) {
                    val kfObj = arr.optJSONObject(i) ?: continue
                    list += Keyframe(
                        id = kfObj.optString("id"),
                        timeMs = kfObj.optLong("timeMs", 0L),
                        value = kfObj.optDouble("value", prop.defaultValue.toDouble()).toFloat(),
                        easing = kfObj.optJSONObject("easing")?.let { decodeEasing(it) }
                            ?: EasingType.CubicBezier(0.42f, 0f, 0.58f, 1f)
                    )
                }
                if (list.isNotEmpty()) {
                    kfMap[prop] = KeyframeTrack(property = prop, keyframes = list.sortedBy { it.timeMs })
                }
            }
        }

        val effects = mutableListOf<VideoEffect>()
        val effectsArr = obj.optJSONArray("effects")
        if (effectsArr != null) {
            for (i in 0 until effectsArr.length()) {
                val eObj = effectsArr.optJSONObject(i) ?: continue
                effects += VideoEffect(
                    id = eObj.optString("id"),
                    type = safeEnum(eObj.optString("type"), EffectType.GLOW),
                    isEnabled = eObj.optBoolean("isEnabled", true),
                    intensity = eObj.optDouble("intensity", 0.5).toFloat(),
                    speed = eObj.optDouble("speed", 1.0).toFloat(),
                    presetId = eObj.optString("presetId").ifBlank { null },
                    customName = eObj.optString("customName").ifBlank { null },
                    suite = eObj.optString("suite").takeIf { it.isNotBlank() }
                        ?.let { runCatching { EffectSuite.valueOf(it) }.getOrNull() },
                    category = eObj.optString("category").ifBlank { null },
                    kernel = eObj.optString("kernel").takeIf { it.isNotBlank() }
                        ?.let { runCatching { EffectKernel.valueOf(it) }.getOrNull() },
                    paramA = eObj.optDouble("paramA", 0.5).toFloat(),
                    paramB = eObj.optDouble("paramB", 0.5).toFloat(),
                    colorHex = eObj.optLong("colorHex", 0xFF00E5FF),
                    paramALabel = eObj.optString("paramALabel", "Detail"),
                    paramBLabel = eObj.optString("paramBLabel", "Phase")
                )
            }
        }

        val masks = mutableListOf<MaskConfig>()
        val masksArr = obj.optJSONArray("masks")
        if (masksArr != null) {
            for (i in 0 until masksArr.length()) {
                val mObj = masksArr.optJSONObject(i) ?: continue
                masks += decodeMask(mObj)
            }
        }

        val textConfig = obj.optJSONObject("textConfig")?.let { tc ->
            TextConfig(
                text = tc.optString("text", "MOTION CUT"),
                fontSizeSp = tc.optDouble("fontSizeSp", 32.0).toFloat(),
                textColor = tc.optLong("textColor", 0xFFFFFFFF),
                isBold = tc.optBoolean("isBold", true),
                strokeColor = tc.optLong("strokeColor", 0xFF000000),
                strokeWidth = tc.optDouble("strokeWidth", 2.0).toFloat(),
                letterSpacing = tc.optDouble("letterSpacing", 2.0).toFloat(),
                fontChoice = safeEnum(tc.optString("fontChoice"), TextFontChoice.MONTSERRAT),
                animationType = safeEnum(tc.optString("animationType"), TextAnimationType.NONE),
                animationSpeed = tc.optDouble("animationSpeed", 1.0).toFloat(),
                hasBackgroundPill = tc.optBoolean("hasBackgroundPill", false),
                backgroundColor = tc.optLong("backgroundColor", 0xAA0A0E17),
                backgroundCornerRadius = tc.optDouble("backgroundCornerRadius", 14.0).toFloat(),
                shadowEnabled = tc.optBoolean("shadowEnabled", true),
                templateId = tc.optString("templateId").ifBlank { null }
            )
        }

        val shapeConfig = obj.optJSONObject("shapeConfig")?.let { sc ->
            ShapeConfig(
                type = safeEnum(sc.optString("type"), ShapeType.RECTANGLE),
                fillColor = sc.optLong("fillColor", 0xFF00E5FF),
                strokeColor = sc.optLong("strokeColor", 0xFFFFFFFF),
                strokeWidth = sc.optDouble("strokeWidth", 4.0).toFloat(),
                cornerRadius = sc.optDouble("cornerRadius", 16.0).toFloat()
            )
        }

        val model3dConfig = obj.optJSONObject("model3dConfig")?.let { m3 ->
            Model3DConfig(
                source = safeEnum(m3.optString("source"), Model3DSource.PRIMITIVE),
                primitiveShape = safeEnum(m3.optString("primitiveShape"), Primitive3DShape.CUBE),
                libraryModel = safeEnum(m3.optString("libraryModel"), Library3DModel.ROCKET),
                importedUri = m3.optString("importedUri").ifBlank { null },
                importedFileName = m3.optString("importedFileName").ifBlank { null },
                extrudedText = m3.optString("extrudedText", "MOTION 3D"),
                imageUri = m3.optString("imageUri").ifBlank { null },
                textureMix = m3.optDouble("textureMix", 0.85).toFloat(),
                rotationX = m3.optDouble("rotationX", 18.0).toFloat(),
                rotationY = m3.optDouble("rotationY", -28.0).toFloat(),
                rotationZ = m3.optDouble("rotationZ", 0.0).toFloat(),
                depthZ = m3.optDouble("depthZ", 0.0).toFloat(),
                extrusionDepth = m3.optDouble("extrusionDepth", 0.40).toFloat(),
                modelScale = m3.optDouble("modelScale", 1.0).toFloat(),
                fovDegrees = m3.optDouble("fovDegrees", 45.0).toFloat(),
                materialColorHex = m3.optLong("materialColorHex", 0xFF00E5FF),
                metallic = m3.optDouble("metallic", 0.35).toFloat(),
                lightAzimuthDeg = m3.optDouble("lightAzimuthDeg", 40.0).toFloat(),
                lightElevationDeg = m3.optDouble("lightElevationDeg", 35.0).toFloat(),
                renderMode = safeEnum(m3.optString("renderMode"), RenderMode3D.SHADED),
                showEdges = m3.optBoolean("showEdges", true)
            )
        }

        val waveform = mutableListOf<Float>()
        val wfArr = obj.optJSONArray("waveformSamples")
        if (wfArr != null) {
            for (i in 0 until wfArr.length()) waveform += wfArr.optDouble(i, 0.0).toFloat()
        }

        val trackPts = mutableListOf<CameraTrackPoint>()
        val ctArr = obj.optJSONArray("cameraTrackPoints")
        if (ctArr != null) {
            for (i in 0 until ctArr.length()) {
                val pObj = ctArr.optJSONObject(i) ?: continue
                trackPts += CameraTrackPoint(
                    timeMs = pObj.optLong("timeMs", 0L),
                    x = pObj.optDouble("x", 0.0).toFloat(),
                    y = pObj.optDouble("y", 0.0).toFloat(),
                    scale = pObj.optDouble("scale", 1.0).toFloat(),
                    rotation = pObj.optDouble("rotation", 0.0).toFloat(),
                    confidence = pObj.optDouble("confidence", 1.0).toFloat()
                )
            }
        }

        return ClipItem(
            id = obj.optString("id"),
            trackId = obj.optString("trackId"),
            title = obj.optString("title", "Clip"),
            type = safeEnum(obj.optString("type"), ClipType.VIDEO_SAMPLE),
            assetResId = if (obj.has("assetResId")) obj.optInt("assetResId") else null,
            mediaUri = obj.optString("mediaUri").ifBlank { null },
            colorHex = obj.optLong("colorHex", 0xFF121722),
            startTimeMs = obj.optLong("startTimeMs", 0L),
            durationMs = obj.optLong("durationMs", 3000L).coerceAtLeast(200L),
            speed = obj.optDouble("speed", 1.0).toFloat().coerceIn(0.1f, 8.0f),
            preserveAudioPitch = obj.optBoolean("preserveAudioPitch", true),
            isReversed = obj.optBoolean("isReversed", false),
            volume = obj.optDouble("volume", 1.0).toFloat(),
            isLocked = obj.optBoolean("isLocked", false),
            baseTransform = baseTransform,
            keyframeTracks = kfMap,
            colorGrading = obj.optJSONObject("colorGrading")?.let { decodeColorGrading(it) }
                ?: ColorGradingSettings(),
            effects = effects,
            masks = masks,
            textConfig = textConfig,
            shapeConfig = shapeConfig,
            model3dConfig = model3dConfig,
            waveformSamples = waveform,
            cameraTrackPoints = trackPts
        )
    }

    private fun encodeEasing(easing: EasingType): JSONObject {
        val obj = JSONObject()
        when (easing) {
            is EasingType.Linear -> obj.put("kind", "Linear")
            is EasingType.EaseIn -> obj.put("kind", "EaseIn")
            is EasingType.EaseOut -> obj.put("kind", "EaseOut")
            is EasingType.EaseInOut -> obj.put("kind", "EaseInOut")
            is EasingType.Bounce -> obj.put("kind", "Bounce")
            is EasingType.Elastic -> obj.put("kind", "Elastic")
            is EasingType.CubicBezier -> {
                obj.put("kind", "CubicBezier")
                obj.put("p1x", easing.p1x.toDouble())
                obj.put("p1y", easing.p1y.toDouble())
                obj.put("p2x", easing.p2x.toDouble())
                obj.put("p2y", easing.p2y.toDouble())
            }
            is EasingType.MultiPoint -> {
                obj.put("kind", "MultiPoint")
                val arr = JSONArray()
                easing.points.forEach { pt ->
                    arr.put(JSONObject().apply {
                        put("x", pt.x.toDouble())
                        put("y", pt.y.toDouble())
                    })
                }
                obj.put("points", arr)
            }
        }
        return obj
    }

    private fun decodeEasing(obj: JSONObject): EasingType {
        return when (obj.optString("kind")) {
            "Linear" -> EasingType.Linear
            "EaseIn" -> EasingType.EaseIn
            "EaseOut" -> EasingType.EaseOut
            "EaseInOut" -> EasingType.EaseInOut
            "Bounce" -> EasingType.Bounce
            "Elastic" -> EasingType.Elastic
            "MultiPoint" -> {
                val arr = obj.optJSONArray("points")
                val pts = mutableListOf<CurveBreakpoint>()
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val p = arr.optJSONObject(i) ?: continue
                        pts += CurveBreakpoint(
                            x = p.optDouble("x", 0.5).toFloat().coerceIn(0.02f, 0.98f),
                            y = p.optDouble("y", 0.5).toFloat().coerceIn(-0.4f, 1.4f)
                        )
                    }
                }
                EasingType.MultiPoint(pts.ifEmpty {
                    listOf(CurveBreakpoint(0.3f, 0.2f), CurveBreakpoint(0.7f, 0.8f))
                })
            }
            else -> EasingType.CubicBezier(
                p1x = obj.optDouble("p1x", 0.42).toFloat(),
                p1y = obj.optDouble("p1y", 0.0).toFloat(),
                p2x = obj.optDouble("p2x", 0.58).toFloat(),
                p2y = obj.optDouble("p2y", 1.0).toFloat()
            )
        }
    }

    private fun encodeColorGrading(cg: ColorGradingSettings): JSONObject {
        val obj = JSONObject()
        fun encWheel(w: ColorWheelValue) = JSONObject().apply {
            put("h", w.hue.toDouble())
            put("s", w.saturation.toDouble())
            put("l", w.luminance.toDouble())
        }
        fun encCurve(c: ChannelCurve) = JSONArray().apply {
            c.points.forEach { p ->
                put(JSONObject().apply {
                    put("x", p.x.toDouble())
                    put("y", p.y.toDouble())
                })
            }
        }
        obj.put("lift", encWheel(cg.lift))
        obj.put("gamma", encWheel(cg.gamma))
        obj.put("gain", encWheel(cg.gain))
        obj.put("offset", encWheel(cg.offset))
        obj.put("exposure", cg.exposure.toDouble())
        obj.put("contrast", cg.contrast.toDouble())
        obj.put("temperature", cg.temperature.toDouble())
        obj.put("tint", cg.tint.toDouble())
        obj.put("highlights", cg.highlights.toDouble())
        obj.put("shadows", cg.shadows.toDouble())
        obj.put("vibrance", cg.vibrance.toDouble())
        obj.put("saturation", cg.saturation.toDouble())
        obj.put("vignette", cg.vignette.toDouble())
        obj.put("sharpness", cg.sharpness.toDouble())
        obj.put("masterCurve", encCurve(cg.masterCurve))
        obj.put("redCurve", encCurve(cg.redCurve))
        obj.put("greenCurve", encCurve(cg.greenCurve))
        obj.put("blueCurve", encCurve(cg.blueCurve))
        val hslObj = JSONObject()
        cg.hslAdjustments.forEach { (band, adj) ->
            hslObj.put(band.name, JSONObject().apply {
                put("h", adj.hueShift.toDouble())
                put("s", adj.saturation.toDouble())
                put("l", adj.luminance.toDouble())
            })
        }
        obj.put("hsl", hslObj)
        obj.put("lutPreset", cg.lutPreset.name)
        obj.put("lutIntensity", cg.lutIntensity.toDouble())
        return obj
    }

    private fun decodeColorGrading(obj: JSONObject): ColorGradingSettings {
        fun decWheel(key: String): ColorWheelValue {
            val w = obj.optJSONObject(key) ?: return ColorWheelValue()
            return ColorWheelValue(
                hue = w.optDouble("h", 0.0).toFloat(),
                saturation = w.optDouble("s", 0.0).toFloat(),
                luminance = w.optDouble("l", 0.0).toFloat()
            )
        }
        fun decCurve(key: String): ChannelCurve {
            val arr = obj.optJSONArray(key) ?: return ChannelCurve()
            val pts = mutableListOf<CurvePoint>()
            for (i in 0 until arr.length()) {
                val p = arr.optJSONObject(i) ?: continue
                pts += CurvePoint(p.optDouble("x", 0.0).toFloat(), p.optDouble("y", 0.0).toFloat())
            }
            return if (pts.size >= 2) ChannelCurve(pts) else ChannelCurve()
        }
        val hslObj = obj.optJSONObject("hsl")
        val hslMap = HslBand.entries.associateWith { band ->
            val bObj = hslObj?.optJSONObject(band.name)
            if (bObj != null) {
                HslAdjustment(
                    hueShift = bObj.optDouble("h", 0.0).toFloat(),
                    saturation = bObj.optDouble("s", 0.0).toFloat(),
                    luminance = bObj.optDouble("l", 0.0).toFloat()
                )
            } else HslAdjustment()
        }
        return ColorGradingSettings(
            lift = decWheel("lift"),
            gamma = decWheel("gamma"),
            gain = decWheel("gain"),
            offset = decWheel("offset"),
            exposure = obj.optDouble("exposure", 0.0).toFloat(),
            contrast = obj.optDouble("contrast", 1.0).toFloat(),
            temperature = obj.optDouble("temperature", 0.0).toFloat(),
            tint = obj.optDouble("tint", 0.0).toFloat(),
            highlights = obj.optDouble("highlights", 0.0).toFloat(),
            shadows = obj.optDouble("shadows", 0.0).toFloat(),
            vibrance = obj.optDouble("vibrance", 0.0).toFloat(),
            saturation = obj.optDouble("saturation", 1.0).toFloat(),
            vignette = obj.optDouble("vignette", 0.0).toFloat(),
            sharpness = obj.optDouble("sharpness", 0.0).toFloat(),
            masterCurve = decCurve("masterCurve"),
            redCurve = decCurve("redCurve"),
            greenCurve = decCurve("greenCurve"),
            blueCurve = decCurve("blueCurve"),
            hslAdjustments = hslMap,
            lutPreset = safeEnum(obj.optString("lutPreset"), LutPreset.NONE),
            lutIntensity = obj.optDouble("lutIntensity", 1.0).toFloat()
        )
    }

    private fun encodeMask(mask: MaskConfig): JSONObject {
        val obj = JSONObject()
        obj.put("id", mask.id)
        obj.put("name", mask.name)
        obj.put("shapeType", mask.shapeType.name)
        obj.put("isEnabled", mask.isEnabled)
        obj.put("isInverted", mask.isInverted)
        obj.put("featherPx", mask.featherPx.toDouble())
        obj.put("opacity", mask.opacity.toDouble())
        obj.put("expansion", mask.expansion.toDouble())
        obj.put("cornerRadiusPx", mask.cornerRadiusPx.toDouble())
        obj.put("aspectWidth", mask.aspectWidth.toDouble())
        obj.put("aspectHeight", mask.aspectHeight.toDouble())
        obj.put("borderWidthPx", mask.borderWidthPx.toDouble())
        obj.put("borderColorHex", mask.borderColorHex)
        obj.put("positionX", mask.positionX.toDouble())
        obj.put("positionY", mask.positionY.toDouble())
        obj.put("scaleX", mask.scaleX.toDouble())
        obj.put("scaleY", mask.scaleY.toDouble())
        obj.put("rotation", mask.rotation.toDouble())
        obj.put("isAnimated", mask.isAnimated)
        val vertsArr = JSONArray()
        mask.vertices.forEach { v ->
            vertsArr.put(JSONObject().apply {
                put("id", v.id)
                put("x", v.x.toDouble())
                put("y", v.y.toDouble())
            })
        }
        obj.put("vertices", vertsArr)
        val kfsArr = JSONArray()
        mask.keyframes.forEach { k ->
            kfsArr.put(JSONObject().apply {
                put("id", k.id)
                put("timeMs", k.timeMs)
                put("positionX", k.positionX.toDouble())
                put("positionY", k.positionY.toDouble())
                put("scale", k.scale.toDouble())
                put("rotation", k.rotation.toDouble())
                put("featherPx", k.featherPx.toDouble())
            })
        }
        obj.put("keyframes", kfsArr)
        return obj
    }

    private fun decodeMask(obj: JSONObject): MaskConfig {
        val verts = mutableListOf<MaskVertex>()
        val vArr = obj.optJSONArray("vertices")
        if (vArr != null) {
            for (i in 0 until vArr.length()) {
                val v = vArr.optJSONObject(i) ?: continue
                verts += MaskVertex(
                    id = v.optString("id"),
                    x = v.optDouble("x", 0.0).toFloat(),
                    y = v.optDouble("y", 0.0).toFloat()
                )
            }
        }
        val kfs = mutableListOf<MaskKeyframe>()
        val kArr = obj.optJSONArray("keyframes")
        if (kArr != null) {
            for (i in 0 until kArr.length()) {
                val k = kArr.optJSONObject(i) ?: continue
                kfs += MaskKeyframe(
                    id = k.optString("id"),
                    timeMs = k.optLong("timeMs", 0L),
                    positionX = k.optDouble("positionX", 0.0).toFloat(),
                    positionY = k.optDouble("positionY", 0.0).toFloat(),
                    scale = k.optDouble("scale", 1.0).toFloat(),
                    rotation = k.optDouble("rotation", 0.0).toFloat(),
                    featherPx = k.optDouble("featherPx", 0.0).toFloat()
                )
            }
        }
        return MaskConfig(
            id = obj.optString("id"),
            name = obj.optString("name", "Mask"),
            shapeType = safeEnum(obj.optString("shapeType"), MaskShapeType.RECTANGLE),
            isEnabled = obj.optBoolean("isEnabled", true),
            isInverted = obj.optBoolean("isInverted", false),
            featherPx = obj.optDouble("featherPx", 0.0).toFloat(),
            opacity = obj.optDouble("opacity", 1.0).toFloat(),
            expansion = obj.optDouble("expansion", 0.0).toFloat(),
            cornerRadiusPx = obj.optDouble("cornerRadiusPx", 16.0).toFloat(),
            aspectWidth = obj.optDouble("aspectWidth", 1.0).toFloat(),
            aspectHeight = obj.optDouble("aspectHeight", 1.0).toFloat(),
            borderWidthPx = obj.optDouble("borderWidthPx", 0.0).toFloat(),
            borderColorHex = obj.optLong("borderColorHex", 0xFFFFFFFF),
            positionX = obj.optDouble("positionX", 0.0).toFloat(),
            positionY = obj.optDouble("positionY", 0.0).toFloat(),
            scaleX = obj.optDouble("scaleX", 1.0).toFloat(),
            scaleY = obj.optDouble("scaleY", 1.0).toFloat(),
            rotation = obj.optDouble("rotation", 0.0).toFloat(),
            vertices = if (verts.isNotEmpty()) verts else MaskConfig().vertices,
            isAnimated = obj.optBoolean("isAnimated", false),
            keyframes = kfs
        )
    }

    private inline fun <reified T : Enum<T>> safeEnum(name: String, default: T): T {
        if (name.isBlank()) return default
        return runCatching { enumValueOf<T>(name) }.getOrDefault(default)
    }
}
