package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.NavStep
import com.example.data.model.Place
import com.example.ui.viewmodel.LocationViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationScreen(
    viewModel: LocationViewModel,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val destPlace by viewModel.navDestination.collectAsStateWithLifecycle()
    val isNavigating by viewModel.isNavigating.collectAsStateWithLifecycle()

    val distanceRemaining by viewModel.navDistanceRemaining.collectAsStateWithLifecycle()
    val bearingDegrees by viewModel.navBearing.collectAsStateWithLifecycle()
    val etaMinutes by viewModel.navEtaMinutes.collectAsStateWithLifecycle()
    val speedKmh by viewModel.navSpeed.collectAsStateWithLifecycle()
    val isSimulating by viewModel.isSimulationActive.collectAsStateWithLifecycle()
    val navSteps by viewModel.navSteps.collectAsStateWithLifecycle()

    val gpsState by viewModel.gpsLocation.collectAsStateWithLifecycle()

    if (!isNavigating || destPlace == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = "Nav",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Active Route",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Select any gas station, hotel, mosque, or searched place and tap 'Navigate' to start real-time GPS tracking.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        return
    }

    val place = destPlace!!

    // Dynamic strings calculations
    val formattedDistance = if (distanceRemaining >= 1000.0) {
        "%.2f km".format(Locale.US, distanceRemaining / 1000.0)
    } else {
        "${distanceRemaining.toInt()} m"
    }

    val formattedEta = if (etaMinutes >= 60) {
        "${etaMinutes / 60} h ${etaMinutes % 60} m"
    } else {
        "$etaMinutes mins"
    }

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
                    title = {
                        Column {
                            Text(
                                text = "Navigating to ${place.name}",
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 18.sp,
                                color = Color(0xFF191C1C)
                            )
                            Text(
                                text = "Destination Coordinates: %.5f, %.5f".format(Locale.US, place.latitude, place.longitude),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF3F4848)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onCloseClick) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Exit Navigation", tint = Color(0xFF191C1C))
                        }
                    },
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
            // 1. Current Active Step Big Header Card
            val activeStep = navSteps.firstOrNull()
            if (activeStep != null) {
                Surface(
                    color = Color(0xFFCCE8E8),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF006A6A).copy(alpha = 0.2f)),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .testTag("active_nav_step_card")
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF006A6A).copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (activeStep.icon) {
                                    "left" -> Icons.Default.ArrowBack
                                    "right" -> Icons.Default.ArrowForward
                                    "arrive" -> Icons.Default.Flag
                                    else -> Icons.Default.ArrowUpward
                                },
                                contentDescription = activeStep.instruction,
                                tint = Color(0xFF002020),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeStep.instruction,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color(0xFF002020)
                            )
                            if (activeStep.distanceMeters > 0.1) {
                                Text(
                                    text = "In %.0f meters".format(Locale.US, activeStep.distanceMeters),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color(0xFF002020).copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Compass & Speed Dial Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive HUD Compass
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "BEARING DIRECTION",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Rotating Navigation Pointer Canvas
                    CompassDial(bearing = bearingDegrees.toFloat())
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    val directionWord = when (bearingDegrees) {
                        in 337.5..360.0, in 0.0..22.5 -> "North (0°)"
                        in 22.5..67.5 -> "North-East (45°)"
                        in 67.5..112.5 -> "East (90°)"
                        in 112.5..157.5 -> "South-East (135°)"
                        in 157.5..202.5 -> "South (180°)"
                        in 202.5..247.5 -> "South-West (225°)"
                        in 247.5..292.5 -> "West (270°)"
                        else -> "North-West (315°)"
                    }
                    Text(
                        text = directionWord,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Speed HUD display
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "GPS VELOCITY",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF3F4848)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE1E3E4)), CircleShape)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Minimalist circular speedometer path
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = Color.LightGray.copy(alpha = 0.3f),
                                style = Stroke(width = 6.dp.toPx())
                            )
                            drawArc(
                                color = if (speedKmh > 40.0) Color(0xFFE53935) else Color(0xFF006A6A),
                                startAngle = -220f,
                                sweepAngle = (speedKmh.toFloat() / 80f * 260f).coerceAtMost(260f),
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx())
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "%.0f".format(Locale.US, speedKmh),
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color(0xFF191C1C)
                            )
                            Text(
                                text = "km/h",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF3F4848)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isSimulating) "Driving Sim" else "Live Satellites",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (isSimulating) MaterialTheme.colorScheme.tertiary else Color(0xFF4CAF50)
                    )
                }
            }

            // 3. ETA & Distance Remaining Segment
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE1E3E4)),
                shadowElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("REMAINING DISTANCE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF3F4848))
                        Text(
                            text = formattedDistance,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = Color(0xFF006A6A),
                            modifier = Modifier.testTag("nav_distance_remaining")
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(Color(0xFFE1E3E4))
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ESTIMATED ETA", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF3F4848))
                        Text(
                            text = formattedEta,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = Color(0xFF191C1C),
                            modifier = Modifier.testTag("nav_eta")
                        )
                    }
                }
            }

            // 4. Simulator and Maps actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Simulation switch
                Button(
                    onClick = { viewModel.toggleSimulation() },
                    shape = RoundedCornerShape(25.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("simulation_toggle_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSimulating) Color(0xFFE53935) 
                                             else Color(0xFFCCE8E8),
                        contentColor = if (isSimulating) Color.White else Color(0xFF002020)
                    )
                ) {
                    Icon(
                        imageVector = if (isSimulating) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Simulate Walk"
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSimulating) "Stop Simulator" else "Demo Driving Walk",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // External Directions
                OutlinedButton(
                    onClick = {
                        val mapsIntentUri = Uri.parse("google.navigation:q=${place.latitude},${place.longitude}")
                        val mapIntent = Intent(Intent.ACTION_VIEW, mapsIntentUri)
                        mapIntent.setPackage("com.google.android.apps.maps")
                        context.startActivity(mapIntent)
                    },
                    shape = RoundedCornerShape(25.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE1E3E4)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF3F4848)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("google_maps_nav_intent")
                ) {
                    Icon(imageVector = Icons.Default.Launch, contentDescription = "Launch")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("External Nav", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 5. Remaining steps lists
            Text(
                text = "UPCOMING TURNS",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                if (navSteps.size <= 1) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("You are arriving at your destination!", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    // Skip the first one which is active
                    val remaining = navSteps.drop(1)
                    itemsIndexed(remaining) { index, step ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (step.icon) {
                                    "left" -> Icons.Default.ArrowBack
                                    "right" -> Icons.Default.ArrowForward
                                    "arrive" -> Icons.Default.Flag
                                    else -> Icons.Default.ArrowUpward
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = step.instruction,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (step.distanceMeters > 0) {
                                Text(
                                    text = "${step.distanceMeters.toInt()}m",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (index < remaining.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun CompassDial(bearing: Float) {
    // Elegant animating rotated compass
    val animatedBearing by animateFloatAsState(
        targetValue = bearing,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "Compass Pointer Rotate"
    )

    Box(
        modifier = Modifier
            .size(110.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE1E3E4)), CircleShape)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2

            // Draw outer Dial Ring
            drawCircle(
                color = Color.LightGray.copy(alpha = 0.3f),
                style = Stroke(width = 4.dp.toPx())
            )

            // Draw North, South, East, West cardinal ticks
            val tickLength = 6.dp.toPx()
            for (i in 0 until 360 step 90) {
                val angleRad = Math.toRadians(i.toDouble() - 90)
                val start = Offset(
                    (center.x + (radius - tickLength) * Math.cos(angleRad)).toFloat(),
                    (center.y + (radius - tickLength) * Math.sin(angleRad)).toFloat()
                )
                val end = Offset(
                    (center.x + radius * Math.cos(angleRad)).toFloat(),
                    (center.y + radius * Math.sin(angleRad)).toFloat()
                )
                drawStrokeLine(start, end, Color.Gray, 2.dp.toPx())
            }

            // Draw directional pointer arrow inside Dial
            rotate(degrees = -animatedBearing, pivot = center) {
                val arrowPath = Path().apply {
                    moveTo(center.x, center.y - radius + 14.dp.toPx())
                    lineTo(center.x - 10.dp.toPx(), center.y + 12.dp.toPx())
                    lineTo(center.x, center.y + 4.dp.toPx())
                    lineTo(center.x + 10.dp.toPx(), center.y + 12.dp.toPx())
                    close()
                }
                // Draw high-contrast North pointing tip
                drawPath(
                    path = arrowPath,
                    color = Color(0xFFE53935)
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStrokeLine(
    start: Offset,
    end: Offset,
    color: Color,
    strokeWidth: Float
) {
    drawLine(
        color = color,
        start = start,
        end = end,
        strokeWidth = strokeWidth
    )
}
