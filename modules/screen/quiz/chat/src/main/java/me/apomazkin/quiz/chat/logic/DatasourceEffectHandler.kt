package me.apomazkin.quiz.chat.logic

import android.util.Log
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateEffectHandler
import me.apomazkin.quiz.chat.R
import me.apomazkin.quiz.chat.logic.ChatMessage.Companion.UserAction
import me.apomazkin.quiz.chat.quiz.QuizGame
import me.apomazkin.ui.resource.ResourceManager
import kotlin.random.Random

/**
 * Effect
 */
internal sealed interface DatasourceEffect : Effect {
    
    data object PrepareToStart : DatasourceEffect
    data object LoadQuiz : DatasourceEffect
    data object ReLoadQuiz : DatasourceEffect
    data object NextQuestion : DatasourceEffect
    data object Skip : DatasourceEffect
    data object GetAnswer : DatasourceEffect
    data class CheckAnswer(val answer: String) : DatasourceEffect
    data object Summary : DatasourceEffect
    data class SessionSummary(val all: Boolean) : DatasourceEffect
}

/**
 * EffectHandler for datastore calls.
 */
internal class DatasourceEffectHandler(
    private val quizGame: QuizGame,
    private val resourceManager: ResourceManager,
) : MateEffectHandler<Msg, Effect> {
    
    override suspend fun runEffect(
        effect: Effect,
        consumer: (Msg) -> Unit
    ) {
        Log.d("##MATE##", "RunEffect: $effect")
        val eff = effect as DatasourceEffect
        return when (eff) {
            is DatasourceEffect.PrepareToStart -> {
                withContext(Dispatchers.IO) {
                    Msg.PrepareToStart
                }
            }
            
            is DatasourceEffect.LoadQuiz -> {
                withContext(Dispatchers.IO) {
                    async { quizGame.loadData() }.await()
                    Msg.QuizLoaded(
                        content = quizGame.getStat()
                    )
                }
            }
            
            is DatasourceEffect.ReLoadQuiz -> {
                withContext(Dispatchers.IO) {
                    async { quizGame.loadNextData() }.await()
                    Msg.QuizReLoaded(
                        content = quizGame.getStat()
                    )
                }
            }
            
            is DatasourceEffect.NextQuestion -> {
                withContext(Dispatchers.IO) {
                    if (quizGame.hasNextQuestion()) {
                        val quiz = quizGame.nextQuestion()
                        delay(Random.nextLong(100, 400))
                        Msg.NextQuestion(
                            content = MessageContent.create(
                                text = quiz,
                            )
                        )
                    } else {
                        
                        async {
                            quizGame.saveSession()
                        }.await()
                        Msg.SessionOver
                    }
                }
            }
            
            is DatasourceEffect.Skip -> {
                quizGame.skip()
                Msg.Skipped
            }
            
            is DatasourceEffect.GetAnswer -> {
                val answer = quizGame.skipAndGetAnswer()
                Msg.ShowAnswer(
                    value = MessageContent.create(
                        text = answer,
                    )
                )
            }
            
            is DatasourceEffect.CheckAnswer -> {
                withContext(Dispatchers.IO) {
                    val assessment = quizGame.makeAssessment(eff.answer)
                    delay(Random.nextLong(100, 400))
                    Msg.Assessment(
                        value = MessageContent.create(
                            text = assessment,
                        )
                    )
                }
            }
            
            is DatasourceEffect.Summary -> {
                if (quizGame.hasSingleSession()) {
                    sendSummary(true)
                } else {
                    Msg.SummaryOptions(
                        value = MessageContent.create(
                            text = resourceManager.stringByResId(R.string.chat_quiz_msg_system_statistic),
                            buttons = listOf(
                                ChatMessage.ChatButton(
                                    R.string.chat_quiz_msg_system_statistic_session,
                                    UserAction.LAST_SESSION_SUMMARY
                                ),
                                ChatMessage.ChatButton(
                                    R.string.chat_quiz_msg_system_statistic_full,
                                    UserAction.FULL_QUIZ_SUMMARY
                                )
                            )
                        )
                    )
                }
            }
            
            is DatasourceEffect.SessionSummary -> {
                if (eff.all) {
                    sendSummary(true)
                } else {
                    sendSummary(false)
                }
            }
        }.let(consumer)
    }
    
    private suspend fun sendSummary(full: Boolean): Msg.Summary {
        val summary: List<AnnotatedString> = quizGame.summary(full)
        delay(Random.nextLong(100, 400))
        return Msg.Summary(
            value = buildList {
                addAll(
                    summary.map {
                        MessageContent.create(
                            text = it.text,
                        )
                    }
                )
            }
        )
    }
}