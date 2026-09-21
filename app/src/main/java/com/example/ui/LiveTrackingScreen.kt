package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LocationHistoryEntity
import com.example.ui.theme.AlertRed
import com.example.ui.theme.NavyCardDark
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.YellowAccent
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LiveTrackingScreen(
    viewModel: ThiefHunterViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val tracker = state.liveTrackerState
    val context = LocalContext.current

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

    // Current coordinates (prefer latest Room fix or sensor telemetry)
    val curLat = state.lastKnownLocation?.latitude ?: tracker.latitude
    val curLng = state.lastKnownLocation?.longitude ?: tracker.longitude
    val latStr = String.format(Locale.US, "%.6f", curLat)
    val lngStr = String.format(Locale.US, "%.6f", curLng)
    val mapsUrl = "https://maps.google.com/?q=$latStr,$lngStr"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. TOP HEADER & STATUS
        item {
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
                        text = "Autonomous 30s Geo-Lock • Cellular SMS Fallback",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = if (state.isLocationTracking30sActive) SafeGreen.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.2f),
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
                                .background(if (state.isLocationTracking30sActive) SafeGreen else Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (state.isLocationTracking30sActive) "30s ACTIVE" else "PAUSED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.isLocationTracking30sActive) SafeGreen else Color.Gray
                        )
                    }
                }
            }
        }

        // 2. LAST KNOWN LOCATION CARD (HERO)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = NavyCardDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = YellowAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LAST KNOWN LOCATION",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = YellowAccent
                            )
                        }

                        Surface(
                            color = SafeGreen.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "±${state.lastKnownLocation?.accuracy?.toInt() ?: tracker.accuracyMeters}m ACCURACY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SafeGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Coordinates display
                    Surface(
                        color = NavyDark,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "$latStr, $lngStr",
                                    fontSize = 15.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = state.lastKnownLocation?.formattedTime ?: "Fix active now • Saved in Room DB",
                                    fontSize = 11.sp,
                                    color = Color.LightGray
                                )
                            }

                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Coordinates", "$latStr, $lngStr"))
                                    Toast.makeText(context, "Coordinates copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Coordinates",
                                    tint = YellowAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // GOOGLE MAPS LINK ACTION BUTTON
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl)).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Cannot open map: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NavyPrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("btn_open_google_maps")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "OPEN GOOGLE MAPS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Maps Link", mapsUrl))
                                Toast.makeText(context, "Google Maps link copied!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text(text = "COPY LINK", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. SILENT SMS & FROZEN DEVICE TRIGGER CONTROLS
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = NavyCardDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Sms,
                                contentDescription = null,
                                tint = SafeGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CELLULAR SMS SENTINEL",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SafeGreen
                            )
                        }

                        Surface(
                            color = if (state.isDeviceFrozen) AlertRed.copy(alpha = 0.2f) else SafeGreen.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (state.isDeviceFrozen) "DEVICE FROZEN" else "UNFROZEN",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (state.isDeviceFrozen) AlertRed else SafeGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Dispatches SMS silently (no sound, no notification) when device is frozen, marked stolen, or offline without internet. Works across all networks.",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // SMS Format Preview
                    Surface(
                        color = NavyDark,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "SMS PAYLOAD FORMAT:",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = YellowAccent
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "THIEF HUNTER: $latStr, $lngStr",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Control Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Silent SMS Button
                        Button(
                            onClick = { viewModel.dispatchSilentLocationSmsManual() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SafeGreen,
                                contentColor = NavyDark
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("btn_send_silent_sms")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "SILENT SMS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Freeze Device Button
                        Button(
                            onClick = { viewModel.toggleDeviceFrozen() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.isDeviceFrozen) AlertRed else NavyPrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("btn_toggle_freeze")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AcUnit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (state.isDeviceFrozen) "UNFREEZE" else "FREEZE DEV",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // 30s Tracking Toggle
                        OutlinedButton(
                            onClick = { viewModel.toggle30sLocationTracking() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (state.isLocationTracking30sActive) SafeGreen else Color.Gray
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (state.isLocationTracking30sActive) "30s ON" else "30s OFF",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (state.isLocationTracking30sActive) SafeGreen else Color.Gray
                            )
                        }
                    }
                }
            }
        }

        // 4. RADAR / SATELLITE CANVAS
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NavyCardDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val maxRadius = (size.minDimension / 2f) - 16.dp.toPx()

                        // Grid Circles
                        for (i in 1..4) {
                            val r = (maxRadius / 4f) * i
                            drawCircle(
                                color = NavyPrimary.copy(alpha = 0.45f),
                                radius = r,
                                center = center,
                                style = Stroke(
                                    width = 1.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                )
                            )
                        }

                        // Crosshairs
                        drawLine(
                            color = NavyPrimary.copy(alpha = 0.35f),
                            start = Offset(center.x, 16.dp.toPx()),
                            end = Offset(center.x, size.height - 16.dp.toPx()),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawLine(
                            color = NavyPrimary.copy(alpha = 0.35f),
                            start = Offset(16.dp.toPx(), center.y),
                            end = Offset(size.width - 16.dp.toPx(), center.y),
                            strokeWidth = 1.dp.toPx()
                        )

                        // Rotating Radar Sweep Line
                        val rad = Math.toRadians(sweepAngle.toDouble())
                        val endX = center.x + maxRadius * cos(rad).toFloat()
                        val endY = center.y + maxRadius * sin(rad).toFloat()
                        drawLine(
                            color = YellowAccent.copy(alpha = 0.85f),
                            start = center,
                            end = Offset(endX, endY),
                            strokeWidth = 2.dp.toPx()
                        )

                        // Target Device Beacon (Offset slightly from center)
                        val targetOffset = Offset(center.x + 35.dp.toPx(), center.y - 25.dp.toPx())
                        drawCircle(
                            color = SafeGreen.copy(alpha = 0.25f),
                            radius = beaconPulse.dp.toPx(),
                            center = targetOffset
                        )
                        drawCircle(
                            color = SafeGreen,
                            radius = 6.dp.toPx(),
                            center = targetOffset
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.dp.toPx(),
                            center = targetOffset
                        )

                        // Center User Device
                        drawCircle(
                            color = YellowAccent,
                            radius = 5.dp.toPx(),
                            center = center
                        )
                    }

                    // RADAR OVERLAY CHIP
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "LAT: $latStr",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = YellowAccent
                            )
                            Text(
                                text = "LNG: $lngStr",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = YellowAccent
                            )
                            Text(
                                text = "BATTERY ACCURACY: ZERO DRAIN",
                                fontSize = 9.sp,
                                color = SafeGreen
                            )
                        }
                    }

                    // RADAR STATUS CHIP
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
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
                                text = "RADAR SYNCED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // 5. LOCATION HISTORY HEADER (SAVED LOCALLY IN ROOM DB)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Location History (Offline Room DB)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = NavyPrimary.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${state.locationHistory.size} fixes",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = YellowAccent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (state.locationHistory.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { viewModel.clearLocationHistory() },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear",
                            modifier = Modifier.size(14.dp),
                            tint = AlertRed
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "CLEAR", fontSize = 10.sp, color = AlertRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 6. LOCATION HISTORY LIST
        if (state.locationHistory.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = NavyCardDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No stored fixes yet. 30-second offline tracker records automatically.",
                            fontSize = 12.sp,
                            color = Color.LightGray
                        )
                    }
                }
            }
        } else {
            items(state.locationHistory.take(25)) { item ->
                LocationHistoryItemCard(
                    item = item,
                    onOpenMap = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.googleMapsUrl)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot open map: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LocationHistoryItemCard(
    item: LocationHistoryEntity,
    onOpenMap: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCardDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.formattedTime,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = when (item.triggerSource) {
                            "FROZEN_DEVICE" -> AlertRed.copy(alpha = 0.25f)
                            "STOLEN_ALARM" -> AlertRed.copy(alpha = 0.25f)
                            else -> SafeGreen.copy(alpha = 0.2f)
                        },
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = item.triggerSource,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (item.triggerSource) {
                                "FROZEN_DEVICE" -> AlertRed
                                "STOLEN_ALARM" -> AlertRed
                                else -> SafeGreen
                            },
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                val latFormatted = String.format(Locale.US, "%.5f", item.latitude)
                val lngFormatted = String.format(Locale.US, "%.5f", item.longitude)
                Text(
                    text = "$latFormatted, $lngFormatted • ±${item.accuracy.toInt()}m",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = YellowAccent
                )

                if (item.isSentViaSms) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "✓ Silent SMS Dispatched to ${item.smsRecipient ?: "Emergency Contact"}",
                        fontSize = 10.sp,
                        color = SafeGreen
                    )
                }
            }

            IconButton(
                onClick = onOpenMap,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = "Open in Maps",
                    tint = YellowAccent,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
