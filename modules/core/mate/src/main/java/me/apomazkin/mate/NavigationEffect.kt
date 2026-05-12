package me.apomazkin.mate

/**
 * Base navigation effects for TEA architecture.
 * Modules extend this interface for screen-specific navigation.
 */
interface NavigationEffect : Effect {
    /** Navigate back (close current screen) */
    data object Back : NavigationEffect
}
