package me.apomazkin.createdictionary.logic

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.apomazkin.createdictionary.CreateDictionaryUseCase
import me.apomazkin.createdictionary.DictionaryData
import me.apomazkin.createdictionary.entity.PresetDictionaryUi
import me.apomazkin.createdictionary.toDictionaryNameRes
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
    private val createDictionaryUseCase: CreateDictionaryUseCase,
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
                                flagRes = createDictionaryUseCase.getFlagRes(it.numericCode),
                                countryNumericCode = it.numericCode,
                                dictionaryNameRes = it.numericCode.toDictionaryNameRes()
                            )
                        }
                    )
                }
            }
            is DatasourceEffect.SaveDictionaryList -> {
                withContext(Dispatchers.IO) {
                    createDictionaryUseCase.addDictionary(
                        eff.numericCode,
                        eff.dictionaryName
                    )
                    createDictionaryUseCase.saveCurrentDictionary(eff.numericCode)
                }
                Msg.Close
            }
            null -> Msg.Empty
        }.let(consumer)
    }
}
