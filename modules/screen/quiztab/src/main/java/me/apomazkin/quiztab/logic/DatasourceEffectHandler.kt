package me.apomazkin.quiztab.logic

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.apomazkin.dictionarypicker.entity.DictUiEntity
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateEffectHandler
import me.apomazkin.quiztab.deps.QuizTabUseCase

/**
 * Effect
 */
internal sealed interface DatasourceEffect : Effect {
    
    /**
     * Effect to get available languages.
     */
    data object LoadDictList : DatasourceEffect
    
    /**
     * Effect to get current language.
     */
    data object LoadCurrentDict : DatasourceEffect
    
    /**
     * Effect to change current language.
     */
    data class ChangeDict(val lang: DictUiEntity) : DatasourceEffect
}

/**
 * EffectHandler for datastore calls.
 */
internal class DatasourceEffectHandler(
    private val quizTabUseCase: QuizTabUseCase,
) : MateEffectHandler<Msg, Effect> {
    
    override suspend fun runEffect(
        effect: Effect,
        consumer: (Msg) -> Unit
    ) {
        Log.d("##MATE##", "RunEffect: $effect")
        return when (val eff = effect as? DatasourceEffect) {
            is DatasourceEffect.LoadDictList -> {
                withContext(Dispatchers.IO) {
                    quizTabUseCase.getAvailableDict().let {
                        TopBarActionMsg.AvailableDict(list = it)
                    }
                }
            }
            
            is DatasourceEffect.LoadCurrentDict -> {
                withContext(Dispatchers.IO) {
                    quizTabUseCase.getCurrentDict()
                        .let { TopBarActionMsg.CurrentDict(lang = it) }
                }
            }
            
            is DatasourceEffect.ChangeDict -> {
                withContext(Dispatchers.IO) {
                    quizTabUseCase
                        .changeDict(numericCode = eff.lang.numericCode)
                        .let { TopBarActionMsg.CurrentDict(lang = eff.lang) }
                }
            }
            
            null -> Msg.Empty
        }.let(consumer)
    }
}