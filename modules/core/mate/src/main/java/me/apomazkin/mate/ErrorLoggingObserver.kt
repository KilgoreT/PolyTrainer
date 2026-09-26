package me.apomazkin.mate

import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.MateObserver
import io.github.kilgoret.mate.Subscription
import me.apomazkin.logger.LexemeLogger

/**
 * Общий наблюдатель ошибок цикла: единственное место, где стектрейсы
 * упавших эффектов и подписок попадают в лог. Хендлеры ошибки не
 * ловят (провал эффекта объявлен в типе через RecoverableEffect) —
 * без этого наблюдателя ошибка видна только как recovery-Msg в
 * reduce-логе, без причины.
 *
 * Контравариантность MateObserver: один инстанс вешается на раннер
 * любого экрана (observers в Assembly.create).
 *
 * @param tag модульный тег логов экрана (###WORDCARD### и т.п.).
 */
class ErrorLoggingObserver(
    private val logger: LexemeLogger,
    private val tag: String,
) : MateObserver<Any?, Any?, Effect> {
    override fun onEffectRecovered(
        effect: Effect,
        error: Throwable,
        message: Any?,
    ) {
        logger.e(tag = tag, message = "effect $effect recovered to $message: $error")
    }

    override fun onEffectFailed(
        effect: Effect,
        error: Throwable,
    ) {
        logger.e(tag = tag, message = "effect $effect failed: $error")
    }

    override fun onSubscriptionError(
        sub: Subscription,
        error: Throwable,
    ) {
        logger.e(tag = tag, message = "subscription $sub failed: $error")
    }
}
