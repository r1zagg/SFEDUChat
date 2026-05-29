package fm.mrc.sfeduchat

import android.app.Application
import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import fm.mrc.sfeduchat.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import java.util.Locale

class SFEDUChatApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(applyRussianLocale(base))
    }

    override fun onCreate() {
        super.onCreate()
        Locale.setDefault(Locale.forLanguageTag("ru"))
        startKoin {
            androidContext(this@SFEDUChatApplication)
            modules(appModule)
        }
    }

    private fun applyRussianLocale(context: Context): Context {
        val locale = Locale.forLanguageTag("ru")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java)
                ?.applicationLocales = LocaleList.forLanguageTags("ru")
        }
        val config = context.resources.configuration
        config.setLocale(locale)
        config.setLocales(LocaleList(locale))
        return context.createConfigurationContext(config)
    }
}
