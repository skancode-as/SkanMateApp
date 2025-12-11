package dk.skancode.skanmate.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dk.skancode.skanmate.ui.viewmodel.TableViewModel
import dk.skancode.skanmate.util.reduceDefault
import org.jetbrains.compose.resources.vectorResource
import skanmate.composeapp.generated.resources.Res
import skanmate.composeapp.generated.resources.cloud_alert

@Composable
fun SkanMateBottomAppBar(
    tableViewModel: TableViewModel,
    onClick: () -> Unit,
) {
    val localData by tableViewModel.localDataFlow.collectAsState()
    val totalRowCount by derivedStateOf {
        localData.reduceDefault(0) { acc, cur -> acc + cur.rows.size }
    }
    val tableCount by derivedStateOf {
        localData.size
    }

    if (totalRowCount > 0) {
        BottomAppBar(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            PanelButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onClick,
                leftPanel = {
                    Icon(
                        imageVector = vectorResource(Res.drawable.cloud_alert),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            ) {
                Text("$totalRowCount rows stored locally in $tableCount tables")
            }
        }
    }
}