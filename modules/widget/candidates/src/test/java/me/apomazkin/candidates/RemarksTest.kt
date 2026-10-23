package me.apomazkin.candidates

import me.apomazkin.lexeme.ComponentTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Разделы «Замечания» (AWP-30) и «Сопоставление» (AWP-31): строки по
 * замечаниям и сопоставлениям ответа. Тексты строк собирает composable из
 * ресурсов по виду строки, здесь проверены вид, число и порядок строк.
 */
class RemarksTest {

    private val form = Fx.form()

    /** Словарь с компонентом «Жопа» (7): о нём нейронка пишет, что назначение непонятно. */
    private val formWithUnclear = Fx.form(components = Fx.tree + Fx.unclear)

    private fun unclear(componentId: Long) = WordRemarkUi.ComponentUnclear(componentId)

    /** Рекомендация ответа, как её присылает сервер. */
    private fun recommendation(
        name: String,
        template: ComponentTemplate,
        vararg options: String,
    ) = ComponentRecommendationUi(name = name, template = template, options = options.toList())

    /** Строка «Замечаний», в которую она превращается. */
    private fun recommended(
        name: String,
        template: ComponentTemplate,
        vararg options: String,
    ) = RemarkUiItem.ComponentRecommended(
        name = name,
        template = template,
        options = options.toList(),
    )

    private fun noTranslationLine(count: Int) =
        RemarkUiItem.NoTranslation(count = count, translationLanguage = "ru")

    // AWP-30 — раздел «Замечания»

    @Test
    fun `AWP-30_1 unclear component of two words is one line`() {
        val words = listOf(
            Fx.perro.copy(remarks = listOf(unclear(Fx.UNCLEAR))),
            Fx.corrian.copy(remarks = listOf(unclear(Fx.UNCLEAR))),
        )

        val lines = remarksOf(words, emptyList(), formWithUnclear)

        assertEquals(listOf<RemarkUiItem>(RemarkUiItem.ComponentUnclear(Fx.UNCLEAR)), lines)
    }

    @Test
    fun `AWP-30_1 unclear component not in the form gives no line`() {
        val words = listOf(Fx.perro.copy(remarks = listOf(unclear(99L))))

        assertTrue(remarksOf(words, emptyList(), form).isEmpty())
    }

    @Test
    fun `AWP-30_2 WCL-16_2 recommended gender is a line with its template and options`() {
        val lines = remarksOf(listOf(Fx.perro), listOf(Fx.genderRecommendation), form)

        assertEquals(
            listOf<RemarkUiItem>(recommended("Род", ComponentTemplate.CHOICE, "м.р.", "ж.р.")),
            lines,
        )
    }

    @Test
    fun `AWP-30_3 WCL-16_1 zarpo with two lexemes without translation is one line, count two`() {
        val lines = remarksOf(listOf(Fx.zarpo), emptyList(), form)

        assertEquals(listOf<RemarkUiItem>(noTranslationLine(2)), lines)
    }

    @Test
    fun `AWP-30_3 the line without translation stays on the place of its first remark`() {
        val words = listOf(Fx.zarpo.copy(remarks = Fx.zarpo.remarks + unclear(Fx.UNCLEAR)))

        val lines = remarksOf(words, emptyList(), formWithUnclear)

        assertEquals(
            listOf(noTranslationLine(2), RemarkUiItem.ComponentUnclear(Fx.UNCLEAR)),
            lines,
        )
    }

    @Test
    fun `AWP-30_4 WCL-16_3 no remarks give no section`() {
        assertTrue(remarksOf(listOf(Fx.perro, Fx.corrian), emptyList(), form).isEmpty())
    }

    @Test
    fun `AWP-30_6 recommended gloss text has no options`() {
        val gloss = recommendation("Толкование", ComponentTemplate.TEXT)

        val lines = remarksOf(listOf(Fx.perro), listOf(gloss), form)

        assertEquals(
            listOf<RemarkUiItem>(recommended("Толкование", ComponentTemplate.TEXT)),
            lines,
        )
    }

    @Test
    fun `AWP-30_7 dictionary es to en gets no line without translation`() {
        val english = Fx.form(translationLanguage = "en")

        assertTrue(remarksOf(listOf(Fx.corrian), emptyList(), english).isEmpty())
    }

    @Test
    fun `AWP-30_9 one lexeme without translation gives count one`() {
        val remark = Fx.noTranslation("zarpar", "enwiktionary/1.verb.1")
        val one = Fx.zarpo.copy(remarks = listOf(remark))

        val lines = remarksOf(listOf(one), emptyList(), form)

        assertEquals(listOf<RemarkUiItem>(noTranslationLine(1)), lines)
    }

    @Test
    fun `AWP-30_10 the same lexeme without translation from two words counts once`() {
        val remark = Fx.noTranslation("run", "enwiktionary/1.verb.2")
        val words = listOf(
            Fx.ran.copy(remarks = listOf(remark)),
            Fx.running.copy(remarks = listOf(remark)),
        )

        val lines = remarksOf(words, emptyList(), form)

        assertEquals(listOf<RemarkUiItem>(noTranslationLine(1)), lines)
    }

    @Test
    fun `AWP-30_11 recommended example is a captioned text`() {
        val example = recommendation("Пример", ComponentTemplate.CAPTIONED_TEXT)

        val lines = remarksOf(listOf(Fx.perro), listOf(example), form)

        assertEquals(
            listOf<RemarkUiItem>(recommended("Пример", ComponentTemplate.CAPTIONED_TEXT)),
            lines,
        )
    }

    @Test
    fun `AWP-30_12 the remark of the word goes before the recommendation of the answer`() {
        val words = listOf(Fx.perro.copy(remarks = listOf(unclear(Fx.UNCLEAR))))

        val lines = remarksOf(words, listOf(Fx.genderRecommendation), formWithUnclear)

        assertEquals(
            listOf(
                RemarkUiItem.ComponentUnclear(Fx.UNCLEAR),
                recommended("Род", ComponentTemplate.CHOICE, "м.р.", "ж.р."),
            ),
            lines,
        )
    }

    @Test
    fun `AWP-30_13 fifteen recommendations give fifteen lines in answer order`() {
        val recommendations = (1..15).map {
            recommendation("Компонент $it", ComponentTemplate.TEXT)
        }

        val lines = remarksOf(listOf(Fx.perro), recommendations, form)

        assertEquals(15, lines.size)
        assertEquals(
            (1..15).map { "Компонент $it" },
            lines.map { (it as RemarkUiItem.ComponentRecommended).name },
        )
    }

    @Test
    fun `AWP-30_14 zarpo and zarpar with the same two lexemes count two`() {
        val remarks = listOf(
            Fx.noTranslation("zarpar", "enwiktionary/1.verb.1"),
            Fx.noTranslation("zarpar", "enwiktionary/2.verb.1"),
        )
        val zarpar = Fx.word("zarpar", Fx.zarpo.lemmas.single(), remarks = remarks)

        val lines = remarksOf(listOf(Fx.zarpo, zarpar), emptyList(), form)

        assertEquals(listOf<RemarkUiItem>(noTranslationLine(2)), lines)
    }

    // AWP-31 — раздел «Сопоставление»

    @Test
    fun `AWP-31_1 perro and corrian give lines in the order of the dictionary components`() {
        val lines = componentMatchLinesOf(listOf(Fx.perro, Fx.corrian), form)

        assertEquals(
            listOf(
                "translations → Перевод",
                "Часть речи: существительное",
                "Часть речи: прилагательное",
                "Часть речи: глагол",
                "Род: м.р.",
                "examples → Пример",
                "gloss → Толкование",
            ),
            lines.map { it.shown(form) },
        )
    }

    @Test
    fun `WCL-17_1 the dialog of perro gets the lines of the answer 5_2`() {
        val lines = componentMatchLinesOf(listOf(Fx.perro), form)

        assertEquals(
            listOf(
                "translations → Перевод",
                "Часть речи: существительное",
                "Часть речи: прилагательное",
                "Род: м.р.",
                "examples → Пример",
                "gloss → Толкование",
            ),
            lines.map { it.shown(form) },
        )
    }

    @Test
    fun `WCL-17_2 no remarks do not hide the component matches`() {
        assertTrue(remarksOf(listOf(Fx.perro), emptyList(), form).isEmpty())
        assertEquals(6, componentMatchLinesOf(listOf(Fx.perro), form).size)
    }

    @Test
    fun `AWP-31_2 an answer without a mapping gives no section`() {
        val bare = Fx.lexeme(key = "ruwiktionary/1.noun.1", translations = listOf("собака"))
        val word = Fx.word("perro", Fx.lemma("perro", listOf(bare)))

        assertTrue(componentMatchLinesOf(listOf(word), form).isEmpty())
    }

    @Test
    fun `AWP-31_3 deleting a lexeme from the window does not change the lines`() {
        val words = listOf(Fx.perro, Fx.corrian)
        val before = componentMatchLinesOf(words, form)
        val cards = Fx.cards(*words.toTypedArray())

        cards.deletingLexeme("perro", "ruwiktionary/1.noun.1")!!.deletingCard("correr")!!

        assertEquals(before, componentMatchLinesOf(words, form))
    }

    @Test
    fun `AWP-31_4 the section is collapsed in a new window`() {
        assertFalse(Fx.view().isComponentMatchesExpanded)
    }

    @Test
    fun `AWP-31_5 two fields into one component are two lines in order of first appearance`() {
        val lines = componentMatchLinesOf(listOf(Fx.corrian, Fx.zarpo), form)

        assertEquals(
            listOf(
                "translations → Перевод",
                "gloss → Перевод",
                "Часть речи: глагол",
                "gloss → Толкование",
            ),
            lines.map { it.shown(form) },
        )
    }

    @Test
    fun `AWP-29_12 lines on a component switched off after the request are dropped`() {
        val withoutGloss = Fx.form(components = Fx.tree.filterNot { it.id == Fx.GLOSS })

        val lines = componentMatchLinesOf(listOf(Fx.perro), withoutGloss)

        assertEquals(
            listOf(
                "translations → Перевод",
                "Часть речи: существительное",
                "Часть речи: прилагательное",
                "Род: м.р.",
                "examples → Пример",
            ),
            lines.map { it.shown(withoutGloss) },
        )
    }

    @Test
    fun `AWP-29_13 an option deleted after the request is not in the section`() {
        val genderOptions = Fx.options.getValue(Fx.GENDER).filterNot { it.id == Fx.MASCULINE }
        val withoutMasculine = Fx.form(options = Fx.options + (Fx.GENDER to genderOptions))

        val lines = componentMatchLinesOf(listOf(Fx.perro), withoutMasculine)

        val genderLines = lines.filterIsInstance<ComponentMatchLineUiItem.ComponentOption>()
            .filter { it.componentId == Fx.GENDER }
        assertTrue(genderLines.isEmpty())
    }
}
