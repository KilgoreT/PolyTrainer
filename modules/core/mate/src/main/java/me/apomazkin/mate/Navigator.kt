package me.apomazkin.mate

/**
 * Базовый интерфейс навигатора для NavigationEffectHandler.
 * Содержит только Back — операцию доступную всем экранам.
 *
 * Per-screen Navigator наследует этот интерфейс и добавляет специфичные методы:
 * - openX(...) — навигация на другой экран
 * - exit() — закрытие приложения (только для root экранов)
 */
interface Navigator {
    fun back()
}
