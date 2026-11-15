package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.engine.RecordedVoiceOver
import com.example.engine.VoiceOverRecorderEngine
import com.example.ui.theme.CutRose
import com.example.ui.theme.KeyframeViolet
import com.example.ui.theme.MotionCyan
import com.example.ui.theme.PlayheadAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioDarkBg
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant

@Composable
fun VoiceOverRecordSheet(
    recorderEngine: VoiceOverRecorderEngine,
    playheadTimeMs: Long,
    onImportVoiceOver: (RecordedVoiceOver) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasRecordPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasRecordPermission = isGranted
        if (isGranted) {
            recorderEngine.startRecording(context)
        }
    }

    val isRecording by recorderEngine.isRecording.collectAsState()
    val isPaused by recorderEngine.isPaused.collectAsState()
    val recordingDurationMs by recorderEngine.recordingDurationMs.collectAsState()
    val liveWaveform by recorderEngine.liveWaveform.collectAsState()
    val currentDecibels by recorderEngine.currentDecibels.collectAsState()
    val lastRecordedVoiceOver by recorderEngine.lastRecordedVoiceOver.collectAsState()

    Dialog(
        onDismissRequest = {
            if (!isRecording) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(24.dp))
                .border(
                    BorderStroke(
                        1.5.dp,
                        Brush.linearGradient(
                            listOf(
                                CutRose.copy(alpha = 0.8f),
                                KeyframeViolet.copy(alpha = 0.8f)
                            )
                        )
                    ),
                    RoundedCornerShape(24.dp)
                )
                .testTag("voiceover_record_dialog"),
            color = StudioDarkBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = CutRose.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, CutRose)
                        ) {
                            Box(modifier = Modifier.padding(6.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = CutRose,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Voice-over Studio",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Text(
                                text = "Record audio track directly at playhead",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            if (isRecording) {
                                recorderEngine.cancelRecording()
                            }
                            onDismiss()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.LightGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Timecode & Track Info Bar
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = StudioSurfaceVariant,
                    border = BorderStroke(1.dp, StudioBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Insertion Playhead:",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                            Text(
                                text = formatTimecode(playheadTimeMs),
                                style = MaterialTheme.typography.bodySmall,
                                color = MotionCyan,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }

                        // Target Track Badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = KeyframeViolet.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, KeyframeViolet.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "Audio Track 🎙️",
                                color = KeyframeViolet,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Microphone Permission Request Card (when not granted)
                if (!hasRecordPermission) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = StudioCardBg),
                        border = BorderStroke(1.dp, PlayheadAmber.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.MicOff,
                                contentDescription = null,
                                tint = PlayheadAmber,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Microphone Access Required",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "To record live voice-over commentary directly synchronized with your video, please grant microphone permissions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PlayheadAmber,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("grant_mic_permission_button")
                            ) {
                                Text("Grant Microphone Access", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    // Real-Time Waveform Visualizer Screen
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        colors = CardDefaults.cardColors(containerColor = StudioSurface),
                        border = BorderStroke(1.dp, if (isRecording) CutRose.copy(alpha = 0.6f) else StudioBorder),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Waveform Canvas
                            WaveformVisualizerCanvas(
                                samples = liveWaveform,
                                isRecording = isRecording,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Top Info Overlay (dB meter and recording duration)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isRecording) CutRose else Color.Gray)
                                    )
                                    Text(
                                        text = if (isRecording) {
                                            if (isPaused) "PAUSED" else "LIVE MIC"
                                        } else "STANDBY",
                                        color = if (isRecording) CutRose else Color.Gray,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = "${currentDecibels.toInt()} dB",
                                    color = if (currentDecibels > -10f) CutRose else MotionCyan,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Timer Counter Display
                    Text(
                        text = formatTimer(recordingDurationMs),
                        color = if (isRecording) Color.White else Color.LightGray,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Recording Action Controls
                    if (lastRecordedVoiceOver == null || isRecording) {
                        // Live Recording Button Area
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isRecording) {
                                // Pause / Resume Button
                                IconButton(
                                    onClick = {
                                        if (isPaused) recorderEngine.resumeRecording() else recorderEngine.pauseRecording()
                                    },
                                    modifier = Modifier
                                        .size(46.dp)
                                        .border(BorderStroke(1.dp, StudioBorder), CircleShape)
                                        .testTag("pause_resume_recording_button")
                                ) {
                                    Icon(
                                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                        contentDescription = if (isPaused) "Resume" else "Pause",
                                        tint = PlayheadAmber,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            // Big Record / Stop Trigger
                            RecordTriggerButton(
                                isRecording = isRecording,
                                onClick = {
                                    if (!isRecording) {
                                        recorderEngine.startRecording(context)
                                    } else {
                                        recorderEngine.stopRecording()
                                    }
                                }
                            )

                            if (isRecording) {
                                // Cancel / Discard Button
                                IconButton(
                                    onClick = { recorderEngine.cancelRecording() },
                                    modifier = Modifier
                                        .size(46.dp)
                                        .border(BorderStroke(1.dp, StudioBorder), CircleShape)
                                        .testTag("cancel_recording_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancel Recording",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isRecording) "Tap square to stop & review" else "Tap red button to start voice-over",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    } else {
                        // Post-Recording Review & Keep Area
                        val recorded = lastRecordedVoiceOver!!
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
                            border = BorderStroke(1.dp, MotionCyan.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MotionCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = recorded.title,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = "${(recorded.durationMs / 1000f * 10).toInt() / 10f}s",
                                        color = MotionCyan,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Retake Button
                                    OutlinedButton(
                                        onClick = {
                                            recorderEngine.cancelRecording()
                                            recorderEngine.startRecording(context)
                                        },
                                        modifier = Modifier.weight(1f).height(40.dp).testTag("retake_voiceover_button"),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, StudioBorder)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Retake", fontSize = 11.sp, color = Color.White)
                                    }

                                    // Insert & Save Button
                                    Button(
                                        onClick = {
                                            onImportVoiceOver(recorded)
                                        },
                                        modifier = Modifier.weight(1.5f).height(40.dp).testTag("insert_voiceover_button"),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MotionCyan,
                                            contentColor = Color.Black
                                        )
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Insert to Timeline", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
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
private fun WaveformVisualizerCanvas(
    samples: List<Float>,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        // Center baseline
        drawLine(
            color = Color.White.copy(alpha = 0.12f),
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = 1f
        )

        if (samples.isEmpty()) return@Canvas

        val barSpacing = width / samples.size.toFloat()
        val barWidth = (barSpacing * 0.65f).coerceAtLeast(2f)

        for (i in samples.indices) {
            val amp = samples[i].coerceIn(0.04f, 1.0f)
            val barHeight = (amp * height * 0.85f).coerceAtLeast(4f)
            val left = i * barSpacing + (barSpacing - barWidth) / 2f
            val top = centerY - barHeight / 2f

            val barColor = if (!isRecording) {
                Color.Gray.copy(alpha = 0.4f)
            } else if (amp > 0.82f) {
                CutRose
            } else if (amp > 0.55f) {
                PlayheadAmber
            } else {
                MotionCyan
            }

            drawRoundRect(
                color = barColor,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@Composable
private fun RecordTriggerButton(
    isRecording: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isRecording) 1.25f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(76.dp)
    ) {
        // Outer pulsing aura
        if (isRecording) {
            Box(
                modifier = Modifier
                    .size(76.dp * pulseScale)
                    .clip(CircleShape)
                    .background(CutRose.copy(alpha = 0.25f))
            )
        }

        Surface(
            shape = CircleShape,
            color = CutRose,
            shadowElevation = 8.dp,
            modifier = Modifier
                .size(62.dp)
                .clickable { onClick() }
                .testTag("record_voiceover_trigger_button")
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                if (isRecording) {
                    // Square Stop Symbol
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White)
                    )
                } else {
                    // Microphone Icon
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Start Recording",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

private fun formatTimecode(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val millis = (ms % 1000) / 100
    return String.format("%02d:%02d.%01d", minutes, seconds, millis)
}

private fun formatTimer(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hundredths = (ms % 1000) / 10
    return String.format("%02d:%02d.%02d", minutes, seconds, hundredths)
}
