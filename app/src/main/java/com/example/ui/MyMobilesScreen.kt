package com.example.ui

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MobileDevice
import com.example.data.Screen
import com.example.ui.theme.AlertRed
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.YellowAccent

@Composable
fun MyMobilesScreen(
    viewModel: ThiefHunterViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "${viewModel.tr("my_mobiles")} (${state.myDevices.size})",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.myDevices) { device ->
                    DeviceCard(
                        device = device,
                        viewModel = viewModel,
                        onToggleLock = { viewModel.toggleDeviceLock(device.id) },
                        onToggleSiren = { viewModel.toggleDeviceSiren(device.id) },
                        onTrack = { viewModel.navigateTo(Screen.LIVE_TRACKING) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }

        // ADD MOBILE FLOATING ACTION BUTTON
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = YellowAccent,
            contentColor = NavyDark,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("fab_add_mobile")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Mobile")
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = viewModel.tr("add_device"), fontWeight = FontWeight.Bold)
            }
        }

        if (showAddDialog) {
            AddMobileDialog(
                viewModel = viewModel,
                onDismiss = { showAddDialog = false },
                onAdd = { name, model, imei ->
                    viewModel.addMobile(name, model, imei)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun DeviceCard(
    device: MobileDevice,
    viewModel: ThiefHunterViewModel,
    onToggleLock: () -> Unit,
    onToggleSiren: () -> Unit,
    onTrack: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (device.isLocked) AlertRed.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                RoundedCornerShape(18.dp)
            )
            .testTag("device_card_${device.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(NavyPrimary.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = NavyPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = device.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = device.model,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = if (device.isLocked) AlertRed.copy(alpha = 0.15f) else SafeGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (device.isLocked) viewModel.tr("status_locked") else viewModel.tr("status_secured"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (device.isLocked) AlertRed else SafeGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // IMEI & DETAILS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "IMEI: ${device.imei}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SimCard,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = device.simCardNumber,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BatteryChargingFull,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = SafeGreen
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${device.batteryPercent}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ACTION BUTTONS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onToggleSiren,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (device.isSirenPlaying) AlertRed else NavyPrimary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (device.isSirenPlaying) Icons.Default.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (device.isSirenPlaying) viewModel.tr("quick_stop") else viewModel.tr("ring_siren"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = onToggleLock,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (device.isLocked) SafeGreen else AlertRed,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (device.isLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (device.isLocked) viewModel.tr("unlock_device") else viewModel.tr("remote_lock"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = onTrack,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = YellowAccent,
                        contentColor = NavyDark
                    ),
                    modifier = Modifier.size(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = "Track",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddMobileDialog(
    viewModel: ThiefHunterViewModel,
    onDismiss: () -> Unit,
    onAdd: (name: String, model: String, imei: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var imei by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = viewModel.tr("add_device"),
                fontWeight = FontWeight.Bold,
                color = NavyPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(viewModel.tr("device_name")) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_device_name")
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text(viewModel.tr("device_model")) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_device_model")
                )
                OutlinedTextField(
                    value = imei,
                    onValueChange = { if (it.length <= 15) imei = it },
                    label = { Text(viewModel.tr("imei_number")) },
                    placeholder = { Text("15 digits") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_device_imei")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(name, model, imei) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = YellowAccent,
                    contentColor = NavyDark
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_confirm_add_device")
            ) {
                Text(text = viewModel.tr("save"), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = viewModel.tr("cancel"))
            }
        }
    )
}
