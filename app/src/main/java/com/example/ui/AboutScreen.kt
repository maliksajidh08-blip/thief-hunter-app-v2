package com.example.ui

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.Screen
import com.example.ui.theme.NavyCardDark
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.YellowAccent

@Composable
fun AboutScreen(
    viewModel: ThiefHunterViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun sendEmail(email: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$email")
                putExtra(Intent.EXTRA_SUBJECT, "Support: Thief Hunter Guard (v1.0.0)")
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Top Back row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(Screen.SETTINGS) },
                modifier = Modifier.testTag("btn_about_back")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = YellowAccent
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "About Thief Hunter Guard",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // 1. BRAND HERO CARD
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NavyCardDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, YellowAccent.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(YellowAccent, Color(0xFFC79100))
                            )
                        )
                        .border(2.5.dp, Color.White, CircleShape)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_thief_hunter_logo),
                        contentDescription = "Thief Hunter Guard Logo",
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Thief Hunter Guard",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Text(
                    text = "Smart Anti-Theft AI Security Suite",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = YellowAccent
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    color = SafeGreen.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SafeGreen.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "VERSION 1.0.0 (PLAY STORE READY)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SafeGreen,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. PLAY STORE & SPECIFICATIONS CARD
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Application Specifications",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                SpecItemRow("App Name", "Thief Hunter Guard")
                SpecItemRow("Package Name", "com.sajid.thiefhunterguard")
                SpecItemRow("Version Code / Name", "1 / 1.0.0")
                SpecItemRow("Target SDK", "Android 14 (API 34)")
                SpecItemRow("Minimum SDK", "Android 7.0 (API 24)")
                SpecItemRow("Category", "Tools / Security & Anti-Theft")
                SpecItemRow("Content Rating", "Everyone (PEGI 3 / ESRB Everyone)")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. OFFICIAL LINKS (PRIVACY POLICY, TERMS, WEBSITE, SUPPORT)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Legal, Privacy & Support",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // PRIVACY POLICY LINK
                LinkRow(
                    icon = Icons.Default.Policy,
                    title = "Privacy Policy",
                    subtitle = "https://maliksajidh08-blip.github.io/thief-hunter-website/privacy.html",
                    onClick = { openUrl("https://maliksajidh08-blip.github.io/thief-hunter-website/privacy.html") },
                    testTag = "link_privacy_policy"
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // TERMS OF SERVICE LINK
                LinkRow(
                    icon = Icons.Default.VerifiedUser,
                    title = "Terms of Service",
                    subtitle = "https://maliksajidh08-blip.github.io/thief-hunter-website/terms.html",
                    onClick = { openUrl("https://maliksajidh08-blip.github.io/thief-hunter-website/terms.html") },
                    testTag = "link_terms_service"
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // OFFICIAL WEBSITE
                LinkRow(
                    icon = Icons.Default.Language,
                    title = "Official Website",
                    subtitle = "https://maliksajidh08-blip.github.io/thief-hunter-website/",
                    onClick = { openUrl("https://maliksajidh08-blip.github.io/thief-hunter-website/") },
                    testTag = "link_website"
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // SUPPORT EMAIL
                LinkRow(
                    icon = Icons.Default.Email,
                    title = "Developer & User Support",
                    subtitle = "sajidhr905@gmail.com",
                    onClick = { sendEmail("sajidhr905@gmail.com") },
                    testTag = "link_support_email"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. PERMISSIONS & GOOGLE PLAY POLICY COMPLIANCE
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NavyCardDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = YellowAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Permissions & Security Standards",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "• Camera: Used strictly for on-device intruder photo captures when incorrect PIN is entered or phone is snatched.\n" +
                            "• Location (GPS): Transmits coordinates to owner via SMS & Network beacon when theft is detected.\n" +
                            "• Sensors (Proximity & Accelerometer): Enables Pocket Guard, Public Charger Guard, and Stillness Detection.\n" +
                            "• SMS Alert: Dispatches emergency coordinates to backup numbers without requiring internet.\n" +
                            "• Foreground Service: Continuous anti-theft monitoring in background.",
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun SpecItemRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun LinkRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(NavyPrimary.copy(alpha = 0.12f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NavyPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = "Open",
            tint = YellowAccent,
            modifier = Modifier.size(16.dp)
        )
    }
}
