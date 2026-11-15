package com.example.data

import com.example.model.Project
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActionRecord(
    val description: String,
    val projectSnapshot: Project,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * ActionStack manages global Undo and Redo history across all editing operations
 * including clip arrangements, keyframing, color grading, VFX, masks, and AI tools.
 */
class ActionStack(private val maxStackSize: Int = 50) {

    private val undoStack = mutableListOf<ActionRecord>()
    private val redoStack = mutableListOf<ActionRecord>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val _lastUndoDescription = MutableStateFlow<String?>(null)
    val lastUndoDescription: StateFlow<String?> = _lastUndoDescription.asStateFlow()

    private val _lastRedoDescription = MutableStateFlow<String?>(null)
    val lastRedoDescription: StateFlow<String?> = _lastRedoDescription.asStateFlow()

    private var lastRecordedTime = 0L
    private var lastRecordedDescription: String? = null

    /**
     * Push current state before a mutation occurs.
     * @param description Human-readable action label (e.g. "Split Clip", "Add Color Grade")
     * @param currentProject Snapshot of project prior to mutation
     * @param coalesceIntervalMs If positive, coalesces rapid identical edits (e.g. slider scrubbing)
     */
    fun pushAction(
        description: String,
        currentProject: Project,
        coalesceIntervalMs: Long = 0L
    ) {
        val now = System.currentTimeMillis()
        if (coalesceIntervalMs > 0 &&
            description == lastRecordedDescription &&
            (now - lastRecordedTime) < coalesceIntervalMs &&
            undoStack.isNotEmpty()
        ) {
            // Keep the earliest baseline snapshot within the coalesce window
            lastRecordedTime = now
            return
        }

        undoStack.add(ActionRecord(description, currentProject, now))
        if (undoStack.size > maxStackSize) {
            undoStack.removeAt(0)
        }
        redoStack.clear()

        lastRecordedTime = now
        lastRecordedDescription = description

        updateFlows()
    }

    /**
     * Undo the last action and return the previous Project snapshot.
     */
    fun undo(currentProject: Project): Pair<Project, String>? {
        if (undoStack.isEmpty()) return null
        val recordToRestore = undoStack.removeAt(undoStack.size - 1)
        redoStack.add(ActionRecord(recordToRestore.description, currentProject))
        if (redoStack.size > maxStackSize) {
            redoStack.removeAt(0)
        }
        lastRecordedDescription = null
        updateFlows()
        return Pair(recordToRestore.projectSnapshot, recordToRestore.description)
    }

    /**
     * Redo the previously undone action.
     */
    fun redo(currentProject: Project): Pair<Project, String>? {
        if (redoStack.isEmpty()) return null
        val recordToRestore = redoStack.removeAt(redoStack.size - 1)
        undoStack.add(ActionRecord(recordToRestore.description, currentProject))
        if (undoStack.size > maxStackSize) {
            undoStack.removeAt(0)
        }
        lastRecordedDescription = null
        updateFlows()
        return Pair(recordToRestore.projectSnapshot, recordToRestore.description)
    }

    /**
     * Clear undo and redo history (e.g. when switching active projects).
     */
    fun clear() {
        undoStack.clear()
        redoStack.clear()
        lastRecordedDescription = null
        updateFlows()
    }

    val undoCount: Int
        get() = undoStack.size

    val redoCount: Int
        get() = redoStack.size

    private fun updateFlows() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
        _lastUndoDescription.value = undoStack.lastOrNull()?.description
        _lastRedoDescription.value = redoStack.lastOrNull()?.description
    }
}
