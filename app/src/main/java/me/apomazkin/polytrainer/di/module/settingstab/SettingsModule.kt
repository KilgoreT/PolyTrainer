package me.apomazkin.polytrainer.di.module.settingstab

import dagger.Binds
import dagger.Module
import me.apomazkin.settingstab.deps.SettingsTabUseCase

@Module
interface SettingsModule {
    
    @Binds
    fun bindSettingsTabUseCase(impl: SettingsTabUseCaseImpl): SettingsTabUseCase
}