package me.apomazkin.polytrainer.di.module.wordCard

import dagger.Binds
import dagger.Module
import me.apomazkin.wordcard.deps.WordCardUseCase

@Module
interface WordCardModule {

    @Binds
    fun bindWordCardUseCase(impl: WordCardUseCaseImpl): WordCardUseCase
}