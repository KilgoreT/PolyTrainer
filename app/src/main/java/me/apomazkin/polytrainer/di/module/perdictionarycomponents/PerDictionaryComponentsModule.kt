package me.apomazkin.polytrainer.di.module.perdictionarycomponents

import dagger.Binds
import dagger.Module
import me.apomazkin.per_dictionary_components.deps.PerDictionaryComponentsUseCase

@Module
interface PerDictionaryComponentsModule {

    @Binds
    fun bindPerDictionaryComponentsUseCase(
        impl: PerDictionaryComponentsUseCaseImpl
    ): PerDictionaryComponentsUseCase
}
