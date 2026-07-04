# Слой UI — план изменений

> **Гайды:** `ui-patterns.md` (stateless composable, `sendMessage`), `ui-primitives.md` (слоты, переиспользование примитивов — `SubentityChip`/`LexemeEditableText`), `theme-and-resources.md` (строки/цвета), `naming.md` (имена composable). UI-resolver лейбла (`componentLabelOf`) — в UI-слое, НЕ в domain (Clean).

Удаляется hardcoded translation/definition UI в WordCard. Вводятся унифицированные composable'ы поверх `LexemeState.components`. Translation рендерится как обычный chip с лейблом «Перевод» (resolver в UI слое).

Затрагиваемые файлы:
- `:modules:screen:wordcard/.../widget/lexeme/ComponentValueField.kt` — **NEW**.
- `:modules:screen:wordcard/.../widget/lexeme/ComponentChipsRow.kt` — **NEW**.
- `:modules:screen:wordcard/.../widget/lexeme/LexemeComponentsBlock.kt` — **NEW**.
- `:modules:screen:wordcard/.../widget/lexeme/ComponentLabel.kt` — **NEW** (resolver).
- `:modules:screen:wordcard/.../widget/lexeme/LexemeMeaningField.kt` — **DELETE**.
- `:modules:screen:wordcard/.../widget/lexeme/AddLexemeMeaningRow.kt` — **DELETE**.
- `:modules:screen:wordcard/.../widget/lexeme/SubentityChip.kt` — **KEEP** (используется ChipsRow'ом).
- `:modules:screen:wordcard/.../widget/lexeme/LexemeCard.kt` — **NO-OP** (slot-pattern, без правок).
- `:modules:screen:wordcard/.../widget/lexeme/DeleteLexemeButton.kt` — **NO-OP**.
- `:modules:screen:wordcard/.../WordCardScreen.kt` — **MODIFY** (lines 154-203 заменяются).
- `core-resources/src/main/res/values/strings.xml` (либо локально модульный strings.xml) — **MODIFY**.

---

## §1. ComponentLabel.kt (NEW)

Чистый UI-resolver. **A12-решение — лейбл резолвится из ЖИВОГО типа по `componentTypeId` (справочник `availableComponentTypes`), с FALLBACK на снимок `componentTypeRef`.** Связь в БД — по id, не по имени; значит переименование типа в Manager должно подхватываться у уже добавленных значений. Snapshot (`componentTypeRef`) остаётся ТОЛЬКО как fallback на **окно загрузки**: между `WordLoaded` (грузит значения) и приходом `ComponentTypesLoaded` справочник пуст, lookup по id не находит. Значения с удалённым типом сюда НЕ относятся — каскадный soft-delete + фильтр маппера их не грузят (см. 09 A9), orphan не существует.

```kotlin
package me.apomazkin.wordcard.widget.lexeme

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.apomazkin.core_resources.R
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentType
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.ComponentTypeRef

/**
 * Лейбл из ref (snapshot ИЛИ живой type.toRef()). Built-in TRANSLATION → @StringRes;
 * user-defined → name.
 */
@Composable
internal fun labelOfRef(ref: ComponentTypeRef): String = when (ref) {
    is ComponentTypeRef.BuiltIn ->
        if (ref.key == BuiltInComponent.TRANSLATION)
            stringResource(id = R.string.word_card_bottom_translation)
        else ref.key.name // forward-compat для будущих built-in
    is ComponentTypeRef.UserDefined -> ref.name
}

/**
 * A12: лейбл ЗНАЧЕНИЯ — приоритет живому типу из справочника (по id),
 * fallback на снимок ref. Rename типа в Manager → живой type обновится →
 * лейбл подхватится. Справочник ещё не загружен (окно между WordLoaded и
 * ComponentTypesLoaded) → snapshot ref. (Удалённого типа тут не бывает — его
 * значение не грузится, 09 A9.)
 * Поле рендерится ВСЕГДА (никакого `?: return` / скрытия — это и был баг F1).
 */
@Composable
internal fun componentValueLabel(
    componentTypeId: ComponentTypeId,
    snapshotRef: ComponentTypeRef,
    availableTypes: List<ComponentType>,
): String {
    val live = availableTypes.firstOrNull { it.id == componentTypeId }
    return labelOfRef(live?.toRef() ?: snapshotRef)
}

/** Лейбл для chip'а в ChipsRow — там всегда есть живой ComponentType. */
@Composable
internal fun componentLabelOf(type: ComponentType): String = labelOfRef(type.toRef())
```

> Проверить на imp форму `ComponentTypeRef` (`BuiltIn(key)` / `UserDefined(name)`), `toRef()` уже существует.

**Важно (стык с B1):** lookup по id используется ТОЛЬКО для лейбла и ТОЛЬКО с fallback — поле значения никогда не скрывается при отсутствии типа в справочнике (видимость в окне до прихода `ComponentTypesLoaded` держится через snapshot). Это сохраняет F1-фикс. `ComponentValueField` получает готовую строку лейбла (резолв делает `LexemeComponentsBlock`, у которого есть `availableComponentTypes`).

`SubentityChip(@StringRes labelRes, ...)` принимает только resource — для user-defined нужен либо overload с `String`, либо мы переключаем chip'ы на инлайн-`Surface { Text }`. Простое решение — добавить overload `SubentityChip(label: String, ...)` (NEW в `SubentityChip.kt`):

```kotlin
@Composable
internal fun SubentityChip(
    label: String,
    @DrawableRes iconRes: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) { /* тело идентично, но Text(text = label) */ }
```

В `ComponentChipsRow` / `ComponentValueField` вызывается перегрузка с `String label = componentLabelOf(type)`.

---

## §2. ComponentValueField.kt (NEW)

Прямой клон `LexemeMeaningField`. **Принимает готовый `label: String`** (резолв делает `LexemeComponentsBlock` через `componentValueLabel` — A12: живой тип по id + fallback на снимок). Поле рендерится ВСЕГДА (видимость до прихода `ComponentTypesLoaded` через snapshot fallback — F1/B1 сохранён).

```kotlin
package me.apomazkin.wordcard.widget.lexeme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.apomazkin.core_resources.R
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.ui.text.base.LexemeEditableText
import me.apomazkin.wordcard.mate.ComponentValueState

@Composable
internal fun ComponentValueField(
    state: ComponentValueState,
    label: String,                       // A12: резолвится снаружи (live type by id ?: snapshot)
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onOpenEditMode: () -> Unit,
    onCommitEdit: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SubentityChip(
            label = label,                                       // A12: живой лейбл, fallback snapshot
            iconRes = R.drawable.ic_close,
            enabled = enabled,
            onClick = onRemove,
        )
        LexemeEditableText(
            originValue = state.origin,
            changedValue = state.edited,
            isEditMode = state.isEdit,
            textColor = MaterialTheme.colorScheme.secondary,
            textStyle = LexemeStyle.BodyL,
            onTextChange = onValueChange,
            onOpenEditMode = onOpenEditMode,
            onFocusLost = { value ->
                // P1 (СОГЛАСОВАНО, финально): пустое поле + потеря фокуса = удаление записи.
                // Parity с переводом. Reducer различает pristine (LocalRemove) vs
                // saved (PessimisticRemove) через commitDecision() (03 §3.2).
                if (value.isEmpty()) onRemove() else onCommitEdit()
            },
        )
    }
}
```

Никаких generics, никаких branches. Поведение autosave (`onFocusLost`) — идентично `LexemeMeaningField` (parity F073).

**P1 (закреплено):** пустое значение при потере фокуса = удаление записи. Прежняя заметка brief «пустое значение остаётся» — **ОТМЕНЕНА**. Финально: пустое = удаляется (для saved — PessimisticRemove из БД; для pristine — LocalRemove).

**NavigateBack («назад» с несохранённым) — flush-on-back с лоадером:** при «назад» сначала коммитим открытую правку и ждём записи под **блокирующим лоадером** (`isExiting=true`), затем закрываем экран. Ошибка записи → экран НЕ закрываем, показываем снек, поле цело. Механика — маркеры `isCommitting` + пост-шаг редьюсера (03 §6.2.3): выход ровно когда снят последний `isCommitting`. Запись локальная (SQLite) → лоадер обычно мелькает доли секунды. UI: при `isExiting` рендерить лоадер поверх контента и игнорировать ввод. (Это L7.1/L2.8 в 09, без P-кода. Метка **P2** = «один активный редактор», 09 Решения #9.)

---

## §3. ComponentChipsRow.kt (NEW)

```kotlin
package me.apomazkin.wordcard.widget.lexeme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.apomazkin.core_resources.R
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentType
import me.apomazkin.lexeme.ComponentTypeId

/**
 * Ряд chip'ов «доступных» компонентов. Built-in TRANSLATION идёт первым.
 * Non-multi: если type уже в addedNonMultiTypeIds — chip скрыт.
 * Multi: всегда показан.
 *
 * P3: image-template типы ФИЛЬТРУЮТСЯ (пропускаем `template != ComponentTemplate.TEXT`).
 * TODO: Image template требует отдельного релиза (UI редактирования картинок
 *       не реализован). Когда появится image-input UI — снять фильтр.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ComponentChipsRow(
    availableTypes: List<ComponentType>,
    addedNonMultiTypeIds: Set<ComponentTypeId>,
    enabled: Boolean,
    onAddComponent: (ComponentTypeId) -> Unit,
) {
    val visible = availableTypes.filter { type ->
        // P3: только TEXT-template. Image (и будущие composite) пропускаем —
        // UI редактирования картинок ещё нет.
        type.template == ComponentTemplate.TEXT &&
            (type.isMulti || type.id !in addedNonMultiTypeIds)
    }
    if (visible.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(0.33f).align(Alignment.End),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
        )
        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            visible.forEach { type ->
                SubentityChip(
                    label = componentLabelOf(type),
                    iconRes = R.drawable.ic_add,
                    enabled = enabled,
                    onClick = { onAddComponent(type.id) },
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
    }
}
```

**P3 — Image template (`ComponentTemplate.IMAGE`) ФИЛЬТРУЕТСЯ.** Out of scope этой фичи (UI редактирования картинок не реализован). `ComponentChipsRow` пропускает любой type с `template != ComponentTemplate.TEXT` (см. фильтр выше + TODO в коде). Это надёжнее прежней посылки «IMAGE типы вообще не создаются» — фильтр работает даже если такой тип появится в БД. Backlog: «WordCard: показывать image-template chip когда появится image-input UI».

---

## §4. LexemeComponentsBlock.kt (NEW)

Orchestrator: список добавленных полей + chip-row.

```kotlin
package me.apomazkin.wordcard.widget.lexeme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.apomazkin.lexeme.ComponentType
import me.apomazkin.wordcard.mate.ComponentValueState
import me.apomazkin.wordcard.mate.LexemeState
import me.apomazkin.wordcard.mate.Msg

@Composable
internal fun LexemeComponentsBlock(
    lexemeState: LexemeState,
    availableTypes: List<ComponentType>,
    enabled: Boolean,
    sendMessage: (Msg) -> Unit,
) {
    // A12: лейбл значения резолвится по componentTypeId из availableTypes (живой тип →
    // переименование подхватывается) с FALLBACK на снимок cv.componentTypeRef (окно до ComponentTypesLoaded).
    // Поле рендерится ВСЕГДА — никакого `?: return@key` (это был баг F1).
    // addedNonMultiTypeIds — computed property на LexemeState (не в composable; гайд §6).

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        lexemeState.components.forEach { cv: ComponentValueState ->
            key(cv.key) {
                ComponentValueField(
                    state = cv,
                    label = componentValueLabel(cv.componentTypeId, cv.componentTypeRef, availableTypes),
                    enabled = enabled,
                    onValueChange = { sendMessage(Msg.UpdateComponentValueInput(lexemeState.id, cv.key, it)) },
                    onOpenEditMode = { sendMessage(Msg.EnterComponentValueEditMode(lexemeState.id, cv.key)) },
                    onCommitEdit = { sendMessage(Msg.CommitComponentValueEdit(lexemeState.id, cv.key)) },
                    onRemove = { sendMessage(Msg.RemoveComponentValueRequested(lexemeState.id, cv.key)) },
                )
            }
        }
        ComponentChipsRow(
            availableTypes = availableTypes,
            addedNonMultiTypeIds = lexemeState.addedNonMultiTypeIds,   // computed на State
            enabled = enabled,
            onAddComponent = { typeId ->
                sendMessage(Msg.CreateComponentValue(lexemeState.id, typeId))
            },
        )
    }
}
```

> **F1 (Б1-целостность).** Эта секция была артефактом краша агента: фикс B1 лёг в §1/§2, но здесь оставался старый `typesById[...] ?: return@key` lookup, который прятал значения в окне до загрузки типов и не компилировался против §2 (`ComponentValueField` без параметра `type`). Исправлено: lookup удалён, `type` не передаётся, рендер только из снимка.

---

## §5. WordCardScreen.kt (MODIFY)

Lines 154-203 (текущий блок translation/definition + AddLexemeMeaningRow) полностью заменяется на:

```kotlin
state.lexemeList.forEach { lexemeState ->
    key(lexemeState.id) {
        LexemeCard {
            LexemeComponentsBlock(
                lexemeState = lexemeState,
                availableTypes = state.availableComponentTypes,
                enabled = !state.isPendingDbOp && !state.isExiting,   // флаг выхода тоже гасит ввод
                sendMessage = sendMessage,
            )
            DeleteLexemeButton(
                enabled = !state.isPendingDbOp && !state.isExiting,
                onClick = { sendMessage(Msg.OpenDeleteLexemeDialog(lexemeState.id)) },
            )
        }
    }
}
```

**Лоадер flush-on-back (обязателен, 03 §6.2.3).** Контент экрана обернуть так, чтобы при `state.isExiting` поверх показывался **блокирующий** лоадер, перехватывающий тапы:
```kotlin
Box {
    /* основной контент WordCard */
    if (state.isExiting) LoadingOverlay()   // полупрозрачный fullscreen + CircularProgressIndicator, clickable-consume
}
```
Запись локальная (SQLite) → обычно мелькает доли секунды; нужен, чтобы пользователь не тыкал по экрану, пока идёт дожим перед выходом.

Удаляются imports: `AddLexemeMeaningRow`, `LexemeMeaningField`, `TextValueState`. Добавляются imports: `LexemeComponentsBlock`, `ComponentValueState`.

Preview composable'ы (`Preview`, `PreviewWithLexeme`) обновить: новый shape `LexemeState(id, components = listOf(ComponentValueState(...)))` **И** `WordState.Loaded` теперь требует `dictionaryId: Long` (без дефолта) — добавить в preview-фикстуры, иначе compile-fail.

---

## §6. strings.xml (MODIFY)

Добавить в существующий `core-resources/src/main/res/values/strings.xml` (или модульный strings.xml):

```xml
<string name="word_card_error_load_component_types">Couldn\'t load components</string>
<string name="word_card_action_retry">Retry</string>
<string name="word_card_error_generic">Couldn\'t save changes</string>
```

> H-7: `word_card_error_generic` нужна для `OperationFailed`. `word_card_error_restore_lexeme` (A17) уже существует — НЕ добавляем.

> **M1 — английский текст.** Существующий `core-resources/.../values/strings.xml` использует английский (`Translation`, `Undo`, `Value deleted`). Русские переводы — в `values-ru` (если есть). Не нарушать языковую конвенцию базового файла.

`word_card_bottom_translation` — уже существует. `word_card_snackbar_lexeme_deleted` / `word_card_snackbar_undo` — уже существуют (используются для cascade-undo).

`word_card_snackbar_translation_deleted` / `word_card_snackbar_definition_deleted` — больше не используются (translation/definition-specific). Не удаляем сразу (другие места могут грепнуть), но можно пометить deprecated. Backlog: cleanup unused strings.

---

## §7. Связи с другими слоями

- **Business → UI:** state.availableComponentTypes (List<ComponentType>) + lexemeState.components (List<ComponentValueState>). UI ничего о CoreDbApi не знает.
- **UI → Business:** Msg через sendMessage. Все Msg уже определены в § 03.
- **Domain → UI:** `BuiltInComponent.TRANSLATION` resolver-key (без displayLabel в domain).
- **Resources:** через `core_resources.R` (existing pattern).

---

## §8. Acceptance Tier 3

**Build / lint:**
- `./scripts/cc-build.sh :modules:screen:wordcard:assembleDebug` — зелёный.
- `./scripts/cc-build.sh :app:lintDebug` — зелёный.

**Visual smoke (manual):**
1. Открыть WordCard для лексемы со словарём где есть N global + M per-dict компонентов.
2. Видим chip-row с (N+M) chips (built-in Translation + N-1 + M).
3. Тап на Translation chip → input pristine «Перевод» появляется выше chip-row, chip Translation исчезает (non-multi).
4. Печать в input, blur → значение сохранено (Crashlytics / app log — UpsertComponentValue path).
5. Re-open экран — chip отсутствует (translation saved), value показан выше chip-row, есть trash.
6. Тап на multi-chip (например User-defined «Example», isMulti=true) дважды → два input'а появляются, chip остаётся.
7. Trash на любом → значение исчезает, для non-multi — chip возвращается; для multi — chip уже был.
8. Удаление последнего component → cascade-delete лексемы, snackbar с undo.
9. Undo → лексема восстанавливается через `addLexemeWithComponents`.

---

## §9. Риски

- **`@Composable componentLabelOf` requires Composable context.** Если попадает в reducer / preview-helper — упадёт. Использовать ТОЛЬКО внутри `@Composable` функций UI.
- **SubentityChip(label: String, ...) overload conflict.** Existing `SubentityChip(@StringRes labelRes, ...)` — оставить как есть, NEW overload с `label: String`. Compile-time распознаются по параметру. Двойник в Preview — обновить.
- **Preview без availableComponentTypes.** Текущие `Preview` / `PreviewWithLexeme` упадут пустыми chip-row (визуально). Это OK для preview (только smoke рендера); тестируем full flow в Manual smoke.
- **`LexemeEditableText` поведение на blur empty.** Текущее: `if (value.isEmpty()) onRemove() else onCommitEdit()` — переносится 1-в-1 в `ComponentValueField`. Reducer должен корректно отличать pristine empty (LocalRemove) от saved empty (PessimisticRemove) — это в `commitDecision()` (§ 03 §3.2).
- **Multi chip + быстрые тапы.** Каждый `CreateComponentValue` сперва зовёт `commitAndCloseAllEdits` (commit-on-create), и пустой pristine на этом шаге получает `LocalRemove` → дропается. Поэтому 3 пустых тапа по multi-chip дают **1 пустое поле, а не 3** (пустые не копятся — контракт 03 §6.4 S-CREATE, тест S22/V5). Несколько полей появляются только если между тапами введён текст. Ожидание «3 пустых тапа → 3 поля» — НЕВЕРНО.
