package com.example.model

import java.util.UUID

enum class MarketplaceCategory(val label: String) {
    ALL("All Creations"),
    TEMPLATES("Video Templates"),
    TRANSITIONS("Transitions"),
    PRESETS("Color Presets / LUTs")
}

data class AuthorInfo(
    val name: String,
    val handle: String,
    val avatarInitials: String = name.take(2).uppercase(),
    val isVerified: Boolean = true
)

data class ReviewItem(
    val id: String = UUID.randomUUID().toString(),
    val userName: String,
    val userHandle: String,
    val rating: Int, // 1 to 5
    val comment: String,
    val date: String = "Today",
    val helpfulCount: Int = 0
)

data class MarketplaceItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val category: MarketplaceCategory,
    val description: String,
    val author: AuthorInfo,
    val downloadsCount: Int,
    val averageRating: Float,
    val reviews: List<ReviewItem> = emptyList(),
    val thumbnailRes: Int? = null,
    val accentColorHex: Long = 0xFF00E5FF,
    val tags: List<String> = emptyList(),
    val presetColorGrading: ColorGradingSettings? = null,
    val sampleDurationSec: Int = 10,
    val isStaffPick: Boolean = false
) {
    val ratingCount: Int
        get() = reviews.size
}
