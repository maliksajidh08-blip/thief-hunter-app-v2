package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.layout.ContentScale
import androidx.camera.view.PreviewView
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.security.CameraCaptureHelper
import java.io.File
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Screen
import com.example.security.TriggerReason
import com.example.ui.theme.AlertRed
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.YellowAccent

@Composable
fun SensorsHubScreen(
    viewModel: ThiefHunterViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val tele = state.sensorTelemetry
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var isCameraPreviewOpen by remember { mutableStateOf(true) }
    var cameraSecondsRemaining by remember { mutableStateOf(5) }

    LaunchedEffect(state.isSirenPlaying) {
        if (state.isSirenPlaying) {
            isCameraPreviewOpen = true
            cameraSecondsRemaining = 5
            while (cameraSecondsRemaining > 0) {
                delay(1000L)
                cameraSecondsRemaining--
            }
            isCameraPreviewOpen = false
            CameraCaptureHelper.unbindAll(context)
        } else {
            isCameraPreviewOpen = false
            CameraCaptureHelper.unbindAll(context)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseColor by infiniteTransition.animateColor(
        initialValue = AlertRed,
        targetValue = YellowAccent,
        animationSpec = infiniteRepeatable(
            animation = tween(400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseColor"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // TOP ARM / DISARM HERO BANNER
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (state.isSirenPlaying) AlertRed else if (state.isSystemArmed) NavyDark else NavyPrimary
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_arm_status")
        ) {
            Column(
                modifier = Modifier
                    .background(
                        brush = Brush.verticalGradient(
                            if (state.isSirenPlaying) listOf(AlertRed, Color(0xFF8B0000))
                            else if (state.isSystemArmed) listOf(NavyDark, NavyPrimary)
                            else listOf(NavyPrimary, NavyDark)
                        )
                    )
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            if (state.isSirenPlaying) pulseColor
                            else if (state.isSystemArmed) SafeGreen
                            else YellowAccent
                        )
                ) {
                    Icon(
                        imageVector = if (state.isSirenPlaying) Icons.Default.Warning
                        else if (state.isSystemArmed) Icons.Default.Shield
                        else Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = NavyDark,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (state.isSirenPlaying) "SECURITY BREACH DETECTED!"
                    else if (state.isArmingCountdown) "ARMING PERIMETER IN ${state.countdownRemaining}s..."
                    else if (state.isSystemArmed) "ALL SENSORS ARMED & MONITORING"
                    else "STANDBY: SENSORS DISARMED",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (state.isSirenPlaying) "${state.activeBreachTrigger?.title ?: "Perimeter breached"} • Enter Master PIN to disarm"
                    else if (state.isSystemArmed) "Pocket, Accelerometer, Charger & USB sensors actively guarding device"
                    else "Tap button below to arm sensors with a 3s countdown delay",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                )

                if (state.isSirenPlaying) {
                    Button(
                        onClick = { viewModel.stopAlarmCompletely() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = YellowAccent,
                            contentColor = NavyDark
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_stop_alarm")
                    ) {
                        Icon(imageVector = Icons.Default.VolumeOff, contentDescription = null, tint = AlertRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("STOP ALARM", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = AlertRed)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { showPinDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("btn_disarm_pin")
                    ) {
                        Icon(imageVector = Icons.Default.LockOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("DISARM WITH MASTER PIN", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                } else if (state.isSystemArmed) {
                    Button(
                        onClick = { showPinDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = YellowAccent,
                            contentColor = NavyDark
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_disarm_pin")
                    ) {
                        Icon(imageVector = Icons.Default.LockOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("DISARM WITH MASTER PIN", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                } else {
                    Button(
                        onClick = { viewModel.armSystem() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = YellowAccent,
                            contentColor = NavyDark
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_arm_system")
                    ) {
                        Icon(imageVector = Icons.Default.Shield, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ARM ALL SENSORS NOW", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        // LIVE CAMERA PREVIEW ON ALARM (BUG 4)
        if (state.isSirenPlaying || state.isCameraPreviewActive) {
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NavyDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, AlertRed, RoundedCornerShape(16.dp))
                    .testTag("card_live_camera_alarm")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = AlertRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "LIVE CAMERA FEED & FACE SCAN",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                        if (state.faceVerificationStatus != null) {
                            Text(
                                text = if (state.faceVerificationStatus?.contains("Owner") == true) "MATCH" else "SCANNING",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (state.faceVerificationStatus?.contains("Owner") == true) SafeGreen else YellowAccent
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (isCameraPreviewOpen) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    PreviewView(ctx).also { pv ->
                                        CameraCaptureHelper.bindPreviewView(pv, lifecycleOwner)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            // Circular face target reticle
                            Box(
                                modifier = Modifier
                                    .size(130.dp)
                                    .border(
                                        2.dp,
                                        if (state.faceVerificationStatus?.contains("Owner") == true) SafeGreen else YellowAccent,
                                        CircleShape
                                    )
                            )

                            // 5s Countdown badge
                            Surface(
                                color = Color.Black.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "Auto-closing in ${cameraSecondsRemaining}s ⏱️",
                                    fontSize = 10.sp,
                                    color = YellowAccent,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NavyPrimary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "📷 Camera Auto-Closed (5s Limit)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = YellowAccent
                            )
                            Text(
                                text = "Camera sensor closed to prevent phone heating & save battery.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                            )
                            Button(
                                onClick = {
                                    isCameraPreviewOpen = true
                                    cameraSecondsRemaining = 5
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = YellowAccent, contentColor = NavyDark),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("REOPEN CAMERA (5s)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (state.faceVerificationStatus != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.faceVerificationStatus ?: "",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.faceVerificationStatus?.contains("Owner") == true) SafeGreen else YellowAccent,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                CameraCaptureHelper.captureFrameBitmap(context, lifecycleOwner) { bmp ->
                                    viewModel.verifyFaceFromBitmap(bmp)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = YellowAccent,
                                contentColor = NavyDark
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("btn_verify_face_alarm")
                        ) {
                            Icon(Icons.Default.Face, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("VERIFY FACE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.recordIntruderCapture("Live Alarm Manual Snapshot")
                                viewModel.showMessage("Intruder photo logged to Vault!")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AlertRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("btn_capture_silent_photo")
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("CAPTURE PHOTO", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // LATEST EVIDENCE PHOTO CARD
        if (state.lastCapturedPhotoPath != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_latest_evidence")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NavyDark)
                    ) {
                        AsyncImage(
                            model = File(state.lastCapturedPhotoPath ?: ""),
                            contentDescription = "Last captured intruder photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Latest Intruder Evidence",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = state.lastCapturedTimestamp ?: "Just now",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Logged silently in Vault with timestamp",
                            fontSize = 11.sp,
                            color = AlertRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // PIN DIALOG / DISARM OVERLAY
        AnimatedVisibility(visible = showPinDialog) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .border(2.dp, AlertRed, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Enter Master PIN to Disarm",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                    Text(
                        text = "Default PIN: ${state.masterPin}. On 3rd wrong PIN, silent front camera photo is automatically captured and saved.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 6) pinInput = it },
                        placeholder = { Text("4-6 Digits") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        isError = state.pinError,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NavyPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_disarm_pin")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                showPinDialog = false
                                pinInput = ""
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                val success = viewModel.verifyAndDisarmPin(pinInput)
                                if (success) {
                                    showPinDialog = false
                                    pinInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_confirm_disarm_pin")
                        ) {
                            Text("Verify & Disarm", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // LIVE SENSOR TELEMETRY METRICS
        Text(
            text = "Real-Time Sensor Telemetry",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        // SMART CONTEXT & ACTIVITY RECOGNITION CARD
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = NavyDark),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = YellowAccent.copy(alpha = 0.2f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🎯", fontSize = 16.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Smart Context & Activity",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                            Text(
                                text = "False-Alarm Filter (Zero false alerts while holding or walking)",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (tele.deviceContext.allowsAlarm) AlertRed.copy(alpha = 0.25f) else SafeGreen.copy(alpha = 0.25f)
                    ) {
                        Text(
                            text = if (tele.deviceContext.allowsAlarm) "ALARM READY" else "SAFE (NO ALARM)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (tele.deviceContext.allowsAlarm) YellowAccent else SafeGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("DEVICE CONTEXT", fontSize = 9.sp, color = YellowAccent, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tele.deviceContext.displayName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("USER ACTIVITY", fontSize = 9.sp, color = YellowAccent, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tele.movementActivity.displayName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                if (tele.isInGracePeriod) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = YellowAccent.copy(alpha = 0.2f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🛡️ Grace Period Active: Initial movement safely ignored for 5s.",
                            fontSize = 11.sp,
                            color = YellowAccent,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SensorMetricCard(
                title = "Proximity Sensor",
                value = if (tele.isProximityNear) "COVERED (Pocket)" else "${tele.proximityCm} cm",
                status = if (tele.isProximityNear) "In Pocket / Bag" else "Exposed to light",
                icon = Icons.Default.Sensors,
                color = if (tele.isProximityNear) SafeGreen else YellowAccent,
                modifier = Modifier.weight(1f)
            )

            SensorMetricCard(
                title = "Ambient Light",
                value = "${tele.lightLux.toInt()} Lux",
                status = if (tele.lightLux < 15f) "Darkness (Pocket)" else "Ambient Light",
                icon = Icons.Default.Visibility,
                color = if (tele.lightLux < 15f) NavyPrimary else Color(0xFFFFA000),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SensorMetricCard(
                title = "G-Force / Accel",
                value = String.format("%.2f m/s²", tele.totalAcceleration),
                status = if (Math.abs(tele.totalAcceleration - 9.8f) > 1.5f) "MOTION DETECTED" else "Stationary",
                icon = Icons.Default.Speed,
                color = if (Math.abs(tele.totalAcceleration - 9.8f) > 1.5f) AlertRed else SafeGreen,
                modifier = Modifier.weight(1f)
            )

            SensorMetricCard(
                title = "Power & USB",
                value = if (tele.isPowerConnected) "Plugged In" else "Battery (${tele.batteryPct}%)",
                status = if (tele.isUsbConnected) "USB Active" else "No USB Cable",
                icon = if (tele.isPowerConnected) Icons.Default.Power else Icons.Default.Usb,
                color = if (tele.isPowerConnected) SafeGreen else Color.Gray,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // AI FEATURE 1: AUTO-SLEEP DETECTION & LEARNING
        AiAutoSleepCard(
            sleepState = state.autoSleepState,
            isSystemArmed = state.isSystemArmed,
            onToggleEnable = { viewModel.toggleAutoSleepDetection() },
            onSetThreshold = { viewModel.setSleepInactivityThreshold(it) },
            onFastForward = { viewModel.simulateSleepFastForward() },
            onSimulateTouch = { isOwner -> viewModel.simulateSleepTouch(isOwner) },
            onResetLearning = { viewModel.resetSleepLearning() }
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 6 CORE FEATURES QUICK TOGGLES & TEST BUTTONS
        Text(
            text = "Security Trigger Rules & Test Simulator",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        TriggerActionRow(
            title = "1. Pocket Detection Alarm",
            description = "Proximity + Light sensors detect phone taken out",
            icon = Icons.Default.Sensors,
            enabled = state.guardConfig.pocketDetectionEnabled,
            onToggle = { enabled ->
                viewModel.updateConfig { it.copy(pocketDetectionEnabled = enabled) }
            },
            onSimulate = { viewModel.triggerManualBreach(TriggerReason.POCKET_REMOVAL) },
            testTag = "toggle_pocket_detection"
        )

        Spacer(modifier = Modifier.height(8.dp))

        TriggerActionRow(
            title = "2. Motion Detection Alarm",
            description = "Accelerometer detects unauthorized device movement",
            icon = Icons.Default.Speed,
            enabled = state.guardConfig.motionDetectionEnabled,
            onToggle = { enabled ->
                viewModel.updateConfig { it.copy(motionDetectionEnabled = enabled) }
            },
            onSimulate = { viewModel.triggerManualBreach(TriggerReason.MOTION_DETECTED) },
            testTag = "toggle_motion_detection"
        )

        Spacer(modifier = Modifier.height(8.dp))

        TriggerActionRow(
            title = "3. Charger Unplug Alarm",
            description = "Triggers immediately if AC/cable disconnected",
            icon = Icons.Default.Power,
            enabled = state.guardConfig.chargerUnplugEnabled,
            onToggle = { enabled ->
                viewModel.updateConfig { it.copy(chargerUnplugEnabled = enabled) }
            },
            onSimulate = { viewModel.triggerManualBreach(TriggerReason.CHARGER_UNPLUGGED) },
            testTag = "toggle_charger_unplug"
        )

        Spacer(modifier = Modifier.height(8.dp))

        TriggerActionRow(
            title = "4. USB Connection Alert",
            description = "Detects unauthorized computer/data cable attachment",
            icon = Icons.Default.Usb,
            enabled = state.guardConfig.usbConnectionEnabled,
            onToggle = { enabled ->
                viewModel.updateConfig { it.copy(usbConnectionEnabled = enabled) }
            },
            onSimulate = { viewModel.triggerManualBreach(TriggerReason.USB_CONNECTED) },
            testTag = "toggle_usb_alert"
        )

        Spacer(modifier = Modifier.height(8.dp))

        TriggerActionRow(
            title = "5. Live GPS Tracking",
            description = "High accuracy satellite radar & coordinate lock",
            icon = Icons.Default.GpsFixed,
            enabled = state.guardConfig.liveGpsTrackingEnabled,
            onToggle = { enabled ->
                viewModel.updateConfig { it.copy(liveGpsTrackingEnabled = enabled) }
            },
            onSimulate = { viewModel.navigateTo(Screen.LIVE_TRACKING) },
            simulateLabel = "View Radar",
            testTag = "toggle_gps_tracking"
        )

        Spacer(modifier = Modifier.height(8.dp))

        TriggerActionRow(
            title = "6. Intruder Selfie (In-App)",
            description = "Captures photo & logs timestamp upon wrong PIN",
            icon = Icons.Default.CameraAlt,
            enabled = state.guardConfig.intruderSelfieEnabled,
            onToggle = { enabled ->
                viewModel.updateConfig { it.copy(intruderSelfieEnabled = enabled) }
            },
            onSimulate = { viewModel.navigateTo(Screen.INTRUDER_SELFIE) },
            simulateLabel = "View Log",
            testTag = "toggle_intruder_selfie"
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SensorMetricCard(
    title: String,
    value: String,
    status: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.border(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            RoundedCornerShape(16.dp)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = status,
                fontSize = 10.sp,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun TriggerActionRow(
    title: String,
    description: String,
    icon: ImageVector,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onSimulate: () -> Unit,
    simulateLabel: String = "Test Trigger",
    testTag: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                RoundedCornerShape(16.dp)
            )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(NavyPrimary.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = NavyPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = description,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = YellowAccent,
                        checkedTrackColor = NavyPrimary
                    ),
                    modifier = Modifier.testTag(testTag)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onSimulate,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NavyPrimary.copy(alpha = 0.1f),
                    contentColor = NavyPrimary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
            ) {
                Text(
                    text = simulateLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
