package me.apomazkin.per_dictionary_components.mate

import me.apomazkin.mate.NavigationEffect

/**
 * Navigation effects для `PerDictionaryComponentsScreen`.
 *
 * Своих переходов нет: Back уже наследуется из базового [NavigationEffect.Back]
 * (`:modules:core:mate`). Drill-в global ComponentsManagerScreen не предусмотрен
 * (ui_placement.md § Cross-flow — экраны независимы). Sealed-каркас оставлен
 * для расширения в будущих фичах.
 */
sealed interface PerDictionaryComponentsNavigationEffect : NavigationEffect
