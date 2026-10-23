package app.premiumhefz

import android.app.Application
import app.premiumhefz.data.AppContainer

class HefzApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Fresh installs prepare the bundled Quran pages in the background right
        // from this first launch — no import step for the user.
        container.bundledPreparer.prepareIfNeeded()
    }
}
