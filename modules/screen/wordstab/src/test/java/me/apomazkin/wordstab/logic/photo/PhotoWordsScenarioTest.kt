package me.apomazkin.wordstab.logic.photo

import io.github.kilgoret.mate.state
import io.github.kilgoret.mate.test.testReduce
import me.apomazkin.lexeme.ChoiceValues
import me.apomazkin.lexeme.TextValues
import me.apomazkin.wordstab.deps.PhotoDataKind
import me.apomazkin.wordstab.deps.PhotoTarget
import me.apomazkin.wordstab.logic.DatasourceEffect
import me.apomazkin.wordstab.logic.Msg
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Сценарные цепочки Msg через окно: ответы handler'а подаются вручную. */
class PhotoWordsScenarioTest {

    private val reducer = reducer()

    @Test
    fun `scenario - three brief cases in one window - new word, lexeme into an existing word, whole word`() {
        // run — новое слово с одной лексемой и частью речи; bank — в существующее; fuera — целиком.
        val window = reducer.window(Fx.ran, Fx.bank, Fx.fuera)
        val runNew = reducer.step(window, Msg.SetPhotoCardTarget("run", PhotoCardTarget.NewWord))
        val runTrimmed = listOf("ru/v2", "ru/v3", "ru/n1", "en/a1").fold(runNew) { state, key -> reducer.step(state, Msg.DeletePhotoLexeme("run", key)) }
        val bankTrimmed = listOf("en/b1", "en/bu1").fold(runTrimmed) { state, key -> reducer.step(state, Msg.DeletePhotoLexeme("bank", key)) }
        val asking = reducer.step(bankTrimmed, Msg.RequestPhotoAdd)

        val result = reducer.testReduce(asking, Msg.ConfirmPhotoAdd)

        val batch = (result.second.single() as DatasourceEffect.AddPhotoWords).batch
        assertEquals(listOf("run", "bank", "fuera", "ser", "ir"), batch.words.map { it.text })
        assertEquals(null, batch.words[0].wordId)
        assertEquals(listOf(Fx.TRANSLATION to TextValues(me.apomazkin.lexeme.Primitive.Text("бежать, бегать")), Fx.PART_OF_SPEECH to ChoiceValues(Fx.VERB)), batch.words[0].lexemes.single().components.map { it.typeId to it.data })
        assertEquals(Fx.BANK_1, batch.words[1].wordId)
        assertEquals(1, batch.words[1].lexemes.size)
        assertEquals(2, batch.words[2].lexemes.size)
        assertEquals("Добавить 6 значений в 5 слов", "Добавить ${countPhrase(asking.photo().summary.lexemeCount, asking.photo().summary.wordCount)}")
    }

    @Test
    fun `scenario - mapping change then write then success closes the window and the next run starts clean`() {
        val window = reducer.window(Fx.ran)
        val remapped = reducer.step(window, Msg.SetPhotoMapping(PhotoDataKind.GLOSS, PhotoTarget.Component(Fx.DEFINITION)))
        val adding = reducer.step(reducer.step(remapped, Msg.RequestPhotoAdd), Msg.ConfirmPhotoAdd)
        val done = reducer.step(adding, Msg.PhotoAddFinished(5, 1, adding.photo().requestId))

        val picked = reducer.step(reducer.step(done, Msg.OpenPhotoSourceChooser), Msg.PhotoPicked("content://photo/2"))
        val runId = picked.photo().requestId
        val answered = reducer.step(
            reducer.step(picked, Msg.TokensReady(listOf(me.apomazkin.textrecognition.RecognizedWord("bank", 0.9f, "en")), runId)),
            Msg.WordsLookedUp(me.apomazkin.wordstab.deps.PhotoLookupOutcome.Words(Fx.DICT, listOf(Fx.bank)), runId),
        )
        val next = reducer.step(answered, Msg.PhotoFormLoaded(me.apomazkin.wordstab.deps.PhotoFormOutcome.Loaded(Fx.form(), null), runId))

        assertFalse(done.photo().isOpen)
        assertEquals(adding.photo().requestId, done.photo().requestId)
        assertTrue(next.photo().requestId > done.photo().requestId)
        assertEquals(Fx.defaultMapping, next.photo().mapping)
        assertEquals(null, next.photo().commonGroupId)
    }

    @Test
    fun `scenario - rejected by a dead group, group reset, retry succeeds`() {
        val window = reducer.step(reducer.window(Fx.ran), Msg.SetPhotoCommonGroup(Fx.POTTER))
        val adding = reducer.step(reducer.step(window, Msg.RequestPhotoAdd), Msg.ConfirmPhotoAdd)
        val rejected = reducer.step(adding, Msg.PhotoAddRejected(PhotoAddFailure.GroupMissing(Fx.POTTER), adding.photo().requestId))

        val retry = reducer.testReduce(rejected, Msg.RetryPhotoAdd)
        val done = reducer.step(retry.state(), Msg.PhotoAddFinished(5, 1, adding.photo().requestId))

        val batch = (retry.second.single() as DatasourceEffect.AddPhotoWords).batch
        assertTrue(batch.words.all { it.groupId == null })
        assertFalse(done.photo().isOpen)
    }

    @Test
    fun `scenario - deletions then back asks, stay keeps, close drops without a write`() {
        val window = reducer.window(Fx.ran, Fx.bank)
        val edited = reducer.step(reducer.step(window, Msg.DeletePhotoCard("bank")), Msg.DeletePhotoLexeme("run", "ru/v1"))
        val asked = reducer.step(edited, Msg.RequestClosePhotoWords)
        val stayed = reducer.step(asked, Msg.StayInPhotoWords)

        val closed = reducer.testReduce(reducer.step(stayed, Msg.RequestClosePhotoWords), Msg.ConfirmClosePhotoWords)

        assertEquals(listOf("run"), stayed.photo().cards.map { it.lemma })
        assertEquals(4, stayed.card("run").lexemes.size)
        assertFalse(closed.state().photo().isOpen)
        assertTrue(closed.second.isEmpty())
    }
}
