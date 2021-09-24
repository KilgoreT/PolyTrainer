package me.apomazkin.core_interactor.di

import android.content.Context
import dagger.BindsInstance
import dagger.Component
import me.apomazkin.core_interactor.CoreInteractorApi
import me.apomazkin.core_interactor.di.module.RepositoryModule
import me.apomazkin.core_interactor.di.module.ScenarioModule
import me.apomazkin.core_interactor.di.module.UseCaseModule
import javax.inject.Singleton


@Singleton
@Component(modules = [RepositoryModule::class, UseCaseModule::class, ScenarioModule::class])
interface CoreInteractorComponent : CoreInteractorApi {

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance context: Context,
        ): CoreInteractorComponent
    }


    companion object {

        private var instance: CoreInteractorComponent? = null

        fun getOrInit(
            context: Context,
        ): CoreInteractorComponent {
            if (instance == null) {
                instance = DaggerCoreInteractorComponent.factory()
                    .create(context)
            }
            return instance ?: throw RuntimeException("njkjhkj")
        }

        fun get() = instance ?: throw RuntimeException("No FeatureAddWordComponent")

    }

    @Singleton
    @Component(dependencies = [CoreInteractorApi::class])
    interface CoreInteractorDependencyComponent : CoreInteractorDependency

}