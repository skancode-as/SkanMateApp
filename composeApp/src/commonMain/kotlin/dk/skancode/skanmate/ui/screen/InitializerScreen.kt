package dk.skancode.skanmate.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dk.skancode.skanmate.nav.NavRoute
import dk.skancode.skanmate.ui.viewmodel.InitializerViewModel

@Composable
fun InitializerScreen(
    viewModel: InitializerViewModel,
    navigate: (NavRoute) -> Unit,
) {
    val isInitialized by viewModel.isInitialized.collectAsState()
    val initializationResult by viewModel.initializationResult

    LaunchedEffect(isInitialized, initializationResult) {
        if (isInitialized && initializationResult != null) {
            val route: NavRoute = when {
                initializationResult?.user == null || initializationResult?.tenant == null -> NavRoute.AuthScreen
                else -> NavRoute.App.MainScreen
            }

            navigate(route)
        }
    }

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            SkanMateAppLoadingIndicator()
        }
    }
}

@Composable
fun SkanMateAppLoadingIndicator() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()

        Spacer(modifier = Modifier.size(8.dp))

        Text("SkanMate")
    }
}