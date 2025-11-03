package dk.skancode.skanmate.util

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

interface InternalStringResource {
    val resource: StringResource
    val args: List<Any>
}

fun InternalStringResource(
    resource: StringResource,
    args: List<Any> = emptyList(),
): InternalStringResource = InternalStringResourceImpl(resource, args)

private data class InternalStringResourceImpl(
    override val resource: StringResource,
    override val args: List<Any> = emptyList(),
): InternalStringResource
    private suspend fun expandArgs(args: List<Any>): Array<Any> {
        return args.map {
            when(it) {
                is StringResource -> getString(it)
                is InternalStringResource -> it.string()
                else -> it
            }
        }.toTypedArray()
    }

    suspend fun InternalStringResource.string(): String {
        return getString(resource, *expandArgs(args))
    }

    @Composable
    private fun composeExpandArgs(args: List<Any>): Array<Any> {
        return args.map {
            when(it) {
                is StringResource -> stringResource(it)
                is InternalStringResource -> it.composeString()
                else -> it
            }
        }.toTypedArray()
    }
    @Composable
    fun InternalStringResource.composeString(): String {
        return stringResource(resource, *composeExpandArgs(args))
    }
