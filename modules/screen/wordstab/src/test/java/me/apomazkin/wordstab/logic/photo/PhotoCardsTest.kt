package me.apomazkin.wordstab.logic.photo

import me.apomazkin.wordstab.deps.PhotoTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Сборка карточек (AWP-04, AWP-05, AWP-10, AWP-12, AWP-18) и пометки (AWP-07, AWP-11, AWP-13). */
class PhotoCardsTest {

    private val form = Fx.form()

    private fun cards(vararg words: me.apomazkin.wordstab.deps.PhotoWord) = buildPhotoCards(words.toList(), form)

    @Test
    fun `AWP-04_1 AWP-04_3 ran and running give cards run and running with source words and no doubled lexemes`() {
        val build = cards(Fx.ran, Fx.running)

        assertEquals(listOf("run", "running"), build.cards.map { it.lemma })
        assertEquals(listOf("ran", "running"), build.cards[0].sourceWords)
        assertEquals(listOf("running"), build.cards[1].sourceWords)
        assertEquals(VISIBLE_LEXEMES, build.cards[0].lexemes.size)
    }

    @Test
    fun `AWP-04_2 fuera gives three cards all from fuera`() {
        val build = cards(Fx.fuera)

        assertEquals(listOf("fuera", "ser", "ir"), build.cards.map { it.lemma })
        assertTrue(build.cards.all { it.sourceWords == listOf("fuera") })
    }

    @Test
    fun `AWP-04_4 answer without found words gives no cards`() {
        val build = cards(Fx.notFound, Fx.errorWord)

        assertTrue(build.cards.isEmpty())
        assertEquals(2, build.notAdded.size)
    }

    @Test
    fun `AWP-04_7 AWP-12_5 lemma without lexemes gets no card and its word goes to not added`() {
        val build = cards(Fx.emptyLemmas)

        assertTrue(build.cards.isEmpty())
        assertEquals(listOf(PhotoNotAdded("void", PhotoNotAddedReason.NO_LEXEMES)), build.notAdded)
    }

    @Test
    fun `AWP-12_1 not found and error words are listed in answer order with reasons`() {
        val build = cards(Fx.notFound, Fx.ran, Fx.errorWord)

        assertEquals(
            listOf(PhotoNotAdded("qwrt", PhotoNotAddedReason.NOT_FOUND), PhotoNotAdded("Grimmauld", PhotoNotAddedReason.ERROR)),
            build.notAdded,
        )
    }

    @Test
    fun `AWP-05_1 run shows one lexeme per part of speech in server order then fills to five and counts the rest`() {
        val run = cards(Fx.ran).cards.single()

        assertEquals(listOf("ru/v1", "ru/v2", "ru/v3", "ru/n1", "en/a1"), run.lexemes.map { it.key })
        assertEquals(2, run.hiddenCount)
    }

    @Test
    fun `AWP-05_3 five or fewer lexemes are all shown without a rest`() {
        val bank = cards(Fx.bank).cards.single()

        assertEquals(3, bank.lexemes.size)
        assertEquals(0, bank.hiddenCount)
    }

    @Test
    fun `AWP-05_6 one part of speech gives the first five in a row`() {
        val lexemes = (1..8).map { Fx.lexeme("ru/n$it", "noun", listOf("значение $it")) }

        val visible = visibleLexemes(lexemes)

        assertEquals((1..5).map { "ru/n$it" }, visible.map { it.key })
    }

    @Test
    fun `AWP-10_1 AWP-10_2 russian lexeme has text only, english one has translations joined and a gloss below with examples`() {
        val run = cards(Fx.ran).cards.single()
        val russian = run.lexemes.first { it.key == "ru/v1" }
        val english = buildPhotoCards(listOf(Fx.word("run", me.apomazkin.wordstab.deps.PhotoLemma("run", null, listOf(Fx.runLexemes[4])))), form)
            .cards.single().lexemes.single()

        assertEquals("бежать, бегать", russian.translation.text)
        assertEquals(null, russian.gloss)
        assertEquals("бегать, побегать, бежать, побежать", english.translation.text)
        assertEquals("To move forward quickly upon two feet…", english.gloss?.text)
        assertEquals(listOf("Run, Sarah, run!", "She ran home."), english.examples.map { it.text })
        assertFalse(english.isEnglishFallback)
    }

    @Test
    fun `AWP-10_3 AWP-10_4 without translations the gloss is the text marked english unless the translation language is english`() {
        val adjective = cards(Fx.ran).cards.single().lexemes.first { it.key == "en/a1" }
        val englishDictionary = buildPhotoCards(listOf(Fx.ran), Fx.form(translationLanguage = ENGLISH)).cards.single().lexemes.first { it.key == "en/a1" }

        assertEquals("In a liquid state; melted or molten.", adjective.translation.text)
        assertTrue(adjective.isEnglishFallback)
        assertEquals(null, adjective.gloss)
        assertFalse(englishDictionary.isEnglishFallback)
    }

    @Test
    fun `AWP-10_5 AWP-10_6 caption and notes stay on the lexeme`() {
        val bank = cards(Fx.bank).cards.single()
        val unmatched = bank.lexemes.first { it.key == "en/bu1" }

        assertEquals("gambling: banker's funds", unmatched.caption)
        assertEquals("ставка", unmatched.translation.text)
    }

    @Test
    fun `AWP-17_2 lexeme without a part of speech key has no option`() {
        val article = Fx.word("el", me.apomazkin.wordstab.deps.PhotoLemma("el", null, listOf(Fx.lexeme("en/el1", null, gloss = "the"))))

        val lexeme = cards(article).cards.single().lexemes.single()

        assertEquals(null, lexeme.partOfSpeechOptionId)
    }

    @Test
    fun `AWP-04_5 part of speech key resolves to the dictionary option`() {
        val run = cards(Fx.ran).cards.single()

        assertEquals(Fx.VERB, run.lexemes.first { it.key == "ru/v1" }.partOfSpeechOptionId)
        assertEquals(Fx.NOUN, run.lexemes.first { it.key == "ru/n1" }.partOfSpeechOptionId)
    }

    @Test
    fun `AWP-07_1 AWP-07_2 existing lemmas mark the card and matching lexemes`() {
        val build = cards(Fx.ran, Fx.bank)

        val run = build.cards.first { it.lemma == "run" }
        val bank = build.cards.first { it.lemma == "bank" }
        val marked = build.cards.withExistsMarks(form)
        assertEquals(1, run.existingCount)
        assertEquals(2, bank.existingCount)
        assertTrue(marked.first { it.lemma == "run" }.lexemes.first { it.key == "ru/v2" }.exists)
        assertFalse(marked.first { it.lemma == "run" }.lexemes.first { it.key == "ru/v1" }.exists)
        assertTrue(marked.first { it.lemma == "bank" }.lexemes.first { it.key == "en/b1" }.exists)
    }

    @Test
    fun `AWP-07_3 AWP-07_4 case stress and edges do not matter for lemma and lexeme`() {
        val upper = Fx.form(existing = listOf(Fx.existing(5L, "Run", listOf(" бежа́ть, бегать "))))

        val marked = buildPhotoCards(listOf(Fx.ran), upper).cards.withExistsMarks(upper)

        assertEquals(1, marked.single().existingCount)
        assertTrue(marked.single().lexemes.first { it.key == "ru/v1" }.exists)
    }

    @Test
    fun `AWP-07_5 definition only dictionary marks by the gloss regardless of mapping`() {
        val definitions = Fx.form(existing = listOf(Fx.existing(5L, "run", listOf("In a liquid state; melted or molten."))))

        val marked = buildPhotoCards(listOf(Fx.ran), definitions).cards.withExistsMarks(definitions)

        assertTrue(marked.single().lexemes.first { it.key == "en/a1" }.exists)
    }

    @Test
    fun `AWP-07_8 no lemma with the text gives no marks`() {
        val build = cards(Fx.fuera)

        val marked = build.cards.withExistsMarks(form)

        assertTrue(marked.all { it.existingCount == 0 })
        assertTrue(marked.flatMap { it.lexemes }.none { it.exists })
    }

    @Test
    fun `AWP-18_1 AWP-18_3 default target is the newest existing lemma, otherwise a new word`() {
        val build = cards(Fx.bank, Fx.fuera)

        assertEquals(PhotoCardTarget.Existing(Fx.BANK_1), build.cards.first { it.lemma == "bank" }.target)
        assertEquals(PhotoCardTarget.NewWord, build.cards.first { it.lemma == "ser" }.target)
    }

    @Test
    fun `AWP-11_1 translation to a core gives no mark to lexemes with a translation or a gloss`() {
        val marked = cards(Fx.ran).cards.withCoreMarks(Fx.defaultMapping, form)

        assertTrue(marked.single().lexemes.none { it.lacksCore })
    }

    @Test
    fun `AWP-11_2 translation none and gloss to a core marks the russian lexeme without a gloss`() {
        val mapping = Fx.defaultMapping.copy(translation = PhotoTarget.None, gloss = PhotoTarget.Component(Fx.DEFINITION))

        val run = cards(Fx.ran).cards.withCoreMarks(mapping, form).single()

        assertTrue(run.lexemes.first { it.key == "ru/v1" }.lacksCore)
        assertFalse(run.lexemes.first { it.key == "en/a1" }.lacksCore)
    }

    @Test
    fun `AWP-11_6 part of speech as the only mapping marks every lexeme`() {
        val mapping = Fx.defaultMapping.copy(translation = PhotoTarget.None, examples = PhotoTarget.None)

        val run = cards(Fx.ran).cards.withCoreMarks(mapping, form).single()

        assertTrue(run.lexemes.all { it.lacksCore })
    }

    @Test
    fun `AWP-13_1 AWP-13_5 summary counts lexemes without exists and words they land in`() {
        val build = cards(Fx.ran, Fx.bank)
        val marked = build.cards.withExistsMarks(form).withCoreMarks(Fx.defaultMapping, form)

        val summary = summaryOf(marked, Fx.defaultMapping, form)

        assertEquals(6, summary.lexemeCount)
        assertEquals(2, summary.wordCount)
        assertEquals(2, summary.existingCount)
        assertTrue(summary.isEnabled)
    }

    @Test
    fun `AWP-13_2 AWP-13_3 AWP-13_6 nothing to add or a lexeme without a core disables the button`() {
        val mapping = Fx.defaultMapping.copy(translation = PhotoTarget.None, gloss = PhotoTarget.Component(Fx.DEFINITION))
        val marked = cards(Fx.ran).cards.withExistsMarks(form).withCoreMarks(mapping, form)
        val onlyExisting = cards(Fx.ran).cards.map { card -> card.copy(lexemes = card.lexemes.map { it.copy(exists = true) }) }

        assertFalse(summaryOf(marked, mapping, form).isEnabled)
        assertEquals(3, summaryOf(marked, mapping, form).lacksCoreCount)
        assertFalse(summaryOf(emptyList(), Fx.defaultMapping, form).isEnabled)
        assertFalse(summaryOf(onlyExisting, Fx.defaultMapping, form).isEnabled)
    }
}
