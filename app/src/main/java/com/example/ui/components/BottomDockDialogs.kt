package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.ViewQuilt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.window.Dialog
import com.example.model.AspectRatio
import com.example.model.ClipItem
import com.example.model.ClipType
import com.example.model.ExportResolution
import com.example.ui.theme.KeyframeViolet
import com.example.ui.theme.MotionCyan
import com.example.ui.theme.PlayheadAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioDarkBg
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import java.util.UUID

private data class LayoutFramePreset(
    val label: String,
    val subtitle: String,
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float
)

@Composable
fun LibraryHubDialog(
    mainVideoTrackId: String,
    currentTimeMs: Long,
    onOpenMarketplace: () -> Unit,
    onOpenMusicLibrary: () -> Unit,
    onOpenEffectLibrary: () -> Unit = {},
    onInsertStockClip: (ClipItem) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .testTag("library_hub_dialog"),
            color = StudioDarkBg,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, StudioBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = MotionCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Creator Asset Library",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                Text(
                    text = "Import templates, LUT presets, royalty-free tracks, or instant stock B-roll.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Marketplace card
                Surface(
                    color = StudioSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, KeyframeViolet.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onDismiss()
                            onOpenMarketplace()
                        }
                        .testTag("library_open_marketplace_button")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Storefront, contentDescription = null, tint = KeyframeViolet)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Templates & LUT Marketplace", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Transitions, color packs, and community templates", color = Color.LightGray, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 2,200+ Multi-Suite Effects Library card
                Surface(
                    color = StudioSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MotionCyan.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onDismiss()
                            onOpenEffectLibrary()
                        }
                        .testTag("library_open_fx_button")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MotionCyan)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Effects Library (2,200+ FX)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Alight Motion, Gold Motion, Ultra Motion, Node Video & CapCut", color = Color.LightGray, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Music & SFX card
                Surface(
                    color = StudioSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, PlayheadAmber.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onDismiss()
                            onOpenMusicLibrary()
                        }
                        .testTag("library_open_music_button")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = PlayheadAmber)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Music & Soundtracks", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Preview & drop beat-synced audio stems onto timeline", color = Color.LightGray, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Stock Backdrop B-Roll card
                Surface(
                    color = StudioSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MotionCyan.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onInsertStockClip(
                                ClipItem(
                                    id = UUID.randomUUID().toString(),
                                    trackId = mainVideoTrackId,
                                    title = "Studio Backdrop B-Roll",
                                    type = ClipType.COLOR_BG,
                                    colorHex = 0xFF0A2E47,
                                    startTimeMs = currentTimeMs,
                                    durationMs = 3000L
                                )
                            )
                            onDismiss()
                        }
                        .testTag("library_insert_stock_button")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Layers, contentDescription = null, tint = MotionCyan)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Insert Studio Stock Backdrop", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Drops a 3.0s studio backdrop layer at playhead", color = Color.LightGray, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LayoutConfigDialog(
    currentAspectRatio: AspectRatio,
    selectedClipTitle: String?,
    onApplyLayout: (AspectRatio, String, Float, Float, Float) -> Unit,
    onDismiss: () -> Unit
) {
    var chosenRatio by remember { mutableStateOf(currentAspectRatio) }
    val presets = remember {
        listOf(
            LayoutFramePreset("Full Frame", "100% centered canvas", 1.0f, 0f, 0f),
            LayoutFramePreset("Cinema Framing", "85% letterbox safe", 0.85f, 0f, 0f),
            LayoutFramePreset("Split Left", "Left column frame", 0.65f, -140f, 0f),
            LayoutFramePreset("Split Right", "Right column frame", 0.65f, 140f, 0f),
            LayoutFramePreset("PiP Corner", "45% floating inset", 0.45f, 150f, -180f)
        )
    }
    var chosenPreset by remember { mutableStateOf(presets.first()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .testTag("layout_config_dialog"),
            color = StudioDarkBg,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, StudioBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AspectRatio, contentDescription = null, tint = MotionCyan, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Canvas Layout & Framing",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("ASPECT RATIO", color = MotionCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AspectRatio.values().forEach { ratio ->
                        val isSel = chosenRatio == ratio
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) MotionCyan.copy(alpha = 0.22f) else StudioSurface,
                            border = BorderStroke(1.dp, if (isSel) MotionCyan else StudioBorder),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { chosenRatio = ratio }
                                .testTag("layout_ratio_${ratio.name.lowercase()}")
                        ) {
                            Text(
                                text = ratio.ratioText,
                                color = if (isSel) MotionCyan else Color.White,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "CLIP FRAME PRESET (${selectedClipTitle ?: "Canvas"})",
                    color = MotionCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    presets.forEach { preset ->
                        val isSel = chosenPreset.label == preset.label
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) KeyframeViolet.copy(alpha = 0.22f) else StudioSurfaceVariant,
                            border = BorderStroke(1.dp, if (isSel) KeyframeViolet else StudioBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { chosenPreset = preset }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(preset.label, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                Text(preset.subtitle, color = if (isSel) KeyframeViolet else Color.Gray, fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        onApplyLayout(
                            chosenRatio,
                            chosenPreset.label,
                            chosenPreset.scale,
                            chosenPreset.offsetX,
                            chosenPreset.offsetY
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("apply_layout_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MotionCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Apply Layout (${chosenRatio.ratioText})", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SplitIntoPartsDialog(
    targetClipTitle: String?,
    targetClipDurationMs: Long?,
    onSplitAtPlayhead: () -> Unit,
    onSplitIntoParts: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .testTag("split_parts_dialog"),
            color = StudioDarkBg,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, StudioBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.ViewQuilt, contentDescription = null, tint = MotionCyan, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Split Into Parts",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                Text(
                    text = if (targetClipTitle != null && targetClipDurationMs != null) {
                        "Target: $targetClipTitle (${targetClipDurationMs / 1000f}s)"
                    } else {
                        "No clip selected on timeline"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = StudioSurfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MotionCyan.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSplitAtPlayhead() }
                        .testTag("split_at_playhead_option")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.ContentCut, contentDescription = null, tint = MotionCyan, modifier = Modifier.size(18.dp))
                        Column {
                            Text("Split at Playhead", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Cut into 2 segments at the current timeline position", color = Color.LightGray, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("EQUAL SEGMENT CUTS", color = MotionCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(2, 3, 4).forEach { count ->
                        Surface(
                            color = StudioCardBg,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, StudioBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSplitIntoParts(count) }
                                .testTag("split_${count}_parts_button")
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("$count Parts", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                if (targetClipDurationMs != null && targetClipDurationMs >= count * 200L) {
                                    val eachSec = (targetClipDurationMs / count) / 1000f
                                    Text(
                                        text = String.format("%.1fs ea", eachSec),
                                        color = MotionCyan,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UpscaleResolutionDialog(
    currentResolution: ExportResolution,
    isProcessing: Boolean,
    statusMessage: String?,
    onApplyUpscale: (ExportResolution) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedRes by remember {
        mutableStateOf(
            if (currentResolution == ExportResolution.UHD_4K) ExportResolution.UHD_4K
            else ExportResolution.UHD_4K
        )
    }

    Dialog(onDismissRequest = { if (!isProcessing) onDismiss() }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .testTag("upscale_resolution_dialog"),
            color = StudioDarkBg,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, StudioBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Hd, contentDescription = null, tint = MotionCyan, modifier = Modifier.size(22.dp))
                        Text(
                            text = "HD / 4K Output Resolution",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, enabled = !isProcessing, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                Text(
                    text = "Current output: ${currentResolution.label} (${currentResolution.width}x${currentResolution.height})",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExportResolution.values().forEach { res ->
                        val isSel = selectedRes == res
                        Surface(
                            color = if (isSel) MotionCyan.copy(alpha = 0.2f) else StudioSurfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (isSel) MotionCyan else StudioBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(enabled = !isProcessing) { selectedRes = res }
                                .testTag("upscale_res_${res.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(res.label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("${res.width} x ${res.height} • ${res.bitrateMbps} Mbps", color = Color.LightGray, fontSize = 11.sp)
                                }
                                if (isSel) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MotionCyan, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                if (isProcessing) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = statusMessage ?: "Updating output resolution...",
                        color = MotionCyan,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = MotionCyan,
                        trackColor = StudioBorder
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = { onApplyUpscale(selectedRes) },
                    enabled = !isProcessing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("run_upscale_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MotionCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Hd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Upscale to ${selectedRes.label}", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
