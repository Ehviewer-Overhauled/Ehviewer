package moe.tarsin.coroutines

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

inline fun <T, reified K : Enum<K>> Iterable<T>.groupByToObserved(keySelector: (T) -> K): MutableMap<K, SnapshotStateList<T>> {
    val destination = enumValues<K>().associateWith { mutableStateListOf<T>() }
    for (element in this) {
        val key = keySelector(element)
        requireNotNull(destination[key]).add(element)
    }
    return destination as MutableMap<K, SnapshotStateList<T>>
}
