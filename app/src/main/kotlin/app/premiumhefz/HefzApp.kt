package app.premiumhefz

import android.app.Application
import app.premiumhefz.data.AppContainer

class HefzApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
