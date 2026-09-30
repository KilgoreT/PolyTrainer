package me.apomazkin.quiz.chat.logic

import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import me.apomazkin.lexeme.ComponentTypeRef
import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.MateEffectHandler
import me.apomazkin.prefs.PrefKey
import me.apomazkin.prefs.PrefsProvider
import me.apomazkin.quiz.chat.deps.QuizChatUseCase
import me.apomazkin.quiz.chat.quiz.QuizGame
import me.apomazkin.mate.LogTags
import me.apomazkin.logger.LexemeLogger
import kotlin.random.Random

sealed interface DatasourceEffect : Effect {

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

    /**
     * Капельная выдача пачки сообщений бота: по одному, каждое после паузы
     * «бот думает». Несколько пузырей одним апдейтом вставали разом, и
     * въезд по одному (чат-фикс 6) на них не работал. Хендлер выдерживает
     * паузу и возвращает первое сообщение как [Msg.SystemMessageDelivered]
     * с остатком очереди; редьюсер кладёт его в ленту и повторяет эффект
     * для остатка, пока очередь не опустеет. Содержимое сообщений — в
     * редьюсере, хендлер только задаёт темп.
     */
    data class DeliverSystemMessages(val messages: List<MessageContent>) : DatasourceEffect

    /**
     * IS481 quiz picker. One-shot fetch на entry — availableTypes + restored
     * selectedRef → `Msg.QuizComponentTypesLoaded`. `dictionaryId` резолвится
     * в handler через `useCase.getCurrentDictionaryId()`.
     */
    data object LoadQuizComponentTypes : DatasourceEffect

    /**
     * IS481 quiz picker. Persist write. Flow подхватит write и emit
     * `Msg.QuizComponentTypesLoaded` для UI update.
     */
    data class SaveQuizPickerSelection(val ref: ComponentTypeRef) : DatasourceEffect

    /**
     * IS500. Имя группы тренировки для сабтайтла аппбара — на входе
     * в экран (init-эффект), до старта сессии.
     */
    data object LoadQuizGroupName : DatasourceEffect
}

/**
 * @param io диспатчер блокирующих операций; прод — Dispatchers.IO,
 *   в тестах можно подставить тестовый.
 */
class DatasourceEffectHandler(
    private val quizGame: QuizGame,
    private val prefsProvider: PrefsProvider,
    private val useCase: QuizChatUseCase,
    private val logger: LexemeLogger,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : MateEffectHandler<Msg, DatasourceEffect> {

    override val effectFamily = DatasourceEffect::class

    override suspend fun runEffect(effect: DatasourceEffect, consumer: (Msg) -> Unit) {
        logger.d(tag = LogTags.MATE, message = "RunEffect: $effect")
        val msg: Msg = when (effect) {
            is DatasourceEffect.PrepareToStart -> withContext(io) {
                Msg.PrepareToStart
            }
            is DatasourceEffect.EarliestOn -> withContext(io) {
                prefsProvider.setBoolean(PrefKey.CHAT_EARLIEST_REVIEWED_STATUS_BOOLEAN, true)
                Msg.Empty
            }
            is DatasourceEffect.EarliestOff -> withContext(io) {
                prefsProvider.setBoolean(PrefKey.CHAT_EARLIEST_REVIEWED_STATUS_BOOLEAN, false)
                Msg.Empty
            }
            is DatasourceEffect.FrequentMistakesOn -> withContext(io) {
                prefsProvider.setBoolean(PrefKey.CHAT_FREQUENT_MISTAKES_STATUS_BOOLEAN, true)
                Msg.Empty
            }
            is DatasourceEffect.FrequentMistakesOff -> withContext(io) {
                prefsProvider.setBoolean(PrefKey.CHAT_FREQUENT_MISTAKES_STATUS_BOOLEAN, false)
                Msg.Empty
            }
            is DatasourceEffect.DebugOn -> withContext(io) {
                prefsProvider.setBoolean(PrefKey.CHAT_DEBUG_STATUS_BOOLEAN, true)
                Msg.Empty
            }
            is DatasourceEffect.DebugOff -> withContext(io) {
                prefsProvider.setBoolean(PrefKey.CHAT_DEBUG_STATUS_BOOLEAN, false)
                Msg.Empty
            }
            is DatasourceEffect.LoadQuiz -> withContext(io) {
                async { quizGame.loadData() }.await()
                // IS500: имя группы сессии обновляется и на старте
                // сессии (страховка «Продолжить» после смены данных).
                consumer(loadQuizGroupName())
                Msg.QuizLoaded(content = quizGame.getStat())
            }
            is DatasourceEffect.LoadQuizGroupName -> withContext(io) {
                loadQuizGroupName()
            }
            is DatasourceEffect.NextQuestion -> withContext(io) {
                if (quizGame.hasNextQuestion()) {
                    val quiz = quizGame.nextQuestion()
                    delay(botPauseMs())
                    Msg.NextQuestion(content = MessageContent.create(text = quiz))
                } else {
                    async { quizGame.saveSession() }.await()
                    // Финал — тоже сообщение бота: пауза, чтобы оценка последнего
                    // ответа успела доехать до вставки итогов.
                    delay(botPauseMs())
                    Msg.SessionOver(MessageContent.create(text = quizGame.summaryGeneral()))
                }
            }
            is DatasourceEffect.Skip -> {
                quizGame.skip()
                Msg.Skipped
            }
            is DatasourceEffect.GetAnswer -> withContext(io) {
                val answer = quizGame.skipAndGetAnswer()
                // Пауза как перед любым сообщением бота: пузырь юзера
                // «Показать ответ» успевает встать, ответ въезжает следом.
                delay(botPauseMs())
                Msg.ShowAnswer(value = MessageContent.create(text = answer))
            }
            is DatasourceEffect.CheckAnswer -> withContext(io) {
                val userAttempt = effect.answer.trim()
                val assessment = quizGame.makeAssessment(userAttempt)
                delay(botPauseMs())
                Msg.Assessment(value = MessageContent.create(text = assessment))
            }
            is DatasourceEffect.Summary -> withContext(io) {
                delay(botPauseMs())
                sendSummary()
            }
            is DatasourceEffect.DeliverSystemMessages -> withContext(io) {
                val first = effect.messages.firstOrNull()
                if (first == null) {
                    Msg.Empty
                } else {
                    delay(botPauseMs())
                    Msg.SystemMessageDelivered(
                        message = first,
                        rest = effect.messages.drop(1),
                    )
                }
            }
            is DatasourceEffect.LoadQuizComponentTypes -> withContext(io) {
                val dictId = useCase.getCurrentDictionaryId()
                if (dictId == null) {
                    Msg.Empty
                } else {
                    Msg.QuizComponentTypesLoaded(
                        types = useCase.getAvailableTypes(dictId),
                        restoredSelectedRef = useCase.getQuizPickerSelection(dictId),
                    )
                }
            }
            is DatasourceEffect.SaveQuizPickerSelection -> withContext(io) {
                val dictId = useCase.getCurrentDictionaryId()
                if (dictId != null) {
                    useCase.setQuizPickerSelection(dictId, effect.ref)
                }
                Msg.Empty
            }
        }
        consumer(msg)
    }

    /**
     * IS500: валидированное имя группы тренировки (null = «Все») для
     * сабтайтла аппбара.
     */
    private suspend fun loadQuizGroupName(): Msg.QuizGroupNameLoaded {
        val groupName = useCase.getCurrentDictionaryId()
            ?.let { useCase.getSelectedQuizGroupName(it) }
        logger.d(
            tag = me.apomazkin.quiz.chat.LogTags.CHAT,
            message = "subtitle: group=${groupName ?: "all"}",
        )
        return Msg.QuizGroupNameLoaded(name = groupName)
    }

    private fun sendSummary(): Msg.Summary {
        val summary: List<AnnotatedString> = listOf(quizGame.summaryDetail())
        return Msg.Summary(
            value = buildList {
                addAll(summary.map { MessageContent.create(text = it.text) })
            }
        )
    }

    /**
     * Пауза «бот думает» перед сообщением. Нижняя граница — не короче
     * анимации въезда пузыря, чтобы пузыри шли по одному, а не
     * накладывались анимациями (чат-фикс 6). Для разглядывания анимаций
     * паузы временно поднимали до 2–7 с (2026-09-29).
     */
    private fun botPauseMs(): Long = Random.nextLong(BOT_PAUSE_MIN_MS, BOT_PAUSE_MAX_MS)
}

private const val BOT_PAUSE_MIN_MS = 400L
private const val BOT_PAUSE_MAX_MS = 650L
