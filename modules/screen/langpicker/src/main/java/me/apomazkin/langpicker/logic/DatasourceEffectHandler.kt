package me.apomazkin.langpicker.logic

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.apomazkin.langpicker.LangPickerUseCase
import me.apomazkin.langpicker.LanguageData
import me.apomazkin.langpicker.entity.PresetLangUi
import me.apomazkin.langpicker.toLangNameRes
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateEffectHandler

/**
 * Effect
 */
internal sealed interface DatasourceEffect : Effect {

    /**
     * Effect to get available languages.
     */
    object LoadLangList : DatasourceEffect

    /**
     * Effect to save selected language.
     */
    data class SaveLangList(
        val numericCode: Int,
        val langName: String
    ) : DatasourceEffect
}

/**
 * EffectHandler for datastore calls.
 */
internal class DatasourceEffectHandler(
    private val langPickerUseCase: LangPickerUseCase,
) : MateEffectHandler<Msg, Effect> {

    override suspend fun runEffect(
        effect: Effect,
        consumer: (Msg) -> Unit
    ) {
        Log.d("##MATE##", "RunEffect: $effect")
        return when (val eff = effect as? DatasourceEffect) {
            is DatasourceEffect.LoadLangList -> {
                withContext(Dispatchers.IO) {
                    Msg.ShowLangList(
                        LanguageData.langList.map {
                            PresetLangUi(
                                flagRes = langPickerUseCase.getFlagRes(it.numericCode),
                                countryNumericCode = it.numericCode,
                                langNameRes = it.numericCode.toLangNameRes()
                            )
                        }
                    )
                }
            }
            is DatasourceEffect.SaveLangList -> {
                withContext(Dispatchers.IO) {
                    langPickerUseCase.addLang(eff.numericCode, eff.langName)
                    langPickerUseCase.saveCurrentLang(eff.numericCode)
                }
                Msg.Close
            }
            null -> Msg.Empty
        }.let(consumer)
    }
}