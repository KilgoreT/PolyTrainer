package me.apomazkin.polytrainer.di.module.componentsmanager

import dagger.Binds
import dagger.Module
import me.apomazkin.components_manager.deps.ComponentsManagerUseCase

@Module
interface ComponentsManagerModule {

    @Binds
    fun bindComponentsManagerUseCase(impl: ComponentsManagerUseCaseImpl): ComponentsManagerUseCase
}
