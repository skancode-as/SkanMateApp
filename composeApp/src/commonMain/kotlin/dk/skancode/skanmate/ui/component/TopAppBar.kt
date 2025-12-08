package dk.skancode.skanmate.ui.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dk.skancode.skanmate.ui.viewmodel.LocalConnectionState
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
        ConnectionIndicator()

        actions()
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
    val connection by LocalConnectionState.current

    val imageVector = when (connection) {
        true  -> vectorResource(Res.drawable.wifi)
        false -> vectorResource(Res.drawable.wifi_off)
    }
    val tint = when(connection) {
        true  -> LocalContentColor.current
        false -> LocalContentColor.current
    }

    Icon(
        modifier = Modifier.padding(12.dp),
        imageVector = imageVector,
        contentDescription = "Wifi Connection icon",
        tint = tint
    )
}
