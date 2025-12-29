package dk.skancode.skanmate.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dk.skancode.skanmate.util.rememberMutableStateOf

@Composable
fun SkanMateDropdown(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(4.dp),
    expandedState: MutableState<Boolean> = rememberMutableStateOf(false),
    enabled: Boolean = true,
    buttonSizeValues: SizeValues = SizeValues(min = 48.dp, max = 64.dp),
    aspectRatio: Float = 0.5f,
    icon: @Composable () -> Unit = { DefaultDropdownIcon() },
    items: @Composable (collapse: () -> Unit) -> Unit,
) {
    var expanded: Boolean by expandedState

    Box {
        IconButton(
            modifier = modifier,
            onClick = {
                expanded = !expanded
            },
            enabled = enabled,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = LocalContentColor.current),
            elevation = CustomButtonElevation.None,
            sizeValues = buttonSizeValues,
            contentPadding = contentPadding,
            aspectRatio = aspectRatio
        ) {
            icon()
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items { expanded = false }
        }
    }
}

@Composable
fun DefaultDropdownIcon() {
    Icon(
        imageVector = Icons.Default.MoreVert,
        contentDescription = null,
    )
}