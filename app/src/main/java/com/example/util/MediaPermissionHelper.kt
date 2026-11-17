package com.example.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

enum class MediaRecordingFeature(
    val permissions: List<String>,
    val title: String,
    val rationale: String
) {
    MICROPHONE_ONLY(
        permissions = listOf(Manifest.permission.RECORD_AUDIO),
        title = "Microphone Access Required",
        rationale = "Allow microphone access to record voice-overs and live audio tracks synchronized with your timeline."
    ),
    CAMERA_ONLY(
        permissions = listOf(Manifest.permission.CAMERA),
        title = "Camera Access Required",
        rationale = "Allow camera access to capture video clips or photos directly into your project."
    ),
    CAMERA_AND_MICROPHONE(
        permissions = listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
        title = "Camera & Microphone Access Required",
        rationale = "Allow camera and microphone access to record video and audio clips directly into MotionCut."
    )
}

@Stable
data class MediaPermissionSnapshot(
    val hasCameraPermission: Boolean,
    val hasMicrophonePermission: Boolean,
    val grantedPermissions: Set<String>,
    val missingPermissions: List<String>,
    val isFullyGranted: Boolean
)

@Stable
data class MediaPermissionControllerState(
    val feature: MediaRecordingFeature,
    val snapshot: MediaPermissionSnapshot,
    val shouldShowRationale: Boolean,
    val isPermanentlyDenied: Boolean,
    val launchRequest: (MediaRecordingFeature) -> Unit,
    val openAppSettings: () -> Unit,
    val refresh: () -> Unit
) {
    val hasCameraPermission: Boolean get() = snapshot.hasCameraPermission
    val hasMicrophonePermission: Boolean get() = snapshot.hasMicrophonePermission
    val isFullyGranted: Boolean get() = snapshot.isFullyGranted
}

/**
 * Utility and Jetpack Compose runtime helper for checking, requesting, and
 * recovering denied CAMERA and RECORD_AUDIO permissions for media recording features.
 */
class MediaPermissionHelper(context: Context) {

    private val appContext: Context = context.applicationContext ?: context

    fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasCameraPermission(): Boolean = isPermissionGranted(CAMERA_PERMISSION)

    fun hasMicrophonePermission(): Boolean = isPermissionGranted(MICROPHONE_PERMISSION)

    fun evaluateFeature(
        feature: MediaRecordingFeature = MediaRecordingFeature.CAMERA_AND_MICROPHONE
    ): MediaPermissionSnapshot {
        val grantedSet = feature.permissions.filter { isPermissionGranted(it) }.toSet()
        val missing = feature.permissions.filter { it !in grantedSet }
        return MediaPermissionSnapshot(
            hasCameraPermission = hasCameraPermission(),
            hasMicrophonePermission = hasMicrophonePermission(),
            grantedPermissions = grantedSet,
            missingPermissions = missing,
            isFullyGranted = missing.isEmpty()
        )
    }

    fun hasRequiredPermissions(
        feature: MediaRecordingFeature = MediaRecordingFeature.CAMERA_AND_MICROPHONE
    ): Boolean = evaluateFeature(feature).isFullyGranted

    fun getMissingPermissions(
        feature: MediaRecordingFeature = MediaRecordingFeature.CAMERA_AND_MICROPHONE
    ): Array<String> = evaluateFeature(feature).missingPermissions.toTypedArray()

    fun shouldShowRationale(
        activity: Activity?,
        feature: MediaRecordingFeature = MediaRecordingFeature.CAMERA_AND_MICROPHONE
    ): Boolean {
        if (activity == null) return false
        return getMissingPermissions(feature).any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
    }

    fun isPermanentlyDenied(
        activity: Activity?,
        feature: MediaRecordingFeature,
        hasRequestedBefore: Boolean
    ): Boolean {
        if (!hasRequestedBefore) return false
        val missing = getMissingPermissions(feature)
        if (missing.isEmpty()) return false
        return !shouldShowRationale(activity, feature)
    }

    fun createAppSettingsIntent(): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", appContext.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun openAppSettings(launchContext: Context = appContext): Boolean {
        return try {
            val intent = createAppSettingsIntent().apply {
                if (launchContext is Activity) {
                    flags = flags and Intent.FLAG_ACTIVITY_NEW_TASK.inv()
                }
            }
            launchContext.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        const val CAMERA_PERMISSION = Manifest.permission.CAMERA
        const val MICROPHONE_PERMISSION = Manifest.permission.RECORD_AUDIO

        val ALL_MEDIA_RECORDING_PERMISSIONS = arrayOf(
            CAMERA_PERMISSION,
            MICROPHONE_PERMISSION
        )

        tailrec fun findActivity(context: Context): Activity? = when (context) {
            is Activity -> context
            is ContextWrapper -> findActivity(context.baseContext)
            else -> null
        }
    }
}

@Composable
fun rememberMediaPermissionState(
    feature: MediaRecordingFeature = MediaRecordingFeature.CAMERA_AND_MICROPHONE,
    onResult: (allGranted: Boolean, results: Map<String, Boolean>) -> Unit = { _, _ -> }
): MediaPermissionControllerState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val helper = remember(context) { MediaPermissionHelper(context) }
    val activity = remember(context) { MediaPermissionHelper.findActivity(context) }

    var activeFeature by remember(feature) { mutableStateOf(feature) }
    var hasRequestedBefore by remember { mutableStateOf(false) }
    var refreshTick by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTick++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grantResults ->
        hasRequestedBefore = true
        refreshTick++
        val allGranted = helper.hasRequiredPermissions(activeFeature)
        onResult(allGranted, grantResults)
    }

    val snapshot = remember(refreshTick, activeFeature, helper) {
        helper.evaluateFeature(activeFeature)
    }
    val showRationale = remember(refreshTick, activeFeature, activity, snapshot) {
        helper.shouldShowRationale(activity, activeFeature)
    }
    val permanentlyDenied = remember(refreshTick, activeFeature, activity, hasRequestedBefore, snapshot) {
        helper.isPermanentlyDenied(activity, activeFeature, hasRequestedBefore)
    }

    return MediaPermissionControllerState(
        feature = activeFeature,
        snapshot = snapshot,
        shouldShowRationale = showRationale,
        isPermanentlyDenied = permanentlyDenied,
        launchRequest = { requestedFeature ->
            activeFeature = requestedFeature
            val missing = helper.getMissingPermissions(requestedFeature)
            if (missing.isEmpty()) {
                refreshTick++
                onResult(
                    true,
                    requestedFeature.permissions.associateWith { true }
                )
            } else {
                launcher.launch(missing)
            }
        },
        openAppSettings = {
            helper.openAppSettings(context)
        },
        refresh = {
            refreshTick++
        }
    )
}
