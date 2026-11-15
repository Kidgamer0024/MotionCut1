package com.example.ui.components

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.engine.ExportedVideoResult
import com.example.engine.VideoExporter
import com.example.model.ExportResolution
import com.example.model.FrameRate
import com.example.model.Project
import com.example.ui.theme.CutRose
import com.example.ui.theme.MotionCyan
import com.example.ui.theme.PlayheadAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioDarkBg
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.SuccessEmerald
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun ExportRenderDialog(
    project: Project,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedRes by remember(project.id) { mutableStateOf(project.resolution) }
    var selectedFps by remember(project.id) { mutableStateOf(project.frameRate) }

    var isRendering by remember { mutableStateOf(false) }
    var renderProgress by remember { mutableFloatStateOf(0f) }
    var currentFrameNum by remember { mutableIntStateOf(0) }
    var totalFrameCount by remember { mutableIntStateOf(0) }
    var exportResult by remember { mutableStateOf<ExportedVideoResult?>(null) }
    var exportError by remember { mutableStateOf<String?>(null) }
    var savedCopyNotice by remember { mutableStateOf<String?>(null) }
    var activeJob by remember { mutableStateOf<Job?>(null) }

    val saveAsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("video/mp4")
    ) { destUri ->
        val srcFile = exportResult?.localFile
        if (destUri != null && srcFile != null && srcFile.exists()) {
            coroutineScope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(destUri)?.use { out ->
                            srcFile.inputStream().use { inp -> inp.copyTo(out) }
                        }
                    }.isSuccess
                }
                savedCopyNotice = if (ok) {
                    "Copied MP4 to selected folder"
                } else {
                    "Could not write to selected location"
                }
            }
        }
    }

    val estimatedMb = remember(selectedRes, selectedFps, project.durationMs) {
        val totalSec = project.durationMs / 1000f
        val bitrate = selectedRes.bitrateMbps * (selectedFps.fps / 30f)
        val mb = (totalSec * bitrate) / 8f
        String.format(Locale.US, "%.1f MB", mb)
    }

    val targetDims = remember(project.aspectRatio, selectedRes) {
        VideoExporter.computeExportDimensions(project.aspectRatio, selectedRes)
    }

    Dialog(onDismissRequest = { if (!isRendering) onDismiss() }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, StudioBorder, RoundedCornerShape(16.dp))
                .testTag("export_render_dialog"),
            color = StudioDarkBg
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                // Header
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
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = MotionCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = when {
                                exportResult != null -> "Export Complete!"
                                isRendering -> "Encoding MP4 Video..."
                                else -> "Export Master Video"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = {
                            if (isRendering) {
                                activeJob?.cancel()
                                isRendering = false
                            } else {
                                onDismiss()
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = if (isRendering) "Cancel export" else "Close",
                            tint = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (!isRendering && exportResult == null) {
                    // Settings Phase
                    if (exportError != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CutRose.copy(alpha = 0.16f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CutRose.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = CutRose, modifier = Modifier.size(18.dp))
                                Text(
                                    text = exportError.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CutRose,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Text(
                        text = "Resolution (${project.aspectRatio.ratioText} → ${targetDims.first}×${targetDims.second})",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ExportResolution.values().forEach { res ->
                            val isSel = res == selectedRes
                            val dims = VideoExporter.computeExportDimensions(project.aspectRatio, res)
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, if (isSel) MotionCyan else StudioBorder, RoundedCornerShape(8.dp))
                                    .clickable { selectedRes = res },
                                color = if (isSel) MotionCyan.copy(alpha = 0.2f) else StudioSurfaceVariant
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = res.label.split(" ").first(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSel) MotionCyan else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "${dims.first}×${dims.second}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Frame Rate (FPS)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FrameRate.values().forEach { fps ->
                            val isSel = fps == selectedFps
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, if (isSel) PlayheadAmber else StudioBorder, RoundedCornerShape(8.dp))
                                    .clickable { selectedFps = fps },
                                color = if (isSel) PlayheadAmber.copy(alpha = 0.2f) else StudioSurfaceVariant
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${fps.fps} FPS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSel) PlayheadAmber else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = if (fps.fps == 24) "Cinema" else if (fps.fps == 60) "Fluid" else "Standard",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Summary Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp)),
                        color = StudioSurfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Estimated File Size",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = estimatedMb,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MotionCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "H.264 MP4 • ${project.durationMs / 1000}s",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.LightGray,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            exportError = null
                            isRendering = true
                            renderProgress = 0f
                            currentFrameNum = 0
                            totalFrameCount = 0
                            activeJob = coroutineScope.launch {
                                try {
                                    val result = VideoExporter.exportProjectToMp4(
                                        context = context,
                                        project = project,
                                        resolution = selectedRes,
                                        frameRate = selectedFps,
                                        onProgress = { progress, frame, total ->
                                            renderProgress = progress
                                            currentFrameNum = frame
                                            totalFrameCount = total
                                        }
                                    )
                                    exportResult = result
                                } catch (_: CancellationException) {
                                    exportError = "Export cancelled"
                                } catch (e: Exception) {
                                    exportError = e.localizedMessage ?: "Video encoder failed on this device"
                                } finally {
                                    isRendering = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("start_export_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MotionCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Export", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                } else if (isRendering) {
                    // Rendering Progress Phase
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${(renderProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MotionCyan,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { renderProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MotionCyan,
                            trackColor = StudioSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Encoding frame $currentFrameNum / ${totalFrameCount.coerceAtLeast(1)} (${targetDims.first}×${targetDims.second} @ ${selectedFps.fps}fps)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = {
                                activeJob?.cancel()
                                isRendering = false
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Cancel", color = Color.LightGray)
                        }
                    }
                } else {
                    // Render Completed Phase
                    val result = exportResult!!
                    val sizeText = if (result.fileSizeBytes >= 1_048_576L) {
                        String.format(Locale.US, "%.2f MB", result.fileSizeBytes / 1_048_576f)
                    } else {
                        String.format(Locale.US, "%.0f KB", (result.fileSizeBytes / 1024f).coerceAtLeast(1f))
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessEmerald,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "MP4 Exported Successfully!",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${result.width}×${result.height} • ${result.totalFrames} frames • $sizeText",
                            style = MaterialTheme.typography.labelSmall,
                            color = MotionCyan,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = result.displayLocation,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center
                        )
                        if (savedCopyNotice != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = savedCopyNotice!!,
                                style = MaterialTheme.typography.labelSmall,
                                color = SuccessEmerald,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedButton(
                            onClick = {
                                val suggestedName = "${VideoExporter.sanitizeFileName(project.title)}.mp4"
                                saveAsLauncher.launch(suggestedName)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("save_as_export_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MotionCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save As (Choose Folder)", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceVariant, contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Done")
                            }

                            Button(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "video/mp4"
                                        putExtra(Intent.EXTRA_STREAM, result.shareUri)
                                        putExtra(Intent.EXTRA_SUBJECT, project.title)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(shareIntent, "Share ${project.title}.mp4")
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("share_export_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MotionCyan, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
