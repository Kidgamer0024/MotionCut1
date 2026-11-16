package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.MotionCyan
import com.example.ui.theme.StudioDivider

private val DockBackground = Color(0xFF0E121B)
private val DockIconSlate = Color(0xFF9AA6B8)
private val DockAddGreen = Color(0xFF10E878)

enum class EditorDockOption(val tag: String) {
    ADD("add"),
    LINK("link"),
    LAYOUT("layout"),
    REMOVE_BG("remove_bg"),
    SPLIT_PARTS("split_parts"),
    UPSCALE("upscale"),
    COLORIZE("colorize"),
    DEPTH_CLOUD("depth_cloud")
}

@Composable
fun EditorBottomBar(
    isSelectedClipLinked: Boolean,
    is4kUpscaled: Boolean,
    isAiProcessing: Boolean,
    onOptionClick: (EditorDockOption) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = EditorDockOption.values().toList()

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = DockBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            HorizontalDivider(color = StudioDivider, thickness = 1.dp)

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("editor_bottom_dock"),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(options, key = { it.name }) { option ->
                    when (option) {
                        EditorDockOption.ADD -> {
                            AddDockItem(
                                label = stringResource(R.string.dock_add),
                                onClick = { onOptionClick(EditorDockOption.ADD) }
                            )
                        }
                        EditorDockOption.LINK -> {
                            val linkTint by animateColorAsState(
                                targetValue = if (isSelectedClipLinked) MotionCyan else DockIconSlate,
                                label = "LinkTint"
                            )
                            StandardDockItem(
                                tag = option.tag,
                                label = stringResource(
                                    if (isSelectedClipLinked) R.string.dock_linked else R.string.dock_link
                                ),
                                icon = if (isSelectedClipLinked) Icons.Default.Link else Icons.Default.LinkOff,
                                tint = linkTint,
                                isActive = isSelectedClipLinked,
                                onClick = { onOptionClick(option) }
                            )
                        }
                        EditorDockOption.LAYOUT -> {
                            CustomGlyphDockItem(
                                tag = option.tag,
                                label = stringResource(R.string.dock_layout),
                                tint = DockIconSlate,
                                onClick = { onOptionClick(option) }
                            ) { glyphColor ->
                                LayoutMosaicGlyph(color = glyphColor, modifier = Modifier.size(22.dp))
                            }
                        }
                        EditorDockOption.REMOVE_BG -> {
                            StandardDockItem(
                                tag = option.tag,
                                label = stringResource(R.string.dock_remove_bg),
                                icon = Icons.Default.AutoFixHigh,
                                tint = DockIconSlate,
                                showBusySpinner = isAiProcessing,
                                onClick = { onOptionClick(option) }
                            )
                        }
                        EditorDockOption.SPLIT_PARTS -> {
                            CustomGlyphDockItem(
                                tag = option.tag,
                                label = stringResource(R.string.dock_split_parts),
                                tint = DockIconSlate,
                                onClick = { onOptionClick(option) }
                            ) { glyphColor ->
                                SplitPartsGlyph(color = glyphColor, modifier = Modifier.size(22.dp))
                            }
                        }
                        EditorDockOption.UPSCALE -> {
                            val upscaleTint by animateColorAsState(
                                targetValue = if (is4kUpscaled) MotionCyan else DockIconSlate,
                                label = "UpscaleTint"
                            )
                            StandardDockItem(
                                tag = option.tag,
                                label = stringResource(R.string.dock_upscale),
                                icon = Icons.Default.Hd,
                                tint = upscaleTint,
                                isActive = is4kUpscaled,
                                onClick = { onOptionClick(option) }
                            )
                        }
                        EditorDockOption.COLORIZE -> {
                            StandardDockItem(
                                tag = option.tag,
                                label = stringResource(R.string.dock_colorize),
                                icon = Icons.Default.Palette,
                                tint = DockIconSlate,
                                onClick = { onOptionClick(option) }
                            )
                        }
                        EditorDockOption.DEPTH_CLOUD -> {
                            CustomGlyphDockItem(
                                tag = option.tag,
                                label = stringResource(R.string.dock_depth_cloud),
                                tint = if (isAiProcessing) MotionCyan else DockIconSlate,
                                onClick = { onOptionClick(option) }
                            ) { glyphColor ->
                                DepthMountainsGlyph(color = glyphColor, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddDockItem(
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .widthIn(min = 52.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .semantics { contentDescription = label }
            .testTag("bottom_bar_add")
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                DockAddGreen.copy(alpha = 0.42f),
                                Color.Transparent
                            )
                        ),
                        radius = size.maxDimension * 0.72f
                    )
                }
                .clip(RoundedCornerShape(12.dp))
                .background(DockAddGreen)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = Color(0xFF06150D),
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = DockAddGreen,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun StandardDockItem(
    tag: String,
    label: String,
    icon: ImageVector,
    tint: Color,
    isActive: Boolean = false,
    showBusySpinner: Boolean = false,
    onClick: () -> Unit
) {
    CustomGlyphDockItem(
        tag = tag,
        label = label,
        tint = tint,
        isActive = isActive,
        onClick = onClick
    ) { glyphColor ->
        if (showBusySpinner) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MotionCyan
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = glyphColor,
                modifier = Modifier.size(23.dp)
            )
        }
    }
}

@Composable
private fun CustomGlyphDockItem(
    tag: String,
    label: String,
    tint: Color,
    isActive: Boolean = false,
    onClick: () -> Unit,
    glyph: @Composable (Color) -> Unit
) {
    val labelColor = if (isActive) MotionCyan else DockIconSlate
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .widthIn(min = 54.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .semantics { contentDescription = label }
            .testTag("bottom_bar_$tag")
    ) {
        Surface(
            color = if (isActive) MotionCyan.copy(alpha = 0.14f) else Color.Transparent,
            shape = RoundedCornerShape(10.dp),
            border = if (isActive) BorderStroke(1.dp, MotionCyan.copy(alpha = 0.45f)) else null
        ) {
            Box(
                modifier = Modifier.size(34.dp),
                contentAlignment = Alignment.Center
            ) {
                glyph(tint)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LayoutMosaicGlyph(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val gap = w * 0.10f
        val leftW = (w - gap) * 0.48f
        val rightX = leftW + gap
        val rightW = w - rightX
        val r = CornerRadius(w * 0.08f, w * 0.08f)

        // Left column: taller top block, shorter bottom block
        val leftTopH = h * 0.56f
        drawRoundRect(
            color = color,
            topLeft = Offset.Zero,
            size = Size(leftW, leftTopH),
            cornerRadius = r
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(0f, leftTopH + gap),
            size = Size(leftW, h - leftTopH - gap),
            cornerRadius = r
        )

        // Right column: shorter top block, taller bottom block
        val rightTopH = h * 0.34f
        drawRoundRect(
            color = color,
            topLeft = Offset(rightX, 0f),
            size = Size(rightW, rightTopH),
            cornerRadius = r
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(rightX, rightTopH + gap),
            size = Size(rightW, h - rightTopH - gap),
            cornerRadius = r
        )
    }
}

@Composable
private fun SplitPartsGlyph(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val gap = w * 0.09f
        val halfW = (w - gap) / 2f
        val halfH = (h - gap) / 2f
        val r = CornerRadius(w * 0.14f, w * 0.14f)

        // Left full pane
        drawRoundRect(
            color = color,
            topLeft = Offset.Zero,
            size = Size(halfW, h),
            cornerRadius = r
        )
        // Top right pane
        drawRoundRect(
            color = color,
            topLeft = Offset(halfW + gap, 0f),
            size = Size(halfW, halfH),
            cornerRadius = r
        )
        // Bottom right pane
        drawRoundRect(
            color = color,
            topLeft = Offset(halfW + gap, halfH + gap),
            size = Size(halfW, halfH),
            cornerRadius = r
        )
    }
}

@Composable
private fun DepthMountainsGlyph(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val baseLine = h * 0.86f

        // Left foreground peak
        val leftPeak = Path().apply {
            moveTo(w * 0.06f, baseLine)
            lineTo(w * 0.34f, h * 0.38f)
            lineTo(w * 0.62f, baseLine)
            close()
        }
        drawPath(leftPeak, color = color)

        // Right taller mountain peak overlapping
        val rightPeak = Path().apply {
            moveTo(w * 0.34f, baseLine)
            lineTo(w * 0.64f, h * 0.16f)
            lineTo(w * 0.94f, baseLine)
            close()
        }
        drawPath(rightPeak, color = color.copy(alpha = 0.85f))
    }
}
