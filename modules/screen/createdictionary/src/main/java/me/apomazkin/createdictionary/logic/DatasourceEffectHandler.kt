package me.apomazkin.createdictionary.logic

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.apomazkin.createdictionary.CreateDictionaryUseCase
import me.apomazkin.createdictionary.LanguageData
import me.apomazkin.createdictionary.entity.PresetLangUi
import me.apomazkin.createdictionary.toLangNameRes
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
    private val createDictionaryUseCase: CreateDictionaryUseCase,
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
                                flagRes = createDictionaryUseCase.getFlagRes(it.numericCode),
                                countryNumericCode = it.numericCode,
                                langNameRes = it.numericCode.toLangNameRes()
                            )
                        }
                    )
                }
            }
            is DatasourceEffect.SaveLangList -> {
                withContext(Dispatchers.IO) {
                    createDictionaryUseCase.addLang(eff.numericCode, eff.langName)
                    createDictionaryUseCase.saveCurrentLang(eff.numericCode)
                }
                Msg.Close
            }
            null -> Msg.Empty
        }.let(consumer)
    }
}