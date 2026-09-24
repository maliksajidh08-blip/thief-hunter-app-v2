package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

/**
 * Quick Activation Card & Widget Component
 * Provides 1-tap rapid arming, disarming, charger guard activation,
 * and immediate siren intervention.
 */
@Composable
fun QuickActionsWidget(
    viewModel: ThiefHunterViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val isArmed = state.isSystemArmed
    val isRinging = state.isSirenPlaying
    val isChargerGuardActive = state.isChargerGuardActive

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_alert")
    val alertGlow by infiniteTransition.animateColor(
        initialValue = if (isRinging) AlertRed else if (isArmed) SafeGreen else YellowAccent,
        targetValue = if (isRinging) YellowAccent else if (isArmed) YellowAccent else SafeGreen,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alertGlow"
    )

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCardDark),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isRinging) AlertRed else if (isArmed) SafeGreen.copy(alpha = 0.8f) else YellowAccent.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("quick_actions_widget_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Title & Real-time Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(NavyPrimary, Color(0xFF283593))
                                )
                            )
                            .border(1.dp, YellowAccent.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Text(text = "🎯", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Quick Activation Guard",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Instant 1-Tap Anti-Theft Actions",
                            fontSize = 10.sp,
                            color = YellowAccent.copy(alpha = 0.9f)
                        )
                    }
                }

                // Current Status Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        isRinging -> AlertRed.copy(alpha = 0.25f)
                        isArmed -> SafeGreen.copy(alpha = 0.22f)
                        else -> Color.White.copy(alpha = 0.1f)
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        when {
                            isRinging -> AlertRed
                            isArmed -> SafeGreen
                            else -> Color.White.copy(alpha = 0.3f)
                        }
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isRinging -> AlertRed
                                        isArmed -> SafeGreen
                                        else -> YellowAccent
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                isRinging -> "ALARM ACTIVE"
                                isArmed -> "ARMED & SAFE"
                                else -> "STANDBY"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = when {
                                isRinging -> AlertRed
                                isArmed -> SafeGreen
                                else -> YellowAccent
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main 2 Big Action Buttons: ONE-TAP ARM ALL vs STOP ALARM
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // BUTTON 1: ARM ALL SENSORS
                Button(
                    onClick = {
                        if (!isArmed) {
                            viewModel.toggleSystemArm()
                        }
                    },
                    enabled = !isArmed,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isArmed) Color(0xFF2C392F) else SafeGreen,
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF1E3223),
                        disabledContentColor = SafeGreen.copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_quick_arm_all")
                ) {
                    Icon(
                        imageVector = if (isArmed) Icons.Default.CheckCircle else Icons.Default.Shield,
                        contentDescription = "Arm All",
                        tint = if (isArmed) SafeGreen else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isArmed) "ALL ARMED" else "ARM ALL",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // BUTTON 2: STOP ALARM / DISARM
                Button(
                    onClick = {
                        if (isRinging) {
                            viewModel.stopSiren()
                        } else if (isArmed) {
                            viewModel.toggleSystemArm()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRinging) AlertRed else Color(0xFF2A3464),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_quick_stop_alarm")
                ) {
                    Icon(
                        imageVector = if (isRinging) Icons.Default.VolumeUp else Icons.Default.StopCircle,
                        contentDescription = "Stop Alarm",
                        tint = if (isRinging) Color.White else YellowAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isRinging) "STOP ALARM!" else "DISARM",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isRinging) Color.White else YellowAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Secondary Quick Actions: Charger Guard toggle & Panic Sound
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // CHARGER GUARD TOGGLE
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isChargerGuardActive) YellowAccent.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.07f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isChargerGuardActive) YellowAccent else Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.toggleChargerGuard() }
                        .testTag("quick_toggle_charger_guard")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isChargerGuardActive) Icons.Default.Power else Icons.Default.PowerOff,
                            contentDescription = "Charger Guard",
                            tint = if (isChargerGuardActive) YellowAccent else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Charger Guard",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isChargerGuardActive) YellowAccent else Color.White
                            )
                            Text(
                                text = if (isChargerGuardActive) "Active (Unplug Alert)" else "Tap to Guard",
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // PANIC SIREN TRIGGER
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isRinging) AlertRed else Color.White.copy(alpha = 0.07f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isRinging) Color.White else AlertRed.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (isRinging) viewModel.stopSiren() else viewModel.startSiren("PANIC_TRIGGER")
                        }
                        .testTag("quick_trigger_panic")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Panic Siren",
                            tint = if (isRinging) Color.White else AlertRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = if (isRinging) "Siren Ringing!" else "Panic Siren",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isRinging) Color.White else AlertRed
                            )
                            Text(
                                text = if (isRinging) "Tap to Mute" else "Max Decibel",
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}
