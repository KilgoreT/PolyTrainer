package me.apomazkin.polytrainer.scenario

import io.github.kilgoret.mate.apptest.HarnessContext
import io.github.kilgoret.mate.apptest.RunnerSlot
import io.github.kilgoret.mate.apptest.ScreenNode
import io.github.kilgoret.mate.apptest.ScreenRegistry
import io.github.kilgoret.mate.apptest.screenRegistry
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlin.coroutines.ContinuationInterceptor
import me.apomazkin.groupstab.deps.GroupsTabUseCase
import me.apomazkin.groupstab.ui.GroupsTabAssembly
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.polytrainer.navigation.AppScreen
import me.apomazkin.wordcard.WordCardAssembly
import me.apomazkin.wordcard.deps.WordCardUseCase
import me.apomazkin.wordstab.deps.WordsTabUseCase
import me.apomazkin.wordstab.ui.WordsTabAssembly
import me.apomazkin.groupstab.logic.Msg as GroupsMsg
import me.apomazkin.wordcard.mate.Msg as WordCardMsg
import me.apomazkin.wordstab.logic.Msg as WordsMsg

/**
 * Registry пилотных сценариев: те же Assembly, что зовёт прод, — со
 * стабами use case'ов вместо базы. Узел Main — композиция раннеров
 * words + groups (как на живом экране: обе вкладки живут
 * одновременно и слушают словарь сами), карточка — плоский узел.
 */
/** Тестовый диспатчер сценария — блокирующие операции идут в виртуальном времени. */
private val HarnessContext.io: CoroutineDispatcher
    get() = scope.coroutineContext[ContinuationInterceptor] as CoroutineDispatcher

fun pilotRegistry(
    wordsUseCase: WordsTabUseCase,
    groupsUseCase: GroupsTabUseCase,
    wordCardUseCase: WordCardUseCase,
): ScreenRegistry {
    val logger = mockk<LexemeLogger>(relaxed = true)
    return screenRegistry {
        on<AppScreen.Main> { screen, context ->
            ScreenNode(
                screen = screen,
                runners = listOf(
                    RunnerSlot(
                        name = "words",
                        messageFamily = WordsMsg::class,
                        holder = WordsTabAssembly.create(
                            useCase = wordsUseCase,
                            logger = logger,
                            navigationHandler = context.navigationHandler,
                            pagingScope = context.scope,
                            io = context.io,
                            coroutineScope = context.scope,
                            observers = listOf(context.observer),
                        ),
                    ),
                    RunnerSlot(
                        name = "groups",
                        messageFamily = GroupsMsg::class,
                        holder = GroupsTabAssembly.create(
                            useCase = groupsUseCase,
                            logger = logger,
                            navigationHandler = context.navigationHandler,
                            io = context.io,
                            coroutineScope = context.scope,
                            observers = listOf(context.observer),
                        ),
                    ),
                ),
            )
        }
        on<AppScreen.WordCard> { screen, context ->
            ScreenNode(
                screen = screen,
                runners = listOf(
                    RunnerSlot(
                        name = "wordcard",
                        messageFamily = WordCardMsg::class,
                        holder = WordCardAssembly.create(
                            useCase = wordCardUseCase,
                            logger = logger,
                            navigationHandler = context.navigationHandler,
                            wordId = screen.wordId,
                            uiHost = mockk(relaxed = true),
                            coroutineScope = context.scope,
                            observers = listOf(context.observer),
                        ),
                    ),
                ),
            )
        }
    }
}
