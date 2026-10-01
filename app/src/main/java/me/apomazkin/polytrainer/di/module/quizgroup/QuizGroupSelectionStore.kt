package me.apomazkin.polytrainer.di.module.quizgroup

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.prefs.PrefsProvider
import me.apomazkin.prefs.quizGroupsPrefKey
import me.apomazkin.quiz.PersistedQuizGroups
import me.apomazkin.quiz.QuizGroupCount
import me.apomazkin.quiz.QuizGroupState
import me.apomazkin.quiz.decodeQuizGroupIds
import me.apomazkin.quiz.encodeQuizGroupIds
import me.apomazkin.quiz.resolveQuizGroupState
import me.apomazkin.quiztab.LogTags
import java.text.Collator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Selection-store набора групп квиза — один на всех потребителей
 * (карточки таба «Квизы» и выборка/сабтайтл квиза). Персист — pref
 * `quiz_groups_<тип>_dict_<id>`, валидация при чтении — доменной
 * воронкой `resolveQuizGroupState` (та же, что в подписке плашки: два
 * пути валидации разойтись не могут).
 *
 * Контракт: `dictionaryId` — ТОЛЬКО параметром. Self-read pref'а
 * текущего словаря внутри store запрещён: вызывающий фиксирует словарь
 * один раз (например, на старте квиз-сессии), и пара «словарь-группы»
 * остаётся консистентной на момент выборки при гонке смены словаря.
 *
 * Записи сериализованы: в открытом меню юзер отмечает несколько групп
 * подряд, и порядок записей обязан совпадать с порядком кликов — иначе
 * последней может лечь устаревшая.
 */
@Singleton
class QuizGroupSelectionStore @Inject constructor(
    private val quizApi: CoreDbApi.QuizApi,
    private val prefsProvider: PrefsProvider,
    private val logger: LexemeLogger,
) {

    private val writeLock = Mutex()

    // Порядок групп — locale-aware, как в пикере плашки (спека групп §5).
    private val collator: Comparator<String> = run {
        val collator = Collator.getInstance()
        Comparator { a, b -> collator.compare(a, b) }
    }

    /**
     * Валидированное состояние по одному снапшоту счётчиков: группы
     * отсортированы, набор очищен воронкой. Чтение без побочек:
     * закрепление очищенного набора — обязанность плашки.
     */
    suspend fun getValidatedState(quizType: String, dictionaryId: Long): QuizGroupState {
        val raw = prefsProvider.getStringByRawKey(quizGroupsPrefKey(quizType, dictionaryId))
        val persisted = decodeQuizGroupIds(raw)
        val counts = quizApi.flowQuizGroupCounts(dictionaryId).first()
        val state = resolveQuizGroupState(
            groupCounts = counts
                .map { QuizGroupCount(id = it.groupId, name = it.name, wordCount = it.wordCount) }
                .sortedWith(compareBy(collator) { it.name }),
            dictionaryWordCount = quizApi.flowDictionaryQuizWordCount(dictionaryId).first(),
            persistedGroupIds = persisted.ids,
        )
        val dropped = (persisted.ids - state.selectedGroupIds).map { id ->
            val reason = if (counts.none { it.groupId == id }) "dead" else "below"
            "$id:$reason"
        }
        logger.d(
            tag = LogTags.QUIZ,
            message = "quizGroupStore: read type=$quizType dict=$dictionaryId " +
                "raw=${raw ?: "none"} resolved=${state.selectedGroupIds.describe()} " +
                "dropped=${dropped.ifEmpty { listOf("none") }.joinToString(",")} " +
                "garbage=${persisted.hasGarbage}",
        )
        return state
    }

    /** Валидированный набор групп: пусто = «Все». */
    suspend fun getValidatedSelection(quizType: String, dictionaryId: Long): Set<Long> =
        getValidatedState(quizType, dictionaryId).selectedGroupIds

    /** Пустой набор («Все») стирает ключ. */
    suspend fun setSelection(quizType: String, dictionaryId: Long, groupIds: Set<Long>) {
        writeLock.withLock {
            prefsProvider.setStringByRawKey(
                key = quizGroupsPrefKey(quizType, dictionaryId),
                value = encodeQuizGroupIds(groupIds),
            )
        }
        logger.d(
            tag = LogTags.QUIZ,
            message = "quizGroupStore: write type=$quizType dict=$dictionaryId " +
                "groups=${groupIds.describe()}",
        )
    }

    /**
     * Сырой pref-поток (без валидации) для combine-подписки плашки:
     * валидацию делает подписка той же воронкой — счётчики у неё уже
     * в руках, двойного чтения БД нет. Пометка мусора нужна, чтобы
     * закрепить очищенный набор и в этом случае.
     */
    fun flowSelection(quizType: String, dictionaryId: Long): Flow<PersistedQuizGroups> =
        prefsProvider.getStringFlowByRawKey(quizGroupsPrefKey(quizType, dictionaryId))
            .map { decodeQuizGroupIds(it) }

    private fun Set<Long>.describe(): String =
        if (isEmpty()) "all" else sorted().joinToString(",")
}
