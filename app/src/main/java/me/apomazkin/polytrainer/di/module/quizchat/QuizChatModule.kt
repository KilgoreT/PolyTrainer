package me.apomazkin.polytrainer.di.module.quizchat

import dagger.Binds
import dagger.Module
import me.apomazkin.quiz.chat.deps.QuizChatUseCase
import me.apomazkin.quiz.chat.quiz.QuizGame
import me.apomazkin.quiz.chat.quiz.QuizGameImpl

@Module
interface QuizChatModule {

    @Binds
    fun bindQuizTabUseCase(impl: QuizChatUseCaseImpl): QuizChatUseCase

    @Binds
    fun bindQuizGame(impl: QuizGameImpl): QuizGame
}
