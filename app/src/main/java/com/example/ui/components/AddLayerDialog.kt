package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ControlCamera
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Rectangle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.DeviceAudioItem
import com.example.data.DeviceMediaItem
import com.example.data.DeviceMediaScanner
import com.example.data.SampleMusicLibrary
import com.example.engine.AudioSynthesizerEngine
import com.example.model.AnimatableProperty
import com.example.model.ClipItem
import com.example.model.ClipType
import com.example.model.EasingType
import com.example.model.EffectType
import com.example.model.Keyframe
import com.example.model.KeyframeTrack
import com.example.model.ShapeConfig
import com.example.model.ShapeType
import com.example.model.TextConfig
import com.example.model.TransformState
import com.example.model.VideoEffect
import com.example.ui.theme.KeyframeViolet
import com.example.ui.theme.MotionCyan
import com.example.ui.theme.PlayheadAmber
import kotlinx.coroutines.launch

private val AddPanelBg = Color(0xFF0D1D1E)
private val AddRailBg = Color(0xFF0A1718)
private val AddCardTileBg = Color(0xFF19262C)
private val AddDividerColor = Color(0xFF1B3032)
private val AddEmerald = Color(0xFF10E878)
private val AddCoralRed = Color(0xFFFF4D3D)
private val AddSlateText = Color(0xFFA0B0BA)

private enum class AddCategoryTab(val label: String, val icon: ImageVector) {
    MEDIA("Media", Icons.Default.PermMedia),
    AUDIO("Audio", Icons.Default.MusicNote),
    SHAPE("Shape", Icons.Default.Category),
    ELEMENT("Element", Icons.Default.FilterVintage),
    TEMPLATES("Templates", Icons.Default.AutoAwesome)
}

private enum class InlineRightTool {
    NONE,
    TEXT,
    DRAW
}

private enum class MediaFilterMode(val label: String) {
    ALL("All"),
    PHOTOS("Photos"),
    VIDEOS("Videos"),
    AUDIOS("Audios")
}

private data class MotionTemplatePreset(
    val id: String,
    val title: String,
    val categoryTag: String,
    val description: String,
    val isAdjustment: Boolean = false,
    val effectType: EffectType? = null,
    val textColor: Long = 0xFFFFFFFF,
    val glowStrokeColor: Long = 0xFF00E5FF
)

@Composable
fun AddLayerDialog(
    currentTimeMs: Long,
    onAddClip: (ClipItem) -> Unit,
    onDismiss: () -> Unit,
    audioSynthesizerEngine: AudioSynthesizerEngine? = null,
    isPlayingPreview: Boolean = false,
    currentPreviewTrackId: String? = null,
    onOpenFullMusicLibrary: () -> Unit = {},
    onOpenVoiceRecorder: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf(AddCategoryTab.MEDIA) }
    var inlineTool by remember { mutableStateOf(InlineRightTool.NONE) }
    var mediaFilter by remember { mutableStateOf(MediaFilterMode.ALL) }

    var deviceMedia by remember { mutableStateOf<List<DeviceMediaItem>>(emptyList()) }
    var deviceAudio by remember { mutableStateOf<List<DeviceAudioItem>>(emptyList()) }
    var audioSearchQuery by remember { mutableStateOf("") }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    var showSolidSwatches by remember { mutableStateOf(false) }

    // Load device MediaStore + persisted + studio media and audio
    LaunchedEffect(refreshTrigger) {
        deviceMedia = DeviceMediaScanner.loadDeviceAndStudioMedia(context)
        deviceAudio = DeviceMediaScanner.loadDeviceAndStudioAudio(context)
    }

    // Play-Policy Compliant Android Photo Picker (Zero storage permission needed)
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            DeviceMediaScanner.persistPickedMediaUris(context, uris)
            refreshTrigger++
        }
    }

    // Play-Policy Compliant Storage Access Framework Audio Picker
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            DeviceMediaScanner.persistPickedAudioUris(context, uris)
            refreshTrigger++
        }
    }

    // PSD / Layered Asset Document Picker
    val psdPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            DeviceMediaScanner.persistPickedMediaUris(context, listOf(uri))
            refreshTrigger++
        }
    }

    var show3DCreatorDialog by remember { mutableStateOf(false) }

    // Play-Policy Compliant Files Directory Picker (Photos / Videos / Audios folders)
    val folderTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri != null) {
            DeviceMediaScanner.persistTreeUri(context, treeUri)
            refreshTrigger++
        }
    }

    Dialog(
        onDismissRequest = {
            audioSynthesizerEngine?.stop()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = null,
                    indication = null,
                    onClick = {
                        audioSynthesizerEngine?.stop()
                        onDismiss()
                    }
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 380.dp, max = 500.dp)
                    .clickable(interactionSource = null, indication = null, onClick = {})
                    .testTag("add_layer_sheet"),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                color = AddPanelBg,
                border = BorderStroke(1.dp, AddDividerColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    // LEFT MAIN PANEL (Top Tabs + Category Content)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        // Top 5 Category Tabs (Media, Audio, Shape, Element, Templates)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AddRailBg)
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AddCategoryTab.values().forEach { tab ->
                                val isSel = inlineTool == InlineRightTool.NONE && activeTab == tab
                                val tint = if (isSel) AddEmerald else AddSlateText
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable(role = Role.Tab) {
                                            inlineTool = InlineRightTool.NONE
                                            activeTab = tab
                                            showSolidSwatches = false
                                        }
                                        .padding(vertical = 4.dp)
                                        .testTag("add_tab_${tab.name.lowercase()}")
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.label,
                                        tint = tint,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = tab.label,
                                        color = tint,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = AddDividerColor, thickness = 1.dp)

                        // Body Content
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(AddPanelBg)
                        ) {
                            when (inlineTool) {
                                InlineRightTool.TEXT -> {
                                    InlineTextCreatorPane(
                                        currentTimeMs = currentTimeMs,
                                        onAddTextClip = { clip ->
                                            onAddClip(clip)
                                            onDismiss()
                                        }
                                    )
                                }
                                InlineRightTool.DRAW -> {
                                    InlineDrawCreatorPane(
                                        currentTimeMs = currentTimeMs,
                                        onAddDrawClip = { clip ->
                                            onAddClip(clip)
                                            onDismiss()
                                        }
                                    )
                                }
                                InlineRightTool.NONE -> {
                                    when (activeTab) {
                                        AddCategoryTab.MEDIA -> {
                                            MediaCategoryContent(
                                                mediaItems = deviceMedia,
                                                audioItems = deviceAudio,
                                                filterMode = mediaFilter,
                                                onSelectFilter = { mediaFilter = it },
                                                onPickPsdOrFile = {
                                                    psdPickerLauncher.launch(arrayOf("image/*", "video/*"))
                                                },
                                                onPickDeviceMedia = {
                                                    mediaPickerLauncher.launch(
                                                        PickVisualMediaRequest(
                                                            ActivityResultContracts.PickVisualMedia.ImageAndVideo
                                                        )
                                                    )
                                                },
                                                onPickFilesFolder = {
                                                    folderTreeLauncher.launch(null)
                                                },
                                                onSelectMedia = { item ->
                                                    onAddClip(
                                                        ClipItem(
                                                            title = item.title,
                                                            type = ClipType.VIDEO_SAMPLE,
                                                            assetResId = item.sampleResId,
                                                            mediaUri = item.uriString,
                                                            colorHex = item.colorHex,
                                                            startTimeMs = currentTimeMs,
                                                            durationMs = item.durationMs.coerceIn(2000L, 15_000L)
                                                        )
                                                    )
                                                    onDismiss()
                                                },
                                                onSelectAudioFromMedia = { item ->
                                                    audioSynthesizerEngine?.stop()
                                                    onAddClip(
                                                        ClipItem(
                                                            title = item.title,
                                                            type = ClipType.AUDIO,
                                                            mediaUri = item.uriString,
                                                            colorHex = item.accentColorHex,
                                                            startTimeMs = currentTimeMs,
                                                            durationMs = item.durationMs.coerceIn(2000L, 15_000L),
                                                            waveformSamples = item.waveformSamples
                                                        )
                                                    )
                                                    onDismiss()
                                                }
                                            )
                                        }

                                        AddCategoryTab.AUDIO -> {
                                            AudioCategoryContent(
                                                audioItems = deviceAudio,
                                                searchQuery = audioSearchQuery,
                                                onSearchChange = { audioSearchQuery = it },
                                                isPlayingPreview = isPlayingPreview,
                                                currentPreviewId = currentPreviewTrackId,
                                                onTogglePreview = { audioItem ->
                                                    val engine = audioSynthesizerEngine ?: return@AudioCategoryContent
                                                    if (audioItem.uriString != null) {
                                                        engine.playAudioUri(
                                                            context = context,
                                                            id = audioItem.id,
                                                            uriString = audioItem.uriString,
                                                            durationMs = audioItem.durationMs
                                                        )
                                                    } else {
                                                        val matchTrack = SampleMusicLibrary.getAllTracks()
                                                            .find { "studio_${it.id}" == audioItem.id }
                                                        if (matchTrack != null) {
                                                            engine.playTrack(matchTrack)
                                                        }
                                                    }
                                                },
                                                onOpenMusicLibrary = {
                                                    audioSynthesizerEngine?.stop()
                                                    onDismiss()
                                                    onOpenFullMusicLibrary()
                                                },
                                                onOpenVoiceRecord = {
                                                    audioSynthesizerEngine?.stop()
                                                    onDismiss()
                                                    onOpenVoiceRecorder()
                                                },
                                                onBrowseDeviceAudio = {
                                                    audioPickerLauncher.launch(arrayOf("audio/*"))
                                                },
                                                onBrowseAudioFolder = {
                                                    folderTreeLauncher.launch(null)
                                                },
                                                onImportAudio = { item ->
                                                    audioSynthesizerEngine?.stop()
                                                    onAddClip(
                                                        ClipItem(
                                                            title = item.title,
                                                            type = ClipType.AUDIO,
                                                            mediaUri = item.uriString,
                                                            colorHex = item.accentColorHex,
                                                            startTimeMs = currentTimeMs,
                                                            durationMs = item.durationMs.coerceIn(2000L, 15_000L),
                                                            waveformSamples = item.waveformSamples
                                                        )
                                                    )
                                                    onDismiss()
                                                }
                                            )
                                        }

                                        AddCategoryTab.SHAPE -> {
                                            ShapeCategoryContent(
                                                onSelectShape = { shapeType ->
                                                    onAddClip(
                                                        ClipItem(
                                                            title = shapeType.label,
                                                            type = ClipType.SHAPE,
                                                            startTimeMs = currentTimeMs,
                                                            durationMs = 5000L,
                                                            shapeConfig = ShapeConfig(
                                                                type = shapeType,
                                                                fillColor = 0xFFD9DEE4,
                                                                strokeColor = 0xFF10E878,
                                                                strokeWidth = 3f
                                                            )
                                                        )
                                                    )
                                                    onDismiss()
                                                }
                                            )
                                        }

                                        AddCategoryTab.ELEMENT -> {
                                            ElementCategoryContent(
                                                showSolidSwatches = showSolidSwatches,
                                                onToggleSolidSwatches = { showSolidSwatches = !showSolidSwatches },
                                                onSelectSolidColor = { name, hex ->
                                                    onAddClip(
                                                        ClipItem(
                                                            title = name,
                                                            type = ClipType.COLOR_BG,
                                                            colorHex = hex,
                                                            startTimeMs = currentTimeMs,
                                                            durationMs = 5000L
                                                        )
                                                    )
                                                    onDismiss()
                                                },
                                                onSelectElement = { elementTitle, clip ->
                                                    onAddClip(clip.copy(title = elementTitle, startTimeMs = currentTimeMs))
                                                    onDismiss()
                                                },
                                                onOpen3DCreator = { show3DCreatorDialog = true }
                                            )
                                        }

                                        AddCategoryTab.TEMPLATES -> {
                                            TemplatesCategoryContent(
                                                onSelectTemplate = { preset ->
                                                    val clip = buildClipFromTemplate(preset, currentTimeMs)
                                                    onAddClip(clip)
                                                    onDismiss()
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    VerticalDivider(color = AddDividerColor, thickness = 1.dp)

                    // RIGHT VERTICAL ACTION RAIL (Text, Draw, Adjustment, Null, Close)
                    Column(
                        modifier = Modifier
                            .width(78.dp)
                            .fillMaxHeight()
                            .background(AddRailBg)
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        RightRailActionButton(
                            tag = "right_rail_text",
                            icon = Icons.Default.Title,
                            label = "Text",
                            isActive = inlineTool == InlineRightTool.TEXT,
                            onClick = {
                                inlineTool = if (inlineTool == InlineRightTool.TEXT) InlineRightTool.NONE else InlineRightTool.TEXT
                            }
                        )
                        RightRailActionButton(
                            tag = "right_rail_draw",
                            icon = Icons.Default.Draw,
                            label = "Draw",
                            isActive = inlineTool == InlineRightTool.DRAW,
                            onClick = {
                                inlineTool = if (inlineTool == InlineRightTool.DRAW) InlineRightTool.NONE else InlineRightTool.DRAW
                            }
                        )
                        RightRailActionButton(
                            tag = "right_rail_adjustment",
                            icon = Icons.Default.FilterVintage,
                            label = "Adjustment",
                            onClick = {
                                onAddClip(
                                    ClipItem(
                                        title = "Adjustment Layer",
                                        type = ClipType.ADJUSTMENT,
                                        colorHex = 0xFF00E5FF,
                                        startTimeMs = currentTimeMs,
                                        durationMs = 5000L,
                                        effects = listOf(VideoEffect(type = EffectType.GLOW, intensity = 0.45f))
                                    )
                                )
                                onDismiss()
                            }
                        )
                        RightRailActionButton(
                            tag = "right_rail_null",
                            icon = Icons.Default.ControlCamera,
                            label = "Null",
                            onClick = {
                                onAddClip(
                                    ClipItem(
                                        title = "Null Transform",
                                        type = ClipType.NULL_OBJECT,
                                        colorHex = 0xFF00E5FF,
                                        startTimeMs = currentTimeMs,
                                        durationMs = 5000L
                                    )
                                )
                                onDismiss()
                            }
                        )
                        RightRailActionButton(
                            tag = "right_rail_close",
                            icon = Icons.Default.Close,
                            label = "Close",
                            onClick = {
                                audioSynthesizerEngine?.stop()
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }

    if (show3DCreatorDialog) {
        Add3DModelDialog(
            currentTimeMs = currentTimeMs,
            onCreate3DClip = { clip3d ->
                onAddClip(clip3d)
                show3DCreatorDialog = false
                onDismiss()
            },
            onDismiss = { show3DCreatorDialog = false }
        )
    }
}

@Composable
private fun RightRailActionButton(
    tag: String,
    icon: ImageVector,
    label: String,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    val tint = if (isActive) AddEmerald else Color.White
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .width(68.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isActive) AddEmerald.copy(alpha = 0.14f) else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 6.dp)
            .testTag(tag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = tint,
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MediaCategoryContent(
    mediaItems: List<DeviceMediaItem>,
    audioItems: List<DeviceAudioItem>,
    filterMode: MediaFilterMode,
    onSelectFilter: (MediaFilterMode) -> Unit,
    onPickPsdOrFile: () -> Unit,
    onPickDeviceMedia: () -> Unit,
    onPickFilesFolder: () -> Unit,
    onSelectMedia: (DeviceMediaItem) -> Unit,
    onSelectAudioFromMedia: (DeviceAudioItem) -> Unit
) {
    var showFilterMenu by remember { mutableStateOf(false) }

    val filtered = remember(mediaItems, filterMode) {
        when (filterMode) {
            MediaFilterMode.ALL -> mediaItems
            MediaFilterMode.VIDEOS -> mediaItems.filter { it.isVideo }
            MediaFilterMode.PHOTOS -> mediaItems.filter { !it.isVideo }
            MediaFilterMode.AUDIOS -> emptyList()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Left Filter Rail (PSD + Filter Dropdown + Folder Picker)
        Column(
            modifier = Modifier
                .width(64.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onPickPsdOrFile)
                    .padding(4.dp)
                    .testTag("media_psd_button")
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AddCardTileBg,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Layers, contentDescription = "PSD", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("PSD", color = AddSlateText, fontSize = 11.sp, maxLines = 1)
            }

            Box {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showFilterMenu = true }
                        .padding(4.dp)
                        .testTag("media_filter_button")
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (filterMode == MediaFilterMode.ALL) AddCardTileBg else AddEmerald.copy(alpha = 0.2f),
                        border = if (filterMode == MediaFilterMode.ALL) null else BorderStroke(1.dp, AddEmerald),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.GridView,
                                contentDescription = "Filter",
                                tint = if (filterMode == MediaFilterMode.ALL) Color.White else AddEmerald,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            filterMode.label,
                            color = if (filterMode == MediaFilterMode.ALL) AddSlateText else AddEmerald,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = AddSlateText, modifier = Modifier.size(14.dp))
                    }
                }

                DropdownMenu(
                    expanded = showFilterMenu,
                    onDismissRequest = { showFilterMenu = false }
                ) {
                    MediaFilterMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = when (mode) {
                                        MediaFilterMode.ALL -> "All Media"
                                        MediaFilterMode.PHOTOS -> "Photos Directory"
                                        MediaFilterMode.VIDEOS -> "Videos Directory"
                                        MediaFilterMode.AUDIOS -> "Audios Directory"
                                    },
                                    fontWeight = if (mode == filterMode) FontWeight.Bold else FontWeight.Normal,
                                    color = if (mode == filterMode) AddEmerald else Color.Unspecified
                                )
                            },
                            onClick = {
                                onSelectFilter(mode)
                                showFilterMenu = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Open Files Folder...", color = AddEmerald, fontWeight = FontWeight.SemiBold) },
                        onClick = {
                            showFilterMenu = false
                            onPickFilesFolder()
                        }
                    )
                }
            }
        }

        // Right 2-column Device Videos, Photos & Audios Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .testTag("add_media_grid"),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            // Card 1: Zero-permission Android Photo Picker + Files Folder Link
            item(span = { GridItemSpan(2) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = AddCardTileBg,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AddEmerald.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onPickDeviceMedia)
                            .testTag("pick_device_media_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Select Device Videos & Photos",
                                tint = AddEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Photos & Videos",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Pick from gallery",
                                    color = AddSlateText,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Surface(
                        color = AddCardTileBg,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AddDividerColor),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onPickFilesFolder)
                            .testTag("pick_media_folder_button")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreateNewFolder,
                                contentDescription = "Link Files Directory",
                                tint = AddEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Files Dir",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            if (filterMode == MediaFilterMode.AUDIOS) {
                items(audioItems, key = { "media_aud_${it.id}" }) { audio ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.22f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectAudioFromMedia(audio) }
                            .testTag("media_audio_tile_${audio.id}"),
                        color = Color(audio.accentColorHex).copy(alpha = 0.88f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AddDividerColor)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color.Black.copy(alpha = 0.55f),
                                modifier = Modifier.align(Alignment.TopStart)
                            ) {
                                Text(
                                    text = audio.folderName,
                                    color = AddEmerald,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = audio.title,
                                tint = Color.White,
                                modifier = Modifier
                                    .size(28.dp)
                                    .align(Alignment.Center)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = audio.title,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = DeviceMediaScanner.formatShortDuration(audio.durationMs),
                                    color = AddEmerald,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                items(filtered, key = { it.id }) { item ->
                    DeviceMediaGridTile(
                        item = item,
                        onClick = { onSelectMedia(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceMediaGridTile(
    item: DeviceMediaItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val loadedThumb by produceState<ImageBitmap?>(initialValue = null, key1 = item.uriString) {
        val uri = item.uriString
        if (uri != null) {
            value = DeviceMediaScanner.loadThumbnailBitmap(context, uri, item.isVideo)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.22f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("media_tile_${item.id}"),
        color = AddCardTileBg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, AddDividerColor)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                loadedThumb != null -> {
                    Image(
                        bitmap = loadedThumb!!,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                item.sampleResId != null -> {
                    Image(
                        painter = painterResource(id = item.sampleResId),
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(item.colorHex)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (item.isVideo) Icons.Default.Videocam else Icons.Default.PermMedia,
                            contentDescription = null,
                            tint = AddSlateText,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }

            if (item.folderName.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.62f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(5.dp)
                ) {
                    Text(
                        text = item.folderName,
                        color = if (item.isDeviceFile) AddEmerald else AddSlateText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            // Bottom gradient + metadata label
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (item.isVideo) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = DeviceMediaScanner.formatShortDuration(item.durationMs),
                            color = AddEmerald,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioCategoryContent(
    audioItems: List<DeviceAudioItem>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    isPlayingPreview: Boolean,
    currentPreviewId: String?,
    onTogglePreview: (DeviceAudioItem) -> Unit,
    onOpenMusicLibrary: () -> Unit,
    onOpenVoiceRecord: () -> Unit,
    onBrowseDeviceAudio: () -> Unit,
    onBrowseAudioFolder: () -> Unit,
    onImportAudio: (DeviceAudioItem) -> Unit
) {
    val filtered = remember(audioItems, searchQuery) {
        if (searchQuery.isBlank()) audioItems
        else audioItems.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                it.artist.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Rounded Pill Search Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(22.dp),
            color = AddCardTileBg
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = AddSlateText, modifier = Modifier.size(20.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search song or artist",
                            color = AddSlateText,
                            fontSize = 13.sp
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                        cursorBrush = SolidColor(AddEmerald),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("audio_search_input")
                    )
                }
            }
        }

        // Scrollable grid/rows matching Screenshot 4: Music circle, Record circle, All count tile + Device/Audio Cards
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clickable(onClick = onOpenMusicLibrary)
                                .testTag("audio_music_button")
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = AddEmerald,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Search, contentDescription = "Music", tint = Color(0xFF06150D), modifier = Modifier.size(26.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Music", color = Color.White, fontSize = 12.sp)
                        }
                    }

                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clickable(onClick = onOpenVoiceRecord)
                                .testTag("audio_record_button")
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = AddCoralRed,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Mic, contentDescription = "Record", tint = Color.White, modifier = Modifier.size(26.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Record", color = Color.White, fontSize = 12.sp)
                        }
                    }

                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clickable(onClick = onBrowseDeviceAudio)
                                .testTag("audio_browse_device_button")
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = AddCardTileBg,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.AudioFile, contentDescription = "Device Audio", tint = Color.White, modifier = Modifier.size(24.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("All", color = Color.White, fontSize = 12.sp)
                            Text("${filtered.size}", color = AddSlateText, fontSize = 11.sp)
                        }
                    }

                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clickable(onClick = onBrowseAudioFolder)
                                .testTag("audio_folder_button")
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = AddCardTileBg,
                                border = BorderStroke(1.dp, AddEmerald.copy(alpha = 0.45f)),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.CreateNewFolder, contentDescription = "Audios Folder", tint = AddEmerald, modifier = Modifier.size(24.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Audios Dir", color = AddEmerald, fontSize = 11.sp)
                        }
                    }
                }
            }

            // 2-column Audio cards with Play toggle, duration badge & title
            items(filtered.chunked(2)) { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { audioItem ->
                        val isPlayingThis = isPlayingPreview && currentPreviewId == audioItem.id
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onImportAudio(audioItem) }
                                .testTag("audio_item_${audioItem.id}")
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(audioItem.accentColorHex),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(74.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp)
                                ) {
                                    // Waveform bars decoration
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.Center),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val bars = audioItem.waveformSamples.take(12).ifEmpty {
                                            listOf(0.3f, 0.7f, 0.5f, 0.9f, 0.6f, 0.8f, 0.4f, 0.75f, 0.5f, 0.85f)
                                        }
                                        bars.forEach { h ->
                                            Box(
                                                modifier = Modifier
                                                    .width(3.dp)
                                                    .height((10f + h * 34f).dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(Color.White.copy(alpha = 0.75f))
                                            )
                                        }
                                    }

                                    // Play/Pause Preview Toggle Button
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.Black.copy(alpha = 0.55f),
                                        modifier = Modifier
                                            .size(28.dp)
                                            .align(Alignment.TopStart)
                                            .clickable { onTogglePreview(audioItem) }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (isPlayingThis) Icons.Default.Stop else Icons.Default.PlayArrow,
                                                contentDescription = if (isPlayingThis) "Stop" else "Preview",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    // Duration Pill
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color.Black.copy(alpha = 0.7f),
                                        modifier = Modifier.align(Alignment.BottomStart)
                                    ) {
                                        Text(
                                            text = DeviceMediaScanner.formatShortDuration(audioItem.durationMs),
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = audioItem.title,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = audioItem.artist,
                                color = AddSlateText,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ShapeCategoryContent(
    onSelectShape: (ShapeType) -> Unit
) {
    val shapes = remember {
        listOf(
            ShapeType.CIRCLE,
            ShapeType.SQUARE,
            ShapeType.ROUNDED,
            ShapeType.RECTANGLE,
            ShapeType.TRIANGLE,
            ShapeType.DIAMOND,
            ShapeType.PENTAGON,
            ShapeType.HEXAGON,
            ShapeType.OCTAGON,
            ShapeType.STAR,
            ShapeType.STAR_6,
            ShapeType.SPARKLE,
            ShapeType.QUAD,
            ShapeType.IRREGULAR,
            ShapeType.MULTIFOIL,
            ShapeType.PLUS,
            ShapeType.TEARDROP,
            ShapeType.PIE,
            ShapeType.ARC,
            ShapeType.MOON,
            ShapeType.STAMP,
            ShapeType.CALLOUT
        )
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
            .testTag("add_shape_grid"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(shapes, key = { it.name }) { shapeType ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = AddCardTileBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.92f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelectShape(shapeType) }
                    .testTag("shape_tile_${shapeType.name.lowercase()}")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Canvas(modifier = Modifier.size(36.dp)) {
                        translate(
                            left = size.width / 2f,
                            top = size.height / 2f
                        ) {
                            drawVectorShapeGeometry(
                                type = shapeType,
                                shapeSize = size.minDimension * 0.78f,
                                fillColor = Color(0xFFD5DBE2),
                                strokeColor = Color.Transparent,
                                strokeWidth = 0f
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = shapeType.label,
                        color = AddSlateText,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ElementCategoryContent(
    showSolidSwatches: Boolean,
    onToggleSolidSwatches: () -> Unit,
    onSelectSolidColor: (String, Long) -> Unit,
    onSelectElement: (String, ClipItem) -> Unit,
    onOpen3DCreator: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .weight(1f)
                .testTag("add_element_grid"),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                ElementTileCard(
                    tag = "element_solid",
                    icon = Icons.Default.Rectangle,
                    label = "Solid",
                    onClick = onToggleSolidSwatches
                )
            }
            item {
                ElementTileCard(
                    tag = "element_3d_model",
                    icon = Icons.Default.ViewInAr,
                    label = "3D model",
                    onClick = onOpen3DCreator
                )
            }
            item {
                ElementTileCard(
                    tag = "element_light",
                    icon = Icons.Default.Lightbulb,
                    label = "Light",
                    onClick = {
                        onSelectElement(
                            "Studio Light Leak",
                            ClipItem(
                                title = "Studio Light Leak",
                                type = ClipType.ADJUSTMENT,
                                colorHex = 0xFFFFB020,
                                startTimeMs = 0L,
                                durationMs = 5000L,
                                effects = listOf(VideoEffect(type = EffectType.GLOW, intensity = 0.85f))
                            )
                        )
                    }
                )
            }
            item {
                ElementTileCard(
                    tag = "element_camera",
                    icon = Icons.Default.Videocam,
                    label = "Camera",
                    onClick = {
                        onSelectElement(
                            "Motion Camera Rig",
                            ClipItem(
                                title = "Motion Camera Rig",
                                type = ClipType.NULL_OBJECT,
                                colorHex = 0xFF00E5FF,
                                startTimeMs = 0L,
                                durationMs = 6000L,
                                keyframeTracks = mapOf(
                                    AnimatableProperty.SCALE to KeyframeTrack(
                                        property = AnimatableProperty.SCALE,
                                        keyframes = listOf(
                                            Keyframe(timeMs = 0L, value = 1.0f, easing = EasingType.EaseOut),
                                            Keyframe(timeMs = 3000L, value = 1.15f)
                                        )
                                    )
                                )
                            )
                        )
                    }
                )
            }
        }

        if (showSolidSwatches) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "Obsidian" to 0xFF090C12,
                    "Emerald" to 0xFF0D3824,
                    "Cyber Blue" to 0xFF0A2444,
                    "Crimson" to 0xFF581020
                ).forEach { (name, hex) ->
                    Surface(
                        color = Color(hex),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, AddEmerald.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectSolidColor(name, hex) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(name, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ElementTileCard(
    tag: String,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = AddCardTileBg,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.0f)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, color = AddSlateText, fontSize = 12.sp)
        }
    }
}

@Composable
private fun TemplatesCategoryContent(
    onSelectTemplate: (MotionTemplatePreset) -> Unit
) {
    val templates = remember {
        listOf(
            MotionTemplatePreset(
                "tpl_bouncy",
                "Bouncy title",
                "Title",
                "Letters that bounce in one by one; playful, for memes, reactions and vlogs.",
                textColor = 0xFFFFFFFF
            ),
            MotionTemplatePreset(
                "tpl_word_quote",
                "Word-by-word quote",
                "Caption",
                "Serif sentence rising word by word; for quotes, lyrics and reflections.",
                textColor = 0xFFF1F5F9
            ),
            MotionTemplatePreset(
                "tpl_typewriter",
                "Typewriter",
                "Caption",
                "Text typed letter by letter; for quotes, captions and dialogue.",
                textColor = 0xFFE2E8F0
            ),
            MotionTemplatePreset(
                "tpl_lower_third",
                "Lower third",
                "Lower third",
                "Dark bar opening at the bottom with name and role; to introduce someone or the channel handle.",
                textColor = 0xFF10E878
            ),
            MotionTemplatePreset(
                "tpl_glitch_intro",
                "Glitch intro",
                "Intro",
                "Title that glitches in and out with RGB split; energetic opener that lands on the drop.",
                effectType = EffectType.CHROMATIC_ABERRATION,
                textColor = 0xFF00E5FF
            ),
            MotionTemplatePreset(
                "tpl_neon_sign",
                "Neon sign",
                "Title",
                "Neon text that flickers on with a strong glow; night vibe, for city and synthwave titles.",
                effectType = EffectType.GLOW,
                textColor = 0xFFA855F7
            ),
            MotionTemplatePreset(
                "tpl_light_sweep",
                "Light sweep",
                "Title",
                "Title with a beam of light sweeping across the letters; trophy shine, to reveal names and logos.",
                effectType = EffectType.GLOW,
                textColor = 0xFFFFD54F
            ),
            MotionTemplatePreset(
                "tpl_glowing_title",
                "Glowing title",
                "Title",
                "Title that grows in and lights up with a golden glow pulse.",
                effectType = EffectType.GLOW,
                textColor = 0xFFFFB020
            ),
            MotionTemplatePreset(
                "tpl_flash",
                "Flash",
                "Impact",
                "Quick white flash that hits on the beat; for cuts, drops and hard transitions.",
                isAdjustment = true,
                effectType = EffectType.GLOW
            ),
            MotionTemplatePreset(
                "tpl_glitch_hit",
                "Glitch hit",
                "Impact",
                "Adjustment layer: everything below glitches with RGB split on the beat and recovers.",
                isAdjustment = true,
                effectType = EffectType.CHROMATIC_ABERRATION
            ),
            MotionTemplatePreset(
                "tpl_zoom_punch",
                "Zoom punch",
                "Impact",
                "Adjustment layer: the picture punches in on the beat and settles.",
                isAdjustment = true,
                effectType = EffectType.MOTION_BLUR
            ),
            MotionTemplatePreset(
                "tpl_blur_in",
                "Blur in",
                "Transition",
                "Adjustment layer: the scene comes in blurred and snaps into focus.",
                isAdjustment = true,
                effectType = EffectType.MOTION_BLUR
            )
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("add_templates_list"),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(templates, key = { it.id }) { tpl ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectTemplate(tpl) }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("template_row_${tpl.id}")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tpl.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = tpl.categoryTag,
                        color = AddEmerald,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tpl.description,
                    color = AddSlateText,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
            HorizontalDivider(color = AddDividerColor.copy(alpha = 0.6f), thickness = 1.dp)
        }
    }
}

@Composable
private fun InlineTextCreatorPane(
    currentTimeMs: Long,
    onAddTextClip: (ClipItem) -> Unit
) {
    var textValue by remember { mutableStateOf("MOTION CUT") }
    var chosenColor by remember { mutableLongStateOf(0xFFFFFFFFL) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Kinetic Text Layer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        OutlinedTextField(
            value = textValue,
            onValueChange = { textValue = it },
            singleLine = true,
            label = { Text("Caption / Title Text") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AddEmerald,
                unfocusedBorderColor = AddDividerColor,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("inline_text_input")
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(0xFFFFFFFFL, 0xFF10E878L, 0xFF00E5FFL, 0xFFFFB020L, 0xFFFF3366L).forEach { col ->
                Surface(
                    shape = CircleShape,
                    color = Color(col),
                    border = if (chosenColor == col) BorderStroke(2.dp, Color.White) else null,
                    modifier = Modifier
                        .size(30.dp)
                        .clickable { chosenColor = col }
                ) {}
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = {
                val safeText = textValue.trim().ifBlank { "MOTION CUT" }
                onAddTextClip(
                    ClipItem(
                        title = safeText,
                        type = ClipType.TEXT,
                        startTimeMs = currentTimeMs,
                        durationMs = 5000L,
                        textConfig = TextConfig(
                            text = safeText,
                            fontSizeSp = 32f,
                            textColor = chosenColor,
                            strokeColor = 0xFF000000,
                            strokeWidth = 3f
                        )
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("confirm_inline_text_button"),
            colors = ButtonDefaults.buttonColors(containerColor = AddEmerald, contentColor = Color.Black),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add Text Layer", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun InlineDrawCreatorPane(
    currentTimeMs: Long,
    onAddDrawClip: (ClipItem) -> Unit
) {
    var brushColor by remember { mutableLongStateOf(0xFF10E878L) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Vector Draw Overlay", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(
            text = "Creates an editable vector stroke overlay positioned above your active video frame.",
            color = AddSlateText,
            fontSize = 12.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(0xFF10E878L, 0xFF00E5FFL, 0xFFFFFFFFL, 0xFFFFB020L, 0xFFA855F7L).forEach { col ->
                Surface(
                    shape = CircleShape,
                    color = Color(col),
                    border = if (brushColor == col) BorderStroke(2.dp, Color.White) else null,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { brushColor = col }
                ) {}
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = {
                onAddDrawClip(
                    ClipItem(
                        title = "Vector Brush Layer",
                        type = ClipType.SHAPE,
                        startTimeMs = currentTimeMs,
                        durationMs = 5000L,
                        shapeConfig = ShapeConfig(
                            type = ShapeType.ARC,
                            fillColor = 0x00000000,
                            strokeColor = brushColor,
                            strokeWidth = 10f
                        )
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("confirm_inline_draw_button"),
            colors = ButtonDefaults.buttonColors(containerColor = AddEmerald, contentColor = Color.Black),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Draw, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add Draw Layer", fontWeight = FontWeight.Bold)
        }
    }
}

private fun buildClipFromTemplate(preset: MotionTemplatePreset, currentTimeMs: Long): ClipItem {
    val fx = preset.effectType?.let { listOf(VideoEffect(type = it, intensity = 0.7f)) } ?: emptyList()
    return if (preset.isAdjustment) {
        ClipItem(
            title = preset.title,
            type = ClipType.ADJUSTMENT,
            colorHex = 0xFF10E878,
            startTimeMs = currentTimeMs,
            durationMs = if (preset.id == "tpl_flash") 1200L else 2500L,
            effects = fx,
            keyframeTracks = mapOf(
                AnimatableProperty.SCALE to KeyframeTrack(
                    property = AnimatableProperty.SCALE,
                    keyframes = listOf(
                        Keyframe(timeMs = 0L, value = 1.0f, easing = EasingType.EaseOut),
                        Keyframe(timeMs = 350L, value = 1.18f, easing = EasingType.EaseInOut),
                        Keyframe(timeMs = 900L, value = 1.0f)
                    )
                )
            )
        )
    } else {
        ClipItem(
            title = preset.title,
            type = ClipType.TEXT,
            startTimeMs = currentTimeMs,
            durationMs = 4000L,
            baseTransform = TransformState(
                positionY = if (preset.categoryTag == "Lower third") 160f else 0f
            ),
            textConfig = TextConfig(
                text = preset.title.uppercase(),
                fontSizeSp = if (preset.categoryTag == "Lower third") 22f else 32f,
                textColor = preset.textColor,
                isBold = true,
                strokeColor = 0xFF000000,
                strokeWidth = 3f
            ),
            effects = fx,
            keyframeTracks = mapOf(
                AnimatableProperty.SCALE to KeyframeTrack(
                    property = AnimatableProperty.SCALE,
                    keyframes = listOf(
                        Keyframe(timeMs = 0L, value = 0.65f, easing = EasingType.Bounce),
                        Keyframe(timeMs = 600L, value = 1.08f, easing = EasingType.EaseInOut),
                        Keyframe(timeMs = 4000L, value = 1.0f)
                    )
                )
            )
        )
    }
}
