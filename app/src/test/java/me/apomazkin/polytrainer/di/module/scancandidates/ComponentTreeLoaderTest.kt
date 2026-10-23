package me.apomazkin.polytrainer.di.module.scancandidates

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.ComponentOptionApiEntity
import me.apomazkin.core_db_api.entity.ComponentTypeApiEntity
import me.apomazkin.core_network_api.entity.ComponentNodeApiEntity
import me.apomazkin.core_network_api.entity.ComponentOptionNodeApiEntity
import me.apomazkin.core_resources.R
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.DependencyTarget
import me.apomazkin.ui.resource.ResourceManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * Дерево компонентов словаря для запроса (WFT-78) на словаре из контракта:
 * «Перевод», «Часть речи», «Пример» и свои «Род» под опцией «существительное» и
 * «Толкование». Порядок обхода — он же порядок компонентов формы окна.
 */
class ComponentTreeLoaderTest {

    private val now = Date(0L)

    // Ресурсы подменены: имя встроенного компонента и опции — их русский текст.
    private val resources = object : ResourceManager {
        private val strings = mapOf(
            R.string.word_card_bottom_translation to "Перевод",
            R.string.builtin_component_part_of_speech to "Часть речи",
            R.string.builtin_component_example to "Пример",
            R.string.part_of_speech_option_noun to "существительное",
            R.string.part_of_speech_option_verb to "глагол",
            R.string.part_of_speech_option_adjective to "прилагательное",
            R.string.part_of_speech_option_idiom to "идиома",
        )

        override fun stringByResId(id: Int): String = strings.getValue(id)

        override fun stringByResId(id: Int, value: String): String = error("not used")

        override fun stringByArrayId(id: Int): String = error("not used")
    }

    private fun type(
        id: Long,
        systemKey: BuiltInComponent? = null,
        name: String? = null,
        template: ComponentTemplate = ComponentTemplate.TEXT,
        position: Int = 0,
        isMultiple: Boolean = false,
        core: Boolean = false,
        enabled: Boolean = true,
        dependsOn: DependencyTarget = DependencyTarget.Lexeme,
    ) = ComponentTypeApiEntity(
        id = id,
        systemKey = systemKey,
        dictionaryId = DICT,
        name = name,
        template = template,
        position = position,
        isMultiple = isMultiple,
        core = core,
        enabled = enabled,
        dependsOn = dependsOn,
        createdAt = now,
        updatedAt = now,
    )

    private fun option(
        id: Long,
        typeId: Long,
        key: String? = null,
        label: String? = null,
        position: Int = 0,
        removed: Boolean = false,
    ) = ComponentOptionApiEntity(
        id = id,
        componentTypeId = typeId,
        systemKey = key,
        label = label,
        position = position,
        removedAt = if (removed) now else null,
    )

    private val translation = type(1L, BuiltInComponent.TRANSLATION, core = true, position = 0)
    private val partOfSpeech = type(
        2L,
        BuiltInComponent.PART_OF_SPEECH,
        template = ComponentTemplate.CHOICE,
        position = 1,
    )
    private val example = type(
        3L,
        BuiltInComponent.EXAMPLE,
        template = ComponentTemplate.CAPTIONED_TEXT,
        position = 2,
        isMultiple = true,
    )
    private val gloss = type(4L, name = "Толкование")
    private val gender = type(
        5L,
        name = "Род",
        template = ComponentTemplate.CHOICE,
        dependsOn = DependencyTarget.Option(21L),
    )

    private val partOfSpeechOptions = listOf(
        option(21L, 2L, key = "noun", position = 0),
        option(22L, 2L, key = "verb", position = 1),
        option(23L, 2L, key = "adjective", position = 2),
    )
    private val genderOptions = listOf(
        option(51L, 5L, label = "м.р.", position = 0),
        option(52L, 5L, label = "ж.р.", position = 1),
        option(53L, 5L, label = "общ.", position = 2),
    )

    private fun loader(
        types: List<ComponentTypeApiEntity>,
        options: Map<Long, List<ComponentOptionApiEntity>> = mapOf(
            2L to partOfSpeechOptions,
            5L to genderOptions,
        ),
    ): ComponentTreeLoader {
        val lexemeApi = mockk<CoreDbApi.LexemeApi>()
        coEvery { lexemeApi.getComponentTypes(DICT) } returns types
        coEvery { lexemeApi.getComponentOptions(any()) } returns emptyList()
        options.forEach { (typeId, list) ->
            coEvery { lexemeApi.getComponentOptions(typeId) } returns list
        }
        return ComponentTreeLoader(lexemeApi, resources)
    }

    private val dictionary = listOf(translation, partOfSpeech, example, gloss, gender)

    private fun List<ComponentNodeApiEntity>.node(id: Long): ComponentNodeApiEntity =
        single { it.id == id }

    @Test
    fun `WFT-78_1 dictionary of the contract - gender under the option noun, gloss in the root`() = runTest {
        val tree = loader(dictionary).treeOf(DICT)

        val expected = listOf(
            ComponentNodeApiEntity(
                id = 1L, key = "translation", name = "Перевод", template = "text",
                core = true, multiple = false, options = emptyList(), children = emptyList(),
            ),
            ComponentNodeApiEntity(
                id = 2L, key = "part_of_speech", name = "Часть речи", template = "choice",
                core = false, multiple = false,
                options = listOf(
                    ComponentOptionNodeApiEntity(
                        id = 21L, key = "noun", label = "существительное",
                        children = listOf(
                            ComponentNodeApiEntity(
                                id = 5L, key = null, name = "Род", template = "choice",
                                core = false, multiple = false,
                                options = listOf(
                                    ComponentOptionNodeApiEntity(51L, null, "м.р.", emptyList()),
                                    ComponentOptionNodeApiEntity(52L, null, "ж.р.", emptyList()),
                                    ComponentOptionNodeApiEntity(53L, null, "общ.", emptyList()),
                                ),
                                children = emptyList(),
                            ),
                        ),
                    ),
                    ComponentOptionNodeApiEntity(22L, "verb", "глагол", emptyList()),
                    ComponentOptionNodeApiEntity(23L, "adjective", "прилагательное", emptyList()),
                ),
                children = emptyList(),
            ),
            ComponentNodeApiEntity(
                id = 3L, key = "example", name = "Пример", template = "captioned_text",
                core = false, multiple = true, options = emptyList(), children = emptyList(),
            ),
            ComponentNodeApiEntity(
                id = 4L, key = null, name = "Толкование", template = "text",
                core = false, multiple = false, options = emptyList(), children = emptyList(),
            ),
        )
        assertEquals(expected, tree)
    }

    @Test
    fun `WFT-78_2 built-in translation has its key, core and template`() = runTest {
        val node = loader(dictionary).treeOf(DICT).node(1L)

        assertEquals("translation", node.key)
        assertTrue(node.core)
        assertEquals("text", node.template)
        assertEquals("Перевод", node.name)
    }

    @Test
    fun `WFT-78_3 own option has no key and its text for the label`() = runTest {
        val tree = loader(dictionary).treeOf(DICT)

        val genderNode = tree.node(2L).options.first { it.id == 21L }.children.single()
        val own = genderNode.options.first()

        assertNull(own.key)
        assertEquals("м.р.", own.label)
    }

    @Test
    fun `WFT-78_4 disabled example is not in the tree, the rest stays`() = runTest {
        val disabled = example.copy(enabled = false)

        val tree = loader(listOf(translation, partOfSpeech, disabled, gloss, gender)).treeOf(DICT)

        assertEquals(listOf(1L, 2L, 4L), tree.map { it.id })
        assertTrue(tree.none { it.key == "example" })
    }

    @Test
    fun `WFT-78_6 disabled part of speech takes the gender under its option with it`() = runTest {
        val disabled = partOfSpeech.copy(enabled = false)

        val tree = loader(listOf(translation, disabled, example, gloss, gender)).treeOf(DICT)

        assertEquals(listOf(1L, 3L, 4L), tree.map { it.id })
        assertTrue(tree.flatMap { it.options }.isEmpty())
    }

    @Test
    fun `WFT-78_7 no enabled components gives an empty tree`() = runTest {
        val off = listOf(translation.copy(enabled = false), gloss.copy(enabled = false))

        assertTrue(loader(off).treeOf(DICT).isEmpty())
        assertTrue(loader(emptyList()).treeOf(DICT).isEmpty())
    }

    @Test
    fun `WFT-78 roots go built-in first, then by position, then by id`() = runTest {
        val ownFirst = type(10L, name = "Первое своё", position = 0)
        val ownSecond = type(11L, name = "Второе своё", position = 0)
        val ownLate = type(9L, name = "Позднее своё", position = 5)

        val shuffled = listOf(ownLate, ownSecond, example, ownFirst, partOfSpeech, translation)
        val tree = loader(shuffled).treeOf(DICT)

        assertEquals(listOf(1L, 2L, 3L, 10L, 11L, 9L), tree.map { it.id })
    }

    @Test
    fun `WFT-78 a component depending on another component sits in its children`() = runTest {
        val note = type(7L, name = "Заметка", dependsOn = DependencyTarget.Component(ComponentTypeId(1L)))
        val deeper = type(8L, name = "Ещё глубже", dependsOn = DependencyTarget.Component(ComponentTypeId(7L)))

        val tree = loader(listOf(translation, note, deeper)).treeOf(DICT)

        assertEquals(listOf(1L), tree.map { it.id })
        assertEquals(listOf(7L), tree.node(1L).children.map { it.id })
        assertEquals(listOf(8L), tree.node(1L).children.single().children.map { it.id })
    }

    @Test
    fun `WFT-78 options of a choice go by position, a removed option is not there with its dependents`() = runTest {
        val onVerb = type(7L, name = "Переходность", dependsOn = DependencyTarget.Option(22L))
        val options = listOf(
            option(23L, 2L, key = "adjective", position = 2),
            option(22L, 2L, key = "verb", position = 1, removed = true),
            option(21L, 2L, key = "noun", position = 0),
        )

        val tree = loader(listOf(partOfSpeech, onVerb), mapOf(2L to options)).treeOf(DICT)

        val node = tree.node(2L)
        assertEquals(listOf(21L, 23L), node.options.map { it.id })
        assertTrue(node.options.all { it.children.isEmpty() })
    }

    @Test
    fun `WFT-78 built-in option label is its resource, renamed built-in option keeps the text`() = runTest {
        val options = listOf(
            option(21L, 2L, key = "noun", position = 0),
            option(22L, 2L, key = "verb", label = "Глагол!", position = 1),
            option(23L, 2L, key = "idiom", position = 2),
        )

        val labels = loader(listOf(partOfSpeech), mapOf(2L to options))
            .treeOf(DICT)
            .node(2L)
            .options
            .associate { it.id to it.label }

        assertEquals(
            mapOf(21L to "существительное", 22L to "Глагол!", 23L to "идиома"),
            labels,
        )
    }

    @Test
    fun `WFT-78 options are read only for enabled choices`() = runTest {
        val lexemeApi = mockk<CoreDbApi.LexemeApi>()
        coEvery { lexemeApi.getComponentTypes(DICT) } returns
            listOf(translation, partOfSpeech.copy(enabled = false))

        ComponentTreeLoader(lexemeApi, resources).treeOf(DICT)

        coVerify(exactly = 0) { lexemeApi.getComponentOptions(any()) }
    }

    @Test
    fun `WFT-78 flat order of the form - component, then dependents of its options, then its own`() = runTest {
        val note = type(7L, name = "Заметка", dependsOn = DependencyTarget.Component(ComponentTypeId(2L)))

        val flat = loader(listOf(translation, partOfSpeech, example, gloss, gender, note))
            .componentsOf(DICT)
            .flattened()

        assertEquals(listOf(1L, 2L, 5L, 7L, 3L, 4L), flat.map { it.type.id })
    }

    private companion object {
        const val DICT = 1L
    }
}
