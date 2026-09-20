package com.example.ui

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FamilyAlert
import com.example.data.FamilyDeviceNode
import com.example.data.FamilyDeviceRole
import com.example.data.Screen
import com.example.security.TriggerReason
import com.example.ui.theme.AlertRed
import com.example.ui.theme.InfoCyan
import com.example.ui.theme.NavyCardDark
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyLight
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.YellowAccent
import com.example.ui.theme.YellowDark

@Composable
fun FamilyNetworkScreen(
    viewModel: ThiefHunterViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val family = state.familyNetwork

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Devices (5), 1: Pocket Sensor Live, 2: Network Feed
    var showGmailDialog by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf(family.accountEmail) }
    var showBroadcastDialog by remember { mutableStateOf(false) }
    var broadcastTitle by remember { mutableStateOf("") }
    var broadcastDesc by remember { mutableStateOf("") }

    var showRegisterDialog by remember { mutableStateOf(false) }
    var regName by remember { mutableStateOf("") }
    var regOwner by remember { mutableStateOf("") }
    var regModel by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("+92-300-1234567") }
    var regImei by remember { mutableStateOf("357891043218765") }

    var editingDeviceId by remember { mutableStateOf<String?>(null) }
    var editingPhoneInput by remember { mutableStateOf("") }

    val infiniteTransition = rememberInfiniteTransition(label = "pulseFailover")
    val failoverGlowColor by infiniteTransition.animateColor(
        initialValue = AlertRed,
        targetValue = YellowAccent,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "failoverGlow"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TOP HEADER: GMAIL & MASTER FAILOVER SUMMARY
        Card(
            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
            colors = CardDefaults.cardColors(containerColor = NavyDark),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (family.isFailoverActive) Modifier.border(2.dp, failoverGlowColor, RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                    else Modifier
                )
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
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (family.isFailoverActive) AlertRed else YellowAccent)
                        ) {
                            Icon(
                                imageVector = if (family.isFailoverActive) Icons.Default.Warning else Icons.Default.Hub,
                                contentDescription = "Network Hub",
                                tint = NavyDark,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Family Security Network",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = YellowAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = family.accountEmail,
                                    fontSize = 12.sp,
                                    color = YellowAccent,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Surface(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.clickable { showGmailDialog = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync",
                                tint = YellowAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "LINK",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = YellowAccent
                            )
                        }
                    }
                }

                // FAILOVER BANNER (ACTIVATES WHEN MASTER IS STOLEN)
                if (family.isFailoverActive) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = AlertRed.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AlertRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = AlertRed,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "MASTER FAILOVER ACTIVATED!",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AlertRed,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Master Phone is STOLEN. Guardian role elected to Mom's Galaxy A54. All 4 devices hold full remote control.",
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "5 Phones Connected • Realtime Sync Active",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Surface(
                            color = SafeGreen.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "MASTER READY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SafeGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // PERSPECTIVE SELECTOR (Allows testing cross-device experience from any of the 5 family devices)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "PERSPECTIVE CONTROLLER (Viewing as):",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(family.devices) { dev ->
                    val isCurrent = (dev.id == family.currentActiveDeviceId)
                    FilterChip(
                        selected = isCurrent,
                        onClick = { viewModel.switchActivePerspectiveDevice(dev.id) },
                        label = {
                            Text(
                                text = dev.name,
                                fontSize = 12.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (dev.isStolen) Icons.Default.Warning else Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = if (dev.isStolen) AlertRed else if (isCurrent) NavyDark else YellowDark,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = YellowAccent,
                            selectedLabelColor = NavyDark
                        )
                    )
                }
            }
        }

        // TAB BAR NAVIGATION
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = NavyDark,
            contentColor = YellowAccent
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("5 Family Phones (${family.devices.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Pocket & Grab Sensor", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(Icons.Default.Sensors, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Breach Alerts (${family.alerts.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        // TAB CONTENTS
        when (selectedTab) {
            0 -> FamilyDevicesTab(
                devices = family.devices,
                currentActiveId = family.currentActiveDeviceId,
                masterId = family.masterDeviceId,
                accountEmail = family.accountEmail,
                isFailover = family.isFailoverActive,
                onRemoteLock = { viewModel.remoteLockFamilyDevice(it) },
                onRemoteUnlock = { viewModel.remoteUnlockFamilyDevice(it) },
                onRemoteSiren = { viewModel.remoteTriggerFamilySiren(it) },
                onStopSiren = { viewModel.remoteStopFamilySiren(it) },
                onMarkStolen = { viewModel.markFamilyDeviceStolen(it) },
                onUnmarkStolen = { viewModel.unmarkFamilyDeviceStolen(it) },
                onViewVault = { viewModel.navigateTo(Screen.INTRUDER_SELFIE) },
                onOpenMap = { lat, lng ->
                    val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(Family Device Location)")
                    val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                    mapIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    try {
                        context.startActivity(mapIntent)
                    } catch (_: Exception) {
                        viewModel.navigateTo(Screen.LIVE_TRACKING)
                    }
                },
                onEditPhoneClick = { devId, currentPhone ->
                    editingDeviceId = devId
                    editingPhoneInput = currentPhone
                },
                onRegisterDeviceClick = {
                    showRegisterDialog = true
                }
            )
            1 -> SmartPocketSensorTab(viewModel = viewModel)
            2 -> FamilyAlertsTab(
                alerts = family.alerts,
                onBroadcastClick = { showBroadcastDialog = true }
            )
        }
    }

    // DIALOG: GMAIL LINKING / SYNC
    if (showGmailDialog) {
        AlertDialog(
            onDismissRequest = { showGmailDialog = false },
            title = { Text("Family Network Gmail Sync", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Enter your family Google account. All 5 phones signed in with this Gmail synchronize auto-lock, siren triggers, live GPS, and thief selfie vaults.",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Gmail Account") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateFamilyGmail(emailInput)
                        showGmailDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyDark)
                ) {
                    Text("Sync Network", color = YellowAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGmailDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // DIALOG: BROADCAST EMERGENCY TO ALL FAMILY PHONES
    if (showBroadcastDialog) {
        AlertDialog(
            onDismissRequest = { showBroadcastDialog = false },
            title = { Text("Broadcast Emergency to All 4 Phones", fontWeight = FontWeight.Bold, color = AlertRed) },
            text = {
                Column {
                    Text(
                        text = "This will immediately send a high-priority push notification and security ping to all 5 connected family devices.",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = broadcastTitle,
                        onValueChange = { broadcastTitle = it },
                        label = { Text("Alert Title") },
                        placeholder = { Text("e.g. Suspicious Activity near Ali") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = broadcastDesc,
                        onValueChange = { broadcastDesc = it },
                        label = { Text("Details / Note") },
                        placeholder = { Text("e.g. Please check on phone location immediately") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (broadcastTitle.isNotBlank()) {
                            viewModel.broadcastFamilyEmergency(broadcastTitle, broadcastDesc)
                            broadcastTitle = ""
                            broadcastDesc = ""
                            showBroadcastDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("Send Broadcast", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBroadcastDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // DIALOG: REGISTER NEW FAMILY DEVICE
    if (showRegisterDialog) {
        AlertDialog(
            onDismissRequest = { showRegisterDialog = false },
            title = {
                Text("Register Family Device", fontWeight = FontWeight.Bold, color = YellowAccent)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Add another device to your Family Security Network under ${family.accountEmail}.",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )
                    OutlinedTextField(
                        value = regName,
                        onValueChange = { regName = it },
                        label = { Text("Device Name (e.g. Son's Pixel 8)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = regOwner,
                        onValueChange = { regOwner = it },
                        label = { Text("Owner Name (e.g. Ali (Son))") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = regModel,
                        onValueChange = { regModel = it },
                        label = { Text("Device Model (e.g. Google Pixel 8)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = regPhone,
                        onValueChange = { regPhone = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = regImei,
                        onValueChange = { regImei = it },
                        label = { Text("IMEI (15 Digits)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (regName.isNotBlank() && regOwner.isNotBlank()) {
                            viewModel.addFamilyDevice(
                                name = regName.trim(),
                                ownerName = regOwner.trim(),
                                model = if (regModel.isBlank()) "Android Smartphone" else regModel.trim(),
                                phoneNumber = regPhone.trim(),
                                imei = regImei.trim()
                            )
                            showRegisterDialog = false
                            regName = ""
                            regOwner = ""
                            regModel = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = YellowAccent, contentColor = NavyDark)
                ) {
                    Text("Register Phone", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegisterDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // DIALOG: EDIT PHONE NUMBER
    editingDeviceId?.let { devId ->
        AlertDialog(
            onDismissRequest = { editingDeviceId = null },
            title = {
                Text("Edit Emergency Phone Number", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Used for emergency broadcast alerts and remote SMS coordination.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editingPhoneInput,
                        onValueChange = { editingPhoneInput = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = SafeGreen) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateFamilyDevicePhone(devId, editingPhoneInput)
                        editingDeviceId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = YellowAccent, contentColor = NavyDark)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingDeviceId = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun FamilyDevicesTab(
    devices: List<FamilyDeviceNode>,
    currentActiveId: String,
    masterId: String,
    accountEmail: String,
    isFailover: Boolean,
    onRemoteLock: (String) -> Unit,
    onRemoteUnlock: (String) -> Unit,
    onRemoteSiren: (String) -> Unit,
    onStopSiren: (String) -> Unit,
    onMarkStolen: (String) -> Unit,
    onUnmarkStolen: (String) -> Unit,
    onViewVault: () -> Unit,
    onOpenMap: (Double, Double) -> Unit,
    onEditPhoneClick: (String, String) -> Unit,
    onRegisterDeviceClick: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Connected Family Devices (${devices.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    Text(
                        text = "All linked via $accountEmail",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                Button(
                    onClick = onRegisterDeviceClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = YellowAccent,
                        contentColor = NavyDark
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_register_device")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Device", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        items(devices, key = { it.id }) { dev ->
            val isCurrentPerspective = (dev.id == currentActiveId)
            val isMaster = (dev.id == masterId)

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (dev.isStolen) Color(0xFF2E0909) else NavyCardDark
                ),
                border = if (dev.isStolen) androidx.compose.foundation.BorderStroke(2.dp, AlertRed)
                else if (isCurrentPerspective) androidx.compose.foundation.BorderStroke(2.dp, YellowAccent)
                else androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("family_device_card_${dev.id}")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // TITLE & ROLE BADGE
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
                                    .background(
                                        if (dev.isStolen) AlertRed
                                        else if (dev.role == FamilyDeviceRole.MASTER) YellowDark
                                        else if (dev.role == FamilyDeviceRole.FAILOVER_GUARDIAN) Color(0xFF9C27B0)
                                        else NavyPrimary
                                    )
                            ) {
                                Icon(
                                    imageVector = if (dev.isStolen) Icons.Default.Warning else Icons.Default.Smartphone,
                                    contentDescription = null,
                                    tint = if (dev.role == FamilyDeviceRole.MASTER) NavyDark else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = dev.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                    if (isCurrentPerspective) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = YellowAccent,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "YOU",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = NavyDark,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "${dev.ownerName} • ${dev.model}",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }

                        // ROLE BADGE
                        Surface(
                            color = when (dev.role) {
                                FamilyDeviceRole.MASTER -> if (dev.isStolen) AlertRed else YellowAccent
                                FamilyDeviceRole.FAILOVER_GUARDIAN -> Color(0xFFAB47BC)
                                FamilyDeviceRole.MEMBER -> InfoCyan
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = when (dev.role) {
                                    FamilyDeviceRole.MASTER -> if (dev.isStolen) "MASTER (STOLEN)" else "MASTER"
                                    FamilyDeviceRole.FAILOVER_GUARDIAN -> "GUARDIAN CONTROLLER"
                                    FamilyDeviceRole.MEMBER -> "MEMBER"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (dev.role == FamilyDeviceRole.MASTER && !dev.isStolen) NavyDark else Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // PHONE NUMBER & IMEI
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        // Phone Number Row (Editable)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { onEditPhoneClick(dev.id, dev.phoneNumber) }
                                    .padding(vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Phone",
                                    tint = SafeGreen,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = dev.phoneNumber,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Phone",
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    try {
                                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${dev.phoneNumber}"))
                                        dialIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        context.startActivity(dialIntent)
                                    } catch (_: Exception) {}
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Call Device",
                                    tint = SafeGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // IMEI Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "IMEI",
                                    tint = YellowAccent,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "IMEI: ${dev.imei}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }

                            Surface(
                                color = Color.White.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "TRACKING ID",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = YellowAccent,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // STATUS ROW: Battery, Locked, Siren, GPS
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.BatteryChargingFull,
                                contentDescription = null,
                                tint = if (dev.batteryPct > 20) SafeGreen else AlertRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${dev.batteryPct}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (dev.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = if (dev.isLocked) AlertRed else SafeGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (dev.isLocked) "Locked" else "Unlocked",
                                fontSize = 12.sp,
                                color = if (dev.isLocked) AlertRed else SafeGreen,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (dev.isSirenActive) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = null,
                                tint = if (dev.isSirenActive) AlertRed else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (dev.isSirenActive) "SIREN ON" else "Silent",
                                fontSize = 12.sp,
                                color = if (dev.isSirenActive) AlertRed else Color.White.copy(alpha = 0.6f)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(SafeGreen)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = dev.lastSeenTime,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // LOCATION BAR
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GpsFixed,
                                contentDescription = null,
                                tint = YellowAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = dev.address,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        TextButton(
                            onClick = { onOpenMap(dev.latitude, dev.longitude) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = null,
                                tint = YellowAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(text = "Map", fontSize = 11.sp, color = YellowAccent)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // INTERACTIVE ACTIONS FOR THIS PHONE
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Remote Lock/Unlock
                        OutlinedButton(
                            onClick = {
                                if (dev.isLocked) onRemoteUnlock(dev.id) else onRemoteLock(dev.id)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (dev.isLocked) SafeGreen else AlertRed
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (dev.isLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (dev.isLocked) "Unlock" else "Lock",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Remote Siren
                        OutlinedButton(
                            onClick = {
                                if (dev.isSirenActive) onStopSiren(dev.id) else onRemoteSiren(dev.id)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = YellowAccent
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (dev.isSirenActive) Icons.Default.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (dev.isSirenActive) "Stop Siren" else "Sound Siren",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Mark as Stolen / Recovered
                        Button(
                            onClick = {
                                if (dev.isStolen) onUnmarkStolen(dev.id) else onMarkStolen(dev.id)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (dev.isStolen) SafeGreen else AlertRed
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Icon(
                                imageVector = if (dev.isStolen) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (dev.isStolen) "Mark Recovered" else "Mark Stolen",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Thief Photo Vault Quick Link if device has photos
                    if (dev.capturedPhotoCount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = Color.White.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onViewVault() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = null,
                                    tint = YellowAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "View Thief Intruder Photo (${dev.capturedPhotoCount} Capture)",
                                    fontSize = 12.sp,
                                    color = YellowAccent,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SmartPocketSensorTab(viewModel: ThiefHunterViewModel) {
    val state by viewModel.uiState.collectAsState()
    val telemetry = state.sensorTelemetry

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NavyDark),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SafeGreen)
                        ) {
                            Icon(Icons.Default.Sensors, contentDescription = null, tint = NavyDark, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Smart Pocket & Hand Grab Filter",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Proximity + Ambient Light + 3-Axis Accelerometer",
                                color = YellowAccent,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "This smart filter distinguishes normal body movements while walking from actual pickpocket extraction and unauthorized outside hand grabs:",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = Color.Black.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SafeGreen))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Phone moves inside pocket (Walking / Shaking): NO ALARM (Filtered)",
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AlertRed))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Phone being taken out (Near -> Far + Light Jump): ALARM",
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(YellowAccent))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Hand touches from outside (Proximity disturbance / Snatch): ALARM",
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // REALTIME TELEMETRY SENSORS STATUS
        item {
            Text(
                text = "LIVE SENSOR TELEMETRY",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // In Pocket indicator
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (telemetry.isInPocket) SafeGreen.copy(alpha = 0.2f) else NavyCardDark
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Pocket Status", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (telemetry.isInPocket) "IN POCKET" else "OUT OF POCKET",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (telemetry.isInPocket) SafeGreen else YellowAccent
                        )
                    }
                }

                // Walking motion filter
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (telemetry.isWalkingFiltered) InfoCyan.copy(alpha = 0.2f) else NavyCardDark
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Walking Filter", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (telemetry.isWalkingFiltered) "FILTERING (SAFE)" else "NORMAL",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (telemetry.isWalkingFiltered) InfoCyan else Color.White
                        )
                    }
                }
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Proximity
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = NavyCardDark),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Proximity Sensor", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (telemetry.isProximityNear) "NEAR (${telemetry.proximityCm.toInt()} cm)" else "FAR (${telemetry.proximityCm.toInt()} cm)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (telemetry.isProximityNear) SafeGreen else Color.White
                        )
                    }
                }

                // Ambient Light
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = NavyCardDark),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Light Sensor", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${telemetry.lightLux.toInt()} Lux ${if (telemetry.lightLux < 25f) "(Dark Pocket)" else "(Exposed)"}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (telemetry.lightLux < 25f) Color(0xFF90CAF9) else YellowAccent
                        )
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = NavyCardDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("3-Axis Accelerometer & Jerk Vector", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Total Force: ${"%.2f".format(telemetry.totalAcceleration)} m/s² | Upward Lift: ${if (telemetry.liftDetected) "DETECTED" else "STABLE"}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (telemetry.liftDetected) AlertRed else Color.White
                    )
                }
            }
        }

        // SIMULATION TEST CONTROLS (So user can test the smart algorithm immediately)
        item {
            Text(
                text = "TEST SENSOR ALGORITHMS",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        viewModel.showMessage("Simulating walking in pocket: Proximity NEAR + Light DARK + Normal step oscillation. Result: SILENT (No Alarm)!")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test Walking inside Pocket (Silent - Filtered)")
                }

                Button(
                    onClick = {
                        viewModel.triggerAlarmWithReason(TriggerReason.POCKET_REMOVAL)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test Pocket Extraction (Triggers Alarm)")
                }

                Button(
                    onClick = {
                        viewModel.triggerAlarmWithReason(TriggerReason.HAND_GRAB_DETECTED)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = YellowDark),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test Hand Touch / Outside Snatch (Triggers Alarm)", color = NavyDark)
                }
            }
        }
    }
}

@Composable
fun FamilyAlertsTab(
    alerts: List<FamilyAlert>,
    onBroadcastClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "CROSS-DEVICE SECURITY FEED",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onBroadcastClick,
                colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Broadcast Alert", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(alerts, key = { it.id }) { alert ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (alert.isUrgent) Color(0xFF280B0B) else NavyCardDark
                    ),
                    border = if (alert.isUrgent) androidx.compose.foundation.BorderStroke(1.dp, AlertRed)
                    else androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (alert.isUrgent) AlertRed else SafeGreen)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = alert.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (alert.isUrgent) AlertRed else Color.White
                                )
                            }
                            Text(
                                text = alert.timestamp,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = alert.description,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Origin: ${alert.originDeviceName}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = YellowAccent
                        )
                    }
                }
            }
        }
    }
}
