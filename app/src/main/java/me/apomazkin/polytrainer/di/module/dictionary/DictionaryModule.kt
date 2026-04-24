package me.apomazkin.polytrainer.di.module.dictionary

import dagger.Binds
import dagger.Module
import me.apomazkin.dictionary.DictionaryUseCase

@Module
interface DictionaryModule {
    @Binds
    fun bindDictionaryUseCase(impl: DictionaryUseCaseImpl): DictionaryUseCase
}
