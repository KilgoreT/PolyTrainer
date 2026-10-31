package me.apomazkin.wordstab.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import me.apomazkin.wordstab.deps.ScanLauncherHolder
import me.apomazkin.wordstab.deps.WordsTabUseCase
import me.apomazkin.wordstab.logic.WordsTabState
import me.apomazkin.wordstab.logic.Msg
import me.apomazkin.logger.LexemeLogger
import io.github.kilgoret.mate.MateStore
import io.github.kilgoret.mate.navigation.MateNavigationHandler

/**
 * VM вкладки «Слова»: тонкая обёртка над [WordsTabAssembly] — сборка
 * раннера живёт там (общая с харнесом), VM даёт только viewModelScope
 * (он же pagingScope) и продовые зависимости.
 *
 * [scanLauncher] живёт здесь, а не в композиции: VM переживает пересоздание
 * Activity, поэтому композиция привязывает к нему свой PhotoPicker заново.
 * [logger] нужен композиции — PhotoPicker пишет в него провал закрепления
 * доступа к файлу галереи.
 */
class WordsTabViewModel @AssistedInject constructor(
    useCase: WordsTabUseCase,
    internal val logger: LexemeLogger,
    navigationHandler: MateNavigationHandler,
) : ViewModel(), MateStore<WordsTabState, Msg> {

    internal val scanLauncher = ScanLauncherHolder()

    private val stateHolder = WordsTabAssembly.create(
        useCase = useCase,
        logger = logger,
        navigationHandler = navigationHandler,
        pagingScope = viewModelScope,
        coroutineScope = viewModelScope,
        scanLauncher = scanLauncher,
    )

    override val state: StateFlow<WordsTabState>
        get() = stateHolder.state

    override fun accept(message: Msg) = stateHolder.accept(message)

    @AssistedFactory
    interface Factory {
        fun create(): WordsTabViewModel
    }
}
