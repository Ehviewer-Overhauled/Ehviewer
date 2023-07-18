package moe.tarsin.coroutines

inline fun <T> MutableIterable<T>.filterInPlaceInline(predicate: (T) -> Boolean, predicateResultToRemove: Boolean): Boolean {
    var result = false
    with(iterator()) {
        while (hasNext())
            if (predicate(next()) == predicateResultToRemove) {
                remove()
                result = true
            }
    }
    return result
}

private suspend inline fun <T> MutableList<T>.filterInPlaceInline(predicate: (T) -> Boolean, predicateResultToRemove: Boolean): Boolean {
    if (this !is RandomAccess) {
        return (this as MutableIterable<T>).filterInPlaceInline(predicate, predicateResultToRemove)
    }

    var writeIndex = 0
    for (readIndex in 0..lastIndex) {
        val element = this[readIndex]
        if (predicate(element) == predicateResultToRemove) {
            continue
        }

        if (writeIndex != readIndex) {
            this[writeIndex] = element
        }

        writeIndex++
    }
    return if (writeIndex < size) {
        for (removeIndex in lastIndex downTo writeIndex)
            removeAt(removeIndex)
        true
    } else {
        false
    }
}

inline fun <T> MutableIterable<T>.removeAllSuspend(predicate: (T) -> Boolean): Boolean = filterInPlaceInline(predicate, true)
