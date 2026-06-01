# Scope analysis: IS482

## Резюме

Доменная сущность `Lexeme` сейчас дублируется в трёх feature-модулях. В `wordcard` и `quiz/chat` — domain-классы `Lexeme.kt` со своими value-классами `Translation`/`Definition`. В `dictionaryTab` domain-слоя нет — там сразу UI-модель `LexemeUiItem` с `TranslationUiEntity`/`DefinitionUiEntity`, маппится `API → UI` напрямую в `DictionaryTabUseCaseImpl`. Маппинг `LexemeApiEntity → <feature>.Lexeme` живёт в `WordCardUseCaseImpl` / `QuizChatUseCaseImpl` (в `app/`), и `LexemeApiEntity → LexemeUiItem` в `DictionaryTabUseCaseImpl`. Задача — создать новую категорию `modules/domain/` и в ней модуль `modules/domain/lexeme` (единый источник domain-типов `Lexeme`, `LexemeId`, `Translation`, `Definition`, computed extensions). Маппер `LexemeApiEntity → Lexeme` живёт в `app/` (общая top-level extension). Wordcard и quiz/chat переключаются на общий domain, локальные `Lexeme.kt` удаляются. **DictionaryTab сохраняет UI-слой** (`LexemeUiItem`, `TranslationUiEntity`, `DefinitionUiEntity`) — composable не работает с domain напрямую (см. `naming.md` § «Сущности по слоям», UI-слой). DictionaryTab получает domain.Lexeme от общего mapper'а и внутри dictionaryTab маппит domain → UI. UI / БД / миграции — не трогаем (бриф явно запрещает изменения UI/UX, схема Room остаётся).

> 📎 guide: docs/guides/data-layer.md — «API → Domain (в UseCase модуле)»: маппер `ApiEntity.toDomainEntity()` принадлежит UseCase-модулю; в архитектуре проекта UseCase-импл живёт в `app/`, значит общий top-level mapper там же — это не drift, а соответствие гайду.

## Замысел задачи

- **Цель.** Устранить тройную копию доменной сущности `Lexeme` и подготовить почву для IS481 (компонентный конструктор) — без нового модуля `domain/lexeme` пришлось бы плодить `@Deprecated` computed extensions в трёх местах вместо одного.
- **Что меняется концептуально.** Вводится новая категория Gradle-модулей `modules/domain/` наряду с `core/`, `screen/`, `widget/`, `library/`, `datasource/`. Первый житель — `lexeme`. Это первый домен-only модуль (без Android/Compose-зависимостей), задаёт шаблон именования и архитектурного слоя для будущих доменов. Принцип: **domain — вершина графа зависимостей, не знает о data/UI/Android** (Dependency Rule).
- **Что НЕ меняется.** UI / composables / layouts; БД-схема (Room) и миграции; контракт `core-db-api` (`LexemeApiEntity`, `CoreDbApi.LexemeApi`); поведение Quiz, dictionaryTab, WordCard на уровне эффектов и сообщений.
- **Семантика после migration.** В wordcard и quiz/chat `import me.apomazkin.wordcard.entity.Lexeme` / `import me.apomazkin.quiz.chat.entity.Lexeme` заменяются на `import me.apomazkin.lexeme.Lexeme`. В dictionaryTab `LexemeUiItem` остаётся как UI-модель — composable продолжают работать с ним. UseCase-интерфейсы (deps) трёх модулей переходят на общий domain `Lexeme` как возвращаемый тип; реализации в `app/` используют один общий маппер `LexemeApiEntity.toDomain(): Lexeme` (top-level extension). Маппинг domain `Lexeme → LexemeUiItem` живёт в dictionaryTab (в reducer'е либо в `DictionaryTabUseCaseImpl` — точное место выбирает business sub-flow).
- **Решение о шейпе общего Lexeme** — задача business sub-flow (там будет сравнение текущих трёх форм и выбор union shape). Здесь только фиксируем что такое решение требуется.

## Spec target

- Существующей feature-spec для общего модуля `lexeme` нет. `docs/features-spec/wordcard.md` упоминает `Lexeme` в контексте mate-state (`LexemeState`, импорт из `wordcard.entity.Lexeme`), но это не спека доменной сущности.
- После завершения фичи спека модуля должна жить в `docs/features-spec/lexeme-domain.md` (новый файл, имя по аналогии с `dictionary-list.md` / `wordcard.md`). Создавать её — задача шага `guides` в finalize, не текущего scope-шага.
- `spec_filename = null` — текущей спеки нет, и решение «создавать ли feature-spec» делегируется finalize-шагу.

## Затронутые слои

- **Infrastructure** — **да**.
  - Новый Gradle-модуль `modules/domain/lexeme` (новая категория `modules/domain/`).
  - `settings.gradle.kts` — добавление `include(":modules:domain:lexeme")` + новый блок-комментарий с категорией.
  - `modules/screen/wordcard/build.gradle.kts` — добавление `implementation(project("path" to ":modules:domain:lexeme"))`.
  - `modules/screen/quiz/chat/build.gradle.kts` — то же.
  - `modules/screen/dictionaryTab/build.gradle.kts` — то же.
  - `app/build.gradle.kts` — добавление dependency на `:modules:domain:lexeme` (в `app/` живут `WordCardUseCaseImpl`, `QuizChatUseCaseImpl`, `DictionaryTabUseCaseImpl` и новый общий mapper, всем им нужен `Lexeme`).
  - Новый `build.gradle.kts` в `modules/domain/lexeme/` — **pure-Kotlin** (`org.jetbrains.kotlin.jvm` / `java-library`), без Android SDK. Это решение зафиксировано (см. аспект `module_kotlin_only`).

- **Business logic** — **да**. Это центральный слой фичи.
  - Перенос/унификация доменных типов `Lexeme`, `LexemeId`, `Translation`, `Definition`. Сейчас три формы:
    - `wordcard`: `LexemeId(Long)`, поля `lexemeId`, `translation`, `definition`, `category: String?`, `addDate`, `changeDate`.
    - `quiz/chat`: `id: Long` (без LexemeId), без `category`.
    - `dictionaryTab.LexemeUiItem`: `id: Long`, `wordId: Long`, `TranslationUiEntity` / `DefinitionUiEntity` (другие value-классы), без `category`. Содержит дополнительное поле `wordId` которого нет в wordcard.Lexeme.

> 📎 guide: docs/guides/naming.md — § «Сущности по слоям»: feature domain-сущности без суффикса (`Lexeme`, `Translation`, `Definition`), `*UiEntity` — не предусмотрен конвенцией.

  - Выбор union-shape (включать ли `wordId`, `category`, какие value-классы оставлять) — задача business sub-flow.
  - Единый маппер `LexemeApiEntity → Lexeme` — общая top-level extension в `app/` (сейчас три места: `WordCardUseCaseImpl.toDomainEntity`, `QuizChatUseCaseImpl.toDomainEntity`, `DictionaryTabUseCaseImpl.getWordList` inline-маппер). Все три UseCaseImpl импортируют общий mapper, локальные копии удаляются. Имя файла и точный пакет (`app/src/main/java/me/apomazkin/polytrainer/mapper/LexemeMapper.kt` либо `app/src/main/java/me/apomazkin/polytrainer/di/module/common/LexemeMapper.kt`) — выбор делает business sub-flow.

> 📎 guide: docs/guides/data-layer.md — § «Маппинг сущностей»: extension `ApiEntity.toDomainEntity()` живёт в UseCase-модуле; общий top-level mapper в `app/` (где живут UseCaseImpl) — прямое соответствие гайду.

  - Computed extensions `Lexeme.builtIn(key)` — заготовка под IS481. В IS482 определяется сигнатура и `null` / TODO-реализация (или совсем не добавляется — решит business).
  - Бриф также упоминает «chain extensions для state mutations» (`enableEdit()` для `TextValueState`). Здесь нужно различать: `TextValueState` — это **UI-state**, не domain (живёт в `wordcard/mate/State.kt`, не имеет аналогов в quiz/chat и dictionaryTab). Перенос его в общий domain-модуль архитектурно сомнителен — это нарушение слоёв (domain не должен знать про mate/UI). **Возражение зафиксировано** ниже в § «Аспекты». Финальное решение — за business sub-flow.

> 📎 guide: docs/guides/state-and-extensions.md — § «Принципы стейта» и § «Конвенции»: extension-функции мутации стейта живут в `State.kt` соответствующего feature-модуля рядом с data class (UI-state-уровень), не в domain-модуле.

- **UI** — **нет**.
  - Бриф явно говорит: «Никаких изменений UI/UX».
  - Composables / widgets читают `lexeme.translation` / `lexeme.definition` через те же поля. При выборе совместимого union-shape подписи не меняются. Если выбран shape с `LexemeId(Long)` (как в wordcard), но `quiz/chat`/`dictionaryTab` сейчас используют сырой `Long` — потребуется правка composable-параметров (например `lexeme.id` → `lexeme.lexemeId.id`). Это правка **импортов и обращений к полям**, не правка UI/UX. Считается частью business sub-flow (миграция консьюмеров), а не UI sub-flow.

- **Data** — **нет**.
  - Бриф не упоминает изменения БД.
  - `core/core-db-api/LexemeApiEntity.kt` — остаётся без изменений (это API-слой data, контракт).
  - `core/core-db-impl/` — без изменений (схемы / миграции / DAO).
  - Маппер `LexemeApiEntity → Lexeme` — это **граница API ↔ domain**, по `data-layer.md` § «Маппинг сущностей» живёт «в UseCase-модуле». В нашей архитектуре UseCase-интерфейс — в feature-модуле, **реализация (UseCaseImpl) — в `app/`**, значит `app/` и есть «UseCase-модуль» для маппера. Раньше маппер был размазан по трём `UseCaseImpl` (`WordCardUseCaseImpl`, `QuizChatUseCaseImpl`, `DictionaryTabUseCaseImpl`); теперь — общая top-level extension в `app/`, доступная всем трём. **Это не правка data-слоя, не drift от data-layer.md, и не инвертирует Dependency Rule** (domain pure-Kotlin не зависит от `core-db-api`, mapper в `app/` зависит и от `core-db-api`, и от `modules/domain/lexeme`).

> 📎 guide: docs/guides/data-layer.md — § «Маппинг сущностей» (DB → API → Domain) и § «UseCase» (интерфейс в feature-модуле, реализация в app-модуле): размещение mapper в `app/` следует контракту гайда напрямую.

  - Prefs / Library — не задеты.

### Доп. флаги

- **`needs_tests` = true** — нужны unit-тесты на:
  - Маппер `LexemeApiEntity.toDomain()` (null-cases для `translation`/`definition`, корректный `LexemeId` wrap, сохранение дат и `wordId`/`category` если они есть в union-shape) — тест живёт в `app/src/test/` (рядом с маппером).
  - Computed extensions если будут добавлены (`Lexeme.builtIn`, `.translation`, `.definition`) — тесты в `modules/domain/lexeme/src/test/`.
- **`needs_migration_tests` = false** — Room-схема не меняется, миграционные тесты не нужны.
- **`feature_has_ui_contract` = false** — UI не меняем, UI-контракта у фичи нет.

## Аспекты

- `chain_extensions_location` — Бриф предлагает перенести `chain extensions для state mutations` (типа `enableEdit()` для `TextValueState`) в общий domain-модуль. **Возражение:** `TextValueState` живёт в `wordcard/mate/State.kt` и не используется в quiz/chat/dictionaryTab. Это **UI-state (mate)**, не domain. Перенос UI-state в `modules/domain/lexeme` нарушает direction зависимостей (domain не должен знать про mate). Если эти расширения действительно «общие» — они должны быть общими **между UI-слоями**, не уехать в domain. Альтернатива: оставить в wordcard или вынести в `core/mate` extensions, если переиспользование подтвердится. Решение — business sub-flow (с явным review позиции).

> 📎 guide: docs/guides/state-and-extensions.md — § «Принципы стейта» (п. 4 «Extension-функции для всех мутаций стейта») и § «Конвенции» (п. 2 «Расширения живут в State.kt рядом с дата-классами»): chain-extensions для `TextValueState` принадлежат UI-state слою feature-модуля, не доменному модулю.

- `lexeme_shape_unification` — Текущие три формы Lexeme отличаются по полям (`wordId` есть в dictionaryTab но нет в wordcard; `category` есть в wordcard но нет в quiz/chat; `LexemeId(Long)` value-class только в wordcard). Нужно выбрать union-shape (включить максимум полей, сделать nullable где не у всех потребителей есть данные) — задача business sub-flow.
- `category_field_future` — `category: String?` в wordcard.Lexeme сейчас всегда `null` (см. маппер `WordCardUseCaseImpl.toDomainEntity` строка 221). При union-shape — оставить ли его. Связано с IS481 (там вводятся `ComponentValue`), но в IS482 — оставляем как есть для backward-compat.
- `value_class_naming` — Два разных слоя, два разных набора имён. **Domain** (после IS482, в `modules/domain/lexeme`): `Translation` / `Definition` без суффикса (по `naming.md` § «Сущности по слоям»). **UI** (в dictionaryTab, остаётся): `TranslationUiEntity` / `DefinitionUiEntity` с суффиксом `UiEntity` — это правильное имя UI-обёртки по обновлённой `naming.md` § «Сущности по слоям» (UI-слой, конвенция `*UiEntity`). Domain и UI имеют **разные** value-классы; маппинг `domain.Translation → TranslationUiEntity` живёт в dictionaryTab. Wordcard и quiz/chat сейчас держат `Translation`/`Definition` локально в feature — после IS482 заменяются на общие domain-типы (composable wordcard/quiz/chat не имеет отдельной UI-обёртки — это решение wordcard/quiz/chat, не IS482).

- `ui_layer_dictionarytab` — dictionaryTab держит **UI-слой** (composable работает с UI-моделями, не domain): `LexemeUiItem`, `TranslationUiEntity`, `DefinitionUiEntity` в `modules/screen/dictionaryTab/entity/`. По обновлённой `naming.md` § «Сущности по слоям» (UI-строка) — это правильные имена: `*UiItem` для item списка, `*UiEntity` для обёртки одной сущности. **Эти файлы НЕ удаляются** в IS482 — IS482 вводит между API и UI новый промежуточный слой (domain), а UI-слой остаётся. Маппинг `domain.Lexeme → LexemeUiItem` живёт в dictionaryTab (mate/reducer либо UseCaseImpl — точное место выбирает business sub-flow).

> 📎 guide: docs/guides/naming.md — § «Сущности по слоям»: domain-классы без суффикса (`Lexeme`, `Translation`); UI-классы с префиксом `Ui` + тип (`*UiItem`, `*UiEntity`, `*UiList`). Domain и UI разделены, маппинг живёт на границе.

- `module_kotlin_only` — **Зафиксированное решение.** Новый модуль `modules/domain/lexeme` — **pure-Kotlin** (`org.jetbrains.kotlin.jvm` / `java-library` plugin), без Android SDK. Это обеспечивает чистоту domain-слоя (Dependency Rule: domain ничего не знает о data/UI/Android). Использование `java.util.Date` для полей дат — ок (это JDK, не Android SDK). Если позже потребуется `androidx.annotation.StringRes` — будет звоночек, что domain тянет UI-смысл. Этот аспект **связан** с `mapper_location`: pure-Kotlin domain не может зависеть от `core-db-api` (последний — `com.android.library`, aar), поэтому mapper не может жить в domain-модуле.
- `package_path_drift_rule` — **Зафиксированное решение.** Пакет доменных классов нового модуля — `me.apomazkin.lexeme` (без `.entity`-сегмента). `naming.md` § «Пакеты» рекомендует `me.apomazkin.<module>.entity` для domain-моделей, но эта конвенция родилась для feature-модулей (`screen/`, `widget/`), где `entity/` отделяет домен от `ui/`, `mate/`, `tools/`. **drift_rule:** для категории `modules/domain/`, где модуль сам является доменом и не содержит UI/mate/tools-подпакетов, suffix `.entity` избыточен — `Lexeme.kt` лежит в корне пакета модуля (`me.apomazkin.lexeme`), не в подпакете `.entity`. Это сознательное расширение конвенции для нового вида модулей domain-as-a-module. Если в будущем у domain-модуля появятся вспомогательные подпакеты (например `me.apomazkin.lexeme.ext`) — это не противоречит правилу: корень пакета остаётся за доменными типами.

> 📎 guide: docs/guides/naming.md — § «Пакеты»: рекомендация `me.apomazkin.<module>.entity` для domain-моделей; для нового domain-as-a-module модуля это сознательный drift (фиксируется как `package_path_drift_rule`).

- `mapper_location` — **Зафиксированное решение.** Mapper `LexemeApiEntity → Lexeme` живёт в `app/` как общая top-level extension (имя файла — `LexemeMapper.kt`, точный пакет — `me.apomazkin.polytrainer.mapper` либо `me.apomazkin.polytrainer.di.module.common` — выбор делает business sub-flow). Это соответствует `data-layer.md` § «Маппинг сущностей» (`// API → Domain (в UseCase модуле)`) — в нашей архитектуре `app/` это UseCase-модуль (там реализации UseCase). **drift_rule не нужен** — мы НЕ отходим от guide. Три UseCaseImpl (`WordCardUseCaseImpl`, `QuizChatUseCaseImpl`, `DictionaryTabUseCaseImpl`) импортируют один общий mapper, локальные копии (`toDomainEntity` extensions, inline-конструкторы `LexemeUiItem`) удаляются. Domain pure-Kotlin не зависит от `core-db-api`, Dependency Rule соблюдён.

> 📎 guide: docs/guides/data-layer.md — § «Маппинг сущностей» и § «UseCase»: `API → Domain` extension живёт в UseCase-модуле (в проекте — `app/`, где находятся `*UseCaseImpl`); общий mapper в `app/` — прямое следование контракту.

- `lexemeid_for_quiz_dictionarytab` — quiz/chat и dictionaryTab сейчас используют `id: Long` напрямую, без `LexemeId` value-class. После унификации они должны переключиться на `LexemeId(Long)`. Это правка в коде reducer / composable (`lexeme.id` → `lexeme.lexemeId.id`). Считается частью business sub-flow (миграция консьюмеров на общий контракт).
- `name_collision_with_lexemelabel` — В `modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/entity/Lexeme.kt` лежит отдельный файл с тем же именем, но содержит `LexemeLabel` enum (лексическая категория: noun/verb/adjective/...) и helpers `toLexemeLabel`, `lexicalCategory`, `toChipPicker`. Это **не** доменный `Lexeme` — имя файла случайно совпадает с фичей IS482. Файл уже помечен `@Deprecated` с TODO о переименовании в `LexicalCategoryLabel`. **Переименование файла — out of scope IS482** (отдельный tech-debt cleanup). См. § «Не трогаем».

## Затронутые файлы

### Создание

- `modules/domain/lexeme/build.gradle.kts` — новый Gradle-модуль, pure-Kotlin (`org.jetbrains.kotlin.jvm` / `java-library`) (infra).
- `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/Lexeme.kt` — `Lexeme`, `LexemeId`, `Translation`, `Definition` (business; точный пакет/имя файла подтверждает business sub-flow).
- `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/LexemeExt.kt` — `Lexeme.builtIn(key)` и computed shims (если решено добавить заранее) (business).
- `app/src/main/java/me/apomazkin/polytrainer/mapper/LexemeMapper.kt` — общая top-level extension `LexemeApiEntity.toDomain(): Lexeme` (имя/локация — business sub-flow; альтернативная локация `app/src/main/java/me/apomazkin/polytrainer/di/module/common/LexemeMapper.kt`) (business).
- `modules/domain/lexeme/src/test/java/me/apomazkin/lexeme/LexemeExtTest.kt` — unit-тесты computed extensions, если добавлены (tests).
- `app/src/test/java/me/apomazkin/polytrainer/mapper/LexemeMapperTest.kt` — unit-тесты маппера (null-cases для translation/definition, корректный LexemeId wrap, сохранение `wordId`/`category` если они в union-shape) (tests).

### Удаление

- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/entity/Lexeme.kt` — переносится в общий модуль (это domain-класс wordcard).
- `modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/entity/Lexeme.kt` — переносится (это domain-класс quiz/chat).

**Замечание:** `modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/entity/LexemeUiItem.kt` — **НЕ удаляется**. Это UI-модель dictionaryTab (см. аспект `ui_layer_dictionarytab`). После IS482 поля `LexemeUiItem` могут измениться (например `wordId: Long` остаётся, типы value-полей `TranslationUiEntity`/`DefinitionUiEntity` остаются как UI-обёртки). См. § «Правка / UI dictionaryTab».

### Правка

**Infra:**

- `settings.gradle.kts` — `include(":modules:domain:lexeme")` + новый комментарий-блок `//Domain`.
- `modules/screen/wordcard/build.gradle.kts` — dependency на `:modules:domain:lexeme`.
- `modules/screen/quiz/chat/build.gradle.kts` — то же.
- `modules/screen/dictionaryTab/build.gradle.kts` — то же.
- `app/build.gradle.kts` — dependency на `:modules:domain:lexeme` (mapper и UseCaseImpl используют `Lexeme`).

**Business — миграция консьюмеров (main):**

- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/entity/Term.kt` — `lexemeList: List<Lexeme>` тип сохраняется, но `Lexeme` импортируется из общего модуля.
- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/State.kt` — import `Lexeme` из нового пути.
- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/Message.kt` — import.
- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/deps/WordCardUseCase.kt` — import + типы возврата.
- `modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/entity/WriteQuiz.kt` — заменить локальный `Lexeme` на общий.
- `modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/quiz/QuizGameImpl.kt` — обращения `lexeme.id` → `lexeme.lexemeId.id` (если union-shape принимает `LexemeId`), плюс возможные правки `lexeme.translation.value` / `lexeme.definition.value` (имена value-классов сохраняются).
- `modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/entity/LexemeUiItem.kt` — **остаётся**, возможно дополняется (например явный маппер `Lexeme.toUiItem(): LexemeUiItem` рядом). Поля и UI-классы (`TranslationUiEntity`/`DefinitionUiEntity`) — без изменений.
- `modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/tools/DataHelper.kt` — моки остаются на `LexemeUiItem` (UI-слой), либо если требуется domain-моки — добавить отдельно. Точное решение — business sub-flow.
- `modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/ui/widget/lexeme/LexemeWidget.kt` / `DefinitionWidget.kt` / `TranslationWidget.kt` — **импорты UI-моделей не меняются** (composable продолжает работать с `LexemeUiItem` / `TranslationUiEntity` / `DefinitionUiEntity`).
- `modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/entity/TermUiItem.kt` — `lexemeList: List<LexemeUiItem>` **остаётся** (UI-слой, не меняется).
- `modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/logic/State.kt` — если использует `LexemeUiItem` в state — остаётся как есть.
- **Новое (business sub-flow решит конкретику):** в dictionaryTab появляется маппинг `domain.Lexeme → LexemeUiItem`. Где живёт — варианты: (a) extension в `dictionaryTab/entity/LexemeUiItem.kt` (`Lexeme.toUiItem()`); (b) в reducer/EffectHandler dictionaryTab; (c) в `DictionaryTabUseCaseImpl` (если контракт UseCase возвращает UI-модель, а не domain — текущий стиль проекта).

**Business — `app/` UseCaseImpl и mapper:**

- `app/src/main/java/me/apomazkin/polytrainer/di/module/wordCard/WordCardUseCaseImpl.kt` — удалить локальный `LexemeApiEntity.toDomainEntity()` (строки 216-225), импортировать общий mapper.
- `app/src/main/java/me/apomazkin/polytrainer/di/module/quizchat/QuizChatUseCaseImpl.kt` — удалить локальный `LexemeApiEntity.toDomainEntity()` (строки 117-123), импортировать общий mapper.
- `app/src/main/java/me/apomazkin/polytrainer/di/module/dictionarytab/DictionaryTabUseCaseImpl.kt` — изменить inline-маппинг `LexemeApiEntity → LexemeUiItem` (строки 108-121, 140-153): теперь идёт через **двойной маппинг** `LexemeApiEntity → domain.Lexeme (общий mapper) → LexemeUiItem (mapping в dictionaryTab)`. Конкретное расположение второго маппинга (extension `Lexeme.toUiItem()` в dictionaryTab или внутри `DictionaryTabUseCaseImpl`) — business sub-flow.

**Tests — правка импортов:**

- `modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/LexemeManagementTest.kt` — import `Lexeme`, `LexemeId`, `Translation`, `Definition` из нового пути.
- `modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/WordLoadedTest.kt` — то же.
- `modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/DatasourceEffectHandlerTest.kt` — то же.

_Полный перечень wordcard-тестов с импортами `me.apomazkin.wordcard.entity.{Lexeme,LexemeId,Translation,Definition}` (найден через rg): три файла выше. В `quiz/chat` и `dictionaryTab` тестовых файлов с импортами доменного `Lexeme` **нет** — подтверждено пустым выводом rg по `modules/screen/quiz/chat/src/test/` и `modules/screen/dictionaryTab/src/test/`._

- `app/src/test/java/me/apomazkin/polytrainer/di/module/quizchat/QuizChatUseCaseImplTest.kt` — только `LexemeApiEntity` (не доменный `Lexeme`); правка не требуется, но проверить компилируется ли после удаления `toDomainEntity`.

### Не трогаем

- `modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/entity/Lexeme.kt` — **имя файла случайно совпадает с фичей IS482.** Содержимое: `LexemeLabel` enum (лексическая категория слова: noun/verb/adjective/...) + helpers `toLexemeLabel`, `lexicalCategory`, `toChipPicker`. Это **не** доменный `Lexeme` (нет полей translation/definition/id), это UI-категоризация для chip picker'а в dictionaryTab. Файл помечен `@Deprecated("Old as ass hole")` и содержит TODO о переименовании в `LexicalCategoryLabel` — но переименование файла **out of scope IS482** (отдельный tech-debt cleanup, не связан с domain unification). Файл остаётся как есть. См. также аспект `name_collision_with_lexemelabel`.

## Релевантные спеки и гайды

- `docs/guides/data-layer.md` § «Маппинг сущностей» (строки 79-105) — три слоя сущностей `DB → API → Domain`, маппер `API → Domain` живёт в UseCase-модуле. В нашей архитектуре `app/` = UseCase-модуль (там UseCaseImpl). После IS482 — общая top-level extension в `app/` (вместо трёх локальных копий).
- `docs/guides/data-layer.md` § «UseCase» (строки 107-135) — интерфейс UseCase в feature-модуле, реализация в app-модуле. Не меняется в IS482, только источник типа `Lexeme` смещается.
- `docs/guides/naming.md` § «Сущности по слоям» — domain без суффикса (`Lexeme`); UI-слой с префиксом `Ui` + тип (`*UiItem`, `*UiEntity`, `*UiList`). Подтверждает: domain `Lexeme` в общем модуле; UI `LexemeUiItem` / `TranslationUiEntity` / `DefinitionUiEntity` в dictionaryTab — правильные имена для UI-слоя.
- `docs/guides/naming.md` § «Пакеты» (строки 21-29) — `me.apomazkin.<module>.entity` для domain-моделей. Для нового модуля: `me.apomazkin.lexeme` (модуль = домен).
- `docs/guides/state-and-extensions.md` § «Принципы стейта» — даёт основу для **возражения** по поводу выноса chain extensions для `TextValueState` в общий domain-модуль (state-extensions живут в State.kt feature-модуля).
- `docs/features/IS481_lexeme_component_constructor/02_design_sketch.md` § «Domain-слой» (строки 80-141) — описывает что нужно от `modules/domain/lexeme` ПОСЛЕ IS481 (поле `components: List<ComponentValue>`, computed `.translation` / `.definition` shim'ы). IS482 готовит модуль с текущим (pre-IS481) shape; после IS481 — модуль расширится.
- `docs/features/IS481_lexeme_component_constructor/05_migration_strategy_review.md` § «Domain Lexeme дублируется в 3+ модулях» (строки 105-107) — оригинальное обоснование IS482, источник решения.
- `docs/Backlog.md` § ВекторныйПиздеж — «Domain unification: `modules/domain/lexeme`» (источник задачи, см. бриф).

## Sub-flow для запуска

| Sub-flow | Запускать? | Обоснование |
|---|---|---|
| Infrastructure | да | Создание новой Gradle-категории `modules/domain/`, нового модуля `modules/domain/lexeme` с собственным `build.gradle.kts` (pure-Kotlin, решение зафиксировано), правки `settings.gradle.kts` и четырёх `build.gradle.kts` (три feature + app). Это первый шаг — без него остальное не компилируется. |
| Business | да | Унификация трёх форм `Lexeme` в один union shape, перенос value-классов `Translation`/`Definition`, создание общего mapper'а в `app/` (точное имя файла/пакета), миграция трёх feature-модулей и трёх `UseCaseImpl` в `app/` на общий контракт. Решение по chain extensions (отдельно от domain — возражение из § Аспекты). Решение по `Lexeme.builtIn(...)` (заготовка под IS481 или отложить). Это сердцевина фичи. |
| UI | нет | Бриф явно запрещает изменения UI/UX. Composables читают те же поля через те же интерфейсы. Любые правки import / обращений к полям в composables — миграционные, считаются частью business sub-flow (миграция консьюмеров на общий контракт), не UI sub-flow. |
| Data | нет | Бриф не упоминает изменения БД. `core-db-api/LexemeApiEntity` и `core-db-impl` (схема, миграции, DAO) — без изменений. Маппер `LexemeApiEntity → Lexeme` — это граница API ↔ domain, а не data-слой; mapper переезжает из inline в трёх UseCaseImpl в общую top-level extension в `app/` (UseCase-модуле по `data-layer.md`). Это перенос внутри одного слоя (UseCase), не правка data-слоя. |

## context_output

```yaml
context_output:
  infra_touched: true
  business_touched: true
  ui_touched: false
  data_touched: false
  needs_tests: true
  needs_migration_tests: false
  feature_has_ui_contract: false
  spec_filename: null
```

_model: Opus 4.7 (1M context)_
