package me.apomazkin.dictionary.form

import io.github.kilgoret.mate.NavigationEffect

/**
 * DictionaryForm — только базовый Back, специфичных эффектов нет.
 * Sealed interface оставлен пустым для единообразия паттерна.
 */
sealed interface FormNavigationEffect : NavigationEffect
