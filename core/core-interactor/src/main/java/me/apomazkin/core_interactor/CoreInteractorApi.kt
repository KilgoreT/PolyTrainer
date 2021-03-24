package me.apomazkin.core_interactor

import me.apomazkin.core_interactor.useCase.writeQuiz.WriteQuizScenario

interface CoreInteractorApi {
    fun writeQuizScenario(): WriteQuizScenario
}