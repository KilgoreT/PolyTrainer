package me.apomazkin.polytrainer.uiDeps

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import me.apomazkin.main.MainUiDeps
import me.apomazkin.ui.logger.LexemeLogger
import me.apomazkin.vocabulary.deps.VocabularyUseCase
import me.apomazkin.vocabulary.ui.VocabularyTabScreen
import me.apomazkin.wordcard.WordCardScreen
import me.apomazkin.wordcard.deps.WordCardUseCase

@Stable
class MainUiDepsProvider(
    private val vocabularyUseCase: VocabularyUseCase,
    private val wordCardUseCase: WordCardUseCase,
    private val logger: LexemeLogger,
) : MainUiDeps {
    @Composable
    override fun VocabularyTab(
        openAddDict: () -> Unit,
        openWordCard: (wordId: Long) -> Unit,
    ) {
        VocabularyTabScreen(
            vocabularyUseCase = vocabularyUseCase,
            logger = logger,
            openAddDict = openAddDict,
            openWordCard = openWordCard,
        )
    }

    @Composable
    override fun WordCardScreenDep(
        wordId: Long,
        onBackPress: () -> Unit,
    ) {
        WordCardScreen(
            wordId = wordId,
            wordCardUseCase = wordCardUseCase,
            onBackPress = onBackPress,
        )
    }
}