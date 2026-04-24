package me.apomazkin.polytrainer.di.module.quiztab

import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.flags.FlagProvider
import me.apomazkin.prefs.PrefsProvider
import me.apomazkin.quiztab.deps.QuizTabUseCase
import javax.inject.Inject

class QuizTabUseCaseImpl @Inject constructor(
    private val dictionaryApi: CoreDbApi.DictionaryApi,
    private val prefsProvider: PrefsProvider,
    private val flagProvider: FlagProvider,
) : QuizTabUseCase {
}