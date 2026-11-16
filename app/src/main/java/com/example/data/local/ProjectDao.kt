package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object for video projects.
 * Adheres strictly to modern Room best practices:
 * - Flow<List<ProjectEntity>> for reactive UI updates
 * - suspend functions for write, update, and delete mutations
 */
@Dao
interface ProjectDao {

    @Query("SELECT * FROM video_projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM video_projects ORDER BY updatedAt DESC")
    suspend fun getAllProjectsSnapshot(): List<ProjectEntity>

    @Query("SELECT * FROM video_projects WHERE id = :id")
    suspend fun getProjectById(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjects(projects: List<ProjectEntity>)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("UPDATE video_projects SET title = :newTitle, updatedAt = :updatedAt WHERE id = :id")
    suspend fun renameProject(id: String, newTitle: String, updatedAt: Long)

    @Query("DELETE FROM video_projects WHERE id = :id")
    suspend fun deleteProjectById(id: String)

    @Query("DELETE FROM video_projects")
    suspend fun deleteAllProjects()

    @androidx.room.Transaction
    suspend fun replaceAllProjects(projects: List<ProjectEntity>) {
        deleteAllProjects()
        if (projects.isNotEmpty()) {
            insertProjects(projects)
        }
    }

    @Query("SELECT COUNT(*) FROM video_projects")
    suspend fun getProjectCount(): Int
}
