package me.apomazkin.per_dictionary_components.mate

import androidx.compose.runtime.Stable
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.DependencyTarget
import me.apomazkin.lexeme.DeletionImpact
import me.apomazkin.lexeme.NameError
import me.apomazkin.lexeme.PerDictionarySnapshot
import me.apomazkin.lexeme.Scope

/**
 * State для `PerDictionaryComponentsScreen` (IS481 scoped CRUD view —
 * global ∪ per-dict для конкретного словаря). Зеркально `ComponentsManagerScreenState`
 * с двумя отличиями:
 *
 * - init-параметр `dictionaryId` (assisted-инжект в ViewModel) хранится в state
 *   для reducer'а (`OpenCreateDialog` использует его для preselect scope).
 * - `dictionaryName` приходит с первым snapshot'ом, отображается в header.
 *
 * Все [F123/F124/F127/F132/F136/F138/F140] инварианты с самого начала (iter 3 retrofit).
 *
 * Invariants:
 * - `[shape]` createDialog/deleteConfirm/editDialog: одновременно ≤1 диалог
 *   (enforced в Reducer mutual-exclusion в Open*Dialog ветках — F138).
 * - `[shape]` is*ing == true → соответствующий dialog != null (race fallback допускает временно null).
 * - `[transition]` ConfirmDelete пока isDeleting=true → ignored.
 * - `[correlation]` *Result Msg несут epochId; reducer применяет только если активный
 *   dialog.epochId совпадает (stale results discarded).
 */
@Stable
data class PerDictionaryComponentsScreenState(
    // ===== Init context =====
    val dictionaryId: Long,
    val dictionaryName: String? = null,

    // ===== Loaded data =====
    val items: List<PerDictRow>? = null,

    // ===== UI flags (explicit) =====
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val isDeleting: Boolean = false,
    /** Phase 2 (IS481): in-flight edit operation. */
    val isEditing: Boolean = false,
    /** IS486: in-flight переключения рубильника enabled (guard от double-tap по typeId). */
    val pendingEnabledToggles: Set<ComponentTypeId> = emptySet(),

    // ===== Dialogs =====
    val createDialog: CreateDialogState? = null,
    val deleteConfirm: DeleteConfirmState? = null,
    /** Phase 2 (IS481): edit dialog state. */
    val editDialog: EditDialogState? = null,

    // ===== Snackbar (F123) =====
    val snackbarState: SnackbarState? = null,

    // ===== Epoch counter (F124/F136 correlation) =====
    val nextEpoch: Long = 0L,
)

@Stable
data class PerDictRow(
    val typeId: ComponentTypeId,
    val name: String,
    val template: ComponentTemplate,
    val isMultiple: Boolean,
    /** `true` если type — global (dictionaryId == null), иначе per-dict для этого словаря. */
    val isGlobal: Boolean,
    val valueCount: Int,
    // ===== IS486: иерархия/рубильник (spec §4–§6) =====
    /** system_key builtin-компонента; null — user-defined. Builtin: только свитч, без Edit/Delete. */
    val systemKey: String? = null,
    /** Ядро словаря (флаг у зависимых от лексемы, spec §7.7). */
    val core: Boolean = false,
    /** Мягкий рубильник (spec §6). */
    val enabled: Boolean = true,
    /** Цель зависимости (для Edit-диалога и меток). */
    val dependsOn: DependencyTarget = DependencyTarget.Lexeme,
    /** Вычисляемо: цель мертва (removed) — компонент «Не работает» (spec §7.6). */
    val degraded: Boolean = false,
    /** Живые опции CHOICE-компонента (пусто для остальных шаблонов). */
    val options: List<OptionRow> = emptyList(),
)

/** IS486: опция CHOICE в списке/диалогах. [display] — label ?: ресурс по systemKey (UI). */
@Stable
data class OptionRow(
    val optionId: Long,
    val systemKey: String?,
    val label: String?,
    val position: Int,
)

@Stable
data class CreateDialogState(
    val epochId: Long,
    val name: String = "",
    val template: ComponentTemplate = ComponentTemplate.TEXT,
    val isMultiple: Boolean = false,
    /**
     * Init-инициализируется текущим dictId через `Scope.PerDictionaries(listOf(dictId))`
     * в Reducer ветке `Msg.OpenCreateDialog` (preselect; см. `business_design_tree.md` #47).
     */
    val scope: Scope,
    val nameError: NameError? = null,
    // ===== IS486: пикер цели (В1) + варианты CHOICE (В2) =====
    /** Цель зависимости. [DependencyTarget.Lexeme] = «Самостоятельный». */
    val target: DependencyTarget = DependencyTarget.Lexeme,
    /** Галка «Ядро» — доступна только при цели-лексеме; иначе форсится false. */
    val core: Boolean = true,
    /** Черновики вариантов CHOICE (текст-поля). Для не-CHOICE игнорируются. */
    val optionDrafts: List<String> = emptyList(),
    /** Валидация UI: CHOICE без единого непустого варианта (spec: UI требует ≥1). */
    val optionsError: Boolean = false,
)

@Stable
data class DeleteConfirmState(
    val epochId: Long,
    val typeId: ComponentTypeId,
    val name: String,
    val impact: DeletionImpact? = null,
    val isLoadingImpact: Boolean = false,
)

/**
 * Edit dialog state (IS481 phase 2). Parity с Manager-вариантом — структурно
 * дублируется (UI sub-flow позже выносит в shared widget module).
 */
@Stable
data class EditDialogState(
    val epochId: Long,
    val typeId: ComponentTypeId,
    val originalName: String,
    val originalTemplate: ComponentTemplate,
    val originalIsMultiple: Boolean,
    val name: String,
    val template: ComponentTemplate,
    val isMultiple: Boolean,
    val nameError: EditNameError? = null,
    val impactedLexemesPreview: ImpactedLexemesPreview? = null,
    // ===== IS486: пикер цели (В1) + варианты CHOICE (В2) =====
    val originalTarget: DependencyTarget = DependencyTarget.Lexeme,
    val originalCore: Boolean = true,
    val target: DependencyTarget = DependencyTarget.Lexeme,
    val core: Boolean = true,
    /**
     * Существующие опции CHOICE ([EditOptionRow]): rename применяется батчем на
     * Submit; удаление — немедленно через [optionDeleteConfirm] (каскад слишком
     * серьёзен для батча). Новые опции — [newOptionDrafts], add на Submit.
     */
    val existingOptions: List<EditOptionRow> = emptyList(),
    val newOptionDrafts: List<String> = emptyList(),
    /** Вложенный конфирм удаления опции (поверх Edit-диалога, не мьютекс с ним). */
    val optionDeleteConfirm: OptionDeleteConfirmState? = null,
    /** In-flight немедленное удаление опции. */
    val isDeletingOption: Boolean = false,
    /**
     * IS486 умный сброс (решение 2026-07-21): конфирм перепривязки — обязателен
     * при любой смене цели/ядра ПЕРЕД применением; показывает «безопасно» либо
     * «будет скрыто N значений».
     */
    val rebindConfirm: RebindConfirmState? = null,
)

/** IS486: confirm-состояние перепривязки (impact = dry-run умного сброса). */
@Stable
data class RebindConfirmState(
    val impact: DeletionImpact? = null,
    val isLoadingImpact: Boolean = false,
)

/** IS486: существующая опция в Edit-диалоге; [label] — редактируемый текст. */
@Stable
data class EditOptionRow(
    val optionId: Long,
    val systemKey: String?,
    val originalLabel: String?,
    val label: String,
)

/** IS486: confirm-состояние удаления существующей опции (В2 — с impact). */
@Stable
data class OptionDeleteConfirmState(
    val optionId: Long,
    val label: String,
    val impact: DeletionImpact? = null,
    val isLoadingImpact: Boolean = false,
)

sealed interface EditNameError {
    data object NameEmpty : EditNameError
    data object SameScopeCollision : EditNameError
    data object CrossScopeCollision : EditNameError
}

sealed interface ImpactedLexemesPreview {
    val impactedLexemeIds: List<Long>

    data class InlineOnly(override val impactedLexemeIds: List<Long>) : ImpactedLexemesPreview

    data class InlineWithDrillIn(
        override val impactedLexemeIds: List<Long>,
        val inlineIds: List<Long>,
    ) : ImpactedLexemesPreview
}

/**
 * Snackbar payload (F123). Nullable on state — отсутствует когда нет активного
 * snackbar'а. UI отрисует SnackbarHost reading state.
 */
@Stable
data class SnackbarState(val text: String)

/** Computed selector. Loaded и пустой ⇒ показ empty state. */
val PerDictionaryComponentsScreenState.isEmpty: Boolean
    get() = items?.isEmpty() == true && !isLoading

/**
 * IS486 (девайс-фидбек C2, решение 2026-07-21): проверка цикла для кандидата цели —
 * подъём от кандидата по цепочке предков (опция резолвится во владельца); цикл =
 * дошли до самого редактируемого компонента [selfId]. Используется и reducer'ом
 * (подстраховочный guard), и пикером (дизейбл цикловых целей с подписью).
 * `visited` — защита от испорченных данных.
 */
internal fun createsCycle(
    items: List<PerDictRow>,
    selfId: ComponentTypeId,
    target: DependencyTarget,
): Boolean {
    val byId = items.associateBy { it.typeId }
    val optionOwner = items
        .flatMap { row -> row.options.map { it.optionId to row.typeId } }
        .toMap()
    var current = when (target) {
        is DependencyTarget.Component -> target.typeId
        is DependencyTarget.Option -> optionOwner[target.optionId]
        DependencyTarget.Lexeme -> null
    }
    val visited = mutableSetOf<ComponentTypeId>()
    while (current != null) {
        if (current == selfId) return true
        if (!visited.add(current)) return false
        current = when (val d = byId[current]?.dependsOn) {
            is DependencyTarget.Component -> d.typeId
            is DependencyTarget.Option -> optionOwner[d.optionId]
            else -> null
        }
    }
    return false
}

/**
 * Маппер snapshot → UI rows. Используется в Reducer на `Msg.ItemsLoaded`.
 *
 * - `isGlobal` infer: dictionaryId == null → true, else false.
 * - `valueCount` берётся из `snapshot.valueCountByType` (fallback 0).
 * - IS486 `degraded` вычисляемо (spec §7.6): снапшот содержит только живые типы
 *   и живые опции — цель, отсутствующая среди них, мертва.
 */
internal fun PerDictionarySnapshot.toPerDictRows(): List<PerDictRow> {
    val aliveTypeIds = types.map { it.id }.toSet()
    val aliveOptionIds = optionsByType.values.flatten().map { it.id }.toSet()
    return types.map { t ->
        val degraded = when (val target = t.dependsOn) {
            DependencyTarget.Lexeme -> false
            is DependencyTarget.Component -> target.typeId !in aliveTypeIds
            is DependencyTarget.Option -> target.optionId !in aliveOptionIds
        }
        PerDictRow(
            typeId = t.id,
            name = t.name.orEmpty(),
            template = t.template,
            isMultiple = t.isMultiple,
            isGlobal = t.dictionaryId == null,
            valueCount = valueCountByType[t.id] ?: 0,
            systemKey = t.systemKey?.key,
            core = t.core,
            enabled = t.enabled,
            dependsOn = t.dependsOn,
            degraded = degraded,
            options = optionsByType[t.id].orEmpty().map { o ->
                OptionRow(
                    optionId = o.id,
                    systemKey = o.systemKey,
                    label = o.label,
                    position = o.position,
                )
            },
        )
    }
}
