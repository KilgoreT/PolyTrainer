package me.apomazkin.mate

import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.ReducerResult
import me.apomazkin.logger.LexemeLogger

/**
 * База логирования атомарных state-экстеншнов reducer'а (конвенция
 * «Msg → цепочка атомов», project-architecture.md §Reducer-конвенция).
 *
 * Класс атомов экрана наследует её и объявляет атомы member-экстеншнами —
 * так [logger] доступен ВНУТРИ атома через dispatch receiver, без
 * проброса параметром и без хранения в state; reducer наследует класс
 * атомов, тест атомов — его же с Noop-логгером.
 *
 * Иерархия: ReducerLogging (mate) ← StateAtoms (атомы экрана) ←
 * XxxReducer / StateAtomsTest.
 */
abstract class ReducerLogging(
    protected val logger: LexemeLogger,
) {
    /**
     * Фичевый тег logcat всей вкладки (`###GROUPS###`): под ним пишут и
     * [logStep], и [logMessage] reducer'а, и effect handler — весь флоу
     * экрана достаётся одним `grep`. Задаётся классом атомов экрана.
     */
    protected abstract val logTag: String

    /**
     * Лог входящего Msg (`Reduce ---message---: SubmitSheet`).
     * @param message описание сообщения (тяжёлые data-Msg — именем класса).
     */
    protected fun logMessage(message: String) {
        logger.log(tag = logTag, message = "Reduce ---message---: $message")
    }

    /**
     * Лог одного атомарного шага: событие + характеристики
     * (`Reduce ---step---: applyAllCount | count=13`). Тяжёлые данные
     * передавать счётчиками (`"words" to words.size`).
     * @param step имя атома.
     * @param attrs пары ключ=значение — параметры/результат шага.
     */
    protected fun logStep(
        step: String,
        vararg attrs: Pair<String, Any?>,
    ) {
        val detail = if (attrs.isEmpty()) {
            ""
        } else {
            attrs.joinToString(separator = " ", prefix = " | ") { (key, value) -> "$key=$value" }
        }
        logger.log(tag = logTag, message = "Reduce ---step---: $step$detail")
    }

    /**
     * Осознанный no-op: сообщение дошло, но состояние не меняется —
     * guard-ветка reducer'а (гонка, спам, повторный LaunchedEffect) либо
     * атом без своих данных. Пишет `Reduce ---step---: no-op | reason=…`,
     * чтобы «событие пришло и было намеренно проигнорировано» читалось в
     * логе прямо, а не по отсутствию шагов; возвращает состояние без
     * изменений и без эффектов.
     * @param reason почему шага нет (`same dictionary`, `sheet closed`).
     */
    protected fun <S> S.noOp(reason: String): ReducerResult<S, Effect> {
        logger.log(tag = logTag, message = "Reduce ---step---: no-op | reason=$reason")
        return this to emptySet()
    }
}
