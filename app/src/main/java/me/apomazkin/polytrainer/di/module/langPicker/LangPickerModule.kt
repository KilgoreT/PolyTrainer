package me.apomazkin.polytrainer.di.module.langPicker

import dagger.Binds
import dagger.Module
import me.apomazkin.langpicker.LangPickerUseCase

@Module
interface LangPickerModule {
    @Binds
    fun bindLangPickerUseCase(impl: LangPickerUseCaseImpl): LangPickerUseCase
}