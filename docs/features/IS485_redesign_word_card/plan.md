# IS485 word_card — План изменений (что и почему)

Порядок — волнами: каждая волна опирается только на предыдущие, внутри волны шаги независимы. Источники: [brief.md](brief.md) (макеты, токены, решения №1–6), [analysis.md](analysis.md) (разбор кода).

## Волна 1 — фундамент (токены, строки)

### 1.1 `modules/core/theme/Color.kt` — +7 val

`wordCardBackground #EDE9F6`, `cardShadowTint rgba(50,40,90,…)`, `draftBorder #DCD6F2`, `draftBadgeBg #EEECEF`, `destructiveRed #D64545`, `cardDivider #F0EEF7`, `editUnderline #C2C1E8`, `fabLavender #C9C3EA`, `fabLavenderDisabled #D8D3EC`.

**Почему:** цветов макета в теме нет (проверено в analysis); паттерн «точечные val» принят на фиче create_dictionary — следующие экраны зонтика будут их переиспользовать. `formTextSecondary`/`formTextHint` уже есть — не дублируем. Чипы — существующий `LexemeColor.primary` (решение №4).

### 1.2 `strings.xml` EN + RU — +3 ключа

`word_card_draft_badge` («Draft» / «Черновик»), `word_card_draft_hint` («Pick a component to add a value» / «Выберите компонент, чтобы добавить значение»), `word_card_add_component_label` («ADD COMPONENT» / «ДОБАВИТЬ КОМПОНЕНТ»).

**Почему:** этих текстов в ресурсах нет; «Удалить» — уже есть (`word_card_lexeme_remove`), решение №3 использует его.

## Волна 2 — Mate-цепочка флага (решение №5, единственная логическая правка)

### 2.1 `entity/Term.kt` — +`dictionaryFlagRes: Int? = null`

**Почему:** флаг словаря должен приехать на экран одним запросом с Term — без нового Msg/Effect/подписки; nullable с дефолтом не ломает 221 существующий тест (фикстуры используют именованные аргументы).

### 2.2 `app/.../WordCardUseCaseImpl.getTermById` — резолв флага (объём уточнён по ревью плана)

По `dictionaryId` берём словарь из БД (`dictionaryApi.getDictionaryById(id)?.numericCode`) → маппим в drawable через country-провайдер (паттерн `findFlag`/`getFlagRes` из `DictionaryUseCaseImpl`). Конкретно:

- +конструкторный параметр `CountryProvider` (его сейчас в `WordCardUseCaseImpl` НЕТ — только wordApi/dictionaryApi/termApi/lexemeApi/prefsProvider/logger);
- правка Dagger-модуля wordCard (передать провайдер);
- в `getTermById`: доп. запрос словаря + маппинг numericCode→flagRes, прокинуть в `Term.dictionaryFlagRes`;
- `WordCardUseCaseImplTest`: +1 мок в конструирование useCase (setUp), + тест-кейсы резолва.

**Почему:** app-слой — единственное место, где сходятся БД словарей и справочник флагов; wordcard-модуль о blongho знать не должен (Dependency Rule). Ревью плана показало, что «~15 строк» было занижено: реалистично ~30-40 строк + DI-проводка.

### 2.3 `mate/State.kt` — `WordState.Loaded` +`dictionaryFlagRes: Int? = null`; `mate/WordCardReducer.kt` — маппинг в ветке `WordLoaded` (1 строка)

**Почему:** UI берёт данные только из стейта (правило explicit state); дефолт null = «флага нет» → плейсхолдер, старые тесты не трогаются.

### 2.4 Тесты: +1 reducer-тест «WordLoaded прокидывает dictionaryFlagRes» (full-state assert, R-TR-003) + дополнение `WordCardUseCaseImplTest` (резолв numericCode→flagRes, словарь без флага → null)

**Почему:** новая функциональность = новый тест рядом с родственными (`WordLoadedTest`); full-state — по правилу R-TR-003 для новых тестов.

## Волна 3 — примитивы виджетов (правки независимы друг от друга)

### 3.1 `widget/lexeme/SubentityChip.kt` — r6→r8, высота 26, текст 13 (стиль из темы), иконка 16→12

**Почему:** канон чипа из решения №2 (самый малый вариант макета); виджет уже filled primary и обслуживает обе роли (label-«×» и добавление-«+») — макет рисует их одинаково, разделять не надо.

### 3.2 `widget/lexeme/ComponentLabel.kt` — под тот же канон

**Почему:** label-чип обязан быть неотличим от SubentityChip (в макете это один вид).

### 3.3 `widget/lexeme/DeleteLexemeButton.kt` — divider `#F0EEF7` сверху + строка: корзина 18 + «Удалить» `#D64545` 15

**Почему:** решение №3 («Удалить» + divider); красный destructive из макета — сейчас в теме есть только error-пара для контейнеров, текстового destructive нет.

### 3.4 `widget/lexeme/ComponentValueField.kt` — view: значение 17 `LexemeColor.secondary`; edit: курсор primary + underline 1px `editUnderline`

**Почему:** фрейм 5027:1469 — тонкий underline `#C2C1E8` (намеренно легче, чем 2dp у формы словаря: поле внутри карточки, а не на фоне). Механика commit/фокуса не меняется.

### 3.5 `widget/AddLexemeWidget.kt` (FAB) — 60dp, r19, `fabLavender` + тень `0 10 22 -6 rgba(100,90,160,0.5)`; disabled: `fabLavenderDisabled` + alpha 0.75 БЕЗ тени

**Почему:** макет задаёт FAB светло-лавандовым (не primary!) с явным disabled-видом. Disabled управляется существующим `state.canAddLexeme` (= `!isPendingDbOp && !isCreatingLexeme`, WordCardScreen.kt:117) — т.е. disabled-вид включается НЕ только при открытом черновике, но и на время любой pending-записи в БД; это корректно, один boolean — один облик (уточнение из ревью плана).

## Волна 4 — карточки и сборка (зависят от 3.x)

### 4.1 `widget/lexeme/LexemeCard.kt` — r12→r18, elevation4 → мягкая тень `0 2 5 rgba(50,40,90,0.06)`, паддинг 16→20; +параметр `isDraft: Boolean` → бордер `draftBorder` + slot бейджа

**Почему:** черновик в макете визуально выделен бордером и бейджем — это ключевая новая семантика карточки; `isDraft` выводится в месте вызова из `id == NOT_IN_DB` (derived от данных, во стейт не заводим — прецедент «ничего не найдено» формы).

### 4.2 Новый мелкий composable `DraftBadge` (в `lexeme/`) — плашка `draftBadgeBg` r6, текст 11 bold `formTextHint`

**Почему:** используется в LexemeCard; отдельный файл — читаемость, как остальные примитивы блока.

### 4.3 `widget/lexeme/ComponentChipsRow.kt` — +right-aligned лейбл «ДОБАВИТЬ КОМПОНЕНТ» над чипами; существующий декоративный divider (33% ширины, primary@30%) УДАЛЯЕТСЯ

**Почему:** макет группирует добавление в явную секцию с заголовком-капсом; right-align чипов уже есть (FlowRow Alignment.End). Старый divider в макете отсутствует — его роль забирает лейбл; новый полноширинный divider живёт только над «Удалить» (3.3). РЕШЕНИЕ ПОЛЬЗОВАТЕЛЯ (2026-07-15): механика прежняя — чипы добавления видны на ВСЕХ лексемах (не только черновике), лейбл показывается везде, где ряд чипов непуст; идея «режима редактирования лексемы» ОТКЛОНЕНА (Клод-дизайн намалевал состояния, которых нет — фрейм 1299 в части «чистой карточки» игнорируется).

### 4.4 `widget/lexeme/LexemeComponentsBlock.kt` — подсказка «Выберите компонент…» (14.5 `formTextSecondary`) при пустом черновике; проброс isDraft вниз

**Почему:** фрейм 1406: пустой черновик объясняет юзеру следующий шаг (сейчас пустой черновик — только кнопка «Удалить», реестр помечал это болью).

### 4.5 `widget/WordFieldWidget.kt` — карточка r18 + мягкая тень; дата: лейбл `formTextHint` + дата bold `LexemeColor.secondary`; **флаг 38dp из `loaded.dictionaryFlagRes`** (fallback — текущий плейсхолдер), убрать `example_ic_flag_gb`

**Почему:** закрывает заглушку UK (решение №5, реестр ⚠); типографика даты — из фрейма; инлайн-редактирование слова не трогаем. ⚠ Приём размера (из ревью плана): у `ImageFlagWidget` НЕТ параметра размера, внутри зашит `.size(24.dp)` — 38dp задаётся ТОЛЬКО внешним `Modifier.size(38.dp)` (внешний size фиксирует constraints, внутренний coerce'ится — так уже работает сетка флагов 48dp/56dp).

## Волна 5 — экран (зависит от всего)

### 5.1 `WordCardScreen.kt` — фон `colorScheme.tertiary` → `wordCardBackground` (контент + system bars); `widget/TopBarWidget.kt` — прозрачный containerColor

**Почему:** макетный `#EDE9F6` ≠ текущий tertiary `#F1E9FA` (решение №1 — фоны зон осознанно разные); топбар в макете сливается с фоном.

### 5.2 `ConfirmDeleteLexemeWidget` / `ConfirmDeleteWordWidget` — стилизация по токенам (destructiveRed для подтверждающей кнопки)

**Почему:** фреймов на диалоги нет — минимальная синхронизация с новой палитрой, без перестройки.

### 5.3 Превью-функции всех тронутых виджетов — обновить/дополнить (черновик с бейджем, edit-режим, FAB disabled)

**Почему:** превью — единственный способ смотреть состояния без девайса; правило с прошлой фичи.

## Волна 6 — проверка

### 6.1 `:modules:screen:wordcard:testDebugUnitTest` (+ новые тесты волны 2) → `:app:lintDebug` → полные `testDebugUnitTest` → `assembleDebug`

**Почему:** тесты wordcard — контракт (221 + 1 новый обязаны быть зелёными без правок старых); дальше стандартная лестница проверок, последовательно.

### 6.2 APK на девайс — ручной прогон (расширен по ревью плана): три состояния из фреймов + edit слова + удаление с Undo + flush-on-back + стек лексем (черновик сверху + несколько сохранённых) + несколько компонентов в одной лексеме (фильтрация чипов, лейбл секции) + диалоги удаления (стилизация 5.2) + снеки Undo/ошибки визуально + FAB-disabled в pending (canAddLexeme при isPendingDbOp)

**Почему:** тени/бордер/бейдж/FAB — визуальные вещи, unit-тестами не ловятся; flush-on-back и снеки — механика, которую рестайл мог задеть композицией; стек и мульти-компоненты — состояния вне фреймов, где новая композиция впервые встретится с реальными данными.

## Что осознанно НЕ делается

- Reducer-механика (commit/flush/чипы/undo) — не трогается: макеты воспроизводят текущее поведение.
- Общие `core:ui`-виджеты — не трогаются (внешние потребители; прецедент create_dictionary, decision №4 той фичи).
- Логирование `###WordCard###` — не добавляется (не было и нет в скоупе).
- Диалоги/снеки — без перестройки, только палитра.

## Оценка

~17 изменённых/новых файлов: 12 UI wordcard + Term/UseCaseImpl/State/Reducer (~15 строк логики) + Color.kt + strings ×2 + 2 теста. Средняя сложность; критический путь — волна 2 (единственная логика) и 4.5 (шапка с флагом).
