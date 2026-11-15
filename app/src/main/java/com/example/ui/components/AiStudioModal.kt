package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.ClipItem
import com.example.ui.theme.KeyframeViolet
import com.example.ui.theme.MotionCyan
import com.example.ui.theme.PlayheadAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioDarkBg
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant

private enum class AiStudioTab(val title: String) {
    CUTOUT_BG("Cutout & BG"),
    COLORIST("AI Colorist"),
    CAPTIONS("Viral Captions")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiStudioModal(
    selectedClip: ClipItem?,
    isProcessing: Boolean,
    statusMessage: String?,
    successFeedback: String?,
    onApplyCutout: (clipId: String?, subject: String, cutoutMode: String, featherPx: Float, expansion: Float) -> Unit,
    onApplyColorGrade: (clipId: String?, moodPrompt: String) -> Unit,
    onApplyCaptions: (sceneDescription: String, styleTheme: String) -> Unit,
    onClearFeedback: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    initialTab: Int = AiStudioTab.CUTOUT_BG.ordinal
) {
    var selectedTab by remember { mutableIntStateOf(initialTab.coerceIn(0, AiStudioTab.values().lastIndex)) }
    LaunchedEffect(initialTab) {
        selectedTab = initialTab.coerceIn(0, AiStudioTab.values().lastIndex)
    }

    // Cutout state
    var selectedSubject by remember { mutableStateOf("Person / Dancer") }
    var customSubject by remember { mutableStateOf("") }
    var selectedCutoutMode by remember { mutableStateOf("Transparent Cutout (Remove BG)") }
    var featherPx by remember { mutableFloatStateOf(12f) }
    var expansionPx by remember { mutableFloatStateOf(2f) }

    // Colorist state
    var selectedMoodPreset by remember { mutableStateOf("Blade Runner Cyberpunk") }
    var customMoodPrompt by remember { mutableStateOf("") }

    // Captions state
    var captionSceneDesc by remember { mutableStateOf("Action-packed urban street visual flow") }
    var captionStyleTheme by remember { mutableStateOf("Viral Neon Cyan") }

    val subjectPresets = listOf(
        "Person / Dancer",
        "Action Skater",
        "Sports Car",
        "Cyber Model",
        "Custom Subject"
    )

    val cutoutModes = listOf(
        "Transparent Cutout (Remove BG)",
        "Cyber Neon Aura",
        "Green Screen Chroma",
        "Invert Spotlight"
    )

    val colorPresets = listOf(
        "Blade Runner Cyberpunk",
        "Wes Anderson Pastel",
        "Golden Hour Sunset",
        "Euphoria Violet",
        "Oppenheimer B&W",
        "90s Retro VHS"
    )

    val captionStyles = listOf(
        "Viral Neon Cyan",
        "Impact Yellow",
        "Clean Minimalist",
        "Cyber Glitch"
    )

    Dialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(
                    BorderStroke(
                        1.5.dp,
                        Brush.linearGradient(
                            listOf(
                                MotionCyan.copy(alpha = 0.8f),
                                KeyframeViolet.copy(alpha = 0.8f)
                            )
                        )
                    ),
                    RoundedCornerShape(20.dp)
                )
                .testTag("ai_studio_modal"),
            color = StudioDarkBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
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
                            color = KeyframeViolet.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, KeyframeViolet)
                        ) {
                            Box(modifier = Modifier.padding(6.dp)) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MotionCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "AI Magic Studio",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MotionCyan.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, MotionCyan.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "Gemini 3.8 Flash",
                                        color = MotionCyan,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Pro neural video synthesis & smart segmentation",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isProcessing,
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

                Spacer(modifier = Modifier.height(12.dp))

                // Target clip badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StudioSurfaceVariant,
                    border = BorderStroke(1.dp, StudioBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = MotionCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Target Clip: ",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                        Text(
                            text = selectedClip?.title ?: "Master Timeline / Whole Project",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Studio Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = StudioSurface,
                    contentColor = MotionCyan,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MotionCyan,
                            height = 2.dp
                        )
                    },
                    divider = {}
                ) {
                    AiStudioTab.values().forEachIndexed { index, tab ->
                        val isSelected = selectedTab == index
                        Tab(
                            selected = isSelected,
                            onClick = { selectedTab = index },
                            modifier = Modifier.testTag("ai_tab_${tab.name.lowercase()}"),
                            text = {
                                Text(
                                    text = tab.title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MotionCyan else Color.LightGray
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (selectedTab) {
                        AiStudioTab.CUTOUT_BG.ordinal -> {
                            // ✂️ TAB 1: Cutout & Remove Background
                            Text(
                                text = "Isolate subject silhouette, strip backgrounds, and generate precise feathered masks.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "TARGET SUBJECT",
                                style = MaterialTheme.typography.labelSmall,
                                color = MotionCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                subjectPresets.forEach { sub ->
                                    val isSel = selectedSubject == sub
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSel) MotionCyan.copy(alpha = 0.25f) else StudioSurface,
                                        border = BorderStroke(1.dp, if (isSel) MotionCyan else StudioBorder),
                                        modifier = Modifier.clickable { selectedSubject = sub }
                                    ) {
                                        Text(
                                            text = sub,
                                            color = if (isSel) MotionCyan else Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            if (selectedSubject == "Custom Subject") {
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = customSubject,
                                    onValueChange = { customSubject = it },
                                    placeholder = { Text("e.g. Electric guitar player in red jacket", fontSize = 12.sp) },
                                    modifier = Modifier.fillMaxWidth().testTag("ai_custom_subject_input"),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MotionCyan,
                                        unfocusedBorderColor = StudioBorder,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "CUTOUT RENDERING MODE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MotionCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                cutoutModes.forEach { mode ->
                                    val isSel = selectedCutoutMode == mode
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSel) KeyframeViolet.copy(alpha = 0.25f) else StudioSurface,
                                        border = BorderStroke(1.dp, if (isSel) KeyframeViolet else StudioBorder),
                                        modifier = Modifier.clickable { selectedCutoutMode = mode }
                                    ) {
                                        Text(
                                            text = mode,
                                            color = if (isSel) KeyframeViolet else Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Feathering slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "EDGE FEATHERING (SOFTNESS)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.LightGray,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = "${featherPx.toInt()} px",
                                    color = MotionCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = featherPx,
                                onValueChange = { featherPx = it },
                                valueRange = 0f..30f,
                                colors = SliderDefaults.colors(
                                    thumbColor = MotionCyan,
                                    activeTrackColor = MotionCyan,
                                    inactiveTrackColor = StudioBorder
                                ),
                                modifier = Modifier.fillMaxWidth().height(28.dp)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Expansion slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SILHOUETTE EXPANSION",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.LightGray,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = "${expansionPx.toInt()} px",
                                    color = KeyframeViolet,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = expansionPx,
                                onValueChange = { expansionPx = it },
                                valueRange = -10f..20f,
                                colors = SliderDefaults.colors(
                                    thumbColor = KeyframeViolet,
                                    activeTrackColor = KeyframeViolet,
                                    inactiveTrackColor = StudioBorder
                                ),
                                modifier = Modifier.fillMaxWidth().height(28.dp)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    val subject = if (selectedSubject == "Custom Subject" && customSubject.isNotBlank()) {
                                        customSubject
                                    } else {
                                        selectedSubject
                                    }
                                    onApplyCutout(
                                        selectedClip?.id,
                                        subject,
                                        selectedCutoutMode,
                                        featherPx,
                                        expansionPx
                                    )
                                },
                                enabled = !isProcessing,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("run_ai_cutout_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MotionCyan,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("⚡ Isolate Subject & Remove BG", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        AiStudioTab.COLORIST.ordinal -> {
                            // 🎨 TAB 2: AI Colorist
                            Text(
                                text = "Calibrate cinematic 3-Way color wheels, contrast curves, and LUTs matched by Gemini AI.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "HOLLYWOOD AESTHETIC MOODS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MotionCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                colorPresets.forEach { preset ->
                                    val isSel = selectedMoodPreset == preset
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSel) MotionCyan.copy(alpha = 0.25f) else StudioSurface,
                                        border = BorderStroke(1.dp, if (isSel) MotionCyan else StudioBorder),
                                        modifier = Modifier.clickable { selectedMoodPreset = preset }
                                    ) {
                                        Text(
                                            text = preset,
                                            color = if (isSel) MotionCyan else Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "CUSTOM COLOR GRADE PROMPT",
                                style = MaterialTheme.typography.labelSmall,
                                color = MotionCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = customMoodPrompt,
                                onValueChange = { customMoodPrompt = it },
                                placeholder = {
                                    Text(
                                        "e.g. Golden hour sunset with soft teal shadows, warm skin tones, and subtle vignette",
                                        fontSize = 12.sp
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().testTag("ai_custom_color_prompt_input"),
                                minLines = 2,
                                maxLines = 3,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MotionCyan,
                                    unfocusedBorderColor = StudioBorder,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    val mood = if (customMoodPrompt.isNotBlank()) customMoodPrompt else selectedMoodPreset
                                    onApplyColorGrade(selectedClip?.id, mood)
                                },
                                enabled = !isProcessing,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("run_ai_color_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = KeyframeViolet,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.ColorLens, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("✨ Generate AI Color Grade", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        AiStudioTab.CAPTIONS.ordinal -> {
                            // 💬 TAB 3: AI Captions
                            Text(
                                text = "Generate auto-timed kinetic viral captions with punchy visual emphasis.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "SCENE TOPIC / CONTEXT",
                                style = MaterialTheme.typography.labelSmall,
                                color = MotionCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = captionSceneDesc,
                                onValueChange = { captionSceneDesc = it },
                                placeholder = {
                                    Text("e.g. Extreme downhill longboard slide through neon city streets", fontSize = 12.sp)
                                },
                                modifier = Modifier.fillMaxWidth().testTag("ai_caption_topic_input"),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MotionCyan,
                                    unfocusedBorderColor = StudioBorder,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "TYPOGRAPHY STYLING",
                                style = MaterialTheme.typography.labelSmall,
                                color = MotionCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                captionStyles.forEach { style ->
                                    val isSel = captionStyleTheme == style
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSel) PlayheadAmber.copy(alpha = 0.25f) else StudioSurface,
                                        border = BorderStroke(1.dp, if (isSel) PlayheadAmber else StudioBorder),
                                        modifier = Modifier.clickable { captionStyleTheme = style }
                                    ) {
                                        Text(
                                            text = style,
                                            color = if (isSel) PlayheadAmber else Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    onApplyCaptions(captionSceneDesc, captionStyleTheme)
                                },
                                enabled = !isProcessing,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("run_ai_captions_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PlayheadAmber,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.FormatQuote, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("🔥 Synthesize Kinetic Captions", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                // Processing Status & Loading bar
                if (isProcessing) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = StudioCardBg),
                        border = BorderStroke(1.dp, MotionCyan.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MotionCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = statusMessage ?: "Synthesizing with Gemini 3.8 Flash...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                color = MotionCyan,
                                trackColor = StudioBorder
                            )
                        }
                    }
                }

                // Success Feedback Banner
                AnimatedVisibility(
                    visible = successFeedback != null && !isProcessing,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    if (successFeedback != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0F3A2C),
                            border = BorderStroke(1.dp, Color(0xFF00E676)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF00E676),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = successFeedback,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = "DISMISS",
                                    color = Color(0xFF00E676),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable { onClearFeedback() }
                                        .padding(4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
