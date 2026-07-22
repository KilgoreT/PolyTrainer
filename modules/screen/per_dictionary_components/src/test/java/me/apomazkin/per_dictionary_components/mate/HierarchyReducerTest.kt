package me.apomazkin.per_dictionary_components.mate

import me.apomazkin.lexeme.ComponentOption
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentType
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.DeletionImpact
import me.apomazkin.lexeme.DependencyTarget
import me.apomazkin.lexeme.OptionOutcome
import me.apomazkin.lexeme.PerDictionarySnapshot
import me.apomazkin.lexeme.Scope
import me.apomazkin.lexeme.SetEnabledOutcome
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.logger.LogLevel
import me.apomazkin.mate.effects
import me.apomazkin.mate.state
import me.apomazkin.mate.test.assertNoEffects
import me.apomazkin.mate.test.testReduce
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * IS486 фаза 3 (блок B): reducer-тесты конструктора иерархии —
 * рубильник enabled (spec §6, §7.8), пикер цели (В1), варианты CHOICE (В2),
 * builtin-ряды (В3), degraded-вычисление в маппере снапшота (spec §7.6).
 */
class HierarchyReducerTest {

    private val reducer = PerDictionaryComponentsReducer(
        logger = object : LexemeLogger {
            override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {}
        }
    )

    private val now = Date(0L)
    private val DICT_ID = 7L

    // ---- helpers ----

    private fun row(
        id: Long = 1L,
        name: String = "Notes",
        systemKey: String? = null,
        core: Boolean = false,
        enabled: Boolean = true,
        dependsOn: DependencyTarget = DependencyTarget.Lexeme,
        options: List<OptionRow> = emptyList(),
    ) = PerDictRow(
        typeId = ComponentTypeId(id),
        name = name,
        template = if (options.isEmpty()) ComponentTemplate.TEXT else ComponentTemplate.CHOICE,
        isMultiple = false,
        isGlobal = false,
        valueCount = 0,
        systemKey = systemKey,
        core = core,
        enabled = enabled,
        dependsOn = dependsOn,
        options = options,
    )

    private fun baseState(items: List<PerDictRow>? = null, nextEpoch: Long = 0L) =
        PerDictionaryComponentsScreenState(
            dictionaryId = DICT_ID,
            items = items,
            nextEpoch = nextEpoch,
        )

    private fun createDialogState(
        template: ComponentTemplate = ComponentTemplate.TEXT,
        target: DependencyTarget = DependencyTarget.Lexeme,
        core: Boolean = true,
        optionDrafts: List<String> = emptyList(),
        name: String = "Kind",
    ) = baseState(items = emptyList(), nextEpoch = 1L).copy(
        createDialog = CreateDialogState(
            epochId = 1L,
            name = name,
            template = template,
            scope = Scope.PerDictionaries(listOf(DICT_ID)),
            target = target,
            core = core,
            optionDrafts = optionDrafts,
        ),
    )

    private fun editDialogState(
        typeId: Long = 1L,
        target: DependencyTarget = DependencyTarget.Lexeme,
        core: Boolean = true,
        existingOptions: List<EditOptionRow> = emptyList(),
        newOptionDrafts: List<String> = emptyList(),
        optionDeleteConfirm: OptionDeleteConfirmState? = null,
        isDeletingOption: Boolean = false,
    ) = baseState(items = listOf(row(id = typeId)), nextEpoch = 1L).copy(
        editDialog = EditDialogState(
            epochId = 1L,
            typeId = ComponentTypeId(typeId),
            originalName = "Notes",
            originalTemplate = ComponentTemplate.CHOICE,
            originalIsMultiple = false,
            name = "Notes",
            template = ComponentTemplate.CHOICE,
            isMultiple = false,
            originalTarget = DependencyTarget.Lexeme,
            originalCore = true,
            target = target,
            core = core,
            existingOptions = existingOptions,
            newOptionDrafts = newOptionDrafts,
            optionDeleteConfirm = optionDeleteConfirm,
            isDeletingOption = isDeletingOption,
        ),
    )

    private fun impact(values: Int = 2, descendant: Int = 1) = DeletionImpact(
        valueCount = values,
        dictionariesWithValues = listOf(DICT_ID),
        affectedQuizConfigs = emptyList(),
        affectedPrefs = emptyList(),
        degradedComponents = emptyList(),
        descendantValueCount = descendant,
    )

    // ============================================================
    // Рубильник enabled (spec §6, §7.8)
    // ============================================================

    @Test
    fun `ToggleEnabled emits SetEnabled and marks pending`() {
        val s = baseState(items = listOf(row(id = 5L, systemKey = "translation", core = true)))
        val result = reducer.testReduce(s, Msg.ToggleEnabled(ComponentTypeId(5L), false))
        assertTrue(ComponentTypeId(5L) in result.state().pendingEnabledToggles)
        assertEquals(
            setOf(DatasourceEffect.SetEnabled(ComponentTypeId(5L), false)),
            result.effects(),
        )
    }

    @Test
    fun `ToggleEnabled guards pending and same value`() {
        val s = baseState(items = listOf(row(id = 5L)))
            .copy(pendingEnabledToggles = setOf(ComponentTypeId(5L)))
        reducer.testReduce(s, Msg.ToggleEnabled(ComponentTypeId(5L), false)).assertNoEffects()

        // Тот же value (enabled=true → true) — no-op.
        val s2 = baseState(items = listOf(row(id = 6L, enabled = true)))
        reducer.testReduce(s2, Msg.ToggleEnabled(ComponentTypeId(6L), true)).assertNoEffects()
    }

    @Test
    fun `SetEnabledResult clears pending, LastEnabledCore snackbars`() {
        val s = baseState(items = listOf(row(id = 5L)))
            .copy(pendingEnabledToggles = setOf(ComponentTypeId(5L)))
        val ok = reducer.testReduce(
            s,
            Msg.SetEnabledResult(ComponentTypeId(5L), SetEnabledOutcome.LastEnabledCore),
        )
        assertTrue(ok.state().pendingEnabledToggles.isEmpty())
        assertEquals(setOf(UiEffect.Snackbar("Last enabled core component")), ok.effects())

        val success = reducer.testReduce(
            s,
            Msg.SetEnabledResult(
                ComponentTypeId(5L),
                SetEnabledOutcome.Success(domainType(5L)),
            ),
        )
        assertTrue(success.state().pendingEnabledToggles.isEmpty())
        success.assertNoEffects()
    }

    // ============================================================
    // Пикер цели (В1)
    // ============================================================

    @Test
    fun `CreateTargetChange to component forces core off`() {
        val s = createDialogState(core = true)
        val result = reducer.testReduce(
            s,
            Msg.CreateTargetChange(DependencyTarget.Component(ComponentTypeId(2L))),
        )
        val dlg = result.state().createDialog!!
        assertEquals(DependencyTarget.Component(ComponentTypeId(2L)), dlg.target)
        assertFalse("ядро снято при уводе с лексемы", dlg.core)
    }

    @Test
    fun `CreateCoreToggle guarded when target is not lexeme`() {
        val s = createDialogState(target = DependencyTarget.Option(9L), core = false)
        val result = reducer.testReduce(s, Msg.CreateCoreToggle(true))
        assertFalse(result.state().createDialog!!.core)
        result.assertNoEffects()
    }

    @Test
    fun `CreateTemplateChange to CHOICE resets isMultiple`() {
        val s = createDialogState().let {
            it.copy(createDialog = it.createDialog!!.copy(isMultiple = true))
        }
        val result = reducer.testReduce(s, Msg.CreateTemplateChange(ComponentTemplate.CHOICE))
        assertFalse(result.state().createDialog!!.isMultiple)
    }

    // ============================================================
    // Варианты CHOICE в Create (В2)
    // ============================================================

    @Test
    fun `create option drafts add change remove`() {
        var s = createDialogState(template = ComponentTemplate.CHOICE)
        s = reducer.testReduce(s, Msg.CreateOptionAdd).state()
        s = reducer.testReduce(s, Msg.CreateOptionAdd).state()
        assertEquals(listOf("", ""), s.createDialog!!.optionDrafts)
        s = reducer.testReduce(s, Msg.CreateOptionChange(0, "муж")).state()
        assertEquals(listOf("муж", ""), s.createDialog!!.optionDrafts)
        s = reducer.testReduce(s, Msg.CreateOptionRemove(1)).state()
        assertEquals(listOf("муж"), s.createDialog!!.optionDrafts)
    }

    @Test
    fun `SubmitCreate CHOICE without options sets error`() {
        val s = createDialogState(template = ComponentTemplate.CHOICE, optionDrafts = listOf("  "))
        val result = reducer.testReduce(s, Msg.SubmitCreate)
        assertTrue(result.state().createDialog!!.optionsError)
        result.assertNoEffects()
    }

    @Test
    fun `SubmitCreate CHOICE passes trimmed labels and target`() {
        val target = DependencyTarget.Option(9L)
        val s = createDialogState(
            template = ComponentTemplate.CHOICE,
            target = target,
            core = false,
            optionDrafts = listOf(" муж ", "", "жен"),
        )
        val result = reducer.testReduce(s, Msg.SubmitCreate)
        assertTrue(result.state().isCreating)
        val effect = result.effects().single() as DatasourceEffect.CreateComponent
        assertEquals(listOf("муж", "жен"), effect.optionLabels)
        assertEquals(target, effect.target)
        assertFalse(effect.core)
    }

    // ============================================================
    // Edit: init, builtin guard, опции (В2)
    // ============================================================

    @Test
    fun `OpenEditDialog builtin is no-op`() {
        val s = baseState(items = listOf(row(id = 5L, systemKey = "translation")))
        val result = reducer.testReduce(s, Msg.OpenEditDialog(ComponentTypeId(5L)))
        assertNull(result.state().editDialog)
        result.assertNoEffects()
    }

    @Test
    fun `OpenEditDialog seeds target core and options from row`() {
        val target = DependencyTarget.Component(ComponentTypeId(2L))
        val s = baseState(
            items = listOf(
                row(
                    id = 1L,
                    core = false,
                    dependsOn = target,
                    options = listOf(OptionRow(optionId = 10L, systemKey = null, label = "муж", position = 0)),
                ),
            ),
        )
        val dlg = reducer.testReduce(s, Msg.OpenEditDialog(ComponentTypeId(1L))).state().editDialog!!
        assertEquals(target, dlg.target)
        assertEquals(target, dlg.originalTarget)
        assertFalse(dlg.core)
        assertEquals(1, dlg.existingOptions.size)
        assertEquals("муж", dlg.existingOptions.single().label)
    }

    @Test
    fun `EditOptionDeleteRequest opens confirm and loads impact`() {
        val s = editDialogState(
            existingOptions = listOf(EditOptionRow(10L, null, "муж", "муж")),
        )
        val result = reducer.testReduce(s, Msg.EditOptionDeleteRequest(10L))
        val confirm = result.state().editDialog!!.optionDeleteConfirm!!
        assertEquals(10L, confirm.optionId)
        assertTrue(confirm.isLoadingImpact)
        assertEquals(setOf(DatasourceEffect.LoadOptionImpact(10L)), result.effects())
    }

    @Test
    fun `ConfirmOptionDelete guarded while impact loading`() {
        val s = editDialogState(
            existingOptions = listOf(EditOptionRow(10L, null, "муж", "муж")),
            optionDeleteConfirm = OptionDeleteConfirmState(10L, "муж", isLoadingImpact = true),
        )
        reducer.testReduce(s, Msg.ConfirmOptionDelete).assertNoEffects()
    }

    @Test
    fun `ConfirmOptionDelete emits DeleteOption`() {
        val s = editDialogState(
            existingOptions = listOf(EditOptionRow(10L, null, "муж", "муж")),
            optionDeleteConfirm = OptionDeleteConfirmState(10L, "муж", impact = impact()),
        )
        val result = reducer.testReduce(s, Msg.ConfirmOptionDelete)
        assertTrue(result.state().editDialog!!.isDeletingOption)
        assertEquals(setOf(DatasourceEffect.DeleteOption(epochId = 1L, optionId = 10L)), result.effects())
    }

    /**
     * Решение 2026-07-21 (девайс-фидбек O3): удаление опции — немедленная
     * самостоятельная операция → Edit-диалог закрывается целиком со снеком.
     */
    @Test
    fun `OptionDeleteResult Deleted closes edit dialog and snackbars combined count`() {
        val s = editDialogState(
            existingOptions = listOf(
                EditOptionRow(10L, null, "муж", "муж"),
                EditOptionRow(11L, null, "жен", "жен"),
            ),
            optionDeleteConfirm = OptionDeleteConfirmState(10L, "муж", impact = impact()),
            isDeletingOption = true,
        )
        val result = reducer.testReduce(
            s,
            Msg.OptionDeleteResult(1L, OptionOutcome.Deleted(impact(values = 2, descendant = 1))),
        )
        assertNull("диалог закрыт", result.state().editDialog)
        assertFalse(result.state().isEditing)
        assertEquals(setOf(UiEffect.Snackbar("3 values hidden")), result.effects())
    }

    /**
     * IS486 умный сброс (решение 2026-07-21): смена цели на Submit НЕ применяется
     * сразу — открывается обязательный rebind-конфирм с загрузкой impact.
     */
    @Test
    fun `SubmitEdit with target change opens rebind confirm and loads impact`() {
        val target = DependencyTarget.Component(ComponentTypeId(3L))
        val s = editDialogState(target = target, core = false)
        val result = reducer.testReduce(s, Msg.SubmitEdit)
        assertFalse("edit не запущен до конфирма", result.state().isEditing)
        val confirm = result.state().editDialog!!.rebindConfirm!!
        assertTrue(confirm.isLoadingImpact)
        assertEquals(
            setOf(DatasourceEffect.LoadRebindImpact(ComponentTypeId(1L), target, false)),
            result.effects(),
        )
    }

    @Test
    fun `RebindImpactLoaded fills confirm, ConfirmRebind emits EditComponent`() {
        val target = DependencyTarget.Component(ComponentTypeId(3L))
        var s = editDialogState(
            target = target,
            core = false,
            existingOptions = listOf(
                EditOptionRow(10L, null, "муж", "мужской"),   // изменён → rename
                EditOptionRow(11L, null, "жен", "жен"),       // не изменён
            ),
            newOptionDrafts = listOf(" общ ", ""),
        ).let {
            it.copy(editDialog = it.editDialog!!.copy(rebindConfirm = RebindConfirmState(isLoadingImpact = true)))
        }

        s = reducer.testReduce(s, Msg.RebindImpactLoaded(ComponentTypeId(1L), impact())).state()
        val confirm = s.editDialog!!.rebindConfirm!!
        assertFalse(confirm.isLoadingImpact)
        assertEquals(impact(), confirm.impact)

        val result = reducer.testReduce(s, Msg.ConfirmRebind)
        assertTrue(result.state().isEditing)
        assertNull("конфирм закрыт", result.state().editDialog!!.rebindConfirm)
        val effect = result.effects().single() as DatasourceEffect.EditComponent
        assertEquals(listOf(10L to "мужской"), effect.optionRenames)
        assertEquals(listOf("общ"), effect.optionAdds)
        assertEquals(target, effect.target)
        assertFalse(effect.core)
    }

    @Test
    fun `ConfirmRebind guarded while impact loading`() {
        val s = editDialogState(target = DependencyTarget.Component(ComponentTypeId(3L)), core = false).let {
            it.copy(editDialog = it.editDialog!!.copy(rebindConfirm = RebindConfirmState(isLoadingImpact = true)))
        }
        reducer.testReduce(s, Msg.ConfirmRebind).assertNoEffects()
    }

    /** Без смены цели/ядра Submit идёт напрямую (конфирм не нужен). */
    @Test
    fun `SubmitEdit without target change goes straight to EditComponent`() {
        val s = editDialogState(
            target = DependencyTarget.Lexeme,
            core = true,
            existingOptions = listOf(EditOptionRow(10L, null, "муж", "мужской")),
        )
        val result = reducer.testReduce(s, Msg.SubmitEdit)
        assertTrue(result.state().isEditing)
        val effect = result.effects().single() as DatasourceEffect.EditComponent
        assertEquals(listOf(10L to "мужской"), effect.optionRenames)
    }

    @Test
    fun `SubmitEdit guarded while option delete in flight`() {
        val s = editDialogState(isDeletingOption = true)
        reducer.testReduce(s, Msg.SubmitEdit).assertNoEffects()
    }

    // ============================================================
    // Ранний цикл-чек в пикере цели (девайс-фидбек C2, решение 2026-07-21)
    // ============================================================

    /**
     * Выбор цели, создающей цикл, отклоняется сразу в пикере: target не меняется,
     * показывается тост. Data-проверка CycleDetected остаётся подстраховкой.
     */
    @Test
    fun `EditTargetChange to descendant component is rejected with toast`() {
        // B(2) зависит от A(1); Edit A → попытка выбрать цель B = цикл.
        val s = baseState(
            items = listOf(
                row(id = 1L),
                row(id = 2L, dependsOn = DependencyTarget.Component(ComponentTypeId(1L))),
            ),
            nextEpoch = 1L,
        ).copy(
            editDialog = EditDialogState(
                epochId = 1L,
                typeId = ComponentTypeId(1L),
                originalName = "A", originalTemplate = ComponentTemplate.TEXT, originalIsMultiple = false,
                name = "A", template = ComponentTemplate.TEXT, isMultiple = false,
            ),
        )
        val result = reducer.testReduce(s, Msg.EditTargetChange(DependencyTarget.Component(ComponentTypeId(2L))))
        assertEquals("цель не изменилась", DependencyTarget.Lexeme, result.state().editDialog!!.target)
        assertEquals(setOf(UiEffect.Snackbar("Dependency cycle detected")), result.effects())
    }

    @Test
    fun `EditTargetChange to option of descendant is rejected with toast`() {
        // CHOICE C(3) зависит от A(1); Edit A → выбор цели «опция 30 у C» = цикл через владельца.
        val s = baseState(
            items = listOf(
                row(id = 1L),
                row(
                    id = 3L,
                    dependsOn = DependencyTarget.Component(ComponentTypeId(1L)),
                    options = listOf(OptionRow(optionId = 30L, systemKey = null, label = "x", position = 0)),
                ),
            ),
            nextEpoch = 1L,
        ).copy(
            editDialog = EditDialogState(
                epochId = 1L,
                typeId = ComponentTypeId(1L),
                originalName = "A", originalTemplate = ComponentTemplate.TEXT, originalIsMultiple = false,
                name = "A", template = ComponentTemplate.TEXT, isMultiple = false,
            ),
        )
        val result = reducer.testReduce(s, Msg.EditTargetChange(DependencyTarget.Option(30L)))
        assertEquals(DependencyTarget.Lexeme, result.state().editDialog!!.target)
        assertEquals(setOf(UiEffect.Snackbar("Dependency cycle detected")), result.effects())
    }

    // ============================================================
    // Degraded в маппере снапшота (spec §7.6)
    // ============================================================

    @Test
    fun `toPerDictRows computes degraded for dead targets`() {
        val choice = domainType(id = 2L, template = ComponentTemplate.CHOICE)
        val snapshot = PerDictionarySnapshot(
            dictionaryId = DICT_ID,
            dictionaryName = "ES",
            types = listOf(
                domainType(id = 1L),                                                     // цель-лексема
                choice,                                                                  // живой CHOICE
                domainType(id = 3L, dependsOn = DependencyTarget.Component(ComponentTypeId(2L))),  // жив
                domainType(id = 4L, dependsOn = DependencyTarget.Component(ComponentTypeId(99L))), // цель мертва
                domainType(id = 5L, dependsOn = DependencyTarget.Option(10L)),           // опция жива
                domainType(id = 6L, dependsOn = DependencyTarget.Option(77L)),           // опция мертва
            ),
            valueCountByType = emptyMap(),
            optionsByType = mapOf(
                ComponentTypeId(2L) to listOf(
                    ComponentOption(id = 10L, componentTypeId = ComponentTypeId(2L), systemKey = null, label = "муж", position = 0),
                ),
            ),
        )
        val rows = snapshot.toPerDictRows().associateBy { it.typeId.id }
        assertFalse(rows[1L]!!.degraded)
        assertFalse(rows[3L]!!.degraded)
        assertTrue(rows[4L]!!.degraded)
        assertFalse(rows[5L]!!.degraded)
        assertTrue(rows[6L]!!.degraded)
        assertNotNull(rows[2L]!!.options.singleOrNull())
    }

    // ---- domain fixture ----

    private fun domainType(
        id: Long,
        template: ComponentTemplate = ComponentTemplate.TEXT,
        dependsOn: DependencyTarget = DependencyTarget.Lexeme,
    ) = ComponentType(
        id = ComponentTypeId(id),
        systemKey = null,
        dictionaryId = DICT_ID,
        name = "T$id",
        template = template,
        position = 0,
        isMultiple = false,
        dependsOn = dependsOn,
        createdAt = now,
        updatedAt = now,
    )
}
