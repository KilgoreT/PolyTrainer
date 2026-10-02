package me.apomazkin.polytrainer.route

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry
import me.apomazkin.main.SCREEN_TRANSITION_DURATION_MS

/**
 * Переходы root-графа.
 *
 * - Форма словаря (создание/редактирование) растёт из места вызова:
 *   проявляется и дорастает до полного размера поверх неподвижного экрана
 *   под ней; закрывается обратным движением. Точка роста — меню словаря
 *   в аппбаре (правый верх) при вызове с главного экрана, центр — из
 *   списка словарей.
 * - «Все словари» — шаг вглубь от главного экрана: въезжает справа, как
 *   вложенные экраны табов, обратно — влево.
 * - Остальное (сплэш, первичная настройка) — затухание по умолчанию.
 */
internal object RootTransitions {

    fun enter(scope: AnimatedContentTransitionScope<NavBackStackEntry>): EnterTransition =
        with(scope) {
            when {
                targetState.isForm() -> formIn(origin = initialState.formOrigin())
                // Форма уходит поверх — экран под ней уже на месте.
                initialState.isForm() -> EnterTransition.None
                isMainListStep() -> slideIntoContainer(listDirection(), slideSpec)
                else -> defaultFadeIn
            }
        }

    fun exit(scope: AnimatedContentTransitionScope<NavBackStackEntry>): ExitTransition =
        with(scope) {
            when {
                // Экран под растущей формой стоит, пока она не закроет его.
                targetState.isForm() -> ExitTransition.KeepUntilTransitionsFinished
                initialState.isForm() -> formOut(origin = targetState.formOrigin())
                isMainListStep() -> slideOutOfContainer(listDirection(), slideSpec)
                else -> defaultFadeOut
            }
        }

    private fun NavBackStackEntry.isForm(): Boolean =
        destination.route == RootPoint.DICTIONARY_CREATE.route

    private fun NavBackStackEntry.isMain(): Boolean =
        destination.route == MainPoint.MAIN.route

    private fun NavBackStackEntry.isList(): Boolean =
        destination.route == RootPoint.DICTIONARY_LIST.route

    /** Откуда вызвана форма: с главного экрана — меню словаря в аппбаре. */
    private fun NavBackStackEntry.formOrigin(): TransformOrigin =
        if (isMain()) APP_BAR_MENU_ORIGIN else TransformOrigin.Center

    private fun AnimatedContentTransitionScope<NavBackStackEntry>.isMainListStep(): Boolean =
        (initialState.isMain() && targetState.isList()) ||
            (initialState.isList() && targetState.isMain())

    /** В список — вперёд (въезд справа), на главный — назад (слева). */
    private fun AnimatedContentTransitionScope<NavBackStackEntry>.listDirection(): SlideDirection =
        if (targetState.isList()) SlideDirection.Left else SlideDirection.Right

    private fun formIn(origin: TransformOrigin): EnterTransition =
        fadeIn(tween(FORM_ENTER_MS, easing = EaseOutCubic)) +
            scaleIn(
                animationSpec = tween(FORM_ENTER_MS, easing = EaseOutCubic),
                initialScale = FORM_HIDDEN_SCALE,
                transformOrigin = origin,
            )

    private fun formOut(origin: TransformOrigin): ExitTransition =
        fadeOut(tween(FORM_EXIT_MS, easing = EaseInCubic)) +
            scaleOut(
                animationSpec = tween(FORM_EXIT_MS, easing = EaseInCubic),
                targetScale = FORM_HIDDEN_SCALE,
                transformOrigin = origin,
            )

    /** Появление формы. Закрытие короче — исчезающему меньше времени. */
    private const val FORM_ENTER_MS = 350
    private const val FORM_EXIT_MS = 250

    /** Размер формы в начале роста и в конце закрытия. */
    private const val FORM_HIDDEN_SCALE = 0.85f

    /** Меню словаря (флажок) — правый верхний угол аппбара. */
    private val APP_BAR_MENU_ORIGIN = TransformOrigin(pivotFractionX = 0.92f, pivotFractionY = 0.07f)

    private val slideSpec = tween<IntOffset>(
        durationMillis = SCREEN_TRANSITION_DURATION_MS,
        easing = EaseInOutCubic,
    )

    /** Как у NavHost по умолчанию. */
    private val defaultFadeIn = fadeIn(tween(DEFAULT_FADE_MS))
    private val defaultFadeOut = fadeOut(tween(DEFAULT_FADE_MS))
    private const val DEFAULT_FADE_MS = 700
}
