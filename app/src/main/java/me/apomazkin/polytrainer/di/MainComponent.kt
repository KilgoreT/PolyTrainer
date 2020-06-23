package me.apomazkin.polytrainer.di

import dagger.Component
import me.apomazkin.core_db_api.CoreDbProvider
import me.apomazkin.polytrainer.MainActivity
import me.apomazkin.polytrainer.api.AppProvider

@Component(
    dependencies = [CoreDbProvider::class]
)
interface MainComponent : AppProvider {

    fun inject(mainActivity: MainActivity)

    @Component.Factory
    interface MainComponentFactory {
        fun create(
            coreDbProvider: CoreDbProvider
        ): MainComponent
    }

    @Component(dependencies = [CoreDbProvider::class])
    interface MainDependenciesComponent : CoreDbProvider
}
