package com.example.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.ui.theme.AlertRed
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.YellowAccent

@Composable
fun AddDeviceDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, imei: String, ownerName: String, ownerPhone: String) -> Unit,
    currentDeviceCount: Int = 0
) {
    var name by remember { mutableStateOf("") }
    var imei by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var ownerPhone by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var imeiError by remember { mutableStateOf<String?>(null) }
    var ownerNameError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(NavyPrimary.copy(alpha = 0.12f))
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = NavyPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Add New Device",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Slot ${currentDeviceCount + 1} of 10 maximum",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Info banner
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = NavyPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Dial *#06# on phone dialpad to view the 15-digit IMEI number.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 1. Device Name
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) nameError = null
                    },
                    label = { Text("Device Name *") },
                    placeholder = { Text("e.g. My Phone, Work Samsung") },
                    leadingIcon = {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = NavyPrimary)
                    },
                    isError = nameError != null,
                    supportingText = {
                        if (nameError != null) {
                            Text(nameError!!, color = AlertRed, fontSize = 11.sp)
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NavyPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_device_name")
                )

                // 2. IMEI Number (15 digits validation)
                OutlinedTextField(
                    value = imei,
                    onValueChange = { input ->
                        val digitsOnly = input.filter { it.isDigit() }
                        if (digitsOnly.length <= 15) {
                            imei = digitsOnly
                            if (digitsOnly.length == 15) imeiError = null
                        }
                    },
                    label = { Text("IMEI Number (15 digits) *") },
                    placeholder = { Text("e.g. 358941092837461") },
                    leadingIcon = {
                        Icon(Icons.Default.Pin, contentDescription = null, tint = NavyPrimary)
                    },
                    trailingIcon = {
                        Text(
                            text = "${imei.length}/15",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (imei.length == 15) SafeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    },
                    isError = imeiError != null,
                    supportingText = {
                        if (imeiError != null) {
                            Text(imeiError!!, color = AlertRed, fontSize = 11.sp)
                        } else if (imei.length == 15) {
                            Text("✓ Valid 15-digit IMEI format", color = SafeGreen, fontSize = 11.sp)
                        } else {
                            Text("Must be exactly 15 digits", fontSize = 11.sp)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NavyPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_device_imei")
                )

                // 3. Owner Name
                OutlinedTextField(
                    value = ownerName,
                    onValueChange = {
                        ownerName = it
                        if (it.isNotBlank()) ownerNameError = null
                    },
                    label = { Text("Owner Name *") },
                    placeholder = { Text("e.g. Sajid, Alex") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = NavyPrimary)
                    },
                    isError = ownerNameError != null,
                    supportingText = {
                        if (ownerNameError != null) {
                            Text(ownerNameError!!, color = AlertRed, fontSize = 11.sp)
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NavyPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_owner_name")
                )

                // 4. Owner Phone
                OutlinedTextField(
                    value = ownerPhone,
                    onValueChange = { ownerPhone = it },
                    label = { Text("Owner Phone (Emergency Contact)") },
                    placeholder = { Text("e.g. +92 300 4589211") },
                    leadingIcon = {
                        Icon(Icons.Default.Call, contentDescription = null, tint = NavyPrimary)
                    },
                    supportingText = {
                        Text("SMS coordinates dispatched here on theft", fontSize = 11.sp)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NavyPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_owner_phone")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    var hasError = false
                    if (name.trim().isBlank()) {
                        nameError = "Device name is required"
                        hasError = true
                    }
                    if (imei.length != 15) {
                        imeiError = "IMEI must be exactly 15 digits"
                        hasError = true
                    }
                    if (ownerName.trim().isBlank()) {
                        ownerNameError = "Owner name is required"
                        hasError = true
                    }

                    if (!hasError) {
                        onSave(name.trim(), imei.trim(), ownerName.trim(), ownerPhone.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = YellowAccent,
                    contentColor = NavyDark
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_save_new_device")
            ) {
                Text(text = "Save Device", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_cancel_add_device")
            ) {
                Text(text = "Cancel")
            }
        }
    )
}
