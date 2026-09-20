package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.security.CameraCaptureHelper
import com.example.ui.theme.AlertRed
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.YellowAccent
import java.io.File

@Composable
fun IntruderSelfieScreen(
    viewModel: ThiefHunterViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var selectedPreviewPhotoPath by remember { mutableStateOf<String?>(null) }
    var selectedCaptureId by remember { mutableStateOf<String?>(null) }
    var isTestingCamera by remember { mutableStateOf(false) }

    // Runtime CAMERA permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.showMessage("Camera permission granted. Triggering front camera...")
            isTestingCamera = true
            CameraCaptureHelper.takeFrontCameraPhotoSilent(
                context = context,
                lifecycleOwner = lifecycleOwner,
                onPhotoSaved = { savedPath ->
                    isTestingCamera = false
                    viewModel.onPhotoCaptured(savedPath, isManualTest = true)
                },
                onError = { err ->
                    isTestingCamera = false
                    viewModel.onPhotoCaptureFailed(err)
                }
            )
        } else {
            viewModel.showMessage("Camera permission denied. Front camera photos require Camera access.")
        }
    }

    // System camera preview fallback
    val cameraPreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        isTestingCamera = false
        if (bmp != null) {
            val savedPath = CameraCaptureHelper.saveBitmapToFile(context, bmp, "test_manual")
            if (savedPath != null) {
                viewModel.onPhotoCaptured(savedPath, isManualTest = true)
            } else {
                viewModel.recordIntruderCapture("Manual Photo Test Captured", null)
                viewModel.showMessage("Test photo captured!")
            }
        }
    }

    // React to 3rd wrong PIN event triggered anywhere in app
    LaunchedEffect(state.triggerPhotoCaptureEvent) {
        if (state.triggerPhotoCaptureEvent > 0L) {
            CameraCaptureHelper.takeFrontCameraPhotoSilent(
                context = context,
                lifecycleOwner = lifecycleOwner,
                onPhotoSaved = { savedPath ->
                    viewModel.onPhotoCaptured(savedPath, isManualTest = false)
                },
                onError = { err ->
                    viewModel.onPhotoCaptureFailed(err)
                }
            )
        }
    }

    // Full screen / Enlarged photo preview dialog
    selectedPreviewPhotoPath?.let { photoPath ->
        val file = File(photoPath)
        val fileExists = file.exists()

        Dialog(onDismissRequest = {
            selectedPreviewPhotoPath = null
            selectedCaptureId = null
        }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NavyDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Intruder Evidence Photo",
                            fontWeight = FontWeight.Bold,
                            color = YellowAccent,
                            fontSize = 15.sp
                        )
                        IconButton(onClick = {
                            selectedPreviewPhotoPath = null
                            selectedCaptureId = null
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (fileExists) {
                        AsyncImage(
                            model = file,
                            contentDescription = "Full Intruder Preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(2.dp, AlertRed, RoundedCornerShape(14.dp))
                        )
                    } else {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.DarkGray)
                        ) {
                            Text("Image file unavailable", color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "File: ${file.name}",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                    Text(
                        text = "Path: ${file.parentFile?.name}/${file.name}",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        selectedCaptureId?.let { capId ->
                            OutlinedButton(
                                onClick = {
                                    viewModel.deleteIntruderCapture(capId)
                                    selectedPreviewPhotoPath = null
                                    selectedCaptureId = null
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete", fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = {
                                selectedPreviewPhotoPath = null
                                selectedCaptureId = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = YellowAccent, contentColor = NavyDark),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Close", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // TOP BANNER
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = NavyPrimary),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("intruder_banner_card")
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(YellowAccent)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraFront,
                            contentDescription = null,
                            tint = NavyDark,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Intruder Selfie Vault",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Front Camera Auto-Capture on 3rd Wrong PIN",
                            fontSize = 12.sp,
                            color = YellowAccent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "If an unauthorized user enters the wrong master PIN 3 times, the front camera automatically snaps a silent photo, saves it as a real JPG into intruder_photos/ folder, and logs timestamp & GPS coordinates.",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED

                            if (!hasPermission) {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            } else {
                                isTestingCamera = true
                                CameraCaptureHelper.takeFrontCameraPhotoSilent(
                                    context = context,
                                    lifecycleOwner = lifecycleOwner,
                                    onPhotoSaved = { savedPath ->
                                        isTestingCamera = false
                                        viewModel.onPhotoCaptured(savedPath, isManualTest = true)
                                    },
                                    onError = { _ ->
                                        try {
                                            cameraPreviewLauncher.launch()
                                        } catch (_: Exception) {
                                            isTestingCamera = false
                                            viewModel.showMessage("Camera preview unavailable")
                                        }
                                    }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = YellowAccent,
                            contentColor = NavyDark
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("btn_test_selfie_camera")
                    ) {
                        if (isTestingCamera || state.isCameraCapturing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = NavyDark,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Capturing...", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Camera", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    // Direct test button for 3rd Wrong PIN trigger
                    Button(
                        onClick = {
                            viewModel.verifyAndDisarmPin("9999")
                            viewModel.verifyAndDisarmPin("9999")
                            viewModel.verifyAndDisarmPin("9999")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AlertRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(42.dp)
                            .testTag("btn_test_wrong_pin_photo")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Simulate Wrong PIN", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    if (state.intruderCaptures.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { viewModel.clearIntruderCaptures() },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .height(42.dp)
                                .testTag("btn_clear_intruder_log")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear All",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // LATEST CAPTURED PHOTO PREVIEW CARD
        state.lastCapturedPhotoPath?.let { photoPath ->
            val photoFile = File(photoPath)
            if (photoFile.exists()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .border(1.5.dp, AlertRed, RoundedCornerShape(16.dp))
                        .clickable {
                            selectedPreviewPhotoPath = photoPath
                            selectedCaptureId = state.intruderCaptures.firstOrNull { it.photoUri == photoPath }?.id
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = photoFile,
                            contentDescription = "Captured Intruder Selfie",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, AlertRed, RoundedCornerShape(12.dp))
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Latest Intruder Photo",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AlertRed
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = AlertRed.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "NEW",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AlertRed,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Real photo saved to ${photoFile.name}. Tap to enlarge.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Intruder Breach Logs (${state.intruderCaptures.size})",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (state.failedPinAttempts > 0) {
                Text(
                    text = "Wrong attempts: ${state.failedPinAttempts}/3",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AlertRed
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (state.intruderCaptures.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SafeGreen,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No Intruder Breaches Recorded",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Your device has had zero unauthorized access attempts.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(state.intruderCaptures) { item ->
                    val photoFile = item.photoUri?.let { File(it) }
                    val hasPhoto = photoFile != null && photoFile.exists()

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable(enabled = hasPhoto) {
                                item.photoUri?.let {
                                    selectedPreviewPhotoPath = it
                                    selectedCaptureId = item.id
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (hasPhoto && photoFile != null) {
                                AsyncImage(
                                    model = photoFile,
                                    contentDescription = "Intruder Photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, AlertRed, RoundedCornerShape(8.dp))
                                )
                            } else {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(AlertRed.copy(alpha = 0.12f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = AlertRed,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.triggerReason,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AlertRed
                                )
                                Text(
                                    text = "Timestamp: ${item.timestamp}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (hasPhoto) {
                                    Text(
                                        text = "Real photo captured • Tap to view",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SafeGreen
                                    )
                                }
                            }

                            IconButton(
                                onClick = { viewModel.deleteIntruderCapture(item.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete entry",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
