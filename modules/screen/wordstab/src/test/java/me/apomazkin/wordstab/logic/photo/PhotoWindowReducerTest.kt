package me.apomazkin.wordstab.logic.photo

import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.state
import io.github.kilgoret.mate.test.assertNoEffects
import io.github.kilgoret.mate.test.testReduce
import me.apomazkin.wordstab.deps.PhotoDataKind
import me.apomazkin.wordstab.deps.PhotoFormOutcome
import me.apomazkin.wordstab.deps.PhotoTarget
import me.apomazkin.wordstab.logic.DatasourceEffect
import me.apomazkin.wordstab.logic.Msg
import me.apomazkin.wordstab.logic.PhotoPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Окно добавления глазами reducer'а: форма (AWP-24), карточки (AWP-04,
 * 05), действия Ш3 (AWP-15…18), соответствия и группы (AWP-06, 08, 09,
 * 28), пометки (AWP-07, 11), сводка (AWP-13). Адреса — `AWP-NN_k`.
 */
class PhotoWindowReducerTest {

    private val reducer = reducer()

    // ===== Ш1 → Ш2 / В1 =====

    @Test
    fun `AWP-04_1 AWP-05_1 loaded form builds cards with five lexemes per card and the rest counted`() {
        val window = reducer.window(Fx.ran, Fx.running)

        assertEquals(PhotoPhase.IDLE, window.photo().phase)
        assertEquals(listOf("run", "running"), window.photo().cards.map { it.lemma })
        assertEquals(listOf("ru/v1", "ru/v2", "ru/v3", "ru/n1", "en/a1"), window.card("run").lexemes.map { it.key })
        assertEquals(2, window.card("run").hiddenCount)
        assertEquals(listOf("ran", "running"), window.card("run").sourceWords)
    }

    @Test
    fun `AWP-06_1 AWP-06_8 loaded form gives default mapping and common group all even after a remembered group last time`() {
        val window = reducer.window(Fx.ran)

        assertEquals(Fx.defaultMapping, window.photo().mapping)
        assertNull(window.photo().commonGroupId)
    }

    @Test
    fun `AWP-07_1 AWP-07_2 loaded form marks existing lemmas and lexemes`() {
        val window = reducer.window(Fx.ran, Fx.bank)

        assertEquals(1, window.card("run").existingCount)
        assertEquals(2, window.card("bank").existingCount)
        assertTrue(window.lexeme("run", "ru/v2").exists)
        assertFalse(window.lexeme("run", "ru/v1").exists)
    }

    @Test
    fun `AWP-13_1 loaded window counts the button`() {
        val window = reducer.window(Fx.ran, Fx.bank)

        assertEquals(6, window.photo().summary.lexemeCount)
        assertEquals(2, window.photo().summary.wordCount)
        assertEquals(2, window.photo().summary.existingCount)
        assertTrue(window.photo().summary.isEnabled)
    }

    @Test
    fun `AWP-24_1 AWP-24_4 failed form and missing dictionary show the error without the indicator`() {
        val answered = reducer.answered(Fx.ran)
        val runId = answered.photo().requestId

        val failed = reducer.step(answered, Msg.PhotoFormLoaded(PhotoFormOutcome.Failed("boom"), runId))
        val missing = reducer.step(answered, Msg.PhotoFormLoaded(PhotoFormOutcome.DictionaryMissing, runId))

        assertEquals(PhotoFormError.LOAD_FAILED, failed.photo().formError)
        assertEquals(PhotoFormError.DICTIONARY_MISSING, missing.photo().formError)
        assertEquals(PhotoPhase.IDLE, failed.photo().phase)
        assertTrue(failed.photo().cards.isEmpty())
    }

    @Test
    fun `AWP-24_2 AWP-24_3 form of a foreign run is dropped and closing during loading drops the result`() {
        val answered = reducer.answered(Fx.ran)
        val closed = reducer.step(answered, Msg.RequestClosePhotoWords)

        val foreign = reducer.testReduce(answered, Msg.PhotoFormLoaded(PhotoFormOutcome.Loaded(Fx.form(), null), requestId = 99L))
        val late = reducer.step(closed, Msg.PhotoFormLoaded(PhotoFormOutcome.Loaded(Fx.form(), null), answered.photo().requestId))

        assertEquals(answered, foreign.state())
        foreign.assertNoEffects()
        assertFalse(closed.photo().isOpen)
        assertEquals(closed, late)
    }

    @Test
    fun `AWP-12_1 AWP-12_3 not found and error words go to not added and no cards expands the block`() {
        val withCards = reducer.window(Fx.ran, Fx.notFound, Fx.errorWord)
        val noCards = reducer.window(Fx.notFound, Fx.errorWord)

        assertEquals(listOf("qwrt", "Grimmauld"), withCards.photo().notAdded.map { it.text })
        assertFalse(withCards.photo().isNotAddedExpanded)
        assertTrue(noCards.photo().isNotAddedExpanded)
        assertFalse(noCards.photo().summary.isEnabled)
    }

    // ===== Ш3: удаление =====

    @Test
    fun `AWP-15_1 AWP-05_4 deleting a lexeme leaves four and the rest unchanged`() {
        val window = reducer.window(Fx.ran)

        val result = reducer.step(window, Msg.DeletePhotoLexeme("run", "ru/v2"))

        assertEquals(4, result.card("run").lexemes.size)
        assertEquals(2, result.card("run").hiddenCount)
        assertTrue(result.photo().isDirty)
        assertEquals(4, result.photo().summary.lexemeCount)
    }

    @Test
    fun `AWP-15_3 AWP-15_4 deleting all lexemes keeps an empty card, deleting a card keeps the others`() {
        val window = reducer.window(Fx.ran, Fx.running)
        val emptied = listOf("ru/v1", "ru/v2", "ru/v3", "ru/n1", "en/a1").fold(window) { state, key -> reducer.step(state, Msg.DeletePhotoLexeme("run", key)) }

        val cardGone = reducer.step(window, Msg.DeletePhotoCard("run"))

        assertTrue(emptied.card("run").lexemes.isEmpty())
        assertEquals(2, emptied.card("run").hiddenCount)
        assertEquals(listOf("running"), cardGone.photo().cards.map { it.lemma })
        assertTrue(cardGone.photo().notAdded.isEmpty())
    }

    @Test
    fun `AWP-15_5 AWP-13_2 deleting everything disables the button`() {
        val window = reducer.window(Fx.ran)

        val result = reducer.step(window, Msg.DeletePhotoCard("run"))

        assertEquals(0, result.photo().summary.lexemeCount)
        assertFalse(result.photo().summary.isEnabled)
    }

    @Test
    fun `AWP-15_2 deleting an example keeps the lexeme`() {
        val window = reducer.window(Fx.word("run", me.apomazkin.wordstab.deps.PhotoLemma("run", null, listOf(Fx.runLexemes[4]))))

        val result = reducer.step(window, Msg.DeletePhotoExample("run", "en/v1", 0))

        assertEquals(listOf("She ran home."), result.lexeme("run", "en/v1").examples.map { it.text })
    }

    // ===== Ш3: правка =====

    @Test
    fun `AWP-16_1 AWP-16_2 submitted edit lands trimmed with the edited mark and the sheet closes`() {
        val window = reducer.window(Fx.ran)
        val opened = reducer.step(window, Msg.OpenPhotoSheet(PhotoSheet.TextEdit("run", "ru/v1", PhotoTextField.Translation, "бежать, бегать")))
        val typed = reducer.step(opened, Msg.UpdatePhotoDraft("  брать "))

        val result = reducer.step(typed, Msg.SubmitPhotoText)

        assertEquals("брать", result.lexeme("run", "ru/v1").translation.text)
        assertTrue(result.lexeme("run", "ru/v1").translation.isEdited)
        assertNull(result.photo().sheet)
        assertTrue(result.photo().isDirty)
    }

    @Test
    fun `AWP-16_4 AWP-11_5 translation emptied marks the lexeme without a core and disables the button`() {
        val window = reducer.window(Fx.ran)
        val opened = reducer.step(window, Msg.OpenPhotoSheet(PhotoSheet.TextEdit("run", "ru/v1", PhotoTextField.Translation, "бежать, бегать")))

        val result = reducer.step(reducer.step(opened, Msg.UpdatePhotoDraft("   ")), Msg.SubmitPhotoText)

        assertTrue(result.lexeme("run", "ru/v1").lacksCore)
        assertEquals(1, result.photo().summary.lacksCoreCount)
        assertFalse(result.photo().summary.isEnabled)
    }

    @Test
    fun `AWP-16_5 example emptied is deleted`() {
        val window = reducer.window(Fx.word("run", me.apomazkin.wordstab.deps.PhotoLemma("run", null, listOf(Fx.runLexemes[4]))))
        val opened = reducer.step(window, Msg.OpenPhotoSheet(PhotoSheet.TextEdit("run", "en/v1", PhotoTextField.Example(0), "Run, Sarah, run!")))

        val result = reducer.step(reducer.step(opened, Msg.UpdatePhotoDraft("")), Msg.SubmitPhotoText)

        assertEquals(listOf("She ran home."), result.lexeme("run", "en/v1").examples.map { it.text })
    }

    @Test
    fun `AWP-16_6 reset returns the source text without the edited mark`() {
        val window = reducer.window(Fx.ran)
        val opened = reducer.step(window, Msg.OpenPhotoSheet(PhotoSheet.TextEdit("run", "ru/v1", PhotoTextField.Translation, "x")))
        val edited = reducer.step(reducer.step(opened, Msg.UpdatePhotoDraft("брать")), Msg.SubmitPhotoText)

        val result = reducer.step(edited, Msg.ResetPhotoText("run", "ru/v1", PhotoTextField.Translation))

        assertEquals("бежать, бегать", result.lexeme("run", "ru/v1").translation.text)
        assertFalse(result.lexeme("run", "ru/v1").translation.isEdited)
    }

    @Test
    fun `AWP-16_7 AWP-07_7 english fallback edits its gloss as the translation text and exists follows`() {
        val definitions = Fx.form(existing = listOf(Fx.existing(5L, "run", listOf("liquid"))))
        val window = reducer.window(Fx.ran, form = definitions)
        val opened = reducer.step(window, Msg.OpenPhotoSheet(PhotoSheet.TextEdit("run", "en/a1", PhotoTextField.Translation, "x")))

        val result = reducer.step(reducer.step(opened, Msg.UpdatePhotoDraft("liquid")), Msg.SubmitPhotoText)

        assertEquals("liquid", result.lexeme("run", "en/a1").translation.text)
        assertTrue(result.lexeme("run", "en/a1").exists)
    }

    // ===== Ш3: часть речи, куда класть, группы =====

    @Test
    fun `AWP-17_1 AWP-19_7 part of speech changes one lexeme and counts as a change`() {
        val window = reducer.window(Fx.ran)

        val result = reducer.step(window, Msg.SetPhotoPartOfSpeech("run", "ru/v1", Fx.NOUN))

        assertEquals(Fx.NOUN, result.lexeme("run", "ru/v1").partOfSpeechOptionId)
        assertEquals(Fx.VERB, result.lexeme("run", "ru/v2").partOfSpeechOptionId)
        assertTrue(result.photo().isDirty)
    }

    @Test
    fun `AWP-17_3 AWP-06_6 part of speech disabled gives no option targets`() {
        val window = reducer.window(Fx.ran, form = Fx.form(components = listOf(Fx.translation, Fx.definition, Fx.example), options = emptyList()))

        assertEquals(PhotoTarget.None, window.photo().mapping.partOfSpeech)
        assertNull(window.lexeme("run", "ru/v1").partOfSpeechOptionId)
    }

    @Test
    fun `AWP-18_1 AWP-18_4 AWP-19_8 target switches between existing lemmas and a new word without counting as a change`() {
        val window = reducer.window(Fx.bank)

        val second = reducer.step(window, Msg.SetPhotoCardTarget("bank", PhotoCardTarget.Existing(Fx.BANK_2)))
        val fresh = reducer.step(second, Msg.SetPhotoCardTarget("bank", PhotoCardTarget.NewWord))

        assertEquals(PhotoCardTarget.Existing(Fx.BANK_1), window.card("bank").target)
        assertEquals(PhotoCardTarget.Existing(Fx.BANK_2), second.card("bank").target)
        assertEquals(PhotoCardTarget.NewWord, fresh.card("bank").target)
        assertFalse(fresh.photo().isDirty)
    }

    @Test
    fun `AWP-08_1 AWP-08_5 AWP-19_6 common and own groups are set and reset without counting as a change`() {
        val window = reducer.window(Fx.ran, Fx.bank)

        val common = reducer.step(window, Msg.SetPhotoCommonGroup(Fx.POTTER))
        val own = reducer.step(common, Msg.SetPhotoCardGroup("bank", PhotoOwnGroup.Group(Fx.FINANCE)))
        val cleared = reducer.step(own, Msg.SetPhotoCardGroup("bank", PhotoOwnGroup.AsCommon))

        assertEquals(Fx.POTTER, common.photo().commonGroupId)
        assertEquals(PhotoOwnGroup.Group(Fx.FINANCE), own.card("bank").ownGroup)
        assertEquals(PhotoOwnGroup.AsCommon, cleared.card("bank").ownGroup)
        assertFalse(cleared.photo().isDirty)
    }

    @Test
    fun `AWP-08_3 dictionary without groups offers only all`() {
        val window = reducer.window(Fx.ran, form = Fx.form(groups = emptyList()))

        assertTrue(window.photo().form!!.groups.isEmpty())
        assertNull(window.photo().commonGroupId)
    }

    // ===== Ш3: соответствия =====

    @Test
    fun `AWP-09_2 AWP-06 chosen mapping is saved for the dictionary and closes the sheet`() {
        val window = reducer.step(reducer.window(Fx.ran), Msg.OpenPhotoSheet(PhotoSheet.MappingTarget(PhotoDataKind.GLOSS)))

        val result = reducer.testReduce(window, Msg.SetPhotoMapping(PhotoDataKind.GLOSS, PhotoTarget.Component(Fx.DEFINITION)))

        val expected = Fx.defaultMapping.copy(gloss = PhotoTarget.Component(Fx.DEFINITION))
        assertEquals(expected, result.state().photo().mapping)
        assertEquals(setOf<Effect>(DatasourceEffect.SavePhotoMapping(Fx.DICT, expected)), result.second)
        assertNull(result.state().photo().sheet)
        assertFalse(result.state().photo().isDirty)
    }

    @Test
    fun `AWP-28_1 AWP-11_2 AWP-11_4 taking the translation component for the gloss frees translation and marks lexemes without a gloss`() {
        val window = reducer.window(Fx.ran)

        val taken = reducer.step(window, Msg.SetPhotoMapping(PhotoDataKind.GLOSS, PhotoTarget.Component(Fx.TRANSLATION)))
        val restored = reducer.step(taken, Msg.SetPhotoMapping(PhotoDataKind.TRANSLATION, PhotoTarget.Component(Fx.TRANSLATION)))

        assertEquals(PhotoTarget.None, taken.photo().mapping.translation)
        assertTrue(taken.lexeme("run", "ru/v1").lacksCore)
        assertFalse(taken.lexeme("run", "en/a1").lacksCore)
        assertFalse(taken.photo().summary.isEnabled)
        assertFalse(restored.lexeme("run", "ru/v1").lacksCore)
        assertTrue(restored.photo().summary.isEnabled)
    }

    @Test
    fun `AWP-11_3 deleting the lexemes without a core enables the button again`() {
        val window = reducer.window(Fx.ran)
        val marked = reducer.step(window, Msg.SetPhotoMapping(PhotoDataKind.TRANSLATION, PhotoTarget.None))
        val withGloss = reducer.step(marked, Msg.SetPhotoMapping(PhotoDataKind.GLOSS, PhotoTarget.Component(Fx.DEFINITION)))

        val result = listOf("ru/v1", "ru/v3", "ru/n1").fold(withGloss) { state, key -> reducer.step(state, Msg.DeletePhotoLexeme("run", key)) }

        assertTrue(withGloss.lexeme("run", "ru/v1").lacksCore)
        assertEquals(3, withGloss.photo().summary.lacksCoreCount)
        assertFalse(withGloss.photo().summary.isEnabled)
        assertEquals(0, result.photo().summary.lacksCoreCount)
        assertEquals(1, result.photo().summary.lexemeCount)
        assertTrue(result.photo().summary.isEnabled)
    }

    @Test
    fun `AWP-11_7 AWP-11_8 no kind reaching a core makes the summary red`() {
        val window = reducer.window(Fx.ran)

        val result = reducer.step(reducer.step(window, Msg.SetPhotoMapping(PhotoDataKind.TRANSLATION, PhotoTarget.Component(Fx.HINT))), Msg.SetPhotoMapping(PhotoDataKind.GLOSS, PhotoTarget.None))

        assertTrue(result.photo().summary.noCoreMapping)
        assertFalse(result.photo().summary.isEnabled)
    }

    @Test
    fun `AWP-09_5 AWP-12_2 blocks toggle`() {
        val window = reducer.window(Fx.ran, Fx.notFound)

        val mapping = reducer.step(window, Msg.TogglePhotoMapping)
        val notAdded = reducer.step(mapping, Msg.TogglePhotoNotAdded)

        assertTrue(mapping.photo().isMappingExpanded)
        assertTrue(notAdded.photo().isNotAddedExpanded)
        assertFalse(reducer.step(notAdded, Msg.TogglePhotoNotAdded).photo().isNotAddedExpanded)
    }

    @Test
    fun `AWP-05_5 card toggles and nothing else reveals hidden lexemes`() {
        val window = reducer.window(Fx.ran)

        val expanded = reducer.step(window, Msg.TogglePhotoCard("run"))

        assertTrue(expanded.card("run").isExpanded)
        assertEquals(VISIBLE_LEXEMES, expanded.card("run").lexemes.size)
        assertFalse(reducer.step(expanded, Msg.TogglePhotoCard("run")).card("run").isExpanded)
    }
}
