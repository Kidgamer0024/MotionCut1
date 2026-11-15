package com.example.engine

import android.graphics.BitmapShader
import android.graphics.LightingColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.example.model.Mesh3DData
import com.example.model.Model3DConfig
import com.example.model.RenderMode3D
import com.example.model.Vec3
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Software 3D projection, Lambertian directional lighting, Painter depth-sorted triangle
 * rasterizer & perspective-affine image texture mapper rendered natively onto Compose [DrawScope].
 */
object Mesh3DRenderer {

    private data class ProjectedTri(
        val x0: Float, val y0: Float,
        val x1: Float, val y1: Float,
        val x2: Float, val y2: Float,
        val u0: Float, val v0: Float,
        val u1: Float, val v1: Float,
        val u2: Float, val v2: Float,
        val avgZ: Float,
        val shade: Float
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
        val modelU = FloatArray(mesh.vertices.size)
        val modelV = FloatArray(mesh.vertices.size)

        // Compute normalized model bounds for clean tri-planar UV mapping on any 3D object
        var minMx = Float.MAX_VALUE; var maxMx = -Float.MAX_VALUE
        var minMy = Float.MAX_VALUE; var maxMy = -Float.MAX_VALUE
        var minMz = Float.MAX_VALUE; var maxMz = -Float.MAX_VALUE
        for (v in mesh.vertices) {
            if (v.x < minMx) minMx = v.x; if (v.x > maxMx) maxMx = v.x
            if (v.y < minMy) minMy = v.y; if (v.y > maxMy) maxMy = v.y
            if (v.z < minMz) minMz = v.z; if (v.z > maxMz) maxMz = v.z
        }
        val spanX = (maxMx - minMx).coerceAtLeast(0.01f)
        val spanY = (maxMy - minMy).coerceAtLeast(0.01f)

        for (i in mesh.vertices.indices) {
            val v = mesh.vertices[i]
            modelU[i] = ((v.x - minMx) / spanX).coerceIn(0f, 1f)
            modelV[i] = ((v.y - minMy) / spanY).coerceIn(0f, 1f)

            val sx = v.x * scale
            val sy = v.y * scale
            val sz = v.z * scale

            val y1 = sy * cosX - sz * sinX
            val z1 = sy * sinX + sz * cosX
            val x2 = sx * cosY + z1 * sinY
            val z2 = -sx * sinY + z1 * cosY
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

            val ux = b.x - a.x; val uy = b.y - a.y; val uz = b.z - a.z
            val vx = c.x - a.x; val vy = c.y - a.y; val vz = c.z - a.z
            var nx = uy * vz - uz * vy
            var ny = uz * vx - ux * vz
            var nz = ux * vy - uy * vx
            val nLen = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-5f)
            nx /= nLen; ny /= nLen; nz /= nLen

            val rawDot = nx * lightDir.x + ny * lightDir.y + nz * lightDir.z
            val lambert = max(0f, abs(rawDot))
            val spec = if (config.metallic > 0.05f) {
                lambert.pow(8f) * config.metallic * 0.65f
            } else 0f
            val shade = (0.30f + lambert * 0.68f + spec).coerceIn(0.22f, 1.32f) * f.tintFactor

            val avgZ = (projZ[f.i0] + projZ[f.i1] + projZ[f.i2]) / 3f

            // Tri-planar UV selection so side/top faces also stretch image cleanly instead of pinching
            val ma = mesh.vertices[f.i0]
            val mb = mesh.vertices[f.i1]
            val mc = mesh.vertices[f.i2]
            val mux = mb.x - ma.x; val muy = mb.y - ma.y; val muz = mb.z - ma.z
            val mvx = mc.x - ma.x; val mvy = mc.y - ma.y; val mvz = mc.z - ma.z
            val mnx = abs(muy * mvz - muz * mvy)
            val mny = abs(muz * mvx - mux * mvz)
            val mnz = abs(mux * mvy - muy * mvx)
            val spanZ = (maxMz - minMz).coerceAtLeast(0.01f)

            fun uvFor(v: Vec3): Pair<Float, Float> {
                return when {
                    mnx >= mny && mnx >= mnz -> {
                        ((v.z - minMz) / spanZ).coerceIn(0f, 1f) to ((v.y - minMy) / spanY).coerceIn(0f, 1f)
                    }
                    mny >= mnx && mny >= mnz -> {
                        ((v.x - minMx) / spanX).coerceIn(0f, 1f) to ((v.z - minMz) / spanZ).coerceIn(0f, 1f)
                    }
                    else -> {
                        ((v.x - minMx) / spanX).coerceIn(0f, 1f) to ((v.y - minMy) / spanY).coerceIn(0f, 1f)
                    }
                }
            }
            val (u0, v0) = uvFor(ma)
            val (u1, v1) = uvFor(mb)
            val (u2, v2) = uvFor(mc)

            tris.add(
                ProjectedTri(
                    x0 = projX[f.i0], y0 = projY[f.i0],
                    x1 = projX[f.i1], y1 = projY[f.i1],
                    x2 = projX[f.i2], y2 = projY[f.i2],
                    u0 = u0, v0 = v0,
                    u1 = u1, v1 = v1,
                    u2 = u2, v2 = v2,
                    avgZ = avgZ,
                    shade = shade
                )
            )
        }

        tris.sortByDescending { it.avgZ }

        val baseColor = Color(config.materialColorHex)
        val wireStrokeWidth = 1.1.dp.toPx()
        val androidTex = planeImageBitmap?.let { runCatching { it.asAndroidBitmap() }.getOrNull() }
        val texMix = config.textureMix.coerceIn(0f, 1f)

        val shaderPaint: Paint?
        val baseShader: BitmapShader?
        val shaderMatrix = Matrix()
        if (androidTex != null && !androidTex.isRecycled && config.renderMode != RenderMode3D.WIREFRAME && texMix > 0.02f) {
            baseShader = BitmapShader(androidTex, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            shaderPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                shader = baseShader
            }
        } else {
            baseShader = null
            shaderPaint = null
        }

        for (t in tris) {
            val path = Path().apply {
                moveTo(t.x0, t.y0)
                lineTo(t.x1, t.y1)
                lineTo(t.x2, t.y2)
                close()
            }

            if (config.renderMode != RenderMode3D.WIREFRAME) {
                val factor = if (config.renderMode == RenderMode3D.SHADED) t.shade else 0.94f
                val baseAlpha = if (shaderPaint != null) (1f - texMix * 0.85f).coerceAtLeast(0.08f) * alpha else alpha
                val shadedColor = Color(
                    red = min(1f, baseColor.red * factor),
                    green = min(1f, baseColor.green * factor),
                    blue = min(1f, baseColor.blue * factor),
                    alpha = baseAlpha
                )
                drawPath(path = path, color = shadedColor)

                if (shaderPaint != null && baseShader != null && androidTex != null) {
                    val tw = androidTex.width.toFloat().coerceAtLeast(2f)
                    val th = androidTex.height.toFloat().coerceAtLeast(2f)
                    val su0 = t.u0 * tw; val sv0 = t.v0 * th
                    val su1 = t.u1 * tw; val sv1 = t.v1 * th
                    val su2 = t.u2 * tw; val sv2 = t.v2 * th
                    val uvArea = abs((su1 - su0) * (sv2 - sv0) - (su2 - su0) * (sv1 - sv0))
                    val scrArea = abs((t.x1 - t.x0) * (t.y2 - t.y0) - (t.x2 - t.x0) * (t.y1 - t.y0))
                    if (uvArea > 1f && scrArea > 2f) {
                        val srcPts = floatArrayOf(su0, sv0, su1, sv1, su2, sv2)
                        val dstPts = floatArrayOf(t.x0, t.y0, t.x1, t.y1, t.x2, t.y2)
                        shaderMatrix.reset()
                        if (shaderMatrix.setPolyToPoly(srcPts, 0, dstPts, 0, 3)) {
                            baseShader.setLocalMatrix(shaderMatrix)
                            val lightC = (factor * 235f).roundToInt().coerceIn(55, 255)
                            shaderPaint.colorFilter = LightingColorFilter(
                                android.graphics.Color.rgb(lightC, lightC, lightC),
                                0
                            )
                            shaderPaint.alpha = (texMix * alpha * 255f).roundToInt().coerceIn(0, 255)
                            drawIntoCanvas { canvas ->
                                canvas.nativeCanvas.drawPath(path.asAndroidPath(), shaderPaint)
                            }
                        }
                    }
                }
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
    }
}
