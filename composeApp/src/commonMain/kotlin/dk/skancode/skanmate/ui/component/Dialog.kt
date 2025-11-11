package dk.skancode.skanmate.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    dismissText: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean = false,
) {
    AlertDialog(
        title ={
            Text(title)
        },
        text = {
            Text(text)
        },
        onDismissRequest = onDismiss,
        dismissButton = {
            Button(
                onClick = onDismiss,
            ) {
                Text(dismissText)
            }
        },
        confirmButton ={
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults
                    .buttonColors()
                    .copy(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(8.dp))
                }
                Text(confirmText)
            }
        },
    )
}

@Composable
fun DismissDialog(
    title: String,
    text: String? = null,
    dismissText: String,
    acceptDismissRequest: Boolean = true,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        title = {
            Text(title)
        },
        text = if(text != null){
            { Text(text) }
        } else null,
        onDismissRequest = if (acceptDismissRequest) onDismiss else ({ Unit }),
        confirmButton = {
            Button(
                onClick = onDismiss,
            ) {
                Text(dismissText)
            }
        },
    )
}

@Composable
fun ContentDialog(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    contentPadding: PaddingValues = PaddingValues(8.dp),
    dialogColors: DialogColors = DialogDefaults.defaultColors(),
    closable: Boolean = true,
    title: @Composable () -> Unit,
    description: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        Box(
            modifier = modifier
                .sizeIn(minWidth = DialogMinWidth, maxWidth = DialogMaxWidth)
                .background(color = dialogColors.containerColor, shape = MaterialTheme.shapes.small),
            propagateMinConstraints = true
        ) {
            CompositionLocalProvider(LocalContentColor provides dialogColors.contentTextColor) {
                Column {
                    Row(
                        modifier = Modifier
                            .background(color = dialogColors.headerColor, shape = RoundedCornerShape(topEnd = 8.dp, topStart = 8.dp))
                            .padding(contentPadding)
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f).wrapContentHeight(),
                        ) {
                            CompositionLocalProvider(LocalContentColor provides dialogColors.titleTextColor) {
                                title()
                            }
                            CompositionLocalProvider(LocalContentColor provides dialogColors.descriptionTextColor) {
                                description()
                            }
                        }
                        if (closable) {
                            IconButton(
                                onClick = onDismissRequest
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "CloseDialogButton"
                                )
                            }
                        }
                    }
                    HorizontalDivider()
                    content()
                }
            }
        }
    }
}

internal val DialogMinWidth = 280.dp
internal val DialogMaxWidth = 560.dp

object DialogDefaults {
    @Composable
    fun defaultColors(): DialogColors {
        return defaultColors(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surfaceContainer,
        )
    }

    @Composable
    fun defaultColors(
        containerColor: Color = MaterialTheme.colorScheme.background,
        headerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
        titleTextColor: Color = contentColorFor(headerColor),
        descriptionTextColor: Color = contentColorFor(headerColor).copy(alpha = 0.8f),
        contentTextColor: Color = contentColorFor(containerColor)
    ): DialogColors {
        return DialogColors(containerColor, headerColor, titleTextColor, descriptionTextColor, contentTextColor)
    }
}

data class DialogColors(
    val containerColor: Color,
    val headerColor: Color,
    val titleTextColor: Color,
    val descriptionTextColor: Color,
    val contentTextColor: Color,
)
