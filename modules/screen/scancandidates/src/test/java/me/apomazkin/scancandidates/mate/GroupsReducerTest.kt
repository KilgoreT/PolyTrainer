package me.apomazkin.scancandidates.mate

import me.apomazkin.candidates.CandidatesSheetUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Общая группа глазами редьюсера (AWP-08): выбор в листе, «Все» у нового окна.
 * На этапе 1 выбор ни на что не влияет — записи нет; хранится ради этапа 2.
 * Адреса строк спеки — `AWP-NN_k`.
 */
class GroupsReducerTest {
    private val reducer = reducer()

    @Test
    fun `choosing a group in the sheet keeps it in the state and closes the sheet`() {
        val opened = reducer.step(reducer.window(), Msg.OpenSheet(CandidatesSheetUi.CommonGroup))

        val chosen = reducer.step(opened, Msg.SetCommonGroup(Fx.POTTER))

        assertEquals(CandidatesSheetUi.CommonGroup, opened.sheet)
        assertEquals(Fx.POTTER, chosen.commonGroupId)
        assertNull(chosen.sheet)
    }

    @Test
    fun `a dictionary without groups has only all`() {
        val window = reducer.window(form = Fx.form(groups = emptyList()))

        assertTrue(requireNotNull(window.form).groups.isEmpty())
        assertNull(window.commonGroupId)
    }

    @Test
    fun `AWP-08_7 a new window starts with the group all whatever was chosen before`() {
        val window = reducer.window()
        val chosen = reducer.step(window, Msg.SetCommonGroup(Fx.POTTER))

        val reopened = reducer.step(chosen, Msg.FormLoaded(Fx.form()))

        assertEquals(Fx.POTTER, chosen.commonGroupId)
        assertNull(window.commonGroupId)
        assertNull(reopened.commonGroupId)
    }

    @Test
    fun `closing the sheet without a choice keeps the group`() {
        val chosen = reducer.step(reducer.window(), Msg.SetCommonGroup(Fx.FINANCE))
        val opened = reducer.step(chosen, Msg.OpenSheet(CandidatesSheetUi.CommonGroup))

        val closed = reducer.step(opened, Msg.CloseSheet)

        assertEquals(Fx.FINANCE, closed.commonGroupId)
        assertNull(closed.sheet)
    }
}
