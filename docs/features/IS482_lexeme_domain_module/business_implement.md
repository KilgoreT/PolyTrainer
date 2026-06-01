# Business implement: IS482 — Lexeme domain unification

## Что создано

- `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/Lexeme.kt` — финальные domain-типы (`LexemeId`, `Translation`, `Definition`, `Lexeme`). Placeholder заменён на полный shape: `lexemeId: LexemeId`, `wordId: Long`, `translation: Translation?`, `definition: Definition?`, `addDate: Date`, `changeDate: Date? = null`. Без `wordClass` / `options` / `category` (out-of-scope по бризу).

> 📎 guide: naming.md § Сущности по слоям / R-N-011 — Feature domain (`modules/domain/<name>/`) без суффикса (`Lexeme`, `LexemeId`, `Translation`, `Definition`).
>
> 📎 guide: project-architecture.md § Граф модулей — общий domain-модуль живёт в `modules/domain/<name>/`, feature-модули зависят от core/domain, не наоборот.

- `app/src/main/java/me/apomazkin/polytrainer/mapper/LexemeMapper.kt` — общий top-level extension `LexemeApiEntity.toDomain(): Lexeme`. Single-expression body. `wordClass` / `options` не пробрасываются.

> 📎 guide: data-layer.md § Маппинг сущностей — три слоя сущностей (DB → API → Domain), каждый через extension-функцию (`.toDomainEntity()` / `.toDomain()`). API → Domain маппер живёт в UseCase-модуле (app).

## Что изменено

### Domain → UI mapper

- `modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/entity/LexemeUiItem.kt` — добавлен extension `Lexeme.toUiItem(): LexemeUiItem` рядом с UI-моделью; import `me.apomazkin.lexeme.Lexeme`. Сам data class и value-классы (`TranslationUiEntity`, `DefinitionUiEntity`) без изменений.

> 📎 guide: naming.md § Сущности по слоям / R-N-011 — UI data class с префиксом `Ui` + тип: `*UiEntity` (одиночная обёртка), `*UiItem` (элемент списка). Маппинг `Domain → Ui*` — extension рядом с UI-моделью.

### wordcard main (миграция импортов на общий domain)

- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/entity/Term.kt` — добавлен `import me.apomazkin.lexeme.Lexeme` (тип `Lexeme` теперь приходит из общего модуля; сигнатура `Term.lexemeList: List<Lexeme>` идентична).
- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/deps/WordCardUseCase.kt` — import `wordcard.entity.Lexeme` → `me.apomazkin.lexeme.Lexeme`. Семь сигнатур (`getTermById`, `deleteLexeme`, `addLexemeTranslation`, `addLexemeDefinition`, `restoreLexeme`, `RemoveTranslationResult.TranslationRemoved`, `RemoveDefinitionResult.DefinitionRemoved`) — без изменения формы, источник типа сменился.

> 📎 guide: naming.md § Пакеты — `me.apomazkin.<module>.deps` для UseCase-интерфейсов.
>
> 📎 guide: data-layer.md § UseCase — интерфейс UseCase живёт в feature-модуле, реализация в app-модуле.

- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/State.kt` — import переключён на общий domain. `Lexeme.toLexemeState()` (`:245-261`) работает без изменений (имена полей `lexemeId.id`, `translation.value`, `definition.value` идентичны).

> 📎 guide: naming.md § Сущности по слоям / R-N-011 — TEA-state (`LexemeState`, `WordCardState`) без `Ui` префикса. Это внутренняя структура редьюсера, не UI-data class.
>
> 📎 guide: naming.md § Database / R-N-006 — колонка со «значением» сущности именуется `value` (отсюда `translation.value`, `definition.value`).

- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/Message.kt` — import переключён. `Msg.RefreshLexemeList(val lexemes: List<Lexeme>)` без изменений.

> 📎 guide: naming.md § Классы и интерфейсы — sealed interface `Msg` (всегда `Msg`).

### wordcard tests (миграция импортов + adapt к новой форме Lexeme)

- `modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/LexemeManagementTest.kt` — импорты `wordcard.entity.{Definition, Lexeme, LexemeId, Translation}` → `me.apomazkin.lexeme.*`. Поскольку у нового `Lexeme` отсутствует `category` и появился обязательный `wordId: Long`, заменены два positional-конструктора `Lexeme(LexemeId(2L), Translation("b"), null, null, Date(0))` → `Lexeme(LexemeId(2L), 1L, Translation("b"), null, Date(0))`.
- `modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/WordLoadedTest.kt` — импорты мигрированы. Два named-конструктора `Lexeme(...)` — убран `category = null`, добавлен `wordId = ...L`. Positional-конструкторы в массиве лексем приведены к новой сигнатуре (с `wordId`).
- `modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/DatasourceEffectHandlerTest.kt` — аналогично: импорты, `category = null` удалён, `wordId = ...L` добавлен. Логика тестов сохранена.

> 📎 guide: naming.md § Tests — имя класса `*Test.kt`, имена тестов backtick-string в свободной форме.

### quiz/chat (миграция на общий domain + .lexemeId.id вместо .id)

- `modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/entity/WriteQuiz.kt` — добавлен `import me.apomazkin.lexeme.Lexeme`. `val lexeme: Lexeme` — сигнатура та же, источник типа теперь общий domain (`Lexeme` теперь содержит `LexemeId`, а не сырой `Long id`).
- `modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/quiz/QuizGameImpl.kt` — `.lexeme.id` → `.lexeme.lexemeId.id` (узел DAG 15, строка ~511). Дополнительно правки smart-cast: после унификации `Lexeme` живёт в другом модуле, и Kotlin не может smart-cast'ить `lexeme.translation` / `lexeme.definition` после проверки на `!= null`. Заменено на `!!.value` в четырёх местах (строки 463, 492, 501, 503). `quiz.chat.entity.Lexeme` импорта в этом файле не было — оставлять было нечего.
- `modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/deps/QuizChatUseCase.kt` — не правился: `Lexeme` напрямую в этом файле не использовался (только транзитивно через `WriteQuiz`).

### app/ UseCaseImpl (выпил локальных mapper'ов, подключение общего)

- `app/src/main/java/me/apomazkin/polytrainer/di/module/wordCard/WordCardUseCaseImpl.kt` — удалён локальный `LexemeApiEntity.toDomainEntity()` (`:216-225`). Семь call-sites (`:49, :68, :78, :101, :111, :153, :212`) переключены с `.toDomainEntity()` на `.toDomain()`. Импорт общего mapper'а добавлен. `TermApiEntity.toDomainEntity()` (`:202-214`) и его вызов на `:36` оставлены без изменений (Term не унифицируется в IS482). Импорт `Lexeme` сменён на `me.apomazkin.lexeme.Lexeme`; неиспользуемые импорты value-классов wordcard удалены.

> 📎 guide: data-layer.md § UseCase — реализация UseCase живёт в app-модуле (`app/.../WordCardUseCaseImpl.kt`), получает `CoreDbApi.*Api` через constructor injection.
>
> 📎 guide: naming.md § Классы и интерфейсы — UseCases: интерфейс `*UseCase`, реализация `*UseCaseImpl`.

- `app/src/main/java/me/apomazkin/polytrainer/di/module/quizchat/QuizChatUseCaseImpl.kt` — удалён локальный `LexemeApiEntity.toDomainEntity()` (`:117-123`). Один call-site (`WriteQuizComplexEntity.toDomainEntity` на `:138`) переключён на общий `lexemeData.toDomain()`. Удалены неиспользуемые импорты `quiz.chat.entity.{Lexeme, Translation, Definition}`. Добавлен импорт общего mapper'а.
- `app/src/main/java/me/apomazkin/polytrainer/di/module/dictionarytab/DictionaryTabUseCaseImpl.kt` — inline-маппинг `LexemeApiEntity → LexemeUiItem` в `getWordList` (`:108-121`) и `searchTerms` (`:140-153`) заменён двойным маппингом `term.lexemes.map { it.toDomain() }.map { it.toUiItem() }`. Импорты `LexemeUiItem`, `TranslationUiEntity`, `DefinitionUiEntity` удалены за ненадобностью; добавлены импорты общего mapper'а и `dictionarytab.entity.toUiItem`. Контракт `DictionaryTabUseCase` (возврат `List<TermUiItem>` / `Flow<PagingData<TermUiItem>>`) сохранён.

> 📎 guide: data-layer.md § Маппинг сущностей — `API → Domain → UI`, двойной маппинг через extension-функции на каждом слое.

## Что удалено

- `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/entity/Lexeme.kt` — старый feature-локальный domain wordcard (4 типа).
- `modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/entity/Lexeme.kt` — старый feature-локальный domain quiz/chat (3 типа, без `LexemeId`).
- `app/src/main/java/me/apomazkin/polytrainer/di/module/wordCard/WordCardUseCaseImpl.kt:216-225` — локальный `LexemeApiEntity.toDomainEntity()`.
- `app/src/main/java/me/apomazkin/polytrainer/di/module/quizchat/QuizChatUseCaseImpl.kt:117-123` — локальный `LexemeApiEntity.toDomainEntity()`.

## Build / Test результат

- `./gradlew compileDebugKotlin` — SUCCESS (exit 0).
- `./gradlew :app:testDebugUnitTest --tests "*LexemeMapperTest"` — SUCCESS, 11 тестов / 0 failures.
- `./gradlew :modules:screen:dictionaryTab:testDebugUnitTest --tests "*LexemeUiItemTest"` — SUCCESS, 10 тестов / 0 failures.
- `./gradlew :modules:screen:wordcard:testDebugUnitTest` — SUCCESS, все suite'ы зелёные (LexemeManagementTest 7/0, WordLoadedTest 4/0, DatasourceEffectHandlerTest 12/0, плюс остальные — все 0 failures).
- `./gradlew :app:testDebugUnitTest` — SUCCESS, в т.ч. QuizChatUseCaseImplTest (3/0) и DictionaryTabUseCaseImplTest (6/0).
- `./gradlew :modules:screen:quiz:chat:testDebugUnitTest` — SUCCESS.

## Решения по ходу

1. **Smart-cast в QuizGameImpl.kt.** После переноса `Lexeme` в `modules/domain/lexeme` Kotlin перестал делать smart-cast `lexeme.translation: Translation?` → `Translation` после `if (lexeme.translation != null)`, потому что поле публично в чужом модуле (compile error: «Smart cast to 'Translation' is impossible, because 'translation' is a public API property declared in different module»). Решение — заменить четыре обращения на `!!.value` (строки 463, 492, 501, 503). Поведение идентично прежнему (контракт сохраняется через `when`-ветку), но кода чуть больше. Альтернатива (вынести в `val t = lexeme.translation ?: ...`) дала бы такой же байткод, выбран минимально-инвазивный фикс.

2. **Wordcard test files — больше чем только импорты.** DAG в узлах 9-11 указывал «Замена 4 импортов. Логика тестов без изменений». Это формально верно, но фактическая сигнатура `Lexeme` сменилась: исчез `category: String?`, появился обязательный `wordId: Long`. В тестах было `Lexeme(... category = null, ...)` и positional `Lexeme(LexemeId(2L), Translation("b"), null, null, Date(0))` — без правок не компилировалось. Сделано минимально: убран `category = null`, добавлен `wordId = N` / в positional — заменён аргумент категории на `wordId`. Логика тестов (assertions, мок-поведение) сохранена. Это не нарушение DAG, а его необходимая интерпретация: «логика без изменений», но «форма Lexeme — новая, и тестовые литералы под неё подгоняются».

3. **`!!.value` vs локальная `val`.** Можно было ввести `val translation = lexeme.translation ?: return@buildAnnotatedString` или вынести `val t = lexeme.translation` после `when`-ветки. Выбран `!!` как минимально-инвазивный (одна правка vs четыре локальных val'а). Семантика идентична — в `when`-ветке `lexeme.translation != null` гарантирует non-null.

## log_messages

- IS482 business_implement выполнен: домен унифицирован, общий mapper `LexemeApiEntity.toDomain()` создан, три feature-модуля (wordcard, quiz/chat, dictionaryTab) мигрированы на общий `me.apomazkin.lexeme.*`. Удалены два старых feature-локальных `Lexeme.kt` и два дубликата `toDomainEntity`. Все TDD-тесты (`LexemeMapperTest` 11/0, `LexemeUiItemTest` 10/0) зелёные.
- Возникла одна нетривиальная правка вне DAG: smart-cast errors в `QuizGameImpl.kt` после переноса `Lexeme` в другой модуль — починено через `!!.value` в четырёх местах. Это естественное следствие cross-module move, не отклонение от scope.
- Tests миграция (узлы 9-11) потребовала более чем замены импортов: новая форма `Lexeme` без `category` и с обязательным `wordId` означала правку литералов. Сделано минимально, логика и assertion'ы сохранены.

_model: Opus 4.7 (1M context)_
