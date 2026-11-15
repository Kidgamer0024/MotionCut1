package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ProjectEntity
import com.example.data.local.ProjectMappers
import com.example.model.AspectRatio
import com.example.model.ExportResolution
import com.example.model.FrameRate
import com.example.ui.CloudSyncState
import com.example.ui.EditorViewModel
import com.example.ui.ProjectSortMode
import com.example.ui.theme.CutRose
import com.example.ui.theme.KeyframeViolet
import com.example.ui.theme.MotionCyan
import com.example.ui.theme.PlayheadAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioDarkBg
import com.example.ui.theme.StudioDivider
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.SuccessEmerald

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandingScreen(
    viewModel: EditorViewModel,
    onOpenProject: (String) -> Unit,
    modifier: Modifier = Modifier,
    userDisplayName: String? = null,
    userEmail: String? = null,
    onSignOutClicked: (() -> Unit)? = null,
    onSignInClicked: (() -> Unit)? = null
) {
    val projects by viewModel.filteredProjects.collectAsStateWithLifecycle()
    val allProjectsCount by viewModel.savedProjects.collectAsStateWithLifecycle()
    val searchQuery by viewModel.projectSearchQuery.collectAsStateWithLifecycle()
    val sortMode by viewModel.projectSortMode.collectAsStateWithLifecycle()
    val cloudSyncState by viewModel.cloudSyncState.collectAsStateWithLifecycle()

    // Dialog States
    var projectToRename by remember { mutableStateOf<ProjectEntity?>(null) }
    var renameInputText by remember { mutableStateOf("") }
    var projectToDelete by remember { mutableStateOf<ProjectEntity?>(null) }
    var showNewProjectDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = StudioDarkBg,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showNewProjectDialog = true },
                containerColor = MotionCyan,
                contentColor = Color.Black,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Project", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("new_project_fab")
            )
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 320.dp),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 12.dp,
                bottom = innerPadding.calculateBottomPadding() + 80.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("landing_projects_grid")
        ) {
            // 1. Studio Header & Branding
            item(span = { GridItemSpan(maxLineSpan) }) {
                LandingHeader(
                    totalProjects = allProjectsCount.size,
                    userDisplayName = userDisplayName,
                    userEmail = userEmail,
                    onSignOutClicked = onSignOutClicked,
                    onSignInClicked = onSignInClicked,
                    onNewProjectClicked = { showNewProjectDialog = true }
                )
            }

            // 2. Search & Filter Bar
            item(span = { GridItemSpan(maxLineSpan) }) {
                LandingSearchAndSortBar(
                    searchQuery = searchQuery,
                    onSearchQueryChanged = { viewModel.setProjectSearchQuery(it) },
                    sortMode = sortMode,
                    onSortModeChanged = { viewModel.setProjectSortMode(it) }
                )
            }

            // 3. Quick-Start Creation Templates
            item(span = { GridItemSpan(maxLineSpan) }) {
                QuickTemplateRow(
                    onCreateFromTemplate = { title, aspect, fps, res ->
                        viewModel.createProject(title, aspect, fps, res)
                        val created = viewModel.currentProject.value
                        onOpenProject(created.id)
                    }
                )
            }

            // 4. Section Title
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MotionCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Saved Projects",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = CircleShape,
                            color = StudioSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder)
                        ) {
                            Text(
                                text = "${projects.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MotionCyan,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    val (syncLabel, syncTint, syncIcon) = when (cloudSyncState) {
                        is CloudSyncState.Synced -> Triple("Cloud Firestore + Room", SuccessEmerald, Icons.Default.CloudDone)
                        is CloudSyncState.Syncing -> Triple("Syncing Cloud...", MotionCyan, Icons.Default.CloudSync)
                        is CloudSyncState.Error -> Triple("Cloud Sync Error", CutRose, Icons.Default.CloudOff)
                        CloudSyncState.LocalOnly -> Triple("Room Local Database", Color.Gray, Icons.Default.Storage)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = syncIcon,
                            contentDescription = syncLabel,
                            tint = syncTint,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = syncLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = syncTint
                        )
                    }
                }
            }

            // 5. Empty State (if no projects found)
            if (projects.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyProjectsState(
                        isSearching = searchQuery.isNotBlank(),
                        onClearSearch = { viewModel.setProjectSearchQuery("") },
                        onCreateProject = { showNewProjectDialog = true }
                    )
                }
            } else {
                // 6. Saved Project Cards
                items(projects, key = { it.id }) { project ->
                    VideoProjectCard(
                        project = project,
                        onClick = {
                            viewModel.selectProject(project.id)
                            onOpenProject(project.id)
                        },
                        onRenameRequested = {
                            projectToRename = project
                            renameInputText = project.title
                        },
                        onDuplicateRequested = {
                            viewModel.duplicateProject(project.id)
                        },
                        onDeleteRequested = {
                            projectToDelete = project
                        }
                    )
                }
            }
        }
    }

    // --- RENAME DIALOG ---
    projectToRename?.let { project ->
        AlertDialog(
            onDismissRequest = { projectToRename = null },
            modifier = Modifier.testTag("rename_project_dialog"),
            containerColor = StudioDarkBg,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MotionCyan
                    )
                    Text(
                        text = "Rename Project",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Enter a new name for your video project:",
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = renameInputText,
                        onValueChange = { renameInputText = it },
                        singleLine = true,
                        placeholder = { Text("Project Title") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MotionCyan,
                            unfocusedBorderColor = StudioBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = StudioSurface,
                            unfocusedContainerColor = StudioSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rename_project_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = renameInputText.trim()
                        if (trimmed.isNotEmpty()) {
                            viewModel.renameProject(project.id, trimmed)
                        }
                        projectToRename = null
                    },
                    enabled = renameInputText.trim().isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MotionCyan,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.testTag("save_rename_button")
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { projectToRename = null },
                    modifier = Modifier.testTag("cancel_rename_button")
                ) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // --- DELETE CONFIRMATION DIALOG ---
    projectToDelete?.let { project ->
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            modifier = Modifier.testTag("delete_project_dialog"),
            containerColor = StudioDarkBg,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = CutRose
                    )
                    Text(
                        text = "Delete Project?",
                        color = CutRose,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Are you sure you want to permanently delete \"${project.title}\"?",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "This will remove all video clips, keyframe tracks, audio, and color gradings from your local Room database. This action cannot be undone.",
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProject(project.id)
                        projectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CutRose,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { projectToDelete = null },
                    modifier = Modifier.testTag("cancel_delete_button")
                ) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // --- NEW PROJECT CREATION DIALOG ---
    if (showNewProjectDialog) {
        NewProjectDialog(
            onDismiss = { showNewProjectDialog = false },
            onCreate = { title, aspect, fps, res ->
                viewModel.createProject(title, aspect, fps, res)
                showNewProjectDialog = false
                val created = viewModel.currentProject.value
                onOpenProject(created.id)
            }
        )
    }
}

@Composable
private fun LandingHeader(
    totalProjects: Int,
    userDisplayName: String?,
    userEmail: String?,
    onSignOutClicked: (() -> Unit)?,
    onSignInClicked: (() -> Unit)?,
    onNewProjectClicked: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.dp,
                Brush.horizontalGradient(listOf(MotionCyan.copy(alpha = 0.5f), KeyframeViolet.copy(alpha = 0.4f))),
                RoundedCornerShape(16.dp)
            ),
        color = StudioSurface
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MotionCyan.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MotionCyan.copy(alpha = 0.18f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MotionCyan.copy(alpha = 0.4f)),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = null,
                                    tint = MotionCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "MotionCut",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = CutRose.copy(alpha = 0.25f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CutRose.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "PRO",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CutRose,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = userDisplayName?.takeIf { it.isNotBlank() }
                                    ?: userEmail?.takeIf { it.isNotBlank() }
                                    ?: "Advanced Mobile Video & Motion Studio",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Total project counter pill
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = StudioSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = PlayheadAmber,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "$totalProjects Saved",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.LightGray,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        if (onSignOutClicked != null) {
                            IconButton(
                                onClick = onSignOutClicked,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                                    .background(StudioSurfaceVariant)
                                    .testTag("sign_out_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = "Sign Out",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else if (onSignInClicked != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MotionCyan.copy(alpha = 0.16f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MotionCyan.copy(alpha = 0.65f)),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onSignInClicked() }
                                    .testTag("header_sign_in_button")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Sign In",
                                        color = MotionCyan,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Hero New Project Banner Button
                Button(
                    onClick = onNewProjectClicked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("new_project_hero_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MotionCyan,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Create New Video Project",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun LandingSearchAndSortBar(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    sortMode: ProjectSortMode,
    onSortModeChanged: (ProjectSortMode) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Search TextField
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            placeholder = { Text("Search projects by name...", color = Color.Gray, fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = if (searchQuery.isNotEmpty()) MotionCyan else Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChanged("") },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MotionCyan,
                unfocusedBorderColor = StudioBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = StudioSurface,
                unfocusedContainerColor = StudioSurface
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_projects_input")
        )

        // Sort Mode Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Sort,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(16.dp)
            )
            ProjectSortMode.values().forEach { mode ->
                val isSelected = sortMode == mode
                FilterChip(
                    selected = isSelected,
                    onClick = { onSortModeChanged(mode) },
                    label = {
                        Text(
                            text = mode.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MotionCyan.copy(alpha = 0.2f),
                        selectedLabelColor = MotionCyan,
                        containerColor = StudioSurface,
                        labelColor = Color.LightGray
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = StudioBorder,
                        selectedBorderColor = MotionCyan
                    ),
                    modifier = Modifier.testTag("sort_chip_${mode.name}")
                )
            }
        }
    }
}

@Composable
private fun QuickTemplateRow(
    onCreateFromTemplate: (String, AspectRatio, FrameRate, ExportResolution) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Quick Presets",
            style = MaterialTheme.typography.labelMedium,
            color = Color.LightGray,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TemplateChip(
                label = "TikTok / Reels",
                ratio = "9:16",
                color = MotionCyan,
                modifier = Modifier.weight(1f)
            ) {
                onCreateFromTemplate("TikTok Motion Edit", AspectRatio.PORTRAIT_9_16, FrameRate.FPS_60, ExportResolution.FHD_1080P)
            }

            TemplateChip(
                label = "YouTube",
                ratio = "16:9",
                color = CutRose,
                modifier = Modifier.weight(1f)
            ) {
                onCreateFromTemplate("YouTube 4K Cinema", AspectRatio.LANDSCAPE_16_9, FrameRate.FPS_24, ExportResolution.UHD_4K)
            }

            TemplateChip(
                label = "Instagram",
                ratio = "1:1",
                color = KeyframeViolet,
                modifier = Modifier.weight(1f)
            ) {
                onCreateFromTemplate("Insta Square Reel", AspectRatio.SQUARE_1_1, FrameRate.FPS_30, ExportResolution.FHD_1080P)
            }
        }
    }
}

@Composable
private fun TemplateChip(
    label: String,
    ratio: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = StudioSurface
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = ratio,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun VideoProjectCard(
    project: ProjectEntity,
    onClick: () -> Unit,
    onRenameRequested: () -> Unit,
    onDuplicateRequested: () -> Unit,
    onDeleteRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, StudioBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("project_card_${project.id}"),
        colors = CardDefaults.cardColors(containerColor = StudioCardBg),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Thumbnail Viewport with Overlays
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color(project.thumbnailColorHex))
            ) {
                if (project.thumbnailResId != null) {
                    Image(
                        painter = painterResource(id = project.thumbnailResId),
                        contentDescription = "Thumbnail for ${project.title}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Procedural Fallback Thumbnail
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(project.thumbnailColorHex),
                                        StudioSurface
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                // Cinematic subtle vignette / bottom gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.75f)
                                )
                            )
                        )
                )

                // Top-Left Aspect Ratio Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = project.aspectRatioRatioText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MotionCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Top-Right Spec Pill
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "${project.frameRateFps} fps",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Bottom-Left Tracks & Clips Badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "${project.trackCount} Tracks • ${project.clipCount} Clips",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Bottom-Right Duration Badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.8f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = ProjectMappers.formatDuration(project.durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // 2. Card Metadata & Options Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "Edited ${ProjectMappers.formatProjectTimestamp(project.updatedAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "•",
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                        Text(
                            text = project.resolutionLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }
                }

                // Three-dot Options Menu
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("project_options_button_${project.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Project options",
                            tint = Color.LightGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier
                            .background(StudioSurface)
                            .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Open in Editor", color = Color.White) },
                            leadingIcon = {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, tint = MotionCyan)
                            },
                            onClick = {
                                menuExpanded = false
                                onClick()
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Rename", color = Color.White) },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = PlayheadAmber)
                            },
                            onClick = {
                                menuExpanded = false
                                onRenameRequested()
                            },
                            modifier = Modifier.testTag("option_rename_${project.id}")
                        )

                        DropdownMenuItem(
                            text = { Text("Duplicate", color = Color.White) },
                            leadingIcon = {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = KeyframeViolet)
                            },
                            onClick = {
                                menuExpanded = false
                                onDuplicateRequested()
                            },
                            modifier = Modifier.testTag("option_duplicate_${project.id}")
                        )

                        DropdownMenuItem(
                            text = { Text("Delete", color = CutRose) },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = CutRose)
                            },
                            onClick = {
                                menuExpanded = false
                                onDeleteRequested()
                            },
                            modifier = Modifier.testTag("option_delete_${project.id}")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyProjectsState(
    isSearching: Boolean,
    onClearSearch: () -> Unit,
    onCreateProject: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, StudioBorder, RoundedCornerShape(16.dp)),
        color = StudioSurface
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isSearching) Icons.Default.Search else Icons.Default.Folder,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isSearching) "No matching projects" else "No projects saved yet",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isSearching) "Try a different search query" else "Create your first cinematic video project to get started",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (isSearching) {
                OutlinedButton(
                    onClick = onClearSearch,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Clear Search", color = MotionCyan)
                }
            } else {
                Button(
                    onClick = onCreateProject,
                    colors = ButtonDefaults.buttonColors(containerColor = MotionCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Create New Project", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun NewProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (String, AspectRatio, FrameRate, ExportResolution) -> Unit
) {
    var title by remember { mutableStateOf("New Cinematic Edit") }
    var selectedAspect by remember { mutableStateOf(AspectRatio.PORTRAIT_9_16) }
    var selectedFps by remember { mutableStateOf(FrameRate.FPS_60) }
    var selectedRes by remember { mutableStateOf(ExportResolution.FHD_1080P) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("new_project_dialog"),
        containerColor = StudioDarkBg,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Videocam, contentDescription = null, tint = MotionCyan)
                Text("Create Video Project", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Project Title") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MotionCyan,
                        unfocusedBorderColor = StudioBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = StudioSurface,
                        unfocusedContainerColor = StudioSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Aspect Ratio", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        AspectRatio.PORTRAIT_9_16,
                        AspectRatio.LANDSCAPE_16_9,
                        AspectRatio.SQUARE_1_1,
                        AspectRatio.CINEMATIC_21_9
                    ).forEach { ratio ->
                        val isSel = selectedAspect == ratio
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.dp, if (isSel) MotionCyan else StudioBorder, RoundedCornerShape(6.dp))
                                .clickable { selectedAspect = ratio },
                            color = if (isSel) MotionCyan.copy(alpha = 0.2f) else StudioSurface
                        ) {
                            Text(
                                text = ratio.ratioText,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSel) MotionCyan else Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Text("Framerate", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(FrameRate.FPS_24, FrameRate.FPS_30, FrameRate.FPS_60).forEach { fps ->
                        val isSel = selectedFps == fps
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.dp, if (isSel) MotionCyan else StudioBorder, RoundedCornerShape(6.dp))
                                .clickable { selectedFps = fps },
                            color = if (isSel) MotionCyan.copy(alpha = 0.2f) else StudioSurface
                        ) {
                            Text(
                                text = "${fps.fps} fps",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSel) MotionCyan else Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalTitle = title.trim().ifEmpty { "Untitled Project" }
                    onCreate(finalTitle, selectedAspect, selectedFps, selectedRes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MotionCyan, contentColor = Color.Black),
                modifier = Modifier.testTag("confirm_create_project_button")
            ) {
                Text("Create Project", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}
