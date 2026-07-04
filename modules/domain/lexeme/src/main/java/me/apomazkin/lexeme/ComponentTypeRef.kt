package me.apomazkin.lexeme

/**
 * Sealed ссылка на тип компонента по stable identity. Используется в quiz config —
 * каждый ref резолвится в конкретный `ComponentValue` лексемы при сборке quiz item.
 *
 * - `BuiltIn(key)` — стабильная ссылка через enum-key (`BuiltInComponent.key`).
 * - `UserDefined(name)` — ссылка через имя; уникальность гарантирована
 *   `UNIQUE(dictionary_id, name)` в `component_types`.
 *
 * TODO: вынести в `modules/domain/quiz` в рамках backlog-фичи «Quiz config UX»
 * (AGG-10). Сейчас живёт в lexeme domain как trade-off — не плодим второй
 * domain модуль ради 2 типов.
 */
sealed interface ComponentTypeRef {
    @JvmInline
    value class BuiltIn(val key: BuiltInComponent) : ComponentTypeRef

    @JvmInline
    value class UserDefined(val name: String) : ComponentTypeRef
}

/**
 * Stable identity mapping `ComponentType` → `ComponentTypeRef`.
 *
 * - `systemKey` non-null → `BuiltIn(systemKey)` (name игнорируется — built-in
 *   identity по enum-key, не name override).
 * - `systemKey` null → `UserDefined(name!!)`. Invariant: user-defined ComponentType
 *   обязан иметь non-null `name` (DB-level CHECK через mate-shim в `ComponentTypeDb`).
 *   Violation → `IllegalStateException`.
 */
fun ComponentType.toRef(): ComponentTypeRef = when (val sk = systemKey) {
    null -> ComponentTypeRef.UserDefined(name ?: error("user-defined ComponentType without name"))
    else -> ComponentTypeRef.BuiltIn(sk)
}
