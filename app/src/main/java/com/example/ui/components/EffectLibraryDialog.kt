package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.EffectCatalog
import com.example.model.EffectPreset
import com.example.model.EffectSuite
import com.example.ui.theme.MotionCyan
import com.example.ui.theme.PlayheadAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioDarkBg
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant

@Composable
fun EffectLibraryDialog(
    favoriteIds: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onSelectEffect: (EffectPreset) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSuite by remember { mutableStateOf(EffectSuite.ALL) }
    var selectedCategory by remember { mutableStateOf("All") }
    var favoritesOnly by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val totalCount = remember { EffectCatalog.allPresets.size }

    val filteredPresets by remember(selectedSuite, selectedCategory, favoritesOnly, searchQuery, favoriteIds) {
        derivedStateOf {
            EffectCatalog.filterPresets(
                suite = selectedSuite,
                category = selectedCategory,
                query = searchQuery,
                favoritesOnly = favoritesOnly,
                favoriteIds = favoriteIds
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(18.dp))
                .testTag("effect_library_dialog"),
            color = StudioDarkBg,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, StudioBorder)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StudioSurface)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(selectedSuite.accentHex),
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Effects Library",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$totalCount effects across 5 pro suites • ${filteredPresets.size} shown",
                                style = MaterialTheme.typography.labelSmall,
                                color = MotionCyan,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("fx_library_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Effects Library",
                            tint = Color.LightGray
                        )
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search 2,200+ effects (Tiles, Glow, Shake, Saber, DV...)",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MotionCyan, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MotionCyan,
                        unfocusedBorderColor = StudioBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = StudioSurfaceVariant,
                        unfocusedContainerColor = StudioSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("fx_library_search_input")
                )

                // Suite Tabs (All, Alight Motion, Gold Motion, Ultra Motion, Node Video, CapCut)
                val suites = EffectSuite.entries
                ScrollableTabRow(
                    selectedTabIndex = suites.indexOf(selectedSuite),
                    containerColor = StudioSurface,
                    contentColor = Color(selectedSuite.accentHex),
                    edgePadding = 10.dp,
                    indicator = { positions ->
                        val idx = suites.indexOf(selectedSuite)
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(positions[idx]),
                            color = Color(selectedSuite.accentHex),
                            height = 2.5.dp
                        )
                    },
                    divider = {}
                ) {
                    suites.forEach { suite ->
                        val count = remember(suite) { EffectCatalog.countForSuite(suite) }
                        val isSelected = suite == selectedSuite
                        val accent = Color(suite.accentHex)
                        Tab(
                            selected = isSelected,
                            onClick = { selectedSuite = suite },
                            modifier = Modifier.testTag("fx_suite_${suite.name.lowercase()}"),
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text(
                                        text = suite.displayName,
                                        color = if (isSelected) accent else Color.LightGray,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                    Surface(
                                        color = accent.copy(alpha = if (isSelected) 0.25f else 0.10f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "$count",
                                            color = accent,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        )
                    }
                }

                // Category Chips + Favorites Toggle
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        Surface(
                            color = if (favoritesOnly) PlayheadAmber.copy(alpha = 0.22f) else StudioSurfaceVariant,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, if (favoritesOnly) PlayheadAmber else StudioBorder),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { favoritesOnly = !favoritesOnly }
                                .testTag("fx_favorites_filter")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = PlayheadAmber,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Favorites (${favoriteIds.size})",
                                    color = if (favoritesOnly) PlayheadAmber else Color.LightGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    items(EffectCatalog.categories) { cat ->
                        val isSelected = !favoritesOnly && cat == selectedCategory
                        val accent = Color(selectedSuite.accentHex)
                        Surface(
                            color = if (isSelected) accent.copy(alpha = 0.22f) else StudioSurfaceVariant,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, if (isSelected) accent else StudioBorder),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    favoritesOnly = false
                                    selectedCategory = cat
                                }
                                .testTag("fx_cat_${cat.lowercase().replace(" ", "_")}")
                        ) {
                            Text(
                                text = cat,
                                color = if (isSelected) Color.White else Color.LightGray,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Effect Cards Grid
                if (filteredPresets.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No matching effects found",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Try clearing the search term or switching suites",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 156.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("fx_library_grid"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredPresets, key = { it.id }) { preset ->
                            EffectPresetCard(
                                preset = preset,
                                isFavorite = favoriteIds.contains(preset.id),
                                onToggleFavorite = { onToggleFavorite(preset.id) },
                                onClick = { onSelectEffect(preset) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EffectPresetCard(
    preset: EffectPreset,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    val suiteColor = Color(preset.suite.accentHex)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("fx_preset_${preset.id}"),
        color = StudioCardBg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, suiteColor.copy(alpha = 0.28f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = suiteColor.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = preset.suite.shortName.uppercase(),
                        color = suiteColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarOutline,
                        contentDescription = if (isFavorite) "Remove favorite" else "Favorite effect",
                        tint = if (isFavorite) PlayheadAmber else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = preset.name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = preset.category,
                    color = Color.LightGray,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${(preset.defaultIntensity * 100).toInt()}%",
                    color = suiteColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
