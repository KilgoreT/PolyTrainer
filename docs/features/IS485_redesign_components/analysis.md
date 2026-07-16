# IS485 components — Анализ: виджеты, объём, несоответствия

По коду `modules/widget/component_widgets/` + `modules/screen/per_dictionary_components/` + `modules/screen/components_manager/` (2026-07-15). Требования: [brief.md](brief.md).

## Ключевой факт: общие виджеты, два потребителя

Весь UI обоих экранов («Компоненты словаря» = per_dict, «Менеджер компонентов» = manager) собран из `:modules:widget:component_widgets`. Рестайл виджетов = согласованное обновление обоих экранов (решение №3).

Виджеты модуля:
- `dialogs/`: `CreateComponentDialog`, `EditComponentDialog`, `DeleteComponentConfirmDialog`, **`RenameComponentDialog` (удаляем)**
- `widgets/`: `PerDictRowWidget`, `UserDefinedRowWidget` (row Manager), `CreateComponentFab`, `ComponentsEmptyStateWidget`, `BlueAssistChip`, `ComponentTemplateLabel`, `NameErrorLabel`, `CardinalityDowngradePreviewWidget`

## Что и как менять (рестайл под макеты)

| Виджет | Правка |
|---|---|
| `widgets/PerDictRowWidget.kt` | карточка r12→r18 + бордер `#ECEAE4` + лёгкая тень; **убрать бейджи** global/single-multi/dictionaryNames (решение №1 — оставляем только чип шаблона); layout по макету: слева квадратная иконка типа 42 r12 на `#E9E6FA`, название 18 bold, «Значений: N» 12.5 `#9A9AA2`, справа две кнопки-иконки 34 r10 на `#F3F2EE` (edit + delete-красная), внизу чип шаблона `#F1F0EC` r8. Параметры `isMultiple`/`isGlobal`/`dictionaryNames` остаются в сигнатуре (данные живут), просто не рендерятся |
| `widgets/UserDefinedRowWidget.kt` (Manager row) | тот же новый облик карточки; проверить — там бейджи scope/словарей тоже убрать по решению №1 |
| `widgets/BlueAssistChip.kt` | чип шаблона под макет: `#F1F0EC` r8 h25, иконка 13 + текст 12.5 `#6E6E76` |
| `widgets/CreateComponentFab.kt` | обычный FAB → **extended** (иконка «+» + текст «Создать»), primary r17 h54, тень `0 12 26 -6 rgba(71,73,184,0.55)` |
| `widgets/ComponentsEmptyStateWidget.kt` | стилизация по токенам (макета нет — по системе) |
| `dialogs/CreateComponentDialog.kt` | под макет диалога: заголовок H, лейбл «НАЗВАНИЕ» caps primary + поле `#F5F4F0` бордер primary r13 h46; лейбл «ТИП ЗНАЧЕНИЯ» caps + радио-ряды r9 h42 (выбран `#F3F1FC` бордер primary; не выбран white бордер `#E6E4DC`); чекбокс primary r7 + лейбл; кнопки h52 r14 «Отменить» `#F1F0EC` / «Создать» primary с тенью. Scope-picker (Manager variant) — причесать по тем же токенам (макета нет, решение №3) |
| `dialogs/EditComponentDialog.kt` | тот же облик диалога (заголовок «Редактировать…», поля/радио/чекбокс/кнопки идентичны Create); + существующий блок превью затронутых лексем (`CardinalityDowngradePreviewWidget`) причесать по токенам |
| `dialogs/DeleteComponentConfirmDialog.kt` | destructive-кнопка `destructiveRed`, диалог по токенам |
| Экран `PerDictionaryComponentsScreen.kt` | фон `#FCFCFA`, прозрачный топбар с именем словаря; убрать монтирование rename (см. ниже) |
| Экран `ComponentsManagerScreen.kt` | тот же фон/топбар; убрать монтирование rename |

## Удаление Rename (решение №2) — объём немаленький

Rename прошит в ОБА экрана (не только UI-диалог; из UI не вызывается — Edit его поглотил):

| Файл | rename-упоминаний | что убрать |
|---|---|---|
| per_dict `mate/Reducer.kt` | 33 | ветки `OpenRenameDialog`/`RenameTextChange`/`SubmitRename`/`CloseRenameDialog` + rename-обработка Result |
| manager `mate/Reducer.kt` | 33 | то же |
| per_dict/manager `mate/Msg.kt` | 7+7 | `OpenRenameDialog`/`RenameTextChange`/`SubmitRename`/`CloseRenameDialog`/`RenameResult` |
| per_dict/manager `mate/State.kt` | 3+5 | `renameDialog`, `isRenaming`, `RenameDialogState` |
| per_dict/manager `mate/DatasourceEffect.kt`+Handler | 1+7 ×2 | `RenameComponent` effect + handler-ветка (вызов `renameUserDefined`) |
| per_dict/manager Screen | 7+7 | монтирование `RenameComponentDialog` |
| `component_widgets/dialogs/RenameComponentDialog.kt` | файл | удалить целиком |
| тесты: per_dict `ReducerTest`+`DatasourceEffectHandlerTest`, manager `ReducerTest`+`DatasourceEffectHandlerTest` | 4 файла | убрать rename-кейсы |
| DAO `renameUserDefined` (core-db-impl) | проверить | если после удаления effect'ов больше нет потребителей — тоже мёртвый, но это data-слой → отдельно, вероятно оставить/Backlog |

**Возражение (объём/скоуп):** удаление rename — ~90 строк логики в двух reducer'ах + Msg/State/Effect/Handler + тесты в 4 файлах — это **чистка мёртвого кода**, логически отдельная от визуального редизайна. Смешивание раздувает diff фичи и усложняет ревью (визуальные правки vs логические удаления в одном коммите).
**Альтернатива:** сделать удаление rename **отдельным первым коммитом** «chore: удалить мёртвый rename-путь компонентов» ДО коммита редизайна. Логика чистая (red→green на существующих тестах: удаляем rename-кейсы, остальные зелёные), diff читаемый, редизайн-коммит остаётся чисто-UI. Пользователь сказал «в ходе имплементации» — предлагаю в ходе, но отдельным коммитом.

## Токены (в `theme/Color.kt` добавить, часть есть)

Есть: `formBackground` #FCFCFA, `destructiveRed` #D64545, `formTextHint` #9A9AA2, `formTextSecondary` #8A8A90, `LexemeColor.primary`.
Добавить: `cardBorder` #ECEAE4, `typeIconBg` #E9E6FA, `iconButtonBg` #F3F2EE, `templateChipBg` #F1F0EC, `templateChipText` #6E6E76, `dialogFieldBg` #F5F4F0, `radioSelectedBg` #F3F1FC, `radioBorderInactive` #E6E4DC/#C4C4CC. Тени карточки/FAB/кнопок — производные / точечные.

## Строки

Вероятно всё есть (`components_*` ключи, «Значений: %d», «Текст»/«Изображение», «Отменить»/«Создать», «Новый компонент», «Разрешить несколько значений на карточке»). Проверить на реализации, чего не хватает.

## Mate — НЕ меняется (кроме удаления rename)

CRUD, scope, валидации, epoch-correlation, deletion-impact, empty-selector — не трогаем. Удаление rename — не логика фичи, а вычистка мёртвого кода (существующие тесты остальных путей остаются зелёными).

## Объём

~10 виджетов рестайл (общий модуль, бьёт по обоим экранам) + 2 экрана (фон/топбар/размонтирование rename) + удаление rename (2 reducer'а + Msg/State/Effect/Handler + 4 тест-файла + 1 виджет-файл) + токены. Больше прошлых фич за счёт rename-чистки и двойного потребителя. Сложность: визуал средний, rename-удаление — аккуратная механическая чистка с тестами.
