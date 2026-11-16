package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import com.example.data.DeviceMediaScanner
import com.example.model.ClipItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.ArrayDeque
import kotlin.math.sqrt

/**
 * Offline border-flood-fill + color-distance background remover that writes a real
 * transparent ARGB_8888 PNG cutout to app storage without requiring network/AI calls.
 */
object AutoBackgroundRemover {

    suspend fun removeBackground(
        context: Context,
        clip: ClipItem,
        maxDimension: Int = 960
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val sourceBitmap = decodeClipToMutableArgb8888(context, clip, maxDimension)
                ?: throw IllegalStateException("Clip has no image source for transparent cutout")
            val cutout = stripBorderBackground(sourceBitmap)
            if (cutout !== sourceBitmap && !sourceBitmap.isRecycled) {
                sourceBitmap.recycle()
            }
            val cutoutsDir = File(context.filesDir, "cutouts").apply { mkdirs() }
            val outFile = File(cutoutsDir, "cutout_${clip.id.take(8)}_${System.currentTimeMillis()}.png")
            FileOutputStream(outFile).use { fos ->
                cutout.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
            if (!cutout.isRecycled) {
                cutout.recycle()
            }
            Uri.fromFile(outFile).toString()
        }
    }

    internal fun stripBorderBackground(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        if (w <= 2 || h <= 2) return src

        val argb = if (src.config == Bitmap.Config.ARGB_8888 && src.isMutable) {
            src
        } else {
            src.copy(Bitmap.Config.ARGB_8888, true)
        }.apply {
            setHasAlpha(true)
        }

        val pixels = IntArray(w * h)
        argb.getPixels(pixels, 0, w, 0, 0, w, h)

        // If the image already contains substantial border transparency, preserve existing alpha
        var transparentBorderCount = 0
        var totalBorderSamples = 0
        var rSum = 0L
        var gSum = 0L
        var bSum = 0L
        var opaqueBorderCount = 0

        fun sampleBorder(idx: Int) {
            totalBorderSamples++
            val c = pixels[idx]
            val a = (c ushr 24) and 0xFF
            if (a < 24) {
                transparentBorderCount++
            } else {
                rSum += (c ushr 16) and 0xFF
                gSum += (c ushr 8) and 0xFF
                bSum += c and 0xFF
                opaqueBorderCount++
            }
        }

        for (x in 0 until w) {
            sampleBorder(x)
            sampleBorder((h - 1) * w + x)
        }
        for (y in 1 until (h - 1)) {
            sampleBorder(y * w)
            sampleBorder(y * w + (w - 1))
        }

        if (totalBorderSamples > 0 && transparentBorderCount > totalBorderSamples * 0.65f) {
            return argb
        }
        if (opaqueBorderCount == 0) {
            return argb
        }

        val bgR = (rSum / opaqueBorderCount).toInt()
        val bgG = (gSum / opaqueBorderCount).toInt()
        val bgB = (bSum / opaqueBorderCount).toInt()

        val hardDist = 42f
        val softDist = 68f

        fun colorDistToBg(color: Int): Float {
            val dr = ((color ushr 16) and 0xFF) - bgR
            val dg = ((color ushr 8) and 0xFF) - bgG
            val db = (color and 0xFF) - bgB
            return sqrt((dr * dr + dg * dg + db * db).toFloat())
        }

        val visited = BooleanArray(w * h)
        val queue = ArrayDeque<Int>(w * 2 + h * 2)

        fun tryEnqueue(x: Int, y: Int) {
            if (x !in 0 until w || y !in 0 until h) return
            val idx = y * w + x
            if (visited[idx]) return
            val c = pixels[idx]
            val a = (c ushr 24) and 0xFF
            if (a < 24 || colorDistToBg(c) <= softDist) {
                visited[idx] = true
                queue.addLast(idx)
            }
        }

        for (x in 0 until w) {
            tryEnqueue(x, 0)
            tryEnqueue(x, h - 1)
        }
        for (y in 1 until h - 1) {
            tryEnqueue(0, y)
            tryEnqueue(w - 1, y)
        }

        while (queue.isNotEmpty()) {
            val idx = queue.removeFirst()
            val c = pixels[idx]
            val dist = colorDistToBg(c)
            val oldA = (c ushr 24) and 0xFF
            val newA = when {
                oldA < 24 || dist <= hardDist -> 0
                else -> {
                    val t = ((dist - hardDist) / (softDist - hardDist)).coerceIn(0f, 1f)
                    (oldA * t).toInt().coerceIn(0, 255)
                }
            }
            pixels[idx] = (newA shl 24) or (c and 0x00FFFFFF)

            if (dist <= hardDist + 8f) {
                val px = idx % w
                val py = idx / w
                tryEnqueue(px - 1, py)
                tryEnqueue(px + 1, py)
                tryEnqueue(px, py - 1)
                tryEnqueue(px, py + 1)
            }
        }

        argb.setPixels(pixels, 0, w, 0, 0, w, h)
        return argb
    }

    private fun decodeClipToMutableArgb8888(context: Context, clip: ClipItem, maxDimension: Int): Bitmap? {
        val rawBitmap = clip.mediaUri?.let { uriStr ->
            val uri = Uri.parse(uriStr)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                runCatching {
                    val source = if (uri.scheme == "file" && uri.path != null) {
                        ImageDecoder.createSource(File(uri.path!!))
                    } else {
                        ImageDecoder.createSource(context.contentResolver, uri)
                    }
                    ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        decoder.isMutableRequired = true
                        val bw = info.size.width
                        val bh = info.size.height
                        if (bw > maxDimension || bh > maxDimension) {
                            val scale = maxDimension.toFloat() / maxOf(bw, bh).toFloat()
                            decoder.setTargetSize(
                                (bw * scale).toInt().coerceAtLeast(1),
                                (bh * scale).toInt().coerceAtLeast(1)
                            )
                        }
                    }
                }.getOrNull()
            } else {
                val opts = DeviceMediaScanner.transparentBitmapOptions(sampleSize = 1)
                if (uri.scheme == "file" && uri.path != null) {
                    BitmapFactory.decodeFile(uri.path, opts)
                } else {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, opts)
                    }
                }
            }
        } ?: clip.assetResId?.let { resId ->
            val opts = DeviceMediaScanner.transparentBitmapOptions(sampleSize = 2)
            BitmapFactory.decodeResource(context.resources, resId, opts)
        } ?: return null

        val maxCur = maxOf(rawBitmap.width, rawBitmap.height)
        val scaled = if (maxCur > maxDimension) {
            val factor = maxDimension.toFloat() / maxCur.toFloat()
            Bitmap.createScaledBitmap(
                rawBitmap,
                (rawBitmap.width * factor).toInt().coerceAtLeast(1),
                (rawBitmap.height * factor).toInt().coerceAtLeast(1),
                true
            ).also {
                if (it !== rawBitmap) rawBitmap.recycle()
            }
        } else {
            rawBitmap
        }
        return if (scaled.config == Bitmap.Config.ARGB_8888 && scaled.isMutable) {
            scaled.apply { setHasAlpha(true) }
        } else {
            scaled.copy(Bitmap.Config.ARGB_8888, true)?.also {
                it.setHasAlpha(true)
                scaled.recycle()
            }
        }
    }
}
