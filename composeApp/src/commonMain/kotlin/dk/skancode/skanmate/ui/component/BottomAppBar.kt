package dk.skancode.skanmate.ui.component

import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dk.skancode.skanmate.ui.viewmodel.TableViewModel
import dk.skancode.skanmate.util.reduceDefault
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.vectorResource
import skanmate.composeapp.generated.resources.Res
import skanmate.composeapp.generated.resources.bottom_app_bar_title
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

    if (totalRowCount > 0) {
        BottomAppBar(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            contentPadding = PaddingValues(0.dp)
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
                Text(
                    text = pluralStringResource(
                        Res.plurals.bottom_app_bar_title,
                        totalRowCount,
                        totalRowCount,
                    ),
                )
            }
        }
    }
}