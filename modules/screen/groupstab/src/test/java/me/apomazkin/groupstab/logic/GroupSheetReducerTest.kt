package me.apomazkin.groupstab.logic

import me.apomazkin.mate.effects
import me.apomazkin.mate.state
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * IS493 | Шторка создания/переименования группы: открытие, ввод,
 * live-фильтр, submit, плоские мутационные Msg, гонки dismiss, снекбар.
 */
class GroupSheetReducerTest {

    private val reducer = GroupsTabReducer(logger = NoopLogger)

    private fun baseState() = GroupsTabState(
        isLoading = false,
        dictionaryId = 1L,
        groups = listOf(
            GroupUiItem(id = 5, name = "Быт", count = 0),
            GroupUiItem(id = 6, name = "Дом", count = 0),
        ),
        visibleGroups = listOf(
            GroupUiItem(id = 5, name = "Быт", count = 0),
            GroupUiItem(id = 6, name = "Дом", count = 0),
        ),
    )

    // === Открытие / ввод / фильтр ===

    @Test
    fun `open create sheet - empty input, no filter applied`() {
        val result = reducer.reduce(baseState(), Msg.OpenCreateSheet)

        val sheet = requireNotNull(result.state().sheet)
        assertEquals(GroupSheetMode.Create, sheet.mode)
        assertEquals("", sheet.input)
        assertEquals(baseState().groups, result.state().visibleGroups)
        assertTrue(result.effects().isEmpty())
    }

    @Test
    fun `open rename sheet - input prefilled, filter active (collapses to self)`() {
        // Правка юзера (ручной прогон): фильтр работает и в rename —
        // видеть занятые имена важнее; предзаполненное имя схлопывает
        // список до самой группы.
        val result = reducer.reduce(baseState(), Msg.OpenRenameSheet(groupId = 6))

        val sheet = requireNotNull(result.state().sheet)
        assertEquals(GroupSheetMode.Rename(groupId = 6), sheet.mode)
        assertEquals("Дом", sheet.input)
        assertEquals(listOf(6L), result.state().visibleGroups.map { it.id })
    }

    @Test
    fun `sheet input in create mode - list filtered prefix case-insensitive`() {
        val opened = reducer.reduce(baseState(), Msg.OpenCreateSheet).state()

        val result = reducer.reduce(opened, Msg.SheetInputChanged("дО"))

        assertEquals(listOf(6L), result.state().visibleGroups.map { it.id })
        assertEquals("дО", requireNotNull(result.state().sheet).input)
    }

    @Test
    fun `sheet input cleared - full list back`() {
        val opened = reducer.reduce(baseState(), Msg.OpenCreateSheet).state()
        val filtered = reducer.reduce(opened, Msg.SheetInputChanged("дО")).state()

        val result = reducer.reduce(filtered, Msg.SheetInputChanged(""))

        assertEquals(listOf(5L, 6L), result.state().visibleGroups.map { it.id })
    }

    @Test
    fun `sheet input in rename mode - list filtered too`() {
        val opened = reducer.reduce(baseState(), Msg.OpenRenameSheet(6)).state()

        val result = reducer.reduce(opened, Msg.SheetInputChanged("Б"))

        assertEquals(listOf(5L), result.state().visibleGroups.map { it.id })
    }

    @Test
    fun `dismiss sheet - closed, filter reset`() {
        val opened = reducer.reduce(baseState(), Msg.OpenCreateSheet).state()
        val filtered = reducer.reduce(opened, Msg.SheetInputChanged("дО")).state()

        val result = reducer.reduce(filtered, Msg.DismissSheet)

        assertEquals(null, result.state().sheet)
        assertEquals(listOf(5L, 6L), result.state().visibleGroups.map { it.id })
    }

    // === Submit ===

    @Test
    fun `submit create - effect with input, submitting`() {
        val opened = reducer.reduce(baseState(), Msg.OpenCreateSheet).state()
        val typed = reducer.reduce(opened, Msg.SheetInputChanged("Сад")).state()

        val result = reducer.reduce(typed, Msg.SubmitSheet)

        assertTrue(requireNotNull(result.state().sheet).isSubmitting)
        assertEquals(
            setOf(GroupsEffect.CreateGroup(dictionaryId = 1L, name = "Сад")),
            result.effects(),
        )
    }

    @Test
    fun `submit rename - rename effect`() {
        val opened = reducer.reduce(baseState(), Msg.OpenRenameSheet(6)).state()
        val typed = reducer.reduce(opened, Msg.SheetInputChanged("Дача")).state()

        val result = reducer.reduce(typed, Msg.SubmitSheet)

        assertEquals(
            setOf(GroupsEffect.RenameGroup(groupId = 6, name = "Дача")),
            result.effects(),
        )
    }

    @Test
    fun `submit while submitting - ignored`() {
        val submitting = baseState().copy(
            sheet = GroupSheetState(mode = GroupSheetMode.Create, input = "Сад", isSubmitting = true),
        )

        val result = reducer.reduce(submitting, Msg.SubmitSheet)

        assertEquals(submitting, result.state())
        assertTrue(result.effects().isEmpty())
    }

    // === Плоские мутационные Msg ===

    @Test
    fun `mutation applied - sheet closed, filter reset`() {
        val submitting = baseState().copy(
            sheet = GroupSheetState(mode = GroupSheetMode.Create, input = "Сад", isSubmitting = true),
            visibleGroups = emptyList(),
        )

        val result = reducer.reduce(submitting, Msg.MutationApplied)

        assertEquals(null, result.state().sheet)
        assertEquals(listOf(5L, 6L), result.state().visibleGroups.map { it.id })
    }

    @Test
    fun `mutation rejected - inline error in sheet, input kept`() {
        // Итог ручного прогона Э3: ошибка — строкой под полем (снекбар под
        // шторкой/клавиатурой не виден), шторка открыта, ввод сохранён.
        val submitting = baseState().copy(
            sheet = GroupSheetState(mode = GroupSheetMode.Create, input = "Дом", isSubmitting = true),
        )

        val result = reducer.reduce(
            submitting,
            Msg.MutationRejected(GroupSheetError.DUPLICATE),
        )

        val sheet = requireNotNull(result.state().sheet)
        assertEquals("Дом", sheet.input)
        assertEquals(GroupSheetError.DUPLICATE, sheet.error)
        assertFalse(sheet.isSubmitting)
        assertEquals(null, result.state().errorSnackbar)
    }

    @Test
    fun `error input change - inline error cleared`() {
        val withError = baseState().copy(
            sheet = GroupSheetState(
                mode = GroupSheetMode.Create,
                input = "Дом",
                error = GroupSheetError.DUPLICATE,
            ),
        )

        val result = reducer.reduce(withError, Msg.SheetInputChanged("Дома"))

        assertEquals(null, requireNotNull(result.state().sheet).error)
    }

    @Test
    fun `error after dismiss race - goes to host snackbar`() {
        // Шторку закрыли, пока запись летела: inline негде показать —
        // снекбар host'а.
        val closed = baseState()

        val result = reducer.reduce(
            closed,
            Msg.MutationRejected(GroupSheetError.DUPLICATE),
        )

        assertEquals(null, result.state().sheet)
        assertEquals(GroupSheetError.DUPLICATE, result.state().errorSnackbar)
    }

    @Test
    fun `error snackbar shown - flag reset`() {
        val withError = baseState().copy(errorSnackbar = GroupSheetError.DUPLICATE)

        val result = reducer.reduce(withError, Msg.ErrorSnackbarShown)

        assertEquals(null, result.state().errorSnackbar)
    }

    @Test
    fun `mutation applied with sheet closed - no-op (dismiss race)`() {
        // U-3: swipe/back закрыл шторку, пока запись летела.
        val closed = baseState()

        val result = reducer.reduce(closed, Msg.MutationApplied)

        assertEquals(closed, result.state())
    }

    @Test
    fun `rename reserved - inline error, sheet stays`() {
        val submitting = baseState().copy(
            sheet = GroupSheetState(
                mode = GroupSheetMode.Rename(groupId = 6),
                input = "Все",
                isSubmitting = true,
            ),
        )

        val result = reducer.reduce(submitting, Msg.MutationRejected(GroupSheetError.RESERVED))

        val sheet = requireNotNull(result.state().sheet)
        assertEquals("Все", sheet.input)
        assertEquals(GroupSheetError.RESERVED, sheet.error)
        assertEquals(null, result.state().errorSnackbar)
    }

    @Test
    fun `mutation ignored (folders stub) - submitting off silently`() {
        // Ignored-исходы (заделы фичи папок): молча снять submit,
        // без ошибок и закрытия шторки.
        val submitting = baseState().copy(
            sheet = GroupSheetState(mode = GroupSheetMode.Create, input = "Сад", isSubmitting = true),
        )

        val result = reducer.reduce(submitting, Msg.MutationIgnored)

        val sheet = requireNotNull(result.state().sheet)
        assertFalse(sheet.isSubmitting)
        assertEquals(null, sheet.error)
        assertEquals(null, result.state().errorSnackbar)
        assertTrue(result.effects().isEmpty())
    }

    @Test
    fun `mutation failed - submitting off, sheet stays`() {
        val submitting = baseState().copy(
            sheet = GroupSheetState(mode = GroupSheetMode.Create, input = "Сад", isSubmitting = true),
        )

        val result = reducer.reduce(submitting, Msg.GroupMutationFailed)

        val sheet = requireNotNull(result.state().sheet)
        assertFalse(sheet.isSubmitting)
    }
}
