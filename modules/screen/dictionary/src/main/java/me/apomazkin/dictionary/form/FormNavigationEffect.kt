package me.apomazkin.dictionary.form

import me.apomazkin.mate.NavigationEffect

/**
 * DictionaryForm — только базовый Back, специфичных эффектов нет.
 * Sealed interface оставлен пустым для единообразия паттерна.
 */
sealed interface FormNavigationEffect : NavigationEffect
