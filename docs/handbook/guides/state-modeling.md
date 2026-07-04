# Гайд по моделированию State в UDF-архитектуре

Конспект доклада: **Михаил Левченко — «Итак, вы выбрали UDF-архитектуру. Как моделировать стейт?»** (Mobius Piter 2021, [YouTube](https://www.youtube.com/watch?v=y0CHhHBzEkw)).

Доклад не привязан к языку/фреймворку — применимо к Kotlin, Swift, Dart. Примеры на Kotlin.

---

## Оглавление

1. [Контекст: что такое UDF и его tradeoff](#1-контекст)
2. [Принцип №1: State — это данные, не объекты](#2-state--это-данные)
3. [Инструменты для работы с данными](#3-инструменты-для-работы-с-данными)
4. [Algebraic Data Types — суть](#4-algebraic-data-types--суть)
5. [Product types — произведение](#5-product-types--произведение)
6. [Sum types — сложение](#6-sum-types--сложение)
7. [Ловушка ложного "или"](#7-ловушка-ложного-или)
8. [Считаем варианты State](#8-считаем-варианты-state)
9. [LCE и Loadable как переиспользуемые контейнеры](#9-lce-и-loadable)
10. [Dependency Rule для моделей State](#10-dependency-rule)
11. [State как база данных](#11-state-как-база-данных)
12. [Selectors — представления State](#12-selectors)
13. [Производительность и копирование](#13-производительность)
14. [Редактируемые поля](#14-редактируемые-поля)
15. [Индексы в State](#15-индексы)
16. [Инварианты State: shape vs transition](#16-инварианты-state)
17. [Q&A — практические нюансы](#17-qa)
18. [Итоги — чеклист](#18-итоги)

---

## 1. Контекст

### Что такое UDF

**UDF (Unidirectional Data Flow)** — семейство архитектурных подходов:
- MVI
- Redux
- TEA (The Elm Architecture)
- BLoC

Конкретный подход не важен для гайда — важна **общая идея однонаправленного потока**.

### Зачем нужен UDF

**1. Контроль над изменениями состояния.**
- State монополизирован store
- Любая синхронная работа безопасна — state в одних «цепких лапах»
- Изменения возможны из одного места

**2. Single Source of Truth.**
- State становится единым источником правды для всей бизнес-логики
- Все функции работают над одним состоянием

**3. Дружит с декларативным UI.**
- Compose, SwiftUI, Flutter, React, React Native — все строят UI из state
- UDF + декларативный UI — естественная пара

### Главный tradeoff UDF

**Гигантский State.**

Без следящего за качеством — state превращается в **башню Jenga**: вот-вот развалится при добавлении новой фичи или удалении старой. Поэтому важно уметь моделировать state правильно.

---

## 2. State — это данные

### Первое правило: разучиться делать ООП на State

Когда работаешь со state — **забыть** всё что знаешь про ООП. Опыт работы с API объектов мешает.

### Антипаттерны

**Антипаттерн 1: State как объект с методом newState().**

```kotlin
// ❌ Так не делать
interface State {
    fun newState(): State
}
```

Сильно связывает state и поведение, нечитаемая логика «каждый state знает в какой может перейти».

**Антипаттерн 2: State как UseCase с бизнес-логикой.**

```kotlin
// ❌ Так не делать
class PaymentScreenState {
    fun executePayment(): PaymentResult { ... }
}
```

State становится «entity из чистой архитектуры» с пластами бизнес-логики. Это **не state**.

### Что такое данные

**Данные = неизменяемые факты о системе:**
- Анемичные (без поведения)
- Immutable
- Складываются как факты, не как действия

**Свойства данных:**

| Свойство | Описание |
|---|---|
| Сериализуются | JSON / XML / Protobuf / любой формат описания данных |
| Открыты для интерпретации | Получатель сам решает что с ними делать |
| Не имеют identity | Две копии с одинаковыми полями неразличимы |

### Метафора: твит Илона Маска

Когда Илон Маск пишет твит «надо покупать Tesla» — это **данные**. Вы прочитав сами решаете что делать.

Если бы твиты были **объектами** — Маск присылал бы сообщение и вызывал метод. Поведение склеено с данными.

State должен быть как твит, не как метод.

### Почему 3 столпа ООП ломаются на данных

| Столп ООП | Зачем для данных? | Вердикт |
|---|---|---|
| **Инкапсуляция** | Скрывать данные нечего — они immutable, неоткуда вред | Не нужна |
| **Identity** | Факты неразличимы между копиями. Identity ломает сериализацию | Не нужна (только вредит) |
| **Наследование / полиморфизм** | Нет поведения — нет полиморфизма. Поведение строится внешними функциями над данными | Не нужны |

---

## 3. Инструменты для работы с данными

### Базовые типы

| Категория | Что это |
|---|---|
| Числа, строки | Примитивы данных |
| Enum / Sealed objects | Именованные метки и перечисления меток |

**Заметка:** автор подчёркивает что это **базовые** типы, не «примитивы» в техническом смысле. Это атомы данных.

### Коллекции

| Тип | Назначение |
|---|---|
| `List<T>` | Упорядоченная последовательность, дубликаты разрешены |
| `Map<K, V>` | Отображение ключ → значение |
| `Set<T>` | Множество, проверка принадлежности |

**Для описания state чаще всего хватает этих трёх.** Специфические интерфейсы коллекций обычно не нужны.

### ADT — следующий уровень

См. раздел [4](#4-algebraic-data-types--суть).

---

## 4. Algebraic Data Types — суть

**Algebra = composability** (по Scott Wlaschin).

«Алгебраические типы» = типы которые можно **собирать друг с другом**.

### Два способа собрать данные

**Сложение (Sum):**
- Между типами стоит **«ИЛИ»**
- Объединение: либо T₁, либо T₂
- Реализация в Kotlin: `sealed class` / `sealed interface`
- Количество вариантов: `|T₁| + |T₂|`

**Произведение (Product):**
- Между типами стоит **«И»**
- Объединение: T₁ И T₂ одновременно
- Реализация в Kotlin: `data class`
- Количество вариантов: `|T₁| × |T₂|`

---

## 5. Product types — произведение

### Пример: фильтр аксессуаров

```
┌─────────────────────────────┐
│ Фильтр аксессуаров          │
│                             │
│ ☐ Место для бутылки         │
│ ☐ Багажник                  │
└─────────────────────────────┘
```

**Анализ:**
- Два независимых чекбокса
- Каждый: вкл / выкл
- Все комбинации валидны

**Количество состояний:** 2 × 2 = **4** (произведение)

**Реализация:**

```kotlin
data class AccessoriesFilter(
    val bottleHolder: Boolean,
    val rack: Boolean,
)
```

### Это всё ещё НЕ ООП

Несмотря на `class`:
- Нет приватных полей
- Нет наследования
- Нет методов

Это **«мешочек с данными»**, объединённый в один тип. Не объект.

### Правило произведения

**Независимые фильтры / поля → product type → data class.**

Союз «И» между значениями — признак произведения.

---

## 6. Sum types — сложение

### Пример: фильтр амортизации

```
┌──────────────────────────────────────┐
│ ☑ Амортизация                        │
│                                       │
│   ○ Подвес: моно                     │
│   ● Подвес: двухподвес               │
└──────────────────────────────────────┘
```

При выключенной амортизации — подвес недоступен.

**Анализ:**
- 3 состояния: выкл / вкл-моно / вкл-двухподвес
- НЕ 4 — потому что выкл-моно и выкл-двухподвес неразличимы (амортизации нет — нечего подвешивать)

### Реализация product way (плохо)

```kotlin
// ❌ Плохо: даёт 4 состояния, одно из которых невалидно
data class SuspensionFilter(
    val hasSuspension: Boolean,
    val fullSuspension: Boolean,
)
```

`hasSuspension=false, fullSuspension=true` — невалидное состояние.

### Реализация sum way (хорошо)

```kotlin
// ✅ Хорошо: ровно 3 валидных состояния
sealed class SuspensionFilter {
    data object NoSuspension : SuspensionFilter()
    data class Suspension(val fullSuspension: Boolean) : SuspensionFilter()
}
```

**Количество:** 1 + 2 = **3** (сложение).

### В других языках

| Язык | Реализация |
|---|---|
| Kotlin | `sealed class` / `sealed interface` |
| Swift | `enum` с associated values |
| Dart | `sealed class` (с Dart 3) |
| Java | Иерархия классов без специальных ключевых слов |

### Правило сложения

**Зависимые фильтры / взаимоисключающие состояния → sum type → sealed class.**

Союз «ИЛИ» между значениями — признак сложения.

**Польза:** исключает невалидные состояния на уровне типов, точнее моделирует домен.

---

## 7. Ловушка ложного "или"

### Сценарий

Пришёл дизайнер, сказал: «Прыгает интерфейс, не понятно куда деваются настройки. Давай покажем подвес disabled при выключенной амортизации».

```
┌──────────────────────────────────────┐
│ ☐ Амортизация                        │
│                                       │
│   ○ Подвес: моно           (disabled)│
│   ○ Подвес: двухподвес     (disabled)│
└──────────────────────────────────────┘
```

### Соблазн: оставить sealed class

```kotlin
// ❌ Соблазн: завести вариант "выключено-но-помним-выбор"
sealed class SuspensionFilter {
    data class Disabled(val rememberedFullSuspension: Boolean) : SuspensionFilter()
    data class Enabled(val fullSuspension: Boolean) : SuspensionFilter()
}
```

**Кажется** что это всё ещё sum: 2 + 2 = 4 варианта.

### Что реально происходит

На самом деле здесь **произведение**:
- Ось 1: видно ли disabled placeholder = «есть ли амортизация» (boolean)
- Ось 2: значение подвеса (boolean)

Это product, замаскированный под sum. Многословная моделька повторяет себя без выгоды.

### Решение: вернуть product + helper-функция

```kotlin
// ✅ Решение
data class SuspensionFilter(
    val hasSuspension: Boolean,
    val fullSuspension: Boolean,
)

// Доменная логика — отдельной функцией
fun SuspensionFilter.toggleFullSuspension(): SuspensionFilter =
    if (hasSuspension) copy(fullSuspension = !fullSuspension)
    else this
```

### Главный принцип

**Разделять данные и код в разные «стопочки».**
- Данные открыты для интерпретации
- Код (helper-функции) реализует доменную логику
- Системы типов mainstream-языков не выражают всю доменную логику — динамику выносим в функции

### Правило

Когда возникает соблазн «расширить sum class новым вариантом» — спросить: **это новое состояние или новая ось координат**? Если ось — это product, не sum.

---

## 8. Считаем варианты State

### Зачем считать

Подсчёт вариантов state помогает:
- Понять насколько точно описан домен
- Найти лишние комбинации (невалидные состояния)
- Найти недостающие комбинации

### Правила подсчёта (нотация типов)

`count(T)` — количество возможных значений типа `T` (cardinality множества). В классической математике это пишется как `|T|`, но pipe-символ ломает markdown-таблицы, поэтому здесь и далее `count(T)`. Это запись для документации; в коде такого «оператора» нет.

| Конструкция | Количество вариантов |
|---|---|
| `Boolean` | 2 |
| `Boolean?` (nullable) | 3 (true / false / null) |
| `T?` | `count(T) + 1` |
| `data class A(x: T₁, y: T₂)` | `count(T₁) × count(T₂)` |
| `sealed class A : T₁; B : T₂` | `count(T₁) + count(T₂)` |

### Пример

```kotlin
// Boolean = 2 варианта
val flag: Boolean = true

// Boolean? = 3 варианта
val nullableFlag: Boolean? = null

// data class A(b: Boolean, n: Boolean?) = 2 × 3 = 6 вариантов
data class A(val b: Boolean, val n: Boolean?)

// sealed class A или B = 2 + 3 = 5 вариантов
sealed class C {
    data object A : C()              // 1 (плюс ничего)
    data class B(val n: Boolean?) : C()  // 3 (b + n)
}
```

---

## 9. LCE и Loadable

### Идея LCE

**LCE = Loading / Content / Error.**

Классический паттерн контейнера для асинхронных данных. В Android он стал клише.

### Расширенный LCE — с Idle

```
        ┌─────────┐
        │  Idle   │  (initial, ещё ничего не грузили)
        └────┬────┘
             │ load()
             ▼
        ┌─────────┐
        │ Loading │
        └─┬─────┬─┘
          │     │
   success │     │ failure
          ▼     ▼
    ┌────────┐ ┌────────┐
    │Success │ │ Error  │
    └────────┘ └────────┘
                  │
                  │ retry()
                  ▼
              (back to Loading)
```

**Количество состояний:** 4 (Idle / Loading / Error / Success).

### Реализация наивная

```kotlin
sealed class LoadableData<T> {
    data object Idle : LoadableData<Nothing>()
    data object Loading : LoadableData<Nothing>()
    data class Error(val cause: Throwable) : LoadableData<Nothing>()
    data class Success<T>(val value: T) : LoadableData<T>()
}
```

### Reloadable — LCE с перезагрузкой

Сценарий: уже загрузили — хочется обновить, **не теряя текущих данных** на экране.

**Состояния (6 штук):**
1. Idle
2. Loading (первая загрузка, value нет)
3. Reloading (есть value, грузим)
4. Error (нет value)
5. LoadingError (есть value, но prev попытка обновления упала)
6. Success (есть value)

```kotlin
sealed class Reloadable<T> {
    data object Idle : Reloadable<Nothing>()
    data object Loading : Reloadable<Nothing>()
    data class Reloading<T>(val value: T) : Reloadable<T>()
    data class Error(val cause: Throwable) : Reloadable<Nothing>()
    data class LoadingError<T>(val value: T, val cause: Throwable) : Reloadable<T>()
    data class Success<T>(val value: T) : Reloadable<T>()
}
```

6 классов = **6 вариантов**. Управляемо, но много.

### Сжатие: Loading и Reloading объединить

`Loading` и `Reloading<T>` отличаются только наличием value. Это **nullable value**.

```kotlin
sealed class Reloadable<T> {
    data object Idle : Reloadable<Nothing>()
    data class Loading<T>(val value: T?) : Reloadable<T>()       // 1 класс, 2 варианта
    data class Error<T>(val value: T?, val cause: Throwable) : Reloadable<T>()  // 1 класс, 2 варианта
    data class Success<T>(val value: T) : Reloadable<T>()
}
```

**4 класса, 6 вариантов** — те же самые состояния, но удобнее работать.

### Дальнейшее сжатие: Status + value

Если value одинаково ведёт себя в нескольких ветках — вынести в parent.

```kotlin
data class Loadable<T>(
    val status: Status,    // Idle / Loading / Error
    val value: T?,
) {
    enum class Status { IDLE, LOADING, ERROR }
}
```

**Считаем:** 2 (value nullable) × 3 (status) = **6 вариантов**. Тот же домен.

### Какой вариант выбрать

Зависит от удобства. Все три эквивалентны по выраженности домена, но дают разный «вкус» при работе:
- Sealed class — лучшее type-safety, smart cast
- Data class + status — компактнее, удобнее `copy()`

### Семантика nullable / generic

| Тип | Семантика |
|---|---|
| `T` | Всегда есть |
| `T?` | Может быть null (на момент создания state или в течение flow) |
| `Loadable<T>` | Загружается во время работы, нужно ждать |

---

## 10. Dependency Rule

### Три уровня моделей

```
┌─────────────────────────────────────────┐
│ Feature-level                           │
│  (модельки конкретного экрана/виджета)  │
│                                          │
│  ┌─────────────────────────────────┐   │
│  │ Domain-level                    │   │
│  │  (модельки используемые многими │   │
│  │   фичами, лежат в `core/`)      │   │
│  │                                  │   │
│  │  ┌──────────────────────────┐   │   │
│  │  │ Library-level            │   │   │
│  │  │  (LCE, Loadable, общие   │   │   │
│  │  │   контейнеры)            │   │   │
│  │  └──────────────────────────┘   │   │
│  └─────────────────────────────────┘   │
└─────────────────────────────────────────┘
```

### Правило

**Внутрь можно, наружу — нельзя.**
- Feature знает о Domain и Library
- Domain знает о Library
- Library не знает ни о чём специфичном

### Антипаттерн

```kotlin
// ❌ Доменная модель Offer знает о фиче "список объявлений"
data class Offer(
    val id: OfferId,
    val title: String,
    val isViewed: Boolean,    // ← feature-level флаг в domain model!
)
```

`isViewed` — это специфика **одной фичи** (список объявлений). Положив его в `Offer`, нарушаем Dependency Rule — domain знает о feature.

### Решение: композиция

```kotlin
// ✅ Domain
data class Offer(
    val id: OfferId,
    val title: String,
)

// ✅ Feature
data class OfferListItem(
    val offer: Offer,         // domain model
    val isViewed: Boolean,    // feature-specific
)
```

Или — отдельный список viewed ids на feature-уровне (см. [State as DB](#11-state-как-база-данных)).

### Почему это важно

- Domain модели переиспользуются между фичами
- Изменение в одной фиче не должно ломать domain
- Тестировать domain отдельно от feature

---

## 11. State как база данных

### Тезис

**С ростом state — структурировать его как БД.**

Как только в state появилась коллекция — вы уже на пути к БД. Применять практики БД.

### Пример: список просмотренных объявлений

**Антипаттерн (денормализация):**

```kotlin
// ❌ Дублирование данных
data class OfferListState(
    val offers: List<Offer>,
    val viewedOffers: List<Offer>,    // те же offers, но просмотренные
)
```

Проблемы:
- Обновляя offer — обновлять в двух местах
- Легко получить рассинхрон

**Правильно (нормализация):**

```kotlin
// ✅ Отдельная "таблица" viewed-ids
data class OfferListState(
    val offers: List<Offer>,
    val viewedIds: Set<OfferId>,    // отдельная "таблица"
)
```

### Почему Set, а не Map<OfferId, Boolean>

| Тип | Варианты на id | Семантика |
|---|---|---|
| `Map<OfferId, Boolean>` | 3 (true / false / нет ключа) | Можно записать `false` — лишний вариант |
| `Set<OfferId>` | 2 (есть / нет) | Принадлежность множеству |

**Set — точнее моделирует «принадлежит или нет»** без лишних вариантов.

### Что даёт подход «State = БД»

| Концепция БД | В State |
|---|---|
| Таблица | Коллекция (`List` / `Set` / `Map`) |
| Колонка | Поле data class |
| Foreign key | id-ссылка между коллекциями |
| Index | Map от id к объекту (см. [§15](#15-индексы)) |
| View | Selector / computed property (см. [§12](#12-selectors)) |
| Нормализация | Не дублировать данные |

**Бонус:** в LinkedIn можно гордо писать «in-memory database developer» 😄

---

## 12. Selectors

### Что такое selector

**Selector = функция, превращающая State в проекцию для конкретной view.**

```kotlin
// State в DB-стиле
data class OfferListState(
    val offers: List<Offer>,
    val viewedIds: Set<OfferId>,
)

// Selector — view-like вычисление
val OfferListState.viewedOffers: List<Offer>
    get() = offers.filter { it.id in viewedIds }
```

Это аналогично **VIEW в БД** — производная таблица, не хранится отдельно.

### Computed property vs хранимое поле

**Антипаттерн: хранить вычисляемое:**

```kotlin
// ❌ Total хранится как поле
data class CartState(
    val services: List<Service>,
    val selectedServices: List<ServiceId>,
    val total: Money,                     // ← вычисляется из services + selectedServices!
)
```

Проблемы:
- Обновлять total в двух местах (когда меняются services и selectedServices)
- Легко забыть обновить
- Несогласованный state

**Правильно: computed property:**

```kotlin
// ✅ Total — selector
data class CartState(
    val services: List<Service>,
    val selectedServiceIds: Set<ServiceId>,
)

val CartState.total: Money
    get() = services
        .filter { it.id in selectedServiceIds }
        .sumOf { it.price }
```

### State vs ViewModel

**В MVVM:** ViewModel содержит свойства для каждого отображаемого элемента — `@Bindable`, `LiveData<>` под каждый виджет.

**В UDF:** State — это **бизнес-логика**, не ViewModel. ViewModel конкретного экрана = **проекция state через selectors**.

```
       UDF State
   (бизнес-сущности, БД)
           │
           │ selectors
           ▼
    View-Model экрана
   (то что рендерит UI)
           │
           ▼
        Composable
        (отрисовка)
```

### Где живут selectors

| Вариант | Когда |
|---|---|
| Extension property/function на State | Простые случаи |
| Отдельный класс / объект | Когда selectors сложные / много |
| Helper-функция в render-методе | Тривиальные мгновенные мапы |

Решение по месту — вопрос рефакторинга. Главное — **разделить state и его проекции**.

---

## 13. Производительность

### Должна ли волновать копирование?

**По дефолту — нет.**

- State immutable
- При `copy()` копируется только поверхностный объект — вложенные поля переиспользуются по ссылке
- Не «копируется весь мир чтобы лампочку поменять»

### Когда становится проблемой

Обычно — **в селекторах**:
- Делают join-подобные операции
- Пересчитывают на каждом state change

### Решение: кэширование селекторов

```kotlin
// Кэшировать по immutable-данным state
class CartTotalSelector {
    private var lastServices: List<Service>? = null
    private var lastSelected: Set<ServiceId>? = null
    private var lastResult: Money? = null

    fun total(state: CartState): Money {
        if (state.services === lastServices && state.selectedServiceIds === lastSelected) {
            return lastResult!!
        }
        val result = state.services
            .filter { it.id in state.selectedServiceIds }
            .sumOf { it.price }
        lastServices = state.services
        lastSelected = state.selectedServiceIds
        lastResult = result
        return result
    }
}
```

**Ключевая идея:** state immutable → можно сравнивать **по ссылке**, не по equals (быстро).

---

## 14. Редактируемые поля

### Сценарий

Экран редактирования заметки:
- Заметка приходит с backend как domain model `Note(offerId, text)`
- Пользователь редактирует текст
- При сохранении — отправляется обновлённая модель

### Антипаттерн: редактировать domain model

```kotlin
// ❌ Кладём domain model и мутируем её
data class NoteEditState(
    val note: Note,
)

// Изменение текста:
state.copy(note = state.note.copy(text = newText))  // мутируем domain
```

Проблемы:
- Domain model скопировалась — теряем оригинал
- Сложно реализовать undo (откат к оригиналу)
- Если backend перезагрузит — конфликт с локальными изменениями

### Решение: domain model + редактируемое поле отдельно

```kotlin
// ✅ Разделение
data class NoteEditState(
    val originalNote: Note,         // domain model — нетронутая
    val editedText: String,         // редактируемое поле — feature-level
)
```

Преимущества:
- `originalNote` остаётся неизменной до следующей загрузки с backend
- Откат: `editedText = originalNote.text`
- Undo-стек: `val textHistory: List<String>` — последовательность правок

### Где хранится edit-стек

```kotlin
data class NoteEditState(
    val originalNote: Note,
    val textHistory: List<String>,     // история правок
) {
    val currentText: String get() = textHistory.last()
}

// Undo: textHistory.dropLast(1)
// Redo: ... отдельная структура
```

---

## 15. Индексы

### Зачем

Backend часто отдаёт `List<Offer>` и `List<Note>` где `Note.offerId` — связь.

«Получить заметку для offer» через `notes.find { it.offerId == offerId }` — линейный поиск каждый раз.

### Решение: индекс как map

```kotlin
data class OfferDetailsState(
    val offers: List<Offer>,
    val notes: List<Note>,
) {
    // Индекс — computed property
    val notesByOfferId: Map<OfferId, Note>
        get() = notes.associateBy { it.offerId }
}
```

Или хранить в state (если строится редко):

```kotlin
data class OfferDetailsState(
    val offers: List<Offer>,
    val notes: List<Note>,
    val notesByOfferId: Map<OfferId, Note>,  // index
)
```

### Когда строить индекс

| Сценарий | Стратегия |
|---|---|
| Часто читать, редко обновлять | Хранить в state |
| Редко читать, часто обновлять | Computed property |
| Большие коллекции | Хранить |

**Принцип БД:** индекс ускоряет чтение, замедляет запись. Применять там где много чтений.

---

## 16. Инварианты State

**State-инвариант** — утверждение об одновременной валидности комбинации полей в одном снимке state. Например: «если `editMode = true`, то `edited != null`».

Граница между **state-инвариантом** и **reducer-правилом** часто стирается — особенно когда инвариант формулируется через действие («после X не должно быть Y»). Это ошибка моделирования: правила про переходы — это reducer-логика, а не свойство state.

### Snapshot-test инварианта

**Правило:** state-инвариант должен проверяться по **одному снимку state**, без знания истории Msg, без знания предыдущего state, без знания текущего пользовательского действия.

Если для проверки нужно знать «что было до» или «что происходит сейчас» — это **не state-инвариант, это reducer-правило**.

Формально: state-инвариант — это предикат `P(state): Boolean`. Если предикат требует ещё аргументов (`prev: State`, `msg: Msg`, `flag: Boolean`) — это reducer-правило.

### Маркировка инвариантов

Каждый инвариант в контракте State помечается одним из тегов:

- **`[shape]`** — свойство одного снимка state. Принадлежит разделу State. ✅
- **`[transition]`** — свойство пары состояний или связки state + действие. Принадлежит разделу Reducer / Msg. ❌ в State.

`[transition]` инварианты **запрещены** в разделе State контракта — их место в разделе Msg/Reducer.

### Пример (из IS479)

❌ **Плохо** (написано как state-инвариант, на самом деле transition):

> «Если `isCreatingLexeme = true`, то при нажатии `NavigateBack` экран не закрывается, а сначала сбрасывает NOT_IN_DB-лексему.»

Это `[transition]` — оно про реакцию на `Msg.NavigateBack`. Snapshot-test провалится: «закрывается экран или нет» нельзя проверить по полю state. Место — reducer-правило в разделе Msg.

✅ **Хорошо** (state-инвариант):

> «Если `isCreatingLexeme = true`, то в `lexemeList` существует ровно одна лексема с `id ∈ NOT_IN_DB`.»

Это `[shape]` — проверяется одним предикатом по `state.isCreatingLexeme` и `state.lexemeList`. Snapshot-test проходит.

### Почему это важно

State-инварианты ловятся типом / тестом отдельно от reducer-правил. Смешение их в одном списке порождает три проблемы:

1. **Каскадные переделки** — reducer-правило в State прорастает в Msg и UseCase, downstream шаги получают противоречивые требования.
2. **Невозможный review** — reviewer не может проверить инвариант без репродукции истории Msg.
3. **Архитектурная путаница** — границы слоёв размываются, читателю контракта неясно «что хранится» vs «как изменяется».

---

## 17. Q&A

### Q: NavController — часть state?

**Зависит от scope state:**

| Scope state | Стек навигации в state? |
|---|---|
| Per-screen | **Нет** — state одного экрана не знает про навигацию |
| Per-flow (несколько экранов) | **Да** — стек в state, библиотечного уровня |
| App-level | **Да** — стек определённо в state |

### Q: Когда отдельный `Idle` state vs шорткат-getter?

- **В render UI** — обрабатывать все 4 состояния (Idle/Loading/Error/Success) чтобы показывать что-то
- **В бизнес-логике** — использовать шорткат-property типа `state.value: T?` который возвращает T только в Success

```kotlin
val Loadable<T>.value: T?
    get() = (this as? Loadable.Success)?.value
```

Бизнес-логика часто пишет: «при нажатии на клиента — открыть детали». Это имеет смысл только когда клиенты есть. Если состояние не Success — игнорировать message (асинхронный рассинхрон).

### Q: Selector ≠ Reducer

| Концепция | Что делает |
|---|---|
| **Reducer** | Берёт state + msg → новый state. Аккумулирует. |
| **Selector** | Берёт state → проекция. Не порождает новый state. |

Селектор похож на VIEW в БД, не на UPDATE.

### Q: data class или sealed class для top-level state?

**Автор советует: `data class` для top-level, даже если очевидно нужен `sealed class`.**

Причины:
- `data class` генерирует `copy()` — удобно для апдейтов
- Переход `sealed → data` и обратно — болезненный рефакторинг
- Sum/product взаимные переводы — тяжёлые операции

**Стратегия:**
- Top-level state = `data class`
- Внутри — sealed class когда нужно XOR

```kotlin
// ✅ Top-level data class
data class CustomerListState(
    val content: LoadableContent,    // ← inner sealed class
)

sealed class LoadableContent {
    data object Idle : LoadableContent()
    data object Loading : LoadableContent()
    data class Error(val cause: Throwable) : LoadableContent()
    data class Success(val items: List<Customer>) : LoadableContent()
}
```

### Q: Empty state как отдельный variant?

**Нет смысла.**

`Success(items = emptyList())` уже различим: `items.isEmpty()`. Создавать отдельный `Empty` variant — дублирование. В render просто:

```kotlin
when (state.content) {
    is Success -> if (items.isEmpty()) renderEmpty() else renderList(items)
    // другие ветки: Idle, Loading, Error
}
```

---

## 18. Итоги

### Чеклист

✅ **State — это данные, не объекты.**
- Нет поведения, нет identity, нет наследования
- Только immutable факты

✅ **Использовать ADT.**
- `data class` (product) для независимых полей
- `sealed class` (sum) для взаимоисключающих состояний

✅ **Не плодить классы — научиться считать варианты.**
- `T?` = `|T| + 1`
- product = умножение, sum = сложение
- Видишь повтор → объединяй через nullable / status

✅ **Соблюдать Dependency Rule моделей.**
- Feature → Domain → Library, наружу нельзя
- Не лепить feature-флаги в domain model

✅ **С ростом state — превращать его в БД.**
- Появилась коллекция → начинай нормализовать
- Set вместо Map<Id, Boolean>
- Computed properties (selectors) вместо хранимых вычисляемых
- Индексы (Map<Id, T>) для поиска по связям

✅ **State и его проекции — разные.**
- State = бизнес-логика, БД
- Selectors = view-model для конкретного UI
- ViewModel в UDF ≠ MVVM ViewModel

✅ **Performance — обычно не проблема.**
- Immutable copy быстрая
- Кэшировать селекторы по reference equality

✅ **Редактируемые поля отдельно от domain model.**
- Domain — оригинал
- Feature — редактируемая копия + история

✅ **State-инварианты — только `[shape]`, не `[transition]`.**
- Snapshot-test: проверяется по одному снимку state без истории Msg
- Правила про переходы — в reducer, не в State

### Главная мысль доклада

> **Формируйте образ мышления, а не выбирайте библиотеку.**

Доклад не про конкретный фреймворк (Redux/MVI/TEA/BLoC) — про **способ работать со state**:
1. State — данные
2. Описываем данные через ADT + коллекции
3. Растёт — становится БД
4. Проекции — селекторами

---

## Ссылки

- [Видео доклада на YouTube](https://www.youtube.com/watch?v=y0CHhHBzEkw)
- [Mobius Piter 2021 — страница доклада](https://mobius-piter.ru/2021/spb/talks/4wrmwp7kpzqzfxxuez5qq9/)
- Михаил Левченко — [@themishkun](https://t.me/themishkun)
- Scott Wlaschin — про ADT и domain modelling ([f-sharpforfunandprofit.com](https://fsharpforfunandprofit.com))
