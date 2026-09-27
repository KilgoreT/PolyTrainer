package me.apomazkin.polytrainer.di.module.quizgroup

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.prefs.PrefsProvider
import me.apomazkin.prefs.quizGroupPrefKey
import me.apomazkin.quiz.QuizGroupCount
import me.apomazkin.quiz.resolveQuizGroupState
import me.apomazkin.quiztab.LogTags
import javax.inject.Inject

/**
 * IS500. Selection-store выбора группы квиза — один на всех
 * потребителей (карточки таба «Тренировки» и выборка квиза).
 * Персист — pref `quiz_group_<тип>_dict_<id>`, валидация при чтении —
 * доменной воронкой `resolveQuizGroupState` (та же, что в подписке
 * плашки: два пути валидации разойтись не могут).
 *
 * Контракт: `dictionaryId` — ТОЛЬКО параметром. Self-read pref'а
 * текущего словаря внутри store запрещён: вызывающий фиксирует словарь
 * один раз (например, на старте квиз-сессии), и пара «словарь-группа»
 * остаётся консистентной на момент выборки при гонке смены словаря.
 */
class QuizGroupSelectionStore @Inject constructor(
    private val quizApi: CoreDbApi.QuizApi,
    private val prefsProvider: PrefsProvider,
    private val logger: LexemeLogger,
) {

    /**
     * Валидированный выбор: `null` = «Все». Мусор в pref'е, мёртвая,
     * усохшая ниже порога или чужая группа — молча «Все» (Д3). Чтение
     * без побочек: закрепление фолбэка (стирание невалидного pref'а) —
     * обязанность плашки, reducer-эффект по `selectionInvalidated`.
     */
    suspend fun getValidatedSelection(quizType: String, dictionaryId: Long): Long? {
        val raw = prefsProvider.getStringByRawKey(quizGroupPrefKey(quizType, dictionaryId))
        val persisted = raw?.toLongOrNull()
        val counts = quizApi.flowQuizGroupCounts(dictionaryId).first()
        val state = resolveQuizGroupState(
            groupCounts = counts.map {
                QuizGroupCount(id = it.groupId, name = it.name, wordCount = it.wordCount)
            },
            dictionaryWordCount = quizApi.flowDictionaryQuizWordCount(dictionaryId).first(),
            persistedGroupId = persisted,
        )
        val fallback = when {
            raw == null -> "none"
            persisted == null -> "garbage"
            state.selectedGroupId != null -> "none"
            counts.none { it.groupId == persisted } -> "dead"
            else -> "below_threshold"
        }
        logger.d(
            tag = LogTags.QUIZ,
            message = "quizGroupStore: read type=$quizType dict=$dictionaryId " +
                "raw=${raw ?: "none"} resolved=${state.selectedGroupId ?: "all"} " +
                "fallback=$fallback",
        )
        return state.selectedGroupId
    }

    /** `null` («Все») стирает ключ. */
    suspend fun setSelection(quizType: String, dictionaryId: Long, groupId: Long?) {
        prefsProvider.setStringByRawKey(
            key = quizGroupPrefKey(quizType, dictionaryId),
            value = groupId?.toString(),
        )
        logger.d(
            tag = LogTags.QUIZ,
            message = "quizGroupStore: write type=$quizType dict=$dictionaryId " +
                "group=${groupId ?: "all"}",
        )
    }

    /**
     * Сырой pref-поток (без валидации) для combine-подписки плашки:
     * валидацию делает подписка той же воронкой — счётчики у неё уже
     * в руках, двойного чтения БД нет.
     */
    fun flowSelection(quizType: String, dictionaryId: Long): Flow<Long?> =
        prefsProvider.getStringFlowByRawKey(quizGroupPrefKey(quizType, dictionaryId))
            .map { it?.toLongOrNull() }
}
