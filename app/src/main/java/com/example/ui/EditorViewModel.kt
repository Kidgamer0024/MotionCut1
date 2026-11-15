package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ProjectRepository
import com.example.data.local.ProjectEntity
import com.example.data.remote.FirestoreProjectRepository
import com.example.engine.AiVideoToolsManager
import com.example.engine.AudioSynthesizerEngine
import com.example.engine.KeyframeEvaluator
import com.example.engine.PlaybackEngine
import com.example.engine.RecordedVoiceOver
import com.example.engine.VoiceOverRecorderEngine
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
import com.example.model.Track
import com.example.model.TrackType
import com.example.model.VideoEffect
import com.example.ui.components.ScopeMode
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class EditorToolTab(val label: String) {
    TIMELINE("Timeline"),
    KEYFRAMES("Keyframe Graph"),
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

    // Real-time Firestore listener with two-tier Flow error handling
    val cloudSyncState: StateFlow<CloudSyncState> = if (firestoreRepository != null && currentUserId != null) {
        firestoreRepository.observeCloudProjects(currentUserId)
            .onEach { cloudProjects ->
                val userCreatedProjects = cloudProjects.filterNot {
                    it.id in com.example.data.SampleProjects.SEEDED_SAMPLE_IDS
                }
                if (userCreatedProjects.isNotEmpty() || _initialSeedFinished.value) {
                    repository.syncFromCloud(userCreatedProjects.map { it.toRoomEntity() })
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
            }
        } else {
            _initialSeedFinished.value = true
        }
    }

    private fun syncCurrentProjectToCloud() {
        val repo = firestoreRepository ?: return
        val proj = currentProject.value
        viewModelScope.launch {
            repo.upsertCloudProject(proj)
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
        syncCurrentProjectToCloud()
    }

    fun updateKeyframeEasing(property: AnimatableProperty, keyframeId: String, easing: EasingType) {
        val clipId = _selectedClipId.value ?: return
        repository.updateKeyframeEasing(clipId, property, keyframeId, easing)
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

    fun addClip(clip: ClipItem) {
        val targetTrackId = clip.trackId
        repository.addClipToTrack(targetTrackId, clip)
        if (clip.type != com.example.model.ClipType.AUDIO) {
            _selectedClipId.value = clip.id
        }
        if (clip.type == com.example.model.ClipType.MODEL_3D) {
            _activeToolTab.value = EditorToolTab.MODEL_3D
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
        if (!show && voiceOverRecorderEngine.isRecording.value) {
            voiceOverRecorderEngine.stopRecording()
        }
    }

    fun importRecordedVoiceOverToTimeline(voiceOver: RecordedVoiceOver) {
        val playhead = playbackEngine.currentTimeMs.value
        repository.importVoiceOverToProject(voiceOver, playhead)
        setShowVoiceOverSheet(false)
        playbackEngine.seek(playhead + voiceOver.durationMs)
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
            _aiStatusMessage.value = "AI Upscaling to ${targetResolution.label}..."
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
                _aiSuccessFeedback.value = "Upscaled to ${targetResolution.label} — ${profile.clarityNote}"
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

    fun applyQuickRemoveBg() {
        val clipId = resolveTargetClipId()
        if (clipId == null) {
            openAiStudioAtTab(0)
            return
        }
        _selectedClipId.value = clipId
        val clip = currentProject.value.findClip(clipId) ?: return
        viewModelScope.launch {
            _isAiProcessing.value = true
            _aiStatusMessage.value = "Removing background on '${clip.title}' with Gemini AI..."
            try {
                val result = aiToolsManager.removeBackgroundAndGenerateMask(
                    clipTitle = clip.title,
                    targetSubject = "Primary Subject",
                    cutoutMode = "Transparent Cutout (Remove BG)"
                )
                repository.addMaskToClip(clipId, result.mask.copy(featherPx = 12f, expansion = 2f))
                _aiSuccessFeedback.value = "Removed background for '${result.subjectLabel}'"
                _undoToastMessage.value = "BG Removed: ${result.subjectLabel}"
                syncCurrentProjectToCloud()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiStatusMessage.value = "Remove BG error: ${e.message}"
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
            _aiStatusMessage.value = "Analyzing video frames with Gemini AI for '$subject'..."
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
