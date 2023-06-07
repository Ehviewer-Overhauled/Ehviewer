package com.hippo.ehviewer.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.reflect.KMutableProperty0

val <T> KMutableProperty0<T>.observed: MutableState<T>
    @Composable
    get() = remember {
        val mutableState = mutableStateOf(this.get())
        object : MutableState<T> by mutableState {
            override var value: T
                get() = mutableState.value
                set(value) {
                    this@observed.set(value)
                    mutableState.value = value
                }
        }
    }

val <T> MutableState<T>.rememberedAccessor: KMutableProperty0<T>
    @Composable
    get() = remember { object { var value by this@rememberedAccessor }::value }
