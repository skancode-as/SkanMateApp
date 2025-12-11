package dk.skancode.skanmate.ui.component


import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dk.skancode.skanmate.util.ProvideContentColorTextStyle
import dk.skancode.skanmate.util.borderColorFor
import dk.skancode.skanmate.util.darken
import dk.skancode.skanmate.util.snackbar.LocalSnackbarManager

data class SizeValues(
    val minHeight: Dp = Dp.Unspecified,
    val maxHeight: Dp = Dp.Unspecified,
    val minWidth: Dp = Dp.Unspecified,
    val maxWidth: Dp = Dp.Unspecified,
) {
    constructor(min: Dp = Dp.Unspecified, max: Dp = Dp.Unspecified) : this(
        minHeight = min,
        maxHeight = max,
        minWidth = min,
        maxWidth = max
    )

    fun heightValues(): Pair<Dp, Dp> {
        return minHeight to maxHeight
    }

    fun widthValues(): Pair<Dp, Dp> {
        return minWidth to minHeight
    }
}

@Composable
fun rememberInteractionSource(): MutableInteractionSource {
    return remember { MutableInteractionSource() }
}

@Composable
fun PanelButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    elevation: CustomButtonElevation = CustomButtonElevation(),
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.12f).compositeOver(MaterialTheme.colorScheme.surface),
        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f).compositeOver(MaterialTheme.colorScheme.surface),
    ),
    textStyle: TextStyle = MaterialTheme.typography.labelLarge,
    enabled: Boolean = true,
    enabledWhenSnackbarActive: Boolean = false,
    loading: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(8.dp),
    interactionSource: InteractionSource = rememberInteractionSource(),
    shape: Shape = RoundedCornerShape(4.dp),
    heightValues: SizeValues = SizeValues(minHeight = 36.dp, maxHeight = 64.dp),
    leftPanel: (@Composable BoxScope.() -> Unit)? = null,
    leftPanelColor: Color = if(enabled) MaterialTheme.colorScheme.surfaceContainerLow else colors.disabledContainerColor.darken(0.15f),
    rightPanel: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val containerColor = if (enabled) colors.containerColor else colors.disabledContainerColor
    val contentColor = if (enabled) colors.contentColor else colors.disabledContentColor
    val (minHeight, maxHeight) = key(heightValues) { heightValues.heightValues() }

    Button(
        modifier = modifier
            .border(width = Dp.Hairline, color = borderColorFor(containerColor), shape = shape),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = verticalAlignment,
        onClick = onClick,
        elevation = elevation,
        colors = colors,
        enabled = enabled && !loading,
        enabledWhenSnackbarActive = enabledWhenSnackbarActive,
        interactionSource = interactionSource,
        shape = shape,
        sizeValues = heightValues,
        contentPadding = PaddingValues(0.dp),
    ) {
        ProvideContentColorTextStyle(
            contentColor = contentColor,
            textStyle = textStyle,
        ) {
            if (leftPanel != null) {
                Box(
                    modifier = Modifier
                        .background(color = leftPanelColor)
                        .padding(contentPadding)
                        .heightIn(min = minHeight, max = maxHeight)
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedContent(
                        targetState = loading
                    ) { targetIsLoading ->
                        if (targetIsLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(contentPadding)
                                    .sizeIn(minHeight = minHeight, maxHeight = maxHeight),
                                color = LocalContentColor.current,
                                trackColor = containerColor,
                            )
                        } else {
                            leftPanel()
                        }
                    }
                }
                VerticalDivider(
                    color = borderColorFor(leftPanelColor),
                )
            }
            Box(
                modifier = Modifier
                    .padding(contentPadding)
                    .weight(1f),
                contentAlignment = Alignment.CenterStart,
            ) {
                content()
            }
            if (rightPanel != null) {
                Box(
                    modifier = Modifier
                        .padding(contentPadding)
                        .heightIn(min = minHeight, max = maxHeight)
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedContent(
                        targetState = loading && leftPanel == null
                    ) { targetIsLoading ->
                        if (targetIsLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(24.dp),
                                color = LocalContentColor.current,
                                trackColor = containerColor,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            rightPanel()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TextButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    elevation: CustomButtonElevation = CustomButtonElevation(),
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        disabledContainerColor = MaterialTheme.colorScheme.background.darken(.1f),
    ),
    enabled: Boolean = true,
    enabledWhenSnackbarActive: Boolean = false,
    interactionSource: InteractionSource = rememberInteractionSource(),
    shape: Shape = RoundedCornerShape(4.dp),
    contentPadding: PaddingValues = PaddingValues(12.dp, 8.dp),
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        modifier = modifier.width(IntrinsicSize.Max),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = verticalAlignment,
        onClick = onClick,
        elevation = elevation,
        colors = colors,
        enabled = enabled,
        enabledWhenSnackbarActive = enabledWhenSnackbarActive,
        interactionSource = interactionSource,
        shape = shape,
        sizeValues = SizeValues(min = Dp.Unspecified, max = Dp.Infinity),
        contentPadding = contentPadding,
        content = content,
    )
}

@Composable
fun FullWidthButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    elevation: CustomButtonElevation = CustomButtonElevation(),
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        disabledContainerColor = MaterialTheme.colorScheme.background.darken(.1f),
    ),
    enabled: Boolean = true,
    enabledWhenSnackbarActive: Boolean = false,
    interactionSource: InteractionSource = rememberInteractionSource(),
    shape: Shape = RoundedCornerShape(4.dp),
    heightValues: SizeValues = SizeValues(minHeight = 36.dp, maxHeight = 64.dp),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = verticalAlignment,
        onClick = onClick,
        elevation = elevation,
        colors = colors,
        enabled = enabled,
        enabledWhenSnackbarActive = enabledWhenSnackbarActive,
        interactionSource = interactionSource,
        shape = shape,
        sizeValues = heightValues.copy(minWidth = Dp.Unspecified, maxWidth = Dp.Unspecified),
        contentPadding = contentPadding,
        content = content,
    )
}

@Composable
fun IconButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    elevation: CustomButtonElevation = CustomButtonElevation(),
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        disabledContainerColor = MaterialTheme.colorScheme.background.darken(.1f),
    ),
    enabled: Boolean = true,
    enabledWhenSnackbarActive: Boolean = false,
    interactionSource: InteractionSource = rememberInteractionSource(),
    shape: Shape = RoundedCornerShape(8.dp),
    sizeValues: SizeValues = SizeValues(min = 48.dp, max = 64.dp),
    contentPadding: PaddingValues = PaddingValues(12.dp),
    aspectRatio: Float = 1f,
    content: @Composable BoxScope.() -> Unit,
) {
    val (minHeight, maxHeight) = key(sizeValues) { sizeValues.heightValues() }
    val (minWidth, maxWidth) = key(sizeValues) { sizeValues.widthValues() }
    Button(
        modifier = modifier
            .requiredSizeIn(
                minWidth = minWidth,
                minHeight = minHeight,
                maxWidth = maxWidth,
                maxHeight = maxHeight
            ),
        onClick = onClick,
        elevation = elevation,
        colors = colors,
        enabled = enabled,
        enabledWhenSnackbarActive = enabledWhenSnackbarActive,
        interactionSource = interactionSource,
        shape = shape,
        sizeValues = sizeValues,
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(contentPadding)
                .aspectRatio(aspectRatio),
            contentAlignment = Alignment.Center,
            propagateMinConstraints = true,
        ) {
            content()
        }
    }
}

@Composable
fun Button(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    elevation: CustomButtonElevation = CustomButtonElevation(),
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        disabledContainerColor = MaterialTheme.colorScheme.background.darken(.1f),
    ),
    enabled: Boolean = true,
    enabledWhenSnackbarActive: Boolean,
    interactionSource: InteractionSource = rememberInteractionSource(),
    shape: Shape = RoundedCornerShape(4.dp),
    sizeValues: SizeValues = SizeValues(minHeight = 36.dp, maxHeight = 64.dp),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val snackbarManager = LocalSnackbarManager.current
    val enabled = enabled && (enabledWhenSnackbarActive || !snackbarManager.errorSnackbarActive)

    val shadowElevation by elevation.shadowElevation(enabled, interactionSource)
    val containerColor =
        if (enabled) colors.containerColor else colors.disabledContainerColor.takeOrElse { colors.containerColor.darken(.1f) }
    val contentColor =
        if (enabled) colors.contentColor else colors.disabledContentColor.takeOrElse { colors.contentColor }
    val (minHeight, maxHeight) = key(sizeValues) { sizeValues.heightValues() }
    val (minWidth, maxWidth) = key(sizeValues) { sizeValues.widthValues() }

    Box(
        modifier = modifier
            .sizeIn(
                minWidth = minWidth,
                minHeight = minHeight,
                maxWidth = maxWidth,
                maxHeight = maxHeight,
            )
            .shadow(elevation = shadowElevation, shape = shape)
            .background(color = containerColor, shape = shape)
            .clip(shape = shape)
            .clickable(enabled) {
                onClick()
            },

        propagateMinConstraints = true,
    ) {
        Row(
            modifier = Modifier
                .padding(contentPadding),
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = verticalAlignment,
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                this@Row.content()
            }
        }
    }
}

internal interface ButtonElevation {
    @Composable
    fun shadowElevation(
        enabled: Boolean,
        interactionSource: InteractionSource
    ): State<Dp>
}

data class CustomButtonElevation(
    private val defaultElevation: Dp = ElevationTokens.Level2,
    private val pressedElevation: Dp = ElevationTokens.Level1,
    private val focusedElevation: Dp = ElevationTokens.Level2,
    private val hoveredElevation: Dp = ElevationTokens.Level3,
    private val disabledElevation: Dp = ElevationTokens.Level1,
) : ButtonElevation {
    companion object {
        val None: CustomButtonElevation = CustomButtonElevation(
            all = 0.dp
        )
    }

    constructor(all: Dp) : this(
        defaultElevation = all,
        pressedElevation = all,
        focusedElevation = all,
        hoveredElevation = all,
        disabledElevation = all,
    )

    @Composable
    override fun shadowElevation(
        enabled: Boolean,
        interactionSource: InteractionSource
    ): State<Dp> {
        val interactions = remember { mutableStateListOf<Interaction>() }
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is HoverInteraction.Enter -> {
                        interactions.add(interaction)
                    }

                    is HoverInteraction.Exit -> {
                        interactions.remove(interaction.enter)
                    }

                    is FocusInteraction.Focus -> {
                        interactions.add(interaction)
                    }

                    is FocusInteraction.Unfocus -> {
                        interactions.remove(interaction.focus)
                    }

                    is PressInteraction.Press -> {
                        interactions.add(interaction)
                    }

                    is PressInteraction.Release -> {
                        interactions.remove(interaction.press)
                    }

                    is PressInteraction.Cancel -> {
                        interactions.remove(interaction.press)
                    }
                }
            }
        }

        val interaction = interactions.lastOrNull()

        val target =
            if (!enabled) {
                disabledElevation
            } else {
                when (interaction) {
                    is PressInteraction.Press -> pressedElevation
                    is HoverInteraction.Enter -> hoveredElevation
                    is FocusInteraction.Focus -> focusedElevation
                    else -> defaultElevation
                }
            }

        val animatable = remember { Animatable(target, Dp.VectorConverter) }

        LaunchedEffect(target) {
            if (animatable.targetValue != target) {
                if (!enabled) {
                    // No transition when moving to a disabled state
                    animatable.snapTo(target)
                } else {
                    val lastInteraction =
                        when (animatable.targetValue) {
                            pressedElevation -> PressInteraction.Press(Offset.Zero)
                            hoveredElevation -> HoverInteraction.Enter()
                            focusedElevation -> FocusInteraction.Focus()
                            else -> null
                        }
                    animatable.animateElevation(
                        from = lastInteraction,
                        to = interaction,
                        target = target
                    )
                }
            }
        }

        return animatable.asState()
    }

}

internal object ElevationDefaults {
    fun incomingAnimationSpecForInteraction(interaction: Interaction): AnimationSpec<Dp>? {
        return when (interaction) {
            is PressInteraction.Press -> DefaultIncomingSpec
            is DragInteraction.Start -> DefaultIncomingSpec
            is HoverInteraction.Enter -> DefaultIncomingSpec
            is FocusInteraction.Focus -> DefaultIncomingSpec
            else -> null
        }
    }

    /**
     * Returns the [AnimationSpec]s used when animating elevation away from [interaction], to the
     * default state. If [interaction] is unknown, then returns `null`.
     *
     * @param interaction the [Interaction] that is being animated away from
     */
    fun outgoingAnimationSpecForInteraction(interaction: Interaction): AnimationSpec<Dp>? {
        return when (interaction) {
            is PressInteraction.Press -> DefaultOutgoingSpec
            is DragInteraction.Start -> DefaultOutgoingSpec
            is HoverInteraction.Enter -> HoveredOutgoingSpec
            is FocusInteraction.Focus -> DefaultOutgoingSpec
            else -> null
        }
    }

    private val OutgoingSpecEasing: Easing = CubicBezierEasing(0.40f, 0.00f, 0.60f, 1.00f)

    private val DefaultIncomingSpec =
        TweenSpec<Dp>(durationMillis = 120, easing = FastOutSlowInEasing)

    private val DefaultOutgoingSpec =
        TweenSpec<Dp>(durationMillis = 150, easing = OutgoingSpecEasing)

    private val HoveredOutgoingSpec =
        TweenSpec<Dp>(durationMillis = 120, easing = OutgoingSpecEasing)
}

internal suspend fun Animatable<Dp, *>.animateElevation(
    target: Dp,
    from: Interaction? = null,
    to: Interaction? = null
) {
    val spec =
        when {
            // Moving to a new state
            to != null -> ElevationDefaults.incomingAnimationSpecForInteraction(to)
            // Moving to default, from a previous state
            from != null -> ElevationDefaults.outgoingAnimationSpecForInteraction(from)
            // Loading the initial state, or moving back to the baseline state from a disabled /
            // unknown state, so just snap to the final value.
            else -> null
        }
    if (spec != null) animateTo(target, spec) else snapTo(target)
}


internal object ElevationTokens {
    val Level1 = 1.0.dp
    val Level2 = 3.0.dp
    val Level3 = 6.0.dp
}
