package moe.tarsin.coroutines

import kotlinx.coroutines.CancellationException

inline fun <R> runCatchingCancellationTransparent(block: () -> R): Result<R> {
    return try {
        Result.success(block())
    } catch (e: Throwable) {
        if (e is CancellationException) throw e
        Result.failure(e)
    }
}

inline fun <T, R> T.runCatchingCancellationTransparent(block: T.() -> R): Result<R> {
    return try {
        Result.success(block())
    } catch (e: Throwable) {
        if (e is CancellationException) throw e
        Result.failure(e)
    }
}
