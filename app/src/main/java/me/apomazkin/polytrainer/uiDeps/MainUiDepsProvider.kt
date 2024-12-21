package me.apomazkin.polytrainer.uiDeps

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import me.apomazkin.dictionarytab.deps.VocabularyUseCase
import me.apomazkin.dictionarytab.ui.VocabularyTabScreen
import me.apomazkin.main.MainUiDeps
import me.apomazkin.quiztab.QuizTabScreen
import me.apomazkin.settingstab.SettingsTabScreen
import me.apomazkin.stattab.StatisticTabScreen
import me.apomazkin.ui.logger.LexemeLogger
import me.apomazkin.wordcard.WordCardScreen
import me.apomazkin.wordcard.deps.WordCardUseCase

@Stable
class MainUiDepsProvider(
    private val vocabularyUseCase: VocabularyUseCase,
    private val wordCardUseCase: WordCardUseCase,
    private val logger: LexemeLogger,
) : MainUiDeps {
    @Composable
    override fun VocabularyTabDep(
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

    @Composable
    override fun QuizTabScreenDep() {
        QuizTabScreen()
    }

    @Composable
    override fun StatisticTabScreenDep() {
        StatisticTabScreen()
    }

    @Composable
    override fun SettingsTabScreenDep() {
        SettingsTabScreen()
    }
}