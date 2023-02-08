package moe.tarsin.coroutines

import kotlinx.coroutines.CancellationException

inline fun <reified T : Throwable> Result<*>.except(): Result<*> =
    onFailure { if (it is T) throw it }

inline fun <R> runSuspendCatching(block: () -> R): Result<R> {
    return runCatching(block).apply { except<CancellationException>() }
}

inline fun <T, R> T.runSuspendCatching(block: T.() -> R): Result<R> {
    return runCatching(block).apply { except<CancellationException>() }
}
