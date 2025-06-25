package me.apomazkin.quiz.chat.logic

import android.util.Log
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateEffectHandler
import me.apomazkin.prefs.PrefKey
import me.apomazkin.prefs.PrefsProvider
import me.apomazkin.quiz.chat.quiz.QuizGame
import kotlin.random.Random

/**
 * Effect
 */
internal sealed interface DatasourceEffect : Effect {

    data object PrepareToStart : DatasourceEffect
    data object EarliestOn : DatasourceEffect
    data object EarliestOff : DatasourceEffect
    data object FrequentMistakesOn : DatasourceEffect
    data object FrequentMistakesOff : DatasourceEffect
    data object DebugOn : DatasourceEffect
    data object DebugOff : DatasourceEffect
    data object LoadQuiz : DatasourceEffect
    data object NextQuestion : DatasourceEffect
    data object Skip : DatasourceEffect
    data object GetAnswer : DatasourceEffect
    data class CheckAnswer(val answer: String) : DatasourceEffect
    data object Summary : DatasourceEffect
}

/**
 * EffectHandler for datastore calls.
 */
internal class DatasourceEffectHandler(
        private val quizGame: QuizGame,
        private val prefsProvider: PrefsProvider,
) : MateEffectHandler<Msg, Effect> {

    override suspend fun runEffect(
            effect: Effect,
            consumer: (Msg) -> Unit,
    ) {
        Log.d("##MATE##", "RunEffect: $effect")
        val eff = effect as DatasourceEffect
        return when (eff) {
            is DatasourceEffect.PrepareToStart -> {
                withContext(Dispatchers.IO) {
                    Msg.PrepareToStart
                }
            }

            is DatasourceEffect.EarliestOn -> {
                withContext(Dispatchers.IO) {
                    prefsProvider.setBoolean(PrefKey.CHAT_EARLIEST_REVIEWED_STATUS_BOOLEAN, true)
                    Msg.Empty
                }
            }

            is DatasourceEffect.EarliestOff -> {
                withContext(Dispatchers.IO) {
                    prefsProvider.setBoolean(PrefKey.CHAT_EARLIEST_REVIEWED_STATUS_BOOLEAN, false)
                    Msg.Empty
                }
            }

            is DatasourceEffect.FrequentMistakesOn -> {
                withContext(Dispatchers.IO) {
                    prefsProvider.setBoolean(PrefKey.CHAT_FREQUENT_MISTAKES_STATUS_BOOLEAN, true)
                    Msg.Empty
                }
            }

            is DatasourceEffect.FrequentMistakesOff -> {
                withContext(Dispatchers.IO) {
                    prefsProvider.setBoolean(PrefKey.CHAT_FREQUENT_MISTAKES_STATUS_BOOLEAN, false)
                    Msg.Empty
                }
            }

            is DatasourceEffect.DebugOn -> {
                withContext(Dispatchers.IO) {
                    prefsProvider.setBoolean(PrefKey.CHAT_DEBUG_STATUS_BOOLEAN, true)
                    Msg.Empty
                }
            }

            is DatasourceEffect.DebugOff -> {
                withContext(Dispatchers.IO) {
                    prefsProvider.setBoolean(PrefKey.CHAT_DEBUG_STATUS_BOOLEAN, false)
                    Msg.Empty
                }
            }

            is DatasourceEffect.LoadQuiz -> {
                withContext(Dispatchers.IO) {
                    async { quizGame.loadData() }.await()
                    Msg.QuizLoaded(content = quizGame.getStat())
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
                        Msg.SessionOver(
                                MessageContent.create(
                                        text = quizGame.summaryGeneral()
                                )
                        )
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
                    val userAttempt = eff.answer.trim()
                    val assessment = quizGame.makeAssessment(userAttempt)
                    delay(Random.nextLong(100, 400))
                    Msg.Assessment(
                            value = MessageContent.create(
                                    text = assessment,
                            )
                    )
                }
            }

            is DatasourceEffect.Summary -> {
                sendSummary()
            }
        }.let(consumer)
    }

    private fun sendSummary(): Msg.Summary {
        val summary: List<AnnotatedString> = listOf(quizGame.summaryDetail())
//        delay(Random.nextLong(100, 400))
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