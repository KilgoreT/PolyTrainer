package me.apomazkin.polytrainer.di.module.dictionarytab

import dagger.Binds
import dagger.Module
import me.apomazkin.dictionarytab.deps.DictionaryTabUseCase

@Module
interface DictionaryTabModule {

    @Binds
    fun bindVocabularyUseCase(impl: DictionaryTabUseCaseImpl): DictionaryTabUseCase
}