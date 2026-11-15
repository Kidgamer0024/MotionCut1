package com.example.engine

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.example.model.Face3D
import com.example.model.Library3DModel
import com.example.model.Mesh3DData
import com.example.model.Model3DConfig
import com.example.model.Model3DSource
import com.example.model.Primitive3DShape
import com.example.model.Vec3
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure-Kotlin 3D mesh generator and file parser for:
 * - Built-in 3D primitives (Cube, Sphere, Pyramid, Torus, Cylinder, Octahedron, Phone Slab)
 * - 3D Extruded Text blocks
 * - 3D Image Planes
 * - Wavefront .OBJ, glTF 2.0 (.gltf JSON), and binary glTF (.glb) files from storage
 */
object Mesh3DParser {

    private const val MAX_FACES = 6000
    private val meshCache = ConcurrentHashMap<String, Mesh3DData>()

    fun registerCustomMesh(key: String, mesh: Mesh3DData) {
        meshCache[key] = normalizeMesh(mesh)
    }

    fun getCachedMesh(key: String?): Mesh3DData? = if (key == null) null else meshCache[key]

    fun getOrBuildMesh(context: Context?, config: Model3DConfig, evaluatedExtrusion: Float = config.extrusionDepth): Mesh3DData {
        return when (config.source) {
            Model3DSource.PRIMITIVE -> {
                val cacheKey = "prim_${config.primitiveShape.name}_${(evaluatedExtrusion * 20).toInt()}"
                meshCache.getOrPut(cacheKey) {
                    buildPrimitiveMesh(config.primitiveShape, evaluatedExtrusion)
                }
            }
            Model3DSource.LIBRARY_MODEL -> {
                val cacheKey = "lib_${config.libraryModel.name}_${(evaluatedExtrusion * 20).toInt()}"
                meshCache.getOrPut(cacheKey) {
                    buildLibraryMesh(config.libraryModel, evaluatedExtrusion)
                }
            }
            Model3DSource.EXTRUDED_TEXT -> {
                val safeText = config.extrudedText.ifBlank { "3D" }.take(16)
                val cacheKey = "txt_${safeText}_${(evaluatedExtrusion * 20).toInt()}"
                meshCache.getOrPut(cacheKey) {
                    buildExtrudedTextMesh(safeText, evaluatedExtrusion)
                }
            }
            Model3DSource.IMAGE_PLANE -> {
                val cacheKey = "img_plane_${(evaluatedExtrusion * 20).toInt()}"
                meshCache.getOrPut(cacheKey) {
                    buildImagePlaneMesh(evaluatedExtrusion)
                }
            }
            Model3DSource.IMPORTED_FILE -> {
                val uriStr = config.importedUri
                if (uriStr != null) {
                    meshCache[uriStr]?.let { return it }
                    if (context != null) {
                        val loaded = loadFromUri(context, uriStr, config.importedFileName).getOrNull()
                        if (loaded != null) {
                            meshCache[uriStr] = loaded
                            return loaded
                        }
                    }
                }
                buildPrimitiveMesh(Primitive3DShape.OCTAHEDRON, evaluatedExtrusion)
            }
        }
    }

    fun loadFromUri(context: Context, uriString: String, fileNameHint: String? = null): Result<Mesh3DData> {
        return runCatching {
            val uri = Uri.parse(uriString)
            val displayName = fileNameHint ?: uri.lastPathSegment ?: "model"
            val lower = displayName.lowercase()
            val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
                val out = ByteArrayOutputStream()
                val buf = ByteArray(16 * 1024)
                var read = input.read(buf)
                while (read != -1) {
                    out.write(buf, 0, read)
                    read = input.read(buf)
                }
                out.toByteArray()
            } ?: throw IllegalStateException("Could not open selected 3D file")

            val parsed = when {
                lower.endsWith(".glb") || (bytes.size >= 4 && bytes[0] == 'g'.code.toByte() && bytes[1] == 'l'.code.toByte() && bytes[2] == 'T'.code.toByte() && bytes[3] == 'F'.code.toByte()) ->
                    parseGlb(bytes, displayName)
                lower.endsWith(".gltf") || bytes.decodeToString(0, minOf(bytes.size, 64)).trimStart().startsWith("{") ->
                    parseGltf(bytes.decodeToString(), displayName)
                else ->
                    parseObj(bytes.decodeToString(), displayName)
            }
            val normalized = normalizeMesh(parsed)
            meshCache[uriString] = normalized
            normalized
        }
    }

    fun parseObj(objContent: String, name: String = "Imported OBJ"): Mesh3DData {
        val verts = ArrayList<Vec3>()
        val faces = ArrayList<Face3D>()

        objContent.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEach
            if (line.startsWith("v ")) {
                val parts = line.split(Regex("\\s+"))
                if (parts.size >= 4) {
                    val x = parts[1].toFloatOrNull() ?: 0f
                    val y = parts[2].toFloatOrNull() ?: 0f
                    val z = parts[3].toFloatOrNull() ?: 0f
                    verts.add(Vec3(x, -y, z))
                }
            } else if (line.startsWith("f ") && faces.size < MAX_FACES) {
                val parts = line.split(Regex("\\s+")).drop(1)
                val idx = parts.mapNotNull { token ->
                    val rawIdx = token.substringBefore('/').toIntOrNull() ?: return@mapNotNull null
                    val resolved = if (rawIdx < 0) verts.size + rawIdx else rawIdx - 1
                    resolved.takeIf { it in verts.indices }
                }
                if (idx.size >= 3) {
                    for (k in 1 until idx.size - 1) {
                        if (faces.size < MAX_FACES) {
                            faces.add(Face3D(idx[0], idx[k], idx[k + 1]))
                        }
                    }
                }
            }
        }

        if (verts.isEmpty() || faces.isEmpty()) {
            throw IllegalArgumentException("OBJ file contains no valid vertices or faces")
        }
        return normalizeMesh(Mesh3DData(verts, faces, name))
    }

    fun parseGlb(bytes: ByteArray, name: String = "Imported GLB"): Mesh3DData {
        if (bytes.size < 20) throw IllegalArgumentException("Invalid GLB header")
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val magic = buf.int
        if (magic != 0x46546C67) throw IllegalArgumentException("File is not a binary glTF (.glb)")
        buf.int // version
        val totalLen = buf.int

        var jsonText: String? = null
        var binChunk: ByteArray? = null

        while (buf.position() + 8 <= bytes.size && buf.position() < totalLen) {
            val chunkLen = buf.int
            val chunkType = buf.int
            if (chunkLen < 0 || buf.position() + chunkLen > bytes.size) break
            val chunkData = ByteArray(chunkLen)
            buf.get(chunkData)
            if (chunkType == 0x4E4F534A) { // "JSON"
                jsonText = chunkData.decodeToString()
            } else if (chunkType == 0x004E4942) { // "BIN\0"
                binChunk = chunkData
            }
        }

        val jsonStr = jsonText ?: throw IllegalArgumentException("GLB missing JSON chunk")
        return parseGltfInternal(JSONObject(jsonStr), binChunk, name)
    }

    fun parseGltf(jsonContent: String, name: String = "Imported GLTF"): Mesh3DData {
        val root = JSONObject(jsonContent)
        val buffers = root.optJSONArray("buffers")
        var binData: ByteArray? = null
        if (buffers != null && buffers.length() > 0) {
            val uri = buffers.getJSONObject(0).optString("uri", "")
            if (uri.startsWith("data:") && uri.contains(";base64,")) {
                val b64 = uri.substringAfter(";base64,")
                binData = Base64.decode(b64, Base64.DEFAULT)
            }
        }
        return parseGltfInternal(root, binData, name)
    }

    private fun parseGltfInternal(root: JSONObject, binChunk: ByteArray?, name: String): Mesh3DData {
        val meshes = root.optJSONArray("meshes")
            ?: throw IllegalArgumentException("glTF contains no meshes")
        val accessors = root.optJSONArray("accessors")
            ?: throw IllegalArgumentException("glTF missing accessors")
        val bufferViews = root.optJSONArray("bufferViews")
            ?: throw IllegalArgumentException("glTF missing bufferViews")
        val buffer = binChunk
            ?: throw IllegalArgumentException("glTF external binary .bin buffers are not embedded; export as .glb or embedded .gltf")

        val allVerts = ArrayList<Vec3>()
        val allFaces = ArrayList<Face3D>()

        for (mIdx in 0 until meshes.length()) {
            val prims = meshes.getJSONObject(mIdx).optJSONArray("primitives") ?: continue
            for (pIdx in 0 until prims.length()) {
                val prim = prims.getJSONObject(pIdx)
                val attrs = prim.optJSONObject("attributes") ?: continue
                val posAccIdx = attrs.optInt("POSITION", -1)
                if (posAccIdx < 0) continue

                val posAccessor = accessors.getJSONObject(posAccIdx)
                val posView = bufferViews.getJSONObject(posAccessor.getInt("bufferView"))
                val posOffset = posView.optInt("byteOffset", 0) + posAccessor.optInt("byteOffset", 0)
                val posCount = posAccessor.getInt("count")
                val stride = posView.optInt("byteStride", 12).coerceAtLeast(12)

                val baseVertIndex = allVerts.size
                val bb = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN)
                for (v in 0 until posCount) {
                    val off = posOffset + v * stride
                    if (off + 12 > buffer.size) break
                    val x = bb.getFloat(off)
                    val y = bb.getFloat(off + 4)
                    val z = bb.getFloat(off + 8)
                    allVerts.add(Vec3(x, -y, z))
                }

                val idxAccIdx = prim.optInt("indices", -1)
                if (idxAccIdx >= 0) {
                    val idxAccessor = accessors.getJSONObject(idxAccIdx)
                    val idxView = bufferViews.getJSONObject(idxAccessor.getInt("bufferView"))
                    val idxOffset = idxView.optInt("byteOffset", 0) + idxAccessor.optInt("byteOffset", 0)
                    val idxCount = idxAccessor.getInt("count")
                    val compType = idxAccessor.getInt("componentType")

                    val rawIndices = ArrayList<Int>(idxCount)
                    for (i in 0 until idxCount) {
                        val parsedIdx = when (compType) {
                            5121 -> if (idxOffset + i < buffer.size) (buffer[idxOffset + i].toInt() and 0xFF) else 0
                            5123 -> if (idxOffset + i * 2 + 2 <= buffer.size) (bb.getShort(idxOffset + i * 2).toInt() and 0xFFFF) else 0
                            5125 -> if (idxOffset + i * 4 + 4 <= buffer.size) bb.getInt(idxOffset + i * 4) else 0
                            else -> 0
                        }
                        rawIndices.add(baseVertIndex + parsedIdx)
                    }
                    var t = 0
                    while (t + 2 < rawIndices.size && allFaces.size < MAX_FACES) {
                        val i0 = rawIndices[t]
                        val i1 = rawIndices[t + 1]
                        val i2 = rawIndices[t + 2]
                        if (i0 in allVerts.indices && i1 in allVerts.indices && i2 in allVerts.indices) {
                            allFaces.add(Face3D(i0, i1, i2))
                        }
                        t += 3
                    }
                } else {
                    var t = 0
                    while (t + 2 < posCount && allFaces.size < MAX_FACES) {
                        allFaces.add(Face3D(baseVertIndex + t, baseVertIndex + t + 1, baseVertIndex + t + 2))
                        t += 3
                    }
                }
            }
        }

        if (allVerts.isEmpty() || allFaces.isEmpty()) {
            throw IllegalArgumentException("glTF contains no renderable triangle geometry")
        }
        return normalizeMesh(Mesh3DData(allVerts, allFaces, name))
    }

    fun normalizeMesh(mesh: Mesh3DData): Mesh3DData {
        if (mesh.vertices.isEmpty()) return mesh
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var maxZ = -Float.MAX_VALUE

        for (v in mesh.vertices) {
            if (v.x < minX) minX = v.x
            if (v.y < minY) minY = v.y
            if (v.z < minZ) minZ = v.z
            if (v.x > maxX) maxX = v.x
            if (v.y > maxY) maxY = v.y
            if (v.z > maxZ) maxZ = v.z
        }

        val cx = (minX + maxX) * 0.5f
        val cy = (minY + maxY) * 0.5f
        val cz = (minZ + maxZ) * 0.5f
        val span = max(maxX - minX, max(maxY - minY, maxZ - minZ)).coerceAtLeast(1e-4f)
        val scale = 1.45f / span

        val normVerts = mesh.vertices.map { v ->
            Vec3((v.x - cx) * scale, (v.y - cy) * scale, (v.z - cz) * scale)
        }
        return mesh.copy(vertices = normVerts)
    }

    fun buildPrimitiveMesh(shape: Primitive3DShape, extrusionDepth: Float = 0.4f): Mesh3DData {
        return when (shape) {
            Primitive3DShape.CUBE -> buildBoxMesh(0.72f, 0.72f, (0.35f + extrusionDepth * 0.7f), "3D Cube")
            Primitive3DShape.PHONE_SLAB -> buildBoxMesh(0.48f, 0.88f, (0.06f + extrusionDepth * 0.14f), "3D Phone")
            Primitive3DShape.PYRAMID -> buildPyramidMesh()
            Primitive3DShape.OCTAHEDRON -> buildOctahedronMesh()
            Primitive3DShape.SPHERE -> buildSphereMesh()
            Primitive3DShape.CYLINDER -> buildCylinderMesh(0.58f, 0.45f + extrusionDepth * 0.6f)
            Primitive3DShape.TORUS -> buildTorusMesh()
        }
    }

    fun buildImagePlaneMesh(extrusionDepth: Float): Mesh3DData {
        val d = (0.04f + extrusionDepth * 0.25f).coerceIn(0.03f, 0.5f)
        return buildBoxMesh(0.85f, 0.58f, d, "3D Image Plane")
    }

    fun buildLibraryMesh(model: Library3DModel, extrusionDepth: Float = 0.4f): Mesh3DData {
        val verts = ArrayList<Vec3>()
        val faces = ArrayList<Face3D>()
        when (model) {
            Library3DModel.ROCKET -> {
                // Sleek fuselage + nose cone + 3 booster fins
                appendCylinderPart(verts, faces, 0f, -0.05f, 0f, 0.24f, 0.42f, 14)
                appendConePart(verts, faces, 0f, -0.66f, 0f, 0.25f, 0.40f, 14)
                appendCylinderPart(verts, faces, 0f, 0.42f, 0f, 0.16f, 0.10f, 12)
                for (i in 0 until 3) {
                    val ang = i * (2f * PI.toFloat() / 3f)
                    appendBox(verts, faces, cos(ang) * 0.30f, 0.28f, 0.10f, 0.20f, 0.04f)
                }
            }
            Library3DModel.SATELLITE -> {
                // Core bus + dish + twin solar arrays
                appendBox(verts, faces, 0f, 0f, 0.22f, 0.22f, 0.22f)
                appendBox(verts, faces, -0.62f, 0f, 0.34f, 0.16f, 0.03f)
                appendBox(verts, faces, 0.62f, 0f, 0.34f, 0.16f, 0.03f)
                appendConePart(verts, faces, 0f, -0.35f, 0f, 0.28f, 0.16f, 14)
            }
            Library3DModel.PORTAL_RING -> {
                // Thick stargate ring + 8 chevrons
                val torus = buildTorusMesh(20, 10)
                verts.addAll(torus.vertices)
                faces.addAll(torus.faces)
                for (i in 0 until 8) {
                    val a = i * (2f * PI.toFloat() / 8f)
                    appendBox(verts, faces, cos(a) * 0.62f, sin(a) * 0.62f, 0.08f, 0.08f, 0.16f)
                }
            }
            Library3DModel.STUDIO_MIC -> {
                // Capsule grille + yoke stand + base
                appendCylinderPart(verts, faces, 0f, -0.25f, 0f, 0.24f, 0.28f, 14)
                appendCylinderPart(verts, faces, 0f, 0.18f, 0f, 0.06f, 0.25f, 10)
                appendCylinderPart(verts, faces, 0f, 0.46f, 0f, 0.34f, 0.05f, 16)
            }
            Library3DModel.HEADPHONES -> {
                // Two earcups + arch segments
                appendCylinderPart(verts, faces, -0.45f, 0.15f, 0f, 0.20f, 0.18f, 14)
                appendCylinderPart(verts, faces, 0.45f, 0.15f, 0f, 0.20f, 0.18f, 14)
                for (s in -4..4) {
                    val a = s * 0.19f
                    appendBox(verts, faces, sin(a) * 0.48f, -cos(a) * 0.48f + 0.10f, 0.09f, 0.06f, 0.09f)
                }
            }
            Library3DModel.SMARTPHONE_RIG -> {
                // Phone slab + left/right cage grips + top mic
                appendBox(verts, faces, 0f, 0f, 0.42f, 0.26f, 0.06f)
                appendBox(verts, faces, -0.52f, 0f, 0.06f, 0.28f, 0.06f)
                appendBox(verts, faces, 0.52f, 0f, 0.06f, 0.28f, 0.06f)
                appendBox(verts, faces, 0f, -0.34f, 0.14f, 0.07f, 0.08f)
            }
            Library3DModel.LAPTOP_PRO -> {
                // Base deck + angled screen lid
                appendBox(verts, faces, 0f, 0.26f, 0.52f, 0.04f, 0.34f)
                appendBox(verts, faces, 0f, -0.14f, 0.52f, 0.34f, 0.03f)
            }
            Library3DModel.TROPHY_CUP -> {
                // Wide cup + stem + pedestal
                appendCylinderPart(verts, faces, 0f, -0.28f, 0f, 0.34f, 0.26f, 16)
                appendCylinderPart(verts, faces, 0f, 0.08f, 0f, 0.10f, 0.16f, 12)
                appendBox(verts, faces, 0f, 0.36f, 0.28f, 0.14f, 0.28f)
                appendBox(verts, faces, -0.40f, -0.28f, 0.08f, 0.14f, 0.05f)
                appendBox(verts, faces, 0.40f, -0.28f, 0.08f, 0.14f, 0.05f)
            }
            Library3DModel.ROYAL_CROWN -> {
                // Band + 6 spires around crown
                appendCylinderPart(verts, faces, 0f, 0.22f, 0f, 0.46f, 0.12f, 16)
                for (i in 0 until 6) {
                    val a = i * (2f * PI.toFloat() / 6f)
                    appendConePart(verts, faces, cos(a) * 0.42f, -0.10f, sin(a) * 0.42f, 0.13f, 0.28f, 6)
                }
            }
            Library3DModel.CRYSTAL_GEM -> {
                val octa = buildOctahedronMesh()
                verts.addAll(octa.vertices.map { Vec3(it.x * 0.72f, it.y * 1.15f, it.z * 0.72f) })
                faces.addAll(octa.faces)
                appendConePart(verts, faces, -0.35f, 0.12f, 0.18f, 0.20f, 0.40f, 6)
                appendConePart(verts, faces, 0.35f, 0.15f, -0.15f, 0.18f, 0.36f, 6)
            }
            Library3DModel.MECHANICAL_GEAR -> {
                val hz = (0.08f + extrusionDepth * 0.18f).coerceIn(0.06f, 0.3f)
                appendCylinderPart(verts, faces, 0f, 0f, 0f, 0.46f, hz, 16)
                for (i in 0 until 10) {
                    val a = i * (2f * PI.toFloat() / 10f)
                    appendBox(verts, faces, cos(a) * 0.56f, sin(a) * 0.56f, 0.10f, 0.10f, hz)
                }
            }
            Library3DModel.LIGHTNING_BOLT -> {
                val hz = (0.08f + extrusionDepth * 0.22f).coerceIn(0.06f, 0.36f)
                appendBox(verts, faces, 0.12f, -0.38f, 0.22f, 0.20f, hz)
                appendBox(verts, faces, 0.0f, -0.04f, 0.30f, 0.14f, hz)
                appendBox(verts, faces, -0.14f, 0.34f, 0.18f, 0.24f, hz)
            }
            Library3DModel.HEART_3D -> {
                val hz = (0.10f + extrusionDepth * 0.25f).coerceIn(0.08f, 0.35f)
                appendCylinderPart(verts, faces, -0.24f, -0.18f, 0f, 0.28f, hz, 12)
                appendCylinderPart(verts, faces, 0.24f, -0.18f, 0f, 0.28f, hz, 12)
                appendConePart(verts, faces, 0f, 0.35f, 0f, 0.50f, -0.52f, 12)
            }
            Library3DModel.PLANET_SATURN -> {
                val sphere = buildSphereMesh(8, 12)
                verts.addAll(sphere.vertices.map { Vec3(it.x * 0.65f, it.y * 0.65f, it.z * 0.65f) })
                faces.addAll(sphere.faces)
                // Flat orbital ring boxes tilted around equator
                val ringSegs = 18
                for (i in 0 until ringSegs) {
                    val a = i * (2f * PI.toFloat() / ringSegs)
                    appendBox(verts, faces, cos(a) * 0.78f, sin(a) * 0.18f, 0.10f, 0.03f, 0.10f)
                }
            }
        }
        return normalizeMesh(Mesh3DData(verts, faces, model.displayName))
    }

    private fun appendCylinderPart(
        verts: MutableList<Vec3>,
        faces: MutableList<Face3D>,
        cx: Float,
        cy: Float,
        cz: Float,
        radius: Float,
        halfHeight: Float,
        segs: Int
    ) {
        val base = verts.size
        val topCenter = base
        val botCenter = base + 1
        verts.add(Vec3(cx, cy - halfHeight, cz))
        verts.add(Vec3(cx, cy + halfHeight, cz))
        val ringStart = base + 2
        for (i in 0..segs) {
            val a = 2f * PI.toFloat() * i / segs
            val x = cx + cos(a) * radius
            val z = cz + sin(a) * radius
            verts.add(Vec3(x, cy - halfHeight, z))
            verts.add(Vec3(x, cy + halfHeight, z))
        }
        for (i in 0 until segs) {
            if (faces.size + 4 > MAX_FACES) return
            val t0 = ringStart + i * 2
            val b0 = t0 + 1
            val t1 = t0 + 2
            val b1 = t0 + 3
            faces.add(Face3D(topCenter, t1, t0))
            faces.add(Face3D(botCenter, b0, b1))
            faces.add(Face3D(t0, b0, b1))
            faces.add(Face3D(t0, b1, t1))
        }
    }

    private fun appendConePart(
        verts: MutableList<Vec3>,
        faces: MutableList<Face3D>,
        cx: Float,
        cy: Float,
        cz: Float,
        radius: Float,
        heightOffset: Float,
        segs: Int
    ) {
        val base = verts.size
        val tipIdx = base
        val baseCenterIdx = base + 1
        verts.add(Vec3(cx, cy - heightOffset, cz))
        verts.add(Vec3(cx, cy + heightOffset * 0.35f, cz))
        val ringStart = base + 2
        for (i in 0..segs) {
            val a = 2f * PI.toFloat() * i / segs
            verts.add(Vec3(cx + cos(a) * radius, cy + heightOffset * 0.35f, cz + sin(a) * radius))
        }
        for (i in 0 until segs) {
            if (faces.size + 2 > MAX_FACES) return
            val p0 = ringStart + i
            val p1 = ringStart + i + 1
            faces.add(Face3D(tipIdx, p0, p1))
            faces.add(Face3D(baseCenterIdx, p1, p0))
        }
    }

    private fun buildBoxMesh(hx: Float, hy: Float, hz: Float, name: String): Mesh3DData {
        val v = listOf(
            Vec3(-hx, -hy, hz), Vec3(hx, -hy, hz), Vec3(hx, hy, hz), Vec3(-hx, hy, hz),
            Vec3(-hx, -hy, -hz), Vec3(hx, -hy, -hz), Vec3(hx, hy, -hz), Vec3(-hx, hy, -hz)
        )
        val f = listOf(
            // Front
            Face3D(0, 1, 2), Face3D(0, 2, 3),
            // Back
            Face3D(5, 4, 7), Face3D(5, 7, 6),
            // Left
            Face3D(4, 0, 3), Face3D(4, 3, 7),
            // Right
            Face3D(1, 5, 6), Face3D(1, 6, 2),
            // Top
            Face3D(4, 5, 1), Face3D(4, 1, 0),
            // Bottom
            Face3D(3, 2, 6), Face3D(3, 6, 7)
        )
        return Mesh3DData(v, f, name)
    }

    private fun buildPyramidMesh(): Mesh3DData {
        val h = 0.72f
        val v = listOf(
            Vec3(0f, -h, 0f),
            Vec3(-h, h * 0.85f, h),
            Vec3(h, h * 0.85f, h),
            Vec3(h, h * 0.85f, -h),
            Vec3(-h, h * 0.85f, -h)
        )
        val f = listOf(
            Face3D(0, 1, 2), Face3D(0, 2, 3), Face3D(0, 3, 4), Face3D(0, 4, 1),
            Face3D(1, 4, 3), Face3D(1, 3, 2)
        )
        return Mesh3DData(v, f, "3D Pyramid")
    }

    private fun buildOctahedronMesh(): Mesh3DData {
        val r = 0.78f
        val v = listOf(
            Vec3(0f, -r, 0f), Vec3(r, 0f, 0f), Vec3(0f, 0f, r),
            Vec3(-r, 0f, 0f), Vec3(0f, 0f, -r), Vec3(0f, r, 0f)
        )
        val f = listOf(
            Face3D(0, 2, 1), Face3D(0, 3, 2), Face3D(0, 4, 3), Face3D(0, 1, 4),
            Face3D(5, 1, 2), Face3D(5, 2, 3), Face3D(5, 3, 4), Face3D(5, 4, 1)
        )
        return Mesh3DData(v, f, "3D Diamond Prism")
    }

    private fun buildSphereMesh(rings: Int = 9, sectors: Int = 14): Mesh3DData {
        val radius = 0.74f
        val verts = ArrayList<Vec3>()
        val faces = ArrayList<Face3D>()
        for (r in 0..rings) {
            val phi = PI.toFloat() * r / rings
            val y = -cos(phi) * radius
            val ringR = sin(phi) * radius
            for (s in 0..sectors) {
                val theta = 2f * PI.toFloat() * s / sectors
                verts.add(Vec3(cos(theta) * ringR, y, sin(theta) * ringR))
            }
        }
        val stride = sectors + 1
        for (r in 0 until rings) {
            for (s in 0 until sectors) {
                val a = r * stride + s
                val b = a + 1
                val c = (r + 1) * stride + s
                val d = c + 1
                faces.add(Face3D(a, c, b))
                faces.add(Face3D(b, c, d))
            }
        }
        return Mesh3DData(verts, faces, "3D Sphere")
    }

    private fun buildCylinderMesh(radius: Float, halfHeight: Float, segs: Int = 18): Mesh3DData {
        val verts = ArrayList<Vec3>()
        val faces = ArrayList<Face3D>()
        val topCenter = 0
        val botCenter = 1
        verts.add(Vec3(0f, -halfHeight, 0f))
        verts.add(Vec3(0f, halfHeight, 0f))
        val ringStart = 2
        for (i in 0..segs) {
            val a = 2f * PI.toFloat() * i / segs
            val x = cos(a) * radius
            val z = sin(a) * radius
            verts.add(Vec3(x, -halfHeight, z))
            verts.add(Vec3(x, halfHeight, z))
        }
        for (i in 0 until segs) {
            val t0 = ringStart + i * 2
            val b0 = t0 + 1
            val t1 = t0 + 2
            val b1 = t0 + 3
            faces.add(Face3D(topCenter, t1, t0))
            faces.add(Face3D(botCenter, b0, b1))
            faces.add(Face3D(t0, b0, b1))
            faces.add(Face3D(t0, b1, t1))
        }
        return Mesh3DData(verts, faces, "3D Cylinder")
    }

    private fun buildTorusMesh(majorSegs: Int = 16, minorSegs: Int = 10): Mesh3DData {
        val majorR = 0.56f
        val minorR = 0.22f
        val verts = ArrayList<Vec3>()
        val faces = ArrayList<Face3D>()
        for (i in 0..majorSegs) {
            val u = 2f * PI.toFloat() * i / majorSegs
            val cu = cos(u)
            val su = sin(u)
            for (j in 0..minorSegs) {
                val v = 2f * PI.toFloat() * j / minorSegs
                val r = majorR + minorR * cos(v)
                verts.add(Vec3(r * cu, minorR * sin(v), r * su))
            }
        }
        val stride = minorSegs + 1
        for (i in 0 until majorSegs) {
            for (j in 0 until minorSegs) {
                val a = i * stride + j
                val b = a + 1
                val c = (i + 1) * stride + j
                val d = c + 1
                faces.add(Face3D(a, c, b))
                faces.add(Face3D(b, c, d))
            }
        }
        return Mesh3DData(verts, faces, "3D Torus")
    }

    /**
     * Generates a clean extruded 3D text mesh from voxel glyph blocks,
     * allowing real 3D rotation, depth extrusion, lighting, and wireframes.
     */
    fun buildExtrudedTextMesh(text: String, extrusionDepth: Float): Mesh3DData {
        val upper = text.uppercase().ifBlank { "3D" }.take(10)
        val verts = ArrayList<Vec3>()
        val faces = ArrayList<Face3D>()
        val cellW = 0.08f
        val cellH = 0.09f
        val hz = (0.04f + extrusionDepth * 0.24f).coerceIn(0.03f, 0.55f)
        val glyphSpacing = 4.2f * cellW
        val totalW = upper.length * glyphSpacing

        upper.forEachIndexed { charIdx, ch ->
            val rows = GLYPH_3X5[ch] ?: GLYPH_3X5['?']!!
            val originX = -totalW * 0.5f + charIdx * glyphSpacing + cellW * 0.5f
            for (r in rows.indices) {
                val mask = rows[r]
                for (c in 0 until 3) {
                    if (((mask shr (2 - c)) and 1) == 1) {
                        val cx = originX + c * cellW
                        val cy = (r - 2) * cellH
                        appendBox(verts, faces, cx, cy, cellW * 0.48f, cellH * 0.48f, hz)
                    }
                }
            }
        }

        if (verts.isEmpty()) return buildPrimitiveMesh(Primitive3DShape.CUBE, extrusionDepth)
        return normalizeMesh(Mesh3DData(verts, faces, "3D Text: $upper"))
    }

    private fun appendBox(
        verts: MutableList<Vec3>,
        faces: MutableList<Face3D>,
        cx: Float,
        cy: Float,
        hx: Float,
        hy: Float,
        hz: Float
    ) {
        val base = verts.size
        verts.add(Vec3(cx - hx, cy - hy, hz))
        verts.add(Vec3(cx + hx, cy - hy, hz))
        verts.add(Vec3(cx + hx, cy + hy, hz))
        verts.add(Vec3(cx - hx, cy + hy, hz))
        verts.add(Vec3(cx - hx, cy - hy, -hz))
        verts.add(Vec3(cx + hx, cy - hy, -hz))
        verts.add(Vec3(cx + hx, cy + hy, -hz))
        verts.add(Vec3(cx - hx, cy + hy, -hz))
        val quads = arrayOf(
            intArrayOf(0, 1, 2, 3),
            intArrayOf(5, 4, 7, 6),
            intArrayOf(4, 0, 3, 7),
            intArrayOf(1, 5, 6, 2),
            intArrayOf(4, 5, 1, 0),
            intArrayOf(3, 2, 6, 7)
        )
        for (q in quads) {
            if (faces.size + 2 > MAX_FACES) return
            faces.add(Face3D(base + q[0], base + q[1], base + q[2]))
            faces.add(Face3D(base + q[0], base + q[2], base + q[3]))
        }
    }

    private val GLYPH_3X5: Map<Char, IntArray> = mapOf(
        'A' to intArrayOf(0b010, 0b101, 0b111, 0b101, 0b101),
        'B' to intArrayOf(0b110, 0b101, 0b110, 0b101, 0b110),
        'C' to intArrayOf(0b011, 0b100, 0b100, 0b100, 0b011),
        'D' to intArrayOf(0b110, 0b101, 0b101, 0b101, 0b110),
        'E' to intArrayOf(0b111, 0b100, 0b110, 0b100, 0b111),
        'F' to intArrayOf(0b111, 0b100, 0b110, 0b100, 0b100),
        'G' to intArrayOf(0b011, 0b100, 0b101, 0b101, 0b011),
        'H' to intArrayOf(0b101, 0b101, 0b111, 0b101, 0b101),
        'I' to intArrayOf(0b111, 0b010, 0b010, 0b010, 0b111),
        'J' to intArrayOf(0b001, 0b001, 0b001, 0b101, 0b010),
        'K' to intArrayOf(0b101, 0b101, 0b110, 0b101, 0b101),
        'L' to intArrayOf(0b100, 0b100, 0b100, 0b100, 0b111),
        'M' to intArrayOf(0b101, 0b111, 0b111, 0b101, 0b101),
        'N' to intArrayOf(0b110, 0b101, 0b101, 0b101, 0b101),
        'O' to intArrayOf(0b010, 0b101, 0b101, 0b101, 0b010),
        'P' to intArrayOf(0b110, 0b101, 0b110, 0b100, 0b100),
        'Q' to intArrayOf(0b010, 0b101, 0b101, 0b111, 0b011),
        'R' to intArrayOf(0b110, 0b101, 0b110, 0b101, 0b101),
        'S' to intArrayOf(0b011, 0b100, 0b010, 0b001, 0b110),
        'T' to intArrayOf(0b111, 0b010, 0b010, 0b010, 0b010),
        'U' to intArrayOf(0b101, 0b101, 0b101, 0b101, 0b111),
        'V' to intArrayOf(0b101, 0b101, 0b101, 0b101, 0b010),
        'W' to intArrayOf(0b101, 0b101, 0b111, 0b111, 0b101),
        'X' to intArrayOf(0b101, 0b101, 0b010, 0b101, 0b101),
        'Y' to intArrayOf(0b101, 0b101, 0b010, 0b010, 0b010),
        'Z' to intArrayOf(0b111, 0b001, 0b010, 0b100, 0b111),
        '0' to intArrayOf(0b111, 0b101, 0b101, 0b101, 0b111),
        '1' to intArrayOf(0b010, 0b110, 0b010, 0b010, 0b111),
        '2' to intArrayOf(0b111, 0b001, 0b111, 0b100, 0b111),
        '3' to intArrayOf(0b111, 0b001, 0b011, 0b001, 0b111),
        '4' to intArrayOf(0b101, 0b101, 0b111, 0b001, 0b001),
        '5' to intArrayOf(0b111, 0b100, 0b111, 0b001, 0b111),
        '6' to intArrayOf(0b111, 0b100, 0b111, 0b101, 0b111),
        '7' to intArrayOf(0b111, 0b001, 0b001, 0b010, 0b010),
        '8' to intArrayOf(0b111, 0b101, 0b111, 0b101, 0b111),
        '9' to intArrayOf(0b111, 0b101, 0b111, 0b001, 0b111),
        ' ' to intArrayOf(0b000, 0b000, 0b000, 0b000, 0b000),
        '-' to intArrayOf(0b000, 0b000, 0b111, 0b000, 0b000),
        '!' to intArrayOf(0b010, 0b010, 0b010, 0b000, 0b010),
        '?' to intArrayOf(0b110, 0b001, 0b010, 0b000, 0b010)
    )
}
