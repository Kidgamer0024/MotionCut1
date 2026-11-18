package com.example

import com.example.data.ProjectRepository
import com.example.data.SampleMarketplaceData
import com.example.data.SampleMusicLibrary
import com.example.data.SampleProjects
import com.example.engine.ColorGradingCompositor
import com.example.engine.KeyframeEvaluator
import com.example.engine.MaskCompositor
import com.example.model.AnimatableProperty
import com.example.model.ColorGradingSettings
import com.example.model.EasingType
import com.example.model.Keyframe
import com.example.model.KeyframeTrack
import com.example.model.LutPreset
import com.example.model.MaskConfig
import com.example.model.MaskKeyframe
import com.example.model.MaskShapeType
import com.example.model.MusicCategory
import com.example.model.ReviewItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.ProjectEntity
import com.example.data.local.ProjectMappers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MotionCutTest {

    @Test
    fun testKeyframeInterpolation() {
        val track = KeyframeTrack(
            property = AnimatableProperty.SCALE,
            keyframes = listOf(
                Keyframe(timeMs = 0L, value = 1.0f, easing = EasingType.Linear),
                Keyframe(timeMs = 1000L, value = 2.0f, easing = EasingType.Linear)
            )
        )

        // At midpoint 500ms -> should be 1.5f
        val midVal = KeyframeEvaluator.interpolate(track, 500L, 1.0f)
        assertEquals(1.5f, midVal, 0.01f)

        // Before start -> clamp to first
        val beforeVal = KeyframeEvaluator.interpolate(track, -100L, 1.0f)
        assertEquals(1.0f, beforeVal, 0.01f)

        // After end -> clamp to last
        val afterVal = KeyframeEvaluator.interpolate(track, 1500L, 1.0f)
        assertEquals(2.0f, afterVal, 0.01f)
    }

    @Test
    fun testCubicBezierGraphEvaluation() {
        // Standard ease in-out
        val yMid = KeyframeEvaluator.solveCubicBezier(0.5f, 0.42f, 0.0f, 0.58f, 1.0f)
        assertEquals(0.5f, yMid, 0.05f)

        // Endpoint boundary checks
        val yZero = KeyframeEvaluator.solveCubicBezier(0f, 0.25f, 0.1f, 0.25f, 1f)
        assertEquals(0f, yZero, 0.01f)

        val yOne = KeyframeEvaluator.solveCubicBezier(1f, 0.25f, 0.1f, 0.25f, 1f)
        assertEquals(1f, yOne, 0.01f)
    }

    @Test
    fun testColorGradingMatrixAndScopes() {
        val settings = ColorGradingSettings(
            lutPreset = LutPreset.CYBER_NEON,
            exposure = 0.5f,
            contrast = 1.2f,
            temperature = -20f
        )

        val matrix = ColorGradingCompositor.createColorMatrix(settings)
        assertNotNull(matrix)

        val scopes = ColorGradingCompositor.computeScopeSamples(settings, 64)
        assertEquals(64, scopes.lumaWaveform.size)
        assertEquals(64, scopes.rParade.size)
        assertEquals(64, scopes.gParade.size)
        assertEquals(64, scopes.bParade.size)

        // Values should be normalized between 0 and 1
        assertTrue(scopes.lumaWaveform.all { it in 0.0f..1.0f })
    }

    @Test
    fun testMaskEvaluationAndAnimation() {
        val animatedMask = MaskConfig(
            id = "mask_test",
            shapeType = MaskShapeType.RECTANGLE,
            featherPx = 10f,
            isAnimated = true,
            keyframes = listOf(
                MaskKeyframe(timeMs = 0L, positionX = 0f, positionY = 0f, scale = 1.0f, rotation = 0f, featherPx = 0f),
                MaskKeyframe(timeMs = 1000L, positionX = 100f, positionY = 50f, scale = 2.0f, rotation = 90f, featherPx = 20f)
            )
        )

        val evalMid = MaskCompositor.evaluateMask(animatedMask, currentTimeMs = 500L, clipStartTimeMs = 0L, clipWidth = 400f, clipHeight = 800f)
        assertEquals(50f, evalMid.positionX, 0.1f)
        assertEquals(25f, evalMid.positionY, 0.1f)
        assertEquals(1.5f, evalMid.scaleX, 0.01f)
        assertEquals(45f, evalMid.rotation, 0.1f)
        assertEquals(10f, evalMid.featherPx, 0.1f)
        assertNotNull(evalMid.path)
        assertFalse(evalMid.isInverted)
    }

    @Test
    fun testMarketplaceReviewSystem() {
        val repo = ProjectRepository()
        val items = repo.marketplaceItems.value
        assertTrue(items.isNotEmpty())

        val firstItem = items.first()
        val initialReviewCount = firstItem.reviews.size

        val newReview = ReviewItem(
            userName = "Pro Director",
            userHandle = "@director_x",
            rating = 5,
            comment = "Astonishing preset quality and smooth transitions!"
        )
        repo.addReviewToMarketplaceItem(firstItem.id, newReview)

        val updated = repo.marketplaceItems.value.find { it.id == firstItem.id }
        assertNotNull(updated)
        assertEquals(initialReviewCount + 1, updated!!.reviews.size)
        assertEquals("Pro Director", updated.reviews.first().userName)
    }

    @Test
    fun testMusicLibraryCategoriesAndImport() {
        val tracks = SampleMusicLibrary.getAllTracks()
        assertTrue(tracks.any { it.category == MusicCategory.UPBEAT })
        assertTrue(tracks.any { it.category == MusicCategory.CINEMATIC })
        assertTrue(tracks.any { it.category == MusicCategory.AMBIENT })
        assertTrue(tracks.any { it.category == MusicCategory.ELECTRONIC })

        val upbeatTrack = tracks.first { it.category == MusicCategory.UPBEAT }
        assertTrue(upbeatTrack.bpm > 100)
        assertTrue(upbeatTrack.waveformSamples.isNotEmpty())

        // Test project audio track import
        val repo = ProjectRepository(seedSampleProjects = true)
        repo.importMusicTrackToProject(upbeatTrack, 1000L)

        val proj = repo.currentProject.value
        val hasImportedTrack = proj.tracks.any { track ->
            track.clips.any { it.title.contains(upbeatTrack.title) }
        }
        assertTrue(hasImportedTrack)
    }

    @Test
    fun testAiVideoCaptionsImportToProject() {
        val repo = ProjectRepository(seedSampleProjects = true)
        val sampleCaptions = listOf(
            com.example.engine.AiCaptionItem(
                text = "VIRAL CUT",
                startOffsetMs = 0L,
                durationMs = 1500L,
                colorHex = 0xFF00E5FF,
                isEmphasis = true
            ),
            com.example.engine.AiCaptionItem(
                text = "NEXT LEVEL SPEED",
                startOffsetMs = 1800L,
                durationMs = 1600L,
                colorHex = 0xFFFFFFFF,
                isEmphasis = false
            )
        )

        repo.addCaptionsToProject(sampleCaptions, baseTimeMs = 500L)

        val proj = repo.currentProject.value
        val textTrack = proj.tracks.find { it.type == com.example.model.TrackType.TEXT_STICKER }
        assertNotNull(textTrack)
        assertTrue(textTrack!!.clips.any { it.title == "VIRAL CUT" && it.textConfig?.isBold == true })
        assertTrue(textTrack.clips.any { it.title == "NEXT LEVEL SPEED" })
    }

    @Test
    fun testAiCutoutMaskStructure() {
        val mask = MaskConfig(
            name = "AI Cutout (Dancer)",
            shapeType = MaskShapeType.FREEFORM_POLYGON,
            featherPx = 14f,
            vertices = listOf(
                com.example.model.MaskVertex(x = -100f, y = -120f),
                com.example.model.MaskVertex(x = 100f, y = -120f),
                com.example.model.MaskVertex(x = 120f, y = 100f),
                com.example.model.MaskVertex(x = 0f, y = 160f),
                com.example.model.MaskVertex(x = -120f, y = 100f)
            )
        )
        val cutoutResult = com.example.engine.AiCutoutResult(
            mask = mask,
            subjectLabel = "Dancer",
            auraColorHex = 0xFF00E5FF,
            confidence = 0.96f
        )

        assertEquals("Dancer", cutoutResult.subjectLabel)
        assertEquals(5, cutoutResult.mask.vertices.size)
        assertEquals(14f, cutoutResult.mask.featherPx, 0.01f)
        assertTrue(cutoutResult.confidence > 0.9f)
    }

    @Test
    fun testActionStackUndoAndRedo() {
        val stack = com.example.data.ActionStack(maxStackSize = 10)
        assertFalse(stack.canUndo.value)
        assertFalse(stack.canRedo.value)

        val projectA = SampleProjects.createCyberpunkProject()
        val projectB = projectA.copy(title = "Edited Title")
        val projectC = projectB.copy(title = "Final Title")

        // 1. Perform edit 1: A -> B
        stack.pushAction("Edit Title 1", projectA)
        assertTrue(stack.canUndo.value)
        assertFalse(stack.canRedo.value)
        assertEquals("Edit Title 1", stack.lastUndoDescription.value)

        // 2. Perform edit 2: B -> C
        stack.pushAction("Edit Title 2", projectB)
        assertEquals(2, stack.undoCount)
        assertEquals(0, stack.redoCount)

        // 3. Undo edit 2 -> restores projectB, pushes current projectC to redo
        val undoResult1 = stack.undo(projectC)
        assertNotNull(undoResult1)
        assertEquals("Edited Title", undoResult1!!.first.title)
        assertEquals("Edit Title 2", undoResult1.second)
        assertTrue(stack.canUndo.value)
        assertTrue(stack.canRedo.value)
        assertEquals("Edit Title 2", stack.lastRedoDescription.value)

        // 4. Undo edit 1 -> restores projectA
        val undoResult2 = stack.undo(undoResult1.first)
        assertNotNull(undoResult2)
        assertEquals(projectA.title, undoResult2!!.first.title)
        assertFalse(stack.canUndo.value)
        assertTrue(stack.canRedo.value)

        // 5. Redo edit 1 -> restores projectB
        val redoResult1 = stack.redo(undoResult2.first)
        assertNotNull(redoResult1)
        assertEquals("Edited Title", redoResult1!!.first.title)
        assertTrue(stack.canUndo.value)

        // 6. Redo edit 2 -> restores projectC
        val redoResult2 = stack.redo(redoResult1.first)
        assertNotNull(redoResult2)
        assertEquals("Final Title", redoResult2!!.first.title)
        assertFalse(stack.canRedo.value)
    }

    @Test
    fun testRepositoryGlobalUndoRedoOperations() {
        val repo = ProjectRepository(seedSampleProjects = true)
        assertFalse(repo.canUndo.value)

        val initialClipsCount = repo.currentProject.value.tracks.first().clips.size
        val targetClip = repo.currentProject.value.tracks.first().clips.first()

        // 1. Duplicate clip
        repo.duplicateClip(targetClip.id)
        val afterDupClipsCount = repo.currentProject.value.tracks.first().clips.size
        assertEquals(initialClipsCount + 1, afterDupClipsCount)
        assertTrue(repo.canUndo.value)

        // 2. Undo duplication
        val undoneDesc = repo.undo()
        assertEquals("Duplicate Clip", undoneDesc)
        assertEquals(initialClipsCount, repo.currentProject.value.tracks.first().clips.size)
        assertTrue(repo.canRedo.value)

        // 3. Redo duplication
        val redoneDesc = repo.redo()
        assertEquals("Duplicate Clip", redoneDesc)
        assertEquals(initialClipsCount + 1, repo.currentProject.value.tracks.first().clips.size)

        // 4. Delete clip
        repo.deleteClip(targetClip.id)
        assertFalse(repo.currentProject.value.tracks.first().clips.any { it.id == targetClip.id })

        // 5. Undo deletion
        val undoDeleteDesc = repo.undo()
        assertNotNull(undoDeleteDesc)
        assertTrue(repo.currentProject.value.tracks.first().clips.any { it.id == targetClip.id })
    }

    @Test
    fun testVoiceOverRecordingImport() {
        val repo = ProjectRepository(seedSampleProjects = true)
        val recordedVoiceOver = com.example.engine.RecordedVoiceOver(
            title = "Voiceover 1",
            durationMs = 4200L,
            waveformPoints = listOf(0.1f, 0.45f, 0.8f, 0.6f, 0.2f),
            fileUri = "file:///data/user/0/com.aistudio.motioncut.vfxpro/files/voiceovers/voiceover_test.wav"
        )

        repo.importVoiceOverToProject(recordedVoiceOver, playheadTimeMs = 2000L)

        val proj = repo.currentProject.value
        val audioTrack = proj.tracks.find { it.type == com.example.model.TrackType.AUDIO }
        assertNotNull(audioTrack)

        val voClip = audioTrack!!.clips.find { it.title.contains("Voiceover 1") }
        assertNotNull(voClip)
        assertEquals(2000L, voClip!!.startTimeMs)
        assertEquals(4200L, voClip.durationMs)
        assertEquals(recordedVoiceOver.fileUri, voClip.mediaUri)
        assertEquals(5, voClip.waveformSamples.size)
        assertTrue(proj.durationMs >= 6200L)
        assertTrue(repo.canUndo.value)

        // Test undoing voice-over insertion
        val undoDesc = repo.undo()
        assertEquals("Record Voiceover 1", undoDesc)
        val audioTrackAfterUndo = repo.currentProject.value.tracks.find { it.type == com.example.model.TrackType.AUDIO }
        assertFalse(audioTrackAfterUndo?.clips?.any { it.title.contains("Voiceover 1") } == true)
    }

    @Test
    fun testRoomDatabaseDaoOperations() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.createInMemory(context)
        val dao = db.projectDao()

        assertEquals(0, dao.getProjectCount())

        // 1. Insert initial sample projects
        val sampleEntities = ProjectMappers.getInitialEntities()
        assertTrue(sampleEntities.isNotEmpty())
        dao.insertProjects(sampleEntities)

        assertEquals(sampleEntities.size, dao.getProjectCount())
        val fetchedList = dao.getAllProjects().first()
        assertEquals(sampleEntities.size, fetchedList.size)

        val firstProject = fetchedList.first()
        assertNotNull(firstProject.thumbnailResId)
        assertTrue(firstProject.durationMs > 0)
        assertTrue(firstProject.updatedAt > 0)

        // 2. Rename existing project in Room
        val renamedTitle = "Studio Timeline Cut"
        val newTimestamp = System.currentTimeMillis() + 1000L
        dao.renameProject(firstProject.id, renamedTitle, newTimestamp)

        val updatedProject = dao.getProjectById(firstProject.id)
        assertNotNull(updatedProject)
        assertEquals(renamedTitle, updatedProject!!.title)
        assertEquals(newTimestamp, updatedProject.updatedAt)

        // 3. Delete project from Room
        dao.deleteProjectById(firstProject.id)
        val afterDeleteCount = dao.getProjectCount()
        assertEquals(sampleEntities.size - 1, afterDeleteCount)
        val checkDeleted = dao.getProjectById(firstProject.id)
        assertTrue(checkDeleted == null)

        db.close()
    }

    @Test
    fun testProjectTimestampAndDurationFormatting() {
        val now = System.currentTimeMillis()

        val justNow = ProjectMappers.formatProjectTimestamp(now - 15_000L)
        assertEquals("Just now", justNow)

        val fiveMins = ProjectMappers.formatProjectTimestamp(now - 5 * 60_000L)
        assertEquals("5m ago", fiveMins)

        val threeHours = ProjectMappers.formatProjectTimestamp(now - 3 * 3600_000L)
        assertEquals("3h ago", threeHours)

        // Duration formatting
        val dur10s = ProjectMappers.formatDuration(10_000L)
        assertEquals("00:10", dur10s)

        val dur85s = ProjectMappers.formatDuration(85_000L)
        assertEquals("01:25", dur85s)
    }

    @Test
    fun testRepositoryWithRoomDaoIntegration() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.createInMemory(context)
        val dao = db.projectDao()
        val repo = ProjectRepository(dao)

        // Create a user-authored project (not auto-seeded sample IDs)
        val userProject = ProjectMappers.toEntity(
            com.example.model.Project(
                id = "user_proj_1",
                title = "My Real Edit",
                aspectRatio = com.example.model.AspectRatio.PORTRAIT_9_16
            )
        )
        dao.insertProjects(listOf(userProject))

        val projectsFlow = repo.savedProjectEntities.first()
        assertTrue(projectsFlow.isNotEmpty())

        val targetProj = projectsFlow.first()
        repo.renameProject(targetProj.id, "Renamed in Repo")

        // Allow async IO persistence to commit
        kotlinx.coroutines.delay(300L)

        val renamedInDao = dao.getProjectById(targetProj.id)
        assertEquals("Renamed in Repo", renamedInDao?.title)

        repo.deleteProject(targetProj.id)
        kotlinx.coroutines.delay(300L)

        val deletedFromDao = dao.getProjectById(targetProj.id)
        assertTrue(deletedFromDao == null)

        db.close()
    }

    @Test
    fun testEditorViewModelCreationViaFactory() {
        val factory = androidx.lifecycle.viewmodel.viewModelFactory {
            addInitializer(com.example.ui.EditorViewModel::class) {
                com.example.ui.EditorViewModel(repository = ProjectRepository())
            }
        }
        val vm = androidx.lifecycle.ViewModelProvider.create(
            store = androidx.lifecycle.ViewModelStore(),
            factory = factory
        )[com.example.ui.EditorViewModel::class.java]
        assertNotNull(vm)
    }

    @Test
    fun testBottomDockOperationsAndUndoRedo() {
        val repo = ProjectRepository(seedSampleProjects = true)
        val targetClip = repo.currentProject.value.tracks.first().clips.first()
        val origCount = repo.currentProject.value.tracks.first().clips.size

        // 1. Split into 3 equal parts + undo
        repo.splitClipIntoParts(targetClip.id, 3)
        val afterSplit = repo.currentProject.value.tracks.first().clips
        assertEquals(origCount + 2, afterSplit.size)
        assertTrue(afterSplit.any { it.title.contains("(1/3)") })
        assertTrue(afterSplit.any { it.title.contains("(3/3)") })
        assertEquals("Split into 3 Parts", repo.undo())
        assertEquals(origCount, repo.currentProject.value.tracks.first().clips.size)

        // 2. Link toggle
        assertFalse(repo.currentProject.value.findClip(targetClip.id)!!.isLocked)
        val linked = repo.toggleClipLink(targetClip.id)
        assertEquals(true, linked)
        assertTrue(repo.currentProject.value.findClip(targetClip.id)!!.isLocked)

        // 3. Layout aspect ratio + frame preset
        repo.updateProjectLayout(
            aspectRatio = com.example.model.AspectRatio.LANDSCAPE_16_9,
            clipId = targetClip.id,
            layoutPresetLabel = "Split Left",
            scale = 0.65f,
            offsetX = -140f,
            offsetY = 0f
        )
        assertEquals(com.example.model.AspectRatio.LANDSCAPE_16_9, repo.currentProject.value.aspectRatio)
        assertEquals(0.65f, repo.currentProject.value.findClip(targetClip.id)!!.baseTransform.scaleX, 0.01f)

        // 4. Upscale resolution to 4K
        repo.upscaleProjectResolution(
            targetResolution = com.example.model.ExportResolution.UHD_4K,
            clipId = targetClip.id,
            contrastBoost = 1.12f,
            saturationBoost = 1.08f
        )
        assertEquals(com.example.model.ExportResolution.UHD_4K, repo.currentProject.value.resolution)

        // 5. Cloud depth pass
        repo.applyDepthCloudPass(
            clipId = targetClip.id,
            depthMask = MaskConfig(name = "Cloud Depth Focus", shapeType = MaskShapeType.ELLIPSE, featherPx = 26f),
            depthEffect = com.example.model.VideoEffect(type = com.example.model.EffectType.GLOW, intensity = 0.42f, speed = 0.8f),
            parallaxScale = 1.06f
        )
        val depthClip = repo.currentProject.value.findClip(targetClip.id)!!
        assertTrue(depthClip.masks.any { it.name == "Cloud Depth Focus" })
        assertTrue(depthClip.effects.any { it.type == com.example.model.EffectType.GLOW })
    }

    @Test
    fun testAddLayerAutoTrackRoutingAndNewShapes() {
        val repo = ProjectRepository()
        val singleTrackProj = com.example.model.Project(
            title = "Single Track Project",
            aspectRatio = com.example.model.AspectRatio.PORTRAIT_9_16,
            tracks = listOf(
                com.example.model.Track(
                    id = "main_1",
                    name = "Video 1",
                    type = com.example.model.TrackType.MAIN_VIDEO
                )
            )
        )
        repo.createNewProject(singleTrackProj)

        // Add clips via blank trackId ("") matching AddLayerDialog behavior
        val audioClip = com.example.model.ClipItem(
            trackId = "",
            title = "Device Track.mp3",
            type = com.example.model.ClipType.AUDIO,
            mediaUri = "content://media/external/audio/media/42",
            startTimeMs = 500L,
            durationMs = 4000L,
            waveformSamples = listOf(0.2f, 0.7f, 0.5f)
        )
        val moonShapeClip = com.example.model.ClipItem(
            trackId = "",
            title = "Moon",
            type = com.example.model.ClipType.SHAPE,
            startTimeMs = 1000L,
            durationMs = 3000L,
            shapeConfig = com.example.model.ShapeConfig(type = com.example.model.ShapeType.MOON)
        )
        val adjustmentClip = com.example.model.ClipItem(
            trackId = "",
            title = "Adjustment Layer",
            type = com.example.model.ClipType.ADJUSTMENT,
            startTimeMs = 0L,
            durationMs = 5000L
        )
        val nullClip = com.example.model.ClipItem(
            trackId = "",
            title = "Null Object",
            type = com.example.model.ClipType.NULL_OBJECT,
            startTimeMs = 0L,
            durationMs = 5000L
        )

        repo.addClipToTrack("", audioClip)
        repo.addClipToTrack("", moonShapeClip)
        repo.addClipToTrack("", adjustmentClip)
        repo.addClipToTrack("", nullClip)

        val updated = repo.currentProject.value
        val audioTrack = updated.tracks.find { it.type == com.example.model.TrackType.AUDIO }
        val shapeTrack = updated.tracks.find { it.type == com.example.model.TrackType.TEXT_STICKER }
        val fxTrack = updated.tracks.find { it.type == com.example.model.TrackType.EFFECTS }

        assertNotNull(audioTrack)
        assertTrue(audioTrack!!.clips.any { it.title == "Device Track.mp3" && it.mediaUri == "content://media/external/audio/media/42" })
        assertNotNull(shapeTrack)
        assertTrue(shapeTrack!!.clips.any { it.shapeConfig?.type == com.example.model.ShapeType.MOON })
        assertNotNull(fxTrack)
        assertTrue(fxTrack!!.clips.any { it.type == com.example.model.ClipType.ADJUSTMENT })
        assertTrue(fxTrack.clips.any { it.type == com.example.model.ClipType.NULL_OBJECT })
        assertTrue(com.example.model.ShapeType.entries.size >= 22)
    }

    @Test
    fun testDeviceMediaScannerFallbackAndPersistence() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val mediaItems = com.example.data.DeviceMediaScanner.loadDeviceAndStudioMedia(context)
        val audioItems = com.example.data.DeviceMediaScanner.loadDeviceAndStudioAudio(context)

        // Studio sample video fixtures are completely removed; only real device/imported items appear
        assertTrue(mediaItems.none { it.folderName.equals("Studio", ignoreCase = true) })
        assertTrue(mediaItems.none { it.title.contains("Studio", ignoreCase = true) && !it.isDeviceFile })

        // Verify transparent PNG bitmap decoding preserves ARGB_8888 alpha & detects .png as image
        val opts = com.example.data.DeviceMediaScanner.transparentBitmapOptions(1)
        assertEquals(android.graphics.Bitmap.Config.ARGB_8888, opts.inPreferredConfig)
        assertTrue(opts.inPremultiplied)
        assertFalse(com.example.data.DeviceMediaScanner.isVideoUri(context, "file:///tmp/logo_cutout.png", isVideoHint = true))
        assertTrue(com.example.data.DeviceMediaScanner.isVideoUri(context, "file:///tmp/scene.mp4", isVideoHint = false))

        assertTrue(audioItems.all { it.waveformSamples.isNotEmpty() })
    }

    @Test
    fun testGuestModeViewModelWithoutSignIn() {
        val vm = com.example.ui.EditorViewModel(
            repository = ProjectRepository(),
            firestoreRepository = null,
            currentUserId = null
        )
        assertEquals(com.example.ui.CloudSyncState.LocalOnly, vm.cloudSyncState.value)

        vm.createProject("Offline Local Edit", com.example.model.AspectRatio.PORTRAIT_9_16)
        assertEquals("Offline Local Edit", vm.currentProject.value.title)

        val videoClip = com.example.model.ClipItem(
            trackId = "",
            title = "CameraClip.mp4",
            type = com.example.model.ClipType.VIDEO_SAMPLE,
            mediaUri = "content://media/external/video/media/99",
            startTimeMs = 0L,
            durationMs = 4000L
        )
        vm.addClip(videoClip)
        val mainTrack = vm.currentProject.value.tracks.find { it.type == com.example.model.TrackType.MAIN_VIDEO }
        assertNotNull(mainTrack)
        assertTrue(mainTrack!!.clips.any { it.title == "CameraClip.mp4" })
    }

    @Test
    fun testKeyframeSliderAutoKeyframeAndInterpolation() {
        val repo = ProjectRepository()
        val vm = com.example.ui.EditorViewModel(repository = repo)

        // Create a fresh clip with no keyframes yet
        val clip = com.example.model.ClipItem(
            id = "kf_test_clip",
            trackId = "",
            title = "Hero Clip",
            startTimeMs = 0L,
            durationMs = 4000L
        )
        vm.addClip(clip)
        assertEquals("kf_test_clip", vm.selectedClipId.value)

        // 1. Adjusting unkeyframed clip sets absolute value (not delta!)
        vm.setActiveProperty(AnimatableProperty.OPACITY)
        vm.updatePropertyValue(AnimatableProperty.OPACITY, 0.5f)
        val afterOpacity = vm.currentProject.value.findClip("kf_test_clip")!!
        assertEquals(0.5f, afterOpacity.baseTransform.opacity, 0.01f)

        // 2. Toggle first Scale keyframe at 0ms (scale = 1.0f)
        vm.setActiveProperty(AnimatableProperty.SCALE)
        vm.seek(0L)
        vm.toggleKeyframeOnSelectedClip()
        assertTrue(vm.currentProject.value.findClip("kf_test_clip")!!.hasKeyframeAt(AnimatableProperty.SCALE, 0L))

        // 3. Seek to 2000ms and drag slider to 2.0f -> should auto-insert keyframe at 2000ms with value 2.0f
        vm.seek(2000L)
        vm.updatePropertyValue(AnimatableProperty.SCALE, 2.0f)
        val afterSecondKf = vm.currentProject.value.findClip("kf_test_clip")!!
        val scaleKfs = afterSecondKf.keyframeTracks[AnimatableProperty.SCALE]?.keyframes.orEmpty()
        assertEquals(2, scaleKfs.size)
        assertEquals(1.0f, scaleKfs[0].value, 0.01f)
        assertEquals(2.0f, scaleKfs[1].value, 0.01f)

        // 4. Evaluate midpoint (1000ms) -> should interpolate strictly between 1.0 and 2.0 (~1.5)
        val midTransform = KeyframeEvaluator.evaluateClipTransform(afterSecondKf, 1000L)
        assertTrue(midTransform.scaleX > 1.3f && midTransform.scaleX < 1.7f)

        // 5. Jump to previous / next keyframe
        vm.seek(1000L)
        vm.jumpToAdjacentKeyframe(next = true)
        assertEquals(2000L, vm.playbackEngine.currentTimeMs.value)
        vm.jumpToAdjacentKeyframe(next = false)
        assertEquals(0L, vm.playbackEngine.currentTimeMs.value)

        // 6. Toggling off both keyframes cleans up track
        vm.toggleKeyframeOnSelectedClip()
        vm.seek(2000L)
        vm.toggleKeyframeOnSelectedClip()
        val afterClear = vm.currentProject.value.findClip("kf_test_clip")!!
        assertFalse(afterClear.keyframeTracks.containsKey(AnimatableProperty.SCALE))
    }

    @Test
    fun testCanvasTransformDeltasUpdateKeyframes() {
        val repo = ProjectRepository()
        val vm = com.example.ui.EditorViewModel(repository = repo)

        val clip = com.example.model.ClipItem(
            id = "canvas_kf_clip",
            trackId = "",
            title = "Motion Shape",
            type = com.example.model.ClipType.SHAPE,
            startTimeMs = 0L,
            durationMs = 4000L,
            shapeConfig = com.example.model.ShapeConfig(type = com.example.model.ShapeType.STAR)
        )
        vm.addClip(clip)

        // Add initial Position X keyframe at 0ms
        vm.setActiveProperty(AnimatableProperty.POSITION_X)
        vm.seek(0L)
        vm.toggleKeyframeOnSelectedClip()

        // Move playhead to 1500ms and pan +50px twice -> should accumulate to +100px at 1500ms keyframe
        vm.seek(1500L)
        vm.updateClipTransform("canvas_kf_clip", panX = 50f, panY = 0f, zoom = 1.0f, rot = 0f)
        vm.updateClipTransform("canvas_kf_clip", panX = 50f, panY = 0f, zoom = 1.0f, rot = 0f)

        val updatedClip = vm.currentProject.value.findClip("canvas_kf_clip")!!
        val posXKfs = updatedClip.keyframeTracks[AnimatableProperty.POSITION_X]?.keyframes.orEmpty()
        assertEquals(2, posXKfs.size)
        assertEquals(0f, posXKfs[0].value, 0.1f)
        assertEquals(100f, posXKfs[1].value, 0.1f)

        val midPos = KeyframeEvaluator.evaluateClipTransform(updatedClip, 750L)
        assertEquals(50f, midPos.positionX, 2f)
    }

    @Test
    fun testDeviceMediaScannerDirectoryDiscovery() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val photosDir = java.io.File(context.filesDir, "photos").apply { mkdirs() }
        val videosDir = java.io.File(context.filesDir, "videos").apply { mkdirs() }
        val audiosDir = java.io.File(context.filesDir, "audios").apply { mkdirs() }

        val testPhoto = java.io.File(photosDir, "sunset_snap.jpg").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
        val testVideo = java.io.File(videosDir, "clip_take1.mp4").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
        val testAudio = java.io.File(audiosDir, "voice_beat.mp3").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }

        try {
            val mediaItems = com.example.data.DeviceMediaScanner.loadDeviceAndStudioMedia(context)
            val audioItems = com.example.data.DeviceMediaScanner.loadDeviceAndStudioAudio(context)

            val foundPhoto = mediaItems.find { it.title == "sunset_snap.jpg" }
            val foundVideo = mediaItems.find { it.title == "clip_take1.mp4" }
            val foundAudio = audioItems.find { it.title == "voice_beat" }

            assertNotNull(foundPhoto)
            assertFalse(foundPhoto!!.isVideo)
            assertEquals("photos", foundPhoto.folderName)

            assertNotNull(foundVideo)
            assertTrue(foundVideo!!.isVideo)
            assertEquals("videos", foundVideo.folderName)

            assertNotNull(foundAudio)
            assertEquals("audios", foundAudio!!.folderName)
        } finally {
            testPhoto.delete()
            testVideo.delete()
            testAudio.delete()
        }
    }

    @Test
    fun testMultiSuiteCuratedEffectCatalogAndApplication() {
        val all = com.example.data.EffectCatalog.allPresets
        assertTrue("Expected curated effects >= 18, got ${all.size}", all.size >= 18)
        assertEquals("Curated preset names must be distinct", all.size, all.map { it.name }.distinct().size)
        assertEquals("Curated preset IDs must be distinct", all.size, all.map { it.id }.distinct().size)

        val rgbMatches = com.example.data.EffectCatalog.filterPresets(
            category = "Filter",
            query = "Chromatic"
        )
        assertTrue(rgbMatches.isNotEmpty())

        val vm = com.example.ui.EditorViewModel(repository = ProjectRepository(seedSampleProjects = true))
        val firstClip = vm.currentProject.value.tracks.first().clips.first()
        vm.selectClip(firstClip.id)
        val initialFxCount = firstClip.effects.size

        vm.addEffectPreset(rgbMatches.first())
        val updatedClip = vm.currentProject.value.findClip(firstClip.id)!!
        assertEquals(initialFxCount + 1, updatedClip.effects.size)
        assertEquals(rgbMatches.first().name, updatedClip.effects.last().effectiveName)
    }

    @Test
    fun testMesh3DPrimitivesObjParsingAndKeyframeEvaluation() {
        // 1. Built-in primitives & extruded 3D text
        val cube = com.example.engine.Mesh3DParser.buildPrimitiveMesh(com.example.model.Primitive3DShape.CUBE)
        assertEquals(8, cube.vertices.size)
        assertEquals(12, cube.faces.size)

        val textMesh = com.example.engine.Mesh3DParser.buildExtrudedTextMesh("NOVA", 0.6f)
        assertTrue(textMesh.vertices.isNotEmpty())
        assertTrue(textMesh.faces.isNotEmpty())

        // 2. Wavefront .obj parser (with quad triangulation & vertex normalization)
        val objSource = """
            # Simple quad prism
            v 0 0 0
            v 2 0 0
            v 2 2 0
            v 0 2 0
            v 0 0 2
            f 1 2 3 4
            f 1 2 5
        """.trimIndent()
        val parsedObj = com.example.engine.Mesh3DParser.parseObj(objSource, "custom_quad.obj")
        assertEquals(5, parsedObj.vertices.size)
        assertEquals(3, parsedObj.faces.size) // quad -> 2 tris + 1 tri

        // 3. 3D Model Clip Keyframe Evaluation (Yaw Y 0 -> 180 deg linearly)
        val repo = ProjectRepository()
        val vm = com.example.ui.EditorViewModel(repository = repo)
        val clip3d = com.example.model.ClipItem(
            id = "mesh_3d_kf",
            trackId = "",
            title = "3D Cube",
            type = com.example.model.ClipType.MODEL_3D,
            startTimeMs = 0L,
            durationMs = 2000L,
            model3dConfig = com.example.model.Model3DConfig(
                source = com.example.model.Model3DSource.PRIMITIVE,
                primitiveShape = com.example.model.Primitive3DShape.CUBE,
                rotationY = 0f
            )
        )
        vm.addClip(clip3d)
        assertEquals(com.example.ui.EditorToolTab.MODEL_3D, vm.activeToolTab.value)

        vm.seek(0L)
        vm.toggleKeyframeForProperty(AnimatableProperty.ROTATION_Y_3D)
        vm.seek(2000L)
        vm.updatePropertyValue(AnimatableProperty.ROTATION_Y_3D, 180f)

        val updated3dClip = vm.currentProject.value.findClip("mesh_3d_kf")!!
        val mid3d = KeyframeEvaluator.evaluateModel3D(updated3dClip, 1000L)!!
        assertEquals(90f, mid3d.rotationY, 3f)
    }

    @Test
    fun testNoAutoSeededRandomProjectsAndPurge() = runBlocking {
        // 1. Default ProjectRepository has zero auto-seeded random projects
        val inMemoryRepo = ProjectRepository()
        assertTrue(inMemoryRepo.projects.value.isEmpty())
        assertTrue(inMemoryRepo.savedProjectEntities.first().isEmpty())

        // 2. Room-backed ProjectRepository purges old SEEDED_SAMPLE_IDS while keeping user projects
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.createInMemory(context)
        val dao = db.projectDao()
        val userProject = ProjectMappers.toEntity(
            com.example.model.Project(
                id = "custom_user_project",
                title = "My Personal Vlog",
                aspectRatio = com.example.model.AspectRatio.LANDSCAPE_16_9
            )
        )
        dao.insertProjects(ProjectMappers.getInitialEntities() + userProject)

        val repo = ProjectRepository(dao)
        kotlinx.coroutines.delay(300L)

        val remaining = dao.getAllProjects().first()
        assertEquals(1, remaining.size)
        assertEquals("custom_user_project", remaining.first().id)
        assertTrue(repo.projects.value.none { it.id in SampleProjects.SEEDED_SAMPLE_IDS })

        // Deleting the last user project leaves 0 projects
        repo.deleteProject("custom_user_project")
        kotlinx.coroutines.delay(250L)
        assertTrue(repo.projects.value.isEmpty())

        db.close()
    }

    @Test
    fun testVideoExporterDimensionsAndFileNameSanitization() {
        val portrait1080 = com.example.engine.VideoExporter.computeExportDimensions(
            com.example.model.AspectRatio.PORTRAIT_9_16,
            com.example.model.ExportResolution.FHD_1080P
        )
        assertEquals(1080 to 1920, portrait1080)

        val landscape720 = com.example.engine.VideoExporter.computeExportDimensions(
            com.example.model.AspectRatio.LANDSCAPE_16_9,
            com.example.model.ExportResolution.HD_720P
        )
        assertEquals(1280 to 720, landscape720)

        val square4k = com.example.engine.VideoExporter.computeExportDimensions(
            com.example.model.AspectRatio.SQUARE_1_1,
            com.example.model.ExportResolution.UHD_4K
        )
        assertEquals(2160 to 2160, square4k)

        val cinematic = com.example.engine.VideoExporter.computeExportDimensions(
            com.example.model.AspectRatio.CINEMATIC_21_9,
            com.example.model.ExportResolution.FHD_1080P
        )
        assertEquals(0, cinematic.first % 2)
        assertEquals(0, cinematic.second % 2)

        assertEquals("My_Summer_Vlog_2026", com.example.engine.VideoExporter.sanitizeFileName("  My Summer Vlog! (2026) "))
        assertEquals("MotionCut_Export", com.example.engine.VideoExporter.sanitizeFileName("   ///   "))
    }

    @Test
    fun testKeyframeDeletionByIdAndAtPlayhead() {
        val repo = ProjectRepository()
        val vm = com.example.ui.EditorViewModel(repository = repo)
        val clip = com.example.model.ClipItem(
            id = "kf_del_clip",
            trackId = "",
            title = "Logo PNG",
            type = com.example.model.ClipType.VIDEO_SAMPLE,
            startTimeMs = 0L,
            durationMs = 4000L
        )
        vm.addClip(clip)
        vm.setActiveProperty(AnimatableProperty.POSITION_X)

        vm.seek(0L)
        vm.toggleKeyframeOnSelectedClip()
        vm.seek(1000L)
        vm.updatePropertyValue(AnimatableProperty.POSITION_X, 120f)
        vm.seek(2500L)
        vm.updatePropertyValue(AnimatableProperty.POSITION_X, 240f)

        val beforeDel = vm.currentProject.value.findClip("kf_del_clip")!!
        val kfs = beforeDel.keyframeTracks[AnimatableProperty.POSITION_X]?.keyframes.orEmpty()
        assertEquals(3, kfs.size)

        // Delete middle keyframe explicitly by ID (bugfix verification)
        vm.deleteKeyframe(AnimatableProperty.POSITION_X, kfs[1].id)
        val afterMidDel = vm.currentProject.value.findClip("kf_del_clip")!!
        assertEquals(2, afterMidDel.keyframeTracks[AnimatableProperty.POSITION_X]?.keyframes?.size)

        // Delete remaining keyframe at time within 160ms tolerance
        vm.seek(2420L)
        vm.deleteKeyframeAtPlayhead(AnimatableProperty.POSITION_X)
        val afterPlayheadDel = vm.currentProject.value.findClip("kf_del_clip")!!
        assertEquals(1, afterPlayheadDel.keyframeTracks[AnimatableProperty.POSITION_X]?.keyframes?.size)

        // Undo restores keyframe
        vm.undo()
        val afterUndo = vm.currentProject.value.findClip("kf_del_clip")!!
        assertEquals(2, afterUndo.keyframeTracks[AnimatableProperty.POSITION_X]?.keyframes?.size)

        // Clear track removes property keyframe track completely
        vm.clearKeyframesForProperty(AnimatableProperty.POSITION_X)
        assertFalse(vm.currentProject.value.findClip("kf_del_clip")!!.keyframeTracks.containsKey(AnimatableProperty.POSITION_X))
    }

    @Test
    fun testMultiPointBreakpointEasingEvaluation() {
        val easing = EasingType.MultiPoint(
            points = listOf(
                com.example.model.CurveBreakpoint(0.25f, 0.85f),
                com.example.model.CurveBreakpoint(0.75f, 0.15f)
            )
        )
        assertEquals(0.0f, KeyframeEvaluator.evaluateEasing(0f, easing), 0.001f)
        assertEquals(1.0f, KeyframeEvaluator.evaluateEasing(1f, easing), 0.001f)
        assertEquals(0.85f, KeyframeEvaluator.evaluateEasing(0.25f, easing), 0.01f)
        assertEquals(0.15f, KeyframeEvaluator.evaluateEasing(0.75f, easing), 0.01f)
        assertEquals(0.50f, KeyframeEvaluator.evaluateEasing(0.50f, easing), 0.02f)
    }

    @Test
    fun testClipSpeedPitchAndReverseIncludingAudio() {
        val repo = ProjectRepository()
        val vm = com.example.ui.EditorViewModel(repository = repo)
        val audioClip = com.example.model.ClipItem(
            id = "audio_speed_clip",
            trackId = "",
            title = "Beat_120BPM.wav",
            type = com.example.model.ClipType.AUDIO,
            startTimeMs = 1000L,
            durationMs = 4000L,
            speed = 1.0f,
            preserveAudioPitch = true,
            isReversed = false
        )
        vm.addClip(audioClip)
        vm.selectClip("audio_speed_clip")

        vm.updateClipSpeed(
            speed = 2.0f,
            preserveAudioPitch = false,
            isReversed = true,
            adjustDuration = true
        )

        val updated = vm.currentProject.value.findClip("audio_speed_clip")!!
        assertEquals(2.0f, updated.speed, 0.01f)
        assertFalse(updated.preserveAudioPitch)
        assertTrue(updated.isReversed)
        assertEquals(2000L, updated.durationMs)

        // Source time mapping in reverse at 2x speed (local t=0 -> end=4000ms; local t=1000ms -> 2000ms)
        assertEquals(4000L, KeyframeEvaluator.mapTimelineToSourceTimeMs(updated, 1000L))
        assertEquals(2000L, KeyframeEvaluator.mapTimelineToSourceTimeMs(updated, 2000L))
    }

    @Test
    fun testTextTemplatesFontsAnimationsAndEvaluation() {
        val templates = com.example.ui.components.TextStyleRegistry.templates
        assertTrue(templates.size >= 8)
        com.example.model.TextFontChoice.entries.forEach { choice ->
            val fam = com.example.ui.components.TextStyleRegistry.composeFontFamily(choice)
            assertNotNull(fam)
        }

        val typewriterCfg = com.example.model.TextConfig(
            text = "HELLO WORLD",
            fontChoice = com.example.model.TextFontChoice.BEBAS_NEUE,
            animationType = com.example.model.TextAnimationType.TYPEWRITER,
            animationSpeed = 1.0f
        )
        val startFrame = com.example.ui.components.TextStyleRegistry.evaluateAnimatedText(typewriterCfg, 50L, 2000L)
        val endFrame = com.example.ui.components.TextStyleRegistry.evaluateAnimatedText(typewriterCfg, 1900L, 2000L)
        assertTrue(startFrame.visibleText.length < endFrame.visibleText.length)
        assertEquals("HELLO WORLD", endFrame.visibleText)
    }

    @Test
    fun testLibrary3DModelsTextureMixAndProjectJsonRoundtrip() {
        // 1. All library 3D models produce valid triangulated meshes
        com.example.model.Library3DModel.entries.forEach { model ->
            val mesh = com.example.engine.Mesh3DParser.buildLibraryMesh(model, 0.4f)
            assertTrue("${model.displayName} vertices empty", mesh.vertices.isNotEmpty())
            assertTrue("${model.displayName} faces empty", mesh.faces.isNotEmpty())
        }

        // 2. ProjectJsonCodec full lossless roundtrip for all new features
        val customClip = com.example.model.ClipItem(
            id = "clip_full_json",
            trackId = "t_main",
            title = "3D Rocket Textured",
            type = com.example.model.ClipType.MODEL_3D,
            startTimeMs = 500L,
            durationMs = 3000L,
            speed = 1.5f,
            preserveAudioPitch = false,
            isReversed = true,
            model3dConfig = com.example.model.Model3DConfig(
                source = com.example.model.Model3DSource.LIBRARY_MODEL,
                libraryModel = com.example.model.Library3DModel.ROCKET,
                imageUri = "content://media/external/images/media/77",
                textureMix = 0.72f
            ),
            textConfig = com.example.model.TextConfig(
                text = "MOTION TITLE",
                fontChoice = com.example.model.TextFontChoice.CINZEL,
                animationType = com.example.model.TextAnimationType.GLITCH_FLICKER,
                hasBackgroundPill = true
            ),
            keyframeTracks = mapOf(
                AnimatableProperty.SCALE to KeyframeTrack(
                    property = AnimatableProperty.SCALE,
                    keyframes = listOf(
                        Keyframe(
                            timeMs = 0L,
                            value = 1f,
                            easing = EasingType.MultiPoint(
                                listOf(com.example.model.CurveBreakpoint(0.4f, 0.9f))
                            )
                        ),
                        Keyframe(timeMs = 1500L, value = 2f)
                    )
                )
            ),
            cameraTrackPoints = listOf(
                com.example.model.CameraTrackPoint(timeMs = 0L, x = 0f, y = 0f, scale = 1f, confidence = 1f),
                com.example.model.CameraTrackPoint(timeMs = 1000L, x = 24f, y = -12f, scale = 1.05f, confidence = 0.92f)
            )
        )
        val project = com.example.model.Project(
            id = "proj_roundtrip",
            title = "Roundtrip Test",
            tracks = listOf(
                com.example.model.Track(
                    id = "t_main",
                    name = "Main",
                    type = com.example.model.TrackType.MAIN_VIDEO,
                    clips = listOf(customClip)
                )
            )
        )

        val json = com.example.data.local.ProjectJsonCodec.encode(project)
        val decoded = com.example.data.local.ProjectJsonCodec.decode(json)
        assertNotNull(decoded)
        val dc = decoded!!.findClip("clip_full_json")!!
        assertEquals(1.5f, dc.speed, 0.01f)
        assertFalse(dc.preserveAudioPitch)
        assertTrue(dc.isReversed)
        assertEquals(com.example.model.Model3DSource.LIBRARY_MODEL, dc.model3dConfig?.source)
        assertEquals(com.example.model.Library3DModel.ROCKET, dc.model3dConfig?.libraryModel)
        assertEquals("content://media/external/images/media/77", dc.model3dConfig?.imageUri)
        assertEquals(0.72f, dc.model3dConfig?.textureMix ?: 0f, 0.01f)
        assertEquals(com.example.model.TextFontChoice.CINZEL, dc.textConfig?.fontChoice)
        assertEquals(com.example.model.TextAnimationType.GLITCH_FLICKER, dc.textConfig?.animationType)
        assertTrue(dc.textConfig?.hasBackgroundPill == true)
        val easing = dc.keyframeTracks[AnimatableProperty.SCALE]?.keyframes?.first()?.easing
        assertTrue(easing is EasingType.MultiPoint)
        assertEquals(0.4f, (easing as EasingType.MultiPoint).points.first().x, 0.01f)
        assertEquals(2, dc.cameraTrackPoints.size)
        assertEquals(24f, dc.cameraTrackPoints[1].x, 0.01f)
    }

    @Test
    fun testOpticalCameraTrackingAndApplyKeyframes() {
        val w = 96
        val h = 96
        fun makeFrame(featureX: Int, featureY: Int): IntArray {
            val grid = IntArray(w * h) { 20 }
            for (y in (featureY - 4)..(featureY + 4)) {
                for (x in (featureX - 4)..(featureX + 4)) {
                    if (x in 0 until w && y in 0 until h) {
                        grid[y * w + x] = 240
                    }
                }
            }
            return grid
        }
        val frames = listOf(
            0L to makeFrame(48, 48),
            500L to makeFrame(52, 49),
            1000L to makeFrame(56, 50)
        )
        val points = com.example.engine.CameraTracker.computeOpticalTrackPoints(
            lumaFrames = frames,
            gridW = w,
            gridH = h,
            roiNormX = 0.5f,
            roiNormY = 0.5f
        )
        assertEquals(3, points.size)
        assertEquals(0f, points[0].x, 0.01f)
        assertTrue("Expected positive X motion displacement, got ${points[2].x}", points[2].x > 5f)

        val repo = ProjectRepository()
        val srcClip = com.example.model.ClipItem(id = "src_vid", trackId = "", title = "Vid", startTimeMs = 0L, durationMs = 2000L)
        val tgtClip = com.example.model.ClipItem(id = "tgt_txt", trackId = "", title = "Tag", type = com.example.model.ClipType.TEXT, startTimeMs = 0L, durationMs = 2000L)
        repo.addClipToTrack("", srcClip)
        repo.addClipToTrack("", tgtClip)

        repo.applyCameraTracking(
            sourceClipId = "src_vid",
            targetClipId = "tgt_txt",
            points = points,
            applyTranslation = true,
            applyScale = false,
            stabilizeMode = false
        )
        val tracked = repo.currentProject.value.findClip("tgt_txt")!!
        assertEquals(3, tracked.cameraTrackPoints.size)
        val xKfs = tracked.keyframeTracks[AnimatableProperty.POSITION_X]?.keyframes.orEmpty()
        assertEquals(3, xKfs.size)
        assertTrue(xKfs.last().value > 5f)
    }

    @Test
    fun testEffectCatalogCategoriesAndColorCustomization() {
        assertEquals(
            listOf("All", "Background", "Blur", "Lighting", "Filter", "Other"),
            com.example.data.EffectCatalog.categories
        )
        com.example.data.EffectCatalog.allPresets.forEach { preset ->
            assertFalse("Unexpected 'cyber' in ${preset.name}", preset.name.contains("cyber", ignoreCase = true))
            assertFalse("Unexpected 'neon' in ${preset.name}", preset.name.contains("neon", ignoreCase = true))
            assertFalse("Unexpected 'cyber' in ${preset.description}", preset.description.contains("cyber", ignoreCase = true))
            assertFalse("Unexpected 'neon' in ${preset.description}", preset.description.contains("neon", ignoreCase = true))
        }

        val vm = com.example.ui.EditorViewModel(repository = ProjectRepository(seedSampleProjects = true))
        val firstClip = vm.currentProject.value.tracks.first().clips.first()
        vm.selectClip(firstClip.id)
        val preset = com.example.data.EffectCatalog.allPresets.first()
        vm.addEffectPreset(preset)
        val addedFx = vm.currentProject.value.findClip(firstClip.id)!!.effects.last()
        vm.updateEffect(
            addedFx.copy(
                intensity = 0.8f,
                speed = 1.2f,
                paramA = 0.5f,
                paramB = 0.5f,
                colorHex = 0xFFFF2A85
            )
        )
        val updatedFx = vm.currentProject.value.findClip(firstClip.id)!!.effects.last()
        assertEquals(0xFFFF2A85, updatedFx.colorHex)
    }

    @Test
    fun testUnlimitedMultiLineExtruded3DText() {
        val longText = "MOTION STUDIO 3D\nPRO CREATOR EDITION 2026"
        assertTrue(longText.length > 20)
        val mesh = com.example.engine.Mesh3DParser.buildExtrudedTextMesh(longText, 0.45f)
        assertTrue(mesh.vertices.isNotEmpty())
        assertTrue(mesh.faces.isNotEmpty())

        val cfg = com.example.model.Model3DConfig(
            source = com.example.model.Model3DSource.EXTRUDED_TEXT,
            extrudedText = longText
        )
        assertEquals(longText, cfg.extrudedText)
    }

    @Test
    fun testMaskingCircleCornerBorderAndAspectCustomization() {
        val circleMask = MaskConfig(
            id = "circle_mask_1",
            shapeType = MaskShapeType.CIRCLE,
            positionX = 12f,
            positionY = -18f,
            aspectWidth = 1.4f,
            aspectHeight = 0.8f,
            cornerRadiusPx = 28f,
            opacity = 0.75f,
            borderWidthPx = 6f,
            borderColorHex = 0xFFFF4D6D
        )
        val evaluated = MaskCompositor.evaluateMask(
            mask = circleMask,
            currentTimeMs = 500L,
            clipStartTimeMs = 0L,
            clipWidth = 400f,
            clipHeight = 400f
        )
        assertEquals(12f, evaluated.positionX, 0.01f)
        assertEquals(-18f, evaluated.positionY, 0.01f)
        assertEquals(0.75f, evaluated.opacity, 0.01f)
        assertEquals(6f, evaluated.borderWidthPx, 0.01f)
        assertEquals(0xFFFF4D6D, evaluated.borderColorHex)
        assertFalse(evaluated.path.isEmpty)
    }

    @Test
    fun testUnifiedMoveKeyframePairingAndDeletion() {
        val repo = ProjectRepository()
        val clip = com.example.model.ClipItem(
            id = "move_clip",
            trackId = "",
            title = "Layer",
            startTimeMs = 0L,
            durationMs = 4000L
        )
        repo.addClipToTrack("", clip)

        // Toggling Move adds keyframes to both POSITION_X and POSITION_Y
        val added = repo.toggleMoveKeyframe("move_clip", 0L, currentX = 10f, currentY = -20f)
        assertTrue(added)
        repo.setMoveValues("move_clip", 1500L, posX = 110f, posY = 80f)

        val midEval = KeyframeEvaluator.evaluateClipTransform(repo.currentProject.value.findClip("move_clip")!!, 750L)
        assertEquals(60f, midEval.positionX, 2f)
        assertEquals(30f, midEval.positionY, 2f)

        // Delete Move keyframe at 1500ms removes both X and Y keyframes at that timestamp
        repo.deleteMoveKeyframeAtTime("move_clip", 1500L)
        val afterDelete = repo.currentProject.value.findClip("move_clip")!!
        assertEquals(1, afterDelete.keyframeTracks[AnimatableProperty.POSITION_X]?.keyframes?.size)
        assertEquals(1, afterDelete.keyframeTracks[AnimatableProperty.POSITION_Y]?.keyframes?.size)

        repo.clearMoveKeyframes("move_clip")
        val afterClear = repo.currentProject.value.findClip("move_clip")!!
        assertFalse(afterClear.keyframeTracks.containsKey(AnimatableProperty.POSITION_X))
        assertFalse(afterClear.keyframeTracks.containsKey(AnimatableProperty.POSITION_Y))
    }

    @Test
    fun testAutoBackgroundRemoverPreservesForegroundAndStripsBorder() {
        val bmp = android.graphics.Bitmap.createBitmap(16, 16, android.graphics.Bitmap.Config.ARGB_8888)
        // White uniform background with opaque red center square
        for (y in 0 until 16) {
            for (x in 0 until 16) {
                val isCenter = x in 5..10 && y in 5..10
                bmp.setPixel(x, y, if (isCenter) android.graphics.Color.RED else android.graphics.Color.WHITE)
            }
        }
        val cutout = com.example.engine.AutoBackgroundRemover.stripBorderBackground(bmp)
        assertEquals(0, android.graphics.Color.alpha(cutout.getPixel(0, 0)))
        assertEquals(255, android.graphics.Color.alpha(cutout.getPixel(8, 8)))
        bmp.recycle()
        if (cutout !== bmp) cutout.recycle()
    }

    @Test
    fun testStillImageOverlayRoutingAndQuickTextLayer() {
        val repo = ProjectRepository()
        val vm = com.example.ui.EditorViewModel(repository = repo)

        // Main video first
        vm.addClip(
            com.example.model.ClipItem(
                id = "main_v1",
                trackId = "",
                title = "Scene.mp4",
                type = com.example.model.ClipType.VIDEO_SAMPLE,
                mediaUri = "content://media/video/1",
                startTimeMs = 0L,
                durationMs = 5000L
            )
        )
        // Transparent PNG overlapping main video should auto-route to OVERLAY track
        vm.addClip(
            com.example.model.ClipItem(
                id = "png_overlay",
                trackId = "",
                title = "badge.png",
                type = com.example.model.ClipType.VIDEO_SAMPLE,
                mediaUri = "file:///tmp/badge.png",
                startTimeMs = 500L,
                durationMs = 4000L
            )
        )
        val overlayTrack = vm.currentProject.value.tracks.find { it.type == com.example.model.TrackType.OVERLAY }
        assertNotNull(overlayTrack)
        assertTrue(overlayTrack!!.clips.any { it.id == "png_overlay" })

        // One-tap addQuickTextLayer creates a selectable TEXT clip and switches to TEXT tab
        vm.addQuickTextLayer()
        val textTrack = vm.currentProject.value.tracks.find { it.type == com.example.model.TrackType.TEXT_STICKER }
        assertNotNull(textTrack)
        assertTrue(textTrack!!.clips.any { it.type == com.example.model.ClipType.TEXT })
        assertEquals(com.example.ui.EditorToolTab.TEXT, vm.activeToolTab.value)
    }

    @Test
    fun testMergeFromCloudPreservesLocalProjectsAndDeletedIds() = runBlocking {
        val repo = ProjectRepository()
        val localOnly = com.example.model.Project(id = "local_only", title = "Local Edit", updatedAt = 200L)
        val deletedLocal = com.example.model.Project(id = "deleted_id", title = "Old Edit", updatedAt = 100L)
        repo.createNewProject(localOnly)
        repo.createNewProject(deletedLocal)
        repo.deleteProject("deleted_id")

        val cloudProj = com.example.model.Project(id = "cloud_proj", title = "Cloud Edit", updatedAt = 300L)
        val staleDeletedFromCloud = com.example.model.Project(id = "deleted_id", title = "Old Edit", updatedAt = 150L)

        repo.mergeFromCloud(
            listOf(
                ProjectMappers.toEntity(cloudProj),
                ProjectMappers.toEntity(staleDeletedFromCloud)
            )
        )
        val ids = repo.projects.value.map { it.id }.toSet()
        assertTrue("Expected local_only preserved", "local_only" in ids)
        assertTrue("Expected cloud_proj merged", "cloud_proj" in ids)
        assertFalse("Expected deleted_id suppressed", "deleted_id" in ids)
    }

    @Test
    fun testMediaPermissionHelperAndManifestDeclarations() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val pkgInfo = app.packageManager.getPackageInfo(
            app.packageName,
            android.content.pm.PackageManager.GET_PERMISSIONS
        )
        val requested = pkgInfo.requestedPermissions?.toSet().orEmpty()
        assertTrue("Expected CAMERA permission in manifest", android.Manifest.permission.CAMERA in requested)
        assertTrue("Expected RECORD_AUDIO permission in manifest", android.Manifest.permission.RECORD_AUDIO in requested)

        val shadowApp = org.robolectric.Shadows.shadowOf(app)
        val helper = com.example.util.MediaPermissionHelper(app)

        // Initially neither permission granted
        shadowApp.denyPermissions(android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO)
        assertFalse(helper.hasCameraPermission())
        assertFalse(helper.hasMicrophonePermission())
        assertEquals(2, helper.getMissingPermissions(com.example.util.MediaRecordingFeature.CAMERA_AND_MICROPHONE).size)
        assertTrue(helper.isPermanentlyDenied(activity = null, feature = com.example.util.MediaRecordingFeature.CAMERA_ONLY, hasRequestedBefore = true))
        assertFalse(helper.isPermanentlyDenied(activity = null, feature = com.example.util.MediaRecordingFeature.CAMERA_ONLY, hasRequestedBefore = false))

        // Grant microphone only
        shadowApp.grantPermissions(android.Manifest.permission.RECORD_AUDIO)
        assertTrue(helper.hasRequiredPermissions(com.example.util.MediaRecordingFeature.MICROPHONE_ONLY))
        assertFalse(helper.hasRequiredPermissions(com.example.util.MediaRecordingFeature.CAMERA_AND_MICROPHONE))
        assertEquals(listOf(android.Manifest.permission.CAMERA), helper.getMissingPermissions(com.example.util.MediaRecordingFeature.CAMERA_AND_MICROPHONE).toList())

        // Grant camera as well
        shadowApp.grantPermissions(android.Manifest.permission.CAMERA)
        val fullSnapshot = helper.evaluateFeature(com.example.util.MediaRecordingFeature.CAMERA_AND_MICROPHONE)
        assertTrue(fullSnapshot.hasCameraPermission)
        assertTrue(fullSnapshot.hasMicrophonePermission)
        assertTrue(fullSnapshot.isFullyGranted)
        assertTrue(fullSnapshot.missingPermissions.isEmpty())

        // Verify App Settings fallback intent
        val settingsIntent = helper.createAppSettingsIntent()
        assertEquals(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, settingsIntent.action)
        assertEquals("package:${app.packageName}", settingsIntent.dataString)
    }

    @Test
    fun testVoiceOverEngineLifecycleAndTimelineAudioSync() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val shadowApp = org.robolectric.Shadows.shadowOf(app)
        val recorder = com.example.engine.VoiceOverRecorderEngine(this)

        // Denied permission blocks recording with descriptive error
        shadowApp.denyPermissions(android.Manifest.permission.RECORD_AUDIO)
        assertFalse(recorder.startRecording(app))
        assertFalse(recorder.isRecording.value)
        assertNotNull(recorder.recorderError.value)

        // Granted permission starts recording; pause/resume/stop/clear work cleanly
        shadowApp.grantPermissions(android.Manifest.permission.RECORD_AUDIO)
        assertTrue(recorder.startRecording(app))
        assertTrue(recorder.isRecording.value)

        recorder.pauseRecording()
        assertTrue(recorder.isPaused.value)
        recorder.resumeRecording()
        assertFalse(recorder.isPaused.value)

        val stopped = recorder.stopRecording()
        assertNotNull(stopped)
        assertFalse(recorder.isRecording.value)
        assertTrue(stopped!!.durationMs >= 800L)

        recorder.clearLastRecording()
        assertTrue(recorder.lastRecordedVoiceOver.value == null)

        // TimelineAudioPlayer handles paused state and nonexistent/active clips safely
        val audioPlayer = com.example.engine.TimelineAudioPlayer(app)
        val repo = ProjectRepository(seedSampleProjects = true)
        repo.importVoiceOverToProject(
            com.example.engine.RecordedVoiceOver(
                title = "Voiceover Missing File",
                durationMs = 2000L,
                waveformPoints = listOf(0.2f, 0.5f),
                fileUri = "file:///tmp/does_not_exist_vo.wav"
            ),
            playheadTimeMs = 0L
        )
        audioPlayer.sync(repo.currentProject.value, isPlaying = true, currentTimeMs = 500L)
        audioPlayer.sync(repo.currentProject.value, isPlaying = false, currentTimeMs = 500L)
        audioPlayer.releaseAll()
    }

    @Test
    fun testFirebaseResourcesAndInitializationConfigured() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val dbIdRes = app.resources.getIdentifier("firestore_database_id", "string", app.packageName)
        assertTrue("Expected firestore_database_id resource", dbIdRes != 0)
        assertTrue(app.getString(dbIdRes).isNotBlank())

        val googleAppIdRes = app.resources.getIdentifier("google_app_id", "string", app.packageName)
        val clientIdRes = app.resources.getIdentifier("default_web_client_id", "string", app.packageName)
        assertTrue("Expected google_app_id generated from google-services.json", googleAppIdRes != 0)
        assertTrue("Expected default_web_client_id generated from google-services.json", clientIdRes != 0)
        assertTrue(com.example.ui.auth.ensureFirebaseInitialized(app))
    }
}
