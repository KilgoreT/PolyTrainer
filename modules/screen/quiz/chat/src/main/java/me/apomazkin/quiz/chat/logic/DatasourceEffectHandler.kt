package me.apomazkin.quiz.chat.logic

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateEffectHandler

/**
 * Effect
 */
internal sealed interface DatasourceEffect : Effect {
    
    /**
     * Effect to get current language.
     */
    data object LoadCurrentDict : DatasourceEffect
    data object LoadQuiz : DatasourceEffect
}

/**
 * EffectHandler for datastore calls.
 */
internal class DatasourceEffectHandler(
    //    private val quizTabUseCase: QuizTabUseCase,
) : MateEffectHandler<Msg, Effect> {
    
    override suspend fun runEffect(
        effect: Effect,
        consumer: (Msg) -> Unit
    ) {
        Log.d("##MATE##", "RunEffect: $effect")
        return when (val eff = effect as? DatasourceEffect) {
            
            is DatasourceEffect.LoadCurrentDict -> {
                withContext(Dispatchers.IO) {
                    //                    quizTabUseCase.getCurrentDict()
                    //                        .let { TopBarActionMsg.CurrentDict(lang = it) }
                    Msg.Empty
                }
            }
            
            is DatasourceEffect.LoadQuiz -> {
                withContext(Dispatchers.IO) {
                    val quizData = mutableListOf<Pair<String, String>>()
                    for (i in 0..50) {
                        quizData.add("Question $i" to "$i")
                    }
                    Msg.QuizData(quizData)
                }
            }
            
            null -> Msg.Empty
        }.let(consumer)
    }
}