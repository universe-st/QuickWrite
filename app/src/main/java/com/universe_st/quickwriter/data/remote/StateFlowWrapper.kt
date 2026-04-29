package com.universe_st.quickwriter.data.remote

import kotlinx.coroutines.flow.StateFlow

class StateFlowWrapper<T>(private val flow: StateFlow<T>) {
    val value: T get() = flow.value
}
