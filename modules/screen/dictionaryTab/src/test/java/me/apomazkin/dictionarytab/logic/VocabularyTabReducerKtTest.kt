package me.apomazkin.dictionarytab.logic

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import me.apomazkin.dictionarypicker.entity.DictUiEntity
import me.apomazkin.dictionarytab.entity.TermUiItem
import me.apomazkin.dictionarytab.entity.WordInfo
import me.apomazkin.mate.*
import me.apomazkin.mate.test.*
import me.apomazkin.ui.logger.LexemeLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date

/**
 * Test cases:
 * 1. Boundary case: загрузка данных словаря
 * 2. Standard case: смена словаря
 * 3. Standard case: добавление слова (есть пересоздание)
 * 4. Standard case: изменение значения слова в диалоге
 * 5. Boundary case: изменение значения слова когда диалог закрыт
 * 6. Standard case: удаление слова (есть пересоздание)
 * 7. Standard case: удаление нескольких слов (есть пересоздание)
 * 8. Standard case: изменение слова (есть пересоздание)
 * 9. Standard case: вход в action mode
 * 10. Standard case: модификация выбора в action mode
 * 11. Standard case: выход из action mode
 * 12. Standard case: управление диалогом добавления слова
 * 13. Standard case: управление диалогом подтверждения удаления
 * 14. Standard case: скрытие диалога подтверждения удаления
 * 15. Standard case: UI сообщения (LifeCycle)
 * 16. Standard case: полный сценарий добавления слова
 * 17. Standard case: полный сценарий изменения слова
 */
class VocabularyTabReducerKtTest {

    private val reducer = VocabularyTabReducer(
        logger = object : LexemeLogger {
            override fun log(tag: String, message: String) {}
        }
    )

    // Тестовые данные
    private val testTermList = listOf(
        TermUiItem(
            id = 1,
            langId = 0,
            wordValue = "1",
            isSelected = false,
            addDate = Date.from(
                LocalDate.now()
                    .atStartOfDay()
                    .toInstant(ZoneOffset.UTC)
            ),
        ),
        TermUiItem(
            id = 2,
            langId = 0,
            wordValue = "2",
            isSelected = false,
            addDate = Date.from(
                LocalDate.now()
                    .minusDays(1)
                    .atStartOfDay()
                    .toInstant(ZoneOffset.UTC)
            ),
        ),
        TermUiItem(
            id = 3,
            langId = 0,
            wordValue = "3",
            isSelected = false,
            addDate = Date.from(
                LocalDate.now()
                    .minusDays(2)
                    .atStartOfDay()
                    .toInstant(ZoneOffset.UTC)
            ),
        ),
    )

    private val testDictEntity = DictUiEntity(
        flagRes = 0,
        title = "Test Language",
        numericCode = 1
    )

    private val testTermFlow = flowOf(PagingData.from(testTermList))
    
    // Создаем состояние с termListMap для тестов
    private fun createTestState(
        isLoading: Boolean = false,
        termListMap: Map<String, Flow<PagingData<TermUiItem>>> = mapOf("" to testTermFlow)
    ) = DictionaryTabState(
        isLoading = isLoading,
        termListMap = termListMap
    )

    // ==================== СЦЕНАРИЙ 1: ЗАГРУЗКА ДАННЫХ СЛОВАРЯ ====================

    /**
     * Сценарий 1: загрузка данных словаря
     */
    @Test
    fun `should load term data when TermDataLoaded is received`() {
        // Test case 1: Boundary case - загрузка данных словаря
        // Given
        val initialState = createTestState(isLoading = true)
        
        // When
        val result = reducer.testReduce(initialState, Msg.TermDataLoaded(pattern = "", termList = testTermFlow))
        
        // Then
        // Main functionality check
        assertFalse("Loading state should be hidden", result.state().isLoading)
        
        // Immutability checks - ALL other properties must remain unchanged
        assertEquals(
            "Top bar state should remain unchanged",
            initialState.topBarState,
            result.state().topBarState
        )
        assertEquals(
            "Add word dialog state should remain unchanged",
            initialState.addWordDialogState,
            result.state().addWordDialogState
        )
        assertEquals(
            "Snackbar state should remain unchanged",
            initialState.snackbarState,
            result.state().snackbarState
        )
        assertEquals(
            "Confirm delete dialog state should remain unchanged",
            initialState.confirmWordDeleteDialogState,
            result.state().confirmWordDeleteDialogState
        )
        result.assertNoEffects("Should have no effects")
    }

    // ==================== СЦЕНАРИЙ 2: СМЕНА СЛОВАРЯ ====================

    /**
     * Сценарий 2: смена словаря
     */
    @Test
    fun `should trigger term flow load when ChangeDict is received`() {
        // Test case 2: Standard case - смена словаря
        // Given
        val initialState = createTestState()
        
        // When
        val result = reducer.testReduce(initialState, Msg.ChangeDict(current = testDictEntity))
        
        // Then
        // Main functionality check
        result.assertEffectsCount(1, "Should have exactly 1 effect")
        result.assertSingleEffect<DatasourceEffect.LoadTermFlow>("Should have LoadTermFlow effect")
        
        // Immutability checks - state should remain unchanged
        assertEquals(
            "State should remain unchanged",
            initialState,
            result.state()
        )
    }

    // ==================== СЦЕНАРИЙ 3: ДОБАВЛЕНИЕ СЛОВА ====================

    /**
     * Сценарий 3: добавление слова (есть пересоздание)
     */
    @Test
    fun `should show add word dialog when ShowAddWordDialog is received`() {
        // Test case 3: Standard case - добавление слова
        // Given
        val initialState = createTestState()

        // When
        val result = reducer.testReduce(initialState, Msg.ShowAddWordDialog())
        
        // Then
        // Main functionality check
        assertTrue("Add word dialog should be open", result.state().addWordDialogState.isOpen)
        assertTrue("Word value should be empty", result.state().addWordDialogState.wordValue.isEmpty())
        assertNull("Word id should be null", result.state().addWordDialogState.wordId)
        
        // Immutability checks - ALL other properties must remain unchanged
        assertEquals(
            "Loading state should remain unchanged",
            initialState.isLoading,
            result.state().isLoading
        )
        assertEquals(
            "Top bar state should remain unchanged",
            initialState.topBarState,
            result.state().topBarState
        )
        assertEquals(
            "Term list should remain unchanged",
            initialState.termList,
            result.state().termList
        )
        assertEquals(
            "Snackbar state should remain unchanged",
            initialState.snackbarState,
            result.state().snackbarState
        )
        assertEquals(
            "Confirm delete dialog state should remain unchanged",
            initialState.confirmWordDeleteDialogState,
            result.state().confirmWordDeleteDialogState
        )
        result.assertNoEffects("Should have no effects")
    }

    // ==================== СЦЕНАРИЙ 4: ИЗМЕНЕНИЕ ЗНАЧЕНИЯ СЛОВА ====================

    /**
     * Сценарий 4: изменение значения слова в диалоге
     */
    @Test
    fun `should update word value when WordValueChange is received and dialog is open`() {
        // Test case 4: Standard case - изменение значения слова в диалоге
        // Given
        val initialState = createTestState().copy(
            addWordDialogState = AddWordDialogState(
                isOpen = true,
                wordValue = ""
            )
        )
        val newValue = "new word"
        
        // When
        val result = reducer.testReduce(initialState, Msg.WordValueChange(newValue))
        
        // Then
        // Main functionality check
        assertTrue("Add word dialog should remain open", result.state().addWordDialogState.isOpen)
        assertEquals("Word value should be updated", newValue, result.state().addWordDialogState.wordValue)
        
        // Immutability checks - ALL other properties must remain unchanged
        assertEquals(
            "Loading state should remain unchanged",
            initialState.isLoading,
            result.state().isLoading
        )
        assertEquals(
            "Top bar state should remain unchanged",
            initialState.topBarState,
            result.state().topBarState
        )
        assertEquals(
            "Snackbar state should remain unchanged",
            initialState.snackbarState,
            result.state().snackbarState
        )
        assertEquals(
            "Confirm delete dialog state should remain unchanged",
            initialState.confirmWordDeleteDialogState,
            result.state().confirmWordDeleteDialogState
        )
        result.assertSingleEffect<DatasourceEffect.LoadTermFlow>("Should have LoadTermFlow effect")
    }

    /**
     * Сценарий 5: изменение значения слова когда диалог закрыт
     */
    @Test
    fun `should not update word value when WordValueChange is received and dialog is closed`() {
        // Test case 5: Boundary case - изменение значения слова когда диалог закрыт
        // Given
        val initialState = createTestState().copy(
            addWordDialogState = AddWordDialogState(
                isOpen = false,
                wordValue = ""
            )
        )
        val newValue = "new word"
        
        // When
        val result = reducer.testReduce(initialState, Msg.WordValueChange(newValue))
        
        // Then
        // Main functionality check
        assertFalse("Add word dialog should remain closed", result.state().addWordDialogState.isOpen)
        assertEquals("Word value should remain unchanged", initialState.addWordDialogState.wordValue, result.state().addWordDialogState.wordValue)
        
        // Immutability checks - state should remain completely unchanged
        assertEquals("State should remain completely unchanged", initialState, result.state())
        result.assertNoEffects("Should have no effects")
    }

    // ==================== СЦЕНАРИЙ 6: УДАЛЕНИЕ СЛОВА ====================

    /**
     * Сценарий 6: удаление слова (есть пересоздание)
     */
    @Test
    fun `should add word when AddWord is received`() {
        // Test case 6: Standard case - удаление слова
        // Given
        val initialState = createTestState().copy(
            addWordDialogState = AddWordDialogState(
                isOpen = true,
                wordValue = "test word"
            )
        )
        val wordValue = "test word"
        
        // When
        val result = reducer.testReduce(initialState, Msg.AddWord(wordValue))
        
        // Then
        // Main functionality check
        assertFalse("Add word dialog should be closed", result.state().addWordDialogState.isOpen)
        assertEquals("Word value should be reset", "", result.state().addWordDialogState.wordValue)
        assertNull("Word id should be reset", result.state().addWordDialogState.wordId)
        
        // Immutability checks - ALL other properties must remain unchanged
        assertEquals(
            "Loading state should remain unchanged",
            initialState.isLoading,
            result.state().isLoading
        )
        assertEquals(
            "Top bar state should remain unchanged",
            initialState.topBarState,
            result.state().topBarState
        )
        assertEquals(
            "Snackbar state should remain unchanged",
            initialState.snackbarState,
            result.state().snackbarState
        )
        assertEquals(
            "Confirm delete dialog state should remain unchanged",
            initialState.confirmWordDeleteDialogState,
            result.state().confirmWordDeleteDialogState
        )
        result.assertSingleEffect<DatasourceEffect.AddWord>("Should have AddWord effect")
    }

    // ==================== СЦЕНАРИЙ 7: УДАЛЕНИЕ НЕСКОЛЬКИХ СЛОВ ====================

    /**
     * Сценарий 7: удаление нескольких слов (есть пересоздание)
     */
    @Test
    fun `should delete multiple words when DeleteWord is received`() {
        // Test case 7: Standard case - удаление нескольких слов
        // Given
        val initialState = createTestState().copy(
            topBarState = TopBarState(isActionMode = true),
            confirmWordDeleteDialogState = ConfirmWordDeleteDialogState(
                isOpen = true,
                wordIds = setOf(WordInfo(1, "1"), WordInfo(2, "2"))
            )
        )
        
        val wordIdsToDelete = setOf(WordInfo(1, "1"), WordInfo(2, "2"))
        
        // When
        val result = reducer.testReduce(initialState, Msg.DeleteWord(wordIdsToDelete))
        
        // Then
        // Main functionality check
        assertFalse("Action mode should be disabled", result.state().topBarState.isActionMode)
        assertFalse("Confirm delete dialog should be closed", result.state().confirmWordDeleteDialogState.isOpen)
        assertTrue("Selected set should be cleared", result.state().topBarState.actionState.selectedTermIds.isEmpty())
        
        // Immutability checks - ALL other properties must remain unchanged
        assertEquals(
            "Loading state should remain unchanged",
            initialState.isLoading,
            result.state().isLoading
        )
        assertEquals(
            "Add word dialog state should remain unchanged",
            initialState.addWordDialogState,
            result.state().addWordDialogState
        )
        assertEquals(
            "Snackbar state should remain unchanged",
            initialState.snackbarState,
            result.state().snackbarState
        )
        
        // Effects check
        result.assertEffectsCount(2, "Should have exactly 2 effects")
        result.assertHasEffect<DatasourceEffect.DeleteWord>("Should have DeleteWord effect")
        result.assertHasEffect<UiEffect.ShowSnackbar>("Should have ShowSnackbar effect")
    }

    // ==================== СЦЕНАРИЙ 8: ИЗМЕНЕНИЕ СЛОВА ====================

    /**
     * Сценарий 8: изменение слова (есть пересоздание)
     */
    @Test
    fun `should start change word when StartChangeWord is received`() {
        // Test case 8: Standard case - изменение слова
        // Given
        val targetTerm = testTermList.first()
        val initialState = createTestState().copy(
            topBarState = TopBarState(isActionMode = true)
        )
        
        // When
        val result = reducer.testReduce(initialState, Msg.StartChangeWord(wordId = targetTerm.id, wordValue = targetTerm.wordValue))
        
        // Then
        // Main functionality check
        assertFalse("Action mode should be disabled", result.state().topBarState.isActionMode)
        assertTrue("Add word dialog should be open", result.state().addWordDialogState.isOpen)
        assertEquals("Word value should be set", targetTerm.wordValue, result.state().addWordDialogState.wordValue)
        assertEquals("Word id should be set", targetTerm.id, result.state().addWordDialogState.wordId)
        
        // Immutability checks - ALL other properties must remain unchanged
        assertEquals(
            "Loading state should remain unchanged",
            initialState.isLoading,
            result.state().isLoading
        )
        assertEquals(
            "Snackbar state should remain unchanged",
            initialState.snackbarState,
            result.state().snackbarState
        )
        assertEquals(
            "Confirm delete dialog state should remain unchanged",
            initialState.confirmWordDeleteDialogState,
            result.state().confirmWordDeleteDialogState
        )
        result.assertNoEffects("Should have no effects")
    }

    /**
     * Сценарий 8: изменение слова - завершение изменения
     */
    @Test
    fun `should change word when ChangeWord is received`() {
        // Test case 8: Standard case - завершение изменения слова
        // Given
        val targetTerm = testTermList.first()
        val initialState = createTestState().copy(
            topBarState = TopBarState(isActionMode = true),
            addWordDialogState = AddWordDialogState(
                isOpen = true,
                wordValue = targetTerm.wordValue,
                wordId = targetTerm.id
            )
        )
        
        // When
        val result = reducer.testReduce(initialState, Msg.ChangeWord(targetTerm.id, "updated word"))
        
        // Then
        // Main functionality check
        assertFalse("Action mode should be disabled", result.state().topBarState.isActionMode)
        assertFalse("Add word dialog should be closed", result.state().addWordDialogState.isOpen)
        assertTrue("Selected set should be cleared", result.state().topBarState.actionState.selectedTermIds.isEmpty())
        
        // Immutability checks - ALL other properties must remain unchanged
        assertEquals(
            "Loading state should remain unchanged",
            initialState.isLoading,
            result.state().isLoading
        )
        assertEquals(
            "Snackbar state should remain unchanged",
            initialState.snackbarState,
            result.state().snackbarState
        )
        assertEquals(
            "Confirm delete dialog state should remain unchanged",
            initialState.confirmWordDeleteDialogState,
            result.state().confirmWordDeleteDialogState
        )
        result.assertSingleEffect<DatasourceEffect.ChangeWord>("Should have ChangeWord effect")
    }

    // ==================== СЦЕНАРИЙ 9: ВХОД В ACTION MODE ====================

    /**
     * Сценарий 9: вход в action mode
     */
    @Test
    fun `should show action mode when ShowActionMode is received`() {
        // Test case 9: Standard case - вход в action mode
        // Given
        val initialState = createTestState()
        val targetWord = WordInfo(1, "test")
        
        // When
        val result = reducer.testReduce(initialState, Msg.ShowActionMode(targetWord))
        
        // Then
        // Main functionality check
        assertTrue("Action mode should be enabled", result.state().topBarState.isActionMode)
        assertTrue("Target word should be in selected set", result.state().topBarState.actionState.selectedTermIds.contains(targetWord))
        
        // Immutability checks - ALL other properties must remain unchanged
        assertEquals(
            "Loading state should remain unchanged",
            initialState.isLoading,
            result.state().isLoading
        )
        assertEquals(
            "Add word dialog state should remain unchanged",
            initialState.addWordDialogState,
            result.state().addWordDialogState
        )
        assertEquals(
            "Snackbar state should remain unchanged",
            initialState.snackbarState,
            result.state().snackbarState
        )
        assertEquals(
            "Confirm delete dialog state should remain unchanged",
            initialState.confirmWordDeleteDialogState,
            result.state().confirmWordDeleteDialogState
        )
        result.assertNoEffects("Should have no effects")
    }

    // ==================== СЦЕНАРИЙ 10: МОДИФИКАЦИЯ ВЫБОРА В ACTION MODE ====================

    /**
     * Сценарий 10: модификация выбора в action mode
     */
    @Test
    fun `should modify selected set when ModifySelectedInActionMode is received`() {
        // Test case 10: Standard case - модификация выбора в action mode
        // Given
        val initialState = createTestState().copy(
            topBarState = TopBarState(
                isActionMode = true,
                actionState = TopBarState.Action(selectedTermIds = setOf(WordInfo(1, "word1")))
            )
        )
        
        val targetWord = WordInfo(2, "word2")
        
        // When
        val result = reducer.testReduce(initialState, Msg.ModifySelectedInActionMode(targetWord))
        
        // Then
        // Main functionality check
        assertTrue("Target word should be added to selected set", result.state().topBarState.actionState.selectedTermIds.contains(targetWord))
        assertTrue("Original word should remain in selected set", result.state().topBarState.actionState.selectedTermIds.contains(WordInfo(1, "word1")))
        
        // Immutability checks - ALL other properties must remain unchanged
        assertEquals(
            "Loading state should remain unchanged",
            initialState.isLoading,
            result.state().isLoading
        )
        assertEquals(
            "Add word dialog state should remain unchanged",
            initialState.addWordDialogState,
            result.state().addWordDialogState
        )
        assertEquals(
            "Snackbar state should remain unchanged",
            initialState.snackbarState,
            result.state().snackbarState
        )
        assertEquals(
            "Confirm delete dialog state should remain unchanged",
            initialState.confirmWordDeleteDialogState,
            result.state().confirmWordDeleteDialogState
        )
        result.assertNoEffects("Should have no effects")
    }

    // ==================== СЦЕНАРИЙ 11: ВЫХОД ИЗ ACTION MODE ====================

    /**
     * Сценарий 11: выход из action mode
     */
    @Test
    fun `should hide action mode when HideActionMode is received`() {
        // Test case 11: Standard case - выход из action mode
        // Given
        val initialState = createTestState().copy(
            topBarState = TopBarState(
                isActionMode = true,
                actionState = TopBarState.Action(selectedTermIds = setOf(WordInfo(1, "word1")))
            )
        )
        
        // When
        val result = reducer.testReduce(initialState, Msg.HideActionMode)
        
        // Then
        // Main functionality check
        assertFalse("Action mode should be disabled", result.state().topBarState.isActionMode)
        assertTrue("Selected set should be cleared", result.state().topBarState.actionState.selectedTermIds.isEmpty())
        
        // Immutability checks - ALL other properties must remain unchanged
        assertEquals(
            "Loading state should remain unchanged",
            initialState.isLoading,
            result.state().isLoading
        )
        assertEquals(
            "Add word dialog state should remain unchanged",
            initialState.addWordDialogState,
            result.state().addWordDialogState
        )
        assertEquals(
            "Snackbar state should remain unchanged",
            initialState.snackbarState,
            result.state().snackbarState
        )
        assertEquals(
            "Confirm delete dialog state should remain unchanged",
            initialState.confirmWordDeleteDialogState,
            result.state().confirmWordDeleteDialogState
        )
        result.assertNoEffects("Should have no effects")
    }

    // ==================== СЦЕНАРИЙ 12: УПРАВЛЕНИЕ ДИАЛОГОМ ДОБАВЛЕНИЯ СЛОВА ====================

    /**
     * Сценарий 12: управление диалогом добавления слова
     */
    @Test
    fun `should hide add word dialog when HideAddWordDialog is received`() {
        // Test case 12: Standard case - управление диалогом добавления слова
        // Given
        val initialState = createTestState().copy(
            addWordDialogState = AddWordDialogState(
                isOpen = true,
                wordValue = "test word",
                wordId = 1L
            )
        )
        
        // When
        val result = reducer.testReduce(initialState, Msg.HideAddWordDialog)
        
        // Then
        // Main functionality check
        assertFalse("Add word dialog should be closed", result.state().addWordDialogState.isOpen)
        assertEquals("Word value should be reset", "", result.state().addWordDialogState.wordValue)
        assertNull("Word id should be reset", result.state().addWordDialogState.wordId)
        
        // Immutability checks - ALL other properties must remain unchanged
        assertEquals(
            "Loading state should remain unchanged",
            initialState.isLoading,
            result.state().isLoading
        )
        assertEquals(
            "Top bar state should remain unchanged",
            initialState.topBarState,
            result.state().topBarState
        )
        assertEquals(
            "Snackbar state should remain unchanged",
            initialState.snackbarState,
            result.state().snackbarState
        )
        assertEquals(
            "Confirm delete dialog state should remain unchanged",
            initialState.confirmWordDeleteDialogState,
            result.state().confirmWordDeleteDialogState
        )
        result.assertNoEffects("Should have no effects")
    }

    // ==================== СЦЕНАРИЙ 13: УПРАВЛЕНИЕ ДИАЛОГОМ ПОДТВЕРЖДЕНИЯ УДАЛЕНИЯ ====================

    /**
     * Сценарий 13: управление диалогом подтверждения удаления
     */
    @Test
    fun `should show confirm delete dialog when ShowConfirmDeleteWordDialog is received`() {
        // Test case 13: Standard case - управление диалогом подтверждения удаления
        // Given
        val initialState = createTestState()
        val wordIds = setOf(WordInfo(1, "word1"), WordInfo(2, "word2"))
        
        // When
        val result = reducer.testReduce(initialState, Msg.ShowConfirmDeleteWordDialog(wordIds))
        
        // Then
        // Main functionality check
        assertTrue("Confirm delete dialog should be open", result.state().confirmWordDeleteDialogState.isOpen)
        assertEquals("Word ids should be set", wordIds, result.state().confirmWordDeleteDialogState.wordIds)
        
        // Immutability checks - ALL other properties must remain unchanged
        assertEquals(
            "Loading state should remain unchanged",
            initialState.isLoading,
            result.state().isLoading
        )
        assertEquals(
            "Top bar state should remain unchanged",
            initialState.topBarState,
            result.state().topBarState
        )
        assertEquals(
            "Term list should remain unchanged",
            initialState.termList,
            result.state().termList
        )
        assertEquals(
            "Add word dialog state should remain unchanged",
            initialState.addWordDialogState,
            result.state().addWordDialogState
        )
        assertEquals(
            "Snackbar state should remain unchanged",
            initialState.snackbarState,
            result.state().snackbarState
        )
        result.assertNoEffects("Should have no effects")
    }

    /**
     * Сценарий 14: скрытие диалога подтверждения удаления
     */
    @Test
    fun `should hide confirm delete dialog when HideConfirmDeleteWordDialog is received`() {
        // Test case 14: Standard case - скрытие диалога подтверждения удаления
        // Given
        val initialState = createTestState().copy(
            confirmWordDeleteDialogState = ConfirmWordDeleteDialogState(
                isOpen = true,
                wordIds = setOf(WordInfo(1, "word1"))
            )
        )
        
        // When
        val result = reducer.testReduce(initialState, Msg.HideConfirmDeleteWordDialog)
        
        // Then
        // Main functionality check
        assertFalse("Confirm delete dialog should be closed", result.state().confirmWordDeleteDialogState.isOpen)
        assertTrue("Word ids should be cleared", result.state().confirmWordDeleteDialogState.wordIds.isEmpty())
        
        // Immutability checks - ALL other properties must remain unchanged
        assertEquals(
            "Loading state should remain unchanged",
            initialState.isLoading,
            result.state().isLoading
        )
        assertEquals(
            "Top bar state should remain unchanged",
            initialState.topBarState,
            result.state().topBarState
        )
        assertEquals(
            "Term list should remain unchanged",
            initialState.termList,
            result.state().termList
        )
        assertEquals(
            "Add word dialog state should remain unchanged",
            initialState.addWordDialogState,
            result.state().addWordDialogState
        )
        assertEquals(
            "Snackbar state should remain unchanged",
            initialState.snackbarState,
            result.state().snackbarState
        )
        result.assertNoEffects("Should have no effects")
    }

    // ==================== СЦЕНАРИЙ 15: UI СООБЩЕНИЯ ====================

    /**
     * Сценарий 15: UI сообщения (LifeCycle)
     */
    @Test
    fun `should process lifecycle event when UiMsgLifeCycleEvent is received`() {
        // Test case 15: Standard case - UI сообщения (LifeCycle)
        // Given
        val initialState = createTestState()
        val lifecycleEvent = UiMsg.LifeCycleEvent(UiMsg.LifeCycleEvent.LifeCycle.ON_CREATE)
        
        // When
        val result = reducer.testReduce(initialState, lifecycleEvent)
        
        // Then
        // Main functionality check - state should remain unchanged
        assertEquals("State should remain unchanged", initialState, result.state())
        result.assertNoEffects("Should have no effects")
    }

    // ==================== СЦЕНАРИЙ 17: ПОЛНЫЙ СЦЕНАРИЙ ДОБАВЛЕНИЯ СЛОВА ====================

    /**
     * Сценарий 17: полный сценарий добавления слова
     */
    @Test
    fun `should handle complete add word flow`() {
        // Test case 17: Standard case - полный сценарий добавления слова
        // Given
        val initialState = createTestState()
        
        // When
        val results = reducer.testScenario(
            initialState,
            Msg.ShowAddWordDialog(),
            Msg.WordValueChange("new word"),
            Msg.AddWord("new word")
        )
        
        // Then
        // Step 1: ShowAddWordDialog
        val step1 = results[0]
        assertTrue("Step 1: Dialog should be open", step1.state().addWordDialogState.isOpen)
        assertTrue("Step 1: Word value should be empty", step1.state().addWordDialogState.wordValue.isEmpty())
        step1.assertNoEffects("Step 1: Should have no effects")
        
        // Step 2: WordValueChange
        val step2 = results[1]
        assertTrue("Step 2: Dialog should remain open", step2.state().addWordDialogState.isOpen)
        assertEquals("Step 2: Word value should be updated", "new word", step2.state().addWordDialogState.wordValue)
        step2.assertSingleEffect<DatasourceEffect.LoadTermFlow>("Step 2: Should have LoadTermFlow effect")
        
        // Step 3: AddWord
        val step3 = results[2]
        assertFalse("Step 3: Dialog should be closed", step3.state().addWordDialogState.isOpen)
        assertEquals("Step 3: Word value should be reset", "", step3.state().addWordDialogState.wordValue)
        step3.assertSingleEffect<DatasourceEffect.AddWord>("Step 3: Should have AddWord effect")
    }

    // ==================== СЦЕНАРИЙ 18: ПОЛНЫЙ СЦЕНАРИЙ ИЗМЕНЕНИЯ СЛОВА ====================

    /**
     * Сценарий 18: полный сценарий изменения слова
     */
    @Test
    fun `should handle complete change word flow`() {
        // Test case 18: Standard case - полный сценарий изменения слова
        // Given
        val targetTerm = testTermList.first()
        val initialState = createTestState().copy(
            topBarState = TopBarState(isActionMode = true)
        )
        
        // When
        val results = reducer.testScenario(
            initialState,
            Msg.StartChangeWord(wordId = targetTerm.id, wordValue = targetTerm.wordValue),
            Msg.WordValueChange("updated word"),
            Msg.ChangeWord(targetTerm.id, "updated word")
        )
        
        // Then
        // Step 1: StartChangeWord
        val step1 = results[0]
        assertFalse("Step 1: Action mode should be disabled", step1.state().topBarState.isActionMode)
        assertTrue("Step 1: Dialog should be open", step1.state().addWordDialogState.isOpen)
        assertEquals("Step 1: Word value should be set", targetTerm.wordValue, step1.state().addWordDialogState.wordValue)
        step1.assertNoEffects("Step 1: Should have no effects")
        
        // Step 2: WordValueChange
        val step2 = results[1]
        assertTrue("Step 2: Dialog should remain open", step2.state().addWordDialogState.isOpen)
        assertEquals("Step 2: Word value should be updated", "updated word", step2.state().addWordDialogState.wordValue)
        step2.assertSingleEffect<DatasourceEffect.LoadTermFlow>("Step 2: Should have LoadTermFlow effect")
        
        // Step 3: ChangeWord
        val step3 = results[2]
        assertFalse("Step 3: Action mode should remain disabled", step3.state().topBarState.isActionMode)
        assertFalse("Step 3: Dialog should be closed", step3.state().addWordDialogState.isOpen)
        step3.assertSingleEffect<DatasourceEffect.ChangeWord>("Step 3: Should have ChangeWord effect")
    }
}