package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.BookmarkedPlace
import com.example.data.db.UserRating
import com.example.data.model.Place
import com.example.ui.viewmodel.LocationViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScreen(
    viewModel: LocationViewModel,
    onPlaceClick: (Place) -> Unit,
    modifier: Modifier = Modifier
) {
    val bookmarks by viewModel.bookmarkedPlaces.collectAsStateWithLifecycle()
    val ratings by viewModel.userSubmittedReviews.collectAsStateWithLifecycle()

    var selectedTabState by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Bookmarks", "My Reviews")

    val backgroundBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFF7F9FF),
            Color(0xFFCCE8E8).copy(alpha = 0.15f),
            Color(0xFFD1E4FF).copy(alpha = 0.25f),
            Color(0xFFF7F9FF)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        // High fidelity ambient glow circles
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0xFF006A6A).copy(alpha = 0.08f),
                radius = 200.dp.toPx(),
                center = Offset(50.dp.toPx(), 150.dp.toPx())
            )
            drawCircle(
                color = Color(0xFFD1E4FF).copy(alpha = 0.15f),
                radius = 250.dp.toPx(),
                center = Offset(size.width - 50.dp.toPx(), size.height - 200.dp.toPx())
            )
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Saved Location Hub", fontWeight = FontWeight.ExtraBold, color = Color(0xFF191C1C)) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
            // Elegant Tab Row
            PrimaryTabRow(
                selectedTabIndex = selectedTabState,
                modifier = Modifier.fillMaxWidth().testTag("saved_screen_tab_row")
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabState == index,
                        onClick = { selectedTabState = index },
                        text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                        modifier = Modifier.testTag("saved_tab_$index")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                if (selectedTabState == 0) {
                    if (bookmarks.isEmpty()) {
                        EmptyTabState(
                            icon = Icons.Default.BookmarkBorder,
                            title = "No Bookmarks Yet",
                            message = "Tap the bookmark icon in any place detail page to save it here for fast offline access."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(bookmarks, key = { it.placeId }) { bookmarked ->
                                BookmarkedItemCard(
                                    bookmark = bookmarked,
                                    onClick = {
                                        val place = Place(
                                            id = bookmarked.placeId,
                                            name = bookmarked.name,
                                            address = bookmarked.address,
                                            latitude = bookmarked.latitude,
                                            longitude = bookmarked.longitude,
                                            category = bookmarked.category,
                                            description = bookmarked.description,
                                            rating = 4.5f, // generic fallback loading
                                            userRatingCount = 100
                                        )
                                        onPlaceClick(place)
                                    }
                                )
                            }
                        }
                    }
                } else {
                    if (ratings.isEmpty()) {
                        EmptyTabState(
                            icon = Icons.Default.RateReview,
                            title = "No Reviews Submitted",
                            message = "Select any nearby gas station, mosque, or hotel and share your experience by submitting a star rating."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(ratings, key = { it.id }) { rating ->
                                UserReviewHistoryCard(review = rating)
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
fun BookmarkedItemCard(
    bookmark: BookmarkedPlace,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE1E3E4)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bookmark_item_${bookmark.placeId}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        when (bookmark.category) {
                            "gas_station" -> MaterialTheme.colorScheme.secondaryContainer
                            "hotel" -> MaterialTheme.colorScheme.tertiaryContainer
                            "mosque" -> Color(0xFFE8F5E9)
                            else -> MaterialTheme.colorScheme.primaryContainer
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (bookmark.category) {
                        "gas_station" -> Icons.Default.LocalGasStation
                        "hotel" -> Icons.Default.Hotel
                        "mosque" -> Icons.Default.Mosque
                        else -> Icons.Default.Place
                    },
                    contentDescription = null,
                    tint = when (bookmark.category) {
                        "gas_station" -> MaterialTheme.colorScheme.onSecondaryContainer
                        "hotel" -> MaterialTheme.colorScheme.onTertiaryContainer
                        "mosque" -> Color(0xFF2E7D32)
                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                    },
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bookmark.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = bookmark.address,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun UserReviewHistoryCard(review: UserRating) {
    val date = Date(review.timestamp)
    val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val dateText = format.format(date)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE1E3E4)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = review.placeName,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(5) { index ->
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Star",
                        tint = if (index < review.rating) Color(0xFFFFB300) else MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "%.0f Stars".format(Locale.US, review.rating),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (review.comment.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "\"${review.comment}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

@Composable
fun EmptyTabState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
