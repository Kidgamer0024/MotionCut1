package com.example.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ProjectRepository
import com.example.data.local.ProjectEntity
import com.example.data.remote.FirestoreProjectRepository
import com.example.engine.AiVideoToolsManager
import com.example.engine.AudioSynthesizerEngine
import com.example.engine.AutoBackgroundRemover
import com.example.engine.CameraTracker
import com.example.engine.KeyframeEvaluator
import com.example.engine.PlaybackEngine
import com.example.engine.RecordedVoiceOver
import com.example.engine.VoiceOverRecorderEngine
import com.example.model.MaskShapeType
import com.example.model.AnimatableProperty
import com.example.model.AspectRatio
import com.example.model.ClipItem
import com.example.model.CloudProject
import com.example.model.ColorGradingSettings
import com.example.model.EasingType
import com.example.model.EffectPreset
import com.example.model.EffectType
import com.example.model.ExportResolution
import com.example.model.FrameRate
import com.example.model.MarketplaceCategory
import com.example.model.MarketplaceItem
import com.example.model.MaskConfig
import com.example.model.Model3DConfig
import com.example.model.MusicTrack
import com.example.model.Project
import com.example.model.ReviewItem
import com.example.model.TextConfig
import com.example.model.Track
import com.example.model.TrackType
import com.example.model.VideoEffect
import com.example.ui.components.ScopeMode
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.UUID

enum class EditorToolTab(val label: String) {
    TIMELINE("Timeline"),
    KEYFRAMES("Keyframe Graph"),
    TEXT("Text Studio"),
    SPEED("Speed"),
    CAMERA_TRACK("Tracker"),
    COLOR_GRADING("Color Grading"),
    EFFECTS("FX Stack"),
    MASKING("Masking"),
    MODEL_3D("3D Studio")
}

enum class ProjectSortMode(val label: String) {
    RECENT("Recently Edited"),
    NAME("Name A-Z"),
    DURATION("Duration")
}

sealed interface CloudSyncState {
    data object LocalOnly : CloudSyncState
    data object Syncing : CloudSyncState
    data class Synced(val count: Int, val lastSyncedAtMs: Long = System.currentTimeMillis()) : CloudSyncState
    data class Error(val message: String) : CloudSyncState
}

class EditorViewModel(
    val repository: ProjectRepository,
    private val firestoreRepository: FirestoreProjectRepository? = null,
    val currentUserId: String? = null,
    val currentUserDisplayName: String? = null,
    val currentUserPhotoUrl: String? = null,
    val aiToolsManager: AiVideoToolsManager = AiVideoToolsManager()
) : ViewModel() {

    val playbackEngine = PlaybackEngine(viewModelScope)
    val audioSynthesizerEngine = AudioSynthesizerEngine(viewModelScope)
    val voiceOverRecorderEngine = VoiceOverRecorderEngine(viewModelScope)

    val currentProject: StateFlow<Project> = repository.currentProject
    val allProjects: StateFlow<List<Project>> = repository.projects

    // Room Database Flow of saved video projects for the landing view
    val savedProjects: StateFlow<List<ProjectEntity>> = repository.savedProjectEntities
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _initialSeedFinished = MutableStateFlow(false)
    private val _cloudSyncRetryTrigger = MutableStateFlow(0)
    private val uploadedVersionKeys = Collections.synchronizedSet(mutableSetOf<String>())

    // Real-time Firestore listener with two-tier Flow error handling and manual retry trigger
    @OptIn(ExperimentalCoroutinesApi::class)
    val cloudSyncState: StateFlow<CloudSyncState> = if (firestoreRepository != null && currentUserId != null) {
        _cloudSyncRetryTrigger
            .flatMapLatest {
                firestoreRepository.observeCloudProjects(currentUserId)
                    .onEach { cloudProjects ->
                        val userCreatedProjects = cloudProjects.filterNot {
                            it.id in com.example.data.SampleProjects.SEEDED_SAMPLE_IDS
                        }
                        val localToUpload = repository.mergeFromCloud(userCreatedProjects.map { it.toRoomEntity() })
                        if (_initialSeedFinished.value && localToUpload.isNotEmpty()) {
                            viewModelScope.launch {
                                localToUpload.forEach { localProj ->
                                    val key = "${localProj.id}:${localProj.updatedAt}"
                                    if (uploadedVersionKeys.add(key)) {
                                        firestoreRepository.upsertCloudProject(localProj)
                                    }
                                }
                            }
                        }
                    }
                    .map<List<CloudProject>, CloudSyncState> { cloudProjects ->
                        val userCount = cloudProjects.count {
                            it.id !in com.example.data.SampleProjects.SEEDED_SAMPLE_IDS
                        }
                        CloudSyncState.Synced(count = userCount)
                    }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val rootError = firestoreError ?: error
                        Log.w(TAG, "Error observing cloud projects", rootError)
                        emit(CloudSyncState.Error(rootError.localizedMessage ?: "Failed to sync cloud projects"))
                    }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = CloudSyncState.Syncing
            )
    } else {
        flowOf(CloudSyncState.LocalOnly).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = CloudSyncState.LocalOnly
        )
    }

    // Landing View search & sorting state
    private val _projectSearchQuery = MutableStateFlow("")
    val projectSearchQuery: StateFlow<String> = _projectSearchQuery.asStateFlow()

    private val _projectSortMode = MutableStateFlow(ProjectSortMode.RECENT)
    val projectSortMode: StateFlow<ProjectSortMode> = _projectSortMode.asStateFlow()

    val filteredProjects: StateFlow<List<ProjectEntity>> = combine(
        savedProjects,
        _projectSearchQuery,
        _projectSortMode
    ) { list, query, sort ->
        val filtered = if (query.isBlank()) {
            list
        } else {
            list.filter { it.title.contains(query, ignoreCase = true) }
        }
        when (sort) {
            ProjectSortMode.RECENT -> filtered.sortedByDescending { it.updatedAt }
            ProjectSortMode.NAME -> filtered.sortedBy { it.title.lowercase() }
            ProjectSortMode.DURATION -> filtered.sortedByDescending { it.durationMs }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val marketplaceItems: StateFlow<List<MarketplaceItem>> = repository.marketplaceItems
    val musicTracks: StateFlow<List<MusicTrack>> = repository.musicTracks

    private val _selectedClipId = MutableStateFlow<String?>(null)
    val selectedClipId: StateFlow<String?> = _selectedClipId.asStateFlow()

    private val _activeToolTab = MutableStateFlow(EditorToolTab.TIMELINE)
    val activeToolTab: StateFlow<EditorToolTab> = _activeToolTab.asStateFlow()

    private val _activeProperty = MutableStateFlow(AnimatableProperty.SCALE)
    val activeProperty: StateFlow<AnimatableProperty> = _activeProperty.asStateFlow()

    private val _showScopes = MutableStateFlow(false)
    val showScopes: StateFlow<Boolean> = _showScopes.asStateFlow()

    private val _selectedScope = MutableStateFlow(ScopeMode.WAVEFORM)
    val selectedScope: StateFlow<ScopeMode> = _selectedScope.asStateFlow()

    private val _showExportDialog = MutableStateFlow(false)
    val showExportDialog: StateFlow<Boolean> = _showExportDialog.asStateFlow()

    private val _showAddLayerDialog = MutableStateFlow(false)
    val showAddLayerDialog: StateFlow<Boolean> = _showAddLayerDialog.asStateFlow()

    private val _showProjectPicker = MutableStateFlow(false)
    val showProjectPicker: StateFlow<Boolean> = _showProjectPicker.asStateFlow()

    // Marketplace & Music Library sheets
    private val _showMarketplaceSheet = MutableStateFlow(false)
    val showMarketplaceSheet: StateFlow<Boolean> = _showMarketplaceSheet.asStateFlow()

    private val _showMusicLibrarySheet = MutableStateFlow(false)
    val showMusicLibrarySheet: StateFlow<Boolean> = _showMusicLibrarySheet.asStateFlow()

    // Voice-over Sheet State
    private val _showVoiceOverSheet = MutableStateFlow(false)
    val showVoiceOverSheet: StateFlow<Boolean> = _showVoiceOverSheet.asStateFlow()

    // Bottom Editing Dock & FX Browser Sheet/Dialog States
    private val _showLibraryHubSheet = MutableStateFlow(false)
    val showLibraryHubSheet: StateFlow<Boolean> = _showLibraryHubSheet.asStateFlow()

    private val _showEffectLibrarySheet = MutableStateFlow(false)
    val showEffectLibrarySheet: StateFlow<Boolean> = _showEffectLibrarySheet.asStateFlow()

    private val _favoriteEffectIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteEffectIds: StateFlow<Set<String>> = _favoriteEffectIds.asStateFlow()

    private val _showLayoutDialog = MutableStateFlow(false)
    val showLayoutDialog: StateFlow<Boolean> = _showLayoutDialog.asStateFlow()

    private val _showSplitPartsDialog = MutableStateFlow(false)
    val showSplitPartsDialog: StateFlow<Boolean> = _showSplitPartsDialog.asStateFlow()

    private val _showUpscaleDialog = MutableStateFlow(false)
    val showUpscaleDialog: StateFlow<Boolean> = _showUpscaleDialog.asStateFlow()

    // AI Studio Modal State
    private val _showAiStudioModal = MutableStateFlow(false)
    val showAiStudioModal: StateFlow<Boolean> = _showAiStudioModal.asStateFlow()

    private val _aiStudioInitialTab = MutableStateFlow(0)
    val aiStudioInitialTab: StateFlow<Int> = _aiStudioInitialTab.asStateFlow()

    private val _isAiProcessing = MutableStateFlow(false)
    val isAiProcessing: StateFlow<Boolean> = _isAiProcessing.asStateFlow()

    private val _aiStatusMessage = MutableStateFlow<String?>(null)
    val aiStatusMessage: StateFlow<String?> = _aiStatusMessage.asStateFlow()

    private val _aiSuccessFeedback = MutableStateFlow<String?>(null)
    val aiSuccessFeedback: StateFlow<String?> = _aiSuccessFeedback.asStateFlow()

    // Global Undo & Redo Action Stack State
    val canUndo: StateFlow<Boolean> = repository.canUndo
    val canRedo: StateFlow<Boolean> = repository.canRedo
    val lastUndoDescription: StateFlow<String?> = repository.lastUndoDescription
    val lastRedoDescription: StateFlow<String?> = repository.lastRedoDescription

    private val _undoToastMessage = MutableStateFlow<String?>(null)
    val undoToastMessage: StateFlow<String?> = _undoToastMessage.asStateFlow()

    private val _isCameraTracking = MutableStateFlow(false)
    val isCameraTracking: StateFlow<Boolean> = _isCameraTracking.asStateFlow()

    private val _cameraTrackingStatus = MutableStateFlow<String?>(null)
    val cameraTrackingStatus: StateFlow<String?> = _cameraTrackingStatus.asStateFlow()

    init {
        playbackEngine.setDuration(currentProject.value.durationMs)
        if (firestoreRepository != null && currentUserId != null) {
            viewModelScope.launch {
                firestoreRepository.ensureUserProfile(
                    displayName = currentUserDisplayName ?: "MotionCut Creator",
                    photoUrl = currentUserPhotoUrl
                )
                com.example.data.SampleProjects.SEEDED_SAMPLE_IDS.forEach { sampleId ->
                    firestoreRepository.deleteCloudProject(sampleId)
                }
                _initialSeedFinished.value = true

                val initialCloud = firestoreRepository.getCloudProjects(currentUserId).getOrNull()
                    ?.filterNot { it.id in com.example.data.SampleProjects.SEEDED_SAMPLE_IDS }
                    ?.map { it.toRoomEntity() }
                if (initialCloud != null) {
                    val toUpload = repository.mergeFromCloud(initialCloud)
                    toUpload.forEach { localProj ->
                        val key = "${localProj.id}:${localProj.updatedAt}"
                        if (uploadedVersionKeys.add(key)) {
                            firestoreRepository.upsertCloudProject(localProj)
                        }
                    }
                }
            }
            @OptIn(FlowPreview::class)
            viewModelScope.launch {
                repository.currentProject
                    .drop(1)
                    .debounce(700L)
                    .collect { proj ->
                        if (proj.id != "__empty_draft__") {
                            val key = "${proj.id}:${proj.updatedAt}"
                            if (uploadedVersionKeys.add(key)) {
                                firestoreRepository.upsertCloudProject(proj)
                            }
                        }
                    }
            }
        } else {
            _initialSeedFinished.value = true
        }
    }

    private fun syncCurrentProjectToCloud() {
        val repo = firestoreRepository ?: return
        val proj = currentProject.value
        if (proj.id == "__empty_draft__") return
        val key = "${proj.id}:${proj.updatedAt}"
        uploadedVersionKeys.add(key)
        viewModelScope.launch {
            repo.upsertCloudProject(proj)
        }
    }

    fun retryCloudSync() {
        val repo = firestoreRepository ?: return
        viewModelScope.launch {
            repo.ensureUserProfile(
                displayName = currentUserDisplayName ?: "MotionCut Creator",
                photoUrl = currentUserPhotoUrl
            )
            val cur = currentProject.value
            if (cur.id != "__empty_draft__") {
                repo.upsertCloudProject(cur)
            }
            _cloudSyncRetryTrigger.value += 1
        }
    }

    // --- LANDING VIEW ACTIONS ---
    fun setProjectSearchQuery(query: String) {
        _projectSearchQuery.value = query
    }

    fun setProjectSortMode(mode: ProjectSortMode) {
        _projectSortMode.value = mode
    }

    fun renameProject(projectId: String, newTitle: String) {
        repository.renameProject(projectId, newTitle)
        firestoreRepository?.let { repo ->
            viewModelScope.launch {
                repo.renameCloudProject(projectId, newTitle)
            }
        }
    }

    fun deleteProject(projectId: String) {
        repository.deleteProject(projectId)
        firestoreRepository?.let { repo ->
            viewModelScope.launch {
                repo.deleteCloudProject(projectId)
            }
        }
    }

    fun duplicateProject(projectId: String) {
        repository.duplicateProject(projectId)
        syncCurrentProjectToCloud()
    }

    fun selectProject(projectId: String) {
        playbackEngine.pause()
        audioSynthesizerEngine.stop()
        repository.selectProject(projectId)
        val proj = repository.currentProject.value
        playbackEngine.setDuration(proj.durationMs)
        playbackEngine.seek(0L)
        _selectedClipId.value = proj.tracks.firstOrNull()?.clips?.firstOrNull()?.id
        _showProjectPicker.value = false
    }

    fun createProject(
        title: String,
        ratio: AspectRatio = AspectRatio.PORTRAIT_9_16,
        frameRate: FrameRate = FrameRate.FPS_60,
        resolution: ExportResolution = ExportResolution.FHD_1080P
    ) {
        val newProj = Project(
            title = title,
            aspectRatio = ratio,
            frameRate = frameRate,
            resolution = resolution,
            durationMs = 10_000L,
            tracks = listOf(
                Track(
                    name = "Main Video",
                    type = TrackType.MAIN_VIDEO,
                    clips = emptyList()
                )
            )
        )
        repository.createNewProject(newProj)
        selectProject(newProj.id)
        syncCurrentProjectToCloud()
    }

    fun setToolTab(tab: EditorToolTab) {
        _activeToolTab.value = tab
    }

    fun selectClip(clipId: String?) {
        _selectedClipId.value = clipId
    }

    fun setActiveProperty(prop: AnimatableProperty) {
        _activeProperty.value = prop
    }

    fun toggleScopes() {
        _showScopes.value = !_showScopes.value
    }

    fun setScopeMode(mode: ScopeMode) {
        _selectedScope.value = mode
    }

    fun setShowExport(show: Boolean) {
        _showExportDialog.value = show
    }

    fun setShowAddLayer(show: Boolean) {
        _showAddLayerDialog.value = show
    }

    fun setShowProjectPicker(show: Boolean) {
        _showProjectPicker.value = show
    }

    fun setShowMarketplace(show: Boolean) {
        _showMarketplaceSheet.value = show
    }

    fun setShowMusicLibrary(show: Boolean) {
        _showMusicLibrarySheet.value = show
        if (!show) {
            audioSynthesizerEngine.stop()
        }
    }

    fun setShowEffectLibrary(show: Boolean) {
        _showEffectLibrarySheet.value = show
    }

    fun toggleFavoriteEffect(presetId: String) {
        val cur = _favoriteEffectIds.value
        _favoriteEffectIds.value = if (cur.contains(presetId)) cur - presetId else cur + presetId
    }

    // --- GLOBAL UNDO & REDO ACTIONS ---
    fun undo() {
        val desc = repository.undo()
        if (desc != null) {
            playbackEngine.setDuration(currentProject.value.durationMs)
            _undoToastMessage.value = "Undone: $desc"
            syncCurrentProjectToCloud()
        }
    }

    fun redo() {
        val desc = repository.redo()
        if (desc != null) {
            playbackEngine.setDuration(currentProject.value.durationMs)
            _undoToastMessage.value = "Redone: $desc"
            syncCurrentProjectToCloud()
        }
    }

    fun clearUndoToast() {
        _undoToastMessage.value = null
    }

    fun togglePlay() {
        playbackEngine.togglePlay()
    }

    fun seek(timeMs: Long) {
        playbackEngine.seek(timeMs)
    }

    fun stepFrame(forward: Boolean) {
        playbackEngine.stepFrame(forward, currentProject.value.frameRate.fps)
    }

    fun splitClip(clipId: String, splitTimeMs: Long) {
        repository.splitClipAtPlayhead(clipId, splitTimeMs)
        syncCurrentProjectToCloud()
    }

    fun duplicateClip(clipId: String) {
        repository.duplicateClip(clipId)
        syncCurrentProjectToCloud()
    }

    fun deleteClip(clipId: String) {
        repository.deleteClip(clipId)
        if (_selectedClipId.value == clipId) {
            _selectedClipId.value = null
        }
        syncCurrentProjectToCloud()
    }

    fun toggleKeyframeOnSelectedClip() {
        toggleKeyframeForProperty(_activeProperty.value)
    }

    fun toggleKeyframeForProperty(prop: AnimatableProperty) {
        val clipId = resolveTargetClipId() ?: return
        _selectedClipId.value = clipId
        _activeProperty.value = prop
        val clip = currentProject.value.findClip(clipId) ?: return
        val curTime = playbackEngine.currentTimeMs.value

        val curVal = KeyframeEvaluator.evaluateProperty(clip, prop, curTime)
        val wasPresent = clip.hasKeyframeAt(prop, curTime)
        repository.toggleKeyframe(clipId, prop, curTime, curVal)
        _undoToastMessage.value = if (wasPresent) {
            "Removed ${prop.displayName} keyframe"
        } else {
            "Added ${prop.displayName} keyframe"
        }
        syncCurrentProjectToCloud()
    }

    fun jumpToAdjacentKeyframe(next: Boolean) {
        val clipId = resolveTargetClipId() ?: return
        val clip = currentProject.value.findClip(clipId) ?: return
        val prop = _activeProperty.value
        val keyframes = clip.keyframeTracks[prop]?.keyframes?.sortedBy { it.timeMs }.orEmpty()
        if (keyframes.isEmpty()) return

        val relTime = playbackEngine.currentTimeMs.value - clip.startTimeMs
        val targetKf = if (next) {
            keyframes.firstOrNull { it.timeMs > relTime + 40L } ?: keyframes.last()
        } else {
            keyframes.lastOrNull { it.timeMs < relTime - 40L } ?: keyframes.first()
        }
        playbackEngine.pause()
        playbackEngine.seek(clip.startTimeMs + targetKf.timeMs)
    }

    fun clearKeyframesForProperty(property: AnimatableProperty) {
        val clipId = resolveTargetClipId() ?: return
        repository.clearKeyframesForProperty(clipId, property)
        _undoToastMessage.value = "Cleared ${property.displayName} keyframes"
        syncCurrentProjectToCloud()
    }

    fun deleteKeyframe(property: AnimatableProperty, keyframeId: String) {
        val clipId = resolveTargetClipId() ?: return
        repository.deleteKeyframe(clipId, property, keyframeId)
        _undoToastMessage.value = "Removed ${property.displayName} keyframe"
        syncCurrentProjectToCloud()
    }

    fun deleteKeyframeAtPlayhead(property: AnimatableProperty) {
        val clipId = resolveTargetClipId() ?: return
        repository.deleteKeyframeAtTime(clipId, property, playbackEngine.currentTimeMs.value)
        _undoToastMessage.value = "Removed ${property.displayName} keyframe"
        syncCurrentProjectToCloud()
    }

    fun updateKeyframeEasing(property: AnimatableProperty, keyframeId: String, easing: EasingType) {
        val clipId = resolveTargetClipId() ?: return
        repository.updateKeyframeEasing(clipId, property, keyframeId, easing)
        syncCurrentProjectToCloud()
    }

    fun toggleMoveKeyframe() {
        val clipId = resolveTargetClipId() ?: return
        _selectedClipId.value = clipId
        _activeProperty.value = AnimatableProperty.POSITION_X
        val clip = currentProject.value.findClip(clipId) ?: return
        val curTime = playbackEngine.currentTimeMs.value
        val curX = KeyframeEvaluator.evaluateProperty(clip, AnimatableProperty.POSITION_X, curTime)
        val curY = KeyframeEvaluator.evaluateProperty(clip, AnimatableProperty.POSITION_Y, curTime)
        val added = repository.toggleMoveKeyframe(clipId, curTime, curX, curY)
        _undoToastMessage.value = if (added) "Added Move keyframe" else "Removed Move keyframe"
        syncCurrentProjectToCloud()
    }

    fun deleteMoveKeyframeAtPlayhead() {
        val clipId = resolveTargetClipId() ?: return
        repository.deleteMoveKeyframeAtTime(clipId, playbackEngine.currentTimeMs.value)
        _undoToastMessage.value = "Removed Move keyframe"
        syncCurrentProjectToCloud()
    }

    fun clearMoveKeyframes() {
        val clipId = resolveTargetClipId() ?: return
        repository.clearMoveKeyframes(clipId)
        _undoToastMessage.value = "Cleared Move keyframes"
        syncCurrentProjectToCloud()
    }

    fun setMovePosition(posX: Float, posY: Float) {
        val clipId = resolveTargetClipId() ?: return
        _selectedClipId.value = clipId
        repository.setMoveValues(clipId, playbackEngine.currentTimeMs.value, posX, posY)
    }

    fun updateMoveKeyframeEasing(relTimeMs: Long, easing: EasingType) {
        val clipId = resolveTargetClipId() ?: return
        repository.updateMoveKeyframeEasing(clipId, relTimeMs, easing)
        syncCurrentProjectToCloud()
    }

    fun addQuickTextLayer() {
        val created = repository.addQuickTextClip(playheadTimeMs = playbackEngine.currentTimeMs.value)
        _selectedClipId.value = created.id
        _activeToolTab.value = EditorToolTab.TEXT
        _undoToastMessage.value = "Added Text Layer"
        syncCurrentProjectToCloud()
    }

    fun updateClipTransform(clipId: String, panX: Float, panY: Float, zoom: Float, rot: Float) {
        val curTime = playbackEngine.currentTimeMs.value
        repository.updateClipTransformByDelta(
            clipId = clipId,
            playheadTimeMs = curTime,
            panDeltaX = panX,
            panDeltaY = panY,
            zoomFactor = zoom,
            rotationDelta = rot
        )
    }

    fun updatePropertyValue(property: AnimatableProperty, newValue: Float) {
        val clipId = resolveTargetClipId() ?: return
        _selectedClipId.value = clipId
        val curTime = playbackEngine.currentTimeMs.value
        repository.setKeyframeValue(
            clipId = clipId,
            property = property,
            playheadTimeMs = curTime,
            newValue = newValue,
            autoCreateIfTrackExists = true
        )
    }

    fun updateColorGrading(grading: ColorGradingSettings) {
        val clipId = _selectedClipId.value
        if (clipId != null) {
            repository.updateClip(clipId, "Color Grade Clip", coalesceIntervalMs = 250L) { clip ->
                clip.copy(colorGrading = grading)
            }
        } else {
            val cur = currentProject.value
            repository.updateCurrentProject(cur.copy(masterColorGrading = grading), "Master Color Grade")
        }
    }

    fun addEffect(type: EffectType) {
        val clipId = resolveTargetClipId() ?: return
        _selectedClipId.value = clipId
        repository.updateClip(clipId, "Add ${type.displayName} Effect") { clip ->
            clip.copy(effects = clip.effects + VideoEffect(type = type))
        }
        syncCurrentProjectToCloud()
    }

    fun addEffectPreset(preset: EffectPreset) {
        val effect = VideoEffect(
            type = preset.kernel.toLegacyEffectType(),
            isEnabled = true,
            intensity = preset.defaultIntensity,
            speed = preset.defaultSpeed,
            presetId = preset.id,
            customName = preset.name,
            suite = preset.suite,
            category = preset.category,
            kernel = preset.kernel,
            colorHex = preset.defaultColorHex,
            paramA = preset.defaultParamA,
            paramB = preset.defaultParamB,
            paramALabel = preset.paramALabel,
            paramBLabel = preset.paramBLabel
        )
        val clipId = resolveTargetClipId()
        if (clipId != null) {
            _selectedClipId.value = clipId
            repository.updateClip(clipId, "Add ${preset.name}") { clip ->
                clip.copy(effects = clip.effects + effect)
            }
        } else {
            val fxClip = ClipItem(
                title = preset.name,
                type = com.example.model.ClipType.ADJUSTMENT,
                colorHex = preset.suite.accentHex,
                startTimeMs = playbackEngine.currentTimeMs.value,
                durationMs = 4000L,
                effects = listOf(effect)
            )
            addClip(fxClip)
        }
        _undoToastMessage.value = "Applied ${preset.name}"
        syncCurrentProjectToCloud()
    }

    fun updateEffect(effect: VideoEffect) {
        val clipId = _selectedClipId.value ?: return
        repository.updateClip(clipId, "Adjust Effect", coalesceIntervalMs = 200L) { clip ->
            clip.copy(effects = clip.effects.map { if (it.id == effect.id) effect else it })
        }
    }

    fun deleteEffect(effectId: String) {
        val clipId = _selectedClipId.value ?: return
        repository.updateClip(clipId, "Remove Effect") { clip ->
            clip.copy(effects = clip.effects.filter { it.id != effectId })
        }
    }

    fun updateClipModel3D(clipId: String, updatedConfig: Model3DConfig, actionDescription: String? = "Edit 3D Model") {
        repository.updateClip(clipId, actionDescription, coalesceIntervalMs = 200L) { clip ->
            clip.copy(model3dConfig = updatedConfig)
        }
    }

    fun updateClipTextConfig(textConfig: TextConfig) {
        val clipId = _selectedClipId.value ?: return
        repository.updateClipTextConfig(clipId, textConfig)
        syncCurrentProjectToCloud()
    }

    fun updateClipSpeed(
        speed: Float,
        preserveAudioPitch: Boolean,
        isReversed: Boolean,
        adjustDuration: Boolean = true
    ) {
        val clipId = resolveTargetClipId() ?: return
        _selectedClipId.value = clipId
        repository.updateClipSpeed(clipId, speed, preserveAudioPitch, isReversed, adjustDuration)
        playbackEngine.setDuration(currentProject.value.durationMs)
        _undoToastMessage.value = if (isReversed) {
            "Speed ${String.format(java.util.Locale.US, "%.2fx", speed)} • Reversed"
        } else {
            "Clip speed set to ${String.format(java.util.Locale.US, "%.2fx", speed)}"
        }
        syncCurrentProjectToCloud()
    }

    fun runCameraTracking(
        context: Context,
        sourceClipId: String,
        targetClipId: String,
        roiNormX: Float,
        roiNormY: Float,
        applyTranslation: Boolean,
        applyScale: Boolean,
        stabilizeMode: Boolean
    ) {
        val sourceClip = currentProject.value.findClip(sourceClipId) ?: return
        viewModelScope.launch {
            _isCameraTracking.value = true
            _cameraTrackingStatus.value = "Tracking motion patches across video frames..."
            try {
                val result = CameraTracker.trackClipMotion(
                    context = context,
                    clip = sourceClip,
                    roiNormX = roiNormX,
                    roiNormY = roiNormY
                )
                if (result.points.size >= 2) {
                    repository.applyCameraTracking(
                        sourceClipId = sourceClipId,
                        targetClipId = targetClipId,
                        points = result.points,
                        applyTranslation = applyTranslation,
                        applyScale = applyScale,
                        stabilizeMode = stabilizeMode
                    )
                    _cameraTrackingStatus.value = result.summary
                    _undoToastMessage.value = if (stabilizeMode) {
                        "Stabilized ${result.points.size} frames"
                    } else {
                        "Applied ${result.points.size} camera track points"
                    }
                    syncCurrentProjectToCloud()
                } else {
                    _cameraTrackingStatus.value = result.summary
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _cameraTrackingStatus.value = "Tracking failed: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                _isCameraTracking.value = false
            }
        }
    }

    fun addClip(clip: ClipItem) {
        val targetTrackId = clip.trackId
        repository.addClipToTrack(targetTrackId, clip)
        if (clip.type != com.example.model.ClipType.AUDIO) {
            _selectedClipId.value = clip.id
        }
        when (clip.type) {
            com.example.model.ClipType.MODEL_3D -> _activeToolTab.value = EditorToolTab.MODEL_3D
            com.example.model.ClipType.TEXT -> _activeToolTab.value = EditorToolTab.TEXT
            else -> Unit
        }
        syncCurrentProjectToCloud()
    }

    // --- MASKING ACTIONS ---
    fun addMaskToSelectedClip(mask: MaskConfig) {
        val clipId = _selectedClipId.value ?: return
        repository.addMaskToClip(clipId, mask)
    }

    fun updateMaskOnSelectedClip(mask: MaskConfig) {
        val clipId = _selectedClipId.value ?: return
        repository.updateMaskOnClip(clipId, mask)
    }

    fun deleteMaskFromSelectedClip(maskId: String) {
        val clipId = _selectedClipId.value ?: return
        repository.deleteMaskFromClip(clipId, maskId)
    }

    fun toggleMaskKeyframeOnSelectedClip(maskId: String) {
        val clipId = _selectedClipId.value ?: return
        val curTime = playbackEngine.currentTimeMs.value
        repository.toggleMaskKeyframe(clipId, maskId, curTime)
    }

    // --- MARKETPLACE ACTIONS ---
    fun downloadAndApplyMarketplaceItem(item: MarketplaceItem) {
        repository.incrementDownloadCount(item.id)
        when (item.category) {
            MarketplaceCategory.PRESETS -> {
                item.presetColorGrading?.let { preset ->
                    val clipId = _selectedClipId.value
                    if (clipId != null) {
                        repository.updateClip(clipId, "Apply Preset (${item.title})") { clip ->
                            clip.copy(colorGrading = preset)
                        }
                    }
                }
            }
            MarketplaceCategory.TRANSITIONS, MarketplaceCategory.TEMPLATES, MarketplaceCategory.ALL -> {
                val cur = currentProject.value
                val mainTrack = cur.tracks.find { it.type == TrackType.MAIN_VIDEO }
                if (mainTrack != null) {
                    val newClip = ClipItem(
                        id = UUID.randomUUID().toString(),
                        trackId = mainTrack.id,
                        title = item.title,
                        type = com.example.model.ClipType.COLOR_BG,
                        colorHex = 0xFF8A2BE2,
                        startTimeMs = playbackEngine.currentTimeMs.value,
                        durationMs = 3000L
                    )
                    repository.addClipToTrack(mainTrack.id, newClip)
                    syncCurrentProjectToCloud()
                }
            }
        }
    }

    fun addMarketplaceReview(itemId: String, review: ReviewItem) {
        repository.addReviewToMarketplaceItem(itemId, review)
    }

    fun publishCreation(item: MarketplaceItem) {
        repository.publishCreationToMarketplace(item)
    }

    // --- MUSIC LIBRARY ACTIONS ---
    fun playMusicPreview(track: MusicTrack) {
        audioSynthesizerEngine.playTrack(track)
    }

    fun stopMusicPreview() {
        audioSynthesizerEngine.stop()
    }

    fun importMusicTrackToTimeline(track: MusicTrack) {
        repository.importMusicTrackToProject(track, playbackEngine.currentTimeMs.value)
        setShowMusicLibrary(false)
        syncCurrentProjectToCloud()
    }

    // --- VOICE-OVER RECORDING ACTIONS ---
    fun setShowVoiceOverSheet(show: Boolean) {
        _showVoiceOverSheet.value = show
        if (show) {
            playbackEngine.pause()
            audioSynthesizerEngine.stop()
            voiceOverRecorderEngine.clearLastRecording()
        } else {
            audioSynthesizerEngine.stop()
            if (voiceOverRecorderEngine.isRecording.value) {
                voiceOverRecorderEngine.stopRecording()
            }
        }
    }

    fun importRecordedVoiceOverToTimeline(voiceOver: RecordedVoiceOver) {
        val playhead = playbackEngine.currentTimeMs.value
        repository.importVoiceOverToProject(voiceOver, playhead)
        playbackEngine.setDuration(currentProject.value.durationMs)
        val inserted = currentProject.value.tracks
            .filter { it.type == com.example.model.TrackType.AUDIO }
            .flatMap { it.clips }
            .lastOrNull()
        if (inserted != null) {
            _selectedClipId.value = inserted.id
        }
        playbackEngine.seek(playhead)
        voiceOverRecorderEngine.clearLastRecording()
        setShowVoiceOverSheet(false)
        syncCurrentProjectToCloud()
    }

    // --- BOTTOM DOCK MODAL CONTROLS & ACTIONS ---
    fun setShowLibraryHub(show: Boolean) {
        _showLibraryHubSheet.value = show
    }

    fun setShowLayoutDialog(show: Boolean) {
        _showLayoutDialog.value = show
    }

    fun setShowSplitPartsDialog(show: Boolean) {
        _showSplitPartsDialog.value = show
    }

    fun setShowUpscaleDialog(show: Boolean) {
        _showUpscaleDialog.value = show
    }

    fun resolveTargetClipId(): String? {
        val proj = currentProject.value
        val selected = _selectedClipId.value
        if (selected != null && proj.findClip(selected) != null) return selected
        val playhead = playbackEngine.currentTimeMs.value
        val atPlayhead = proj.tracks
            .flatMap { it.clips }
            .firstOrNull { playhead in it.startTimeMs..it.endTimeMs }
        return atPlayhead?.id ?: proj.tracks.firstOrNull()?.clips?.firstOrNull()?.id
    }

    fun toggleSelectedClipLink() {
        val clipId = resolveTargetClipId()
        if (clipId == null) {
            _undoToastMessage.value = "Add or select a clip first to link"
            return
        }
        _selectedClipId.value = clipId
        val linked = repository.toggleClipLink(clipId)
        val clipTitle = currentProject.value.findClip(clipId)?.title ?: "Clip"
        if (linked != null) {
            _undoToastMessage.value = if (linked) {
                "Linked & sync-locked '$clipTitle'"
            } else {
                "Unlinked '$clipTitle'"
            }
            syncCurrentProjectToCloud()
        }
    }

    fun applyProjectLayout(
        aspectRatio: AspectRatio,
        layoutPresetLabel: String,
        scale: Float,
        offsetX: Float,
        offsetY: Float
    ) {
        val clipId = resolveTargetClipId()
        repository.updateProjectLayout(
            aspectRatio = aspectRatio,
            clipId = clipId,
            layoutPresetLabel = layoutPresetLabel,
            scale = scale,
            offsetX = offsetX,
            offsetY = offsetY
        )
        _showLayoutDialog.value = false
        _undoToastMessage.value = "Layout: $layoutPresetLabel (${aspectRatio.ratioText})"
        syncCurrentProjectToCloud()
    }

    fun splitActiveClipIntoParts(partCount: Int) {
        val clipId = resolveTargetClipId()
        if (clipId == null) {
            _showSplitPartsDialog.value = false
            _undoToastMessage.value = "Add or select a clip to split"
            return
        }
        _selectedClipId.value = clipId
        repository.splitClipIntoParts(clipId, partCount)
        _showSplitPartsDialog.value = false
        _undoToastMessage.value = "Split clip into $partCount parts"
        syncCurrentProjectToCloud()
    }

    fun splitActiveClipAtPlayhead() {
        val clipId = resolveTargetClipId()
        if (clipId == null) {
            _showSplitPartsDialog.value = false
            _undoToastMessage.value = "Add or select a clip to split"
            return
        }
        val clip = currentProject.value.findClip(clipId) ?: return
        val splitPoint = playbackEngine.currentTimeMs.value.let { cur ->
            if (cur > clip.startTimeMs && cur < clip.endTimeMs) cur
            else clip.startTimeMs + (clip.durationMs / 2L)
        }
        _selectedClipId.value = clipId
        repository.splitClipAtPlayhead(clipId, splitPoint)
        _showSplitPartsDialog.value = false
        _undoToastMessage.value = "Split '${clip.title}' at playhead"
        syncCurrentProjectToCloud()
    }

    fun applyAiUpscale(targetResolution: ExportResolution) {
        val clipId = resolveTargetClipId()
        val clipTitle = clipId?.let { currentProject.value.findClip(it)?.title } ?: currentProject.value.title
        viewModelScope.launch {
            _isAiProcessing.value = true
            _aiStatusMessage.value = "Optimizing resolution to ${targetResolution.label}..."
            try {
                val profile = aiToolsManager.generateHdUpscaleProfile(
                    clipTitle = clipTitle,
                    targetResolutionLabel = targetResolution.label
                )
                repository.upscaleProjectResolution(
                    targetResolution = targetResolution,
                    clipId = clipId,
                    contrastBoost = profile.contrastBoost,
                    saturationBoost = profile.saturationBoost
                )
                _showUpscaleDialog.value = false
                _aiSuccessFeedback.value = "Output set to ${targetResolution.label} — ${profile.clarityNote}"
                _undoToastMessage.value = "Upscaled output to ${targetResolution.label}"
                syncCurrentProjectToCloud()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiStatusMessage.value = "Upscale error: ${e.message}"
            } finally {
                _isAiProcessing.value = false
            }
        }
    }

    fun applyQuickRemoveBg(context: Context? = null) {
        val clipId = resolveTargetClipId()
        if (clipId == null) {
            _undoToastMessage.value = "Add or select an image or video clip first"
            return
        }
        _selectedClipId.value = clipId
        val clip = currentProject.value.findClip(clipId) ?: return
        viewModelScope.launch {
            _isAiProcessing.value = true
            _aiStatusMessage.value = "Removing background on '${clip.title}'..."
            try {
                val appContext = context?.applicationContext
                val cutoutUri = if (appContext != null && (clip.mediaUri != null || clip.assetResId != null)) {
                    AutoBackgroundRemover.removeBackground(appContext, clip).getOrNull()
                } else null

                if (cutoutUri != null) {
                    repository.updateClip(clipId, "Remove Background") { c ->
                        c.copy(
                            mediaUri = cutoutUri,
                            assetResId = null,
                            colorHex = 0x00000000L
                        )
                    }
                    _aiSuccessFeedback.value = "Background removed from '${clip.title}'"
                    _undoToastMessage.value = "Background removed"
                } else {
                    repository.addMaskToClip(
                        clipId,
                        MaskConfig(
                            name = "Auto Cutout",
                            shapeType = MaskShapeType.CIRCLE,
                            featherPx = 12f,
                            expansion = 2f
                        )
                    )
                    _aiSuccessFeedback.value = "Applied cutout mask to '${clip.title}'"
                    _undoToastMessage.value = "Applied cutout mask"
                }
                syncCurrentProjectToCloud()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _undoToastMessage.value = "Remove BG failed: ${e.message}"
            } finally {
                _isAiProcessing.value = false
            }
        }
    }

    fun applyCloudDepthPass() {
        val clipId = resolveTargetClipId()
        if (clipId == null) {
            _undoToastMessage.value = "Add or select a clip for Cloud Depth"
            return
        }
        _selectedClipId.value = clipId
        val clip = currentProject.value.findClip(clipId) ?: return
        viewModelScope.launch {
            _isAiProcessing.value = true
            _aiStatusMessage.value = "Computing cloud depth map & parallax for '${clip.title}'..."
            try {
                val depth = aiToolsManager.generateCloudDepthPass(
                    clipTitle = clip.title,
                    focusSubject = "Foreground Subject"
                )
                repository.applyDepthCloudPass(
                    clipId = clipId,
                    depthMask = depth.depthMask,
                    depthEffect = depth.depthEffect,
                    parallaxScale = depth.parallaxScale
                )
                _aiSuccessFeedback.value = depth.summary
                _undoToastMessage.value = "Cloud Depth Map applied to '${clip.title}'"
                syncCurrentProjectToCloud()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiStatusMessage.value = "Cloud Depth error: ${e.message}"
            } finally {
                _isAiProcessing.value = false
            }
        }
    }

    // --- AI VIDEO TOOLS ACTIONS ---
    fun setShowAiStudioModal(show: Boolean) {
        if (show) {
            _aiStudioInitialTab.value = 0
        }
        _showAiStudioModal.value = show
        if (!show) {
            _aiStatusMessage.value = null
            _aiSuccessFeedback.value = null
        }
    }

    fun openAiStudioAtTab(tabIndex: Int) {
        _aiStudioInitialTab.value = tabIndex.coerceIn(0, 2)
        _showAiStudioModal.value = true
    }

    fun applyAiCutout(clipId: String?, subject: String, mode: String, feather: Float, expansion: Float) {
        if (clipId == null) return
        val clip = currentProject.value.findClip(clipId) ?: return
        viewModelScope.launch {
            _isAiProcessing.value = true
            _aiStatusMessage.value = "Analyzing subject bounds for '$subject'..."
            try {
                val result = aiToolsManager.removeBackgroundAndGenerateMask(
                    clipTitle = clip.title,
                    targetSubject = subject,
                    cutoutMode = mode
                )
                val customizedMask = result.mask.copy(
                    featherPx = feather,
                    expansion = expansion
                )
                repository.addMaskToClip(clipId, customizedMask)
                _aiSuccessFeedback.value = "✨ Extracted '${result.subjectLabel}' with AI alpha mask!"
                syncCurrentProjectToCloud()
            } catch (e: Exception) {
                _aiStatusMessage.value = "AI Cutout error: ${e.message}"
            } finally {
                _isAiProcessing.value = false
            }
        }
    }

    fun applyAiColorGrade(clipId: String?, moodDescription: String) {
        if (clipId == null) return
        val clip = currentProject.value.findClip(clipId) ?: return
        viewModelScope.launch {
            _isAiProcessing.value = true
            _aiStatusMessage.value = "Synthesizing Hollywood color look: '$moodDescription'..."
            try {
                val grading = aiToolsManager.generateAiColorGrade(
                    clipTitle = clip.title,
                    desiredMood = moodDescription
                )
                repository.updateClip(clipId, "AI Color Grade ($moodDescription)") { it.copy(colorGrading = grading) }
                _aiSuccessFeedback.value = "🎨 Applied custom AI color grade inspired by '$moodDescription'!"
                syncCurrentProjectToCloud()
            } catch (e: Exception) {
                _aiStatusMessage.value = "AI Color grading error: ${e.message}"
            } finally {
                _isAiProcessing.value = false
            }
        }
    }

    fun applyAiCaptions(sceneDescription: String, styleTheme: String) {
        viewModelScope.launch {
            _isAiProcessing.value = true
            _aiStatusMessage.value = "Generating kinetic animated captions ($styleTheme)..."
            try {
                val captions = aiToolsManager.generateKineticCaptions(
                    sceneDescription = sceneDescription,
                    styleTheme = styleTheme
                )
                repository.addCaptionsToProject(captions, baseTimeMs = playbackEngine.currentTimeMs.value)
                _aiSuccessFeedback.value = "⚡ Inserted ${captions.size} AI kinetic animated captions!"
                syncCurrentProjectToCloud()
            } catch (e: Exception) {
                _aiStatusMessage.value = "AI Captions error: ${e.message}"
            } finally {
                _isAiProcessing.value = false
            }
        }
    }

    fun clearAiFeedback() {
        _aiStatusMessage.value = null
        _aiSuccessFeedback.value = null
    }

    override fun onCleared() {
        super.onCleared()
        audioSynthesizerEngine.release()
        voiceOverRecorderEngine.release()
    }

    private companion object {
        const val TAG = "EditorViewModel"
    }
}
