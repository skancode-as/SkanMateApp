package dk.skancode.skanmate.util

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
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

interface Animator<T: Number>{
    val value: State<T>
    fun start()
}
@Composable
fun animator(
    initialValue: Float,
    targetValue: Float,
    animationSpec: AnimationSpec<Float> = SpringSpec(visibilityThreshold = Spring.DefaultDisplacementThreshold)
): Animator<Float> {
    val animator = remember {
        object : Animator<Float> {
            val isStarted = mutableStateOf(false)
            val internalValue = mutableStateOf(initialValue)
            override val value: State<Float>
                get() = internalValue

            override fun start() {
                isStarted.value = true
            }
        }
    }

    val rotationAnimatable = Animatable(initialValue)
    LaunchedEffect(animator.isStarted.value) {
        if (animator.isStarted.value && !rotationAnimatable.isRunning) {
            rotationAnimatable.animateTo(targetValue, animationSpec) {
                animator.internalValue.value = value
            }
            animator.isStarted.value = false
        }
    }

    return animator
}