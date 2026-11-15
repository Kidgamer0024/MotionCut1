package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ClipItem
import com.example.model.ClipType
import com.example.model.TextAnimationType
import com.example.model.TextConfig
import com.example.model.TextFontChoice
import com.example.ui.theme.KeyframeViolet
import com.example.ui.theme.MotionCyan
import com.example.ui.theme.PlayheadAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioDarkBg
import com.example.ui.theme.StudioSurfaceVariant
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TextEditorPanel(
    selectedClip: ClipItem?,
    onUpdateTextConfig: (TextConfig) -> Unit,
    onOpenAddTextModal: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selectedClip == null || selectedClip.type != ClipType.TEXT || selectedClip.textConfig == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(StudioDarkBg)
                .padding(16.dp)
                .testTag("text_studio_empty_state"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.TextFields,
                    contentDescription = "Text Studio",
                    tint = PlayheadAmber,
                    modifier = Modifier.size(34.dp)
                )
                Text(
                    text = "Select a Text layer or add an animated title template",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Button(
                    onClick = onOpenAddTextModal,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PlayheadAmber,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("text_studio_add_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Text Layer", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Text Template", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
        return
    }

    val cfg = selectedClip.textConfig
    val textSwatches = listOf(0xFFFFFFFF, 0xFF00E5FF, 0xFFFFD54F, 0xFFFF4081, 0xFF10E878, 0xFF111111)
    val strokeSwatches = listOf(0xFF000000, 0xFFFFFFFF, 0xFFFF2E63, 0xFF00E5FF, 0xFF3E2723)
    val pillSwatches = listOf(0xAA0A0E17, 0xE6D81B60, 0xD90A120E, 0xFFFEE440, 0xCC1E3A5F)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(StudioDarkBg)
            .padding(10.dp)
            .testTag("text_editor_panel"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.TextFields, contentDescription = null, tint = PlayheadAmber, modifier = Modifier.size(16.dp))
                Text(
                    text = "Text Studio • ${selectedClip.title}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "${cfg.fontChoice.displayName} • ${cfg.animationType.label}",
                color = MotionCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Live Title Text Input
        OutlinedTextField(
            value = cfg.text,
            onValueChange = { onUpdateTextConfig(cfg.copy(text = it)) },
            label = { Text("Caption / Title Text", fontSize = 11.sp) },
            singleLine = false,
            maxLines = 2,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PlayheadAmber,
                unfocusedBorderColor = StudioBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = PlayheadAmber,
                unfocusedLabelColor = Color.LightGray
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("text_studio_input")
        )

        // 1-Tap Style Templates
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PlayheadAmber, modifier = Modifier.size(14.dp))
            Text("Templates", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(TextStyleRegistry.templates, key = { it.id }) { tpl ->
                val active = cfg.templateId == tpl.id
                Surface(
                    color = if (active) PlayheadAmber.copy(alpha = 0.22f) else StudioCardBg,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (active) PlayheadAmber else StudioBorder),
                    modifier = Modifier
                        .width(126.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            val preserved = cfg.text.ifBlank { tpl.config.text }
                            onUpdateTextConfig(tpl.config.copy(text = preserved))
                        }
                        .testTag("text_template_${tpl.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tpl.badge,
                                color = Color(tpl.config.textColor),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = TextStyleRegistry.composeFontFamily(tpl.config.fontChoice)
                            )
                            Text(
                                text = tpl.category,
                                color = Color.Gray,
                                fontSize = 9.sp
                            )
                        }
                        Text(
                            text = tpl.name,
                            color = if (active) Color.White else Color.LightGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = tpl.config.animationType.label,
                            color = MotionCyan,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }

        // Bundled Google Fonts Selector
        Text("Typography (${TextFontChoice.entries.size} Fonts)", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(TextFontChoice.entries, key = { it.name }) { font ->
                val active = cfg.fontChoice == font
                Surface(
                    color = if (active) MotionCyan.copy(alpha = 0.2f) else StudioSurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (active) MotionCyan else StudioBorder),
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onUpdateTextConfig(cfg.copy(fontChoice = font)) }
                        .testTag("text_font_${font.fontKey}")
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = font.displayName,
                            color = if (active) Color.White else Color.LightGray,
                            fontSize = 12.sp,
                            fontFamily = TextStyleRegistry.composeFontFamily(font),
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // Animations Selector
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(Icons.Default.Animation, contentDescription = null, tint = KeyframeViolet, modifier = Modifier.size(14.dp))
            Text("Animation", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(TextAnimationType.entries, key = { it.name }) { anim ->
                val active = cfg.animationType == anim
                Surface(
                    color = if (active) KeyframeViolet.copy(alpha = 0.24f) else StudioSurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (active) KeyframeViolet else StudioBorder),
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onUpdateTextConfig(cfg.copy(animationType = anim)) }
                        .testTag("text_anim_${anim.name.lowercase()}")
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = anim.label,
                            color = if (active) Color.White else Color.LightGray,
                            fontSize = 11.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Sliders
        if (cfg.animationType != TextAnimationType.NONE) {
            TextSliderRow(
                label = "Anim Speed",
                value = cfg.animationSpeed,
                range = 0.3f..2.5f,
                displayValue = String.format(Locale.US, "%.1fx", cfg.animationSpeed),
                accentColor = KeyframeViolet,
                onValueChange = { onUpdateTextConfig(cfg.copy(animationSpeed = it)) }
            )
        }
        TextSliderRow(
            label = "Size",
            value = cfg.fontSizeSp,
            range = 14f..72f,
            displayValue = "${cfg.fontSizeSp.toInt()}sp",
            accentColor = PlayheadAmber,
            onValueChange = { onUpdateTextConfig(cfg.copy(fontSizeSp = it)) }
        )
        TextSliderRow(
            label = "Tracking",
            value = cfg.letterSpacing,
            range = 0f..14f,
            displayValue = String.format(Locale.US, "%.1f", cfg.letterSpacing),
            accentColor = MotionCyan,
            onValueChange = { onUpdateTextConfig(cfg.copy(letterSpacing = it)) }
        )
        TextSliderRow(
            label = "Outline",
            value = cfg.strokeWidth,
            range = 0f..10f,
            displayValue = String.format(Locale.US, "%.1fpx", cfg.strokeWidth),
            accentColor = MotionCyan,
            onValueChange = { onUpdateTextConfig(cfg.copy(strokeWidth = it)) }
        )

        // Style Toggles
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(StudioSurfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.FormatBold, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                Text("Bold", color = Color.White, fontSize = 11.sp)
                Switch(
                    checked = cfg.isBold,
                    onCheckedChange = { onUpdateTextConfig(cfg.copy(isBold = it)) },
                    colors = SwitchDefaults.colors(checkedThumbColor = PlayheadAmber),
                    modifier = Modifier.testTag("text_bold_toggle")
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(StudioSurfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("Drop Shadow", color = Color.White, fontSize = 11.sp)
                Switch(
                    checked = cfg.shadowEnabled,
                    onCheckedChange = { onUpdateTextConfig(cfg.copy(shadowEnabled = it)) },
                    colors = SwitchDefaults.colors(checkedThumbColor = MotionCyan),
                    modifier = Modifier.testTag("text_shadow_toggle")
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(StudioSurfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("Backdrop Pill", color = Color.White, fontSize = 11.sp)
                Switch(
                    checked = cfg.hasBackgroundPill,
                    onCheckedChange = { onUpdateTextConfig(cfg.copy(hasBackgroundPill = it)) },
                    colors = SwitchDefaults.colors(checkedThumbColor = KeyframeViolet),
                    modifier = Modifier.testTag("text_pill_toggle")
                )
            }
        }

        // Color Swatches
        SwatchRow(
            label = "Text Color",
            swatches = textSwatches,
            selectedHex = cfg.textColor,
            onSelect = { onUpdateTextConfig(cfg.copy(textColor = it)) }
        )
        SwatchRow(
            label = "Stroke Color",
            swatches = strokeSwatches,
            selectedHex = cfg.strokeColor,
            onSelect = { onUpdateTextConfig(cfg.copy(strokeColor = it)) }
        )
        if (cfg.hasBackgroundPill) {
            SwatchRow(
                label = "Pill Color",
                swatches = pillSwatches,
                selectedHex = cfg.backgroundColor,
                onSelect = { onUpdateTextConfig(cfg.copy(backgroundColor = it)) }
            )
        }
    }
}

@Composable
private fun TextSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    displayValue: String,
    accentColor: Color,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            color = Color.LightGray,
            fontSize = 11.sp,
            modifier = Modifier.width(66.dp)
        )
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = StudioBorder
            ),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = displayValue,
            color = accentColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(42.dp)
        )
    }
}

@Composable
private fun SwatchRow(
    label: String,
    swatches: List<Long>,
    selectedHex: Long,
    onSelect: (Long) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.LightGray,
            fontSize = 11.sp,
            modifier = Modifier.width(68.dp)
        )
        swatches.forEach { hex ->
            val selected = hex == selectedHex
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(hex))
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) Color.White else StudioBorder,
                        shape = CircleShape
                    )
                    .clickable { onSelect(hex) }
            )
        }
    }
}
