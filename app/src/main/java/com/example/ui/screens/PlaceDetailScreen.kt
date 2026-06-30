package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.UserRating
import com.example.data.model.Place
import com.example.ui.viewmodel.LocationViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceDetailScreen(
    viewModel: LocationViewModel,
    onBackClick: () -> Unit,
    onNavigateClick: (Place) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val place by viewModel.selectedPlace.collectAsStateWithLifecycle()
    val localReviews by viewModel.selectedPlaceReviews.collectAsStateWithLifecycle(emptyList())
    val isBookmarked by viewModel.isSelectedPlaceBookmarked.collectAsStateWithLifecycle(false)

    var showReviewDialog by remember { mutableStateOf(false) }

    if (place == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No place selected.")
        }
        return
    }

    val currentPlace = place!!

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
        // Soft glowing blur blobs
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
                    title = { Text(currentPlace.name, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.toggleBookmark(currentPlace) },
                            modifier = Modifier.testTag("bookmark_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (isBookmarked) Color(0xFF006A6A) else Color(0xFF191C1C)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { showReviewDialog = true },
                    icon = { Icon(Icons.Default.RateReview, "Write Review") },
                    text = { Text("Submit Rating") },
                    containerColor = Color(0xFFCCE8E8),
                    contentColor = Color(0xFF002020),
                    modifier = Modifier.testTag("write_review_fab")
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Visual Header Banner
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        when (currentPlace.category) {
                                            "gas_station" -> MaterialTheme.colorScheme.secondaryContainer
                                            "hotel" -> MaterialTheme.colorScheme.tertiaryContainer
                                            "mosque" -> Color(0xFFE8F5E9)
                                            else -> MaterialTheme.colorScheme.primaryContainer
                                        },
                                        MaterialTheme.colorScheme.surface
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (currentPlace.category) {
                                "gas_station" -> Icons.Default.LocalGasStation
                                "hotel" -> Icons.Default.Hotel
                                "mosque" -> Icons.Default.Mosque
                                else -> Icons.Default.Place
                            },
                            contentDescription = currentPlace.category,
                            tint = when (currentPlace.category) {
                                "gas_station" -> MaterialTheme.colorScheme.onSecondaryContainer
                                "hotel" -> MaterialTheme.colorScheme.onTertiaryContainer
                                "mosque" -> Color(0xFF2E7D32)
                                else -> MaterialTheme.colorScheme.onPrimaryContainer
                            },
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }

                // Place Main Info
                item {
                    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                        Text(
                            text = currentPlace.name,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = currentPlace.category.replace("_", " ").uppercase(Locale.US),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Rating",
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "%.1f".format(Locale.US, currentPlace.rating),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "(${currentPlace.userRatingCount} ratings)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = currentPlace.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 22.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        // Quick Navigation Action Buttons Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { onNavigateClick(currentPlace) },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(50.dp)
                                    .testTag("start_navigation_button"),
                                shape = RoundedCornerShape(25.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF006A6A),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.Navigation, contentDescription = "Nav")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Navigate (GPS)", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    val gmmIntentUri = Uri.parse("google.navigation:q=${currentPlace.latitude},${currentPlace.longitude}")
                                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                    mapIntent.setPackage("com.google.android.apps.maps")
                                    if (mapIntent.resolveActivity(context.packageManager) != null) {
                                        context.startActivity(mapIntent)
                                    } else {
                                        // Open standard web view coordinate maps link
                                        val webMapsUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${currentPlace.latitude},${currentPlace.longitude}")
                                        context.startActivity(Intent(Intent.ACTION_VIEW, webMapsUri))
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("google_maps_action"),
                                shape = RoundedCornerShape(25.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE1E3E4)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF3F4848)
                                )
                            ) {
                                Icon(Icons.Outlined.Map, contentDescription = "GMap")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Google Maps", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Business details: Address, Phone, Website
                        Text("Location Details", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))

                        DetailInfoRow(icon = Icons.Default.LocationOn, label = "Address", value = currentPlace.address)
                        if (currentPlace.phone.isNotEmpty()) {
                            DetailInfoRow(
                                icon = Icons.Default.Phone,
                                label = "Phone Number",
                                value = currentPlace.phone,
                                clickableAction = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${currentPlace.phone}"))
                                    context.startActivity(intent)
                                }
                            )
                        }
                        if (currentPlace.website.isNotEmpty()) {
                            DetailInfoRow(
                                icon = Icons.Default.Language,
                                label = "Website",
                                value = currentPlace.website,
                                clickableAction = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentPlace.website))
                                    context.startActivity(intent)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("User Reviews & Ratings", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Reviews List section
                if (localReviews.isEmpty()) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RateReview,
                                contentDescription = "No Reviews",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Be the first to rate!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(localReviews) { review ->
                        ReviewItemCard(review = review)
                    }
                }
            }
        }
    }
}

    // Write review bottom sheet/dialog
    if (showReviewDialog) {
        ReviewDialog(
            placeName = currentPlace.name,
            onDismiss = { showReviewDialog = false },
            onSubmit = { rating, comment, userName ->
                viewModel.submitRating(currentPlace, rating, comment, userName)
                showReviewDialog = false
            }
        )
    }
}

@Composable
fun DetailInfoRow(
    imageVector: ImageVector = Icons.Default.Info,
    icon: ImageVector,
    label: String,
    value: String,
    clickableAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .then(if (clickableAction != null) Modifier.clickable { clickableAction() } else Modifier),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = if (clickableAction != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (clickableAction != null) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun ReviewItemCard(review: UserRating) {
    val date = Date(review.timestamp)
    val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val dateText = format.format(date)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE1E3E4)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = review.userName,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
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
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "%.1f".format(Locale.US, review.rating),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            if (review.comment.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = review.comment,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ReviewDialog(
    placeName: String,
    onDismiss: () -> Unit,
    onSubmit: (rating: Float, comment: String, userName: String) -> Unit
) {
    var rating by remember { mutableFloatStateOf(5f) }
    var comment by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }

    var errorText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Rate $placeName",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Your submission will update this place's average score globally.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Star Selection Row
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Select Rating", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(5) { index ->
                            val starValue = index + 1
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "$starValue Stars",
                                tint = if (starValue <= rating) Color(0xFFFFB300) else MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable { rating = starValue.toFloat() }
                                    .testTag("star_rating_$starValue")
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "%.0f out of 5 Stars".format(Locale.US, rating),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Name Input
                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it },
                    label = { Text("Your Name") },
                    placeholder = { Text("e.g. John Doe") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reviewer_name_input")
                )

                // Comment Input
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Write your review (Optional)") },
                    placeholder = { Text("Provide details about customer service, wait times, cleanliness...") },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reviewer_comment_input")
                )

                if (errorText.isNotEmpty()) {
                    Text(text = errorText, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (userName.trim().isEmpty()) {
                        errorText = "Please enter your name to submit."
                    } else {
                        onSubmit(rating, comment.trim(), userName.trim())
                    }
                },
                modifier = Modifier.testTag("submit_review_confirm")
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
