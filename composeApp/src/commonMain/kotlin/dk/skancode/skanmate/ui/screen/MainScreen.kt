package dk.skancode.skanmate.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberTooltipState
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
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dk.skancode.skanmate.data.model.TableSummaryModel
import dk.skancode.skanmate.nav.NavRoute
import dk.skancode.skanmate.ui.component.Badge
import dk.skancode.skanmate.ui.component.CustomButtonElevation
import dk.skancode.skanmate.ui.component.IconButton
import dk.skancode.skanmate.ui.component.SkanMateBottomAppBar
import dk.skancode.skanmate.ui.component.SkanMateTopAppBar
import dk.skancode.skanmate.ui.viewmodel.LocalConnectionState
import dk.skancode.skanmate.ui.viewmodel.TableViewModel
import dk.skancode.skanmate.util.HapticKind
import dk.skancode.skanmate.util.InternalStringResource
import dk.skancode.skanmate.util.rememberHaptic
import dk.skancode.skanmate.util.snackbar.UserMessageServiceImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import skanmate.composeapp.generated.resources.Res
import skanmate.composeapp.generated.resources.main_screen_could_not_update_tables
import skanmate.composeapp.generated.resources.main_screen_title
import skanmate.composeapp.generated.resources.main_screen_not_available_offline_tooltip
import skanmate.composeapp.generated.resources.table_no_description
import skanmate.composeapp.generated.resources.wifi_off

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    tableViewModel: TableViewModel,
    navigateTable: (NavRoute.App.TableScreen) -> Unit,
    navigateSyncPage: () -> Unit,
    signOut: () -> Unit,
) {
    val errorHaptic = rememberHaptic(HapticKind.Error)

    val tables by tableViewModel.tableFlow.collectAsState()
    var isRefreshing by remember { mutableStateOf(tables.isEmpty()) }

    LaunchedEffect(tableViewModel) {
        tableViewModel.resetUiState()
        tableViewModel.updateTableFlow {
            isRefreshing = false
        }
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
        TableCardColumnBox(
            allowPullToRefresh = LocalConnectionState.current.value,
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
                items(items = tables) { table ->
                    TableCard(
                        modifier = Modifier.animateItem(),
                        table = table,
                    ) {
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
fun TableCardColumnBox(
    modifier: Modifier = Modifier,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    allowPullToRefresh: Boolean = true,
    content: @Composable () -> Unit,
) {
    if (allowPullToRefresh) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = modifier,
        ) {
            content()
        }
    } else {
        Box(modifier = modifier) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableCard(
    modifier: Modifier = Modifier,
    table: TableSummaryModel,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    onClick: () -> Unit,
) {
    val hasConnection by LocalConnectionState.current
    val enabled = hasConnection || table.availableOffline()

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
        enabled = enabled,
        onClick = onClick,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp, disabledElevation = 0.dp)
    ) {
        val scope = CoroutineScope(Dispatchers.IO)
        val tooltipState = rememberTooltipState(isPersistent = false)
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp).let {
                if (!enabled) {
                    it.clickable(interactionSource = null, indication = null) {
                        scope.launch { tooltipState.show() }
                    }
                } else it
            },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            ) {
                Text(
                    text = table.name,
                    style = MaterialTheme.typography.titleMedium.merge(color = LocalContentColor.current),
                    fontWeight = FontWeight.SemiBold,
                )
                val description: String = table.description.let { desc ->
                    if (desc.isNullOrBlank()) stringResource(Res.string.table_no_description) else desc
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.merge(
                        color = LocalContentColor.current.copy(alpha = .8f)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (!enabled) {
                TableNotAvailableIcon(
                    tooltipState = tooltipState,
                    scope = scope,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableNotAvailableIcon(
    tooltipState: TooltipState,
    scope: CoroutineScope,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            TooltipAnchorPosition.Above
        ),
        tooltip = {
            PlainTooltip(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shadowElevation = 4.dp,
            ) {
                Text(text = stringResource(Res.string.main_screen_not_available_offline_tooltip))
            }
        },
        state = tooltipState,
    ) {
        Badge(
            modifier = Modifier
                .clickable(
                    interactionSource = null,
                    indication = null,
                ) {
                    scope.launch {
                        tooltipState.show()
                    }
                },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f).compositeOver(Color.White),
            contentStyle = MaterialTheme.typography.labelMedium,
            contentPadding = PaddingValues(6.dp),
        ) {
            Icon(
                modifier = Modifier.size(18.dp),
                imageVector = vectorResource(Res.drawable.wifi_off),
                contentDescription = "TableNotAvailableIcon",
            )
        }
    }
}
