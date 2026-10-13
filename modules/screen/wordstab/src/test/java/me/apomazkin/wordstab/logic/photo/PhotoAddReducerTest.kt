package me.apomazkin.wordstab.logic.photo

import io.github.kilgoret.mate.state
import io.github.kilgoret.mate.test.assertNoEffects
import io.github.kilgoret.mate.test.testReduce
import me.apomazkin.wordstab.deps.PhotoDataKind
import me.apomazkin.wordstab.deps.PhotoTarget
import me.apomazkin.wordstab.logic.DatasourceEffect
import me.apomazkin.wordstab.logic.Msg
import me.apomazkin.wordstab.logic.UiEffect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Добавление и закрытие глазами reducer'а: вопрос и запись (AWP-13,
 * AWP-20), один пакет на нажатие (AWP-02), отказ и повтор (AWP-21,
 * AWP-26), после записи (AWP-23), закрытие с вопросом (AWP-19, AWP-25).
 */
class PhotoAddReducerTest {

    private val reducer = reducer()

    private fun confirmed(window: me.apomazkin.wordstab.logic.WordsTabState) =
        reducer.testReduce(reducer.step(window, Msg.RequestPhotoAdd), Msg.ConfirmPhotoAdd)

    // ===== Ш4: вопрос и запись =====

    @Test
    fun `AWP-13_1 pressing add asks the question with the counts`() {
        val window = reducer.window(Fx.ran, Fx.bank)

        val result = reducer.testReduce(window, Msg.RequestPhotoAdd)

        assertTrue(result.state().photo().isConfirmingAdd)
        assertEquals(6, result.state().photo().summary.lexemeCount)
        result.assertNoEffects()
    }

    @Test
    fun `AWP-13_7 cancel keeps the window and writes nothing`() {
        val asking = reducer.step(reducer.window(Fx.ran), Msg.RequestPhotoAdd)

        val result = reducer.testReduce(asking, Msg.CancelPhotoAdd)

        assertFalse(result.state().photo().isConfirmingAdd)
        assertEquals(asking.photo().cards, result.state().photo().cards)
        result.assertNoEffects()
    }

    @Test
    fun `AWP-20_1 AWP-13_4 yes starts one batch write and the window goes busy`() {
        val window = reducer.step(reducer.window(Fx.ran, Fx.bank), Msg.SetPhotoCommonGroup(Fx.POTTER))

        val result = confirmed(window)

        val effect = result.second.single() as DatasourceEffect.AddPhotoWords
        assertTrue(result.state().photo().isAdding)
        assertFalse(result.state().photo().isConfirmingAdd)
        assertEquals(window.photo().requestId, effect.requestId)
        assertEquals(listOf("run", "bank"), effect.batch.words.map { it.text })
        assertEquals(listOf(Fx.RUN_WORD, Fx.BANK_1), effect.batch.words.map { it.wordId })
        assertEquals(listOf(4, 2), effect.batch.words.map { it.lexemes.size })
        assertTrue(effect.batch.words.all { it.groupId == Fx.POTTER })
    }

    @Test
    fun `AWP-02_1 second press during the write does nothing`() {
        val adding = confirmed(reducer.window(Fx.ran)).state()

        val again = reducer.testReduce(adding, Msg.RequestPhotoAdd)
        val confirmAgain = reducer.testReduce(adding, Msg.ConfirmPhotoAdd)

        assertEquals(adding, again.state())
        again.assertNoEffects()
        confirmAgain.assertNoEffects()
    }

    @Test
    fun `AWP-13_2 AWP-13_3 disabled button ignores the press`() {
        val empty = reducer.step(reducer.window(Fx.ran), Msg.DeletePhotoCard("run"))
        val noCore = reducer.step(reducer.step(reducer.window(Fx.ran), Msg.SetPhotoMapping(PhotoDataKind.TRANSLATION, PhotoTarget.None)), Msg.SetPhotoMapping(PhotoDataKind.GLOSS, PhotoTarget.Component(Fx.DEFINITION)))

        assertFalse(reducer.step(empty, Msg.RequestPhotoAdd).photo().isConfirmingAdd)
        assertFalse(reducer.step(noCore, Msg.RequestPhotoAdd).photo().isConfirmingAdd)
    }

    @Test
    fun `AWP-03_1 AWP-03_5 the batch carries what is left in the window without the existing lexemes`() {
        val window = reducer.window(Fx.ran)
        val trimmed = listOf("ru/v3", "ru/n1", "en/a1").fold(window) { state, key -> reducer.step(state, Msg.DeletePhotoLexeme("run", key)) }

        val effect = confirmed(trimmed).second.single() as DatasourceEffect.AddPhotoWords

        assertEquals(1, effect.batch.words.single().lexemes.size)
    }

    // ===== Ш5: записано =====

    @Test
    fun `AWP-23_1 finished write closes the window and shows the count`() {
        val adding = confirmed(reducer.window(Fx.ran, Fx.bank)).state()

        val result = reducer.testReduce(adding, Msg.PhotoAddFinished(lexemeCount = 3, wordCount = 2, requestId = adding.photo().requestId))

        assertFalse(result.state().photo().isOpen)
        assertEquals(setOf<io.github.kilgoret.mate.Effect>(UiEffect.ShowNotification("Добавлено 3 значения в 2 слова")), result.second)
    }

    @Test
    fun `AWP-24_2 finished write of a foreign run is ignored`() {
        val adding = confirmed(reducer.window(Fx.ran)).state()

        val result = reducer.testReduce(adding, Msg.PhotoAddFinished(1, 1, requestId = 99L))

        assertEquals(adding, result.state())
        result.assertNoEffects()
    }

    // ===== В3: отказ и повтор =====

    @Test
    fun `AWP-21_1 failed write keeps everything and offers retry`() {
        val adding = confirmed(reducer.window(Fx.ran, Fx.bank)).state()

        val result = reducer.step(adding, Msg.PhotoAddRejected(PhotoAddFailure.Failed, adding.photo().requestId))

        assertTrue(result.photo().isOpen)
        assertFalse(result.photo().isAdding)
        assertEquals(PhotoAddFailure.Failed, result.photo().addFailure)
        assertEquals(adding.photo().cards, result.photo().cards)
    }

    @Test
    fun `AWP-21_3 dead common group resets to all and keeps own groups of other cards`() {
        val window = reducer.step(reducer.step(reducer.window(Fx.ran, Fx.bank), Msg.SetPhotoCommonGroup(Fx.POTTER)), Msg.SetPhotoCardGroup("bank", PhotoOwnGroup.Group(Fx.FINANCE)))
        val adding = confirmed(window).state()

        val result = reducer.step(adding, Msg.PhotoAddRejected(PhotoAddFailure.GroupMissing(Fx.POTTER), adding.photo().requestId))

        assertNull(result.photo().commonGroupId)
        assertEquals(PhotoOwnGroup.Group(Fx.FINANCE), result.card("bank").ownGroup)
        assertEquals(PhotoAddFailure.GroupMissing(Fx.POTTER), result.photo().addFailure)
    }

    @Test
    fun `AWP-21_4 dead target lemma resets the card to a new word`() {
        val adding = confirmed(reducer.window(Fx.bank)).state()

        val result = reducer.step(adding, Msg.PhotoAddRejected(PhotoAddFailure.WordMissing(Fx.BANK_1), adding.photo().requestId))

        assertEquals(PhotoCardTarget.NewWord, result.card("bank").target)
    }

    @Test
    fun `AWP-21_5 dead own group falls back to the common one`() {
        val window = reducer.step(reducer.window(Fx.bank), Msg.SetPhotoCardGroup("bank", PhotoOwnGroup.Group(Fx.FINANCE)))
        val adding = confirmed(window).state()

        val result = reducer.step(adding, Msg.PhotoAddRejected(PhotoAddFailure.GroupMissing(Fx.FINANCE), adding.photo().requestId))

        assertEquals(PhotoOwnGroup.AsCommon, result.card("bank").ownGroup)
    }

    @Test
    fun `AWP-21_6 disabled component returns its kind to the default and recounts the marks`() {
        val window = reducer.step(reducer.window(Fx.ran), Msg.SetPhotoMapping(PhotoDataKind.GLOSS, PhotoTarget.Component(Fx.DEFINITION)))
        val adding = confirmed(window).state()

        val result = reducer.step(adding, Msg.PhotoAddRejected(PhotoAddFailure.TypeUnavailable(Fx.DEFINITION), adding.photo().requestId))

        assertEquals(PhotoTarget.None, result.photo().mapping.gloss)
        assertTrue(result.photo().summary.isEnabled)
        assertEquals(PhotoAddFailure.TypeUnavailable(Fx.DEFINITION), result.photo().addFailure)
    }

    @Test
    fun `AWP-26_1 AWP-21_2 AWP-02_2 retry writes again once without the question`() {
        val adding = confirmed(reducer.window(Fx.ran)).state()
        val failed = reducer.step(adding, Msg.PhotoAddRejected(PhotoAddFailure.Failed, adding.photo().requestId))

        val result = reducer.testReduce(failed, Msg.RetryPhotoAdd)

        assertTrue(result.state().photo().isAdding)
        assertNull(result.state().photo().addFailure)
        assertFalse(result.state().photo().isConfirmingAdd)
        assertTrue(result.second.single() is DatasourceEffect.AddPhotoWords)
    }

    // ===== Ш2 → В2: закрытие =====

    @Test
    fun `AWP-19_1 closing an untouched window closes it at once`() {
        val window = reducer.window(Fx.ran)

        val result = reducer.testReduce(window, Msg.RequestClosePhotoWords)

        assertFalse(result.state().photo().isOpen)
        result.assertNoEffects()
    }

    @Test
    fun `AWP-19_2 AWP-25_1 closing after a deletion asks first`() {
        val dirty = reducer.step(reducer.window(Fx.ran), Msg.DeletePhotoLexeme("run", "ru/v1"))

        val result = reducer.step(dirty, Msg.RequestClosePhotoWords)

        assertTrue(result.photo().isOpen)
        assertTrue(result.photo().isConfirmingClose)
    }

    @Test
    fun `AWP-19_3 AWP-19_4 AWP-25_2 stay keeps the window as it was, close drops it`() {
        val dirty = reducer.step(reducer.window(Fx.ran), Msg.DeletePhotoLexeme("run", "ru/v1"))
        val asking = reducer.step(dirty, Msg.RequestClosePhotoWords)

        val stayed = reducer.step(asking, Msg.StayInPhotoWords)
        val closed = reducer.testReduce(asking, Msg.ConfirmClosePhotoWords)

        assertFalse(stayed.photo().isConfirmingClose)
        assertEquals(dirty.photo().cards, stayed.photo().cards)
        assertFalse(closed.state().photo().isOpen)
        closed.assertNoEffects()
    }

    @Test
    fun `AWP-19_5 closing during the write does nothing`() {
        val adding = confirmed(reducer.window(Fx.ran)).state()

        val result = reducer.testReduce(adding, Msg.RequestClosePhotoWords)

        assertEquals(adding, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `AWP-19_6 AWP-19_8 changing groups mapping or target closes without a question`() {
        val window = reducer.window(Fx.ran, Fx.bank)
        val touched = reducer.step(reducer.step(reducer.step(window, Msg.SetPhotoCommonGroup(Fx.POTTER)), Msg.SetPhotoMapping(PhotoDataKind.GLOSS, PhotoTarget.Component(Fx.DEFINITION))), Msg.SetPhotoCardTarget("bank", PhotoCardTarget.NewWord))

        val result = reducer.step(touched, Msg.RequestClosePhotoWords)

        assertFalse(result.photo().isOpen)
    }

    @Test
    fun `AWP-26_2 closing from the failed state asks as after changes when something was deleted`() {
        val dirty = reducer.step(reducer.window(Fx.ran), Msg.DeletePhotoLexeme("run", "ru/v1"))
        val adding = confirmed(dirty).state()
        val failed = reducer.step(adding, Msg.PhotoAddRejected(PhotoAddFailure.Failed, adding.photo().requestId))

        val result = reducer.step(failed, Msg.RequestClosePhotoWords)

        assertTrue(result.photo().isConfirmingClose)
    }
}
