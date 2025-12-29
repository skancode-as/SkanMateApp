package dk.skancode.skanmate.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp
import dk.skancode.skanmate.ui.viewmodel.LocalConnectionState
import dk.skancode.skanmate.ui.viewmodel.LocalConnectivityViewModel
import org.jetbrains.compose.resources.vectorResource
import skanmate.composeapp.generated.resources.Res
import skanmate.composeapp.generated.resources.wifi
import skanmate.composeapp.generated.resources.wifi_off

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkanMateTopAppBar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val wrappedActions: @Composable RowScope.() -> Unit = {
        actions()

        ConnectionIndicator()
    }

    TopAppBar(
        modifier = modifier,
        title = title,
        navigationIcon = navigationIcon,
        actions = wrappedActions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    )
}

@Composable
fun ConnectionIndicator() {
    val connectivityViewModel = LocalConnectivityViewModel.current
    val connection by LocalConnectionState.current
    val offlineMode by connectivityViewModel.offlineModeFlow.collectAsState()

    val imageVector = when (connection) {
        true  -> vectorResource(Res.drawable.wifi)
        false -> vectorResource(Res.drawable.wifi_off)
    }
    val containerColor = when(connection) {
        true  -> Color.Transparent
        false -> MaterialTheme.colorScheme.background
    }
    val contentColor = when(connection) {
        true  -> LocalContentColor.current
        false -> MaterialTheme.colorScheme.error.copy(alpha = 0.8f).compositeOver(Color.White)
    }

    IconButton(
        modifier = Modifier.padding(start = 10.dp, end = 16.dp),
        onClick = {
            if (offlineMode) {
                connectivityViewModel.disableOfflineMode()
            } else {
                connectivityViewModel.enableOfflineMode()
            }
        },
        colors = ButtonDefaults.outlinedButtonColors(containerColor = containerColor, contentColor = contentColor),
        elevation = CustomButtonElevation.None,
        shape = CircleShape,
        sizeValues = SizeValues(min = 36.dp, max = 52.dp),
        contentPadding = PaddingValues(6.dp),
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = "Wifi Connection icon",
        )
    }
}
