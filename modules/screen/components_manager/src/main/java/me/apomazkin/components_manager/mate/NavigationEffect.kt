package me.apomazkin.components_manager.mate

import me.apomazkin.mate.NavigationEffect

/**
 * Navigation effects для `ComponentsManagerScreen`.
 *
 * Своих переходов нет: Back уже наследуется из базового [NavigationEffect.Back]
 * (`:modules:core:mate`). Drill-in в per-dictionary view не предусмотрен
 * (ui_placement.md § Cross-flow — экраны независимы). Sealed-каркас оставлен
 * для расширения в будущих фичах.
 */
sealed interface ComponentsManagerNavigationEffect : NavigationEffect
