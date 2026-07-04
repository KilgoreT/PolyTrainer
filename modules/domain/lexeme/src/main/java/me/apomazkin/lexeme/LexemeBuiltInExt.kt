package me.apomazkin.lexeme

/**
 * Built-in lookup — единый источник истины для доступа к built-in компонентам
 * лексемы. Type-safe через enum, никаких магических строк.
 */
fun Lexeme.builtIn(key: BuiltInComponent): ComponentValue? =
    components.firstOrNull { it.type.systemKey == key }
