package me.apomazkin.quiztab.logic

import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.RecoverableEffect
import me.apomazkin.quiz.QuizGroupOptions

sealed interface Msg {

    /** Тап по карточке квиза — вход в чат. */
    data class OpenChat(val quizType: String) : Msg

    /**
     * Эмиссия подписки [QuizTabSub.GroupOptions] — пункты пикера и уже
     * валидированный набор (резолв воронкой на каждом эмите; пусто =
     * «Все»; `dictionaryId == null` — «словарей нет»).
     *
     * [selectionInvalidated] — сохранённый набор отличается от
     * валидированного (выпала мёртвая/усохшая группа или в pref мусор):
     * reducer закрепляет очищенный набор записью в pref — выпавшая
     * группа сама не вернётся.
     */
    data class GroupOptionsLoaded(
        val quizType: String,
        val dictionaryId: Long?,
        val options: QuizGroupOptions,
        val selectedGroupIds: Set<Long>,
        val selectionInvalidated: Boolean,
    ) : Msg

    /**
     * Клик по группе в пикере карточки [quizType]: [checked] — новое
     * положение галки. Отмеченная группа снимает «Все»; снятая последняя
     * возвращает «Все».
     */
    data class ToggleGroup(
        val quizType: String,
        val groupId: Long,
        val checked: Boolean,
    ) : Msg

    /** Клик по «Все» в пикере карточки [quizType]: снимает все группы. */
    data class PickAll(val quizType: String) : Msg

    /**
     * IS500: подписка опций упала (catch). State не трогаем — дефолт
     * рабочий («Все», карточка кликабельна); стектрейс в логе handler'а.
     */
    data class GroupOptionsFailed(val quizType: String) : Msg

    data object Empty : Msg
}

sealed interface UiMsg : Msg {
    /**
     * Message for Snackbar
     * @param message text of Snackbar.
     * @param show variable to reset show status for state.
     */
    data class Snackbar(val message: String, val show: Boolean) : UiMsg
    data class LifeCycleEvent(val lifeCycle: LifeCycle) : UiMsg {
        enum class LifeCycle {
            ON_CREATE,
            ON_START,
            ON_RESUME,
            ON_PAUSE,
            ON_STOP,
            ON_DESTROY,
            ON_ANY,
        }
    }
}

sealed interface QuizTabDatasourceEffect : Effect {
    /**
     * Персист набора групп (пусто = «Все»). Провал записи pref'а
     * некритичен (state обновлён оптимистично, pref-поток подписки
     * выровняет расхождение) — [onFail] молчит, стектрейс уходит в
     * ErrorLoggingObserver. Быстрые клики в открытом меню пишутся по
     * порядку: store сериализует записи.
     */
    data class PersistGroupSelection(
        val quizType: String,
        val dictionaryId: Long,
        val groupIds: Set<Long>,
    ) : QuizTabDatasourceEffect,
        RecoverableEffect<Msg> {
        override fun onFail(error: Throwable): Msg = Msg.Empty
    }
}
