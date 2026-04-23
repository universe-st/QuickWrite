package com.universe_st.quickwriter.presentation

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.universe_st.quickwriter.presentation.ui.screens.SplashScreen
import kotlinx.coroutines.delay
import timber.log.Timber

@Composable
fun QuickWriterApp() {
    var isReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        Timber.tag("QuickWriterApp").d("LaunchedEffect start")
        delay(1000)
        isReady = true
        Timber.tag("QuickWriterApp").d("LaunchedEffect set isReady=true")
    }

    Crossfade(targetState = isReady, label = "splash") { ready ->
        if (!ready) {
            SplashScreen()
        } else {
            MainScreen()
        }
    }
}
