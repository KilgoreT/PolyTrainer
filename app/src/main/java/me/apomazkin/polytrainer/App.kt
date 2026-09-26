package me.apomazkin.polytrainer

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import me.apomazkin.core_db.di.CoreDbComponent
import me.apomazkin.core_db_api.entity.ReservedGroupNames
import me.apomazkin.polytrainer.di.AppComponent
import me.apomazkin.polytrainer.di.DaggerAppComponent
import me.apomazkin.polytrainer.di.DaggerAppComponent_CoreDbDependenciesComponent
import me.apomazkin.polytrainer.di.LoggerComponent
import java.util.Locale

class App : Application() {

    lateinit var appComponent: AppComponent

    override fun onCreate() {
        super.onCreate()
        val logger = LoggerComponent.create().getLogger()
        appComponent = DaggerAppComponent
            .factory()
            .create(
                appContext = this,
                logger = logger,
                coreDbProvider = DaggerAppComponent_CoreDbDependenciesComponent
                    //TODO kilg 13.05.2020 06:39 заменить билдер на фабрику
                    .builder()
                    .coreDbProvider(CoreDbComponent.init(this, logger, reservedGroupNames()))
                    .build(),
            )
        initCrashlytics()
        // Дренаж очереди навигации живёт на application-scope (Main.immediate —
        // команды дёргают NavController): гейт открывает/закрывает активити,
        // очередь и её содержимое переживают пересоздание хоста.
        appComponent.getNavigationHandler().attach(
            CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
        )
    }

    /**
     * IS493 Э3 (D12.3/Р7): резерв имён групп — строка «Все» ВСЕХ локалей
     * проекта (default values → "All", values-ru-rRU → «Все»): юзер не
     * создаст группу с именем виртуального узла ни в одной локали.
     */
    private fun reservedGroupNames(): ReservedGroupNames {
        val locales = listOf(Locale.ROOT, Locale.forLanguageTag("ru-RU"))
        val names = locales.mapTo(mutableSetOf()) { locale ->
            val configuration = Configuration(resources.configuration).apply { setLocale(locale) }
            createConfigurationContext(configuration)
                .getString(me.apomazkin.core_resources.R.string.group_all_title)
        }
        return ReservedGroupNames(values = names)
    }

    private fun initCrashlytics() {
        FirebaseApp.initializeApp(applicationContext)
        val isCrashlyticsEnable = !BuildConfig.DEBUG
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(isCrashlyticsEnable)
    }
}

val Context.appComponent: AppComponent
    get() = when (this) {
        is App -> this.appComponent
        else -> this.applicationContext.appComponent
    }