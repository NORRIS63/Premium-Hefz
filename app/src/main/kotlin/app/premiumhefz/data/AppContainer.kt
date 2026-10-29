package app.premiumhefz.data

import android.content.Context
import app.premiumhefz.domain.PageManifest
import app.premiumhefz.domain.QuranIndex

/** Hand-rolled dependency container: one instance per process, created lazily. */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext

    val database: HefzDatabase by lazy { HefzDatabase.build(appContext) }
    val settings: SettingsStore by lazy { SettingsStore(appContext) }
    val quranIndex: QuranIndex by lazy { QuranIndexLoader.load(appContext) }
    val pagePack: PagePack by lazy {
        val csv = appContext.assets.open("page_manifest.csv").bufferedReader(Charsets.UTF_8).use { it.readText() }
        PagePack(appContext, PageManifest.parse(csv))
    }
    val repository: HefzRepository by lazy { HefzRepository(database, settings) }
    val hadithRepository: HadithRepository by lazy { HadithRepository(appContext) }
    val bundledPreparer: BundledPagePreparer by lazy { BundledPagePreparer(appContext) { pagePack } }
}
