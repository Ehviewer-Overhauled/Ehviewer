package com.hippo.ehviewer.ui.tools

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

class StateMapViewModel : ViewModel() {
    val states: MutableMap<Int, ArrayDeque<Any>> = mutableMapOf()
}

@Composable
fun <T : Any> rememberInVM(
    vararg inputs: Any?,
    init: ViewModel.() -> T,
): T {
    val vm: StateMapViewModel = viewModel()
    val key = currentCompositeKeyHash
    val value = remember(*inputs) {
        val states = vm.states[key] ?: ArrayDeque<Any>().also { vm.states[key] = it }
        @Suppress("UNCHECKED_CAST")
        states.removeFirstOrNull() as T? ?: init(vm)
    }
    val valueState = rememberUpdatedState(value)
    DisposableEffect(key) {
        onDispose {
            vm.states[key]?.addFirst(valueState.value)
        }
    }
    return value
}
