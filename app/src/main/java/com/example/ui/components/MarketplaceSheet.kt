package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.AuthorInfo
import com.example.model.MarketplaceCategory
import com.example.model.MarketplaceItem
import com.example.model.ReviewItem
import com.example.ui.theme.KeyframeViolet
import com.example.ui.theme.MotionCyan
import com.example.ui.theme.PlayheadAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioDarkBg
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.SuccessEmerald

@Composable
fun MarketplaceSheet(
    items: List<MarketplaceItem>,
    onDownloadItem: (MarketplaceItem) -> Unit,
    onAddReview: (String, ReviewItem) -> Unit,
    onPublishCreation: (MarketplaceItem) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(MarketplaceCategory.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    var selectedItemForReviews by remember { mutableStateOf<MarketplaceItem?>(null) }
    var showWriteReviewModal by remember { mutableStateOf(false) }
    var showPublishModal by remember { mutableStateOf(false) }

    val filteredItems = remember(selectedCategory, searchQuery, items) {
        items.filter { item ->
            (selectedCategory == MarketplaceCategory.ALL || item.category == selectedCategory) &&
            (searchQuery.isBlank() ||
             item.title.contains(searchQuery, ignoreCase = true) ||
             item.description.contains(searchQuery, ignoreCase = true) ||
             item.author.name.contains(searchQuery, ignoreCase = true) ||
             item.tags.any { it.contains(searchQuery, ignoreCase = true) })
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(620.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, StudioBorder, RoundedCornerShape(16.dp)),
            color = StudioDarkBg
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Storefront, contentDescription = null, tint = MotionCyan, modifier = Modifier.size(22.dp))
                        Column {
                            Text(
                                text = "MotionCut Creator Hub",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Templates, Transitions & Color Presets",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Publish Action
                        Button(
                            onClick = { showPublishModal = true },
                            modifier = Modifier.height(30.dp).testTag("publish_creation_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = KeyframeViolet, contentColor = Color.White),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Publish", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search templates, LUTs, transitions...", fontSize = 12.sp, color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MotionCyan,
                        unfocusedBorderColor = StudioBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Categories Row (All, Templates, Transitions, Presets)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(MarketplaceCategory.values()) { cat ->
                        val isSel = cat == selectedCategory
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) MotionCyan.copy(alpha = 0.25f) else StudioSurfaceVariant)
                                .border(1.dp, if (isSel) MotionCyan else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSel) MotionCyan else Color.LightGray,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Marketplace Items Grid / List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredItems) { item ->
                        MarketplaceCard(
                            item = item,
                            onDownload = { onDownloadItem(item) },
                            onOpenReviews = { selectedItemForReviews = item }
                        )
                    }
                }
            }
        }
    }

    // Reviews & Rating Dialog
    selectedItemForReviews?.let { item ->
        ReviewsDialog(
            item = item,
            onDismiss = { selectedItemForReviews = null },
            onWriteReview = {
                showWriteReviewModal = true
            }
        )
    }

    // Write Review Modal
    if (showWriteReviewModal && selectedItemForReviews != null) {
        val targetItem = selectedItemForReviews!!
        WriteReviewDialog(
            itemTitle = targetItem.title,
            onSubmit = { rating, comment ->
                val newReview = ReviewItem(
                    userName = "You (Motion Artist)",
                    userHandle = "@creator_pro",
                    rating = rating,
                    comment = comment,
                    date = "Just now"
                )
                onAddReview(targetItem.id, newReview)
                showWriteReviewModal = false
            },
            onDismiss = { showWriteReviewModal = false }
        )
    }

    // Publish Creation Modal
    if (showPublishModal) {
        PublishCreationDialog(
            onPublish = { newItem ->
                onPublishCreation(newItem)
                showPublishModal = false
            },
            onDismiss = { showPublishModal = false }
        )
    }
}

@Composable
private fun MarketplaceCard(
    item: MarketplaceItem,
    onDownload: () -> Unit,
    onOpenReviews: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, StudioBorder, RoundedCornerShape(10.dp)),
        color = StudioSurfaceVariant
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Category & Author Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(item.accentColorHex).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = item.category.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(item.accentColorHex),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = item.author.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "• ${item.downloadsCount / 1000}k DLs",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Title & Description
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                maxLines = 2,
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Ratings & Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Star rating clickable pill to open reviews
                Surface(
                    color = StudioDarkBg,
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
                    modifier = Modifier.clickable { onOpenReviews() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = PlayheadAmber, modifier = Modifier.size(13.dp))
                        Text(
                            text = String.format("%.1f", item.averageRating),
                            style = MaterialTheme.typography.labelSmall,
                            color = PlayheadAmber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "(${item.ratingCount} reviews)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontSize = 9.sp
                        )
                    }
                }

                // Download & Apply button
                Button(
                    onClick = onDownload,
                    modifier = Modifier.height(30.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MotionCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Apply", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun ReviewsDialog(
    item: MarketplaceItem,
    onDismiss: () -> Unit,
    onWriteReview: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, StudioBorder, RoundedCornerShape(16.dp)),
            color = StudioDarkBg
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Ratings & Reviews", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(text = item.title, style = MaterialTheme.typography.bodySmall, color = MotionCyan, fontSize = 11.sp)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Summary Score Card
                Surface(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
                    color = StudioSurfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = String.format("%.1f", item.averageRating),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = PlayheadAmber,
                                    fontWeight = FontWeight.Black
                                )
                                Column {
                                    Row {
                                        repeat(5) { idx ->
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = if (idx < item.averageRating.toInt()) PlayheadAmber else Color.DarkGray,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Text("${item.ratingCount} community ratings", style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontSize = 10.sp)
                                }
                            }
                        }

                        Button(
                            onClick = onWriteReview,
                            modifier = Modifier.height(32.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MotionCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.RateReview, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rate Item", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Review items list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (item.reviews.isEmpty()) {
                        item {
                            Text("No reviews yet. Be the first to leave a review!", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    } else {
                        items(item.reviews) { rev ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)),
                                color = StudioSurfaceVariant
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(text = rev.userName, style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                                        Row {
                                            repeat(rev.rating) {
                                                Icon(Icons.Default.Star, contentDescription = null, tint = PlayheadAmber, modifier = Modifier.size(12.dp))
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = rev.comment, style = MaterialTheme.typography.bodySmall, color = Color.LightGray, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WriteReviewDialog(
    itemTitle: String,
    onSubmit: (Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    var starRating by remember { mutableIntStateOf(5) }
    var reviewText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, StudioBorder, RoundedCornerShape(16.dp)),
            color = StudioDarkBg
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Rate & Review Creation", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                Text(text = itemTitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontSize = 11.sp)

                Spacer(modifier = Modifier.height(14.dp))

                // Interactive 5 Star Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    for (star in 1..5) {
                        IconButton(
                            onClick = { starRating = star },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (star <= starRating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "$star stars",
                                tint = if (star <= starRating) PlayheadAmber else Color.Gray,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = reviewText,
                    onValueChange = { reviewText = it },
                    placeholder = { Text("Write your feedback, tips or how you used it...", fontSize = 12.sp, color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MotionCyan,
                        unfocusedBorderColor = StudioBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().height(90.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceVariant, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (reviewText.isNotBlank()) {
                                onSubmit(starRating, reviewText)
                            }
                        },
                        modifier = Modifier.weight(1f).height(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MotionCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Submit Review", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PublishCreationDialog(
    onPublish: (MarketplaceItem) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("My Custom Video Preset") }
    var description by remember { mutableStateOf("Created with MotionCut advanced keyframe curve & color grade.") }
    var category by remember { mutableStateOf(MarketplaceCategory.PRESETS) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, StudioBorder, RoundedCornerShape(16.dp)),
            color = StudioDarkBg
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Publish Creation to Marketplace", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                Text(text = "Share with millions of mobile video editors worldwide", style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontSize = 10.sp)

                Spacer(modifier = Modifier.height(12.dp))

                // Title field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Creation Title") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MotionCyan,
                        unfocusedBorderColor = StudioBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Description field
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MotionCyan,
                        unfocusedBorderColor = StudioBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().height(80.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category selector
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(MarketplaceCategory.TEMPLATES, MarketplaceCategory.TRANSITIONS, MarketplaceCategory.PRESETS).forEach { cat ->
                        val isSel = cat == category
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.dp, if (isSel) KeyframeViolet else StudioBorder, RoundedCornerShape(6.dp))
                                .clickable { category = cat },
                            color = if (isSel) KeyframeViolet.copy(alpha = 0.25f) else StudioSurfaceVariant
                        ) {
                            Text(
                                text = cat.label.split(" ").first(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSel) KeyframeViolet else Color.White,
                                modifier = Modifier.padding(vertical = 6.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        val newItem = MarketplaceItem(
                            title = title,
                            category = category,
                            description = description,
                            author = AuthorInfo(name = "You", handle = "@creator_pro"),
                            downloadsCount = 1,
                            averageRating = 5.0f,
                            tags = listOf("Community", "Custom", category.name.lowercase())
                        )
                        onPublish(newItem)
                    },
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KeyframeViolet, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Publish Creation", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
