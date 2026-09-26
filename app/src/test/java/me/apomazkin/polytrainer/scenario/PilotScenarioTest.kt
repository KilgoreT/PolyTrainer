package me.apomazkin.polytrainer.scenario

import io.github.kilgoret.mate.apptest.runAppScenario
import io.github.kilgoret.mate.apptest.stubFlow
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import me.apomazkin.dictionarypicker.entity.DictUiEntity
import me.apomazkin.group.GroupNode
import me.apomazkin.group.MembershipEntry
import me.apomazkin.groupstab.deps.GroupsTabUseCase
import me.apomazkin.groupstab.logic.GroupsTabState
import me.apomazkin.polytrainer.navigation.AppScreen
import me.apomazkin.polytrainer.navigation.appNavTable
import me.apomazkin.wordcard.deps.WordCardUseCase
import me.apomazkin.wordstab.deps.WordsTabUseCase
import me.apomazkin.wordstab.logic.DatasourceEffect
import me.apomazkin.wordstab.logic.WordsTabState
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.seconds
import me.apomazkin.groupstab.logic.Msg as GroupsMsg
import me.apomazkin.wordstab.logic.Msg as WordsMsg

/**
 * Пилот сценарного харнеса: бизнес-логика приложения гоняется целиком
 * на JVM — те же Assembly и ТА ЖЕ таблица навигации [appNavTable], что
 * в проде; стабятся только use case'ы. Живая база симулируется
 * управляемыми stubFlow, время — виртуальное.
 */
class PilotScenarioTest {

    private class Stubs {
        val currentDictId = stubFlow<Long?>()
        val membership = stubFlow<List<MembershipEntry>>()
        val groupTree = stubFlow<List<GroupNode>>()

        val wordsUseCase = mockk<WordsTabUseCase>(relaxed = true)
        val groupsUseCase = mockk<GroupsTabUseCase>(relaxed = true) {
            every { flowCurrentDictId() } returns currentDictId
            every { membershipSlice(any()) } returns membership
            every { groupTree(any()) } returns groupTree
        }
        val wordCardUseCase = mockk<WordCardUseCase>(relaxed = true)

        val registry = pilotRegistry(wordsUseCase, groupsUseCase, wordCardUseCase)
    }

    /** Создание слова: Msg → эффект → use case; слово ушло в базу. */
    @Test
    fun `create word goes through effect to use case`() {
        val stubs = Stubs()
        coEvery { stubs.wordsUseCase.getCurrentDict() } returns
            DictUiEntity(id = 0, flagRes = 0, title = "English", numericCode = 840)
        coEvery { stubs.wordsUseCase.addWord("dom") } returns 42L

        runAppScenario(stubs.registry, appNavTable) {
            launch(AppScreen.Main)

            send(WordsMsg.CreateWord("dom"))

            expectEffect(DatasourceEffect.CreateWord("dom"))
            expectState<WordsTabState> { state ->
                assertEquals(false, state.addWordDialogState.isOpen)
            }
        }
    }

    /**
     * Тап по слову → карточка по продовой таблице; слово к этому
     * моменту удалено извне — карточка сама закрывается (WordNotFound
     * → Back → pop той же таблицей).
     */
    @Test
    fun `open word card for dead word auto-closes back to main`() {
        val stubs = Stubs()
        coEvery { stubs.wordCardUseCase.getTermById(42L) } returns null

        runAppScenario(stubs.registry, appNavTable) {
            launch(AppScreen.Main)

            send(WordsMsg.OpenWordCard(42L))

            expectScreen(AppScreen.Main)
        }
    }

    /**
     * Живой словарь: вкладка «Группы» слушает базу сама — эмиссии
     * стаба «играют базу», счётчик «Все» растёт без единого Msg от UI.
     */
    @Test
    fun `live slice updates groups tab as database changes`() {
        val stubs = Stubs()

        runAppScenario(stubs.registry, appNavTable) {
            launch(AppScreen.Main)

            stubs.currentDictId.emit(1L)
            stubs.groupTree.emit(listOf(GroupNode(id = 5, parentGroupId = null, name = "Дом")))
            stubs.membership.emit(
                listOf(
                    MembershipEntry(wordId = 10, groupId = 5),
                    MembershipEntry(wordId = 11, groupId = null),
                ),
            )
            awaitIdle()

            expectState<GroupsTabState> { state ->
                assertEquals(1L, state.dictionaryId)
                assertEquals(2, state.allNode?.count)
            }

            // База изменилась «под ногами» — слово добавилось.
            stubs.membership.emit(
                listOf(
                    MembershipEntry(wordId = 10, groupId = 5),
                    MembershipEntry(wordId = 11, groupId = null),
                    MembershipEntry(wordId = 12, groupId = 5),
                ),
            )
            awaitIdle()

            expectState<GroupsTabState> { state ->
                assertEquals(3, state.allNode?.count)
            }
        }
    }

    /**
     * Пауза осмысления деструктива на ВИРТУАЛЬНОМ времени: галка
     * запускает тикер-подписку, 5 секунд тиков доводят счётчик до
     * нуля, после чего тикер гаснет диффом — дальнейшее время
     * счётчик не трогает.
     */
    @Test
    fun `destructive countdown ticks on virtual time and stops at zero`() {
        val stubs = Stubs()

        runAppScenario(stubs.registry, appNavTable) {
            launch(AppScreen.Main)
            stubs.currentDictId.emit(1L)
            awaitIdle()

            send(GroupsMsg.RequestDelete(groupId = 5L))
            send(GroupsMsg.ToggleDeleteWords)
            expectState<GroupsTabState> { state ->
                assertEquals(5, state.confirmDelete?.countdownLeft)
            }

            advanceTimeBy(2.seconds)
            expectState<GroupsTabState> { state ->
                assertEquals(3, state.confirmDelete?.countdownLeft)
            }

            advanceTimeBy(3.seconds)
            expectState<GroupsTabState> { state ->
                assertEquals(0, state.confirmDelete?.countdownLeft)
            }

            // Тикер погашен диффом — время больше не тикает в минус.
            advanceTimeBy(10.seconds)
            expectState<GroupsTabState> { state ->
                assertEquals(0, state.confirmDelete?.countdownLeft)
            }
        }
    }

    /** Смена словаря: подписка на выбор переключает slice-подписки. */
    @Test
    fun `dictionary switch rewires group subscriptions`() {
        val stubs = Stubs()

        runAppScenario(stubs.registry, appNavTable) {
            launch(AppScreen.Main)

            stubs.currentDictId.emit(1L)
            stubs.groupTree.emit(emptyList())
            stubs.membership.emit(listOf(MembershipEntry(wordId = 10, groupId = null)))
            awaitIdle()
            expectState<GroupsTabState> { state ->
                assertEquals(1L, state.dictionaryId)
                assertEquals(1, state.allNode?.count)
            }

            // Юзер переключил словарь в аппбаре — prefs-поток эмитит.
            stubs.currentDictId.emit(2L)
            stubs.membership.emit(
                listOf(
                    MembershipEntry(wordId = 20, groupId = null),
                    MembershipEntry(wordId = 21, groupId = null),
                    MembershipEntry(wordId = 22, groupId = null),
                ),
            )
            awaitIdle()

            expectState<GroupsTabState> { state ->
                assertEquals(2L, state.dictionaryId)
                assertEquals(3, state.allNode?.count)
            }
        }
    }
}
