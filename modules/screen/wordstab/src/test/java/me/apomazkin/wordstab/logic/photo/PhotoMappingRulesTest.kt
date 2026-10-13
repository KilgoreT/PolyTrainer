package me.apomazkin.wordstab.logic.photo

import me.apomazkin.wordstab.deps.PhotoDataKind
import me.apomazkin.wordstab.deps.PhotoMapping
import me.apomazkin.wordstab.deps.PhotoTarget
import me.apomazkin.wordstab.deps.PhotoTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Соответствия: умолчания (AWP-06), цели видов (AWP-09), один компонент — один вид (AWP-28). */
class PhotoMappingRulesTest {

    private val form = Fx.form()

    private fun component(id: Long) = PhotoTarget.Component(id)

    @Test
    fun `AWP-06_1 nothing remembered gives defaults`() {
        assertEquals(Fx.defaultMapping, form.resolveMapping(remembered = null))
    }

    @Test
    fun `AWP-06_2 remembered gloss target is kept`() {
        val remembered = Fx.defaultMapping.copy(gloss = component(Fx.DEFINITION))

        assertEquals(component(Fx.DEFINITION), form.resolveMapping(remembered).gloss)
    }

    @Test
    fun `AWP-06_3 translation disabled falls back to the first text core by position then id`() {
        val note = Fx.component(9L, null, "Заметка", PhotoTemplate.TEXT, isCore = true, position = 10)
        val withoutTranslation = Fx.form(components = listOf(Fx.definition, note, Fx.partOfSpeech, Fx.example))

        assertEquals(component(Fx.DEFINITION), withoutTranslation.resolveMapping(null).translation)
    }

    @Test
    fun `AWP-06_4 no text cores give no translation target and a red summary`() {
        val noCores = Fx.form(components = listOf(Fx.partOfSpeech, Fx.example, Fx.hint))

        val mapping = noCores.resolveMapping(null)

        assertEquals(PhotoTarget.None, mapping.translation)
        assertTrue(mapping.hasNoCore(noCores))
    }

    @Test
    fun `AWP-06_5 remembered component off the form or of a wrong template gives the default of the kind`() {
        val foreign = Fx.defaultMapping.copy(translation = component(999L), examples = component(Fx.TRANSLATION))

        val mapping = form.resolveMapping(foreign)

        assertEquals(component(Fx.TRANSLATION), mapping.translation)
        assertEquals(component(Fx.EXAMPLE), mapping.examples)
    }

    @Test
    fun `AWP-06_6 part of speech and example disabled give none`() {
        val slim = Fx.form(components = listOf(Fx.translation, Fx.definition))

        val mapping = slim.resolveMapping(null)

        assertEquals(PhotoTarget.None, mapping.partOfSpeech)
        assertEquals(PhotoTarget.None, mapping.examples)
    }

    @Test
    fun `AWP-06_7 remembered none for translation stays none`() {
        val remembered = Fx.defaultMapping.copy(translation = PhotoTarget.None)

        assertEquals(PhotoTarget.None, form.resolveMapping(remembered).translation)
    }

    @Test
    fun `AWP-06_9 AWP-28_2 remembered gloss on the component the translation default would take wins`() {
        val withoutTranslation = Fx.form(components = listOf(Fx.definition, Fx.partOfSpeech, Fx.example))
        val remembered = PhotoMapping(
            translation = component(999L),
            gloss = component(Fx.DEFINITION),
            examples = component(Fx.EXAMPLE),
            partOfSpeech = component(Fx.PART_OF_SPEECH),
        )

        val mapping = withoutTranslation.resolveMapping(remembered)

        assertEquals(component(Fx.DEFINITION), mapping.gloss)
        assertEquals(PhotoTarget.None, mapping.translation)
    }

    @Test
    fun `AWP-28_3 defaults never collide — gloss default is none`() {
        val withoutTranslation = Fx.form(components = listOf(Fx.definition, Fx.partOfSpeech, Fx.example))

        val mapping = withoutTranslation.resolveMapping(null)

        assertEquals(component(Fx.DEFINITION), mapping.translation)
        assertEquals(PhotoTarget.None, mapping.gloss)
    }

    @Test
    fun `AWP-09_1 AWP-09_3 AWP-09_6 AWP-09_7 targets per kind — text components, captioned multiple, built-in part of speech`() {
        val quote = Fx.component(10L, null, "Цитата", PhotoTemplate.CAPTIONED_TEXT, isCore = false, isMultiple = false, position = 5)
        val image = Fx.component(11L, null, "Картинка", PhotoTemplate.IMAGE, isCore = true, position = 6)
        val wide = Fx.form(components = listOf(Fx.translation, Fx.definition, Fx.partOfSpeech, Fx.example, Fx.hint, quote, image))

        assertEquals(listOf(Fx.TRANSLATION, Fx.DEFINITION, Fx.HINT), wide.targetsFor(PhotoDataKind.TRANSLATION).map { it.id })
        assertEquals(listOf(Fx.TRANSLATION, Fx.DEFINITION, Fx.HINT), wide.targetsFor(PhotoDataKind.GLOSS).map { it.id })
        assertEquals(listOf(Fx.EXAMPLE), wide.targetsFor(PhotoDataKind.EXAMPLES).map { it.id })
        assertEquals(listOf(Fx.PART_OF_SPEECH), wide.targetsFor(PhotoDataKind.PART_OF_SPEECH).map { it.id })
    }

    @Test
    fun `AWP-28_1 choosing a component taken by another kind frees that kind`() {
        val mapping = Fx.defaultMapping.copy(gloss = component(Fx.DEFINITION))

        val chosen = mapping.choose(PhotoDataKind.TRANSLATION, component(Fx.DEFINITION))

        assertEquals(component(Fx.DEFINITION), chosen.translation)
        assertEquals(PhotoTarget.None, chosen.gloss)
    }

    @Test
    fun `AWP-28_4 choosing the only core for gloss leaves translation none and the summary red`() {
        val onlyTranslation = Fx.form(components = listOf(Fx.translation, Fx.partOfSpeech, Fx.hint))
        val mapping = onlyTranslation.resolveMapping(null).choose(PhotoDataKind.GLOSS, component(Fx.TRANSLATION))

        assertEquals(PhotoTarget.None, mapping.translation)
        assertEquals(component(Fx.TRANSLATION), mapping.gloss)
        assertFalse(mapping.hasNoCore(onlyTranslation))
    }

    @Test
    fun `AWP-11_7 translation to a parameter and gloss none means no kind reaches a core`() {
        val mapping = Fx.defaultMapping.copy(translation = component(Fx.HINT), gloss = PhotoTarget.None)

        assertTrue(mapping.hasNoCore(form))
    }

    @Test
    fun `AWP-21_6 disabled component drops out and its kind returns to the default`() {
        val mapping = Fx.defaultMapping.copy(gloss = component(Fx.DEFINITION))
        val narrowed = form.copy(components = form.components.filterNot { it.id == Fx.DEFINITION })

        val revalidated = narrowed.revalidate(mapping)

        assertEquals(PhotoTarget.None, revalidated.gloss)
        assertEquals(component(Fx.TRANSLATION), revalidated.translation)
    }
}
