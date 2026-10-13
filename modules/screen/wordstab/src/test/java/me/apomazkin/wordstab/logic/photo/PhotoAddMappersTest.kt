package me.apomazkin.wordstab.logic.photo

import me.apomazkin.lexeme.CaptionedTextValues
import me.apomazkin.lexeme.ChoiceValues
import me.apomazkin.lexeme.Primitive
import me.apomazkin.lexeme.TextValues
import me.apomazkin.wordstab.deps.PhotoTarget
import me.apomazkin.wordstab.logic.PhotoWordsState
import me.apomazkin.wordstab.logic.WordsTabState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Содержимое окна → пакет записи (AWP-20, AWP-22, AWP-08). */
class PhotoAddMappersTest {

    private val form = Fx.form()

    private fun cardsOf(vararg words: me.apomazkin.wordstab.deps.PhotoWord) =
        buildPhotoCards(words.toList(), form).cards.withExistsMarks(form)

    private fun state(
        cards: List<PhotoCard>,
        mapping: me.apomazkin.wordstab.deps.PhotoMapping = Fx.defaultMapping,
        commonGroupId: Long? = null,
    ): PhotoWordsState = WordsTabState(
        photoWordsState = PhotoWordsState(isOpen = true, dictionaryId = Fx.DICT, form = form, mapping = mapping, commonGroupId = commonGroupId, cards = cards),
    ).photoWordsState

    private fun text(value: String) = TextValues(Primitive.Text(value))

    private val russianVerb = cardsOf(Fx.ran).single().lexemes.first { it.key == "ru/v1" }
    private val englishVerb = buildPhotoCards(listOf(Fx.word("run", me.apomazkin.wordstab.deps.PhotoLemma("run", null, listOf(Fx.runLexemes[4])))), form)
        .cards.single().lexemes.single()
    private val adjective = cardsOf(Fx.ran).single().lexemes.first { it.key == "en/a1" }

    @Test
    fun `AWP-22_1 russian verb without examples gives translation and part of speech only`() {
        val components = russianVerb.componentsFor(Fx.defaultMapping, form)

        assertEquals(
            listOf(Fx.TRANSLATION to text("бежать, бегать"), Fx.PART_OF_SPEECH to ChoiceValues(Fx.VERB)),
            components.map { it.typeId to it.data },
        )
    }

    @Test
    fun `AWP-22_2 AWP-22_10 english lexeme with gloss mapping gives translation definition and captioned examples`() {
        val mapping = Fx.defaultMapping.copy(gloss = PhotoTarget.Component(Fx.DEFINITION))

        val components = englishVerb.componentsFor(mapping, form)

        assertEquals(
            listOf(
                Fx.TRANSLATION to text("бегать, побегать, бежать, побежать"),
                Fx.DEFINITION to text("To move forward quickly upon two feet…"),
                Fx.PART_OF_SPEECH to ChoiceValues(Fx.VERB),
                Fx.EXAMPLE to CaptionedTextValues(Primitive.Text("Run, Sarah, run!"), Primitive.Text(EXAMPLE_CAPTION)),
                Fx.EXAMPLE to CaptionedTextValues(Primitive.Text("She ran home."), Primitive.Text(EXAMPLE_CAPTION)),
            ),
            components.map { it.typeId to it.data },
        )
    }

    @Test
    fun `AWP-22_3 gloss only lexeme with a core gloss target writes the gloss there and leaves translation empty`() {
        val mapping = Fx.defaultMapping.copy(gloss = PhotoTarget.Component(Fx.DEFINITION))

        val components = adjective.componentsFor(mapping, form)

        assertEquals(listOf(Fx.DEFINITION), components.filter { it.data is TextValues }.map { it.typeId })
    }

    @Test
    fun `AWP-22_4 gloss only lexeme with gloss none writes the gloss into the translation component`() {
        val components = adjective.componentsFor(Fx.defaultMapping, form)

        assertEquals(listOf(Fx.TRANSLATION to text("In a liquid state; melted or molten.")), components.filter { it.data is TextValues }.map { it.typeId to it.data })
    }

    @Test
    fun `AWP-22_5 gloss only lexeme with a parameter gloss target writes into translation and leaves the parameter empty`() {
        val mapping = Fx.defaultMapping.copy(gloss = PhotoTarget.Component(Fx.HINT))

        val components = adjective.componentsFor(mapping, form)

        assertEquals(listOf(Fx.TRANSLATION), components.filter { it.data is TextValues }.map { it.typeId })
    }

    @Test
    fun `AWP-22_6 AWP-16_3 edited text lands trimmed`() {
        val edited = russianVerb.copy(translation = russianVerb.translation.copy(text = "  брать  "))

        val components = edited.componentsFor(Fx.defaultMapping, form)

        assertEquals(text("брать"), components.first { it.typeId == Fx.TRANSLATION }.data)
    }

    @Test
    fun `AWP-22_7 without a part of speech no choice component is written`() {
        val components = russianVerb.copy(partOfSpeechOptionId = null).componentsFor(Fx.defaultMapping, form)

        assertTrue(components.none { it.data is ChoiceValues })
    }

    @Test
    fun `AWP-22_9 examples none or all deleted give no example component`() {
        val noTarget = englishVerb.componentsFor(Fx.defaultMapping.copy(examples = PhotoTarget.None), form)
        val deleted = englishVerb.copy(examples = emptyList()).componentsFor(Fx.defaultMapping, form)

        assertTrue(noTarget.none { it.data is CaptionedTextValues })
        assertTrue(deleted.none { it.data is CaptionedTextValues })
    }

    @Test
    fun `AWP-20_6 empty translation text creates no component`() {
        val blank = russianVerb.copy(translation = russianVerb.translation.copy(text = "   "))

        val components = blank.componentsFor(Fx.defaultMapping, form)

        assertTrue(components.none { it.data is TextValues })
    }

    @Test
    fun `AWP-20_1 AWP-20_4 batch carries new and existing words with lexemes but not the existing ones`() {
        val cards = cardsOf(Fx.ran, Fx.bank)

        val batch = state(cards, commonGroupId = Fx.POTTER).toWordsBatch()

        assertEquals(Fx.DICT, batch.dictionaryId)
        assertEquals(listOf("run", "bank"), batch.words.map { it.text })
        assertEquals(Fx.RUN_WORD, batch.words[0].wordId)
        assertEquals(4, batch.words[0].lexemes.size)
        assertEquals(Fx.BANK_1, batch.words[1].wordId)
        assertEquals(2, batch.words[1].lexemes.size)
        assertTrue(batch.words.all { it.groupId == Fx.POTTER })
    }

    @Test
    fun `AWP-20_2 common group all gives words without membership`() {
        val batch = state(cardsOf(Fx.fuera)).toWordsBatch()

        assertTrue(batch.words.all { it.groupId == null })
        assertTrue(batch.words.all { it.wordId == null })
    }

    @Test
    fun `AWP-08_1 AWP-08_4 AWP-20_7 own group of a card beats the common one, own all gives no group`() {
        val cards = cardsOf(Fx.ran, Fx.bank).map { card ->
            when (card.lemma) {
                "bank" -> card.copy(ownGroup = PhotoOwnGroup.Group(Fx.FINANCE))
                else -> card.copy(ownGroup = PhotoOwnGroup.All)
            }
        }

        val batch = state(cards, commonGroupId = Fx.POTTER).toWordsBatch()

        assertNull(batch.words.first { it.text == "run" }.groupId)
        assertEquals(Fx.FINANCE, batch.words.first { it.text == "bank" }.groupId)
    }

    @Test
    fun `AWP-15_3 card with every lexeme deleted is not in the batch`() {
        val cards = cardsOf(Fx.ran, Fx.bank).map { card -> if (card.lemma == "run") card.copy(lexemes = emptyList()) else card }

        val batch = state(cards).toWordsBatch()

        assertEquals(listOf("bank"), batch.words.map { it.text })
    }
}
