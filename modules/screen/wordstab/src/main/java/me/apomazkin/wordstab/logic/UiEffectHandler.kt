package me.apomazkin.wordstab.logic

import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.MateEffectHandler
import me.apomazkin.wordstab.R
import me.apomazkin.wordstab.deps.ScanLauncher

sealed interface UiEffect : Effect {
    data class ShowNotification(val message: String) : UiEffect

    /** Открыть системную камеру, снимок — в файл в кеше приложения (WFP-06.1). */
    data object LaunchCamera : UiEffect

    /** Открыть системный выбор снимка (WFP-06.2). */
    data object LaunchGallery : UiEffect
}

/**
 * Исполнитель UI-эффектов вкладки: сообщение в снекбар и запуск системных
 * приложений за снимком через [ScanLauncher].
 */
class UiEffectHandler(
    private val launcher: ScanLauncher,
) : MateEffectHandler<Msg, UiEffect> {

    override val effectFamily = UiEffect::class

    override suspend fun runEffect(effect: UiEffect, consumer: (Msg) -> Unit) {
        when (effect) {
            is UiEffect.ShowNotification -> consumer(
                UiMsg.ShowNotification(
                    message = effect.message,
                    show = true,
                ),
            )

            UiEffect.LaunchCamera -> if (!launcher.launchCamera()) {
                consumer(Msg.CameraUnavailable(errorRes = R.string.photo_words_error_camera))
            }

            UiEffect.LaunchGallery -> launcher.launchGallery()
        }
    }
}
