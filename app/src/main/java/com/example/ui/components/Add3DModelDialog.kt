package com.example.ui.components

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import com.example.data.DeviceMediaScanner
import com.example.engine.Mesh3DParser
import com.example.engine.Mesh3DRenderer.drawModel3D
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
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioDarkBg
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Add3DModelDialog(
    currentTimeMs: Long,
    onCreate3DClip: (ClipItem) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedSource by remember { mutableStateOf(Model3DSource.PRIMITIVE) }
    var selectedPrimitive by remember { mutableStateOf(Primitive3DShape.CUBE) }
    var selectedLibraryModel by remember { mutableStateOf(Library3DModel.ROCKET) }
    var extrudedText by remember { mutableStateOf("NOVA 3D") }
    var extrusionDepth by remember { mutableFloatStateOf(0.42f) }
    var textureMix by remember { mutableFloatStateOf(0.85f) }
    var materialColorHex by remember { mutableLongStateOf(0xFF00E5FF) }

    var importedUri by remember { mutableStateOf<String?>(null) }
    var importedFileName by remember { mutableStateOf<String?>(null) }
    var importedStats by remember { mutableStateOf<String?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }
    var isImporting by remember { mutableStateOf(false) }

    var imagePlaneUri by remember { mutableStateOf<String?>(null) }

    val modelFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            isImporting = true
            importError = null
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
                isImporting = false
                result.onSuccess { mesh ->
                    importedUri = uri.toString()
                    importedFileName = resolvedName
                    importedStats = "${mesh.vertices.size} vertices • ${mesh.faces.size} triangles"
                    importError = null
                }.onFailure { err ->
                    importError = err.localizedMessage ?: "Unsupported 3D file (use .obj, .gltf, or .glb)"
                }
            }
        }
    }

    val imagePlaneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            imagePlaneUri = uri.toString()
        }
    }

    val previewTextureBmp by produceState<ImageBitmap?>(initialValue = null, key1 = imagePlaneUri) {
        value = imagePlaneUri?.let { uri ->
            DeviceMediaScanner.loadFullImageBitmap(context, uri, isVideoHint = false)
        }
    }

    val previewConfig = remember(
        selectedSource,
        selectedPrimitive,
        selectedLibraryModel,
        extrudedText,
        extrusionDepth,
        textureMix,
        materialColorHex,
        importedUri,
        importedFileName,
        imagePlaneUri
    ) {
        Model3DConfig(
            source = selectedSource,
            primitiveShape = selectedPrimitive,
            libraryModel = selectedLibraryModel,
            importedUri = importedUri,
            importedFileName = importedFileName,
            extrudedText = extrudedText,
            imageUri = imagePlaneUri,
            textureMix = textureMix,
            rotationX = 20f,
            rotationY = -32f,
            extrusionDepth = extrusionDepth,
            materialColorHex = materialColorHex,
            renderMode = RenderMode3D.SHADED
        )
    }

    val previewMesh = remember(previewConfig) {
        Mesh3DParser.getOrBuildMesh(context, previewConfig, extrusionDepth)
    }

    val swatches = listOf(0xFF00E5FF, 0xFF10E878, 0xFFD580FF, 0xFFFFB020, 0xFFFF4D6D, 0xFFF1F5F9)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .testTag("add_3d_model_dialog"),
            color = StudioDarkBg,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, StudioBorder)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.ViewInAr, contentDescription = null, tint = MotionCyan)
                        Text(
                            text = "Create or Import 3D Model",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Live 3D Mesh Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(145.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(StudioSurface)
                        .border(1.dp, StudioBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        translate(left = size.width / 2f, top = size.height / 2f) {
                            drawModel3D(
                                mesh = previewMesh,
                                config = previewConfig,
                                viewportSize = size.minDimension * 1.1f,
                                planeImageBitmap = previewTextureBmp
                            )
                        }
                    }
                    Text(
                        text = previewMesh.name,
                        color = Color.LightGray,
                        fontSize = 10.sp,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3D Source Type Selector
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Model3DSource.entries.forEach { src ->
                        val active = src == selectedSource
                        Surface(
                            color = if (active) MotionCyan.copy(alpha = 0.22f) else StudioSurfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (active) MotionCyan else StudioBorder),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedSource = src }
                                .testTag("source_3d_${src.name.lowercase()}")
                        ) {
                            Text(
                                text = src.label,
                                color = if (active) MotionCyan else Color.LightGray,
                                fontSize = 11.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (selectedSource) {
                    Model3DSource.PRIMITIVE -> {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Primitive3DShape.entries.forEach { prim ->
                                val active = prim == selectedPrimitive
                                Surface(
                                    color = if (active) KeyframeViolet.copy(alpha = 0.25f) else StudioCardBg,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (active) KeyframeViolet else StudioBorder),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedPrimitive = prim }
                                        .testTag("prim_3d_${prim.name.lowercase()}")
                                ) {
                                    Text(
                                        text = prim.label,
                                        color = if (active) Color.White else Color.LightGray,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    Model3DSource.LIBRARY_MODEL -> {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Library3DModel.entries.forEach { libModel ->
                                val active = libModel == selectedLibraryModel
                                Surface(
                                    color = if (active) MotionCyan.copy(alpha = 0.22f) else StudioCardBg,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (active) MotionCyan else StudioBorder),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedLibraryModel = libModel }
                                        .testTag("lib_3d_${libModel.name.lowercase()}")
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)) {
                                        Text(
                                            text = libModel.displayName,
                                            color = if (active) Color.White else Color.LightGray,
                                            fontSize = 11.sp,
                                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                        )
                                        Text(
                                            text = libModel.category,
                                            color = if (active) MotionCyan else Color.Gray,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Model3DSource.IMPORTED_FILE -> {
                        OutlinedButton(
                            onClick = { modelFileLauncher.launch(arrayOf("*/*")) },
                            border = BorderStroke(1.dp, MotionCyan),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MotionCyan),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("import_3d_file_button")
                        ) {
                            if (isImporting) {
                                CircularProgressIndicator(color = MotionCyan, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = if (importedFileName != null) "Replace Mesh ($importedFileName)"
                                else "Import .OBJ / .GLTF / .GLB File",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (importedStats != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "Loaded: $importedStats", color = Color(0xFF10E878), fontSize = 11.sp)
                        }
                        if (importError != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = importError!!, color = CutRose, fontSize = 11.sp)
                        }
                    }

                    Model3DSource.EXTRUDED_TEXT -> {
                        OutlinedTextField(
                            value = extrudedText,
                            onValueChange = { if (it.length <= 12) extrudedText = it },
                            label = { Text("3D Text", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.TextFields, contentDescription = null, tint = MotionCyan) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MotionCyan,
                                unfocusedBorderColor = StudioBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("extruded_text_3d_input")
                        )
                    }

                    Model3DSource.IMAGE_PLANE -> {
                        Text(
                            text = "3D Photo Card projects your selected image onto a dual-sided 3D card.",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Universal Image Texture Picker for ALL 3D objects
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            imagePlaneLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        border = BorderStroke(1.dp, PlayheadAmber),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PlayheadAmber),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_3d_plane_image_button")
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = if (imagePlaneUri != null) "Change Image Texture" else "Apply Image Texture",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (imagePlaneUri != null) {
                        OutlinedButton(
                            onClick = { imagePlaneUri = null },
                            border = BorderStroke(1.dp, CutRose),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CutRose)
                        ) {
                            Text("Clear", fontSize = 11.sp)
                        }
                    }
                }

                if (imagePlaneUri != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Texture Mix", color = Color.LightGray, fontSize = 11.sp)
                        Slider(
                            value = textureMix,
                            onValueChange = { textureMix = it },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(thumbColor = PlayheadAmber, activeTrackColor = PlayheadAmber),
                            modifier = Modifier.weight(1f)
                        )
                        Text("${(textureMix * 100).toInt()}%", color = PlayheadAmber, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Extrusion depth slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Extrusion", color = Color.LightGray, fontSize = 11.sp)
                    Slider(
                        value = extrusionDepth,
                        onValueChange = { extrusionDepth = it },
                        valueRange = 0.08f..1.2f,
                        colors = SliderDefaults.colors(thumbColor = MotionCyan, activeTrackColor = MotionCyan),
                        modifier = Modifier.weight(1f)
                    )
                    Text(String.format(java.util.Locale.US, "%.2f", extrusionDepth), color = MotionCyan, fontSize = 11.sp)
                }

                // Material Color Swatches
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Material", color = Color.LightGray, fontSize = 11.sp)
                    swatches.forEach { hex ->
                        val selected = hex == materialColorHex
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .border(2.dp, if (selected) Color.White else Color.Transparent, CircleShape)
                                .clickable { materialColorHex = hex }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        val title = when (selectedSource) {
                            Model3DSource.PRIMITIVE -> "3D ${selectedPrimitive.label}"
                            Model3DSource.LIBRARY_MODEL -> "3D ${selectedLibraryModel.displayName}"
                            Model3DSource.IMPORTED_FILE -> importedFileName ?: "3D Imported Mesh"
                            Model3DSource.EXTRUDED_TEXT -> "3D Text: ${extrudedText.ifBlank { "3D" }}"
                            Model3DSource.IMAGE_PLANE -> "3D Image Card"
                        }
                        onCreate3DClip(
                            ClipItem(
                                title = title,
                                type = ClipType.MODEL_3D,
                                mediaUri = imagePlaneUri ?: importedUri,
                                colorHex = materialColorHex,
                                startTimeMs = currentTimeMs,
                                durationMs = 5000L,
                                model3dConfig = previewConfig
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10E878), contentColor = Color.Black),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_3d_model_confirm_button")
                ) {
                    Text("Add 3D Layer to Timeline", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
