package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AspectRatio
import com.example.model.ColorGradingSettings
import com.example.model.ExportResolution
import com.example.model.TrackType
import com.example.ui.EditorToolTab
import com.example.ui.EditorViewModel
import com.example.ui.components.Add3DModelDialog
import com.example.ui.components.AddLayerDialog
import com.example.ui.components.AiStudioModal
import com.example.ui.components.ColorGradingMainPanel
import com.example.ui.components.EditorBottomBar
import com.example.ui.components.EditorDockOption
import com.example.ui.components.EffectLibraryDialog
import com.example.ui.components.EffectsStackEditor
import com.example.ui.components.ExportRenderDialog
import com.example.ui.components.KeyframeGraphEditor
import com.example.ui.components.LayoutConfigDialog
import com.example.ui.components.LibraryHubDialog
import com.example.ui.components.MarketplaceSheet
import com.example.ui.components.MaskingToolEditor
import com.example.ui.components.Model3DEditorPanel
import com.example.ui.components.MusicLibrarySheet
import com.example.ui.components.ScopesVisualizer
import com.example.ui.components.SplitIntoPartsDialog
import com.example.ui.components.TimelineView
import com.example.ui.components.UpscaleResolutionDialog
import com.example.ui.components.VideoMonitorCanvas
import com.example.ui.components.VoiceOverRecordSheet
import com.example.ui.theme.CutRose
import com.example.ui.theme.KeyframeViolet
import com.example.ui.theme.MotionCyan
import com.example.ui.theme.PlayheadAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioDarkBg
import com.example.ui.theme.StudioDivider
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant

@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier,
    onNavigateToLanding: (() -> Unit)? = null
) {
    if (onNavigateToLanding != null) {
        androidx.activity.compose.BackHandler {
            onNavigateToLanding()
        }
    }
    val project by viewModel.currentProject.collectAsState()
    val allProjects by viewModel.allProjects.collectAsState()
    val currentTimeMs by viewModel.playbackEngine.currentTimeMs.collectAsState()
    val isPlaying by viewModel.playbackEngine.isPlaying.collectAsState()

    val selectedClipId by viewModel.selectedClipId.collectAsState()
    val activeToolTab by viewModel.activeToolTab.collectAsState()
    val activeProperty by viewModel.activeProperty.collectAsState()

    val showScopes by viewModel.showScopes.collectAsState()
    val selectedScope by viewModel.selectedScope.collectAsState()
    val showExportDialog by viewModel.showExportDialog.collectAsState()
    val showAddLayerDialog by viewModel.showAddLayerDialog.collectAsState()
    val showProjectPicker by viewModel.showProjectPicker.collectAsState()

    val showMarketplaceSheet by viewModel.showMarketplaceSheet.collectAsState()
    val showMusicLibrarySheet by viewModel.showMusicLibrarySheet.collectAsState()
    val marketplaceItems by viewModel.marketplaceItems.collectAsState()
    val musicTracks by viewModel.musicTracks.collectAsState()
    val isPlayingMusicPreview by viewModel.audioSynthesizerEngine.isPlayingPreview.collectAsState()
    val currentMusicPreviewTrackId by viewModel.audioSynthesizerEngine.currentTrackId.collectAsState()
    val musicPreviewProgress by viewModel.audioSynthesizerEngine.previewProgress.collectAsState()

    val showAiStudioModal by viewModel.showAiStudioModal.collectAsState()
    val aiStudioInitialTab by viewModel.aiStudioInitialTab.collectAsState()
    val isAiProcessing by viewModel.isAiProcessing.collectAsState()
    val aiStatusMessage by viewModel.aiStatusMessage.collectAsState()
    val aiSuccessFeedback by viewModel.aiSuccessFeedback.collectAsState()

    val showLibraryHubSheet by viewModel.showLibraryHubSheet.collectAsState()
    val showEffectLibrarySheet by viewModel.showEffectLibrarySheet.collectAsState()
    val favoriteEffectIds by viewModel.favoriteEffectIds.collectAsState()
    val showLayoutDialog by viewModel.showLayoutDialog.collectAsState()
    val showSplitPartsDialog by viewModel.showSplitPartsDialog.collectAsState()
    val showUpscaleDialog by viewModel.showUpscaleDialog.collectAsState()

    var showAdd3DModelDialog by remember { mutableStateOf(false) }

    val showVoiceOverSheet by viewModel.showVoiceOverSheet.collectAsState()

    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val lastUndoDesc by viewModel.lastUndoDescription.collectAsState()
    val lastRedoDesc by viewModel.lastRedoDescription.collectAsState()
    val undoToastMessage by viewModel.undoToastMessage.collectAsState()

    val resolvedTargetClipId = viewModel.resolveTargetClipId()
    val resolvedTargetClip = resolvedTargetClipId?.let { project.findClip(it) }
    val selectedClip = selectedClipId?.let { project.findClip(it) }

    LaunchedEffect(undoToastMessage) {
        if (undoToastMessage != null) {
            kotlinx.coroutines.delay(2200L)
            viewModel.clearUndoToast()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = StudioDarkBg,
        bottomBar = {
            EditorBottomBar(
                isSelectedClipLinked = resolvedTargetClip?.isLocked == true,
                is4kUpscaled = project.resolution == ExportResolution.UHD_4K,
                isAiProcessing = isAiProcessing,
                onOptionClick = { option ->
                    when (option) {
                        EditorDockOption.ADD -> viewModel.setShowAddLayer(true)
                        EditorDockOption.AI -> viewModel.openAiStudioAtTab(0)
                        EditorDockOption.LIBRARY -> viewModel.setShowLibraryHub(true)
                        EditorDockOption.CAPTIONS -> viewModel.openAiStudioAtTab(2)
                        EditorDockOption.LINK -> viewModel.toggleSelectedClipLink()
                        EditorDockOption.LAYOUT -> viewModel.setShowLayoutDialog(true)
                        EditorDockOption.REMOVE_BG -> viewModel.applyQuickRemoveBg()
                        EditorDockOption.SPLIT_PARTS -> viewModel.setShowSplitPartsDialog(true)
                        EditorDockOption.UPSCALE -> viewModel.setShowUpscaleDialog(true)
                        EditorDockOption.COLORIZE -> viewModel.openAiStudioAtTab(1)
                        EditorDockOption.DEPTH_CLOUD -> viewModel.applyCloudDepthPass()
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
            // 1. Studio Top Navigation Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                color = StudioSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Project Selector Pill + Global Undo / Redo Cluster
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (onNavigateToLanding != null) {
                            Surface(
                                color = StudioSurfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
                                modifier = Modifier
                                    .clickable { onNavigateToLanding() }
                                    .testTag("back_to_landing_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = "Back to Projects",
                                        tint = MotionCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Projects",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                        Surface(
                            color = StudioSurfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
                            modifier = Modifier
                                .clickable { viewModel.setShowProjectPicker(true) }
                                .testTag("project_picker_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MotionCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = project.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    fontSize = 11.sp
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Select Project",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        // Undo & Redo Quick Action Cluster
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Surface(
                                color = if (canUndo) StudioSurfaceVariant else Color.Transparent,
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (canUndo) StudioBorder else Color.Transparent),
                                modifier = Modifier
                                    .clickable(enabled = canUndo) { viewModel.undo() }
                                    .testTag("global_undo_button")
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Undo,
                                        contentDescription = lastUndoDesc?.let { "Undo $it" } ?: "Undo",
                                        tint = if (canUndo) Color.White else Color.Gray.copy(alpha = 0.35f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Surface(
                                color = if (canRedo) StudioSurfaceVariant else Color.Transparent,
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (canRedo) StudioBorder else Color.Transparent),
                                modifier = Modifier
                                    .clickable(enabled = canRedo) { viewModel.redo() }
                                    .testTag("global_redo_button")
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Redo,
                                        contentDescription = lastRedoDesc?.let { "Redo $it" } ?: "Redo",
                                        tint = if (canRedo) Color.White else Color.Gray.copy(alpha = 0.35f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Hub Buttons (AI Magic, Voice-over, Marketplace & Music Library) + Export Action
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // AI Magic Studio Button
                        Surface(
                            color = MotionCyan.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MotionCyan),
                            modifier = Modifier
                                .clickable { viewModel.setShowAiStudioModal(true) }
                                .testTag("open_ai_studio_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MotionCyan, modifier = Modifier.size(13.dp))
                                Text("AI Magic", color = MotionCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Voiceover Button
                        Surface(
                            color = CutRose.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CutRose.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .clickable { viewModel.setShowVoiceOverSheet(true) }
                                .testTag("open_top_voiceover_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = CutRose, modifier = Modifier.size(13.dp))
                                Text("Voice", color = CutRose, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Marketplace Hub Button
                        Surface(
                            color = KeyframeViolet.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, KeyframeViolet.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .clickable { viewModel.setShowMarketplace(true) }
                                .testTag("open_marketplace_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Default.Storefront, contentDescription = null, tint = KeyframeViolet, modifier = Modifier.size(13.dp))
                                Text("Market", color = KeyframeViolet, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Music Library Button
                        Surface(
                            color = PlayheadAmber.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PlayheadAmber.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .clickable { viewModel.setShowMusicLibrary(true) }
                                .testTag("open_music_library_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = PlayheadAmber, modifier = Modifier.size(13.dp))
                                Text("Music", color = PlayheadAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Export Button
                        Button(
                            onClick = { viewModel.setShowExport(true) },
                            modifier = Modifier
                                .height(30.dp)
                                .testTag("open_export_dialog_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MotionCyan,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Export", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }

            // 2. Video Monitor Canvas (Preview Monitor with Direct Manipulation & Masks)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.05f)
            ) {
                VideoMonitorCanvas(
                    project = project,
                    currentTimeMs = currentTimeMs,
                    selectedClipId = selectedClipId,
                    isPlaying = isPlaying,
                    showScopes = showScopes,
                    onTogglePlay = { viewModel.togglePlay() },
                    onStepFrame = { forward -> viewModel.stepFrame(forward) },
                    onToggleScopes = { viewModel.toggleScopes() },
                    onUpdateClipTransform = { clipId, panX, panY, zoom, rot ->
                        viewModel.updateClipTransform(clipId, panX, panY, zoom, rot)
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Pro Scopes floating HUD overlay
                if (showScopes) {
                    val activeGrading = selectedClip?.colorGrading ?: project.masterColorGrading
                    ScopesVisualizer(
                        colorGradingSettings = activeGrading,
                        selectedScope = selectedScope,
                        onSelectScope = { viewModel.setScopeMode(it) },
                        onClose = { viewModel.toggleScopes() },
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp)
                    )
                }
            }

            // 3. Mode Switcher Tabs (Timeline, Keyframe Graph, Color Grading, FX Stack, Masking)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                color = StudioSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder)
            ) {
                ScrollableTabRow(
                    selectedTabIndex = activeToolTab.ordinal,
                    containerColor = StudioSurface,
                    contentColor = MotionCyan,
                    edgePadding = 8.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[activeToolTab.ordinal]),
                            color = when (activeToolTab) {
                                EditorToolTab.KEYFRAMES -> KeyframeViolet
                                EditorToolTab.MASKING -> MotionCyan
                                else -> MotionCyan
                            },
                            height = 2.dp
                        )
                    },
                    divider = {}
                ) {
                    EditorToolTab.values().forEach { tab ->
                        val isSelected = tab == activeToolTab
                        Tab(
                            selected = isSelected,
                            onClick = { viewModel.setToolTab(tab) },
                            modifier = Modifier.testTag("tab_${tab.name.lowercase()}"),
                            text = {
                                Text(
                                    text = tab.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) {
                                        if (tab == EditorToolTab.KEYFRAMES) KeyframeViolet else MotionCyan
                                    } else Color.LightGray
                                )
                            }
                        )
                    }
                }
            }

            // 4. Mode Content Workspace
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.15f)
                    .background(StudioDarkBg)
            ) {
                when (activeToolTab) {
                    EditorToolTab.TIMELINE -> {
                        TimelineView(
                            project = project,
                            currentTimeMs = currentTimeMs,
                            selectedClipId = selectedClipId,
                            activeProperty = activeProperty,
                            canUndo = canUndo,
                            canRedo = canRedo,
                            onUndo = { viewModel.undo() },
                            onRedo = { viewModel.redo() },
                            onSeek = { viewModel.seek(it) },
                            onSelectClip = { viewModel.selectClip(it) },
                            onSelectProperty = { viewModel.setActiveProperty(it) },
                            onSplitClip = { id, time -> viewModel.splitClip(id, time) },
                            onDuplicateClip = { id -> viewModel.duplicateClip(id) },
                            onDeleteClip = { id -> viewModel.deleteClip(id) },
                            onToggleKeyframe = { viewModel.toggleKeyframeOnSelectedClip() },
                            onAddClipRequested = { viewModel.setShowAddLayer(true) },
                            onRecordVoiceOverRequested = { viewModel.setShowVoiceOverSheet(true) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    EditorToolTab.KEYFRAMES -> {
                        if (selectedClip != null) {
                            KeyframeGraphEditor(
                                clip = selectedClip,
                                currentTimeMs = currentTimeMs,
                                selectedProperty = activeProperty,
                                onSelectProperty = { viewModel.setActiveProperty(it) },
                                onToggleKeyframe = { viewModel.toggleKeyframeOnSelectedClip() },
                                onJumpToKeyframe = { next -> viewModel.jumpToAdjacentKeyframe(next) },
                                onSeekInClip = { timeMs -> viewModel.seek(timeMs) },
                                onUpdateEasing = { prop, kfId, easing ->
                                    viewModel.updateKeyframeEasing(prop, kfId, easing)
                                },
                                onUpdateValue = { prop, newVal ->
                                    viewModel.updatePropertyValue(prop, newVal)
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Select a clip on the timeline to edit keyframe curves",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                    EditorToolTab.COLOR_GRADING -> {
                        val activeGrading = selectedClip?.colorGrading ?: project.masterColorGrading
                        ColorGradingMainPanel(
                            settings = activeGrading,
                            onUpdateSettings = { viewModel.updateColorGrading(it) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    EditorToolTab.EFFECTS -> {
                        val effectsList = selectedClip?.effects ?: emptyList()
                        EffectsStackEditor(
                            effects = effectsList,
                            onAddEffect = { viewModel.addEffect(it) },
                            onUpdateEffect = { viewModel.updateEffect(it) },
                            onDeleteEffect = { viewModel.deleteEffect(it) },
                            onOpenEffectLibrary = { viewModel.setShowEffectLibrary(true) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    EditorToolTab.MASKING -> {
                        if (selectedClip != null) {
                            MaskingToolEditor(
                                clip = selectedClip,
                                currentTimeMs = currentTimeMs,
                                onAddMask = { viewModel.addMaskToSelectedClip(it) },
                                onUpdateMask = { viewModel.updateMaskOnSelectedClip(it) },
                                onDeleteMask = { viewModel.deleteMaskFromSelectedClip(it) },
                                onToggleMaskKeyframe = { viewModel.toggleMaskKeyframeOnSelectedClip(it) },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Select a clip on the timeline to configure masks",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                    EditorToolTab.MODEL_3D -> {
                        Model3DEditorPanel(
                            selectedClip = selectedClip,
                            currentTimeMs = currentTimeMs,
                            onUpdateModel3D = { clipId, cfg -> viewModel.updateClipModel3D(clipId, cfg) },
                            onUpdateKeyframeProperty = { prop, newVal -> viewModel.updatePropertyValue(prop, newVal) },
                            onToggleKeyframeProperty = { prop -> viewModel.toggleKeyframeForProperty(prop) },
                            onOpenAdd3DModal = { showAdd3DModelDialog = true },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // Floating Undo / Redo Toast HUD
        AnimatedVisibility(
                visible = undoToastMessage != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -20 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -20 }),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 54.dp)
            ) {
                if (undoToastMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = StudioCardBg.copy(alpha = 0.95f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MotionCyan.copy(alpha = 0.6f)),
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .clickable { viewModel.clearUndoToast() },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (undoToastMessage!!.startsWith("Undone")) Icons.AutoMirrored.Filled.Undo else Icons.AutoMirrored.Filled.Redo,
                                contentDescription = null,
                                tint = MotionCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = undoToastMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Floating AI Processing HUD (for bottom bar quick AI triggers like Remove BG & Depth Cloud)
            AnimatedVisibility(
                visible = isAiProcessing && !showAiStudioModal && !showUpscaleDialog,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -20 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -20 }),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 92.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = StudioCardBg.copy(alpha = 0.96f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, KeyframeViolet),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = MotionCyan
                        )
                        Text(
                            text = aiStatusMessage ?: "Processing with Gemini AI...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Dialogs & Sheets
        if (showExportDialog) {
            ExportRenderDialog(
                project = project,
                onDismiss = { viewModel.setShowExport(false) }
            )
        }

        if (showAddLayerDialog) {
            AddLayerDialog(
                currentTimeMs = currentTimeMs,
                onAddClip = { clip ->
                    viewModel.addClip(clip)
                    viewModel.setShowAddLayer(false)
                },
                onDismiss = {
                    viewModel.stopMusicPreview()
                    viewModel.setShowAddLayer(false)
                },
                audioSynthesizerEngine = viewModel.audioSynthesizerEngine,
                isPlayingPreview = isPlayingMusicPreview,
                currentPreviewTrackId = currentMusicPreviewTrackId,
                onOpenFullMusicLibrary = {
                    viewModel.setShowAddLayer(false)
                    viewModel.setShowMusicLibrary(true)
                },
                onOpenVoiceRecorder = {
                    viewModel.setShowAddLayer(false)
                    viewModel.setShowVoiceOverSheet(true)
                }
            )
        }

        if (showProjectPicker) {
            ProjectListDialog(
                projects = allProjects,
                currentProjectId = project.id,
                onSelectProject = { viewModel.selectProject(it) },
                onCreateProject = { title, ratio -> viewModel.createProject(title, ratio) },
                onDismiss = { viewModel.setShowProjectPicker(false) }
            )
        }

        if (showMarketplaceSheet) {
            MarketplaceSheet(
                items = marketplaceItems,
                onDownloadItem = { viewModel.downloadAndApplyMarketplaceItem(it) },
                onAddReview = { itemId, review -> viewModel.addMarketplaceReview(itemId, review) },
                onPublishCreation = { viewModel.publishCreation(it) },
                onDismiss = { viewModel.setShowMarketplace(false) }
            )
        }

        if (showMusicLibrarySheet) {
            MusicLibrarySheet(
                tracks = musicTracks,
                isPlayingPreview = isPlayingMusicPreview,
                currentPreviewTrackId = currentMusicPreviewTrackId,
                previewProgress = musicPreviewProgress,
                onPlayPreview = { viewModel.playMusicPreview(it) },
                onStopPreview = { viewModel.stopMusicPreview() },
                onImportTrack = { viewModel.importMusicTrackToTimeline(it) },
                onDismiss = { viewModel.setShowMusicLibrary(false) }
            )
        }

        if (showAiStudioModal) {
            AiStudioModal(
                selectedClip = selectedClip ?: resolvedTargetClip,
                initialTab = aiStudioInitialTab,
                isProcessing = isAiProcessing,
                statusMessage = aiStatusMessage,
                successFeedback = aiSuccessFeedback,
                onApplyCutout = { clipId, sub, mode, f, exp ->
                    viewModel.applyAiCutout(clipId, sub, mode, f, exp)
                },
                onApplyColorGrade = { clipId, mood ->
                    viewModel.applyAiColorGrade(clipId, mood)
                },
                onApplyCaptions = { desc, style ->
                    viewModel.applyAiCaptions(desc, style)
                },
                onClearFeedback = { viewModel.clearAiFeedback() },
                onDismiss = { viewModel.setShowAiStudioModal(false) }
            )
        }

        if (showLibraryHubSheet) {
            val mainTrackId = project.tracks.find { it.type == TrackType.MAIN_VIDEO }?.id
                ?: project.tracks.firstOrNull()?.id
                ?: "track_main_video"
            LibraryHubDialog(
                mainVideoTrackId = mainTrackId,
                currentTimeMs = currentTimeMs,
                onOpenMarketplace = { viewModel.setShowMarketplace(true) },
                onOpenMusicLibrary = { viewModel.setShowMusicLibrary(true) },
                onOpenEffectLibrary = { viewModel.setShowEffectLibrary(true) },
                onInsertStockClip = { clip -> viewModel.addClip(clip) },
                onDismiss = { viewModel.setShowLibraryHub(false) }
            )
        }

        if (showEffectLibrarySheet) {
            EffectLibraryDialog(
                favoriteIds = favoriteEffectIds,
                onToggleFavorite = { viewModel.toggleFavoriteEffect(it) },
                onSelectEffect = { preset ->
                    viewModel.addEffectPreset(preset)
                    viewModel.setShowEffectLibrary(false)
                },
                onDismiss = { viewModel.setShowEffectLibrary(false) }
            )
        }

        if (showAdd3DModelDialog) {
            Add3DModelDialog(
                currentTimeMs = currentTimeMs,
                onCreate3DClip = { clip3d ->
                    viewModel.addClip(clip3d)
                    showAdd3DModelDialog = false
                },
                onDismiss = { showAdd3DModelDialog = false }
            )
        }

        if (showLayoutDialog) {
            LayoutConfigDialog(
                currentAspectRatio = project.aspectRatio,
                selectedClipTitle = resolvedTargetClip?.title,
                onApplyLayout = { ratio, preset, scale, offX, offY ->
                    viewModel.applyProjectLayout(ratio, preset, scale, offX, offY)
                },
                onDismiss = { viewModel.setShowLayoutDialog(false) }
            )
        }

        if (showSplitPartsDialog) {
            SplitIntoPartsDialog(
                targetClipTitle = resolvedTargetClip?.title,
                targetClipDurationMs = resolvedTargetClip?.durationMs,
                onSplitAtPlayhead = { viewModel.splitActiveClipAtPlayhead() },
                onSplitIntoParts = { parts -> viewModel.splitActiveClipIntoParts(parts) },
                onDismiss = { viewModel.setShowSplitPartsDialog(false) }
            )
        }

        if (showUpscaleDialog) {
            UpscaleResolutionDialog(
                currentResolution = project.resolution,
                isProcessing = isAiProcessing,
                statusMessage = aiStatusMessage,
                onApplyUpscale = { targetRes -> viewModel.applyAiUpscale(targetRes) },
                onDismiss = { viewModel.setShowUpscaleDialog(false) }
            )
        }

        if (showVoiceOverSheet) {
            VoiceOverRecordSheet(
                recorderEngine = viewModel.voiceOverRecorderEngine,
                playheadTimeMs = currentTimeMs,
                onImportVoiceOver = { voiceOver ->
                    viewModel.importRecordedVoiceOverToTimeline(voiceOver)
                },
                onDismiss = { viewModel.setShowVoiceOverSheet(false) }
            )
        }
    }
}

@Composable
private fun ProjectListDialog(
    projects: List<com.example.model.Project>,
    currentProjectId: String,
    onSelectProject: (String) -> Unit,
    onCreateProject: (String, AspectRatio) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, StudioBorder, RoundedCornerShape(16.dp)),
            color = StudioDarkBg
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "MotionCut Projects",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                projects.forEach { proj ->
                    val isSel = proj.id == currentProjectId
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, if (isSel) MotionCyan else StudioBorder, RoundedCornerShape(8.dp))
                            .clickable { onSelectProject(proj.id) },
                        color = if (isSel) MotionCyan.copy(alpha = 0.2f) else StudioSurfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = proj.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSel) MotionCyan else Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${proj.aspectRatio.ratioText} • ${proj.durationMs / 1000}s • ${proj.tracks.size} Tracks",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        onCreateProject("New Cinematic Edit", AspectRatio.PORTRAIT_9_16)
                    },
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MotionCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("+ Create New Project", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
