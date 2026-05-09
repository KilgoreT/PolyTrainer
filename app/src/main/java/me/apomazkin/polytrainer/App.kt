package me.apomazkin.polytrainer

import android.app.Application
import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import me.apomazkin.core_db.di.CoreDbComponent
import me.apomazkin.polytrainer.di.AppComponent
import me.apomazkin.polytrainer.di.DaggerAppComponent
import me.apomazkin.polytrainer.di.DaggerAppComponent_CoreDbDependenciesComponent
import me.apomazkin.polytrainer.di.LoggerComponent

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
                    .coreDbProvider(CoreDbComponent.init(this, logger))
                    .build(),
            )
        initCrashlytics()
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