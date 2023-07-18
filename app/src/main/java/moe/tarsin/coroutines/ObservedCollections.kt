package moe.tarsin.coroutines

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

inline fun <T, K> Iterable<T>.groupByToObserved(keySelector: (T) -> K): LinkedHashMap<K, SnapshotStateList<T>> {
    val destination = LinkedHashMap<K, SnapshotStateList<T>>()
    for (element in this) {
        val key = keySelector(element)
        val list = destination.getOrPut(key) { mutableStateListOf() }
        list.add(element)
    }
    return destination
}
