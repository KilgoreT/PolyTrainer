package me.apomazkin.polytrainer.di.module.wordCard

import dagger.Binds
import dagger.Module
import dagger.Provides
import me.apomazkin.polytrainer.BuildConfig
import me.apomazkin.polytrainer.di.module.candidates.ServerAccess
import me.apomazkin.wordcard.deps.WordCardUseCase

@Module
interface WordCardModule {

    @Binds
    fun bindWordCardUseCase(impl: WordCardUseCaseImpl): WordCardUseCase

    companion object {
        /** IS533: ключ сервера попадает в сборку из `local.properties`; без него кнопки в карточке нет (WCL-08). */
        @Provides
        fun provideServerAccess(): ServerAccess = ServerAccess(hasKey = BuildConfig.LEXEME_ACCESS_KEY.isNotBlank())
    }
}