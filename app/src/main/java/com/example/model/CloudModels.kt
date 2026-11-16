package com.example.model

import com.example.data.local.ProjectEntity
import com.example.data.local.ProjectMappers
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> {
        val safeName = displayName.trim().ifBlank { "MotionCut Creator" }.take(100)
        val raw = mutableMapOf<String, Any?>(
            "userId" to ownerUid,
            "displayName" to safeName,
            "photoUrl" to photoUrl?.takeIf { it.isNotBlank() }?.take(500),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }

    fun toUpdateMap(): Map<String, Any> {
        val safeName = displayName.trim().ifBlank { "MotionCut Creator" }.take(100)
        val raw = mutableMapOf<String, Any?>(
            "displayName" to safeName,
            "photoUrl" to photoUrl?.takeIf { it.isNotBlank() }?.take(500),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@IgnoreExtraProperties
data class CloudProject(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val aspectRatioName: String = AspectRatio.PORTRAIT_9_16.name,
    val aspectRatioRatioText: String = AspectRatio.PORTRAIT_9_16.ratioText,
    val frameRateFps: Long = 60L,
    val resolutionLabel: String = ExportResolution.FHD_1080P.label,
    val durationMs: Long = 10_000L,
    val thumbnailColorHex: Long = 0xFF141424,
    val thumbnailResId: Int? = null,
    val clipCount: Long = 1L,
    val trackCount: Long = 1L,
    val projectJson: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "userId" to ownerUid,
            "title" to title.trim().ifBlank { "Untitled Project" }.take(120),
            "aspectRatioName" to aspectRatioName.take(40).ifBlank { AspectRatio.PORTRAIT_9_16.name },
            "aspectRatioRatioText" to aspectRatioRatioText.take(20).ifBlank { AspectRatio.PORTRAIT_9_16.ratioText },
            "frameRateFps" to frameRateFps.coerceIn(1L, 240L),
            "resolutionLabel" to resolutionLabel.take(40).ifBlank { ExportResolution.FHD_1080P.label },
            "durationMs" to durationMs.coerceIn(0L, 86_400_000L),
            "thumbnailColorHex" to thumbnailColorHex,
            "thumbnailResId" to thumbnailResId,
            "clipCount" to clipCount.coerceIn(0L, 10_000L),
            "trackCount" to trackCount.coerceIn(0L, 500L),
            "projectJson" to projectJson.take(850_000),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }

    fun toMetadataUpdateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "title" to title.trim().ifBlank { "Untitled Project" }.take(120),
            "aspectRatioName" to aspectRatioName.take(40).ifBlank { AspectRatio.PORTRAIT_9_16.name },
            "aspectRatioRatioText" to aspectRatioRatioText.take(20).ifBlank { AspectRatio.PORTRAIT_9_16.ratioText },
            "frameRateFps" to frameRateFps.coerceIn(1L, 240L),
            "resolutionLabel" to resolutionLabel.take(40).ifBlank { ExportResolution.FHD_1080P.label },
            "durationMs" to durationMs.coerceIn(0L, 86_400_000L),
            "thumbnailColorHex" to thumbnailColorHex,
            "thumbnailResId" to thumbnailResId,
            "clipCount" to clipCount.coerceIn(0L, 10_000L),
            "trackCount" to trackCount.coerceIn(0L, 500L),
            "projectJson" to projectJson.take(850_000),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }

    fun toRoomEntity(): ProjectEntity {
        val createdMs = createdAt?.toDate()?.time ?: System.currentTimeMillis()
        val updatedMs = updatedAt?.toDate()?.time ?: createdMs
        return ProjectEntity(
            id = id,
            title = title,
            aspectRatioName = aspectRatioName,
            aspectRatioRatioText = aspectRatioRatioText,
            frameRateFps = frameRateFps.toInt(),
            resolutionLabel = resolutionLabel,
            durationMs = durationMs,
            thumbnailResId = thumbnailResId,
            thumbnailColorHex = thumbnailColorHex,
            clipCount = clipCount.toInt(),
            trackCount = trackCount.toInt(),
            projectJson = projectJson,
            createdAt = createdMs,
            updatedAt = updatedMs
        )
    }

    companion object {
        fun fromProject(project: Project, ownerUid: String): CloudProject {
            val entity = ProjectMappers.toEntity(project)
            val safeJson = if (entity.projectJson.length > 840_000) {
                com.example.data.local.ProjectJsonCodec.encodeBoundedForCloud(project, 840_000)
            } else {
                entity.projectJson
            }
            return fromEntity(entity.copy(projectJson = safeJson), ownerUid)
        }

        fun fromEntity(entity: ProjectEntity, ownerUid: String): CloudProject {
            val safeJson = if (entity.projectJson.length > 840_000) {
                com.example.data.local.ProjectJsonCodec.decode(entity.projectJson)
                    ?.let { com.example.data.local.ProjectJsonCodec.encodeBoundedForCloud(it, 840_000) }
                    ?: entity.projectJson.take(840_000)
            } else {
                entity.projectJson
            }
            return CloudProject(
                id = entity.id,
                userId = ownerUid,
                title = entity.title,
                aspectRatioName = entity.aspectRatioName,
                aspectRatioRatioText = entity.aspectRatioRatioText,
                frameRateFps = entity.frameRateFps.toLong(),
                resolutionLabel = entity.resolutionLabel,
                durationMs = entity.durationMs,
                thumbnailColorHex = entity.thumbnailColorHex,
                thumbnailResId = entity.thumbnailResId,
                clipCount = entity.clipCount.toLong(),
                trackCount = entity.trackCount.toLong(),
                projectJson = safeJson
            )
        }
    }
}
