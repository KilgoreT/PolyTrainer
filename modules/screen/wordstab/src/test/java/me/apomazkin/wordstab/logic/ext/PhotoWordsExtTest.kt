package me.apomazkin.wordstab.logic.ext

import me.apomazkin.wordstab.deps.PhotoWord
import me.apomazkin.wordstab.deps.PhotoWordStatus
import me.apomazkin.wordstab.logic.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 1. showPhotoSourceChooser открывает диалог на шаге выбора
 * 2. startRecognizing переводит в фазу распознавания и сбрасывает слова и ошибку
 * 3. startLooking переводит в фазу поиска тем же запуском
 * 4. showPhotoWords показывает список, индикатора нет
 * 5. showNoPhotoWords и showPhotoError — без слов и без индикатора
 * 6. hidePhotoWords закрывает диалог и сбрасывает всё, кроме requestId
 * 7. requestId растёт на открытии выбора и на старте распознавания, на других шагах нет
 * 8. остальные поля состояния не меняются
 */
class PhotoWordsExtTest {

    private val baseState = WordsTabState(
        isLoading = false,
        snackbarState = SnackbarState(title = "test", show = true),
    )

    private val hello = PhotoWord(text = "hello", status = PhotoWordStatus.FOUND, lemma = "hello", partOfSpeech = "noun", meaning = "привет", gloss = "greeting")

    @Test
    fun `should open dialog on source chooser step`() {
        val result = baseState.showPhotoSourceChooser()

        assertTrue(result.photoWordsState.isOpen)
        assertTrue(result.photoWordsState.isChoosingSource)
        assertFalse(result.photoWordsState.isBusy)
    }

    @Test
    fun `should switch to recognizing phase and drop previous result`() {
        val result = baseState
            .showPhotoError(errorRes = 1)
            .startRecognizing()

        assertEquals(PhotoPhase.RECOGNIZING, result.photoWordsState.phase)
        assertFalse(result.photoWordsState.isChoosingSource)
        assertNull(result.photoWordsState.errorRes)
        assertTrue(result.photoWordsState.words.isEmpty())
    }

    @Test
    fun `should switch to looking phase within the same run`() {
        val recognizing = baseState.startRecognizing()

        val result = recognizing.startLooking()

        assertEquals(PhotoPhase.LOOKING, result.photoWordsState.phase)
        assertEquals(recognizing.photoWordsState.requestId, result.photoWordsState.requestId)
    }

    @Test
    fun `should show words without the indicator`() {
        val result = baseState.startRecognizing().startLooking().showPhotoWords(listOf(hello))

        assertEquals(listOf(hello), result.photoWordsState.words)
        assertTrue(result.photoWordsState.isOpen)
        assertFalse(result.photoWordsState.isBusy)
        assertNull(result.photoWordsState.errorRes)
    }

    @Test
    fun `should show empty result and error without words and indicator`() {
        val empty = baseState.startRecognizing().showNoPhotoWords()
        val error = baseState.startRecognizing().showPhotoError(errorRes = 7)

        assertTrue(empty.photoWordsState.isEmptyResult)
        assertFalse(empty.photoWordsState.isBusy)
        assertEquals(7, error.photoWordsState.errorRes)
        assertTrue(error.photoWordsState.words.isEmpty())
        assertFalse(error.photoWordsState.isBusy)
    }

    @Test
    fun `should close dialog and reset everything but request id`() {
        val result = baseState
            .showPhotoSourceChooser()
            .startRecognizing()
            .showPhotoWords(listOf(hello))
            .hidePhotoWords()

        assertEquals(PhotoWordsState(requestId = 2L), result.photoWordsState)
    }

    @Test
    fun `should increment request id on chooser and on recognizing start only`() {
        val chooser = baseState.showPhotoSourceChooser()
        val recognizing = chooser.startRecognizing()
        val shown = recognizing.startLooking().showPhotoWords(listOf(hello))
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
