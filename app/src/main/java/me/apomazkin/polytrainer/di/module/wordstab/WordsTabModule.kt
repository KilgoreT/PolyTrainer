package me.apomazkin.polytrainer.di.module.dictionarytab

import dagger.Binds
import dagger.Module
import me.apomazkin.wordstab.deps.WordsTabUseCase

@Module
interface WordsTabModule {

    @Binds
    fun bindVocabularyUseCase(impl: WordsTabUseCaseImpl): WordsTabUseCase
}