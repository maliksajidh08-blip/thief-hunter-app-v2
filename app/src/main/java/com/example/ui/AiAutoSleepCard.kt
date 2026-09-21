package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.AutoSleepState
import com.example.ui.theme.AlertRed
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.YellowAccent

@Composable
fun AiAutoSleepCard(
    sleepState: AutoSleepState,
    isSystemArmed: Boolean,
    onToggleEnable: () -> Unit,
    onSetThreshold: (Int) -> Unit,
    onFastForward: () -> Unit,
    onSimulateTouch: (isOwner: Boolean) -> Unit,
    onResetLearning: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg = if (sleepState.isSleepArmed) {
        NavyDark
    } else {
        MaterialTheme.colorScheme.surface
    }

    val badgeColor by animateColorAsState(
        targetValue = when {
            sleepState.isSleepArmed -> SafeGreen
            sleepState.inactivitySeconds > 0 -> YellowAccent
            else -> Color.Gray
        },
        label = "sleepBadge"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = if (sleepState.isSleepArmed) 2.dp else 1.dp,
                color = if (sleepState.isSleepArmed) SafeGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(20.dp)
            )
            .testTag("card_ai_auto_sleep")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // HEADER & TOGGLE
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(NavyPrimary, Color(0xFF1E3A8A))
                                )
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = YellowAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "AI AUTO-SLEEP DETECTION",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (sleepState.isSleepArmed) Color.White else NavyDark,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Auto-arms after 30s stillness • Touch alert",
                            fontSize = 11.sp,
                            color = if (sleepState.isSleepArmed) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = sleepState.isEnabled,
                    onCheckedChange = { onToggleEnable() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = YellowAccent,
                        checkedTrackColor = NavyPrimary
                    ),
                    modifier = Modifier.testTag("switch_auto_sleep")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // AI STATUS BADGE & CONFIDENCE
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (sleepState.isSleepArmed) Color(0xFF0F172A) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(badgeColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                !sleepState.isEnabled -> "AI SLEEP GUARD: DISABLED"
                                sleepState.isSleepArmed -> "AI: OWNER SLEEPING (ARMED)"
                                sleepState.inactivitySeconds > 0 -> "INACTIVITY: ${sleepState.inactivitySeconds}s / ${sleepState.sleepThresholdSeconds}s"
                                else -> "AI MONITORING: STILLNESS STANDBY"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (sleepState.isSleepArmed) SafeGreen else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Night Schedule Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (sleepState.isNightTime) Color(0xFF312E81) else Color(0xFFF3F4F6)
                    ) {
                        Text(
                            text = if (sleepState.isNightTime) "🌙 NIGHT (10PM-6AM)" else "☀️ DAY TIME",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (sleepState.isNightTime) Color(0xFFC7D2FE) else Color.DarkGray,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // INACTIVITY PROGRESS BAR (0s to 30s)
            val progressFraction = (sleepState.inactivitySeconds.toFloat() / sleepState.sleepThresholdSeconds.toFloat())
                .coerceIn(0f, 1f)

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Inactivity Timer (${sleepState.inactivitySeconds}s of ${sleepState.sleepThresholdSeconds}s)",
                        fontSize = 11.sp,
                        color = if (sleepState.isSleepArmed) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${sleepState.sleepConfidencePct}% AI Confidence",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (sleepState.isSleepArmed) SafeGreen else NavyPrimary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                LinearProgressIndicator(
                    progress = { if (sleepState.isSleepArmed) 1f else progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (sleepState.isSleepArmed) SafeGreen else YellowAccent,
                    trackColor = if (sleepState.isSleepArmed) Color.DarkGray else Color.LightGray.copy(alpha = 0.4f),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // LEARNED SLEEP PATTERNS INFO GRID
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (sleepState.isSleepArmed) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Learned Sleep Pattern Profile",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (sleepState.isSleepArmed) YellowAccent else NavyPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = "Learned Bedtime",
                                fontSize = 10.sp,
                                color = if (sleepState.isSleepArmed) Color.White.copy(alpha = 0.6f) else Color.Gray
                            )
                            Text(
                                text = sleepState.learnedBedtime,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (sleepState.isSleepArmed) Color.White else NavyDark
                            )
                        }

                        Column {
                            Text(
                                text = "Learned Wake-Up",
                                fontSize = 10.sp,
                                color = if (sleepState.isSleepArmed) Color.White.copy(alpha = 0.6f) else Color.Gray
                            )
                            Text(
                                text = sleepState.learnedWakeTime,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (sleepState.isSleepArmed) Color.White else NavyDark
                            )
                        }

                        Column {
                            Text(
                                text = "Learned Sessions",
                                fontSize = 10.sp,
                                color = if (sleepState.isSleepArmed) Color.White.copy(alpha = 0.6f) else Color.Gray
                            )
                            Text(
                                text = "${sleepState.sleepSessionsLearnedCount} sessions",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (sleepState.isSleepArmed) Color.White else NavyDark
                            )
                        }

                        Column {
                            Text(
                                text = "Touch Sensitivity",
                                fontSize = 10.sp,
                                color = if (sleepState.isSleepArmed) Color.White.copy(alpha = 0.6f) else Color.Gray
                            )
                            Text(
                                text = "Gentle (${sleepState.touchSensitivityScore}g)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (sleepState.isSleepArmed) Color.White else NavyDark
                            )
                        }
                    }
                }
            }

            // LAST EVENT MESSAGE / AI REASONING
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (sleepState.isSleepArmed) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🤖 ${sleepState.lastSleepEventMessage}",
                    fontSize = 11.sp,
                    color = if (sleepState.isSleepArmed) Color(0xFF93C5FD) else Color(0xFF1E3A8A),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }

            if (sleepState.lastVerificationResult != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = sleepState.lastVerificationResult,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (sleepState.lastVerificationResult.contains("OWNER")) SafeGreen else AlertRed
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // INTERACTIVE TEST BUTTONS (FOR USER TO TEST ALL 4 SPECIFIED CONDITIONS)
            Text(
                text = "Interactive Test & Simulation Controls",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (sleepState.isSleepArmed) YellowAccent else NavyPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onFastForward,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (sleepState.isSleepArmed) YellowAccent else NavyPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_fast_forward_sleep")
                ) {
                    Icon(imageVector = Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Fast-Forward 30s", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onResetLearning,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (sleepState.isSleepArmed) Color.LightGray else Color.DarkGray
                    ),
                    modifier = Modifier
                        .weight(0.7f)
                        .testTag("btn_reset_sleep")
                ) {
                    Text("Reset AI", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // SIMULATE TOUCH DURING SLEEP: OWNER VS STRANGER
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { onSimulateTouch(true) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SafeGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_simulate_owner_touch")
                ) {
                    Icon(imageVector = Icons.Default.Face, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Touch: Owner Wakes Up\n(Face Check → No Alarm)", fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }

                Button(
                    onClick = { onSimulateTouch(false) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AlertRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_simulate_stranger_touch")
                ) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Touch: Stranger / Thief\n(Immediate ALARM + SMS)", fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
