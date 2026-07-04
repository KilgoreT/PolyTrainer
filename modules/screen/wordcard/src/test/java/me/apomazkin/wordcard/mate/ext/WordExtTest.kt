package me.apomazkin.wordcard.mate.ext

import me.apomazkin.wordcard.mate.WordCardState
import me.apomazkin.wordcard.mate.WordState
import me.apomazkin.wordcard.mate.disableWordEdit
import me.apomazkin.wordcard.mate.enableWordEdit
import me.apomazkin.wordcard.mate.hideWordWarningDialog
import me.apomazkin.wordcard.mate.showWordWarningDialog
import me.apomazkin.wordcard.mate.updateWordEdited
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * Tests for extensions on Word edit/warning state under sealed WordState.
 * Surviving extensions: enableWordEdit, disableWordEdit, updateWordEdited,
 * showWordWarningDialog, hideWordWarningDialog.
 *
 * Удалённые extensions (setWordId / setWordAdded / setWordValue / setTerm) тестов не требуют —
 * мэппинг term делается в reducer-ветке WordLoaded.
 */
class WordExtTest {

    private fun loaded(
        value: String = "word",
        isEditMode: Boolean = false,
        edited: String = "",
        showWarningDialog: Boolean = false,
    ): WordCardState = WordCardState(
        isLoading = false,
        wordState = WordState.Loaded(
            id = 1L,
            dictionaryId = 3L,
            added = Date(0L),
            value = value,
            isEditMode = isEditMode,
            edited = edited,
            showWarningDialog = showWarningDialog,
        ),
    )

    @Test
    fun `enableWordEdit on Loaded sets isEditMode true and edited to value`() {
        val initial = loaded(value = "abc")

        val result = initial.enableWordEdit()

        val w = result.wordState as WordState.Loaded
        assertTrue(w.isEditMode)
        assertEquals("abc", w.edited)
    }

    @Test
    fun `disableWordEdit on Loaded resets isEditMode and edited`() {
        val initial = loaded(isEditMode = true, edited = "abc")

        val result = initial.disableWordEdit()

        val w = result.wordState as WordState.Loaded
        assertFalse(w.isEditMode)
        assertEquals("", w.edited)
    }

    @Test
    fun `updateWordEdited on Loaded updates edited only`() {
        val initial = loaded(value = "original", isEditMode = true, edited = "old")

        val result = initial.updateWordEdited("new")

        val w = result.wordState as WordState.Loaded
        assertEquals("new", w.edited)
        assertEquals("original", w.value)
    }

    @Test
    fun `showWordWarningDialog on Loaded sets showWarningDialog true`() {
        val initial = loaded()

        val result = initial.showWordWarningDialog()

        val w = result.wordState as WordState.Loaded
        assertTrue(w.showWarningDialog)
    }

    @Test
    fun `hideWordWarningDialog on Loaded sets showWarningDialog false`() {
        val initial = loaded(showWarningDialog = true)

        val result = initial.hideWordWarningDialog()

        val w = result.wordState as WordState.Loaded
        assertFalse(w.showWarningDialog)
    }

    @Test
    fun `enableWordEdit on NotLoaded is no-op (guard)`() {
        val initial = WordCardState(isLoading = false, wordState = WordState.NotLoaded)

        val result = initial.enableWordEdit()

        assertEquals(initial, result)
    }
}
