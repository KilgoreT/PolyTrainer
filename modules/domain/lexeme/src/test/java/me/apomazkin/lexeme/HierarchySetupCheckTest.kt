package me.apomazkin.lexeme

import me.apomazkin.lexeme.HierarchyFixtures.option
import me.apomazkin.lexeme.HierarchyFixtures.removed
import me.apomazkin.lexeme.HierarchyFixtures.type
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * IS486 фаза 1, зона A3: валидатор создания/редактирования (spec §7.1, §7.5, §7.7).
 * phase1_plan.md § Зона A.
 *
 * Граф D1: перевод(1, ядро), часть речи(2, CHOICE, опции 10/11 + removed 12),
 * пример(3 ← компонент 1), removed-компонент(4). Чужой словарь D2: компонент(5, опция 20).
 */
class HierarchySetupCheckTest {

    private val graph = ComponentGraph(
        types = listOf(
            type(id = 1, core = true, systemKey = BuiltInComponent.TRANSLATION),
            type(id = 2, template = ComponentTemplate.CHOICE),
            type(id = 3, dependsOn = DependencyTarget.Component(ComponentTypeId(1))),
            type(id = 4, removedAt = removed),
            type(id = 5, dictionaryId = 2L),
        ),
        options = listOf(
            option(id = 10, ownerTypeId = 2),
            option(id = 11, ownerTypeId = 2),
            option(id = 12, ownerTypeId = 2, removedAt = removed),
            option(id = 20, ownerTypeId = 5),
        ),
    )

    private fun check(
        template: ComponentTemplate = ComponentTemplate.TEXT,
        isMultiple: Boolean = false,
        core: Boolean = false,
        dependsOn: DependencyTarget = DependencyTarget.Lexeme,
        dictionaryId: Long? = 1L,
    ) = graph.checkSetup(template, isMultiple, core, dependsOn, dictionaryId)

    @Test
    fun `lexeme target with core is ok`() {
        assertEquals(SetupCheck.Ok, check(core = true))
    }

    @Test
    fun `lexeme target without core is ok`() {
        assertEquals(SetupCheck.Ok, check(core = false))
    }

    @Test
    fun `component target without core is ok`() {
        assertEquals(
            SetupCheck.Ok,
            check(dependsOn = DependencyTarget.Component(ComponentTypeId(1))),
        )
    }

    @Test
    fun `component target with core is InvalidTarget`() {
        assertEquals(
            SetupCheck.InvalidTarget,
            check(core = true, dependsOn = DependencyTarget.Component(ComponentTypeId(1))),
        )
    }

    @Test
    fun `alive option target without core is ok`() {
        assertEquals(SetupCheck.Ok, check(dependsOn = DependencyTarget.Option(10)))
    }

    @Test
    fun `option target with core is InvalidTarget`() {
        assertEquals(
            SetupCheck.InvalidTarget,
            check(core = true, dependsOn = DependencyTarget.Option(10)),
        )
    }

    @Test
    fun `removed component target is InvalidTarget`() {
        assertEquals(
            SetupCheck.InvalidTarget,
            check(dependsOn = DependencyTarget.Component(ComponentTypeId(4))),
        )
    }

    @Test
    fun `removed option target is InvalidTarget`() {
        assertEquals(
            SetupCheck.InvalidTarget,
            check(dependsOn = DependencyTarget.Option(12)),
        )
    }

    @Test
    fun `missing component target is InvalidTarget`() {
        assertEquals(
            SetupCheck.InvalidTarget,
            check(dependsOn = DependencyTarget.Component(ComponentTypeId(99))),
        )
    }

    @Test
    fun `cross-dictionary component target is InvalidTarget`() {
        assertEquals(
            SetupCheck.InvalidTarget,
            check(dependsOn = DependencyTarget.Component(ComponentTypeId(5))),
        )
    }

    @Test
    fun `cross-dictionary option target is InvalidTarget`() {
        assertEquals(
            SetupCheck.InvalidTarget,
            check(dependsOn = DependencyTarget.Option(20)),
        )
    }

    @Test
    fun `CHOICE with isMultiple is MultiForbiddenForChoice - create and edit path share validator`() {
        assertEquals(
            SetupCheck.MultiForbiddenForChoice,
            check(template = ComponentTemplate.CHOICE, isMultiple = true),
        )
    }

    @Test
    fun `CHOICE without options is valid at domain level - UI forbids separately`() {
        assertEquals(SetupCheck.Ok, check(template = ComponentTemplate.CHOICE))
    }

    @Test
    fun `TEXT with isMultiple stays allowed`() {
        assertEquals(SetupCheck.Ok, check(isMultiple = true))
    }
}
