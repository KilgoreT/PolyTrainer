package me.apomazkin.scancandidates

import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.Mate
import io.github.kilgoret.mate.MateEffectHandler
import io.github.kilgoret.mate.MateObserver
import io.github.kilgoret.mate.NavigationEffect
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.mate.ErrorLoggingObserver
import me.apomazkin.scancandidates.deps.ScanCandidatesUseCase
import me.apomazkin.scancandidates.mate.DatasourceEffect
import me.apomazkin.scancandidates.mate.DatasourceEffectHandler
import me.apomazkin.scancandidates.mate.Msg
import me.apomazkin.scancandidates.mate.ScanCandidatesReducer
import me.apomazkin.scancandidates.mate.ScanCandidatesState

/**
 * ЕДИНСТВЕННОЕ место сборки цикла экрана лексем-кандидатов. Прод
 * ([ScanCandidatesViewModel]) и сценарные тесты зовут одну фабрику; тест лишь
 * подставляет порт, свой хендлер навигации и диспатчер. Первый эффект —
 * распознавание снимка [uri], как `LoadWord` у карточки слова. UI-эффектов у
 * экрана нет: сообщение со счётом показывает вкладка по результату навигации.
 */
object ScanCandidatesAssembly {
    fun create(
        useCase: ScanCandidatesUseCase,
        logger: LexemeLogger,
        navigationHandler: MateEffectHandler<Nothing, NavigationEffect>,
        uri: String,
        coroutineScope: CoroutineScope,
        io: CoroutineDispatcher = Dispatchers.IO,
        observers: List<MateObserver<Any?, Any?, Effect>> = emptyList(),
    ): Mate<ScanCandidatesState, Msg, Effect> = Mate(
        initState = ScanCandidatesState(uri = uri),
        initEffects = setOf(DatasourceEffect.RecognizeText(uri)),
        coroutineScope = coroutineScope,
        reducer = ScanCandidatesReducer(logger),
        effectHandlers = listOf(
            DatasourceEffectHandler(useCase, logger, io),
            navigationHandler,
        ),
        observers = observers + ErrorLoggingObserver(logger, LogTags.SCAN),
    )
}
