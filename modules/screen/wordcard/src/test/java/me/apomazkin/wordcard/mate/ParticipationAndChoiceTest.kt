package me.apomazkin.wordcard.mate

import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ChoiceValues
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentType
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.lexeme.ComponentValueId
import me.apomazkin.lexeme.DependencyTarget
import me.apomazkin.mate.state
import me.apomazkin.mate.test.assertEffects
import me.apomazkin.mate.test.assertNoEffects
import me.apomazkin.mate.test.testReduce
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * IS486 фаза 2, зона W3: правило участия (addableTypeIdsFor) + CHOICE-пикер
 * (Msg.SelectComponentOption). phase2_plan.md.
 *
 * Типы: перевод(50, ядро) | часть речи(60, CHOICE, не-ядро, от лексемы, опции 601/602)
 * | род(61 ← опция 601) | пример(62, мульти ← компонент 50).
 */
class ParticipationAndChoiceTest {

    private val reducer = WordCardReducer()

    private fun hierType(
        id: Long,
        template: ComponentTemplate = ComponentTemplate.TEXT,
        core: Boolean = false,
        isMultiple: Boolean = false,
        dependsOn: DependencyTarget = DependencyTarget.Lexeme,
        systemKey: BuiltInComponent? = null,
        name: String? = "t$id",
    ) = ComponentType(
        id = ComponentTypeId(id),
        systemKey = systemKey,
        dictionaryId = 3L,
        name = if (systemKey == null) name else null,
        template = template,
        position = 0,
        isMultiple = isMultiple,
        core = core,
        dependsOn = dependsOn,
        createdAt = Date(0L),
        updatedAt = Date(0L),
    )

    private val translation = hierType(50L, core = true, systemKey = BuiltInComponent.TRANSLATION)
    private val partOfSpeech = hierType(
        60L,
        template = ComponentTemplate.CHOICE,
        systemKey = BuiltInComponent.PART_OF_SPEECH,
    )
    private val gender = hierType(61L, dependsOn = DependencyTarget.Option(601L))
    private val example = hierType(
        62L,
        isMultiple = true,
        dependsOn = DependencyTarget.Component(ComponentTypeId(50L)),
    )
    private val allTypes = listOf(translation, partOfSpeech, gender, example)

    private fun choiceCv(id: Long, optionId: Long?) = savedCv(id, typeId = 60L).copy(
        template = ComponentTemplate.CHOICE,
        selectedOptionId = optionId,
        componentTypeRef = choiceRef,
    )

    // ===== Правило участия =====

    @Test
    fun `draft offers only cores`() {
        val state = loaded(availableTypes = allTypes, lexemes = listOf(lexeme(NOT_IN_DB, emptyList())))
        assertEquals(
            setOf(ComponentTypeId(50L)),
            state.addableTypeIdsFor(state.lexemeList.single()),
        )
    }

    @Test
    fun `real lexeme opens lexeme-target non-cores`() {
        val state = loaded(
            availableTypes = allTypes,
            lexemes = listOf(lexeme(8L, listOf(savedCv(1L, typeId = 50L, origin = "кот")))),
        )
        val addable = state.addableTypeIdsFor(state.lexemeList.single())
        // Часть речи (не-ядро от лексемы) и пример (цель 50 активна) открыты; род — нет.
        assertTrue(ComponentTypeId(60L) in addable)
        assertTrue(ComponentTypeId(62L) in addable)
        assertTrue(ComponentTypeId(61L) !in addable)
        // Перевод не-multi уже добавлен — скрыт.
        assertTrue(ComponentTypeId(50L) !in addable)
    }

    // ===== Девайс-баги (репро 2026-07-21): удаление/повторное добавление CHOICE =====

    /**
     * БАГ-1: у CHOICE origin всегда пуст (payload в selectedOptionId) — ветка
     * «пустой origin = локальный мусор» (IS481, текстовые) НЕ должна его ловить.
     * Удаление сохранённого CHOICE обязано идти через БД (RemoveComponentValue),
     * иначе значение остаётся живым и повторное добавление бьётся о single-cardinality.
     */
    @Test
    fun `removing saved CHOICE emits db remove`() {
        val cv = choiceCv(70L, optionId = 601L)
        val state = loaded(availableTypes = allTypes, lexemes = listOf(lexeme(8L, listOf(cv))))
        val result = reducer.testReduce(state, Msg.RemoveComponentValueRequested(8L, cv.key))
        assertTrue(result.state().isPendingDbOp)
        result.assertEffects(
            setOf(DatasourceEffect.RemoveComponentValue(ComponentValueId(70L), 8L)),
        )
    }

    /**
     * БАГ-2: провал AddValue (OperationFailed) оставлял осиротевший committing-pristine
     * CHOICE в state — мусорный чип, тапы по которому плодили дубли. Пользовательского
     * ввода в CHOICE-pristine нет — при ошибке он удаляется из state.
     */
    @Test
    fun `OperationFailed drops committing CHOICE pristine`() {
        val pristine = pristineCv(1L, typeId = 60L, ref = choiceRef, isCommitting = true).copy(
            template = ComponentTemplate.CHOICE,
            selectedOptionId = 601L,
        )
        val state = loaded(
            isPendingDbOp = true,
            availableTypes = allTypes,
            lexemes = listOf(lexeme(8L, listOf(savedCv(1L, typeId = 50L, origin = "кот"), pristine))),
        )
        val result = reducer.testReduce(state, Msg.OperationFailed(R_STRING_X))
        val comps = result.state().lexemeList.single().components
        assertEquals("CHOICE-pristine удалён, перевод жив", listOf(ComponentTypeId(50L)), comps.map { it.componentTypeId })
    }

    /**
     * БАГ-3: повторный выбор опции, пока AddValue этого типа в полёте (pristine
     * с isCommitting), — игнор: нельзя плодить второй pristine того же типа.
     */
    @Test
    fun `select ignored while pristine of same type in flight`() {
        val pristine = pristineCv(1L, typeId = 60L, ref = choiceRef, isCommitting = true).copy(
            template = ComponentTemplate.CHOICE,
            selectedOptionId = 601L,
        )
        val state = loaded(
            availableTypes = allTypes,
            lexemes = listOf(lexeme(8L, listOf(savedCv(1L, typeId = 50L, origin = "кот"), pristine))),
        )
        val result = reducer.testReduce(state, Msg.SelectComponentOption(8L, ComponentTypeId(60L), 602L))
        assertEquals(state, result.state())
        result.assertNoEffects()
    }

    /**
     * IS486 фаза 4 (spec §6): disabled — компонент не предлагается для добавления
     * новых значений. Существующие значения живут (не проверяется здесь — data-слой
     * их не трогает), но в чипы добавления disabled-тип не попадает.
     */
    @Test
    fun `disabled type is not addable`() {
        val disabledPos = partOfSpeech.copy(enabled = false)
        val state = loaded(
            availableTypes = listOf(translation, disabledPos, example),
            lexemes = listOf(lexeme(8L, listOf(savedCv(1L, typeId = 50L, origin = "кот")))),
        )
        val addable = state.addableTypeIdsFor(state.lexemeList.single())
        assertTrue("disabled часть речи скрыта из предложений", ComponentTypeId(60L) !in addable)
        assertTrue("пример (enabled) предлагается", ComponentTypeId(62L) in addable)
    }

    @Test
    fun `option-dependent opens after choice selected`() {
        val state = loaded(
            availableTypes = allTypes,
            lexemes = listOf(
                lexeme(8L, listOf(savedCv(1L, typeId = 50L, origin = "кот"), choiceCv(2L, optionId = 601L))),
            ),
        )
        val addable = state.addableTypeIdsFor(state.lexemeList.single())
        assertTrue("Род открывается после выбора опции 601", ComponentTypeId(61L) in addable)
    }

    @Test
    fun `option-dependent stays closed on other option`() {
        val state = loaded(
            availableTypes = allTypes,
            lexemes = listOf(
                lexeme(8L, listOf(savedCv(1L, typeId = 50L, origin = "кот"), choiceCv(2L, optionId = 602L))),
            ),
        )
        assertTrue(ComponentTypeId(61L) !in state.addableTypeIdsFor(state.lexemeList.single()))
    }

    @Test
    fun `component-target closed without parent value`() {
        val state = loaded(
            availableTypes = allTypes,
            lexemes = listOf(lexeme(8L, listOf(choiceCv(2L, optionId = 601L)))),
        )
        // Перевода нет → пример (цель 50) закрыт.
        assertTrue(ComponentTypeId(62L) !in state.addableTypeIdsFor(state.lexemeList.single()))
    }

    // ===== SelectComponentOption =====

    @Test
    fun `select on empty adds pristine and emits AddValue with ChoiceValues`() {
        val initial = loaded(
            availableTypes = allTypes,
            lexemes = listOf(lexeme(8L, listOf(savedCv(1L, typeId = 50L, origin = "кот")))),
            nextPristineKey = 5L,
        )
        val result = reducer.testReduce(initial, Msg.SelectComponentOption(8L, ComponentTypeId(60L), 601L))

        val lex = result.state().lexemeList.single()
        val pristine = lex.components.single { it.isPristine }
        assertEquals(601L, pristine.selectedOptionId)
        assertEquals(true, pristine.isCommitting)
        assertEquals(ComponentTemplate.CHOICE, pristine.template)
        assertEquals(true, result.state().isPendingDbOp)
        result.assertEffects(
            setOf(
                DatasourceEffect.UpsertComponentValue.AddValue(
                    wordId = 7L,
                    dictionaryId = 3L,
                    lexemeId = 8L,
                    pristineKey = 5L,
                    componentTypeId = ComponentTypeId(60L),
                    componentTypeRef = ComponentTypeRef.BuiltIn(BuiltInComponent.PART_OF_SPEECH),
                    data = ChoiceValues(601L),
                ),
            ),
        )
    }

    /**
     * Решение 2026-07-21: смена опции как операция упразднена (глухой чип,
     * смена = удалить + добавить) — выбор при существующем значении типа
     * игнорируется, UpdateValue-путь выпилен.
     */
    @Test
    fun `select with existing value is ignored`() {
        val initial = loaded(
            availableTypes = allTypes,
            lexemes = listOf(lexeme(8L, listOf(choiceCv(2L, optionId = 601L)))),
        )
        val result = reducer.testReduce(initial, Msg.SelectComponentOption(8L, ComponentTypeId(60L), 602L))
        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `select on draft is no-op`() {
        val initial = loaded(
            availableTypes = allTypes,
            lexemes = listOf(lexeme(NOT_IN_DB, emptyList())),
        )
        val result = reducer.testReduce(initial, Msg.SelectComponentOption(NOT_IN_DB, ComponentTypeId(60L), 601L))
        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `select guarded by pending`() {
        val initial = loaded(
            isPendingDbOp = true,
            availableTypes = allTypes,
            lexemes = listOf(lexeme(8L, listOf(choiceCv(2L, optionId = 601L)))),
        )
        val result = reducer.testReduce(initial, Msg.SelectComponentOption(8L, ComponentTypeId(60L), 602L))
        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    private companion object {
        val choiceRef = ComponentTypeRef.BuiltIn(BuiltInComponent.PART_OF_SPEECH)
    }
}
