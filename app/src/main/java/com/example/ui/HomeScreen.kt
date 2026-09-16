package com.example.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Security
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
    val isRinging = state.liveTrackerState.isSirenRinging

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // TOP SECURITY STATUS CARD
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = NavyPrimary
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("home_status_card")
        ) {
            Column(
                modifier = Modifier
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(NavyDark, NavyPrimary)
                        )
                    )
                    .padding(18.dp)
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
                                .background(YellowAccent)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Shield Active",
                                tint = NavyDark,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = viewModel.tr("device_protected"),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(SafeGreen)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = viewModel.tr("shield_active"),
                                    fontSize = 12.sp,
                                    color = YellowAccent
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // SIREN TRIGGER BUTTON
                Button(
                    onClick = { viewModel.triggerGlobalSiren() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRinging) AlertRed else YellowAccent,
                        contentColor = if (isRinging) Color.White else NavyDark
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("siren_trigger_button")
                ) {
                    Icon(
                        imageVector = if (isRinging) Icons.Default.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRinging) viewModel.tr("quick_stop") else viewModel.tr("quick_alert"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = viewModel.tr("home_title"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        )

        // 6 MAIN BUTTONS GRID
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 12.dp)
        ) {
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
                    title = viewModel.tr("live_tracking"),
                    description = viewModel.tr("live_tracking_desc"),
                    icon = Icons.Default.GpsFixed,
                    badgeText = "RADAR",
                    badgeColor = NavyPrimary,
                    testTag = "btn_live_tracking",
                    onClick = { viewModel.navigateTo(Screen.LIVE_TRACKING) }
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
                    description = state.selectedLanguage.nativeName,
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
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp)
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
                .padding(14.dp),
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
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NavyPrimary.copy(alpha = 0.1f))
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = NavyPrimary,
                        modifier = Modifier.size(24.dp)
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
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
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
