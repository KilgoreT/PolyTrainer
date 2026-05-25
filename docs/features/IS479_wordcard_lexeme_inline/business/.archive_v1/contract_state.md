# contract_state — IS479 wordcard_lexeme_inline

## Режим работы

**Режим 1 — макет-driven.**

Источники, доступные по факту:
- Спека: `spec_filename = null` — отсутствует.
- Макет: ЕСТЬ. Figma `w8GmGCdOZJUi99Cuv4q4W9`, frames `9154-82509` (общий экран), `9154-82519` (карточка лексемы inline), `9154-82532` (FAB), `9154-82521` (chip «Перевод» — образец стиля), `9154-82625` (зона chip-кнопок).
- Код: ЕСТЬ. `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/State.kt`.

Приоритет — макет. Он определяет шейп нового State. Код используется как «что есть сейчас» для diff. Спека отсутствует и в этом шаге не создаётся (это задача `publish_spec`).

### Прочтение макета (ключевой момент для State)

Frame `9154-82509` показывает экран `word card` с двумя FRAME-сущностями верхнего уровня:
1. **`word header`** — шапка со словом (не трогаем).
2. **`Lexeme` (frame `9154-82519`)** — карточка одной существующей лексемы с inline-редакторами полей: chip «Перевод» (раскрыт с tui-input), chip «Определение» (свёрнутый), chip «Пример» (исключаем по брифу), кнопка «Удалить».
3. **`FAB` (frame `9154-82532`)** — отдельная плавающая кнопка «Добавить значение».

**Семантическое прочтение:** FAB создаёт новую пустую лексему сразу (без промежуточного диалога/чекбоксов). Дальше пользователь внутри карточки лексемы chip'ами «Перевод»/«Определение» создаёт нужные суб-сущности через уже существующие сообщения `CreateTranslation` / `CreateDefinition`. Промежуточный экран «выбери чекбоксами что нужно перед созданием лексемы» **исчезает**.

**Возражение и почему всё-таки так:** Альтернатива — оставить inline-форму с чекбоксами (без bottom sheet, но с теми же двумя флагами). Отверг: (a) Figma `9154-82519` показывает именно карточку существующей лексемы с inline-редакторами, а не «форму создания»; (b) семантика «создай сначала пустую сущность, потом добавляй к ней содержимое chip'ами» уже реализована для translation/definition внутри `LexemeState` — переиспользуем; (c) убирает целый промежуточный экран и устраняет лишнюю ось состояния. Если бизнес-смысл другой — оспаривать на ревью.

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
    val isCreatingLexeme: Boolean = false,
    val wordState: WordState = WordState(),
    val lexemeList: List<LexemeState> = listOf(),
    val snackbarState: SnackbarState = SnackbarState()
)

@Stable
data class TopBarState(
    val isMenuOpen: Boolean = false
)

@Stable
data class WordState(
    val id: Long = NOT_IN_DB,
    val added: Date? = null,
    val value: String = "",
    val isEditMode: Boolean = false,
    val edited: String = "",
    val showWarningDialog: Boolean = false,
)

@Stable
data class LexemeState(
    val id: Long = NOT_IN_DB,
    val translation: TextValueState? = null,
    val definition: TextValueState? = null,
    val isMenuOpen: Boolean = false,
)

@Stable
data class TextValueState(
    val isEdit: Boolean = true,
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
- **`isCreatingLexeme: Boolean`** — Что: флаг «эффект `CreateLexeme` в полёте» — между нажатием FAB и приходом `RefreshLexeme(lexeme)`. Почему отдельное поле (whitelist (c) — async-данные не дефолтным паттерном):
  - **Защита от двойного нажатия FAB.** Без флага возможно `CreateLexeme(wordId)` дважды → две пустые лексемы. UI блокирует FAB по `isCreatingLexeme == true`.
  - **Не сворачиваем в `isLoading`.** `isLoading` — первичная загрузка слова; `isCreatingLexeme` — частный async на ту же сущность word. Семантика разная (общий vs локальный), сворачивание в один флаг исказит UI (full-screen spinner vs disabled FAB).
  - **Не делаем sealed** (variant `Idle | Creating`). На текущем уровне фичи нужен булев флаг, sealed избыточен. Если появятся новые async-операции на word — рефактор в `Loadable<T>`-подобный паттерн (см. memory `feedback_simple_state_loading`).
  - **Не делаем оптимистичную лексему в `lexemeList`** (с `id = NOT_IN_DB` до возврата). Это нарушит инвариант 3 и потребует дедуп-логики при `RefreshLexeme`. Флаг проще и удерживает инвариант 3.
- **`wordState: WordState`** — Что: суб-стейт самого слова (заголовок карточки).
- **`lexemeList: List<LexemeState>`** — Что: упорядоченный список лексем слова. Почему `List`, а не `Map<Id, LexemeState>` или `Set`: порядок важен для UI, дубли по id невозможны (id из БД), стоимость линейного поиска при `update` амортизируется небольшим количеством лексем у одного слова.
> 📎 guide: docs/guides/state-modeling.md — "Для описания state чаще всего хватает List/Map/Set; выбирать тип под семантику (List — упорядоченная последовательность, Set — принадлежность, Map — отображение)"
- **`snackbarState: SnackbarState`** — Что: транзиентный UI-сигнал (один на экран).

**Empty state.** Отдельного поля или sealed-variant для пустого списка лексем **нет**. Конвенция фиксируется явно: **`lexemeList.isEmpty() && !isLoading` ≡ empty state**. UI-слой рендерит placeholder по этому предикату. Обоснование: пустой `List<LexemeState>` уже различим от `null`/loading через комбинацию с `isLoading`; вводить отдельный variant — over-engineering.
> 📎 guide: docs/guides/state-modeling.md — "Empty state как отдельный variant не нужен: Success(items = emptyList()) уже различим через items.isEmpty()"

**Удалено:** `addLexemeBottomState: AddLexemeBottomState` — поле и тип уничтожаются. См. раздел «Удаляемые поля».

#### `LexemeState`

Без изменений в шейпе. Сохраняем существующие поля:
- **`id: Long`** — Что: идентификатор лексемы из БД, `NOT_IN_DB` до первой записи.
- **`translation: TextValueState?`** — Что: суб-стейт перевода. Почему nullable: «перевод не задан» — отдельное семантическое состояние, не пустая строка. Создаётся только когда пользователь нажал chip «Перевод».
> 📎 guide: docs/guides/state-modeling.md — "Семантика nullable: T? — может быть null на момент создания state или в течение flow; точнее моделирует «не задан» против пустого значения"
- **`definition: TextValueState?`** — Что: суб-стейт определения. Та же логика что и `translation`.
- **`isMenuOpen: Boolean`** — Что: контекстное меню лексемы (например, удаление).

#### `TextValueState`

Edit-сценарий с двумя осями `origin` / `edited` — whitelist триггер (b):
- **`isEdit: Boolean = true`** — Что: режим текущего редактирования поля. Почему дефолт `true` (whitelist (e) — обоснование существующего дефолта):
  - `TextValueState` **никогда не существует до момента, когда пользователь нажал chip «Перевод» / «Определение»**. Само создание экземпляра ⇒ пользователь явно вошёл в режим редактирования (chip = вход в edit). Поэтому дефолт `true` соответствует единственному легитимному моменту создания.
  - `null` (поле в `LexemeState`) и `TextValueState(isEdit=true, origin="", edited="")` — не противоречие: nullable различает «суб-стейт ещё не существует» vs «существует и сейчас в режиме edit». Переход `null → TextValueState(isEdit=true)` атомарный и происходит в одном reducer-шаге по `CreateTranslation` / `CreateDefinition`. См. инвариант 8.
  - Альтернатива (дефолт `false` + явное `isEdit=true` в фабрике) рассматривалась — отвергнута: дефолт всё равно никогда не используется как `false` при создании, дефолт-`false` ⇒ каждое место создания обязано писать `isEdit = true` ⇒ шум.
- **`origin: String`** — Что: значение из БД, last known good.
- **`edited: String`** — Что: текущий ввод пользователя. Почему два поля: при отмене редактирования нужно вернуть `origin`; при коммите — записать `edited` в БД и обновить `origin`. Один `value` это не поддерживает.
> 📎 guide: docs/guides/state-modeling.md — "Редактируемые поля держать отдельно от domain model: оригинал нетронут, edited — feature-level; позволяет undo и предотвращает конфликт с backend"

#### Прочие nested без изменений

`TopBarState`, `WordState`, `SnackbarState` — структурно остаются. Их поля тривиальны, описание не дублирую.

### Computed properties

На этом этапе **новых computed не вводится**. Существующие хелперы (`TextValueState.toValue(isEdit)`, `TextValueState.isChanged()`) сохраняются — они не computed на `WordCardState`, а utility-функции на `TextValueState`.

> 📎 guide: docs/guides/state-and-extensions.md — "Computed properties для derived полей: если значение выводится из других полей State — оформлять как extension val с custom getter, не хранить в data class"
> 📎 guide: docs/guides/state-modeling.md — "Computed property вместо хранимого вычисляемого поля: source of truth — только исходные поля, derived — view/selector"

Кандидаты, которые НЕ вводим (с обоснованием почему):

- `WordCardState.hasAnyLexeme: Boolean` (для скрытия пустого состояния) — не нужно на уровне State, UI-слой определит по `lexemeList.isEmpty()` напрямую (см. раздел Empty state выше). Не плодим.
- `WordCardState.isFabEnabled: Boolean` (= `!isCreatingLexeme && !isLoading`) — пока не вводим, UI считает на месте. Если предикат станет повторяться в нескольких местах — выделим в extension val.

### Раздел: Удаляемые поля / extensions / computed

Это переделка существующего State — раздел обязателен.

**Удаляется поле в `WordCardState`:**
- `addLexemeBottomState: AddLexemeBottomState` — больше не существует промежуточный UI «выбери что добавить». FAB создаёт пустую лексему сразу, внутри карточки лексемы существующие chip'ы создают нужные translation/definition. Триггер (e) — удаление поля.

**Добавляется поле в `WordCardState`:**
- `isCreatingLexeme: Boolean = false` — отражение async-операции `CreateLexeme` в полёте. Триггер (c) — async-данные не дефолтным паттерном (см. per-field описание выше). Триггер (e) — добавление нового поля в существующий state.

**Удаляется тип:**
- `data class AddLexemeBottomState(show, isTranslationCheck, isDefinitionCheck)` — целиком. Все три поля семантически принадлежали удаляемому промежуточному UI.

**Удаляются extensions:**
- `WordCardState.showAddLexemeBottom()` — больше нет «открытого» состояния промежуточного UI.
- `WordCardState.hideAddLexemeBottom()` — нет «закрытого» состояния.
- `WordCardState.setTranslationCheck(checked: Boolean)` — нет чекбокса «нужен перевод».
- `WordCardState.setDefinitionCheck(checked: Boolean)` — нет чекбокса «нужно определение».
> 📎 guide: docs/guides/state-and-extensions.md — "Extension-функции для всех мутаций стейта; нейминг — глагол в начале (show, hide, enable, disable, update, set)"

**Не удаляется (важно):**
- `LexemeState` и весь блок specialized lexeme extensions (`createLexemeTranslation`, `createLexemeDefinition`, `refreshLexemeTranslation` и т.д.) — переиспользуется механика chip'ов внутри лексемы.
- `addLexeme(lexeme: LexemeState)` — нужна для добавления пустой лексемы после `CreateLexeme` → `RefreshLexeme(lexeme)`. Эта же ext также сбрасывает `isCreatingLexeme = false` (см. инвариант 9). Конкретная сигнатура — в `contract_ui_msg` (Reducer).

## Инварианты

Инварианты — **жёсткие утверждения**, нарушение которых означает баг. Soft-rules вынесены в раздел «Политики».

1. **`wordState.id == NOT_IN_DB ⇒ isLoading || wordState.value.isEmpty()`** — пока слово не загружено из БД, идентификатор отсутствует. Без инварианта возможна попытка `CreateLexeme(wordId = NOT_IN_DB)`.
2. **`wordState.isEditMode == false ⇒ wordState.edited == ""`** — `edited` имеет смысл только в режиме редактирования. Существующий extension `disableWordEdit()` это гарантирует. Если поправят руками — нарушится.
3. **`forall l ∈ lexemeList: l.id != NOT_IN_DB`** — в список попадают только лексемы, у которых уже есть id из БД (через `RefreshLexeme`). FAB не добавляет «оптимистично» пустую лексему до возврата id — это обеспечивается полем `isCreatingLexeme` (между нажатием FAB и `RefreshLexeme` лексема не в списке).
4. **`forall l ∈ lexemeList: l.id уникален`** — гарантируется БД и контрактом `addLexeme` (без дедупа). UI-Compose-ключи (`items(... key = { it.id })`) полагаются на это.
> 📎 guide: docs/guides/state-modeling.md — "С ростом state — структурировать его как БД: появилась коллекция, начинай нормализовать; foreign key/индекс — связи между коллекциями"
5. **Порядок `lexemeList`.** Жёстко: `lexemeList` — упорядоченная последовательность; reducer обязан сохранять порядок при всех мутациях:
   - **При добавлении (`addLexeme`):** новая лексема добавляется **в конец** списка (append-семантика).
   - **При удалении лексемы:** оставшиеся элементы **сохраняют относительный порядок** (no reorder). Реализуется через `filter { it.id != removedId }`.
   - **При обновлении (`updateLexeme` / `refresh*`):** элемент остаётся **на той же позиции** (in-place replace по `id`).
   - **При configuration change:** порядок переживает rotation через ViewModel (state — поле в ViewModel, не пересоздаётся). Дополнительно: первичная загрузка из БД возвращает порядок согласно `WordCardUseCase` контракту (этот шаг не пересматривает контракт use case — порядок из БД считается стабильным для одного процесса).
6. **`snackbarState.show == false ⇒ snackbarState.title не отображается`** — текст без флага `show` UI не показывает. `hideSnackbar()` сбрасывает оба поля для чистоты, но инвариант не требует `title == ""`.
7. **`isCreatingLexeme == true ⇒ FAB заблокирован`** (UI-инвариант, проецируемый из State) — гарантия защиты от двойного нажатия. State-проекция: при `isCreatingLexeme == true` повторный `Msg.AddLexemeClick` либо игнорируется reducer'ом, либо UI не отправляет его. Контракт reducer'а — в `contract_ui_msg`.
8. **`l.translation != null ⇒ l.translation.isEdit == true || l.translation.isChanged() == false`** (то же для `definition`) — после коммита значения `isEdit` сбрасывается в `false`, и в этот момент `edited == origin`. Если `isEdit == false` и `edited != origin` — рассинхрон, баг. Формально: `l.translation != null ∧ l.translation.isEdit == false ⇒ l.translation.edited == l.translation.origin`.
9. **`isCreatingLexeme == true ⇒ предыдущий `CreateLexeme` ещё не вернул `RefreshLexeme`** — флаг сбрасывается в `false` атомарно при обработке `RefreshLexeme` (success) и при обработке любой ошибки `CreateLexeme` (failure). Никакой другой код не имеет права писать в `isCreatingLexeme`.

### Политики (soft-rules)

Не инварианты — поведенческие правила Reducer/UI, нарушение которых не делает State невалидным, но противоречит UX-замыслу. Закрепляются здесь, чтобы reducer не сделал «как удобнее».

- **Пустой `edited` в режиме редактирования допустим как промежуточное состояние.** Пользователь может стереть текст, не выходя из edit-mode. Сохранение пустого `translation` отдельным сообщением приведёт к удалению (см. `RemoveTranslation`) — это **осознанное** поведение, не инвариант. State не запрещает `isEdit == true ∧ edited == ""`.
- **`isCreatingLexeme` сбрасывается также при NavigateBack** (если пользователь успел уйти со страницы до возврата `RefreshLexeme`). Reducer-политика; детали — в `contract_ui_msg`.

### Закрытие open questions из scope_analysis (lifecycle_after_modal_removal)

Сейчас в State были «бесплатные» механики bottom sheet. С их потерей надо явно ответить:

1. **NavigateBack с открытым inline и заполненными чекбоксами — сбрасывать или сохранять?**
   → **Вопрос снимается.** Промежуточный inline-UI с чекбоксами отсутствует как сущность. NavigateBack уходит со страницы без специальной обработки.
> 📎 guide: docs/guides/state-and-extensions.md — "State = только отображаемое; никаких навигационных флагов (needClose, closeScreen) — навигация через Effect + NavigationEffectHandler"
> 📎 guide: docs/guides/reducer-patterns.md — "Навигация — не через boolean в State; reducer порождает NavigationEffect.Back, NavigationEffectHandler вызывает callback"

2. **Configuration change — сохранять состояние inline или сбрасывать?**
   → **Сохранять автоматически.** Inline-редакторы внутри лексемы (translation/definition) хранятся в `LexemeState.translation.edited` / `definition.edited`, флаг `isCreatingLexeme` — поле `WordCardState`. Всё переживёт configuration change через ViewModel. Порядок `lexemeList` — стабилен (инвариант 5).

3. **Ошибка `CreateLexeme` — inline остаётся «открытым» или закрывается?**
   → Поведение: FAB создаёт лексему через эффект `CreateLexeme(wordId)`, ставит `isCreatingLexeme = true`. Если эффект вернул ошибку (а не `RefreshLexeme`) — reducer обрабатывает failure-сообщение: `isCreatingLexeme = false`, `lexemeList` не меняется, ошибка показывается через `snackbarState`. **Нет состояния «открыто/закрыто» — нечего откатывать.** Защита от двойного нажатия (инвариант 7) удерживается до момента resolve (success или failure).
> 📎 guide: docs/guides/mate-framework.md — "Редьюсер всегда чистый; эффекты — sealed interfaces; эффект-хендлеры конвертируют результат в Message — состояние меняется только через reducer"

4. **Семантика «открыто/закрыто» inline без флага `show` — атомарный признак, enum или sealed?**
   → **Не нужно ни то, ни другое.** Признака «открыто/закрыто» больше нет в модели. Состояние «есть/нет translation у лексемы» уже выражено через `LexemeState.translation: TextValueState?` (nullable как ось — whitelist (a)). Аналогично для definition.
> 📎 guide: docs/guides/state-modeling.md — "Ловушка ложного «или»: если новая «ось координат» — это product (data class), не sum; nullable T? точнее моделирует «есть/нет» чем дополнительный variant"

## Расхождения spec ↔ code

**Не применимо.** Режим работы — 1 (макет-driven), не 2 (spec-driven). Спека отсутствует (`spec_filename = null`). Сверка кода со спецификацией не производится; источник истины — Figma макет. Создание/обновление спеки делегируется шагу `publish_spec`.

---

_model: claude opus 4.7 (1M context)_
