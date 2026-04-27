package com.universe_st.quickwriter

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.universe_st.quickwriter.presentation.QuickWriterApp
import com.universe_st.quickwriter.ui.theme.QuickWriterTheme
import com.universe_st.quickwriter.util.LocaleHelper
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context?) {
        val languageCode = try {
            val appContainer = (newBase?.applicationContext as? QuickWriteApplication)?.appContainer
            appContainer?.let {
                runBlocking { it.settingsUseCase.getLanguage() }
            } ?: LocaleHelper.CODE_SYSTEM
        } catch (_: Exception) {
            LocaleHelper.CODE_SYSTEM
        }
        val wrappedContext = newBase?.let { LocaleHelper.wrapContextForLocale(it, languageCode) }
        super.attachBaseContext(wrappedContext ?: newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        applySavedLanguage()

        enableEdgeToEdge()
        setContent {
            QuickWriterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    QuickWriterApp()
                }
            }
        }
    }

    private fun applySavedLanguage() {
        val appContainer = (application as QuickWriteApplication).appContainer
        val languageCode = runBlocking {
            appContainer.settingsUseCase.getLanguage()
        }
        LocaleHelper.applyLocale(this, languageCode)
    }
}
