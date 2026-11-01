package com.edith.assistant

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.edith.assistant.data.NovaAppState
import com.edith.assistant.forge.ForgeController
import com.edith.assistant.license.AccessKeyManager
import com.edith.assistant.ui.fx.ListeningEdgeGlow
import com.edith.assistant.ui.screens.splash.SplashSession
import com.edith.assistant.ui.screens.splash.edithSplash3D
import com.edith.assistant.ui.theme.EdithTheme

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_OPEN_FORGE = "com.edith.assistant.EXTRA_OPEN_FORGE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleForgeIntent(intent)

        setContent {
            val appState = remember { NovaAppState(this@MainActivity, lifecycleScope) }
            var showSplash by remember { mutableStateOf(!SplashSession.played) }

            EdithTheme(
                mode = appState.themeMode,
                reducedMotion = appState.reducedMotion
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    NovaApp(appState = appState)

                    if (showSplash) {
                        edithSplash3D(
                            onFinish = {
                                showSplash = false
                            }
                        )
                    }

                    ListeningEdgeGlow()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AccessKeyManager.liveCheck(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleForgeIntent(intent)
    }

    private fun handleForgeIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_FORGE, false) == true) {
            ForgeController.requestOpen()
        }
    }
}
