package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.AppLanguage
import com.example.data.Screen
import com.example.ui.CommunityScreen
import com.example.ui.HomeScreen
import com.example.ui.IntruderSelfieScreen
import com.example.ui.LiveTrackingScreen
import com.example.ui.MyMobilesScreen
import com.example.ui.ReportTheftScreen
import com.example.ui.SensorsHubScreen
import com.example.ui.SettingsScreen
import com.example.ui.SplashScreen
import com.example.ui.StolenDevicesScreen
import com.example.ui.ThiefHunterViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.YellowAccent

class MainActivity : ComponentActivity() {

    private val viewModel: ThiefHunterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                ThiefHunterApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThiefHunterApp(viewModel: ThiefHunterViewModel) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var showLanguageMenu by remember { mutableStateOf(false) }

    // Request permissions for Notifications, Location and Camera
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle runtime results gracefully
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.SEND_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val needed = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            permissionsLauncher.launch(needed.toTypedArray())
        }
    }

    val layoutDirection = if (state.selectedLanguage.isRtl) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }

    // Handle back button
    BackHandler(enabled = state.currentScreen != Screen.HOME && state.currentScreen != Screen.SPLASH) {
        viewModel.navigateTo(Screen.HOME)
    }

    LaunchedEffect(state.activeSnackbarMessage) {
        state.activeSnackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (state.currentScreen != Screen.SPLASH) {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (state.currentScreen == Screen.HOME) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_thief_hunter_logo),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = viewModel.tr("app_name"),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = YellowAccent
                                    )
                                } else {
                                    Text(
                                        text = when (state.currentScreen) {
                                            Screen.SENSORS_HUB -> "Sensors & Alarms"
                                            Screen.INTRUDER_SELFIE -> "Intruder Vault"
                                            Screen.MY_MOBILES -> viewModel.tr("my_mobiles")
                                            Screen.STOLEN_DEVICES -> viewModel.tr("stolen_devices")
                                            Screen.LIVE_TRACKING -> viewModel.tr("live_tracking")
                                            Screen.COMMUNITY -> viewModel.tr("community")
                                            Screen.REPORT_THEFT -> viewModel.tr("report_theft")
                                            Screen.SETTINGS -> viewModel.tr("settings")
                                            else -> viewModel.tr("app_name")
                                        },
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = YellowAccent
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            if (state.currentScreen != Screen.HOME) {
                                IconButton(
                                    onClick = { viewModel.navigateTo(Screen.HOME) },
                                    modifier = Modifier.testTag("btn_back")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = YellowAccent
                                    )
                                }
                            }
                        },
                        actions = {
                            // SENSORS HUB QUICK SHORTCUT
                            if (state.currentScreen != Screen.SENSORS_HUB) {
                                IconButton(
                                    onClick = { viewModel.navigateTo(Screen.SENSORS_HUB) },
                                    modifier = Modifier.testTag("top_bar_sensors_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sensors,
                                        contentDescription = "Sensors Hub",
                                        tint = if (state.isSystemArmed) YellowAccent else Color.White
                                    )
                                }
                            }

                            // LANGUAGE PICKER ACTION
                            Box {
                                Surface(
                                    color = Color.White.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier
                                        .clickable { showLanguageMenu = true }
                                        .testTag("top_bar_language_btn")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Language,
                                            contentDescription = "Language",
                                            tint = YellowAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = state.selectedLanguage.code.uppercase(),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = YellowAccent
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showLanguageMenu,
                                    onDismissRequest = { showLanguageMenu = false }
                                ) {
                                    AppLanguage.values().forEach { lang ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "${lang.nativeName} (${lang.displayName})",
                                                    fontWeight = if (state.selectedLanguage == lang) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                viewModel.setLanguage(lang)
                                                showLanguageMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            if (state.currentScreen != Screen.SETTINGS) {
                                IconButton(
                                    onClick = { viewModel.navigateTo(Screen.SETTINGS) },
                                    modifier = Modifier.testTag("top_bar_settings_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = Color.White
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = NavyDark,
                            titleContentColor = YellowAccent
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            AnimatedContent(
                targetState = state.currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScreenTransition",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) { targetScreen ->
                when (targetScreen) {
                    Screen.SPLASH -> SplashScreen(viewModel = viewModel)
                    Screen.HOME -> HomeScreen(viewModel = viewModel)
                    Screen.SENSORS_HUB -> SensorsHubScreen(viewModel = viewModel)
                    Screen.INTRUDER_SELFIE -> IntruderSelfieScreen(viewModel = viewModel)
                    Screen.MY_MOBILES -> MyMobilesScreen(viewModel = viewModel)
                    Screen.STOLEN_DEVICES -> StolenDevicesScreen(viewModel = viewModel)
                    Screen.LIVE_TRACKING -> LiveTrackingScreen(viewModel = viewModel)
                    Screen.COMMUNITY -> CommunityScreen(viewModel = viewModel)
                    Screen.REPORT_THEFT -> ReportTheftScreen(viewModel = viewModel)
                    Screen.SETTINGS -> SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
