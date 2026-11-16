package com.example.data.remote

import com.example.base.FirestoreEmulatorTestBase
import com.example.model.AspectRatio
import com.example.model.ExportResolution
import com.example.model.FrameRate
import com.example.model.Project
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class FirestoreProjectRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureUserProfile_authenticatedOwner_createsAndUpdatesProfile() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = FirestoreProjectRepository(firestore, auth)

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureUserProfile("Alice Creator", "https://example.com/alice.png")
        }
        assertTrue("Expected profile creation success, got ${result.exceptionOrNull()}", result.isSuccess)
        val profile = result.getOrThrow()
        assertEquals(uid, profile.userId)
        assertEquals("Alice Creator", profile.displayName)
        assertNotNull(profile.createdAt)
    }

    @Test
    fun upsertAndObserveCloudProjects_authenticatedOwner_emitsRealtimeProject() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val uid = auth.currentUser!!.uid
        val repo = FirestoreProjectRepository(firestore, auth)

        val projId = "proj_${UUID.randomUUID().toString().replace("-", "_")}"
        val project = Project(
            id = projId,
            title = "Emulator Studio Cut",
            aspectRatio = AspectRatio.PORTRAIT_9_16,
            frameRate = FrameRate.FPS_60,
            resolution = ExportResolution.FHD_1080P,
            durationMs = 15_000L
        )

        val upsertResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.upsertCloudProject(project)
        }
        assertTrue("Upsert failed: ${upsertResult.exceptionOrNull()}", upsertResult.isSuccess)
        assertEquals(projId, upsertResult.getOrThrow())

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeCloudProjects(uid).first { list -> list.any { it.id == projId } }
        }
        val found = observed.find { it.id == projId }
        assertNotNull(found)
        assertEquals("Emulator Studio Cut", found!!.title)

        val renameResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.renameCloudProject(projId, "Renamed Director Cut")
        }
        assertTrue("Rename failed: ${renameResult.exceptionOrNull()}", renameResult.isSuccess)

        val renamed = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.getCloudProjectById(projId).getOrThrow()
        }
        assertNotNull(renamed)
        assertEquals("Renamed Director Cut", renamed!!.title)

        val deleteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.deleteCloudProject(projId)
        }
        assertTrue("Delete failed: ${deleteResult.exceptionOrNull()}", deleteResult.isSuccess)

        val afterDelete = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.getCloudProjectById(projId).getOrThrow()
        }
        assertNull(afterDelete)
    }

    @Test
    fun getCloudProjectById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = FirestoreProjectRepository(firestore, auth)
        val projId = "proj_${UUID.randomUUID().toString().replace("-", "_")}"
        val project = Project(id = projId, title = "Alice Secret Cut")
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.upsertCloudProject(project).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = FirestoreProjectRepository(firestore, auth)

        val readResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getCloudProjectById(projId, targetUserId = aliceUid)
        }
        assertTrue(readResult.isFailure)
        val firestoreException = readResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException", firestoreException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException!!.code)
    }

    @Test
    fun observeCloudProjects_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = FirestoreProjectRepository(firestore, auth)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeCloudProjects("alice_test_uid").first()
            }
            fail("Expected PERMISSION_DENIED exception when unauthenticated")
        } catch (t: Throwable) {
            caught = t
        }
        val firestoreException = caught?.unwrapFirestoreException()
        assertNotNull("Expected nested FirebaseFirestoreException, got $caught", firestoreException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException!!.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@motioncut.test"
        const val BOB_EMAIL = "bob@motioncut.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
