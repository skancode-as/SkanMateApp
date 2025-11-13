package dk.skancode.skanmate.util.snackbar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf

val LocalSnackbarHost: ProvidableCompositionLocal<@Composable () -> Unit> = compositionLocalOf { {} }