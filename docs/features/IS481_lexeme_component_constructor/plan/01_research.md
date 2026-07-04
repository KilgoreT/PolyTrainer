# Research: конструктор типов компонентов лексемы

Предварительный обзор открытых вопросов и вариантов реализации. **Решения не приняты** — это материал для обсуждения. По итогам обсуждения зафиксируем выбранные варианты в `02_decisions.md` (или эквивалент) и запустим адаптивный flow.

---

## 1. Скоуп хранения типов

**Вопрос:** где живут пользовательские типы компонентов — на уровне всего аккаунта или конкретного словаря?

| Вариант | Плюсы | Минусы |
|---|---|---|
| **A. Глобально (per-account)** | Один раз создал тип «pronunciation» — он есть во всех словарях | Засорение списка типов при разных тематиках словарей (EN-RU vs RU-EN vs cooking-terms — у каждого свои нужды) |
| **B. Per-dictionary** | Чистый набор типов под тематику словаря; можно копировать словарь со своим набором | Дублирование «typical» типов (pronunciation, example) в каждом словаре; невозможность переиспользования |
| **C. Mixed: глобальные + per-dictionary** | Гибкость: общие типы доступны везде, специфичные — только в своём словаре | Сложнее модель (два scope), пользователю надо понимать различие |

**Мой выбор:** B (per-dictionary) на старте — проще модель, понятнее юзеру (тип = атрибут словаря, как и сама лексема). Если будет потребность шарить — добавим A или C позже.

**🔒 Принято (теоретически, уточнится при реализации):**
- БД: `ComponentType.dictionaryId: Long?` — nullable FK. `null` = global, `!= null` = per-dictionary. Схема готова к mixed-варианту сразу, удорожание ~0 (одно nullable поле).
- **Built-in типы (translation / definition) — global** (`dictionaryId = null`, `isSystem = true`). См. § 5.
- **User-defined типы на старте — per-dictionary** (`dictionaryId != null`, `isSystem = false`).
- UI запрос «типы доступные в словаре X» = `dictionaryId == X OR dictionaryId == null` — естественно подхватывает и built-in (global), и user-defined (per-dictionary).
- User-defined global типы — отдельная фича когда появится концепция учётки / шаринга.

**⚠ TBD (важное, откладываем): как именно объявлять scope компонента.**

Подход с `dictionaryId: Long?` — рабочее первое приближение, но **не финальное архитектурное решение**. Варианты для будущего обсуждения:
- **Иерархия владельца** через `owner_type: enum (SYSTEM | DICTIONARY | USER | GROUP) + owner_id: Long?`. Гибче, но сложнее.
- **Список scope'ов** — тип имеет список диапазонов применимости (`scopes: List<Scope>`), Scope = sealed (Global / DictionaryRef / TagRef / …).
- **Иерархия наследования типов** — global родительский тип может быть «переопределён» per-dictionary типом с тем же именем (типа CSS-cascade).
- **Composition через теги** — тип помечен набором тегов (`tags: Set<String>`), словарь подхватывает по совпадению.

Решение принимается отдельно — когда появится концепция учётки / шаринга / реальной потребности. На старте идём с nullable `dictionaryId` чтобы не блокировать фичу.

---

## 2. Built-in типы (translation / definition)

**Вопрос:** как сохранить translation / definition после введения конструктора.

| Вариант | Плюсы | Минусы |
|---|---|---|
| **A. Хардкод в коде + спец-обработка в UI** | Меньше миграций; built-in типы не конфликтуют по имени | Два кодпуть (built-in vs user-defined) — сложность |
| **B. Built-in типы как **системные записи** в той же таблице** с флагом `isSystem: true` (защита от удаления) | Единая модель — один путь для всех типов | Нужна миграция существующих данных в новый формат + защита от удаления / переименования built-in |
| **C. Только user-defined типы**, translation / definition мигрируют как обычные user-defined типы без защиты | Самая простая модель | Юзер может удалить translation — потеряет UX-смысл |

**Мой выбор:** B — единая модель + флаг защиты. Это даст возможность в будущем добавлять built-in типы без правок UI (только данные).

**🔒 Принято:** B. Built-in типы (translation / definition) — системные записи в той же таблице `ComponentType` с `isSystem: true`. Защита от удаления / переименования built-in типов — на уровне reducer / UseCase. Добавление новых built-in типов в будущем — миграция данных, без правок UI.

---

## 3. Структура данных

### Базовые сущности

```kotlin
ComponentType(
    id: Long,
    dictionaryId: Long,        // если выбран вариант 1.B
    name: String,              // "translation", "pronunciation", "my-note"
    template: ComponentTemplate,
    isSystem: Boolean,         // true для translation / definition
    sortOrder: Int,            // порядок отображения в лексеме
)

ComponentTemplate = TEXT | LONG_TEXT | IMAGE | ...   // sealed

ComponentValue(
    id: Long,
    lexemeId: Long,
    componentTypeId: Long,
    value: String,             // для TEXT / LONG_TEXT
    imageUri: String?,         // для IMAGE
    // ... другие поля по шаблонам
)
```

**Вопрос:** как хранить value для разных шаблонов?

| Вариант | Плюсы | Минусы |
|---|---|---|
| **A. Универсальное `value: String`** — для image хранится URI/path | Простая схема | Слабая типизация; для будущих шаблонов (audio / video) — те же костыли |
| **B. Sealed `ComponentValueData` с per-template payload** в коде, в БД храним JSON | Чистая типизация в Kotlin | JSON-парсинг при чтении; миграция при изменении формата сложнее |
| **C. Отдельная таблица per template** (TextValue, ImageValue, ...) | Чистая схема | Усложнение JOIN; полиморфные запросы плохо ложатся на SQL |

**Мой выбор:** B (JSON + sealed) — даёт типизацию в коде + расширяемость без миграции БД при добавлении новых шаблонов.

**🔒 Принято:** B. В БД `ComponentValue` хранит `payload: String` (JSON). В коде — sealed `ComponentValueData`:
```kotlin
sealed interface ComponentValueData {
    @Serializable data class Text(val text: String) : ComponentValueData
    @Serializable data class LongText(val text: String) : ComponentValueData
    @Serializable data class Image(val uri: String /* + опционально alt, dimensions */) : ComponentValueData
    // дальше — Audio, Video, Link, Table, ...
}
```
Сериализация / десериализация по `ComponentType.template`. Миграция при изменении формата конкретного шаблона — через версию payload (поле `version: Int` в JSON) или fallback на default.

### Связь Lexeme ↔ Components

Сейчас Lexeme хранит translation / definition как отдельные поля. После рефакторинга:

```kotlin
Lexeme(
    id: Long,
    wordId: Long,
    // translation / definition удаляются как отдельные поля
    components: List<ComponentValue>   // через @Relation
)
```

---

## 4. Список шаблонов на старте

Минимально — `text` / `long-text` / `image`. Возможные:
- `text` — однострочный, ограничение по длине (например 100 символов).
- `long-text` — многострочный, без жёсткого лимита.
- `image` — путь к файлу (локально) или URI.

**Вопрос:** как ограничить расширяемость на старте?

Предлагаю: `ComponentTemplate` = sealed класс в коде, в БД храним `templateKey: String`. Новые шаблоны добавляются как новые подклассы + миграция «прочитать неизвестный templateKey → fallback на text или skip».

**🔒 Принято:** sealed `ComponentTemplate` в коде, в БД хранится `templateKey: String` (например `"text"` / `"long-text"` / `"image"`). Стартовый набор — text / long-text / image. Расширение — добавление нового подкласса + ключа. Чтение неизвестного `templateKey` — fallback на text (с warning в логе) или skip — выбор на этапе реализации.

---

## 5. Миграция существующих данных

Существующие translation / definition в Lexeme при upgrade БД:

1. **Один раз** создать built-in `ComponentType` с `dictionaryId = null` (global) — для translation и для definition: `(name="translation", template=TEXT, isSystem=true, dictionaryId=null)` + аналог для definition.
2. Для каждой Lexeme — создать `ComponentValue` со ссылкой на соответствующий global built-in тип.
3. Удалить колонки translation / definition из Lexeme.

**🔒 Принято:** built-in типы создаются один раз (не для каждого словаря). См. § 1 — built-in глобальные (`dictionaryId = null`), UI подхватывает их во всех словарях через запрос `dictionaryId == X OR dictionaryId == null`.

**Open:** что делать с пустыми translation / definition? Не создавать ComponentValue (потому что нет данных) или создавать пустой? Думаю не создавать — пустой компонент в UI не нужен.

---

## 6. Порядок компонентов в лексеме

**Вопрос:** компоненты в лексеме упорядочены по `ComponentType.sortOrder` или **отдельно** per-lexeme?

| Вариант | Плюсы | Минусы |
|---|---|---|
| **A. По sortOrder из ComponentType** (глобальный порядок для словаря) | Простая модель; одинаковый порядок во всех лексемах словаря | Нельзя переставить компоненты в конкретной лексеме |
| **B. Per-lexeme order** (sortOrder в ComponentValue) | Юзер может выделять разные компоненты как «важные» в разных лексемах | Усложнение UI / state; ценность сомнительна |

**Мой выбор:** A — порядок задаётся на уровне типа в словаре. Drag-and-drop типов в настройках словаря.

**🔒 Принято:** A. `ComponentType.sortOrder: Int` — глобальный порядок в словаре. Один порядок для всех лексем словаря. Per-lexeme переопределение порядка — отдельной задачей если появится потребность.

**🔒 Built-in всегда раньше user-defined.** Built-in типы (global, `isSystem=true`) идут первыми с фиксированным порядком (их собственный sortOrder между собой). User-defined типы (per-dictionary) идут после built-in. Юзер не может переставить user-defined тип выше built-in.

---

## 7. Удаление типа

**Вопрос:** что происходит с ComponentValue'ами при удалении ComponentType.

| Вариант | Плюсы | Минусы |
|---|---|---|
| **A. Cascade delete** — удаляются все ComponentValue этого типа из всех лексем | Чисто, нет orphan-данных | Юзер может случайно потерять данные многих лексем |
| **B. Soft delete** — ComponentType помечается `deleted=true`, ComponentValue остаются, в UI не показываются | Безопасно, можно восстановить | Засорение БД |
| **C. Запрет удаления при наличии данных** — сначала удалить все ComponentValue (или auto-prompt) | Юзер явно подтверждает потерю | UX-сложнее |

**Мой выбор:** C — confirm dialog при удалении типа с показом числа лексем где он используется. + опция «удалить тип и все его значения». Built-in типы не удаляются вообще.

**🔒 Принято:** B + отложенный cleanup + confirm-диалоги с числами.
- При удалении user-defined типа — `ComponentType.deleted = true` + `deletedAt: timestamp`. ComponentValue остаются в БД.
- В UI удалённый тип и его значения не показываются.
- **Через какое-то время** (период TBD — например 30 дней) — физическое удаление: cleanup job / при следующем запуске. Удаляются `ComponentType` где `deleted=true` и `deletedAt < now() - retention_period`, каскадно с ComponentValue.
- Built-in типы (`isSystem=true`) не удаляются вообще — попытка удаления → error / disabled UI.
- Период retention и триггер cleanup (job / app start / другое) — детали на этапе реализации.

**🔒 UX подтверждений (обязательно):** перед удалением типа — confirm-диалог с **числами затронутых сущностей**. Минимум: «удаляется тип `<name>` — затронуто N лексем (N ComponentValue будут скрыты)». Юзер видит масштаб прежде чем подтвердить. Аналогичные предупреждения — при других потенциально опасных операциях с типами (переименование которое ломает миграции, изменение шаблона если такое будет разрешено, и т.п.).

---

## 8. UI входа в конструктор

Где живёт UI создания / управления типами? Варианты:
- В настройках словаря (Settings → Dictionary → Component types).
- В отдельном экране (Dictionary → ⋮ → Types).
- Inline в карточке слова (быстрое создание нового типа при добавлении компонента).

**Моё предложение:** комбо — основной экран в настройках словаря + быстрое создание inline в карточке слова (для случая «вижу компонент которого не хватает — добавляю на лету»).

**⚠ TBD (отложено):** UI входа в конструктор на старте не делаем.

На первой итерации фичи — только **built-in компоненты** (translation / definition; в будущем добавим example / note как built-in). UI создания / редактирования / удаления user-defined типов **не реализуем сейчас**.

То есть фактический скоуп первой итерации = **рефакторинг** translation / definition в новую модель (`ComponentType` + `ComponentValue` + sealed `ComponentTemplate` / `ComponentValueData`) без user-facing новой функциональности. Конструктор как механизм существует, но используется только для built-in.

UI входа (settings / отдельный экран / inline в карточке) — отдельной задачей когда понадобится открыть для пользователей. Решение по точке входа (комбо vs только settings) принимается тогда.

---

## 9. Открытые вопросы для дальнейшего обсуждения

1. **Локализация имён built-in типов.** Сейчас translation / definition — это локализованные строки (`R.string.*`). После миграции у нас name: String в БД. Хранить ключ (`"translation"`) и резолвить через ResourceManager? Или хранить локализованное имя пользователя?

   **🔒 Принято:** built-in типы — имена из ресурсов (`R.string.*`). В БД у built-in `ComponentType` хранится **ключ ресурса** (например `"translation"`), резолв через ResourceManager при отображении. User-defined типы (когда появятся) — хранят литеральное имя как ввёл пользователь.

2. **Шаблон `image` — где хранить файл.** Внутреннее хранилище приложения / public storage / Photo Library? Backup при пере-установке?

   **⚠ TBD:** обсуждается при внедрении шаблона `image`. На первой итерации image не активен (см. § 8 — только built-in типы translation / definition).

3. **Квиз-режим — какие типы участвуют.** Сейчас квиз основан на translation / definition. После конструктора — пользователь сам отмечает какие типы участвуют в квизе? Или только text/long-text-типы автоматически?

   **🔒 Принято:** на старте ничего не меняется. Квиз работает через текущие translation / definition (после миграции — через built-in типы с теми же именами). Логика участия user-defined типов в квизе — отдельная задача когда такие типы появятся.

4. **Поиск / фильтр лексем по компонентам.** Сейчас поиск по term + translation. После конструктора — поиск по value всех text-типов?

   **🔒 Принято:** на старте ничего не меняется. Поиск работает как сейчас (term + translation через built-in). Расширение поиска на user-defined типы — отдельная задача.

---

## Следующие шаги

После обсуждения этих пунктов:
1. Фиксируем выбранные варианты в `02_decisions.md`.
2. Уточняем шаблоны (полный список на старте).
3. Запускаем адаптивный flow.
