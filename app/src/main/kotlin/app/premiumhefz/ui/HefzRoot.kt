package app.premiumhefz.ui

import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.premiumhefz.HefzApp
import app.premiumhefz.ui.splash.StartupOverlay
import app.premiumhefz.ui.theme.PremiumHefzTheme

@Composable
fun HefzRoot() {
    val context = LocalContext.current
    val container = remember { (context.applicationContext as HefzApp).container }
    val settings by container.settings.settings.collectAsStateWithLifecycle()
    val systemReducedMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }

    CompositionLocalProvider(LocalContainer provides container) {
        PremiumHefzTheme(mode = settings.theme) {
            // Saved across rotation, so the opening animation plays once per launch and never when the
            // user comes back from another screen.
            var introDone by rememberSaveable { mutableStateOf(false) }
            var welcomePlayed by rememberSaveable { mutableStateOf(false) }
            val reducedMotion = settings.reduceMotion || systemReducedMotion
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                AppNavigation(
                    showWelcome = introDone && !welcomePlayed && !reducedMotion,
                    onWelcomeStarted = { welcomePlayed = true }
                )
                if (!introDone) {
                    StartupOverlay(
                        reducedMotion = reducedMotion,
                        onFinished = { introDone = true }
                    )
                }
            }
        }
    }
}
