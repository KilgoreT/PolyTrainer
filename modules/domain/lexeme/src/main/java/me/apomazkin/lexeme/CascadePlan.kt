package me.apomazkin.lexeme

/**
 * IS486: каскад-планировщик (spec §9.2, §12) — чистое ядро каскад-модуля.
 *
 * Вход: граф компонентов словаря ([ComponentGraph], ссылки включая removed) +
 * живые значения лексем + событие. Выход: множество id значений, которые
 * обязаны быть сброшены (soft-delete) — включая начальные жертвы события.
 *
 * Закон (spec §9.2): сбрасывается то, чей предок деактивировался — по каждой
 * лексеме отдельно, до неподвижной точки. Активность цели у лексемы:
 * - цель-компонент активна ⇔ у лексемы есть живое значение цели (мульти —
 *   хоть одно: цель гаснет только со смертью последнего значения);
 * - цель-опция активна ⇔ у лексемы есть живое значение с этой опцией;
 * - цель-лексема — в фазе 1 всегда активна (критерий оформленности и
 *   каскад черновика — фаза 2, карточка слова).
 *
 * Dry-run = сам план: исполнитель применяет его одним UPDATE, превью —
 * считает счётчики по нему же.
 */

/** Живое значение лексемы для планировщика (проекция component_values). */
data class CascadeValue(
    val id: Long,
    val lexemeId: Long,
    val typeId: ComponentTypeId,
    val optionId: Long? = null,
)

/** Событие, запускающее каскад. */
sealed interface CascadeEvent {
    /** Soft-delete компонента: умирают его значения + всё зависимое вниз. */
    data class ComponentRemoved(val typeId: ComponentTypeId) : CascadeEvent

    /** Удаление опции (К4/К5): умирают значения-выборы опции + зависимое от неё вниз. */
    data class OptionRemoved(val optionId: Long) : CascadeEvent

    /** Смерть одного значения (фаза 2: удаление/смена значения в карточке). */
    data class ValueRemoved(val valueId: Long) : CascadeEvent

    /**
     * IS486 (умный сброс, решение 2026-07-21): перепривязка цели компонента.
     * Начальных жертв НЕТ — граф подаётся уже С НОВОЙ целью узла, и fixpoint
     * сам гасит значения, у которых новое условие в их лексеме не выполнено
     * (+ их поддеревья). Значения в лексемах с выполненным условием живут.
     */
    data class TargetRebound(val typeId: ComponentTypeId) : CascadeEvent
}

/**
 * План каскадного сброса: id значений к soft-delete (включая начальные жертвы).
 * Fixpoint по каждой лексеме: значение умирает, когда цель его типа перестала
 * быть активной среди оставшихся живых значений этой лексемы.
 */
fun ComponentGraph.planCascade(
    values: List<CascadeValue>,
    event: CascadeEvent,
): Set<Long> {
    val killed = mutableSetOf<Long>()

    // Начальные жертвы события.
    values.forEach { value ->
        val isInitialVictim = when (event) {
            is CascadeEvent.ComponentRemoved -> value.typeId == event.typeId
            is CascadeEvent.OptionRemoved -> value.optionId == event.optionId
            is CascadeEvent.ValueRemoved -> value.id == event.valueId
            is CascadeEvent.TargetRebound -> false // умный сброс: только fixpoint
        }
        if (isInitialVictim) killed += value.id
    }

    // Fixpoint: цель значения активна среди живых значений его лексемы?
    val byLexeme = values.groupBy { it.lexemeId }
    var changed = true
    while (changed) {
        changed = false
        byLexeme.forEach { (_, lexemeValues) ->
            val alive = lexemeValues.filter { it.id !in killed }
            alive.forEach { value ->
                val type = typeById(value.typeId) ?: return@forEach
                val targetActive = when (val target = type.dependsOn) {
                    DependencyTarget.Lexeme -> true // фаза 1: оформленность — фаза 2
                    is DependencyTarget.Component ->
                        alive.any { it.typeId == target.typeId && it.id != value.id }

                    is DependencyTarget.Option ->
                        alive.any { it.optionId == target.optionId && it.id != value.id }
                }
                // Событие OptionRemoved деактивирует опцию и без значений:
                // зависимые от удалённой опции умирают даже если выбор уже мёртв.
                val targetKilledByEvent = event is CascadeEvent.OptionRemoved &&
                    (type.dependsOn as? DependencyTarget.Option)?.optionId == event.optionId
                if (!targetActive || targetKilledByEvent) {
                    killed += value.id
                    changed = true
                }
            }
        }
    }
    return killed
}
