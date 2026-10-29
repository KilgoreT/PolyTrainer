package me.apomazkin.wordstab.logic

import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.state
import io.github.kilgoret.mate.test.assertEffects
import io.github.kilgoret.mate.test.assertNoEffects
import io.github.kilgoret.mate.test.testReduce
import io.github.kilgoret.mate.test.testScenario
import me.apomazkin.wordstab.R
import me.apomazkin.wordstab.ui.WordsNavigationEffect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Вход в скан глазами reducer'а: диалог выбора источника, запуск камеры и
 * галереи эффектом, переход на экран со снимком.
 * Адреса строк спеки в именах — `TFP-NN_k`, `AWP-NN_k`.
 */
class ScanDialogReducerTest {

    private val reducer = reducer()

    /**
     * Одно исходное состояние на тест: два `WordsTabState` равны, только если у них
     * общий поток списка, поэтому все проверки идут от одного экземпляра.
     */
    private val base = idle()

    private val chooser = ScanDialogState(isOpen = true)

    private fun opened(): WordsTabState = reducer.testReduce(base, Msg.OpenScanDialog).state()

    // ===== Ш2: кнопка-камера и выбор способа =====

    @Test
    fun `AWP-37_1 button opens the chooser without an error and without effects`() {
        val result = reducer.testReduce(base, Msg.OpenScanDialog)

        assertEquals(chooser, result.state().scanDialogState)
        result.assertNoEffects()
    }

    @Test
    fun `TFP-06_1 choose camera sends LaunchCamera and leaves the dialog as it was`() {
        val shown = opened()

        val result = reducer.testReduce(shown, Msg.ChooseCamera)

        result.assertEffects(setOf<Effect>(UiEffect.LaunchCamera))
        assertEquals(shown, result.state())
    }

    @Test
    fun `TFP-06_2 choose gallery sends LaunchGallery and leaves the dialog as it was`() {
        val shown = opened()

        val result = reducer.testReduce(shown, Msg.ChooseGallery)

        result.assertEffects(setOf<Effect>(UiEffect.LaunchGallery))
        assertEquals(shown, result.state())
    }

    @Test
    fun `AWP-37_3 TFP-06_4 closing the chooser changes only the dialog and sends nothing`() {
        val result = reducer.testReduce(opened(), Msg.CloseScanDialog)

        assertEquals(base, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `AWP-37_2 second press of the camera button keeps one chooser`() {
        val first = reducer.testReduce(base, Msg.OpenScanDialog)

        val second = reducer.testReduce(first.state(), Msg.OpenScanDialog)

        assertEquals(first.state(), second.state())
        second.assertNoEffects()
    }

    // ===== Ш3: камера не запустилась =====

    @Test
    fun `TFP-07_1 TFP-07_2 camera unavailable puts the reason into the open dialog`() {
        val result = reducer.testReduce(
            opened(),
            Msg.CameraUnavailable(errorRes = R.string.photo_words_error_camera),
        )

        assertEquals(
            ScanDialogState(isOpen = true, errorRes = R.string.photo_words_error_camera),
            result.state().scanDialogState,
        )
        result.assertNoEffects()
    }

    @Test
    fun `closing the dialog with a camera error clears the error for the next press`() {
        val failed = reducer.testReduce(
            opened(),
            Msg.CameraUnavailable(errorRes = R.string.photo_words_error_camera),
        ).state()

        val closed = reducer.testReduce(failed, Msg.CloseScanDialog).state()
        val reopened = reducer.testReduce(closed, Msg.OpenScanDialog).state()

        assertEquals(base, closed)
        assertEquals(chooser, reopened.scanDialogState)
        assertNull(reopened.scanDialogState.errorRes)
    }

    // ===== В1: возврат без снимка =====

    @Test
    fun `TFP-18_1 TFP-18_2 cancel sends nothing, the chooser stays with both sources`() {
        val steps = reducer.testScenario(
            base,
            Msg.OpenScanDialog,
            Msg.ChooseCamera,
            Msg.ChooseGallery,
        )

        assertEquals(chooser, steps[1].state().scanDialogState)
        assertEquals(chooser, steps[2].state().scanDialogState)
        steps[1].assertEffects(setOf<Effect>(UiEffect.LaunchCamera))
        steps[2].assertEffects(setOf<Effect>(UiEffect.LaunchGallery))
    }

    // ===== Ш3 → экран: снимок получен =====

    @Test
    fun `picked image closes the open chooser and opens the scan screen with its uri`() {
        val result = reducer.testReduce(opened(), Msg.ImagePicked(uri = "content://photo/1"))

        assertEquals(base, result.state())
        result.assertEffects(
            setOf<Effect>(WordsNavigationEffect.OpenScanCandidates(uri = "content://photo/1")),
        )
    }

    // ===== В5: перезапуск при открытой камере или галерее =====

    @Test
    fun `TFP-22_1 TFP-22_2 image picked with the chooser closed still opens the scan screen`() {
        val result = reducer.testReduce(base, Msg.ImagePicked(uri = "content://photo/2"))

        assertEquals(base, result.state())
        result.assertEffects(
            setOf<Effect>(WordsNavigationEffect.OpenScanCandidates(uri = "content://photo/2")),
        )
    }

    @Test
    fun `TFP-22_3 cancel in the camera after process death sends nothing, the tab has no dialog`() {
        // Отмена сообщения не шлёт (PhotoPicker): у свежей вкладки диалога нет.
        assertEquals(ScanDialogState(), base.scanDialogState)
    }

    // ===== В6: перезапуск во время запуска =====

    @Test
    fun `TFP-23_1 fresh tab after process death shows no dialog`() {
        val fresh = WordsTabState()

        assertFalse(fresh.scanDialogState.isOpen)
        assertNull(fresh.scanDialogState.errorRes)
    }

    // ===== Сценарий целиком =====

    @Test
    fun `scenario - button, camera, snapshot, scan screen`() {
        val steps = reducer.testScenario(
            base,
            Msg.OpenScanDialog,
            Msg.ChooseCamera,
            Msg.ImagePicked(uri = "content://photo/3"),
        )

        steps[0].assertNoEffects()
        steps[1].assertEffects(setOf<Effect>(UiEffect.LaunchCamera))
        steps[2].assertEffects(
            setOf<Effect>(WordsNavigationEffect.OpenScanCandidates(uri = "content://photo/3")),
        )
        assertEquals(base, steps[2].state())
    }
}
