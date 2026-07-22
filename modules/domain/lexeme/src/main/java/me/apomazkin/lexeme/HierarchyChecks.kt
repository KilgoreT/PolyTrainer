package me.apomazkin.lexeme

/**
 * IS486: чистые проверки иерархии компонентов (spec §7–§8).
 *
 * Все функции — синхронные и детерминированные: граф передаётся целиком,
 * IO нет. Room-слой лишь вызывает их до записи (UseCase-валидация,
 * дом-стиль M13 — Room не умеет CHECK).
 */

/**
 * Граф компонентов одного словаря (для проверок фазы 1 достаточно словаря —
 * кросс-словарные зависимости запрещены §7.7).
 *
 * [types] — компоненты (включая soft-deleted: ссылки не зануляются, граф — все ссылки).
 * [options] — опции CHOICE-компонентов (включая soft-deleted).
 */
data class ComponentGraph(
    val types: List<ComponentType>,
    val options: List<ComponentOption> = emptyList(),
) {
    internal fun typeById(id: ComponentTypeId): ComponentType? =
        types.firstOrNull { it.id == id }

    internal fun optionById(id: Long): ComponentOption? =
        options.firstOrNull { it.id == id }

    /**
     * Компонент-владелец цели: для опции — её владелец, для компонента — он сам,
     * для лексемы — null (супер-корень). Отсутствующая в графе опция → null
     * (битая ссылка — не участвует в подъёме).
     */
    internal fun ownerOf(target: DependencyTarget): ComponentTypeId? = when (target) {
        DependencyTarget.Lexeme -> null
        is DependencyTarget.Component -> target.typeId
        is DependencyTarget.Option -> optionById(target.optionId)?.componentTypeId
    }
}

/** Результат валидации создания/редактирования компонента (spec §7.1). */
sealed interface SetupCheck {
    data object Ok : SetupCheck

    /** Цель отсутствует/удалена/чужого словаря; core при не-лексемной цели. */
    data object InvalidTarget : SetupCheck

    /** isMultiple = true для шаблона CHOICE (spec §7.5). */
    data object MultiForbiddenForChoice : SetupCheck
}

/**
 * Валидация параметров компонента при создании и редактировании (spec §7.1, §7.5, §7.7):
 * цель обязана существовать и быть живой; core валиден только при цели-лексеме;
 * цель — только внутри словаря [dictionaryId]; CHOICE не мульти.
 */
fun ComponentGraph.checkSetup(
    template: ComponentTemplate,
    isMultiple: Boolean,
    core: Boolean,
    dependsOn: DependencyTarget,
    dictionaryId: Long?,
): SetupCheck {
    if (template == ComponentTemplate.CHOICE && isMultiple) {
        return SetupCheck.MultiForbiddenForChoice
    }
    return when (dependsOn) {
        DependencyTarget.Lexeme -> SetupCheck.Ok

        is DependencyTarget.Component -> {
            val target = typeById(dependsOn.typeId)
            when {
                core -> SetupCheck.InvalidTarget
                target == null -> SetupCheck.InvalidTarget
                target.removedAt != null -> SetupCheck.InvalidTarget
                target.dictionaryId != dictionaryId -> SetupCheck.InvalidTarget
                else -> SetupCheck.Ok
            }
        }

        is DependencyTarget.Option -> {
            val option = optionById(dependsOn.optionId)
            val owner = option?.let { typeById(it.componentTypeId) }
            when {
                core -> SetupCheck.InvalidTarget
                option == null || owner == null -> SetupCheck.InvalidTarget
                option.removedAt != null -> SetupCheck.InvalidTarget
                owner.removedAt != null -> SetupCheck.InvalidTarget
                owner.dictionaryId != dictionaryId -> SetupCheck.InvalidTarget
                else -> SetupCheck.Ok
            }
        }
    }
}

/** Результат проверки ацикличности (spec §8). */
sealed interface AcyclicityCheck {
    data object Ok : AcyclicityCheck
    data object CycleDetected : AcyclicityCheck
}

/**
 * Проверка назначения компоненту [componentId] цели [newTarget] (spec §8):
 * нормализация опции в компонент-владельца, подъём по цепочке предков до лексемы.
 * Подъём идёт по ссылкам НЕЗАВИСИМО от removed-статуса узлов — мёртвое звено
 * может ожить restore'ом, кольцо через него тоже [AcyclicityCheck.CycleDetected].
 * Visited-защита: уже испорченный граф (готовое кольцо) не зацикливает проверку.
 */
fun ComponentGraph.checkAcyclic(
    componentId: ComponentTypeId,
    newTarget: DependencyTarget,
): AcyclicityCheck {
    val visited = mutableSetOf<ComponentTypeId>()
    var current: ComponentTypeId? = ownerOf(newTarget)
    while (current != null) {
        if (current == componentId) return AcyclicityCheck.CycleDetected
        if (!visited.add(current)) return AcyclicityCheck.CycleDetected
        current = typeById(current)?.let { ownerOf(it.dependsOn) }
    }
    return AcyclicityCheck.Ok
}

/**
 * Degraded-предикат (spec §6): компонент degraded ⇔ его цель мертва.
 * Цель-лексема всегда жива; компонент/опция мертвы при removed-статусе
 * или отсутствии в графе (битая ссылка).
 */
fun ComponentGraph.isDegraded(type: ComponentType): Boolean = when (val target = type.dependsOn) {
    DependencyTarget.Lexeme -> false
    is DependencyTarget.Component -> {
        val t = typeById(target.typeId)
        t == null || t.removedAt != null
    }
    is DependencyTarget.Option -> {
        val option = optionById(target.optionId)
        option == null || option.removedAt != null
    }
}

/** Результат проверки потери последнего включённого ядра (spec §7.8). */
sealed interface CoreLossCheck {
    data object Ok : CoreLossCheck
    data object LastEnabledCore : CoreLossCheck
}

/**
 * Проверка «[componentId] — последнее включённое ядро словаря?» (spec §7.8).
 * Единая для всех трёх путей потери: disable, soft-delete, перепривязка на не-лексему.
 * Считаются только живые (не removed) включённые ядра словаря; терять уже
 * выключенное/удалённое ядро — безопасно.
 */
fun ComponentGraph.checkCoreLoss(componentId: ComponentTypeId): CoreLossCheck {
    val target = typeById(componentId) ?: return CoreLossCheck.Ok
    val losable = target.core && target.enabled && target.removedAt == null
    if (!losable) return CoreLossCheck.Ok
    val otherEnabledCores = types.any {
        it.id != componentId &&
            it.dictionaryId == target.dictionaryId &&
            it.core && it.enabled && it.removedAt == null
    }
    return if (otherEnabledCores) CoreLossCheck.Ok else CoreLossCheck.LastEnabledCore
}

/**
 * Уникальность builtin «(ключ, словарь)» (spec §10): свободна ли пара
 * [key] + [dictionaryId] среди живых [ComponentGraph.types].
 * Замена дропнутому UNIQUE-индексу `system_key`.
 */
fun ComponentGraph.isBuiltInKeyAvailable(
    key: BuiltInComponent,
    dictionaryId: Long?,
): Boolean = types.none {
    it.systemKey == key && it.dictionaryId == dictionaryId && it.removedAt == null
}
