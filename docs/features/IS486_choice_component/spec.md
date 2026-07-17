# Спецификация: компоненты лексемы и иерархия зависимостей

Единая спека системы компонентов: концепция иерархии (IS486) + текущая реализация конструктора (IS481 phase 2 + редизайн IS485) + карточка слова.
Статус: черновик в папке фичи; после обкатки переезжает в `docs/handbook/specs/` и **полностью заменяет** component-constructor spec. Разделы 1–12 — домен и механика (итоговое состояние с IS486), разделы 13–19 — экранный слой в текущем виде (rename-флоу удалён в IS485; IS486-расширения экранов перечислены в §12).

---

## 1. Концепция

**Лексема** — значение слова. Содержимое лексемы полностью описывается **компонентами**: перевод, часть речи, примеры, определение — всё это компоненты, различающиеся шаблоном и местом в иерархии.

**Компонент** (тип, `ComponentType`) — определение «что можно заполнить у лексемы»: имя, шаблон содержимого, кардинальность, место в иерархии. **Значение компонента** (`ComponentValue`) — конкретное наполнение у конкретной лексемы.

**Иерархия.** Каждый компонент зависит ровно от одного узла. Узел — одно из трёх:

- **лексема** — супер-корень; все деревья компонентов растут из неё;
- **компонент** — «доступен, когда у парента есть значение» (Пример → Перевод);
- **опция** CHOICE-компонента — «доступен при конкретном значении» (Род → «существительное»).

**Закон доступности:** компонент доступен для заполнения ⇔ его цель активна:

- лексема активна ⇔ оформлена (не черновик);
- компонент активен ⇔ у лексемы есть его значение;
- опция активна ⇔ у лексемы выбрана именно она.

**Ядро (core)** — флаг, возможный только у зависимых от лексемы. Ядро доступно всегда (включая черновик) и оформляет лексему. Единственное исключение из закона доступности — по определению: ядра — то, из чего лексема строится; остальное — то, что на неё навешивается.

**Черновик.** Лексема оформлена ⇔ заполнено хотя бы одно ядро. Не-ядра (в т.ч. зависимые от лексемы, как Часть речи) черновик не закрывают.

---

## 2. Термины

- **Компонент** — тип (`ComponentType`); **значение** — наполнение у лексемы (`ComponentValue`).
- **Шаблон** (`ComponentTemplate`) — форма содержимого: TEXT | IMAGE | CHOICE.
- **Опция** (`ComponentOption`) — один из предопределённых вариантов CHOICE-компонента.
- **Узел / цель** (`DependencyTarget`) — то, от чего зависит компонент: Lexeme | Component | Option.
- **Ядро (core)** — зависимый от лексемы компонент, оформляющий её.
- **Предки** — цепочка целей от компонента вверх до лексемы.
- **Участие** — компонент показывается и заполняется в лексемах (см. правило участия).
- **Degraded** — вычисляемое состояние: цель компонента удалена.
- **Disabled** — `enabled = false`, ручной рубильник.
- **Черновик** — лексема без единого заполненного ядра.

---

## 3. Шаблоны

| Шаблон | Содержимое значения | Мульти | Статус |
|---|---|---|---|
| TEXT | текст (JSON `TemplateValues`) | разрешён | released |
| IMAGE | изображение (JSON) | разрешён | в UI задизейблен (IS485, фича картинок не готова) |
| CHOICE | ссылка на опцию (`option_id`) | **запрещён** | IS486, новый |

**Правило мульти:** запрет привязан к шаблону, не к зависимости — структура CHOICE подразумевает выбор одной вершины из набора. Запрет мягкий: валидация UseCase/UI, схема хранения single не зашивает (значения CHOICE — та же таблица values, constraint «одна строка» в валидации). Разрешить multiple позже = снять валидацию, без миграции. Зависимость мульти не запрещает: «Пример» (TEXT, мульти) может зависеть от перевода.

**Template immutability** (существующее правило, сохраняется): шаблон компонента после создания менять нельзя — `EditOutcome.TemplateImmutable` на UseCase-уровне, без обращения к data API.

---

## 4. Builtin-набор

**Builtin — пословарные** (решение 2026-07-17): при создании словаря к нему seed'ятся дефолтные builtin-строки. У каждого словаря — **свои** строки перевода и части речи: свой рубильник `enabled`, свои опции. Глобальных компонентов в продукте нет — builtin-слой и есть «глобальный» в смысле «есть в каждом словаре» (потенциальный настоящий global-охват — см. §20).

Официальных builtin два:

- **Перевод** (`system_key = 'translation'`) — TEXT, ядро, зависит от лексемы. Особый для тренировок: тренится симметрично (слово-перевод и перевод-слово). Ядерность других компонентов симметричных тренировок не даёт.
- **Часть речи** (`system_key = 'part_of_speech'`) — CHOICE, зависит от лексемы, НЕ ядро. Стартовый набор опций — открытый вопрос. Опции пословарные — набор частей речи зависит от языка словаря.

Builtin-правила:

- rename / delete / смена шаблона — запрещены (`BuiltInProtected`, существующее);
- **disable — можно, пословарно** (мотивирующий кейс: словарь «только определения» — создаёшь кастомное ядро «Определение», выключаешь перевод этого словаря). Ограничение — общий инвариант §7.8: нельзя выключить последнее включённое ядро словаря;
- builtin **показываются в per-dict конструкторе** с ограниченным набором действий: enable/disable (редактируемость опций части речи — открытый вопрос №2). Раньше builtin скрывались полностью — с пословарным рубильником это больше невозможно;
- builtin участвуют как **цели** зависимостей — видимы в пикере цели.

---

## 5. Доменная модель

Всё в `:modules:domain:lexeme` (package `me.apomazkin.lexeme`). Типы даны в **итоговом** виде (после IS486).

### Примитивы и поля

```kotlin
/**
 * Примитивное значение — атом содержимого компонента.
 *
 * - [Text] — строка; пример: `Text("собака")`.
 * - [Image] — URI изображения; пример: `Image("content://media/external/images/42")`.
 * - [Color] — hex-цвет; пример: `Color("#4A49BC")`. Зарезервирован, шаблонами пока не используется.
 */
sealed interface Primitive {
    data class Text(val value: String) : Primitive
    data class Image(val uri: String) : Primitive
    data class Color(val hex: String) : Primitive
}

/**
 * Поле шаблона — именованный слот в JSON-envelope значения.
 *
 * [name] — имя поля внутри envelope; пример: `"value"`.
 * [type] — тип примитива в этом поле; пример: `PrimitiveType.TEXT`.
 */
data class Field(
    val name: String,
    val type: PrimitiveType,
)

/** Тип примитива поля: TEXT — строка, IMAGE — URI картинки, COLOR — hex-цвет (резерв). */
enum class PrimitiveType { TEXT, IMAGE, COLOR }
```

### Шаблон

```kotlin
/**
 * Шаблон содержимого компонента — определяет, чем выражено значение.
 *
 * [key] — стабильный строковый ключ для БД (`component_types.template_key`); пример: `"choice"`.
 * [fields] — состав JSON-envelope значения: TEXT/IMAGE — одно поле `"value"`;
 *   CHOICE — пусто, значение выражено ссылкой `option_id` (вне fields-модели).
 */
enum class ComponentTemplate(val key: String) {
    TEXT("text"),
    IMAGE("image"),
    CHOICE("choice"),   // IS486
    ;

    val fields: List<Field> get() = when (this) {
        TEXT -> listOf(Field("value", PrimitiveType.TEXT))
        IMAGE -> listOf(Field("value", PrimitiveType.IMAGE))
        CHOICE -> emptyList()   // значение — ссылка на опцию, вне fields-модели
    }

    companion object {
        /** Fail-soft парсинг: unknown key → null + caller логирует в Crashlytics. */
        fun fromKey(key: String): ComponentTemplate? = entries.firstOrNull { it.key == key }
    }
}
```

### Значения

```kotlin
/** Типизированное по шаблону значение компонента у лексемы. */
sealed interface TemplateValues

/** Текстовое значение; пример: `TextValues(Primitive.Text("перевод слова"))`. */
data class TextValues(val value: Primitive.Text) : TemplateValues

/** Значение-картинка; пример: `ImageValues(Primitive.Image("content://..."))`. */
data class ImageValues(val value: Primitive.Image) : TemplateValues

/**
 * IS486: значение CHOICE — выбранная опция.
 * [optionId] — id строки `component_options`; пример: `ChoiceValues(optionId = 7)` — «существительное».
 * Сама опция и есть значение — текст лейбла не копируется.
 */
data class ChoiceValues(val optionId: Long) : TemplateValues
```

### Компонент, опция, зависимость

```kotlin
/**
 * Определение компонента — «что можно заполнить у лексемы».
 *
 * [id] — идентификатор типа.
 * [systemKey] — ключ builtin (TRANSLATION / PART_OF_SPEECH); null → user-defined.
 * [dictionaryId] — словарь-владелец; null → global (возможность в коде, в UI не используется).
 * [name] — имя; у builtin null (display из enum), у user-defined обязательно; пример: `"Род"`.
 * [template] — шаблон содержимого; пример: `CHOICE`.
 * [position] — порядок в списках.
 * [isMultiple] — несколько значений на лексему; пример: true у «Пример». Для CHOICE всегда false (§3).
 * [core] — IS486: ядро — оформляет лексему, доступно в черновике; валиден только при
 *   [dependsOn] = Lexeme; пример: true у Перевода, false у «Части речи».
 * [enabled] — IS486: рубильник; false → компонент и ветка вниз скрыты, значения хранятся.
 *   Разрешён любому, включая ядро/builtin; запрет один — последнее включённое ядро словаря (§7.8).
 * [dependsOn] — IS486: цель зависимости; пример: `Option(7)` у «Рода» — доступен при «существительное».
 * [createdAt] / [updatedAt] — audit; при создании равны (§10).
 * [removedAt] — soft-delete; null → живой.
 */
data class ComponentType(
    val id: ComponentTypeId,
    val systemKey: BuiltInComponent?,
    val dictionaryId: Long?,
    val name: String?,
    val template: ComponentTemplate,
    val position: Int,
    val isMultiple: Boolean = false,
    val core: Boolean = false,
    val enabled: Boolean = true,
    val dependsOn: DependencyTarget,
    val createdAt: Date,
    val updatedAt: Date,
    val removedAt: Date? = null,
)

/**
 * IS486: цель зависимости — узел, при активности которого компонент доступен (§1).
 *
 * - [Lexeme] — сама лексема: доступен у оформленной; пример: «Часть речи».
 * - [Component] — другой компонент: доступен, когда у того есть значение;
 *   [Component.typeId] — его id; пример: «Пример» → Перевод.
 * - [Option] — опция CHOICE-компонента: доступен при конкретном выборе;
 *   [Option.optionId] — id опции; пример: «Род» → «существительное».
 */
sealed interface DependencyTarget {
    data object Lexeme : DependencyTarget
    data class Component(val typeId: ComponentTypeId) : DependencyTarget
    data class Option(val optionId: Long) : DependencyTarget
}

/**
 * IS486: опция CHOICE-компонента.
 *
 * [id] — адрес опции; на него ссылаются зависимости ([DependencyTarget.Option]) и значения ([ChoiceValues]).
 * [componentTypeId] — чья опция; пример: id «Части речи».
 * [label] — текст; пример: `"существительное"`. Переименование (К2) меняет только его.
 * [position] — порядок в списке опций.
 */
data class ComponentOption(
    val id: Long,
    val componentTypeId: ComponentTypeId,
    val label: String,
    val position: Int,
)

/**
 * Builtin-компоненты. [key] — стабильный ключ в `component_types.system_key`.
 *
 * - [TRANSLATION] — Перевод: TEXT, ядро; особый для тренировок (симметричный квиз).
 * - [PART_OF_SPEECH] — IS486: Часть речи: CHOICE, зависит от лексемы, не ядро.
 */
enum class BuiltInComponent(val key: String) {
    TRANSLATION("translation"),
    PART_OF_SPEECH("part_of_speech"),
}
```

### Impact и снапшоты

```kotlin
/**
 * Превью каскада удаления компонента (read-only; показывается в confirm-диалоге).
 *
 * [valueCount] — сколько живых значений компонента скроется; пример: 23.
 * [dictionariesWithValues] — id словарей, где у компонента есть значения; пример: `[1, 4]`.
 * [affectedQuizConfigs] — квиз-конфиги, ссылающиеся на компонент (будут почищены каскадом).
 * [affectedPrefs] — id словарей, чьи prefs квиз-пикера сбросятся.
 * [degradedComponents] — IS486: компоненты, которые станут degraded (их цель умирает);
 *   пример: удаляем «Часть речи» → `[id «Рода»]`.
 * [descendantValueCount] — IS486: значения потомков по цепочке вниз, которые сбросятся каскадом;
 *   пример: 5 значений «Рода» у лексем.
 */
data class DeletionImpact(
    val valueCount: Int,
    val dictionariesWithValues: List<Long>,
    val affectedQuizConfigs: List<AffectedQuizConfig>,
    val affectedPrefs: List<Long>,
    val degradedComponents: List<ComponentTypeId>,
    val descendantValueCount: Int,
)

/**
 * Затронутый квиз-конфиг.
 *
 * [dictionaryId] — словарь, которому принадлежит конфиг.
 * [quizMode] — режим квиза; пример: `"write"`.
 */
data class AffectedQuizConfig(
    val dictionaryId: Long,
    val quizMode: String,
)

/**
 * Снапшот экрана компонентов словаря.
 *
 * [dictionaryId] — словарь-контекст экрана.
 * [dictionaryName] — его имя (для заголовка); пример: `"Английский"`.
 * [types] — компоненты словаря (IS486: включая builtin-строки словаря).
 * [valueCountByType] — живые значения по типам в этом словаре; пример: `{3: 7}`.
 * [optionsByType] — IS486: опции CHOICE-типов; пример: `{5: [существительное, глагол]}`.
 *   Отдельная map — [ComponentType] не раздувается списком опций.
 */
data class PerDictionarySnapshot(
    val dictionaryId: Long,
    val dictionaryName: String,
    val types: List<ComponentType>,
    val valueCountByType: Map<ComponentTypeId, Int>,
    val optionsByType: Map<ComponentTypeId, List<ComponentOption>>,
)
```

Manager-типы (`UserDefinedTypesSnapshot`, `ComponentUsage`) — §20 (потенциальная фича).

### Outcomes

```kotlin
/** Результат создания компонента. */
sealed interface CreateOutcome {
    /** Создано N rows (по одному на словарь; в UI всегда 1 — создание на один словарь). */
    data class Success(val created: List<ComponentType>) : CreateOutcome
    /** Имя пустое после trim — валидация UseCase, без обращения к data API. */
    data object NameEmpty : CreateOutcome
    /** Имя занято живым типом в том же scope (тот же словарь / global). */
    data object SameScopeCollision : CreateOutcome
    /** Имя занято в противоположном scope: global против per-dict (инвариант §7.2). */
    data object CrossScopeCollision : CreateOutcome
    /** Exception data-слоя (try-catch на UseCaseImpl; CancellationException re-throw). */
    data class Failure(val cause: Throwable) : CreateOutcome
    // Removed не моделируется — Create не оперирует existing id; коллизия с soft-deleted
    // именем покрывается SameScope/CrossScope (фильтр removed_at IS NULL).
}

/** Результат soft-delete компонента. */
sealed interface DeleteOutcome {
    /** Удалено; [impact] — фактический каскад (для snackbar «N values hidden»). */
    data class Success(val impact: DeletionImpact) : DeleteOutcome
    /** Попытка удалить builtin — запрещено. */
    data object BuiltInProtected : DeleteOutcome
    /** removed_at IS NOT NULL — повторный soft-delete (race с параллельным удалением). */
    data object Removed : DeleteOutcome
    /** Exception data-слоя. */
    data class Failure(val cause: Throwable) : DeleteOutcome
}

/** Результат редактирования компонента (имя / кардинальность / цель). */
sealed interface EditOutcome {
    /** UPDATE прошёл; cascade quiz_configs.component_refs выполнен если name изменился. */
    data class Success(val updated: ComponentType) : EditOutcome
    /** Имя пустое после trim — валидация UseCase, без обращения к data API. */
    data object NameEmpty : EditOutcome
    /** Name занят в том же scope (dictionary_id + system_key IS NULL + removed_at IS NULL). */
    data object SameScopeCollision : EditOutcome
    /** Name занят в global / другом dict (cross-scope invariant). */
    data object CrossScopeCollision : EditOutcome
    /**
     * Downgrade isMultiple true → false заблокирован — есть лексемы с count > 1.
     * impactedLexemeIds — полный список, deterministic sort
     * (ORDER BY component_values.updated_at DESC, lexeme_id ASC).
     * Reducer делит: InlineOnly (size ≤ 3) | InlineWithDrillIn (size > 3, inline = take(3)).
     */
    data class CardinalityDowngradeBlocked(val impactedLexemeIds: List<Long>) : EditOutcome
    /** Попытка изменить template — возврат БЕЗ обращения к data API. */
    data object TemplateImmutable : EditOutcome
    /** Попытка редактировать builtin — запрещено. */
    data object BuiltInProtected : EditOutcome
    /** removed_at IS NOT NULL — soft-deleted (race с параллельным удалением). */
    data object Removed : EditOutcome
    /** IS486: перепривязка создала бы цикл (алгоритм §8). */
    data object CycleDetected : EditOutcome
    /** IS486: isMultiple = true для шаблона CHOICE запрещён. */
    data object MultiForbiddenForChoice : EditOutcome
    /** Exception data-слоя. */
    data class Failure(val cause: Throwable) : EditOutcome
}
```

CRUD опций (К1–К5) — новое семейство outcome, контракт фиксируется при дизайне data API: Success | LabelEmpty | Removed | Failure; удаление опции с живыми ссылками — через impact-превью (К4/К5).

**Dead path:** rename-флоу удалён в IS485, переименование — через Edit. Оставшиеся в data-слое `renameComponentType` / `RenameOutcome` — на выпил (зафиксировано в Backlog).

---

## 6. Состояния компонента

- **Активный** — цель жива, `enabled = true`.
- **Degraded** — вычисляемое состояние: цель указывает на удалённый (`removed_at`) узел. Ссылки при деградации **не зануляются** — restore цели автоматически оживляет компонент. В списке компонентов помечается, в лексемах не участвует.
- **Disabled** — `enabled = false`. Значения хранятся, но не показываются; ветка вниз выпадает из участия; в новых лексемах не указать. Re-enable возвращает всё на место.
  - Дизейблить можно любой компонент, включая ядро и builtin (пословарно). Единственный запрет — §7.8: последнее включённое ядро словаря не выключить.
  - Дизейбл ядра со значениями — impact-превью: «N лексем станут черновиками» (державшиеся только на нём деградируют по общему правилу).

Состояния независимы: компонент может быть degraded и disabled одновременно; участвует — только активный.

**Правило участия** — компонент участвует в лексеме, когда:

- `enabled` у него и у всех предков;
- цепочка предков доходит до лексемы по живым (не удалённым) узлам;
- для не-ядер дополнительно: лексема оформлена.

Дети degraded/disabled-компонента выпадают автоматически — отдельный флаг им не нужен.

### Переходы

| Событие | Результат |
|---|---|
| создание | активный (инвариант создания гарантирует цель) |
| цель удалена (soft-delete компонента-парента / опции) | degraded (вычисляемо) |
| цель восстановлена | активный (вычисляемо, автоматически) |
| перепривязка degraded | активный (без подтверждения — терять нечего) |
| `enabled = false` | disabled (запрещено только последнему включённому ядру словаря — §7.8) |
| `enabled = true` | активный/degraded (по цели) |
| soft-delete самого компонента | удалён (existing семантика `removed_at`) |

---

## 7. Инварианты

1. **Инвариант создания** (UseCase-валидация; Room не умеет CHECK — дом-стиль M13):
   - при создании компонент обязан иметь цель: лексема | компонент | опция;
   - создать сразу degraded нельзя;
   - `core = true` валиден только при цели-лексеме;
   - обе depends-ссылки заполнены одновременно — нелегально.
2. **Уникальность имени** (существующее, сохраняется): имя уникально в своём scope и cross-scope (`userdefined_identity_invariant`); enforce через two-prong SELECT перед INSERT (UNIQUE-индекса нет — поддержка пересоздания после soft-delete).
3. **Ацикличность** (см. §8).
4. **Инвариант значений:** пустых значений не существует — значение либо живое и непустое, либо удалено (`removed_at`).
5. **Мульти:** CHOICE всегда single (мягкий запрет, §3).
6. **Template immutability** (существующее): шаблон после создания не меняется.
7. **Кросс-границы:** кросс-словарные зависимости запрещены на уровне домена — дерево всегда целиком внутри словаря. С пословарными builtin (§4) исключений нет: зависимость от перевода — ссылка на строку перевода **своего** словаря.
8. **Минимум одно включённое ядро словаря:** нельзя выключить (`enabled = false`) последнее включённое ядро — иначе словарь мёртв: черновик нечем оформить, лексему не создать. UseCase-валидация при disable.

---

## 8. Ацикличность

**Инвариант.** У каждого компонента одна цель, лес растёт из лексемы. Цикл возможен только назначением цели, делающим компонент предком самого себя.

**Алгоритм проверки** (назначаем компоненту C цель P):

1. Нормализация: если P — опция, `P := компонент-владелец опции` (ловит и перепривязку к собственной опции).
2. `X := P`; пока X — компонент: если `X == C` → отказ (цикл); иначе `X :=` цель X (для опции-цели — её владелец).
3. Дошли до лексемы, не встретив C → допустимо.

Сложность O(глубины цепочки). При создании проверка не нужна — у нового компонента нет потомков. Для UI пикера цели: из кандидатов исключаются сам C и всё его поддерево.

---

## 9. Кейсы поведения

### 9.1 Жизненный цикл лексемы

- **Создание (черновик):** доступны для заполнения только ядра.
- **Оформление:** заполнено первое ядро → лексема оформлена; открываются зависимые от лексемы не-ядра (Часть речи) и далее вглубь по мере активации целей.
- **Деградация в черновик:** удалено значение последнего заполненного ядра → лексема снова черновик; значения зависимых от лексемы не-ядер сбрасываются по общему закону каскада (цель «лексема» деактивировалась). Запретов на удаление нет.
- Существующая механика WordCard «удалён последний компонент → лексема удаляется» **заменяется** деградацией в черновик; каскадное удаление лексемы — только явным действием пользователя.

### 9.2 Значения

- **Добавление:** только у участвующих компонентов при активной цели. CHOICE — выбор одной опции (`option_id`).
- **Изменение TEXT/IMAGE:** обычный edit, каскадов нет (значение остаётся живым — цель детей активна).
- **Смена значения CHOICE** (существительное → глагол): цель-опция деактивировалась → **полный сброс** значений всех зависимых по цепочке вниз. Никаких скрытых сохранений.
- **Удаление значения:** цель деактивировалась → сброс зависимых по цепочке вниз. Если это было последнее ядро — деградация лексемы в черновик (§9.1).
- **Сброс = soft delete** (`removed_at` в values); для приложения значение исчезло.

### 9.3 Опции (К1–К5)

- **К1. Добавление** — свободно, опция сразу доступна.
- **К2. Переименование** — свободно; id устойчив, значения не трогаются, лейбл меняется везде задним числом.
- **К3. Удаление без ссылок** (никто не выбрал, никто не зависит) — свободно.
- **К4. Удаление с живыми значениями** — impact-превью («выбрана у N лексем, значения будут удалены. Продолжить?») + каскадный сброс значений по цепочке вниз.
- **К5. Удаление опции с зависимым компонентом** — опция soft-delete; зависимый компонент становится degraded вычисляемо (ссылка живёт, указывает на удалённую опцию). Restore опции — компонент ожил. Деградация никогда не меняет доступность молча (отвергнуты «повысить до ядра» и «расширить до любого значения парента»).

### 9.4 Компоненты

- **Создание:** имя + шаблон + цель (+ опции для CHOICE, + core при цели-лексеме). Только на **один словарь** (multi-dict путь в коде остаётся, из UI уходит; валидация: «несколько словарей + зависимость» не поддерживается). Инвариант создания §7.1; коллизии имён — существующие ветки.
- **Редактирование (Edit):** имя (с коллизиями), кардинальность (с `CardinalityDowngradeBlocked` и превью затронутых лексем), шаблон — immutable. Для CHOICE: мульти запрещён (`MultiForbiddenForChoice`).
- **Перепривязка (смена цели):** разрешена всегда, с валидацией ацикличности (§8):
  - живой компонент — impact-превью + **полный сброс** значений (своих и потомков) + перепривязка; любая смена условия = сброс, без «умных» исключений — сначала настрой иерархию, потом заполняй;
  - degraded — просто перепривязка, без подтверждения.
- **Disable / enable:** §6. Можно любому, включая ядро и builtin; запрет один — последнее включённое ядро словаря (§7.8). Дизейбл ядра со значениями — impact «N лексем станут черновиками».
- **Удаление (soft-delete):** impact-превью + подтверждение. `DeletionImpact` расширяется: деградирующие потомки (список), счётчики значений по цепочке. Каскад существующий (cleanup quiz-конфигов + prefs, одна транзакция) сохраняется; дети НЕ удаляются и НЕ повышаются — становятся degraded вычисляемо; их значения сбрасываются по общему каскаду.
- **Restore компонента** (data-уровень, без UI): дети-degraded оживают автоматически (вычисляемое состояние). Значения детей НЕ восстанавливаются (были сброшены каскадом).
- **CRUD опций** — отдельные операции Edit-флоу CHOICE-компонента (К1–К5).

### 9.5 Словари

- **Создание словаря:** seed **пословарных** builtin-строк — свой «Перевод» (ядро) + своя «Часть речи» (CHOICE, не ядро) со стартовым набором опций. Дефолтный состав нового словаря — только builtin.
- **Удаление словаря:** все компоненты словаря (кастомные и builtin-строки) умирают вместе с ним (FK CASCADE); вопросов деградации не возникает — дерево целиком внутри словаря (§7.7).

### 9.6 Квизы

- **Чистка конфигов — только при удалении** компонента (существующий каскад). Disabled/degraded — обратимые состояния: конфиги живут.
- **Скип на лету:** сборка квиза фильтрует компоненты правилом участия; пикер конфигов выпавшие не предлагает.
- **CHOICE в v1 в квизах не участвует** (исключается тем же фильтром); задел держать.
- Резолв name-based `ComponentTypeRef` → тип — единой функцией (миграция refs на id — Backlog).

---

## 10. Хранение

Конвенция audit-полей (все таблицы): при создании `created_at` и `updated_at` получают одно и то же значение `now`; `updated_at` меняется только при UPDATE. Инвариант: `updated_at == created_at` ⇔ сущность ни разу не редактировалась.

### `component_types`

- `id` — PK autoincrement; идентификатор компонента.
- `system_key` — стабильный ключ builtin (`'translation'`, `'part_of_speech'`); null → user-defined. IMMUTABLE после INSERT. IS486: UNIQUE-индекс по одному ключу **дропается** (builtin пословарные — по строке на словарь); уникальность «(ключ, словарь)» — UseCase-валидация (дом-стиль M13).
- `dictionary_id` — FK → `dictionaries.id`, CASCADE. IS486: у builtin-строк заполнен (пословарные). null = global — в продукте не используется, остаётся для потенциального global-охвата (§20).
- `name` — имя компонента; null допустим для builtin (display из enum), user-defined требует name. Уникальность — в UseCase (§7.2).
- `template_key` — ключ шаблона: `"text"` / `"image"` / `"choice"`.
- `position` — порядок в списках.
- `is_multiple` — кардинальность; true → несколько значений на лексему. Для CHOICE всегда false (валидация).
- `core` — **IS486**: флаг ядра; валиден только при цели-лексеме.
- `enabled` — **IS486**: рубильник, дефолт true.
- `depends_on_type_id` — **IS486**: ссылка на компонент-цель (FK → `component_types.id`), nullable.
- `depends_on_option_id` — **IS486**: ссылка на опцию-цель (FK → `component_options.id`), nullable.
- `created_at` / `updated_at` — audit timestamps.
- `removed_at` — soft-delete; null → живой.

Состояния depends-ссылок: обе пустые → цель «лексема»; заполнена одна → зависимый от компонента / от опции; обе → нелегально (UseCase-валидация).

### `component_options` (новая, IS486)

- `id` — PK autoincrement; адрес опции, на него ссылаются зависимости и значения.
- `component_type_id` — FK → `component_types.id`, CASCADE; какому CHOICE-компоненту принадлежит.
- `label` — текст опции («существительное»); переименование меняет только его.
- `position` — порядок в списке.
- `created_at` / `updated_at` — audit timestamps.
- `removed_at` — soft-delete; null → живая.

### `component_values`

- `id` — PK autoincrement.
- `lexeme_id` — FK → `lexemes.id`, CASCADE; чья лексема.
- `component_type_id` — FK → `component_types.id`, CASCADE; какой компонент заполнен.
- `value` — JSON-сериализованный `TemplateValues` (envelope `{"fields": {...}}`); для CHOICE — пустой envelope (колонка NOT NULL сохраняется).
- `option_id` — **IS486**: FK → `component_options.id`, nullable; выбранная опция CHOICE-значения. Строка values — факт «компонент у лексемы заполнен»; сама опция и есть значение, копий лейбла нет. Impact К4 — один COUNT по `option_id`.
- `created_at` / `updated_at` — audit timestamps.
- `removed_at` — soft-delete; null → живое. Каскадные сбросы значений пишут сюда (§9.2).

Индексы существующие: `lexeme_id`, `component_type_id` (+ `dictionary_id`, UNIQUE `system_key` на types). UNIQUE `(lexeme_id, component_type_id)` снят ещё в M13 — поддержка `is_multiple`.

Перевод и Часть речи — обычные строки `component_types` (`system_key`); зависимость от них — обычные ссылки.

---

## 11. Миграция 12→13

- Схема: таблица `component_options`; `core`, `enabled`, `depends_on_type_id`, `depends_on_option_id` в types; `option_id` в values; **дроп UNIQUE-индекса `system_key`** (builtin становятся пословарными).
- **Размножение builtin:** единственная глобальная строка translation заменяется пословарными — по строке «Перевод» на каждый существующий словарь; значения лексем перевязываются на строку своего словаря. Seed «Части речи» — по строке на словарь, со стартовым набором опций.
- **Backfill:**
  - Строки «Перевод» — ядро (`core = true`).
  - Кастомный компонент, имеющий хоть одно значение в лексеме без перевода (фактически используется сам — Definition), — ядро.
  - Остальные кастомные — зависимость от перевода **своего словаря** (`depends_on_type_id`).
  - `enabled = true` всем.
- Квиз-refs `system:translation` — резолв по ключу становится пословарным (ключ + словарь конфига).
- Fresh install: seed обоих builtin на каждый создаваемый словарь (внимание на destructive-fallback путь seed — известная дыра из Backlog).
- Побочный эффект принят: пользователь один, иерархию донастроит руками перепривязкой.
- Migration-тесты по образцу `MigrationFrom11to12`.

---

## 12. Влияние на существующую реализацию

- **Каскад-модуль** — новый, единый, рекурсивный, транзакционный (уровень LexemeApi, обход дерева в памяти внутри `immediateTransaction`). Триггеры: смена/удаление значения парента, удаление опции, удаление компонента, перепривязка. Dry-run режим = impact-превью.
- **WordCard:**
  - «удалён последний компонент → лексема удаляется» → деградация в черновик (§9.1);
  - `ComponentValueState` текстоцентричен → typed-контент (sealed Text | Choice), `ChoiceValues` в `TemplateValues`, коммит/`CommitOutcome` ветвится по шаблону;
  - фильтрация чипсов по правилу участия — явными полями state, заполняемыми в reducer (дом-правило: явные флаги, не вычисления в composable);
  - draft-anchor обязан быть ядром (закрывается фильтрацией: не-ядра в черновик не попадают).
- **Impact-превью** (`previewDeletionImpact`) — расширяется полями деградирующих потомков и счётчиками по цепочке; UI-механика превью/подтверждения переиспользуется.
- **Квизы** — фильтр участия при сборке quiz item (`QuizGameImpl`) и в пикере компонентов; graceful skip уже есть.
- **Конструктор (экран компонентов):** Create/Edit-диалоги получают пикер цели и CRUD опций; список помечает degraded/disabled; builtin-строки словаря показываются с ограниченными действиями (§4) и участвуют как кандидаты-цели.
- **Manager-экран:** вход из Settings (`ComponentsManageWidget`) удаляется; модуль `components_manager` остаётся в коде как консерв потенциальной фичи (§20), из навигации недостижим.
- **Spec-правки:** component-constructor spec — CHOICE, пикер цели, опции, расширенный impact, новые ветки EditOutcome, запрет мульти для CHOICE; lexeme-domain spec — устарел (плоские translation/definition), владелец lifecycle-правил лексемы — эта спека.

---

## 13. Экран и точка входа

Разделы 13–19 описывают экранный слой (IS481 phase 2 + редизайн IS485, rename-флоу удалён; IS486-расширения — §12: пикер цели, CRUD опций, degraded/disabled-метки, builtin-строки в списке).

Конструктор компонентов — **один экран**:

- **`PerDictionaryComponentsScreen`** — компоненты словаря. Вход: `DictionaryAppBar` → icon-button «молоток» (`ComponentsToolsIconButton`, `ic_hammer`, виден при выбранном словаре).
- Список: кастомные компоненты словаря + builtin-строки словаря (с ограниченными действиями — §4). До IS486 builtin скрывались.
- Scaffold с `formBackground`, прозрачный TopAppBar (IS485), snackbar host, FAB «Создать»; состояния контента: loading (`CircularProgressIndicator`) / error (`ErrorStateWidget` + retry) / empty (`ComponentsEmptyStateWidget` с CTA) / список (`LazyColumn` строк-карточек).

`ComponentsManagerScreen` (aggregated view из Settings) — **убран из продукта** вместе с глобальным охватом; описание — §20 (потенциальная фича).

## 14. State (экранный слой)

Domain-shared типы (`:modules:domain:lexeme`): `Scope`, `NameError`, outcomes, снапшот, `DeletionImpact` (§5). Screen-specific — в пакете `me.apomazkin.per_dictionary_components.mate` (Manager-пакет — §20).

```kotlin
/**
 * Охват компонента при создании.
 *
 * - [Global] — компонент для всех словарей (`dictionary_id IS NULL`); в UI не используется, живёт в коде.
 * - [PerDictionaries] — привязка к словарям; [PerDictionaries.ids] — их id;
 *   в UI всегда один словарь (решение Д); пример: `PerDictionaries(listOf(1))`.
 */
sealed interface Scope {
    data object Global : Scope
    data class PerDictionaries(val ids: List<Long>) : Scope
}

/**
 * Ошибка имени в Create-диалоге (показывается под полем ввода).
 *
 * - [Empty] — имя пустое после trim.
 * - [SameScopeCollision] — имя занято живым типом в том же scope.
 * - [CrossScopeCollision] — имя занято в противоположном scope (global против per-dict).
 */
sealed interface NameError {
    data object Empty : NameError
    data object SameScopeCollision : NameError
    data object CrossScopeCollision : NameError
}
```

### `PerDictionaryComponentsScreenState`

```kotlin
/**
 * State экрана компонентов словаря.
 *
 * [dictionaryId] — словарь-контекст (init-параметр экрана).
 * [dictionaryName] — имя словаря для заголовка; null пока не загружено.
 * [items] — строки списка; null = не загружено (initial loading),
 *   emptyList = загружено и пусто (empty state).
 * [isLoading] — initial load / refresh в полёте. Explicit-флаг, не вычисляется из null-данных.
 * [isCreating] / [isDeleting] / [isEditing] — submit соответствующей операции в полёте;
 *   блокируют кнопку от двойного тапа (дом-правило: явные флаги, не вычисления в composable).
 * [createDialog] / [deleteConfirm] / [editDialog] — состояние диалога; видим ⇔ != null;
 *   одновременно открыт максимум один (инварианты ниже).
 * [snackbarState] — единственный источник UI-фидбека; UI показывает и сбрасывает `DismissSnackbar`.
 */
data class PerDictionaryComponentsScreenState(
    val dictionaryId: Long,
    val dictionaryName: String? = null,
    val items: List<PerDictRow>? = null,
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val isDeleting: Boolean = false,
    val isEditing: Boolean = false,
    val createDialog: CreateDialogState? = null,
    val deleteConfirm: DeleteConfirmState? = null,
    val editDialog: EditDialogState? = null,
    val snackbarState: SnackbarState? = null,
)
```

### Nested state

```kotlin
/**
 * Строка списка компонентов словаря.
 *
 * [typeId] — id типа (для onEdit/onDelete).
 * [name] — имя; пример: `"Синонимы"`.
 * [template] — шаблон (для чипа «Текст»).
 * [isMultiple] — кардинальность; в UI IS485 бейдж скрыт, данные живут.
 * [isGlobal] — компонент global-охвата; в продукте всегда false (глобалов нет — §20), поле живёт.
 * [valueCount] — живые значения в этом словаре; пример: 7 → «Значений: 7».
 */
data class PerDictRow(
    val typeId: ComponentTypeId,
    val name: String,
    val template: ComponentTemplate,
    val isMultiple: Boolean,
    val isGlobal: Boolean,
    val valueCount: Int,
)

/**
 * State Create-диалога.
 *
 * [name] — вводимое имя.
 * [template] — выбранный шаблон; дефолт TEXT.
 * [isMultiple] — чекбокс «Несколько значений».
 * [scope] — охват; в продукте всегда `PerDictionaries(listOf(dictionaryId))` — текущий словарь.
 * [nameError] — ошибка имени под полем; сбрасывается любым `*Change`.
 * [selectedDictionaryIds] — словари multi-dict пикера; потенциальный Manager (§20), в продукте пусто.
 */
data class CreateDialogState(
    val name: String = "",
    val template: ComponentTemplate = ComponentTemplate.TEXT,
    val isMultiple: Boolean = false,
    val scope: Scope = Scope.Global,
    val nameError: NameError? = null,
    val selectedDictionaryIds: Set<Long> = emptySet(),
)

/**
 * State confirm-диалога удаления.
 *
 * [typeId] — что удаляем.
 * [name] — имя для заголовка «Удалить „Синонимы"?».
 * [impact] — превью каскада; null пока грузится или не запрошен.
 * [isLoadingImpact] — превью в полёте; `impact == null && !isLoadingImpact` — «не запрошен».
 */
data class DeleteConfirmState(
    val typeId: ComponentTypeId,
    val name: String,
    val impact: DeletionImpact? = null,
    val isLoadingImpact: Boolean = false,
)

/**
 * State Edit-диалога.
 *
 * [typeId] — редактируемый тип.
 * [originalName] / [originalTemplate] / [originalIsMultiple] — snapshot на момент открытия:
 *   diff на submit (template-immutability проверяется по нему на UseCase).
 * [name] / [template] / [isMultiple] — текущий ввод.
 * [nameError] — ошибка имени под полем.
 * [impactedLexemesPreview] — превью блокировки cardinality-downgrade; null = не показывается.
 * [epochId] — correlation id: каждый submit ++, `EditResult` со stale epoch игнорируется.
 */
data class EditDialogState(
    val typeId: ComponentTypeId,
    val originalName: String,
    val originalTemplate: ComponentTemplate,
    val originalIsMultiple: Boolean,
    val name: String,
    val template: ComponentTemplate,
    val isMultiple: Boolean,
    val nameError: EditNameError? = null,
    val impactedLexemesPreview: ImpactedLexemesPreview? = null,
    val epochId: Long = 0L,
)

/**
 * Превью лексем, блокирующих cardinality-downgrade.
 *
 * [impactedLexemeIds] — полный список id затронутых лексем (deterministic sort с data-уровня).
 *
 * - [InlineOnly] — 1..3 лексемы: показываются все, drill-in кнопка скрыта.
 * - [InlineWithDrillIn] — больше 3: [InlineWithDrillIn.inlineIds] — top-3 для inline,
 *   drill-in кнопка «Показать все (N)» видна.
 */
sealed interface ImpactedLexemesPreview {
    val impactedLexemeIds: List<Long>
    data class InlineOnly(override val impactedLexemeIds: List<Long>) : ImpactedLexemesPreview
    data class InlineWithDrillIn(
        override val impactedLexemeIds: List<Long>,
        val inlineIds: List<Long>,
    ) : ImpactedLexemesPreview
}

/**
 * Ошибка имени в Edit-диалоге (зеркало [NameError]; отдельный тип — живёт в mate-пакете экрана).
 *
 * - [NameEmpty] — имя пустое после trim.
 * - [SameScopeCollision] / [CrossScopeCollision] — коллизии имён (§7.2).
 */
sealed interface EditNameError {
    data object NameEmpty : EditNameError
    data object SameScopeCollision : EditNameError
    data object CrossScopeCollision : EditNameError
}

/** Snackbar-состояние. [text] — готовый текст сообщения. */
data class SnackbarState(val text: String)
```

- `ImpactedLexemesPreview` с `size == 0` веткой не моделируется — downgrade в этом случае проходит → `Success`, превью не показывается. Reducer `inlineIds` не пересортировывает.
- `CardinalityDowngradeBlocked` / `TemplateImmutable` / `BuiltInProtected` / `Removed` / `Failure` идут НЕ через `nameError` — top-level реакции (snackbar + закрытие диалога либо preview-ветка).

Manager-state (`ComponentsManagerScreenState`, `UserDefinedRow`, `availableDictionaries`) — §20.

### Computed properties

```kotlin
/** Empty state: данные загружены и пусты (не путать с initial loading, где список null). */
val isEmpty: Boolean get() = items?.isEmpty() == true && !isLoading

/** Доступность submit в Create-диалоге: имя непустое; для PerDictionaries выбран хотя бы один словарь. */
val CreateDialogState.canSubmit: Boolean
    get() = name.trim().isNotEmpty() && when (scope) {
        Scope.Global -> true
        is Scope.PerDictionaries -> selectedDictionaryIds.isNotEmpty()
    }
```

### Инварианты state

- `[shape]` открыт не более одного диалога: `createDialog ⊕ deleteConfirm ⊕ editDialog`; любой `Open*Dialog` закрывает остальные.
- `[shape]` in-flight не более одной write-операции: `isCreating ⊕ isDeleting ⊕ isEditing`; флаг подразумевает открытый соответствующий диалог.
- `[shape]` `deleteConfirm.impact == null && !isLoadingImpact` — валидное состояние «preview не запрошен» (отдельное от «грузится»).
- `[transition]` `ConfirmDelete` → `isDeleting=true` + dispatch ровно один раз; повтор при `isDeleting=true` игнорируется.
- `[transition]` `Open*Dialog` сбрасывает in-flight флаги в false.

## 15. Msg и reducer-контракт

Msg-семейства (multi-dict семейство Manager-экрана — §20):

- **Lifecycle:** `ItemsLoaded(snapshot)` / `ItemsLoadFailed(cause)` — из flow-handler'а.
- **Create:** `OpenCreateDialog`, `CloseCreateDialog`, `CreateNameChange`, `CreateTemplateChange`, `CreateMultiToggle`, `CreateScopeChange`, `SubmitCreate`, `CreateResult(epochId, outcome)`.
- **Delete:** `OpenDeleteConfirm(typeId)`, `CloseDeleteConfirm`, `ImpactPreviewLoaded(typeId, impact)`, `ImpactPreviewFailed(typeId, cause)`, `ConfirmDelete`, `DeleteResult(epochId, outcome)`.
- **Edit:** `OpenEditDialog(typeId)`, `CloseEditDialog`, `EditNameChange`, `EditTemplateChange`, `EditMultiToggle`, `SubmitEdit`, `EditResult(epochId, outcome)`.
- **Прочее:** `DismissSnackbar`, `RequestBack`, `OnRetryClick`, `Empty`; `UiMsg.Snackbar(text)`.

Ключевые reducer-реакции:

| Msg | State | Effect |
|---|---|---|
| `ItemsLoaded(s)` | `items = s.toRows(), dictionaryName = s.dictionaryName, isLoading=false` | — |
| `OpenCreateDialog` | `createDialog = CreateDialogState()`; остальные диалоги null; in-flight false | — |
| `CreateScopeChange(Global)` | scope=Global, `selectedDictionaryIds=emptySet()` | — |
| `SubmitCreate` | `isCreating=true; epoch++` | `CreateComponent(epoch, name, template, isMultiple, scope)` |
| `CreateResult(Success)` | `isCreating=false, createDialog=null`, snackbar | — |
| `CreateResult(Same/CrossScopeCollision)` | `nameError` в диалоге, диалог НЕ закрывается | — |
| `OpenDeleteConfirm(id)` | `deleteConfirm(isLoadingImpact=true)`; остальные null | `LoadImpact(id)` |
| `ImpactPreviewLoaded` (typeId == открытый) | `impact`, `isLoadingImpact=false` | — |
| `ConfirmDelete` | `isDeleting=true; epoch++` | `SoftDeleteComponent(epoch, id)` |
| `DeleteResult(Success/Removed)` | закрыть диалог, snackbar | — |
| `OpenEditDialog(id)` | `editDialog` из snapshot строки; остальные null | — |
| `EditMultiToggle(b)` | `isMultiple=b`, `impactedLexemesPreview=null` | — |
| `SubmitEdit` | `isEditing=true; editDialog.epochId++` | `EditComponent(epoch, id, name, template, isMultiple)` |
| `EditResult(CardinalityDowngradeBlocked(ids))` | preview: `InlineOnly` (≤3) / `InlineWithDrillIn` (>3, take(3)); диалог открыт | — |
| `EditResult(TemplateImmutable/BuiltInProtected/Removed/Failure)` | закрыть диалог, snackbar | — |
| `OnRetryClick` | `isLoading=true` | re-subscribe effect |
| `RequestBack` | — | `NavigationEffect.Back` |

Guards: submit-сообщения обрабатываются только при соответствующем in-flight == false; `*Change` сбрасывает `nameError`; `EditMultiToggle` сбрасывает preview; `*Result` со stale epoch и `ImpactPreview*` с чужим typeId — игнорируются.

## 16. IO (эффекты и подписки)

### DatasourceEffect

- `CreateComponent(epochId, name, template, isMultiple, scope)` → `useCase.createUserDefinedComponent` → `CreateResult`.
- `LoadImpact(typeId)` → `useCase.previewDeletionImpact` → non-null: `ImpactPreviewLoaded`; null: `ImpactPreviewFailed(cause=null)`.
- `SoftDeleteComponent(epochId, typeId)` → `useCase.softDeleteComponent` → `DeleteResult`.
- `EditComponent(epochId, typeId, name, template, isMultiple)` → `useCase.editComponent` → `EditResult`.
- `LoadComponentsForDictionary` — re-subscribe триггер retry-флоу.

**Handler-инвариант:** `catch (e: Throwable)` c re-throw `CancellationException` (structured concurrency); остальные exceptions → `Failure`-outcome / `ImpactPreviewFailed`.

`UiEffect.Snackbar(text)`; `NavigationEffect` — только Back.

### FlowHandlers

- `ComponentsForDictionaryFlowHandler`: `flowComponentsForDictionary(dictId)` → `ItemsLoaded` / `ItemsLoadFailed`; init-trigger + re-subscribe.

Manager-эффекты и handlers (`SubscribeDictionaries`, `AllUserDefinedTypesFlowHandler`, `DictionariesFlowHandler`) — §20.

`failureLabel(cause)` — общий helper (`:modules:core:tools`) для snackbar-текстов.

## 17. UseCase-интерфейсы и Data API

### `PerDictionaryComponentsUseCase`

- `flowComponentsForDictionary(dictionaryId): Flow<PerDictionarySnapshot>` — реактивная подписка на компоненты словаря (IS486: включая builtin-строки словаря).
- `createUserDefinedComponent(name, template, isMultiple, scope): CreateOutcome` — валидация имени → two-prong SELECT (same-scope + cross-scope) → INSERT (N строк для N словарей; в продукте — всегда 1, текущий словарь).
- `previewDeletionImpact(typeId): DeletionImpact?` — read-only preview каскада; null = тип не найден/удалён.
- `softDeleteComponent(typeId): DeleteOutcome` — атомарно: `removed_at`, cleanup `quiz_configs.component_refs`, cleanup prefs (best-effort).
- `editComponent(typeId, name, template, isMultiple): EditOutcome` — UseCase-уровень: `NameEmpty` и `TemplateImmutable` без обращения к API; try-catch → `Failure`. API-уровень: `Removed` / `BuiltInProtected` / коллизии / `CardinalityDowngradeBlocked` / `Success` (+cascade refs при смене имени).

`ComponentsManagerUseCase` (flowAllUserDefinedTypes, flowDictionaries) — §20.

### Data API (`core/core-db-api`)

- `LexemeApi.editComponentType(typeId, name, template, isMultiple): EditComponentOutcome` — ветки: Success / SameScopeCollision / CrossScopeCollision / CardinalityDowngradeBlocked(ids) / TemplateImmutable (defense-in-depth) / BuiltInProtected / Removed. Cardinality-SELECT запускается только при downgrade (`true → false`).
- `NameEmpty` и `Failure` в API не входят — UseCaseImpl-уровень.
- IS486 расширит API: CRUD опций, перепривязка цели, каскад-модуль (§12) — контракты при дизайне.

## 18. Виджеты, строки, иконки (актуальный набор после IS485)

Shared-модуль `:modules:widget:component_widgets`:

- **Строки списков:** `UserDefinedRowWidget` / `PerDictRowWidget` → общий `ComponentRowCard` (карточка: иконка типа `ComponentTypeIcon`, имя, «Значений: N», кнопки edit/delete `ComponentIconButton`, чип шаблона `TemplateChip`). Бейджи охвата/мульти/словарей скрыты (данные в сигнатурах живут).
- **Диалоги:** `CreateComponentDialog` (hostVariant Manager — со scope-picker'ом; PerDict — без), `EditComponentDialog` (+`CardinalityDowngradePreviewWidget`), `DeleteComponentConfirmDialog` — собраны из `ComponentDialogParts` (лейблы, поле имени, радио-группа шаблонов с дизейблом IMAGE, чекбокс мульти, кнопки; destructive-вариант для удаления).
- **Прочее:** `ComponentsEmptyStateWidget`, `CreateComponentFab`, `ErrorStateWidget` (core:ui).
- Display-only DTO у диалогов: `DictionaryRef(id, name)`, `DeletionImpactRef(counts)`, `HostVariant` — shared-виджеты не связаны с mate-state экранов (плоские примитивы). `EditNameError.toLabelRes()` — host-local в каждом экране.
- Известные placeholder'ы (Backlog): `lexemeLabel` = "Лексема №N" (нет реального лейбла), `onShowAllImpacted` = no-op (drill-in не реализован).

Строки: семейство `components_*` (диалоги create/edit/delete, ошибки коллизий, scope-лейблы, cardinality-preview, empty/error states). Иконки: `ic_hammer`, `ic_components`, `ic_text_lines` (IS485), `ic_edit`, `ic_trash`, `ic_add`. Палитра — токены темы IS485 (`componentCardBorder`, `typeIconBg`, `iconButtonBg`, `templateChipBg/Text`, `dialogFieldBg`, `radioSelectedBg`, `radioBorderInactive`, `destructiveRed`, `formBackground`).

## 19. Тестовые сценарии (поведенческий контракт)

Rename-сценарии исключены (флоу удалён в IS485). Существующие тесты — неизменяемый контракт.

**Create:** happy path (dispatch → Success → диалог закрыт, snackbar); same-scope collision (nameError, диалог открыт); cross-scope collision; recreate после soft-delete (разрешено); stale epoch (игнор).
**Delete:** preview + confirm (impact-поля → Success → snackbar); preview not-found (null → `ImpactPreviewFailed`, snackbar); double-tap guard; Removed parity (параллельный soft-delete → «Компонент удалён»).
**Edit:** happy path (rename-only → Success + cascade refs); cardinality downgrade blocked ≤3 (InlineOnly) и >3 (InlineWithDrillIn, take(3)); template immutability gate (без вызова API); race с soft-delete (Removed); same-scope collision (nameError, диалог открыт); built-in protected; Failure handling; stale epoch; double-tap guard; downgrade-SELECT precondition (не вызывается без downgrade).
**Общие:** mutual exclusion диалогов (3-way после IS485); пре-селект scope текущим словарём; `CancellationException` re-throw.
**Manager/multi-dict сценарии** (happy path на N словарей, chip staleness, `DictionariesLoaded`-инвариант, global-компоненты в per-dict списке) — §20; их тесты в коде остаются валидными, пока жив Manager-модуль.

**IS486 добавит** (контракты при дизайне): сценарии CHOICE (создание с опциями, выбор значения, запрет мульти), зависимостей (доступность по цели, каскады, ацикличность/`CycleDetected`, перепривязка живого/degraded), degraded/disabled (вычисление, скип в квизах), деградации лексемы в черновик, миграции 12→13 (backfill, seed).

## 20. Потенциальная фича: глобальный охват и Manager-экран (за скобками)

Глобальных компонентов в продукте нет (решение 2026-07-17). Возможность **учитывается в коде**, но не поддерживается в UI. Эта секция — консервация контрактов на случай воскрешения.

### Что остаётся в коде

- `component_types.dictionary_id` — nullable: глобальная строка (NULL) выразима.
- `Scope.Global` и ветки `CrossScopeCollision` — живут, из UI недостижимы.
- Multi-dict путь создания (`Scope.PerDictionaries(N)` → N строк) — живёт, UI создаёт всегда на один словарь.
- Модуль `components_manager` — остаётся в коде, вход из Settings удалён, из навигации недостижим.
- Поля `PerDictRow.isGlobal`, `CreateDialogState.selectedDictionaryIds` — живут, в продукте не заполняются.

### Влияние пословарных builtin на глобалы

- Схема готова: uniqueness builtin уже «(ключ, словарь)» в UseCase — глобальный builtin потом = строка с NULL-словарём, без миграции схемы.
- Единственное решение при воскрешении: **кросс-охватные зависимости** (сейчас запрещены §7.7). Безопасное правило: per-dict может зависеть от global (глобал не умирает со словарём); global от per-dict — нет.
- Manager-контракты ниже остаются концептуально валидными.

### Manager-экран (консервированный контракт)

- **Экран:** aggregated view всех user-defined компонентов из всех словарей; вход был: `SettingsTab` → `ComponentsManageWidget`.
- **State:** `ComponentsManagerScreenState` — зеркало per-dict state (§14) с отличиями: данные — `userDefinedTypes: List<UserDefinedRow>?`; `availableDictionaries: List<DictionaryApiEntity>` для multi-dict пикера.
- **`UserDefinedRow`:** `typeId, name, template, isMultiple, scope (Global | PerDictionaries), usageCount (суммарно по словарям), dictionaryNames (для бейджей)`.
- **Msg:** lifecycle `TypesLoaded`/`TypesLoadFailed`; multi-dict семейство — `CreateDictionaryToggle(dictionaryId)` (toggle выбора), `DictionariesLoaded(list)` (обновление списка + фильтрация stale selection; `editDialog` НЕ мутируется — инвариант).
- **IO:** `AllUserDefinedTypesFlowHandler` (`flowAllUserDefinedTypes()` → `TypesLoaded`), `DictionariesFlowHandler` (`flowDictionaries()` → `DictionariesLoaded`; ошибка collect → пустой список, picker деградирует к Global-only), effect `SubscribeDictionaries` для re-subscribe.
- **UseCase:** `ComponentsManagerUseCase` — те же CRUD + `flowAllUserDefinedTypes(): Flow<UserDefinedTypesSnapshot>` + `flowDictionaries()`.
- **Domain-типы:** `UserDefinedTypesSnapshot(types, usage, optionsByType)`; `ComponentUsage(valueCountByType, dictionaryIdsByType, dictionaryNames)` — aggregated-статистика (пример: `{3: 12}` — у типа 3 двенадцать значений суммарно).
- **Тест-контракты:** multi-dict happy path (N словарей → N строк); submit disabled при пустом selection; chip staleness filtering; `DictionariesLoaded` не мутирует `EditDialogState`; global-компоненты видны в per-dict списке.

## 21. Открытые вопросы

1. **UX флоу создания компонента:** человек без знания механики создаёт компонент — галка «ядро»? Пре-селект цели «перевод»? Решить при UI-дизайне.
2. **Опции builtin «Часть речи»:** стартовый набор (существительное, глагол, ...) — пословарный, задаётся при seed; редактируемость опций builtin пользователем.
