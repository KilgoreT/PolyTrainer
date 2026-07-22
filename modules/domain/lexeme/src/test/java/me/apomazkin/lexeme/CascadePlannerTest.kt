package me.apomazkin.lexeme

import me.apomazkin.lexeme.HierarchyFixtures.option
import me.apomazkin.lexeme.HierarchyFixtures.type
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * IS486 фаза 1, зона E1: каскад-планировщик (spec §9.2, phase1_plan.md § Зона E).
 *
 * Граф D1: Перевод(1, ядро) ← Пример(2, мульти) ; Часть речи(3, CHOICE, опции 30/31)
 * ← Род(4 ← опция 30) ← Форма(5 ← компонент 4). Пример зависит от Перевода.
 */
class CascadePlannerTest {

    private val graph = ComponentGraph(
        types = listOf(
            type(id = 1, core = true),
            type(id = 2, isMultiple = true, dependsOn = DependencyTarget.Component(ComponentTypeId(1))),
            type(id = 3, template = ComponentTemplate.CHOICE),
            type(id = 4, dependsOn = DependencyTarget.Option(30)),
            type(id = 5, dependsOn = DependencyTarget.Component(ComponentTypeId(4))),
        ),
        options = listOf(
            option(id = 30, ownerTypeId = 3),
            option(id = 31, ownerTypeId = 3),
        ),
    )

    private fun value(id: Long, lexemeId: Long, typeId: Long, optionId: Long? = null) =
        CascadeValue(id = id, lexemeId = lexemeId, typeId = ComponentTypeId(typeId), optionId = optionId)

    // Лексема L1: перевод(100) + 2 примера(101,102) + часть речи=сущ(103) + род(104) + форма(105).
    private val fullLexeme = listOf(
        value(100, 1, 1),
        value(101, 1, 2),
        value(102, 1, 2),
        value(103, 1, 3, optionId = 30),
        value(104, 1, 4),
        value(105, 1, 5),
    )

    @Test
    fun `component removed - own values and whole subtree die`() {
        // Убили Перевод(1): его значение + оба примера; часть речи/род/форма живут (не зависят).
        val plan = graph.planCascade(fullLexeme, CascadeEvent.ComponentRemoved(ComponentTypeId(1)))
        assertEquals(setOf(100L, 101L, 102L), plan)
    }

    @Test
    fun `option deactivation kills only option subtree`() {
        // Умерло значение-выбор опции 30: род(104) и форма(105) вниз; перевод/примеры не тронуты.
        val plan = graph.planCascade(fullLexeme, CascadeEvent.ValueRemoved(103))
        assertEquals(setOf(103L, 104L, 105L), plan)
    }

    @Test
    fun `combined K4 K5 - option removal kills selections and dependent subtree`() {
        // Удаление опции 30 (К4+К5): выбор(103) + зависимый род(104) + его форма(105).
        val plan = graph.planCascade(fullLexeme, CascadeEvent.OptionRemoved(30))
        assertEquals(setOf(103L, 104L, 105L), plan)
    }

    // ===== IS486 умный сброс (решение 2026-07-21): CascadeEvent.TargetRebound =====

    /**
     * Пример перепривязан «Самостоятельный → от Перевода»: в лексеме перевод есть —
     * новое условие выполнено, план ПУСТ (ничего не сбрасывается).
     */
    @Test
    fun `rebind to satisfied target plans nothing`() {
        // Граф с УЖЕ подменённой целью: Пример(2) → Component(1) (как в fixture).
        val plan = graph.planCascade(fullLexeme, CascadeEvent.TargetRebound(ComponentTypeId(2)))
        assertEquals(emptySet<Long>(), plan)
    }

    /**
     * Форма перепривязана «от Рода → от опции 31 (жен.)»: в лексеме выбрана опция 30 —
     * новое условие НЕ выполнено, значение формы гаснет.
     */
    @Test
    fun `rebind to unsatisfied option kills value in that lexeme`() {
        val rebound = ComponentGraph(
            types = graph.types.map {
                if (it.id == ComponentTypeId(5)) it.copy(dependsOn = DependencyTarget.Option(31)) else it
            },
            options = graph.options,
        )
        val plan = rebound.planCascade(fullLexeme, CascadeEvent.TargetRebound(ComponentTypeId(5)))
        assertEquals(setOf(105L), plan)
    }

    /**
     * Пер-лексемность: Род перепривязан «от опции 30 → от опции 31». Лексема L1
     * выбрала 30 — род и его поддерево гаснут; лексема L2 выбрала 31 — живут.
     */
    @Test
    fun `rebind is per-lexeme - only unsatisfied lexemes reset with subtree`() {
        val rebound = ComponentGraph(
            types = graph.types.map {
                if (it.id == ComponentTypeId(4)) it.copy(dependsOn = DependencyTarget.Option(31)) else it
            },
            options = graph.options,
        )
        val values = fullLexeme + listOf(
            value(200, 2, 1),
            value(203, 2, 3, optionId = 31),
            value(204, 2, 4),
            value(205, 2, 5),
        )
        val plan = rebound.planCascade(values, CascadeEvent.TargetRebound(ComponentTypeId(4)))
        // L1: род(104) без опоры (выбрана 30) + форма(105) вниз; L2 (выбор 31) живёт целиком.
        assertEquals(setOf(104L, 105L), plan)
    }

    @Test
    fun `multi target - death of non-last value plans nothing below`() {
        // Пример-мульти: умер один из двух — цель «Пример» ещё активна.
        val graphWithDependentOnExample = ComponentGraph(
            types = graph.types + type(id = 6, dependsOn = DependencyTarget.Component(ComponentTypeId(2))),
            options = graph.options,
        )
        val values = fullLexeme + value(106, 1, 6)
        val plan = graphWithDependentOnExample.planCascade(values, CascadeEvent.ValueRemoved(101))
        assertEquals(setOf(101L), plan)
    }

    @Test
    fun `multi target - death of last value cascades below`() {
        val graphWithDependentOnExample = ComponentGraph(
            types = graph.types + type(id = 6, dependsOn = DependencyTarget.Component(ComponentTypeId(2))),
            options = graph.options,
        )
        // Только один живой пример (101) + зависимый(106).
        val values = listOf(value(100, 1, 1), value(101, 1, 2), value(106, 1, 6))
        val plan = graphWithDependentOnExample.planCascade(values, CascadeEvent.ValueRemoved(101))
        assertEquals(setOf(101L, 106L), plan)
    }

    @Test
    fun `chain depth three cascades through`() {
        // Смерть части речи: 103 → род(104) → форма(105) (цепочка глубины 3).
        val plan = graph.planCascade(fullLexeme, CascadeEvent.ValueRemoved(103))
        assertEquals(setOf(103L, 104L, 105L), plan)
    }

    @Test
    fun `cascade is per lexeme - other lexeme untouched`() {
        val secondLexeme = listOf(
            value(200, 2, 1),
            value(203, 2, 3, optionId = 30),
            value(204, 2, 4),
        )
        // Убили значение опции только у L1 — L2 живёт.
        val plan = graph.planCascade(fullLexeme + secondLexeme, CascadeEvent.ValueRemoved(103))
        assertEquals(setOf(103L, 104L, 105L), plan)
    }

    @Test
    fun `option removed kills selections on all lexemes`() {
        val secondLexeme = listOf(
            value(200, 2, 1),
            value(203, 2, 3, optionId = 30),
            value(204, 2, 4),
        )
        val plan = graph.planCascade(fullLexeme + secondLexeme, CascadeEvent.OptionRemoved(30))
        assertEquals(setOf(103L, 104L, 105L, 203L, 204L), plan)
    }

    @Test
    fun `lexeme target values are never cascaded in phase one`() {
        // Убили перевод — часть речи (цель-лексема) не сбрасывается планировщиком фазы 1.
        val plan = graph.planCascade(fullLexeme, CascadeEvent.ComponentRemoved(ComponentTypeId(1)))
        assertEquals(false, plan.contains(103L))
    }
}
