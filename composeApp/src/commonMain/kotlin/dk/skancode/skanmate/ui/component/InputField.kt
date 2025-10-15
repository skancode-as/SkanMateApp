package dk.skancode.skanmate.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutIdParentData
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.fastFirstOrNull
import dk.skancode.skanmate.LocalScanModule
import dk.skancode.skanmate.ScanModule
import dk.skancode.skanmate.util.darken
import dk.skancode.skanmate.util.keyboardVisibleAsState
import org.jetbrains.compose.resources.vectorResource
import skanmate.composeapp.generated.resources.Res
import skanmate.composeapp.generated.resources.scan_barcode
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun ScanableInputField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    scanIconOnClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = RoundedCornerShape(4.dp),
    colors: TextFieldColors = TextFieldDefaults.colors(),
    onFocusChange: (Boolean) -> Unit = {},
    scanModule: ScanModule = LocalScanModule.current,
) {
    val trailingIcon: (@Composable () -> Unit)? = if (!scanModule.isHardwareScanner()) {
        (@Composable {
            val focusManager = LocalFocusManager.current
            val isImeVisible by keyboardVisibleAsState()
            IconButton( {
                if (isImeVisible) {
                    focusManager.clearFocus()
                }
                scanIconOnClick()
                scanModule.enableScan()
            }) {
                Icon(
                    imageVector = vectorResource(Res.drawable.scan_barcode),
                    contentDescription = null,
                )
            }
        })
    } else null

    InputField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        prefix = prefix,
        suffix = suffix,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        interactionSource = interactionSource,
        shape = shape,
        colors = colors,
        onFocusChange = onFocusChange,
    )
}


@Composable
fun InputField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = RoundedCornerShape(4.dp),
    colors: TextFieldColors = TextFieldDefaults.colors(),
    onFocusChange: (Boolean) -> Unit = {},
) {
    val paddingValues = PaddingValues(16.dp)
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    // If color is not provided via the text style, use content color as a default
    val focused = interactionSource.collectIsFocusedAsState().value
    val textColor =
        textStyle.color.takeOrElse {
            colors.textColor(enabled, isError, focused)
        }
    val containerColor = colors.containerColor(enabled, isError, focused)
    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))

    CompositionLocalProvider(LocalTextSelectionColors provides colors.textSelectionColors) {
        Column(
            modifier = Modifier.wrapContentSize(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (label != null) {
                label()
            }
            BasicTextField(
                value = value,
                modifier =
                    modifier
                        .onFocusChanged {
                            val isFocused = it.isFocused
                            if (isFocused) {
                                onValueChange(value.copy(
                                    selection = TextRange(0, value.text.length)
                                ))
                            }
                            onFocusChange(isFocused)
                        }
                        .defaultMinSize(
                            minWidth = TextFieldDefaults.MinWidth,
                            minHeight = TextFieldDefaults.MinHeight
                        ),
                onValueChange = { v ->
                    onValueChange(v)
                },
                enabled = enabled,
                readOnly = readOnly,
                textStyle = mergedTextStyle,
                cursorBrush = SolidColor(colors.cursorColor(isError)),
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                interactionSource = interactionSource,
                singleLine = singleLine,
                maxLines = maxLines,
                minLines = minLines,
                decorationBox =
                    @Composable { innerTextField ->
                        val measurePolicy =
                            remember(singleLine, paddingValues) {
                                TextFieldMeasurePolicy(singleLine, paddingValues)
                            }
                        val layoutDirection = LocalLayoutDirection.current

                        Layout(
                            modifier = Modifier,
                            measurePolicy = measurePolicy,
                            content = {
                                val transformedText =
                                    remember(value, visualTransformation) {
                                        visualTransformation.filter(AnnotatedString(value.text))
                                    }
                                        .text
                                        .text
                                val inputState =
                                    when {
                                        focused -> InputPhase.Focused
                                        transformedText.isEmpty() -> InputPhase.UnfocusedEmpty
                                        else -> InputPhase.UnfocusedNotEmpty
                                    }
                                val transition =
                                    updateTransition(inputState, label = "TextFieldInputState")
                                val placeholderOpacity by
                                transition.animateFloat(
                                    label = "PlaceholderOpacity",
                                    transitionSpec = {
                                        if (InputPhase.Focused isTransitioningTo InputPhase.UnfocusedEmpty) {
                                            tween(
                                                durationMillis = PlaceholderAnimationDelayOrDuration,
                                                easing = LinearEasing
                                            )
                                        } else if (
                                            InputPhase.UnfocusedEmpty isTransitioningTo InputPhase.Focused ||
                                            InputPhase.UnfocusedNotEmpty isTransitioningTo InputPhase.UnfocusedEmpty
                                        ) {
                                            tween(
                                                durationMillis = PlaceholderAnimationDuration,
                                                delayMillis = PlaceholderAnimationDelayOrDuration,
                                                easing = LinearEasing
                                            )
                                        } else {
                                            spring()
                                        }
                                    }
                                ) {
                                    when (it) {
                                        InputPhase.Focused -> 1f
                                        InputPhase.UnfocusedEmpty -> 1f
                                        InputPhase.UnfocusedNotEmpty -> 0f
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .layoutId(ContainerId),
                                    propagateMinConstraints = true
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .border(
                                                width = Dp.Hairline,
                                                color = colors.borderColor(
                                                    enabled,
                                                    isError,
                                                    focused
                                                ),
                                                shape = shape
                                            )
                                            .background(containerColor, shape = shape)
                                            .clip(shape),
                                    )
                                }

                                if (leadingIcon != null) {
                                    val leadingIconColor =
                                        colors.leadingIconColor(enabled, isError, focused)
                                    Box(
                                        modifier = Modifier.layoutId(LeadingId).then(
                                            IconDefaultSizeModifier
                                        ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Decoration(
                                            contentColor = leadingIconColor,
                                            content = leadingIcon
                                        )
                                    }
                                }
                                if (trailingIcon != null) {
                                    val trailingIconColor =
                                        colors.trailingIconColor(enabled, isError, focused)
                                    Box(
                                        modifier = Modifier.layoutId(TrailingId).then(
                                            IconDefaultSizeModifier
                                        ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Decoration(
                                            contentColor = trailingIconColor,
                                            content = trailingIcon
                                        )
                                    }
                                }

                                val startTextFieldPadding =
                                    paddingValues.calculateStartPadding(layoutDirection)
                                val endTextFieldPadding =
                                    paddingValues.calculateEndPadding(layoutDirection)

                                val startPadding =
                                    if (leadingIcon != null) {
                                        (startTextFieldPadding - HorizontalIconPadding).coerceAtLeast(
                                            0.dp
                                        )
                                    } else {
                                        startTextFieldPadding
                                    }
                                val endPadding =
                                    if (trailingIcon != null) {
                                        (endTextFieldPadding - HorizontalIconPadding).coerceAtLeast(
                                            0.dp
                                        )
                                    } else {
                                        endTextFieldPadding
                                    }
                                val textPadding =
                                    Modifier.heightIn(min = MinTextLineHeight)
                                        .wrapContentHeight()
                                        .padding(
                                            start = if (prefix == null) startPadding else 0.dp,
                                            end = if (suffix == null) endPadding else 0.dp,
                                        )
                                val prefixSuffixAlpha by animateFloatAsState(if (focused) 1.0f else 0.0f)
                                if (prefix != null) {
                                    val prefixColor = colors.prefixColor(enabled, isError, focused)
                                    Box(
                                        Modifier.layoutId(PrefixId)
                                            .heightIn(min = MinTextLineHeight)
                                            .wrapContentHeight()
                                            .padding(
                                                start = startPadding,
                                                end = PrefixSuffixTextPadding
                                            )
                                            .graphicsLayer { alpha = prefixSuffixAlpha }
                                    ) {
                                        Decoration(
                                            contentColor = prefixColor,
                                            textStyle = MaterialTheme.typography.bodyLarge,
                                            content = prefix
                                        )
                                    }
                                }
                                if (suffix != null) {
                                    val suffixColor = colors.suffixColor(enabled, isError, focused)
                                    Box(
                                        Modifier.layoutId(SuffixId)
                                            .heightIn(min = MinTextLineHeight)
                                            .wrapContentHeight()
                                            .padding(
                                                start = PrefixSuffixTextPadding,
                                                end = endPadding
                                            )
                                            .graphicsLayer { alpha = prefixSuffixAlpha }
                                    ) {
                                        Decoration(
                                            contentColor = suffixColor,
                                            textStyle = MaterialTheme.typography.bodyLarge,
                                            content = suffix
                                        )
                                    }
                                }

                                val showPlaceholder by remember {
                                    derivedStateOf(structuralEqualityPolicy()) { placeholderOpacity > 0f }
                                }
                                if (placeholder != null && transformedText.isEmpty() && showPlaceholder) {
                                    Box(
                                        modifier = Modifier
                                            .layoutId(PlaceholderId)
                                            .then(textPadding)
                                            .graphicsLayer { alpha = placeholderOpacity },
                                    ) {
                                        val placeholderColor =
                                            colors.placeholderColor(enabled, isError, focused)
                                        Decoration(
                                            contentColor = placeholderColor,
                                            textStyle = MaterialTheme.typography.bodyLarge,
                                            content = placeholder,
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier.layoutId(TextFieldId).then(textPadding)
                                ) {
                                    innerTextField()
                                }
                            }
                        )
                    }
            )
        }
    }
}

enum class InputPhase {
    Focused,
    UnfocusedEmpty,
    UnfocusedNotEmpty,
}

@Composable
private fun Decoration(
    contentColor: Color,
    textStyle: TextStyle,
    content: @Composable () -> Unit
) {
    val mergedStyle = LocalTextStyle.current.merge(textStyle)
    CompositionLocalProvider(
        LocalContentColor provides contentColor,
        LocalTextStyle provides mergedStyle,
        content = content
    )
}

@Composable
private fun Decoration(contentColor: Color, content: @Composable () -> Unit) =
    CompositionLocalProvider(LocalContentColor provides contentColor, content = content)

fun TextFieldColors.borderColor(
    enabled: Boolean, isError: Boolean, focused: Boolean,
): Color = containerColor(enabled, isError, focused).darken(.1f)

fun TextFieldColors.containerColor(
    enabled: Boolean, isError: Boolean, focused: Boolean,
): Color = when {
    !enabled -> disabledContainerColor
    isError -> errorContainerColor
    focused -> focusedContainerColor
    else -> unfocusedContainerColor
}

fun TextFieldColors.placeholderColor(
    enabled: Boolean, isError: Boolean, focused: Boolean,
): Color = when {
    !enabled -> disabledPlaceholderColor
    isError -> errorPlaceholderColor
    focused -> focusedPlaceholderColor
    else -> unfocusedPlaceholderColor
}

fun TextFieldColors.leadingIconColor(
    enabled: Boolean, isError: Boolean, focused: Boolean,
): Color = when {
    !enabled -> disabledLeadingIconColor
    isError -> errorLeadingIconColor
    focused -> focusedLeadingIconColor
    else -> unfocusedLeadingIconColor
}

fun TextFieldColors.trailingIconColor(
    enabled: Boolean, isError: Boolean, focused: Boolean,
): Color = when {
    !enabled -> disabledTrailingIconColor
    isError -> errorTrailingIconColor
    focused -> focusedTrailingIconColor
    else -> unfocusedTrailingIconColor
}

fun TextFieldColors.suffixColor(
    enabled: Boolean, isError: Boolean, focused: Boolean,
): Color = when {
    !enabled -> disabledSuffixColor
    isError -> errorSuffixColor
    focused -> focusedSuffixColor
    else -> unfocusedSuffixColor
}

fun TextFieldColors.prefixColor(
    enabled: Boolean, isError: Boolean, focused: Boolean,
): Color = when {
    !enabled -> disabledPrefixColor
    isError -> errorPrefixColor
    focused -> focusedPrefixColor
    else -> unfocusedPrefixColor
}

fun TextFieldColors.textColor(
    enabled: Boolean, isError: Boolean, focused: Boolean,
): Color = when {
    !enabled -> disabledTextColor
    isError -> errorTextColor
    focused -> focusedTextColor
    else -> unfocusedTextColor
}

fun TextFieldColors.cursorColor(
    isError: Boolean,
): Color = when {
    isError -> errorTextColor
    else -> cursorColor
}

private class TextFieldMeasurePolicy(
    private val singleLine: Boolean,
    private val paddingValues: PaddingValues
) : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        val topPaddingValue = paddingValues.calculateTopPadding().roundToPx()
        val bottomPaddingValue = paddingValues.calculateBottomPadding().roundToPx()

        var occupiedSpaceHorizontally = 0

        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        // measure leading icon
        val leadingPlaceable =
            measurables.fastFirstOrNull { it.layoutId == LeadingId }?.measure(looseConstraints)
        occupiedSpaceHorizontally += widthOrZero(leadingPlaceable)

        // measure trailing icon
        val trailingPlaceable =
            measurables
                .fastFirstOrNull { it.layoutId == TrailingId }
                ?.measure(looseConstraints.offset(horizontal = -occupiedSpaceHorizontally))
        occupiedSpaceHorizontally += widthOrZero(trailingPlaceable)

        // measure prefix
        val prefixPlaceable =
            measurables
                .fastFirstOrNull { it.layoutId == PrefixId }
                ?.measure(looseConstraints.offset(horizontal = -occupiedSpaceHorizontally))
        occupiedSpaceHorizontally += widthOrZero(prefixPlaceable)

        // measure suffix
        val suffixPlaceable =
            measurables
                .fastFirstOrNull { it.layoutId == SuffixId }
                ?.measure(looseConstraints.offset(horizontal = -occupiedSpaceHorizontally))
        occupiedSpaceHorizontally += widthOrZero(suffixPlaceable)

        // measure input field
        val effectiveTopOffset = topPaddingValue
        val textFieldConstraints =
            constraints
                .copy(minHeight = 0)
                .offset(
                    vertical = -effectiveTopOffset - bottomPaddingValue,
                    horizontal = -occupiedSpaceHorizontally
                )
        val textFieldPlaceable =
            measurables.fastFirst { it.layoutId == TextFieldId }.measure(textFieldConstraints)

        // measure placeholder
        val placeholderConstraints = textFieldConstraints.copy(minWidth = 0)
        val placeholderPlaceable =
            measurables
                .fastFirstOrNull { it.layoutId == PlaceholderId }
                ?.measure(placeholderConstraints)

        val width =
            calculateWidth(
                leadingWidth = widthOrZero(leadingPlaceable),
                trailingWidth = widthOrZero(trailingPlaceable),
                prefixWidth = widthOrZero(prefixPlaceable),
                suffixWidth = widthOrZero(suffixPlaceable),
                textFieldWidth = textFieldPlaceable.width,
                placeholderWidth = widthOrZero(placeholderPlaceable),
                constraints = constraints,
            )

        val totalHeight =
            calculateHeight(
                textFieldHeight = textFieldPlaceable.height,
                leadingHeight = heightOrZero(leadingPlaceable),
                trailingHeight = heightOrZero(trailingPlaceable),
                prefixHeight = heightOrZero(prefixPlaceable),
                suffixHeight = heightOrZero(suffixPlaceable),
                placeholderHeight = heightOrZero(placeholderPlaceable),
                constraints = constraints,
                density = density,
                paddingValues = paddingValues,
            )
        val height = totalHeight

        val containerPlaceable =
            measurables
                .fastFirst { it.layoutId == ContainerId }
                .measure(
                    Constraints(
                        minWidth = if (width != Constraints.Infinity) width else 0,
                        maxWidth = width,
                        minHeight = if (height != Constraints.Infinity) height else 0,
                        maxHeight = height
                    )
                )

        return layout(width, totalHeight) {
            placeWithoutLabel(
                width = width,
                totalHeight = totalHeight,
                textPlaceable = textFieldPlaceable,
                placeholderPlaceable = placeholderPlaceable,
                leadingPlaceable = leadingPlaceable,
                trailingPlaceable = trailingPlaceable,
                prefixPlaceable = prefixPlaceable,
                suffixPlaceable = suffixPlaceable,
                containerPlaceable = containerPlaceable,
                singleLine = singleLine,
                density = density,
                paddingValues = paddingValues
            )
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int
    ): Int {
        return intrinsicHeight(measurables, width) { intrinsicMeasurable, w ->
            intrinsicMeasurable.maxIntrinsicHeight(w)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int
    ): Int {
        return intrinsicHeight(measurables, width) { intrinsicMeasurable, w ->
            intrinsicMeasurable.minIntrinsicHeight(w)
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int
    ): Int {
        return intrinsicWidth(measurables, height) { intrinsicMeasurable, h ->
            intrinsicMeasurable.maxIntrinsicWidth(h)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int
    ): Int {
        return intrinsicWidth(measurables, height) { intrinsicMeasurable, h ->
            intrinsicMeasurable.minIntrinsicWidth(h)
        }
    }

    private fun intrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
        intrinsicMeasurer: (IntrinsicMeasurable, Int) -> Int
    ): Int {
        val textFieldWidth =
            intrinsicMeasurer(measurables.fastFirst { it.layoutId == TextFieldId }, height)
        val trailingWidth =
            measurables
                .fastFirstOrNull { it.layoutId == TrailingId }
                ?.let { intrinsicMeasurer(it, height) } ?: 0
        val prefixWidth =
            measurables
                .fastFirstOrNull { it.layoutId == PrefixId }
                ?.let { intrinsicMeasurer(it, height) } ?: 0
        val suffixWidth =
            measurables
                .fastFirstOrNull { it.layoutId == SuffixId }
                ?.let { intrinsicMeasurer(it, height) } ?: 0
        val leadingWidth =
            measurables
                .fastFirstOrNull { it.layoutId == LeadingId }
                ?.let { intrinsicMeasurer(it, height) } ?: 0
        val placeholderWidth =
            measurables
                .fastFirstOrNull { it.layoutId == PlaceholderId }
                ?.let { intrinsicMeasurer(it, height) } ?: 0
        return calculateWidth(
            leadingWidth = leadingWidth,
            trailingWidth = trailingWidth,
            prefixWidth = prefixWidth,
            suffixWidth = suffixWidth,
            textFieldWidth = textFieldWidth,
            placeholderWidth = placeholderWidth,
            constraints = ZeroConstraints
        )
    }

    private fun IntrinsicMeasureScope.intrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
        intrinsicMeasurer: (IntrinsicMeasurable, Int) -> Int
    ): Int {
        var remainingWidth = width
        val leadingHeight =
            measurables
                .fastFirstOrNull { it.layoutId == LeadingId }
                ?.let {
                    remainingWidth =
                        remainingWidth.subtractConstraintSafely(
                            it.maxIntrinsicWidth(Constraints.Infinity)
                        )
                    intrinsicMeasurer(it, width)
                } ?: 0
        val trailingHeight =
            measurables
                .fastFirstOrNull { it.layoutId == TrailingId }
                ?.let {
                    remainingWidth =
                        remainingWidth.subtractConstraintSafely(
                            it.maxIntrinsicWidth(Constraints.Infinity)
                        )
                    intrinsicMeasurer(it, width)
                } ?: 0

        val prefixHeight =
            measurables
                .fastFirstOrNull { it.layoutId == PrefixId }
                ?.let {
                    val height = intrinsicMeasurer(it, remainingWidth)
                    remainingWidth =
                        remainingWidth.subtractConstraintSafely(
                            it.maxIntrinsicWidth(Constraints.Infinity)
                        )
                    height
                } ?: 0
        val suffixHeight =
            measurables
                .fastFirstOrNull { it.layoutId == SuffixId }
                ?.let {
                    val height = intrinsicMeasurer(it, remainingWidth)
                    remainingWidth =
                        remainingWidth.subtractConstraintSafely(
                            it.maxIntrinsicWidth(Constraints.Infinity)
                        )
                    height
                } ?: 0

        val textFieldHeight =
            intrinsicMeasurer(measurables.fastFirst { it.layoutId == TextFieldId }, remainingWidth)
        val placeholderHeight =
            measurables
                .fastFirstOrNull { it.layoutId == PlaceholderId }
                ?.let { intrinsicMeasurer(it, remainingWidth) } ?: 0

        return calculateHeight(
            textFieldHeight = textFieldHeight,
            leadingHeight = leadingHeight,
            trailingHeight = trailingHeight,
            prefixHeight = prefixHeight,
            suffixHeight = suffixHeight,
            placeholderHeight = placeholderHeight,
            constraints = ZeroConstraints,
            density = density,
            paddingValues = paddingValues
        )
    }
}

private fun Int.subtractConstraintSafely(other: Int): Int {
    if (this == Constraints.Infinity) {
        return this
    }
    return (this - other).coerceAtLeast(0)
}

private fun calculateWidth(
    leadingWidth: Int,
    trailingWidth: Int,
    prefixWidth: Int,
    suffixWidth: Int,
    textFieldWidth: Int,
    placeholderWidth: Int,
    constraints: Constraints
): Int {
    val affixTotalWidth = prefixWidth + suffixWidth
    val middleSection =
        maxOf(
            textFieldWidth + affixTotalWidth,
            placeholderWidth + affixTotalWidth,
        )
    val wrappedWidth = leadingWidth + middleSection + trailingWidth
    return max(wrappedWidth, constraints.minWidth)
}

private fun calculateHeight(
    textFieldHeight: Int,
    leadingHeight: Int,
    trailingHeight: Int,
    prefixHeight: Int,
    suffixHeight: Int,
    placeholderHeight: Int,
    constraints: Constraints,
    density: Float,
    paddingValues: PaddingValues
): Int {
    val verticalPadding =
        density *
                (paddingValues.calculateTopPadding() + paddingValues.calculateBottomPadding()).value
    // Even though the padding is defined by the developer, if there's a label, it only affects the
    // text field in the focused state. Otherwise, we use the default value.
    val actualVerticalPadding = verticalPadding

    val inputFieldHeight =
        maxOf(
            textFieldHeight,
            placeholderHeight,
            prefixHeight,
            suffixHeight,
        )

    val middleSectionHeight =
        actualVerticalPadding + inputFieldHeight

    return max(
        constraints.minHeight,
        maxOf(leadingHeight, trailingHeight, middleSectionHeight.roundToInt())
    )
}

private fun Placeable.PlacementScope.placeWithoutLabel(
    width: Int,
    totalHeight: Int,
    textPlaceable: Placeable,
    placeholderPlaceable: Placeable?,
    leadingPlaceable: Placeable?,
    trailingPlaceable: Placeable?,
    prefixPlaceable: Placeable?,
    suffixPlaceable: Placeable?,
    containerPlaceable: Placeable,
    singleLine: Boolean,
    density: Float,
    paddingValues: PaddingValues
) {
    // place container
    containerPlaceable.place(IntOffset.Zero)

    // Most elements should be positioned w.r.t the text field's "visual" height, i.e., excluding
    // the supporting text on bottom
    val height = totalHeight
    val topPadding = (paddingValues.calculateTopPadding().value * density).roundToInt()

    leadingPlaceable?.placeRelative(
        0,
        Alignment.CenterVertically.align(leadingPlaceable.height, height)
    )

    // Single line text field without label places its text components centered vertically.
    // Multiline text field without label places its text components at the top with padding.
    fun calculateVerticalPosition(placeable: Placeable): Int {
        return if (singleLine) {
            Alignment.CenterVertically.align(placeable.height, height)
        } else {
            topPadding
        }
    }

    prefixPlaceable?.placeRelative(
        widthOrZero(leadingPlaceable),
        calculateVerticalPosition(prefixPlaceable)
    )

    val textHorizontalPosition = widthOrZero(leadingPlaceable) + widthOrZero(prefixPlaceable)

    textPlaceable.placeRelative(textHorizontalPosition, calculateVerticalPosition(textPlaceable))

    placeholderPlaceable?.placeRelative(
        textHorizontalPosition,
        calculateVerticalPosition(placeholderPlaceable)
    )

    suffixPlaceable?.placeRelative(
        width - widthOrZero(trailingPlaceable) - suffixPlaceable.width,
        calculateVerticalPosition(suffixPlaceable),
    )

    trailingPlaceable?.placeRelative(
        width - trailingPlaceable.width,
        Alignment.CenterVertically.align(trailingPlaceable.height, height)
    )
}

private fun widthOrZero(placeable: Placeable?) = placeable?.width ?: 0
private fun heightOrZero(placeable: Placeable?) = placeable?.height ?: 0

internal val IntrinsicMeasurable.layoutId: Any?
    get() = (parentData as? LayoutIdParentData)?.layoutId

internal const val ContainerId = "Container"
internal const val TextFieldId = "TextField"
internal const val PlaceholderId = "Hint"
internal const val LeadingId = "Leading"
internal const val TrailingId = "Trailing"
internal const val PrefixId = "Prefix"
internal const val SuffixId = "Suffix"
internal val ZeroConstraints = Constraints(0, 0, 0, 0)

private const val PlaceholderAnimationDuration = 83
private const val PlaceholderAnimationDelayOrDuration = 67
internal val HorizontalIconPadding = 12.dp
internal val PrefixSuffixTextPadding = 2.dp
internal val MinTextLineHeight = 24.dp

internal val IconDefaultSizeModifier = Modifier.defaultMinSize(48.dp, 48.dp)
