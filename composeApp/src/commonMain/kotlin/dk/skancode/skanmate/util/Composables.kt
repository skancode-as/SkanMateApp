package dk.skancode.skanmate.util

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

sealed class BorderSide {
    abstract fun start(size: Size): Offset
    abstract fun end(size: Size): Offset

    data object Left: BorderSide() {
        override fun start(size: Size): Offset {
            return Offset(
                x = 0f,
                y = 0f,
            )
        }

        override fun end(size: Size): Offset {
            return Offset(
                x = 0f,
                y = size.height,
            )
        }
    }
}

fun Modifier.singleSideBorder(
    width: Dp,
    color: Color,
    side: BorderSide,
): Modifier {
    return this then drawBehind {
        val strokeWidth = width.toPx()

        drawLine(
            color = color,
            start = side.start(size),
            end = side.end(size),
            strokeWidth = strokeWidth,
        )
    }
}

@Composable
inline fun <reified T> rememberMutableStateOf(value: T): MutableState<T> {
    return remember { mutableStateOf(value) }
}

@Composable
inline fun <reified T> rememberStateOf(value: T): State<T> {
    return remember { mutableStateOf(value) }
}

@Composable
fun borderColorFor(color: Color, mix: Float = 0.3f): Color {
    return MaterialTheme.colorScheme.outline.copy(mix).compositeOver(color)
}

@Composable
fun measureText(text: String, style: TextStyle = LocalTextStyle.current): DpSize {
    val textMeasurer = rememberTextMeasurer()
    val sizeInPixels = textMeasurer.measure(text, style).size
    return with(LocalDensity.current) {
        DpSize(
            width = sizeInPixels.width.toDp(),
            height = sizeInPixels.height.toDp()
        )
    }
}

@Composable
fun ProvideContentColorTextStyle(
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
fun keyboardVisibleAsState(): State<Boolean> {
    val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    return rememberUpdatedState(isImeVisible)
}

interface Animator<T : Number> {
    val value: State<T>
    fun start()
    fun animateTo(value: T)
    fun animateToAndReset(value: T)
}

private data class AnimationRequest<T: Number>(val target: T, val resetOnEnd: Boolean)

@Composable
fun animator(
    initialValue: Float,
    targetValue: Float,
    animationSpec: AnimationSpec<Float> = SpringSpec(visibilityThreshold = Spring.DefaultDisplacementThreshold),
    coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
): Animator<Float> {
    val animator = remember {
        object : Animator<Float> {
            val requestChannel: Channel<AnimationRequest<Float>> = Channel(capacity = Channel.BUFFERED)
            val animatable: Animatable<Float, AnimationVector1D> = Animatable(initialValue)
            val internalValue = mutableStateOf(initialValue)
            override val value: State<Float>
                get() = internalValue

            override fun start() {
                animateToAndReset(targetValue)
            }

            override fun animateTo(value: Float) {
                coroutineScope.launch {
                    requestChannel.send(AnimationRequest(value, resetOnEnd = false))
                }
            }

            override fun animateToAndReset(value: Float) {
                coroutineScope.launch {
                    requestChannel.send(AnimationRequest(value, resetOnEnd = true))
                }
            }
        }
    }

    LaunchedEffect(animator.requestChannel) {
        for (request in animator.requestChannel) {
            if (animator.animatable.isRunning) animator.animatable.stop()
            launch {
                animator.animatable.animateTo(
                    targetValue = request.target,
                    animationSpec = animationSpec,
                ) {
                    animator.internalValue.value = value
                }
                if (request.resetOnEnd) {
                    animator.animatable.snapTo(initialValue)
                }
            }
        }
    }

    return animator
}