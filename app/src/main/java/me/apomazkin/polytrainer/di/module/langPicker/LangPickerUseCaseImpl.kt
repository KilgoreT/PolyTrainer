package me.apomazkin.polytrainer.di.module.langPicker

import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.flags.FlagProvider
import me.apomazkin.langpicker.LangPickerUseCase
import javax.inject.Inject

class LangPickerUseCaseImpl @Inject constructor(
    private val dbApi: CoreDbApi,
    private val flagProvider: FlagProvider,
) : LangPickerUseCase {
    override suspend fun getFlagRes(numericCode: Int): Int =
        flagProvider.getFlagRes(numericCode)

    override suspend fun addLang(numericCode: Int, name: String) {
        dbApi.addLangSuspend(numericCode, name)
    }
}