# IS485 components — План изменений (что и почему)

Волнами. Источники: [brief.md](brief.md) (макеты, решения №1-5), [analysis.md](analysis.md) (разбор кода).

## Волна 0 — чистка Rename (отдельным коммитом, ДО редизайна)

**Почему отдельно:** удаление мёртвого rename-пути — ~90 строк логики в двух reducer'ах + Msg/State/Effect/Handler + тесты в 4 файлах + виджет — это вычистка мёртвого кода, а не визуал. Отдельный коммит держит diff редизайна чисто-UI и делает оба ревью читаемыми (решение №2 «удаляем в ходе имплементации» — соблюдено, просто первым коммитом).

### 0.1 Удалить rename в `per_dictionary_components` и `components_manager` (симметрично)
- `mate/Msg.kt`: `OpenRenameDialog`, `RenameTextChange`, `SubmitRename`, `CloseRenameDialog`, `RenameResult`.
- `mate/State.kt`: `renameDialog`, `isRenaming`, `RenameDialogState`.
- `mate/Reducer.kt`: ветки перечисленных Msg + rename-обработка Result.
- `mate/DatasourceEffect.kt` + `DatasourceEffectHandler.kt`: `RenameComponent` effect + handler-ветка.
- `Screen.kt`: монтирование `RenameComponentDialog`.

**Почему:** из UI не вызывается (карандаш → `OpenEditDialog`, полный Edit поглощает переименование). Мёртвый вход → вся цепочка недостижима.

### 0.2 Удалить `component_widgets/dialogs/RenameComponentDialog.kt` целиком
**Почему:** после 0.1 нет потребителей.

### 0.3 Тесты: убрать rename-кейсы (per_dict `ReducerTest`+`DatasourceEffectHandlerTest`, manager те же)
**Почему:** тесты мёртвого пути тестируют несуществующий сценарий; остальные кейсы остаются зелёными (изоляция).

### 0.4 DAO `renameUserDefined` — НЕ трогаем в этой волне
**Почему:** data-слой, после удаления effect'ов формально мёртвый, но проверка потребителей и удаление из DAO/схемы — отдельный data-риск; фиксируем в Backlog, не мешаем с UI-фичей.

### 0.5 Проверка волны 0: `:per_dictionary_components:test` + `:components_manager:test` зелёные, модули компилируются
**Почему:** удаление обязано оставить остальные пути рабочими до начала редизайна.

## Волна 1 — токены

### 1.1 `theme/Color.kt` — +8 val
`cardBorder #ECEAE4`, `typeIconBg #E9E6FA`, `iconButtonBg #F3F2EE`, `templateChipBg #F1F0EC`, `templateChipText #6E6E76`, `dialogFieldBg #F5F4F0`, `radioSelectedBg #F3F1FC`, `radioBorderInactive #C4C4CC`. Тени — производные.

**Почему:** цветов макета в теме нет; паттерн «точечные val» принят; `formBackground`/`destructiveRed`/`formTextHint`/`formTextSecondary`/`primary` уже есть — переиспользуем.

### 1.2 Строки — добавить недостающие (проверить на месте)
**Почему:** большинство `components_*` ключей есть; финальный список — при реализации.

## Волна 2 — примитивы виджетов (независимы)

### 2.1 `widgets/BlueAssistChip.kt` → чип шаблона: `templateChipBg` r8 h25, иконка 13 + текст 12.5 `templateChipText`
**Почему:** в макете чип шаблона — единственный оставшийся чип строки (бейджи убраны, решение №1); мелкий нейтральный вид.

### 2.2 `widgets/CreateComponentFab.kt` → extended FAB «+ Создать», primary r17 h54, тень
**Почему:** макет 5027:1534 — расширенный FAB с текстом (сейчас обычный круглый).

## Волна 3 — карточки-строки (зависят от 2.x)

### 3.1 `widgets/PerDictRowWidget.kt` — новый layout карточки
Карточка r18 + бордер `cardBorder` + лёгкая тень: слева иконка типа 42 r12 `typeIconBg`, название 18 bold + «Значений: N» 12.5 `formTextHint`, справа edit/delete 34 r10 `iconButtonBg` (delete — `destructiveRed`), внизу чип шаблона. **Убрать** рендер global/single-multi/dictionaryNames-чипов; параметры оставить в сигнатуре.

**Почему:** решение №1 (бейджи не показываем, терминология не финализирована); данные в стейте живут — чтобы вернуть бейджи позже без правки контракта.

### 3.2 `widgets/UserDefinedRowWidget.kt` (Manager row) — тот же облик
**Почему:** решение №3 (оба экрана согласованно); Manager-строка визуально идентична per-dict.

## Волна 4 — диалоги (зависят от 1.x)

### 4.1 `dialogs/CreateComponentDialog.kt` — под макет
Заголовок, лейбл «НАЗВАНИЕ» caps primary + поле `dialogFieldBg` бордер primary r13 h46, лейбл «ТИП ЗНАЧЕНИЯ» caps + радио-ряды r9 h42 (выбран `radioSelectedBg` бордер primary; не выбран white бордер `radioBorderInactive`), чекбокс primary r7, кнопки h52 r14 «Отменить»/«Создать». Scope-picker (Manager variant) — по тем же токенам.

**Почему:** макет 5027:1587; radio/чекбокс — заметно иной вид, чем текущие `LexemeRadioRow`/`Checkbox`. Общие `core:ui`-инпуты (`LexemeTextFieldWidget`, `CancelButtonWidget`, `PrimaryFullButtonWidget`) НЕ рестайлим (внешние потребители) — либо локальные поля диалога, либо параметризация; решит реализация.

### 4.2 `dialogs/EditComponentDialog.kt` — тот же облик + причесать превью затронутых лексем
**Почему:** Edit визуально идентичен Create (заголовок другой); блок `CardinalityDowngradePreviewWidget` — по токенам.

### 4.3 `dialogs/DeleteComponentConfirmDialog.kt` — destructive-кнопка + токены
**Почему:** синхронизация палитры (фрейма нет).

## Волна 5 — экраны (зависят от всего)

### 5.1 `PerDictionaryComponentsScreen.kt` + `ComponentsManagerScreen.kt` — фон `formBackground`, прозрачный топбар с именем словаря/заголовком; system bars
**Почему:** решение №5 (`#FCFCFA`); топбар в макете сливается с фоном.

### 5.2 Превью-функции тронутых виджетов — обновить (новая карточка, extended FAB, диалог, radio-состояния)
**Почему:** единственный способ смотреть состояния без девайса.

## Волна 6 — проверка

### 6.1 (по одной задаче) `:component_widgets` + оба screen-модуля `testDebugUnitTest` → `:app:lintDebug` → `assembleDebug`
**Почему:** тесты обоих экранов — контракт; ноут тормозят три задачи разом — гоняем последовательно.

### 6.2 APK на девайс: список компонентов, диалог create/edit (radio-переключение, чекбокс), delete-confirm с impact-превью, empty-state, extended FAB; ОБА экрана (per-dict из словаря + Manager из Настроек)
**Почему:** radio/чип/тени — визуальное; оба экрана делят виджеты — проверить, что рестайл не сломал Manager-специфику (scope-picker, multi-select словарей).

## Что осознанно НЕ делается

- Mate-логика CRUD/scope/impact/epoch — не трогается (кроме удаления rename).
- Бейджи global/multiple/scope — убраны из рендера, но данные и параметры сохранены (вернём, когда пользователь решит терминологию).
- Общие `core:ui`-виджеты (текст-поле, кнопки) — не рестайлим глобально (внешние потребители).
- DAO `renameUserDefined` — в Backlog (data-риск).

## Оценка

Волна 0 (rename-чистка, отдельный коммит) + ~10 виджетов рестайл (бьёт по обоим экранам) + 2 экрана + токены + строки. Крупнее прошлых фич: двойной потребитель + удаление мёртвого пути. Критический путь — волна 0 (аккуратность с тестами) и 4.1 (radio/чекбокс diverge от текущих примитивов).
