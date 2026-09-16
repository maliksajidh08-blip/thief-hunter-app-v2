package com.example.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AlertRed
import com.example.ui.theme.NavyCardDark
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.YellowAccent
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LiveTrackingScreen(
    viewModel: ThiefHunterViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val tracker = state.liveTrackerState

    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepAngle"
    )

    val beaconPulse by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beaconPulse"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // TOP GPS HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = viewModel.tr("live_tracking"),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = tracker.locationName,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                color = SafeGreen.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(SafeGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "GPS LOCK: ±${tracker.accuracyMeters}m",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeGreen
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // INTERACTIVE RADAR / SATELLITE CANVAS MAP
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NavyDark),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(2.dp, YellowAccent.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                .testTag("radar_canvas_card")
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxRadius = minOf(size.width, size.height) * 0.42f

                    // Map grid background lines
                    val gridSpacing = 40f
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

                    var x = 0f
                    while (x < size.width) {
                        drawLine(
                            color = Color(0x1AFFFFFF),
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1f,
                            pathEffect = dashEffect
                        )
                        x += gridSpacing
                    }

                    var y = 0f
                    while (y < size.height) {
                        drawLine(
                            color = Color(0x1AFFFFFF),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f,
                            pathEffect = dashEffect
                        )
                        y += gridSpacing
                    }

                    // Concentric Range Rings
                    for (fraction in listOf(0.33f, 0.66f, 1f)) {
                        drawCircle(
                            color = YellowAccent.copy(alpha = 0.25f),
                            radius = maxRadius * fraction,
                            center = center,
                            style = Stroke(width = 1.5f)
                        )
                    }

                    // Crosshair Axes
                    drawLine(
                        color = YellowAccent.copy(alpha = 0.35f),
                        start = Offset(center.x - maxRadius, center.y),
                        end = Offset(center.x + maxRadius, center.y),
                        strokeWidth = 1.5f
                    )
                    drawLine(
                        color = YellowAccent.copy(alpha = 0.35f),
                        start = Offset(center.x, center.y - maxRadius),
                        end = Offset(center.x, center.y + maxRadius),
                        strokeWidth = 1.5f
                    )

                    // Rotating Radar Sweep Beam
                    val rad = Math.toRadians(sweepAngle.toDouble())
                    val sweepEnd = Offset(
                        (center.x + maxRadius * cos(rad)).toFloat(),
                        (center.y + maxRadius * sin(rad)).toFloat()
                    )
                    drawLine(
                        color = YellowAccent,
                        start = center,
                        end = sweepEnd,
                        strokeWidth = 2.5f
                    )

                    // Target Device Beacon (Offset slightly from center to represent real device coordinates)
                    val targetOffset = Offset(center.x + 35f, center.y - 45f)

                    // Beacon Pulse Ring
                    drawCircle(
                        color = AlertRed.copy(alpha = 0.4f),
                        radius = beaconPulse + 12f,
                        center = targetOffset
                    )

                    // Beacon Dot
                    drawCircle(
                        color = AlertRed,
                        radius = 8f,
                        center = targetOffset
                    )

                    // Device Center Reference
                    drawCircle(
                        color = YellowAccent,
                        radius = 6f,
                        center = center
                    )
                }

                // RADAR OVERLAY CHIP
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "LAT: ${tracker.latitude}° N",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = YellowAccent
                        )
                        Text(
                            text = "LNG: ${tracker.longitude}° E",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = YellowAccent
                        )
                        Text(
                            text = "DISTANCE: ~ 14 METERS",
                            fontSize = 10.sp,
                            color = Color.White
                        )
                    }
                }

                // RADAR STATUS CHIP
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CellTower,
                            contentDescription = null,
                            tint = YellowAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "RADAR ACTIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // QUICK CONTROL BUTTONS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.triggerGlobalSiren() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (tracker.isSirenRinging) AlertRed else YellowAccent,
                    contentColor = if (tracker.isSirenRinging) Color.White else NavyDark
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("btn_tracking_siren")
            ) {
                Icon(
                    imageVector = if (tracker.isSirenRinging) Icons.Default.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (tracker.isSirenRinging) viewModel.tr("quick_stop") else viewModel.tr("ring_siren"),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = { viewModel.toggleTrackerLock() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (tracker.isDeviceLocked) SafeGreen else NavyPrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("btn_tracking_lock")
            ) {
                Icon(
                    imageVector = if (tracker.isDeviceLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (tracker.isDeviceLocked) viewModel.tr("unlock_device") else viewModel.tr("remote_lock"),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = { viewModel.toggleTorchStrobe() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (tracker.isTorchFlashing) YellowAccent else NavyCardDark,
                    contentColor = if (tracker.isTorchFlashing) NavyDark else Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("btn_tracking_strobe")
            ) {
                Icon(
                    imageVector = Icons.Default.FlashOn,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (tracker.isTorchFlashing) viewModel.tr("stop_strobe") else viewModel.tr("strobe_light"),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}
