package me.apomazkin.candidates

import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.DependencyTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Значения лексемы-кандидата по сопоставлению (AWP-29) и их активность по
 * зависимостям компонентов (AWP-17): что видит пользователь строками
 * «Имя: значение» в порядке компонентов словаря, как в карточке слова.
 */
class CandidateValuesTest {

    private val form = Fx.form()

    private fun shownOf(
        word: LookupWordUi,
        lemma: String,
        key: String,
        form: DictionaryFormUi = this.form,
    ): List<String> = Fx.cards(word, form = form).candidate(lemma, key).shown(form)

    /** Все показанные строки «perro», `ruwiktionary/1.noun.1`, при полном сопоставлении. */
    private val perroNoun1Shown = listOf(
        "Перевод: собака; пёс (также ругательство)",
        "Часть речи: существительное",
        "Род: м.р.",
        "Пример: perro callejero",
        "Толкование: dog (the species Canis familiaris …)",
    )

    @Test
    fun `AWP-29_1 perro noun shows translation, part of speech, gender, example, gloss`() {
        val shown = shownOf(Fx.perro, "perro", "ruwiktionary/1.noun.1")

        assertEquals(perroNoun1Shown, shown)
    }

    @Test
    fun `AWP-29_2 corrian verb shows no gender`() {
        val shown = shownOf(Fx.corrian, "correr", "ruwiktionary/1.verb.3")

        assertEquals(
            listOf(
                "Перевод: течь, литься, струиться (о жидкости)",
                "Часть речи: глагол",
                "Толкование: to flow",
            ),
            shown,
        )
    }

    @Test
    fun `AWP-29_3 zarpo puts the english gloss into translation and into gloss`() {
        val shown = shownOf(Fx.zarpo, "zarpar", "enwiktionary/1.verb.1")

        assertEquals(
            listOf(
                "Перевод: to embark; to set sail",
                "Часть речи: глагол",
                "Толкование: to embark; to set sail",
            ),
            shown,
        )
    }

    @Test
    fun `AWP-29_5 synonyms without a mapping line are not shown`() {
        val dog = Fx.lexeme(
            key = "enwiktionary/1.noun.1",
            gloss = "dog (the species Canis familiaris …)",
            synonyms = listOf("can", "chucho"),
            grammar = mapOf("gender" to "masculine"),
            partOfSpeechKey = "noun",
            matches = listOf(
                Fx.byField(Fx.TRANSLATION, "gloss"),
                Fx.byOption(Fx.PART_OF_SPEECH, Fx.NOUN),
                Fx.byOption(Fx.GENDER, Fx.MASCULINE),
                Fx.byField(Fx.GLOSS, "gloss"),
            ),
        )
        val word = Fx.word("perro", Fx.lemma("perro", listOf(dog)))

        val shown = shownOf(word, "perro", "enwiktionary/1.noun.1")

        assertEquals(
            listOf(
                "Перевод: dog (the species Canis familiaris …)",
                "Часть речи: существительное",
                "Род: м.р.",
                "Толкование: dog (the species Canis familiaris …)",
            ),
            shown,
        )
        assertTrue(shown.none { "can" in it || "chucho" in it })
    }

    @Test
    fun `AWP-29_6 tiempo shows three values of the multiple example component`() {
        val shown = shownOf(Fx.tiempo, "tiempo", "ruwiktionary/1.noun.3")

        assertEquals(
            listOf(
                "Перевод: погода",
                "Часть речи: существительное",
                "Пример: ¡Hoy hace un tiempo de perro!",
                "Пример: ¿Qué tiempo hace?",
                "Пример: Hace buen tiempo.",
            ),
            shown,
        )
    }

    @Test
    fun `AWP-29_7 notes are not shown without a notes mapping line`() {
        val shown = shownOf(Fx.perro, "perro", "ruwiktionary/2.adjective.1")

        assertEquals(
            listOf(
                "Перевод: дурной, скверный, паршивый; несладкий, собачий",
                "Часть речи: прилагательное",
                "Толкование: wicked; mean",
            ),
            shown,
        )
        assertTrue(shown.none { "разг." in it })
    }

    @Test
    fun `AWP-29_9 a list in a component with one value is joined with comma`() {
        val shown = shownOf(Fx.runEnglish, "run", "enwiktionary/1.verb.1")

        assertEquals(listOf("Перевод: бегать, побегать, бежать, побежать"), shown)
    }

    @Test
    fun `AWP-29_9 a multiple component keeps the list items apart`() {
        val multiple = Fx.form(components = listOf(Fx.translation.copy(isMultiple = true)))

        val shown = shownOf(Fx.runEnglish, "run", "enwiktionary/1.verb.1", multiple)

        assertEquals(
            listOf("Перевод: бегать", "Перевод: побегать", "Перевод: бежать", "Перевод: побежать"),
            shown,
        )
    }

    private val withTranscription = Fx.form(components = listOf(Fx.translation, Fx.transcription))

    private fun bankWith(pronunciations: List<String>): LookupWordUi {
        val bank = Fx.lexeme(
            key = "ruwiktionary/1.noun.1",
            translations = listOf("банк"),
            matches = listOf(
                Fx.byField(Fx.TRANSLATION, "translations"),
                Fx.byField(Fx.TRANSCRIPTION, "pronunciations"),
            ),
        )
        return Fx.word("bank", Fx.lemma("bank", listOf(bank), pronunciations = pronunciations))
    }

    @Test
    fun `AWP-29_10 pronunciations of the lemma go to the transcription component`() {
        val word = bankWith(listOf("/ˈbæŋk/"))

        val shown = shownOf(word, "bank", "ruwiktionary/1.noun.1", withTranscription)

        assertEquals(listOf("Перевод: банк", "Транскрипция: /ˈbæŋk/"), shown)
    }

    @Test
    fun `AWP-29_10 several pronunciations are joined with comma`() {
        val word = bankWith(listOf("/ˈbæŋk/", "/bank/"))

        val shown = shownOf(word, "bank", "ruwiktionary/1.noun.1", withTranscription)

        assertEquals(listOf("Перевод: банк", "Транскрипция: /ˈbæŋk/, /bank/"), shown)
    }

    @Test
    fun `AWP-29_11 mapping lines in reverse order show the same values as in order`() {
        val inOrder = Fx.perroNoun1
        val reversed = inOrder.copy(componentMatches = inOrder.componentMatches.reversed())
        val word = Fx.word("perro", Fx.lemma("perro", listOf(reversed)))

        val shown = shownOf(word, "perro", "ruwiktionary/1.noun.1")

        assertEquals(perroNoun1Shown, shown)
    }

    @Test
    fun `AWP-29_12 a line on a component switched off after the request gives no value`() {
        val withoutGloss = Fx.form(components = Fx.tree.filterNot { it.id == Fx.GLOSS })

        val shown = shownOf(Fx.perro, "perro", "ruwiktionary/1.noun.1", withoutGloss)

        assertEquals(perroNoun1Shown.dropLast(1), shown)
    }

    @Test
    fun `AWP-29_13 a deleted option leaves the choice without value and keeps the rest`() {
        val genderOptions = Fx.options.getValue(Fx.GENDER).filterNot { it.id == Fx.MASCULINE }
        val withoutMasculine = Fx.form(options = Fx.options + (Fx.GENDER to genderOptions))

        val shown = shownOf(Fx.perro, "perro", "ruwiktionary/1.noun.1", withoutMasculine)

        assertEquals(
            perroNoun1Shown.map { if (it == "Род: м.р.") "Род: без значения" else it },
            shown,
        )
    }

    @Test
    fun `AWP-29_14 a grammar label goes to a text gender component as in the entry`() {
        val masculine = Fx.lexeme(
            key = "ruwiktionary/1.noun.1",
            translations = listOf("собака"),
            grammar = mapOf("gender" to "masculine"),
            partOfSpeechKey = "noun",
            matches = listOf(
                Fx.byField(Fx.TRANSLATION, "translations"),
                Fx.byField(Fx.GENDER, "grammar.gender"),
            ),
        )
        val word = Fx.word("perro", Fx.lemma("perro", listOf(masculine)))
        val textGender = Fx.form(components = listOf(Fx.translation, Fx.genderText))

        val shown = shownOf(word, "perro", "ruwiktionary/1.noun.1", textGender)

        assertEquals(listOf("Перевод: собака", "Род: masculine"), shown)
    }

    @Test
    fun `AWP-17_2 article without part of speech shows no part of speech line`() {
        val lexeme = Fx.cards(Fx.los).candidate("el", "enwiktionary/1.article.1")

        assertEquals(listOf(Fx.TRANSLATION), lexeme.shownComponents())
        assertEquals(listOf("Перевод: the"), lexeme.shown(form))
    }

    @Test
    fun `AWP-17_3 part of speech switched off is neither in mapping nor in the card`() {
        val withoutPartOfSpeech = Fx.form(components = listOf(Fx.translation, Fx.example, Fx.gloss))

        val shown = shownOf(Fx.perro, "perro", "ruwiktionary/1.noun.1", withoutPartOfSpeech)

        assertEquals(
            listOf(
                "Перевод: собака; пёс (также ругательство)",
                "Пример: perro callejero",
                "Толкование: dog (the species Canis familiaris …)",
            ),
            shown,
        )
    }

    @Test
    fun `AWP-17_6 gender depends on the noun option and goes with another part of speech`() {
        val partOfSpeech = Fx.componentRef("perro", "ruwiktionary/1.noun.1", Fx.PART_OF_SPEECH)

        val asVerb = Fx.cards(Fx.perro)
            .withOption(partOfSpeech, Fx.VERB, form)!!
            .withActiveValues(form)

        val lexeme = asVerb.candidate("perro", "ruwiktionary/1.noun.1")
        assertEquals(
            listOf(
                "Перевод: собака; пёс (также ругательство)",
                "Часть речи: глагол",
                "Пример: perro callejero",
                "Толкование: dog (the species Canis familiaris …)",
            ),
            lexeme.shown(form),
        )
        // Значение «Рода» не потеряно, оно только неактивно и не пишется.
        assertEquals(listOf(Fx.MASCULINE), lexeme.chosen(Fx.GENDER))
        assertFalse(lexeme.values.first { it.componentId == Fx.GENDER }.isActive)
    }

    @Test
    fun `AWP-17_7 back to noun brings the gender value back`() {
        val partOfSpeech = Fx.componentRef("perro", "ruwiktionary/1.noun.1", Fx.PART_OF_SPEECH)
        val asVerb = Fx.cards(Fx.perro)
            .withOption(partOfSpeech, Fx.VERB, form)!!
            .withActiveValues(form)

        val backToNoun = asVerb
            .withOption(partOfSpeech, Fx.NOUN, form)!!
            .withActiveValues(form)

        assertEquals(
            perroNoun1Shown,
            backToNoun.candidate("perro", "ruwiktionary/1.noun.1").shown(form),
        )
    }

    @Test
    fun `AWP-17_6 a component under a component is active only while the parent has content`() {
        val glossUnderTranslation = Fx.gloss.copy(
            dependsOn = DependencyTarget.Component(ComponentTypeId(Fx.TRANSLATION)),
        )
        val dependent = Fx.form(components = listOf(Fx.translation, glossUnderTranslation))
        val lexeme = Fx.lexeme(
            key = "ruwiktionary/1.noun.1",
            translations = listOf("собака"),
            gloss = "dog",
            matches = listOf(
                Fx.byField(Fx.TRANSLATION, "translations"),
                Fx.byField(Fx.GLOSS, "gloss"),
            ),
        )
        val cards = Fx.cards(Fx.word("perro", Fx.lemma("perro", listOf(lexeme))), form = dependent)
        val translation = Fx.ref("perro", "ruwiktionary/1.noun.1", Fx.TRANSLATION)

        val erased = cards.withTextEdit(translation, "")!!.withActiveValues(dependent)
        val filled = erased.withTextEdit(translation, "пёс")!!.withActiveValues(dependent)

        fun List<LemmaCardUiItem>.shownLines() =
            candidate("perro", "ruwiktionary/1.noun.1").shown(dependent)
        assertEquals(listOf("Перевод: собака", "Толкование: dog"), cards.shownLines())
        assertEquals(listOf("Перевод: "), erased.shownLines())
        assertEquals(listOf("Перевод: пёс", "Толкование: dog"), filled.shownLines())
    }
}
