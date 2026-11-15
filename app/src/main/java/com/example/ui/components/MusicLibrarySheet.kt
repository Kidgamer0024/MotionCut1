package com.example.ui.components

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.MusicCategory
import com.example.model.MusicTrack
import com.example.ui.theme.MotionCyan
import com.example.ui.theme.PlayheadAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioDarkBg
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant

@Composable
fun MusicLibrarySheet(
    tracks: List<MusicTrack>,
    isPlayingPreview: Boolean,
    currentPreviewTrackId: String?,
    previewProgress: Float,
    onPlayPreview: (MusicTrack) -> Unit,
    onStopPreview: () -> Unit,
    onImportTrack: (MusicTrack) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(MusicCategory.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredTracks = remember(selectedCategory, searchQuery, tracks) {
        tracks.filter { track ->
            (selectedCategory == MusicCategory.ALL || track.category == selectedCategory) &&
            (searchQuery.isBlank() ||
             track.title.contains(searchQuery, ignoreCase = true) ||
             track.artist.contains(searchQuery, ignoreCase = true) ||
             track.tags.any { it.contains(searchQuery, ignoreCase = true) })
        }
    }

    Dialog(onDismissRequest = {
        onStopPreview()
        onDismiss()
    }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(580.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, StudioBorder, RoundedCornerShape(16.dp)),
            color = StudioDarkBg
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
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
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = MotionCyan, modifier = Modifier.size(22.dp))
                        Column {
                            Text(
                                text = "Royalty-Free Music Library",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Preview & import soundtrack directly to timeline",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                fontSize = 10.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            onStopPreview()
                            onDismiss()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by title, artist, genre...", fontSize = 12.sp, color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MotionCyan,
                        unfocusedBorderColor = StudioBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category Chips (All, Upbeat, Cinematic, Ambient, Electronic)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(MusicCategory.values()) { cat ->
                        val isSel = cat == selectedCategory
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) MotionCyan.copy(alpha = 0.25f) else StudioSurfaceVariant)
                                .border(1.dp, if (isSel) MotionCyan else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSel) MotionCyan else Color.LightGray,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Track List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTracks) { track ->
                        val isThisPlaying = isPlayingPreview && currentPreviewTrackId == track.id

                        TrackListItem(
                            track = track,
                            isPlaying = isThisPlaying,
                            playbackProgress = if (isThisPlaying) previewProgress else 0f,
                            onPlayPause = { onPlayPreview(track) },
                            onImport = {
                                onStopPreview()
                                onImportTrack(track)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackListItem(
    track: MusicTrack,
    isPlaying: Boolean,
    playbackProgress: Float,
    onPlayPause: () -> Unit,
    onImport: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, if (isPlaying) MotionCyan else StudioBorder, RoundedCornerShape(10.dp)),
        color = StudioSurfaceVariant
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play / Pause Round Button
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(36.dp)
                        .background(if (isPlaying) MotionCyan else StudioDarkBg, CircleShape)
                        .testTag("play_music_${track.id}")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause Preview" else "Play Preview",
                        tint = if (isPlaying) Color.Black else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Title & Artist
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "${track.artist} • ${track.category.label} • ${track.bpm} BPM",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Import Button
                Button(
                    onClick = onImport,
                    modifier = Modifier.height(30.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MotionCyan.copy(alpha = 0.2f), contentColor = MotionCyan),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Add", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Dynamic Audio Waveform Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(Color(0xFF070A0F), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                val w = size.width
                val h = size.height
                val samples = track.waveformSamples
                val barSpacing = w / samples.size
                val barWidth = barSpacing * 0.65f

                for (i in samples.indices) {
                    val sample = samples[i]
                    val barH = h * sample
                    val x = i * barSpacing
                    val y = (h - barH) / 2f
                    val progressFraction = i.toFloat() / samples.size

                    val isPlayed = isPlaying && (progressFraction <= playbackProgress)
                    val barColor = if (isPlayed) MotionCyan else Color(track.coverColorHex).copy(alpha = 0.7f)

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barH),
                        cornerRadius = CornerRadius(2f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Duration & Key badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Key: ${track.musicalKey}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray,
                    fontSize = 9.sp
                )
                Text(
                    text = track.durationFormatted,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    fontSize = 10.sp
                )
            }
        }
    }
}
