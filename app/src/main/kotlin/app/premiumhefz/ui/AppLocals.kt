package app.premiumhefz.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.premiumhefz.data.AppContainer

val LocalContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer was not provided") }

/** Creates a ViewModel whose dependencies come from the app's container. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(crossinline create: (AppContainer) -> VM): VM {
    val container = LocalContainer.current
    return viewModel(factory = viewModelFactory { initializer { create(container) } })
}
