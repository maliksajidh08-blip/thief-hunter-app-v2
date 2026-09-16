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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import com.example.data.Screen
import com.example.ui.theme.AlertRed
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.YellowAccent

@Composable
fun ReportTheftScreen(
    viewModel: ThiefHunterViewModel,
    modifier: Modifier = Modifier
) {
    var imei by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var modelName by remember { mutableStateOf("") }
    var theftLocation by remember { mutableStateOf("") }
    var firNumber by remember { mutableStateOf("") }
    var ownerPhone by remember { mutableStateOf("") }
    var rewardAmount by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // HEADER ALERT CARD
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = AlertRed.copy(alpha = 0.1f)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AlertRed.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(AlertRed)
                ) {
                    Icon(
                        imageVector = Icons.Default.ReportProblem,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = viewModel.tr("report_theft"),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = AlertRed
                    )
                    Text(
                        text = viewModel.tr("report_theft_desc"),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // FORM FIELDS
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
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Device Identification",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )

                OutlinedTextField(
                    value = imei,
                    onValueChange = { if (it.length <= 15) imei = it },
                    label = { Text(viewModel.tr("imei_number") + " *") },
                    placeholder = { Text("15-digit hardware identifier") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Dialpad, contentDescription = null, tint = NavyPrimary)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NavyPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("report_input_imei")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = { Text("Brand *") },
                        placeholder = { Text("e.g. Samsung") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NavyPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("report_input_brand")
                    )

                    OutlinedTextField(
                        value = modelName,
                        onValueChange = { modelName = it },
                        label = { Text("Model *") },
                        placeholder = { Text("e.g. S24 Ultra") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NavyPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("report_input_model")
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Police Case & Location Details",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )

                OutlinedTextField(
                    value = firNumber,
                    onValueChange = { firNumber = it },
                    label = { Text(viewModel.tr("police_fir")) },
                    placeholder = { Text("e.g. FIR-2026/894 Police Station") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = NavyPrimary)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NavyPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("report_input_fir")
                )

                OutlinedTextField(
                    value = theftLocation,
                    onValueChange = { theftLocation = it },
                    label = { Text(viewModel.tr("theft_location") + " *") },
                    placeholder = { Text("City, Market, Street") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = NavyPrimary)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NavyPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("report_input_location")
                )

                OutlinedTextField(
                    value = ownerPhone,
                    onValueChange = { ownerPhone = it },
                    label = { Text(viewModel.tr("owner_phone") + " *") },
                    placeholder = { Text("+92 3XX •••••••") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = NavyPrimary)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NavyPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("report_input_phone")
                )

                OutlinedTextField(
                    value = rewardAmount,
                    onValueChange = { rewardAmount = it },
                    label = { Text(viewModel.tr("reward") + " (Optional)") },
                    placeholder = { Text("e.g. $100 / Rs 25,000") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.AttachMoney, contentDescription = null, tint = NavyPrimary)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NavyPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("report_input_reward")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (imei.isNotBlank() && brand.isNotBlank()) {
                            viewModel.submitTheftReport(
                                imei = imei,
                                brand = brand,
                                modelName = if (modelName.isBlank()) "Phone" else modelName,
                                firNumber = firNumber,
                                theftLocation = if (theftLocation.isBlank()) "Unknown" else theftLocation,
                                ownerPhone = ownerPhone,
                                rewardAmount = rewardAmount
                            )
                            viewModel.navigateTo(Screen.STOLEN_DEVICES)
                        } else {
                            viewModel.showMessage("Please fill IMEI and Brand details.")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AlertRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_submit_theft_report")
                ) {
                    Icon(imageVector = Icons.Default.ReportProblem, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = viewModel.tr("submit_report"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
