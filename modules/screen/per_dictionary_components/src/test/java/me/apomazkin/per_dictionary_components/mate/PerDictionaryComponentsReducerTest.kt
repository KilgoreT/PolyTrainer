package me.apomazkin.per_dictionary_components.mate

import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentType
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.CreateOutcome
import me.apomazkin.lexeme.DeleteOutcome
import me.apomazkin.lexeme.DeletionImpact
import me.apomazkin.lexeme.EditOutcome
import me.apomazkin.lexeme.NameError
import me.apomazkin.lexeme.PerDictionarySnapshot
import me.apomazkin.lexeme.Scope
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.logger.LogLevel
import me.apomazkin.mate.NavigationEffect
import me.apomazkin.mate.effects
import me.apomazkin.mate.state
import me.apomazkin.mate.test.assertNoEffects
import me.apomazkin.mate.test.testReduce
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * Unit tests для [PerDictionaryComponentsReducer]. Зеркально CM tests +
 * - scope preselect в OpenCreateDialog (Scope.PerDictionaries([dictionaryId])),
 * - `dictionaryName` propagation через ItemsLoaded,
 * - `toPerDictRows(dictionaryId=...)` helper coverage.
 *
 * Все critical fixes (F123/F124/F127/F132/F136/F138/F140) проверяются с самого начала.
 */
class PerDictionaryComponentsReducerTest {

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
        isGlobal: Boolean = false,
        isMultiple: Boolean = false,
        valueCount: Int = 0,
    ) = PerDictRow(
        typeId = ComponentTypeId(id),
        name = name,
        template = ComponentTemplate.TEXT,
        isMultiple = isMultiple,
        isGlobal = isGlobal,
        valueCount = valueCount,
    )

    private fun baseState(
        items: List<PerDictRow>? = null,
        nextEpoch: Long = 0L,
    ) = PerDictionaryComponentsScreenState(
        dictionaryId = DICT_ID,
        items = items,
        nextEpoch = nextEpoch,
    )

    private fun stateWithRows(vararg rows: PerDictRow) =
        baseState(items = rows.toList())

    private fun stateWithCreateDialog(
        epochId: Long = 1L,
        name: String = "",
        template: ComponentTemplate = ComponentTemplate.TEXT,
        isMultiple: Boolean = false,
        scope: Scope = Scope.PerDictionaries(listOf(DICT_ID)),
        nameError: NameError? = null,
        isCreating: Boolean = false,
    ) = baseState(items = emptyList(), nextEpoch = epochId).copy(
        isCreating = isCreating,
        createDialog = CreateDialogState(
            epochId = epochId,
            name = name,
            template = template,
            isMultiple = isMultiple,
            scope = scope,
            nameError = nameError,
        ),
    )

    private fun stateWithDeleteConfirm(
        epochId: Long = 1L,
        typeId: Long = 1L,
        name: String = "Notes",
        impact: DeletionImpact? = null,
        isLoadingImpact: Boolean = false,
        isDeleting: Boolean = false,
    ) = baseState(
        items = listOf(row(id = typeId, name = name)),
        nextEpoch = epochId,
    ).copy(
        isDeleting = isDeleting,
        deleteConfirm = DeleteConfirmState(
            epochId = epochId,
            typeId = ComponentTypeId(typeId),
            name = name,
            impact = impact,
            isLoadingImpact = isLoadingImpact,
        ),
    )

    private fun domainType(
        id: Long = 1L,
        name: String = "Notes",
        dictionaryId: Long? = DICT_ID,
        isMultiple: Boolean = false,
    ) = ComponentType(
        id = ComponentTypeId(id),
        systemKey = null,
        dictionaryId = dictionaryId,
        name = name,
        template = ComponentTemplate.TEXT,
        position = 0,
        isMultiple = isMultiple,
        createdAt = now,
        updatedAt = now,
    )

    private fun emptyImpact() = DeletionImpact(
        valueCount = 0,
        dictionariesWithValues = emptyList(),
        affectedQuizConfigs = emptyList(),
        affectedPrefs = emptyList(),
    )

    // ---- phase 2 helpers ----

    private fun editDialog(
        epochId: Long = 1L,
        typeId: Long = 1L,
        originalName: String = "Notes",
        originalTemplate: ComponentTemplate = ComponentTemplate.TEXT,
        originalIsMultiple: Boolean = false,
        name: String = originalName,
        template: ComponentTemplate = originalTemplate,
        isMultiple: Boolean = originalIsMultiple,
        nameError: EditNameError? = null,
        impactedLexemesPreview: ImpactedLexemesPreview? = null,
    ) = EditDialogState(
        epochId = epochId,
        typeId = ComponentTypeId(typeId),
        originalName = originalName,
        originalTemplate = originalTemplate,
        originalIsMultiple = originalIsMultiple,
        name = name,
        template = template,
        isMultiple = isMultiple,
        nameError = nameError,
        impactedLexemesPreview = impactedLexemesPreview,
    )

    private fun stateWithEditDialog(
        epochId: Long = 1L,
        typeId: Long = 1L,
        originalName: String = "Notes",
        originalTemplate: ComponentTemplate = ComponentTemplate.TEXT,
        originalIsMultiple: Boolean = false,
        name: String = originalName,
        template: ComponentTemplate = originalTemplate,
        isMultiple: Boolean = originalIsMultiple,
        nameError: EditNameError? = null,
        impactedLexemesPreview: ImpactedLexemesPreview? = null,
        isEditing: Boolean = false,
    ) = baseState(
        items = listOf(row(id = typeId, name = originalName, isMultiple = originalIsMultiple)),
        nextEpoch = epochId,
    ).copy(
        isEditing = isEditing,
        editDialog = editDialog(
            epochId = epochId,
            typeId = typeId,
            originalName = originalName,
            originalTemplate = originalTemplate,
            originalIsMultiple = originalIsMultiple,
            name = name,
            template = template,
            isMultiple = isMultiple,
            nameError = nameError,
            impactedLexemesPreview = impactedLexemesPreview,
        ),
    )

    // ===== 4.1 Lifecycle =====

    @Test
    fun `ItemsLoaded with snapshot updates items, dictionaryName and clears isLoading`() {
        val initial = baseState().copy(isLoading = true)
        val snapshot = PerDictionarySnapshot(
            dictionaryId = DICT_ID,
            dictionaryName = "Spanish",
            types = listOf(domainType(id = 1L, name = "Notes")),
            valueCountByType = mapOf(ComponentTypeId(1L) to 5),
        )

        val result = reducer.testReduce(initial, Msg.ItemsLoaded(snapshot))

        assertEquals(false, result.state().isLoading)
        assertEquals("Spanish", result.state().dictionaryName)
        assertEquals(1, result.state().items?.size)
        assertEquals("Notes", result.state().items?.first()?.name)
        assertEquals(5, result.state().items?.first()?.valueCount)
        result.assertNoEffects()
    }

    @Test
    fun `ItemsLoaded empty snapshot - state is empty`() {
        val initial = baseState().copy(isLoading = true)
        val snapshot = PerDictionarySnapshot(
            dictionaryId = DICT_ID,
            dictionaryName = "Spanish",
            types = emptyList(),
            valueCountByType = emptyMap(),
        )

        val result = reducer.testReduce(initial, Msg.ItemsLoaded(snapshot))

        assertTrue(result.state().isEmpty)
    }

    @Test
    fun `ItemsLoadFailed emits snackbar and clears isLoading`() {
        val initial = baseState().copy(isLoading = true)

        val result = reducer.testReduce(initial, Msg.ItemsLoadFailed(RuntimeException("boom")))

        assertEquals(false, result.state().isLoading)
        assertTrue(result.effects().any { it is UiEffect.Snackbar })
    }

    // ===== 4.2 Create dialog =====

    @Test
    fun `OpenCreateDialog opens with scope preselect to current dictionary`() {
        val result = reducer.testReduce(baseState(), Msg.OpenCreateDialog)

        val dlg = result.state().createDialog
        assertNotNull(dlg)
        assertEquals(1L, dlg?.epochId)
        // preselect: текущий словарь
        assertEquals(Scope.PerDictionaries(listOf(DICT_ID)), dlg?.scope)
        result.assertNoEffects()
    }

    @Test
    fun `OpenCreateDialog when already open - overwrite reset and bumps epoch (F106)`() {
        val initial = stateWithCreateDialog(
            epochId = 5L,
            name = "Dirty",
            nameError = NameError.SameScopeCollision,
            isMultiple = true,
        )

        val result = reducer.testReduce(initial, Msg.OpenCreateDialog)

        val dlg = result.state().createDialog
        assertEquals(6L, dlg?.epochId)
        assertEquals("", dlg?.name)
        assertEquals(false, dlg?.isMultiple)
        assertNull(dlg?.nameError)
        // preselect не теряется на reset
        assertEquals(Scope.PerDictionaries(listOf(DICT_ID)), dlg?.scope)
    }

    @Test
    fun `OpenCreateDialog closes other dialogs (F138)`() {
        val initial = baseState(items = listOf(row(id = 1L))).copy(
            deleteConfirm = DeleteConfirmState(epochId = 4L, typeId = ComponentTypeId(1L), name = "X"),
            nextEpoch = 4L,
            isDeleting = true,
        )

        val result = reducer.testReduce(initial, Msg.OpenCreateDialog)

        assertNotNull(result.state().createDialog)
        assertNull(result.state().deleteConfirm)
        assertEquals(false, result.state().isDeleting)
        assertEquals(false, result.state().isCreating)
    }

    @Test
    fun `CloseCreateDialog clears dialog`() {
        val result = reducer.testReduce(
            stateWithCreateDialog(name = "X"),
            Msg.CloseCreateDialog,
        )

        assertNull(result.state().createDialog)
    }

    @Test
    fun `CreateNameChange updates name and clears nameError`() {
        val initial = stateWithCreateDialog(name = "", nameError = NameError.Empty)

        val result = reducer.testReduce(initial, Msg.CreateNameChange("X"))

        assertEquals("X", result.state().createDialog?.name)
        assertNull(result.state().createDialog?.nameError)
    }

    @Test
    fun `CreateTemplateChange updates template`() {
        val result = reducer.testReduce(
            stateWithCreateDialog(),
            Msg.CreateTemplateChange(ComponentTemplate.IMAGE),
        )

        assertEquals(ComponentTemplate.IMAGE, result.state().createDialog?.template)
    }

    @Test
    fun `CreateMultiToggle updates isMultiple`() {
        val result = reducer.testReduce(stateWithCreateDialog(), Msg.CreateMultiToggle(true))

        assertEquals(true, result.state().createDialog?.isMultiple)
    }

    @Test
    fun `CreateScopeChange updates scope`() {
        val newScope = Scope.Global
        val result = reducer.testReduce(stateWithCreateDialog(), Msg.CreateScopeChange(newScope))

        assertEquals(newScope, result.state().createDialog?.scope)
    }

    @Test
    fun `SubmitCreate happy path - dispatches effect with epochId`() {
        val initial = stateWithCreateDialog(epochId = 7L, name = "Notes")

        val result = reducer.testReduce(initial, Msg.SubmitCreate)

        assertEquals(true, result.state().isCreating)
        assertEquals(
            setOf(
                DatasourceEffect.CreateComponent(
                    epochId = 7L,
                    name = "Notes",
                    template = ComponentTemplate.TEXT,
                    isMultiple = false,
                    scope = Scope.PerDictionaries(listOf(DICT_ID)),
                ),
            ),
            result.effects(),
        )
    }

    @Test
    fun `SubmitCreate blank name - NameError Empty`() {
        val initial = stateWithCreateDialog(name = "")

        val result = reducer.testReduce(initial, Msg.SubmitCreate)

        assertEquals(NameError.Empty, result.state().createDialog?.nameError)
        assertEquals(false, result.state().isCreating)
        result.assertNoEffects()
    }

    @Test
    fun `SubmitCreate guard when isCreating true - no double-effect`() {
        val initial = stateWithCreateDialog(name = "Notes", isCreating = true)

        val result = reducer.testReduce(initial, Msg.SubmitCreate)

        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `CreateResult Success - clear dialog, snackbar`() {
        val initial = stateWithCreateDialog(epochId = 2L, name = "Notes", isCreating = true)

        val result = reducer.testReduce(
            initial,
            Msg.CreateResult(
                epochId = 2L,
                outcome = CreateOutcome.Success(listOf(domainType(1L), domainType(2L))),
            ),
        )

        assertEquals(false, result.state().isCreating)
        assertNull(result.state().createDialog)
        assertEquals(setOf(UiEffect.Snackbar("Created 2")), result.effects())
    }

    @Test
    fun `CreateResult NameEmpty sets nameError`() {
        val initial = stateWithCreateDialog(epochId = 2L, name = "", isCreating = true)

        val result = reducer.testReduce(
            initial,
            Msg.CreateResult(epochId = 2L, outcome = CreateOutcome.NameEmpty),
        )

        assertEquals(NameError.Empty, result.state().createDialog?.nameError)
        assertEquals(false, result.state().isCreating)
    }

    @Test
    fun `CreateResult SameScopeCollision sets nameError`() {
        val initial = stateWithCreateDialog(epochId = 2L, name = "Notes", isCreating = true)

        val result = reducer.testReduce(
            initial,
            Msg.CreateResult(epochId = 2L, outcome = CreateOutcome.SameScopeCollision),
        )

        assertEquals(NameError.SameScopeCollision, result.state().createDialog?.nameError)
    }

    @Test
    fun `CreateResult CrossScopeCollision sets nameError`() {
        val initial = stateWithCreateDialog(epochId = 2L, name = "Notes", isCreating = true)

        val result = reducer.testReduce(
            initial,
            Msg.CreateResult(epochId = 2L, outcome = CreateOutcome.CrossScopeCollision),
        )

        assertEquals(NameError.CrossScopeCollision, result.state().createDialog?.nameError)
    }

    @Test
    fun `CreateResult Failure emits snackbar`() {
        val initial = stateWithCreateDialog(epochId = 2L, name = "Notes", isCreating = true)

        val result = reducer.testReduce(
            initial,
            Msg.CreateResult(epochId = 2L, outcome = CreateOutcome.Failure(RuntimeException("boom"))),
        )

        assertEquals(false, result.state().isCreating)
        assertTrue(result.effects().any { it is UiEffect.Snackbar })
    }

    @Test
    fun `CreateResult race condition - dialog closed during flight (F101)`() {
        val initial = baseState().copy(createDialog = null, isCreating = true)

        val result = reducer.testReduce(
            initial,
            Msg.CreateResult(epochId = 0L, outcome = CreateOutcome.SameScopeCollision),
        )

        assertEquals(false, result.state().isCreating)
        assertNull(result.state().createDialog)
        assertTrue(result.effects().any { it is UiEffect.Snackbar })
    }

    @Test
    fun `CreateResult with stale epochId - discarded (F136)`() {
        val initial = stateWithCreateDialog(epochId = 5L, name = "Notes", isCreating = true)

        val result = reducer.testReduce(
            initial,
            Msg.CreateResult(epochId = 2L, outcome = CreateOutcome.SameScopeCollision),
        )

        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    // ===== 4.4 Delete confirm =====

    @Test
    fun `OpenDeleteConfirm row found - opens dialog + dispatches LoadImpact`() {
        val initial = stateWithRows(row(id = 1L, name = "Notes"))

        val result = reducer.testReduce(initial, Msg.OpenDeleteConfirm(ComponentTypeId(1L)))

        val dlg = result.state().deleteConfirm
        assertEquals(ComponentTypeId(1L), dlg?.typeId)
        assertEquals(true, dlg?.isLoadingImpact)
        assertEquals(1L, dlg?.epochId)
        assertEquals(setOf(DatasourceEffect.LoadImpact(ComponentTypeId(1L))), result.effects())
    }

    @Test
    fun `OpenDeleteConfirm closes other dialogs (F138)`() {
        val initial = baseState(items = listOf(row(id = 1L, name = "Notes"))).copy(
            createDialog = CreateDialogState(epochId = 2L, scope = Scope.PerDictionaries(listOf(DICT_ID))),
            nextEpoch = 3L,
        )

        val result = reducer.testReduce(initial, Msg.OpenDeleteConfirm(ComponentTypeId(1L)))

        assertNotNull(result.state().deleteConfirm)
        assertNull(result.state().createDialog)
    }

    @Test
    fun `OpenDeleteConfirm same typeId twice - no re-trigger`() {
        val initial = stateWithDeleteConfirm(epochId = 3L, typeId = 1L, isLoadingImpact = true)

        val result = reducer.testReduce(initial, Msg.OpenDeleteConfirm(ComponentTypeId(1L)))

        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `OpenDeleteConfirm row missing - no-op`() {
        val initial = stateWithRows()

        val result = reducer.testReduce(initial, Msg.OpenDeleteConfirm(ComponentTypeId(99L)))

        assertNull(result.state().deleteConfirm)
        result.assertNoEffects()
    }

    @Test
    fun `CloseDeleteConfirm clears dialog`() {
        val result = reducer.testReduce(stateWithDeleteConfirm(), Msg.CloseDeleteConfirm)
        assertNull(result.state().deleteConfirm)
    }

    @Test
    fun `ImpactPreviewLoaded matching typeId fills impact`() {
        val initial = stateWithDeleteConfirm(typeId = 1L, isLoadingImpact = true)
        val impact = emptyImpact()

        val result = reducer.testReduce(
            initial,
            Msg.ImpactPreviewLoaded(typeId = ComponentTypeId(1L), impact = impact),
        )

        assertEquals(impact, result.state().deleteConfirm?.impact)
        assertEquals(false, result.state().deleteConfirm?.isLoadingImpact)
    }

    @Test
    fun `ImpactPreviewLoaded with stale typeId - discarded (F124)`() {
        val initial = stateWithDeleteConfirm(typeId = 1L, isLoadingImpact = true)

        val result = reducer.testReduce(
            initial,
            Msg.ImpactPreviewLoaded(typeId = ComponentTypeId(99L), impact = emptyImpact()),
        )

        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `ImpactPreviewFailed clears isLoadingImpact and emits snackbar`() {
        val initial = stateWithDeleteConfirm(typeId = 1L, isLoadingImpact = true)

        val result = reducer.testReduce(
            initial,
            Msg.ImpactPreviewFailed(typeId = ComponentTypeId(1L), cause = RuntimeException("boom")),
        )

        assertEquals(false, result.state().deleteConfirm?.isLoadingImpact)
        assertTrue(result.effects().any { it is UiEffect.Snackbar })
    }

    @Test
    fun `ImpactPreviewFailed with stale typeId - discarded (F124)`() {
        val initial = stateWithDeleteConfirm(typeId = 1L, isLoadingImpact = true)

        val result = reducer.testReduce(
            initial,
            Msg.ImpactPreviewFailed(typeId = ComponentTypeId(99L), cause = RuntimeException("boom")),
        )

        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    // F144: ImpactPreviewFailed когда deleteConfirm == null (user закрыл dialog
    // in-flight) — silent, без snackbar.
    @Test
    fun `ImpactPreviewFailed with closed deleteConfirm - silent no snackbar (F144)`() {
        val initial = baseState()

        val result = reducer.testReduce(
            initial,
            Msg.ImpactPreviewFailed(typeId = ComponentTypeId(1L), cause = RuntimeException("boom")),
        )

        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `ConfirmDelete happy path - dispatches SoftDelete with epochId`() {
        val initial = stateWithDeleteConfirm(epochId = 5L, impact = emptyImpact(), isLoadingImpact = false)

        val result = reducer.testReduce(initial, Msg.ConfirmDelete)

        assertEquals(true, result.state().isDeleting)
        assertEquals(
            setOf(DatasourceEffect.SoftDeleteComponent(epochId = 5L, typeId = ComponentTypeId(1L))),
            result.effects(),
        )
    }

    @Test
    fun `ConfirmDelete guard when isDeleting true - no double-effect`() {
        val initial = stateWithDeleteConfirm(impact = emptyImpact(), isDeleting = true)

        val result = reducer.testReduce(initial, Msg.ConfirmDelete)

        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `ConfirmDelete guard when isLoadingImpact (F102) - no effect`() {
        val initial = stateWithDeleteConfirm(isLoadingImpact = true)

        val result = reducer.testReduce(initial, Msg.ConfirmDelete)

        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `DeleteResult Success closes dialog + snackbar with valueCount`() {
        val initial = stateWithDeleteConfirm(epochId = 5L, impact = emptyImpact(), isDeleting = true)
        val impact = DeletionImpact(
            valueCount = 23,
            dictionariesWithValues = emptyList(),
            affectedQuizConfigs = emptyList(),
            affectedPrefs = emptyList(),
        )

        val result = reducer.testReduce(
            initial,
            Msg.DeleteResult(epochId = 5L, outcome = DeleteOutcome.Success(impact)),
        )

        assertEquals(false, result.state().isDeleting)
        assertNull(result.state().deleteConfirm)
        val snackbar = result.effects().filterIsInstance<UiEffect.Snackbar>().firstOrNull()
        assertTrue("snackbar should mention 23", snackbar?.text?.contains("23") == true)
    }

    @Test
    fun `DeleteResult BuiltInProtected closes dialog + snackbar`() {
        val initial = stateWithDeleteConfirm(epochId = 5L, isDeleting = true)

        val result = reducer.testReduce(
            initial,
            Msg.DeleteResult(epochId = 5L, outcome = DeleteOutcome.BuiltInProtected),
        )

        assertEquals(false, result.state().isDeleting)
        assertNull(result.state().deleteConfirm)
        assertTrue(result.effects().any { it is UiEffect.Snackbar })
    }

    @Test
    fun `DeleteResult Failure clears isDeleting + snackbar`() {
        val initial = stateWithDeleteConfirm(epochId = 5L, isDeleting = true)

        val result = reducer.testReduce(
            initial,
            Msg.DeleteResult(epochId = 5L, outcome = DeleteOutcome.Failure(RuntimeException("boom"))),
        )

        assertEquals(false, result.state().isDeleting)
        assertTrue(result.effects().any { it is UiEffect.Snackbar })
    }

    @Test
    fun `DeleteResult race condition - dialog closed during flight (F101)`() {
        val initial = baseState().copy(deleteConfirm = null, isDeleting = true)

        val result = reducer.testReduce(
            initial,
            Msg.DeleteResult(epochId = 0L, outcome = DeleteOutcome.Failure(RuntimeException("boom"))),
        )

        assertEquals(false, result.state().isDeleting)
        assertNull(result.state().deleteConfirm)
        assertTrue(result.effects().any { it is UiEffect.Snackbar })
    }

    @Test
    fun `DeleteResult with stale epochId - discarded (F136)`() {
        val initial = stateWithDeleteConfirm(epochId = 7L, isDeleting = true)

        val result = reducer.testReduce(
            initial,
            Msg.DeleteResult(epochId = 2L, outcome = DeleteOutcome.Failure(RuntimeException("boom"))),
        )

        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    // ===== 4.5 Snackbar state (F123) =====

    @Test
    fun `UiMsg Snackbar writes snackbarState into state (F123)`() {
        val initial = baseState()

        val result = reducer.testReduce(initial, UiMsg.Snackbar("hello"))

        assertEquals(SnackbarState("hello"), result.state().snackbarState)
        result.assertNoEffects()
    }

    @Test
    fun `DismissSnackbar clears snackbarState`() {
        val initial = baseState().copy(snackbarState = SnackbarState("old"))

        val result = reducer.testReduce(initial, Msg.DismissSnackbar)

        assertNull(result.state().snackbarState)
    }

    // ===== 4.6 Navigation / no-op =====

    @Test
    fun `RequestBack emits NavigationEffect Back`() {
        val initial = baseState()

        val result = reducer.testReduce(initial, Msg.RequestBack)

        assertEquals(initial, result.state())
        assertTrue(result.effects().contains(NavigationEffect.Back))
    }

    @Test
    fun `Empty Msg - no change, no effects`() {
        val initial = baseState()

        val result = reducer.testReduce(initial, Msg.Empty)

        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    // ===== 4.7 toPerDictRows() helper =====

    @Test
    fun `toPerDictRows - empty types yields empty rows`() {
        val snapshot = PerDictionarySnapshot(
            dictionaryId = DICT_ID,
            dictionaryName = "Spanish",
            types = emptyList(),
            valueCountByType = emptyMap(),
        )

        assertTrue(snapshot.toPerDictRows().isEmpty())
    }

    @Test
    fun `toPerDictRows - isGlobal inference`() {
        val snapshot = PerDictionarySnapshot(
            dictionaryId = DICT_ID,
            dictionaryName = "Spanish",
            types = listOf(
                domainType(1L, "Global", dictionaryId = null),
                domainType(2L, "Local", dictionaryId = DICT_ID),
            ),
            valueCountByType = emptyMap(),
        )

        val rows = snapshot.toPerDictRows()

        assertEquals(true, rows[0].isGlobal)
        assertEquals(false, rows[1].isGlobal)
    }

    @Test
    fun `toPerDictRows - valueCount lookup with fallback 0`() {
        val snapshot = PerDictionarySnapshot(
            dictionaryId = DICT_ID,
            dictionaryName = "Spanish",
            types = listOf(
                domainType(1L, "Has"),
                domainType(2L, "Empty"),
            ),
            valueCountByType = mapOf(ComponentTypeId(1L) to 7),
        )

        val rows = snapshot.toPerDictRows()

        assertEquals(7, rows[0].valueCount)
        assertEquals(0, rows[1].valueCount)
    }

    // =========================================================================
    // ===== PHASE 2 ============================================================
    // =========================================================================

    // ===== P2.1 Edit dialog — Open / Close / changes =====

    @Test
    fun `whenOpenEditDialog_thenEditDialogStateOpened_andOtherDialogsReset`() {
        val initial = baseState(items = listOf(row(id = 1L, name = "Notes", isMultiple = true))).copy(
            createDialog = CreateDialogState(epochId = 1L, scope = Scope.PerDictionaries(listOf(DICT_ID))),
            deleteConfirm = DeleteConfirmState(epochId = 3L, typeId = ComponentTypeId(1L), name = "Notes"),
            isCreating = true,
            isDeleting = true,
            nextEpoch = 3L,
        )

        val result = reducer.testReduce(initial, Msg.OpenEditDialog(ComponentTypeId(1L)))

        val dlg = result.state().editDialog
        assertNotNull(dlg)
        assertEquals(ComponentTypeId(1L), dlg?.typeId)
        assertEquals("Notes", dlg?.originalName)
        assertEquals(true, dlg?.originalIsMultiple)
        // F138 mutual-exclusion
        assertNull(result.state().createDialog)
        assertNull(result.state().deleteConfirm)
        // F140 in-flight reset
        assertEquals(false, result.state().isCreating)
        assertEquals(false, result.state().isDeleting)
        assertEquals(false, result.state().isEditing)
    }

    @Test
    fun `whenOpenCreateDialog_closesEditDialog_mutualExclusion`() {
        val initial = stateWithEditDialog(epochId = 5L, isEditing = true)

        val result = reducer.testReduce(initial, Msg.OpenCreateDialog)

        assertNotNull(result.state().createDialog)
        assertNull(result.state().editDialog)
        assertEquals(false, result.state().isEditing)
    }

    @Test
    fun `whenOpenDeleteConfirm_closesEditDialog_mutualExclusion`() {
        val initial = stateWithEditDialog(epochId = 5L, isEditing = true)

        val result = reducer.testReduce(initial, Msg.OpenDeleteConfirm(ComponentTypeId(1L)))

        assertNotNull(result.state().deleteConfirm)
        assertNull(result.state().editDialog)
        assertEquals(false, result.state().isEditing)
    }

    @Test
    fun `whenCloseEditDialog_clearsEditDialogState`() {
        val initial = stateWithEditDialog(name = "Modified")

        val result = reducer.testReduce(initial, Msg.CloseEditDialog)

        assertNull(result.state().editDialog)
        assertEquals(false, result.state().isEditing)
    }

    @Test
    fun `whenEditNameChange_updatesEditDialogStateName`() {
        val initial = stateWithEditDialog(nameError = EditNameError.NameEmpty)

        val result = reducer.testReduce(initial, Msg.EditNameChange("Updated"))

        assertEquals("Updated", result.state().editDialog?.name)
        assertNull(result.state().editDialog?.nameError)
    }

    @Test
    fun `whenEditTemplateChange_updatesEditDialogStateTemplate`() {
        val initial = stateWithEditDialog()

        val result = reducer.testReduce(initial, Msg.EditTemplateChange(ComponentTemplate.IMAGE))

        assertEquals(ComponentTemplate.IMAGE, result.state().editDialog?.template)
    }

    @Test
    fun `whenEditMultiToggle_updatesEditDialogStateIsMulti_andClearsPreview`() {
        val initial = stateWithEditDialog(
            isMultiple = true,
            impactedLexemesPreview = ImpactedLexemesPreview.InlineOnly(listOf(1L, 2L)),
        )

        val result = reducer.testReduce(initial, Msg.EditMultiToggle(false))

        assertEquals(false, result.state().editDialog?.isMultiple)
        assertNull(result.state().editDialog?.impactedLexemesPreview)
    }

    // ===== P2.2 SubmitEdit =====

    @Test
    fun `whenSubmitEdit_emitsDatasourceEffectEditComponent_andSetsIsEditing`() {
        val initial = stateWithEditDialog(
            epochId = 7L,
            typeId = 42L,
            name = "Updated",
            template = ComponentTemplate.TEXT,
            isMultiple = true,
        )

        val result = reducer.testReduce(initial, Msg.SubmitEdit)

        assertEquals(true, result.state().isEditing)
        assertEquals(
            setOf(
                DatasourceEffect.EditComponent(
                    epochId = 7L,
                    typeId = ComponentTypeId(42L),
                    name = "Updated",
                    template = ComponentTemplate.TEXT,
                    isMultiple = true,
                ),
            ),
            result.effects(),
        )
    }

    @Test
    fun `whenSubmitEdit_whileIsEditing_noDoubleEffect_doubleTapGuard`() {
        val initial = stateWithEditDialog(name = "Updated", isEditing = true)

        val result = reducer.testReduce(initial, Msg.SubmitEdit)

        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `whenSubmitEdit_blankName_setsNameEmptyError`() {
        val initial = stateWithEditDialog(name = "   ")

        val result = reducer.testReduce(initial, Msg.SubmitEdit)

        assertEquals(EditNameError.NameEmpty, result.state().editDialog?.nameError)
        assertEquals(false, result.state().isEditing)
        result.assertNoEffects()
    }

    // ===== P2.3 EditResult outcomes =====

    @Test
    fun `whenEditResultSuccess_thenDialogClosedAndSnackbarEmitted`() {
        val initial = stateWithEditDialog(epochId = 3L, isEditing = true)

        val result = reducer.testReduce(
            initial,
            Msg.EditResult(
                epochId = 3L,
                outcome = EditOutcome.Success(domainType(1L, "Updated")),
            ),
        )

        assertEquals(false, result.state().isEditing)
        assertNull(result.state().editDialog)
        assertTrue(result.effects().any { it is UiEffect.Snackbar })
    }

    @Test
    fun `whenEditResultNameEmpty_thenInlineErrorShown`() {
        val initial = stateWithEditDialog(epochId = 3L, isEditing = true)

        val result = reducer.testReduce(
            initial,
            Msg.EditResult(epochId = 3L, outcome = EditOutcome.NameEmpty),
        )

        assertEquals(false, result.state().isEditing)
        assertNotNull(result.state().editDialog)
        assertEquals(EditNameError.NameEmpty, result.state().editDialog?.nameError)
        result.assertNoEffects()
    }

    @Test
    fun `whenEditResultSameScopeCollision_thenInlineErrorShown`() {
        val initial = stateWithEditDialog(epochId = 3L, isEditing = true)

        val result = reducer.testReduce(
            initial,
            Msg.EditResult(epochId = 3L, outcome = EditOutcome.SameScopeCollision),
        )

        assertEquals(false, result.state().isEditing)
        assertNotNull(result.state().editDialog)
        assertEquals(EditNameError.SameScopeCollision, result.state().editDialog?.nameError)
    }

    @Test
    fun `whenEditResultCrossScopeCollision_thenInlineErrorShown`() {
        val initial = stateWithEditDialog(epochId = 3L, isEditing = true)

        val result = reducer.testReduce(
            initial,
            Msg.EditResult(epochId = 3L, outcome = EditOutcome.CrossScopeCollision),
        )

        assertEquals(false, result.state().isEditing)
        assertNotNull(result.state().editDialog)
        assertEquals(EditNameError.CrossScopeCollision, result.state().editDialog?.nameError)
    }

    @Test
    fun `whenEditResultCardinalityDowngradeBlocked_sizeLessOrEqualThree_thenInlineOnlyPreview_andDrillInHidden`() {
        val initial = stateWithEditDialog(epochId = 3L, isMultiple = true, isEditing = true)

        val result = reducer.testReduce(
            initial,
            Msg.EditResult(
                epochId = 3L,
                outcome = EditOutcome.CardinalityDowngradeBlocked(impactedLexemeIds = listOf(10L, 20L)),
            ),
        )

        assertEquals(false, result.state().isEditing)
        assertNotNull(result.state().editDialog)
        val preview = result.state().editDialog?.impactedLexemesPreview
        assertTrue("expected InlineOnly", preview is ImpactedLexemesPreview.InlineOnly)
        assertEquals(listOf(10L, 20L), preview?.impactedLexemeIds)
    }

    @Test
    fun `whenEditResultCardinalityDowngradeBlocked_sizeMoreThanThree_thenTop3Inline_andDrillInVisible`() {
        val initial = stateWithEditDialog(epochId = 3L, isMultiple = true, isEditing = true)

        val ids = listOf(10L, 20L, 30L, 40L, 50L, 60L, 70L)
        val result = reducer.testReduce(
            initial,
            Msg.EditResult(
                epochId = 3L,
                outcome = EditOutcome.CardinalityDowngradeBlocked(impactedLexemeIds = ids),
            ),
        )

        val preview = result.state().editDialog?.impactedLexemesPreview
        assertTrue("expected InlineWithDrillIn", preview is ImpactedLexemesPreview.InlineWithDrillIn)
        val drillIn = preview as? ImpactedLexemesPreview.InlineWithDrillIn
        assertEquals(ids, drillIn?.impactedLexemeIds)
        assertEquals(listOf(10L, 20L, 30L), drillIn?.inlineIds)
    }

    @Test
    fun `whenEditResultTemplateImmutable_thenDialogClosed_andSnackbarEmitted`() {
        val initial = stateWithEditDialog(epochId = 3L, template = ComponentTemplate.IMAGE, isEditing = true)

        val result = reducer.testReduce(
            initial,
            Msg.EditResult(epochId = 3L, outcome = EditOutcome.TemplateImmutable),
        )

        assertEquals(false, result.state().isEditing)
        assertNull(result.state().editDialog)
        assertTrue(result.effects().any { it is UiEffect.Snackbar })
    }

    @Test
    fun `whenEditResultBuiltInProtected_thenSnackbarEmitted_andDialogClosed`() {
        val initial = stateWithEditDialog(epochId = 3L, isEditing = true)

        val result = reducer.testReduce(
            initial,
            Msg.EditResult(epochId = 3L, outcome = EditOutcome.BuiltInProtected),
        )

        assertEquals(false, result.state().isEditing)
        assertNull(result.state().editDialog)
        assertTrue(result.effects().any { it is UiEffect.Snackbar })
    }

    @Test
    fun `whenEditResultRemoved_thenSnackbar_andDialogClosed_andListRefreshed`() {
        val initial = stateWithEditDialog(epochId = 3L, isEditing = true)

        val result = reducer.testReduce(
            initial,
            Msg.EditResult(epochId = 3L, outcome = EditOutcome.Removed),
        )

        assertEquals(false, result.state().isEditing)
        assertNull(result.state().editDialog)
        val snackbar = result.effects().filterIsInstance<UiEffect.Snackbar>().firstOrNull()
        assertNotNull("Removed should emit snackbar", snackbar)
    }

    @Test
    fun `whenEditResultFailure_thenSnackbar_andDialogClosed`() {
        val initial = stateWithEditDialog(epochId = 3L, isEditing = true)

        val result = reducer.testReduce(
            initial,
            Msg.EditResult(epochId = 3L, outcome = EditOutcome.Failure(RuntimeException("boom"))),
        )

        assertEquals(false, result.state().isEditing)
        assertNull(result.state().editDialog)
        assertTrue(result.effects().any { it is UiEffect.Snackbar })
    }

    @Test
    fun `whenStaleEpochEditResult_thenIgnored_F136`() {
        val initial = stateWithEditDialog(epochId = 7L, isEditing = true)

        val result = reducer.testReduce(
            initial,
            Msg.EditResult(epochId = 2L, outcome = EditOutcome.Success(domainType(1L, "Updated"))),
        )

        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `whenEditResultArrivesAfterCloseDialog_thenSnackbarFallback_F101`() {
        val initial = baseState().copy(editDialog = null, isEditing = true)

        val result = reducer.testReduce(
            initial,
            Msg.EditResult(epochId = 0L, outcome = EditOutcome.Success(domainType(1L, "Updated"))),
        )

        assertEquals(false, result.state().isEditing)
        assertNull(result.state().editDialog)
        assertTrue(
            "fallback snackbar on race",
            result.effects().any { it is UiEffect.Snackbar },
        )
    }

    // ===== P2.4 Delete Removed parity =====

    @Test
    fun `whenDeleteResultRemoved_thenDialogClosed_andRemovedSnackbar`() {
        val initial = stateWithDeleteConfirm(epochId = 5L, isDeleting = true)

        val result = reducer.testReduce(
            initial,
            Msg.DeleteResult(epochId = 5L, outcome = DeleteOutcome.Removed),
        )

        assertEquals(false, result.state().isDeleting)
        assertNull(result.state().deleteConfirm)
        assertTrue(result.effects().any { it is UiEffect.Snackbar })
    }
}
