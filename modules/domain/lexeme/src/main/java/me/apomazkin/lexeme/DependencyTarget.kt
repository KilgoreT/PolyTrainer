package me.apomazkin.lexeme

/**
 * IS486: цель зависимости компонента — узел, при активности которого компонент доступен.
 *
 * - [Lexeme] — сама лексема: доступен у оформленной лексемы (для ядер — всегда).
 * - [Component] — другой компонент: доступен, когда у того есть значение.
 * - [Option] — опция CHOICE-компонента: доступен при конкретном выборе.
 */
sealed interface DependencyTarget {
    data object Lexeme : DependencyTarget
    data class Component(val typeId: ComponentTypeId) : DependencyTarget
    data class Option(val optionId: Long) : DependencyTarget
}
