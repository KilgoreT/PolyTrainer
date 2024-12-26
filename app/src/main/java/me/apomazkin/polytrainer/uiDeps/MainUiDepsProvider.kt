package me.apomazkin.polytrainer.uiDeps

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import me.apomazkin.dictionarytab.deps.DictionaryTabUseCase
import me.apomazkin.dictionarytab.ui.DictionaryTabScreen
import me.apomazkin.main.MainUiDeps
import me.apomazkin.quiztab.QuizTabScreen
import me.apomazkin.quiztab.deps.QuizTabUseCase
import me.apomazkin.settingstab.SettingsTabScreen
import me.apomazkin.stattab.StatisticTabScreen
import me.apomazkin.ui.logger.LexemeLogger
import me.apomazkin.wordcard.WordCardScreen
import me.apomazkin.wordcard.deps.WordCardUseCase

@Stable
class MainUiDepsProvider(
    private val dictionaryTabUseCase: DictionaryTabUseCase,
    private val wordCardUseCase: WordCardUseCase,
    private val quizTabUseCase: QuizTabUseCase,
    private val logger: LexemeLogger,
) : MainUiDeps {
    @Composable
    override fun VocabularyTabDep(
        openAddDict: () -> Unit,
        openWordCard: (wordId: Long) -> Unit,
    ) {
        DictionaryTabScreen(
            dictionaryTabUseCase = dictionaryTabUseCase,
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
    override fun QuizTabScreenDep(
        openAddDict: () -> Unit,
    ) {
        QuizTabScreen(
            quizTabUseCase = quizTabUseCase,
            logger = logger,
            openAddDict = openAddDict,
        )
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