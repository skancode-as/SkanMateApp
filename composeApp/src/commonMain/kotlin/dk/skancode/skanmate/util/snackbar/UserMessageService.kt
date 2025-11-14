package dk.skancode.skanmate.util.snackbar

import androidx.compose.material3.SnackbarDuration
import dk.skancode.skanmate.util.InternalStringResource
import dk.skancode.skanmate.util.string
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

interface UserMessageService {
    fun displayMessage(
        message: InternalStringResource,
        options: MessageOptions = MessageOptions(),
    )

    fun displayError(
        message: InternalStringResource,
        options: MessageOptions = DefaultErrorOptions,
    )
}

data class MessageOptions(
    val snackbarWithAction: Boolean = false,
    val description: String? = null,
)

val DefaultErrorOptions = MessageOptions(snackbarWithAction = true)

object UserMessageServiceImpl: UserMessageService {
    lateinit var adapter: SnackbarAdapter
    lateinit var externalScope: CoroutineScope

    override fun displayMessage(message: InternalStringResource, options: MessageOptions) {
        println("UserMessageServiceImpl::displayMessage($message, $options)")
        externalScope.launch {
            val m = message.string()
            adapter.displaySnackbar(
                m,
                config = SnackbarAdapter.Config(
                    withDismissAction = options.snackbarWithAction,
                    description = options.description,
                ),
            )
        }
    }

    override fun displayError(message: InternalStringResource, options: MessageOptions) {
        println("UserMessageServiceImpl::displayError($message, $options)")
        externalScope.launch {
            val m = message.string()
            adapter.displaySnackbar(
                m,
                config = SnackbarAdapter.Config(
                    isError = true,
                    withDismissAction = options.snackbarWithAction,
                    description = options.description,
                    duration = if (options.snackbarWithAction) SnackbarDuration.Indefinite else SnackbarDuration.Long,
                ),
            )
        }
    }

}