package me.apomazkin.dictionary.logic

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.apomazkin.dictionary.DictionaryUseCase
import me.apomazkin.dictionary.DictionaryData
import me.apomazkin.dictionary.entity.PresetDictionaryUi
import me.apomazkin.dictionary.toDictionaryNameRes
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateEffectHandler

/**
 * Effect
 */
internal sealed interface DatasourceEffect : Effect {

    /**
     * Effect to get available dictionaries.
     */
    object LoadDictionaryList : DatasourceEffect

    /**
     * Effect to save selected dictionary.
     */
    data class SaveDictionaryList(
        val numericCode: Int,
        val dictionaryName: String
    ) : DatasourceEffect
}

/**
 * EffectHandler for datastore calls.
 */
internal class DatasourceEffectHandler(
    private val dictionaryUseCase: DictionaryUseCase,
) : MateEffectHandler<Msg, Effect> {

    override suspend fun runEffect(
        effect: Effect,
        consumer: (Msg) -> Unit
    ) {
        Log.d("##MATE##", "RunEffect: $effect")
        return when (val eff = effect as? DatasourceEffect) {
            is DatasourceEffect.LoadDictionaryList -> {
                withContext(Dispatchers.IO) {
                    Msg.ShowDictionaryList(
                        DictionaryData.dictionaryList.map {
                            PresetDictionaryUi(
                                flagRes = dictionaryUseCase.getFlagRes(it.numericCode),
                                countryNumericCode = it.numericCode,
                                dictionaryNameRes = it.numericCode.toDictionaryNameRes()
                            )
                        }
                    )
                }
            }
            is DatasourceEffect.SaveDictionaryList -> {
                withContext(Dispatchers.IO) {
                    dictionaryUseCase.addDictionary(
                        eff.numericCode,
                        eff.dictionaryName
                    )
                    dictionaryUseCase.saveCurrentDictionary(eff.numericCode)
                }
                Msg.Close
            }
            null -> Msg.Empty
        }.let(consumer)
    }
}
