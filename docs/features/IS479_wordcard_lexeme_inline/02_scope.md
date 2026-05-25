# Scope analysis: IS479 — wordcard lexeme inline UI

## Резюме

Задача затрагивает экран `modules/screen/wordcard` (карточка слова). Меняется механика и UI добавления лексемы: вместо bottom sheet (`AddLexemeBottomState` + `AddLexemeBottomWidget`) — встроенный inline-UI внутри основного скролл-контента карточки. Кнопка-FAB добавления лексемы заменяется новой кнопкой по Figma-frame `9154-82532`; кнопка «Перевод» и остальные кнопки добавления получают chip-стиль. Опция «Пример» (Example) исключается из брифа — в текущем коде её нет (`Example` отсутствует в `Msg` / `State` / виджетах `addlexeme/*`), поэтому исключение касается только Figma-исходника и финального UI, не текущей реализации. Sub-flow Infra и Data не нужны — DI/mate/Manifest/ProGuard/Navigation не меняются, контракт `WordCardUseCase` и Room-схема остаются прежними.

## Затронутые слои

- **Infrastructure** — нет — chip-стиль реализуется локально в `screen/wordcard` через Material3 chip напрямую (готовые `AssistChip` / `FilterChip` / `SuggestionChip` / `InputChip`); повторное применение внутри одного экрана = локальный паттерн, не системный. `PrimaryLongFabWidget` + `LexemeLongFab` в `core/ui` — wordcard-специфичные виджеты (единственный потребитель — `AddLexemeWidget`), их удаление = производная задача от UI-changes, не самостоятельная инфра-работа. Если на UI sub-flow станет очевидно что chip имеет смысл вынести в `core/ui` для других экранов — это отдельная задача в backlog (YAGNI).
- **Business logic** — да — `WordCardState.addLexemeBottomState` теряет смысл (флаг `show` несовместим с inline-механикой); конкретный шейп нового суб-стейта определяется на шаге `contract_state`. Msg-входы `OpenAddLexemeDialog` / `CloseAddLexemeDialog` / `EnableTranslationCreation` / `EnableDefinitionCreation` / `CreateLexeme` / `RefreshLexeme` остаются по смыслу, но требуют ревизии семантики и имён под inline-механику. Соответствующие ветки `WordCardReducer` и ext-функции `showAddLexemeBottom` / `hideAddLexemeBottom` / `setTranslationCheck` / `setDefinitionCheck` подлежат рефакторингу.
- **UI** — да — основной скоуп фичи. Заменяется `AddLexemeWidget` (FAB) на новую кнопку по `9154-82532`, удаляется/перестраивается `AddLexemeBottomWidget` + `ActionsWidget` + `LexemeMeaningWidget` в пользу inline-композиции по `9154-82519`. «Перевод» и кнопки добавления переводятся на chip-стиль (`9154-82521`, `9154-82625`). Влияет на `WordCardScreen.kt` (Scaffold-слот `floatingActionButton`, условный рендер `state.addLexemeBottomState.show`).
- **Data** — нет — `WordCardUseCase` контракт без изменений; БД, схемы, prefs, library-обёртки не затрагиваются.

## Аспекты

- `ui_redesign` — замена bottom sheet на inline UI + перевод кнопок на chip-стиль по Figma-frames.
- `state_reshape` — `AddLexemeBottomState` (флаг `show`) несовместим с inline-механикой; форма суб-стейта будет переработана. Конкретный шейп определяется на шаге `contract_state`. Ломаются существующие тесты, конструирующие `AddLexemeBottomState(...)` — см. раздел «Затронутые файлы → Тесты».
- `feature_subtraction` — позиция «Пример» исключается из спецификации Figma; в коде её и так нет, но контракт спецификации должен зафиксировать отсутствие.
- `figma_to_code` — переход от готового дизайн-исходника (frames `9154-*`) к Compose-композиции; могут понадобиться design-токены (chip-стиль, цвета, отступы).
- `chip_local_in_wordcard` — chip-стиль применяется в нескольких местах одного экрана (`9154-82521` образец, `9154-82625` группа). Реализация **локальная в `screen/wordcard`** через Material3 chip напрямую (`AssistChip` / `FilterChip` / `SuggestionChip` / `InputChip` — выбор на UI sub-flow по сценарию-действию). Вынос в `core/ui` как переиспользуемый компонент — **не делаем превентивно** (YAGNI). Если будущая фича потребует chip в другом экране — выделим тогда отдельной infra-задачей.

  **Precedent в проекте:** `modules/widget/chipPicker/.../ChipPickerWidget.kt` уже использует Material3 `SuggestionChip` + `InputChip` (другая фича — picker лексем). Подтверждает что Material3 chip совместим с проектной палитрой и `ExperimentalMaterial3Api` OptIn приемлем.
- `lifecycle_after_modal_removal` — переход модальное → inline убивает «бесплатные» механики bottom sheet (dismiss-жест, авто-сброс при тапе вне, восстановление `rememberModalBottomSheetState`). Конкретные сценарии (NavigateBack, configuration change, ошибка CreateLexeme, семантика «открыто/закрыто» без флага `show`) — решаются на шагах `contract_state` / `contract_io`.

Аспекта `public_contract_change` нет — `deps/WordCardUseCase` остаётся прежним. Аспекта `db_migration` / `production_crash` / `release_only_bug` / `new_dependency` / `cross_tab_subscription` нет.

## Затронутые файлы

Основная зона:

- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/WordCardScreen.kt` — слот FAB и условный рендер `AddLexemeBottomWidget` заменяются на встроенный inline-блок в основной Column.
- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/AddLexemeWidget.kt` — FAB (`PrimaryLongFabWidget`) либо удаляется, либо превращается в обычную inline-кнопку по новому фрейму.
- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/addlexeme/AddLexemeBottomWidget.kt` — удаляется или превращается в inline-композицию (без `ModalBottomSheet`).
- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/addlexeme/ActionsWidget.kt` — кнопки «Добавить»/«Отмена» переоформляются под inline-сценарий и chip-стиль.
- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/addlexeme/LexemeMeaningWidget.kt` — переключатели «Перевод/Definition» переоформляются в chip-стиль (или заменяются на chip-кнопки добавления).
- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/State.kt` — `AddLexemeBottomState` (флаг `show`, `isTranslationCheck`, `isDefinitionCheck`) перепроектируется под inline-механику; ext-функции `showAddLexemeBottom` / `hideAddLexemeBottom` / `setTranslationCheck` / `setDefinitionCheck` ревизируются.
- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/Message.kt` — `Msg.OpenAddLexemeDialog` / `CloseAddLexemeDialog` / `EnableTranslationCreation` / `EnableDefinitionCreation` пересматриваются на семантику inline-режима.
- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/WordCardReducer.kt` — ветки add-lexeme подсистемы: `OpenAddLexemeDialog`, `CloseAddLexemeDialog`, `EnableTranslationCreation`, `EnableDefinitionCreation`, `CreateLexeme`, `RefreshLexeme` (логика `RefreshLexeme` зависит от `isTranslationCheck` / `isDefinitionCheck` — её надо сохранить семантически).
- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/AddTranslationLexemeMenuItem.kt`, `AddDefinitionLexemeMenuItem.kt` — точечно проверить, нужны ли изменения; пункты меню «добавить перевод/определение» внутри существующей лексемы сохраняются.

Мёртвый код после удаления потребителя (пометка для UI sub-flow):

- `modules/core/ui/src/main/java/me/apomazkin/ui/btn/PrimaryLongFabWidget.kt`, `LexemeLongFab.kt` — wordcard-специфичные виджеты в `core/ui` (единственный потребитель — `AddLexemeWidget`); после удаления потребителя могут стать мёртвым кодом. Решение об удалении — в UI sub-flow.

Тесты (под `business_touched`):

- `modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/LexemeManagementTest.kt` — основной набор add-lexeme, требует переписывания под новую форму состояния и новые Msg-входы.
- `modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/ext/AddLexemeExtTest.kt` — тесты `showAddLexemeBottom` / `hideAddLexemeBottom` / `setTranslationCheck` / `setDefinitionCheck`. Подлежат удалению или переписыванию под новые ext-имена (решение в `contract_state` / `design_tree`).
- Все остальные `mate/*Test.kt` и `mate/ext/*Test.kt`, которые конструируют `WordCardState(addLexemeBottomState = AddLexemeBottomState(...))`, потребуют косметической правки под новую форму поля.

  **Явные конструкции `AddLexemeBottomState(...)`** (ломаются при изменении конструктора): `OpenTopBarMenuTest`, `CloseTopBarMenuTest`, `NavigateBackTest`, `NoOperationTest`, `DeleteWordDialogTest`, `ShowNotificationTest`, `WordEditTest`, `LoadingWordTest`, `WordLoadedTest`, `mate/ext/LoadingExtTest`, `mate/ext/LexemeExtTest`, `mate/ext/SpecializedLexemeExtTest`, `mate/ext/SnackbarExtTest` (7 конструкций), `mate/ext/TopBarExtTest` (6 конструкций).

  **Неявная зависимость через дефолт `AddLexemeBottomState()` в `WordCardState(...)`**: `mate/ext/WordExtTest` — типовой представитель категории. Ломается при изменении дефолт-конструктора суб-стейта; immutability-assertions потребуют ревизии если поменяется имя поля или сигнатура. По факту аналогичная неявная зависимость есть во **всех** тестах, конструирующих `WordCardState(...)` без явной передачи `addLexemeBottomState` — полный список найти `grep -rln "WordCardState(" modules/screen/wordcard/src/test/`.

  Близнецов дефекта нет — это не баг-кейс.

Ресурсы (`core/core-resources/src/main/res/values/strings.xml` + `values-ru-rRU/strings.xml`): возможны новые ключи под chip-метки, ревизия `word_card_bottom_title`, `word_card_bottom_title_append`, `word_card_bottom_translation`, `word_card_bottom_definition`, `word_card_add_lexeme` (часть ключей может стать неактуальной после удаления bottom sheet).

**Blast radius переименования ключей `word_card_bottom_*` ограничен модулем `screen/wordcard`** — production-консьюмеры:
- `modules/screen/wordcard/.../widget/lexeme/LexemeItemWidget.kt:54` (`word_card_bottom_translation` / `_definition`)
- `modules/screen/wordcard/.../widget/lexeme/LexemeValueFieldWidget.kt:80` (то же)
- `modules/screen/wordcard/.../widget/addlexeme/AddLexemeBottomWidget.kt:62` (удаляется в рамках фичи)

В `core/ui/.../text/PrimaryEditableWidget.kt:78` ключ упомянут **только в превью-функции** (`@PreviewWidget @Composable private fun Preview(...)`); сам widget принимает `titleRes: Int` параметром. Production cross-module зависимостей нет — переименование ключей безопасно для других модулей (превью можно обновить косметически).

## Релевантные спеки и гайды

Спека модуля `wordcard` в `docs/features-spec/` отсутствует (в `docs/features-spec/README.md` пункт «Карточка слова (WordCard)» без ссылки — в разделе известных пробелов). Поэтому в шаге `contract_spec` будет создаваться новая.

Гайды:

- `docs/guides/mate-framework.md` — TEA-цикл State/Msg/Reducer/Effect для wordcard.
- `docs/guides/state-and-extensions.md` и `docs/guides/state-modeling.md` — паттерн ext-функций над `WordCardState` (`show*`, `hide*`, `set*Check`); обновление формы суб-стейта `AddLexemeBottomState`.
- `docs/guides/reducer-patterns.md` — переходы add-lexeme подсистемы.
- `docs/guides/testing-reducers.md` и `docs/guides/testing-extensions.md` — обновление существующих тестов под новую форму состояния.
- `docs/guides/ui-patterns.md` и `docs/guides/theme-and-resources.md` — chip-стиль кнопок, design-токены.
- `docs/guides/effect-handlers.md` — `DatasourceEffect.CreateLexeme` (`data class` в `sealed interface DatasourceEffect`, обрабатывается `DatasourceEffectHandler`) остаётся как есть, но контекст полезен.
- `docs/guides/messages.md` — ревизия имён `Msg.OpenAddLexemeDialog` / `CloseAddLexemeDialog` под inline-семантику.

Спеки `docs/features-spec/dictionary-list.md`, `dictionary-appbar.md`, `dictionary-create.md`, `flag-placeholder-widget.md` — не релевантны (другая фича-зона).

## Sub-flow для запуска

| Sub-flow | Запускать? | Обоснование |
|---|---|---|
| Infrastructure | нет | Chip-стиль реализуется локально в `screen/wordcard` через Material3 chip напрямую. `PrimaryLongFabWidget` / `LexemeLongFab` в `core/ui` — wordcard-локальные виджеты, их удаление — производное от UI-changes, не самостоятельная инфра-работа. Вынос chip в `core/ui` — отдельная задача в backlog, если станет очевидно по результатам UI. |
| Business | да | Перепроектирование `AddLexemeBottomState` и его ext-функций, ревизия Msg add-lexeme подсистемы, переходы Reducer, обновление Reducer-тестов. Имеется UI-контракт (State/Msg/Reducer + Composable). |
| UI | да | Замена bottom sheet на inline-композицию, новая кнопка добавления, chip-стиль кнопок по Figma-frames `9154-82509/82519/82521/82532/82625/86012/86182/86353/86499`. |
| Data | нет | `WordCardUseCase` контракт, Room-схема, prefs не меняются. |

## Context output

```yaml
infra_touched: false
business_touched: true
ui_touched: true
data_touched: false
needs_tests: true
needs_migration_tests: false
feature_has_ui_contract: true
spec_filename: null
```
