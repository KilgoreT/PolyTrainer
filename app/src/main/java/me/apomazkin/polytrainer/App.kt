package me.apomazkin.polytrainer

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import me.apomazkin.core_db.di.CoreDbComponent
import me.apomazkin.polytrainer.di.DaggerMainComponent
import me.apomazkin.polytrainer.di.DaggerMainComponent_MainDependenciesComponent
import me.apomazkin.polytrainer.di.MainComponent

class App : Application() {

    init {
        instance = this
    }

    override fun onCreate() {
        super.onCreate()
        initCrashlytics()
    }

    private fun initCrashlytics() {
        FirebaseApp.initializeApp(applicationContext)
        val isCrashlyticsEnable = !BuildConfig.DEBUG
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(isCrashlyticsEnable)
        Log.d("###", ">>>> Enable Crashlytics: $isCrashlyticsEnable")
    }

    companion object {
        lateinit var instance: App
        lateinit var appComponent: MainComponent

        fun getComponent(): MainComponent {
            if (!::appComponent.isInitialized) {
                appComponent = DaggerMainComponent
                    .factory()
                    .create(
                        DaggerMainComponent_MainDependenciesComponent
                            //TODO kilg 13.05.2020 06:39 заменить билдер на фабрику
                            .builder()
                            .coreDbProvider(CoreDbComponent.get(instance))
                            .build()
                    )
            }
            return appComponent
        }
    }
}