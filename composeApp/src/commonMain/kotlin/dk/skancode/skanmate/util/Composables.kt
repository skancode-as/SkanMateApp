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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.DpSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

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
}

@Composable
fun animator(
    initialValue: Float,
    targetValue: Float,
    animationSpec: AnimationSpec<Float> = SpringSpec(visibilityThreshold = Spring.DefaultDisplacementThreshold),
    coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
): Animator<Float> {
    val animator = remember {
        object : Animator<Float> {
            val targetChannel: Channel<Float> = Channel(capacity = Channel.BUFFERED)
            val animatable: Animatable<Float, AnimationVector1D> = Animatable(initialValue)
            val internalValue = mutableStateOf(initialValue)
            override val value: State<Float>
                get() = internalValue

            override fun start() {
                animateTo(targetValue)
            }

            override fun animateTo(value: Float) {
                coroutineScope.launch {
                    targetChannel.send(value)
                }
            }
        }
    }

    LaunchedEffect(animator.targetChannel) {
        for (target in animator.targetChannel) {
            if (animator.animatable.isRunning) animator.animatable.stop()
            launch {
                animator.animatable.animateTo(
                    targetValue = target,
                    animationSpec = animationSpec,
                ) {
                    animator.internalValue.value = value
                }
            }
        }
    }

    return animator
}