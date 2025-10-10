package dk.skancode.skanmate.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dk.skancode.skanmate.data.model.TableSummaryModel
import dk.skancode.skanmate.data.model.TenantModel
import dk.skancode.skanmate.data.model.UserModel
import dk.skancode.skanmate.nav.NavRoute
import dk.skancode.skanmate.ui.component.LocalAuthTenant
import dk.skancode.skanmate.ui.component.LocalAuthUser
import dk.skancode.skanmate.ui.viewmodel.TableViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    user: UserModel = LocalAuthUser.current,
    tenant: TenantModel = LocalAuthTenant.current,
    tableViewModel: TableViewModel,
    navigateTable: (NavRoute.App.TableScreen) -> Unit,
    signOut: () -> Unit,
) {
    LaunchedEffect(tableViewModel) {
        tableViewModel.resetUiState()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Welcome ${user.name} from ${tenant.name}")
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                actions = {
                    IconButton(
                        onClick = signOut,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                    }
                },
            )
        }
    ) { padding ->
        val tables by tableViewModel.tableFlow.collectAsState()
        var isRefreshing by remember { mutableStateOf(false) }
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                tableViewModel.updateTableFlow { ok ->
                    isRefreshing = false
                    if (!ok) {
                        println("Could not update table flow")
                    }
                }
            },
            modifier = Modifier
                .padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 20.dp, horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),

                ) {
                itemsIndexed(tables) { i, table ->
                    TableCard(table = table) {
                        navigateTable(
                            NavRoute.App.TableScreen(tableId = table.id),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TableCard(
    modifier: Modifier = Modifier,
    table: TableSummaryModel,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    onClick: () -> Unit,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        ) {
            Text(
                text = table.name,
                style = MaterialTheme.typography.titleMedium.merge(color = LocalContentColor.current),
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = table.description ?: "No description provided",
                style = MaterialTheme.typography.bodySmall.merge(color = LocalContentColor.current.copy(alpha = .9f)),
            )
        }
    }
}

