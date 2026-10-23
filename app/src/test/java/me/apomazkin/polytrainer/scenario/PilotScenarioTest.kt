package me.apomazkin.polytrainer.scenario

import io.github.kilgoret.mate.NavigationEffect
import io.github.kilgoret.mate.apptest.runAppScenario
import io.github.kilgoret.mate.apptest.stubFlow
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import me.apomazkin.dictionarypicker.entity.DictUiEntity
import me.apomazkin.group.GroupNode
import me.apomazkin.group.MembershipEntry
import me.apomazkin.groupstab.deps.GroupsTabUseCase
import me.apomazkin.groupstab.logic.GroupsTabState
import me.apomazkin.lexeme.AddWordsBatchOutcome
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentMatch
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.DependencyTarget
import me.apomazkin.lexeme.DictionaryForm
import me.apomazkin.lexeme.FormComponent
import me.apomazkin.lexeme.FormOutcome
import me.apomazkin.lexeme.LookupLemma
import me.apomazkin.lexeme.LookupLexeme
import me.apomazkin.lexeme.LookupOutcome
import me.apomazkin.lexeme.LookupToken
import me.apomazkin.lexeme.LookupWord
import me.apomazkin.lexeme.LookupWordStatus
import me.apomazkin.lexeme.RecognitionOutcome
import me.apomazkin.polytrainer.navigation.AppScreen
import me.apomazkin.polytrainer.navigation.appNavTable
import me.apomazkin.scancandidates.deps.ScanCandidatesUseCase
import me.apomazkin.scancandidates.mate.ScanCandidatesState
import me.apomazkin.scancandidates.mate.ScanNavigationEffect
import me.apomazkin.scancandidates.mate.ScanPhase
import me.apomazkin.wordcard.deps.WordCardUseCase
import me.apomazkin.wordstab.deps.WordsTabUseCase
import me.apomazkin.wordstab.logic.DatasourceEffect
import me.apomazkin.wordstab.logic.WordsTabState
import me.apomazkin.wordstab.ui.WordsNavigationEffect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds
import me.apomazkin.groupstab.logic.Msg as GroupsMsg
import me.apomazkin.scancandidates.mate.Msg as ScanMsg
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
        val scanUseCase = mockk<ScanCandidatesUseCase>(relaxed = true)

        val registry = pilotRegistry(wordsUseCase, groupsUseCase, wordCardUseCase, scanUseCase)
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

    // ===== Экран лексем-кандидатов со снимка (Д51) =====

    private val perroLexeme = LookupLexeme(
        key = "ruwiktionary/1.noun.1",
        partOfSpeechKey = "noun",
        source = "ruwiktionary",
        translations = listOf("собака"),
        gloss = null,
        caption = null,
        notes = emptyList(),
        examples = emptyList(),
        grammar = emptyMap(),
        level = null,
        synonyms = emptyList(),
        antonyms = emptyList(),
        componentMatches = listOf(
            ComponentMatch(componentId = TRANSLATION, optionId = null, field = "translations"),
        ),
    )

    private val perro = LookupWord(
        text = "perro",
        status = LookupWordStatus.FOUND,
        lemmas = listOf(
            LookupLemma(
                text = "perro",
                formNote = null,
                pronunciations = emptyList(),
                lexemes = listOf(perroLexeme),
            ),
        ),
        remarks = emptyList(),
    )

    private val form = DictionaryForm(
        dictionaryId = 1L,
        translationLanguage = "ru",
        components = listOf(
            FormComponent(
                id = TRANSLATION,
                systemKey = BuiltInComponent.TRANSLATION,
                name = null,
                template = ComponentTemplate.TEXT,
                isCore = true,
                isMultiple = false,
                position = 0,
                dependsOn = DependencyTarget.Lexeme,
            ),
        ),
        options = emptyMap(),
        groups = emptyList(),
        existingLemmas = emptyList(),
    )

    /** Снимок с одним словом «perro»: распознан, найден на сервере, форма собрана. */
    private fun Stubs.photoWithPerro() {
        coEvery { scanUseCase.recognize(PHOTO) } returns
            RecognitionOutcome.Text(listOf(LookupToken("perro", 0.9f, "es")))
        coEvery { scanUseCase.releaseImageAccess(PHOTO) } just Runs
        coEvery { scanUseCase.lookupWords(any()) } returns
            LookupOutcome.Words(dictionaryId = 1L, words = listOf(perro), recommendations = emptyList())
        coEvery { scanUseCase.loadForm(1L, listOf("perro")) } returns FormOutcome.Loaded(form)
    }

    /**
     * Путь снимка целиком (AWP-23, WFP-23): кнопка вкладки → выбор источника →
     * снимок получен → вкладка открывает экран по продовой таблице → экран сам
     * распознаёт, спрашивает сервер и собирает форму → «Добавить» → запись →
     * экран закрывается результатом, который таблица кладёт псевдоэкраном.
     */
    @Test
    fun `AWP-23_1 from the scan button to the result of the write`() {
        val stubs = Stubs()
        stubs.photoWithPerro()
        coEvery { stubs.scanUseCase.addWords(any()) } returns
            AddWordsBatchOutcome.Added(wordIds = listOf(100L), lexemeIds = listOf(1000L))

        runAppScenario(stubs.registry, appNavTable) {
            launch(AppScreen.Main)

            send(WordsMsg.OpenScanDialog)
            expectState<WordsTabState> { state -> assertTrue(state.scanDialogState.isOpen) }
            send(WordsMsg.ChooseGallery)
            send(WordsMsg.ImagePicked(PHOTO))

            expectEffect(WordsNavigationEffect.OpenScanCandidates(PHOTO))
            expectScreen(AppScreen.ScanCandidates(PHOTO))
            expectState<ScanCandidatesState> { state ->
                assertEquals(ScanPhase.WINDOW, state.phase)
                assertEquals(listOf("perro"), state.cards.map { it.lemma })
            }

            send(ScanMsg.RequestAdd)
            send(ScanMsg.ConfirmAdd)

            expectEffect(ScanNavigationEffect.CloseWithResult(lexemeCount = 1, wordCount = 1))
            expectScreen(AppScreen.ScanCandidatesAdded(lexemeCount = 1, wordCount = 1))
        }

        coVerify(exactly = 1) { stubs.scanUseCase.recognize(PHOTO) }
        coVerify(exactly = 1) { stubs.scanUseCase.releaseImageAccess(PHOTO) }
        coVerify(exactly = 1) { stubs.scanUseCase.addWords(any()) }
    }

    /** Снимок пришёл при закрытом диалоге — после гибели процесса в камере (WFP-22, Д52). */
    @Test
    fun `WFP-22_1 a photo that comes with the dialog closed still opens the screen`() {
        val stubs = Stubs()
        stubs.photoWithPerro()

        runAppScenario(stubs.registry, appNavTable) {
            launch(AppScreen.Main)

            send(WordsMsg.ImagePicked(PHOTO))

            expectScreen(AppScreen.ScanCandidates(PHOTO))
            expectState<ScanCandidatesState> { state -> assertEquals(ScanPhase.WINDOW, state.phase) }
        }
    }

    /** Закрыть нетронутое окно — назад на вкладку без результата и без записи (AWP-19, AWP-23). */
    @Test
    fun `AWP-19_1 closing an untouched window goes back to the tab, nothing is written`() {
        val stubs = Stubs()
        stubs.photoWithPerro()

        runAppScenario(stubs.registry, appNavTable) {
            launch(AppScreen.Main)
            send(WordsMsg.ImagePicked(PHOTO))
            expectScreen(AppScreen.ScanCandidates(PHOTO))

            send(ScanMsg.RequestClose)

            expectEffect(NavigationEffect.Back)
            expectScreen(AppScreen.Main)
            assertFalse(screens.any { it is AppScreen.ScanCandidatesAdded })
        }

        coVerify(exactly = 0) { stubs.scanUseCase.addWords(any()) }
    }

    private companion object {
        const val TRANSLATION = 1L
        const val PHOTO = "content://media/picker/0/com.android.providers.media.photopicker/media/1000012"
    }
}
