package me.apomazkin.wordstab.logic

import io.github.kilgoret.mate.state
import me.apomazkin.wordstab.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Слой атомов: каждый атом — один шаг, состояние и эффекты. Тест наследует
 * [ScanDialogAtoms] с пустым логгером, как reducer.
 */
class ScanDialogAtomsTest : ScanDialogAtoms(NoopLogger) {

    private val base = idle().copy(snackbarState = SnackbarState(title = "test", show = true))

    @Test
    fun `show opens the dialog without an error and touches nothing else`() {
        val (shown, effects) = base.showScanDialog()

        assertEquals(base.copy(scanDialogState = ScanDialogState(isOpen = true)), shown)
        assertTrue(effects.isEmpty())
    }

    @Test
    fun `show on a dialog with an error drops the error`() {
        val failed = base.showScanDialogError(R.string.photo_words_error_camera).state()

        val (shown, effects) = failed.showScanDialog()

        assertEquals(ScanDialogState(isOpen = true), shown.scanDialogState)
        assertTrue(effects.isEmpty())
    }

    @Test
    fun `hide returns the dialog to the defaults and touches nothing else`() {
        val failed = base.showScanDialogError(R.string.photo_words_error_camera).state()

        val (hidden, effects) = failed.hideScanDialog()

        assertEquals(base, hidden)
        assertTrue(effects.isEmpty())
    }

    @Test
    fun `hide on a closed dialog changes nothing`() {
        val (hidden, effects) = base.hideScanDialog()

        assertEquals(base, hidden)
        assertTrue(effects.isEmpty())
    }

    @Test
    fun `show error opens the dialog with the reason`() {
        val (failed, effects) = base.showScanDialogError(R.string.photo_words_error_camera)

        assertEquals(
            ScanDialogState(isOpen = true, errorRes = R.string.photo_words_error_camera),
            failed.scanDialogState,
        )
        assertEquals(base.snackbarState, failed.snackbarState)
        assertTrue(effects.isEmpty())
    }
}
