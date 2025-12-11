package dk.skancode.skanmate.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import dk.skancode.skanmate.nav.NavRoute
import dk.skancode.skanmate.ui.component.CustomButtonElevation
import dk.skancode.skanmate.ui.component.IconButton
import dk.skancode.skanmate.ui.component.SkanMateBottomAppBar
import dk.skancode.skanmate.ui.component.SkanMateTopAppBar
import dk.skancode.skanmate.ui.viewmodel.TableViewModel
import dk.skancode.skanmate.util.HapticKind
import dk.skancode.skanmate.util.InternalStringResource
import dk.skancode.skanmate.util.rememberHaptic
import dk.skancode.skanmate.util.snackbar.UserMessageServiceImpl
import org.jetbrains.compose.resources.stringResource
import skanmate.composeapp.generated.resources.Res
import skanmate.composeapp.generated.resources.main_screen_could_not_update_tables
import skanmate.composeapp.generated.resources.main_screen_title
import skanmate.composeapp.generated.resources.table_no_description

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    tableViewModel: TableViewModel,
    navigateTable: (NavRoute.App.TableScreen) -> Unit,
    navigateSyncPage: () -> Unit,
    signOut: () -> Unit,
) {
    val errorHaptic = rememberHaptic(HapticKind.Error)

    LaunchedEffect(tableViewModel) {
        tableViewModel.resetUiState()
    }

    Scaffold(
        topBar = {
            SkanMateTopAppBar(
                title = {
                    Text(stringResource(Res.string.main_screen_title))
                },
                actions = {
                    IconButton(
                        onClick = signOut,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LocalContentColor.current),
                        elevation = CustomButtonElevation.None,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
        bottomBar = {
            SkanMateBottomAppBar(
                tableViewModel = tableViewModel,
                onClick = navigateSyncPage,
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
                        UserMessageServiceImpl.displayError(
                            message = InternalStringResource(Res.string.main_screen_could_not_update_tables)
                        )
                        errorHaptic.start()
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
                items(tables) { table ->
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
        onClick = onClick,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
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
                text = if(table.description.isNullOrBlank()) stringResource(Res.string.table_no_description) else table.description,
                style = MaterialTheme.typography.bodySmall.merge(color = LocalContentColor.current.copy(alpha = .8f)),
            )
        }
    }
}

