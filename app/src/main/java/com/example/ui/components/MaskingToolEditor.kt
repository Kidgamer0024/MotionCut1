package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.Polyline
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.model.ClipItem
import com.example.model.MaskConfig
import com.example.model.MaskShapeType
import com.example.model.MaskVertex
import com.example.ui.theme.CutRose
import com.example.ui.theme.KeyframeViolet
import com.example.ui.theme.MotionCyan
import com.example.ui.theme.PlayheadAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioDarkBg
import com.example.ui.theme.StudioSurfaceVariant
import java.util.Locale

@Composable
fun MaskingToolEditor(
    clip: ClipItem,
    currentTimeMs: Long,
    onAddMask: (MaskConfig) -> Unit,
    onUpdateMask: (MaskConfig) -> Unit,
    onDeleteMask: (String) -> Unit,
    onToggleMaskKeyframe: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMaskId by remember(clip.id, clip.masks.size) {
        mutableStateOf(clip.masks.firstOrNull()?.id)
    }

    val selectedMask = clip.masks.find { it.id == selectedMaskId } ?: clip.masks.firstOrNull()
    val relTime = (currentTimeMs - clip.startTimeMs).coerceIn(0L, clip.durationMs)
    val hasMaskKeyframe = selectedMask?.keyframes?.any { kotlin.math.abs(it.timeMs - relTime) <= 80L } == true

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioDarkBg)
            .padding(10.dp)
    ) {
        // Top row: Mask selector & "+ New Mask" action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Clip Masks (${clip.masks.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                // Keyframe diamond indicator for animated mask
                if (selectedMask != null) {
                    Surface(
                        color = if (hasMaskKeyframe) KeyframeViolet else KeyframeViolet.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (hasMaskKeyframe) Color.White else KeyframeViolet),
                        modifier = Modifier
                            .clickable { onToggleMaskKeyframe(selectedMask.id) }
                            .testTag("mask_keyframe_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Diamond, contentDescription = null, tint = if (hasMaskKeyframe) Color.White else KeyframeViolet, modifier = Modifier.size(13.dp))
                            Text(
                                text = if (hasMaskKeyframe) "Mask KF ●" else "+ Mask KF",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (hasMaskKeyframe) Color.White else KeyframeViolet,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Add Mask button
            Surface(
                color = MotionCyan.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MotionCyan),
                modifier = Modifier.clickable {
                    val newMask = MaskConfig(name = "Mask ${clip.masks.size + 1}")
                    onAddMask(newMask)
                    selectedMaskId = newMask.id
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = MotionCyan, modifier = Modifier.size(14.dp))
                    Text("Add Mask", style = MaterialTheme.typography.labelSmall, color = MotionCyan, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (clip.masks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(StudioSurfaceVariant, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No masks applied to this clip",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap '+ Add Mask' to draw shapes, apply feathering, and keyframe paths",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }
        } else if (selectedMask != null) {
            // Mask List Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(clip.masks) { mask ->
                    val isSel = mask.id == selectedMask.id
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, if (isSel) MotionCyan else StudioBorder, RoundedCornerShape(8.dp))
                            .clickable { selectedMaskId = mask.id },
                        color = if (isSel) MotionCyan.copy(alpha = 0.25f) else StudioSurfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = mask.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSel) MotionCyan else Color.White,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            )
                            if (mask.isAnimated) {
                                Icon(Icons.Default.Timeline, contentDescription = "Animated", tint = KeyframeViolet, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Shape Type Selector Row
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(MaskShapeType.values()) { shapeType ->
                    val isSel = shapeType == selectedMask.shapeType
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .border(1.dp, if (isSel) MotionCyan else StudioBorder, RoundedCornerShape(6.dp))
                            .clickable { onUpdateMask(selectedMask.copy(shapeType = shapeType)) },
                        color = if (isSel) MotionCyan.copy(alpha = 0.2f) else StudioSurfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val icon = when (shapeType) {
                                MaskShapeType.RECTANGLE -> Icons.Default.CropSquare
                                MaskShapeType.CIRCLE,
                                MaskShapeType.ELLIPSE,
                                MaskShapeType.RADIAL_VIGNETTE -> Icons.Default.RadioButtonUnchecked
                                else -> Icons.Default.Polyline
                            }
                            Icon(icon, contentDescription = null, tint = if (isSel) MotionCyan else Color.Gray, modifier = Modifier.size(14.dp))
                            Text(
                                text = shapeType.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSel) Color.White else Color.LightGray,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Controls: Invert Toggle, Feather, Position, Width/Height, Corner Radius, Expand, Opacity, Border
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Switch(
                                checked = selectedMask.isInverted,
                                onCheckedChange = { onUpdateMask(selectedMask.copy(isInverted = it)) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MotionCyan,
                                    checkedTrackColor = MotionCyan.copy(alpha = 0.3f),
                                    uncheckedThumbColor = Color.DarkGray,
                                    uncheckedTrackColor = StudioDarkBg
                                ),
                                modifier = Modifier.size(width = 40.dp, height = 24.dp)
                            )
                            Text("Invert Mask", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }

                        IconButton(
                            onClick = { onDeleteMask(selectedMask.id) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Mask", tint = CutRose, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Feathering Slider
                item {
                    MaskSliderRow(
                        label = "Feather",
                        value = selectedMask.featherPx,
                        valueRange = 0f..50f,
                        unit = "px",
                        onValueChange = { onUpdateMask(selectedMask.copy(featherPx = it)) }
                    )
                }

                // Mask Scale
                item {
                    MaskSliderRow(
                        label = "Scale",
                        value = selectedMask.scaleX,
                        valueRange = 0.2f..2.5f,
                        unit = "x",
                        onValueChange = { onUpdateMask(selectedMask.copy(scaleX = it, scaleY = it)) }
                    )
                }

                // Mask Offset X & Y
                item {
                    MaskSliderRow(
                        label = "Pos X",
                        value = selectedMask.positionX,
                        valueRange = -250f..250f,
                        unit = "px",
                        onValueChange = { onUpdateMask(selectedMask.copy(positionX = it)) }
                    )
                }
                item {
                    MaskSliderRow(
                        label = "Pos Y",
                        value = selectedMask.positionY,
                        valueRange = -350f..350f,
                        unit = "px",
                        onValueChange = { onUpdateMask(selectedMask.copy(positionY = it)) }
                    )
                }

                // Aspect Width & Height
                if (selectedMask.shapeType != MaskShapeType.CIRCLE && selectedMask.shapeType != MaskShapeType.LINEAR_SPLIT) {
                    item {
                        MaskSliderRow(
                            label = "Width",
                            value = selectedMask.aspectWidth,
                            valueRange = 0.25f..2.5f,
                            unit = "x",
                            onValueChange = { onUpdateMask(selectedMask.copy(aspectWidth = it)) }
                        )
                    }
                    item {
                        MaskSliderRow(
                            label = "Height",
                            value = selectedMask.aspectHeight,
                            valueRange = 0.25f..2.5f,
                            unit = "x",
                            onValueChange = { onUpdateMask(selectedMask.copy(aspectHeight = it)) }
                        )
                    }
                }

                // Corner Radius for Rectangle mask
                if (selectedMask.shapeType == MaskShapeType.RECTANGLE) {
                    item {
                        MaskSliderRow(
                            label = "Corner",
                            value = selectedMask.cornerRadiusPx,
                            valueRange = 0f..160f,
                            unit = "px",
                            onValueChange = { onUpdateMask(selectedMask.copy(cornerRadiusPx = it)) }
                        )
                    }
                }

                // Expansion Slider
                item {
                    MaskSliderRow(
                        label = "Expand",
                        value = selectedMask.expansion,
                        valueRange = -100f..100f,
                        unit = "px",
                        onValueChange = { onUpdateMask(selectedMask.copy(expansion = it)) }
                    )
                }

                // Opacity Slider
                item {
                    MaskSliderRow(
                        label = "Opacity",
                        value = selectedMask.opacity * 100f,
                        valueRange = 0f..100f,
                        unit = "%",
                        onValueChange = { onUpdateMask(selectedMask.copy(opacity = (it / 100f).coerceIn(0f, 1f))) }
                    )
                }

                // Border Width & Color Swatches
                item {
                    MaskSliderRow(
                        label = "Border",
                        value = selectedMask.borderWidthPx,
                        valueRange = 0f..24f,
                        unit = "px",
                        onValueChange = { onUpdateMask(selectedMask.copy(borderWidthPx = it)) }
                    )
                }

                item {
                    val borderSwatches = listOf(0xFFFFFFFFL, 0xFF00E5FFL, 0xFFFFB020L, 0xFF10E878L, 0xFFFF4D6DL, 0xFF000000L)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Stroke",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            modifier = Modifier.width(65.dp)
                        )
                        borderSwatches.forEach { hex ->
                            val selectedColor = selectedMask.borderColorHex == hex
                            Surface(
                                shape = CircleShape,
                                color = Color(hex),
                                border = if (selectedColor) BorderStroke(2.dp, MotionCyan) else BorderStroke(1.dp, StudioBorder),
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .clickable { onUpdateMask(selectedMask.copy(borderColorHex = hex)) }
                                    .testTag("mask_border_color_${hex.toString(16)}")
                            ) {}
                        }
                    }
                }

                // Mask Rotation
                item {
                    MaskSliderRow(
                        label = "Rotation",
                        value = selectedMask.rotation,
                        valueRange = -180f..180f,
                        unit = "°",
                        onValueChange = { onUpdateMask(selectedMask.copy(rotation = it)) }
                    )
                }

                // Custom Polygon vertex count and add vertex
                if (selectedMask.shapeType == MaskShapeType.FREEFORM_POLYGON) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Polygon Vertices (${selectedMask.vertices.size} points)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MotionCyan,
                                fontSize = 11.sp
                            )
                            Button(
                                onClick = {
                                    val newVertex = MaskVertex(
                                        x = (selectedMask.vertices.lastOrNull()?.x ?: 0f) + 20f,
                                        y = (selectedMask.vertices.lastOrNull()?.y ?: 0f) + 30f
                                    )
                                    onUpdateMask(selectedMask.copy(vertices = selectedMask.vertices + newVertex))
                                },
                                modifier = Modifier.height(28.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceVariant, contentColor = MotionCyan),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text("+ Add Vertex", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MaskSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    unit: String,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(34.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.LightGray,
            modifier = Modifier.size(width = 65.dp, height = 18.dp),
            fontSize = 11.sp
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = MotionCyan,
                activeTrackColor = MotionCyan,
                inactiveTrackColor = StudioBorder
            ),
            modifier = Modifier.weight(1f)
        )
        val formatted = if (unit == "x") {
            String.format(Locale.US, "%.2fx", value)
        } else {
            "${value.toInt()}$unit"
        }
        Text(
            text = formatted,
            style = MaterialTheme.typography.labelSmall,
            color = MotionCyan,
            modifier = Modifier.size(width = 44.dp, height = 18.dp),
            fontSize = 11.sp
        )
    }
}
