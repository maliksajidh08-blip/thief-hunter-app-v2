package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Intruder Vault Screen: Live Camera Preview, ML Kit Face Verification & Captured Intruder Evidence.
 */
@Composable
fun IntruderVaultScreen(
    viewModel: ThiefHunterViewModel,
    modifier: Modifier = Modifier
) {
    IntruderSelfieScreen(
        viewModel = viewModel,
        modifier = modifier
    )
}
