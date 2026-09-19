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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // TOP SECURITY STATUS CARD (DYNAMICS FOR SENSORS & SIREN)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isRinging) AlertRed else NavyPrimary
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .then(
                    if (isRinging) Modifier.border(2.dp, pulseBorderColor, RoundedCornerShape(20.dp))
                    else Modifier
                )
                .testTag("home_status_card")
        ) {
            Column(
                modifier = Modifier
                    .background(
                        brush = Brush.horizontalGradient(
                            if (isRinging) listOf(AlertRed, Color(0xFF7B0000))
                            else listOf(NavyDark, NavyPrimary)
                        )
                    )
                    .padding(16.dp)
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
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isRinging) YellowAccent else if (state.isSystemArmed) SafeGreen else YellowAccent)
                        ) {
                            Icon(
                                imageVector = if (isRinging) Icons.Default.Warning else Icons.Default.Shield,
                                contentDescription = "Shield Active",
                                tint = NavyDark,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = if (isRinging) "EMERGENCY ALARM ACTIVE"
                                else if (state.isSystemArmed) "Device Sensors Armed"
                                else viewModel.tr("device_protected"),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isRinging) YellowAccent else SafeGreen)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isRinging) "${state.activeBreachTrigger?.title ?: "Breach"} Alert"
                                    else if (state.isSystemArmed) "All 6 Sensors Active"
                                    else viewModel.tr("shield_active"),
                                    fontSize = 12.sp,
                                    color = YellowAccent
                                )
                            }
                        }
                    }

                    // QUICK LINK TO SENSORS HUB
                    Surface(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.clickable { viewModel.navigateTo(Screen.SENSORS_HUB) }
                    ) {
                        Text(
                            text = if (state.isSystemArmed) "ARMED" else "ARM HUB",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = YellowAccent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // SIREN TRIGGER BUTTON
                Button(
                    onClick = {
                        if (isRinging) {
                            viewModel.navigateTo(Screen.SENSORS_HUB)
                        } else {
                            viewModel.triggerGlobalSiren()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRinging) YellowAccent else YellowAccent,
                        contentColor = NavyDark
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("siren_trigger_button")
                ) {
                    Icon(
                        imageVector = if (isRinging) Icons.Default.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRinging) "DISARM WITH PIN / STOP SIREN" else viewModel.tr("quick_alert"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // HERO CARD: FAMILY SECURITY NETWORK
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NavyCardDark),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (state.familyNetwork.isFailoverActive) AlertRed else YellowAccent
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.navigateTo(Screen.FAMILY_NETWORK) }
                .testTag("home_family_network_hero")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (state.familyNetwork.isFailoverActive) AlertRed else YellowAccent)
                    ) {
                        Icon(
                            imageVector = if (state.familyNetwork.isFailoverActive) Icons.Default.Warning else Icons.Default.Hub,
                            contentDescription = null,
                            tint = NavyDark,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Family Security Network",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = if (state.familyNetwork.isFailoverActive) AlertRed else SafeGreen,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (state.familyNetwork.isFailoverActive) "FAILOVER" else "5 PHONES",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (state.familyNetwork.isFailoverActive)
                                "🚨 Master stolen! Guardian failover controlling network"
                            else
                                "Synced via ${state.familyNetwork.accountEmail}",
                            fontSize = 11.sp,
                            color = if (state.familyNetwork.isFailoverActive) AlertRed else YellowAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "OPEN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = YellowAccent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = viewModel.tr("home_title"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )

        // NAVIGATION GRID INCLUDING SENSORS HUB & INTRUDER SELFIE
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 8.dp)
        ) {
            item {
                HomeMenuCard(
                    title = "Family Network",
                    description = "5 Phones • Failover Guard",
                    icon = Icons.Default.Hub,
                    badgeText = "5 SYNC",
                    badgeColor = YellowDark,
                    testTag = "btn_family_network_grid",
                    onClick = { viewModel.navigateTo(Screen.FAMILY_NETWORK) }
                )
            }
            item {
                HomeMenuCard(
                    title = "Sensors & Alarms",
                    description = if (state.isSystemArmed) "Armed (6 Active)" else "Pocket, Accel, USB",
                    icon = Icons.Default.Sensors,
                    badgeText = if (state.isSystemArmed) "ARMED" else "READY",
                    badgeColor = if (state.isSystemArmed) SafeGreen else AlertRed,
                    testTag = "btn_sensors_hub",
                    onClick = { viewModel.navigateTo(Screen.SENSORS_HUB) }
                )
            }

            item {
                HomeMenuCard(
                    title = "Intruder Selfie",
                    description = "${state.intruderCaptures.size} Breach Logs",
                    icon = Icons.Default.CameraAlt,
                    badgeText = "CAMERA",
                    badgeColor = NavyPrimary,
                    testTag = "btn_intruder_selfie",
                    onClick = { viewModel.navigateTo(Screen.INTRUDER_SELFIE) }
                )
            }

            item {
                HomeMenuCard(
                    title = viewModel.tr("live_tracking"),
                    description = viewModel.tr("live_tracking_desc"),
                    icon = Icons.Default.GpsFixed,
                    badgeText = "GPS",
                    badgeColor = NavyDark,
                    testTag = "btn_live_tracking",
                    onClick = { viewModel.navigateTo(Screen.LIVE_TRACKING) }
                )
            }

            item {
                HomeMenuCard(
                    title = viewModel.tr("my_mobiles"),
                    description = "${state.myDevices.size} " + viewModel.tr("status_secured"),
                    icon = Icons.Default.PhoneAndroid,
                    badgeText = "${state.myDevices.size}",
                    badgeColor = SafeGreen,
                    testTag = "btn_my_mobiles",
                    onClick = { viewModel.navigateTo(Screen.MY_MOBILES) }
                )
            }

            item {
                HomeMenuCard(
                    title = viewModel.tr("stolen_devices"),
                    description = viewModel.tr("stolen_devices_desc"),
                    icon = Icons.Default.Security,
                    badgeText = "IMEI",
                    badgeColor = YellowDark,
                    testTag = "btn_stolen_devices",
                    onClick = { viewModel.navigateTo(Screen.STOLEN_DEVICES) }
                )
            }

            item {
                HomeMenuCard(
                    title = viewModel.tr("community"),
                    description = viewModel.tr("community_desc"),
                    icon = Icons.Default.Groups,
                    badgeText = "ALERTS",
                    badgeColor = Color(0xFF00897B),
                    testTag = "btn_community",
                    onClick = { viewModel.navigateTo(Screen.COMMUNITY) }
                )
            }

            item {
                HomeMenuCard(
                    title = viewModel.tr("report_theft"),
                    description = viewModel.tr("report_theft_desc"),
                    icon = Icons.Default.ReportProblem,
                    badgeText = "FIR",
                    badgeColor = AlertRed,
                    testTag = "btn_report_theft",
                    onClick = { viewModel.navigateTo(Screen.REPORT_THEFT) }
                )
            }

            item {
                HomeMenuCard(
                    title = viewModel.tr("settings"),
                    description = "Sensors & Languages",
                    icon = Icons.Default.Settings,
                    badgeText = state.selectedLanguage.code.uppercase(),
                    badgeColor = NavyDark,
                    testTag = "btn_settings",
                    onClick = { viewModel.navigateTo(Screen.SETTINGS) }
                )
            }
        }
    }
}

@Composable
fun HomeMenuCard(
    title: String,
    description: String,
    icon: ImageVector,
    badgeText: String,
    badgeColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(138.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(NavyPrimary.copy(alpha = 0.1f))
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = NavyPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Surface(
                    color = badgeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp
                )
            }
        }
    }
}
