package dk.skancode.skanmate.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dk.skancode.skanmate.util.OnSuccess
import dk.skancode.skanmate.util.Success
import dk.skancode.skanmate.util.animator
import dk.skancode.skanmate.util.snackbar.LocalSnackbarManager

@Composable
fun Switch(
    modifier: Modifier = Modifier,
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    trackShape: Shape = RoundedCornerShape(4.dp),
    thumbShape: Shape = RoundedCornerShape(4.dp),
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
    style: SwitchStyle = SwitchStyle(),
    interactionSource: MutableInteractionSource? = null,
) {
    val snackbarManager = LocalSnackbarManager.current
    val enabled = enabled && !snackbarManager.errorSnackbarActive

    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    val toggleableModifier =
        if (onCheckedChange != null) {
            Modifier.minimumInteractiveComponentSize()
                .toggleable(
                    value = checked,
                    onValueChange = onCheckedChange,
                    enabled = enabled,
                    role = Role.Switch,
                    interactionSource = interactionSource,
                    indication = null
                )
        } else {
            Modifier
        }

    SwitchImpl(
        modifier = modifier
            .then(toggleableModifier)
            .wrapContentSize(Alignment.Center)
            .requiredSize(width = TrackWidth, height = TrackHeight),
        checked = checked,
        enabled = enabled,
        colors = colors,
        trackShape = trackShape,
        thumbShape = thumbShape,
        style = style,
        thumbContent = thumbContent,
        interactionSource = interactionSource,
    )
}

@Composable
private fun SwitchImpl(
    modifier: Modifier,
    checked: Boolean,
    enabled: Boolean,
    colors: SwitchColors,
    trackShape: Shape,
    thumbShape: Shape,
    style: SwitchStyle,
    thumbContent: (@Composable () -> Unit)?,
    interactionSource: MutableInteractionSource,
) {
    val containerColor = colors.containerColor(checked, enabled)
    val thumbColor = colors.thumbColor(checked, enabled)
    val borderColor = colors.borderColor(checked, enabled)
    val thumbAnimator = animator(
        initialValue = if (checked) (style.trackWidth.value - style.thumbWidth.value) else 0f,
        targetValue = if (checked) 0f else (style.trackWidth.value - style.thumbWidth.value),
    )

    LaunchedEffect(thumbAnimator, interactionSource) {
        var target = if (checked) 1f else 0f
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Release -> {
                    target = if (target == 1f) 0f else 1f
                    thumbAnimator.animateTo(target * (style.trackWidth.value - style.thumbWidth.value))
                }
            }
        }
    }

    Box(
        modifier = modifier
            .border(style.trackOutlineWidth, borderColor, trackShape)
            .background(containerColor, trackShape),
    ) {
        val animatedThumbOffset by thumbAnimator.value
        Box(
            modifier = Modifier
                .size(width = style.thumbWidth, style.thumbHeight)
                .aspectRatio(style.thumbAspectRatio)
                .offset(x = animatedThumbOffset.dp)
                .background(thumbColor, thumbShape),
            contentAlignment = Alignment.Center,
        ) {
            if (thumbContent != null) {
                thumbContent()
            }
        }
    }
}

data class SwitchStyle(
    val trackWidth: Dp = TrackWidth,
    val trackHeight: Dp = TrackHeight,
    val trackOutlineWidth: Dp = TrackOutlineWidth,
    val thumbWidth: Dp = trackWidth / 2,
    val thumbHeight: Dp = trackHeight,
    val thumbAspectRatio: Float = thumbWidth / thumbHeight,
)

data class SwitchColors(
    val containerColor: Color = Color.Unspecified,
    val checkedContainerColor: Color = Color.Unspecified,
    val disabledContainerColor: Color = Color.Unspecified,
    val disabledCheckedContainerColor: Color = Color.Unspecified,
    val thumbColor: Color = Color.Unspecified,
    val checkedThumbColor: Color = Color.Unspecified,
    val disabledThumbColor: Color = Color.Unspecified,
    val disabledCheckedThumbColor: Color = Color.Unspecified,
) {
    fun containerColor(checked: Boolean, enabled: Boolean): Color =
        if (enabled) {
            if (checked) checkedContainerColor else containerColor
        } else {
            if (checked) disabledCheckedContainerColor else disabledContainerColor
        }

    fun thumbColor(checked: Boolean, enabled: Boolean): Color =
        if (enabled) {
            if (checked) checkedThumbColor else thumbColor
        } else {
            if (checked) disabledCheckedThumbColor else disabledThumbColor
        }

    fun borderColor(checked: Boolean, enabled: Boolean): Color =
        containerColor(checked, enabled)
}

class SwitchDefaults {
    companion object {
        @Composable
        fun colors(): SwitchColors {
            return SwitchColors(
                containerColor = MaterialTheme.colorScheme.error,
                checkedContainerColor = Color.Success,
                disabledContainerColor =
                    MaterialTheme.colorScheme.error
                        .copy(alpha = 0.12f)
                        .compositeOver(
                            MaterialTheme.colorScheme.background
                        ),
                disabledCheckedContainerColor =
                    Color.Success
                        .copy(alpha = 0.12f)
                        .compositeOver(
                            MaterialTheme.colorScheme.background
                        ),

                thumbColor = MaterialTheme.colorScheme.onError,
                checkedThumbColor = Color.OnSuccess,
                disabledThumbColor =
                    MaterialTheme.colorScheme.onError
                        .copy(alpha = 0.12f)
                        .compositeOver(
                            MaterialTheme.colorScheme.background
                        ),
                disabledCheckedThumbColor =
                    Color.OnSuccess
                        .copy(alpha = 0.12f)
                        .compositeOver(
                            MaterialTheme.colorScheme.background
                        ),
            )
        }
    }

}

private val TrackOutlineWidth = 2.0.dp
private val TrackWidth = 52.0.dp
private val TrackHeight = 26.0.dp

