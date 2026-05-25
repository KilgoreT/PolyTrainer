# contract_state — IS479 wordcard_lexeme_inline (v2.5)

## Режим работы

**Режим 1 — макет-driven.**

Источники, доступные по факту:
- Спека: `spec_filename = null` — отсутствует.
- Макет: ЕСТЬ. Figma `w8GmGCdOZJUi99Cuv4q4W9`, frames:
  - `9154-82532` — FAB «Добавить значение» (только icon, без label, accent-цвет).
  - `9154-82519` — карточка существующей лексемы: chip «Перевод» (раскрыт с tui-input), chip «Определение» (свёрнутый), chip «Пример» (исключаем по брифу), кнопка «Удалить».
  - `9154-82521` — chip «Перевод» (образец стиля раскрытого chip с trailing close-rounded и leading icon).
  - `9154-82625` — зона невыбранных chip'ов «Определение» / «Пример».
- Код: ЕСТЬ. `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/State.kt`.

Приоритет — макет. Он определяет шейп нового State. Код используется как «что есть сейчас» для diff. Спека отсутствует и в этом шаге не создаётся (это задача `publish_spec`).

### Прочтение макета (ключевой момент для State)

Frame `9154-82519` — карточка одной существующей лексемы с inline-редакторами полей. Frame `9154-82532` — отдельный FAB «Добавить значение» (только icon).

**Семантическое прочтение замысла:**

1. FAB создаёт **новую пустую лексему сразу локально в state** (без промежуточного диалога/чекбоксов «выбери что хочешь», без обращения к БД). Новая лексема добавляется в `lexemeList` с `id = NOT_IN_DB`, `translation = null`, `definition = null`.
2. Дальше пользователь внутри карточки конкретной лексемы (`9154-82519`) chip'ами «Перевод» / «Определение» создаёт нужные суб-сущности через уже существующие сообщения (`CreateTranslation` / `CreateDefinition`). Запись в БД происходит **только на первом Commit Translation/Definition**: handler делает инсерт лексемы + соответствующей суб-сущности и возвращает лексему с реальным id; reducer заменяет в `lexemeList` элемент с `id = NOT_IN_DB` на возвращённый.
3. Промежуточный экран «выбери чекбоксами что нужно перед созданием лексемы» **исчезает целиком** — нет ни bottom sheet, ни inline-формы с чекбоксами.

**Возражение на альтернативу:** теоретически можно оставить промежуточный inline-блок с чекбоксами `isTranslationCheck` / `isDefinitionCheck` (выкинуть только bottom sheet wrapper). Отвергаю:
- Figma `9154-82519` показывает карточку **существующей** лексемы с inline-редакторами, а не «форму создания».
- Семантика «создай пустую сущность, добавляй контент chip'ами» уже реализована для translation/definition внутри `LexemeState` — переиспользуем механику без дублирования.
- Убирает целую ось состояния (`AddLexemeBottomState` целиком) — снижение сложности State.

Если бизнес-смысл другой — оспаривать на ревью.

## State

```kotlin
package me.apomazkin.wordcard.mate

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import me.apomazkin.mate.EMPTY_STRING
import me.apomazkin.wordcard.entity.Lexeme
import me.apomazkin.wordcard.entity.Term
import java.util.Date

const val NOT_IN_DB = -1L

@Stable
data class WordCardState(
    val topBarState: TopBarState = TopBarState(),
    val isLoading: Boolean = true,
    val isPendingDbOp: Boolean = false,
    val wordState: WordState = WordState.NotLoaded,
    val lexemeList: List<LexemeState> = listOf(),
    val snackbarState: SnackbarState = SnackbarState()
)

// computed: «у этого слова есть незавершённое создание новой лексемы».
// Локальная неподтверждённая лексема живёт прямо в lexemeList с id = NOT_IN_DB.
val WordCardState.isCreatingLexeme: Boolean
    get() = lexemeList.any { it.id == NOT_IN_DB }

@Stable
data class TopBarState(
    val isMenuOpen: Boolean = false
)

sealed interface WordState {
    data object NotLoaded : WordState

    @Stable
    data class Loaded(
        val id: Long,                  // != NOT_IN_DB по конструкции
        val added: Date,               // non-null по конструкции
        val value: String,             // non-empty по контракту Loaded
        val isEditMode: Boolean = false,
        val edited: String = "",
        val showWarningDialog: Boolean = false,
    ) : WordState
}

@Stable
data class LexemeState(
    val id: Long = NOT_IN_DB,
    val translation: TextValueState? = null,
    val definition: TextValueState? = null,
    val isMenuOpen: Boolean = false,
)

@Stable
data class TextValueState(
    val isEdit: Boolean = false,
    val origin: String = "",
    val edited: String = origin,
)

@Immutable
data class SnackbarState(
    val title: String = EMPTY_STRING,
    val show: Boolean = false,
)
```

> 📎 guide: docs/guides/state-and-extensions.md — "Иммутабельные data class'ы с аннотацией @Stable или @Immutable для оптимизации Compose"
> 📎 guide: docs/guides/state-and-extensions.md — "Иерархическая вложенность: корневой стейт экрана содержит вложенные под-стейты"
> 📎 guide: docs/guides/state-and-extensions.md — "Дефолтные значения для всех полей — безопасное создание через конструктор без аргументов"
> 📎 guide: docs/guides/state-and-extensions.md — "Корневой стейт — одно поле на каждый UI-регион (topBar, контент, bottomSheet, snackbar)"
> 📎 guide: docs/guides/state-and-extensions.md — "Списки: List<ItemState>, не List<DomainModel>"
> 📎 guide: docs/guides/state-and-extensions.md — "NOT_IN_DB = -1L для неинициализированных ID"
> 📎 guide: docs/guides/state-and-extensions.md — "TextValueState — паттерн для toggle edit/view: origin (сохранённое) + edited (в процессе) + isEdit (режим)"
> 📎 guide: docs/guides/state-modeling.md — "Top-level state = data class; внутри — sealed class когда нужно XOR"
> 📎 guide: docs/guides/state-modeling.md — "State — данные, не объекты: нет поведения, нет identity, нет наследования; только immutable факты"

### Per-field описание

#### `WordCardState` (top-level)

- **`topBarState: TopBarState`** — Что: суб-стейт верхней панели (меню Edit/Delete word).
- **`isLoading: Boolean`** — Что: флаг первичной загрузки данных слова из БД. Простой паттерн (см. memory `feedback_simple_state_loading`). Не сворачиваем в sealed.
> 📎 guide: docs/guides/state-modeling.md — "Loadable<T> = data class(status, value?) — компактнее sealed; data class и data class + status эквивалентны по выраженности домена"
- **`isPendingDbOp: Boolean`** — глобальный pending-флаг БД-операции. `true` ⇒ Datasource Effect отправлен, reducer ожидает confirm-Msg (`Refresh*` / `*Failed` / `ShowNotification` / `NavigateBack` / `LexemeCascadeRemoved`). Поле управляется только из UI Msg-веток шлющих Effect (→ true) и из Datasource Msg reducer-веток (→ false). UI читает поле для блокировки кнопок.

  Обоснование выбора (хранимое vs computed): не computed. Причин три. (1) Pending-состояние не выводится из других полей state — Effect может «улетать» при состоянии state, неотличимом от «не pending» (например `Update*Translation` при `lexeme.translation.isEdit = false` после reducer-мутации). (2) Универсально — закрывает race для **всех** Effect, не только NOT_IN_DB. (3) Reducer-симметрия: set/clear ровно в Effect-sending/confirm Msg.
- **`wordState: WordState`** — Что: суб-стейт самого слова (заголовок карточки).
- **`lexemeList: List<LexemeState>`** — Что: упорядоченная последовательность лексем слова. Почему `List`, а не `Map<Id, LexemeState>` или `Set`: порядок важен для UI, дубли по id невозможны (id из БД), стоимость линейного поиска при `update` амортизируется небольшим количеством лексем у одного слова.
> 📎 guide: docs/guides/state-modeling.md — "Для описания state чаще всего хватает List/Map/Set; выбирать тип под семантику (List — упорядоченная последовательность, Set — принадлежность, Map — отображение)"
- **`snackbarState: SnackbarState`** — Что: транзиентный UI-сигнал (один на экран).

**Empty state.** Отдельного поля или sealed-variant для пустого списка лексем **нет**. Конвенция: пустой `lexemeList` уже различим типом (`List<LexemeState>.isEmpty()`) в комбинации с `isLoading`. UI-слой рендерит placeholder по предикату `lexemeList.isEmpty() && !isLoading`. Не плодим хранимое поле под derived.
> 📎 guide: docs/guides/state-modeling.md — "Empty state как отдельный variant не нужен: Success(items = emptyList()) уже различим через items.isEmpty()"

**Удалено:** `addLexemeBottomState: AddLexemeBottomState` — поле и тип уничтожаются. См. раздел «Удаляемые поля / extensions / computed».

#### `WordState` (sealed — Loaded vs NotLoaded)

Sum-тип (whitelist (d) — sealed вместо product). Жёсткая корреляция «id ↔ added ↔ value» в исходном data class раскрывалась тремя инвариантами одновременно; sealed закрывает их **компилятором**:

- **`WordState.NotLoaded`** — Что: начальное состояние до прихода `WordLoaded`. Не имеет полей `id / added / value` — обращение к ним вне Loaded физически невозможно. Дефолт `WordCardState.wordState = WordState.NotLoaded`.
- **`WordState.Loaded(id, added, value, isEditMode, edited, showWarningDialog)`** — Что: слово получено из БД. Поля:
  - `id: Long` — гарантированно `!= NOT_IN_DB` по конструкции (`NotLoaded` отдельный variant).
  - `added: Date` — гарантированно non-null по конструкции.
  - `value: String` — текст слова. По контракту Loaded предполагается непустым (БД не хранит пустых слов).
  - `isEditMode: Boolean` — режим редактирования заголовка.
  - `edited: String` — буфер ввода. Связан инвариантом 1 (применим только когда `wordState is WordState.Loaded`): `isEditMode == false ⇒ edited == ""`.
  - `showWarningDialog: Boolean` — флаг отображения диалога подтверждения удаления слова.

**Почему sealed, а не data class + 3 инварианта:**
- Жёсткая семантическая корреляция: либо все три (`id, added, value`) валидны, либо ничего нет. Это XOR — каноничный кейс для sum.
- Подсчёт вариантов уровня 1 для `(id × added × value)` показал 2 валидных комбинации из 8 (NotLoaded vs Loaded) — sealed выражает это типом точно.
- Корреляция трёх полей `id ↔ added ↔ value` (либо все валидны и присутствуют — `Loaded`, либо ничего нет — `NotLoaded`) закрывается компилятором. Не требует отдельных инвариантов.

**Trade-off (обоснование выбора):** требует переписать reducer-ветки (везде `when (state.wordState)`) + всех читателей. Это в скоупе `contract_ui_msg` / `implement` шагов IS479. Принимается осознанно для type-safety.

> 📎 guide: docs/guides/state-modeling.md — "Top-level state = data class; внутри — sealed class когда нужно XOR"

#### `LexemeState`

Без изменений в шейпе. Сохраняем существующие поля:
- **`id: Long`** — Что: идентификатор лексемы из БД, `NOT_IN_DB` до первой записи. Тип остаётся `Long` (а не sealed `LexemeId.Local | Remote`) — `NOT_IN_DB = -1L` это **существующая конвенция кодбазы** (объявлена в `modules/screen/wordcard/.../mate/State.kt`), новые типы не вводим.
- **`translation: TextValueState?`** — Что: суб-стейт перевода лексемы. Почему nullable (whitelist (a) — nullable как ось координат): «перевод не задан» — отдельное семантическое состояние, не пустая строка. `null` ≠ `TextValueState(origin="", edited="")`. `null` — суб-сущность не существует; пустая строка — суб-сущность существует, но текст пустой (промежуточное состояние редактирования).
> 📎 guide: docs/guides/state-modeling.md — "Семантика nullable: T? — может быть null на момент создания state или в течение flow; точнее моделирует «не задан» против пустого значения"
- **`definition: TextValueState?`** — Что: суб-стейт определения лексемы. Та же логика что и `translation`.
- **`isMenuOpen: Boolean`** — Что: контекстное меню одной лексемы (например, удаление).

**Константа `NOT_IN_DB = -1L`** (объявлена в существующем `State.kt`). Используется как маркер «лексема ещё не в БД». `LexemeState(id = NOT_IN_DB, translation = null, definition = null, isMenuOpen = false)` — допустимое **временное** состояние новой лексемы между тапом FAB «+ Лексема» и первым Commit Translation/Definition.

Архитектурный пересмотр (ит.5): лексема создаётся в БД **только** при первом Commit Translation/Definition, а не при тапе FAB. До этого момента она живёт **локально в state** с `id = NOT_IN_DB`. После Commit handler возвращает лексему с реальным id, reducer заменяет `NOT_IN_DB` на реальный.

Семантика «локальной» лексемы:
- `translation = null` и `definition = null` — допустимо как временное состояние сразу после тапа FAB.
- Активный edit достигается только через `CreateTranslation` / `CreateDefinition` UI Msg, который ставит `translation = TextValueState(origin = "", isEdit = true)` (или аналогично для `definition`) **локально**, без обращения к БД.
- При первом Commit Translation/Definition handler выполняет инсерт лексемы + соответствующей суб-сущности; возвращает лексему с реальным id; reducer заменяет в `lexemeList` элемент с `id = NOT_IN_DB` на возвращённый.

#### `TextValueState`

Edit-сценарий с двумя осями `origin` / `edited` — whitelist триггер (b):
- **`isEdit: Boolean = false`** — Что: режим редактирования поля (`true` — текстовый input виден и активен; `false` — отображается значение из `origin`). Почему дефолт `false` (whitelist (e) — изменение существующего дефолта):
  - Дефолт `false` — безопасный: «свежесозданный или загруженный из БД `TextValueState` по умолчанию **не в edit-mode**». Случайный `TextValueState()` не создаёт фоновый edit-режим.
  - `TextValueState` создаётся в двух семантических ситуациях: (1) маппинг из БД (`Lexeme.toLexemeState()`) — дефолт `false` совпадает с ожиданием view-mode; (2) пользователь нажал chip «Перевод» / «Определение» (вход в режим создания) — reducer **обязан явно** ставить `isEdit = true`.
  - В прошлой версии был дефолт `true`, что закрывало шум при создании через chip (один writer переопределял один параметр), но открывало риск: если БД-маппер забудет переопределить `isEdit`, лексема придёт сразу в edit-mode без видимого триггера. Дефолт `false` снимает этот риск ценой одного явного `isEdit = true` в reducer-ветке `Create*`.
- **`origin: String`** — Что: последнее значение, синхронизированное с БД (last known good).
- **`edited: String`** — Что: текущий ввод пользователя. Почему два поля: при отмене редактирования нужно вернуть `origin`; при коммите — записать `edited` в БД и обновить `origin`. Один `value` это не поддерживает.
> 📎 guide: docs/guides/state-modeling.md — "Редактируемые поля держать отдельно от domain model: оригинал нетронут, edited — feature-level; позволяет undo и предотвращает конфликт с backend"

#### Прочие nested без изменений

`TopBarState`, `SnackbarState` — структурно остаются. Их поля тривиальны (выходят за whitelist триггеры), описание не дублирую. `WordState` переделан на sealed — см. отдельный раздел выше.

### Computed properties

**Вводится один computed на `WordCardState`:**

- `WordCardState.isCreatingLexeme: Boolean` = `lexemeList.any { it.id == NOT_IN_DB }`
  - Назначение: семантический предикат «у этого слова есть незавершённое создание новой лексемы».
  - Почему computed, а не хранимое поле: source of truth — само наличие лексемы с `id = NOT_IN_DB` в `lexemeList`. Хранимый `isCreatingLexeme: Boolean` дублировал бы эту информацию и создавал риск рассинхронизации (флаг = true, но лексемы нет, или наоборот). По принципу нормализации state — не дублируем derived.
  - До ит.5 значение было хранимым полем с обоснованием «не оптимистичная лексема в list». После архитектурного пересмотра (лексема создаётся в БД только при первом Commit*) **локальная лексема с `NOT_IN_DB` в `lexemeList` — норма**, поэтому хранимый флаг становится избыточным.
> 📎 guide: docs/guides/state-and-extensions.md — "Computed properties для derived полей: если значение выводится из других полей State — оформлять как extension val с custom getter, не хранить в data class"
> 📎 guide: docs/guides/state-modeling.md — "Computed property вместо хранимого вычисляемого поля: source of truth — только исходные поля, derived — view/selector"

Существующие хелперы (`TextValueState.toValue(isEdit)`, `TextValueState.isChanged()`) сохраняются — это extension-функции на `TextValueState`, не computed на `WordCardState`. Их роль — view-selector'ы для UI.

> 📎 guide: docs/guides/state-modeling.md — "Selector = функция, превращающая State в проекцию для конкретной view; State и его проекции — разные"

> 📎 guide: docs/guides/state-and-extensions.md — "Computed properties для derived полей: если значение выводится из других полей State — оформлять как extension val с custom getter, не хранить в data class"
> 📎 guide: docs/guides/state-modeling.md — "Computed property вместо хранимого вычисляемого поля: source of truth — только исходные поля, derived — view/selector"

Кандидаты, которые НЕ вводим (с обоснованием почему):

- `WordCardState.hasAnyLexeme: Boolean` (для скрытия пустого состояния) — не нужно на уровне State, UI определит по `lexemeList.isEmpty()` напрямую (см. раздел Empty state выше). Не плодим.
- `WordCardState.isFabEnabled: Boolean` (= `!isCreatingLexeme && !isLoading`) — пока не вводим, UI считает на месте через уже введённый computed `isCreatingLexeme`. Если предикат начнёт повторяться в нескольких местах — выделим в extension val в отдельной задаче.

### Раздел: Удаляемые поля / extensions / computed

Это переделка существующего State — раздел обязателен.

**Удаляется поле в `WordCardState`:**
- `addLexemeBottomState: AddLexemeBottomState` — больше не существует промежуточный UI «выбери что добавить». FAB создаёт пустую лексему, внутри карточки лексемы существующие chip'ы создают нужные translation/definition. Триггер (e) — удаление поля.

**Не добавляется хранимое поле:**
- `isCreatingLexeme: Boolean` — **не вводится** как поле data class. Семантика «у этого слова есть незавершённое создание новой лексемы» выражается computed property через наличие `LexemeState(id = NOT_IN_DB, ...)` в `lexemeList` (см. секцию Computed properties). До ит.5 предполагалось хранимое поле — отменено архитектурным пересмотром: лексема живёт локально в `lexemeList` до первого Commit*.

**Добавляется computed:**
- `WordCardState.isCreatingLexeme: Boolean` — extension val, derived из `lexemeList`. Подробности — в секции Computed properties.

**Удаляется тип:**
- `data class AddLexemeBottomState(show, isTranslationCheck, isDefinitionCheck)` — целиком. Все три поля семантически принадлежали удаляемому промежуточному UI.

> 📎 guide: docs/guides/state-and-extensions.md — "State = только отображаемое; никаких навигационных флагов и кэшей данных для вычислений в state"

**Удаляются extensions:**
- `WordCardState.showAddLexemeBottom()` — нет «открытого» состояния промежуточного UI.
- `WordCardState.hideAddLexemeBottom()` — нет «закрытого» состояния.
- `WordCardState.setTranslationCheck(checked: Boolean)` — нет чекбокса «нужен перевод».
- `WordCardState.setDefinitionCheck(checked: Boolean)` — нет чекбокса «нужно определение».
> 📎 guide: docs/guides/state-and-extensions.md — "Extension-функции для всех мутаций стейта; нейминг — глагол в начале (show, hide, enable, disable, update, set)"

**Не удаляется (важно):**
- `LexemeState` и весь блок specialized lexeme extensions (`createLexemeTranslation`, `createLexemeDefinition`, `refreshLexemeTranslation` и т.д.) — переиспользуется механика chip'ов внутри лексемы.
- `addLexeme(lexeme: LexemeState)` — нужна для добавления непустой лексемы (с id из БД) после возврата `RefreshLexeme(lexeme)` от data-слоя. Конкретные сигнатуры reducer-веток — в `contract_ui_msg`.

## Инварианты

Только **синхронные структурные утверждения о форме state** — каждое проверяемо по одному snapshot без знания истории reduce.

**Что закрыто типом (sealed `WordState`)** — не требует инвариантов:
- `WordState.NotLoaded` не имеет полей `id / added / value` ⇒ невалидные комбинации `(NOT_IN_DB, non-empty value)`, `(valid id, null added)`, `(valid id, "" value)` физически невозможны.
- Корреляция `id ↔ added ↔ value` (либо все три есть в `Loaded`, либо ни одного в `NotLoaded`) удерживается компилятором без отдельных инвариантов.

### Инвариант 1: буфер `wordState.edited` пуст вне edit-mode (только в `Loaded`)

```
wordState is WordState.Loaded ∧ wordState.isEditMode == false  ⇒  wordState.edited == ""
```

**Что:** буфер ввода `edited` имеет смысл только когда `isEditMode == true`. Вне режима редактирования буфер пуст. `NotLoaded` не имеет `edited` (закрыто типом).

**Snapshot test:** два поля одного `wordState.Loaded` на одной фотографии. ✅ Валидный структурный инвариант.

**Зачем:** удерживает «отсутствие незавершённой работы» наблюдаемым по форме state.

### Инвариант 2: в списке допустима максимум одна локальная (NOT_IN_DB) лексема

```
| { l ∈ lexemeList : l.id == NOT_IN_DB } | ≤ 1
```

**Что:** в `lexemeList` может присутствовать **не более одной** лексемы с `id = NOT_IN_DB` (свежесозданная локальная лексема, ещё не записанная в БД). Все остальные лексемы — с реальным id из БД.

**Snapshot test:** обход списка, подсчёт элементов с `id == NOT_IN_DB`. ✅ Валидный.

**Зачем:** UX-инвариант: FAB заблокирован пока есть локальная неподтверждённая лексема (через computed `isCreatingLexeme`), значит две локальные одновременно невозможны. Compose-ключи `items(... key = { it.id })` всё ещё корректны: один `NOT_IN_DB` уникален в пределах списка.

**Изменение vs ит.4:** до ит.5 инвариант звучал «∀ l: l.id != NOT_IN_DB» (полный запрет). Архитектурный пересмотр (лексема создаётся в БД только при первом Commit*) делает локальную `NOT_IN_DB`-лексему легитимным временным состоянием — запрет ослаблен до «≤ 1».

> 📎 guide: docs/guides/state-modeling.md — "Использовать ADT и точно моделировать домен — исключать невалидные состояния на уровне типов/инвариантов"

### Инвариант 3: уникальность id лексем

```
∀ l1, l2 ∈ lexemeList : l1 != l2 ⇒ l1.id != l2.id
```

**Snapshot test:** обход списка на одном снимке. ✅ Валидный.

**Зачем:** гарантирует корректность ключей в Compose `items(...)` и однозначность `updateLexeme(lexemeId, ...)`.

**Ремарка про `NOT_IN_DB`:** `NOT_IN_DB` тоже подпадает под это правило — может быть **не более одной** лексемы с `id = NOT_IN_DB` одновременно (см. инв.2). Таким образом два инварианта вместе гарантируют, что любой `id` (включая `NOT_IN_DB`) уникален в `lexemeList`.

### Инвариант 4: буфер `TextValueState.edited` пуст вне edit-mode

```
∀ l ∈ lexemeList :
  l.translation != null ∧ l.translation.isEdit == false ⇒ l.translation.edited == ""
  l.definition  != null ∧ l.definition.isEdit  == false ⇒ l.definition.edited  == ""
```

**Что:** `edited` — буфер ввода. Вне edit-mode (`isEdit == false`) буфер пуст.

**Snapshot test:** для каждой лексемы проверить `translation?.isEdit == false ⇒ translation.edited == ""` (то же для definition). ✅ Валидный.

**Симметрия:** аналогично инварианту 1 для `wordState.Loaded`. Паттерн «editable text с origin/edited» применяется одинаково на двух уровнях.

**Жизненный цикл `edited`:**
- `EnterEditMode`: `edited = origin`.
- `UpdateInput(text)`: `edited = text`.
- `Commit / Cancel / Refresh` (выход из edit-mode): `isEdit = false`, `edited = ""`.

### Инвариант 5: непустой `lexemeList` принадлежит загруженному слову (cross-field)

```
lexemeList.isNotEmpty()  ⇒  wordState is WordState.Loaded
```

**Что:** лексемы могут существовать в state только когда слово уже загружено. Лексемы у `NotLoaded` — нарушение FK на уровне state.

**Snapshot test:** длина списка + тип `wordState`. ✅ Валидный.

**Зачем:** инвариант 2 защищает id **внутри** lexemeList, но не cross-field «лексемы у слова без id». Этот инвариант — структурный FK.

### Инвариант 6: создание лексемы возможно только при загруженном слове

```
(∃ l ∈ lexemeList : l.id == NOT_IN_DB)  ⇒  wordState is WordState.Loaded
```

(эквивалентно через computed: `isCreatingLexeme ⇒ wordState is WordState.Loaded`)

**Snapshot test:** обход списка + тип `wordState`. ✅ Валидный.

**Зачем:** закрывает невалидную комбинацию «локальная NOT_IN_DB-лексема при `NotLoaded`-слове». Это частный случай инв.5 (`lexemeList.isNotEmpty() ⇒ Loaded`), оставлен явно — UX-замысел «создание лексемы привязано к загруженному слову» наблюдаем напрямую. Дополнительная гарантия `!isLoading` следует производно через инвариант 11 (`isLoading ⇒ NotLoaded`) — отдельно не требуется.

### Инвариант 7: первичная загрузка эксклюзивна с непустым списком лексем (производный)

```
isLoading == true  ⇒  lexemeList.isEmpty()
```

**Snapshot test:** два поля. ✅ Валидный.

**Зачем:** **производный** инвариант — следует из инв.11 (`isLoading ⇒ NotLoaded`) + инв.5 контрапозиция (`NotLoaded ⇒ lexemeList.isEmpty()`). Цепочка: `isLoading ⇒ NotLoaded ⇒ empty`. Оставлен явно для читабельности и self-check без необходимости выводить через два других инварианта.

### Инвариант 8: snackbar показывает только непустой title

```
snackbarState.show == true  ⇒  snackbarState.title != ""
```

**Snapshot test:** два поля `snackbarState`. ✅ Валидный.

**Зачем:** защищает UI от пустой плашки.

### Инвариант 9: глобальный single-edit-mode

```
let wordEdit = if (wordState is WordState.Loaded ∧ wordState.isEditMode) 1 else 0
let lexEdits = Σ_{l ∈ lexemeList} (
    (if (l.translation != null ∧ l.translation.isEdit) 1 else 0)
    + (if (l.definition != null ∧ l.definition.isEdit) 1 else 0)
)

wordEdit + lexEdits  ≤  1
```

**Что:** во всём state одновременно может быть активен максимум **один** edit-mode. Либо редактируется заголовок слова, либо одно конкретное `translation`/`definition` одной конкретной лексемы — но не несколько одновременно.

**Snapshot test:** обход всего state — сумма булевых флагов `isEdit*` ≤ 1. По одному снимку. ✅ Валидный (хоть и нелокальный, проверка O(|lexemeList|)).

**Зачем:** UX-замысел Android — один TextField в фокусе IME. Если архитектурно не закрыть — Reducer случайно может оставить два `isEdit=true` (например `EnterTranslationEditMode` забыл закрыть `wordState.isEditMode`), что породит несинхронный двойной фокус и неоднозначность «какой Commit/Cancel сейчас выполнять». Инвариант делает single-focus **наблюдаемым по форме state**, а не невидимым контрактом reducer-а.

**Закрытие в reducer (для следующего шага `contract_ui_msg`):** любой `EnterXEditMode` обязан в reducer-логике закрыть все остальные edit-mode флаги в одном reduce-шаге. То же для `Create*` (создание `TextValueState` с `isEdit=true` обязано закрыть прочие).

**Корректность для локальной (NOT_IN_DB) лексемы:** свежесозданная локальная лексема имеет `translation = null` и `definition = null` — оба `isEdit`-флага отсутствуют, в сумму не вносят. Активный edit появляется только после `CreateTranslation`/`CreateDefinition`, который явно ставит `isEdit = true` и обязан закрыть прочие. Инвариант сохраняется.

### Инвариант 10: `WordState.Loaded.value` непустой

```
wordState is WordState.Loaded  ⇒  wordState.value != ""
```

**Что:** загруженное слово имеет непустой текст. Пустое слово в БД не хранится (контракт data-слоя).

**Snapshot test:** проверяется при типизации `wordState as WordState.Loaded` + длина value. ✅ Валидный.

**Зачем:** sealed закрывает наличие поля `value`, но не его непустоту. Type system не выражает «non-empty String» — нужен инвариант. Закрывает дыру «`Loaded(value="")` возможен по типу».

### Инвариант 11: первичная загрузка эксклюзивна со статусом `Loaded`

```
isLoading == true  ⇒  wordState is WordState.NotLoaded
```

**Что:** первичная загрузка идёт только пока слово ещё не получено. После `WordLoaded` (статус `Loaded`) `isLoading` сразу `false`. Re-load для уже загруженного — невалиден.

**Snapshot test:** два поля. ✅ Валидный.

**Зачем:** закрывает комбинацию `(Loaded, isLoading=true)`, которая иначе валидна по форме. Усиливает инвариант 7 в обратную сторону.

### Инвариант 12: `isPendingDbOp` согласован с эффект-каналом (runtime, не snapshot)

```
isPendingDbOp == true  ⇒  был отправлен хотя бы один pending Datasource Effect,
                           для которого reducer ещё не получил confirm-Msg
```

**Что:** `isPendingDbOp` отражает наличие неподтверждённого Datasource Effect в эффект-канале.

**Snapshot test:** ❌ невозможен — это **runtime-инвариант reducer-логики**, не snapshot-инвариант. Проверка требует знания истории отправленных Effect и пришедших confirm-Msg, которое не присутствует в одном снимке State.

**Зачем:** зафиксирован для отслеживания. Не входит в множество структурных snapshot-инвариантов (1–11), но именно он обосновывает почему `isPendingDbOp` — хранимое поле, а не computed: source of truth (эффект-канал) лежит вне State, дублирование в state — единственный способ сделать pending наблюдаемым для UI/reducer.

### Self-check по запрещённым формулировкам

Прошёл каждый инвариант на запрещённые формулировки из step file:

- ✅ Нет «после `Msg.X` поле Y = Z».
- ✅ Нет «поле Y сбрасывается при `Msg.X`».
- ✅ Нет «не имеет права писать в Y кроме как из reducer'а X».
- ✅ Нет «после Refresh поле A синхронизируется с B».
- ✅ Нет «при коммите Y становится Z».
- ✅ Нет упоминаний Msg, reducer-веток, эффектов.

Только утверждения о форме state на одной фотографии.

### Кандидаты-инварианты, которые НЕ фиксируются (с разбором)

Чтобы зафиксировать почему именно эти 11 инвариантов, а не больше:

- **«`l.translation != null ∧ l.translation.isEdit == false ⇒ l.translation.edited == l.translation.origin»`** — формально структурный (по snapshot), но **закрывает легитимное transient-окно** между Commit (пользователь нажал «Сохранить» → `isEdit=false`) и Refresh (data-слой вернул новое `origin`). В этом окне reducer уже выставил `isEdit=false`, но `origin` ещё не обновился — `edited` отличается от `origin`. По новым правилам transient-окна — норма, не нарушение шейпа. Не фиксируем как инвариант. Если хотим закрыть окно — нужно ввести новое поле State (например `pendingValue: String?` или флаг `isCommitting: Boolean`) — на текущем уровне фичи это over-engineering.
- **«Порядок lexemeList сохраняется при мутациях»** — это **поведение reducer'а** (как именно reducer пишет в список). Не проверяется по одному снимку — нужна история двух снимков и знание Msg между ними. Не state-инвариант. Тип `List<LexemeState>` уже выражает структурное «упорядоченная последовательность».
- **«`isCreatingLexeme == true ⇒ FAB заблокирован»`** — это **UI-инвариант** (правило рендера), не state-инвариант. State не знает про FAB. То что UI рендерит disabled FAB при флаге — отдельный контракт UI sub-flow.
- **«`isCreatingLexeme` сбрасывается атомарно при success/failure»`** — это **reducer-rule** (writers-policy), не структурный инвариант. По snapshot нельзя проверить «сбросится ли в будущем». Удалено.
- **«`snackbarState.show == false ⇒ snackbarState.title не отображается»`** — UI-инвариант, не state. State не запрещает `show=false ∧ title="..."`; UI рендерит по флагу `show`. Не фиксируем.

## Расхождения spec ↔ code

**Не применимо.** Режим работы — 1 (макет-driven), не 2 (spec-driven). Спека отсутствует (`spec_filename = null`). Сверка кода со спецификацией не производится; источник истины — Figma макет. Создание/обновление спеки делегируется шагу `publish_spec`.

---

## Лог итераций

### ит.5 (2026-05-19T20:00:00-0600) — мини-патч NOT_IN_DB
Архитектурный пересмотр: лексема создаётся в БД при первом Commit*, не при тапе FAB.
Добавлена семантика локальной NOT_IN_DB лексемы. Убран флаг `isCreatingLexeme` (хранимое поле) — заменён на computed property `WordCardState.isCreatingLexeme = lexemeList.any { it.id == NOT_IN_DB }`.
Инв.2 ослаблен: вместо «∀ l: l.id != NOT_IN_DB» теперь «≤ 1 лексема с id = NOT_IN_DB». Инв.3 дополнен ремаркой про NOT_IN_DB. Инв.6 переформулирован через первичный источник истины (lexemeList) вместо удалённого поля. Инв.9 — корректность для локальной лексемы зафиксирована явно.
Версия v2.3 → v2.4.

### ит.6 (2026-05-19T20:45:00-0600) — мини-патч isPendingDbOp
Добавлен глобальный pending-флаг БД-операции. UI блокирует кнопки при true.
Эксперимент: проверить universal-guard вместо локальных guard'ов в reducer per-Effect.
Версия v2.4 → v2.5.

Состав патча:
- Поле `isPendingDbOp: Boolean = false` в `WordCardState` рядом с `isLoading` (top-level флаг).
- Per-field описание `isPendingDbOp` с обоснованием хранимое vs computed (три причины: не выводится из state; универсальный guard для всех Effect; reducer-симметрия set/clear).
- Инв.12 «`isPendingDbOp` согласован с эффект-каналом» — помечен как **runtime-инвариант reducer-логики**, не snapshot. Зафиксирован для отслеживания, проверка по одному снимку невозможна.
- Computed `isCreatingLexeme` оставлен как есть — другая семантика: «есть локальная пустая лексема» vs «отправлен effect, ждём ответ».
- Existing инварианты 1–11 не тронуты.

Замечание о «cross-field top-level подсчёте»: в артефакте отсутствует выделенный раздел подсчёта вариантов на уровне `WordCardState` (есть только локальный подсчёт для `WordState` в обосновании sealed). Отдельный раздел для top-level подсчёта не вводится в этой итерации (out-of-scope мини-патча); `isPendingDbOp` как новая булева ось удваивает мощность top-level комбинаций — это утверждение фиксируется здесь в логе и подлежит формализации в отдельной итерации если потребуется.

---

_model: claude opus 4.7 (1M context)_
