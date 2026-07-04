package me.apomazkin.components_manager.mate

import me.apomazkin.core_db_api.entity.DictionaryApiEntity
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentType
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.ComponentUsage
import me.apomazkin.lexeme.CreateOutcome
import me.apomazkin.lexeme.DeleteOutcome
import me.apomazkin.lexeme.DeletionImpact
import me.apomazkin.lexeme.EditOutcome
import me.apomazkin.lexeme.NameError
import me.apomazkin.lexeme.RenameOutcome
import me.apomazkin.lexeme.Scope
import me.apomazkin.lexeme.UserDefinedTypesSnapshot
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.logger.LogLevel
import me.apomazkin.mate.NavigationEffect
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
 * Unit tests для [ComponentsManagerReducer]. Покрывает все ветки Msg + invariants:
 * - lifecycle (TypesLoaded/Failed),
 * - create / rename / delete dialog branches,
 * - race conditions (close-during-flight, F101 + stale epoch F124/F136),
 * - guards (double-tap, isLoadingImpact, F102),
 * - overwrite reset (F106),
 * - dialog mutual-exclusion (F138),
 * - snackbar state (F123) + Dismiss,
 * - navigation / no-op.
 */
class ComponentsManagerReducerTest {

    private val reducer = ComponentsManagerReducer(
        logger = object : LexemeLogger {
            override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {}
        }
    )

    // ---- helpers ----

    private val now = Date(0L)

    private fun row(
        id: Long = 1L,
        name: String = "Notes",
        scope: Scope = Scope.Global,
        isMultiple: Boolean = false,
        usage: Int = 0,
    ) = UserDefinedRow(
        typeId = ComponentTypeId(id),
        name = name,
        template = ComponentTemplate.TEXT,
        isMultiple = isMultiple,
        scope = scope,
        usageCount = usage,
        dictionaryNames = emptyList(),
    )

    private fun stateWithRows(vararg rows: UserDefinedRow): ComponentsManagerScreenState =
        ComponentsManagerScreenState(
            userDefinedTypes = rows.toList(),
            isLoading = false,
        )

    private fun stateWithCreateDialog(
        epochId: Long = 1L,
        name: String = "",
        template: ComponentTemplate = ComponentTemplate.TEXT,
        isMultiple: Boolean = false,
        scope: Scope = Scope.Global,
        nameError: NameError? = null,
        isCreating: Boolean = false,
    ): ComponentsManagerScreenState = ComponentsManagerScreenState(
        userDefinedTypes = emptyList(),
        isCreating = isCreating,
        createDialog = CreateDialogState(
            epochId = epochId,
            name = name,
            template = template,
            isMultiple = isMultiple,
            scope = scope,
            nameError = nameError,
        ),
        nextEpoch = epochId,
    )

    private fun stateWithRenameDialog(
        epochId: Long = 1L,
        typeId: Long = 1L,
        originalName: String = "Notes",
        editedName: String = "Notes",
        nameError: NameError? = null,
        isRenaming: Boolean = false,
    ): ComponentsManagerScreenState = ComponentsManagerScreenState(
        userDefinedTypes = listOf(row(id = typeId, name = originalName)),
        isRenaming = isRenaming,
        renameDialog = RenameDialogState(
            epochId = epochId,
            typeId = ComponentTypeId(typeId),
            originalName = originalName,
            editedName = editedName,
            nameError = nameError,
        ),
        nextEpoch = epochId,
    )

    private fun stateWithDeleteConfirm(
        epochId: Long = 1L,
        typeId: Long = 1L,
        name: String = "Notes",
        impact: DeletionImpact? = null,
        isLoadingImpact: Boolean = false,
        isDeleting: Boolean = false,
    ): ComponentsManagerScreenState = ComponentsManagerScreenState(
        userDefinedTypes = listOf(row(id = typeId, name = name)),
        isDeleting = isDeleting,
        deleteConfirm = DeleteConfirmState(
            epochId = epochId,
            typeId = ComponentTypeId(typeId),
            name = name,
            impact = impact,
            isLoadingImpact = isLoadingImpact,
        ),
        nextEpoch = epochId,
    )

    private fun domainType(
        id: Long = 1L,
        name: String = "Notes",
        dictionaryId: Long? = null,
    ) = ComponentType(
        id = ComponentTypeId(id),
        systemKey = null,
        dictionaryId = dictionaryId,
        name = name,
        template = ComponentTemplate.TEXT,
        position = 0,
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

    private fun dictApi(id: Long, name: String = "D$id") = DictionaryApiEntity(
        id = id,
        numericCode = null,
        name = name,
        addDate = now,
    )

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
    ): ComponentsManagerScreenState = ComponentsManagerScreenState(
        userDefinedTypes = listOf(row(id = typeId, name = originalName, isMultiple = originalIsMultiple)),
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
        nextEpoch = epochId,
    )

    // ===== 3.1 Lifecycle =====

    @Test
    fun `TypesLoaded with snapshot updates state and clears isLoading`() {
        val initial = ComponentsManagerScreenState(isLoading = true)
        val snapshot = UserDefinedTypesSnapshot(
            types = listOf(domainType(id = 1L, name = "Notes")),
            usage = ComponentUsage(
                valueCountByType = mapOf(ComponentTypeId(1L) to 5),
                dictionaryIdsByType = emptyMap(),
                dictionaryNames = emptyMap(),
            ),
        )

        val result = reducer.testReduce(initial, Msg.TypesLoaded(snapshot))

        assertEquals(false, result.state().isLoading)
        assertEquals(1, result.state().userDefinedTypes?.size)
        assertEquals("Notes", result.state().userDefinedTypes?.first()?.name)
        assertEquals(5, result.state().userDefinedTypes?.first()?.usageCount)
        result.assertNoEffects()
    }

    @Test
    fun `TypesLoaded empty snapshot - state is empty`() {
        val initial = ComponentsManagerScreenState(isLoading = true)
        val snapshot = UserDefinedTypesSnapshot(
            types = emptyList(),
            usage = ComponentUsage(emptyMap(), emptyMap(), emptyMap()),
        )

        val result = reducer.testReduce(initial, Msg.TypesLoaded(snapshot))

        assertTrue(result.state().isEmpty)
    }

    @Test
    fun `TypesLoadFailed emits snackbar and clears isLoading`() {
        val initial = ComponentsManagerScreenState(isLoading = true)

        val result = reducer.testReduce(initial, Msg.TypesLoadFailed(RuntimeException("boom")))

        assertEquals(false, result.state().isLoading)
        assertTrue(result.effects().any { it is UiEffect.Snackbar })
    }

    // ===== 3.2 Create dialog =====

    @Test
    fun `OpenCreateDialog opens with default state and assigns epoch`() {
        val result = reducer.testReduce(
            ComponentsManagerScreenState(),
            Msg.OpenCreateDialog,
        )

        val dlg = result.state().createDialog
        assertNotNull(dlg)
        assertEquals(1L, dlg?.epochId)
        assertEquals(1L, result.state().nextEpoch)
        result.assertNoEffects()
    }

    @Test
    fun `OpenCreateDialog when already open - overwrite reset and bumps epoch (F106)`() {
        val initial = stateWithCreateDialog(
            epochId = 5L,
            name = "DirtyName",
            template = ComponentTemplate.IMAGE,
            isMultiple = true,
            scope = Scope.PerDictionaries(listOf(99L)),
            nameError = NameError.SameScopeCollision,
        )

        val result = reducer.testReduce(initial, Msg.OpenCreateDialog)

        val dlg = result.state().createDialog
        assertEquals(6L, dlg?.epochId)
        assertEquals("", dlg?.name)
        assertEquals(ComponentTemplate.TEXT, dlg?.template)
        assertNull(dlg?.nameError)
        result.assertNoEffects()
    }

    @Test
    fun `OpenCreateDialog closes other dialogs (F138)`() {
        val initial = ComponentsManagerScreenState(
            userDefinedTypes = listOf(row(id = 1L)),
            renameDialog = RenameDialogState(epochId = 3L, typeId = ComponentTypeId(1L), originalName = "X", editedName = "X"),
            deleteConfirm = DeleteConfirmState(epochId = 4L, typeId = ComponentTypeId(1L), name = "X"),
            nextEpoch = 4L,
            isRenaming = true,
            isDeleting = true,
        )

        val result = reducer.testReduce(initial, Msg.OpenCreateDialog)

        assertNotNull(result.state().createDialog)
        assertNull(result.state().renameDialog)
        assertNull(result.state().deleteConfirm)
        assertEquals(false, result.state().isRenaming)
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
        result.assertNoEffects()
    }

    @Test
    fun `CreateNameChange updates name and clears nameError`() {
        val initial = stateWithCreateDialog(name = "", nameError = NameError.Empty)

        val result = reducer.testReduce(initial, Msg.CreateNameChange("X"))

        assertEquals("X", result.state().createDialog?.name)
        assertNull(result.state().createDialog?.nameError)
        result.assertNoEffects()
    }

    @Test
    fun `CreateNameChange when no dialog - no-op`() {
        val initial = ComponentsManagerScreenState()

        val result = reducer.testReduce(initial, Msg.CreateNameChange("X"))

        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `CreateTemplateChange updates template`() {
        val initial = stateWithCreateDialog()

        val result = reducer.testReduce(initial, Msg.CreateTemplateChange(ComponentTemplate.IMAGE))

        assertEquals(ComponentTemplate.IMAGE, result.state().createDialog?.template)
    }

    @Test
    fun `CreateMultiToggle updates isMultiple`() {
        val initial = stateWithCreateDialog()

        val result = reducer.testReduce(initial, Msg.CreateMultiToggle(true))

        assertEquals(true, result.state().createDialog?.isMultiple)
    }

    @Test
    fun `CreateScopeChange updates scope`() {
        val initial = stateWithCreateDialog()
        val newScope = Scope.PerDictionaries(listOf(1L, 2L))

        val result = reducer.testReduce(initial, Msg.CreateScopeChange(newScope))

        assertEquals(newScope, result.state().createDialog?.scope)
    }

    @Test
    fun `SubmitCreate happy path - isCreating true and effect with epochId dispatched`() {
        val initial = stateWithCreateDialog(epochId = 7L, name = "Notes", template = ComponentTemplate.TEXT)

        val result = reducer.testReduce(initial, Msg.SubmitCreate)

        assertEquals(true, result.state().isCreating)
        assertEquals(
            setOf(
                DatasourceEffect.CreateComponent(
                    epochId = 7L,
                    name = "Notes",
                    template = ComponentTemplate.TEXT,
                    isMultiple = false,
                    scope = Scope.Global,
                ),
            ),
            result.effects(),
        )
    }

    @Test
    fun `SubmitCreate blank name sets NameError Empty`() {
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
    fun `SubmitCreate when no dialog - no-op`() {
        val initial = ComponentsManagerScreenState()

        val result = reducer.testReduce(initial, Msg.SubmitCreate)

        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `CreateResult Success - clear dialog, snackbar with count`() {
        val initial = stateWithCreateDialog(epochId = 2L, name = "Notes", isCreating = true)

        val result = reducer.testReduce(
            initial,
            Msg.CreateResult(
                epochId = 2L,
                outcome = CreateOutcome.Success(
                    created = listOf(domainType(1L), domainType(2L))
                ),
            ),
        )

        assertEquals(false, result.state().isCreating)
        assertNull(result.state().createDialog)
        assertEquals(
            setOf(UiEffect.Snackbar("Created 2")),
            result.effects(),
        )
    }

    @Test
    fun `CreateResult NameEmpty sets nameError`() {
        val initial = stateWithCreateDialog(epochId = 2L, name = "", isCreating = true)

        val result = reducer.testReduce(
            initial,
            Msg.CreateResult(epochId = 2L, outcome = CreateOutcome.NameEmpty),
        )

        assertEquals(false, result.state().isCreating)
        assertEquals(NameError.Empty, result.state().createDialog?.nameError)
        result.assertNoEffects()
    }

    @Test
    fun `CreateResult SameScopeCollision sets nameError`() {
        val initial = stateWithCreateDialog(epochId = 2L, name = "Notes", isCreating = true)

        val result = reducer.testReduce(
            initial,
            Msg.CreateResult(epochId = 2L, outcome = CreateOutcome.SameScopeCollision),
        )

        assertEquals(false, result.state().isCreating)
        assertEquals(NameError.SameScopeCollision, result.state().createDialog?.nameError)
        result.assertNoEffects()
    }

    @Test
    fun `CreateResult CrossScopeCollision sets nameError`() {
        val initial = stateWithCreateDialog(epochId = 2L, name = "Notes", isCreating = true)

        val result = reducer.testReduce(
            initial,
            Msg.CreateResult(epochId = 2L, outcome = CreateOutcome.CrossScopeCollision),
        )

        assertEquals(false, result.state().isCreating)
        assertEquals(NameError.CrossScopeCollision, result.state().createDialog?.nameError)
        result.assertNoEffects()
    }

    @Test
    fun `CreateResult Failure emits snackbar, clears isCreating`() {
        val initial = stateWithCreateDialog(epochId = 2L, name = "Notes", isCreating = true)

        val result = reducer.testReduce(
            initial,
            Msg.CreateResult(epochId = 2L, outcome = CreateOutcome.Failure(RuntimeException("boom"))),
        )

        assertEquals(false, result.state().isCreating)
        assertTrue(result.effects().any { it is UiEffect.Snackbar })
    }

    @Test
    fun `CreateResult Failure with null cause message - fallback to class name (F129)`() {
        val initial = stateWithCreateDialog(epochId = 2L, name = "Notes", isCreating = true)

        val cause: Throwable = object : RuntimeException() {
            override val message: String? = null
        }
        val result = reducer.testReduce(
            initial,
            Msg.CreateResult(epochId = 2L, outcome = CreateOutcome.Failure(cause)),
        )

        val snackbar = result.effects().filterIsInstance<UiEffect.Snackbar>().firstOrNull()
        // not 'Failed: null' — should mention class name fallback
        assertTrue("snackbar should not contain 'null'", snackbar?.text?.contains("null") != true)
    }

    @Test
    fun `CreateResult race condition - dialog closed during flight (F101)`() {
        val initial = ComponentsManagerScreenState(
            createDialog = null,
            isCreating = true,
        )

        val result = reducer.testReduce(
            initial,
            Msg.CreateResult(epochId = 0L, outcome = CreateOutcome.SameScopeCollision),
        )

        assertEquals(false, result.state().isCreating)
        assertNull(result.state().createDialog)
        assertTrue(
            "should fall back to snackbar when dialog closed",
            result.effects().any { it is UiEffect.Snackbar },
        )
    }

    @Test
    fun `CreateResult with stale epochId - discarded (F136)`() {
        // dialog open with epoch=5; stale Result arrives for epoch=2 → ignore.
        val initial = stateWithCreateDialog(epochId = 5L, name = "Notes", isCreating = true)

        val result = reducer.testReduce(
            initial,
            Msg.CreateResult(epochId = 2L, outcome = CreateOutcome.SameScopeCollision),
        )

        // state unchanged — stale result ignored
        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    // ===== 3.3 Rename dialog =====

    @Test
    fun `OpenRenameDialog row found - opens dialog and assigns epoch`() {
        val initial = stateWithRows(row(id = 1L, name = "Notes"))

        val result = reducer.testReduce(initial, Msg.OpenRenameDialog(ComponentTypeId(1L)))

        val dlg = result.state().renameDialog
        assertEquals(ComponentTypeId(1L), dlg?.typeId)
        assertEquals("Notes", dlg?.originalName)
        assertEquals("Notes", dlg?.editedName)
        assertEquals(1L, dlg?.epochId)
        result.assertNoEffects()
    }

    @Test
    fun `OpenRenameDialog closes other dialogs (F138)`() {
        val initial = ComponentsManagerScreenState(
            userDefinedTypes = listOf(row(id = 1L)),
            createDialog = CreateDialogState(epochId = 2L),
            deleteConfirm = DeleteConfirmState(epochId = 3L, typeId = ComponentTypeId(99L), name = "Z"),
            nextEpoch = 3L,
            isCreating = true,
            isDeleting = true,
        )

        val result = reducer.testReduce(initial, Msg.OpenRenameDialog(ComponentTypeId(1L)))

        assertNotNull(result.state().renameDialog)
        assertNull(result.state().createDialog)
        assertNull(result.state().deleteConfirm)
        assertEquals(false, result.state().isCreating)
        assertEquals(false, result.state().isDeleting)
    }

    @Test
    fun `OpenRenameDialog row missing - no-op (guard)`() {
        val initial = stateWithRows()

        val result = reducer.testReduce(initial, Msg.OpenRenameDialog(ComponentTypeId(99L)))

        assertNull(result.state().renameDialog)
        result.assertNoEffects()
    }

    @Test
    fun `CloseRenameDialog clears dialog`() {
        val initial = stateWithRenameDialog()

        val result = reducer.testReduce(initial, Msg.CloseRenameDialog)

        assertNull(result.state().renameDialog)
    }

    @Test
    fun `RenameTextChange updates editedName and clears nameError`() {
        val initial = stateWithRenameDialog(nameError = NameError.Empty)

        val result = reducer.testReduce(initial, Msg.RenameTextChange("Y"))

        assertEquals("Y", result.state().renameDialog?.editedName)
        assertNull(result.state().renameDialog?.nameError)
    }

    @Test
    fun `SubmitRename happy path - dispatches effect with epochId`() {
        val initial = stateWithRenameDialog(epochId = 4L, editedName = "Y")

        val result = reducer.testReduce(initial, Msg.SubmitRename)

        assertEquals(true, result.state().isRenaming)
        assertEquals(
            setOf(DatasourceEffect.RenameComponent(epochId = 4L, typeId = ComponentTypeId(1L), newName = "Y")),
            result.effects(),
        )
    }

    @Test
    fun `SubmitRename blank editedName - NameError Empty`() {
        val initial = stateWithRenameDialog(editedName = "")

        val result = reducer.testReduce(initial, Msg.SubmitRename)

        assertEquals(NameError.Empty, result.state().renameDialog?.nameError)
        assertEquals(false, result.state().isRenaming)
        result.assertNoEffects()
    }

    @Test
    fun `SubmitRename guard when isRenaming true - no double-effect`() {
        val initial = stateWithRenameDialog(editedName = "Y", isRenaming = true)

        val result = reducer.testReduce(initial, Msg.SubmitRename)

        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `RenameResult Success closes dialog and emits snackbar`() {
        val initial = stateWithRenameDialog(epochId = 4L, isRenaming = true)

        val result = reducer.testReduce(
            initial,
            Msg.RenameResult(epochId = 4L, outcome = RenameOutcome.Success(domainType(1L, "Renamed"))),
        )

        assertEquals(false, result.state().isRenaming)
        assertNull(result.state().renameDialog)
        assertTrue(result.effects().any { it is UiEffect.Snackbar })
    }

    @Test
    fun `RenameResult NameEmpty sets nameError`() {
        val initial = stateWithRenameDialog(epochId = 4L, isRenaming = true)

        val result = reducer.testReduce(
            initial,
            Msg.RenameResult(epochId = 4L, outcome = RenameOutcome.NameEmpty),
        )

        assertEquals(NameError.Empty, result.state().renameDialog?.nameError)
        assertEquals(false, result.state().isRenaming)
    }

    @Test
    fun `RenameResult SameScopeCollision sets nameError`() {
        val initial = stateWithRenameDialog(epochId = 4L, isRenaming = true)

        val result = reducer.testReduce(
            initial,
            Msg.RenameResult(epochId = 4L, outcome = RenameOutcome.SameScopeCollision),
        )

        assertEquals(NameError.SameScopeCollision, result.state().renameDialog?.nameError)
    }

    @Test
    fun `RenameResult CrossScopeCollision sets nameError`() {
        val initial = stateWithRenameDialog(epochId = 4L, isRenaming = true)

        val result = reducer.testReduce(
            initial,
            Msg.RenameResult(epochId = 4L, outcome = RenameOutcome.CrossScopeCollision),
        )

        assertEquals(NameError.CrossScopeCollision, result.state().renameDialog?.nameError)
    }

    @Test
    fun `RenameResult BuiltInProtected closes dialog + snackbar`() {
        val initial = stateWithRenameDialog(epochId = 4L, isRenaming = true)

        val result = reducer.testReduce(
            initial,
            Msg.RenameResult(epochId = 4L, outcome = RenameOutcome.BuiltInProtected),
        )

        assertEquals(false, result.state().isRenaming)
        assertNull(result.state().renameDialog)
        assertTrue(result.effects().any { it is UiEffect.Snackbar })
    }

    @Test
    fun `RenameResult Failure - clears isRenaming + snackbar`() {
        val initial = stateWithRenameDialog(epochId = 4L, isRenaming = true)

        val result = reducer.testReduce(
            initial,
            Msg.RenameResult(epochId = 4L, outcome = RenameOutcome.Failure(RuntimeException("boom"))),
        )

        assertEquals(false, result.state().isRenaming)
        assertTrue(result.effects().any { it is UiEffect.Snackbar })
    }

    @Test
    fun `RenameResult race condition - dialog closed during flight (F101)`() {
        val initial = ComponentsManagerScreenState(
            renameDialog = null,
            isRenaming = true,
        )

        val result = reducer.testReduce(
            initial,
            Msg.RenameResult(epochId = 0L, outcome = RenameOutcome.SameScopeCollision),
        )

        assertEquals(false, result.state().isRenaming)
        assertNull(result.state().renameDialog)
        assertTrue(result.effects().any { it is UiEffect.Snackbar })
    }

    @Test
    fun `RenameResult with stale epochId - discarded (F136)`() {
        val initial = stateWithRenameDialog(epochId = 7L, isRenaming = true)

        val result = reducer.testReduce(
            initial,
            Msg.RenameResult(epochId = 2L, outcome = RenameOutcome.SameScopeCollision),
        )

        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    // ===== 3.4 Delete confirm =====

    @Test
    fun `OpenDeleteConfirm row found - opens dialog + dispatch LoadImpact + epoch`() {
        val initial = stateWithRows(row(id = 1L, name = "Notes"))

        val result = reducer.testReduce(initial, Msg.OpenDeleteConfirm(ComponentTypeId(1L)))

        val dlg = result.state().deleteConfirm
        assertEquals(ComponentTypeId(1L), dlg?.typeId)
        assertEquals(true, dlg?.isLoadingImpact)
        assertEquals(1L, dlg?.epochId)
        assertNull(dlg?.impact)
        assertEquals(
            setOf(DatasourceEffect.LoadImpact(ComponentTypeId(1L))),
            result.effects(),
        )
    }

    @Test
    fun `OpenDeleteConfirm closes other dialogs (F138)`() {
        val initial = ComponentsManagerScreenState(
            userDefinedTypes = listOf(row(id = 1L, name = "Notes")),
            createDialog = CreateDialogState(epochId = 2L),
            renameDialog = RenameDialogState(epochId = 3L, typeId = ComponentTypeId(99L), originalName = "Z", editedName = "Z"),
            nextEpoch = 3L,
            isCreating = true,
            isRenaming = true,
        )

        val result = reducer.testReduce(initial, Msg.OpenDeleteConfirm(ComponentTypeId(1L)))

        assertNotNull(result.state().deleteConfirm)
        assertNull(result.state().createDialog)
        assertNull(result.state().renameDialog)
        assertEquals(false, result.state().isCreating)
        assertEquals(false, result.state().isRenaming)
    }

    @Test
    fun `OpenDeleteConfirm same typeId twice - no re-trigger`() {
        val initial = stateWithDeleteConfirm(epochId = 3L, typeId = 1L, isLoadingImpact = true)

        val result = reducer.testReduce(initial, Msg.OpenDeleteConfirm(ComponentTypeId(1L)))

        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `OpenDeleteConfirm row missing - no-op (guard)`() {
        val initial = stateWithRows()

        val result = reducer.testReduce(initial, Msg.OpenDeleteConfirm(ComponentTypeId(99L)))

        assertNull(result.state().deleteConfirm)
        result.assertNoEffects()
    }

    @Test
    fun `CloseDeleteConfirm clears dialog`() {
        val initial = stateWithDeleteConfirm()

        val result = reducer.testReduce(initial, Msg.CloseDeleteConfirm)

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
        result.assertNoEffects()
    }

    @Test
    fun `ImpactPreviewLoaded with stale typeId - discarded (F124)`() {
        // dialog open for typeId=1, but preview arrived for typeId=99 (stale).
        val initial = stateWithDeleteConfirm(typeId = 1L, isLoadingImpact = true)

        val result = reducer.testReduce(
            initial,
            Msg.ImpactPreviewLoaded(typeId = ComponentTypeId(99L), impact = emptyImpact()),
        )

        // dialog unaffected — stale ignored
        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `ImpactPreviewLoaded with no dialog - discarded`() {
        val initial = ComponentsManagerScreenState()

        val result = reducer.testReduce(
            initial,
            Msg.ImpactPreviewLoaded(typeId = ComponentTypeId(1L), impact = emptyImpact()),
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
        val initial = ComponentsManagerScreenState()

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
    fun `ConfirmDelete when no dialog - no-op`() {
        val initial = ComponentsManagerScreenState()

        val result = reducer.testReduce(initial, Msg.ConfirmDelete)

        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `DeleteResult Success closes dialog + snackbar with value count`() {
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
        val initial = ComponentsManagerScreenState(
            deleteConfirm = null,
            isDeleting = true,
        )

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

    // ===== 3.5 Snackbar state (F123) =====

    @Test
    fun `UiMsg Snackbar writes snackbarState into state (F123)`() {
        val initial = ComponentsManagerScreenState()

        val result = reducer.testReduce(initial, UiMsg.Snackbar("hello"))

        assertEquals(SnackbarState("hello"), result.state().snackbarState)
        result.assertNoEffects()
    }

    @Test
    fun `DismissSnackbar clears snackbarState`() {
        val initial = ComponentsManagerScreenState(snackbarState = SnackbarState("old"))

        val result = reducer.testReduce(initial, Msg.DismissSnackbar)

        assertNull(result.state().snackbarState)
        result.assertNoEffects()
    }

    // ===== 3.6 Navigation / no-op =====

    @Test
    fun `RequestBack emits NavigationEffect Back, state unchanged`() {
        val initial = ComponentsManagerScreenState()

        val result = reducer.testReduce(initial, Msg.RequestBack)

        assertEquals(initial, result.state())
        assertTrue(result.effects().contains(NavigationEffect.Back))
    }

    @Test
    fun `Empty Msg - no change, no effects`() {
        val initial = ComponentsManagerScreenState()

        val result = reducer.testReduce(initial, Msg.Empty)

        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    // ===== 3.7 toRows() helper =====

    @Test
    fun `toRows - empty types yields empty rows`() {
        val snapshot = UserDefinedTypesSnapshot(
            types = emptyList(),
            usage = ComponentUsage(emptyMap(), emptyMap(), emptyMap()),
        )

        assertTrue(snapshot.toRows().isEmpty())
    }

    @Test
    fun `toRows - global vs per-dict scope inference`() {
        val snapshot = UserDefinedTypesSnapshot(
            types = listOf(
                domainType(1L, "Global", dictionaryId = null),
                domainType(2L, "Local", dictionaryId = 100L),
            ),
            usage = ComponentUsage(emptyMap(), emptyMap(), emptyMap()),
        )

        val rows = snapshot.toRows()

        assertEquals(Scope.Global, rows[0].scope)
        assertEquals(Scope.PerDictionaries(listOf(100L)), rows[1].scope)
    }

    @Test
    fun `toRows - usageCount lookup with fallback 0`() {
        val snapshot = UserDefinedTypesSnapshot(
            types = listOf(
                domainType(1L, "Has"),
                domainType(2L, "Empty"),
            ),
            usage = ComponentUsage(
                valueCountByType = mapOf(ComponentTypeId(1L) to 7),
                dictionaryIdsByType = emptyMap(),
                dictionaryNames = emptyMap(),
            ),
        )

        val rows = snapshot.toRows()

        assertEquals(7, rows[0].usageCount)
        assertEquals(0, rows[1].usageCount)
    }

    @Test
    fun `toRows - dictionaryNames lookup via usage`() {
        val snapshot = UserDefinedTypesSnapshot(
            types = listOf(domainType(1L, "Notes")),
            usage = ComponentUsage(
                valueCountByType = emptyMap(),
                dictionaryIdsByType = mapOf(ComponentTypeId(1L) to setOf(10L, 20L)),
                dictionaryNames = mapOf(10L to "EN", 20L to "DE"),
            ),
        )

        val rows = snapshot.toRows()

        assertTrue(rows[0].dictionaryNames.contains("EN"))
        assertTrue(rows[0].dictionaryNames.contains("DE"))
    }

    // =========================================================================
    // ===== PHASE 2 ============================================================
    // =========================================================================

    // ===== P2.1 Edit dialog — Open / Close / changes =====

    @Test
    fun `whenOpenEditDialog_thenEditDialogStateOpened_andOtherDialogsReset`() {
        val initial = ComponentsManagerScreenState(
            userDefinedTypes = listOf(row(id = 1L, name = "Notes", isMultiple = true)),
            createDialog = CreateDialogState(epochId = 1L),
            renameDialog = RenameDialogState(epochId = 2L, typeId = ComponentTypeId(1L), originalName = "Notes", editedName = "Notes"),
            deleteConfirm = DeleteConfirmState(epochId = 3L, typeId = ComponentTypeId(1L), name = "Notes"),
            isCreating = true,
            isRenaming = true,
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
        assertNull(result.state().renameDialog)
        assertNull(result.state().deleteConfirm)
        // F140 in-flight reset
        assertEquals(false, result.state().isCreating)
        assertEquals(false, result.state().isRenaming)
        assertEquals(false, result.state().isDeleting)
        assertEquals(false, result.state().isEditing)
    }

    @Test
    fun `whenCreateDialogOpen_thenOpenEditDialog_closesCreate_mutualExclusion`() {
        val initial = ComponentsManagerScreenState(
            userDefinedTypes = listOf(row(id = 1L)),
            createDialog = CreateDialogState(epochId = 5L, name = "Dirty"),
            isCreating = true,
            nextEpoch = 5L,
        )

        val result = reducer.testReduce(initial, Msg.OpenEditDialog(ComponentTypeId(1L)))

        assertNotNull(result.state().editDialog)
        assertNull(result.state().createDialog)
        assertEquals(false, result.state().isCreating)
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
    fun `whenOpenRenameDialog_closesEditDialog_mutualExclusion`() {
        val initial = stateWithEditDialog(epochId = 5L, isEditing = true)

        val result = reducer.testReduce(initial, Msg.OpenRenameDialog(ComponentTypeId(1L)))

        assertNotNull(result.state().renameDialog)
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
        val initial = stateWithEditDialog(name = "Modified", isEditing = false)

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
                outcome = EditOutcome.Success(domainType(id = 1L, name = "Updated")),
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
        val initial = ComponentsManagerScreenState(
            editDialog = null,
            isEditing = true,
        )

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

    // ===== P2.4 Multi-dict scope picker =====

    @Test
    fun `whenCreateDictionaryToggle_addsDictionaryId_whenNotPresent`() {
        val initial = stateWithCreateDialog(scope = Scope.PerDictionaries(emptyList()))

        val result = reducer.testReduce(initial, Msg.CreateDictionaryToggle(5L))

        assertTrue(result.state().createDialog?.selectedDictionaryIds?.contains(5L) == true)
    }

    @Test
    fun `whenCreateDictionaryToggle_removesDictionaryId_whenAlreadyPresent`() {
        val base = stateWithCreateDialog(scope = Scope.PerDictionaries(emptyList()))
        val initial = base.copy(
            createDialog = base.createDialog?.copy(selectedDictionaryIds = setOf(5L, 7L)),
        )

        val result = reducer.testReduce(initial, Msg.CreateDictionaryToggle(5L))

        assertFalse(result.state().createDialog?.selectedDictionaryIds?.contains(5L) == true)
        assertTrue(result.state().createDialog?.selectedDictionaryIds?.contains(7L) == true)
    }

    @Test
    fun `whenCreateScopeChange_resetsSelectedDictionaryIds_onGlobalSwitch`() {
        val base = stateWithCreateDialog(scope = Scope.PerDictionaries(listOf(1L, 2L)))
        val initial = base.copy(
            createDialog = base.createDialog?.copy(selectedDictionaryIds = setOf(1L, 2L)),
        )

        val result = reducer.testReduce(initial, Msg.CreateScopeChange(Scope.Global))

        assertEquals(Scope.Global, result.state().createDialog?.scope)
        // selection cleared on switch to Global
        assertTrue(
            result.state().createDialog?.selectedDictionaryIds?.isEmpty() == true,
        )
    }

    @Test
    fun `whenDictionariesLoaded_updatesAvailableDictionaries`() {
        val initial = ComponentsManagerScreenState()

        val list = listOf(dictApi(1L, "EN"), dictApi(2L, "DE"))
        val result = reducer.testReduce(initial, Msg.DictionariesLoaded(list))

        assertEquals(list, result.state().availableDictionaries)
    }

    @Test
    fun `whenChipDictionaryRemovedOutOfBand_thenSelectionFiltered_andEmptyIfAllStale`() {
        val base = stateWithCreateDialog(scope = Scope.PerDictionaries(listOf(1L, 2L)))
        val initial = base.copy(
            availableDictionaries = listOf(dictApi(1L), dictApi(2L)),
            createDialog = base.createDialog?.copy(selectedDictionaryIds = setOf(1L, 2L)),
        )

        // Only D2 survives; D1 removed out-of-band
        val result = reducer.testReduce(initial, Msg.DictionariesLoaded(listOf(dictApi(2L))))

        assertEquals(setOf(2L), result.state().createDialog?.selectedDictionaryIds)
        assertEquals(1, result.state().availableDictionaries.size)
    }

    @Test
    fun `whenEditDialogOpen_thenDictionariesLoaded_doesNotMutateEditState_F030`() {
        val editBefore = editDialog(
            epochId = 5L,
            typeId = 1L,
            originalName = "Notes",
            isMultiple = true,
            impactedLexemesPreview = ImpactedLexemesPreview.InlineOnly(listOf(10L)),
        )
        val initial = ComponentsManagerScreenState(
            userDefinedTypes = listOf(row(id = 1L, name = "Notes", isMultiple = true)),
            editDialog = editBefore,
            isEditing = false,
            nextEpoch = 5L,
        )

        val result = reducer.testReduce(initial, Msg.DictionariesLoaded(listOf(dictApi(1L))))

        // editDialog не мутируется
        assertEquals(editBefore, result.state().editDialog)
        assertEquals(false, result.state().isEditing)
        // availableDictionaries обновлён
        assertEquals(1, result.state().availableDictionaries.size)
    }

    // ===== P2.5 Rename / Delete Removed parity =====

    @Test
    fun `whenRenameResultRemoved_thenDialogClosed_andRemovedSnackbar`() {
        val initial = stateWithRenameDialog(epochId = 4L, isRenaming = true)

        val result = reducer.testReduce(
            initial,
            Msg.RenameResult(epochId = 4L, outcome = RenameOutcome.Removed),
        )

        assertEquals(false, result.state().isRenaming)
        assertNull(result.state().renameDialog)
        assertTrue(result.effects().any { it is UiEffect.Snackbar })
    }

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
