package com.example.ui

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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Screen
import com.example.ui.theme.AlertRed
import com.example.ui.theme.NavyCardDark
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyLight
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.YellowAccent
import com.example.ui.theme.YellowDark

@Composable
fun HomeScreen(
    viewModel: ThiefHunterViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val isRinging = state.isSirenPlaying

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseBorderColor by infiniteTransition.animateColor(
        initialValue = AlertRed,
        targetValue = YellowAccent,
        animationSpec = infiniteRepeatable(
            animation = tween(450),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseBorderColor"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        // 1. FAMILY NETWORK CARD - EXACTLY 80DP HEIGHT, COMPACT
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = NavyCardDark),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (state.familyNetwork.isFailoverActive) AlertRed else YellowAccent.copy(alpha = 0.6f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clickable { viewModel.navigateTo(Screen.FAMILY_NETWORK) }
                .testTag("home_family_network_hero")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (state.familyNetwork.isFailoverActive) AlertRed else YellowAccent)
                    ) {
                        Icon(
                            imageVector = if (state.familyNetwork.isFailoverActive) Icons.Default.Warning else Icons.Default.Hub,
                            contentDescription = null,
                            tint = NavyDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "Family Network",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Synced: ${state.familyNetwork.accountEmail}",
                            fontSize = 11.sp,
                            color = YellowAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    color = YellowAccent,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .clickable { viewModel.navigateTo(Screen.FAMILY_NETWORK) }
                        .testTag("btn_open_family_network")
                ) {
                    Text(
                        text = "OPEN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NavyDark,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 2. DEVICE PROTECTED CARD - EXACTLY 100DP HEIGHT, COMPACT
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = NavyDark),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .then(
                    if (isRinging) Modifier.border(2.dp, pulseBorderColor, RoundedCornerShape(14.dp))
                    else Modifier.border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                )
                .testTag("home_status_card")
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (isRinging) AlertRed else SafeGreen)
                        ) {
                            Icon(
                                imageVector = if (isRinging) Icons.Default.Warning else Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Text(
                                text = if (isRinging) "EMERGENCY ALARM ACTIVE" else "Device is Protected",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (isRinging) "Security breach detected!" else "All 6 Sensors Active • Guard On",
                                fontSize = 10.sp,
                                color = if (isRinging) AlertRed else SafeGreen
                            )
                        }
                    }

                    // Small Sensors Hub shortcut chip
                    Surface(
                        color = Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.clickable { viewModel.navigateTo(Screen.SENSORS_HUB) }
                    ) {
                        Text(
                            text = "SENSORS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = YellowAccent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                // SIREN BUTTON (SOUND SIREN / STOP SIREN)
                Button(
                    onClick = {
                        if (isRinging) viewModel.stopGlobalSiren()
                        else viewModel.triggerGlobalSiren()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRinging) AlertRed else YellowAccent,
                        contentColor = if (isRinging) Color.White else NavyDark
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .testTag("siren_trigger_button")
                ) {
                    Icon(
                        imageVector = if (isRinging) Icons.Default.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isRinging) "STOP SIREN (DISARM)" else "SOUND SIREN",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 3. SECTION HEADER
        Text(
            text = "Security Command Center",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(vertical = 2.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 4. ALL 6 CARDS SHOWN ON ONE SCREEN (COMPACT 2-COLUMN ROWS)
        // ROW 1: My Mobiles & Stolen Devices
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompactMenuCard(
                title = "My Mobiles",
                subtitle = "${state.myDevices.size} Secured",
                icon = Icons.Default.PhoneAndroid,
                badge = "${state.myDevices.size}",
                badgeColor = SafeGreen,
                testTag = "btn_my_mobiles",
                modifier = Modifier.weight(1f),
                onClick = { viewModel.navigateTo(Screen.MY_MOBILES) }
            )
            CompactMenuCard(
                title = "Stolen Devices",
                subtitle = "IMEI Database",
                icon = Icons.Default.Security,
                badge = "IMEI",
                badgeColor = YellowDark,
                testTag = "btn_stolen_devices",
                modifier = Modifier.weight(1f),
                onClick = { viewModel.navigateTo(Screen.STOLEN_DEVICES) }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // ROW 2: Live Tracking & Community
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompactMenuCard(
                title = "Live Tracking",
                subtitle = "GPS Satellite",
                icon = Icons.Default.GpsFixed,
                badge = "GPS",
                badgeColor = NavyDark,
                testTag = "btn_live_tracking",
                modifier = Modifier.weight(1f),
                onClick = { viewModel.navigateTo(Screen.LIVE_TRACKING) }
            )
            CompactMenuCard(
                title = "Community",
                subtitle = "${state.communityAlerts.size} Live Alerts",
                icon = Icons.Default.Groups,
                badge = "ALERTS",
                badgeColor = Color(0xFF00897B),
                testTag = "btn_community",
                modifier = Modifier.weight(1f),
                onClick = { viewModel.navigateTo(Screen.COMMUNITY) }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // ROW 3: Report Theft & Settings
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompactMenuCard(
                title = "Report Theft",
                subtitle = "FIR Dispatch",
                icon = Icons.Default.ReportProblem,
                badge = "FIR",
                badgeColor = AlertRed,
                testTag = "btn_report_theft",
                modifier = Modifier.weight(1f),
                onClick = { viewModel.navigateTo(Screen.REPORT_THEFT) }
            )
            CompactMenuCard(
                title = "Settings",
                subtitle = "Sensors & Account",
                icon = Icons.Default.Settings,
                badge = "CONFIG",
                badgeColor = NavyDark,
                testTag = "btn_settings",
                modifier = Modifier.weight(1f),
                onClick = { viewModel.navigateTo(Screen.SETTINGS) }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // QUICK ACCESS SHORTCUTS FOR SENSORS & INTRUDER SELFIE (COMPACT BAR)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = NavyCardDark,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clickable { viewModel.navigateTo(Screen.SENSORS_HUB) }
                    .testTag("btn_sensors_hub_quick")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = null,
                        tint = YellowAccent,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Sensors Hub (6)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            Surface(
                color = NavyCardDark,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clickable { viewModel.navigateTo(Screen.INTRUDER_SELFIE) }
                    .testTag("btn_intruder_selfie_quick")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = SafeGreen,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Intruder Vault (${state.intruderCaptures.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactMenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badge: String,
    badgeColor: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCardDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .height(68.dp)
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(NavyPrimary)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = YellowAccent,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                color = badgeColor.copy(alpha = 0.25f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = badge,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = badgeColor,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}
