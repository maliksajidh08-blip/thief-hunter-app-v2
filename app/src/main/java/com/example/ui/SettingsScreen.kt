package com.example.ui

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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppLanguage
import com.example.ui.theme.AlertRed
import com.example.ui.theme.NavyCardDark
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.YellowAccent

@Composable
fun SettingsScreen(
    viewModel: ThiefHunterViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val config = state.guardConfig

    var pinText by remember(state.masterPin) { mutableStateOf(state.masterPin) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = viewModel.tr("settings"),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Text(
            text = "Configure security triggers, sensor thresholds, background service and master credentials.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // 1. ALL 6 SECURITY FEATURES MASTER CONFIGURATION
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    RoundedCornerShape(18.dp)
                )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Sensor Security Triggers (On/Off)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Feature 1: Pocket Detection
                SettingToggleRow(
                    title = "1. Pocket Detection Alarm",
                    subtitle = "Proximity + Light sensors detect when pulled from pocket or handbag",
                    checked = config.pocketDetectionEnabled,
                    onCheckedChange = { viewModel.updateConfig { c -> c.copy(pocketDetectionEnabled = it) } },
                    testTag = "settings_switch_pocket"
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Feature 2: Motion Detection
                SettingToggleRow(
                    title = "2. Motion Detection Alarm",
                    subtitle = "Accelerometer detects unauthorized movement from table or surface",
                    checked = config.motionDetectionEnabled,
                    onCheckedChange = { viewModel.updateConfig { c -> c.copy(motionDetectionEnabled = it) } },
                    testTag = "settings_switch_motion"
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Feature 3: Charger Unplug
                SettingToggleRow(
                    title = "3. Charger Disconnect Alarm",
                    subtitle = "Fires sirens immediately if charging cable is unhooked",
                    checked = config.chargerUnplugEnabled,
                    onCheckedChange = { viewModel.updateConfig { c -> c.copy(chargerUnplugEnabled = it) } },
                    testTag = "settings_switch_charger"
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Feature 4: USB Connection Alert
                SettingToggleRow(
                    title = "4. USB Cable Attachment Alert",
                    subtitle = "Detects unauthorized computer/data theft USB interface",
                    checked = config.usbConnectionEnabled,
                    onCheckedChange = { viewModel.updateConfig { c -> c.copy(usbConnectionEnabled = it) } },
                    testTag = "settings_switch_usb"
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Feature 5: Live GPS Tracking
                SettingToggleRow(
                    title = "5. Live GPS Satellite Tracking",
                    subtitle = "Continuous coordinates updates and radar beacon",
                    checked = config.liveGpsTrackingEnabled,
                    onCheckedChange = { viewModel.updateConfig { c -> c.copy(liveGpsTrackingEnabled = it) } },
                    testTag = "settings_switch_gps"
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Feature 6: Intruder Selfie
                SettingToggleRow(
                    title = "6. Intruder Selfie Capture",
                    subtitle = "Silent photo snapshot & log saved upon failed PIN entry",
                    checked = config.intruderSelfieEnabled,
                    onCheckedChange = { viewModel.updateConfig { c -> c.copy(intruderSelfieEnabled = it) } },
                    testTag = "settings_switch_selfie"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // FEATURE 7: MOTION SENSOR SENSITIVITY (Better)
                Text(
                    text = "Motion Sensor Sensitivity",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )
                Text(
                    text = "Filters out table vibrations. Requires physical lift (G-Force > 2.0, tilt angle > 20°).",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                val currentSensitivityLevel = when {
                    config.motionSensitivity <= 1.5f -> 1
                    config.motionSensitivity <= 2.5f -> 2
                    else -> 3
                }
                val sensitivityLabel = when (currentSensitivityLevel) {
                    1 -> "Low (Only big movements / high jerk)"
                    2 -> "Medium (Normal - Lift > 2.0G [Default])"
                    else -> "High (Sensitive to minor shifts)"
                }

                Text(
                    text = "Active Level: $sensitivityLabel",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = YellowAccent
                )

                Slider(
                    value = config.motionSensitivity,
                    onValueChange = { viewModel.updateConfig { c -> c.copy(motionSensitivity = it) } },
                    valueRange = 1f..3f,
                    steps = 1,
                    colors = SliderDefaults.colors(
                        thumbColor = YellowAccent,
                        activeTrackColor = NavyPrimary
                    ),
                    modifier = Modifier.testTag("slider_motion_sensitivity")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        Triple(1f, "Low", "Big movements"),
                        Triple(2f, "Medium (Def)", "Normal lift"),
                        Triple(3f, "High", "Sensitive")
                    ).forEach { (value, label, desc) ->
                        val isSelected = when (value.toInt()) {
                            1 -> config.motionSensitivity <= 1.5f
                            2 -> config.motionSensitivity in 1.5f..2.5f
                            else -> config.motionSensitivity > 2.5f
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) NavyPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, YellowAccent) else null,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    viewModel.updateConfig { c -> c.copy(motionSensitivity = value) }
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) YellowAccent else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = desc,
                                    fontSize = 9.sp,
                                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // FEATURE 4: ARM TIME SETTINGS CARD
        var showCustomArmDialog by remember { mutableStateOf(false) }
        var customArmInput by remember { mutableStateOf(config.armingDelaySeconds.toString()) }

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    RoundedCornerShape(18.dp)
                )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Arm Delay Settings",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                        Text(
                            text = "Arm Delay: [ ${config.armingDelaySeconds} sec ]",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = YellowAccent
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = NavyPrimary.copy(alpha = 0.1f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = NavyPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "Provides enough time to safely set the phone down or place inside pocket before sensors arm.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                // Presets: 10s, 20s, 30s (Default), 60s, Custom
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(10, 20, 30, 60).forEach { sec ->
                        val isSelected = config.armingDelaySeconds == sec
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) NavyPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, YellowAccent) else null,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    viewModel.updateConfig { c -> c.copy(armingDelaySeconds = sec) }
                                    viewModel.showMessage("Arm delay set to $sec seconds")
                                }
                                .testTag("btn_arm_delay_$sec")
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${sec}s",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) YellowAccent else MaterialTheme.colorScheme.onSurface
                                )
                                if (sec == 30) {
                                    Text(
                                        text = "Def",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Custom Option Button
                    val isCustom = config.armingDelaySeconds !in listOf(10, 20, 30, 60)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isCustom) NavyPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = if (isCustom) androidx.compose.foundation.BorderStroke(1.5.dp, YellowAccent) else null,
                        modifier = Modifier
                            .weight(1.2f)
                            .clickable {
                                customArmInput = config.armingDelaySeconds.toString()
                                showCustomArmDialog = true
                            }
                            .testTag("btn_arm_delay_custom")
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isCustom) "${config.armingDelaySeconds}s" else "Custom",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCustom) YellowAccent else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Edit",
                                fontSize = 9.sp,
                                color = if (isCustom) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (showCustomArmDialog) {
                    androidx.compose.ui.window.Dialog(onDismissRequest = { showCustomArmDialog = false }) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = NavyDark),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "Custom Arm Delay",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = YellowAccent
                                )
                                Text(
                                    text = "Enter countdown duration in seconds (5 - 300 sec):",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                                OutlinedTextField(
                                    value = customArmInput,
                                    onValueChange = { customArmInput = it.filter { ch -> ch.isDigit() } },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = YellowAccent,
                                        unfocusedBorderColor = Color.Gray
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    OutlinedButton(onClick = { showCustomArmDialog = false }) {
                                        Text("Cancel", color = Color.White)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            val parsed = customArmInput.toIntOrNull()?.coerceIn(5, 300) ?: 30
                                            viewModel.updateConfig { c -> c.copy(armingDelaySeconds = parsed) }
                                            viewModel.showMessage("Arm delay set to $parsed seconds")
                                            showCustomArmDialog = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = YellowAccent, contentColor = NavyDark)
                                    ) {
                                        Text("Save", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. FOREGROUND SERVICE STATUS & TOGGLE
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    RoundedCornerShape(18.dp)
                )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Foreground Guard Service",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                        Text(
                            text = if (state.isServiceRunning) "Running in background (Persistent notification active)" else "Service stopped",
                            fontSize = 11.sp,
                            color = if (state.isServiceRunning) SafeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = state.isServiceRunning,
                        onCheckedChange = { shouldRun ->
                            if (shouldRun) viewModel.startForegroundGuardService()
                            else viewModel.stopForegroundGuardService()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = YellowAccent,
                            checkedTrackColor = NavyPrimary
                        ),
                        modifier = Modifier.testTag("switch_foreground_service")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. MASTER SECURITY PIN
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    RoundedCornerShape(18.dp)
                )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = NavyPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = viewModel.tr("security_pin"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                }

                Text(
                    text = "This PIN is required to disarm the alarm siren when triggered.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = pinText,
                        onValueChange = { if (it.length <= 6) pinText = it },
                        placeholder = { Text("4-6 Digits") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NavyPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_master_pin")
                    )

                    Button(
                        onClick = {
                            viewModel.updateMasterPin(pinText)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = YellowAccent,
                            contentColor = NavyDark
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_save_pin")
                    ) {
                        Text(text = viewModel.tr("save"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. MULTI-LANGUAGE SELECTOR
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    RoundedCornerShape(18.dp)
                )
                .testTag("card_language_selector")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(NavyPrimary.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = NavyPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = viewModel.tr("language_select"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "English • اردو • हिन्दी • العربية • Español",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppLanguage.values().forEach { lang ->
                        val isSelected = state.selectedLanguage == lang
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) NavyPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setLanguage(lang) }
                                .testTag("lang_option_${lang.code}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = lang.nativeName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) YellowAccent else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = lang.displayName,
                                        fontSize = 11.sp,
                                        color = if (isSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = YellowAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // GOOGLE ACCOUNT & LOGOUT SECTION
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NavyCardDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1A73E8))
                        ) {
                            Text(
                                text = (state.userEmail ?: "G").take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Google Account",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = state.userEmail ?: state.familyNetwork.accountEmail,
                                fontSize = 12.sp,
                                color = YellowAccent
                            )
                        }
                    }

                    Surface(
                        color = SafeGreen.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "SYNCED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafeGreen,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "All devices registered under ${state.userEmail ?: state.familyNetwork.accountEmail} stay synchronized across the Family Security Network.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // LOGOUT BUTTON
                Button(
                    onClick = {
                        viewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AlertRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("btn_logout")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Logout",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Logout",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = YellowAccent,
                checkedTrackColor = NavyPrimary
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}
