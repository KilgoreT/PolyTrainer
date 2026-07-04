package me.apomazkin.wordcard.mate

import me.apomazkin.lexeme.ComponentValueId

/**
 * Identity для component_value в state. Pristine — pre-INSERT черновик, Saved — после
 * квитанции из БД. Flip Pristine → Saved делает reducer на `Msg.ComponentValueInserted`.
 * Sealed XOR (state-modeling.md «impossible states impossible»).
 */
sealed interface ComponentValueKey {
    @JvmInline
    value class Pristine(val pristineKey: Long) : ComponentValueKey

    @JvmInline
    value class Saved(val componentValueId: ComponentValueId) : ComponentValueKey
}
