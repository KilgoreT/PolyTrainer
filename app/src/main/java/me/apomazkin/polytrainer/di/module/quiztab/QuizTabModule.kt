package me.apomazkin.polytrainer.di.module.quiztab

import dagger.Binds
import dagger.Module
import me.apomazkin.quiztab.deps.QuizTabUseCase

@Module
interface QuizTabModule {
    
    @Binds
    fun bindQuizTabUseCase(impl: QuizTabUseCaseImpl): QuizTabUseCase
}