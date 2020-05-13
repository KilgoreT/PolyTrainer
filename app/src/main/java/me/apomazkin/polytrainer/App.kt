package me.apomazkin.polytrainer

import android.app.Application
import me.apomazkin.core_db.di.CoreDbComponent
import me.apomazkin.polytrainer.di.DaggerMainComponent
import me.apomazkin.polytrainer.di.DaggerMainComponent_MainDependenciesComponent
import me.apomazkin.polytrainer.di.MainComponent

class App : Application() {

    init {
        instance = this
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