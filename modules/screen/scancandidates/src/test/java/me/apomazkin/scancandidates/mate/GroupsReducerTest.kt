package me.apomazkin.scancandidates.mate

import me.apomazkin.candidates.CandidatesSheetUi
import me.apomazkin.candidates.CardOwnGroupUi
import me.apomazkin.candidates.CardTargetUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Группы и «куда класть» глазами редьюсера: общая и своя группа (AWP-08), выбор
 * леммы словаря для карточки (AWP-18). Выбор — не правка: флага «были
 * изменения» не ставит (AWP-19.6, AWP-19.8). Адреса строк спеки — `AWP-NN_k`.
 */
class GroupsReducerTest {
    private val reducer = reducer()

    private val withBank = Fx.form(existing = listOf(Fx.bank1, Fx.bank2))
    private val secondBank = CardTargetUi.Existing(Fx.BANK_2)

    // ===== Группа общая и своя (AWP-08) =====

    @Test
    fun `AWP-08_1 AWP-20_7 a common group and an own group go to the batch each to its words`() {
        val window = reducer.window(Fx.ran, Fx.bank, form = withBank)
        val common = reducer.step(window, Msg.SetCommonGroup(Fx.POTTER))
        val own = reducer.step(common, Msg.SetCardGroup("bank", CardOwnGroupUi.Group(Fx.FINANCE)))

        val batch = reducer.batchOf(own)

        assertEquals(listOf("run", "bank"), batch.words.map { it.text })
        assertEquals(listOf(Fx.POTTER, Fx.FINANCE), batch.words.map { it.groupId })
    }

    @Test
    fun `AWP-08_3 AWP-20_2 a dictionary without groups has only all, the batch has none`() {
        val window = reducer.window(Fx.ran, form = Fx.form(groups = emptyList()))

        val batch = reducer.batchOf(window)

        assertTrue(requireNotNull(window.form).groups.isEmpty())
        assertNull(window.commonGroupId)
        assertEquals(listOf<Long?>(null), batch.words.map { it.groupId })
    }

    @Test
    fun `AWP-08_4 an own all beats the common group and leaves the lemma without a group`() {
        val window = reducer.window(Fx.ran, Fx.bank, form = withBank)
        val common = reducer.step(window, Msg.SetCommonGroup(Fx.POTTER))
        val own = reducer.step(common, Msg.SetCardGroup("bank", CardOwnGroupUi.All))

        val batch = reducer.batchOf(own)

        assertEquals(listOf(Fx.POTTER, null), batch.words.map { it.groupId })
    }

    @Test
    fun `AWP-08_5 taking the own group off brings the common group back`() {
        val window = reducer.window(Fx.bank, form = withBank)
        val common = reducer.step(window, Msg.SetCommonGroup(Fx.POTTER))
        val own = reducer.step(common, Msg.SetCardGroup("bank", CardOwnGroupUi.Group(Fx.FINANCE)))

        val cleared = reducer.step(own, Msg.SetCardGroup("bank", CardOwnGroupUi.AsCommon))

        assertEquals(CardOwnGroupUi.Group(Fx.FINANCE), own.card("bank").ownGroup)
        assertEquals(CardOwnGroupUi.AsCommon, cleared.card("bank").ownGroup)
        assertEquals(listOf(Fx.POTTER), reducer.batchOf(cleared).words.map { it.groupId })
    }

    @Test
    fun `AWP-08_7 a new window starts with the group all whatever was chosen before`() {
        val window = reducer.window(Fx.ran)
        val chosen = reducer.step(window, Msg.SetCommonGroup(Fx.POTTER))

        val reopened = reducer.step(chosen, Msg.FormLoaded(Fx.form()))

        assertEquals(Fx.POTTER, chosen.commonGroupId)
        assertNull(window.commonGroupId)
        assertNull(reopened.commonGroupId)
    }

    @Test
    fun `AWP-08_1 choosing a group closes the sheet and is not a change`() {
        val window = reducer.window(Fx.ran, Fx.bank, form = withBank)
        val common = reducer.step(window, Msg.OpenSheet(CandidatesSheetUi.CommonGroup))
        val own = reducer.step(window, Msg.OpenSheet(CandidatesSheetUi.CardGroup("bank")))

        val afterCommon = reducer.step(common, Msg.SetCommonGroup(Fx.POTTER))
        val afterOwn = reducer.step(own, Msg.SetCardGroup("bank", CardOwnGroupUi.Group(Fx.FINANCE)))

        assertNull(afterCommon.sheet)
        assertNull(afterOwn.sheet)
        assertFalse(afterCommon.isDirty)
        assertFalse(afterOwn.isDirty)
    }

    // ===== Куда класть (AWP-18) =====

    @Test
    fun `AWP-18_1 a card switches between the lemmas of the dictionary and a new word`() {
        val window = reducer.window(Fx.bank, form = withBank)

        val second = reducer.step(window, Msg.SetCardTarget("bank", secondBank))
        val fresh = reducer.step(second, Msg.SetCardTarget("bank", CardTargetUi.NewWord))

        assertEquals(2, window.card("bank").existingCount)
        assertEquals(CardTargetUi.Existing(Fx.BANK_1), window.card("bank").target)
        assertEquals(secondBank, second.card("bank").target)
        assertEquals(CardTargetUi.NewWord, fresh.card("bank").target)
    }

    @Test
    fun `AWP-18_3 the first lemma of the list is the default and without lemmas a new word`() {
        val window = reducer.window(Fx.ran, Fx.bank, form = withBank)

        assertEquals(CardTargetUi.Existing(Fx.BANK_1), window.card("bank").target)
        assertEquals(CardTargetUi.NewWord, window.card("run").target)
    }

    @Test
    fun `AWP-18_4 a new word at two lemmas with the text goes to the batch without a word id`() {
        val window = reducer.window(Fx.bank, form = withBank)
        val fresh = reducer.step(window, Msg.SetCardTarget("bank", CardTargetUi.NewWord))
        val second = reducer.step(window, Msg.SetCardTarget("bank", secondBank))

        assertEquals(listOf<Long?>(Fx.BANK_1), reducer.batchOf(window).words.map { it.wordId })
        assertEquals(listOf<Long?>(null), reducer.batchOf(fresh).words.map { it.wordId })
        assertEquals(listOf<Long?>(Fx.BANK_2), reducer.batchOf(second).words.map { it.wordId })
    }

    @Test
    fun `AWP-18_4 AWP-19_8 choosing where to put closes the sheet and is not a change`() {
        val window = reducer.window(Fx.bank, form = withBank)
        val opened = reducer.step(window, Msg.OpenSheet(CandidatesSheetUi.CardTarget("bank")))

        val result = reducer.step(opened, Msg.SetCardTarget("bank", CardTargetUi.NewWord))

        assertNull(result.sheet)
        assertFalse(result.isDirty)
    }
}
