package com.example.ui.components

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.KeyframeEvaluator
import com.example.engine.Mesh3DParser
import com.example.model.AnimatableProperty
import com.example.model.ClipItem
import com.example.model.ClipType
import com.example.model.Library3DModel
import com.example.model.Model3DConfig
import com.example.model.Model3DSource
import com.example.model.Primitive3DShape
import com.example.model.RenderMode3D
import com.example.ui.theme.CutRose
import com.example.ui.theme.KeyframeViolet
import com.example.ui.theme.MotionCyan
import com.example.ui.theme.PlayheadAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioDarkBg
import com.example.ui.theme.StudioSurfaceVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Model3DEditorPanel(
    selectedClip: ClipItem?,
    currentTimeMs: Long,
    onUpdateModel3D: (String, Model3DConfig) -> Unit,
    onUpdateKeyframeProperty: (AnimatableProperty, Float) -> Unit,
    onToggleKeyframeProperty: (AnimatableProperty) -> Unit,
    onOpenAdd3DModal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var importError by remember { mutableStateOf<String?>(null) }

    val modelFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        val clip = selectedClip ?: return@rememberLauncherForActivityResult
        val cfg = clip.model3dConfig ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            scope.launch {
                val resolvedName = withContext(Dispatchers.IO) {
                    var name: String? = null
                    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                        if (c.moveToFirst()) name = c.getString(0)
                    }
                    name ?: uri.lastPathSegment ?: "model.obj"
                }
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }
                    Mesh3DParser.loadFromUri(context, uri.toString(), resolvedName)
                }
                result.onSuccess {
                    importError = null
                    onUpdateModel3D(
                        clip.id,
                        cfg.copy(
                            source = Model3DSource.IMPORTED_FILE,
                            importedUri = uri.toString(),
                            importedFileName = resolvedName
                        )
                    )
                }.onFailure { err ->
                    importError = err.localizedMessage ?: "Failed to load .obj/.gltf/.glb file"
                }
            }
        }
    }

    val textureImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        val clip = selectedClip ?: return@rememberLauncherForActivityResult
        val cfg = clip.model3dConfig ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            onUpdateModel3D(
                clip.id,
                cfg.copy(imageUri = uri.toString())
            )
        }
    }

    if (selectedClip == null || selectedClip.type != ClipType.MODEL_3D || selectedClip.model3dConfig == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(StudioDarkBg)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.ViewInAr, contentDescription = null, tint = MotionCyan, modifier = Modifier.size(34.dp))
                Text(
                    text = "Select a 3D Model layer or create/import one",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Button(
                    onClick = onOpenAdd3DModal,
                    colors = ButtonDefaults.buttonColors(containerColor = MotionCyan, contentColor = Color.Black),
                    modifier = Modifier.testTag("studio_3d_create_button")
                ) {
                    Icon(Icons.Default.AddBox, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Create or Import .OBJ / .GLB Model", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
        return
    }

    val baseCfg = selectedClip.model3dConfig
    val liveCfg = KeyframeEvaluator.evaluateModel3D(selectedClip, currentTimeMs) ?: baseCfg
    val swatches = listOf(0xFF00E5FF, 0xFF10E878, 0xFFD580FF, 0xFFFFB020, 0xFFFF4D6D, 0xFFF1F5F9)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(StudioDarkBg)
            .padding(10.dp)
            .testTag("model_3d_editor_panel"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header + Import / New action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "3D Studio Inspector",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = when (baseCfg.source) {
                        Model3DSource.LIBRARY_MODEL -> "${baseCfg.source.label} • ${baseCfg.libraryModel.displayName}"
                        else -> "${baseCfg.source.label} • ${baseCfg.importedFileName ?: baseCfg.primitiveShape.label}"
                    },
                    color = MotionCyan,
                    fontSize = 11.sp
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = { modelFileLauncher.launch(arrayOf("*/*")) },
                    border = BorderStroke(1.dp, MotionCyan),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MotionCyan),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Import .OBJ/.GLB", fontSize = 10.sp)
                }
                OutlinedButton(
                    onClick = onOpenAdd3DModal,
                    border = BorderStroke(1.dp, KeyframeViolet),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = KeyframeViolet),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("+ New 3D", fontSize = 10.sp)
                }
            }
        }

        if (importError != null) {
            Text(text = importError!!, color = CutRose, fontSize = 11.sp)
        }

        // Source-specific quick editor (Primitives, Library Models, or Extruded Text)
        if (baseCfg.source == Model3DSource.PRIMITIVE) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Primitive3DShape.entries.forEach { shape ->
                    val active = shape == baseCfg.primitiveShape
                    Surface(
                        color = if (active) MotionCyan.copy(alpha = 0.2f) else StudioSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (active) MotionCyan else StudioBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onUpdateModel3D(selectedClip.id, baseCfg.copy(primitiveShape = shape)) }
                    ) {
                        Text(
                            text = shape.label,
                            color = if (active) Color.White else Color.LightGray,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        } else if (baseCfg.source == Model3DSource.LIBRARY_MODEL) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Library3DModel.entries.forEach { libModel ->
                    val active = libModel == baseCfg.libraryModel
                    Surface(
                        color = if (active) MotionCyan.copy(alpha = 0.2f) else StudioSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (active) MotionCyan else StudioBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onUpdateModel3D(selectedClip.id, baseCfg.copy(libraryModel = libModel)) }
                    ) {
                        Text(
                            text = libModel.displayName,
                            color = if (active) Color.White else Color.LightGray,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        } else if (baseCfg.source == Model3DSource.EXTRUDED_TEXT) {
            OutlinedTextField(
                value = baseCfg.extrudedText,
                onValueChange = { onUpdateModel3D(selectedClip.id, baseCfg.copy(extrudedText = it)) },
                label = { Text("Extruded 3D Text", fontSize = 11.sp) },
                singleLine = false,
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MotionCyan,
                    unfocusedBorderColor = StudioBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("panel_extruded_text_3d_input")
            )
        }

        // Universal Image Texture controls on ALL 3D objects
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = {
                    textureImageLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                border = BorderStroke(1.dp, PlayheadAmber),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PlayheadAmber),
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .testTag("panel_3d_texture_button")
            ) {
                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.size(5.dp))
                Text(
                    text = if (baseCfg.imageUri != null) "Change Texture Image" else "Apply Image Texture",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (baseCfg.imageUri != null) {
                OutlinedButton(
                    onClick = { onUpdateModel3D(selectedClip.id, baseCfg.copy(imageUri = null)) },
                    border = BorderStroke(1.dp, CutRose),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CutRose),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("panel_3d_clear_texture_button")
                ) {
                    Text("Clear", fontSize = 10.sp)
                }
            }
        }
        if (baseCfg.imageUri != null) {
            Keyframe3DSliderRow(
                label = "Tex Mix",
                value = baseCfg.textureMix,
                range = 0f..1f,
                unit = "",
                hasKf = false,
                showKfButton = false,
                onValueChange = { onUpdateModel3D(selectedClip.id, baseCfg.copy(textureMix = it)) },
                onToggleKf = {}
            )
        }

        // Keyframeable 3D Transform Sliders
        Keyframe3DSliderRow(
            label = "Pitch X",
            value = liveCfg.rotationX,
            range = -180f..180f,
            unit = "°",
            hasKf = selectedClip.hasKeyframeAt(AnimatableProperty.ROTATION_X_3D, currentTimeMs),
            onValueChange = { onUpdateKeyframeProperty(AnimatableProperty.ROTATION_X_3D, it) },
            onToggleKf = { onToggleKeyframeProperty(AnimatableProperty.ROTATION_X_3D) }
        )
        Keyframe3DSliderRow(
            label = "Yaw Y",
            value = liveCfg.rotationY,
            range = -180f..180f,
            unit = "°",
            hasKf = selectedClip.hasKeyframeAt(AnimatableProperty.ROTATION_Y_3D, currentTimeMs),
            onValueChange = { onUpdateKeyframeProperty(AnimatableProperty.ROTATION_Y_3D, it) },
            onToggleKf = { onToggleKeyframeProperty(AnimatableProperty.ROTATION_Y_3D) }
        )
        Keyframe3DSliderRow(
            label = "Roll Z",
            value = liveCfg.rotationZ,
            range = -180f..180f,
            unit = "°",
            hasKf = selectedClip.hasKeyframeAt(AnimatableProperty.ROTATION_Z_3D, currentTimeMs),
            onValueChange = { onUpdateKeyframeProperty(AnimatableProperty.ROTATION_Z_3D, it) },
            onToggleKf = { onToggleKeyframeProperty(AnimatableProperty.ROTATION_Z_3D) }
        )
        Keyframe3DSliderRow(
            label = "Extrude",
            value = liveCfg.extrusionDepth,
            range = 0.05f..1.5f,
            unit = "",
            hasKf = selectedClip.hasKeyframeAt(AnimatableProperty.EXTRUSION_3D, currentTimeMs),
            onValueChange = { onUpdateKeyframeProperty(AnimatableProperty.EXTRUSION_3D, it) },
            onToggleKf = { onToggleKeyframeProperty(AnimatableProperty.EXTRUSION_3D) }
        )
        Keyframe3DSliderRow(
            label = "Depth Z",
            value = liveCfg.depthZ,
            range = -2.0f..2.0f,
            unit = "",
            hasKf = selectedClip.hasKeyframeAt(AnimatableProperty.DEPTH_Z_3D, currentTimeMs),
            onValueChange = { onUpdateKeyframeProperty(AnimatableProperty.DEPTH_Z_3D, it) },
            onToggleKf = { onToggleKeyframeProperty(AnimatableProperty.DEPTH_Z_3D) }
        )
        Keyframe3DSliderRow(
            label = "Cam FOV",
            value = liveCfg.fovDegrees,
            range = 20f..110f,
            unit = "°",
            hasKf = selectedClip.hasKeyframeAt(AnimatableProperty.FOV_3D, currentTimeMs),
            onValueChange = { onUpdateKeyframeProperty(AnimatableProperty.FOV_3D, it) },
            onToggleKf = { onToggleKeyframeProperty(AnimatableProperty.FOV_3D) }
        )

        // Render Mode + Wireframe Edge toggle + Scale
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RenderMode3D.entries.forEach { mode ->
                    val active = mode == baseCfg.renderMode
                    Surface(
                        color = if (active) KeyframeViolet.copy(alpha = 0.25f) else StudioSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (active) KeyframeViolet else StudioBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onUpdateModel3D(selectedClip.id, baseCfg.copy(renderMode = mode)) }
                            .testTag("render_mode_${mode.name.lowercase()}")
                    ) {
                        Text(
                            text = mode.label,
                            color = if (active) Color.White else Color.LightGray,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Edges", color = Color.LightGray, fontSize = 10.sp)
                Switch(
                    checked = baseCfg.showEdges,
                    onCheckedChange = { onUpdateModel3D(selectedClip.id, baseCfg.copy(showEdges = it)) },
                    colors = SwitchDefaults.colors(checkedThumbColor = MotionCyan)
                )
            }
        }

        // Material Color + Scale + Light Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Color", color = Color.LightGray, fontSize = 10.sp)
            swatches.forEach { hex ->
                val selected = hex == baseCfg.materialColorHex
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(hex))
                        .border(2.dp, if (selected) Color.White else Color.Transparent, CircleShape)
                        .clickable { onUpdateModel3D(selectedClip.id, baseCfg.copy(materialColorHex = hex)) }
                )
            }
        }

        Keyframe3DSliderRow(
            label = "3D Scale",
            value = baseCfg.modelScale,
            range = 0.3f..2.8f,
            unit = "x",
            hasKf = false,
            showKfButton = false,
            onValueChange = { onUpdateModel3D(selectedClip.id, baseCfg.copy(modelScale = it)) },
            onToggleKf = {}
        )
        Keyframe3DSliderRow(
            label = "Light Dir",
            value = baseCfg.lightAzimuthDeg,
            range = -180f..180f,
            unit = "°",
            hasKf = false,
            showKfButton = false,
            onValueChange = { onUpdateModel3D(selectedClip.id, baseCfg.copy(lightAzimuthDeg = it)) },
            onToggleKf = {}
        )
        Keyframe3DSliderRow(
            label = "Metallic",
            value = baseCfg.metallic,
            range = 0f..1f,
            unit = "",
            hasKf = false,
            showKfButton = false,
            onValueChange = { onUpdateModel3D(selectedClip.id, baseCfg.copy(metallic = it)) },
            onToggleKf = {}
        )
    }
}

@Composable
private fun Keyframe3DSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    unit: String,
    hasKf: Boolean,
    showKfButton: Boolean = true,
    onValueChange: (Float) -> Unit,
    onToggleKf: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            color = Color.LightGray,
            fontSize = 11.sp,
            modifier = Modifier.size(width = 58.dp, height = 16.dp)
        )
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = if (hasKf) KeyframeViolet else MotionCyan,
                activeTrackColor = if (hasKf) KeyframeViolet else MotionCyan,
                inactiveTrackColor = StudioBorder
            ),
            modifier = Modifier.weight(1f)
        )
        val formatted = if (unit == "°") {
            "${value.toInt()}°"
        } else {
            String.format(Locale.US, "%.2f%s", value, unit)
        }
        Text(
            text = formatted,
            color = MotionCyan,
            fontSize = 10.sp,
            modifier = Modifier.size(width = 38.dp, height = 14.dp)
        )
        if (showKfButton) {
            IconButton(onClick = onToggleKf, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Diamond,
                    contentDescription = "Toggle $label Keyframe",
                    tint = if (hasKf) KeyframeViolet else Color.Gray,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
