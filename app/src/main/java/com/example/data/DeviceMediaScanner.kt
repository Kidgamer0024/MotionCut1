package com.example.data

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.abs

data class DeviceMediaItem(
    val id: String,
    val title: String,
    val uriString: String?,
    val isVideo: Boolean,
    val durationMs: Long,
    val mimeType: String,
    val sampleResId: Int? = null,
    val colorHex: Long = 0xFF14242A,
    val isDeviceFile: Boolean = true,
    val folderName: String = "Device"
)

data class DeviceAudioItem(
    val id: String,
    val title: String,
    val artist: String,
    val uriString: String?,
    val durationMs: Long,
    val accentColorHex: Long,
    val bpm: Int = 110,
    val isDeviceFile: Boolean = true,
    val waveformSamples: List<Float> = emptyList(),
    val folderName: String = "Audios"
)

object DeviceMediaScanner {
    private const val PREFS_NAME = "motioncut_device_library"
    private const val KEY_MEDIA_URIS = "persisted_media_uris"
    private const val KEY_AUDIO_URIS = "persisted_audio_uris"
    private const val KEY_TREE_URIS = "persisted_tree_uris"

    private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "heic", "heif", "gif", "bmp")
    private val VIDEO_EXTENSIONS = setOf("mp4", "mov", "webm", "mkv", "m4v", "3gp", "avi")
    private val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "wav", "aac", "ogg", "flac", "opus", "3gp")

    private val AUDIO_CARD_COLORS = listOf(
        0xFF0BE87A,
        0xFF00B8D9,
        0xFF10B981,
        0xFF38BDF8,
        0xFFA855F7,
        0xFFFF7043
    )

    suspend fun loadDeviceAndStudioMedia(context: Context): List<DeviceMediaItem> =
        withContext(Dispatchers.IO) {
            val items = mutableListOf<DeviceMediaItem>()
            val seenUris = mutableSetOf<String>()

            // 1. User-picked persisted URIs (Photo Picker / SAF Files)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedUris = prefs.getStringSet(KEY_MEDIA_URIS, emptySet()).orEmpty()
            for (uriStr in savedUris) {
                runCatching {
                    val uri = Uri.parse(uriStr)
                    val item = buildMediaItemFromUri(context, uri)
                    if (item != null && seenUris.add(uriStr)) {
                        items.add(item)
                    }
                }
            }

            // 2. Persisted SAF Folders from "Files" (e.g. Photos, Videos, Pictures, Movies, DCIM)
            val treeUris = prefs.getStringSet(KEY_TREE_URIS, emptySet()).orEmpty()
            for (treeStr in treeUris) {
                runCatching {
                    val treeUri = Uri.parse(treeStr)
                    val treeCollected = scanDocumentTreeForMedia(context, treeUri)
                    treeCollected.forEach { item ->
                        val key = item.uriString ?: item.id
                        if (seenUris.add(key)) {
                            items.add(item)
                        }
                    }
                }
            }

            // 3. App-scoped & accessible "Photos" / "Videos" / "Pictures" / "Movies" directories in Files
            collectMediaFromDirectories(
                directories = getPhotoAndVideoDirectories(context),
                seenUris = seenUris,
                outItems = items
            )

            // 4. Existing device videos via MediaStore
            runCatching {
                val projection = arrayOf(
                    MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.DISPLAY_NAME,
                    MediaStore.Video.Media.DURATION,
                    MediaStore.Video.Media.MIME_TYPE,
                    MediaStore.Video.Media.BUCKET_DISPLAY_NAME
                )
                context.contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                    val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                    val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                    val bucketCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                    var count = 0
                    while (cursor.moveToNext() && count < 60) {
                        val mediaId = cursor.getLong(idCol)
                        val title = cursor.getString(nameCol) ?: "Device Video"
                        val duration = cursor.getLong(durCol).coerceAtLeast(3000L)
                        val mime = cursor.getString(mimeCol) ?: "video/mp4"
                        val folder = if (bucketCol >= 0) {
                            cursor.getString(bucketCol)?.takeIf { it.isNotBlank() } ?: "Videos"
                        } else "Videos"
                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            mediaId
                        ).toString()
                        if (seenUris.add(contentUri)) {
                            items.add(
                                DeviceMediaItem(
                                    id = "ms_vid_$mediaId",
                                    title = title,
                                    uriString = contentUri,
                                    isVideo = true,
                                    durationMs = duration,
                                    mimeType = mime,
                                    isDeviceFile = true,
                                    folderName = folder
                                )
                            )
                            count++
                        }
                    }
                }
            }

            // 5. Existing device images via MediaStore
            runCatching {
                val projection = arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.MIME_TYPE,
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME
                )
                context.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                    val bucketCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                    var count = 0
                    while (cursor.moveToNext() && count < 60) {
                        val mediaId = cursor.getLong(idCol)
                        val title = cursor.getString(nameCol) ?: "Device Photo"
                        val mime = cursor.getString(mimeCol) ?: "image/jpeg"
                        val folder = if (bucketCol >= 0) {
                            cursor.getString(bucketCol)?.takeIf { it.isNotBlank() } ?: "Photos"
                        } else "Photos"
                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            mediaId
                        ).toString()
                        if (seenUris.add(contentUri)) {
                            items.add(
                                DeviceMediaItem(
                                    id = "ms_img_$mediaId",
                                    title = title,
                                    uriString = contentUri,
                                    isVideo = false,
                                    durationMs = 5000L,
                                    mimeType = mime,
                                    isDeviceFile = true,
                                    folderName = folder
                                )
                            )
                            count++
                        }
                    }
                }
            }

            items
        }

    suspend fun loadDeviceAndStudioAudio(context: Context): List<DeviceAudioItem> =
        withContext(Dispatchers.IO) {
            val items = mutableListOf<DeviceAudioItem>()
            val seenUris = mutableSetOf<String>()

            // 1. Persisted user-picked SAF audio files
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedAudio = prefs.getStringSet(KEY_AUDIO_URIS, emptySet()).orEmpty()
            for (uriStr in savedAudio) {
                runCatching {
                    val uri = Uri.parse(uriStr)
                    val built = buildAudioItemFromUri(context, uri)
                    if (built != null && seenUris.add(uriStr)) {
                        items.add(built)
                    }
                }
            }

            // 2. Persisted SAF Folders from "Files" (e.g. Audios, Music, Recordings)
            val treeUris = prefs.getStringSet(KEY_TREE_URIS, emptySet()).orEmpty()
            for (treeStr in treeUris) {
                runCatching {
                    val treeUri = Uri.parse(treeStr)
                    val treeAudio = scanDocumentTreeForAudio(context, treeUri)
                    treeAudio.forEach { item ->
                        val key = item.uriString ?: item.id
                        if (seenUris.add(key)) {
                            items.add(item)
                        }
                    }
                }
            }

            // 3. App-scoped & accessible "Audios" / "Music" / "Recordings" directories in Files
            collectAudioFromDirectories(
                directories = getAudioDirectories(context),
                seenUris = seenUris,
                outItems = items
            )

            // 4. Existing device audio via MediaStore
            runCatching {
                val projection = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.DURATION
                )
                context.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    "${MediaStore.Audio.Media.DATE_MODIFIED} DESC"
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                    val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    var count = 0
                    while (cursor.moveToNext() && count < 60) {
                        val audioId = cursor.getLong(idCol)
                        val title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() }
                            ?: cursor.getString(nameCol)
                            ?: "Device Audio"
                        val artist = cursor.getString(artistCol)
                            ?.takeIf { it.isNotBlank() && it != "<unknown>" }
                            ?: "Audios"
                        val duration = cursor.getLong(durCol).coerceAtLeast(3000L)
                        val uriStr = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            audioId
                        ).toString()
                        if (seenUris.add(uriStr)) {
                            items.add(
                                DeviceAudioItem(
                                    id = "ms_aud_$audioId",
                                    title = title,
                                    artist = artist,
                                    uriString = uriStr,
                                    durationMs = duration,
                                    accentColorHex = AUDIO_CARD_COLORS[abs(title.hashCode()) % AUDIO_CARD_COLORS.size],
                                    isDeviceFile = true,
                                    waveformSamples = defaultWaveForTitle(title),
                                    folderName = "Audios"
                                )
                            )
                            count++
                        }
                    }
                }
            }

            items
        }

    fun persistPickedMediaUris(context: Context, uris: List<Uri>) {
        if (uris.isEmpty()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getStringSet(KEY_MEDIA_URIS, emptySet()).orEmpty().toMutableSet()
        for (uri in uris) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            existing.add(uri.toString())
        }
        prefs.edit().putStringSet(KEY_MEDIA_URIS, existing).apply()
    }

    fun persistPickedAudioUris(context: Context, uris: List<Uri>) {
        if (uris.isEmpty()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getStringSet(KEY_AUDIO_URIS, emptySet()).orEmpty().toMutableSet()
        for (uri in uris) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            existing.add(uri.toString())
        }
        prefs.edit().putStringSet(KEY_AUDIO_URIS, existing).apply()
    }

    fun persistTreeUri(context: Context, treeUri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getStringSet(KEY_TREE_URIS, emptySet()).orEmpty().toMutableSet()
        existing.add(treeUri.toString())
        prefs.edit().putStringSet(KEY_TREE_URIS, existing).apply()
    }

    fun isVideoUri(context: Context, uriString: String, isVideoHint: Boolean = true): Boolean {
        val lower = uriString.lowercase(Locale.US)
        val ext = lower.substringBefore('?').substringAfterLast('.', "")
        if (ext in IMAGE_EXTENSIONS) return false
        if (ext in VIDEO_EXTENSIONS) return true
        val mime = runCatching { context.contentResolver.getType(Uri.parse(uriString)) }.getOrNull().orEmpty()
        if (mime.startsWith("image/")) return false
        if (mime.startsWith("video/")) return true
        return isVideoHint
    }

    fun transparentBitmapOptions(sampleSize: Int = 1): BitmapFactory.Options {
        return BitmapFactory.Options().apply {
            inSampleSize = sampleSize.coerceAtLeast(1)
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inPremultiplied = true
        }
    }

    private fun decodeAndroidBitmapFromUri(context: Context, uri: Uri, sampleSize: Int): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val decoded = runCatching {
                val source = if (uri.scheme == "file" && uri.path != null) {
                    ImageDecoder.createSource(File(uri.path!!))
                } else {
                    ImageDecoder.createSource(context.contentResolver, uri)
                }
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = false
                    if (sampleSize > 1) {
                        val targetW = (info.size.width / sampleSize).coerceAtLeast(1)
                        val targetH = (info.size.height / sampleSize).coerceAtLeast(1)
                        decoder.setTargetSize(targetW, targetH)
                    }
                }
            }.getOrNull()
            if (decoded != null) {
                return if (decoded.config != Bitmap.Config.ARGB_8888) {
                    decoded.copy(Bitmap.Config.ARGB_8888, false)?.also {
                        it.setHasAlpha(true)
                        decoded.recycle()
                    } ?: decoded
                } else {
                    decoded.also { runCatching { it.setHasAlpha(true) } }
                }
            }
        }

        val opts = transparentBitmapOptions(sampleSize)
        val decoded = if (uri.scheme == "file" && uri.path != null) {
            BitmapFactory.decodeFile(uri.path, opts)
        } else {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, opts)
            }
        } ?: return null

        return if (decoded.config != Bitmap.Config.ARGB_8888) {
            decoded.copy(Bitmap.Config.ARGB_8888, false)?.also {
                it.setHasAlpha(true)
                decoded.recycle()
            } ?: decoded
        } else {
            decoded.also { runCatching { it.setHasAlpha(true) } }
        }
    }

    suspend fun loadThumbnailBitmap(context: Context, uriString: String, isVideo: Boolean): ImageBitmap? =
        withContext(Dispatchers.IO) {
            runCatching {
                val uri = Uri.parse(uriString)
                val actuallyVideo = isVideoUri(context, uriString, isVideo)
                if (!actuallyVideo) {
                    // Decode images directly in ARGB_8888 so PNG/WebP transparency is preserved
                    // (MediaStore/ContentResolver.loadThumbnail composites opaque black over alpha).
                    decodeAndroidBitmapFromUri(context, uri, sampleSize = 2)?.asImageBitmap()
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && uri.scheme == "content") {
                    context.contentResolver.loadThumbnail(uri, Size(360, 260), null).asImageBitmap()
                } else {
                    val retriever = MediaMetadataRetriever()
                    try {
                        if (uri.scheme == "file" && uri.path != null) {
                            retriever.setDataSource(uri.path)
                        } else {
                            retriever.setDataSource(context, uri)
                        }
                        retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.asImageBitmap()
                    } finally {
                        retriever.release()
                    }
                }
            }.getOrNull()
        }

    suspend fun loadFullBitmap(context: Context, uriString: String, isVideoHint: Boolean = false): Bitmap? =
        withContext(Dispatchers.IO) {
            runCatching {
                val uri = Uri.parse(uriString)
                val actuallyVideo = isVideoUri(context, uriString, isVideoHint)
                if (!actuallyVideo) {
                    decodeAndroidBitmapFromUri(context, uri, sampleSize = 1)?.let { bmp ->
                        val maxDim = maxOf(bmp.width, bmp.height)
                        if (maxDim > 1920) {
                            val scale = 1920f / maxDim.toFloat()
                            val scaled = Bitmap.createScaledBitmap(
                                bmp,
                                (bmp.width * scale).toInt().coerceAtLeast(1),
                                (bmp.height * scale).toInt().coerceAtLeast(1),
                                true
                            )
                            if (scaled !== bmp) bmp.recycle()
                            scaled
                        } else bmp
                    }
                } else {
                    val retriever = MediaMetadataRetriever()
                    try {
                        if (uri.scheme == "file" && uri.path != null) {
                            retriever.setDataSource(uri.path)
                        } else {
                            retriever.setDataSource(context, uri)
                        }
                        retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    } finally {
                        retriever.release()
                    }
                }
            }.getOrNull()
        }

    suspend fun loadFullImageBitmap(context: Context, uriString: String, isVideoHint: Boolean = false): ImageBitmap? {
        return loadFullBitmap(context, uriString, isVideoHint)?.asImageBitmap()
    }

    fun formatShortDuration(durationMs: Long): String {
        val totalSec = (durationMs / 1000L).coerceAtLeast(1L)
        val mins = totalSec / 60L
        val secs = totalSec % 60L
        return String.format(Locale.US, "%d:%02d", mins, secs)
    }

    @Suppress("DEPRECATION")
    internal fun getPhotoAndVideoDirectories(context: Context): List<File> {
        val roots = mutableListOf<File>()
        // App files & external files folders (photos / videos / pictures / movies / dcim)
        listOfNotNull(
            context.filesDir,
            context.cacheDir,
            context.getExternalFilesDir(null),
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            context.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
            context.getExternalFilesDir(Environment.DIRECTORY_DCIM)
        ).forEach { base ->
            roots.add(base)
            listOf("photos", "Photos", "videos", "Videos", "images", "Images", "media", "Media").forEach { sub ->
                roots.add(File(base, sub))
            }
        }
        // Device public Files directories
        runCatching {
            val extRoot = Environment.getExternalStorageDirectory()
            listOf(
                "Photos", "photos", "Videos", "videos",
                Environment.DIRECTORY_PICTURES,
                Environment.DIRECTORY_MOVIES,
                Environment.DIRECTORY_DCIM,
                "Images", "images",
                Environment.DIRECTORY_DOWNLOADS
            ).forEach { name ->
                roots.add(File(extRoot, name))
            }
            roots.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES))
            roots.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES))
            roots.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM))
        }
        return roots.distinctBy { runCatching { it.canonicalPath }.getOrDefault(it.absolutePath) }
    }

    @Suppress("DEPRECATION")
    internal fun getAudioDirectories(context: Context): List<File> {
        val roots = mutableListOf<File>()
        listOfNotNull(
            context.filesDir,
            context.cacheDir,
            context.getExternalFilesDir(null),
            context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        ).forEach { base ->
            roots.add(base)
            listOf("audios", "Audios", "audio", "Audio", "music", "Music", "recordings", "Recordings").forEach { sub ->
                roots.add(File(base, sub))
            }
        }
        runCatching {
            val extRoot = Environment.getExternalStorageDirectory()
            listOf(
                "Audios", "audios", "Audio", "audio",
                Environment.DIRECTORY_MUSIC,
                "Recordings", "Podcasts", "Audiobooks",
                Environment.DIRECTORY_DOWNLOADS
            ).forEach { name ->
                roots.add(File(extRoot, name))
            }
            roots.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC))
        }
        return roots.distinctBy { runCatching { it.canonicalPath }.getOrDefault(it.absolutePath) }
    }

    private fun collectMediaFromDirectories(
        directories: List<File>,
        seenUris: MutableSet<String>,
        outItems: MutableList<DeviceMediaItem>
    ) {
        val allowedExt = IMAGE_EXTENSIONS + VIDEO_EXTENSIONS
        for (dir in directories) {
            val files = listFilesRecursively(dir, allowedExt, maxDepth = 3, maxFiles = 60)
            for (file in files) {
                val uriStr = Uri.fromFile(file).toString()
                if (seenUris.add(uriStr)) {
                    val ext = file.extension.lowercase(Locale.US)
                    val isVid = ext in VIDEO_EXTENSIONS
                    val dur = if (isVid) readDurationMsFromPath(file.absolutePath) else 5000L
                    val folderLabel = file.parentFile?.name?.takeIf { it.isNotBlank() }
                        ?: if (isVid) "Videos" else "Photos"
                    outItems.add(
                        DeviceMediaItem(
                            id = "file_media_${uriStr.hashCode()}",
                            title = file.name,
                            uriString = uriStr,
                            isVideo = isVid,
                            durationMs = dur,
                            mimeType = if (isVid) "video/$ext" else "image/$ext",
                            isDeviceFile = true,
                            folderName = folderLabel
                        )
                    )
                }
            }
        }
    }

    private fun collectAudioFromDirectories(
        directories: List<File>,
        seenUris: MutableSet<String>,
        outItems: MutableList<DeviceAudioItem>
    ) {
        for (dir in directories) {
            val files = listFilesRecursively(dir, AUDIO_EXTENSIONS, maxDepth = 3, maxFiles = 60)
            for (file in files) {
                val uriStr = Uri.fromFile(file).toString()
                if (seenUris.add(uriStr)) {
                    val dur = readDurationMsFromPath(file.absolutePath)
                    val folderLabel = file.parentFile?.name?.takeIf { it.isNotBlank() } ?: "Audios"
                    outItems.add(
                        DeviceAudioItem(
                            id = "file_aud_${uriStr.hashCode()}",
                            title = file.nameWithoutExtension,
                            artist = folderLabel,
                            uriString = uriStr,
                            durationMs = dur,
                            accentColorHex = AUDIO_CARD_COLORS[abs(file.name.hashCode()) % AUDIO_CARD_COLORS.size],
                            isDeviceFile = true,
                            waveformSamples = defaultWaveForTitle(file.name),
                            folderName = folderLabel
                        )
                    )
                }
            }
        }
    }

    private fun listFilesRecursively(
        root: File,
        extensions: Set<String>,
        maxDepth: Int,
        maxFiles: Int
    ): List<File> {
        if (!root.exists() || !root.isDirectory) return emptyList()
        val results = mutableListOf<File>()
        fun walk(current: File, depth: Int) {
            if (depth > maxDepth || results.size >= maxFiles) return
            val children = runCatching { current.listFiles() }.getOrNull() ?: return
            for (child in children) {
                if (results.size >= maxFiles) break
                if (child.isDirectory) {
                    if (!child.name.startsWith(".")) {
                        walk(child, depth + 1)
                    }
                } else if (child.isFile && child.length() > 0L) {
                    if (child.extension.lowercase(Locale.US) in extensions) {
                        results.add(child)
                    }
                }
            }
        }
        walk(root, 0)
        return results
    }

    private fun scanDocumentTreeForMedia(context: Context, treeUri: Uri): List<DeviceMediaItem> {
        val results = mutableListOf<DeviceMediaItem>()
        val rootDocId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull() ?: return emptyList()
        val rootFolderLabel = queryDocumentDisplayName(context, treeUri, rootDocId) ?: "Files"

        fun walkDoc(parentDocId: String, folderName: String, depth: Int) {
            if (depth > 3 || results.size >= 80) return
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            )
            runCatching {
                context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                    val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    while (cursor.moveToNext() && results.size < 80) {
                        val docId = if (idCol >= 0) cursor.getString(idCol) else continue
                        val name = (if (nameCol >= 0) cursor.getString(nameCol) else null) ?: "Media"
                        val mime = (if (mimeCol >= 0) cursor.getString(mimeCol) else null).orEmpty()
                        if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                            walkDoc(docId, name, depth + 1)
                        } else {
                            val ext = name.substringAfterLast('.', "").lowercase(Locale.US)
                            val isVideo = mime.startsWith("video/") || ext in VIDEO_EXTENSIONS
                            val isImage = mime.startsWith("image/") || ext in IMAGE_EXTENSIONS
                            if (isVideo || isImage) {
                                val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                                val dur = if (isVideo) readDurationMsFromUri(context, docUri) else 5000L
                                results.add(
                                    DeviceMediaItem(
                                        id = "saf_media_${docUri.toString().hashCode()}",
                                        title = name,
                                        uriString = docUri.toString(),
                                        isVideo = isVideo,
                                        durationMs = dur,
                                        mimeType = mime.ifBlank { if (isVideo) "video/mp4" else "image/jpeg" },
                                        isDeviceFile = true,
                                        folderName = folderName
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        walkDoc(rootDocId, rootFolderLabel, 0)
        return results
    }

    private fun scanDocumentTreeForAudio(context: Context, treeUri: Uri): List<DeviceAudioItem> {
        val results = mutableListOf<DeviceAudioItem>()
        val rootDocId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull() ?: return emptyList()
        val rootFolderLabel = queryDocumentDisplayName(context, treeUri, rootDocId) ?: "Audios"

        fun walkDoc(parentDocId: String, folderName: String, depth: Int) {
            if (depth > 3 || results.size >= 80) return
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            )
            runCatching {
                context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                    val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    while (cursor.moveToNext() && results.size < 80) {
                        val docId = if (idCol >= 0) cursor.getString(idCol) else continue
                        val name = (if (nameCol >= 0) cursor.getString(nameCol) else null) ?: "Audio"
                        val mime = (if (mimeCol >= 0) cursor.getString(mimeCol) else null).orEmpty()
                        if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                            walkDoc(docId, name, depth + 1)
                        } else {
                            val ext = name.substringAfterLast('.', "").lowercase(Locale.US)
                            if (mime.startsWith("audio/") || ext in AUDIO_EXTENSIONS) {
                                val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                                val dur = readDurationMsFromUri(context, docUri)
                                results.add(
                                    DeviceAudioItem(
                                        id = "saf_aud_${docUri.toString().hashCode()}",
                                        title = name.substringBeforeLast("."),
                                        artist = folderName,
                                        uriString = docUri.toString(),
                                        durationMs = dur,
                                        accentColorHex = AUDIO_CARD_COLORS[abs(name.hashCode()) % AUDIO_CARD_COLORS.size],
                                        isDeviceFile = true,
                                        waveformSamples = defaultWaveForTitle(name),
                                        folderName = folderName
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        walkDoc(rootDocId, rootFolderLabel, 0)
        return results
    }

    private fun queryDocumentDisplayName(context: Context, treeUri: Uri, docId: String): String? {
        return runCatching {
            val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
            context.contentResolver.query(
                docUri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null
            )?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()
    }

    private fun buildMediaItemFromUri(context: Context, uri: Uri): DeviceMediaItem? {
        val mime = context.contentResolver.getType(uri) ?: "video/mp4"
        val isVideo = mime.startsWith("video/")
        val name = queryDisplayName(context, uri) ?: (if (isVideo) "Device Video" else "Device Image")
        val duration = if (isVideo) readDurationMsFromUri(context, uri) else 5000L
        return DeviceMediaItem(
            id = "picked_${uri.toString().hashCode()}",
            title = name,
            uriString = uri.toString(),
            isVideo = isVideo,
            durationMs = duration,
            mimeType = mime,
            isDeviceFile = true,
            folderName = if (isVideo) "Videos" else "Photos"
        )
    }

    private fun buildAudioItemFromUri(context: Context, uri: Uri): DeviceAudioItem? {
        val name = queryDisplayName(context, uri) ?: "Device Audio"
        val duration = readDurationMsFromUri(context, uri)
        return DeviceAudioItem(
            id = "picked_aud_${uri.toString().hashCode()}",
            title = name.substringBeforeLast("."),
            artist = "Device Audio File",
            uriString = uri.toString(),
            durationMs = duration,
            accentColorHex = AUDIO_CARD_COLORS[abs(name.hashCode()) % AUDIO_CARD_COLORS.size],
            isDeviceFile = true,
            waveformSamples = defaultWaveForTitle(name),
            folderName = "Audios"
        )
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
        }.getOrNull() ?: uri.lastPathSegment
    }

    private fun readDurationMsFromUri(context: Context, uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.coerceIn(1000L, 600_000L)
                ?: 6000L
        } catch (_: Exception) {
            6000L
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun readDurationMsFromPath(path: String): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.coerceIn(1000L, 600_000L)
                ?: 5000L
        } catch (_: Exception) {
            5000L
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun defaultWaveForTitle(seed: String): List<Float> {
        val base = abs(seed.hashCode())
        return List(20) { idx ->
            val v = ((base + idx * 37) % 85) / 100f + 0.15f
            v.coerceIn(0.15f, 1.0f)
        }
    }
}
