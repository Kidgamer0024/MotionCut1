package com.example.data

import com.example.data.local.ProjectDao
import com.example.data.local.ProjectEntity
import com.example.data.local.ProjectMappers
import com.example.engine.KeyframeEvaluator
import com.example.model.AnimatableProperty
import com.example.model.AspectRatio
import com.example.model.ClipItem
import com.example.model.EasingType
import com.example.model.ExportResolution
import com.example.model.Keyframe
import com.example.model.KeyframeTrack
import com.example.model.MarketplaceItem
import com.example.model.MaskConfig
import com.example.model.MaskKeyframe
import com.example.model.MusicTrack
import com.example.model.Project
import com.example.model.ReviewItem
import com.example.model.Track
import com.example.model.TrackType
import com.example.model.VideoEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.UUID

class ProjectRepository(
    private val projectDao: ProjectDao? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    private val seedSampleProjects: Boolean = false
) {
    private val emptyDraftProject = Project(
        id = "__empty_draft__",
        title = "Untitled Project",
        aspectRatio = AspectRatio.PORTRAIT_9_16,
        durationMs = 10_000L,
        tracks = listOf(
            Track(
                name = "Main Video",
                type = TrackType.MAIN_VIDEO,
                clips = emptyList()
            )
        )
    )

    // In-memory full Project models cache (no auto-seeded random projects unless explicitly requested)
    private val _projects = MutableStateFlow<List<Project>>(
        if (seedSampleProjects) SampleProjects.getAllInitialProjects() else emptyList()
    )
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    private val _currentProject = MutableStateFlow<Project>(
        _projects.value.firstOrNull() ?: emptyDraftProject
    )
    val currentProject: StateFlow<Project> = _currentProject.asStateFlow()

    // Fallback in-memory entities if projectDao is null (e.g. lightweight tests)
    private val _fallbackEntities = MutableStateFlow<List<ProjectEntity>>(
        if (seedSampleProjects) ProjectMappers.getInitialEntities() else emptyList()
    )

    // Room Database Flow of project entities for the landing view
    val savedProjectEntities: Flow<List<ProjectEntity>> = projectDao?.getAllProjects() ?: _fallbackEntities.asStateFlow()

    // Marketplace state
    private val _marketplaceItems = MutableStateFlow<List<MarketplaceItem>>(SampleMarketplaceData.getInitialMarketplaceItems())
    val marketplaceItems: StateFlow<List<MarketplaceItem>> = _marketplaceItems.asStateFlow()

    // Royalty-free Music Library state
    private val _musicTracks = MutableStateFlow<List<MusicTrack>>(SampleMusicLibrary.getAllTracks())
    val musicTracks: StateFlow<List<MusicTrack>> = _musicTracks.asStateFlow()

    // Action Stack for global Undo & Redo
    val actionStack = ActionStack()
    val canUndo: StateFlow<Boolean> = actionStack.canUndo
    val canRedo: StateFlow<Boolean> = actionStack.canRedo
    val lastUndoDescription: StateFlow<String?> = actionStack.lastUndoDescription
    val lastRedoDescription: StateFlow<String?> = actionStack.lastRedoDescription

    init {
        if (projectDao != null) {
            scope.launch {
                if (seedSampleProjects) {
                    if (projectDao.getProjectCount() == 0) {
                        projectDao.insertProjects(ProjectMappers.getInitialEntities())
                    }
                } else {
                    // Remove any previously auto-seeded random sample projects user didn't create
                    SampleProjects.SEEDED_SAMPLE_IDS.forEach { sampleId ->
                        projectDao.deleteProjectById(sampleId)
                    }
                }
            }
            // Synchronize in-memory list whenever Room database changes
            projectDao.getAllProjects().onEach { entities ->
                val visibleEntities = if (seedSampleProjects) {
                    entities
                } else {
                    entities.filterNot { it.id in SampleProjects.SEEDED_SAMPLE_IDS }
                }
                val currentMemoryProjects = _projects.value
                val updatedProjects = visibleEntities.map { entity ->
                    val existing = currentMemoryProjects.find { it.id == entity.id }
                    ProjectMappers.toProject(entity, existing)
                }
                _projects.value = updatedProjects
                val cur = _currentProject.value
                val matchingCur = updatedProjects.find { it.id == cur.id }
                if (matchingCur != null && matchingCur.title != cur.title) {
                    _currentProject.value = cur.copy(title = matchingCur.title, updatedAt = matchingCur.updatedAt)
                } else if (cur.id == emptyDraftProject.id && updatedProjects.isNotEmpty()) {
                    _currentProject.value = updatedProjects.first()
                }
            }.launchIn(scope)
        }
    }

    fun syncFromCloud(cloudEntities: List<ProjectEntity>) {
        val filteredEntities = if (seedSampleProjects) {
            cloudEntities
        } else {
            cloudEntities.filterNot { it.id in SampleProjects.SEEDED_SAMPLE_IDS }
        }
        val currentMemoryProjects = _projects.value
        val updatedProjects = filteredEntities.map { entity ->
            val existing = currentMemoryProjects.find { it.id == entity.id }
            ProjectMappers.toProject(entity, existing)
        }
        _projects.value = updatedProjects

        val cur = _currentProject.value
        val matchingCur = updatedProjects.find { it.id == cur.id }
        if (matchingCur != null) {
            if (matchingCur.updatedAt > cur.updatedAt ||
                matchingCur.tracks != cur.tracks ||
                matchingCur.title != cur.title
            ) {
                _currentProject.value = if (matchingCur.tracks.isNotEmpty() || cur.tracks.isEmpty()) {
                    matchingCur
                } else {
                    cur.copy(title = matchingCur.title, updatedAt = matchingCur.updatedAt)
                }
            }
        } else if (updatedProjects.isNotEmpty()) {
            actionStack.clear()
            _currentProject.value = updatedProjects.first()
        } else {
            actionStack.clear()
            _currentProject.value = emptyDraftProject
        }

        if (projectDao != null) {
            scope.launch {
                projectDao.replaceAllProjects(filteredEntities)
            }
        } else {
            _fallbackEntities.value = filteredEntities
        }
    }

    fun selectProject(projectId: String) {
        val found = _projects.value.find { it.id == projectId }
        if (found != null) {
            actionStack.clear()
            _currentProject.value = found
        }
    }

    fun updateCurrentProject(
        updated: Project,
        actionDescription: String? = null,
        coalesceIntervalMs: Long = 0L
    ) {
        val cur = _currentProject.value
        if (actionDescription != null) {
            actionStack.pushAction(actionDescription, cur, coalesceIntervalMs)
        }
        val newUpdated = updated.copy(updatedAt = System.currentTimeMillis())
        _currentProject.value = newUpdated
        _projects.value = _projects.value.map { if (it.id == newUpdated.id) newUpdated else it }

        // Persist update in Room Database
        val entity = ProjectMappers.toEntity(newUpdated)
        if (projectDao != null) {
            scope.launch {
                projectDao.insertProject(entity)
            }
        } else {
            _fallbackEntities.value = _fallbackEntities.value.map { if (it.id == entity.id) entity else it }
        }
    }

    fun renameProject(projectId: String, newTitle: String) {
        val trimmed = newTitle.trim()
        if (trimmed.isEmpty()) return
        val now = System.currentTimeMillis()

        // 1. Update in Room Database
        if (projectDao != null) {
            scope.launch {
                projectDao.renameProject(projectId, trimmed, now)
            }
        } else {
            _fallbackEntities.value = _fallbackEntities.value.map {
                if (it.id == projectId) it.copy(title = trimmed, updatedAt = now) else it
            }
        }

        // 2. Update in memory
        _projects.value = _projects.value.map {
            if (it.id == projectId) it.copy(title = trimmed, updatedAt = now) else it
        }
        if (_currentProject.value.id == projectId) {
            _currentProject.value = _currentProject.value.copy(title = trimmed, updatedAt = now)
        }
    }

    fun deleteProject(projectId: String) {
        // 1. Delete from Room Database
        if (projectDao != null) {
            scope.launch {
                projectDao.deleteProjectById(projectId)
            }
        } else {
            _fallbackEntities.value = _fallbackEntities.value.filter { it.id != projectId }
        }

        // 2. Remove from memory
        val remaining = _projects.value.filter { it.id != projectId }
        _projects.value = remaining
        if (_currentProject.value.id == projectId) {
            actionStack.clear()
            _currentProject.value = remaining.firstOrNull() ?: emptyDraftProject
        }
    }

    fun duplicateProject(projectId: String) {
        val source = _projects.value.find { it.id == projectId } ?: return
        val newId = UUID.randomUUID().toString()
        val duplicated = source.copy(
            id = newId,
            title = "${source.title} (Copy)",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        createNewProject(duplicated)
    }

    fun undo(): String? {
        val result = actionStack.undo(_currentProject.value) ?: return null
        val (restored, desc) = result
        _currentProject.value = restored
        _projects.value = _projects.value.map { if (it.id == restored.id) restored else it }
        val entity = ProjectMappers.toEntity(restored)
        if (projectDao != null) {
            scope.launch { projectDao.insertProject(entity) }
        } else {
            _fallbackEntities.value = _fallbackEntities.value.map { if (it.id == entity.id) entity else it }
        }
        return desc
    }

    fun redo(): String? {
        val result = actionStack.redo(_currentProject.value) ?: return null
        val (restored, desc) = result
        _currentProject.value = restored
        _projects.value = _projects.value.map { if (it.id == restored.id) restored else it }
        val entity = ProjectMappers.toEntity(restored)
        if (projectDao != null) {
            scope.launch { projectDao.insertProject(entity) }
        } else {
            _fallbackEntities.value = _fallbackEntities.value.map { if (it.id == entity.id) entity else it }
        }
        return desc
    }

    fun createNewProject(project: Project) {
        val updatedList = listOf(project) + _projects.value
        _projects.value = updatedList
        actionStack.clear()
        _currentProject.value = project

        val entity = ProjectMappers.toEntity(project)
        if (projectDao != null) {
            scope.launch {
                projectDao.insertProject(entity)
            }
        } else {
            _fallbackEntities.value = listOf(entity) + _fallbackEntities.value
        }
    }

    fun addClipToTrack(trackId: String, clip: ClipItem) {
        val cur = _currentProject.value
        val desiredType = when (clip.type) {
            com.example.model.ClipType.AUDIO -> TrackType.AUDIO
            com.example.model.ClipType.TEXT,
            com.example.model.ClipType.SHAPE,
            com.example.model.ClipType.MODEL_3D -> TrackType.TEXT_STICKER
            com.example.model.ClipType.ADJUSTMENT, com.example.model.ClipType.NULL_OBJECT -> TrackType.EFFECTS
            else -> TrackType.MAIN_VIDEO
        }
        val targetTrack = cur.tracks.find { it.id == trackId }
            ?: cur.tracks.find { it.type == desiredType }

        val updatedTracks = if (targetTrack != null) {
            val routedClip = if (clip.trackId == targetTrack.id) clip else clip.copy(trackId = targetTrack.id)
            cur.tracks.map { track ->
                if (track.id == targetTrack.id) {
                    track.copy(clips = track.clips + routedClip)
                } else track
            }
        } else {
            val newTrackId = "track_${desiredType.name.lowercase()}_${UUID.randomUUID().toString().take(4)}"
            val newTrack = Track(
                id = newTrackId,
                name = desiredType.label,
                type = desiredType,
                clips = listOf(clip.copy(trackId = newTrackId))
            )
            cur.tracks + newTrack
        }
        val label = if (clip.title.isNotBlank()) "Add '${clip.title}'" else "Add ${clip.type.label}"
        updateCurrentProject(cur.copy(tracks = updatedTracks), label)
    }

    fun updateClip(
        clipId: String,
        actionDescription: String? = "Edit Clip",
        coalesceIntervalMs: Long = 0L,
        transform: (ClipItem) -> ClipItem
    ) {
        val cur = _currentProject.value
        val updatedTracks = cur.tracks.map { track ->
            val updatedClips = track.clips.map { clip ->
                if (clip.id == clipId) transform(clip) else clip
            }
            track.copy(clips = updatedClips)
        }
        updateCurrentProject(cur.copy(tracks = updatedTracks), actionDescription, coalesceIntervalMs)
    }

    fun splitClipAtPlayhead(clipId: String, splitTimeMs: Long) {
        val cur = _currentProject.value
        val updatedTracks = cur.tracks.map { track ->
            val clip = track.clips.find { it.id == clipId }
            if (clip != null && splitTimeMs > clip.startTimeMs && splitTimeMs < clip.endTimeMs) {
                val firstDuration = splitTimeMs - clip.startTimeMs
                val secondDuration = clip.endTimeMs - splitTimeMs

                val firstClip = clip.copy(
                    durationMs = firstDuration
                )
                val secondClip = clip.copy(
                    id = UUID.randomUUID().toString(),
                    title = "${clip.title} (Part 2)",
                    startTimeMs = splitTimeMs,
                    durationMs = secondDuration
                )
                val newClips = track.clips.flatMap {
                    if (it.id == clipId) listOf(firstClip, secondClip) else listOf(it)
                }
                track.copy(clips = newClips)
            } else track
        }
        updateCurrentProject(cur.copy(tracks = updatedTracks), "Split Clip")
    }

    fun splitClipIntoParts(clipId: String, partCount: Int) {
        val count = partCount.coerceIn(2, 8)
        val cur = _currentProject.value
        val targetClip = cur.findClip(clipId) ?: return
        if (targetClip.durationMs < count * 200L) return

        val partDuration = targetClip.durationMs / count
        val remainder = targetClip.durationMs % count

        val updatedTracks = cur.tracks.map { track ->
            if (track.clips.any { it.id == clipId }) {
                val newClips = track.clips.flatMap { clip ->
                    if (clip.id == clipId) {
                        var offset = clip.startTimeMs
                        (0 until count).map { idx ->
                            val segDur = partDuration + if (idx == count - 1) remainder else 0L
                            val seg = clip.copy(
                                id = if (idx == 0) clip.id else UUID.randomUUID().toString(),
                                title = "${clip.title.substringBefore(" (")} (${idx + 1}/$count)",
                                startTimeMs = offset,
                                durationMs = segDur
                            )
                            offset += segDur
                            seg
                        }
                    } else {
                        listOf(clip)
                    }
                }
                track.copy(clips = newClips)
            } else {
                track
            }
        }
        updateCurrentProject(cur.copy(tracks = updatedTracks), "Split into $count Parts")
    }

    fun toggleClipLink(clipId: String): Boolean? {
        val cur = _currentProject.value
        val clip = cur.findClip(clipId) ?: return null
        val newLinkedState = !clip.isLocked
        val label = if (newLinkedState) "Link & Sync Lock '${clip.title}'" else "Unlink '${clip.title}'"
        updateClip(clipId, label) { c ->
            c.copy(isLocked = newLinkedState)
        }
        return newLinkedState
    }

    fun updateProjectLayout(
        aspectRatio: AspectRatio,
        clipId: String?,
        layoutPresetLabel: String,
        scale: Float,
        offsetX: Float,
        offsetY: Float
    ) {
        val cur = _currentProject.value
        val updatedTracks = if (clipId != null) {
            cur.tracks.map { track ->
                track.copy(
                    clips = track.clips.map { clip ->
                        if (clip.id == clipId) {
                            clip.copy(
                                baseTransform = clip.baseTransform.copy(
                                    scaleX = scale,
                                    scaleY = scale,
                                    positionX = offsetX,
                                    positionY = offsetY
                                )
                            )
                        } else clip
                    }
                )
            }
        } else {
            cur.tracks
        }
        updateCurrentProject(
            cur.copy(aspectRatio = aspectRatio, tracks = updatedTracks),
            "Layout ($layoutPresetLabel • ${aspectRatio.ratioText})"
        )
    }

    fun upscaleProjectResolution(
        targetResolution: ExportResolution,
        clipId: String?,
        contrastBoost: Float,
        saturationBoost: Float
    ) {
        val cur = _currentProject.value
        val updatedTracks = if (clipId != null) {
            cur.tracks.map { track ->
                track.copy(
                    clips = track.clips.map { clip ->
                        if (clip.id == clipId) {
                            val newContrast = (clip.colorGrading.contrast * contrastBoost).coerceIn(0.8f, 1.8f)
                            val newSat = (clip.colorGrading.saturation * saturationBoost).coerceIn(0.8f, 1.8f)
                            clip.copy(
                                colorGrading = clip.colorGrading.copy(
                                    contrast = newContrast,
                                    saturation = newSat
                                )
                            )
                        } else clip
                    }
                )
            }
        } else {
            cur.tracks
        }
        updateCurrentProject(
            cur.copy(resolution = targetResolution, tracks = updatedTracks),
            "Upscale ${targetResolution.label}"
        )
    }

    fun applyDepthCloudPass(
        clipId: String,
        depthMask: MaskConfig,
        depthEffect: VideoEffect,
        parallaxScale: Float
    ) {
        updateClip(clipId, "Cloud Depth Map") { clip ->
            val filteredMasks = clip.masks.filterNot { it.name == depthMask.name }
            val filteredEffects = clip.effects.filterNot { it.type == depthEffect.type && it.speed == 0.8f }
            clip.copy(
                masks = filteredMasks + depthMask,
                effects = filteredEffects + depthEffect,
                baseTransform = clip.baseTransform.copy(
                    scaleX = (clip.baseTransform.scaleX * parallaxScale).coerceAtMost(2.5f),
                    scaleY = (clip.baseTransform.scaleY * parallaxScale).coerceAtMost(2.5f)
                )
            )
        }
    }

    fun duplicateClip(clipId: String) {
        val cur = _currentProject.value
        val updatedTracks = cur.tracks.map { track ->
            val clip = track.clips.find { it.id == clipId }
            if (clip != null) {
                val duplicated = clip.copy(
                    id = UUID.randomUUID().toString(),
                    title = "${clip.title} (Copy)",
                    startTimeMs = clip.endTimeMs + 200L
                )
                track.copy(clips = track.clips + duplicated)
            } else track
        }
        updateCurrentProject(cur.copy(tracks = updatedTracks), "Duplicate Clip")
    }

    fun deleteClip(clipId: String) {
        val cur = _currentProject.value
        val deletedClip = cur.findClip(clipId)
        val updatedTracks = cur.tracks.map { track ->
            track.copy(clips = track.clips.filter { it.id != clipId })
        }
        val label = if (deletedClip != null) "Delete '${deletedClip.title}'" else "Delete Clip"
        updateCurrentProject(cur.copy(tracks = updatedTracks), label)
    }

    fun toggleKeyframe(clipId: String, property: AnimatableProperty, playheadTimeMs: Long, currentValue: Float) {
        updateClip(clipId, "Toggle ${property.displayName} Keyframe") { clip ->
            val relTime = (playheadTimeMs - clip.startTimeMs).coerceIn(0L, clip.durationMs)
            val track = clip.keyframeTracks[property] ?: KeyframeTrack(property = property)
            val existing = track.getKeyframeAt(relTime, toleranceMs = 160L)

            val newKeyframes = if (existing != null) {
                track.keyframes.filter { it.id != existing.id }
            } else {
                val newKeyframe = Keyframe(
                    timeMs = relTime,
                    value = currentValue.coerceIn(property.minVal, property.maxVal),
                    easing = EasingType.CubicBezier(0.42f, 0.0f, 0.58f, 1.0f)
                )
                (track.keyframes + newKeyframe).sortedBy { it.timeMs }
            }

            val updatedMap = clip.keyframeTracks.toMutableMap()
            if (newKeyframes.isEmpty()) {
                updatedMap.remove(property)
            } else {
                updatedMap[property] = track.copy(keyframes = newKeyframes)
            }
            clip.copy(keyframeTracks = updatedMap)
        }
    }

    fun deleteKeyframe(clipId: String, property: AnimatableProperty, keyframeId: String) {
        updateClip(clipId, "Delete ${property.displayName} Keyframe") { clip ->
            val track = clip.keyframeTracks[property] ?: return@updateClip clip
            val remaining = track.keyframes.filter { it.id != keyframeId }
            val updatedMap = clip.keyframeTracks.toMutableMap()
            if (remaining.isEmpty()) {
                updatedMap.remove(property)
            } else {
                updatedMap[property] = track.copy(keyframes = remaining)
            }
            clip.copy(keyframeTracks = updatedMap)
        }
    }

    fun deleteKeyframeAtTime(clipId: String, property: AnimatableProperty, playheadTimeMs: Long) {
        updateClip(clipId, "Delete ${property.displayName} Keyframe") { clip ->
            val relTime = (playheadTimeMs - clip.startTimeMs).coerceIn(0L, clip.durationMs)
            val track = clip.keyframeTracks[property] ?: return@updateClip clip
            val target = track.getKeyframeAt(relTime, toleranceMs = 160L) ?: return@updateClip clip
            val remaining = track.keyframes.filter { it.id != target.id }
            val updatedMap = clip.keyframeTracks.toMutableMap()
            if (remaining.isEmpty()) {
                updatedMap.remove(property)
            } else {
                updatedMap[property] = track.copy(keyframes = remaining)
            }
            clip.copy(keyframeTracks = updatedMap)
        }
    }

    fun setKeyframeValue(
        clipId: String,
        property: AnimatableProperty,
        playheadTimeMs: Long,
        newValue: Float,
        autoCreateIfTrackExists: Boolean = true,
        forceCreateKeyframe: Boolean = false
    ) {
        val clamped = newValue.coerceIn(property.minVal, property.maxVal)
        updateClip(clipId, "Adjust ${property.displayName}", coalesceIntervalMs = 200L) { clip ->
            val relTime = (playheadTimeMs - clip.startTimeMs).coerceIn(0L, clip.durationMs)
            val track = clip.keyframeTracks[property]
            val hasTrackKeyframes = track != null && track.keyframes.isNotEmpty()

            val updatedTransform = when (property) {
                AnimatableProperty.POSITION_X -> clip.baseTransform.copy(positionX = clamped)
                AnimatableProperty.POSITION_Y -> clip.baseTransform.copy(positionY = clamped)
                AnimatableProperty.SCALE -> clip.baseTransform.copy(scaleX = clamped, scaleY = clamped)
                AnimatableProperty.ROTATION -> clip.baseTransform.copy(rotation = clamped)
                AnimatableProperty.OPACITY -> clip.baseTransform.copy(opacity = clamped)
                else -> clip.baseTransform
            }

            val updatedModel3d = clip.model3dConfig?.let { m3 ->
                when (property) {
                    AnimatableProperty.ROTATION_X_3D -> m3.copy(rotationX = clamped)
                    AnimatableProperty.ROTATION_Y_3D -> m3.copy(rotationY = clamped)
                    AnimatableProperty.ROTATION_Z_3D -> m3.copy(rotationZ = clamped)
                    AnimatableProperty.DEPTH_Z_3D -> m3.copy(depthZ = clamped)
                    AnimatableProperty.EXTRUSION_3D -> m3.copy(extrusionDepth = clamped)
                    AnimatableProperty.FOV_3D -> m3.copy(fovDegrees = clamped)
                    else -> m3
                }
            }

            val updatedMap = clip.keyframeTracks.toMutableMap()
            if (hasTrackKeyframes || forceCreateKeyframe) {
                val activeTrack = track ?: KeyframeTrack(property = property)
                val existing = activeTrack.getKeyframeAt(relTime, toleranceMs = 160L)
                val nextKeyframes = when {
                    existing != null -> activeTrack.keyframes.map { kf ->
                        if (kf.id == existing.id) kf.copy(value = clamped) else kf
                    }
                    autoCreateIfTrackExists || forceCreateKeyframe -> (
                        activeTrack.keyframes + Keyframe(
                            timeMs = relTime,
                            value = clamped,
                            easing = EasingType.CubicBezier(0.42f, 0.0f, 0.58f, 1.0f)
                        )
                    ).sortedBy { it.timeMs }
                    else -> activeTrack.keyframes
                }
                updatedMap[property] = activeTrack.copy(keyframes = nextKeyframes)
            }

            clip.copy(
                baseTransform = if (hasTrackKeyframes) clip.baseTransform else updatedTransform,
                model3dConfig = if (hasTrackKeyframes) clip.model3dConfig else updatedModel3d,
                keyframeTracks = updatedMap
            )
        }
    }

    fun updateClipTransformByDelta(
        clipId: String,
        playheadTimeMs: Long,
        panDeltaX: Float,
        panDeltaY: Float,
        zoomFactor: Float,
        rotationDelta: Float
    ) {
        updateClip(clipId, "Transform Clip", coalesceIntervalMs = 200L) { clip ->
            val current = KeyframeEvaluator.evaluateClipTransform(clip, playheadTimeMs)
            val newPosX = (current.positionX + panDeltaX)
                .coerceIn(AnimatableProperty.POSITION_X.minVal, AnimatableProperty.POSITION_X.maxVal)
            val newPosY = (current.positionY + panDeltaY)
                .coerceIn(AnimatableProperty.POSITION_Y.minVal, AnimatableProperty.POSITION_Y.maxVal)
            val newScale = (current.scaleX * zoomFactor)
                .coerceIn(AnimatableProperty.SCALE.minVal, AnimatableProperty.SCALE.maxVal)
            val newRot = (current.rotation + rotationDelta)
                .coerceIn(AnimatableProperty.ROTATION.minVal, AnimatableProperty.ROTATION.maxVal)

            val relTime = (playheadTimeMs - clip.startTimeMs).coerceIn(0L, clip.durationMs)
            val updatedMap = clip.keyframeTracks.toMutableMap()

            fun upsertIfAnimated(prop: AnimatableProperty, value: Float) {
                val track = updatedMap[prop] ?: return
                if (track.keyframes.isEmpty()) return
                val existing = track.getKeyframeAt(relTime, toleranceMs = 160L)
                val updatedKfs = if (existing != null) {
                    track.keyframes.map { if (it.id == existing.id) it.copy(value = value) else it }
                } else {
                    (track.keyframes + Keyframe(timeMs = relTime, value = value)).sortedBy { it.timeMs }
                }
                updatedMap[prop] = track.copy(keyframes = updatedKfs)
            }

            upsertIfAnimated(AnimatableProperty.POSITION_X, newPosX)
            upsertIfAnimated(AnimatableProperty.POSITION_Y, newPosY)
            upsertIfAnimated(AnimatableProperty.SCALE, newScale)
            upsertIfAnimated(AnimatableProperty.ROTATION, newRot)

            clip.copy(
                baseTransform = clip.baseTransform.copy(
                    positionX = if (updatedMap[AnimatableProperty.POSITION_X]?.keyframes?.isNotEmpty() == true) clip.baseTransform.positionX else newPosX,
                    positionY = if (updatedMap[AnimatableProperty.POSITION_Y]?.keyframes?.isNotEmpty() == true) clip.baseTransform.positionY else newPosY,
                    scaleX = if (updatedMap[AnimatableProperty.SCALE]?.keyframes?.isNotEmpty() == true) clip.baseTransform.scaleX else newScale,
                    scaleY = if (updatedMap[AnimatableProperty.SCALE]?.keyframes?.isNotEmpty() == true) clip.baseTransform.scaleY else newScale,
                    rotation = if (updatedMap[AnimatableProperty.ROTATION]?.keyframes?.isNotEmpty() == true) clip.baseTransform.rotation else newRot
                ),
                keyframeTracks = updatedMap
            )
        }
    }

    fun clearKeyframesForProperty(clipId: String, property: AnimatableProperty) {
        updateClip(clipId, "Clear ${property.displayName} Keyframes") { clip ->
            val updatedMap = clip.keyframeTracks.toMutableMap()
            updatedMap.remove(property)
            clip.copy(keyframeTracks = updatedMap)
        }
    }

    fun updateKeyframeEasing(clipId: String, property: AnimatableProperty, keyframeId: String, easing: EasingType) {
        updateClip(clipId, "Change Keyframe Curve") { clip ->
            val track = clip.keyframeTracks[property] ?: return@updateClip clip
            val updatedKeyframes = track.keyframes.map {
                if (it.id == keyframeId) it.copy(easing = easing) else it
            }
            val updatedMap = clip.keyframeTracks.toMutableMap()
            updatedMap[property] = track.copy(keyframes = updatedKeyframes)
            clip.copy(keyframeTracks = updatedMap)
        }
    }

    fun updateClipSpeed(
        clipId: String,
        speed: Float,
        preserveAudioPitch: Boolean,
        isReversed: Boolean,
        adjustDuration: Boolean = true
    ) {
        val safeSpeed = speed.coerceIn(0.25f, 4.0f)
        val cur = _currentProject.value
        val existingClip = cur.findClip(clipId) ?: return
        val newDuration = if (adjustDuration && existingClip.speed > 0f) {
            ((existingClip.durationMs * existingClip.speed) / safeSpeed).toLong().coerceIn(300L, 600_000L)
        } else {
            existingClip.durationMs
        }
        val ratio = if (existingClip.durationMs > 0L) {
            newDuration.toFloat() / existingClip.durationMs.toFloat()
        } else 1f

        val updatedTracks = cur.tracks.map { track ->
            track.copy(
                clips = track.clips.map { clip ->
                    if (clip.id == clipId) {
                        val rescaledKfs = if (adjustDuration && ratio != 1f) {
                            clip.keyframeTracks.mapValues { (_, kfTrack) ->
                                kfTrack.copy(
                                    keyframes = kfTrack.keyframes.map { kf ->
                                        kf.copy(timeMs = (kf.timeMs * ratio).toLong().coerceIn(0L, newDuration))
                                    }
                                )
                            }
                        } else clip.keyframeTracks
                        clip.copy(
                            speed = safeSpeed,
                            preserveAudioPitch = preserveAudioPitch,
                            isReversed = isReversed,
                            durationMs = newDuration,
                            keyframeTracks = rescaledKfs
                        )
                    } else clip
                }
            )
        }
        val maxEnd = updatedTracks.flatMap { it.clips }.maxOfOrNull { it.endTimeMs } ?: cur.durationMs
        val newProjDur = maxOf(cur.durationMs, maxEnd)
        updateCurrentProject(
            cur.copy(tracks = updatedTracks, durationMs = newProjDur),
            "Clip Speed ${String.format(java.util.Locale.US, "%.2fx", safeSpeed)}",
            coalesceIntervalMs = 200L
        )
    }

    fun updateClipTextConfig(clipId: String, textConfig: com.example.model.TextConfig) {
        updateClip(clipId, "Update Text Style", coalesceIntervalMs = 250L) { clip ->
            clip.copy(
                title = textConfig.text.take(28).ifBlank { clip.title },
                textConfig = textConfig
            )
        }
    }

    fun applyCameraTracking(
        sourceClipId: String,
        targetClipId: String,
        points: List<com.example.model.CameraTrackPoint>,
        applyTranslation: Boolean,
        applyScale: Boolean,
        stabilizeMode: Boolean = false
    ) {
        val cur = _currentProject.value
        val updatedTracks = cur.tracks.map { track ->
            track.copy(
                clips = track.clips.map { clip ->
                    var next = clip
                    if (clip.id == sourceClipId) {
                        next = next.copy(cameraTrackPoints = points)
                    }
                    if (clip.id == targetClipId && points.isNotEmpty()) {
                        val updatedMap = next.keyframeTracks.toMutableMap()
                        val sign = if (stabilizeMode) -1f else 1f
                        if (applyTranslation) {
                            val pxKfs = points.map { pt ->
                                Keyframe(
                                    timeMs = pt.timeMs.coerceIn(0L, next.durationMs),
                                    value = (next.baseTransform.positionX + sign * pt.x)
                                        .coerceIn(AnimatableProperty.POSITION_X.minVal, AnimatableProperty.POSITION_X.maxVal),
                                    easing = EasingType.Linear
                                )
                            }
                            val pyKfs = points.map { pt ->
                                Keyframe(
                                    timeMs = pt.timeMs.coerceIn(0L, next.durationMs),
                                    value = (next.baseTransform.positionY + sign * pt.y)
                                        .coerceIn(AnimatableProperty.POSITION_Y.minVal, AnimatableProperty.POSITION_Y.maxVal),
                                    easing = EasingType.Linear
                                )
                            }
                            updatedMap[AnimatableProperty.POSITION_X] = KeyframeTrack(AnimatableProperty.POSITION_X, pxKfs)
                            updatedMap[AnimatableProperty.POSITION_Y] = KeyframeTrack(AnimatableProperty.POSITION_Y, pyKfs)
                        }
                        if (applyScale) {
                            val scKfs = points.map { pt ->
                                val targetScale = if (stabilizeMode) (2f - pt.scale) else pt.scale
                                Keyframe(
                                    timeMs = pt.timeMs.coerceIn(0L, next.durationMs),
                                    value = (next.baseTransform.scaleX * targetScale)
                                        .coerceIn(AnimatableProperty.SCALE.minVal, AnimatableProperty.SCALE.maxVal),
                                    easing = EasingType.Linear
                                )
                            }
                            updatedMap[AnimatableProperty.SCALE] = KeyframeTrack(AnimatableProperty.SCALE, scKfs)
                        }
                        next = next.copy(keyframeTracks = updatedMap, cameraTrackPoints = points)
                    }
                    next
                }
            )
        }
        val modeLabel = if (stabilizeMode) "Stabilize" else "Camera Track"
        updateCurrentProject(cur.copy(tracks = updatedTracks), "$modeLabel (${points.size} pts)")
    }

    // --- MASKING MANAGEMENT ---
    fun addMaskToClip(clipId: String, mask: MaskConfig) {
        updateClip(clipId, "Add Mask (${mask.name})") { clip ->
            clip.copy(masks = clip.masks + mask)
        }
    }

    fun updateMaskOnClip(clipId: String, updatedMask: MaskConfig) {
        updateClip(clipId, "Update Mask (${updatedMask.name})", coalesceIntervalMs = 250L) { clip ->
            clip.copy(masks = clip.masks.map { if (it.id == updatedMask.id) updatedMask else it })
        }
    }

    fun deleteMaskFromClip(clipId: String, maskId: String) {
        updateClip(clipId, "Delete Mask") { clip ->
            clip.copy(masks = clip.masks.filter { it.id != maskId })
        }
    }

    fun toggleMaskKeyframe(clipId: String, maskId: String, playheadTimeMs: Long) {
        updateClip(clipId, "Toggle Mask Keyframe") { clip ->
            val relTime = (playheadTimeMs - clip.startTimeMs).coerceIn(0L, clip.durationMs)
            val updatedMasks = clip.masks.map { mask ->
                if (mask.id == maskId) {
                    val existing = mask.keyframes.find { kotlin.math.abs(it.timeMs - relTime) <= 160L }
                    val newKfs = if (existing != null) {
                        mask.keyframes.filter { it.id != existing.id }
                    } else {
                        val newKf = MaskKeyframe(
                            timeMs = relTime,
                            positionX = mask.positionX,
                            positionY = mask.positionY,
                            scale = mask.scaleX,
                            rotation = mask.rotation,
                            featherPx = mask.featherPx
                        )
                        (mask.keyframes + newKf).sortedBy { it.timeMs }
                    }
                    mask.copy(isAnimated = true, keyframes = newKfs)
                } else mask
            }
            clip.copy(masks = updatedMasks)
        }
    }

    // --- MARKETPLACE & REVIEWS ---
    fun addReviewToMarketplaceItem(itemId: String, review: ReviewItem) {
        val items = _marketplaceItems.value.map { item ->
            if (item.id == itemId) {
                val newReviews = listOf(review) + item.reviews
                val newAvg = newReviews.map { it.rating }.average().toFloat()
                item.copy(reviews = newReviews, averageRating = newAvg)
            } else item
        }
        _marketplaceItems.value = items
    }

    fun publishCreationToMarketplace(item: MarketplaceItem) {
        _marketplaceItems.value = listOf(item) + _marketplaceItems.value
    }

    fun incrementDownloadCount(itemId: String) {
        _marketplaceItems.value = _marketplaceItems.value.map {
            if (it.id == itemId) it.copy(downloadsCount = it.downloadsCount + 1) else it
        }
    }

    // --- MUSIC IMPORT TO PROJECT ---
    fun importMusicTrackToProject(track: MusicTrack, playheadTimeMs: Long) {
        val cur = _currentProject.value
        val audioTrack = cur.tracks.find { it.type == TrackType.AUDIO }

        val newClip = ClipItem(
            id = UUID.randomUUID().toString(),
            trackId = audioTrack?.id ?: "track_audio_imported",
            title = "${track.title} (${track.artist})",
            type = com.example.model.ClipType.AUDIO,
            colorHex = track.coverColorHex,
            startTimeMs = playheadTimeMs,
            durationMs = (track.durationSec * 1000L).coerceAtMost(cur.durationMs - playheadTimeMs).coerceAtLeast(4000L),
            volume = 0.9f,
            waveformSamples = track.waveformSamples
        )

        val updatedTracks = if (audioTrack != null) {
            cur.tracks.map {
                if (it.id == audioTrack.id) it.copy(clips = it.clips + newClip) else it
            }
        } else {
            val createdTrack = Track(
                id = "track_audio_imported",
                name = "Audio Soundtrack",
                type = TrackType.AUDIO,
                clips = listOf(newClip)
            )
            cur.tracks + createdTrack
        }

        updateCurrentProject(cur.copy(tracks = updatedTracks), "Import Music (${track.title})")
    }

    // --- VOICE-OVER RECORDING IMPORT ---
    fun importVoiceOverToProject(voiceOver: com.example.engine.RecordedVoiceOver, playheadTimeMs: Long) {
        val cur = _currentProject.value
        val audioTrack = cur.tracks.find { it.type == TrackType.AUDIO }

        val newClip = ClipItem(
            id = UUID.randomUUID().toString(),
            trackId = audioTrack?.id ?: "track_voiceovers",
            title = "🎙️ ${voiceOver.title}",
            type = com.example.model.ClipType.AUDIO,
            colorHex = 0xFF8E24AA,
            startTimeMs = playheadTimeMs,
            durationMs = voiceOver.durationMs,
            volume = 1.0f,
            waveformSamples = voiceOver.waveformPoints
        )

        val updatedTracks = if (audioTrack != null) {
            cur.tracks.map {
                if (it.id == audioTrack.id) it.copy(clips = it.clips + newClip) else it
            }
        } else {
            val createdTrack = Track(
                id = "track_voiceovers",
                name = "Voice-overs & Audio",
                type = TrackType.AUDIO,
                clips = listOf(newClip)
            )
            cur.tracks + createdTrack
        }

        updateCurrentProject(cur.copy(tracks = updatedTracks), "Record ${voiceOver.title}")
    }

    // --- AI KINETIC CAPTIONS INSERTION ---
    fun addCaptionsToProject(captions: List<com.example.engine.AiCaptionItem>, baseTimeMs: Long) {
        val cur = _currentProject.value
        val textTrack = cur.tracks.find { it.type == TrackType.TEXT_STICKER }

        val newClips = captions.mapIndexed { index, cap ->
            val clipStartTime = (baseTimeMs + cap.startOffsetMs).coerceIn(0L, (cur.durationMs - 500L).coerceAtLeast(0L))
            val clipDur = cap.durationMs.coerceAtMost(cur.durationMs - clipStartTime).coerceAtLeast(600L)
            ClipItem(
                id = UUID.randomUUID().toString(),
                trackId = textTrack?.id ?: "track_text_captions",
                title = cap.text,
                type = com.example.model.ClipType.TEXT,
                startTimeMs = clipStartTime,
                durationMs = clipDur,
                baseTransform = com.example.model.TransformState(
                    positionY = 120f,
                    scaleX = if (cap.isEmphasis) 1.25f else 1.0f,
                    scaleY = if (cap.isEmphasis) 1.25f else 1.0f
                ),
                textConfig = com.example.model.TextConfig(
                    text = cap.text,
                    fontSizeSp = if (cap.isEmphasis) 32f else 24f,
                    textColor = cap.colorHex,
                    isBold = true,
                    strokeColor = 0xFF000000,
                    strokeWidth = if (cap.isEmphasis) 4f else 2f
                )
            )
        }

        val updatedTracks = if (textTrack != null) {
            cur.tracks.map {
                if (it.id == textTrack.id) it.copy(clips = it.clips + newClips) else it
            }
        } else {
            val createdTrack = Track(
                id = "track_text_captions",
                name = "AI Kinetic Captions",
                type = TrackType.TEXT_STICKER,
                clips = newClips
            )
            cur.tracks + createdTrack
        }

        updateCurrentProject(cur.copy(tracks = updatedTracks), "Add AI Captions (${captions.size})")
    }
}
