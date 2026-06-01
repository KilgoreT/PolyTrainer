# Test: business — IS482

## Решение: тесты нужны

Унификация `Lexeme` вводит два публичных маппера на границах слоёв:

1. **`LexemeApiEntity.toDomain(): Lexeme`** — общий API → domain mapper (app/.../mapper/LexemeMapper.kt), заменяет три дубликата (два top-level `toDomainEntity` + inline в dictionaryTab).
2. **`Lexeme.toUiItem(): LexemeUiItem`** — domain → UI mapper в dictionaryTab.

> 📎 guide: naming.md § R-N-011 — суффиксы по слою: API → `ApiEntity`, domain → без суффикса, UI элемент списка → `UiItem`.

Оба маппера — публичное поведение, точки риска при дальнейшем сопровождении (изменение domain-shape, добавление полей, ошибки в nullable-propagation). Покрываем unit-тестами.

Поля `wordClass` / `options` из `LexemeApiEntity` сознательно НЕ пробрасываются в domain (см. `business_contract.md` § Domain shape). Это контрактное решение, нужно зафиксировать тестом.

---

## Что сделано

### Создано

- `app/src/test/java/me/apomazkin/polytrainer/mapper/LexemeMapperTest.kt` — 11 test methods.
- `modules/screen/dictionaryTab/src/test/java/me/apomazkin/dictionarytab/entity/LexemeUiItemTest.kt` — 10 test methods.

> 📎 guide: naming.md § Tests — имя класса теста `*Test.kt`.

### Test framework

Проект использует **JUnit 4** + `org.junit.Assert.*`:
- `app/` — `testImplementation(testLibs.junit)` + `mockk` + `coroutinesTest` (см. `app/build.gradle.kts:166-168`).
- `dictionaryTab` — `testImplementation("junit:junit:4.13.2")` + `:modules:core:mate` (см. `modules/screen/dictionaryTab/build.gradle.kts:53-54`).

Стиль теста (BDD-нейминг `given ... when ... then ...`) взят из существующих тестов в обоих модулях:
- `modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/LexemeManagementTest.kt`
- `modules/screen/dictionaryTab/src/test/java/me/apomazkin/dictionarytab/logic/ext/LoadingExtTest.kt`

> 📎 guide: testing-extensions.md § Структура теста — Given/When/Then блоки; имя метода `should [ожидание] when [условие]` (для extension-стиля). BDD-нейминг `given ... when ... then ...` — альтернативный стиль, согласованный с существующими тестами проекта.
>
> 📎 guide: naming.md § Tests — имя метода теста: backtick-string в свободной форме («что проверяем»).

Мокинг не нужен — мапперы — pure functions.

### Покрытие — `LexemeMapperTest`

| # | Test | Что проверяет |
|---|---|---|
| 1 | `given null translation when toDomain then domain translation is null` | nullable propagation `translation` |
| 2 | `given non-null translation when toDomain then wraps into Translation value class` | wrap `TranslationApiEntity` → `Translation` |
| 3 | `given null definition when toDomain then domain definition is null` | nullable propagation `definition` |
| 4 | `given non-null definition when toDomain then wraps into Definition value class` | wrap `DefinitionApiEntity` → `Definition` |
| 5 | `given id when toDomain then wraps into LexemeId` | wrap `Long` → `LexemeId` |
| 6 | `given wordId when toDomain then wordId is passed through unchanged` | passthrough `wordId` |
| 7 | `given addDate when toDomain then addDate is passed through unchanged` | passthrough `addDate` |
| 8 | `given non-null changeDate when toDomain then changeDate is passed through unchanged` | passthrough `changeDate` non-null |
| 9 | `given null changeDate when toDomain then changeDate is null` | nullable propagation `changeDate` |
| 10 | `given wordClass and options in api when toDomain then domain ignores them` | контрактное: `wordClass` / `options` НЕ влияют на domain |
| 11 | `given full api entity when toDomain then all mapped fields match` | end-to-end smoke с непустыми значениями |

> 📎 guide: testing-extensions.md § Порядок тест-кейсов — Boundary → Standard → Edge; nullable cases (1, 3, 9) — boundary, passthrough (6, 7, 8) — standard, smoke (11) — edge/end-to-end.
>
> 📎 guide: testing-extensions.md § Документация класса — нумерованный список всех тест-кейсов в KDoc класса.

### Покрытие — `LexemeUiItemTest`

| # | Test | Что проверяет |
|---|---|---|
| 1 | `given null translation when toUiItem then ui translation is null` | nullable propagation `translation` |
| 2 | `given non-null translation when toUiItem then wraps into TranslationUiEntity` | wrap domain → UI value class |
| 3 | `given null definition when toUiItem then ui definition is null` | nullable propagation `definition` |
| 4 | `given non-null definition when toUiItem then wraps into DefinitionUiEntity` | wrap domain → UI value class |
| 5 | `given LexemeId when toUiItem then id is unwrapped to raw Long` | unwrap `LexemeId.id` → `Long` (UI всё ещё использует сырой `Long`) |
| 6 | `given wordId when toUiItem then wordId is passed through unchanged` | passthrough `wordId` |
| 7 | `given addDate when toUiItem then addDate is passed through unchanged` | passthrough `addDate` |
| 8 | `given non-null changeDate when toUiItem then changeDate is passed through unchanged` | passthrough `changeDate` non-null |
| 9 | `given null changeDate when toUiItem then changeDate is null` | nullable propagation `changeDate` |
| 10 | `given full domain Lexeme when toUiItem then all mapped fields match` | end-to-end smoke |

> 📎 guide: naming.md § R-N-011 — `*UiEntity` для обёртки одного домен-объекта (`TranslationUiEntity`, `DefinitionUiEntity`), `*UiItem` для элемента списка (`LexemeUiItem`).

### Что НЕ покрыто (осознанно)

- **Отсутствие `category` / `wordClass` / `options` на `Lexeme`** — это compile-time гарантия (поля просто нет в data class). Отдельный runtime-тест не нужен. Косвенно зафиксировано тестом #10 `LexemeMapperTest` (домен идентичен при разных `wordClass`/`options` в API).
- **`Term` миграция** — `Term` остаётся в feature-модуле wordcard, не входит в IS482 scope.
- **`QuizGameImpl.kt:511` правка `lexeme.id` → `lexeme.lexemeId.id`** — это правка консьюмера, не отдельная функция. Покрывается компиляцией модуля quiz/chat после миграции.
- **`WordCardUseCaseImpl` / `QuizChatUseCaseImpl` / `DictionaryTabUseCaseImpl` после миграции** — поведенческого изменения нет (тип сменился, семантика та же); существующие `app/test/` тесты (например `QuizChatUseCaseImplTest`) проверяют сценарии через `getRandomWriteQuizList`, что косвенно валидирует mapper-цепочку.

> 📎 guide: testing-extensions.md § Что НЕ тестировать в тестах расширений — бизнес-логику (задача редьюсера), сайд-эффекты, генерацию эффектов, сложные сценарии.

---

## TDD status

Тесты **не компилируются** на момент написания — это **ожидаемо** по TDD:

- `me.apomazkin.lexeme.{Lexeme, LexemeId, Translation, Definition}` пока существуют только как placeholder (`modules/domain/lexeme/.../Lexeme.kt` содержит только `package` declaration без типов).
- `LexemeApiEntity.toDomain()` extension ещё не создан (`app/src/main/java/me/apomazkin/polytrainer/mapper/LexemeMapper.kt` отсутствует).
- `Lexeme.toUiItem()` extension ещё не создан в `LexemeUiItem.kt`.

Запуск `./gradlew :app:testDebugUnitTest --tests "*.LexemeMapperTest"` подтвердил ожидаемые `Unresolved reference` ошибки на:
- `me.apomazkin.lexeme.Lexeme` / `LexemeId` / `Translation` / `Definition` (узел 1 DAG, business_implement).
- `toDomain` (узел 2 DAG, business_implement).

После выполнения `business_implement` (узлы 1, 2, 3 design tree):
1. `Lexeme.kt` заполнится финальными типами.
2. `LexemeMapper.kt` будет создан.
3. `LexemeUiItem.kt` получит extension `toUiItem()`.

Тесты должны зазеленеть без правок.

---

## Запуски

```bash
./gradlew :app:testDebugUnitTest --tests "*.LexemeMapperTest"
# → BUILD FAILED (compile errors: Unresolved reference 'lexeme', 'toDomain', 'LexemeId', 'Translation', 'Definition')
# → ожидаемо (TDD: красный → реализация → зелёный)
```

После `business_implement` запустим повторно:

```bash
./gradlew :app:testDebugUnitTest --tests "*.LexemeMapperTest"
./gradlew :modules:screen:dictionaryTab:testDebugUnitTest --tests "*.LexemeUiItemTest"
```

---

## log_messages

- Созданы два теста-маппера (11 + 10 кейсов) на `LexemeApiEntity.toDomain()` и `Lexeme.toUiItem()`; покрыты nullable propagation, value-class wrap/unwrap, passthrough, контрактное игнорирование `wordClass`/`options`.
- Test framework — JUnit 4 + `org.junit.Assert.*` (тот же стек что в `wordcard/test/` и `dictionaryTab/test/`); зависимости в `build.gradle.kts` уже на месте, новых не добавляли.
- TDD-статус подтверждён: `./gradlew :app:testDebugUnitTest --tests "*.LexemeMapperTest"` падает на compile с `Unresolved reference` на domain-типы и `toDomain` — ожидаемо, реализация в `business_implement`.

_model: Opus 4.7 (1M context)_
