package me.apomazkin.polytrainer.di.module.quizchat

import dagger.Binds
import dagger.Module
import me.apomazkin.quiz.chat.deps.QuizChatUseCase

@Module
interface QuizChatModule {
    
    @Binds
    fun bindQuizTabUseCase(impl: QuizChatUseCaseImpl): QuizChatUseCase
}