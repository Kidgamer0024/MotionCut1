package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.model.ExportResolution
import com.example.model.FrameRate
import com.example.model.Project
import com.example.ui.theme.MotionCyan
import com.example.ui.theme.PlayheadAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioDarkBg
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.SuccessEmerald
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ExportRenderDialog(
    project: Project,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedRes by remember { mutableStateOf(project.resolution) }
    var selectedFps by remember { mutableStateOf(project.frameRate) }

    var isRendering by remember { mutableStateOf(false) }
    var renderProgress by remember { mutableFloatStateOf(0f) }
    var isRenderComplete by remember { mutableStateOf(false) }

    val estimatedMb = remember(selectedRes, selectedFps, project.durationMs) {
        val totalSec = project.durationMs / 1000f
        val bitrate = selectedRes.bitrateMbps * (selectedFps.fps / 30f)
        val mb = (totalSec * bitrate) / 8f
        String.format("%.1f MB", mb)
    }

    Dialog(onDismissRequest = { if (!isRendering) onDismiss() }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, StudioBorder, RoundedCornerShape(16.dp)),
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
                            text = if (isRenderComplete) "Export Complete!" else if (isRendering) "Rendering Frame Pipeline..." else "Export Master Video",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!isRendering) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (!isRendering && !isRenderComplete) {
                    // Settings Phase
                    Text(
                        text = "Resolution",
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
                                        text = "${res.width}x${res.height}",
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
                                text = "H.265 / HEVC • 48kHz Stereo",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.LightGray,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            isRendering = true
                            coroutineScope.launch {
                                for (p in 1..100) {
                                    delay(25L)
                                    renderProgress = p / 100f
                                }
                                isRendering = false
                                isRenderComplete = true
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
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
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
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = MotionCyan,
                            trackColor = StudioSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        val totalFrames = ((project.durationMs / 1000f) * selectedFps.fps).toInt()
                        val currentFrame = (totalFrames * renderProgress).toInt()
                        Text(
                            text = "Encoding frame $currentFrame / $totalFrames (${selectedRes.label} @ ${selectedFps.fps}fps)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }
                } else {
                    // Render Completed Phase
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
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
                            text = "Video Saved Successfully!",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Saved to Gallery • Movies/MotionCut/${project.title}.mp4",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f).height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceVariant, contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Done")
                            }

                            Button(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f).height(44.dp),
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
