package com.example.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.model.Mesh3DData
import com.example.model.Model3DConfig
import com.example.model.Model3DSource
import com.example.model.RenderMode3D
import com.example.model.Vec3
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Software 3D projection, Lambertian directional lighting & Painter depth-sorted triangle
 * rasterizer rendered natively onto Compose [DrawScope].
 */
object Mesh3DRenderer {

    private data class ProjectedTri(
        val x0: Float, val y0: Float,
        val x1: Float, val y1: Float,
        val x2: Float, val y2: Float,
        val avgZ: Float,
        val shade: Float,
        val isFrontQuad: Boolean
    )

    fun DrawScope.drawModel3D(
        mesh: Mesh3DData,
        config: Model3DConfig,
        viewportSize: Float,
        alpha: Float = 1f,
        planeImageBitmap: ImageBitmap? = null,
        colorFilter: ColorFilter? = null
    ) {
        if (mesh.vertices.isEmpty() || mesh.faces.isEmpty()) return

        val rx = Math.toRadians(config.rotationX.toDouble()).toFloat()
        val ry = Math.toRadians(config.rotationY.toDouble()).toFloat()
        val rz = Math.toRadians(config.rotationZ.toDouble()).toFloat()

        val cosX = cos(rx); val sinX = sin(rx)
        val cosY = cos(ry); val sinY = sin(ry)
        val cosZ = cos(rz); val sinZ = sin(rz)

        val fovRad = Math.toRadians(config.fovDegrees.coerceIn(20f, 110f).toDouble()).toFloat()
        val focal = 1f / tan(fovRad * 0.5f).coerceAtLeast(0.15f)
        val camDist = 3.2f + config.depthZ.coerceIn(-2.0f, 3.0f)
        val scale = config.modelScale.coerceIn(0.2f, 3.5f)
        val pixelScale = viewportSize * 0.36f * focal

        // Directional light unit vector
        val la = Math.toRadians(config.lightAzimuthDeg.toDouble()).toFloat()
        val le = Math.toRadians(config.lightElevationDeg.toDouble()).toFloat()
        val lightDir = Vec3(
            x = cos(le) * sin(la),
            y = -sin(le),
            z = cos(le) * cos(la)
        )

        val transformed = ArrayList<Vec3>(mesh.vertices.size)
        val projX = FloatArray(mesh.vertices.size)
        val projY = FloatArray(mesh.vertices.size)
        val projZ = FloatArray(mesh.vertices.size)

        for (i in mesh.vertices.indices) {
            val v = mesh.vertices[i]
            val sx = v.x * scale
            val sy = v.y * scale
            val sz = v.z * scale

            // Rotate X
            val y1 = sy * cosX - sz * sinX
            val z1 = sy * sinX + sz * cosX
            // Rotate Y
            val x2 = sx * cosY + z1 * sinY
            val z2 = -sx * sinY + z1 * cosY
            // Rotate Z
            val x3 = x2 * cosZ - y1 * sinZ
            val y3 = x2 * sinZ + y1 * cosZ

            transformed.add(Vec3(x3, y3, z2))
            val zCam = (camDist - z2).coerceAtLeast(0.35f)
            projX[i] = (x3 / zCam) * pixelScale
            projY[i] = (y3 / zCam) * pixelScale
            projZ[i] = zCam
        }

        val tris = ArrayList<ProjectedTri>(mesh.faces.size)
        for (faceIdx in mesh.faces.indices) {
            val f = mesh.faces[faceIdx]
            if (f.i0 !in transformed.indices || f.i1 !in transformed.indices || f.i2 !in transformed.indices) continue
            val a = transformed[f.i0]
            val b = transformed[f.i1]
            val c = transformed[f.i2]

            // Face normal via cross product
            val ux = b.x - a.x; val uy = b.y - a.y; val uz = b.z - a.z
            val vx = c.x - a.x; val vy = c.y - a.y; val vz = c.z - a.z
            var nx = uy * vz - uz * vy
            var ny = uz * vx - ux * vz
            var nz = ux * vy - uy * vx
            val nLen = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-5f)
            nx /= nLen; ny /= nLen; nz /= nLen

            // Two-sided lighting for crisp display regardless of winding order
            val rawDot = nx * lightDir.x + ny * lightDir.y + nz * lightDir.z
            val lambert = max(0f, abs(rawDot))
            val spec = if (config.metallic > 0.05f) {
                lambert.pow(8f) * config.metallic * 0.65f
            } else 0f
            val shade = (0.28f + lambert * 0.68f + spec).coerceIn(0.20f, 1.35f)

            val avgZ = (projZ[f.i0] + projZ[f.i1] + projZ[f.i2]) / 3f
            tris.add(
                ProjectedTri(
                    x0 = projX[f.i0], y0 = projY[f.i0],
                    x1 = projX[f.i1], y1 = projY[f.i1],
                    x2 = projX[f.i2], y2 = projY[f.i2],
                    avgZ = avgZ,
                    shade = shade,
                    isFrontQuad = config.source == Model3DSource.IMAGE_PLANE && faceIdx < 2
                )
            )
        }

        // Painter's depth sort (far -> near)
        tris.sortByDescending { it.avgZ }

        val baseColor = Color(config.materialColorHex)
        val wireStrokeWidth = 1.1.dp.toPx()

        for (t in tris) {
            val path = Path().apply {
                moveTo(t.x0, t.y0)
                lineTo(t.x1, t.y1)
                lineTo(t.x2, t.y2)
                close()
            }

            if (config.renderMode != RenderMode3D.WIREFRAME) {
                val factor = if (config.renderMode == RenderMode3D.SHADED) t.shade else 0.92f
                val shadedColor = Color(
                    red = min(1f, baseColor.red * factor),
                    green = min(1f, baseColor.green * factor),
                    blue = min(1f, baseColor.blue * factor),
                    alpha = alpha
                )
                drawPath(path = path, color = shadedColor)
            }

            if (config.renderMode == RenderMode3D.WIREFRAME || config.showEdges) {
                val edgeColor = if (config.renderMode == RenderMode3D.WIREFRAME) {
                    baseColor.copy(alpha = alpha)
                } else {
                    Color.White.copy(alpha = 0.22f * alpha)
                }
                drawPath(
                    path = path,
                    color = edgeColor,
                    style = Stroke(width = wireStrokeWidth)
                )
            }
        }

        // For 3D Image Plane, draw the mapped image bounded inside the front face quad
        if (config.source == Model3DSource.IMAGE_PLANE && planeImageBitmap != null && mesh.vertices.size >= 4) {
            val minX = minOf(projX[0], projX[1], projX[2], projX[3])
            val maxX = maxOf(projX[0], projX[1], projX[2], projX[3])
            val minY = minOf(projY[0], projY[1], projY[2], projY[3])
            val maxY = maxOf(projY[0], projY[1], projY[2], projY[3])
            val w = (maxX - minX).toInt().coerceAtLeast(1)
            val h = (maxY - minY).toInt().coerceAtLeast(1)
            if (w > 4 && h > 4) {
                drawImage(
                    image = planeImageBitmap,
                    dstOffset = IntOffset(minX.toInt(), minY.toInt()),
                    dstSize = IntSize(w, h),
                    alpha = 0.92f * alpha,
                    colorFilter = colorFilter
                )
            }
        }
    }
}
