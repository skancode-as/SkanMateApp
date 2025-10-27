package dk.skancode.skanmate.util

import androidx.annotation.FloatRange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorModel
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.max
import kotlin.math.min
import kotlin.String
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@Composable
fun<T> StateFlow<List<T>>.find(predicate: (T) -> Boolean): T? {
    return this.collectAsState().value.find(predicate)
}

fun Color.darken(@FloatRange(from = 0.0, 1.0) factor: Float): Color {
    return this.add(
        red = -factor,
        green = -factor,
        blue = -factor,
    )
}

fun Color.add(red: Float = 0f, green: Float = 0f, blue: Float = 0f): Color {
    val colorSpace = this.colorSpace
    val (redRange, greenRange, blueRange) = when {
        colorSpace.model == ColorModel.Rgb -> {
            Triple(
                (colorSpace.getMaxValue(0) - colorSpace.getMinValue(0)),
                (colorSpace.getMaxValue(1) - colorSpace.getMinValue(1)),
                (colorSpace.getMaxValue(2) - colorSpace.getMinValue(2)),
            )
        }

        (colorSpace.isSrgb) -> {
            Triple(
                (colorSpace.getMaxValue(1) - colorSpace.getMinValue(1)),
                (colorSpace.getMaxValue(2) - colorSpace.getMinValue(2)),
                (colorSpace.getMaxValue(3) - colorSpace.getMinValue(3)),
            )
        }

        else -> Triple(0f, 0f, 0f)
    }
    val (redAddition, greenAddition, blueAddition) = Triple(
        redRange * red, greenRange * green, blueRange * blue
    )

    return this.copy(
        red = this.red + redAddition,
        green = this.green + greenAddition,
        blue = this.blue + blueAddition,
    )
}

/** Checks equality between floating point numbers, with a built in epsilon. For inequality see [Float.notEqual]. */
infix fun Float.equal(b: Float): Boolean {
    return this in (b - epsilon .. b + epsilon)
}
/** Checks inequality between floating point numbers, with a built in epsilon. For equality see [Float.notEqual]. */
infix fun Float.notEqual(b: Float): Boolean {
    return !this.equal(b)
}

/** Checks equality between floating point numbers, with a built in epsilon. For inequality see [Double.notEqual]. */
infix fun Double.equal(b: Double): Boolean {
    return this in (b - epsilon .. b + epsilon)
}
/** Checks inequality between floating point numbers, with a built in epsilon. For equality see [Double.notEqual]. */
infix fun Double.notEqual(b: Double): Boolean {
    return !this.equal(b)
}

private const val epsilon = 0.001


fun Float.clamp(minValue: Float, maxValue: Float): Float {
    return min(
        maxValue,
        max(minValue, this)
    )
}

fun Double.clamp(minValue: Double, maxValue: Double): Double {
    return min(
        maxValue,
        max(minValue, this)
    )
}

fun Int.clamp(minValue: Int, maxValue: Int): Int {
    return min(
        maxValue,
        max(minValue, this)
    )
}

fun Float.toOneDecimalString(): String {
    var tmp = this * 10

    tmp = tmp.roundToInt().toFloat() / 10

    return tmp.toString()
}

fun Double.toOneDecimalString(): String {
    var tmp = this * 10

    tmp = tmp.roundToLong().toDouble() / 10

    return tmp.toString()
}

inline fun unreachable(): Nothing = throw UnreachableException()
inline fun unreachable(reason: String): Nothing = throw UnreachableException(message = "An unreachable statement has been reached: $reason")

class UnreachableException(override val message: String? = "An unreachable statement has been reached"): Exception()