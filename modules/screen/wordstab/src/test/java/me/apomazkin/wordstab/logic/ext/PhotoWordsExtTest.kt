package me.apomazkin.wordstab.logic.ext

import me.apomazkin.wordstab.logic.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 1. showPhotoSourceChooser открывает диалог на шаге выбора
 * 2. startRecognizing переводит на шаг распознавания и сбрасывает слова и ошибку
 * 3. showRecognizedWords показывает список, флаги шагов сняты
 * 4. showRecognitionError показывает ошибку, слова пусты
 * 5. hidePhotoWords закрывает диалог и сбрасывает всё, кроме requestId
 * 6. requestId растёт на открытии выбора и на старте распознавания, на других шагах нет
 * 7. остальные поля состояния не меняются
 */
class PhotoWordsExtTest {

    private val baseState = WordsTabState(
        isLoading = false,
        snackbarState = SnackbarState(title = "test", show = true),
    )

    @Test
    fun `should open dialog on source chooser step`() {
        val result = baseState.showPhotoSourceChooser()

        assertTrue(result.photoWordsState.isOpen)
        assertTrue(result.photoWordsState.isChoosingSource)
        assertFalse(result.photoWordsState.isRecognizing)
    }

    @Test
    fun `should switch to recognizing step and drop previous result`() {
        val result = baseState
            .showRecognitionError(errorRes = 1)
            .startRecognizing()

        assertTrue(result.photoWordsState.isRecognizing)
        assertFalse(result.photoWordsState.isChoosingSource)
        assertNull(result.photoWordsState.errorRes)
        assertTrue(result.photoWordsState.words.isEmpty())
    }

    @Test
    fun `should show words and clear step flags`() {
        val result = baseState.startRecognizing().showRecognizedWords(listOf("hello", "world"))

        assertEquals(listOf("hello", "world"), result.photoWordsState.words)
        assertTrue(result.photoWordsState.isOpen)
        assertFalse(result.photoWordsState.isRecognizing)
        assertNull(result.photoWordsState.errorRes)
    }

    @Test
    fun `should show error without words`() {
        val result = baseState.startRecognizing().showRecognitionError(errorRes = 7)

        assertEquals(7, result.photoWordsState.errorRes)
        assertTrue(result.photoWordsState.words.isEmpty())
        assertFalse(result.photoWordsState.isRecognizing)
    }

    @Test
    fun `should close dialog and reset everything but request id`() {
        val result = baseState
            .showPhotoSourceChooser()
            .startRecognizing()
            .showRecognizedWords(listOf("hello"))
            .hidePhotoWords()

        assertEquals(PhotoWordsState(requestId = 2L), result.photoWordsState)
    }

    @Test
    fun `should increment request id on chooser and on recognizing start only`() {
        val chooser = baseState.showPhotoSourceChooser()
        val recognizing = chooser.startRecognizing()
        val shown = recognizing.showRecognizedWords(listOf("hello"))
        val reopened = shown.hidePhotoWords().showPhotoSourceChooser()

        assertEquals(1L, chooser.photoWordsState.requestId)
        assertEquals(2L, recognizing.photoWordsState.requestId)
        assertEquals(2L, shown.photoWordsState.requestId)
        assertEquals(3L, reopened.photoWordsState.requestId)
        assertTrue(recognizing.isCurrentPhotoRequest(2L))
        assertFalse(reopened.isCurrentPhotoRequest(2L))
        assertFalse(shown.hidePhotoWords().isCurrentPhotoRequest(2L))
    }

    @Test
    fun `should not touch other fields`() {
        val result = baseState.showPhotoSourceChooser()

        assertEquals(baseState.isLoading, result.isLoading)
        assertEquals(baseState.snackbarState, result.snackbarState)
        assertEquals(baseState.addWordDialogState, result.addWordDialogState)
        assertEquals(baseState.topBarState, result.topBarState)
    }
}
