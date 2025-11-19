package dk.skancode.skanmate.ui.component.fab

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dk.skancode.skanmate.ui.component.Button
import dk.skancode.skanmate.ui.component.CustomButtonElevation
import dk.skancode.skanmate.ui.component.SizeValues
import dk.skancode.skanmate.ui.component.rememberInteractionSource
import dk.skancode.skanmate.util.ProvideContentColorTextStyle
import dk.skancode.skanmate.util.darken

@Composable
fun FloatingActionButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    elevation: CustomButtonElevation = CustomButtonElevation(),
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        disabledContainerColor = MaterialTheme.colorScheme.background.darken(.1f),
    ),
    enabled: Boolean = true,
    expanded: Boolean = true,
    enabledWhenSnackbarActive: Boolean = false,
    interactionSource: InteractionSource = rememberInteractionSource(),
    shape: Shape = RoundedCornerShape(4.dp),
    icon: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Button(
        modifier = modifier
            .minimumInteractiveComponentSize(),
        onClick = onClick,
        elevation = elevation,
        colors = colors,
        enabled = enabled,
        enabledWhenSnackbarActive = enabledWhenSnackbarActive,
        interactionSource = interactionSource,
        shape = shape,
        sizeValues = SizeValues(min = Dp.Unspecified, max = Dp.Infinity),
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 16.dp),
    ) {
        val startPadding = if (expanded) FabStartIconPadding else 0.dp
        val endPadding = if (expanded) FabTextPadding else 0.dp

        ProvideContentColorTextStyle(
            contentColor = colors.contentColor,
            textStyle = MaterialTheme.typography.labelLarge,
        ) {
            Row(
                modifier =
                    Modifier.sizeIn(
                        minWidth =
                            if (expanded) FabMinimumWidth
                            else FabContainerWidth
                    )
                        .padding(start = startPadding, end = endPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center
            ) {
                icon()
                AnimatedVisibility(
                    visible = expanded,
                    enter = FabExpandAnimation,
                    exit = FabCollapseAnimation,
                ) {
                    Row(Modifier.clearAndSetSemantics {}) {
                        Spacer(Modifier.width(FabEndIconPadding))
                        content()
                    }
                }
            }
        }
    }
}

private val FabStartIconPadding = 16.dp
private val FabEndIconPadding = 12.dp

private val FabContainerWidth = 56.dp

private val FabTextPadding = 20.dp

private val FabMinimumWidth = 80.dp
private val FabCollapseAnimation =
    fadeOut(
        animationSpec =
            tween(
                durationMillis = MotionTokens.DurationShort2.toInt(),
                easing = MotionTokens.EasingLinearCubicBezier,
            )
    ) +
            shrinkHorizontally(
                animationSpec =
                    tween(
                        durationMillis = MotionTokens.DurationLong2.toInt(),
                        easing = MotionTokens.EasingEmphasizedCubicBezier,
                    ),
                shrinkTowards = Alignment.Start,
            )

private val FabExpandAnimation =
    fadeIn(
        animationSpec =
            tween(
                durationMillis = MotionTokens.DurationShort4.toInt(),
                delayMillis = MotionTokens.DurationShort2.toInt(),
                easing = MotionTokens.EasingLinearCubicBezier,
            ),
    ) +
            expandHorizontally(
                animationSpec =
                    tween(
                        durationMillis = MotionTokens.DurationLong2.toInt(),
                        easing = MotionTokens.EasingEmphasizedCubicBezier,
                    ),
                expandFrom = Alignment.Start,
            )

private object MotionTokens {
    const val DurationLong2 = 500.0
    const val DurationShort2 = 100.0
    const val DurationShort4 = 200.0
    val EasingEmphasizedCubicBezier = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val EasingLinearCubicBezier = CubicBezierEasing(0.0f, 0.0f, 1.0f, 1.0f)
}
