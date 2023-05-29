package moe.tarsin.coroutines

import kotlinx.coroutines.runInterruptible
import java.io.InterruptedIOException
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

// See https://github.com/Kotlin/kotlinx.coroutines/issues/3551
suspend inline fun <T> runInterruptibleOkio(
    context: CoroutineContext = EmptyCoroutineContext,
    crossinline block: () -> T,
): T = runInterruptible(context) {
    try {
        block()
    } catch (e: InterruptedIOException) {
        if (Thread.currentThread().isInterrupted) {
            // Coroutine cancelled
            throw InterruptedException().initCause(e)
        } else {
            // AsyncTimeout reached
            throw e
        }
    }
}
