package me.apomazkin.quiztab.logic

import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.RecoverableEffect
import me.apomazkin.quiz.QuizGroupOptions

sealed interface Msg {

    /** Тап по карточке квиза — вход в чат. */
    data class OpenChat(val quizType: String) : Msg

    /**
     * IS500: выбор пункта в пикере группы карточки [quizType];
     * `groupId == null` — «Все».
     */
    data class PickGroup(
        val quizType: String,
        val groupId: Long?,
    ) : Msg

    /**
     * IS500: эмиссия подписки [QuizTabSub.GroupOptions] — пункты пикера
     * и уже валидированный выбор (резолв воронкой на каждом эмите;
     * `dictionaryId == null` — «словарей нет»).
     *
     * [selectionInvalidated] — персист ссылался на мёртвую/усохшую
     * группу и был отвергнут воронкой: reducer закрепляет фолбэк,
     * стирая pref (решение прогона 2026-09-26 — «Все» после сброса
     * остаётся, воскрешений нет).
     */
    data class GroupOptionsLoaded(
        val quizType: String,
        val dictionaryId: Long?,
        val options: QuizGroupOptions,
        val selectedGroupId: Long?,
        val selectionInvalidated: Boolean,
    ) : Msg

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
     * IS500: персист выбора группы. Провал записи pref'а некритичен
     * (state обновлён оптимистично, pref-поток подписки выровняет
     * расхождение) — [onFail] молчит, стектрейс уходит в
     * ErrorLoggingObserver. Гонка быстрых перевыборов принята
     * (запись pref быстрая, эхо подписки выравнивает).
     */
    data class PersistGroupSelection(
        val quizType: String,
        val dictionaryId: Long,
        val groupId: Long?,
    ) : QuizTabDatasourceEffect,
        RecoverableEffect<Msg> {
        override fun onFail(error: Throwable): Msg = Msg.Empty
    }
}
